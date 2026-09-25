-- ধাপ ৩২.৮৫ (৩২.৮ আর ৩২.৯-এর মাঝে, আগের সেশনে চিহ্নিত "১৪-১৫ নম্বর" গ্যাপের অংশ) —
-- markDisputeResultSeen() / markCompletionResultSeen() — শুধু UI "দেখা হয়েছে" ফ্ল্যাগ,
-- টাকা/ফাংশনালিটি প্রভাব নেই, কিন্তু Firebase সরালে cross-device sync হারাবে (আগের
-- session-এর অডিটে চিহ্নিত)।
--
-- ⚠️ এই session-এ Supabase MCP (লাইভ DB টুল) কানেক্টেড ছিল না — তাই নিচের RLS-সংক্রান্ত
-- ধারণা MIGRATION_PROGRESS.md-এ আগে থেকে নথিভুক্ত তথ্য + কোডবেসের established প্যাটার্নের
-- ওপর ভিত্তি করে করা, লাইভ pg_policies যাচাই করে না। পরের session প্রথমে Supabase MCP দিয়ে
-- এই ধারণাগুলো再confirm করবে (rule #১১)।
--
-- কেন RPC লাগলো, সরাসরি .update() না: `problems` টেবিলে `problems_update_owner`
-- (auth.uid() = user_id) পোস্টদাতাকে পুরো row লিখতে দেয়, কিন্তু সলভারের জন্য কোনো UPDATE
-- policy নেই (step23_update_solver_live_location_rpc.sql-এ একই কারণে RPC বানানো হয়েছিল)।
-- isUser=true হলে caller পোস্টদাতা, isUser=false হলে caller accepted solver -- দুই ক্ষেত্রেই
-- কভার করতে single SECURITY DEFINER RPC, ভেতরে auth.uid() দিয়ে caller যাচাই করে সঠিক
-- কলাম লেখে।

create or replace function public.mark_dispute_result_seen(
  p_problem_id text,
  p_is_user boolean
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

  if p_is_user then
    if auth.uid() is null or auth.uid() <> v_problem.user_id then
      raise exception 'NOT_AUTHORIZED';
    end if;
    update public.problems set dispute_result_seen_by_user = true where id = p_problem_id;
  else
    if auth.uid() is null or auth.uid() <> v_problem.accepted_solver_id then
      raise exception 'NOT_AUTHORIZED';
    end if;
    update public.problems set dispute_result_seen_by_solver = true where id = p_problem_id;
  end if;

  return jsonb_build_object('result', 'OK');
end;
$$;

grant execute on function public.mark_dispute_result_seen(text, boolean) to authenticated;

create or replace function public.mark_completion_result_seen(
  p_problem_id text,
  p_is_user boolean
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

  if p_is_user then
    if auth.uid() is null or auth.uid() <> v_problem.user_id then
      raise exception 'NOT_AUTHORIZED';
    end if;
    update public.problems set completion_result_seen_by_user = true where id = p_problem_id;
  else
    if auth.uid() is null or auth.uid() <> v_problem.accepted_solver_id then
      raise exception 'NOT_AUTHORIZED';
    end if;
    update public.problems set completion_result_seen_by_solver = true where id = p_problem_id;
  end if;

  return jsonb_build_object('result', 'OK');
end;
$$;

grant execute on function public.mark_completion_result_seen(text, boolean) to authenticated;
