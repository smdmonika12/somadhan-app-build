package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase টেবিল `public.notifications` এর DTO। */
@Serializable
data class NotificationDto(
    val id: String, // text, PK
    @SerialName("user_id") val userId: String, // uuid, FK -> users.id
    val title: String = "",
    val message: String = "",
    val timestamp: String? = null, // timestamptz
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("related_problem_id") val relatedProblemId: String? = null,
    @SerialName("target_type") val targetType: String = "problem",
    @SerialName("target_id") val targetId: String? = null,
    @SerialName("scheduled_for") val scheduledFor: String? = null, // timestamptz
    // Supabase-সাইডে `role` কলাম আগে থেকেই আছে (create_notification/admin_notify_user/
    // log_admin_action-এর p_role-সহ overload, `notifications`/`admin_audit_logs` টেবিলে ডিফল্ট
    // '') -- এতদিন এই DTO-তে ফিল্ডটাই ছিল না, তাই cloud থেকে অন্য ডিভাইসে sync হয়ে আসা
    // notification role-neutral ("") হিসেবে ম্যাপ হতো, ব্যান-ফিল্টার কাজ করত না (multi-device গ্যাপ)।
    val role: String = ""
)
