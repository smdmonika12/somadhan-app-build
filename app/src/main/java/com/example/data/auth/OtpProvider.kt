package com.example.data.auth

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ৩
 *
 * OTP পাঠানো/যাচাই করার জন্য abstraction। বর্তমানে শুধু [DemoOtpProvider] implementation আছে
 * (demo/mock, real SMS যায় না); ভবিষ্যতে real Supabase phone-OTP ভিত্তিক implementation
 * (SupabaseAuthManager ব্যবহার করে) বসালে বাকি কোড (repository/viewmodel) না বদলিয়ে শুধু এই
 * ইন্টারফেসের নতুন implementation সুইচ করলেই "live" হয়ে যাবে।
 */
interface OtpProvider {
    suspend fun sendOtp(phone: String, purpose: OtpPurpose): Result<Unit>
    suspend fun verifyOtp(phone: String, code: String, purpose: OtpPurpose): Result<Boolean>
}

enum class OtpPurpose { LOGIN, REGISTER, PASSWORD_RESET }
