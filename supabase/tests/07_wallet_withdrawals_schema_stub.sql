-- 07_wallet_withdrawals_schema_stub.sql
-- ⚠️ TEMPORARY / INFERRED — CI_TEST_SUITE_MASTER_PROMPT.md rule #6।
--
-- Step 4 PART 2 (request_withdrawal, process_withdrawal)-এর জন্য একমাত্র নতুন
-- টেবিল — `withdrawals` — migrations-এ কোনো CREATE TABLE নেই (আগের সব টেবিলের
-- মতোই, সরাসরি live Supabase project-এ বানানো হয়েছিল)। কলামগুলো
-- `step38_request_withdrawal_client_id.sql` (request_withdrawal, ৮-argument
-- চূড়ান্ত সংজ্ঞা — পুরনো ৭-argument overload `step38b_...sql`-এ DROP হয়ে গেছে)
-- এবং `step36_transaction_role_column_and_rpc_dual_write.sql` (process_withdrawal)
-- — এই দুটো ফাংশনের real body থেকে reverse-engineer করে বসানো হয়েছে, অনুমান
-- করে না।
--
-- ⚠️ এই stub-এর ভিত্তিতে চলা যেকোনো টেস্টের ফলাফল (pass বা fail) সম্ভাব্যভাবে
-- schema-mismatch-জনিত ভুল হতে পারে। আসল `supabase db dump --schema public`
-- পাওয়া গেলে এটাকে replace করা priority (rule #6)।
--
-- users/transactions/notifications/platform_settings — Step 4 PART 1 এবং
-- আগের ধাপগুলোর stub-এ (01, 05) যা আছে তাই যথেষ্ট, এখানে নতুন কিছু লাগেনি:
--   users.balance / balance_user / balance_solver / has_user_role /
--     has_solver_role (01 + 05 stub)
--   transactions.escrow_id / role (01 stub) — request_withdrawal-এর
--     TRX_WD_DEDUCT_ insert-এ escrow_id কলামে আসলে withdrawal id-ই লেখা হয়
--     (column পুনর্ব্যবহার, নতুন কলাম না)
--   platform_settings (key, value) — min_withdrawal key; row না থাকলে
--     function নিজেই coalesce(..., 100) ডিফল্ট ধরে, তাই খালি টেবিলেও ভাঙবে না

CREATE TABLE IF NOT EXISTS public.withdrawals (
  id                    text PRIMARY KEY,
  solver_id             uuid NOT NULL REFERENCES public.users(id),  -- আসলে যেকোনো role (USER/SOLVER)-এর withdrawer, নাম historical
  solver_name           text,
  amount                numeric(12,2) NOT NULL,
  method                text,
  account_number        text,
  bank_name             text,
  branch_name           text,
  account_holder_name   text,
  status                text NOT NULL DEFAULT 'PENDING',  -- PENDING | COMPLETED | REJECTED
  role                  text,                              -- 'USER' | 'SOLVER' — request_withdrawal-এ সেট হয়
  created_at            timestamptz NOT NULL DEFAULT now(),
  trx_id                text,
  rejection_reason      text
);

GRANT ALL ON public.withdrawals TO anon, authenticated, service_role;
