-- zz_20260913145059_users_broadcast_event_naming_fix_ci_order.sql
--
-- ⚠️ Step 13.1/13.2 ফিক্স (২০২৬-০৯-২২, ব্যবহারকারীর স্পষ্ট অনুমতিতে — "13.2 er sathe eta fix
-- kore deo") — এটা **কোনো নতুন লজিক-পরিবর্তন না**, লাইভ Supabase-এ (project
-- `mghvvpndkxnscwryfkib`) `notify_users_broadcast()`-এর body ইতিমধ্যেই সঠিক (নিচের এই একই
-- function-বডি — `mcp__Supabase__execute_sql` দিয়ে সরাসরি `pg_proc.prosrc` পড়ে বাইট-বাই-বাইট
-- মিলিয়ে নিশ্চিত করা হয়েছে, ২০২৬-০৯-২২)। সমস্যাটা শুধু **CI/sandbox-এর migration-reconstruction
-- order**-এ:
--
--   live-এ প্রকৃত (chronological, `list_migrations` থেকে) ক্রম:
--     1. realtime_scoping_step5_users_escrows_broadcast  (version 20260913144826)
--        — bare TG_OP দিয়ে notify_users_broadcast() প্রথমবার বানায়
--     2. fix_users_broadcast_event_naming_collision      (version 20260913145059)
--        — 'users_'||TG_OP-এ ঠিক করে (এই ফাইলটাই এই migration-এর ভিত্তি)
--
--   কিন্তু CI/sandbox (full-test.yml, scripts/setup_test_env.sh,
--   scripts/local_pgtap_bootstrap.sh — তিনটাই `ls supabase/migrations/*.sql | sort`,
--   alphabetical) `fix_users_broadcast_event_naming_collision.sql` ('f'...)-কে
--   `realtime_scoping_step5_users_escrows_broadcast.sql` ('r'...)-এর **আগে** চালায়
--   (বর্ণানুক্রমে 'f' < 'r') — ফলে step5 সবার শেষে গিয়ে function-টা আবার bare TG_OP-তে
--   ওভাররাইট করে ফেলে। এই সমস্যাটা ঠিক Step 12.12-এ যে কারণে `zz_`-প্রিফিক্স ট্রিক ব্যবহার
--   করা হয়েছিল তারই পুনরাবৃত্তি (বিস্তারিত: CI_TEST_SUITE_PROGRESS.md-এর "Step 12.12" ও
--   "Step 13.1 সম্পূর্ণ" সেকশন) — `zz_` প্রিফিক্স নিশ্চিত করে এই ফাইলটা alphabetically সবার
--   শেষে চলবে, তাই CI-র reconstruction চূড়ান্তভাবে live-এর সাথে মেলে।
--
-- **live-এ এই migration আবার apply করার দরকার নেই** — live ইতিমধ্যেই সঠিক (উপরে বলা হয়েছে),
-- এই ফাইলটা শুধু CI/sandbox-এর disposable/ephemeral DB reconstruction-এর জন্য।

create or replace function public.notify_users_broadcast()
returns trigger
language plpgsql
security definer
set search_path to ''
as $function$
begin
  perform realtime.broadcast_changes(
    'user:' || coalesce(new.id, old.id)::text,
    'users_' || TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, new, old
  );
  return null;
end;
$function$;
