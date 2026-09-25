package com.example.ui.screens

import com.example.data.security.AdminPermissionCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ADMIN_ROLE_PROFILE সেশন ৩ — অংশ ২.২] লাইভ প্রিভিউর pure লজিক + ক্যাটালগের কাঠামোগত ইনভ্যারিয়েন্ট।
 *
 * ⚠️ এই ফাইল লেখার সময় Gradle/SDK ছিল না — টেস্টটা কখনো চালানো হয়নি। প্রথম CI/Android Studio রানে
 * পাস/ফেল যাচাই হবে। ব্যর্থ হলে আগে ঠিক করতে হবে কোডটা ভুল নাকি টেস্টের অনুমান।
 *
 * কী নিশ্চিত করে:
 * 1. ফ্ল্যাগড অ্যাডমিন: `view` অক্ষত, বাকি সব অ্যাকশন ব্লকড (মাস্টার প্রম্পট ৭.০-এর নিয়ম)।
 * 2. সুপার সবকিছু পায় (ফ্ল্যাগড না হলে); রোল-অ্যাডমিন শুধু নিজের টিক-করা কী পায়।
 * 3. মেনু দেখা যাওয়া = `view` কী; অন্য অ্যাকশনের কী থাকলেও `view` ছাড়া মেনু লুকানো।
 * 4. ক্যাটালগ: ট্যাব-ইনডেক্স ইউনিক, রোল-ম্যানেজমেন্ট = ২৫, `admin_mgmt` সুপার-অনলি ও nonSuperKeys-এ নেই।
 */
class AdminRolePreviewLogicTest {

    private val perms = setOf("users:users:view", "users:users:ban")

    @Test
    fun roleAdmin_getsOnlyTickedKeys() {
        assertTrue(previewActionAllowed(perms, isSuper = false, isFlagged = false, "users", "users", "ban"))
        assertFalse(previewActionAllowed(perms, isSuper = false, isFlagged = false, "users", "users", "delete"))
    }

    @Test
    fun flagged_keepsViewButBlocksEveryOtherAction() {
        assertTrue(previewActionAllowed(perms, isSuper = false, isFlagged = true, "users", "users", "view"))
        assertFalse(previewActionAllowed(perms, isSuper = false, isFlagged = true, "users", "users", "ban"))
    }

    @Test
    fun super_getsEverything_whenNotFlagged() {
        assertTrue(previewActionAllowed(emptySet(), isSuper = true, isFlagged = false, "finance", "withdrawals", "approve"))
    }

    @Test
    fun menuVisibility_requiresViewKey_evenIfOtherActionsTicked() {
        assertTrue(previewItemVisible(perms, isSuper = false, "users", "users"))
        val banOnly = setOf("users:users:ban")
        assertFalse(previewItemVisible(banOnly, isSuper = false, "users", "users"))
        assertTrue(previewItemVisible(emptySet(), isSuper = true, "users", "users"))
    }

    @Test
    fun catalog_tabIndexesAreUnique() {
        val indexes = AdminPermissionCatalog.GROUPS.flatMap { g -> g.items.mapNotNull { it.tabIndex } }
        assertEquals("ডুপ্লিকেট tabIndex আছে: $indexes", indexes.size, indexes.toSet().size)
    }

    @Test
    fun catalog_roleMgmtUsesDedicatedTabIndex() {
        val item = AdminPermissionCatalog.findItem("admin_mgmt", "role_mgmt")
        assertEquals(AdminPermissionCatalog.ROLE_MGMT_TAB_INDEX, item?.tabIndex)
        assertEquals(25, AdminPermissionCatalog.ROLE_MGMT_TAB_INDEX)
    }

    @Test
    fun catalog_adminMgmtIsSuperOnly_andNeverAssignable() {
        val group = AdminPermissionCatalog.GROUPS.first { it.id == "admin_mgmt" }
        assertTrue(group.superOnly)
        assertTrue(AdminPermissionCatalog.nonSuperKeys().none { it.startsWith("admin_mgmt:") })
        assertTrue(AdminPermissionCatalog.allKeys().any { it.startsWith("admin_mgmt:") })
    }
}
