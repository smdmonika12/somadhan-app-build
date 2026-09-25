-- ধাপ ২৩ — লাইভ GPS: update_solver_live_location RPC
--
-- এই মাইগ্রেশন এই session-এ Supabase MCP (Supabase:apply_migration) দিয়ে সরাসরি
-- ইতিমধ্যে LIVE DB-তে apply করা হয়েছে এবং apply-পরবর্তী verify-query দিয়ে নিশ্চিত করা
-- হয়েছে (pg_proc + information_schema.role_routine_grants)। এই ফাইলটা শুধু
-- ইতিহাস/রোলব্যাক-রেফারেন্সের জন্য রাখা হলো (নিয়ম #১৩)।
--
-- কেন দরকার হলো: problems টেবিলের বিদ্যমান RLS UPDATE policy শুধুই
-- (problems_update_owner: auth.uid() = user_id) পোস্টদাতাকে UPDATE করতে দেয়, আর
-- (problems_update_admin) শুধু admin-কে। accepted_solver_id-এর জন্য কোনো UPDATE policy
-- নেই -- মানে সলভার সরাসরি postgrest .update() দিয়ে তার নিজের লাইভ GPS লোকেশন লিখতে
-- পারতো না (RLS ব্লক করত)। সাধারণ একটা নতুন "solver can update own accepted problem"
-- policy দিলে সলভার পুরো row-ই (status/amount ইত্যাদি সহ) লিখতে পারতো, যেটা নিরাপদ না --
-- তাই money-related টেবিলের প্রতিষ্ঠিত প্যাটার্নে (RPC দিয়ে column-scoped write) এই
-- SECURITY DEFINER RPC বানানো হলো, শুধু solver_live_lat/solver_live_lng/
-- solver_live_updated_at (+ last_activity_at) কলাম লিখে, ভেতরে auth.uid() =
-- accepted_solver_id চেক করে।

create or replace function public.update_solver_live_location(
  p_problem_id text,
  p_lat double precision,
  p_lng double precision
)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  if auth.uid() is null or auth.uid() <> v_problem.accepted_solver_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  if v_problem.status in ('COMPLETED', 'CANCELLED') then
    return jsonb_build_object('result', 'ALREADY_TERMINAL');
  end if;

  update public.problems set
    solver_live_lat = p_lat,
    solver_live_lng = p_lng,
    solver_live_updated_at = v_now,
    last_activity_at = v_now
  where id = p_problem_id;

  return jsonb_build_object('result', 'OK');
end;
$function$;

grant execute on function public.update_solver_live_location(text, double precision, double precision) to authenticated;
