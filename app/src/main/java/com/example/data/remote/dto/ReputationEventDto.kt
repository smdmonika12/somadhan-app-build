package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase টেবিল `public.reputation_events` এর DTO। */
@Serializable
data class ReputationEventDto(
    val id: String, // text, PK
    @SerialName("user_id") val userId: String, // uuid, FK -> users.id
    @SerialName("event_type") val eventType: String,
    @SerialName("problem_id") val problemId: String? = null,
    @SerialName("score_change") val scoreChange: Double,
    @SerialName("score_after") val scoreAfter: Double,
    val note: String = "",
    @SerialName("created_at") val createdAt: String? = null // timestamptz
)
