package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index("phone"),
        Index("email"),
        Index("role"),
        Index("linkedAccountId"),
        Index("createdAt")
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val displayUid: String = "",
    val phone: String,
    val email: String,
    val password: String,
    val role: String, // "USER", "SOLVER", "ADMIN"
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val profileImageUri: String? = null,
    val isKycVerified: Boolean = false,
    val kycStatus: String = "none", // "none", "pending", "verified", "rejected"
    val kycDocumentType: String? = null,
    val kycDocumentNumber: String? = null,
    val kycDocumentImage: String? = null,
    val kycDocumentFrontImage: String? = null,
    val kycDocumentBackImage: String? = null,
    val kycSelfieImage: String? = null,
    val kycFirstName: String? = null,
    val kycLastName: String? = null,
    val kycAddress: String? = null,
    val kycSubmissionDate: Long? = null,
    val kycRejectReason: String? = null,
    val hasCompletedSolverSetup: Boolean = false,
    val solverCategories: String = "", // comma-separated category IDs e.g. "CAT_ELEC,CAT_PLUMB"
    val favoriteSolverIds: String = "", // comma-separated solver user IDs
    val balance: Double = 0.0,
    val isVerifiedBadge: Boolean = true,
    val isBanned: Boolean = false,
    val isRestricted: Boolean = false,
    val hasUserRole: Boolean = true,
    val hasSolverRole: Boolean = false,
    val linkedAccountId: String? = null,
    val reputationScore: Double = 50.0,
    // ধাপ ১৪.৫ (Role-Profile Redesign) — role-scoped কলাম, Supabase `users` টেবিলে (migration
    // `role_profile_14_5a...14_5d`) ইতিমধ্যেই আছে। এখানে শুধু local Room cache pass-through
    // এর জন্য যোগ করা হলো (UserMappers.kt দেখুন) — এখনো কোনো UI/repository ফাংশন এই ফিল্ডগুলো
    // read/write করে না (Kotlin-wiring এর বাকি সাব-ধাপ, যেমন switchRole() redesign, এখনো বাকি)।
    // পুরনো shared কলাম (balance/isBanned/isRestricted/reputationScore) dual-write cutover না
    // হওয়া পর্যন্ত অক্ষত থাকবে, স্পর্শ করা হয়নি।
    val balanceUser: Double = 0.0,
    val balanceSolver: Double = 0.0,
    val reputationScoreUser: Double = 50.0,
    val reputationScoreSolver: Double = 50.0,
    val isBannedUser: Boolean = false,
    val isBannedSolver: Boolean = false,
    val isRestrictedUser: Boolean = false,
    val isRestrictedSolver: Boolean = false,
    // ধাপ ৮ (MONEY_FLOW_AND_ADMIN_BUGS, বাগ D৩) — verified badge role-scoped কলাম। ডিফল্ট
    // true, শেয়ার্ড `isVerifiedBadge`-এর সাথে সামঞ্জস্যপূর্ণ (Supabase migration দিয়ে
    // বিদ্যমান রো ব্যাকফিল করা হয়েছে)।
    val verifiedBadgeUser: Boolean = true,
    val verifiedBadgeSolver: Boolean = true,
    // ধাপ ১৪.৫ (গ) — Legacy dual-row device-data merge গার্ড। Supabase `users` টেবিলে এর কোনো
    // সমতুল্য কলাম নেই (এটা সম্পূর্ণ local/device-only flag, ঠিক `password`/`displayUid`-এর
    // মতো) — তাই UserMappers.toUserEntity() এটা সেট করে না (ডিফল্ট 0L রেখে দেয়), caller-কেই
    // (SomadhanRepository.cacheUserLocally) বিদ্যমান local row থেকে এই মান preserve করতে হয়,
    // password preserve করার মতোই। 0L মানে "এই ডিভাইসে এখনো legacy dual-row merge হয়নি";
    // non-zero মানে merge সম্পন্নের timestamp (দেখুন SomadhanRepository.mergeLegacyDualRoleDataFromCloud)।
    val legacyDualRoleMergeDoneAt: Long = 0L,
    // [ROLE_UID ফিক্স - ধাপ ৩] পুরনো dual-row (`SOLVER_xxx`/`USER_xxx` id-ওয়ালা linked) row
    // root row-এ merge হয়ে গেছে কিনা তার মার্কার। এই row-গুলো **মোছা হয় না** (rule #৩, #৫ —
    // rollback সম্ভব রাখতে), শুধু true করে চিহ্নিত করা হয় যাতে migration আর দ্বিতীয়বার একই
    // row প্রসেস না করে। সম্পূর্ণ local/device-only ফ্ল্যাগ — Supabase `users` টেবিলে এর কোনো
    // সমতুল্য কলাম নেই (ঠিক `password`/`legacyDualRoleMergeDoneAt`-এর মতো), তাই
    // UserMappers.toUserEntity() এটা সেট করে না; caller (SomadhanRepository.cacheUserLocally)
    // বিদ্যমান local row থেকে মান preserve করে। বিদ্যমান `legacyDualRoleMergeDoneAt`
    // ইচ্ছাকৃতভাবে reuse করা হয়নি — সেটার অর্থ আলাদা (cloud role-scoped মান দিয়ে local
    // dual-row ওভাররাইট, ধাপ ১৪.৫গ), দেখুন ROLE_UID_FIX_DESIGN.md সেকশন ৪।
    val localDualRowArchived: Boolean = false,
    val freeJobsUsedThisMonth: Int = 0,
    val freeJobsMonthKey: String = "", // format: "yyyy-MM", e.g. "2026-08"
    val cycleJobCount: Int = 0,        // বর্তমান ১০-কাজের সাইকেলে এখন পর্যন্ত সম্পন্ন কাজের সংখ্যা
    val cycleMissCount: Int = 0,       // ঐ সাইকেলে কতটাতে extra bill নিতে পারেনি
    val instantJobNotificationsEnabled: Boolean = true,
    val lastReputationDecayCheckAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

// [ব্যালেন্স ফিক্স — ধাপ ১] `balance` (legacy shared কলাম) role-switch/sync টাইমিং-এর ওপর
// নির্ভর করে stale হয়ে যেতে পারে (দেখুন MONEY_FLOW_AND_ADMIN_BUGS_MASTER_PROMPT.md, বাগ A)।
// UI/ViewModel-এর যেকোনো জায়গায় "এই মুহূর্তে ইউজার যে role-এ আছে তার ব্যালেন্স" দরকার হলে
// সবসময় এই extension দিয়ে পড়তে হবে — সরাসরি `user.balance` পড়া যাবে না। এটা সবসময়
// role-scoped `balanceUser`/`balanceSolver`-এর মধ্যে থেকে সঠিকটা বেছে নেয়, তাই কোনো stale
// mirror-এর ওপর নির্ভর করে না।
val UserEntity.activeRoleBalance: Double
    get() = if (role == "SOLVER") balanceSolver else balanceUser

// [MONEY_FLOW_AND_ADMIN_BUGS ধাপ ৮ — বাগ D/D২/D৩] Ban/Restrict/Verified badge এখন
// role-scoped কলামে রাখা হয় (`is*User`/`is*Solver`)। শেয়ার্ড `isBanned`/`isRestricted`/
// `isVerifiedBadge` পড়া শুধু legacy fallback-এর জন্য থাকবে (নতুন কোনো enforcement সাইট
// আর সরাসরি shared ফিল্ড পড়বে না) — "এই মুহূর্তে যে role-এ active" তার জন্য এই তিনটা
// extension ব্যবহার করা উচিত, `activeRoleBalance`-এর একই প্যাটার্নে।
val UserEntity.activeRoleBanned: Boolean
    get() = if (role == "SOLVER") isBannedSolver else isBannedUser

val UserEntity.activeRoleRestricted: Boolean
    get() = if (role == "SOLVER") isRestrictedSolver else isRestrictedUser

val UserEntity.activeRoleVerifiedBadge: Boolean
    get() = if (role == "SOLVER") verifiedBadgeSolver else verifiedBadgeUser
