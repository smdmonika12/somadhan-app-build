-- সমাধান (Somadhan) — Supabase migration ধাপ ২১ (Realtime Foundation B)
--
-- messages/transactions/escrows/gateway_payments টেবিলে Postgres Changes (Realtime) enable
-- করার জন্য। এই সেশনে Supabase MCP দিয়ে সরাসরি DB-তে গিয়ে যাচাই করা হয়েছে (`SELECT * FROM
-- pg_publication_tables WHERE pubname = 'supabase_realtime'`) -- ফলাফল খালি, অর্থাৎ এই ৪টার
-- (বা users/problems/bids-সহ প্রজেক্টের অন্য কোনো টেবিলের) কোনোটাই এখনো `supabase_realtime`
-- publication-এ যোগ করা হয়নি -- ধাপ ২০-এর migration ফাইলটাও (step20_...sql) এখনো apply করা
-- হয়নি বলে মনে হচ্ছে।
--
-- ⚠️ গুরুত্বপূর্ণ: এই ফাইলটা এই session-এ deploy/apply করা হয়নি (এই কাজের পরিবেশে
-- network/Supabase dashboard access নেই -- পার্ট ২ এর গ্লোবাল নিয়ম #১৩ অনুযায়ী)।
-- ব্যবহারকারীকে নিজে থেকে নিচের যেকোনো একভাবে এটা চালাতে হবে (step20 ফাইলটাও যদি এখনো না
-- চালানো হয়ে থাকে, সেটাও একসাথে চালান):
--   ১. Supabase Dashboard -> SQL Editor -এ গিয়ে এই ফাইলের কনটেন্ট পেস্ট করে Run করা, অথবা
--   ২. Supabase CLI থাকলে: এই ফাইলটা প্রজেক্টের `supabase/migrations/` ফোল্ডারে রেখে
--      `supabase db push` চালানো।
--
-- Realtime enable করার পরেও RLS policy অক্ষত থাকবে -- অর্থাৎ কোনো authenticated ব্যবহারকারী
-- RLS-এ তার জন্য visible না এমন row-এর change event পাবে না। এই ৪টা টেবিলের exact SELECT
-- policy (pg_policies থেকে সরাসরি পড়া, ধাপ ২১-এ verify করা):
--   - messages: sender/receiver/admin/সংশ্লিষ্ট problem-এর owner-or-accepted-solver
--   - transactions: userId/solverId/admin
--   - escrows: userId/solverId/admin
--   - gateway_payments: userId/admin
-- চারটার কোনোটাতেই DELETE policy নেই (users/problems/bids-এর মতোই) -- অর্থাৎ client-role থেকে
-- হার্ড ডিলিট সম্ভব না, শুধু admin/service-role থেকে সরাসরি delete হলে (বিরল) DELETE event
-- আসতে পারে। বিস্তারিত: MIGRATION_PROGRESS.md-এ "ধাপ ২১" এন্ট্রি।

ALTER PUBLICATION supabase_realtime ADD TABLE
    public.messages,
    public.transactions,
    public.escrows,
    public.gateway_payments;
