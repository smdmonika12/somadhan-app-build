-- 09_kyc_roles_part2.sql — Step 6 (KYC & roles), PART 2 of 2 (এই সেশন)
--
-- এই ফাইলে কভার করা হয়েছে বাকি ৪টা ফাংশন:
--   switch_role_get_or_create_linked_profile → recovered_role_switch.sql
--   admin_change_role                        → recovered_admin_kyc_ban_role.sql (লাইন ~৩৩১)
--   sync_linked_account_profile              → step32_5_sync_linked_account_profile.sql
--   generate_unique_display_uid              → add_display_uid_generator.sql
-- (PART 1-এর submit_kyc/admin_approve_kyc/admin_reject_kyc/admin_revoke_kyc দেখুন
-- 09_kyc_roles_part1.sql-এ — এই ফাইলে আবার ছোঁয়া হয়নি।)
--
-- সব ৪টা ফাংশনের real body এই সেশনে সরাসরি পড়ে verify করা হয়েছে (অনুমান না)।
-- case-insensitive grep-এ প্রতিটার সংজ্ঞা migrations-এ ঠিক একবার — কোনো পরের
-- migration override করে না।
--
-- ⚠️ এই ফাইলও (আগের সব ধাপের মতোই এই সেশনে) real Postgres+pgTAP-এ চালানো
-- যায়নি — sandbox network বন্ধ ছিল (`apt-get install postgresql
-- postgresql-16-pgtap` → 403 Forbidden, চেষ্টা করে নিশ্চিত হওয়া হয়েছে)।
-- শুধু static verification: (ক) migration-এর real body পড়ে assertion লেখা,
-- (খ) assertion সংখ্যা `grep -cE` দিয়ে গুনে plan(29)-এর সাথে মেলানো (প্রথম
-- ড্রাফটে ভুলবশত plan(30) লেখা হয়েছিল, গুনে ২৯ পাওয়ার পর ঠিক করা হয়েছে —
-- আগের স্টেপগুলোয় ধরা পড়া plan()-mismatch ভুল এড়াতে), (গ) `$$`/quote/parentheses
-- ভারসাম্য, (ঘ) ব্যবহৃত সব error-message/notification-title migration-এর
-- টেক্সটে হুবহু আছে কিনা যাচাই।
--
-- ⚠️⚠️ গুরুত্বপূর্ণ schema-stub bug এই সেশনেই ধরা পড়ে ফিক্স করা হয়েছে (দেখুন
-- 01_bidding_flow_schema_stub.sql, 00_helpers.sql, 08_disputes_part2.sql-এর
-- এই সেশনের ডিফ): `01_bidding_flow_schema_stub.sql`-এর `CREATE TABLE
-- public.users` স্টাবে `display_uid` কলাম ভুলবশত `text` টাইপে ছিল, কিন্তু আসল
-- migration (`add_display_uid_generator.sql`, এখানে টেস্ট হচ্ছে) কলামটা
-- `bigint` ধরে নেয় (`where display_uid between v_low and v_high`,
-- v_low/v_high বিগint)। যেহেতু CI workflow-এ schema stub migration-এর *আগে*
-- apply হয় (`full-test.yml`, "stub আগে, migration পরে" ফিক্স), তাই
-- migration-এর নিজের `ADD COLUMN IF NOT EXISTS display_uid bigint` একটা
-- no-op হয়ে যেত (কলাম আগে থেকেই আছে, শুধু ভুল টাইপে) — real Postgres-এ এটা
-- generate_unique_display_uid()-এর ভেতরের তুলনায় "operator does not exist:
-- text >= bigint" দিয়ে ভাঙত। এই ফাংশনটাই প্রথমবার টেস্ট করতে গিয়ে এই সেশনে
-- ধরা পড়েছে — আগের কোনো সেশন কখনো এই কলাম নিয়ে কিছু করেনি বলে চোখে পড়েনি।
-- ফিক্স: stub-এ কলাম টাইপ `bigint` করা হয়েছে, আর `00_helpers.sql`/
-- `08_disputes_part2.sql`-এ যেখানে এই (ভুল-টাইপ-নির্ভর) কলামে হার্ডকোডেড টেক্সট
-- ('T-CLIENT-1' ইত্যাদি) বসানো হতো, সেগুলো বাদ দেওয়া হয়েছে — migration
-- apply থাকলে `trg_set_display_uid` trigger নিজে থেকেই auto-fill করে।

BEGIN;
SELECT plan(29);

SELECT test.seed_users();
-- CLIENT: 11111111-...  SOLVER 1: 22222222-...  SOLVER 2: 33333333-...  ADMIN: 99999999-...

----------------------------------------------------------------------
-- switch_role_get_or_create_linked_profile
----------------------------------------------------------------------
-- login ছাড়া (test.seed_users()-এর পর ডিফল্টভাবে কোনো session role সেট করা
-- নেই, নিশ্চিত করার জন্য explicit logout)
SELECT test.logout();

SELECT throws_ok(
  $$ SELECT public.switch_role_get_or_create_linked_profile('SOLVER', NULL) $$,
  'P0001', 'NOT_AUTHENTICATED',
  'switch_role_get_or_create_linked_profile: auth.uid() NULL হলে NOT_AUTHENTICATED'
);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT throws_ok(
  $$ SELECT public.switch_role_get_or_create_linked_profile('ADMIN', NULL) $$,
  'P0001', 'INVALID_ROLE',
  'switch_role_get_or_create_linked_profile: p_target_role=ADMIN হলে INVALID_ROLE (শুধু USER/SOLVER অনুমোদিত)'
);

-- happy path — প্রথমবার SOLVER-এ switch (client-এর has_solver_role আগে থেকে false)
SELECT ok(
  (SELECT r->>'created' = 'false'
      AND (r->>'balance_user')::numeric = 1000
      AND r->>'solver_categories' = 'রান্না,টিউশন'
      AND r->>'has_completed_solver_setup' = 'true'
   FROM (SELECT public.switch_role_get_or_create_linked_profile('SOLVER', 'রান্না,টিউশন') AS r) s),
  'switch_role_get_or_create_linked_profile: SOLVER happy path — created=false, balance_user অপরিবর্তিত, solver_categories/has_completed_solver_setup রিটার্ন-এ সঠিক'
);

SELECT results_eq(
  $$ SELECT has_user_role, has_solver_role, solver_categories, has_completed_solver_setup
     FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111' $$,
  $$ VALUES (true, true, 'রান্না,টিউশন'::text, true) $$,
  'switch_role_get_or_create_linked_profile: SOLVER happy path — has_user_role/has_solver_role/solver_categories/has_completed_solver_setup সব সঠিকভাবে আপডেট হয়'
);

-- দ্বিতীয়বার কল, p_solver_categories না দিয়ে (ডিফল্ট NULL) — coalesce(nullif(NULL,''),
-- nullif(পুরোনো,'')) পুরোনো মান রাখে, মোছে না
SELECT ok(
  (SELECT r->>'solver_categories' = 'রান্না,টিউশন'
   FROM (SELECT public.switch_role_get_or_create_linked_profile('SOLVER') AS r) s),
  'switch_role_get_or_create_linked_profile: p_solver_categories না দিলে (NULL ডিফল্ট) পুরোনো categories বজায় থাকে (মোছে না)'
);

-- USER-এ switch করলে — has_user_role আগে থেকেই true; has_solver_role/solver_categories/
-- has_completed_solver_setup "else" ব্রাঞ্চে অপরিবর্তিত থাকে (কখনো regress করে না)
SELECT ok(
  (SELECT r->>'created' = 'false' FROM (SELECT public.switch_role_get_or_create_linked_profile('USER') AS r) s),
  'switch_role_get_or_create_linked_profile: USER-এ switch করলেও created=false রিটার্ন করে (linked-profile তৈরি হয় না, শুধু flag আপডেট)'
);

SELECT results_eq(
  $$ SELECT has_user_role, has_solver_role, solver_categories, has_completed_solver_setup
     FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111' $$,
  $$ VALUES (true, true, 'রান্না,টিউশন'::text, true) $$,
  'switch_role_get_or_create_linked_profile: DOCUMENTED CURRENT BEHAVIOUR — USER-এ switch করলে আগের SOLVER flags/categories/setup কখনো regress করে না (else ব্রাঞ্চ কিছু বদলায় না)'
);

-- ROOT_ACCOUNT_NOT_FOUND — অস্তিত্বহীন uid দিয়ে login (test.login_as শুধু jwt claim সেট করে, existence চেক করে না)
SELECT test.login_as('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');
SELECT throws_ok(
  $$ SELECT public.switch_role_get_or_create_linked_profile('USER', NULL) $$,
  'P0001', 'ROOT_ACCOUNT_NOT_FOUND',
  'switch_role_get_or_create_linked_profile: public.users-এ row না থাকলে ROOT_ACCOUNT_NOT_FOUND'
);
SELECT test.logout();

----------------------------------------------------------------------
-- admin_change_role
----------------------------------------------------------------------
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role) VALUES
  ('cccccccc-0001-0001-0001-000000000001', 'SOLVER', 'Role Target 1 (promote to ADMIN)', '01720000001', true, true),
  ('cccccccc-0002-0002-0002-000000000002', 'SOLVER', 'Role Target 2 (flip to USER)',     '01720000002', false, true);

-- NOT_AUTHORIZED — non-admin caller
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.admin_change_role('cccccccc-0001-0001-0001-000000000001', 'ADMIN') $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_change_role: non-admin কল করলে NOT_AUTHORIZED'
);

SELECT test.login_as('99999999-9999-9999-9999-999999999999');

-- happy path — SOLVER → ADMIN (has_user_role/has_solver_role দুটোই আগে থেকে true, তাই OR-এ অপরিবর্তিত)
SELECT is(
  (public.admin_change_role('cccccccc-0001-0001-0001-000000000001', 'ADMIN'))->>'result',
  'OK',
  'admin_change_role: admin happy path (SOLVER→ADMIN) → result OK'
);
SELECT results_eq(
  $$ SELECT role, has_user_role, has_solver_role
     FROM public.users WHERE id = 'cccccccc-0001-0001-0001-000000000001' $$,
  $$ VALUES ('ADMIN'::text, true, true) $$,
  'admin_change_role: role=ADMIN সেট হয়, has_user_role/has_solver_role অপরিবর্তিত (আগেই true ছিল)'
);

-- happy path — SOLVER (has_user_role=false) → USER: has_user_role এখন true হয়ে যায় (OR (p_new_role='USER'))
SELECT is(
  (public.admin_change_role('cccccccc-0002-0002-0002-000000000002', 'USER'))->>'result',
  'OK',
  'admin_change_role: admin happy path (SOLVER, has_user_role=false → USER) → result OK'
);
SELECT results_eq(
  $$ SELECT role, has_user_role, has_solver_role
     FROM public.users WHERE id = 'cccccccc-0002-0002-0002-000000000002' $$,
  $$ VALUES ('USER'::text, true, true) $$,
  'admin_change_role: role=USER-এ পরিবর্তনের ফলে has_user_role false→true হয়ে যায়, has_solver_role স্পর্শ হয় না (আগের true-ই থাকে)'
);

SELECT ok(
  EXISTS(
    SELECT 1 FROM public.notifications
    WHERE user_id = 'cccccccc-0001-0001-0001-000000000001'
      AND title = 'অ্যাকাউন্ট রোল পরিবর্তন'
      AND position('ADMIN' in message) > 0
  ),
  'admin_change_role: "রোল পরিবর্তন" notification-এর message-এ নতুন role অন্তর্ভুক্ত থাকে'
);

-- INVALID_ROLE
SELECT throws_ok(
  $$ SELECT public.admin_change_role('cccccccc-0001-0001-0001-000000000001', 'BANNED') $$,
  'P0001', 'INVALID_ROLE',
  'admin_change_role: p_new_role শুধু USER/SOLVER/ADMIN — অন্য কিছু দিলে INVALID_ROLE'
);

-- USER_NOT_FOUND
SELECT is(
  (public.admin_change_role('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'USER'))->>'result',
  'USER_NOT_FOUND',
  'admin_change_role: অস্তিত্বহীন p_user_id → result USER_NOT_FOUND (exception না)'
);
SELECT test.logout();

----------------------------------------------------------------------
-- sync_linked_account_profile
----------------------------------------------------------------------
-- linked child (client-এর linked_account_id সরাসরি সেট করা — একই পরিবার)
INSERT INTO public.users (id, role, name, phone, email, linked_account_id) VALUES
  ('dddddddd-0001-0001-0001-000000000001', 'SOLVER', 'Linked Child', '01799999911', 'child@example.com',
   '11111111-1111-1111-1111-111111111111');

-- একই phone-এর duplicate (linked_account_id এখনো সেট হয়নি — pre-link duplicate কেস)
INSERT INTO public.users (id, role, name, phone) VALUES
  ('eeeeeeee-0001-0001-0001-000000000001', 'CLIENT', 'Phone Dup', '01700000001');

-- সম্পূর্ণ অসম্পর্কিত stranger
INSERT INTO public.users (id, role, name, phone) VALUES
  ('ffffffff-0001-0001-0001-000000000001', 'CLIENT', 'Stranger', '01711111199');

-- not authenticated
SELECT test.logout();
SELECT throws_ok(
  $$ SELECT public.sync_linked_account_profile('dddddddd-0001-0001-0001-000000000001', 'Hack Attempt') $$,
  'P0001', 'not authenticated',
  'sync_linked_account_profile: login ছাড়া কল করলে "not authenticated" exception'
);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');

-- নিজের row — সবসময় অনুমোদিত (v_authorized সরাসরি true)
SELECT ok(
  (SELECT r->>'success' = 'true' AND r->>'target_id' = '11111111-1111-1111-1111-111111111111'
   FROM (SELECT public.sync_linked_account_profile(
     '11111111-1111-1111-1111-111111111111', 'Client Renamed') AS r) s),
  'sync_linked_account_profile: caller নিজের row sync করলে সবসময় অনুমোদিত, success=true'
);
SELECT is(
  (SELECT name FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111'),
  'Client Renamed',
  'sync_linked_account_profile: নিজের row-এ name আসলেই আপডেট হয়'
);

-- linked child (is_same_account_family via linked_account_id) sync
SELECT ok(
  (SELECT r->>'success' = 'true'
   FROM (SELECT public.sync_linked_account_profile(
     'dddddddd-0001-0001-0001-000000000001', 'Child Renamed', NULL, NULL, NULL, NULL, NULL, NULL, true) AS r) s),
  'sync_linked_account_profile: caller-এর linked (একই পরিবারের) child-কে sync করা যায়'
);
SELECT results_eq(
  $$ SELECT name, is_verified_badge, linked_account_id
     FROM public.users WHERE id = 'dddddddd-0001-0001-0001-000000000001' $$,
  $$ VALUES ('Child Renamed'::text, true, '11111111-1111-1111-1111-111111111111'::uuid) $$,
  'sync_linked_account_profile: linked child-এর name/is_verified_badge আপডেট হয়, linked_account_id অপরিবর্তিত থাকে (আগে থেকেই root)'
);

-- phone-duplicate stranger sync — phone-ম্যাচ দিয়ে authorized; sync-এর পর defense-in-depth
-- হিসেবে linked_account_id root-এ সেট হয়ে যায়
SELECT ok(
  (SELECT r->>'success' = 'true'
   FROM (SELECT public.sync_linked_account_profile(
     'eeeeeeee-0001-0001-0001-000000000001', 'Dup Renamed') AS r) s),
  'sync_linked_account_profile: caller-এর সাথে একই ফোন-নম্বরের pre-link duplicate row sync করা যায়'
);
SELECT results_eq(
  $$ SELECT name, linked_account_id
     FROM public.users WHERE id = 'eeeeeeee-0001-0001-0001-000000000001' $$,
  $$ VALUES ('Dup Renamed'::text, '11111111-1111-1111-1111-111111111111'::uuid) $$,
  'sync_linked_account_profile: phone-duplicate row sync-এর পর linked_account_id root caller-এর id-তে সেট হয়ে যায় (defense-in-depth লিংক)'
);

-- সম্পূর্ণ stranger (না family, না phone/email মিল) → not authorized
SELECT throws_ok(
  $$ SELECT public.sync_linked_account_profile('ffffffff-0001-0001-0001-000000000001', 'Hacked Name') $$,
  'P0001', 'not authorized to sync this linked account',
  'sync_linked_account_profile: অসম্পর্কিত stranger sync করতে চাইলে "not authorized to sync this linked account"'
);
SELECT is(
  (SELECT name FROM public.users WHERE id = 'ffffffff-0001-0001-0001-000000000001'),
  'Stranger',
  'sync_linked_account_profile: unauthorized কলে stranger-এর row একদম অপরিবর্তিত থাকে'
);
SELECT test.logout();

----------------------------------------------------------------------
-- generate_unique_display_uid
----------------------------------------------------------------------
-- মৌলিক uniqueness: দুইবার কল, মাঝে আসল insert করে (শুধু ফাংশন দুইবার কল করলে
-- collision-probability থাকত — insert না করলে ফাংশন আগের কলের রিটার্ন-করা মান
-- সম্পর্কে জানে না) — এভাবে সত্যিকারের uniqueness-guarantee টেস্ট হয়।
CREATE TEMP TABLE tmp_duid_test (v bigint);
INSERT INTO tmp_duid_test SELECT public.generate_unique_display_uid();
INSERT INTO public.users (id, role, name, phone, display_uid)
  SELECT gen_random_uuid(), 'SOLVER', 'DUIDTest1', '01799999901', v FROM tmp_duid_test;
INSERT INTO tmp_duid_test SELECT public.generate_unique_display_uid();

SELECT ok(
  (SELECT count(DISTINCT v) FROM tmp_duid_test) = 2,
  'generate_unique_display_uid: বাস্তব insert-এর মাঝে দুইবার কল করলে দুটো ভিন্ন unique মান আসে'
);
SELECT ok(
  (SELECT bool_and(v BETWEEN 100000 AND 999999) FROM tmp_duid_test),
  'generate_unique_display_uid: ডিফল্ট current_digits=6 window (100000-999999)-এর মধ্যেই মান আসে'
);

-- digit-window growth: current_digits=1 (window 1-9, range 9, ৯০% থ্রেশহোল্ড floor(9*0.9)=8)
-- বসিয়ে ৮টা (>= থ্রেশহোল্ড) row insert করে verify করা হচ্ছে window সত্যিই ২-digit-এ বাড়ে
UPDATE public.display_uid_state SET current_digits = 1 WHERE id = 1;
INSERT INTO public.users (id, role, name, phone, display_uid)
  SELECT gen_random_uuid(), 'SOLVER', 'DUIDFill' || g, '017999999' || g, g
  FROM generate_series(1, 8) AS g;

CREATE TEMP TABLE tmp_duid_growth (v bigint);
INSERT INTO tmp_duid_growth SELECT public.generate_unique_display_uid();

SELECT ok(
  (SELECT v FROM tmp_duid_growth) BETWEEN 10 AND 99,
  'generate_unique_display_uid: ১-digit window ৯০%+ ভরে গেলে (৮/৯ ব্যবহৃত) পরের কল ২-digit window থেকে মান দেয়'
);
SELECT is(
  (SELECT current_digits FROM public.display_uid_state WHERE id = 1),
  2::smallint,
  'generate_unique_display_uid: window বৃদ্ধি স্থায়ীভাবে display_uid_state.current_digits আপডেট করে (কখনো কমে না)'
);

SELECT * FROM finish();
ROLLBACK;
