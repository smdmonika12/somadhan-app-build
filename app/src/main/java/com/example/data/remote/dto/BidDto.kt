package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase টেবিল `public.bids` এর DTO। */
@Serializable
data class BidDto(
    val id: String, // text, PK
    @SerialName("problem_id") val problemId: String, // FK -> problems.id
    @SerialName("solver_id") val solverId: String, // uuid, FK -> users.id
    @SerialName("solver_name") val solverName: String = "",
    @SerialName("solver_phone") val solverPhone: String = "",
    @SerialName("solver_rating") val solverRating: Double = 5.0,
    val amount: Double,
    val message: String = "",
    @SerialName("estimated_time") val estimatedTime: String = "",
    val status: String = "PENDING", // check: PENDING | ACCEPTED | REJECTED | CANCELLED
    @SerialName("progress_at_cancel") val progressAtCancel: Int? = null,
    @SerialName("created_at") val createdAt: String? = null, // timestamptz
    @SerialName("resolution_type") val resolutionType: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null // timestamptz
)
