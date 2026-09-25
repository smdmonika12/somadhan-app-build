-- 08_disputes_schema_stub.sql — Step 5 (Disputes) schema extension
--
-- rule #6 (TEMPORARY/INFERRED, আসল `supabase db dump --schema public` না
-- পাওয়া পর্যন্ত) অনুযায়ী — আগের ধাপের stub-গুলো (01, 05, 07) না ভেঙে শুধু
-- ALTER TABLE দিয়ে extend করা হলো।
--
-- ⚠️ এই সেশনে একটা প্রকৃত schema-stub bug ধরা পড়েছে এবং ফিক্স করা হলো:
-- `01_bidding_flow_schema_stub.sql`-এ `dispute_progress_at_settlement` ভুলভাবে
-- `text` টাইপে declare করা হয়েছিল (কোনো migration তখনো এই কলাম ছুঁয়নি বলে
-- অনুমান ধরেই লেখা হয়েছিল)। কিন্তু `step29_5_resolve_dispute_split.sql`
-- (resolve_dispute_split RPC) সরাসরি
-- `coalesce(p_progress_at_settlement, dispute_progress_at_settlement)` করে,
-- যেখানে `p_progress_at_settlement` প্যারামিটার `integer` টাইপ — আর Postgres-এ
-- `COALESCE(integer, text)` কোনো implicit cast পায় না (assignment-cast, না
-- implicit), তাই real Postgres-এ এটা "COALESCE types integer and text cannot
-- be matched" error দিত (এই সেশনে সত্যিই psql-এ চালিয়ে ধরা পড়েছে)। ফিক্স:
-- এখানে কলামটা `integer`-এ বদলে দেওয়া হলো (নিচে ALTER দিয়ে)। `dispute_progress_at_raise`
-- এখনো কোথাও type-sensitive ভাবে ছোঁয়া হয় না (শুধু null করা হয়, migrations
-- grep করে নিশ্চিত করা হয়েছে) — তাই text-ই থাকল, ভবিষ্যতে কোনো ধাপে সত্যিই
-- মান বসানো লাগলে তখন আবার verify করতে হবে।
ALTER TABLE public.problems ALTER COLUMN dispute_progress_at_settlement TYPE integer USING NULL;

-- resolve_dispute (SPLIT_SETTLEMENT branch) আর resolve_dispute_split — দুটোই
-- এই কলাম লেখে (Step 5 PART 2-এর স্কোপ, কিন্তু কলামটা এখানেই যোগ করে রাখা
-- হলো যাতে schema stub একবারই বদলাতে হয়)।
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS dispute_split_solver_percent numeric;

-- resolve_dispute (সব resolution branch) সেট করে — মূল migration
-- (recovered_disputes.sql) থেকে verify করা, boolean, default false।
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS dispute_result_seen_by_user boolean NOT NULL DEFAULT false;
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS dispute_result_seen_by_solver boolean NOT NULL DEFAULT false;

-- admin_manually_flag_dispute / raise_dispute / settle_dispute / resolve_dispute
-- বাকি সব কলাম (is_disputed, dispute_reason, dispute_initiator_id,
-- dispute_initiator_role, disputed_at, dispute_settled_at, dispute_resolved_at,
-- dispute_resolution_decision/_type/_note, title, user_id, user_name,
-- accepted_solver_id, accepted_solver_name, last_activity_at) আগের stub-গুলোতে
-- (01, 05) আগে থেকেই আছে — নতুন কিছু লাগেনি।
