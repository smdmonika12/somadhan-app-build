-- ধাপ ৪০ — resolve_dispute dual-write fix + ব্যবহারকারীর অনুমোদিত ৩টা ফিক্স
--
-- ব্যাকগ্রাউন্ড: docs/proposed_migrations/PROPOSED_step40_dispute_split_dual_write_fix.sql
-- এ প্রস্তাবিত dual-write fix (balance_solver/balance_user, transactions.role,
-- job_status, clamped dispute_split_solver_percent) + সেই ফাইলের হেডারে "ইচ্ছাকৃতভাবে
-- বদলানো হয়নি (আপনার সিদ্ধান্ত লাগবে)" অংশে তালিকাভুক্ত ৩টা প্রশ্ন — ব্যবহারকারী
-- 2026-09-20 এ স্পষ্টভাবে সবক'টা অনুমোদন করেছেন। এই migration সেই অনুমোদিত ভার্সন।
--
-- ⚠️ স্কোপ নোট (গুরুত্বপূর্ণ): এই ফিক্স শুধু `resolve_dispute` (non-split) RPC-তে।
-- এই RPC অ্যাপের কোথাও থেকে কল হয় না — dead code, নিজেদের কমেন্টেই কনফার্ম করা আছে
-- (SupabaseSyncManager.kt লাইন ~1296, SomadhanViewModel.kt লাইন ~4710)। আসল/live dispute
-- resolve path `adminResolveDisputeLocked()` (Kotlin) → confirmReleaseAndComplete() /
-- refundEscrowOnce() / resolve_dispute_split() RPC দিয়ে চলে, যেখানে isDisputed=true
-- resolve-এর পরেও ইচ্ছাকৃতভাবে রাখা হয় (historical marker, disputeSettledAt দিয়ে
-- "active" বোঝা হয় — SomadhanRepository.kt লাইন ~3742 কমেন্ট দেখুন)। তাই
-- `resolve_dispute_split()` ফাংশনের is_disputed=true এখানে **ইচ্ছাকৃতভাবে ছোঁয়া হয়নি** —
-- সেটা ছোঁয়া মানেই লাইভ path-এ local(true)-vs-cloud(false) নতুন dual-write মিসম্যাচ
-- তৈরি করা, exactly এই CI প্রজেক্ট যা ধরার চেষ্টা করছে তার উল্টো। শুধু `resolve_dispute_split`-এ
-- cosmetic role='SOLVER' + %%% message ফিক্স প্রয়োগ করা হয়েছে (প্রস্তাবিত ফাইল থেকে অপরিবর্তিত)।
--
-- ============================================================================
-- resolve_dispute-এ কী বদলাল
-- ============================================================================
-- ১. (proposed fix, অপরিবর্তিত) SPLIT branch-এ solver payout balance_solver-এও,
--    owner refund balance_user-এও dual-write; transactions.role সেট ('SOLVER'/'USER');
--    job_status='JOB_COMPLETED'; dispute_split_solver_percent clamp করা।
-- ২. (ব্যবহারকারীর সিদ্ধান্ত ১) তিনটা branch-ই এখন problems.is_disputed = false সেট করে
--    resolve-এর সময় — dispute_resolved_at/dispute_settled_at-এর পাশাপাশি is_disputed
--    নিজেও বন্ধ হয়ে যায় (settle_dispute/withdraw_dispute-এর প্যাটার্নের সাথে মিলিয়ে)।
--    resolve করা dispute আর কখনো "open" থাকবে না।
-- ৩. (ব্যবহারকারীর সিদ্ধান্ত ২) SPLIT branch-এ solver-কে টাকা দেওয়ার আগে এখন
--    has_solver_role চেক হয় (release_escrow / resolve_dispute_split-এর প্যাটার্ন
--    অনুসরণ করে) — SOLVER_NOT_FOUND / SOLVER_ROLE_INACTIVE।
-- ৪. (ব্যবহারকারীর সিদ্ধান্ত ৩) RELEASE_TO_SOLVER / REFUND_TO_USER branch-এ HELD escrow
--    না পেলে এখন silently OK না করে exception (NO_ESCROW_TO_RELEASE /
--    NO_ESCROW_TO_REFUND) — SPLIT branch-এর NO_ESCROW_TO_SPLIT-এর সাথে সঙ্গতিপূর্ণ।
--
-- সব `CREATE OR REPLACE` — আগের GRANT অপরিবর্তিত থাকে, তাই আবার দেওয়া হয়নি।
-- ============================================================================

CREATE OR REPLACE FUNCTION public.resolve_dispute(p_problem_id text, p_resolution text, p_decision_note text, p_solver_percent numeric DEFAULT 50)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_escrow public.escrows%rowtype;
  v_now timestamptz := now();
  v_total numeric(12,2);
  v_solver_gross numeric(12,2);
  v_user_refund numeric(12,2);
  v_commission_rate numeric(6,2);
  v_commission numeric(12,2);
  v_solver_net numeric(12,2);
  v_ratio numeric;
  v_release_result jsonb;
  v_refund_result jsonb;
  v_solver_has_role boolean;
begin
  if not public.is_admin(auth.uid()) then raise exception 'NOT_AUTHORIZED'; end if;

  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;

  if v_problem.dispute_resolved_at is not null then
    raise exception 'ALREADY_RESOLVED: dispute for % was already resolved at %', p_problem_id, v_problem.dispute_resolved_at;
  end if;

  select * into v_escrow from public.escrows where problem_id = p_problem_id and status = 'HELD'
    order by created_at desc limit 1 for update;

  if p_resolution = 'RELEASE_TO_SOLVER' then
    if not found then
      raise exception 'NO_ESCROW_TO_RELEASE';
    end if;
    v_release_result := public.release_escrow(v_escrow.id);
    update public.problems set
      status = 'COMPLETED', job_status = 'JOB_COMPLETED',
      is_disputed = false,
      dispute_resolution_decision = 'RELEASE_TO_SOLVER', dispute_resolution_type = 'RELEASE_TO_SOLVER',
      dispute_resolution_note = p_decision_note, dispute_resolved_at = v_now, dispute_settled_at = v_now,
      dispute_result_seen_by_user = false, dispute_result_seen_by_solver = false, last_activity_at = v_now
    where id = p_problem_id;
    return jsonb_build_object('result', 'OK', 'resolution', 'RELEASE_TO_SOLVER', 'release', v_release_result);

  elsif p_resolution = 'REFUND_TO_USER' then
    if not found then
      raise exception 'NO_ESCROW_TO_REFUND';
    end if;
    v_refund_result := public.refund_escrow_once(v_escrow.id, 'DISPUTE_REFUND', 100);
    update public.problems set
      status = 'CANCELLED',
      is_disputed = false,
      dispute_resolution_decision = 'REFUND_TO_USER', dispute_resolution_type = 'REFUND_TO_USER',
      dispute_resolution_note = p_decision_note, dispute_resolved_at = v_now, dispute_settled_at = v_now,
      dispute_result_seen_by_user = false, dispute_result_seen_by_solver = false, last_activity_at = v_now
    where id = p_problem_id;
    return jsonb_build_object('result', 'OK', 'resolution', 'REFUND_TO_USER', 'refund', v_refund_result);

  elsif p_resolution in ('SPLIT_SETTLEMENT', 'SPLIT_50_50', 'CUSTOM_SPLIT', 'SETTLE') then
    if not found then
      raise exception 'NO_ESCROW_TO_SPLIT';
    end if;
    v_ratio := greatest(least(coalesce(p_solver_percent, 50), 100), 0) / 100.0;
    v_total := v_escrow.base_amount + v_escrow.extra_amount;
    v_solver_gross := round(v_total * v_ratio, 2);
    v_user_refund := v_total - v_solver_gross;

    v_commission_rate := public.resolve_commission_rate(v_escrow.solver_id);
    v_commission := round(v_solver_gross * (v_commission_rate / 100.0), 2);
    v_solver_net := v_solver_gross - v_commission;

    if v_solver_net > 0 and v_escrow.solver_id is not null then
      select has_solver_role into v_solver_has_role from public.users where id = v_escrow.solver_id for update;
      if v_solver_has_role is null then
        raise exception 'SOLVER_NOT_FOUND';
      end if;
      if not v_solver_has_role then
        raise exception 'SOLVER_ROLE_INACTIVE';
      end if;

      update public.users set balance = balance + v_solver_net, balance_solver = balance_solver + v_solver_net, updated_at = v_now where id = v_escrow.solver_id;
      insert into public.transactions (id, problem_id, problem_title, user_id, solver_id, gross_amount,
        commission_percent, commission_amount, net_amount, type, escrow_id, role, "timestamp")
      values ('TRX_SPLIT_SOLVER_' || v_escrow.id, v_problem.id, v_problem.title, v_problem.user_id, v_escrow.solver_id,
        v_solver_gross, v_commission_rate, v_commission, v_solver_net, 'DISPUTE_SPLIT', v_escrow.id, 'SOLVER', v_now)
      on conflict (id) do nothing;
    end if;

    if v_user_refund > 0 then
      update public.users set balance = balance + v_user_refund, balance_user = balance_user + v_user_refund, updated_at = v_now where id = v_escrow.user_id;
      insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, escrow_id, refund_type, role, "timestamp")
      values ('TRX_SPLIT_USER_' || v_escrow.id, v_problem.id, v_problem.title, v_escrow.user_id, v_user_refund, v_user_refund,
        'DISPUTE_SPLIT_REFUND', v_escrow.id, 'DISPUTE_SPLIT', 'USER', v_now)
      on conflict (id) do nothing;
    end if;

    update public.escrows set status = 'RELEASED', released_at = v_now, updated_at = v_now where id = v_escrow.id;

    update public.problems set
      status = 'COMPLETED', job_status = 'JOB_COMPLETED',
      is_disputed = false,
      dispute_resolution_decision = p_resolution, dispute_resolution_type = 'SPLIT_SETTLEMENT',
      dispute_resolution_note = p_decision_note,
      dispute_split_solver_percent = greatest(least(coalesce(p_solver_percent, 50), 100), 0),
      dispute_resolved_at = v_now, dispute_settled_at = v_now, completed_at = v_now,
      dispute_result_seen_by_user = false, dispute_result_seen_by_solver = false, last_activity_at = v_now
    where id = p_problem_id;

    return jsonb_build_object('result', 'OK', 'resolution', 'SPLIT_SETTLEMENT', 'solver_net', v_solver_net, 'user_refund', v_user_refund);
  else
    raise exception 'UNKNOWN_RESOLUTION: %', p_resolution;
  end if;
end;
$function$;

-- ---------------------------------------------------------------------------
-- resolve_dispute_split — শুধু cosmetic ফিক্স (role='SOLVER' payout txn-এ, আর
-- PERCENT_MISMATCH message-এর %%% → 'percent')। is_disputed=true ইচ্ছাকৃতভাবে
-- অপরিবর্তিত — এটা live path, উপরের স্কোপ নোট দেখুন।
-- ---------------------------------------------------------------------------
create or replace function public.resolve_dispute_split(
  p_problem_id text,
  p_escrow_id text,
  p_split_solver_percent numeric,
  p_solver_gross_amount numeric,
  p_commission_amount numeric,
  p_solver_net_amount numeric,
  p_user_refund_amount numeric,
  p_resolution_decision text,
  p_decision_note text,
  p_progress_at_settlement integer default null
)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_problem public.problems%rowtype;
  v_escrow public.escrows%rowtype;
  v_now timestamptz := now();
  v_trx_id text := 'TRX_SPLIT_' || p_problem_id;
  v_clamped_percent numeric;
  v_total numeric(12,2);
  v_expected_gross numeric(12,2);
  v_solver_has_role boolean;
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  if v_problem.dispute_resolved_at is not null then
    return jsonb_build_object('result', 'ALREADY_RESOLVED', 'resolved_at', v_problem.dispute_resolved_at);
  end if;

  select * into v_escrow from public.escrows where id = p_escrow_id for update;
  if not found then
    raise exception 'ESCROW_NOT_FOUND';
  end if;
  if v_escrow.problem_id <> p_problem_id then
    raise exception 'ESCROW_PROBLEM_MISMATCH';
  end if;

  if p_solver_gross_amount < 0 or p_commission_amount < 0 or p_solver_net_amount < 0 or p_user_refund_amount < 0 then
    raise exception 'INVALID_AMOUNT';
  end if;

  v_clamped_percent := greatest(least(coalesce(p_split_solver_percent, 50), 100), 0);
  v_total := v_escrow.base_amount + v_escrow.extra_amount;

  if p_solver_gross_amount > v_total + 1 then
    raise exception 'AMOUNT_EXCEEDS_ESCROW: gross % > escrow total %', p_solver_gross_amount, v_total;
  end if;
  if p_solver_net_amount > p_solver_gross_amount + 1 then
    raise exception 'NET_EXCEEDS_GROSS';
  end if;

  v_expected_gross := round(v_total * (v_clamped_percent / 100.0), 2);
  if abs(p_solver_gross_amount - v_expected_gross) > 2 then
    raise exception 'PERCENT_MISMATCH: gross % vs expected % for % percent', p_solver_gross_amount, v_expected_gross, v_clamped_percent;
  end if;

  if exists (select 1 from public.transactions where id = v_trx_id) then
    return jsonb_build_object('result', 'ALREADY_PAID');
  end if;

  if p_solver_net_amount > 0 and v_escrow.solver_id is not null then
    select has_solver_role into v_solver_has_role from public.users where id = v_escrow.solver_id for update;
    if v_solver_has_role is null then
      raise exception 'SOLVER_NOT_FOUND';
    end if;
    if not v_solver_has_role then
      raise exception 'SOLVER_ROLE_INACTIVE';
    end if;

    update public.users
    set balance = balance + p_solver_net_amount,
        balance_solver = balance_solver + p_solver_net_amount,
        updated_at = v_now
    where id = v_escrow.solver_id;

    insert into public.transactions (id, problem_id, problem_title, user_id, solver_id, gross_amount,
      commission_percent, commission_amount, net_amount, type, escrow_id, release_type, role, "timestamp")
    values (v_trx_id, v_problem.id, v_problem.title, v_problem.user_id, v_escrow.solver_id,
      p_solver_gross_amount,
      case when p_solver_gross_amount > 0 then round((p_commission_amount / p_solver_gross_amount) * 100.0, 2) else 0 end,
      p_commission_amount, p_solver_net_amount, 'PAYMENT', p_escrow_id, 'SPLIT_RELEASE', 'SOLVER', v_now)
    on conflict (id) do nothing;
  end if;

  if p_user_refund_amount <= 0 then
    update public.escrows set status = 'RELEASED', released_at = v_now, updated_at = v_now
    where id = p_escrow_id;
  end if;

  update public.problems set
    status = 'COMPLETED',
    job_status = 'JOB_COMPLETED',
    completed_at = v_now,
    is_disputed = true,
    dispute_resolution_decision = p_resolution_decision,
    dispute_resolution_type = 'SPLIT_SETTLEMENT',
    dispute_resolution_note = p_decision_note,
    dispute_resolved_at = v_now,
    dispute_settled_at = v_now,
    dispute_result_seen_by_user = false,
    dispute_result_seen_by_solver = false,
    dispute_split_solver_percent = v_clamped_percent,
    dispute_progress_at_settlement = coalesce(p_progress_at_settlement, dispute_progress_at_settlement),
    last_activity_at = v_now
  where id = p_problem_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    'বিরোধ মীমাংসা সম্পন্ন ⚖️',
    '"' || v_problem.title || '" বিরোধে অ্যাডমিন মীমাংসা করেছেন। ৳' || p_user_refund_amount::text || ' আপনার ওয়ালেটে ফেরত দেওয়া হয়েছে।',
    'balance', v_problem.user_id, p_problem_id, v_now);

  if v_escrow.solver_id is not null then
    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_escrow.solver_id,
      'বিরোধ মীমাংসা সম্পন্ন ⚖️',
      '"' || v_problem.title || '" বিরোধে অ্যাডমিন মীমাংসা করেছেন। ৳' || p_solver_net_amount::text || ' আপনার ব্যালেন্সে জমা হয়েছে।',
      'balance', v_escrow.solver_id, p_problem_id, v_now);
  end if;

  return jsonb_build_object(
    'result', 'OK',
    'solver_net_amount', p_solver_net_amount,
    'user_refund_amount', p_user_refund_amount,
    'escrow_closed_here', (p_user_refund_amount <= 0)
  );
end;
$$;
