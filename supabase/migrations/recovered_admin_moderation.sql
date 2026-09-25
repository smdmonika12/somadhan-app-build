-- RECOVERED from live DB on 2026-09-18, verbatim capture, no logic changes. See
-- RPC_SYNC_FIX_PROGRESS.md Step 2.
-- Functions in this file (Step 1 missing-RPC group): admin_delete_message, admin_delete_notification_group, admin_delete_rating, admin_manually_flag_dispute, admin_send_message_to_problem_chat, admin_broadcast_notification, admin_notify_user, admin_reassign_solver, admin_remove_category_from_solvers, admin_update_direct_contract_status, admin_update_problem_budget, admin_update_problem_status, admin_force_cancel_instant_job, log_admin_action

-- signature: admin_delete_message(text)
CREATE OR REPLACE FUNCTION public.admin_delete_message(p_message_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_deleted_id text;
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'ADMIN_ONLY';
  end if;

  delete from public.messages where id = p_message_id returning id into v_deleted_id;
  if v_deleted_id is null then
    return jsonb_build_object('result', 'NOT_FOUND');
  end if;

  return jsonb_build_object('result', 'OK', 'id', v_deleted_id);
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_delete_message(text) TO "-";
GRANT EXECUTE ON FUNCTION admin_delete_message(text) TO anon;
GRANT EXECUTE ON FUNCTION admin_delete_message(text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_delete_message(text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_delete_message(text) TO service_role;

-- signature: admin_delete_notification_group(text,timestamp with time zone,timestamp with time zone)
CREATE OR REPLACE FUNCTION public.admin_delete_notification_group(p_title text, p_scheduled_for timestamp with time zone DEFAULT NULL::timestamp with time zone, p_timestamp timestamp with time zone DEFAULT NULL::timestamp with time zone)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_deleted int;
begin
  if not is_admin(auth.uid()) then
    raise exception 'ADMIN_ONLY';
  end if;
  if p_title is null or trim(p_title) = '' then
    raise exception 'TITLE_REQUIRED';
  end if;

  if p_scheduled_for is not null then
    delete from public.notifications where title = trim(p_title) and scheduled_for = p_scheduled_for;
  elsif p_timestamp is not null then
    delete from public.notifications where title = trim(p_title) and "timestamp" = p_timestamp;
  else
    delete from public.notifications where title = trim(p_title);
  end if;
  get diagnostics v_deleted = row_count;

  return jsonb_build_object('result', 'OK', 'deleted', v_deleted);
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_delete_notification_group(text,timestamp with time zone,timestamp with time zone) TO "-";
GRANT EXECUTE ON FUNCTION admin_delete_notification_group(text,timestamp with time zone,timestamp with time zone) TO anon;
GRANT EXECUTE ON FUNCTION admin_delete_notification_group(text,timestamp with time zone,timestamp with time zone) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_delete_notification_group(text,timestamp with time zone,timestamp with time zone) TO postgres;
GRANT EXECUTE ON FUNCTION admin_delete_notification_group(text,timestamp with time zone,timestamp with time zone) TO service_role;

-- signature: admin_delete_rating(text)
CREATE OR REPLACE FUNCTION public.admin_delete_rating(p_rating_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_deleted_id text;
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'ADMIN_ONLY';
  end if;

  delete from public.ratings where id = p_rating_id returning id into v_deleted_id;
  if v_deleted_id is null then
    return jsonb_build_object('result', 'NOT_FOUND');
  end if;

  return jsonb_build_object('result', 'OK', 'id', v_deleted_id);
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_delete_rating(text) TO "-";
GRANT EXECUTE ON FUNCTION admin_delete_rating(text) TO anon;
GRANT EXECUTE ON FUNCTION admin_delete_rating(text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_delete_rating(text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_delete_rating(text) TO service_role;

-- signature: admin_manually_flag_dispute(text,text)
CREATE OR REPLACE FUNCTION public.admin_manually_flag_dispute(p_problem_id text, p_reason text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
  v_reason_text text := trim(coalesce(p_reason, ''));
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if v_problem.is_disputed then
    return jsonb_build_object('result', 'ALREADY_DISPUTED');
  end if;

  update public.problems set
    is_disputed = true,
    dispute_reason = v_reason_text,
    dispute_initiator_id = null,
    dispute_initiator_role = 'ADMIN',
    disputed_at = v_now,
    last_activity_at = v_now
  where id = p_problem_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    '⚠️ অ্যাডমিন দ্বারা বিরোধ (Dispute) ফ্ল্যাগ করা হয়েছে',
    'অ্যাডমিন \"' || v_problem.title || '\" কাজের বিষয়টি বিরোধ হিসেবে চিহ্নিত করেছেন। কারণ: ' || v_reason_text,
    'problem', p_problem_id, p_problem_id, 'USER', v_now);

  if v_problem.accepted_solver_id is not null then
    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
      '⚠️ অ্যাডমিন দ্বারা বিরোধ (Dispute) ফ্ল্যাগ করা হয়েছে',
      'অ্যাডমিন \"' || v_problem.title || '\" কাজের বিষয়টি বিরোধ হিসেবে চিহ্নিত করেছেন। কারণ: ' || v_reason_text,
      'problem', p_problem_id, p_problem_id, 'SOLVER', v_now);
  end if;

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_manually_flag_dispute(text,text) TO "-";
GRANT EXECUTE ON FUNCTION admin_manually_flag_dispute(text,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_manually_flag_dispute(text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_manually_flag_dispute(text,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_manually_flag_dispute(text,text) TO service_role;

-- signature: admin_send_message_to_problem_chat(text,text,uuid)
CREATE OR REPLACE FUNCTION public.admin_send_message_to_problem_chat(p_problem_id text, p_content text, p_receiver_id uuid DEFAULT NULL::uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
  v_msg_id text := 'MSG_' || replace(gen_random_uuid()::text, '-', '');
  v_content text := trim(coalesce(p_content, ''));
  v_receiver_id uuid;
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'ADMIN_ONLY';
  end if;
  if v_content = '' then
    raise exception 'CONTENT_REQUIRED';
  end if;

  select * into v_problem from public.problems where id = p_problem_id;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  -- [ধাপ ৭ ফিক্স] receiver এখন ঐচ্ছিকভাবে owner বা accepted_solver_id হতে পারে (আগে সবসময়
  -- owner হার্ডকোডেড ছিল)। p_receiver_id না দিলে (NULL) আগের মতোই owner-এ resolve হয়
  -- (backward-compatible, existing caller-দের কোনো পরিবর্তন লাগবে না)। validation করা হচ্ছে
  -- যাতে admin শুধু problem owner বা এর accepted solver-কেই টার্গেট করতে পারে, arbitrary user না।
  if p_receiver_id is null then
    v_receiver_id := v_problem.user_id;
  elsif p_receiver_id = v_problem.user_id or p_receiver_id = v_problem.accepted_solver_id then
    v_receiver_id := p_receiver_id;
  else
    raise exception 'INVALID_RECEIVER';
  end if;

  insert into public.messages (
    id, problem_id, sender_id, receiver_id, sender_name, content, "timestamp",
    is_read, is_admin_message
  ) values (
    v_msg_id, p_problem_id, auth.uid(), v_receiver_id, 'Support Manager 🛡️', v_content, v_now,
    false, true
  );

  update public.problems
    set is_admin_involved_in_chat = true, last_activity_at = v_now
    where id = p_problem_id;

  return jsonb_build_object('result', 'OK', 'id', v_msg_id);
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_send_message_to_problem_chat(text,text,uuid) TO "-";
GRANT EXECUTE ON FUNCTION admin_send_message_to_problem_chat(text,text,uuid) TO anon;
GRANT EXECUTE ON FUNCTION admin_send_message_to_problem_chat(text,text,uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_send_message_to_problem_chat(text,text,uuid) TO postgres;
GRANT EXECUTE ON FUNCTION admin_send_message_to_problem_chat(text,text,uuid) TO service_role;

-- signature: admin_broadcast_notification(text,text,text,text,timestamp with time zone)
CREATE OR REPLACE FUNCTION public.admin_broadcast_notification(p_target_role text, p_title text, p_message text, p_target_type text DEFAULT 'general'::text, p_scheduled_for timestamp with time zone DEFAULT NULL::timestamp with time zone)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_count int;
  v_role text := upper(coalesce(p_target_role, 'ALL'));
begin
  if not is_admin(auth.uid()) then
    raise exception 'ADMIN_ONLY';
  end if;
  if p_title is null or trim(p_title) = '' then
    raise exception 'TITLE_REQUIRED';
  end if;
  if v_role not in ('ALL', 'USER', 'SOLVER') then
    raise exception 'INVALID_TARGET_ROLE';
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, scheduled_for, "timestamp")
  select
    'NOTIF_ADMIN_' || replace(gen_random_uuid()::text, '-', ''),
    u.id,
    trim(p_title),
    trim(p_message),
    coalesce(p_target_type, 'general'),
    null,
    null,
    p_scheduled_for,
    now()
  from public.users u
  where v_role = 'ALL' or u.role = v_role;

  get diagnostics v_count = row_count;
  return jsonb_build_object('result', 'OK', 'count', v_count);
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_broadcast_notification(text,text,text,text,timestamp with time zone) TO "-";
GRANT EXECUTE ON FUNCTION admin_broadcast_notification(text,text,text,text,timestamp with time zone) TO anon;
GRANT EXECUTE ON FUNCTION admin_broadcast_notification(text,text,text,text,timestamp with time zone) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_broadcast_notification(text,text,text,text,timestamp with time zone) TO postgres;
GRANT EXECUTE ON FUNCTION admin_broadcast_notification(text,text,text,text,timestamp with time zone) TO service_role;

-- signature: admin_notify_user(uuid,text,text,text,text,text)
CREATE OR REPLACE FUNCTION public.admin_notify_user(p_user_id uuid, p_title text, p_message text, p_target_type text DEFAULT 'general'::text, p_target_id text DEFAULT NULL::text, p_related_problem_id text DEFAULT NULL::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_id text := 'NOTIF_' || replace(gen_random_uuid()::text, '-', '');
begin
  if not is_admin(auth.uid()) then
    raise exception 'ADMIN_ONLY';
  end if;
  if p_user_id is null then
    raise exception 'USER_ID_REQUIRED';
  end if;
  if p_title is null or trim(p_title) = '' then
    raise exception 'TITLE_REQUIRED';
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  values (v_id, p_user_id, trim(p_title), trim(p_message), coalesce(p_target_type, 'general'), p_target_id, p_related_problem_id, now());

  return jsonb_build_object('result', 'OK', 'id', v_id);
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_notify_user(uuid,text,text,text,text,text) TO "-";
GRANT EXECUTE ON FUNCTION admin_notify_user(uuid,text,text,text,text,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_notify_user(uuid,text,text,text,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_notify_user(uuid,text,text,text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_notify_user(uuid,text,text,text,text,text) TO service_role;

-- signature: admin_notify_user(uuid,text,text,text,text,text,text)
CREATE OR REPLACE FUNCTION public.admin_notify_user(p_user_id uuid, p_title text, p_message text, p_target_type text DEFAULT 'general'::text, p_target_id text DEFAULT NULL::text, p_related_problem_id text DEFAULT NULL::text, p_role text DEFAULT ''::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_id text := 'NOTIF_' || replace(gen_random_uuid()::text, '-', '');
begin
  if not is_admin(auth.uid()) then
    raise exception 'ADMIN_ONLY';
  end if;
  if p_user_id is null then
    raise exception 'USER_ID_REQUIRED';
  end if;
  if p_title is null or trim(p_title) = '' then
    raise exception 'TITLE_REQUIRED';
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
  values (v_id, p_user_id, trim(p_title), trim(p_message), coalesce(p_target_type, 'general'), p_target_id, p_related_problem_id, coalesce(p_role, ''), now());

  return jsonb_build_object('result', 'OK', 'id', v_id);
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_notify_user(uuid,text,text,text,text,text,text) TO "-";
GRANT EXECUTE ON FUNCTION admin_notify_user(uuid,text,text,text,text,text,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_notify_user(uuid,text,text,text,text,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_notify_user(uuid,text,text,text,text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_notify_user(uuid,text,text,text,text,text,text) TO service_role;

-- signature: admin_reassign_solver(text,uuid,text)
CREATE OR REPLACE FUNCTION public.admin_reassign_solver(p_problem_id text, p_solver_id uuid, p_solver_name text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  update public.problems
  set accepted_solver_id = p_solver_id,
      accepted_solver_name = p_solver_name,
      last_activity_at = v_now
  where id = p_problem_id;

  -- ইচ্ছাকৃতভাবে কোনো notification insert করা হয় না -- Kotlin-সাইডে existing local+cloud
  -- (create_notification RPC, দুই পক্ষকে) dual-write দিয়েই আলাদাভাবে হয়, ধাপ ২-এর
  -- admin_update_problem_budget-এর মতোই।

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_reassign_solver(text,uuid,text) TO "-";
GRANT EXECUTE ON FUNCTION admin_reassign_solver(text,uuid,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_reassign_solver(text,uuid,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_reassign_solver(text,uuid,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_reassign_solver(text,uuid,text) TO service_role;

-- signature: admin_remove_category_from_solvers(text)
CREATE OR REPLACE FUNCTION public.admin_remove_category_from_solvers(p_category_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_caller uuid := auth.uid();
  v_rows int;
begin
  if v_caller is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if not is_admin(v_caller) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.users u
  set solver_categories = coalesce((
        select string_agg(trim(x), ',')
        from unnest(string_to_array(u.solver_categories, ',')) as x
        where trim(x) <> '' and trim(x) <> p_category_id
      ), ''),
      updated_at = now()
  where u.solver_categories is not null
    and u.solver_categories <> ''
    and exists (
      select 1
      from unnest(string_to_array(u.solver_categories, ',')) as x
      where trim(x) = p_category_id
    );

  get diagnostics v_rows = row_count;

  return jsonb_build_object('result', 'OK', 'rows_affected', v_rows);
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_remove_category_from_solvers(text) TO "-";
GRANT EXECUTE ON FUNCTION admin_remove_category_from_solvers(text) TO anon;
GRANT EXECUTE ON FUNCTION admin_remove_category_from_solvers(text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_remove_category_from_solvers(text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_remove_category_from_solvers(text) TO service_role;

-- signature: admin_update_direct_contract_status(text,text,text)
CREATE OR REPLACE FUNCTION public.admin_update_direct_contract_status(p_problem_id text, p_status text, p_direct_contract_status text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_escrow public.escrows%rowtype;
  v_now timestamptz := now();
  v_inner jsonb := null;
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  if p_status = 'COMPLETED' and v_problem.status <> 'COMPLETED' then
    select * into v_escrow from public.escrows where problem_id = p_problem_id and status = 'HELD' order by created_at desc limit 1 for update;
    if found then
      v_inner := public.release_escrow(v_escrow.id);
    end if;
    update public.problems set direct_contract_status = p_direct_contract_status, last_activity_at = v_now where id = p_problem_id;
  elsif p_status = 'CANCELLED' and v_problem.status <> 'CANCELLED' then
    select * into v_escrow from public.escrows where problem_id = p_problem_id and status = 'HELD' order by created_at desc limit 1 for update;
    if found then
      v_inner := public.refund_escrow_once(v_escrow.id, 'ADMIN_DIRECT_CONTRACT_CANCEL', 100);
    end if;
    update public.problems set status = 'CANCELLED', direct_contract_status = p_direct_contract_status, last_activity_at = v_now where id = p_problem_id;
  else
    update public.problems set status = p_status, direct_contract_status = p_direct_contract_status, last_activity_at = v_now where id = p_problem_id;
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    'ডাইরেক্ট চুক্তি স্ট্যাটাস আপডেট',
    '\"' || v_problem.title || '\" ডাইরেক্ট চুক্তির স্ট্যাটাস পরিবর্তন করে ''' || p_status || ''' করা হয়েছে।',
    'problem', p_problem_id, p_problem_id, 'USER', v_now);

  if v_problem.accepted_solver_id is not null then
    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
      'ডাইরেক্ট চুক্তি স্ট্যাটাস আপডেট',
      '\"' || v_problem.title || '\" ডাইরেক্ট চুক্তির স্ট্যাটাস পরিবর্তন করে ''' || p_status || ''' করা হয়েছে।',
      'problem', p_problem_id, p_problem_id, 'SOLVER', v_now);
  end if;

  return jsonb_build_object('result', 'OK', 'inner', v_inner);
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_update_direct_contract_status(text,text,text) TO "-";
GRANT EXECUTE ON FUNCTION admin_update_direct_contract_status(text,text,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_update_direct_contract_status(text,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_update_direct_contract_status(text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_update_direct_contract_status(text,text,text) TO service_role;

-- signature: admin_update_problem_budget(text,numeric,numeric)
CREATE OR REPLACE FUNCTION public.admin_update_problem_budget(p_problem_id text, p_min_budget numeric, p_max_budget numeric)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  update public.problems
  set min_budget = p_min_budget,
      max_budget = p_max_budget,
      last_activity_at = v_now
  where id = p_problem_id;

  -- ইচ্ছাকৃতভাবে কোনো notification insert করা হয় না -- মাস্টার প্রম্পটের ধাপ ২ স্পেক অনুযায়ী owner-notify
  -- Kotlin-সাইডে existing local+cloud (create_notification RPC) dual-write দিয়েই আলাদাভাবে হয়।

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_update_problem_budget(text,numeric,numeric) TO "-";
GRANT EXECUTE ON FUNCTION admin_update_problem_budget(text,numeric,numeric) TO anon;
GRANT EXECUTE ON FUNCTION admin_update_problem_budget(text,numeric,numeric) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_update_problem_budget(text,numeric,numeric) TO postgres;
GRANT EXECUTE ON FUNCTION admin_update_problem_budget(text,numeric,numeric) TO service_role;

-- signature: admin_update_problem_status(text,text)
CREATE OR REPLACE FUNCTION public.admin_update_problem_status(p_problem_id text, p_status text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  update public.problems set status = p_status, last_activity_at = v_now where id = p_problem_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    'সমস্যার স্ট্যাটাস আপডেট',
    '\"' || v_problem.title || '\" সমস্যার স্ট্যাটাস পরিবর্তন করে ''' || p_status || ''' করা হয়েছে।',
    'problem', p_problem_id, p_problem_id, 'USER', v_now);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_update_problem_status(text,text) TO "-";
GRANT EXECUTE ON FUNCTION admin_update_problem_status(text,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_update_problem_status(text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_update_problem_status(text,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_update_problem_status(text,text) TO service_role;

-- signature: admin_force_cancel_instant_job(text,text,text,integer)
CREATE OR REPLACE FUNCTION public.admin_force_cancel_instant_job(p_problem_id text, p_reason text, p_target_action text DEFAULT 'CANCEL'::text, p_progress_step integer DEFAULT 1)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
  v_step int := greatest(coalesce(p_progress_step, 1), 1);
  v_had_solver boolean;
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if v_problem.status in ('COMPLETED', 'CANCELLED') then
    return jsonb_build_object('result', 'ALREADY_TERMINAL');
  end if;

  v_had_solver := v_problem.accepted_solver_id is not null;

  if v_had_solver then
    update public.bids set
      status = 'CANCELLED', progress_at_cancel = v_step, resolution_type = 'ADMIN_FORCE_ACTION', resolved_at = v_now
    where problem_id = p_problem_id
      and (id = v_problem.accepted_bid_id or solver_id = v_problem.accepted_solver_id)
      and status <> 'CANCELLED';
  end if;

  if p_target_action = 'REBROADCAST' then
    update public.problems set
      is_instant_job = true, job_status = 'BROADCASTING', status = 'OPEN',
      accepted_bid_id = null, accepted_solver_id = null, accepted_solver_name = null, accepted_amount = null,
      solver_live_lat = null, solver_live_lng = null, solver_live_updated_at = null,
      arrived_at = null, job_started_at = null,
      has_release_request = false, release_request_extra_amount = 0, release_request_note = '', release_requested_at = null,
      confirmed_extra_amount_total = 0, broadcast_timer_started_at = v_now, last_activity_at = v_now
    where id = p_problem_id;

    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
      'জরুরি জব পুনরায় ব্রডকাস্ট করা হয়েছে ⚡',
      'অ্যাডমিন কর্তৃক আপনার জরুরি জবটি নতুন করে ব্রডকাস্ট করা হয়েছে। কারণ: ' || coalesce(p_reason, ''),
      'problem', p_problem_id, p_problem_id, v_now);

    if v_problem.accepted_solver_id is not null then
      insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
      values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
        'জরুরি কাজ পুনরায় ব্রডকাস্ট করা হয়েছে (অ্যাডমিন) ⚡',
        'অ্যাডমিন কর্তৃক "' || v_problem.title || '" কাজটি পুনরায় ব্রডকাস্ট করা হয়েছে। কারণ: ' || coalesce(p_reason, ''),
        'problem', p_problem_id, p_problem_id, v_now);
    end if;

  elsif p_target_action = 'TO_NORMAL_BIDDING' then
    update public.problems set
      is_instant_job = false, job_status = null, status = 'OPEN',
      accepted_bid_id = null, accepted_solver_id = null, accepted_solver_name = null, accepted_amount = null,
      solver_live_lat = null, solver_live_lng = null, solver_live_updated_at = null,
      arrived_at = null, job_started_at = null,
      has_release_request = false, release_request_extra_amount = 0, release_request_note = '', release_requested_at = null,
      confirmed_extra_amount_total = 0, last_activity_at = v_now
    where id = p_problem_id;

    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
      'কাজটি সাধারণ বিডিংয়ে রূপান্তর করা হয়েছে 📋',
      'জরুরি জবটি সাধারণ বিডিং তালিকায় পাঠানো হয়েছে। কারণ: ' || coalesce(p_reason, ''),
      'problem', p_problem_id, p_problem_id, v_now);

    if v_problem.accepted_solver_id is not null then
      insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
      values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
        'কাজটি সাধারণ বিডিংয়ে রূপান্তর করা হয়েছে (অ্যাডমিন) 📋',
        'অ্যাডমিন কর্তৃক "' || v_problem.title || '" কাজটি সাধারণ বিডিং তালিকায় পাঠানো হয়েছে। কারণ: ' || coalesce(p_reason, ''),
        'problem', p_problem_id, p_problem_id, v_now);
    end if;

  else -- CANCEL
    update public.problems set
      status = 'CANCELLED', job_status = 'CANCELLED',
      accepted_bid_id = null,
      has_release_request = false, release_request_extra_amount = 0, release_request_note = '', release_requested_at = null,
      last_activity_at = v_now
    where id = p_problem_id;

    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
      'জরুরি কাজ বাতিল (অ্যাডমিন) ❌',
      'অ্যাডমিন কর্তৃক "' || v_problem.title || '" কাজটি বাতিল করা হয়েছে। কারণ: ' || coalesce(p_reason, ''),
      'problem', p_problem_id, p_problem_id, v_now);

    if v_problem.accepted_solver_id is not null then
      insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
      values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
        'জরুরি কাজ বাতিল (অ্যাডমিন) ❌',
        'অ্যাডমিন কর্তৃক "' || v_problem.title || '" কাজটি বাতিল করা হয়েছে। কারণ: ' || coalesce(p_reason, ''),
        'problem', p_problem_id, p_problem_id, v_now);
    end if;
  end if;

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_force_cancel_instant_job(text,text,text,integer) TO "-";
GRANT EXECUTE ON FUNCTION admin_force_cancel_instant_job(text,text,text,integer) TO anon;
GRANT EXECUTE ON FUNCTION admin_force_cancel_instant_job(text,text,text,integer) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_force_cancel_instant_job(text,text,text,integer) TO postgres;
GRANT EXECUTE ON FUNCTION admin_force_cancel_instant_job(text,text,text,integer) TO service_role;

-- signature: log_admin_action(text,text,text,text)
CREATE OR REPLACE FUNCTION public.log_admin_action(p_action_type text, p_target_id text DEFAULT ''::text, p_target_name text DEFAULT ''::text, p_details text DEFAULT ''::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_id text := 'LOG_' || (extract(epoch from now())::bigint)::text || '_' || substr(replace(gen_random_uuid()::text,'-',''),1,6);
begin
  if auth.uid() is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if p_action_type is null or trim(p_action_type) = '' then
    raise exception 'ACTION_TYPE_REQUIRED';
  end if;

  insert into public.admin_audit_logs (id, action_type, target_id, target_name, details, "timestamp")
  values (v_id, p_action_type, coalesce(p_target_id, ''), coalesce(p_target_name, ''), coalesce(p_details, ''), now());

  return jsonb_build_object('result', 'OK', 'id', v_id);
end;
$function$
;
GRANT EXECUTE ON FUNCTION log_admin_action(text,text,text,text) TO "-";
GRANT EXECUTE ON FUNCTION log_admin_action(text,text,text,text) TO anon;
GRANT EXECUTE ON FUNCTION log_admin_action(text,text,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION log_admin_action(text,text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION log_admin_action(text,text,text,text) TO service_role;

-- signature: log_admin_action(text,text,text,text,text)
CREATE OR REPLACE FUNCTION public.log_admin_action(p_action_type text, p_target_id text DEFAULT ''::text, p_target_name text DEFAULT ''::text, p_details text DEFAULT ''::text, p_role text DEFAULT ''::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_id text := 'LOG_' || (extract(epoch from now())::bigint)::text || '_' || substr(replace(gen_random_uuid()::text,'-',''),1,6);
begin
  if auth.uid() is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if p_action_type is null or trim(p_action_type) = '' then
    raise exception 'ACTION_TYPE_REQUIRED';
  end if;

  insert into public.admin_audit_logs (id, action_type, target_id, target_name, details, role, "timestamp")
  values (v_id, p_action_type, coalesce(p_target_id, ''), coalesce(p_target_name, ''), coalesce(p_details, ''), coalesce(p_role, ''), now());

  return jsonb_build_object('result', 'OK', 'id', v_id);
end;
$function$
;
GRANT EXECUTE ON FUNCTION log_admin_action(text,text,text,text,text) TO "-";
GRANT EXECUTE ON FUNCTION log_admin_action(text,text,text,text,text) TO anon;
GRANT EXECUTE ON FUNCTION log_admin_action(text,text,text,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION log_admin_action(text,text,text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION log_admin_action(text,text,text,text,text) TO service_role;

