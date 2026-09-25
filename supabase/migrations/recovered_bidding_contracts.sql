-- RECOVERED from live DB on 2026-09-18, verbatim capture, no logic changes. See
-- RPC_SYNC_FIX_PROGRESS.md Step 2.
-- Functions in this file (Step 1 missing-RPC group): accept_bid, cancel_bid, reject_bid, accept_direct_contract, decline_direct_contract

-- signature: accept_bid(text,text,text,text,numeric)
CREATE OR REPLACE FUNCTION public.accept_bid(p_problem_id text, p_bid_id text, p_gateway_trx_id text DEFAULT NULL::text, p_gateway text DEFAULT NULL::text, p_gateway_amount numeric DEFAULT NULL::numeric)
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

  v_escrow_id := 'ESC_' || replace(gen_random_uuid()::text, '-', '');
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
$function$
;
GRANT EXECUTE ON FUNCTION accept_bid(text,text,text,text,numeric) TO "-";
GRANT EXECUTE ON FUNCTION accept_bid(text,text,text,text,numeric) TO anon;
GRANT EXECUTE ON FUNCTION accept_bid(text,text,text,text,numeric) TO authenticated;
GRANT EXECUTE ON FUNCTION accept_bid(text,text,text,text,numeric) TO postgres;
GRANT EXECUTE ON FUNCTION accept_bid(text,text,text,text,numeric) TO service_role;

-- signature: cancel_bid(text)
CREATE OR REPLACE FUNCTION public.cancel_bid(p_bid_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_bid public.bids%rowtype;
begin
  select * into v_bid from public.bids where id = p_bid_id for update;
  if not found then raise exception 'BID_NOT_FOUND'; end if;
  if auth.uid() <> v_bid.solver_id then raise exception 'NOT_AUTHORIZED'; end if;
  if v_bid.status <> 'PENDING' then
    return jsonb_build_object('result', 'INVALID_TRANSITION', 'status', v_bid.status);
  end if;
  update public.bids set status = 'CANCELLED', resolved_at = now() where id = p_bid_id;
  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION cancel_bid(text) TO anon;
GRANT EXECUTE ON FUNCTION cancel_bid(text) TO authenticated;
GRANT EXECUTE ON FUNCTION cancel_bid(text) TO postgres;
GRANT EXECUTE ON FUNCTION cancel_bid(text) TO service_role;

-- signature: reject_bid(text)
CREATE OR REPLACE FUNCTION public.reject_bid(p_bid_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_bid public.bids%rowtype;
  v_problem public.problems%rowtype;
begin
  select * into v_bid from public.bids where id = p_bid_id for update;
  if not found then raise exception 'BID_NOT_FOUND'; end if;
  select * into v_problem from public.problems where id = v_bid.problem_id;
  if auth.uid() <> v_problem.user_id and not public.is_admin(auth.uid()) then raise exception 'NOT_AUTHORIZED'; end if;
  if v_bid.status <> 'PENDING' then
    return jsonb_build_object('result', 'INVALID_TRANSITION', 'status', v_bid.status);
  end if;
  update public.bids set status = 'REJECTED', resolved_at = now() where id = p_bid_id;
  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION reject_bid(text) TO anon;
GRANT EXECUTE ON FUNCTION reject_bid(text) TO authenticated;
GRANT EXECUTE ON FUNCTION reject_bid(text) TO postgres;
GRANT EXECUTE ON FUNCTION reject_bid(text) TO service_role;

-- signature: accept_direct_contract(text)
CREATE OR REPLACE FUNCTION public.accept_direct_contract(p_problem_id text)
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

  v_escrow_id := 'ESC_' || replace(gen_random_uuid()::text, '-', '');
  insert into public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, extra_amount, status, created_at, updated_at)
  values (v_escrow_id, v_problem.id, v_problem.title, v_problem.user_id, v_problem.accepted_solver_id, coalesce(v_problem.accepted_amount, 0), 0, 'HELD', v_now, v_now);

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    'সরাসরি কাজের প্রস্তাব গৃহীত হয়েছে! 🎉',
    coalesce(v_problem.accepted_solver_name, 'সমাধানকারী') || ' আপনার সরাসরি কাজের প্রস্তাব (\"' || v_problem.title || '\") গ্রহণ করেছেন। কাজ শুরু হয়েছে।',
    'problem', p_problem_id, p_problem_id, 'USER', v_now);

  return jsonb_build_object('result', 'OK', 'escrow_id', v_escrow_id, 'commission_rate', v_rate);
end;
$function$
;
GRANT EXECUTE ON FUNCTION accept_direct_contract(text) TO "-";
GRANT EXECUTE ON FUNCTION accept_direct_contract(text) TO anon;
GRANT EXECUTE ON FUNCTION accept_direct_contract(text) TO authenticated;
GRANT EXECUTE ON FUNCTION accept_direct_contract(text) TO postgres;
GRANT EXECUTE ON FUNCTION accept_direct_contract(text) TO service_role;

-- signature: decline_direct_contract(text,text)
CREATE OR REPLACE FUNCTION public.decline_direct_contract(p_problem_id text, p_reason text DEFAULT ''::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
  v_reason_text text;
begin
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

  update public.problems set
    direct_contract_status = 'DECLINED',
    status = 'CANCELLED',
    last_activity_at = v_now
  where id = p_problem_id;

  v_reason_text := case when trim(coalesce(p_reason, '')) <> '' then ' (কারণ: ' || trim(p_reason) || ')' else '' end;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    'সরাসরি কাজের প্রস্তাব প্রত্যাখ্যাত ❌',
    coalesce(v_problem.accepted_solver_name, 'সমাধানকারী') || ' আপনার সরাসরি কাজের প্রস্তাব (\"' || v_problem.title || '\") প্রত্যাখ্যান করেছেন' || v_reason_text || '।',
    'problem', p_problem_id, p_problem_id, 'USER', v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION decline_direct_contract(text,text) TO "-";
GRANT EXECUTE ON FUNCTION decline_direct_contract(text,text) TO anon;
GRANT EXECUTE ON FUNCTION decline_direct_contract(text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION decline_direct_contract(text,text) TO postgres;
GRANT EXECUTE ON FUNCTION decline_direct_contract(text,text) TO service_role;

