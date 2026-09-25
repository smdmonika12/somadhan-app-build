package com.example.repository

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * [Step 12.11] escrow id mismatch fix-এর wiring গার্ড (static source-scan, DualWriteGapTest-এর ধরনে; pure JVM)।
 *
 * fix-এর সারকথা: client-এর local escrow id ("ESCROW_<৮>") cloud RPC-তে `p_escrow_id` হিসেবে যায়, তাই cloud id ==
 * local id। এই টেস্ট শুধু নিশ্চিত করে যে (ক) দুই কল-সাইট id পাঠায়, (খ) outbox-replay dispatcher সেই ঐচ্ছিক
 * "escrowId" key পড়ে — কেউ ভুলে একটা সরালে আবার mismatch ফিরে আসবে, সেটা এখানে ধরা পড়বে।
 */
class EscrowIdWiringTest {

    private fun readSource(relative: String): List<String> {
        val candidates = listOf(File("src/main/java/com/example/$relative"), File("app/src/main/java/com/example/$relative"))
        val f = candidates.firstOrNull { it.exists() }
            ?: error("$relative পাওয়া যায়নি (cwd=${File(".").absolutePath})")
        return f.readText().lines()
    }

    @Test
    fun `repository passes local escrow id to accept_bid and accept_direct_contract`() {
        val repo = readSource("data/repository/SomadhanRepository.kt")
        val acceptBidCalls = repo.filter { it.contains("SupabaseSyncManager.acceptBid(") && !it.trimStart().startsWith("//") }
        val directCalls = repo.filter { it.contains("SupabaseSyncManager.acceptDirectContract(") && !it.trimStart().startsWith("//") }
        assertTrue("SupabaseSyncManager.acceptBid( কল পাওয়া যায়নি", acceptBidCalls.isNotEmpty())
        assertTrue("SupabaseSyncManager.acceptDirectContract( কল পাওয়া যায়নি", directCalls.isNotEmpty())
        acceptBidCalls.forEach { assertTrue("acceptBid কলে escrowId নেই: $it", it.contains("escrowId =")) }
        directCalls.forEach { assertTrue("acceptDirectContract কলে escrowId নেই: $it", it.contains("escrowId =")) }
    }

    @Test
    fun `outbox dispatcher reads optional escrowId for accept_bid and accept_direct_contract`() {
        val text = readSource("data/sync/OutboxRpcDispatcher.kt").joinToString("\n")
        for (branch in listOf("\"accept_bid\" ->", "\"accept_direct_contract\" ->")) {
            val start = text.indexOf(branch)
            assertTrue("dispatcher-এ $branch branch পাওয়া যায়নি", start >= 0)
            val end = text.indexOf("\n\n", start).let { if (it < 0) text.length else it }
            val block = text.substring(start, end)
            assertTrue("$branch branch optionalString(\"escrowId\") পড়ে না", block.contains("optionalString(\"escrowId\")"))
        }
    }
}
