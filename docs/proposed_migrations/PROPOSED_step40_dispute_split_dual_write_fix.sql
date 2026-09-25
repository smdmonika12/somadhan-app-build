-- ⚠️ PROPOSED — এটা এখনো apply করা হয় না। supabase/migrations/-এ নেই ইচ্ছাকৃতভাবে
-- (CI সব migration চালায়, আর master prompt rule #1 অনুযায়ী migration আমি নিজে থেকে বসাই না;
-- এটা টাকার লজিক, মানুষের review + staging-এ পরীক্ষা লাগবে)।
--
-- বসাতে চাইলে: এই ফাইলটা supabase/migrations/step40_dispute_split_dual_write_fix.sql নামে কপি করুন
-- (নাম এমন হতে হবে যাতে sort-এ step38b_… এবং step36_…-এর পরে আসে), তারপর নিচের
-- "টেস্টে কী বদলাতে হবে" অংশ অনুযায়ী supabase/tests/08_disputes_part2.sql আপডেট করুন।
--
-- ============================================================================
-- কী ঠিক করে (সব `CREATE OR REPLACE` — আগের GRANT/REVOKE অপরিবর্তিত থাকে, তাই আবার দেওয়া হয়নি)
-- ============================================================================
-- ১. resolve_dispute-এর SPLIT branch (SPLIT_SETTLEMENT/SPLIT_50_50/CUSTOM_SPLIT/SETTLE):
--    • solver-এর payout এখন legacy `balance`-এর পাশাপাশি `balance_solver`-এও যায়, আর owner-এর
--      refund `balance_user`-এও — বাকি সব payout ফাংশন (release_escrow, refund_escrow_once,
--      resolve_dispute_split, request_wallet_deposit …) step36-এ যে dual-write প্যাটার্ন পেয়েছে
--      এটা সেই একটা বাদ পড়া ফাংশন। এটা ছাড়া role-scoped balance পড়া UI-তে এই path-এর টাকা
--      "হারিয়ে গেছে" মনে হয় (Step 4-এর deposit_money_via_gateway-এর মতো একই ধরনের গ্যাপ)।
--    • transactions.role এখন সেট হয় (solver-এর → 'SOLVER', owner-refund-এর → 'USER') — আগে
--      column default '' থেকে যেত, ফলে role-scoped transaction history-তে এই row দেখা যেত না।
--    • job_status = 'JOB_COMPLETED' সেট হয় (RELEASE_TO_SOLVER branch আর resolve_dispute_split
--      দুটোই করে, শুধু এই branch করত না)।
--    • dispute_split_solver_percent-এ এখন clamped মান (০–১০০) বসে, আগে কাঁচা মান (১৫০, -২০)
--      বসত। step32_6_admin_repair_missing_refunds `100 - percent` হিসাব করে — clamped মানে সেটা
--      আরও নিরাপদ, ভাঙে না।
-- ২. resolve_dispute_split: payout transaction-এ role='SOLVER' (আগে '' ছিল)।
-- ৩. resolve_dispute_split: PERCENT_MISMATCH message-এর format-string `%%%` (= literal % + placeholder
--    → "for %60") ঠিক করে "for 60 percent" — শুধু cosmetic।
--
-- ============================================================================
-- ইচ্ছাকৃতভাবে বদলানো হয়নি (আপনার সিদ্ধান্ত লাগবে — ইচ্ছাকৃত আচরণও হতে পারে)
-- ============================================================================
-- • resolve_dispute কোথাও is_disputed ছোঁয় না (resolve-এর পরেও true থাকে)। Kotlin/UI যদি
--   dispute_resolved_at দিয়ে "resolved" চেনে তাহলে ঠিক আছে; যদি is_disputed=false আশা করে, তাহলে
--   প্রতিটা branch-এর problems UPDATE-এ `is_disputed = false` যোগ করতে হবে।
-- • resolve_dispute-এর SPLIT branch solver-এর has_solver_role চেক করে না (release_escrow আর
--   resolve_dispute_split করে)। যোগ করলে নিষ্ক্রিয়-role solver থাকা dispute resolve হতে আটকে যেতে
--   পারে — ব্যবসায়িক সিদ্ধান্ত।
-- • HELD escrow না থাকলেও RELEASE_TO_SOLVER/REFUND_TO_USER problem-কে resolved করে দেয় (টাকা
--   না নড়ে)। ইচ্ছাকৃত হতে পারে (escrow ছাড়া বিরোধ বন্ধ করা)।
--
-- ============================================================================
-- টেস্টে কী বদলাতে হবে (supabase/tests/08_disputes_part2.sql — এই fix বসালে "DOCUMENTED CURRENT
-- BEHAVIOUR" assertion-গুলো ইচ্ছাকৃতভাবেই ফেল করবে; নতুন প্রত্যাশা):
-- ============================================================================
--   SP1  solver B (balance, balance_solver): (225, 0)  → (225, 225)
--   SP1  owner  B (balance, balance_user)  : (1250, 1000) → (1250, 1250)
--   SP1  problem row job_status              : NULL → 'JOB_COMPLETED'
--   SP1/SP2/SP3/SP4 transactions.role        : নতুন assertion — solver txn 'SOLVER', refund txn 'USER'
--   SP3  dispute_split_solver_percent        : 150 → 100
--   SP4  dispute_split_solver_percent        : -20 → 0
--   SP3  solver D balance_solver             : নতুন assertion 180;  SP4: owner E balance_user 1300
--   RDS-A/B/C payout txn role                : নতুন assertion 'SOLVER'
-- সব সংখ্যা ১০% commission stub (resolve_commission_rate) ধরে।

-- ---------------------------------------------------------------------------
-- resolve_dispute (শুধু SPLIT branch বদলেছে; RELEASE_TO_SOLVER/REFUND_TO_USER আগের মতোই)
-- ---------------------------------------------------------------------------
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
    if found then
      v_release_result := public.release_escrow(v_escrow.id);
    end if;
    update public.problems set
      status = 'COMPLETED', job_status = 'JOB_COMPLETED',
      dispute_resolution_decision = 'RELEASE_TO_SOLVER', dispute_resolution_type = 'RELEASE_TO_SOLVER',
      dispute_resolution_note = p_decision_note, dispute_resolved_at = v_now, dispute_settled_at = v_now,
      dispute_result_seen_by_user = false, dispute_result_seen_by_solver = false, last_activity_at = v_now
    where id = p_problem_id;
    return jsonb_build_object('result', 'OK', 'resolution', 'RELEASE_TO_SOLVER', 'release', v_release_result);

  elsif p_resolution = 'REFUND_TO_USER' then
    if found then
      v_refund_result := public.refund_escrow_once(v_escrow.id, 'DISPUTE_REFUND', 100);
    end if;
    update public.problems set
      status = 'CANCELLED',
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

    if v_solver_net > 0 then
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
-- resolve_dispute_split (role='SOLVER' + cosmetic message ছাড়া আর কিছু বদলায়নি)
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

  -- Idempotent no-op on retry/race — Kotlin already ran its own local guard before ever calling
  -- this RPC, so a non-null dispute_resolved_at here just means an earlier dual-write attempt for
  -- this same resolution already succeeded (or another admin session got there first).
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

  -- ৳২ tolerance — Kotlin Math.round() আর Postgres round() মাঝেমধ্যে ১ টাকা এদিক-ওদিক হতে পারে
  -- (আলাদা base/extra rounding ধাপের কারণে); সম্পূর্ণ ভিন্ন percent/amount পাঠানো হলে ধরার জন্য।
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

  -- সলভার ১০০% পেলে (userRefund=0) Kotlin-সাইডে refundEscrowOnce() কখনো কল হয় না, তাই সেই
  -- ক্ষেত্রেই কেবল এই RPC নিজে escrow বন্ধ করে দেয়। userRefund>0 হলে refund_escrow_once() ইতিমধ্যে
  -- এই escrow-কে REFUNDED করে দিয়েছে ধরে নেওয়া হয় — এখানে আবার ছোঁয়া হয় না।
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
