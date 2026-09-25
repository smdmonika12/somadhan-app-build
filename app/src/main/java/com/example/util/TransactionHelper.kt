package com.example.util

import com.example.data.entity.ProblemEntity
import com.example.data.entity.TransactionEntity

enum class RefundCategory {
    SOLVER_CANCEL,
    DISPUTE_REFUND,
    SPLIT_REFUND
}

enum class ReleaseCategory {
    NORMAL,
    DISPUTE_RELEASE,
    SPLIT_RELEASE
}

data class ReleaseMeta(
    val category: ReleaseCategory,
    val labelBn: String,
    val labelEn: String,
    val badgeBn: String,
    val badgeEn: String
)

data class RefundMeta(
    val category: RefundCategory,
    val percentage: Double,
    val labelBn: String,
    val labelEn: String,
    val badgeBn: String,
    val badgeEn: String
)

object TransactionHelper {

    // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ৫, TRANSACTION_ROLE_FIELD_DESIGN.md #৫ অপশন B]
    // SomadhanRepository.resolveLegacyTransactionRole()-এর (ধাপ ৪-এ প্রথম যোগ হয়েছিল,
    // reconcileUserBalances()-এর জন্য) হুবহু কপি -- ইচ্ছাকৃতভাবে এখানে util লেয়ারে তোলা হলো
    // যাতে UI স্ক্রিনগুলোও (এই ধাপে TransactionHistoryScreen/UserWalletScreen/DashboardScreen/
    // SolverCompletedJobsScreen) একই ম্যাপিং ব্যবহার করে -- Repository-র প্রাইভেট ফাংশনটা এই
    // ধাপের বর্ণিত স্কোপের বাইরে বলে (General Rule #১) হাত দেওয়া হয়নি, তাই আপাতত দুই জায়গায়
    // দুটো কপি আছে। **এই দুটো ম্যাপিং ভবিষ্যতে বদলালে দুই জায়গাতেই বদলাতে হবে** (drift-ঝুঁকি,
    // রিপোর্টে আলাদা করে উল্লেখ করা হয়েছে)।
    fun resolveLegacyTransactionRole(trx: TransactionEntity): String? = when (trx.type) {
        "PAYMENT" -> "SOLVER"
        "WALLET_DEPOSIT", "REFUND", "BID_ACCEPT_DEDUCTION", "RELEASE_DEDUCTION",
        "EXTRA_CHARGE_DEDUCTION", "DUPLICATE_CORRECTION" -> "USER"
        "WITHDRAWAL_DEDUCTION", "WITHDRAWAL_REFUND" -> "SOLVER"
        else -> null // ADMIN_ADJUSTMENT, BALANCE_RECONCILIATION, বা অজানা কোনো type
    }

    // এই transaction-টা কার্যকরভাবে কোন role-এর ledger-এর -- সরাসরি trx.role (ধাপ ৩-এর পরে
    // তৈরি সব row-এ থাকে), blank হলে (ধাপ ৩-এর আগের legacy row) টাইপ থেকে অনুমান।
    fun effectiveRole(trx: TransactionEntity): String? =
        trx.role.ifBlank { resolveLegacyTransactionRole(trx) }

    // [ধাপ ৫ প্রশ্ন #১-#৩-এর ব্যবহারকারী-কনফার্ম করা উত্তর]
    // #১-#২: এই অ্যাকাউন্টের USER/SOLVER role-এর transaction history সম্পূর্ণ আলাদা -- একই
    // অ্যাকাউন্ট দুটো role ব্যবহার করলেও দুই role-এর মধ্যে balance/history-এর কোনো সম্পর্ক নেই
    // (শুধু নাম/ছবি/identity শেয়ার হয়)। তাই PAYMENT-এর মতো টাইপ, যেখানে এই অ্যাকাউন্ট job
    // poster হিসেবে userId/problemId-তে জড়িত থাকলেও role টেকনিক্যালি "SOLVER" (অন্য পক্ষের
    // income), সেটা এই অ্যাকাউন্টের USER-role history-তে দেখানো হবে না -- শুধু trx.role (বা
    // legacy হলে resolveLegacyTransactionRole()) যেই role বলে, সেই role-এর history-তেই দেখানো
    // হবে।
    // #৩: পুরনো (ধাপ ৩-এর আগের) blank-role row যেগুলোর role টাইপ থেকেও অনুমান করা যায় না
    // (ADMIN_ADJUSTMENT/BALANCE_RECONCILIATION-এর সত্যিকারের অনির্ধারণযোগ্য legacy কেস) --
    // ব্যবহারকারীর সিদ্ধান্ত "business model অনুযায়ী যেটা best" অনুযায়ী স্থির করা হলো: বাদ দিলে
    // ব্যবহারকারীর কাছে টাকা "হারিয়ে যাওয়ার" মতো মনে হবে (মাস্টার প্রম্পটের নিজেরই ইঙ্গিত করা
    // ঝুঁকি), তাই এই সত্যিকারের-অনির্ধারণযোগ্য row-গুলো দুই role-এর history-তেই দেখানো হবে।
    // এটা ইচ্ছাকৃতভাবে reconcileUserBalances()-এর UNCLASSIFIED_LEGACY আচরণ (কোনো ledger-sum-এ
    // যোগ না করা) থেকে আলাদা -- ওখানে ভুল role-এ ভুল balance-correction হওয়ার আর্থিক ঝুঁকি
    // ছিল, এখানে শুধু read-only history display, কোনো ঝুঁকি নেই।
    fun matchesRoleForHistory(trx: TransactionEntity, targetRole: String, accountId: String): Boolean {
        if (accountId.isBlank()) return false
        if (trx.userId != accountId && trx.solverId != accountId) return false
        val role = effectiveRole(trx)
        return role == null || role == targetRole
    }

    fun isDepositTrx(trx: TransactionEntity): Boolean {
        return trx.type.equals("WALLET_DEPOSIT", ignoreCase = true) ||
                trx.type.equals("DEPOSIT", ignoreCase = true) ||
                trx.type.equals("RECHARGE", ignoreCase = true) ||
                trx.type.equals("WALLET_RECHARGE", ignoreCase = true) ||
                trx.id.startsWith("TRX_DEP", ignoreCase = true) ||
                trx.problemTitle.contains("ওয়ালেট রিচার্জ", ignoreCase = true) ||
                trx.problemTitle.contains("টপ আপ", ignoreCase = true) ||
                trx.problemTitle.contains("রিচার্জ", ignoreCase = true)
    }

    fun isRefundTrx(trx: TransactionEntity): Boolean {
        if (isDepositTrx(trx)) return false
        return trx.type.equals("REFUND", ignoreCase = true) ||
                trx.type.equals("DISPUTE_REFUND", ignoreCase = true) ||
                trx.type.equals("SPLIT_REFUND", ignoreCase = true) ||
                trx.id.startsWith("TRX_REFUND", ignoreCase = true) ||
                trx.id.contains("REFUND", ignoreCase = true) ||
                trx.refundType.isNotBlank() ||
                trx.problemTitle.contains("রিফান্ড", ignoreCase = true) ||
                trx.problemTitle.contains("বাতিল", ignoreCase = true) ||
                (trx.commissionAmount == 0.0 && trx.commissionPercent == 0.0 && trx.grossAmount > 0.0 && trx.grossAmount == trx.netAmount && trx.problemId.isNotBlank() && (trx.solverId.isBlank() || trx.solverId == "N/A" || trx.solverId == "প্রযোজ্য নয়"))
    }

    fun isPaymentTrx(trx: TransactionEntity): Boolean {
        return !isDepositTrx(trx) && !isRefundTrx(trx)
    }

    fun getRefundMeta(trx: TransactionEntity, problem: ProblemEntity? = null): RefundMeta {
        val rawRefundType = trx.refundType.trim().uppercase()
        val rawType = trx.type.trim().uppercase()
        val rawId = trx.id.uppercase()
        val title = trx.problemTitle

        val category: RefundCategory
        var pct = trx.refundPercentage

        when {
            rawRefundType == "SPLIT_REFUND" || rawType == "SPLIT_REFUND" || rawId.contains("SPLIT") || title.contains("স্প্লিট", ignoreCase = true) || title.contains("SPLIT", ignoreCase = true) -> {
                category = RefundCategory.SPLIT_REFUND
                if (pct <= 0.0 || pct >= 100.0) {
                    if (problem != null && problem.disputeSplitSolverPercent != null && problem.disputeSplitSolverPercent > 0.0) {
                        pct = (100.0 - problem.disputeSplitSolverPercent).coerceIn(0.0, 100.0)
                    } else {
                        pct = 50.0
                    }
                }
            }
            rawRefundType == "DISPUTE_REFUND" || rawType == "DISPUTE_REFUND" || rawId.contains("DISPUTE") || title.contains("ডিসপিউট", ignoreCase = true) || title.contains("DISPUTE", ignoreCase = true) -> {
                category = RefundCategory.DISPUTE_REFUND
                pct = 100.0
            }
            // IMPORTANT: a plain solver-cancellation refund permanently records
            // refundType = "SOLVER_CANCEL" on ITS OWN transaction row at the moment it's
            // created (see refundEscrowOnce()'s default parameter). That's a fact about this
            // specific payment cycle and must never change. Without this explicit branch, such
            // a transaction fell through to the problem?.disputeResolutionDecision checks below
            // — but problemDao only stores ONE row per problemId, shared across every bid/escrow
            // cycle that post ever goes through. If a LATER, unrelated bid on the same post gets
            // disputed and resolved as a split/refund, that shared field changes — and the next
            // time this OLDER solver-cancel transaction is re-rendered (e.g. list recomposition),
            // it would silently pick up the newer cycle's tag ("স্প্লিট রিফান্ড") even though
            // nothing about this transaction itself changed. Checking the transaction's own
            // recorded refundType FIRST, before ever consulting the shared problem row, is what
            // keeps a transaction's tag permanently tied to what actually happened in its own
            // payment cycle.
            rawRefundType == "SOLVER_CANCEL" -> {
                category = RefundCategory.SOLVER_CANCEL
                pct = 100.0
            }
            problem?.disputeResolutionDecision == "SPLIT_SETTLEMENT" || problem?.disputeResolutionDecision == "CUSTOM_SPLIT" -> {
                category = RefundCategory.SPLIT_REFUND
                if (pct <= 0.0 || pct >= 100.0) {
                    val solverPct = problem.disputeSplitSolverPercent ?: 50.0
                    pct = (100.0 - solverPct).coerceIn(0.0, 100.0)
                }
            }
            problem?.disputeResolutionDecision == "REFUND_TO_USER" -> {
                category = RefundCategory.DISPUTE_REFUND
                pct = 100.0
            }
            else -> {
                category = RefundCategory.SOLVER_CANCEL
                pct = 100.0
            }
        }

        val pctInt = pct.toInt()
        val pctBn = DistanceUtil.toBengaliDigits(pctInt.toString())

        return when (category) {
            RefundCategory.SPLIT_REFUND -> {
                RefundMeta(
                    category = category,
                    percentage = pct,
                    labelBn = "স্প্লিট রিফান্ড ($pctBn%)",
                    labelEn = "Split Refund ($pctInt%)",
                    badgeBn = "✂️ স্প্লিট রিফান্ড ($pctBn%)",
                    badgeEn = "✂️ Split Refund ($pctInt%)"
                )
            }
            RefundCategory.DISPUTE_REFUND -> {
                RefundMeta(
                    category = category,
                    percentage = 100.0,
                    labelBn = "ডিসপিউট রিফান্ড",
                    labelEn = "Dispute Refund",
                    badgeBn = "⚖️ ডিসপিউট রিফান্ড (Dispute Refund)",
                    badgeEn = "⚖️ Dispute Refund"
                )
            }
            RefundCategory.SOLVER_CANCEL -> {
                RefundMeta(
                    category = category,
                    percentage = 100.0,
                    labelBn = "রিফান্ড",
                    labelEn = "Refund",
                    badgeBn = "🔄 রিফান্ড (Refund)",
                    badgeEn = "🔄 Refund"
                )
            }
        }
    }

    // Mirror of getRefundMeta(), but for the solver's PAYMENT side: distinguishes an ordinary
    // job-completion payment from one that resulted from an admin dispute resolution (full
    // release, or this solver's share of a split). Reads ONLY trx.releaseType (stamped once, at
    // the moment this specific transaction was created) -- never a shared/mutable problem-level
    // field -- so this tag stays permanently correct regardless of what happens in later,
    // unrelated cycles on the same problem.
    fun getReleaseMeta(trx: TransactionEntity): ReleaseMeta {
        return when (trx.releaseType.trim().uppercase()) {
            "SPLIT_RELEASE" -> ReleaseMeta(
                category = ReleaseCategory.SPLIT_RELEASE,
                labelBn = "স্প্লিট রিলিজ",
                labelEn = "Split Release",
                badgeBn = "✂️ স্প্লিট রিলিজ (Split Release)",
                badgeEn = "✂️ Split Release"
            )
            "DISPUTE_RELEASE" -> ReleaseMeta(
                category = ReleaseCategory.DISPUTE_RELEASE,
                labelBn = "ডিসপিউট রিলিজ",
                labelEn = "Dispute Release",
                badgeBn = "⚖️ ডিসপিউট রিলিজ (Dispute Release)",
                badgeEn = "⚖️ Dispute Release"
            )
            // Bug fix: a plain, non-dispute successful release used to get an empty
            // labelBn/labelEn here, which meant getDisplayTitle()'s "else" branch had nothing
            // to show for it and fell through to the generic "সমস্যা: <title>" fallback. A
            // normal release is still a release and should say so.
            else -> ReleaseMeta(
                category = ReleaseCategory.NORMAL,
                labelBn = "রিলিজ",
                labelEn = "Release",
                badgeBn = "✅ রিলিজ (Release)",
                badgeEn = "✅ Release"
            )
        }
    }

    fun cleanProblemTitle(title: String): String {
        return title
            .replace(Regex("^(রিফান্ড|স্প্লিট রিফান্ড|ডিসপিউট রিফান্ড|স্প্লিট রিলিজ|ডিসপিউট রিলিজ|Refund|Dispute Refund|Split Refund|Dispute Release|Split Release)\\s*(\\(.*?\\))?\\s*[:\\-–—]?\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    fun getDisplayTitle(trx: TransactionEntity, problem: ProblemEntity? = null): String {
        val clean = cleanProblemTitle(trx.problemTitle)
        return when {
            isDepositTrx(trx) -> if (clean.isNotBlank()) clean else "ওয়ালেট রিচার্জ (টপ-আপ)"
            isRefundTrx(trx) -> {
                val meta = getRefundMeta(trx, problem)
                if (clean.isNotBlank()) "${meta.labelBn}: $clean" else "${meta.labelBn} লেনদেন"
            }
            // Bug fix: this branch used to only special-case dispute/split releases
            // (releaseMeta.category != NORMAL) and dumped every other payment-side transaction
            // -- bid-accept wallet deduction, extra-bill wallet deduction, the release-side
            // customer ledger deduction, and even a plain non-dispute release -- into the
            // generic "সমস্যা: <title>" fallback. Each of those has its own trx.type and needs
            // its own specific tag instead.
            trx.type.equals("BID_ACCEPT_DEDUCTION", ignoreCase = true) -> {
                if (clean.isNotBlank()) "বিড গ্রহণ: $clean" else "বিড গ্রহণ লেনদেন"
            }
            trx.type.equals("EXTRA_CHARGE_DEDUCTION", ignoreCase = true) -> {
                if (clean.isNotBlank()) "অতিরিক্ত বিল: $clean" else "অতিরিক্ত বিল লেনদেন"
            }
            trx.type.equals("RELEASE_DEDUCTION", ignoreCase = true) -> {
                if (clean.isNotBlank()) "রিলিজ: $clean" else "রিলিজ লেনদেন"
            }
            trx.type.equals("BALANCE_RECONCILIATION", ignoreCase = true) -> {
                if (clean.isNotBlank()) "ব্যালেন্স সংশোধন: $clean" else "ব্যালেন্স সংশোধন লেনদেন"
            }
            else -> {
                // Remaining PAYMENT-type rows: the solver payout side of a release (normal,
                // dispute, or split). getReleaseMeta() now returns a non-empty labelBn for the
                // NORMAL case too, so a plain successful release is properly tagged "রিলিজ"
                // instead of falling through to the generic fallback.
                val releaseMeta = getReleaseMeta(trx)
                if (clean.isNotBlank()) "${releaseMeta.labelBn}: $clean" else "${releaseMeta.labelBn} লেনদেন"
            }
        }
    }
}
