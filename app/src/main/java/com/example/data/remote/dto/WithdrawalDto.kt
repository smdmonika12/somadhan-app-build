package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase টেবিল `public.withdrawals` এর DTO। */
@Serializable
data class WithdrawalDto(
    val id: String, // text, PK
    @SerialName("solver_id") val solverId: String, // uuid, FK -> users.id
    @SerialName("solver_name") val solverName: String = "",
    val amount: Double,
    val method: String,
    @SerialName("account_number") val accountNumber: String,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("branch_name") val branchName: String? = null,
    @SerialName("account_holder_name") val accountHolderName: String? = null,
    val status: String = "PENDING", // check: PENDING | COMPLETED | REJECTED
    @SerialName("trx_id") val trxId: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String? = null // timestamptz
)
