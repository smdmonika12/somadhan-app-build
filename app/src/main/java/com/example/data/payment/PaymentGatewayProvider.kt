package com.example.data.payment

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ৩
 *
 * Payment gateway (bKash/Nagad/Rocket ইত্যাদি) abstraction। বর্তমানে শুধু
 * [DemoPaymentGatewayProvider] আছে (demo/mock, real gateway তে টাকা যায় না)। ভবিষ্যতে real
 * gateway integrate করার সময় বাকি কোড না বদলিয়ে শুধু এই ইন্টারফেসের নতুন implementation
 * বসালেই হবে।
 *
 * নোট: এই একই নামের (`PaymentGatewayProvider`) একটা **আলাদা** `enum class` ইতিমধ্যে
 * `com.example.ui.components.MerchantPaymentDialog.kt` তে আছে (bKash/Nagad/Rocket UI
 * সিলেকশনের জন্য) — সেটা ভিন্ন প্যাকেজে, তাই compile-এ কোনো সংঘর্ষ নেই, কিন্তু ভবিষ্যতে কোনো
 * ফাইলে দুটোই import করার দরকার হলে নাম-সংঘর্ষ হবে (alias import লাগবে)। এই ধাপে UI ফাইলে
 * হাত দেওয়া নিষেধ থাকায় সেই enum রিনেম/স্পর্শ করা হয়নি — শুধু এখানে নোট করে রাখা হলো।
 */
interface PaymentGatewayProvider {
    suspend fun initiateDeposit(amount: Double, purpose: String): Result<GatewayCheckoutInfo>
    suspend fun verifyPayment(gatewayTrxId: String): Result<Boolean>
}

/**
 * Gateway checkout শুরু করার পর ফিরে আসা তথ্য — UI/repository পরের ধাপে এটা ব্যবহার করে
 * `gateway_payments` টেবিলে রেকর্ড করবে ([com.example.data.remote.dto.GatewayPaymentDto] এর
 * সাথে মেলে এমন ফিল্ড রাখা হয়েছে)।
 */
data class GatewayCheckoutInfo(
    val gatewayTrxId: String,
    val gatewayName: String, // যেমন: "BKASH", "NAGAD", "ROCKET"
    val amount: Double,
    val checkoutUrl: String? = null, // demo তে null, real gateway তে redirect URL থাকতে পারে
    val message: String = ""
)
