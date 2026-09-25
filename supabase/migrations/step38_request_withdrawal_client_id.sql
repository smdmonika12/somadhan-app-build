-- [RPC_SYNC_FIX — Step 7 ব্যাচ ২ পরবর্তী ফিক্স] request_withdrawal-এ ঐচ্ছিক
-- p_client_withdrawal_id প্যারামিটার যোগ করা হলো।
--
-- সমস্যা যা ফিক্স করা হচ্ছে (RPC_SYNC_FIX_PROGRESS.md, Step 7 ব্যাচ ২ এন্ট্রি দ্রষ্টব্য):
-- আগে RPC নিজে থেকেই (server-side) v_withdraw_id বানাতো, client কোনো id পাঠাতে পারতো না।
-- Kotlin সাইড (SomadhanRepository.requestWithdrawal) RPC কল করার *আগেই* একটা local
-- withdrawId বানায় (best-effort dual-write প্যাটার্ন)। RPC তাৎক্ষণিকভাবে ব্যর্থ হলে outbox-এ
-- retry queue হতো, আর retry পরে সফল হলে সার্ভার নতুন id বানাতো যা local withdrawId-এর সাথে
-- মিলতো না — ফলে cloud-এ একটা "এতিম" row তৈরি হতো আর পরবর্তী process_withdrawal
-- (admin reject/complete) local id দিয়ে সেটা খুঁজে পেত না (WITHDRAWAL_NOT_FOUND)।
--
-- ফিক্স: এখন client (Kotlin) চাইলে তার নিজের local withdrawId পাঠাতে পারবে
-- (p_client_withdrawal_id) — দেওয়া হলে আর ফাঁকা না হলে RPC সেই id-ই ব্যবহার করবে, local আর
-- cloud সবসময় একই id শেয়ার করবে (তাৎক্ষণিক সাফল্য হোক বা পরে outbox retry-তে সাফল্য)।
-- প্যারামিটারটা DEFAULT NULL, তাই না পাঠালে আগের মতোই server-side random id জেনারেট হবে —
-- বিদ্যমান কোনো কলার/আচরণ ভাঙে না (pure additive)।
CREATE OR REPLACE FUNCTION public.request_withdrawal(
  p_amount numeric,
  p_method text,
  p_account_number text,
  p_bank_name text DEFAULT NULL::text,
  p_branch_name text DEFAULT NULL::text,
  p_account_holder_name text DEFAULT NULL::text,
  p_role text DEFAULT 'SOLVER'::text,
  p_client_withdrawal_id text DEFAULT NULL::text
)
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
  -- [ফিক্স] client id দেওয়া থাকলে (ফাঁকা স্ট্রিং না হলে) সেটাই ব্যবহার হবে, নাহলে আগের মতোই
  -- server-side random id।
  v_withdraw_id text := coalesce(
    nullif(trim(p_client_withdrawal_id), ''),
    'WID-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8))
  );
  v_now timestamptz := now();
begin
  -- [ধাপ ১৪.৫খ] p_role validate
  if p_role not in ('USER', 'SOLVER') then
    raise exception 'INVALID_ROLE';
  end if;

  -- [ফিক্স] id collision হলে (একই id আগে থেকেই আছে) স্পষ্ট এরর — সাইলেন্টলি ওভাররাইট না করে।
  -- বাস্তবে এটা ঘটার কথা না (client-generated id সবসময় random/UUID-based), কিন্তু
  -- ডিফেন্সিভভাবে চেক করা হলো যাতে insert-এর primary-key violation-এর বদলে একটা বোধগম্য
  -- error message পাওয়া যায়।
  if exists (select 1 from public.withdrawals where id = v_withdraw_id) then
    raise exception 'WITHDRAWAL_ID_COLLISION';
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
