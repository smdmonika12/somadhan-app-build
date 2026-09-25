package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase টেবিল `public.categories` এর DTO। */
@Serializable
data class CategoryDto(
    val id: String, // text, PK
    @SerialName("name_bangla") val nameBangla: String,
    @SerialName("name_english") val nameEnglish: String,
    @SerialName("is_physical") val isPhysical: Boolean,
    @SerialName("icon_name") val iconName: String = "",
    val keywords: String = "",
    @SerialName("min_budget") val minBudget: Double = 0.0,
    @SerialName("max_budget") val maxBudget: Double = 0.0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("instant_job_enabled") val instantJobEnabled: Boolean = true,
    @SerialName("instant_job_radius_km") val instantJobRadiusKm: Double = 5.0
)
