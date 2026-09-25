package com.example.data.payment

import kotlinx.coroutines.delay

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ৩
 *
 * [PaymentGatewayProvider] এর demo/mock implementation।
 *
 * **কেন এটা বিদ্যমান কোড থেকে "move" করা হয়নি, standalone নতুন করে লেখা হয়েছে (রিপোর্টে বলা
 * নিয়ম অনুযায়ী ব্যাখ্যা):**
 * বর্তমানে demo payment gateway-র আসল simulation
 * (`com.example.ui.components.MerchantPaymentDialog.kt`) সম্পূর্ণভাবে একটা Jetpack Compose
 * UI Dialog এর ভেতরে — Compose animation/state (`LaunchedEffect`, `Animatable`,
 * `delay(1500)` দিয়ে processing→success transition, callback দিয়ে trxId ফেরত পাঠানো)
 * এর সাথে শক্তভাবে জড়িয়ে আছে। এই ধাপের অলঙ্ঘনীয় নিয়ম অনুযায়ী UI ফাইলে হাত দেওয়া যাবে না,
 * তাই সেই লজিক এখানে সরিয়ে (move) আনা সম্ভব হয়নি। এর বদলে, একই *behavior* (কোনো real
 * network call না করে ~১.৫ সেকেন্ড delay এর পর সবসময় success, আর
 * `com.example.data.repository.SomadhanRepository.recordGatewayPayment()` এ ইতিমধ্যে
 * ব্যবহৃত "TRX" + timestamp + random suffix ফরম্যাট মিলিয়ে trxId তৈরি) মিরর করে একটা
 * standalone নতুন implementation লেখা হলো — এখনো কোথাও wire/call করা হয়নি।
 * পরের কোনো ধাপে UI/ViewModel migrate হওয়ার সময় `MerchantPaymentDialog` কে এই provider
 * ব্যবহার করানো যাবে (তখন UI ফাইল বদলানোর অনুমতি থাকবে)।
 */
object DemoPaymentGatewayProvider : PaymentGatewayProvider {

    private const val SIMULATED_PROCESSING_DELAY_MS = 1500L

    override suspend fun initiateDeposit(amount: Double, purpose: String): Result<GatewayCheckoutInfo> {
        delay(SIMULATED_PROCESSING_DELAY_MS) // বর্তমান MerchantPaymentDialog এর মতোই ~১.৫ সেকেন্ড simulated processing

        val trxId = "TRX" + System.currentTimeMillis().toString().takeLast(8).uppercase()
        return Result.success(
            GatewayCheckoutInfo(
                gatewayTrxId = trxId,
                gatewayName = "DEMO",
                amount = amount,
                checkoutUrl = null,
                message = "ডেমো পেমেন্ট সম্পন্ন হয়েছে (কোনো আসল টাকা কাটা হয়নি)।"
            )
        )
    }

    override suspend fun verifyPayment(gatewayTrxId: String): Result<Boolean> {
        // Demo মোডে initiateDeposit() ইতিমধ্যে সফল ধরে নেয় (বর্তমান MerchantPaymentDialog এও কোনো
        // ব্যর্থতার পথ নেই), তাই এখানেও সবসময় true — real gateway implementation এ এটা আসল
        // ট্রানজ্যাকশন স্ট্যাটাস query করবে।
        return Result.success(true)
    }
}
