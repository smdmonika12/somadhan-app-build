package com.example.data.remote

import com.example.data.entity.MessageEntity
import com.example.data.remote.dto.MessageDto

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ১১ (Repository Migration D1: Chat/Rating/Reputation)
 *
 * [ProblemBidMappers.kt]-এর মতোই শুধু write (insert) দিকের Entity → Dto mapper — read path এখনো
 * local Room-ই থেকে যাচ্ছে (একই কারণে: app-wide realtime/sync engine migration এখনো হয়নি,
 * "ধাপ ১৪.৫" আলাদা ভবিষ্যৎ ধাপের অপেক্ষায়)।
 *
 * Ratings-এর জন্য আলাদা mapper নেই — `submit_rating` RPC নিজেই সব প্রয়োজনীয় ফিল্ড (problem_title,
 * user_id, solver_id ইত্যাদি) problem row থেকে সার্ভার-সাইডে বের করে নেয়, তাই client থেকে শুধু
 * primitive প্যারামিটার (problemId/stars/comment/raterRole) পাঠালেই যথেষ্ট — কোনো পূর্ণাঙ্গ RatingDto
 * পাঠানোর দরকার নেই।
 */

/**
 * `sendMessage()`-এ ব্যবহারের জন্য। `timestamp` ইচ্ছাকৃতভাবে null রাখা হয়েছে (ProblemBidMappers.kt
 * এর `createdAt` এর মতোই একই কারণে) — DB-র নিজস্ব `default now()` ব্যবহার হবে, client clock-এর
 * বদলে।
 */
fun MessageEntity.toMessageDto(): MessageDto = MessageDto(
    id = id,
    problemId = problemId,
    senderId = senderId,
    receiverId = receiverId,
    senderName = senderName,
    content = content,
    isRead = isRead,
    fileUrl = fileUrl,
    fileName = fileName,
    fileType = fileType,
    isDirectContractProposal = isDirectContractProposal,
    directContractBudget = directContractBudget,
    directContractDuration = directContractDuration,
    isAdminMessage = isAdminMessage,
    isDisputeNotice = isDisputeNotice,
    isSystemEvent = isSystemEvent,
    systemEventType = systemEventType
)
