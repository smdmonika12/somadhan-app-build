-- ধাপ ৩২.৮৫ — markProblemSeen() — "১৪-১৫ নম্বর" গ্যাপের অংশ (ছোট syncProblem কল, আগের
-- session-এর অডিটে "হয়তো redundant/dead" হিসেবে চিহ্নিত ছিল, এই session-এ লাইন-বাই-লাইন
-- দেখে নিশ্চিত হলো এটা redundant না — user_last_seen_at/solver_last_seen_at আসলে UI-তে
-- "শেষ কবে দেখেছে" দেখাতে ব্যবহৃত হয়, cross-device sync দরকার)।
--
-- একই কারণে RPC (owner update নিজের row পুরোটাই পারে, কিন্তু solver-এর কোনো UPDATE policy
-- নেই -- step23_update_solver_live_location_rpc.sql-এর মতোই)।

create or replace function public.mark_problem_seen(
  p_problem_id text,
  p_role text
)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_problem public.problems%rowtype;
begin
  select * into v_problem from public.problems where id = p_problem_id;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  if upper(p_role) = 'SOLVER' then
    if auth.uid() is null or auth.uid() <> v_problem.accepted_solver_id then
      raise exception 'NOT_AUTHORIZED';
    end if;
    update public.problems set solver_last_seen_at = now() where id = p_problem_id;
  else
    if auth.uid() is null or auth.uid() <> v_problem.user_id then
      raise exception 'NOT_AUTHORIZED';
    end if;
    update public.problems set user_last_seen_at = now() where id = p_problem_id;
  end if;

  return jsonb_build_object('result', 'OK');
end;
$$;

grant execute on function public.mark_problem_seen(text, text) to authenticated;
