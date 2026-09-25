@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.AdditionalChargeEntity
import com.example.data.entity.AdminAuditLogEntity
import com.example.data.entity.BidEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.EscrowEntity
import com.example.data.entity.FaqEntity
import com.example.data.entity.MessageEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.RatingEntity
import com.example.data.entity.ReputationEventEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.WithdrawalEntity
import com.example.data.repository.AdminDashboardMetrics
import com.example.ui.components.OutboxPendingIndicator
import com.example.ui.components.PulsingValue
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.ReputationBadge
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.navigation.Screen
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.theme.SomadhanYellowVerified
import com.example.ui.theme.SomadhanYellowVerifiedBg
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.CsvExportUtil
import com.example.util.DistanceUtil
import com.example.util.FileAttachmentUtil
import com.example.util.Formatters
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.text.SimpleDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.example.ui.components.BottomSlideAlertDialog
import com.example.ui.components.AdminAccessLockedState
import com.example.data.security.AdminSession

@Composable
fun AdminKycView(
    users: List<com.example.data.entity.UserEntity>,
    onApproveKyc: (String) -> Unit,
    onRejectKyc: (String, String) -> Unit,
    onRevokeKyc: (String, String) -> Unit,
    onUpdateKycInfo: (String, String, String, String, String) -> Unit,
    onBulkApprove: (List<String>) -> Unit,
    onResetKycToPending: (String) -> Unit,
    // Admin Panel Loading fix, সেশন ২.৩ — শুধু নিচের তালিকা pulse করার জন্য (structure/সার্চ
    // বার/ট্যাব হেডার/pagination bar অপরিবর্তিত রাখতে) নতুন ঐচ্ছিক প্যারামিটার দুটো। ডিফল্ট
    // মান দেওয়া আছে বলে এই দুটো বাদ দিয়ে কল করলেও (এই মুহূর্তে কোনো call-site নেই) আগের
    // আচরণেই থাকবে (pulse কখনোই ট্রিগার হবে না)।
    viewModel: SomadhanViewModel? = null,
    isManualRefreshing: Boolean = false
) {
    // [ADMIN_ROLE_PROFILE সেশন ৭.১] view-গেট — আগের দুইটা স্ক্রিনের ঠিক একই প্যাটার্নে, সবার আগে
    // early-return। "approve"/"reject"/"revoke"/"reset_pending"/"edit" — প্রতিটা অ্যাকশনের
    // এন্ট্রি-পয়েন্ট বাটনে নিচে আলাদাভাবে AdminSession.canAct(...) দিয়ে গেট হয়েছে; সংশ্লিষ্ট ডায়ালগ
    // নিজে ছোঁয়া হয়নি (AdminUsersView.kt-এর একই যুক্তি — এন্ট্রি-পয়েন্ট বন্ধ থাকলে ডায়ালগ খোলার পথই নেই)।
    if (!AdminSession.canView("users", "kyc")) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            AdminAccessLockedState()
        }
        return
    }
    var selectedKycTab by rememberSaveable { mutableIntStateOf(0) }
    var pendingCurrentPage by rememberSaveable { mutableIntStateOf(1) }
    var verifiedCurrentPage by rememberSaveable { mutableIntStateOf(1) }
    var rejectedCurrentPage by rememberSaveable { mutableIntStateOf(1) }
    val pageSize = 10

    // [অফলাইন Action Gating ধাপ ১২, গ্রুপ A ভেরিফিকেশন] outbox pending sync ইন্ডিকেটর
    // (adminApproveKyc/adminRejectKyc/adminRevokeKyc/adminBulkApproveKyc -এর জন্য, এই ফাইলের
    // Group A কল-সাইট)। viewModel এখানে ঐচ্ছিক বলে null হলে 0 fallback (AdminWithdrawalsView-এর
    // একই প্যাটার্ন)।
    val outboxPendingCount by viewModel?.outboxPendingCount?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(0) }

    var searchQuery by remember { mutableStateOf("") }
    var isSelectMode by remember { mutableStateOf(false) }
    var selectedUserIds by remember { mutableStateOf(setOf<String>()) }
    var showBulkApproveDialog by remember { mutableStateOf(false) }

    // Dialog & Sheet states
    var rejectTargetUserId by remember { mutableStateOf<String?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    var revokeTargetUserId by remember { mutableStateOf<String?>(null) }
    var revokeTargetUserName by remember { mutableStateOf("") }
    var revokeReason by remember { mutableStateOf("") }

    var editingUser by remember { mutableStateOf<com.example.data.entity.UserEntity?>(null) }
    var editDocNumber by remember { mutableStateOf("") }
    var editFirstName by remember { mutableStateOf("") }
    var editLastName by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }

    var viewDocUser by remember { mutableStateOf<com.example.data.entity.UserEntity?>(null) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var menuExpandedSolverId by remember { mutableStateOf<String?>(null) }

    val allPendingSolvers = remember(users) {
        users.filter { (it.role.equals("SOLVER", ignoreCase = true) || it.hasSolverRole) && !it.isKycVerified && it.kycStatus != "rejected" && (!it.kycDocumentNumber.isNullOrBlank() || !it.kycDocumentFrontImage.isNullOrBlank() || !it.kycDocumentImage.isNullOrBlank() || it.kycStatus == "pending") }
            .sortedByDescending { it.kycSubmissionDate ?: it.updatedAt }
    }

    val filteredPendingSolvers = remember(allPendingSolvers, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            allPendingSolvers
        } else {
            allPendingSolvers.filter {
                it.name.lowercase().contains(query) ||
                    it.phone.lowercase().contains(query) ||
                    it.email.lowercase().contains(query) ||
                    (it.kycDocumentNumber ?: "").lowercase().contains(query)
            }
        }
    }

    val allVerifiedSolvers = remember(users) {
        users.filter { (it.role.equals("SOLVER", ignoreCase = true) || it.hasSolverRole) && it.isKycVerified }
            .sortedByDescending { it.updatedAt }
    }

    val filteredVerifiedSolvers = remember(allVerifiedSolvers, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            allVerifiedSolvers
        } else {
            allVerifiedSolvers.filter {
                it.name.lowercase().contains(query) ||
                    it.phone.lowercase().contains(query) ||
                    it.email.lowercase().contains(query) ||
                    (it.kycDocumentNumber ?: "").lowercase().contains(query)
            }
        }
    }

    val allRejectedSolvers = remember(users) {
        users.filter { (it.role.equals("SOLVER", ignoreCase = true) || it.hasSolverRole) && it.kycStatus == "rejected" }
            .sortedByDescending { it.updatedAt }
    }

    val filteredRejectedSolvers = remember(allRejectedSolvers, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            allRejectedSolvers
        } else {
            allRejectedSolvers.filter {
                it.name.lowercase().contains(query) ||
                    it.phone.lowercase().contains(query) ||
                    it.email.lowercase().contains(query) ||
                    (it.kycDocumentNumber ?: "").lowercase().contains(query)
            }
        }
    }

    // Reset pagination when searching
    LaunchedEffect(searchQuery) {
        pendingCurrentPage = 1
        verifiedCurrentPage = 1
        rejectedCurrentPage = 1
    }

    val pendingTotalPages = remember(filteredPendingSolvers.size, pageSize) {
        maxOf(1, (filteredPendingSolvers.size + pageSize - 1) / pageSize)
    }
    val safePendingPage = pendingCurrentPage.coerceIn(1, pendingTotalPages)
    val paginatedPendingSolvers = remember(filteredPendingSolvers, safePendingPage, pageSize) {
        if (filteredPendingSolvers.isEmpty()) emptyList()
        else {
            val fromIndex = (safePendingPage - 1) * pageSize
            val toIndex = minOf(fromIndex + pageSize, filteredPendingSolvers.size)
            if (fromIndex in 0 until filteredPendingSolvers.size) {
                filteredPendingSolvers.subList(fromIndex, toIndex)
            } else emptyList()
        }
    }

    val verifiedTotalPages = remember(filteredVerifiedSolvers.size, pageSize) {
        maxOf(1, (filteredVerifiedSolvers.size + pageSize - 1) / pageSize)
    }
    val safeVerifiedPage = verifiedCurrentPage.coerceIn(1, verifiedTotalPages)
    val paginatedVerifiedSolvers = remember(filteredVerifiedSolvers, safeVerifiedPage, pageSize) {
        if (filteredVerifiedSolvers.isEmpty()) emptyList()
        else {
            val fromIndex = (safeVerifiedPage - 1) * pageSize
            val toIndex = minOf(fromIndex + pageSize, filteredVerifiedSolvers.size)
            if (fromIndex in 0 until filteredVerifiedSolvers.size) {
                filteredVerifiedSolvers.subList(fromIndex, toIndex)
            } else emptyList()
        }
    }

    val rejectedTotalPages = remember(filteredRejectedSolvers.size, pageSize) {
        maxOf(1, (filteredRejectedSolvers.size + pageSize - 1) / pageSize)
    }
    val safeRejectedPage = rejectedCurrentPage.coerceIn(1, rejectedTotalPages)
    val paginatedRejectedSolvers = remember(filteredRejectedSolvers, safeRejectedPage, pageSize) {
        if (filteredRejectedSolvers.isEmpty()) emptyList()
        else {
            val fromIndex = (safeRejectedPage - 1) * pageSize
            val toIndex = minOf(fromIndex + pageSize, filteredRejectedSolvers.size)
            if (fromIndex in 0 until filteredRejectedSolvers.size) {
                filteredRejectedSolvers.subList(fromIndex, toIndex)
            } else emptyList()
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(safePendingPage, safeVerifiedPage, safeRejectedPage, selectedKycTab) {
        listState.scrollToItem(0)
    }

    // Admin Panel Loading fix, সেশন ২.৩০ রেট্রোফিট (Ground Rule ২০) — সার্চ/পেজিনেশন বদলালে
    // দৃশ্যমান সব solver-card ছোট্ট করে একসাথে pulse করবে (per-item Ground Rule ১৯ পাল্স অপরিবর্তিত)।
    // selectedKycTab এই key-তে রাখা হয়নি ইচ্ছাকৃতভাবে — ট্যাব বদল শুধু ভিউ বদলায়, ডেটা রিফিল্টার হয় না।
    var isFilterRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(searchQuery, safePendingPage, safeVerifiedPage, safeRejectedPage) {
        isFilterRefreshing = true
        try {
            delay(350L)
        } finally {
            isFilterRefreshing = false
        }
    }

    // Ground Rule ২১, সেশন ২.১৯.৪ — যে user-দের kyc_status সবেমাত্র pending/verified/rejected-এ
    // ট্রানজিশন করেছে তাদের id-র সংক্ষিপ্ত-সময়ের সেট (viewModel থেকে সরাসরি, দেখো
    // SomadhanViewModel.recentlyKycChangedUserIds/SupabaseRealtimeManager-এর কমেন্ট)। viewModel
    // এই ফাইলে ঐচ্ছিক (`= null` ডিফল্ট, দেখো ফাংশন-সিগনেচার) বলে null হলে খালি সেট fallback।
    // নিচে items(...)-এর ভেতরে per-item membership-check-এ ব্যবহার হয়।
    val recentlyKycChangedUserIds by viewModel?.recentlyKycChangedUserIds?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(emptySet<String>()) }

    // Admin Panel Loading fix, সেশন ২.১৭ রেট্রোফিট (per-item pulse, ব্যবহারকারীর স্ট্যান্ডিং
    // সিদ্ধান্ত — সেশন ২.১৬-এ AdminUsersView-তে প্রথম প্রয়োগ করা "per-item + no-scroll-pulse"
    // প্যাটার্নই এখন KYC-তে) — আগে এখানে একটা single visible-list rememberFieldChangePulse(value =
    // selectedKycTab to visibleKycList) ছিল, যেটা কোনো একটা সলভারের action (approve/reject/...)
    // চাপলেই visibleKycList রেফারেন্স বদলে যেত বলে সেই ট্যাবের *সব* কার্ড একসাথে pulse করতো। এখন
    // সেই কলটা প্রতিটা ট্যাবের items(...) { solver -> ... }-এর ভেতরে per-item সরানো হয়েছে (value =
    // solver, পুরো লিস্ট না) — data class বলে equals-ভিত্তিক তুলনায় শুধু যে সলভারের ডেটা সত্যিই
    // বদলেছে তার কার্ডই pulse করবে। flashOnReentry = false per-item কলে (আগে whole-list কলে সত্যি
    // ছিল, sessionKey "admin_kyc_sync" AdminPanelScreen.kt-এর cold-load gate-এর সাথে শেয়ার্ড বলে) —
    // কারণ per-item sessionKey/viewModel দিলে LazyColumn-এ স্ক্রল করে নতুন কার্ড প্রথমবার compose
    // হওয়ার মুহূর্তেই সেটা "re-entry" ধরে নিয়ে নিজে থেকে pulse দেখাতো (অনিচ্ছাকৃত স্ক্রল-pulse) —
    // flashOnReentry বন্ধ রাখায় শুধু genuine data-change আর isManualRefreshing সম্পন্ন হওয়াতেই pulse
    // হবে, স্ক্রলে না। ট্যাব পাল্টানো/pagination পাল্টানোতে আর আলাদা pulse হয় না (আগে হতো, কারণ
    // "value" -এ selectedKycTab যুক্ত ছিল) — নতুন ট্যাব/পেজের কার্ডগুলো নতুন compose হয় বলে সেটা
    // flashOnReentry=false-এর আওতায় pulse-হীন থাকে, যা List-লোডিং UX-এর সাথে সামঞ্জস্যপূর্ণ
    // (Users রেট্রোফিটেও একই আচরণ)। withdrawal (সেশন ২.১৫) এখনো পুরনো whole-list প্যাটার্নে —
    // ADMIN_PANEL_LOADING_MASTER_PROMPT.md-এর Ground Rule ১৯ দেখো। আগে এখানে একটা
    // visibleKycList = when (selectedKycTab) {...} ভ্যাল ছিল যেটা শুধু kycListPulse-এর জন্য
    // দরকার ছিল — per-item রেট্রোফিটে সেটা আর দরকার নেই বলে সরানো হয়েছে।

    // --- 1. Fullscreen Image Viewer Dialog ---
    if (fullscreenImageUrl != null) {
        Dialog(
            onDismissRequest = { fullscreenImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = fullscreenImageUrl,
                    contentDescription = "ফুলস্ক্রিন ছবি",
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { fullscreenImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // --- 2. Reject KYC Dialog ---
    if (rejectTargetUserId != null) {
        BottomSlideAlertDialog(
            onDismissRequest = {
                rejectTargetUserId = null
                rejectReason = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cancel, contentDescription = null, tint = SomadhanError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KYC আবেদন বাতিল", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Column {
                    Text("বাতিল করার কারণ লিখুন:", fontSize = 13.sp, color = SomadhanTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        placeholder = { Text("যেমন: NID ছবি অস্পষ্ট বা ভুল তথ্য") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = rejectTargetUserId
                        if (uid != null) {
                            onRejectKyc(uid, if (rejectReason.isBlank()) "তথ্য অস্পষ্ট" else rejectReason)
                            rejectTargetUserId = null
                            rejectReason = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("বাতিল করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    rejectTargetUserId = null
                    rejectReason = ""
                }) {
                    Text("ফিরে যান", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // --- 3. Revoke KYC Dialog ---
    if (revokeTargetUserId != null) {
        val uid = revokeTargetUserId!!
        BottomSlideAlertDialog(
            onDismissRequest = {
                revokeTargetUserId = null
                revokeReason = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = SomadhanError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ভেরিফিকেশন প্রত্যাহার", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "আপনি কি নিশ্চিত যে '${revokeTargetUserName}'-এর ভেরিফিকেশন প্রত্যাহার করতে চান? সলভার আবার বাতিলকৃত তালিকায় চলে যাবে।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = revokeReason,
                        onValueChange = { revokeReason = it },
                        label = { Text("প্রত্যাহারের কারণ") },
                        placeholder = { Text("যেমন: ভুয়া ডকুমেন্ট শনাক্তকরণ বা নীতি লঙ্ঘন") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRevokeKyc(uid, if (revokeReason.isBlank()) "অ্যাডমিন কর্তৃক ভেরিফিকেশন প্রত্যাহার" else revokeReason)
                        revokeTargetUserId = null
                        revokeReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("প্রত্যাহার করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    revokeTargetUserId = null
                    revokeReason = ""
                }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // --- 4. Edit KYC Info Dialog ---
    if (editingUser != null) {
        val targetUser = editingUser!!
        BottomSlideAlertDialog(
            onDismissRequest = { editingUser = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = SomadhanOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KYC তথ্য এডিট করুন", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "${targetUser.name} (${Formatters.toLocalDisplayFormat(targetUser.phone)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanOrange
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editDocNumber,
                        onValueChange = { editDocNumber = it },
                        label = { Text("ডকুমেন্ট নম্বর") },
                        placeholder = { Text("NID / পাসপোর্ট নম্বর") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editFirstName,
                        onValueChange = { editFirstName = it },
                        label = { Text("নামের প্রথমাংশ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editLastName,
                        onValueChange = { editLastName = it },
                        label = { Text("নামের শেষাংশ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("ঠিকানা") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateKycInfo(
                            targetUser.id,
                            editDocNumber.trim(),
                            editFirstName.trim(),
                            editLastName.trim(),
                            editAddress.trim()
                        )
                        editingUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("সংরক্ষণ করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingUser = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // --- 5. Bulk Approve Confirmation Dialog ---
    if (showBulkApproveDialog) {
        val count = selectedUserIds.size
        BottomSlideAlertDialog(
            onDismissRequest = { showBulkApproveDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SomadhanSuccess,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "বাল্ক KYC অনুমোদন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে নির্বাচিত ${DistanceUtil.toBengaliDigits(count.toString())}টি KYC আবেদন একসাথে অনুমোদন করতে চান?",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBulkApprove(selectedUserIds.toList())
                        selectedUserIds = emptySet()
                        isSelectMode = false
                        showBulkApproveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("হ্যাঁ, অনুমোদন করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkApproveDialog = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // --- 6. View Documents ModalBottomSheet (for Verified Solvers) ---
    if (viewDocUser != null) {
        val target = viewDocUser!!
        val frontImg = target.kycDocumentFrontImage ?: target.kycDocumentImage
        val backImg = target.kycDocumentBackImage
        val selfieImg = target.kycSelfieImage

        ModalBottomSheet(
            onDismissRequest = { viewDocUser = null },
            containerColor = SomadhanBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${target.name} - KYC ডকুমেন্টস",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "ফোন: ${Formatters.toLocalDisplayFormat(target.phone)} | স্ট্যাটাস: ভেরিফাইড ✅",
                            fontSize = 12.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                    IconButton(onClick = { viewDocUser = null }) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = SomadhanTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                if (!target.kycDocumentNumber.isNullOrBlank()) {
                    Text(
                        text = "ডকুমেন্ট নম্বর: ${target.kycDocumentType ?: "NID"} - ${target.kycDocumentNumber}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanOrange
                    )
                }
                if (!target.kycFirstName.isNullOrBlank() || !target.kycLastName.isNullOrBlank()) {
                    Text(
                        text = "নাম: ${target.kycFirstName ?: ""} ${target.kycLastName ?: ""}".trim(),
                        fontSize = 12.sp,
                        color = SomadhanTextPrimary
                    )
                }
                if (!target.kycAddress.isNullOrBlank()) {
                    Text(
                        text = "ঠিকানা: ${target.kycAddress}",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "আপলোডকৃত ছবি (বড় করে দেখতে ক্লিক করুন):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SomadhanTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!frontImg.isNullOrBlank()) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                                    .background(SomadhanCardBg)
                                    .clickable { fullscreenImageUrl = frontImg }
                            ) {
                                AsyncImage(
                                    model = frontImg,
                                    contentDescription = "সামনের ছবি",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("সামনের ছবি 🔍", fontSize = 11.sp, color = SomadhanTextSecondary)
                        }
                    }
                    if (!backImg.isNullOrBlank()) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                                    .background(SomadhanCardBg)
                                    .clickable { fullscreenImageUrl = backImg }
                            ) {
                                AsyncImage(
                                    model = backImg,
                                    contentDescription = "পেছনের ছবি",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("পেছনের ছবি 🔍", fontSize = 11.sp, color = SomadhanTextSecondary)
                        }
                    }
                    if (!selfieImg.isNullOrBlank()) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                                    .background(SomadhanCardBg)
                                    .clickable { fullscreenImageUrl = selfieImg }
                            ) {
                                AsyncImage(
                                    model = selfieImg,
                                    contentDescription = "সেলফি",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("সেলফি 🔍", fontSize = 11.sp, color = SomadhanTextSecondary)
                        }
                    }
                }

                if (frontImg.isNullOrBlank() && backImg.isNullOrBlank() && selfieImg.isNullOrBlank()) {
                    Text(
                        text = "কোনো ছবি সংরক্ষিত নেই।",
                        fontSize = 12.sp,
                        color = SomadhanTextHint,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = if (selectedKycTab == 0 && isSelectMode && selectedUserIds.isNotEmpty()) 80.dp else 0.dp)
            ) {
            // [অফলাইন Action Gating ধাপ ১২] outbox pending sync ইন্ডিকেটর (non-blocking, খালি
            // থাকলে কিছুই দেখায় না) — LazyColumn বলে item{} ব্লকে, সার্চ বারের ঠিক ওপরে।
            item {
                OutboxPendingIndicator(
                    pendingCount = outboxPendingCount,
                    onRetryClick = { viewModel?.retryOutboxSyncNow() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )
            }

            // 1. Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("নাম, ফোন বা ডকুমেন্ট দিয়ে খুঁজুন...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = SomadhanOrange
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "মুছুন",
                                    tint = SomadhanTextHint
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        unfocusedBorderColor = SomadhanDivider,
                        focusedContainerColor = SomadhanCardBg,
                        unfocusedContainerColor = SomadhanCardBg
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 2. 3 Clean Tabs: অপেক্ষমাণ (Pending), ভেরিফাইড (Verified), বাতিলকৃত (Rejected)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                ) {
                    TabRow(
                        selectedTabIndex = selectedKycTab,
                        containerColor = Color.Transparent,
                        contentColor = SomadhanOrange,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedKycTab == 0,
                            onClick = { selectedKycTab = 0 },
                            text = {
                                Text(
                                    text = "অপেক্ষমাণ (${DistanceUtil.toBengaliDigits(filteredPendingSolvers.size.toString())})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedKycTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                        Tab(
                            selected = selectedKycTab == 1,
                            onClick = { selectedKycTab = 1 },
                            text = {
                                Text(
                                    text = "ভেরিফাইড (${DistanceUtil.toBengaliDigits(filteredVerifiedSolvers.size.toString())})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedKycTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                        Tab(
                            selected = selectedKycTab == 2,
                            onClick = { selectedKycTab = 2 },
                            text = {
                                Text(
                                    text = "বাতিলকৃত (${DistanceUtil.toBengaliDigits(filteredRejectedSolvers.size.toString())})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedKycTab == 2) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ==========================================
            // TAB 0: অপেক্ষমাণ KYC আবেদন (Pending Solvers)
            // ==========================================
            if (selectedKycTab == 0) {
                // Header with Select Mode Toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "অপেক্ষমাণ আবেদন তালিকা (${DistanceUtil.toBengaliDigits(filteredPendingSolvers.size.toString())})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )

                        if (filteredPendingSolvers.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelectMode) {
                                    TextButton(
                                        onClick = {
                                            selectedUserIds = if (selectedUserIds.size == filteredPendingSolvers.size) {
                                                emptySet()
                                            } else {
                                                filteredPendingSolvers.map { it.id }.toSet()
                                            }
                                        }
                                    ) {
                                        Text(
                                            if (selectedUserIds.size == filteredPendingSolvers.size) "সব আনসিলেক্ট" else "সব সিলেক্ট",
                                            fontSize = 12.sp,
                                            color = SomadhanOrange,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        isSelectMode = !isSelectMode
                                        if (!isSelectMode) selectedUserIds = emptySet()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSelectMode) SomadhanOrange else SomadhanDivider),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelectMode) SomadhanOrange.copy(alpha = 0.1f) else Color.Transparent
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelectMode) Icons.Default.Assignment else Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelectMode) SomadhanOrange else SomadhanTextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSelectMode) "বাতিল" else "সিলেক্ট মোড",
                                        fontSize = 12.sp,
                                        color = if (isSelectMode) SomadhanOrange else SomadhanTextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (filteredPendingSolvers.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "অনুসন্ধানের সাথে কোনো আবেদন মেলেনি।" else "কোনো অপেক্ষমাণ KYC আবেদন নেই।",
                                    fontSize = 13.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        }
                    }
                } else {
                    items(paginatedPendingSolvers, key = { it.id }) { solver ->
                        val isSelected = solver.id in selectedUserIds
                        // per-item pulse (উপরের কমেন্ট দেখো) — flashOnReentry = false বলে স্ক্রল
                        // করে এই কার্ড প্রথমবার viewport-এ আসলে pulse হবে না।
                        val solverCardPulse = rememberFieldChangePulse(
                            value = solver,
                            isManualRefreshing = isManualRefreshing,
                            sessionKey = "admin_kyc_sync",
                            viewModel = viewModel,
                            flashOnReentry = false
                        )
                        // Ground Rule ২১, সেশন ২.১৯.৪ — kyc_status সবেমাত্র pending-এ ট্রানজিশন
                        // করেছে এমন কার্ডও সংক্ষিপ্ত সময়ের জন্য pulse করবে (উপরের কমেন্ট দেখো)।
                        val isNewFromRealtime = recentlyKycChangedUserIds.contains(solver.id)
                        PulsingValue(isUpdating = solverCardPulse || isFilterRefreshing || isNewFromRealtime) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SomadhanOrange.copy(alpha = 0.05f) else SomadhanBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .border(
                                    1.dp,
                                    if (isSelected) SomadhanOrange else SomadhanDivider,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelectMode) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    selectedUserIds = if (checked) {
                                                        selectedUserIds + solver.id
                                                    } else {
                                                        selectedUserIds - solver.id
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = SomadhanOrange)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Column {
                                            Text(
                                                text = solver.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanTextPrimary
                                            )
                                            Text(
                                                text = "ফোন: ${Formatters.toLocalDisplayFormat(solver.phone)} | ইমেইল: ${solver.email}",
                                                fontSize = 11.sp,
                                                color = SomadhanTextSecondary
                                            )
                                        }
                                    }
                                    StatusBadge(status = "PENDING")
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Exact Date & Time
                                val submissionTime = solver.kycSubmissionDate ?: solver.updatedAt
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SomadhanCardBg)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "আবেদনের সময়: ${Formatters.formatDateTimeBengali(submissionTime)} (${Formatters.formatTimeAgo(submissionTime)})",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("ঠিকানা: ${solver.address}", fontSize = 12.sp, color = SomadhanTextSecondary)
                                if (!solver.kycDocumentNumber.isNullOrBlank()) {
                                    Text(
                                        text = "ডকুমেন্ট: ${solver.kycDocumentType ?: "NID"} - ${solver.kycDocumentNumber}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanOrange
                                    )
                                }
                                if (!solver.kycFirstName.isNullOrBlank() || !solver.kycLastName.isNullOrBlank()) {
                                    Text(
                                        text = "আবেদনের নাম: ${solver.kycFirstName ?: ""} ${solver.kycLastName ?: ""}".trim(),
                                        fontSize = 12.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }

                                // Uploaded KYC Documents Preview (Front, Back, Selfie)
                                val frontImg = solver.kycDocumentFrontImage ?: solver.kycDocumentImage
                                val backImg = solver.kycDocumentBackImage
                                val selfieImg = solver.kycSelfieImage

                                if (!frontImg.isNullOrBlank() || !backImg.isNullOrBlank() || !selfieImg.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (!frontImg.isNullOrBlank()) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(70.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .border(1.dp, SomadhanDivider, RoundedCornerShape(6.dp))
                                                        .background(SomadhanCardBg)
                                                        .clickable { fullscreenImageUrl = frontImg }
                                                ) {
                                                    AsyncImage(
                                                        model = frontImg,
                                                        contentDescription = "সামনের ছবি",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("সামনের ছবি 🔍", fontSize = 10.sp, color = SomadhanTextSecondary)
                                            }
                                        }
                                        if (!backImg.isNullOrBlank()) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(70.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .border(1.dp, SomadhanDivider, RoundedCornerShape(6.dp))
                                                        .background(SomadhanCardBg)
                                                        .clickable { fullscreenImageUrl = backImg }
                                                ) {
                                                    AsyncImage(
                                                        model = backImg,
                                                        contentDescription = "পেছনের ছবি",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("পেছনের ছবি 🔍", fontSize = 10.sp, color = SomadhanTextSecondary)
                                            }
                                        }
                                        if (!selfieImg.isNullOrBlank()) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(70.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .border(1.dp, SomadhanDivider, RoundedCornerShape(6.dp))
                                                        .background(SomadhanCardBg)
                                                        .clickable { fullscreenImageUrl = selfieImg }
                                                ) {
                                                    AsyncImage(
                                                        model = selfieImg,
                                                        contentDescription = "সেলফি",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("সেলফি 🔍", fontSize = 10.sp, color = SomadhanTextSecondary)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Edit Info Button
                                // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:kyc:edit" গেট
                                OutlinedButton(
                                    onClick = {
                                        editingUser = solver
                                        editDocNumber = solver.kycDocumentNumber ?: ""
                                        editFirstName = solver.kycFirstName ?: ""
                                        editLastName = solver.kycLastName ?: ""
                                        editAddress = solver.kycAddress ?: solver.address
                                    },
                                    enabled = AdminSession.canAct("users:kyc:edit"),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("তথ্য এডিট করুন", fontSize = 11.sp, color = SomadhanOrange, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:kyc:approve" গেট
                                    Button(
                                        onClick = { onApproveKyc(solver.id) },
                                        enabled = AdminSession.canAct("users:kyc:approve"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("অনুমোদন করুন", fontSize = 11.sp)
                                    }

                                    // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:kyc:reject" গেট
                                    Button(
                                        onClick = { rejectTargetUserId = solver.id },
                                        enabled = AdminSession.canAct("users:kyc:reject"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("বাতিল করুন", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }

            // ==========================================
            // TAB 1: ভেরিফাইড সলভার তালিকা (Verified Solvers)
            // ==========================================
            if (selectedKycTab == 1) {
                item {
                    Text(
                        text = "ভেরিফাইড সলভার তালিকা (${DistanceUtil.toBengaliDigits(filteredVerifiedSolvers.size.toString())})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (filteredVerifiedSolvers.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "অনুসন্ধানের সাথে কোনো ভেরিফাইড সলভার মেলেনি।" else "এখনও কোনো ভেরিফাইড সলভার নেই।",
                                    fontSize = 12.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        }
                    }
                } else {
                    items(paginatedVerifiedSolvers, key = { it.id }) { solver ->
                        // per-item pulse (উপরের কমেন্ট দেখো) — flashOnReentry = false বলে স্ক্রল
                        // করে এই কার্ড প্রথমবার viewport-এ আসলে pulse হবে না।
                        val solverCardPulse = rememberFieldChangePulse(
                            value = solver,
                            isManualRefreshing = isManualRefreshing,
                            sessionKey = "admin_kyc_sync",
                            viewModel = viewModel,
                            flashOnReentry = false
                        )
                        // Ground Rule ২১, সেশন ২.১৯.৪ — kyc_status সবেমাত্র verified-এ ট্রানজিশন
                        // করেছে এমন কার্ডও সংক্ষিপ্ত সময়ের জন্য pulse করবে (উপরের কমেন্ট দেখো)।
                        val isNewFromRealtime = recentlyKycChangedUserIds.contains(solver.id)
                        PulsingValue(isUpdating = solverCardPulse || isFilterRefreshing || isNewFromRealtime) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = solver.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        StatusBadge(status = "ACCEPTED")
                                    }
                                    Text("ফোন: ${Formatters.toLocalDisplayFormat(solver.phone)} | ${solver.address}", fontSize = 11.sp, color = SomadhanTextSecondary)
                                    if (!solver.kycDocumentNumber.isNullOrBlank()) {
                                        Text(
                                            text = "ডকুমেন্ট: ${solver.kycDocumentType ?: "NID"} - ${solver.kycDocumentNumber}",
                                            fontSize = 10.sp,
                                            color = SomadhanOrange
                                        )
                                    }

                                    // Exact Verification timestamp
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = SomadhanSuccess,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ভেরিফিকেশন: ${Formatters.formatDateTimeBengali(solver.updatedAt)}",
                                            fontSize = 10.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }
                                }

                                Box {
                                    IconButton(onClick = { menuExpandedSolverId = solver.id }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "মেনু",
                                            tint = SomadhanTextSecondary
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = menuExpandedSolverId == solver.id,
                                        onDismissRequest = { menuExpandedSolverId = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("ডকুমেন্ট দেখুন", fontSize = 12.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Visibility,
                                                    contentDescription = null,
                                                    tint = SomadhanOrange,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            onClick = {
                                                menuExpandedSolverId = null
                                                viewDocUser = solver
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("তথ্য এডিট করুন", fontSize = 12.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = SomadhanOrange,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            enabled = AdminSession.canAct("users:kyc:edit"),
                                            onClick = {
                                                menuExpandedSolverId = null
                                                editingUser = solver
                                                editDocNumber = solver.kycDocumentNumber ?: ""
                                                editFirstName = solver.kycFirstName ?: ""
                                                editLastName = solver.kycLastName ?: ""
                                                editAddress = solver.kycAddress ?: solver.address
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("ভেরিফিকেশন বাতিল করুন (Revoke)", fontSize = 12.sp, color = SomadhanError) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Cancel,
                                                    contentDescription = null,
                                                    tint = SomadhanError,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            enabled = AdminSession.canAct("users:kyc:revoke"),
                                            onClick = {
                                                menuExpandedSolverId = null
                                                revokeTargetUserId = solver.id
                                                revokeTargetUserName = solver.name
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }

            // ==========================================
            // TAB 2: বাতিলকৃত KYC আবেদন (Rejected Solvers)
            // ==========================================
            if (selectedKycTab == 2) {
                item {
                    Text(
                        text = "বাতিলকৃত / রিজেক্টেড আবেদন তালিকা (${DistanceUtil.toBengaliDigits(filteredRejectedSolvers.size.toString())})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (filteredRejectedSolvers.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "অনুসন্ধানের সাথে কোনো বাতিলকৃত আবেদন মেলেনি।" else "কোনো রিজেক্টেড বা বাতিলকৃত আবেদন নেই।",
                                    fontSize = 12.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        }
                    }
                } else {
                    items(paginatedRejectedSolvers, key = { it.id }) { solver ->
                        // per-item pulse (উপরের কমেন্ট দেখো) — flashOnReentry = false বলে স্ক্রল
                        // করে এই কার্ড প্রথমবার viewport-এ আসলে pulse হবে না।
                        val solverCardPulse = rememberFieldChangePulse(
                            value = solver,
                            isManualRefreshing = isManualRefreshing,
                            sessionKey = "admin_kyc_sync",
                            viewModel = viewModel,
                            flashOnReentry = false
                        )
                        // Ground Rule ২১, সেশন ২.১৯.৪ — kyc_status সবেমাত্র rejected-এ ট্রানজিশন
                        // করেছে এমন কার্ডও সংক্ষিপ্ত সময়ের জন্য pulse করবে (উপরের কমেন্ট দেখো)।
                        val isNewFromRealtime = recentlyKycChangedUserIds.contains(solver.id)
                        PulsingValue(isUpdating = solverCardPulse || isFilterRefreshing || isNewFromRealtime) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, SomadhanError.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = solver.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary
                                        )
                                        Text("ফোন: ${Formatters.toLocalDisplayFormat(solver.phone)} | ${solver.address}", fontSize = 11.sp, color = SomadhanTextSecondary)
                                    }
                                    StatusBadge(status = "REJECTED")
                                }

                                if (!solver.kycDocumentNumber.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ডকুমেন্ট: ${solver.kycDocumentType ?: "NID"} - ${solver.kycDocumentNumber}",
                                        fontSize = 11.sp,
                                        color = SomadhanOrange,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Exact Timestamp for rejection
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = SomadhanError,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "বাতিলের সময়: ${Formatters.formatDateTimeBengali(solver.updatedAt)} (${Formatters.formatTimeAgo(solver.updatedAt)})",
                                        fontSize = 10.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }

                                if (!solver.kycRejectReason.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SomadhanError.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "বাতিলের কারণ: ${solver.kycRejectReason}",
                                            fontSize = 11.sp,
                                            color = SomadhanError,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { viewDocUser = solver },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.6f))
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ডকুমেন্ট", fontSize = 11.sp, color = SomadhanOrange)
                                    }

                                    // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:kyc:reset_pending" গেট
                                    Button(
                                        onClick = { onResetKycToPending(solver.id) },
                                        enabled = AdminSession.canAct("users:kyc:reset_pending"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1.5f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "পেন্ডিং-এ পাঠান",
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
        }

        // Fixed Pagination Controls at Bottom
        when (selectedKycTab) {
            0 -> {
                if (filteredPendingSolvers.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = if (isSelectMode && selectedUserIds.isNotEmpty()) 72.dp else 0.dp)
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (safePendingPage > 1) pendingCurrentPage = safePendingPage - 1 },
                                enabled = safePendingPage > 1,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পূর্ববর্তী", modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("পূর্ববর্তী", fontSize = 11.sp)
                            }

                            Text(
                                text = "পেজ ${DistanceUtil.toBengaliDigits(safePendingPage.toString())} / ${DistanceUtil.toBengaliDigits(pendingTotalPages.toString())}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )

                            OutlinedButton(
                                onClick = { if (safePendingPage < pendingTotalPages) pendingCurrentPage = safePendingPage + 1 },
                                enabled = safePendingPage < pendingTotalPages,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("পরবর্তী", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "পরবর্তী", modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }
            1 -> {
                if (filteredVerifiedSolvers.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (safeVerifiedPage > 1) verifiedCurrentPage = safeVerifiedPage - 1 },
                                enabled = safeVerifiedPage > 1,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পূর্ববর্তী", modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("পূর্ববর্তী", fontSize = 11.sp)
                            }

                            Text(
                                text = "পেজ ${DistanceUtil.toBengaliDigits(safeVerifiedPage.toString())} / ${DistanceUtil.toBengaliDigits(verifiedTotalPages.toString())}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )

                            OutlinedButton(
                                onClick = { if (safeVerifiedPage < verifiedTotalPages) verifiedCurrentPage = safeVerifiedPage + 1 },
                                enabled = safeVerifiedPage < verifiedTotalPages,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("পরবর্তী", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "পরবর্তী", modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }
            2 -> {
                if (filteredRejectedSolvers.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (safeRejectedPage > 1) rejectedCurrentPage = safeRejectedPage - 1 },
                                enabled = safeRejectedPage > 1,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পূর্ববর্তী", modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("পূর্ববর্তী", fontSize = 11.sp)
                            }

                            Text(
                                text = "পেজ ${DistanceUtil.toBengaliDigits(safeRejectedPage.toString())} / ${DistanceUtil.toBengaliDigits(rejectedTotalPages.toString())}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )

                            OutlinedButton(
                                onClick = { if (safeRejectedPage < rejectedTotalPages) rejectedCurrentPage = safeRejectedPage + 1 },
                                enabled = safeRejectedPage < rejectedTotalPages,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("পরবর্তী", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "পরবর্তী", modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }
        }
    }

        // 6. Floating Bulk Approval Action Bar
        if (isSelectMode && selectedUserIds.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanSuccess.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "নির্বাচিত: ${DistanceUtil.toBengaliDigits(selectedUserIds.size.toString())}টি আবেদন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = "একসাথে অনুমোদন করতে চাপুন",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                        // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:kyc:approve" গেট (bulk-approve একই অ্যাকশন)
                        Button(
                            onClick = { showBulkApproveDialog = true },
                            enabled = AdminSession.canAct("users:kyc:approve"),
                            colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "বাল্ক অনুমোদন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
