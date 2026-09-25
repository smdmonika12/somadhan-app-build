package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase টেবিল `public.transactions` এর DTO। */
@Serializable
data class TransactionDto(
    val id: String, // text, PK
    @SerialName("problem_id") val problemId: String = "",
    @SerialName("problem_title") val problemTitle: String = "",
    @SerialName("user_id") val userId: String? = null, // uuid, nullable, FK -> users.id
    @SerialName("solver_id") val solverId: String? = null, // uuid, nullable, FK -> users.id
    @SerialName("gross_amount") val grossAmount: Double = 0.0,
    @SerialName("commission_percent") val commissionPercent: Double = 0.0,
    @SerialName("commission_amount") val commissionAmount: Double = 0.0,
    @SerialName("net_amount") val netAmount: Double = 0.0,
    val timestamp: String? = null, // timestamptz
    @SerialName("base_amount") val baseAmount: Double = 0.0,
    @SerialName("extra_amount") val extraAmount: Double = 0.0,
    @SerialName("base_commission_amount") val baseCommissionAmount: Double = 0.0,
    @SerialName("extra_commission_amount") val extraCommissionAmount: Double = 0.0,
    @SerialName("extra_commission_applied") val extraCommissionApplied: Boolean = false,
    @SerialName("was_free_quota_job") val wasFreeQuotaJob: Boolean = false,
    val type: String,
    @SerialName("escrow_id") val escrowId: String? = null, // nullable, FK -> escrows.id
    @SerialName("refund_type") val refundType: String = "",
    @SerialName("refund_percentage") val refundPercentage: Double = 100.0,
    @SerialName("release_type") val releaseType: String = "",
    val role: String = "" // "USER" | "SOLVER" | "" (legacy/unclassified) — ধাপ ৩৬ migration-এ যোগ হওয়া কলাম
)
