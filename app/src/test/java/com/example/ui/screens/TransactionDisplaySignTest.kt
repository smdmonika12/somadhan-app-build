package com.example.ui.screens

import com.example.data.entity.TransactionEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [Step 15.2 — CI_TEST_SUITE_MASTER_PROMPT.md] TransactionHistoryScreen.kt-এর
 * transactionDisplaySign() pure function-এর টেস্ট।
 *
 * আসল বাগ (রিপোর্ট-করা): ADMIN_ADJUSTMENT টাইপের transaction-এ isEarning/isPositive
 * classification সবসময় "−" দেখাতো, admin add বা deduct যেটাই করুক না কেন — কারণ পুরনো
 * `isEarning || isUserRefund || isUserDeposit` লজিক শুধু তিনটা নির্দিষ্ট শেপ চিনত এবং বাকি
 * সব `trx.type`-এর জন্য নীরবে `false`-এ default হয়ে যেত।
 *
 * ফিক্স verify করে যে transactionDisplaySign() প্রতিটা transaction-এর net_amount-এর sign থেকে
 * সঠিকভাবে +/− ঠিক করে — টাইপ-নির্দিষ্ট enumeration না করেই — তাই প্রতিটা known টাইপের জন্যই
 * (এবং ভবিষ্যতে নতুন কোনো টাইপের জন্যও, যতক্ষণ net_amount সাইন করে লেখা হয়) সঠিক ফলাফল দেয়।
 *
 * প্রতিটা টেস্ট কেসের net_amount sign migrations-এর আসল `insert into public.transactions` থেকে
 * নেওয়া (case-insensitive grep দিয়ে ক্রস-চেক করা, Step 15.1/15.2-এর session notes-এ বিস্তারিত):
 * - পজিটিভ net_amount: PAYMENT/DISPUTE_SPLIT (solver earning, gross−commission), REFUND/
 *   DISPUTE_REFUND/SPLIT_REFUND/DISPUTE_SPLIT_REFUND (user-side refund), WALLET_DEPOSIT (deposit),
 *   WITHDRAWAL_REFUND (বাতিল হওয়া withdrawal-এর টাকা ফেরত), ADMIN_ADJUSTMENT (p_is_addition=true)
 * - নেগেটিভ net_amount: BID_ACCEPT_DEDUCTION, EXTRA_CHARGE_DEDUCTION, WITHDRAWAL_DEDUCTION,
 *   ADMIN_ADJUSTMENT (p_is_addition=false), DUPLICATE_CORRECTION (যখন মূল duplicate positive ছিল)
 * - BALANCE_RECONCILIATION আর CANCELLED_EXTRA উভয় দিকেই যেতে পারে (data-নির্ভর) — দুটো দিকই
 *   আলাদা টেস্ট কেসে কভার করা হলো।
 */
class TransactionDisplaySignTest {

    private val viewerId = "USER_1"

    private fun trx(
        type: String,
        netAmount: Double,
        userId: String = viewerId,
        solverId: String = "",
        grossAmount: Double = kotlin.math.abs(netAmount)
    ) = TransactionEntity(
        id = "TRX_TEST_$type",
        problemId = "",
        problemTitle = "",
        userId = userId,
        solverId = solverId,
        grossAmount = grossAmount,
        commissionPercent = 0.0,
        commissionAmount = 0.0,
        netAmount = netAmount,
        type = type
    )

    // ------------------------------------------------------------------
    // মূল রিপোর্ট-করা বাগ: ADMIN_ADJUSTMENT — দুই দিকই
    // ------------------------------------------------------------------

    @Test
    fun `ADMIN_ADJUSTMENT addition shows positive`() {
        val t = trx(type = "ADMIN_ADJUSTMENT", netAmount = 500.0)
        assertTrue(
            "admin balance addition আগে ভুলভাবে '−' দেখাতো, এখন '+' হওয়ার কথা",
            transactionDisplaySign(t, viewerId)
        )
    }

    @Test
    fun `ADMIN_ADJUSTMENT deduction shows negative`() {
        val t = trx(type = "ADMIN_ADJUSTMENT", netAmount = -500.0)
        assertFalse(
            "admin balance deduction '−' দেখানোর কথা",
            transactionDisplaySign(t, viewerId)
        )
    }

    // ------------------------------------------------------------------
    // বাকি সব known transaction type — happy path (প্রতিটার জন্য একটা করে কেস, master
    // prompt rule অনুযায়ী)
    // ------------------------------------------------------------------

    @Test
    fun `PAYMENT solver earning shows positive`() {
        val t = trx(type = "PAYMENT", netAmount = 900.0, userId = "OTHER_USER", solverId = viewerId)
        assertTrue(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `DISPUTE_SPLIT solver share shows positive`() {
        val t = trx(type = "DISPUTE_SPLIT", netAmount = 450.0, userId = "OTHER_USER", solverId = viewerId)
        assertTrue(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `REFUND to user shows positive`() {
        val t = trx(type = "REFUND", netAmount = 1000.0)
        assertTrue(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `DISPUTE_REFUND shows positive`() {
        val t = trx(type = "DISPUTE_REFUND", netAmount = 1000.0)
        assertTrue(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `SPLIT_REFUND shows positive`() {
        val t = trx(type = "SPLIT_REFUND", netAmount = 500.0)
        assertTrue(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `DISPUTE_SPLIT_REFUND shows positive`() {
        val t = trx(type = "DISPUTE_SPLIT_REFUND", netAmount = 500.0)
        assertTrue(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `WALLET_DEPOSIT shows positive`() {
        val t = trx(type = "WALLET_DEPOSIT", netAmount = 2000.0)
        assertTrue(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `BID_ACCEPT_DEDUCTION shows negative`() {
        val t = trx(type = "BID_ACCEPT_DEDUCTION", netAmount = -300.0)
        assertFalse(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `EXTRA_CHARGE_DEDUCTION shows negative`() {
        val t = trx(type = "EXTRA_CHARGE_DEDUCTION", netAmount = -150.0)
        assertFalse(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `RELEASE_DEDUCTION shows negative`() {
        val t = trx(type = "RELEASE_DEDUCTION", netAmount = -900.0)
        assertFalse(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `WITHDRAWAL_DEDUCTION shows negative`() {
        val t = trx(type = "WITHDRAWAL_DEDUCTION", netAmount = -700.0)
        assertFalse(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `WITHDRAWAL_REFUND shows positive (আগে ভুলভাবে negative দেখাতো)`() {
        // পুরনো লজিকে isRefundTrx()-এ "WITHDRAWAL_REFUND" ধরা পড়ত না (শুধু "REFUND"/
        // "DISPUTE_REFUND"/"SPLIT_REFUND" চেক হতো) এবং solverId ফিল্ডও এই টাইপে সেট হয় না
        // (userId কলামেই solver-এর id বসে) — তাই isEarning/isUserRefund দুটোই false হতো,
        // এই ধরনের row-ও সাইলেন্টলি "−" দেখাতো যদিও এটা আসলে টাকা ফেরত (positive)।
        val t = trx(type = "WITHDRAWAL_REFUND", netAmount = 700.0, userId = viewerId, solverId = "")
        assertTrue(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `DUPLICATE_CORRECTION removing a positive duplicate shows negative`() {
        val t = trx(type = "DUPLICATE_CORRECTION", netAmount = -250.0)
        assertFalse(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `DUPLICATE_CORRECTION removing a negative duplicate shows positive`() {
        val t = trx(type = "DUPLICATE_CORRECTION", netAmount = 250.0)
        assertTrue(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `BALANCE_RECONCILIATION upward correction shows positive`() {
        val t = trx(type = "BALANCE_RECONCILIATION", netAmount = 120.0)
        assertTrue(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `BALANCE_RECONCILIATION downward correction shows negative`() {
        val t = trx(type = "BALANCE_RECONCILIATION", netAmount = -120.0)
        assertFalse(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `CANCELLED_EXTRA positive direction shows positive`() {
        val t = trx(type = "CANCELLED_EXTRA", netAmount = 80.0)
        assertTrue(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `CANCELLED_EXTRA negative direction shows negative`() {
        val t = trx(type = "CANCELLED_EXTRA", netAmount = -80.0)
        assertFalse(transactionDisplaySign(t, viewerId))
    }

    // ------------------------------------------------------------------
    // ভবিষ্যত-প্রুফিং: সম্পূর্ণ অজানা/নতুন কোনো টাইপও net_amount-এর sign অনুযায়ী সঠিক
    // ফলাফল দেয় (এটাই মূল রিগ্রেশন-প্রতিরোধ — পুরনো লজিক নতুন টাইপ চিনত না)
    // ------------------------------------------------------------------

    @Test
    fun `completely unknown future type still classified correctly by sign`() {
        val positive = trx(type = "SOME_BRAND_NEW_TYPE_2027", netAmount = 42.0)
        val negative = trx(type = "SOME_BRAND_NEW_TYPE_2027", netAmount = -42.0)
        assertTrue(transactionDisplaySign(positive, viewerId))
        assertFalse(transactionDisplaySign(negative, viewerId))
    }

    // ------------------------------------------------------------------
    // Edge cases
    // ------------------------------------------------------------------

    @Test
    fun `zero net amount is treated as positive (neutral default)`() {
        val t = trx(type = "ADMIN_ADJUSTMENT", netAmount = 0.0)
        assertTrue(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `viewer not part of the transaction returns false regardless of sign`() {
        val t = trx(type = "PAYMENT", netAmount = 900.0, userId = "OTHER_USER", solverId = "SOME_OTHER_SOLVER")
        assertFalse(transactionDisplaySign(t, viewerId))
    }

    @Test
    fun `blank viewerId returns false`() {
        val t = trx(type = "ADMIN_ADJUSTMENT", netAmount = 500.0)
        assertFalse(transactionDisplaySign(t, ""))
    }
}
