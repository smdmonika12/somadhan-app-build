package com.example.data.security

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * [ADMIN_ROLE_PROFILE সেশন ৬] প্রোফাইল-স্ক্রিনের pure (Android-নির্ভরতাহীন) অংশ — ইউনিট-টেস্টযোগ্য।
 *
 * - [AdminSessionRecord]: `admin_sessions` টেবিলের এক সারি (সাম্প্রতিক সেশন লিস্ট)। টেবিলটা RLS-এ সুরক্ষিত
 *   (নিজের সেশন সবাই, সুপার সবার) — তাই আলাদা RPC ছাড়াই সরাসরি select।
 * - [AdminProfileRules]: ক্লায়েন্ট-সাইড ভ্যালিডেশন — সার্ভারের `admin_profile_update`-এর সীমার সাথে হুবহু মেলানো
 *   (নাম ≤৮০ ও ফাঁকা নয়, পদবি ≤৮০, ইমেইল ≤১২০, bio ≤৫০০)। সার্ভার আবার যাচাই করে; এটা শুধু দ্রুত ফিডব্যাকের জন্য।
 * - [adminPermissionSummary]: রোলের পারমিশন-কী → "আইটেম — অ্যাকশন, অ্যাকশন" চিপ-টেক্সট, [AdminPermissionCatalog] থেকে।
 */
data class AdminSessionRecord(
    val id: String,
    val device: String,
    val ip: String,
    val location: String,
    val loggedInAt: String?,
    val lastSeenAt: String?,
    val endedAt: String?
) {
    companion object {
        /** `admin_sessions` সারি → [AdminSessionRecord]; `id` না থাকলে null। */
        fun fromJson(obj: JsonObject): AdminSessionRecord? {
            fun str(k: String): String? =
                (obj[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
            val id = str("id") ?: return null
            return AdminSessionRecord(
                id = id,
                device = str("device").orEmpty(),
                ip = str("ip").orEmpty(),
                location = str("location").orEmpty(),
                loggedInAt = str("logged_in_at"),
                lastSeenAt = str("last_seen_at"),
                endedAt = str("ended_at")
            )
        }
    }
}

object AdminProfileRules {
    const val NAME_MAX = 80
    const val DESIGNATION_MAX = 80
    const val EMAIL_MAX = 120
    const val BIO_MAX = 500

    /** খুব সরল ইমেইল-আকৃতি (কিছু@কিছু.কিছু, ফাঁকা নয়) — সার্ভার শুধু দৈর্ঘ্য দেখে, তাই ক্লায়েন্টে আকৃতিও দেখা হয়। */
    fun isPlausibleEmail(email: String): Boolean =
        Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email)

    /**
     * ইনপুট ঠিক থাকলে null, নইলে বাংলা ত্রুটি-বার্তা। ইমেইল ফাঁকা হলে বৈধ (মুছে ফেলা)। পদবি ফাঁকা হলেও বৈধ
     * (সার্ভারের `designation` ডিফল্ট '')। প্যারামিটার আগেই trim করা ধরে নেওয়া হয়।
     */
    fun validate(name: String, designation: String, email: String, bio: String): String? = when {
        name.isBlank() -> "নাম দিন।"
        name.length > NAME_MAX -> "নাম সর্বোচ্চ $NAME_MAX অক্ষরের হতে পারবে।"
        designation.length > DESIGNATION_MAX -> "পদবি সর্বোচ্চ $DESIGNATION_MAX অক্ষরের হতে পারবে।"
        email.length > EMAIL_MAX -> "ইমেইল সর্বোচ্চ $EMAIL_MAX অক্ষরের হতে পারবে।"
        email.isNotEmpty() && !isPlausibleEmail(email) -> "ইমেইল ঠিকানাটা সঠিক নয় (যেমন: you@example.com)।"
        bio.length > BIO_MAX -> "সংক্ষিপ্ত পরিচিতি সর্বোচ্চ $BIO_MAX অক্ষরের হতে পারবে।"
        else -> null
    }
}

/** সুপার রোলের একমাত্র চিপ (সুপার রোলের `permissions` তালিকা ফাঁকা থাকে — `isSuper` মানেই সবকিছু)। */
const val ADMIN_SUPER_ACCESS_CHIP = "সব মেনু ও ফাংশনে সম্পূর্ণ এক্সেস — কোনো সীমাবদ্ধতা নেই"

/**
 * রোলের পারমিশন → পড়ার-উপযোগী চিপ-তালিকা (ক্যাটালগের ক্রম অনুযায়ী)। প্রতিটা আইটেমে যে অ্যাকশনগুলো সত্যিই
 * আছে শুধু সেগুলো — `"কেওয়াইসি — দেখুন, অনুমোদন, রিজেক্ট"`। ক্যাটালগে নেই এমন কী (পুরনো/অজানা) নিঃশব্দে বাদ।
 * কোনো পারমিশন না থাকলে ফাঁকা তালিকা (UI "কোনো ফাংশন-এক্সেস নেই" দেখায়)।
 */
fun adminPermissionSummary(isSuper: Boolean, permissions: Set<String>): List<String> {
    if (isSuper) return listOf(ADMIN_SUPER_ACCESS_CHIP)
    val chips = mutableListOf<String>()
    for (group in AdminPermissionCatalog.GROUPS) {
        for (item in group.items) {
            val granted = item.actions.filter { "${group.id}:${item.id}:${it.id}" in permissions }
            if (granted.isNotEmpty()) {
                chips += "${item.label} — ${granted.joinToString(", ") { it.label }}"
            }
        }
    }
    return chips
}
