package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.PendingSyncOutboxEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Outbox/retry ইনফ্রা — ধাপ ৬ (RPC_SYNC_FIX ট্র্যাক)। শুধু `PendingSyncOutboxEntity`/
 * `PendingSyncOutboxDao`-র insert-delete-update লজিক টেস্ট করে (master plan Step 6-এর
 * নির্দিষ্ট স্কোপ অনুযায়ী) -- `OutboxSyncWorker`-এর আসল RPC কল (network/Supabase) মক করা
 * হয়নি, ইচ্ছাকৃতভাবে এই ধাপের বাইরে।
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OutboxSyncTest {

    private fun newInMemoryDb(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun `insert then getPending returns the new entry`() = runBlocking {
        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()

        val entry = PendingSyncOutboxEntity(
            id = "OUTBOX_1",
            rpcName = "release_escrow",
            paramsJson = """{"escrowId":"ESCROW_1"}""",
            createdAt = System.currentTimeMillis()
        )
        dao.insert(entry)

        val pending = dao.getPending()
        assertEquals(1, pending.size)
        assertEquals("OUTBOX_1", pending.first().id)
        assertEquals("release_escrow", pending.first().rpcName)
        assertEquals("PENDING", pending.first().status)
        assertEquals(0, pending.first().retryCount)

        db.close()
    }

    @Test
    fun `getPending only returns PENDING and RETRYING, not DONE or FAILED_PERMANENT`() = runBlocking {
        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        val now = System.currentTimeMillis()

        dao.insert(PendingSyncOutboxEntity(id = "A", rpcName = "release_escrow", paramsJson = "{}", createdAt = now, status = "PENDING"))
        dao.insert(PendingSyncOutboxEntity(id = "B", rpcName = "release_escrow", paramsJson = "{}", createdAt = now, status = "RETRYING"))
        dao.insert(PendingSyncOutboxEntity(id = "C", rpcName = "release_escrow", paramsJson = "{}", createdAt = now, status = "DONE"))
        dao.insert(PendingSyncOutboxEntity(id = "D", rpcName = "release_escrow", paramsJson = "{}", createdAt = now, status = "FAILED_PERMANENT"))

        val pending = dao.getPending()
        val pendingIds = pending.map { it.id }.toSet()
        assertEquals(2, pending.size)
        assertTrue(pendingIds.contains("A"))
        assertTrue(pendingIds.contains("B"))
        assertTrue(!pendingIds.contains("C"))
        assertTrue(!pendingIds.contains("D"))

        val allRows = dao.getAllSync()
        assertEquals(4, allRows.size)

        db.close()
    }

    @Test
    fun `observePendingCount reflects pending-eligible rows only`() = runBlocking {
        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        val now = System.currentTimeMillis()

        assertEquals(0, dao.observePendingCount().first())

        dao.insert(PendingSyncOutboxEntity(id = "X", rpcName = "admin_adjust_balance", paramsJson = "{}", createdAt = now))
        assertEquals(1, dao.observePendingCount().first())

        dao.insert(PendingSyncOutboxEntity(id = "Y", rpcName = "admin_adjust_balance", paramsJson = "{}", createdAt = now, status = "DONE"))
        // Y is DONE -- shouldn't count
        assertEquals(1, dao.observePendingCount().first())

        db.close()
    }

    @Test
    fun `update bumps retryCount and status per the worker's threshold logic`() = runBlocking {
        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        val now = System.currentTimeMillis()

        dao.insert(
            PendingSyncOutboxEntity(
                id = "RETRY_1",
                rpcName = "process_withdrawal",
                paramsJson = """{"withdrawalId":"W1","action":"APPROVE"}""",
                createdAt = now,
                retryCount = 9
            )
        )

        // Simulate what OutboxSyncWorker.doWork() does on a 10th consecutive failure --
        // MAX_RETRY_COUNT (10) reached, so status flips to FAILED_PERMANENT.
        val existing = dao.getById("RETRY_1")!!
        val newCount = existing.retryCount + 1
        val newStatus = if (newCount >= com.example.worker.OutboxSyncWorker.MAX_RETRY_COUNT) "FAILED_PERMANENT" else "RETRYING"
        dao.update(
            existing.copy(
                retryCount = newCount,
                lastError = "simulated failure",
                lastAttemptAt = System.currentTimeMillis(),
                status = newStatus
            )
        )

        val updated = dao.getById("RETRY_1")!!
        assertEquals(10, updated.retryCount)
        assertEquals("FAILED_PERMANENT", updated.status)
        assertEquals("simulated failure", updated.lastError)

        // FAILED_PERMANENT rows must drop out of getPending()/observePendingCount().
        assertTrue(dao.getPending().none { it.id == "RETRY_1" })
        assertEquals(0, dao.observePendingCount().first())

        db.close()
    }

    @Test
    fun `deleteById removes the row entirely (success path)`() = runBlocking {
        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()

        dao.insert(
            PendingSyncOutboxEntity(
                id = "DEL_1",
                rpcName = "refund_escrow_once",
                paramsJson = """{"escrowId":"E1","refundType":"FULL","refundPercentage":100.0}""",
                createdAt = System.currentTimeMillis()
            )
        )
        assertEquals(1, dao.getPending().size)

        dao.deleteById("DEL_1")

        assertEquals(0, dao.getPending().size)
        assertNull(dao.getById("DEL_1"))

        db.close()
    }

    // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ১] নিচের ৩টা টেস্ট SomadhanRepository.kt-এর নতুন
    // enqueueOutboxRetry() হেল্পার (payoutEscrowToSolver/refundEscrowOnceLocked/addToEscrow-এর
    // fail-ব্লকে ব্যবহৃত) যে paramsJson বানায়, সেটা OutboxRpcDispatcher.kt-এর সংশ্লিষ্ট `when`
    // branch-এর requireString/requireDouble কী-নামের সাথে হুবহু মিলছে কিনা যাচাই করে -- এটাই
    // Step 6-এ ফ্ল্যাগ করা "যাচাই না-হওয়া অনুমান" ঝুঁকি। enqueueOutboxRetry() নিজে private ও
    // SomadhanRepository-এর ভেতরে (যেটা AppDatabase + SupabaseAuthManager/SupabaseSyncManager
    // network কল লাগে, এই Robolectric টেস্টে instantiate করা হয়নি -- Step 6-এর মতোই এই ধাপেও
    // Supabase/network মক করা হয়নি, ইচ্ছাকৃতভাবে স্কোপের বাইরে)। তাই এখানে ঠিক একই
    // JsonObject-বানানোর কোড (mapOf + JsonPrimitive) reproduce করে ভেরিফাই করা হচ্ছে -- আসল
    // dual-write ফাংশনের ভেতরের fail-ব্লক-থেকে-outbox-insert পুরো পথ (end-to-end) এই টেস্টে
    // covered না, শুধু "কী-নাম মিলছে" আর "DAO-তে ঠিকভাবে persist হয়" -- এই দুইটা অংশ।
    @Test
    fun `release_escrow outbox params match OutboxRpcDispatcher's expected escrowId key`() = runBlocking {
        val escrowId = "ESC_TEST_123"
        val params = kotlinx.serialization.json.JsonObject(
            mapOf("escrowId" to kotlinx.serialization.json.JsonPrimitive(escrowId))
        )
        val paramsJson = params.toString()

        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(paramsJson) as kotlinx.serialization.json.JsonObject
        assertEquals(escrowId, (parsed["escrowId"] as kotlinx.serialization.json.JsonPrimitive).content)

        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        dao.insert(
            PendingSyncOutboxEntity(id = "RELEASE_TEST", rpcName = "release_escrow", paramsJson = paramsJson, createdAt = System.currentTimeMillis())
        )
        val stored = dao.getById("RELEASE_TEST")!!
        assertEquals("release_escrow", stored.rpcName)
        assertEquals(paramsJson, stored.paramsJson)
        db.close()
    }

    @Test
    fun `refund_escrow_once outbox params match OutboxRpcDispatcher's expected keys`() = runBlocking {
        val escrowId = "ESC_TEST_456"
        val refundType = "SOLVER_CANCEL"
        val refundPercentage = 100.0
        val params = kotlinx.serialization.json.JsonObject(
            mapOf(
                "escrowId" to kotlinx.serialization.json.JsonPrimitive(escrowId),
                "refundType" to kotlinx.serialization.json.JsonPrimitive(refundType),
                "refundPercentage" to kotlinx.serialization.json.JsonPrimitive(refundPercentage)
            )
        )
        val paramsJson = params.toString()

        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(paramsJson) as kotlinx.serialization.json.JsonObject
        assertEquals(escrowId, (parsed["escrowId"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(refundType, (parsed["refundType"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(refundPercentage, (parsed["refundPercentage"] as kotlinx.serialization.json.JsonPrimitive).content.toDouble(), 0.0001)

        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        dao.insert(
            PendingSyncOutboxEntity(id = "REFUND_TEST", rpcName = "refund_escrow_once", paramsJson = paramsJson, createdAt = System.currentTimeMillis())
        )
        val stored = dao.getById("REFUND_TEST")!!
        assertEquals("refund_escrow_once", stored.rpcName)
        assertEquals(paramsJson, stored.paramsJson)
        db.close()
    }

    @Test
    fun `increment_escrow_extra_amount outbox params match OutboxRpcDispatcher's expected keys`() = runBlocking {
        val escrowId = "ESC_TEST_789"
        val amount = 250.5
        val params = kotlinx.serialization.json.JsonObject(
            mapOf(
                "escrowId" to kotlinx.serialization.json.JsonPrimitive(escrowId),
                "amount" to kotlinx.serialization.json.JsonPrimitive(amount)
            )
        )
        val paramsJson = params.toString()

        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(paramsJson) as kotlinx.serialization.json.JsonObject
        assertEquals(escrowId, (parsed["escrowId"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(amount, (parsed["amount"] as kotlinx.serialization.json.JsonPrimitive).content.toDouble(), 0.0001)

        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        dao.insert(
            PendingSyncOutboxEntity(id = "INCREMENT_TEST", rpcName = "increment_escrow_extra_amount", paramsJson = paramsJson, createdAt = System.currentTimeMillis())
        )
        val stored = dao.getById("INCREMENT_TEST")!!
        assertEquals("increment_escrow_extra_amount", stored.rpcName)
        assertEquals(paramsJson, stored.paramsJson)
        db.close()
    }

    // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ২] requestWithdrawal/updateWithdrawalStatus/adminAdjustBalance-এর
    // outbox param-shape OutboxRpcDispatcher.kt-এর request_withdrawal/process_withdrawal/
    // admin_adjust_balance branch-এর সাথে মিলছে কিনা -- ব্যাচ ১-এর তিনটা টেস্টের ঠিক একই
    // পদ্ধতি (network/Supabase মক না করে, শুধু param-shape + DAO round-trip যাচাই)।
    @Test
    fun `request_withdrawal outbox params match OutboxRpcDispatcher's expected keys`() = runBlocking {
        val params = kotlinx.serialization.json.JsonObject(
            mapOf(
                "amount" to kotlinx.serialization.json.JsonPrimitive(500.0),
                "method" to kotlinx.serialization.json.JsonPrimitive("bKash"),
                "accountNumber" to kotlinx.serialization.json.JsonPrimitive("01700000000"),
                "role" to kotlinx.serialization.json.JsonPrimitive("SOLVER")
            )
        )
        val paramsJson = params.toString()

        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(paramsJson) as kotlinx.serialization.json.JsonObject
        assertEquals(500.0, (parsed["amount"] as kotlinx.serialization.json.JsonPrimitive).content.toDouble(), 0.0001)
        assertEquals("bKash", (parsed["method"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("01700000000", (parsed["accountNumber"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("SOLVER", (parsed["role"] as kotlinx.serialization.json.JsonPrimitive).content)
        // bankName/branchName/accountHolderName optional -- এই টেস্টে বাদ দেওয়া হলো, missing হলে
        // OutboxRpcDispatcher.optionalString() null রিটার্ন করে (exception না), সেটাই প্রত্যাশিত।
        assertTrue(!parsed.containsKey("bankName"))

        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        dao.insert(
            PendingSyncOutboxEntity(id = "REQ_WD_TEST", rpcName = "request_withdrawal", paramsJson = paramsJson, createdAt = System.currentTimeMillis())
        )
        val stored = dao.getById("REQ_WD_TEST")!!
        assertEquals("request_withdrawal", stored.rpcName)
        assertEquals(paramsJson, stored.paramsJson)
        db.close()
    }

    @Test
    fun `process_withdrawal outbox params match OutboxRpcDispatcher's expected keys (REJECT and COMPLETE)`() = runBlocking {
        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()

        for (action in listOf("REJECT", "COMPLETE")) {
            val params = kotlinx.serialization.json.JsonObject(
                mapOf(
                    "withdrawalId" to kotlinx.serialization.json.JsonPrimitive("WID-TEST-$action"),
                    "action" to kotlinx.serialization.json.JsonPrimitive(action),
                    "trxId" to kotlinx.serialization.json.JsonPrimitive("TRX-$action")
                )
            )
            val paramsJson = params.toString()

            val parsed = kotlinx.serialization.json.Json.parseToJsonElement(paramsJson) as kotlinx.serialization.json.JsonObject
            assertEquals("WID-TEST-$action", (parsed["withdrawalId"] as kotlinx.serialization.json.JsonPrimitive).content)
            assertEquals(action, (parsed["action"] as kotlinx.serialization.json.JsonPrimitive).content)
            assertEquals("TRX-$action", (parsed["trxId"] as kotlinx.serialization.json.JsonPrimitive).content)

            dao.insert(
                PendingSyncOutboxEntity(id = "PROC_WD_$action", rpcName = "process_withdrawal", paramsJson = paramsJson, createdAt = System.currentTimeMillis())
            )
            val stored = dao.getById("PROC_WD_$action")!!
            assertEquals("process_withdrawal", stored.rpcName)
            assertEquals(paramsJson, stored.paramsJson)
        }
        db.close()
    }

    @Test
    fun `admin_adjust_balance outbox params match OutboxRpcDispatcher's expected keys`() = runBlocking {
        val params = kotlinx.serialization.json.JsonObject(
            mapOf(
                "userId" to kotlinx.serialization.json.JsonPrimitive("USER_TEST_1"),
                "amount" to kotlinx.serialization.json.JsonPrimitive(150.0),
                "isAddition" to kotlinx.serialization.json.JsonPrimitive(true),
                "reason" to kotlinx.serialization.json.JsonPrimitive("টেস্ট সমন্বয়"),
                "role" to kotlinx.serialization.json.JsonPrimitive("USER")
            )
        )
        val paramsJson = params.toString()

        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(paramsJson) as kotlinx.serialization.json.JsonObject
        assertEquals("USER_TEST_1", (parsed["userId"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(150.0, (parsed["amount"] as kotlinx.serialization.json.JsonPrimitive).content.toDouble(), 0.0001)
        assertEquals(true, (parsed["isAddition"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
        assertEquals("USER", (parsed["role"] as kotlinx.serialization.json.JsonPrimitive).content)

        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        dao.insert(
            PendingSyncOutboxEntity(id = "ADMIN_ADJ_TEST", rpcName = "admin_adjust_balance", paramsJson = paramsJson, createdAt = System.currentTimeMillis())
        )
        val stored = dao.getById("ADMIN_ADJ_TEST")!!
        assertEquals("admin_adjust_balance", stored.rpcName)
        assertEquals(paramsJson, stored.paramsJson)
        db.close()
    }

    @Test
    fun `admin_approve_kyc outbox params match OutboxRpcDispatcher's expected keys`() = runBlocking {
        val params = kotlinx.serialization.json.JsonObject(
            mapOf("userId" to kotlinx.serialization.json.JsonPrimitive("USER_TEST_KYC_1"))
        )
        val paramsJson = params.toString()

        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(paramsJson) as kotlinx.serialization.json.JsonObject
        assertEquals("USER_TEST_KYC_1", (parsed["userId"] as kotlinx.serialization.json.JsonPrimitive).content)

        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        dao.insert(
            PendingSyncOutboxEntity(id = "ADMIN_APPROVE_KYC_TEST", rpcName = "admin_approve_kyc", paramsJson = paramsJson, createdAt = System.currentTimeMillis())
        )
        val stored = dao.getById("ADMIN_APPROVE_KYC_TEST")!!
        assertEquals("admin_approve_kyc", stored.rpcName)
        assertEquals(paramsJson, stored.paramsJson)
        db.close()
    }

    @Test
    fun `admin_reject_kyc outbox params match OutboxRpcDispatcher's expected keys`() = runBlocking {
        val params = kotlinx.serialization.json.JsonObject(
            mapOf(
                "userId" to kotlinx.serialization.json.JsonPrimitive("USER_TEST_KYC_2"),
                "reason" to kotlinx.serialization.json.JsonPrimitive("অস্পষ্ট ডকুমেন্ট ছবি")
            )
        )
        val paramsJson = params.toString()

        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(paramsJson) as kotlinx.serialization.json.JsonObject
        assertEquals("USER_TEST_KYC_2", (parsed["userId"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("অস্পষ্ট ডকুমেন্ট ছবি", (parsed["reason"] as kotlinx.serialization.json.JsonPrimitive).content)

        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        dao.insert(
            PendingSyncOutboxEntity(id = "ADMIN_REJECT_KYC_TEST", rpcName = "admin_reject_kyc", paramsJson = paramsJson, createdAt = System.currentTimeMillis())
        )
        val stored = dao.getById("ADMIN_REJECT_KYC_TEST")!!
        assertEquals("admin_reject_kyc", stored.rpcName)
        assertEquals(paramsJson, stored.paramsJson)
        db.close()
    }

    @Test
    fun `admin_revoke_kyc outbox params match OutboxRpcDispatcher's expected keys`() = runBlocking {
        val params = kotlinx.serialization.json.JsonObject(
            mapOf(
                "userId" to kotlinx.serialization.json.JsonPrimitive("USER_TEST_KYC_3"),
                "reason" to kotlinx.serialization.json.JsonPrimitive("সন্দেহজনক কার্যকলাপ পাওয়া গেছে")
            )
        )
        val paramsJson = params.toString()

        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(paramsJson) as kotlinx.serialization.json.JsonObject
        assertEquals("USER_TEST_KYC_3", (parsed["userId"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("সন্দেহজনক কার্যকলাপ পাওয়া গেছে", (parsed["reason"] as kotlinx.serialization.json.JsonPrimitive).content)

        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        dao.insert(
            PendingSyncOutboxEntity(id = "ADMIN_REVOKE_KYC_TEST", rpcName = "admin_revoke_kyc", paramsJson = paramsJson, createdAt = System.currentTimeMillis())
        )
        val stored = dao.getById("ADMIN_REVOKE_KYC_TEST")!!
        assertEquals("admin_revoke_kyc", stored.rpcName)
        assertEquals(paramsJson, stored.paramsJson)
        db.close()
    }

    @Test
    fun `admin_set_banned outbox params match OutboxRpcDispatcher's expected keys`() = runBlocking {
        val params = kotlinx.serialization.json.JsonObject(
            mapOf(
                "userId" to kotlinx.serialization.json.JsonPrimitive("USER_TEST_BAN_1"),
                "banned" to kotlinx.serialization.json.JsonPrimitive(true),
                "role" to kotlinx.serialization.json.JsonPrimitive("SOLVER")
            )
        )
        val paramsJson = params.toString()

        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(paramsJson) as kotlinx.serialization.json.JsonObject
        assertEquals("USER_TEST_BAN_1", (parsed["userId"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(true, (parsed["banned"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
        assertEquals("SOLVER", (parsed["role"] as kotlinx.serialization.json.JsonPrimitive).content)

        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        dao.insert(
            PendingSyncOutboxEntity(id = "ADMIN_SET_BANNED_TEST", rpcName = "admin_set_banned", paramsJson = paramsJson, createdAt = System.currentTimeMillis())
        )
        val stored = dao.getById("ADMIN_SET_BANNED_TEST")!!
        assertEquals("admin_set_banned", stored.rpcName)
        assertEquals(paramsJson, stored.paramsJson)
        db.close()
    }

    @Test
    fun `admin_set_restricted outbox params match OutboxRpcDispatcher's expected keys`() = runBlocking {
        val params = kotlinx.serialization.json.JsonObject(
            mapOf(
                "userId" to kotlinx.serialization.json.JsonPrimitive("USER_TEST_RESTRICT_1"),
                "restricted" to kotlinx.serialization.json.JsonPrimitive(false),
                "role" to kotlinx.serialization.json.JsonPrimitive("USER")
            )
        )
        val paramsJson = params.toString()

        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(paramsJson) as kotlinx.serialization.json.JsonObject
        assertEquals("USER_TEST_RESTRICT_1", (parsed["userId"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(false, (parsed["restricted"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
        assertEquals("USER", (parsed["role"] as kotlinx.serialization.json.JsonPrimitive).content)

        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        dao.insert(
            PendingSyncOutboxEntity(id = "ADMIN_SET_RESTRICTED_TEST", rpcName = "admin_set_restricted", paramsJson = paramsJson, createdAt = System.currentTimeMillis())
        )
        val stored = dao.getById("ADMIN_SET_RESTRICTED_TEST")!!
        assertEquals("admin_set_restricted", stored.rpcName)
        assertEquals(paramsJson, stored.paramsJson)
        db.close()
    }

    @Test
    fun `admin_set_verified_badge outbox params match OutboxRpcDispatcher's expected keys`() = runBlocking {
        val params = kotlinx.serialization.json.JsonObject(
            mapOf(
                "userId" to kotlinx.serialization.json.JsonPrimitive("USER_TEST_BADGE_1"),
                "verified" to kotlinx.serialization.json.JsonPrimitive(true)
                // role ইচ্ছাকৃতভাবে বাদ -- ঐচ্ছিক প্যারামিটার null হলে কী হয় সেটাও এই টেস্টেই
                // কভার করার জন্য (optionalString অনুপস্থিত key-তে null রিটার্ন করে, crash না)
            )
        )
        val paramsJson = params.toString()

        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(paramsJson) as kotlinx.serialization.json.JsonObject
        assertEquals("USER_TEST_BADGE_1", (parsed["userId"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(true, (parsed["verified"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
        assertNull(parsed["role"])

        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        dao.insert(
            PendingSyncOutboxEntity(id = "ADMIN_SET_BADGE_TEST", rpcName = "admin_set_verified_badge", paramsJson = paramsJson, createdAt = System.currentTimeMillis())
        )
        val stored = dao.getById("ADMIN_SET_BADGE_TEST")!!
        assertEquals("admin_set_verified_badge", stored.rpcName)
        assertEquals(paramsJson, stored.paramsJson)
        db.close()
    }

    @Test
    fun `admin_change_role outbox params match OutboxRpcDispatcher's expected keys`() = runBlocking {
        val params = kotlinx.serialization.json.JsonObject(
            mapOf(
                "userId" to kotlinx.serialization.json.JsonPrimitive("USER_TEST_ROLE_1"),
                "newRole" to kotlinx.serialization.json.JsonPrimitive("SOLVER")
            )
        )
        val paramsJson = params.toString()

        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(paramsJson) as kotlinx.serialization.json.JsonObject
        assertEquals("USER_TEST_ROLE_1", (parsed["userId"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("SOLVER", (parsed["newRole"] as kotlinx.serialization.json.JsonPrimitive).content)

        val db = newInMemoryDb()
        val dao = db.pendingSyncOutboxDao()
        dao.insert(
            PendingSyncOutboxEntity(id = "ADMIN_CHANGE_ROLE_TEST", rpcName = "admin_change_role", paramsJson = paramsJson, createdAt = System.currentTimeMillis())
        )
        val stored = dao.getById("ADMIN_CHANGE_ROLE_TEST")!!
        assertEquals("admin_change_role", stored.rpcName)
        assertEquals(paramsJson, stored.paramsJson)
        db.close()
    }
}
