-- step36_transaction_role_column_and_rpc_dual_write.sql
--
-- ধাপ ৩ (BALANCE_REPUTATION_ROLE_SEPARATION_MASTER_PROMPT.md) — Supabase-সাইড অংশ।
-- TRANSACTION_ROLE_FIELD_DESIGN.md #৩ ও #৪-এর সিদ্ধান্ত অনুযায়ী: (ক) transactions টেবিলে role
-- কলাম যোগ, (খ) প্রতিটা টাকা-সংক্রান্ত RPC (যেগুলো `public.transactions`-এ row insert করে)
-- এখন সেই role কলামেও লিখবে।
--
-- ⚠️ এই ফাইলটা শুধু লেখা হয়েছে, নিজে থেকে Supabase-এ apply করা হয়নি (General Rules #৩ অনুযায়ী,
-- apply করা ব্যবহারকারীর কাজ)। নিচের সব `CREATE OR REPLACE FUNCTION`-এর বডি লাইভ DB থেকে
-- read-only query দিয়ে verbatim তোলা হয়েছে (mcp Supabase execute_sql, শুধু SELECT — কোনো লেখা/
-- apply হয়নি), শুধু `public.transactions`-এ INSERT হওয়া প্রতিটা জায়গায় `role` কলাম + মান যোগ
-- করা হয়েছে। বাকি লজিক অক্ষত/অপরিবর্তিত (rule #২: কোনো existing behavior বদলায়নি, শুধু নতুন
-- কলাম-মান যোগ হয়েছে)।
--
-- ⚠️ দুটো গুরুত্বপূর্ণ পর্যবেক্ষণ (এই migration-এর স্কোপের বাইরে, ছোঁয়া হয়নি, শুধু ফ্ল্যাগ করা হলো
-- — বিস্তারিত রিপোর্টে/progress log-এ):
--   ১. `admin_adjust_balance`-এর লাইভ DB-তে দুটো আলাদা overload আছে (৪-প্যারামিটার পুরনোটা, আর
--      ৫-প্যারামিটার p_role-সহ নতুনটা) — এটা একটা প্রি-এক্সিস্টিং সম্ভাব্য bug (PostgREST
--      ambiguous-function-resolution ঝুঁকি), এই migration-এ ঠিক করা হয়নি।
--   ২. `user_confirm_extra_amount()` এখনও `v_user.balance` (shared/legacy কলাম) থেকে deduction
--      হিসাব করে, তার sibling `respond_additional_charge()`-এর মতো `balance_user`-ভিত্তিক না —
--      এটাও এই migration-এর স্কোপের বাইরে (এই ধাপ শুধু role ট্যাগিং, ব্যালেন্স-হিসাব-লজিক না)।

-- ১. transactions টেবিলে role কলাম (nullable/blank default, বিদ্যমান row না ভেঙে)
alter table public.transactions
  add column if not exists role text not null default '';

-- ২. accept_bid — BID_ACCEPT_DEDUCTION transaction role='USER' (পোস্টারের wallet deduction)
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

-- ৩ক. admin_adjust_balance — পুরনো ৪-প্যারামিটার overload (কোনো role তথ্য নেই এই ওভারলোডে,
-- তাই role='' — unclassified, ঠিক Kotlin-সাইড sites #৫/#১২-এর "adjustRole ?: \"\"" fallback প্যাটার্নের সাথে সামঞ্জস্যপূর্ণ)
-- ⚠️ এই overload আর নিচেরটা (৩খ) একসাথে থাকা প্রি-এক্সিস্টিং সমস্যা, migration নোট দেখুন উপরে।
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

  -- [ধাপ ৩৬] transactions.role যোগ — এই ওভারলোডে role তথ্য পাওয়া যায় না, তাই '' (unclassified)
  insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, role, "timestamp")
  values (v_trx_id, '', case when p_is_addition then 'অ্যাডমিন কর্তৃক ব্যালেন্স সংযোজন' else 'অ্যাডমিন কর্তৃক ব্যালেন্স কর্তন' end,
    p_user_id, p_amount, case when p_is_addition then p_amount else -p_amount end, 'ADMIN_ADJUSTMENT', '', v_now);

  insert into public.notifications (id, user_id, title, message, target_type, target_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), p_user_id,
    case when p_is_addition then 'ব্যালেন্স যোগ করা হয়েছে' else 'ব্যালেন্স কর্তন করা হয়েছে' end,
    'অ্যাডমিন কর্তৃক আপনার ব্যালেন্স ' || (case when p_is_addition then 'যোগ' else 'কর্তন' end) || ' করা হয়েছে। কারণ: ' || p_reason,
    'balance', p_user_id, v_now);

  insert into public.admin_audit_logs (id, action_type, target_id, target_name, details, "timestamp")
  values ('AUDIT_' || replace(gen_random_uuid()::text, '-', ''), case when p_is_addition then 'ADD_BALANCE' else 'DEDUCT_BALANCE' end,
    p_user_id::text, coalesce(v_user_name, p_user_id::text),
    (case when p_is_addition then 'যোগ' else 'কর্তন' end) || ': ৳' || p_amount::text || ', কারণ: ' || p_reason, v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;

-- ৩খ. admin_adjust_balance — নতুন ৫-প্যারামিটার (p_role) overload — role = p_role
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

  -- [ধাপ ৩৬] transactions.role যোগ — role = p_role (এই ওভারলোডে ইতিমধ্যেই জানা)
  insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, role, "timestamp")
  values (v_trx_id, '', case when p_is_addition then 'অ্যাডমিন কর্তৃক ব্যালেন্স সংযোজন' else 'অ্যাডমিন কর্তৃক ব্যালেন্স কর্তন' end,
    p_user_id, p_amount, case when p_is_addition then p_amount else -p_amount end, 'ADMIN_ADJUSTMENT', p_role, v_now);

  insert into public.notifications (id, user_id, title, message, target_type, target_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), p_user_id,
    case when p_is_addition then 'ব্যালেন্স যোগ করা হয়েছে' else 'ব্যালেন্স কর্তন করা হয়েছে' end,
    'অ্যাডমিন কর্তৃক আপনার ব্যালেন্স ' || (case when p_is_addition then 'যোগ' else 'কর্তন' end) || ' করা হয়েছে। কারণ: ' || p_reason,
    'balance', p_user_id, v_now);

  insert into public.admin_audit_logs (id, action_type, target_id, target_name, details, "timestamp")
  values ('AUDIT_' || replace(gen_random_uuid()::text, '-', ''), case when p_is_addition then 'ADD_BALANCE' else 'DEDUCT_BALANCE' end,
    p_user_id::text, coalesce(v_user_name, p_user_id::text),
    (case when p_is_addition then 'যোগ' else 'কর্তন' end) || ': ৳' || p_amount::text || ', কারণ: ' || p_reason, v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;

-- ৪. admin_confirm_gateway_deposit — role = v_pay.role (gateway_payments.role, request_wallet_deposit-এ আগে থেকেই সেট হয়)
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
        insert into public.notifications (id, user_id, title, message, target_type, target_id, "timestamp")
        values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_pay.user_id,
            'রিচার্জ প্রত্যাখ্যাত', 'আপনার ৳' || v_pay.amount::text || ' রিচার্জ অনুরোধ যাচাই করা যায়নি, তাই প্রত্যাখ্যান করা হয়েছে।',
            'balance', v_pay.user_id, v_now);
        return jsonb_build_object('result', 'REJECTED');
    end if;

    update public.gateway_payments set status = 'SUCCESS' where id = p_payment_id;

    -- [ধাপ ১৪.৫গ] role-scoped dual-write (v_pay.role অনুযায়ী, request_wallet_deposit-এ সেট হয়েছিল)
    if v_pay.role = 'SOLVER' then
      update public.users set balance = balance + v_pay.amount, balance_solver = balance_solver + v_pay.amount, updated_at = v_now where id = v_pay.user_id;
    else
      update public.users set balance = balance + v_pay.amount, balance_user = balance_user + v_pay.amount, updated_at = v_now where id = v_pay.user_id;
    end if;

    -- [ধাপ ৩৬] transactions.role যোগ — role = v_pay.role
    insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount,
        base_amount, type, escrow_id, role, "timestamp")
    values ('TRX_DEP_' || v_pay.id, '', 'ওয়ালেট রিচার্জ (' || v_pay.gateway || ') — অ্যাডমিন কর্তৃক অনুমোদিত',
        v_pay.user_id, v_pay.amount, v_pay.amount, v_pay.amount, 'WALLET_DEPOSIT', v_pay.id, v_pay.role, v_now);

    insert into public.notifications (id, user_id, title, message, target_type, target_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_pay.user_id,
        'রিচার্জ সফল', '৳' || v_pay.amount::text || ' আপনার ব্যালেন্সে যোগ হয়েছে।', 'balance', v_pay.user_id, v_now);

    return jsonb_build_object('result', 'OK', 'amount', v_pay.amount);
end;
$function$
;

-- ৫. admin_reconcile_user_balances — ইতিমধ্যেই role অনুযায়ী দুইটা আলাদা ব্লক (USER/SOLVER),
-- শুধু প্রতিটা correction-insert-এ role কলাম-মান যোগ করা হলো
CREATE OR REPLACE FUNCTION public.admin_reconcile_user_balances(p_dry_run boolean DEFAULT true)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_now timestamptz := now();
  v_tolerance numeric := 1.0;
  v_row record;
  v_scanned int := 0;
  v_mismatch_count int := 0;
  v_corrected_count int := 0;
  v_total_abs_diff numeric := 0;
  v_items jsonb := '[]'::jsonb;
  v_diff numeric;
begin
  if not is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  create temp table tmp_ledger_user on commit drop as
    select user_id as id, sum(net_amount) as ledger
    from public.transactions
    where user_id is not null
      and type <> 'PAYMENT' and type <> 'BALANCE_RECONCILIATION'
    group by user_id;

  create temp table tmp_ledger_solver on commit drop as
    select solver_id as id, sum(net_amount) as ledger
    from public.transactions
    where solver_id is not null and type = 'PAYMENT'
    group by solver_id;

  for v_row in
    select u.id, u.name, u.balance_user, u.balance_solver,
      coalesce(lu.ledger, 0) as ledger_user, coalesce(ls.ledger, 0) as ledger_solver
    from public.users u
    left join tmp_ledger_user lu on lu.id = u.id
    left join tmp_ledger_solver ls on ls.id = u.id
  loop
    v_scanned := v_scanned + 1;

    v_diff := v_row.balance_user - v_row.ledger_user;
    if abs(v_diff) > v_tolerance then
      v_mismatch_count := v_mismatch_count + 1;
      v_total_abs_diff := v_total_abs_diff + abs(v_diff);
      v_items := v_items || jsonb_build_object(
        'user_id', v_row.id, 'name', v_row.name, 'role', 'USER',
        'stored', v_row.balance_user, 'ledger', v_row.ledger_user, 'difference', v_diff
      );
      if not p_dry_run then
        update public.users set balance_user = v_row.ledger_user, updated_at = v_now where id = v_row.id;
        -- [ধাপ ৩৬] transactions.role যোগ — role='USER' (এই ব্লকই balance_user সংশোধন করছে)
        insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, role, "timestamp")
        values ('TRX_RECONCILE_U_' || v_row.id || '_' || extract(epoch from v_now)::bigint, '',
          'ব্যালেন্স সংশোধন (লেজার অডিট, USER)', v_row.id, abs(v_diff), (v_row.ledger_user - v_row.balance_user),
          'BALANCE_RECONCILIATION', 'USER', v_now);
        v_corrected_count := v_corrected_count + 1;
      end if;
    end if;

    v_diff := v_row.balance_solver - v_row.ledger_solver;
    if abs(v_diff) > v_tolerance then
      v_mismatch_count := v_mismatch_count + 1;
      v_total_abs_diff := v_total_abs_diff + abs(v_diff);
      v_items := v_items || jsonb_build_object(
        'user_id', v_row.id, 'name', v_row.name, 'role', 'SOLVER',
        'stored', v_row.balance_solver, 'ledger', v_row.ledger_solver, 'difference', v_diff
      );
      if not p_dry_run then
        update public.users set balance_solver = v_row.ledger_solver, updated_at = v_now where id = v_row.id;
        -- [ধাপ ৩৬] transactions.role যোগ — role='SOLVER' (এই ব্লকই balance_solver সংশোধন করছে)
        insert into public.transactions (id, problem_id, problem_title, solver_id, gross_amount, net_amount, type, role, "timestamp")
        values ('TRX_RECONCILE_S_' || v_row.id || '_' || extract(epoch from v_now)::bigint, '',
          'ব্যালেন্স সংশোধন (লেজার অডিট, SOLVER)', v_row.id, abs(v_diff), (v_row.ledger_solver - v_row.balance_solver),
          'BALANCE_RECONCILIATION', 'SOLVER', v_now);
        v_corrected_count := v_corrected_count + 1;
      end if;
    end if;
  end loop;

  if not p_dry_run and v_corrected_count > 0 then
    insert into public.admin_audit_logs (id, action_type, target_id, target_name, details, "timestamp")
    values ('AUDIT_' || replace(gen_random_uuid()::text, '-', ''), 'ADMIN_RECONCILE_USER_BALANCES', '', '',
      'corrected=' || v_corrected_count || ', total_abs_diff=' || v_total_abs_diff, v_now);
  end if;

  return jsonb_build_object(
    'dry_run', p_dry_run, 'scanned_users', v_scanned, 'mismatch_count', v_mismatch_count,
    'corrected_count', v_corrected_count, 'total_absolute_difference', v_total_abs_diff, 'items', v_items
  );
end;
$function$
;

-- ৬. process_withdrawal — REJECT-refund path: role = v_wd.role (withdrawals.role, request_withdrawal-এ সেট হয়)
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
  v_role_active boolean; -- [ধাপ ১৪.৫খ]
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

    -- [ধাপ ১৪.৫খ] role-activation safety check: reject-refund যে role-এ ফেরত যাচ্ছে সেটা এখনো active কিনা
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

    -- [ধাপ ১৪.৫খ] dual-write: পুরনো shared balance (অপরিবর্তিত, rule #2) + role-scoped কলাম (v_wd.role অনুযায়ী)
    if v_wd.role = 'SOLVER' then
      update public.users set balance = balance + v_wd.amount, balance_solver = balance_solver + v_wd.amount, updated_at = v_now where id = v_wd.solver_id;
    else
      update public.users set balance = balance + v_wd.amount, balance_user = balance_user + v_wd.amount, updated_at = v_now where id = v_wd.solver_id;
    end if;

    -- [ধাপ ৩৬] transactions.role যোগ — role = v_wd.role
    insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, role, "timestamp")
    values (v_refund_trx_id, '', 'উইথড্র আবেদন বাতিল রিফান্ড', v_wd.solver_id, v_wd.amount, v_wd.amount, 'WITHDRAWAL_REFUND', v_wd.role, v_now);

    update public.withdrawals set status = 'REJECTED', rejection_reason = p_trx_id where id = p_withdrawal_id;

    insert into public.notifications (id, user_id, title, message, target_type, target_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_wd.solver_id, 'উইথড্র আবেদন বাতিল ও ব্যালেন্স রিফান্ড',
      'আপনার ৳' || v_wd.amount::text || ' উত্তোলনের আবেদন বাতিল করা হয়েছে এবং অর্থ আপনার ব্যালেন্সে ফিরিয়ে দেওয়া হয়েছে।' ||
      (case when p_trx_id is not null then ' কারণ: ' || p_trx_id else '' end), 'balance', v_wd.solver_id, v_now);

    return jsonb_build_object('result', 'OK', 'status', 'REJECTED');
  end if;
end;
$function$
;

-- ৭. refund_escrow_once — role='USER' (ফাংশনের নিজস্ব কমেন্টেই বলা আছে: refund সবসময় USER-role balance-এ যায়)
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
  v_user_has_role boolean; -- [ধাপ ১৪.৫খ] role-activation check
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

  -- [ধাপ ১৪.৫খ] refund সবসময় USER-role balance-এ যায় (escrow.user_id = problem poster) — p_role লাগবে না,
  -- টার্গেট structurally fixed। USER role deactivated (has_user_role=false) হলে refund ব্লক করা হয়।
  select has_user_role into v_user_has_role from public.users where id = v_escrow.user_id for update;
  if v_user_has_role is null then
    raise exception 'USER_NOT_FOUND';
  end if;
  if not v_user_has_role then
    raise exception 'USER_ROLE_INACTIVE';
  end if;

  v_amount := round((v_escrow.base_amount + v_escrow.extra_amount) * (p_refund_percentage / 100.0), 2);

  -- [ধাপ ১৪.৫খ] dual-write: পুরনো shared balance (Kotlin app এখনও এটাই পড়ে, rule #2) + নতুন balance_user
  update public.users set balance = balance + v_amount, balance_user = balance_user + v_amount, updated_at = v_now where id = v_escrow.user_id;

  -- [ধাপ ৩৬] transactions.role যোগ — role='USER'
  insert into public.transactions (id, problem_id, problem_title, user_id, solver_id, gross_amount,
    net_amount, type, escrow_id, refund_type, refund_percentage, role, "timestamp")
  values (v_trx_id, v_escrow.problem_id, v_escrow.problem_title, v_escrow.user_id, v_escrow.solver_id,
    v_amount, v_amount, 'REFUND', p_escrow_id, p_refund_type, p_refund_percentage, 'USER', v_now);

  update public.escrows set status = 'REFUNDED', released_at = v_now, updated_at = v_now where id = p_escrow_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_escrow.user_id,
    'রিফান্ড সম্পন্ন', 'আপনার ৳' || v_amount::text || ' রিফান্ড করা হয়েছে।', 'balance', v_escrow.user_id, v_now);

  return jsonb_build_object('result', 'OK', 'amount', v_amount);
end;
$function$
;

-- ৮. release_escrow — role='SOLVER' (ফাংশনের নিজস্ব কমেন্টেই বলা আছে: release সবসময় SOLVER-role balance-এ যায়)
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
  v_solver_has_role boolean; -- [ধাপ ১৪.৫খ] role-activation check
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

  -- [ধাপ ১৪.৫খ] escrow release সবসময় SOLVER-role balance-এ যায় (unambiguous — এই RPC-তে
  -- p_role প্যারামিটার যোগ করার দরকার নেই, টার্গেট role structurally fixed)। টার্গেট
  -- (escrow.solver_id) এর has_solver_role সক্রিয় কিনা যাচাই — deactivated role-এ payout আটকানো।
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

  -- [ধাপ ১৪.৫খ] dual-write: পুরনো shared balance কলাম (Kotlin app এখনও এটাই পড়ে, rule #2)
  -- এর পাশাপাশি নতুন role-scoped balance_solver কলামেও লেখা হচ্ছে। ১৪.৫ঘ-এর Kotlin-wiring
  -- এর পর balance_solver read-side-এ কাটওভার হলে dual-write সরানো যাবে।
  update public.users set balance = balance + v_net, balance_solver = balance_solver + v_net, updated_at = v_now where id = v_escrow.solver_id;

  -- [ধাপ ৩৬] transactions.role যোগ — role='SOLVER'
  insert into public.transactions (id, problem_id, problem_title, user_id, solver_id, gross_amount,
    commission_percent, commission_amount, net_amount, base_amount, extra_amount, type, escrow_id,
    release_type, role, "timestamp")
  values (v_trx_id, v_escrow.problem_id, v_escrow.problem_title, v_escrow.user_id, v_escrow.solver_id,
    v_gross, v_commission_rate, v_commission, v_net, v_escrow.base_amount, v_escrow.extra_amount,
    'PAYMENT', p_escrow_id, 'FULL', 'SOLVER', v_now);

  update public.escrows set status = 'RELEASED', released_at = v_now, updated_at = v_now where id = p_escrow_id;

  update public.problems set status = 'COMPLETED', completed_at = v_now, last_activity_at = v_now
  where id = v_escrow.problem_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_escrow.solver_id,
    'পেমেন্ট প্রকাশিত হয়েছে', 'আপনার ৳' || v_net::text || ' পেমেন্ট আপনার ব্যালেন্সে যোগ হয়েছে।', 'balance', v_escrow.solver_id, v_now);

  return jsonb_build_object('result', 'OK', 'net_amount', v_net, 'commission', v_commission);
end;
$function$
;

-- ৯. request_wallet_deposit — role = p_role (auto-approve path-এ transaction তৈরি হয়; pending path-এ হয় না,
-- সেটা পরে admin_confirm_gateway_deposit-এ তৈরি হয় #৪-এ)
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

    -- [ধাপ ১৪.৫গ] role-activation safety check — নিষ্ক্রিয় role-এ deposit রাউট করা বন্ধ
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

        -- [ধাপ ৩৬] transactions.role যোগ — role = p_role
        insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount,
            base_amount, type, escrow_id, role, "timestamp")
        values ('TRX_DEP_' || v_payment_id, '', 'ওয়ালেট রিচার্জ (' || p_gateway || ')',
            auth.uid(), p_amount, p_amount, p_amount, 'WALLET_DEPOSIT', v_payment_id, p_role, v_now);

        insert into public.notifications (id, user_id, title, message, target_type, target_id, "timestamp")
        values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), auth.uid(),
            'রিচার্জ সফল', '৳' || p_amount::text || ' আপনার ব্যালেন্সে যোগ হয়েছে।', 'balance', v_payment_id, v_now);

        return jsonb_build_object('result', 'OK', 'payment_id', v_payment_id);
    else
        insert into public.gateway_payments (id, gateway_trx_id, user_id, amount, gateway, purpose, status, note, role, "timestamp")
        values (v_payment_id, p_gateway_trx_id, auth.uid(), p_amount, p_gateway, 'WALLET_DEPOSIT', 'PENDING',
            coalesce(nullif(p_note, ''), 'ওয়ালেট রিচার্জ অনুরোধ — অ্যাডমিন যাচাইয়ের অপেক্ষায়') ||
            case when p_sender_phone <> '' then ' | প্রেরকের নম্বর: ' || p_sender_phone else '' end,
            p_role, v_now);

        insert into public.notifications (id, user_id, title, message, target_type, target_id, "timestamp")
        values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), auth.uid(),
            'রিচার্জ অনুরোধ জমা হয়েছে',
            '৳' || p_amount::text || ' রিচার্জের অনুরোধ জমা হয়েছে, অ্যাডমিন যাচাই করার পর ব্যালেন্সে যোগ হবে।',
            'balance', v_payment_id, v_now);

        return jsonb_build_object('result', 'PENDING_APPROVAL', 'payment_id', v_payment_id);
    end if;
end;
$function$
;

-- ১০. request_withdrawal — role = p_role (withdrawal-deduction transaction)
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
  -- [ধাপ ১৪.৫খ] p_role validate
  if p_role not in ('USER', 'SOLVER') then
    raise exception 'INVALID_ROLE';
  end if;

  select * into v_acct from public.users where id = auth.uid() for update;
  if not found then raise exception 'USER_NOT_FOUND'; end if;

  -- [ধাপ ১৪.৫খ] role-activation safety check — নিষ্ক্রিয় role-এর balance থেকে withdraw বন্ধ
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

  -- [ধাপ ১৪.৫খ] dual-write: পুরনো shared balance (অপরিবর্তিত আচরণ, rule #2) + role-scoped balance_user/balance_solver
  if p_role = 'SOLVER' then
    update public.users set balance = balance - p_amount, balance_solver = balance_solver - p_amount, updated_at = v_now where id = v_acct.id;
  else
    update public.users set balance = balance - p_amount, balance_user = balance_user - p_amount, updated_at = v_now where id = v_acct.id;
  end if;

  insert into public.withdrawals (id, solver_id, solver_name, amount, method, account_number, bank_name,
    branch_name, account_holder_name, status, role, created_at)
  values (v_withdraw_id, v_acct.id, v_acct.name, p_amount, p_method, p_account_number, p_bank_name,
    p_branch_name, p_account_holder_name, 'PENDING', p_role, v_now);

  -- [ধাপ ৩৬] transactions.role যোগ — role = p_role
  insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, escrow_id, role, "timestamp")
  values ('TRX_WD_DEDUCT_' || v_withdraw_id, '', 'উইথড্র আবেদন — ব্যালেন্স কর্তন', v_acct.id, p_amount, -p_amount,
    'WITHDRAWAL_DEDUCTION', v_withdraw_id, p_role, v_now);

  insert into public.notifications (id, user_id, title, message, target_type, target_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_acct.id, 'উইথড্র রিকোয়েস্ট জমা হয়েছে',
    '৳' || p_amount::text || ' উত্তোলনের আবেদন জমা হয়েছে (' || p_method || ': ' || p_account_number ||
    ', উইথড্র আইডি: ' || v_withdraw_id || ')। অ্যাডমিন অনুমোদন সাপেক্ষে টাকা পাঠানো হবে।', 'balance', v_acct.id, v_now);

  return jsonb_build_object('result', 'OK', 'withdrawal_id', v_withdraw_id);
end;
$function$
;

-- ১১. respond_additional_charge — role='USER' (ফাংশনের নিজস্ব কমেন্টেই বলা আছে: structurally USER role)
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

-- ১২. user_confirm_extra_amount — role='USER' (transaction problem owner/user-এর জন্য)
-- ⚠️ নোট: এই ফাংশন এখনও v_user.balance (shared) থেকে deduction হিসাব করে, balance_user থেকে না
-- (তার sibling respond_additional_charge()-এর মতো না) — এটা এই migration-এর স্কোপের বাইরে,
-- শুধু role ট্যাগিং করা হলো, ব্যালেন্স-হিসাব-লজিক অপরিবর্তিত।
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
    -- [ধাপ ৩৬] transactions.role যোগ — role='USER'
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
    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
      'অতিরিক্ত বিল অনুমোদিত ✅',
      'ক্লায়েন্ট আপনার ৳' || trim(to_char(v_amt, 'FM999999999')) || ' অতিরিক্ত বিলের অনুরোধ অনুমোদন করেছেন।',
      'problem', p_problem_id, p_problem_id, v_now);
  end if;

  return jsonb_build_object('result', 'OK', 'confirmed_amount', v_amt, 'wallet_deduction', v_wallet_deduction);
end;
$function$
;

-- নোট: নিচের RPC-গুলো public.transactions-এ কোনো row insert করে না, তাই এই migration-এ
-- স্পর্শ করা হয়নি (role কলামের সাথে সম্পর্কহীন):
--   - admin_refund_and_reopen_problem (শুধু bids/problems আপডেট করে)
--   - request_extra_amount (শুধু notification পাঠায়)
--   - user_reject_extra_amount (শুধু notification পাঠায়)
