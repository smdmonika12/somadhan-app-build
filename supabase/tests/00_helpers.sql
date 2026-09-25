-- 00_helpers.sql
-- Supabase RPC ফাংশনগুলো auth.uid() ব্যবহার করে user চেনে (SECURITY DEFINER + RLS)।
-- Local test DB-তে real login নেই, তাই আমরা এই সহজ trick ব্যবহার করি:
-- request.jwt.claims সেট করে দিলে auth.uid() ঠিক সেই id-ই রিটার্ন করে।
--
-- ব্যবহার: SELECT test.login_as('11111111-1111-1111-1111-111111111111');

CREATE SCHEMA IF NOT EXISTS test;

-- Step 2 PART 1 সেশনে ধরা পড়েছে (প্রথমবার সত্যিই psql+pgTAP দিয়ে চালিয়ে):
-- test.login_as('...') কল করার পর current role authenticated/anon হয়ে যায়, আর তার
-- পরের প্রতিটা কল (test.logout() নিজেও, আর verification-এর জন্য সরাসরি
-- `SELECT ... FROM public.<table>` — SECURITY DEFINER RPC-র বাইরে) সেই কমানো
-- privilege নিয়েই চলে। schema "test"-এ PUBLIC-এর ডিফল্ট USAGE নেই (শুধু owner/
-- superuser-এর আছে), তাই এই GRANT ছাড়া test.logout() নিজেই "permission denied
-- for schema test" দিয়ে ভেঙে যায়। এটা শুধু Step 2-এর না — Step 1-এর
-- 01_bidding_flow.sql/02_bidding_flow_part2.sql-ও একইভাবে প্রভাবিত (এতদিন ধরা
-- পড়েনি কারণ সম্ভবত আগের কোনো সেশনই আসলে psql দিয়ে চালিয়ে verify করেনি)।
GRANT USAGE ON SCHEMA test TO anon, authenticated, service_role;

-- auth.uid() যদি আপনার প্রজেক্টে ইতিমধ্যে define করা না থাকে (local pg-এ
-- supabase auth schema সাধারণত থাকে না), তাহলে নিচের ছোট mock টা লাগবে।
-- Supabase-এর নিজস্ব local dev image (supabase/postgres) ব্যবহার করলে
-- auth.uid() আগে থেকেই আছে, এই ব্লকটা তখন কিছু করবে না।
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'auth' AND p.proname = 'uid'
  ) THEN
    CREATE SCHEMA IF NOT EXISTS auth;
    CREATE OR REPLACE FUNCTION auth.uid() RETURNS uuid AS $fn$
      SELECT nullif(current_setting('request.jwt.claim.sub', true), '')::uuid;
    $fn$ LANGUAGE sql STABLE;
  END IF;
END $$;

CREATE OR REPLACE FUNCTION test.login_as(p_user_id uuid) RETURNS void AS $$
BEGIN
  PERFORM set_config('request.jwt.claim.sub', p_user_id::text, true);
  PERFORM set_config('role', 'authenticated', true);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION test.logout() RETURNS void AS $$
BEGIN
  PERFORM set_config('request.jwt.claim.sub', '', true);
  PERFORM set_config('role', 'anon', true);
END;
$$ LANGUAGE plpgsql;

-- প্রতিটা টেস্ট ফাইলের শুরুতে ২টা টেস্ট ইউজার আর একটা admin বানানোর helper।
-- Step 1 সেশনে `accept_bid` verify করতে গিয়ে `balance_user` আর `has_user_role`
-- কলাম দুটো লাগলো (accept_bid legacy `balance` না, role-scoped `balance_user`
-- থেকে wallet deduction করে — দেখুন recovered_bidding_contracts.sql) — তাই
-- এই helper-টা সেই দুটো কলামও সেট করার জন্য আপডেট করা হলো।
-- ⚠️ Step 6 PART 2 সেশনে ফিক্স: `display_uid` কলাম আগে এখানে হার্ডকোডেড টেক্সট
-- ('T-CLIENT-1' ইত্যাদি) দিয়ে সেট করা হতো, যেটা 01-stub-এর তখনকার ভুল `text`
-- টাইপের সাথে মিলত। কলামটা `bigint`-এ ফিক্স হওয়ার পর এই টেক্সট মানগুলো আর
-- insert-ই হতো না ('T-CLIENT-1'::bigint casting error)। এখন display_uid এই
-- INSERT থেকে বাদ দেওয়া হলো — `add_display_uid_generator.sql` migration
-- apply হয়ে থাকলে তার `trg_set_display_uid` (BEFORE INSERT) trigger নিজে থেকেই
-- একটা ইউনিক bigint বসিয়ে দেয় (column NULL দেখলে); migration ছাড়া চালালে
-- (শুধু stub দিয়ে) column NULL-ই থাকে, যেটা 01-stub-এ অনুমোদিত (NOT NULL না)।
CREATE OR REPLACE FUNCTION test.seed_users() RETURNS void AS $$
BEGIN
  INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_user_role)
  VALUES
    ('11111111-1111-1111-1111-111111111111', 'CLIENT', 'Test Client', '01700000001', 1000, 1000, true),
    ('22222222-2222-2222-2222-222222222222', 'SOLVER', 'Test Solver One', '01700000002', 0, 0, true),
    ('33333333-3333-3333-3333-333333333333', 'SOLVER', 'Test Solver Two', '01700000003', 0, 0, true),
    ('99999999-9999-9999-9999-999999999999', 'ADMIN',  'Test Admin', '01700000099', 0, 0, true)
  ON CONFLICT (id) DO NOTHING;
END;
$$ LANGUAGE plpgsql;
