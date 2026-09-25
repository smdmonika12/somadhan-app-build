-- ধাপ ২৯.৫ — Dispute Split-Settlement Payout RPC (সলভারের ভাগ এখনো Supabase-এ যাচ্ছিল না)
--
-- প্রেক্ষাপট: adminResolveDisputeLocked() (Kotlin) এর SPLIT_SETTLEMENT/CUSTOM_SPLIT/SETTLE শাখায়
-- মালিকের (owner) রিফান্ড অংশ ইতিমধ্যে migrate করা refund_escrow_once() পথ দিয়ে dual-write হয় (ধাপ
-- ৯/১২), কিন্তু সলভারের ভাগ (userDao.addBalance) সম্পূর্ণ local/Firebase-only ছিল। এই migration সেই
-- gap বন্ধ করে — কিন্তু ইচ্ছাকৃতভাবে escrow/owner-refund আবার ছোঁয় না (নিচে নোট দেখুন)।
--
-- ডিজাইন সিদ্ধান্ত (রিপোর্টে বিস্তারিত):
-- ১. এই RPC শুধু সলভার-পেআউট + problem-row আপডেট + দুই পক্ষের notification করে। owner-refund এই RPC
--    করে না — সেটা ইতিমধ্যেই refund_escrow_once() দিয়ে আলাদাভাবে dual-write হয়, এখানেও করলে
--    ডাবল-রিফান্ড হয়ে যেত।
-- ২. কমিশন হিসাব Kotlin থেকেই (calculateCommissionBreakdown() থেকে) pre-calculated এসে RPC শুধু
--    bounds re-verify করে (percent-অনুযায়ী প্রত্যাশিত gross-এর কাছাকাছি কিনা, net<=gross<=escrow-total
--    ইত্যাদি) — SQL-এ পুরো commission-logic (promo/free-quota/extra-discount) পোর্ট না করে, cloud
--    balance যেন LOCAL-এ যা ক্রেডিট হয়েছে তার সাথে হুবহু মিলে সেটাই নিশ্চিত করা হচ্ছে। এটা
--    release_escrow()-এর মতো RPC নিজে থেকে re-derive করার চেয়ে বেশি নিরাপদ, কারণ এখানে
--    promo/free-quota-aware দুই আলাদা কমিশন-ফর্মুলা (Kotlin-এ base+extra আলাদাভাবে) থাকার
--    ঝুঁকি নেই।
-- ৩. escrow.status conditional-ভাবে ছোঁয়া হয় — p_user_refund_amount <= 0 (সলভার ১০০% পেলে)
--    ক্ষেত্রেই কেবল এই RPC নিজে escrow status = RELEASED করে (কারণ সেক্ষেত্রে refundEscrowOnce()
--    Kotlin-সাইডে কখনো কলই হয় না, escrow status cloud-এ HELD-ই থেকে যেত)। p_user_refund_amount > 0
--    হলে escrow status ছোঁয়া হয় না (refund_escrow_once() ইতিমধ্যে REFUNDED করে দিয়েছে ধরে নেওয়া হয়)।
-- ৪. Idempotency: problems.dispute_resolved_at ইতিমধ্যে সেট থাকলে exception না ছুঁড়ে নিরাপদে
--    ALREADY_RESOLVED রিটার্ন করে (no-op) — Kotlin-এর নিজস্ব local guard-এর cloud-সমতুল্য, যাতে
--    dual-write retry/race-এ ডাবল-পেমেন্ট না হয়। আলাদাভাবে TRX_SPLIT_<problem_id> deterministic-id
--    transaction-চেকও রাখা হয়েছে (Kotlin-এর splitTrxId প্যাটার্নের সাথে হুবহু মিলিয়ে) — defense in
--    depth।

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
    raise exception 'PERCENT_MISMATCH: gross % vs expected % for %%%', p_solver_gross_amount, v_expected_gross, v_clamped_percent;
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
      commission_percent, commission_amount, net_amount, type, escrow_id, release_type, "timestamp")
    values (v_trx_id, v_problem.id, v_problem.title, v_problem.user_id, v_escrow.solver_id,
      p_solver_gross_amount,
      case when p_solver_gross_amount > 0 then round((p_commission_amount / p_solver_gross_amount) * 100.0, 2) else 0 end,
      p_commission_amount, p_solver_net_amount, 'PAYMENT', p_escrow_id, 'SPLIT_RELEASE', v_now)
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

revoke all on function public.resolve_dispute_split(text, text, numeric, numeric, numeric, numeric, numeric, text, text, integer) from public;
grant execute on function public.resolve_dispute_split(text, text, numeric, numeric, numeric, numeric, numeric, text, text, integer) to authenticated;
