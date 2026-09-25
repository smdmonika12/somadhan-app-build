-- ধাপ ৩২.৭ — adminResetSolverFreeQuota(solverId)-এর সমতুল্য: admin অন্য একজন solver-এর মাসিক
-- ফ্রি-কোটা রিসেট করে। এটা লাইভ DB-তে "_fix" migration হিসেবে দ্বিতীয়বার apply হওয়া চূড়ান্ত
-- ভার্সন (প্রথম ভার্সনের সাথে লজিক অভিন্ন, শুধু ভেরিয়েবল নাম v_found → v_rows কসমেটিক রিনেম —
-- কোনো money-bug ছিল না, ধাপ ৩২.৬-এর ডুপ্লিকেট-এন্ট্রি ইস্যুর মতো নয়)।

create or replace function public.admin_reset_free_job_quota(p_solver_id uuid)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_caller uuid := auth.uid();
  v_month_key text := to_char(now(), 'YYYY-MM');
  v_rows int;
begin
  if v_caller is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if not is_admin(v_caller) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.users
  set free_jobs_used_this_month = 0,
      free_jobs_month_key = v_month_key,
      updated_at = now()
  where id = p_solver_id;

  get diagnostics v_rows = row_count;
  if v_rows = 0 then
    raise exception 'SOLVER_NOT_FOUND';
  end if;

  return jsonb_build_object('result', 'OK', 'solver_id', p_solver_id, 'month_key', v_month_key);
end;
$$;

grant execute on function public.admin_reset_free_job_quota(uuid) to anon, authenticated;
