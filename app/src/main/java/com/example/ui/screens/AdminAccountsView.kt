package com.example.ui.screens

/**
 * [ADMIN_ROLE_PROFILE সেশন ৪ — UI সম্পন্ন] `AdminAccountsView` — এডমিন অ্যাকাউন্ট তালিকা + নতুন এডমিন তৈরি +
 * রোল বদল + পাসওয়ার্ড রিসেট + ফ্ল্যাগ/আনফ্ল্যাগ + সক্রিয়/নিষ্ক্রিয় + অ্যাকাউন্ট-ভিত্তিক লাইভ প্রিভিউ।
 *
 * রেফারেন্স: `admin-role-management.html`-এর "এডমিন অ্যাকাউন্ট" ট্যাব (`renderAccounts`, শুধু ইন্টারঅ্যাকশন/লেআউট),
 * কনভেনশন: `AdminRoleManagementView.kt` (per-item pulse, BottomSlideAlertDialog, Somadhan* রঙ)।
 * ব্যাকএন্ড (ViewModel/RPC) আগের চেকপয়েন্টেই তৈরি — এই ফাইলের নিচের অংশ (হেল্পার) তারই সহায়ক।
 *
 * - loading: প্রতিটা কার্ড per-item `rememberFieldChangePulse` (`flashOnReentry = false`); heartbeat-এ প্রতি
 *   পোলে বদলানো `lastSeenAt` তুলনা থেকে বাদ (নইলে ২০সে পরপর সব কার্ড ঝলকাতো)। সার্চ বদলালে দৃশ্যমান সব কার্ড
 *   pulse (Ground Rule ২০, `AdminUsersView`-এর `isFilterRefreshing` প্যাটার্ন)। sessionKey = "admin_accounts_sync"
 *   (AdminPanelScreen-এর SyncAwareContent-এর সাথে হুবহু এক)।
 * - সব মিউটেটিং কলব্যাকে শেষে `onDone: (Boolean) -> Unit` — সফল হলেই ফর্ম/ডায়ালগ বন্ধ, ব্যর্থ হলে খোলা (ইনপুট হারায় না)।
 * - `TEMP-SESSION7` চিহ্নিত সতর্কতা-টেক্সটগুলো সেশন ৭ শেষে মুছতে হবে (গ্রেপ করে খুঁজে নাও)।
 *
 * ⚠️ কম্পাইল/রান করা হয়নি (এই এনভায়রনমেন্টে Gradle/SDK নেই) — Android Studio-তে প্রথম বিল্ডে ধরা পড়বে।
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.AdminAccountInfo
import com.example.data.security.AdminRoleInfo
import com.example.ui.components.BottomSlideAlertDialog
import com.example.ui.components.PulsingValue
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.theme.SomadhanAdminSlateLight
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeContainer
import com.example.ui.theme.SomadhanOrangePressed
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import kotlinx.coroutines.delay

/**
 * সার্ভার RPC-র raise-করা কোড (exception message-এ থাকে) → বাংলা বার্তা।
 * `contains` দিয়ে মেলানো হয় (postgrest message-এ কোডের আগে-পরে লেখা থাকে)।
 */
fun accountErrorMessage(raw: String): String = when {
    raw.contains("INVALID_NAME") -> "নাম দিন (সর্বোচ্চ ৮০ অক্ষর)।"
    raw.contains("INVALID_PHONE") -> "সঠিক ফোন নম্বর দিন (যেমন: 01712345678)।"
    raw.contains("INVALID_PASSWORD") -> "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে (সর্বোচ্চ ৭২ বাইট)।"
    raw.contains("INVALID_DESIGNATION") -> "পদবি সর্বোচ্চ ৮০ অক্ষরের হতে পারবে।"
    raw.contains("INVALID_EMAIL") -> "ইমেইল ঠিকানাটা সঠিক নয়।"
    raw.contains("CANNOT_ASSIGN_SUPER_ROLE") -> "সুপার অ্যাডমিন রোল কাউকে দেওয়া যায় না।"
    raw.contains("ROLE_NOT_FOUND") -> "রোলটা খুঁজে পাওয়া যায়নি (হয়তো অন্য কোথাও থেকে মুছে ফেলা হয়েছে)।"
    raw.contains("ADMIN_PHONE_TAKEN") -> "এই ফোন নম্বরে আরেকটা এডমিন অ্যাকাউন্ট আগে থেকেই আছে।"
    raw.contains("PHONE_IN_USE_BY_USER") ->
        "এই নম্বরটা একজন সাধারণ ইউজার/সলভারের অ্যাকাউন্টে ব্যবহৃত — এডমিন বানানো যাবে না। অন্য নম্বর দিন।"
    raw.contains("USER_ROW_MISSING") -> "অ্যাকাউন্ট তৈরিতে সমস্যা হয়েছে, আবার চেষ্টা করুন।"
    raw.contains("ACCOUNT_NOT_FOUND") -> "অ্যাকাউন্টটা খুঁজে পাওয়া যায়নি।"
    raw.contains("ACCOUNT_HAS_NO_LOGIN") -> "এই অ্যাকাউন্টের কোনো লগইন নেই।"
    raw.contains("SUPER_ADMIN_IMMUTABLE") -> "সুপার অ্যাডমিন অ্যাকাউন্ট নিষ্ক্রিয়, ফ্ল্যাগ বা এডিট করা যায় না।"
    raw.contains("CANNOT_CHANGE_SELF") -> "নিজের অ্যাকাউন্ট নিজে বদলানো যায় না।"
    raw.contains("SUPER_ADMIN_REQUIRED") || raw.contains("AUTH_REQUIRED") -> "শুধু সুপার অ্যাডমিন এই কাজ করতে পারবেন।"
    else -> "কাজটা সম্পন্ন করা যায়নি। ইন্টারনেট দেখে আবার চেষ্টা করুন।"
}

/** ইনপুট থেকে অঙ্ক বের করে `01XXXXXXXXX` রূপে আনে (+880…, 880…, 1XXXXXXXXX সব গ্রহণ)। সার্ভারের `_admin_norm_phone`-এর মতো। */
fun normalizeAdminPhoneInput(input: String): String {
    val digits = input.filter { it.isDigit() }
    return when {
        digits.length == 13 && digits.startsWith("880") -> "0" + digits.substring(3)
        digits.length == 10 && digits.startsWith("1") -> "0$digits"
        else -> digits
    }
}

/** বৈধ বাংলাদেশি মোবাইল = `01` দিয়ে শুরু, মোট ১১ অঙ্ক (সার্ভারের `^01[0-9]{9}$`-এর সমান)। */
fun isValidAdminPhone(normalized: String): Boolean =
    normalized.length == 11 && normalized.startsWith("01") && normalized.all { it.isDigit() }

/** নাম/ফোন/রোল/পদবি — যেকোনোটায় [query] থাকলে মেলে (কেস-ইনসেনসিটিভ); ফাঁকা query = সব। */
fun filterAdminAccounts(accounts: List<AdminAccountInfo>, query: String): List<AdminAccountInfo> {
    val q = query.trim()
    if (q.isEmpty()) return accounts
    val qDigits = normalizeAdminPhoneInput(q)
    return accounts.filter { a ->
        a.name.contains(q, ignoreCase = true) ||
            a.roleName.contains(q, ignoreCase = true) ||
            a.designation.contains(q, ignoreCase = true) ||
            a.phone.contains(q) ||
            (qDigits.length >= 3 && a.phone.contains(qDigits))
    }
}

/** অ্যাভাটারের আদ্যক্ষর — নামের প্রথম দুই শব্দের প্রথম অক্ষর; নাম ফাঁকা হলে "?"। */
fun adminInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    return parts.take(2).joinToString("") { it.take(1) }
}

/** সার্ভারের ISO টাইমস্ট্যাম্প → ডিভাইসের টাইমজোনে "dd/MM/yyyy hh:mm a"; null/পার্স-ব্যর্থ হলে "—" / মূল টেক্সট। */
fun formatAdminTimestamp(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        val zoned = java.time.OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault())
        zoned.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a"))
    } catch (e: Exception) {
        iso
    }
}

// ============================================================
// টপ-লেভেল স্ক্রিন
// ============================================================

/** ফ্ল্যাগ/সক্রিয় ডায়ালগ খোলার মুহূর্তের লক্ষ্য-মান ধরে রাখে — সফল হয়ে তালিকা রিফ্রেশ হলে (ডায়ালগ বন্ধ হওয়ার
 * আগের ফ্রেমে) শিরোনাম উল্টে না যায়। */
private data class PendingToggle(val accountId: String, val target: Boolean)

@Composable
fun AdminAccountsView(
    accounts: List<AdminAccountInfo>,
    roles: List<AdminRoleInfo>,
    currentAdminId: String?,
    onCreate: (
        name: String, phone: String, password: String, roleId: String,
        designation: String, email: String, onDone: (Boolean) -> Unit
    ) -> Unit,
    onChangeRole: (account: AdminAccountInfo, roleId: String, onDone: (Boolean) -> Unit) -> Unit,
    onSetActive: (account: AdminAccountInfo, active: Boolean, reason: String, onDone: (Boolean) -> Unit) -> Unit,
    onSetFlagged: (account: AdminAccountInfo, flagged: Boolean, reason: String, onDone: (Boolean) -> Unit) -> Unit,
    onResetPassword: (account: AdminAccountInfo, newPassword: String, onDone: (Boolean) -> Unit) -> Unit,
    viewModel: SomadhanViewModel? = null,
    isManualRefreshing: Boolean = false,
    // তালিকা একবারও আনা যায়নি — তখন খালি-তালিকার জায়গায় এরর + retry (রোল-ট্যাবের মতোই)।
    loadFailed: Boolean = false,
    onRetry: () -> Unit = {}
) {
    var showCreate by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var previewAccountId by remember { mutableStateOf<String?>(null) }
    var roleDialogId by remember { mutableStateOf<String?>(null) }
    var passwordDialogId by remember { mutableStateOf<String?>(null) }
    var flagDialog by remember { mutableStateOf<PendingToggle?>(null) }
    var activeDialog by remember { mutableStateOf<PendingToggle?>(null) }
    var query by remember { mutableStateOf("") }

    // ডায়ালগ/প্রিভিউ আইডি ধরে রাখা হয় — রিফ্রেশে অ্যাকাউন্টের ডেটা বদলালে তালিকা থেকে তাজা কপি খুঁজে নেওয়া হয়।
    fun byId(id: String?): AdminAccountInfo? = if (id == null) null else accounts.find { it.id == id }
    val assignableRoles = roles.filter { !it.isSuper }

    val previewAccount = byId(previewAccountId)
    if (showCreate) {
        AccountCreatePane(
            roles = assignableRoles,
            isSaving = isCreating,
            onCancel = { showCreate = false },
            onSubmit = { name, phone, password, roleId, designation, email ->
                if (!isCreating) {
                    isCreating = true
                    onCreate(name, phone, password, roleId, designation, email) { ok ->
                        isCreating = false
                        // সফল হলেই বন্ধ; ব্যর্থ হলে (টোস্টে কারণ) ফর্ম খোলা — ইনপুট হারায় না।
                        if (ok) showCreate = false
                    }
                }
            }
        )
    } else if (previewAccount != null) {
        AccountPreviewPane(account = previewAccount, onBack = { previewAccountId = null })
    } else {
        AccountListPane(
            accounts = accounts,
            query = query,
            onQueryChange = { query = it },
            currentAdminId = currentAdminId,
            loadFailed = loadFailed,
            onRetry = onRetry,
            isManualRefreshing = isManualRefreshing,
            viewModel = viewModel,
            onAddClick = { showCreate = true },
            onPreview = { previewAccountId = it.id },
            onChangeRoleClick = { roleDialogId = it.id },
            onPasswordClick = { passwordDialogId = it.id },
            onFlagClick = { flagDialog = PendingToggle(it.id, !it.flagged) },
            onActiveClick = { activeDialog = PendingToggle(it.id, !it.active) }
        )
    }

    byId(roleDialogId)?.let { a ->
        AccountRoleDialog(
            account = a,
            roles = assignableRoles,
            onDismiss = { roleDialogId = null },
            onConfirm = { roleId, done -> onChangeRole(a, roleId, done) }
        )
    }

    byId(passwordDialogId)?.let { a ->
        AccountPasswordDialog(
            account = a,
            onDismiss = { passwordDialogId = null },
            onConfirm = { pw, done -> onResetPassword(a, pw, done) }
        )
    }

    flagDialog?.let { pending ->
        val a = byId(pending.accountId)
        if (a != null) {
            val flagging = pending.target
            AccountReasonDialog(
                title = if (flagging) "🚩 ফ্ল্যাগ করুন" else "ফ্ল্যাগ সরান",
                message = if (flagging) {
                    "\"${a.name}\" ফ্ল্যাগড হলে সব মেনু দেখতে পারবেন, কিন্তু কোনো অ্যাকশন (ব্যান, অনুমোদন, মুছে ফেলা ইত্যাদি) নিতে পারবেন না।"
                } else {
                    "\"${a.name}\"-এর ফ্ল্যাগ সরালে তিনি আবার রোল-অনুযায়ী সব অ্যাকশন নিতে পারবেন।"
                },
                reasonRequired = flagging,
                confirmLabel = if (flagging) "ফ্ল্যাগ করুন" else "আনফ্ল্যাগ",
                destructive = flagging,
                // TEMP-SESSION7: বিদ্যমান স্ক্রিনে বাটন-লক সেশন ৭-এ ওয়্যার হবে — তখন এই নোট মুছবে।
                tempNote = if (flagging) {
                    "ℹ️ সাময়িক: সেশন ৭ শেষ না হওয়া পর্যন্ত ফ্ল্যাগ শুধু চিহ্ন + প্রিভিউতে দেখায়; বিদ্যমান স্ক্রিনের অ্যাকশন-বাটন এখনো আসলে লক হয় না।"
                } else null,
                onDismiss = { flagDialog = null },
                onConfirm = { reason, done -> onSetFlagged(a, flagging, reason, done) }
            )
        }
    }

    activeDialog?.let { pending ->
        val a = byId(pending.accountId)
        if (a != null) {
            val activating = pending.target
            AccountReasonDialog(
                title = if (activating) "অ্যাকাউন্ট সক্রিয় করুন" else "অ্যাকাউন্ট নিষ্ক্রিয় করুন",
                message = if (activating) {
                    "\"${a.name}\" আবার লগইন করে কাজ করতে পারবেন।"
                } else {
                    "\"${a.name}\" নিষ্ক্রিয় হলে লগইন করা অবস্থায় ~৩০ সেকেন্ডের মধ্যে অটো-লগআউট হবেন এবং আর লগইন করতে পারবেন না।"
                },
                reasonRequired = !activating,
                confirmLabel = if (activating) "সক্রিয় করুন" else "নিষ্ক্রিয় করুন",
                destructive = !activating,
                tempNote = null,
                onDismiss = { activeDialog = null },
                onConfirm = { reason, done -> onSetActive(a, activating, reason, done) }
            )
        }
    }
}

// ============================================================
// তালিকা-পেইন
// ============================================================

@Composable
private fun AccountListPane(
    accounts: List<AdminAccountInfo>,
    query: String,
    onQueryChange: (String) -> Unit,
    currentAdminId: String?,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    isManualRefreshing: Boolean,
    viewModel: SomadhanViewModel?,
    onAddClick: () -> Unit,
    onPreview: (AdminAccountInfo) -> Unit,
    onChangeRoleClick: (AdminAccountInfo) -> Unit,
    onPasswordClick: (AdminAccountInfo) -> Unit,
    onFlagClick: (AdminAccountInfo) -> Unit,
    onActiveClick: (AdminAccountInfo) -> Unit
) {
    val filtered = filterAdminAccounts(accounts, query)

    // Ground Rule ২০ — সার্চ বদলালে দৃশ্যমান সব কার্ড pulse (AdminUsersView-এর প্যাটার্ন; try/finally বাধ্যতামূলক)।
    var isFilterRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        isFilterRefreshing = true
        try {
            delay(350L)
        } finally {
            isFilterRefreshing = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(SomadhanBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("এডমিন অ্যাকাউন্ট", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                Text("${accounts.size} জন এডমিন", fontSize = 12.sp, color = SomadhanTextSecondary)
            }
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("নতুন এডমিন")
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("নাম, ফোন, রোল বা পদবি দিয়ে খুঁজুন") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (accounts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (loadFailed) {
                    Text(
                        "এডমিন অ্যাকাউন্টের তালিকা আনা যায়নি। ইন্টারনেট সংযোগ দেখে আবার চেষ্টা করুন।",
                        color = SomadhanTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onRetry) { Text("আবার চেষ্টা করুন") }
                } else {
                    Text("কোনো এডমিন অ্যাকাউন্ট নেই।", color = SomadhanTextSecondary, fontSize = 13.sp)
                }
            }
        } else if (filtered.isEmpty()) {
            Text(
                "\"${query.trim()}\" দিয়ে কোনো এডমিন পাওয়া যায়নি।",
                color = SomadhanTextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(filtered, key = { it.id }) { account ->
                    // per-item pulse (Ground Rule ১৯): শুধু যে অ্যাকাউন্টের ডেটা সত্যিই বদলেছে তার কার্ড।
                    // lastSeenAt বাদ — heartbeat-এ প্রতি পোলে বদলায়, নইলে সব কার্ড ২০সে পরপর ঝলকাতো।
                    // flashOnReentry = false — স্ক্রলে নতুন কার্ড compose হওয়াকে re-entry ধরে pulse করা ঠেকাতে।
                    val itemPulse = rememberFieldChangePulse(
                        value = account.copy(lastSeenAt = null),
                        isManualRefreshing = isManualRefreshing,
                        sessionKey = "admin_accounts_sync",
                        viewModel = viewModel,
                        flashOnReentry = false
                    )
                    PulsingValue(isUpdating = itemPulse || isFilterRefreshing) {
                        AccountCard(
                            account = account,
                            isSelf = account.id == currentAdminId,
                            onPreview = { onPreview(account) },
                            onChangeRole = { onChangeRoleClick(account) },
                            onPassword = { onPasswordClick(account) },
                            onFlag = { onFlagClick(account) },
                            onActive = { onActiveClick(account) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountCard(
    account: AdminAccountInfo,
    isSelf: Boolean,
    onPreview: () -> Unit,
    onChangeRole: () -> Unit,
    onPassword: () -> Unit,
    onFlag: () -> Unit,
    onActive: () -> Unit
) {
    val online = account.isOnline && account.active
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = if (account.flagged) SomadhanErrorLight else SomadhanCardBg),
        border = BorderStroke(
            1.dp,
            when {
                account.flagged -> SomadhanError.copy(alpha = 0.45f)
                account.isSuper -> SomadhanOrange
                else -> SomadhanBorder
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // [ADMIN_ROLE_PROFILE সেশন ৬] ছবি থাকলে দেখায়, নইলে আগের মতোই আদ্যক্ষর — শেয়ার্ড AdminAvatar।
                com.example.ui.components.AdminAvatar(
                    name = account.name,
                    photo = account.photoUrl,
                    isSuper = account.isSuper,
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (account.flagged) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🚩 ফ্ল্যাগড", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SomadhanError)
                        }
                    }
                    Text(account.phone, fontSize = 12.sp, color = SomadhanTextHint)
                    if (account.designation.isNotBlank()) {
                        Text(account.designation, fontSize = 12.sp, color = SomadhanTextSecondary)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (online) SomadhanSuccess else SomadhanTextHint)
                    )
                    Text(
                        text = if (online) "অনলাইন" else "অফলাইন",
                        fontSize = 10.sp,
                        color = if (online) Color(0xFF0B7A4B) else SomadhanTextHint
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (account.isSuper) SomadhanAdminSlateLight else SomadhanOrangeContainer)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = account.roleName.ifBlank { "—" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (account.isSuper) SomadhanTextPrimary else SomadhanOrangePressed
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (account.active) SomadhanSuccessLight else SomadhanErrorLight)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when {
                            account.isSuper -> "🔒 সক্রিয়"
                            account.active -> "সক্রিয়"
                            else -> "নিষ্ক্রিয়"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (account.active) Color(0xFF0B7A4B) else SomadhanError
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            val lastLogin = buildString {
                append("সর্বশেষ লগইন: ")
                append(formatAdminTimestamp(account.lastLoginAt))
                if (!account.lastLoginDevice.isNullOrBlank()) append(" · ${account.lastLoginDevice}")
            }
            Text(lastLogin, fontSize = 11.sp, color = SomadhanTextSecondary)

            Spacer(modifier = Modifier.height(10.dp))
            if (account.isSuper) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccountActionButton("লাইভ প্রিভিউ", onPreview)
                    Text(
                        "🔒 সুপার অ্যাডমিন নিষ্ক্রিয়, ফ্ল্যাগ বা এডিট করা যায় না",
                        fontSize = 10.5.sp,
                        color = SomadhanTextHint,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AccountActionButton("লাইভ প্রিভিউ", onPreview)
                    AccountActionButton("রোল বদল", onChangeRole)
                    AccountActionButton("পাসওয়ার্ড রিসেট", onPassword)
                    if (!isSelf) {
                        AccountActionButton(
                            label = if (account.flagged) "আনফ্ল্যাগ" else "🚩 ফ্ল্যাগ করুন",
                            onClick = onFlag,
                            tint = if (account.flagged) SomadhanTextPrimary else SomadhanError
                        )
                        AccountActionButton(
                            label = if (account.active) "নিষ্ক্রিয় করুন" else "সক্রিয় করুন",
                            onClick = onActive,
                            tint = if (account.active) SomadhanError else Color(0xFF0B7A4B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountActionButton(label: String, onClick: () -> Unit, tint: Color = SomadhanTextPrimary) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tint)
    ) {
        Text(label, fontSize = 12.sp)
    }
}

// ============================================================
// প্রিভিউ-পেইন (অ্যাকাউন্ট-ভিত্তিক — ফ্ল্যাগড অবস্থাসহ)
// ============================================================

@Composable
private fun AccountPreviewPane(account: AdminAccountInfo, onBack: () -> Unit) {
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
                Text("${account.name} · ${account.roleName}", fontSize = 12.sp, color = SomadhanTextSecondary)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            AdminRolePreviewPanel(
                name = "${account.name} · ${account.roleName}",
                permissions = account.permissions,
                isSuper = account.isSuper,
                isFlagged = account.flagged
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ============================================================
// নতুন এডমিন তৈরির ফর্ম (পূর্ণ-পেন)
// ============================================================

@Composable
private fun AccountCreatePane(
    roles: List<AdminRoleInfo>,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSubmit: (name: String, phone: String, password: String, roleId: String, designation: String, email: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var designation by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    // ইচ্ছাকৃতভাবে কোনো রোল আগে থেকে সিলেক্ট করা নেই — ভুলে বেশি-পারমিশনের রোল বসে যাওয়া ঠেকাতে স্পষ্ট বাছাই।
    var roleId by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var roleError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(SomadhanBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel, enabled = !isSaving) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "বাতিল", tint = SomadhanTextPrimary)
            }
            Text("নতুন এডমিন", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // TEMP-SESSION7: পারমিশন-গেটিং বিদ্যমান স্ক্রিনে ওয়্যার হওয়া পর্যন্ত — তখন এই সতর্কতা মুছবে।
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanErrorLight),
                border = BorderStroke(1.dp, SomadhanError.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚠️ সাময়িক সতর্কতা: সেশন ৭ শেষ না হওয়া পর্যন্ত নতুন এডমিন (রোল যাই হোক) সুপার-অনলি ট্যাব বাদে " +
                        "বিদ্যমান সব স্ক্রিনে পূর্ণ অ্যাক্সেস পাবেন। তাই আসল স্টাফের জন্য নয়, শুধু টেস্ট-অ্যাকাউন্ট বানান।",
                    fontSize = 12.sp,
                    color = SomadhanError,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = { Text("নাম *") },
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it, color = SomadhanError) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; phoneError = null },
                label = { Text("ফোন নম্বর * (যেমন: 01712345678)") },
                isError = phoneError != null,
                supportingText = { phoneError?.let { Text(it, color = SomadhanError) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; passwordError = null },
                label = { Text("পাসওয়ার্ড * (কমপক্ষে ৮ অক্ষর)") },
                isError = passwordError != null,
                supportingText = { passwordError?.let { Text(it, color = SomadhanError) } },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "লুকান" else "দেখান"
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = designation,
                onValueChange = { designation = it },
                label = { Text("পদবি (ঐচ্ছিক)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("ইমেইল (ঐচ্ছিক)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("রোল *", fontSize = 12.sp, color = SomadhanTextSecondary)
            if (roles.isEmpty()) {
                Text(
                    "কোনো রোল নেই — আগে \"রোল ম্যানেজমেন্ট\" থেকে একটা রোল বানান।",
                    fontSize = 12.5.sp,
                    color = SomadhanError,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            } else {
                roles.forEach { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { roleId = r.id; roleError = null }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = roleId == r.id, onClick = { roleId = r.id; roleError = null })
                        Text(r.name, fontSize = 14.sp, color = SomadhanTextPrimary)
                    }
                }
            }
            roleError?.let { Text(it, color = SomadhanError, fontSize = 12.sp) }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val cleanPhone = normalizeAdminPhoneInput(phone)
                    nameError = if (name.isBlank()) accountErrorMessage("INVALID_NAME") else null
                    phoneError = if (!isValidAdminPhone(cleanPhone)) accountErrorMessage("INVALID_PHONE") else null
                    passwordError = if (password.length < 8) accountErrorMessage("INVALID_PASSWORD") else null
                    roleError = if (roleId == null) "একটা রোল বেছে নিন।" else null
                    val selectedRole = roleId
                    if (nameError == null && phoneError == null && passwordError == null && selectedRole != null) {
                        onSubmit(name, cleanPhone, password, selectedRole, designation, email)
                    }
                },
                enabled = !isSaving && roles.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSaving) "তৈরি হচ্ছে…" else "এডমিন অ্যাকাউন্ট তৈরি করুন")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ============================================================
// ডায়ালগ
// ============================================================

/** ফ্ল্যাগ/আনফ্ল্যাগ ও সক্রিয়/নিষ্ক্রিয়ের অভিন্ন কারণ-ডায়ালগ। কারণ ≥৩ অক্ষর (আবশ্যক হলে); সফল হলেই বন্ধ। */
@Composable
private fun AccountReasonDialog(
    title: String,
    message: String,
    reasonRequired: Boolean,
    confirmLabel: String,
    destructive: Boolean,
    tempNote: String?,
    onDismiss: () -> Unit,
    onConfirm: (reason: String, onDone: (Boolean) -> Unit) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    BottomSlideAlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary) },
        text = {
            Column {
                Text(message, color = SomadhanTextSecondary, fontSize = 13.sp)
                if (tempNote != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(tempNote, color = SomadhanTextHint, fontSize = 11.5.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it; error = null },
                    label = { Text(if (reasonRequired) "কারণ (আবশ্যক)" else "কারণ (ঐচ্ছিক)") },
                    isError = error != null,
                    supportingText = { error?.let { Text(it, color = SomadhanError) } },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    val r = reason.trim()
                    if (reasonRequired && r.length < 3) {
                        error = "কারণ কমপক্ষে ৩ অক্ষরে লিখুন।"
                    } else {
                        busy = true
                        onConfirm(r) { ok ->
                            busy = false
                            if (ok) onDismiss()
                        }
                    }
                }
            ) {
                Text(
                    text = if (busy) "অপেক্ষা করুন…" else confirmLabel,
                    color = if (destructive) SomadhanError else SomadhanOrange
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

/** রোল বদল — নন-সুপার রোলের রেডিও-তালিকা; বর্তমান রোলই সিলেক্টেড থাকলে কনফার্ম বন্ধ। */
@Composable
private fun AccountRoleDialog(
    account: AdminAccountInfo,
    roles: List<AdminRoleInfo>,
    onDismiss: () -> Unit,
    onConfirm: (roleId: String, onDone: (Boolean) -> Unit) -> Unit
) {
    var selected by remember { mutableStateOf(account.roleId) }
    var busy by remember { mutableStateOf(false) }

    BottomSlideAlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("রোল বদল — ${account.name}", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary) },
        text = {
            if (roles.isEmpty()) {
                Text("বদলানোর মতো কোনো রোল নেই।", color = SomadhanTextSecondary, fontSize = 13.sp)
            } else {
                Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    roles.forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = r.id }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected == r.id, onClick = { selected = r.id })
                            Text(r.name, fontSize = 14.sp, color = SomadhanTextPrimary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && roles.isNotEmpty() && selected != account.roleId,
                onClick = {
                    busy = true
                    onConfirm(selected) { ok ->
                        busy = false
                        if (ok) onDismiss()
                    }
                }
            ) { Text(if (busy) "অপেক্ষা করুন…" else "রোল বদলান", color = SomadhanOrange) }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

/** অন্য এডমিনের পাসওয়ার্ড রিসেট — নতুন পাসওয়ার্ড ≥৮ অক্ষর, দেখা/লুকানোর টগল। */
@Composable
private fun AccountPasswordDialog(
    account: AdminAccountInfo,
    onDismiss: () -> Unit,
    onConfirm: (newPassword: String, onDone: (Boolean) -> Unit) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    BottomSlideAlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("পাসওয়ার্ড রিসেট — ${account.name}", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary) },
        text = {
            Column {
                Text(
                    "নতুন পাসওয়ার্ড বসালে ওই এডমিনের চলমান সেশন বন্ধ হবে; তাকে নতুন পাসওয়ার্ড জানিয়ে দিন।",
                    color = SomadhanTextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("নতুন পাসওয়ার্ড (কমপক্ষে ৮ অক্ষর)") },
                    isError = error != null,
                    supportingText = { error?.let { Text(it, color = SomadhanError) } },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (visible) "লুকান" else "দেখান"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    if (password.length < 8) {
                        error = accountErrorMessage("INVALID_PASSWORD")
                    } else {
                        busy = true
                        onConfirm(password) { ok ->
                            busy = false
                            if (ok) onDismiss()
                        }
                    }
                }
            ) { Text(if (busy) "অপেক্ষা করুন…" else "রিসেট করুন", color = SomadhanError) }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("বাতিল") }
        }
    )
}
