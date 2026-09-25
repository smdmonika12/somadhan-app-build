package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ২ (Data Models)
 *
 * Supabase টেবিল `public.users` এর জন্য DTO। column নাম/টাইপ সরাসরি Supabase MCP দিয়ে
 * schema দেখে (snake_case) মেলানো হয়েছে — অনুমান করা হয়নি।
 *
 * নোট: `timestamp with time zone` কলামগুলো এখানে `String` (PostgREST এর ডিফল্ট ISO-8601
 * representation) হিসেবে রাখা হয়েছে; kotlinx-datetime এখনো dependency হিসেবে যোগ করা হয়নি
 * (এই ধাপের স্কোপে নেই), তাই পরের ধাপে repository/mapper লেয়ারে প্রয়োজনে parse করা হবে।
 */
@Serializable
data class UserDto(
    val id: String, // uuid, PK, auth.users.id এর সাথে FK
    @SerialName("display_uid") val displayUid: Long? = null, // ৬-সংখ্যা (প্রয়োজনে বেশি) থেকে শুরু হওয়া ইউনিক, র‍্যান্ডম, সার্ভার-জেনারেটেড ইউজার-facing UID; ক্লায়েন্ট কখনো এটা লেখে না
    val name: String = "",
    val phone: String? = null, // nullable, unique
    val email: String? = null, // nullable, unique
    val role: String = "USER", // check: USER | SOLVER | ADMIN
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String = "",
    @SerialName("profile_image_uri") val profileImageUri: String? = null,
    @SerialName("is_kyc_verified") val isKycVerified: Boolean = false,
    @SerialName("kyc_status") val kycStatus: String = "NONE", // check: NONE | PENDING | APPROVED | REJECTED
    @SerialName("kyc_document_type") val kycDocumentType: String? = null,
    @SerialName("kyc_document_number") val kycDocumentNumber: String? = null,
    @SerialName("kyc_document_front_image") val kycDocumentFrontImage: String? = null,
    @SerialName("kyc_document_back_image") val kycDocumentBackImage: String? = null,
    @SerialName("kyc_selfie_image") val kycSelfieImage: String? = null,
    @SerialName("kyc_first_name") val kycFirstName: String? = null,
    @SerialName("kyc_last_name") val kycLastName: String? = null,
    @SerialName("kyc_address") val kycAddress: String? = null,
    @SerialName("kyc_submission_date") val kycSubmissionDate: String? = null, // timestamptz
    @SerialName("kyc_reject_reason") val kycRejectReason: String? = null,
    @SerialName("has_completed_solver_setup") val hasCompletedSolverSetup: Boolean = false,
    @SerialName("solver_categories") val solverCategories: String = "", // comma-separated category IDs
    @SerialName("favorite_solver_ids") val favoriteSolverIds: String = "", // comma-separated solver user IDs
    val balance: Double = 0.0, // check: balance >= 0
    @SerialName("is_verified_badge") val isVerifiedBadge: Boolean = true,
    @SerialName("is_banned") val isBanned: Boolean = false,
    @SerialName("is_restricted") val isRestricted: Boolean = false,
    @SerialName("has_user_role") val hasUserRole: Boolean = true,
    @SerialName("has_solver_role") val hasSolverRole: Boolean = false,
    @SerialName("linked_account_id") val linkedAccountId: String? = null, // uuid, self-FK
    @SerialName("reputation_score") val reputationScore: Double = 50.0,
    @SerialName("free_jobs_used_this_month") val freeJobsUsedThisMonth: Int = 0,
    @SerialName("free_jobs_month_key") val freeJobsMonthKey: String = "", // "yyyy-MM"
    @SerialName("cycle_job_count") val cycleJobCount: Int = 0,
    @SerialName("cycle_miss_count") val cycleMissCount: Int = 0,
    @SerialName("instant_job_notifications_enabled") val instantJobNotificationsEnabled: Boolean = true,
    @SerialName("last_reputation_decay_check_at") val lastReputationDecayCheckAt: String? = null, // timestamptz
    @SerialName("created_at") val createdAt: String? = null, // timestamptz
    @SerialName("updated_at") val updatedAt: String? = null, // timestamptz
    // ধাপ ১৪.৫ (Role-Profile Redesign, migration role_profile_14_5a...14_5d এ schema-তে যোগ
    // হয়েছিল, Supabase MCP দিয়ে verify করা) — dual-row এর বদলে single-row-এ role-scoped ডেটা।
    // পুরনো shared কলাম (balance/is_banned/is_restricted/reputation_score) dual-write cutover
    // না হওয়া পর্যন্ত অক্ষত রাখা হয়েছে, drop করা হয়নি (উপরে দেখুন)।
    @SerialName("balance_user") val balanceUser: Double = 0.0,
    @SerialName("balance_solver") val balanceSolver: Double = 0.0,
    @SerialName("reputation_score_user") val reputationScoreUser: Double = 50.0,
    @SerialName("reputation_score_solver") val reputationScoreSolver: Double = 50.0,
    @SerialName("is_banned_user") val isBannedUser: Boolean = false,
    @SerialName("is_banned_solver") val isBannedSolver: Boolean = false,
    @SerialName("is_restricted_user") val isRestrictedUser: Boolean = false,
    @SerialName("is_restricted_solver") val isRestrictedSolver: Boolean = false,
    // ধাপ ৮ (MONEY_FLOW_AND_ADMIN_BUGS, বাগ D৩) — verified badge role-scoped কলাম, শেয়ার্ড
    // `is_verified_badge`-এর মতোই ডিফল্ট true (২০২৬-০৯ মাইগ্রেশনে ব্যাকফিল করা হয়েছে)।
    @SerialName("verified_badge_user") val verifiedBadgeUser: Boolean = true,
    @SerialName("verified_badge_solver") val verifiedBadgeSolver: Boolean = true
)
