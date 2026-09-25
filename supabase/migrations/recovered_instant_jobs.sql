-- RECOVERED from live DB on 2026-09-18, verbatim capture, no logic changes. See
-- RPC_SYNC_FIX_PROGRESS.md Step 2.
-- Functions in this file (Step 1 missing-RPC group): broadcast_instant_job, cancel_instant_job, expire_broadcasting_instant_job, mark_job_started, mark_solver_arrived, mark_solver_on_way, solver_cancel_job, system_notify_48hour_auto_release

-- signature: broadcast_instant_job(text)
CREATE OR REPLACE FUNCTION public.broadcast_instant_job(p_problem_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_category_id_lower text;
  v_category_name_lower text;
  v_radius numeric;
  v_count int;
begin
  select * into v_problem from public.problems where id = p_problem_id;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;
  if auth.uid() is null or auth.uid() <> v_problem.user_id then
    raise exception 'NOT_AUTHORIZED';
  end if;
  if not v_problem.is_instant_job then
    raise exception 'NOT_INSTANT_JOB';
  end if;

  v_category_id_lower := lower(coalesce(v_problem.category_id, ''));
  v_category_name_lower := lower(coalesce(v_problem.category_name, ''));
  v_radius := coalesce(v_problem.broadcast_radius_km, 5.0);

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  select
    'NOTIF_INSTANT_' || replace(gen_random_uuid()::text, '-', ''),
    u.id,
    'নতুন জরুরি জব: ' || coalesce(v_problem.category_name, ''),
    v_problem.user_name || ' একটি জরুরি কাজ পোস্ট করেছেন: ' || left(v_problem.title, 40) ||
      ' (বাজেট ৳' ||
      case when v_problem.min_budget = v_problem.max_budget
        then trim(to_char(v_problem.min_budget, 'FM999999999'))
        else trim(to_char(v_problem.min_budget, 'FM999999999')) || ' – ' || trim(to_char(v_problem.max_budget, 'FM999999999'))
      end || ')',
    'problem',
    p_problem_id,
    p_problem_id,
    now()
  from public.users u
  where u.id <> v_problem.user_id
    and (u.role = 'SOLVER' or u.has_solver_role = true)
    and u.instant_job_notifications_enabled = true
    and (
      trim(coalesce(u.solver_categories,'')) = ''
      or exists (
        select 1 from unnest(string_to_array(u.solver_categories, ',')) as cat(val)
        where lower(trim(cat.val)) = v_category_id_lower or lower(trim(cat.val)) = v_category_name_lower
      )
    )
    and (
      u.latitude is null or u.longitude is null or u.latitude = 0 or u.longitude = 0
      or v_problem.latitude is null or v_problem.longitude is null or v_problem.latitude = 0 or v_problem.longitude = 0
      or (
        2 * 6371 * asin(sqrt(
          power(sin(radians((v_problem.latitude - u.latitude) / 2)), 2) +
          cos(radians(u.latitude)) * cos(radians(v_problem.latitude)) *
          power(sin(radians((v_problem.longitude - u.longitude) / 2)), 2)
        )) <= v_radius
      )
    );

  get diagnostics v_count = row_count;
  return jsonb_build_object('result', 'OK', 'notified_count', v_count);
end;
$function$
;
GRANT EXECUTE ON FUNCTION broadcast_instant_job(text) TO "-";
GRANT EXECUTE ON FUNCTION broadcast_instant_job(text) TO anon;
GRANT EXECUTE ON FUNCTION broadcast_instant_job(text) TO authenticated;
GRANT EXECUTE ON FUNCTION broadcast_instant_job(text) TO postgres;
GRANT EXECUTE ON FUNCTION broadcast_instant_job(text) TO service_role;

-- signature: cancel_instant_job(text,text,integer)
CREATE OR REPLACE FUNCTION public.cancel_instant_job(p_problem_id text, p_reason text DEFAULT 'ইউজার কর্তৃক বাতিল'::text, p_progress_step integer DEFAULT 1)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
  v_step int := greatest(coalesce(p_progress_step, 1), 1);
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if auth.uid() <> v_problem.user_id then
    raise exception 'NOT_AUTHORIZED';
  end if;
  if v_problem.status in ('COMPLETED', 'CANCELLED') then
    return jsonb_build_object('result', 'ALREADY_TERMINAL');
  end if;

  update public.problems set
    job_status = 'CANCELLED',
    status = 'CANCELLED',
    accepted_bid_id = null,
    accepted_solver_id = null,
    accepted_solver_name = null,
    accepted_amount = null,
    has_release_request = false,
    release_request_extra_amount = 0,
    release_request_note = '',
    release_requested_at = null,
    pending_extra_amount = null,
    pending_extra_amount_note = null,
    pending_extra_amount_requested_at = null,
    solver_cancelled_notice = null,
    last_activity_at = v_now
  where id = p_problem_id;

  update public.bids set
    status = 'CANCELLED',
    progress_at_cancel = v_step,
    resolution_type = 'USER_CANCEL',
    resolved_at = v_now
  where problem_id = p_problem_id and status <> 'CANCELLED';

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    'কাজ বাতিল নিশ্চিত ❌',
    '"' || v_problem.title || '" কাজটি সফলভাবে বাতিল করা হয়েছে।',
    'problem', p_problem_id, p_problem_id, v_now);

  if v_problem.accepted_solver_id is not null then
    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
      'কাজটি বাতিল করা হয়েছে ❌',
      'ক্লায়েন্ট "' || v_problem.title || '" কাজটি বাতিল করেছেন। কারণ: ' || coalesce(p_reason, 'ইউজার কর্তৃক বাতিল'),
      'problem', p_problem_id, p_problem_id, v_now);
  end if;

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION cancel_instant_job(text,text,integer) TO "-";
GRANT EXECUTE ON FUNCTION cancel_instant_job(text,text,integer) TO anon;
GRANT EXECUTE ON FUNCTION cancel_instant_job(text,text,integer) TO authenticated;
GRANT EXECUTE ON FUNCTION cancel_instant_job(text,text,integer) TO postgres;
GRANT EXECUTE ON FUNCTION cancel_instant_job(text,text,integer) TO service_role;

-- signature: expire_broadcasting_instant_job(text,integer)
CREATE OR REPLACE FUNCTION public.expire_broadcasting_instant_job(p_problem_id text, p_progress_step integer DEFAULT 1)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
  v_step int := greatest(coalesce(p_progress_step, 1), 1);
  v_bid record;
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if auth.uid() <> v_problem.user_id then
    raise exception 'NOT_AUTHORIZED';
  end if;
  if v_problem.job_status <> 'BROADCASTING' or v_problem.status in ('COMPLETED', 'CANCELLED') then
    return jsonb_build_object('result', 'NOT_BROADCASTING');
  end if;

  update public.problems set
    job_status = 'CANCELLED',
    is_user_deleted = true,
    status = 'CANCELLED',
    last_activity_at = v_now
  where id = p_problem_id;

  for v_bid in
    update public.bids set status = 'CANCELLED', progress_at_cancel = v_step, resolution_type = 'EXPIRED', resolved_at = v_now
    where problem_id = p_problem_id and status = 'PENDING'
    returning solver_id
  loop
    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_bid.solver_id,
      'জরুরি জবটি বাতিল হয়েছে ⏱️',
      '"' || v_problem.title || '" কাজের সময়সীমা শেষ হওয়ায় পোস্টটি বাতিল হয়ে গেছে।',
      'problem', p_problem_id, p_problem_id, v_now);
  end loop;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    'জরুরি পোস্ট বাতিল ⏱️',
    'কোনো সলভার সময়মতো বিড না দেওয়ায় আপনার জরুরি পোস্টটি বাতিল হয়ে গেছে।',
    'problem', p_problem_id, p_problem_id, v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION expire_broadcasting_instant_job(text,integer) TO "-";
GRANT EXECUTE ON FUNCTION expire_broadcasting_instant_job(text,integer) TO anon;
GRANT EXECUTE ON FUNCTION expire_broadcasting_instant_job(text,integer) TO authenticated;
GRANT EXECUTE ON FUNCTION expire_broadcasting_instant_job(text,integer) TO postgres;
GRANT EXECUTE ON FUNCTION expire_broadcasting_instant_job(text,integer) TO service_role;

-- signature: mark_job_started(text)
CREATE OR REPLACE FUNCTION public.mark_job_started(p_problem_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if v_problem.accepted_solver_id is null or auth.uid() <> v_problem.accepted_solver_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.problems set
    job_status = 'IN_PROGRESS',
    status = 'IN_PROGRESS',
    job_started_at = coalesce(job_started_at, v_now),
    last_activity_at = v_now
  where id = p_problem_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    'কাজ শুরু হয়েছে ⚡',
    coalesce(v_problem.accepted_solver_name, 'সলভার') || ' "' || v_problem.title || '" কাজটি শুরু করেছেন।',
    'problem', p_problem_id, p_problem_id, v_now);

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
    'কাজ চলমান ⚡',
    '"' || v_problem.title || '" কাজটির সময় গণনা শুরু হয়েছে। মনোযোগ দিয়ে সম্পন্ন করুন।',
    'problem', p_problem_id, p_problem_id, v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION mark_job_started(text) TO "-";
GRANT EXECUTE ON FUNCTION mark_job_started(text) TO anon;
GRANT EXECUTE ON FUNCTION mark_job_started(text) TO authenticated;
GRANT EXECUTE ON FUNCTION mark_job_started(text) TO postgres;
GRANT EXECUTE ON FUNCTION mark_job_started(text) TO service_role;

-- signature: mark_solver_arrived(text)
CREATE OR REPLACE FUNCTION public.mark_solver_arrived(p_problem_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if v_problem.accepted_solver_id is null or auth.uid() <> v_problem.accepted_solver_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.problems set
    job_status = 'ARRIVED',
    arrived_at = coalesce(arrived_at, v_now),
    last_activity_at = v_now
  where id = p_problem_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    'সলভার পৌঁছে গেছেন 📍',
    coalesce(v_problem.accepted_solver_name, 'সলভার') || ' আপনার ঠিকানায় পৌঁছে গেছেন।',
    'problem', p_problem_id, p_problem_id, v_now);

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
    'আপনি লোকেশনে পৌঁছেছেন 📍',
    'ক্লায়েন্টের সাথে দেখা করে কাজ শুরু করুন।',
    'problem', p_problem_id, p_problem_id, v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION mark_solver_arrived(text) TO "-";
GRANT EXECUTE ON FUNCTION mark_solver_arrived(text) TO anon;
GRANT EXECUTE ON FUNCTION mark_solver_arrived(text) TO authenticated;
GRANT EXECUTE ON FUNCTION mark_solver_arrived(text) TO postgres;
GRANT EXECUTE ON FUNCTION mark_solver_arrived(text) TO service_role;

-- signature: mark_solver_on_way(text)
CREATE OR REPLACE FUNCTION public.mark_solver_on_way(p_problem_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if v_problem.accepted_solver_id is null or auth.uid() <> v_problem.accepted_solver_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.problems set
    job_status = 'ON_THE_WAY',
    on_way_at = coalesce(on_way_at, v_now),
    last_activity_at = v_now
  where id = p_problem_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    'সলভার রওয়ানা হয়েছেন 🚗',
    coalesce(v_problem.accepted_solver_name, 'সলভার') || ' আপনার লোকেশনের উদ্দেশ্যে রওয়ানা হয়েছেন।',
    'problem', p_problem_id, p_problem_id, v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION mark_solver_on_way(text) TO "-";
GRANT EXECUTE ON FUNCTION mark_solver_on_way(text) TO anon;
GRANT EXECUTE ON FUNCTION mark_solver_on_way(text) TO authenticated;
GRANT EXECUTE ON FUNCTION mark_solver_on_way(text) TO postgres;
GRANT EXECUTE ON FUNCTION mark_solver_on_way(text) TO service_role;

-- signature: solver_cancel_job(text,text,boolean)
CREATE OR REPLACE FUNCTION public.solver_cancel_job(p_problem_id text, p_reason text, p_reopen_as_open boolean DEFAULT true)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_escrow public.escrows%rowtype;
  v_now timestamptz := now();
  v_refund_result jsonb := jsonb_build_object('result', 'NO_ESCROW');
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if auth.uid() <> v_problem.accepted_solver_id and not public.is_admin(auth.uid()) then raise exception 'NOT_AUTHORIZED'; end if;

  if v_problem.status in ('COMPLETED', 'CANCELLED') then
    return jsonb_build_object('result', 'ALREADY_TERMINAL', 'status', v_problem.status);
  end if;

  update public.problems set
    status = case when p_reopen_as_open then 'OPEN' else 'CANCELLED' end,
    job_status = case when is_instant_job then (case when p_reopen_as_open then 'BROADCASTING' else 'CANCELLED' end) else job_status end,
    broadcast_timer_started_at = case when is_instant_job and p_reopen_as_open then v_now else broadcast_timer_started_at end,
    accepted_bid_id = case when p_reopen_as_open then null else accepted_bid_id end,
    accepted_solver_id = case when p_reopen_as_open then null else accepted_solver_id end,
    accepted_solver_name = case when p_reopen_as_open then null else accepted_solver_name end,
    accepted_amount = case when p_reopen_as_open then null else accepted_amount end,
    solver_live_lat = null, solver_live_lng = null, solver_live_updated_at = null,
    arrived_at = null, job_started_at = null,
    has_release_request = false, release_request_extra_amount = 0, release_request_note = '',
    release_requested_at = null,
    is_disputed = false, dispute_reason = null, dispute_initiator_id = null,
    confirmed_extra_amount_total = 0,
    solver_cancelled_notice = case when p_reopen_as_open
      then 'সমাধানকারী আপনার পোস্টটির বিড বাতিল করেছেন। আপনি পুনরায় বিড নির্বাচন করুন।'
      else 'সমাধানকারী আপনার পোস্টটির বিড বাতিল করেছেন।' end,
    last_activity_at = v_now
  where id = p_problem_id;

  select * into v_escrow from public.escrows where problem_id = p_problem_id and status = 'HELD'
    order by created_at desc limit 1 for update;

  if found then
    v_refund_result := public.refund_escrow_once(v_escrow.id, 'SOLVER_CANCEL', 100);
  end if;

  return jsonb_build_object('result', 'OK', 'refund', v_refund_result);
end;
$function$
;
GRANT EXECUTE ON FUNCTION solver_cancel_job(text,text,boolean) TO anon;
GRANT EXECUTE ON FUNCTION solver_cancel_job(text,text,boolean) TO authenticated;
GRANT EXECUTE ON FUNCTION solver_cancel_job(text,text,boolean) TO postgres;
GRANT EXECUTE ON FUNCTION solver_cancel_job(text,text,boolean) TO service_role;

-- signature: system_notify_48hour_auto_release(text,uuid,text,text,text,text)
CREATE OR REPLACE FUNCTION public.system_notify_48hour_auto_release(p_problem_id text, p_target_user_id uuid, p_title text, p_message text, p_target_type text DEFAULT 'problem'::text, p_target_id text DEFAULT NULL::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_id text := 'NOTIF_' || replace(gen_random_uuid()::text, '-', '');
  v_prob record;
  v_role text;
begin
  if auth.uid() is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if p_problem_id is null or p_target_user_id is null then
    raise exception 'MISSING_PARAMS';
  end if;
  if p_title is null or trim(p_title) = '' then
    raise exception 'TITLE_REQUIRED';
  end if;

  select id, user_id, accepted_solver_id, status, has_release_request, release_requested_at, is_disputed
    into v_prob
  from public.problems
  where id = p_problem_id
  for share;

  if v_prob.id is null then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  if not (v_prob.status = 'IN_PROGRESS' or v_prob.status = 'OPEN') then
    raise exception 'PROBLEM_NOT_ELIGIBLE';
  end if;
  if v_prob.accepted_solver_id is null then
    raise exception 'PROBLEM_NOT_ELIGIBLE';
  end if;
  if not coalesce(v_prob.has_release_request, false) then
    raise exception 'PROBLEM_NOT_ELIGIBLE';
  end if;
  if v_prob.release_requested_at is null then
    raise exception 'PROBLEM_NOT_ELIGIBLE';
  end if;
  if coalesce(v_prob.is_disputed, false) then
    raise exception 'PROBLEM_NOT_ELIGIBLE';
  end if;
  if (now() - v_prob.release_requested_at) < interval '48 hours' then
    raise exception 'NOT_YET_ELIGIBLE';
  end if;

  if not (p_target_user_id = v_prob.user_id or p_target_user_id = v_prob.accepted_solver_id) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  -- [ধাপ ৩৭] role: target owner হলে USER, accepted-solver হলে SOLVER (Kotlin
  -- checkAndProcess48HourAutoReleases-এর userNotif/solverNotif প্যাটার্নের সমতুল্য)
  v_role := case when p_target_user_id = v_prob.user_id then 'USER' when p_target_user_id = v_prob.accepted_solver_id then 'SOLVER' else '' end;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
  values (v_id, p_target_user_id, trim(p_title), trim(p_message), coalesce(p_target_type, 'problem'), p_target_id, p_problem_id, v_role, now());

  return jsonb_build_object('result', 'OK', 'id', v_id);
end;
$function$
;
GRANT EXECUTE ON FUNCTION system_notify_48hour_auto_release(text,uuid,text,text,text,text) TO "-";
GRANT EXECUTE ON FUNCTION system_notify_48hour_auto_release(text,uuid,text,text,text,text) TO anon;
GRANT EXECUTE ON FUNCTION system_notify_48hour_auto_release(text,uuid,text,text,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION system_notify_48hour_auto_release(text,uuid,text,text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION system_notify_48hour_auto_release(text,uuid,text,text,text,text) TO service_role;

