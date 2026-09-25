package com.example.data.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * [ADMIN_ROLE_PROFILE সেশন ২] বর্তমানে লগইন করা এডমিনের অ্যাকাউন্ট-স্ন্যাপশট।
 *
 * সার্ভারের `admin_session_start` / `admin_heartbeat` / `admin_me` RPC-র `_admin_account_view` JSON থেকে
 * তৈরি হয় — ক্লায়েন্ট কখনো নিজে থেকে রোল/পারমিশন বানায় না, শুধু সার্ভারের দেওয়া মান ধরে রাখে।
 * `permissions` হলো রোলের পারমিশন-কী সেট (যেমন "users:users:ban"); সুপার রোলের তালিকা ফাঁকা থাকে
 * ([isSuper] = true মানেই সবকিছুর অনুমতি — সেশন ৭.০-এর `canAct` এটা ব্যবহার করবে)।
 */
data class AdminAccountInfo(
    val id: String,
    val name: String,
    val designation: String,
    val phone: String,
    val email: String?,
    val photoUrl: String?,
    val bio: String,
    val roleId: String,
    val roleName: String,
    val isSuper: Boolean,
    val permissions: Set<String>,
    val active: Boolean,
    val flagged: Boolean,
    val lastLoginAt: String?,
    val lastLoginDevice: String?,
    val lastLoginIp: String?,
    val isOnline: Boolean,
    // [ADMIN_ROLE_PROFILE সেশন ৪] এডমিন-অ্যাকাউন্ট তালিকা/প্রোফাইলের জন্য — সবগুলো ডিফল্টসহ, তাই সেশন ২-৩-এর
    // বিদ্যমান কল-সাইট/টেস্ট অপরিবর্তিত। `last_seen_at` প্রতি heartbeat-এ বদলায় (UI-র per-item pulse এটা বাদ দেয়)।
    val createdAt: String? = null,
    val flaggedAt: String? = null,
    val lastSeenAt: String? = null
) {
    companion object {
        /** সার্ভারের `_admin_account_view` JSON → [AdminAccountInfo]; দরকারি ফিল্ড না থাকলে null। */
        fun fromJson(obj: JsonObject): AdminAccountInfo? {
            fun str(k: String): String? =
                (obj[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
            fun bool(k: String): Boolean =
                (obj[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content == "true"
            val id = str("id") ?: return null
            val perms = (obj["permissions"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p !is JsonNull }?.content }
                ?.toSet() ?: emptySet()
            return AdminAccountInfo(
                id = id,
                name = str("name") ?: "",
                designation = str("designation") ?: "",
                phone = str("phone") ?: "",
                email = str("email"),
                photoUrl = str("photo_url"),
                bio = str("bio") ?: "",
                roleId = str("role_id") ?: "",
                roleName = str("role_name") ?: "",
                isSuper = bool("is_super"),
                permissions = perms,
                active = (obj["active"] as? JsonPrimitive)?.content != "false",
                flagged = bool("flagged"),
                lastLoginAt = str("last_login_at"),
                lastLoginDevice = str("last_login_device"),
                lastLoginIp = str("last_login_ip"),
                isOnline = bool("is_online"),
                createdAt = str("created_at"),
                flaggedAt = str("flagged_at"),
                lastSeenAt = str("last_seen_at")
            )
        }
    }
}

/** এক লগইন-সেশনের সম্পূর্ণ অবস্থা: সার্ভার-সেশন আইডি (heartbeat/লগআউটের জন্য) + অ্যাকাউন্ট-স্ন্যাপশট। */
data class AdminSessionState(
    val sessionId: String?,
    val account: AdminAccountInfo
)

/**
 * [ADMIN_ROLE_PROFILE সেশন ৭.০] `AdminPermissionCatalog`-এর যে কয়েকটা অ্যাকশন-কী ব্যবহারকারী কনফার্মড
 * সিদ্ধান্ত অনুযায়ী গ্র্যানুলার রোল-পারমিশনের বদলে **সরাসরি "শুধু সুপার অ্যাডমিন" হার্ড-চেক** (দেখুন
 * `ADMIN_ROLE_PROFILE_PROGRESS.md`-এর সেশন ৭-পূর্ব কনফার্মেশন #২ ও মাস্টার প্রম্পট সাব-স্টেপ ৭.৪/৭.৬) —
 * এই কী-গুলো কোনো রোলের `permissions` সেটে থাকলেও (পুরনো/ভুলবশত টিক করা ডেটা হলেও) উপেক্ষা করা হয়,
 * শুধু `isSuper` দিয়েই সিদ্ধান্ত হয়। `system:explorer:*`/`system:refund_debug:*` পুরো স্ক্রিন-ই (view
 * সহ) সুপার-অনলি — এই দুটো high-risk/debug টুল কোনো নন-সুপার রোলকে দেখাতেও দেওয়া হবে না।
 *
 * ⚠️ `AdminRolePreviewPanel.kt`-এর `previewActionAllowed`/`previewItemVisible` (সেশন ৩, রোল-এডিটরের
 * লাইভ প্রিভিউ সিমুলেটর) **এখনো এই হার্ড-সুপার-ওভাররাইড জানে না** — ইচ্ছাকৃতভাবে এই সেশনে ছোঁয়া হয়নি
 * (স্কোপ সংকীর্ণ রাখতে), তাই এই মুহূর্তে প্রিভিউ-প্যানেল যদি এমন কোনো রোলে এই কী-গুলো টিক-করা দেখায়
 * (বাস্তবে সম্ভব না যেহেতু ক্যাটালগ-UI-ও এখনো বদলায়নি) সিমুলেশন ভুল দেখাতে পারে। প্রকৃত গেটিং (এই
 * ফাইলের [adminCanAct]/[AdminSession.canAct]) সবসময় সঠিক থাকে যেহেতু সুপার-নন-সুপার নির্বিশেষে এই
 * ওভাররাইড প্রয়োগ হয়। সামঞ্জস্যের জন্য প্রিভিউ-প্যানেলও একই ওভাররাইড ব্যবহার করা উচিত — সাব-স্টেপ
 * ৭.৪/৭.৬-এ (যখন রোল-এডিটর UI-ও এই কী-গুলোর জন্য চেকবক্সের বদলে "শুধু সুপার" নোট দেখাবে) একসাথে ঠিক
 * করার জন্য এখানে ফ্ল্যাগ করে রাখা হলো, যাতে ভুলে বাদ না যায়।
 */
private val ADMIN_HARD_SUPER_ONLY_PREFIXES = listOf("system:explorer:", "system:refund_debug:")
private val ADMIN_HARD_SUPER_ONLY_EXACT_KEYS = setOf(
    "config:settings:factory_reset",
    "config:settings:cleanup_commission",
    // [ADMIN_ROLE_PROFILE সেশন ৭.১.২, ২০২৬-০৯-২৫] "বানাও অ্যাডমিন" — admin_account_create RPC নিজেই
    // সার্ভার-সাইডে শুধু সুপার caller accept করে (_admin_require_super()), তাই ক্লায়েন্ট-গেটও হার্ড-সুপার
    // (রোলে টিক থাকলেও নন-সুপারের জন্য বাটন hidden)।
    "users:users:create_admin"
)

private fun isHardSuperOnlyActionKey(actionKey: String): Boolean =
    actionKey in ADMIN_HARD_SUPER_ONLY_EXACT_KEYS ||
        ADMIN_HARD_SUPER_ONLY_PREFIXES.any { actionKey.startsWith(it) }

/**
 * [ADMIN_ROLE_PROFILE সেশন ৭.০] শেয়ার্ড পারমিশন-গেট — pure ফাংশন হিসেবে (কোনো গ্লোবাল স্টেট পড়ে না,
 * `AdminSession.canAct` এটাকেই কল করে) যাতে ইউনিট-টেস্ট সহজ থাকে, ঠিক `AdminRolePreviewPanel.kt`-এর
 * `previewActionAllowed`-এর স্টাইলে।
 *
 * নিয়ম (মাস্টার প্রম্পট সাব-স্টেপ ৭.০, মকআপ ১-এর আচরণ অনুযায়ী):
 * 1. `account == null` বা `account.active == false` → সবসময় false (সেশন না থাকলে/নিষ্ক্রিয় হলে কিছুই না)।
 * 2. [isHardSuperOnlyActionKey] হলে → শুধু `account.isSuper` (রোল-পারমিশন/ফ্ল্যাগ কিছুই প্রভাব ফেলে না)।
 * 3. সুপার অ্যাডমিন (এবং হার্ড-সুপার-অনলি না) → সবসময় true (সুপার কখনো ফ্ল্যাগড হতে পারে না, DB-trigger
 *    দিয়ে সুরক্ষিত — সেশন ১, তাই এখানে আলাদা flagged-চেক লাগে না)।
 * 4. নন-সুপার, ফ্ল্যাগড, `view` ছাড়া অন্য যেকোনো অ্যাকশন → false ("view" অক্ষত — মকআপের ফ্ল্যাগ-আচরণ,
 *    ফ্ল্যাগড অ্যাডমিন মেনু দেখতে পারবে কিন্তু কোনো অ্যাকশন-বাটন কাজ করবে না)।
 * 5. বাকি সব ক্ষেত্রে → রোলের `permissions` সেটে `actionKey` আছে কিনা।
 */
fun adminCanAct(actionKey: String, account: AdminAccountInfo?): Boolean {
    if (account == null || !account.active) return false
    if (isHardSuperOnlyActionKey(actionKey)) return account.isSuper
    if (account.isSuper) return true
    val isViewAction = actionKey.endsWith(":view")
    if (account.flagged && !isViewAction) return false
    return actionKey in account.permissions
}

/**
 * [ADMIN_ROLE_PROFILE সেশন ২] অ্যাপ-জুড়ে একটাই "কে এডমিন হিসেবে লগইন করেছে" state-holder।
 *
 * আগে অ্যাপে "কে লগইন করেছে" ধারণাই ছিল না (একটাই shared admin credential)। এখন লগইনের পর
 * [SomadhanViewModel] এটা সেট করে, প্রতি ~৩০ সেকেন্ডের heartbeat-এ সার্ভারের সর্বশেষ মান দিয়ে [update]
 * করে (ফলে রোল/পারমিশন/flag বদলালে রিলগইন ছাড়াই ধরা পড়ে), আর লগআউট/জোর-লগআউটে [clear] করে।
 *
 * সেশন ৩-৭ এই [state] থেকেই পড়বে (রোল-ম্যানেজমেন্ট সুপার-অনলি গেট, প্রোফাইল, canAct ইত্যাদি)।
 * এটা কোনো নিরাপত্তা-সীমানা না — আসল সীমানা সার্ভারে (`is_admin`/RLS/`admin_can_act`); এটা শুধু UI-র জন্য।
 */
object AdminSession {
    private val _state = MutableStateFlow<AdminSessionState?>(null)
    val state: StateFlow<AdminSessionState?> = _state.asStateFlow()

    /** বর্তমান স্ন্যাপশট (কম্পোজিশনের বাইরে দ্রুত পড়ার জন্য)। */
    val current: AdminSessionState? get() = _state.value

    fun set(sessionId: String?, account: AdminAccountInfo) {
        _state.value = AdminSessionState(sessionId = sessionId, account = account)
    }

    /** heartbeat-এর সর্বশেষ অ্যাকাউন্ট-মান বসায়; সেশন আইডি অপরিবর্তিত থাকে। সেশন না থাকলে কিছু করে না। */
    fun update(account: AdminAccountInfo) {
        val cur = _state.value ?: return
        _state.value = cur.copy(account = account)
    }

    fun clear() {
        _state.value = null
    }

    /**
     * [ADMIN_ROLE_PROFILE সেশন ৭.০] বর্তমান লগইন-করা এডমিন `actionKey` (যেমন `"users:users:ban"`,
     * `"finance:withdrawals:view"`) করতে পারবে কিনা — এই একটা ফাংশনই সেশন ৭.১-৭.৮-এ ২৫টা
     * `AdminXxxView.kt`-এর প্রতিটা স্ক্রিন-গেট ও অ্যাকশন-বাটনের জন্য ব্যবহৃত হবে, প্রতি স্ক্রিনে আলাদা
     * `if (isSuper || ...)` না লিখে। আসল যুক্তি [adminCanAct]-এ (pure, টেস্টযোগ্য)।
     *
     * ⚠️ এটা শুধু UI-র জন্য সুবিধা — কোনো নিরাপত্তা-সীমানা না (দেখুন এই ফাইলের হেডার কমেন্ট); আসল
     * সীমানা সার্ভারে (RLS/RPC-এর ভেতরের `is_super_admin`/গার্ড, সেশন ১)।
     */
    fun canAct(actionKey: String): Boolean = adminCanAct(actionKey, current?.account)

    /** শর্টহ্যান্ড: `"$groupId:$itemId:view"` — কোনো ট্যাব/স্ক্রিন খোলার আগে শুধু 'দেখুন'-অনুমতি চেক করতে। */
    fun canView(groupId: String, itemId: String): Boolean = canAct("$groupId:$itemId:view")
}
