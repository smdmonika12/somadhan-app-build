package com.example.ui.screens

import com.example.data.security.AdminAccountInfo
import com.example.data.security.AdminActivityLogEntry
import com.example.data.security.AdminActivityLogFilter
import com.example.data.security.AdminActivityLogPage
import com.example.data.security.AdminPermissionCatalog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ADMIN_ROLE_PROFILE সেশন ৫] অ্যাক্টিভিটি লগের pure লজিক (মডেল-পার্সিং, ফিল্টার-গঠন, লেবেল, ক্যাটালগ-ইনডেক্স)।
 * ⚠️ লেখার সময় Gradle/SDK ছিল না — কখনো চালানো হয়নি; প্রথম রানে পাস/ফেল যাচাই হবে।
 */
class AdminActivityLogLogicTest {

    private fun obj(json: String): JsonObject = Json.parseToJsonElement(json) as JsonObject

    private fun admin(id: String, name: String) = AdminAccountInfo(
        id = id, name = name, designation = "", phone = "01711111111", email = null, photoUrl = null, bio = "",
        roleId = "r", roleName = "Support", isSuper = false, permissions = emptySet(), active = true, flagged = false,
        lastLoginAt = null, lastLoginDevice = null, lastLoginIp = null, isOnline = false
    )

    @Test
    fun entry_parsesAttributedRow_andKeepsTimestampVerbatim() {
        val e = AdminActivityLogEntry.fromJson(
            obj(
                """{"id":"LOG_1_abc","action_type":"BAN_USER","target_id":"u1","target_name":"রহিম",
                   "details":"স্প্যাম","role":"USER","timestamp":"2026-09-24T17:59:26.144985+00:00",
                   "admin_id":"a1","admin_name":"সুমাইয়া","admin_role_name":"KYC"}"""
            )
        )
        assertNotNull(e)
        // মাইক্রোসেকেন্ডসহ হুবহু — পরের পেজের কার্সর হিসেবে ফেরত যায়, বদলালে সারি বাদ পড়ত
        assertEquals("2026-09-24T17:59:26.144985+00:00", e!!.timestamp)
        assertTrue(e.isAttributed)
        assertEquals("সুমাইয়া", activityActorLabel(e))
        assertEquals("KYC", e.adminRoleName)
    }

    @Test
    fun entry_legacyRow_hasNoAdmin_andShowsSystemLabel() {
        val e = AdminActivityLogEntry.fromJson(
            obj(
                """{"id":"LOG_2","action_type":"WALLET_DEPOSIT","target_id":"","target_name":"","details":"","role":"",
                   "timestamp":"2026-09-19T10:00:00+00:00","admin_id":null,"admin_name":"","admin_role_name":""}"""
            )
        )!!
        assertNull(e.adminId)
        assertFalse(e.isAttributed)
        assertEquals("সিস্টেম / লিগ্যাসি", activityActorLabel(e))
    }

    @Test
    fun entry_missingRequiredFields_returnsNull() {
        assertNull(AdminActivityLogEntry.fromJson(obj("""{"action_type":"X","timestamp":"t"}""")))
        assertNull(AdminActivityLogEntry.fromJson(obj("""{"id":"1","timestamp":"t"}""")))
        assertNull(AdminActivityLogEntry.fromJson(obj("""{"id":"1","action_type":"X"}""")))
    }

    @Test
    fun page_parsesRowsAndHasMore_andRejectsBadShape() {
        val page = AdminActivityLogPage.fromJson(
            Json.parseToJsonElement(
                """{"rows":[{"id":"1","action_type":"A","timestamp":"t1"},{"id":"2","action_type":"B","timestamp":"t2"},{"bad":true}],
                    "has_more":true}"""
            )
        )!!
        assertEquals(listOf("1", "2"), page.rows.map { it.id }) // ভাঙা সারি বাদ
        assertTrue(page.hasMore)

        assertNull(AdminActivityLogPage.fromJson(Json.parseToJsonElement("""[1,2]""")))
        assertNull(AdminActivityLogPage.fromJson(Json.parseToJsonElement("""{"has_more":false}""")))
        assertFalse(AdminActivityLogPage.fromJson(Json.parseToJsonElement("""{"rows":[]}"""))!!.hasMore)
    }

    @Test
    fun filter_build_unattributedDropsAdminAndName_otherwiseTrimsName() {
        assertEquals(
            AdminActivityLogFilter(unattributedOnly = true),
            buildActivityFilter(adminId = "a1", unattributedOnly = true, nameInput = "সুমাইয়া")
        )
        assertEquals(
            AdminActivityLogFilter(adminId = "a1", nameQuery = "সুমাইয়া"),
            buildActivityFilter(adminId = "a1", unattributedOnly = false, nameInput = "  সুমাইয়া ")
        )
        assertFalse(buildActivityFilter(null, false, "   ").isActive)
        assertTrue(buildActivityFilter(null, false, "x").isActive)
    }

    @Test
    fun filter_describe() {
        val admins = listOf(admin("a1", "সুমাইয়া"))
        assertEquals("সিস্টেম / লিগ্যাসি", describeActivityFilter(AdminActivityLogFilter(unattributedOnly = true), admins))
        assertEquals("সুমাইয়া", describeActivityFilter(AdminActivityLogFilter(adminId = "a1"), admins))
        assertEquals("নির্বাচিত এডমিন", describeActivityFilter(AdminActivityLogFilter(adminId = "gone"), admins))
        assertTrue(describeActivityFilter(AdminActivityLogFilter(nameQuery = "রা"), admins).contains("রা"))
    }

    @Test
    fun actionLabel_newCodes_legacyFallback_andUnknownPassthrough() {
        assertEquals("এডমিন লগইন", activityActionLabel("ADMIN_LOGIN"))
        assertTrue(activityActionLabel("ADMIN_FLAGGED").contains("ফ্ল্যাগ"))
        // বিদ্যমান লিগ্যাসি ম্যাপ থেকে
        assertTrue(activityActionLabel("BAN_USER").contains("Ban"))
        // একদম অজানা কোড — কোডটাই ফেরত (কিছু লুকানো/ভাঙা না)
        assertEquals("SOMETHING_NEW", activityActionLabel("SOMETHING_NEW"))
    }

    @Test
    fun roleScope_andErrorMessages() {
        assertNull(activityRoleScopeLabel(""))
        assertEquals("ইউজার প্রোফাইলে", activityRoleScopeLabel("user"))
        assertEquals("সলভার প্রোফাইলে", activityRoleScopeLabel("SOLVER"))
        assertTrue(activityLogErrorMessage("ERROR: SUPER_ADMIN_REQUIRED (P0001)").contains("সুপার"))
        assertEquals(activityLogErrorMessage("whatever"), activityLogErrorMessage(""))
    }

    @Test
    fun catalog_activityLogTab_is27_andIndexesStayUnique() {
        assertEquals(27, AdminPermissionCatalog.ACTIVITY_LOG_TAB_INDEX)
        val item = AdminPermissionCatalog.findItem("admin_mgmt", "activity_log")
        assertNotNull(item)
        assertEquals(27, item!!.tabIndex)
        val indexes = AdminPermissionCatalog.GROUPS.flatMap { g -> g.items.mapNotNull { it.tabIndex } }
        assertEquals(indexes.size, indexes.toSet().size)
    }
}
