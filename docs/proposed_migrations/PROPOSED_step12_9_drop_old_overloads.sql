-- ============================================================================
-- PROPOSED — Step 12.9 — পুরনো (ছোট) RPC-overload DROP করার প্রস্তাব
-- ============================================================================
-- ⚠️ এটা শুধু প্রস্তাব। supabase/migrations/-এ কিছু বসানো হয়নি (rule #1)। ব্যবহারকারী নিজে
-- Supabase SQL editor/MCP দিয়ে review করে, নিচের প্রতিটা DROP লাইন **আলাদাভাবে uncomment করে**
-- চালাবেন — এই ফাইলটা এখন as-is চালানো উচিত না (নিচে ৪টা statement এখনো ব্যবহারকারীর
-- সিদ্ধান্তের অপেক্ষায় আছে, blanket-apply করলে ভুল হতে পারে)।
--
-- প্রতিটা DROP চালানোর **আগে**:
--   ১. docs/diagnostics/step12_9_duplicate_overload_live_check.sql-এর Q1/Q2 চালিয়ে লাইভে
--      signature/grants এখনো এই ফাইলে ধরে নেওয়া অবস্থার সাথে মিলছে কিনা যাচাই করবেন
--      (migration ফাইল আর লাইভ DB আলাদা হতে পারে — 12.8b/12.8c-এ যেমন হয়েছে)।
--   ২. নিচে "কোড-সাইট বিশ্লেষণ" অংশ পড়ে বুঝবেন Kotlin app এখনো পুরনো overload-এর উপর
--      নির্ভরশীল কিনা।
--
-- ============================================================================
-- অংশ ক — ✅ APPLIED (২০২৬-০৯-২১, ব্যবহারকারীর স্পষ্ট নির্দেশে, Supabase MCP দিয়ে সরাসরি লাইভে,
-- migration নাম `step12_9_drop_old_notification_log_reputation_overloads_part_a`, project
-- `mghvvpndkxnscwryfkib`) — নিচের ৩টা DROP এখন লাইভে কার্যকর, আর কমেন্ট-আউট রাখা হয়নি (ইতিহাসের
-- জন্য as-applied রাখা হলো)। Apply-পরবর্তী MCP যাচাই: ৩টাই এখন single-signature লাইভে।
--
-- এই ৩টার যুক্তি ছিল: Kotlin wrapper সবসময় `p_role` পাঠায় (non-nullable ডিফল্ট ""), তাই পুরনো
-- (role-বিহীন) overload app কোড থেকে কখনো reachable ছিল না (SupabaseSyncManager.kt যাচাই করা
-- হয়েছে)। সতর্কতা: এই নিশ্চয়তা শুধু **এই app**-এর দৃষ্টিকোণ থেকে — অন্য কোনো external
-- client/script যদি সরাসরি PostgREST দিয়ে এই পুরনো signature কল করে থাকত, সেটা এখন ভাঙবে (এই
-- zip-এ এমন কোনো caller দেখা যায়নি, ব্যবহারকারী নিজের organization-এর জ্ঞান অনুযায়ী নিশ্চিত হয়ে
-- apply করার অনুমতি দিয়েছেন)।
-- ============================================================================

-- admin_notify_user: পুরনো ৬-arg (p_role ছাড়া) — ✅ DROP হয়ে গেছে।
DROP FUNCTION IF EXISTS public.admin_notify_user(uuid, text, text, text, text, text);

-- create_notification: পুরনো ৬-arg (p_role ছাড়া) — ✅ DROP হয়ে গেছে।
DROP FUNCTION IF EXISTS public.create_notification(uuid, text, text, text, text, text);

-- log_admin_action: পুরনো ৪-arg (p_role ছাড়া) — ✅ DROP হয়ে গেছে।
DROP FUNCTION IF EXISTS public.log_admin_action(text, text, text, text);

-- ============================================================================
-- অংশ খ — ✅ APPLIED (২০২৬-০৯-২১, একই সেশনে, ব্যবহারকারীর স্পষ্ট নির্দেশে "তাহলে করো", Supabase
-- MCP দিয়ে সরাসরি লাইভে, migration নাম
-- `step12_9_drop_old_balance_ban_restrict_reputation_overloads_part_b`, project
-- `mghvvpndkxnscwryfkib`) — নিচের ৪টা DROP এখন লাইভে কার্যকর।
--
-- এই ৪টার সব traced caller (UI + outbox retry, SomadhanRepository.kt/SomadhanViewModel.kt/
-- AdminUserLookupView.kt/AdminUsersView.kt/OutboxRpcDispatcher.kt) পড়ে যাচাই করা হয়েছিল —
-- বর্তমান app কোড থেকে role কখনো null পাঠানো হতো না (প্রতিটা caller-ই explicit "USER"/"SOLVER"
-- পাঠায়), তাই পুরনো role-বিহীন overload কার্যত unreachable ছিল। Apply-পরবর্তী MCP যাচাই: ৭টা
-- ফাংশনই এখন প্রতিটা single-signature।
-- ============================================================================

-- admin_adjust_balance: পুরনো ৪-arg — ✅ DROP হয়ে গেছে।
DROP FUNCTION IF EXISTS public.admin_adjust_balance(uuid, numeric, boolean, text);

-- admin_set_banned: পুরনো ২-arg — ✅ DROP হয়ে গেছে।
DROP FUNCTION IF EXISTS public.admin_set_banned(uuid, boolean);

-- admin_set_restricted: পুরনো ২-arg — ✅ DROP হয়ে গেছে।
DROP FUNCTION IF EXISTS public.admin_set_restricted(uuid, boolean);

-- submit_reputation_event: পুরনো ৫-arg — ✅ DROP হয়ে গেছে।
DROP FUNCTION IF EXISTS public.submit_reputation_event(uuid, text, text, numeric, text);

-- ============================================================================
-- অংশ গ — এই ধাপের scope-এ না (শুধু রেফারেন্সের জন্য): request_wallet_deposit ও
-- user_confirm_extra_amount-এর ২-signature অবস্থা **ইচ্ছাকৃত/বৈধ** (Step 12.8b/12.8c)।
-- এখানে কোনো DROP প্রস্তাব নেই, ভুল করে অংশ ক/খ-এর প্যাটার্নে বসানো হয়নি।
-- ============================================================================
