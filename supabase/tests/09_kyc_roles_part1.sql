-- 09_kyc_roles_part1.sql — Step 6 (KYC & roles), PART 1 of 2
--
-- এই ফাইলে কভার করা হয়েছে ৪টা KYC ফাংশন: submit_kyc, admin_approve_kyc,
-- admin_reject_kyc, admin_revoke_kyc। real body verify করা:
--   submit_kyc → recovered_kyc_rating_reputation.sql
--   admin_approve_kyc / admin_reject_kyc / admin_revoke_kyc → recovered_admin_kyc_ban_role.sql
-- (case-insensitive grep-এ কোনো পরের migration এই ৪টাকে override করে না।)
--
-- বাকি ৪টা (switch_role_get_or_create_linked_profile, admin_change_role,
-- sync_linked_account_profile, generate_unique_display_uid) PART 2 — পরের সেশনের
-- কাজ, দেখুন CI_TEST_SUITE_PROGRESS.md।
--
-- ⚠️ এই ফাইল real Postgres+pgTAP-এ চালানো যায়নি (sandbox network বন্ধ ছিল) —
-- শুধু static verification। নির্ভর করে 01 + 09 schema stub-এর উপর (inferred)।
--
-- "DOCUMENTED CURRENT BEHAVIOUR" চিহ্নিত assertion = migration সত্যিই পড়ে পাওয়া
-- আচরণ, "এটাই সঠিক" দাবি না; বদলালে যেন সচেতনভাবে টেস্টও বদলানো হয়।

BEGIN;
SELECT plan(30);

SELECT test.seed_users();
-- CLIENT: 11111111-...  SOLVER 1: 22222222-...  SOLVER 2: 33333333-...  ADMIN: 99999999-...

-- solver 2: আগে থেকেই APPROVED + verified + পুরোনো reject reason (resubmit টেস্টের জন্য)
UPDATE public.users
SET kyc_status = 'APPROVED', is_kyc_verified = true, kyc_reject_reason = 'পুরনো কারণ'
WHERE id = '33333333-3333-3333-3333-333333333333';

-- admin_*_kyc-এর টার্গেট user-রা (প্রতিটা ফাংশনের আলাদা fixture)
INSERT INTO public.users (id, role, name, phone, kyc_status, is_kyc_verified) VALUES
  ('44444444-4444-4444-4444-444444444444', 'SOLVER', 'KYC Approve Target', '01700000044', 'PENDING',  false),
  ('55555555-5555-5555-5555-555555555555', 'SOLVER', 'KYC Reject Target',  '01700000055', 'PENDING',  false),
  ('66666666-6666-6666-6666-666666666666', 'SOLVER', 'KYC Reject-On-Approved Target', '01700000066', 'APPROVED', true),
  ('77777777-7777-7777-7777-777777777777', 'SOLVER', 'KYC Revoke Target',  '01700000077', 'APPROVED', true);

----------------------------------------------------------------------
-- submit_kyc — happy path (solver 1)
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT ok(
  (SELECT r->>'result' = 'OK'
      AND r->>'kyc_status' = 'PENDING'
      AND (r->>'user_id')::uuid = '22222222-2222-2222-2222-222222222222'
   FROM (SELECT public.submit_kyc('রহিম', 'উদ্দিন', 'ঢাকা, বাংলাদেশ', 'NID', '1234567890',
                                  'front.jpg', 'back.jpg', 'selfie.jpg') AS r) s),
  'submit_kyc: happy path — result OK, kyc_status PENDING, user_id = caller'
);

SELECT results_eq(
  $$ SELECT kyc_first_name, kyc_last_name, kyc_address, kyc_document_type, kyc_document_number,
            kyc_document_front_image, kyc_document_back_image, kyc_selfie_image,
            kyc_status, is_kyc_verified
     FROM public.users WHERE id = '22222222-2222-2222-2222-222222222222' $$,
  $$ VALUES ('রহিম'::text, 'উদ্দিন'::text, 'ঢাকা, বাংলাদেশ'::text, 'NID'::text, '1234567890'::text,
             'front.jpg'::text, 'back.jpg'::text, 'selfie.jpg'::text,
             'PENDING'::text, false) $$,
  'submit_kyc: সব KYC কলাম সঠিক সেট হয়, kyc_status=PENDING, is_kyc_verified=false'
);

SELECT isnt(
  (SELECT kyc_submission_date FROM public.users WHERE id = '22222222-2222-2222-2222-222222222222'),
  NULL,
  'submit_kyc: kyc_submission_date সেট হয়'
);

SELECT results_eq(
  $$ SELECT title, target_type, target_id, role FROM public.notifications
     WHERE user_id = '22222222-2222-2222-2222-222222222222' AND target_type = 'kyc' $$,
  $$ VALUES ('KYC আবেদন গৃহীত হয়েছে'::text, 'kyc'::text,
             '22222222-2222-2222-2222-222222222222'::text, 'SOLVER'::text) $$,
  'submit_kyc: caller-কে ঠিক ১টা "আবেদন গৃহীত" notification (target_id = নিজের uid, role=SOLVER)'
);

SELECT ok(
  (SELECT kyc_status IS NULL FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111')
  AND NOT EXISTS (SELECT 1 FROM public.notifications WHERE user_id = '11111111-1111-1111-1111-111111111111'),
  'submit_kyc: অন্য user-এর (client) row/notification অপরিবর্তিত'
);

SELECT test.logout();

----------------------------------------------------------------------
-- submit_kyc — resubmit, আগে APPROVED+verified ছিল (solver 2)
----------------------------------------------------------------------
SELECT test.login_as('33333333-3333-3333-3333-333333333333');

SELECT is(
  (public.submit_kyc('করিম', 'মিয়া', 'চট্টগ্রাম', 'PASSPORT', 'P998877', 'f2.jpg', 'b2.jpg', 's2.jpg'))->>'result',
  'OK',
  'submit_kyc: APPROVED user আবার জমা দিলে result OK'
);

SELECT results_eq(
  $$ SELECT kyc_status, is_kyc_verified FROM public.users
     WHERE id = '33333333-3333-3333-3333-333333333333' $$,
  $$ VALUES ('PENDING'::text, false) $$,
  'submit_kyc: resubmit-এ kyc_status→PENDING এবং is_kyc_verified→false (verification হারায়)'
);

-- DOCUMENTED CURRENT BEHAVIOUR: submit_kyc কোথাও kyc_reject_reason ছোঁয় না — পুরোনো
-- reject কারণ resubmit-এর পরেও থেকে যায় (UI PENDING অবস্থায় পুরোনো কারণ দেখালে ঝুঁকি)।
SELECT is(
  (SELECT kyc_reject_reason FROM public.users WHERE id = '33333333-3333-3333-3333-333333333333'),
  'পুরনো কারণ',
  'submit_kyc [DOCUMENTED CURRENT BEHAVIOUR]: resubmit-এ পুরোনো kyc_reject_reason মোছা হয় না'
);

SELECT test.logout();

----------------------------------------------------------------------
-- submit_kyc — failure cases
----------------------------------------------------------------------
SELECT throws_ok(
  $$ SELECT public.submit_kyc('a', 'b', 'c', 'NID', '1', 'f', 'b', 's') $$,
  'NOT_AUTHENTICATED',
  'submit_kyc: লগইন ছাড়া (auth.uid() NULL) NOT_AUTHENTICATED'
);

SELECT test.login_as('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');

SELECT throws_ok(
  $$ SELECT public.submit_kyc('a', 'b', 'c', 'NID', '1', 'f', 'b', 's') $$,
  'USER_ROW_NOT_FOUND_FOR_UID: aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  'submit_kyc: users-এ row নেই এমন uid → USER_ROW_NOT_FOUND_FOR_UID'
);

SELECT test.logout();

----------------------------------------------------------------------
-- admin_approve_kyc
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');  -- non-admin

SELECT throws_ok(
  $$ SELECT public.admin_approve_kyc('44444444-4444-4444-4444-444444444444') $$,
  'NOT_AUTHORIZED',
  'admin_approve_kyc: non-admin কল করলে NOT_AUTHORIZED'
);

SELECT results_eq(
  $$ SELECT kyc_status, is_kyc_verified FROM public.users
     WHERE id = '44444444-4444-4444-4444-444444444444' $$,
  $$ VALUES ('PENDING'::text, false) $$,
  'admin_approve_kyc: NOT_AUTHORIZED হলে টার্গেট user অপরিবর্তিত'
);

SELECT test.logout();
SELECT test.login_as('99999999-9999-9999-9999-999999999999');  -- admin

SELECT is(
  (public.admin_approve_kyc('44444444-4444-4444-4444-444444444444'))->>'result',
  'OK',
  'admin_approve_kyc: admin হিসেবে কল করলে result OK'
);

SELECT results_eq(
  $$ SELECT kyc_status, is_kyc_verified FROM public.users
     WHERE id = '44444444-4444-4444-4444-444444444444' $$,
  $$ VALUES ('APPROVED'::text, true) $$,
  'admin_approve_kyc: kyc_status→APPROVED, is_kyc_verified→true'
);

SELECT results_eq(
  $$ SELECT title, target_type, target_id, role FROM public.notifications
     WHERE user_id = '44444444-4444-4444-4444-444444444444' $$,
  $$ VALUES ('KYC ভেরিফিকেশন সফল! ✅'::text, 'kyc'::text,
             '44444444-4444-4444-4444-444444444444'::text, 'SOLVER'::text) $$,
  'admin_approve_kyc: টার্গেট user-কে ঠিক ১টা "ভেরিফিকেশন সফল" notification'
);

SELECT is(
  (public.admin_approve_kyc('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'))->>'result',
  'USER_NOT_FOUND',
  'admin_approve_kyc: অস্তিত্বহীন user → result USER_NOT_FOUND (exception না)'
);

SELECT test.logout();

----------------------------------------------------------------------
-- admin_reject_kyc
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');  -- non-admin

SELECT throws_ok(
  $$ SELECT public.admin_reject_kyc('55555555-5555-5555-5555-555555555555', 'x') $$,
  'NOT_AUTHORIZED',
  'admin_reject_kyc: non-admin কল করলে NOT_AUTHORIZED'
);

SELECT results_eq(
  $$ SELECT kyc_status, is_kyc_verified, kyc_reject_reason FROM public.users
     WHERE id = '55555555-5555-5555-5555-555555555555' $$,
  $$ VALUES ('PENDING'::text, false, NULL::text) $$,
  'admin_reject_kyc: NOT_AUTHORIZED হলে টার্গেট user অপরিবর্তিত'
);

SELECT test.logout();
SELECT test.login_as('99999999-9999-9999-9999-999999999999');  -- admin

SELECT is(
  (public.admin_reject_kyc('55555555-5555-5555-5555-555555555555', 'ছবি অস্পষ্ট'))->>'result',
  'OK',
  'admin_reject_kyc: admin হিসেবে কল করলে result OK'
);

SELECT results_eq(
  $$ SELECT kyc_status, is_kyc_verified, kyc_reject_reason FROM public.users
     WHERE id = '55555555-5555-5555-5555-555555555555' $$,
  $$ VALUES ('REJECTED'::text, false, 'ছবি অস্পষ্ট'::text) $$,
  'admin_reject_kyc: kyc_status→REJECTED, is_kyc_verified→false, kyc_reject_reason সেট'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.notifications
    WHERE user_id = '55555555-5555-5555-5555-555555555555'
      AND title = 'KYC আবেদন বাতিল হয়েছে'
      AND target_type = 'kyc' AND role = 'SOLVER'
      AND position('ছবি অস্পষ্ট' in message) > 0
  ),
  'admin_reject_kyc: notification-এর message-এ reject কারণ অন্তর্ভুক্ত থাকে'
);

-- DOCUMENTED CURRENT BEHAVIOUR: reject-এ আগের status-এর কোনো guard নেই — APPROVED+verified
-- user-কেও সরাসরি REJECTED করে দেয় (admin_revoke_kyc-এর সাথে কার্যত একই effect)।
SELECT is(
  (public.admin_reject_kyc('66666666-6666-6666-6666-666666666666', 'ভুল তথ্য'))->>'result',
  'OK',
  'admin_reject_kyc [DOCUMENTED CURRENT BEHAVIOUR]: APPROVED user-এও result OK (status guard নেই)'
);

SELECT results_eq(
  $$ SELECT kyc_status, is_kyc_verified FROM public.users
     WHERE id = '66666666-6666-6666-6666-666666666666' $$,
  $$ VALUES ('REJECTED'::text, false) $$,
  'admin_reject_kyc [DOCUMENTED CURRENT BEHAVIOUR]: APPROVED user REJECTED + unverified হয়ে যায়'
);

SELECT is(
  (public.admin_reject_kyc('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'x'))->>'result',
  'USER_NOT_FOUND',
  'admin_reject_kyc: অস্তিত্বহীন user → result USER_NOT_FOUND'
);

SELECT test.logout();

----------------------------------------------------------------------
-- admin_revoke_kyc
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');  -- non-admin

SELECT throws_ok(
  $$ SELECT public.admin_revoke_kyc('77777777-7777-7777-7777-777777777777', 'x') $$,
  'NOT_AUTHORIZED',
  'admin_revoke_kyc: non-admin কল করলে NOT_AUTHORIZED'
);

SELECT results_eq(
  $$ SELECT kyc_status, is_kyc_verified FROM public.users
     WHERE id = '77777777-7777-7777-7777-777777777777' $$,
  $$ VALUES ('APPROVED'::text, true) $$,
  'admin_revoke_kyc: NOT_AUTHORIZED হলে টার্গেট user অপরিবর্তিত'
);

SELECT test.logout();
SELECT test.login_as('99999999-9999-9999-9999-999999999999');  -- admin

SELECT is(
  (public.admin_revoke_kyc('77777777-7777-7777-7777-777777777777', 'জাল ডকুমেন্ট'))->>'result',
  'OK',
  'admin_revoke_kyc: admin হিসেবে কল করলে result OK'
);

-- DOCUMENTED CURRENT BEHAVIOUR: revoke-ও kyc_status='REJECTED' সেট করে ('REVOKED' নামে
-- আলাদা status নেই) — শুধু notification-এর title/message আলাদা; status দেখে revoke আর
-- reject আলাদা করা যায় না।
SELECT results_eq(
  $$ SELECT kyc_status, is_kyc_verified, kyc_reject_reason FROM public.users
     WHERE id = '77777777-7777-7777-7777-777777777777' $$,
  $$ VALUES ('REJECTED'::text, false, 'জাল ডকুমেন্ট'::text) $$,
  'admin_revoke_kyc [DOCUMENTED CURRENT BEHAVIOUR]: kyc_status→REJECTED (REVOKED না), unverified, reason সেট'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.notifications
    WHERE user_id = '77777777-7777-7777-7777-777777777777'
      AND title = 'KYC ভেরিফিকেশন প্রত্যাহার করা হয়েছে'
      AND target_type = 'kyc' AND role = 'SOLVER'
      AND position('জাল ডকুমেন্ট' in message) > 0
  ),
  'admin_revoke_kyc: "প্রত্যাহার" notification-এর message-এ কারণ অন্তর্ভুক্ত থাকে'
);

SELECT is(
  (public.admin_revoke_kyc('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'x'))->>'result',
  'USER_NOT_FOUND',
  'admin_revoke_kyc: অস্তিত্বহীন user → result USER_NOT_FOUND'
);

SELECT test.logout();

SELECT * FROM finish();
ROLLBACK;
