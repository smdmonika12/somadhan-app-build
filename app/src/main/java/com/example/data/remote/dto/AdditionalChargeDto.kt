package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase টেবিল `public.additional_charges` এর DTO। */
@Serializable
data class AdditionalChargeDto(
    val id: String, // text, PK
    @SerialName("problem_id") val problemId: String, // FK -> problems.id
    @SerialName("solver_id") val solverId: String, // uuid, FK -> users.id
    @SerialName("user_id") val userId: String, // uuid, FK -> users.id
    val reason: String = "",
    val amount: Double,
    val status: String = "PENDING", // check: PENDING | ACCEPTED | REJECTED
    @SerialName("created_at") val createdAt: String? = null, // timestamptz
    @SerialName("responded_at") val respondedAt: String? = null // timestamptz
)
