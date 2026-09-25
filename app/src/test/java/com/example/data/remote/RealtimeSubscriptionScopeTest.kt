package com.example.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Step 18.3 — CI_TEST_SUITE_MASTER_PROMPT.md (Step 18 — multi-account/same-device
 * balance-escrow desync)।
 *
 * ============================================================================
 * প্রেক্ষাপট (Step 18.1/18.2-এর findings, বিস্তারিত CI_TEST_SUITE_PROGRESS.md-এ)
 * ============================================================================
 * Step 18.1: SQL-স্তরে (`accept_bid`/`release_escrow`/`refund_escrow_once`) কোনো atomicity gap
 * বা user-scoping ভুল পাওয়া যায়নি — root cause SQL/RPC-স্তরে না।
 *
 * Step 18.2: Kotlin-স্তরে ট্রেস করে **প্রধান সন্দেহভাজন root cause** পাওয়া গেছে:
 * `SupabaseRealtimeManager`-এর প্রতি-ইউজার `user:<uuid>` broadcast channel
 * (`startUserTopicBroadcastSubscription()`, balance/escrow/transaction/withdrawal সহ ৭টা টেবিল
 * কভার করে) app-process-এর জীবনে **প্রথম login/attach-এর সময় যে account সক্রিয় ছিল সেই topic-এই
 * চিরকাল আটকে থাকে** — কারণ `logout()`, `completeLoginAfterOtp()`, `switchRoleToSolver()`,
 * `switchRoleToUser()`, `loginAsAdmin()` — এই ৫টা account-switch-lifecycle ফাংশনের **একটাও**
 * `SupabaseRealtimeManager.startRealtimeListeners()`/`stopRealtimeListeners()` কল করে না। অথচ
 * `startUserTopicBroadcastSubscription()`-এর নিজস্ব doc-কমেন্ট স্পষ্ট বলে এটা "idempotent —
 * একই/ভিন্ন userId দিয়ে বারবার কল হলে (... বা role/user পরিবর্তনে) আগেরটা বন্ধ করে নতুন করে
 * subscribe করে" — অর্থাৎ এই মেকানিজম role/user-পরিবর্তনে re-scope হওয়ার জন্যই ডিজাইন করা, কিন্তু
 * কোনো caller-ই সেই re-scope ট্রিগার করে না। `AppDatabase`-এর singleton + `attachDatabase()`-এর
 * idempotency guard (`if (localDb === database) return`) এই বাগকে আরও পোক্ত করে — দ্বিতীয়বার
 * `attachDatabase()` কল (যেমন role-switch/re-login-এর পরে ViewModel পুনরায় init হলে) no-op হয়ে
 * যায়, তাই সেই পথেও re-subscribe হয় না।
 *
 * ============================================================================
 * কেন এটা static source-scan টেস্ট (real multi-account runtime simulation না)
 * ============================================================================
 * `DualWriteGapTest.kt` (Step 12)/`OfflineGatingSyncTest.kt` (Step 16.3)-এর একই architectural
 * সীমাবদ্ধতা: `SupabaseRealtimeManager` একটা Kotlin `object` (সিঙ্গেলটন, কোনো DI seam নেই),
 * `SupabaseClientProvider.client.realtime` real network websocket ছোঁয়, আর `mockk`
 * `build.gradle.kts`-এ নেই (rule #1 অনুযায়ী rule #1a-এর ব্যতিক্রম ছাড়া সেই ফাইল এডিট করা যাবে
 * না)। তাই দুইটা account দিয়ে সত্যিই logout→login সিমুলেট করে "কোন topic-এ সাবস্ক্রাইবড আছে"
 * সরাসরি assert করা এই sandbox-এ সম্ভব না। এর বদলে — ঠিক `OfflineGatingSyncTest`-এর
 * `requireOnlineOrWarn()`-গার্ড-সাইট-স্ক্যানের প্যাটার্নেই — সোর্স টেক্সট থেকে প্রতিটা লাইফসাইকেল
 * ফাংশনের boundary বের করে সরাসরি assert করা হয় যে ফাংশন-বডিতে
 * `startRealtimeListeners()`/`stopRealtimeListeners()` কল আছে কিনা।
 *
 * ⚠️ **এই টেস্ট ইচ্ছাকৃতভাবে এখনই ব্যর্থ হওয়ার কথা (Step 17.2-এর নীতি: "বাগ থাকলে এই সেশনেই fail
 * দেখানোর কথা")।** এটা encode করে *প্রত্যাশিত সঠিক আচরণ* (প্রতিটা account-switch lifecycle ফাংশন
 * realtime subscription re-scope করবে) — বর্তমান কোডে এই ৫টার একটাও তা করে না, তাই নিচের
 * `accountSwitchFunctionsShouldRescopeBroadcast` টেস্ট **৫টা failure দেখানোর কথা**। rule #1
 * অনুযায়ী এই সেশনে production Kotlin কোড বদলানো হয়নি (শুধু টেস্ট) — ভবিষ্যতে কেউ Step 18.4-এর
 * প্রস্তাবিত ফিক্স প্রয়োগ করলে এই টেস্ট pass করা শুরু করবে (regression-guard)।
 */
class RealtimeSubscriptionScopeTest {

    // ------------------------------------------------------------------
    // সোর্স ফাইল লোড করা (`DualWriteGapTest.kt`/`OfflineGatingSyncTest.kt`-এর একই
    // candidate-path প্যাটার্ন — Gradle-এর test working directory root বা `app/` দুটোই হতে পারে)
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
                    "বদলেছে সম্ভবত, RealtimeSubscriptionScopeTest-এর candidate paths আপডেট করো।"
            )
    }

    private val vmLines: List<String> by lazy {
        sourceFile("src/main/java/com/example/ui/viewmodel/SomadhanViewModel.kt").readLines()
    }

    private val realtimeManagerLines: List<String> by lazy {
        sourceFile("src/main/java/com/example/data/remote/SupabaseRealtimeManager.kt").readLines()
    }

    private val appDatabaseText: String by lazy {
        sourceFile("src/main/java/com/example/data/database/AppDatabase.kt").readText()
    }

    // ------------------------------------------------------------------
    // member-level `fun <name>` খোঁজা -- সব occurrence, লাইন-নাম্বার-নির্ভরতা ছাড়া
    // (OfflineGatingSyncTest.kt-এর allFunStarts/functionRegions()-এর হুবহু প্যাটার্ন, generic
    // করা হলো দুটো ভিন্ন ফাইলে একই লজিক পুনর্ব্যবহারের জন্য)
    // ------------------------------------------------------------------

    private val memberFunPrefix = "^ {4}(?:(?:private|internal|public|suspend|override|inline)\\s+)*fun\\s+"
    private val anyMemberFunRegex = Regex(memberFunPrefix)

    private fun isCommentLine(line: String): Boolean {
        val t = line.trimStart()
        return t.startsWith("//") || t.startsWith("/*") || t.startsWith("*")
    }

    private fun allFunStarts(lines: List<String>): List<Pair<Int, String>> {
        val nameRegex = Regex(memberFunPrefix + "([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")
        return lines.mapIndexedNotNull { idx, line ->
            nameRegex.find(line)?.let { idx to it.groupValues[1] }
        }
    }

    /** `funName`-এর সব occurrence-এর region (0-indexed, inclusive), ফাইলে যে ক্রমে আছে সেই ক্রমেই। */
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

    // ------------------------------------------------------------------
    // ১. ⚠️ মূল বাগ-demonstrating টেস্ট (এখনই ব্যর্থ হওয়ার কথা, উপরের class-KDoc দ্রষ্টব্য) —
    //    ৫টা account-switch lifecycle ফাংশনের প্রতিটাই realtime subscription re-scope করা উচিত।
    // ------------------------------------------------------------------

    private val accountSwitchLifecycleFunctions = listOf(
        "logout",
        "completeLoginAfterOtp",
        "switchRoleToSolver",
        "switchRoleToUser",
        "loginAsAdmin"
    )

    private val rescopeCallRegex = Regex("""startRealtimeListeners\(\)|stopRealtimeListeners\(\)""")

    @Test
    fun `logout, completeLoginAfterOtp, switchRoleToSolver, switchRoleToUser and loginAsAdmin should rescope the per-user realtime broadcast subscription`() {
        val failures = mutableListOf<String>()

        for (funName in accountSwitchLifecycleFunctions) {
            val regions = functionRegions(vmLines, funName)
            if (regions.isEmpty()) {
                failures += "`$funName` -- SomadhanViewModel.kt-এ পাওয়াই যায়নি (rename/সরানো হয়েছে সম্ভবত)।"
                continue
            }
            // প্রতিটা occurrence-এই (ওভারলোড থাকলে) re-scope কল থাকা উচিত -- এই ৫টার কোনোটারই
            // বর্তমানে একাধিক ওভারলোড নেই (SomadhanViewModel.kt-এ), তাই সাধারণত একটাই region।
            regions.forEachIndexed { idx, region ->
                val text = regionText(vmLines, region)
                if (!rescopeCallRegex.containsMatchIn(text)) {
                    failures += "`$funName` (occurrence ${idx + 1}/${regions.size}, লাইন ${region.first + 1}) -- " +
                        "`startRealtimeListeners()`/`stopRealtimeListeners()` কোনোটাই কল করছে না। এর ফলে এই " +
                        "account-switch-এর পরেও আগের account-এর `user:<uuid>` broadcast subscription-এই " +
                        "আটকে থাকবে (Step 18.2-এ চিহ্নিত root cause — বিস্তারিত CI_TEST_SUITE_PROGRESS.md-এর " +
                        "\"Step 18.2\" সেকশনে)।"
                }
            }
        }

        assertTrue(
            "⚠️ প্রত্যাশিতভাবেই ব্যর্থ (Step 18-এর confirmed বাগ, rule #1 অনুযায়ী এখনো ফিক্স করা হয়নি) -- " +
                "নিচের account-switch lifecycle ফাংশন(গুলো) realtime subscription re-scope করছে না:\n" +
                failures.joinToString("\n") { "  - $it" },
            failures.isEmpty()
        )
    }

    // ------------------------------------------------------------------
    // ২. `startUserTopicBroadcastSubscription()`-এর idempotent unsubscribe→resubscribe প্যাটার্ন
    //    এখনো অক্ষত আছে কিনা (drift-guard) -- এই ফাংশনের নিজস্ব doc-কমেন্ট claim করে এটা "role/user
    //    পরিবর্তনে" re-scope-এর জন্য প্রস্তুত -- সেটাই যেন সত্যি থাকে, নাহলে উপরের টেস্ট ১-এর assertion
    //    (ফিক্স হিসেবে caller-রা এটা কল করবে) ভিত্তিহীন হয়ে যাবে।
    // ------------------------------------------------------------------

    @Test
    fun `startUserTopicBroadcastSubscription still unsubscribes the previous channel before subscribing the new one`() {
        val regions = functionRegions(realtimeManagerLines, "startUserTopicBroadcastSubscription")
        assertTrue(
            "`startUserTopicBroadcastSubscription` ফাংশনই পাওয়া যায়নি SupabaseRealtimeManager.kt-এ -- " +
                "rename/সরানো হয়েছে সম্ভবত, যেটা Step 18-এর পুরো root-cause hypothesis-কেই invalid করে।",
            regions.size == 1
        )
        val text = regionText(realtimeManagerLines, regions.single())

        assertTrue(
            "`startUserTopicBroadcastSubscription()`-এ আর পুরনো `userTopicBroadcastChannel`-এর " +
                "`unsubscribe()` কল নেই -- বারবার কল হলে (ফিক্স হিসেবে account-switch-এ কল হওয়া শুরু " +
                "করলে) পুরনো channel leak হতে পারে।",
            Regex("""userTopicBroadcastChannel\?\.let\s*\{\s*runCatching\s*\{\s*it\.unsubscribe\(\)""").containsMatchIn(text)
        )
        assertTrue(
            "`startUserTopicBroadcastSubscription()`-এর শেষে আর `userTopicBroadcastChannel = ch` " +
                "(নতুন channel স্টোর করা) নেই -- future unsubscribe/re-scope কল কাজ করবে না।",
            Regex("""userTopicBroadcastChannel\s*=\s*ch\b""").containsMatchIn(text)
        )
        assertTrue(
            "`startUserTopicBroadcastSubscription(userId)`-এর প্যারামিটার সিগনেচার বদলে গেছে সম্ভবত " +
                "(আর `userId` নামে নেয় না) -- উপরের টেস্ট ১-এর প্রস্তাবিত ফিক্স (caller থেকে সঠিক নতুন " +
                "account-এর id পাস করা) reconcile করার আগে এটা যাচাই করা দরকার।",
            realtimeManagerLines[regions.single().first].contains("startUserTopicBroadcastSubscription(userId: String)")
        )
    }

    // ------------------------------------------------------------------
    // ৩. `attachDatabase()`-এর idempotency guard (drift-guard) -- Step 18.2-এর finding অনুযায়ী
    //    এই guard-ই কারণ দ্বিতীয়বার (role-switch/re-login-এর পরে) কল হলেও no-op হয়ে যায়, তাই এই
    //    পথ দিয়েও re-subscribe হয় না। guard সরে গেলে (বা logic বদলালে) Step 18-এর hypothesis
    //    পুনর্মূল্যায়ন লাগবে, তাই এখানে আলাদা করে pin করা হলো।
    // ------------------------------------------------------------------

    @Test
    fun `attachDatabase keeps its localDb identity idempotency guard (contributes to the account-switch bug)`() {
        val regions = functionRegions(realtimeManagerLines, "attachDatabase")
        assertTrue(
            "`attachDatabase` ফাংশনই পাওয়া যায়নি SupabaseRealtimeManager.kt-এ -- rename/সরানো হয়েছে সম্ভবত।",
            regions.size == 1
        )
        val text = regionText(realtimeManagerLines, regions.single())
        assertTrue(
            "`attachDatabase()`-এ আর `if (localDb === database) { return }` guard নেই -- Step 18.2-এর " +
                "hypothesis (দ্বিতীয়বার attachDatabase() কল no-op হয়ে যাওয়াই একটা contributing factor) " +
                "আর প্রযোজ্য না থাকতে পারে, progress doc-এ পুনর্মূল্যায়ন দরকার।",
            Regex("""if\s*\(\s*localDb\s*===\s*database\s*\)\s*\{?\s*return""").containsMatchIn(text)
        )
    }

    // ------------------------------------------------------------------
    // ৪. `AppDatabase`-এর single shared singleton (drift-guard) -- Step 18.2-এর কনফার্মড finding
    //    "একটাই সিঙ্গেলটন INSTANCE, প্রতিটা account/role-এর জন্য আলাদা কোনো DB ফাইল/instance না"।
    // ------------------------------------------------------------------

    @Test
    fun `AppDatabase still uses a single shared Room instance, not a per-account database`() {
        assertTrue(
            "`AppDatabase.kt`-এ আর একক `\"somadhan_database\"` নামের Room ডাটাবেস তৈরি হচ্ছে না -- " +
                "single-shared-DB hypothesis (Step 18.2) হয়তো আর সত্যি না, পুনর্মূল্যায়ন দরকার।",
            appDatabaseText.contains("\"somadhan_database\"")
        )
        assertFalse(
            "`AppDatabase.kt`-এ এখন account/user-id-নির্ভর ডাটাবেস-নাম পাওয়া যাচ্ছে (যেমন ইন্টারপোলেটেড " +
                "স্ট্রিং টেমপ্লেট) -- per-account DB-তে migrate হয়ে থাকলে Step 18-এর পুরো hypothesis " +
                "পুনর্বিবেচনা দরকার (এটা ভালো খবর হতে পারে, কিন্তু এই টেস্টের ধরে নেওয়া premise বদলে যাবে)।",
            Regex("""somadhan_database\$\{""").containsMatchIn(appDatabaseText)
        )
    }
}
