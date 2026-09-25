-- [ADMIN_ROLE_PROFILE সেশন ১] মাল্টি-এডমিন রোল/অ্যাকাউন্ট/সেশন স্কিমা + DB-লেভেল গার্ড + RPC।
--
-- ⚠️ মাস্টার প্রম্পটের প্রস্তাবিত স্কিমা থেকে ইচ্ছাকৃত ৩টা পরিবর্তন (কারণসহ, PROGRESS ফাইলেও লেখা):
--   (১) `admin_accounts`-এ `password_hash` নেই — পরিবর্তে `auth_user_id` (Supabase Auth user-এর সাথে ১:১)।
--       কারণ: বিদ্যমান সব admin RPC/RLS `is_admin(auth.uid())`-এর উপর দাঁড়িয়ে (= আসল Supabase Auth
--       সেশন লাগে)। প্রতিটা এডমিনের নিজস্ব auth user থাকলে সার্ভার নিজেই জানে "কে কল করছে" —
--       ক্লায়েন্ট-সাপ্লাইড p_admin_id/p_admin_name spoof করা যেত। ক্রেডেনশিয়াল একটাই জায়গায় (auth.users)।
--   (২) `log_admin_action`-এর signature বদলায়নি — admin identity এখন auth.uid() থেকে সার্ভার-সাইডে
--       নেওয়া হয়। ফলে বিদ্যমান ১৫+ কল-সাইট আপডেট করতে হবে না, নতুন overload-ও নেই।
--   (৩) `is_admin()` এখন নিষ্ক্রিয় (active=false) এডমিনের জন্য false দেয় — ডিঅ্যাক্টিভেশন শুধু UI না,
--       সব RPC/RLS-এ সাথে সাথে কার্যকর।
-- বিদ্যমান `admin_credentials` টেবিল/RPC এই সেশনে অপরিবর্তিত (লগইন প্রতিস্থাপন সেশন ২-এ)।
-- এডমিন তৈরির RPC (auth.users সহ) সেশন ৪-এ, real sign-in দিয়ে যাচাই করে।

-- ───────────── ১) admin_roles ─────────────
create table if not exists public.admin_roles (
  id           text primary key,
  name         text not null,
  is_super     boolean not null default false,
  permissions  jsonb not null default '[]'::jsonb,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now(),
  constraint admin_roles_permissions_is_array check (jsonb_typeof(permissions) = 'array')
);
create unique index if not exists admin_roles_single_super on public.admin_roles (is_super) where is_super;
create unique index if not exists admin_roles_name_unique on public.admin_roles (lower(name));

insert into public.admin_roles (id, name, is_super, permissions)
values ('super', 'সুপার অ্যাডমিন', true, '[]'::jsonb)
on conflict (id) do nothing;

-- ───────────── ২) admin_accounts ─────────────
create table if not exists public.admin_accounts (
  id                 uuid primary key default gen_random_uuid(),
  auth_user_id       uuid unique references auth.users(id) on delete set null,
  name               text not null,
  designation        text not null default '',
  phone              text not null,
  email              text,
  photo_url          text,
  bio                text not null default '',
  role_id            text not null references public.admin_roles(id) on delete restrict,
  active             boolean not null default true,
  flagged            boolean not null default false,
  flagged_at         timestamptz,
  flagged_by         uuid references public.admin_accounts(id) on delete set null,
  last_login_at      timestamptz,
  last_login_device  text,
  last_login_ip      text,
  last_seen_at       timestamptz,
  created_by         uuid references public.admin_accounts(id) on delete set null,
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now()
);
create unique index if not exists admin_accounts_phone_unique on public.admin_accounts (phone);
create index if not exists admin_accounts_role_idx on public.admin_accounts (role_id);

-- ───────────── ৩) admin_sessions ─────────────
create table if not exists public.admin_sessions (
  id            uuid primary key default gen_random_uuid(),
  admin_id      uuid not null references public.admin_accounts(id) on delete cascade,
  device        text not null default '',
  ip            text not null default '',
  location      text not null default '',
  logged_in_at  timestamptz not null default now(),
  last_seen_at  timestamptz not null default now(),
  ended_at      timestamptz
);
create index if not exists admin_sessions_admin_idx on public.admin_sessions (admin_id, logged_in_at desc);

-- ───────────── ৪) DB-লেভেল গার্ড: সুপার অ্যাডমিন অপরিবর্তনীয় (client bug / RPC misuse হলেও) ─────────────
create or replace function public.admin_roles_guard()
returns trigger language plpgsql set search_path to 'public' as $$
begin
  if tg_op = 'DELETE' then
    if old.is_super then raise exception 'SUPER_ROLE_IMMUTABLE'; end if;
    return old;
  end if;
  if old.is_super and (new.is_super is distinct from old.is_super or new.id is distinct from old.id
                       or new.permissions is distinct from old.permissions) then
    raise exception 'SUPER_ROLE_IMMUTABLE';
  end if;
  new.updated_at := now();
  return new;
end $$;
drop trigger if exists trg_admin_roles_guard on public.admin_roles;
create trigger trg_admin_roles_guard before update or delete on public.admin_roles
  for each row execute function public.admin_roles_guard();

create or replace function public.admin_accounts_guard()
returns trigger language plpgsql set search_path to 'public' as $$
declare v_old_super boolean;
begin
  select coalesce((select r.is_super from public.admin_roles r where r.id = old.role_id), false) into v_old_super;
  if tg_op = 'DELETE' then
    if v_old_super then raise exception 'SUPER_ADMIN_IMMUTABLE'; end if;
    return old;
  end if;
  if v_old_super and (new.active = false or new.flagged = true or new.role_id is distinct from old.role_id) then
    raise exception 'SUPER_ADMIN_IMMUTABLE';
  end if;
  new.updated_at := now();
  return new;
end $$;
drop trigger if exists trg_admin_accounts_guard on public.admin_accounts;
create trigger trg_admin_accounts_guard before update or delete on public.admin_accounts
  for each row execute function public.admin_accounts_guard();

-- ───────────── ৫) is_admin(): নিষ্ক্রিয় এডমিন → false ─────────────
-- লাইভে param নাম `uid`, CI stub-এ `p_user_id` — CREATE OR REPLACE-এ নাম বদলানো যায় না, তাই
-- বিদ্যমান নামটাই পড়ে নিয়ে সেটা দিয়েই বানানো হচ্ছে (দুই পরিবেশেই কাজ করে)।
-- admin_accounts-এ row না থাকলে (যেমন CI) আগের আচরণই বহাল: role='ADMIN' হলেই true।
do $$
declare v_arg text;
begin
  select coalesce((p.proargnames)[1], 'uid') into v_arg
  from pg_proc p where p.oid = 'public.is_admin(uuid)'::regprocedure;
  execute format($f$
    create or replace function public.is_admin(%1$I uuid)
    returns boolean language sql stable security definer set search_path to 'public' as $b$
      select exists (select 1 from public.users u where u.id = %1$I and u.role = 'ADMIN')
        and not exists (select 1 from public.admin_accounts a where a.auth_user_id = %1$I and a.active = false);
    $b$
  $f$, v_arg);
end $$;

-- ───────────── ৬) সহায়ক ফাংশন ─────────────
create or replace function public.is_super_admin(p_uid uuid default auth.uid())
returns boolean language sql stable security definer set search_path to 'public' as $$
  select p_uid is not null and public.is_admin(p_uid) and exists (
    select 1 from public.admin_accounts a join public.admin_roles r on r.id = a.role_id
    where a.auth_user_id = p_uid and a.active and r.is_super);
$$;

-- সার্ভার-সাইড পারমিশন চেক (সেশন ৭-এ ঐচ্ছিক hardening-এর জন্য প্রস্তুত; এখনো কোনো বিদ্যমান RPC এটা ডাকে না)।
-- view-key (`:view` দিয়ে শেষ) → flagged হলেও চলে; অন্য সব অ্যাকশন → flagged হলে false।
create or replace function public.admin_can_act(p_action_key text, p_uid uuid default auth.uid())
returns boolean language sql stable security definer set search_path to 'public' as $$
  select coalesce((
    select case
      when not a.active then false
      when r.is_super then true
      when p_action_key like '%:view' then r.permissions ? p_action_key
      else (not a.flagged) and r.permissions ? p_action_key
    end
    from public.admin_accounts a join public.admin_roles r on r.id = a.role_id
    where a.auth_user_id = p_uid), false);
$$;

create or replace function public._admin_require_super()
returns void language plpgsql stable security definer set search_path to 'public' as $$
begin
  if auth.uid() is null then raise exception 'AUTH_REQUIRED'; end if;
  if not public.is_super_admin(auth.uid()) then raise exception 'SUPER_ADMIN_REQUIRED'; end if;
end $$;

create or replace function public._admin_account_view(p_id uuid)
returns jsonb language sql stable security definer set search_path to 'public' as $$
  select jsonb_build_object(
    'id', a.id, 'name', a.name, 'designation', a.designation, 'phone', a.phone, 'email', a.email,
    'photo_url', a.photo_url, 'bio', a.bio, 'role_id', a.role_id, 'role_name', r.name,
    'is_super', r.is_super, 'permissions', r.permissions, 'active', a.active, 'flagged', a.flagged,
    'flagged_at', a.flagged_at, 'last_login_at', a.last_login_at, 'last_login_device', a.last_login_device,
    'last_login_ip', a.last_login_ip, 'last_seen_at', a.last_seen_at, 'created_at', a.created_at,
    'is_online', exists (select 1 from public.admin_sessions s
                         where s.admin_id = a.id and s.ended_at is null
                           and s.last_seen_at > now() - interval '90 seconds'))
  from public.admin_accounts a join public.admin_roles r on r.id = a.role_id
  where a.id = p_id;
$$;

-- ───────────── ৭) audit log: এডমিন আইডেন্টিটি কলাম + log_admin_action (signature অপরিবর্তিত) ─────────────
-- ⚠️ `role` কলাম = user/solver role (পুরনো, সাংঘর্ষিক নাম) — এডমিনের রোলের নাম আলাদা `admin_role_name`-এ।
-- ২০২৬-০৯-২৪-এর আগের লগে admin_id/admin_name ফাঁকা থাকবে (তখন একটাই shared admin ছিল, পরিচয় রেকর্ড হয়নি) —
-- ইচ্ছাকৃতভাবে backfill করা হয়নি, UI "লিগ্যাসি" দেখাবে।
alter table public.admin_audit_logs add column if not exists admin_id uuid references public.admin_accounts(id) on delete set null;
alter table public.admin_audit_logs add column if not exists admin_name text not null default '';
alter table public.admin_audit_logs add column if not exists admin_role_name text not null default '';
create index if not exists admin_audit_logs_admin_idx on public.admin_audit_logs (admin_id, "timestamp" desc);

create or replace function public.log_admin_action(
  p_action_type text, p_target_id text default ''::text, p_target_name text default ''::text,
  p_details text default ''::text, p_role text default ''::text)
returns jsonb language plpgsql security definer set search_path to 'public' as $$
declare
  v_id text := 'LOG_' || (extract(epoch from now())::bigint)::text || '_' || substr(replace(gen_random_uuid()::text,'-',''),1,6);
  v_admin_id uuid; v_admin_name text := ''; v_role_name text := '';
begin
  if auth.uid() is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if p_action_type is null or trim(p_action_type) = '' then
    raise exception 'ACTION_TYPE_REQUIRED';
  end if;

  select a.id, a.name, r.name into v_admin_id, v_admin_name, v_role_name
  from public.admin_accounts a join public.admin_roles r on r.id = a.role_id
  where a.auth_user_id = auth.uid();

  insert into public.admin_audit_logs (id, action_type, target_id, target_name, details, role, "timestamp",
                                       admin_id, admin_name, admin_role_name)
  values (v_id, p_action_type, coalesce(p_target_id, ''), coalesce(p_target_name, ''), coalesce(p_details, ''),
          coalesce(p_role, ''), now(), v_admin_id, coalesce(v_admin_name, ''), coalesce(v_role_name, ''));

  return jsonb_build_object('result', 'OK', 'id', v_id);
end;
$$;

-- ───────────── ৮) RLS (লেখা শুধু RPC দিয়ে; সরাসরি টেবিল-রাইট বন্ধ) ─────────────
alter table public.admin_roles enable row level security;
alter table public.admin_accounts enable row level security;
alter table public.admin_sessions enable row level security;
revoke all on public.admin_roles, public.admin_accounts, public.admin_sessions from anon, authenticated;
grant select on public.admin_roles, public.admin_accounts, public.admin_sessions to authenticated;

drop policy if exists admin_roles_select on public.admin_roles;
create policy admin_roles_select on public.admin_roles for select to authenticated
  using (public.is_super_admin(auth.uid())
         or id = (select a.role_id from public.admin_accounts a where a.auth_user_id = auth.uid()));
drop policy if exists admin_accounts_select on public.admin_accounts;
create policy admin_accounts_select on public.admin_accounts for select to authenticated
  using (public.is_super_admin(auth.uid()) or auth_user_id = auth.uid());
drop policy if exists admin_sessions_select on public.admin_sessions;
create policy admin_sessions_select on public.admin_sessions for select to authenticated
  using (public.is_super_admin(auth.uid())
         or admin_id = (select a.id from public.admin_accounts a where a.auth_user_id = auth.uid()));

-- ───────────── ৯) সুপার অ্যাডমিন সিড (বিদ্যমান একমাত্র ADMIN user থেকে; কেউ লকড-আউট হবে না) ─────────────
-- dynamic SQL: CI stub-এ users.email / admin_credentials নাও থাকতে পারে, আর সেখানে কোনো ADMIN user নেই।
do $$
begin
  if not exists (select 1 from public.admin_accounts) then
    execute $q$
      insert into public.admin_accounts (auth_user_id, name, designation, phone, email, role_id)
      select u.id,
             coalesce(nullif(u.name, ''), 'Super Admin'),
             'সুপার অ্যাডমিন',
             coalesce(nullif(u.phone, ''), 'admin-' || substr(u.id::text, 1, 8)),
             nullif(to_jsonb(u) ->> 'email', ''),
             'super'
      from public.users u
      where u.role = 'ADMIN' and exists (select 1 from auth.users au where au.id = u.id)
      order by u.created_at nulls last, u.id
      limit 1
    $q$;
  end if;
exception when undefined_column then
  -- CI stub-এ users.created_at না থাকলে — সিড ছাড়াই এগোয় (CI-তে ADMIN user থাকে না)
  null;
end $$;
-- আসল admin_credentials-এর ফোন (আছে ধরে নিয়ে, না থাকলে ইগনোর) — ডিসপ্লে ফোন সঠিক রাখতে
do $$
begin
  if to_regclass('public.admin_credentials') is not null then
    execute $q$
      update public.admin_accounts a set phone = c.phone
      from public.admin_credentials c
      where c.id = 1 and coalesce(c.phone, '') <> '' and a.role_id = 'super'
        and not exists (select 1 from public.admin_accounts x where x.phone = c.phone and x.id <> a.id)
    $q$;
  end if;
end $$;

-- ───────────── ১০) RPC ─────────────
-- (ক) নিজের অ্যাকাউন্ট/সেশন
create or replace function public.admin_me()
returns jsonb language plpgsql stable security definer set search_path to 'public' as $$
declare v_a public.admin_accounts;
begin
  if auth.uid() is null then raise exception 'AUTH_REQUIRED'; end if;
  select * into v_a from public.admin_accounts where auth_user_id = auth.uid();
  if not found then raise exception 'NOT_AN_ADMIN'; end if;
  if not v_a.active then raise exception 'ACCOUNT_INACTIVE'; end if;
  return public._admin_account_view(v_a.id);
end $$;

create or replace function public.admin_session_start(p_device text default '', p_ip text default '', p_location text default '')
returns jsonb language plpgsql security definer set search_path to 'public' as $$
declare v_a public.admin_accounts; v_sid uuid; v_ip text;
begin
  if auth.uid() is null then raise exception 'AUTH_REQUIRED'; end if;
  select * into v_a from public.admin_accounts where auth_user_id = auth.uid();
  if not found then raise exception 'NOT_AN_ADMIN'; end if;
  if not v_a.active then raise exception 'ACCOUNT_INACTIVE'; end if;
  -- IP: প্রক্সির x-forwarded-for আগে (spoof-প্রতিরোধ), না পেলে ক্লায়েন্টের দেওয়া মান
  v_ip := coalesce(nullif(trim(split_part(coalesce((nullif(current_setting('request.headers', true), '')::json) ->> 'x-forwarded-for', ''), ',', 1)), ''),
                   nullif(trim(coalesce(p_ip, '')), ''), '');
  insert into public.admin_sessions (admin_id, device, ip, location)
  values (v_a.id, coalesce(p_device, ''), v_ip, coalesce(p_location, ''))
  returning id into v_sid;
  update public.admin_accounts
     set last_login_at = now(), last_login_device = coalesce(p_device, ''), last_login_ip = v_ip, last_seen_at = now()
   where id = v_a.id;
  perform public.log_admin_action('ADMIN_LOGIN', v_a.id::text, v_a.name, coalesce(p_device, ''), '');
  return jsonb_build_object('session_id', v_sid, 'account', public._admin_account_view(v_a.id));
end $$;

-- ক্লায়েন্ট ~৩০সে পরপর ডাকবে। ফেরত মান = সর্বশেষ active/flagged/রোল/পারমিশন → রোল বদলালে বা
-- ডিঅ্যাক্টিভেট/ফ্ল্যাগ করলে ক্লায়েন্ট রিলগইন ছাড়াই ধরতে পারবে। ডিঅ্যাক্টিভ হলে ACCOUNT_INACTIVE raise।
create or replace function public.admin_heartbeat(p_session_id uuid default null)
returns jsonb language plpgsql security definer set search_path to 'public' as $$
declare v_a public.admin_accounts;
begin
  if auth.uid() is null then raise exception 'AUTH_REQUIRED'; end if;
  select * into v_a from public.admin_accounts where auth_user_id = auth.uid();
  if not found then raise exception 'NOT_AN_ADMIN'; end if;
  if not v_a.active then raise exception 'ACCOUNT_INACTIVE'; end if;
  update public.admin_accounts set last_seen_at = now() where id = v_a.id;
  if p_session_id is not null then
    update public.admin_sessions set last_seen_at = now()
     where id = p_session_id and admin_id = v_a.id and ended_at is null;
  end if;
  return public._admin_account_view(v_a.id);
end $$;

-- p_session_id null হলে নিজের সব খোলা সেশন শেষ (লগআউট)
create or replace function public.admin_session_end(p_session_id uuid default null)
returns boolean language plpgsql security definer set search_path to 'public' as $$
declare v_id uuid;
begin
  if auth.uid() is null then raise exception 'AUTH_REQUIRED'; end if;
  select id into v_id from public.admin_accounts where auth_user_id = auth.uid();
  if v_id is null then raise exception 'NOT_AN_ADMIN'; end if;
  update public.admin_sessions set ended_at = now()
   where admin_id = v_id and ended_at is null and (p_session_id is null or id = p_session_id);
  return true;
end $$;

-- নিজের (বা সুপার হলে যেকারো) প্রোফাইল; null প্যারামিটার = অপরিবর্তিত। ফোন এখান থেকে বদলানো যায় না।
create or replace function public.admin_profile_update(
  p_id uuid default null, p_name text default null, p_designation text default null,
  p_email text default null, p_bio text default null, p_photo_url text default null)
returns jsonb language plpgsql security definer set search_path to 'public' as $$
declare v_me public.admin_accounts; v_target uuid;
begin
  if auth.uid() is null then raise exception 'AUTH_REQUIRED'; end if;
  select * into v_me from public.admin_accounts where auth_user_id = auth.uid();
  if not found then raise exception 'NOT_AN_ADMIN'; end if;
  if not v_me.active then raise exception 'ACCOUNT_INACTIVE'; end if;
  v_target := coalesce(p_id, v_me.id);
  if v_target <> v_me.id then perform public._admin_require_super(); end if;
  if not exists (select 1 from public.admin_accounts where id = v_target) then raise exception 'ACCOUNT_NOT_FOUND'; end if;
  if p_name is not null and (trim(p_name) = '' or length(p_name) > 80) then raise exception 'INVALID_NAME'; end if;
  if p_designation is not null and length(p_designation) > 80 then raise exception 'INVALID_DESIGNATION'; end if;
  if p_email is not null and length(p_email) > 120 then raise exception 'INVALID_EMAIL'; end if;
  if p_bio is not null and length(p_bio) > 500 then raise exception 'INVALID_BIO'; end if;
  update public.admin_accounts set
    name = coalesce(trim(p_name), name),
    designation = coalesce(trim(p_designation), designation),
    email = case when p_email is null then email else nullif(trim(p_email), '') end,
    bio = coalesce(p_bio, bio),
    photo_url = case when p_photo_url is null then photo_url else nullif(trim(p_photo_url), '') end
  where id = v_target;
  if v_target <> v_me.id then
    perform public.log_admin_action('ADMIN_PROFILE_UPDATED', v_target::text,
      (select name from public.admin_accounts where id = v_target), '', '');
  end if;
  return public._admin_account_view(v_target);
end $$;

-- (খ) রোল ম্যানেজমেন্ট — সুপার-অনলি
create or replace function public.admin_roles_list()
returns jsonb language plpgsql stable security definer set search_path to 'public' as $$
begin
  perform public._admin_require_super();
  return coalesce((
    select jsonb_agg(jsonb_build_object(
      'id', r.id, 'name', r.name, 'is_super', r.is_super, 'permissions', r.permissions,
      'account_count', (select count(*) from public.admin_accounts a where a.role_id = r.id),
      'created_at', r.created_at, 'updated_at', r.updated_at)
      order by r.is_super desc, r.created_at)
    from public.admin_roles r), '[]'::jsonb);
end $$;

create or replace function public.admin_role_upsert(p_id text, p_name text, p_permissions jsonb default '[]'::jsonb)
returns jsonb language plpgsql security definer set search_path to 'public' as $$
declare v_id text := nullif(trim(coalesce(p_id, '')), ''); v_name text := trim(coalesce(p_name, '')); v_perms jsonb; v_new boolean := false;
begin
  perform public._admin_require_super();
  if v_name = '' or length(v_name) > 80 then raise exception 'ROLE_NAME_REQUIRED'; end if;
  if p_permissions is null or jsonb_typeof(p_permissions) <> 'array' then raise exception 'PERMISSIONS_MUST_BE_ARRAY'; end if;
  if exists (select 1 from jsonb_array_elements(p_permissions) e where jsonb_typeof(e) <> 'string') then
    raise exception 'PERMISSIONS_MUST_BE_STRINGS';
  end if;
  select coalesce(jsonb_agg(x order by x), '[]'::jsonb) into v_perms
  from (select distinct jsonb_array_elements_text(p_permissions) as x) s;
  if exists (select 1 from public.admin_roles where lower(name) = lower(v_name) and id is distinct from v_id) then
    raise exception 'ROLE_NAME_TAKEN';
  end if;
  if v_id is null then
    v_new := true;
    v_id := 'role_' || substr(replace(gen_random_uuid()::text, '-', ''), 1, 10);
    insert into public.admin_roles (id, name, permissions) values (v_id, v_name, v_perms);
  else
    if not exists (select 1 from public.admin_roles where id = v_id) then raise exception 'ROLE_NOT_FOUND'; end if;
    if (select is_super from public.admin_roles where id = v_id) then raise exception 'SUPER_ROLE_IMMUTABLE'; end if;
    update public.admin_roles set name = v_name, permissions = v_perms where id = v_id;
  end if;
  perform public.log_admin_action(case when v_new then 'ROLE_CREATED' else 'ROLE_UPDATED' end, v_id, v_name,
                                  'permissions=' || jsonb_array_length(v_perms), '');
  return (select jsonb_build_object('id', r.id, 'name', r.name, 'is_super', r.is_super, 'permissions', r.permissions,
                                    'account_count', (select count(*) from public.admin_accounts a where a.role_id = r.id),
                                    'created_at', r.created_at, 'updated_at', r.updated_at)
          from public.admin_roles r where r.id = v_id);
end $$;

create or replace function public.admin_role_delete(p_id text)
returns boolean language plpgsql security definer set search_path to 'public' as $$
declare v_role public.admin_roles;
begin
  perform public._admin_require_super();
  select * into v_role from public.admin_roles where id = p_id;
  if not found then raise exception 'ROLE_NOT_FOUND'; end if;
  if v_role.is_super then raise exception 'SUPER_ROLE_IMMUTABLE'; end if;
  if exists (select 1 from public.admin_accounts where role_id = p_id) then raise exception 'ROLE_IN_USE'; end if;
  delete from public.admin_roles where id = p_id;
  perform public.log_admin_action('ROLE_DELETED', p_id, v_role.name, '', '');
  return true;
end $$;

-- (গ) এডমিন অ্যাকাউন্ট — সুপার-অনলি
create or replace function public.admin_accounts_list()
returns jsonb language plpgsql stable security definer set search_path to 'public' as $$
begin
  perform public._admin_require_super();
  return coalesce((select jsonb_agg(public._admin_account_view(a.id) order by a.created_at) from public.admin_accounts a), '[]'::jsonb);
end $$;

-- সুপার রোল অ্যাসাইন করা যায় না (ইচ্ছাকৃত — সুপার শুধু সিডেড অ্যাকাউন্ট); সুপার অ্যাকাউন্টের রোল বদলানোও যায় না
create or replace function public.admin_account_set_role(p_id uuid, p_role_id text)
returns jsonb language plpgsql security definer set search_path to 'public' as $$
declare v_a public.admin_accounts; v_role public.admin_roles;
begin
  perform public._admin_require_super();
  select * into v_a from public.admin_accounts where id = p_id;
  if not found then raise exception 'ACCOUNT_NOT_FOUND'; end if;
  select * into v_role from public.admin_roles where id = p_role_id;
  if not found then raise exception 'ROLE_NOT_FOUND'; end if;
  if v_role.is_super then raise exception 'CANNOT_ASSIGN_SUPER_ROLE'; end if;
  if (select is_super from public.admin_roles where id = v_a.role_id) then raise exception 'SUPER_ADMIN_IMMUTABLE'; end if;
  update public.admin_accounts set role_id = p_role_id where id = p_id;
  perform public.log_admin_action('ADMIN_ROLE_CHANGED', p_id::text, v_a.name, 'role=' || v_role.name, '');
  return public._admin_account_view(p_id);
end $$;

create or replace function public.admin_account_set_active(p_id uuid, p_active boolean, p_reason text default '')
returns jsonb language plpgsql security definer set search_path to 'public' as $$
declare v_a public.admin_accounts; v_me uuid;
begin
  perform public._admin_require_super();
  select id into v_me from public.admin_accounts where auth_user_id = auth.uid();
  select * into v_a from public.admin_accounts where id = p_id;
  if not found then raise exception 'ACCOUNT_NOT_FOUND'; end if;
  if p_active is null then raise exception 'ACTIVE_REQUIRED'; end if;
  if (select is_super from public.admin_roles where id = v_a.role_id) then raise exception 'SUPER_ADMIN_IMMUTABLE'; end if;
  if p_id = v_me then raise exception 'CANNOT_CHANGE_SELF'; end if;
  update public.admin_accounts set active = p_active where id = p_id;
  if not p_active then
    update public.admin_sessions set ended_at = now() where admin_id = p_id and ended_at is null;
  end if;
  perform public.log_admin_action(case when p_active then 'ADMIN_ACTIVATED' else 'ADMIN_DEACTIVATED' end,
                                  p_id::text, v_a.name, coalesce(p_reason, ''), '');
  return public._admin_account_view(p_id);
end $$;

-- ফ্ল্যাগড = view অক্ষত, কিন্তু কোনো অ্যাকশন নয় (ক্লায়েন্ট-গেটিং সেশন ৭-এ; admin_can_act সার্ভার-সাইডে একই নিয়ম জানে)
create or replace function public.admin_account_set_flagged(p_id uuid, p_flagged boolean, p_reason text default '')
returns jsonb language plpgsql security definer set search_path to 'public' as $$
declare v_a public.admin_accounts; v_me uuid;
begin
  perform public._admin_require_super();
  select id into v_me from public.admin_accounts where auth_user_id = auth.uid();
  select * into v_a from public.admin_accounts where id = p_id;
  if not found then raise exception 'ACCOUNT_NOT_FOUND'; end if;
  if p_flagged is null then raise exception 'FLAGGED_REQUIRED'; end if;
  if (select is_super from public.admin_roles where id = v_a.role_id) then raise exception 'SUPER_ADMIN_IMMUTABLE'; end if;
  if p_id = v_me then raise exception 'CANNOT_CHANGE_SELF'; end if;
  update public.admin_accounts
     set flagged = p_flagged,
         flagged_at = case when p_flagged then now() else null end,
         flagged_by = case when p_flagged then v_me else null end
   where id = p_id;
  perform public.log_admin_action(case when p_flagged then 'ADMIN_FLAGGED' else 'ADMIN_UNFLAGGED' end,
                                  p_id::text, v_a.name, coalesce(p_reason, ''), '');
  return public._admin_account_view(p_id);
end $$;

-- ───────────── ১১) GRANT: শুধু authenticated (+service_role); anon নয় ─────────────
revoke execute on function
  public.admin_roles_guard(), public.admin_accounts_guard(),
  public.is_super_admin(uuid), public.admin_can_act(text, uuid),
  public._admin_require_super(), public._admin_account_view(uuid),
  public.admin_me(), public.admin_session_start(text, text, text), public.admin_heartbeat(uuid),
  public.admin_session_end(uuid),
  public.admin_profile_update(uuid, text, text, text, text, text),
  public.admin_roles_list(), public.admin_role_upsert(text, text, jsonb), public.admin_role_delete(text),
  public.admin_accounts_list(), public.admin_account_set_role(uuid, text),
  public.admin_account_set_active(uuid, boolean, text), public.admin_account_set_flagged(uuid, boolean, text)
  from public, anon;
grant execute on function
  public.is_super_admin(uuid), public.admin_can_act(text, uuid),
  public.admin_me(), public.admin_session_start(text, text, text), public.admin_heartbeat(uuid),
  public.admin_session_end(uuid),
  public.admin_profile_update(uuid, text, text, text, text, text),
  public.admin_roles_list(), public.admin_role_upsert(text, text, jsonb), public.admin_role_delete(text),
  public.admin_accounts_list(), public.admin_account_set_role(uuid, text),
  public.admin_account_set_active(uuid, boolean, text), public.admin_account_set_flagged(uuid, boolean, text)
  to authenticated, service_role;
-- internal helpers: SECURITY DEFINER RPC-গুলো (owner হিসেবে) ডাকে, ক্লায়েন্ট সরাসরি নয়
grant execute on function public._admin_require_super(), public._admin_account_view(uuid) to service_role;
