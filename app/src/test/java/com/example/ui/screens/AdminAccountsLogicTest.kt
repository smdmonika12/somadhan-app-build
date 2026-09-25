package com.example.ui.screens

import com.example.data.security.AdminAccountInfo
import com.example.data.security.AdminPermissionCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ADMIN_ROLE_PROFILE সেশন ৪] অ্যাকাউন্ট-হেল্পারের pure লজিক।
 * ⚠️ লেখার সময় Gradle/SDK ছিল না — কখনো চালানো হয়নি; প্রথম রানে পাস/ফেল যাচাই হবে।
 */
class AdminAccountsLogicTest {

    private fun acct(name: String, phone: String, role: String = "Support", designation: String = "") = AdminAccountInfo(
        id = name, name = name, designation = designation, phone = phone, email = null,
        photoUrl = null, bio = "", roleId = role.lowercase(), roleName = role, isSuper = false,
        permissions = emptySet(), active = true, flagged = false, lastLoginAt = null,
        lastLoginDevice = null, lastLoginIp = null, isOnline = false
    )

    @Test
    fun phone_normalizesAllCommonForms() {
        assertEquals("01712345678", normalizeAdminPhoneInput("01712345678"))
        assertEquals("01712345678", normalizeAdminPhoneInput("+880 1712-345678"))
        assertEquals("01712345678", normalizeAdminPhoneInput("8801712345678"))
        assertEquals("01712345678", normalizeAdminPhoneInput("1712345678"))
    }

    @Test
    fun phone_validation() {
        assertTrue(isValidAdminPhone("01712345678"))
        assertFalse(isValidAdminPhone("1712345678"))
        assertFalse(isValidAdminPhone("0171234567"))
        assertFalse(isValidAdminPhone("02712345678"))
        assertFalse(isValidAdminPhone(""))
    }

    @Test
    fun errorMessages_mapKnownServerCodes() {
        assertTrue(accountErrorMessage("PHONE_IN_USE_BY_USER").contains("সাধারণ ইউজার"))
        assertTrue(accountErrorMessage("ERROR: ADMIN_PHONE_TAKEN (P0001)").contains("আগে থেকেই"))
        assertTrue(accountErrorMessage("SUPER_ADMIN_IMMUTABLE").contains("সুপার"))
        assertEquals(accountErrorMessage("something else"), accountErrorMessage(""))
    }

    @Test
    fun filter_matchesNamePhoneRoleAndBlankReturnsAll() {
        val list = listOf(acct("রহিম", "01711111111", "Support"), acct("করিম", "01822222222", "Finance"))
        assertEquals(2, filterAdminAccounts(list, "  ").size)
        assertEquals(listOf("করিম"), filterAdminAccounts(list, "finance").map { it.name })
        assertEquals(listOf("রহিম"), filterAdminAccounts(list, "+880 1711111111").map { it.name })
        assertEquals(listOf("করিম"), filterAdminAccounts(list, "করিম").map { it.name })
        assertTrue(filterAdminAccounts(list, "zzz").isEmpty())
    }

    @Test
    fun initials_andTimestampFallbacks() {
        assertEquals("আর", adminInitials("আব্দুর রহিম খান"))
        assertEquals("?", adminInitials("   "))
        assertEquals("—", formatAdminTimestamp(null))
        assertEquals("not-a-date", formatAdminTimestamp("not-a-date"))
    }

    @Test
    fun catalog_accountsTabIndex_is26_andUnique() {
        assertEquals(26, AdminPermissionCatalog.ADMIN_ACCOUNTS_TAB_INDEX)
        val indexes = AdminPermissionCatalog.GROUPS.flatMap { g -> g.items.mapNotNull { it.tabIndex } }
        assertEquals(indexes.size, indexes.toSet().size)
        assertTrue(indexes.contains(26))
    }

    @Test
    fun errorMessages_invalidPassword_mentionsServerByteLimit() {
        val msg = accountErrorMessage("INVALID_PASSWORD")
        assertTrue(msg.contains("৮"))
        assertTrue(msg.contains("৭২"))
    }

    // per-item pulse (AdminAccountsView) `account.copy(lastSeenAt = null)` তুলনা করে: শুধু heartbeat-এর
    // lastSeenAt বদলালে সমান থাকতেই হবে (নইলে প্রতি ২০সে পোলে সব কার্ড ঝলকাতো); অনলাইন অবস্থা বদলালে অসমান।
    @Test
    fun pulseKey_ignoresLastSeenAt_butNotOnlineOrFlagChanges() {
        val base = acct("রহিম", "01711111111")
        val heartbeatOnly = base.copy(lastSeenAt = "2026-09-25T10:00:00Z")
        assertEquals(base.copy(lastSeenAt = null), heartbeatOnly.copy(lastSeenAt = null))
        assertFalse(base.copy(lastSeenAt = null) == base.copy(isOnline = true, lastSeenAt = null))
        assertFalse(base.copy(lastSeenAt = null) == base.copy(flagged = true, lastSeenAt = null))
    }
}
