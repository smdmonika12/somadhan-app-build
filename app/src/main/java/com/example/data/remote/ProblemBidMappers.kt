package com.example.data.remote

import com.example.data.entity.BidEntity
import com.example.data.entity.ProblemEntity
import com.example.data.remote.dto.BidDto
import com.example.data.remote.dto.ProblemDto

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ৮ (Repository Migration B: Problem/Bid)
 *
 * **এখানে ইচ্ছাকৃতভাবে শুধু Entity → Dto (write/insert দিকের) mapper আছে, Dto → Entity (read
 * দিকের) mapper নেই** (যেমন ধাপ ১৪-এ `UserMappers.kt`-এ দুই দিকই ছিল)। কারণ: এই ধাপে শুধু
 * write path (createProblem/placeBid ইত্যাদি) migrate করা হচ্ছে — read path
 * (getOpenProblems/getAllProblems/getProblemById/subscribeToProblem) এখনো local Room-ই থেকে
 * যাচ্ছে (Firebase realtime listener দিয়ে populate হয়), কারণ app-wide realtime/sync engine
 * migration (checklist item E, "ধাপ ১৪.৫") এখনো হয়নি — এখনই read path Supabase-এ সুইচ করলে
 * write Supabase-এ যাবে কিন্তু read local Firebase-sync-করা Room থেকে আসবে, যেটা একটা
 * split-brain data সমস্যা তৈরি করত (নতুন createProblem করা পোস্ট নিজের ডিভাইসেই না দেখানো, ইত্যাদি)।
 * তাই read migration ইচ্ছাকৃতভাবে এই ধাপে করা হয়নি — `MIGRATION_PROGRESS.md`-এ বিস্তারিত।
 */

/**
 * `createProblem()`-এ ব্যবহারের জন্য — শুধু creation-এর সময় যেসব ফিল্ড সেট করা হয় সেগুলোই এখানে
 * pass হয়, বাকি সব DTO-এর ডিফল্ট মান ব্যবহার করে (status="OPEN", bidsCount=0, ইত্যাদি — নতুন
 * problem-এর জন্য এগুলোই সঠিক প্রাথমিক মান)। timestamp ফিল্ড (createdAt) ইচ্ছাকৃতভাবে null রাখা
 * হয়েছে যাতে DB-এর নিজস্ব `default now()` ব্যবহার হয় (client clock-এর বদলে, বেশি নির্ভরযোগ্য)।
 *
 * [BUGFIX] এই mapper আগে শুধু plain `createProblem()`-এর জন্য লেখা হয়েছিল, কিন্তু পরে
 * `createInstantJob()`-ও এটাই reuse করে (একই `problem.toProblemDto()` কল) — অথচ `jobStatus`
 * ("BROADCASTING") আর `broadcastRadiusKm` এখানে pass করা হচ্ছিল না, ফলে DTO-এর ডিফল্ট null-ই
 * Supabase-এ চলে যেত। Local Room-এ ঠিক থাকলেও (সরাসরি insert), পরে realtime sync থেকে
 * job_status=null ফেরত এসে local সাইলেন্টলি ওভাররাইট করে দিত — জরুরি পোস্ট "আমার সমস্যা"-তে
 * দেখা যেত কিন্তু "জরুরি" ব্রডকাস্ট মেনুতে না (নিজের ডিভাইসেও, অন্য সলভারদের ডিভাইসেও, কারণ
 * `getBroadcastingJobsByCategory`/`getActiveInstantJobForSolver` উভয়েই `jobStatus = 'BROADCASTING'`
 * শর্ত রাখে যেটা null-এ ম্যাচ করে না)। এখন এই দুটো ফিল্ড যোগ করা হলো।
 */
fun ProblemEntity.toProblemDto(): ProblemDto = ProblemDto(
    id = id,
    userId = userId,
    userName = userName,
    userPhone = userPhone,
    userAddress = userAddress,
    title = title,
    description = description,
    categoryId = categoryId,
    categoryName = categoryName,
    isPhysical = isPhysical,
    latitude = latitude,
    longitude = longitude,
    minBudget = minBudget,
    maxBudget = maxBudget,
    urgency = urgency,
    status = status,
    isDirectContract = isDirectContract,
    isPublic = isPublic,
    isInstantJob = isInstantJob,
    jobStatus = jobStatus,
    broadcastRadiusKm = broadcastRadiusKm
)

/**
 * `placeBid()`-এ ব্যবহারের জন্য। `createdAt` একইভাবে null রাখা হয়েছে (DB default)। server-side
 * `handle_new_bid` trigger (ধাপ ৮-এ নতুন যোগ করা, নিচে দেখুন) insert-এর পর problems.bids_count
 * বাড়িয়ে দেয় — client-কে আলাদা করে problem row আপডেট করতে হয় না (RLS-এ সেই অনুমতিও নেই)।
 */
fun BidEntity.toBidDto(): BidDto = BidDto(
    id = id,
    problemId = problemId,
    solverId = solverId,
    solverName = solverName,
    solverPhone = solverPhone,
    solverRating = solverRating,
    amount = amount,
    message = message,
    estimatedTime = estimatedTime,
    status = status
)

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ২০ (Realtime Foundation A)
 *
 * `ProblemDto`/`BidDto` → `ProblemEntity`/`BidEntity` (read দিকের) mapper -- এই ফাইলে আগে
 * (ধাপ ৮) ইচ্ছাকৃতভাবে বাদ দেওয়া হয়েছিল (উপরের ক্লাস-লেভেল কমেন্ট দেখুন: তখন read path এখনো
 * Firestore-নির্ভর ছিল)। এখন `SupabaseRealtimeManager.kt` (ধাপ ২০)-এর জন্য এই দিকটা প্রথমবার
 * দরকার হলো।
 *
 * `UserMappers.kt`-এর `UserDto.toUserEntity()`-এর থেকে একটা গুরুত্বপূর্ণ পার্থক্য: ওখানে সব
 * timestamp ইচ্ছাকৃতভাবে parse না করে `System.currentTimeMillis()` বসানো হয়েছিল (কারণ user
 * timestamp গুলো UI-তে সরাসরি critical কিছু ড্রাইভ করে না)। কিন্তু `ProblemEntity`-এর timestamp
 * ফিল্ডগুলো (`jobStartedAt`, `arrivedAt`, `onWayAt`, `completedAt`, ইত্যাদি) সরাসরি
 * `calculateProgressStep()`-এ ব্যবহৃত হয় (job tracking UI progress bar) -- device-এর বর্তমান
 * সময় বসিয়ে দিলে ভুল progress দেখাবে। তাই এখানে [SupabaseTimestampUtil.parseTimestamptz]
 * দিয়ে আসল timestamp পার্স করা হয়েছে।
 */
fun ProblemDto.toProblemEntity(): ProblemEntity = ProblemEntity(
    id = id,
    userId = userId,
    userName = userName,
    userPhone = userPhone,
    userAddress = userAddress,
    title = title,
    description = description,
    categoryId = categoryId ?: "",
    categoryName = categoryName,
    isPhysical = isPhysical,
    latitude = latitude ?: 0.0,
    longitude = longitude ?: 0.0,
    minBudget = minBudget,
    maxBudget = maxBudget,
    urgency = urgency,
    status = status,
    bidsCount = bidsCount,
    acceptedBidId = acceptedBidId,
    acceptedSolverId = acceptedSolverId,
    acceptedSolverName = acceptedSolverName,
    acceptedAmount = acceptedAmount,
    hasReleaseRequest = hasReleaseRequest,
    releaseRequestExtraAmount = releaseRequestExtraAmount,
    releaseRequestNote = releaseRequestNote,
    releaseRequestedAt = SupabaseTimestampUtil.parseTimestamptz(releaseRequestedAt),
    // ProblemEntity.createdAt নন-নাল, DB default (now()) মিস হলে device সময় fallback হিসেবে
    createdAt = SupabaseTimestampUtil.parseTimestamptz(createdAt) ?: System.currentTimeMillis(),
    completedAt = SupabaseTimestampUtil.parseTimestamptz(completedAt),
    isDirectContract = isDirectContract,
    isPublic = isPublic,
    directContractStatus = directContractStatus,
    deadline = deadline,
    isDisputed = isDisputed,
    disputeReason = disputeReason,
    disputeInitiatorId = disputeInitiatorId,
    disputeInitiatorRole = disputeInitiatorRole,
    disputedAt = SupabaseTimestampUtil.parseTimestamptz(disputedAt),
    isAdminInvolvedInChat = isAdminInvolvedInChat,
    adminAssistanceRequestedBy = adminAssistanceRequestedBy,
    adminAssistanceRequestedAt = SupabaseTimestampUtil.parseTimestamptz(adminAssistanceRequestedAt),
    isUserDeleted = isUserDeleted,
    appliedCommissionRate = appliedCommissionRate,
    lastActivityAt = SupabaseTimestampUtil.parseTimestamptz(lastActivityAt),
    solverLastSeenAt = SupabaseTimestampUtil.parseTimestamptz(solverLastSeenAt),
    userLastSeenAt = SupabaseTimestampUtil.parseTimestamptz(userLastSeenAt),
    isInstantJob = isInstantJob,
    jobStatus = jobStatus,
    broadcastRadiusKm = broadcastRadiusKm,
    acceptedAt2 = SupabaseTimestampUtil.parseTimestamptz(acceptedAt2),
    onWayAt = SupabaseTimestampUtil.parseTimestamptz(onWayAt),
    arrivedAt = SupabaseTimestampUtil.parseTimestamptz(arrivedAt),
    jobStartedAt = SupabaseTimestampUtil.parseTimestamptz(jobStartedAt),
    solverLiveLat = solverLiveLat,
    solverLiveLng = solverLiveLng,
    solverLiveUpdatedAt = SupabaseTimestampUtil.parseTimestamptz(solverLiveUpdatedAt),
    userLiveLat = userLiveLat,
    userLiveLng = userLiveLng,
    pendingExtraAmount = pendingExtraAmount,
    pendingExtraAmountNote = pendingExtraAmountNote,
    pendingExtraAmountRequestedAt = SupabaseTimestampUtil.parseTimestamptz(pendingExtraAmountRequestedAt),
    confirmedExtraAmountTotal = confirmedExtraAmountTotal,
    broadcastTimerStartedAt = SupabaseTimestampUtil.parseTimestamptz(broadcastTimerStartedAt),
    disputeResolutionDecision = disputeResolutionDecision,
    disputeResolutionType = disputeResolutionType,
    disputeResolutionNote = disputeResolutionNote,
    disputeResolvedAt = SupabaseTimestampUtil.parseTimestamptz(disputeResolvedAt),
    disputeResultSeenByUser = disputeResultSeenByUser,
    disputeResultSeenBySolver = disputeResultSeenBySolver,
    disputeProgressAtSettlement = disputeProgressAtSettlement,
    solverCancelledNotice = solverCancelledNotice,
    disputeSettledAt = SupabaseTimestampUtil.parseTimestamptz(disputeSettledAt),
    disputeSplitSolverPercent = disputeSplitSolverPercent,
    disputeProgressAtRaise = disputeProgressAtRaise,
    completionResultSeenByUser = completionResultSeenByUser,
    completionResultSeenBySolver = completionResultSeenBySolver
)

/** [ProblemDto.toProblemEntity] এর মতোই কারণে (job-cancel history-তে ব্যবহৃত `resolvedAt`) আসল timestamp পার্স করা হয়েছে। */
fun BidDto.toBidEntity(): BidEntity = BidEntity(
    id = id,
    problemId = problemId,
    solverId = solverId,
    solverName = solverName,
    solverPhone = solverPhone,
    solverRating = solverRating,
    amount = amount,
    message = message,
    estimatedTime = estimatedTime,
    status = status,
    progressAtCancel = progressAtCancel,
    createdAt = SupabaseTimestampUtil.parseTimestamptz(createdAt) ?: System.currentTimeMillis(),
    resolutionType = resolutionType,
    resolvedAt = SupabaseTimestampUtil.parseTimestamptz(resolvedAt)
)
