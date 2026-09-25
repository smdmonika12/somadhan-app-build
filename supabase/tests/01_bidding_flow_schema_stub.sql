-- 01_bidding_flow_schema_stub.sql
-- ⚠️ TEMPORARY / INFERRED — CI_TEST_SUITE_MASTER_PROMPT.md rule #6।
--
-- আসল migrations-এ কোনো CREATE TABLE নেই core টেবিলগুলোর জন্য (users, problems,
-- bids, escrows, gateway_payments, transactions, notifications) — এগুলো সরাসরি
-- live Supabase project-এ বানানো হয়েছিল, migrations শুধু পরবর্তী ALTER/RPC ধরে।
-- তাই এই ধাপে দরকারি কলামগুলো accept_bid/cancel_bid/reject_bid/
-- solver_has_ended_bid/accept_direct_contract/decline_direct_contract/
-- user_delete_problem — এই ৭টা RPC-র (recovered_bidding_contracts.sql,
-- step32_85_user_delete_problem.sql, fix_problems_select_bids_rls_recursion.sql)
-- আসল function body থেকে reverse-engineer করে বসানো হয়েছে।
--
-- ⚠️ এই stub-এর ভিত্তিতে চলা যেকোনো টেস্টের ফলাফল (pass বা fail) সম্ভাব্যভাবে
-- schema-mismatch-জনিত ভুল হতে পারে। আসল `supabase db dump --schema public`
-- পাওয়া গেলে এটাকে replace করা priority।
--
-- পরবর্তী কোনো step-এ নতুন কলাম/টেবিল লাগলে এই ফাইল পুরোটা না বদলে
-- `ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...` দিয়ে extend করবে, বা আলাদা
-- `0N_<feature>_schema_stub.sql` ফাইল বানাবে — এই ফাইলের CREATE TABLE
-- ভাঙবে না (idempotent রাখার জন্য সবই IF NOT EXISTS)।

CREATE TABLE IF NOT EXISTS public.users (
  id               uuid PRIMARY KEY,
  role             text NOT NULL,               -- 'CLIENT' | 'SOLVER' | 'ADMIN' (inferred; app calls these USER/SOLVER elsewhere — কনফার্ম হয়নি)
  name             text,
  phone            text,
  balance          numeric(12,2) NOT NULL DEFAULT 0,   -- legacy shared balance
  balance_user     numeric(12,2) NOT NULL DEFAULT 0,   -- role-scoped USER-role balance (accept_bid এটাই ব্যবহার করে)
  has_user_role    boolean NOT NULL DEFAULT true,
  kyc_status       text,
  -- ⚠️ Step 6 PART 2 সেশনে ফিক্স হলো: আগে এখানে ভুলবশত `text` টাইপ ছিল। আসল
  -- migration `add_display_uid_generator.sql` কলামটা `bigint` হিসেবে বসায়
  -- (`ALTER TABLE ... ADD COLUMN IF NOT EXISTS display_uid bigint`), কিন্তু
  -- সেই migration চলে এই stub-এর *পরে* (`full-test.yml`-এ stub আগে, migration
  -- পরে) — তাই `IF NOT EXISTS` এই ভুল `text` কলামটাই রেখে দিত, migration কিছু
  -- বদলাতো না। ফলে `generate_unique_display_uid()`-এর ভেতরের
  -- `where display_uid between v_low and v_high` (v_low/v_high bigint) real
  -- Postgres-এ "operator does not exist: text >= bigint" দিয়ে ভেঙে যেত —
  -- এই সেশনেই generate_unique_display_uid টেস্ট করতে গিয়ে ধরা পড়েছে, আগের
  -- কোনো সেশন এই ফাংশনটা কখনো টেস্ট করেনি বলে চোখে পড়েনি। ঠিক করে `bigint`
  -- করা হলো যাতে migration-এর `ADD COLUMN IF NOT EXISTS`/`ALTER ... SET NOT
  -- NULL`/`generate_unique_display_uid()` সবই আসল টাইপের সাথে সামঞ্জস্যপূর্ণ
  -- থাকে। NOT NULL এখানে বসানো হয়নি — আসল migration নিজেই শেষে
  -- `ALTER COLUMN display_uid SET NOT NULL` করে, আর `trg_set_display_uid`
  -- (BEFORE INSERT trigger) NULL হলে auto-generate করে, তাই migration
  -- চালানোর পরে insert করা প্রতিটা row-এই এমনিতেই একটা মান বসে যায়।
  display_uid      bigint,
  linked_account_id uuid,
  updated_at       timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.problems (
  id                        text PRIMARY KEY,
  user_id                   uuid NOT NULL REFERENCES public.users(id),
  title                     text,
  status                    text NOT NULL DEFAULT 'OPEN',   -- OPEN | IN_PROGRESS | CANCELLED | ...
  job_status                text,
  is_instant_job            boolean NOT NULL DEFAULT false,
  is_public                 boolean NOT NULL DEFAULT true,
  is_user_deleted           boolean NOT NULL DEFAULT false,
  is_direct_contract        boolean NOT NULL DEFAULT false,
  direct_contract_status    text,                            -- PENDING_ACCEPTANCE | ACCEPTED | DECLINED
  accepted_bid_id           text,
  accepted_solver_id        uuid REFERENCES public.users(id),
  accepted_solver_name      text,
  accepted_amount           numeric(12,2),
  applied_commission_rate   numeric(6,2),
  solver_cancelled_notice   text,
  on_way_at                 timestamptz,
  arrived_at                timestamptz,
  job_started_at            timestamptz,
  completed_at              timestamptz,
  solver_live_lat           numeric,
  solver_live_lng           numeric,
  solver_live_updated_at    timestamptz,
  has_release_request       boolean NOT NULL DEFAULT false,
  release_request_extra_amount numeric(12,2) NOT NULL DEFAULT 0,
  release_request_note      text NOT NULL DEFAULT '',
  release_requested_at      timestamptz,
  pending_extra_amount      numeric(12,2),
  pending_extra_amount_note text,
  pending_extra_amount_requested_at timestamptz,
  confirmed_extra_amount_total numeric(12,2) NOT NULL DEFAULT 0,
  is_disputed               boolean NOT NULL DEFAULT false,
  dispute_reason            text,
  dispute_initiator_id      uuid,
  dispute_settled_at        timestamptz,
  dispute_resolution_decision text,
  dispute_resolution_type   text,
  dispute_resolution_note   text,
  dispute_resolved_at       timestamptz,
  dispute_progress_at_raise text,
  dispute_progress_at_settlement text,
  last_activity_at          timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.bids (
  id           text PRIMARY KEY,
  problem_id   text NOT NULL REFERENCES public.problems(id),
  solver_id    uuid NOT NULL REFERENCES public.users(id),
  solver_name  text,
  amount       numeric(12,2) NOT NULL,
  status       text NOT NULL DEFAULT 'PENDING',   -- PENDING | ACCEPTED | REJECTED | CANCELLED | WITHDRAWN
  resolved_at  timestamptz
);

CREATE TABLE IF NOT EXISTS public.escrows (
  id            text PRIMARY KEY,
  problem_id    text NOT NULL REFERENCES public.problems(id),
  problem_title text,
  user_id       uuid NOT NULL REFERENCES public.users(id),
  solver_id     uuid NOT NULL REFERENCES public.users(id),
  base_amount   numeric(12,2) NOT NULL DEFAULT 0,
  extra_amount  numeric(12,2) NOT NULL DEFAULT 0,
  status        text NOT NULL DEFAULT 'HELD',
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.gateway_payments (
  id            text PRIMARY KEY,
  gateway_trx_id text,
  user_id       uuid REFERENCES public.users(id),
  user_name     text,
  user_phone    text,
  amount        numeric(12,2),
  gateway       text,
  purpose       text,
  problem_id    text,
  problem_title text,
  status        text,
  note          text,
  "timestamp"   timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.transactions (
  id                  text PRIMARY KEY,
  problem_id          text,
  problem_title       text,
  user_id             uuid REFERENCES public.users(id),
  solver_id           uuid REFERENCES public.users(id),
  gross_amount        numeric(12,2),
  commission_percent  numeric(6,2),
  commission_amount   numeric(12,2),
  net_amount          numeric(12,2),
  type                text,
  role                text,   -- step36: 'USER' | 'SOLVER'
  "timestamp"         timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.notifications (
  id                  text PRIMARY KEY,
  user_id             uuid REFERENCES public.users(id),
  title               text,
  message             text,
  target_type         text,
  target_id           text,
  related_problem_id  text,
  role                text,
  "timestamp"         timestamptz NOT NULL DEFAULT now()
);

-- is_admin() / resolve_commission_rate() — rule #6-এর known blocker, migrations-এ
-- সংজ্ঞায়িত নেই। এখানে সবচেয়ে সহজ যুক্তিসঙ্গত অনুমান দিয়ে stub করা হলো:
-- is_admin সেই user-এর role='ADMIN' কিনা দেখে, resolve_commission_rate একটা
-- ফিক্সড ডেমো রেট রিটার্ন করে। **আসল লজিক নিশ্চিত না হওয়া পর্যন্ত commission-amount
-- সংক্রান্ত কোনো টেস্ট assertion-এর সংখ্যাসূচক মান বিশ্বাসযোগ্য না** — শুধু
-- "একটা rate resolve হয়েছে" এই পর্যন্ত টেস্ট করা নিরাপদ।
CREATE OR REPLACE FUNCTION public.is_admin(p_user_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.users WHERE id = p_user_id AND role = 'ADMIN'
  );
$$;

CREATE OR REPLACE FUNCTION public.resolve_commission_rate(p_solver_id uuid)
RETURNS numeric
LANGUAGE sql
STABLE
AS $$
  SELECT 10.00::numeric;
$$;

-- ============================================================================
-- Step 2 সেশন (PART 1, 2026-09-19) — Instant jobs/broadcasting টেস্টের জন্য
-- extend করা হলো (rule #6 অনুযায়ী: পুরো ফাইল rewrite না করে ALTER TABLE ...
-- ADD COLUMN IF NOT EXISTS)। কলামগুলো broadcast_instant_job/cancel_instant_job/
-- expire_broadcasting_instant_job/mark_job_started/mark_solver_arrived/
-- mark_solver_on_way — এই ৬টা ফাংশনের বডি (recovered_instant_jobs.sql) থেকে
-- reverse-engineer করা।
-- ============================================================================

ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS category_id text;
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS category_name text;
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS min_budget numeric(12,2);
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS max_budget numeric(12,2);
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS broadcast_radius_km numeric NOT NULL DEFAULT 5.0;
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS latitude numeric;
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS longitude numeric;
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS user_name text;
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS broadcast_timer_started_at timestamptz;

ALTER TABLE public.users ADD COLUMN IF NOT EXISTS latitude numeric;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS longitude numeric;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS has_solver_role boolean NOT NULL DEFAULT false;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS instant_job_notifications_enabled boolean NOT NULL DEFAULT true;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS solver_categories text NOT NULL DEFAULT '';

ALTER TABLE public.bids ADD COLUMN IF NOT EXISTS progress_at_cancel integer;
ALTER TABLE public.bids ADD COLUMN IF NOT EXISTS resolution_type text;

-- ============================================================================
-- Step 2 PART 1 সেশনে ধরা পড়া একটা systemic gap (শুধু Step 2-এর না, Step 0/1-এর
-- টেস্টগুলোতেও প্রযোজ্য — বিস্তারিত CI_TEST_SUITE_PROGRESS.md-তে):
--
-- আসল Supabase Postgres প্রজেক্টে (supabase/postgres ডকার ইমেজ) anon/authenticated/
-- service_role রোলগুলোকে ডিফল্টভাবে public schema-র সব টেবিলে GRANT দেওয়া থাকে —
-- নিরাপত্তা RLS policy দিয়ে row-level-এ হয়, table-level GRANT দিয়ে না। এই
-- inferred schema stub-এ আগে সেটা ছিল না, ফলে test.login_as('...')-এর পর সরাসরি
-- `SELECT ... FROM public.<table>` (SECURITY DEFINER ফাংশনের বাইরে, verification-এর
-- জন্য) করলে real local Postgres-এ "permission denied for table ..." error দিত (এই
-- সেশনে প্রথমবার সত্যিই psql+pgTAP দিয়ে চালিয়ে ধরা পড়েছে — আগের সেশনগুলো সম্ভবত
-- SQL শুধু ম্যানুয়ালি পড়ে verify করেছিল, বাস্তবে চালিয়ে দেখেনি)। নিচের GRANT +
-- ALTER DEFAULT PRIVILEGES real Supabase-এর behavior মিমিক করে এই গ্যাপ বন্ধ করে।
-- ============================================================================

GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO anon, authenticated, service_role;

-- ============================================================================
-- Step 2 PART 2 সেশন (2026-09-19) — বাকি ৫টা ফাংশনের (solver_cancel_job,
-- expire_stale_instant_jobs, clear_solver_cancelled_notice,
-- update_solver_live_location, sync_solver_free_job_quota) জন্য extend করা
-- হলো, rule #6 অনুযায়ী পুরোনো কিছু না ভেঙে।
-- ============================================================================

-- expire_stale_instant_jobs() coalesce(broadcast_timer_started_at, created_at)
-- ব্যবহার করে — problems টেবিলে created_at এতদিন ছিল না।
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT now();

-- sync_solver_free_job_quota(...) এই দুটো কলামে লেখে।
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS free_jobs_used_this_month integer NOT NULL DEFAULT 0;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS free_jobs_month_key text;

-- expire_stale_instant_jobs() এখান থেকে টাইমআউট (সেকেন্ড) পড়ে, না পেলে 300
-- ডিফল্ট ধরে (function নিজেই coalesce করে, তাই খালি টেবিলেও ভাঙবে না)।
CREATE TABLE IF NOT EXISTS public.platform_settings (
  key   text PRIMARY KEY,
  value text
);
GRANT ALL ON public.platform_settings TO anon, authenticated, service_role;

-- refund_escrow_once(text, text, numeric) — Step 3 (Job release & escrow)-এর
-- ফাংশন, এখনো ঐ ধাপ শুরু হয়নি, কিন্তু `solver_cancel_job` (এই ধাপের স্কোপে)
-- ভেতরে এটা কল করে। এই বডিটা **অনুমান করে লেখা না** — আসল, বর্তমানে সবচেয়ে
-- সাম্প্রতিক migration (`supabase/migrations/step36_transaction_role_column_and_rpc_dual_write.sql`,
-- যেটা `recovered_money_flow.sql`-এর আগের সংজ্ঞাকে CREATE OR REPLACE দিয়ে
-- override করে — ফাইলনাম sort-এ 'r' < 's' বলে migrations প্রয়োগের ক্রমেও এটাই
-- চূড়ান্ত সংস্করণ) থেকে হুবহু verify করে বসানো হয়েছে। শুধু "TEMPORARY" এই অর্থে
-- যে Step 3 আনুষ্ঠানিকভাবে শুরু হওয়ার আগে এখানে ধার করে আনা হলো — বডি বদলাবে
-- না, Step 3 শুরু হলে এই একই সংজ্ঞা `0X_job_release_escrow_schema_stub.sql`-এ
-- সরে যাবে (এখান থেকে সরানো হবে) আর সেখানেই আরও এক্সটেন্ড হবে।
CREATE OR REPLACE FUNCTION public.refund_escrow_once(p_escrow_id text, p_refund_type text DEFAULT 'SOLVER_CANCEL'::text, p_refund_percentage numeric DEFAULT 100)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_escrow public.escrows%rowtype;
  v_amount numeric(12,2);
  v_trx_id text := 'TRX_REFUND_' || p_escrow_id;
  v_now timestamptz := now();
  v_user_has_role boolean;
begin
  select * into v_escrow from public.escrows where id = p_escrow_id for update;
  if not found then
    raise exception 'ESCROW_NOT_FOUND';
  end if;

  if not (auth.uid() = v_escrow.user_id or auth.uid() = v_escrow.solver_id or public.is_admin(auth.uid())) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  if v_escrow.status in ('RELEASED', 'REFUNDED') then
    return jsonb_build_object('result', 'ALREADY_TERMINAL', 'status', v_escrow.status);
  end if;

  if exists (select 1 from public.transactions where id = v_trx_id) then
    return jsonb_build_object('result', 'ALREADY_REFUNDED');
  end if;

  select has_user_role into v_user_has_role from public.users where id = v_escrow.user_id for update;
  if v_user_has_role is null then
    raise exception 'USER_NOT_FOUND';
  end if;
  if not v_user_has_role then
    raise exception 'USER_ROLE_INACTIVE';
  end if;

  v_amount := round((v_escrow.base_amount + v_escrow.extra_amount) * (p_refund_percentage / 100.0), 2);

  update public.users set balance = balance + v_amount, balance_user = balance_user + v_amount, updated_at = v_now where id = v_escrow.user_id;

  insert into public.transactions (id, problem_id, problem_title, user_id, solver_id, gross_amount,
    net_amount, type, escrow_id, refund_type, refund_percentage, role, "timestamp")
  values (v_trx_id, v_escrow.problem_id, v_escrow.problem_title, v_escrow.user_id, v_escrow.solver_id,
    v_amount, v_amount, 'REFUND', p_escrow_id, p_refund_type, p_refund_percentage, 'USER', v_now);

  update public.escrows set status = 'REFUNDED', released_at = v_now, updated_at = v_now where id = p_escrow_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_escrow.user_id,
    'রিফান্ড সম্পন্ন', 'আপনার ৳' || v_amount::text || ' রিফান্ড করা হয়েছে।', 'balance', v_escrow.user_id, v_now);

  return jsonb_build_object('result', 'OK', 'amount', v_amount);
end;
$function$;

-- উপরের ফাংশনের body-তে ব্যবহৃত escrows.escrow_id/refund_type/refund_percentage
-- কলামগুলো transactions টেবিলে, আর escrows.released_at কলামটা এখনো stub-এ নেই।
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS escrow_id text;
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS refund_type text;
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS refund_percentage numeric(6,2);
ALTER TABLE public.escrows ADD COLUMN IF NOT EXISTS released_at timestamptz;
