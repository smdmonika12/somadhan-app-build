package com.example.data.security

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ADMIN_ROLE_PROFILE সেশন ৭.০] `adminCanAct`/`AdminSession.canAct` — শেয়ার্ড পারমিশন-গেট ফাউন্ডেশনের
 * টেস্ট। সেশন ৭.১-৭.৮-এর ২৫টা স্ক্রিনের সব গেটিং এই একটা ফাংশনের ওপর দাঁড়াবে, তাই এখানে ভুল থাকলে সব
 * সাব-স্টেপে ছড়িয়ে পড়বে — তাই আগে-ভাগে যতটা সম্ভব নিয়ম টেস্ট করা হলো।
 *
 * ⚠️ এই ফাইল লেখার সময় Gradle/SDK ছিল না — কখনো চালানো হয়নি (আগের সেশনগুলোর মতোই সীমাবদ্ধতা)। প্রথম
 * CI/Android Studio রানে পাস/ফেল যাচাই হবে।
 */
class AdminSessionCanActLogicTest {

    private fun account(
        isSuper: Boolean = false,
        permissions: Set<String> = emptySet(),
        active: Boolean = true,
        flagged: Boolean = false
    ) = AdminAccountInfo(
        id = "acc-1",
        name = "টেস্ট এডমিন",
        designation = "",
        phone = "01700000000",
        email = null,
        photoUrl = null,
        bio = "",
        roleId = "role-1",
        roleName = "টেস্ট রোল",
        isSuper = isSuper,
        permissions = permissions,
        active = active,
        flagged = flagged,
        lastLoginAt = null,
        lastLoginDevice = null,
        lastLoginIp = null,
        isOnline = false
    )

    @After
    fun tearDown() {
        // পরের টেস্ট ফাইল/ক্লাসের ওপর প্রভাব না পড়ে, যেহেতু `AdminSession` একটা প্রসেস-জুড়ে সিঙ্গলটন `object`।
        AdminSession.clear()
    }

    // ---------- pure `adminCanAct` — মূল নিয়ম ----------

    @Test
    fun noAccount_alwaysDenied() {
        assertFalse(adminCanAct("users:users:view", account = null))
    }

    @Test
    fun inactiveAccount_alwaysDenied_evenIfSuper() {
        assertFalse(adminCanAct("users:users:view", account(isSuper = true, active = false)))
    }

    @Test
    fun roleAdmin_getsOnlyTickedKeys() {
        val acc = account(permissions = setOf("users:users:view", "users:users:ban"))
        assertTrue(adminCanAct("users:users:ban", acc))
        assertFalse(adminCanAct("users:users:delete", acc))
    }

    @Test
    fun flagged_keepsViewButBlocksEveryOtherAction() {
        val acc = account(permissions = setOf("users:users:view", "users:users:ban"), flagged = true)
        assertTrue(adminCanAct("users:users:view", acc))
        assertFalse(adminCanAct("users:users:ban", acc))
    }

    @Test
    fun nonFlagged_roleAdmin_viewNotAutomaticallyGranted_withoutTheKey() {
        // view নিজেও একটা সাধারণ কী — টিক না থাকলে (flagged না হলেও) allowed না।
        val acc = account(permissions = setOf("users:users:ban"))
        assertFalse(adminCanAct("users:users:view", acc))
    }

    @Test
    fun super_getsEverything_whenNotFlagged() {
        val acc = account(isSuper = true)
        assertTrue(adminCanAct("finance:withdrawals:approve", acc))
        assertTrue(adminCanAct("admin_mgmt:role_mgmt:delete", acc))
    }

    // ---------- হার্ড-সুপার-অনলি ওভাররাইড (৭.৪/৭.৬-কনফার্মড সিদ্ধান্ত) ----------

    @Test
    fun hardSuperOnly_explorerAndRefundDebug_blockedForNonSuper_evenViewAndEvenIfTicked() {
        // `system:explorer:view`/`system:refund_debug:view` — পুরো স্ক্রিনই সুপার-অনলি, view-ও ব্যতিক্রম না।
        val accWithoutKey = account(permissions = emptySet())
        val accWithKeyAnyway = account(permissions = setOf("system:explorer:view", "system:refund_debug:view"))
        assertFalse(adminCanAct("system:explorer:view", accWithoutKey))
        assertFalse(adminCanAct("system:refund_debug:view", accWithoutKey))
        // রোলে ভুলবশত/পুরনো-ডেটায় কী টিক-করা থাকলেও হার্ড-ওভাররাইড জেতে
        assertFalse(adminCanAct("system:explorer:view", accWithKeyAnyway))
        assertFalse(adminCanAct("system:refund_debug:view", accWithKeyAnyway))
    }

    @Test
    fun hardSuperOnly_explorerAndRefundDebug_allowedForSuper() {
        val acc = account(isSuper = true)
        assertTrue(adminCanAct("system:explorer:edit_field", acc))
        assertTrue(adminCanAct("system:explorer:view", acc))
        assertTrue(adminCanAct("system:refund_debug:view", acc))
    }

    @Test
    fun hardSuperOnly_factoryResetAndCommissionCleanup_blockedForNonSuper_evenIfTicked() {
        val accWithKeyAnyway = account(
            permissions = setOf("config:settings:factory_reset", "config:settings:cleanup_commission")
        )
        assertFalse(adminCanAct("config:settings:factory_reset", accWithKeyAnyway))
        assertFalse(adminCanAct("config:settings:cleanup_commission", accWithKeyAnyway))
    }

    @Test
    fun hardSuperOnly_doesNotAffectOtherKeysInSameItem() {
        // `config:settings:edit`/`view` স্বাভাবিক গ্র্যানুলার-পারমিশন নিয়মেই থাকবে, শুধু factory_reset/cleanup_commission হার্ড-সুপার।
        val acc = account(permissions = setOf("config:settings:view", "config:settings:edit"))
        assertTrue(adminCanAct("config:settings:edit", acc))
        assertFalse(adminCanAct("config:settings:factory_reset", acc))
    }

    // ---------- `AdminSession.canAct`/`canView` — সিঙ্গলটন-ওয়্যারিং ----------

    @Test
    fun adminSession_canAct_readsCurrentState() {
        AdminSession.set(sessionId = "s1", account = account(permissions = setOf("users:users:view")))
        assertTrue(AdminSession.canAct("users:users:view"))
        assertFalse(AdminSession.canAct("users:users:ban"))
    }

    @Test
    fun adminSession_canAct_falseWhenNoSessionSet() {
        AdminSession.clear()
        assertFalse(AdminSession.canAct("users:users:view"))
    }

    @Test
    fun adminSession_canView_isShorthandForViewKey() {
        AdminSession.set(sessionId = "s1", account = account(permissions = setOf("finance:withdrawals:view")))
        assertTrue(AdminSession.canView("finance", "withdrawals"))
        assertFalse(AdminSession.canView("finance", "escrow"))
    }
}
