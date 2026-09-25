package com.example.data.security

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * [ADMIN_ROLE_PROFILE সেশন ৩ — অংশ ১/২] পারমিশন ক্যাটালগ — এই ফাইলই এখন সোর্স অফ ট্রুথ।
 *
 * `admin-role-management.html` মকআপের `GROUPS` অবজেক্টে যে ক্যাটালগ ছিল সেটা **শুধু ডিজাইন-প্লেসহোল্ডার**
 * ছিল (প্রতি স্ক্রিনে গড়ে ১-২টা অ্যাকশন)। ব্যবহারকারীর নির্দেশে (২০২৬-০৯-২৪, নতুন প্রশ্নের উত্তর ১:
 * "এখনই ২৫টা স্ক্রিন অডিট করে পূর্ণ ক্যাটালগ") নিচের প্রতিটা গ্রুপ/আইটেম/অ্যাকশন আসল `AdminXxxView.kt`
 * ফাইল গ্রেপ করে (viewModel.adminXxx(...) কল, onXxx callback, actionType স্ট্রিং) ভেরিফাই করে বসানো
 * হয়েছে — অনুমান না। প্রতিটা অ্যাকশনের পাশে কমেন্টে আসল ফাংশন/কল-সাইটের নাম লেখা আছে।
 *
 * `tabIndex` = `AdminPanelScreen.kt`-এর `AdminDrawerItem` ইনডেক্স (২০২৬-০৯-২৪ পর্যন্ত ব্যবহৃত ০-২৪,
 * ওখান থেকেই কপি-ভেরিফায়েড)। `null` মানে স্ক্রিনটা এখনো তৈরি হয়নি (সেশন ৩/৪/৫/৬-এর নতুন স্ক্রিন)।
 *
 * ⚠️ **মকআপ বনাম বাস্তবতা — গুরুত্বপূর্ণ অমিল (দেখুন `ADMIN_ROLE_PROFILE_PROGRESS.md`-এর
 * "সেশন ৩ (অংশ ১) — অডিট ফাইন্ডিংস" সেকশন)।** সংক্ষেপে:
 * 1. ✅ **কনফার্মড (২০২৬-০৯-২৪, ব্যবহারকারী):** `users:user_search` (AdminUserLookupView) থেকেই
 *    ব্যান/কেওয়াইসি/সমস্যা/উইথড্রয়াল অ্যাকশন সরাসরি কল হয় — নিচের ক্যাটালগে সেগুলো তাদের
 *    **নিজ নিজ ডোমেইন** আইটেমে রাখা হয়েছে (`users:users:ban`, `users:kyc:approve`, ইত্যাদি),
 *    `user_search`-এর নিজস্ব কী হিসেবে না (তাই `user_search`-এ শুধু `view` অ্যাকশন)। কারণ: বিপরীত
 *    পথ (Lookup-এর নিজস্ব আলাদা কী, যেমন `user_search:manage`) নিরাপত্তা-ফাঁক তৈরি করত — কেউ নিজের
 *    Users/KYC ট্যাবে ব্যান/অ্যাপ্রুভ করতে না পারলেও Lookup ঘুরে গিয়ে করতে পারত। **সেশন ৭.০-এর
 *    `canAct()` এবং সেশন ৩ অংশ ২-এর রোল-এডিটর/লাইভ-প্রিভিউ UI দুটোই এই নিয়ম মেনে বানাতে হবে।**
 * 2. কিছু অ্যাকশন মকআপে ছিল কিন্তু বাস্তব কোডে নেই (বাদ দেওয়া হয়েছে): `finance:withdrawals:delete`,
 *    `finance:extra_charges:mark_settled`, `moderation:reviews:hide`, `system:refund_debug:run_fix`।
 * 3. অনেক অ্যাকশন বাস্তব কোডে আছে কিন্তু মকআপে ছিল না (নতুন যোগ হয়েছে) — যেমন
 *    `users:users:verified_badge`/`reset_password`, `config:categories:toggle_active`/
 *    `set_physical_work_enabled`/`set_virtual_work_enabled`, `finance:escrow:release`/`refund`/
 *    `repair_missing_refunds`, `work:instant_jobs:rebroadcast`/`convert_to_normal_bidding`, ইত্যাদি —
 *    প্রতিটা নিচে কমেন্টে চিহ্নিত।
 * 4. `config:settings` (adminFactoryResetAllData, adminCleanupCorruptedCommissionRates) এবং
 *    `system:explorer`/`system:refund_debug` — এগুলো এত ঝুঁকিপূর্ণ যে গ্র্যানুলার পারমিশনের বদলে
 *    সরাসরি "শুধু সুপার অ্যাডমিন" হওয়া উচিত কিনা (মাস্টার প্রম্পট ধাপ ৪, সাব-স্টেপ ৭.৬-এর মতো) —
 *    এখনো কনফার্ম হয়নি, তাই আপাতত গ্র্যানুলার হিসেবেই ক্যাটালগে রাখা হলো।
 *
 * এই ফাইল দিয়ে এখনো কোনো UI/গেটিং ওয়্যার করা হয়নি (সেশন ৩-এর অংশ ২, পরের সেশনে) — শুধু ডেটা।
 */

data class AdminPermissionAction(val id: String, val label: String)

data class AdminPermissionItem(
    val id: String,
    val label: String,
    val tabIndex: Int?,
    val actions: List<AdminPermissionAction>
)

data class AdminPermissionGroup(
    val id: String,
    val title: String,
    val superOnly: Boolean = false,
    val items: List<AdminPermissionItem>
)

object AdminPermissionCatalog {

    /** [সেশন ৩ অংশ ২.২] `AdminPanelScreen.kt`-এ রোল ম্যানেজমেন্ট ট্যাবের ইনডেক্স (০-২৪ ব্যবহৃত ছিল, পরের ফ্রি = ২৫)।
     * ViewModel + Panel + ক্যাটালগ একই কনস্ট্যান্ট ব্যবহার করে। */
    const val ROLE_MGMT_TAB_INDEX = 25

    /** [সেশন ৪] এডমিন অ্যাকাউন্ট ট্যাবের ইনডেক্স — রোল ম্যানেজমেন্টের পরের ফ্রি ইনডেক্স (২৬)। সেশন ৫-এর
     * অ্যাক্টিভিটি লগ (সুপার-অনলি) ও সেশন ৬-এর প্রোফাইলের জন্য পরের ফ্রি ইনডেক্স ২৭। */
    const val ADMIN_ACCOUNTS_TAB_INDEX = 26

    /** [সেশন ৫] অ্যাক্টিভিটি লগ (সুপার-অনলি, ক্লাউড থেকে সব এডমিনের লগ) — পরের ফ্রি ইনডেক্স ২৭। এটা বিদ্যমান
     * লিগ্যাসি লগ-ট্যাব (১২, শুধু ওই ডিভাইসের Room-লগ) থেকে আলাদা। সেশন ৬-এর প্রোফাইলের জন্য পরের ফ্রি = ২৮। */
    const val ACTIVITY_LOG_TAB_INDEX = 27

    private fun act(vararg pairs: Pair<String, String>): List<AdminPermissionAction> =
        pairs.map { AdminPermissionAction(it.first, it.second) }

    val GROUPS: List<AdminPermissionGroup> = listOf(
        AdminPermissionGroup(
            id = "dashboard", title = "ড্যাশবোর্ড",
            items = listOf(
                AdminPermissionItem(
                    "stats", "পরিসংখ্যান ও চার্ট", tabIndex = 0,
                    actions = act(
                        "view" to "দেখুন",
                        "export" to "রিপোর্ট এক্সপোর্ট", // exportUsersCsv/exportProblemsCsv/exportTransactionsCsv/exportAllReports
                        "sync" to "ক্লাউড সিঙ্ক" // onSyncClick; onOpenCloudConfig শুধু নেভিগেশন, আলাদা অ্যাকশন না
                    )
                )
            )
        ),
        AdminPermissionGroup(
            id = "users", title = "ইউজার ব্যবস্থাপনা",
            items = listOf(
                AdminPermissionItem("user_search", "ইউজার/সলভার সার্চ", tabIndex = 18, actions = act("view" to "দেখুন")),
                AdminPermissionItem(
                    "solver_quota", "সলভার কোটা", tabIndex = 19,
                    actions = act(
                        "view" to "দেখুন",
                        "reset_quota" to "ফ্রি কোটা রিসেট", // adminResetSolverFreeQuota
                        "reset_miss_cycle" to "মিস-সাইকেল রিসেট" // adminResetSolverMissCycle
                    )
                ),
                AdminPermissionItem(
                    "users", "ইউজারগণ", tabIndex = 4,
                    actions = act(
                        "view" to "দেখুন",
                        "ban" to "ব্যান", // adminSetBanned
                        "restrict" to "রেস্ট্রিক্ট", // adminSetRestricted
                        "balance_adjust" to "ব্যালেন্স সমন্বয়", // adminAdjustBalance
                        "score_adjust" to "স্কোর সমন্বয়", // adminAdjustReputation
                        "change_role" to "রোল পরিবর্তন", // adminChangeRole
                        "verified_badge" to "ভেরিফায়েড ব্যাজ", // adminSetVerifiedBadge — নতুন, মকআপে ছিল না
                        "reset_password" to "পাসওয়ার্ড রিসেট", // adminResetUserPassword — নতুন
                        // [ADMIN_ROLE_PROFILE সেশন ৭.১.২, ২০২৬-০৯-২৫] "বানাও অ্যাডমিন" — AdminUsersView.kt-এর
                        // ইউজার-কার্ড মেনুর নতুন অ্যাকশন (admin_account_create RPC পুনর্ব্যবহার, in-place
                        // bottom sheet)। hard-super-only (AdminSession.kt-এর ADMIN_HARD_SUPER_ONLY_EXACT_KEYS-ও
                        // দ্রষ্টব্য — RPC নিজেই শুধু সুপার caller accept করে)।
                        "create_admin" to "বানাও অ্যাডমিন", // admin_account_create (নতুন কল-সাইট) — নতুন
                        "delete" to "মুছুন" // onDeleteUser
                    )
                ),
                AdminPermissionItem(
                    "kyc", "কেওয়াইসি", tabIndex = 1,
                    actions = act(
                        "view" to "দেখুন",
                        "approve" to "অনুমোদন", // onApproveKyc / onBulkApprove
                        "reject" to "প্রত্যাখ্যান", // onRejectKyc
                        "revoke" to "বাতিল", // onRevokeKyc
                        "reset_pending" to "পেন্ডিং-এ ফেরত", // onResetKycToPending — নতুন
                        "edit" to "তথ্য এডিট" // onUpdateKycInfo — নতুন
                    )
                ),
                // লিগ্যাসি সব-অ্যাডমিন অ্যাক্টিভিটি লগ (সেশন ৫-এ সুপার-অনলি হবে) — admin_mgmt:activity_log
                // (নতুন, সুপার-অনলি এডমিন-অ্যাক্টিভিটি লগ) থেকে আলাদা, গুলিয়ে ফেলা যাবে না।
                AdminPermissionItem("activity_log", "অ্যাক্টিভিটি লগ (লিগ্যাসি)", tabIndex = 12, actions = act("view" to "দেখুন"))
            )
        ),
        AdminPermissionGroup(
            id = "work", title = "কাজ ও চুক্তি",
            items = listOf(
                AdminPermissionItem(
                    "problems", "সমস্যাসমূহ", tabIndex = 5,
                    actions = act(
                        "view" to "দেখুন",
                        "update_status" to "স্ট্যাটাস পরিবর্তন", // onUpdateStatus / adminUpdateProblemStatus — নতুন
                        "update_budget" to "বাজেট পরিবর্তন", // onUpdateBudget / adminUpdateProblemBudget — নতুন
                        "reassign" to "পুনরায় বরাদ্দ", // onReassign — নতুন
                        "delete" to "মুছুন" // onDeleteProblem / adminDeleteProblem
                    )
                ),
                AdminPermissionItem(
                    "instant_jobs", "জরুরি / ইনস্ট্যান্ট জবস", tabIndex = 21,
                    actions = act(
                        "view" to "দেখুন",
                        "cancel" to "বাতিল", // actionType CANCEL
                        "rebroadcast" to "পুনঃসম্প্রচার", // actionType REBROADCAST — নতুন
                        "convert_normal" to "সাধারণ বিডিং-এ রূপান্তর" // actionType TO_NORMAL_BIDDING — নতুন
                    )
                ),
                // read-only যাচাই হয়েছে (কোনো viewModel.adminXxx() মিউটেটিং কল নেই, শুধু ফোন-কল ইউটিলিটি)
                AdminPermissionItem("cancelled_bids", "বিড বাতিলের ইতিহাস", tabIndex = 14, actions = act("view" to "দেখুন")),
                AdminPermissionItem(
                    "direct_contracts", "সরাসরি চুক্তি প্রজেক্ট", tabIndex = 16,
                    actions = act(
                        "view" to "দেখুন",
                        "update_status" to "স্ট্যাটাস পরিবর্তন", // adminUpdateDirectContractStatus
                        "cancel_refund" to "বাতিল ও রিফান্ড", // adminCancelAndRefundDirectContract — নতুন
                        "escalate" to "এসকালেট", // adminEscalateDirectContract — নতুন
                        "send_message" to "মেসেজ পাঠান", // adminSendMessageToProblemChat (ওভারল্যাপ: chat_monitoring)
                        "delete_message" to "মেসেজ মুছুন" // adminDeleteMessage (ওভারল্যাপ: chat_monitoring)
                    )
                ),
                AdminPermissionItem(
                    "chat_monitoring", "চ্যাট মনিটরিং", tabIndex = 15,
                    actions = act(
                        "view" to "দেখুন",
                        "send_message" to "মেসেজ পাঠান", // adminSendMessageToProblemChat
                        "delete_message" to "মেসেজ মুছুন" // adminDeleteMessage
                    )
                )
            )
        ),
        AdminPermissionGroup(
            id = "finance", title = "আর্থিক ব্যবস্থাপনা",
            items = listOf(
                AdminPermissionItem(
                    "withdrawals", "উইথড্রয়াল", tabIndex = 2,
                    actions = act(
                        "view" to "দেখুন",
                        "approve" to "অনুমোদন (COMPLETED)", // onUpdateStatus(..., "COMPLETED", trx)
                        "reject" to "প্রত্যাখ্যান" // onUpdateStatus(..., "REJECTED", reason)
                        // "delete" মকআপে ছিল, কোডে নেই — বাদ দেওয়া হয়েছে
                    )
                ),
                AdminPermissionItem(
                    "escrow", "Escrow", tabIndex = 7,
                    actions = act(
                        "view" to "দেখুন",
                        "release" to "রিলিজ", // adminReleaseEscrow — নতুন
                        "refund" to "রিফান্ড", // adminRefundEscrow — নতুন
                        "reconcile" to "রিকনসাইল", // adminReconcileBalances
                        "repair_refunds" to "মিসিং রিফান্ড রিপেয়ার" // adminRepairMissingRefunds — নতুন
                    )
                ),
                // read-only যাচাই হয়েছে — মকআপের "mark_settled" কোডে খুঁজে পাওয়া যায়নি (সম্ভবত cron/RPC-only,
                // UI বাটন নেই) — কনফার্ম করা দরকার এটা ইচ্ছাকৃত না বাদ পড়ে গেছে
                AdminPermissionItem("extra_charges", "অতিরিক্ত চার্জ", tabIndex = 8, actions = act("view" to "দেখুন")),
                // read-only যাচাই হয়েছে (CSV এক্সপোর্ট আছে কিন্তু কোনো DB-মিউটেশন নেই)
                AdminPermissionItem("transactions", "লেনদেন হিস্ট্রি", tabIndex = 9, actions = act("view" to "দেখুন", "export" to "CSV এক্সপোর্ট")),
                AdminPermissionItem(
                    "gateway_payments", "অনলাইন গেটওয়ে লেনদেন", tabIndex = 23,
                    actions = act(
                        "view" to "দেখুন",
                        "update_status" to "স্ট্যাটাস পরিবর্তন" // adminUpdateGatewayPaymentStatus — নতুন; মকআপ ধরে নিয়েছিল read-only, বাস্তবে না
                    )
                )
            )
        ),
        AdminPermissionGroup(
            id = "config", title = "কনফিগারেশন",
            items = listOf(
                AdminPermissionItem(
                    "categories", "ক্যাটাগরি", tabIndex = 6,
                    actions = act(
                        "view" to "দেখুন",
                        "create" to "তৈরি", // onAddClick
                        "edit" to "এডিট", // onEditCategory/onSave
                        "toggle_active" to "সক্রিয়/নিষ্ক্রিয় টগল", // onToggleActive — নতুন
                        "physical_work_toggle" to "ফিজিক্যাল-ওয়ার্ক টগল", // onSetPhysicalWorkEnabled — নতুন
                        "virtual_work_toggle" to "ভার্চুয়াল-ওয়ার্ক টগল", // onSetVirtualWorkEnabled — নতুন
                        "delete" to "মুছুন" // onDeleteCategory
                    )
                ),
                AdminPermissionItem(
                    "reputation", "রেপুটেশন ইঞ্জিন", tabIndex = 20,
                    actions = act(
                        "view" to "দেখুন",
                        "edit_settings" to "সেটিংস/ক্যাপ/স্কোর এডিট", // adminBatchUpdatePlatformSettings, onCapChange, onScoreChange
                        "manage_custom_events" to "কাস্টম ইভেন্ট তৈরি/এডিট/মুছুন/টগল" // adminSaveCustomReputationEvent/adminDeleteCustomReputationEvent/adminToggleCustomReputationEventStatus — নতুন, একসাথে গ্রুপ করা
                    )
                ),
                AdminPermissionItem(
                    "settings", "সেটিংস", tabIndex = 11,
                    actions = act(
                        "view" to "দেখুন",
                        "edit" to "সাধারণ সেটিং এডিট", // adminUpdatePlatformSetting
                        "run_quota_reset" to "মাসিক ফ্রি-কোটা রিসেট রান", // adminRunMonthlyFreeQuotaReset — নতুন
                        "cleanup_commission" to "কমিশন-রেট ক্লিনআপ", // adminCleanupCorruptedCommissionRates — নতুন, ঝুঁকিপূর্ণ
                        "factory_reset" to "ফ্যাক্টরি রিসেট (সব ডেটা মুছুন)" // adminFactoryResetAllData — নতুন, অত্যন্ত ঝুঁকিপূর্ণ; নিজের পাসওয়ার্ড-পরিবর্তন (adminUpdateCredentials) এই ক্যাটালগে নেই, সবসময় নিজের জন্য allowed থাকবে (প্রোফাইল স্কোপ, সেশন ৬)
                    )
                ),
                AdminPermissionItem(
                    "faq", "FAQ ম্যানেজমেন্ট", tabIndex = 13,
                    actions = act(
                        "view" to "দেখুন",
                        "create" to "তৈরি", // onAddClick
                        "edit" to "এডিট", // onEditFaq/onSave
                        "delete" to "মুছুন" // onDeleteFaq
                    )
                )
            )
        ),
        AdminPermissionGroup(
            id = "disputes", title = "বিরোধ ব্যবস্থাপনা",
            items = listOf(
                AdminPermissionItem(
                    "dispute_center", "বিরোধ কেন্দ্র", tabIndex = 22,
                    actions = act(
                        "view" to "দেখুন",
                        "resolve_split" to "স্প্লিট-রেজল্যুশন", // adminResolveDispute
                        "manual_flag" to "ম্যানুয়াল ফ্ল্যাগ", // adminManuallyFlagDispute — নতুন
                        "issue_warning" to "ওয়ার্নিং/স্ট্রাইক ইস্যু", // adminIssueWarningStrike — নতুন
                        "send_message" to "মেসেজ পাঠান", // adminSendMessageToProblemChat
                        "delete_message" to "মেসেজ মুছুন" // adminDeleteMessage
                    )
                )
            )
        ),
        AdminPermissionGroup(
            id = "contact", title = "যোগাযোগ",
            items = listOf(
                AdminPermissionItem(
                    "notifications", "বিজ্ঞপ্তি প্রেরণ", tabIndex = 3,
                    actions = act(
                        "view" to "দেখুন",
                        "send" to "পাঠান", // onSendNotification
                        "cancel_scheduled" to "শিডিউল বাতিল", // onCancelScheduledNotification — নতুন
                        "delete" to "মুছুন" // onDeleteNotification — নতুন
                    )
                )
            )
        ),
        AdminPermissionGroup(
            id = "moderation", title = "মডারেশন",
            items = listOf(
                AdminPermissionItem(
                    "reviews", "রিভিউ মডারেশন", tabIndex = 10,
                    actions = act(
                        "view" to "দেখুন",
                        "delete" to "মুছুন" // onDelete — মকআপে "hide" ছিল, কোডে নেই, বাদ দেওয়া হয়েছে
                    )
                )
            )
        ),
        AdminPermissionGroup(
            id = "system", title = "সিস্টেম / অ্যাডভান্সড",
            items = listOf(
                AdminPermissionItem(
                    "explorer", "Supabase ডাটা এক্সপ্লোরার", tabIndex = 17,
                    actions = act(
                        "view" to "দেখুন",
                        "edit_field" to "ফিল্ড এডিট", // onEditField / SUPABASE_EXPLORER_UPDATE_FIELD
                        "insert_row" to "রো ইনসার্ট", // SUPABASE_EXPLORER_INSERT_ROW — নতুন
                        "delete_row" to "রো মুছুন" // onDeleteRow / SUPABASE_EXPLORER_DELETE_ROW
                    )
                ),
                // ⚠️ যাচাই: এই স্ক্রিনে কোনো viewModel মিউটেটিং কল নেই — শুধু runDiagnostic() (read) +
                // ক্লিপবোর্ডে কপি। মকআপের "run_fix" অ্যাকশন বাস্তবে নেই, তাই বাদ।
                AdminPermissionItem("refund_debug", "রিফান্ড ডায়াগনস্টিক (Debug)", tabIndex = 24, actions = act("view" to "দেখুন"))
            )
        ),
        // নতুন গ্রুপ — role_mgmt সেশন ৩-এ (ইনডেক্স ২৫), admin_accounts সেশন ৪-এ (ইনডেক্স ২৬); activity_log সেশন ৫-এ (ইনডেক্স ২৭)
        AdminPermissionGroup(
            id = "admin_mgmt", title = "অ্যাডমিন ব্যবস্থাপনা", superOnly = true,
            items = listOf(
                AdminPermissionItem("role_mgmt", "রোল ম্যানেজমেন্ট", tabIndex = ROLE_MGMT_TAB_INDEX, actions = act(
                    "view" to "দেখুন", "create" to "তৈরি", "edit" to "এডিট", "delete" to "মুছুন"
                )),
                AdminPermissionItem("admin_accounts", "এডমিন অ্যাকাউন্ট", tabIndex = ADMIN_ACCOUNTS_TAB_INDEX, actions = act(
                    "view" to "দেখুন", "create" to "তৈরি", "edit" to "এডিট",
                    "set_active" to "সক্রিয়/নিষ্ক্রিয়", "set_flagged" to "ফ্ল্যাগ/আনফ্ল্যাগ"
                )),
                AdminPermissionItem("activity_log", "এডমিন অ্যাক্টিভিটি লগ", tabIndex = ACTIVITY_LOG_TAB_INDEX, actions = act("view" to "দেখুন"))
            )
        )
    )

    fun allKeys(): List<String> =
        GROUPS.flatMap { g -> g.items.flatMap { i -> i.actions.map { a -> "${g.id}:${i.id}:${a.id}" } } }

    fun nonSuperKeys(): List<String> =
        GROUPS.filter { !it.superOnly }.flatMap { g -> g.items.flatMap { i -> i.actions.map { a -> "${g.id}:${i.id}:${a.id}" } } }

    fun findItem(groupId: String, itemId: String): AdminPermissionItem? =
        GROUPS.find { it.id == groupId }?.items?.find { it.id == itemId }
}

/**
 * [ADMIN_ROLE_PROFILE সেশন ৩ — অংশ ২.১] একটা রোলের সার্ভার-স্ন্যাপশট — `admin_roles_list`/
 * `admin_role_upsert` RPC-র জবাবের সাথে ১:১ মেলে (দেখুন migration
 * `zz_20260924150000_admin_role_system_session1_schema_rpc.sql`-এর `admin_roles_list`)।
 * `AdminRoleManagementView.kt` এই টাইপেই কাজ করে, raw JSON নিয়ে না।
 */
data class AdminRoleInfo(
    val id: String,
    val name: String,
    val isSuper: Boolean,
    val permissions: Set<String>,
    val accountCount: Int,
    val createdAt: String?,
    val updatedAt: String?
) {
    companion object {
        fun fromJson(obj: JsonObject): AdminRoleInfo? {
            fun str(k: String): String? =
                (obj[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
            fun bool(k: String): Boolean =
                (obj[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content == "true"
            val id = str("id") ?: return null
            val perms = (obj["permissions"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p !is JsonNull }?.content }
                ?.toSet() ?: emptySet()
            return AdminRoleInfo(
                id = id,
                name = str("name") ?: "",
                isSuper = bool("is_super"),
                permissions = perms,
                accountCount = str("account_count")?.toIntOrNull() ?: 0,
                createdAt = str("created_at"),
                updatedAt = str("updated_at")
            )
        }
    }
}
