package com.example.repository

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Step 12 (PART 2) — CI_TEST_SUITE_MASTER_PROMPT.md sub-task ৩।
 *
 * ============================================================================
 * ⚠️ কেন এটা mock-ভিত্তিক (SupabaseSyncManager fail করিয়ে repository ফাংশন কল করে,
 * outbox-এ entry পড়েছে কিনা assert করে) রানটাইম টেস্ট না -- static structural
 * verification কেন, তার সম্পূর্ণ কারণ:
 * ============================================================================
 * এই সেশনে যাচাই করা গেছে (`CI_TEST_SUITE_PROGRESS.md`-এ বিস্তারিত):
 *  ১. `SupabaseSyncManager` আর `SupabaseAuthManager` দুটোই Kotlin `object` (singleton,
 *     `object SupabaseSyncManager { ... }`) -- কোনো interface/DI seam নেই যা দিয়ে
 *     test-double সাবস্টিটিউট করা যায়।
 *  ২. `mockk` (বা সমতুল্য কোনো object-mocking library) `app/build.gradle.kts`-এ নেই,
 *     আর master-prompt-এর rule #১ অনুযায়ী `build.gradle.kts` কখনো এডিট করা যাবে না --
 *     তাই `mockkObject(SupabaseSyncManager)` ব্যবহারের কোনো উপায় এই সেশনে নেই।
 *  ৩. `SupabaseAuthManager.currentUserId()` বাস্তব Supabase Auth SDK-এর in-memory
 *     সেশন-স্টেট (`client.auth.currentUserOrNull()?.id`) থেকে সরাসরি আসে -- এটা
 *     সেট করার কোনো টেস্ট-হুক এক্সপোজড না। MONEY-CRITICAL সাইটগুলোর প্রায় সবগুলোই
 *     `if (SupabaseAuthManager.currentUserId() == ...)`-জাতীয় guard-এর ভেতরে, তাই
 *     mock ছাড়া সেই guard পার হয়ে আসল dual-write কল পর্যন্ত end-to-end পৌঁছানো এই
 *     আর্কিটেকচারে সম্ভব না।
 * ফলাফল: pure JVM/Robolectric টেস্ট থেকে `SomadhanRepository`-এর এই ফাংশনগুলো সত্যিই
 * ইনভোক করে network-fail সিমুলেট করা এই সেশনের কনস্ট্রেইন্টে করা যায়নি -- ভুল/মিথ্যা
 * "pass" দেখানো একটা ভাঙা টেস্টের চেয়ে, এই সীমাবদ্ধতা মেনে একটা টেস্ট লেখা হলো যেটা
 * বাস্তব gap-টাই সরাসরি ধরে।
 *
 * ============================================================================
 * এই টেস্ট কী করে
 * ============================================================================
 * PART 1 (আগের সেশন)-এ ঠিক এই একই brace-matching পদ্ধতি (bug-ফিক্সের পরের ভার্সন --
 * `.onFailure`-এর ঠিক পরের `{` থেকে brace-count শুরু) দিয়ে `SomadhanRepository.kt`-এর
 * প্রতিটা `.onFailure {` সাইট স্ক্যান করে ১৪৭টার মধ্যে ১৩৩টা "unprotected"
 * (`enqueueOutboxRetry(` নেই) হিসেবে চিহ্নিত হয়েছিল, আর তার মধ্যে ২৬টা physical call-site
 * MONEY-CRITICAL হিসেবে classify হয়েছিল। (PART 1-এ "২৪টা distinct function, দুটো ফাংশনে ডাবল সাইট"
 * লেখা ছিল; Step 12.2-এ সাইট-টেবিল বানাতে গিয়ে স্ট্যাটিকভাবে দেখা গেছে আসলে ২৫টা distinct ফাংশন --
 * ডাবল সাইট শুধু `trackExtraPaymentMissCycle`-এ; `adminUpdateDirectContractStatus` ও
 * `adminCancelAndRefundDirectContract` আলাদা দুটো ফাংশন, নিচের নামকরণ-নোট দেখো।)
 * এই টেস্ট ঠিক সেই ২৬টা সাইটের প্রতিটার `.onFailure { ... }` ব্লক সরাসরি
 * `SomadhanRepository.kt` থেকে পড়ে (Step 12.2 থেকে সাইট খোঁজা হয় ফাংশনের নাম + ভেতরের
 * `SupabaseSyncManager.<কল>(` দিয়ে -- হার্ডকোড লাইন নাম্বার দিয়ে না; নিচের টেবিল দেখো), আর
 * assert করে যে ব্লকটাতে `enqueueOutboxRetry(` কল আছে কিনা।
 *
 * **বর্তমানে এই ২৬টা সাইটের একটাও protected না -- তাই এই ২৬টা টেস্টের প্রতিটাই
 * এখন FAIL করার কথা।** এটাই আসল বাগ প্রমাণ করে (master-prompt sub-task ৩-এর
 * প্রত্যাশিত প্রথম-রান আচরণ -- ঠিক mock-ভিত্তিক টেস্টের ক্ষেত্রেও যা আশা করা হতো)।
 * পরের কোনো সেশনে যখন কোনো নির্দিষ্ট সাইটের `.onFailure {}`-এ `enqueueOutboxRetry(...)`
 * যোগ হবে, সেই একটা টেস্ট pass করবে -- বাকিগুলো এখনো fail থেকে বাকি কাজের রিমাইন্ডার
 * হিসেবে কাজ করবে, কোনো silent skip ছাড়াই।
 *
 * ============================================================================
 * Step 12.2 -- লাইন-নাম্বার-নির্ভরতা থেকে মুক্তি
 * ============================================================================
 * আগে প্রতিটা টেস্ট `SomadhanRepository.kt`-এর হার্ডকোড লাইন নাম্বার (2829, 2952, ...) দিয়ে সাইট
 * খুঁজত -- পরের ধাপগুলোতে (12.3 ...) কোনো ফাংশনে `enqueueOutboxRetry(...)` যোগ হলে ফাইলের লাইন
 * সরে যেত ও বাকি সব টেস্ট ভুল কারণে ভাঙত। এখন প্রতিটা সাইট খোঁজা হয়:
 *   (১) `SomadhanRepository`-এর member-level `fun <funName>` খুঁজে (৪-স্পেস ইন্ডেন্ট; পরের member-level
 *       `fun`-এর আগ পর্যন্ত সেই ফাংশনের সীমা),
 *   (২) তার ভেতরে `occurrence`-তম (১-ভিত্তিক) `SupabaseSyncManager.<rpcCall>(` কল -- কমেন্ট লাইন
 *       (`//` দিয়ে শুরু, বা ব্লক-কমেন্টের `*` লাইন) গোনা হয় না; কারণ কিছু ফাংশনের কমেন্টেই কলের নাম লেখা আছে
 *       (যেমন `reconcileEscrowStates`-এর কমেন্টে `SupabaseSyncManager.adminReconcileEscrowStates()`),
 *   (৩) ওই কল থেকে (কলের লাইন সহ) পরের প্রথম non-comment `.onFailure`।
 * brace-matching (`onFailureBlockAt`) আগের সংস্করণ থেকে অপরিবর্তিত। ফাংশন rename/সরানো হলে বা কল না
 * পাওয়া গেলে টেস্ট স্পষ্ট বার্তাসহ error দেয় -- নীরবে ভুল ব্লক চেক করে না।
 *
 * সাইট টেবিল (ফাংশন | কল | কত নম্বর কল / ফাংশনে মোট non-comment কল | PART 1-এর পুরনো লাইন, শুধু ইতিহাস):
 *  acceptBid                           | acceptBid                         | 1/1   | 2829
 *  requestJobRelease                   | requestJobRelease                 | 1/1   | 2952
 *  cancelJobReleaseRequest             | cancelJobReleaseRequest           | 1/1   | 3052
 *  rejectJobReleaseRequest             | rejectJobReleaseRequest           | 1/1   | 3131
 *  withdrawDispute                     | withdrawDispute                   | 1/1   | 3417
 *  settleDispute                       | settleDispute                     | 1/1   | 3483
 *  adminResolveDisputeLocked           | resolveDisputeSplit               | 1/1   | 3943
 *  reconcileEscrowStates               | adminReconcileEscrowStates        | 1/1   | 4584
 *  reconcileUserBalances               | adminReconcileUserBalances        | 1/1   | 4788
 *  cleanupDuplicateRefunds             | adminCleanupDuplicateRefunds      | 1/1   | 5006
 *  repairMissingRefunds                | adminRepairMissingRefunds         | 1/1   | 5217
 *  solverCancelJob                     | solverCancelJob                   | 1/1   | 5405
 *  confirmReleaseAndComplete           | markAdditionalChargeSettled       | 1/1   | 5843
 *  adminUpdateWithdrawalTrxId          | adminUpdateWithdrawalTrxId        | 1/1   | 7296
 *  depositMoneyViaGateway              | requestWalletDeposit              | 1/1   | 7466
 *  adminRefundEscrow                   | adminRefundAndReopenProblem       | 1/1   | 8544
 *  requestAdditionalCharge             | requestAdditionalCharge           | 1/1   | 8677
 *  respondToAdditionalCharge           | respondToAdditionalCharge         | 1/1   | 8754
 *  trackExtraPaymentMissCycle          | systemTrackExtraPaymentMiss       | 1/2   | 9082
 *  trackExtraPaymentMissCycle          | systemTrackExtraPaymentMiss       | 2/2   | 9110
 *  adminUpdateDirectContractStatus     | adminUpdateDirectContractStatus   | 1/1   | 9560
 *  adminCancelAndRefundDirectContract  | adminUpdateDirectContractStatus   | 1/1   | 9597
 *  requestExtraAmount                  | requestExtraAmount                | 1/1   | 10313
 *  userConfirmExtraAmount              | userConfirmExtraAmount            | 1/1   | 10423
 *  userRejectExtraAmount               | userRejectExtraAmount             | 1/1   | 10474
 *  cleanupCorruptedCommissionRates     | adminUpdateProblemCommissionRate  | 1/1   | 10494
 *
 * ⚠️ নামকরণ-নোট: পুরনো লাইন 9560-এর সাইটটা আসলে `fun adminUpdateDirectContractStatus`-এর নিজের ভেতরে
 * (`adminCancelAndRefundDirectContract`-এর ভেতরে না) -- PART 1-এ এটাকে "adminCancelAndRefundDirectContract
 * call-site 1" নামে গোনা হয়েছিল। টেস্টের নাম/বর্ণনা (ও তাই ব্যর্থতার বার্তা) অপরিবর্তিত রাখা হয়েছে; শুধু
 * খোঁজা হয় আসল ফাংশনে। নাম ঠিক করা Step 12.7-এর সময় সিদ্ধান্ত।
 */
class DualWriteGapTest {

    /**
     * Gradle-এর ডিফল্ট JVM test working directory মডিউল-রুট (`app/`) হওয়ার কথা, কিন্তু
     * ভিন্ন invocation context থেকে চালানো হলেও কাজ করার জন্য দুটো candidate path চেক করা
     * হলো।
     */
    private val repoFile: File by lazy {
        val candidates = listOf(
            File("src/main/java/com/example/data/repository/SomadhanRepository.kt"),
            File("app/src/main/java/com/example/data/repository/SomadhanRepository.kt")
        )
        candidates.firstOrNull { it.exists() }
            ?: error(
                "SomadhanRepository.kt পাওয়া যায়নি কোনো candidate path-এ " +
                    "(cwd=${File(".").absolutePath}) -- Gradle-এর test working directory " +
                    "বদলেছে সম্ভবত, DualWriteGapTest-এর candidate paths আপডেট করো।"
            )
    }

    private val repoLines: List<String> by lazy { repoFile.readText().lines() }

    // ------------------------------------------------------------------
    // Step 12.2: সাইট খোঁজা -- ফাংশনের নাম + `SupabaseSyncManager.<rpcCall>(` কল দিয়ে
    // (হার্ডকোড লাইন নাম্বার নেই)। DualWriteGapTestBorderline-এর একই পদ্ধতি।
    // ------------------------------------------------------------------

    private val memberFunPrefix = "^ {4}(?:(?:private|internal|public|suspend|override|inline)\\s+)*fun\\s+"

    private fun isCommentLine(line: String): Boolean {
        val t = line.trimStart()
        return t.startsWith("//") || t.startsWith("/*") || t.startsWith("*")
    }

    /** `fun <funName>` শুরু হওয়া লাইন থেকে পরের member-level `fun`-এর আগের লাইন পর্যন্ত (0-indexed)। */
    private fun functionRegion(funName: String): IntRange {
        val startRegex = Regex(memberFunPrefix + Regex.escape(funName) + "\\b")
        val startIdx = repoLines.indexOfFirst { startRegex.containsMatchIn(it) }
        require(startIdx >= 0) {
            "SomadhanRepository.kt-এ `fun $funName` (member-level) পাওয়া যায়নি -- ফাংশন rename/সরানো " +
                "হয়েছে সম্ভবত, DualWriteGapTest-এর সাইট-টেবিল আপডেট করো।"
        }
        val nextFunRegex = Regex(memberFunPrefix)
        var endIdx = repoLines.size - 1
        for (i in (startIdx + 1) until repoLines.size) {
            if (nextFunRegex.containsMatchIn(repoLines[i])) {
                endIdx = i - 1
                break
            }
        }
        return startIdx..endIdx
    }

    /**
     * `funName`-এর ভেতরে `occurrence`-তম (1-based) non-comment `SupabaseSyncManager.<rpcCall>(`
     * কলের পরের (কলের লাইন সহ) প্রথম non-comment `.onFailure`-এর বর্তমান 1-based লাইন নাম্বার।
     */
    private fun onFailureLineAfterCall(funName: String, rpcCall: String, occurrence: Int): Int {
        val region = functionRegion(funName)
        val needle = "SupabaseSyncManager.$rpcCall("
        val callIdxs = region.filter { repoLines[it].contains(needle) && !isCommentLine(repoLines[it]) }
        require(callIdxs.size >= occurrence) {
            "`$funName`-এর ভেতরে `$needle` কল $occurrence-তম বার পাওয়া যায়নি (পাওয়া গেছে ${callIdxs.size}টা) -- " +
                "কল rename/সরানো হয়েছে সম্ভবত, DualWriteGapTest-এর সাইট-টেবিল আপডেট করো।"
        }
        val callIdx = callIdxs[occurrence - 1]

        var failIdx = -1
        for (i in callIdx..region.last) {
            if (repoLines[i].contains(".onFailure") && !isCommentLine(repoLines[i])) {
                failIdx = i
                break
            }
        }
        require(failIdx >= 0) {
            "`$funName`-এর `$needle` ($occurrence-তম) কলের পরে `.onFailure` পাওয়া যায়নি -- " +
                "কলের ধরন বদলেছে সম্ভবত, DualWriteGapTest-এর সাইট-খোঁজা আপডেট করো।"
        }
        return failIdx + 1
    }

    /**
     * `exactLine` (1-indexed)-এর `.onFailure {` থেকে brace-matching করে সম্পূর্ণ `.onFailure { ... }`
     * ব্লকের টেক্সট রিটার্ন করে (PART 1-এর ফিক্সড brace-matching পদ্ধতি অনুযায়ী: `.onFailure`-এর ঠিক
     * পরের `{` থেকে count শুরু, যাতে আগের `.onSuccess {}`-এর ক্লোজিং brace-এর সাথে গুলিয়ে না যায়)।
     * `exactLine` এখন `onFailureLineAfterCall` থেকে আসে (Step 12.2) -- brace-matching লজিক অপরিবর্তিত।
     */
    private fun onFailureBlockAt(exactLine: Int): String {
        require(exactLine in 1..repoLines.size) {
            "Line $exactLine, ফাইলে মোট ${repoLines.size} লাইন আছে -- সাইট-খোঁজা (onFailureLineAfterCall) " +
                "অবৈধ লাইন দিয়েছে।"
        }
        val lineText = repoLines[exactLine - 1]
        val onFailureIdx = lineText.indexOf(".onFailure")
        require(onFailureIdx >= 0) {
            "Line $exactLine-এ `.onFailure` নেই (পাওয়া গেছে: \"$lineText\") -- সাইট-খোঁজা " +
                "(onFailureLineAfterCall) ভুল লাইন দিয়েছে।"
        }
        val braceIdx = lineText.indexOf('{', onFailureIdx)
        require(braceIdx >= 0) {
            "Line $exactLine-এ `.onFailure`-এর পরে `{` পাওয়া যায়নি (পাওয়া গেছে: \"$lineText\")।"
        }

        var depth = 0
        var started = false
        val sb = StringBuilder()
        outer@ for (i in exactLine..repoLines.size) {
            val line = repoLines[i - 1]
            val chars = if (i == exactLine) line.substring(braceIdx) else line
            for (c in chars) {
                if (c == '{') {
                    depth++
                    started = true
                }
                if (c == '}') {
                    depth--
                }
                sb.append(c)
                if (started && depth == 0) break@outer
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    private fun assertSiteIsOutboxProtected(
        funName: String,
        rpcCall: String,
        description: String,
        occurrence: Int = 1
    ) {
        val exactLine = onFailureLineAfterCall(funName, rpcCall, occurrence)
        val block = onFailureBlockAt(exactLine)
        assertTrue(
            "MONEY-CRITICAL dual-write সাইট `$description` (SomadhanRepository.kt:$exactLine) " +
                "এখনো UNPROTECTED: এর .onFailure {} ব্লকে enqueueOutboxRetry(...) কল নেই -- এখানে " +
                "Supabase RPC fail করলে local balance/escrow/wallet/dispute state চিরস্থায়ীভাবে " +
                "cloud থেকে out-of-sync থেকে যাবে, কোনো retry ছাড়াই। বিস্তারিত: " +
                "CI_TEST_SUITE_PROGRESS.md-এর Step 12 MONEY-CRITICAL টেবিল।",
            block.contains("enqueueOutboxRetry(")
        )
    }

    // ------------------------------------------------------------------
    // MONEY-CRITICAL unprotected সাইট (২৫টা distinct function -- PART 1-এ ভুলে ২৪ লেখা ছিল --
    // ২৬টা physical call-site)। প্রতিটা এই মুহূর্তে fail করার কথা।
    // ------------------------------------------------------------------

    @Test
    fun `acceptBid (accept_bid RPC, escrow deduction) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "acceptBid",
            rpcCall = "acceptBid",
            description = "acceptBid -> accept_bid"
        )

    @Test
    fun `requestJobRelease is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "requestJobRelease",
            rpcCall = "requestJobRelease",
            description = "requestJobRelease"
        )

    @Test
    fun `cancelJobReleaseRequest is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "cancelJobReleaseRequest",
            rpcCall = "cancelJobReleaseRequest",
            description = "cancelJobReleaseRequest"
        )

    @Test
    fun `rejectJobReleaseRequest is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "rejectJobReleaseRequest",
            rpcCall = "rejectJobReleaseRequest",
            description = "rejectJobReleaseRequest"
        )

    @Test
    fun `withdrawDispute is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "withdrawDispute",
            rpcCall = "withdrawDispute",
            description = "withdrawDispute"
        )

    @Test
    fun `settleDispute is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "settleDispute",
            rpcCall = "settleDispute",
            description = "settleDispute"
        )

    @Test
    fun `adminResolveDisputeLocked (resolveDisputeSplit, escrow split) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "adminResolveDisputeLocked",
            rpcCall = "resolveDisputeSplit",
            description = "adminResolveDisputeLocked -> resolveDisputeSplit"
        )

    @Test
    fun `reconcileEscrowStates (adminReconcileEscrowStates) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "reconcileEscrowStates",
            rpcCall = "adminReconcileEscrowStates",
            description = "reconcileEscrowStates -> adminReconcileEscrowStates"
        )

    @Test
    fun `reconcileUserBalances (adminReconcileUserBalances) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "reconcileUserBalances",
            rpcCall = "adminReconcileUserBalances",
            description = "reconcileUserBalances -> adminReconcileUserBalances"
        )

    @Test
    fun `cleanupDuplicateRefunds (adminCleanupDuplicateRefunds) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "cleanupDuplicateRefunds",
            rpcCall = "adminCleanupDuplicateRefunds",
            description = "cleanupDuplicateRefunds -> adminCleanupDuplicateRefunds"
        )

    @Test
    fun `repairMissingRefunds (adminRepairMissingRefunds) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "repairMissingRefunds",
            rpcCall = "adminRepairMissingRefunds",
            description = "repairMissingRefunds -> adminRepairMissingRefunds"
        )

    @Test
    fun `solverCancelJob (possible refund trigger) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "solverCancelJob",
            rpcCall = "solverCancelJob",
            description = "solverCancelJob"
        )

    @Test
    fun `confirmReleaseAndComplete (markAdditionalChargeSettled) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "confirmReleaseAndComplete",
            rpcCall = "markAdditionalChargeSettled",
            description = "confirmReleaseAndComplete -> markAdditionalChargeSettled"
        )

    @Test
    fun `adminUpdateWithdrawalTrxId is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "adminUpdateWithdrawalTrxId",
            rpcCall = "adminUpdateWithdrawalTrxId",
            description = "adminUpdateWithdrawalTrxId"
        )

    @Test
    fun `depositMoneyViaGateway (requestWalletDeposit) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "depositMoneyViaGateway",
            rpcCall = "requestWalletDeposit",
            description = "depositMoneyViaGateway -> requestWalletDeposit"
        )

    @Test
    fun `adminRefundEscrow (adminRefundAndReopenProblem) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "adminRefundEscrow",
            rpcCall = "adminRefundAndReopenProblem",
            description = "adminRefundEscrow -> adminRefundAndReopenProblem"
        )

    @Test
    fun `requestAdditionalCharge is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "requestAdditionalCharge",
            rpcCall = "requestAdditionalCharge",
            description = "requestAdditionalCharge"
        )

    @Test
    fun `respondToAdditionalCharge is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "respondToAdditionalCharge",
            rpcCall = "respondToAdditionalCharge",
            description = "respondToAdditionalCharge"
        )

    @Test
    fun `trackExtraPaymentMissCycle call-site 1 (systemTrackExtraPaymentMiss) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "trackExtraPaymentMissCycle",
            rpcCall = "systemTrackExtraPaymentMiss",
            occurrence = 1,
            description = "trackExtraPaymentMissCycle site 1 -> systemTrackExtraPaymentMiss"
        )

    @Test
    fun `trackExtraPaymentMissCycle call-site 2 (systemTrackExtraPaymentMiss) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "trackExtraPaymentMissCycle",
            rpcCall = "systemTrackExtraPaymentMiss",
            occurrence = 2,
            description = "trackExtraPaymentMissCycle site 2 -> systemTrackExtraPaymentMiss"
        )

    @Test
    fun `adminCancelAndRefundDirectContract call-site 1 (adminUpdateDirectContractStatus) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "adminUpdateDirectContractStatus",
            rpcCall = "adminUpdateDirectContractStatus",
            description = "adminCancelAndRefundDirectContract site 1 -> adminUpdateDirectContractStatus"
        )

    @Test
    fun `adminCancelAndRefundDirectContract call-site 2 (adminUpdateDirectContractStatus) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "adminCancelAndRefundDirectContract",
            rpcCall = "adminUpdateDirectContractStatus",
            description = "adminCancelAndRefundDirectContract site 2 -> adminUpdateDirectContractStatus"
        )

    @Test
    fun `requestExtraAmount is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "requestExtraAmount",
            rpcCall = "requestExtraAmount",
            description = "requestExtraAmount"
        )

    @Test
    fun `userConfirmExtraAmount is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "userConfirmExtraAmount",
            rpcCall = "userConfirmExtraAmount",
            description = "userConfirmExtraAmount"
        )

    @Test
    fun `userRejectExtraAmount is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "userRejectExtraAmount",
            rpcCall = "userRejectExtraAmount",
            description = "userRejectExtraAmount"
        )

    @Test
    fun `cleanupCorruptedCommissionRates (adminUpdateProblemCommissionRate) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "cleanupCorruptedCommissionRates",
            rpcCall = "adminUpdateProblemCommissionRate",
            description = "cleanupCorruptedCommissionRates -> adminUpdateProblemCommissionRate"
        )
}
