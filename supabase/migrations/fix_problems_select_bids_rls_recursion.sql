-- আগের migration (problems_select_allow_ended_bid_solver) সরাসরি bids টেবিলে EXISTS
-- subquery করছিল, যা bids_select RLS ট্রিগার করে -- আর bids_select নিজেই problems-এ
-- EXISTS subquery করে -- ফলে "infinite recursion detected in policy for relation problems"।
-- ফিক্স: is_admin()-এর মতোই একটা SECURITY DEFINER হেল্পার ফাংশন দিয়ে bids চেক করা, যেটা RLS
-- বাইপাস করে (owner হিসেবে চলে), তাই recursion হয় না।

create or replace function public.solver_has_ended_bid(p_problem_id text, p_solver_id uuid)
returns boolean
language sql
stable security definer
set search_path to 'public'
as $$
  select exists (
    select 1 from public.bids b
    where b.problem_id = p_problem_id
      and b.solver_id = p_solver_id
      and b.status in ('CANCELLED', 'WITHDRAWN', 'REJECTED')
  );
$$;

alter policy problems_select on public.problems
using (
  (auth.uid() = user_id)
  OR (auth.uid() = accepted_solver_id)
  OR is_admin(auth.uid())
  OR ((status = 'OPEN'::text) AND (is_public = true) AND (is_user_deleted = false))
  OR public.solver_has_ended_bid(id, auth.uid())
);
