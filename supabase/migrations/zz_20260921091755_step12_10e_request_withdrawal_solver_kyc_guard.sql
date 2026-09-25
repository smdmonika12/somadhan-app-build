-- ============================================================================
-- step12_10e_request_withdrawal_solver_kyc_guard   [Step 12.10e — migration-ফাইল-সিঙ্ক, Step 12.12]
-- ============================================================================
-- এটা live Supabase প্রজেক্টে (mghvvpndkxnscwryfkib) ইতিমধ্যে apply-করা migration
-- `20260921091755 step12_10e_request_withdrawal_solver_kyc_guard`-এর repo-কপি। live-ই সত্যের উৎস: নিচের SQL live pg_proc (MCP,
-- ২০২৬-০৯-২১) থেকে টেনে যাচাই করা — প্রতিটা ফাংশন-বডির md5(prosrc) live-এর সাথে বাইট-বাই-বাইট মিলেছে।
-- কী বদলায়: request_withdrawal(…8-arg): SOLVER-role withdrawal-এ users.is_kyc_verified না থাকলে KYC_REQUIRED (USER-role অপ্রভাবিত)।
-- live md5(prosrc) request_withdrawal(…8-arg): da8cb64bbca584c071ae81e01665b9dc
-- CREATE OR REPLACE-এ grants অপরিবর্তিত থাকে (live ACL: PUBLIC/anon/authenticated/service_role/postgres EXECUTE)।
-- ⚠️ ফাইলের নামের `zz_<live-version>_` prefix ইচ্ছাকৃত: CI/setup script `ls supabase/migrations/*.sql | sort`
--    (অক্ষরক্রম) অনুযায়ী apply করে, timestamp অনুযায়ী না — `step12_*` নাম `step36_*`-এর আগে বসত, আর তখন
--    step36-এর পুরনো বডি এই বদলকে fresh-DB-তে overwrite করত। `zz_` সবার শেষে বসায়, live-এর ক্রম বজায় থাকে।
-- ⚠️ live-এ এটা আবার চালানো নিরাপদ (CREATE OR REPLACE / DROP IF EXISTS) — কিন্তু দরকার নেই, live আগেই apply-করা।
-- ============================================================================
CREATE OR REPLACE FUNCTION public.request_withdrawal(p_amount numeric, p_method text, p_account_number text, p_bank_name text DEFAULT NULL::text, p_branch_name text DEFAULT NULL::text, p_account_holder_name text DEFAULT NULL::text, p_role text DEFAULT 'SOLVER'::text, p_client_withdrawal_id text DEFAULT NULL::text)
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
  v_withdraw_id text := coalesce(
    nullif(trim(p_client_withdrawal_id), ''),
    'WID-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8))
  );
  v_now timestamptz := now();
begin
  if p_role not in ('USER', 'SOLVER') then
    raise exception 'INVALID_ROLE';
  end if;

  if exists (select 1 from public.withdrawals where id = v_withdraw_id) then
    raise exception 'WITHDRAWAL_ID_COLLISION';
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

  -- [Step 12.10e] SOLVER-role withdrawal-এ KYC বাধ্যতামূলক (USER-role অপ্রভাবিত)। grandfather নয় —
  -- বিদ্যমান unverified solver-ও আটকাবে। admin_revoke_kyc পরে is_kyc_verified=false করলে এটা
  -- পরবর্তী withdrawal-এও স্বয়ংক্রিয়ভাবে প্রযোজ্য হবে (আলাদা কোনো কোড লাগে না)।
  if p_role = 'SOLVER' and not coalesce(v_acct.is_kyc_verified, false) then
    raise exception 'KYC_REQUIRED';
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

  insert into public.notifications (id, user_id, title, message, target_type, target_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_acct.id, 'উইথড্র রিকোয়েস্ট জমা হয়েছে',
    '৳' || p_amount::text || ' উত্তোলনের আবেদন জমা হয়েছে (' || p_method || ': ' || p_account_number ||
    ', উইথড্র আইডি: ' || v_withdraw_id || ')। অ্যাডমিন অনুমোদন সাপেক্ষে টাকা পাঠানো হবে।', 'balance', v_acct.id, v_now);

  return jsonb_build_object('result', 'OK', 'withdrawal_id', v_withdraw_id);
end;
$function$;
