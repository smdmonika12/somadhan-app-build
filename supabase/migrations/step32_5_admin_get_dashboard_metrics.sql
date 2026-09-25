-- [ধাপ ৩২.৫] Category-৩ গ্যাপ #৩ ফিক্স — admin dashboard server-side aggregation।
--
-- Firestore-এর count()/aggregate(sum()) সার্ভার-সাইড কোয়েরির Postgres সমতুল্য। admin-only
-- (is_admin চেক সার্ভার-সাইডে, ক্লায়েন্টের দাবির ওপর ভরসা না করে) -- পুরো users/problems/bids/
-- transactions/withdrawals টেবিল client-এ ডাউনলোড না করেই headline সংখ্যাগুলো ফেরত দেয়।

create or replace function public.admin_get_dashboard_metrics()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_total_users int;
  v_total_solvers int;
  v_total_problems int;
  v_open_problems int;
  v_completed_problems int;
  v_in_progress_problems int;
  v_total_bids int;
  v_pending_bids int;
  v_accepted_bids int;
  v_gross_sum numeric;
  v_commission_sum numeric;
  v_pending_withdrawals int;
  v_completed_withdrawals_sum numeric;
  v_category_problem_counts jsonb;
  v_category_bid_counts jsonb;
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'admin access required';
  end if;

  select count(*) into v_total_users from public.users;
  select count(*) into v_total_solvers from public.users where role = 'SOLVER';

  select count(*) into v_total_problems from public.problems;
  select
    count(*) filter (where status = 'OPEN'),
    count(*) filter (where status = 'COMPLETED'),
    count(*) filter (where status = 'IN_PROGRESS')
  into v_open_problems, v_completed_problems, v_in_progress_problems
  from public.problems;

  select count(*) into v_total_bids from public.bids;
  select
    count(*) filter (where status = 'PENDING'),
    count(*) filter (where status = 'ACCEPTED')
  into v_pending_bids, v_accepted_bids
  from public.bids;

  select coalesce(sum(gross_amount), 0), coalesce(sum(commission_amount), 0)
  into v_gross_sum, v_commission_sum
  from public.transactions;

  select count(*) filter (where status = 'PENDING') into v_pending_withdrawals from public.withdrawals;
  select coalesce(sum(amount), 0) into v_completed_withdrawals_sum
  from public.withdrawals where status = 'COMPLETED';

  select coalesce(jsonb_object_agg(category_name, cnt), '{}'::jsonb) into v_category_problem_counts
  from (
    select coalesce(nullif(category_name, ''), 'অন্যান্য') as category_name, count(*) as cnt
    from public.problems
    group by 1
  ) t;

  select coalesce(jsonb_object_agg(category_name, cnt), '{}'::jsonb) into v_category_bid_counts
  from (
    select coalesce(nullif(p.category_name, ''), 'সাধারণ') as category_name, count(*) as cnt
    from public.bids b
    join public.problems p on p.id = b.problem_id
    group by 1
  ) t;

  return jsonb_build_object(
    'total_users', v_total_users,
    'total_solvers', v_total_solvers,
    'total_problems', v_total_problems,
    'open_problems', v_open_problems,
    'completed_problems', v_completed_problems,
    'in_progress_problems', v_in_progress_problems,
    'total_bids', v_total_bids,
    'pending_bids', v_pending_bids,
    'accepted_bids', v_accepted_bids,
    'total_transaction_volume', v_gross_sum,
    'platform_revenue', v_commission_sum,
    'pending_withdrawals', v_pending_withdrawals,
    'completed_withdrawals', v_completed_withdrawals_sum,
    'category_problem_counts', v_category_problem_counts,
    'category_bid_counts', v_category_bid_counts
  );
end;
$$;

grant execute on function public.admin_get_dashboard_metrics() to authenticated;
