package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase টেবিল `public.faqs` এর DTO। */
@Serializable
data class FaqDto(
    val id: String, // text, PK
    val question: String,
    val answer: String,
    @SerialName("target_audience") val targetAudience: String = "USER", // check: USER | SOLVER
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null // timestamptz
)
