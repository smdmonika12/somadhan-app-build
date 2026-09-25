package com.example.data.remote

import com.example.data.entity.UserEntity
import com.example.data.remote.dto.UserDto

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ১৪ (Repository Migration: Auth/Profile)
 *
 * `UserDto` (Supabase `public.users` টেবিলের shape) আর `UserEntity` (লোকাল Room ক্যাশ)-এর
 * মধ্যে mapping। দুটো জায়গায় স্কিমা আলাদা বলে সরাসরি copy করা যায় না, বিশেষত:
 *
 * - **kyc_status casing/value mismatch**: Supabase check constraint
 *   `NONE | PENDING | APPROVED | REJECTED` (uppercase, "APPROVED") — কিন্তু লোকাল
 *   `UserEntity.kycStatus` সবসময় lowercase `"none"/"pending"/"verified"/"rejected"` ব্যবহার করে
 *   (পুরনো Firebase কোড জুড়ে এই lowercase ভ্যালুগুলোর সাথে string-compare হয়)। এই মিসম্যাচ
 *   ধাপ ২-এ DTO বানানোর সময় লক্ষ্য করা হয়নি (তখন শুধু column count/nullability মেলানো হয়েছিল,
 *   value-level semantics না) — এই ধাপে (১৪) প্রথমবার ধরা পড়ল যখন real read/write path লেখা
 *   হচ্ছে। তাই এখানে explicit map করা হলো, দুই দিকেই।
 * - **password**: `UserEntity.password`-এর Supabase-সমতুল্য কোনো column নেই (users টেবিলে কোনো
 *   password column-ই নেই — password পুরোপুরি Supabase Auth (`auth.users`) নিজে সামলায়)। DTO →
 *   Entity map করার সময় password blank রাখা হয় (caller প্রয়োজনে পুরনো cached local password রেখে
 *   দিতে পারে, [com.example.data.repository.SomadhanRepository.cacheUserLocally] যেভাবে করে)।
 * - **timestamp fields** (`created_at`/`updated_at`/`kyc_submission_date`/
 *   `last_reputation_decay_check_at`): DTO-তে raw ISO-8601 `String?` (ধাপ ২-এর সিদ্ধান্ত অনুযায়ী,
 *   kotlinx-datetime এখনো যোগ করা হয়নি)। `UserEntity`-তে এগুলো `Long` (epoch millis)। এই ধাপে
 *   এই চারটা ফিল্ড mapping-এ **ইচ্ছাকৃতভাবে parse করা হয়নি** — DTO থেকে আসা মান ব্যবহার না করে
 *   local device সময় (`System.currentTimeMillis()`) বসানো হয়েছে (ঠিক যেমন আগে থেকেই
 *   `registerUser`/`updateUser` ইত্যাদি করে) — সামান্য imprecise (device clock বনাম server clock)
 *   কিন্তু app এর বাকি অংশ এই imprecision নিয়েই আগে থেকে চলছে, নতুন কোনো ঝুঁকি না। ভবিষ্যতে
 *   kotlinx-datetime যোগ হলে এখানে proper parsing বসানো যাবে।
 * - **kycDocumentImage** (লোকাল-only combined legacy field, DTO-তে নেই): null রাখা হয়েছে।
 * - **linkedAccountId**: DTO-তে আছে (schema সমর্থন করে), কিন্তু ধাপ ১৪-এ `switchRole()` migrate
 *   হয়নি (নিচে repository-এর comment দেখুন) — তাই এই মান সরাসরি pass-through করা হলো, নতুন কোনো
 *   লজিক যোগ করা হয়নি।
 */

/** Supabase `kyc_status` (uppercase) → local `UserEntity.kycStatus` (lowercase) */
fun mapKycStatusFromSupabase(supabaseStatus: String): String = when (supabaseStatus.uppercase()) {
    "APPROVED" -> "verified"
    "PENDING" -> "pending"
    "REJECTED" -> "rejected"
    else -> "none"
}

/** Local `UserEntity.kycStatus` (lowercase) → Supabase `kyc_status` (uppercase) */
fun mapKycStatusToSupabase(localStatus: String): String = when (localStatus.lowercase()) {
    "verified" -> "APPROVED"
    "pending" -> "PENDING"
    "rejected" -> "REJECTED"
    else -> "NONE"
}

/**
 * `UserDto` (Supabase থেকে আসা) কে `UserEntity`-তে map করে। [existingPassword] দিলে সেটা
 * local password হিসেবে রাখা হয় (Supabase কখনো password পাঠায় না, তাই caller-কেই আগের local
 * hash সরবরাহ করতে হয় — নাহলে blank থাকবে, যেমন নতুন cross-device login-এর প্রথমবার)।
 */
fun UserDto.toUserEntity(existingPassword: String = ""): UserEntity {
    val now = System.currentTimeMillis()
    return UserEntity(
        id = id,
        name = name,
        // [display_uid ফিক্স] Supabase-এ এখন আসল `display_uid` কলাম আছে (server-generated,
        // ৬-সংখ্যা থেকে শুরু, প্রয়োজনে ৭/৮... সংখ্যায় স্বয়ংক্রিয়ভাবে বাড়ে) — আগে এখানে সবসময়
        // "" বসানো হতো কারণ কোনো কলামই ছিল না, যার ফলে root account-এর displayUid কখনো সেট
        // হতো না আর UI বড় raw UUID দেখাতো। এখন সরাসরি DTO থেকে pass-through করা হচ্ছে।
        displayUid = displayUid?.toString() ?: "",
        phone = phone ?: "",
        email = email ?: "",
        password = existingPassword,
        role = role,
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        address = address,
        profileImageUri = profileImageUri,
        isKycVerified = isKycVerified,
        kycStatus = mapKycStatusFromSupabase(kycStatus),
        kycDocumentType = kycDocumentType,
        kycDocumentNumber = kycDocumentNumber,
        kycDocumentImage = null,
        kycDocumentFrontImage = kycDocumentFrontImage,
        kycDocumentBackImage = kycDocumentBackImage,
        kycSelfieImage = kycSelfieImage,
        kycFirstName = kycFirstName,
        kycLastName = kycLastName,
        kycAddress = kycAddress,
        kycSubmissionDate = if (kycSubmissionDate != null) now else null,
        kycRejectReason = kycRejectReason,
        hasCompletedSolverSetup = hasCompletedSolverSetup,
        solverCategories = solverCategories,
        favoriteSolverIds = favoriteSolverIds,
        balance = balance,
        isVerifiedBadge = isVerifiedBadge,
        isBanned = isBanned,
        isRestricted = isRestricted,
        hasUserRole = hasUserRole,
        hasSolverRole = hasSolverRole,
        linkedAccountId = linkedAccountId,
        reputationScore = reputationScore,
        freeJobsUsedThisMonth = freeJobsUsedThisMonth,
        freeJobsMonthKey = freeJobsMonthKey,
        cycleJobCount = cycleJobCount,
        cycleMissCount = cycleMissCount,
        instantJobNotificationsEnabled = instantJobNotificationsEnabled,
        lastReputationDecayCheckAt = now,
        updatedAt = now,
        createdAt = now,
        // ধাপ ১৪.৫ role-scoped কলাম — সরাসরি pass-through (কোনো mapping/derivation লাগে না,
        // দুই স্কিমাতেই একই shape)। এখনো কোনো caller এই ফিল্ডগুলো read করে না (Kotlin-wiring
        // বাকি — switchRole()/repository redesign পরের সাব-ধাপে)।
        balanceUser = balanceUser,
        balanceSolver = balanceSolver,
        reputationScoreUser = reputationScoreUser,
        reputationScoreSolver = reputationScoreSolver,
        isBannedUser = isBannedUser,
        isBannedSolver = isBannedSolver,
        isRestrictedUser = isRestrictedUser,
        isRestrictedSolver = isRestrictedSolver,
        // ধাপ ৮ (বাগ D৩) — সরাসরি pass-through, দুই স্কিমাতেই একই shape।
        verifiedBadgeUser = verifiedBadgeUser,
        verifiedBadgeSolver = verifiedBadgeSolver
    )
}
