-- RECOVERED from live DB on 2026-09-18, verbatim capture, no logic changes. See
-- RPC_SYNC_FIX_PROGRESS.md Step 2.
-- Functions in this file (Step 1 missing-RPC group): create_notification, notify_admins

-- signature: create_notification(uuid,text,text,text,text,text)
CREATE OR REPLACE FUNCTION public.create_notification(p_target_user_id uuid, p_title text, p_message text, p_target_type text DEFAULT 'general'::text, p_target_id text DEFAULT NULL::text, p_related_problem_id text DEFAULT NULL::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_id text := 'NOTIF_' || replace(gen_random_uuid()::text, '-', '');
  v_caller uuid := auth.uid();
  v_owner uuid;
  v_solver uuid;
  v_caller_is_party boolean := false;
  v_target_is_party boolean := false;
begin
  if v_caller is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if p_target_user_id is null then
    raise exception 'TARGET_USER_ID_REQUIRED';
  end if;
  if p_title is null or trim(p_title) = '' then
    raise exception 'TITLE_REQUIRED';
  end if;

  if is_admin(v_caller) then
    -- admin: any target allowed
    null;
  elsif v_caller = p_target_user_id then
    -- self-notify (e.g. wallet deposit confirmation)
    null;
  elsif p_related_problem_id is not null then
    select user_id, accepted_solver_id into v_owner, v_solver
    from public.problems where id = p_related_problem_id;

    if v_owner is null then
      raise exception 'PROBLEM_NOT_FOUND';
    end if;

    v_caller_is_party := (v_caller = v_owner) or (v_caller = v_solver)
      or exists (select 1 from public.bids b where b.problem_id = p_related_problem_id and b.solver_id = v_caller);
    v_target_is_party := (p_target_user_id = v_owner) or (p_target_user_id = v_solver)
      or exists (select 1 from public.bids b where b.problem_id = p_related_problem_id and b.solver_id = p_target_user_id);

    if not (v_caller_is_party and v_target_is_party) then
      raise exception 'NOT_AUTHORIZED';
    end if;
  else
    raise exception 'NOT_AUTHORIZED';
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  values (v_id, p_target_user_id, trim(p_title), trim(p_message), coalesce(p_target_type, 'general'), p_target_id, p_related_problem_id, now());

  return jsonb_build_object('result', 'OK', 'id', v_id);
end;
$function$
;
GRANT EXECUTE ON FUNCTION create_notification(uuid,text,text,text,text,text) TO "-";
GRANT EXECUTE ON FUNCTION create_notification(uuid,text,text,text,text,text) TO anon;
GRANT EXECUTE ON FUNCTION create_notification(uuid,text,text,text,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION create_notification(uuid,text,text,text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION create_notification(uuid,text,text,text,text,text) TO service_role;

-- signature: create_notification(uuid,text,text,text,text,text,text)
CREATE OR REPLACE FUNCTION public.create_notification(p_target_user_id uuid, p_title text, p_message text, p_target_type text DEFAULT 'general'::text, p_target_id text DEFAULT NULL::text, p_related_problem_id text DEFAULT NULL::text, p_role text DEFAULT ''::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_id text := 'NOTIF_' || replace(gen_random_uuid()::text, '-', '');
  v_caller uuid := auth.uid();
  v_owner uuid;
  v_solver uuid;
  v_caller_is_party boolean := false;
  v_target_is_party boolean := false;
begin
  if v_caller is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if p_target_user_id is null then
    raise exception 'TARGET_USER_ID_REQUIRED';
  end if;
  if p_title is null or trim(p_title) = '' then
    raise exception 'TITLE_REQUIRED';
  end if;

  if is_admin(v_caller) then
    null;
  elsif v_caller = p_target_user_id then
    null;
  elsif p_related_problem_id is not null then
    select user_id, accepted_solver_id into v_owner, v_solver
    from public.problems where id = p_related_problem_id;

    if v_owner is null then
      raise exception 'PROBLEM_NOT_FOUND';
    end if;

    v_caller_is_party := (v_caller = v_owner) or (v_caller = v_solver)
      or exists (select 1 from public.bids b where b.problem_id = p_related_problem_id and b.solver_id = v_caller);
    v_target_is_party := (p_target_user_id = v_owner) or (p_target_user_id = v_solver)
      or exists (select 1 from public.bids b where b.problem_id = p_related_problem_id and b.solver_id = p_target_user_id);

    if not (v_caller_is_party and v_target_is_party) then
      raise exception 'NOT_AUTHORIZED';
    end if;
  else
    raise exception 'NOT_AUTHORIZED';
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
  values (v_id, p_target_user_id, trim(p_title), trim(p_message), coalesce(p_target_type, 'general'), p_target_id, p_related_problem_id, coalesce(p_role, ''), now());

  return jsonb_build_object('result', 'OK', 'id', v_id);
end;
$function$
;
GRANT EXECUTE ON FUNCTION create_notification(uuid,text,text,text,text,text,text) TO "-";
GRANT EXECUTE ON FUNCTION create_notification(uuid,text,text,text,text,text,text) TO anon;
GRANT EXECUTE ON FUNCTION create_notification(uuid,text,text,text,text,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION create_notification(uuid,text,text,text,text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION create_notification(uuid,text,text,text,text,text,text) TO service_role;

-- signature: notify_admins(text,text,text)
CREATE OR REPLACE FUNCTION public.notify_admins(p_title text, p_message text, p_related_problem_id text DEFAULT NULL::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_count int;
begin
  if auth.uid() is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if p_title is null or trim(p_title) = '' then
    raise exception 'TITLE_REQUIRED';
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  select
    'NOTIF_ADMIN_' || replace(gen_random_uuid()::text, '-', ''),
    u.id,
    trim(p_title),
    trim(p_message),
    'problem',
    p_related_problem_id,
    p_related_problem_id,
    now()
  from public.users u
  where u.role = 'ADMIN';

  get diagnostics v_count = row_count;
  return jsonb_build_object('result', 'OK', 'count', v_count);
end;
$function$
;
GRANT EXECUTE ON FUNCTION notify_admins(text,text,text) TO "-";
GRANT EXECUTE ON FUNCTION notify_admins(text,text,text) TO anon;
GRANT EXECUTE ON FUNCTION notify_admins(text,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION notify_admins(text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION notify_admins(text,text,text) TO service_role;

