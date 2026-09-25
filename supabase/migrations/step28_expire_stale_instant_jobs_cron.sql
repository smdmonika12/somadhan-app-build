-- ধাপ ২৮ — Instant Job Expire: pg_cron sweep
--
-- এই মাইগ্রেশন সরাসরি Supabase MCP (Supabase:apply_migration) দিয়ে লাইভ প্রজেক্টে
-- (mghvvpndkxnscwryfkib) apply করা হয়েছে এই session-এই (পার্ট ২-এর সংশোধিত নিয়ম #১৩
-- অনুযায়ী)। এই ফাইলটা শুধু ইতিহাস/রোলব্যাক-রেফারেন্সের জন্য repo-তে রাখা হচ্ছে —
-- ব্যবহারকারীর নিজে থেকে এটা আবার চালানোর দরকার নেই।
--
-- উদ্দেশ্য: `checkAndExpireInstantJobs()` (Kotlin, client-triggered) আগে থেকেই চালু
-- আছে এবং অপরিবর্তিত থাকছে (rule #২)। কিন্তু সেটা owner-app-না-খোলা অবস্থায় কখনো
-- cloud-এ reflect হয় না। এই নতুন `expire_stale_instant_jobs()` একটা independent,
-- caller-scoping ছাড়া (system-wide sweep) সার্ভার-সাইড ব্যাকস্টপ, যেটা pg_cron দিয়ে
-- প্রতি ৫ মিনিটে (app-এর নিজস্ব ৩০০-সেকেন্ড ডিফল্ট টাইমআউটের সাথে মিলিয়ে) নিজে থেকেই
-- চলবে — কোনো ইউজারের ডিভাইসের ওপর নির্ভর না করে।

create extension if not exists pg_cron with schema extensions;

create or replace function public.expire_stale_instant_jobs()
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
  v_timeout_seconds bigint := coalesce(
    (select value::bigint from public.platform_settings where key = 'instant_job_broadcast_timeout_seconds'),
    300
  );
  v_problem record;
  v_bid record;
  v_now timestamptz := now();
  v_expired_count int := 0;
begin
  -- `expire_broadcasting_instant_job` RPC (ধাপ ১২ ব্যাচ ৪গ) এর মতোই একই এলিজিবিলিটি
  -- শর্ত ও একই cancellation/notification লজিক — শুধু auth.uid() = owner চেক নেই
  -- (কারণ এটা cron দিয়ে system-wide চলে, কোনো caller session থাকে না), আর একটা
  -- problem না, সব eligible broadcasting job একসাথে লুপ করে।
  for v_problem in
    select * from public.problems
    where is_instant_job = true
      and job_status = 'BROADCASTING'
      and is_user_deleted = false
      and status not in ('CANCELLED', 'COMPLETED')
      and (solver_cancelled_notice is null or solver_cancelled_notice = '')
      and coalesce(broadcast_timer_started_at, created_at) + (v_timeout_seconds || ' seconds')::interval <= v_now
    for update skip locked
  loop
    update public.problems set
      job_status = 'CANCELLED',
      is_user_deleted = true,
      status = 'CANCELLED',
      last_activity_at = v_now
    where id = v_problem.id;

    -- progress_at_cancel = 1 হার্ডকোড করা হয়েছে: BROADCASTING অবস্থার জব মানেই এখনো কোনো
    -- সলভার accept করেনি (accepted_solver_id/on_way_at/... সব null) — তাই
    -- ProblemEntity.calculateProgressStep() এই কেসে সবসময় 1 রিটার্ন করবে
    -- (Kotlin কোডে যাচাই করা হয়েছে), হার্ডকোড করাটা যুক্তিসঙ্গত।
    for v_bid in
      update public.bids set status = 'CANCELLED', progress_at_cancel = 1, resolution_type = 'EXPIRED', resolved_at = v_now
      where problem_id = v_problem.id and status = 'PENDING'
      returning solver_id
    loop
      insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
      values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_bid.solver_id,
        'জরুরি জবটি বাতিল হয়েছে ⏱️',
        '"' || v_problem.title || '" কাজের সময়সীমা শেষ হওয়ায় পোস্টটি বাতিল হয়ে গেছে।',
        'problem', v_problem.id, v_problem.id, v_now);
    end loop;

    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
      'জরুরি পোস্ট বাতিল ⏱️',
      'কোনো সলভার সময়মতো বিড না দেওয়ায় আপনার জরুরি পোস্টটি বাতিল হয়ে গেছে।',
      'problem', v_problem.id, v_problem.id, v_now);

    v_expired_count := v_expired_count + 1;
  end loop;

  return jsonb_build_object('expired_count', v_expired_count);
end;
$function$;

-- ক্লায়েন্ট থেকে সরাসরি কল করার জন্য না — শুধু cron/superuser-এর জন্য।
revoke all on function public.expire_stale_instant_jobs() from public, anon, authenticated;

-- প্রতি ৫ মিনিটে চলবে (app-এর নিজস্ব ৩০০-সেকেন্ড ডিফল্ট টাইমআউটের সাথে সামঞ্জস্যপূর্ণ)।
select cron.schedule(
  'expire-stale-instant-jobs',
  '*/5 * * * *',
  $$select public.expire_stale_instant_jobs();$$
);
