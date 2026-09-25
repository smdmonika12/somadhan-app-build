-- RECOVERED from live DB on 2026-09-18, verbatim capture, no logic changes. See
-- RPC_SYNC_FIX_PROGRESS.md Step 2.
-- Functions in this file (Step 1 missing-RPC group): switch_role_get_or_create_linked_profile

-- signature: switch_role_get_or_create_linked_profile(text,text)
CREATE OR REPLACE FUNCTION public.switch_role_get_or_create_linked_profile(p_target_role text, p_solver_categories text DEFAULT NULL::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_uid uuid := auth.uid();
  v_root public.users%rowtype;
begin
  if v_uid is null then
    raise exception 'NOT_AUTHENTICATED';
  end if;
  if p_target_role not in ('USER', 'SOLVER') then
    raise exception 'INVALID_ROLE';
  end if;

  select * into v_root from public.users where id = v_uid;
  if not found then
    raise exception 'ROOT_ACCOUNT_NOT_FOUND';
  end if;

  update public.users set
    has_user_role = true,
    has_solver_role = (p_target_role = 'SOLVER' or has_solver_role),
    solver_categories = case when p_target_role = 'SOLVER'
        then coalesce(nullif(p_solver_categories, ''), nullif(solver_categories, ''))
        else solver_categories end,
    has_completed_solver_setup = case when p_target_role = 'SOLVER' then true else has_completed_solver_setup end,
    updated_at = now()
  where id = v_uid;

  select * into v_root from public.users where id = v_uid;

  return jsonb_build_object(
    'id', v_root.id,
    'created', false,
    'balance_user', v_root.balance_user,
    'balance_solver', v_root.balance_solver,
    'reputation_score_user', v_root.reputation_score_user,
    'reputation_score_solver', v_root.reputation_score_solver,
    'is_banned_user', v_root.is_banned_user,
    'is_banned_solver', v_root.is_banned_solver,
    'is_restricted_user', v_root.is_restricted_user,
    'is_restricted_solver', v_root.is_restricted_solver,
    'solver_categories', v_root.solver_categories,
    'has_completed_solver_setup', v_root.has_completed_solver_setup
  );
end;
$function$
;
GRANT EXECUTE ON FUNCTION switch_role_get_or_create_linked_profile(text,text) TO "-";
GRANT EXECUTE ON FUNCTION switch_role_get_or_create_linked_profile(text,text) TO anon;
GRANT EXECUTE ON FUNCTION switch_role_get_or_create_linked_profile(text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION switch_role_get_or_create_linked_profile(text,text) TO postgres;
GRANT EXECUTE ON FUNCTION switch_role_get_or_create_linked_profile(text,text) TO service_role;

