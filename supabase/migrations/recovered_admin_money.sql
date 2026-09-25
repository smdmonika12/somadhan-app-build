-- RECOVERED from live DB on 2026-09-18, verbatim capture, no logic changes. See
-- RPC_SYNC_FIX_PROGRESS.md Step 2.
-- Functions in this file (Step 1 missing-RPC group): admin_adjust_balance, admin_confirm_gateway_deposit, admin_refund_and_reopen_problem

-- signature: admin_adjust_balance(uuid,numeric,boolean,text)
CREATE OR REPLACE FUNCTION public.admin_adjust_balance(p_user_id uuid, p_amount numeric, p_is_addition boolean, p_reason text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_signature text := md5(p_user_id::text || '|' || p_is_addition::text || '|' || p_amount::text || '|' || p_reason);
  v_now timestamptz := now();
  v_user_name text;
  v_trx_id text := 'TRX_ADMIN_ADJ_' || replace(gen_random_uuid()::text, '-', '');
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;
  if exists (
    select 1 from public.idempotency_keys
    where key = v_signature and created_at > v_now - interval '5 seconds'
  ) then
    return jsonb_build_object('result', 'DUPLICATE_SKIPPED');
  end if;
  insert into public.idempotency_keys (key, request_type, created_at) values (v_signature, 'admin_adjust_balance', v_now)
    on conflict (key) do update set created_at = v_now;

  if p_is_addition then
    update public.users set balance = balance + p_amount, updated_at = v_now where id = p_user_id returning name into v_user_name;
  else
    update public.users set balance = greatest(balance - p_amount, 0), updated_at = v_now where id = p_user_id returning name into v_user_name;
  end if;

  insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, role, "timestamp")
  values (v_trx_id, '', case when p_is_addition then 'অ্যাডমিন কর্তৃক ব্যালেন্স সংযোজন' else 'অ্যাডমিন কর্তৃক ব্যালেন্স কর্তন' end,
    p_user_id, p_amount, case when p_is_addition then p_amount else -p_amount end, 'ADMIN_ADJUSTMENT', '', v_now);

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), p_user_id,
    case when p_is_addition then 'ব্যালেন্স যোগ করা হয়েছে' else 'ব্যালেন্স কর্তন করা হয়েছে' end,
    'অ্যাডমিন কর্তৃক আপনার ব্যালেন্স ' || (case when p_is_addition then 'যোগ' else 'কর্তন' end) || ' করা হয়েছে। কারণ: ' || p_reason,
    'balance', p_user_id, '', v_now);

  insert into public.admin_audit_logs (id, action_type, target_id, target_name, details, role, "timestamp")
  values ('AUDIT_' || replace(gen_random_uuid()::text, '-', ''), case when p_is_addition then 'ADD_BALANCE' else 'DEDUCT_BALANCE' end,
    p_user_id::text, coalesce(v_user_name, p_user_id::text),
    (case when p_is_addition then 'যোগ' else 'কর্তন' end) || ': ৳' || p_amount::text || ', কারণ: ' || p_reason, '', v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_adjust_balance(uuid,numeric,boolean,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_adjust_balance(uuid,numeric,boolean,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_adjust_balance(uuid,numeric,boolean,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_adjust_balance(uuid,numeric,boolean,text) TO service_role;

-- signature: admin_adjust_balance(uuid,numeric,boolean,text,text)
CREATE OR REPLACE FUNCTION public.admin_adjust_balance(p_user_id uuid, p_amount numeric, p_is_addition boolean, p_reason text, p_role text DEFAULT 'USER'::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_signature text := md5(p_user_id::text || '|' || p_is_addition::text || '|' || p_amount::text || '|' || p_reason || '|' || p_role);
  v_now timestamptz := now();
  v_user_name text;
  v_role_active boolean;
  v_trx_id text := 'TRX_ADMIN_ADJ_' || replace(gen_random_uuid()::text, '-', '');
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;
  if p_role not in ('USER', 'SOLVER') then
    raise exception 'INVALID_ROLE';
  end if;
  if exists (
    select 1 from public.idempotency_keys
    where key = v_signature and created_at > v_now - interval '5 seconds'
  ) then
    return jsonb_build_object('result', 'DUPLICATE_SKIPPED');
  end if;
  insert into public.idempotency_keys (key, request_type, created_at) values (v_signature, 'admin_adjust_balance', v_now)
    on conflict (key) do update set created_at = v_now;

  select (case when p_role = 'SOLVER' then has_solver_role else has_user_role end) into v_role_active
    from public.users where id = p_user_id;
  if not coalesce(v_role_active, false) then
    raise exception 'ROLE_INACTIVE';
  end if;

  if p_role = 'SOLVER' then
    if p_is_addition then
      update public.users set balance = balance + p_amount, balance_solver = balance_solver + p_amount, updated_at = v_now where id = p_user_id returning name into v_user_name;
    else
      update public.users set balance = greatest(balance - p_amount, 0), balance_solver = greatest(balance_solver - p_amount, 0), updated_at = v_now where id = p_user_id returning name into v_user_name;
    end if;
  else
    if p_is_addition then
      update public.users set balance = balance + p_amount, balance_user = balance_user + p_amount, updated_at = v_now where id = p_user_id returning name into v_user_name;
    else
      update public.users set balance = greatest(balance - p_amount, 0), balance_user = greatest(balance_user - p_amount, 0), updated_at = v_now where id = p_user_id returning name into v_user_name;
    end if;
  end if;

  insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, role, "timestamp")
  values (v_trx_id, '', case when p_is_addition then 'অ্যাডমিন কর্তৃক ব্যালেন্স সংযোজন' else 'অ্যাডমিন কর্তৃক ব্যালেন্স কর্তন' end,
    p_user_id, p_amount, case when p_is_addition then p_amount else -p_amount end, 'ADMIN_ADJUSTMENT', p_role, v_now);

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), p_user_id,
    case when p_is_addition then 'ব্যালেন্স যোগ করা হয়েছে' else 'ব্যালেন্স কর্তন করা হয়েছে' end,
    'অ্যাডমিন কর্তৃক আপনার ব্যালেন্স ' || (case when p_is_addition then 'যোগ' else 'কর্তন' end) || ' করা হয়েছে। কারণ: ' || p_reason,
    'balance', p_user_id, p_role, v_now);

  insert into public.admin_audit_logs (id, action_type, target_id, target_name, details, role, "timestamp")
  values ('AUDIT_' || replace(gen_random_uuid()::text, '-', ''), case when p_is_addition then 'ADD_BALANCE' else 'DEDUCT_BALANCE' end,
    p_user_id::text, coalesce(v_user_name, p_user_id::text),
    (case when p_is_addition then 'যোগ' else 'কর্তন' end) || ': ৳' || p_amount::text || ', কারণ: ' || p_reason, p_role, v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_adjust_balance(uuid,numeric,boolean,text,text) TO "-";
GRANT EXECUTE ON FUNCTION admin_adjust_balance(uuid,numeric,boolean,text,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_adjust_balance(uuid,numeric,boolean,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_adjust_balance(uuid,numeric,boolean,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_adjust_balance(uuid,numeric,boolean,text,text) TO service_role;

-- signature: admin_confirm_gateway_deposit(text,text)
CREATE OR REPLACE FUNCTION public.admin_confirm_gateway_deposit(p_payment_id text, p_action text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
    v_pay public.gateway_payments%rowtype;
    v_now timestamptz := now();
begin
    if not public.is_admin(auth.uid()) then
        raise exception 'NOT_AUTHORIZED';
    end if;
    if p_action not in ('APPROVE','REJECT') then
        raise exception 'INVALID_ACTION';
    end if;

    select * into v_pay from public.gateway_payments where id = p_payment_id for update;
    if not found then
        raise exception 'PAYMENT_NOT_FOUND';
    end if;
    if v_pay.status <> 'PENDING' then
        return jsonb_build_object('result', 'ALREADY_PROCESSED', 'status', v_pay.status);
    end if;

    if p_action = 'REJECT' then
        update public.gateway_payments set status = 'FAILED' where id = p_payment_id;
        insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
        values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_pay.user_id,
            'রিচার্জ প্রত্যাখ্যাত', 'আপনার ৳' || v_pay.amount::text || ' রিচার্জ অনুরোধ যাচাই করা যায়নি, তাই প্রত্যাখ্যান করা হয়েছে।',
            'balance', v_pay.user_id, v_pay.role, v_now);
        return jsonb_build_object('result', 'REJECTED');
    end if;

    update public.gateway_payments set status = 'SUCCESS' where id = p_payment_id;

    if v_pay.role = 'SOLVER' then
      update public.users set balance = balance + v_pay.amount, balance_solver = balance_solver + v_pay.amount, updated_at = v_now where id = v_pay.user_id;
    else
      update public.users set balance = balance + v_pay.amount, balance_user = balance_user + v_pay.amount, updated_at = v_now where id = v_pay.user_id;
    end if;

    insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount,
        base_amount, type, escrow_id, role, "timestamp")
    values ('TRX_DEP_' || v_pay.id, '', 'ওয়ালেট রিচার্জ (' || v_pay.gateway || ') — অ্যাডমিন কর্তৃক অনুমোদিত',
        v_pay.user_id, v_pay.amount, v_pay.amount, v_pay.amount, 'WALLET_DEPOSIT', v_pay.id, v_pay.role, v_now);

    insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_pay.user_id,
        'রিচার্জ সফল', '৳' || v_pay.amount::text || ' আপনার ব্যালেন্সে যোগ হয়েছে।', 'balance', v_pay.user_id, v_pay.role, v_now);

    return jsonb_build_object('result', 'OK', 'amount', v_pay.amount);
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_confirm_gateway_deposit(text,text) TO "-";
GRANT EXECUTE ON FUNCTION admin_confirm_gateway_deposit(text,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_confirm_gateway_deposit(text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_confirm_gateway_deposit(text,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_confirm_gateway_deposit(text,text) TO service_role;

-- signature: admin_refund_and_reopen_problem(text)
CREATE OR REPLACE FUNCTION public.admin_refund_and_reopen_problem(p_problem_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
  v_accepted_bid_id text;
  v_accepted_solver_id uuid;
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  v_accepted_bid_id := v_problem.accepted_bid_id;
  v_accepted_solver_id := v_problem.accepted_solver_id;

  update public.problems
  set status = 'OPEN',
      accepted_bid_id = null,
      accepted_solver_id = null,
      accepted_solver_name = null,
      accepted_amount = null,
      last_activity_at = v_now
  where id = p_problem_id;

  -- Kotlin adminRefundEscrow()-এর ঠিক একই fallback প্যাটার্ন: আগে accepted_bid_id দিয়ে খোঁজা,
  -- না থাকলে solver_id + status=ACCEPTED দিয়ে খোঁজা।
  if v_accepted_bid_id is not null then
    update public.bids
    set status = 'CANCELLED', resolution_type = 'ADMIN_MANUAL_REFUND', resolved_at = v_now
    where id = v_accepted_bid_id and status <> 'CANCELLED';
  elsif v_accepted_solver_id is not null then
    update public.bids
    set status = 'CANCELLED', resolution_type = 'ADMIN_MANUAL_REFUND', resolved_at = v_now
    where problem_id = p_problem_id and solver_id = v_accepted_solver_id and status = 'ACCEPTED';
  end if;

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_refund_and_reopen_problem(text) TO "-";
GRANT EXECUTE ON FUNCTION admin_refund_and_reopen_problem(text) TO anon;
GRANT EXECUTE ON FUNCTION admin_refund_and_reopen_problem(text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_refund_and_reopen_problem(text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_refund_and_reopen_problem(text) TO service_role;

