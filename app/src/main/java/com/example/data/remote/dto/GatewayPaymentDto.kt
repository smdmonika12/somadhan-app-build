package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase টেবিল `public.gateway_payments` এর DTO। */
@Serializable
data class GatewayPaymentDto(
    val id: String, // text, PK
    @SerialName("gateway_trx_id") val gatewayTrxId: String, // unique
    @SerialName("user_id") val userId: String? = null, // uuid, nullable, FK -> users.id
    @SerialName("user_name") val userName: String = "",
    @SerialName("user_phone") val userPhone: String = "",
    val amount: Double,
    val gateway: String = "",
    val purpose: String = "WALLET_DEPOSIT",
    @SerialName("problem_id") val problemId: String = "",
    @SerialName("problem_title") val problemTitle: String = "",
    val status: String = "SUCCESS",
    val note: String = "",
    val timestamp: String? = null // timestamptz
)
