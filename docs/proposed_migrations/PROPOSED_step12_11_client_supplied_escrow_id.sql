-- ============================================================================
-- Step 12.11 — escrow id mismatch fix: cloud RPC-কে client-এর local escrow id গ্রহণ করতে দেওয়া
-- ============================================================================
-- সমস্যা: client local-এ escrow বানায় id = 'ESCROW_<৮>', কিন্তু accept_bid / accept_direct_contract
-- cloud-এ বানায় id = 'ESC_<uuid>'। release_escrow / refund_escrow_once / resolve_dispute_split /
-- increment_escrow_extra_amount সবাই `where id = p_escrow_id` দিয়ে খোঁজে (কোনো fallback নেই) →
-- client local id পাঠালে ESCROW_NOT_FOUND; realtime-এ আসা cloud row local-এ দ্বিতীয় row হয়ে বসে।
--
-- সমাধান (business model অপরিবর্তিত): দুই RPC-তে নতুন ঐচ্ছিক প্যারামিটার `p_escrow_id text default null`।
--   * দিলে ও ফরম্যাট ঠিক থাকলে (^ESCROW_[0-9A-Za-z-]{8,36}$) ও ওই id আগে থেকে না থাকলে → সেটাই escrow id।
--   * না দিলে (পুরনো client / outbox-এ আগে জমা পুরনো entry) বা id আগে থেকেই থাকলে (collision) → আগের মতো 'ESC_<uuid>' (কখনো fail করে না)।
--   * ভুল ফরম্যাট → 'INVALID_ESCROW_ID' (কিছু লেখার আগেই, তাই rollback-সেফ)।
-- idempotency অপরিবর্তিত: accept_bid-এ `status <> 'OPEN'` → ALREADY_ACCEPTED, accept_direct_contract-এ
-- `direct_contract_status <> 'PENDING_ACCEPTANCE'` → ALREADY_PROCESSED — দুটোই escrow insert-এর আগে, তাই replay-এ দ্বিতীয় escrow খোলে না।
--
-- ⚠️ পুরনো সিগনেচার DROP করা হয় একই migration-এ (নাহলে দুটো overload থেকে যেত — Step 12.9-এর শিক্ষা)।
-- ⚠️ live বডির ওপর ভিত্তি করে (accept_bid md5 ddc5f1b097b69c1afbb838390f7b5913, accept_direct_contract md5
--    4e4fced210bfb9c91fbd636c95972b6e, ২০২৬-০৯-২১) — বাকি বডি বাইট-বাই-বাইট অপরিবর্তিত।
-- ⚠️ supabase/migrations/-এ কপি করা Step 12.12 (migration-ফাইল-সিঙ্ক) পর্যন্ত বাকি।

drop function if exists public.accept_bid(text, text, text, text, numeric);
drop function if exists public.accept_direct_contract(text);

CREATE OR REPLACE FUNCTION public.accept_bid(p_problem_id text, p_bid_id text, p_gateway_trx_id text DEFAULT NULL::text, p_gateway text DEFAULT NULL::text, p_gateway_amount numeric DEFAULT NULL::numeric, p_escrow_id text DEFAULT NULL::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_bid public.bids%rowtype;
  v_user public.users%rowtype;
  v_commission_rate numeric(6,2);
  v_escrow_id text;
  v_now timestamptz := now();
  v_wallet_deduction numeric(12,2);
  v_shortfall numeric(12,2);
begin
  -- [Step 12.11] client-এর দেওয়া escrow id-র ফরম্যাট যাচাই (কিছু লেখার আগেই)
  if p_escrow_id is not null and p_escrow_id <> '' and p_escrow_id !~ '^ESCROW_[0-9A-Za-z-]{8,36}$' then
    raise exception 'INVALID_ESCROW_ID';
  end if;

  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  if auth.uid() <> v_problem.user_id and not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  if v_problem.status <> 'OPEN' then
    return jsonb_build_object('result', 'ALREADY_ACCEPTED');
  end if;

  select * into v_bid from public.bids where id = p_bid_id and problem_id = p_problem_id for update;
  if not found then
    raise exception 'BID_NOT_FOUND';
  end if;

  select * into v_user from public.users where id = v_problem.user_id for update;

  -- [ধাপ ১: money-flow fix] wallet কভারেজ role-scoped balance_user থেকে হিসাব হবে, legacy shared balance থেকে না
  -- (shared balance role-switch-এর সময় stale/ambiguous হতে পারে; owner সবসময় USER role-এর মানুষ)
  v_wallet_deduction := least(greatest(v_user.balance_user, 0), v_bid.amount);
  v_shortfall := v_bid.amount - v_wallet_deduction;

  if v_shortfall > 0 then
    if p_gateway_trx_id is null or p_gateway_amount is null or p_gateway_amount < v_shortfall then
      return jsonb_build_object(
        'result', 'INSUFFICIENT_BALANCE',
        'required', v_bid.amount,
        'available', v_user.balance_user,
        'shortfall', v_shortfall
      );
    end if;

    insert into public.gateway_payments (
      id, gateway_trx_id, user_id, user_name, user_phone, amount, gateway, purpose,
      problem_id, problem_title, status, note, "timestamp"
    ) values (
      'GW_BID_' || v_bid.id, p_gateway_trx_id, v_user.id, v_user.name, v_user.phone,
      p_gateway_amount, coalesce(p_gateway, 'DEMO'), 'ESCROW_PAYMENT',
      v_problem.id, v_problem.title, 'SUCCESS', 'বিড accept করার সময় গেটওয়ে দিয়ে শর্টফল পরিশোধ', v_now
    )
    on conflict (id) do nothing;
  end if;

  v_commission_rate := public.resolve_commission_rate(v_bid.solver_id);

  if v_wallet_deduction > 0 then
    -- [ধাপ ১৪.৫ঘ] role-activation check: নিষ্ক্রিয় USER role থেকে বিড-accept ওয়ালেট-ডিডাকশন বন্ধ।
    if not coalesce(v_user.has_user_role, false) then
      raise exception 'USER_ROLE_INACTIVE';
    end if;

    -- [ধাপ ১৪.৫ঘ] dual-write: পুরনো shared balance (অপরিবর্তিত আচরণ, rule #2) + balance_user।
    update public.users set balance = balance - v_wallet_deduction, balance_user = balance_user - v_wallet_deduction, updated_at = v_now where id = v_user.id;
    -- [ধাপ ৩৬] transactions.role যোগ — এই deduction সবসময় USER-role (পোস্টার), TRANSACTION_ROLE_FIELD_DESIGN.md সাইট #২
    insert into public.transactions (id, problem_id, problem_title, user_id, solver_id, gross_amount,
      commission_percent, commission_amount, net_amount, type, role, "timestamp")
    values ('TRX_BID_DEDUCT_' || v_bid.id, v_problem.id, v_problem.title, v_user.id, null,
      v_wallet_deduction, 0, 0, -v_wallet_deduction, 'BID_ACCEPT_DEDUCTION', 'USER', v_now)
    on conflict (id) do nothing;
  end if;

  update public.bids set status = 'ACCEPTED', resolved_at = v_now where id = v_bid.id;
  update public.bids set status = 'PENDING' where problem_id = p_problem_id and id <> v_bid.id and status <> 'CANCELLED';

  update public.problems set
    status = 'IN_PROGRESS',
    job_status = case when is_instant_job then 'ACCEPTED' else job_status end,
    accepted_bid_id = v_bid.id,
    accepted_solver_id = v_bid.solver_id,
    accepted_solver_name = v_bid.solver_name,
    accepted_amount = v_bid.amount,
    applied_commission_rate = v_commission_rate,
    solver_cancelled_notice = null,
    on_way_at = null, arrived_at = null, job_started_at = null, completed_at = null,
    solver_live_lat = null, solver_live_lng = null, solver_live_updated_at = null,
    has_release_request = false, release_request_extra_amount = 0, release_request_note = '',
    release_requested_at = null,
    pending_extra_amount = null, pending_extra_amount_note = null, pending_extra_amount_requested_at = null,
    confirmed_extra_amount_total = 0,
    is_disputed = false, dispute_reason = null, dispute_initiator_id = null, dispute_settled_at = null,
    dispute_resolution_decision = null, dispute_resolution_type = null, dispute_resolution_note = null,
    dispute_resolved_at = null, dispute_progress_at_raise = null, dispute_progress_at_settlement = null,
    last_activity_at = v_now
  where id = p_problem_id;

  -- [Step 12.11] client-এর দেওয়া id (ফরম্যাট ঠিক + আগে থেকে নেই হলে) গ্রহণ; নাহলে আগের মতো 'ESC_<uuid>'
  if p_escrow_id is not null and p_escrow_id <> '' and not exists (select 1 from public.escrows where id = p_escrow_id) then
    v_escrow_id := p_escrow_id;
  else
    v_escrow_id := 'ESC_' || replace(gen_random_uuid()::text, '-', '');
  end if;
  insert into public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, extra_amount, status, created_at, updated_at)
  values (v_escrow_id, v_problem.id, v_problem.title, v_problem.user_id, v_bid.solver_id, v_bid.amount, 0, 'HELD', v_now, v_now);

  return jsonb_build_object(
    'result', 'OK',
    'escrow_id', v_escrow_id,
    'wallet_deduction', v_wallet_deduction,
    'gateway_amount', case when v_shortfall > 0 then p_gateway_amount else 0 end,
    'commission_rate', v_commission_rate
  );
end;
$function$;

CREATE OR REPLACE FUNCTION public.accept_direct_contract(p_problem_id text, p_escrow_id text DEFAULT NULL::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_rate numeric(6,2);
  v_escrow_id text;
  v_now timestamptz := now();
begin
  -- [Step 12.11] client-এর দেওয়া escrow id-র ফরম্যাট যাচাই (কিছু লেখার আগেই)
  if p_escrow_id is not null and p_escrow_id <> '' and p_escrow_id !~ '^ESCROW_[0-9A-Za-z-]{8,36}$' then
    raise exception 'INVALID_ESCROW_ID';
  end if;

  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  if auth.uid() <> v_problem.accepted_solver_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  if not v_problem.is_direct_contract then
    raise exception 'NOT_A_DIRECT_CONTRACT';
  end if;

  if v_problem.direct_contract_status <> 'PENDING_ACCEPTANCE' then
    return jsonb_build_object('result', 'ALREADY_PROCESSED', 'status', v_problem.direct_contract_status);
  end if;

  v_rate := public.resolve_commission_rate(v_problem.accepted_solver_id);

  update public.problems set
    direct_contract_status = 'ACCEPTED',
    status = 'IN_PROGRESS',
    applied_commission_rate = v_rate,
    last_activity_at = v_now
  where id = p_problem_id;

  -- [Step 12.11] client-এর দেওয়া id (ফরম্যাট ঠিক + আগে থেকে নেই হলে) গ্রহণ; নাহলে আগের মতো 'ESC_<uuid>'
  if p_escrow_id is not null and p_escrow_id <> '' and not exists (select 1 from public.escrows where id = p_escrow_id) then
    v_escrow_id := p_escrow_id;
  else
    v_escrow_id := 'ESC_' || replace(gen_random_uuid()::text, '-', '');
  end if;
  insert into public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, extra_amount, status, created_at, updated_at)
  values (v_escrow_id, v_problem.id, v_problem.title, v_problem.user_id, v_problem.accepted_solver_id, coalesce(v_problem.accepted_amount, 0), 0, 'HELD', v_now, v_now);

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    'সরাসরি কাজের প্রস্তাব গৃহীত হয়েছে! 🎉',
    coalesce(v_problem.accepted_solver_name, 'সমাধানকারী') || ' আপনার সরাসরি কাজের প্রস্তাব (\"' || v_problem.title || '\") গ্রহণ করেছেন। কাজ শুরু হয়েছে।',
    'problem', p_problem_id, p_problem_id, 'USER', v_now);

  return jsonb_build_object('result', 'OK', 'escrow_id', v_escrow_id, 'commission_rate', v_rate);
end;
$function$;

grant execute on function public.accept_bid(text, text, text, text, numeric, text) to anon, authenticated, service_role;
grant execute on function public.accept_direct_contract(text, text) to anon, authenticated, service_role;
