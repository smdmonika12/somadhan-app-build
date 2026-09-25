-- ধাপ ৩২.৯৫ (ব্যবহারকারীর অনুরোধে, ৩৩ শুরুর আগে) — clearSolverCancelledNotice() গ্যাপ ফিক্স।
--
-- সলভার instant-job বাতিল করার পর owner "পুনরায় broadcast করো" চাপলে এই ফাংশন কল হয় (caller
-- সবসময় problem owner -- JobTrackingScreen.kt-এ যাচাই করা হয়েছে)। problem-এর accepted-বিড
-- সংক্রান্ত ফিল্ড রিসেট করে + broadcast টাইমার নতুন করে শুরু করে, আর আগের accepted/cancelled
-- সলভারদের বিড strictly CANCELLED করে দেয় (Kotlin `clearSolverCancelledNotice()`-এর লজিকের
-- সাথে হুবহু মিলিয়ে)। bids টেবিলে owner-scoped UPDATE RLS policy নেই বলে (bids-এর সব
-- status-পরিবর্তন RPC দিয়েই হয়, acceptBid/userDeleteProblem-এর মতো), এটাও একই প্যাটার্নে single
-- SECURITY DEFINER RPC-তে করা হলো যাতে problem + bids একই transaction-এ বদলায়।
create or replace function public.clear_solver_cancelled_notice(
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
  v_prev_accepted_solver_id uuid;
  v_prev_accepted_bid_id text;
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  if auth.uid() is null or auth.uid() <> v_problem.user_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  v_prev_accepted_solver_id := v_problem.accepted_solver_id;
  v_prev_accepted_bid_id := v_problem.accepted_bid_id;

  update public.problems set
    solver_cancelled_notice = null,
    accepted_bid_id = null,
    accepted_solver_id = null,
    accepted_solver_name = null,
    accepted_amount = null,
    broadcast_timer_started_at = v_now,
    last_activity_at = v_now
  where id = p_problem_id;

  -- Kotlin সাইডের cancelledSolverIds সেট: যেসব সলভারের বিড আগে থেকেই CANCELLED/WITHDRAWN/
  -- REJECTED, প্লাস আগের accepted solver (যদি থাকে) -- এদের সব বিড, আর যেকোনো ACCEPTED বিড,
  -- আর prevAcceptedBidId -- সব CANCELLED করে দেওয়া হয়।
  update public.bids set status = 'CANCELLED'
  where problem_id = p_problem_id
    and (
      status in ('ACCEPTED', 'CANCELLED', 'WITHDRAWN', 'REJECTED')
      or id = v_prev_accepted_bid_id
      or solver_id = v_prev_accepted_solver_id
      or solver_id in (
        select b2.solver_id from public.bids b2
        where b2.problem_id = p_problem_id
          and b2.status in ('CANCELLED', 'WITHDRAWN', 'REJECTED')
      )
    );

  return jsonb_build_object('result', 'OK');
end;
$$;

grant execute on function public.clear_solver_cancelled_notice(text) to authenticated;
