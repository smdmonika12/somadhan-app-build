@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.data.repository

import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.entity.AdditionalChargeEntity
import com.example.data.entity.AdminAuditLogEntity
import com.example.data.entity.BidEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.EscrowEntity
import com.example.data.entity.FaqEntity
import com.example.data.entity.GatewayPaymentEntity
import com.example.data.entity.MessageEntity
import com.example.data.entity.NotificationEntity
import com.example.data.entity.PlatformSettingEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.RatingEntity
import com.example.data.entity.ReputationEventEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.WithdrawalEntity
import com.example.data.security.PasswordHasher
import com.example.data.remote.SupabaseAuthManager
import com.example.data.remote.SupabaseSyncManager
import com.example.data.remote.SupabaseRealtimeManager
import com.example.data.remote.dto.UserDto
import com.example.data.remote.dto.CategoryDto
import com.example.data.remote.dto.FaqDto
import kotlinx.datetime.Instant
import com.example.data.remote.toUserEntity
import com.example.data.remote.toProblemDto
import com.example.data.remote.toBidDto
import com.example.data.remote.toMessageDto
import com.example.data.remote.toAdditionalChargeEntity
import com.example.util.DistanceUtil
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.util.UUID

// [Somadhan Bug-Fix Step 3 — গ্রুপ ২.২] `updateWithdrawalStatus()`-এর client-side status-guard
// (COMPLETED/REJECTED terminal-state guard, PENDING-only guard) আগে silent no-op ছিল (Unit
// রিটার্ন) — caller (adminUpdateWithdrawalStatus, bulk-approve dialog) সবসময় "সফল" toast দেখাতো,
// এমনকি guard fail হলেও। এই sealed result-এর মাধ্যমে caller এখন real outcome জানতে পারবে।
// (OtpSendResult/OtpVerifyResult-এর একই sealed-result কনভেনশন, util/OtpService.kt)
sealed class WithdrawalUpdateResult {
    object Updated : WithdrawalUpdateResult()
    data class GuardBlocked(val reason: String) : WithdrawalUpdateResult()
}

// [Somadhan Bug-Fix Step 6 — গ্রুপ ৩.১a] `updatePlatformSetting()`-এর cloud dual-write
// (`SupabaseSyncManager.upsertPlatformSetting`) আগে ব্যর্থ হলে শুধু `Log.w` করত, caller (
// `adminUpdatePlatformSetting()`) সবসময় unconditional "সফল" toast দেখাত -- local Room-এ সেভ
// হলেও cloud-এ না গেলে অন্য ডিভাইসে (বা fresh install-এ) সেই মান দেখা যেত না, কিন্তু admin
// কোনো ইঙ্গিত পেতেন না। `WithdrawalUpdateResult`-এর একই sealed-result কনভেনশন অনুসরণ করা হলো
// (Somadhan Bug-Fix Step 3-এর precedent)।
sealed class PlatformSettingUpdateResult {
    object Updated : PlatformSettingUpdateResult()
    data class CloudSyncFailed(val reason: String) : PlatformSettingUpdateResult()
}

class SomadhanRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val categoryDao = db.categoryDao()
    private val problemDao = db.problemDao()
    private val bidDao = db.bidDao()
    private val messageDao = db.messageDao()
    private val ratingDao = db.ratingDao()
    private val notificationDao = db.notificationDao()
    private val withdrawalDao = db.withdrawalDao()
    private val transactionDao = db.transactionDao()
    private val platformSettingDao = db.platformSettingDao()
    private val escrowDao = db.escrowDao()
    private val additionalChargeDao = db.additionalChargeDao()
    private val reputationEventDao = db.reputationEventDao()
    private val adminAuditLogDao = db.adminAuditLogDao()
    private val faqDao = db.faqDao()
    private val gatewayPaymentDao = db.gatewayPaymentDao()

    // [OUTBOX WIRE - ধাপ ৭] Step 6-এ তৈরি হওয়া outbox DAO-র রেফারেন্স -- এই ধাপের আগে কোথাও
    // ব্যবহৃত হতো না (RPC_SYNC_FIX_PROGRESS.md Step 6 দেখুন)। এখানে যোগ করা প্রয়োজনীয় হয়ে পড়ে
    // যাতে নিচের enqueueOutboxRetry() হেল্পার (এবং payoutEscrowToSolver/refundEscrowOnceLocked/
    // addToEscrow-এর fail-ব্লক) এটা ব্যবহার করতে পারে -- বিদ্যমান কোনো DAO field/লাইন বদলায়নি,
    // শুধু একটা নতুন additive লাইন।
    private val pendingSyncOutboxDao = db.pendingSyncOutboxDao()

    val lastDebugInfo = MutableStateFlow<String?>(null)

    // refundEscrowOnce() is invoked from several independent places (solverCancelJob() directly,
    // and reconcileEscrowStates()'s self-healing pass, which itself is triggered from multiple
    // ViewModel call sites). None of those callers coordinate with each other, so two calls for the
    // SAME escrow/refund can genuinely run concurrently as separate coroutines — most commonly when
    // reconcileEscrowStates() fires again (e.g. on a screen navigation) while an earlier refund for
    // the same escrow is still mid-flight (slow network, offline fallback retries, etc). Firestore's
    // transaction guards against a double SERVER-side write, but the local Room "already refunded?"
    // pre-check and the offline-fallback wallet credit are plain read-then-write code with no lock
    // around them, so two concurrent calls can both pass the pre-check before either has committed
    // its write — each one then credits the wallet, producing the double/triple-credit bug. Gating
    // the whole function body on a per-escrow/problem key mutex serializes any concurrent attempts
    // so only the first one actually executes the refund; the rest see its result already committed
    // and correctly no-op.
    private val refundMutexes = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.sync.Mutex>()
    private fun refundMutexFor(key: String): kotlinx.coroutines.sync.Mutex =
        refundMutexes.getOrPut(key) { kotlinx.coroutines.sync.Mutex() }

    // Same per-key-mutex pattern as refundMutexFor() above, for adminResolveDispute() -- see that
    // function's own comment for why a plain "already resolved?" read-then-write check isn't
    // enough on its own to stop a genuine near-simultaneous double-tap.
    private val disputeResolveMutexes = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.sync.Mutex>()
    private fun disputeResolveMutexFor(key: String): kotlinx.coroutines.sync.Mutex =
        disputeResolveMutexes.getOrPut(key) { kotlinx.coroutines.sync.Mutex() }

    // Same per-key-mutex pattern, for adminAdjustBalance() below. Unlike the refund/dispute
    // cases above, a manual admin balance adjustment has no natural stable id of its own (no
    // problemId/escrowId to key off) -- there is no fully deterministic idempotency key
    // possible here without changing the function's callers to pass one in. This mutex plus the
    // short "same adjustment submitted again within a few seconds" check inside the function is
    // a best-effort guard against the common real case (double-tap, an accidental double
    // network send of the same request) -- it is NOT a complete guarantee against every
    // possible duplicate the way TRX_SPLIT_/TRX_REFUND_'s deterministic ids are elsewhere.
    private val adminAdjustMutexes = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.sync.Mutex>()
    private fun adminAdjustMutexFor(key: String): kotlinx.coroutines.sync.Mutex =
        adminAdjustMutexes.getOrPut(key) { kotlinx.coroutines.sync.Mutex() }
    private val recentAdminAdjustments = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()

    data class CommissionBreakdown(
        val rate: Double = 10.0,
        val baseCommission: Double,
        val extraCommission: Double,
        val totalCommission: Double,
        val netAmount: Double,
        val extraCommissionApplied: Boolean,
        val wasFreeQuotaJob: Boolean
    )

    suspend fun calculateCommissionBreakdown(
        problem: ProblemEntity,
        solverId: String,
        baseAmount: Double,
        extraAmount: Double
    ): CommissionBreakdown {
        val normalRate = problem.appliedCommissionRate
            ?: platformSettingDao.getSetting("commission_percent")?.toDoubleOrNull()
            ?: 10.0

        // এই জবটা কি accept করার সময় ফ্রি-কোটা হিসেবে লক হয়েছিল? (appliedCommissionRate == 0.0 মানে ফ্রি ছিল)
        val wasFreeQuotaJob = problem.appliedCommissionRate == 0.0

        val baseCommission = if (wasFreeQuotaJob) 0.0 else Math.round(baseAmount * (normalRate / 100.0)).toDouble()

        val extraToggleOn = platformSettingDao.getSetting("extra_amount_commission_enabled")?.toBooleanStrictOrNull() ?: true
        val discountPercent = platformSettingDao.getSetting("extra_amount_commission_discount_percent")?.toDoubleOrNull()
            ?: platformSettingDao.getSetting("extra_amount_commission_percent")?.toDoubleOrNull()
            ?: 50.0

        // অতিরিক্ত বিলে কমিশনের হিসাব (সংশোধিত ডিসকাউন্ট ফর্মুলা):
        // ১. সাধারণ প্ল্যাটফর্ম কমিশনের হার দিয়ে extra amount-এর স্বাভাবিক কমিশন হিসাব:
        //    val normalExtraCommission = extraAmount * (normalCommissionRate / 100.0)
        // ২. টগল চালু থাকলে সেই স্বাভাবিক কমিশনের উপর discountPercent% ছাড় (যেমন ৫০% ছাড়):
        //    val extraCommission = normalExtraCommission * (1.0 - (discountPercent / 100.0))
        // ৩. টগল বন্ধ থাকলে কোনো ছাড় নেই (পুরো normal rate-এ কমিশন হবে):
        //    val extraCommission = normalExtraCommission
        val normalExtraCommission = if (extraAmount > 0.0) extraAmount * (normalRate / 100.0) else 0.0
        val rawExtraCommission = if (extraAmount <= 0.0) {
            0.0
        } else if (extraToggleOn) {
            val clampedDiscount = discountPercent.coerceIn(0.0, 100.0)
            normalExtraCommission * (1.0 - (clampedDiscount / 100.0))
        } else {
            normalExtraCommission
        }
        val extraCommission = Math.round(rawExtraCommission).toDouble()

        val totalCommission = Math.round(baseCommission + extraCommission).toDouble()
        val grossAmount = Math.round(baseAmount + extraAmount).toDouble()
        val netAmount = Math.round(grossAmount - totalCommission).toDouble()

        return CommissionBreakdown(
            rate = if (wasFreeQuotaJob) 0.0 else normalRate,
            baseCommission = baseCommission,
            extraCommission = extraCommission,
            totalCommission = totalCommission,
            netAmount = netAmount,
            extraCommissionApplied = extraToggleOn && extraAmount > 0.0,
            wasFreeQuotaJob = wasFreeQuotaJob
        )
    }

    suspend fun previewCommissionBreakdown(
        problem: ProblemEntity,
        solverId: String,
        baseAmount: Double,
        extraAmount: Double
    ): CommissionBreakdown = calculateCommissionBreakdown(problem, solverId, baseAmount, extraAmount)

    /**
     * Estimates commission rate for a solver without mutating their free quota counters (Phase M).
     */
    suspend fun estimateCommissionRate(solverId: String): Double {
        val normalRate = platformSettingDao.getSetting("commission_percent")?.toDoubleOrNull() ?: 10.0
        val isFreeQuotaEnabled = platformSettingDao.getSetting("free_quota_enabled")?.let { it != "false" } ?: true
        if (!isFreeQuotaEnabled) return normalRate

        val threshold = platformSettingDao.getSetting("free_quota_reputation_threshold")?.toDoubleOrNull() ?: 80.0
        val quotaLimit = platformSettingDao.getSetting("free_quota_job_count")?.toIntOrNull() ?: 10

        val solver = userDao.getUserById(solverId) ?: return normalRate
        if (solver.reputationScore < threshold) return normalRate

        val currentMonthKey = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
            .format(java.util.Date())

        val storedUsed = if (solver.freeJobsMonthKey == currentMonthKey) solver.freeJobsUsedThisMonth else 0
        val actualMonthlyFreeTrx = transactionDao.getAllTransactionsList().count { trx ->
            trx.solverId == solverId &&
            (trx.wasFreeQuotaJob || (trx.baseCommissionAmount == 0.0 && trx.commissionPercent == 0.0 && trx.grossAmount > 0.0)) &&
            java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date(trx.timestamp)) == currentMonthKey
        }
        val actualActiveFreeProblems = problemDao.getAllProblemsList().count { prob ->
            prob.acceptedSolverId == solverId &&
            prob.status == "IN_PROGRESS" &&
            prob.appliedCommissionRate == 0.0 &&
            java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date(prob.lastActivityAt ?: prob.createdAt)) == currentMonthKey
        }
        val usedCount = maxOf(storedUsed, actualMonthlyFreeTrx + actualActiveFreeProblems)

        return if (usedCount < quotaLimit) 0.0 else normalRate
    }

    /**
     * মানি-ফ্লো ফিক্স, ধাপ ৪ — `problemId` প্যারামিটার যোগ করা হলো যাতে নতুন
     * `sync_solver_free_job_quota` RPC-কে কোন problem-এর প্রেক্ষিতে এই কল হচ্ছে সেটা জানানো
     * যায় (RPC নিজে সার্ভার-সাইডে caller আসলেই ওই problem-এর owner/accepted-solver/admin
     * কিনা যাচাই করে)। কারণ নিচের কমেন্টে বিস্তারিত।
     */
    private suspend fun resolveCommissionRateForNewJob(solverId: String, problemId: String): Double {
        val normalRate = platformSettingDao.getSetting("commission_percent")?.toDoubleOrNull() ?: 10.0
        val isFreeQuotaEnabled = platformSettingDao.getSetting("free_quota_enabled")?.let { it != "false" } ?: true
        if (!isFreeQuotaEnabled) return normalRate

        val threshold = platformSettingDao.getSetting("free_quota_reputation_threshold")?.toDoubleOrNull() ?: 80.0
        val quotaLimit = platformSettingDao.getSetting("free_quota_job_count")?.toIntOrNull() ?: 10

        val solver = userDao.getUserById(solverId) ?: return normalRate
        if (solver.reputationScore < threshold) return normalRate

        val currentMonthKey = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
            .format(java.util.Date())

        val storedUsed = if (solver.freeJobsMonthKey == currentMonthKey) solver.freeJobsUsedThisMonth else 0
        val actualMonthlyFreeTrx = transactionDao.getAllTransactionsList().count { trx ->
            trx.solverId == solverId &&
            (trx.wasFreeQuotaJob || (trx.baseCommissionAmount == 0.0 && trx.commissionPercent == 0.0 && trx.grossAmount > 0.0)) &&
            java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date(trx.timestamp)) == currentMonthKey
        }
        val actualActiveFreeProblems = problemDao.getAllProblemsList().count { prob ->
            prob.acceptedSolverId == solverId &&
            prob.status == "IN_PROGRESS" &&
            prob.appliedCommissionRate == 0.0 &&
            java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date(prob.lastActivityAt ?: prob.createdAt)) == currentMonthKey
        }
        val usedCount = maxOf(storedUsed, actualMonthlyFreeTrx + actualActiveFreeProblems)

        return if (usedCount < quotaLimit) {
            // ফ্রি স্লট ব্যবহার হচ্ছে — কাউন্টার +১ করে সেভ করো
            val updatedUser = solver.copy(
                freeJobsUsedThisMonth = usedCount + 1,
                freeJobsMonthKey = currentMonthKey,
                updatedAt = System.currentTimeMillis()
            )
            userDao.updateUser(updatedUser)
            // মানি-ফ্লো ফিক্স, ধাপ ৪ — আগে এখানে `SupabaseAuthManager.currentUserId() == solverId`
            // guard-সহ `syncFreeJobQuota()` (সরাসরি users.update()) কল হতো। কিন্তু `acceptBid()`
            // থেকে কল হলে caller job **owner** (poster), solver না -- তাই এই guard কখনো true হতো
            // না, sync silently skip হয়ে যেত (RLS-ও নিজের row ছাড়া UPDATE পাস করতো না, তাই guard
            // ছাড়া সরাসরি কল করলেও কোনো লাভ হতো না)। ফলে solver-এর ফ্রি-কোটা কাউন্টার শুধু
            // owner-এর local Room cache-এ থেকে যেত, cloud/অন্য ডিভাইসে কখনো পৌঁছাত না।
            // এখন নতুন SECURITY DEFINER RPC (`sync_solver_free_job_quota`) ব্যবহার করা হচ্ছে,
            // যেটা সার্ভার-সাইডে caller আসলেই এই problem-এর owner/accepted-solver/admin কিনা
            // যাচাই করে তারপর টার্গেট solver-এর কোটা আপডেট করে -- তাই owner বা solver, যে-ই কল
            // করুক, sync কাজ করবে।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.syncSolverFreeJobQuota(problemId, solverId, usedCount + 1, currentMonthKey)
                    .onFailure { e ->
                        Log.w("SomadhanRepo", "resolveCommissionRateForNewJob: free-quota dual-write failed for solver=$solverId problem=$problemId (local flow unaffected): ${e.message}")
                        // [Step 12.8a] outbox retry -- idempotency (migration
                        // step_money_flow_fix4_sync_solver_free_job_quota.sql থেকে যাচাই): RPC কাউন্টার
                        // *সেট* করে (increment না) `p_used_count`-এর absolute মানে -- তাই একই replay
                        // দুইবার চললেও ফলাফল একই থাকে, কোটা দুইবার বাড়ে না। paramsJson keys
                        // "problemId","solverId","usedCount"(Int),"monthKey" -- OutboxRpcDispatcher.kt-এর
                        // "sync_solver_free_job_quota" branch-এর সাথে অক্ষরে অক্ষরে মিলছে।
                        enqueueOutboxRetry(
                            rpcName = "sync_solver_free_job_quota",
                            params = kotlinx.serialization.json.JsonObject(
                                mapOf(
                                    "problemId" to kotlinx.serialization.json.JsonPrimitive(problemId),
                                    "solverId" to kotlinx.serialization.json.JsonPrimitive(solverId),
                                    "usedCount" to kotlinx.serialization.json.JsonPrimitive(usedCount + 1),
                                    "monthKey" to kotlinx.serialization.json.JsonPrimitive(currentMonthKey)
                                )
                            ),
                            error = e
                        )
                    }
            }
            0.0 // কমিশন-ফ্রি
        } else {
            // কোটা শেষ — স্বাভাবিক কমিশন প্রযোজ্য
            if (solver.freeJobsMonthKey != currentMonthKey || solver.freeJobsUsedThisMonth != usedCount) {
                val updatedUser = solver.copy(
                    freeJobsUsedThisMonth = usedCount,
                    freeJobsMonthKey = currentMonthKey,
                    updatedAt = System.currentTimeMillis()
                )
                userDao.updateUser(updatedUser)
                // মানি-ফ্লো ফিক্স, ধাপ ৪ — উপরের একই কারণে এখানেও নতুন RPC ব্যবহার করা হলো।
                if (SupabaseAuthManager.currentUserId() != null) {
                    SupabaseSyncManager.syncSolverFreeJobQuota(problemId, solverId, usedCount, currentMonthKey)
                        .onFailure { e ->
                            Log.w("SomadhanRepo", "resolveCommissionRateForNewJob: free-quota dual-write failed for solver=$solverId problem=$problemId (local flow unaffected): ${e.message}")
                            // [Step 12.8a] outbox retry -- একই idempotency যুক্তি উপরের সাইটের মতো (RPC
                            // absolute value সেট করে, increment না)। paramsJson keys একই চারটা, শুধু
                            // "usedCount" এখানে `usedCount` (বাড়ানো ছাড়া)।
                            enqueueOutboxRetry(
                                rpcName = "sync_solver_free_job_quota",
                                params = kotlinx.serialization.json.JsonObject(
                                    mapOf(
                                        "problemId" to kotlinx.serialization.json.JsonPrimitive(problemId),
                                        "solverId" to kotlinx.serialization.json.JsonPrimitive(solverId),
                                        "usedCount" to kotlinx.serialization.json.JsonPrimitive(usedCount),
                                        "monthKey" to kotlinx.serialization.json.JsonPrimitive(currentMonthKey)
                                    )
                                ),
                                error = e
                            )
                        }
                }
            }
            normalRate
        }
    }

    fun getRecentAuditLogs() = adminAuditLogDao.getRecentLogs()

    suspend fun adminLogChatView(
        problemId: String,
        problemTitle: String,
        details: String = ""
    ) {
        logAdminAction(
            actionType = "VIEW_CHAT",
            targetId = problemId,
            targetName = problemTitle,
            details = details
        )
    }

    suspend fun logAdminCustomAction(
        actionType: String,
        targetId: String,
        targetName: String,
        details: String = ""
    ) {
        logAdminAction(
            actionType = actionType,
            targetId = targetId,
            targetName = targetName,
            details = details
        )
    }

    // Shared mechanical payout, extracted out of confirmReleaseAndComplete()/adminReleaseEscrow()
    // so every path that flips an escrow to RELEASED is forced to go through the same
    // commission-calc + wallet-credit + Transaction(PAYMENT)-insert sequence. Before this existed,
    // reconcileEscrowStates()'s Case-3 self-heal just set escrow.status = "RELEASED" directly --
    // the escrow left HELD forever, the solver's balance was never credited, no Transaction row
    // was created, and the money was effectively lost with no trace (RELEASED being a terminal
    // status, nothing would ever retry it). This helper is the single place that logic lives now.
    //
    // Does NOT touch problem.status, notifications, reputation events, or chat messages --
    // callers remain responsible for whatever side effects are specific to their own flow.
    // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ১] payoutEscrowToSolver/refundEscrowOnceLocked/addToEscrow --
    // এই ৩টা ফাংশনের Supabase dual-write fail-ব্লকে শেয়ার্ড ব্যবহারের জন্য নতুন হেল্পার।
    // RPC_SYNC_FIX_PROGRESS.md Step 5 (ডিজাইন)/Step 6 (ইনফ্রা)/Step 7 (এই এন্ট্রি) দেখুন।
    //
    // শুধু outbox টেবিলে (Step 6-এ তৈরি) একটা নতুন PENDING entry INSERT করে -- OutboxSyncWorker
    // (periodic, ১৫ মিনিট পরপর, SomadhanApp.kt-এ এই ধাপে schedule করা হলো) পরে এটা রিট্রাই
    // করবে। এই হেল্পার নিজে RPC আবার কল করে না, এবং exception ছোড়ে না (নিজের insert ব্যর্থ হলেও
    // শুধু Log.e করে রিটার্ন করে) -- তাই caller-এর existing happy/fail-path আচরণ একবিন্দুও
    // বদলায় না, এটা একটা pure বাড়তি সাইড-ইফেক্ট, ব্লকিং না।
    //
    // paramsJson-এর key-নাম অবশ্যই OutboxRpcDispatcher.kt-এর সংশ্লিষ্ট `when` branch-এর
    // requireString/requireDouble কী-নামের সাথে হুবহু মিলতে হবে (Step 6-এ ফ্ল্যাগ করা
    // "যাচাই না-হওয়া অনুমান" ঝুঁকি) -- call site-গুলোতে (নিচে) এবং
    // OutboxSyncTest.kt-এর নতুন টেস্টে এই মিল যাচাই করা হয়েছে।
    private suspend fun enqueueOutboxRetry(
        rpcName: String,
        params: kotlinx.serialization.json.JsonObject,
        error: Throwable?
    ) {
        try {
            pendingSyncOutboxDao.insert(
                com.example.data.entity.PendingSyncOutboxEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    rpcName = rpcName,
                    paramsJson = params.toString(),
                    createdAt = System.currentTimeMillis(),
                    lastError = error?.message?.take(500)
                )
            )
        } catch (ex: Exception) {
            // outbox insert নিজেই ব্যর্থ হলে (যেমন Room I/O সমস্যা) -- এটাও শুধু log, কারণ এই
            // পুরো মেকানিজমটাই best-effort সেফটি-নেট, caller-কে কখনো crash/fail করানো যাবে না।
            Log.e(
                "SomadhanRepo",
                "enqueueOutboxRetry: failed to insert outbox entry for rpc=$rpcName (retry will be lost; original RPC failure was: ${error?.message}): ${ex.message}"
            )
        }
    }

    // [Step 12.10d] release_escrow RPC-কে **local write-এর আগে** চালানো (RPC-first)।
    // - সফল / non-OK result → শুধু log, caller এগোয় (payoutEscrowToSolver-এ `cloudReleaseHandled = true` পাস হয়)।
    // - সাময়িক ব্যর্থতা (নেটওয়ার্ক ইত্যাদি) → আগের মতোই outbox-এ enqueue, local flow এগোয়।
    // - স্থায়ী প্রত্যাখ্যান (PROBLEM_DISPUTED, NOT_AUTHORIZED...) → EscrowReleaseRejectedException, **কিছু লেখার আগেই**
    //   থামে — তাই local rollback লাগে না (আগে local credit/COMPLETED হয়ে গিয়ে সার্ভারের সাথে অমিল হতো)।
    // session না থাকলে বা escrow না থাকলে কিছু করে না (আগের আচরণ)।
    private suspend fun releaseEscrowCloudFirst(escrow: EscrowEntity?) {
        if (escrow == null || SupabaseAuthManager.currentUserId() == null) return
        SupabaseSyncManager.releaseEscrow(escrow.id)
            .onSuccess { json ->
                val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                    ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                if (resultField != null && resultField != "OK") {
                    Log.w("SomadhanRepo", "releaseEscrowCloudFirst: non-OK result for escrow ${escrow.id}: $resultField (local flow unaffected)")
                }
            }
            .onFailure { e ->
                if (RpcErrorClassifier.isPermanent(e)) {
                    Log.e("SomadhanRepo", "releaseEscrowCloudFirst: server permanently rejected release for escrow ${escrow.id}: ${e.message} -- কোনো local write হয়নি, outbox-এ পাঠানো হয়নি")
                    throw EscrowReleaseRejectedException(RpcErrorClassifier.escrowReleaseUserMessage(e))
                }
                Log.w("SomadhanRepo", "releaseEscrowCloudFirst: Supabase dual-write failed for escrow ${escrow.id} (local flow unaffected): ${e.message}")
                enqueueOutboxRetry(
                    rpcName = "release_escrow",
                    params = kotlinx.serialization.json.JsonObject(
                        mapOf("escrowId" to kotlinx.serialization.json.JsonPrimitive(escrow.id))
                    ),
                    error = e
                )
            }
    }

    // ধাপ ৮ (UI ইন্ডিকেটর) — শুধু read-only observe, outbox টেবিলের কোনো ডেটা এই ফাংশন
    // বদলায় না। PendingSyncOutboxDao.observePendingCount() (Step 6-এ রেডি করা, "UI
    // ইন্ডিকেটরের জন্য" কমেন্ট সহ) সরাসরি এক্সপোজ করা হলো, যাতে ViewModel বিদ্যমান
    // repository.getXxx() → stateIn() কনভেনশন অনুসরণ করে এটা ব্যবহার করতে পারে।
    fun observeOutboxPendingCount(): Flow<Int> = pendingSyncOutboxDao.observePendingCount()

    private suspend fun payoutEscrowToSolver(
        escrow: EscrowEntity?,
        problem: ProblemEntity,
        solverId: String,
        baseAmount: Double,
        extraAmount: Double,
        releaseType: String = "",
        // [Step 12.10d] true হলে caller আগেই releaseEscrowCloudFirst() চালিয়েছে — এখানে RPC আবার কল হয় না।
        cloudReleaseHandled: Boolean = false
    ): CommissionBreakdown {
        val now = System.currentTimeMillis()
        val breakdown = calculateCommissionBreakdown(
            problem = problem,
            solverId = solverId,
            baseAmount = baseAmount,
            extraAmount = extraAmount
        )
        val grossAmount = baseAmount + extraAmount
        val commSetting = if (grossAmount > 0.0) (breakdown.totalCommission / grossAmount) * 100.0 else 0.0

        // Add net earnings to Solver's balance. INSTANT local credit (Room); the matching cloud
        // side goes through the Supabase RPC dual-write below, which enqueues an outbox retry
        // (enqueueOutboxRetry(), replayed by OutboxSyncWorker) on failure instead of the old
        // incrementUserBalance() fire-and-forget call, which had no retry at all if its one
        // attempt failed/timed out. [Step 7.7 cleanup] The TransactionEntity's pendingCloudSync
        // flag set below and FirebaseSyncManager.syncPendingCloudRefunds() referenced by the old
        // version of this comment are both Firebase-era leftovers -- syncPendingCloudRefunds()
        // no longer exists in the codebase and nothing reads pendingCloudSync; the outbox is the
        // real retry mechanism now.
        // [ব্যালেন্স ফিক্স] solver earning তাই balanceSolver mirror-ও একসাথে আপডেট হয়।
        userDao.addBalanceForSolverRole(solverId, breakdown.netAmount, now)

        // Update Escrow
        if (escrow != null) {
            val updatedEscrow = escrow.copy(
                baseAmount = baseAmount,
                extraAmount = extraAmount,
                status = "RELEASED",
                releasedAt = now
            )
            escrowDao.insert(updatedEscrow)

            // [SUPABASE-MIGRATED - ধাপ ৯, guard broadened ধাপ ১২ ব্যাচ ৫] `release_escrow` RPC
            // সোর্স পড়ে যাচাই করা হয়েছে — শুধু problem owner (auth.uid() = escrow.user_id) বা
            // admin কল করতে পারে (solver না)। এই একটাই জায়গা (payoutEscrowToSolver) যেখানে escrow
            // আসলে RELEASED হয়, আর confirmReleaseAndComplete()/adminReleaseEscrow()/
            // reconcileEscrowStates() — সব ক'টা caller-ই এই একই হেল্পার দিয়ে যায়, তাই এখানে একবার
            // dual-write যোগ করলেই সবগুলো path কভার হয়ে যায়। গার্ড আগে শুধু
            // `currentUserId() == escrow.userId` ছিল — এর ফলে adminReleaseEscrow() (caller =
            // admin, owner না) থেকে কখনো এই RPC কল-ই হতো না, যদিও RPC নিজেই admin authorize করে।
            // ধাপ ১২ ব্যাচ ৫-এ গার্ড শিথিল করা হলো (শুধু session আছে কিনা) — RPC নিজেই
            // owner-or-admin authorization চূড়ান্তভাবে যাচাই করে (admin_adjust_balance-এর মতোই
            // established প্যাটার্ন), ব্যর্থ/non-OK হলে শুধু log হয়, local flow অপ্রভাবিত থাকে।
            if (!cloudReleaseHandled && SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.releaseEscrow(escrow.id)
                    .onSuccess { json ->
                        val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                            ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        if (resultField != null && resultField != "OK") {
                            Log.w("SomadhanRepo", "payoutEscrowToSolver: Supabase dual-write non-OK result for escrow ${escrow.id}: $resultField (local flow unaffected)")
                        }
                    }
                    .onFailure { e ->
                        Log.w("SomadhanRepo", "payoutEscrowToSolver: Supabase dual-write failed for escrow ${escrow.id} (local flow unaffected): ${e.message}")
                        // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ১] paramsJson key "escrowId" --
                        // OutboxRpcDispatcher.kt-এর "release_escrow" branch-এর
                        // requireString("escrowId")-এর সাথে মিলছে (কমেন্টে যাচাই করা)।
                        // [Step 12.10d] স্থায়ী প্রত্যাখ্যান (যেমন PROBLEM_DISPUTED) retry-তে পাঠানো হয় না --
                        // পরে dispute মিটলে retry সফল হয়ে সিদ্ধান্তের বিপরীতে টাকা নড়ে যেতে পারত।
                        // (local rollback এখানে ইচ্ছাকৃতভাবে নেই — progress doc-এর backlog দ্রষ্টব্য।)
                        if (RpcErrorClassifier.isPermanent(e)) {
                            Log.e("SomadhanRepo", "payoutEscrowToSolver: server permanently rejected release for escrow ${escrow.id}: ${e.message} -- outbox-এ পাঠানো হয়নি; local state সার্ভারের সাথে অমিল")
                        } else {
                            enqueueOutboxRetry(
                                rpcName = "release_escrow",
                                params = kotlinx.serialization.json.JsonObject(
                                    mapOf("escrowId" to kotlinx.serialization.json.JsonPrimitive(escrow.id))
                                ),
                                error = e
                            )
                        }
                    }
            }
        } else {
            // Defensive fallback: caller passed escrow=null (e.g. confirmReleaseAndComplete()
            // found no escrow row for this problem via escrowDao.getByProblemId()). The old
            // local releaseEscrow(problemId): EscrowEntity? helper this used to call was removed
            // as dead code in an earlier pass, but this call site was missed -- there's no local
            // escrow row to mark RELEASED here, so just log it loudly instead of crashing the
            // build/payout; the solver's balance was already credited above.
            Log.e(
                "SomadhanRepo",
                "payoutEscrowToSolver: called with escrow=null for problem ${problem.id} -- no local escrow row found to release (solver $solverId was still credited ৳${baseAmount + extraAmount})."
            )
        }

        // Insert Transaction Record. Deterministic id (keyed on the escrow's own id, falling back
        // to problem.id when there's no escrow row) instead of the previous random UUID -- this
        // was the one remaining payout-record id in the file that wasn't collision-proof. All
        // three callers (confirmReleaseAndComplete(), adminReleaseEscrow(),
        // reconcileEscrowStates()) already guard against invoking this function twice for the
        // same escrow via their own escrow-status checks, so this is defense-in-depth rather than
        // a fix for an observed bug -- but it also means reconcileEscrowStates() re-running this
        // function for an escrow that was already paid out will now update the SAME transaction
        // record instead of minting a second one, matching the TRX_SPLIT_/TRX_REFUND_ convention
        // used everywhere else balance-affecting transactions are created.
        val trx = TransactionEntity(
            id = "TRX_RELEASE_${escrow?.id ?: problem.id}",
            problemId = escrow?.problemId ?: problem.id,
            problemTitle = escrow?.problemTitle ?: problem.title,
            userId = escrow?.userId ?: problem.userId,
            solverId = solverId,
            grossAmount = grossAmount,
            commissionPercent = commSetting,
            commissionAmount = breakdown.totalCommission,
            netAmount = breakdown.netAmount,
            timestamp = now,
            baseAmount = baseAmount,
            extraAmount = extraAmount,
            baseCommissionAmount = breakdown.baseCommission,
            extraCommissionAmount = breakdown.extraCommission,
            extraCommissionApplied = breakdown.extraCommissionApplied,
            wasFreeQuotaJob = breakdown.wasFreeQuotaJob,
            escrowId = escrow?.id ?: "ESC_${problem.id.take(8).uppercase()}",
            releaseType = releaseType,
            pendingCloudSync = true,
            role = "SOLVER"
        )
        transactionDao.insertTransaction(trx)
        // [Step 7.7 cleanup] pendingCloudSync above is a Firebase-era leftover, not read by
        // anything; the actual (retrying) cloud balance increment for this payout is the
        // Supabase RPC dual-write + outbox retry, see the comment above this function's body.

        return breakdown
    }

    private suspend fun logAdminAction(
        actionType: String,
        targetId: String,
        targetName: String,
        details: String = "",
        // [ROLE_SEPARATION ধাপ ৬, অংশ খ] caller-এর কাছে role ইতিমধ্যে জানা থাকলে পাস করবে
        // ("USER"/"SOLVER"), role-নিরপেক্ষ action-এ ডিফল্ট "" (পুরনো caller ভাঙবে না)।
        // [cloud-sync role গ্যাপ ফিক্স] Supabase-এ admin_audit_logs.role কলাম ও
        // log_admin_action-এর p_role overload আগে থেকেই ছিল -- শুধু এই Kotlin wrapper role
        // পাস করছিল না। এখন নিচে dual-write-এও পাস করা হচ্ছে।
        role: String = ""
    ) {
        try {
            val log = AdminAuditLogEntity(
                id = "LOG_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                actionType = actionType,
                targetId = targetId,
                targetName = targetName,
                details = details,
                timestamp = System.currentTimeMillis(),
                role = role
            )
            adminAuditLogDao.insertLog(log)
            // Bug fix: this used to only write to the LOCAL device's Room database. This function
            // can be triggered from any regular user/solver's phone (e.g.
            // reconcileEscrowStates()'s self-heal alert, fired from a pull-to-refresh/wallet
            // refresh on a normal session) -- a log written only to that device's local Room would
            // never reach the admin's own phone. Pushing to Firestore here closes that gap; the
            // admin side picks it up via pullAdminAuditLogs() (on-demand, see its own doc comment
            // for why not a live listener).

            // [SUPABASE-MIGRATED - ধাপ ১২] `log_admin_action` RPC guard শুধু "session আছে কিনা"
            // (RPC নিজে `is_admin` চেক করে না, কারণ এই ফাংশনটা normal user/solver session থেকেও
            // ট্রিগার হতে পারে -- উপরের কমেন্টে ব্যাখ্যা করা একই কারণে)। ব্যর্থ হলেও local Room
            // audit log flow সম্পূর্ণ অপ্রভাবিত।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.logAdminAction(actionType, targetId, targetName, details, role)
                    .onFailure { e ->
                        Log.w("SomadhanRepo", "logAdminAction: Supabase dual-write failed for $actionType/$targetId (local flow unaffected): ${e.message}")
                    }
            }
        } catch (_: Exception) {
            // Fail-safe
        }
    }

    init {
        // Attach the Room database so all remote Firestore collections (users, posts, bids,
        // withdrawals, categories, ratings, messages) are automatically synced and cached into Room in real time.
    }

    // ---------------- AUTH & USERS ----------------

    suspend fun getUserByPhone(phone: String): UserEntity? = userDao.getUserByPhone(phone)

    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email)

    suspend fun getUserByPhoneOrEmail(phone: String, email: String): UserEntity? = userDao.getUserByPhoneOrEmail(phone, email)

    suspend fun getUserById(id: String): UserEntity? = userDao.getUserById(id)

    fun getUserByIdFlow(id: String): Flow<UserEntity?> = userDao.getUserByIdFlow(id)

    // Caches a user profile fetched from Firestore into the local Room
    // database — used when logging in on a device that doesn't already
    // have this account locally (see cross-device login in the ViewModel).
    suspend fun cacheUserLocally(user: UserEntity) {
        val existing = userDao.getUserById(user.id)
        var userToCache = if (existing != null && user.password.isBlank() && existing.password.isNotBlank()) {
            user.copy(password = existing.password)
        } else user
        // ধাপ ১৪.৫ (গ): legacyDualRoleMergeDoneAt সম্পূর্ণ local/device-only ফ্ল্যাগ (Supabase-এ
        // সমতুল্য কলাম নেই), তাই cloud থেকে map করা `user`-এ এটা সবসময় ডিফল্ট 0L থাকে -- password
        // preserve করার মতোই বিদ্যমান local row থেকে এই মান ধরে রাখা হলো, নাহলে প্রতিবার cloud
        // refresh-এ এই guard রিসেট হয়ে যেত আর mergeLegacyDualRoleDataFromCloud() বারবার চলত।
        if (existing != null && existing.legacyDualRoleMergeDoneAt > 0L) {
            userToCache = userToCache.copy(legacyDualRoleMergeDoneAt = existing.legacyDualRoleMergeDoneAt)
        }
        // [ROLE_UID ফিক্স - ধাপ ৩] localDualRowArchived-ও ঠিক একই কারণে preserve করা হচ্ছে —
        // এটাও local/device-only ফ্ল্যাগ, cloud থেকে map হওয়া `user`-এ সবসময় ডিফল্ট false
        // থাকে। preserve না করলে cloud refresh-এ archived মার্কার রিসেট হয়ে migration আবার
        // একই row প্রসেস করত।
        if (existing != null && existing.localDualRowArchived) {
            userToCache = userToCache.copy(localDualRowArchived = true)
        }
        userDao.insertUser(userToCache)
    }

    /**
     * ধাপ ১৪: এখন প্রথমে Supabase থেকে fetch করার চেষ্টা করে (নতুন migrate হওয়া account-দের
     * জন্য id সরাসরি Supabase Auth UUID)। Supabase থেকে না পেলে (যেমন এখনো migrate না হওয়া
     * demo/admin account, যাদের id "ADMIN_SYSTEM"-এর মতো non-UUID স্ট্রিং — এই id দিয়ে Supabase
     * query করলে স্বাভাবিকভাবেই কিছু ফেরত আসবে না, ব্যতিক্রম না) পুরনো Firebase fallback-এ যায়,
     * যাতে সেই account গুলোর জন্য এই ফাংশন এখনো আগের মতোই কাজ করে (rule #2)।
     */
    suspend fun refreshUserDataFromCloud(userId: String): UserEntity? {
        if (userId.isBlank()) return null

        val supabaseUser = SupabaseSyncManager.getUserById(userId).getOrNull()
        if (supabaseUser != null) {
            val existingLocal = userDao.getUserById(userId)
            val needsLegacyMerge = (existingLocal?.legacyDualRoleMergeDoneAt ?: 0L) == 0L
            // [Somadhan Bug-Fix — গ্রুপ ১, নতুন সাব-বাগ ১.৩] account-switch balance-reset-এর
            // দ্বিতীয় root cause: escrow/wallet dual-write RPC (addToEscrow/
            // payoutEscrowToSolver/refundEscrowOnceLocked) cloud-এ push ব্যর্থ হলে
            // enqueueOutboxRetry() একটা PENDING outbox entry বানায় (OutboxSyncWorker পরে
            // retry করে) -- অর্থাৎ সেই মুহূর্তে Supabase-এর balance/status এখনো stale, এই
            // device-এর local মানই তখন authoritative। আগে এখানে unconditionally cloud→local
            // overwrite হতো, তাই ঠিক এই sync-pending সময়েই logout/login (account-switch)
            // করলে local (সঠিক) balance/reputationScore/isBanned/isRestricted cloud-এর
            // পুরনো মান দিয়ে চাপা পড়ে যেত -- ইউজার-রিপোর্টেড "balance reset" বাগ।
            // ফিক্স: pending outbox entry থাকলে শুধু এই money/status-critical "active"
            // ফিল্ডগুলো local-ই রাখা হচ্ছে (bug ১.১-এর মতোই, খুব সংক্ষিপ্ত window-এ প্রযোজ্য);
            // বাকি সব (profile/KYC/categories ইত্যাদি non-money) ফিল্ড আগের মতোই cloud থেকে
            // আপডেট হবে -- legacy dual-role merge (নিচে) এই গার্ডের বাইরে, আলাদা এককালীন
            // per-device migration, রুল #৩ অনুযায়ী স্কোপের বাইরে রাখা হলো।
            val hasPendingOutboxSync = runCatching { pendingSyncOutboxDao.getPending() }
                .getOrDefault(emptyList())
                .isNotEmpty()
            var mappedUser = supabaseUser.toUserEntity(existingPassword = existingLocal?.password ?: "")
            if (hasPendingOutboxSync && existingLocal != null) {
                // [সাব-বাগ ১.৫-এর সময় ধরা পড়েছে] role-scoped mirror কলামগুলো
                // (balanceUser/balanceSolver/reputationScoreUser/_Solver/isBannedUser/_Solver/
                // isRestrictedUser/_Solver) আগে এই গার্ডের বাইরে ছিল -- শুধু legacy plain
                // ফিল্ডই protect হতো, মিরর কলাম তখনও stale cloud মান নিয়ে নিত। এটা exactly সেই
                // mismatch তৈরি করত যা mergeAndSaveUser()-এর ক্লাস-ডক (SupabaseRealtimeManager.kt)
                // এর ২ নং পয়েন্টে সতর্ক করেছিল। এখন legacy + mirror দুটোই একসাথে protect করা হচ্ছে।
                mappedUser = mappedUser.copy(
                    balance = existingLocal.balance,
                    reputationScore = existingLocal.reputationScore,
                    isBanned = existingLocal.isBanned,
                    isRestricted = existingLocal.isRestricted,
                    balanceUser = existingLocal.balanceUser,
                    balanceSolver = existingLocal.balanceSolver,
                    reputationScoreUser = existingLocal.reputationScoreUser,
                    reputationScoreSolver = existingLocal.reputationScoreSolver,
                    isBannedUser = existingLocal.isBannedUser,
                    isBannedSolver = existingLocal.isBannedSolver,
                    isRestrictedUser = existingLocal.isRestrictedUser,
                    isRestrictedSolver = existingLocal.isRestrictedSolver
                )
                Log.w(
                    "SomadhanRepo",
                    "refreshUserDataFromCloud: pending outbox sync exists for device -- keeping local balance/status (legacy + role-scoped) for user $userId instead of stale cloud value"
                )
            }
            cacheUserLocally(mappedUser)
            val cachedRoot = userDao.getUserById(userId)
            // ধাপ ১৪.৫ (গ): এই root row-টা একটা real Supabase session-ওয়ালা account (তবেই এই
            // if-ব্লকে ঢুকেছে) কিনা যাচাই করার দরকার নেই -- getUserById(userId) এই root uid
            // দিয়েই কল হয়েছে, তাই supabaseUser != null মানেই এটা root। প্রতি root account-এ
            // একবারই (per device) এই merge চালানো হয় -- guard: legacyDualRoleMergeDoneAt।
            if (cachedRoot != null && needsLegacyMerge) {
                mergeLegacyDualRoleDataFromCloud(cachedRoot, supabaseUser)
            }
            return userDao.getUserById(userId)
        }

        // supabaseUser was null above (Supabase fetch didn't find this id -- e.g. a
        // not-yet-migrated demo/admin account like "ADMIN_SYSTEM"). The old Firebase-fallback
        // path that used to live here ("cloudUser") is gone now that Firebase has been fully
        // migrated off of; whatever's already cached locally is the best we can do.
        return userDao.getUserById(userId)
    }

    /**
     * ধাপ ১৪.৫ (গ) — Legacy dual-row device-data migrate/merge।
     *
     * প্রেক্ষাপট: ধাপ ১৪.৫খ-এর আগে, role switch করলে এই ডিভাইসে প্রতিটা role-এর জন্য একটা
     * সম্পূর্ণ আলাদা local `UserEntity` row তৈরি হতো (`SOLVER_xxxxxxxx`/`USER_xxxxxxxx` id,
     * `linkedAccountId` দিয়ে root-এর সাথে যুক্ত) -- আর সেই row-গুলোর নিজস্ব `balance`/
     * `reputationScore`/`isBanned`/`isRestricted` (plain/"active" কলাম) স্বাধীনভাবে বাড়তো-কমতো,
     * কারণ তখন Supabase-এ role-scoped কলামই ছিল না। এই dual-row model-এর কোনো row-এরই নিজস্ব
     * Supabase Auth session নেই (শুধু root-এর আছে, `id == auth.uid()`), তাই এই linked
     * row-গুলোর ডেটা কখনো Supabase-এ যায়নি।
     *
     * ধাপ ১৪.৫ক-খ-এ Supabase root row-তে role-scoped কলাম (`balance_user`/`balance_solver`
     * ইত্যাদি) যোগ হওয়ার পর থেকে সেগুলোই দুই role-এর জন্য একমাত্র cloud source-of-truth --
     * কিন্তু এই ডিভাইসের পুরনো local linked row-গুলো তখনও তাদের প্রাক-redesign plain মান নিয়েই
     * বসে আছে, cloud-এর সাথে সামঞ্জস্যহীন হতে পারে। এই ফাংশন **প্রতি root account-এ একবারই**
     * (per device, `UserEntity.legacyDualRoleMergeDoneAt` guard দিয়ে -- caller
     * [refreshUserDataFromCloud] দেখুন) cloud role-scoped কলামগুলোকে জয়ী ধরে local dual-row
     * ডেটা ওভাররাইট/মার্জ করে:
     * - root row-এর নিজের role-এর জন্য "active" plain ফিল্ড (balance/reputationScore/
     *   isBanned/isRestricted) cloud-এর সেই role-এর role-scoped মান দিয়ে বসানো হয় (এতদিন
     *   toUserEntity() শুধু role-scoped কলাম pass-through করত, plain "active" কলাম না --
     *   এখানেই প্রথম সেই gap বন্ধ করা হলো)।
     * - এই root-এর সাথে `linkedAccountId` দিয়ে যুক্ত পুরনো local linked row (অন্য role-এর
     *   dual-row entity) থাকলে, সেটার plain "active" ফিল্ডও ওই role-এর cloud role-scoped
     *   মান দিয়ে ওভাররাইট করা হয়, আর তার নিজের role-scoped কলামগুলোও (পরবর্তী switchRole()
     *   কলে root-এর সাথে মেলার জন্য) cloud-এর সাথে sync করে দেওয়া হয়।
     * - রুট বা linked -- কোনো row-ই নতুন করে তৈরি হয় না, শুধু বিদ্যমান row আপডেট হয় (rule #২)।
     *
     * প্রতিটা local আপডেটের সাথে established dual-write প্যাটার্ন অনুযায়ী
     * `FirebaseSyncManager.syncUser()`ও কল করা হয়েছে, যাতে Firebase (এখনও এই ডেটার জন্য
     * চালু/live path) পরবর্তী listener/pull-এ এই সংশোধিত মান overwrite করে না ফেলে।
     *
     * ব্যর্থ হলে শুধু log হয়, `legacyDualRoleMergeDoneAt` guard **সেট করা হয় না** -- যাতে
     * পরের সফল login-এ আবার চেষ্টা হয় (money-adjacent ডেটা, তাই আংশিক/ব্যর্থ merge-কে
     * "সম্পন্ন" হিসেবে চিহ্নিত করা হয় না)।
     */
    private suspend fun mergeLegacyDualRoleDataFromCloud(rootLocal: UserEntity, cloudDto: UserDto) {
        try {
            fun roleScoped(role: String): Triple<Double, Double, Pair<Boolean, Boolean>> = when (role) {
                "SOLVER" -> Triple(
                    cloudDto.balanceSolver,
                    cloudDto.reputationScoreSolver,
                    cloudDto.isBannedSolver to cloudDto.isRestrictedSolver
                )
                else -> Triple(
                    cloudDto.balanceUser,
                    cloudDto.reputationScoreUser,
                    cloudDto.isBannedUser to cloudDto.isRestrictedUser
                )
            }

            // ১) root row নিজের role অনুযায়ী "active" plain ফিল্ড cloud role-scoped মান দিয়ে
            var updatedRoot = rootLocal
            val (rootBal, rootRep, rootBanRestrict) = roleScoped(rootLocal.role)
            if (updatedRoot.balance != rootBal || updatedRoot.reputationScore != rootRep ||
                updatedRoot.isBanned != rootBanRestrict.first || updatedRoot.isRestricted != rootBanRestrict.second
            ) {
                updatedRoot = updatedRoot.copy(
                    balance = rootBal,
                    reputationScore = rootRep,
                    isBanned = rootBanRestrict.first,
                    isRestricted = rootBanRestrict.second,
                    updatedAt = System.currentTimeMillis()
                )
            }

            // ২) এই root-এর সাথে যুক্ত পুরনো local linked (dual-row) entity, যদি এই ডিভাইসে থাকে
            val linkedRows = userDao.getLinkedAccounts(rootLocal.id).filter { it.id != rootLocal.id }
            val updatedLinkedRows = mutableListOf<UserEntity>()
            for (linked in linkedRows) {
                val (linkedBal, linkedRep, linkedBanRestrict) = roleScoped(linked.role)
                updatedLinkedRows += linked.copy(
                    balance = linkedBal,
                    reputationScore = linkedRep,
                    isBanned = linkedBanRestrict.first,
                    isRestricted = linkedBanRestrict.second,
                    balanceUser = cloudDto.balanceUser,
                    balanceSolver = cloudDto.balanceSolver,
                    reputationScoreUser = cloudDto.reputationScoreUser,
                    reputationScoreSolver = cloudDto.reputationScoreSolver,
                    isBannedUser = cloudDto.isBannedUser,
                    isBannedSolver = cloudDto.isBannedSolver,
                    isRestrictedUser = cloudDto.isRestrictedUser,
                    isRestrictedSolver = cloudDto.isRestrictedSolver,
                    updatedAt = System.currentTimeMillis()
                )
            }

            // ৩) সবকিছু ঠিকঠাক হিসেব হওয়ার পরই local DB + Firebase-এ লেখা হচ্ছে (guard flag
            // সবার শেষে, root-এর উপর) -- যাতে মাঝপথে ব্যতিক্রম ঘটলে আংশিক merge "সম্পন্ন"
            // হিসেবে চিহ্নিত না হয়।
            updatedRoot = updatedRoot.copy(legacyDualRoleMergeDoneAt = System.currentTimeMillis())
            userDao.updateUser(updatedRoot)
            // [ধাপ ৩৩.১] আগে এখানে FirebaseSyncManager.syncUser(updatedRoot) কল ছিল -- এখন
            // ইচ্ছাকৃতভাবে সরানো হলো, নতুন কোনো Supabase RPC না বানিয়েই, কারণ যাচাই করে দেখা
            // গেছে এটা আসলে কোনো gap ছিল না: `cloudDto` (এই ফাংশনের প্যারামিটার) নিজেই
            // Supabase থেকে আসে (caller `refreshUserDataFromCloud()` দেখুন,
            // `SupabaseSyncManager.getUserById()`), তাই এখানে যা রিক্যালকুলেট হচ্ছে তা আগে
            // থেকেই Supabase-এ আছে -- ফিরিয়ে লেখা নিছক no-op/circular হতো। Firebase sync-টা
            // শুধু পুরনো Firestore listener (handleUsersSnapshot) যেন পরের pull-এ এই সংশোধিত
            // মান overwrite করে না ফেলে সেটা ঠেকানোর জন্য ছিল -- এই ধাপেই (৩৩) সেই listener/
            // FirebaseSyncManager সম্পূর্ণ মুছে যাচ্ছে বলে সেই ঝুঁকিও আর নেই।
            for (updatedLinked in updatedLinkedRows) {
                userDao.updateUser(updatedLinked)
                // [ধাপ ৩১ ক] switchRole()-এর মতোই কারণে redundant linked-row Firebase sync
                // সরানো হলো -- root row-এর sync (নিচে, updatedRoot-এর জন্য) যথেষ্ট।
            }
            Log.d(
                "SomadhanRepo",
                "mergeLegacyDualRoleDataFromCloud: root=${rootLocal.id} সম্পন্ন, " +
                    "${updatedLinkedRows.size}টা linked row মার্জ হলো"
            )
        } catch (e: Exception) {
            Log.w(
                "SomadhanRepo",
                "mergeLegacyDualRoleDataFromCloud: root=${rootLocal.id}-এর জন্য ব্যর্থ (পরের " +
                    "login-এ আবার চেষ্টা হবে, guard সেট হয়নি): ${e.message}"
            )
        }
    }

    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()
    suspend fun getAllUsersPage(limit: Int, offset: Int): List<UserEntity> =
        userDao.getAllUsersPage(limit, offset)
    suspend fun getUsersByRolePage(role: String, limit: Int, offset: Int): List<UserEntity> =
        userDao.getUsersByRolePage(role, limit, offset)

    /**
     * ধাপ ১৪: নিজের row-এর জন্য (user.id == বর্তমান Supabase session-এর uid) non-sensitive
     * profile ফিল্ডগুলো Supabase-এও sync করে (`SupabaseSyncManager.updateOwnProfile`)। linked
     * account sync loop (নিচে) **Supabase-এ migrate করা হয়নি** — কারণ Supabase RLS-এ users
     * টেবিলে কোনো INSERT policy নেই (শুধু `handle_new_auth_user` trigger দিয়ে row তৈরি হয়), আর
     * linked account গুলো ভিন্ন id-এর (অন্য auth.uid()) row — বর্তমান session দিয়ে সেগুলো লেখার
     * অনুমতিই নেই। এটা মূলত `switchRole()`-এর dual-row মডেলের সাথে জড়িত একই architecture সমস্যা
     * (নিচে `switchRole` এর কমেন্ট, আর `MIGRATION_PROGRESS.md` দেখুন) — এই ধাপে সমাধান করা হয়নি,
     * পরের ধাপের জন্য flag করা হলো। Firebase sync (নিচে) অপরিবর্তিত রাখা হয়েছে (bridge, rule #2)।
     */
    suspend fun updateUser(user: UserEntity, syncLinkedProfiles: Boolean = true) {
        val updated = user.copy(updatedAt = System.currentTimeMillis())
        userDao.updateUser(updated)

        if (SupabaseAuthManager.currentUserId() == user.id) {
            SupabaseSyncManager.updateOwnProfile(
                userId = user.id,
                name = updated.name,
                address = updated.address,
                latitude = updated.latitude,
                longitude = updated.longitude,
                profileImageUri = updated.profileImageUri,
                solverCategories = updated.solverCategories,
                favoriteSolverIds = updated.favoriteSolverIds,
                hasCompletedSolverSetup = updated.hasCompletedSolverSetup,
                email = updated.email.ifBlank { null }
            )
        }

        // Keep linked account (User <-> Solver) basic profile info & photo synchronized
        if (syncLinkedProfiles) {
            val rootId = user.linkedAccountId?.takeIf { it.isNotBlank() } ?: user.id
            val linkedList = userDao.getLinkedAccounts(rootId).filter { it.id != user.id }
            val samePhoneEmailList = userDao.getAllUsersList().filter { 
                it.id != user.id && (it.phone == user.phone || (it.email.isNotBlank() && it.email == user.email)) 
            }
            val allLinkedToSync = (linkedList + samePhoneEmailList).distinctBy { it.id }

            for (linked in allLinkedToSync) {
                val syncedLinked = linked.copy(
                    name = user.name,
                    displayUid = if (user.displayUid.isNotBlank()) user.displayUid else linked.displayUid,
                    phone = user.phone,
                    email = user.email,
                    address = user.address,
                    latitude = user.latitude,
                    longitude = user.longitude,
                    profileImageUri = user.profileImageUri,
                    password = user.password,
                    isVerifiedBadge = user.isVerifiedBadge,
                    linkedAccountId = rootId,
                    updatedAt = System.currentTimeMillis()
                )
                userDao.updateUser(syncedLinked)

                // ধাপ ৩২.৫ — Firebase কলের ঠিক পাশে (dual-run): DB-তে verify করে দেখা গেছে RLS
                // linked family-র মধ্যে UPDATE আসলে অনুমোদিত (`sync_linked_account_profile` RPC,
                // caller নিজেই authorization চেক করে) — তাই শুধু caller নিজে যে account থেকে এই
                // updateUser() কল করছে তার জন্যই এই কল করা হচ্ছে (guard: নিজের session আছে কিনা)।
                if (SupabaseAuthManager.currentUserId() == user.id) {
                    SupabaseSyncManager.syncLinkedAccountProfile(
                        targetUserId = linked.id,
                        name = user.name,
                        phone = user.phone,
                        email = user.email.ifBlank { null },
                        address = user.address,
                        latitude = user.latitude,
                        longitude = user.longitude,
                        profileImageUri = user.profileImageUri,
                        isVerifiedBadge = user.isVerifiedBadge
                    )
                }
            }
        }
    }

    /**
     * ধাপ ১৪: এখন `public.users`-এ password column নেই — password পুরোপুরি Supabase Auth
     * (`auth.users`) সামলায়। তাই বর্তমান user-এর id-ই যদি বর্তমান Supabase session-এর সাথে মেলে,
     * `SupabaseAuthManager.updatePassword` কল করা হয় (real password change)। session না থাকলে
     * (এখনো migrate-না-হওয়া demo/legacy account) পুরনো local bcrypt পথে fallback করে, যাতে rule
     * #2 অনুযায়ী সেই account গুলোর জন্য password-change ফিচার ভেঙে না যায়।
     */
    suspend fun updateUserPassword(userId: String, newPass: String) {
        val user = userDao.getUserById(userId) ?: return
        if (SupabaseAuthManager.currentUserId() == userId) {
            val result = SupabaseAuthManager.updatePassword(newPass)
            if (result.isSuccess) {
                // Supabase-এ password পরিবর্তন সফল — local UserEntity.password field আর source of
                // truth না, তাই এখানে blank রাখা হচ্ছে (touchOnly updatedAt-এর জন্য updateUser কল)।
                updateUser(user.copy(password = ""))
                return
            }
            // Supabase কল ব্যর্থ হলে নিচের legacy local path fallback হিসেবে চলবে।
        }
        updateUser(user.copy(password = PasswordHasher.hash(newPass)))
    }

    // One-time, transparent migration for accounts created before password
    // hashing was introduced. Called right after a successful legacy
    // (plaintext) login so the stored password becomes a bcrypt hash going
    // forward, locally and in Firestore. The user never notices this happen.
    suspend fun migrateLegacyPlaintextPassword(user: UserEntity, plainPassword: String) {
        updateUser(user.copy(password = PasswordHasher.hash(plainPassword)))
    }

    suspend fun hasExistingSolverProfile(user: UserEntity): Boolean {
        // [বাগফিক্স, ব্যবহারকারীর রিপোর্ট: "প্রথমবার ক্যাটাগরি সিলেক্ট করে Solver-এ সুইচ করা
        // স্বাভাবিক, কিন্তু বারবার ক্যাটাগরি সিলেক্ট অপশন আসছে — এটা আমার অ্যাপের প্যাটার্ন না"]
        // আগে এখানে `user.role == "SOLVER"` চেক করা হতো — কিন্তু এই ফাংশনটা কল হয় ঠিক তখনই যখন
        // ব্যবহারকারী এখনো USER role-এ আছে এবং Solver-এ সুইচ করতে চাইছে (দেখুন
        // SomadhanViewModel.requestSwitchRole())। অর্থাৎ কলের সময় `user.role` সবসময় "USER"
        // থাকে, "SOLVER" কখনোই না — তাই এই শর্তটা সবসময় false হয়ে সরাসরি নিচের legacy
        // dual-row fallback-এ চলে যেত। single-row আর্কিটেকচারে (switchRoleInPlace(), দেখুন
        // ROLE_UID ফিক্স) আলাদা কোনো "SOLVER_xxxx" linked row আর তৈরি হয় না — root row-এই
        // `hasCompletedSolverSetup`/`solverCategories` স্থায়ীভাবে সেভ থাকে (role পরে USER-এ
        // ফিরে গেলেও এই দুটো ফিল্ড রিসেট হয় না)। ফলে সেই legacy fallback কখনো কিছু খুঁজে
        // পেত না, আর প্রতিবার Solver-এ সুইচ করতে গেলে প্রথমবারের মতোই ক্যাটাগরি-পিকার দেখাত।
        // ফিক্স: role নির্বিশেষে root row-এর নিজের ফিল্ড দুটোই চেক করা হচ্ছে (এটাই আসল
        // single-row source of truth)।
        if (user.hasCompletedSolverSetup && user.solverCategories.isNotBlank()) {
            return true
        }
        val rootAccountId = user.linkedAccountId?.takeIf { it.isNotBlank() } ?: user.id
        val solverAcc = userDao.getLinkedUserByRole(rootAccountId, "SOLVER")
            ?: userDao.getUserByContactAndRole(user.phone, user.email, "SOLVER")
        return solverAcc != null && (solverAcc.hasCompletedSolverSetup || solverAcc.solverCategories.isNotBlank())
    }

    /**
     * ধাপ ১৪ (Repository Migration: Auth/Profile) — এখন real Supabase Auth (phone+password,
     * `SupabaseAuthManager` দেখুন — কেন phone+password এবং OTP এখনো demo সেই architecture
     * decision-এর বিস্তারিত ব্যাখ্যা সেখানে আছে) দিয়ে account তৈরি করে, তারপর
     * `complete_registration_profile` RPC দিয়ে বাকি প্রোফাইল বসায়।
     *
     * পুরনো local-only duplicate check (`userDao.getUserByPhoneOrEmail`) রাখা হয়েছে (এই
     * ডিভাইসে আগে থেকে থাকলে দ্রুত ধরা পড়ে, network call লাগে না) — কিন্তু আগের cross-device
     * check (`FirebaseSyncManager.fetchUserByCredentialFromCloud`) **সরানো হয়েছে**, কারণ এটা
     * users টেবিলের RLS (`auth.uid() = id`, ধাপ ৭-এ verified) এর কারণে আর সম্ভব না —
     * unauthenticated অবস্থায় অন্য কারো row read করা যায় না। এর বদলে Supabase Auth নিজেই
     * `auth.users.phone` এর উপর unique constraint enforce করে — সাইন-আপ কলটাই duplicate ধরবে,
     * ব্যর্থ হলে সেই error message-কে নিচে আগের মতোই বাংলা বার্তায় ম্যাপ করা হয়েছে।
     *
     * **Firestore dual-write বজায় রাখা হয়েছে** (`FirebaseSyncManager.syncUser`) — যদিও এখন
     * Supabase-ই source of truth, কিন্তু app-wide sync/realtime engine
     * (`MIGRATION_PROGRESS.md`-এর checklist item E, ধাপ ১৩-এ পাওয়া, এখনো migrate হয়নি) এবং
     * admin screens (ধাপ ১৬-১৯, এখনো migrate হয়নি) — এই দুটোই এখনো Firestore থেকে পড়ে। এই
     * dual-write সরিয়ে দিলে নতুন রেজিস্টার হওয়া user admin panel-এ/অন্য ডিভাইসের sync-এ দেখাই
     * যেত না — global rule #2 ("চালু ফিচার ভাঙা যাবে না") ভঙ্গ হতো। checklist item E migrate
     * হওয়ার পর এই dual-write সরিয়ে ফেলা যাবে (নতুন follow-up আইটেম হিসেবে
     * `MIGRATION_PROGRESS.md`-এ নোট করা হলো)।
     *
     * **⚠️ Not fully transactional**: signUp সফল হয়ে completeRegistrationProfile/
     * updateOwnProfile ব্যর্থ হলে — Supabase Auth-এ user তৈরি হয়ে গেছে কিন্তু profile আধা-খালি
     * থেকে যাবে (শুধু trigger-এর bare row)। এই ধাপে rollback/retry logic যোগ করা হয়নি (RPC
     * নিজেই idempotent না, আর client থেকে auth.users delete করার permission নেই) — ব্যর্থ হলে
     * caller একটা স্পষ্ট error message পাবে; recovery flow এই ধাপের স্কোপে যোগ করা হয়নি।
     */
    suspend fun registerUser(
        name: String,
        phone: String,
        email: String,
        password: String,
        role: String,
        latitude: Double,
        longitude: Double,
        address: String,
        solverCategories: List<String>
    ): Result<UserEntity> {
        val existing = userDao.getUserByPhoneOrEmail(phone.trim(), email.trim())
        if (existing != null) {
            return Result.failure(Exception("এই মোবাইল নম্বর বা ইমেইল দিয়ে ইতোমধ্যে অ্যাকাউন্ট রয়েছে। অনুগ্রহ করে লগইন করুন।"))
        }

        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        val trimmedEmail = email.trim()
        val trimmedAddress = address.trim()
        val categoriesCsv = solverCategories.joinToString(",")

        // [ফিক্স - ধাপ ৩৪ পরবর্তী] Supabase phone auth provider E.164 ফরম্যাট চায় (+৮৮০...),
        // trimmedPhone লোকাল ফরম্যাটে (০১...) থাকে — শুধু auth কলের জন্য normalize করা হলো,
        // trimmedPhone (DB-তে সেভ হওয়া ফরম্যাট) অপরিবর্তিত রাখা হয়েছে।
        val e164Phone = com.example.util.OtpService.normalizeTarget(trimmedPhone)
        val signUpResult = SupabaseAuthManager.signUpWithPhonePassword(e164Phone, password, trimmedName)
        if (signUpResult.isFailure) {
            val msg = signUpResult.exceptionOrNull()?.message ?: ""
            val friendlyMsg = if (msg.contains("already", ignoreCase = true) ||
                msg.contains("exists", ignoreCase = true) || msg.contains("registered", ignoreCase = true)
            ) {
                "এই মোবাইল নম্বর দিয়ে ইতোমধ্যে অ্যাকাউন্ট রয়েছে। অনুগ্রহ করে লগইন করুন।"
            } else {
                "রেজিস্ট্রেশন ব্যর্থ হয়েছে: ${msg.ifBlank { "অজানা সমস্যা" }}"
            }
            return Result.failure(Exception(friendlyMsg))
        }

        val uid = SupabaseAuthManager.currentUserId()
            ?: return Result.failure(Exception("রেজিস্ট্রেশন সম্পন্ন হয়নি — সেশন তৈরি করা যায়নি। আবার চেষ্টা করুন।"))

        val profileResult = SupabaseAuthManager.completeRegistrationProfile(
            name = trimmedName,
            address = trimmedAddress,
            latitude = latitude,
            longitude = longitude,
            role = role,
            solverCategories = categoriesCsv,
            hasSolverRole = role == "SOLVER",
            hasUserRole = true
        )
        if (profileResult.isFailure) {
            return Result.failure(
                Exception("অ্যাকাউন্ট তৈরি হয়েছে কিন্তু প্রোফাইল সেভ করা যায়নি: ${profileResult.exceptionOrNull()?.message ?: "অজানা সমস্যা"}")
            )
        }

        // email এই RPC-এর প্যারামিটারে নেই (SupabaseAuthManager-এর কমেন্ট দেখুন) — আলাদাভাবে বসানো
        // হচ্ছে। এটা best-effort: ব্যর্থ হলে পুরো registration ব্যর্থ করা হচ্ছে না (email
        // critical-path না)।
        if (trimmedEmail.isNotBlank()) {
            SupabaseSyncManager.updateOwnProfile(uid, email = trimmedEmail)
        }

        val cloudUser = SupabaseSyncManager.getUserById(uid).getOrNull()
        val newUser = cloudUser?.toUserEntity()?.copy(email = trimmedEmail.ifBlank { cloudUser.email ?: "" })
            ?: UserEntity( // Supabase read ব্যর্থ হলে fallback — local mirror অন্তত তৈরি থাকুক
                id = uid,
                name = trimmedName,
                phone = trimmedPhone,
                email = trimmedEmail,
                password = "",
                role = role,
                latitude = latitude,
                longitude = longitude,
                address = trimmedAddress,
                isKycVerified = false,
                kycStatus = "none",
                hasCompletedSolverSetup = role == "SOLVER",
                solverCategories = categoriesCsv,
                hasUserRole = true,
                hasSolverRole = role == "SOLVER",
                balance = 0.0,
                updatedAt = System.currentTimeMillis()
            )
        userDao.insertUser(newUser)
        // Firestore dual-write bridge — উপরের ক্লাস-ডক দেখুন কেন এখনো রাখা হয়েছে।

        // Add welcome notification
        val welcomeNotif = NotificationEntity(
            role = role,
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = newUser.id,
            title = "সমাধান প্ল্যাটফর্মে স্বাগতম!",
            message = if (role == "SOLVER") "আপনার অ্যাকাউন্ট তৈরি হয়েছে। কাজ শুরু করতে প্রোফাইল থেকে KYC সম্পন্ন করুন।"
                      else "আপনার অ্যাকাউন্ট সফলভাবে তৈরি হয়েছে। যেকোনো সমস্যা পোস্ট করুন মুহূর্তেই!",
            targetType = if (role == "SOLVER") "kyc" else "role",
            targetId = newUser.id
        )
        notificationDao.insertNotification(welcomeNotif)

        // [SUPABASE-MIGRATED - ধাপ ২৬] `complete_registration_profile` RPC (ধাপ ৩/১৪-এ wire করা,
        // উপরে কল হয়েছে) কোনো welcome notification insert করে না (সোর্স পড়ে যাচাই করা হয়েছে —
        // শুধু users row আপডেট করে) — তাই এটা এখানে আলাদাভাবে create_notification RPC দিয়ে
        // dual-write করা হলো। guard: caller নিজে (signUp-এর ফলে তৈরি হওয়া session-এর owner)
        // newUser নিজেই কিনা — best-effort, ব্যর্থ হলে শুধু log, local flow অপ্রভাবিত।
        if (SupabaseAuthManager.currentUserId() == newUser.id) {
            SupabaseSyncManager.createNotification(
                targetUserId = newUser.id,
                title = welcomeNotif.title,
                message = welcomeNotif.message,
                targetType = welcomeNotif.targetType,
                targetId = welcomeNotif.targetId,
                role = welcomeNotif.role
            ).onFailure { e ->
                Log.w("SomadhanRepo", "registerUser: welcome notification dual-write failed for ${newUser.id} (local flow unaffected): ${e.message}")
            }
        }

        return Result.success(newUser)
    }

    /**
     * ধাপ ১৪: real Supabase Auth phone+password sign-in (নতুন ফাংশন — আগে এই লজিকটা
     * `SomadhanViewModel.validateLoginCredentials()`-এ local bcrypt + Firestore cross-device
     * fetch দিয়ে হতো, এখন repository-লেয়ারে সরানো হলো যাতে data-layer boundary বজায় থাকে)।
     *
     * সফল হলে Supabase session তৈরি হয়ে যায় (এটাই সেই মুহূর্ত যেখানে real credential check হয় —
     * `SupabaseAuthManager`-এর ক্লাস-ডক-এ বর্ণিত timing trade-off অনুযায়ী, demo OTP এর *আগেই*)।
     * Supabase ভুল credential-এর জন্য generic error দেয় (phone না থাকা বনাম password ভুল —
     * আলাদা করা যায় না, ইচ্ছাকৃত Supabase নিরাপত্তা-ডিজাইন) — তাই পুরনো দুটো আলাদা বার্তার বদলে
     * এখন একটাই মিলিত বার্তা — এটা একটা সচেতন, user-facing behavior change।
     */
    suspend fun loginWithPhonePassword(phone: String, password: String): Result<UserEntity> {
        val trimmedPhone = phone.trim()
        // [ফিক্স - ধাপ ৩৪ পরবর্তী] উপরের signUpWithPhonePassword-এর মতোই — auth কলের জন্য
        // E.164 normalize করা হলো, trimmedPhone অপরিবর্তিত রাখা হয়েছে (নিচে ব্যবহৃত হতে পারে)।
        val e164Phone = com.example.util.OtpService.normalizeTarget(trimmedPhone)
        val signInResult = SupabaseAuthManager.signInWithPhonePassword(e164Phone, password)
        if (signInResult.isFailure) {
            return Result.failure(Exception("ফোন নম্বর বা পাসওয়ার্ড সঠিক নয়। আবার চেষ্টা করুন।"))
        }
        val uid = SupabaseAuthManager.currentUserId()
            ?: return Result.failure(Exception("লগইন সম্পন্ন হয়নি — সেশন তৈরি করা যায়নি। আবার চেষ্টা করুন।"))

        val cloudResult = SupabaseSyncManager.getUserById(uid)
        val cloudUser = cloudResult.getOrNull()
            ?: return Result.failure(Exception("প্রোফাইল খুঁজে পাওয়া যায়নি। সাপোর্টে যোগাযোগ করুন।"))

        val existingLocal = userDao.getUserById(uid)
        var entity = cloudUser.toUserEntity(existingPassword = existingLocal?.password ?: "")
        // [Somadhan Bug-Fix — সাব-বাগ ১.৬, একই stale-cloud-balance বাগের তৃতীয় (এবং আসলে
        // সবচেয়ে আগে ঘটা) root cause] এটাই প্রতিটা লগইন/account-switch-এর **Step 1**
        // (`SomadhanViewModel.validateLoginCredentials()` থেকে কল হয়, OTP-এর *আগেই*) --
        // `completeLoginAfterOtp()`-এর `refreshUserDataFromCloud()` (সাব-বাগ ১.৩/১.৫-এ
        // ফিক্স করা) চলারও **আগে** এটা unconditionally cloud→local overwrite করত। ফলে
        // account-switch করে ফিরে এলে, যদি ওই সময় escrow/wallet RPC dual-write ব্যর্থ হয়ে
        // pending outbox entry জমা থাকতো (cloud-এর balance তখনো stale), এই Step 1-ই প্রথমে
        // local (সঠিক) balance-কে stale cloud মান দিয়ে চাপা দিয়ে দিত -- ১.৩/১.৫-এর গার্ড
        // ততক্ষণে অকেজো, কারণ ওগুলো যে `existingLocal` পড়ে সেটা ইতিমধ্যেই এই ওভাররাইটের
        // পরে পড়া হতো। ফিক্স: ১.৩/১.৫-এর মতোই pending outbox থাকলে money/status-critical
        // ফিল্ড (legacy + role-scoped মিরর দুটোই) local-ই রাখা হচ্ছে।
        if (existingLocal != null) {
            val hasPendingOutboxSync = runCatching { pendingSyncOutboxDao.getPending() }
                .getOrDefault(emptyList())
                .isNotEmpty()
            if (hasPendingOutboxSync) {
                entity = entity.copy(
                    balance = existingLocal.balance,
                    reputationScore = existingLocal.reputationScore,
                    isBanned = existingLocal.isBanned,
                    isRestricted = existingLocal.isRestricted,
                    balanceUser = existingLocal.balanceUser,
                    balanceSolver = existingLocal.balanceSolver,
                    reputationScoreUser = existingLocal.reputationScoreUser,
                    reputationScoreSolver = existingLocal.reputationScoreSolver,
                    isBannedUser = existingLocal.isBannedUser,
                    isBannedSolver = existingLocal.isBannedSolver,
                    isRestrictedUser = existingLocal.isRestrictedUser,
                    isRestrictedSolver = existingLocal.isRestrictedSolver
                )
                Log.w(
                    "SomadhanRepo",
                    "loginWithPhonePassword: pending outbox sync exists for device -- keeping local balance/status (legacy + role-scoped) for user $uid instead of stale cloud value"
                )
            }
        }
        cacheUserLocally(entity)

        if (entity.isBanned) {
            return Result.failure(Exception("আপনার অ্যাকাউন্টটি সাময়িকভাবে স্থগিত করা হয়েছে। অ্যাডমিনের সাথে যোগাযোগ করুন।"))
        }

        return Result.success(entity)
    }

    suspend fun switchRole(user: UserEntity, newRole: String, newCategories: List<String> = emptyList()): UserEntity {
        val rootAccountId = user.linkedAccountId?.takeIf { it.isNotBlank() } ?: user.id

        // Ensure original record retains the common linkedAccountId and synced info
        val updatedOriginal = user.copy(
            linkedAccountId = rootAccountId,
            hasUserRole = true,
            hasSolverRole = if (newRole == "SOLVER" || user.role == "SOLVER") true else user.hasSolverRole,
            updatedAt = System.currentTimeMillis()
        )
        userDao.updateUser(updatedOriginal)

        // Check if an existing linked account for this person already exists in the target role
        val existingLinked = userDao.getLinkedUserByRole(rootAccountId, newRole)
            ?: userDao.getUserByContactAndRole(user.phone, user.email, newRole)

        var resultUser: UserEntity = if (existingLinked != null) {
            val finalCategories = if (newRole == "SOLVER") {
                if (newCategories.isNotEmpty()) newCategories.joinToString(",")
                else if (existingLinked.solverCategories.isNotBlank()) existingLinked.solverCategories
                else user.solverCategories
            } else existingLinked.solverCategories

            val updatedLinked = existingLinked.copy(
                name = user.name,
                phone = user.phone,
                email = user.email,
                password = user.password,
                address = user.address,
                latitude = user.latitude,
                longitude = user.longitude,
                profileImageUri = user.profileImageUri,
                isVerifiedBadge = user.isVerifiedBadge,
                linkedAccountId = rootAccountId,
                hasUserRole = true,
                hasSolverRole = true,
                hasCompletedSolverSetup = if (newRole == "SOLVER") true else existingLinked.hasCompletedSolverSetup,
                solverCategories = finalCategories,
                updatedAt = System.currentTimeMillis()
            )
            userDao.updateUser(updatedLinked)
            // [ধাপ ৩১ ক] linked ("SOLVER_xxxx"/"USER_xxxx", non-UUID) row-এর জন্য আলাদা কোনো
            // Firebase Auth/Firestore identity নেই যেটা এই sync অর্থবহভাবে টার্গেট করতে পারে --
            // root row-এর sync (উপরে, updatedOriginal-এর জন্য) দিয়েই এই ব্যক্তির cloud-সাইড ডেটা
            // যথেষ্ট প্রতিফলিত হয়। তাই এই redundant কলটা সরানো হলো (global rule #২ অনুযায়ী root
            // row-এর sync অক্ষত রাখা হয়েছে)।
            updatedLinked
        } else {
            // Create a brand new distinct UserEntity record linked via linkedAccountId
            // [UID ফিক্স] linked local row-এর id এখনো নিজস্ব "SOLVER_xxxx"/"USER_xxxx" প্যাটার্নেই
            // থাকছে (local Room PK, ইউজারকে দেখানো হয় না) — কিন্তু displayUid (নিচে) এখন root
            // account-এর আসল সার্ভার-জেনারেটেড UID থেকে আসে, তাই আর এলোমেলো লম্বা সংখ্যা বানানো লাগে না।
            val newId = if (newRole == "SOLVER") "SOLVER_${UUID.randomUUID().toString().take(8)}" else "USER_${UUID.randomUUID().toString().take(8)}"
            val categoriesToUse = if (newRole == "SOLVER") {
                if (newCategories.isNotEmpty()) newCategories.joinToString(",") else user.solverCategories
            } else ""

            val newEntry = UserEntity(
                id = newId,
                // [UID ফিক্স] আগে এখানে ৯-সংখ্যার একটা এলোমেলো লোকাল সংখ্যা বানানো হতো (কোনো
                // uniqueness guarantee ছাড়াই, ডিভাইস-only, cross-device sync হতো না)। এখন
                // root account-এর real server-generated displayUid (Supabase `display_uid`
                // কলাম থেকে সিঙ্ক হয়ে আসা, ৬-সংখ্যা দিয়ে শুরু) ব্যবহার করা হচ্ছে — একজন
                // মানুষের USER আর SOLVER দুই role-এর জন্যই একই UID দেখাবে, যা প্রত্যাশিত।
                displayUid = user.displayUid,
                name = user.name,
                phone = user.phone,
                email = user.email,
                password = user.password,
                role = newRole,
                latitude = user.latitude,
                longitude = user.longitude,
                address = user.address,
                profileImageUri = user.profileImageUri,
                isKycVerified = if (newRole == "SOLVER") user.isKycVerified else false,
                kycStatus = if (newRole == "SOLVER") user.kycStatus else "none",
                kycDocumentType = user.kycDocumentType,
                kycDocumentNumber = user.kycDocumentNumber,
                kycDocumentImage = user.kycDocumentImage,
                kycDocumentFrontImage = user.kycDocumentFrontImage,
                kycDocumentBackImage = user.kycDocumentBackImage,
                kycSelfieImage = user.kycSelfieImage,
                kycFirstName = user.kycFirstName,
                kycLastName = user.kycLastName,
                kycAddress = user.kycAddress,
                kycSubmissionDate = user.kycSubmissionDate,
                kycRejectReason = user.kycRejectReason,
                hasCompletedSolverSetup = if (newRole == "SOLVER") (categoriesToUse.isNotBlank()) else true,
                solverCategories = categoriesToUse,
                // [ব্যালেন্স ফিক্স] আগে এখানে হার্ডকোডেড 0.0 বসানো হতো, এবং নিচের best-effort
                // cloud sync ব্যর্থ হলে (নেটওয়ার্ক সমস্যা ইত্যাদি) UI-তে ভুলভাবে ০ আটকে থাকতে
                // পারত। এখন root account-এ ইতিমধ্যে সিঙ্ক হয়ে থাকা role-scoped ব্যালেন্স
                // (balanceUser/balanceSolver) সরাসরি ব্যবহার করা হচ্ছে, যেটা নিচের cloud sync
                // ব্যর্থ হলেও সঠিক থাকে (network ছাড়াই তাৎক্ষণিকভাবে সঠিক মান দেখায়)।
                balance = if (newRole == "SOLVER") user.balanceSolver else user.balanceUser,
                balanceUser = user.balanceUser,
                balanceSolver = user.balanceSolver,
                isVerifiedBadge = user.isVerifiedBadge,
                isBanned = false,
                isRestricted = false,
                hasUserRole = true,
                hasSolverRole = true,
                linkedAccountId = rootAccountId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            userDao.insertUser(newEntry)
            newEntry
        }

        // [SUPABASE-MIGRATED - ধাপ ১৪.৫খ] উপরের local dual-row (per-role আলাদা UserEntity) লজিক
        // হুবহু অপরিবর্তিত রাখা হলো (global rule #২)। এখানে শুধু Supabase-এর single-row RPC-কে
        // best-effort dual-write হিসেবে কল করা হচ্ছে (ব্যর্থ হলেও উপরের local Room flow
        // ইতিমধ্যে সম্পন্ন, user-facing error দেখাবে না -- acceptBid()/placeBid()-এর মতোই প্যাটার্ন)।
        // RPC caller-এর নিজের root row-ই (id = auth.uid()) আপডেট করে, তাই এটা কেবল তখনই কল করা
        // হচ্ছে যখন সক্রিয় Supabase session-এর uid রুট অ্যাকাউন্টের সাথে মেলে (linked "SOLVER_xxxx"/
        // "USER_xxxx" local row-গুলোর জন্য আলাদা কোনো cloud row/session নেই)। সফল হলে RPC-এর
        // ফেরত দেওয়া role-scoped balance/reputation/ban/restrict কলাম (balance_user/
        // balance_solver ইত্যাদি) দিয়ে resultUser-এর role-scoped local ফিল্ড আর "active" plain
        // ফিল্ড (balance/reputationScore/isBanned/isRestricted) দুটোই আপডেট করে persist করা হয়,
        // যাতে UI (যেটা plain ফিল্ড পড়ে) সঠিক cloud role-scoped মান দেখে।
        if (SupabaseAuthManager.currentUserId() == rootAccountId) {
            SupabaseSyncManager.switchRole(
                newRole,
                if (newRole == "SOLVER") newCategories.joinToString(",").ifBlank { null } else null
            ).onSuccess { json ->
                val obj = json as? kotlinx.serialization.json.JsonObject
                fun readDouble(key: String): Double? =
                    (obj?.get(key) as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()
                fun readBoolean(key: String): Boolean? =
                    (obj?.get(key) as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull()

                val cloudBalanceUser = readDouble("balance_user")
                val cloudBalanceSolver = readDouble("balance_solver")
                val cloudRepUser = readDouble("reputation_score_user")
                val cloudRepSolver = readDouble("reputation_score_solver")
                val cloudBannedUser = readBoolean("is_banned_user")
                val cloudBannedSolver = readBoolean("is_banned_solver")
                val cloudRestrictedUser = readBoolean("is_restricted_user")
                val cloudRestrictedSolver = readBoolean("is_restricted_solver")

                val activeBalance = if (newRole == "SOLVER") cloudBalanceSolver else cloudBalanceUser
                val activeRep = if (newRole == "SOLVER") cloudRepSolver else cloudRepUser
                val activeBanned = if (newRole == "SOLVER") cloudBannedSolver else cloudBannedUser
                val activeRestricted = if (newRole == "SOLVER") cloudRestrictedSolver else cloudRestrictedUser

                val cloudSynced = resultUser.copy(
                    balanceUser = cloudBalanceUser ?: resultUser.balanceUser,
                    balanceSolver = cloudBalanceSolver ?: resultUser.balanceSolver,
                    reputationScoreUser = cloudRepUser ?: resultUser.reputationScoreUser,
                    reputationScoreSolver = cloudRepSolver ?: resultUser.reputationScoreSolver,
                    isBannedUser = cloudBannedUser ?: resultUser.isBannedUser,
                    isBannedSolver = cloudBannedSolver ?: resultUser.isBannedSolver,
                    isRestrictedUser = cloudRestrictedUser ?: resultUser.isRestrictedUser,
                    isRestrictedSolver = cloudRestrictedSolver ?: resultUser.isRestrictedSolver,
                    balance = activeBalance ?: resultUser.balance,
                    reputationScore = activeRep ?: resultUser.reputationScore,
                    isBanned = activeBanned ?: resultUser.isBanned,
                    isRestricted = activeRestricted ?: resultUser.isRestricted,
                    updatedAt = System.currentTimeMillis()
                )
                userDao.updateUser(cloudSynced)
                resultUser = cloudSynced
            }.onFailure { e ->
                Log.w("SomadhanRepo", "switchRole: Supabase dual-write failed for $newRole (local flow unaffected): ${e.message}")
            }
        }

        // Send role change notification to the switched profile
        val roleNotif = NotificationEntity(
            role = newRole,
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = resultUser.id,
            title = "অ্যাকাউন্ট রোল পরিবর্তন",
            message = "আপনার অ্যাকাউন্ট সফলভাবে ${if (newRole == "SOLVER") "সমাধানকারী (Solver)" else "ইউজার (User)"} মোডে পরিবর্তিত হয়েছে।",
            targetType = "profile",
            targetId = resultUser.id
        )
        notificationDao.insertNotification(roleNotif)

        return resultUser
    }

    /**
     * [ROLE_UID ফিক্স - ধাপ ২] নতুন single-row role-switch ফাংশন। উপরের পুরনো `switchRole()`
     * এখনো অপরিবর্তিত রাখা হয়েছে এবং এখনো ViewModel থেকেই কল হচ্ছে — এই ফাংশন এখনো কোথাও
     * থেকে কল হয় না, wiring পরের ধাপে (৪) হবে। এটা কোনো নতুন `UserEntity` insert করে না,
     * `SOLVER_xxxx`/`USER_xxxx` id বানায় না — শুধু root row-এর (id সবসময় root Supabase Auth
     * UID, এই ফাংশনে কখনো বদলায় না) role-সংক্রান্ত ফিল্ডগুলো flip করে। ROLE_UID_FIX_DESIGN.md
     * সেকশন ২-এর স্পেক অনুযায়ী লেখা হয়েছে।
     */
    suspend fun switchRoleInPlace(user: UserEntity, newRole: String, newCategories: List<String> = emptyList()): UserEntity {
        val rootAccountId = user.linkedAccountId?.takeIf { it.isNotBlank() } ?: user.id

        // root row নিজেই লোড করা হচ্ছে (linked/dual row থেকে না) — যদি ইতিমধ্যে user.id-ই
        // root হয়, userDao.getUserById(rootAccountId) সেটাই ফেরত দেবে। null হলে (কখনো হওয়ার
        // কথা না, defensive) পাস করা user-কেই fallback হিসেবে ব্যবহার করা হচ্ছে।
        val rootUser = userDao.getUserById(rootAccountId) ?: user

        val newSolverCategories = if (newRole == "SOLVER") {
            if (newCategories.isNotEmpty()) newCategories.joinToString(",")
            else rootUser.solverCategories
        } else rootUser.solverCategories

        val activeBalance = if (newRole == "SOLVER") rootUser.balanceSolver else rootUser.balanceUser
        val activeRep = if (newRole == "SOLVER") rootUser.reputationScoreSolver else rootUser.reputationScoreUser
        val activeBanned = if (newRole == "SOLVER") rootUser.isBannedSolver else rootUser.isBannedUser
        val activeRestricted = if (newRole == "SOLVER") rootUser.isRestrictedSolver else rootUser.isRestrictedUser

        var updatedRoot = rootUser.copy(
            role = newRole,
            hasUserRole = true,
            hasSolverRole = if (newRole == "SOLVER" || rootUser.hasSolverRole) true else rootUser.hasSolverRole,
            hasCompletedSolverSetup = if (newRole == "SOLVER") true else rootUser.hasCompletedSolverSetup,
            solverCategories = newSolverCategories,
            balance = activeBalance,
            reputationScore = activeRep,
            isBanned = activeBanned,
            isRestricted = activeRestricted,
            updatedAt = System.currentTimeMillis()
        )
        userDao.updateUser(updatedRoot)

        // বিদ্যমান cloud RPC — এটা আগে থেকেই root row (id = auth.uid())-ভিত্তিক, তাই বদলানো
        // হয়নি (ROLE_UID_FIX_DESIGN.md সেকশন ২, পয়েন্ট ১-২)। পুরনো switchRole()-এর মতোই guard
        // আর response-handling প্যাটার্ন — শুধু dual-row অংশ বাদ দেওয়া হয়েছে।
        if (SupabaseAuthManager.currentUserId() == rootAccountId) {
            SupabaseSyncManager.switchRole(
                newRole,
                if (newRole == "SOLVER") newSolverCategories.ifBlank { null } else null
            ).onSuccess { json ->
                // [বাগ ১.৮ ফিক্স] ১.৩/১.৫/১.৬-এর মতোই গার্ড — pending outbox sync থাকলে (মানে কোনো
                // dual-write RPC আগে fail করে local balance/status এখনো cloud-এর চেয়ে এগিয়ে আছে)
                // switch_role RPC-এর ফেরত দেওয়া balance_user/balance_solver (স্টেল হতে পারে) দিয়ে
                // local overwrite করা হবে না -- role বদলে গেলেও balance/reputation/ban/restrict সব
                // updatedRoot (local, সঠিক)-এর মানই থাকবে। bug ১.৮: এই ব্লকে আগে এই গার্ডটা ছিলই না।
                val hasPendingOutboxSync = runCatching { pendingSyncOutboxDao.getPending() }
                    .getOrDefault(emptyList())
                    .isNotEmpty()
                if (hasPendingOutboxSync) {
                    Log.w(
                        "SomadhanRepo",
                        "switchRoleInPlace: pending outbox sync exists -- keeping local balance/status instead of switch_role RPC's cloud values for user ${updatedRoot.id}"
                    )
                } else {
                    val obj = json as? kotlinx.serialization.json.JsonObject
                    fun readDouble(key: String): Double? =
                        (obj?.get(key) as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()
                    fun readBoolean(key: String): Boolean? =
                        (obj?.get(key) as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull()

                    val cloudBalanceUser = readDouble("balance_user")
                    val cloudBalanceSolver = readDouble("balance_solver")
                    val cloudRepUser = readDouble("reputation_score_user")
                    val cloudRepSolver = readDouble("reputation_score_solver")
                    val cloudBannedUser = readBoolean("is_banned_user")
                    val cloudBannedSolver = readBoolean("is_banned_solver")
                    val cloudRestrictedUser = readBoolean("is_restricted_user")
                    val cloudRestrictedSolver = readBoolean("is_restricted_solver")

                    val cloudActiveBalance = if (newRole == "SOLVER") cloudBalanceSolver else cloudBalanceUser
                    val cloudActiveRep = if (newRole == "SOLVER") cloudRepSolver else cloudRepUser
                    val cloudActiveBanned = if (newRole == "SOLVER") cloudBannedSolver else cloudBannedUser
                    val cloudActiveRestricted = if (newRole == "SOLVER") cloudRestrictedSolver else cloudRestrictedUser

                    val cloudSynced = updatedRoot.copy(
                        balanceUser = cloudBalanceUser ?: updatedRoot.balanceUser,
                        balanceSolver = cloudBalanceSolver ?: updatedRoot.balanceSolver,
                        reputationScoreUser = cloudRepUser ?: updatedRoot.reputationScoreUser,
                        reputationScoreSolver = cloudRepSolver ?: updatedRoot.reputationScoreSolver,
                        isBannedUser = cloudBannedUser ?: updatedRoot.isBannedUser,
                        isBannedSolver = cloudBannedSolver ?: updatedRoot.isBannedSolver,
                        isRestrictedUser = cloudRestrictedUser ?: updatedRoot.isRestrictedUser,
                        isRestrictedSolver = cloudRestrictedSolver ?: updatedRoot.isRestrictedSolver,
                        balance = cloudActiveBalance ?: updatedRoot.balance,
                        reputationScore = cloudActiveRep ?: updatedRoot.reputationScore,
                        isBanned = cloudActiveBanned ?: updatedRoot.isBanned,
                        isRestricted = cloudActiveRestricted ?: updatedRoot.isRestricted,
                        updatedAt = System.currentTimeMillis()
                    )
                    userDao.updateUser(cloudSynced)
                    updatedRoot = cloudSynced
                }
            }.onFailure { e ->
                Log.w("SomadhanRepo", "switchRoleInPlace: Supabase dual-write failed for $newRole (local flow unaffected): ${e.message}")
            }
        }

        // Send role change notification — updatedRoot.id সবসময় root id (এই ফাংশনে id কখনো
        // বদলায়নি, পুরনো dual-row switchRole()-এর মতো linked-row id না)।
        val roleNotifInPlace = NotificationEntity(
            role = newRole,
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = updatedRoot.id,
            title = "অ্যাকাউন্ট রোল পরিবর্তন",
            message = "আপনার অ্যাকাউন্ট সফলভাবে ${if (newRole == "SOLVER") "সমাধানকারী (Solver)" else "ইউজার (User)"} মোডে পরিবর্তিত হয়েছে।",
            targetType = "profile",
            targetId = updatedRoot.id
        )
        notificationDao.insertNotification(roleNotifInPlace)

        return updatedRoot
    }

    /**
     * [ROLE_UID ফিক্স - ধাপ ৩] এক legacy dual-row migration চালানোর ফলাফল — manual
     * verification/লগিং-এর জন্য (কোনো UI এটা ব্যবহার করে না)।
     */
    data class LegacyDualRowMigrationResult(
        val rootId: String,
        val candidateRows: Int = 0,      // এই root-এর সাথে যুক্ত, এখনো archive না হওয়া legacy row-এর সংখ্যা
        val archivedRows: Int = 0,       // এই রানে migrated হিসেবে চিহ্নিত হওয়া row-এর সংখ্যা
        val kycMerged: Boolean = false,  // root-এ KYC ব্লক কপি হয়েছে কিনা
        val categoriesMerged: Boolean = false,
        val balanceConflicts: List<String> = emptyList(), // শুধু log/report — কোনো balance বদলানো হয় না
        val rootChanged: Boolean = false
    )

    /**
     * [ROLE_UID ফিক্স - ধাপ ৩] existing installs-এর জন্য migration/merge।
     * ROLE_UID_FIX_DESIGN.md সেকশন ৪-এর প্ল্যান অনুযায়ী লেখা।
     *
     * প্রেক্ষাপট: পুরনো `switchRole()` প্রতি role-এর জন্য local Room-এ আলাদা row বানাতো
     * (`SOLVER_xxxxxxxx`/`USER_xxxxxxxx` id, `linkedAccountId` দিয়ে root-এর সাথে যুক্ত)।
     * single-row মডেলে যাওয়ার পর ঐ row-গুলোর KYC/categories ডেটা যেন হারিয়ে না যায়, তাই
     * এই ফাংশন root row-এ **শুধু ফাঁকা ফিল্ড পূরণ** করে (root-এ ইতিমধ্যে থাকা কোনো ডেটা
     * কখনো overwrite করে না — rule #৫), আর পুরনো row **delete না করে** শুধু
     * `localDualRowArchived = true` দিয়ে চিহ্নিত করে রাখে (rule #৩, rollback সম্ভব রাখতে)।
     *
     * **Balance সম্পর্কে সিদ্ধান্ত (ব্যবহারকারীর সাথে confirm করা, ধাপ ৩):** এই migration
     * কোনো balance ফিল্ড **লেখে না** — না plain `balance`, না role-scoped
     * `balanceUser`/`balanceSolver`। কারণ ধাপ ১৪.৫ক-খ-এর পর থেকে Supabase-এর role-scoped
     * কলামই টাকার একমাত্র source-of-truth, আর ঐ legacy local row-গুলোর balance কখনো
     * cloud-এ যায়নি (তাদের নিজস্ব auth session ছিল না)। সেই কখনো-sync-না-হওয়া local
     * সংখ্যা root-এ কপি করলে cloud-এ নেই এমন টাকা তৈরি হতো, যা পরে reconcile করা যেত না।
     * তাই conflict শুধু detect করে log + result-এ ফেরত দেওয়া হয় (`balanceConflicts`),
     * সিদ্ধান্ত মানুষ/admin নেবে। এটাই এই ফাংশনের একমাত্র money-সংক্রান্ত আচরণ —
     * ভবিষ্যতে নীতি বদলালে পরিবর্তনটা নিচের "৪)" ব্লকেই সীমাবদ্ধ থাকবে।
     *
     * **এই ধাপে এই ফাংশন কোথাও থেকে কল হয় না** — login flow-এ wiring ধাপ ৪-এ হবে।
     * কোনো Supabase/cloud write এখানে নেই (pure local Room merge)।
     */
    suspend fun migrateLegacyDualRowsIntoRoot(rootUserId: String): LegacyDualRowMigrationResult {
        // [কম্পাইল ফিক্স] userDao.getUserById(...) এর রিটার্ন টাইপ UserEntity? — শুধু
        // `if (x == null) return` দিয়ে null-check করলে Kotlin `val root`-কে এই স্কোপে
        // smart-cast করে non-null ধরে, কিন্তু নিচে `var mergedRoot = root` লেখার সময় সেই
        // narrowed টাইপ var-এ প্রোপাগেট হয় না (var-এর initializer টাইপ root-এর *declared*
        // টাইপ UserEntity? থেকে infer হয়) — ফলে mergedRoot নিজেই nullable হয়ে যায় আর
        // mergedRoot.kycStatus/mergedRoot.copy(...) এর মতো সব জায়গায় কম্পাইল এরর দেয়।
        // এলভিস (?:) দিয়ে explicit non-null টাইপ (`val root: UserEntity`) অ্যানোটেট করলে এই
        // সমস্যা হয় না — লজিক অপরিবর্তিত (root==null হলে আগের মতোই লগ করে একই আগের ফলাফল
        // রিটার্ন করে)।
        val root: UserEntity = userDao.getUserById(rootUserId) ?: run {
            Log.w("SomadhanRepo", "migrateLegacyDualRowsIntoRoot: root row পাওয়া যায়নি ($rootUserId), কিছু করা হলো না")
            return LegacyDualRowMigrationResult(rootId = rootUserId)
        }

        // ১) candidate legacy row বাছাই: root-এর সাথে linked, root নিজে না, id-তে পুরনো
        //    dual-row prefix আছে (root id সবসময় UUID-shape Supabase Auth UID, তাই এটাই
        //    চেনার নির্ভরযোগ্য উপায় — ROLE_UID_FIX_DESIGN.md সেকশন ৪.২), এবং আগে archive
        //    হয়নি (idempotency guard — একই row দ্বিতীয়বার প্রসেস হবে না)।
        val candidates = userDao.getLinkedAccounts(root.id).filter {
            it.id != root.id &&
                (it.id.startsWith("SOLVER_") || it.id.startsWith("USER_")) &&
                !it.localDualRowArchived
        }
        if (candidates.isEmpty()) {
            return LegacyDualRowMigrationResult(rootId = root.id)
        }

        var mergedRoot = root
        var kycMerged = false
        var categoriesMerged = false
        val balanceConflicts = mutableListOf<String>()

        for (legacy in candidates) {
            // ২) KYC — পুরো ব্লক একসাথে, field-by-field না। কারণ দুই row-এর আলাদা KYC
            //    submission-এর ফিল্ড মিশে গেলে (যেমন এক row-এর front image + অন্য row-এর
            //    document number) অর্থহীন/অসঙ্গত KYC তৈরি হতো। root-এর KYC "ফাঁকা" ধরা হয়
            //    যখন kycStatus == "none" এবং isKycVerified == false।
            val rootKycEmpty = mergedRoot.kycStatus == "none" && !mergedRoot.isKycVerified
            val legacyHasKyc = legacy.kycStatus != "none" || legacy.isKycVerified
            if (rootKycEmpty && legacyHasKyc) {
                mergedRoot = mergedRoot.copy(
                    isKycVerified = legacy.isKycVerified,
                    kycStatus = legacy.kycStatus,
                    kycDocumentType = legacy.kycDocumentType,
                    kycDocumentNumber = legacy.kycDocumentNumber,
                    kycDocumentImage = legacy.kycDocumentImage,
                    kycDocumentFrontImage = legacy.kycDocumentFrontImage,
                    kycDocumentBackImage = legacy.kycDocumentBackImage,
                    kycSelfieImage = legacy.kycSelfieImage,
                    kycFirstName = legacy.kycFirstName,
                    kycLastName = legacy.kycLastName,
                    kycAddress = legacy.kycAddress,
                    kycSubmissionDate = legacy.kycSubmissionDate,
                    kycRejectReason = legacy.kycRejectReason
                )
                kycMerged = true
            }

            // ৩) solver categories / setup ফ্ল্যাগ — শুধু root ফাঁকা হলে পূরণ।
            if (mergedRoot.solverCategories.isBlank() && legacy.solverCategories.isNotBlank()) {
                mergedRoot = mergedRoot.copy(solverCategories = legacy.solverCategories)
                categoriesMerged = true
            }
            // role-ownership ফ্ল্যাগ OR করা হচ্ছে: legacy row-এর অস্তিত্বই প্রমাণ করে
            // ব্যবহারকারী ঐ role-টা কোনো এক সময় ব্যবহার করেছে। এটা কোনো ডেটা মোছে না,
            // শুধু false → true হতে পারে।
            val legacyImpliesSolver = legacy.hasSolverRole || legacy.role == "SOLVER"
            mergedRoot = mergedRoot.copy(
                hasUserRole = mergedRoot.hasUserRole || legacy.hasUserRole || legacy.role == "USER",
                hasSolverRole = mergedRoot.hasSolverRole || legacyImpliesSolver,
                hasCompletedSolverSetup = mergedRoot.hasCompletedSolverSetup || legacy.hasCompletedSolverSetup
            )

            // ৪) Balance — কিছুই লেখা হয় না (উপরের KDoc-এ কারণ)। শুধু conflict detect + log।
            val rootScopedBalance = if (legacy.role == "SOLVER") mergedRoot.balanceSolver else mergedRoot.balanceUser
            if (legacy.balance > 0.0 && legacy.balance != rootScopedBalance) {
                val note = "legacy=${legacy.id} role=${legacy.role} legacyBalance=${legacy.balance} " +
                    "rootScopedBalance=$rootScopedBalance (কোনো balance বদলানো হয়নি)"
                balanceConflicts += note
                Log.w("SomadhanRepo", "migrateLegacyDualRowsIntoRoot: balance conflict — $note")
            }
        }

        // ৫) সব হিসেব শেষ হওয়ার পরই লেখা হচ্ছে — মাঝপথে ব্যতিক্রম ঘটলে কোনো row "migrated"
        //    হিসেবে চিহ্নিত হবে না, পরের রানে আবার পুরোটা চেষ্টা হবে।
        val rootChanged = mergedRoot != root
        if (rootChanged) {
            userDao.updateUser(mergedRoot.copy(updatedAt = System.currentTimeMillis()))
        }
        for (legacy in candidates) {
            // row **মোছা হচ্ছে না** — শুধু archived মার্কার (rule #৩, rollback সম্ভব রাখতে)।
            userDao.updateUser(legacy.copy(localDualRowArchived = true, updatedAt = System.currentTimeMillis()))
        }

        Log.d(
            "SomadhanRepo",
            "migrateLegacyDualRowsIntoRoot: root=${root.id}, ${candidates.size}টা legacy row archived, " +
                "kycMerged=$kycMerged, categoriesMerged=$categoriesMerged, " +
                "balanceConflicts=${balanceConflicts.size}"
        )

        return LegacyDualRowMigrationResult(
            rootId = root.id,
            candidateRows = candidates.size,
            archivedRows = candidates.size,
            kycMerged = kycMerged,
            categoriesMerged = categoriesMerged,
            balanceConflicts = balanceConflicts,
            rootChanged = rootChanged
        )
    }

    // ---------------- CATEGORIES ----------------

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun getCategoryById(id: String): CategoryEntity? = categoryDao.getCategoryById(id)

    suspend fun updateCategory(category: CategoryEntity) = adminUpdateCategory(category)

    private fun CategoryEntity.toSupabaseDto() = CategoryDto(
        id = id,
        nameBangla = nameBangla,
        nameEnglish = nameEnglish,
        isPhysical = isPhysical,
        iconName = iconName,
        keywords = keywords,
        minBudget = minBudget,
        maxBudget = maxBudget,
        isActive = isActive,
        instantJobEnabled = instantJobEnabled,
        instantJobRadiusKm = instantJobRadiusKm
    )

    suspend fun insertCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি। `categories`
        // টেবিলে RLS পলিসি `categories_admin_write` (ALL, is_admin(auth.uid())) ইতিমধ্যেই admin-only লেখা
        // নিশ্চিত করে (Supabase MCP দিয়ে verify করা) — তাই RPC লাগেনি, সরাসরি upsert()।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.upsertCategory(category.toSupabaseDto()).onFailure { e ->
                Log.w("SomadhanRepo", "insertCategory: Supabase dual-write failed for ${category.id} (local flow unaffected): ${e.message}")
            }
        }
        logAdminAction(
            actionType = "ADD_CATEGORY",
            targetId = category.id,
            targetName = category.nameBangla,
            details = "নতুন ক্যাটাগরি যুক্ত করা হয়েছে (${category.nameEnglish})"
        )
    }

    suspend fun adminUpdateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি। RLS একই
        // `categories_admin_write` পলিসি দিয়ে কভার্ড।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.upsertCategory(category.toSupabaseDto()).onFailure { e ->
                Log.w("SomadhanRepo", "adminUpdateCategory: Supabase dual-write failed for ${category.id} (local flow unaffected): ${e.message}")
            }
        }
        logAdminAction(
            actionType = "UPDATE_CATEGORY",
            targetId = category.id,
            targetName = category.nameBangla,
            details = "বাজেট: ৳${category.minBudget.toInt()} - ৳${category.maxBudget.toInt()}"
        )
    }

    suspend fun adminToggleCategoryActive(categoryId: String, isActive: Boolean) {
        categoryDao.setCategoryActive(categoryId, isActive)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি।
        // পুরো row পাঠানোর দরকার নেই — শুধু is_active কলাম আপডেট করলেই যথেষ্ট (RLS একই
        // `categories_admin_write` পলিসি দিয়ে কভার্ড)।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.setCategoryActive(categoryId, isActive).onFailure { e ->
                Log.w("SomadhanRepo", "adminToggleCategoryActive: Supabase dual-write failed for $categoryId (local flow unaffected): ${e.message}")
            }
        }
        logAdminAction(
            actionType = if (isActive) "ENABLE_CATEGORY" else "DISABLE_CATEGORY",
            targetId = categoryId,
            targetName = categoryId,
            details = if (isActive) "ক্যাটাগরি সক্রিয় করা হয়েছে" else "ক্যাটাগরি নিষ্ক্রিয় করা হয়েছে"
        )
    }

    suspend fun deleteCategory(categoryId: String) {
        categoryDao.deleteCategory(categoryId)
        logAdminAction(
            actionType = "DELETE_CATEGORY",
            targetId = categoryId,
            targetName = categoryId,
            details = "ক্যাটাগরি এবং সলভারের সংশ্লিষ্ট স্কিল মুছে ফেলা হয়েছে"
        )

        // [SUPABASE-MIGRATED - ধাপ ৪] `categories` row delete dual-write -- RLS `categories_admin_write`
        // (cmd=ALL) পলিসি দিয়ে কভার্ড, তাই সরাসরি delete (RPC লাগেনি), ঠিক upsertCategory()-র
        // প্যাটার্নে। কোনো problem এখনও এই category রেফার করলে (FK, ON DELETE NO ACTION) এই কলটা
        // ব্যর্থ হতে পারে -- expected, best-effort বলে শুধু log হবে, local flow অক্ষুণ্ণ থাকে।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.deleteCategoryRemote(categoryId).onFailure { e ->
                Log.w("SomadhanRepo", "deleteCategory: category-delete dual-write failed for $categoryId (local flow unaffected): ${e.message}")
            }
        }

        // Cascade removal from Solvers' selected categories and send notification
        val users = userDao.getAllUsers().firstOrNull() ?: emptyList()
        var cascadeAffectedAny = false
        users.forEach { user ->
            if (user.solverCategories.isNotBlank()) {
                val catList = user.solverCategories.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (catList.contains(categoryId)) {
                    cascadeAffectedAny = true
                    val updatedCats = catList.filter { it != categoryId }.joinToString(",")
                    userDao.updateUser(user.copy(solverCategories = updatedCats, updatedAt = System.currentTimeMillis()))

                    val notif = NotificationEntity(
                        role = "SOLVER",
                        id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                        userId = user.id,
                        title = "স্কিল ক্যাটাগরি আপডেট",
                        message = "আপনার নির্বাচিত একটি স্কিল ক্যাটাগরি প্ল্যাটফর্ম অ্যাডমিন কর্তৃক অপসারিত হয়েছে।",
                        targetType = "solver_skills",
                        targetId = user.id
                    )
                    notificationDao.insertNotification(notif)

                    // [SUPABASE-MIGRATED - ধাপ ২৬] `categories` টেবিলের admin-write policy শুধু
                    // categories টেবিলই কভার করে, `notifications`-এ কোনো client-writable INSERT
                    // policy নেই (আগের ধাপগুলোতে verified) — তাই এই cascade notification
                    // create_notification RPC দিয়েই পাঠাতে হবে। caller এখানে সবসময় admin
                    // (deleteCategory admin-only ফাংশন) — RPC-এর admin-path party-check বাইপাস
                    // করবে। best-effort, ব্যর্থ হলে শুধু log।
                    if (SupabaseAuthManager.currentUserId() != null) {
                        SupabaseSyncManager.createNotification(
                            targetUserId = user.id,
                            title = notif.title,
                            message = notif.message,
                            targetType = notif.targetType,
                            targetId = notif.targetId,
                            role = notif.role
                        ).onFailure { e ->
                            Log.w("SomadhanRepo", "deleteCategory: skill-cascade notification dual-write failed for ${user.id} (local flow unaffected): ${e.message}")
                        }
                    }
                }
            }
        }

        // [SUPABASE-MIGRATED - ধাপ ৪] cascade-এর bulk dual-write -- `admin_remove_category_from_solvers`
        // RPC একবারেই (loop-এর বাইরে, N-বার কল না করে) সব affected solver-এর `users.solver_categories`
        // থেকে এই category_id বাদ দেয়, ঠিক `runMonthlyFreeQuotaReset()`-এর bulk প্যাটার্নে। শুধু
        // তখনই কল হচ্ছে যখন উপরের লুপে অন্তত একজন solver affected হয়েছে (অহেতুক কল এড়াতে)।
        if (cascadeAffectedAny && SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminRemoveCategoryFromSolvers(categoryId).onFailure { e ->
                Log.w("SomadhanRepo", "deleteCategory: bulk solver-cascade dual-write failed for $categoryId (local flow unaffected): ${e.message}")
            }
        }
    }

    suspend fun canFavoriteSolver(userId: String, solverId: String): Boolean {
        if (userId.isBlank() || solverId.isBlank() || userId == solverId) return false
        val completedVirtualJobs = problemDao.getCompletedProblemsBetween(userId, solverId)
        return completedVirtualJobs.any { !it.isPhysical }
    }

    suspend fun getCompletedProblemsBetween(userId: String, solverId: String): List<ProblemEntity> {
        return problemDao.getCompletedProblemsBetween(userId, solverId)
    }

    suspend fun toggleFavoriteSolver(userId: String, solverId: String): Boolean {
        val user = userDao.getUserById(userId) ?: return false
        val currentList = user.favoriteSolverIds.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        val isNowFavorite: Boolean
        if (currentList.contains(solverId)) {
            currentList.remove(solverId)
            isNowFavorite = false
        } else {
            currentList.add(solverId)
            isNowFavorite = true
        }
        val updatedString = currentList.joinToString(",")
        val updatedUser = user.copy(favoriteSolverIds = updatedString, updatedAt = System.currentTimeMillis())
        userDao.updateFavoriteSolverIds(userId, updatedString, updatedUser.updatedAt)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি। নতুন RPC/wrapper
        // লাগেনি — বিদ্যমান `updateOwnProfile()` (ধাপ ১৪) এর favoriteSolverIds প্যারামিটারই
        // এই কলাম কভার করে (RLS `users_update_own` কালার নিজের id-তে UPDATE এমনিতেই permit করে)।
        if (SupabaseAuthManager.currentUserId() == userId) {
            SupabaseSyncManager.updateOwnProfile(userId = userId, favoriteSolverIds = updatedString).onFailure { e ->
                Log.w("SomadhanRepo", "toggleFavoriteSolver: Supabase dual-write failed for $userId (local flow unaffected): ${e.message}")
            }
        }
        return isNowFavorite
    }

    suspend fun addFavoriteSolver(userId: String, solverId: String): Boolean {
        val user = userDao.getUserById(userId) ?: return false
        val currentList = user.favoriteSolverIds.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        if (currentList.contains(solverId)) {
            return false // Already added
        }
        currentList.add(solverId)
        val updatedString = currentList.joinToString(",")
        val updatedUser = user.copy(favoriteSolverIds = updatedString, updatedAt = System.currentTimeMillis())
        userDao.updateFavoriteSolverIds(userId, updatedString, updatedUser.updatedAt)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি। নতুন RPC/wrapper
        // লাগেনি — বিদ্যমান `updateOwnProfile()` (ধাপ ১৪) এর favoriteSolverIds প্যারামিটারই
        // এই কলাম কভার করে (RLS `users_update_own` কালার নিজের id-তে UPDATE এমনিতেই permit করে)।
        if (SupabaseAuthManager.currentUserId() == userId) {
            SupabaseSyncManager.updateOwnProfile(userId = userId, favoriteSolverIds = updatedString).onFailure { e ->
                Log.w("SomadhanRepo", "addFavoriteSolver: Supabase dual-write failed for $userId (local flow unaffected): ${e.message}")
            }
        }
        return true
    }

    suspend fun getFavoriteSolvers(userId: String): List<UserEntity> {
        val user = userDao.getUserById(userId) ?: return emptyList()
        val ids = user.favoriteSolverIds.split(",").map { it.trim() }.filter { it.isNotBlank() }
        return ids.mapNotNull { userDao.getUserById(it) }
    }

    // ============================================================
    // Loading/Sync Fix Roadmap v2, ধাপ ২ — bulk-pull-এর বাইরে থাকা ডাটার জন্য SyncPhase সিগন্যাল।
    //
    // কোড ঘেঁটে (repository/SupabaseSyncManager/SupabaseRealtimeManager) যাচাই করা বাস্তবতা,
    // যেটা ধাপ ২ প্রম্পটের প্রাথমিক অনুমান থেকে ভিন্ন -- categories/faqs/platform_settings/
    // admin_audit_logs/ratings-এর একটারও Supabase থেকে Room-এ pull-করা bulk-fetch ফাংশন নেই:
    //   - categories: সম্পূর্ণ locally seed হয় (নিচে ensureCategoriesSeeded()) -- network লাগে না।
    //     (SupabaseSyncManager.getAllCategories() টেবিল থেকে read করে ঠিকই, কিন্তু এটা কোথাও
    //     কল হয় না -- dead code, verify করা হয়েছে।)
    //   - faqs: একইভাবে সম্পূর্ণ locally seed হয় (নিচে ensureFaqsSeeded()) -- network লাগে না।
    //     (SupabaseSyncManager.getAllFaqs() একইভাবে unused dead code।)
    //   - platform_settings: শুধু local Room read (platformSettingDao.getSetting) + local write
    //     (updatePlatformSetting দিয়ে, dual-write সহ) -- কোনো bulk cloud->Room pull নেই, তাই
    //     এই ডিভাইসে অন্য ডিভাইসের করা সেটিং পরিবর্তন কখনো নিজে থেকে আসে না (এটা একটা
    //     pre-existing gap, এই রোডম্যাপের স্কোপের বাইরে -- এখানে শুধু signal যোগ হচ্ছে)।
    //   - admin_audit_logs: শুধু local insert (logAdminAction) + cloud dual-write -- কোনো
    //     cloud->Room read-back নেই (AdminAuditLogView সবসময় শুধু এই ডিভাইসের নিজের লগ দেখায়)।
    //   - ratings: শুধু local insert (submitUserRatingForSolver/submitSolverRatingForUser) --
    //     অন্য ইউজারের দেওয়া rating pull করে আনার কোনো ফাংশন/realtime channel নেই।
    //   - cancelled_bids: আলাদা কোনো টেবিল/ফাংশন না -- AdminCancelledBidsView Room-এর `bids`
    //     টেবিল থেকেই status="CANCELLED" ফিল্টার করে (getAllCancelledBids()), আর `bids` টেবিল
    //     ইতিমধ্যেই ধাপ ১-এর pullBulkDataFromSupabase()-এর অংশ -- তাই এর জন্য আলাদা SyncPhase
    //     না বানিয়ে বিদ্যমান SupabaseRealtimeManager.initialSyncPhase-ই পুনরায় ব্যবহার করা হবে
    //     (UI-স্ক্রিন ধাপ ৩/৪-এ সেটাই দেখবে)।
    //
    // যেহেতু categories/faqs/platform_settings/admin_audit_logs/ratings-এর কোনোটাতেই আসল
    // নেটওয়ার্ক কল নেই, তাই withTimeout(12_000L)-এর কোনো ব্যবহারিক প্রয়োজন নেই (timeout শুধু
    // network hang ধরার জন্য, আর এখানে network-ই নেই) -- categories-এর জন্য মূল প্রম্পটে যে
    // যুক্তি দেওয়া হয়েছিল ("local-only, প্রায় সাথে সাথেই LOADED") ঠিক একই যুক্তি বাকি চারটার
    // জন্যও প্রযোজ্য। প্রতিটাই তাদের নিজ নিজ (স্থানীয়) কাজ শুরুর আগে LOADING আর কাজ শেষে/ব্যর্থ
    // হলে LOADED/ERROR সেট করে -- সিগন্যাল-শেপ ভবিষ্যতে সত্যিকারের নেটওয়ার্ক কল যোগ হলেও অক্ষত
    // থাকবে।
    // ============================================================
    private val _categoriesSyncPhase = MutableStateFlow(SupabaseRealtimeManager.SyncPhase.LOADING)
    val categoriesSyncPhase: StateFlow<SupabaseRealtimeManager.SyncPhase> = _categoriesSyncPhase.asStateFlow()

    private val _faqsSyncPhase = MutableStateFlow(SupabaseRealtimeManager.SyncPhase.LOADING)
    val faqsSyncPhase: StateFlow<SupabaseRealtimeManager.SyncPhase> = _faqsSyncPhase.asStateFlow()

    // platform_settings/admin_audit_logs/ratings-এর জন্য এই মুহূর্তে সত্যিকারের কোনো async
    // seed/fetch ফাংশন নেই (উপরের ব্যাখ্যা দেখুন) -- তাই এদের phase শুধু repository init-এই
    // সরাসরি LOADED-এ বসানো হলো (স্থানীয় Room read সবসময় সাথে সাথেই প্রস্তুত)। ভবিষ্যতে
    // এদের জন্য সত্যিকারের cloud->Room pull যোগ হলে, ধাপ ১/categories-এর প্যাটার্ন অনুসরণ করে
    // এখানে LOADING/withTimeout/LOADED-ERROR বসানো যাবে -- বাইরের API (StateFlow) বদলাবে না।
    private val _platformSettingsSyncPhase = MutableStateFlow(SupabaseRealtimeManager.SyncPhase.LOADED)
    val platformSettingsSyncPhase: StateFlow<SupabaseRealtimeManager.SyncPhase> = _platformSettingsSyncPhase.asStateFlow()

    private val _auditLogsSyncPhase = MutableStateFlow(SupabaseRealtimeManager.SyncPhase.LOADED)
    val auditLogsSyncPhase: StateFlow<SupabaseRealtimeManager.SyncPhase> = _auditLogsSyncPhase.asStateFlow()

    private val _ratingsSyncPhase = MutableStateFlow(SupabaseRealtimeManager.SyncPhase.LOADED)
    val ratingsSyncPhase: StateFlow<SupabaseRealtimeManager.SyncPhase> = _ratingsSyncPhase.asStateFlow()

    // Loading/Sync Fix Roadmap v2, ধাপ ৫ (duplicate/racing-fetch guard) — [_categoriesSyncPhase]-এর
    // initial value নিজেই LOADING (fetch শুরুর আগেই), তাই phase-কে নিজেই guard হিসেবে ব্যবহার
    // করলে প্রথম কলটাই ভুলভাবে স্কিপ হয়ে যেত -- তাই আলাদা "in flight" ফ্ল্যাগ, একই কারণে
    // SupabaseRealtimeManager.initialSyncInFlight-এর মতো।
    private val categoriesSyncInFlight = java.util.concurrent.atomic.AtomicBoolean(false)

    suspend fun ensureCategoriesSeeded() {
        if (!categoriesSyncInFlight.compareAndSet(false, true)) {
            Log.d("SomadhanRepo", "ensureCategoriesSeeded: already in flight — skipping duplicate call")
            return
        }
        try {
            _categoriesSyncPhase.value = SupabaseRealtimeManager.SyncPhase.LOADING
            try {
                ensureCategoriesSeededInternal()
                _categoriesSyncPhase.value = SupabaseRealtimeManager.SyncPhase.LOADED
            } catch (e: Exception) {
                _categoriesSyncPhase.value = SupabaseRealtimeManager.SyncPhase.ERROR
                throw e
            }
        } finally {
            categoriesSyncInFlight.set(false)
        }
    }

    private suspend fun ensureCategoriesSeededInternal() {
        val existing = categoryDao.getAllCategories().firstOrNull() ?: emptyList()
        if (existing.isEmpty()) {
            val defaultCategories = listOf(
                CategoryEntity(
                    id = "CAT_ELEC",
                    nameBangla = "ইলেকট্রিশিয়ান ও ওয়্যারিং",
                    nameEnglish = "Electrician & Wiring",
                    isPhysical = true,
                    iconName = "Bolt",
                    keywords = "কারেন্ট,ফ্যান,সুইচ,ওয়্যারিং,শর্ট সার্কিট,বাল্ব,বিদ্যুৎ,wiring,switch,fan,circuit,ac,light,fuse,meter,voltage",
                    minBudget = 200.0,
                    maxBudget = 2000.0,
                    instantJobEnabled = true,
                    instantJobRadiusKm = 5.0
                ),
                CategoryEntity(
                    id = "CAT_PLUMB",
                    nameBangla = "প্লাম্বিং ও পাইপফিটিং",
                    nameEnglish = "Plumbing & Pipefitting",
                    isPhysical = true,
                    iconName = "Plumbing",
                    keywords = "পানি,ট্যাপ,পাইপ,বেসিন,প্লাম্বার,লিক,লাইল,pipe,leak,tap,basin,commode,faucet,drainage,water",
                    minBudget = 250.0,
                    maxBudget = 2500.0,
                    instantJobEnabled = true,
                    instantJobRadiusKm = 5.0
                ),
                CategoryEntity(
                    id = "CAT_TECH",
                    nameBangla = "কম্পিউটার ও আইটি সাপোর্ট",
                    nameEnglish = "Computer & IT Support",
                    isPhysical = false,
                    iconName = "Computer",
                    keywords = "উইন্ডোজ,ল্যাপটপ,কম্পিউটার,পিসি,সফটওয়্যার,ইন্সটল,প্রিন্টার,windows,laptop,computer,pc,software,install,slow,ram,ssd,display,printer",
                    minBudget = 300.0,
                    maxBudget = 3000.0,
                    instantJobEnabled = false,
                    instantJobRadiusKm = 5.0
                ),
                CategoryEntity(
                    id = "CAT_AC",
                    nameBangla = "এসি ও রেফ্রিজারেটর সার্ভিসিং",
                    nameEnglish = "AC & Refrigerator Servicing",
                    isPhysical = true,
                    iconName = "AcUnit",
                    keywords = "এসি,ফ্রিজ,গ্যাস,ঠান্ডা,কুলিং,সার্ভিসিং,ac,fridge,cooling,gas,repair,inverter",
                    minBudget = 500.0,
                    maxBudget = 5000.0,
                    instantJobEnabled = true,
                    instantJobRadiusKm = 5.0
                ),
                CategoryEntity(
                    id = "CAT_HOME",
                    nameBangla = "গৃহস্থালি মেরামত ও শিফটিং",
                    nameEnglish = "Home Repair & Shifting",
                    isPhysical = true,
                    iconName = "HomeRepairService",
                    keywords = "শিফটিং,রং,ফার্নিচার,কাঠমিস্ত্রি,মেরামত,carpenter,paint,furniture,drill,shifting,door,lock",
                    minBudget = 400.0,
                    maxBudget = 4000.0,
                    instantJobEnabled = true,
                    instantJobRadiusKm = 5.0
                ),
                CategoryEntity(
                    id = "CAT_GRAPHIC",
                    nameBangla = "গ্রাফিক ডিজাইন ও ফটো এডিট",
                    nameEnglish = "Graphic Design & Photo Edit",
                    isPhysical = false,
                    iconName = "DesignServices",
                    keywords = "লোগো,ব্যানার,পোস্টার,ছবি,এডিটিং,ডিজাইন,logo,banner,poster,photo,photoshop,illustrator,canva,thumbnail",
                    minBudget = 200.0,
                    maxBudget = 2500.0,
                    instantJobEnabled = false,
                    instantJobRadiusKm = 5.0
                ),
                CategoryEntity(
                    id = "CAT_WEB",
                    nameBangla = "ওয়েব ডেভেলপমেন্ট ও কোডিং",
                    nameEnglish = "Web Development & Coding",
                    isPhysical = false,
                    iconName = "Code",
                    keywords = "ওয়েবসাইট,কোডিং,প্রোগ্রামিং,ওয়েব,এইচটিএমএল,website,web,html,css,javascript,react,wordpress,backend,database,app",
                    minBudget = 500.0,
                    maxBudget = 10000.0,
                    instantJobEnabled = false,
                    instantJobRadiusKm = 5.0
                ),
                CategoryEntity(
                    id = "CAT_TUTOR",
                    nameBangla = "হোম ও অনলাইন টিউশন",
                    nameEnglish = "Home & Online Tuition",
                    isPhysical = true,
                    iconName = "School",
                    keywords = "টিউশন,পড়ানো,টিচার,ইংরেজি,গণিত,টিউটর,tuition,teacher,math,english,science,physics,study",
                    minBudget = 500.0,
                    maxBudget = 6000.0,
                    instantJobEnabled = true,
                    instantJobRadiusKm = 5.0
                )
            )
            categoryDao.insertCategories(defaultCategories)
        } else {
            // Auto-heal existing categories if none are flagged for instant jobs
            if (existing.none { it.instantJobEnabled }) {
                existing.forEach { cat ->
                    if (cat.isPhysical) {
                        categoryDao.updateCategory(cat.copy(instantJobEnabled = true))
                    }
                }
            }
        }
    }

    // ---------------- FAQ (Frequently Asked Questions) ----------------

    fun getActiveFaqs(): Flow<List<FaqEntity>> = faqDao.getActiveFaqs()

    fun getActiveFaqsForAudience(audience: String): Flow<List<FaqEntity>> = faqDao.getActiveFaqsForAudience(audience)

    fun getAllFaqsForAdmin(): Flow<List<FaqEntity>> = faqDao.getAllFaqs()

    fun getAllFaqsForAudience(audience: String): Flow<List<FaqEntity>> = faqDao.getAllFaqsForAudience(audience)

    suspend fun getFaqById(id: String): FaqEntity? = faqDao.getFaqById(id)

    private fun FaqEntity.toSupabaseDto() = FaqDto(
        id = id,
        question = question,
        answer = answer,
        targetAudience = targetAudience,
        displayOrder = displayOrder,
        isActive = isActive,
        createdAt = Instant.fromEpochMilliseconds(createdAt).toString()
    )

    suspend fun addFaq(
        question: String,
        answer: String,
        targetAudience: String = "USER",
        displayOrder: Int = 0,
        isActive: Boolean = true
    ): FaqEntity {
        val faq = FaqEntity(
            id = "FAQ_${UUID.randomUUID().toString().take(8)}",
            question = question.trim(),
            answer = answer.trim(),
            targetAudience = targetAudience,
            displayOrder = displayOrder,
            isActive = isActive,
            createdAt = System.currentTimeMillis()
        )
        faqDao.insertFaq(faq)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি। `faqs` টেবিলে
        // RLS পলিসি `faqs_admin_write` (ALL, is_admin(auth.uid())) ইতিমধ্যেই admin-only লেখা নিশ্চিত
        // করে (Supabase MCP দিয়ে verify করা) — তাই RPC লাগেনি, সরাসরি upsert()।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.upsertFaq(faq.toSupabaseDto()).onFailure { e ->
                Log.w("SomadhanRepo", "addFaq: Supabase dual-write failed for ${faq.id} (local flow unaffected): ${e.message}")
            }
        }
        logAdminAction("ADD_FAQ", faq.id, faq.question, "FAQ ($targetAudience) তৈরি করা হয়েছে")
        return faq
    }

    suspend fun updateFaq(faq: FaqEntity) {
        faqDao.updateFaq(faq)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি। RLS `faqs_admin_write`
        // পলিসি দিয়ে কভার্ড।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.upsertFaq(faq.toSupabaseDto()).onFailure { e ->
                Log.w("SomadhanRepo", "updateFaq: Supabase dual-write failed for ${faq.id} (local flow unaffected): ${e.message}")
            }
        }
        logAdminAction("UPDATE_FAQ", faq.id, faq.question, "FAQ আপডেট করা হয়েছে")
    }

    suspend fun deleteFaq(faq: FaqEntity) {
        faqDao.deleteFaq(faq)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি। RLS `faqs_admin_write`
        // পলিসি দিয়ে কভার্ড।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.deleteFaqRow(faq.id).onFailure { e ->
                Log.w("SomadhanRepo", "deleteFaq: Supabase dual-write failed for ${faq.id} (local flow unaffected): ${e.message}")
            }
        }
        logAdminAction("DELETE_FAQ", faq.id, faq.question, "FAQ মুছে ফেলা হয়েছে")
    }

    suspend fun deleteFaqById(id: String) {
        faqDao.deleteFaqById(id)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি। RLS `faqs_admin_write`
        // পলিসি দিয়ে কভার্ড।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.deleteFaqRow(id).onFailure { e ->
                Log.w("SomadhanRepo", "deleteFaqById: Supabase dual-write failed for $id (local flow unaffected): ${e.message}")
            }
        }
        logAdminAction("DELETE_FAQ", id, id, "FAQ আইডি দিয়ে মুছে ফেলা হয়েছে")
    }

    // Loading/Sync Fix Roadmap v2, ধাপ ৫ (duplicate/racing-fetch guard) — categoriesSyncInFlight-এর
    // মতোই আলাদা "in flight" ফ্ল্যাগ, phase নিজেই guard হিসেবে ব্যবহার করা হয়নি একই কারণে
    // (initial value LOADING)।
    private val faqsSyncInFlight = java.util.concurrent.atomic.AtomicBoolean(false)

    suspend fun ensureFaqsSeeded() {
        if (!faqsSyncInFlight.compareAndSet(false, true)) {
            Log.d("SomadhanRepo", "ensureFaqsSeeded: already in flight — skipping duplicate call")
            return
        }
        try {
            _faqsSyncPhase.value = SupabaseRealtimeManager.SyncPhase.LOADING
            try {
                ensureFaqsSeededInternal()
                _faqsSyncPhase.value = SupabaseRealtimeManager.SyncPhase.LOADED
            } catch (e: Exception) {
                _faqsSyncPhase.value = SupabaseRealtimeManager.SyncPhase.ERROR
                throw e
            }
        } finally {
            faqsSyncInFlight.set(false)
        }
    }

    private suspend fun ensureFaqsSeededInternal() {
        val existing = faqDao.getAllFaqs().firstOrNull() ?: emptyList()
        if (existing.isEmpty()) {
            val defaultFaqs = listOf(
                // User FAQs
                FaqEntity(
                    id = "FAQ_USER_01",
                    question = "কীভাবে সমস্যা পোস্ট করব?",
                    answer = "হোম স্ক্রিনের নিচে 'সমস্যা পোস্ট' বাটনে চাপ দিন। আপনার সমস্যার ক্যাটাগরি, বিস্তারিত বিবরণ, এলাকা/লোকেশন ও আনুমানিক বাজেট দিয়ে পোস্ট নিশ্চিত করুন।",
                    targetAudience = "USER",
                    displayOrder = 1,
                    isActive = true
                ),
                FaqEntity(
                    id = "FAQ_USER_02",
                    question = "টাকা কখন সলভারকে দেওয়া হয়?",
                    answer = "বিড গ্রহণের পর পেমেন্ট এসক্রোতে নিরাপদে জমা থাকে। কাজ সম্পূর্ণ সন্তোষজনকভাবে শেষ হওয়ার পর আপনার নিশ্চিতকরণ সাপেক্ষে সলভারের একাউন্টে টাকা পৌঁছায়।",
                    targetAudience = "USER",
                    displayOrder = 2,
                    isActive = true
                ),
                FaqEntity(
                    id = "FAQ_USER_03",
                    question = "বিড কীভাবে অ্যাকসেপ্ট করব?",
                    answer = "আপনার পোস্টের বিস্তারিত স্ক্রিনে গিয়ে সলভারদের দেওয়া অফার ও রেটিং দেখুন। পছন্দের বিডের পাশে 'অ্যাকসেপ্ট করুন' বোতামে চাপুন।",
                    targetAudience = "USER",
                    displayOrder = 3,
                    isActive = true
                ),
                FaqEntity(
                    id = "FAQ_USER_04",
                    question = "রিফান্ড কীভাবে পাব?",
                    answer = "সলভার কাজ না করলে বা কোনো অনাকাঙ্ক্ষিত জটিলতা তৈরি হলে সাপোর্ট সেন্টারে যোগাযোগ করে অথবা বিরোধ (Dispute) নিষ্পত্তির মাধ্যমে সরাসরি ওয়ালেটে রিফান্ড পেতে পারেন।",
                    targetAudience = "USER",
                    displayOrder = 4,
                    isActive = true
                ),
                // Solver FAQs
                FaqEntity(
                    id = "FAQ_SOLVER_01",
                    question = "সলভার হিসেবে কীভাবে কাজে বিড করব?",
                    answer = "হোম ফিডে উন্মুক্ত সমস্যাগুলো দেখুন। আপনার দক্ষতা অনুযায়ী কাজের বিস্তারিত পেজে গিয়ে প্রস্তাবিত মূল্য ও আনুমানিক সময় দিয়ে বিড সাবমিট করুন।",
                    targetAudience = "SOLVER",
                    displayOrder = 1,
                    isActive = true
                ),
                FaqEntity(
                    id = "FAQ_SOLVER_02",
                    question = "কাজের পেমেন্ট কীভাবে উইথড্র করব?",
                    answer = "কাজ সম্পন্ন হলে এবং গ্রাহক অনুমোদন দিলে কমিশন বাদে নিট অর্থ আপনার ওয়ালেটে জমা হবে। ওয়ালেট পেজ থেকে বিকাশ, নগদ বা রকেটের মাধ্যমে টাকা তোলার রিকোয়েস্ট পাঠাতে পারবেন।",
                    targetAudience = "SOLVER",
                    displayOrder = 2,
                    isActive = true
                ),
                FaqEntity(
                    id = "FAQ_SOLVER_03",
                    question = "বিড করার পর কি বাতিল করা যায়?",
                    answer = "গ্রাহক বিড গ্রহণ করার পূর্ব পর্যন্ত বিড প্রত্যাহার করা যাবে না। তবে গ্রাহক বিড গ্রহণ করে কাজ শুরু করার পর কোনো অনিবার্য কারণে সমস্যা হলে শর্তসাপেক্ষে কাজ বাতিলের ব্যবস্থা রয়েছে।",
                    targetAudience = "SOLVER",
                    displayOrder = 3,
                    isActive = true
                ),
                FaqEntity(
                    id = "FAQ_SOLVER_04",
                    question = "কমিশন কীভাবে কাটা হয়?",
                    answer = "প্ল্যাটফর্মের নির্ধারিত ফি কাজের মোট বাজেট থেকে স্বয়ংক্রিয়ভাবে কর্তন করে অবশিষ্ট সম্পূর্ণ টাকা সলভারের ওয়ালেটে জমা হয়।",
                    targetAudience = "SOLVER",
                    displayOrder = 4,
                    isActive = true
                )
            )
            faqDao.insertFaqs(defaultFaqs)
        }
    }

    // ---------------- PROBLEMS ----------------

    fun getAllProblems(): Flow<List<ProblemEntity>> = problemDao.getAllProblems()
    suspend fun getAllProblemsPage(limit: Int, offset: Int): List<ProblemEntity> =
        problemDao.getAllProblemsPage(limit, offset)

    fun getOpenProblems(): Flow<List<ProblemEntity>> = problemDao.getOpenProblems()
    suspend fun getOpenProblemsPage(limit: Int, offset: Int): List<ProblemEntity> =
        problemDao.getOpenProblemsPage(limit, offset)

    suspend fun getOpenProblemsByCategoryPage(categoryId: String, limit: Int, offset: Int): List<ProblemEntity> =
        problemDao.getOpenProblemsByCategoryPage(categoryId, limit, offset)

    fun getCompletedProblems(): Flow<List<ProblemEntity>> = problemDao.getCompletedProblems()
    suspend fun getCompletedProblemsPage(limit: Int, offset: Int): List<ProblemEntity> =
        problemDao.getCompletedProblemsPage(limit, offset)

    suspend fun getAllDirectContractsPage(limit: Int, offset: Int): List<ProblemEntity> =
        problemDao.getAllDirectContractsPage(limit, offset)

    fun getActiveProblemsByUserId(userId: String): Flow<List<ProblemEntity>> = problemDao.getActiveProblemsByUserId(userId)
    suspend fun getActiveProblemsByUserIdPage(userId: String, limit: Int, offset: Int): List<ProblemEntity> =
        problemDao.getActiveProblemsByUserIdPage(userId, limit, offset)

    fun getCompletedProblemsByUserId(userId: String): Flow<List<ProblemEntity>> = problemDao.getCompletedProblemsByUserId(userId)
    suspend fun getCompletedProblemsByUserIdPage(userId: String, limit: Int, offset: Int): List<ProblemEntity> =
        problemDao.getCompletedProblemsByUserIdPage(userId, limit, offset)

    fun getProblemsByUserId(userId: String): Flow<List<ProblemEntity>> = problemDao.getProblemsByUserId(userId)
    suspend fun getProblemsByUserIdPage(userId: String, limit: Int, offset: Int): List<ProblemEntity> =
        problemDao.getProblemsByUserIdPage(userId, limit, offset)

    fun getCompletedProblemsBySolver(solverId: String): Flow<List<ProblemEntity>> = problemDao.getCompletedProblemsBySolver(solverId)
    suspend fun getCompletedProblemsBySolverPage(solverId: String, limit: Int, offset: Int): List<ProblemEntity> =
        problemDao.getCompletedProblemsBySolverPage(solverId, limit, offset)

    suspend fun getProblemsBySolverPage(solverId: String, limit: Int, offset: Int): List<ProblemEntity> =
        problemDao.getProblemsBySolverPage(solverId, limit, offset)

    suspend fun getProblemById(id: String): ProblemEntity? = problemDao.getProblemById(id)

    fun getProblemByIdFlow(id: String): Flow<ProblemEntity?> = problemDao.getProblemByIdFlow(id)

    suspend fun createProblem(
        user: UserEntity,
        title: String,
        description: String,
        category: CategoryEntity,
        latitude: Double,
        longitude: Double,
        address: String,
        minBudget: Double,
        maxBudget: Double,
        urgency: String
    ): ProblemEntity {
        val currentCommRate = platformSettingDao.getSetting("commission_percent")?.toDoubleOrNull() ?: 10.0
        val problem = ProblemEntity(
            id = "PROB_${UUID.randomUUID().toString().take(8)}",
            userId = user.id,
            userName = user.name,
            userPhone = user.phone,
            categoryId = category.id,
            categoryName = category.nameBangla,
            isPhysical = category.isPhysical,
            title = title.trim(),
            description = description.trim(),
            latitude = latitude,
            longitude = longitude,
            userAddress = address.trim(),
            minBudget = minBudget,
            maxBudget = maxBudget,
            urgency = urgency,
            status = "OPEN",
            appliedCommissionRate = null,
            createdAt = System.currentTimeMillis(),
            lastActivityAt = System.currentTimeMillis()
        )
        problemDao.insertProblem(problem)
        // [SUPABASE-MIGRATED - ধাপ ৮] নিজের row হলে (RLS: auth.uid() = user_id) Supabase-এও insert
        // করে। ব্যর্থ হলেও local Room পথ আগের মতোই কাজ করবে (silent best-effort) -- problem
        // পোস্ট করা user-facing critical path, Supabase network glitch-এ পুরো পোস্ট আটকে থাকা উচিত না।
        if (SupabaseAuthManager.currentUserId() == user.id) {
            SupabaseSyncManager.createProblem(problem.toProblemDto())
        }

        // Notification to user
        notificationDao.insertNotification(
            NotificationEntity(
                role = "USER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = user.id,
                title = "সমস্যা পোস্ট নিশ্চিত হয়েছে",
                message = "\"$title\" সমস্যাটি সফলভাবে পোস্ট হয়েছে। শীঘ্রই সমাধানকারীরা বিড করবেন।",
                targetType = "problem",
                targetId = problem.id,
                relatedProblemId = problem.id
            )
        )
        // [SUPABASE-MIGRATED - ধাপ ২৫] self-notify (caller = target = user.id, `create_notification`
        // RPC-এর `v_caller = p_target_user_id` শর্তে অনুমোদিত, ধাপ ২৪-এ যাচাই করা হয়েছিল)। এই
        // পোস্ট-কনফার্মেশন নোটিফিকেশনের জন্য কোনো নিজস্ব RPC নেই (createProblem শুধু raw problem
        // insert করে), তাই এটা একটা আসল গ্যাপ ছিল -- submitKyc()-এর মতোই ফিক্স করা হলো।
        // best-effort, ব্যর্থ হলে শুধু log হবে।
        if (SupabaseAuthManager.currentUserId() == user.id) {
            SupabaseSyncManager.createNotification(
                targetUserId = user.id,
                title = "সমস্যা পোস্ট নিশ্চিত হয়েছে",
                message = "\"$title\" সমস্যাটি সফলভাবে পোস্ট হয়েছে। শীঘ্রই সমাধানকারীরা বিড করবেন।",
                targetType = "problem",
                targetId = problem.id,
                relatedProblemId = problem.id,
                role = "USER"
            ).onFailure { e ->
                Log.w("SomadhanRepo", "createProblem: createNotification dual-write failed for ${problem.id} (local flow unaffected): ${e.message}")
            }
        }

        val problemPostScore = platformSettingDao.getSetting("rep_score_problem_posted")?.toDoubleOrNull() ?: 0.2
        val problemPostCap = platformSettingDao.getSetting("rep_cap_daily_problem_posted")?.toDoubleOrNull() ?: 2.0
        applyCappedPerEventReputation(user.id, "PROBLEM_POSTED", problemPostScore, defaultDailyCap = problemPostCap, problemId = problem.id, note = "নতুন সমস্যা পোস্ট করেছেন")

        return problem
    }

    suspend fun createDirectContract(
        user: UserEntity,
        solver: UserEntity,
        title: String,
        description: String,
        category: CategoryEntity,
        budget: Double,
        durationDays: Int,
        address: String,
        latitude: Double,
        longitude: Double
    ): ProblemEntity {
        val problemId = "PROB_DIR_${UUID.randomUUID().toString().take(8)}"
        val problem = ProblemEntity(
            id = problemId,
            userId = user.id,
            userName = user.name,
            userPhone = user.phone,
            categoryId = category.id,
            categoryName = category.nameBangla,
            isPhysical = category.isPhysical,
            title = title.trim(),
            description = description.trim(),
            latitude = latitude,
            longitude = longitude,
            userAddress = address.trim(),
            minBudget = budget,
            maxBudget = budget,
            urgency = "সাধারণ",
            status = "OPEN",
            bidsCount = 1,
            acceptedSolverId = solver.id,
            acceptedSolverName = solver.name,
            acceptedAmount = budget,
            createdAt = System.currentTimeMillis(),
            lastActivityAt = System.currentTimeMillis(),
            isDirectContract = true,
            isPublic = false,
            directContractStatus = "PENDING_ACCEPTANCE",
            deadline = "$durationDays দিন"
        )
        problemDao.insertProblem(problem)
        // [SUPABASE-MIGRATED - ধাপ ১২ (মূল কাজ, ব্যাচ ২)] নিজের row হলে (RLS: auth.uid() =
        // user_id) Supabase-এও insert করে — ঠিক normal createProblem()-এর মতোই raw-insert
        // প্যাটার্ন (কোনো নতুন RPC লাগেনি)। best-effort, ব্যর্থ হলে local Room flow অপ্রভাবিত।
        if (SupabaseAuthManager.currentUserId() == user.id) {
            SupabaseSyncManager.createProblem(problem.toProblemDto()).onFailure { e ->
                Log.w("SomadhanRepo", "createDirectContract: Supabase createProblem dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }

        sendMessage(
            problemId = problemId,
            senderId = user.id,
            receiverId = solver.id,
            senderName = user.name,
            content = "সরাসরি প্রজেক্ট প্রস্তাব: $title (বাজেট: ৳${DistanceUtil.toBengaliDigits(budget.toInt().toString())}, মেয়াদ: $durationDays দিন)",
            isDirectContractProposal = true,
            directContractBudget = budget,
            directContractDuration = "$durationDays দিন"
        )

        val notif = NotificationEntity(
            role = "SOLVER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = solver.id,
            title = "নতুন সরাসরি কাজের প্রস্তাব! 🎯",
            message = "${user.name} আপনাকে সরাসরি \"$title\" কাজটি অফার করেছেন (বাজেট: ৳${DistanceUtil.toBengaliDigits(budget.toInt().toString())})। চ্যাটে বিস্তারিত দেখুন।",
            targetType = "problem",
            targetId = problemId,
            relatedProblemId = problemId
        )
        notificationDao.insertNotification(notif)
        // [SUPABASE-MIGRATED - ধাপ ১২ (মূল কাজ, ব্যাচ ২)] `create_notification` RPC দিয়ে
        // solver-কে জানানো হচ্ছে (caller=owner, target=solver, উভয়েই এই problem-এর party হওয়ায়
        // RPC-এর party-check pass করবে)। best-effort।
        if (SupabaseAuthManager.currentUserId() == user.id) {
            SupabaseSyncManager.createNotification(
                targetUserId = solver.id,
                title = "নতুন সরাসরি কাজের প্রস্তাব! 🎯",
                message = "${user.name} আপনাকে সরাসরি \"$title\" কাজটি অফার করেছেন (বাজেট: ৳${DistanceUtil.toBengaliDigits(budget.toInt().toString())})। চ্যাটে বিস্তারিত দেখুন।",
                targetType = "problem",
                targetId = problemId,
                relatedProblemId = problemId,
                role = notif.role
            ).onFailure { e ->
                Log.w("SomadhanRepo", "createDirectContract: createNotification dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }

        return problem
    }

    suspend fun acceptDirectContractProposal(problemId: String, solverId: String): ProblemEntity? {
        val problem = problemDao.getProblemById(problemId) ?: return null
        if (!problem.isDirectContract || problem.acceptedSolverId != solverId) return null
        if (problem.directContractStatus != "PENDING_ACCEPTANCE") return problem

        val solverBeforeResolve = userDao.getUserById(solverId)
        val threshold = platformSettingDao.getSetting("free_quota_reputation_threshold")?.toDoubleOrNull() ?: 80.0
        val currentRate = resolveCommissionRateForNewJob(solverId, problemId)
        val solverAfterResolve = userDao.getUserById(solverId)

        val debugInfo = "🔍 DEBUG (Direct Contract):\n" +
            "reputationScore=${solverBeforeResolve?.reputationScore}\n" +
            "threshold=${threshold}\n" +
            "appliedCommissionRate(problem আগে)=${problem.appliedCommissionRate}\n" +
            "resolvedRate=${currentRate}\n" +
            "freeJobsUsedThisMonth(পরে)=${solverAfterResolve?.freeJobsUsedThisMonth}\n" +
            "freeJobsMonthKey(পরে)=${solverAfterResolve?.freeJobsMonthKey}"
        lastDebugInfo.value = debugInfo

        val updatedProblem = problem.copy(
            directContractStatus = "ACCEPTED",
            status = "IN_PROGRESS",
            appliedCommissionRate = currentRate,
            lastActivityAt = System.currentTimeMillis()
        )
        problemDao.updateProblem(updatedProblem)

        // Open Escrow
        val contractBudget = problem.acceptedAmount ?: problem.minBudget
        val escrow = EscrowEntity(
            id = "ESCROW_${UUID.randomUUID().toString().take(8)}",
            problemId = problem.id,
            problemTitle = problem.title,
            userId = problem.userId,
            solverId = solverId,
            baseAmount = contractBudget
        )
        escrowDao.insert(escrow)

        // Notification to User
        val userNotif = NotificationEntity(
            role = "USER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = problem.userId,
            title = "সরাসরি কাজের প্রস্তাব গৃহীত হয়েছে! 🎉",
            message = "${problem.acceptedSolverName ?: "সমাধানকারী"} আপনার সরাসরি কাজের প্রস্তাব (\"${problem.title}\") গ্রহণ করেছেন। Escrow-তে বাজেট ৳${DistanceUtil.toBengaliDigits(contractBudget.toInt().toString())} সুরক্ষিত আছে এবং কাজ শুরু হয়েছে।",
            targetType = "problem",
            targetId = problem.id,
            relatedProblemId = problem.id
        )
        notificationDao.insertNotification(userNotif)
        // [SUPABASE-MIGRATED - ধাপ ১২ (মূল কাজ, ব্যাচ ২)] `accept_direct_contract` RPC (এই
        // session-এ নতুন migration দিয়ে তৈরি) caller=solver হওয়ায় দরকার (RLS-এ owner ছাড়া কেউ
        // problems update করতে পারে না)। RPC নিজেই problem status/direct_contract_status/
        // applied_commission_rate আপডেট করে, escrow insert করে, আর owner-কে notify করে (উপরের
        // local notif-এর সমতুল্য) — তাই আলাদা createNotification() লাগবে না। guard: caller নিজে
        // solver কিনা (RPC-ও auth.uid()=accepted_solver_id চেক করে, ডাবল-চেক)। best-effort।
        if (SupabaseAuthManager.currentUserId() == solverId) {
            SupabaseSyncManager.acceptDirectContract(problemId, escrowId = escrow.id).onFailure { e ->
                Log.w("SomadhanRepo", "acceptDirectContractProposal: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
                // [Step 12.8a] outbox retry -- idempotency (migration recovered_bidding_contracts.sql
                // থেকে যাচাই): accept_direct_contract problem row `for update` লক করে,
                // direct_contract_status <> 'PENDING_ACCEPTANCE' হলে ALREADY_PROCESSED ফেরত দেয় (কোনো
                // নতুন escrow insert হয় না) -- তাই একই RPC replay দুইবার চললেও escrow দুইবার খোলে না।
                // paramsJson key "problemId" -- OutboxRpcDispatcher.kt-এর "accept_direct_contract"
                // branch-এর সাথে অক্ষরে অক্ষরে মিলছে।
                enqueueOutboxRetry(
                    rpcName = "accept_direct_contract",
                    params = kotlinx.serialization.json.JsonObject(
                        mapOf(
                            "problemId" to kotlinx.serialization.json.JsonPrimitive(problemId),
                            // [Step 12.11] ঐচ্ছিক "escrowId" -- OutboxRpcDispatcher-এর accept_direct_contract branch পড়ে
                            "escrowId" to kotlinx.serialization.json.JsonPrimitive(escrow.id)
                        )
                    ),
                    error = e
                )
            }
        }

        // Send status confirmation in chat
        sendMessage(
            problemId = problemId,
            senderId = solverId,
            receiverId = problem.userId,
            senderName = problem.acceptedSolverName ?: "সমাধানকারী",
            content = "✅ আমি সরাসরি কাজের চুক্তি গ্রহণ করেছি। কাজ শুরু করা হলো।"
        )

        val bidScore = platformSettingDao.getSetting("rep_score_bid_won")?.toDoubleOrNull() ?: 0.5
        val bidCap = platformSettingDao.getSetting("rep_cap_daily_bid_won")?.toDoubleOrNull() ?: 2.0
        applyCappedPerEventReputation(solverId, "BID_WON", bidScore, defaultDailyCap = bidCap, problemId = problem.id, note = "সরাসরি প্রজেক্ট চুক্তি গ্রহণ করেছেন")
        return updatedProblem
    }

    suspend fun declineDirectContractProposal(problemId: String, solverId: String, reason: String = ""): ProblemEntity? {
        val problem = problemDao.getProblemById(problemId) ?: return null
        if (!problem.isDirectContract || problem.acceptedSolverId != solverId) return null
        if (problem.directContractStatus != "PENDING_ACCEPTANCE") return problem

        val updatedProblem = problem.copy(
            directContractStatus = "DECLINED",
            status = "CANCELLED",
            lastActivityAt = System.currentTimeMillis()
        )
        problemDao.updateProblem(updatedProblem)

        val reasonText = if (reason.isNotBlank()) " (কারণ: $reason)" else ""

        // Notification to User
        val userNotif = NotificationEntity(
            role = "USER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = problem.userId,
            title = "সরাসরি কাজের প্রস্তাব প্রত্যাখ্যাত ❌",
            message = "${problem.acceptedSolverName ?: "সমাধানকারী"} আপনার সরাসরি কাজের প্রস্তাব (\"${problem.title}\") প্রত্যাখ্যান করেছেন$reasonText।",
            targetType = "problem",
            targetId = problem.id,
            relatedProblemId = problem.id
        )
        notificationDao.insertNotification(userNotif)
        // [SUPABASE-MIGRATED - ধাপ ১২ (মূল কাজ, ব্যাচ ২)] `decline_direct_contract` RPC
        // caller=solver হওয়ায় দরকার। RPC নিজেই problem status/direct_contract_status আপডেট করে
        // আর owner-কে notify করে (উপরের local notif-এর সমতুল্য) — আলাদা createNotification()
        // লাগবে না। guard: caller নিজে solver কিনা। best-effort।
        if (SupabaseAuthManager.currentUserId() == solverId) {
            SupabaseSyncManager.declineDirectContract(problemId, reason).onFailure { e ->
                Log.w("SomadhanRepo", "declineDirectContractProposal: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }

        // Send decline notice in chat
        sendMessage(
            problemId = problemId,
            senderId = solverId,
            receiverId = problem.userId,
            senderName = problem.acceptedSolverName ?: "সমাধানকারী",
            content = "❌ আমি আন্তরিকভাবে দুঃখিত, এই সরাসরি কাজের প্রস্তাবটি গ্রহণ করা সম্ভব হচ্ছে না$reasonText।"
        )

        return updatedProblem
    }

    suspend fun reportUser(reporterId: String, reportedUserId: String, reason: String, problemId: String? = null) {
        val reporter = userDao.getUserById(reporterId)
        val reported = userDao.getUserById(reportedUserId)
        logAdminAction(
            actionType = "REPORT_USER",
            targetId = reportedUserId,
            targetName = reported?.name ?: reportedUserId,
            details = "রিপোর্টকারী: ${reporter?.name ?: reporterId}, কারণ: $reason${if (problemId != null) ", রেফারেন্স: $problemId" else ""}"
        )
    }

    suspend fun deleteProblem(problemId: String) {
        val problem = problemDao.getProblemById(problemId)
        problemDao.deleteProblem(problemId)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮] Local Room/Firestore পাশে hard-delete অক্ষত থাকছে
        // (উপরের দুই লাইন অপরিবর্তিত)। Supabase পাশে এটা **soft-delete** (is_user_deleted=true) --
        // কারণ bids/escrows/messages/ratings/additional_charges টেবিলের FK (NO ACTION)
        // problems(id)-কে রেফারেন্স করে, তাই literal hard-delete FK ভায়োলেশন দেবে যেকোনো
        // activity-থাকা সমস্যার জন্য। এই বিচ্যুতি ইচ্ছাকৃত ও নথিভুক্ত (SupabaseSyncManager.kt দেখুন)।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminSoftDeleteProblem(problemId).onFailure { e ->
                Log.w("SomadhanRepo", "adminSoftDeleteProblem: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }

        // [SUPABASE-MIGRATED - Admin Action bug-fix master prompt, ধাপ ৮] owner-কে আগে কোনো
        // notification যেত না। এখানে UI/ViewModel-এর কোনো call-site থেকে কোনো "কারণ" পাস করা হয়
        // না (তিনটা call-site-ই শুধু problemId পাঠায়), তাই generic বার্তা -- ভবিষ্যতে কোনো
        // call-site কারণ পাস করতে শুরু করলে এখানে যোগ করা যাবে। problem null হলে (owner
        // resolve করার উপায় নেই) notification skip হবে, বাকি সব অপরিবর্তিত।
        if (problem != null) {
            val notif = NotificationEntity(
                role = "USER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = problem.userId,
                title = "আপনার পোস্ট সরানো হয়েছে ⚠️",
                message = "আপনার সমস্যা পোস্ট \"${problem.title}\" অ্যাডমিন কর্তৃক সরিয়ে দেওয়া হয়েছে।",
                targetType = "problem",
                targetId = problem.id,
                relatedProblemId = problem.id
            )
            notificationDao.insertNotification(notif)

            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.createNotification(
                    targetUserId = notif.userId,
                    title = notif.title,
                    message = notif.message,
                    targetType = notif.targetType,
                    targetId = notif.targetId,
                    relatedProblemId = notif.relatedProblemId,
                    role = notif.role
                ).onFailure { e ->
                    Log.w("SomadhanRepo", "deleteProblem: notif dual-write failed for $problemId (local flow unaffected): ${e.message}")
                }
            }
        }

        logAdminAction(
            actionType = "DELETE_PROBLEM",
            targetId = problemId,
            targetName = problem?.title ?: problemId,
            details = "সমস্যাটি পার্মানেন্টলি ডিলিট করা হয়েছে"
        )
    }

    // ---------------- BIDS ----------------

    fun getBidsForProblem(problemId: String): Flow<List<BidEntity>> = bidDao.getBidsForProblem(problemId)
    suspend fun getBidsForProblemPage(problemId: String, limit: Int, offset: Int): List<BidEntity> =
        bidDao.getBidsForProblemPage(problemId, limit, offset)

    fun getActiveBidsForProblem(problemId: String): Flow<List<BidEntity>> = bidDao.getActiveBidsForProblem(problemId)
    suspend fun getActiveBidsForProblemPage(problemId: String, limit: Int, offset: Int): List<BidEntity> =
        bidDao.getActiveBidsForProblemPage(problemId, limit, offset)

    suspend fun countSolverBidsForProblem(problemId: String, solverId: String): Int = bidDao.countSolverBidsForProblem(problemId, solverId)

    fun getBidsBySolver(solverId: String): Flow<List<BidEntity>> = bidDao.getBidsBySolver(solverId)
    suspend fun getBidsBySolverPage(solverId: String, limit: Int, offset: Int): List<BidEntity> =
        bidDao.getBidsBySolverPage(solverId, limit, offset)

    fun getBidsForSolver(solverId: String): Flow<List<BidEntity>> = bidDao.getBidsForSolver(solverId)
    suspend fun getBidsForSolverPage(solverId: String, limit: Int, offset: Int): List<BidEntity> =
        bidDao.getBidsForSolverPage(solverId, limit, offset)

    fun getAllBids(): Flow<List<BidEntity>> = bidDao.getAllBids()
    suspend fun getAllBidsPage(limit: Int, offset: Int): List<BidEntity> =
        bidDao.getAllBidsPage(limit, offset)

    fun getAllCancelledBids(): Flow<List<BidEntity>> = bidDao.getAllCancelledBids()
    suspend fun getAllCancelledBidsPage(limit: Int, offset: Int): List<BidEntity> =
        bidDao.getAllCancelledBidsPage(limit, offset)

    suspend fun placeBid(
        problem: ProblemEntity,
        solver: UserEntity,
        amount: Double,
        message: String,
        estimatedTime: String
    ): Result<BidEntity> {
        if (problem.isDirectContract) {
            return Result.failure(Exception("সরাসরি চুক্তির কাজে সাধারণ বিড করা যায় না।"))
        }
        val existingBids = bidDao.getBidsForProblemSync(problem.id).filter { it.solverId == solver.id }
        if (existingBids.any { it.status == "CANCELLED" }) {
            return Result.failure(Exception("আপনি পূর্বে এই কাজ বাতিল করেছেন, তাই পুনরায় বিড করা সম্ভব নয়।"))
        }
        if (existingBids.any { it.status != "CANCELLED" }) {
            return Result.failure(Exception("আপনি ইতোমধ্যে এই কাজে বিড জমা দিয়েছেন।"))
        }
        val bid = BidEntity(
            id = "BID_${UUID.randomUUID().toString().take(8)}",
            problemId = problem.id,
            solverId = solver.id,
            solverName = solver.name,
            solverPhone = solver.phone,
            solverRating = 5.0,
            amount = amount,
            message = message.trim(),
            estimatedTime = estimatedTime.trim(),
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        bidDao.insertBid(bid)
        // [SUPABASE-MIGRATED - ধাপ ৮] নিজের (solver-এর) session হলে Supabase-এও bid insert করে।
        // problems.bids_count client থেকে আলাদা update করার দরকার নেই -- সেটা DB-সাইড
        // `handle_new_bid` trigger (ধাপ ৮-এ যোগ করা) নিজেই করে দেয় (RLS-এ solver problem-owner না
        // হওয়ায় client থেকে সরাসরি problems row update করার অনুমতিই নেই)।
        if (SupabaseAuthManager.currentUserId() == solver.id) {
            SupabaseSyncManager.createBid(bid.toBidDto())
        }

        // Increment problem bids count
        val updatedProblem = problem.copy(
            bidsCount = problem.bidsCount + 1,
            lastActivityAt = System.currentTimeMillis()
        )
        problemDao.updateProblem(updatedProblem)

        // Notify problem owner (User)
        notificationDao.insertNotification(
            NotificationEntity(
                role = "USER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = problem.userId,
                title = "নতুন বিড প্রস্তাব এসেছে!",
                message = "${solver.name} আপনার সমস্যা \"${problem.title}\"-এ ৳${amount.toInt()} প্রস্তাব করেছেন।",
                targetType = "problem",
                targetId = problem.id,
                relatedProblemId = problem.id
            )
        )
        // [SUPABASE-MIGRATED - ধাপ ২৫] `create_notification` RPC দিয়ে owner-কে জানানো হচ্ছে।
        // caller = solver.id, target = problem.userId (owner) -- RPC-এর party-check অনুযায়ী,
        // related_problem_id দেওয়া হলে caller নিজে ওই problem-এর owner/accepted-solver/bidder
        // হতে হবে (উপরেই bid insert হয়েছে, তাই bids-এ caller-এর row আছে) আর target owner
        // হওয়ায় target-check-ও pass করবে -- Supabase MCP দিয়ে `create_notification` সোর্স পড়ে
        // এই লজিক যাচাই করা হয়েছে। guard: caller নিজে এই solver কিনা। best-effort।
        if (SupabaseAuthManager.currentUserId() == solver.id) {
            SupabaseSyncManager.createNotification(
                targetUserId = problem.userId,
                title = "নতুন বিড প্রস্তাব এসেছে!",
                message = "${solver.name} আপনার সমস্যা \"${problem.title}\"-এ ৳${amount.toInt()} প্রস্তাব করেছেন।",
                targetType = "problem",
                targetId = problem.id,
                relatedProblemId = problem.id,
                role = "USER"
            ).onFailure { e ->
                Log.w("SomadhanRepo", "placeBid: createNotification dual-write failed for ${bid.id} (local flow unaffected): ${e.message}")
            }
        }

        return Result.success(bid)
    }

    // [SUPABASE-MIGRATED - ধাপ ৮] gatewayTrxId/gateway/gatewayAmount ঐচ্ছিক নতুন প্যারামিটার
    // (default null) -- পুরনো ২-আর্গুমেন্ট call site (ViewModel, এখনো ধাপ ১৪-এ wire হয়নি)
    // অপরিবর্তিত থাকবে, compile ভাঙবে না। নিচের local Room লজিক (walletDeduction,
    // openEscrow ইত্যাদি) হুবহু আগের মতোই আছে, স্পর্শ করা হয়নি -- এটাই এখনো authoritative
    // path। শুধু openEscrow()-এর পরে, নিজের row হলে, Supabase-এর `accept_bid` RPC-কে
    // best-effort dual-write হিসেবে কল করা হয়েছে (নিচে দেখুন)। RPC নিজে থেকেই partial-wallet
    // + gateway-shortfall লজিক (ব্যবহারকারীর সিদ্ধান্ত অনুযায়ী, পুরনো Firebase system-এর সাথে
    // সামঞ্জস্যপূর্ণ) মেনে চলে -- gatewayTrxId/gatewayAmount ViewModel/UI থেকে তখনই আসবে যখন
    // ধাপ ১৪-এ acceptBid wiring হবে (MerchantPaymentDialog-এর success callback থেকে)। এখন এই
    // দুটো না-পাঠানো অবস্থায়, shortfall থাকলে Supabase RPC `INSUFFICIENT_BALANCE` ফেরত দেবে --
    // এটা silently log হবে, local Room flow-কে প্রভাবিত করবে না।
    suspend fun acceptBid(
        problem: ProblemEntity,
        bid: BidEntity,
        gatewayTrxId: String? = null,
        gateway: String? = null,
        gatewayAmount: Double? = null
    ): String {
        // Idempotency guard: re-fetch the problem's CURRENT status right before acting, instead
        // of trusting the possibly-stale `problem` object passed in from the UI. Without this,
        // a fast double-tap on "Accept" (or two different bids accepted in quick succession for
        // the same problem, e.g. under app slowness) each ran the FULL flow below independently
        // -- separately deducting the user's wallet (walletDeduction) and minting a brand-new
        // escrow row via openEscrow() every single time -- since nothing here previously checked
        // whether this problem had already moved out of "OPEN". Bailing out here unless the
        // fresh copy is still "OPEN" closes that race at the root.
        val freshProblemForGuard = problemDao.getProblemById(problem.id)
        if (freshProblemForGuard == null || freshProblemForGuard.status != "OPEN") {
            Log.w("SomadhanRepo", "acceptBid: skipped — problem ${problem.id} is already ${freshProblemForGuard?.status ?: "missing"}, refusing to accept bid ${bid.id} again")
            return "ALREADY_ACCEPTED"
        }

        val now = System.currentTimeMillis()

        val solverBeforeResolve = userDao.getUserById(bid.solverId)
        val threshold = platformSettingDao.getSetting("free_quota_reputation_threshold")?.toDoubleOrNull() ?: 80.0
        val currentRate = resolveCommissionRateForNewJob(bid.solverId, problem.id)
        val solverAfterResolve = userDao.getUserById(bid.solverId)

        val debugInfo = "🔍 DEBUG:\n" +
            "reputationScore=${solverBeforeResolve?.reputationScore}\n" +
            "threshold=${threshold}\n" +
            "appliedCommissionRate(problem আগে)=${problem.appliedCommissionRate}\n" +
            "resolvedRate=${currentRate}\n" +
            "freeJobsUsedThisMonth(পরে)=${solverAfterResolve?.freeJobsUsedThisMonth}\n" +
            "freeJobsMonthKey(পরে)=${solverAfterResolve?.freeJobsMonthKey}"
        lastDebugInfo.value = debugInfo

        // Deduct from user's wallet balance whatever amount is available up to bid.amount
        val user = userDao.getUserById(problem.userId)
        // [ব্যালেন্স ফিক্স — ধাপ ৪] আগে এখানে legacy shared user.balance পড়া হতো — ঠিক সেই একই
        // বাগ যেটা accept_bid RPC-তে ধাপ ১-এ ফিক্স হয়েছিল (v_user.balance -> v_user.balance_user),
        // কিন্তু এই Kotlin local mirror-এ মিস হয়ে গিয়েছিল। bid accept সবসময় owner/USER-role
        // টাকা, তাই balanceUser-ই সঠিক সোর্স।
        val userBalance = user?.balanceUser ?: 0.0
        val walletDeduction = if (userBalance > 0.0) minOf(userBalance, bid.amount) else 0.0
        if (walletDeduction > 0.0) {
            // INSTANT local debit (Room); the cloud side goes through the accept_bid RPC
            // dual-write below, which enqueues an outbox retry on failure -- not the
            // pendingCloudSync flag set on the TransactionEntity below, which is a Firebase-era
            // leftover nothing reads anymore [Step 7.7 cleanup]. Still safer than the old
            // incrementUserBalance() fire-and-forget (no retry if that single attempt failed).
            // [ব্যালেন্স ফিক্স] user হিসেবে টাকা কাটা হচ্ছে, তাই balanceUser mirror-ও আপডেট হয়।
            userDao.deductBalanceForUserRole(problem.userId, walletDeduction, now)
        }
        // Bug fix (transaction-history gap): this used to be nested inside `if (walletDeduction
        // > 0.0)` above and used `walletDeduction` as the recorded amount. That meant: whenever
        // the user's wallet balance was 0 (true for most users, since ৳0 is the default starting
        // balance) and the rest of bid.amount was paid straight through the payment gateway,
        // walletDeduction came out as 0.0 -- so this whole block was skipped and NO transaction
        // record was ever written for accepting the bid. The money still correctly reached
        // escrow (openEscrow() below always uses the full bid.amount, independent of
        // walletDeduction), but the user's Transaction History screen showed nothing for the
        // event. Deterministic id keyed on bid.id (unique per accepted bid, guarded above by the
        // problem-status "OPEN" check so this whole function only runs once per bid-accept).
        // Always record the FULL bid.amount here (not just the wallet-covered slice) -- that is
        // the true total the user paid to accept this bid, whether it came from wallet balance,
        // the gateway, or a mix of both; only the actual Room wallet debit above stays limited to
        // walletDeduction, since that's the only part that really left the wallet balance.
        run {
            val deductionTrx = TransactionEntity(
                id = "TRX_BID_DEDUCT_${bid.id}",
                problemId = problem.id,
                problemTitle = problem.title,
                userId = problem.userId,
                // solverId left blank on purpose: this record is a pure user-wallet-ledger entry
                // for the deduction, not a solver earning. Setting a real solverId here would
                // make it match getTransactionsForUser(solverId)'s "solverId = :userId" clause
                // too (since type != "REFUND"), and TransactionHistoryScreen's isEarning check
                // (currentUserId == trx.solverId) would then wrongly render it as a POSITIVE
                // "+" earning for that solver, with the negative netAmount below showing through
                // as a broken "+ ৳ -N" amount. The solver's actual earning is recorded
                // separately (as its own PAYMENT transaction) when the escrow is later released.
                solverId = "",
                grossAmount = bid.amount,
                commissionPercent = 0.0,
                commissionAmount = 0.0,
                netAmount = -bid.amount,
                type = "BID_ACCEPT_DEDUCTION",
                pendingCloudSync = true,
                role = "USER"
            )
            transactionDao.insertTransaction(deductionTrx)
        }

        // Update bid status
        val acceptedBid = bid.copy(status = "ACCEPTED")
        bidDao.updateBid(acceptedBid)

        // Keep all other bids in PENDING (waiting) status so they remain available in the list
        val otherBids = bidDao.getBidsForProblemSync(problem.id)
        otherBids.forEach { other ->
            if (other.id != bid.id && other.status != "CANCELLED") {
                val waitingBid = other.copy(status = "PENDING")
                bidDao.updateBid(waitingBid)
            }
        }

        // Update problem
        val updatedProblem = problem.copy(
            status = "IN_PROGRESS",
            jobStatus = if (problem.isInstantJob) "ACCEPTED" else problem.jobStatus,
            acceptedBidId = bid.id,
            acceptedSolverId = bid.solverId,
            acceptedSolverName = bid.solverName,
            acceptedAmount = bid.amount,
            appliedCommissionRate = currentRate,
            solverCancelledNotice = null,
            onWayAt = null,
            arrivedAt = null,
            jobStartedAt = null,
            completedAt = null,
            solverLiveLat = null,
            solverLiveLng = null,
            solverLiveUpdatedAt = null,
            hasReleaseRequest = false,
            releaseRequestExtraAmount = 0.0,
            releaseRequestNote = "",
            releaseRequestedAt = null,
            pendingExtraAmount = null,
            pendingExtraAmountNote = null,
            pendingExtraAmountRequestedAt = null,
            confirmedExtraAmountTotal = 0.0,
            isDisputed = false,
            disputeReason = null,
            disputeInitiatorId = null,
            disputeSettledAt = null,
            disputeResolutionDecision = null,
            disputeResolutionType = null,
            disputeResolutionNote = null,
            disputeResolvedAt = null,
            disputeProgressAtRaise = null,
            disputeProgressAtSettlement = null,
            lastActivityAt = now
        )
        problemDao.updateProblem(updatedProblem)

        // Open a fresh Escrow for this payment cycle.
        //
        // IMPORTANT: this used to "reuse" the existing escrow row (same id) whenever its status
        // was still non-terminal ("HELD"), on the assumption that a non-terminal escrow could only
        // mean "no cancel/refund has started yet for this problem". That assumption is false: when
        // a solver cancels, the problem's status flips to OPEN immediately (fast local Room write),
        // while the matching refundEscrowOnce() call (Firestore transaction + retries) is still
        // running in the background and can take several seconds. If a new bid is accepted for the
        // SAME problem inside that window, the escrow was still "HELD" at that instant — so the old
        // code reused the SAME escrow id for the new solver. That created two compounding bugs:
        //   1. The still-in-flight refund from the OLD cancellation would later finish and blindly
        //      overwrite this row's status to "REFUNDED" — even though it now belongs to the NEW
        //      solver's active job (fixed separately below via the solverId re-check in
        //      refundEscrowOnce()).
        //   2. Worse: refundEscrowOnce()'s idempotency doc id is deterministic
        //      ("TRX_REFUND_<escrowId>"). Reusing the same escrow id means the NEW solver's future
        //      cancellation would generate the EXACT SAME refund doc id as the OLD solver's refund
        //      that already exists in Firestore/Room — so the new refund is seen as "already
        //      refunded" and silently no-ops: the new solver's escrow gets marked REFUNDED with no
        //      wallet credit at all.
        // Always minting a brand-new escrow id here removes both failure modes at the root: every
        // payment cycle gets its own row and its own refund-doc id, so an old in-flight refund can
        // never collide with, or be mistaken for, a newer one. Old rows become harmless orphans that
        // still get correctly resolved to REFUNDED/RELEASED by their own original caller.
        // [Step 12.11] openEscrow()-এর রিটার্ন আগে ফেলে দেওয়া হতো; এখন local escrow id cloud RPC-তে পাঠানো হয়
        // (accept_bid নতুন `p_escrow_id` প্যারামে সেই id-তেই cloud escrow খোলে) — local id == cloud id, তাই
        // release/refund RPC ESCROW_NOT_FOUND পায় না আর realtime-এ আসা cloud row local row-কে REPLACE করে (duplicate নয়)।
        val openedEscrow = openEscrow(updatedProblem, acceptedBid)

        // [SUPABASE-MIGRATED - ধাপ ৮] নিজের row হলে Supabase-এর `accept_bid` RPC-ও কল করা হয়
        // (best-effort dual-write, ব্যর্থ হলেও উপরের local Room flow ইতিমধ্যে সম্পন্ন,
        // এখানে ব্যর্থতা user-facing error দেখাবে না -- ঠিক createProblem()/placeBid()-এর মতোই
        // প্যাটার্ন)। RPC-এর জবাবে result != "OK" (যেমন INSUFFICIENT_BALANCE বা ALREADY_ACCEPTED)
        // হলেও শুধু log করা হয়, exception ছোঁড়া হয় না -- এই দুই সিস্টেম (local Room+Firebase বনাম
        // Supabase) সাময়িকভাবে race করতে পারে (যেমন একই মুহূর্তে অন্য কোনো ডিভাইস থেকে বিড
        // accept), সেটা এই dual-write bridge পর্যায়ে প্রত্যাশিত, চূড়ান্ত cutover ধাপ ১৪-এর পরে হবে।
        if (SupabaseAuthManager.currentUserId() == problem.userId) {
            SupabaseSyncManager.acceptBid(problem.id, bid.id, gatewayTrxId, gateway, gatewayAmount, escrowId = openedEscrow.id)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK") {
                        Log.w("SomadhanRepo", "acceptBid: Supabase dual-write non-OK result for bid ${bid.id}: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "acceptBid: Supabase dual-write failed for bid ${bid.id} (local flow unaffected): ${e.message}")
                    // [OUTBOX WIRE - Step 12.3 (Pilot)] paramsJson keys "problemId"/"bidId" (+ ঐচ্ছিক
                    // "gatewayTrxId"/"gateway"/"gatewayAmount", শুধু non-null হলে put হয়) --
                    // OutboxRpcDispatcher.kt-এর "accept_bid" branch-এর সাথে অক্ষরে অক্ষরে মিলছে।
                    // idempotency (migration step36 বডি থেকে যাচাই): accept_bid problem row `for update`
                    // লক করে, status <> 'OPEN' হলে ALREADY_ACCEPTED ফেরত দেয় (টাকা নড়ে না) -- তাই
                    // একই accept_bid replay দুইবার চললেও টাকা দুইবার কাটে না। বিস্তারিত ও সীমাবদ্ধতা:
                    // CI_TEST_SUITE_PROGRESS.md "Step 12.3" সেকশন।
                    enqueueOutboxRetry(
                        rpcName = "accept_bid",
                        params = kotlinx.serialization.json.JsonObject(
                            buildMap {
                                put("problemId", kotlinx.serialization.json.JsonPrimitive(problem.id))
                                put("bidId", kotlinx.serialization.json.JsonPrimitive(bid.id))
                                // [Step 12.11] ঐচ্ছিক "escrowId" -- OutboxRpcDispatcher-এর accept_bid branch optionalString("escrowId") পড়ে
                                put("escrowId", kotlinx.serialization.json.JsonPrimitive(openedEscrow.id))
                                gatewayTrxId?.let { put("gatewayTrxId", kotlinx.serialization.json.JsonPrimitive(it)) }
                                gateway?.let { put("gateway", kotlinx.serialization.json.JsonPrimitive(it)) }
                                gatewayAmount?.let { put("gatewayAmount", kotlinx.serialization.json.JsonPrimitive(it)) }
                            }
                        ),
                        error = e
                    )
                }

            // [SUPABASE-MIGRATED - ধাপ ১২ ফিক্স] notification-gap বন্ধ: `accept_bid` RPC নিজে
            // solver-কে notify করে না (ধাপ ১২ audit এ নিশ্চিত হওয়া গ্যাপ), তাই `create_notification`
            // RPC দিয়ে আলাদাভাবে solver-কে জানানো হচ্ছে -- নিচের local Room NotificationEntity
            // (একই title/message) এর সমতুল্য। caller (problem.userId, owner) ও target (bid.solverId,
            // এতক্ষণে accepted_solver) দুজনেই এই problem-এর party, তাই RPC-এর party-check pass করার
            // কথা। Best-effort -- ব্যর্থ হলেও local Room notification আগেই কাজ করছে বলে
            // end-user এখনই টের পাবেন না।
            SupabaseSyncManager.createNotification(
                targetUserId = bid.solverId,
                title = "বিড গৃহীত হয়েছে! 🎉",
                message = "${problem.userName} আপনার ৳${bid.amount.toInt()} মূল্যের প্রস্তাব গ্রহণ করেছেন। কাজ শুরু করুন!",
                targetType = "problem",
                targetId = problem.id,
                relatedProblemId = problem.id,
                role = "SOLVER"
            ).onFailure { e ->
                Log.w("SomadhanRepo", "acceptBid: createNotification dual-write failed for bid ${bid.id} (local flow unaffected): ${e.message}")
            }
        } else {
            // [বাগ ১.৯ ফিক্স] আগে এই else শাখাটা ছিলই না -- currentUserId() != problem.userId হলে
            // পুরো RPC ব্লকটাই silently skip হয়ে যেত: কোনো RPC কল না, কোনো enqueueOutboxRetry() না,
            // এমনকি কোনো log-ও না। ফলে `pending_sync_outbox`-এ কোনো entry তৈরিই হতো না -- আর ১.৩/১.৫/
            // ১.৬/১.৮-এর গার্ড সবগুলোই `pendingSyncOutboxDao.getPending()`-এর উপর নির্ভরশীল, তাই
            // "pending কিছু নেই" দেখে stale cloud balance-কে নিরাপদ ভেবে local-এর উপর overwrite করার
            // অনুমতি দিয়ে দিত -- এটাই ছিল আসল কারণ কেন role-switch **এবং** logout/login দুটোতেই
            // balance ৭৫০-এ ফিরে যাচ্ছিল (ব্যবহারকারী-কনফার্মড: শুধু role-switch না, logout করে আবার
            // login করলেও একই সমস্যা)। ফিক্স: এই identity-mismatch case-টাকেও onFailure-এর মতোই একটা
            // outbox retry entry হিসেবে গণ্য করা হচ্ছে (একই paramsJson shape, OutboxRpcDispatcher-এর
            // accept_bid branch-এর সাথে মিলিয়ে) -- পরে worker/manual-sync currentUserId() ঠিক থাকা
            // অবস্থায় আবার চেষ্টা করবে, আর ততক্ষণ সব pending-outbox গার্ড সঠিকভাবে local balance
            // protect করবে।
            Log.w(
                "SomadhanRepo",
                "acceptBid: currentUserId() (${SupabaseAuthManager.currentUserId()}) != problem.userId (${problem.userId}) at accept-bid time -- skipping direct RPC, enqueueing outbox retry instead so cloud eventually catches up and pending-outbox guards protect local balance"
            )
            enqueueOutboxRetry(
                rpcName = "accept_bid",
                params = kotlinx.serialization.json.JsonObject(
                    buildMap {
                        put("problemId", kotlinx.serialization.json.JsonPrimitive(problem.id))
                        put("bidId", kotlinx.serialization.json.JsonPrimitive(bid.id))
                        put("escrowId", kotlinx.serialization.json.JsonPrimitive(openedEscrow.id))
                        gatewayTrxId?.let { put("gatewayTrxId", kotlinx.serialization.json.JsonPrimitive(it)) }
                        gateway?.let { put("gateway", kotlinx.serialization.json.JsonPrimitive(it)) }
                        gatewayAmount?.let { put("gatewayAmount", kotlinx.serialization.json.JsonPrimitive(it)) }
                    }
                ),
                error = Exception("currentUserId() mismatch at accept_bid time (expected ${problem.userId}, got ${SupabaseAuthManager.currentUserId()})")
            )
        }

        val bidScore = platformSettingDao.getSetting("rep_score_bid_won")?.toDoubleOrNull() ?: 0.5
        val bidCap = platformSettingDao.getSetting("rep_cap_daily_bid_won")?.toDoubleOrNull() ?: 2.0
        applyCappedPerEventReputation(bid.solverId, "BID_WON", bidScore, defaultDailyCap = bidCap, problemId = problem.id, note = "বিড জিতে কাজ পেয়েছেন")

        // Trigger dynamic suggested events if enabled: FAST_RESPONSE_ACCEPTED & REPEAT_CLIENT_HIRE
        val timeToBid = bid.createdAt - problem.createdAt
        val timeToAccept = now - problem.createdAt
        if (timeToBid in 0..(30 * 60 * 1000L) || timeToAccept in 0..(30 * 60 * 1000L)) {
            triggerDynamicReputationEvent(
                userId = bid.solverId,
                eventType = "FAST_RESPONSE_ACCEPTED",
                problemId = problem.id,
                defaultScore = 0.5,
                defaultCap = 1.5,
                isPositive = true,
                customNote = "সমস্যা পোস্টের ৩০ মিনিটের মধ্যে দ্রুত সাড়া দিয়ে বিড গৃহীত হয়েছে"
            )
        }

        val priorJobsWithClient = problemDao.getAllProblems().firstOrNull()?.count {
            it.id != problem.id && it.status == "COMPLETED" && it.userId == problem.userId && it.acceptedSolverId == bid.solverId
        } ?: 0
        if (priorJobsWithClient > 0) {
            triggerDynamicReputationEvent(
                userId = bid.solverId,
                eventType = "REPEAT_CLIENT_HIRE",
                problemId = problem.id,
                defaultScore = 1.0,
                defaultCap = 2.0,
                isPositive = true,
                customNote = "পূর্ববর্তী সন্তুষ্ট ক্লায়েন্ট পুনরায় কাজ প্রদান করেছেন"
            )
        }

        // Notify Solver
        val notif = NotificationEntity(
            role = "SOLVER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = bid.solverId,
            title = "বিড গৃহীত হয়েছে! 🎉",
            message = "${problem.userName} আপনার ৳${bid.amount.toInt()} মূল্যের প্রস্তাব গ্রহণ করেছেন। কাজ শুরু করুন!",
            targetType = "problem",
            targetId = problem.id,
            relatedProblemId = problem.id
        )
        notificationDao.insertNotification(notif)

        // Post chat message notice
        sendMessage(
            problemId = problem.id,
            senderId = problem.userId,
            receiverId = bid.solverId,
            senderName = problem.userName,
            content = "🎉 আমি আপনার ৳${bid.amount.toInt()} মূল্যের প্রস্তাব গ্রহণ করেছি। কাজ শুরু করুন!"
        )

        return debugInfo
    }

    suspend fun withdrawBid(bid: BidEntity) {
        val problem = problemDao.getProblemById(bid.problemId)
        val progressStep = problem?.calculateProgressStep() ?: 1
        val updated = bid.copy(status = "CANCELLED", progressAtCancel = progressStep)
        bidDao.updateBid(updated)
        // [SUPABASE-MIGRATED - ধাপ ৮] `cancel_bid` RPC সোর্স পড়ে যাচাই করা হয়েছে: শুধু নিজের
        // (auth.uid() = solver_id) এখনো PENDING বিড cancel করে, row-locked (`for update`) —
        // কোনো টাকা/escrow জড়িত না (accept হওয়ার আগের বিড)।
        if (SupabaseAuthManager.currentUserId() == bid.solverId) {
            SupabaseSyncManager.cancelBid(bid.id)
        }
    }

    private suspend fun applyCancellationPenalty(solverId: String, problemId: String) {
        val cancelPenalty = platformSettingDao.getSetting("rep_penalty_job_cancelled")?.toDoubleOrNull() ?: 3.0
        applyReputationChange(solverId, "JOB_CANCELLED_BY_SOLVER", -cancelPenalty, problemId, "কাজ অ্যাকসেপ্ট করার পর বাতিল করেছেন")
    }

    suspend fun solverCancelAcceptedJob(problem: ProblemEntity, bid: BidEntity) {
        solverCancelJob(problem.id, bid.solverId, "সমাধানকারী কর্তৃক গৃহীত কাজ বাতিল", reopenAsOpen = true)
    }

    suspend fun requestJobRelease(problem: ProblemEntity, extraAmount: Double, note: String) {
        val now = System.currentTimeMillis()
        val safeExtra = if (extraAmount > 0.0) extraAmount else 0.0
        val updatedProblem = problem.copy(
            hasReleaseRequest = true,
            releaseRequestExtraAmount = safeExtra,
            releaseRequestNote = note.trim(),
            releaseRequestedAt = now,
            lastActivityAt = now
        )
        problemDao.updateProblem(updatedProblem)

        // [SUPABASE-MIGRATED - ধাপ ১২ (মূল কাজ, ব্যাচ ৩)] `request_job_release` RPC caller=solver
        // হওয়ায় দরকার (problems_update_owner শুধু owner-কেই raw update allow করে)। RPC নিজেই
        // has_release_request/release_request_* সেট করে, দরকার হলে pending additional_charge
        // insert করে (নিচের local ব্লকের সমতুল্য, নিজস্ব cloud id দিয়ে), আর owner-কে notify করে —
        // আলাদা createNotification() লাগবে না। guard: caller নিজে accepted solver কিনা। best-effort।
        if (SupabaseAuthManager.currentUserId() == problem.acceptedSolverId) {
            SupabaseSyncManager.requestJobRelease(problem.id, safeExtra, note).onFailure { e ->
                Log.w("SomadhanRepo", "requestJobRelease: Supabase dual-write failed for ${problem.id} (local flow unaffected): ${e.message}")
                // [OUTBOX WIRE - Step 12.4] paramsJson keys "problemId"/"extraAmount"/"note" --
                // OutboxRpcDispatcher.kt-এর "request_job_release" branch-এর সাথে অক্ষরে অক্ষরে
                // মিলছে। idempotency (migration recovered_job_release.sql বডি থেকে যাচাই): এই RPC
                // কোনো balance/escrow টেবিল ছোঁয় না -- শুধু problems-এর release-request ফিল্ডগুলো
                // SET করে (increment না) আর একটা নতুন PENDING additional_charges row insert করে।
                // তাই replay-তে সরাসরি টাকা দুইবার নড়ে না। ⚠️ অবশিষ্ট ঝুঁকি (BLOCKED না, শুধু নথিভুক্ত):
                // additional_charges insert deterministic id/on-conflict-guard ছাড়া (gen_random_uuid) --
                // replay দুইবার চললে একই amount-এর ডুপ্লিকেট PENDING charge row তৈরি হতে পারে; সেই
                // ডুপ্লিকেট চার্জ পরে আলাদাভাবে (respondToAdditionalCharge) accept হলেই কেবল বাস্তব
                // টাকা নড়বে। বিস্তারিত: CI_TEST_SUITE_PROGRESS.md "Step 12.4" সেকশন।
                enqueueOutboxRetry(
                    rpcName = "request_job_release",
                    params = kotlinx.serialization.json.JsonObject(
                        buildMap {
                            put("problemId", kotlinx.serialization.json.JsonPrimitive(problem.id))
                            put("extraAmount", kotlinx.serialization.json.JsonPrimitive(safeExtra))
                            put("note", kotlinx.serialization.json.JsonPrimitive(note))
                        }
                    ),
                    error = e
                )
            }
        }

        // Create the pending additional-charge record for this release-request extra amount.
        //
        // Bug fix: this block used to ALSO bump escrow.extraAmount directly
        // (escrow.copy(extraAmount = escrow.extraAmount + safeExtra)) right here. That was wrong
        // on two counts:
        //   1. No money has actually moved yet at this point -- there's no wallet deduction and
        //      no addToEscrow() call for this amount, it is purely a pending request the user
        //      hasn't confirmed. If a cancel/dispute/refund happened while it was still pending,
        //      the refund (which reads escrow.baseAmount + escrow.extraAmount directly, e.g. in
        //      refundEscrowOnce()) would refund this un-paid amount too.
        //   2. If this SAME charge was later accepted through the general "respond to additional
        //      charge" flow (respondToAdditionalCharge -> addToEscrow()), that call would ADD
        //      charge.amount to escrow.extraAmount a second time -- double-counting the same
        //      extra amount into escrow, which is what produced the double refund.
        // The authoritative escrow.extraAmount now only ever gets touched by addToEscrow() (real,
        // confirmed money) and by confirmReleaseAndComplete(), which SETS (not adds) the final
        // value once everything is actually confirmed -- matching the same SET pattern
        // rejectJobReleaseRequest() already uses when a request is rejected. The pending amount
        // itself lives on problem.releaseRequestExtraAmount (set above), which is all
        // resolveSettlementAmounts()/confirmReleaseAndComplete() need to read.
        if (safeExtra > 0.0) {
            val chargeId = "EXTRA_${problem.id.takeLast(6)}_${now.toString().takeLast(4)}"
            val charge = AdditionalChargeEntity(
                id = chargeId,
                problemId = problem.id,
                solverId = problem.acceptedSolverId ?: "SOLVER",
                userId = problem.userId,
                reason = note.ifBlank { "কাজের অতিরিক্ত বিল রিকোয়েস্ট" },
                amount = safeExtra,
                status = "PENDING",
                createdAt = now
            )
            additionalChargeDao.insert(charge)
        }

        // Notify problem owner (User)
        val solverName = problem.acceptedSolverName ?: "সমাধানকারী"
        val baseAmt = problem.acceptedAmount ?: 0.0
        val totalAmt = baseAmt + safeExtra
        val extraText = if (safeExtra > 0.0) " (মূল চুক্তি: ৳${DistanceUtil.toBengaliDigits(baseAmt.toInt().toString())} + অতিরিক্ত: ৳${DistanceUtil.toBengaliDigits(safeExtra.toInt().toString())})" else ""
        val notif = NotificationEntity(
            role = "USER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = problem.userId,
            title = "কাজ সম্পন্ন ও অর্থ রিলিজের অনুরোধ এসেছে! 🔔",
            message = "$solverName \"${problem.title}\" কাজটি সম্পন্ন করেছেন এবং ৳${DistanceUtil.toBengaliDigits(totalAmt.toInt().toString())} রিলিজের অনুরোধ পাঠিয়েছেন$extraText। অনুগ্রহ করে ৪৮ ঘণ্টার মধ্যে যাচাই করে পেমেন্ট রিলিজ করুন।",
            targetType = "problem",
            targetId = problem.id,
            relatedProblemId = problem.id
        )
        notificationDao.insertNotification(notif)

        // Post chat message notice
        sendMessage(
            problemId = problem.id,
            senderId = problem.acceptedSolverId ?: "SOLVER",
            receiverId = problem.userId,
            senderName = solverName,
            content = "🚀 আমি কাজ সম্পন্ন করেছি এবং পেমেন্ট রিলিজের অনুরোধ পাঠিয়েছি$extraText। নোট: ${note.ifBlank { "সকল কাজ বুঝিয়ে দেওয়া হয়েছে।" }}"
        )
    }

    suspend fun cancelJobReleaseRequest(problem: ProblemEntity) {
        val wasDisputed = problem.isDisputed
        val updatedProblem = if (wasDisputed) {
            problem.copy(
                hasReleaseRequest = false,
                releaseRequestExtraAmount = 0.0,
                releaseRequestNote = "",
                releaseRequestedAt = null,
                isDisputed = false,
                disputeReason = null,
                disputeInitiatorId = null,
                disputeInitiatorRole = null,
                disputedAt = null,
                isAdminInvolvedInChat = false,
                adminAssistanceRequestedBy = null,
                adminAssistanceRequestedAt = null,
                lastActivityAt = System.currentTimeMillis()
            )
        } else {
            problem.copy(
                hasReleaseRequest = false,
                releaseRequestExtraAmount = 0.0,
                releaseRequestNote = "",
                releaseRequestedAt = null,
                lastActivityAt = System.currentTimeMillis()
            )
        }
        problemDao.updateProblem(updatedProblem)

        // [SUPABASE-MIGRATED - step 12 batch 3] cancel_job_release_request RPC needed because
        // caller is the solver, not the owner. RPC itself resets fields based on wasDisputed and
        // notifies the owner. Guard: caller must be the accepted solver. Best-effort.
        if (SupabaseAuthManager.currentUserId() == problem.acceptedSolverId) {
            SupabaseSyncManager.cancelJobReleaseRequest(problem.id).onFailure { e ->
                Log.w("SomadhanRepo", "cancelJobReleaseRequest: Supabase dual-write failed for ${problem.id} (local flow unaffected): ${e.message}")
                // [OUTBOX WIRE - Step 12.4] paramsJson key "problemId" -- OutboxRpcDispatcher.kt-এর
                // "cancel_job_release_request" branch-এর সাথে অক্ষরে অক্ষরে মিলছে। idempotency
                // (migration recovered_job_release.sql বডি থেকে যাচাই): কোনো balance/escrow টেবিল
                // ছোঁয় না -- শুধু problems-এর release-request/dispute ফিল্ডগুলো SET করে (increment
                // না), তাই replay নিরাপদ। একমাত্র পার্শ্ব-প্রতিক্রিয়া একটা নতুন notification row
                // (non-deterministic id) -- ডুপ্লিকেট হলেও টাকা-সংক্রান্ত না।
                enqueueOutboxRetry(
                    rpcName = "cancel_job_release_request",
                    params = kotlinx.serialization.json.JsonObject(
                        buildMap {
                            put("problemId", kotlinx.serialization.json.JsonPrimitive(problem.id))
                        }
                    ),
                    error = e
                )
            }
        }

        val notifTitle = if (wasDisputed) "বিরোধ বন্ধ ও রিলিজ অনুরোধ প্রত্যাহার" else "রিলিজের অনুরোধ প্রত্যাহার করা হয়েছে"
        val notifMsg = if (wasDisputed) {
            "${problem.acceptedSolverName ?: "সমাধানকারী"} \"${problem.title}\" কাজের বিরোধ বন্ধ করে রিলিজ অনুরোধ প্রত্যাহার করেছেন।"
        } else {
            "${problem.acceptedSolverName ?: "সমাধানকারী"} \"${problem.title}\" কাজের রিলিজ অনুরোধ সাময়িকভাবে প্রত্যাহার করেছেন।"
        }

        val notif = NotificationEntity(
            role = "USER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = problem.userId,
            title = notifTitle,
            message = notifMsg,
            targetType = "problem",
            targetId = problem.id,
            relatedProblemId = problem.id
        )
        notificationDao.insertNotification(notif)

        if (wasDisputed) {
            val solverId = problem.acceptedSolverId
            if (!solverId.isNullOrBlank()) {
                triggerDynamicReputationEvent(
                    userId = solverId,
                    eventType = "DISPUTE_SETTLED_FRIENDLY",
                    problemId = problem.id,
                    defaultScore = 1.0,
                    defaultCap = 2.0,
                    isPositive = true,
                    customNote = "পারস্পরিক সমঝোতায় বিরোধ প্রত্যাহার"
                )
            }
        }

        val chatContent = if (wasDisputed) {
            "ℹ️ বিরোধ বন্ধ করে রিলিজের অনুরোধটি প্রত্যাহার করা হয়েছে।"
        } else {
            "ℹ️ রিলিজের অনুরোধটি সাময়িকভাবে প্রত্যাহার করা হয়েছে।"
        }

        sendMessage(
            problemId = problem.id,
            senderId = problem.acceptedSolverId ?: "SOLVER",
            receiverId = problem.userId,
            senderName = problem.acceptedSolverName ?: "সমাধানকারী",
            content = chatContent
        )
    }

    suspend fun rejectJobReleaseRequest(problem: ProblemEntity, reason: String = "") {
        val updatedProblem = problem.copy(
            hasReleaseRequest = false,
            releaseRequestExtraAmount = 0.0,
            releaseRequestNote = "",
            releaseRequestedAt = null,
            lastActivityAt = System.currentTimeMillis()
        )
        problemDao.updateProblem(updatedProblem)

        // Reset escrow extra amount if needed
        val escrow = escrowDao.getByProblemId(problem.id)
        if (escrow != null && escrow.extraAmount > 0.0) {
            val acceptedCharges = additionalChargeDao.getAllByProblemId(problem.id).firstOrNull()?.filter { it.status == "ACCEPTED" } ?: emptyList()
            val totalAcceptedExtra = acceptedCharges.sumOf { it.amount }
            val updatedEscrow = escrow.copy(extraAmount = totalAcceptedExtra)
            escrowDao.insert(updatedEscrow)
        }

        // [SUPABASE-MIGRATED - step 12 batch 3] reject_job_release_request RPC needed because
        // caller is the problem owner but the flow also touches escrows/additional_charges
        // together (multi-table), matching the pattern of other batch-2/3 RPCs. RPC itself
        // resets the release fields, recomputes escrow.extra_amount from ACCEPTED additional
        // charges, and notifies the solver. Guard: caller must be the problem owner. Best-effort.
        if (SupabaseAuthManager.currentUserId() == problem.userId) {
            SupabaseSyncManager.rejectJobReleaseRequest(problem.id, reason).onFailure { e ->
                Log.w("SomadhanRepo", "rejectJobReleaseRequest: Supabase dual-write failed for ${problem.id} (local flow unaffected): ${e.message}")
                // [OUTBOX WIRE - Step 12.4] paramsJson keys "problemId"/"reason" -- OutboxRpcDispatcher.kt-এর
                // "reject_job_release_request" branch-এর সাথে অক্ষরে অক্ষরে মিলছে। idempotency
                // (migration recovered_job_release.sql বডি থেকে যাচাই): balance/wallet ছোঁয় না;
                // escrow.extra_amount একমাত্র touched ভ্যালু, কিন্তু সেটাও ACCEPTED additional_charges-এর
                // যোগফল থেকে প্রতিবার নতুন করে SET (recompute) হয়, আগের ভ্যালুর ওপর increment না --
                // তাই replay দুইবার চললেও একই ফলাফল, টাকা নড়ে না।
                enqueueOutboxRetry(
                    rpcName = "reject_job_release_request",
                    params = kotlinx.serialization.json.JsonObject(
                        buildMap {
                            put("problemId", kotlinx.serialization.json.JsonPrimitive(problem.id))
                            put("reason", kotlinx.serialization.json.JsonPrimitive(reason))
                        }
                    ),
                    error = e
                )
            }
        }

        val reasonText = if (reason.isNotBlank()) " (কারণ: $reason)" else ""
        val solverId = problem.acceptedSolverId
        if (!solverId.isNullOrBlank()) {
            if (reason.contains("জাল") || reason.contains("ভুয়া") || reason.contains("fake", ignoreCase = true) || reason.contains("proof", ignoreCase = true)) {
                triggerDynamicReputationEvent(
                    userId = solverId,
                    eventType = "FAKE_PROOF_SUBMISSION",
                    problemId = problem.id,
                    defaultScore = 10.0,
                    defaultCap = 20.0,
                    defaultPenalty = 10.0,
                    isPositive = false,
                    customNote = "জাল বা ভুয়া প্রুফ সাবমিটের দায়ে পেনাল্টি"
                )
            }
            val notif = NotificationEntity(
                role = "SOLVER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = solverId,
                title = "রিলিজের অনুরোধ প্রত্যাখ্যাত ❌",
                message = "${problem.userName} আপনার \"${problem.title}\" কাজের রিলিজ অনুরোধ প্রত্যাখ্যান করেছেন$reasonText। কাজটি সম্পন্ন বা সংশোধন করে পুনরায় অনুরোধ পাঠাতে পারবেন।",
                targetType = "problem",
                targetId = problem.id,
                relatedProblemId = problem.id
            )
            notificationDao.insertNotification(notif)

            sendMessage(
                problemId = problem.id,
                senderId = problem.userId,
                receiverId = solverId,
                senderName = problem.userName,
                content = "❌ রিলিজ অনুরোধ প্রত্যাখ্যান করা হয়েছে$reasonText। দয়া করে কাজটি সংশোধন করে পুনরায় জমা দিন।"
            )
        }
    }

    suspend fun raiseDispute(problem: ProblemEntity, userId: String, reason: String) {
        val now = System.currentTimeMillis()
        val isSolver = userId == problem.acceptedSolverId
        val role = if (isSolver) "SOLVER" else "USER"
        val initiatorName = if (isSolver) (problem.acceptedSolverName ?: "সমাধানকারী") else problem.userName
        val progressStep = problem.calculateProgressStep()
        val updatedProblem = problem.copy(
            isDisputed = true,
            disputeReason = reason.trim(),
            disputeInitiatorId = userId,
            disputeInitiatorRole = role,
            disputedAt = now,
            disputeSettledAt = null,
            disputeProgressAtRaise = progressStep,
            lastActivityAt = now
        )
        problemDao.updateProblem(updatedProblem)
        // [SUPABASE-MIGRATED - ধাপ ৮] `raise_dispute` RPC সোর্স পড়ে যাচাই করা হয়েছে -- একই লজিক
        // (owner/accepted-solver ছাড়া আর কেউ পারবে না, notification insert সহ)। জানা সীমাবদ্ধতা:
        // RPC-তে dispute_progress_at_raise সেট হয় না (client-side calculateProgressStep()
        // নির্ভর business logic, RPC-তে পোর্ট করা হয়নি) -- শুধু এই একটা history/display ফিল্ড
        // cloud-এ ফাঁকা থাকবে, টাকা-সংক্রান্ত না, non-blocking।
        if (SupabaseAuthManager.currentUserId() == userId) {
            SupabaseSyncManager.raiseDispute(problem.id, reason.trim())
        }

        val targetUserId = if (isSolver) problem.userId else problem.acceptedSolverId
        if (!targetUserId.isNullOrBlank()) {
            val notifTitle = if (isSolver) "⚠️ সমাধানকারী বিরোধ (Dispute) উত্থাপন করেছেন" else "⚠️ কাজের রিলিজ নিয়ে বিরোধ/ডিসপিউট তোলা হয়েছে"
            val notifMsg = if (isSolver) {
                "${initiatorName} \"${problem.title}\" কাজের বিষয়ে বিরোধ (Dispute) তুলেছেন। কারণ: ${reason.trim()}। দয়া করে চ্যাটে আলোচনা করুন।"
            } else {
                "${initiatorName} \"${problem.title}\" কাজের রিলিজ নিয়ে বিরোধ (Dispute) তুলেছেন। কারণ: ${reason.trim()}। দয়া করে চ্যাটে আলোচনা করুন।"
            }
            val notif = NotificationEntity(
                role = if (isSolver) "USER" else "SOLVER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = targetUserId,
                title = notifTitle,
                message = notifMsg,
                targetType = "problem",
                targetId = problem.id,
                relatedProblemId = problem.id
            )
            notificationDao.insertNotification(notif)
        }

        // Send a dedicated dispute notice / system event in problem chat
        val receiverId = if (isSolver) problem.userId else (problem.acceptedSolverId ?: "")
        sendSystemEventMessage(
            problemId = problem.id,
            receiverId = receiverId,
            eventType = "DISPUTE_OPENED",
            content = "🚨 বিরোধ (Dispute) শুরু হয়েছে: ${reason.trim()}\nঅনুগ্রহ করে চ্যাটে আলোচনার মাধ্যমে বিষয়টি সমাধান করুন। প্রয়োজনে অ্যাডমিন সহায়তা যুক্ত করতে পারেন।"
        )

        val actorLabel = if (isSolver) "সমাধানকারী" else "গ্রাহক"
        logAdminAction(
            actionType = "DISPUTE_RAISED",
            targetId = problem.id,
            targetName = problem.title,
            details = "${actorLabel} (${initiatorName}) বিরোধ তুলেছেন। কারণ: ${reason.trim()}"
        )
    }

    suspend fun adminManuallyFlagDispute(problemId: String, reason: String) {
        val problem = getProblemById(problemId) ?: return
        if (problem.isDisputed) return
        val now = System.currentTimeMillis()
        val progressStep = problem.calculateProgressStep()
        val updated = problem.copy(
            isDisputed = true,
            disputeReason = reason.trim(),
            disputeInitiatorId = "ADMIN",
            disputeInitiatorRole = "ADMIN",
            disputedAt = now,
            disputeProgressAtRaise = progressStep,
            lastActivityAt = now
        )
        problemDao.updateProblem(updated)

        // [SUPABASE-MIGRATED - step 12 batch 3] admin_manually_flag_dispute RPC checks
        // is_admin(auth.uid()) itself server-side, guard/pattern same as adminSetBanned(): just
        // require a session, RPC will fail (logged only) if not actually an admin. Known
        // limitation: RPC stores dispute_initiator_id as null (uuid column can't hold the string
        // "ADMIN"), only dispute_initiator_role='ADMIN' is set on the cloud side.
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminManuallyFlagDispute(problemId, reason).onFailure { e ->
                Log.w("SomadhanRepo", "adminManuallyFlagDispute: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
                // [Step 12.8a] outbox retry -- idempotency (migration recovered_admin_moderation.sql
                // থেকে যাচাই): admin_manually_flag_dispute problem row `for update` লক করে,
                // is_disputed হলে ALREADY_DISPUTED ফেরত দেয় (কোনো ফিল্ড আবার overwrite হয় না) -- তাই
                // একই RPC replay দুইবার চললেও ক্ষতি নেই। ⚠️ RPC-তে problem-status (COMPLETED/CANCELLED)
                // এর কোনো চেক নেই -- product-সিদ্ধান্ত (Step 12.8a, ব্যবহারকারী, ২০২৬-০৯-২১): admin
                // COMPLETED/CANCELLED কাজেও dispute flag করতে পারবেন, তাই এখানে retry যোগ করা নিরাপদ
                // ধরা হয়েছে। paramsJson keys "problemId","reason" -- OutboxRpcDispatcher.kt-এর
                // "admin_manually_flag_dispute" branch-এর সাথে অক্ষরে অক্ষরে মিলছে।
                enqueueOutboxRetry(
                    rpcName = "admin_manually_flag_dispute",
                    params = kotlinx.serialization.json.JsonObject(
                        mapOf(
                            "problemId" to kotlinx.serialization.json.JsonPrimitive(problemId),
                            "reason" to kotlinx.serialization.json.JsonPrimitive(reason)
                        )
                    ),
                    error = e
                )
            }
        }

        // কাস্টমার ও সমাধানকারী উভয়কেই নোটিফিকেশন পাঠাও
        val notifUser = NotificationEntity(
            role = "USER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = problem.userId,
            title = "⚠️ অ্যাডমিন দ্বারা বিরোধ (Dispute) ফ্ল্যাগ করা হয়েছে",
            message = "অ্যাডমিন \"${problem.title}\" কাজের বিষয়টি বিরোধ হিসেবে চিহ্নিত করেছেন। কারণ: ${reason.trim()}",
            targetType = "problem",
            targetId = problem.id,
            relatedProblemId = problem.id
        )
        notificationDao.insertNotification(notifUser)

        if (!problem.acceptedSolverId.isNullOrBlank()) {
            val notifSolver = NotificationEntity(
                role = "SOLVER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = problem.acceptedSolverId,
                title = "⚠️ অ্যাডমিন দ্বারা বিরোধ (Dispute) ফ্ল্যাগ করা হয়েছে",
                message = "অ্যাডমিন \"${problem.title}\" কাজের বিষয়টি বিরোধ হিসেবে চিহ্নিত করেছেন। কারণ: ${reason.trim()}",
                targetType = "problem",
                targetId = problem.id,
                relatedProblemId = problem.id
            )
            notificationDao.insertNotification(notifSolver)
        }

        // চ্যাটে সিস্টেম ইভেন্ট মেসেজ
        val receiverId = if (!problem.acceptedSolverId.isNullOrBlank()) problem.acceptedSolverId else problem.userId
        sendSystemEventMessage(
            problemId = problem.id,
            receiverId = receiverId,
            eventType = "ADMIN_FLAGGED_DISPUTE",
            content = "🚨 অ্যাডমিন এই কাজটি বিরোধ (Dispute) হিসেবে চিহ্নিত করেছেন: ${reason.trim()}"
        )

        logAdminAction(
            actionType = "ADMIN_MANUAL_DISPUTE_FLAG",
            targetId = problem.id,
            targetName = problem.title,
            details = "অ্যাডমিন ম্যানুয়ালি বিরোধ ফ্ল্যাগ করেছেন। কারণ: ${reason.trim()}"
        )
    }

    // [Offline Action Gating ধাপ ৯] এই ফাংশন কোনো UI বাটন সরাসরি কল করে না — শুধু বড় ফ্লো-র
    // (`adminManuallyFlagDispute`, `withdrawDispute` ইত্যাদি, যেগুলো ধাপ ১০-এর Dispute ডোমেইনের
    // অংশ) ভেতর থেকে একটা সাব-স্টেপ হিসেবে কল হয় — ধাপ ৮-এর cron-triggered RPC-গুলোর মতোই এখানে
    // guard বসানো হয়নি, বরং সেই বাইরের entry-point ফাংশনগুলোতে (ধাপ ১০-এ) guard বসলে এটা
    // স্বয়ংক্রিয়ভাবে কভার্ড হয়ে যাবে।
    suspend fun sendSystemEventMessage(
        problemId: String,
        receiverId: String,
        eventType: String,
        content: String,
        senderId: String = "SYSTEM",
        senderName: String = "সিস্টেম"
    ) {
        val msg = MessageEntity(
            id = "MSG_${UUID.randomUUID().toString().take(8)}",
            problemId = problemId,
            senderId = senderId,
            receiverId = receiverId,
            senderName = senderName,
            content = content,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            isAdminMessage = false,
            isSystemEvent = true,
            systemEventType = eventType
        )
        messageDao.insertMessage(msg)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৯৫] `system_event_message` RPC সোর্স পড়ে যাচাই করা হয়েছে —
        // caller admin/owner/accepted_solver যেকোনো একজন হলেই কল করতে পারে, RPC নিজেই sender_id
        // null রেখে is_system_event=true দিয়ে insert করে + problem.last_activity_at আপডেট করে।
        // best-effort — ব্যর্থ হলেও উপরের local Room flow অপ্রভাবিত থাকে।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.systemEventMessage(
                problemId = problemId,
                receiverId = receiverId.ifBlank { null },
                eventType = eventType,
                content = content,
                senderName = senderName
            ).onFailure { e ->
                Log.w("SomadhanRepo", "sendSystemEventMessage: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }

        val prob = problemDao.getProblemById(problemId)
        if (prob != null) {
            val updatedProb = prob.copy(lastActivityAt = msg.timestamp)
            problemDao.updateProblem(updatedProb)
        }
    }

    suspend fun withdrawDispute(problem: ProblemEntity, requesterId: String) {
        if (!problem.isDisputed) return
        // মূল ফিক্স: শুধু initiator-ই প্রত্যাহার করতে পারবে
        if (problem.disputeInitiatorId.isNullOrBlank() || problem.disputeInitiatorId != requesterId) return

        val isSolverInitiator = requesterId == problem.acceptedSolverId
        val initiatorLabel = if (isSolverInitiator) "সমাধানকারী" else "গ্রাহক"
        val otherPartyId = if (isSolverInitiator) problem.userId else (problem.acceptedSolverId ?: "")

        val now = System.currentTimeMillis()
        val updatedProblem = problem.copy(
            isDisputed = false,
            disputeReason = null,
            disputeInitiatorId = null,
            disputeInitiatorRole = null,
            disputedAt = null,
            isAdminInvolvedInChat = problem.isAdminInvolvedInChat, // অ্যাডমিন যুক্ত থাকলে সেটা রাখো, রিমুভ কোরো না
            lastActivityAt = now
        )
        problemDao.updateProblem(updatedProblem)

        // অপর পক্ষকে নোটিফিকেশন পাঠাও
        if (otherPartyId.isNotBlank()) {
            val notif = NotificationEntity(
                role = if (isSolverInitiator) "USER" else "SOLVER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = otherPartyId,
                title = "বিরোধ প্রত্যাহার হয়েছে ✅",
                message = "${initiatorLabel} \"${problem.title}\" কাজের বিরোধ প্রত্যাহার করে নিয়েছেন এবং কাজটি পুনরায় স্বাভাবিকভাবে চলছে।",
                targetType = "problem",
                targetId = problem.id,
                relatedProblemId = problem.id
            )
            notificationDao.insertNotification(notif)
        }

        // চ্যাটে একটা সিস্টেম মেসেজ পাঠাও
        sendSystemEventMessage(
            problemId = problem.id,
            receiverId = otherPartyId,
            eventType = "DISPUTE_WITHDRAWN",
            content = "🤝 ${initiatorLabel} বিরোধ প্রত্যাহার করেছেন। কাজটি পুনরায় স্বাভাবিক অবস্থায় ফিরে এসেছে।"
        )

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৩] `withdraw_dispute` RPC সোর্স পড়ে যাচাই করা হয়েছে —
        // শুধু dispute_initiator_id (auth.uid()) নিজে কল করতে পারে, RPC নিজেই problem reset করে
        // আর অপর পক্ষকে notification insert করে (দুটোই একই transaction-এ)। ব্যর্থ হলেও উপরের
        // local Room flow ইতিমধ্যে সম্পন্ন, best-effort dual-write।
        if (SupabaseAuthManager.currentUserId() == requesterId) {
            SupabaseSyncManager.withdrawDispute(problem.id)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK") {
                        Log.w("SomadhanRepo", "withdrawDispute: Supabase dual-write non-OK result for problem ${problem.id}: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "withdrawDispute: Supabase dual-write failed for problem ${problem.id} (local flow unaffected): ${e.message}")
                    // [OUTBOX WIRE - Step 12.4] paramsJson key "problemId" -- OutboxRpcDispatcher.kt-এর
                    // "withdraw_dispute" branch-এর সাথে অক্ষরে অক্ষরে মিলছে। idempotency
                    // (migration recovered_disputes.sql বডি থেকে যাচাই): কোনো balance/escrow টেবিল
                    // ছোঁয় না; state-গার্ড আছে -- is_disputed ইতিমধ্যে false হলে exception না ছুঁড়ে
                    // 'NOT_DISPUTED' রিটার্ন করে (no-op)। IDEMPOTENT।
                    enqueueOutboxRetry(
                        rpcName = "withdraw_dispute",
                        params = kotlinx.serialization.json.JsonObject(
                            buildMap {
                                put("problemId", kotlinx.serialization.json.JsonPrimitive(problem.id))
                            }
                        ),
                        error = e
                    )
                }
        }

        logAdminAction(
            actionType = if (isSolverInitiator) "SOLVER_WITHDRAW_DISPUTE" else "USER_WITHDRAW_DISPUTE",
            targetId = problem.id,
            targetName = problem.title,
            details = "${initiatorLabel} (${requesterId}) নিজে বিরোধ প্রত্যাহার করেছেন।"
        )
    }

    suspend fun settleDispute(problem: ProblemEntity, userId: String) {
        if (!problem.isDisputed && problem.disputeSettledAt != null) return
        val now = System.currentTimeMillis()
        val isSolver = userId == problem.acceptedSolverId
        val actorName = if (isSolver) (problem.acceptedSolverName ?: "সমাধানকারী") else problem.userName
        val otherPartyId = if (isSolver) problem.userId else (problem.acceptedSolverId ?: "")

        val updatedProblem = problem.copy(
            isDisputed = false,
            disputeSettledAt = now,
            disputeReason = null,
            disputeInitiatorId = null,
            disputeInitiatorRole = null,
            lastActivityAt = now
        )
        problemDao.updateProblem(updatedProblem)

        // Send a dedicated dispute settled notice in problem chat
        sendSystemEventMessage(
            problemId = problem.id,
            receiverId = otherPartyId,
            eventType = "DISPUTE_SETTLED",
            content = "🤝 উভয় পক্ষ সমঝোতায় পৌঁছেছেন ($actorName দ্বারা নিশ্চিতকৃত)। কাজের প্রক্রিয়া পুনরায় স্বাভাবিকভাবে চলবে।"
        )

        // Notification to other party
        if (otherPartyId.isNotBlank()) {
            val notif = NotificationEntity(
                role = if (isSolver) "USER" else "SOLVER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = otherPartyId,
                title = "সমঝোতা সম্পন্ন হয়েছে 🤝",
                message = "${actorName} \"${problem.title}\" কাজের বিরোধে সমঝোতা নিশ্চিত করেছেন। কাজের পরবর্তী ধাপগুলো পুনরায় সক্রিয় হয়েছে।",
                targetType = "problem",
                targetId = problem.id,
                relatedProblemId = problem.id
            )
            notificationDao.insertNotification(notif)
        }

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৩] `settle_dispute` RPC সোর্স পড়ে যাচাই করা হয়েছে —
        // dispute-এর যেকোনো পক্ষ (owner অথবা solver) কল করতে পারে, RPC নিজেই problem reset করে
        // আর অপর পক্ষকে notification insert করে (একই transaction-এ)। best-effort dual-write,
        // ব্যর্থ হলেও উপরের local Room flow অপ্রভাবিত।
        if (SupabaseAuthManager.currentUserId() == userId) {
            SupabaseSyncManager.settleDispute(problem.id)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK") {
                        Log.w("SomadhanRepo", "settleDispute: Supabase dual-write non-OK result for problem ${problem.id}: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "settleDispute: Supabase dual-write failed for problem ${problem.id} (local flow unaffected): ${e.message}")
                    // [OUTBOX WIRE - Step 12.4] paramsJson key "problemId" -- OutboxRpcDispatcher.kt-এর
                    // "settle_dispute" branch-এর সাথে অক্ষরে অক্ষরে মিলছে। idempotency
                    // (migration recovered_disputes.sql বডি থেকে যাচাই): কোনো balance/escrow টেবিল
                    // ছোঁয় না; state-গার্ড আছে -- is_disputed false এবং dispute_settled_at ইতিমধ্যে
                    // সেট থাকলে exception না ছুঁড়ে 'ALREADY_SETTLED' রিটার্ন করে (no-op)। IDEMPOTENT।
                    enqueueOutboxRetry(
                        rpcName = "settle_dispute",
                        params = kotlinx.serialization.json.JsonObject(
                            buildMap {
                                put("problemId", kotlinx.serialization.json.JsonPrimitive(problem.id))
                            }
                        ),
                        error = e
                    )
                }
        }

        logAdminAction(
            actionType = "DISPUTE_SETTLED",
            targetId = problem.id,
            targetName = problem.title,
            details = "উভয় পক্ষ সমঝোতায় পৌঁছেছেন ($actorName দ্বারা নিশ্চিতকৃত)"
        )
    }

    suspend fun requestAdminAssistance(problem: ProblemEntity, requesterId: String, requesterRole: String) {
        val now = System.currentTimeMillis()
        val updatedProblem = problem.copy(
            isAdminInvolvedInChat = true,
            adminAssistanceRequestedBy = requesterRole,
            adminAssistanceRequestedAt = now,
            lastActivityAt = now
        )
        problemDao.updateProblem(updatedProblem)

        val roleBangla = if (requesterRole.equals("USER", ignoreCase = true)) "গ্রাহক" else "সমাধানকারী"
        val receiverId = if (requesterRole.equals("USER", ignoreCase = true)) problem.acceptedSolverId ?: "" else problem.userId

        val adminNoticeMsg = MessageEntity(
            id = "MSG_${UUID.randomUUID().toString().take(8)}",
            problemId = problem.id,
            senderId = requesterId,
            receiverId = receiverId,
            senderName = "অ্যাডমিন সাপোর্ট ডেস্ক 🛡️",
            content = "🛡️ $roleBangla অ্যাডমিন সহায়তা চেয়েছেন। অ্যাডমিন টিম দ্রুত এই বিরোধ/চ্যাট পর্যবেক্ষণ করে সহায়তা প্রদান করবেন।",
            timestamp = now,
            isRead = false,
            isAdminMessage = true
        )
        messageDao.insertMessage(adminNoticeMsg)

        // Notify Admins
        val adminNotif = NotificationEntity(
            role = "",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = "admin_broadcast",
            title = "🛡️ নতুন অ্যাডমিন সহায়তা অনুরোধ",
            message = "\"${problem.title}\" কাজে $roleBangla অ্যাডমিন সহায়তার অনুরোধ জানিয়েছেন।",
            targetType = "problem",
            targetId = problem.id,
            relatedProblemId = problem.id
        )
        notificationDao.insertNotification(adminNotif)

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৩] `request_admin_assistance` RPC সোর্স পড়ে যাচাই
        // করা হয়েছে — শুধু problem owner/accepted-solver কল করতে পারে, RPC নিজেই problem flag
        // সেট করে আর party-to-party notice message insert করে। admin-broadcast নোটিফিকেশন এই
        // RPC-এর স্কোপে নেই (local-এর মতো uuid না হওয়া sentinel `"admin_broadcast"` ব্যবহার করা
        // যায় না) — তাই আলাদাভাবে `notify_admins` RPC কল করা হয়, যেটার কোনো admin-check নেই
        // caller-এর উপর (শুধু auth থাকলেই চলে, কারণ normal user/solver session থেকেই এই ফ্লো
        // ট্রিগার হয়)। দুটোই best-effort dual-write, ব্যর্থ হলেও উপরের local Room flow
        // অপ্রভাবিত।
        if (SupabaseAuthManager.currentUserId() == requesterId) {
            SupabaseSyncManager.requestAdminAssistance(problem.id, requesterRole)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK") {
                        Log.w("SomadhanRepo", "requestAdminAssistance: Supabase dual-write non-OK result for problem ${problem.id}: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "requestAdminAssistance: Supabase dual-write (RPC) failed for problem ${problem.id} (local flow unaffected): ${e.message}")
                }

            SupabaseSyncManager.notifyAdmins(
                title = "🛡️ নতুন অ্যাডমিন সহায়তা অনুরোধ",
                message = "\"${problem.title}\" কাজে $roleBangla অ্যাডমিন সহায়তার অনুরোধ জানিয়েছেন।",
                relatedProblemId = problem.id
            ).onFailure { e ->
                Log.w("SomadhanRepo", "requestAdminAssistance: Supabase dual-write (notifyAdmins) failed for problem ${problem.id} (local flow unaffected): ${e.message}")
            }
        }

        logAdminAction(
            actionType = "ADMIN_ASSISTANCE_REQUESTED",
            targetId = problem.id,
            targetName = problem.title,
            details = "$roleBangla চ্যাটে অ্যাডমিন সহায়তা চেয়েছেন।"
        )
    }

    suspend fun adminSendMessageToProblemChat(problemId: String, adminUser: UserEntity, content: String) {
        val problem = problemDao.getProblemById(problemId) ?: return
        val now = System.currentTimeMillis()

        // Ensure problem marks admin involved
        val updatedProb = problem.copy(isAdminInvolvedInChat = true, lastActivityAt = now)
        problemDao.updateProblem(updatedProb)

        val msg = MessageEntity(
            id = "MSG_${UUID.randomUUID().toString().take(8)}",
            problemId = problemId,
            senderId = adminUser.id,
            receiverId = problem.userId,
            senderName = "Support Manager 🛡️",
            content = content.trim(),
            timestamp = now,
            isRead = false,
            isAdminMessage = true
        )
        messageDao.insertMessage(msg)

        // [SUPABASE-MIGRATED - ধাপ ১২ প্রি-ফিক্স] `messages_insert` RLS পলিসি admin-কে অন্যের
        // chat-এ সরাসরি insert করতে দেয় না, তাই একটা নতুন SECURITY DEFINER RPC
        // (`admin_send_message_to_problem_chat`) বানানো হয়েছে — guard হিসেবে শুধু session আছে
        // কিনা দেখা হচ্ছে (RPC নিজেই is_admin() চেক করে, admin_adjust_balance-এর মতো একই প্যাটার্ন)।
        // ব্যর্থ হলেও local Room flow অপ্রভাবিত থাকে।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminSendMessageToProblemChat(problemId, content.trim())
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK") {
                        Log.w("SomadhanRepo", "adminSendMessageToProblemChat: Supabase dual-write non-OK result for $problemId: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "adminSendMessageToProblemChat: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
                }
        }

        // Notify both User and Solver
        val notifUser = NotificationEntity(
            role = "USER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = problem.userId,
            title = "🛡️ সাপোর্ট ম্যানেজার বার্তা: ${problem.title}",
            message = "Support Manager: $content",
            targetType = "problem",
            targetId = problemId,
            relatedProblemId = problemId
        )
        notificationDao.insertNotification(notifUser)

        // [SUPABASE-MIGRATED - ধাপ ১২ ফিক্স] notification-gap বন্ধ: `admin_send_message_to_problem_chat`
        // RPC শুধু chat message insert করে, কোনো notification না (ধাপ ১২ audit এ নিশ্চিত হওয়া
        // গ্যাপ) — তাই `create_notification` RPC দিয়ে আলাদাভাবে পাঠানো হচ্ছে। caller admin হওয়ায়
        // RPC-এর party-check এড়িয়ে যেকোনো target-এ পাঠাতে পারার কথা।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.createNotification(
                targetUserId = problem.userId,
                title = notifUser.title,
                message = notifUser.message,
                targetType = "problem",
                targetId = problemId,
                relatedProblemId = problemId,
                role = notifUser.role
            ).onFailure { e ->
                Log.w("SomadhanRepo", "adminSendMessageToProblemChat: createNotification(owner) dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }

        val solverId = problem.acceptedSolverId
        if (!solverId.isNullOrBlank()) {
            val notifSolver = NotificationEntity(
                role = "SOLVER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = solverId,
                title = "🛡️ সাপোর্ট ম্যানেজার বার্তা: ${problem.title}",
                message = "Support Manager: $content",
                targetType = "problem",
                targetId = problemId,
                relatedProblemId = problemId
            )
            notificationDao.insertNotification(notifSolver)

            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.createNotification(
                    targetUserId = solverId,
                    title = notifSolver.title,
                    message = notifSolver.message,
                    targetType = "problem",
                    targetId = problemId,
                    relatedProblemId = problemId,
                    role = notifSolver.role
                ).onFailure { e ->
                    Log.w("SomadhanRepo", "adminSendMessageToProblemChat: createNotification(solver) dual-write failed for $problemId (local flow unaffected): ${e.message}")
                }
            }
        }

        logAdminAction(
            actionType = "ADMIN_CHAT_MESSAGE",
            targetId = problemId,
            targetName = problem.title,
            details = "অ্যাডমিন (${adminUser.name}) মেসেজ দিয়েছেন: $content"
        )
    }

    suspend fun adminResolveDispute(
        problemId: String,
        resolution: String,
        decisionNote: String,
        splitSolverPercent: Double = 50.0
    ) {
        // Serialize concurrent resolve attempts for the SAME problem (an admin double-tapping
        // "resolve", or two admin sessions racing on the same dispute) so two calls can never both
        // pass the "already resolved?" pre-check below before either has actually committed --
        // same per-key-mutex pattern as refundEscrowOnce()/refundMutexFor(). Without this, that
        // check was a plain read-then-write race: for SPLIT_SETTLEMENT specifically, where the
        // solver's userDao.addBalance() has no idempotency guard of its own (unlike the user's
        // refund half, which goes through refundEscrowOnce()'s own guard), a genuine
        // near-simultaneous double-tap could let both calls read the pre-resolution problem, both
        // pass the check, and credit the solver's split share twice.
        disputeResolveMutexFor(problemId).withLock {
            adminResolveDisputeLocked(problemId, resolution, decisionNote, splitSolverPercent)
        }
    }

    private suspend fun adminResolveDisputeLocked(
        problemId: String,
        resolution: String,
        decisionNote: String,
        splitSolverPercent: Double = 50.0
    ) {
        val problem = problemDao.getProblemById(problemId) ?: return
        val now = System.currentTimeMillis()

        // Idempotency guard: every branch below pays money out or issues a refund. The
        // RELEASE_TO_SOLVER branch is protected by confirmReleaseAndComplete()'s own guard, and
        // REFUND_TO_USER is protected by refundEscrowOnce()'s escrow-status guard — but
        // SPLIT_SETTLEMENT/CUSTOM_SPLIT/SETTLE credits the solver's split share directly via
        // userDao.addBalance() with no guard of its own (only the user's half of that split is
        // covered, via refundEscrowOnce()). Every branch sets disputeResolvedAt when it finishes,
        // so checking it fresh here catches a repeat resolution regardless of which branch ran
        // the first time. The mutex above is what makes this check race-proof; on its own it was
        // still vulnerable to a true near-simultaneous double-tap.
        if (problem.disputeResolvedAt != null) {
            Log.w("SomadhanRepo", "adminResolveDispute: skipped — problem $problemId dispute already resolved at ${problem.disputeResolvedAt}, refusing to resolve/pay out again")
            // BUGFIX: this used to just `return` here, which meant the ViewModel's caller
            // (which treats a clean return as success) would show "ডিসপিউট সিদ্ধান্ত সফলভাবে
            // কার্যকর করা হয়েছে!" / "ডিসপিউট সফলভাবে মীমাংসা ও ফান্ড বিতরণ করা হয়েছে!" even
            // though this call did nothing at all -- no balance moved, no re-release, no
            // re-refund. Balance-wise that silent no-op was always safe (this guard is exactly
            // what prevents a double payout when the same dispute is resolved twice), but it
            // falsely told the admin the action succeeded. Throwing here instead routes into
            // the existing onError path so the admin sees an honest "already resolved" message
            // instead of a fake success toast.
            throw IllegalStateException("এই বিরোধটি ইতিমধ্যে মীমাংসা করা হয়েছে (${resolution})। একই বিরোধ পুনরায় সমাধান/পেমেন্ট করা সম্ভব নয় — কোনো নতুন টাকা স্থানান্তরিত হয়নি।")
        }

        val progressStep = problem.disputeProgressAtRaise ?: problem.calculateProgressStep()

        when (resolution) {
            "RELEASE_TO_SOLVER" -> {
                val escrow = escrowDao.getByProblemId(problemId)
                val (_, resolvedExtra) = resolveSettlementAmounts(problem, escrow)
                confirmReleaseAndComplete(
                    problem,
                    includeExtraAmount = (resolvedExtra > 0.0 || problem.releaseRequestExtraAmount > 0.0 || problem.confirmedExtraAmountTotal > 0.0),
                    releaseType = "DISPUTE_RELEASE"
                )
                val resolvedProb = problemDao.getProblemById(problemId) ?: problem
                val updatedProb = resolvedProb.copy(
                    status = "COMPLETED",
                    jobStatus = "JOB_COMPLETED",
                    // isDisputed intentionally stays true here as a historical "this job had a
                    // dispute" marker (several screens use it that way) -- but disputeSettledAt
                    // is set alongside disputeResolvedAt so every consumer that gates on
                    // "isDisputeActive = isDisputed && disputeSettledAt == null" (JobTrackingScreen)
                    // stops treating this RESOLVED dispute as still active/pending.
                    isDisputed = true,
                    disputeResolutionDecision = "RELEASE_TO_SOLVER",
                    disputeResolutionType = "RELEASE_TO_SOLVER",
                    disputeResolutionNote = decisionNote,
                    disputeResolvedAt = now,
                    disputeSettledAt = now,
                    disputeResultSeenByUser = false,
                    disputeResultSeenBySolver = false,
                    disputeProgressAtSettlement = progressStep,
                    lastActivityAt = now
                )
                problemDao.updateProblem(updatedProb)

                val acceptedBidForRelease = updatedProb.acceptedBidId?.let { bidDao.getBidById(it) }
                    ?: bidDao.getBidsForProblemSync(problemId).find { it.solverId == updatedProb.acceptedSolverId }
                if (acceptedBidForRelease != null) {
                    val stampedBid = acceptedBidForRelease.copy(resolutionType = "ADMIN_RELEASE_TO_SOLVER", resolvedAt = now)
                    bidDao.updateBid(stampedBid)
                }

                sendSystemEventMessage(
                    problemId = problemId,
                    receiverId = problem.userId,
                    eventType = "ADMIN_RESOLVED_RELEASE",
                    content = "⚖️ অ্যাডমিন সিদ্ধান্ত: বিরোধ সমাধান করে কাজ সম্পন্ন ঘোষণা করা হয়েছে এবং সমাধানকারীকে অর্থ রিলিজ করা হয়েছে। নোট: $decisionNote"
                )
            }
            "SPLIT_SETTLEMENT", "SPLIT_50_50", "CUSTOM_SPLIT", "SETTLE" -> {
                val escrow = escrowDao.getByProblemId(problemId)
                val (baseAmt, extraAmt) = resolveSettlementAmounts(problem, escrow)
                val totalAmount = baseAmt + extraAmt

                val clampedPercent = splitSolverPercent.coerceIn(0.0, 100.0)
                val ratio = clampedPercent / 100.0
                val solverGrossBase = Math.round(baseAmt * ratio).toDouble()
                val solverGrossExtra = Math.round(extraAmt * ratio).toDouble()
                val solverGrossTotal = Math.round(totalAmount * ratio).toDouble()
                val userRefund = Math.round(totalAmount - solverGrossTotal).toDouble()

                val solverId = problem.acceptedSolverId
                
                // Centralized Commission Calculation for Solver's split share (respects promo/free quota and extra amount discount)
                val solverBreakdown = if (!solverId.isNullOrBlank() && solverGrossTotal > 0.0) {
                    calculateCommissionBreakdown(
                        problem = problem,
                        solverId = solverId,
                        baseAmount = solverGrossBase,
                        extraAmount = solverGrossExtra
                    )
                } else null

                val commissionAmount = solverBreakdown?.totalCommission ?: 0.0
                val solverNetAmount = solverBreakdown?.netAmount ?: solverGrossTotal
                val solverCommSetting = if (solverGrossTotal > 0.0) (commissionAmount / solverGrossTotal) * 100.0 else 0.0

                // Idempotency guard for the solver's split payout specifically, kept alongside
                // the disputeResolveMutexFor() lock above as defense-in-depth: the mutex is
                // in-memory and process-local, so it only serializes calls from THIS device/app
                // instance. It does not protect against two different admin devices/sessions
                // both resolving the same dispute at genuinely the same moment. This deterministic
                // transaction id makes that repeat detectable (and skippable) via local Room state
                // too, regardless of which process the earlier call ran in.
                val splitTrxId = "TRX_SPLIT_${problem.id}"
                val solverAlreadyPaid = transactionDao.getTransactionsForProblem(problem.id)
                    .any { it.id == splitTrxId }
                if (!solverAlreadyPaid && !solverId.isNullOrBlank() && solverNetAmount > 0.0) {
                    // INSTANT local credit only here -- the cloud side is pushed via the
                    // splitTrxId TransactionEntity's pendingCloudSync flag below (same
                    // safe/retrying pattern as refundEscrowOnce(), not the old one-shot
                    // incrementUserBalance() fire-and-forget, which had no retry if it failed).
                    // This was the exact gap behind "solver পায় কিন্তু user history-তে balance
                    // add হয় না" -- the solver's share used to be the ONE part of a dispute
                    // split with no retry safety net at all.
                    // [ব্যালেন্স ফিক্স] solver earning তাই balanceSolver mirror-ও একসাথে আপডেট হয়।
                    userDao.addBalanceForSolverRole(solverId, solverNetAmount, now)
                } else if (solverAlreadyPaid) {
                    Log.w("SomadhanRepo", "adminResolveDispute(SPLIT): skipped — solver $solverId already paid for problem ${problem.id}, refusing to pay again")
                }

                val userRefundPercent = (100.0 - clampedPercent).coerceIn(0.0, 100.0)
                if (userRefund > 0.0) {
                    refundEscrowOnce(
                        problemId = problem.id,
                        amount = userRefund,
                        userId = problem.userId,
                        solverId = solverId ?: "",
                        problemTitle = problem.title,
                        baseAmount = (baseAmt - solverGrossBase).coerceAtLeast(0.0),
                        extraAmount = (extraAmt - solverGrossExtra).coerceAtLeast(0.0),
                        customDocId = "TRX_REFUND_${problem.id}_SPLIT",
                        escrowId = escrow?.id,
                        refundType = "SPLIT_REFUND",
                        refundPercentage = userRefundPercent
                    )
                }

                if (escrow != null) {
                    val updatedEscrow = escrow.copy(
                        baseAmount = baseAmt,
                        extraAmount = extraAmt,
                        status = "RELEASED",
                        releasedAt = now
                    )
                    escrowDao.insert(updatedEscrow)
                }

                val splitLabel = if (clampedPercent == 50.0) "৫০/৫০ সমঝোতা" else "কাস্টম ভাগাভাগি (${DistanceUtil.toBengaliDigits(clampedPercent.toInt().toString())}% সলভার / ${DistanceUtil.toBengaliDigits((100 - clampedPercent.toInt()).toString())}% ক্লায়েন্ট)"

                // Problem is COMPLETED with dispute resolution record
                val updatedProb = problem.copy(
                    status = "COMPLETED",
                    jobStatus = "JOB_COMPLETED",
                    completedAt = now,
                    isDisputed = true,
                    disputeResolutionDecision = if (clampedPercent == 50.0) "SPLIT_SETTLEMENT" else "CUSTOM_SPLIT",
                    disputeResolutionType = "SPLIT_SETTLEMENT",
                    disputeResolutionNote = "$splitLabel | $decisionNote",
                    disputeResolvedAt = now,
                    disputeSettledAt = now,
                    disputeResultSeenByUser = false,
                    disputeResultSeenBySolver = false,
                    disputeProgressAtSettlement = progressStep,
                    disputeSplitSolverPercent = clampedPercent,
                    lastActivityAt = now
                )
                problemDao.updateProblem(updatedProb)

                val acceptedBidForSplit = problem.acceptedBidId?.let { bidDao.getBidById(it) }
                    ?: (solverId?.let { sid -> bidDao.getBidsForProblemSync(problemId).find { it.solverId == sid } })
                if (acceptedBidForSplit != null) {
                    val stampedBid = acceptedBidForSplit.copy(resolutionType = "ADMIN_SPLIT", resolvedAt = now)
                    bidDao.updateBid(stampedBid)
                }

                // Record solver's earning transaction. Deterministic id (see splitTrxId guard
                // above) so a repeat resolution attempt can be detected instead of a fresh
                // random id slipping past the idempotency check every time.
                val trx = TransactionEntity(
                    id = splitTrxId,
                    problemId = problem.id,
                    problemTitle = problem.title,
                    userId = problem.userId,
                    solverId = solverId ?: "",
                    grossAmount = solverGrossTotal,
                    commissionPercent = solverCommSetting,
                    commissionAmount = commissionAmount,
                    netAmount = solverNetAmount,
                    timestamp = now,
                    baseAmount = solverGrossBase,
                    extraAmount = solverGrossExtra,
                    baseCommissionAmount = solverBreakdown?.baseCommission ?: 0.0,
                    extraCommissionAmount = solverBreakdown?.extraCommission ?: 0.0,
                    extraCommissionApplied = solverBreakdown?.extraCommissionApplied ?: false,
                    wasFreeQuotaJob = solverBreakdown?.wasFreeQuotaJob ?: false,
                    type = "PAYMENT",
                    escrowId = escrow?.id ?: "ESC_${problem.id.take(8).uppercase()}",
                    releaseType = "SPLIT_RELEASE",
                    pendingCloudSync = true,
                    role = "SOLVER"
                )
                transactionDao.insertTransaction(trx)
                // [Step 7.7 cleanup] pendingCloudSync above is a Firebase-era leftover, not
                // read by anything; the actual (retrying) cloud increment for the solver's
                // split share is the Supabase RPC dual-write + outbox retry, further below.

                // [SUPABASE-MIGRATED - ধাপ ২৯.৫] সলভারের split-share পেআউট এতদিন সম্পূর্ণ
                // local Room-only ছিল (ধাপ ২৫-এ এই কারণেই এই পুরো ফাংশন ইচ্ছাকৃতভাবে বাদ রাখা
                // হয়েছিল) -- এখন resolve_dispute_split() দিয়ে best-effort dual-write। Owner-এর
                // অংশ এখানে ফের পাঠানো হয় না (সেটা refundEscrowOnce()-এর ভেতরেই আগে থেকে migrate করা
                // refund_escrow_once দিয়ে যায়, ওপরের if (userRefund > 0.0) ব্লকে ইতিমধ্যে হয়ে গেছে)
                // -- শুধু bookkeeping-এর জন্য userRefund পাঠানো হচ্ছে, যাতে RPC বুঝতে পারে escrow
                // আগে থেকেই বন্ধ হয়ে গেছে কিনা। solverNetAmount/solverGrossTotal/commissionAmount
                // এখানেই (calculateCommissionBreakdown() থেকে) already computed -- RPC এগুলো আবার
                // recompute করে না, শুধু bounds re-verify করে, যাতে cloud balance local-এর সাথে
                // হুবহু মেলে। escrow?.id না থাকলে (তাত্ত্বিকভাবে অসম্ভব, কিন্তু defensive) কল করা
                // হয় না -- RPC-র p_escrow_id আবশ্যক।
                val splitEscrowId = escrow?.id
                if (!splitEscrowId.isNullOrBlank() && SupabaseAuthManager.currentUserId() != null) {
                    SupabaseSyncManager.resolveDisputeSplit(
                        problemId = problem.id,
                        escrowId = splitEscrowId,
                        splitSolverPercent = clampedPercent,
                        solverGrossAmount = solverGrossTotal,
                        commissionAmount = commissionAmount,
                        solverNetAmount = solverNetAmount,
                        userRefundAmount = userRefund,
                        resolutionDecision = updatedProb.disputeResolutionDecision ?: "SPLIT_SETTLEMENT",
                        decisionNote = updatedProb.disputeResolutionNote ?: decisionNote,
                        progressAtSettlement = progressStep
                    ).onSuccess { json ->
                        Log.d("SomadhanRepo", "resolveDisputeSplit: Supabase dual-write OK for ${problem.id}: $json")
                    }.onFailure { e ->
                        Log.w("SomadhanRepo", "resolveDisputeSplit: Supabase dual-write failed for ${problem.id} (local payout already done, not blocked)", e)
                        // [OUTBOX WIRE - Step 12.4] paramsJson keys "problemId"/"escrowId"/"splitSolverPercent"/
                        // "solverGrossAmount"/"commissionAmount"/"solverNetAmount"/"userRefundAmount"/
                        // "resolutionDecision"/"decisionNote"/"progressAtSettlement" -- OutboxRpcDispatcher.kt-এর
                        // "resolve_dispute_split" branch-এর সাথে অক্ষরে অক্ষরে মিলছে। idempotency (migration
                        // step40_dispute_split_dual_write_fix.sql-এ থাকা সর্বশেষ resolve_dispute_split বডি থেকে
                        // যাচাই, স্ট্যাটাসে এটাই replace করে): ডাবল গার্ড -- (ক) problems.dispute_resolved_at
                        // ইতিমধ্যে সেট থাকলে exception ছাড়া 'ALREADY_RESOLVED' রিটার্ন করে (কোনো write আগে হয়
                        // না); (খ) দুটোই পেরোলেও deterministic id ('TRX_SPLIT_'||problemId)-এর transactions row
                        // আগে থেকে থাকলে 'ALREADY_PAID' রিটার্ন করে (solver-balance UPDATE-এর *আগে*)। solver
                        // payout ও transactions insert দুটোই এই গার্ডের পরে, insert-এও `on conflict (id) do
                        // nothing` -- তাই একই resolveDisputeSplit replay দুইবার চললেও সলভারের টাকা দুইবার
                        // যোগ হয় না। ⚠️ owner-এর refund এই RPC করে না (আলাদাভাবে refund_escrow_once() দিয়ে
                        // ইতিমধ্যে dual-write হয়ে গেছে, উপরের if (userRefund > 0.0) ব্লকে) -- তাই এই retry
                        // শুধু solver-এর অংশটাই replay করে, owner-এর অংশ ডাবল হওয়ার প্রশ্নই নেই। বিস্তারিত:
                        // CI_TEST_SUITE_PROGRESS.md "Step 12.4" সেকশন।
                        enqueueOutboxRetry(
                            rpcName = "resolve_dispute_split",
                            params = kotlinx.serialization.json.JsonObject(
                                buildMap {
                                    put("problemId", kotlinx.serialization.json.JsonPrimitive(problem.id))
                                    put("escrowId", kotlinx.serialization.json.JsonPrimitive(splitEscrowId))
                                    put("splitSolverPercent", kotlinx.serialization.json.JsonPrimitive(clampedPercent))
                                    put("solverGrossAmount", kotlinx.serialization.json.JsonPrimitive(solverGrossTotal))
                                    put("commissionAmount", kotlinx.serialization.json.JsonPrimitive(commissionAmount))
                                    put("solverNetAmount", kotlinx.serialization.json.JsonPrimitive(solverNetAmount))
                                    put("userRefundAmount", kotlinx.serialization.json.JsonPrimitive(userRefund))
                                    put("resolutionDecision", kotlinx.serialization.json.JsonPrimitive(updatedProb.disputeResolutionDecision ?: "SPLIT_SETTLEMENT"))
                                    put("decisionNote", kotlinx.serialization.json.JsonPrimitive(updatedProb.disputeResolutionNote ?: decisionNote))
                                    put("progressAtSettlement", kotlinx.serialization.json.JsonPrimitive(progressStep))
                                }
                            ),
                            error = e
                        )
                    }
                }

                if (!solverId.isNullOrBlank()) {
                    triggerDynamicReputationEvent(
                        userId = solverId,
                        eventType = "DISPUTE_SETTLED_FRIENDLY",
                        problemId = problemId,
                        defaultScore = 1.0,
                        defaultCap = 2.0,
                        isPositive = true,
                        customNote = "অ্যাডমিন মধ্যস্থতায় বিরোধ নিষ্পত্তি"
                    )
                }
                triggerDynamicReputationEvent(
                    userId = problem.userId,
                    eventType = "DISPUTE_SETTLED_FRIENDLY",
                    problemId = problemId,
                    defaultScore = 1.0,
                    defaultCap = 2.0,
                    isPositive = true,
                    customNote = "অ্যাডমিন মধ্যস্থতায় বিরোধ নিষ্পত্তি"
                )

                val solverPctBengali = DistanceUtil.toBengaliDigits(clampedPercent.toInt().toString())
                val clientPctBengali = DistanceUtil.toBengaliDigits((100 - clampedPercent.toInt()).toString())
                notificationDao.insertNotification(
                    NotificationEntity(
                        role = "USER",
                        id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                        userId = problem.userId,
                        title = "বিরোধ মীমাংসা সম্পন্ন ⚖️",
                        message = "\"${problem.title}\" বিরোধে অ্যাডমিন মীমাংসা করেছেন ($clientPctBengali% ক্লায়েন্ট / $solverPctBengali% সলভার)। ৳${DistanceUtil.toBengaliDigits(userRefund.toInt().toString())} আপনার ওয়ালেটে ফেরত দেওয়া হয়েছে।",
                        targetType = "balance",
                        targetId = problem.userId,
                        relatedProblemId = problem.id
                    )
                )
                if (!solverId.isNullOrBlank()) {
                    val commText = if (commissionAmount > 0.0) " (প্ল্যাটফর্ম ফি বাদে নিট ৳${DistanceUtil.toBengaliDigits(solverNetAmount.toInt().toString())})" else ""
                    notificationDao.insertNotification(
                        NotificationEntity(
                            role = "SOLVER",
                            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                            userId = solverId,
                            title = "বিরোধ মীমাংসা সম্পন্ন ⚖️",
                            message = "\"${problem.title}\" বিরোধে অ্যাডমিন মীমাংসা করেছেন ($solverPctBengali% সলভার / $clientPctBengali% ক্লায়েন্ট)। ৳${DistanceUtil.toBengaliDigits(solverGrossTotal.toInt().toString())} অংশের মধ্যে$commText আপনার ব্যালেন্সে জমা হয়েছে।",
                            targetType = "balance",
                            targetId = solverId,
                            relatedProblemId = problem.id
                        )
                    )
                }

                sendSystemEventMessage(
                    problemId = problemId,
                    receiverId = problem.userId,
                    eventType = "ADMIN_RESOLVED_SPLIT",
                    content = "⚖️ অ্যাডমিন সিদ্ধান্ত: বিরোধ মীমাংসা করা হয়েছে ($splitLabel)। সলভার মোট অংশ: ৳${DistanceUtil.toBengaliDigits(solverGrossTotal.toInt().toString())}, ক্লায়েন্ট রিফান্ড: ৳${DistanceUtil.toBengaliDigits(userRefund.toInt().toString())}। নোট: $decisionNote"
                )
            }
            "REFUND_TO_USER" -> {
                val escrow = escrowDao.getByProblemId(problemId)
                val (baseAmt, extraAmt) = resolveSettlementAmounts(problem, escrow)
                val totalRefund = baseAmt + extraAmt

                if (totalRefund > 0.0) {
                    refundEscrowOnce(
                        problemId = problem.id,
                        amount = totalRefund,
                        userId = problem.userId,
                        solverId = problem.acceptedSolverId ?: "",
                        problemTitle = problem.title,
                        baseAmount = baseAmt,
                        extraAmount = extraAmt,
                        customDocId = "TRX_REFUND_${problem.id}_DISPUTE",
                        escrowId = escrow?.id,
                        refundType = "DISPUTE_REFUND",
                        refundPercentage = 100.0
                    )
                }

                // Problem is CANCELLED (or OPEN for non-instant jobs).
                // Keep acceptedSolverId intact so both user & solver can access the dispute resolution summary!
                val updatedProb = problem.copy(
                    status = if (problem.isInstantJob) "CANCELLED" else "OPEN",
                    jobStatus = if (problem.isInstantJob) "CANCELLED" else null,
                    hasReleaseRequest = false,
                    releaseRequestExtraAmount = 0.0,
                    releaseRequestNote = "",
                    releaseRequestedAt = null,
                    isDisputed = true,
                    disputeReason = problem.disputeReason,
                    disputeInitiatorId = problem.disputeInitiatorId,
                    disputeResolutionDecision = "REFUND_TO_USER",
                    disputeResolutionType = "REFUND_TO_USER",
                    disputeResolutionNote = decisionNote,
                    disputeResolvedAt = now,
                    disputeSettledAt = now,
                    disputeResultSeenByUser = false,
                    disputeResultSeenBySolver = false,
                    disputeProgressAtSettlement = progressStep,
                    solverCancelledNotice = null,
                    lastActivityAt = now
                )
                problemDao.updateProblem(updatedProb)

                // Cancel accepted solver's bid, restore all other bids to PENDING
                val bids = bidDao.getBidsForProblemSync(problemId)
                val acceptedBidId = problem.acceptedBidId
                bids.forEach { b ->
                    if (b.id == acceptedBidId || b.solverId == problem.acceptedSolverId) {
                        val cancelledBid = b.copy(status = "CANCELLED", progressAtCancel = progressStep, resolutionType = "ADMIN_REFUND_TO_USER", resolvedAt = now)
                        bidDao.updateBid(cancelledBid)
                    } else if (b.status != "CANCELLED" && b.status != "WITHDRAWN" && b.status != "REJECTED") {
                        val restoredBid = b.copy(status = "PENDING")
                        bidDao.updateBid(restoredBid)
                    }
                }

                notificationDao.insertNotification(
                    NotificationEntity(
                        role = "USER",
                        id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                        userId = problem.userId,
                        title = "রিফান্ড সম্পন্ন ও পোস্টটি পুনরায় উন্মুক্ত ↩️",
                        message = "অ্যাডমিন সিদ্ধান্তক্রমে \"${problem.title}\" কাজের ৳${DistanceUtil.toBengaliDigits(totalRefund.toInt().toString())} আপনার ব্যালেন্সে রিফান্ড করা হয়েছে এবং পোস্টটি পুনরায় সকল বিডের জন্য উন্মুক্ত (OPEN) রাখা হয়েছে।",
                        targetType = "balance",
                        targetId = problem.userId,
                        relatedProblemId = problem.id
                    )
                )

                val solverId = problem.acceptedSolverId
                if (!solverId.isNullOrBlank()) {
                    triggerDynamicReputationEvent(
                        userId = solverId,
                        eventType = "DISPUTE_LOST",
                        problemId = problemId,
                        defaultScore = 5.0,
                        defaultCap = 10.0,
                        defaultPenalty = 5.0,
                        isPositive = false,
                        customNote = "অ্যাডমিন তদন্তে বিবাদে দোষী সাব্যস্ত হওয়ার পেনাল্টি"
                    )
                    notificationDao.insertNotification(
                        NotificationEntity(
                            role = "SOLVER",
                            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                            userId = solverId,
                            title = "বিরোধ নিষ্পত্তি ও রিফান্ড ℹ️",
                            message = "অ্যাডমিন সিদ্ধান্ত অনুযায়ী \"${problem.title}\" কাজের অর্থ ক্লায়েন্টকে রিফান্ড করা হয়েছে।",
                            targetType = "problem",
                            targetId = problem.id,
                            relatedProblemId = problem.id
                        )
                    )
                }

                sendSystemEventMessage(
                    problemId = problemId,
                    receiverId = problem.userId,
                    eventType = "ADMIN_RESOLVED_REFUND",
                    content = "⚖️ অ্যাডমিন সিদ্ধান্ত: ক্লায়েন্টকে সম্পূর্ণ অর্থ রিফান্ড করা হয়েছে এবং সমস্যাটি পুনরায় উন্মুক্ত (OPEN) রাখা হয়েছে। নোট: $decisionNote"
                )
            }
        }

        logAdminAction(
            actionType = "ADMIN_RESOLVE_DISPUTE",
            targetId = problemId,
            targetName = problem.title,
            details = "রেজোলিউশন: $resolution, নোট: $decisionNote"
        )
    }

    suspend fun markDisputeResultSeen(problemId: String, isUser: Boolean) {
        val problem = problemDao.getProblemById(problemId) ?: return
        val updated = if (isUser) {
            problem.copy(disputeResultSeenByUser = true)
        } else {
            problem.copy(disputeResultSeenBySolver = true)
        }
        problemDao.updateProblem(updated)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮৫] শুধু UI "দেখা হয়েছে" ফ্ল্যাগ, টাকা/ফাংশনালিটি প্রভাব
        // নেই, কিন্তু Firebase সরালে cross-device sync হারাবে -- তাই dual-write।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.markDisputeResultSeen(problemId, isUser).onFailure { e ->
                Log.w("SomadhanRepo", "markDisputeResultSeen: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }
    }

    suspend fun markCompletionResultSeen(problemId: String, isUser: Boolean) {
        val problem = problemDao.getProblemById(problemId) ?: return
        val updated = if (isUser) {
            problem.copy(completionResultSeenByUser = true)
        } else {
            problem.copy(completionResultSeenBySolver = true)
        }
        problemDao.updateProblem(updated)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮৫] শুধু UI "দেখা হয়েছে" ফ্ল্যাগ, একই কারণে dual-write।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.markCompletionResultSeen(problemId, isUser).onFailure { e ->
                Log.w("SomadhanRepo", "markCompletionResultSeen: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }
    }

    suspend fun adminIssueWarningStrike(
        targetUserId: String,
        problemId: String,
        reason: String,
        penaltyReputation: Double = 5.0,
        customMessage: String = "",
        // [ROLE_SEPARATION ধাপ ৬] caller (AdminDisputeCenterView-এর targetParty) ইতিমধ্যেই জানে
        // কোন role-কে strike দেওয়া হচ্ছে -- notification-এ সেটা ট্যাগ করার জন্য পাস করা হলো।
        // পুরনো caller (এই প্যারামিটার না দিলে) আগের মতোই role-neutral ("") থাকবে, ভাঙবে না।
        targetRole: String = ""
    ) {
        val user = userDao.getUserById(targetUserId) ?: return
        val now = System.currentTimeMillis()
        val penalty = penaltyReputation.coerceAtLeast(0.0)

        if (penalty > 0.0) {
            applyReputationChange(
                userId = targetUserId,
                eventType = "ADMIN_DISPUTE_STRIKE",
                scoreChange = -penalty,
                problemId = problemId,
                note = "বিরোধে অনিয়ম/অসদুপায়ের জন্য অ্যাডমিন সতর্কতা ও রেপুটেশন পেনাল্টি: $reason"
            )
        }

        val finalMsg = if (customMessage.isNotBlank()) customMessage else "বিরোধ কার্যক্রমে নিয়ম লঙ্ঘনের কারণে অ্যাডমিন আপনাকে আনুষ্ঠানিক সতর্কবার্তা (Warning Strike) প্রদান করেছেন। কারণ: $reason"
        val notif = NotificationEntity(
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = targetUserId,
            title = "অ্যাডমিন সতর্কবার্তা ও পেনাল্টি 🚨",
            message = finalMsg,
            timestamp = now,
            role = targetRole,
            targetType = "problem",
            targetId = problemId,
            relatedProblemId = problemId
        )
        notificationDao.insertNotification(notif)

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৬] `create_notification` RPC দিয়ে notif dual-write.
        // এই ফাংশনটা শুধু AdminDisputeCenterView (admin স্ক্রীন) থেকে কল হয়, কাজেই caller সবসময়
        // admin -- RPC-এর admin-path (যেকোনো target-এ পাঠানো অনুমোদিত) পাস করার কথা (targetUserId owner অথবা
        // solver যেকোনোই হতে পারে, party-check প্রয়োজন নেই)। best-effort, ব্যর্থ হলেও local Room flow অপ্রভাবিত।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.createNotification(
                targetUserId = notif.userId,
                title = notif.title,
                message = notif.message,
                targetType = notif.targetType,
                targetId = notif.targetId?.ifBlank { null },
                relatedProblemId = notif.relatedProblemId,
                role = notif.role
            ).onFailure { e ->
                Log.w("SomadhanRepo", "adminIssueWarningStrike: notif dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }

        val strikeNoticeMsg = MessageEntity(
            id = "MSG_${UUID.randomUUID().toString().take(8)}",
            problemId = problemId,
            senderId = "ADMIN_SYSTEM",
            receiverId = targetUserId,
            senderName = "Support Manager 🛡️",
            content = "🚨 অ্যাডমিন নোটিশ: [${user.name}]-কে বিরোধ তদন্তের প্রেক্ষিতে আনুষ্ঠানিক সতর্কবার্তা (Warning Strike) ও ${DistanceUtil.toBengaliDigits(penalty.toInt().toString())} পয়েন্ট পেনাল্টি প্রদান করা হয়েছে। কারণ: $reason",
            timestamp = now,
            isRead = false,
            isAdminMessage = true
        )
        messageDao.insertMessage(strikeNoticeMsg)

        // [SUPABASE-MIGRATED - Admin Action bug-fix master prompt, ধাপ ৭] `admin_send_message_to_problem_chat`
        // RPC-টা generalize করা হয়েছে (নতুন ঐচ্ছিক p_receiver_id প্যারামিটার, owner/accepted_solver_id
        // দুটোই accept করে, RPC নিজেই validate করে) -- তাই আর owner-only শর্ত/skip লাগে না,
        // targetUserId (owner বা solver যেকোনোই) সরাসরি receiverId হিসেবে পাস করা হচ্ছে।
        // best-effort, ব্যর্থ হলেও local Room flow অপ্রভাবিত।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminSendMessageToProblemChat(problemId, strikeNoticeMsg.content, receiverId = targetUserId)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "adminIssueWarningStrike: chat dual-write failed for $problemId (local flow unaffected): ${e.message}")
                }
        }

        logAdminAction(
            actionType = "ADMIN_WARNING_STRIKE",
            targetId = targetUserId,
            targetName = user.name,
            details = "সমস্যা #${problemId}, কারণ: $reason, পেনাল্টি: -$penalty"
        )
    }

    /**
     * refundEscrowOnce: Centralized atomic and idempotent escrow refund engine.
     * Guarantees that regardless of concurrent triggers, retry attempts, or cross-device actions
     * (e.g. Solver cancels on device A while User clicks cancel on device B), the refund transaction
     * and wallet credit occur EXACTLY ONCE.
     *
     * Key mechanisms:
     * 1. Deterministic transaction document ID: "TRX_REFUND_${problemId}"
     * 2. Firestore atomic transaction: performs server-side read check on the refund document & escrow status
     *    and atomically updates user balance and writes the refund record.
     * 3. Local Room consistency: immediately records the refund in Room to provide instant UI feedback.
     */
    suspend fun refundEscrowOnce(
        problemId: String,
        amount: Double,
        userId: String,
        solverId: String,
        problemTitle: String,
        baseAmount: Double = amount,
        extraAmount: Double = 0.0,
        customDocId: String? = null,
        escrowId: String? = null,
        refundType: String = "SOLVER_CANCEL",
        refundPercentage: Double = 100.0
    ): Boolean {
        // Serialize concurrent refund attempts for the SAME escrow (or problem, if no escrowId was
        // passed) so two callers racing each other (e.g. solverCancelJob() and a concurrent
        // reconcileEscrowStates() self-heal pass) can never both pass the "already refunded?"
        // pre-check before either has committed — see refundMutexFor() for the full explanation.
        val mutexKey = escrowId?.takeIf { it.isNotBlank() } ?: "problem:$problemId"
        return refundMutexFor(mutexKey).withLock {
            refundEscrowOnceLocked(
                problemId, amount, userId, solverId, problemTitle,
                baseAmount, extraAmount, customDocId, escrowId, refundType, refundPercentage
            )
        }
    }

    private suspend fun refundEscrowOnceLocked(
        problemId: String,
        amount: Double,
        userId: String,
        solverId: String,
        problemTitle: String,
        baseAmount: Double = amount,
        extraAmount: Double = 0.0,
        customDocId: String? = null,
        escrowId: String? = null,
        refundType: String = "SOLVER_CANCEL",
        refundPercentage: Double = 100.0
    ): Boolean {
        if (amount <= 0.0 || userId.isBlank()) return false

        // Resolve the SPECIFIC escrow this refund belongs to. Prefer the escrowId explicitly
        // passed by the caller — this is what makes the idempotency key unique PER PAYMENT
        // CYCLE instead of per problem (a problem can go through several bid->pay->cancel or
        // dispute cycles, each with its own EscrowEntity row). Falling back to problemId-only
        // lookup is kept only for any caller that doesn't pass escrowId yet.
        val localEscrow = if (!escrowId.isNullOrBlank()) {
            escrowDao.getEscrowById(escrowId) ?: escrowDao.getByProblemId(problemId)
        } else {
            escrowDao.getByProblemId(problemId)
        }
        val now = System.currentTimeMillis()
        val fallbackEscrowId = "ESC_${problemId.take(8).uppercase()}_$now"

        val resolvedEscrowId = localEscrow?.id ?: escrowId

        // Idempotency guard: never refund an escrow that has already reached a terminal state.
        // The local-transaction-history check below only catches "this exact refund doc already
        // ran" — it does nothing to stop refunding an escrow that was already paid OUT to the
        // solver via confirmReleaseAndComplete()/adminReleaseEscrow() (status "RELEASED"). Every
        // cancel path (cancelInstantJob, solverCancelJob, solverCancelAcceptedJob, normal-problem
        // cancel, dispute refund, etc.) funnels through here with no terminal-state check of its
        // own — if any of them were ever invoked on a job that had already completed (a stale UI
        // screen, an admin action racing a completion, state corruption), this would otherwise
        // credit the user's wallet with money that was already paid to the solver — a double
        // payout of the same escrow. This is scoped to the SPECIFIC resolved escrow row only, so
        // it never blocks refunding a later, brand-new payment cycle's escrow for the same
        // problem (openEscrow() always mints a fresh id per cycle).
        if (localEscrow != null && localEscrow.status in setOf("RELEASED", "REFUNDED", "REFUND_PENDING_SYNC")) {
            Log.w("SomadhanRepo", "refundEscrowOnce: skipped — escrow ${localEscrow.id} is already ${localEscrow.status}, refusing to refund again")
            return false
        }

        val refundDocId = customDocId
            ?: resolvedEscrowId?.let { "TRX_REFUND_$it" }
            ?: "TRX_REFUND_${fallbackEscrowId}"

        // 1. Fast local Room idempotency check — scoped to THIS escrow/refund doc, not the
        // whole problem. (Matching any REFUND row for the problemId was the root cause: it made
        // every payment cycle after the first look like "already refunded".)
        val localRefunds = transactionDao.getTransactionsForProblem(problemId).filter { trx ->
            trx.id == refundDocId ||
                (trx.type == "REFUND" && !resolvedEscrowId.isNullOrBlank() && trx.escrowId == resolvedEscrowId)
        }
        if (localRefunds.isNotEmpty()) {
            Log.d("SomadhanRepo", "refundEscrowOnce: Already refunded locally for escrow $resolvedEscrowId (problem $problemId)")
            return false
        }

        // 2. INSTANT local credit. Wallet balance, escrow status, and transaction history are
        // written to Room immediately — no network round trip on the critical path, so the UI
        // reflects the refund the moment this call returns. [Step 7.7 cleanup] The matching
        // cloud write is the `refund_escrow_once` Supabase RPC dual-write further below, which
        // is atomic and idempotent on the server side (checked by result/status, see the RPC
        // call's comment); on failure it enqueues an outbox retry (enqueueOutboxRetry(),
        // replayed by OutboxSyncWorker) instead of leaving anything for a client-side flag to
        // pick up. The TransactionEntity's pendingCloudSync/cloudBalanceSynced fields set below,
        // and the Firestore/FirebaseSyncManager.syncPendingCloudRefunds() mechanism the old
        // version of this comment described in detail, are all Firebase-era leftovers:
        // syncPendingCloudRefunds() no longer exists in the codebase and nothing reads either
        // flag anymore.
        // [ব্যালেন্স ফিক্স] escrow refund সবসময় যে user পোস্ট করেছিল তার কাছেই ফেরত যায়,
        // তাই balanceUser mirror-ও একসাথে আপডেট হয়।
        userDao.addBalanceForUserRole(userId, amount, now)
        if (localEscrow != null) {
            val currentEscrowState = escrowDao.getEscrowById(localEscrow.id)
            if (currentEscrowState != null && currentEscrowState.solverId != solverId) {
                // এই সময়ের মধ্যে escrow অন্য (নতুন) সলভারকে reassign হয়ে গেছে (acceptBid()-এর
                // "reuse" পাথ দিয়ে) — এই escrow-row-এর status touch করা যাবে না, নাহলে
                // নতুন সলভারের সক্রিয় HELD escrow ভুলভাবে REFUNDED হয়ে যাবে।
                Log.w("SomadhanRepo", "refundEscrowOnce: escrow ${localEscrow.id} was reassigned to solver ${currentEscrowState.solverId} mid-refund (original solver was $solverId) — skipping escrow status write, but still crediting wallet since the refund is legitimately owed")
            } else {
                val updatedEscrow = localEscrow.copy(status = "REFUND_PENDING_SYNC", releasedAt = now)
                escrowDao.insert(updatedEscrow)
            }
        }

        val refundTrx = TransactionEntity(
            id = refundDocId,
            problemId = problemId,
            problemTitle = problemTitle,
            userId = userId,
            solverId = solverId,
            grossAmount = amount,
            commissionPercent = 0.0,
            commissionAmount = 0.0,
            netAmount = amount,
            timestamp = now,
            baseAmount = baseAmount,
            extraAmount = extraAmount,
            baseCommissionAmount = 0.0,
            extraCommissionAmount = 0.0,
            extraCommissionApplied = false,
            wasFreeQuotaJob = false,
            type = "REFUND",
            escrowId = resolvedEscrowId ?: fallbackEscrowId,
            pendingCloudSync = true,
            cloudBalanceSynced = false,
            refundType = refundType,
            refundPercentage = refundPercentage,
            role = "USER"
        )
        transactionDao.insertTransaction(refundTrx)

        // 3. Push to the cloud in the background — does not block the caller or the UI. The
        // `refund_escrow_once` RPC below is the real mechanism (see the comment on step 2
        // above); it's safe to call even while another refund for a different escrow is in
        // flight.

        // [SUPABASE-MIGRATED - ধাপ ৯, guard broadened ধাপ ১২ ব্যাচ ৫] `refund_escrow_once` RPC
        // সোর্স পড়ে যাচাই করা হয়েছে — owner, solver, বা admin যে কেউ কল করতে পারে (আর
        // escrow.user_id-কেই টাকা ফেরত দেয়, solver_id-কে না — সঠিক)। refundEscrowOnceLocked()
        // হলো একমাত্র জায়গা যেখান দিয়ে প্রতিটা refund path (normal cancel, dispute refund,
        // adminRefundEscrow, reconcileEscrowStates self-heal, ইত্যাদি) যায়, তাই এখানে একবার
        // dual-write যোগ করলেই সব path কভার হয়। জানা সীমাবদ্ধতা: solverCancelJob() নিজেই
        // আলাদাভাবে SupabaseSyncManager.solverCancelJob() RPC কল করে (ধাপ ৮), যেটা ভেতরে
        // refund_escrow_once() আবার কল করে — অর্থাৎ solver-cancel পথে এই RPC দুইবার কল হতে পারে,
        // কিন্তু RPC নিজেই idempotent (status/transaction-id ডাবল-চেক আছে, উপরে যাচাই করা হয়েছে)
        // তাই এটা harmless, শুধু একটা বাড়তি নেটওয়ার্ক কল। গার্ড আগে শুধু
        // `currentUserId() == userId || currentUserId() == solverId` ছিল — এর ফলে
        // adminRefundEscrow() (caller = admin, owner/solver কোনোটাই না) থেকে কখনো এই RPC কল-ই
        // হতো না, যদিও RPC নিজেই admin authorize করে। ধাপ ১২ ব্যাচ ৫-এ গার্ড শিথিল করা হলো (শুধু
        // session আছে কিনা) — RPC নিজেই owner/solver/admin authorization চূড়ান্তভাবে যাচাই করে।
        if (!resolvedEscrowId.isNullOrBlank() && SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.refundEscrow(resolvedEscrowId, refundType, refundPercentage)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK") {
                        Log.w("SomadhanRepo", "refundEscrowOnceLocked: Supabase dual-write non-OK result for escrow $resolvedEscrowId: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "refundEscrowOnceLocked: Supabase dual-write failed for escrow $resolvedEscrowId (local flow unaffected): ${e.message}")
                    // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ১] paramsJson keys "escrowId"/"refundType"/
                    // "refundPercentage" -- OutboxRpcDispatcher.kt-এর "refund_escrow_once"
                    // branch-এর সাথে মিলছে (কমেন্টে যাচাই করা)। এই if-ব্লকের ভেতরে
                    // resolvedEscrowId already !isNullOrBlank() চেক করা (guard condition),
                    // তাই এখানে non-null smart-cast নিরাপদ।
                    // [Step 12.10d] স্থায়ী প্রত্যাখ্যান (NOT_AUTHORIZED, INVALID_REFUND_PERCENTAGE...) retry-তে পাঠানো হয় না।
                    if (RpcErrorClassifier.isPermanent(e)) {
                        Log.e("SomadhanRepo", "refundEscrowOnceLocked: server permanently rejected refund for escrow $resolvedEscrowId: ${e.message} -- outbox-এ পাঠানো হয়নি")
                    } else {
                        enqueueOutboxRetry(
                            rpcName = "refund_escrow_once",
                            params = kotlinx.serialization.json.JsonObject(
                                mapOf(
                                    "escrowId" to kotlinx.serialization.json.JsonPrimitive(resolvedEscrowId),
                                    "refundType" to kotlinx.serialization.json.JsonPrimitive(refundType),
                                    "refundPercentage" to kotlinx.serialization.json.JsonPrimitive(refundPercentage)
                                )
                            ),
                            error = e
                        )
                    }
                }
        }

        return true
    }

    /**
     * Proactively reconciles all escrow states across Room and Firestore.
     * Fixes any escrows that are stuck in HELD status if the job is cancelled/refunded/completed.
     */
    suspend fun reconcileEscrowStates() {
        try {
            val now = System.currentTimeMillis()
            val allEscrows = escrowDao.getAllEscrowsSync()
            val heldEscrows = allEscrows.filter { it.status.equals("HELD", ignoreCase = true) }
            if (heldEscrows.isEmpty()) return

            var repairedCount = 0
            val maxRepairsPerRun = 5

            for (escrow in heldEscrows) {
                val problem = problemDao.getProblemById(escrow.problemId)
                val refunds = transactionDao.getTransactionsForProblem(escrow.problemId).filter {
                    it.type.equals("REFUND", ignoreCase = true) && it.escrowId == escrow.id
                }
                val isCancelledOrUnassignedOpen = problem != null && (
                    problem.status == "CANCELLED" ||
                    (problem.status == "OPEN" && problem.acceptedSolverId.isNullOrBlank())
                )

                if (refunds.isNotEmpty()) {
                    // Case 1: Verified refund transaction already exists -> Safe status sync
                    val targetStatus = "REFUNDED"
                    val updated = escrow.copy(status = targetStatus, releasedAt = escrow.releasedAt ?: now)
                    escrowDao.insert(updated)

                    // আগে এখানে raw Firestore db.collection("escrows").document(...).update()
                    // কল ছিল -- dead leftover, db এখন AppDatabase/Room, Firestore SDK নেই
                    // (ধাপ ৩৩.৩-এ dependency সরানো হয়েছে), তাই এটা compile হতোই না। নিচের
                    // ফাংশন-শেষের SupabaseSyncManager.adminReconcileEscrowStates() dual-write
                    // ইতিমধ্যে cloud-side sync কাভার করে বলে এই ব্লকটা সম্পূর্ণ সরিয়ে ফেলা হলো
                    // (ধাপ ৩৩.৫ ফাইনাল-গ্রেপ চলাকালীন আবিষ্কৃত ও ফিক্স করা হয়েছে)।
                } else if (isCancelledOrUnassignedOpen) {
                    // Case 2: Problem was cancelled or open without solver, but NO refund transaction exists!
                    // Do NOT simply mark as REFUNDED. Trigger full idempotent refund execution.
                    if (repairedCount < maxRepairsPerRun) {
                        repairedCount++
                        Log.e(
                            "SomadhanRepo",
                            "reconcileEscrowStates: Detected unrefunded held escrow for cancelled/unassigned problem ${escrow.problemId}. Triggering self-healing refundEscrowOnce ($repairedCount/$maxRepairsPerRun)."
                        )
                        refundEscrowOnce(
                            problemId = escrow.problemId,
                            amount = escrow.baseAmount + escrow.extraAmount,
                            userId = escrow.userId,
                            solverId = escrow.solverId,
                            problemTitle = problem?.title ?: "সমস্যা সমাধান রিফান্ড",
                            baseAmount = escrow.baseAmount,
                            extraAmount = escrow.extraAmount,
                            escrowId = escrow.id
                        )
                    } else {
                        Log.w(
                            "SomadhanRepo",
                            "reconcileEscrowStates: Reached max repairs limit ($maxRepairsPerRun) for this run. Problem ${escrow.problemId} will be processed on the next cycle."
                        )
                    }
                } else if (problem?.status == "COMPLETED" && refunds.isEmpty()) {
                    // Case 3: Problem completed but the escrow was left HELD without the solver
                    // ever actually being paid. This used to just stamp escrow.status =
                    // "RELEASED" directly here -- since RELEASED is a terminal status, nothing
                    // would ever retry it, so the money was silently lost (no wallet credit, no
                    // Transaction record, no admin alert). Now it runs the real payout through
                    // the same helper confirmReleaseAndComplete()/adminReleaseEscrow() use, and
                    // raises an admin alert so this inconsistent state doesn't go unnoticed.
                    if (repairedCount < maxRepairsPerRun) {
                        repairedCount++
                        val completedProblem = problem
                        val solverId = escrow.solverId.ifBlank { completedProblem?.acceptedSolverId ?: "" }
                        if (completedProblem == null || solverId.isBlank()) {
                            Log.e(
                                "SomadhanRepo",
                                "reconcileEscrowStates: Case-3 skipped — HELD escrow ${escrow.id} for COMPLETED problem ${escrow.problemId} has no solverId to pay out."
                            )
                        } else {
                            Log.e(
                                "SomadhanRepo",
                                "reconcileEscrowStates: Detected COMPLETED problem ${escrow.problemId} with un-paid-out HELD escrow ${escrow.id}. Triggering self-healing payoutEscrowToSolver ($repairedCount/$maxRepairsPerRun)."
                            )
                            // [Step 12.10d] সার্ভার স্থায়ীভাবে প্রত্যাখ্যান করলে (যেমন PROBLEM_DISPUTED) এই repair এড়ানো হয় —
                            // কোনো local credit হয় না, প্রতি রানে অমিল তৈরিও হয় না।
                            val releaseRejected = try {
                                releaseEscrowCloudFirst(escrow)
                                false
                            } catch (rejected: EscrowReleaseRejectedException) {
                                Log.e("SomadhanRepo", "reconcileEscrowStates: Case-3 repair skipped for escrow ${escrow.id} -- সার্ভার স্থায়ীভাবে প্রত্যাখ্যান করেছে: ${rejected.message}")
                                true
                            }
                            if (!releaseRejected) {
                                payoutEscrowToSolver(
                                    escrow = escrow,
                                    problem = completedProblem,
                                    solverId = solverId,
                                    baseAmount = escrow.baseAmount,
                                    extraAmount = escrow.extraAmount,
                                    releaseType = "AUTO_REPAIR",
                                    cloudReleaseHandled = true
                                )
                                logAdminAction(
                                    actionType = "ESCROW_PAYOUT_AUTO_REPAIRED",
                                    targetId = escrow.id,
                                    targetName = completedProblem.title,
                                    details = "escrow ছিল HELD কিন্তু জব সম্পন্ন ছিল; স্বয়ংক্রিয়ভাবে সলভারকে ৳${(escrow.baseAmount + escrow.extraAmount).toInt()} পেআউট করা হয়েছে (problemId=${escrow.problemId})"
                                )
                            }
                        }
                    } else {
                        Log.w(
                            "SomadhanRepo",
                            "reconcileEscrowStates: Reached max repairs limit ($maxRepairsPerRun) for this run. Problem ${escrow.problemId} will be processed on the next cycle."
                        )
                    }
                }
            }

            // [SUPABASE-MIGRATED - ধাপ ৩২.৬] admin-only money-reconciliation টুল, best-effort
            // dual-write। guard শুধু session আছে কিনা (RPC নিজেই is_admin(auth.uid()) চেক করে,
            // adminAdjustBalance()-এর established প্যাটার্নে)। এই Kotlin ফাংশন সবসময় live-run
            // (কোনো dryRun প্যারামিটার নেই), তাই RPC-ও dryRun=false দিয়ে কল হয়।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.adminReconcileEscrowStates(dryRun = false)
                    .onFailure { e ->
                        Log.w("SomadhanRepo", "reconcileEscrowStates: Supabase dual-write failed (local flow unaffected): ${e.message}")
                        // [OUTBOX WIRE - Step 12.5] paramsJson key "dryRun" -- OutboxRpcDispatcher.kt-এর
                        // "admin_reconcile_escrow_states" branch-এর সাথে অক্ষরে অক্ষরে মিলছে। idempotency
                        // (migration step32_6_admin_reconcile_escrow_states.sql বডি থেকে যাচাই): শুধু
                        // status='HELD' escrow স্ক্যান করে -- ইতিমধ্যে REFUNDED/RELEASED হওয়া escrow
                        // পুনরায় select-ই হয় না (query-scope নিজেই ২য়বার নিরাপদ), আর ভেতরে কল হওয়া
                        // refund_escrow_once()/release_escrow() দুটোই নিজেদের status-গার্ডেড। IDEMPOTENT।
                        enqueueOutboxRetry(
                            rpcName = "admin_reconcile_escrow_states",
                            params = kotlinx.serialization.json.JsonObject(
                                buildMap {
                                    put("dryRun", kotlinx.serialization.json.JsonPrimitive(false))
                                }
                            ),
                            error = e
                        )
                    }
            }
        } catch (e: Exception) {
            Log.e("SomadhanRepo", "reconcileEscrowStates error: ${e.message}", e)
        }
    }

    /**
     * cleanupDuplicateRefunds: Admin maintenance utility to detect and clean up any historical
     * duplicate refund transactions caused by earlier race conditions.
     * Keeps the primary/earliest refund transaction for each problem and removes extra duplicates,
     * adjusting the user's wallet balance if duplicate excess funds were credited.
     */
    // Key in platform_settings (synced to/from Firestore like every other platform setting, so
    // it's effectively shared across every admin's device, not per-device local state) tracking
    // when reconcileUserBalances() last ran, so maybeAutoReconcileBalances() below can space
    // automatic runs out across the whole admin user base instead of every admin device running
    // its own independent copy every time any admin happens to open the app.
    private val LAST_BALANCE_RECONCILIATION_KEY = "last_balance_reconciliation_run_at"

    // Minimum gap between automatic reconciliation runs. ~8 hours gives roughly 2-3 runs across
    // a day of normal admin app usage without scanning every user's full transaction history on
    // every single app open (which would be wasteful/slow at any real scale) -- this is
    // opportunistic (piggybacks on whenever an admin happens to have the app open), not a true
    // fixed-schedule background job (that would need WorkManager, deliberately out of scope here
    // per the earlier conversation about staying with the client-side approach for now).
    private val AUTO_RECONCILE_MIN_GAP_MS = 8L * 60 * 60 * 1000L

    /**
     * Ledger-vs-stored-balance safety net. reconcileUserBalances() answers a DIFFERENT question
     * than the RPC dual-write + outbox retry mechanism used everywhere else in this file: that
     * retry only catches balance changes the app ALREADY KNOWS are still pending (via the
     * outbox queue). This catches balance changes nothing is tracking anymore at all -- e.g. an
     * app crash mid-write, a bug in older code, or any other drift -- by recomputing what each
     * user's balance SHOULD be purely from the transaction ledger (the actual source of truth)
     * and comparing against what's stored on the UserEntity.
     *
     * dryRun = true (default, and what the automatic 2-3x/day trigger always uses) only detects
     * and reports mismatches -- it changes nothing. Only an explicit dryRun = false call (wired
     * to an admin-only "সংশোধন করুন" button, mirroring repairMissingRefunds()'s existing
     * dry-run-by-default convention) actually corrects a balance: instant local Room write, then
     * the `admin_reconcile_user_balances` Supabase RPC dual-write below (outbox retry on
     * failure) -- the same RPC + outbox pattern as every other balance change in this file. The
     * `pendingCloudSync = true` marker also set on the correction TransactionEntity is, like
     * everywhere else, an unread Firebase-era leftover [Step 7.7 cleanup].
     */
    suspend fun reconcileUserBalances(dryRun: Boolean = true): BalanceReconciliationReport {
        val mismatches = mutableListOf<BalanceMismatchItem>()
        var correctedCount = 0
        var totalAbsDiff = 0.0
        var scannedCount = 0
        var unclassifiedLegacyCount = 0
        var unclassifiedLegacyNetAmount = 0.0

        try {
            val allUsers = userDao.getAllUsersList()
            val allTransactions = transactionDao.getAllTransactionsList()
            scannedCount = allUsers.size

            // Single O(n) pass building each user's ledger total, instead of re-scanning the
            // full transaction list once per user (O(users * transactions) -- slow at any real
            // scale). "PAYMENT" is the one type where the money moved to solverId, not userId
            // (see payoutEscrowToSolver()/adminResolveDisputeLocked()'s SPLIT branch, both of
            // which set solverId to the actual earning solver and netAmount to what was added to
            // THEIR balance); "BALANCE_RECONCILIATION" is excluded entirely (see below -- it
            // would otherwise perpetuate its own mismatch forever instead of closing it); every
            // other type used in this file (WALLET_DEPOSIT, REFUND, WITHDRAWAL_REFUND,
            // WITHDRAWAL_DEDUCTION, BID_ACCEPT_DEDUCTION, EXTRA_CHARGE_DEDUCTION,
            // RELEASE_DEDUCTION, ADMIN_ADJUSTMENT, DUPLICATE_CORRECTION) is a userId-side ledger
            // entry with netAmount already correctly signed (positive for a credit, negative for
            // a deduction) -- see each function's own comments from this session for why
            // solverId is deliberately left blank on the deduction types, which is exactly what
            // keeps them from being double-counted here too.
            //
            // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ৪] আগে এই ম্যাপটা flat
            // `HashMap<userId, Double>` ছিল, আর তুলনা হতো merged shared `user.balance`-এর
            // সাথে। কিন্তু `user.balance` আসলে merged sum না -- এটা "এই মুহূর্তে active role
            // (`user.role`)-এর balance"-এর mirror (দেখুন AppDaos.kt-এর
            // addBalanceForUserRole/SolverRole-এর cross-role-mix-বিরোধী CASE guard কমেন্ট)।
            // তাই আগের কোড আসলে "active role-এর balance" vs "USER+SOLVER দুই role-এর
            // transaction মিলিয়ে merged ledger sum" তুলনা করত -- এই দুটো ভিন্ন জিনিস, ফলে
            // dual-role ইউজারদের জন্য মিথ্যা mismatch (বা মিথ্যা মিল, কাকতালীয়ভাবে) দুটোই হতে
            // পারত। এখন key = (userId, role) -- প্রতিটা role-এর ledger sum আলাদাভাবে
            // `user.balanceUser`/`user.balanceSolver`-এর সাথে তুলনা হয়।
            val ledgerByUserRole = HashMap<Pair<String, String>, Double>()
            for (trx in allTransactions) {
                if (trx.type.equals("BALANCE_RECONCILIATION", ignoreCase = true)) {
                    // Deliberately excluded from the ledger sum. This transaction's whole purpose
                    // is to document a prior correction that brought balanceUser/balanceSolver up
                    // to what the ledger already said -- it is not new "real" activity. If it
                    // were counted here, it would shift ledgerSum by the exact same amount
                    // addBalanceForUserRole/SolverRole just shifted the balance by, which leaves
                    // the diff completely unchanged on the next audit -- the same mismatch
                    // reappears every run, forever, instead of closing to 0.
                    continue
                }
                // role ফিল্ড ধাপ ৩-এ যোগ হয়েছে -- এর আগে তৈরি row-গুলোয় role = "" (blank)।
                // TRANSACTION_ROLE_FIELD_DESIGN.md #৫ (অপশন B, ব্যবহারকারীর কনফার্মড সিদ্ধান্ত):
                // blank হলে type থেকে read-time backfill করো (destructive DB write না করে),
                // truly ambiguous হলে (ADMIN_ADJUSTMENT/অজানা type) null রেখে unclassified
                // হিসেবে গোনা -- কোনো role-এর ledger-এ যোগ না করে শুধু informational রাখা।
                val effectiveRole = when (trx.role) {
                    "USER", "SOLVER" -> trx.role
                    else -> resolveLegacyTransactionRole(trx)
                }
                val targetId = if (trx.type.equals("PAYMENT", ignoreCase = true)) trx.solverId else trx.userId
                if (targetId.isBlank()) continue
                if (effectiveRole == null) {
                    unclassifiedLegacyCount++
                    unclassifiedLegacyNetAmount += trx.netAmount
                    continue
                }
                val key = targetId to effectiveRole
                ledgerByUserRole[key] = (ledgerByUserRole[key] ?: 0.0) + trx.netAmount
            }

            for (user in allUsers) {
                // দুই role-ই আলাদাভাবে চেক হয় -- একজন dual-role ইউজারের দুই role-এই আলাদা
                // mismatch থাকতে পারে (একটার সংশোধন আরেকটাকে প্রভাবিত করে না)।
                for (role in listOf("USER", "SOLVER")) {
                    val storedBalance = if (role == "USER") user.balanceUser else user.balanceSolver
                    val ledgerSum = ledgerByUserRole[user.id to role] ?: 0.0
                    val diff = storedBalance - ledgerSum
                    // ৳1 tolerance absorbs harmless floating-point drift from summing many small
                    // amounts -- not treated as a real mismatch.
                    if (kotlin.math.abs(diff) <= 1.0) continue

                    mismatches.add(
                        BalanceMismatchItem(
                            userId = user.id,
                            userName = user.name,
                            role = role,
                            storedBalance = storedBalance,
                            ledgerBalance = ledgerSum,
                            difference = diff
                        )
                    )
                    totalAbsDiff += kotlin.math.abs(diff)

                    if (!dryRun) {
                        val correctionAmount = ledgerSum - storedBalance
                        // [ব্যালেন্স ফিক্স] cloud-সাইড `admin_reconcile_user_balances` RPC
                        // (নিচে, best-effort dual-write) ইতিমধ্যেই role-scoped —
                        // balance_user/balance_solver আলাদা আলাদা নিজস্ব ledger দিয়ে সঠিকভাবে
                        // সংশোধন করে সার্ভার-সাইডে। এখানের local echo-টা শুধু এই ডিভাইসের
                        // তাৎক্ষণিক UI-এর জন্য -- উপরের loop-এ নির্ধারিত `role`-ই এখন সরাসরি
                        // ব্যবহার হচ্ছে (আগের মতো ইউজারের *বর্তমান active* `user.role` অনুমান
                        // করে না, যেটা ভুল হতে পারত -- মাস্টার প্রম্পটের ধাপ ৪ ঠিক এই ভুলটাই
                        // এড়াতে বলেছে)।
                        if (correctionAmount > 0.0) {
                            if (role == "SOLVER") userDao.addBalanceForSolverRole(user.id, correctionAmount)
                            else userDao.addBalanceForUserRole(user.id, correctionAmount)
                        } else if (correctionAmount < 0.0) {
                            if (role == "SOLVER") userDao.deductBalanceForSolverRole(user.id, -correctionAmount)
                            else userDao.deductBalanceForUserRole(user.id, -correctionAmount)
                        }
                        val correctionTrx = TransactionEntity(
                            id = "TRX_RECONCILE_${user.id}_${role}_${System.currentTimeMillis()}",
                            problemId = "",
                            problemTitle = "ব্যালেন্স সংশোধন (লেজার অডিট)",
                            userId = user.id,
                            solverId = "",
                            grossAmount = kotlin.math.abs(correctionAmount),
                            commissionPercent = 0.0,
                            commissionAmount = 0.0,
                            netAmount = correctionAmount,
                            type = "BALANCE_RECONCILIATION",
                            pendingCloudSync = true,
                            role = role
                        )
                        transactionDao.insertTransaction(correctionTrx)
                        correctedCount++

                        logAdminAction(
                            actionType = "BALANCE_RECONCILIATION",
                            targetId = user.id,
                            targetName = user.name,
                            details = "[$role] স্টোরড ব্যালেন্স ৳${DistanceUtil.toBengaliDigits(storedBalance.toInt().toString())} থেকে লেজার অনুযায়ী ৳${DistanceUtil.toBengaliDigits(ledgerSum.toInt().toString())}-এ সংশোধন করা হয়েছে (পার্থক্য ৳${DistanceUtil.toBengaliDigits(diff.toInt().toString())})"
                        )
                    } else {
                        Log.w(
                            "SomadhanRepo",
                            "reconcileUserBalances (dry-run): mismatch for user=${user.id} (${user.name}) role=$role stored=$storedBalance ledger=$ledgerSum diff=$diff"
                        )
                    }
                }
            }

            if (unclassifiedLegacyCount > 0) {
                Log.w(
                    "SomadhanRepo",
                    "reconcileUserBalances: $unclassifiedLegacyCount legacy transaction(s) with unresolvable role (netAmount sum=$unclassifiedLegacyNetAmount) excluded from ledger sums -- see TRANSACTION_ROLE_FIELD_DESIGN.md #5"
                )
            }
        } catch (e: Exception) {
            Log.e("SomadhanRepo", "reconcileUserBalances failed: ${e.message}", e)
        }

        // [SUPABASE-MIGRATED - ধাপ ৩২.৬] best-effort dual-write, dryRun প্যারামিটার হুবহু পাস করা
        // হয় (local ও cloud একই মোডে চলে)। guard শুধু session আছে কিনা, RPC নিজেই is_admin() চেক
        // করে।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminReconcileUserBalances(dryRun = dryRun)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "reconcileUserBalances: Supabase dual-write failed (local flow unaffected): ${e.message}")
                    // [OUTBOX WIRE - Step 12.5] paramsJson key "dryRun" -- OutboxRpcDispatcher.kt-এর
                    // "admin_reconcile_user_balances" branch-এর সাথে অক্ষরে অক্ষরে মিলছে। idempotency
                    // (migration step37_admin_reconcile_user_balances_v2_flat_balance.sql বডি থেকে
                    // যাচাই, এটাই সর্বশেষ লাইভ ভার্সন): প্রতিটা user-এর জন্য ledger sum recompute করে
                    // stored balance-এর সাথে তুলনা করে SET করে (increment না); নিজের তৈরি
                    // BALANCE_RECONCILIATION correction-transaction ledger sum-এ বাদ দেওয়া হয় (`type <>
                    // 'BALANCE_RECONCILIATION'`), তাই ২য়বার চললে diff আর পাওয়া যায় না, কিছু আপডেট হয়
                    // না। IDEMPOTENT (recompute-and-SET, converges)।
                    enqueueOutboxRetry(
                        rpcName = "admin_reconcile_user_balances",
                        params = kotlinx.serialization.json.JsonObject(
                            buildMap {
                                put("dryRun", kotlinx.serialization.json.JsonPrimitive(dryRun))
                            }
                        ),
                        error = e
                    )
                }
        }

        return BalanceReconciliationReport(
            dryRun = dryRun,
            scannedUsersCount = scannedCount,
            mismatchCount = mismatches.size,
            correctedCount = correctedCount,
            totalAbsoluteDifference = totalAbsDiff,
            items = mismatches,
            unclassifiedLegacyCount = unclassifiedLegacyCount,
            unclassifiedLegacyNetAmount = unclassifiedLegacyNetAmount
        )
    }

    // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ৪, TRANSACTION_ROLE_FIELD_DESIGN.md #৫ অপশন B]
    // ধাপ ৩-এর আগে তৈরি হওয়া transaction row-গুলোর role blank ("") -- destructive backfill না
    // করে (rule #৪), reconcileUserBalances()-এর ভেতরে শুধু read-time (in-memory) এই
    // type-ভিত্তিক mapping দিয়ে role অনুমান করা হয়। প্রতিটা mapping
    // TRANSACTION_ROLE_FIELD_DESIGN.md-এর #২ টেবিলে থাকা প্রতিটা creation site-এর হার্ডকোড
    // role-এর সাথে হুবহু মেলে (প্রতিটা type মোটামুটি একটাই call-site থেকে আসে)।
    // ADMIN_ADJUSTMENT (এবং BALANCE_RECONCILIATION, যদিও সেটা আলাদাভাবে আগেই বাদ পড়ে যায়
    // caller-এ) থেকে role অনুমান করা অসম্ভব -- admin যেকোনো role-এর balance adjust করতে পারত,
    // তাই এগুলো null (truly unclassified) থেকে যায়, কোনো role-এর ledger sum-এ যোগ হয় না।
    private fun resolveLegacyTransactionRole(trx: TransactionEntity): String? = when (trx.type) {
        "PAYMENT" -> "SOLVER"
        "WALLET_DEPOSIT", "REFUND", "BID_ACCEPT_DEDUCTION", "RELEASE_DEDUCTION",
        "EXTRA_CHARGE_DEDUCTION", "DUPLICATE_CORRECTION" -> "USER"
        "WITHDRAWAL_DEDUCTION", "WITHDRAWAL_REFUND" -> "SOLVER"
        else -> null // ADMIN_ADJUSTMENT, BALANCE_RECONCILIATION, বা অজানা কোনো type
    }

    /**
     * Opportunistic auto-trigger for reconcileUserBalances(), meant to be called once after a
     * successful full sync (see FirebaseSyncManager.pullAllCloudDataToLocal() call sites in
     * SomadhanViewModel.kt) -- ONLY when the signed-in user is an admin, since this scans every
     * user's full transaction history and shouldn't run on every ordinary user's/solver's device
     * on every app open. Always runs dry-run (detect + log only, never auto-corrects) -- an
     * admin still has to review and explicitly tap "সংশোধন করুন" (dryRun = false) to actually
     * change anyone's balance. Spaced out via AUTO_RECONCILE_MIN_GAP_MS using the shared
     * platform_settings timestamp so it lands at roughly 2-3 runs/day in practice across however
     * many admins actually use the app that day, not once per admin device per app-open.
     */
    suspend fun maybeAutoReconcileBalances() {
        try {
            val lastRunAt = platformSettingDao.getSetting(LAST_BALANCE_RECONCILIATION_KEY)?.toLongOrNull() ?: 0L
            val now = System.currentTimeMillis()
            if (now - lastRunAt < AUTO_RECONCILE_MIN_GAP_MS) return

            // Claim this run BEFORE doing the (potentially slow) scan, so two admin devices
            // opening the app at nearly the same moment don't both start a full scan.
            val settingEntity = PlatformSettingEntity(LAST_BALANCE_RECONCILIATION_KEY, now.toString())
            platformSettingDao.insertSetting(settingEntity)

            // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৬] `platform_settings` টেবিলে কোনো RPC নেই --
            // সরাসরি Postgrest upsert (RLS-ই admin-only লেখা নিশ্চিত করে, দেখুন
            // SupabaseSyncManager.upsertPlatformSetting()-এর KDoc)। এই ফাংশনটা শুধু
            // role=="ADMIN" চেক করা SomadhanViewModel থেকেই কল হয় (repository.maybeAutoReconcileBalances()
            // এর caller দেখুন), তাই RLS পাশ হওয়ার কথা -- ব্যর্থ হলেও local Room flow অপ্রভাবিত।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.upsertPlatformSetting(LAST_BALANCE_RECONCILIATION_KEY, now.toString())
                    .onFailure { e ->
                        Log.w("SomadhanRepo", "maybeAutoReconcileBalances: platform_settings dual-write failed (local flow unaffected): ${e.message}")
                    }
            }

            val report = reconcileUserBalances(dryRun = true)
            if (report.mismatchCount > 0) {
                Log.w(
                    "SomadhanRepo",
                    "maybeAutoReconcileBalances: ${report.mismatchCount} balance mismatch(es) found across ${report.scannedUsersCount} users, total abs diff ৳${report.totalAbsoluteDifference} -- needs admin review (Admin Panel -> ব্যালেন্স রিকনসিলিয়েশন)"
                )
                // NotificationEntity has no broadcast/all-admins target -- userId is always a
                // single specific user -- so send one to each actual admin individually rather
                // than a made-up sentinel id that no real device would ever match.
                val admins = userDao.getUsersByRole("ADMIN")
                for ((index, admin) in admins.withIndex()) {
                    val notif = NotificationEntity(
                        role = "",
                        id = "NOTIF_RECONCILE_${now}_$index",
                        userId = admin.id,
                        title = "ব্যালেন্স গরমিল সনাক্ত হয়েছে ⚠️",
                        message = "স্বয়ংক্রিয় অডিটে ${report.mismatchCount}টি user-এর ব্যালেন্সে গরমিল পাওয়া গেছে (মোট ৳${report.totalAbsoluteDifference.toInt()})। Admin Panel-এ গিয়ে বিস্তারিত দেখুন ও সংশোধন করুন।",
                        targetType = "balance_reconciliation",
                        targetId = ""
                    )
                    notificationDao.insertNotification(notif)

                    // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৬] caller এখানে সবসময় admin (উপরের নোট
                    // দেখুন), তাই create_notification RPC-এর admin-path (যেকোনো target-এ পাঠানো
                    // অনুমোদিত) দিয়ে প্রতি admin-কে আলাদাভাবে notify করা হলো -- local loop-এর সাথে
                    // হুবহু মিলিয়ে (targetType="balance_reconciliation" স্থানীয় custom টাইপ, RPC
                    // সেটা যেমন আছে তেমনই insert করে, নিজে ব্যাখ্যা করে না)।
                    if (SupabaseAuthManager.currentUserId() != null) {
                        SupabaseSyncManager.createNotification(
                            targetUserId = admin.id,
                            title = notif.title,
                            message = notif.message,
                            targetType = notif.targetType,
                            targetId = notif.targetId?.ifBlank { null },
                            role = notif.role
                        ).onFailure { e ->
                            Log.w("SomadhanRepo", "maybeAutoReconcileBalances: admin notification dual-write failed for ${admin.id} (local flow unaffected): ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("SomadhanRepo", "maybeAutoReconcileBalances: skipped due to error: ${e.message}")
        }
    }

    suspend fun cleanupDuplicateRefunds(): Int {
        var removedCount = 0
        try {
            val allTransactions = transactionDao.getAllTransactionsList()
            val refundTransactions = allTransactions.filter { trx ->
                trx.type.equals("REFUND", ignoreCase = true) ||
                trx.id.startsWith("TRX_REFUND", ignoreCase = true) ||
                trx.id.contains("REFUND", ignoreCase = true)
            }

            val groupedByEscrow = refundTransactions.groupBy { it.escrowId }.filter { it.key.isNotBlank() }
            val now = System.currentTimeMillis()

            for ((escrowId, trxList) in groupedByEscrow) {
                if (trxList.size > 1) {
                    // Sort so canonical or earliest transaction comes first
                    val sorted = trxList.sortedWith(
                        compareBy<TransactionEntity> { if (it.id == "TRX_REFUND_${escrowId}" || it.id == "TRX_REFUND_${it.problemId}") 0 else 1 }
                            .thenBy { it.timestamp }
                    )
                    val primaryTrx = sorted.first()
                    val duplicates = sorted.drop(1)

                    for (dup in duplicates) {
                        if (dup.escrowId.startsWith("ESC_")) {
                            Log.w(
                                "SomadhanRepo",
                                "cleanupDuplicateRefunds: skipping potential false-positive (fallback escrowId) — trxId=${dup.id}, escrowId=${dup.escrowId}. Needs manual review."
                            )
                            continue
                        }
                        // 1. Delete from local Room
                        transactionDao.deleteTransaction(dup.id)

                        // আগে এখানে "2. Delete from Firestore" নামে একটা raw
                        // db?.collection("transactions")?.document(dup.id)?.delete()?.await()
                        // কল ছিল -- dead leftover, db এখন AppDatabase/Room, Firestore SDK
                        // নেই (ধাপ ৩৩.৩-এ dependency সরানো হয়েছে), তাই এটা compile হতোই না।
                        // নিচের ফাংশন-শেষের SupabaseSyncManager.adminCleanupDuplicateRefunds()
                        // dual-write ইতিমধ্যে cloud-side cleanup কাভার করে বলে এই ব্লকটা সম্পূর্ণ
                        // সরিয়ে ফেলা হলো (ধাপ ৩৩.৫ ফাইনাল-গ্রেপ চলাকালীন আবিষ্কৃত ও ফিক্স করা হয়েছে)।

                        // 2. Deduct the duplicate extra refund from user's balance. INSTANT
                        // local debit; the cloud side goes through the admin correction RPC's
                        // dual-write + outbox retry, the same pattern as every other balance
                        // change in this file [Step 7.7 cleanup: the dedicated correction
                        // TransactionEntity's pendingCloudSync flag below is an unread
                        // Firebase-era leftover, not the real retry mechanism], instead of the
                        // old incrementUserBalance() fire-and-forget (no retry if that single attempt
                        // failed, which would have left this repair tool's own correction only
                        // half-applied: local balance reduced but cloud balance still wrong).
                        // Uses a NEW id (not dup.id, which was just deleted above) so it doesn't
                        // collide with the delete; deterministic and keyed on dup.id so re-running
                        // cleanupDuplicateRefunds() can't apply the same correction twice even if
                        // dup somehow reappears (e.g. a stale offline write racing this cleanup).
                        // Deliberately avoids the words "রিফান্ড"/"বাতিল" and the substring
                        // "REFUND" anywhere in the id/title/type -- TransactionHelper.isRefundTrx()
                        // treats any of those as a refund and would render this NEGATIVE
                        // correction with a false "+" and green color (the exact display bug
                        // already found and fixed for other deduction transactions this session).
                        if (dup.netAmount > 0.0 && dup.userId.isNotBlank()) {
                            try {
                                // [ব্যালেন্স ফিক্স] refund সবসময় user pool থেকেই যায়/আসে (এই পুরো
                                // সেশনের established convention — escrow refund সবসময় পোস্টকারী
                                // user-কে যায়), তাই ডুপ্লিকেট সংশোধনও balanceUser mirror-এই হবে।
                                userDao.deductBalanceForUserRole(dup.userId, dup.netAmount, now)
                                val correctionTrx = TransactionEntity(
                                    id = "TRX_DUP_CORRECTION_${dup.id}",
                                    problemId = dup.problemId,
                                    problemTitle = "ডুপ্লিকেট লেনদেন সমন্বয়",
                                    userId = dup.userId,
                                    solverId = "",
                                    grossAmount = dup.netAmount,
                                    commissionPercent = 0.0,
                                    commissionAmount = 0.0,
                                    netAmount = -dup.netAmount,
                                    type = "DUPLICATE_CORRECTION",
                                    pendingCloudSync = true,
                                    role = "USER"
                                )
                                transactionDao.insertTransaction(correctionTrx)
                            } catch (e: Exception) {
                                Log.w("SomadhanRepo", "Failed to adjust user balance for duplicate refund: ${e.message}")
                            }
                        }

                        removedCount++
                    }

                    logAdminAction(
                        actionType = "CLEANUP_DUPLICATE_REFUNDS",
                        targetId = primaryTrx.problemId,
                        targetName = primaryTrx.problemTitle,
                        details = "${duplicates.size}টি ডুপ্লিকেট রিফান্ড ট্রানজেকশন মুছে ফেলা হয়েছে এবং ব্যালেন্স অ্যাডজাস্ট করা হয়েছে।"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("SomadhanRepo", "cleanupDuplicateRefunds failed: ${e.message}")
        }

        // [SUPABASE-MIGRATED - ধাপ ৩২.৬] best-effort dual-write, সবসময় live-run (এই Kotlin
        // ফাংশনে কোনো dryRun প্যারামিটার নেই)। guard শুধু session আছে কিনা।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminCleanupDuplicateRefunds(dryRun = false)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "cleanupDuplicateRefunds: Supabase dual-write failed (local flow unaffected): ${e.message}")
                    // [OUTBOX WIRE - Step 12.5] paramsJson key "dryRun" -- OutboxRpcDispatcher.kt-এর
                    // "admin_cleanup_duplicate_refunds" branch-এর সাথে অক্ষরে অক্ষরে মিলছে। idempotency
                    // (migration step32_6_admin_cleanup_duplicate_refunds.sql বডি থেকে যাচাই): কোয়েরি
                    // নিজেই শুধু `having count(*) > 1` গ্রুপ খোঁজে -- ১ম রান সফল হলে ডুপ্লিকেট row মুছে
                    // যায়, ২য়বার আর গ্রুপ পাওয়া যাবে না; correction transaction-এর id deterministic
                    // (`TRX_DUP_CORRECTION_<dup.id>`) + `on conflict (id) do nothing`। IDEMPOTENT।
                    enqueueOutboxRetry(
                        rpcName = "admin_cleanup_duplicate_refunds",
                        params = kotlinx.serialization.json.JsonObject(
                            buildMap {
                                put("dryRun", kotlinx.serialization.json.JsonPrimitive(false))
                            }
                        ),
                        error = e
                    )
                }
        }
        return removedCount
    }

    /**
     * repairMissingRefunds: One-time manual maintenance function for Admin panel.
     * Finds all EscrowEntity records where status == "REFUNDED" / "REFUND_PENDING_SYNC" but no matching
     * TransactionEntity with type == "REFUND" exists.
     *
     * In dryRun = true mode (default): Audits and logs missing refunds without modifying data.
     * In dryRun = false mode: Atomically credits the user's wallet, generates deterministic TransactionEntity,
     * updates Firestore and Room, and logs the admin action.
     */
    suspend fun repairMissingRefunds(dryRun: Boolean = true): MissingRefundRepairReport {
        val reportItems = mutableListOf<MissingRefundRepairItem>()
        var scannedCount = 0
        var missingCount = 0
        var repairedCount = 0
        var totalAmountSum = 0.0

        try {
            val allEscrows: List<EscrowEntity> = escrowDao.getAllEscrowsSync()
            val allTransactions: List<TransactionEntity> = transactionDao.getAllTransactionsList()
            scannedCount = allEscrows.size

            // 1. Target escrows marked as REFUNDED or REFUND_PENDING_SYNC
            val refundedEscrows = allEscrows.filter { escrow ->
                escrow.status.equals("REFUNDED", ignoreCase = true) ||
                escrow.status.equals("REFUND_PENDING_SYNC", ignoreCase = true)
            }

            // 2. Map of existing refund transactions by escrowId
            val existingRefundsByEscrow = allTransactions
                .filter { trx -> trx.type.equals("REFUND", ignoreCase = true) || trx.id.startsWith("TRX_REFUND", ignoreCase = true) }
                .filter { trx -> trx.escrowId.isNotBlank() }
                .groupBy { trx -> trx.escrowId }

            val now = System.currentTimeMillis()

            for (escrow in refundedEscrows) {
                val totalAmount = escrow.baseAmount + escrow.extraAmount
                if (totalAmount <= 0.0 || escrow.problemId.isBlank() || escrow.userId.isBlank()) {
                    continue
                }

                // Check local Room refund transaction
                val localRefunds = existingRefundsByEscrow[escrow.id] ?: emptyList()
                if (localRefunds.isNotEmpty()) {
                    continue
                }

                // [SUPABASE-MIGRATED - ধাপ ৩৩.৩] আগে এখানে Cloud Firestore-এ refundDocId খুঁজে
                // ১০০% idempotent থাকার একটা client-side check ছিল (`firestoreDb.collection(...)`) --
                // Firebase সম্পূর্ণ অপসারণের অংশ হিসেবে সরানো হলো। নোট: এই ব্লকটা আসলে
                // `firestoreDb` নামের একটা identifier ব্যবহার করছিল যা প্রজেক্টের কোথাও ঘোষিতই
                // ছিল না (grep দিয়ে নিশ্চিত করা হয়েছে) -- অর্থাৎ এটা ইতিমধ্যেই একটা অসলভড-রেফারেন্স
                // কম্পাইল-এরর ছিল, বাস্তবে চলছিল না। idempotency এখন সম্পূর্ণভাবে (ক) নিচের local
                // Room `localRefunds` চেক আর (খ) Supabase-সাইড `admin_repair_missing_refunds` RPC
                // (SECURITY DEFINER, নিজের ভেতরেই duplicate-scan করে, ধাপ ৩২.৬-এ verified) দিয়ে হয়।
                val refundDocId = "TRX_REFUND_${escrow.id}"

                // Verify the problem is actually cancelled / disputed in user's favor, NOT completed & released
                val problem = problemDao.getProblemById(escrow.problemId)
                val isValidRefundScenario = problem == null ||
                    problem.status in listOf("CANCELLED", "USER_CANCELLED", "SOLVER_CANCELLED", "CANCELLED_AUTO_REFUNDED", "EXPIRED", "OPEN", "PENDING_ACCEPTANCE", "DISPUTED") ||
                    (problem.disputeResolutionDecision in listOf("REFUND_USER", "SPLIT_PAYOUT")) ||
                    (problem.disputeResolutionType in listOf("REFUND_USER", "SPLIT_PAYOUT"))

                if (!isValidRefundScenario && problem?.status == "COMPLETED" && escrow.releasedAt == null) {
                    // Was completed and solver released, not a refund scenario
                    continue
                }

                missingCount++
                totalAmountSum += totalAmount

                val problemTitleString: String = problem?.title ?: "সমস্যা #${escrow.problemId.take(8)}"

                if (dryRun) {
                    Log.i("SomadhanRepo", "repairMissingRefunds [DRY RUN]: Missing refund detected for problemId=${escrow.problemId}, userId=${escrow.userId}, amount=$totalAmount")
                    reportItems.add(
                        MissingRefundRepairItem(
                            problemId = escrow.problemId,
                            problemTitle = problemTitleString,
                            userId = escrow.userId,
                            solverId = escrow.solverId,
                            escrowId = escrow.id,
                            baseAmount = escrow.baseAmount,
                            extraAmount = escrow.extraAmount,
                            totalAmount = totalAmount,
                            status = "DRY_RUN_ELIGIBLE",
                            message = "রিফান্ড ট্রানজেকশন মিসিং। ড্রাইভ-রান অডিটে শনাক্ত হয়েছে (৳${totalAmount.toInt()})"
                        )
                    )
                } else {
                    // LIVE EXECUTION: Atomic repair
                    Log.w("SomadhanRepo", "repairMissingRefunds [LIVE]: Repairing missing refund for problemId=${escrow.problemId}, userId=${escrow.userId}, amount=$totalAmount")
                    var repairSuccess = false

                    val probForRepair = problemDao.getProblemById(escrow.problemId)
                    val repairRefType = when {
                        probForRepair?.disputeResolutionDecision == "SPLIT_SETTLEMENT" || probForRepair?.disputeResolutionDecision == "CUSTOM_SPLIT" -> "SPLIT_REFUND"
                        probForRepair?.disputeResolutionDecision == "REFUND_TO_USER" -> "DISPUTE_REFUND"
                        else -> "SOLVER_CANCEL"
                    }
                    val repairRefPct = if (repairRefType == "SPLIT_REFUND") {
                        (100.0 - (probForRepair?.disputeSplitSolverPercent ?: 50.0)).coerceIn(0.0, 100.0)
                    } else 100.0

                    // [SUPABASE-MIGRATED - ধাপ ৩৩.৩] আগে এখানে "১. Try atomic Firestore transaction"
                    // (ব্যালেন্স ক্রেডিট + escrow status আপডেট + refund transaction write, সব একই
                    // Firestore transaction-এ) ব্লকটা ছিল -- Firebase সম্পূর্ণ অপসারণের অংশ হিসেবে
                    // সরানো হলো। `repairSuccess` এখন সবসময় `false`-ই থাকবে (নিচে declared), তাই
                    // নিচের local TransactionEntity-তে `pendingCloudSync = true`/`cloudBalanceSynced
                    // = false` বসবে -- এটা সঠিক আচরণ, কারণ আসল cloud-side repair এখন এই ফাংশনের
                    // শেষে থাকা `SupabaseSyncManager.adminRepairMissingRefunds()` ব্যাচ RPC কল দিয়ে
                    // অ্যাসিনক্রোনাসভাবে হয় (per-escrow synchronous না) -- local Room ক্রেডিট এখনো
                    // সাথে সাথেই হয় (optimistic UI), Supabase RPC ব্যর্থ হলেও local flow অপ্রভাবিত।

                    // 2. Credit local Room balance and insert TransactionEntity
                    try {
                        // [ব্যালেন্স ফিক্স] escrow refund → user pool
                        userDao.addBalanceForUserRole(escrow.userId, totalAmount, now)
                        val refundTrx = TransactionEntity(
                            id = refundDocId,
                            problemId = escrow.problemId,
                            problemTitle = problemTitleString,
                            userId = escrow.userId,
                            solverId = escrow.solverId,
                            grossAmount = totalAmount,
                            commissionPercent = 0.0,
                            commissionAmount = 0.0,
                            netAmount = totalAmount,
                            timestamp = now,
                            baseAmount = escrow.baseAmount,
                            extraAmount = escrow.extraAmount,
                            baseCommissionAmount = 0.0,
                            extraCommissionAmount = 0.0,
                            extraCommissionApplied = false,
                            wasFreeQuotaJob = false,
                            type = "REFUND",
                            escrowId = escrow.id,
                            pendingCloudSync = !repairSuccess,
                            cloudBalanceSynced = repairSuccess,
                            refundType = repairRefType,
                            refundPercentage = repairRefPct,
                            role = "USER"
                        )
                        transactionDao.insertTransaction(refundTrx)

                        val updatedEscrow = escrow.copy(
                            status = "REFUNDED",
                            releasedAt = escrow.releasedAt ?: now
                        )
                        escrowDao.insert(updatedEscrow)


                        repairedCount++
                        reportItems.add(
                            MissingRefundRepairItem(
                                problemId = escrow.problemId,
                                problemTitle = problemTitleString,
                                userId = escrow.userId,
                                solverId = escrow.solverId,
                                escrowId = escrow.id,
                                baseAmount = escrow.baseAmount,
                                extraAmount = escrow.extraAmount,
                                totalAmount = totalAmount,
                                status = "REPAIRED",
                                message = "সফলভাবে ওয়ালেটে ৳${totalAmount.toInt()} ক্রেডিট ও ট্রানজেকশন রেকর্ড তৈরি করা হয়েছে"
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("SomadhanRepo", "repairMissingRefunds: Local repair failed: ${e.message}", e)
                        reportItems.add(
                            MissingRefundRepairItem(
                                problemId = escrow.problemId,
                                problemTitle = problemTitleString,
                                userId = escrow.userId,
                                solverId = escrow.solverId,
                                escrowId = escrow.id,
                                baseAmount = escrow.baseAmount,
                                extraAmount = escrow.extraAmount,
                                totalAmount = totalAmount,
                                status = "FAILED",
                                message = "রিপেয়ার ব্যর্থ: ${e.message}"
                            )
                        )
                    }
                }
            }

            if (!dryRun && repairedCount > 0) {
                logAdminAction(
                    actionType = "REPAIR_MISSING_REFUNDS",
                    targetId = "ESCROW_REPAIR",
                    targetName = "Missing Escrow Refunds Recovery",
                    details = "সফলভাবে $repairedCount টি মিসিং রিফান্ড পুনরুদ্ধার করা হয়েছে (মোট ৳ ${totalAmountSum.toInt()})"
                )
            }
        } catch (e: Exception) {
            Log.e("SomadhanRepo", "repairMissingRefunds failed: ${e.message}", e)
        }

        // [SUPABASE-MIGRATED - ধাপ ৩২.৬] best-effort dual-write, dryRun প্যারামিটার হুবহু পাস করা
        // হয়। guard শুধু session আছে কিনা।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminRepairMissingRefunds(dryRun = dryRun)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "repairMissingRefunds: Supabase dual-write failed (local flow unaffected): ${e.message}")
                    // [OUTBOX WIRE - Step 12.5] paramsJson key "dryRun" -- OutboxRpcDispatcher.kt-এর
                    // "admin_repair_missing_refunds" branch-এর সাথে অক্ষরে অক্ষরে মিলছে। idempotency
                    // (migration step32_6_admin_repair_missing_refunds.sql বডি থেকে যাচাই): deterministic
                    // id (`TRX_REFUND_<escrow_id>`) দিয়ে balance UPDATE-এর *আগেই* exists-চেক করে `continue`
                    // করে -- ১ম রান সফল হলে ২য়বার ওই escrow-এর জন্য balance আর ছোঁয়া হয় না। IDEMPOTENT।
                    enqueueOutboxRetry(
                        rpcName = "admin_repair_missing_refunds",
                        params = kotlinx.serialization.json.JsonObject(
                            buildMap {
                                put("dryRun", kotlinx.serialization.json.JsonPrimitive(dryRun))
                            }
                        ),
                        error = e
                    )
                }
        }

        return MissingRefundRepairReport(
            dryRun = dryRun,
            scannedEscrowsCount = scannedCount,
            missingRefundCount = missingCount,
            repairedCount = repairedCount,
            totalAmountRepairedOrAudited = totalAmountSum,
            items = reportItems
        )
    }

    /**
     * Safe recovery for the "orphaned accepted bid" dead-end (case 3 in the bug report): a race
     * between a stale local bid list and a fast-moving urgent/joruri post could previously result
     * in acceptedSolverId pointing at a bid the owner never actually tapped "accept" on, with no
     * escrow ever locked and no tracking button appearing for either side -- leaving the owner
     * with only a "Dispute" button, even though there was never any money to dispute over.
     *
     * The screen-scoped live bid listener added for case 3 makes the underlying race far less
     * likely going forward, but this exists as a narrow, safe way out if it ever still happens:
     * it only allows a reset when there is provably no money at stake (no escrow row, or an
     * escrow row with nothing actually locked in it) and the post isn't already disputed/settled.
     * If real funds are locked, this refuses and tells the caller to use the normal dispute flow
     * instead -- that path stays admin-supervised on purpose.
     */
    suspend fun ownerResetOrphanedAcceptedBid(problemId: String, ownerId: String): Boolean {
        val problem = problemDao.getProblemById(problemId) ?: return false
        if (problem.userId != ownerId) return false
        if (problem.acceptedSolverId == null) return false
        if (problem.isDisputed || problem.status == "COMPLETED" || problem.status == "CANCELLED") return false

        val escrow = escrowDao.getByProblemId(problemId)
        val lockedAmount = if (escrow != null && escrow.status !in setOf("RELEASED", "REFUNDED")) {
            (escrow.baseAmount) + (escrow.extraAmount)
        } else 0.0
        if (lockedAmount > 0.0) {
            // There IS real money locked -- this is a genuinely accepted, funded job, not an
            // orphaned/stuck state. Refuse and let the normal dispute flow (which is
            // admin-supervised for exactly this reason) handle it.
            return false
        }

        val now = System.currentTimeMillis()
        val updatedProblem = problem.copy(
            status = "OPEN",
            jobStatus = if (problem.isInstantJob) "BROADCASTING" else problem.jobStatus,
            broadcastTimerStartedAt = if (problem.isInstantJob) now else problem.broadcastTimerStartedAt,
            acceptedBidId = null,
            acceptedSolverId = null,
            acceptedSolverName = null,
            acceptedAmount = null,
            solverLiveLat = null,
            solverLiveLng = null,
            solverLiveUpdatedAt = null,
            arrivedAt = null,
            jobStartedAt = null,
            hasReleaseRequest = false,
            releaseRequestExtraAmount = 0.0,
            releaseRequestNote = "",
            releaseRequestedAt = null,
            solverCancelledNotice = null,
            lastActivityAt = now
        )
        problemDao.updateProblem(updatedProblem)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮৫] Supabase-সাইড RPC নিজেই escrow-locked-amount গার্ড
        // আবার চেক করে (client guard-কে বিশ্বাস করে না, কারণ এটা টাকা-সংক্রান্ত একটা reset)।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.ownerResetOrphanedAcceptedBid(problemId).onFailure { e ->
                Log.w("SomadhanRepo", "ownerResetOrphanedAcceptedBid: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }
        return true
    }

    suspend fun solverCancelJob(problemId: String, solverId: String, reason: String, reopenAsOpen: Boolean = true) {
        val problem = problemDao.getProblemById(problemId) ?: return
        val now = System.currentTimeMillis()

        // Guard: same reasoning as cancelInstantJob() above — don't let a stale/duplicate cancel
        // action overwrite a job that has already reached a terminal state. refundEscrowOnce()'s
        // escrow-status guard already stops the money from moving twice, but without this check
        // the problem row itself (and its bids below) would still get incorrectly reset even
        // though the job already finished or was already cancelled.
        if (problem.status == "COMPLETED" || problem.status == "CANCELLED") {
            Log.w("SomadhanRepo", "solverCancelJob: skipped — problem $problemId is already ${problem.status}, refusing to cancel again")
            return
        }

        // Bug fix: সব caller (viewModel-এর তিনটা ভিন্ন solverCancelJob/solverCancelAcceptedJob
        // পাথ) আগে এই ফাংশনে সবসময় reopenAsOpen=true পাঠাত, ইনস্ট্যান্ট জব কিনা তা না দেখেই।
        // ইনস্ট্যান্ট জবের ক্ষেত্রে এর ফলাফল ছিল: সলভার বিড বাতিল করার সাথে সাথেই পোস্টটা আবার
        // status="OPEN" + jobStatus="BROADCASTING" হয়ে যেত, broadcastTimerStartedAt রিসেট হয়ে
        // নতুন করে সম্প্রচার শুরু হয়ে যেত -- ব্যবহারকারী নিজে থেকে কিছু না করা সত্ত্বেও, "পুনরায়
        // সম্প্রচার" স্বয়ংক্রিয়ভাবে ঘটে যাচ্ছিল। ফিক্স: ইনস্ট্যান্ট জবের ক্ষেত্রে (caller যাই পাঠাক
        // না কেন) reopenAsOpen জোর করে false ধরা হচ্ছে -- পোস্টটা CANCELLED অবস্থাতেই থাকবে, যতক্ষণ
        // না ব্যবহারকারী নিজে থেকে নতুন করে পোস্ট/সম্প্রচার শুরু করেন। সাধারণ (non-instant) জবের
        // পুরনো "reopen করে অন্য বিড বেছে নেওয়ার সুযোগ" আচরণ অপরিবর্তিত থাকছে।
        val effectiveReopenAsOpen = reopenAsOpen && !problem.isInstantJob

        // 1. Mark problem cancelled / reset to OPEN if requested
        val updatedProblem = if (effectiveReopenAsOpen) {
            problem.copy(
                status = "OPEN",
                jobStatus = if (problem.isInstantJob) "BROADCASTING" else problem.jobStatus,
                broadcastTimerStartedAt = if (problem.isInstantJob) System.currentTimeMillis() else problem.broadcastTimerStartedAt,
                acceptedBidId = null,
                acceptedSolverId = null,
                acceptedSolverName = null,
                acceptedAmount = null,
                solverLiveLat = null,
                solverLiveLng = null,
                solverLiveUpdatedAt = null,
                arrivedAt = null,
                jobStartedAt = null,
                hasReleaseRequest = false,
                releaseRequestExtraAmount = 0.0,
                releaseRequestNote = "",
                releaseRequestedAt = null,
                isDisputed = false,
                disputeReason = null,
                disputeInitiatorId = null,
                // Bug fix: previously left at its old value across a cancel+re-broadcast cycle
                // (hasReleaseRequest/releaseRequestExtraAmount right above were already being
                // reset, this one was missed). Doesn't lose any money -- the refund below reads
                // from the escrow row, not this field, and acceptBid()/acceptInstantJobBid()
                // reset it again on the NEXT accept regardless -- but it left stale "previous
                // solver's extra bill" data readable on the still-OPEN problem during the
                // re-broadcast window (e.g. the includeExtraAmount checks elsewhere that fall
                // back to this field).
                confirmedExtraAmountTotal = 0.0,
                solverCancelledNotice = "সমাধানকারী আপনার পোস্টটির বিড বাতিল করেছেন। আপনি পুনরায় বিড নির্বাচন করুন।",
                lastActivityAt = now
            )
        } else {
            problem.copy(
                status = "CANCELLED",
                jobStatus = if (problem.isInstantJob) "CANCELLED" else problem.jobStatus,
                hasReleaseRequest = false,
                releaseRequestExtraAmount = 0.0,
                releaseRequestNote = "",
                releaseRequestedAt = null,
                isDisputed = false,
                disputeReason = null,
                disputeInitiatorId = null,
                // Same reasoning as the reopenAsOpen branch above -- the problem is terminal
                // here too, so this should reflect no dangling "pending" charge state.
                confirmedExtraAmountTotal = 0.0,
                solverCancelledNotice = "সমাধানকারী আপনার পোস্টটির বিড বাতিল করেছেন।",
                lastActivityAt = now
            )
        }
        problemDao.updateProblem(updatedProblem)

        // 2. Refund Escrow to User's Wallet Balance (Strictly Idempotent via refundEscrowOnce)
        val escrow = escrowDao.getByProblemId(problemId)
        val refundAmount = (escrow?.baseAmount ?: (problem.acceptedAmount ?: 0.0)) + (escrow?.extraAmount ?: 0.0)
        if (refundAmount > 0.0) {
            refundEscrowOnce(
                problemId = problemId,
                amount = refundAmount,
                userId = problem.userId,
                solverId = solverId,
                problemTitle = problem.title,
                baseAmount = escrow?.baseAmount ?: (problem.acceptedAmount ?: 0.0),
                extraAmount = escrow?.extraAmount ?: 0.0,
                escrowId = escrow?.id
            )
        }

        // [SUPABASE-MIGRATED - ধাপ ৮] নিজের row হলে (caller-ই cancel করা solver) Supabase-এর
        // `solver_cancel_job` RPC-ও কল করা হয় (best-effort dual-write, acceptBid()-এর মতোই
        // প্যাটার্ন)। RPC নিজেই ভেতরে problem reset + refund_escrow_once() দুটোই একসাথে করে
        // (একই transaction-এ), তাই এখানে আলাদা করে refund RPC কল করার দরকার নেই। ব্যর্থ হলেও
        // উপরের local Room flow ইতিমধ্যে সম্পন্ন, এখানে ব্যর্থতা user-facing error দেখাবে না।
        if (SupabaseAuthManager.currentUserId() == solverId) {
            SupabaseSyncManager.solverCancelJob(problemId, reason, effectiveReopenAsOpen)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK") {
                        Log.w("SomadhanRepo", "solverCancelJob: Supabase dual-write non-OK result for problem $problemId: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "solverCancelJob: Supabase dual-write failed for problem $problemId (local flow unaffected): ${e.message}")
                    // [OUTBOX WIRE - Step 12.5] paramsJson key "problemId","reason": String,
                    // "reopenAsOpen": Boolean -- OutboxRpcDispatcher.kt-এর "solver_cancel_job" branch-এর
                    // সাথে অক্ষরে অক্ষরে মিলছে। idempotency (migration recovered_instant_jobs.sql বডি
                    // থেকে যাচাই): row-লক (`for update`) + `status in ('COMPLETED','CANCELLED')` হলে
                    // ALREADY_TERMINAL (early-return, no-op)। ⚠️ reopenAsOpen=true পাথে ২য়বার status
                    // এখনো 'OPEN' থাকে (COMPLETED/CANCELLED না), তাই ওই guard কেটে যায় না -- কিন্তু ভেতরের
                    // refund_escrow_once() নিজে `status='HELD'` escrow খোঁজে, ১ম রানে সফল রিফান্ডের পর
                    // escrow আর HELD থাকে না, তাই টাকা দ্বিতীয়বার নড়ে না (money-safe)। **residual ঝুঁকি
                    // (BLOCKED করা হয়নি, শুধু নথিভুক্ত, accept_bid-এর মতোই ক্লাস):** failure ও replay-এর
                    // মাঝে অন্য কোনো solver নতুন বিড accept করলে/problem-এর state বদলালে, replay আবার
                    // problem-এর accepted_bid_id/solver ফিল্ড null করে দিতে পারে -- stale replay, এই
                    // ধাপের স্কোপের বাইরে।
                    enqueueOutboxRetry(
                        rpcName = "solver_cancel_job",
                        params = kotlinx.serialization.json.JsonObject(
                            buildMap {
                                put("problemId", kotlinx.serialization.json.JsonPrimitive(problemId))
                                put("reason", kotlinx.serialization.json.JsonPrimitive(reason))
                                put("reopenAsOpen", kotlinx.serialization.json.JsonPrimitive(effectiveReopenAsOpen))
                            }
                        ),
                        error = e
                    )
                }
        }

        // 3. Mark the cancelling solver's accepted bid as CANCELLED, and restore all other valid bids to PENDING
        val bids = bidDao.getBidsForProblemSync(problemId)
        val acceptedBidId = problem.acceptedBidId
        val progressStep = problem.calculateProgressStep().coerceAtLeast(2)
        bids.forEach { bid ->
            if (bid.id == acceptedBidId || bid.solverId == solverId || bid.solverId == problem.acceptedSolverId) {
                val updatedBid = bid.copy(status = "CANCELLED", progressAtCancel = progressStep, resolutionType = "SOLVER_CANCEL", resolvedAt = System.currentTimeMillis())
                bidDao.insertBid(updatedBid)
            } else if (bid.status != "CANCELLED" && bid.status != "WITHDRAWN" && bid.status != "REJECTED") {
                val restoredBid = bid.copy(status = "PENDING")
                bidDao.insertBid(restoredBid)
            }
        }

        // 4. Apply penalty to solver
        applyCancellationPenalty(solverId, problem.id)
        applyReputationChange(solverId, "JOB_CANCELLED_BY_SOLVER", -3.0, problemId, "কাজ অ্যাকসেপ্ট করার পর বাতিল করেছেন")
        triggerDynamicReputationEvent(
            userId = solverId,
            eventType = "FREQUENT_BID_WITHDRAWAL",
            problemId = problem.id,
            defaultScore = 2.0,
            defaultCap = 4.0,
            defaultPenalty = 2.0,
            isPositive = false,
            customNote = "বিড গ্রহণ করার পর কাজ বাতিল করার পেনাল্টি"
        )

        // 5. Notify User
        val notifMessage = if (effectiveReopenAsOpen) {
            "সমাধানকারী আপনার পোস্টটির বিড বাতিল করেছেন। এসক্রোর ৳${DistanceUtil.toBengaliDigits(refundAmount.toInt().toString())} আপনার ওয়ালেট ব্যালেন্সে রিফান্ড করা হয়েছে এবং পোস্টটি পুনরায় উন্মুক্ত (OPEN) করা হয়েছে। আপনি পুনরায় বিড নির্বাচন করতে পারবেন।"
        } else {
            "সমাধানকারী আপনার পোস্টটির বিড বাতিল করেছেন। এসক্রোর ৳${DistanceUtil.toBengaliDigits(refundAmount.toInt().toString())} আপনার ওয়ালেট ব্যালেন্সে রিফান্ড করা হয়েছে।"
        }

        val notif = NotificationEntity(
            role = "USER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = problem.userId,
            title = "সমাধানকারী আপনার পোস্টের বিড বাতিল করেছেন ⚠️",
            message = notifMessage,
            targetType = "tracking",
            targetId = problem.id,
            relatedProblemId = problem.id
        )
        notificationDao.insertNotification(notif)

        // [SUPABASE-MIGRATED - ধাপ ১২ ফিক্স] notification-gap বন্ধ: `solver_cancel_job` RPC ভেতরে
        // `refund_escrow_once()` কল করে যেটা একটা জেনেরিক "রিফান্ড সম্পন্ন" balance-notification
        // পাঠায়, কিন্তু উপরের "বিড বাতিল" notification-টা (আলাদা বিষয়বস্তু) কখনো পাঠায় না (ধাপ ১২
        // audit এ নিশ্চিত হওয়া গ্যাপ) -- তাই `create_notification` RPC দিয়ে ঠিক এই একই title/message
        // owner-কে আলাদাভাবে পাঠানো হচ্ছে। caller (solverId) ও target (problem.userId, owner) দুজনেই
        // এই problem-এর party, তাই RPC-এর party-check pass করার কথা। Best-effort।
        if (SupabaseAuthManager.currentUserId() == solverId) {
            SupabaseSyncManager.createNotification(
                targetUserId = problem.userId,
                title = notif.title,
                message = notif.message,
                targetType = "tracking",
                targetId = problem.id,
                relatedProblemId = problem.id,
                role = notif.role
            ).onFailure { e ->
                Log.w("SomadhanRepo", "solverCancelJob: createNotification dual-write failed for problem $problemId (local flow unaffected): ${e.message}")
            }
        }

        // 6. Send message in chat
        val chatContent = if (effectiveReopenAsOpen) {
            "❌ সমাধানকারী আপনার পোস্টটির বিড বাতিল করেছেন। এসক্রোর অর্থ ক্লায়েন্টের ওয়ালেটে রিফান্ড করা হয়েছে এবং পোস্টটি পুনরায় বিড নির্বাচনের জন্য উন্মুক্ত (OPEN) হয়েছে।"
        } else {
            "❌ সমাধানকারী কাজটির কার্যক্রম বাতিল করেছেন। এসক্রোর অর্থ গ্রাহকের ওয়ালেটে রিফান্ড করা হয়েছে।"
        }

        sendMessage(
            problemId = problemId,
            senderId = solverId.ifBlank { "SOLVER" },
            receiverId = problem.userId,
            senderName = problem.acceptedSolverName ?: "সমাধানকারী",
            content = chatContent
        )

        logAdminAction(
            actionType = "SOLVER_CANCEL_JOB",
            targetId = problemId,
            targetName = problem.title,
            details = "সমাধানকারী কাজ বাতিল করেছেন (কারণ: $reason)" + if (effectiveReopenAsOpen) ", পোস্ট পুনরায় OPEN" else ", পোস্ট CANCELLED অবস্থায় রইলো"
        )
    }

    suspend fun solverCancelAcceptedJob(
        problemId: String,
        solverId: String,
        reason: String = "সমাধানকারী কর্তৃক বাতিল",
        reopenAsOpen: Boolean = true
    ) {
        solverCancelJob(problemId, solverId, reason, reopenAsOpen)
    }

    suspend fun clearSolverCancelledNotice(problemId: String) {
        val problem = problemDao.getProblemById(problemId) ?: return
        val now = System.currentTimeMillis()
        val prevAcceptedSolverId = problem.acceptedSolverId
        val prevAcceptedBidId = problem.acceptedBidId
        val updated = problem.copy(
            solverCancelledNotice = null,
            acceptedBidId = null,
            acceptedSolverId = null,
            acceptedSolverName = null,
            acceptedAmount = null,
            broadcastTimerStartedAt = now,
            lastActivityAt = now
        )
        problemDao.updateProblem(updated)

        // Ensure any cancelled bids or bids belonging to previous accepted solvers or any solver who cancelled are strictly CANCELLED
        val bids = bidDao.getBidsForProblemSync(problemId)
        val cancelledSolverIds = bids
            .filter { it.status == "CANCELLED" || it.status == "WITHDRAWN" || it.status == "REJECTED" }
            .map { it.solverId }
            .toMutableSet()
        if (!prevAcceptedSolverId.isNullOrBlank()) {
            cancelledSolverIds.add(prevAcceptedSolverId)
        }

        bids.forEach { bid ->
            if (bid.status == "ACCEPTED" ||
                bid.status == "CANCELLED" ||
                bid.status == "WITHDRAWN" ||
                bid.status == "REJECTED" ||
                bid.solverId in cancelledSolverIds ||
                bid.id == prevAcceptedBidId
            ) {
                val updatedBid = bid.copy(status = "CANCELLED")
                bidDao.insertBid(updatedBid)
            }
        }

        // [SUPABASE-MIGRATED - ধাপ ৩২.৯৫] `clear_solver_cancelled_notice` RPC সোর্স পড়ে যাচাই
        // করা হয়েছে — শুধু problem owner (auth.uid() == problem.userId) কল করতে পারে, RPC
        // নিজেই problem reset + প্রাসঙ্গিক সব বিড CANCELLED করে দেয় একই transaction-এ। best-effort
        // — ব্যর্থ হলেও উপরের local Room flow অপ্রভাবিত থাকে।
        if (SupabaseAuthManager.currentUserId() == problem.userId) {
            SupabaseSyncManager.clearSolverCancelledNotice(problemId).onFailure { e ->
                Log.w("SomadhanRepo", "clearSolverCancelledNotice: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }
    }

    suspend fun userDeleteProblem(problemId: String, userId: String) {
        val problem = problemDao.getProblemById(problemId)
            ?: throw IllegalStateException("সমস্যাটি পাওয়া যায়নি।")

        if (problem.userId != userId) {
            throw IllegalStateException("আপনি শুধুমাত্র আপনার নিজের পোস্ট ডিলিট করতে পারবেন।")
        }

        if (problem.status != "OPEN" || problem.acceptedBidId != null || !problem.acceptedSolverId.isNullOrBlank()) {
            throw IllegalStateException("চলমান বা বিড গৃহীত হওয়া কাজ ডিলিট করা সম্ভব নয়।")
        }

        val now = System.currentTimeMillis()
        val updatedProblem = problem.copy(
            isUserDeleted = true,
            status = "CANCELLED",
            jobStatus = if (problem.isInstantJob) "CANCELLED" else problem.jobStatus,
            solverCancelledNotice = null,
            lastActivityAt = now
        )
        problemDao.updateProblem(updatedProblem)

        // Mark all bids for this problem as CANCELLED
        val bids = bidDao.getBidsForProblem(problemId).firstOrNull() ?: emptyList()
        val progressStep = problem.calculateProgressStep()
        bids.forEach { bid ->
            val updatedBid = bid.copy(status = "CANCELLED", progressAtCancel = progressStep)
            bidDao.insertBid(updatedBid)
        }

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮৫] একটা RPC-ই problem soft-delete + সব bid CANCELLED
        // একসাথে করে (bids টেবিলে owner-scoped UPDATE policy নেই বলে RPC-ভিত্তিক, established
        // প্যাটার্ন)। নোট: RPC bid-এর progress_at_cancel সেট করে না (display-only, নথিভুক্ত গ্যাপ)।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.userDeleteProblem(problemId).onFailure { e ->
                Log.w("SomadhanRepo", "userDeleteProblem: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }

        logAdminAction(
            actionType = "USER_DELETE_PROBLEM",
            targetId = problemId,
            targetName = problem.title,
            details = "ইউজার $userId তার ওপেন পোস্টটি ডিলিট করেছেন।"
        )
    }

    suspend fun checkAndProcess48HourAutoReleases() {
        val now = System.currentTimeMillis()
        val allProbs = problemDao.getAllProblems().firstOrNull() ?: emptyList()
        val pendingAutoReleases = allProbs.filter { prob ->
            (prob.status == "IN_PROGRESS" || prob.status == "OPEN") &&
            !prob.acceptedSolverId.isNullOrBlank() &&
            prob.hasReleaseRequest &&
            prob.releaseRequestedAt != null &&
            !prob.isDisputed &&
            (now - (prob.releaseRequestedAt ?: 0L)) >= (48L * 60 * 60 * 1000L) // 48 Hours
        }

        for (prob in pendingAutoReleases) {
            try {
                // Auto-confirm release to solver
                confirmReleaseAndComplete(prob, includeExtraAmount = (prob.releaseRequestExtraAmount > 0.0))

                // Deduct reputation points from the user for negligence in responding within 48 hours
                val timeoutPenalty = platformSettingDao.getSetting("rep_penalty_release_timeout")?.toDoubleOrNull() ?: 10.0
                applyReputationChange(
                    userId = prob.userId,
                    eventType = "USER_RELEASE_TIMEOUT_PENALTY",
                    scoreChange = -timeoutPenalty,
                    problemId = prob.id,
                    note = "৪৮ ঘণ্টার মধ্যে রিলিজ রিকোয়েস্টে সাড়া না দেওয়ায় অবহেলার কারণে পেনাল্টি"
                )

                // Notify User
                val userNotif = NotificationEntity(
                    role = "USER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = prob.userId,
                    title = "⏰ স্বয়ংক্রিয় রিলিজ ও রেপুটেশন পেনাল্টি (-১০)",
                    message = "\"${prob.title}\" কাজে সমাধানকারীর রিলিজ অনুরোধের পর ৪৮ ঘণ্টার মধ্যে কোনো সাড়া না দেওয়ায় অর্থ স্বয়ংক্রিয়ভাবে রিলিজ হয়েছে এবং অবহেলার কারণে আপনার রেপুটেশন থেকে ১০ পয়েন্ট কাটা হয়েছে।",
                    targetType = "problem",
                    targetId = prob.id,
                    relatedProblemId = prob.id
                )
                notificationDao.insertNotification(userNotif)

                // Notify Solver
                val solverId = prob.acceptedSolverId!!
                val solverNotif = NotificationEntity(
                    role = "SOLVER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = solverId,
                    title = "⏰ ৪৮ ঘণ্টা পর স্বয়ংক্রিয় পেমেন্ট রিলিজ সম্পন্ন! 🎉",
                    message = "\"${prob.title}\" কাজে গ্রাহক ৪৮ ঘণ্টা সাড়া না দেওয়ায় অর্থ স্বয়ংক্রিয়ভাবে আপনার ব্যালেন্সে জমা করা হয়েছে।",
                    targetType = "balance",
                    targetId = solverId,
                    relatedProblemId = prob.id
                )
                notificationDao.insertNotification(solverNotif)

                // [SUPABASE-MIGRATED - ধাপ ২৭] আগে (ধাপ ১২ ব্যাচ ৬) এখানে সাধারণ `create_notification`
                // RPC ব্যবহার হতো — কিন্তু সেই RPC caller-কে admin/self-notify/problem-party হতে
                // বাধ্য করে, আর এই ফাংশনটা app startup/pull-to-refresh-এ *যেকোনো* logged-in user
                // (admin না-ও হতে পারে, প্রায়ই এই নির্দিষ্ট problem-এর party-ও না) থেকে কল হয় বলে
                // dual-write বেশিরভাগ সময় ব্যর্থ হতো (নথিভুক্ত caller-scoping গ্যাপ, দেখুন
                // MIGRATION_PROGRESS.md-এর শুরুর "৪টা বাধ্যতামূলক আইটেম" তালিকার আইটেম #৪)। ধাপ
                // ২৭-এ caller-এর identity না দেখে, বরং টার্গেট problem-টা আসলেই ৪৮-ঘণ্টা-পার-হওয়া
                // auto-release শর্ত পূরণ করে কিনা তা সার্ভার-সাইডে যাচাই করা একটা নতুন, সংকীর্ণভাবে
                // scoped RPC (`system_notify_48hour_auto_release`) বানানো হয়েছে — এখন caller
                // যে-ই হোক (admin/party না হলেও), এই dual-write সফল হবে, কারণ RPC নিজেই
                // business-rule verify করে, caller-এর role/identity দিয়ে না। (solverNotif এই
                // release_escrow RPC-এর নিজস্ব (server-side) "পেমেন্ট প্রকাশিত হয়েছে" notification
                // থেকে আলাদা তথ্য বহন করে (৪৮-ঘণ্টা-timeout-নির্দিষ্ট বার্তা), তাই ডুপ্লিকেট নয় —
                // batch ৬-এর আবিষ্কার ১, যা confirmReleaseAndComplete()-এর নিজস্ব solverNotif-এ
                // প্রযোজ্য, এখানে প্রযোজ্য না)।
                if (SupabaseAuthManager.currentUserId() != null) {
                    SupabaseSyncManager.systemNotify48HourAutoRelease(
                        problemId = prob.id,
                        targetUserId = userNotif.userId,
                        title = userNotif.title,
                        message = userNotif.message,
                        targetType = userNotif.targetType,
                        targetId = userNotif.targetId?.ifBlank { null }
                    ).onFailure { e ->
                        Log.w("SomadhanRepo", "checkAndProcess48HourAutoReleases: userNotif dual-write failed for ${prob.id} (local flow unaffected): ${e.message}")
                    }
                    SupabaseSyncManager.systemNotify48HourAutoRelease(
                        problemId = prob.id,
                        targetUserId = solverNotif.userId,
                        title = solverNotif.title,
                        message = solverNotif.message,
                        targetType = solverNotif.targetType,
                        targetId = solverNotif.targetId?.ifBlank { null }
                    ).onFailure { e ->
                        Log.w("SomadhanRepo", "checkAndProcess48HourAutoReleases: solverNotif dual-write failed for ${prob.id} (local flow unaffected): ${e.message}")
                    }
                }

                logAdminAction(
                    actionType = "AUTO_RELEASE_48_HOURS",
                    targetId = prob.id,
                    targetName = prob.title,
                    details = "৪৮ ঘণ্টা সময় অতিক্রান্ত হওয়ায় স্বয়ংক্রিয় রিলিজ ও গ্রাহকের ১০ পয়েন্ট জরিমানা"
                )
            } catch (e: Exception) {
                Log.e("SomadhanRepo", "Auto-release failed for problem ${prob.id}: ${e.message}")
            }
        }
    }

    suspend fun confirmReleaseAndComplete(
        problem: ProblemEntity,
        includeExtraAmount: Boolean,
        stars: Int = 0,
        reviewComment: String = "",
        releaseType: String = "",
        walletDeduction: Double = 0.0
    ) {
        // Idempotency guard: re-fetch the problem's CURRENT status right before paying out.
        // confirmReleaseAndComplete() is the single choke point for every settlement path in the
        // app — the user-facing "confirm & complete" button (normal jobs and instant jobs),
        // admin dispute resolution's "RELEASE_TO_SOLVER" branch, the 48-hour auto-release
        // background sweep (checkAndProcess48HourAutoReleases), and
        // adminUpdateDirectContractStatus() — several of which read a possibly-stale
        // ProblemEntity snapshot before calling in. Without this guard, any two of those racing
        // on the same problem (a double-tap on "Confirm & Complete", the 48-hour sweep firing at
        // the same moment a user manually confirms, or an admin resolving a dispute right as the
        // sweep also picks the same problem up) would each independently credit netAmount to the
        // solver's balance and insert a fresh Transaction row — nothing here previously checked
        // whether this problem had already been paid out. This mirrors the same guard already
        // applied to acceptBid() and adminReleaseEscrow().
        val freshProblemForGuard = problemDao.getProblemById(problem.id)
        if (freshProblemForGuard != null && freshProblemForGuard.status == "COMPLETED") {
            Log.w("SomadhanRepo", "confirmReleaseAndComplete: skipped — problem ${problem.id} is already COMPLETED, refusing to release/pay out again")
            return
        }

        // Bug fix: also guard on the escrow's own status, not just problem.status. Some paths
        // (adminReleaseEscrow's manual admin release button in particular) pay the solver and
        // flip the escrow to RELEASED/REFUNDED WITHOUT ever setting problem.status to
        // "COMPLETED". If we only checked problem.status above, a subsequent normal-flow call
        // into this same choke point — the user's own "Confirm & Complete" tap, or the 48-hour
        // auto-release sweep, both of which still see the problem sitting in IN_PROGRESS/OPEN —
        // would sail past the problem-status guard and pay the same escrow out a second time.
        // Checking the escrow's CURRENT status here (re-fetched fresh, not the possibly-stale
        // one resolved further down) closes that gap regardless of which admin/automated path
        // already settled it.
        val freshEscrowForGuard = escrowDao.getByProblemId(problem.id)
        if (freshEscrowForGuard != null && (freshEscrowForGuard.status == "RELEASED" || freshEscrowForGuard.status == "REFUNDED")) {
            Log.w("SomadhanRepo", "confirmReleaseAndComplete: skipped — escrow ${freshEscrowForGuard.id} for problem ${problem.id} is already ${freshEscrowForGuard.status}, refusing to pay out again")
            return
        }

        // [Step 12.10d] RPC-first: সার্ভার স্থায়ীভাবে প্রত্যাখ্যান করলে (যেমন PROBLEM_DISPUTED) এখানেই থামে —
        // এর নিচের কোনো local write (wallet deduction, problem COMPLETED, payout) তখনো হয়নি, তাই rollback লাগে না।
        if (!problem.acceptedSolverId.isNullOrBlank()) {
            releaseEscrowCloudFirst(freshEscrowForGuard)
        }

        val completedAt = System.currentTimeMillis()

        if (walletDeduction > 0.0) {
            // INSTANT local debit (Room); the cloud side goes through the RPC dual-write
            // further below with outbox retry on failure [Step 7.7 cleanup: pendingCloudSync
            // on the TransactionEntity below is an unread Firebase-era leftover, not the real
            // retry mechanism], instead of the old incrementUserBalance() fire-and-forget (no
            // retry if that single attempt failed).
            // [ব্যালেন্স ফিক্স] user হিসেবে extra বিল কাটা হচ্ছে, balanceUser mirror-ও আপডেট হয়।
            userDao.deductBalanceForUserRole(problem.userId, walletDeduction, completedAt)
        }
        val escrow = escrowDao.getByProblemId(problem.id)
        val (calcBase, calcExtra) = resolveSettlementAmounts(problem, escrow)
        val extraAmt = if (includeExtraAmount) calcExtra else 0.0
        val baseAmt = calcBase
        // Bug fix (transaction-history gap): previously this transaction record used
        // `walletDeduction` (the amount the CALLER claimed was taken from the wallet) and was
        // only written when that was > 0.0. The UI's gateway-payment path only ever passes the
        // wallet-covered slice here and quietly drops the gateway-paid remainder -- so with a
        // ৳0 wallet balance (the default for most users) and the whole extra bill paid via
        // gateway, walletDeduction came out 0.0 and NOTHING was recorded, even though the job
        // completed and the gateway charge went through. `extraAmt` (resolved just above from
        // the problem/escrow, the same value the UI itself used to size the gateway charge) is
        // the true total the user owed for this release, regardless of the wallet/gateway split,
        // so it's used here for the recorded amount; only the actual Room wallet debit above
        // stays limited to `walletDeduction`, since that's the only part that really left the
        // wallet balance.
        if (extraAmt > 0.0) {
            val deductionTrx = TransactionEntity(
                id = "TRX_RELEASE_DEDUCT_${problem.id}",
                problemId = problem.id,
                problemTitle = problem.title,
                userId = problem.userId,
                // solverId left blank on purpose -- see the identical note on TRX_BID_DEDUCT_
                // above; this is a user-wallet-ledger deduction record, not a solver earning.
                solverId = "",
                grossAmount = extraAmt,
                commissionPercent = 0.0,
                commissionAmount = 0.0,
                netAmount = -extraAmt,
                type = "RELEASE_DEDUCTION",
                pendingCloudSync = true,
                role = "USER"
            )
            transactionDao.insertTransaction(deductionTrx)
        }
        val updatedProblem = problem.copy(
            status = "COMPLETED",
            completedAt = completedAt,
            hasReleaseRequest = true,
            releaseRequestExtraAmount = extraAmt,
            confirmedExtraAmountTotal = if (extraAmt > 0.0) extraAmt else problem.confirmedExtraAmountTotal,
            completionResultSeenByUser = false,
            completionResultSeenBySolver = false,
            lastActivityAt = completedAt
        )
        problemDao.updateProblem(updatedProblem)

        val solverId = problem.acceptedSolverId ?: return

        // Update or insert Additional Charge record as ACCEPTED if extra amount is involved
        if (extraAmt > 0.0) {
            val existingCharges = additionalChargeDao.getAllByProblemId(problem.id).firstOrNull() ?: emptyList()
            val pendingCharge = existingCharges.find { it.status == "PENDING" }
            if (pendingCharge != null) {
                val updatedCharge = pendingCharge.copy(status = "ACCEPTED", respondedAt = completedAt)
                additionalChargeDao.insert(updatedCharge)

                // [SUPABASE-MIGRATED - ধাপ ২৯] `mark_additional_charge_settled` RPC -- এটা
                // **বুক-কিপিং-অনলি** (শুধু additional_charges.status/responded_at আপডেট করে,
                // কোনো wallet/escrow side-effect নেই), কারণ এই charge-এর টাকা ততক্ষণে ইতিমধ্যে
                // নড়ে গেছে এই ফাংশনের নিজের walletDeduction param + নিচের payoutEscrowToSolver()
                // (-> release_escrow RPC) দিয়ে। ভুল করে এখানে `respond_additional_charge` RPC কল
                // করলে সেই একই টাকা সার্ভার-সাইডে দ্বিতীয়বার wallet থেকে কাটা হতো (রিপোর্ট হওয়া
                // ডাবল-ডিডাকশন বাগ) -- তাই এটা কখনোই SupabaseSyncManager.respondToAdditionalCharge()
                // দিয়ে replace করবে না। **জানা সীমাবদ্ধতা**: RPC-টা শুধু owner/solver/admin caller
                // authorize করে -- 48-ঘণ্টা auto-release sweep (যেই ডিভাইসে app খোলা আছে সে ট্রিগার
                // করে) caller এই তিনটার কোনোটাই না হলে RPC `NOT_AUTHORIZED` ছুঁড়বে, যা শুধু log
                // হয়, local Room flow সম্পূর্ণ অপ্রভাবিত থাকে (best-effort, established
                // প্যাটার্নের মতোই)। এই charge Supabase-এ না-থাকা অবস্থাতেও (যেমন
                // requestAdditionalCharge()-এর dual-write আগে ব্যর্থ হয়ে থাকলে) RPC শুধু
                // `CHARGE_NOT_FOUND` ছুঁড়বে -- একইভাবে শুধু log, স্কোপের বাইরে কিছু ভাঙে না।
                if (SupabaseAuthManager.currentUserId() != null) {
                    SupabaseSyncManager.markAdditionalChargeSettled(updatedCharge.id)
                        .onSuccess { json ->
                            val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                                ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                            if (resultField != null && resultField != "OK" && resultField != "ALREADY_RESPONDED") {
                                Log.w("SomadhanRepo", "confirmReleaseAndComplete: markAdditionalChargeSettled non-OK result for ${updatedCharge.id}: $resultField (local flow unaffected)")
                            }
                        }
                        .onFailure { e ->
                            Log.w("SomadhanRepo", "confirmReleaseAndComplete: markAdditionalChargeSettled dual-write failed for ${updatedCharge.id} (local flow unaffected, likely NOT_AUTHORIZED/CHARGE_NOT_FOUND): ${e.message}")
                            // [OUTBOX WIRE - Step 12.5] paramsJson key "chargeId" -- OutboxRpcDispatcher.kt-এর
                            // "mark_additional_charge_settled" branch-এর সাথে অক্ষরে অক্ষরে মিলছে।
                            // idempotency (migration step29_mark_additional_charge_settled.sql বডি থেকে
                            // যাচাই): row-লক (`for update`) + `status <> 'PENDING'` হলে ALREADY_RESPONDED
                            // (early-return, no-op); কোনো wallet/escrow/balance ছোঁয় না, বুক-কিপিং-অনলি
                            // (RPC-র নিজের কমেন্টে স্পষ্ট লেখা)। IDEMPOTENT। ⚠️ residual (BLOCKED না, শুধু
                            // নথিভুক্ত): NOT_AUTHORIZED (caller owner/solver/admin না হলে, যেমন 48hr sweep
                            // যদি replay-সেশনে চলে) exception হিসেবে আসে -- retry-তেও সেই একই caller-session
                            // হলে চিরকাল fail-ই হবে, কিন্তু কোনো টাকা/গুরুত্বপূর্ণ state আটকে থাকে না (শুধু
                            // এই একটা bookkeeping label sync miss থাকবে, log-এ visible)।
                            enqueueOutboxRetry(
                                rpcName = "mark_additional_charge_settled",
                                params = kotlinx.serialization.json.JsonObject(
                                    buildMap {
                                        put("chargeId", kotlinx.serialization.json.JsonPrimitive(updatedCharge.id))
                                    }
                                ),
                                error = e
                            )
                        }
                }
            } else if (existingCharges.none { it.status == "ACCEPTED" }) {
                // [ধাপ ২৯ -- স্কোপের বাইরে, শুধু নোট] এই শাখায় Supabase-এ কোনো charge row-ই আগে
                // থেকে থাকে না (কোনো requestAdditionalCharge() dual-write হয়নি) -- সরাসরি একটা
                // ACCEPTED charge তৈরি হচ্ছে locally। `mark_additional_charge_settled` শুধু
                // UPDATE করে, existing row না থাকলে CHARGE_NOT_FOUND ছুঁড়বে -- তাই এখানে সেটা
                // কাজে লাগবে না, একটা আলাদা INSERT-based RPC লাগবে। মূল রিপোর্ট হওয়া বাগ
                // (respond_additional_charge দিয়ে ডাবল-ডিডাকশন) এই শাখায় প্রযোজ্য না (এখানে কোনো
                // RPC-ই কল হয় না, তাই ডাবল-ডিডাকশনের ঝুঁকি নেই) -- তবে এই শাখাটা এখনো Supabase-এ
                // dual-write হয় না, ভবিষ্যতের কোনো cleanup ধাপে (যেমন ধাপ ৩১) আলাদাভাবে দেখা
                // যেতে পারে।
                val chargeId = "EXTRA_${problem.id.takeLast(6)}_${completedAt.toString().takeLast(4)}"
                val newCharge = AdditionalChargeEntity(
                    id = chargeId,
                    problemId = problem.id,
                    solverId = solverId,
                    userId = problem.userId,
                    reason = problem.releaseRequestNote.ifBlank { "অতিরিক্ত কাজের বিল পরিশোধিত" },
                    amount = extraAmt,
                    status = "ACCEPTED",
                    createdAt = problem.releaseRequestedAt ?: completedAt,
                    respondedAt = completedAt
                )
                additionalChargeDao.insert(newCharge)
            }
        }

        val jobScore = platformSettingDao.getSetting("rep_score_job_completed")?.toDoubleOrNull() ?: 0.5
        val jobCap = platformSettingDao.getSetting("rep_cap_daily_job_completed")?.toDoubleOrNull() ?: 2.0
        applyCappedPerEventReputation(solverId, "JOB_COMPLETED", jobScore, defaultDailyCap = jobCap, problemId = problem.id, note = "কাজ সম্পন্ন করেছেন")
        applyCappedPerEventReputation(problem.userId, "JOB_COMPLETED", jobScore, defaultDailyCap = jobCap, problemId = problem.id, note = "কাজ শেষ পর্যন্ত সম্পন্ন করিয়েছেন")
        trackExtraPaymentMissCycle(solverId, extraAmt)

        // Trigger dynamic suggested events if enabled:
        // 1. FIRST_SOLVER_BADGE: First completed job in this category
        val priorCompletedInCat = problemDao.getAllProblems().firstOrNull()?.count {
            it.id != problem.id && it.status == "COMPLETED" && it.acceptedSolverId == solverId && it.categoryId == problem.categoryId
        } ?: 0
        if (priorCompletedInCat == 0) {
            triggerDynamicReputationEvent(
                userId = solverId,
                eventType = "FIRST_SOLVER_BADGE",
                problemId = problem.id,
                defaultScore = 0.5,
                defaultCap = 1.0,
                isPositive = true,
                customNote = "এই ক্যাটাগরিতে প্রথম কাজ সফলভাবে সম্পন্ন করার বোনাস"
            )
        }

        // 2. TIP_BONUS_RECEIVED: Client provided extra amount / tip
        if (extraAmt > 0.0) {
            triggerDynamicReputationEvent(
                userId = solverId,
                eventType = "TIP_BONUS_RECEIVED",
                problemId = problem.id,
                defaultScore = 0.5,
                defaultCap = 1.5,
                isPositive = true,
                customNote = "কাজে সন্তুষ্ট হয়ে ক্লায়েন্ট অতিরিক্ত অর্থ/টিপ প্রদান করেছেন"
            )
        }

        // 3. URGENT_SOS_HELP: Urgent SOS task completion
        if (problem.urgency == "জরুরি" || problem.urgency == "খুব জরুরি" || problem.urgency.equals("URGENT", ignoreCase = true)) {
            triggerDynamicReputationEvent(
                userId = solverId,
                eventType = "URGENT_SOS_HELP",
                problemId = problem.id,
                defaultScore = 1.5,
                defaultCap = 3.0,
                isPositive = true,
                customNote = "জরুরি SOS সমস্যা সফলভাবে সমাধান করেছেন"
            )
        }

        // 4. ZERO_DISPUTE_MILESTONE: Milestone of 10, 20, 30... completed jobs
        val totalCompletedForSolver = (problemDao.getAllProblems().firstOrNull()?.count {
            it.status == "COMPLETED" && it.acceptedSolverId == solverId
        } ?: 0) + 1
        if (totalCompletedForSolver > 0 && totalCompletedForSolver % 10 == 0) {
            triggerDynamicReputationEvent(
                userId = solverId,
                eventType = "ZERO_DISPUTE_MILESTONE",
                problemId = problem.id,
                defaultScore = 2.0,
                defaultCap = 2.0,
                isPositive = true,
                customNote = "ধারাবাহিক $totalCompletedForSolver টি সফল কাজ সম্পন্ন করার মাইলস্টোন"
            )
        }

        // 5. DISPUTE_SETTLED_FRIENDLY: Settled dispute mutually
        if (problem.isDisputed) {
            triggerDynamicReputationEvent(
                userId = solverId,
                eventType = "DISPUTE_SETTLED_FRIENDLY",
                problemId = problem.id,
                defaultScore = 1.0,
                defaultCap = 2.0,
                isPositive = true,
                customNote = "পারস্পরিক সমঝোতায় বিবাদ নিষ্পত্তি"
            )
            triggerDynamicReputationEvent(
                userId = problem.userId,
                eventType = "DISPUTE_SETTLED_FRIENDLY",
                problemId = problem.id,
                defaultScore = 1.0,
                defaultCap = 2.0,
                isPositive = true,
                customNote = "পারস্পরিক সমঝোতায় বিবাদ নিষ্পত্তি"
            )
        }

        // 6. LATE_DELIVERY: Excessive delay (>7 days)
        val timeTaken = completedAt - (problem.createdAt)
        if (timeTaken > 7L * 24 * 60 * 60 * 1000L) {
            triggerDynamicReputationEvent(
                userId = solverId,
                eventType = "LATE_DELIVERY",
                problemId = problem.id,
                defaultScore = 2.0,
                defaultCap = 4.0,
                defaultPenalty = 2.0,
                isPositive = false,
                customNote = "কাজে অতিরিক্ত বিলম্ব (Late Delivery) পেনাল্টি"
            )
        }

        val grossAmount = baseAmt + extraAmt

        // Centralized Commission Calculation + wallet credit + escrow release + Transaction insert
        // (shared with adminReleaseEscrow() and reconcileEscrowStates()'s self-heal path).
        val breakdown = payoutEscrowToSolver(
            escrow = escrow,
            problem = problem,
            solverId = solverId,
            baseAmount = baseAmt,
            extraAmount = extraAmt,
            releaseType = releaseType,
            cloudReleaseHandled = true
        )
        val commissionAmount = breakdown.totalCommission
        val netAmount = breakdown.netAmount
        val commSetting = if (grossAmount > 0.0) (commissionAmount / grossAmount) * 100.0 else 0.0

        // Notify Solver of payment & completion
        val extraNoteText = if (extraAmt > 0.0) " (অতিরিক্ত বিল ৳${DistanceUtil.toBengaliDigits(extraAmt.toInt().toString())} সহ)" else if (problem.releaseRequestExtraAmount > 0.0 && !includeExtraAmount) " (গ্রাহক মূল চুক্তি ৳${DistanceUtil.toBengaliDigits(baseAmt.toInt().toString())} অনুমোদন করেছেন)" else ""
        val solverNotif = NotificationEntity(
            role = "SOLVER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = solverId,
            title = "কাজ সম্পন্ন ও পেমেন্ট জমা! ৳${DistanceUtil.toBengaliDigits(netAmount.toInt().toString())}",
            message = "\"${problem.title}\" কাজটি সম্পন্ন হয়েছে$extraNoteText। প্ল্যাটফর্ম কমিশন (${DistanceUtil.toBengaliDigits(commSetting.toInt().toString())}%) বাদে ৳${DistanceUtil.toBengaliDigits(netAmount.toInt().toString())} আপনার ব্যালেন্সে যোগ হয়েছে।",
            targetType = "balance",
            targetId = solverId,
            relatedProblemId = problem.id
        )
        notificationDao.insertNotification(solverNotif)

        // Notify User of completion
        val userNotif = NotificationEntity(
            role = "USER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = problem.userId,
            title = "কাজ সম্পন্ন নিশ্চিত ✅",
            message = "\"${problem.title}\" কাজটি সফলভাবে সম্পন্ন হয়েছে। সমাধান ব্যবহারের জন্য ধন্যবাদ!",
            targetType = "problem",
            targetId = problem.id,
            relatedProblemId = problem.id
        )
        notificationDao.insertNotification(userNotif)

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৬] `create_notification` RPC দিয়ে userNotif dual-write.
        // এখানে শুধু userNotif -- solverNotif ইচ্ছাকৃতভাবে বাদ দেওয়া হলো, কারণ এই
        // confirmReleaseAndComplete() এর escrow-release path (payoutEscrowToSolver() -> `release_escrow`
        // RPC, batch ৫-এ ইতিমধ্যে wire করা) নিজেই server-side সলভারকে "পেমেন্ট প্রকাশিত হয়েছে"
        // notification পাঠায় -- এখানে আলাদা solverNotif dual-write যোগ করলে ডুপ্লিকেট হতো (batch ৬-এর
        // আবিষ্কার ১, দেখুন checkAndProcess48HourAutoReleases()-এর উপরের মন্তব্য)। userNotif আলাদা
        // তথ্য (owner-এর "কাজ সম্পন্ন নিশ্চিত" বার্তা) বহন করে, release_escrow RPC-এর সাথে সংঘর্ষ নেই।
        // caller (problem owner নিজে / admin dispute-resolution / 48hr sweep) এর উপর নির্ভর করে
        // RPC-এর admin/self-notify/problem-party চেক pass/fail করতে পারে (দেখুন
        // SupabaseSyncManager.createNotification() KDoc) -- best-effort, শুধু log হয়।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.createNotification(
                targetUserId = userNotif.userId,
                title = userNotif.title,
                message = userNotif.message,
                targetType = userNotif.targetType,
                targetId = userNotif.targetId?.ifBlank { null },
                relatedProblemId = userNotif.relatedProblemId,
                role = userNotif.role
            ).onFailure { e ->
                Log.w("SomadhanRepo", "confirmReleaseAndComplete: userNotif dual-write failed for ${problem.id} (local flow unaffected): ${e.message}")
            }
        }

        // Post chat message notice
        val releaseChatContent = if (extraAmt > 0.0) {
            "🎉 কাজ সম্পন্ন ও অনুমোদন করা হয়েছে! মূল চুক্তি ও অতিরিক্ত বিলসহ মোট ৳${DistanceUtil.toBengaliDigits(grossAmount.toInt().toString())} রিলিজ করা হলো।"
        } else {
            "🎉 কাজ সম্পন্ন ও অনুমোদন করা হয়েছে! মোট ৳${DistanceUtil.toBengaliDigits(grossAmount.toInt().toString())} রিলিজ করা হলো।"
        }
        sendMessage(
            problemId = problem.id,
            senderId = problem.userId,
            receiverId = solverId,
            senderName = problem.userName,
            content = releaseChatContent
        )

        if (stars > 0) {
            submitUserRatingForSolver(problem, stars, reviewComment)
        }
    }

    suspend fun markProblemCompleted(
        problem: ProblemEntity
    ) {
        confirmReleaseAndComplete(problem, includeExtraAmount = true)
    }

    suspend fun markProblemCompleted(problem: ProblemEntity, stars: Int, reviewComment: String) {
        confirmReleaseAndComplete(problem, includeExtraAmount = true, stars = stars, reviewComment = reviewComment)
    }

    suspend fun markProblemSeen(problemId: String, role: String) {
        val problem = problemDao.getProblemById(problemId) ?: return
        val now = System.currentTimeMillis()
        val updatedProblem = if (role.equals("SOLVER", ignoreCase = true)) {
            problem.copy(solverLastSeenAt = now)
        } else {
            problem.copy(userLastSeenAt = now)
        }
        problemDao.updateProblem(updatedProblem)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮৫] "শেষ কবে দেখেছে" ফ্ল্যাগ, cross-device sync দরকার।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.markProblemSeen(problemId, role).onFailure { e ->
                Log.w("SomadhanRepo", "markProblemSeen: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }
    }

    // ---------------- MESSAGING ----------------

    fun getMessagesForProblem(problemId: String): Flow<List<MessageEntity>> = messageDao.getMessagesForProblem(problemId)
    suspend fun getMessagesForProblemPage(problemId: String, limit: Int, offset: Int): List<MessageEntity> =
        messageDao.getMessagesForProblemPage(problemId, limit, offset)

    fun getAllUserMessages(userId: String): Flow<List<MessageEntity>> = messageDao.getAllUserMessages(userId)
    suspend fun getAllUserMessagesPage(userId: String, limit: Int, offset: Int): List<MessageEntity> =
        messageDao.getAllUserMessagesPage(userId, limit, offset)

    fun getAllMessagesFlow(): Flow<List<MessageEntity>> = messageDao.getAllMessagesFlow()
    suspend fun getAllMessagesFlowPage(limit: Int, offset: Int): List<MessageEntity> =
        messageDao.getAllMessagesFlowPage(limit, offset)

    fun getUnreadMessagesCount(userId: String): Flow<Int> = messageDao.getUnreadMessagesCountForUser(userId)

    fun getUnreadMessagesCountForProblem(problemId: String, userId: String): Flow<Int> =
        messageDao.getUnreadMessagesCountForProblem(problemId, userId)

    // [Offline Action Gating ধাপ ৯] সম্পূর্ণ local-first ("mark-seen" flag সবসময় instant Room
    // update, Supabase dual-write নিচে best-effort .onFailure দিয়ে wrap করা, কখনো caller পর্যন্ত
    // throw করে না)। ChatScreen থেকে কোনো callback ছাড়াই fire-and-forget কল হয় — markProblemSeen
    // (ধাপ ৮)/লাইভ-স্ট্যাটাস ফাংশনগুলোর (ধাপ ৭) মতোই ইচ্ছাকৃতভাবে guard বসানো হয়নি, কারণ guard
    // বসালে লোকাল read-flag আপডেটও স্কিপ হয়ে যেত, কোনো real সুবিধা ছাড়াই (টাকা/state-integrity
    // ঝুঁকি নেই — শুধু "পঠিত" tick)।
    suspend fun markMessagesAsReadForProblem(problemId: String, userId: String) {
        messageDao.markMessagesAsReadForProblem(problemId, userId)

        // [SUPABASE-MIGRATED - ধাপ ১১] `messages_update` RLS পলিসি অনুযায়ী শুধু receiver নিজেই
        // নিজের পাওয়া মেসেজ read মার্ক করতে পারে — তাই guard: currentUserId() == userId। ব্যর্থ
        // হলে শুধু log হয়, local read-status flow সম্পূর্ণ অপ্রভাবিত থাকে (best-effort)।
        if (SupabaseAuthManager.currentUserId() == userId) {
            SupabaseSyncManager.markMessagesAsReadForProblem(problemId, userId)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "markMessagesAsReadForProblem: Supabase dual-write failed for $problemId/$userId (local flow unaffected): ${e.message}")
                }
        }
    }

    // [Offline Action Gating ধাপ ৯] ইচ্ছাকৃতভাবে এখানে `requireOnlineOrWarn()`-স্টাইল হার্ড গার্ড
    // বসানো হয়নি (ধাপ ৫–৮-এর সাধারণ Group B প্যাটার্নের ব্যতিক্রম) — ইউজার স্পষ্টভাবে messenger-
    // অ্যাপের মতো UX চেয়েছেন: অফলাইনেও মেসেজ সাথে সাথে চ্যাট-বক্সে দেখা যাবে (local insert আটকানো
    // যাবে না), শুধু cloud-এ না পাঠাতে পারলে বাবলের নিচে ছোট "পাঠানো যায়নি — retry" দেখাবে। এই
    // ফাংশন এমনিতেই আগে থেকে local-first ছিল (`messageDao.insertMessage()` সবসময় হয়, dual-write
    // best-effort) — এই ধাপ শুধু আগের "নীরব silent-fail" (শুধু Log.w)-কে ইউজার-দৃশ্যমান
    // PENDING→SENT/FAILED স্ট্যাটাসে upgrade করছে, বিদ্যমান local-insert আচরণ অপরিবর্তিত রেখে (rule ১)।
    suspend fun sendMessage(
        problemId: String,
        senderId: String,
        receiverId: String,
        senderName: String,
        content: String,
        fileUrl: String? = null,
        fileName: String? = null,
        fileType: String? = null,
        isDirectContractProposal: Boolean = false,
        directContractBudget: Double? = null,
        directContractDuration: String? = null
    ): MessageEntity {
        var msg = MessageEntity(
            id = "MSG_${UUID.randomUUID().toString().take(8)}",
            problemId = problemId,
            senderId = senderId,
            receiverId = receiverId,
            senderName = senderName,
            content = content.trim(),
            timestamp = System.currentTimeMillis(),
            isRead = false,
            fileUrl = fileUrl,
            fileName = fileName,
            fileType = fileType,
            isDirectContractProposal = isDirectContractProposal,
            directContractBudget = directContractBudget,
            directContractDuration = directContractDuration,
            sendStatus = "PENDING"
        )
        messageDao.insertMessage(msg)

        // [SUPABASE-MIGRATED - ধাপ ১১] `messages_insert` RLS পলিসি অনুযায়ী শুধু sender নিজে, আর
        // এই problem-এর owner/accepted_solver হলেই insert করতে পারবে — guard:
        // currentUserId() == senderId। এই একই guard-এর কারণে `sendSystemEventMessage()`
        // (senderId="SYSTEM") আর admin-sent message এই ফাংশন দিয়ে যায় না, তাদের dual-write
        // এমনিতেই স্বাভাবিকভাবে skip হয়ে যাবে যদি কখনো এই ফাংশন ব্যবহার করা হয় — কিন্তু ওরা আলাদা
        // ফাংশন (`sendSystemEventMessage`/`adminSendMessageToProblemChat`) ব্যবহার করে, যেগুলো এই
        // ধাপে ইচ্ছাকৃতভাবে migrate করা হয়নি (নিচে/MIGRATION_PROGRESS.md-এ কারণ ব্যাখ্যা করা আছে)।
        if (SupabaseAuthManager.currentUserId() == senderId) {
            val result = SupabaseSyncManager.sendMessage(msg.toMessageDto())
            if (result.isSuccess) {
                messageDao.updateMessageSendStatus(msg.id, "SENT")
                msg = msg.copy(sendStatus = "SENT")
            } else {
                messageDao.updateMessageSendStatus(msg.id, "FAILED")
                msg = msg.copy(sendStatus = "FAILED")
                Log.w("SomadhanRepo", "sendMessage: Supabase dual-write failed for ${msg.id} (local message stays visible, marked FAILED for retry): ${result.exceptionOrNull()?.message}")
            }
        } else {
            // ডিফেন্সিভ ব্রাঞ্চ -- বাস্তবে ঘটে না (senderId সবসময় currentUser নিজেই), কিন্তু এই
            // পথ নিলে কোনো network attempt-ই হয় না বলে PENDING-এ আটকে থাকা এড়াতে সরাসরি SENT।
            messageDao.updateMessageSendStatus(msg.id, "SENT")
            msg = msg.copy(sendStatus = "SENT")
        }

        val prob = problemDao.getProblemById(problemId)
        if (prob != null) {
            val updatedProb = prob.copy(lastActivityAt = msg.timestamp)
            problemDao.updateProblem(updatedProb)
        }
        return msg
    }

    // [Offline Action Gating ধাপ ৯] ChatScreen-এর ইনলাইন "retry" ট্যাপ (ও reconnect-এ auto-retry,
    // দেখুন SomadhanViewModel.retryAllFailedMessagesOnReconnect) এই ফাংশন কল করে -- ইতিমধ্যে-লোকাল
    // মেসেজটা আবার cloud-এ পাঠানোর চেষ্টা করে, কোনো নতুন insert না (id অপরিবর্তিত রাখে যাতে
    // ChatScreen-এর LazyColumn key স্থিতিশীল থাকে, ডুপ্লিকেট বাবল না হয়)।
    suspend fun retrySendMessage(messageId: String): Boolean {
        val msg = messageDao.getMessageById(messageId) ?: return false
        if (msg.sendStatus != "FAILED") return true // ইতিমধ্যে SENT/PENDING -- করার কিছু নেই
        if (SupabaseAuthManager.currentUserId() != msg.senderId) return false

        val result = SupabaseSyncManager.sendMessage(msg.toMessageDto())
        return if (result.isSuccess) {
            messageDao.updateMessageSendStatus(messageId, "SENT")
            val prob = problemDao.getProblemById(msg.problemId)
            if (prob != null) {
                problemDao.updateProblem(prob.copy(lastActivityAt = msg.timestamp))
            }
            true
        } else {
            Log.w("SomadhanRepo", "retrySendMessage: এখনো ব্যর্থ $messageId: ${result.exceptionOrNull()?.message}")
            false
        }
    }

    // [Offline Action Gating ধাপ ৯] অফলাইন→অনলাইন reconnect-এ auto-retry-এর জন্য (দেখুন
    // SomadhanViewModel.retryAllFailedMessagesOnReconnect)।
    suspend fun getFailedMessagesForSender(userId: String): List<MessageEntity> =
        messageDao.getFailedMessagesForSender(userId)

    fun setTypingStatus(problemId: String, userId: String, isTyping: Boolean) {

        // [SUPABASE-MIGRATED - ধাপ ৩২.৯ক] SupabaseRealtimeManager.sendTypingStatus() suspend --
        // caller (ChatScreen/ViewModel) নন-suspend context থেকে fire-and-forget কল করে, তাই
        // FirebaseSyncManager.setTypingStatus()-এর syncScope.launch প্যাটার্নের মতোই এখানে
        // একটা lightweight ad-hoc coroutine launch করা হলো (best-effort, ব্যর্থ হলে শুধু log)।
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { SupabaseRealtimeManager.sendTypingStatus(problemId, userId, isTyping) }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "setTypingStatus: Supabase dual-run failed (local Room flow unaffected): ${e.message}")
                }
        }
    }

    // [SUPABASE-MIGRATED - ধাপ ৩২.৯ক] FirebaseSyncManager.typingStatusMap ও
    // SupabaseRealtimeManager.typingStatusMap (একই key format: "${problemId}_${userId}") merge
    // করা হলো -- ঠিক যেভাবে ধাপ ৩২.৫-এ admin metrics-এর জন্য combine() ব্যবহার হয়েছিল। কোনো
    // key conflict নেই বলে (উভয় সোর্সই একই format ব্যবহার করে) সরল ম্যাপ-মার্জই যথেষ্ট।
    // [SUPABASE-MIGRATED - ধাপ ৩৩.৩ কাজ ৫-৭] আগে FirebaseSyncManager.typingStatusMap ও
    // SupabaseRealtimeManager.typingStatusMap combine() করা হতো -- FirebaseSyncManager ডিলিট
    // হওয়ায় এখন শুধু Supabase-সাইড source ব্যবহার হচ্ছে।
    val typingStatusMap: Flow<Map<String, Long>> = SupabaseRealtimeManager.typingStatusMap

    // Realtime Scoping ফিক্স, ধাপ ৩ (Chat, dual-run) — ChatScreen খোলা/বন্ধ হওয়ার সময় `problem:<id>`
    // broadcast channel join/leave করে (postgresChangeFlow টেবিল-ওয়াইড messagesChannel-এর পাশাপাশি,
    // প্রতিস্থাপন না)। suspend ফাংশন সরাসরি -- caller (ViewModel-এর LaunchedEffect/DisposableEffect
    // suspend context) থেকে সরাসরি await করা যায়, typing-এর মতো fire-and-forget wrapper লাগে না।
    suspend fun joinProblemChatBroadcast(problemId: String) {
        runCatching { SupabaseRealtimeManager.joinProblemMessagesBroadcastChannel(problemId) }
            .onFailure { e -> Log.w("SomadhanRepo", "joinProblemChatBroadcast: Supabase dual-run failed (local Room flow unaffected): ${e.message}") }
    }

    suspend fun leaveProblemChatBroadcast(problemId: String) {
        runCatching { SupabaseRealtimeManager.leaveProblemMessagesBroadcastChannel(problemId) }
            .onFailure { e -> Log.w("SomadhanRepo", "leaveProblemChatBroadcast: Supabase dual-run failed (local Room flow unaffected): ${e.message}") }
    }

    // Realtime Scoping ফিক্স, ধাপ ৪ (Bids, dual-run) — ProblemDetailScreen খোলা/বন্ধ হওয়ার সময়
    // `problem:<id>:bids` broadcast channel join/leave করে (postgresChangeFlow টেবিল-ওয়াইড
    // bidsChannel-এর পাশাপাশি, প্রতিস্থাপন না)। joinProblemChatBroadcast-এর মতোই suspend সরাসরি।
    suspend fun joinProblemBidsBroadcast(problemId: String) {
        runCatching { SupabaseRealtimeManager.joinProblemBidsBroadcastChannel(problemId) }
            .onFailure { e -> Log.w("SomadhanRepo", "joinProblemBidsBroadcast: Supabase dual-run failed (local Room flow unaffected): ${e.message}") }
    }

    suspend fun leaveProblemBidsBroadcast(problemId: String) {
        runCatching { SupabaseRealtimeManager.leaveProblemBidsBroadcastChannel(problemId) }
            .onFailure { e -> Log.w("SomadhanRepo", "leaveProblemBidsBroadcast: Supabase dual-run failed (local Room flow unaffected): ${e.message}") }
    }

    suspend fun adminDeleteMessage(messageId: String) {
        messageDao.deleteMessage(messageId)

        // [SUPABASE-MIGRATED - ধাপ ১২ প্রি-ফিক্স] `messages` টেবিলে কোনো DELETE RLS policy নেই,
        // তাই একটা নতুন SECURITY DEFINER RPC (`admin_delete_message`) বানানো হয়েছে — guard
        // হিসেবে শুধু session আছে কিনা দেখা হচ্ছে (RPC নিজেই is_admin() চেক করে,
        // admin_adjust_balance-এর মতো একই প্যাটার্ন)। ব্যর্থ হলেও local Room flow অপ্রভাবিত।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminDeleteMessage(messageId)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "adminDeleteMessage: Supabase dual-write failed for $messageId (local flow unaffected): ${e.message}")
                }
        }

        logAdminAction(
            actionType = "DELETE_MESSAGE",
            targetId = messageId,
            targetName = "মেসেজ ($messageId)",
            details = "অ্যাডমিন কর্তৃক মেসেজ মুছে ফেলা হয়েছে"
        )
    }

    // ---------------- RATINGS ----------------

    fun getAllRatings(): Flow<List<RatingEntity>> = ratingDao.getAllRatings()
    suspend fun getAllRatingsPage(limit: Int, offset: Int): List<RatingEntity> =
        ratingDao.getAllRatingsPage(limit, offset)

    fun getRatingsForSolver(solverId: String): Flow<List<RatingEntity>> = ratingDao.getRatingsForSolver(solverId)
    suspend fun getRatingsForSolverPage(solverId: String, limit: Int, offset: Int): List<RatingEntity> =
        ratingDao.getRatingsForSolverPage(solverId, limit, offset)

    fun getRatingsByUserId(userId: String): Flow<List<RatingEntity>> = ratingDao.getRatingsByUserId(userId)
    suspend fun getRatingsByUserIdPage(userId: String, limit: Int, offset: Int): List<RatingEntity> =
        ratingDao.getRatingsByUserIdPage(userId, limit, offset)

    fun getRatingsReceivedBySolver(solverId: String): Flow<List<RatingEntity>> = ratingDao.getRatingsReceivedBySolver(solverId)
    suspend fun getRatingsReceivedBySolverPage(solverId: String, limit: Int, offset: Int): List<RatingEntity> =
        ratingDao.getRatingsReceivedBySolverPage(solverId, limit, offset)

    fun getRatingsReceivedByUser(userId: String): Flow<List<RatingEntity>> = ratingDao.getRatingsReceivedByUser(userId)
    suspend fun getRatingsReceivedByUserPage(userId: String, limit: Int, offset: Int): List<RatingEntity> =
        ratingDao.getRatingsReceivedByUserPage(userId, limit, offset)

    fun getRatingsGivenByUser(userId: String): Flow<List<RatingEntity>> = ratingDao.getRatingsGivenByUser(userId)
    suspend fun getRatingsGivenByUserPage(userId: String, limit: Int, offset: Int): List<RatingEntity> =
        ratingDao.getRatingsGivenByUserPage(userId, limit, offset)

    fun getRatingsGivenBySolver(solverId: String): Flow<List<RatingEntity>> = ratingDao.getRatingsGivenBySolver(solverId)
    suspend fun getRatingsGivenBySolverPage(solverId: String, limit: Int, offset: Int): List<RatingEntity> =
        ratingDao.getRatingsGivenBySolverPage(solverId, limit, offset)

    fun getAllRatingsReceivedByPerson(personId: String): Flow<List<RatingEntity>> = ratingDao.getAllRatingsReceivedByPerson(personId)
    suspend fun getAllRatingsReceivedByPersonPage(personId: String, limit: Int, offset: Int): List<RatingEntity> =
        ratingDao.getAllRatingsReceivedByPersonPage(personId, limit, offset)

    suspend fun adminDeleteRating(ratingId: String) {
        ratingDao.deleteRating(ratingId)

        // [SUPABASE-MIGRATED - ধাপ ১২ প্রি-ফিক্স] `ratings` টেবিলে কোনো DELETE RLS policy নেই,
        // তাই একটা নতুন SECURITY DEFINER RPC (`admin_delete_rating`) বানানো হয়েছে — guard
        // হিসেবে শুধু session আছে কিনা দেখা হচ্ছে (RPC নিজেই is_admin() চেক করে,
        // admin_adjust_balance-এর মতো একই প্যাটার্ন)। ব্যর্থ হলেও local Room flow অপ্রভাবিত।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminDeleteRating(ratingId)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "adminDeleteRating: Supabase dual-write failed for $ratingId (local flow unaffected): ${e.message}")
                }
        }

        logAdminAction(
            actionType = "DELETE_RATING",
            targetId = ratingId,
            targetName = "রিভিউ ($ratingId)",
            details = "অ্যাডমিন কর্তৃক রিভিউ মুছে ফেলা হয়েছে"
        )
    }

    suspend fun submitUserRatingForSolver(problem: ProblemEntity, stars: Int, comment: String) {
        val solverId = problem.acceptedSolverId ?: return
        val solverName = problem.acceptedSolverName ?: "সমাধানকারী"

        // Duplicate আটকাতে: এই problem-এ এই দিক থেকে আগে থেকেই রেটিং আছে কিনা চেক করো
        val existing = ratingDao.getRatingsForSolver(solverId).firstOrNull()
            ?.any { it.problemId == problem.id && it.raterRole == "USER" } ?: false
        if (existing) return

        val now = System.currentTimeMillis()
        val rating = RatingEntity(
            id = "RATING_${UUID.randomUUID().toString().take(8)}",
            problemId = problem.id,
            problemTitle = problem.title,
            userId = problem.userId,
            userName = problem.userName,
            solverId = solverId,
            solverName = solverName,
            stars = stars,
            comment = comment.trim(),
            createdAt = now,
            raterRole = "USER"
        )
        ratingDao.insertRating(rating)

        // [SUPABASE-MIGRATED - ধাপ ১১] `submit_rating` RPC সোর্স পড়ে যাচাই করা হয়েছে — RPC
        // নিজেই raterRole="USER" হলে caller-কে problem.user_id-এর সাথে মিলিয়ে authorize করে,
        // তাই guard এখানে currentUserId() == problem.userId। RPC নিজের rating_id বানায় (এখানে
        // ব্যবহার হয়নি — ratings-এর জন্য পরে কোনো cloud-id-নির্ভর dual-write ফাংশন নেই, একবারই
        // submit হয়)।
        if (SupabaseAuthManager.currentUserId() == problem.userId) {
            SupabaseSyncManager.submitRating(problem.id, stars, comment.trim(), "USER")
                .onFailure { e ->
                    Log.w("SomadhanRepo", "submitUserRatingForSolver: Supabase dual-write failed for ${rating.id} (local flow unaffected): ${e.message}")
                }
        }

        val prob = problemDao.getProblemById(problem.id)
        if (prob != null) {
            val updatedProb = prob.copy(lastActivityAt = now)
            problemDao.updateProblem(updatedProb)
        }

        val fiveStarScore = platformSettingDao.getSetting("rep_score_rating_5_star")?.toDoubleOrNull() ?: 1.5
        val fourStarScore = platformSettingDao.getSetting("rep_score_rating_4_star")?.toDoubleOrNull() ?: 0.5
        val badRatingPenalty = platformSettingDao.getSetting("rep_penalty_rating_bad")?.toDoubleOrNull() ?: 1.0

        if (stars >= 4) {
            val starBonus = if (stars == 5) fiveStarScore else fourStarScore
            val ratingCap = platformSettingDao.getSetting("rep_cap_daily_rating")?.toDoubleOrNull() ?: 2.0
            applyCappedPerEventReputation(
                userId = solverId,
                eventType = "RATING_BONUS",
                defaultRawScore = starBonus,
                customScore = starBonus,
                defaultDailyCap = ratingCap,
                problemId = problem.id,
                note = "${stars} স্টার রেটিং পেয়েছেন"
            )
        } else {
            applyReputationChange(solverId, "RATING_PENALTY", -badRatingPenalty, problem.id, "${stars} স্টার রেটিং (নেতিবাচক রেটিং)")
        }
    }

    suspend fun submitSolverRatingForUser(problem: ProblemEntity, stars: Int, comment: String) {
        val solverId = problem.acceptedSolverId ?: return
        val solver = userDao.getUserById(solverId) ?: return

        // Duplicate আটকাতে: এই problem-এ এই দিক থেকে আগে থেকেই রেটিং আছে কিনা চেক করো
        val existing = ratingDao.getRatingsForSolver(solverId).firstOrNull()
            ?.any { it.problemId == problem.id && it.raterRole == "SOLVER" } ?: false
        if (existing) return

        val now = System.currentTimeMillis()
        val rating = RatingEntity(
            id = "RATING_${UUID.randomUUID().toString().take(8)}",
            problemId = problem.id,
            problemTitle = problem.title,
            userId = problem.userId,
            userName = problem.userName,
            solverId = solverId,
            solverName = solver.name,
            stars = stars,
            comment = comment,
            raterRole = "SOLVER"
        )
        ratingDao.insertRating(rating)

        // [SUPABASE-MIGRATED - ধাপ ১১] উপরের submitUserRatingForSolver()-এর মতোই — raterRole=
        // "SOLVER" হলে RPC caller-কে problem.accepted_solver_id-এর সাথে মিলিয়ে authorize করে।
        if (SupabaseAuthManager.currentUserId() == solverId) {
            SupabaseSyncManager.submitRating(problem.id, stars, comment, "SOLVER")
                .onFailure { e ->
                    Log.w("SomadhanRepo", "submitSolverRatingForUser: Supabase dual-write failed for ${rating.id} (local flow unaffected): ${e.message}")
                }
        }

        val prob = problemDao.getProblemById(problem.id)
        if (prob != null) {
            val updatedProb = prob.copy(lastActivityAt = now)
            problemDao.updateProblem(updatedProb)
        }
    }

    // ---------------- NOTIFICATIONS ----------------

    // [ROLE_SEPARATION ধাপ ৬] currentRole ডিফল্ট "" (পুরনো caller ভাঙবে না -- সব দেখাবে,
    // আগের মতোই)। নতুন caller (ViewModel.observeUserData/loadNextNotificationsPage) বর্তমান
    // active role পাস করে।
    fun getNotificationsForUser(userId: String, currentRole: String = ""): Flow<List<NotificationEntity>> =
        notificationDao.getNotificationsForUser(userId, currentRole)
    suspend fun getNotificationsForUserPage(userId: String, limit: Int, offset: Int, currentRole: String = "", now: Long = System.currentTimeMillis()): List<NotificationEntity> =
        notificationDao.getNotificationsForUserPage(userId, currentRole, limit, offset, now)

    fun getAllNotifications(): Flow<List<NotificationEntity>> = notificationDao.getAllNotificationsFlow()
    suspend fun getAllNotificationsFlowPage(limit: Int, offset: Int): List<NotificationEntity> =
        notificationDao.getAllNotificationsFlowPage(limit, offset)

    fun getUnreadNotificationCount(userId: String, currentRole: String = ""): Flow<Int> = notificationDao.getUnreadCount(userId, currentRole)

    fun getUnreadProblemNotificationCount(userId: String, currentRole: String = ""): Flow<Int> = notificationDao.getUnreadProblemNotificationsFlow(userId, currentRole)

    suspend fun getUnreadCountForUser(userId: String, currentRole: String = ""): Int = notificationDao.getUnreadCountForUser(userId, currentRole)

    suspend fun markAllNotificationsAsRead(userId: String) {
        notificationDao.markAllAsRead(userId)

        // [SUPABASE-MIGRATED - ধাপ ১২] `notifications_update` RLS পলিসি অনুযায়ী শুধু নিজের
        // (auth.uid() = user_id) row read মার্ক করা যায় -- guard: currentUserId() == userId।
        if (SupabaseAuthManager.currentUserId() == userId) {
            SupabaseSyncManager.markAllNotificationsAsRead(userId)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "markAllNotificationsAsRead: Supabase dual-write failed for $userId (local flow unaffected): ${e.message}")
                }
        }
    }

    suspend fun markNotificationAsRead(notificationId: String) {
        notificationDao.markAsRead(notificationId)

        // [SUPABASE-MIGRATED - ধাপ ১২] এই ফাংশনের signature-এ userId নেই, কিন্তু RLS guard
        // (auth.uid() = user_id) মেলাতে notification-টা আসলে কার সেটা জানা দরকার -- তাই local Room
        // থেকে notification lookup করে owner-এর userId বের করা হয় (markAsRead() উপরে already
        // চলে গেছে, isRead ছাড়া বাকি সব ফিল্ড অপরিবর্তিত থাকে, তাই এই lookup নিরাপদ)।
        val notif = notificationDao.getNotificationById(notificationId)
        if (notif != null && SupabaseAuthManager.currentUserId() == notif.userId) {
            SupabaseSyncManager.markNotificationAsRead(notificationId)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "markNotificationAsRead: Supabase dual-write failed for $notificationId (local flow unaffected): ${e.message}")
                }
        }
    }

    suspend fun sendManualNotification(
        targetRole: String, // "ALL", "USER", "SOLVER"
        title: String,
        message: String,
        targetType: String = "general",
        scheduledFor: Long? = null
    ): Int {
        val users = when (targetRole.uppercase()) {
            "USER" -> userDao.getUsersByRole("USER")
            "SOLVER" -> userDao.getUsersByRole("SOLVER")
            else -> userDao.getAllUsersList()
        }
        if (users.isEmpty()) return 0

        // [ROLE_SEPARATION ধাপ ৬] ADMIN নিজে থেকে "ALL" পাঠালে role-neutral (""), নাহলে
        // targetRole অনুযায়ী নির্দিষ্ট role-এ ট্যাগ হবে।
        val notifRole = when (targetRole.uppercase()) {
            "USER" -> "USER"
            "SOLVER" -> "SOLVER"
            else -> ""
        }

        val timestamp = System.currentTimeMillis()
        val notifs = users.map { u ->
            NotificationEntity(
                id = "NOTIF_ADMIN_${UUID.randomUUID().toString().take(8)}",
                userId = u.id,
                title = title.trim(),
                message = message.trim(),
                timestamp = timestamp,
                isRead = false,
                role = notifRole,
                targetType = targetType,
                targetId = null,
                relatedProblemId = null,
                scheduledFor = scheduledFor
            )
        }
        notificationDao.insertNotifications(notifs)

        // [SUPABASE-MIGRATED - ধাপ ১২] `notifications` টেবিলে কোনো client-side INSERT policy নেই,
        // তাই SECURITY DEFINER RPC `admin_broadcast_notification` ব্যবহার করা হয় (ধাপ ১২
        // প্রি-ফিক্সে সোর্স পড়ে যাচাই করা হয়েছে -- caller admin না হলে RPC নিজেই `ADMIN_ONLY`
        // exception ছোঁড়ে)। RPC নিজে server-side আলাদাভাবে target user list বের করে (একই
        // targetRole ফিল্টার দিয়ে) আর id/timestamp নিজে generate করে -- তাই local `notifs` লিস্টের
        // id গুলোর সাথে cloud-এ তৈরি হওয়া id হুবহু নাও মিলতে পারে, কিন্তু row content
        // (title/message/targetType/scheduledFor/টার্গেট user set) সমতুল্য থাকে। ব্যর্থ হলেও
        // local Room notification flow সম্পূর্ণ অপ্রভাবিত।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminBroadcastNotification(targetRole, title.trim(), message.trim(), targetType, scheduledFor)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "sendManualNotification: Supabase dual-write failed for role=$targetRole (local flow unaffected): ${e.message}")
                }
        }

        logAdminAction(
            actionType = "SEND_NOTIFICATION",
            targetId = targetRole,
            targetName = "টার্গেট: $targetRole",
            details = "শিরোনাম: $title (${notifs.size} জনকে পাঠানো হয়েছে${if (scheduledFor != null) ", শিডিউল করা" else ""})"
        )
        return notifs.size
    }

    suspend fun deleteScheduledNotification(title: String, scheduledFor: Long?, timestamp: Long) {
        // Bug fix: previously only "title" was sent to deleteMatchingFromCloud, which deleted
        // EVERY cloud notification doc with that title regardless of which batch/time it was
        // from. Now timestamp/scheduledFor is included too, so only this specific batch's cloud
        // docs are removed -- other batches with the same title survive a resync.
        if (scheduledFor != null) {
            notificationDao.deleteScheduledNotificationGroup(title.trim(), scheduledFor)
        } else {
            notificationDao.deleteNotificationGroup(title.trim(), timestamp)
        }

        // [SUPABASE-MIGRATED - dhap 12] `notifications` table e kono client-side DELETE policy
        // nei, tai SECURITY DEFINER RPC `admin_delete_notification_group` babohar kora hoy (dhap
        // 12 pre-fix e source pore jachai kora hoyeche). scheduledFor thakle seta diye scope kora
        // hoy, na hole timestamp diye. bortho holeo local Room flow shomporno oprovabito.
        if (SupabaseAuthManager.currentUserId() != null) {
            val result = if (scheduledFor != null) {
                SupabaseSyncManager.adminDeleteNotificationGroup(title.trim(), scheduledForMillis = scheduledFor)
            } else {
                SupabaseSyncManager.adminDeleteNotificationGroup(title.trim(), timestampMillis = timestamp)
            }
            result.onFailure { e ->
                Log.w("SomadhanRepo", "deleteScheduledNotification: Supabase dual-write failed for '$title' (local flow unaffected): ${e.message}")
            }
        }

        logAdminAction(
            actionType = "DELETE_NOTIFICATION",
            targetId = title,
            targetName = title,
            details = "বিজ্ঞপ্তি মুছে ফেলা/বাতিল করা হয়েছে"
        )
    }

    suspend fun deleteNotificationGroup(title: String, timestamp: Long) {
        // Bug fix: scope the cloud delete to title + timestamp, same reasoning as
        // deleteScheduledNotification() above -- see the comment there.
        notificationDao.deleteNotificationGroup(title.trim(), timestamp)

        // [SUPABASE-MIGRATED - dhap 12] deleteScheduledNotification()-er else-branch er motoi
        // eki RPC (`admin_delete_notification_group`), timestamp diye scope kora.
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminDeleteNotificationGroup(title.trim(), timestampMillis = timestamp)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "deleteNotificationGroup: Supabase dual-write failed for '$title' (local flow unaffected): ${e.message}")
                }
        }

        logAdminAction(
            actionType = "DELETE_NOTIFICATION",
            targetId = title,
            targetName = title,
            details = "বিজ্ঞপ্তি মুছে ফেলা হয়েছে"
        )
    }

    // ---------------- KYC ----------------

    suspend fun submitKyc(
        user: UserEntity,
        firstName: String,
        lastName: String,
        address: String,
        documentType: String,
        documentNumber: String,
        docFrontUri: String,
        docBackUri: String,
        selfieUri: String
    ) {
        val updated = user.copy(
            kycFirstName = firstName.trim(),
            kycLastName = lastName.trim(),
            kycAddress = address.trim(),
            kycDocumentType = documentType.trim(),
            kycDocumentNumber = documentNumber.trim(),
            kycDocumentFrontImage = docFrontUri,
            kycDocumentBackImage = docBackUri,
            kycSelfieImage = selfieUri,
            kycStatus = "pending",
            isKycVerified = false, // Critical Rule 10.2: NEVER auto-verify!
            kycSubmissionDate = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        userDao.updateUser(updated)

        // [ফিক্স] KYC শুধু Solver role-এর জন্য, আর বেশিরভাগ ব্যবহারকারীই আগে User হিসেবে
        // sign up করে পরে Solver role যোগ করে — অর্থাৎ তাদের SOLVER local row-টাই root না,
        // "linked" row (id auth.uid()-এর সাথে মেলে না, শুধু root row-এরই মেলে; দ্রষ্টব্য
        // depositMoneyViaGateway()/switchRoleInPlace()-এর একই প্যাটার্ন)। আগে এখানে সরাসরি
        // `currentUserId() == user.id` চেক করা হতো, যেটা এই common case-এ সবসময় false হতো —
        // ফলে cloud dual-write (users.kyc_status ইত্যাদি আপডেট) কখনো হতোই না, এমনকি কোনো
        // error/log ছাড়াই। তাই local device-এ "pending" ঠিকই দেখাত, কিন্তু admin panel (cloud
        // data থেকে read করে) কখনো সেই pending KYC দেখতে পেত না। এখন rootAccountId দিয়ে চেক
        // করা হচ্ছে, ঠিক depositMoneyViaGateway()-এর ফিক্সের মতোই।
        val rootAccountId = user.linkedAccountId?.takeIf { it.isNotBlank() } ?: user.id
        if (SupabaseAuthManager.currentUserId() == rootAccountId) {
            SupabaseSyncManager.submitKyc(
                firstName = updated.kycFirstName ?: "",
                lastName = updated.kycLastName ?: "",
                address = updated.kycAddress ?: "",
                documentType = updated.kycDocumentType ?: "",
                documentNumber = updated.kycDocumentNumber ?: "",
                docFrontUri = updated.kycDocumentFrontImage ?: "",
                docBackUri = updated.kycDocumentBackImage ?: "",
                selfieUri = updated.kycSelfieImage ?: "",
                submissionDateMillis = updated.kycSubmissionDate ?: System.currentTimeMillis()
            ).onFailure { e ->
                // [বাগফিক্স] আগে এই Result সম্পূর্ণ discard করা হতো — ব্যর্থ হলেও কোনো log না
                // থাকায় এই বাগটা বহুদিন ধরে অদৃশ্য ছিল। এখন অন্য সব dual-write-এর কনভেনশন
                // অনুযায়ী log করা হচ্ছে, যাতে ভবিষ্যতে আবার ব্যর্থ হলে অন্তত logcat-এ ধরা পড়ে।
                Log.w("SomadhanRepo", "submitKyc: Supabase dual-write failed for $rootAccountId (local flow unaffected): ${e.message}")
            }
        }

        notificationDao.insertNotification(
            NotificationEntity(
                role = "SOLVER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = user.id,
                title = "KYC আবেদন গৃহীত হয়েছে",
                message = "আপনার KYC ভেরিফিকেশন আবেদন অ্যাডমিন পর্যালোচনার জন্য জমা হয়েছে। শীঘ্রই স্ট্যাটাস জানানো হবে।",
                targetType = "kyc",
                targetId = user.id
            )
        )

        // [SUPABASE-MIGRATED - ধাপ ২৪ ব্যাচ ১] `create_notification` RPC দিয়ে dual-write —
        // এটা self-notify (caller নিজেই notify হচ্ছে, RPC-এর "v_caller = p_target_user_id"
        // শর্তে explicitly অনুমোদিত, এই session-এ Supabase MCP দিয়ে সোর্স পড়ে verify করা
        // হয়েছে)। adminApproveKyc/adminRejectKyc/adminRevokeKyc-এর মতো এখানে কোনো RPC নেই যেটা
        // নিজে থেকে notification insert করে (submitKyc()-এর জন্য আলাদা কোনো RPC আগে থেকে নেই)
        // — তাই এই একটাই গ্যাপ ছিল KYC গ্রুপে। guard: caller নিজেই এই user কিনা (self-notify)।
        // ব্যর্থ হলে শুধু log হবে, local Room flow অপ্রভাবিত থাকে।
        if (SupabaseAuthManager.currentUserId() == rootAccountId) {
            SupabaseSyncManager.createNotification(
                targetUserId = rootAccountId,
                title = "KYC আবেদন গৃহীত হয়েছে",
                message = "আপনার KYC ভেরিফিকেশন আবেদন অ্যাডমিন পর্যালোচনার জন্য জমা হয়েছে। শীঘ্রই স্ট্যাটাস জানানো হবে।",
                targetType = "kyc",
                targetId = rootAccountId,
                role = "SOLVER"
            ).onFailure { e ->
                Log.w("SomadhanRepo", "submitKyc: createNotification dual-write failed for ${user.id} (local flow unaffected): ${e.message}")
            }
        }
    }

    suspend fun adminApproveKyc(userId: String) {
        val user = userDao.getUserById(userId) ?: return
        val wasAlreadyVerified = user.isKycVerified
        val updated = user.copy(
            isKycVerified = true,
            kycStatus = "verified",
            updatedAt = System.currentTimeMillis()
        )
        userDao.updateUser(updated)

        val notif = NotificationEntity(
            role = "SOLVER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = userId,
            title = "KYC ভেরিফিকেশন সফল! ✅",
            message = "অভিনন্দন! আপনার KYC আবেদন অনুমোদিত হয়েছে এবং আপনার প্রোফাইল ভেরিফাইড হয়েছে। এখন সব পোস্টে বিড করতে পারবেন।",
            targetType = "kyc",
            targetId = userId
        )
        notificationDao.insertNotification(notif)

        // [SUPABASE-MIGRATED - ধাপ ১২ (মূল কাজ, ব্যাচ ১)] `admin_approve_kyc` RPC (এই session-এ
        // নতুন migration দিয়ে তৈরি) is_admin() চেক করে, users.is_kyc_verified/kyc_status আপডেট
        // করে, আর নিজেই notification insert করে (উপরের local notif-এর সমতুল্য টেক্সট) — তাই এখানে
        // আলাদা করে createNotification() কল করার দরকার নেই। guard: শুধু session আছে কিনা (RPC
        // নিজেই admin authorize করে, adminAdjustBalance-এর মতোই প্যাটার্ন)। best-effort।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminApproveKyc(userId).onFailure { e ->
                Log.w("SomadhanRepo", "adminApproveKyc: Supabase dual-write failed for $userId (local flow unaffected): ${e.message}")
                // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ৩ (KYC গ্রুপ)] paramsJson key "userId" --
                // OutboxRpcDispatcher.kt-এর "admin_approve_kyc" branch-এর সাথে মিলছে। এই RPC
                // নিজে notification insert করে বলে অতিরিক্ত কোনো param লাগে না।
                enqueueOutboxRetry(
                    rpcName = "admin_approve_kyc",
                    params = kotlinx.serialization.json.JsonObject(
                        mapOf("userId" to kotlinx.serialization.json.JsonPrimitive(userId))
                    ),
                    error = e
                )
            }
        }

        if (!wasAlreadyVerified) {
            val kycBonus = platformSettingDao.getSetting("rep_score_kyc_verified")?.toDoubleOrNull() ?: 5.0
            applyReputationChange(userId, "KYC_VERIFIED", kycBonus, null, "KYC ভেরিফাই সম্পন্ন")
            
            // Trigger 100% profile completed event if all fields are filled
            if (user.name.isNotBlank() && user.phone.isNotBlank() && user.address.isNotBlank() && !user.profileImageUri.isNullOrBlank()) {
                triggerDynamicReputationEvent(
                    userId = userId,
                    eventType = "PROFILE_COMPLETED_100",
                    problemId = null,
                    defaultScore = 2.0,
                    defaultCap = 2.0,
                    isPositive = true,
                    customNote = "প্রোফাইল শতভাগ সম্পূর্ণকরণ বোনাস"
                )
            }
        }

        logAdminAction(
            actionType = "APPROVE_KYC",
            targetId = userId,
            targetName = user.name,
            details = "KYC অনুমোদন করা হয়েছে এবং একাউন্ট ভেরিফাই করা হয়েছে",
            role = "SOLVER"
        )
    }

    suspend fun adminRejectKyc(userId: String, reason: String) {
        val user = userDao.getUserById(userId) ?: return
        val updated = user.copy(
            isKycVerified = false,
            kycStatus = "rejected",
            kycRejectReason = reason,
            updatedAt = System.currentTimeMillis()
        )
        userDao.updateUser(updated)

        val notif = NotificationEntity(
            role = "SOLVER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = userId,
            title = "KYC আবেদন বাতিল হয়েছে",
            message = "আপনার KYC আবেদন বাতিল হয়েছে। কারণ: $reason। অনুগ্রহ করে পুনরায় সঠিক তথ্য ও ছবি জমা দিন।",
            targetType = "kyc",
            targetId = userId
        )
        notificationDao.insertNotification(notif)

        // [SUPABASE-MIGRATED - ধাপ ১২ (মূল কাজ, ব্যাচ ১)] `admin_reject_kyc` RPC — নিজেই
        // notification insert করে, guard/প্যাটার্ন `adminApproveKyc()`-এর মতোই।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminRejectKyc(userId, reason).onFailure { e ->
                Log.w("SomadhanRepo", "adminRejectKyc: Supabase dual-write failed for $userId (local flow unaffected): ${e.message}")
                // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ৩ (KYC গ্রুপ)] paramsJson keys "userId"/"reason" --
                // OutboxRpcDispatcher.kt-এর "admin_reject_kyc" branch-এর সাথে মিলছে।
                enqueueOutboxRetry(
                    rpcName = "admin_reject_kyc",
                    params = kotlinx.serialization.json.JsonObject(
                        mapOf(
                            "userId" to kotlinx.serialization.json.JsonPrimitive(userId),
                            "reason" to kotlinx.serialization.json.JsonPrimitive(reason)
                        )
                    ),
                    error = e
                )
            }
        }

        logAdminAction(
            actionType = "REJECT_KYC",
            targetId = userId,
            targetName = user.name,
            details = "বাতিলের কারণ: $reason",
            role = "SOLVER"
        )
    }

    suspend fun adminRevokeKyc(userId: String, reason: String) {
        val user = userDao.getUserById(userId) ?: return
        userDao.revokeKyc(userId, reason, System.currentTimeMillis())

        val notif = NotificationEntity(
            role = "SOLVER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = userId,
            title = "KYC ভেরিফিকেশন প্রত্যাহার করা হয়েছে",
            message = "আপনার KYC ভেরিফিকেশন প্রত্যাহার করা হয়েছে। কারণ: $reason",
            targetType = "kyc",
            targetId = userId
        )
        notificationDao.insertNotification(notif)

        // [SUPABASE-MIGRATED - ধাপ ১২ (মূল কাজ, ব্যাচ ১)] `admin_revoke_kyc` RPC — নিজেই
        // notification insert করে, guard/প্যাটার্ন `adminApproveKyc()`-এর মতোই।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminRevokeKyc(userId, reason).onFailure { e ->
                Log.w("SomadhanRepo", "adminRevokeKyc: Supabase dual-write failed for $userId (local flow unaffected): ${e.message}")
                // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ৩ (KYC গ্রুপ)] paramsJson keys "userId"/"reason" --
                // OutboxRpcDispatcher.kt-এর "admin_revoke_kyc" branch-এর সাথে মিলছে।
                enqueueOutboxRetry(
                    rpcName = "admin_revoke_kyc",
                    params = kotlinx.serialization.json.JsonObject(
                        mapOf(
                            "userId" to kotlinx.serialization.json.JsonPrimitive(userId),
                            "reason" to kotlinx.serialization.json.JsonPrimitive(reason)
                        )
                    ),
                    error = e
                )
            }
        }

        logAdminAction(
            actionType = "REVOKE_KYC",
            targetId = userId,
            targetName = user.name,
            details = "ভেরিফিকেশন প্রত্যাহার: $reason",
            role = "SOLVER"
        )
    }

    suspend fun adminUpdateKycInfo(
        userId: String,
        docNumber: String,
        firstName: String,
        lastName: String,
        address: String
    ) {
        val user = userDao.getUserById(userId) ?: return
        userDao.updateKycInfo(userId, docNumber, firstName, lastName, address)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮] `users_update_admin` RLS টেবিল-লেভেলে admin write কভার করে
        // (বিস্তারিত মন্তব্য SupabaseSyncManager.kt-এ) -- তাই RPC লাগেনি, সরাসরি partial update()।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminUpdateKycInfo(userId, docNumber, firstName, lastName, address).onFailure { e ->
                Log.w("SomadhanRepo", "adminUpdateKycInfo: Supabase dual-write failed for $userId (local flow unaffected): ${e.message}")
            }
        }

        logAdminAction(
            actionType = "UPDATE_KYC_INFO",
            targetId = userId,
            targetName = user.name,
            details = "ডকুমেন্ট: $docNumber, নাম: $firstName $lastName, ঠিকানা: $address",
            role = "SOLVER"
        )
    }

    suspend fun adminResetKycToPending(userId: String) {
        val user = userDao.getUserById(userId) ?: return
        userDao.resetKycToPending(userId, System.currentTimeMillis())

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮] একই `users_update_admin` RLS দিয়ে কভার্ড, RPC লাগেনি।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminResetKycToPending(userId).onFailure { e ->
                Log.w("SomadhanRepo", "adminResetKycToPending: Supabase dual-write failed for $userId (local flow unaffected): ${e.message}")
            }
        }

        logAdminAction(
            actionType = "RESET_KYC_PENDING",
            targetId = userId,
            targetName = user.name,
            details = "আবেদন পুনরায় বিবেচনা করতে পেন্ডিং তালিকায় নেওয়া হয়েছে",
            role = "SOLVER"
        )
    }

    // ---------------- WITHDRAWALS ----------------

    fun getWithdrawalsForSolver(solverId: String): Flow<List<WithdrawalEntity>> = withdrawalDao.getWithdrawalsForSolver(solverId)
    suspend fun getWithdrawalsForSolverPage(solverId: String, limit: Int, offset: Int): List<WithdrawalEntity> =
        withdrawalDao.getWithdrawalsForSolverPage(solverId, limit, offset)

    fun getAllWithdrawals(): Flow<List<WithdrawalEntity>> = withdrawalDao.getAllWithdrawals()
    suspend fun getAllWithdrawalsPage(limit: Int, offset: Int): List<WithdrawalEntity> =
        withdrawalDao.getAllWithdrawalsPage(limit, offset)

    suspend fun requestWithdrawal(
        solver: UserEntity,
        amount: Double,
        method: String,
        accountNumber: String,
        bankName: String?,
        branchName: String?,
        accountHolderName: String?,
        // [Step 12.10d] কোন role-এর ব্যালেন্স থেকে উইথড্র: "SOLVER" (ডিফল্ট, আগের আচরণ) বা "USER"।
        role: String = "SOLVER"
    ): Result<WithdrawalEntity> {
        if (role != "SOLVER" && role != "USER") {
            return Result.failure(Exception("অবৈধ ভূমিকা (role)।"))
        }
        val roleBalance = if (role == "SOLVER") solver.balanceSolver else solver.balanceUser
        val minWithdrawal = platformSettingDao.getSetting("min_withdrawal")?.toDoubleOrNull() ?: 100.0
        if (amount < minWithdrawal) {
            return Result.failure(Exception("সর্বনিম্ন উত্তোলনের পরিমাণ ৳${DistanceUtil.toBengaliDigits(minWithdrawal.toInt().toString())}।"))
        }
        // [ব্যালেন্স ফিক্স — ধাপ ৪] আগে এখানে legacy shared solver.balance পড়া হতো — উইথড্র সবসময়
        // SOLVER-context থেকেই হয় (এই ফাংশনের প্যারামিটারই solver: UserEntity), তাই
        // solver.balanceSolver-ই সঠিক, role-switch-এর পর stale হতে পারা shared balance না।
        // [Step 12.10d] role-অনুযায়ী ব্যালেন্স (`roleBalance`), সবসময় balanceSolver না।
        if (amount > roleBalance) {
            return Result.failure(Exception("আপনার অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই। বর্তমান ব্যালেন্স: ৳${DistanceUtil.toBengaliDigits(roleBalance.toInt().toString())}"))
        }

        // [RPC_SYNC_FIX — id-mismatch ফিক্স, Step 7 ব্যাচ ২-এর পরে] আগে RPC সার্ভার-সাইডে
        // নিজে থেকে একটা নতুন withdrawal_id বানাতো (client পাঠাতে পারতো না) — তাই RPC fail
        // করে outbox retry পরে সফল হলে cloud-এ একটা আলাদা ("এতিম") id-র withdrawal তৈরি হতো
        // যেটা এই local `withdrawId`-এর সাথে link থাকতো না (progress note-এ বিস্তারিত ছিল)।
        // এখন migration step38 (`request_withdrawal`-এ নতুন ঐচ্ছিক p_client_withdrawal_id
        // প্যারামিটার) দেওয়া হলে RPC এই local `withdrawId`-টাই cloud id হিসেবে ব্যবহার করে —
        // তাই এখন এই id-ই নিচে তাৎক্ষণিক কলে *এবং* outbox retry params-এ পাঠানো হচ্ছে, দুই
        // ক্ষেত্রেই local আর cloud সবসময় একই id শেয়ার করবে (immediate success হোক বা পরে
        // retry-তে success)। RPC fail করলে (session নেই, network error) আগের মতোই local
        // flow অপ্রভাবিত থাকে (best-effort, local এই RPC-এর উপর নির্ভর করে না)।
        val withdrawId = "WID-${UUID.randomUUID().toString().replace("-", "").take(8).uppercase()}"
        var permanentFailure: Throwable? = null
        if (SupabaseAuthManager.currentUserId() == solver.id) {
            // [SUPABASE-MIGRATED - ধাপ ১৪.৫ (Kotlin wiring, উপ-ধাপ "ঘ")] উইথড্র সবসময় solver
            // context থেকেই হয় (এই ফাংশনের প্যারামিটারই `solver: UserEntity`) — তাই role সবসময়
            // "SOLVER" পাঠানো হচ্ছে (নতুন ৭-আর্গুমেন্ট RPC overload, p_role default 'SOLVER'-এর
            // সাথে সামঞ্জস্যপূর্ণ, শুধু এখন explicit)।
            SupabaseSyncManager.requestWithdrawal(
                amount = amount,
                method = method,
                accountNumber = accountNumber,
                bankName = bankName,
                branchName = branchName,
                accountHolderName = accountHolderName,
                role = role,
                clientWithdrawalId = withdrawId
            ).onSuccess { json ->
                val obj = json as? kotlinx.serialization.json.JsonObject
                val resultField = (obj?.get("result") as? kotlinx.serialization.json.JsonPrimitive)?.content
                val cloudId = (obj?.get("withdrawal_id") as? kotlinx.serialization.json.JsonPrimitive)?.content
                if (resultField != "OK" || cloudId != withdrawId) {
                    // [ফিক্স] cloudId এখন সবসময় withdrawId-এর সমান হওয়া উচিত (client-supplied
                    // id RPC গ্রহণ করেছে) — অমিল হলে শুধু লগ করা হলো, local id-ই টিকে থাকবে
                    // (আগের মতো cloudId দিয়ে override করা হচ্ছে না, কারণ override করাটাই
                    // মূল id-mismatch বাগের উৎস ছিল)।
                    Log.w("SomadhanRepo", "requestWithdrawal: Supabase dual-write non-OK/id-mismatch result: $resultField cloudId=$cloudId localId=$withdrawId")
                }
            }.onFailure { e ->
                // [Step 12.10d] server-এর স্থায়ী প্রত্যাখ্যান (KYC_REQUIRED, ROLE_INACTIVE, INSUFFICIENT_BALANCE...)
                // outbox-এ পাঠানো হয় না, আর local write-এর আগেই থামানো হয় (RPC আগে চলে বলে rollback লাগে না)।
                if (RpcErrorClassifier.isPermanent(e)) {
                    Log.e("SomadhanRepo", "requestWithdrawal: server permanently rejected (role=$role, withdrawId=$withdrawId): ${e.message} -- outbox-এ পাঠানো হয়নি, local write হয়নি")
                    permanentFailure = e
                    return@onFailure
                }
                Log.w("SomadhanRepo", "requestWithdrawal: Supabase dual-write failed (using local-generated id, local flow unaffected): ${e.message}")
                // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ২, id-mismatch ফিক্স পরে যোগ] paramsJson keys
                // "amount"/"method"/"accountNumber"/"bankName"/"branchName"/
                // "accountHolderName"/"role"/"clientWithdrawalId" -- OutboxRpcDispatcher.kt-এর
                // "request_withdrawal" branch-এর সাথে মিলছে (কমেন্টে যাচাই করা)।
                // "clientWithdrawalId" যোগ হওয়ায় retry পরে সফল হলে cloud row এই একই
                // `withdrawId` পাবে — আগের "এতিম cloud row" সমস্যা আর হবে না।
                enqueueOutboxRetry(
                    rpcName = "request_withdrawal",
                    params = kotlinx.serialization.json.JsonObject(
                        buildMap {
                            put("amount", kotlinx.serialization.json.JsonPrimitive(amount))
                            put("method", kotlinx.serialization.json.JsonPrimitive(method))
                            put("accountNumber", kotlinx.serialization.json.JsonPrimitive(accountNumber))
                            bankName?.let { put("bankName", kotlinx.serialization.json.JsonPrimitive(it)) }
                            branchName?.let { put("branchName", kotlinx.serialization.json.JsonPrimitive(it)) }
                            accountHolderName?.let { put("accountHolderName", kotlinx.serialization.json.JsonPrimitive(it)) }
                            put("role", kotlinx.serialization.json.JsonPrimitive(role))
                            put("clientWithdrawalId", kotlinx.serialization.json.JsonPrimitive(withdrawId))
                        }
                    ),
                    error = e
                )
            }
        }
        permanentFailure?.let { return Result.failure(Exception(RpcErrorClassifier.withdrawalUserMessage(it))) }
        val withdrawal = WithdrawalEntity(
            id = withdrawId,
            solverId = solver.id,
            solverName = solver.name,
            amount = amount,
            method = method,
            accountNumber = accountNumber,
            bankName = bankName,
            branchName = branchName,
            accountHolderName = accountHolderName,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        withdrawalDao.insertWithdrawal(withdrawal)

        // Deduct balance. INSTANT local debit (Room); the cloud side goes through the
        // request_withdrawal RPC dual-write further below, with outbox retry on failure
        // [Step 7.7 cleanup: pendingCloudSync on the TransactionEntity below is an unread
        // Firebase-era leftover, not the real retry mechanism]. This one runs on the requesting
        // user's own device, so the local balance is safe either way,
        // but without this the CLOUD copy of the balance could permanently under-count if the
        // one-shot increment failed -- visible on a different device or after a reinstall.
        // [ব্যালেন্স ফিক্স] সমাধানকারীর (solver) উইথড্র, balanceSolver mirror-ও আপডেট হয়।
        if (role == "SOLVER") userDao.deductBalanceForSolverRole(solver.id, amount) else userDao.deductBalanceForUserRole(solver.id, amount)
        val deductionTrx = TransactionEntity(
            id = "TRX_WD_DEDUCT_$withdrawId",
            problemId = "",
            problemTitle = "উইথড্র আবেদন — ব্যালেন্স কর্তন",
            userId = solver.id,
            solverId = "",
            grossAmount = amount,
            commissionPercent = 0.0,
            commissionAmount = 0.0,
            netAmount = -amount,
            type = "WITHDRAWAL_DEDUCTION",
            escrowId = withdrawId,
            pendingCloudSync = true,
            role = role
        )
        transactionDao.insertTransaction(deductionTrx)

        notificationDao.insertNotification(
            NotificationEntity(
                role = role,
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = solver.id,
                title = "উইথড্র রিকোয়েস্ট জমা হয়েছে",
                message = "৳${DistanceUtil.toBengaliDigits(amount.toInt().toString())} উত্তোলনের আবেদন জমা হয়েছে ($method: $accountNumber, উইথড্র আইডি: $withdrawId)। অ্যাডমিন অনুমোদন সাপেক্ষে টাকা পাঠানো হবে।",
                targetType = "balance",
                targetId = solver.id
            )
        )

        return Result.success(withdrawal)
    }

    suspend fun updateWithdrawalStatus(withdrawal: WithdrawalEntity, status: String, trxId: String? = null): WithdrawalUpdateResult {
        val currentWithdrawal = withdrawalDao.getWithdrawalById(withdrawal.id) ?: withdrawal
        val currentStatus = currentWithdrawal.status.trim().uppercase()
        val targetStatus = status.trim().uppercase()

        // Strict Final State Rule (Option 2):
        // COMPLETED and REJECTED are terminal states and cannot be transitioned to any other status.
        if (currentStatus == "COMPLETED" || currentStatus == "REJECTED") {
            if (currentStatus == "COMPLETED" && !trxId.isNullOrBlank() && currentWithdrawal.trxId != trxId) {
                // Allow updating TrxID reference for completed withdrawals
                val updated = currentWithdrawal.copy(trxId = trxId)
                withdrawalDao.updateWithdrawal(updated)
                logAdminAction(
                    actionType = "UPDATE_WITHDRAWAL_TRX_ID",
                    targetId = updated.id,
                    targetName = updated.solverName,
                    details = "TrxID পরিবর্তিত: $trxId"
                )
                return WithdrawalUpdateResult.Updated
            }
            return WithdrawalUpdateResult.GuardBlocked(
                "এই উইথড্র ইতিমধ্যে চূড়ান্ত অবস্থায় ($currentStatus) আছে, স্ট্যাটাস আর পরিবর্তন করা যাবে না।"
            )
        }

        // Only transitions originating from PENDING status are allowed
        if (currentStatus != "PENDING") {
            return WithdrawalUpdateResult.GuardBlocked(
                "এই উইথড্র বর্তমানে \"$currentStatus\" অবস্থায় আছে (PENDING না), তাই স্ট্যাটাস পরিবর্তন সম্ভব হয়নি।"
            )
        }

        // Find the effective user record in the local database
        val solverId = currentWithdrawal.solverId
        val targetUser = userDao.getUserById(solverId)
            ?: userDao.getUserByDisplayUid(solverId)
            ?: userDao.getUserByPhone(solverId)
            ?: userDao.getAllUsersList().firstOrNull { it.id == solverId || it.displayUid == solverId || it.phone == solverId || it.linkedAccountId == solverId }
        val effectiveUserId = targetUser?.id ?: solverId

        if (targetStatus == "REJECTED") {
            // Final Transition: PENDING -> REJECTED
            // Refund the requested amount back to user's wallet exactly once.
            // INSTANT local credit here (Room); the cloud side goes through the withdrawal-status
            // RPC dual-write further below, with outbox retry on failure [Step 7.7 cleanup:
            // pendingCloudSync on the TransactionEntity below is an unread Firebase-era
            // leftover, not the real retry mechanism]. This matters most exactly here: this
            // refund runs on the ADMIN's device for a DIFFERENT user's balance, so unlike a
            // self-initiated action, that user's own device has no local optimistic write to
            // fall back on if the cloud write is lost.
            //
            // A dedicated TransactionEntity is created for this (previously there was none at
            // all for a withdrawal-rejection refund) so it (a) is now covered by the outbox
            // retry like every other balance change, and (b) is now visible
            // to the user in their transaction history -- it wasn't before. Deterministic id
            // (based on the withdrawal's own id) makes a repeat call to this function for the
            // same withdrawal safely idempotent, same convention as TRX_SPLIT_/TRX_REFUND_.
            val withdrawalRefundTrxId = "TRX_WD_REFUND_${currentWithdrawal.id}"
            val alreadyRefunded = transactionDao.getTransactionById(withdrawalRefundTrxId) != null
            if (!alreadyRefunded) {
                // [ব্যালেন্স ফিক্স] withdrawal reject হলে টাকা solver-এর নিজের earning pool-এই
                // ফেরত যায় (solverId ভ্যারিয়েবল থেকেই স্পষ্ট এটা solver-এর উইথড্র)।
                // [Step 12.10d] refund সেই role-এর pool-এ ফেরত যায় যেখান থেকে কাটা হয়েছিল — role আসে withdrawal-এর
                // deduction transaction (`TRX_WD_DEDUCT_<id>`) থেকে (WithdrawalEntity-তে role কলাম নেই, তাই Room
                // migration লাগে না); না পেলে আগের আচরণ (SOLVER)। server `process_withdrawal` নিজে `v_wd.role` ধরে।
                val withdrawalRole = transactionDao.getTransactionById("TRX_WD_DEDUCT_${currentWithdrawal.id}")
                    ?.role?.takeIf { it == "USER" || it == "SOLVER" } ?: "SOLVER"
                if (withdrawalRole == "USER") userDao.addBalanceForUserRole(effectiveUserId, currentWithdrawal.amount)
                else userDao.addBalanceForSolverRole(effectiveUserId, currentWithdrawal.amount)
                val refundTrx = TransactionEntity(
                    id = withdrawalRefundTrxId,
                    problemId = "",
                    problemTitle = "উইথড্র আবেদন বাতিল রিফান্ড",
                    userId = effectiveUserId,
                    solverId = "",
                    grossAmount = currentWithdrawal.amount,
                    commissionPercent = 0.0,
                    commissionAmount = 0.0,
                    netAmount = currentWithdrawal.amount,
                    type = "WITHDRAWAL_REFUND",
                    pendingCloudSync = true,
                    role = withdrawalRole
                )
                transactionDao.insertTransaction(refundTrx)
            } else {
                Log.w("SomadhanRepo", "updateWithdrawalStatus(REJECTED): skipped — withdrawal ${currentWithdrawal.id} already refunded, refusing to refund again")
            }

            val updated = currentWithdrawal.copy(status = "REJECTED", rejectionReason = trxId)
            withdrawalDao.updateWithdrawal(updated)

            // [SUPABASE-MIGRATED - ধাপ ৯] `process_withdrawal` RPC নিজেই server-side
            // is_admin(auth.uid()) চেক করে, তাই আলাদা করে client-side role check করা হয়নি — এখানে
            // শুধু কোনো Supabase session আছে কিনা দেখা হচ্ছে (demo admin login এখনো real session
            // তৈরি করে না, ধাপ ১৪ নোট অনুযায়ী — তখন এই কলটা এমনিতেই স্কিপ হবে)। **জানা
            // সীমাবদ্ধতা**: শুধু তখনই কাজ করবে যখন এই withdrawal-টা requestWithdrawal()-এর
            // dual-write দিয়েই তৈরি হয়েছিল (তাই local id == cloud withdrawal_id) — নাহলে RPC
            // `WITHDRAWAL_NOT_FOUND` রিটার্ন করবে, যা শুধু log হয়, local flow অপ্রভাবিত।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.processWithdrawal(currentWithdrawal.id, "REJECT", trxId)
                    .onSuccess { json ->
                        val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                            ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        if (resultField != null && resultField != "OK") {
                            Log.w("SomadhanRepo", "updateWithdrawalStatus(REJECTED): Supabase dual-write non-OK result for ${currentWithdrawal.id}: $resultField (local flow unaffected)")
                        }
                    }
                    .onFailure { e ->
                        Log.w("SomadhanRepo", "updateWithdrawalStatus(REJECTED): Supabase dual-write failed for ${currentWithdrawal.id} (local flow unaffected): ${e.message}")
                        // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ২] paramsJson keys "withdrawalId"/"action"/
                        // "trxId" -- OutboxRpcDispatcher.kt-এর "process_withdrawal" branch-এর
                        // সাথে মিলছে। ⚠️ উপরের কমেন্টে উল্লিখিত একই "জানা সীমাবদ্ধতা" এখানেও
                        // প্রযোজ্য -- যদি এই withdrawal-টা মূলত requestWithdrawal()-এর
                        // dual-write fail হওয়ার কারণে তৈরি হয়ে থাকে (local id ≠ cloud id),
                        // তাহলে এই outbox retry-ও বারবার WITHDRAWAL_NOT_FOUND পেয়ে শেষে
                        // FAILED_PERMANENT হয়ে যাবে -- এটা ক্ষতিকর না (local REJECTED status +
                        // রিফান্ড ইতিমধ্যে হয়ে গেছে, এই RPC শুধু cloud mirror), শুধু ব্যর্থ
                        // retry attempt।
                        enqueueOutboxRetry(
                            rpcName = "process_withdrawal",
                            params = kotlinx.serialization.json.JsonObject(
                                buildMap {
                                    put("withdrawalId", kotlinx.serialization.json.JsonPrimitive(currentWithdrawal.id))
                                    put("action", kotlinx.serialization.json.JsonPrimitive("REJECT"))
                                    trxId?.let { put("trxId", kotlinx.serialization.json.JsonPrimitive(it)) }
                                }
                            ),
                            error = e
                        )
                    }
            }

            notificationDao.insertNotification(
                NotificationEntity(
                    role = "SOLVER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = effectiveUserId,
                    title = "উইথড্র আবেদন বাতিল ও ব্যালেন্স রিফান্ড",
                    message = "আপনার ৳${DistanceUtil.toBengaliDigits(currentWithdrawal.amount.toInt().toString())} উত্তোলনের আবেদন বাতিল করা হয়েছে এবং অর্থ আপনার ব্যালেন্সে ফিরিয়ে দেওয়া হয়েছে।" +
                        (if (!trxId.isNullOrBlank()) " কারণ: $trxId" else ""),
                    targetType = "balance",
                    targetId = effectiveUserId
                )
            )

            logAdminAction(
                actionType = "REJECT_WITHDRAWAL",
                targetId = currentWithdrawal.id,
                targetName = currentWithdrawal.solverName,
                details = "উইথড্র বাতিল ও ৳${currentWithdrawal.amount.toInt()} রিফান্ড করা হয়েছে${if (!trxId.isNullOrBlank()) ", কারণ: $trxId" else ""}"
            )
            return WithdrawalUpdateResult.Updated
        } else if (targetStatus == "COMPLETED") {
            // Final Transition: PENDING -> COMPLETED
            // Balance was already deducted upon withdrawal request creation
            val updated = currentWithdrawal.copy(status = "COMPLETED", trxId = trxId, rejectionReason = null)
            withdrawalDao.updateWithdrawal(updated)

            // [SUPABASE-MIGRATED - ধাপ ৯] উপরের REJECTED ব্রাঞ্চের কমেন্টে ব্যাখ্যা করা একই
            // প্যাটার্ন ও একই জানা সীমাবদ্ধতা প্রযোজ্য।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.processWithdrawal(currentWithdrawal.id, "COMPLETE", trxId)
                    .onSuccess { json ->
                        val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                            ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        if (resultField != null && resultField != "OK" && resultField != "TRX_ID_UPDATED") {
                            Log.w("SomadhanRepo", "updateWithdrawalStatus(COMPLETED): Supabase dual-write non-OK result for ${currentWithdrawal.id}: $resultField (local flow unaffected)")
                        }
                    }
                    .onFailure { e ->
                        Log.w("SomadhanRepo", "updateWithdrawalStatus(COMPLETED): Supabase dual-write failed for ${currentWithdrawal.id} (local flow unaffected): ${e.message}")
                        // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ২] REJECTED ব্রাঞ্চের সাথে অভিন্ন প্যাটার্ন ও
                        // একই "জানা সীমাবদ্ধতা" (উপরে দেখুন) -- শুধু action="COMPLETE"।
                        enqueueOutboxRetry(
                            rpcName = "process_withdrawal",
                            params = kotlinx.serialization.json.JsonObject(
                                buildMap {
                                    put("withdrawalId", kotlinx.serialization.json.JsonPrimitive(currentWithdrawal.id))
                                    put("action", kotlinx.serialization.json.JsonPrimitive("COMPLETE"))
                                    trxId?.let { put("trxId", kotlinx.serialization.json.JsonPrimitive(it)) }
                                }
                            ),
                            error = e
                        )
                    }
            }

            val ratePer100 = platformSettingDao.getSetting("rep_rate_withdrawal_per_100")?.toDoubleOrNull() ?: 0.1
            val withdrawalScore = (currentWithdrawal.amount / 100.0) * ratePer100
            val withdrawalCap = platformSettingDao.getSetting("rep_cap_daily_withdrawal")?.toDoubleOrNull() ?: 2.0
            applyCappedPerEventReputation(
                userId = effectiveUserId,
                eventType = "WITHDRAWAL_COMPLETED",
                defaultRawScore = withdrawalScore,
                customScore = withdrawalScore,
                defaultDailyCap = withdrawalCap,
                problemId = null,
                note = "সফলভাবে উইথড্র সম্পন্ন করেছেন (৳${DistanceUtil.toBengaliDigits(currentWithdrawal.amount.toInt().toString())})"
            )

            notificationDao.insertNotification(
                NotificationEntity(
                    role = "SOLVER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = effectiveUserId,
                    title = "টাকা পাঠানো সম্পন্ন! ৳${DistanceUtil.toBengaliDigits(currentWithdrawal.amount.toInt().toString())} 💸",
                    message = "আপনার ৳${DistanceUtil.toBengaliDigits(currentWithdrawal.amount.toInt().toString())} উত্তোলনের পেমেন্ট (${currentWithdrawal.method}) সম্পন্ন হয়েছে। TrxID: ${trxId ?: "N/A"}",
                    targetType = "balance",
                    targetId = effectiveUserId
                )
            )

            logAdminAction(
                actionType = "COMPLETE_WITHDRAWAL",
                targetId = currentWithdrawal.id,
                targetName = currentWithdrawal.solverName,
                details = "উইথড্র অনুমোদন সম্পন্ন, পরিমাণ: ৳${currentWithdrawal.amount.toInt()}, মেথড: ${currentWithdrawal.method}${if (trxId != null) ", TrxID: $trxId" else ""}"
            )
            return WithdrawalUpdateResult.Updated
        }
        // targetStatus না REJECTED, না COMPLETED -- অজানা/অসমর্থিত স্ট্যাটাস, কিছুই বদলায়নি।
        return WithdrawalUpdateResult.GuardBlocked(
            "\"$status\" একটা অসমর্থিত উইথড্র স্ট্যাটাস (শুধু REJECTED/COMPLETED সমর্থিত)।"
        )
    }

    suspend fun adminUpdateWithdrawalTrxId(withdrawalId: String, newTrxId: String) {
        val existing = withdrawalDao.getWithdrawalById(withdrawalId) ?: return
        val updated = existing.copy(trxId = newTrxId)
        withdrawalDao.updateWithdrawal(updated)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮] `withdrawals` টেবিলে কোনো admin-write RLS পলিসি নেই (verify করা
        // হয়েছে) -- তাই নতুন RPC `admin_update_withdrawal_trx_id` লাগলো, `process_withdrawal()` থেকে আলাদা --
        // শুধু trx_id সংশোধন করে, status/money-movement স্পর্শ করে না।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminUpdateWithdrawalTrxId(withdrawalId, newTrxId).onFailure { e ->
                Log.w("SomadhanRepo", "adminUpdateWithdrawalTrxId: Supabase dual-write failed for $withdrawalId (local flow unaffected): ${e.message}")
                // [OUTBOX WIRE - Step 12.6] paramsJson key "withdrawalId","newTrxId": String --
                // OutboxRpcDispatcher.kt-এর "admin_update_withdrawal_trx_id" branch-এর সাথে অক্ষরে
                // অক্ষরে মিলছে। idempotency (migration step32_8_admin_update_withdrawal_trx_id.sql
                // বডি থেকে যাচাই): is_admin() গার্ড, শুধু trx_id SET (কোনো balance/escrow ছোঁয় না) --
                // ২য়বার চললে একই মান আবার SET হয়, কোনো ক্ষতি নেই। IDEMPOTENT, BLOCKED না।
                enqueueOutboxRetry(
                    rpcName = "admin_update_withdrawal_trx_id",
                    params = kotlinx.serialization.json.JsonObject(
                        buildMap {
                            put("withdrawalId", kotlinx.serialization.json.JsonPrimitive(withdrawalId))
                            put("newTrxId", kotlinx.serialization.json.JsonPrimitive(newTrxId))
                        }
                    ),
                    error = e
                )
            }
        }

        logAdminAction(
            actionType = "UPDATE_WITHDRAWAL_TRX_ID",
            targetId = updated.id,
            targetName = updated.solverName,
            details = "TrxID পরিবর্তিত: $newTrxId"
        )
    }

    // ---------------- TRANSACTIONS & SETTINGS ----------------

    fun getAllGatewayPayments(): Flow<List<GatewayPaymentEntity>> = gatewayPaymentDao.getAllPaymentsFlow()

    fun getGatewayPaymentsForUser(userId: String): Flow<List<GatewayPaymentEntity>> = gatewayPaymentDao.getPaymentsForUserFlow(userId)

    suspend fun recordGatewayPayment(
        userId: String,
        amount: Double,
        gateway: String,
        gatewayTrxId: String,
        senderPhone: String = "",
        purpose: String = "ESCROW_PAYMENT",
        problemId: String = "",
        problemTitle: String = "",
        status: String = "SUCCESS",
        note: String = ""
    ): GatewayPaymentEntity {
        val user = userDao.getUserById(userId)
        val payment = GatewayPaymentEntity(
            id = "GW_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().take(6).uppercase(),
            gatewayTrxId = gatewayTrxId.ifBlank { "TRX" + System.currentTimeMillis().toString().takeLast(8).uppercase() },
            userId = userId,
            userName = user?.name ?: "গ্রাহক",
            userPhone = senderPhone.ifBlank { user?.phone ?: "" },
            amount = amount,
            gateway = gateway,
            purpose = purpose,
            problemId = problemId,
            problemTitle = problemTitle,
            status = status,
            note = note,
            timestamp = System.currentTimeMillis()
        )
        gatewayPaymentDao.insertPayment(payment)

        // [বাগফিক্স, ব্যবহারকারীর রিপোর্ট: "৫০০ টাকা ডিপোজিট করলাম, রোল সুইচ করলাম, ব্যালেন্স ০
        // হয়ে গেল, ট্রানজ্যাকশন হিস্ট্রিতে রিচার্জ দেখাচ্ছে কিন্তু ব্যালেন্স ০"]
        // আগে এখানে `record_gateway_payment_log` RPC কল করে এই একই `gatewayTrxId` দিয়ে cloud
        // `gateway_payments` টেবিলে একটা log-only row আগেভাগেই ইনসার্ট করে দেওয়া হতো। এই
        // ফাংশনের একমাত্র caller `depositMoneyViaGateway()` তার ঠিক পরেই আসল টাকা-জমাদানকারী
        // `requestWalletDeposit()` RPC কল করে — কিন্তু সেই RPC-র প্রথম চেকই হলো
        // `if exists (select 1 from gateway_payments where gateway_trx_id = p_gateway_trx_id)
        // then return 'ALREADY_SUBMITTED'` (duplicate-submit protection)। যেহেতু log-only
        // insert-টা একই gateway_trx_id দিয়ে আগেই সেই row বানিয়ে ফেলত, `requestWalletDeposit()`
        // প্রতিবারই সেই guard-এ ধরা পড়ে সাথে সাথে `ALREADY_SUBMITTED` রিটার্ন করত — balance
        // আপডেট বা transactions insert (দুটোই সেই guard-এর পরে থাকে) কখনো চলতই না। Kotlin কোড
        // `ALREADY_SUBMITTED`-কে প্রত্যাশিত/সফল ফলাফল হিসেবেই ট্রিট করে (কোনো warning/error log
        // হয় না), তাই silently টাকা যোগ না হয়েই থেকে যেত — অথচ gateway_payments-এ (log-only
        // row-এর কারণে) status=SUCCESS দেখাত, এবং local balance ঠিকই বেড়েছিল বলে অ্যাপে তৎক্ষণাৎ
        // সব স্বাভাবিক লাগত। পরে role switch করলে `switchRoleInPlace()` cloud থেকে (কখনো না-বাড়া)
        // আসল ০ ব্যালেন্স পড়ে local ব্যালেন্স ওভাররাইট করে দিত — এটাই "ডিপোজিট করা টাকা হারিয়ে
        // যাওয়া" উপসর্গ।
        //
        // ফিক্স: এই log-only dual-write সম্পূর্ণ বাদ দেওয়া হলো। `requestWalletDeposit()` RPC
        // নিজেই তার নিজস্ব `GWPAY_...` id দিয়ে gateway_payments row ইনসার্ট করে (একই তথ্য, বরং
        // বেশি — balance আর transactions-সহ একসাথে, একই atomic RPC-তে), তাই এই আলাদা প্রি-এম্পটিভ
        // log write অপ্রয়োজনীয় ছিল এবং সরাসরি ক্ষতিকর প্রমাণিত হলো।
        return payment
    }

    suspend fun depositMoneyViaGateway(
        userId: String,
        amount: Double,
        gateway: String,
        gatewayTrxId: String,
        senderPhone: String = "",
        note: String = ""
    ): Result<GatewayPaymentEntity> {
        val user = userDao.getUserById(userId) ?: return Result.failure(Exception("ব্যবহারকারী পাওয়া যায়নি"))
        if (amount <= 0) return Result.failure(Exception("সঠিক টাকার পরিমাণ দিন"))

        // Idempotency guard: if this exact gateway transaction ID was already processed
        // (e.g. user double-tapped submit, or a network retry resent the same request),
        // do not add the balance a second time.
        if (gatewayTrxId.isNotBlank()) {
            val existingPayment = gatewayPaymentDao.getPaymentByGatewayTrxId(gatewayTrxId)
            if (existingPayment != null) {
                return Result.failure(Exception("এই লেনদেনটি (TrxID: $gatewayTrxId) ইতিমধ্যে প্রসেস করা হয়েছে। একই টাকা দুইবার যোগ করা হয়নি।"))
            }
        }

        // Atomic, INSTANT local balance update — never read the old balance and write a full
        // copy back; Room's UPDATE (balance = balance + amount) can never lose a concurrent
        // balance change (escrow release, admin adjustment, withdrawal, etc.) the way a
        // read-modify-write + full-object overwrite would. The cloud side is pushed via the
        // deposit_money_via_gateway RPC dual-write further below, with outbox retry on failure
        // [Step 7.7 cleanup: the TransactionEntity's pendingCloudSync flag below is an unread
        // Firebase-era leftover, not the real retry mechanism] -- instead of the old
        // incrementUserBalance() fire-and-forget, which had no retry if its one attempt failed.
        // [ব্যালেন্স ফিক্স] ওয়ালেট টপ-আপ সবসময় user pool-এ যায়।
        userDao.addBalanceForUserRole(userId, amount)

        val payment = recordGatewayPayment(
            userId = userId,
            amount = amount,
            gateway = gateway,
            gatewayTrxId = gatewayTrxId,
            senderPhone = senderPhone,
            purpose = "WALLET_DEPOSIT",
            status = "SUCCESS",
            note = note.ifBlank { "ওয়ালেট ব্যালেন্স রিচার্জ (টপ-আপ)" }
        )

        // [SUPABASE-MIGRATED - ধাপ ১২ ফিক্স] এটাই [com.example.data.payment.DemoPaymentGatewayProvider]
        // এর "সফল payment" callback থেকে call হওয়া ফাংশন (payment gateway UI এখনো demo/mock,
        // কিন্তু এই RPC কলটা real)। **আগে এখানে `deposit_money_via_gateway` RPC কল করা হতো, কিন্তু
        // লাইভ প্রজেক্টে সেই RPC-তে anon/authenticated কারো EXECUTE গ্র্যান্টই নেই
        // (`has_function_privilege` দিয়ে সরাসরি Supabase MCP-তে যাচাই করা হয়েছে — দুটোই `false`),
        // তাই প্রতিবার এই কলটা PERMISSION_DENIED দিয়ে ব্যর্থ হচ্ছিল আর এই পথের কোনো deposit কখনো
        // Supabase ledger-এ পৌঁছাচ্ছিল না। ফিক্স: এর বদলে `request_wallet_deposit` RPC কল করা
        // হচ্ছে — এটা auth.uid()-ভিত্তিক (তাই p_user_id লাগে না), anon/authenticated উভয়েরই
        // EXECUTE গ্র্যান্ট আছে (যাচাই করা হয়েছে), gateway_trx_id দিয়ে ইতিমধ্যে-জমা দেখলে
        // ALREADY_SUBMITTED রিটার্ন করে (duplicate-submit সুরক্ষা, ঠিক উপরের local idempotency
        // guard-এর মতোই), আর সফল হলে নিজেই `notifications` টেবিলে "রিচার্জ সফল/অনুরোধ জমা হয়েছে"
        // notification insert করে (তাই এখানে আলাদা করে notification dual-write লাগে না)। এই RPC
        // `platform_settings.gateway_auto_approve_deposits` অনুযায়ী `OK` (balance তৎক্ষণাৎ যোগ) বা
        // `PENDING_APPROVAL` (admin approve করার অপেক্ষা) রিটার্ন করতে পারে — দুটোই স্বাভাবিক
        // ফলাফল, error না। **জানা সীমাবদ্ধতা**: local `payment.id` (`GW_...`) আর cloud
        // `gateway_payments.id` (`GWPAY_...`) আলাদা format/id — id-সমন্বয় করা হয়নি, শুধু
        // ledger/history হিসেবে দুই পাশে দুটো আলাদা কিন্তু সমতুল্য রেকর্ড থাকবে। এই dual-write
        // সবসময় best-effort — ব্যর্থ/non-OK হলে শুধু log হবে, local balance/transaction flow
        // সম্পূর্ণ অপ্রভাবিত থাকে।
        // [ফিক্স] `userId` এখানে dual-row architecture-এর যেকোনো role-row-এর (root বা linked
        // "SOLVER_xxxx"/"USER_xxxx") local id হতে পারে, কিন্তু cloud-এ শুধু root row-এরই
        // (id = auth.uid()) real Supabase row/session আছে — linked local row-গুলোর জন্য আলাদা
        // কোনো cloud row নেই (ঠিক `switchRoleInPlace()`-এর মতোই, দ্রষ্টব্য তার rootAccountId
        // ব্যবহার)। আগে এখানে সরাসরি `currentUserId() == userId` চেক করা হতো, যেটা user বর্তমানে
        // linked (non-root) role-এ থাকলে সবসময় false হতো — ফলে এই পুরো cloud dual-write ব্লকটাই
        // silently skip হয়ে যেত, RPC কলই হতো না (এমনকি কোনো error/log ছাড়াই)। তাই local balance
        // তৎক্ষণাৎ বাড়লেও cloud balance_user/balance_solver কখনো বাড়েনি, আর পরে role switch করলে
        // `switch_role_get_or_create_linked_profile` cloud থেকে সেই (অপরিবর্তিত) ০ balance পড়ে
        // local value overwrite করে দিত — এটাই "deposit করা balance হারিয়ে যাওয়া" উপসর্গের আসল
        // কারণ ছিল (escrow_id FK constraint বাগ ফিক্স হওয়ার পরেও)।
        val rootAccountId = user.linkedAccountId?.takeIf { it.isNotBlank() } ?: user.id
        // [ধাপ ৩ — TransactionEntity.role wiring] আগে এই ভ্যারিয়েবলটা নিচের if-ব্লকের ভেতরেই
        // declare হতো (শুধু RPC কলের জন্য), তাই নিচের TransactionEntity তৈরির সাইটে এটা scope-এর
        // বাইরে ছিল। এখন if-ব্লকের বাইরে তোলা হলো (একই মান, একই লজিক) যাতে দুই জায়গাতেই (RPC dual-
        // write + local TransactionEntity.role) একই ভ্যারিয়েবল পুনর্ব্যবহার করা যায় — কোনো নতুন
        // ডুপ্লিকেট গণনা তৈরি হলো না, বরং একটা সম্ভাব্য future-divergence এড়ানো হলো।
        val depositRole = if (user.role == "SOLVER") "SOLVER" else "USER"
        if (SupabaseAuthManager.currentUserId() == rootAccountId) {
            // [SUPABASE-MIGRATED - ধাপ ১৪.৫ (Kotlin wiring, উপ-ধাপ "ঘ")] ওয়ালেট ডিপোজিট user ও
            // solver উভয় role থেকেই হতে পারে — উপরে fetch করা `user` (dual-row architecture-এ
            // এই userId-টাই একটা নির্দিষ্ট role-এর row) থেকে সরাসরি role জানা যায়।
            SupabaseSyncManager.requestWalletDeposit(
                amount = amount,
                gateway = gateway,
                gatewayTrxId = payment.gatewayTrxId,
                senderPhone = senderPhone,
                note = note,
                role = depositRole
            ).onSuccess { json ->
                val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                    ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                if (resultField != null && resultField != "OK" && resultField != "PENDING_APPROVAL" && resultField != "ALREADY_SUBMITTED") {
                    Log.w("SomadhanRepo", "depositMoneyViaGateway: Supabase dual-write unexpected result for ${payment.id}: $resultField (local flow unaffected)")
                }
            }.onFailure { e ->
                Log.w("SomadhanRepo", "depositMoneyViaGateway: Supabase dual-write failed for ${payment.id} (local flow unaffected): ${e.message}")
                // [Step 12.8b] retry -- server-side auth guard (p_expected_user_id) apply হওয়ার পরেই যোগ।
                // expectedUserId = rootAccountId (cloud auth.uid(), deposit-কারী), local linked-row id নয়:
                // replay-এর সময় ভিন্ন user লগইন থাকলে RPC NOT_AUTHORIZED দেয়, ভুল ওয়ালেটে টাকা যায় না।
                // paramsJson keys: amount (Double), gateway, gatewayTrxId, senderPhone, note, role, expectedUserId --
                // OutboxRpcDispatcher-এর request_wallet_deposit branch-এর সাথে অক্ষরে অক্ষরে মিলছে।
                enqueueOutboxRetry(
                    rpcName = "request_wallet_deposit",
                    params = kotlinx.serialization.json.JsonObject(
                        mapOf(
                            "amount" to kotlinx.serialization.json.JsonPrimitive(amount),
                            "gateway" to kotlinx.serialization.json.JsonPrimitive(gateway),
                            "gatewayTrxId" to kotlinx.serialization.json.JsonPrimitive(payment.gatewayTrxId),
                            "senderPhone" to kotlinx.serialization.json.JsonPrimitive(senderPhone),
                            "note" to kotlinx.serialization.json.JsonPrimitive(note),
                            "role" to kotlinx.serialization.json.JsonPrimitive(depositRole),
                            "expectedUserId" to kotlinx.serialization.json.JsonPrimitive(rootAccountId)
                        )
                    ),
                    error = e
                )
            }
        }

        // Insert TransactionEntity for wallet ledger / history
        val trx = TransactionEntity(
            id = "TRX_DEP_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6).uppercase()}",
            problemId = "",
            problemTitle = "ওয়ালেট রিচার্জ ($gateway)",
            userId = userId,
            solverId = "",
            grossAmount = amount,
            commissionPercent = 0.0,
            commissionAmount = 0.0,
            netAmount = amount,
            timestamp = System.currentTimeMillis(),
            baseAmount = amount,
            extraAmount = 0.0,
            baseCommissionAmount = 0.0,
            extraCommissionAmount = 0.0,
            extraCommissionApplied = false,
            wasFreeQuotaJob = false,
            type = "WALLET_DEPOSIT",
            escrowId = payment.id,
            pendingCloudSync = true,
            role = depositRole
        )
        transactionDao.insertTransaction(trx)
        // [Step 7.7 cleanup] pendingCloudSync above is a Firebase-era leftover, not read by
        // anything; the actual (retrying) cloud balance increment for this deposit is the
        // Supabase RPC dual-write + outbox retry, see the comment above this transaction.
        // Note trx.type is "WALLET_DEPOSIT", not "REFUND", so the escrow-touch part of that
        // function is skipped -- correct here since escrowId above is a GatewayPaymentEntity id,
        // not a real escrow doc.

        // Notification for user
        val formattedAmt = DistanceUtil.toBengaliDigits(DistanceUtil.roundTaka(amount))
        val notif = NotificationEntity(
            role = depositRole,
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = "ওয়ালেটে টাকা যোগ হয়েছে 💳",
            message = "$gateway এর মাধ্যমে আপনার ওয়ালেটে $formattedAmt টাকা সফলভাবে যোগ হয়েছে। (TrxID: ${payment.gatewayTrxId})",
            targetType = "balance",
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(notif)

        logAdminAction(
            actionType = "WALLET_DEPOSIT",
            targetId = payment.id,
            targetName = user.name,
            details = "$gateway থেকে ৳$amount ডিপোজিট (TrxID: ${payment.gatewayTrxId})"
        )

        return Result.success(payment)
    }

    suspend fun adminUpdateGatewayPaymentStatus(paymentId: String, newStatus: String) {
        gatewayPaymentDao.updateStatus(paymentId, newStatus)
        val p = gatewayPaymentDao.getPaymentById(paymentId)
        if (p != null) {

            // [SUPABASE-MIGRATED - ধাপ ৬] আগে এখানে `admin_confirm_gateway_deposit` RPC dual-write
            // করার চেষ্টা হতো, কিন্তু ট্রেস করে (Supabase MCP দিয়ে লাইভ সোর্স পড়ে) নিশ্চিত হওয়া গেছে
            // এটা কখনোই real কিছু করত না -- `recordGatewayPayment()` সবসময় status="SUCCESS" দিয়ে
            // cloud-এ dual-write করে (কোনো call-site কখনো "PENDING" পাস করে না), তাই RPC হয় ইতিমধ্যেই
            // SUCCESS পেয়ে `ALREADY_PROCESSED` রিটার্ন করত (কোনো real change ছাড়াই), অথবা সেই creation
            // dual-write ব্যর্থ হয়ে থাকলে `PAYMENT_NOT_FOUND`। "PENDING"/"REFUNDED" status-দুটোর জন্য
            // RPC কলই হতো না (action mapping-এ null)। প্রকৃত "PENDING deposit" workflow
            // (`request_wallet_deposit()`, আলাদা `GWPAY_...` id) সম্পূর্ণ আলাদা আর local-এ
            // representation-ই নেই। তাই এই ফাংশনটা এখন থেকে ইচ্ছাকৃতভাবে pure local audit/bookkeeping
            // status-editor (৪-state: SUCCESS/PENDING/FAILED/REFUNDED) -- কোনো cloud dual-write করে না।
            logAdminAction(
                actionType = "GATEWAY_STATUS_UPDATE",
                targetId = p.id,
                targetName = p.gatewayTrxId,
                details = "স্ট্যাটাস পরিবর্তন: $newStatus (${p.gateway})"
            )
        }
    }

    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    suspend fun getAllTransactionsPage(limit: Int, offset: Int): List<TransactionEntity> =
        transactionDao.getAllTransactionsPage(limit, offset)

    fun getTransactionsForUser(userId: String): Flow<List<TransactionEntity>> = transactionDao.getTransactionsForUser(userId)
    suspend fun getTransactionsForUserPage(userId: String, limit: Int, offset: Int): List<TransactionEntity> =
        transactionDao.getTransactionsForUserPage(userId, limit, offset)

    fun getTransactionsForSolver(solverId: String): Flow<List<TransactionEntity>> = transactionDao.getTransactionsForSolver(solverId)
    suspend fun getTransactionsForSolverPage(solverId: String, limit: Int, offset: Int): List<TransactionEntity> =
        transactionDao.getTransactionsForSolverPage(solverId, limit, offset)

    fun getEscrowsForUser(userId: String): Flow<List<EscrowEntity>> = escrowDao.getEscrowsForUser(userId)

    fun getEscrowsForSolver(solverId: String): Flow<List<EscrowEntity>> = escrowDao.getEscrowsForSolver(solverId)

    fun getAllEscrows(): Flow<List<EscrowEntity>> = escrowDao.getAllEscrows()

    fun getEscrowByProblemIdFlow(problemId: String): Flow<EscrowEntity?> = escrowDao.getEscrowByProblemIdFlow(problemId)

    fun getAllSettings(): Flow<List<PlatformSettingEntity>> = platformSettingDao.getAllSettings()

    // [ধাপ ৩৩.১] `updateSetting(key, value)` (আগে এখানে ছিল) সম্পূর্ণ ডিলিট করা হলো -- `grep`
    // যাচাই করে নিশ্চিত হওয়া গেছে পুরো কোডবেসে এর ০ caller ছিল (dead code, `updatePlatformSetting`
    // নিচেই একই কাজ করে এবং সক্রিয়ভাবে ব্যবহৃত)।

    suspend fun updatePlatformSetting(key: String, value: String): PlatformSettingUpdateResult {
        val entity = PlatformSettingEntity(key, value)
        platformSettingDao.insertSetting(entity)
        // [ধাপ ৩৩.১] Supabase বাদ পড়ে গিয়েছিল যদিও `SupabaseSyncManager.upsertPlatformSetting()`
        // আগে থেকেই বানানো ও `AdminCredentials.kt`-তে ব্যবহৃত (একই established প্যাটার্ন এখানে
        // অনুসরণ করা হলো)। `platform_settings`-এর UPDATE RLS qual=true (সবার জন্য উন্মুক্ত,
        // non-sensitive config-ই এখানে যায়) -- তাই currentUserId() গার্ডের দরকার নেই।
        // [Somadhan Bug-Fix Step 6 — গ্রুপ ৩.১a] আগে dual-write ব্যর্থতা শুধু log হতো, caller
        // জানতে পারত না (local write সবসময় হতো বলে caller-কে ব্লক করা হয় না -- শুধু জানানো হয়)।
        val cloudResult: PlatformSettingUpdateResult = try {
            SupabaseSyncManager.upsertPlatformSetting(key, value).fold(
                onSuccess = { PlatformSettingUpdateResult.Updated },
                onFailure = { e ->
                    Log.w("SomadhanRepo", "updatePlatformSetting: Supabase dual-write failed for $key (local flow unaffected): ${e.message}")
                    PlatformSettingUpdateResult.CloudSyncFailed(e.message ?: "অজানা ত্রুটি")
                }
            )
        } catch (e: Exception) {
            Log.w("SomadhanRepo", "updatePlatformSetting: Supabase dual-write threw for $key (local flow unaffected): ${e.message}")
            PlatformSettingUpdateResult.CloudSyncFailed(e.message ?: "অজানা ত্রুটি")
        }
        logAdminAction(
            actionType = "UPDATE_SETTING",
            targetId = key,
            targetName = key,
            details = "মান পরিবর্তন: $value"
        )
        return cloudResult
    }

    /**
     * [Offline Action Gating ধাপ ২, ইনভিওলেবল রুল ৪] `strict_offline_block` platform_settings
     * key-এর cloud মান টেনে local Room-এ cache করে। এই key-টার জন্য (আগে থেকে থাকা অন্য
     * platform_settings key-গুলোর মতোই) কোনো bulk cloud->Room pull নেই (দেখুন উপরে
     * `_platformSettingsSyncPhase`-এর KDoc -- এটা একটা pre-existing, স্কোপের বাইরের gap), তাই
     * অন্য ডিভাইসে (বা এই ডিভাইসেই fresh install-এ) admin panel থেকে করা toggle পরিবর্তন
     * নিজে থেকে আসত না -- MainActivity-এর অফলাইন-ব্লকিং সিদ্ধান্তের জন্য এই key-টা critical
     * বলে এখানে আলাদাভাবে (bulk pull না বাড়িয়ে) ছোট, targeted single-row pull যোগ করা হলো।
     * Cloud read ব্যর্থ হলে (network নেই ইত্যাদি) local cache-এ যা আগে থেকে আছে (বা কিছু না
     * থাকলে caller-সাইড ডিফল্ট "true") সেটাই বহাল থাকে -- silent, non-blocking, ঠিক
     * updatePlatformSetting()-এর dual-write ব্যর্থতার প্যাটার্নের মতোই।
     */
    suspend fun syncStrictOfflineBlockSettingFromCloud() {
        try {
            SupabaseSyncManager.getPlatformSetting("strict_offline_block")
                .onSuccess { cloudValue ->
                    if (cloudValue != null) {
                        platformSettingDao.insertSetting(PlatformSettingEntity("strict_offline_block", cloudValue))
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "syncStrictOfflineBlockSettingFromCloud: cloud pull failed (local cache unaffected): ${e.message}")
                }
        } catch (e: Exception) {
            Log.w("SomadhanRepo", "syncStrictOfflineBlockSettingFromCloud: threw (local cache unaffected): ${e.message}")
        }
    }

    suspend fun deleteUser(userId: String) {
        val user = userDao.getUserById(userId)
        userDao.deleteUser(userId)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৮] Local Room/Firestore পাশে hard-delete অক্ষত থাকছে (উপরের
        // দুই লাইন অপরিবর্তিত)। Supabase পাশে এটা soft-delete flag (`is_deleted`) সেট করে (RPC
        // `admin_soft_delete_user`) -- literal hard-delete `users(id)`-এর ওপর থাকা সবকটা FK (NO ACTION)
        // ভায়োলেট করবে বলে এই session-এ verify করা হয়েছে। **ইচ্ছাকৃতভাবে এই flag লগইন ব্লক করে না বা
        // auth.users থেকে সরায় না -- এই দুটো সিদ্ধান্তই ইচ্ছাকৃতভাবে খোলা রাখা হলো, ব্যবহারকারীর সিদ্ধান্তের
        // অপেক্ষায় (MIGRATION_PROGRESS.md-এ নথিভুক্ত)।**
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminSoftDeleteUser(userId).onFailure { e ->
                Log.w("SomadhanRepo", "adminSoftDeleteUser: Supabase dual-write failed for $userId (local flow unaffected): ${e.message}")
            }
        }

        logAdminAction(
            actionType = "DELETE_USER",
            targetId = userId,
            targetName = user?.name ?: userId,
            details = "ইউজার অ্যাকাউন্ট স্থায়ীভাবে ডিলিট করা হয়েছে"
        )
    }

    // [MONEY_FLOW_AND_ADMIN_BUGS ধাপ ৮ — বাগ D২] নতুন [role] প্যারামিটার ("USER"/"SOLVER"/
    // null)। null (ডিফল্ট) দিলে আগের মতোই শুধু shared `isBanned` কলাম আপডেট হয় (backward
    // compatible — AdminUserLookupView এখনো এভাবেই কল করে, এই ধাপের স্কোপের বাইরে)। "USER"/
    // "SOLVER" দিলে শুধু সংশ্লিষ্ট role-scoped কলাম আপডেট হয়, শেয়ার্ড কলাম অস্পৃষ্ট থাকে।
    suspend fun adminSetBanned(userId: String, banned: Boolean, role: String? = null) {
        val now = System.currentTimeMillis()
        when (role) {
            "SOLVER" -> userDao.setBannedStatusForSolverRole(userId, banned, now)
            "USER" -> userDao.setBannedStatusForUserRole(userId, banned, now)
            else -> userDao.setBannedStatus(userId, banned, now)
        }
        val user = userDao.getUserById(userId)
        if (user != null) {
            if (banned) {
                val banPenalty = platformSettingDao.getSetting("rep_penalty_banned")?.toDoubleOrNull() ?: 25.0
                applyReputationChange(userId, "ADMIN_BANNED", -banPenalty, null, "অ্যাডমিন কর্তৃক অ্যাকাউন্ট ব্যান হওয়ায় রেপুটেশন হ্রাস")
            }
            val notif = NotificationEntity(
                role = role ?: "",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                title = if (banned) "অ্যাকাউন্ট স্থগিত (Banned) করা হয়েছে" else "অ্যাকাউন্ট সক্রিয় করা হয়েছে",
                message = if (banned) "আপনার অ্যাকাউন্টটি অ্যাডমিন কর্তৃক সাময়িকভাবে স্থগিত (Banned) করা হয়েছে।" else "আপনার অ্যাকাউন্টের স্থগিতাদেশ তুলে নেওয়া হয়েছে।",
                targetType = "role",
                targetId = userId
            )
            notificationDao.insertNotification(notif)

            // [SUPABASE-MIGRATED - ধাপ ১২ (মূল কাজ, ব্যাচ ১)] `admin_set_banned` RPC — নিজেই
            // notification insert করে, guard/প্যাটার্ন `adminApproveKyc()`-এর মতোই। (রেপুটেশন
            // পেনাল্টি অংশ উপরে `applyReputationChange()`-এর মাধ্যমে আলাদাভাবেই dual-write হয়,
            // যদিও `ADMIN_BANNED` event type `submit_reputation_event` RPC-এর সমর্থিত তালিকায় নেই
            // — এটা আগে থেকেই জানা সীমাবদ্ধতা, এই session-এর scope-এ না।)
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.adminSetBanned(userId, banned, role).onFailure { e ->
                    Log.w("SomadhanRepo", "adminSetBanned: Supabase dual-write failed for $userId (local flow unaffected): ${e.message}")
                    // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ৪ (ব্যান/রোল/ব্যাজ গ্রুপ)] paramsJson keys
                    // "userId"/"banned"/"role"? -- OutboxRpcDispatcher.kt-এর "admin_set_banned"
                    // branch-এর সাথে মিলছে।
                    enqueueOutboxRetry(
                        rpcName = "admin_set_banned",
                        params = kotlinx.serialization.json.JsonObject(
                            buildMap {
                                put("userId", kotlinx.serialization.json.JsonPrimitive(userId))
                                put("banned", kotlinx.serialization.json.JsonPrimitive(banned))
                                role?.let { put("role", kotlinx.serialization.json.JsonPrimitive(it)) }
                            }
                        ),
                        error = e
                    )
                }
            }
        }
        logAdminAction(
            actionType = if (banned) "BAN_USER" else "UNBAN_USER",
            targetId = userId,
            targetName = user?.name ?: userId,
            details = (if (banned) "ব্যবহারকারীকে ব্যান করা হয়েছে" else "ব্যবহারকারীর ব্যান প্রত্যাহার করা হয়েছে") +
                (if (role != null) " (role: $role)" else ""),
            role = role ?: ""
        )
    }

    // [MONEY_FLOW_AND_ADMIN_BUGS ধাপ ৮ — বাগ D২] উপরের `adminSetBanned()`-এর একই প্যাটার্ন।
    suspend fun adminSetRestricted(userId: String, restricted: Boolean, role: String? = null) {
        val now = System.currentTimeMillis()
        when (role) {
            "SOLVER" -> userDao.setRestrictedStatusForSolverRole(userId, restricted, now)
            "USER" -> userDao.setRestrictedStatusForUserRole(userId, restricted, now)
            else -> userDao.setRestrictedStatus(userId, restricted, now)
        }
        val user = userDao.getUserById(userId)
        if (user != null) {
            if (restricted) {
                val restrictPenalty = platformSettingDao.getSetting("rep_penalty_restricted")?.toDoubleOrNull() ?: 10.0
                applyReputationChange(userId, "ADMIN_RESTRICTED", -restrictPenalty, null, "অ্যাডমিন কর্তৃক অ্যাকাউন্ট সীমাবদ্ধ (Restricted) হওয়ায় রেপুটেশন হ্রাস")
            }
            val notif = NotificationEntity(
                role = role ?: "",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                title = if (restricted) "অ্যাকাউন্ট সীমাবদ্ধ (Restricted) করা হয়েছে" else "অ্যাকাউন্টের সীমাবদ্ধতা তুলে নেওয়া হয়েছে",
                message = if (restricted) "আপনার অ্যাকাউন্টটিতে রেস্ট্রিকশন দেওয়া হয়েছে। কিছু ফিচার সীমিত থাকতে পারে।" else "আপনার অ্যাকাউন্টের সীমাবদ্ধতা সফলভাবে প্রত্যাহার করা হয়েছে।",
                targetType = "role",
                targetId = userId
            )
            notificationDao.insertNotification(notif)

            // [SUPABASE-MIGRATED - ধাপ ১২ (মূল কাজ, ব্যাচ ১)] `admin_set_restricted` RPC — নিজেই
            // notification insert করে, guard/প্যাটার্ন উপরের `adminSetBanned()`-এর মতোই।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.adminSetRestricted(userId, restricted, role).onFailure { e ->
                    Log.w("SomadhanRepo", "adminSetRestricted: Supabase dual-write failed for $userId (local flow unaffected): ${e.message}")
                    // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ৪ (ব্যান/রোল/ব্যাজ গ্রুপ)] paramsJson keys
                    // "userId"/"restricted"/"role"? -- OutboxRpcDispatcher.kt-এর
                    // "admin_set_restricted" branch-এর সাথে মিলছে।
                    enqueueOutboxRetry(
                        rpcName = "admin_set_restricted",
                        params = kotlinx.serialization.json.JsonObject(
                            buildMap {
                                put("userId", kotlinx.serialization.json.JsonPrimitive(userId))
                                put("restricted", kotlinx.serialization.json.JsonPrimitive(restricted))
                                role?.let { put("role", kotlinx.serialization.json.JsonPrimitive(it)) }
                            }
                        ),
                        error = e
                    )
                }
            }
        }
        logAdminAction(
            actionType = if (restricted) "RESTRICT_USER" else "UNRESTRICT_USER",
            targetId = userId,
            targetName = user?.name ?: userId,
            details = (if (restricted) "ব্যবহারকারীকে সীমাবদ্ধ (Restricted) করা হয়েছে" else "সীমাবদ্ধতা প্রত্যাহার করা হয়েছে") +
                (if (role != null) " (role: $role)" else ""),
            role = role ?: ""
        )
    }

    // [MONEY_FLOW_AND_ADMIN_BUGS ধাপ ৮ — বাগ D৩] উপরের দুইটার একই প্যাটার্ন।
    suspend fun adminSetVerifiedBadge(userId: String, verified: Boolean, role: String? = null) {
        val now = System.currentTimeMillis()
        when (role) {
            "SOLVER" -> userDao.setVerifiedBadgeForSolverRole(userId, verified, now)
            "USER" -> userDao.setVerifiedBadgeForUserRole(userId, verified, now)
            else -> userDao.setVerifiedBadge(userId, verified)
        }
        val user = userDao.getUserById(userId)
        if (user != null) {
            if (verified) {
                triggerDynamicReputationEvent(
                    userId = userId,
                    eventType = "BADGE_EXPERT_VERIFIED",
                    problemId = null,
                    defaultScore = 3.0,
                    defaultCap = 3.0,
                    isPositive = true,
                    customNote = "ভেরিফাইড এক্সপার্ট ব্যাজ অর্জন"
                )
            }

            // [SUPABASE-MIGRATED - ধাপ ৩২.৮; ধাপ ৮-এ RPC-ভিত্তিক + role-aware করা হলো]
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.adminSetVerifiedBadge(userId, verified, role).onFailure { e ->
                    Log.w("SomadhanRepo", "adminSetVerifiedBadge: Supabase dual-write failed for $userId (local flow unaffected): ${e.message}")
                    // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ৪ (ব্যান/রোল/ব্যাজ গ্রুপ)] paramsJson keys
                    // "userId"/"verified"/"role"? -- OutboxRpcDispatcher.kt-এর
                    // "admin_set_verified_badge" branch-এর সাথে মিলছে।
                    enqueueOutboxRetry(
                        rpcName = "admin_set_verified_badge",
                        params = kotlinx.serialization.json.JsonObject(
                            buildMap {
                                put("userId", kotlinx.serialization.json.JsonPrimitive(userId))
                                put("verified", kotlinx.serialization.json.JsonPrimitive(verified))
                                role?.let { put("role", kotlinx.serialization.json.JsonPrimitive(it)) }
                            }
                        ),
                        error = e
                    )
                }
            }
        }
        logAdminAction(
            actionType = if (verified) "ADD_VERIFIED_BADGE" else "REMOVE_VERIFIED_BADGE",
            targetId = userId,
            targetName = user?.name ?: userId,
            details = (if (verified) "ভেরিফাইড ব্লু ব্যাজ প্রদান করা হয়েছে" else "ভেরিফাইড ব্যাজ অপসারণ করা হয়েছে") +
                (if (role != null) " (role: $role)" else ""),
            role = role ?: ""
        )
    }

    suspend fun adminAdjustBalance(userId: String, amount: Double, isAddition: Boolean, reason: String, role: String? = null) = adminAdjustMutexFor(userId).withLock {
        val now = System.currentTimeMillis()

        // Best-effort duplicate-submission guard (see adminAdjustMutexFor()'s comment above for
        // why this can't be a fully deterministic idempotency key like TRX_SPLIT_/TRX_REFUND_).
        // Catches a double-tap or an accidental resubmission of the exact same adjustment
        // arriving within 5 seconds of the last one for this user.
        val signature = "$isAddition|$amount|$reason"
        val recent = recentAdminAdjustments[userId]
        if (recent != null && recent.first == signature && (now - recent.second) < 5000L) {
            Log.w("SomadhanRepo", "adminAdjustBalance: skipped — identical adjustment for $userId submitted again within 5s, refusing to apply twice")
            return@withLock
        }
        recentAdminAdjustments[userId] = signature to now

        // INSTANT local credit/debit; the cloud side goes through the admin_adjust_balance RPC
        // (already called above) with outbox retry on failure [Step 7.7 cleanup: pendingCloudSync
        // on the TransactionEntity below is an unread Firebase-era leftover, not the real retry
        // mechanism], instead of the old incrementUserBalance() fire-and-forget (no retry if
        // that single attempt failed/timed out).
        // [ব্যালেন্স ফিক্স] cloud RPC (উপরে) ইতিমধ্যেই role-aware (adjustRole ভিত্তিক
        // balance_user/balance_solver আপডেট করে) — কিন্তু নিচের local instant echo আগে শুধু
        // plain `balance` ছুঁতো, role-scoped mirror না, তাই role switch করলে ডিভাইসে স্টেল
        // সংখ্যা দেখাতে পারত এই adjustment-এর পরে। এখন adjustRole জানা থাকায় সরাসরি সঠিক
        // role-scoped DAO ভ্যারিয়েন্ট ব্যবহার করা যাচ্ছে (নিচের adjustRole গণনা এখানে তুলে
        // আনা হলো, আগে এটা এই কলের পরে হিসাব হতো)।
        val targetForAdjust = userDao.getUserById(userId)
        // [ধাপ ৭ ফিক্স, ROLE_SEPARATION_AUDIT.md আইটেম ৩] role এখন caller (admin UI)-এর কাছ
        // থেকে এক্সপ্লিসিট প্যারামিটার হিসেবে নেওয়া যায় — dual-role ইউজারের ক্ষেত্রে
        // targetForAdjust?.role (অ্যাকাউন্টের বর্তমান সক্রিয় role, যেটা ইউজার নিজে যেকোনো সময়
        // switch করতে পারে) সবসময় admin ঠিক কোন role-টা adjust করতে চেয়েছিল তা বোঝায় না। UI-তে
        // (AdminUsersView-এর cardRole, AdminUserLookupView-এর currentPerspectiveRole) এই তথ্য
        // ক্লিকের মুহূর্তেই জানা থাকে, তাই সেটাই এখন প্রাধান্য পাচ্ছে। role এক্সপ্লিসিটলি পাস না
        // হলে (পুরনো caller, যদি থাকে) আগের behavior — অ্যাকাউন্টের সক্রিয় role থেকে অনুমান —
        // অপরিবর্তিত fallback হিসেবে রয়ে গেল (কোনো পুরনো পথ মুছে ফেলা হয়নি)।
        val adjustRole = when (role) {
            "SOLVER" -> "SOLVER"
            "USER" -> "USER"
            else -> when (targetForAdjust?.role) {
                "SOLVER" -> "SOLVER"
                "USER" -> "USER"
                else -> null
            }
        }
        when (adjustRole) {
            "SOLVER" -> if (isAddition) userDao.addBalanceForSolverRole(userId, amount, now) else userDao.deductBalanceForSolverRole(userId, amount, now)
            "USER" -> if (isAddition) userDao.addBalanceForUserRole(userId, amount, now) else userDao.deductBalanceForUserRole(userId, amount, now)
            else -> if (isAddition) userDao.addBalance(userId, amount, now) else userDao.deductBalance(userId, amount, now)
        }
        val adjustmentTrx = TransactionEntity(
            id = "TRX_ADMIN_ADJ_${userId}_$now",
            problemId = "",
            problemTitle = if (isAddition) "অ্যাডমিন কর্তৃক ব্যালেন্স সংযোজন" else "অ্যাডমিন কর্তৃক ব্যালেন্স কর্তন",
            userId = userId,
            solverId = "",
            grossAmount = amount,
            commissionPercent = 0.0,
            commissionAmount = 0.0,
            netAmount = if (isAddition) amount else -amount,
            type = "ADMIN_ADJUSTMENT",
            pendingCloudSync = true,
            role = adjustRole ?: ""
        )
        transactionDao.insertTransaction(adjustmentTrx)

        // [SUPABASE-MIGRATED - ধাপ ১০, ধাপ ১৪.৫ (Kotlin wiring, উপ-ধাপ "ঘ")] `admin_adjust_balance`
        // RPC সোর্স পড়ে যাচাই করা হয়েছে — শুধু is_admin(auth.uid()) হলেই কল করা যায়, ভেতরে
        // নিজস্ব ৫-সেকেন্ড idempotency guard আছে (md5 signature দিয়ে, `idempotency_keys` টেবিলে)
        // — উপরের local recentAdminAdjustments in-memory guard-এর মতোই কনসেপ্ট, দুই স্তরেই
        // ডাবল-সাবমিট আটকায়। Deduct হলে `greatest(balance - amount, 0)` দিয়ে negative balance
        // আটকায় (Kotlin-সাইড userDao.deductBalance()-এর মতোই আচরণ)। guard হিসেবে শুধু session
        // আছে কিনা দেখা হচ্ছে (RPC নিজেই admin authorize করে)। ব্যর্থ/non-OK হলে শুধু log হবে,
        // local balance/transaction flow সম্পূর্ণ অপ্রভাবিত থাকে।
        // role: এখন উপরে গণনা করা `adjustRole` — caller-এর এক্সপ্লিসিট `role` প্যারামিটার (UI-তে
        // cardRole/currentPerspectiveRole থেকে) থাকলে সেটাই, নাহলে fallback হিসেবে
        // targetForAdjust?.role (দেখুন উপরের কমেন্ট, ধাপ ৭ ফিক্স) — "ADMIN" হলে (এই কনটেক্সটে
        // হওয়ার কথা না) p_role null থাকে, পুরনো ডিফল্ট আচরণে পড়ে থাকে।
        // (targetForAdjust/adjustRole এখন উপরে, local echo-র জন্যও ব্যবহার হচ্ছে বলে সরিয়ে আনা হলো)
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminAdjustBalance(userId, amount, isAddition, reason, role = adjustRole)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK" && resultField != "DUPLICATE_SKIPPED") {
                        Log.w("SomadhanRepo", "adminAdjustBalance: Supabase dual-write non-OK result for $userId: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "adminAdjustBalance: Supabase dual-write failed for $userId (local flow unaffected): ${e.message}")
                    // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ২] paramsJson keys "userId"/"amount"/
                    // "isAddition"/"reason"/"role" -- OutboxRpcDispatcher.kt-এর
                    // "admin_adjust_balance" branch-এর সাথে মিলছে। এই RPC-র নিজস্ব সার্ভার-সাইড
                    // ৫-সেকেন্ড idempotency guard আছে (md5 signature, উপরের কমেন্টে বর্ণিত) --
                    // requestWithdrawal-এর মতো কোনো id-mismatch ঝুঁকি নেই এখানে, তাই outbox
                    // retry নিরাপদে/পূর্ণাঙ্গভাবে কাজ করবে।
                    enqueueOutboxRetry(
                        rpcName = "admin_adjust_balance",
                        params = kotlinx.serialization.json.JsonObject(
                            buildMap {
                                put("userId", kotlinx.serialization.json.JsonPrimitive(userId))
                                put("amount", kotlinx.serialization.json.JsonPrimitive(amount))
                                put("isAddition", kotlinx.serialization.json.JsonPrimitive(isAddition))
                                put("reason", kotlinx.serialization.json.JsonPrimitive(reason))
                                adjustRole?.let { put("role", kotlinx.serialization.json.JsonPrimitive(it)) }
                            }
                        ),
                        error = e
                    )
                }
        }

        val user = targetForAdjust
        if (user != null) {
            val notif = NotificationEntity(
                role = adjustRole ?: "",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                title = if (isAddition) "ব্যালেন্স যোগ করা হয়েছে ৳${amount.toInt()}" else "ব্যালেন্স কর্তন করা হয়েছে ৳${amount.toInt()}",
                message = "অ্যাডমিন কর্তৃক আপনার ব্যালেন্স ${if (isAddition) "যোগ" else "কর্তন"} করা হয়েছে। কারণ: $reason",
                targetType = "balance",
                targetId = userId
            )
            notificationDao.insertNotification(notif)
        }
        logAdminAction(
            actionType = if (isAddition) "ADD_BALANCE" else "DEDUCT_BALANCE",
            targetId = userId,
            targetName = user?.name ?: userId,
            details = "${if (isAddition) "যোগ" else "কর্তন"}: ৳${amount.toInt()}, কারণ: $reason",
            role = adjustRole ?: ""
        )
    }

    suspend fun adminChangeRole(userId: String, newRole: String) {
        val user = userDao.getUserById(userId) ?: return
        val updated = user.copy(
            role = newRole,
            hasUserRole = user.hasUserRole || newRole == "USER",
            hasSolverRole = user.hasSolverRole || newRole == "SOLVER",
            updatedAt = System.currentTimeMillis()
        )
        userDao.updateUser(updated)
        val notif = NotificationEntity(
            role = "",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = userId,
            title = "অ্যাকাউন্ট রোল পরিবর্তন",
            message = "আপনার অ্যাকাউন্ট রোল পরিবর্তন করে '$newRole' করা হয়েছে।",
            targetType = "role",
            targetId = userId
        )
        notificationDao.insertNotification(notif)

        // [SUPABASE-MIGRATED - ধাপ ১২ (মূল কাজ, ব্যাচ ১)] `admin_change_role` RPC — নিজেই
        // notification insert করে (আর has_user_role/has_solver_role flag দুটোও local logic-এর
        // সাথে হুবহু মিলিয়ে আপডেট করে)। guard/প্যাটার্ন `adminApproveKyc()`-এর মতোই। `newRole`
        // অবশ্যই "USER"/"SOLVER"/"ADMIN" (uppercase) — local কোডেও এই কেসিংই ব্যবহৃত হয়, তাই কোনো
        // রূপান্তর লাগে না।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminChangeRole(userId, newRole).onFailure { e ->
                Log.w("SomadhanRepo", "adminChangeRole: Supabase dual-write failed for $userId (local flow unaffected): ${e.message}")
                // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ৪ (ব্যান/রোল/ব্যাজ গ্রুপ)] paramsJson keys
                // "userId"/"newRole" -- OutboxRpcDispatcher.kt-এর "admin_change_role"
                // branch-এর সাথে মিলছে।
                enqueueOutboxRetry(
                    rpcName = "admin_change_role",
                    params = kotlinx.serialization.json.JsonObject(
                        mapOf(
                            "userId" to kotlinx.serialization.json.JsonPrimitive(userId),
                            "newRole" to kotlinx.serialization.json.JsonPrimitive(newRole)
                        )
                    ),
                    error = e
                )
            }
        }

        logAdminAction(
            actionType = "CHANGE_ROLE",
            targetId = userId,
            targetName = user.name,
            details = "নতুন রোল: $newRole"
        )
    }

    suspend fun adminResetUserPassword(userId: String, newPlainPassword: String) {
        val hashedPassword = PasswordHasher.hash(newPlainPassword)
        val now = System.currentTimeMillis()
        userDao.resetPassword(userId, hashedPassword, now)
        val user = userDao.getUserById(userId)
        if (user != null) {
            val notif = NotificationEntity(
                role = "",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                title = "পাসওয়ার্ড রিসেট করা হয়েছে 🔒",
                message = "অ্যাডমিন কর্তৃক আপনার অ্যাকাউন্টের পাসওয়ার্ড রিসেট করা হয়েছে।",
                targetType = "role",
                targetId = userId
            )
            notificationDao.insertNotification(notif)
        }
        logAdminAction(
            actionType = "RESET_PASSWORD",
            targetId = userId,
            targetName = user?.name ?: userId,
            details = "নতুন পাসওয়ার্ড সেট করা হয়েছে"
        )

        // [SUPABASE-MIGRATED - ধাপ ৩০] উপরের local bcrypt + Firebase flow হুবহু অপরিবর্তিত রাখা
        // হলো (এখনো authoritative path)। শুধু best-effort dual-write হিসেবে admin-reset-user-password
        // Edge Function কল করা হচ্ছে, যাতে ওই একই ব্যক্তির real Supabase Auth পাসওয়ার্ডও sync থাকে।
        //
        // ⚠️ গুরুত্বপূর্ণ সীমাবদ্ধতা: Supabase Auth-এ প্রতি ফোন নম্বরে একটাই account (এই ব্যক্তির
        // "root" row) থাকে — কিন্তু dual-role local UserEntity মডেলে একই ব্যক্তির জন্য দুটো আলাদা
        // row থাকতে পারে (root + "USER_xxxx"/"SOLVER_xxxx" linked row, ধাপ ৩১-এ single-row-এ
        // একীভূত হওয়ার কথা)। তাই এখানে সরাসরি `userId` (যেটা linked/non-root row হতে পারে) না
        // পাঠিয়ে, আগে root account id বের করা হচ্ছে (ঠিক switchRole()-এর মতোই লজিক) — Edge Function
        // শুধু তখনই কল হবে যখন সেই root id একটা বৈধ UUID (অর্থাৎ সত্যিই কোনো Supabase Auth account-এর
        // সাথে যুক্ত) — লোকাল-শুধু/ডেমো-ধাঁচের নন-UUID id (থাকলে) নিরাপদে স্কিপ হবে, শুধু log হবে।
        if (user != null) {
            val rootAccountId = user.linkedAccountId?.takeIf { it.isNotBlank() } ?: user.id
            val looksLikeUuid = Regex(
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            ).matches(rootAccountId)
            if (looksLikeUuid && SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.adminResetUserPasswordViaEdgeFunction(rootAccountId, newPlainPassword)
                    .onFailure { e ->
                        Log.w("SomadhanRepo", "adminResetUserPassword: Edge Function dual-write failed (local flow unaffected): ${e.message}")
                    }
            } else {
                Log.w("SomadhanRepo", "adminResetUserPassword: root id '$rootAccountId' Supabase UUID মনে হচ্ছে না বা কোনো active session নেই — শুধু local Room reset করা হলো, Supabase dual-write স্কিপ করা হলো।")
            }
        }
    }

    suspend fun adminAdjustReputation(userId: String, scoreChange: Double, note: String, role: String? = null) {
        applyReputationChange(
            userId = userId,
            eventType = "ADMIN_ADJUSTMENT",
            scoreChange = scoreChange,
            problemId = null,
            note = note.ifBlank { "অ্যাডমিন কর্তৃক রেপুটেশন সমন্বয়" },
            // [BALANCE_REPUTATION_ROLE_SEPARATION ধাপ ৭খ] adminAdjustBalance()-এর `role`
            // প্যারামিটারের ঠিক একই প্যাটার্নে -- admin UI (AdminUsersView-এর cardRole,
            // AdminUserLookupView-এর currentPerspectiveRole) ক্লিকের মুহূর্তে যে role জানে
            // সেটাই এখন এখানে এক্সপ্লিসিটলি পাস করা যায়, applyReputationChange()-এর ভেতরের
            // user.role-ভিত্তিক অনুমানকে override করতে।
            role = role
        )
        val user = userDao.getUserById(userId)
        logAdminAction(
            actionType = "ADJUST_REPUTATION",
            targetId = userId,
            targetName = user?.name ?: userId,
            details = "স্কোর পরিবর্তন: ${if (scoreChange >= 0) "+$scoreChange" else "$scoreChange"}, নোট: $note",
            // [ধাপ ৭খ ফিক্স] caller-এর এক্সপ্লিসিট `role` থাকলে সেটাই লগ হবে
            // (adminAdjustBalance-এর adjustRole-এর মতোই), নাহলে fallback হিসেবে আগের মতো
            // user?.role।
            role = (role?.takeIf { it == "USER" || it == "SOLVER" }
                ?: user?.role?.takeIf { it == "USER" || it == "SOLVER" }
                ?: "")
        )
    }

    suspend fun adminUpdateProblemStatus(problemId: String, status: String) {
        problemDao.adminUpdateStatus(problemId, status)
        val problem = problemDao.getProblemById(problemId)
        if (problem != null) {
            val notif = NotificationEntity(
                role = "USER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = problem.userId,
                title = "সমস্যার স্ট্যাটাস আপডেট",
                message = "\"${problem.title}\" সমস্যার স্ট্যাটাস পরিবর্তন করে '$status' করা হয়েছে।",
                targetType = "problem",
                targetId = problem.id,
                relatedProblemId = problem.id
            )
            notificationDao.insertNotification(notif)
            // [SUPABASE-MIGRATED - ধাপ ১২ (মূল কাজ, ব্যাচ ২)] `admin_update_problem_status` RPC
            // (এই session-এ নতুন migration দিয়ে তৈরি) is_admin() চেক করে, problems.status আপডেট
            // করে, আর নিজেই owner-কে notify করে (উপরের local notif-এর সমতুল্য) — আলাদা
            // createNotification() লাগবে না। guard: শুধু session আছে কিনা (RPC নিজেই admin
            // authorize করে, adminApproveKyc-এর মতোই প্যাটার্ন)। best-effort।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.adminUpdateProblemStatus(problemId, status).onFailure { e ->
                    Log.w("SomadhanRepo", "adminUpdateProblemStatus: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
                }
            }
        }
        logAdminAction(
            actionType = "UPDATE_PROBLEM_STATUS",
            targetId = problemId,
            targetName = problem?.title ?: problemId,
            details = "নতুন স্ট্যাটাস: $status"
        )
    }

    suspend fun adminUpdateProblemBudget(problemId: String, minBudget: Double, maxBudget: Double) {
        problemDao.adminUpdateBudget(problemId, minBudget, maxBudget)
        val problem = problemDao.getProblemById(problemId)
        if (problem != null) {
            notificationDao.insertNotification(
                NotificationEntity(
                    role = "USER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = problem.userId,
                    title = "বাজেট পরিবর্তন করা হয়েছে",
                    message = "\"${problem.title}\" সমস্যার বাজেট ৳${minBudget.toInt()} - ৳${maxBudget.toInt()} হিসেবে পুনর্নির্ধারণ করা হয়েছে।",
                    targetType = "problem",
                    targetId = problem.id,
                    relatedProblemId = problem.id
                )
            )
            // [SUPABASE-MIGRATED - ধাপ ২] `admin_update_problem_budget` RPC দিয়ে আসল বাজেট-ডেটা
            // (min_budget/max_budget) এখন Supabase-এ dual-write হয় -- আগে শুধু নোটিফিকেশনই যেত,
            // আসল কলাম কখনো sync হতো না। guard: শুধু session আছে কিনা। best-effort।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.adminUpdateProblemBudget(problemId, minBudget, maxBudget).onFailure { e ->
                    Log.w("SomadhanRepo", "adminUpdateProblemBudget: budget dual-write failed for $problemId (local flow unaffected): ${e.message}")
                }
                SupabaseSyncManager.createNotification(
                    targetUserId = problem.userId,
                    title = "বাজেট পরিবর্তন করা হয়েছে",
                    message = "\"${problem.title}\" সমস্যার বাজেট ৳${minBudget.toInt()} - ৳${maxBudget.toInt()} হিসেবে পুনর্নির্ধারণ করা হয়েছে।",
                    targetType = "problem",
                    targetId = problem.id,
                    relatedProblemId = problem.id,
                    role = "USER"
                ).onFailure { e ->
                    Log.w("SomadhanRepo", "adminUpdateProblemBudget: createNotification dual-write failed for $problemId (local flow unaffected): ${e.message}")
                }
            }
        }
        logAdminAction(
            actionType = "UPDATE_PROBLEM_BUDGET",
            targetId = problemId,
            targetName = problem?.title ?: problemId,
            details = "বাজেট: ৳${minBudget.toInt()} - ৳${maxBudget.toInt()}"
        )
    }

    suspend fun adminReassignSolver(problemId: String, solverId: String, solverName: String) {
        problemDao.adminReassignSolver(problemId, solverId, solverName)
        val problem = problemDao.getProblemById(problemId)
        if (problem != null) {
            notificationDao.insertNotification(
                NotificationEntity(
                    role = "SOLVER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = solverId,
                    title = "নতুন সমস্যা অ্যাসাইন করা হয়েছে",
                    message = "অ্যাডমিন কর্তৃক আপনাকে \"${problem.title}\" সমস্যার দায়িত্ব দেওয়া হয়েছে।",
                    targetType = "problem",
                    targetId = problem.id,
                    relatedProblemId = problem.id
                )
            )
            notificationDao.insertNotification(
                NotificationEntity(
                    role = "USER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = problem.userId,
                    title = "সমাধানকারী পরিবর্তন করা হয়েছে",
                    message = "আপনার সমস্যা \"${problem.title}\"-এ সমাধানকারী হিসেবে $solverName-কে নির্ধারণ করা হয়েছে।",
                    targetType = "problem",
                    targetId = problem.id,
                    relatedProblemId = problem.id
                )
            )
            // [SUPABASE-MIGRATED - ধাপ ৩] `admin_reassign_solver` RPC দিয়ে আসল
            // accepted_solver_id/accepted_solver_name এখন Supabase-এ dual-write হয় -- আগে শুধু
            // নোটিফিকেশনই যেত, আসল কলাম কখনো sync হতো না (ধাপ ২৫-এর নোট অনুযায়ী)। নোটিফিকেশন
            // দুটোও `create_notification` RPC দিয়ে dual-write করা হলো -- caller = admin হওয়ায়
            // is_admin() bypass প্রযোজ্য (party-check ছাড়াই যেকোনো target), Supabase MCP দিয়ে
            // সোর্স পড়ে যাচাই করা হয়েছে। guard: শুধু session আছে কিনা। best-effort।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.adminReassignSolver(problemId, solverId, solverName).onFailure { e ->
                    Log.w("SomadhanRepo", "adminReassignSolver: solver-reassign dual-write failed for $problemId (local flow unaffected): ${e.message}")
                }
                SupabaseSyncManager.createNotification(
                    targetUserId = solverId,
                    title = "নতুন সমস্যা অ্যাসাইন করা হয়েছে",
                    message = "অ্যাডমিন কর্তৃক আপনাকে \"${problem.title}\" সমস্যার দায়িত্ব দেওয়া হয়েছে।",
                    targetType = "problem",
                    targetId = problem.id,
                    relatedProblemId = problem.id,
                    role = "SOLVER"
                ).onFailure { e ->
                    Log.w("SomadhanRepo", "adminReassignSolver: createNotification(solver) dual-write failed for $problemId (local flow unaffected): ${e.message}")
                }
                SupabaseSyncManager.createNotification(
                    targetUserId = problem.userId,
                    title = "সমাধানকারী পরিবর্তন করা হয়েছে",
                    message = "আপনার সমস্যা \"${problem.title}\"-এ সমাধানকারী হিসেবে $solverName-কে নির্ধারণ করা হয়েছে।",
                    targetType = "problem",
                    targetId = problem.id,
                    relatedProblemId = problem.id,
                    role = "USER"
                ).onFailure { e ->
                    Log.w("SomadhanRepo", "adminReassignSolver: createNotification(owner) dual-write failed for $problemId (local flow unaffected): ${e.message}")
                }
            }
        }
        logAdminAction(
            actionType = "REASSIGN_SOLVER",
            targetId = problemId,
            targetName = problem?.title ?: problemId,
            details = "নতুন সমাধানকারী: $solverName ($solverId)"
        )
    }

    suspend fun adminRejectBid(bidId: String) {
        bidDao.adminRejectBid(bidId)
        val bid = bidDao.getBidById(bidId)
        if (bid != null) {
            notificationDao.insertNotification(
                NotificationEntity(
                    role = "SOLVER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = bid.solverId,
                    title = "বিড বাতিল (Rejected) করা হয়েছে",
                    message = "অ্যাডমিন কর্তৃক আপনার ৳${bid.amount.toInt()} মূল্যের বিডটি বাতিল করা হয়েছে।",
                    targetType = "bid",
                    targetId = bid.id,
                    relatedProblemId = bid.problemId
                )
            )
            // [SUPABASE-MIGRATED - ধাপ ২৫] `reject_bid` RPC আগে থেকেই SupabaseSyncManager-এ
            // wrapper আকারে ছিল (ধাপ ৮-এর সময় যোগ হয়েছিল) কিন্তু কোনো call-site থেকে ব্যবহার
            // হচ্ছিল না -- এই ফাংশনই একমাত্র caller (cancelBid()-এর জন্য আলাদা RPC আছে)। RPC
            // নিজে owner-অথবা-admin দুটোই authorize করে, কিন্তু notification insert করে না --
            // তাই bid status dual-write-এর সাথে `create_notification` RPC আলাদাভাবে কল করা
            // হলো। caller = admin হওয়ায় `create_notification`-এর is_admin() bypass (party-check
            // ছাড়াই যেকোনো target) প্রযোজ্য -- Supabase MCP দিয়ে সোর্স পড়ে যাচাই করা হয়েছে।
            // guard: শুধু session আছে কিনা (RPC নিজে admin/owner authorize করবে)। best-effort।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.rejectBid(bid.id).onFailure { e ->
                    Log.w("SomadhanRepo", "adminRejectBid: reject_bid dual-write failed for $bidId (local flow unaffected): ${e.message}")
                }
                SupabaseSyncManager.createNotification(
                    targetUserId = bid.solverId,
                    title = "বিড বাতিল (Rejected) করা হয়েছে",
                    message = "অ্যাডমিন কর্তৃক আপনার ৳${bid.amount.toInt()} মূল্যের বিডটি বাতিল করা হয়েছে।",
                    targetType = "bid",
                    targetId = bid.id,
                    relatedProblemId = bid.problemId,
                    role = "SOLVER"
                ).onFailure { e ->
                    Log.w("SomadhanRepo", "adminRejectBid: createNotification dual-write failed for $bidId (local flow unaffected): ${e.message}")
                }
            }
        }
        logAdminAction(
            actionType = "REJECT_BID",
            targetId = bidId,
            targetName = bid?.solverName ?: bidId,
            details = "বিড বাতিল করা হয়েছে (৳${bid?.amount?.toInt() ?: 0})"
        )
    }

    // ---------------- ESCROW ----------------

    suspend fun openEscrow(problem: ProblemEntity, bid: BidEntity): EscrowEntity {
        val escrow = EscrowEntity(
            id = "ESCROW_${UUID.randomUUID().toString().take(8)}",
            problemId = problem.id,
            problemTitle = problem.title,
            userId = problem.userId,
            solverId = bid.solverId,
            baseAmount = bid.amount
        )
        escrowDao.insert(escrow)
        // [SUPABASE-MIGRATED - ধাপ ৩৩.৩ কাজ ৫-৭] আগে এখানে FirebaseSyncManager.syncEscrow(escrow)
        // কল হতো। এটা সরানো হলো কারণ এই escrow-এর cloud-সাইড রেকর্ড ইতিমধ্যেই acceptBid()-এর
        // Supabase dual-write (accept_bid RPC নিজেই escrow row তৈরি করে) দিয়ে হয়ে যায় -- এই
        // লাইনটা ছিল শুধু একটা সমান্তরাল Firestore-সাইড কপি, কোনো একমুখী নির্ভরতা না।

        notificationDao.insertNotification(
            NotificationEntity(
                role = "USER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = problem.userId,
                title = "Escrow-এ টাকা সুরক্ষিত হয়েছে 🔒",
                message = "${problem.title}-এর জন্য ৳${bid.amount.toInt()} Escrow-তে হোল্ড করা হয়েছে। কাজ সম্পন্ন হলে Solver-কে পরিশোধ করা হবে।",
                targetType = "escrow",
                targetId = escrow.id,
                relatedProblemId = problem.id
            )
        )
        return escrow
    }

    suspend fun addToEscrow(problemId: String, amount: Double) {
        // Bug fix: resolve the SPECIFIC (current) escrow row for this problem first, and write
        // to it by id (addExtraAmountById), instead of the old problemId-scoped addExtraAmount()
        // which touched every escrow row ever created for this problemId -- including old,
        // already-refunded rows from a previous solver's cancelled bid. See
        // addExtraAmountById()'s doc comment in AppDaos.kt for the full story. getByProblemId()
        // already resolves to the most recent row (ORDER BY createdAt DESC), i.e. the currently
        // active payment cycle.
        val current = escrowDao.getByProblemId(problemId) ?: return
        escrowDao.addExtraAmountById(current.id, amount, System.currentTimeMillis())
        val updated = escrowDao.getEscrowById(current.id)
        if (updated != null) {
            // Atomic cloud increment instead of pushing the locally-read value via a full-document
            // merge -- see the comment above incrementEscrowExtraAmount() for why this matters.

            // [SUPABASE-MIGRATED - ধাপ ৩২.৭] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি।
            // guard শুধু session আছে কিনা (RPC নিজেই caller escrow-এর সাথে জড়িত (user/solver/admin)
            // কিনা চেক করে, refund_escrow_once()-এর established প্যাটার্নে)। এটাও পুরো row
            // ওভাররাইট না করে atomic `extra_amount = extra_amount + p_amount` — race-condition
            // এড়ানোর একই কারণ Supabase-সাইডেও প্রযোজ্য।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.incrementEscrowExtraAmount(updated.id, amount)
                    .onFailure { e ->
                        Log.w("SomadhanRepo", "addToEscrow: Supabase dual-write failed for escrow ${updated.id} (local flow unaffected): ${e.message}")
                        // [OUTBOX WIRE - ধাপ ৭ ব্যাচ ১] paramsJson keys "escrowId"/"amount" --
                        // OutboxRpcDispatcher.kt-এর "increment_escrow_extra_amount" branch-এর
                        // সাথে মিলছে (কমেন্টে যাচাই করা)।
                        enqueueOutboxRetry(
                            rpcName = "increment_escrow_extra_amount",
                            params = kotlinx.serialization.json.JsonObject(
                                mapOf(
                                    "escrowId" to kotlinx.serialization.json.JsonPrimitive(updated.id),
                                    "amount" to kotlinx.serialization.json.JsonPrimitive(amount)
                                )
                            ),
                            error = e
                        )
                    }
            }
        }
    }

    // [ধাপ ৩৩.১] `releaseEscrow(problemId: String): EscrowEntity?` (আগে এখানে ছিল) সম্পূর্ণ
    // ডিলিট করা হলো -- মূল প্রম্পটেই (ধাপ ৩২-এর অডিট) এটা dead-function উদাহরণ হিসেবে উল্লেখ
    // ছিল; `grep` দিয়ে পুনরায় যাচাই করে নিশ্চিত হওয়া গেছে পুরো কোডবেসে এর ০ caller
    // (নিচের `adminReleaseEscrow(escrow: EscrowEntity)` সম্পূর্ণ ভিন্ন, সক্রিয় ফাংশন)।

    suspend fun adminReleaseEscrow(escrow: EscrowEntity) {
        // Idempotency guard: re-fetch the escrow's CURRENT status right before crediting.
        // Without this, a double-tap on the release button, a network retry, or two admin
        // sessions releasing the same escrow around the same time would each pass a stale
        // "escrow" snapshot in and pay the solver twice for the same job. This mirrors the
        // same-escrow-once guarantee that refundEscrowOnce() already provides on the refund
        // side — release needed the same protection since it also credits a wallet balance.
        val currentEscrow = escrowDao.getEscrowById(escrow.id)
        if (currentEscrow == null || currentEscrow.status == "RELEASED" || currentEscrow.status == "REFUNDED") {
            Log.w("SomadhanRepo", "adminReleaseEscrow: skipped — escrow ${escrow.id} is already ${currentEscrow?.status ?: "missing"}, refusing to pay out again")
            return
        }
        // [Step 12.10d] RPC-first (স্থায়ী প্রত্যাখ্যানে এখানেই থামে, কোনো local write হয়নি)।
        releaseEscrowCloudFirst(currentEscrow)
        val now = System.currentTimeMillis()
        val totalGrossAmount = escrow.baseAmount + escrow.extraAmount
        val problem = problemDao.getProblemById(escrow.problemId)
        val dummyProblem = problem ?: ProblemEntity(
            id = escrow.problemId,
            userId = escrow.userId,
            userName = "",
            userPhone = "",
            userAddress = "",
            title = escrow.problemTitle,
            description = "",
            categoryId = "",
            categoryName = "",
            isPhysical = false,
            latitude = 0.0,
            longitude = 0.0,
            minBudget = escrow.baseAmount,
            maxBudget = escrow.baseAmount,
            urgency = "সাধারণ",
            acceptedAmount = escrow.baseAmount,
            appliedCommissionRate = platformSettingDao.getSetting("commission_percent")?.toDoubleOrNull() ?: 10.0
        )
        // Bug fix: mark the underlying problem COMPLETED here too. Previously this function paid
        // the solver and flipped the escrow to RELEASED but left problem.status untouched (still
        // OPEN/IN_PROGRESS). That meant: (1) the job screen kept showing "Confirm & Complete" to
        // the user/solver as if nothing had happened, and (2) confirmReleaseAndComplete()'s
        // duplicate-payment guard — which at the time only checked problem.status=="COMPLETED" —
        // would not trip, so the user's own confirm tap or the 48-hour auto-release sweep could
        // pay the same escrow out a second time. Setting status here closes both gaps. (A fresh
        // escrow-status check was also added to confirmReleaseAndComplete() as defense-in-depth.)
        if (problem != null && problem.status != "COMPLETED") {
            val updatedProblem = problem.copy(
                status = "COMPLETED",
                completedAt = now,
                hasReleaseRequest = true,
                completionResultSeenByUser = false,
                completionResultSeenBySolver = false,
                lastActivityAt = now
            )
            problemDao.updateProblem(updatedProblem)
        }

        // Centralized commission calc + wallet credit + escrow release + Transaction insert
        // (shared with confirmReleaseAndComplete() and reconcileEscrowStates()'s self-heal path).
        val breakdown = payoutEscrowToSolver(
            escrow = escrow,
            problem = dummyProblem,
            solverId = escrow.solverId,
            baseAmount = escrow.baseAmount,
            extraAmount = escrow.extraAmount,
            cloudReleaseHandled = true
        )
        val netPayout = breakdown.netAmount
        val commissionAmount = breakdown.totalCommission
        val activePlatformRate = if (totalGrossAmount > 0.0) (commissionAmount / totalGrossAmount) * 100.0 else 0.0

        val solverNotif = NotificationEntity(
            role = "SOLVER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = escrow.solverId,
            title = "অ্যাডমিন কর্তৃক Escrow রিলিজ! ৳${netPayout.toInt()}",
            message = "${escrow.problemTitle}-এর Escrow থেকে প্ল্যাটফর্ম কমিশন (${DistanceUtil.toBengaliDigits(activePlatformRate.toInt().toString())}%) বাদে ৳${netPayout.toInt()} আপনার ব্যালেন্সে জমা করা হয়েছে।",
            targetType = "balance",
            targetId = escrow.solverId,
            relatedProblemId = escrow.problemId
        )
        notificationDao.insertNotification(solverNotif)

        val userNotif = NotificationEntity(
            role = "USER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = escrow.userId,
            title = "Escrow পেমেন্ট প্রদান সম্পন্ন",
            message = "অ্যাডমিন প্যানেল থেকে ${escrow.problemTitle}-এর Escrow ৳${totalGrossAmount.toInt()} সমাধানকারীকে প্রদান করা হয়েছে (কমিশন কর্তনসহ)।",
            targetType = "escrow",
            targetId = escrow.id,
            relatedProblemId = escrow.problemId
        )
        notificationDao.insertNotification(userNotif)

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৫] `release_escrow` RPC (উপরে, payoutEscrowToSolver
        // এর ভেতরে dual-write হয়) নিজে কোনো notification insert করে না — শুধু money/escrow অংশ।
        // এই দুটো admin-নির্দিষ্ট নোটিফিকেশন (solverNotif/userNotif) এখানে আলাদাভাবে
        // create_notification RPC দিয়ে wire করা হলো, acceptBid()/solverCancelJob()-এর নোটিফিকেশন-
        // গ্যাপ ফিক্সের মতোই একই প্যাটার্নে (caller admin বলে RPC-এর party-check এড়িয়ে
        // যেকোনো target-এ পাঠাতে পারবে)। best-effort, ব্যর্থ হলে শুধু log।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.createNotification(
                targetUserId = escrow.solverId,
                title = solverNotif.title,
                message = solverNotif.message,
                targetType = solverNotif.targetType,
                targetId = solverNotif.targetId,
                relatedProblemId = solverNotif.relatedProblemId,
                role = solverNotif.role
            ).onFailure { e ->
                Log.w("SomadhanRepo", "adminReleaseEscrow: Supabase solver notification dual-write failed for escrow ${escrow.id} (local flow unaffected): ${e.message}")
            }
            SupabaseSyncManager.createNotification(
                targetUserId = escrow.userId,
                title = userNotif.title,
                message = userNotif.message,
                targetType = userNotif.targetType,
                targetId = userNotif.targetId,
                relatedProblemId = userNotif.relatedProblemId,
                role = userNotif.role
            ).onFailure { e ->
                Log.w("SomadhanRepo", "adminReleaseEscrow: Supabase owner notification dual-write failed for escrow ${escrow.id} (local flow unaffected): ${e.message}")
            }
        }

        logAdminAction(
            actionType = "RELEASE_ESCROW",
            targetId = escrow.id,
            targetName = escrow.problemTitle,
            details = "মোট: ৳${totalGrossAmount.toInt()}, কমিশন: ৳${commissionAmount.toInt()}, পেআউট: ৳${netPayout.toInt()}"
        )
    }

    suspend fun adminRefundEscrow(escrow: EscrowEntity) {
        if (escrow.status == "REFUNDED") return
        val now = System.currentTimeMillis()
        val totalAmount = escrow.baseAmount + escrow.extraAmount
        val refundSucceeded = refundEscrowOnce(
            problemId = escrow.problemId,
            amount = totalAmount,
            userId = escrow.userId,
            solverId = escrow.solverId,
            problemTitle = escrow.problemTitle,
            baseAmount = escrow.baseAmount,
            extraAmount = escrow.extraAmount,
            escrowId = escrow.id
        )

        // Reset problem to OPEN and clear accepted solver/bid details
        val problem = problemDao.getProblemById(escrow.problemId)
        if (problem != null) {
            val acceptedBidId = problem.acceptedBidId
            val updatedProblem = problem.copy(
                status = "OPEN",
                acceptedBidId = null,
                acceptedSolverId = null,
                acceptedSolverName = null,
                acceptedAmount = null,
                lastActivityAt = now
            )
            problemDao.updateProblem(updatedProblem)

            // Cancel the accepted bid if present
            val progressStep = problem?.calculateProgressStep() ?: 1
            if (!acceptedBidId.isNullOrBlank()) {
                val bid = bidDao.getBidById(acceptedBidId)
                if (bid != null && bid.status != "CANCELLED") {
                    val updatedBid = bid.copy(status = "CANCELLED", progressAtCancel = progressStep, resolutionType = "ADMIN_MANUAL_REFUND", resolvedAt = now)
                    bidDao.updateBid(updatedBid)
                }
            } else {
                val bids = bidDao.getBidsForProblemSync(escrow.problemId)
                bids.filter { it.solverId == escrow.solverId && it.status == "ACCEPTED" }.forEach { b ->
                    val updatedBid = b.copy(status = "CANCELLED", progressAtCancel = progressStep, resolutionType = "ADMIN_MANUAL_REFUND", resolvedAt = now)
                    bidDao.updateBid(updatedBid)
                }
            }

            // [SUPABASE-MIGRATED - ধাপ ১ক] উপরের problemDao.updateProblem()/bidDao.updateBid()
            // শুধু local Room-এ হয় — `refund_escrow_once` RPC (refundEscrowOnce() ভেতরে) শুধু
            // escrows/transactions/users.balance ছোঁয়, problem/bid ছোঁয় না। তাই এখানে আলাদা
            // dual-write হিসেবে `admin_refund_and_reopen_problem` RPC কল করা হলো, বাকি সব
            // dual-write-এর existing কনভেনশন অনুযায়ী: best-effort, শুধু session থাকলে, ব্যর্থ
            // হলে শুধু log।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.adminRefundAndReopenProblem(escrow.problemId).onFailure { e ->
                    Log.w("SomadhanRepo", "adminRefundEscrow: Supabase problem/bid reopen dual-write failed for ${escrow.problemId} (local flow unaffected): ${e.message}")
                    // [OUTBOX WIRE - Step 12.6] paramsJson key "problemId": String --
                    // OutboxRpcDispatcher.kt-এর "admin_refund_and_reopen_problem" branch-এর সাথে
                    // অক্ষরে অক্ষরে মিলছে। idempotency (migration recovered_admin_money.sql বডি থেকে
                    // যাচাই): problem `for update` লক, status='OPEN'+null-ফিল্ড অকন্ডিশনাল SET (এই RPC
                    // নিজে কোনো balance/escrow ছোঁয় না, শুধু problem/bid state), bid cancel
                    // `status <> 'CANCELLED'`/`status = 'ACCEPTED'` গার্ডেড। ২য়বার চললে টাকা নড়ে না।
                    // ⚠️ residual (BLOCKED করা হয়নি, শুধু নথিভুক্ত, solverCancelJob-এর মতোই ক্লাস):
                    // RPC সার্ভারের *বর্তমান* accepted_bid_id/accepted_solver_id পড়ে -- failure ও
                    // replay-এর মাঝে নতুন কোনো solver bid accept করলে, stale replay সেই নতুন bid-কে
                    // ভুলভাবে CANCELLED করে দিতে পারে।
                    enqueueOutboxRetry(
                        rpcName = "admin_refund_and_reopen_problem",
                        params = kotlinx.serialization.json.JsonObject(
                            buildMap {
                                put("problemId", kotlinx.serialization.json.JsonPrimitive(escrow.problemId))
                            }
                        ),
                        error = e
                    )
                }
            }
        }

        // Apply reputation penalty to solver for not completing / abandonment
        val cancelPenalty = platformSettingDao.getSetting("rep_penalty_job_cancelled")?.toDoubleOrNull() ?: 3.0
        applyReputationChange(
            userId = escrow.solverId,
            eventType = "JOB_CANCELLED_BY_SOLVER",
            scoreChange = -cancelPenalty,
            problemId = escrow.problemId,
            note = "কাজ অ্যাকসেপ্ট করার পর সলভার সাড়া দেননি, অ্যাডমিন কর্তৃক রিফান্ড"
        )

        val userNotif = NotificationEntity(
            role = "USER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = escrow.userId,
            title = "Escrow টাকা রিফান্ড সম্পন্ন! ৳${totalAmount.toInt()}",
            message = "${escrow.problemTitle}-এর Escrow থেকে ৳${totalAmount.toInt()} আপনার ওয়ালেটে ফেরত দেওয়া হয়েছে এবং সমস্যাটি পুনরায় OPEN করা হয়েছে।",
            targetType = "balance",
            targetId = escrow.userId,
            relatedProblemId = escrow.problemId
        )
        notificationDao.insertNotification(userNotif)

        val solverNotif = NotificationEntity(
            role = "SOLVER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = escrow.solverId,
            title = "Escrow বাতিল ও রিফান্ড",
            message = "${escrow.problemTitle}-এর Escrow তহবিল গ্রাহককে রিফান্ড করা হয়েছে এবং কাজটি বাতিল করা হয়েছে।",
            targetType = "escrow",
            targetId = escrow.id,
            relatedProblemId = escrow.problemId
        )
        notificationDao.insertNotification(solverNotif)

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৫] `refund_escrow_once` RPC (উপরে, refundEscrowOnce এর
        // ভেতরে dual-write হয়) নিজে কোনো notification insert করে না — শুধু money/escrow অংশ। এই
        // দুটো admin-নির্দিষ্ট নোটিফিকেশন (userNotif/solverNotif) এখানে আলাদাভাবে create_notification
        // RPC দিয়ে wire করা হলো, adminReleaseEscrow()-এর ঠিক একই প্যাটার্নে। best-effort, ব্যর্থ
        // হলে শুধু log।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.createNotification(
                targetUserId = escrow.userId,
                title = userNotif.title,
                message = userNotif.message,
                targetType = userNotif.targetType,
                targetId = userNotif.targetId,
                relatedProblemId = userNotif.relatedProblemId,
                role = userNotif.role
            ).onFailure { e ->
                Log.w("SomadhanRepo", "adminRefundEscrow: Supabase owner notification dual-write failed for escrow ${escrow.id} (local flow unaffected): ${e.message}")
            }
            SupabaseSyncManager.createNotification(
                targetUserId = escrow.solverId,
                title = solverNotif.title,
                message = solverNotif.message,
                targetType = solverNotif.targetType,
                targetId = solverNotif.targetId,
                relatedProblemId = solverNotif.relatedProblemId,
                role = solverNotif.role
            ).onFailure { e ->
                Log.w("SomadhanRepo", "adminRefundEscrow: Supabase solver notification dual-write failed for escrow ${escrow.id} (local flow unaffected): ${e.message}")
            }
        }

        logAdminAction(
            actionType = "REFUND_ESCROW",
            targetId = escrow.id,
            targetName = escrow.problemTitle,
            details = "মোট: ৳${totalAmount.toInt()} গ্রাহকের ব্যালেন্সে রিফান্ড করা হয়েছে, সমস্যা OPEN করা হয়েছে"
        )
    }

    fun getEscrowForProblem(problemId: String) = escrowDao.getEscrowByProblemIdFlow(problemId)
    fun getAllHeldEscrows() = escrowDao.getAllHeldEscrows()
    fun getAllReleasedEscrows() = escrowDao.getAllReleasedEscrows()
    fun getAllRefundedEscrows() = escrowDao.getAllRefundedEscrows()
    fun getTotalHeldEscrowAmount() = escrowDao.getTotalHeldAmount()
    fun getAllAdditionalCharges() = additionalChargeDao.getAllAdditionalCharges()

    /**
     * ধাপ ৩২.৫: `additional_charges` টেবিল live-listen করা হয় না (ENGINEERING_NOTES.md §৯) —
     * `FirebaseSyncManager.refreshAdditionalCharges()`-এর Supabase সমতুল্য, explicit pull করে
     * প্রতিটা row `additionalChargeDao.insert()` (REPLACE conflict strategy) দিয়ে upsert করে।
     */
    suspend fun refreshAdditionalChargesFromSupabase() {
        SupabaseSyncManager.getAllAdditionalCharges().onSuccess { charges ->
            charges.forEach { dto ->
                additionalChargeDao.insert(dto.toAdditionalChargeEntity())
            }
        }.onFailure { e ->
            Log.w("SomadhanRepo", "refreshAdditionalChargesFromSupabase failed: ${e.message}")
        }
    }
 
     // ---------------- ADDITIONAL CHARGES ----------------

    suspend fun requestAdditionalCharge(problem: ProblemEntity, solverId: String, reason: String, amount: Double) {
        val existingPending = additionalChargeDao.getPendingByProblemId(problem.id).firstOrNull() ?: emptyList()
        if (existingPending.any { it.solverId == solverId }) {
            throw IllegalStateException("ইতোমধ্যে একটি অতিরিক্ত বিলের অনুরোধ পর্যালোচনায় রয়েছে। ব্যবহারকারী সিদ্ধান্ত নিলে পুনরায় অনুরোধ করতে পারবেন।")
        }

        val now = System.currentTimeMillis()
        // [SUPABASE-MIGRATED - ধাপ ১০] ধাপ ৯-এর requestWithdrawal()-এর মতোই একই কারণে: এখানেও
        // `request_additional_charge` RPC নিজে থেকেই (server-side) একটা নতুন charge_id বানায়
        // (client থেকে পাঠানো যায় না) — তাই RPC-টা local record বানানোর *আগেই* কল করা হচ্ছে
        // (শুধু নিজের row হলে — currentUserId() == solverId, RPC-এর
        // auth.uid() = problem.accepted_solver_id চেকের সাথে মিলিয়ে), সফল হলে RPC-এর ফেরত দেওয়া
        // charge_id-টাই local id হিসেবে ব্যবহার করা হয় (দুই পাশেই একই id, তাই পরের
        // respondToAdditionalCharge()-এর dual-write ঠিকভাবে এই একই charge খুঁজে পাবে)। ব্যর্থ
        // হলে (session নেই, network error, ইত্যাদি) আগের মতোই local-generated id দিয়ে চালিয়ে
        // যাওয়া হয় — local flow কখনোই এই RPC কলের উপর নির্ভর করে না, সম্পূর্ণ best-effort।
        var chargeId = "EXTRA_${UUID.randomUUID().toString().take(8)}"
        if (SupabaseAuthManager.currentUserId() == solverId) {
            SupabaseSyncManager.requestAdditionalCharge(
                problemId = problem.id,
                reason = reason,
                amount = amount
            ).onSuccess { json ->
                val obj = json as? kotlinx.serialization.json.JsonObject
                val resultField = (obj?.get("result") as? kotlinx.serialization.json.JsonPrimitive)?.content
                val cloudId = (obj?.get("charge_id") as? kotlinx.serialization.json.JsonPrimitive)?.content
                if (resultField == "OK" && !cloudId.isNullOrBlank()) {
                    chargeId = cloudId
                } else {
                    Log.w("SomadhanRepo", "requestAdditionalCharge: Supabase dual-write non-OK result: $resultField (using local-generated id, local flow unaffected)")
                }
            }.onFailure { e ->
                Log.w("SomadhanRepo", "requestAdditionalCharge: Supabase dual-write failed (using local-generated id, local flow unaffected): ${e.message}")
                // [OUTBOX WIRE - Step 12.6] paramsJson key "problemId","reason": String, "amount": Double --
                // OutboxRpcDispatcher.kt-এর "request_additional_charge" branch-এর সাথে অক্ষরে অক্ষরে
                // মিলছে। idempotency (migration step34_additional_charges_realtime_and_chat_notice.sql
                // বডি থেকে যাচাই): কোনো balance/escrow ছোঁয় না (শুধু insert + notification), `PENDING_CHARGE_EXISTS`
                // exception গার্ড (একই problem+solver-এ একসাথে একাধিক PENDING charge আটকায়)। ⚠️ জানা
                // সীমাবদ্ধতা (এই ধাপের স্কোপের বাইরে, উপরের কমেন্টেই আগে থেকে নথিভুক্ত): RPC নিজে নতুন
                // deterministic-না-হওয়া `charge_id` বানায়, যা এই retry-র সময় local-generated id-র সাথে
                // মিলবে না -- তবু retry cloud ledger-এ entry নিশ্চিত করে (dual-write gap বন্ধ করাই এই
                // ধাপের লক্ষ্য), শুধু id-sync আলাদা bug হিসেবে থেকে যায়।
                enqueueOutboxRetry(
                    rpcName = "request_additional_charge",
                    params = kotlinx.serialization.json.JsonObject(
                        buildMap {
                            put("problemId", kotlinx.serialization.json.JsonPrimitive(problem.id))
                            put("reason", kotlinx.serialization.json.JsonPrimitive(reason))
                            put("amount", kotlinx.serialization.json.JsonPrimitive(amount))
                        }
                    ),
                    error = e
                )
            }
        }
        val charge = AdditionalChargeEntity(
            id = chargeId,
            problemId = problem.id,
            solverId = solverId,
            userId = problem.userId,
            reason = reason,
            amount = amount
        )
        additionalChargeDao.insert(charge)

        val updatedProblem = problem.copy(lastActivityAt = now)
        problemDao.updateProblem(updatedProblem)

        val solverName = problem.acceptedSolverName ?: "সমাধানকারী"
        val formattedAmount = DistanceUtil.toBengaliDigits(amount.toInt().toString())

        val notif = NotificationEntity(
            role = "USER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = problem.userId,
            title = "অতিরিক্ত বিল অনুরোধ",
            message = "${problem.title}-এর জন্য ৳$formattedAmount অতিরিক্ত বিল অনুরোধ করা হয়েছে। কারণ: $reason",
            targetType = "additional_charge",
            targetId = charge.id,
            relatedProblemId = problem.id
        )
        notificationDao.insertNotification(notif)

        // Post chat message notice
        sendMessage(
            problemId = problem.id,
            senderId = solverId,
            receiverId = problem.userId,
            senderName = solverName,
            content = "💰 আমি ৳$formattedAmount অতিরিক্ত বিলের অনুরোধ পাঠিয়েছি। কারণ: $reason"
        )
    }

    suspend fun respondToAdditionalCharge(charge: AdditionalChargeEntity, accept: Boolean) {
        // Idempotency guard: re-fetch this charge's CURRENT status right before acting on it. A
        // double-tap on Accept/Reject, or a network retry resending the same request, would
        // otherwise deduct the user's wallet a second time and credit escrow a second time for
        // the same charge -- since respondToAdditionalCharge() had no such guard before, this was
        // a real, reachable bug, not just a theoretical one.
        val currentCharge = additionalChargeDao.getChargeById(charge.id)
        if (currentCharge == null || currentCharge.status != "PENDING") {
            Log.w("SomadhanRepo", "respondToAdditionalCharge: skipped — charge ${charge.id} is already ${currentCharge?.status ?: "missing"}, refusing to process again")
            return
        }
        val respondedAt = System.currentTimeMillis()
        val newStatus = if (accept) "ACCEPTED" else "REJECTED"
        additionalChargeDao.updateStatus(charge.id, newStatus, respondedAt)

        // [SUPABASE-MIGRATED - ধাপ ১০] `respond_additional_charge` RPC সোর্স পড়ে যাচাই করা
        // হয়েছে — owner (auth.uid() = charge.user_id) অথবা admin কল করতে পারে, ভেতরে নিজেই
        // idempotency guard আছে (status != PENDING হলে শুধু ALREADY_RESPONDED রিটার্ন করে, exception
        // ছোঁড়ে না)। Accept হলে RPC ঠিক Kotlin-সাইডের মতোই user balance থেকে
        // min(balance, charge.amount) deduct করে escrow-এর extra_amount-এ যোগ করে (line-by-line
        // মিলিয়ে verify করা হয়েছে, রিপোর্টে বিস্তারিত)। **জানা সীমাবদ্ধতা**: শুধু তখনই কাজ করবে
        // যখন এই charge-টা requestAdditionalCharge()-এর dual-write দিয়েই তৈরি হয়েছিল (local id ==
        // cloud charge_id) — নাহলে RPC `CHARGE_NOT_FOUND` exception ছুঁড়বে, যা শুধু log হয়, local
        // flow (উপরের additionalChargeDao.updateStatus + নিচের balance/escrow লজিক) সম্পূর্ণ
        // অপ্রভাবিত থাকে। guard হিসেবে শুধু session আছে কিনা দেখা হচ্ছে (process_withdrawal-এর
        // মতোই), কারণ RPC নিজেই owner/admin authorize করে।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.respondToAdditionalCharge(charge.id, accept)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK") {
                        Log.w("SomadhanRepo", "respondToAdditionalCharge: Supabase dual-write non-OK result for ${charge.id}: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "respondToAdditionalCharge: Supabase dual-write failed for ${charge.id} (local flow unaffected): ${e.message}")
                    // [OUTBOX WIRE - Step 12.6] paramsJson key "chargeId": String, "accept": Boolean --
                    // OutboxRpcDispatcher.kt-এর "respond_additional_charge" branch-এর সাথে অক্ষরে অক্ষরে
                    // মিলছে। idempotency (migration step36_transaction_role_column_and_rpc_dual_write.sql
                    // বডি থেকে যাচাই): charge `for update` লক + `status <> 'PENDING'` হলে balance-touch-এর
                    // *আগেই* `ALREADY_RESPONDED` early-return (exception ছোঁড়ে না, সফল non-OK result)।
                    // IDEMPOTENT (উপরের status<>PENDING গার্ড atomic RPC-transaction-এর জন্য যথেষ্ট --
                    // ১ম কল সার্ভারে কমিট হয়ে থাকলে replay সবসময় ALREADY_RESPONDED-এ আটকাবে, টাকা
                    // দ্বিতীয়বার নড়বে না)। ⚠️ residual stale-replay ঝুঁকি (BLOCKED করা হয়নি, শুধু
                    // নথিভুক্ত, solverCancelJob-এর মতোই ক্লাস): ১ম কল আদৌ সার্ভারে না পৌঁছালে charge
                    // এখনো PENDING থাকে; failure ও replay-এর মাঝে সেই escrow (status='HELD') আলাদাভাবে
                    // release/refund হয়ে গেলে, replay guard পার হয়ে accept-এ wallet ঠিকই কাটবে কিন্তু
                    // কোনো HELD escrow না পেয়ে সেই টাকা কোনো escrow-এর extra_amount-এ যোগ হবে না।
                    enqueueOutboxRetry(
                        rpcName = "respond_additional_charge",
                        params = kotlinx.serialization.json.JsonObject(
                            buildMap {
                                put("chargeId", kotlinx.serialization.json.JsonPrimitive(charge.id))
                                put("accept", kotlinx.serialization.json.JsonPrimitive(accept))
                            }
                        ),
                        error = e
                    )
                }
        }

        val prob = problemDao.getProblemById(charge.problemId)
        if (prob != null) {
            val updatedProb = prob.copy(lastActivityAt = respondedAt)
            problemDao.updateProblem(updatedProb)
        }

        if (accept) {
            val user = userDao.getUserById(charge.userId)
            // [ব্যালেন্স ফিক্স — ধাপ ৪] আগে এখানে legacy shared user.balance পড়া হতো — extra
            // charge accept করা সবসময় owner/USER-role টাকা (charge.userId = problem owner),
            // তাই balanceUser-ই সঠিক সোর্স, ঠিক acceptBid()-এর একই ফিক্সের প্যাটার্নে।
            val userBalance = user?.balanceUser ?: 0.0
            val walletDeduction = if (userBalance > 0.0) minOf(userBalance, charge.amount) else 0.0
            if (walletDeduction > 0.0) {
                // INSTANT local debit (Room); the cloud side goes through the RPC dual-write
                // further below, with outbox retry on failure [Step 7.7 cleanup:
                // pendingCloudSync on the TransactionEntity below is an unread Firebase-era
                // leftover, not the real retry mechanism], instead of the old
                // incrementUserBalance() fire-and-forget (no retry if that single attempt failed).
                // [ব্যালেন্স ফিক্স] user হিসেবে extra charge কাটা হচ্ছে, balanceUser mirror-ও আপডেট হয়।
                userDao.deductBalanceForUserRole(charge.userId, walletDeduction, respondedAt)
            }
            // Bug fix (transaction-history + escrow-undercounting gap): this record used to be
            // nested inside `if (walletDeduction > 0.0)` and use `walletDeduction` as the amount,
            // and the escrow top-up right below used to add only `walletDeduction` too (an
            // earlier "fix" that kept escrow in sync with the wallet, but only by ignoring
            // whatever the gateway had collected). With a ৳0 wallet balance -- the default for
            // most users -- and the charge paid via gateway, walletDeduction was 0.0: no
            // transaction was ever recorded, AND escrow.extraAmount never received the
            // gateway-paid money either, so the solver's eventual payout would have been short by
            // exactly that amount. `charge.amount` (the full accepted charge, known regardless of
            // payment source) is used for both below; only the actual Room wallet debit above
            // stays limited to `walletDeduction`, since that's the only part that really left the
            // wallet balance.
            val deductionTrx = TransactionEntity(
                id = "TRX_EXTRA_CHARGE_${charge.id}",
                problemId = charge.problemId,
                problemTitle = prob?.title ?: "",
                userId = charge.userId,
                // solverId left blank on purpose -- see the identical note on
                // TRX_BID_DEDUCT_ above; this is a user-wallet-ledger deduction record, not
                // a solver earning (the solver never receives this directly -- it goes into
                // escrow and is paid out separately later as its own PAYMENT transaction).
                solverId = "",
                grossAmount = charge.amount,
                commissionPercent = 0.0,
                commissionAmount = 0.0,
                netAmount = -charge.amount,
                type = "EXTRA_CHARGE_DEDUCTION",
                pendingCloudSync = true,
                role = "USER"
            )
            transactionDao.insertTransaction(deductionTrx)
            addToEscrow(charge.problemId, charge.amount)
            applyCappedPerProblemReputation(
                userId = charge.solverId,
                eventType = "EXTRA_CHARGE_VIA_APP",
                rawChange = (1.0 + (charge.amount / 500.0 * 0.5)).coerceAtMost(3.0),
                problemId = charge.problemId,
                note = "App-এর মাধ্যমে অতিরিক্ত বিল আদায় করেছেন"
            )
            applyCappedPerProblemReputation(
                userId = charge.userId,
                eventType = "EXTRA_CHARGE_ACCEPTED",
                rawChange = 0.5,
                problemId = charge.problemId,
                note = "অতিরিক্ত বিল ন্যায্যভাবে গ্রহণ করেছেন"
            )
        }

        val formattedAmount = DistanceUtil.toBengaliDigits(charge.amount.toInt().toString())

        val notif = NotificationEntity(
            role = "SOLVER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = charge.solverId,
            title = if (accept) "অতিরিক্ত বিল অনুরোধ গৃহীত হয়েছে ✅" else "অতিরিক্ত বিল অনুরোধ প্রত্যাখ্যান হয়েছে ❌",
            message = "৳$formattedAmount বিলের অনুরোধ ${if (accept) "গৃহীত" else "প্রত্যাখ্যান"} হয়েছে।",
            targetType = "additional_charge",
            targetId = charge.id,
            relatedProblemId = charge.problemId
        )
        notificationDao.insertNotification(notif)

        // [SUPABASE-MIGRATED - ধাপ ১২ ফিক্স] notification-gap বন্ধ: `respond_additional_charge`
        // RPC কোনো notification insert করে না (ধাপ ১২ audit এ নিশ্চিত হওয়া গ্যাপ), তাই
        // `create_notification` RPC দিয়ে solver-কে accept/reject জানানো হচ্ছে -- উপরের
        // local Room NotificationEntity-এর সমতুল্য। caller (charge.userId, owner অথবা admin)
        // ও target (charge.solverId) দুজনেই charge.problemId এর party, তাই party-check pass করার
        // কথা (admin হলে এমনিতেও allowed)। Best-effort।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.createNotification(
                targetUserId = charge.solverId,
                title = notif.title,
                message = notif.message,
                targetType = "additional_charge",
                targetId = charge.id,
                relatedProblemId = charge.problemId,
                role = notif.role
            ).onFailure { e ->
                Log.w("SomadhanRepo", "respondToAdditionalCharge: createNotification dual-write failed for ${charge.id} (local flow unaffected): ${e.message}")
            }
        }

        val userName = prob?.userName ?: "গ্রাহক"
        val chatMessageContent = if (accept) {
            "✅ আমি ৳$formattedAmount অতিরিক্ত বিল অনুমোদন করেছি।"
        } else {
            "❌ আমি ৳$formattedAmount অতিরিক্ত বিলের অনুরোধ প্রত্যাখ্যান করেছি।"
        }

        sendMessage(
            problemId = charge.problemId,
            senderId = charge.userId,
            receiverId = charge.solverId,
            senderName = userName,
            content = chatMessageContent
        )
    }

    fun getPendingAdditionalCharges(problemId: String) = additionalChargeDao.getPendingByProblemId(problemId)

    fun getAllAdditionalCharges(problemId: String) = additionalChargeDao.getAllByProblemId(problemId)

    fun getRecentReputationEvents(userId: String, limit: Int = 20) = reputationEventDao.getRecentByUserId(userId, limit)
    fun getReputationHistory(userId: String) = reputationEventDao.getRecentByUserId(userId, 20)
    suspend fun getSumScoreChangeForProblem(userId: String, eventTypes: List<String>, problemId: String): Double =
        reputationEventDao.getSumScoreChangeForProblem(userId, eventTypes, problemId)

    suspend fun clearAllDatabaseAndReset(context: android.content.Context? = null) {
        // Clear all Room tables
        db.clearAllTables()

        // Wipe Firestore collections & Storage

        // [SUPABASE-MIGRATED - ধাপ ৩২.৯খ] নতুন RPC `admin_wipe_all_data()` (is_admin() চেক করে
        // users/problems/bids/... সব টেবিল wipe করে, auth.users অক্ষত রাখে -- বিস্তারিত
        // SupabaseSyncManager.adminWipeAllData()-এর KDoc-এ)। best-effort dual-run -- ব্যর্থ হলে
        // শুধু log, Firebase/local wipe flow অপ্রভাবিত।
        SupabaseSyncManager.adminWipeAllData()
            .onFailure { e ->
                Log.w("SomadhanRepo", "clearAllDatabaseAndReset: Supabase dual-run wipe failed (local Room wipe unaffected): ${e.message}")
            }

        // Re-seed default categories & base data
        db.seedInitialData()

        // Log critical audit entry
        try {
            logAdminAction(
                actionType = "FACTORY_RESET",
                targetId = "ALL_DATA",
                targetName = "SYSTEM",
                details = "অ্যাডমিন কর্তৃক সমস্ত ডেটাবেস এবং ক্লাউড ডেটা ফ্যাক্টরি রিসেট করা হয়েছে।"
            )
        } catch (_: Exception) {}
    }

    suspend fun applyCappedPerEventReputation(
        userId: String,
        eventType: String,
        defaultRawScore: Double,
        customScore: Double? = null,
        defaultDailyCap: Double = 2.0,
        problemId: String? = null,
        note: String
    ) {
        val scoreSettingKey = when (eventType) {
            "BID_WON" -> "rep_score_bid_won"
            "JOB_COMPLETED" -> "rep_score_job_completed"
            "PROBLEM_POSTED" -> "rep_score_problem_posted"
            "WITHDRAWAL_COMPLETED" -> "rep_score_withdrawal_completed"
            "RATING_BONUS" -> "rep_score_rating_5_star"
            else -> "rep_score_${eventType.lowercase()}"
        }
        val configuredScore = customScore ?: platformSettingDao.getSetting(scoreSettingKey)?.toDoubleOrNull() ?: defaultRawScore

        val capSettingKey = when (eventType) {
            "BID_WON" -> "rep_cap_daily_bid_won"
            "JOB_COMPLETED" -> "rep_cap_daily_job_completed"
            "PROBLEM_POSTED" -> "rep_cap_daily_problem_posted"
            "WITHDRAWAL_COMPLETED" -> "rep_cap_daily_withdrawal"
            "RATING_BONUS" -> "rep_cap_daily_rating"
            else -> "rep_cap_daily_${eventType.lowercase()}"
        }
        val dailyCap = platformSettingDao.getSetting(capSettingKey)?.toDoubleOrNull() ?: defaultDailyCap

        if (configuredScore <= 0.0) return

        val since = System.currentTimeMillis() - (24L * 60 * 60 * 1000)
        // Per-event daily cap query: checks only this eventType for the user in the past 24 hours
        val todaysGain = reputationEventDao.getSumScoreChangeSince(userId, listOf(eventType), since)
        if (todaysGain >= dailyCap) return // Daily cap reached for this event category

        val allowedChange = configuredScore.coerceAtMost((dailyCap - todaysGain).coerceAtLeast(0.0))
        if (allowedChange <= 0.0) return

        applyReputationChange(userId, eventType, allowedChange, problemId, note)
    }

    private suspend fun applyCappedActivityReputation(
        userId: String,
        eventType: String,
        scoreChange: Double,
        problemId: String?,
        note: String
    ) {
        applyCappedPerEventReputation(
            userId = userId,
            eventType = eventType,
            defaultRawScore = scoreChange,
            defaultDailyCap = 2.0,
            problemId = problemId,
            note = note
        )
    }

    /**
     * Universal Dynamic Reputation Trigger:
     * Evaluates any custom or suggested event configured dynamically in Admin Panel.
     * Supports both positive (+) bonus events and negative (-) penalty events with independent daily caps.
     */
    suspend fun triggerDynamicReputationEvent(
        userId: String,
        eventType: String,
        problemId: String? = null,
        defaultScore: Double = 0.5,
        defaultCap: Double = 2.0,
        defaultPenalty: Double = 1.0,
        isPositive: Boolean = true,
        customNote: String? = null
    ) {
        val normalizedKey = eventType.trim().uppercase()
        val scoreKey = "rep_score_${normalizedKey.lowercase()}"
        val capKey = "rep_cap_daily_${normalizedKey.lowercase()}"
        val penaltyKey = "rep_penalty_${normalizedKey.lowercase()}"
        val titleKey = "rep_title_${normalizedKey.lowercase()}"
        val typeKey = "rep_type_${normalizedKey.lowercase()}"
        val isPosKey = "rep_is_positive_${normalizedKey.lowercase()}"
        val statusKey = "rep_status_${normalizedKey.lowercase()}"
        val enabledKey = "rep_enabled_${normalizedKey.lowercase()}"

        val status = platformSettingDao.getSetting(statusKey)
        val enabled = platformSettingDao.getSetting(enabledKey)?.toBooleanStrictOrNull()
        val customEventsList = platformSettingDao.getSetting("rep_custom_events_list") ?: ""
        val isRegisteredInList = customEventsList.split(",").map { it.trim().uppercase() }.contains(normalizedKey)

        if (status == "PAUSED" || status == "DISABLED" || status == "DELETED" || enabled == false) return

        val configuredScore = platformSettingDao.getSetting(scoreKey)?.toDoubleOrNull()
        val configuredPenalty = platformSettingDao.getSetting(penaltyKey)?.toDoubleOrNull()

        // Only trigger if enabled, registered in custom events list, or explicitly configured
        val isConfigured = enabled == true || isRegisteredInList || configuredScore != null || configuredPenalty != null
        if (!isConfigured) return

        val effectiveIsPositive = platformSettingDao.getSetting(typeKey)?.toBooleanStrictOrNull()
            ?: platformSettingDao.getSetting(isPosKey)?.toBooleanStrictOrNull()
            ?: isPositive

        val eventTitle = platformSettingDao.getSetting(titleKey) ?: normalizedKey
        val note = customNote ?: eventTitle

        if (effectiveIsPositive) {
            val scoreToApply = configuredScore ?: defaultScore
            val dailyCap = platformSettingDao.getSetting(capKey)?.toDoubleOrNull() ?: defaultCap
            if (scoreToApply <= 0.0) return

            val since = System.currentTimeMillis() - (24L * 60 * 60 * 1000)
            val todaysGain = reputationEventDao.getSumScoreChangeSince(userId, listOf(normalizedKey), since)
            if (todaysGain >= dailyCap) return

            val allowedGain = scoreToApply.coerceAtMost((dailyCap - todaysGain).coerceAtLeast(0.0))
            if (allowedGain > 0.0) {
                applyReputationChange(userId, normalizedKey, allowedGain, problemId, note)
            }
        } else {
            val penaltyToApply = configuredPenalty
                ?: configuredScore
                ?: defaultPenalty
            if (penaltyToApply <= 0.0) return

            val dailyCap = platformSettingDao.getSetting(capKey)?.toDoubleOrNull() ?: defaultCap
            val actualPenalty: Double
            if (dailyCap > 0.0) {
                val since = System.currentTimeMillis() - (24L * 60 * 60 * 1000)
                val todaysLoss = Math.abs(reputationEventDao.getSumScoreChangeSince(userId, listOf(normalizedKey), since))
                if (todaysLoss >= dailyCap) return
                actualPenalty = penaltyToApply.coerceAtMost((dailyCap - todaysLoss).coerceAtLeast(0.0))
            } else {
                actualPenalty = penaltyToApply
            }
            if (actualPenalty > 0.0) {
                applyReputationChange(userId, normalizedKey, -actualPenalty, problemId, note)
            }
        }
    }

    private suspend fun trackExtraPaymentMissCycle(solverId: String, extraAmt: Double) {
        val ruleEnabled = platformSettingDao.getSetting("extra_payment_miss_rule_enabled")?.toBooleanStrictOrNull() ?: true
        if (!ruleEnabled) return

        val cycleSize = platformSettingDao.getSetting("extra_payment_miss_cycle_size")?.toIntOrNull() ?: 10
        val threshold = platformSettingDao.getSetting("extra_payment_miss_threshold")?.toIntOrNull() ?: 3
        val penalty = platformSettingDao.getSetting("extra_payment_miss_penalty")?.toDoubleOrNull() ?: 5.0

        val solver = userDao.getUserById(solverId) ?: return
        val newJobCount = solver.cycleJobCount + 1
        val newMissCount = solver.cycleMissCount + if (extraAmt <= 0.0) 1 else 0

        if (newJobCount >= cycleSize) {
            // সাইকেল পূর্ণ — মূল্যায়ন করো, তারপর রিসেট করো
            val updatedSolver = solver.copy(
                cycleJobCount = 0,
                cycleMissCount = 0,
                updatedAt = System.currentTimeMillis()
            )
            userDao.updateUser(updatedSolver)

            // [SUPABASE-MIGRATED - ধাপ ৩২.৭] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি। সাবধানতা: এই ফাংশন private, caller নিজে solver নাও হতে পারে
            // (job-completion flow problem-owner-এর ডিভাইস থেকেও ট্রিগার হতে পারে) — তাই guard শুধু session আছে কিনা তা, সুনির্দিষ্ট userId
            // ম্যাচ না (system_notify_48hour_auto_release-এর caller-scoping প্যাটার্নে, rule #১১)।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.systemTrackExtraPaymentMiss(solverId, updatedSolver.cycleJobCount, updatedSolver.cycleMissCount)
                    .onFailure { e ->
                        Log.w("SomadhanRepo", "trackExtraPaymentMissCycle: Supabase dual-write failed for $solverId (local flow unaffected): ${e.message}")
                        // [Step 12.7] outbox retry -- `system_track_extra_payment_miss` RPC absolute SET করে
                        // (increment না): ২য়বার চললে একই মান আবার বসে, কোনো টাকা/counter দ্বিগুণ হয় না।
                        enqueueOutboxRetry(
                            rpcName = "system_track_extra_payment_miss",
                            params = kotlinx.serialization.json.JsonObject(
                                mapOf(
                                    "solverId" to kotlinx.serialization.json.JsonPrimitive(solverId),
                                    "newJobCount" to kotlinx.serialization.json.JsonPrimitive(updatedSolver.cycleJobCount),
                                    "newMissCount" to kotlinx.serialization.json.JsonPrimitive(updatedSolver.cycleMissCount)
                                )
                            ),
                            error = e
                        )
                    }
            }

            if (newMissCount >= threshold) {
                applyReputationChange(
                    solverId,
                    "EXTRA_PAYMENT_MISS_CYCLE_PENALTY",
                    -penalty,
                    null,
                    "সর্বশেষ ${cycleSize}টি কাজের মধ্যে ${newMissCount}টিতে অতিরিক্ত বিল আদায় করতে পারেননি"
                )
            }
        } else {
            // সাইকেল চলমান — শুধু কাউন্টার আপডেট করো
            val updatedSolver = solver.copy(
                cycleJobCount = newJobCount,
                cycleMissCount = newMissCount,
                updatedAt = System.currentTimeMillis()
            )
            userDao.updateUser(updatedSolver)

            // [SUPABASE-MIGRATED - ধাপ ৩২.৭] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি। সাবধানতা: এই ফাংশন private, caller নিজে solver নাও হতে পারে
            // (job-completion flow problem-owner-এর ডিভাইস থেকেও ট্রিগার হতে পারে) — তাই guard শুধু session আছে কিনা তা, সুনির্দিষ্ট userId
            // ম্যাচ না (system_notify_48hour_auto_release-এর caller-scoping প্যাটার্নে, rule #১১)।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.systemTrackExtraPaymentMiss(solverId, updatedSolver.cycleJobCount, updatedSolver.cycleMissCount)
                    .onFailure { e ->
                        Log.w("SomadhanRepo", "trackExtraPaymentMissCycle: Supabase dual-write failed for $solverId (local flow unaffected): ${e.message}")
                        // [Step 12.7] outbox retry -- `system_track_extra_payment_miss` RPC absolute SET করে
                        // (increment না): ২য়বার চললে একই মান আবার বসে, কোনো টাকা/counter দ্বিগুণ হয় না।
                        enqueueOutboxRetry(
                            rpcName = "system_track_extra_payment_miss",
                            params = kotlinx.serialization.json.JsonObject(
                                mapOf(
                                    "solverId" to kotlinx.serialization.json.JsonPrimitive(solverId),
                                    "newJobCount" to kotlinx.serialization.json.JsonPrimitive(updatedSolver.cycleJobCount),
                                    "newMissCount" to kotlinx.serialization.json.JsonPrimitive(updatedSolver.cycleMissCount)
                                )
                            ),
                            error = e
                        )
                    }
            }
        }
    }

    private suspend fun applyCappedPerProblemReputation(
        userId: String,
        eventType: String,
        rawChange: Double,
        problemId: String,
        note: String
    ) {
        val capSetting = platformSettingDao.getSetting("extra_bill_reputation_cap_per_problem")?.toDoubleOrNull() ?: 10.0
        val alreadyGained = reputationEventDao.getSumScoreChangeForProblem(
            userId,
            listOf("EXTRA_CHARGE_VIA_APP", "EXTRA_CHARGE_ACCEPTED"),
            problemId
        )
        val allowedChange = (capSetting - alreadyGained).coerceIn(0.0, rawChange.coerceAtLeast(0.0))
        if (allowedChange > 0.0) {
            applyReputationChange(userId, eventType, allowedChange, problemId, note)
        }
    }

    suspend fun applyReputationChange(
        userId: String,
        eventType: String,
        scoreChange: Double,
        problemId: String? = null,
        note: String,
        // [BALANCE_REPUTATION_ROLE_SEPARATION ধাপ ৭খ] caller (admin UI)
        // এখন এক্সপ্লিসিট role পাস করতে পারে (adminAdjustBalance()-এর `role` প্যারামিটারের
        // ঠিক একই প্যাটার্নে) -- থাকলে নিচের `user.role`-ভিত্তিক অনুমানকে override করে,
        // না থাকলে (পুরনো caller, যেখানে এই প্যারামিটার নেই) আগের মতোই fallback অপরিবর্তিত রয়ে যায়।
        role: String? = null
    ) {
        val user = userDao.getUserById(userId) ?: return
        val newScore = (user.reputationScore + scoreChange).coerceIn(0.0, 100.0)
        val updated = user.copy(reputationScore = newScore, updatedAt = System.currentTimeMillis())
        userDao.updateUser(updated)

        // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ১] `user.role` থেকে গণনা করা হলো এখানেই
        // (নিচে RPC dual-write-এর জন্য আগে থেকে যে একই লজিক দিয়ে `reputationRole` গণনা হতো,
        // সেই ভ্যারিয়েবল-ডিক্লারেশন এখন এখানে তুলে আনা হলো — নিচে দ্বিতীয়বার গণনা না করে এই
        // একই ভ্যারিয়েবল পুনর্ব্যবহার হচ্ছে, ডুপ্লিকেট লজিক এড়াতে)। উপরের plain
        // `userDao.updateUser(updated)` কল (dual-write cutover না হওয়া পর্যন্ত) ইচ্ছাকৃতভাবে
        // অপরিবর্তিত/পাশাপাশি রাখা হলো (rule #৩) — শুধু তার পাশে role-scoped mirror যোগ করা
        // হলো, ঠিক adminAdjustBalance()-এ `adjustRole` যেভাবে গণনা/ব্যবহার হয় সেই প্যাটার্নে।
        // `newScore` এখানে একটা absolute (delta না) স্কোর, তাই DAO ফাংশন দুটো সরাসরি SET করে,
        // addBalanceForUserRole/SolverRole-এর মতো +/- করে না।
        //
        // fallback: role "USER"/"SOLVER" ছাড়া অন্য কিছু হলে (যেমন "ADMIN", বা অপ্রত্যাশিতভাবে
        // ফাঁকা/অজানা কোনো ভ্যালু) নিচের role-scoped mirror কল **করা হয় না** — তখন শুধু উপরের
        // shared/plain `reputationScore` write-টাই কার্যকর থাকে (পুরনো আচরণ অক্ষত)। এটা
        // ব্যবহারিকভাবে ঘটতে পারে যদি `applyReputationChange()` কখনো কোনো ADMIN-role ইউজারের
        // (বা ভবিষ্যতে যোগ হওয়া কোনো নতুন role value-র) ওপর কল হয় — বর্তমান কলার-লিস্টে
        // (applyCappedPerEventReputation, applyCappedPerProblemReputation,
        // triggerDynamicReputationEvent, adminAdjustReputation, runInactivityReputationDecay)
        // এমনটা ঘটার কথা না (সবগুলোই USER/SOLVER role-এর ইউজারকে টার্গেট করে), কিন্তু defensive
        // fallback হিসেবে রাখা হলো।
        // [ধাপ ৭খ ফিক্স] caller-এর এক্সপ্লিসিট `role` প্যারামিটার থাকলে (AdminUsersView-এর cardRole,
        // AdminUserLookupView-এর currentPerspectiveRole থেকে) সেটাই প্রাধান্য পায় -- adminAdjustBalance()-এর `adjustRole`
        // গণনার ঠিক একই প্যাটার্নে। এক্সপ্লিসিট পাস না হলে (পুরনো caller, যেমন স্বয়ংক্রিয়
        // reputation-event callerগুলো) fallback হিসেবে আগের মতোই user.role থেকে অনুমান অপরিবর্তিত রয়ে গেলো।
        val reputationRole = when (role) {
            "SOLVER" -> "SOLVER"
            "USER" -> "USER"
            else -> when (user.role) {
                "SOLVER" -> "SOLVER"
                "USER" -> "USER"
                else -> null
            }
        }
        when (reputationRole) {
            "SOLVER" -> userDao.updateReputationForSolverRole(userId, newScore, updated.updatedAt)
            "USER" -> userDao.updateReputationForUserRole(userId, newScore, updated.updatedAt)
            else -> { /* fallback: উপরের নোট দ্রষ্টব্য — শুধু shared-column write বহাল থাকে */ }
        }

        val event = ReputationEventEntity(
            id = "REP_${UUID.randomUUID().toString().take(8)}",
            userId = userId,
            eventType = eventType,
            problemId = problemId,
            scoreChange = scoreChange,
            scoreAfter = newScore,
            note = note
        )
        reputationEventDao.insert(event)

        // [SUPABASE-MIGRATED - ধাপ ১১, ধাপ ১২ প্রি-ফিক্সে guard আপডেট] `applyReputationChange()`
        // হলো সব reputation পরিবর্তনের একমাত্র কেন্দ্রীয় ফাংশন
        // (applyCappedPerEventReputation/applyCappedPerProblemReputation/
        // triggerDynamicReputationEvent/adminAdjustReputation/runInactivityReputationDecay সবাই
        // এখানে এসে মেলে) — তাই dual-write এখানেই একবার যোগ করা হলো।
        //
        // guard (ধাপ ১২ প্রি-ফিক্সের পর): শুধু session থাকলেই যথেষ্ট (RPC নিজেই authorization/
        // eligibility চূড়ান্তভাবে যাচাই করে)। আগে এখানে non-ADMIN_ADJUSTMENT event-এ
        // `currentUserId() == userId` বাধ্যতামূলক ছিল, যার ফলে counterparty/admin অন্য কারো পক্ষে
        // (যেমন problem owner-এর session থেকে solver-এর BID_WON, admin-এর session থেকে
        // solver-এর WITHDRAWAL_COMPLETED) trigger হওয়া event dual-write হতোই না — RPC নিজে
        // authorize করার আগেই local guard skip করে দিত। `submit_reputation_event` RPC-এর
        // authorization মডেল এখন p_user_id (target)-ভিত্তিক eligibility + caller
        // (self/counterparty/admin) authorization আলাদাভাবে চেক করে (Supabase MCP দিয়ে migrate
        // করে যাচাই করা হয়েছে), তাই local guard আর caller==userId ম্যাচ করানোর দরকার নেই — RPC
        // ব্যর্থ হলে (NOT_ELIGIBLE ইত্যাদি) শুধু log হবে, local flow অপ্রভাবিত থাকবে।
        //
        // ⚠️ জানা সীমাবদ্ধতা [ধাপ ৩৩.১-এ আপডেট]: `submit_reputation_event` RPC এখন নয়টা event
        // type সমর্থন করে (ADMIN_ADJUSTMENT, BID_WON, JOB_COMPLETED, PROBLEM_POSTED, RATING_BONUS,
        // WITHDRAWAL_COMPLETED, EXTRA_CHARGE_VIA_APP/EXTRA_CHARGE_ACCEPTED, এবং এখন
        // INACTIVE_7_DAYS/INACTIVE_30_DAYS — migration step33_1_reputation_event_inactive_decay_support_6arg,
        // Supabase MCP দিয়ে verify করা হয়েছে DB-তে আগে থেকেই প্রয়োগ করা ছিল, তাই নতুন migration
        // বানানো হয়নি)। এই দুইটাসহ role এখন নিচে সবসময় user.role থেকে derive করে পাঠানো হয় বলে
        // runInactivityReputationDecay()-এর dual-write ইতিমধ্যে কার্যকর — আলাদা কোনো call-site
        // পরিবর্তন লাগেনি। বাকি (RATING_PENALTY, UNRESPONSIVE_CHAT, EXTRA_PAYMENT_MISS_CYCLE_PENALTY,
        // বা Admin Panel-এর dynamic custom event) পাঠালে RPC এখনো `UNSUPPORTED_EVENT_TYPE`
        // exception ছুঁড়বে, যা শুধু log হয় — local flow সম্পূর্ণ অপ্রভাবিত থাকে
        // (SupabaseSyncManager.submitReputationEvent()-এর KDoc-এ বিস্তারিত)।
        // [SUPABASE-MIGRATED - ধাপ ১৪.৫ (Kotlin wiring, উপ-ধাপ "ঘ")] role: উপরের `user`
        // (`userDao.getUserById(userId)`)-এর নিজস্ব `.role` ফিল্ড থেকেই সরাসরি জানা যায় (dual-row
        // architecture-এ এই userId-টাই একটা নির্দিষ্ট role-এর row) — এটা RPC-এর নিজস্ব per-event
        // role-derivation-এর (BID_WON/RATING_BONUS/WITHDRAWAL_COMPLETED→SOLVER,
        // PROBLEM_POSTED→USER, EXTRA_CHARGE_VIA_APP→SOLVER, EXTRA_CHARGE_ACCEPTED→USER,
        // JOB_COMPLETED→contextual) সাথেই মিলে যায়, কারণ প্রতিটা call-site-এই userId ইতিমধ্যে
        // সঠিক role-এর row (solverId বা userId/problem owner)। শুধু **`ADMIN_ADJUSTMENT`**-এ RPC
        // আসলে `p_role` ব্যবহার করে (বাধ্যতামূলক) — বাকি ৬টা সমর্থিত event type-এ RPC নিজেই
        // structurally/contextually derive করে, `p_role` পাঠানো ঐচ্ছিক/নিরাপদ (ignore হয়)।
        // [ধাপ ৩৩.১] INACTIVE_7_DAYS/INACTIVE_30_DAYS-এ RPC এখন `p_role` আসলে ব্যবহার করে এবং
        // বাধ্যতামূলক (ADMIN_ADJUSTMENT-এর মতোই) — উপরের reputationRole সেই দুইটাতেও সঠিকভাবে যায়।
        // এখনো অসমর্থিত event type (RATING_PENALTY ইত্যাদি)-এ role পাঠানোর কোনো প্রভাব নেই (RPC
        // তার আগেই UNSUPPORTED_EVENT_TYPE ছোঁড়ে)। "ADMIN" role হলে (এই কনটেক্সটে হওয়ার কথা না)
        // p_role পাঠানো হয় না।
        // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ১] `reputationRole` এখন ফাংশনের শুরুতেই
        // (local DAO mirror-এর জন্য) একবার গণনা হয়ে গেছে — এখানে সেই একই ভ্যারিয়েবল
        // পুনর্ব্যবহার হচ্ছে, দ্বিতীয়বার গণনা করা হচ্ছে না (আগে এখানে আলাদাভাবে গণনা হতো)।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.submitReputationEvent(
                userId = userId,
                eventType = eventType,
                refId = problemId,
                scoreChange = scoreChange,
                note = note,
                role = reputationRole
            ).onFailure { e ->
                Log.w("SomadhanRepo", "applyReputationChange: Supabase dual-write failed for ${event.id} (event=$eventType, local flow unaffected): ${e.message}")
            }
        }
    }

    suspend fun runMonthlyFreeQuotaReset() {
        val currentMonthKey = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date())
        val allUsers = userDao.getAllUsersOnce()
        allUsers.forEach { user ->
            val updated = user.copy(freeJobsUsedThisMonth = 0, freeJobsMonthKey = currentMonthKey, updatedAt = System.currentTimeMillis())
            userDao.updateUser(updated)
        }

        // [SUPABASE-MIGRATED - ধাপ ৩২.৭] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি।
        // RPC নিজেই bulk (সব ইউজারের ওপর একটা কলেই) — উপরের client-side loop-এর ভেতরে না,
        // লুপের বাইরে/পরে একবারই কল করা হচ্ছে যাতে N-বার RPC কল না হয় (rule #১৩)। RPC নিজেই
        // `is_admin(auth.uid())` চেক করে।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminBulkResetFreeJobQuota(monthKey = currentMonthKey)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "runMonthlyFreeQuotaReset: Supabase dual-write failed (local flow unaffected): ${e.message}")
                }
        }
        logAdminAction(
            actionType = "RESET_MONTHLY_FREE_QUOTA",
            targetId = "GLOBAL",
            targetName = "All Solvers",
            details = "সকল সলভারের মাসিক ফ্রি কোটা সফলভাবে রিসেট করা হয়েছে ($currentMonthKey)"
        )
    }

    suspend fun adminResetSolverFreeQuota(solverId: String) {
        val currentMonthKey = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date())
        val user = userDao.getUserById(solverId) ?: return
        val updated = user.copy(
            freeJobsUsedThisMonth = 0,
            freeJobsMonthKey = currentMonthKey,
            updatedAt = System.currentTimeMillis()
        )
        userDao.updateUser(updated)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৭] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি। RPC
        // নিজেই `is_admin(auth.uid())` চেক করে (adminAdjustBalance()-এর established প্যাটার্নে)।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminResetFreeJobQuota(solverId)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "adminResetSolverFreeQuota: Supabase dual-write failed for $solverId (local flow unaffected): ${e.message}")
                }
        }
        logAdminAction(
            actionType = "RESET_SOLVER_FREE_QUOTA",
            targetId = solverId,
            targetName = user.name,
            details = "সলভারের মাসিক ব্যবহৃত ফ্রি কোটা ০-এ রিসেট করা হয়েছে ($currentMonthKey)"
        )
    }

    suspend fun adminResetSolverMissCycle(solverId: String) {
        val user = userDao.getUserById(solverId) ?: return
        val updated = user.copy(
            cycleJobCount = 0,
            cycleMissCount = 0,
            updatedAt = System.currentTimeMillis()
        )
        userDao.updateUser(updated)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৭] best-effort dual-write, Firebase পাশ স্পর্শ করা হয়নি। RPC নিজেই
        // `is_admin(auth.uid())` চেক করে (adminResetSolverFreeQuota()-এ যেভাবে করা হয়েছে, হুবহু সেই প্যাটার্নে)।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminResetMissCycle(solverId)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "adminResetSolverMissCycle: Supabase dual-write failed for $solverId (local flow unaffected): ${e.message}")
                }
        }
        logAdminAction(
            actionType = "RESET_SOLVER_MISS_CYCLE",
            targetId = solverId,
            targetName = user.name,
            details = "সলভারের Extra Bill মিস-সাইকেল কাউন্টার রিসেট করা হয়েছে"
        )
    }

    suspend fun searchSolverDirectFromCloudOrLocal(query: String): UserEntity? {
        val clean = query.trim()
        if (clean.isBlank()) return null

        // [SUPABASE-MIGRATED - E.164 ফলো-আপ ফিক্স ২] `clean` phone/UID/name যেকোনো কিছু হতে পারে,
        // তাই সরাসরি normalize না করে আলাদা ভ্যারিয়েবলে রাখা হলো — শুধু phone-lookup-এ ব্যবহারের জন্য।
        // normalizeTarget() phone না হলে (UID/name/email) প্রায় no-op বা harmless lowercase করে।
        val normalizedForPhoneSearch = com.example.util.OtpService.normalizeTarget(clean)

        // 1. Try local Room first - specifically look for solver profile (role == "SOLVER")
        val allLocal = userDao.getAllUsersList()
        val localSolver = allLocal.find { u ->
            u.role.equals("SOLVER", ignoreCase = true) && (
                u.displayUid.equals(clean, ignoreCase = true) ||
                (u.phone.equals(clean, ignoreCase = true) || u.phone.equals(normalizedForPhoneSearch, ignoreCase = true)) ||
                u.id.equals(clean, ignoreCase = true) ||
                u.name.contains(clean, ignoreCase = true)
            )
        } ?: allLocal.find { u ->
            (u.hasSolverRole && !u.role.equals("USER", ignoreCase = true)) && (
                u.displayUid.equals(clean, ignoreCase = true) ||
                (u.phone.equals(clean, ignoreCase = true) || u.phone.equals(normalizedForPhoneSearch, ignoreCase = true)) ||
                u.id.equals(clean, ignoreCase = true) ||
                u.name.contains(clean, ignoreCase = true)
            )
        }

        // 2. The old Firebase-based "freshest live solver data" fetch that used to live here
        // (a dangling `cloudSolver` reference with no declaration -- leftover from an incomplete
        // Firebase-to-Supabase migration pass) has been removed. It's fully superseded by the
        // Supabase-based dual-run fallback immediately below (getUserById() then
        // getUserByPhone()), which does the same job.

        // [SUPABASE-MIGRATED - ধাপ ৩২.৯গ] Firebase fallback ব্যর্থ/null হলে (উপরে) Supabase-ভিত্তিক
        // fallback dual-run হিসেবে চেষ্টা করা হয় -- getUserById() তারপর getUserByPhone()
        // (`users_select_own_or_admin` RLS: caller admin হলেই cross-user read কাজ করবে, আর এই
        // ফাংশনের একমাত্র caller `searchSolverForQuota()` সবসময় admin-only quota-reset স্ক্রিন
        // থেকেই আসে)। role check "SOLVER" আগের মতোই রাখা হয়েছে।
        try {
            val byId = SupabaseSyncManager.getUserById(clean).getOrNull()
            if (byId != null && (byId.role.equals("SOLVER", ignoreCase = true) || byId.hasSolverRole)) {
                val entity = byId.toUserEntity()
                userDao.insertUser(entity)
                return entity
            }
            val byPhone = SupabaseSyncManager.getUserByPhone(normalizedForPhoneSearch).getOrNull()
            if (byPhone != null && (byPhone.role.equals("SOLVER", ignoreCase = true) || byPhone.hasSolverRole)) {
                val entity = byPhone.toUserEntity()
                userDao.insertUser(entity)
                return entity
            }
        } catch (e: Exception) {
            Log.w("SomadhanRepo", "searchSolverDirectFromCloudOrLocal: Supabase fallback failed: ${e.message}")
        }

        return localSolver
    }

    suspend fun runInactivityReputationDecay() {
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val allUsers = userDao.getAllUsersList()
        for (user in allUsers) {
            val inactiveDuration = now - user.updatedAt
            val alreadyChecked = now - user.lastReputationDecayCheckAt
            if (alreadyChecked < sevenDaysMs) continue // একই সপ্তাহে দুইবার চেক না করার জন্য

            val inact7Penalty = platformSettingDao.getSetting("rep_penalty_inactive_7d")?.toDoubleOrNull() ?: 2.0
            val inact30Penalty = platformSettingDao.getSetting("rep_penalty_inactive_30d")?.toDoubleOrNull() ?: 5.0

            if (inactiveDuration >= thirtyDaysMs) {
                applyReputationChange(user.id, "INACTIVE_30_DAYS", -inact30Penalty, null, "৩০ দিন নিষ্ক্রিয় থাকার কারণে রেপুটেশন হ্রাস")
            } else if (inactiveDuration >= sevenDaysMs) {
                applyReputationChange(user.id, "INACTIVE_7_DAYS", -inact7Penalty, null, "৭ দিন নিষ্ক্রিয় থাকার কারণে রেপুটেশন হ্রাস")
            }
            val updatedUser = userDao.getUserById(user.id)
            if (updatedUser != null) {
                val decayCheckedUser = updatedUser.copy(lastReputationDecayCheckAt = now)
                userDao.updateUser(decayCheckedUser)
                // [SUPABASE-MIGRATED - ধাপ ৩৩.৩ কাজ ৫-৭] score change (থাকলে) আর
                // last_reputation_decay_check_at দুটোই ইতিমধ্যে Supabase-এ পৌঁছায় উপরের
                // applyReputationChange() কলের ভেতরের dual-write দিয়ে (RPC নিজেই
                // last_reputation_decay_check_at সেট করে -- দেখুন submit_reputation_event-এর
                // INACTIVE_7_DAYS/INACTIVE_30_DAYS ব্র্যাঞ্চ)। আগে এখানে
                // FirebaseSyncManager.syncUser(decayCheckedUser) কল ছিল যেটা শুধু একটা
                // সমান্তরাল Firestore-সাইড বুককিপিং মিরর ছিল -- এখন সরানো হলো (FirebaseSyncManager
                // ডিলিট হওয়ায়), Supabase-সাইড ডেটা অপ্রভাবিত থাকে।
            }
        }

        // Trigger dynamic suggested event: UNRESPONSIVE_CHAT
        val allActiveProblems = problemDao.getAllProblems().firstOrNull()?.filter { it.status == "IN_PROGRESS" && !it.acceptedSolverId.isNullOrBlank() } ?: emptyList()
        val oneDayMs = 24L * 60 * 60 * 1000L
        for (prob in allActiveProblems) {
            val solverId = prob.acceptedSolverId ?: continue
            val lastSeen = prob.solverLastSeenAt ?: prob.lastActivityAt ?: prob.createdAt
            if (now - lastSeen >= oneDayMs) {
                triggerDynamicReputationEvent(
                    userId = solverId,
                    eventType = "UNRESPONSIVE_CHAT",
                    problemId = prob.id,
                    defaultScore = 1.0,
                    defaultCap = 2.0,
                    defaultPenalty = 1.0,
                    isPositive = false,
                    customNote = "চলমান কাজে চ্যাটে ২৪ ঘণ্টার বেশি নিষ্ক্রিয় থাকার দায়ে পেনাল্টি"
                )
            }
        }
    }

    suspend fun reportAbuse(
        reporterId: String,
        reportedUserId: String,
        reason: String,
        details: String = "",
        contextType: String? = null,
        relatedProblemId: String? = null
    ) {
        val reportedUser = userDao.getUserById(reportedUserId)
        val reporterUser = userDao.getUserById(reporterId)
        val ctx = if (!contextType.isNullOrBlank()) " [উৎস: $contextType]" else ""
        val det = if (details.isNotBlank()) ", বিবরণ: $details" else ""
        val prob = if (!relatedProblemId.isNullOrBlank()) ", সম্পর্কিত সমস্যা: $relatedProblemId" else ""
        logAdminAction(
            actionType = "USER_REPORT",
            targetId = reportedUserId,
            targetName = reportedUser?.name ?: reportedUserId,
            details = "অভিযোগকারী: ${reporterUser?.name ?: reporterId} ($reporterId), কারণ: $reason$det$ctx$prob"
        )

        // Dynamic suggested events: OFFLINE_PAYMENT_ATTEMPT & CLIENT_RUDE_BEHAVIOR
        if (reason.contains("offline", ignoreCase = true) || reason.contains("বাইরে") || reason.contains("নগদ লেনদেন") || details.contains("offline", ignoreCase = true) || details.contains("বাইরে")) {
            triggerDynamicReputationEvent(
                userId = reportedUserId,
                eventType = "OFFLINE_PAYMENT_ATTEMPT",
                problemId = relatedProblemId,
                defaultScore = 15.0,
                defaultCap = 30.0,
                defaultPenalty = 15.0,
                isPositive = false,
                customNote = "অ্যাপের বাইরে সরাসরি লেনদেনের চেষ্টার দায়ে পেনাল্টি"
            )
        }

        if (reason.contains("গালাগালি") || reason.contains("অসদাচরণ") || reason.contains("খারাপ ব্যবহার") || reason.contains("rude", ignoreCase = true) || reason.contains("harass", ignoreCase = true) || details.contains("গালাগালি") || details.contains("অসদাচরণ")) {
            triggerDynamicReputationEvent(
                userId = reportedUserId,
                eventType = "CLIENT_RUDE_BEHAVIOR",
                problemId = relatedProblemId,
                defaultScore = 3.0,
                defaultCap = 6.0,
                defaultPenalty = 3.0,
                isPositive = false,
                customNote = "চ্যাট বা কলে অসদাচরণের দায়ে পেনাল্টি"
            )
        }
    }

    suspend fun adminUpdateDirectContractStatus(problemId: String, status: String, directContractStatus: String) {
        val problem = problemDao.getProblemById(problemId) ?: return

        if (status == "COMPLETED" && problem.status != "COMPLETED") {
            // Automatically complete problem and credit solver wallet balance via escrow release
            confirmReleaseAndComplete(problem, includeExtraAmount = false)
            val completedProblem = problemDao.getProblemById(problemId) ?: problem
            val updated = completedProblem.copy(
                directContractStatus = directContractStatus,
                lastActivityAt = System.currentTimeMillis()
            )
            problemDao.updateProblem(updated)
        } else if (status == "CANCELLED" && problem.status != "CANCELLED") {
            adminCancelAndRefundDirectContract(problemId, "অ্যাডমিন কর্তৃক স্ট্যাটাস বাতিল করা হয়েছে")
            return
        } else {
            val updated = problem.copy(
                status = status,
                directContractStatus = directContractStatus,
                lastActivityAt = System.currentTimeMillis()
            )
            problemDao.updateProblem(updated)
        }

        val notifClient = NotificationEntity(
            role = "USER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = problem.userId,
            title = "ডাইরেক্ট চুক্তি স্ট্যাটাস আপডেট",
            message = "\"${problem.title}\" ডাইরেক্ট চুক্তির স্ট্যাটাস পরিবর্তন করে '$status' করা হয়েছে।",
            targetType = "problem",
            targetId = problem.id,
            relatedProblemId = problem.id
        )
        notificationDao.insertNotification(notifClient)

        if (!problem.acceptedSolverId.isNullOrBlank()) {
            val notifSolver = NotificationEntity(
                role = "SOLVER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = problem.acceptedSolverId,
                title = "ডাইরেক্ট চুক্তি স্ট্যাটাস আপডেট",
                message = "\"${problem.title}\" ডাইরেক্ট চুক্তির স্ট্যাটাস পরিবর্তন করে '$status' করা হয়েছে।",
                targetType = "problem",
                targetId = problem.id,
                relatedProblemId = problem.id
            )
            notificationDao.insertNotification(notifSolver)
        }

        // [SUPABASE-MIGRATED - dhap 12 (mul kaj, batch 2)] `admin_update_direct_contract_status`
        // RPC (ei session-e notun migration diye toiri) is_admin() check kore, COMPLETED/CANCELLED/
        // onnanno case onujayi escrow release/refund (existing release_escrow/refund_escrow_once
        // RPC nije call kore) + problem status update kore, ar duipokkhoke-i notify kore (upor-er
        // local notif duitor somotulyo) -- alada createNotification() lagbe na. Jana simaboddhota:
        // local confirmReleaseAndComplete(includeExtraAmount = false)-er extra-amount baad deoyar
        // sukkho logic-ta RPC-te hubohu protifolito na (RPC puro escrow, base+extra, release kore)
        // -- ei dual-write best-effort, local Room+Firebase-i ekhono source of truth, tai end-user
        // experience oprovabito. guard: shudhu session ache kina (RPC nijei admin authorize kore).
        // (ei block CANCELLED pothe pouchay na, karon oi poth upore agei return kore -- shei
        // dual-write adminCancelAndRefundDirectContract()-er nijer moddhei ache.)
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminUpdateDirectContractStatus(problemId, status, directContractStatus).onFailure { e ->
                Log.w("SomadhanRepo", "adminUpdateDirectContractStatus: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
                // [Step 12.7] outbox retry -- RPC-র টাকা-সংক্রান্ত পথ (release_escrow/refund_escrow_once)
                // শুধু `status = HELD` escrow পেলে চলে, তাই ২য়বার চললে escrow আর HELD থাকে না =
                // টাকা দ্বিতীয়বার নড়ে না। CANCELLED পথ `status <> CANCELLED` গার্ডেড।
                enqueueOutboxRetry(
                    rpcName = "admin_update_direct_contract_status",
                    params = kotlinx.serialization.json.JsonObject(
                        mapOf(
                            "problemId" to kotlinx.serialization.json.JsonPrimitive(problemId),
                            "status" to kotlinx.serialization.json.JsonPrimitive(status),
                            "directContractStatus" to kotlinx.serialization.json.JsonPrimitive(directContractStatus)
                        )
                    ),
                    error = e
                )
            }
        }

        logAdminAction(
            actionType = "UPDATE_DIRECT_CONTRACT_STATUS",
            targetId = problemId,
            targetName = problem.title,
            details = "নতুন সমস্যা স্ট্যাটাস: $status, চুক্তি স্ট্যাটাস: $directContractStatus"
        )
    }

    suspend fun adminCancelAndRefundDirectContract(problemId: String, reason: String) {
        val problem = problemDao.getProblemById(problemId) ?: return
        val updated = problem.copy(
            status = "CANCELLED",
            directContractStatus = "DECLINED",
            lastActivityAt = System.currentTimeMillis()
        )
        problemDao.updateProblem(updated)

        // মানি-ফ্লো ফিক্স, ধাপ ৩ — problem status/directContractStatus আগে শুধু local Room-এ
        // থেকে যেত, cloud dual-write ছিল না (অন্য ডিভাইসে/অন্য পার্টির কাছে এই বাতিল হওয়া কখনো
        // sync হতো না)। `adminUpdateDirectContractStatus` RPC আগে থেকেই ছিল (client wrapper),
        // ঠিক এই status+directContractStatus কম্বিনেশনের জন্যই বানানো -- শুধু এখানে কল করা হচ্ছিল
        // না। best-effort: ব্যর্থ হলেও local flow অপ্রভাবিত।
        // ⚠️ নোট: এই zip-এ `supabase/migrations` ফোল্ডারে `admin_update_direct_contract_status`
        // RPC-এর সংজ্ঞা (CREATE FUNCTION) পাওয়া যায়নি -- migrations ফোল্ডারটা আংশিক (step20+ থেকে
        // শুরু, আগের ধাপগুলো এই zip-এ নেই) হওয়ায় এটা নিশ্চিত করা যায়নি যে RPC-টা আসল DB-তে সত্যিই
        // আছে কিনা। Deploy করার আগে Supabase dashboard-এ (Database → Functions) গিয়ে
        // `admin_update_direct_contract_status` ফাংশনটা আসলে আছে কিনা যাচাই করে নাও।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminUpdateDirectContractStatus(
                problemId = problemId,
                status = "CANCELLED",
                directContractStatus = "DECLINED"
            ).onFailure { e ->
                Log.w("SomadhanRepo", "adminCancelAndRefundDirectContract: problem-status dual-write failed for $problemId (local flow unaffected): ${e.message}")
                // [Step 12.7] outbox retry -- এখানে প্যারামিটার ধ্রুবক (CANCELLED/DECLINED)। replay-এ
                // problem ইতিমধ্যে CANCELLED থাকলে RPC-র else-শাখা শুধু একই মান আবার SET করে, কোনো
                // refund দ্বিতীয়বার হয় না। escrow refund এই ফাংশনের আলাদা refundEscrowOnce পথে।
                enqueueOutboxRetry(
                    rpcName = "admin_update_direct_contract_status",
                    params = kotlinx.serialization.json.JsonObject(
                        mapOf(
                            "problemId" to kotlinx.serialization.json.JsonPrimitive(problemId),
                            "status" to kotlinx.serialization.json.JsonPrimitive("CANCELLED"),
                            "directContractStatus" to kotlinx.serialization.json.JsonPrimitive("DECLINED")
                        )
                    ),
                    error = e
                )
            }
        }

        // If escrow was held for this problem, refund it — সরাসরি নিচু-স্তরের refundEscrowOnce()
        // কল, ঠিক adminForceCancelInstantJob()-এর প্যাটার্নে (শুধু টাকা ফেরত দেয়, কোনো reputation
        // penalty/notification/problem-reset side-effect নেই)। আগে এখানে adminRefundEscrow(escrow)
        // কল হতো, যেটা ভুলভাবে সলভারের reputation-এ পেনাল্টি বসাতো ("সাড়া দেননি" ধরে নিয়ে), problem-কে
        // এখনই CANCELLED করার পরও আবার OPEN বানিয়ে দিতো, আর নিচের notifClient/notifSolver-এর সাথে
        // বিরোধী ডাবল নোটিফিকেশন পাঠাতো।
        val escrow = escrowDao.getByProblemId(problemId)
        if (escrow != null && escrow.status == "HELD") {
            refundEscrowOnce(
                problemId = escrow.problemId,
                amount = escrow.baseAmount + escrow.extraAmount,
                userId = escrow.userId,
                solverId = escrow.solverId,
                problemTitle = escrow.problemTitle,
                baseAmount = escrow.baseAmount,
                extraAmount = escrow.extraAmount,
                escrowId = escrow.id,
                refundType = "ADMIN_DIRECT_CONTRACT_CANCEL"
            )
        }

        val notifClient = NotificationEntity(
            role = "USER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = problem.userId,
            title = "ডাইরেক্ট চুক্তি বাতিল ও রিফান্ড",
            message = "\"${problem.title}\" চুক্তিটি অ্যাডমিন কর্তৃক বাতিল করা হয়েছে। কারণ: $reason",
            targetType = "problem",
            targetId = problem.id,
            relatedProblemId = problem.id
        )
        notificationDao.insertNotification(notifClient)

        // মানি-ফ্লো ফিক্স, ধাপ ৩ — এই notification-ও আগে শুধু local Room-এ থেকে যেত। এখানে
        // `adminSendMessageToProblemChat`-এর মতোই `create_notification` RPC দিয়ে dual-write করা
        // হলো (best-effort, ব্যর্থ হলে local flow অপ্রভাবিত)।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.createNotification(
                targetUserId = notifClient.userId,
                title = notifClient.title,
                message = notifClient.message,
                targetType = notifClient.targetType,
                targetId = notifClient.targetId,
                relatedProblemId = notifClient.relatedProblemId,
                role = notifClient.role
            ).onFailure { e ->
                Log.w("SomadhanRepo", "adminCancelAndRefundDirectContract: createNotification(owner) dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }

        if (!problem.acceptedSolverId.isNullOrBlank()) {
            val notifSolver = NotificationEntity(
                role = "SOLVER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = problem.acceptedSolverId,
                title = "ডাইরেক্ট চুক্তি বাতিল",
                message = "\"${problem.title}\" চুক্তিটি অ্যাডমিন কর্তৃক বাতিল করা হয়েছে। কারণ: $reason",
                targetType = "problem",
                targetId = problem.id,
                relatedProblemId = problem.id
            )
            notificationDao.insertNotification(notifSolver)

            // মানি-ফ্লো ফিক্স, ধাপ ৩ — solver-এর notification-এর জন্যও একই dual-write।
            if (SupabaseAuthManager.currentUserId() != null) {
                SupabaseSyncManager.createNotification(
                    targetUserId = notifSolver.userId,
                    title = notifSolver.title,
                    message = notifSolver.message,
                    targetType = notifSolver.targetType,
                    targetId = notifSolver.targetId,
                    relatedProblemId = notifSolver.relatedProblemId,
                    role = notifSolver.role
                ).onFailure { e ->
                    Log.w("SomadhanRepo", "adminCancelAndRefundDirectContract: createNotification(solver) dual-write failed for $problemId (local flow unaffected): ${e.message}")
                }
            }
        }

        // [ধাপ ১খ ফিক্স] এখানে আগে একটা নতুন `admin_cancel_and_refund_direct_contract` RPC কল হতো,
        // যেটা refundEscrowOnce()-এর সাথে ডুপ্লিকেট escrow-রিফান্ড ট্রাই করতো আর উপরের notifClient/
        // notifSolver-এর সাথে বিরোধী নিজস্ব নোটিফিকেশনও পাঠাতো (মাস্টার প্রম্পটের স্কোপে ছিল না)।
        // মাস্টার প্রম্পটের ১খ স্পেক অনুযায়ী এই ফাংশনের একমাত্র প্রয়োজন ছিল refund call-site ফিক্স
        // (উপরে হয়ে গেছে) — তাই এই dual-write ব্লকটা সরিয়ে ফেলা হলো। RPC-টা Supabase-এ থেকে যাচ্ছে
        // (অব্যবহৃত), কোনো destructive migration চালানো হয়নি।

        // Post chat message notice
        sendMessage(
            problemId = problemId,
            senderId = "ADMIN",
            receiverId = problem.userId,
            senderName = "অ্যাডমিন সিদ্ধান্ত 🛡️",
            content = "⚠️ অ্যাডমিন কর্তৃক সরাসরি চুক্তিটি বাতিল ও রিফান্ড করা হয়েছে। কারণ: $reason"
        )

        // মানি-ফ্লো ফিক্স, ধাপ ৩ — উপরের sendMessage()-এ senderId="ADMIN" থাকায় ওই ফাংশনের
        // `SupabaseAuthManager.currentUserId() == senderId` guard কখনো true হয় না (আসল admin
        // auth uid কখনো literal string "ADMIN" এর সমান হবে না) -- ফলে এই chat notice-টা আগে
        // পুরোপুরি local-only থেকে যেত, user/solver-এর ডিভাইসে কখনো পৌঁছাত না।
        // `adminSendMessageToProblemChat()`-এ ব্যবহৃত একই is_admin()-checked RPC দিয়ে এখানে
        // আলাদাভাবে cloud dual-write করা হলো। (RPC নিজস্ব sender label/format ব্যবহার করতে পারে,
        // তাই cloud-এ দেখানো sender name local কপির "অ্যাডমিন সিদ্ধান্ত 🛡️"-এর চেয়ে ভিন্ন হতে পারে
        // -- content/বার্তা একই থাকে, শুধু cosmetic sender-label পার্থক্য থাকতে পারে।)
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminSendMessageToProblemChat(
                problemId,
                "⚠️ অ্যাডমিন কর্তৃক সরাসরি চুক্তিটি বাতিল ও রিফান্ড করা হয়েছে। কারণ: $reason"
            ).onFailure { e ->
                Log.w("SomadhanRepo", "adminCancelAndRefundDirectContract: chat-notice dual-write failed for $problemId (local flow unaffected): ${e.message}")
            }
        }

        logAdminAction(
            actionType = "CANCEL_DIRECT_CONTRACT",
            targetId = problemId,
            targetName = problem.title,
            details = "চুক্তি বাতিল ও রিফান্ড। কারণ: $reason"
        )
    }

    suspend fun adminEscalateDirectContract(problemId: String, note: String) {
        val problem = problemDao.getProblemById(problemId) ?: return
        logAdminAction(
            actionType = "ESCALATE_DIRECT_CONTRACT",
            targetId = problemId,
            targetName = problem.title,
            details = "অ্যাডমিন তদন্ত ও এসকেলেশন নোট: $note"
        )
    }

    suspend fun getPlatformSetting(key: String): String? {
        return platformSettingDao.getSetting(key)
    }

    fun getActiveInstantJobForUser(userId: String): Flow<List<ProblemEntity>> {
        return problemDao.getActiveInstantJobForUser(userId)
    }

    suspend fun getActiveInstantJobForUserSync(userId: String): ProblemEntity? {
        return problemDao.getActiveInstantJobForUserSync(userId)
    }

    suspend fun createInstantJob(
        user: UserEntity,
        title: String,
        description: String,
        category: CategoryEntity,
        lat: Double,
        lng: Double,
        address: String,
        minBudget: Double,
        maxBudget: Double
    ): ProblemEntity {
        require(category.isPhysical) { "Instant job শুধু physical ক্যাটাগরির জন্য" }
        require(category.instantJobEnabled) { "এই ক্যাটাগরিতে জরুরি সার্ভিস বন্ধ আছে" }
        
        val activeJob = problemDao.getActiveInstantJobForUserSync(user.id)
            ?: problemDao.getActiveInstantJobForUser(user.id).firstOrNull()?.firstOrNull { prob ->
                prob.isInstantJob &&
                prob.jobStatus != null &&
                prob.jobStatus !in listOf("JOB_COMPLETED", "COMPLETED", "CANCELLED") &&
                prob.status !in listOf("COMPLETED", "CANCELLED")
            }
        if (activeJob != null) {
            throw Exception("আপনার ইতোমধ্যেই একটি সক্রিয় জরুরি জব চলমান রয়েছে।")
        }

        val problem = ProblemEntity(
            id = "PROB_${UUID.randomUUID().toString().take(8)}",
            userId = user.id,
            userName = user.name,
            userPhone = user.phone,
            userAddress = address,
            title = title,
            description = description,
            categoryId = category.id,
            categoryName = category.nameBangla,
            isPhysical = true,
            latitude = lat,
            longitude = lng,
            minBudget = minBudget,
            maxBudget = maxBudget,
            urgency = "খুব জরুরি",
            status = "OPEN",
            isInstantJob = true,
            jobStatus = "BROADCASTING",
            broadcastRadiusKm = category.instantJobRadiusKm,
            userLiveLat = lat,
            userLiveLng = lng,
            createdAt = System.currentTimeMillis(),
            broadcastTimerStartedAt = System.currentTimeMillis(),
            lastActivityAt = System.currentTimeMillis()
        )
        problemDao.insertProblem(problem)
        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৪গ] নিজের row হলে (RLS: auth.uid() = user_id)
        // Supabase-এ problem insert করা হয় আগে থেকেই-migrate-করা raw-insert `createProblem()`
        // (ধাপ ৮) দিয়ে, তারপর সফল হলেই `broadcast_instant_job` RPC কল করে matched solver-দের
        // notify করানো হয় (RPC নিজেই owner-check + matching লজিক করে)। insert ব্যর্থ হলে broadcast
        // কল করা হয় না (নিশ্চিতভাবেই PROBLEM_NOT_FOUND দেবে) — শুধু log, local Room flow
        // সবসময় অপ্রভাবিত।
        if (SupabaseAuthManager.currentUserId() == user.id) {
            SupabaseSyncManager.createProblem(problem.toProblemDto())
                .onSuccess {
                    SupabaseSyncManager.broadcastInstantJob(problem.id).onFailure { e ->
                        Log.w("SomadhanRepo", "createInstantJob: broadcastInstantJob dual-write failed for ${problem.id} (local flow unaffected): ${e.message}")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "createInstantJob: Supabase createProblem dual-write failed for ${problem.id} (local flow unaffected): ${e.message}")
                }
        }

        // 7.1 Notify matched solvers who have instantJobNotificationsEnabled == true
        try {
            val allUsers = userDao.getAllUsersList()
            val categoryIdLower = category.id.lowercase()
            val categoryNameLower = category.nameBangla.lowercase()
            val targetSolvers = allUsers.filter { u ->
                u.id != user.id &&
                (u.role.equals("SOLVER", ignoreCase = true) || u.hasSolverRole) &&
                u.instantJobNotificationsEnabled && // MUST be enabled: if off, skip completely
                (
                    u.solverCategories.isBlank() ||
                    u.solverCategories.split(",").any { cat ->
                        val c = cat.trim().lowercase()
                        c == categoryIdLower || c == categoryNameLower
                    }
                ) &&
                (
                    if (u.latitude != 0.0 && u.longitude != 0.0 && lat != 0.0 && lng != 0.0) {
                        DistanceUtil.calculateDistanceKm(u.latitude, u.longitude, lat, lng) <= category.instantJobRadiusKm
                    } else true
                )
            }

            val banglaBudget = if (minBudget == maxBudget) {
                "৳" + DistanceUtil.toBengaliDigits(minBudget.toInt().toString())
            } else {
                "৳" + DistanceUtil.toBengaliDigits(minBudget.toInt().toString()) + " – " + DistanceUtil.toBengaliDigits(maxBudget.toInt().toString())
            }
            targetSolvers.forEach { s ->
                val notif = NotificationEntity(
                    role = "SOLVER",
                    id = "NOTIF_INSTANT_${UUID.randomUUID().toString().take(8)}",
                    userId = s.id,
                    title = "নতুন জরুরি জব: ${category.nameBangla}",
                    message = "${user.name} একটি জরুরি কাজ পোস্ট করেছেন: ${title.take(40)} (বাজেট $banglaBudget)",
                    relatedProblemId = problem.id,
                    targetType = "problem",
                    targetId = problem.id
                )
                notificationDao.insertNotification(notif)
            }
        } catch (e: Exception) {
            Log.w("SomadhanRepo", "Failed to broadcast instant job notifications: ${e.message}")
        }

        return problem
    }

    suspend fun updateInstantJobToggle(userId: String, enabled: Boolean) {
        userDao.updateInstantJobToggle(userId, enabled)

        // [SUPABASE-MIGRATED - ধাপ ৩৩.৩] পুরনো Firestore leg এখানে ছিল -- সরানো হলো (দেখুন
        // MIGRATION_PROGRESS.md "ধাপ ৩৩.৩")। এখন শুধু Room local write (উপরে) + নিচের Supabase
        // dual-write (RPC না, সরাসরি own-row Postgrest update, `syncFreeJobQuota()`-এর মতোই
        // প্যাটার্নে -- RLS `users_update_own` policy নিজের id-তে permit করে)। best-effort,
        // ব্যর্থ হলে local flow অপ্রভাবিত।
        if (SupabaseAuthManager.currentUserId() == userId) {
            SupabaseSyncManager.syncInstantJobNotificationToggle(userId, enabled)
                .onFailure { e ->
                    Log.w("SomadhanRepo", "Failed to sync instantJobNotificationsEnabled to Supabase: ${e.message}")
                }
        }
    }

    fun getBroadcastingJobsByCategory(categoryId: String): Flow<List<ProblemEntity>> {
        return problemDao.getBroadcastingJobsByCategory(categoryId)
    }

    fun getActiveInstantJobForSolver(solverId: String): Flow<List<ProblemEntity>> {
        return problemDao.getActiveInstantJobForSolver(solverId)
    }

    suspend fun acceptInstantJobBid(
        problem: ProblemEntity,
        bid: BidEntity,
        userLiveLat: Double? = null,
        userLiveLng: Double? = null
    ): String {
        val result = acceptBid(problem, bid)
        if (result == "ALREADY_ACCEPTED") {
            // acceptBid() bailed out because this problem is no longer OPEN (already accepted by
            // a previous call). Don't fall through to the unconditional field-reset below --
            // that copy() call zeroes out onWayAt/arrivedAt/jobStartedAt/etc, which would wipe
            // out real live-tracking progress the solver has already made if this call is just a
            // stale duplicate racing in behind the original accept.
            return result
        }
        val now = System.currentTimeMillis()
        val freshProblem = problemDao.getProblemById(problem.id) ?: problem
        val updated = freshProblem.copy(
            jobStatus = "ON_WAY",
            onWayAt = null,
            arrivedAt = null,
            jobStartedAt = null,
            completedAt = null,
            solverLiveLat = null,
            solverLiveLng = null,
            solverLiveUpdatedAt = null,
            hasReleaseRequest = false,
            releaseRequestExtraAmount = 0.0,
            releaseRequestNote = "",
            releaseRequestedAt = null,
            pendingExtraAmount = null,
            pendingExtraAmountNote = null,
            pendingExtraAmountRequestedAt = null,
            confirmedExtraAmountTotal = 0.0,
            isDisputed = false,
            disputeReason = null,
            disputeInitiatorId = null,
            disputeSettledAt = null,
            disputeResolutionDecision = null,
            disputeResolutionType = null,
            disputeResolutionNote = null,
            disputeResolvedAt = null,
            disputeProgressAtRaise = null,
            disputeProgressAtSettlement = null,
            solverCancelledNotice = null,
            userLiveLat = userLiveLat ?: freshProblem.userLiveLat ?: if (freshProblem.latitude != 0.0) freshProblem.latitude else null,
            userLiveLng = userLiveLng ?: freshProblem.userLiveLng ?: if (freshProblem.longitude != 0.0) freshProblem.longitude else null,
            lastActivityAt = now
        )
        problemDao.updateProblem(updated)

        // [SUPABASE-MIGRATED - ধাপ ৩২.৯৫] কোনো নতুন RPC লাগেনি -- `problems_update_owner` RLS
        // policy (qual/with_check auth.uid()=user_id, কোনো column-level restriction নেই)
        // ইতিমধ্যেই owner-এর জন্য পুরো row আপডেট কভার করে, ঠিক এই ফাংশনের শুরুতে কল হওয়া
        // acceptBid()-এর accept_bid RPC bridge-এর একই caller-guard-এর মতোই। পুরো DTO পাঠানো
        // হচ্ছে (partial না) যাতে local `updated` কপির সাথে হুবহু মিলে যায়। best-effort —
        // ব্যর্থ হলেও উপরের local Room flow অপ্রভাবিত থাকে।
        if (SupabaseAuthManager.currentUserId() == updated.userId) {
            SupabaseSyncManager.updateProblemFull(updated.toProblemDto()).onFailure { e ->
                Log.w("SomadhanRepo", "acceptInstantJobBid: Supabase dual-write failed for ${updated.id} (local flow unaffected): ${e.message}")
            }
        }
        return result
    }

    suspend fun markSolverOnWay(problemId: String) {
        val now = System.currentTimeMillis()
        val problem = problemDao.getProblemById(problemId) ?: return
        val updated = problem.copy(
            jobStatus = "ON_THE_WAY",
            onWayAt = problem.onWayAt ?: now,
            lastActivityAt = now
        )
        problemDao.updateProblem(updated)

        // 7.3 Status Change Notification: On the way
        try {
            val solverName = problem.acceptedSolverName ?: "সলভার"
            val userNotif = NotificationEntity(
                role = "USER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = problem.userId,
                title = "সলভার রওয়ানা হয়েছেন 🚗",
                message = "$solverName আপনার লোকেশনের উদ্দেশ্যে রওয়ানা হয়েছেন।",
                relatedProblemId = problem.id,
                targetType = "problem",
                targetId = problem.id
            )
            notificationDao.insertNotification(userNotif)
        } catch (e: Exception) {
            Log.w("SomadhanRepo", "Failed to notify on the way: ${e.message}")
        }

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৪ক] `mark_solver_on_way` RPC সোর্স পড়ে যাচাই করা হয়েছে —
        // শুধু accepted_solver_id (auth.uid()) নিজে কল করতে পারে, RPC নিজেই problem আপডেট করে আর
        // owner-কে notification insert করে (একই transaction-এ)। best-effort — ব্যর্থ হলেও উপরের
        // local Room flow ইতিমধ্যে সম্পন্ন, অপ্রভাবিত থাকে।
        if (SupabaseAuthManager.currentUserId() == problem.acceptedSolverId) {
            SupabaseSyncManager.markSolverOnWay(problemId)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK") {
                        Log.w("SomadhanRepo", "markSolverOnWay: Supabase dual-write non-OK result for problem $problemId: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "markSolverOnWay: Supabase dual-write failed for problem $problemId (local flow unaffected): ${e.message}")
                }
        }
    }

    suspend fun markSolverArrived(problemId: String) {
        val now = System.currentTimeMillis()
        val problem = problemDao.getProblemById(problemId) ?: return
        val updated = problem.copy(
            jobStatus = "ARRIVED",
            arrivedAt = problem.arrivedAt ?: now,
            lastActivityAt = now
        )
        problemDao.updateProblem(updated)

        // 7.3 Status Change Notification: Arrived (Both Parties)
        try {
            val solverName = problem.acceptedSolverName ?: "সলভার"
            val userNotif = NotificationEntity(
                role = "USER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = problem.userId,
                title = "সলভার পৌঁছে গেছেন 📍",
                message = "$solverName আপনার ঠিকানায় পৌঁছে গেছেন।",
                relatedProblemId = problem.id,
                targetType = "problem",
                targetId = problem.id
            )
            notificationDao.insertNotification(userNotif)

            problem.acceptedSolverId?.let { sId ->
                val solverNotif = NotificationEntity(
                    role = "SOLVER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = sId,
                    title = "আপনি লোকেশনে পৌঁছেছেন 📍",
                    message = "ক্লায়েন্টের সাথে দেখা করে কাজ শুরু করুন।",
                    relatedProblemId = problem.id,
                    targetType = "problem",
                    targetId = problem.id
                )
                notificationDao.insertNotification(solverNotif)
            }
        } catch (e: Exception) {
            Log.w("SomadhanRepo", "Failed to notify arrived: ${e.message}")
        }

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৪ক] `mark_solver_arrived` RPC — শুধু accepted_solver_id
        // (auth.uid()) কল করতে পারে, RPC নিজেই উভয় পক্ষকে notify করে। best-effort।
        if (SupabaseAuthManager.currentUserId() == problem.acceptedSolverId) {
            SupabaseSyncManager.markSolverArrived(problemId)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK") {
                        Log.w("SomadhanRepo", "markSolverArrived: Supabase dual-write non-OK result for problem $problemId: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "markSolverArrived: Supabase dual-write failed for problem $problemId (local flow unaffected): ${e.message}")
                }
        }
    }

    suspend fun markJobStarted(problemId: String) {
        val now = System.currentTimeMillis()
        val problem = problemDao.getProblemById(problemId) ?: return
        val updated = problem.copy(
            jobStatus = "IN_PROGRESS",
            status = "IN_PROGRESS",
            jobStartedAt = problem.jobStartedAt ?: now,
            lastActivityAt = now
        )
        problemDao.updateProblem(updated)

        // 7.3 Status Change Notification: Job Started (Both Parties)
        try {
            val solverName = problem.acceptedSolverName ?: "সলভার"
            val userNotif = NotificationEntity(
                role = "USER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = problem.userId,
                title = "কাজ শুরু হয়েছে ⚡",
                message = "$solverName \"${problem.title}\" কাজটি শুরু করেছেন।",
                relatedProblemId = problem.id,
                targetType = "problem",
                targetId = problem.id
            )
            notificationDao.insertNotification(userNotif)

            problem.acceptedSolverId?.let { sId ->
                val solverNotif = NotificationEntity(
                    role = "SOLVER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = sId,
                    title = "কাজ চলমান ⚡",
                    message = "\"${problem.title}\" কাজটির সময় গণনা শুরু হয়েছে। মনোযোগ দিয়ে সম্পন্ন করুন।",
                    relatedProblemId = problem.id,
                    targetType = "problem",
                    targetId = problem.id
                )
                notificationDao.insertNotification(solverNotif)
            }
        } catch (e: Exception) {
            Log.w("SomadhanRepo", "Failed to notify job started: ${e.message}")
        }

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৪ক] `mark_job_started` RPC — শুধু accepted_solver_id
        // (auth.uid()) কল করতে পারে, RPC নিজেই job_status/status দুটোই IN_PROGRESS করে আর উভয়
        // পক্ষকে notify করে। best-effort।
        if (SupabaseAuthManager.currentUserId() == problem.acceptedSolverId) {
            SupabaseSyncManager.markJobStarted(problemId)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK") {
                        Log.w("SomadhanRepo", "markJobStarted: Supabase dual-write non-OK result for problem $problemId: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "markJobStarted: Supabase dual-write failed for problem $problemId (local flow unaffected): ${e.message}")
                }
        }
    }

    suspend fun cancelInstantJob(problemId: String, reason: String = "ইউজার কর্তৃক বাতিল") {
        val now = System.currentTimeMillis()
        val problem = problemDao.getProblemById(problemId) ?: return

        // Guard: don't let a stale "cancel" action overwrite a job that has already reached a
        // terminal state. Without this, calling cancelInstantJob() on an already-COMPLETED job
        // (a stale UI screen, a delayed/duplicate tap) would flip its status back to CANCELLED,
        // corrupting the completed job's record — its escrow is already RELEASED by this point,
        // so refundEscrowOnce() below would correctly refuse to refund it (see its own
        // escrow-status guard), but the problem row itself would still get wrongly overwritten
        // without this check.
        if (problem.status == "COMPLETED" || problem.status == "CANCELLED") {
            Log.w("SomadhanRepo", "cancelInstantJob: skipped — problem $problemId is already ${problem.status}, refusing to cancel again")
            return
        }

        val progressStep = problem.calculateProgressStep().coerceAtLeast(1)
        val updated = problem.copy(
            jobStatus = "CANCELLED",
            status = "CANCELLED",
            // Bug fix: solverCancelJob()'s reopenAsOpen=true path and
            // adminForceCancelInstantJob() both clear these fields on cancel; this function was
            // the one path that left the stale accepted-solver snapshot sitting on a CANCELLED
            // problem row, which could confuse future reporting/admin tooling into thinking a
            // solver is still attached to a job that's actually over.
            acceptedBidId = null,
            acceptedSolverId = null,
            acceptedSolverName = null,
            acceptedAmount = null,
            hasReleaseRequest = false,
            releaseRequestExtraAmount = 0.0,
            releaseRequestNote = "",
            releaseRequestedAt = null,
            pendingExtraAmount = null,
            pendingExtraAmountNote = null,
            pendingExtraAmountRequestedAt = null,
            solverCancelledNotice = null,
            lastActivityAt = now
        )
        problemDao.updateProblem(updated)

        // Refund Escrow (Base Amount + Confirmed Extra Amount) to User's Wallet Balance (Strictly Idempotent via refundEscrowOnce)
        val escrow = escrowDao.getByProblemId(problemId)
        val refundAmount = (escrow?.baseAmount ?: (problem.acceptedAmount ?: 0.0)) + (escrow?.extraAmount ?: problem.confirmedExtraAmountTotal)
        val refundSucceeded = if (refundAmount > 0.0) {
            refundEscrowOnce(
                problemId = problem.id,
                amount = refundAmount,
                userId = problem.userId,
                solverId = problem.acceptedSolverId ?: "",
                problemTitle = problem.title,
                baseAmount = escrow?.baseAmount ?: (problem.acceptedAmount ?: 0.0),
                extraAmount = escrow?.extraAmount ?: problem.confirmedExtraAmountTotal,
                escrowId = escrow?.id
            )
        } else false

        // Cancel all active bids with progressAtCancel
        val bids = bidDao.getBidsForProblemSync(problemId)
        bids.forEach { bid ->
            if (bid.status != "CANCELLED") {
                val updatedBid = bid.copy(status = "CANCELLED", progressAtCancel = progressStep, resolutionType = "USER_CANCEL", resolvedAt = System.currentTimeMillis())
                bidDao.insertBid(updatedBid)
            }
        }

        // 7.3 Status Change Notification: Cancelled (Both Parties)
        try {
            val refundText = if (refundSucceeded && refundAmount > 0.0) " এসক্রো বাবদ কাটা ৳${DistanceUtil.toBengaliDigits(refundAmount.toInt().toString())} আপনার ওয়ালেটে রিফান্ড করা হয়েছে।" else ""
            val userNotif = NotificationEntity(
                role = "USER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = problem.userId,
                title = "কাজ বাতিল নিশ্চিত ❌",
                message = "\"${problem.title}\" কাজটি সফলভাবে বাতিল করা হয়েছে।$refundText",
                relatedProblemId = problem.id,
                targetType = "problem",
                targetId = problem.id
            )
            notificationDao.insertNotification(userNotif)

            problem.acceptedSolverId?.let { sId ->
                val solverNotif = NotificationEntity(
                    role = "SOLVER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = sId,
                    title = "কাজটি বাতিল করা হয়েছে ❌",
                    message = "ক্লায়েন্ট \"${problem.title}\" কাজটি বাতিল করেছেন। কারণ: $reason",
                    relatedProblemId = problem.id,
                    targetType = "problem",
                    targetId = problem.id
                )
                notificationDao.insertNotification(solverNotif)
            }
        } catch (e: Exception) {
            Log.w("SomadhanRepo", "Failed to notify cancel: ${e.message}")
        }

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৪ক] `cancel_instant_job` RPC সোর্স পড়ে যাচাই করা হয়েছে —
        // শুধু problem owner (auth.uid() = user_id) কল করতে পারে, RPC problem/bids রিসেট করে আর
        // উভয় পক্ষকে notify করে — কিন্তু escrow refund করে না ইচ্ছাকৃতভাবে, কারণ সেটা উপরে
        // refundEscrowOnce() দিয়ে ইতিমধ্যে (ধাপ ৯-এ migrate করা পথে) আলাদাভাবে হয়ে গেছে; এখানে
        // ডুপ্লিকেট করলে ডাবল-রিফান্ডের ঝুঁকি তৈরি হতো। best-effort — ব্যর্থ হলেও উপরের
        // local Room flow (রিফান্ডসহ) ইতিমধ্যে সম্পন্ন, অপ্রভাবিত থাকে।
        if (SupabaseAuthManager.currentUserId() == problem.userId) {
            SupabaseSyncManager.cancelInstantJob(problemId, reason, progressStep)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK" && resultField != "ALREADY_TERMINAL") {
                        Log.w("SomadhanRepo", "cancelInstantJob: Supabase dual-write non-OK result for problem $problemId: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "cancelInstantJob: Supabase dual-write failed for problem $problemId (local flow unaffected): ${e.message}")
                }
        }
    }

    suspend fun updateSolverLiveLocation(problemId: String, lat: Double, lng: Double) {
        val now = System.currentTimeMillis()
        try {
            problemDao.updateSolverLiveLocation(problemId, lat, lng, now)
        } catch (e: Exception) {
            Log.w("SomadhanRepo", "Failed to update local solver live location: ${e.message}")
        }
        // [SUPABASE-MIGRATED - ধাপ ৩৩.৩] পুরনো Firestore leg (problems/{problemId} document-এ
        // সরাসরি update) এখানে ছিল -- সরানো হলো (দেখুন MIGRATION_PROGRESS.md "ধাপ ৩৩.৩")। Room
        // local write (উপরে) এবং নিচের Supabase RPC dual-write-ই এখন এই ফাংশনের একমাত্র sync
        // path। `update_solver_live_location` RPC (ধাপ ২৩-এ apply করা) দিয়ে dual-write। RPC
        // নিজেই ভেতরে auth.uid() == accepted_solver_id যাচাই করে, তাই এখানে caller-scoping guard
        // আলাদা করে দরকার নেই (শুধু signed-in আছে কিনা)। ব্যর্থ হলেও local flow (উপরে) ইতিমধ্যে
        // সম্পন্ন, অপ্রভাবিত থাকে -- non-blocking, best-effort, GPS আপডেট প্রতি কয়েক সেকেন্ডে আসে
        // বলে একটা ব্যর্থ কল উপেক্ষা করা নিরাপদ।
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.updateSolverLiveLocation(problemId, lat, lng)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK" && resultField != "ALREADY_TERMINAL") {
                        Log.w("SomadhanRepo", "updateSolverLiveLocation: Supabase dual-write non-OK result for problem $problemId: $resultField (local Room flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "updateSolverLiveLocation: Supabase dual-write failed for problem $problemId (local Room flow unaffected): ${e.message}")
                }
        }
    }

    suspend fun requestExtraAmount(problem: ProblemEntity, amount: Double, note: String) {
        val now = System.currentTimeMillis()
        val safeExtra = if (amount > 0.0) amount else 0.0
        val updated = problem.copy(
            pendingExtraAmount = safeExtra,
            pendingExtraAmountNote = note.trim(),
            pendingExtraAmountRequestedAt = now,
            lastActivityAt = now
        )
        problemDao.updateProblem(updated)

        val solverName = problem.acceptedSolverName ?: "সমাধানকারী"
        val userNotif = NotificationEntity(
            role = "USER",
            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
            userId = problem.userId,
            title = "অতিরিক্ত বিলের অনুরোধ এসেছে 🔔",
            message = "$solverName আপনার \"${problem.title}\" কাজের জন্য ৳${DistanceUtil.toBengaliDigits(safeExtra.toInt().toString())} অতিরিক্ত বিল অনুরোধ করেছেন।${if (note.isNotBlank()) " (নোট: ${note.trim()})" else ""}",
            relatedProblemId = problem.id,
            targetType = "problem",
            targetId = problem.id
        )
        notificationDao.insertNotification(userNotif)

        sendMessage(
            problemId = problem.id,
            senderId = problem.acceptedSolverId ?: "SOLVER",
            receiverId = problem.userId,
            senderName = solverName,
            content = "➕ কাজের জন্য ৳${DistanceUtil.toBengaliDigits(safeExtra.toInt().toString())} অতিরিক্ত বিল অনুরোধ করা হয়েছে। ${if (note.isNotBlank()) "নোট: ${note.trim()}" else ""}"
        )
        // চ্যাট মেসেজের dual-write উপরের sendMessage() কলেই হয়ে যায় (ধাপ ১১-এ migrate করা),
        // এখানে আলাদা করার দরকার নেই।

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৪খ] `request_extra_amount` RPC — শুধু accepted_solver_id
        // (auth.uid()) কল করতে পারে, RPC নিজেই pending fields সেট করে আর owner-কে notify করে।
        // best-effort।
        if (SupabaseAuthManager.currentUserId() == problem.acceptedSolverId) {
            SupabaseSyncManager.requestExtraAmount(problem.id, safeExtra, note)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK") {
                        Log.w("SomadhanRepo", "requestExtraAmount: Supabase dual-write non-OK result for problem ${problem.id}: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "requestExtraAmount: Supabase dual-write failed for problem ${problem.id} (local flow unaffected): ${e.message}")
                    // [Step 12.7] outbox retry -- RPC শুধু pending_extra_amount ফিল্ড SET করে (কোনো
                    // balance/escrow ছোঁয় না), আর amount প্যারামিটার থেকেই আসে, সার্ভার-স্টেট থেকে না --
                    // তাই ২য়বার চললে একই অনুরোধ আবার বসে, টাকা নড়ে না।
                    enqueueOutboxRetry(
                        rpcName = "request_extra_amount",
                        params = kotlinx.serialization.json.JsonObject(
                            mapOf(
                                "problemId" to kotlinx.serialization.json.JsonPrimitive(problem.id),
                                "amount" to kotlinx.serialization.json.JsonPrimitive(safeExtra),
                                "note" to kotlinx.serialization.json.JsonPrimitive(note)
                            )
                        ),
                        error = e
                    )
                }
        }
    }

    suspend fun userConfirmExtraAmount(problem: ProblemEntity, walletDeducted: Double = 0.0) {
        // Idempotency guard: re-fetch the problem fresh and confirm there's still a pending
        // extra-amount request to act on. Without this, a double-tap on "Confirm" (or the same
        // stale `problem` object being submitted twice before the first write lands) would add
        // the SAME amount to escrow.extraAmount and confirmedExtraAmountTotal a second time, and
        // deduct the wallet twice if walletDeducted was passed again on the retry -- the same bug
        // class already fixed for respondToAdditionalCharge().
        val freshProblem = problemDao.getProblemById(problem.id) ?: return
        val amt = freshProblem.pendingExtraAmount ?: return
        val now = System.currentTimeMillis()

        if (walletDeducted > 0.0) {
            // INSTANT local debit (Room); the cloud side goes through the RPC dual-write
            // further below, with outbox retry on failure [Step 7.7 cleanup: pendingCloudSync
            // on the TransactionEntity below is an unread Firebase-era leftover, not the real
            // retry mechanism], instead of the old incrementUserBalance() fire-and-forget (no
            // retry if that single attempt failed).
            // [ব্যালেন্স ফিক্স] user হিসেবে confirmed extra amount কাটা হচ্ছে, balanceUser mirror-ও আপডেট হয়।
            userDao.deductBalanceForUserRole(freshProblem.userId, walletDeducted, now)
        }
        // Bug fix (transaction-history gap): this record used to be nested inside
        // `if (walletDeducted > 0.0)` and use `walletDeducted` as the amount. With a ৳0 wallet
        // balance and the extra amount paid via gateway, walletDeducted was 0.0 -- so no
        // transaction was ever recorded, even though addToEscrow() below already (correctly)
        // adds the FULL `amt` to escrow regardless of payment source. Using `amt` here too keeps
        // the transaction record consistent with what escrow actually received. Deterministic id
        // keyed on pendingExtraAmountRequestedAt, which is stable for the lifetime of THIS
        // specific pending-extra-amount request cycle (unchanged until this request is confirmed
        // or a new one is raised), same convention as splitTrxId.
        if (amt > 0.0) {
            val deductionTrx = TransactionEntity(
                id = "TRX_CONFIRM_EXTRA_${freshProblem.id}_${freshProblem.pendingExtraAmountRequestedAt}",
                problemId = freshProblem.id,
                problemTitle = freshProblem.title,
                userId = freshProblem.userId,
                // solverId left blank on purpose -- see the identical note on TRX_BID_DEDUCT_
                // above; this is a user-wallet-ledger deduction record, not a solver earning.
                solverId = "",
                grossAmount = amt,
                commissionPercent = 0.0,
                commissionAmount = 0.0,
                netAmount = -amt,
                type = "EXTRA_CHARGE_DEDUCTION",
                pendingCloudSync = true,
                role = "USER"
            )
            transactionDao.insertTransaction(deductionTrx)
        }

        // Route through addToEscrow() instead of a bare read-then-insert() here -- addToEscrow()
        // resolves the SPECIFIC, currently-active escrow row by id and applies an atomic
        // DB-level increment (addExtraAmountById), rather than reading a possibly-stale
        // EscrowEntity snapshot and overwriting the whole row. A plain read-modify-write here
        // could silently lose a concurrent escrow update (e.g. a mid-job additional-charge
        // acceptance landing at nearly the same moment) -- exactly the race addToEscrow()'s own
        // doc comment describes already having to fix.
        addToEscrow(freshProblem.id, amt)

        val updated = freshProblem.copy(
            confirmedExtraAmountTotal = freshProblem.confirmedExtraAmountTotal + amt,
            pendingExtraAmount = null,
            pendingExtraAmountNote = null,
            pendingExtraAmountRequestedAt = null,
            lastActivityAt = now
        )
        problemDao.updateProblem(updated)

        freshProblem.acceptedSolverId?.let { sId ->
            val solverNotif = NotificationEntity(
                role = "SOLVER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = sId,
                title = "অতিরিক্ত বিল অনুমোদিত ✅",
                message = "ক্লায়েন্ট আপনার ৳${DistanceUtil.toBengaliDigits(amt.toInt().toString())} অতিরিক্ত বিলের অনুরোধ অনুমোদন করেছেন।",
                relatedProblemId = freshProblem.id,
                targetType = "problem",
                targetId = freshProblem.id
            )
            notificationDao.insertNotification(solverNotif)
        }

        sendMessage(
            problemId = freshProblem.id,
            senderId = freshProblem.userId,
            receiverId = freshProblem.acceptedSolverId ?: "",
            senderName = freshProblem.userName,
            content = "✅ ৳${DistanceUtil.toBengaliDigits(amt.toInt().toString())} অতিরিক্ত বিল অনুমোদন করা হয়েছে।"
        )
        // চ্যাট মেসেজের dual-write উপরের sendMessage() কলেই হয়ে যায় (ধাপ ১১), আলাদা করার দরকার নেই।

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৪খ] `user_confirm_extra_amount` RPC সোর্স পড়ে যাচাই করা
        // হয়েছে — শুধু problem owner (auth.uid()) কল করতে পারে, RPC নিজেই
        // wallet_deduction = least(balance, pending_extra_amount) স্বাধীনভাবে হিসাব করে (client-এর
        // walletDeducted প্যারামিটার পাঠানো হয়নি, RPC নিজে recompute করে) — escrow ও
        // confirmed_extra_amount_total সম্পূর্ণ amt দিয়ে বাড়ে (Kotlin-সাইডের addToEscrow(amt)-এর
        // সাথে মিলিয়ে, শুধু walletDeducted দিয়ে না)। best-effort।
        if (SupabaseAuthManager.currentUserId() == freshProblem.userId) {
            SupabaseSyncManager.userConfirmExtraAmount(freshProblem.id)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK" && resultField != "NOT_PENDING") {
                        Log.w("SomadhanRepo", "userConfirmExtraAmount: Supabase dual-write non-OK result for problem ${freshProblem.id}: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "userConfirmExtraAmount: Supabase dual-write failed for problem ${freshProblem.id} (local flow unaffected): ${e.message}")
                    // [Step 12.8c] retry -- server-side amount-changed guard (p_expected_amount, নতুন
                    // ২-arg overload) apply হওয়ার পরেই যোগ। expectedAmount = amt (এই মুহূর্তের
                    // pending_extra_amount, ঠিক যা confirm করা হচ্ছে): replay-এর সময় ততক্ষণে সলভার
                    // নতুন/ভিন্ন অঙ্কের extra charge রিকোয়েস্ট করলে RPC non-OK `AMOUNT_CHANGED` দেয়,
                    // owner-এর অজান্তে ভুল অঙ্ক ওয়ালেট থেকে কাটা হয় না।
                    enqueueOutboxRetry(
                        rpcName = "user_confirm_extra_amount",
                        params = kotlinx.serialization.json.JsonObject(
                            buildMap {
                                put("problemId", kotlinx.serialization.json.JsonPrimitive(freshProblem.id))
                                put("expectedAmount", kotlinx.serialization.json.JsonPrimitive(amt))
                            }
                        ),
                        error = e
                    )
                }
        }
    }

    suspend fun userRejectExtraAmount(problem: ProblemEntity) {
        val now = System.currentTimeMillis()
        val updated = problem.copy(
            pendingExtraAmount = null,
            pendingExtraAmountNote = null,
            pendingExtraAmountRequestedAt = null,
            lastActivityAt = now
        )
        problemDao.updateProblem(updated)

        problem.acceptedSolverId?.let { sId ->
            val solverNotif = NotificationEntity(
                role = "SOLVER",
                id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                userId = sId,
                title = "অতিরিক্ত বিল প্রত্যাখ্যাত ❌",
                message = "ক্লায়েন্ট আপনার অতিরিক্ত বিলের অনুরোধ প্রত্যাখ্যান করেছেন।",
                relatedProblemId = problem.id,
                targetType = "problem",
                targetId = problem.id
            )
            notificationDao.insertNotification(solverNotif)
        }

        sendMessage(
            problemId = problem.id,
            senderId = problem.userId,
            receiverId = problem.acceptedSolverId ?: "",
            senderName = problem.userName,
            content = "❌ অতিরিক্ত বিলের অনুরোধ প্রত্যাখ্যান করা হয়েছে।"
        )
        // চ্যাট মেসেজের dual-write উপরের sendMessage() কলেই হয়ে যায় (ধাপ ১১), আলাদা করার দরকার নেই।

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৪খ] `user_reject_extra_amount` RPC — শুধু problem owner
        // (auth.uid()) কল করতে পারে, RPC pending fields ক্লিয়ার করে আর সলভারকে notify করে।
        // best-effort।
        if (SupabaseAuthManager.currentUserId() == problem.userId) {
            SupabaseSyncManager.userRejectExtraAmount(problem.id)
                .onSuccess { json ->
                    val resultField = (json as? kotlinx.serialization.json.JsonObject)?.get("result")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    if (resultField != null && resultField != "OK") {
                        Log.w("SomadhanRepo", "userRejectExtraAmount: Supabase dual-write non-OK result for problem ${problem.id}: $resultField (local flow unaffected)")
                    }
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "userRejectExtraAmount: Supabase dual-write failed for problem ${problem.id} (local flow unaffected): ${e.message}")
                    // [Step 12.7] outbox retry -- RPC শুধু pending_extra_amount ফিল্ডগুলো null করে
                    // (কোনো balance/escrow ছোঁয় না); ২য়বার চললে সেগুলো আগে থেকেই null, কিছুই বদলায় না।
                    enqueueOutboxRetry(
                        rpcName = "user_reject_extra_amount",
                        params = kotlinx.serialization.json.JsonObject(
                            mapOf(
                                "problemId" to kotlinx.serialization.json.JsonPrimitive(problem.id)
                            )
                        ),
                        error = e
                    )
                }
        }
    }

    suspend fun cleanupCorruptedCommissionRates(): Int {
        val allProblems = problemDao.getAllProblems().firstOrNull() ?: emptyList()
        var fixedCount = 0
        allProblems.forEach { problem ->
            // যেসব জব এখনো কোনো সলভার অ্যাক্সেপ্ট করেনি (status == "OPEN", acceptedSolverId == null),
            // কিন্তু appliedCommissionRate ইতিমধ্যে সেট হয়ে আছে — এগুলো null-এ ফিরিয়ে দেওয়া
            if (problem.status == "OPEN" && problem.acceptedSolverId == null && problem.appliedCommissionRate != null) {
                val fixed = problem.copy(appliedCommissionRate = null)
                problemDao.updateProblem(fixed)

                // [SUPABASE-MIGRATED - ধাপ ৩২.৯ঘ] সরাসরি Postgrest update (RPC লাগেনি --
                // `problems_update_admin` RLS policy আগে থেকেই admin-এর জন্য যেকোনো কলাম আপডেট
                // অনুমোদন করে, adminSoftDeleteProblem()-এর মতোই)। best-effort, ব্যর্থ হলে শুধু log।
                val newRate: Double? = null
                SupabaseSyncManager.adminUpdateProblemCommissionRate(fixed.id, newRate)
                    .onFailure { e ->
                        Log.w("SomadhanRepo", "cleanupCorruptedCommissionRates: Supabase dual-write failed for ${fixed.id} (local Room fix unaffected): ${e.message}")
                        // [Step 12.8c] retry -- ফাংশনটা RPC না, তাই এখানে migration/apply লাগে না
                        // (নিচের এন্ট্রি: SupabaseSyncManager.adminUpdateProblemCommissionRate()-এর
                        // নিজের status='OPEN'/accepted_solver_id-is-null গার্ড, ২য়বার চললে ততক্ষণে
                        // সলভার accept করে ফেললে ০-row বেনাইন no-op)। rate এখানে সবসময় null, তবু
                        // ভবিষ্যতের non-null caller-দের জন্য ঐচ্ছিক রাখা হলো (x?.let { put(...) }).
                        enqueueOutboxRetry(
                            rpcName = "admin_update_problem_commission_rate",
                            params = kotlinx.serialization.json.JsonObject(
                                buildMap {
                                    put("problemId", kotlinx.serialization.json.JsonPrimitive(fixed.id))
                                    newRate?.let { put("rate", kotlinx.serialization.json.JsonPrimitive(it)) }
                                }
                            ),
                            error = e
                        )
                    }

                fixedCount++
            }
        }
        return fixedCount
    }

    // [Somadhan Bug-Fix Step 7.9] রিটার্ন-টাইপ Unit থেকে Boolean-এ বদলানো হয়েছে: RPC (Step 7.2-এর
    // ফিক্সের পর) যদি owner-এর USER role নিষ্ক্রিয় থাকায় escrow refund করতে না পারে, তাহলে
    // 'refund_pending': true ফেরত দেয় (cancel/rebroadcast/to-normal-bidding তবু সম্পন্ন হয়, শুধু
    // escrow HELD থেকে যায় role আবার active না হওয়া পর্যন্ত)। আগে এই ফ্ল্যাগ কোথাও পড়া হতো না --
    // এখন caller (ViewModel)-কে ফেরত দেওয়া হয় যাতে admin-কে জানানো যায়। early-return path
    // (already-terminal, problem-not-found) এবং offline/no-session path-এ সবসময় false।
    suspend fun adminForceCancelInstantJob(problemId: String, reason: String, targetAction: String = "CANCEL"): Boolean {
        val problem = problemDao.getProblemById(problemId) ?: return false
        val now = System.currentTimeMillis()

        // Guard: same reasoning as cancelInstantJob()/solverCancelJob() above -- don't let a
        // stale/duplicate admin action overwrite a job that has already reached a terminal state.
        if (problem.status == "COMPLETED" || problem.status == "CANCELLED") {
            Log.w("SomadhanRepo", "adminForceCancelInstantJob: skipped — problem $problemId is already ${problem.status}, refusing to act again")
            return false
        }

        // Bug fix: all three branches below used to reset acceptedSolverId to null WITHOUT ever
        // refunding the escrow tied to that accepted solver. If a solver had already been
        // accepted (money already moved out of the user's wallet/gateway into escrow -- status
        // "HELD"), that escrow became a permanent orphan:
        //   - REBROADCAST/TO_NORMAL_BIDDING used to set problem.status = "PENDING", a value no
        //     query anywhere in the app (open-problem lists, instant-job lists,
        //     reconcileEscrowStates()'s self-heal condition, checkAndProcess48HourAutoReleases())
        //     recognizes -- so the escrow could never be auto-healed, and for TO_NORMAL_BIDDING
        //     the problem itself (isInstantJob flipped to false + status "PENDING") became
        //     invisible in every listing (normal-open queries require status = "OPEN").
        //   - CANCEL used to only flip status to "CANCELLED" with no refund call at all, even
        //     though its own admin-panel UI promises "পূর্ণ রিফান্ড" (full refund) right away --
        //     the money only came back once reconcileEscrowStates() happened to run later for
        //     that specific user (next login/pull-to-refresh), with no transaction record or
        //     notification generated at the moment of cancellation.
        // Refunding explicitly here, synchronously, before resetting the problem row -- exactly
        // like cancelInstantJob()/solverCancelJob() already do -- closes all three gaps at once.
        val escrow = escrowDao.getByProblemId(problemId)
        val hadAcceptedSolver = !problem.acceptedSolverId.isNullOrBlank()
        val refundAmount = if (hadAcceptedSolver) {
            (escrow?.baseAmount ?: (problem.acceptedAmount ?: 0.0)) + (escrow?.extraAmount ?: problem.confirmedExtraAmountTotal)
        } else 0.0
        if (refundAmount > 0.0) {
            refundEscrowOnce(
                problemId = problemId,
                amount = refundAmount,
                userId = problem.userId,
                solverId = problem.acceptedSolverId ?: "",
                problemTitle = problem.title,
                baseAmount = escrow?.baseAmount ?: (problem.acceptedAmount ?: 0.0),
                extraAmount = escrow?.extraAmount ?: problem.confirmedExtraAmountTotal,
                escrowId = escrow?.id,
                refundType = "ADMIN_FORCE_ACTION"
            )
        }

        // Cancel the previously-accepted bid (if any) so it doesn't linger as "ACCEPTED" once
        // the solver assignment is cleared below -- mirrors solverCancelJob()/adminRefundEscrow().
        // (progressStep হোস্ট করা হলো ফাংশনের এই পর্যায়ে যাতে নিচের Supabase dual-write RPC
        // কলেও -- hadAcceptedSolver false থাকলেও -- একই মান ব্যবহার করা যায়, ধাপ ১২ ব্যাচ ৪গ)
        val progressStep = problem.calculateProgressStep().coerceAtLeast(1)
        if (hadAcceptedSolver) {
            val bids = bidDao.getBidsForProblemSync(problemId)
            bids.forEach { bid ->
                if ((bid.id == problem.acceptedBidId || bid.solverId == problem.acceptedSolverId) && bid.status != "CANCELLED") {
                    val updatedBid = bid.copy(status = "CANCELLED", progressAtCancel = progressStep, resolutionType = "ADMIN_FORCE_ACTION", resolvedAt = now)
                    bidDao.updateBid(updatedBid)
                }
            }
        }

        when (targetAction) {
            "REBROADCAST" -> {
                val updated = problem.copy(
                    isInstantJob = true,
                    jobStatus = "BROADCASTING",
                    // Bug fix: "OPEN" (not the unrecognized "PENDING") so this matches every
                    // other instant-job-in-broadcast row (see createInstantJob()) and so
                    // reconcileEscrowStates()'s self-heal can still find/refund this problem's
                    // escrow if anything is ever left HELD against it in the future.
                    status = "OPEN",
                    acceptedBidId = null,
                    acceptedSolverId = null,
                    acceptedSolverName = null,
                    acceptedAmount = null,
                    solverLiveLat = null,
                    solverLiveLng = null,
                    solverLiveUpdatedAt = null,
                    arrivedAt = null,
                    jobStartedAt = null,
                    hasReleaseRequest = false,
                    releaseRequestExtraAmount = 0.0,
                    releaseRequestNote = "",
                    releaseRequestedAt = null,
                    confirmedExtraAmountTotal = 0.0,
                    broadcastTimerStartedAt = now,
                    lastActivityAt = now
                )
                problemDao.updateProblem(updated)
                logAdminAction("INSTANT_JOB_REBROADCAST", problem.id, problem.title, "Reason: $reason" + if (refundAmount > 0.0) " | রিফান্ড: ৳${refundAmount.toInt()}" else "")

                val userNotif = NotificationEntity(
                    role = "USER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = problem.userId,
                    title = "জরুরি জব পুনরায় ব্রডকাস্ট করা হয়েছে ⚡",
                    message = "অ্যাডমিন কর্তৃক আপনার জরুরি জবটি নতুন করে ব্রডকাস্ট করা হয়েছে। কারণ: $reason" +
                        if (refundAmount > 0.0) " পূর্বের এসক্রো বাবদ ৳${DistanceUtil.toBengaliDigits(refundAmount.toInt().toString())} আপনার ওয়ালেটে রিফান্ড করা হয়েছে।" else "",
                    relatedProblemId = problem.id,
                    targetType = "problem",
                    targetId = problem.id
                )
                notificationDao.insertNotification(userNotif)

                problem.acceptedSolverId?.let { sId ->
                    val solverNotif = NotificationEntity(
                        role = "SOLVER",
                        id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                        userId = sId,
                        title = "জরুরি কাজ পুনরায় ব্রডকাস্ট করা হয়েছে (অ্যাডমিন) ⚡",
                        message = "অ্যাডমিন কর্তৃক \"${problem.title}\" কাজটি পুনরায় ব্রডকাস্ট করা হয়েছে। কারণ: $reason",
                        relatedProblemId = problem.id,
                        targetType = "problem",
                        targetId = problem.id
                    )
                    notificationDao.insertNotification(solverNotif)
                }
            }
            "TO_NORMAL_BIDDING" -> {
                val updated = problem.copy(
                    isInstantJob = false,
                    jobStatus = null,
                    // Bug fix: "OPEN" (not "PENDING") -- normal-job listing queries filter on
                    // status = 'OPEN' (see AppDaos.kt). With "PENDING" + isInstantJob=false,
                    // this problem matched NEITHER the instant-job queries NOR the normal-open-
                    // problem queries and became invisible everywhere -- to the poster, to
                    // solvers, and to normal admin problem lists.
                    status = "OPEN",
                    acceptedBidId = null,
                    acceptedSolverId = null,
                    acceptedSolverName = null,
                    acceptedAmount = null,
                    solverLiveLat = null,
                    solverLiveLng = null,
                    solverLiveUpdatedAt = null,
                    arrivedAt = null,
                    jobStartedAt = null,
                    hasReleaseRequest = false,
                    releaseRequestExtraAmount = 0.0,
                    releaseRequestNote = "",
                    releaseRequestedAt = null,
                    confirmedExtraAmountTotal = 0.0,
                    lastActivityAt = now
                )
                problemDao.updateProblem(updated)
                logAdminAction("INSTANT_JOB_TO_NORMAL_BIDDING", problem.id, problem.title, "Converted to normal bidding. Reason: $reason" + if (refundAmount > 0.0) " | রিফান্ড: ৳${refundAmount.toInt()}" else "")

                val userNotif = NotificationEntity(
                    role = "USER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = problem.userId,
                    title = "কাজটি সাধারণ বিডিংয়ে রূপান্তর করা হয়েছে 📋",
                    message = "জরুরি জবটি সাধারণ বিডিং তালিকায় পাঠানো হয়েছে। কারণ: $reason" +
                        if (refundAmount > 0.0) " পূর্বের এসক্রো বাবদ ৳${DistanceUtil.toBengaliDigits(refundAmount.toInt().toString())} আপনার ওয়ালেটে রিফান্ড করা হয়েছে।" else "",
                    relatedProblemId = problem.id,
                    targetType = "problem",
                    targetId = problem.id
                )
                notificationDao.insertNotification(userNotif)

                problem.acceptedSolverId?.let { sId ->
                    val solverNotif = NotificationEntity(
                        role = "SOLVER",
                        id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                        userId = sId,
                        title = "কাজটি সাধারণ বিডিংয়ে রূপান্তর করা হয়েছে (অ্যাডমিন) 📋",
                        message = "অ্যাডমিন কর্তৃক \"${problem.title}\" কাজটি সাধারণ বিডিং তালিকায় পাঠানো হয়েছে। কারণ: $reason",
                        relatedProblemId = problem.id,
                        targetType = "problem",
                        targetId = problem.id
                    )
                    notificationDao.insertNotification(solverNotif)
                }
            }
            else -> { // CANCEL
                val updated = problem.copy(
                    status = "CANCELLED",
                    jobStatus = "CANCELLED",
                    acceptedBidId = null,
                    hasReleaseRequest = false,
                    releaseRequestExtraAmount = 0.0,
                    releaseRequestNote = "",
                    releaseRequestedAt = null,
                    lastActivityAt = now
                )
                problemDao.updateProblem(updated)
                logAdminAction("FORCE_CANCEL_INSTANT_JOB", problem.id, problem.title, "Reason: $reason" + if (refundAmount > 0.0) " | রিফান্ড: ৳${refundAmount.toInt()}" else "")

                val refundText = if (refundAmount > 0.0) " এসক্রো বাবদ ৳${DistanceUtil.toBengaliDigits(refundAmount.toInt().toString())} আপনার ওয়ালেটে রিফান্ড করা হয়েছে।" else ""
                val userNotif = NotificationEntity(
                    role = "USER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = problem.userId,
                    title = "জরুরি কাজ বাতিল (অ্যাডমিন) ❌",
                    message = "অ্যাডমিন কর্তৃক \"${problem.title}\" কাজটি বাতিল করা হয়েছে। কারণ: $reason$refundText",
                    relatedProblemId = problem.id,
                    targetType = "problem",
                    targetId = problem.id
                )
                notificationDao.insertNotification(userNotif)

                problem.acceptedSolverId?.let { sId ->
                    val solverNotif = NotificationEntity(
                        role = "SOLVER",
                        id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                        userId = sId,
                        title = "জরুরি কাজ বাতিল (অ্যাডমিন) ❌",
                        message = "অ্যাডমিন কর্তৃক \"${problem.title}\" কাজটি বাতিল করা হয়েছে। কারণ: $reason",
                        relatedProblemId = problem.id,
                        targetType = "problem",
                        targetId = problem.id
                    )
                    notificationDao.insertNotification(solverNotif)
                }
            }
        }

        // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৪গ] `admin_force_cancel_instant_job` RPC (৩টা
        // targetAction-ই এক RPC-তে হ্যান্ডেল করে, নিজেই is_admin() চেক করে + notification insert
        // করে) — guard প্যাটার্ন adminManuallyFlagDispute()-এর মতোই: শুধু session থাকলেই কল করা
        // হয়, RPC নিজে is_admin() যাচাই করে ব্যর্থ হবে (log-only) যদি আসলে admin না হয়। escrow
        // refund এখানে ডুপ্লিকেট করা হয়নি -- সেটা উপরে ইতিমধ্যে refundEscrowOnce() দিয়ে (নিজের
        // dual-write সহ, ধাপ ৯) হয়ে গেছে।
        // [Step 7.9] RPC-এর জবাব থেকে 'refund_pending' ফ্ল্যাগ পড়া হয় (acceptBid()-এর 'result' field
        // পড়ার একই প্যাটার্নে) — এই মুহূর্তে dual-write ব্যর্থ হলে বা কোনো session না থাকলে false-ই
        // থাকবে (সেটা escrow HELD থাকার সিদ্ধান্তমূলক প্রমাণ না, শুধু জানানোর সুযোগ পাওয়া যায়নি)।
        var refundPending = false
        if (SupabaseAuthManager.currentUserId() != null) {
            SupabaseSyncManager.adminForceCancelInstantJob(problemId, reason, targetAction, progressStep)
                .onSuccess { json ->
                    refundPending = ((json as? kotlinx.serialization.json.JsonObject)?.get("refund_pending")
                        as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
                }
                .onFailure { e ->
                    Log.w("SomadhanRepo", "adminForceCancelInstantJob: Supabase dual-write failed for $problemId (local flow unaffected): ${e.message}")
                }
        }
        return refundPending
    }

    suspend fun checkAndExpireInstantJobs(): Int {
        val timeoutSeconds = platformSettingDao.getSetting("instant_job_broadcast_timeout_seconds")?.toLongOrNull() ?: 300L
        val timeoutMillis = timeoutSeconds * 1000L
        val now = System.currentTimeMillis()

        val allProblems = problemDao.getAllProblemsList()
        val broadcastingJobs = allProblems.filter {
            it.isInstantJob && it.jobStatus == "BROADCASTING" && !it.isUserDeleted && it.status != "CANCELLED" && it.status != "COMPLETED" && it.solverCancelledNotice.isNullOrBlank()
        }

        var expiredCount = 0
        for (problem in broadcastingJobs) {
            val timerStart = problem.broadcastTimerStartedAt ?: problem.createdAt
            if (timerStart > 0L && (timerStart + timeoutMillis) <= now) {
                val updated = problem.copy(
                    jobStatus = "CANCELLED",
                    isUserDeleted = true,
                    status = "CANCELLED",
                    lastActivityAt = now
                )
                problemDao.updateProblem(updated)

                // Cancel all pending bids & notify solvers
                val problemBids = bidDao.getBidsForProblemSync(problem.id)
                val progressStep = problem.calculateProgressStep()
                for (bid in problemBids) {
                    if (bid.status == "PENDING") {
                        val updatedBid = bid.copy(status = "CANCELLED", progressAtCancel = progressStep, resolutionType = "EXPIRED", resolvedAt = now)
                        bidDao.updateBid(updatedBid)

                        val solverNotif = NotificationEntity(
                            role = "SOLVER",
                            id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                            userId = bid.solverId,
                            title = "জরুরি জবটি বাতিল হয়েছে ⏱️",
                            message = "\"${problem.title}\" কাজের সময়সীমা শেষ হওয়ায় পোস্টটি বাতিল হয়ে গেছে।",
                            relatedProblemId = problem.id,
                            targetType = "problem",
                            targetId = problem.id
                        )
                        notificationDao.insertNotification(solverNotif)
                    }
                }

                // F.2: User notification
                val userNotif = NotificationEntity(
                    role = "USER",
                    id = "NOTIF_${UUID.randomUUID().toString().take(8)}",
                    userId = problem.userId,
                    title = "জরুরি পোস্ট বাতিল ⏱️",
                    message = "কোনো সলভার সময়মতো বিড না দেওয়ায় আপনার জরুরি পোস্টটি বাতিল হয়ে গেছে।",
                    relatedProblemId = problem.id,
                    targetType = "problem",
                    targetId = problem.id
                )
                notificationDao.insertNotification(userNotif)

                // [SUPABASE-MIGRATED - ধাপ ১২ ব্যাচ ৪গ] `expire_broadcasting_instant_job` RPC
                // ইচ্ছাকৃতভাবে per-problem/owner-scoped (RPC নিজেই auth.uid() = problems.user_id
                // চেক করে) — তাই guard এখানে current session-এর userId == এই problem-এর owner
                // কিনা মিলিয়ে করা হচ্ছে, শুধু session আছে কিনা তা না।
                // জানা সীমাবদ্ধতা: এই ফাংশনটা প্রতিটা ইউজারের ডিভাইস থেকে periodic
                // worker/alarm/startup-এ চলে আর সব ইউজারের broadcasting job scan করে — কিন্তু
                // dual-write শুধু তখনই সফল হবে যখন current logged-in ডিভাইসটাই সেই নির্দিষ্ট
                // problem-টার owner (নিজের job নিজে expire করলে)। অন্য কারো job এই sweep-এ local/
                // Firebase-এ ঠিকই expire হবে, কিন্তু Supabase dual-write তখন স্কিপ হয়ে যাবে (RPC
                // owner-check এ আটকাবে) — সেই problem-টা Supabase-এ dual-write হবে শুধু তখনই যখন
                // তার আসল owner-এর নিজের ডিভাইস কোনো এক সময় এই একই sweep চালাবে। local Room
                // flow (যেটা user-facing) সবসময় অপ্রভাবিত।
                if (SupabaseAuthManager.currentUserId() == problem.userId) {
                    SupabaseSyncManager.expireBroadcastingInstantJob(problem.id, progressStep).onFailure { e ->
                        Log.w("SomadhanRepo", "checkAndExpireInstantJobs: Supabase dual-write failed for ${problem.id} (local flow unaffected): ${e.message}")
                    }
                }

                expiredCount++
            }
        }
        return expiredCount
    }

    /**
     * ধাপ ৩৩.২ — "Force Sync" বাটনের পুরনো `FirebaseSyncManager.syncAllLocalToFirestore(users,
     * problems, bids, withdrawals)` কলের Supabase সমতুল্য। Firestore document-overwrite (idempotent)
     * থেকে ভিন্ন, Supabase-এর দিকটা raw `.insert()`/RLS-scoped কল দিয়ে বানানো — তাই এখানে
     * **ইচ্ছাকৃতভাবে withdrawals বাদ**: `requestWithdrawal()` একটা balance-বদলানো RPC (নতুন row +
     * সার্ভার-সাইড ব্যালেন্স ডিডাকশন), সেটা আগে-সিঙ্ক-হওয়া withdrawal-এর জন্য আবার কল করলে ডুপ্লিকেট
     * টাকা কাটা হতে পারে — non-idempotent, তাই bulk recovery-তে নিরাপদ না। withdrawal তৈরির সময়
     * `requestWithdrawal()` (repository-এর `requestWithdrawal()` ফাংশন দেখুন) ইতিমধ্যেই dual-write
     * করে, তাই এই recovery-নেট ছাড়াও সেটা কভার্ড।
     *
     * users/problems/bids-এর জন্য নিরাপদ কারণ: প্রতিটা শুধু **নিজের own row** হলেই (RLS/ownership
     * guard, স্বাভাবিক create path-এর মতোই) পাঠানো হয়, আর ইতিমধ্যে-সিঙ্ক-হওয়া row-এর জন্য duplicate-key
     * ব্যর্থতা শুধু log হয় (best-effort, `Result.failure` catch করা) — local flow কখনো ভাঙে না।
     */
    suspend fun syncAllLocalToSupabase(
        users: List<UserEntity>,
        problems: List<ProblemEntity>,
        bids: List<BidEntity>
    ) {
        val myId = SupabaseAuthManager.currentUserId() ?: return
        users.filter { it.id == myId }.forEach { user ->
            SupabaseSyncManager.updateOwnProfile(
                userId = user.id,
                name = user.name,
                address = user.address,
                latitude = user.latitude,
                longitude = user.longitude,
                profileImageUri = user.profileImageUri,
                solverCategories = user.solverCategories,
                favoriteSolverIds = user.favoriteSolverIds,
                hasCompletedSolverSetup = user.hasCompletedSolverSetup,
                email = user.email.ifBlank { null }
            ).onFailure { e ->
                Log.w("SomadhanRepo", "syncAllLocalToSupabase: profile push failed for ${user.id} (local flow unaffected): ${e.message}")
            }
        }
        problems.filter { it.userId == myId }.forEach { problem ->
            SupabaseSyncManager.createProblem(problem.toProblemDto()).onFailure { e ->
                // ইতিমধ্যে-সিঙ্ক-হওয়া problem-এর জন্য duplicate-key ব্যর্থতা প্রত্যাশিত ও নিরাপদ, তাই
                // শুধু log — এটাই এই recovery-loop-এর স্বাভাবিক আচরণ, নতুনগুলোর জন্যই insert সফল হবে।
                Log.d("SomadhanRepo", "syncAllLocalToSupabase: problem ${problem.id} push skipped/failed (likely already synced): ${e.message}")
            }
        }
        bids.filter { it.solverId == myId }.forEach { bid ->
            SupabaseSyncManager.createBid(bid.toBidDto()).onFailure { e ->
                Log.d("SomadhanRepo", "syncAllLocalToSupabase: bid ${bid.id} push skipped/failed (likely already synced): ${e.message}")
            }
        }
    }

    companion object {
        fun resolveSettlementAmounts(problem: ProblemEntity, escrow: EscrowEntity?): Pair<Double, Double> {
            val baseAmount = escrow?.baseAmount?.takeIf { it > 0.0 }
                ?: problem.acceptedAmount?.takeIf { it > 0.0 }
                ?: problem.minBudget.takeIf { it > 0.0 }
                ?: 0.0
            val extraAmount = if (problem.releaseRequestExtraAmount > 0.0) {
                problem.releaseRequestExtraAmount
            } else if (problem.confirmedExtraAmountTotal > 0.0) {
                problem.confirmedExtraAmountTotal
            } else {
                escrow?.extraAmount ?: 0.0
            }
            return Pair(baseAmount, extraAmount)
        }
    }

    fun resolveSettlementAmounts(problem: ProblemEntity, escrow: EscrowEntity?): Pair<Double, Double> {
        return SomadhanRepository.resolveSettlementAmounts(problem, escrow)
    }
}

data class MissingRefundRepairItem(
    val problemId: String,
    val problemTitle: String,
    val userId: String,
    val solverId: String,
    val escrowId: String,
    val baseAmount: Double,
    val extraAmount: Double,
    val totalAmount: Double,
    val status: String,
    val message: String
)

data class BalanceMismatchItem(
    val userId: String,
    val userName: String,
    // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ৪] কোন role-এর ledger-এ এই mismatch পাওয়া
    // গেছে ("USER" | "SOLVER") -- একজন dual-role ইউজারের দুই role-এই আলাদা mismatch থাকতে
    // পারে, তাই এখন সম্ভাব্য দুটো আলাদা BalanceMismatchItem আসতে পারে একই userId-এর জন্য।
    val role: String,
    val storedBalance: Double,
    val ledgerBalance: Double,
    // storedBalance - ledgerBalance. Positive => stored balance is too HIGH (was over-credited
    // somewhere and never corrected); negative => too LOW (a credit was lost/never synced).
    val difference: Double
)

data class BalanceReconciliationReport(
    val dryRun: Boolean,
    val scannedUsersCount: Int,
    val mismatchCount: Int,
    val correctedCount: Int,
    val totalAbsoluteDifference: Double,
    val items: List<BalanceMismatchItem>,
    // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ৪] role নির্ধারণ করা যায়নি এমন legacy
    // transaction (blank role + type ADMIN_ADJUSTMENT/অজানা type, ধাপ ৩-এর আগে তৈরি) --
    // কোনো role-এর ledger sum-এ গোনা হয়নি (ভুল role-এ ভুল correction এড়াতে), শুধু informational.
    // TRANSACTION_ROLE_FIELD_DESIGN.md #৫ (অপশন B) দ্রষ্টব্য।
    val unclassifiedLegacyCount: Int = 0,
    val unclassifiedLegacyNetAmount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

data class MissingRefundRepairReport(
    val dryRun: Boolean,
    val scannedEscrowsCount: Int,
    val missingRefundCount: Int,
    val repairedCount: Int,
    val totalAmountRepairedOrAudited: Double,
    val items: List<MissingRefundRepairItem>,
    val timestamp: Long = System.currentTimeMillis()
)


