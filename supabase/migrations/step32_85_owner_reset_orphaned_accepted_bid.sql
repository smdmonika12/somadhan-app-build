-- ধাপ ৩২.৮৫ — ownerResetOrphanedAcceptedBid() — "১৪-১৫ নম্বর" গ্যাপের অংশ।
--
-- Kotlin-সাইড ফাংশনের docstring অনুযায়ী এটা শুধু তখনই reset করে যখন "provably no money is
-- at stake" (কোনো escrow নেই, বা escrow-এ কিছুই আটকে নেই) আর post আগে থেকে disputed/settled
-- না। এই গার্ডগুলো এখন পর্যন্ত শুধু client-সাইড (Kotlin) চেক করা হচ্ছিল -- এই RPC-তে সেই
-- একই গার্ড সার্ভার-সাইডেও বসানো হলো (client-কে বিশ্বাস না করে), কারণ এটা টাকা-সংক্রান্ত
-- একটা reset (acceptedBidId/acceptedSolverId নাল করে দেয়)।
--
-- auth.uid() থেকেই owner যাচাই হয় (problems_update_owner পলিসির সাথে সামঞ্জস্যপূর্ণ,
-- caller নিজের owner_id parameter দিয়ে spoof করতে পারবে না)।

create or replace function public.owner_reset_orphaned_accepted_bid(
  p_problem_id text
)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_problem public.problems%rowtype;
  v_escrow public.escrows%rowtype;
  v_locked_amount double precision := 0;
  v_now timestamptz := now();
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  if auth.uid() is null or auth.uid() <> v_problem.user_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  if v_problem.accepted_solver_id is null then
    return jsonb_build_object('result', 'NOT_ACCEPTED');
  end if;

  if v_problem.is_disputed
     or v_problem.status = 'COMPLETED'
     or v_problem.status = 'CANCELLED' then
    return jsonb_build_object('result', 'NOT_ELIGIBLE');
  end if;

  select * into v_escrow from public.escrows where problem_id = p_problem_id;
  if found and v_escrow.status not in ('RELEASED', 'REFUNDED') then
    v_locked_amount := coalesce(v_escrow.base_amount, 0) + coalesce(v_escrow.extra_amount, 0);
  end if;

  if v_locked_amount > 0 then
    -- সত্যিকারের টাকা লক করা আছে -- এটা orphaned না, normal dispute flow ব্যবহার করতে হবে।
    return jsonb_build_object('result', 'FUNDS_LOCKED');
  end if;

  update public.problems set
    status = 'OPEN',
    job_status = case when v_problem.is_instant_job then 'BROADCASTING' else v_problem.job_status end,
    broadcast_timer_started_at = case when v_problem.is_instant_job then v_now else v_problem.broadcast_timer_started_at end,
    accepted_bid_id = null,
    accepted_solver_id = null,
    accepted_solver_name = null,
    accepted_amount = null,
    solver_live_lat = null,
    solver_live_lng = null,
    solver_live_updated_at = null,
    arrived_at = null,
    job_started_at = null,
    has_release_request = false,
    release_request_extra_amount = 0,
    release_request_note = '',
    release_requested_at = null,
    solver_cancelled_notice = null,
    last_activity_at = v_now
  where id = p_problem_id;

  return jsonb_build_object('result', 'OK');
end;
$$;

grant execute on function public.owner_reset_orphaned_accepted_bid(text) to authenticated;
