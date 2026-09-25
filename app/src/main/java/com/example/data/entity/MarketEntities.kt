package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index("problemId"),
        Index("senderId"),
        Index("receiverId"),
        Index("timestamp"),
        Index("isRead")
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val problemId: String,
    val senderId: String,
    val receiverId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileType: String? = null, // "image", "document", "file"
    val isDirectContractProposal: Boolean = false,
    val directContractBudget: Double? = null,
    val directContractDuration: String? = null,
    val isAdminMessage: Boolean = false,
    val isDisputeNotice: Boolean = false,
    val isSystemEvent: Boolean = false,
    val systemEventType: String? = null,
    // [Offline Action Gating ধাপ ৯] messenger-স্টাইল send-status ট্র্যাকিং -- "PENDING" (এইমাত্র
    // local insert হয়েছে, cloud dual-write এখনো চলছে/হয়নি), "SENT" (dual-write সফল), "FAILED"
    // (dual-write ব্যর্থ -- অফলাইন বা network error, ইউজার ChatScreen-এ ইনলাইন "retry" ট্যাপ করে
    // আবার পাঠাতে পারবে)। ডিফল্ট "SENT" রাখা হয়েছে যাতে মাইগ্রেশনের আগে ইতিমধ্যে পাঠানো পুরনো
    // মেসেজগুলো (যেগুলো আসলে already-synced ধরে নেওয়া নিরাপদ) ভুলভাবে "failed" না দেখায়।
    val sendStatus: String = "SENT"
)

@Entity(
    tableName = "ratings",
    indices = [
        Index("problemId"),
        Index("solverId"),
        Index("userId"),
        Index("raterRole"),
        Index("createdAt")
    ]
)
data class RatingEntity(
    @PrimaryKey val id: String,
    val problemId: String,
    val problemTitle: String,
    val userId: String,
    val userName: String,
    val solverId: String,
    val solverName: String,
    val stars: Int,
    val comment: String,
    val raterRole: String = "USER", // "USER" মানে User→Solver রেটিং (আগের মতোই ডিফল্ট), "SOLVER" মানে Solver→User রেটিং (নতুন)
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notifications",
    indices = [
        Index("userId"),
        Index("isRead"),
        Index("timestamp"),
        Index("targetType")
    ]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val relatedProblemId: String? = null,
    val targetType: String = "problem", // "problem", "kyc", "balance", "role", "category"
    val targetId: String? = null,
    val scheduledFor: Long? = null,
    // [ROLE_SEPARATION ধাপ ৬] কোন role-এর ঘটনার জন্য এই notification — "USER"/"SOLVER"/""
    // (blank = role-neutral, দুই role-এই দেখাবে)। পুরনো row-এ ডিফল্ট "" থাকবে (ভাঙবে না)।
    val role: String = ""
)

@Entity(
    tableName = "withdrawals",
    indices = [
        Index("solverId"),
        Index("status"),
        Index("createdAt")
    ]
)
data class WithdrawalEntity(
    @PrimaryKey val id: String,
    val solverId: String,
    val solverName: String,
    val amount: Double,
    val method: String, // "বিকাশ", "নগদ", "রকেট", "ব্যাংক"
    val accountNumber: String,
    val bankName: String? = null,
    val branchName: String? = null,
    val accountHolderName: String? = null,
    val status: String = "PENDING", // "PENDING", "COMPLETED", "REJECTED"
    val trxId: String? = null,
    val rejectionReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    indices = [
        Index("userId"),
        Index("solverId"),
        Index("problemId"),
        Index("timestamp")
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val problemId: String,
    val problemTitle: String,
    val userId: String,
    val solverId: String,
    val grossAmount: Double,
    val commissionPercent: Double,
    val commissionAmount: Double,
    val netAmount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val baseAmount: Double = 0.0,
    val extraAmount: Double = 0.0,
    val baseCommissionAmount: Double = 0.0,
    val extraCommissionAmount: Double = 0.0,
    val extraCommissionApplied: Boolean = false, // toggle ON ছিল কিনা, ঐ মুহূর্তে
    val wasFreeQuotaJob: Boolean = false, // এই জবটা reputation free-quota দিয়ে কমিশন-ফ্রি হয়েছিল কিনা
    val type: String = "PAYMENT", // "PAYMENT" (সম্পন্ন কাজের পেমেন্ট), "REFUND" (রিফান্ড), "CANCELLED_EXTRA" (বাতিল হওয়া কাজের অতিরিক্ত বিল)
    val escrowId: String = "",
    // [Step 7.7 cleanup, ২০২৬-০৯-২৪] pendingCloudSync/cloudBalanceSynced (নিচে) দুটোই
    // Firebase-era leftover bookkeeping flag -- সেই সময় FirebaseSyncManager.syncPendingCloudRefunds()
    // এই flag পড়ে retry করত। সেই ফাংশন এখন কোডবেসে নেই, আর কোনো DAO/worker এই দুটো flag read
    // করে retry করে না -- সেট করা হয় (বেশিরভাগ balance-change call-site-এ, প্রায় সবসময়
    // pendingCloudSync = true দিয়ে), কিন্তু কখনো পড়া হয় না। আসল cloud-sync retry মেকানিজম এখন
    // Supabase RPC dual-write + enqueueOutboxRetry()/OutboxSyncWorker (outbox queue) -- এই দুটো
    // flag সরিয়ে ফেলা এই cleanup-এর স্কোপে ধরা হয়নি (schema/migration change, বড় ঝুঁকি), শুধু
    // এখানে নোট রাখা হলো যাতে ভবিষ্যতে কেউ এগুলোকে সক্রিয় retry মেকানিজম না ভাবে।
    val pendingCloudSync: Boolean = false,
    val refundType: String = "", // "SOLVER_CANCEL", "DISPUTE_REFUND", "SPLIT_REFUND"
    val refundPercentage: Double = 100.0, // e.g. 50.0, 60.0, 100.0
    val cloudBalanceSynced: Boolean = false,
    val releaseType: String = "", // for type="PAYMENT" only: "DISPUTE_RELEASE", "SPLIT_RELEASE" (blank = ordinary job-completion payment). Kept separate from refundType because isRefundTrx() treats any non-blank refundType as a refund signal.
    val role: String = "" // "USER" | "SOLVER" | "" (legacy row created before this field existed, or a legacy ambiguous case like an ADMIN-role user's own correction). Says which role's ledger (balanceUser/balanceSolver) this transaction's money counts against -- NOT necessarily which of userId/solverId got paid. ধাপ ৩ (TRANSACTION_ROLE_FIELD_DESIGN.md) অনুযায়ী যোগ করা হলো।
)

@Entity(tableName = "platform_settings")
data class PlatformSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(
    tableName = "escrows",
    indices = [
        Index("problemId"),
        Index("userId"),
        Index("solverId"),
        Index("status")
    ]
)
data class EscrowEntity(
    @PrimaryKey val id: String,
    val problemId: String,
    val problemTitle: String = "",
    val userId: String,
    val solverId: String,
    val baseAmount: Double,
    val extraAmount: Double = 0.0,
    val status: String = "HELD", // "HELD", "RELEASED", "REFUNDED"
    val createdAt: Long = System.currentTimeMillis(),
    val releasedAt: Long? = null,
    // Bug fix: stamped every time extraAmount changes (additional-charge accept), so the
    // real-time Supabase listener (formerly the Firestore listener, before that path was removed)
    // can tell a freshly-incremented local extraAmount apart from
    // a stale/in-flight cloud snapshot and avoid overwriting it. Without this, createdAt/
    // releasedAt never change on an extra-bill accept, so getEscrowTimestamp() couldn't detect
    // staleness and a lagging cloud snapshot could silently wipe out part of the locked amount.
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "additional_charges",
    indices = [
        Index("problemId"),
        Index("solverId"),
        Index("userId"),
        Index("status")
    ]
)
data class AdditionalChargeEntity(
    @PrimaryKey val id: String,
    val problemId: String,
    val solverId: String,
    val userId: String,
    val reason: String,
    val amount: Double,
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "REJECTED"
    val createdAt: Long = System.currentTimeMillis(),
    val respondedAt: Long? = null
)

@Entity(
    tableName = "reputation_events",
    indices = [
        Index("userId"),
        Index("problemId"),
        Index("createdAt")
    ]
)
data class ReputationEventEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val eventType: String,
    val problemId: String? = null,
    val scoreChange: Double,
    val scoreAfter: Double,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "gateway_payments",
    indices = [
        Index("userId"),
        Index("gatewayTrxId"),
        Index("gateway"),
        Index("status"),
        Index("purpose"),
        Index("timestamp")
    ]
)
data class GatewayPaymentEntity(
    @PrimaryKey val id: String,
    val gatewayTrxId: String, // e.g. TRX92817281, 9HA82KS9
    val userId: String,
    val userName: String = "",
    val userPhone: String = "",
    val amount: Double,
    val gateway: String, // "BKASH", "NAGAD", "ROCKET", "CARD"
    val purpose: String = "WALLET_DEPOSIT", // "WALLET_DEPOSIT" (ওয়ালেট রিচার্জ), "ESCROW_PAYMENT" (এসক্রো পেমেন্ট), "EXTRA_BILL" (অতিরিক্ত বিল), "DIRECT_PAYMENT"
    val problemId: String = "",
    val problemTitle: String = "",
    val status: String = "SUCCESS", // "SUCCESS", "PENDING", "FAILED"
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

