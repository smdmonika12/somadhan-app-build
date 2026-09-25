-- মানি-ফ্লো ফিক্স, ধাপ ৪ — `resolveCommissionRateForNewJob()`-এর caller-mismatch গ্যাপ বন্ধ।
--
-- আগে ক্লায়েন্ট সরাসরি `users` টেবিলে `.update()` করতো (SupabaseSyncManager.syncFreeJobQuota),
-- যেটা RLS-এর কারণে শুধু caller-এর *নিজের* row-এর জন্যই কাজ করে। কিন্তু `acceptBid()`-এ caller
-- হয় job owner (poster), টার্গেট হয় solver-এর ফ্রি-কোটা কাউন্টার -- ফলে সেই কল-সাইট থেকে sync
-- silently কোনো row না বদলিয়েই "সফল" রিটার্ন করতো, আর solver-এর `free_jobs_used_this_month`/
-- `free_jobs_month_key` কখনো cloud-এ পৌঁছাত না।
--
-- এই RPC সার্ভার-সাইডে যাচাই করে caller আসলেই ওই problem-এর owner/accepted-solver/admin কিনা
-- (আর p_solver_id সত্যিই সেই problem-এর accepted solver কিনা), তারপর টার্গেট solver-এর কোটা-
-- কাউন্টার আপডেট করে। এতে owner (acceptBid) আর solver নিজে (acceptDirectContractProposal) --
-- দুই caller-ই একইভাবে কাজ করতে পারবে।
--
-- ⚠️ এই ফাইলটা এই সেশনে নতুন লেখা হয়েছে, কোনো লাইভ DB থেকে sync করা statement না (অন্য migration
-- ফাইলগুলোর মতো) -- Supabase project-এ apply করার আগে column নাম/টাইপ (users.id, problems.id/
-- user_id/accepted_solver_id, is_admin() ফাংশনের existing signature) একবার নিজে মিলিয়ে
-- দেখে নাও, কারণ এই zip-এ থাকা migrations ফোল্ডার আংশিক (step20+ থেকে শুরু) বলে পুরো স্কিমা
-- সরাসরি verify করা যায়নি।

create or replace function public.sync_solver_free_job_quota(
  p_problem_id text,
  p_solver_id uuid,
  p_used_count integer,
  p_month_key text
)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_caller uuid := auth.uid();
  v_problem record;
begin
  if v_caller is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if p_problem_id is null or p_solver_id is null or p_used_count is null or p_month_key is null then
    raise exception 'MISSING_PARAMS';
  end if;

  select id, user_id, accepted_solver_id into v_problem
  from public.problems
  where id = p_problem_id;

  if v_problem.id is null then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  if v_problem.accepted_solver_id is null or v_problem.accepted_solver_id <> p_solver_id then
    raise exception 'SOLVER_MISMATCH';
  end if;

  if not (v_caller = v_problem.user_id or v_caller = p_solver_id or is_admin(v_caller)) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.users
  set free_jobs_used_this_month = p_used_count,
      free_jobs_month_key = p_month_key,
      updated_at = now()
  where id = p_solver_id;

  return jsonb_build_object('result', 'OK', 'solver_id', p_solver_id, 'used_count', p_used_count, 'month_key', p_month_key);
end;
$$;

grant execute on function public.sync_solver_free_job_quota(text, uuid, integer, text) to authenticated;
