-- 09_kyc_roles_schema_stub.sql — Step 6 (KYC & roles) schema extension
--
-- rule #6 (TEMPORARY/INFERRED, আসল `supabase db dump --schema public` না পাওয়া
-- পর্যন্ত) অনুযায়ী — আগের stub-গুলো (01, 05, 07, 08) না ভেঙে শুধু
-- ALTER TABLE ... ADD COLUMN IF NOT EXISTS দিয়ে extend করা হলো।
--
-- PART 1 (KYC ফাংশন ৪টা: submit_kyc, admin_approve_kyc, admin_reject_kyc,
-- admin_revoke_kyc) যে কলামগুলো লেখে/পড়ে — সবগুলো
-- `recovered_kyc_rating_reputation.sql` (submit_kyc) আর
-- `recovered_admin_kyc_ban_role.sql` (admin_*_kyc)-এর UPDATE স্টেটমেন্ট থেকে
-- হুবহু নেওয়া। অন্য কোনো migration এই কলামগুলো ছোঁয় না (grep করে নিশ্চিত)।
-- টাইপ অনুমান: ফাংশন-বডিতে text প্যারামিটার → text; `= true/false` → boolean;
-- `v_now timestamptz` অ্যাসাইন → timestamptz।
--
-- ⚠️ PART 2 (switch_role_get_or_create_linked_profile, admin_change_role,
-- sync_linked_account_profile, generate_unique_display_uid) যে বাড়তি কলাম
-- লাগবে সেগুলো এই ফাইলের নিচে PART 2 সেশনে যোগ হবে (ফাইল rewrite না করে)।
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS kyc_first_name text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS kyc_last_name text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS kyc_address text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS kyc_document_type text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS kyc_document_number text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS kyc_document_front_image text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS kyc_document_back_image text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS kyc_selfie_image text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS kyc_submission_date timestamptz;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS kyc_reject_reason text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_kyc_verified boolean NOT NULL DEFAULT false;

-- =====================================================================
-- PART 2 (এই সেশন) — switch_role_get_or_create_linked_profile,
-- admin_change_role, sync_linked_account_profile, generate_unique_display_uid
-- =====================================================================
--
-- switch_role_get_or_create_linked_profile (recovered_role_switch.sql) যে
-- কলাম পড়ে/লেখে তার মধ্যে has_user_role/has_solver_role/solver_categories/
-- updated_at/balance_user/balance_solver আগের stub-এ (01) আগে থেকেই আছে;
-- বাকিগুলো (has_completed_solver_setup, reputation_score_user/_solver,
-- is_banned_user/_solver, is_restricted_user/_solver) নতুন — সবগুলো নাম হুবহু
-- ফাংশনের RETURNS jsonb build_object কল থেকে, টাইপ অনুমান (boolean flag /
-- numeric score, বডিতে সরাসরি assignment না থাকায় শুধু read হয়, তাই টাইপ
-- conservative ধরা হলো)।
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS has_completed_solver_setup boolean NOT NULL DEFAULT false;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS reputation_score_user numeric(4,2) NOT NULL DEFAULT 0;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS reputation_score_solver numeric(4,2) NOT NULL DEFAULT 0;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_banned_user boolean NOT NULL DEFAULT false;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_banned_solver boolean NOT NULL DEFAULT false;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_restricted_user boolean NOT NULL DEFAULT false;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_restricted_solver boolean NOT NULL DEFAULT false;

-- sync_linked_account_profile (step32_5_sync_linked_account_profile.sql) —
-- linked_account_id আগের stub-এ আছে; বাকিগুলো (email, address,
-- profile_image_uri, is_verified_badge) নতুন, ফাংশনের UPDATE ... SET থেকে
-- হুবহু কলাম-নাম; টাইপ প্যারামিটারের টাইপ থেকে (text/boolean)।
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS email text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS address text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS profile_image_uri text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_verified_badge boolean NOT NULL DEFAULT false;

-- public.is_same_account_family(uuid, uuid) — rule #6 known blocker।
-- sync_linked_account_profile এটা কল করে authorization-এর জন্য, কিন্তু
-- এই ফাংশনের সংজ্ঞা কোনো migration-এ নেই (case-insensitive grep দিয়ে
-- নিশ্চিত — শুধু migration-এর কমেন্টে নাম উল্লেখ আছে, কোনো CREATE FUNCTION
-- নেই)। যুক্তিসঙ্গত অনুমান: দুইটা uid "একই পরিবার" যদি তাদের root account
-- একই হয় (`linked_account_id` না থাকলে নিজের id-ই root) — is_admin()/
-- resolve_commission_rate()-এর মতোই TEMPORARY/INFERRED হিসেবে চিহ্নিত।
-- আসল সংজ্ঞা পাওয়া গেলে এটাই সবার আগে replace করতে হবে (এই stub-এর ভিত্তিতে
-- sync_linked_account_profile-এর পাস/ফেল দুটোই ভুল হতে পারে)।
CREATE OR REPLACE FUNCTION public.is_same_account_family(p_uid1 uuid, p_uid2 uuid)
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.users u1, public.users u2
    WHERE u1.id = p_uid1 AND u2.id = p_uid2
      AND coalesce(u1.linked_account_id, u1.id) = coalesce(u2.linked_account_id, u2.id)
  );
$$;
