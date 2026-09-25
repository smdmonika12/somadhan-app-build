package com.example.data.security

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * [ADMIN_ROLE_PROFILE সেশন ৫] অ্যাক্টিভিটি লগের এক সারি — সার্ভারের `admin_activity_logs_list` RPC-র এক এলিমেন্ট।
 *
 * এডমিন-পরিচয় (`adminName`/`adminRoleName`/`adminId`) সার্ভারে `log_admin_action` নিজেই বসায় (auth.uid() থেকে) —
 * ক্লায়েন্ট কখনো নিজের নাম লগে পাঠায় না। ২০২৬-০৯-২৪-এর আগের লগ, আর সিস্টেম/ইউজার-ট্রিগার্ড ইভেন্টে এগুলো ফাঁকা
 * ([isAttributed] = false) — UI সেগুলো "সিস্টেম / লিগ্যাসি" দেখায়।
 *
 * ⚠️ [timestamp] সার্ভারের ISO স্ট্রিং **হুবহু** রাখা হয় (মাইক্রোসেকেন্ডসহ) — পরের পেজের কার্সর হিসেবে সেটাই ফেরত
 * যায়। Long/Instant-এ পার্স করে ফেরত পাঠালে মাইক্রোসেকেন্ড হারিয়ে (timestamp, id) তুলনা ভেঙে সারি বাদ পড়ত।
 */
data class AdminActivityLogEntry(
    val id: String,
    val actionType: String,
    val targetId: String,
    val targetName: String,
    val details: String,
    /** user/solver role ("USER"/"SOLVER"/"") — এডমিনের রোল না (সেটা [adminRoleName])। */
    val role: String,
    val timestamp: String,
    val adminId: String?,
    val adminName: String,
    val adminRoleName: String
) {
    val isAttributed: Boolean get() = adminName.isNotBlank()

    companion object {
        fun fromJson(obj: JsonObject): AdminActivityLogEntry? {
            fun str(k: String): String? =
                (obj[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
            val id = str("id") ?: return null
            val ts = str("timestamp") ?: return null
            val action = str("action_type") ?: return null
            return AdminActivityLogEntry(
                id = id,
                actionType = action,
                targetId = str("target_id") ?: "",
                targetName = str("target_name") ?: "",
                details = str("details") ?: "",
                role = str("role") ?: "",
                timestamp = ts,
                adminId = str("admin_id"),
                adminName = str("admin_name") ?: "",
                adminRoleName = str("admin_role_name") ?: ""
            )
        }
    }
}

/** এক পেজের ফলাফল। [hasMore] সার্ভার নির্ধারণ করে (limit+1 আনার কৌশলে) — ক্লায়েন্ট আন্দাজ করে না। */
data class AdminActivityLogPage(
    val rows: List<AdminActivityLogEntry>,
    val hasMore: Boolean
) {
    companion object {
        /** `{"rows":[...], "has_more":bool}` → [AdminActivityLogPage]; কাঠামো ভুল হলে null। */
        fun fromJson(el: JsonElement): AdminActivityLogPage? {
            val obj = el as? JsonObject ?: return null
            val arr = obj["rows"] as? JsonArray ?: return null
            val hasMore = (obj["has_more"] as? JsonPrimitive)?.content == "true"
            return AdminActivityLogPage(
                rows = arr.mapNotNull { (it as? JsonObject)?.let(AdminActivityLogEntry::fromJson) },
                hasMore = hasMore
            )
        }
    }
}

/**
 * ফিল্টারের প্রয়োগকৃত অবস্থা। UI-তে ইনপুট টাইপ করা আর "ফিল্টার করুন" চাপা আলাদা — শুধু চাপলে এটা বদলায় ও
 * সার্ভারে যায় (মাস্টার প্রম্পট: লাইভ-ফিল্টার না, explicit বাটন)।
 */
data class AdminActivityLogFilter(
    /** নির্দিষ্ট এডমিন (ড্রপডাউন)। null = সব। */
    val adminId: String? = null,
    /** এডমিনের নামের অংশ (সার্চ ইনপুট, denormalized `admin_name`-এ মেলে)। */
    val nameQuery: String = "",
    /** true = শুধু "সিস্টেম / লিগ্যাসি" (এডমিন-পরিচয়হীন) লগ। */
    val unattributedOnly: Boolean = false
) {
    val isActive: Boolean get() = adminId != null || nameQuery.isNotBlank() || unattributedOnly
}
