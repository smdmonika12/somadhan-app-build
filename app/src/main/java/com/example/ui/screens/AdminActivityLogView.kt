package com.example.ui.screens

/**
 * [ADMIN_ROLE_PROFILE সেশন ৫] `AdminActivityLogView` — সুপার-অনলি অ্যাক্টিভিটি লগ (ট্যাব ২৭): *সব* এডমিনের
 * কাজের তালিকা, ক্লাউড থেকে (`admin_activity_logs_list`)। বিদ্যমান লিগ্যাসি লগ-ট্যাব (১২, `AdminAuditLogView`) থেকে আলাদা —
 * সেটা শুধু ওই ডিভাইসের নিজের Room-লগ দেখায়, তাই মাল্টি-এডমিনে সুপারের কাজে লাগে না; সেটা অপরিবর্তিত।
 *
 * রেফারেন্স: `admin-role-management.html`-এর "অ্যাক্টিভিটি লগ" ট্যাব (শুধু ইন্টারঅ্যাকশন/লেআউট) + `AdminAccountsView.kt`
 * ও `AdminUsersView.kt`-এর কনভেনশন।
 *
 * - প্রতিটা এন্ট্রি: হেডলাইনে **এডমিনের নাম + রোল-ব্যাজ** পাশাপাশি, তারপর অ্যাকশন-ভার্ব + টার্গেট + কারণ (`details`,
 *   প্রশ্ন ৪: আলাদা কলাম না) + টাইমস্ট্যাম্প। এডমিন-পরিচয়হীন লগ (২০২৬-০৯-২৪-এর আগের shared-admin যুগ, সিস্টেম/
 *   ইউজার-ট্রিগার্ড ইভেন্ট) "সিস্টেম / লিগ্যাসি" দেখায়।
 * - ফিল্টার = এডমিনের নাম দিয়ে: সার্চ-ইনপুট **এবং** ড্রপডাউন-সিলেক্ট, দুটোই; লাইভ না — শুধু "ফিল্টার করুন" বাটনে প্রয়োগ।
 *   ফিল্টার + পেজিনেশন দুটোই সার্ভারে (তাই ফিল্টার-সহ পেজিং সঠিক)।
 * - loading নিয়ম: cold-load skeleton AdminPanelScreen-এর `SyncAwareContent` (sessionKey "admin_activity_log_sync");
 *   Ground Rule ১৮ — কখনো পুরো-পেজ ঝলকায় না, শুধু কার্ডগুলো pulse; Ground Rule ২০ — ফিল্টার/পেজিনেশন বদলে
 *   দৃশ্যমান সব কার্ড pulse (`AdminUsersView`-এর `isFilterRefreshing` প্যাটার্ন); pull-to-refresh শেষেও pulse।
 *   লগ immutable (আপডেট হয় না), তাই per-item ডেটা-বদল pulse (GR১৯) ও realtime-insert pulse (GR২১) এখানে প্রযোজ্য না।
 *
 * ⚠️ কম্পাইল/রান করা হয়নি (এই এনভায়রনমেন্টে Gradle/SDK নেই) — Android Studio-তে প্রথম বিল্ডে ধরা পড়বে।
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.AdminAccountInfo
import com.example.data.security.AdminActivityLogEntry
import com.example.data.security.AdminActivityLogFilter
import com.example.ui.components.PulsingValue
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.theme.SomadhanAdminSlateLight
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeContainer
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanOrangePressed
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import kotlinx.coroutines.delay

// ============================================================
// পিওর হেল্পার (Compose-নির্ভরতা নেই, নিজেরাই টেস্টযোগ্য)
// ============================================================

/** সার্ভার RPC-র raise-করা কোড → বাংলা বার্তা (`contains` দিয়ে, postgrest message-এ কোডের আগে-পরে লেখা থাকে)। */
fun activityLogErrorMessage(raw: String): String = when {
    raw.contains("SUPER_ADMIN_REQUIRED") || raw.contains("AUTH_REQUIRED") -> "শুধু সুপার অ্যাডমিন অ্যাক্টিভিটি লগ দেখতে পারবেন।"
    else -> "লগ আনা যায়নি। ইন্টারনেট সংযোগ দেখে আবার চেষ্টা করুন।"
}

/**
 * ইনপুট-অবস্থা → প্রয়োগযোগ্য ফিল্টার। "সিস্টেম / লিগ্যাসি" বাছলে এডমিন-আইডি ও নাম দুটোই বাদ (সার্ভারে দুটো
 * একসাথে AND হয় — এডমিন-পরিচয়হীন লগে নাম ফাঁকা, তাই মিশালে সবসময় খালি ফল আসত)।
 */
fun buildActivityFilter(adminId: String?, unattributedOnly: Boolean, nameInput: String): AdminActivityLogFilter =
    if (unattributedOnly) {
        AdminActivityLogFilter(unattributedOnly = true)
    } else {
        AdminActivityLogFilter(adminId = adminId, nameQuery = nameInput.trim())
    }

/** সক্রিয় ফিল্টারের এক-লাইন বিবরণ ("ফিল্টার সক্রিয়: …")। */
fun describeActivityFilter(filter: AdminActivityLogFilter, admins: List<AdminAccountInfo>): String {
    val parts = mutableListOf<String>()
    if (filter.unattributedOnly) parts += "সিস্টেম / লিগ্যাসি"
    filter.adminId?.let { id ->
        parts += admins.find { it.id == id }?.name ?: "নির্বাচিত এডমিন"
    }
    if (filter.nameQuery.isNotBlank()) parts += "নাম: \"${filter.nameQuery.trim()}\""
    return parts.joinToString(" · ")
}

/**
 * অ্যাকশন-কোডের বাংলা নাম। এই ফিচারে (সেশন ১-৪) এবং লাইভ লগে দেখা নতুন কোডগুলো এখানে; বাকি সব বিদ্যমান
 * [getBengaliActionName]-এ (অজানা হলে কোডটাই ফেরত)।
 */
fun activityActionLabel(actionType: String): String = when (actionType) {
    "ADMIN_LOGIN" -> "এডমিন লগইন"
    "ADMIN_CREATED" -> "নতুন এডমিন অ্যাকাউন্ট তৈরি"
    "ADMIN_ROLE_CHANGED" -> "এডমিনের রোল বদল"
    "ADMIN_ACTIVATED" -> "এডমিন অ্যাকাউন্ট সক্রিয়"
    "ADMIN_DEACTIVATED" -> "এডমিন অ্যাকাউন্ট নিষ্ক্রিয়"
    "ADMIN_FLAGGED" -> "🚩 এডমিন ফ্ল্যাগ করা হয়েছে"
    "ADMIN_UNFLAGGED" -> "এডমিনের ফ্ল্যাগ সরানো হয়েছে"
    "ADMIN_PASSWORD_RESET" -> "এডমিনের পাসওয়ার্ড রিসেট"
    "ADMIN_PROFILE_UPDATED" -> "এডমিন প্রোফাইল আপডেট"
    "ROLE_CREATED" -> "নতুন রোল তৈরি"
    "ROLE_UPDATED" -> "রোল আপডেট"
    "ROLE_DELETED" -> "রোল মুছে ফেলা"
    "ADMIN_RESOLVE_DISPUTE" -> "বিরোধ নিষ্পত্তি"
    "ADMIN_RECONCILE_ESCROW_STATES" -> "Escrow অবস্থা মিলিয়ে দেখা (Reconcile)"
    "ADMIN_RECONCILE_USER_BALANCES" -> "ইউজার ব্যালেন্স মিলিয়ে দেখা (Reconcile)"
    "BALANCE_RECONCILIATION" -> "ব্যালেন্স রিকনসিলিয়েশন"
    "COMPLETE_WITHDRAWAL" -> "উইথড্র সম্পন্ন"
    "REJECT_WITHDRAWAL" -> "উইথড্র বাতিল"
    "DISPUTE_RAISED" -> "বিরোধ উত্থাপিত (ইউজার)"
    "SOLVER_CANCEL_JOB" -> "সলভার কাজ বাতিল করেছেন"
    "WALLET_DEPOSIT" -> "ওয়ালেটে ডিপোজিট"
    "INSTANT_JOB_REBROADCAST" -> "ইনস্ট্যান্ট জব আবার ব্রডকাস্ট"
    "FORCE_CANCEL_INSTANT_JOB" -> "ইনস্ট্যান্ট জব জোর করে বাতিল"
    "VIEW_CHAT" -> "চ্যাট দেখা"
    else -> getBengaliActionName(actionType)
}

/** এন্ট্রির পরিচয়-লেবেল: এডমিনের নাম, নইলে "সিস্টেম / লিগ্যাসি"। */
fun activityActorLabel(entry: AdminActivityLogEntry): String =
    if (entry.isAttributed) entry.adminName else "সিস্টেম / লিগ্যাসি"

/** user/solver রোল-স্কোপের বাংলা নাম; ফাঁকা হলে null (কিছু দেখানোর নেই)। */
fun activityRoleScopeLabel(role: String): String? = when (role.uppercase()) {
    "USER" -> "ইউজার প্রোফাইলে"
    "SOLVER" -> "সলভার প্রোফাইলে"
    else -> null
}

// ============================================================
// টপ-লেভেল স্ক্রিন
// ============================================================

@Composable
fun AdminActivityLogView(
    logs: List<AdminActivityLogEntry>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    isFiltering: Boolean,
    appliedFilter: AdminActivityLogFilter,
    admins: List<AdminAccountInfo>,
    onApplyFilter: (AdminActivityLogFilter) -> Unit,
    onLoadMore: () -> Unit,
    viewModel: SomadhanViewModel? = null,
    isManualRefreshing: Boolean = false,
    // প্রথম পেজ একবারও আনা যায়নি — তখন "কোনো লগ নেই"-র জায়গায় এরর + retry (নেই আর আনা যায়নি — আলাদা অবস্থা)।
    loadFailed: Boolean = false,
    onRetry: () -> Unit = {}
) {
    // ইনপুট-অবস্থা (টাইপ করা ≠ প্রয়োগ করা)। প্রয়োগকৃত ফিল্টার থেকে শুরু, তাই ট্যাবে ফিরলে বর্তমান ফিল্টার দেখায়।
    var nameInput by rememberSaveable { mutableStateOf(appliedFilter.nameQuery) }
    var pendingAdminId by rememberSaveable { mutableStateOf(appliedFilter.adminId) }
    var pendingUnattributed by rememberSaveable { mutableStateOf(appliedFilter.unattributedOnly) }
    var menuOpen by remember { mutableStateOf(false) }

    val applyPending = {
        onApplyFilter(buildActivityFilter(pendingAdminId, pendingUnattributed, nameInput))
    }

    val selectedLabel = when {
        pendingUnattributed -> "সিস্টেম / লিগ্যাসি"
        pendingAdminId != null -> admins.find { it.id == pendingAdminId }?.let { "${it.name} · ${it.roleName}" } ?: "নির্বাচিত এডমিন"
        else -> "সব এডমিন"
    }

    val listState = rememberLazyListState()

    // scroll-to-load: শেষের ~৩ আইটেমের কাছে এলে পরের পেজ। isLoadingMore ইচ্ছাকৃতভাবে এই derived-state-এ নেই —
    // থাকলে ব্যর্থ লোডের পর মান false→true হয়ে আবার আবার চেষ্টা চালাত (টোস্ট-ঝড়); ডুপ্লিকেট কল VM-ই ঠেকায়।
    val shouldLoadMore by remember(hasMore) {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasMore && total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    // ফিল্টার বদলালে টপে ফেরা — raw appliedFilter-এ key (Ground Rule ২০-এর scroll-jump নিয়ম: শুধু ইচ্ছাকৃত বদলে, পেজ-append বা
    // রিফ্রেশে না)।
    LaunchedEffect(appliedFilter) {
        listState.scrollToItem(0)
    }

    // Ground Rule ২০ — ফিল্টার/পেজিনেশন বদলে দৃশ্যমান সব কার্ড pulse; try/finally বাধ্যতামূলক (stuck-pulse এড়াতে)।
    var isFilterRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(appliedFilter, logs.size) {
        isFilterRefreshing = true
        try {
            delay(350L)
        } finally {
            isFilterRefreshing = false
        }
    }
    // pull-to-refresh শেষে pulse (isManualRefreshing সত্যি→মিথ্যা); লগ immutable, তাই value = Unit।
    val manualPulse = rememberFieldChangePulse(
        value = Unit,
        isManualRefreshing = isManualRefreshing,
        sessionKey = "admin_activity_log_sync",
        viewModel = viewModel,
        flashOnReentry = false
    )
    val cardsPulse = isFilterRefreshing || manualPulse

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(SomadhanBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ---------- হেডার + ফিল্টার কার্ড ----------
        item(key = "filter_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(SomadhanOrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("এডমিন অ্যাক্টিভিটি লগ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary)
                            Text("কোন এডমিন কী করেছেন — সব এডমিনের কাজের রেকর্ড", fontSize = 12.sp, color = SomadhanTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // এডমিনের নাম দিয়ে খোঁজা (সার্চ-ইনপুট) — "সিস্টেম / লিগ্যাসি" বাছা থাকলে নিষ্ক্রিয়
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        enabled = !pendingUnattributed,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("এডমিনের নাম লিখুন...", fontSize = 13.sp, color = SomadhanTextHint) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SomadhanTextSecondary, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (nameInput.isNotEmpty()) {
                                IconButton(onClick = { nameInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "মুছুন", tint = SomadhanTextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { applyPending() }),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ড্রপডাউন-সিলেক্ট (স্থিতিশীল DropdownMenu API — ExposedDropdownMenu-র menuAnchor সংস্করণ-নির্ভর)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = selectedLabel,
                                color = SomadhanTextPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SomadhanTextSecondary)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("সব এডমিন") },
                                onClick = {
                                    pendingAdminId = null
                                    pendingUnattributed = false
                                    menuOpen = false
                                }
                            )
                            admins.forEach { a ->
                                DropdownMenuItem(
                                    text = { Text("${a.name} · ${a.roleName}") },
                                    onClick = {
                                        pendingAdminId = a.id
                                        pendingUnattributed = false
                                        menuOpen = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("সিস্টেম / লিগ্যাসি (এডমিন-পরিচয়হীন)") },
                                onClick = {
                                    pendingAdminId = null
                                    pendingUnattributed = true
                                    nameInput = ""
                                    menuOpen = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { applyPending() },
                            enabled = !isFiltering,
                            colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                            modifier = Modifier.weight(1f)
                        ) { Text(if (isFiltering) "খোঁজা হচ্ছে…" else "ফিল্টার করুন") }
                        OutlinedButton(
                            onClick = {
                                nameInput = ""
                                pendingAdminId = null
                                pendingUnattributed = false
                                onApplyFilter(AdminActivityLogFilter())
                            },
                            enabled = !isFiltering && (appliedFilter.isActive || nameInput.isNotEmpty() || pendingAdminId != null || pendingUnattributed)
                        ) { Text("রিসেট") }
                    }

                    if (isFiltering) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = SomadhanOrange)
                    }
                    if (appliedFilter.isActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "ফিল্টার সক্রিয়: ${describeActivityFilter(appliedFilter, admins)}",
                            fontSize = 12.sp,
                            color = SomadhanOrangePressed
                        )
                    }
                }
            }
        }

        // ---------- লগ-তালিকা ----------
        if (logs.isEmpty()) {
            item(key = "empty_state") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        loadFailed -> {
                            Text(
                                "অ্যাক্টিভিটি লগ আনা যায়নি। ইন্টারনেট সংযোগ দেখে আবার চেষ্টা করুন।",
                                color = SomadhanTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = onRetry) { Text("আবার চেষ্টা করুন") }
                        }
                        appliedFilter.isActive -> Text("এই ফিল্টারে কোনো লগ পাওয়া যায়নি।", color = SomadhanTextSecondary, fontSize = 13.sp)
                        else -> Text("এখনো কোনো লগ নেই।", color = SomadhanTextSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(logs, key = { it.id }) { entry ->
                PulsingValue(isUpdating = cardsPulse) {
                    ActivityLogCard(entry)
                }
            }
            item(key = "footer") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        isLoadingMore -> Text("আরও লগ আনা হচ্ছে…", fontSize = 12.sp, color = SomadhanTextSecondary)
                        // স্বয়ংক্রিয় লোড ব্যর্থ হলে/না-চললে হাতে চাপার উপায়
                        hasMore -> OutlinedButton(onClick = onLoadMore) { Text("আরও দেখুন") }
                        else -> Text("সব লগ দেখানো হয়েছে।", fontSize = 12.sp, color = SomadhanTextHint)
                    }
                }
            }
        }
    }
}

// ============================================================
// লগ কার্ড
// ============================================================

@Composable
private fun ActivityLogCard(entry: AdminActivityLogEntry) {
    val (icon, tint, tintBg) = getAuditActionVisuals(entry.actionType)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SomadhanBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // হেডলাইন: এডমিনের নাম + রোল-ব্যাজ পাশাপাশি
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (entry.isAttributed) SomadhanOrangeContainer else SomadhanAdminSlateLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (entry.isAttributed) {
                        Text(
                            text = adminInitials(entry.adminName),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanOrangePressed
                        )
                    } else {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = SomadhanTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = activityActorLabel(entry),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (entry.isAttributed && entry.adminRoleName.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(SomadhanOrangeContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(entry.adminRoleName, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = SomadhanOrangePressed)
                            }
                        }
                    }
                    Text(formatAdminTimestamp(entry.timestamp), fontSize = 11.sp, color = SomadhanTextHint)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // অ্যাকশন-ভার্ব
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(tintBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = activityActionLabel(entry.actionType),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SomadhanTextPrimary
                )
            }

            // টার্গেট
            val target = entry.targetName.ifBlank { entry.targetId }
            if (target.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("টার্গেট: $target", fontSize = 12.5.sp, color = SomadhanTextSecondary)
            }
            activityRoleScopeLabel(entry.role)?.let { scope ->
                Text(scope, fontSize = 11.5.sp, color = SomadhanTextHint)
            }

            // কারণ / বিবরণ (`details` ফিল্ড — প্রশ্ন ৪: কারণ আলাদা কলামে না)
            if (entry.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("কারণ / বিবরণ: ${entry.details}", fontSize = 12.5.sp, color = SomadhanTextPrimary)
            }

            if (!entry.isAttributed) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "এডমিন-পরিচয় রেকর্ড নেই (মাল্টি-এডমিন চালুর আগের লগ, অথবা সিস্টেম/ইউজার-ট্রিগার্ড ইভেন্ট)",
                    fontSize = 10.5.sp,
                    color = SomadhanTextHint
                )
            }
        }
    }
}
