-- 11_notifications_ratings_part2.sql — Step 8 (Notifications & ratings), PART 2 of 2
--
-- এই ফাইলে Step 8-এর বাকি ২টা ফাংশন কভার করা হলো (প্রথম ৭টা `11_notifications_ratings_part1.sql`-এ):
--   submit_rating                                  → recovered_kyc_rating_reputation.sql:৬৪
--   submit_reputation_event (৬-আর্গ = effective;   → recovered_kyc_rating_reputation.sql:৯৪ (৫-আর্গ), :৩২৪ (৬-আর্গ)
--                            ৫-আর্গ = ambiguity)
--
-- সংজ্ঞা যাচাই (rule #5/#5a): case-insensitive grep -i দিয়ে দুটোরই সব সংজ্ঞা/DROP/GRANT খোঁজা হয়েছে —
-- ঠিক একটাই ফাইলে (recovered_kyc_rating_reputation.sql), কোনো পরবর্তী migration override/DROP করেনি।
-- দুটো ফাংশনের real body সরাসরি পড়ে (প্রতিটা `raise exception`, প্রতিটা শাখা) টেস্ট লেখা।
--
-- কভারেজ:
--   submit_rating — check-ক্রম (PROBLEM_NOT_FOUND → INVALID_RATER_ROLE → NOT_AUTHORIZED), দুই role-এর happy path
--     ও সারির প্রতিটা কলাম, stars-এর table-CHECK (inferred), duplicate/COMPLETED-গার্ড নেই, NULL-তুলনা ফাঁক।
--   submit_reputation_event — overload (pg_proc = ২, ৫-আর্গ কল 42725), AUTH_REQUIRED/USER_ID_REQUIRED/
--     UNSUPPORTED_EVENT_TYPE, ADMIN_ADJUSTMENT (dual clamp), INACTIVE_7_DAYS/INACTIVE_30_DAYS (rate-limit সীমা,
--     NOT_YET_INACTIVE, linked-account দুই দিক, platform_settings penalty), EXTRA_CHARGE_VIA_APP/ACCEPTED
--     (স্কোর-সূত্র, ৩.০ সীমা, সর্বশেষ responded_at, per-problem cap, replay-guard নেই), BID_WON, JOB_COMPLETED,
--     PROBLEM_POSTED, RATING_BONUS (৫★/৪★/৩★), WITHDRAWAL_COMPLETED — প্রতিটায় REF_ID_REQUIRED/NOT_ELIGIBLE
--     (eligibility ও authorization আলাদা), ALREADY_CLAIMED, দৈনিক cap (partial-fit + DAILY_CAP_REACHED),
--     SKIPPED_ZERO_SCORE, ROLE_INACTIVE, ডিফল্ট স্কোর ও platform_settings override।
--
-- ⚠️ "DOCUMENTED CURRENT BEHAVIOUR" লেবেলযুক্ত assertion সম্ভাব্য বাগ লক করে রাখে (কোনো migration/ফাংশন বদলানো
-- হয়নি — rule #1; ফিক্স হলে ওই assertion ইচ্ছাকৃতভাবে ভাঙবে, তখন প্রত্যাশিত মান আপডেট করবে):
--   ১) submit_rating: auth.uid() NULL (anon — migration-এ GRANT TO anon আছে) হলে `NULL <> uuid` = NULL → গার্ড পাশ
--      কাটে; লগইন ছাড়াই যেকোনো problem-এ owner-এর নামে rating ঢোকানো যায়।
--   ২) submit_rating: accepted_solver_id NULL problem-এ SOLVER-role rating যেকোনো লগইন-করা ইউজার ঢোকাতে পারে।
--   ৩) submit_rating: stars validate করে না (নিরাপত্তা শুধু inferred table CHECK-এ), duplicate-গার্ড নেই,
--      problem COMPLETED কিনা দেখে না।
--   ৪) submit_reputation_event ৫-আর্গ ↔ ৬-আর্গ overload ambiguous (42725) — Step 11 candidate।
--   ৫) EXTRA_CHARGE_* cap-এর ফল 'DAILY_CAP_REACHED' — আসলে per-problem cap (নাম বিভ্রান্তিকর, cosmetic)।
--   ৬) WITHDRAWAL_COMPLETED: reputation_events.problem_id-তে withdrawal-এর id বসে (কলামের নাম বিভ্রান্তিকর)।
--
-- ⚠️ এই সেশনেও sandbox-এ Postgres/pgTAP নেই এবং network বন্ধ (apt-get update → 403; pip-ও ব্যর্থ) — তাই এই ফাইল
-- শুধু STATIC ভাবে যাচাই করা: প্রতিটা assertion ফাংশন-বডির সাথে মিলিয়ে (স্কোর-সূত্র হাতে কষে), throws_ok-এর
-- প্রতিটা message বডির `raise exception '…'`-এর সাথে script দিয়ে মিলিয়ে, fixture-কলাম ↔ stub মিলিয়ে, plan() সংখ্যা
-- script দিয়ে গুনে, statement-structure lint করে। **real Postgres+pgTAP-এ কখনো চালানো হয়নি।**
--
-- pgTAP-সংক্রান্ত: `like()` ব্যবহার করা হয়নি (এই সেশনে সন্দেহ — pgTAP-এ pattern-ফাংশনের নাম `alike`/`matches`,
-- `like` না; real-run-এর "function like(text, unknown, unknown) does not exist" সম্ভবত টাইপ নয়, নামের সমস্যা) —
-- prefix-যাচাই `ok(starts_with(…))` দিয়ে। results_eq/set_eq-র প্রতিটা কলামের নাম আলাদা (duplicate `?column?` এড়াতে)।
-- Bengali literal (note) হাতে টাইপ করা হয়নি — migration থেকে Python-regex দিয়ে extract করে বসানো।
--
-- Fixture uuid prefix: f9… ; problem/id prefix: NR2_… ; পুরো ফাইল BEGIN … ROLLBACK-এর ভেতরে।
-- ফাংশন-কল আর তার প্রভাব যাচাই আলাদা statement-এ; verification/fixture-এর আগে `RESET ROLE`;
-- সংখ্যা তুলনা `::numeric`; কোনো migration কখনো `rep_*` platform_settings কী seed করে না (grep-এ যাচাই), তবু শুরুতে মুছে নেওয়া।
--
-- ⚠️⚠️ ২০২৬-০৯-২১ (Step 12.12, F1 ফিক্স, ব্যবহারকারীর অনুমতিতে) — submit_reputation_event-এর পুরনো
-- (p_role-বিহীন) ৫-আর্গ overload step12_9 migration-এ DROP হওয়ায় "২টা overload আছে" ও "৫-আর্গ কল
-- ambiguous (42725)" assertion দুটো ঠিক থাকে না। সৌভাগ্যক্রমে ৫-আর্গ কলের একই argument সেট
-- (user=f9000001, ref=NR2_PP1) real business-validation-এ এমনিই ব্যর্থ হয় (NR2_PP1 problem-এর
-- মালিক আসলে f9000035, f9000001 না) — তাই throws_ok-এর errcode/message শুধু '42725'/NULL থেকে
-- 'P0001'/'NOT_ELIGIBLE'-এ বদলানো হলো, কোনো নতুন assertion লাগেনি, plan() অপরিবর্তিত (১৬০)। এই
-- সেশনেও sandbox network বন্ধ — static-ভাবেই যাচাই, real Postgres-এ চালানো হয়নি। বিস্তারিত:
-- CI_TEST_SUITE_PROGRESS.md-এর এই সেশনের "Step 12.12" সেকশন।

BEGIN;
SELECT plan(162);

SELECT test.seed_users();
-- CLIENT: 11111111-...  SOLVER 1: 22222222-...  SOLVER 2: 33333333-...  ADMIN: 99999999-...

RESET ROLE;
-- প্রতিটা assertion-গ্রুপের নিজস্ব ইউজার (fixture uuid prefix f9…) — reputation_score জমা হয়ে একে অপরকে যেন না ছোঁয়।
-- has_user_role/has_solver_role স্পষ্টভাবে সেট (seed_users solver-দের has_solver_role false রাখে)।
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role) VALUES
  ('f9000001-0000-0000-0000-000000000001', 'CLIENT', 'NR2 OWN', '01910000001', true, false),
  ('f9000002-0000-0000-0000-000000000002', 'SOLVER', 'NR2 SOL', '01910000002', true, true),
  ('f9000003-0000-0000-0000-000000000003', 'CLIENT', 'NR2 OUT', '01910000003', true, false),
  ('f9000004-0000-0000-0000-000000000004', 'CLIENT', 'NR2 NOR', '01910000004', false, false),
  ('f9000010-0000-0000-0000-000000000010', 'CLIENT', 'NR2 C2', '01910000016', true, true),
  ('f9000014-0000-0000-0000-000000000014', 'CLIENT', 'NR2 FOWN', '01910000020', true, false),
  ('f9000015-0000-0000-0000-000000000015', 'SOLVER', 'NR2 FSOL', '01910000021', true, true),
  ('f9000016-0000-0000-0000-000000000016', 'SOLVER', 'NR2 FSOLX', '01910000022', false, false),
  ('f9000017-0000-0000-0000-000000000017', 'SOLVER', 'NR2 FSOL2', '01910000023', true, true),
  ('f9000018-0000-0000-0000-000000000018', 'SOLVER', 'NR2 FSOL3', '01910000024', true, true),
  ('f900001e-0000-0000-0000-00000000001e', 'CLIENT', 'NR2 GO', '01910000030', true, false),
  ('f900001f-0000-0000-0000-00000000001f', 'SOLVER', 'NR2 GS', '01910000031', true, true),
  ('f9000020-0000-0000-0000-000000000020', 'SOLVER', 'NR2 GX', '01910000032', true, true),
  ('f9000021-0000-0000-0000-000000000021', 'SOLVER', 'NR2 GS2', '01910000033', true, true),
  ('f9000028-0000-0000-0000-000000000028', 'SOLVER', 'NR2 RS1', '01910000040', true, true),
  ('f9000029-0000-0000-0000-000000000029', 'SOLVER', 'NR2 WS1', '01910000041', true, true),
  ('f900002a-0000-0000-0000-00000000002a', 'SOLVER', 'NR2 WS2', '01910000042', true, true),
  ('f900002b-0000-0000-0000-00000000002b', 'SOLVER', 'NR2 WS3', '01910000043', true, true),
  ('f900002c-0000-0000-0000-00000000002c', 'SOLVER', 'NR2 WS4', '01910000044', true, true),
  ('f900002d-0000-0000-0000-00000000002d', 'SOLVER', 'NR2 RS2', '01910000045', true, true),
  ('f900002e-0000-0000-0000-00000000002e', 'CLIENT', 'NR2 RO', '01910000046', true, false),
  ('f9000032-0000-0000-0000-000000000032', 'CLIENT', 'NR2 JO', '01910000050', true, false),
  ('f9000033-0000-0000-0000-000000000033', 'SOLVER', 'NR2 JS', '01910000051', true, true),
  ('f9000034-0000-0000-0000-000000000034', 'SOLVER', 'NR2 JS2', '01910000052', true, true),
  ('f9000035-0000-0000-0000-000000000035', 'CLIENT', 'NR2 PO', '01910000053', true, false),
  ('f9000036-0000-0000-0000-000000000036', 'CLIENT', 'NR2 PO2', '01910000054', true, false),
  ('f9000037-0000-0000-0000-000000000037', 'CLIENT', 'NR2 JO2', '01910000055', true, false),
  ('f9000038-0000-0000-0000-000000000038', 'SOLVER', 'NR2 JS3', '01910000056', true, true),
  ('f9000039-0000-0000-0000-000000000039', 'CLIENT', 'NR2 PU', '01910000057', true, false);
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role, reputation_score, reputation_score_user, reputation_score_solver) VALUES
  ('f9000005-0000-0000-0000-000000000005', 'SOLVER', 'NR2 TGT', '01910000005', true, true, 50, 10, 20),
  ('f9000006-0000-0000-0000-000000000006', 'SOLVER', 'NR2 TGT2', '01910000006', true, true, 50, 0, 95),
  ('f9000007-0000-0000-0000-000000000007', 'CLIENT', 'NR2 TGT3', '01910000007', true, true, 2, 1, 0);
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role, reputation_score, reputation_score_user, reputation_score_solver, updated_at) VALUES
  ('f9000008-0000-0000-0000-000000000008', 'SOLVER', 'NR2 IN1', '01910000008', true, true, 50, 30, 30, now() - interval '40 days'),
  ('f9000009-0000-0000-0000-000000000009', 'CLIENT', 'NR2 IN2', '01910000009', true, true, 50, 30, 0, now() - interval '40 days');
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role, updated_at) VALUES
  ('f900000a-0000-0000-0000-00000000000a', 'CLIENT', 'NR2 IN3', '01910000010', true, true, now() - interval '3 days'),
  ('f900000b-0000-0000-0000-00000000000b', 'CLIENT', 'NR2 IN4', '01910000011', true, true, now() - interval '20 days'),
  ('f900000d-0000-0000-0000-00000000000d', 'CLIENT', 'NR2 INA', '01910000013', true, true, now() - interval '40 days'),
  ('f9000011-0000-0000-0000-000000000011', 'CLIENT', 'NR2 IN6', '01910000017', true, true, now() - interval '40 days');
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role, updated_at, last_reputation_decay_check_at) VALUES
  ('f900000c-0000-0000-0000-00000000000c', 'CLIENT', 'NR2 IN5', '01910000012', true, true, now() - interval '40 days', now() - interval '3 days');
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role, linked_account_id) VALUES
  ('f900000e-0000-0000-0000-00000000000e', 'CLIENT', 'NR2 LNK', '01910000014', true, true, 'f900000d-0000-0000-0000-00000000000d');
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role, updated_at, linked_account_id) VALUES
  ('f900000f-0000-0000-0000-00000000000f', 'CLIENT', 'NR2 T2', '01910000015', true, true, now() - interval '40 days', 'f9000010-0000-0000-0000-000000000010');
-- কোনো migration platform_settings-এ rep_* কী seed করে না (grep-এ যাচাই) — তবু শুরুতে পরিষ্কার করা হলো, যাতে ডিফল্ট-আচরণ নির্ভুলভাবে যাচাই হয়।
DELETE FROM public.platform_settings WHERE left(key, 4) = 'rep_' OR key = 'extra_bill_reputation_cap_per_problem';
-- Problems — id prefix NR2_ (Part 1-এর NR_ থেকে আলাদা)। NR2_W1/W3/W4: WITHDRAWAL_COMPLETED-এ withdrawal-এর id-ই reputation_events.problem_id-তে বসে (10-stub-এর inferred FK→problems),
-- তাই একই id-তে একটা dummy problems সারি — FK থাকুক বা না থাকুক টেস্ট একই ফল দেয়।
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id) VALUES
  ('NR2_R1', 'f9000001-0000-0000-0000-000000000001', 'NR2 R1', 'COMPLETED', 'f9000002-0000-0000-0000-000000000002'),
  ('NR2_R2', 'f9000001-0000-0000-0000-000000000001', 'NR2 R2', 'OPEN', NULL),
  ('NR2_R3', 'f9000001-0000-0000-0000-000000000001', 'NR2 R3', 'COMPLETED', 'f9000002-0000-0000-0000-000000000002'),
  ('NR2_R4', 'f9000001-0000-0000-0000-000000000001', 'NR2 R4', 'IN_PROGRESS', 'f9000002-0000-0000-0000-000000000002'),
  ('NR2_X1', 'f9000014-0000-0000-0000-000000000014', 'NR2 X1', 'COMPLETED', 'f9000015-0000-0000-0000-000000000015'),
  ('NR2_X2', 'f9000014-0000-0000-0000-000000000014', 'NR2 X2', 'COMPLETED', 'f9000017-0000-0000-0000-000000000017'),
  ('NR2_X3', 'f9000014-0000-0000-0000-000000000014', 'NR2 X3', 'COMPLETED', 'f9000016-0000-0000-0000-000000000016'),
  ('NR2_X5', 'f9000014-0000-0000-0000-000000000014', 'NR2 X5', 'COMPLETED', 'f9000018-0000-0000-0000-000000000018'),
  ('NR2_X6', 'f9000014-0000-0000-0000-000000000014', 'NR2 X6', 'COMPLETED', 'f9000015-0000-0000-0000-000000000015'),
  ('NR2_G1', 'f900001e-0000-0000-0000-00000000001e', 'NR2 G1', 'IN_PROGRESS', 'f900001f-0000-0000-0000-00000000001f'),
  ('NR2_G2', 'f900001e-0000-0000-0000-00000000001e', 'NR2 G2', 'IN_PROGRESS', 'f900001f-0000-0000-0000-00000000001f'),
  ('NR2_G3', 'f900001e-0000-0000-0000-00000000001e', 'NR2 G3', 'IN_PROGRESS', 'f900001f-0000-0000-0000-00000000001f'),
  ('NR2_G4', 'f900001e-0000-0000-0000-00000000001e', 'NR2 G4', 'IN_PROGRESS', 'f900001f-0000-0000-0000-00000000001f'),
  ('NR2_G5', 'f900001e-0000-0000-0000-00000000001e', 'NR2 G5', 'IN_PROGRESS', 'f900001f-0000-0000-0000-00000000001f'),
  ('NR2_G6', 'f900001e-0000-0000-0000-00000000001e', 'NR2 G6', 'OPEN', NULL),
  ('NR2_G7', 'f900001e-0000-0000-0000-00000000001e', 'NR2 G7', 'IN_PROGRESS', 'f9000004-0000-0000-0000-000000000004'),
  ('NR2_G8', 'f900001e-0000-0000-0000-00000000001e', 'NR2 G8', 'IN_PROGRESS', 'f9000021-0000-0000-0000-000000000021'),
  ('NR2_G9', 'f900001e-0000-0000-0000-00000000001e', 'NR2 G9', 'IN_PROGRESS', 'f9000021-0000-0000-0000-000000000021'),
  ('NR2_G10', 'f900001e-0000-0000-0000-00000000001e', 'NR2 G10', 'IN_PROGRESS', 'f9000021-0000-0000-0000-000000000021'),
  ('NR2_J1', 'f9000032-0000-0000-0000-000000000032', 'NR2 J1', 'COMPLETED', 'f9000033-0000-0000-0000-000000000033'),
  ('NR2_J2', 'f9000032-0000-0000-0000-000000000032', 'NR2 J2', 'COMPLETED', 'f9000034-0000-0000-0000-000000000034'),
  ('NR2_J3', 'f9000032-0000-0000-0000-000000000032', 'NR2 J3', 'COMPLETED', 'f9000033-0000-0000-0000-000000000033'),
  ('NR2_J4', 'f9000032-0000-0000-0000-000000000032', 'NR2 J4', 'IN_PROGRESS', 'f9000033-0000-0000-0000-000000000033'),
  ('NR2_J5', 'f9000037-0000-0000-0000-000000000037', 'NR2 J5', 'COMPLETED', 'f9000038-0000-0000-0000-000000000038'),
  ('NR2_J6', 'f9000037-0000-0000-0000-000000000037', 'NR2 J6', 'COMPLETED', 'f9000038-0000-0000-0000-000000000038'),
  ('NR2_J7', 'f9000037-0000-0000-0000-000000000037', 'NR2 J7', 'COMPLETED', 'f9000038-0000-0000-0000-000000000038'),
  ('NR2_PP1', 'f9000035-0000-0000-0000-000000000035', 'NR2 PP1', 'OPEN', NULL),
  ('NR2_PP3', 'f9000036-0000-0000-0000-000000000036', 'NR2 PP3', 'OPEN', NULL),
  ('NR2_PP4', 'f9000039-0000-0000-0000-000000000039', 'NR2 PP4', 'OPEN', NULL),
  ('NR2_PP5', 'f9000039-0000-0000-0000-000000000039', 'NR2 PP5', 'OPEN', NULL),
  ('NR2_PP6', 'f9000039-0000-0000-0000-000000000039', 'NR2 PP6', 'OPEN', NULL),
  ('NR2_PP9', 'f9000004-0000-0000-0000-000000000004', 'NR2 PP9', 'OPEN', NULL),
  ('NR2_RB1', 'f900002e-0000-0000-0000-00000000002e', 'NR2 RB1', 'COMPLETED', 'f900002d-0000-0000-0000-00000000002d'),
  ('NR2_RB2', 'f900002e-0000-0000-0000-00000000002e', 'NR2 RB2', 'COMPLETED', 'f900002d-0000-0000-0000-00000000002d'),
  ('NR2_RB3', 'f900002e-0000-0000-0000-00000000002e', 'NR2 RB3', 'COMPLETED', 'f900002d-0000-0000-0000-00000000002d'),
  ('NR2_RB4', 'f900002e-0000-0000-0000-00000000002e', 'NR2 RB4', 'COMPLETED', 'f900002d-0000-0000-0000-00000000002d'),
  ('NR2_RB5', 'f900002e-0000-0000-0000-00000000002e', 'NR2 RB5', 'COMPLETED', 'f9000028-0000-0000-0000-000000000028'),
  ('NR2_RB6', 'f900002e-0000-0000-0000-00000000002e', 'NR2 RB6', 'COMPLETED', 'f9000028-0000-0000-0000-000000000028'),
  ('NR2_RB7', 'f900002e-0000-0000-0000-00000000002e', 'NR2 RB7', 'COMPLETED', 'f9000028-0000-0000-0000-000000000028'),
  ('NR2_W1', 'f9000001-0000-0000-0000-000000000001', 'NR2 W1', 'OPEN', NULL),
  ('NR2_W3', 'f9000001-0000-0000-0000-000000000001', 'NR2 W3', 'OPEN', NULL),
  ('NR2_W4', 'f9000001-0000-0000-0000-000000000001', 'NR2 W4', 'OPEN', NULL);
INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status) VALUES
  ('NR2_GB1', 'NR2_G1', 'f900001f-0000-0000-0000-00000000001f', 'NR2 bidder', 100, 'ACCEPTED'),
  ('NR2_GB2', 'NR2_G2', 'f900001f-0000-0000-0000-00000000001f', 'NR2 bidder', 100, 'ACCEPTED'),
  ('NR2_GB3', 'NR2_G3', 'f900001f-0000-0000-0000-00000000001f', 'NR2 bidder', 100, 'ACCEPTED'),
  ('NR2_GB4', 'NR2_G4', 'f900001f-0000-0000-0000-00000000001f', 'NR2 bidder', 100, 'ACCEPTED'),
  ('NR2_GB5', 'NR2_G5', 'f900001f-0000-0000-0000-00000000001f', 'NR2 bidder', 100, 'ACCEPTED'),
  ('NR2_GB6', 'NR2_G6', 'f9000020-0000-0000-0000-000000000020', 'NR2 bidder', 100, 'PENDING'),
  ('NR2_GB7', 'NR2_G7', 'f9000004-0000-0000-0000-000000000004', 'NR2 bidder', 100, 'ACCEPTED'),
  ('NR2_GB8', 'NR2_G8', 'f9000021-0000-0000-0000-000000000021', 'NR2 bidder', 100, 'ACCEPTED'),
  ('NR2_GB9', 'NR2_G9', 'f9000021-0000-0000-0000-000000000021', 'NR2 bidder', 100, 'ACCEPTED'),
  ('NR2_GB10', 'NR2_G10', 'f9000021-0000-0000-0000-000000000021', 'NR2 bidder', 100, 'ACCEPTED');
INSERT INTO public.ratings (id, problem_id, problem_title, user_id, solver_id, stars, comment, rater_role) VALUES
  ('NR2_RT1', 'NR2_RB1', 'NR2 RB1', 'f900002e-0000-0000-0000-00000000002e', 'f900002d-0000-0000-0000-00000000002d', 5, 'fixture', 'USER'),
  ('NR2_RT2', 'NR2_RB2', 'NR2 RB2', 'f900002e-0000-0000-0000-00000000002e', 'f900002d-0000-0000-0000-00000000002d', 4, 'fixture', 'USER'),
  ('NR2_RT3', 'NR2_RB3', 'NR2 RB3', 'f900002e-0000-0000-0000-00000000002e', 'f900002d-0000-0000-0000-00000000002d', 3, 'fixture', 'USER'),
  ('NR2_RT5', 'NR2_RB5', 'NR2 RB5', 'f900002e-0000-0000-0000-00000000002e', 'f9000028-0000-0000-0000-000000000028', 5, 'fixture', 'USER'),
  ('NR2_RT6', 'NR2_RB6', 'NR2 RB6', 'f900002e-0000-0000-0000-00000000002e', 'f9000028-0000-0000-0000-000000000028', 5, 'fixture', 'USER'),
  ('NR2_RT7', 'NR2_RB7', 'NR2 RB7', 'f900002e-0000-0000-0000-00000000002e', 'f9000028-0000-0000-0000-000000000028', 5, 'fixture', 'USER');
-- additional_charges — NR2_AC7-এর solver_id-র কোনো users সারি নেই (stub-এ FK নেই — 05-stub মন্তব্য) → USER_NOT_FOUND শাখা যাচাইয়ের জন্য।
INSERT INTO public.additional_charges (id, problem_id, solver_id, user_id, reason, amount, status, responded_at) VALUES
  ('NR2_AC1', 'NR2_X1', 'f9000015-0000-0000-0000-000000000015', 'f9000014-0000-0000-0000-000000000014', 'fixture', 500, 'ACCEPTED', now() - interval '2 hours'),
  ('NR2_AC2', 'NR2_X2', 'f9000017-0000-0000-0000-000000000017', 'f9000014-0000-0000-0000-000000000014', 'fixture', 5000, 'ACCEPTED', now() - interval '2 hours'),
  ('NR2_AC3', 'NR2_X3', 'f9000016-0000-0000-0000-000000000016', 'f9000014-0000-0000-0000-000000000014', 'fixture', 500, 'ACCEPTED', now() - interval '2 hours'),
  ('NR2_AC5A', 'NR2_X5', 'f9000018-0000-0000-0000-000000000018', 'f9000014-0000-0000-0000-000000000014', 'fixture', 100, 'ACCEPTED', now() - interval '5 hours'),
  ('NR2_AC5B', 'NR2_X5', 'f9000018-0000-0000-0000-000000000018', 'f9000014-0000-0000-0000-000000000014', 'fixture', 500, 'ACCEPTED', now() - interval '1 hour'),
  ('NR2_AC6', 'NR2_X6', 'f9000015-0000-0000-0000-000000000015', 'f9000014-0000-0000-0000-000000000014', 'fixture', 500, 'PENDING', NULL),
  ('NR2_AC7', 'NR2_X7', 'f9ffff01-0000-0000-0000-000000000001', 'f9000014-0000-0000-0000-000000000014', 'fixture', 500, 'ACCEPTED', now() - interval '2 hours');
INSERT INTO public.withdrawals (id, solver_id, solver_name, amount, method, status, role) VALUES
  ('NR2_W1', 'f9000029-0000-0000-0000-000000000029', 'NR2 withdrawer', 500, 'bKash', 'COMPLETED', 'SOLVER'),
  ('NR2_W2', 'f9000029-0000-0000-0000-000000000029', 'NR2 withdrawer', 300, 'bKash', 'PENDING', 'SOLVER'),
  ('NR2_W3', 'f900002a-0000-0000-0000-00000000002a', 'NR2 withdrawer', 2500, 'bKash', 'COMPLETED', 'SOLVER'),
  ('NR2_W4', 'f900002b-0000-0000-0000-00000000002b', 'NR2 withdrawer', 1000, 'bKash', 'COMPLETED', 'SOLVER'),
  ('NR2_W5', 'f900002c-0000-0000-0000-00000000002c', 'NR2 withdrawer', 0, 'bKash', 'COMPLETED', 'SOLVER');

----------------------------------------------------------------------
-- submit_rating — PROBLEM_NOT_FOUND → INVALID_RATER_ROLE → NOT_AUTHORIZED → INSERT
----------------------------------------------------------------------
SELECT test.login_as('f9000001-0000-0000-0000-000000000001');
SELECT throws_ok(
  $$ SELECT public.submit_rating('NR2_NOPE'::text, 5, 'x'::text, 'USER'::text) $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'submit_rating: অস্তিত্বহীন problem → PROBLEM_NOT_FOUND'
);
SELECT throws_ok(
  $$ SELECT public.submit_rating('NR2_NOPE'::text, 5, 'x'::text, 'ADMIN'::text) $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'submit_rating: check-ক্রম — problem না পেলে role অবৈধ হলেও আগে PROBLEM_NOT_FOUND'
);
SELECT throws_ok(
  $$ SELECT public.submit_rating('NR2_R1'::text, 5, 'x'::text, 'ADMIN'::text) $$,
  'P0001', 'INVALID_RATER_ROLE',
  'submit_rating: p_rater_role শুধু USER/SOLVER — ADMIN দিলে INVALID_RATER_ROLE'
);
SELECT throws_ok(
  $$ SELECT public.submit_rating('NR2_R1'::text, 5, 'x'::text, 'user'::text) $$,
  'P0001', 'INVALID_RATER_ROLE',
  'submit_rating: role তুলনা case-sensitive — lower-case user → INVALID_RATER_ROLE'
);
SELECT test.login_as('f9000003-0000-0000-0000-000000000003');
SELECT throws_ok(
  $$ SELECT public.submit_rating('NR2_R1'::text, 5, 'x'::text, 'USER'::text) $$,
  'P0001', 'NOT_AUTHORIZED',
  'submit_rating: p_rater_role=USER কিন্তু caller problem-এর owner নয় → NOT_AUTHORIZED'
);
SELECT throws_ok(
  $$ SELECT public.submit_rating('NR2_R1'::text, 5, 'x'::text, 'SOLVER'::text) $$,
  'P0001', 'NOT_AUTHORIZED',
  'submit_rating: p_rater_role=SOLVER কিন্তু caller accepted solver নয় (আছে solver) → NOT_AUTHORIZED'
);
SELECT test.login_as('f9000001-0000-0000-0000-000000000001');
SELECT throws_ok(
  $$ SELECT public.submit_rating('NR2_R1'::text, 5, 'x'::text, 'SOLVER'::text) $$,
  'P0001', 'NOT_AUTHORIZED',
  'submit_rating: owner নিজেকে SOLVER দাবি করলে NOT_AUTHORIZED'
);
SELECT test.login_as('f9000002-0000-0000-0000-000000000002');
SELECT throws_ok(
  $$ SELECT public.submit_rating('NR2_R1'::text, 5, 'x'::text, 'USER'::text) $$,
  'P0001', 'NOT_AUTHORIZED',
  'submit_rating: accepted solver নিজেকে USER দাবি করলে NOT_AUTHORIZED'
);
RESET ROLE;
SELECT is(
  (SELECT count(*) FROM public.ratings WHERE problem_id = 'NR2_R1')::int,
  0,
  'submit_rating: ব্যর্থ কলগুলো কোনো ratings সারি ঢোকায় না (NR2_R1-এ ০টা)'
);

----------------------------------------------------------------------
-- submit_rating — happy paths (USER ও SOLVER), table CHECK, duplicate/COMPLETED-গার্ড নেই
----------------------------------------------------------------------
SELECT test.login_as('f9000001-0000-0000-0000-000000000001');
SELECT set_config('test.rt_user', (public.submit_rating('NR2_R1'::text, 5, '  great  '::text, 'USER'::text))::text, true);
SELECT is(
  current_setting('test.rt_user')::jsonb->>'result',
  'OK',
  'submit_rating: owner (USER role) happy path → result OK'
);
SELECT ok(
  starts_with(current_setting('test.rt_user')::jsonb->>'rating_id', 'RATE_'),
  'submit_rating: রিটার্ন করা rating_id-র prefix RATE_'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT problem_id, problem_title, user_id::text AS uid, solver_id::text AS sid, stars, comment, rater_role FROM public.ratings WHERE id = (current_setting('test.rt_user')::jsonb->>'rating_id') $$,
  $$ VALUES ('NR2_R1'::text, 'NR2 R1'::text, 'f9000001-0000-0000-0000-000000000001'::text, 'f9000002-0000-0000-0000-000000000002'::text, 5, '  great  '::text, 'USER'::text) $$,
  'submit_rating: সারি — problem_title problem থেকে, user_id=owner, solver_id=accepted solver, stars/rater_role যেমন দেওয়া, comment trim হয় না'
);
SELECT test.login_as('f9000002-0000-0000-0000-000000000002');
SELECT set_config('test.rt_solver', (public.submit_rating('NR2_R1'::text, 4, 'thanks'::text, 'SOLVER'::text))::text, true);
SELECT is(
  current_setting('test.rt_solver')::jsonb->>'result',
  'OK',
  'submit_rating: accepted solver (SOLVER role) happy path → result OK'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT stars, rater_role, user_id::text AS uid, solver_id::text AS sid FROM public.ratings WHERE id = (current_setting('test.rt_solver')::jsonb->>'rating_id') $$,
  $$ VALUES (4, 'SOLVER'::text, 'f9000001-0000-0000-0000-000000000001'::text, 'f9000002-0000-0000-0000-000000000002'::text) $$,
  'submit_rating: SOLVER-rater-এর সারিতেও user_id=owner ও solver_id=accepted solver (rater যেই হোক)'
);
SELECT test.login_as('f9000001-0000-0000-0000-000000000001');
SELECT throws_ok(
  $$ SELECT public.submit_rating('NR2_R1'::text, 0, 'x'::text, 'USER'::text) $$,
  '23514', NULL,
  'submit_rating: stars=0 → SQLSTATE 23514 — table CHECK (inferred stub ratings_stars_range_inferred), RPC নিজে validate করে না'
);
SELECT throws_ok(
  $$ SELECT public.submit_rating('NR2_R1'::text, 6, 'x'::text, 'USER'::text) $$,
  '23514', NULL,
  'submit_rating: stars=6 → SQLSTATE 23514 — table CHECK (inferred), RPC নিজে validate করে না'
);
SELECT set_config('test.rt_dup1', (public.submit_rating('NR2_R4'::text, 3, 'first'::text, 'USER'::text))::text, true);
SELECT set_config('test.rt_dup2', (public.submit_rating('NR2_R4'::text, 3, 'second'::text, 'USER'::text))::text, true);
SELECT is(
  current_setting('test.rt_dup1')::jsonb->>'result',
  'OK',
  'submit_rating: problem COMPLETED না হলেও (IN_PROGRESS) rating গৃহীত হয়'
);
-- [৭.১ ফিক্স, ২০২৬-০৯-২৪] আগে এই টেস্ট "DOCUMENTED CURRENT BEHAVIOUR — duplicate-rating গার্ড নেই,
-- ২য় কলেও নতুন rating_id সহ OK" ডকুমেন্ট করত — এখন idempotency guard যোগ হওয়ায় ২য় কল একই
-- rating_id-ই ফেরত দেয় (নতুন row বানায় না), আর already_rated=true ফ্ল্যাগ থাকে।
SELECT is(
  current_setting('test.rt_dup1')::jsonb->>'already_rated',
  'false',
  'submit_rating: প্রথম কলে already_rated=false'
);
SELECT ok(
  current_setting('test.rt_dup2')::jsonb->>'result' = 'OK' AND current_setting('test.rt_dup2')::jsonb->>'rating_id' = current_setting('test.rt_dup1')::jsonb->>'rating_id',
  'submit_rating: ৭.১ ফিক্স — একই problem+role-এ ২য় কলে নতুন row না বানিয়ে ১ম কলের rating_id-ই ফেরত দেয় (idempotent)'
);
SELECT is(
  current_setting('test.rt_dup2')::jsonb->>'already_rated',
  'true',
  'submit_rating: ৭.১ ফিক্স — ডুপ্লিকেট কলে already_rated=true'
);
RESET ROLE;
SELECT is(
  (SELECT count(*) FROM public.ratings WHERE problem_id = 'NR2_R4' AND rater_role = 'USER')::int,
  1,
  'submit_rating: ৭.১ ফিক্স — দুইবার কল করলেও NR2_R4-এ মাত্র ১টা ratings সারি (আগে ২টা হতো)'
);

----------------------------------------------------------------------
-- submit_rating — DOCUMENTED CURRENT BEHAVIOUR: NULL-তুলনা গার্ড (three-valued logic) — anon ও solver-ছাড়া problem
----------------------------------------------------------------------
SELECT test.logout();
SELECT set_config('test.rt_anon', (public.submit_rating('NR2_R3'::text, 5, 'anon'::text, 'USER'::text))::text, true);
SELECT is(
  current_setting('test.rt_anon')::jsonb->>'result',
  'OK',
  'submit_rating: DOCUMENTED CURRENT BEHAVIOUR ⚠️ auth.uid() NULL (anon, migration-এ GRANT TO anon আছে) — USER-role guard NULL হয়ে পাশ কাটে, rating ঢোকে'
);
RESET ROLE;
RESET ROLE;
SELECT is(
  (SELECT count(*) FROM public.ratings WHERE id = (current_setting('test.rt_anon')::jsonb->>'rating_id') AND rater_role = 'USER' AND user_id = 'f9000001-0000-0000-0000-000000000001')::int,
  1,
  'submit_rating: anon-এর ঢোকানো সারিটা owner-এর নামে (user_id=owner) সত্যিই আছে'
);
SELECT test.login_as('f9000003-0000-0000-0000-000000000003');
SELECT set_config('test.rt_nosolver', (public.submit_rating('NR2_R2'::text, 2, 'no solver'::text, 'SOLVER'::text))::text, true);
SELECT is(
  current_setting('test.rt_nosolver')::jsonb->>'result',
  'OK',
  'submit_rating: DOCUMENTED CURRENT BEHAVIOUR ⚠️ accepted_solver_id NULL problem-এ যেকোনো লগইন-করা ইউজার SOLVER হিসেবে rating ঢোকাতে পারে (auth.uid() <> NULL → NULL)'
);
RESET ROLE;
RESET ROLE;
SELECT is(
  (SELECT count(*) FROM public.ratings WHERE id = (current_setting('test.rt_nosolver')::jsonb->>'rating_id') AND solver_id IS NULL AND rater_role = 'SOLVER')::int,
  1,
  'submit_rating: ওই সারিতে solver_id NULL'
);
SELECT test.login_as('f9000001-0000-0000-0000-000000000001');
SELECT set_config('test.rt_owner_nosolver', (public.submit_rating('NR2_R2'::text, 5, 'owner'::text, 'USER'::text))::text, true);
SELECT is(
  current_setting('test.rt_owner_nosolver')::jsonb->>'result',
  'OK',
  'submit_rating: solver-ছাড়া problem-এ owner (USER) ঠিকঠাক OK — solver_id NULL সহ সারি তৈরি'
);

----------------------------------------------------------------------
-- submit_reputation_event — overload (৫-আর্গ ↔ ৬-আর্গ) ও প্রাথমিক validation
----------------------------------------------------------------------
RESET ROLE;
SELECT is(
  (SELECT count(*)::int FROM pg_proc WHERE proname = 'submit_reputation_event' AND pronamespace = 'public'::regnamespace),
  1,
  'submit_reputation_event: DOCUMENTED CURRENT BEHAVIOUR (12.9-পরবর্তী) — পুরনো ৫-আর্গ overload DROP হওয়ায় এখন public schema-য় ঠিক ১টা overload (৬-আর্গ)'
);
SELECT test.login_as('f9000001-0000-0000-0000-000000000001');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000001-0000-0000-0000-000000000001'::uuid, 'PROBLEM_POSTED'::text, 'NR2_PP1'::text, NULL::numeric, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'submit_reputation_event: DOCUMENTED CURRENT BEHAVIOUR (12.9-পরবর্তী) — পুরনো ৫-আর্গ overload DROP হওয়ায় এই কল আর ambiguous না, বরং p_role DEFAULT NULL দিয়ে ৬-আর্গ ফাংশনেই resolve হয় ও স্বাভাবিক business validation চলে (NR2_PP1 problem-এর মালিক f9000035, f9000001 না → NOT_ELIGIBLE)'
);
SELECT test.logout();
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000001-0000-0000-0000-000000000001'::uuid, 'PROBLEM_POSTED'::text, 'NR2_PP1'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'AUTH_REQUIRED',
  'submit_reputation_event: লগইন ছাড়া (auth.uid() NULL) → AUTH_REQUIRED'
);
SELECT test.login_as('f9000001-0000-0000-0000-000000000001');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event(NULL::uuid, 'PROBLEM_POSTED'::text, 'NR2_PP1'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'USER_ID_REQUIRED',
  'submit_reputation_event: p_user_id NULL → USER_ID_REQUIRED'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000001-0000-0000-0000-000000000001'::uuid, 'BOGUS'::text, 'NR2_PP1'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'UNSUPPORTED_EVENT_TYPE',
  'submit_reputation_event: অজানা event type → UNSUPPORTED_EVENT_TYPE'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000001-0000-0000-0000-000000000001'::uuid, NULL::text, 'NR2_PP1'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'UNSUPPORTED_EVENT_TYPE',
  'submit_reputation_event: event type NULL (→ খালি স্ট্রিং) → UNSUPPORTED_EVENT_TYPE'
);

----------------------------------------------------------------------
-- submit_reputation_event — ADMIN_ADJUSTMENT
----------------------------------------------------------------------
SELECT test.login_as('f9000001-0000-0000-0000-000000000001');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000005-0000-0000-0000-000000000005'::uuid, 'ADMIN_ADJUSTMENT'::text, NULL::text, 5::numeric, NULL::text, 'SOLVER'::text) $$,
  'P0001', 'ADMIN_ONLY',
  'ADMIN_ADJUSTMENT: non-admin caller → ADMIN_ONLY'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000005-0000-0000-0000-000000000005'::uuid, 'admin_adjustment'::text, NULL::text, 5::numeric, NULL::text, 'SOLVER'::text) $$,
  'P0001', 'ADMIN_ONLY',
  'ADMIN_ADJUSTMENT: lower-case event type-ও upper() হয়ে একই শাখায় যায় → non-admin-এর জন্য ADMIN_ONLY'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000005-0000-0000-0000-000000000005'::uuid, 'ADMIN_ADJUSTMENT'::text, NULL::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'SCORE_CHANGE_REQUIRED',
  'ADMIN_ADJUSTMENT: p_score_change NULL → SCORE_CHANGE_REQUIRED (role NULL হলেও এটাই আগে)'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000005-0000-0000-0000-000000000005'::uuid, 'ADMIN_ADJUSTMENT'::text, NULL::text, 5::numeric, NULL::text, NULL::text) $$,
  'P0001', 'ROLE_REQUIRED_FOR_ADMIN_ADJUSTMENT',
  'ADMIN_ADJUSTMENT: p_role NULL → ROLE_REQUIRED_FOR_ADMIN_ADJUSTMENT'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000005-0000-0000-0000-000000000005'::uuid, 'ADMIN_ADJUSTMENT'::text, NULL::text, 5::numeric, NULL::text, 'ADMIN'::text) $$,
  'P0001', 'ROLE_REQUIRED_FOR_ADMIN_ADJUSTMENT',
  'ADMIN_ADJUSTMENT: p_role শুধু USER/SOLVER — ADMIN দিলে ROLE_REQUIRED_FOR_ADMIN_ADJUSTMENT'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000005-0000-0000-0000-000000000005'::uuid, 'ADMIN_ADJUSTMENT'::text, NULL::text, 5::numeric, NULL::text, 'solver'::text) $$,
  'P0001', 'ROLE_REQUIRED_FOR_ADMIN_ADJUSTMENT',
  'ADMIN_ADJUSTMENT: p_role case-sensitive — lower-case solver গৃহীত হয় না'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9ffff02-0000-0000-0000-000000000002'::uuid, 'ADMIN_ADJUSTMENT'::text, NULL::text, 5::numeric, NULL::text, 'SOLVER'::text) $$,
  'P0001', 'USER_NOT_FOUND',
  'ADMIN_ADJUSTMENT: অস্তিত্বহীন p_user_id → USER_NOT_FOUND'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000004-0000-0000-0000-000000000004'::uuid, 'ADMIN_ADJUSTMENT'::text, NULL::text, 5::numeric, NULL::text, 'SOLVER'::text) $$,
  'P0001', 'ROLE_INACTIVE',
  'ADMIN_ADJUSTMENT: টার্গেটের has_solver_role false → p_role=SOLVER-এ ROLE_INACTIVE'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000004-0000-0000-0000-000000000004'::uuid, 'ADMIN_ADJUSTMENT'::text, NULL::text, 5::numeric, NULL::text, 'USER'::text) $$,
  'P0001', 'ROLE_INACTIVE',
  'ADMIN_ADJUSTMENT: টার্গেটের has_user_role false → p_role=USER-এ ROLE_INACTIVE'
);
SELECT set_config('test.adm1', (public.submit_reputation_event('f9000005-0000-0000-0000-000000000005'::uuid, 'ADMIN_ADJUSTMENT'::text, NULL::text, 5.5::numeric, NULL::text, 'SOLVER'::text))::text, true);
SELECT ok(
  current_setting('test.adm1')::jsonb->>'result' = 'OK' AND (current_setting('test.adm1')::jsonb->>'score_change')::numeric = 5.5 AND (current_setting('test.adm1')::jsonb->>'new_score')::numeric = 55.5,
  'ADMIN_ADJUSTMENT: SOLVER +5.5 → OK, score_change 5.5, new_score 55.5'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f9000005-0000-0000-0000-000000000005' $$,
  $$ VALUES (55.5::numeric, 10::numeric, 25.5::numeric) $$,
  'ADMIN_ADJUSTMENT: SOLVER-adjust শুধু combined ও reputation_score_solver বদলায় (50→55.5, 20→25.5); reputation_score_user (10) অপরিবর্তিত'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT event_type, problem_id, score_change, score_after, role, note FROM public.reputation_events WHERE id = (current_setting('test.adm1')::jsonb->>'id') $$,
  $$ VALUES ('ADMIN_ADJUSTMENT'::text, NULL::text, 5.5::numeric, 55.5::numeric, 'SOLVER'::text, 'অ্যাডমিন কর্তৃক রেপুটেশন সমন্বয়'::text) $$,
  'ADMIN_ADJUSTMENT: reputation_events সারি — problem_id NULL, role SOLVER, note ডিফল্ট (migration থেকে extract করা)'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT set_config('test.adm2', (public.submit_reputation_event('f9000005-0000-0000-0000-000000000005'::uuid, 'admin_adjustment'::text, NULL::text, -3::numeric, 'manual fix'::text, 'USER'::text))::text, true);
SELECT ok(
  current_setting('test.adm2')::jsonb->>'result' = 'OK' AND (current_setting('test.adm2')::jsonb->>'score_change')::numeric = -3 AND (current_setting('test.adm2')::jsonb->>'new_score')::numeric = 52.5,
  'ADMIN_ADJUSTMENT: lower-case event type + USER −3 → OK, new_score 52.5'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f9000005-0000-0000-0000-000000000005' $$,
  $$ VALUES (52.5::numeric, 7::numeric, 25.5::numeric) $$,
  'ADMIN_ADJUSTMENT: USER-adjust শুধু combined ও reputation_score_user বদলায় (55.5→52.5, 10→7); solver (25.5) অপরিবর্তিত'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT event_type, problem_id, score_change, score_after, role, note FROM public.reputation_events WHERE id = (current_setting('test.adm2')::jsonb->>'id') $$,
  $$ VALUES ('ADMIN_ADJUSTMENT'::text, NULL::text, -3::numeric, 52.5::numeric, 'USER'::text, 'manual fix'::text) $$,
  'ADMIN_ADJUSTMENT: event_type upper-case-এ সংরক্ষিত, custom p_note হুবহু বসে, role USER'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT set_config('test.adm3', (public.submit_reputation_event('f9000006-0000-0000-0000-000000000006'::uuid, 'ADMIN_ADJUSTMENT'::text, NULL::text, 10::numeric, NULL::text, 'SOLVER'::text))::text, true);
SELECT ok(
  current_setting('test.adm3')::jsonb->>'result' = 'OK' AND (current_setting('test.adm3')::jsonb->>'score_change')::numeric = 10 AND (current_setting('test.adm3')::jsonb->>'new_score')::numeric = 60,
  'ADMIN_ADJUSTMENT: combined ৫০+১০ = ৬০ (clamp হয় না)'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f9000006-0000-0000-0000-000000000006' $$,
  $$ VALUES (60::numeric, 0::numeric, 100::numeric) $$,
  'ADMIN_ADJUSTMENT: combined ও role-scoped আলাদা আলাদাভাবে clamp — solver ৯৫+১০ → ১০০ (clamp), combined ৬০ (clamp ছাড়া)'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT set_config('test.adm4', (public.submit_reputation_event('f9000007-0000-0000-0000-000000000007'::uuid, 'ADMIN_ADJUSTMENT'::text, NULL::text, -5::numeric, NULL::text, 'USER'::text))::text, true);
SELECT ok(
  current_setting('test.adm4')::jsonb->>'result' = 'OK' AND (current_setting('test.adm4')::jsonb->>'score_change')::numeric = -5 AND (current_setting('test.adm4')::jsonb->>'new_score')::numeric = 0,
  'ADMIN_ADJUSTMENT: নিচের clamp — ২−৫ → ০ (new_score 0), event-এ score_change কাঁচা −৫ থাকে'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f9000007-0000-0000-0000-000000000007' $$,
  $$ VALUES (0::numeric, 0::numeric, 0::numeric) $$,
  'ADMIN_ADJUSTMENT: reputation_score ও reputation_score_user দুটোই ০-তে clamp (১−৫ → ০)'
);

----------------------------------------------------------------------
-- submit_reputation_event — INACTIVE_7_DAYS / INACTIVE_30_DAYS
----------------------------------------------------------------------
SELECT test.login_as('f9000003-0000-0000-0000-000000000003');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000008-0000-0000-0000-000000000008'::uuid, 'INACTIVE_7_DAYS'::text, NULL::text, NULL::numeric, NULL::text, 'SOLVER'::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'INACTIVE_7_DAYS: caller নিজে/admin/linked কেউ নয় → NOT_ELIGIBLE'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000008-0000-0000-0000-000000000008'::uuid, 'INACTIVE_7_DAYS'::text, NULL::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'INACTIVE_7_DAYS: check-ক্রম — authorization আগে (p_role NULL হলেও NOT_ELIGIBLE, ROLE_REQUIRED না)'
);
SELECT test.login_as('f900000a-0000-0000-0000-00000000000a');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f900000a-0000-0000-0000-00000000000a'::uuid, 'INACTIVE_7_DAYS'::text, NULL::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'ROLE_REQUIRED_FOR_INACTIVE_DECAY',
  'INACTIVE_7_DAYS: p_role NULL → ROLE_REQUIRED_FOR_INACTIVE_DECAY'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f900000a-0000-0000-0000-00000000000a'::uuid, 'INACTIVE_7_DAYS'::text, NULL::text, NULL::numeric, NULL::text, 'ADMIN'::text) $$,
  'P0001', 'ROLE_REQUIRED_FOR_INACTIVE_DECAY',
  'INACTIVE_7_DAYS: p_role শুধু USER/SOLVER — ADMIN দিলে ROLE_REQUIRED_FOR_INACTIVE_DECAY'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9ffff03-0000-0000-0000-000000000003'::uuid, 'INACTIVE_7_DAYS'::text, NULL::text, NULL::numeric, NULL::text, 'USER'::text) $$,
  'P0001', 'USER_NOT_FOUND',
  'INACTIVE_7_DAYS: admin অস্তিত্বহীন p_user_id দিলে USER_NOT_FOUND'
);
SELECT test.login_as('f9000004-0000-0000-0000-000000000004');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000004-0000-0000-0000-000000000004'::uuid, 'INACTIVE_7_DAYS'::text, NULL::text, NULL::numeric, NULL::text, 'SOLVER'::text) $$,
  'P0001', 'ROLE_INACTIVE',
  'INACTIVE_7_DAYS: নিজের has_solver_role false → ROLE_INACTIVE'
);
SELECT test.login_as('f9000008-0000-0000-0000-000000000008');
SELECT set_config('test.in1', (public.submit_reputation_event('f9000008-0000-0000-0000-000000000008'::uuid, 'INACTIVE_7_DAYS'::text, NULL::text, NULL::numeric, NULL::text, 'SOLVER'::text))::text, true);
SELECT ok(
  current_setting('test.in1')::jsonb->>'result' = 'OK' AND (current_setting('test.in1')::jsonb->>'score_change')::numeric = -2 AND (current_setting('test.in1')::jsonb->>'new_score')::numeric = 48,
  'INACTIVE_7_DAYS: ৪০ দিন নিষ্ক্রিয় ইউজার self-call → OK, ডিফল্ট penalty −2.0 (কোনো platform_settings ছাড়া), new_score 48'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f9000008-0000-0000-0000-000000000008' $$,
  $$ VALUES (48::numeric, 30::numeric, 28::numeric) $$,
  'INACTIVE_7_DAYS: SOLVER-role → combined (50→48) ও reputation_score_solver (30→28) কমে; reputation_score_user (30) অপরিবর্তিত'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT event_type, problem_id, score_change, score_after, role, note FROM public.reputation_events WHERE id = (current_setting('test.in1')::jsonb->>'id') $$,
  $$ VALUES ('INACTIVE_7_DAYS'::text, NULL::text, -2::numeric, 48::numeric, 'SOLVER'::text, '৭ দিন নিষ্ক্রিয় থাকার কারণে রেপুটেশন হ্রাস'::text) $$,
  'INACTIVE_7_DAYS: event সারি — problem_id NULL, role SOLVER, ডিফল্ট ৭-দিনের note'
);
RESET ROLE;
RESET ROLE;
SELECT is(
  (SELECT count(*) FROM public.users WHERE id = 'f9000008-0000-0000-0000-000000000008' AND last_reputation_decay_check_at IS NOT NULL)::int,
  1,
  'INACTIVE_7_DAYS: সফল decay-এ last_reputation_decay_check_at সেট হয়'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT set_config('test.in2', (public.submit_reputation_event('f9000009-0000-0000-0000-000000000009'::uuid, 'INACTIVE_30_DAYS'::text, NULL::text, NULL::numeric, NULL::text, 'USER'::text))::text, true);
SELECT ok(
  current_setting('test.in2')::jsonb->>'result' = 'OK' AND (current_setting('test.in2')::jsonb->>'score_change')::numeric = -5 AND (current_setting('test.in2')::jsonb->>'new_score')::numeric = 45,
  'INACTIVE_30_DAYS: admin caller (self নয়) → OK, ডিফল্ট penalty −5.0, new_score 45'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f9000009-0000-0000-0000-000000000009' $$,
  $$ VALUES (45::numeric, 25::numeric, 0::numeric) $$,
  'INACTIVE_30_DAYS: USER-role → combined (50→45) ও reputation_score_user (30→25) কমে; solver (0) অপরিবর্তিত'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT event_type, problem_id, score_change, score_after, role, note FROM public.reputation_events WHERE id = (current_setting('test.in2')::jsonb->>'id') $$,
  $$ VALUES ('INACTIVE_30_DAYS'::text, NULL::text, -5::numeric, 45::numeric, 'USER'::text, '৩০ দিন নিষ্ক্রিয় থাকার কারণে রেপুটেশন হ্রাস'::text) $$,
  'INACTIVE_30_DAYS: event সারি — ডিফল্ট ৩০-দিনের note, role USER'
);
SELECT test.login_as('f900000a-0000-0000-0000-00000000000a');
SELECT set_config('test.in3', (public.submit_reputation_event('f900000a-0000-0000-0000-00000000000a'::uuid, 'INACTIVE_7_DAYS'::text, NULL::text, NULL::numeric, NULL::text, 'USER'::text))::text, true);
SELECT is(
  current_setting('test.in3')::jsonb->>'result',
  'NOT_YET_INACTIVE',
  'INACTIVE_7_DAYS: মাত্র ৩ দিন আগে সক্রিয় ইউজার → NOT_YET_INACTIVE'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, last_reputation_decay_check_at IS NOT NULL AS checked FROM public.users WHERE id = 'f900000a-0000-0000-0000-00000000000a' $$,
  $$ VALUES (50::numeric, true) $$,
  'NOT_YET_INACTIVE: স্কোর অপরিবর্তিত (৫০), কিন্তু last_reputation_decay_check_at সেট হয় (rate-limit শুরু)'
);
RESET ROLE;
SELECT is(
  (SELECT count(*) FROM public.reputation_events WHERE user_id = 'f900000a-0000-0000-0000-00000000000a' AND event_type = 'INACTIVE_7_DAYS')::int,
  0,
  'NOT_YET_INACTIVE: কোনো reputation_events সারি তৈরি হয় না'
);
SELECT test.login_as('f900000b-0000-0000-0000-00000000000b');
SELECT set_config('test.in4a', (public.submit_reputation_event('f900000b-0000-0000-0000-00000000000b'::uuid, 'INACTIVE_30_DAYS'::text, NULL::text, NULL::numeric, NULL::text, 'USER'::text))::text, true);
SELECT is(
  current_setting('test.in4a')::jsonb->>'result',
  'NOT_YET_INACTIVE',
  'INACTIVE_30_DAYS: ২০ দিন নিষ্ক্রিয় ইউজার — ৩০-দিনের সীমা পেরোয়নি → NOT_YET_INACTIVE (৭-দিনের সীমা পেরোলেও)'
);
SELECT set_config('test.in4b', (public.submit_reputation_event('f900000b-0000-0000-0000-00000000000b'::uuid, 'INACTIVE_7_DAYS'::text, NULL::text, NULL::numeric, NULL::text, 'USER'::text))::text, true);
SELECT is(
  current_setting('test.in4b')::jsonb->>'result',
  'ALREADY_CHECKED_RECENTLY',
  'INACTIVE_7_DAYS: একটু আগেই check হয়ে গেছে (last_check=now()) → ALREADY_CHECKED_RECENTLY, যদিও ইউজার ৭ দিনের বেশি নিষ্ক্রিয়'
);
SELECT test.login_as('f900000c-0000-0000-0000-00000000000c');
SELECT set_config('test.in5a', (public.submit_reputation_event('f900000c-0000-0000-0000-00000000000c'::uuid, 'INACTIVE_7_DAYS'::text, NULL::text, NULL::numeric, NULL::text, 'USER'::text))::text, true);
SELECT is(
  current_setting('test.in5a')::jsonb->>'result',
  'ALREADY_CHECKED_RECENTLY',
  'INACTIVE_7_DAYS: last_check ৩ দিন আগে (<৭ দিন) → ALREADY_CHECKED_RECENTLY'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f900000c-0000-0000-0000-00000000000c' $$,
  $$ VALUES (50::numeric, 0::numeric, 0::numeric) $$,
  'ALREADY_CHECKED_RECENTLY: কোনো স্কোর বদলায় না'
);
RESET ROLE;
UPDATE public.users SET last_reputation_decay_check_at = now() - interval '8 days', updated_at = now() - interval '40 days' WHERE id = 'f900000c-0000-0000-0000-00000000000c';
SELECT test.login_as('f900000c-0000-0000-0000-00000000000c');
SELECT set_config('test.in5b', (public.submit_reputation_event('f900000c-0000-0000-0000-00000000000c'::uuid, 'INACTIVE_7_DAYS'::text, NULL::text, NULL::numeric, NULL::text, 'USER'::text))::text, true);
SELECT ok(
  current_setting('test.in5b')::jsonb->>'result' = 'OK' AND (current_setting('test.in5b')::jsonb->>'score_change')::numeric = -2 AND (current_setting('test.in5b')::jsonb->>'new_score')::numeric = 48,
  'INACTIVE_7_DAYS: last_check ৮ দিন আগে (>৭ দিন) → আর rate-limit নয়, penalty প্রযোজ্য (−2, new_score 48)'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f900000c-0000-0000-0000-00000000000c' $$,
  $$ VALUES (48::numeric, 0::numeric, 0::numeric) $$,
  'INACTIVE_7_DAYS: USER-role decay-এ reputation_score_user ০ থেকে clamp-এ ০ থাকে, combined ৪৮'
);
RESET ROLE;
INSERT INTO public.platform_settings (key, value) VALUES ('rep_penalty_inactive_7d', '-3.5');
SELECT test.login_as('f9000011-0000-0000-0000-000000000011');
SELECT set_config('test.in6', (public.submit_reputation_event('f9000011-0000-0000-0000-000000000011'::uuid, 'INACTIVE_7_DAYS'::text, NULL::text, NULL::numeric, NULL::text, 'USER'::text))::text, true);
SELECT ok(
  current_setting('test.in6')::jsonb->>'result' = 'OK' AND (current_setting('test.in6')::jsonb->>'score_change')::numeric = -3.5 AND (current_setting('test.in6')::jsonb->>'new_score')::numeric = 46.5,
  'INACTIVE_7_DAYS: platform_settings rep_penalty_inactive_7d = -3.5 → -1*abs() = −3.5 (কী-নাম সঠিক, ঋণাত্মক মানও একই penalty)'
);
RESET ROLE;
DELETE FROM public.platform_settings WHERE key = 'rep_penalty_inactive_7d';
SELECT test.login_as('f900000e-0000-0000-0000-00000000000e');
SELECT set_config('test.lnk1', (public.submit_reputation_event('f900000d-0000-0000-0000-00000000000d'::uuid, 'INACTIVE_7_DAYS'::text, NULL::text, NULL::numeric, NULL::text, 'USER'::text))::text, true);
SELECT ok(
  current_setting('test.lnk1')::jsonb->>'result' = 'OK' AND (current_setting('test.lnk1')::jsonb->>'score_change')::numeric = -2 AND (current_setting('test.lnk1')::jsonb->>'new_score')::numeric = 48,
  'INACTIVE_7_DAYS: caller-এর linked_account_id = p_user_id (linked account) → authorized, OK'
);
SELECT test.login_as('f9000010-0000-0000-0000-000000000010');
SELECT set_config('test.lnk2', (public.submit_reputation_event('f900000f-0000-0000-0000-00000000000f'::uuid, 'INACTIVE_7_DAYS'::text, NULL::text, NULL::numeric, NULL::text, 'USER'::text))::text, true);
SELECT ok(
  current_setting('test.lnk2')::jsonb->>'result' = 'OK' AND (current_setting('test.lnk2')::jsonb->>'score_change')::numeric = -2 AND (current_setting('test.lnk2')::jsonb->>'new_score')::numeric = 48,
  'INACTIVE_7_DAYS: p_user_id-র linked_account_id = caller (উল্টো দিক) → authorized, OK'
);

----------------------------------------------------------------------
-- submit_reputation_event — EXTRA_CHARGE_VIA_APP / EXTRA_CHARGE_ACCEPTED (per-problem cap)
----------------------------------------------------------------------
SELECT test.login_as('f9000015-0000-0000-0000-000000000015');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000015-0000-0000-0000-000000000015'::uuid, 'EXTRA_CHARGE_VIA_APP'::text, NULL::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'REF_ID_REQUIRED',
  'EXTRA_CHARGE_VIA_APP: p_ref_id NULL → REF_ID_REQUIRED'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000014-0000-0000-0000-000000000014'::uuid, 'EXTRA_CHARGE_VIA_APP'::text, 'NR2_X1'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'EXTRA_CHARGE_VIA_APP: p_user_id ≠ caller → NOT_ELIGIBLE'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000015-0000-0000-0000-000000000015'::uuid, 'EXTRA_CHARGE_VIA_APP'::text, 'NR2_X6'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'EXTRA_CHARGE_VIA_APP: charge status PENDING (ACCEPTED নয়) → NOT_ELIGIBLE'
);
SELECT test.login_as('f9000014-0000-0000-0000-000000000014');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000014-0000-0000-0000-000000000014'::uuid, 'EXTRA_CHARGE_VIA_APP'::text, 'NR2_X1'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'EXTRA_CHARGE_VIA_APP: caller charge-এর solver নয় (owner) → NOT_ELIGIBLE'
);
SELECT test.login_as('f9000015-0000-0000-0000-000000000015');
SELECT set_config('test.xc1', (public.submit_reputation_event('f9000015-0000-0000-0000-000000000015'::uuid, 'EXTRA_CHARGE_VIA_APP'::text, 'NR2_X1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.xc1')::jsonb->>'result' = 'OK' AND (current_setting('test.xc1')::jsonb->>'score_change')::numeric = 1.5 AND (current_setting('test.xc1')::jsonb->>'new_score')::numeric = 51.5,
  'EXTRA_CHARGE_VIA_APP: ৫০০ টাকার charge → 1.0 + 500/500*0.5 = 1.5, new_score 51.5'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f9000015-0000-0000-0000-000000000015' $$,
  $$ VALUES (51.5::numeric, 0::numeric, 1.5::numeric) $$,
  'EXTRA_CHARGE_VIA_APP: role SOLVER → combined ও reputation_score_solver +1.5; user (0) অপরিবর্তিত'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT event_type, problem_id, score_change, score_after, role, note FROM public.reputation_events WHERE id = (current_setting('test.xc1')::jsonb->>'id') $$,
  $$ VALUES ('EXTRA_CHARGE_VIA_APP'::text, 'NR2_X1'::text, 1.5::numeric, 51.5::numeric, 'SOLVER'::text, 'EXTRA_CHARGE_VIA_APP'::text) $$,
  'EXTRA_CHARGE_VIA_APP: event সারিতে problem_id = p_ref_id, role SOLVER, ডিফল্ট note = event নাম'
);
SELECT test.login_as('f9000014-0000-0000-0000-000000000014');
SELECT set_config('test.xc2', (public.submit_reputation_event('f9000014-0000-0000-0000-000000000014'::uuid, 'EXTRA_CHARGE_ACCEPTED'::text, 'NR2_X1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.xc2')::jsonb->>'result' = 'OK' AND (current_setting('test.xc2')::jsonb->>'score_change')::numeric = 0.5 AND (current_setting('test.xc2')::jsonb->>'new_score')::numeric = 50.5,
  'EXTRA_CHARGE_ACCEPTED: charge-এর user_id = caller → ফ্ল্যাট 0.5, new_score 50.5'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f9000014-0000-0000-0000-000000000014' $$,
  $$ VALUES (50.5::numeric, 0.5::numeric, 0::numeric) $$,
  'EXTRA_CHARGE_ACCEPTED: role USER → combined ও reputation_score_user +0.5; solver (0) অপরিবর্তিত'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT event_type, problem_id, score_change, score_after, role, note FROM public.reputation_events WHERE id = (current_setting('test.xc2')::jsonb->>'id') $$,
  $$ VALUES ('EXTRA_CHARGE_ACCEPTED'::text, 'NR2_X1'::text, 0.5::numeric, 50.5::numeric, 'USER'::text, 'EXTRA_CHARGE_ACCEPTED'::text) $$,
  'EXTRA_CHARGE_ACCEPTED: event সারি — role USER, ডিফল্ট note = event নাম'
);
SELECT test.login_as('f9000017-0000-0000-0000-000000000017');
SELECT set_config('test.xc3', (public.submit_reputation_event('f9000017-0000-0000-0000-000000000017'::uuid, 'EXTRA_CHARGE_VIA_APP'::text, 'NR2_X2'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.xc3')::jsonb->>'result' = 'OK' AND (current_setting('test.xc3')::jsonb->>'score_change')::numeric = 3 AND (current_setting('test.xc3')::jsonb->>'new_score')::numeric = 53,
  'EXTRA_CHARGE_VIA_APP: ৫০০০ টাকার charge → 1.0+5.0 = 6.0 কিন্তু সর্বোচ্চ 3.0-তে সীমিত'
);
SELECT test.login_as('f9000018-0000-0000-0000-000000000018');
SELECT set_config('test.xc4', (public.submit_reputation_event('f9000018-0000-0000-0000-000000000018'::uuid, 'EXTRA_CHARGE_VIA_APP'::text, 'NR2_X5'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.xc4')::jsonb->>'result' = 'OK' AND (current_setting('test.xc4')::jsonb->>'score_change')::numeric = 1.5 AND (current_setting('test.xc4')::jsonb->>'new_score')::numeric = 51.5,
  'EXTRA_CHARGE_VIA_APP: একাধিক ACCEPTED charge থাকলে responded_at সবচেয়ে নতুনটা (৫০০) ব্যবহৃত — ১.৫ (পুরনো ১০০ হলে ১.১ হতো)'
);
SELECT test.login_as('f9000016-0000-0000-0000-000000000016');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000016-0000-0000-0000-000000000016'::uuid, 'EXTRA_CHARGE_VIA_APP'::text, 'NR2_X3'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'ROLE_INACTIVE',
  'EXTRA_CHARGE_VIA_APP: যোগ্য charge আছে কিন্তু has_solver_role false → ROLE_INACTIVE'
);
SELECT test.login_as('f9ffff01-0000-0000-0000-000000000001');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9ffff01-0000-0000-0000-000000000001'::uuid, 'EXTRA_CHARGE_VIA_APP'::text, 'NR2_X7'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'USER_NOT_FOUND',
  'EXTRA_CHARGE_VIA_APP: charge আছে কিন্তু caller-এর users সারি নেই → USER_NOT_FOUND'
);
RESET ROLE;
INSERT INTO public.platform_settings (key, value) VALUES ('extra_bill_reputation_cap_per_problem', '2.0');
SELECT test.login_as('f9000015-0000-0000-0000-000000000015');
SELECT set_config('test.xc5', (public.submit_reputation_event('f9000015-0000-0000-0000-000000000015'::uuid, 'EXTRA_CHARGE_VIA_APP'::text, 'NR2_X1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.xc5')::jsonb->>'result' = 'OK' AND (current_setting('test.xc5')::jsonb->>'score_change')::numeric = 0.5 AND (current_setting('test.xc5')::jsonb->>'new_score')::numeric = 52,
  'per-problem cap ২.০: আগে ১.৫ পেয়েছে → replay-guard নেই, ২য় কলে অবশিষ্ট ০.৫ partial-fit হয় (new_score 52)'
);
SELECT set_config('test.xc6', (public.submit_reputation_event('f9000015-0000-0000-0000-000000000015'::uuid, 'EXTRA_CHARGE_VIA_APP'::text, 'NR2_X1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT is(
  current_setting('test.xc6')::jsonb->>'result',
  'DAILY_CAP_REACHED',
  'per-problem cap পূর্ণ (২.০/২.০) → result DAILY_CAP_REACHED (নাম বিভ্রান্তিকর — আসলে per-problem cap)'
);
RESET ROLE;
SELECT is(
  (SELECT count(*) FROM public.reputation_events WHERE user_id = 'f9000015-0000-0000-0000-000000000015' AND event_type = 'EXTRA_CHARGE_VIA_APP' AND problem_id = 'NR2_X1')::int,
  2,
  'per-problem cap: DAILY_CAP_REACHED কোনো নতুন event সারি ঢোকায় না — মোট ২টা (১.৫ + ০.৫)'
);
RESET ROLE;
DELETE FROM public.platform_settings WHERE key = 'extra_bill_reputation_cap_per_problem';

----------------------------------------------------------------------
-- submit_reputation_event — BID_WON (ডিফল্ট স্কোর ০.৫, দৈনিক cap ২.০)
----------------------------------------------------------------------
SELECT test.login_as('f900001f-0000-0000-0000-00000000001f');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f900001f-0000-0000-0000-00000000001f'::uuid, 'BID_WON'::text, NULL::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'REF_ID_REQUIRED',
  'BID_WON: p_ref_id NULL → REF_ID_REQUIRED'
);
SELECT test.login_as('f9000020-0000-0000-0000-000000000020');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000020-0000-0000-0000-000000000020'::uuid, 'BID_WON'::text, 'NR2_G1'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'BID_WON: এই problem-এ caller-এর কোনো ACCEPTED bid নেই → NOT_ELIGIBLE'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000020-0000-0000-0000-000000000020'::uuid, 'BID_WON'::text, 'NR2_G6'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'BID_WON: bid আছে কিন্তু PENDING (ACCEPTED নয়) → NOT_ELIGIBLE'
);
SELECT test.login_as('f9000003-0000-0000-0000-000000000003');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f900001f-0000-0000-0000-00000000001f'::uuid, 'BID_WON'::text, 'NR2_G1'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'BID_WON: bid ঠিক আছে কিন্তু caller self/problem owner/admin কেউ নয় → NOT_ELIGIBLE'
);
SELECT test.login_as('f900001f-0000-0000-0000-00000000001f');
SELECT set_config('test.bw1', (public.submit_reputation_event('f900001f-0000-0000-0000-00000000001f'::uuid, 'BID_WON'::text, 'NR2_G1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.bw1')::jsonb->>'result' = 'OK' AND (current_setting('test.bw1')::jsonb->>'score_change')::numeric = 0.5 AND (current_setting('test.bw1')::jsonb->>'new_score')::numeric = 50.5,
  'BID_WON: solver self-call → OK, ডিফল্ট স্কোর 0.5, new_score 50.5'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f900001f-0000-0000-0000-00000000001f' $$,
  $$ VALUES (50.5::numeric, 0::numeric, 0.5::numeric) $$,
  'BID_WON: role SOLVER → combined ও reputation_score_solver +0.5'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT event_type, problem_id, score_change, score_after, role, note FROM public.reputation_events WHERE id = (current_setting('test.bw1')::jsonb->>'id') $$,
  $$ VALUES ('BID_WON'::text, 'NR2_G1'::text, 0.5::numeric, 50.5::numeric, 'SOLVER'::text, 'বিড জিতে কাজ পেয়েছেন'::text) $$,
  'BID_WON: event সারি — problem_id=ref, role SOLVER, ডিফল্ট note (migration থেকে)'
);
SELECT set_config('test.bw1b', (public.submit_reputation_event('f900001f-0000-0000-0000-00000000001f'::uuid, 'BID_WON'::text, 'NR2_G1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT is(
  current_setting('test.bw1b')::jsonb->>'result',
  'ALREADY_CLAIMED',
  'BID_WON: একই user+event+ref দ্বিতীয়বার → ALREADY_CLAIMED'
);
RESET ROLE;
SELECT is(
  (SELECT count(*) FROM public.reputation_events WHERE user_id = 'f900001f-0000-0000-0000-00000000001f' AND event_type = 'BID_WON' AND problem_id = 'NR2_G1')::int,
  1,
  'BID_WON: ALREADY_CLAIMED নতুন সারি ঢোকায় না (NR2_G1-এ ১টাই)'
);
SELECT test.login_as('f900001e-0000-0000-0000-00000000001e');
SELECT set_config('test.bw2', (public.submit_reputation_event('f900001f-0000-0000-0000-00000000001f'::uuid, 'BID_WON'::text, 'NR2_G2'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.bw2')::jsonb->>'result' = 'OK' AND (current_setting('test.bw2')::jsonb->>'score_change')::numeric = 0.5 AND (current_setting('test.bw2')::jsonb->>'new_score')::numeric = 51,
  'BID_WON: problem owner (solver-এর হয়ে) caller → authorized, OK'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT set_config('test.bw3', (public.submit_reputation_event('f900001f-0000-0000-0000-00000000001f'::uuid, 'BID_WON'::text, 'NR2_G3'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.bw3')::jsonb->>'result' = 'OK' AND (current_setting('test.bw3')::jsonb->>'score_change')::numeric = 0.5 AND (current_setting('test.bw3')::jsonb->>'new_score')::numeric = 51.5,
  'BID_WON: admin caller → authorized, OK'
);
SELECT test.login_as('f900001f-0000-0000-0000-00000000001f');
SELECT set_config('test.bw4', (public.submit_reputation_event('f900001f-0000-0000-0000-00000000001f'::uuid, 'BID_WON'::text, 'NR2_G4'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.bw4')::jsonb->>'result' = 'OK' AND (current_setting('test.bw4')::jsonb->>'score_change')::numeric = 0.5 AND (current_setting('test.bw4')::jsonb->>'new_score')::numeric = 52,
  'BID_WON: ৪র্থ event — দৈনিক মোট ২.০-তে পৌঁছায়'
);
SELECT set_config('test.bw5', (public.submit_reputation_event('f900001f-0000-0000-0000-00000000001f'::uuid, 'BID_WON'::text, 'NR2_G5'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT is(
  current_setting('test.bw5')::jsonb->>'result',
  'DAILY_CAP_REACHED',
  'BID_WON: ২৪ ঘণ্টায় মোট ২.০ (ডিফল্ট cap) পূর্ণ → পরের বৈধ event-এ DAILY_CAP_REACHED'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f900001f-0000-0000-0000-00000000001f' $$,
  $$ VALUES (52::numeric, 0::numeric, 2::numeric) $$,
  'BID_WON: cap-পূর্ণ কলে স্কোর বদলায় না (combined ৫২, solver ২.০)'
);
SELECT test.login_as('f9000004-0000-0000-0000-000000000004');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000004-0000-0000-0000-000000000004'::uuid, 'BID_WON'::text, 'NR2_G7'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'ROLE_INACTIVE',
  'BID_WON: যোগ্য bid আছে কিন্তু has_solver_role false → ROLE_INACTIVE'
);

----------------------------------------------------------------------
-- submit_reputation_event — JOB_COMPLETED (ডিফল্ট স্কোর ০.৫)
----------------------------------------------------------------------
SELECT test.login_as('f9000032-0000-0000-0000-000000000032');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000032-0000-0000-0000-000000000032'::uuid, 'JOB_COMPLETED'::text, NULL::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'REF_ID_REQUIRED',
  'JOB_COMPLETED: p_ref_id NULL → REF_ID_REQUIRED'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000032-0000-0000-0000-000000000032'::uuid, 'JOB_COMPLETED'::text, 'NR2_J4'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'JOB_COMPLETED: problem COMPLETED নয় (IN_PROGRESS) → NOT_ELIGIBLE'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000003-0000-0000-0000-000000000003'::uuid, 'JOB_COMPLETED'::text, 'NR2_J1'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'JOB_COMPLETED: p_user_id problem-এর owner/accepted solver কেউ নয় → NOT_ELIGIBLE'
);
SELECT test.login_as('f9000003-0000-0000-0000-000000000003');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000032-0000-0000-0000-000000000032'::uuid, 'JOB_COMPLETED'::text, 'NR2_J1'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'JOB_COMPLETED: caller problem-এর party/admin কেউ নয় → NOT_ELIGIBLE'
);
SELECT test.login_as('f9000032-0000-0000-0000-000000000032');
SELECT set_config('test.jc1', (public.submit_reputation_event('f9000032-0000-0000-0000-000000000032'::uuid, 'JOB_COMPLETED'::text, 'NR2_J1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.jc1')::jsonb->>'result' = 'OK' AND (current_setting('test.jc1')::jsonb->>'score_change')::numeric = 0.5 AND (current_setting('test.jc1')::jsonb->>'new_score')::numeric = 50.5,
  'JOB_COMPLETED: owner self-call → OK, ডিফল্ট 0.5'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f9000032-0000-0000-0000-000000000032' $$,
  $$ VALUES (50.5::numeric, 0.5::numeric, 0::numeric) $$,
  'JOB_COMPLETED: p_user_id = owner → role USER → combined ও reputation_score_user +0.5'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT event_type, problem_id, score_change, score_after, role, note FROM public.reputation_events WHERE id = (current_setting('test.jc1')::jsonb->>'id') $$,
  $$ VALUES ('JOB_COMPLETED'::text, 'NR2_J1'::text, 0.5::numeric, 50.5::numeric, 'USER'::text, 'কাজ সম্পন্ন করেছেন'::text) $$,
  'JOB_COMPLETED: owner-এর event সারি — role USER, ডিফল্ট note'
);
SELECT test.login_as('f9000033-0000-0000-0000-000000000033');
SELECT set_config('test.jc2', (public.submit_reputation_event('f9000033-0000-0000-0000-000000000033'::uuid, 'JOB_COMPLETED'::text, 'NR2_J1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.jc2')::jsonb->>'result' = 'OK' AND (current_setting('test.jc2')::jsonb->>'score_change')::numeric = 0.5 AND (current_setting('test.jc2')::jsonb->>'new_score')::numeric = 50.5,
  'JOB_COMPLETED: accepted solver self-call (একই problem-এ owner-এর claim-এর পরেও) → OK, ০.৫'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f9000033-0000-0000-0000-000000000033' $$,
  $$ VALUES (50.5::numeric, 0::numeric, 0.5::numeric) $$,
  'JOB_COMPLETED: p_user_id = accepted solver → role SOLVER → combined ও reputation_score_solver +0.5'
);
SELECT set_config('test.jc2b', (public.submit_reputation_event('f9000033-0000-0000-0000-000000000033'::uuid, 'JOB_COMPLETED'::text, 'NR2_J1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT is(
  current_setting('test.jc2b')::jsonb->>'result',
  'ALREADY_CLAIMED',
  'JOB_COMPLETED: solver-এর একই ref-এ ২য় কল → ALREADY_CLAIMED (owner-এর claim আলাদা, বাধা দেয়নি)'
);
SELECT test.login_as('f9000033-0000-0000-0000-000000000033');
SELECT set_config('test.jc3', (public.submit_reputation_event('f9000032-0000-0000-0000-000000000032'::uuid, 'JOB_COMPLETED'::text, 'NR2_J3'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.jc3')::jsonb->>'result' = 'OK' AND (current_setting('test.jc3')::jsonb->>'score_change')::numeric = 0.5 AND (current_setting('test.jc3')::jsonb->>'new_score')::numeric = 51,
  'JOB_COMPLETED: accepted solver caller owner-এর হয়ে claim করতে পারে (caller party) → owner-এর স্কোর ৫১'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT event_type, problem_id, score_change, score_after, role, note FROM public.reputation_events WHERE id = (current_setting('test.jc3')::jsonb->>'id') $$,
  $$ VALUES ('JOB_COMPLETED'::text, 'NR2_J3'::text, 0.5::numeric, 51::numeric, 'USER'::text, 'কাজ সম্পন্ন করেছেন'::text) $$,
  'JOB_COMPLETED: solver-caller-এর claim owner-এর নামে সংরক্ষিত — role USER'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT set_config('test.jc4', (public.submit_reputation_event('f9000034-0000-0000-0000-000000000034'::uuid, 'JOB_COMPLETED'::text, 'NR2_J2'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.jc4')::jsonb->>'result' = 'OK' AND (current_setting('test.jc4')::jsonb->>'score_change')::numeric = 0.5 AND (current_setting('test.jc4')::jsonb->>'new_score')::numeric = 50.5,
  'JOB_COMPLETED: admin caller → authorized, OK'
);

----------------------------------------------------------------------
-- submit_reputation_event — PROBLEM_POSTED (ডিফল্ট স্কোর ০.২)
----------------------------------------------------------------------
SELECT test.login_as('f9000035-0000-0000-0000-000000000035');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000035-0000-0000-0000-000000000035'::uuid, 'PROBLEM_POSTED'::text, NULL::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'REF_ID_REQUIRED',
  'PROBLEM_POSTED: p_ref_id NULL → REF_ID_REQUIRED'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000035-0000-0000-0000-000000000035'::uuid, 'PROBLEM_POSTED'::text, 'NR2_PP3'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'PROBLEM_POSTED: problem অন্যের (p_user_id owner নয়) → NOT_ELIGIBLE'
);
SELECT test.login_as('f9000003-0000-0000-0000-000000000003');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000035-0000-0000-0000-000000000035'::uuid, 'PROBLEM_POSTED'::text, 'NR2_PP1'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'PROBLEM_POSTED: caller self/admin নয় → NOT_ELIGIBLE'
);
SELECT test.login_as('f9000035-0000-0000-0000-000000000035');
SELECT set_config('test.pp1', (public.submit_reputation_event('f9000035-0000-0000-0000-000000000035'::uuid, 'PROBLEM_POSTED'::text, 'NR2_PP1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.pp1')::jsonb->>'result' = 'OK' AND (current_setting('test.pp1')::jsonb->>'score_change')::numeric = 0.2 AND (current_setting('test.pp1')::jsonb->>'new_score')::numeric = 50.2,
  'PROBLEM_POSTED: owner self-call → OK, ডিফল্ট 0.2'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT reputation_score, reputation_score_user, reputation_score_solver FROM public.users WHERE id = 'f9000035-0000-0000-0000-000000000035' $$,
  $$ VALUES (50.2::numeric, 0.2::numeric, 0::numeric) $$,
  'PROBLEM_POSTED: role USER → combined ও reputation_score_user +0.2'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT event_type, problem_id, score_change, score_after, role, note FROM public.reputation_events WHERE id = (current_setting('test.pp1')::jsonb->>'id') $$,
  $$ VALUES ('PROBLEM_POSTED'::text, 'NR2_PP1'::text, 0.2::numeric, 50.2::numeric, 'USER'::text, 'নতুন সমস্যা পোস্ট করেছেন'::text) $$,
  'PROBLEM_POSTED: event সারি — role USER, ডিফল্ট note'
);
SELECT set_config('test.pp1b', (public.submit_reputation_event('f9000035-0000-0000-0000-000000000035'::uuid, 'PROBLEM_POSTED'::text, 'NR2_PP1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT is(
  current_setting('test.pp1b')::jsonb->>'result',
  'ALREADY_CLAIMED',
  'PROBLEM_POSTED: একই ref-এ ২য় কল → ALREADY_CLAIMED'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT set_config('test.pp2', (public.submit_reputation_event('f9000036-0000-0000-0000-000000000036'::uuid, 'PROBLEM_POSTED'::text, 'NR2_PP3'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.pp2')::jsonb->>'result' = 'OK' AND (current_setting('test.pp2')::jsonb->>'score_change')::numeric = 0.2 AND (current_setting('test.pp2')::jsonb->>'new_score')::numeric = 50.2,
  'PROBLEM_POSTED: admin caller → authorized, OK'
);
SELECT test.login_as('f9000004-0000-0000-0000-000000000004');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000004-0000-0000-0000-000000000004'::uuid, 'PROBLEM_POSTED'::text, 'NR2_PP9'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'ROLE_INACTIVE',
  'PROBLEM_POSTED: has_user_role false ইউজার → ROLE_INACTIVE'
);

----------------------------------------------------------------------
-- submit_reputation_event — RATING_BONUS (৫★=১.৫, ৪★=০.৫, দৈনিক cap ২.০)
----------------------------------------------------------------------
SELECT test.login_as('f900002d-0000-0000-0000-00000000002d');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f900002d-0000-0000-0000-00000000002d'::uuid, 'RATING_BONUS'::text, NULL::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'REF_ID_REQUIRED',
  'RATING_BONUS: p_ref_id NULL → REF_ID_REQUIRED'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f900002d-0000-0000-0000-00000000002d'::uuid, 'RATING_BONUS'::text, 'NR2_RB4'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'RATING_BONUS: ওই problem-এ কোনো rating নেই → NOT_ELIGIBLE'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f900002d-0000-0000-0000-00000000002d'::uuid, 'RATING_BONUS'::text, 'NR2_RB3'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'RATING_BONUS: rating ৩★ (৪-এর কম) → NOT_ELIGIBLE'
);
SELECT test.login_as('f9000003-0000-0000-0000-000000000003');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f900002d-0000-0000-0000-00000000002d'::uuid, 'RATING_BONUS'::text, 'NR2_RB1'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'RATING_BONUS: caller solver/rating-দাতা/admin কেউ নয় → NOT_ELIGIBLE'
);
SELECT test.login_as('f900002d-0000-0000-0000-00000000002d');
SELECT set_config('test.rb1', (public.submit_reputation_event('f900002d-0000-0000-0000-00000000002d'::uuid, 'RATING_BONUS'::text, 'NR2_RB1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.rb1')::jsonb->>'result' = 'OK' AND (current_setting('test.rb1')::jsonb->>'score_change')::numeric = 1.5 AND (current_setting('test.rb1')::jsonb->>'new_score')::numeric = 51.5,
  'RATING_BONUS: ৫★ rating, solver self-call → OK, স্কোর 1.5'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT event_type, problem_id, score_change, score_after, role, note FROM public.reputation_events WHERE id = (current_setting('test.rb1')::jsonb->>'id') $$,
  $$ VALUES ('RATING_BONUS'::text, 'NR2_RB1'::text, 1.5::numeric, 51.5::numeric, 'SOLVER'::text, '5 স্টার রেটিং পেয়েছেন'::text) $$,
  'RATING_BONUS: event সারি — role SOLVER, ডিফল্ট note "<stars> স্টার রেটিং পেয়েছেন"'
);
SELECT test.login_as('f900002e-0000-0000-0000-00000000002e');
SELECT set_config('test.rb2', (public.submit_reputation_event('f900002d-0000-0000-0000-00000000002d'::uuid, 'RATING_BONUS'::text, 'NR2_RB2'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.rb2')::jsonb->>'result' = 'OK' AND (current_setting('test.rb2')::jsonb->>'score_change')::numeric = 0.5 AND (current_setting('test.rb2')::jsonb->>'new_score')::numeric = 52,
  'RATING_BONUS: ৪★ rating, rating-দাতা (ratings.user_id) caller → authorized, স্কোর 0.5'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT event_type, problem_id, score_change, score_after, role, note FROM public.reputation_events WHERE id = (current_setting('test.rb2')::jsonb->>'id') $$,
  $$ VALUES ('RATING_BONUS'::text, 'NR2_RB2'::text, 0.5::numeric, 52::numeric, 'SOLVER'::text, '4 স্টার রেটিং পেয়েছেন'::text) $$,
  'RATING_BONUS: ৪★ event-এর note-এ stars ৪'
);
SELECT test.login_as('f900002d-0000-0000-0000-00000000002d');
SELECT set_config('test.rb1b', (public.submit_reputation_event('f900002d-0000-0000-0000-00000000002d'::uuid, 'RATING_BONUS'::text, 'NR2_RB1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT is(
  current_setting('test.rb1b')::jsonb->>'result',
  'ALREADY_CLAIMED',
  'RATING_BONUS: একই ref-এ ২য় কল → ALREADY_CLAIMED'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT set_config('test.rb5', (public.submit_reputation_event('f9000028-0000-0000-0000-000000000028'::uuid, 'RATING_BONUS'::text, 'NR2_RB5'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.rb5')::jsonb->>'result' = 'OK' AND (current_setting('test.rb5')::jsonb->>'score_change')::numeric = 1.5 AND (current_setting('test.rb5')::jsonb->>'new_score')::numeric = 51.5,
  'RATING_BONUS: admin caller → authorized, ১ম ৫★ → ১.৫'
);
SELECT test.login_as('f9000028-0000-0000-0000-000000000028');
SELECT set_config('test.rb6', (public.submit_reputation_event('f9000028-0000-0000-0000-000000000028'::uuid, 'RATING_BONUS'::text, 'NR2_RB6'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.rb6')::jsonb->>'result' = 'OK' AND (current_setting('test.rb6')::jsonb->>'score_change')::numeric = 0.5 AND (current_setting('test.rb6')::jsonb->>'new_score')::numeric = 52,
  'RATING_BONUS: দৈনিক cap ২.০ — ১.৫ আগে পেয়েছে, ২য় ৫★-এ অবশিষ্ট ০.৫ partial-fit (১.৫ নয়)'
);
SELECT set_config('test.rb7', (public.submit_reputation_event('f9000028-0000-0000-0000-000000000028'::uuid, 'RATING_BONUS'::text, 'NR2_RB7'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT is(
  current_setting('test.rb7')::jsonb->>'result',
  'DAILY_CAP_REACHED',
  'RATING_BONUS: cap (২.০) পূর্ণ → ৩য় ৫★-এ DAILY_CAP_REACHED'
);

----------------------------------------------------------------------
-- submit_reputation_event — WITHDRAWAL_COMPLETED (স্কোর = amount/100 × ০.১, দৈনিক cap ২.০)
----------------------------------------------------------------------
SELECT test.login_as('f9000029-0000-0000-0000-000000000029');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000029-0000-0000-0000-000000000029'::uuid, 'WITHDRAWAL_COMPLETED'::text, NULL::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'REF_ID_REQUIRED',
  'WITHDRAWAL_COMPLETED: p_ref_id NULL → REF_ID_REQUIRED'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000029-0000-0000-0000-000000000029'::uuid, 'WITHDRAWAL_COMPLETED'::text, 'NR2_W2'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'WITHDRAWAL_COMPLETED: withdrawal PENDING (COMPLETED নয়) → NOT_ELIGIBLE'
);
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000029-0000-0000-0000-000000000029'::uuid, 'WITHDRAWAL_COMPLETED'::text, 'NR2_W3'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'WITHDRAWAL_COMPLETED: withdrawal অন্যের (solver_id ≠ p_user_id) → NOT_ELIGIBLE'
);
SELECT test.login_as('f9000003-0000-0000-0000-000000000003');
SELECT throws_ok(
  $$ SELECT public.submit_reputation_event('f9000029-0000-0000-0000-000000000029'::uuid, 'WITHDRAWAL_COMPLETED'::text, 'NR2_W1'::text, NULL::numeric, NULL::text, NULL::text) $$,
  'P0001', 'NOT_ELIGIBLE',
  'WITHDRAWAL_COMPLETED: caller self/admin নয় → NOT_ELIGIBLE'
);
SELECT test.login_as('f9000029-0000-0000-0000-000000000029');
SELECT set_config('test.wd1', (public.submit_reputation_event('f9000029-0000-0000-0000-000000000029'::uuid, 'WITHDRAWAL_COMPLETED'::text, 'NR2_W1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.wd1')::jsonb->>'result' = 'OK' AND (current_setting('test.wd1')::jsonb->>'score_change')::numeric = 0.5 AND (current_setting('test.wd1')::jsonb->>'new_score')::numeric = 50.5,
  'WITHDRAWAL_COMPLETED: ৫০০ টাকা → 500/100 × 0.1 = 0.5, new_score 50.5'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT event_type, problem_id, score_change, score_after, role, note FROM public.reputation_events WHERE id = (current_setting('test.wd1')::jsonb->>'id') $$,
  $$ VALUES ('WITHDRAWAL_COMPLETED'::text, 'NR2_W1'::text, 0.5::numeric, 50.5::numeric, 'SOLVER'::text, 'সফলভাবে উইথড্র সম্পন্ন করেছেন'::text) $$,
  'WITHDRAWAL_COMPLETED: reputation_events.problem_id-তে withdrawal-এর id বসে (⚠️ নামের বিভ্রান্তি), role SOLVER, ডিফল্ট note'
);
SELECT set_config('test.wd1b', (public.submit_reputation_event('f9000029-0000-0000-0000-000000000029'::uuid, 'WITHDRAWAL_COMPLETED'::text, 'NR2_W1'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT is(
  current_setting('test.wd1b')::jsonb->>'result',
  'ALREADY_CLAIMED',
  'WITHDRAWAL_COMPLETED: একই withdrawal ২য়বার → ALREADY_CLAIMED'
);
SELECT test.login_as('f900002a-0000-0000-0000-00000000002a');
SELECT set_config('test.wd3', (public.submit_reputation_event('f900002a-0000-0000-0000-00000000002a'::uuid, 'WITHDRAWAL_COMPLETED'::text, 'NR2_W3'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.wd3')::jsonb->>'result' = 'OK' AND (current_setting('test.wd3')::jsonb->>'score_change')::numeric = 2 AND (current_setting('test.wd3')::jsonb->>'new_score')::numeric = 52,
  'WITHDRAWAL_COMPLETED: ২৫০০ টাকা → কাঁচা ২.৫ কিন্তু দৈনিক cap ২.০-তে সীমিত'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT set_config('test.wd4', (public.submit_reputation_event('f900002b-0000-0000-0000-00000000002b'::uuid, 'WITHDRAWAL_COMPLETED'::text, 'NR2_W4'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.wd4')::jsonb->>'result' = 'OK' AND (current_setting('test.wd4')::jsonb->>'score_change')::numeric = 1 AND (current_setting('test.wd4')::jsonb->>'new_score')::numeric = 51,
  'WITHDRAWAL_COMPLETED: admin caller → authorized, ১০০০ টাকা → ১.০'
);
SELECT test.login_as('f900002c-0000-0000-0000-00000000002c');
SELECT set_config('test.wd5', (public.submit_reputation_event('f900002c-0000-0000-0000-00000000002c'::uuid, 'WITHDRAWAL_COMPLETED'::text, 'NR2_W5'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT is(
  current_setting('test.wd5')::jsonb->>'result',
  'SKIPPED_ZERO_SCORE',
  'WITHDRAWAL_COMPLETED: amount ০ → স্কোর ০ → SKIPPED_ZERO_SCORE'
);
RESET ROLE;
SELECT is(
  (SELECT count(*) FROM public.reputation_events WHERE user_id = 'f900002c-0000-0000-0000-00000000002c' AND event_type = 'WITHDRAWAL_COMPLETED')::int,
  0,
  'SKIPPED_ZERO_SCORE: কোনো event সারি ঢোকে না'
);

----------------------------------------------------------------------
-- submit_reputation_event — platform_settings override (স্কোর ও দৈনিক cap-এর কী-নাম যাচাই)
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.platform_settings (key, value) VALUES ('rep_score_job_completed', '0.9');
RESET ROLE;
INSERT INTO public.platform_settings (key, value) VALUES ('rep_cap_daily_job_completed', '1.0');
SELECT test.login_as('f9000038-0000-0000-0000-000000000038');
SELECT set_config('test.so1', (public.submit_reputation_event('f9000038-0000-0000-0000-000000000038'::uuid, 'JOB_COMPLETED'::text, 'NR2_J5'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.so1')::jsonb->>'result' = 'OK' AND (current_setting('test.so1')::jsonb->>'score_change')::numeric = 0.9 AND (current_setting('test.so1')::jsonb->>'new_score')::numeric = 50.9,
  'rep_score_job_completed = 0.9 → ডিফল্ট ০.৫ নয়, ০.৯ প্রযোজ্য'
);
SELECT set_config('test.so2', (public.submit_reputation_event('f9000038-0000-0000-0000-000000000038'::uuid, 'JOB_COMPLETED'::text, 'NR2_J6'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.so2')::jsonb->>'result' = 'OK' AND (current_setting('test.so2')::jsonb->>'score_change')::numeric = 0.1 AND (current_setting('test.so2')::jsonb->>'new_score')::numeric = 51,
  'rep_cap_daily_job_completed = 1.0 — ০.৯ আগে পেয়েছে, ২য় event-এ অবশিষ্ট ০.১ partial-fit'
);
SELECT set_config('test.so3', (public.submit_reputation_event('f9000038-0000-0000-0000-000000000038'::uuid, 'JOB_COMPLETED'::text, 'NR2_J7'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT is(
  current_setting('test.so3')::jsonb->>'result',
  'DAILY_CAP_REACHED',
  'rep_cap_daily_job_completed পূর্ণ (১.০/১.০) → ৩য় event-এ DAILY_CAP_REACHED'
);
RESET ROLE;
DELETE FROM public.platform_settings WHERE key = 'rep_score_job_completed';
RESET ROLE;
DELETE FROM public.platform_settings WHERE key = 'rep_cap_daily_job_completed';
RESET ROLE;
INSERT INTO public.platform_settings (key, value) VALUES ('rep_cap_daily_bid_won', '0.7');
SELECT test.login_as('f9000021-0000-0000-0000-000000000021');
SELECT set_config('test.sc1', (public.submit_reputation_event('f9000021-0000-0000-0000-000000000021'::uuid, 'BID_WON'::text, 'NR2_G8'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.sc1')::jsonb->>'result' = 'OK' AND (current_setting('test.sc1')::jsonb->>'score_change')::numeric = 0.5 AND (current_setting('test.sc1')::jsonb->>'new_score')::numeric = 50.5,
  'rep_cap_daily_bid_won = 0.7: ১ম BID_WON → ডিফল্ট ০.৫ (cap-এর মধ্যে)'
);
SELECT set_config('test.sc2', (public.submit_reputation_event('f9000021-0000-0000-0000-000000000021'::uuid, 'BID_WON'::text, 'NR2_G9'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.sc2')::jsonb->>'result' = 'OK' AND (current_setting('test.sc2')::jsonb->>'score_change')::numeric = 0.2 AND (current_setting('test.sc2')::jsonb->>'new_score')::numeric = 50.7,
  'rep_cap_daily_bid_won = 0.7: ২য় BID_WON → অবশিষ্ট ০.২ partial-fit (০.৭−০.৫)'
);
SELECT set_config('test.sc3', (public.submit_reputation_event('f9000021-0000-0000-0000-000000000021'::uuid, 'BID_WON'::text, 'NR2_G10'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT is(
  current_setting('test.sc3')::jsonb->>'result',
  'DAILY_CAP_REACHED',
  'rep_cap_daily_bid_won পূর্ণ (০.৭/০.৭) → ৩য় BID_WON-এ DAILY_CAP_REACHED'
);
RESET ROLE;
DELETE FROM public.platform_settings WHERE key = 'rep_cap_daily_bid_won';
RESET ROLE;
INSERT INTO public.platform_settings (key, value) VALUES ('rep_cap_daily_problem_posted', '0.3');
SELECT test.login_as('f9000039-0000-0000-0000-000000000039');
SELECT set_config('test.sp1', (public.submit_reputation_event('f9000039-0000-0000-0000-000000000039'::uuid, 'PROBLEM_POSTED'::text, 'NR2_PP4'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.sp1')::jsonb->>'result' = 'OK' AND (current_setting('test.sp1')::jsonb->>'score_change')::numeric = 0.2 AND (current_setting('test.sp1')::jsonb->>'new_score')::numeric = 50.2,
  'rep_cap_daily_problem_posted = 0.3: ১ম PROBLEM_POSTED → ০.২'
);
SELECT set_config('test.sp2', (public.submit_reputation_event('f9000039-0000-0000-0000-000000000039'::uuid, 'PROBLEM_POSTED'::text, 'NR2_PP5'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT ok(
  current_setting('test.sp2')::jsonb->>'result' = 'OK' AND (current_setting('test.sp2')::jsonb->>'score_change')::numeric = 0.1 AND (current_setting('test.sp2')::jsonb->>'new_score')::numeric = 50.3,
  'rep_cap_daily_problem_posted = 0.3: ২য় → অবশিষ্ট ০.১ partial-fit'
);
SELECT set_config('test.sp3', (public.submit_reputation_event('f9000039-0000-0000-0000-000000000039'::uuid, 'PROBLEM_POSTED'::text, 'NR2_PP6'::text, NULL::numeric, NULL::text, NULL::text))::text, true);
SELECT is(
  current_setting('test.sp3')::jsonb->>'result',
  'DAILY_CAP_REACHED',
  'rep_cap_daily_problem_posted পূর্ণ (০.৩/০.৩) → ৩য়-তে DAILY_CAP_REACHED'
);
RESET ROLE;
DELETE FROM public.platform_settings WHERE key = 'rep_cap_daily_problem_posted';
SELECT test.logout();

SELECT * FROM finish();
ROLLBACK;
