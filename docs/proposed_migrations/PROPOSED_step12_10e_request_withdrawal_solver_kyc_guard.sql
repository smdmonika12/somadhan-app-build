-- ============================================================================
-- PROPOSED (Step 12.10e) — request_withdrawal-এ SOLVER-role KYC বাধ্যতামূলক  [২০২৬-০৯-২১, live বডির ওপর]
-- ⚠️ apply হয়নি (ব্যবহারকারীর স্পষ্ট "apply koro" আসেনি)। live md5 (apply-এর আগে): c81f9815f49a2af81fac1d4933ff19cf, len 3541
--   (repo `supabase/migrations/step38_request_withdrawal_client_id.sql`-এর সাথে বাইট-বাই-বাইট মিলেছে — drift নেই, তাই এই
--   ফাইলটাই ভিত্তি হিসেবে ব্যবহার করা নিরাপদ।)
--   select md5(prosrc), length(prosrc), (prosrc ilike '%KYC_REQUIRED%')
--   from pg_proc where oid = 'public.request_withdrawal(numeric,text,text,text,text,text,text,text)'::regprocedure;
--   -- apply-এর ঠিক আগে আবার চালাও — মিলতে হবে: c81f9815…19cf | 3541 | false। না মিললে থামো, বডি আবার টেনে diff করো।
--
-- সিদ্ধান্ত (ব্যবহারকারী, ২০২৬-০৯-২১, Q5 — CI_TEST_SUITE_PROGRESS.md "Step 12.10 সম্পূর্ণ" সেকশন):
--   KYC verified বাধ্যতামূলক হবে **শুধু SOLVER-role** withdrawal-এ (USER-role-এ না)। unverified solver-কে
--   আটকাবে — grandfather নয় (বিদ্যমান অ্যাকাউন্টও আটকাবে)। live-এ যাচাই করা হয়েছে: এই মুহূর্তে unverified
--   solver যার balance_solver > 0, এমন অ্যাকাউন্ট **০টা** (মোট ৩ solver, সবাই verified) — তাই grandfather
--   বনাম না-grandfather-এর ব্যবহারিক পার্থক্য এখন নেই, কিন্তু নীতিটা ভবিষ্যতের জন্য প্রযোজ্য থাকবে।
--
-- prerequisite (আগেই সমাধা, Step 12.10d): user-role withdraw ভুলভাবে সবসময় SOLVER-রুটে (balance_solver
--   থেকে) যেত — dual-role অ্যাকাউন্টে এই KYC-গেট বসালে সেটা ভুলভাবে user-withdraw-ও আটকে দিত। 12.10d-তে
--   `requestWithdrawal(role=...)` প্যারামিটার + `UserWithdrawScreen`-এ `role="USER"` ফিক্স হয়ে গেছে (Windows-verified),
--   তাই এখন p_role সঠিকভাবে ভিন্ন করা যায় — এই গেট নিরাপদে শুধু SOLVER-এ প্রযোজ্য হবে।
--
-- যোগ হচ্ছে (live বডির বাকি সব হুবহু, নিচে ROLE_INACTIVE-চেকের ঠিক পরে একটা নতুন if-ব্লক):
--   p_role = 'SOLVER' and not coalesce(v_acct.is_kyc_verified, false) → raise 'KYC_REQUIRED'
--   (users.is_kyc_verified boolean কলাম — live-এ confirm করা হয়েছে; kyc_status টেক্সট-এ না, বুলিয়ানেই গেট)।
--
-- admin_revoke_kyc (live md5 3215dd9286bc2bdfaee10dcbbd59bec1) ইতিমধ্যেই `is_kyc_verified = false` সেট করে —
--   তাই এই migration apply হলে "revoke করলে জমা ব্যালেন্সও (future withdrawal) আটকাবে" শর্তটা admin_revoke_kyc
--   বদলানো ছাড়াই স্বয়ংক্রিয়ভাবে পূরণ হয় (পরের withdrawal call একই is_kyc_verified কলাম পড়ে)। admin_revoke_kyc
--   ছোঁয়া হয়নি।
--
-- ROLLBACK: নতুন if-ব্লকটা মুছে আবার create or replace (= md5 c81f9815…19cf)। grants CREATE OR REPLACE-এ
--   অপরিবর্তিত থাকে (anon EXECUTE নেই এই ফাংশনে — শুধু authenticated/postgres/service_role, live-এ যাচাই করা,
--   তাই এই ফাংশনে NULL-uid/anon ফাঁক প্রযোজ্যই না, আলাদা guard লাগবে না)।
-- ============================================================================
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
