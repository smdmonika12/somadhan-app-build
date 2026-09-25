package com.example.data.remote

import com.example.data.entity.AdditionalChargeEntity
import com.example.data.entity.EscrowEntity
import com.example.data.entity.GatewayPaymentEntity
import com.example.data.entity.MessageEntity
import com.example.data.entity.NotificationEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.WithdrawalEntity
import com.example.data.remote.dto.AdditionalChargeDto
import com.example.data.remote.dto.EscrowDto
import com.example.data.remote.dto.GatewayPaymentDto
import com.example.data.remote.dto.MessageDto
import com.example.data.remote.dto.NotificationDto
import com.example.data.remote.dto.TransactionDto
import com.example.data.remote.dto.WithdrawalDto

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ২১ (Realtime Foundation B)
 *
 * `ProblemBidMappers.kt`/`UserMappers.kt`-এর read-side (Dto → Entity) mapper-দের মতোই এই ফাইলে
 * `messages`/`transactions`/`escrows`/`gateway_payments` — এই ৪টা টেবিলের Dto → Entity mapper আছে,
 * `SupabaseRealtimeManager.kt`-এর channel handler-দের জন্য (তখন ধাপ ২১-এ লেখার সময় এখনো wire
 * করা হয়নি, কিন্তু ধাপ ২২-এর Cutover-এর পর থেকে এখন সক্রিয়ভাবে ব্যবহৃত হচ্ছে)।
 * `ChatRatingMappers.kt`-এ `MessageEntity.toMessageDto()` (write দিক) আগে থেকেই আছে, তাই এখানে শুধু
 * বিপরীত দিকটা (read/incoming) যোগ করা হলো -- দুটো ফাইল মিলিয়ে `MessageEntity`/`MessageDto` দুই
 * দিকেই সম্পূর্ণ।
 *
 * timestamp ফিল্ডগুলো (`timestamp`/`created_at`/`released_at`/`updated_at`) সব
 * `SupabaseTimestampUtil.parseTimestamptz()` দিয়ে পার্স করা হয়েছে, `ProblemBidMappers.kt`-এর মতোই।
 */

/**
 * `পুরনো (এখন মুছে ফেলা) FirebaseSyncManager.mapToMessageEntity()`-এর isAdmin heuristic এখানে অবিকল রাখা হয়েছে
 * (defensive fallback -- `is_admin_message` কলাম সঠিকভাবে সেট না থাকা পুরনো/অস্বাভাবিক রো থাকলেও
 * সেটা এডমিন মেসেজ হিসেবে চেনার জন্য)। `MessageEntity.isRead` read-receipt merge (local-read-wins)
 * এই mapper-এর দায়িত্ব না -- সেটা `SupabaseRealtimeManager.handleMessageAction()`-এ (caller-সাইডে)
 * হয়, `পুরনো (এখন মুছে ফেলা) FirebaseSyncManager.handleMessagesSnapshot()`-এর প্যাটার্ন অনুসরণ করে।
 */
fun MessageDto.toMessageEntity(): MessageEntity {
    val resolvedSenderId = senderId ?: ""
    val rawIsAdmin = isAdminMessage
    val isAdmin = rawIsAdmin ||
        senderName.contains("Support Manager", ignoreCase = true) ||
        senderName.contains("সাপোর্ট ম্যানেজার", ignoreCase = true) ||
        senderName.contains("অ্যাডমিন", ignoreCase = true) ||
        senderName.contains("Admin", ignoreCase = true) ||
        resolvedSenderId.startsWith("ADMIN", ignoreCase = true) ||
        resolvedSenderId.equals("ADMIN_SYSTEM", ignoreCase = true) ||
        resolvedSenderId.equals("admin", ignoreCase = true)

    return MessageEntity(
        id = id,
        problemId = problemId,
        senderId = resolvedSenderId,
        receiverId = receiverId ?: "",
        senderName = senderName,
        content = content,
        timestamp = SupabaseTimestampUtil.parseTimestamptz(timestamp) ?: System.currentTimeMillis(),
        isRead = isRead,
        fileUrl = fileUrl,
        fileName = fileName,
        fileType = fileType,
        isDirectContractProposal = isDirectContractProposal,
        directContractBudget = directContractBudget,
        directContractDuration = directContractDuration,
        isAdminMessage = isAdmin,
        isDisputeNotice = isDisputeNotice,
        isSystemEvent = isSystemEvent,
        systemEventType = systemEventType
    )
}

/**
 * `পুরনো (এখন মুছে ফেলা) FirebaseSyncManager.mapToTransactionEntity()`-এর মতোই `type` ফাঁকা এলে id-ভিত্তিক অনুমান
 * (REFUND/WALLET_DEPOSIT/PAYMENT) fallback রাখা হয়েছে -- বাস্তবে Supabase RPC-গুলো `type`
 * সবসময় সেট করে, কিন্তু defensively রাখা হলো (পুরনো/অস্বাভাবিক row-এর জন্য)।
 * `pendingCloudSync`/`cloudBalanceSynced` -- এই দুটো পুরনো (এখন মুছে ফেলা) Firestore dual-write যুগের local-only
 * bookkeeping ফ্ল্যাগ, Supabase Dto-তে নেই; realtime থেকে আসা রো-কে ইতিমধ্যে "cloud-synced" ধরে
 * `false`/`true`-এর বদলে উভয়ই ডিফল্ট (false/false) রাখা হয়েছে -- এই দুটো ফ্ল্যাগ আদৌ ব্যবহৃত হয়
 * কিনা Supabase write path-এ (নাকি পুরোপুরি dead) তা এই ধাপে যাচাই করা হয়নি, cutover ধাপে
 * (ধাপ ২২) খেয়াল রাখা দরকার।
 */
fun TransactionDto.toTransactionEntity(): TransactionEntity {
    val rawType = type.trim()
    val resolvedType = when {
        rawType.isNotBlank() -> rawType
        id.startsWith("TRX_REFUND", ignoreCase = true) || id.contains("REFUND", ignoreCase = true) -> "REFUND"
        id.startsWith("TRX_DEP", ignoreCase = true) || id.contains("DEPOSIT", ignoreCase = true) -> "WALLET_DEPOSIT"
        else -> "PAYMENT"
    }
    return TransactionEntity(
        id = id,
        problemId = problemId,
        problemTitle = problemTitle,
        userId = userId ?: "",
        solverId = solverId ?: "",
        grossAmount = grossAmount,
        commissionPercent = commissionPercent,
        commissionAmount = commissionAmount,
        netAmount = netAmount,
        timestamp = SupabaseTimestampUtil.parseTimestamptz(timestamp) ?: System.currentTimeMillis(),
        baseAmount = baseAmount,
        extraAmount = extraAmount,
        baseCommissionAmount = baseCommissionAmount,
        extraCommissionAmount = extraCommissionAmount,
        extraCommissionApplied = extraCommissionApplied,
        wasFreeQuotaJob = wasFreeQuotaJob,
        type = resolvedType,
        escrowId = escrowId ?: "",
        refundType = refundType,
        refundPercentage = refundPercentage,
        releaseType = releaseType,
        role = role
    )
}

/**
 * `পুরনো (এখন মুছে ফেলা) FirebaseSyncManager.mapToEscrowEntity()`-এর মতোই `updated_at` না থাকলে `created_at`-এ fallback
 * করা হয়েছে (পুরনো row staleness-guard-এ ভুলভাবে fresh/stale না দেখাক)। extraAmount/updatedAt
 * local-preserve merge (staleness guard) এবং `resolveIncomingEscrowStatus()`-এর সমতুল্য
 * cancelled/refund-reclassify লজিক এই mapper-এর দায়িত্ব না -- caller (`SupabaseRealtimeManager.
 * handleEscrowAction()`) সেটা করে, `পুরনো (এখন মুছে ফেলা) FirebaseSyncManager.handleEscrowsSnapshot()`-এর প্যাটার্ন
 * অনুসরণ করে।
 */
fun EscrowDto.toEscrowEntity(): EscrowEntity = EscrowEntity(
    id = id,
    problemId = problemId,
    problemTitle = problemTitle,
    userId = userId,
    solverId = solverId,
    baseAmount = baseAmount,
    extraAmount = extraAmount,
    status = status,
    createdAt = SupabaseTimestampUtil.parseTimestamptz(createdAt) ?: System.currentTimeMillis(),
    releasedAt = SupabaseTimestampUtil.parseTimestamptz(releasedAt),
    updatedAt = SupabaseTimestampUtil.parseTimestamptz(updatedAt)
        ?: SupabaseTimestampUtil.parseTimestamptz(createdAt)
        ?: System.currentTimeMillis()
)

/** `পুরনো (এখন মুছে ফেলা) FirebaseSyncManager.mapToGatewayPaymentEntity()`-এর সমতুল্য, কোনো বিশেষ merge/staleness লজিক নেই মূল ফাংশনেও। */
fun GatewayPaymentDto.toGatewayPaymentEntity(): GatewayPaymentEntity = GatewayPaymentEntity(
    id = id,
    gatewayTrxId = gatewayTrxId,
    userId = userId ?: "",
    userName = userName,
    userPhone = userPhone,
    amount = amount,
    gateway = gateway,
    purpose = purpose,
    problemId = problemId,
    problemTitle = problemTitle,
    status = status,
    note = note,
    timestamp = SupabaseTimestampUtil.parseTimestamptz(timestamp) ?: System.currentTimeMillis()
)

/**
 * ধাপ ৩২.৫: `additional_charges` টেবিলের জন্য Dto → Entity mapper (আগে ছিল না)।
 * `createdAt`/`respondedAt` timestamptz — `SupabaseTimestampUtil.parseTimestamptz()` দিয়ে,
 * বাকি ফাইলের প্যাটার্নে। `respondedAt` null-permitted (charge এখনো PENDING থাকলে খালি থাকে)।
 */
fun AdditionalChargeDto.toAdditionalChargeEntity(): AdditionalChargeEntity = AdditionalChargeEntity(
    id = id,
    problemId = problemId,
    solverId = solverId,
    userId = userId,
    reason = reason,
    amount = amount,
    status = status,
    createdAt = SupabaseTimestampUtil.parseTimestamptz(createdAt) ?: System.currentTimeMillis(),
    respondedAt = SupabaseTimestampUtil.parseTimestamptz(respondedAt)
)

/**
 * ধাপ ৩৫ (ব্যবহারকারীর অনুরোধে, notifications/withdrawals realtime): `notifications` টেবিলের
 * জন্য Dto → Entity mapper (আগে ছিল না, কারণ এই টেবিল এতদিন কোনো realtime channel-এই ছিল না)।
 * কোনো বিশেষ merge/staleness লজিক নেই -- `additional_charges`/`gateway_payments`-এর মতোই সরাসরি
 * overwrite (notification row তৈরির পর content বদলায় না, শুধু isRead ফ্ল্যাগ বদলাতে পারে, যেটা
 * এই mapper-ই ঠিকভাবে carry করে কারণ DTO-তেও isRead আছে)।
 */
fun NotificationDto.toNotificationEntity(): NotificationEntity = NotificationEntity(
    id = id,
    userId = userId,
    title = title,
    message = message,
    timestamp = SupabaseTimestampUtil.parseTimestamptz(timestamp) ?: System.currentTimeMillis(),
    isRead = isRead,
    relatedProblemId = relatedProblemId,
    targetType = targetType,
    targetId = targetId,
    scheduledFor = SupabaseTimestampUtil.parseTimestamptz(scheduledFor),
    // Supabase cloud-sync গ্যাপ ফিক্স: role এখন DTO থেকে সরাসরি pass-through (আগে এই লাইন
    // অনুপস্থিত ছিল বলে সব cloud-origin notification role="" হয়ে যেত, ফলে multi-device-এ
    // sync হয়ে আসা notification role-filter এড়িয়ে দুই role-এই দেখাত)।
    role = role
)

/**
 * ধাপ ৩৫ (ব্যবহারকারীর অনুরোধে, notifications/withdrawals realtime): `withdrawals` টেবিলের জন্য
 * Dto → Entity mapper (আগে ছিল না, একই কারণে উপরের notifications mapper-এর মতো)। কোনো বিশেষ
 * merge/staleness লজিক নেই -- status PENDING→COMPLETED/REJECTED একবারই বদলায় (`process_withdrawal`
 * RPC-এর মাধ্যমে), conflict হওয়ার সুযোগ কম, তাই সরাসরি overwrite।
 */
fun WithdrawalDto.toWithdrawalEntity(): WithdrawalEntity = WithdrawalEntity(
    id = id,
    solverId = solverId,
    solverName = solverName,
    amount = amount,
    method = method,
    accountNumber = accountNumber,
    bankName = bankName,
    branchName = branchName,
    accountHolderName = accountHolderName,
    status = status,
    trxId = trxId,
    rejectionReason = rejectionReason,
    createdAt = SupabaseTimestampUtil.parseTimestamptz(createdAt) ?: System.currentTimeMillis()
)
