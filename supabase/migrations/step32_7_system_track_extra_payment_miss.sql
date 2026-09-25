-- ধাপ ৩২.৭ — trackExtraPaymentMissCycle(solverId, ...)-এর সমতুল্য: সিস্টেম-ট্রিগার্ড
-- bookkeeping-only sync (caller solver নিজে নাও হতে পারে)। caller-scoping নিয়ম #১১-এর মতো,
-- system_notify_48hour_auto_release-এর প্যাটার্ন অনুসরণ করে। লাইভ DB থেকে হুবহু sync করা।

create or replace function public.system_track_extra_payment_miss(
  p_solver_id uuid,
  p_new_job_count int,
  p_new_miss_count int
)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_caller uuid := auth.uid();
  v_solver record;
begin
  if v_caller is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if p_solver_id is null or p_new_job_count is null or p_new_miss_count is null then
    raise exception 'MISSING_PARAMS';
  end if;
  if p_new_job_count < 0 or p_new_miss_count < 0 then
    raise exception 'INVALID_PARAMS';
  end if;

  select id, has_solver_role into v_solver from public.users where id = p_solver_id;
  if v_solver.id is null then
    raise exception 'SOLVER_NOT_FOUND';
  end if;
  if not coalesce(v_solver.has_solver_role, false) then
    raise exception 'SOLVER_ROLE_INACTIVE';
  end if;

  -- সিস্টেম-ট্রিগার্ড bookkeeping-only sync: caller solver নিজে নাও হতে পারে (problem owner-এর
  -- ডিভাইস থেকেও job-completion flow-এ ট্রিগার হয়) — তাই caller==solver ম্যাচ করানো হয়নি, শুধু
  -- authenticated + solver row বাস্তব ও active কিনা যাচাই করা হয়েছে (রুল #১১, system_notify_
  -- 48hour_auto_release-এর caller-scoping প্যাটার্ন অনুসরণ করে)।
  update public.users
  set cycle_job_count = p_new_job_count,
      cycle_miss_count = p_new_miss_count,
      updated_at = now()
  where id = p_solver_id;

  return jsonb_build_object('result', 'OK', 'solver_id', p_solver_id);
end;
$$;

grant execute on function public.system_track_extra_payment_miss(uuid, int, int) to anon, authenticated;
