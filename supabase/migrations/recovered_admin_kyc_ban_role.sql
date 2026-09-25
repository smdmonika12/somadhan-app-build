-- RECOVERED from live DB on 2026-09-18, verbatim capture, no logic changes. See
-- RPC_SYNC_FIX_PROGRESS.md Step 2.
-- Functions in this file (Step 1 missing-RPC group): admin_approve_kyc, admin_reject_kyc, admin_revoke_kyc, admin_set_banned, admin_set_restricted, admin_set_verified_badge, admin_change_role

-- signature: admin_approve_kyc(uuid)
CREATE OR REPLACE FUNCTION public.admin_approve_kyc(p_user_id uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_now timestamptz := now();
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.users
  set is_kyc_verified = true, kyc_status = 'APPROVED', updated_at = v_now
  where id = p_user_id;

  if not found then
    return jsonb_build_object('result', 'USER_NOT_FOUND');
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), p_user_id,
    'KYC ভেরিফিকেশন সফল! ✅',
    'অভিনন্দন! আপনার KYC আবেদন অনুমোদিত হয়েছে এবং আপনার প্রোফাইল ভেরিফাইড হয়েছে। এখন সব পোস্টে বিড করতে পারবেন।',
    'kyc', p_user_id::text, 'SOLVER', v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_approve_kyc(uuid) TO "-";
GRANT EXECUTE ON FUNCTION admin_approve_kyc(uuid) TO anon;
GRANT EXECUTE ON FUNCTION admin_approve_kyc(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_approve_kyc(uuid) TO postgres;
GRANT EXECUTE ON FUNCTION admin_approve_kyc(uuid) TO service_role;

-- signature: admin_reject_kyc(uuid,text)
CREATE OR REPLACE FUNCTION public.admin_reject_kyc(p_user_id uuid, p_reason text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_now timestamptz := now();
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.users
  set is_kyc_verified = false, kyc_status = 'REJECTED', kyc_reject_reason = p_reason, updated_at = v_now
  where id = p_user_id;

  if not found then
    return jsonb_build_object('result', 'USER_NOT_FOUND');
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), p_user_id,
    'KYC আবেদন বাতিল হয়েছে',
    'আপনার KYC আবেদন বাতিল হয়েছে। কারণ: ' || p_reason || '। অনুগ্রহ করে পুনরায় সঠিক তথ্য ও ছবি জমা দিন।',
    'kyc', p_user_id::text, 'SOLVER', v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_reject_kyc(uuid,text) TO "-";
GRANT EXECUTE ON FUNCTION admin_reject_kyc(uuid,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_reject_kyc(uuid,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_reject_kyc(uuid,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_reject_kyc(uuid,text) TO service_role;

-- signature: admin_revoke_kyc(uuid,text)
CREATE OR REPLACE FUNCTION public.admin_revoke_kyc(p_user_id uuid, p_reason text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_now timestamptz := now();
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.users
  set is_kyc_verified = false, kyc_status = 'REJECTED', kyc_reject_reason = p_reason, updated_at = v_now
  where id = p_user_id;

  if not found then
    return jsonb_build_object('result', 'USER_NOT_FOUND');
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), p_user_id,
    'KYC ভেরিফিকেশন প্রত্যাহার করা হয়েছে',
    'আপনার KYC ভেরিফিকেশন প্রত্যাহার করা হয়েছে। কারণ: ' || p_reason,
    'kyc', p_user_id::text, 'SOLVER', v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_revoke_kyc(uuid,text) TO "-";
GRANT EXECUTE ON FUNCTION admin_revoke_kyc(uuid,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_revoke_kyc(uuid,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_revoke_kyc(uuid,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_revoke_kyc(uuid,text) TO service_role;

-- signature: admin_set_banned(uuid,boolean)
CREATE OR REPLACE FUNCTION public.admin_set_banned(p_user_id uuid, p_banned boolean)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_now timestamptz := now();
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.users
  set is_banned = p_banned, updated_at = v_now
  where id = p_user_id;

  if not found then
    return jsonb_build_object('result', 'USER_NOT_FOUND');
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), p_user_id,
    case when p_banned then 'অ্যাকাউন্ট স্থগিত (Banned) করা হয়েছে' else 'অ্যাকাউন্ট সক্রিয় করা হয়েছে' end,
    case when p_banned then 'আপনার অ্যাকাউন্টটি অ্যাডমিন কর্তৃক সাময়িকভাবে স্থগিত (Banned) করা হয়েছে।' else 'আপনার অ্যাকাউন্টের স্থগিতাদেশ তুলে নেওয়া হয়েছে।' end,
    'role', p_user_id::text, '', v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_set_banned(uuid,boolean) TO "-";
GRANT EXECUTE ON FUNCTION admin_set_banned(uuid,boolean) TO anon;
GRANT EXECUTE ON FUNCTION admin_set_banned(uuid,boolean) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_set_banned(uuid,boolean) TO postgres;
GRANT EXECUTE ON FUNCTION admin_set_banned(uuid,boolean) TO service_role;

-- signature: admin_set_banned(uuid,boolean,text)
CREATE OR REPLACE FUNCTION public.admin_set_banned(p_user_id uuid, p_banned boolean, p_role text DEFAULT NULL::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_now timestamptz := now();
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;
  if p_role is not null and p_role not in ('USER', 'SOLVER') then
    raise exception 'INVALID_ROLE';
  end if;

  if p_role = 'SOLVER' then
    update public.users set is_banned_solver = p_banned, updated_at = v_now where id = p_user_id;
  elsif p_role = 'USER' then
    update public.users set is_banned_user = p_banned, updated_at = v_now where id = p_user_id;
  else
    update public.users set is_banned = p_banned, updated_at = v_now where id = p_user_id;
  end if;

  if not found then
    return jsonb_build_object('result', 'USER_NOT_FOUND');
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), p_user_id,
    case when p_banned then 'অ্যাকাউন্ট স্থগিত (Banned) করা হয়েছে' else 'অ্যাকাউন্ট সক্রিয় করা হয়েছে' end,
    case when p_banned then 'আপনার অ্যাকাউন্টটি অ্যাডমিন কর্তৃক সাময়িকভাবে স্থগিত (Banned) করা হয়েছে।' else 'আপনার অ্যাকাউন্টের স্থগিতাদেশ তুলে নেওয়া হয়েছে।' end,
    'role', p_user_id::text, coalesce(p_role, ''), v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_set_banned(uuid,boolean,text) TO "-";
GRANT EXECUTE ON FUNCTION admin_set_banned(uuid,boolean,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_set_banned(uuid,boolean,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_set_banned(uuid,boolean,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_set_banned(uuid,boolean,text) TO service_role;

-- signature: admin_set_restricted(uuid,boolean)
CREATE OR REPLACE FUNCTION public.admin_set_restricted(p_user_id uuid, p_restricted boolean)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_now timestamptz := now();
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.users
  set is_restricted = p_restricted, updated_at = v_now
  where id = p_user_id;

  if not found then
    return jsonb_build_object('result', 'USER_NOT_FOUND');
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), p_user_id,
    case when p_restricted then 'অ্যাকাউন্ট সীমাবদ্ধ (Restricted) করা হয়েছে' else 'অ্যাকাউন্টের সীমাবদ্ধতা তুলে নেওয়া হয়েছে' end,
    case when p_restricted then 'আপনার অ্যাকাউন্টটিতে রেস্ট্রিকশন দেওয়া হয়েছে। কিছু ফিচার সীমিত থাকতে পারে।' else 'আপনার অ্যাকাউন্টের সীমাবদ্ধতা সফলভাবে প্রত্যাহার করা হয়েছে।' end,
    'role', p_user_id::text, '', v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_set_restricted(uuid,boolean) TO "-";
GRANT EXECUTE ON FUNCTION admin_set_restricted(uuid,boolean) TO anon;
GRANT EXECUTE ON FUNCTION admin_set_restricted(uuid,boolean) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_set_restricted(uuid,boolean) TO postgres;
GRANT EXECUTE ON FUNCTION admin_set_restricted(uuid,boolean) TO service_role;

-- signature: admin_set_restricted(uuid,boolean,text)
CREATE OR REPLACE FUNCTION public.admin_set_restricted(p_user_id uuid, p_restricted boolean, p_role text DEFAULT NULL::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_now timestamptz := now();
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;
  if p_role is not null and p_role not in ('USER', 'SOLVER') then
    raise exception 'INVALID_ROLE';
  end if;

  if p_role = 'SOLVER' then
    update public.users set is_restricted_solver = p_restricted, updated_at = v_now where id = p_user_id;
  elsif p_role = 'USER' then
    update public.users set is_restricted_user = p_restricted, updated_at = v_now where id = p_user_id;
  else
    update public.users set is_restricted = p_restricted, updated_at = v_now where id = p_user_id;
  end if;

  if not found then
    return jsonb_build_object('result', 'USER_NOT_FOUND');
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), p_user_id,
    case when p_restricted then 'অ্যাকাউন্ট সীমাবদ্ধ (Restricted) করা হয়েছে' else 'অ্যাকাউন্টের সীমাবদ্ধতা তুলে নেওয়া হয়েছে' end,
    case when p_restricted then 'আপনার অ্যাকাউন্টটিতে রেস্ট্রিকশন দেওয়া হয়েছে। কিছু ফিচার সীমিত থাকতে পারে।' else 'আপনার অ্যাকাউন্টের সীমাবদ্ধতা সফলভাবে প্রত্যাহার করা হয়েছে।' end,
    'role', p_user_id::text, coalesce(p_role, ''), v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_set_restricted(uuid,boolean,text) TO "-";
GRANT EXECUTE ON FUNCTION admin_set_restricted(uuid,boolean,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_set_restricted(uuid,boolean,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_set_restricted(uuid,boolean,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_set_restricted(uuid,boolean,text) TO service_role;

-- signature: admin_set_verified_badge(uuid,boolean,text)
CREATE OR REPLACE FUNCTION public.admin_set_verified_badge(p_user_id uuid, p_verified boolean, p_role text DEFAULT NULL::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_now timestamptz := now();
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;
  if p_role is not null and p_role not in ('USER', 'SOLVER') then
    raise exception 'INVALID_ROLE';
  end if;

  if p_role = 'SOLVER' then
    update public.users set verified_badge_solver = p_verified, updated_at = v_now where id = p_user_id;
  elsif p_role = 'USER' then
    update public.users set verified_badge_user = p_verified, updated_at = v_now where id = p_user_id;
  else
    update public.users set is_verified_badge = p_verified, updated_at = v_now where id = p_user_id;
  end if;

  if not found then
    return jsonb_build_object('result', 'USER_NOT_FOUND');
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), p_user_id,
    case when p_verified then 'ভেরিফাইড ব্যাজ প্রদান করা হয়েছে ✅' else 'ভেরিফাইড ব্যাজ প্রত্যাহার করা হয়েছে' end,
    case when p_verified then 'অ্যাডমিন আপনাকে ভেরিফাইড ব্যাজ প্রদান করেছেন।' else 'আপনার ভেরিফাইড ব্যাজ প্রত্যাহার করা হয়েছে।' end,
    'role', p_user_id::text, coalesce(p_role, ''), v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_set_verified_badge(uuid,boolean,text) TO "-";
GRANT EXECUTE ON FUNCTION admin_set_verified_badge(uuid,boolean,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_set_verified_badge(uuid,boolean,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_set_verified_badge(uuid,boolean,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_set_verified_badge(uuid,boolean,text) TO service_role;

-- signature: admin_change_role(uuid,text)
CREATE OR REPLACE FUNCTION public.admin_change_role(p_user_id uuid, p_new_role text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_now timestamptz := now();
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;
  if p_new_role not in ('USER','SOLVER','ADMIN') then
    raise exception 'INVALID_ROLE';
  end if;

  update public.users
  set role = p_new_role,
      has_user_role = has_user_role or (p_new_role = 'USER'),
      has_solver_role = has_solver_role or (p_new_role = 'SOLVER'),
      updated_at = v_now
  where id = p_user_id;

  if not found then
    return jsonb_build_object('result', 'USER_NOT_FOUND');
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), p_user_id,
    'অ্যাকাউন্ট রোল পরিবর্তন',
    'আপনার অ্যাকাউন্ট রোল পরিবর্তন করে ''' || p_new_role || ''' করা হয়েছে।',
    'role', p_user_id::text, '', v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_change_role(uuid,text) TO "-";
GRANT EXECUTE ON FUNCTION admin_change_role(uuid,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_change_role(uuid,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_change_role(uuid,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_change_role(uuid,text) TO service_role;

