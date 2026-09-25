-- ============================================================================
-- step12_9_drop_old_notification_log_reputation_overloads_part_a   [Step 12.9a — migration-ফাইল-সিঙ্ক, Step 12.12]
-- ============================================================================
-- এটা live Supabase প্রজেক্টে (mghvvpndkxnscwryfkib) ইতিমধ্যে apply-করা migration
-- `20260921052510 step12_9_drop_old_notification_log_reputation_overloads_part_a`-এর repo-কপি। live-ই সত্যের উৎস: নিচের SQL live pg_proc (MCP,
-- ২০২৬-০৯-২১) থেকে টেনে যাচাই করা — প্রতিটা ফাংশন-বডির md5(prosrc) live-এর সাথে বাইট-বাই-বাইট মিলেছে।
-- কী বদলায়: পুরনো (p_role-বিহীন) ৩টা overload DROP — admin_notify_user (৬-arg), create_notification (৬-arg), log_admin_action (৪-arg); নতুন p_role-সহ overload-ই একমাত্র signature থাকে।
-- ⚠️ ফাইলের নামের `zz_<live-version>_` prefix ইচ্ছাকৃত: CI/setup script `ls supabase/migrations/*.sql | sort`
--    (অক্ষরক্রম) অনুযায়ী apply করে, timestamp অনুযায়ী না — `step12_*` নাম `step36_*`-এর আগে বসত, আর তখন
--    step36-এর পুরনো বডি এই বদলকে fresh-DB-তে overwrite করত। `zz_` সবার শেষে বসায়, live-এর ক্রম বজায় থাকে।
-- ⚠️ live-এ এটা আবার চালানো নিরাপদ (CREATE OR REPLACE / DROP IF EXISTS) — কিন্তু দরকার নেই, live আগেই apply-করা।
-- ============================================================================
DROP FUNCTION IF EXISTS public.admin_notify_user(uuid, text, text, text, text, text);
DROP FUNCTION IF EXISTS public.create_notification(uuid, text, text, text, text, text);
DROP FUNCTION IF EXISTS public.log_admin_action(text, text, text, text);
