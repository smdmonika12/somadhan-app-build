package com.example.ui.viewmodel

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Step 19.6 — CI_TEST_SUITE_MASTER_PROMPT.md (Step 19 — admin panel: overview metrics
 * realtime-desync + admin action buttons execute/reflect না করা)।
 *
 * ============================================================================
 * প্রেক্ষাপট — confirmed root causes (19.2–19.5, বিস্তারিত CI_TEST_SUITE_PROGRESS.md-এ)
 * ============================================================================
 * ১. **Overview metrics bug-class** (19.2/19.3): `adminDashboardMetrics`-এর
 *    `combine()` ব্লকে headline metric-গুলোর জন্য `if (supa.totalUsers > 0) supa.X else local.X`
 *    প্যাটার্ন একটা **স্থায়ী (permanent) override** — একবার সফল হলে চিরকাল supa-ভ্যালু ব্যবহার
 *    হয়, local Room-এ পরবর্তী পরিবর্তন প্রতিফলিত হয় না যতক্ষণ না admin আবার manually
 *    `refreshAdminMetrics()`/`triggerCloudSync()` ট্রিগার করেন। channel-subscription/RLS/
 *    admin-permission — এই তিনটাই যাচাই করে বাতিল করা হয়েছে (19.3)।
 *
 * ২. **Admin action bug-class** (19.4/19.5), তিনটা confirmed distinct বাগ:
 *    - বাগ #১: `SomadhanRepository.updateWithdrawalStatus()`-এর client-side status-guard
 *      silent no-op (Unit রিটার্ন) + caller (`adminUpdateWithdrawalStatus`)-এর unconditional
 *      "সফল" toast।
 *    - বাগ #২: `adminReconcileBalances`/`adminRepairMissingRefunds`-এর `catch (e: Exception)`
 *      ব্লকে `onComplete()` না ডাকা — `AdminEscrowView.kt`-এর isRepairRunning/isReconcileRunning
 *      স্পিনার স্থায়ীভাবে আটকে থাকে।
 *    - বাগ #৩: `AdminWithdrawalsView.kt`-এর ডিফল্ট (unfiltered) ভিউ `viewModel.adminWithdrawalsPaged`
 *      (non-reactive, এক-বারের snapshot) ব্যবহার করে, কোনো mutation বা pull-to-refresh
 *      (`refreshAdminTab()`)-এই এটা রিফ্রেশ হয় না — শুধু filter-toggle/re-entry-তে হয়।
 *      `AdminUsersView.kt`-এ হুবহু একই আর্কিটেকচার আছে (generalization, individually
 *      RPC-level ট্রেস করা হয়নি)।
 *
 * ============================================================================
 * কেন এটা static source-scan টেস্ট (real runtime/Compose recomposition simulation না)
 * ============================================================================
 * `RealtimeSubscriptionScopeTest.kt` (Step 18.3)/`OfflineGatingSyncTest.kt` (Step 16.3)-এর একই
 * architectural সীমাবদ্ধতা: `SomadhanViewModel` constructor-এই সরাসরি real
 * `SomadhanRepository`/Supabase বানায় (কোনো DI seam নেই), আর `mockk`/Compose UI test framework
 * `build.gradle.kts`-এ নেই (rule #1 অনুযায়ী rule #1a-এর ব্যতিক্রম ছাড়া সেই ফাইল এডিট করা যাবে না)।
 * তাই সত্যিকারের admin session বানিয়ে ক্লিক সিমুলেট করে recomposition/state-update সরাসরি assert
 * করা এই sandbox-এ সম্ভব না। এর বদলে সোর্স টেক্সট থেকে ফাংশন-বডি/ব্লক বের করে প্রত্যাশিত প্যাটার্নের
 * উপস্থিতি/অনুপস্থিতি সরাসরি assert করা হয় (`OfflineGatingSyncTest`-এর guard-site-scan প্যাটার্নে)।
 *
 * ⚠️ নিচের বাগ-demonstrating টেস্টগুলো (নাম-এ "BugStillPresent" আছে এমন সব ক'টা) ইচ্ছাকৃতভাবে
 * এখনই FAIL করার কথা (Step 17.2-এর নীতি: "বাগ থাকলে এই সেশনেই fail দেখানোর কথা")। rule #1
 * অনুযায়ী এই সেশনে production Kotlin কোড বদলানো হয়নি (শুধু টেস্ট) — ভবিষ্যতে কেউ 19.6-এর
 * প্রস্তাবিত ফিক্স (নিচে ও progress doc-এ) প্রয়োগ করলে এই টেস্টগুলো pass করা শুরু করবে
 * (regression-guard)। বাকি টেস্টগুলো (drift-guard/premise-check) বাগ থাকা অবস্থাতেও pass করার
 * কথা — এগুলো শুধু নিশ্চিত করে যে উপরের বাগ-demonstrating টেস্টগুলোর ভিত্তি (premise) এখনো সত্যি।
 */
class AdminPanelStep19RegressionTest {

    // ------------------------------------------------------------------
    // সোর্স ফাইল লোড করা (RealtimeSubscriptionScopeTest.kt-এর একই candidate-path প্যাটার্ন)
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
                    "বদলেছে সম্ভবত, candidate paths আপডেট করো।"
            )
    }

    private val vmLines: List<String> by lazy {
        sourceFile("src/main/java/com/example/ui/viewmodel/SomadhanViewModel.kt").readLines()
    }
    private val vmText: String by lazy { vmLines.joinToString("\n") }

    private val adminUsersViewText: String by lazy {
        sourceFile("src/main/java/com/example/ui/screens/AdminUsersView.kt").readText()
    }
    private val adminEscrowViewText: String by lazy {
        sourceFile("src/main/java/com/example/ui/screens/AdminEscrowView.kt").readText()
    }

    // ------------------------------------------------------------------
    // member-level `fun <name>` region খোঁজা (RealtimeSubscriptionScopeTest.kt-এর হুবহু প্যাটার্ন)
    // ------------------------------------------------------------------

    private val memberFunPrefix = "^ {4}(?:(?:private|internal|public|suspend|override|inline)\\s+)*fun\\s+"
    private val anyMemberFunRegex = Regex(memberFunPrefix)

    private fun allFunStarts(lines: List<String>): List<Pair<Int, String>> {
        val nameRegex = Regex(memberFunPrefix + "([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")
        return lines.mapIndexedNotNull { idx, line ->
            nameRegex.find(line)?.let { idx to it.groupValues[1] }
        }
    }

    private fun functionRegions(lines: List<String>, funName: String): List<IntRange> {
        val starts = allFunStarts(lines)
        val myStarts = starts.filter { it.second == funName }
        return myStarts.map { (startIdx, _) ->
            var endIdx = lines.size - 1
            for (i in (startIdx + 1) until lines.size) {
                if (anyMemberFunRegex.containsMatchIn(lines[i])) {
                    endIdx = i - 1
                    break
                }
            }
            startIdx..endIdx
        }
    }

    private fun regionText(lines: List<String>, region: IntRange): String =
        lines.subList(region.first, region.last + 1).joinToString("\n")

    private fun singleFunctionRegionText(lines: List<String>, funName: String): String {
        val regions = functionRegions(lines, funName)
        assertTrue(
            "`$funName` পাওয়া যায়নি (বা একাধিকবার/ওভারলোড পাওয়া গেছে, regions.size=${regions.size}) -- " +
                "rename/সরানো/ওভারলোড হয়ে থাকতে পারে, এই টেস্ট ফাইলের premise পুনর্মূল্যায়ন দরকার।",
            regions.size == 1
        )
        return regionText(lines, regions.single())
    }

    // ------------------------------------------------------------------
    // class-level property `val <name>: <Type> = ...` region খোঁজা (`fun` না, তাই আলাদা হেল্পার —
    // `adminDashboardMetrics`-এর জন্য দরকার)
    // ------------------------------------------------------------------

    private fun valBlockRegion(lines: List<String>, valName: String): IntRange {
        val startRegex = Regex("^\\s*val\\s+$valName\\s*:")
        val startIdx = lines.indexOfFirst { startRegex.containsMatchIn(it) }
        require(startIdx >= 0) { "`val $valName` পাওয়া যায়নি।" }
        val nextValRegex = Regex("^ {4}val\\s+[A-Za-z_]")
        var endIdx = lines.size - 1
        for (i in (startIdx + 1) until lines.size) {
            if (nextValRegex.containsMatchIn(lines[i])) {
                endIdx = i - 1
                break
            }
        }
        return startIdx..endIdx
    }

    // ==========================================================================================
    // ১. Overview metrics bug-class (Step 19.2/19.3) — permanent override
    // ==========================================================================================

    @Test
    fun testOverviewMetricsPermanentOverrideBugFixed() {
        // Somadhan Bug-Fix Step 2: আগে এই টেস্টের নাম ছিল
        // `testOverviewMetricsPermanentOverrideBugStillPresent` আর assertion ইচ্ছাকৃতভাবে fail
        // করতো (bug-demonstrating, CI_TEST_SUITE Step 19.6)। এখন ফিক্স হয়ে যাওয়ায় "StillPresent"
        // নামটা আর সত্যি না -- rule অনুযায়ী (master prompt Step 2 নোট) রিনেম + pass-প্রত্যাশিত করা
        // হলো। assertion অপরিবর্তিত রইলো (overrideCount == 0), শুধু নাম আর মন্তব্য আপডেট।
        val region = valBlockRegion(vmLines, "adminDashboardMetrics")
        val text = regionText(vmLines, region)
        val overridePattern = Regex("""if\s*\(\s*supa\.totalUsers\s*>\s*0\s*\)""")
        val overrideCount = overridePattern.findAll(text).count()

        // সঠিক আচরণ (19.6-এর সুপারিশ অনুযায়ী প্রয়োগ করা হয়েছে, Somadhan Bug-Fix Step 2): permanent
        // override বাদ দিয়ে সবসময় local reactive aggregate ব্যবহার -- "একবার সফল হলে চিরকাল" প্যাটার্ন
        // আর নেই।
        assertTrue(
            "✅ প্রত্যাশিত পাস (Step 19.2/19.3-এ confirmed বাগ, Somadhan Bug-Fix Step 2-এ ফিক্স করা " +
                "হয়েছে) -- `adminDashboardMetrics`-এ এখনও ${overrideCount}টা `if (supa.totalUsers > 0)` " +
                "permanent-override পাওয়া গেছে (প্রত্যাশিত ০টা) -- থাকলে বুঝতে হবে ফিক্স কোথাও রিগ্রেস " +
                "করেছে অথবা নতুন কোনো override যোগ হয়েছে।",
            overrideCount == 0
        )
    }

    @Test
    fun testRefreshAdminMetricsPremiseStillManualOnly() {
        // drift-guard/premise-check (বাগ-demonstrating না) — 19.3-এ confirmed হয়েছিল
        // `refreshAdminMetrics()` শুধু ৩টা manual জায়গা থেকে কল হয়: নিজের definition,
        // `refreshAdminTab()`-এর ভেতর (শুধু Overview ট্যাব, tabIndex == 0), আর `triggerCloudSync()`।
        // কোনো periodic/timer বা admin-mutation-triggered কল-সাইট নেই। এই সংখ্যা বদলে গেলে
        // (বাড়লে) 19.2/19.3-এর "শুধু manual trigger" ফাইন্ডিং পুনর্মূল্যায়ন দরকার হতে পারে।
        val occurrenceCount = Regex("""\brefreshAdminMetrics\(\)""").findAll(vmText).count()
        assertTrue(
            "`refreshAdminMetrics()`-এর occurrence সংখ্যা প্রত্যাশার চেয়ে কম (${occurrenceCount}টা " +
                "পাওয়া গেছে, কমপক্ষে ৩টা প্রত্যাশিত: definition + refreshAdminTab + triggerCloudSync) " +
                "-- ফাংশনটা rename/সরানো হয়ে থাকতে পারে, 19.2/19.3-এর ফাইন্ডিং পুনর্মূল্যায়ন দরকার।",
            occurrenceCount >= 3
        )
    }

    // ==========================================================================================
    // ২. Withdrawal approve/reject: silent no-op guard + unconditional success toast
    //    (Step 19.4, বাগ #১)
    // ==========================================================================================

    @Test
    fun testWithdrawalStatusUpdateSurfacesFailureBugFixed() {
        // Somadhan Bug-Fix Step 3: আগে এই টেস্টের নাম ছিল
        // `testWithdrawalStatusUpdateSwallowsFailureBugStillPresent` আর দ্বিতীয় assertion
        // ইচ্ছাকৃতভাবে fail করতো (bug-demonstrating, CI_TEST_SUITE Step 19.6)। এখন ফিক্স হয়ে
        // যাওয়ায় (repository sealed `WithdrawalUpdateResult` রিটার্ন করে, caller `when` দিয়ে
        // conditional toast দেখায়) "StillPresent" নামটা আর সত্যি না -- rule অনুযায়ী (master
        // prompt Step 2/3 নোট) রিনেম + pass-প্রত্যাশিত করা হলো। assertion-এর regex/premise
        // অপরিবর্তিত, শুধু নাম আর মন্তব্য আপডেট।
        val text = singleFunctionRegionText(vmLines, "adminUpdateWithdrawalStatus")

        val callsRepo = Regex("""repository\.updateWithdrawalStatus\(""").containsMatchIn(text)
        assertTrue(
            "`adminUpdateWithdrawalStatus()`-এ আর `repository.updateWithdrawalStatus(...)` কল পাওয়া " +
                "যাচ্ছে না -- ফাংশনটা rename/রিফ্যাক্টর হয়ে থাকতে পারে, এই টেস্টের premise পুনর্মূল্যায়ন দরকার।",
            callsRepo
        )

        val unconditionalToastRightAfter = Regex(
            """repository\.updateWithdrawalStatus\([^)]*\)\s*\n\s*showToast\("""
        ).containsMatchIn(text)

        // সঠিক আচরণ (19.6-এর সুপারিশ অনুযায়ী প্রয়োগ করা হয়েছে, Somadhan Bug-Fix Step 3):
        // `repository.updateWithdrawalStatus()` এখন sealed `WithdrawalUpdateResult`
        // (Updated/GuardBlocked) রিটার্ন করে, আর caller `when` দিয়ে শর্তসাপেক্ষে toast দেখায় --
        // তাই unconditional toast-right-after-call প্যাটার্ন আর নেই।
        assertTrue(
            "✅ প্রত্যাশিত পাস (Step 19.4-এ confirmed বাগ #১, Somadhan Bug-Fix Step 3-এ ফিক্স করা হয়েছে) " +
                "-- `adminUpdateWithdrawalStatus()`-এ এখনও `repository.updateWithdrawalStatus()`-এর পরের " +
                "লাইনেই unconditional \"সফল\" toast পাওয়া গেছে -- থাকলে বুঝতে হবে ফিক্স কোথাও রিগ্রেস করেছে।",
            !unconditionalToastRightAfter
        )
    }

    // ==========================================================================================
    // ৩. Reconcile/repair: catch-এ onComplete() না ডাকা → স্থায়ী স্টাক স্পিনার (Step 19.4, বাগ #২)
    // ==========================================================================================

    @Test
    fun testReconcileAndRepairStuckSpinnerBugStillPresent() {
        val failures = mutableListOf<String>()
        for (funName in listOf("adminRepairMissingRefunds", "adminReconcileBalances")) {
            val text = singleFunctionRegionText(vmLines, funName)
            val catchIdx = text.indexOf("catch (e: Exception)")
            if (catchIdx < 0) {
                failures += "`$funName`-এ `catch (e: Exception)` ব্লকই পাওয়া যায়নি (রিফ্যাক্টর হয়ে " +
                    "থাকতে পারে, premise পুনর্মূল্যায়ন দরকার)।"
                continue
            }
            val catchBlockText = text.substring(catchIdx)
            if (!catchBlockText.contains("onComplete(")) {
                failures += "`$funName`-এর catch ব্লকে এখনো `onComplete(...)` কল নেই -- exception হলে " +
                    "UI-স্তরের isRepairRunning/isReconcileRunning স্পিনার চিরস্থায়ী আটকে থাকবে " +
                    "(AdminEscrowView.kt-এ শুধু onComplete callback-এর ভেতরেই এই flag false হয়)।"
            }
        }
        assertTrue(
            "⚠️ প্রত্যাশিতভাবেই ব্যর্থ (Step 19.4-এ confirmed বাগ #২, rule #1 অনুযায়ী এখনো ফিক্স করা হয়নি):\n" +
                failures.joinToString("\n") { "  - $it" },
            failures.isEmpty()
        )
    }

    @Test
    fun testAdminEscrowViewSpinnerPremiseStillOnlyResetInOnComplete() {
        // drift-guard/premise-check (বাগ-demonstrating না) -- উপরের টেস্টের ভিত্তি এখনো সত্যি কিনা।
        assertTrue(
            "`isRepairRunning = false` আর `AdminEscrowView.kt`-এ পাওয়া যাচ্ছে না -- Step 19.4-এর " +
                "premise পুনর্মূল্যায়ন দরকার।",
            Regex("""isRepairRunning\s*=\s*false""").containsMatchIn(adminEscrowViewText)
        )
        assertTrue(
            "`isReconcileRunning = false` আর `AdminEscrowView.kt`-এ পাওয়া যাচ্ছে না -- Step 19.4-এর " +
                "premise পুনর্মূল্যায়ন দরকার।",
            Regex("""isReconcileRunning\s*=\s*false""").containsMatchIn(adminEscrowViewText)
        )
    }

    // ==========================================================================================
    // ৪. AdminWithdrawalsView ডিফল্ট ভিউ: non-reactive paged snapshot staleness (Step 19.5, বাগ #৩)
    // ==========================================================================================

    @Test
    fun testAdminWithdrawalsPagedStalenessBugStillPresent() {
        val text = singleFunctionRegionText(vmLines, "adminUpdateWithdrawalStatus")
        // প্রত্যাশিত সঠিক আচরণ (ফিক্সের পর): সফল status-update-এর পর
        // `resetAdminWithdrawalsPagination()` কল করে ডিফল্ট-ভিউ-এর snapshot রিফ্রেশ করা উচিত।
        assertTrue(
            "⚠️ প্রত্যাশিতভাবেই ব্যর্থ (Step 19.5-এ confirmed বাগ #৩, rule #1 অনুযায়ী এখনো ফিক্স করা হয়নি) " +
                "-- `adminUpdateWithdrawalStatus()` এখনো কোথাও `resetAdminWithdrawalsPagination()` কল " +
                "করে না -- ফলে admin Withdrawals ট্যাবের ডিফল্ট (unfiltered) ভিউ-তে থাকা " +
                "`adminWithdrawalsPaged` non-reactive স্ন্যাপশট approve/reject-এর পরেও পুরনো " +
                "status-এই আটকে থাকবে, যতক্ষণ না admin filter টগল করেন বা স্ক্রিন থেকে বেরিয়ে আবার " +
                "ঢোকেন।",
            text.contains("resetAdminWithdrawalsPagination()")
        )
    }

    @Test
    fun testRefreshAdminTabDoesNotResetWithdrawalsOrUsersPagedCacheBugStillPresent() {
        val text = singleFunctionRegionText(vmLines, "refreshAdminTab")
        // প্রত্যাশিত সঠিক আচরণ (ফিক্সের পর): pull-to-refresh-এও Withdrawals/Users ট্যাবের paged
        // snapshot রিফ্রেশ হওয়া উচিত।
        assertTrue(
            "⚠️ প্রত্যাশিতভাবেই ব্যর্থ (Step 19.5-এ confirmed -- pull-to-refresh এই বাগ ফিক্স করে না) " +
                "-- `refreshAdminTab()`-এ এখনো `resetAdminWithdrawalsPagination()`/" +
                "`resetAdminUsersPagination()` কোনোটাই কল হয় না, তাই Withdrawals/Users ট্যাবে " +
                "pull-to-refresh টানলেও ডিফল্ট (unfiltered) ভিউ-এর paged snapshot রিফ্রেশ হয় না।",
            text.contains("resetAdminWithdrawalsPagination(") && text.contains("resetAdminUsersPagination(")
        )
    }

    @Test
    fun testAdminUsersViewSharesTheSameNonReactivePagedCacheArchitecture() {
        // drift-guard/generalization (19.5-এর ফাইন্ডিং, বাগ-demonstrating না -- এই স্ক্রিনের জন্য
        // পৃথক action-level RPC ট্রেস এই ধাপে করা হয়নি) -- AdminUsersView.kt হুবহু একই আর্কিটেকচার
        // ব্যবহার করে কিনা যাচাই করে (করলে বাগ #৩-এর "generalization" দাবির ভিত্তি অক্ষত থাকে)।
        assertTrue(
            "`AdminUsersView.kt`-এ আর `isBrowsingUnfiltered` + `viewModel.adminUsersPaged` প্যাটার্ন " +
                "একসাথে পাওয়া যাচ্ছে না -- Step 19.5-এর generalization-finding পুনর্মূল্যায়ন দরকার " +
                "(হয়তো ভালো খবর -- স্ক্রিনটা অন্যভাবে ফিক্স হয়ে থাকতে পারে)।",
            adminUsersViewText.contains("isBrowsingUnfiltered") &&
                adminUsersViewText.contains("viewModel.adminUsersPaged")
        )
    }
}
