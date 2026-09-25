package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase টেবিল `public.admin_audit_logs` এর DTO। */
@Serializable
data class AdminAuditLogDto(
    val id: String, // text, PK
    @SerialName("action_type") val actionType: String,
    @SerialName("target_id") val targetId: String = "",
    @SerialName("target_name") val targetName: String = "",
    val details: String = "",
    val timestamp: String? = null, // timestamptz
    // `admin_audit_logs`-এ role কলাম আগে থেকেই আছে (log_admin_action-এর p_role overload) --
    // DTO-তে completeness-এর জন্য যোগ করা হলো। ⚠️ এই মুহূর্তে কোনো mapper/pull path এটা ব্যবহার
    // করে না (admin_audit_logs কখনোই cloud→local bulk-pull হয় না, দেখুন
    // SomadhanRepository.kt-এর "admin_audit_logs: শুধু local insert" কমেন্ট) -- future-proofing।
    val role: String = ""
)
