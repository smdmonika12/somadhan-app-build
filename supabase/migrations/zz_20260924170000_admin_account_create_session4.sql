-- [ADMIN_ROLE_PROFILE সেশন ৪] এডমিন অ্যাকাউন্ট তৈরি + পাসওয়ার্ড রিসেট (সুপার-অনলি RPC)।
--
-- সেশন ১-এ list/set_role/set_active/set_flagged তৈরি ছিল; বাকি ছিল "নতুন এডমিন বানানো" — এটাই এই ফাইল।
-- সেশন ১-এর মন্তব্য অনুযায়ী প্রতিটা এডমিনের নিজস্ব Supabase Auth user লাগে (`admin_accounts.auth_user_id`),
-- আর বিদ্যমান সব RPC/RLS `is_admin(auth.uid())`-এর উপর দাঁড়িয়ে — যেখানে `public.users.role = 'ADMIN'` লাগে।
--
-- ⚙️ কীভাবে auth user বানানো হয় (কেন Edge Function না):
--   `step33_2_seed_real_admin_auth_account.sql`-এ সুপার অ্যাডমিনের যে auth.users/auth.identities রো হাতে সিড করা
--   হয়েছিল এবং লাইভে real sign-in দিয়ে কাজ করছে (সেশন ২ টেস্ট পাস), এই RPC হুবহু সেই রো-এর গঠন অনুকরণ করে
--   (লাইভ রো থেকে যাচাই করা, ২০২৬-০৯-২৪):
--     • auth.users.phone = `8801XXXXXXXXX` (E.164, '+' ছাড়া — GoTrue এভাবেই রাখে)
--     • টোকেন কলামগুলো NULL না, `''` (নইলে GoTrue লগইনে "Database error querying schema" দিতে পারে)
--     • confirmed_at generated কলাম — সেট করা হয় না
--     • auth.identities.provider_id = user id, identity_data = {sub, phone}
--   এতে সবটা একটা ট্রানজ্যাকশনে (অ্যাটমিক) — Edge Function হলে auth user তৈরি হওয়ার পর DB-ধাপ ব্যর্থ হলে
--   অনাথ auth user থেকে যেত। `on_auth_user_created` ট্রিগার (handle_new_auth_user) নিজেই public.users রো বানায়
--   (role='USER'), তারপর এই RPC সেটাকে 'ADMIN'-এ প্রমোট করে — সিডের ধাপ ২-এর মতোই।
--
-- 🔒 সুরক্ষা: (১) `_admin_require_super()` — সুপার ছাড়া কেউ ডাকতে পারে না; (২) সুপার রোল অ্যাসাইন নিষিদ্ধ;
--   (৩) যে ফোন আগে থেকেই কোনো সাধারণ ইউজার/সলভারের (auth.users বা public.users) — সেই ফোনে এডমিন বানানো যায় না
--   (`PHONE_IN_USE_BY_USER`) — নইলে কারো বিদ্যমান অ্যাকাউন্ট চুপচাপ এডমিনে রূপান্তর হয়ে যেত।
-- পাসওয়ার্ড কোথাও লগ/সংরক্ষণ হয় না (শুধু bcrypt হ্যাশ auth.users-এ)।

create or replace function public.admin_account_create(
  p_name text, p_phone text, p_password text, p_role_id text,
  p_designation text default ''::text, p_email text default null)
returns jsonb language plpgsql security definer set search_path to 'public' as $$
declare
  v_name text := trim(coalesce(p_name, ''));
  v_local text := public._admin_norm_phone(p_phone);      -- 01XXXXXXXXX
  v_auth_phone text;                                        -- 8801XXXXXXXXX
  v_designation text := trim(coalesce(p_designation, ''));
  v_email text := nullif(trim(coalesce(p_email, '')), '');
  v_role public.admin_roles;
  v_uid uuid := gen_random_uuid();
  v_creator uuid;
  v_id uuid;
begin
  perform public._admin_require_super();

  if v_name = '' or length(v_name) > 80 then raise exception 'INVALID_NAME'; end if;
  if v_local !~ '^01[0-9]{9}$' then raise exception 'INVALID_PHONE'; end if;
  -- ন্যূনতম ৮ অক্ষর (অ্যাপের বিদ্যমান এডমিন-পাসওয়ার্ড নিয়ম); bcrypt ৭২ বাইটের বেশি পড়ে না
  if length(coalesce(p_password, '')) < 8 or octet_length(p_password) > 72 then raise exception 'INVALID_PASSWORD'; end if;
  if length(v_designation) > 80 then raise exception 'INVALID_DESIGNATION'; end if;
  if v_email is not null and (length(v_email) > 120 or v_email !~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$') then
    raise exception 'INVALID_EMAIL';
  end if;

  select * into v_role from public.admin_roles where id = p_role_id;
  if not found then raise exception 'ROLE_NOT_FOUND'; end if;
  if v_role.is_super then raise exception 'CANNOT_ASSIGN_SUPER_ROLE'; end if;

  v_auth_phone := '88' || v_local;

  if exists (select 1 from public.admin_accounts a where public._admin_norm_phone(a.phone) = v_local) then
    raise exception 'ADMIN_PHONE_TAKEN';
  end if;
  if exists (select 1 from auth.users u where public._admin_norm_phone(u.phone) = v_local)
     or exists (select 1 from public.users x where public._admin_norm_phone(x.phone) = v_local) then
    raise exception 'PHONE_IN_USE_BY_USER';
  end if;

  begin
    insert into auth.users (
      id, instance_id, aud, role, phone, phone_confirmed_at,
      encrypted_password, raw_app_meta_data, raw_user_meta_data,
      created_at, updated_at,
      confirmation_token, recovery_token, email_change_token_new,
      email_change, phone_change, phone_change_token, email_change_token_current,
      reauthentication_token, is_sso_user, is_anonymous
    ) values (
      v_uid, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated',
      v_auth_phone, now(),
      extensions.crypt(p_password, extensions.gen_salt('bf')),
      '{"provider":"phone","providers":["phone"]}'::jsonb,
      jsonb_build_object('name', v_name),
      now(), now(),
      '', '', '', '', '', '', '', '', false, false
    );
    insert into auth.identities (provider_id, user_id, identity_data, provider, last_sign_in_at, created_at, updated_at)
    values (v_uid::text, v_uid, jsonb_build_object('sub', v_uid::text, 'phone', v_auth_phone), 'phone', now(), now(), now());
  exception when unique_violation then
    raise exception 'ADMIN_PHONE_TAKEN';
  end;

  -- handle_new_auth_user ট্রিগার public.users রো বানিয়েছে (role='USER'); এডমিনে প্রমোট (সিডের ধাপ ২-এর মতো)।
  update public.users
     set role = 'ADMIN', phone = v_local, email = v_email,
         latitude = 23.8103, longitude = 90.4125, address = 'ঢাকা, বাংলাদেশ',
         has_user_role = true, has_solver_role = false
   where id = v_uid;
  if not found then raise exception 'USER_ROW_MISSING'; end if;

  select id into v_creator from public.admin_accounts where auth_user_id = auth.uid();
  insert into public.admin_accounts (auth_user_id, name, designation, phone, email, role_id, created_by)
  values (v_uid, v_name, v_designation, v_local, v_email, p_role_id, v_creator)
  returning id into v_id;

  perform public.log_admin_action('ADMIN_CREATED', v_id::text, v_name, 'role=' || v_role.name, '');
  return public._admin_account_view(v_id);
end $$;

-- সুপার অ্যাডমিন অন্য এডমিনের (সুপার বাদে) পাসওয়ার্ড রিসেট করে — ভুলে গেলে/হারালে। সুপারের নিজের পাসওয়ার্ড
-- এখান থেকে বদলানো যায় না (সেটা সেটিংস/প্রোফাইলে, বর্তমান পাসওয়ার্ড যাচাইসহ)। রিসেটের পর পুরনো
-- পাসওয়ার্ডে খোলা সেশনগুলো বন্ধ করা হয় (best-effort — access-token মেয়াদ শেষ হওয়া পর্যন্ত ঘণ্টাখানেক টিকতে পারে)।
create or replace function public.admin_account_reset_password(p_id uuid, p_new_password text)
returns boolean language plpgsql security definer set search_path to 'public' as $$
declare v_a public.admin_accounts;
begin
  perform public._admin_require_super();
  select * into v_a from public.admin_accounts where id = p_id;
  if not found then raise exception 'ACCOUNT_NOT_FOUND'; end if;
  if (select is_super from public.admin_roles where id = v_a.role_id) then raise exception 'SUPER_ADMIN_IMMUTABLE'; end if;
  if v_a.auth_user_id is null then raise exception 'ACCOUNT_HAS_NO_LOGIN'; end if;
  if length(coalesce(p_new_password, '')) < 8 or octet_length(p_new_password) > 72 then raise exception 'INVALID_PASSWORD'; end if;

  update auth.users
     set encrypted_password = extensions.crypt(p_new_password, extensions.gen_salt('bf')), updated_at = now()
   where id = v_a.auth_user_id;

  begin
    delete from auth.sessions where user_id = v_a.auth_user_id;
  exception when undefined_table then
    null;
  end;
  update public.admin_sessions set ended_at = now() where admin_id = p_id and ended_at is null;

  perform public.log_admin_action('ADMIN_PASSWORD_RESET', p_id::text, v_a.name, '', '');
  return true;
end $$;

revoke execute on function
  public.admin_account_create(text, text, text, text, text, text),
  public.admin_account_reset_password(uuid, text)
  from public, anon;
grant execute on function
  public.admin_account_create(text, text, text, text, text, text),
  public.admin_account_reset_password(uuid, text)
  to authenticated, service_role;
