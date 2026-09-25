package com.example.data.auth

import com.example.util.OtpSendResult
import com.example.util.OtpService
import com.example.util.OtpVerifyResult

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ৩
 *
 * [OtpProvider] এর demo/mock implementation।
 *
 * **কেন এটা `com.example.util.OtpService` এর ভেতরের আসল লজিক "move" না করে delegate/wrap
 * করছে (গুরুত্বপূর্ণ নোট, রিপোর্টেও লেখা আছে):**
 * বর্তমান demo OTP-র আসল implementation (৬-ডিজিট secure random OTP generate, ৫-মিনিট expiry,
 * ৬০-সেকেন্ড rate-limit, সর্বোচ্চ ৫-বার ভুল চেষ্টা, local heads-up notification, BD SMS Gateway/
 * SendGrid fallback সহ, universal testing code "123456") `OtpService.kt` তে আছে, আর তার সাথে
 * থাকা `OtpSendResult` / `OtpVerifyResult` sealed class দুটো `SomadhanViewModel.kt` আর
 * `SomadhanApp.kt` থেকে package-qualified import (`com.example.util.OtpSendResult` ইত্যাদি)
 * দিয়ে সরাসরি ব্যবহৃত হচ্ছে। এই ধাপের অলঙ্ঘনীয় নিয়ম অনুযায়ী repository/viewmodel/UI কোনো
 * ফাইলে হাত দেওয়া যাবে না — তাই `OtpService.kt` থেকে ওই টাইপগুলো সরিয়ে আনলে সেই import
 * ভেঙে যেত। তাই এখানে লজিক copy/duplicate না করে, `DemoOtpProvider` পুরোপুরি বিদ্যমান
 * (অপরিবর্তিত) `OtpService` কে delegate/wrap করছে — behavior হুবহু একই থাকছে, ডুপ্লিকেট
 * কোডও নেই। ভবিষ্যতে যখন `SomadhanViewModel.kt` migrate হবে (পরের ধাপগুলোতে), তখন চাইলে
 * `OtpService` এর আসল লজিক সরাসরি এখানে move করে এই adapter সরিয়ে ফেলা যাবে।
 */
object DemoOtpProvider : OtpProvider {

    override suspend fun sendOtp(phone: String, purpose: OtpPurpose): Result<Unit> {
        return when (val result = OtpService.requestOtp(phone, purpose.name.lowercase())) {
            is OtpSendResult.Success -> Result.success(Unit)
            is OtpSendResult.RateLimited -> Result.failure(Exception(result.message))
            is OtpSendResult.Error -> Result.failure(Exception(result.message))
        }
    }

    override suspend fun verifyOtp(phone: String, code: String, purpose: OtpPurpose): Result<Boolean> {
        // OtpService.verifyOtp সাসপেন্ড ফাংশন না (সিঙ্ক্রোনাস, in-memory চেক), তাই সরাসরি call করা নিরাপদ।
        return when (val result = OtpService.verifyOtp(phone, code)) {
            is OtpVerifyResult.Success -> Result.success(true)
            is OtpVerifyResult.InvalidCode -> Result.success(false)
            is OtpVerifyResult.Expired -> Result.failure(Exception(result.message))
            is OtpVerifyResult.TooManyAttempts -> Result.failure(Exception(result.message))
            is OtpVerifyResult.NotFound -> Result.failure(Exception(result.message))
        }
    }
}
