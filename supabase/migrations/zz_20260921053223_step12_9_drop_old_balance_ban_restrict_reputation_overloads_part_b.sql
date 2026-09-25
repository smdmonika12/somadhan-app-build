-- ============================================================================
-- step12_9_drop_old_balance_ban_restrict_reputation_overloads_part_b   [Step 12.9b — migration-ফাইল-সিঙ্ক, Step 12.12]
-- ============================================================================
-- এটা live Supabase প্রজেক্টে (mghvvpndkxnscwryfkib) ইতিমধ্যে apply-করা migration
-- `20260921053223 step12_9_drop_old_balance_ban_restrict_reputation_overloads_part_b`-এর repo-কপি। live-ই সত্যের উৎস: নিচের SQL live pg_proc (MCP,
-- ২০২৬-০৯-২১) থেকে টেনে যাচাই করা — প্রতিটা ফাংশন-বডির md5(prosrc) live-এর সাথে বাইট-বাই-বাইট মিলেছে।
-- কী বদলায়: পুরনো (p_role-বিহীন) ৪টা overload DROP — admin_adjust_balance (৪-arg), admin_set_banned (২-arg), admin_set_restricted (২-arg), submit_reputation_event (৫-arg)।
-- ⚠️ ফাইলের নামের `zz_<live-version>_` prefix ইচ্ছাকৃত: CI/setup script `ls supabase/migrations/*.sql | sort`
--    (অক্ষরক্রম) অনুযায়ী apply করে, timestamp অনুযায়ী না — `step12_*` নাম `step36_*`-এর আগে বসত, আর তখন
--    step36-এর পুরনো বডি এই বদলকে fresh-DB-তে overwrite করত। `zz_` সবার শেষে বসায়, live-এর ক্রম বজায় থাকে।
-- ⚠️ live-এ এটা আবার চালানো নিরাপদ (CREATE OR REPLACE / DROP IF EXISTS) — কিন্তু দরকার নেই, live আগেই apply-করা।
-- ============================================================================
DROP FUNCTION IF EXISTS public.admin_adjust_balance(uuid, numeric, boolean, text);
DROP FUNCTION IF EXISTS public.admin_set_banned(uuid, boolean);
DROP FUNCTION IF EXISTS public.admin_set_restricted(uuid, boolean);
DROP FUNCTION IF EXISTS public.submit_reputation_event(uuid, text, text, numeric, text);
