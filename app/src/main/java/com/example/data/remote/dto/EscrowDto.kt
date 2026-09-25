package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase টেবিল `public.escrows` এর DTO। */
@Serializable
data class EscrowDto(
    val id: String, // text, PK
    @SerialName("problem_id") val problemId: String, // FK -> problems.id
    @SerialName("problem_title") val problemTitle: String = "",
    @SerialName("user_id") val userId: String, // uuid, FK -> users.id
    @SerialName("solver_id") val solverId: String, // uuid, FK -> users.id
    @SerialName("base_amount") val baseAmount: Double,
    @SerialName("extra_amount") val extraAmount: Double = 0.0,
    val status: String = "HELD", // check: HELD | RELEASED | REFUNDED
    @SerialName("created_at") val createdAt: String? = null, // timestamptz
    @SerialName("released_at") val releasedAt: String? = null, // timestamptz
    @SerialName("updated_at") val updatedAt: String? = null // timestamptz
)
