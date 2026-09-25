package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase টেবিল `public.messages` এর DTO। */
@Serializable
data class MessageDto(
    val id: String, // text, PK
    @SerialName("problem_id") val problemId: String, // FK -> problems.id
    @SerialName("sender_id") val senderId: String? = null, // uuid, nullable, FK -> users.id
    @SerialName("receiver_id") val receiverId: String? = null, // uuid, nullable, FK -> users.id
    @SerialName("sender_name") val senderName: String = "",
    val content: String = "",
    val timestamp: String? = null, // timestamptz
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_type") val fileType: String? = null,
    @SerialName("is_direct_contract_proposal") val isDirectContractProposal: Boolean = false,
    @SerialName("direct_contract_budget") val directContractBudget: Double? = null,
    @SerialName("direct_contract_duration") val directContractDuration: String? = null,
    @SerialName("is_admin_message") val isAdminMessage: Boolean = false,
    @SerialName("is_dispute_notice") val isDisputeNotice: Boolean = false,
    @SerialName("is_system_event") val isSystemEvent: Boolean = false,
    @SerialName("system_event_type") val systemEventType: String? = null
)
