-- 05_job_release_escrow_schema_stub.sql
-- ⚠️ TEMPORARY / INFERRED — CI_TEST_SUITE_MASTER_PROMPT.md rule #6।
--
-- Step 3 (Job release & escrow) PART 1-এর জন্য schema stub extension।
-- কোনো পুরোনো stub ভাঙা হয়নি — শুধু ALTER TABLE ... ADD COLUMN IF NOT EXISTS /
-- CREATE TABLE IF NOT EXISTS (idempotent)। এই ফাইল `*_schema_stub.sql` glob-এ ধরা পড়ে
-- বলে scripts/run_tests.sh এটা 01_..._schema_stub.sql-এর পরেই (sorted order) apply করে।
--
-- প্রতিটা কলাম নিচের ফাংশনগুলোর আসল body থেকে verify করে বসানো হয়েছে (অনুমান না):
--   cancel_job_release_request / reject_job_release_request / request_job_release
--     → supabase/migrations/recovered_job_release.sql
--   release_escrow / refund_escrow_once
--     → supabase/migrations/step36_transaction_role_column_and_rpc_dual_write.sql
--       (recovered_money_flow.sql-এর পুরোনো সংজ্ঞা override করে)
--
-- ⚠️ additional_charges-এর এখানে শুধু PART 1-এ লাগা কলামগুলো আছে
-- (id, problem_id, solver_id, user_id, reason, amount, status, created_at)।
-- PART 2-এর ফাংশনগুলো (respond_additional_charge, mark_additional_charge_settled ইত্যাদি)
-- আরও কলাম (যেমন responded_at) ব্যবহার করে — PART 2 সেশনে সেগুলোর আসল body থেকে verify
-- করে এখানে ALTER দিয়ে যোগ করতে হবে।

-- cancel_job_release_request dispute reset করার সময় এই কলামগুলো null/false করে
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS dispute_initiator_role text;
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS disputed_at timestamptz;
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS is_admin_involved_in_chat boolean NOT NULL DEFAULT false;
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS admin_assistance_requested_by uuid;
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS admin_assistance_requested_at timestamptz;

-- release_escrow solver-role balance-এ (balance_solver) dual-write করে
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS balance_solver numeric(12,2) NOT NULL DEFAULT 0;

-- release_escrow-এর transactions insert-এ এই ৩টা কলাম লাগে
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS base_amount numeric(12,2);
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS extra_amount numeric(12,2);
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS release_type text;

-- request_job_release insert করে, reject_job_release_request ACCEPTED-গুলোর sum পড়ে।
-- FK ইচ্ছাকৃতভাবে রাখা হয়নি (real constraints অজানা — অনুমান না করার জন্য)।
CREATE TABLE IF NOT EXISTS public.additional_charges (
  id          text PRIMARY KEY,
  problem_id  text,
  solver_id   uuid,
  user_id     uuid,
  reason      text,
  amount      numeric(12,2),
  status      text,          -- PENDING | ACCEPTED | REJECTED (REJECTED PART 2-এ verify করা হলো)
  created_at  timestamptz NOT NULL DEFAULT now()
);
GRANT ALL ON public.additional_charges TO anon, authenticated, service_role;

-- ============================================================================
-- Step 3 PART 2 সেশন (2026-09-19) — request_extra_amount, respond_additional_charge,
-- increment_escrow_extra_amount, record_gateway_payment_log,
-- mark_additional_charge_settled, system_track_extra_payment_miss — এই ৬টার জন্য
-- extend করা হলো (rule #6, আগের কিছু না ভেঙে)। প্রতিটা কলাম আসল function body থেকে
-- verify করা (recovered_money_flow.sql-এর request_extra_amount; step36_...sql-এর
-- respond_additional_charge; step32_7_increment_escrow_extra_amount.sql;
-- step32_7_record_gateway_payment_log.sql; step29_mark_additional_charge_settled.sql —
-- এখানেই কমেন্টে স্পষ্ট লেখা আছে `settled_at` না, `responded_at` reuse হয়;
-- step32_7_system_track_extra_payment_miss.sql)।
-- ============================================================================

-- respond_additional_charge()/mark_additional_charge_settled() দুটোই এই কলামে লেখে
-- (settled_at নামে আলাদা কলাম নেই — step29-এর কমেন্টেই নিশ্চিত করা)
ALTER TABLE public.additional_charges ADD COLUMN IF NOT EXISTS responded_at timestamptz;

-- record_gateway_payment_log() এই কলামে লেখে (gateway_payments-এ আগে ছিল না)
ALTER TABLE public.gateway_payments ADD COLUMN IF NOT EXISTS role text;

-- system_track_extra_payment_miss() এই দুটো কলামে লেখে
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS cycle_job_count integer NOT NULL DEFAULT 0;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS cycle_miss_count integer NOT NULL DEFAULT 0;
