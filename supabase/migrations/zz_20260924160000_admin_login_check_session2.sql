-- [ADMIN_ROLE_PROFILE সেশন ২] মাল্টি-এডমিন লগইনের জন্য pre-auth "এই ফোনটা কি এডমিন অ্যাকাউন্টের?" চেক।
--
-- কেন লাগে: লগইন স্ক্রিন পাসওয়ার্ড দেখার আগে জানতে পারে না ফোনটা এডমিনের (→ Supabase Auth + admin_session_start
-- পথ) নাকি সাধারণ ইউজার/সলভারের (→ আগের OTP পথ)। `admin_accounts`-এ anon-এর কোনো অ্যাক্সেস নেই (সেশন ১-এর RLS),
-- তাই এই একটা সংকীর্ণ SECURITY DEFINER ফাংশন — শুধু boolean ফেরত দেয়, কোনো নাম/রোল/আইডি না।
--
-- ⚠️ সচেতন ট্রেড-অফ: anon এখন জানতে পারবে একটা নির্দিষ্ট ফোন এডমিনের কিনা (enumeration)। এটা আগের অবস্থার চেয়ে খারাপ
-- না — বিদ্যমান `admin_credentials_get_phone` anon-কে সরাসরি একমাত্র এডমিনের ফোন নম্বরটাই দিত। পাসওয়ার্ড
-- Supabase Auth-এর হাতে, তাই ফোন জানলেও লগইন হয় না।
-- নিষ্ক্রিয় (active=false) অ্যাকাউন্টের ফোনেও true — যাতে লগইন স্ক্রিন "নিষ্ক্রিয়" বার্তা দেখাতে পারে।

create or replace function public._admin_norm_phone(p_phone text)
returns text language sql immutable set search_path to 'public' as $$
  select case
    when d ~ '^8801[0-9]{9}$' then '0' || substr(d, 4)
    when d ~ '^1[0-9]{9}$'    then '0' || d
    else d
  end
  from (select regexp_replace(coalesce(p_phone, ''), '[^0-9]', '', 'g') as d) t;
$$;

create or replace function public.admin_login_check(p_phone text)
returns boolean language sql stable security definer set search_path to 'public' as $$
  select exists (
    select 1 from public.admin_accounts a
    where public._admin_norm_phone(a.phone) = public._admin_norm_phone(p_phone)
      and length(public._admin_norm_phone(p_phone)) >= 8
  );
$$;

revoke execute on function public._admin_norm_phone(text), public.admin_login_check(text) from public, anon, authenticated;
grant execute on function public.admin_login_check(text) to anon, authenticated, service_role;
grant execute on function public._admin_norm_phone(text) to service_role;
