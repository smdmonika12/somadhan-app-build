-- ধাপ ৩২.৭ — adminResetSolverMissCycle(solverId)-এর সমতুল্য: admin অন্য একজন solver-এর Extra Bill
-- মিস-সাইকেল কাউন্টার (cycleJobCount/cycleMissCount) রিসেট করে। লাইভ DB থেকে হুবহু sync করা।

create or replace function public.admin_reset_miss_cycle(p_solver_id uuid)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $$
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

  update public.users
  set cycle_job_count = 0,
      cycle_miss_count = 0,
      updated_at = now()
  where id = p_solver_id;

  get diagnostics v_rows = row_count;
  if v_rows = 0 then
    raise exception 'SOLVER_NOT_FOUND';
  end if;

  return jsonb_build_object('result', 'OK', 'solver_id', p_solver_id);
end;
$$;

grant execute on function public.admin_reset_miss_cycle(uuid) to anon, authenticated;
