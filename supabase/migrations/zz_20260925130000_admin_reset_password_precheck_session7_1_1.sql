-- [ADMIN_ROLE_PROFILE সেশন ৭.১.১] `admin-reset-user-password` Edge Function-এর নিরাপত্তা-ফাঁক ফিক্স।
--
-- সমস্যা (মাস্টার প্রম্পট সেশন ৭, সাব-স্টেপ ৭.১.১-এ কনফার্মড): Edge Function এখন পর্যন্ত শুধু
-- `is_admin(caller)` চেক করে — caller *যেকোনো* সক্রিয় admin হলেই *যেকোনো* target_user_id-এর
-- পাসওয়ার্ড রিসেট করতে পারে, টার্গেট নিজে admin হোক বা caller-এর রোলে reset_password পারমিশন
-- না থাকুক বা caller flagged থাকুক — কিছুই চেক হয় না। Edge Function service_role key দিয়ে
-- সরাসরি কল করাও টেকনিক্যালি সম্ভব, তাই শুধু ক্লায়েন্ট-সাইড গেটের ওপর ভরসা করা যায় না।
--
-- সমাধান: একটাই নতুন SECURITY DEFINER RPC — `admin_reset_password_precheck(p_target_user_id)` —
-- যা caller-এর auth.uid() (auth.getUser() দিয়ে ভেরিফাইড, নিজে থেকে বসানো যায় না) নিয়ে পুরো
-- সিদ্ধান্তটা সার্ভার-সাইডে একবারেই নেয়:
--   ১) caller সক্রিয় admin না হলে → ADMIN_ONLY
--   ২) target_user_id কোনো admin identity হলে (admin_accounts.auth_user_id মিলে, বা
--      public.users.role='ADMIN' — যেকোনো একটা, active/flagged/inactive যাই হোক) এবং caller
--      সুপার-অ্যাডমিন না হলে → TARGET_IS_ADMIN (সাধারণ এডমিন কখনো অন্য কোনো এডমিনের —
--      এমনকি সুপারেরও — পাসওয়ার্ড রিসেট করতে পারবে না)
--   ৩) target সাধারণ ইউজার হলে caller-এর `users:users:reset_password` পারমিশন (রোল থেকে) +
--      flagged-না থাকা — `admin_can_act(...)`-এর মাধ্যমেই (সুপার এমনিতেই সবসময় true, flagged
--      non-super সবসময় false, নন-flagged non-super রোল-পারমিশন অনুযায়ী) → না থাকলে
--      PERMISSION_DENIED
--   ৪) বাকি সব ঠিক থাকলে {allowed: true}
--
-- Edge Function client-পাঠানো কোনো flag বিশ্বাস করে না — caller-identity JWT থেকে, target
-- admin কিনা DB নিজে চেক করে, পারমিশন রোল-টেবিল থেকে — client শুধু target_user_id/new_password
-- পাঠায়, বাকি পুরোটাই এই RPC-র ভেতরে।

create or replace function public.admin_reset_password_precheck(p_target_user_id uuid)
returns jsonb language plpgsql stable security definer set search_path to 'public' as $$
declare
  v_caller uuid := auth.uid();
  v_caller_super boolean;
  v_target_is_admin boolean;
begin
  if v_caller is null then
    return jsonb_build_object('allowed', false, 'reason', 'NOT_AUTHENTICATED');
  end if;
  if p_target_user_id is null then
    return jsonb_build_object('allowed', false, 'reason', 'INVALID_TARGET_USER_ID');
  end if;
  if not public.is_admin(v_caller) then
    return jsonb_build_object('allowed', false, 'reason', 'ADMIN_ONLY');
  end if;

  v_caller_super := public.is_super_admin(v_caller);

  -- টার্গেট কোনো admin identity কিনা — active/flagged/inactive নির্বিশেষে (দুই লেয়ার, historical
  -- ADMIN role/legacy সিডসহ কভার করতে): admin_accounts-এ auth_user_id মিলে, অথবা এখনো
  -- public.users.role='ADMIN' (যেমন admin_account_create-এ প্রমোট করা কোনো ইউজার)।
  v_target_is_admin := exists (
    select 1 from public.admin_accounts a where a.auth_user_id = p_target_user_id
  ) or exists (
    select 1 from public.users u where u.id = p_target_user_id and u.role = 'ADMIN'
  );

  if v_target_is_admin and not v_caller_super then
    return jsonb_build_object('allowed', false, 'reason', 'TARGET_IS_ADMIN');
  end if;

  if not v_target_is_admin and not public.admin_can_act('users:users:reset_password', v_caller) then
    return jsonb_build_object('allowed', false, 'reason', 'PERMISSION_DENIED');
  end if;

  return jsonb_build_object('allowed', true, 'reason', null);
end $$;

revoke execute on function public.admin_reset_password_precheck(uuid) from public, anon;
grant execute on function public.admin_reset_password_precheck(uuid) to authenticated, service_role;
