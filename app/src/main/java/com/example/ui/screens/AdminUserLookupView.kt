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
import com.example.util.ImageStorageUtil
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
import com.example.util.RefundCategory
import com.example.util.RefundMeta
import com.example.util.TransactionHelper
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.text.SimpleDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.example.ui.components.BottomSlideAlertDialog
import com.example.ui.components.AdminAccessLockedState
import com.example.data.security.AdminSession

@Composable
fun AdminUserLookupView(
    allUsers: List<UserEntity>,
    allProblems: List<ProblemEntity>,
    allWithdrawals: List<WithdrawalEntity>,
    allHeldEscrows: List<EscrowEntity>,
    allRefundedEscrows: List<EscrowEntity>,
    allCancelledBids: List<BidEntity>,
    allAdditionalCharges: List<AdditionalChargeEntity>,
    allTransactions: List<TransactionEntity>,
    allRatings: List<RatingEntity>,
    viewModel: SomadhanViewModel,
    onNavigate: (String) -> Unit = {}
) {
    // [ADMIN_ROLE_PROFILE সেশন ৭.১(ঘ)] view-গেট — আগের স্ক্রিনগুলোর (AdminSolverQuotaView/
    // AdminUsersView/AdminKycView) ঠিক একই প্যাটার্নে, সবার আগে early-return। এই স্ক্রিনের
    // ব্যান/রেস্ট্রিক্ট/ব্যালেন্স/রোল/রেপুটেশন/পাসওয়ার্ড/ডিলিট/KYC-অ্যাপ্রুভ/KYC-রিভোক/সমস্যা-
    // স্ট্যাটাস/সমস্যা-ডিলিট/রিভিউ-ডিলিট/উইথড্র-অ্যাপ্রুভ/উইথড্র-রিজেক্ট — প্রতিটা তাদের নিজ নিজ
    // ডোমেইনের কী দিয়ে গেটেড (users:users:*, users:kyc:*, work:problems:*, moderation:reviews:*,
    // finance:withdrawals:*) — user_search-এর নিজস্ব কী দিয়ে না, কারণ AdminPermissionCatalog.kt-এর
    // হেডার-কমেন্টে আগে থেকেই কনফার্মড: বিপরীত পথ (Lookup-এর নিজস্ব কী) নিরাপত্তা-ফাঁক তৈরি করত।
    // সংশ্লিষ্ট ডায়ালগ নিজে ছোঁয়া হয়নি — এন্ট্রি-পয়েন্ট বন্ধ থাকলে ডায়ালগ খোলার পথই নেই।
    if (!AdminSession.canView("users", "user_search")) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            AdminAccessLockedState()
        }
        return
    }
    val context = LocalContext.current
    // Loading Pattern Master Prompt — এই স্ক্রিনের rule ২+৩ পুরনো 9-list raw diff-key দিয়ে না
    // করে (সেটা প্রায় সবসময় flash করাতো — কোনো-না-কোনো global list প্রায় সবসময় বদলায়), বরং
    // AdminStatsView-এর Overview ট্যাবের মতোই independent per-section shimmer-zone দিয়ে করা হলো
    // (নিচের প্রতিটা "item {}" ব্লকে দেখুন) — যা বদলায় তা-ই শিমার করে, বাকিটা স্থির থাকে।
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    // [অফলাইন Action Gating ধাপ ১২, গ্রুপ A ভেরিফিকেশন] outbox pending sync ইন্ডিকেটর
    // (adminSetBanned/adminSetRestricted/adminChangeRole/adminRejectKyc/adminRevokeKyc -সহ এই
    // স্ক্রিনের Group A ফাংশনগুলোর জন্য) — AdminWithdrawalsView-এর মতোই একই প্যাটার্ন।
    val outboxPendingCount by viewModel.outboxPendingCount.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<UserEntity?>(null) }
    var userPerspectiveTab by remember { mutableStateOf(0) } // 0 -> User Perspective, 1 -> Solver Perspective
    var activeSubTab by remember { mutableStateOf(0) } // 0: সারসংক্ষেপ, 1: সমস্যাসমূহ/কন্ট্রাক্ট, 2: ট্রানজেকশন ও এসক্রো, 3: রিভিউ

    // Sync selected user if allUsers updates
    val currentUser = remember(selectedUser, allUsers) {
        selectedUser?.let { target -> allUsers.find { it.id == target.id } ?: target }
    }

    // Filter matching users for live suggestions / list
    val matchingUsers = remember(searchQuery, allUsers) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            emptyList()
        } else {
            // [SUPABASE-MIGRATED - E.164 ফলো-আপ ফিক্স ৩] admin লোকাল ফরম্যাটে (017...) টাইপ করলেও
            // E.164-এ সেভ হওয়া phone-এর সাথে মিলুক -- normalizeTarget() UID/name/email-এ harmless
            // (email হলে শুধু lowercase, non-phone হলে প্রায় no-op)।
            val qNormalized = com.example.util.OtpService.normalizeTarget(q).lowercase()
            allUsers.filter { u ->
                u.id.lowercase().contains(q) ||
                    u.displayUid.lowercase().contains(q) ||
                    u.phone.lowercase().contains(q) || u.phone.lowercase().contains(qNormalized) ||
                    u.email.lowercase().contains(q) ||
                    u.name.lowercase().contains(q)
            }.take(10)
        }
    }

    // Admin Action Dialog States
    var userForBanToggle by remember { mutableStateOf<UserEntity?>(null) }
    var roleForBanToggle by remember { mutableStateOf("USER") }
    var userForRestrictToggle by remember { mutableStateOf<UserEntity?>(null) }
    var roleForRestrictToggle by remember { mutableStateOf("USER") }

    var userForBalanceAdjust by remember { mutableStateOf<UserEntity?>(null) }
    // [ধাপ ৭ ফিক্স, ROLE_SEPARATION_AUDIT.md] ban/restrict-এর roleForBanToggle/
    // roleForRestrictToggle-এর ঠিক একই প্যাটার্নে — ক্লিকের মুহূর্তে (currentPerspectiveRole
    // থেকে) কোন role-এর ব্যালেন্স adjust করা হচ্ছে তা মনে রাখতে, dual-role ইউজারের ক্ষেত্রে
    // অ্যাকাউন্টের বর্তমান সক্রিয় role-এর সাথে মিশে না যাওয়ার জন্য।
    var roleForBalanceAdjust by remember { mutableStateOf("USER") }
    var balanceAmountInput by remember { mutableStateOf("") }
    var balanceReasonInput by remember { mutableStateOf("") }
    var isBalanceAddition by remember { mutableStateOf(true) }

    var userForRoleChange by remember { mutableStateOf<UserEntity?>(null) }
    var selectedNewRole by remember { mutableStateOf("USER") }

    var userForReputationAdjust by remember { mutableStateOf<UserEntity?>(null) }
    // [BALANCE_REPUTATION_ROLE_SEPARATION ধাপ ৭খ, ROLE_SEPARATION_AUDIT.md] roleForBalanceAdjust-এর
    // ঠিক একই প্যাটার্নে — ক্লিকের মুহূর্তে (currentPerspectiveRole থেকে) কোন role-এর রেপুটেশন
    // adjust করা হচ্ছে তা মনে রাখতে, dual-role ইউজারের ক্ষেত্রে অ্যাকাউন্টের বর্তমান সক্রিয়
    // role-এর সাথে মিশে না যাওয়ার জন্য।
    var roleForReputationAdjust by remember { mutableStateOf("USER") }
    var repScoreChangeInput by remember { mutableStateOf("") }
    var repNoteInput by remember { mutableStateOf("") }
    var isRepAddition by remember { mutableStateOf(true) }

    var userForPasswordReset by remember { mutableStateOf<UserEntity?>(null) }
    var newPasswordInput by remember { mutableStateOf("") }

    var userForDeleteConfirm by remember { mutableStateOf<UserEntity?>(null) }

    var kycRejectTargetUser by remember { mutableStateOf<UserEntity?>(null) }
    var kycRejectReason by remember { mutableStateOf("") }

    var kycRevokeTargetUser by remember { mutableStateOf<UserEntity?>(null) }
    var kycRevokeReason by remember { mutableStateOf("") }

    // --- Dialog Implementations ---
    // 1. Ban Toggle Dialog
    if (userForBanToggle != null) {
        val target = userForBanToggle!!
        val isCurrentlyBanned = if (roleForBanToggle == "SOLVER") target.isBannedSolver else target.isBannedUser
        val banRoleLabel = if (roleForBanToggle == "SOLVER") "সলভার" else "গ্রাহক"
        BottomSlideAlertDialog(
            onDismissRequest = { userForBanToggle = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCurrentlyBanned) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (isCurrentlyBanned) SomadhanSuccess else SomadhanError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCurrentlyBanned) "ব্যান প্রত্যাহার করুন" else "ব্যবহারকারীকে ব্যান করুন",
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = if (isCurrentlyBanned)
                        "আপনি কি নিশ্চিত যে '${target.name}'-এর $banRoleLabel পরিচয়ের ওপর থেকে ব্যান নিষেধাজ্ঞা প্রত্যাহার করতে চান? এই পরিচয়ে ব্যবহারকারী পুনরায় স্বাভাবিকভাবে অ্যাপ ব্যবহার করতে পারবেন। অন্য ভূমিকা (থাকলে) অপরিবর্তিত থাকবে।"
                    else
                        "আপনি কি নিশ্চিত যে '${target.name}'-এর $banRoleLabel পরিচয়কে ব্যান করতে চান? ব্যান করা অবস্থায় এই পরিচয়ে ব্যবহারকারী কোনো সমাধান পোস্ট বা বিড করতে পারবেন না। অন্য ভূমিকা (থাকলে) অপরিবর্তিত থাকবে।",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminSetBanned(target.id, !isCurrentlyBanned, roleForBanToggle)
                        userForBanToggle = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentlyBanned) SomadhanSuccess else SomadhanError
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isCurrentlyBanned) "আনব্যান করুন" else "হ্যাঁ, ব্যান করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { userForBanToggle = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // 2. Restrict Toggle Dialog
    if (userForRestrictToggle != null) {
        val target = userForRestrictToggle!!
        val isCurrentlyRestricted = if (roleForRestrictToggle == "SOLVER") target.isRestrictedSolver else target.isRestrictedUser
        val restrictRoleLabel = if (roleForRestrictToggle == "SOLVER") "সলভার" else "গ্রাহক"
        BottomSlideAlertDialog(
            onDismissRequest = { userForRestrictToggle = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCurrentlyRestricted) Icons.Default.Check else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isCurrentlyRestricted) SomadhanSuccess else SomadhanOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCurrentlyRestricted) "সীমাবদ্ধতা প্রত্যাহার করুন" else "অ্যাকাউন্ট রেস্ট্রিক্ট করুন",
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = if (isCurrentlyRestricted)
                        "আপনি কি নিশ্চিত যে '${target.name}'-এর $restrictRoleLabel পরিচয়ের সীমাবদ্ধতা প্রত্যাহার করতে চান? অন্য ভূমিকা (থাকলে) অপরিবর্তিত থাকবে।"
                    else
                        "আপনি কি নিশ্চিত যে '${target.name}'-এর $restrictRoleLabel পরিচয় রেস্ট্রিক্ট করতে চান? এর ফলে এই পরিচয়ে ব্যবহারকারীর কিছু কার্যকলাপ সীমিত থাকবে। অন্য ভূমিকা (থাকলে) অপরিবর্তিত থাকবে।",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminSetRestricted(target.id, !isCurrentlyRestricted, roleForRestrictToggle)
                        userForRestrictToggle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isCurrentlyRestricted) "সীমাবদ্ধতা তুলুন" else "রেস্ট্রিক্ট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { userForRestrictToggle = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // 3. Balance Adjust Dialog
    if (userForBalanceAdjust != null) {
        val target = userForBalanceAdjust!!
        // [ধাপ ৭ ফিক্স] generic `target.balance`-এর বদলে roleForBalanceAdjust অনুযায়ী
        // role-scoped ব্যালেন্স, লেবেল-সহ — যাতে dual-role ইউজারের ক্ষেত্রে admin স্পষ্ট দেখতে
        // পায় কোন role-এর ব্যালেন্স এখন adjust হতে যাচ্ছে।
        val balanceForRole = if (roleForBalanceAdjust == "SOLVER") target.balanceSolver else target.balanceUser
        val balanceRoleLabel = if (roleForBalanceAdjust == "SOLVER") "সলভার" else "গ্রাহক"
        BottomSlideAlertDialog(
            onDismissRequest = { userForBalanceAdjust = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SomadhanOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ব্যালেন্স সমন্বয় - ${target.name}", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "বর্তমান ব্যালেন্স ($balanceRoleLabel): ৳ ${DistanceUtil.toBengaliDigits(balanceForRole.toInt().toString())}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanOrange
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isBalanceAddition = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBalanceAddition) SomadhanSuccess else SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (isBalanceAddition) SomadhanSuccess else SomadhanDivider, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "+ যোগ করুন",
                                color = if (isBalanceAddition) Color.White else SomadhanTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { isBalanceAddition = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isBalanceAddition) SomadhanError else SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (!isBalanceAddition) SomadhanError else SomadhanDivider, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "- কর্তন করুন",
                                color = if (!isBalanceAddition) Color.White else SomadhanTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = balanceAmountInput,
                        onValueChange = { balanceAmountInput = it },
                        label = { Text("টাকার পরিমাণ (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = balanceReasonInput,
                        onValueChange = { balanceReasonInput = it },
                        label = { Text("কারণ (যেমন: রিফান্ড, বোনাস, জরিমানা)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = balanceAmountInput.trim().toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            val reason = balanceReasonInput.trim().ifBlank {
                                if (isBalanceAddition) "অ্যাডমিন কর্তৃক ব্যালেন্স ক্রেডিট" else "অ্যাডমিন কর্তৃক ব্যালেন্স ডেবিট"
                            }
                            // [ধাপ ৭ ফিক্স] roleForBalanceAdjust এখন এক্সপ্লিসিটলি পাস করা হচ্ছে।
                            viewModel.adminAdjustBalance(target.id, amount, isBalanceAddition, reason, role = roleForBalanceAdjust)
                            userForBalanceAdjust = null
                        } else {
                            viewModel.showToast("অনুগ্রহ করে সঠিক টাকার পরিমাণ প্রদান করুন")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { userForBalanceAdjust = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // 4. Role Change Dialog
    if (userForRoleChange != null) {
        val target = userForRoleChange!!
        val roles = listOf(
            Triple("USER", "গ্রাহক (Customer / Problem Poster)", "সমস্যা পোস্ট ও সমাধান গ্রহণ করতে পারেন"),
            Triple("SOLVER", "সমাধানকারী (Solver / Service Provider)", "সমস্যা সমাধান ও বিড পেশ করতে পারেন"),
            Triple("ADMIN", "অ্যাডমিন (System Administrator)", "সম্পূর্ণ অ্যাডমিন কন্ট্রোল ও ম্যানেজমেন্ট সুবিধা")
        )
        BottomSlideAlertDialog(
            onDismissRequest = { userForRoleChange = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = SomadhanInfo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("রোল পরিবর্তন - ${target.name}", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text("ব্যবহারকারীর প্রাথমিক ভূমিকা (Role) নির্বাচন করুন:", fontSize = 13.sp, color = SomadhanTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    roles.forEach { (roleKey, roleTitle, roleDesc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedNewRole = roleKey }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedNewRole == roleKey,
                                onClick = { selectedNewRole = roleKey },
                                colors = RadioButtonDefaults.colors(selectedColor = SomadhanOrange)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(roleTitle, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary, fontSize = 13.sp)
                                Text(roleDesc, fontSize = 11.sp, color = SomadhanTextHint)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminChangeRole(target.id, selectedNewRole)
                        userForRoleChange = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("পরিবর্তন করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { userForRoleChange = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // 5. Reputation Adjustment Dialog
    if (userForReputationAdjust != null) {
        val target = userForReputationAdjust!!
        // [ধাপ ৭খ ফিক্স] generic `target.reputationScore`-এর বদলে roleForReputationAdjust
        // অনুযায়ী role-scoped স্কোর, লেবেল-সহ — যাতে dual-role ইউজারের ক্ষেত্রে admin স্পষ্ট
        // দেখতে পায় কোন role-এর স্কোর এখন adjust হতে যাচ্ছে (balance-adjust dialog-এর ঠিক
        // একই ফিক্স)।
        val reputationForRole = if (roleForReputationAdjust == "SOLVER") target.reputationScoreSolver else target.reputationScoreUser
        val repRoleLabel = if (roleForReputationAdjust == "SOLVER") "সলভার" else "গ্রাহক"
        BottomSlideAlertDialog(
            onDismissRequest = { userForReputationAdjust = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SomadhanOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("রেপুটেশন সমন্বয় - ${target.name}", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "বর্তমান রেপুটেশন স্কোর ($repRoleLabel): ${DistanceUtil.toBengaliDigits(reputationForRole.toInt().toString())} / ১০০",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanOrange
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isRepAddition = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRepAddition) SomadhanSuccess else SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (isRepAddition) SomadhanSuccess else SomadhanDivider, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "+ স্কোর বৃদ্ধি",
                                color = if (isRepAddition) Color.White else SomadhanTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { isRepAddition = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isRepAddition) SomadhanError else SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (!isRepAddition) SomadhanError else SomadhanDivider, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "- স্কোর হ্রাস",
                                color = if (!isRepAddition) Color.White else SomadhanTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = repScoreChangeInput,
                        onValueChange = { repScoreChangeInput = it },
                        label = { Text("স্কোর পরিবর্তনের পরিমাণ (যেমন: ৫, ১০)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = repNoteInput,
                        onValueChange = { repNoteInput = it },
                        label = { Text("পরিবর্তনের বিবরণ / নোট") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val score = repScoreChangeInput.trim().toDoubleOrNull()
                        if (score != null && score > 0) {
                            val change = if (isRepAddition) score else -score
                            val note = repNoteInput.trim().ifBlank { "অ্যাডমিন কর্তৃক রেপুটেশন সমন্বয়" }
                            // [ধাপ ৭খ ফিক্স] roleForReputationAdjust এখন এক্সপ্লিসিটলি পাস করা
                            // হচ্ছে।
                            viewModel.adminAdjustReputation(target.id, change, note, role = roleForReputationAdjust)
                            userForReputationAdjust = null
                        } else {
                            viewModel.showToast("অনুগ্রহ করে সঠিক স্কোর মান প্রদান করুন")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { userForReputationAdjust = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // 6. Password Reset Dialog
    if (userForPasswordReset != null) {
        val target = userForPasswordReset!!
        BottomSlideAlertDialog(
            onDismissRequest = { userForPasswordReset = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SomadhanOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("পাসওয়ার্ড রিসেট - ${target.name}", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "এই ব্যবহারকারীর জন্য একটি নতুন পাসওয়ার্ড সেট করুন। ইউজার পরবর্তীতে এই পাসওয়ার্ড দিয়ে লগইন করতে পারবেন।",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("নতুন পাসওয়ার্ড (কমপক্ষে ৮ অক্ষর)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val plainPass = newPasswordInput.trim()
                        if (plainPass.length >= 8) {
                            viewModel.adminResetUserPassword(target.id, plainPass)
                            userForPasswordReset = null
                        } else {
                            viewModel.showToast("পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("রিসেট সম্পন্ন করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { userForPasswordReset = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // 7. Delete User Dialog
    if (userForDeleteConfirm != null) {
        val target = userForDeleteConfirm!!
        BottomSlideAlertDialog(
            onDismissRequest = { userForDeleteConfirm = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = SomadhanError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ব্যবহারকারী মুছে ফেলুন", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে '${target.name}' ব্যবহারকারীর অ্যাকাউন্টটি স্থায়ীভাবে মুছে ফেলতে চান? এই ক্রিয়াটি আর পূর্বাবস্থায় ফিরিয়ে আনা যাবে না।",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminDeleteUser(target.id)
                        if (selectedUser?.id == target.id) {
                            selectedUser = null
                        }
                        userForDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { userForDeleteConfirm = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // 8. KYC Reject Dialog
    if (kycRejectTargetUser != null) {
        val target = kycRejectTargetUser!!
        BottomSlideAlertDialog(
            onDismissRequest = {
                kycRejectTargetUser = null
                kycRejectReason = ""
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
                        value = kycRejectReason,
                        onValueChange = { kycRejectReason = it },
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
                        viewModel.adminRejectKyc(target.id, if (kycRejectReason.isBlank()) "তথ্য অস্পষ্ট" else kycRejectReason)
                        kycRejectTargetUser = null
                        kycRejectReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("বাতিল করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    kycRejectTargetUser = null
                    kycRejectReason = ""
                }) {
                    Text("ফিরে যান", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // 9. KYC Revoke Dialog
    if (kycRevokeTargetUser != null) {
        val target = kycRevokeTargetUser!!
        BottomSlideAlertDialog(
            onDismissRequest = {
                kycRevokeTargetUser = null
                kycRevokeReason = ""
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
                        text = "আপনি কি নিশ্চিত যে '${target.name}'-এর ভেরিফিকেশন প্রত্যাহার করতে চান?",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = kycRevokeReason,
                        onValueChange = { kycRevokeReason = it },
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
                        viewModel.adminRevokeKyc(target.id, if (kycRevokeReason.isBlank()) "অ্যাডমিন কর্তৃক ভেরিফিকেশন প্রত্যাহার" else kycRevokeReason)
                        kycRevokeTargetUser = null
                        kycRevokeReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("প্রত্যাহার করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    kycRevokeTargetUser = null
                    kycRevokeReason = ""
                }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // Main Screen Content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // [অফলাইন Action Gating ধাপ ১২] outbox pending sync ইন্ডিকেটর (non-blocking, খালি থাকলে
        // কিছুই দেখায় না) — সার্চ কার্ডের ঠিক ওপরে বসানো হলো।
        OutboxPendingIndicator(
            pendingCount = outboxPendingCount,
            onRetryClick = { viewModel.retryOutboxSyncNow() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        // Search Bar Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SomadhanOrange.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ইউনিফাইড ইউজার/সলভার ৩৬০° সার্চ", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SomadhanTextPrimary)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        // Auto-match exact UID/phone if typed
                        val q = it.trim()
                        if (q.isNotBlank()) {
                            // [SUPABASE-MIGRATED - E.164 ফলো-আপ ফিক্স ৩] লোকাল-ফরম্যাট টাইপ করলেও
                            // E.164-এ সেভ হওয়া phone-এর সাথে exact-match হোক।
                            val qNormalized = com.example.util.OtpService.normalizeTarget(q)
                            val exactMatch = allUsers.find { u ->
                                u.id.equals(q, ignoreCase = true) ||
                                    (u.displayUid.isNotBlank() && u.displayUid.equals(q, ignoreCase = true)) ||
                                    u.phone.equals(q, ignoreCase = true) || u.phone.equals(qNormalized, ignoreCase = true) ||
                                    u.email.equals(q, ignoreCase = true)
                            }
                            if (exactMatch != null) {
                                selectedUser = exactMatch
                            }
                        }
                    },
                    label = { Text("UID, ফোন নম্বর বা ইমেইল লিখুন...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SomadhanTextHint) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                selectedUser = null
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = SomadhanTextHint)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        unfocusedBorderColor = SomadhanDivider
                    )
                )

                // Search Results Dropdown Suggestions if searching and no exact match selected or typing
                if (searchQuery.isNotBlank() && matchingUsers.isNotEmpty() && (currentUser == null || !searchQuery.equals(currentUser.phone, ignoreCase = true))) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("পাওয়া ফলাফল (${matchingUsers.size} জন):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SomadhanOrange)
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SomadhanCardBg, RoundedCornerShape(8.dp))
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        matchingUsers.forEach { match ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedUser = match
                                        searchQuery = match.phone.ifBlank { match.name }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(match.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SomadhanTextPrimary)
                                    Text(
                                        "UID: ${match.displayUid.ifBlank { match.id.take(8) }} | ফোন: ${Formatters.toLocalDisplayFormat(match.phone)} | রোল: ${match.role}",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(16.dp))
                            }
                            if (match != matchingUsers.last()) {
                                Divider(color = SomadhanDivider, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Profile Details Area
        if (currentUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = SomadhanTextHint.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("ব্যবহারকারীর ৩৬০° তথ্য দেখতে সার্চ বক্সে UID, ফোন নম্বর বা ইমেইল লিখুন", color = SomadhanTextHint, fontSize = 13.sp)
                }
            }
        } else {
            val user = currentUser
            // [MONEY_FLOW_AND_ADMIN_BUGS ধাপ ৮] Ban/Restrict এখন role-scoped কলামে থাকে
            // (`is*User`/`is*Solver`)। এই স্ক্রিনে আগে থেকেই থাকা `userPerspectiveTab`
            // (0=User Perspective, 1=Solver Perspective — লাইন ~১১৭৪-এ PublicProfile
            // navigation-এও একই প্যাটার্নে ব্যবহৃত হয়) সিদ্ধান্ত নেয় কোন role-এর প্রেক্ষাপটে
            // ব্যান/রেস্ট্রিক্ট দেখানো/করা হবে — dual-card-এর মতো আলাদা wrapper class লাগেনি,
            // কারণ এই স্ক্রিনে একবারে একজন ইউজারই দেখানো হয়, পার্সপেক্টিভ ট্যাব দিয়ে role
            // নির্বাচন আগে থেকেই আছে।
            val currentPerspectiveRole = if (userPerspectiveTab == 1) "SOLVER" else "USER"
            val userBannedForPerspective = if (userPerspectiveTab == 1) user.isBannedSolver else user.isBannedUser
            val userRestrictedForPerspective = if (userPerspectiveTab == 1) user.isRestrictedSolver else user.isRestrictedUser

            // Calculate aggregated metrics using remember for performance
            val userProblems = remember(user.id, allProblems) {
                allProblems.filter { it.userId == user.id }
            }
            val userPostedCompleted = remember(userProblems) {
                userProblems.count { it.status.equals("COMPLETED", true) }
            }
            val userTotalSpent = remember(user.id, allTransactions) {
                allTransactions.filter { it.userId == user.id }.sumOf { it.grossAmount }
            }

            val solverProblems = remember(user.id, allProblems) {
                allProblems.filter { it.acceptedSolverId == user.id }
            }
            val solverCompleted = remember(solverProblems) {
                solverProblems.count { it.status.equals("COMPLETED", true) }
            }
            val solverDirectContracts = remember(solverProblems) {
                solverProblems.filter { it.isDirectContract }
            }
            val solverTotalEarned = remember(user.id, allTransactions) {
                allTransactions.filter { it.solverId == user.id }.sumOf { it.netAmount }
            }

            val userWithdrawals = remember(user.id, allWithdrawals) {
                allWithdrawals.filter { it.solverId == user.id }
            }
            val userEscrows = remember(user.id, allHeldEscrows, allRefundedEscrows) {
                (allHeldEscrows + allRefundedEscrows).filter { it.userId == user.id || it.solverId == user.id }
            }
            val userRatingsReceived = remember(user.id, allRatings) {
                allRatings.filter { it.solverId == user.id || it.userId == user.id }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Identity & Quick Profile Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header Row: Avatar, Name, Badges & Public Profile Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(SomadhanOrange.copy(alpha = 0.15f))
                                            .border(1.5.dp, SomadhanOrange, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val isPhotoValid = ImageStorageUtil.isValidDisplayUri(user.profileImageUri)
                                        if (isPhotoValid) {
                                            var loadFailed by remember(user.profileImageUri) { mutableStateOf(false) }
                                            if (!loadFailed) {
                                                AsyncImage(
                                                    model = user.profileImageUri,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop,
                                                    onError = { loadFailed = true }
                                                )
                                            } else {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(28.dp))
                                            }
                                        } else {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(28.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary)
                                            if (user.isKycVerified) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Default.Verified, contentDescription = "KYC Verified", tint = SomadhanSuccess, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "UID: ${user.displayUid.ifBlank { user.id }}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SomadhanOrange
                                        )
                                    }
                                }

                                // Public Profile Navigation
                                Button(
                                    onClick = {
                                        val roleParam = if (userPerspectiveTab == 1) "SOLVER" else "USER"
                                        onNavigate(Screen.PublicProfile.createRoute(user.id, roleParam))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("পাবলিক প্রোফাইল", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = SomadhanDivider, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Contact and Role details grid
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = SomadhanTextSecondary, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ফোন: ${if (user.phone.isBlank()) "প্রযোজ্য নয়" else Formatters.toLocalDisplayFormat(user.phone)}", fontSize = 12.sp, color = SomadhanTextPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = SomadhanTextSecondary, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ইমেইল: ${user.email.ifBlank { "প্রযোজ্য নয়" }}", fontSize = 12.sp, color = SomadhanTextPrimary)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    // [ধাপ ৭গ ফিক্স, ROLE_SEPARATION_AUDIT.md] আগে এই সামারি Text
                                    // `user.activeRoleBalance` পড়ত, যেটা অ্যাকাউন্টের বর্তমান সক্রিয়
                                    // role (user.role) অনুযায়ী balanceUser/balanceSolver বেছে নেয় —
                                    // admin কোন perspective ট্যাবে আছে সেটা বিবেচনা করত না। dual-role
                                    // ইউজারের ক্ষেত্রে (যখন user.role ≠ currentPerspectiveRole) এটা
                                    // ভুল সংখ্যা দেখাত, অথচ পাশের reputation score আর balance-adjust
                                    // ডায়ালগ ঠিকই currentPerspectiveRole ব্যবহার করত। এখন এই
                                    // Text-টাও currentPerspectiveRole অনুযায়ী role-scoped
                                    // balanceUser/balanceSolver দেখাচ্ছে, বাকি সব কার্ডের সাথে সামঞ্জস্যপূর্ণ।
                                    val perspectiveBalance = if (currentPerspectiveRole == "SOLVER") user.balanceSolver else user.balanceUser
                                    Text(
                                        "ব্যালেন্স: ৳ ${DistanceUtil.toBengaliDigits(perspectiveBalance.toInt().toString())}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrange
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // [ধাপ ৭খ ফিক্স, ROLE_SEPARATION_AUDIT.md] এই স্ক্রিনে আগে
                                    // থেকেই currentPerspectiveRole (userPerspectiveTab থেকে) দিয়ে
                                    // ban/restrict/balance-adjust role-context ঠিক করা আছে — কিন্তু
                                    // এই সামারি Text-টা এখনো শেয়ার্ড `user.reputationScore` পড়ত,
                                    // পার্সপেক্টিভ ট্যাব বদলালেও একই সংখ্যা দেখাত। এখন
                                    // currentPerspectiveRole অনুযায়ী role-scoped
                                    // reputationScoreUser/reputationScoreSolver দেখানো হচ্ছে।
                                    val perspectiveReputationScore = if (currentPerspectiveRole == "SOLVER") user.reputationScoreSolver else user.reputationScoreUser
                                    Text(
                                        "রেপুটেশন স্কোর: ${perspectiveReputationScore.toInt()} / ১০০",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (perspectiveReputationScore >= 70) SomadhanSuccess else if (perspectiveReputationScore >= 40) SomadhanOrange else SomadhanError
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Roles and Badges Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(SomadhanOrange.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("মূল রোল: ${user.role}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SomadhanOrange)
                                }

                                if (user.hasUserRole) {
                                    Box(
                                        modifier = Modifier
                                            .background(SomadhanInfo.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("গ্রাহক সক্রিয়", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SomadhanInfo)
                                    }
                                }

                                if (user.hasSolverRole) {
                                    Box(
                                        modifier = Modifier
                                            .background(SomadhanSuccess.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("সলভার সক্রিয়", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SomadhanSuccess)
                                    }
                                }

                                if (userBannedForPerspective) {
                                    Box(
                                        modifier = Modifier
                                            .background(SomadhanError.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("BANNED (${if (userPerspectiveTab == 1) "সলভার" else "গ্রাহক"})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SomadhanError)
                                    }
                                }
                                if (userRestrictedForPerspective) {
                                    Box(
                                        modifier = Modifier
                                            .background(SomadhanError.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("RESTRICTED (${if (userPerspectiveTab == 1) "সলভার" else "গ্রাহক"})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SomadhanError)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Admin Direct Action Buttons Panel
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("অ্যাডমিন অ্যাকশন ও নিয়ন্ত্রণ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SomadhanOrange)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Action buttons row 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        roleForBanToggle = currentPerspectiveRole
                                        userForBanToggle = user
                                    },
                                    enabled = AdminSession.canAct("users:users:ban"),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(if (userBannedForPerspective) Icons.Default.CheckCircle else Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(13.dp), tint = SomadhanOrange)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (userBannedForPerspective) "আনব্যান" else "ব্যান", fontSize = 11.sp, color = SomadhanTextPrimary)
                                }

                                OutlinedButton(
                                    onClick = {
                                        roleForRestrictToggle = currentPerspectiveRole
                                        userForRestrictToggle = user
                                    },
                                    enabled = AdminSession.canAct("users:users:restrict"),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(13.dp), tint = SomadhanOrange)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (userRestrictedForPerspective) "সীমাবদ্ধতা তুলুন" else "রেস্ট্রিক্ট", fontSize = 11.sp, color = SomadhanTextPrimary)
                                }

                                OutlinedButton(
                                    onClick = {
                                        balanceAmountInput = ""
                                        balanceReasonInput = ""
                                        isBalanceAddition = true
                                        // [ধাপ ৭ ফিক্স] ban/restrict বাটনের মতোই ক্লিকের মুহূর্তের
                                        // currentPerspectiveRole সংরক্ষণ করা হচ্ছে।
                                        roleForBalanceAdjust = currentPerspectiveRole
                                        userForBalanceAdjust = user
                                    },
                                    enabled = AdminSession.canAct("users:users:balance_adjust"),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(13.dp), tint = SomadhanOrange)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ব্যালেন্স", fontSize = 11.sp, color = SomadhanTextPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Action buttons row 2
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        selectedNewRole = user.role
                                        userForRoleChange = user
                                    },
                                    enabled = AdminSession.canAct("users:users:change_role"),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(13.dp), tint = SomadhanOrange)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("রোল", fontSize = 11.sp, color = SomadhanTextPrimary)
                                }

                                OutlinedButton(
                                    onClick = {
                                        repScoreChangeInput = ""
                                        repNoteInput = ""
                                        isRepAddition = true
                                        // [ধাপ ৭খ ফিক্স] ban/restrict/balance বাটনের মতোই
                                        // ক্লিকের মুহূর্তের currentPerspectiveRole সংরক্ষণ করা
                                        // হচ্ছে।
                                        roleForReputationAdjust = currentPerspectiveRole
                                        userForReputationAdjust = user
                                    },
                                    enabled = AdminSession.canAct("users:users:score_adjust"),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(13.dp), tint = SomadhanOrange)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("রেপুটেশন", fontSize = 11.sp, color = SomadhanTextPrimary)
                                }

                                OutlinedButton(
                                    onClick = {
                                        newPasswordInput = ""
                                        userForPasswordReset = user
                                    },
                                    enabled = AdminSession.canAct("users:users:reset_password"),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(13.dp), tint = SomadhanOrange)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("পাসওয়ার্ড", fontSize = 11.sp, color = SomadhanTextPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Action buttons row 3 (Delete + KYC)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { userForDeleteConfirm = user },
                                    enabled = AdminSession.canAct("users:users:delete"),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(13.dp), tint = SomadhanError)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("মুছে ফেলুন", fontSize = 11.sp, color = SomadhanError)
                                }

                                if (user.kycStatus == "pending" || !user.isKycVerified) {
                                    Button(
                                        onClick = { viewModel.adminApproveKyc(user.id) },
                                        enabled = AdminSession.canAct("users:kyc:approve"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("KYC অনুমোদন", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            kycRejectReason = ""
                                            kycRejectTargetUser = user
                                        },
                                        enabled = AdminSession.canAct("users:kyc:reject"),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(13.dp), tint = SomadhanError)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("KYC বাতিল", fontSize = 11.sp, color = SomadhanError)
                                    }
                                } else if (user.isKycVerified) {
                                    OutlinedButton(
                                        onClick = {
                                            kycRevokeReason = ""
                                            kycRevokeTargetUser = user
                                        },
                                        enabled = AdminSession.canAct("users:kyc:revoke"),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(13.dp), tint = SomadhanError)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("KYC প্রত্যাহার", fontSize = 11.sp, color = SomadhanError)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Perspective Switcher Tabs: "ইউজার হিসেবে" vs "সলভার হিসেবে"
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (userPerspectiveTab == 0) SomadhanOrange else SomadhanCardBg)
                                        .clickable {
                                            userPerspectiveTab = 0
                                            activeSubTab = 0
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (userPerspectiveTab == 0) Color.White else SomadhanTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ইউজার / ক্লায়েন্ট ভিউ (${userProblems.size} সমস্যা)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (userPerspectiveTab == 0) Color.White else SomadhanTextSecondary
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (userPerspectiveTab == 1) SomadhanOrange else SomadhanCardBg)
                                        .clickable {
                                            userPerspectiveTab = 1
                                            activeSubTab = 0
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Engineering,
                                            contentDescription = null,
                                            tint = if (userPerspectiveTab == 1) Color.White else SomadhanTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "সলভার ভিউ (${solverProblems.size} সমাধান)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (userPerspectiveTab == 1) Color.White else SomadhanTextSecondary
                                        )
                                    }
                                }
                            }

                            // Sub-tabs row
                            val subTabs = if (userPerspectiveTab == 0) {
                                listOf("সারসংক্ষেপ", "পোস্টকৃত সমস্যা (${userProblems.size})", "পেমেন্ট ও এসক্রো", "রিভিউসমূহ")
                            } else {
                                listOf("সারসংক্ষেপ", "গৃহীত কাজ ও কন্ট্রাক্ট (${solverProblems.size})", "উইথড্র ও লেনদেন (${userWithdrawals.size})", "রিভিউসমূহ")
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                subTabs.forEachIndexed { idx, label ->
                                    val isSelected = activeSubTab == idx
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (isSelected) SomadhanOrange.copy(alpha = 0.15f) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) SomadhanOrange else SomadhanDivider,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { activeSubTab = idx }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) SomadhanOrange else SomadhanTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Perspective Subtab Content Rendering
                if (userPerspectiveTab == 0) {
                    // USER / CLIENT PERSPECTIVE
                    when (activeSubTab) {
                        0 -> {
                            // User Summary
                            item {
                                val summaryPulse = rememberFieldChangePulse(
                                    value = listOf(user, userProblems, userPostedCompleted, userTotalSpent),
                                    isManualRefreshing = isRefreshing
                                )
                                PulsingValue(isUpdating = summaryPulse) {
                                    UserClientSummaryCard(
                                        user = user,
                                        problems = userProblems,
                                        completedCount = userPostedCompleted,
                                        totalSpent = userTotalSpent
                                    )
                                }
                            }
                        }
                        1 -> {
                            // User Problems List
                            if (userProblems.isEmpty()) {
                                item {
                                    EmptyStateView("এই ব্যবহারকারী কোনো সমস্যা পোস্ট করেননি।")
                                }
                            } else {
                                items(userProblems, key = { "up_${it.id}" }) { prob ->
                                    val rowPulse = rememberFieldChangePulse(value = prob, isManualRefreshing = isRefreshing)
                                    PulsingValue(isUpdating = rowPulse) {
                                        ProblemItemLookupCard(
                                            problem = prob,
                                            allUsers = allUsers,
                                            viewModel = viewModel,
                                            onNavigate = onNavigate
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            // User Escrow & Transactions
                            item {
                                val userTransactionsFiltered = allTransactions.filter { it.userId == user.id }
                                val userChargesFiltered = allAdditionalCharges.filter { it.userId == user.id }
                                val financialPulse = rememberFieldChangePulse(
                                    value = listOf(userEscrows, userTransactionsFiltered, userChargesFiltered),
                                    isManualRefreshing = isRefreshing
                                )
                                PulsingValue(isUpdating = financialPulse) {
                                    UserFinancialDetailsCard(
                                        userId = user.id,
                                        escrows = userEscrows,
                                        transactions = userTransactionsFiltered,
                                        additionalCharges = userChargesFiltered
                                    )
                                }
                            }
                        }
                        3 -> {
                            // User Reviews (Given & Received)
                            val reviews = userRatingsReceived.filter { it.userId == user.id }
                            if (reviews.isEmpty()) {
                                item {
                                    EmptyStateView("কোনো রিভিউ তথ্য পাওয়া যায়নি।")
                                }
                            } else {
                                items(reviews, key = { "ur_${it.id}" }) { r ->
                                    val rowPulse = rememberFieldChangePulse(value = r, isManualRefreshing = isRefreshing)
                                    PulsingValue(isUpdating = rowPulse) {
                                        RatingLookupCard(rating = r, onDelete = { viewModel.adminDeleteRating(r.id) })
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // SOLVER PERSPECTIVE
                    when (activeSubTab) {
                        0 -> {
                            // Solver Summary
                            item {
                                val summaryPulse = rememberFieldChangePulse(
                                    value = listOf(user, solverProblems, solverCompleted, solverDirectContracts.size, solverTotalEarned),
                                    isManualRefreshing = isRefreshing
                                )
                                PulsingValue(isUpdating = summaryPulse) {
                                    SolverSummaryCard(
                                        user = user,
                                        problems = solverProblems,
                                        completedCount = solverCompleted,
                                        directContractsCount = solverDirectContracts.size,
                                        totalEarned = solverTotalEarned
                                    )
                                }
                            }
                        }
                        1 -> {
                            // Solver Jobs / Contracts List
                            if (solverProblems.isEmpty()) {
                                item {
                                    EmptyStateView("এই সলভারের কোনো সমাধান বা কন্ট্রাক্ট পাওয়া যায়নি।")
                                }
                            } else {
                                items(solverProblems, key = { "sp_${it.id}" }) { prob ->
                                    val rowPulse = rememberFieldChangePulse(value = prob, isManualRefreshing = isRefreshing)
                                    PulsingValue(isUpdating = rowPulse) {
                                        ProblemItemLookupCard(
                                            problem = prob,
                                            allUsers = allUsers,
                                            viewModel = viewModel,
                                            onNavigate = onNavigate
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Solver Withdrawals & Transactions
                            item {
                                val solverTransactionsFiltered = allTransactions.filter { it.solverId == user.id }
                                val financialPulse = rememberFieldChangePulse(
                                    value = listOf(userWithdrawals, solverTransactionsFiltered),
                                    isManualRefreshing = isRefreshing
                                )
                                PulsingValue(isUpdating = financialPulse) {
                                    SolverFinancialDetailsCard(
                                        solverId = user.id,
                                        withdrawals = userWithdrawals,
                                        transactions = solverTransactionsFiltered,
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                        3 -> {
                            // Solver Reviews
                            val reviews = userRatingsReceived.filter { it.solverId == user.id }
                            if (reviews.isEmpty()) {
                                item {
                                    EmptyStateView("সলভার হিসেবে কোনো রিভিউ পাওয়া যায়নি।")
                                }
                            } else {
                                items(reviews, key = { "sr_${it.id}" }) { r ->
                                    val rowPulse = rememberFieldChangePulse(value = r, isManualRefreshing = isRefreshing)
                                    PulsingValue(isUpdating = rowPulse) {
                                        RatingLookupCard(rating = r, onDelete = { viewModel.adminDeleteRating(r.id) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Helper Components for Lookup ----------------

@Composable
private fun UserClientSummaryCard(
    user: UserEntity,
    problems: List<ProblemEntity>,
    completedCount: Int,
    totalSpent: Double
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("গ্রাহক হিস্ট্রি সারসংক্ষেপ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SomadhanTextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBox(label = "মোট পোস্ট", value = problems.size.toString(), modifier = Modifier.weight(1f))
                MetricBox(label = "সম্পন্ন কাজ", value = completedCount.toString(), modifier = Modifier.weight(1f))
                MetricBox(label = "মোট ব্যয়", value = "৳ ${totalSpent.toInt()}", modifier = Modifier.weight(1f), isHighlight = true)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = SomadhanDivider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Text("বর্তমান অ্যাকাউন্টের বিবরণ:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("ঠিকানা: ${user.address.ifBlank { "তথ্য নেই" }}", fontSize = 12.sp, color = SomadhanTextPrimary)
            Text("অ্যাকাউন্ট খোলার তারিখ: ${Formatters.formatDateBengali(user.createdAt)}", fontSize = 11.sp, color = SomadhanTextHint)
        }
    }
}

@Composable
private fun SolverSummaryCard(
    user: UserEntity,
    problems: List<ProblemEntity>,
    completedCount: Int,
    directContractsCount: Int,
    totalEarned: Double
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("সমাধানকারী (Solver) হিস্ট্রি সারসংক্ষেপ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SomadhanTextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBox(label = "মোট সমাধান", value = problems.size.toString(), modifier = Modifier.weight(1f))
                MetricBox(label = "সম্পন্ন", value = completedCount.toString(), modifier = Modifier.weight(1f))
                MetricBox(label = "কন্ট্রাক্ট", value = directContractsCount.toString(), modifier = Modifier.weight(1f))
                MetricBox(label = "মোট আয়", value = "৳ ${totalEarned.toInt()}", modifier = Modifier.weight(1f), isHighlight = true)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = SomadhanDivider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Text("দক্ষতা ও ক্যাটাগরি:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("ক্যাটাগরি আইডি: ${user.solverCategories.ifBlank { "সকল ক্যাটাগরি" }}", fontSize = 12.sp, color = SomadhanTextPrimary)
            Text("KYC স্ট্যাটাস: ${user.kycStatus.uppercase()} (ডকুমেন্ট: ${user.kycDocumentNumber ?: "নেই"})", fontSize = 12.sp, color = if (user.isKycVerified) SomadhanSuccess else SomadhanOrange)
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier = Modifier, isHighlight: Boolean = false) {
    Box(
        modifier = modifier
            .background(SomadhanCardBg, RoundedCornerShape(8.dp))
            .border(1.dp, if (isHighlight) SomadhanOrange.copy(alpha = 0.5f) else SomadhanDivider, RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isHighlight) SomadhanOrange else SomadhanTextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, color = SomadhanTextSecondary)
        }
    }
}

@Composable
private fun EmptyStateView(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, color = SomadhanTextHint)
    }
}

@Composable
private fun ProblemItemLookupCard(
    problem: ProblemEntity,
    allUsers: List<UserEntity>,
    viewModel: SomadhanViewModel,
    onNavigate: (String) -> Unit
) {
    var problemForChatView by remember { mutableStateOf<ProblemEntity?>(null) }
    var problemForBidsView by remember { mutableStateOf<ProblemEntity?>(null) }
    var problemForBudgetEdit by remember { mutableStateOf<ProblemEntity?>(null) }
    var problemForStatusChange by remember { mutableStateOf<ProblemEntity?>(null) }
    var problemForDeleteConfirm by remember { mutableStateOf<ProblemEntity?>(null) }

    if (problemForBudgetEdit != null) {
        AdminEditBudgetDialog(
            problem = problemForBudgetEdit!!,
            onUpdateBudget = { minBudget, maxBudget ->
                viewModel.adminUpdateProblemBudget(problem.id, minBudget, maxBudget)
                problemForBudgetEdit = null
            },
            onDismiss = { problemForBudgetEdit = null }
        )
    }

    if (problemForStatusChange != null) {
        AdminUpdateStatusDialog(
            problem = problemForStatusChange!!,
            onUpdateStatus = { status ->
                viewModel.adminUpdateProblemStatus(problem.id, status)
                problemForStatusChange = null
            },
            onDismiss = { problemForStatusChange = null }
        )
    }

    if (problemForBidsView != null) {
        AdminProblemBidsDialog(
            problem = problemForBidsView!!,
            viewModel = viewModel,
            onDismiss = { problemForBidsView = null }
        )
    }

    if (problemForChatView != null) {
        AdminProblemChatBottomSheet(
            problem = problemForChatView!!,
            viewModel = viewModel,
            onDismiss = { problemForChatView = null }
        )
    }

    if (problemForDeleteConfirm != null) {
        BottomSlideAlertDialog(
            onDismissRequest = { problemForDeleteConfirm = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = SomadhanError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সমস্যা মুছে ফেলা", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Text("আপনি কি নিশ্চিত যে এই সমস্যাটি মুছে ফেলতে চান?", fontSize = 13.sp, color = SomadhanTextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminDeleteProblem(problem.id)
                        problemForDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { problemForDeleteConfirm = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(problem.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SomadhanTextPrimary, maxLines = 2)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "ক্যাটাগরি: ${problem.categoryName} | ক্লায়েন্ট: ${problem.userName}",
                        fontSize = 11.sp,
                        color = SomadhanTextSecondary
                    )
                }
                StatusBadge(status = problem.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "বাজেট: ৳ ${problem.minBudget.toInt()} - ৳ ${problem.maxBudget.toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SomadhanOrange
                )
                if (!problem.acceptedSolverName.isNullOrBlank()) {
                    Text(
                        text = "সলভার: ${problem.acceptedSolverName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SomadhanInfo
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = SomadhanDivider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { problemForBidsView = problem },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("বিড (${problem.bidsCount})", fontSize = 10.sp, color = SomadhanTextPrimary)
                    }
                    OutlinedButton(
                        onClick = { problemForChatView = problem },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("চ্যাট", fontSize = 10.sp, color = SomadhanTextPrimary)
                    }
                    OutlinedButton(
                        onClick = { problemForStatusChange = problem },
                        enabled = AdminSession.canAct("work:problems:update_status"),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("স্ট্যাটাস", fontSize = 10.sp, color = SomadhanTextPrimary)
                    }
                    OutlinedButton(
                        onClick = { problemForBudgetEdit = problem },
                        enabled = AdminSession.canAct("work:problems:update_budget"),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("বাজেট", fontSize = 10.sp, color = SomadhanTextPrimary)
                    }
                }

                IconButton(
                    onClick = { problemForDeleteConfirm = problem },
                    enabled = AdminSession.canAct("work:problems:delete"),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "মুছে ফেলুন", tint = SomadhanError, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun UserFinancialDetailsCard(
    userId: String,
    escrows: List<EscrowEntity>,
    transactions: List<TransactionEntity>,
    additionalCharges: List<AdditionalChargeEntity>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("এসক্রো ও ট্রানজেকশন তালিকা", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SomadhanOrange)
            Spacer(modifier = Modifier.height(8.dp))

            if (escrows.isEmpty() && transactions.isEmpty()) {
                Text("কোনো আর্থিক লেনদেন রেকর্ড নেই।", fontSize = 12.sp, color = SomadhanTextHint)
            } else {
                if (escrows.isNotEmpty()) {
                    Text("এসক্রো রেকর্ড (${escrows.size}টি):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    escrows.take(5).forEach { esc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(esc.problemTitle.ifBlank { "Problem #${esc.problemId.take(6)}" }, fontSize = 12.sp, color = SomadhanTextPrimary)
                            Text("৳ ${(esc.baseAmount + esc.extraAmount).toInt()} (${esc.status})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (esc.status == "RELEASED") SomadhanSuccess else SomadhanOrange)
                        }
                    }
                    Divider(color = SomadhanDivider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (transactions.isNotEmpty()) {
                    Text("লেনদেন হিস্ট্রি (${transactions.size}টি):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    transactions.take(5).forEach { trx ->
                        val isRefund = TransactionHelper.isRefundTrx(trx)
                        val refundMeta = if (isRefund) TransactionHelper.getRefundMeta(trx) else null
                        val refundColor = when (refundMeta?.category) {
                            RefundCategory.SPLIT_REFUND -> Color(0xFF7E22CE)
                            RefundCategory.DISPUTE_REFUND -> Color(0xFFB91C1C)
                            else -> Color(0xFF1D4ED8)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = TransactionHelper.getDisplayTitle(trx),
                                    fontSize = 12.sp,
                                    color = SomadhanTextPrimary,
                                    maxLines = 1
                                )
                                if (isRefund && refundMeta != null) {
                                    Text(
                                        text = refundMeta.badgeBn,
                                        fontSize = 10.sp,
                                        color = refundColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRefund) "৳ ${trx.netAmount.toInt()} (${refundMeta?.labelBn ?: "রিফান্ড"})" else "৳ ${trx.grossAmount.toInt()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRefund) refundColor else SomadhanOrange
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SolverFinancialDetailsCard(
    solverId: String,
    withdrawals: List<WithdrawalEntity>,
    transactions: List<TransactionEntity>,
    viewModel: SomadhanViewModel
) {
    var singleWithdrawalToApprove by remember { mutableStateOf<WithdrawalEntity?>(null) }
    var singleWithdrawalToReject by remember { mutableStateOf<WithdrawalEntity?>(null) }
    var singleTrxId by remember { mutableStateOf("") }
    var rejectReasonInput by remember { mutableStateOf("") }

    if (singleWithdrawalToApprove != null) {
        val target = singleWithdrawalToApprove!!
        BottomSlideAlertDialog(
            onDismissRequest = { singleWithdrawalToApprove = null },
            title = { Text("উইথড্র অনুমোদন", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("উইথড্র ৳ ${target.amount.toInt()} (${target.method}: ${target.accountNumber}) অনুমোদন করতে চান?")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = singleTrxId,
                        onValueChange = { singleTrxId = it },
                        label = { Text("TrxID (ঐচ্ছিক)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminUpdateWithdrawalStatus(target, "COMPLETED", singleTrxId.ifBlank { null })
                        singleWithdrawalToApprove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess)
                ) {
                    Text("অনুমোদন করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { singleWithdrawalToApprove = null }) { Text("বাতিল") }
            }
        )
    }

    if (singleWithdrawalToReject != null) {
        val target = singleWithdrawalToReject!!
        BottomSlideAlertDialog(
            onDismissRequest = { singleWithdrawalToReject = null },
            title = { Text("উইথড্র বাতিল", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("উইথড্র ৳ ${target.amount.toInt()} বাতিল করতে চান?")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReasonInput,
                        onValueChange = { rejectReasonInput = it },
                        label = { Text("বাতিলের কারণ") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminUpdateWithdrawalStatus(target, "REJECTED", null)
                        singleWithdrawalToReject = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError)
                ) {
                    Text("বাতিল করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { singleWithdrawalToReject = null }) { Text("ফিরে যান") }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("উইথড্রয়াল ও কমিশন হিস্ট্রি", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SomadhanOrange)
            Spacer(modifier = Modifier.height(8.dp))

            if (withdrawals.isEmpty()) {
                Text("কোনো উইথড্র রিকোয়েস্ট নেই।", fontSize = 12.sp, color = SomadhanTextHint)
            } else {
                Text("উইথড্র রিকোয়েস্টসমূহ (${withdrawals.size}টি):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                withdrawals.forEach { w ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("উইথড্র আইডি: ${w.id}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = SomadhanOrange)
                                Text("৳ ${w.amount.toInt()} (${w.method}: ${w.accountNumber})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                                Text("স্ট্যাটাস: ${w.status} | ${Formatters.formatDateTimeBengali(w.createdAt)}", fontSize = 10.sp, color = SomadhanTextHint)
                            }
                            if (w.status == "PENDING") {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = {
                                            singleTrxId = ""
                                            singleWithdrawalToApprove = w
                                        },
                                        enabled = AdminSession.canAct("finance:withdrawals:approve"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Approve", fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = {
                                            rejectReasonInput = ""
                                            singleWithdrawalToReject = w
                                        },
                                        enabled = AdminSession.canAct("finance:withdrawals:reject"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Reject", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingLookupCard(rating: RatingEntity, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(rating.stars) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${rating.stars} স্টার", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanOrange)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(rating.comment.ifBlank { "(মন্তব্য নেই)" }, fontSize = 12.sp, color = SomadhanTextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text("প্রদত্তকারী: ${rating.userName} → সলভার: ${rating.solverName}", fontSize = 10.sp, color = SomadhanTextSecondary)
            }
            IconButton(
                onClick = onDelete,
                enabled = AdminSession.canAct("moderation:reviews:delete"),
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SomadhanError, modifier = Modifier.size(16.dp))
            }
        }
    }
}
