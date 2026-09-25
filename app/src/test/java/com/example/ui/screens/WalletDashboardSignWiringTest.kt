package com.example.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Step 15.3 — CI_TEST_SUITE_MASTER_PROMPT.md.
 *
 * ============================================================================
 * কেন এটা source-scan (static structural) টেস্ট, Compose/Robolectric রানটাইম
 * টেস্ট না
 * ============================================================================
 * `UserWalletScreen.kt` আর `DashboardScreen.kt`-এ ধরা পড়া বাগ (Step 15.1 inventory)
 * `TransactionHistoryScreen.kt`-এর (Step 15.2) একই বাগ-ক্লাসের হুবহু/প্রায়-হুবহু কপি
 * ছিল — তিনটাই `isEarning || isUserRefund || (isUserDeposit)` ধরনের একটা enumeration
 * দিয়ে `isPositive` বানাত যেটা কিছু `trx.type`-এ নীরবে `false`-এ পড়ে যেত। Step 15.2-এ
 * ইতিমধ্যে একটা pure, type-agnostic ফাংশন `transactionDisplaySign(trx, viewerId): Boolean`
 * বানানো হয়েছে (`TransactionHistoryScreen.kt`-এ top-level, `com.example.ui.screens`
 * প্যাকেজে — তিনটা ফাইলই একই প্যাকেজে, তাই import ছাড়াই visible) আর
 * `TransactionDisplaySignTest.kt`-এ ২৩টা কেস দিয়ে সম্পূর্ণ যাচাই করা হয়েছে।
 *
 * Step 15.3-এর ফিক্স তাই **নতুন কোনো pure function বের করেনি** — একই ফাংশন পুনর্ব্যবহার
 * করা হয়েছে দুটো নতুন call-site-এ। যেহেতু ফাংশনের behavior ইতিমধ্যেই সম্পূর্ণ টেস্ট করা,
 * এখানে সেই ২৩টা কেস আবার ডুপ্লিকেট করা মূল্যহীন — বরং আসল ঝুঁকি হলো **wiring**:
 * (ক) কেউ ভুলে পুরনো buggy inline expression ফিরিয়ে আনতে পারে, বা (খ) `viewerId`
 * প্যারামিটারে ভুল ভ্যারিয়েবল (যেমন role-scoped filtering-এর সাথে না-মেলা কোনো id)
 * বসাতে পারে। এই টেস্ট ঠিক এই দুটো জিনিসই সোর্স-লেভেলে verify করে — `DualWriteGapTest.kt`-এর
 * একই স্ট্যাটিক-স্ক্যান কনভেনশন অনুসরণ করে (কারণ `SupabaseSyncManager`-এর মতো এখানেও কোনো
 * DI/mocking seam নেই যা দিয়ে পুরো Composable রেন্ডার করে item-লেভেল state যাচাই করা যায়,
 * আর sandbox-এ Gradle/Robolectric রান সম্ভব না — Step 12.x/15.2-এর নোট দ্রষ্টব্য)।
 *
 * এই টেস্ট **assert করে না** যে sign-লজিক সঠিক (সেটা `TransactionDisplaySignTest.kt`-এর
 * কাজ) — শুধু assert করে যে দুটো screen আসলে সেই verified ফাংশনটাই কল করছে, পুরনো
 * buggy expression না।
 */
class WalletDashboardSignWiringTest {

    private fun sourceFile(relativeFromApp: String): File {
        val candidates = listOf(
            File(relativeFromApp),
            File("app/$relativeFromApp")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error(
                "$relativeFromApp পাওয়া যায়নি কোনো candidate path-এ " +
                    "(cwd=${File(".").absolutePath}) -- Gradle-এর test working directory " +
                    "বদলেছে সম্ভবত, WalletDashboardSignWiringTest-এর candidate paths আপডেট করো।"
            )
    }

    private val historyScreenText: String by lazy {
        sourceFile("src/main/java/com/example/ui/screens/TransactionHistoryScreen.kt").readText()
    }

    private val walletScreenText: String by lazy {
        sourceFile("src/main/java/com/example/ui/screens/UserWalletScreen.kt").readText()
    }

    private val dashboardScreenText: String by lazy {
        sourceFile("src/main/java/com/example/ui/screens/DashboardScreen.kt").readText()
    }

    // ------------------------------------------------------------------
    // ১. shared pure function এখনো TransactionHistoryScreen.kt-তেই আছে, আর তার
    //    signature/source-of-truth (netAmount sign) অপরিবর্তিত — এটা ধরে রাখে যে
    //    UserWalletScreen/DashboardScreen যেটার উপর নির্ভর করছে সেটা এখনো সেই একই,
    //    আগে-verified ফাংশন, অন্য কোনো re-derivation না।
    // ------------------------------------------------------------------

    @Test
    fun `shared transactionDisplaySign still lives top-level in TransactionHistoryScreen`() {
        assertTrue(
            "TransactionHistoryScreen.kt-এ `fun transactionDisplaySign(...)` পাওয়া যায়নি — " +
                "সরানো/rename হয়ে থাকলে UserWalletScreen.kt ও DashboardScreen.kt-এর নতুন কল-সাইট " +
                "কম্পাইলই হবে না, কিন্তু এই টেস্ট সেটা এখানে স্পষ্টভাবে ধরে রাখে।",
            Regex("""fun\s+transactionDisplaySign\s*\(""").containsMatchIn(historyScreenText)
        )
        assertTrue(
            "transactionDisplaySign()-এর body-তে আর `trx.netAmount >= 0.0` (single source-of-truth " +
                "sign check) নেই — Step 15.2-এর মূল ফিক্স ফিরিয়ে নেওয়া হয়েছে সম্ভবত, যা এই ফাংশনের " +
                "উপর নির্ভরশীল UserWalletScreen/DashboardScreen-এর ফিক্সও নীরবে ভেঙে দেবে।",
            historyScreenText.contains("trx.netAmount >= 0.0")
        )
    }

    // ------------------------------------------------------------------
    // ২. UserWalletScreen.kt — নতুন কল-সাইট সঠিক ফাংশন + সঠিক viewerId (currentUid,
    //    যেটা matchesRoleForHistory()-এ ব্যবহৃত হয় role/identity filter করতে) দিয়ে
    //    হচ্ছে কিনা, আর পুরনো buggy inline expression `isPositive`-এর মান হিসেবে
    //    আর ব্যবহৃত হচ্ছে না কিনা।
    // ------------------------------------------------------------------

    @Test
    fun `UserWalletScreen isPositive now calls shared transactionDisplaySign with currentUid`() {
        assertTrue(
            "UserWalletScreen.kt-এ `val isPositive = transactionDisplaySign(trx, currentUid)` " +
                "পাওয়া যায়নি — Step 15.3-এর ফিক্স সরানো/বদলানো হয়েছে সম্ভবত।",
            walletScreenText.contains("val isPositive = transactionDisplaySign(trx, currentUid)")
        )
        assertFalse(
            "UserWalletScreen.kt-এ পুরনো buggy inline expression " +
                "`val isPositive = isEarning || isUserRefund || isUserDeposit` এখনো আছে — " +
                "Step 15.3-এর ফিক্স আসলে প্রয়োগ হয়নি বা আংশিক revert হয়ে গেছে।",
            walletScreenText.contains("val isPositive = isEarning || isUserRefund || isUserDeposit")
        )
    }

    // ------------------------------------------------------------------
    // ৩. DashboardScreen.kt — একই যাচাই, শুধু viewerId এখানে currentSolverId
    //    (recentTransactions ইতিমধ্যেই matchesRoleForHistory(it, "SOLVER", currentSolverId)
    //    দিয়ে স্কোপড, তাই এই id-ই সঠিক)।
    // ------------------------------------------------------------------

    @Test
    fun `DashboardScreen isPositive now calls shared transactionDisplaySign with currentSolverId`() {
        assertTrue(
            "DashboardScreen.kt-এ `val isPositive = transactionDisplaySign(trx, currentSolverId)` " +
                "পাওয়া যায়নি — Step 15.3-এর ফিক্স সরানো/বদলানো হয়েছে সম্ভবত।",
            dashboardScreenText.contains("val isPositive = transactionDisplaySign(trx, currentSolverId)")
        )
        assertFalse(
            "DashboardScreen.kt-এ পুরনো buggy inline expression " +
                "`val isPositive = isEarning || isUserRefund` এখনো আছে — Step 15.3-এর ফিক্স আসলে " +
                "প্রয়োগ হয়নি বা আংশিক revert হয়ে গেছে।",
            dashboardScreenText.contains("val isPositive = isEarning || isUserRefund")
        )
    }

    // ------------------------------------------------------------------
    // ৪. drift-guard: UserWalletScreen.kt/DashboardScreen.kt নিজেরা যেন কোনো
    //    প্রতিদ্বন্দ্বী/আলাদা `fun transactionDisplaySign` declare না করে ফেলে (তাহলে
    //    Kotlin overload-resolution অনুযায়ী কোনটা আসলে কল হচ্ছে অস্পষ্ট হয়ে যাবে, আর
    //    দুটো কপি ভবিষ্যতে আলাদা আচরণে drift করতে পারে — ঠিক যে ঝুঁকির কথা
    //    `TransactionHelper.kt`-এর `resolveLegacyTransactionRole()` কমেন্টে ইতিমধ্যেই
    //    উল্লেখ আছে)।
    // ------------------------------------------------------------------

    @Test
    fun `wallet and dashboard screens do not declare a competing transactionDisplaySign`() {
        val declRegex = Regex("""fun\s+transactionDisplaySign\s*\(""")
        assertFalse(
            "UserWalletScreen.kt নিজেই একটা `transactionDisplaySign` declare করছে — এটা shared " +
                "ফাংশনের সাথে drift করতে পারে, TransactionHistoryScreen.kt-এর একটাই বজায় রাখা উচিত।",
            declRegex.containsMatchIn(walletScreenText)
        )
        assertFalse(
            "DashboardScreen.kt নিজেই একটা `transactionDisplaySign` declare করছে — এটা shared " +
                "ফাংশনের সাথে drift করতে পারে, TransactionHistoryScreen.kt-এর একটাই বজায় রাখা উচিত।",
            declRegex.containsMatchIn(dashboardScreenText)
        )
    }
}
