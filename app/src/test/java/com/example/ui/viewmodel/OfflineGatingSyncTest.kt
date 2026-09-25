package com.example.ui.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Step 16.3 — CI_TEST_SUITE_MASTER_PROMPT.md.
 *
 * ============================================================================
 * কেন এটা source-scan (static structural) টেস্ট, real `SomadhanViewModel` instantiate
 * করে (Robolectric দিয়ে `_isOnline` টগল করে) actual runtime gating verify করা টেস্ট না
 * ============================================================================
 * `SomadhanViewModel(application: Application) : AndroidViewModel(application)` -- constructor-এ
 * সরাসরি `SomadhanRepository(AppDatabase.getDatabase(application))` বানায়, আর `init {}`
 * ব্লকে `viewModelScope.launch { ... }` দিয়ে কয়েকটা ব্যাকগ্রাউন্ড কোরুটিন শুরু করে যেগুলো আসল
 * `SupabaseRealtimeManager`/`repository.*` নেটওয়ার্ক কল করে (`runCatching { }`-এ মোড়া থাকায়
 * থ্রো করে না, কিন্তু আসল HTTP কল চেষ্টা করে)। `Room` ইন-মেমোরি ডাটাবেস Robolectric দিয়ে বানানো
 * যায় (`OutboxSyncTest.kt`-এর প্যাটার্ন), কিন্তু পুরো ViewModel বানাতে গেলে সেই ব্যাকগ্রাউন্ড
 * নেটওয়ার্ক-কলগুলোও (sandbox-এ ব্লকড, বা real CI-তে non-deterministic/ধীর) সাথে চালু হয়ে যাবে --
 * `DualWriteGapTest.kt`-এ (Step 12 PART 2) যে একই কারণে `SupabaseSyncManager`/`SupabaseAuthManager`
 * (দুটোই Kotlin `object`, কোনো DI seam নেই, `mockk` `build.gradle.kts`-এ নেই ও rule ১ অনুযায়ী
 * সেই ফাইল এডিট করা যাবে না) mock করে রানটাইম টেস্ট লেখা যায়নি, ঠিক সেই একই আর্কিটেকচারাল
 * সীমাবদ্ধতা এখানেও প্রযোজ্য -- `requireOnlineOrWarn()` নিজেই `isOnline.value` (ViewModel-এর
 * প্রাইভেট `_isOnline` StateFlow) পড়ে, যেটা সেট করার কোনো টেস্ট-হুক এক্সপোজড না, আর পুরো
 * ViewModel instantiate করে reflection দিয়ে প্রাইভেট ফিল্ড বদলানো একটা ভঙ্গুর, non-deterministic
 * (ব্যাকগ্রাউন্ড কোরুটিন-নির্ভর) টেস্ট তৈরি করত।
 *
 * তাই, `DualWriteGapTest.kt`/`WalletDashboardSignWiringTest.kt`-এর কনভেনশন অনুসরণ করে, এই টেস্ট
 * সরাসরি সোর্স টেক্সট থেকে প্রতিটা ফাংশনের boundary বের করে (member-level `fun` regex দিয়ে,
 * ওভারলোড-সহ) verify করে যে "flag" (`requireOnlineOrWarn()` গার্ড কল) আর "actual gating"
 * (কোন ফাংশন আসলে গার্ডেড, কোনটা ইচ্ছাকৃতভাবে না) sync আছে কিনা -- ঠিক master prompt-এর মূল
 * assertion, শুধু runtime execution-এর বদলে compile-time-এর মতো structural verification দিয়ে।
 * এই পদ্ধতি একটা সত্যিকারের ভাঙা wiring (নিচে দেখুন, `loginAsAdmin` কেস) সরাসরি ধরেও ফেলেছে --
 * তাই এটা কোনো দুর্বল substitute না, বরং এই কোডবেসের নির্দিষ্ট constraint-এর মধ্যে কার্যকর একটা
 * পদ্ধতি।
 *
 * ============================================================================
 * ⚠️ Step 16.1 inventory-তে পাওয়া তিনটা সংশোধন (এই সেশনে আবিষ্কৃত, সোর্স পড়ে verify করা)
 * ============================================================================
 * Step 16.1-এর টেবিল ৩/৪ (গার্ডেড ৯৬ + গার্ডবিহীন ১৩) হাতে verify করতে গিয়ে auth/session
 * ডোমেইনে একটা লাইন-নাম্বার-সাইটেশন শিফট এরর পাওয়া গেছে (প্রতিটা function name তার comment-এর
 * এক ফাংশন *পরের* comment-এর সাথে ভুলভাবে মেলানো হয়েছিল):
 *  ১. `loginAsAdmin` **আসলে গার্ডবিহীন** (টেবিল ৩-এ ভুলভাবে "গার্ডেড" হিসেবে তালিকাভুক্ত ছিল) --
 *     Step 16.2-এর root-cause finding (গ)-এর সাথেই সামঞ্জস্যপূর্ণ (degraded admin session-এও
 *     স্থানীয়ভাবে এগিয়ে যাওয়ার জন্য ইচ্ছাকৃতভাবে গার্ডবিহীন, ঠিক `loginAsAdmin`-এর নিজের ডক-কমেন্ট
 *     যেমন বলে)। তাই এই টেস্টে `loginAsAdmin` গার্ডেড-৯৫ তালিকা থেকে সরিয়ে গার্ডবিহীন তালিকায়
 *     রাখা হলো।
 *  ২. `validateLoginCredentials` ও `register` **আসলে দুজনেই সরাসরি গার্ডেড** (টেবিল ৪-এ ভুলভাবে
 *     "গার্ডবিহীন" হিসেবে তালিকাভুক্ত ছিল) -- সোর্স verify করে কনফার্মড (`requireOnlineOrWarn()`
 *     কল সরাসরি দুটোতেই আছে)।
 *  ৩. প্রকৃত গার্ডবিহীন auth-সংশ্লিষ্ট ফাংশন তিনটা হলো `completeLoginAfterOtp`, `loginAsAdmin`,
 *     `logout` (টেবিল ৪-এ এই তিনটার বদলে ভুলভাবে `validateLoginCredentials`/`register`/
 *     `loginAsAdmin` লেখা হয়েছিল) -- `completeLoginAfterOtp` ও `logout` টেবিল ৪-এ কোনো নামেই
 *     ছিল না, যদিও তাদের নিজস্ব ডক-কমেন্টে স্পষ্টভাবে "ইচ্ছাকৃতভাবে গার্ড বসানো হয়নি" লেখা আছে।
 *
 * এছাড়া একটা completeness gap (বাগ না, শুধু আগে নথিভুক্ত হয়নি):
 *  ৪. `solverCancelAcceptedJob`-এর **দুটো ওভারলোড আছে** (টেবিল ৩-এ `solverCancelJob`-এর মতো
 *     "২টা ওভারলোড" নোট ছাড়াই একটা single entry হিসেবে তালিকাভুক্ত ছিল)। প্রথম ওভারলোড
 *     (`problem, bid, ...`) সরাসরি গার্ডেড। দ্বিতীয়টা (`problem, reason, ...`) সরাসরি গার্ডেড
 *     না, কিন্তু ভেতরে `solverCancelJob(...)`-কেই কল করে (যেটা নিজেই গার্ডেড) -- অর্থাৎ পরোক্ষভাবে
 *     সুরক্ষিত, কোনো gap না। এই টেস্ট দুটো ওভারলোডকেই আলাদাভাবে সঠিক প্যাটার্নে যাচাই করে।
 *
 * এই তিনটা inventory-সংশোধন এই সেশনে **শুধু progress doc/টেস্টে** প্রতিফলিত হয়েছে -- rule ১
 * অনুযায়ী `loginAsAdmin`-এ কোনো গার্ড যোগ করা (production কোড বদলানো) হয়নি, কারণ Step 16.2-এর
 * root-cause analysis অনুযায়ী এটা ইচ্ছাকৃত আচরণ, কোনো প্রমাণিত বাগ না।
 */
class OfflineGatingSyncTest {

    // ------------------------------------------------------------------
    // সোর্স ফাইল লোড করা (`DualWriteGapTest.kt`-এর একই candidate-path প্যাটার্ন)
    // ------------------------------------------------------------------

    private fun sourceFile(relativeFromApp: String): File {
        val candidates = listOf(
            File(relativeFromApp),
            File("app/$relativeFromApp")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error(
                "$relativeFromApp পাওয়া যায়নি কোনো candidate path-এ " +
                    "(cwd=${File(".").absolutePath}) -- Gradle-এর test working directory " +
                    "বদলেছে সম্ভবত, OfflineGatingSyncTest-এর candidate paths আপডেট করো।"
            )
    }

    private val vmLines: List<String> by lazy {
        sourceFile("src/main/java/com/example/ui/viewmodel/SomadhanViewModel.kt").readLines()
    }

    private val mainActivityText: String by lazy {
        sourceFile("src/main/java/com/example/MainActivity.kt").readText()
    }

    // ------------------------------------------------------------------
    // member-level `fun <name>` খোঁজা -- সব occurrence (ওভারলোড-সহ), লাইন-নাম্বার-নির্ভরতা ছাড়া
    // (DualWriteGapTest.kt-এর functionRegion()-এর মতোই, কিন্তু এখানে একই নামের একাধিক ওভারলোডের
    // *প্রতিটা* region দরকার, শুধু প্রথমটা না)
    // ------------------------------------------------------------------

    private val memberFunPrefix = "^ {4}(?:(?:private|internal|public|suspend|override|inline)\\s+)*fun\\s+"
    private val anyMemberFunRegex = Regex(memberFunPrefix)

    private fun isCommentLine(line: String): Boolean {
        val t = line.trimStart()
        return t.startsWith("//") || t.startsWith("/*") || t.startsWith("*")
    }

    /** সব member-level `fun` শুরুর লাইন (0-indexed) + নাম, ফাইলের ক্রমে। */
    private val allFunStarts: List<Pair<Int, String>> by lazy {
        val nameRegex = Regex(memberFunPrefix + "([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")
        vmLines.mapIndexedNotNull { idx, line ->
            nameRegex.find(line)?.let { idx to it.groupValues[1] }
        }
    }

    /** `funName`-এর সব occurrence-এর region (0-indexed, inclusive), ফাইলে যে ক্রমে আছে সেই ক্রমেই। */
    private fun functionRegions(funName: String): List<IntRange> {
        val myStarts = allFunStarts.filter { it.second == funName }
        return myStarts.map { (startIdx, _) ->
            var endIdx = vmLines.size - 1
            for (i in (startIdx + 1) until vmLines.size) {
                if (anyMemberFunRegex.containsMatchIn(vmLines[i])) {
                    endIdx = i - 1
                    break
                }
            }
            startIdx..endIdx
        }
    }

    private fun regionText(region: IntRange): String = vmLines.subList(region.first, region.last + 1).joinToString("\n")

    private fun bodyCallsGuard(region: IntRange): Boolean =
        region.any { i -> vmLines[i].contains("requireOnlineOrWarn(") && !isCommentLine(vmLines[i]) }

    // ------------------------------------------------------------------
    // ১. `requireOnlineOrWarn()` হেল্পারের নিজস্ব structural verification -- guard বডি এখনো
    //    ডকুমেন্টেড আচরণ মেনে চলছে কিনা: অফলাইনে toast + false, অনলাইনে true, ব্যতিক্রম ছাড়া।
    //    (Runtime-এ `isOnline.value` সেট করে সত্যিই কল করে দেখানো যায় না -- উপরের ক্লাস-লেভেল
    //    KDoc দ্রষ্টব্য -- তাই এটা structural/source-level verification, behavior-level না।)
    // ------------------------------------------------------------------

    @Test
    fun `requireOnlineOrWarn still checks isOnline value, warns and returns false when offline, true otherwise`() {
        val regions = functionRegions("requireOnlineOrWarn")
        assertTrue(
            "`requireOnlineOrWarn` ফাংশনই পাওয়া যায়নি SomadhanViewModel.kt-এ -- rename/সরানো হয়েছে সম্ভবত, " +
                "যেটা এই ফাইলের ৯৫টা গার্ডেড কল-সাইটকেই কম্পাইল-ব্রেক করবে।",
            regions.size == 1
        )
        val text = regionText(regions.single())

        assertTrue(
            "`requireOnlineOrWarn()`-এ আর `if (!isOnline.value)` চেক নেই -- গার্ডের মূল কন্ডিশনই " +
                "বদলে গেছে সম্ভবত।",
            text.contains("if (!isOnline.value)")
        )
        assertTrue(
            "`requireOnlineOrWarn()`-এ অফলাইন-শাখায় আর `showToast(message)` কল নেই -- ইউজার আর " +
                "কোনো ইঙ্গিত ছাড়াই silently blocked হবে।",
            text.contains("showToast(message)")
        )
        assertTrue(
            "`requireOnlineOrWarn()`-এ অফলাইন-শাখায় আর `return false` নেই -- caller-রা ভুল করে " +
                "true পেয়ে ব্লকড action চালিয়ে যেতে পারে।",
            Regex("""return\s+false""").containsMatchIn(text)
        )
        assertTrue(
            "`requireOnlineOrWarn()`-এর শেষে আর unconditional `return true` নেই -- online অবস্থায় " +
                "action ভুলভাবে ব্লক হতে পারে।",
            Regex("""return\s+true""").containsMatchIn(text)
        )
    }

    // ------------------------------------------------------------------
    // ২. গার্ডেড ৯৫টা ফাংশন (Step 16.1-এর ৯৬টা, `loginAsAdmin` বাদে -- উপরের KDoc সংশোধন ১ দ্রষ্টব্য)
    //    -- প্রতিটার প্রতিটা occurrence-এ সরাসরি `requireOnlineOrWarn(` কল আছে কিনা।
    //    `solverCancelAcceptedJob` এখানে বাদ (৩-নং টেস্টে আলাদাভাবে, ওভারলোড-ডেলিগেশন লজিক-সহ)।
    // ------------------------------------------------------------------

    /** ফাংশন নাম -> প্রত্যাশিত occurrence সংখ্যা (ডিফল্ট ১, ওভারলোড থাকলে override)। */
    private val guardedFunctionsExpectedOccurrences: Map<String, Int> = buildMap {
        val singleOccurrence = listOf(
            // Auth/session (loginAsAdmin বাদ, সংশোধন ১ দ্রষ্টব্য)
            "validateLoginCredentials", "register", "sendOtp", "switchRoleToSolver", "switchRoleToUser",
            // প্রোফাইল/প্রেফারেন্স
            "updateUser", "updatePhoneNumber", "updateProfile", "updateProfileImage",
            "updateSolverSkills", "toggleFavoriteSolver", "addFavoriteSolver",
            // Wallet/Payment
            "requestWithdrawal", "depositMoneyViaGateway",
            // সমস্যা পোস্টিং
            "createProblem", "createInstantJob", "userDeleteProblem",
            // Bidding/Instant-job lifecycle (solverCancelJob বাদে -- নিচে ২ occurrence)
            "acceptInstantJobBid", "cancelInstantJob", "clearSolverCancelledNotice", "placeBid",
            "acceptBid", "withdrawBid", "ownerResetOrphanedAcceptedBid",
            "requestJobRelease", "cancelJobReleaseRequest", "rejectJobReleaseRequest", "confirmReleaseAndComplete",
            // Dispute
            "raiseDispute", "withdrawDispute", "settleDispute", "requestAdminAssistance",
            "adminResolveDispute", "adminManuallyFlagDispute",
            // Extra charge
            "requestExtraAmount", "userConfirmExtraAmountPaid", "userRejectExtraAmount",
            "requestAdditionalCharge", "respondToAdditionalCharge",
            // Chat/Messages
            "retryFailedMessage", "adminSendMessageToProblemChat", "adminDeleteMessage",
            // Ratings
            "submitSolverRatingForUser", "submitUserRatingForSolver", "adminDeleteRating",
            // Direct contract
            "createDirectContractProject", "acceptDirectContractProposal", "declineDirectContractProposal",
            "adminUpdateDirectContractStatus", "adminCancelAndRefundDirectContract",
            // KYC (submitKyc বাদে -- নিচে ৩ occurrence)
            "adminUpdateKycInfo", "adminResetKycToPending",
            // Admin — ইউজার/সমস্যা
            "adminDeleteProblem", "adminDeleteUser", "adminAdjustBalance", "adminResetUserPassword",
            "adminAdjustReputation", "adminUpdateProblemStatus", "adminUpdateProblemBudget",
            "adminReassignSolver", "adminRejectBid", "reportAbuse",
            // Admin — ক্যাটাগরি/FAQ/সেটিংস
            "adminAddCategory", "adminUpdateCategory", "adminSetPhysicalWorkEnabled",
            "adminSetVirtualWorkEnabled", "adminToggleCategoryActive", "adminDeleteCategory",
            "adminAddFaq", "adminUpdateFaq", "adminDeleteFaq", "adminUpdatePlatformSetting",
            "adminBatchUpdatePlatformSettings", "adminUpdateCredentials", "adminFactoryResetAllData",
            "adminToggleCategoryInstantJob",
            // Admin — নোটিফিকেশন
            "adminSendManualNotification", "adminCancelScheduledNotification", "adminDeleteManualNotification",
            // Admin — রেপুটেশন-ইভেন্ট
            "adminSaveCustomReputationEvent", "adminDeleteCustomReputationEvent",
            "adminToggleCustomReputationEventStatus",
            // Admin — ফ্রি-কোটা
            "adminRunMonthlyFreeQuotaReset", "adminResetSolverFreeQuota", "adminResetSolverMissCycle",
            // Admin — escrow/balance integrity (dryRun-conditional দুটোও এখানেই, নিচের টেস্ট ৪-এ আলাদা নোট)
            "adminReleaseEscrow", "adminRefundEscrow", "adminCleanupDuplicateRefunds",
            "adminRepairMissingRefunds", "adminReconcileBalances",
            "adminCleanupCorruptedCommissionRates", "adminForceCancelInstantJob"
        )
        singleOccurrence.forEach { put(it, 1) }
        put("solverCancelJob", 2)
        put("submitKyc", 3)
    }

    @Test
    fun `all 95 domain-listed guarded functions still call requireOnlineOrWarn at every overload`() {
        val failures = mutableListOf<String>()

        for ((funName, expectedCount) in guardedFunctionsExpectedOccurrences) {
            val regions = functionRegions(funName)
            if (regions.isEmpty()) {
                failures += "`$funName` -- SomadhanViewModel.kt-এ পাওয়াই যায়নি (rename/সরানো হয়েছে সম্ভবত)।"
                continue
            }
            if (regions.size != expectedCount) {
                failures += "`$funName` -- প্রত্যাশিত $expectedCount টা overload, পাওয়া গেছে ${regions.size} টা " +
                    "(লাইন: ${regions.map { vmLines.indexOf(vmLines[it.first]) + 1 }})। ওভারলোড " +
                    "যোগ/বাদ হয়েছে, guardedFunctionsExpectedOccurrences ম্যাপ আপডেট করো।"
                continue
            }
            regions.forEachIndexed { idx, region ->
                if (!bodyCallsGuard(region)) {
                    failures += "`$funName` (overload ${idx + 1}/${regions.size}, লাইন ${region.first + 1}) -- " +
                        "`requireOnlineOrWarn(` কল আর পাওয়া যাচ্ছে না। এই ফাংশন Step 16.1 ইনভেন্টরিতে " +
                        "গার্ডেড হিসেবে তালিকাভুক্ত -- গার্ড সরানো হয়ে থাকলে flag আর actual gating আচরণ আর " +
                        "sync নেই (এই টেস্টের মূল assertion)।"
                }
            }
        }

        assertTrue(
            "নিচের গার্ডেড ফাংশন(গুলো)-এ আর সরাসরি requireOnlineOrWarn() গার্ড পাওয়া যাচ্ছে না " +
                "(flag/actual-gating sync ভেঙে গেছে):\n" + failures.joinToString("\n") { "  - $it" },
            failures.isEmpty()
        )
    }

    // ------------------------------------------------------------------
    // ৩. `solverCancelAcceptedJob` — দুটো ওভারলোড, ডেলিগেশন-প্যাটার্ন (উপরের KDoc সংশোধন ৪)।
    //    overload ১ (problem, bid, ...) সরাসরি গার্ডেড; overload ২ (problem, reason, ...)
    //    সরাসরি গার্ডেড না, কিন্তু গার্ডেড `solverCancelJob(...)`-কেই ভেতরে কল করে -- তাই
    //    পরোক্ষভাবে সুরক্ষিত, এটাই এই টেস্টের assertion (সরাসরি গার্ড-অভাবকে ভুলবশত bug ধরার বদলে)।
    // ------------------------------------------------------------------

    @Test
    fun `solverCancelAcceptedJob overload 1 guards directly, overload 2 delegates to guarded solverCancelJob`() {
        val regions = functionRegions("solverCancelAcceptedJob")
        assertTrue(
            "`solverCancelAcceptedJob`-এর প্রত্যাশিত ২টা ওভারলোড পাওয়া যায়নি (পাওয়া গেছে ${regions.size}টা) -- " +
                "ওভারলোড যোগ/বাদ হয়েছে সম্ভবত, এই টেস্ট আপডেট করো।",
            regions.size == 2
        )
        val (bidOverload, reasonOverload) = regions

        assertTrue(
            "`solverCancelAcceptedJob(problem, bid, ...)` (১ম ওভারলোড, লাইন ${bidOverload.first + 1}) -- আর " +
                "সরাসরি `requireOnlineOrWarn(` কল করছে না।",
            bodyCallsGuard(bidOverload)
        )

        val reasonOverloadText = regionText(reasonOverload)
        assertFalse(
            "`solverCancelAcceptedJob(problem, reason, ...)` (২য় ওভারলোড, লাইন ${reasonOverload.first + 1}) -- " +
                "এখন সরাসরি `requireOnlineOrWarn(` কল করছে (আগে করত না, ডেলিগেশনের বদলে) -- এই টেস্টের " +
                "ধরে নেওয়া প্যাটার্ন বদলেছে, আপডেট করো (এটা ব্যর্থতা না, শুধু ধরে নেওয়া প্যাটার্নের সাথে না " +
                "মেলা)।",
            bodyCallsGuard(reasonOverload)
        )
        assertTrue(
            "`solverCancelAcceptedJob(problem, reason, ...)` (২য় ওভারলোড) -- আর ভেতরে `solverCancelJob(` " +
                "কল করছে না (ডেলিগেশন সরানো হয়েছে সম্ভবত) -- এখন এই ওভারলোডের কোনো offline-guard-ই নেই, " +
                "flag/actual-gating sync ভেঙে গেছে।",
            Regex("""solverCancelJob\(""").containsMatchIn(reasonOverloadText)
        )
    }

    // ------------------------------------------------------------------
    // ৪. dry-run exception — `adminRepairMissingRefunds`/`adminReconcileBalances`-এ গার্ড
    //    `if (!dryRun && !requireOnlineOrWarn(...))`-এর মতো conditional (dry-run মোডে অফলাইনেও
    //    চলে, শুধু আসল write-মোডে গার্ড কার্যকর) -- Step 16.1 টেবিল ৩-এর ⚠️ নোট অনুযায়ী।
    // ------------------------------------------------------------------

    @Test
    fun `adminRepairMissingRefunds and adminReconcileBalances keep the documented dryRun-conditional guard`() {
        for (funName in listOf("adminRepairMissingRefunds", "adminReconcileBalances")) {
            val region = functionRegions(funName).single()
            val text = regionText(region)
            assertTrue(
                "`$funName`-এ আর `!dryRun &&` conditional গার্ড-প্যাটার্ন নেই -- এটা এখন dry-run মোডেও " +
                    "অফলাইন-ব্লকড হয়ে যেতে পারে (read-only analysis-এর জন্য regression), অথবা write-মোডেও " +
                    "আনগার্ডেড হয়ে গেছে -- দুটোই Step 16.1 টেবিল ৩-এর ডকুমেন্টেড exception ভাঙে।",
                Regex("""!\s*dryRun\s*&&\s*!requireOnlineOrWarn\(""").containsMatchIn(text)
            )
        }
    }

    // ------------------------------------------------------------------
    // ৫. ইচ্ছাকৃতভাবে গার্ডবিহীন ১১টা ViewModel ফাংশন (Step 16.1 টেবিল ৪, সংশোধিত নাম-তালিকা --
    //    উপরের KDoc সংশোধন ৩ দ্রষ্টব্য) -- drift-guard: কেউ যেন future-এ ভুলবশত এদের গার্ড না
    //    বসিয়ে দেয় স্বতন্ত্রভাবে rule ১ ভাঙার ঝুঁকি না বুঝে (এদের প্রতিটার নিজস্ব ডক-কমেন্ট আছে
    //    কেন গার্ডবিহীন থাকা ইচ্ছাকৃত)। বাকি ২টা (gateway-payment হেল্পার রেফারেন্স) ViewModel-এর
    //    না, repository-level -- এই টেস্টের স্কোপে না।
    // ------------------------------------------------------------------

    private val intentionallyUnguardedFunctions = listOf(
        "completeLoginAfterOtp", // সংশোধন ৩: টেবিল ৪-এ ভুলভাবে "validateLoginCredentials" লেখা ছিল
        "loginAsAdmin", // সংশোধন ১: টেবিল ৩ থেকে সরিয়ে এখানে আনা হলো
        "logout", // সংশোধন ৩: টেবিল ৪-এ ভুলভাবে "loginAsAdmin" লেখা ছিল
        "markSolverOnWay",
        "markSolverArrived",
        "markJobStarted",
        "updateSolverLiveLocation",
        "markDisputeResultSeen",
        "markCompletionResultSeen",
        "markProblemSeen",
        "triggerInstantJobExpiryCheck"
    )

    @Test
    fun `intentionally-unguarded functions still have no direct requireOnlineOrWarn call`() {
        val failures = mutableListOf<String>()
        for (funName in intentionallyUnguardedFunctions) {
            val regions = functionRegions(funName)
            if (regions.isEmpty()) {
                failures += "`$funName` -- SomadhanViewModel.kt-এ পাওয়াই যায়নি (rename/সরানো হয়েছে সম্ভবত)।"
                continue
            }
            regions.forEachIndexed { idx, region ->
                if (bodyCallsGuard(region)) {
                    failures += "`$funName` (overload ${idx + 1}/${regions.size}) -- এখন `requireOnlineOrWarn(` " +
                        "কল করছে, যদিও এটা rule ১ রক্ষার জন্য ইচ্ছাকৃতভাবে গার্ডবিহীন রাখা হয়েছিল (নিজস্ব " +
                        "ডক-কমেন্ট দ্রষ্টব্য) -- গার্ড যোগ হয়ে থাকলে আগের ইচ্ছাকৃত exception ভেঙে থাকতে পারে, " +
                        "অথবা এই টেস্টের তালিকা stale (দুটোই progress doc-এ যাচাই করা দরকার)।"
                }
            }
        }
        assertTrue(
            "ইচ্ছাকৃতভাবে গার্ডবিহীন ফাংশনগুলোর মধ্যে নিচেরগুলো এখন গার্ড কল করছে:\n" +
                failures.joinToString("\n") { "  - $it" },
            failures.isEmpty()
        )
    }

    // ------------------------------------------------------------------
    // ৬. MainActivity.kt wiring — `isStrictOfflineBlockEnabled` flag আসলেই `NoInternetOverlay`
    //    (strict, full-block) বনাম `OfflineStatusBanner` (non-strict, non-blocking)-এর মধ্যে
    //    branch করছে কিনা -- ঠিক রিপোর্ট-করা বাগের UI-লেভেল অংশ (toggle অনুযায়ী block/unblock)।
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // ৭. Somadhan Bug-Fix Step 6 — গ্রুপ ৩ (Offline-action-gating), sub-bug ৩.১b —
    //    bug-demonstrating টেস্ট (BUG_INVENTORY.md ৩.১b, CI_TEST_SUITE_PROGRESS.md Step 16.2
    //    root-cause (b))।
    //
    //    `requireOnlineOrWarn()` যে `isOnline.value` (ViewModel-এর প্রাইভেট `_isOnline`
    //    StateFlow) পড়ে গার্ড করে, সেই `_isOnline` `startConnectivityObserver()`-এ সেট হয় —
    //    কিন্তু এখনো (শুরুর মান, `onAvailable()`, `onLost()` তিনটা জায়গাতেই) শুধু
    //    `NET_CAPABILITY_INTERNET` চেক করে, `NET_CAPABILITY_VALIDATED` কখনো চেক করে না।
    //    `NET_CAPABILITY_INTERNET` মানে নেটওয়ার্ক *দাবি করে* তার ইন্টারনেট আছে (declared),
    //    `NET_CAPABILITY_VALIDATED` মানে *সত্যিই যাচাই করা হয়েছে* (captive-portal/actual
    //    reachability চেক) — তুলনায় `NetworkConnectivityObserver.kt`-এর ইতিমধ্যে-ফিক্সড
    //    `isCurrentlyConnected()` দুটোই চেক করে (লাইন ৪৩)।
    //
    //    Step 6 sub-step ২-এ `startConnectivityObserver()`-এ `NET_CAPABILITY_VALIDATED` চেক
    //    (AND-সহ, শুধু `NET_CAPABILITY_INTERNET` না) যোগ হওয়ার পর এখন এই টেস্ট **pass করার কথা**
    //    (নাম থেকে "currently failing" অংশ Step 2/3-এর precedent অনুযায়ী রিনেম করা হয়েছে, যেহেতু
    //    আর সত্যি না)।
    // ------------------------------------------------------------------

    @Test
    fun `startConnectivityObserver checks NET_CAPABILITY_VALIDATED before marking isOnline true (BUG 3_1b fixed)`() {
        val region = functionRegions("startConnectivityObserver").single()
        val text = regionText(region)
        assertTrue(
            "`startConnectivityObserver()`-এ এখনো `NET_CAPABILITY_VALIDATED` চেক নেই — শুধু " +
                "`NET_CAPABILITY_INTERNET` চেক করে (শুরুর মান/`onAvailable()`/`onLost()` তিনটাতেই), " +
                "যা captive-portal/\"wifi আছে কিন্তু ইন্টারনেট নেই\" নেটওয়ার্কে false-positive " +
                "`_isOnline=true` তৈরি করতে পারে — admin এমন নেটওয়ার্কে টগল/অ্যাকশন \"সফল\" দেখতে " +
                "পারেন যদিও আসল Supabase কল fail করবে (BUG_INVENTORY.md ৩.১b, " +
                "CI_TEST_SUITE_PROGRESS.md Step 16.2 root-cause (b))।",
            text.contains("NET_CAPABILITY_VALIDATED")
        )
    }

    @Test
    fun `MainActivity still branches NoInternetOverlay vs OfflineStatusBanner on isStrictOfflineBlockEnabled`() {
        val ifIdx = mainActivityText.indexOf("if (isStrictOfflineBlockEnabled)")
        assertTrue(
            "MainActivity.kt-এ আর `if (isStrictOfflineBlockEnabled)` ব্রাঞ্চ-পয়েন্ট নেই -- Strict/non-strict " +
                "মোডের UI-লেভেল branching সরানো/rename হয়েছে সম্ভবত।",
            ifIdx >= 0
        )
        // if-ব্লকের পরের ~300 ক্যারেক্টারের মধ্যেই NoInternetOverlay( কল থাকার কথা (strict শাখা),
        // আর তার পরে else শাখায় OfflineStatusBanner( -- দুটোই একই if/else statement-এর অংশ কিনা
        // সেটা নিখুঁতভাবে brace-parse না করে, কাছাকাছি windowed-proximity দিয়েই যথেষ্ট signal পাওয়া
        // যায় (এই ফাইলে অন্য কোথাও এই দুটো কম্পোনেন্ট কল হয় না, `NoInternetScreen.kt`-এর ইনভেন্টরি
        // এন্ট্রি দ্রষ্টব্য)।
        val windowAfterIf = mainActivityText.substring(ifIdx, minOf(mainActivityText.length, ifIdx + 400))
        assertTrue(
            "`if (isStrictOfflineBlockEnabled)`-এর পরের ব্লকে (৪০০ ক্যারেক্টারের মধ্যে) আর `NoInternetOverlay(` " +
                "কল পাওয়া যাচ্ছে না -- strict-মোড full-block আচরণ ভেঙে গেছে সম্ভবত।",
            windowAfterIf.contains("NoInternetOverlay(")
        )

        val elseIdx = mainActivityText.indexOf("} else {", ifIdx)
        assertTrue(
            "`if (isStrictOfflineBlockEnabled) { ... }`-এর পরে আর `} else {` পাওয়া যাচ্ছে না -- non-strict " +
                "শাখাই সরানো হয়েছে সম্ভবত (তাহলে toggle OFF করলেও app সবসময় strict-ব্লক থেকে যাবে, ঠিক " +
                "মূল রিপোর্ট-করা বাগ)।",
            elseIdx > ifIdx
        )
        val windowAfterElse = mainActivityText.substring(elseIdx, minOf(mainActivityText.length, elseIdx + 300))
        assertTrue(
            "else শাখায় (৩০০ ক্যারেক্টারের মধ্যে) আর `OfflineStatusBanner(` কল পাওয়া যাচ্ছে না -- non-strict " +
                "মোডের non-blocking UI সরানো হয়েছে সম্ভবত।",
            windowAfterElse.contains("OfflineStatusBanner(")
        )
    }
}
