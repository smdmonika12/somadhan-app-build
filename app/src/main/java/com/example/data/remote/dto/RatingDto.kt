package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase টেবিল `public.ratings` এর DTO। */
@Serializable
data class RatingDto(
    val id: String, // text, PK
    @SerialName("problem_id") val problemId: String, // FK -> problems.id
    @SerialName("problem_title") val problemTitle: String = "",
    @SerialName("user_id") val userId: String? = null, // uuid, nullable, FK -> users.id
    @SerialName("user_name") val userName: String = "",
    @SerialName("solver_id") val solverId: String? = null, // uuid, nullable, FK -> users.id
    @SerialName("solver_name") val solverName: String = "",
    val stars: Int, // check: 1..5
    val comment: String = "",
    @SerialName("rater_role") val raterRole: String = "USER", // check: USER | SOLVER
    @SerialName("created_at") val createdAt: String? = null // timestamptz
)
