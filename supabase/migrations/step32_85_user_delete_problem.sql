-- ধাপ ৩২.৮৫ — userDeleteProblem() — "১৪-১৫ নম্বর" গ্যাপের অংশ।
--
-- পোস্টদাতা নিজের OPEN (কোনো bid accept হয়নি এমন) পোস্ট soft-delete করে (is_user_deleted=true,
-- status=CANCELLED) এবং সেই পোস্টের সব বিড CANCELLED করে দেয়। শেষ অংশটা (অন্যের -- অর্থাৎ
-- সলভারদের -- বিড বাতিল করা) `bids` টেবিলে কোনো owner-scoped UPDATE RLS policy দিয়ে সম্ভব
-- না (কোডবেসে bids-এর সব status-পরিবর্তন ইতিমধ্যেই RPC দিয়ে হয় -- acceptBid/cancelBid/
-- rejectBid), তাই এটাও একই প্যাটার্নে single SECURITY DEFINER RPC-তে করা হলো, যাতে problem
-- row + তার সব bid row একই transaction-এ বদলায়।
--
-- auth.uid() থেকেই owner যাচাই হয় -- caller নিজের user_id parameter দিয়ে spoof করতে পারবে না।

create or replace function public.user_delete_problem(
  p_problem_id text
)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  if auth.uid() is null or auth.uid() <> v_problem.user_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  if v_problem.status <> 'OPEN'
     or v_problem.accepted_bid_id is not null
     or (v_problem.accepted_solver_id is not null) then
    raise exception 'NOT_DELETABLE';
  end if;

  update public.problems set
    is_user_deleted = true,
    status = 'CANCELLED',
    job_status = case when v_problem.is_instant_job then 'CANCELLED' else v_problem.job_status end,
    solver_cancelled_notice = null,
    last_activity_at = v_now
  where id = p_problem_id;

  update public.bids set
    status = 'CANCELLED'
  where problem_id = p_problem_id
    and status not in ('CANCELLED', 'REJECTED');

  return jsonb_build_object('result', 'OK');
end;
$$;

grant execute on function public.user_delete_problem(text) to authenticated;
