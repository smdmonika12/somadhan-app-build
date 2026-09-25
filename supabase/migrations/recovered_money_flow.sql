-- RECOVERED from live DB on 2026-09-18, verbatim capture, no logic changes. See
-- RPC_SYNC_FIX_PROGRESS.md Step 2.
-- Functions in this file (Step 1 missing-RPC group): deposit_money_via_gateway, process_withdrawal, refund_escrow_once, release_escrow, request_extra_amount, request_wallet_deposit, request_withdrawal, respond_additional_charge, user_confirm_extra_amount, user_reject_extra_amount

-- signature: deposit_money_via_gateway(uuid,numeric,text,text,text,text)
CREATE OR REPLACE FUNCTION public.deposit_money_via_gateway(p_user_id uuid, p_amount numeric, p_gateway text, p_gateway_trx_id text, p_sender_phone text DEFAULT ''::text, p_note text DEFAULT ''::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_payment_id text;
  v_now timestamptz := now();
begin
  if p_amount <= 0 then
    raise exception 'INVALID_AMOUNT';
  end if;
  if not (auth.uid() = p_user_id or public.is_admin(auth.uid())) then
    raise exception 'NOT_AUTHORIZED';
  end if;
  if p_gateway_trx_id is not null and p_gateway_trx_id <> '' and exists (
    select 1 from public.gateway_payments where gateway_trx_id = p_gateway_trx_id
  ) then
    return jsonb_build_object('result', 'ALREADY_PROCESSED');
  end if;

  v_payment_id := 'GWPAY_' || replace(gen_random_uuid()::text, '-', '');

  update public.users set balance = balance + p_amount, updated_at = v_now where id = p_user_id;

  insert into public.gateway_payments (id, gateway_trx_id, user_id, amount, gateway, purpose, status, note, "timestamp")
  values (v_payment_id, p_gateway_trx_id, p_user_id, p_amount, p_gateway, 'WALLET_DEPOSIT', 'SUCCESS',
    coalesce(nullif(p_note, ''), 'ওয়ালেট ব্যালেন্স রিচার্জ (টপ-আপ)'), v_now);

  insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount,
    base_amount, type, escrow_id, "timestamp")
  values ('TRX_DEP_' || replace(gen_random_uuid()::text, '-', ''), '', 'ওয়ালেট রিচার্জ (' || p_gateway || ')',
    p_user_id, p_amount, p_amount, p_amount, 'WALLET_DEPOSIT', v_payment_id, v_now);

  insert into public.notifications (id, user_id, title, message, target_type, target_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), p_user_id,
    'রিচার্জ সফল', '৳' || p_amount::text || ' আপনার ব্যালেন্সে যোগ হয়েছে।', 'balance', p_user_id, v_now);

  return jsonb_build_object('result', 'OK', 'payment_id', v_payment_id);
end;
$function$
;
GRANT EXECUTE ON FUNCTION deposit_money_via_gateway(uuid,numeric,text,text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION deposit_money_via_gateway(uuid,numeric,text,text,text,text) TO service_role;

-- signature: process_withdrawal(text,text,text)
CREATE OR REPLACE FUNCTION public.process_withdrawal(p_withdrawal_id text, p_action text, p_trx_id text DEFAULT NULL::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_wd public.withdrawals%rowtype;
  v_now timestamptz := now();
  v_refund_trx_id text;
  v_role_active boolean;
begin
  if not public.is_admin(auth.uid()) then raise exception 'NOT_AUTHORIZED'; end if;
  if p_action not in ('COMPLETE', 'REJECT') then raise exception 'INVALID_ACTION'; end if;

  select * into v_wd from public.withdrawals where id = p_withdrawal_id for update;
  if not found then raise exception 'WITHDRAWAL_NOT_FOUND'; end if;

  if v_wd.status in ('COMPLETED', 'REJECTED') then
    if v_wd.status = 'COMPLETED' and p_action = 'COMPLETE' and p_trx_id is not null and v_wd.trx_id is distinct from p_trx_id then
      update public.withdrawals set trx_id = p_trx_id where id = p_withdrawal_id;
      return jsonb_build_object('result', 'TRX_ID_UPDATED');
    end if;
    return jsonb_build_object('result', 'ALREADY_TERMINAL', 'status', v_wd.status);
  end if;

  if v_wd.status <> 'PENDING' then
    return jsonb_build_object('result', 'INVALID_TRANSITION');
  end if;

  if p_action = 'COMPLETE' then
    update public.withdrawals set status = 'COMPLETED', trx_id = p_trx_id where id = p_withdrawal_id;
    return jsonb_build_object('result', 'OK', 'status', 'COMPLETED');
  else
    v_refund_trx_id := 'TRX_WD_REFUND_' || v_wd.id;
    if exists (select 1 from public.transactions where id = v_refund_trx_id) then
      update public.withdrawals set status = 'REJECTED', rejection_reason = p_trx_id where id = p_withdrawal_id;
      return jsonb_build_object('result', 'OK', 'status', 'REJECTED', 'note', 'already_refunded');
    end if;

    if v_wd.role = 'SOLVER' then
      select has_solver_role into v_role_active from public.users where id = v_wd.solver_id for update;
    else
      select has_user_role into v_role_active from public.users where id = v_wd.solver_id for update;
    end if;
    if v_role_active is null then
      raise exception 'ACCOUNT_NOT_FOUND';
    end if;
    if not v_role_active then
      raise exception 'ROLE_INACTIVE';
    end if;

    if v_wd.role = 'SOLVER' then
      update public.users set balance = balance + v_wd.amount, balance_solver = balance_solver + v_wd.amount, updated_at = v_now where id = v_wd.solver_id;
    else
      update public.users set balance = balance + v_wd.amount, balance_user = balance_user + v_wd.amount, updated_at = v_now where id = v_wd.solver_id;
    end if;

    insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, role, "timestamp")
    values (v_refund_trx_id, '', 'উইথড্র আবেদন বাতিল রিফান্ড', v_wd.solver_id, v_wd.amount, v_wd.amount, 'WITHDRAWAL_REFUND', v_wd.role, v_now);

    update public.withdrawals set status = 'REJECTED', rejection_reason = p_trx_id where id = p_withdrawal_id;

    insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_wd.solver_id, 'উইথড্র আবেদন বাতিল ও ব্যালেন্স রিফান্ড',
      'আপনার ৳' || v_wd.amount::text || ' উত্তোলনের আবেদন বাতিল করা হয়েছে এবং অর্থ আপনার ব্যালেন্সে ফিরিয়ে দেওয়া হয়েছে।' ||
      (case when p_trx_id is not null then ' কারণ: ' || p_trx_id else '' end), 'balance', v_wd.solver_id, v_wd.role, v_now);

    return jsonb_build_object('result', 'OK', 'status', 'REJECTED');
  end if;
end;
$function$
;
GRANT EXECUTE ON FUNCTION process_withdrawal(text,text,text) TO anon;
GRANT EXECUTE ON FUNCTION process_withdrawal(text,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION process_withdrawal(text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION process_withdrawal(text,text,text) TO service_role;

-- signature: refund_escrow_once(text,text,numeric)
CREATE OR REPLACE FUNCTION public.refund_escrow_once(p_escrow_id text, p_refund_type text DEFAULT 'SOLVER_CANCEL'::text, p_refund_percentage numeric DEFAULT 100)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_escrow public.escrows%rowtype;
  v_amount numeric(12,2);
  v_trx_id text := 'TRX_REFUND_' || p_escrow_id;
  v_now timestamptz := now();
  v_user_has_role boolean;
begin
  select * into v_escrow from public.escrows where id = p_escrow_id for update;
  if not found then
    raise exception 'ESCROW_NOT_FOUND';
  end if;

  if not (auth.uid() = v_escrow.user_id or auth.uid() = v_escrow.solver_id or public.is_admin(auth.uid())) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  if v_escrow.status in ('RELEASED', 'REFUNDED') then
    return jsonb_build_object('result', 'ALREADY_TERMINAL', 'status', v_escrow.status);
  end if;

  if exists (select 1 from public.transactions where id = v_trx_id) then
    return jsonb_build_object('result', 'ALREADY_REFUNDED');
  end if;

  select has_user_role into v_user_has_role from public.users where id = v_escrow.user_id for update;
  if v_user_has_role is null then
    raise exception 'USER_NOT_FOUND';
  end if;
  if not v_user_has_role then
    raise exception 'USER_ROLE_INACTIVE';
  end if;

  v_amount := round((v_escrow.base_amount + v_escrow.extra_amount) * (p_refund_percentage / 100.0), 2);

  update public.users set balance = balance + v_amount, balance_user = balance_user + v_amount, updated_at = v_now where id = v_escrow.user_id;

  insert into public.transactions (id, problem_id, problem_title, user_id, solver_id, gross_amount,
    net_amount, type, escrow_id, refund_type, refund_percentage, role, "timestamp")
  values (v_trx_id, v_escrow.problem_id, v_escrow.problem_title, v_escrow.user_id, v_escrow.solver_id,
    v_amount, v_amount, 'REFUND', p_escrow_id, p_refund_type, p_refund_percentage, 'USER', v_now);

  update public.escrows set status = 'REFUNDED', released_at = v_now, updated_at = v_now where id = p_escrow_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_escrow.user_id,
    'রিফান্ড সম্পন্ন', 'আপনার ৳' || v_amount::text || ' রিফান্ড করা হয়েছে।', 'balance', v_escrow.user_id, 'USER', v_now);

  return jsonb_build_object('result', 'OK', 'amount', v_amount);
end;
$function$
;
GRANT EXECUTE ON FUNCTION refund_escrow_once(text,text,numeric) TO anon;
GRANT EXECUTE ON FUNCTION refund_escrow_once(text,text,numeric) TO authenticated;
GRANT EXECUTE ON FUNCTION refund_escrow_once(text,text,numeric) TO postgres;
GRANT EXECUTE ON FUNCTION refund_escrow_once(text,text,numeric) TO service_role;

-- signature: release_escrow(text)
CREATE OR REPLACE FUNCTION public.release_escrow(p_escrow_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_escrow public.escrows%rowtype;
  v_problem public.problems%rowtype;
  v_commission_rate numeric(6,2);
  v_gross numeric(12,2);
  v_commission numeric(12,2);
  v_net numeric(12,2);
  v_trx_id text := 'TRX_RELEASE_' || p_escrow_id;
  v_now timestamptz := now();
  v_solver_has_role boolean;
begin
  select * into v_escrow from public.escrows where id = p_escrow_id for update;
  if not found then
    raise exception 'ESCROW_NOT_FOUND';
  end if;

  select * into v_problem from public.problems where id = v_escrow.problem_id for update;

  if not (auth.uid() = v_escrow.user_id or public.is_admin(auth.uid())) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  if v_escrow.status in ('RELEASED', 'REFUNDED') then
    return jsonb_build_object('result', 'ALREADY_TERMINAL', 'status', v_escrow.status);
  end if;

  if exists (select 1 from public.transactions where id = v_trx_id) then
    return jsonb_build_object('result', 'ALREADY_RELEASED');
  end if;

  select has_solver_role into v_solver_has_role from public.users where id = v_escrow.solver_id for update;
  if v_solver_has_role is null then
    raise exception 'SOLVER_NOT_FOUND';
  end if;
  if not v_solver_has_role then
    raise exception 'SOLVER_ROLE_INACTIVE';
  end if;

  v_gross := v_escrow.base_amount + v_escrow.extra_amount;
  v_commission_rate := coalesce(v_problem.applied_commission_rate,
    (select value::numeric from public.platform_settings where key = 'commission_percent'), 10.0);
  v_commission := round(v_gross * (v_commission_rate / 100.0), 2);
  v_net := v_gross - v_commission;

  update public.users set balance = balance + v_net, balance_solver = balance_solver + v_net, updated_at = v_now where id = v_escrow.solver_id;

  insert into public.transactions (id, problem_id, problem_title, user_id, solver_id, gross_amount,
    commission_percent, commission_amount, net_amount, base_amount, extra_amount, type, escrow_id,
    release_type, role, "timestamp")
  values (v_trx_id, v_escrow.problem_id, v_escrow.problem_title, v_escrow.user_id, v_escrow.solver_id,
    v_gross, v_commission_rate, v_commission, v_net, v_escrow.base_amount, v_escrow.extra_amount,
    'PAYMENT', p_escrow_id, 'FULL', 'SOLVER', v_now);

  update public.escrows set status = 'RELEASED', released_at = v_now, updated_at = v_now where id = p_escrow_id;

  update public.problems set status = 'COMPLETED', completed_at = v_now, last_activity_at = v_now
  where id = v_escrow.problem_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_escrow.solver_id,
    'পেমেন্ট প্রকাশিত হয়েছে', 'আপনার ৳' || v_net::text || ' পেমেন্ট আপনার ব্যালেন্সে যোগ হয়েছে।', 'balance', v_escrow.solver_id, 'SOLVER', v_now);

  return jsonb_build_object('result', 'OK', 'net_amount', v_net, 'commission', v_commission);
end;
$function$
;
GRANT EXECUTE ON FUNCTION release_escrow(text) TO anon;
GRANT EXECUTE ON FUNCTION release_escrow(text) TO authenticated;
GRANT EXECUTE ON FUNCTION release_escrow(text) TO postgres;
GRANT EXECUTE ON FUNCTION release_escrow(text) TO service_role;

-- signature: request_extra_amount(text,numeric,text)
CREATE OR REPLACE FUNCTION public.request_extra_amount(p_problem_id text, p_amount numeric, p_note text DEFAULT ''::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
  v_safe_amount numeric := greatest(coalesce(p_amount, 0), 0);
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if v_problem.accepted_solver_id is null or auth.uid() <> v_problem.accepted_solver_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.problems set
    pending_extra_amount = v_safe_amount,
    pending_extra_amount_note = trim(coalesce(p_note, '')),
    pending_extra_amount_requested_at = v_now,
    last_activity_at = v_now
  where id = p_problem_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    'অতিরিক্ত বিলের অনুরোধ এসেছে 🔔',
    coalesce(v_problem.accepted_solver_name, 'সমাধানকারী') || ' আপনার "' || v_problem.title || '" কাজের জন্য ৳' || trim(to_char(v_safe_amount, 'FM999999999')) || ' অতিরিক্ত বিল অনুরোধ করেছেন।' ||
      case when trim(coalesce(p_note,'')) <> '' then ' (নোট: ' || trim(p_note) || ')' else '' end,
    'problem', p_problem_id, p_problem_id, v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION request_extra_amount(text,numeric,text) TO "-";
GRANT EXECUTE ON FUNCTION request_extra_amount(text,numeric,text) TO anon;
GRANT EXECUTE ON FUNCTION request_extra_amount(text,numeric,text) TO authenticated;
GRANT EXECUTE ON FUNCTION request_extra_amount(text,numeric,text) TO postgres;
GRANT EXECUTE ON FUNCTION request_extra_amount(text,numeric,text) TO service_role;

-- signature: request_wallet_deposit(numeric,text,text,text,text,text)
CREATE OR REPLACE FUNCTION public.request_wallet_deposit(p_amount numeric, p_gateway text, p_gateway_trx_id text, p_sender_phone text DEFAULT ''::text, p_note text DEFAULT ''::text, p_role text DEFAULT 'USER'::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
    v_acct public.users%rowtype;
    v_role_active boolean;
    v_payment_id text := 'GWPAY_' || replace(gen_random_uuid()::text, '-', '');
    v_now timestamptz := now();
    v_auto_approve boolean := coalesce(
      (select value = 'true' from public.platform_settings where key = 'gateway_auto_approve_deposits'), true
    );
begin
    if p_role not in ('USER', 'SOLVER') then
        raise exception 'INVALID_ROLE';
    end if;
    if p_amount <= 0 then
        raise exception 'INVALID_AMOUNT';
    end if;
    if p_gateway not in ('BKASH','NAGAD','ROCKET','CARD') then
        raise exception 'INVALID_GATEWAY';
    end if;
    if p_gateway_trx_id is null or p_gateway_trx_id = '' then
        raise exception 'TRX_ID_REQUIRED';
    end if;
    if exists (select 1 from public.gateway_payments where gateway_trx_id = p_gateway_trx_id) then
        return jsonb_build_object('result', 'ALREADY_SUBMITTED');
    end if;

    select * into v_acct from public.users where id = auth.uid();
    if not found then raise exception 'USER_NOT_FOUND'; end if;

    if p_role = 'SOLVER' then
      v_role_active := v_acct.has_solver_role;
    else
      v_role_active := v_acct.has_user_role;
    end if;
    if not coalesce(v_role_active, false) then
      raise exception 'ROLE_INACTIVE';
    end if;

    if v_auto_approve then
        insert into public.gateway_payments (id, gateway_trx_id, user_id, amount, gateway, purpose, status, note, role, "timestamp")
        values (v_payment_id, p_gateway_trx_id, auth.uid(), p_amount, p_gateway, 'WALLET_DEPOSIT', 'SUCCESS',
            coalesce(nullif(p_note, ''), 'ওয়ালেট ব্যালেন্স রিচার্জ (টপ-আপ)') ||
            case when p_sender_phone <> '' then ' | প্রেরকের নম্বর: ' || p_sender_phone else '' end,
            p_role, v_now);

        if p_role = 'SOLVER' then
          update public.users set balance = balance + p_amount, balance_solver = balance_solver + p_amount, updated_at = v_now where id = auth.uid();
        else
          update public.users set balance = balance + p_amount, balance_user = balance_user + p_amount, updated_at = v_now where id = auth.uid();
        end if;

        insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount,
            base_amount, type, escrow_id, role, "timestamp")
        values ('TRX_DEP_' || v_payment_id, '', 'ওয়ালেট রিচার্জ (' || p_gateway || ')',
            auth.uid(), p_amount, p_amount, p_amount, 'WALLET_DEPOSIT', v_payment_id, p_role, v_now);

        insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
        values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), auth.uid(),
            'রিচার্জ সফল', '৳' || p_amount::text || ' আপনার ব্যালেন্সে যোগ হয়েছে।', 'balance', v_payment_id, p_role, v_now);

        return jsonb_build_object('result', 'OK', 'payment_id', v_payment_id);
    else
        insert into public.gateway_payments (id, gateway_trx_id, user_id, amount, gateway, purpose, status, note, role, "timestamp")
        values (v_payment_id, p_gateway_trx_id, auth.uid(), p_amount, p_gateway, 'WALLET_DEPOSIT', 'PENDING',
            coalesce(nullif(p_note, ''), 'ওয়ালেট রিচার্জ অনুরোধ — অ্যাডমিন যাচাইয়ের অপেক্ষায়') ||
            case when p_sender_phone <> '' then ' | প্রেরকের নম্বর: ' || p_sender_phone else '' end,
            p_role, v_now);

        insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
        values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), auth.uid(),
            'রিচার্জ অনুরোধ জমা হয়েছে',
            '৳' || p_amount::text || ' রিচার্জের অনুরোধ জমা হয়েছে, অ্যাডমিন যাচাই করার পর ব্যালেন্সে যোগ হবে।',
            'balance', v_payment_id, p_role, v_now);

        return jsonb_build_object('result', 'PENDING_APPROVAL', 'payment_id', v_payment_id);
    end if;
end;
$function$
;
GRANT EXECUTE ON FUNCTION request_wallet_deposit(numeric,text,text,text,text,text) TO "-";
GRANT EXECUTE ON FUNCTION request_wallet_deposit(numeric,text,text,text,text,text) TO anon;
GRANT EXECUTE ON FUNCTION request_wallet_deposit(numeric,text,text,text,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION request_wallet_deposit(numeric,text,text,text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION request_wallet_deposit(numeric,text,text,text,text,text) TO service_role;

-- signature: request_withdrawal(numeric,text,text,text,text,text,text)
CREATE OR REPLACE FUNCTION public.request_withdrawal(p_amount numeric, p_method text, p_account_number text, p_bank_name text DEFAULT NULL::text, p_branch_name text DEFAULT NULL::text, p_account_holder_name text DEFAULT NULL::text, p_role text DEFAULT 'SOLVER'::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_acct public.users%rowtype;
  v_role_balance numeric(12,2);
  v_role_active boolean;
  v_min_withdrawal numeric := coalesce((select value::numeric from public.platform_settings where key = 'min_withdrawal'), 100);
  v_withdraw_id text := 'WID-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8));
  v_now timestamptz := now();
begin
  if p_role not in ('USER', 'SOLVER') then
    raise exception 'INVALID_ROLE';
  end if;

  select * into v_acct from public.users where id = auth.uid() for update;
  if not found then raise exception 'USER_NOT_FOUND'; end if;

  if p_role = 'SOLVER' then
    v_role_active := v_acct.has_solver_role;
    v_role_balance := v_acct.balance_solver;
  else
    v_role_active := v_acct.has_user_role;
    v_role_balance := v_acct.balance_user;
  end if;

  if not coalesce(v_role_active, false) then
    raise exception 'ROLE_INACTIVE';
  end if;

  if p_amount < v_min_withdrawal then
    raise exception 'BELOW_MIN_WITHDRAWAL: %', v_min_withdrawal;
  end if;
  if p_amount > v_role_balance then
    raise exception 'INSUFFICIENT_BALANCE: %', v_role_balance;
  end if;

  if p_role = 'SOLVER' then
    update public.users set balance = balance - p_amount, balance_solver = balance_solver - p_amount, updated_at = v_now where id = v_acct.id;
  else
    update public.users set balance = balance - p_amount, balance_user = balance_user - p_amount, updated_at = v_now where id = v_acct.id;
  end if;

  insert into public.withdrawals (id, solver_id, solver_name, amount, method, account_number, bank_name,
    branch_name, account_holder_name, status, role, created_at)
  values (v_withdraw_id, v_acct.id, v_acct.name, p_amount, p_method, p_account_number, p_bank_name,
    p_branch_name, p_account_holder_name, 'PENDING', p_role, v_now);

  insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, escrow_id, role, "timestamp")
  values ('TRX_WD_DEDUCT_' || v_withdraw_id, '', 'উইথড্র আবেদন — ব্যালেন্স কর্তন', v_acct.id, p_amount, -p_amount,
    'WITHDRAWAL_DEDUCTION', v_withdraw_id, p_role, v_now);

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_acct.id, 'উইথড্র রিকোয়েস্ট জমা হয়েছে',
    '৳' || p_amount::text || ' উত্তোলনের আবেদন জমা হয়েছে (' || p_method || ': ' || p_account_number ||
    ', উইথড্র আইডি: ' || v_withdraw_id || ')। অ্যাডমিন অনুমোদন সাপেক্ষে টাকা পাঠানো হবে।', 'balance', v_acct.id, p_role, v_now);

  return jsonb_build_object('result', 'OK', 'withdrawal_id', v_withdraw_id);
end;
$function$
;
GRANT EXECUTE ON FUNCTION request_withdrawal(numeric,text,text,text,text,text,text) TO "-";
GRANT EXECUTE ON FUNCTION request_withdrawal(numeric,text,text,text,text,text,text) TO anon;
GRANT EXECUTE ON FUNCTION request_withdrawal(numeric,text,text,text,text,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION request_withdrawal(numeric,text,text,text,text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION request_withdrawal(numeric,text,text,text,text,text,text) TO service_role;

-- signature: respond_additional_charge(text,boolean)
CREATE OR REPLACE FUNCTION public.respond_additional_charge(p_charge_id text, p_accept boolean)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_charge public.additional_charges%rowtype;
  v_user public.users%rowtype;
  v_escrow public.escrows%rowtype;
  v_wallet_deduction numeric(12,2) := 0;
  v_now timestamptz := now();
begin
  select * into v_charge from public.additional_charges where id = p_charge_id for update;
  if not found then raise exception 'CHARGE_NOT_FOUND'; end if;
  if auth.uid() <> v_charge.user_id and not public.is_admin(auth.uid()) then raise exception 'NOT_AUTHORIZED'; end if;
  if v_charge.status <> 'PENDING' then
    return jsonb_build_object('result', 'ALREADY_RESPONDED', 'status', v_charge.status);
  end if;

  update public.additional_charges set status = case when p_accept then 'ACCEPTED' else 'REJECTED' end, responded_at = v_now
  where id = p_charge_id;
  update public.problems set last_activity_at = v_now where id = v_charge.problem_id;

  if p_accept then
    select * into v_user from public.users where id = v_charge.user_id for update;

    -- [ধাপ ১৪.৫গ] role-activation safety check (structurally USER role, release_escrow-এর প্যাটার্নে)
    if not coalesce(v_user.has_user_role, false) then
      raise exception 'USER_ROLE_INACTIVE';
    end if;

    -- [মানি-ফ্লো ফিক্স, ধাপ ৪] deduction হিসাব role-scoped balance_user থেকে হবে, legacy shared
    -- balance থেকে না (role-switch-এর সময় stale/ambiguous হতে পারে; owner সবসময় USER role-এর মানুষ,
    -- ঠিক accept_bid-এর ধাপ ১ ফিক্সের একই প্যাটার্ন)।
    v_wallet_deduction := least(greatest(v_user.balance_user, 0), v_charge.amount);
    if v_wallet_deduction > 0 then
      update public.users set balance = balance - v_wallet_deduction, balance_user = balance_user - v_wallet_deduction, updated_at = v_now where id = v_user.id;
      -- [ধাপ ৩৬] transactions.role যোগ — role='USER'
      insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, role, "timestamp")
      values ('TRX_EXTRA_CHARGE_' || v_charge.id, v_charge.problem_id, '', v_user.id, v_wallet_deduction, -v_wallet_deduction,
        'EXTRA_CHARGE_DEDUCTION', 'USER', v_now)
      on conflict (id) do nothing;
    end if;

    select * into v_escrow from public.escrows where problem_id = v_charge.problem_id and status = 'HELD'
      order by created_at desc limit 1 for update;
    if found then
      update public.escrows set extra_amount = extra_amount + v_wallet_deduction, updated_at = v_now where id = v_escrow.id;
      update public.problems set confirmed_extra_amount_total = confirmed_extra_amount_total + v_wallet_deduction where id = v_charge.problem_id;
    end if;
  end if;

  return jsonb_build_object('result', 'OK', 'status', case when p_accept then 'ACCEPTED' else 'REJECTED' end, 'wallet_deduction', v_wallet_deduction);
end;
$function$
;
GRANT EXECUTE ON FUNCTION respond_additional_charge(text,boolean) TO anon;
GRANT EXECUTE ON FUNCTION respond_additional_charge(text,boolean) TO authenticated;
GRANT EXECUTE ON FUNCTION respond_additional_charge(text,boolean) TO postgres;
GRANT EXECUTE ON FUNCTION respond_additional_charge(text,boolean) TO service_role;

-- signature: user_confirm_extra_amount(text)
CREATE OR REPLACE FUNCTION public.user_confirm_extra_amount(p_problem_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_user public.users%rowtype;
  v_escrow public.escrows%rowtype;
  v_now timestamptz := now();
  v_amt numeric;
  v_wallet_deduction numeric(12,2) := 0;
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if auth.uid() <> v_problem.user_id then raise exception 'NOT_AUTHORIZED'; end if;
  if v_problem.pending_extra_amount is null then
    return jsonb_build_object('result', 'NOT_PENDING');
  end if;
  v_amt := v_problem.pending_extra_amount;

  select * into v_user from public.users where id = v_problem.user_id for update;
  v_wallet_deduction := least(greatest(v_user.balance, 0), v_amt);
  if v_wallet_deduction > 0 then
    update public.users set balance = balance - v_wallet_deduction, updated_at = v_now where id = v_user.id;
    insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, role, "timestamp")
    values ('TRX_CONFIRM_EXTRA_' || replace(gen_random_uuid()::text, '-', ''), v_problem.id, v_problem.title, v_user.id,
      v_wallet_deduction, -v_wallet_deduction, 'EXTRA_CHARGE_DEDUCTION', 'USER', v_now);
  end if;

  select * into v_escrow from public.escrows where problem_id = v_problem.id and status = 'HELD'
    order by created_at desc limit 1 for update;
  if found then
    update public.escrows set extra_amount = extra_amount + v_amt, updated_at = v_now where id = v_escrow.id;
  end if;

  update public.problems set
    confirmed_extra_amount_total = confirmed_extra_amount_total + v_amt,
    pending_extra_amount = null,
    pending_extra_amount_note = null,
    pending_extra_amount_requested_at = null,
    last_activity_at = v_now
  where id = p_problem_id;

  if v_problem.accepted_solver_id is not null then
    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
      'অতিরিক্ত বিল অনুমোদিত ✅',
      'ক্লায়েন্ট আপনার ৳' || trim(to_char(v_amt, 'FM999999999')) || ' অতিরিক্ত বিলের অনুরোধ অনুমোদন করেছেন।',
      'problem', p_problem_id, p_problem_id, 'SOLVER', v_now);
  end if;

  return jsonb_build_object('result', 'OK', 'confirmed_amount', v_amt, 'wallet_deduction', v_wallet_deduction);
end;
$function$
;
GRANT EXECUTE ON FUNCTION user_confirm_extra_amount(text) TO "-";
GRANT EXECUTE ON FUNCTION user_confirm_extra_amount(text) TO anon;
GRANT EXECUTE ON FUNCTION user_confirm_extra_amount(text) TO authenticated;
GRANT EXECUTE ON FUNCTION user_confirm_extra_amount(text) TO postgres;
GRANT EXECUTE ON FUNCTION user_confirm_extra_amount(text) TO service_role;

-- signature: user_reject_extra_amount(text)
CREATE OR REPLACE FUNCTION public.user_reject_extra_amount(p_problem_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if auth.uid() <> v_problem.user_id then raise exception 'NOT_AUTHORIZED'; end if;

  update public.problems set
    pending_extra_amount = null,
    pending_extra_amount_note = null,
    pending_extra_amount_requested_at = null,
    last_activity_at = v_now
  where id = p_problem_id;

  if v_problem.accepted_solver_id is not null then
    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
      'অতিরিক্ত বিল প্রত্যাখ্যাত ❌',
      'ক্লায়েন্ট আপনার অতিরিক্ত বিলের অনুরোধ প্রত্যাখ্যান করেছেন।',
      'problem', p_problem_id, p_problem_id, v_now);
  end if;

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION user_reject_extra_amount(text) TO "-";
GRANT EXECUTE ON FUNCTION user_reject_extra_amount(text) TO anon;
GRANT EXECUTE ON FUNCTION user_reject_extra_amount(text) TO authenticated;
GRANT EXECUTE ON FUNCTION user_reject_extra_amount(text) TO postgres;
GRANT EXECUTE ON FUNCTION user_reject_extra_amount(text) TO service_role;

