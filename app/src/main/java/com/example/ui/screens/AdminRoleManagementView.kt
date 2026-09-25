package com.example.ui.screens

/**
 * [ADMIN_ROLE_PROFILE সেশন ৩ — অংশ ২.১ + ২.২] `AdminRoleManagementView` — রোল কার্ড গ্রিড + রোল এডিটর
 * (পারমিশন ট্রি, tri-state, "অ্যাকশন টিক → view অটো-টিক" লজিক) + লাইভ প্রিভিউ।
 *
 * রেফারেন্স: `admin-role-management.html` মকআপ (**শুধু ইন্টারঅ্যাকশন প্যাটার্নের জন্য**, এর `GROUPS`
 * ডেটা ব্যবহার করা হয়নি) + `AdminPermissionCatalog.kt` (আসল ডেটা সোর্স) +
 * `ADMIN_ROLE_PROFILE_PROGRESS.md`-এর "সেশন ৩" এন্ট্রিগুলো।
 *
 * ✅ **অংশ ২.২-এ যা ওয়্যার হয়েছে (এই সেশনে):**
 * - `SomadhanViewModel`: `adminRoles`/`adminRolesSyncPhase`/`loadAdminRoles()`/`adminSaveRole()`/
 *   `adminDeleteRole()` — সার্ভার-এরর [roleErrorMessage] দিয়ে বাংলা টোস্ট।
 * - `AdminPanelScreen.kt`: সুপার-অনলি "অ্যাডমিন ব্যবস্থাপনা" ড্রয়ার গ্রুপ (ইনডেক্স
 *   `AdminPermissionCatalog.ROLE_MGMT_TAB_INDEX` = ২৫) + `SyncAwareContent(sessionKey =
 *   "admin_role_mgmt_sync")` dispatch (নিচের `rememberFieldChangePulse`-এর একই sessionKey)।
 * - লাইভ প্রিভিউ ([AdminRolePreviewPanel], সুপার-অনলি — প্রশ্ন ২ কনফার্মড): রোল-কার্ডের "প্রিভিউ" বাটন
 *   (আলাদা পেইন) এবং এডিটরের ভেতরে collapsible প্রিভিউ (টিক দিলে সাথে সাথে বদলায়)।
 * - `onSaveRole` এখন `onDone: (Boolean) -> Unit` নেয় — সার্ভার ব্যর্থ করলে (যেমন নাম আগে থেকেই আছে)
 *   এডিটর খোলা থাকে, টিক-করা পারমিশন হারায় না (২.১-এ fire-and-forget ছিল, সেখানে এডিটর সাথে সাথে
 *   বন্ধ হয়ে ব্যর্থতায় সব কাজ হারাতো)। `onDeleteRole` আগের মতোই fire-and-forget (ডিলিটে হারানোর কিছু নেই)।
 *
 * ⚠️ **এখনো যা যাচাই হয়নি:** Gradle/SDK না থাকায় কিছুই কম্পাইল/রান করা হয়নি (সেশন ১-২-এর মতোই)।
 * কোড বিদ্যমান `AdminCategoriesView.kt`/`AdminFaqManagementView.kt`/`AdminPanelScreen.kt`-এর কনভেনশন
 * গ্রেপ করে মিলিয়ে লেখা — Android Studio-তে প্রথম বিল্ডেই কম্পাইল-ত্রুটি (থাকলে) ধরা পড়বে।
 *
 * **২.১-এর কাজ (অপরিবর্তিত):**
 * - রোল কার্ড গ্রিড — per-item pulse (`rememberFieldChangePulse`, sessionKey `"admin_role_mgmt_sync"`)।
 * - রোল এডিটর — গ্রুপ → আইটেম → অ্যাকশন tri-state ট্রি, "সব নির্বাচন" গ্রুপ/আইটেম চেকবক্স,
 *   **অ্যাকশন টিক দিলে 'view' অটো-টিক, 'view' untick করলে বাকি সব untick** (প্রশ্ন ৩ কনফার্মড)।
 * - সুপার রোল এডিট/ডিলিট UI-তে লকড — DB-trigger/RPC গার্ড সেশন ১ থেকেই আছে, এটা শুধু UI-প্রতিফলন।
 * - ডিলিট কনফার্মেশনে account_count > 0 হলে সতর্কবার্তা + কনফার্ম বাটন disabled।
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.AdminPermissionCatalog
import com.example.data.security.AdminPermissionGroup
import com.example.data.security.AdminPermissionItem
import com.example.data.security.AdminRoleInfo
import com.example.ui.components.BottomSlideAlertDialog
import com.example.ui.components.PulsingValue
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel

/** সার্ভারের raise-করা কোড (RPC exception message) → বাংলা বার্তা। `SomadhanViewModel`-এর
 * `adminSaveRole`/`adminDeleteRole` এটা কল করে টোস্ট দেখায়। */
fun roleErrorMessage(raw: String): String = when {
    raw.contains("ROLE_NAME_REQUIRED") -> "রোলের নাম আবশ্যক।"
    raw.contains("ROLE_NAME_TAKEN") -> "এই নামে আরেকটা রোল আগে থেকেই আছে।"
    raw.contains("ROLE_NOT_FOUND") -> "রোলটা খুঁজে পাওয়া যায়নি (হয়তো অন্য কোথাও থেকে মুছে ফেলা হয়েছে)।"
    raw.contains("SUPER_ROLE_IMMUTABLE") -> "সুপার অ্যাডমিন রোল এডিট বা মুছে ফেলা যায় না।"
    raw.contains("ROLE_IN_USE") -> "এই রোলে এখনো এডমিন অ্যাকাউন্ট অ্যাসাইনড আছে, তাই মুছে ফেলা যাবে না।"
    raw.contains("PERMISSIONS_MUST_BE_ARRAY") || raw.contains("PERMISSIONS_MUST_BE_STRINGS") ->
        "পারমিশন তালিকায় সমস্যা হয়েছে, আবার চেষ্টা করুন।"
    raw.contains("SUPER_ADMIN_REQUIRED") || raw.contains("AUTH_REQUIRED") ->
        "শুধু সুপার অ্যাডমিন এই কাজ করতে পারবেন।"
    else -> "কিছু একটা সমস্যা হয়েছে, আবার চেষ্টা করুন।"
}

// ============================================================
// পারমিশন-সেট পিওর হেল্পার (Compose-নির্ভরতা নেই, নিজেরাই টেস্টযোগ্য)
// ============================================================

private fun itemActionKeys(groupId: String, item: AdminPermissionItem): List<String> =
    item.actions.map { "$groupId:${item.id}:${it.id}" }

private fun groupActionKeys(group: AdminPermissionGroup): List<String> =
    group.items.flatMap { itemActionKeys(group.id, it) }

private fun itemFull(groupId: String, item: AdminPermissionItem, perms: Set<String>): Boolean =
    itemActionKeys(groupId, item).all { it in perms }

private fun itemPartial(groupId: String, item: AdminPermissionItem, perms: Set<String>): Boolean {
    val keys = itemActionKeys(groupId, item)
    return keys.any { it in perms } && !keys.all { it in perms }
}

private fun groupFull(group: AdminPermissionGroup, perms: Set<String>): Boolean =
    group.items.all { itemFull(group.id, it, perms) }

private fun groupPartial(group: AdminPermissionGroup, perms: Set<String>): Boolean {
    val anySelected = group.items.any { item -> itemActionKeys(group.id, item).any { it in perms } }
    return anySelected && !groupFull(group, perms)
}

/** অ্যাকশন টিক/আনটিক — [ADMIN_ROLE_PROFILE সেশন ৩ অংশ ১, নতুন-প্রশ্ন ৩ কনফার্মড]:
 * যেকোনো non-view অ্যাকশন টিক দিলে সেই আইটেমের 'view' অটো-টিক হয়ে যায় (view ছাড়া অ্যাকশন অর্থহীন)।
 * 'view' আনটিক করলে সেই আইটেমের বাকি সব অ্যাকশনও আনটিক হয় (উল্টোটা না — এটাই ইচ্ছাকৃত অসাম্যতা)। */
private fun toggleAction(perms: Set<String>, groupId: String, item: AdminPermissionItem, actionId: String): Set<String> {
    val key = "$groupId:${item.id}:$actionId"
    return if (actionId == "view") {
        if (key in perms) perms - itemActionKeys(groupId, item).toSet() else perms + key
    } else {
        if (key in perms) perms - key else perms + key + "$groupId:${item.id}:view"
    }
}

private fun toggleItemAll(perms: Set<String>, groupId: String, item: AdminPermissionItem): Set<String> {
    val keys = itemActionKeys(groupId, item).toSet()
    return if (keys.all { it in perms }) perms - keys else perms + keys
}

private fun toggleGroupAll(perms: Set<String>, group: AdminPermissionGroup): Set<String> {
    val keys = groupActionKeys(group).toSet()
    return if (keys.all { it in perms }) perms - keys else perms + keys
}

// ============================================================
// টপ-লেভেল স্ক্রিন
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRoleManagementView(
    roles: List<AdminRoleInfo>,
    onSaveRole: (id: String?, name: String, permissions: Set<String>, onDone: (Boolean) -> Unit) -> Unit,
    onDeleteRole: (AdminRoleInfo) -> Unit,
    viewModel: SomadhanViewModel? = null,
    isManualRefreshing: Boolean = false,
    // তালিকা একবারও আনা যায়নি (সার্ভার/নেটওয়ার্ক ব্যর্থ) — তখন খালি-তালিকার জায়গায় এরর + retry দেখায়,
    // "এখনো কোনো রোল নেই" নয় (নেই আর আনা যায়নি — দুটো আলাদা অবস্থা)।
    loadFailed: Boolean = false,
    onRetry: () -> Unit = {}
) {
    var editingRole by remember { mutableStateOf<AdminRoleInfo?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var roleToDelete by remember { mutableStateOf<AdminRoleInfo?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    // প্রিভিউ-পেইনে কোন রোল দেখা হচ্ছে (id ধরে রাখা — রিফ্রেশে রোলের পারমিশন বদলালে ফ্রেশ কপি খুঁজে নেওয়া হয়)
    var previewRoleId by remember { mutableStateOf<String?>(null) }
    val previewRole = roles.find { it.id == previewRoleId }

    // sessionKey "admin_role_mgmt_sync" — অংশ ২.২-এ AdminPanelScreen.kt-এর ইনডেক্স ২৫-এর
    // SyncAwareContent cold-load gate-এর সাথে এই একই স্ট্রিং শেয়ার করতে হবে (AdminCategoriesView-এর
    // "admin_categories_sync" প্যাটার্ন অনুসরণ করে)।
    val rolesPulse = rememberFieldChangePulse(
        value = roles,
        isManualRefreshing = isManualRefreshing,
        sessionKey = "admin_role_mgmt_sync",
        viewModel = viewModel
    )

    if (showEditor) {
        RoleEditorPane(
            role = if (isCreatingNew) null else editingRole,
            isSaving = isSaving,
            onCancel = {
                showEditor = false
                editingRole = null
            },
            onSave = { name, perms ->
                if (!isSaving) {
                    isSaving = true
                    onSaveRole(if (isCreatingNew) null else editingRole?.id, name, perms) { ok ->
                        isSaving = false
                        // সফল হলেই বন্ধ; ব্যর্থ হলে (টোস্টে কারণ দেখানো হয়েছে) এডিটর খোলা — কাজ হারায় না।
                        if (ok) {
                            showEditor = false
                            editingRole = null
                        }
                    }
                }
            }
        )
    } else if (previewRole != null) {
        RolePreviewPane(role = previewRole, onBack = { previewRoleId = null })
    } else {
        RoleListPane(
            roles = roles,
            pulse = rolesPulse,
            loadFailed = loadFailed,
            onRetry = onRetry,
            onPreviewClick = { role -> previewRoleId = role.id },
            onAddClick = {
                editingRole = null
                isCreatingNew = true
                showEditor = true
            },
            onEditClick = { role ->
                editingRole = role
                isCreatingNew = false
                showEditor = true
            },
            onDeleteClick = { role -> roleToDelete = role }
        )
    }

    roleToDelete?.let { r ->
        BottomSlideAlertDialog(
            onDismissRequest = { roleToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = SomadhanError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("রোল মুছে ফেলুন", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Column {
                    Text("\"${r.name}\" রোলটা স্থায়ীভাবে মুছে ফেলতে চান?", color = SomadhanTextSecondary)
                    if (r.accountCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ এই মুহূর্তে ${r.accountCount} জন এডমিন এই রোলে আছেন — সার্ভার এই রোল মুছতে দেবে না, " +
                                "আগে তাদের অন্য কোনো রোলে সরাতে হবে।",
                            color = SomadhanError,
                            fontSize = 12.5.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRole(r)
                        roleToDelete = null
                    },
                    enabled = r.accountCount == 0
                ) {
                    Text("মুছুন", color = if (r.accountCount == 0) SomadhanError else SomadhanTextHint)
                }
            },
            dismissButton = {
                TextButton(onClick = { roleToDelete = null }) { Text("বাতিল") }
            }
        )
    }
}

// ============================================================
// রোল লিস্ট (কার্ড গ্রিড)
// ============================================================

@Composable
private fun RoleListPane(
    roles: List<AdminRoleInfo>,
    pulse: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    onPreviewClick: (AdminRoleInfo) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (AdminRoleInfo) -> Unit,
    onDeleteClick: (AdminRoleInfo) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(SomadhanBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("রোল ম্যানেজমেন্ট", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                Text("${roles.size} টা রোল", fontSize = 12.sp, color = SomadhanTextSecondary)
            }
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("নতুন রোল")
            }
        }

        if (roles.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (loadFailed) {
                    Text(
                        "রোলের তালিকা আনা যায়নি। ইন্টারনেট সংযোগ দেখে আবার চেষ্টা করুন।",
                        color = SomadhanTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onRetry) { Text("আবার চেষ্টা করুন") }
                } else {
                    Text("এখনো কোনো রোল নেই।", color = SomadhanTextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(roles, key = { it.id }) { role ->
                    PulsingValue(isUpdating = pulse) {
                        RoleCard(
                            role = role,
                            onPreviewClick = { onPreviewClick(role) },
                            onEditClick = { onEditClick(role) },
                            onDeleteClick = { onDeleteClick(role) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun RoleCard(
    role: AdminRoleInfo,
    onPreviewClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val totalNonSuper = remember { AdminPermissionCatalog.nonSuperKeys().size }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        border = BorderStroke(1.dp, if (role.isSuper) SomadhanOrange else SomadhanBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = role.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (role.isSuper) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (role.isSuper) {
                    "সব পারমিশন — অপরিবর্তনীয়"
                } else {
                    "${role.permissions.count { !it.startsWith("admin_mgmt:") }}/$totalNonSuper ফাংশন"
                },
                fontSize = 12.sp,
                color = SomadhanTextSecondary
            )
            Text("${role.accountCount} জন এডমিন এই রোলে আছেন", fontSize = 12.sp, color = SomadhanTextSecondary)

            Spacer(modifier = Modifier.height(10.dp))
            // প্রিভিউ সব রোলেই (সুপারসহ) — শুধু দেখা, কোনো পরিবর্তন না।
            OutlinedButton(onClick = onPreviewClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("লাইভ প্রিভিউ দেখুন", fontSize = 13.sp)
            }
            if (!role.isSuper) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEditClick, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("এডিট", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("মুছুন", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ============================================================
// রোল প্রিভিউ-পেইন (রোল-লিস্ট থেকে, শুধু-দেখা)
// ============================================================

@Composable
private fun RolePreviewPane(role: AdminRoleInfo, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(SomadhanBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান", tint = SomadhanTextPrimary)
            }
            Column {
                Text("লাইভ প্রিভিউ", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                Text(role.name, fontSize = 12.sp, color = SomadhanTextSecondary)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            AdminRolePreviewPanel(name = role.name, permissions = role.permissions, isSuper = role.isSuper)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ============================================================
// রোল এডিটর (নাম + পারমিশন ট্রি)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleEditorPane(
    role: AdminRoleInfo?,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSave: (name: String, permissions: Set<String>) -> Unit
) {
    var name by remember(role?.id) { mutableStateOf(role?.name ?: "") }
    var perms by remember(role?.id) { mutableStateOf(role?.permissions ?: emptySet()) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    val openGroups = remember { mutableStateMapOf<String, Boolean>() }
    val openItems = remember { mutableStateMapOf<String, Boolean>() }

    val totalNonSuper = remember { AdminPermissionCatalog.nonSuperKeys().size }
    val selCount = perms.count { !it.startsWith("admin_mgmt:") }

    Column(modifier = Modifier.fillMaxSize().background(SomadhanBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "বাতিল", tint = SomadhanTextPrimary)
            }
            Text(
                text = if (role == null) "নতুন রোল" else "রোল এডিট করুন",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text("রোলের নাম", fontSize = 12.sp, color = SomadhanTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                placeholder = { Text("যেমন: মডারেশন টিম") },
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it, color = SomadhanError) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "প্রতিটা মেনুর নিচে তার নির্দিষ্ট ফাংশনগুলো আলাদাভাবে টিক দিন। \"দেখুন\" ছাড়া মেনুটাই মেনু-তালিকায় দেখাবে না।",
                fontSize = 11.5.sp,
                color = SomadhanTextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // লাইভ প্রিভিউ (collapsible) — টিক দেওয়ার সাথে সাথে বদলায়; ডিফল্টে বন্ধ যাতে ট্রি সহজে নাগালে থাকে।
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, SomadhanBorder), RoundedCornerShape(10.dp))
                    .clickable { showPreview = !showPreview }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "লাইভ প্রিভিউ — এই রোলের অ্যাডমিন যা দেখবে",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SomadhanTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (showPreview) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = SomadhanTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (showPreview) {
                Spacer(modifier = Modifier.height(8.dp))
                AdminRolePreviewPanel(
                    name = name.trim().ifEmpty { "নতুন রোল" },
                    permissions = perms,
                    isSuper = false
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            AdminPermissionCatalog.GROUPS.forEach { group ->
                PermissionGroupBlock(
                    group = group,
                    perms = perms,
                    isOpen = openGroups[group.id] == true,
                    onToggleOpen = { openGroups[group.id] = !(openGroups[group.id] ?: false) },
                    openItems = openItems,
                    onToggleItemOpen = { itemKey -> openItems[itemKey] = !(openItems[itemKey] ?: false) },
                    onToggleGroupAll = { perms = toggleGroupAll(perms, group) },
                    onToggleItemAll = { item -> perms = toggleItemAll(perms, group.id, item) },
                    onToggleAction = { item, actionId -> perms = toggleAction(perms, group.id, item, actionId) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, SomadhanDivider))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("নির্বাচিত: $selCount/$totalNonSuper ফাংশন", fontSize = 12.sp, color = SomadhanTextSecondary)
            Row {
                TextButton(onClick = onCancel, enabled = !isSaving) { Text("বাতিল") }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isEmpty()) {
                            nameError = "রোলের নাম দিন।"
                        } else {
                            onSave(trimmed, perms)
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange)
                ) { Text(if (isSaving) "সংরক্ষণ হচ্ছে…" else "সংরক্ষণ করুন") }
            }
        }
    }
}

@Composable
private fun PermissionGroupBlock(
    group: AdminPermissionGroup,
    perms: Set<String>,
    isOpen: Boolean,
    onToggleOpen: () -> Unit,
    openItems: Map<String, Boolean>,
    onToggleItemOpen: (String) -> Unit,
    onToggleGroupAll: () -> Unit,
    onToggleItemAll: (AdminPermissionItem) -> Unit,
    onToggleAction: (AdminPermissionItem, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        border = BorderStroke(1.dp, SomadhanBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        if (group.superOnly) {
            // মকআপের "locked-group" (🔒) সংস্করণ — এই গ্রুপ কোনো রোলে বরাদ্দযোগ্য না, শুধু সুপার
            // অ্যাডমিনের জন্য অন্তর্নিহিতভাবে বরাদ্দ (AdminSessionState.isSuper == true)।
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = SomadhanTextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(group.title, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary, fontSize = 14.sp)
                    Text("শুধু সুপার অ্যাডমিন — কোনো রোলে বরাদ্দযোগ্য নয়", fontSize = 11.sp, color = SomadhanTextSecondary)
                }
            }
        } else {
            val gFull = groupFull(group, perms)
            val gPartial = groupPartial(group, perms)
            val groupState = when {
                gFull -> ToggleableState.On
                gPartial -> ToggleableState.Indeterminate
                else -> ToggleableState.Off
            }
            val selInGroup = groupActionKeys(group).count { it in perms }
            val totalInGroup = groupActionKeys(group).size

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TriStateCheckbox(state = groupState, onClick = onToggleGroupAll)
                    Column(modifier = Modifier.weight(1f).clickable { onToggleOpen() }) {
                        Text(group.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SomadhanTextPrimary)
                        Text("$selInGroup/$totalInGroup নির্বাচিত", fontSize = 11.sp, color = SomadhanTextSecondary)
                    }
                    Icon(
                        imageVector = if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = SomadhanTextSecondary,
                        modifier = Modifier.size(20.dp).clickable { onToggleOpen() }
                    )
                }
                if (isOpen) {
                    Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
                        group.items.forEach { item ->
                            val itemKey = "${group.id}:${item.id}"
                            PermissionItemBlock(
                                groupId = group.id,
                                item = item,
                                perms = perms,
                                isOpen = openItems[itemKey] == true,
                                onToggleOpen = { onToggleItemOpen(itemKey) },
                                onToggleItemAll = { onToggleItemAll(item) },
                                onToggleAction = { actionId -> onToggleAction(item, actionId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItemBlock(
    groupId: String,
    item: AdminPermissionItem,
    perms: Set<String>,
    isOpen: Boolean,
    onToggleOpen: () -> Unit,
    onToggleItemAll: () -> Unit,
    onToggleAction: (String) -> Unit
) {
    val keys = itemActionKeys(groupId, item)
    val iFull = itemFull(groupId, item, perms)
    val iPartial = itemPartial(groupId, item, perms)
    val itemState = when {
        iFull -> ToggleableState.On
        iPartial -> ToggleableState.Indeterminate
        else -> ToggleableState.Off
    }
    val selInItem = keys.count { it in perms }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SomadhanBg)
            .border(BorderStroke(1.dp, SomadhanDivider), RoundedCornerShape(8.dp))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TriStateCheckbox(state = itemState, onClick = onToggleItemAll)
            Text(
                text = item.label,
                fontSize = 13.sp,
                color = SomadhanTextPrimary,
                modifier = Modifier.weight(1f).clickable { onToggleOpen() }
            )
            Text("$selInItem/${keys.size}", fontSize = 11.sp, color = SomadhanTextSecondary)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = SomadhanTextSecondary,
                modifier = Modifier.size(18.dp).clickable { onToggleOpen() }
            )
        }
        if (isOpen) {
            Column(modifier = Modifier.padding(start = 30.dp, end = 10.dp, bottom = 8.dp)) {
                item.actions.forEach { action ->
                    val key = "$groupId:${item.id}:${action.id}"
                    val checked = key in perms
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleAction(action.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { onToggleAction(action.id) })
                        Text(
                            text = action.label + if (action.id == "view") " (এই মেনু মোটেও দেখা যাবে কিনা)" else "",
                            fontSize = 12.5.sp,
                            fontWeight = if (action.id == "view") FontWeight.SemiBold else FontWeight.Normal,
                            color = SomadhanTextPrimary
                        )
                    }
                }
            }
        }
    }
}
