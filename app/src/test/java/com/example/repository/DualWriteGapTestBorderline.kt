package com.example.repository

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Step 12.1 — CI_TEST_SUITE_MASTER_PROMPT.md, "Borderline" সাইট যাচাইয়ের ফলাফল।
 *
 * PART 1 (Step 12)-এ ৮টা function/১০টা call-site "BORDERLINE" ছিল -- migration RPC বডি খুলে
 * যাচাই না করে classify করা যায়নি। Step 12.1-এ প্রতিটার আসল RPC বডি পড়া হয়েছে
 * (`CI_TEST_SUITE_PROGRESS.md`-এর "Step 12.1" সেকশনে লাইন-উদ্ধৃতিসহ বিস্তারিত)। তার মধ্যে
 * **শুধু একটা** MONEY-CRITICAL প্রমাণিত হয়েছে:
 *
 *  - `acceptDirectContractProposal` -> `SupabaseSyncManager.acceptDirectContract` ->
 *    `accept_direct_contract` RPC: cloud-এ `escrows` টেবিলে HELD escrow row insert করে
 *    (base_amount = accepted_amount) ও problem-কে IN_PROGRESS করে। লোকাল ফ্লো-ও একই কাজ করে
 *    (escrowDao.insert)। RPC fail করলে local-এ escrow খোলা কিন্তু cloud-এ নেই -- `acceptBid()`-এর
 *    escrow বাগের একই ধরন।
 *
 * বাকিগুলোর সিদ্ধান্ত (NON-MONEY / MONEY-ADJACENT — ব্যবহারকারীর সিদ্ধান্তের অপেক্ষায়) progress doc-এ।
 *
 * ============================================================================
 * ⚠️ DualWriteGapTest.kt থেকে পার্থক্য
 * ============================================================================
 * এই ক্লাস সাইট খোঁজে **ফাংশনের নাম + ভেতরের `SupabaseSyncManager.<rpcCall>(` কল** দিয়ে, হার্ডকোড লাইন
 * নাম্বার দিয়ে না। ফলে `SomadhanRepository.kt`-এ কোড যোগ/বাদ হয়ে লাইন সরে গেলেও টেস্ট ঠিক ব্লকেই
 * পৌঁছায় (Step 12.2 এই একই পদ্ধতি DualWriteGapTest.kt-তেও আনবে)। brace-matching লজিক
 * DualWriteGapTest.kt-এর হুবহু একই (`.onFailure`-এর ঠিক পরের `{` থেকে count শুরু)।
 *
 * **বর্তমানে এই টেস্টটা FAIL করার কথা** -- ঠিক এই fail-ই আসল বাগ প্রমাণ করে। Step 12.8-এ সাইটটায়
 * `enqueueOutboxRetry(...)` যোগ হলে pass করবে।
 */
class DualWriteGapTestBorderline {

    private val repoFile: File by lazy {
        val candidates = listOf(
            File("src/main/java/com/example/data/repository/SomadhanRepository.kt"),
            File("app/src/main/java/com/example/data/repository/SomadhanRepository.kt")
        )
        candidates.firstOrNull { it.exists() }
            ?: error(
                "SomadhanRepository.kt পাওয়া যায়নি কোনো candidate path-এ " +
                    "(cwd=${File(".").absolutePath}) -- Gradle-এর test working directory " +
                    "বদলেছে সম্ভবত, DualWriteGapTestBorderline-এর candidate paths আপডেট করো।"
            )
    }

    private val repoLines: List<String> by lazy { repoFile.readText().lines() }

    private val memberFunPrefix = "^ {4}(?:(?:private|internal|public|suspend|override|inline)\\s+)*fun\\s+"

    /** `fun <funName>` শুরু হওয়া লাইন থেকে পরের member-level `fun`-এর আগের লাইন পর্যন্ত (0-indexed)। */
    private fun functionRegion(funName: String): IntRange {
        val startRegex = Regex(memberFunPrefix + Regex.escape(funName) + "\\b")
        val startIdx = repoLines.indexOfFirst { startRegex.containsMatchIn(it) }
        require(startIdx >= 0) {
            "SomadhanRepository.kt-এ `fun $funName` (member-level) পাওয়া যায়নি -- ফাংশন rename/সরানো " +
                "হয়েছে সম্ভবত, DualWriteGapTestBorderline আপডেট করো।"
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
     * `funName`-এর ভেতরে `occurrence`-তম (1-based) `SupabaseSyncManager.<rpcCall>(` কলের পরের প্রথম
     * `.onFailure { ... }` ব্লকের সম্পূর্ণ টেক্সট ও তার (বর্তমান) 1-based লাইন নাম্বার রিটার্ন করে।
     */
    private fun onFailureBlockAfterCall(funName: String, rpcCall: String, occurrence: Int = 1): Pair<String, Int> {
        val region = functionRegion(funName)
        val needle = "SupabaseSyncManager.$rpcCall("
        val callIdxs = region.filter { repoLines[it].contains(needle) }
        require(callIdxs.size >= occurrence) {
            "`$funName`-এর ভেতরে `$needle` কল $occurrence-তম বার পাওয়া যায়নি (পাওয়া গেছে ${callIdxs.size}টা)।"
        }
        val callIdx = callIdxs[occurrence - 1]

        var failIdx = -1
        for (i in callIdx..region.last) {
            if (repoLines[i].contains(".onFailure")) {
                failIdx = i
                break
            }
        }
        require(failIdx >= 0) { "`$funName`-এর `$needle` কলের পরে `.onFailure` পাওয়া যায়নি।" }

        val failLine = repoLines[failIdx]
        val onFailureCol = failLine.indexOf(".onFailure")
        val braceCol = failLine.indexOf('{', onFailureCol)
        require(braceCol >= 0) { "লাইন ${failIdx + 1}-এ `.onFailure`-এর পরে `{` পাওয়া যায়নি: \"$failLine\"" }

        var depth = 0
        var started = false
        val sb = StringBuilder()
        outer@ for (i in failIdx until repoLines.size) {
            val chars = if (i == failIdx) repoLines[i].substring(braceCol) else repoLines[i]
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
        return sb.toString() to (failIdx + 1)
    }

    private fun assertSiteIsOutboxProtected(funName: String, rpcCall: String, description: String, occurrence: Int = 1) {
        val (block, currentLine) = onFailureBlockAfterCall(funName, rpcCall, occurrence)
        assertTrue(
            "MONEY-CRITICAL dual-write সাইট `$description` (SomadhanRepository.kt:$currentLine, `fun $funName`) " +
                "এখনো UNPROTECTED: এর .onFailure {} ব্লকে enqueueOutboxRetry(...) কল নেই -- এখানে Supabase RPC " +
                "fail করলে local escrow/problem state চিরস্থায়ীভাবে cloud থেকে out-of-sync থেকে যাবে, কোনো " +
                "retry ছাড়াই। বিস্তারিত: CI_TEST_SUITE_PROGRESS.md-এর Step 12.1 সেকশন।",
            block.contains("enqueueOutboxRetry(")
        )
    }

    @Test
    fun `acceptDirectContractProposal (accept_direct_contract RPC, opens HELD escrow) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "acceptDirectContractProposal",
            rpcCall = "acceptDirectContract",
            description = "acceptDirectContractProposal -> accept_direct_contract"
        )

    // ============================================================================
    // Step 12.8a — MONEY-ADJACENT ২টা, ব্যবহারকারীর সিদ্ধান্তে ২০২৬-০৯-২১ অন্তর্ভুক্ত হয়েছে।
    // ============================================================================

    @Test
    fun `resolveCommissionRateForNewJob site 1 (sync_solver_free_job_quota RPC, quota-bypass risk) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "resolveCommissionRateForNewJob",
            rpcCall = "syncSolverFreeJobQuota",
            description = "resolveCommissionRateForNewJob site 1 -> sync_solver_free_job_quota",
            occurrence = 1
        )

    @Test
    fun `resolveCommissionRateForNewJob site 2 (sync_solver_free_job_quota RPC, quota-bypass risk) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "resolveCommissionRateForNewJob",
            rpcCall = "syncSolverFreeJobQuota",
            description = "resolveCommissionRateForNewJob site 2 -> sync_solver_free_job_quota",
            occurrence = 2
        )

    @Test
    fun `adminManuallyFlagDispute (admin_manually_flag_dispute RPC, dispute-flag sync risk) is outbox-protected`() =
        assertSiteIsOutboxProtected(
            funName = "adminManuallyFlagDispute",
            rpcCall = "adminManuallyFlagDispute",
            description = "adminManuallyFlagDispute -> admin_manually_flag_dispute"
        )
}
