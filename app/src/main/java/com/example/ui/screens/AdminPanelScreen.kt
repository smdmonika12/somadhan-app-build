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
import android.util.Log
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
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
import androidx.compose.material.icons.filled.Gavel
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
import androidx.compose.material.icons.filled.Payment
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
import com.example.data.remote.SupabaseRealtimeManager
import com.example.data.security.AdminPermissionCatalog
import com.example.data.security.AdminSession
import com.example.data.repository.AdminDashboardMetrics
import com.example.ui.components.AdminAvatar
import com.example.ui.components.SupabaseConfigDialog
import com.example.ui.components.ListScreenSkeleton
import com.example.ui.components.LoadingAwareContent
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.SyncAwareContent
import com.example.ui.components.SyncAwareRefreshableContent
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.DegradedAdminSessionBar
import com.example.ui.components.ReputationBadge
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.navigation.Screen
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanAdminSlate
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.example.ui.components.BottomSlideAlertDialog

@Composable
fun AdminPanelScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    // allCategories moved to tabs 6/21's branches below (§11 part 4) -- 2-tab usage, neither
    // touched by Tab 0, so hoisting kept it loading even when the admin never opened those tabs.
    // allWithdrawals moved to tabs 2/18's branches below (§11 part 4) -- same reasoning.
    // allAdminNotifications moved to tab 3's branch below (§11 follow-up) -- single-tab usage,
    // WhileSubscribed(5000) means it now stops loading when this tab isn't open.
    val allHeldEscrows by viewModel.allHeldEscrows.collectAsStateWithLifecycle()
    // allReleasedEscrows moved to tabs 7/21's branches below (§11 part 4).
    // allRefundedEscrows moved to tabs 7/18/21's branches below (§11 part 4) -- turned out to be
    // 3-tab usage, not 2 as originally estimated; verified by grep before moving.
    // allCancelledBids moved to tabs 14/18's branches below (§11 part 4).
    val allAdditionalCharges by viewModel.allAdditionalCharges.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    // allGatewayPayments moved to tabs 9/23's branches below (§11 part 4).
    // allRatings moved to tabs 10/18's branches below (§11 part 4).
    // allFaqs and recentAuditLogs moved to tabs 13/12's branches below (§11 follow-up) --
    // each is single-tab usage, so hoisting them here kept their Room queries running even
    // when the admin was never on the FAQ/Audit-Log tab.
    val adminMetrics by viewModel.adminDashboardMetrics.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val commissionPercent by viewModel.platformCommissionPercent.collectAsStateWithLifecycle()
    val physicalCategoryRadiusKm by viewModel.physicalCategoryRadiusKm.collectAsStateWithLifecycle()
    val isPhysicalWorkEnabled by viewModel.isPhysicalWorkEnabled.collectAsStateWithLifecycle()
    val isVirtualWorkEnabled by viewModel.isVirtualWorkEnabled.collectAsStateWithLifecycle()
    // [Somadhan Bug-Fix Step 6 — গ্রুপ ৩.১c] degraded (local-only) admin-session persistent সতর্কতা।
    val isDegradedAdminSession by viewModel.isDegradedAdminSession.collectAsStateWithLifecycle()
    val degradedAdminSessionReason by viewModel.degradedAdminSessionReason.collectAsStateWithLifecycle()
    // [ADMIN_ROLE_PROFILE সেশন ২] heartbeat-এ সার্ভার জানালে (অ্যাকাউন্ট নিষ্ক্রিয়/এডমিন-অ্যাক্সেস নেই)
    // সাথে সাথে টোস্ট + লগআউট — সুপার অ্যাডমিন কাউকে deactivate করলে সে ~৩০ সেকেন্ডের মধ্যে বেরিয়ে যায়।
    val adminForcedLogoutReason by viewModel.adminForcedLogoutReason.collectAsStateWithLifecycle()
    // [ADMIN_ROLE_PROFILE সেশন ৩ — অংশ ২.২] কে লগইন করেছে — সুপার হলেই "অ্যাডমিন ব্যবস্থাপনা" ড্রয়ার-গ্রুপ
    // দেখা যায়। heartbeat-এ রোল বদলালে (সুপার হারালে) রিলগইন ছাড়াই গ্রুপটা সরে যায়।
    val adminSessionState by AdminSession.state.collectAsStateWithLifecycle()
    // [ADMIN_ROLE_PROFILE সেশন ৬] প্রোফাইল ওভারলে (সুপার হলে "সব এডমিন" তালিকা দেখাতে) টপ-লেভেলে দরকার —
    // অ্যাকাউন্ট-ট্যাব (২৬)-এর নিজস্ব কপির পাশাপাশি এই দ্বিতীয় collectAsStateWithLifecycle() নিরাপদ (একই
    // StateFlow-এর আরেকটা সাবস্ক্রাইবার মাত্র, নতুন লোড ট্রিগার করে না — লোড এখনো শুধু অ্যাকাউন্ট-ট্যাব/এই
    // ওভারলে খোলাতেই হয়, নিচে LaunchedEffect(showAdminProfileOverlay)-এ)।
    val adminAccountsForProfile by viewModel.adminAccounts.collectAsStateWithLifecycle()
    val isSuperAdmin = adminSessionState?.account?.isSuper == true
    androidx.compose.runtime.LaunchedEffect(adminForcedLogoutReason) {
        val reason = adminForcedLogoutReason
        if (reason != null) {
            viewModel.showToast(reason)
            viewModel.consumeAdminForcedLogout()
            onLogout()
        }
    }

    // Loading/Sync Fix Roadmap v2, ধাপ ৪ — withdrawals/escrows/transactions টেবিল
    // initialSyncPhase-এর (bulk-pull) অংশ। প্রতিটা ট্যাবের নিজস্ব আলাদা "_sync" sessionKey
    // (বিদ্যমান admin_withdrawals/admin_escrow/admin_transactions key-র সাথে সংঘর্ষ এড়াতে,
    // WithdrawalHistoryScreen/MessagesScreen-এর ধাপ ৪-এর মতোই কারণ)।
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()
    // Loading/Sync Fix Roadmap v2, ধাপ ৬ — AdminPanelScreen-এর ১৩টা ট্যাবে (Overview,
    // Withdrawals, Users, Problems, Escrow, Transactions, ChatMonitoring, DirectContracts,
    // UserLookup, SolverQuota, InstantJobs, DisputeCenter, GatewayPayments) আগে pull-to-refresh
    // ছিল না। প্রতিটা ট্যাবের জন্য আলাদা wrapper না বসিয়ে, সবচেয়ে কম-ঝুঁকিপূর্ণ উপায় হিসেবে পুরো
    // `when (selectedTabIndex) {...}` ব্লকটাকেই একটা একক SomadhanPullToRefresh দিয়ে মুড়ানো
    // হয়েছে (নিচে) -- কারণ ট্যাবগুলোর প্রায় সবকটাই একই initialSyncPhase-এর উপর নির্ভরশীল, তাই
    // একটা শেয়ার্ড retry-if-ERROR handler-ই যথেষ্ট। বিদ্যমান "Force Sync" আইকন-বাটন
    // (viewModel.triggerCloudSync(), টপ-বারে) এই পরিবর্তনে অক্ষত রাখা হয়েছে -- সেটা একটা
    // সম্পূর্ণ ভিন্ন, ব্যাপক push+pull অপারেশন, pull-to-refresh-এর retry-if-ERROR-এর সাথে গুলিয়ে
    // ফেলা হয়নি।
    //
    // [বাগফিক্স, ব্যবহারকারীর রিপোর্ট] উপরের ডিজাইনে একটা বাস্তব বাগ ছিল: `initialSyncPhase`
    // প্রায় সবসময়ই `LOADED` থাকে (শুধু আসল নেটওয়ার্ক এরর হলেই `ERROR` হয়) — তাই স্বাভাবিক
    // অবস্থায় (যা প্রায় সবসময়) পুল-টু-রিফ্রেশ টানলে এই হ্যান্ডলার **কিছুই করতো না**, `isRefreshing`
    // কখনো `true`-ই হতো না, ফলে `SomadhanPullToRefresh`/`PullToRefreshBox`-এর ইনডিকেটর ছেড়ে দেওয়ার
    // সাথে সাথেই কোনো প্রকৃত রিফ্রেশ-সাইকেল ছাড়াই উপরে উঠে হারিয়ে যেত (রিপোর্ট: "স্পিনার নিচে আসে
    // কিন্তু রিফ্রেশ হয় না")। ফিক্স: এখন স্বাভাবিক অবস্থায়ও `triggerCloudSync()` (টপ-বারের Force
    // Sync বাটনের মতোই, `_isRefreshing` ঠিকভাবে true→false সেট করে) কল হবে — এটাই `isRefreshing`-কে
    // আসলেই ড্রাইভ করে, তাই ইনডিকেটর ঠিকভাবে স্পিন করে ও কাজ শেষ হলেই বন্ধ হবে; পাশাপাশি প্রতিটা
    // ট্যাবের বিদ্যমান `isManualRefreshing = isRefreshing` per-card pulse-ও এখন আসলেই ট্রিগার
    // হবে, আগে যা কখনো হতো না। `ERROR` অবস্থায় আগের মতোই `retryInitialSync()`-ও কল হয় (আলাদা
    // রিকভারি পাথ, `triggerCloudSync()`-এর পাশাপাশি)।
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // [MONEY_FLOW_AND_ADMIN_BUGS ধাপ ৭ — বাগ C ফিক্স] আগে এখানে সরাসরি triggerCloudSync()
    // (টপ-বারের Force Sync বাটনের মতোই ফুল push+pull, ৪৫s পর্যন্ত) কল হতো — এখন স্কোপড
    // viewModel.refreshAdminTab(selectedTabIndex) কল হচ্ছে (user/solver-সাইড refreshData()-এর
    // প্যাটার্নে, শুধু হালকা health-check fallback + active ট্যাবের non-realtime ডেটা রিফ্রেশ)।
    // retry-if-ERROR লজিক refreshAdminTab()-এর ভেতরেই মুভ করা হয়েছে (আচরণ অপরিবর্তিত)। টপ-বারের
    // Force Sync বাটন (viewModel.triggerCloudSync(), লাইন ~৯৩৬) ইচ্ছাকৃতভাবে অক্ষত রাখা হয়েছে।
    val onAdminPullToRefresh: () -> Unit = {
        viewModel.refreshAdminTab(selectedTabIndex)
    }

    data class AdminDrawerItem(
        val index: Int,
        val title: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector
    )

    data class AdminDrawerGroup(
        val groupTitle: String,
        val groupIcon: androidx.compose.ui.graphics.vector.ImageVector,
        val items: List<AdminDrawerItem>
    )

    val baseDrawerGroups = listOf(
        AdminDrawerGroup(
            groupTitle = "ড্যাশবোর্ড",
            groupIcon = Icons.Default.BarChart,
            items = listOf(
                AdminDrawerItem(0, "পরিসংখ্যান ও চার্ট", Icons.Default.BarChart)
            )
        ),
        AdminDrawerGroup(
            groupTitle = "ইউজার ব্যবস্থাপনা",
            groupIcon = Icons.Default.Group,
            items = listOf(
                AdminDrawerItem(18, "ইউজার/সলভার সার্চ", Icons.Default.Search),
                AdminDrawerItem(19, "সলভার কোটা", Icons.Default.EmojiEvents),
                AdminDrawerItem(4, "ইউজারগণ", Icons.Default.Group),
                AdminDrawerItem(1, "কেওয়াইসি", Icons.Default.VerifiedUser),
                AdminDrawerItem(12, "অ্যাক্টিভিটি লগ", Icons.Default.History)
            )
        ),
        AdminDrawerGroup(
            groupTitle = "কাজ ও চুক্তি",
            groupIcon = Icons.Default.Assignment,
            items = listOf(
                AdminDrawerItem(5, "সমস্যাসমূহ", Icons.Default.Assignment),
                AdminDrawerItem(21, "জরুরি / ইনস্ট্যান্ট জবস", Icons.Default.FlashOn),
                AdminDrawerItem(14, "বিড বাতিলের ইতিহাস", Icons.Default.Cancel),
                AdminDrawerItem(16, "সরাসরি চুক্তি প্রজেক্ট", Icons.Default.Assignment),
                AdminDrawerItem(15, "চ্যাট মনিটরিং", Icons.Default.Chat)
            )
        ),
        AdminDrawerGroup(
            groupTitle = "আর্থিক ব্যবস্থাপনা",
            groupIcon = Icons.Default.AccountBalanceWallet,
            items = listOf(
                AdminDrawerItem(2, "উইথড্রয়াল", Icons.Default.AccountBalanceWallet),
                AdminDrawerItem(7, "Escrow", Icons.Default.Shield),
                AdminDrawerItem(8, "অতিরিক্ত চার্জ", Icons.Default.PostAdd),
                AdminDrawerItem(9, "লেনদেন হিস্ট্রি", Icons.Default.ReceiptLong),
                AdminDrawerItem(23, "অনলাইন গেটওয়ে লেনদেন", Icons.Default.Payment)
            )
        ),
        AdminDrawerGroup(
            groupTitle = "কনফিগারেশন",
            groupIcon = Icons.Default.Settings,
            items = listOf(
                AdminDrawerItem(6, "ক্যাটাগরি", Icons.Default.Category),
                AdminDrawerItem(20, "রেপুটেশন ইঞ্জিন", Icons.Default.EmojiEvents),
                AdminDrawerItem(11, "সেটিংস", Icons.Default.Settings),
                AdminDrawerItem(13, "FAQ ম্যানেজমেন্ট", Icons.Default.QuestionAnswer)
            )
        ),
        AdminDrawerGroup(
            groupTitle = "বিরোধ ব্যবস্থাপনা",
            groupIcon = Icons.Default.Gavel,
            items = listOf(
                AdminDrawerItem(22, "বিরোধ কেন্দ্র", Icons.Default.Gavel)
            )
        ),
        AdminDrawerGroup(
            groupTitle = "যোগাযোগ",
            groupIcon = Icons.Default.NotificationsActive,
            items = listOf(
                AdminDrawerItem(3, "বিজ্ঞপ্তি প্রেরণ", Icons.Default.NotificationsActive)
            )
        ),
        AdminDrawerGroup(
            groupTitle = "মডারেশন",
            groupIcon = Icons.Default.RateReview,
            items = listOf(
                AdminDrawerItem(10, "রিভিউ মডারেশন", Icons.Default.RateReview)
            )
        ),
        AdminDrawerGroup(
            groupTitle = "সিস্টেম / অ্যাডভান্সড",
            groupIcon = Icons.Default.Storage,
            items = listOf(
                AdminDrawerItem(17, "Supabase ডাটা এক্সপ্লোরার", Icons.Default.Storage),
                AdminDrawerItem(24, "রিফান্ড ডায়াগনস্টিক (Debug)", Icons.Default.BugReport)
            )
        )
    )

    // [ADMIN_ROLE_PROFILE সেশন ৩ — অংশ ২.২] "অ্যাডমিন ব্যবস্থাপনা" — কন্ডিশনালি রেন্ডার (শুধু সুপার)।
    // এই ড্রয়ারের প্রথম conditional গ্রুপ। সেশন ৪-এ দ্বিতীয় আইটেম "এডমিন অ্যাকাউন্ট" (ইনডেক্স ২৬) যোগ হয়েছে,
    // তাই এখন এটা গ্রুপ-শিরোনামসহ expandable (একাধিক আইটেম)।
    val drawerGroups = if (isSuperAdmin) {
        baseDrawerGroups + AdminDrawerGroup(
            groupTitle = "অ্যাডমিন ব্যবস্থাপনা",
            groupIcon = Icons.Default.AdminPanelSettings,
            items = listOf(
                AdminDrawerItem(
                    AdminPermissionCatalog.ROLE_MGMT_TAB_INDEX,
                    "রোল ম্যানেজমেন্ট",
                    Icons.Default.AdminPanelSettings
                ),
                AdminDrawerItem(
                    AdminPermissionCatalog.ADMIN_ACCOUNTS_TAB_INDEX,
                    "এডমিন অ্যাকাউন্ট",
                    Icons.Default.People
                ),
                AdminDrawerItem(
                    AdminPermissionCatalog.ACTIVITY_LOG_TAB_INDEX,
                    "অ্যাক্টিভিটি লগ",
                    Icons.Default.History
                )
            )
        )
    } else {
        baseDrawerGroups
    }

    val drawerItems = drawerGroups.flatMap { it.items }

    var expandedGroupTitles by rememberSaveable {
        val initialGroup = drawerGroups.find { group -> group.items.size > 1 && group.items.any { it.index == selectedTabIndex } }
        mutableStateOf(if (initialGroup != null) setOf(initialGroup.groupTitle) else emptySet())
    }

    // Automatically expand only the group containing the currently selected tab, and collapse if navigating to a single item
    LaunchedEffect(selectedTabIndex) {
        val currentGroup = drawerGroups.find { group -> group.items.size > 1 && group.items.any { it.index == selectedTabIndex } }
        expandedGroupTitles = if (currentGroup != null) {
            setOf(currentGroup.groupTitle)
        } else {
            emptySet()
        }
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryForEditDialog by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddFaqDialog by remember { mutableStateOf(false) }
    var addFaqTargetAudience by remember { mutableStateOf("USER") }
    var faqForEditDialog by remember { mutableStateOf<FaqEntity?>(null) }
    var showAdminLogoutDialog by remember { mutableStateOf(false) }
    var showCloudConfigDialog by remember { mutableStateOf(false) }
    // [ADMIN_ROLE_PROFILE সেশন ৬] টপ-বারের অবতারে ট্যাপ করলে খোলে — সব এডমিনের জন্য (সুপার সীমাবদ্ধ না)।
    var showAdminProfileOverlay by remember { mutableStateOf(false) }

    // [SUPABASE-MIGRATED - ধাপ ৩৩.৩ কাজ ৫] আগে এখানে FirebaseSyncManager.startRealtimeListeners()
    // ও .pullAllCloudDataToLocal() ও কল হতো, নিচের SupabaseRealtimeManager কলের পাশাপাশি (dual-run)।
    // এই স্ক্রিন-ওপেন মুহূর্তের admin bulk-pull/listener-startup-টা এখন সম্পূর্ণভাবে
    // SupabaseRealtimeManager দিয়েই হয় (pullBulkDataFromSupabase() Firebase-এর
    // pullAllCloudDataToLocal()-এর সমতুল্য, startRealtimeListeners() একই নামের সমতুল্য ফাংশন) —
    // Firebase কলদুটো নিরাপদে সরানো হয়েছে, কারণ এটা ছিল pure dual-run (একই কাজ দুইবার), একমুখী
    // নির্ভরতা না। ⚠️ জানা সীমাবদ্ধতা (ধাপ ১৪-এ flag করা, MIGRATION_PROGRESS.md-এ বিস্তারিত):
    // "ADMIN_SYSTEM" ফিক্সড-আইডি demo-অ্যাডমিন লগইন পথে কোনো real Supabase Auth session নেই,
    // তাই সেই সেশনে RLS admin-scoping কাজ করবে না (anon-level visibility-ই পাবে) -- একজন
    // সত্যিকারের registered ব্যবহারকারীকে `admin_change_role` RPC দিয়ে ADMIN role দেওয়া হলে
    // (real Supabase Auth session সহ) এই কল স্বাভাবিকভাবে কাজ করবে।
    LaunchedEffect(Unit) {
        runCatching {
            SupabaseRealtimeManager.startRealtimeListeners()
            SupabaseRealtimeManager.pullBulkDataFromSupabase()
        }.onFailure { Log.w("AdminPanelScreen", "Supabase admin refresh failed: ${it.message}") }
    }

    if (showCloudConfigDialog) {
        SupabaseConfigDialog(
            viewModel = viewModel,
            onDismiss = { showCloudConfigDialog = false }
        )
    }

    val myAccountForProfile = adminSessionState?.account
    if (showAdminProfileOverlay && myAccountForProfile != null) {
        // সুপার হলে বাম কলামের "সব এডমিন" তালিকার জন্য — অ্যাকাউন্ট-ট্যাবে না গিয়েও সরাসরি প্রোফাইল খুললে তালিকা যেন ফাঁকা না থাকে।
        LaunchedEffect(showAdminProfileOverlay) { if (isSuperAdmin) viewModel.loadAdminAccounts() }
        Dialog(
            onDismissRequest = { showAdminProfileOverlay = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AdminProfileView(
                viewModel = viewModel,
                currentAdminId = myAccountForProfile.id,
                isSuper = isSuperAdmin,
                allAccounts = if (isSuperAdmin) adminAccountsForProfile else listOf(myAccountForProfile),
                onBack = { showAdminProfileOverlay = false }
            )
        }
    }

    if (showAdminLogoutDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showAdminLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = SomadhanError,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "অ্যাডমিন লগআউট নিশ্চিতকরণ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে অ্যাডমিন প্যানেল থেকে লগআউট করতে চান? পুনরায় লগইন করতে অ্যাডমিন মোবাইল ও পাসওয়ার্ড প্রয়োজন হবে।",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAdminLogoutDialog = false
                        viewModel.showToast("অ্যাডমিন প্যানেল থেকে সফলভাবে লগআউট হয়েছে।")
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("admin_drawer_confirm_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("লগআউট করুন", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAdminLogoutDialog = false },
                    modifier = Modifier.testTag("admin_drawer_cancel_logout_button")
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary, fontWeight = FontWeight.Medium)
                }
            },
            containerColor = SomadhanCardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showAddCategoryDialog) {
        CategoryFormDialog(
            categoryToEdit = null,
            radiusKm = physicalCategoryRadiusKm,
            onDismiss = { showAddCategoryDialog = false },
            onSave = { newCategory ->
                viewModel.adminAddCategory(newCategory)
                showAddCategoryDialog = false
            }
        )
    }

    if (categoryForEditDialog != null) {
        CategoryFormDialog(
            categoryToEdit = categoryForEditDialog,
            radiusKm = physicalCategoryRadiusKm,
            onDismiss = { categoryForEditDialog = null },
            onSave = { updatedCategory ->
                viewModel.adminUpdateCategory(updatedCategory)
                categoryForEditDialog = null
            }
        )
    }

    if (showAddFaqDialog) {
        FaqFormDialog(
            faqToEdit = null,
            defaultAudience = addFaqTargetAudience,
            onDismiss = { showAddFaqDialog = false },
            onSave = { newFaq ->
                viewModel.adminAddFaq(
                    question = newFaq.question,
                    answer = newFaq.answer,
                    targetAudience = newFaq.targetAudience,
                    displayOrder = newFaq.displayOrder,
                    isActive = newFaq.isActive
                )
                showAddFaqDialog = false
            }
        )
    }

    if (faqForEditDialog != null) {
        FaqFormDialog(
            faqToEdit = faqForEditDialog,
            onDismiss = { faqForEditDialog = null },
            onSave = { updatedFaq ->
                viewModel.adminUpdateFaq(updatedFaq)
                faqForEditDialog = null
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SomadhanBg,
                drawerContentColor = SomadhanTextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                // Header section: "সমাধান" ব্র্যান্ড নাম + "অ্যাডমিন কন্ট্রোল প্যানেল" সাবটাইটেল
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(SomadhanOrange, Color(0xFFE65100))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column {
                        Text(
                            text = "সমাধান",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "অ্যাডমিন কন্ট্রোল প্যানেল",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Divider(color = SomadhanDivider, thickness = 1.dp)

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    drawerGroups.forEach { group ->
                        if (group.items.size == 1) {
                            val item = group.items.first()
                            val isSelected = selectedTabIndex == item.index
                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = if (isSelected) SomadhanOrange else SomadhanTextSecondary
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    selectedTabIndex = item.index
                                    expandedGroupTitles = emptySet()
                                    coroutineScope.launch {
                                        drawerState.close()
                                    }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = SomadhanOrange.copy(alpha = 0.12f),
                                    unselectedContainerColor = Color.Transparent,
                                    selectedTextColor = SomadhanOrange,
                                    unselectedTextColor = SomadhanTextSecondary,
                                    selectedIconColor = SomadhanOrange,
                                    unselectedIconColor = SomadhanTextSecondary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .testTag("admin_drawer_item_${item.index}")
                            )
                        } else {
                            val isExpanded = expandedGroupTitles.contains(group.groupTitle)
                            val hasSelectedChild = group.items.any { it.index == selectedTabIndex }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (hasSelectedChild && !isExpanded) SomadhanOrange.copy(alpha = 0.08f) else Color.Transparent)
                                        .clickable {
                                            expandedGroupTitles = if (isExpanded) {
                                                emptySet()
                                            } else {
                                                setOf(group.groupTitle)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = group.groupIcon,
                                                contentDescription = null,
                                                tint = if (hasSelectedChild) SomadhanOrange else SomadhanTextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = group.groupTitle,
                                                fontSize = 13.sp,
                                                fontWeight = if (hasSelectedChild) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (hasSelectedChild) SomadhanOrange else SomadhanTextPrimary
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isExpanded) "সংকোচন করুন" else "প্রসারিত করুন",
                                            tint = if (hasSelectedChild) SomadhanOrange else SomadhanTextHint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, top = 2.dp, bottom = 4.dp)
                                    ) {
                                        group.items.forEach { item ->
                                            val isSelected = selectedTabIndex == item.index
                                            NavigationDrawerItem(
                                                icon = {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = item.title,
                                                        tint = if (isSelected) SomadhanOrange else SomadhanTextSecondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = item.title,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 13.sp
                                                    )
                                                },
                                                selected = isSelected,
                                                onClick = {
                                                    selectedTabIndex = item.index
                                                    expandedGroupTitles = setOf(group.groupTitle)
                                                    coroutineScope.launch {
                                                        drawerState.close()
                                                    }
                                                },
                                                colors = NavigationDrawerItemDefaults.colors(
                                                    selectedContainerColor = SomadhanOrange.copy(alpha = 0.12f),
                                                    unselectedContainerColor = Color.Transparent,
                                                    selectedTextColor = SomadhanOrange,
                                                    unselectedTextColor = SomadhanTextSecondary,
                                                    selectedIconColor = SomadhanOrange,
                                                    unselectedIconColor = SomadhanTextSecondary
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 1.dp)
                                                    .testTag("admin_drawer_item_${item.index}")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = SomadhanDivider, thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "লগআউট",
                                tint = SomadhanError
                            )
                        },
                        label = {
                            Text(
                                text = "লগআউট (Logout)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SomadhanError
                            )
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                            }
                            showAdminLogoutDialog = true
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = SomadhanError.copy(alpha = 0.08f),
                            unselectedTextColor = SomadhanError,
                            unselectedIconColor = SomadhanError
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .testTag("admin_drawer_logout_item")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "মূল অ্যাপে ফিরুন",
                                tint = SomadhanTextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = "মূল অ্যাপে ফিরুন",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                            }
                            onNavigateBack()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = SomadhanTextSecondary,
                            unselectedIconColor = SomadhanTextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            val currentItem = drawerItems.find { it.index == selectedTabIndex }
                            val currentGroup = drawerGroups.find { group -> group.items.any { it.index == selectedTabIndex } }
                            Column {
                                Text(
                                    text = "অ্যাডমিন কন্ট্রোল প্যানেল",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = SomadhanTextPrimary
                                )
                                val subtitleText = if (currentGroup != null && currentGroup.items.size > 1 && currentGroup.groupTitle != currentItem?.title) {
                                    "${currentGroup.groupTitle} • ${currentItem?.title ?: ""}"
                                } else {
                                    currentItem?.title ?: "ড্যাশবোর্ড"
                                }
                                Text(
                                    text = subtitleText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanOrange,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        drawerState.open()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "মেনু খুলুন",
                                    tint = SomadhanTextPrimary
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showCloudConfigDialog = true },
                                modifier = Modifier.testTag("admin_top_bar_cloud_config_button")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "ক্লাউড প্রজেক্ট সেটিংস", tint = SomadhanOrange)
                            }
                            IconButton(
                                onClick = { viewModel.triggerCloudSync() },
                                enabled = !isRefreshing
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SomadhanOrange, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = "সিঙ্ক করুন", tint = SomadhanOrange)
                                }
                            }
                            // [ADMIN_ROLE_PROFILE সেশন ৬] সবার জন্য (সুপার সীমাবদ্ধ না) — প্রোফাইল খোলে।
                            adminSessionState?.account?.let { me ->
                                IconButton(
                                    onClick = { showAdminProfileOverlay = true },
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .testTag("admin_top_bar_profile_avatar")
                                ) {
                                    AdminAvatar(
                                        name = me.name,
                                        photo = me.photoUrl,
                                        isSuper = me.isSuper,
                                        size = 30.dp,
                                        ring = true
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = SomadhanBg),
                        modifier = Modifier.border(1.dp, SomadhanDivider)
                    )
                    RealtimeLocationBar(
                        locationAddress = liveLocation.address,
                        onRefresh = { viewModel.refreshLiveLocation() }
                    )
                    // [Somadhan Bug-Fix Step 6 — গ্রুপ ৩.১c] RealtimeLocationBar-এর ঠিক নিচে —
                    // পুরো AdminPanelScreen (সব ট্যাব) একই Scaffold-এর ভেতরে বলে এই একটা জায়গায়
                    // বসালেই প্রতিটা admin ট্যাবে persistent থাকবে, ট্যাব বদলালেও হারাবে না।
                    DegradedAdminSessionBar(
                        visible = isDegradedAdminSession,
                        reason = degradedAdminSessionReason,
                        onDismiss = { viewModel.dismissDegradedAdminSessionWarning() }
                    )
                }
            },
            containerColor = SomadhanBg
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(SomadhanBg)
            ) {
                SomadhanPullToRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = onAdminPullToRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                when (selectedTabIndex) {
                    0 -> {
                        LaunchedEffect(selectedTabIndex) {
                            // Populates FirebaseSyncManager.liveMetrics via server-side count()/
                            // sum() aggregation instead of downloading full collections -- see
                            // ENGINEERING_NOTES.md §2. Runs once per visit to this tab; the
                            // existing sync button (onSyncClick below) also re-triggers it.
                            viewModel.refreshAdminMetrics()
                        }
                        SyncAwareContent(
                            sessionKey = "admin_overview_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                            AdminStatsView(
                                metrics = adminMetrics,
                                commissionPercent = commissionPercent,
                                heldEscrows = allHeldEscrows,
                                additionalCharges = allAdditionalCharges,
                                allTransactions = allTransactions,
                                allProblems = allProblems,
                                allUsers = allUsers,
                                isRefreshing = isRefreshing,
                                onSyncClick = { viewModel.triggerCloudSync() },
                                onOpenCloudConfig = { showCloudConfigDialog = true }
                            )
                        }
                    }
                    1 -> {
                        // Admin Panel Loading fix, সেশন ২.৩ — cold-load skeleton gate (rule ১+২)
                        // শুধু। ভেতরের তালিকা-ভিত্তিক pulse (tab/pagination/data-change/pull-to-
                        // refresh) নিজে AdminKycView-এর ভেতরেই (rememberFieldChangePulse +
                        // PulsingValue দিয়ে, শুধু list-item card গুলোতে) হ্যান্ডেল করা হয় বলে
                        // এখানে SyncAwareRefreshableContent-এর whole-content data-diff flash
                        // ব্যবহার করা হয়নি (সেটা সার্চ বার/ট্যাব হেডার/pagination bar-সহ পুরো
                        // ট্যাবটাকেই flash করাতো, যা ব্যবহারকারীর কাঙ্ক্ষিত না)।
                        SyncAwareContent(
                            sessionKey = "admin_kyc_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminKycView(
                            users = allUsers,
                            onApproveKyc = { userId -> viewModel.adminApproveKyc(userId) },
                            onRejectKyc = { userId, reason -> viewModel.adminRejectKyc(userId, reason) },
                            onRevokeKyc = { userId, reason -> viewModel.adminRevokeKyc(userId, reason) },
                            onUpdateKycInfo = { userId, docNumber, firstName, lastName, address ->
                                viewModel.adminUpdateKycInfo(userId, docNumber, firstName, lastName, address)
                            },
                            onBulkApprove = { userIds -> viewModel.adminBulkApproveKyc(userIds) },
                            onResetKycToPending = { userId -> viewModel.adminResetKycToPending(userId) },
                            viewModel = viewModel,
                            isManualRefreshing = isRefreshing
                        )
                        }
                    }
                    2 -> {
                        // Admin Panel Loading fix, সেশন ২.১৫ (Ground Rule ১৮ retrofit) — আগে
                        // SyncAwareRefreshableContent-এ পুরো content (allWithdrawals+allUsers)
                        // data হিসেবে পাস করা হতো বলে re-entry/pull-to-refresh/data-change-এ
                        // পুরো তালিকা একসাথে flash হতো। ব্যবহারকারীর কনফার্মড ডিজাইন: প্রথম
                        // ভিজিটে (cold-load) পুরো ট্যাব skeleton থাকবে (rule ১, অপরিবর্তিত),
                        // কিন্তু re-entry/ফিল্টার-পরিবর্তন/pagination কোনোটাতেই পুরো তালিকা না —
                        // শুধু কার্ডগুলো pulse করবে (নিচে AdminWithdrawalsView.kt-এর ভেতরে
                        // rememberFieldChangePulse দিয়ে)। plain SyncAwareContent (data-বিহীন) —
                        // pull-to-refresh গ্লোবাল SomadhanPullToRefresh wrapper থেকেই আসে (isRefreshing),
                        // এখানে আলাদা করে বাদ দেওয়া হয়নি।
                        val allWithdrawals by viewModel.allWithdrawals.collectAsStateWithLifecycle()
                        SyncAwareContent(
                            sessionKey = "admin_withdrawals_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminWithdrawalsView(
                            withdrawals = allWithdrawals,
                            allUsers = allUsers,
                            viewModel = viewModel,
                            onUpdateStatus = { w, s, trx -> viewModel.adminUpdateWithdrawalStatus(w, s, trx) },
                            isManualRefreshing = isRefreshing
                        )
                        }
                    }
                    3 -> {
                        // Admin Panel Loading fix, সেশন ২.৪ — শুধু rule ১ (প্রথম-ভিজিট cold-load
                        // পুরো ট্যাব skeleton)। ব্যবহারকারীর কনফার্মড ডিজাইন: re-entry/pagination/
                        // realtime data-change কোনোটাতেই কোনো re-flash/pulse হবে না (KYC-র মতো
                        // আলাদা list-pulse না), আর pull-to-refresh এই dev/admin-ধর্মী ট্যাবে দরকার
                        // নেই। তাই plain SyncAwareContent (data প্যারামিটার-বিহীন, isManualRefreshing
                        // পাস করা হয়নি) — এটাই rule ২/৩/৪ কোনোটাই প্রযোজ্য না এমন B2-ক্যাটেগরি প্যাটার্ন।
                        val allAdminNotifications by viewModel.allAdminNotifications.collectAsStateWithLifecycle()
                        SyncAwareContent(
                            sessionKey = "admin_manual_notification_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminManualNotificationView(
                            allUsers = allUsers,
                            allNotifications = allAdminNotifications,
                            onSendNotification = { role, title, message, type, scheduledFor, onSuccess, onError ->
                                viewModel.adminSendManualNotification(role, title, message, type, scheduledFor, onSuccess, onError)
                            },
                            onCancelScheduledNotification = { title, scheduledFor, timestamp ->
                                viewModel.adminCancelScheduledNotification(title, scheduledFor, timestamp)
                            },
                            onDeleteNotification = { title, timestamp ->
                                viewModel.adminDeleteManualNotification(title, timestamp)
                            }
                        )
                        }
                    }
                    // Admin Panel Loading fix, সেশন ২.১৬ (Ground Rule ১৮ retrofit) — শুধু প্রথম
                    // ভিজিটে (cold-load) পুরো ট্যাব skeleton থাকবে (rule ১, অপরিবর্তিত), কিন্তু
                    // re-entry/সার্চ/ফিল্টার-পরিবর্তন/pagination কোনোটাতেই পুরো তালিকা না — শুধু
                    // কার্ডগুলো pulse করবে (নিচে AdminUsersView.kt-এর ভেতরে rememberFieldChangePulse
                    // দিয়ে)। plain SyncAwareContent (data-বিহীন) — pull-to-refresh গ্লোবাল
                    // SomadhanPullToRefresh wrapper থেকেই আসে (isRefreshing), এখানে আলাদা করে
                    // বাদ দেওয়া হয়নি।
                    4 -> SyncAwareContent(
                        sessionKey = "admin_users_sync",
                        viewModel = viewModel,
                        syncPhase = initialSyncPhase,
                        onRetry = { viewModel.retryInitialSync() },
                        skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                    ) {
                        AdminUsersView(
                            users = allUsers,
                            viewModel = viewModel,
                            onDeleteUser = { viewModel.adminDeleteUser(it) },
                            onNavigateToReputation = { userId, role ->
                                onNavigate(Screen.ReputationDetail.createRoute(userId, role))
                            },
                            isManualRefreshing = isRefreshing
                        )
                    }
                    5 -> {
                        // Admin Panel Loading fix, সেশন ২.১৮ — Ground Rule ১৮ retrofit: আগের
                        // SyncAwareRefreshableContent(data = listOf(allProblems, allUsers)) পুরো
                        // ট্যাব (সার্চ বার/ফিল্টার চিপ/pagination bar-সহ) whole-content flash
                        // করাতো re-entry/pull-to-refresh/ডেটা-বদলে। প্লেইন SyncAwareContent
                        // (rule ১+২, data-বিহীন) দিয়ে বদলানো হয়েছে — ভেতরের list-pulse এখন
                        // AdminProblemsView-এর ভেতরেই (per-item rememberFieldChangePulse +
                        // PulsingValue, Ground Rule ১৯/২০) হ্যান্ডেল করা হয়, শুধু প্রবলেম-কার্ড
                        // pulse করবে, সার্চ/ফিল্টার/pagination bar স্থির থাকবে।
                        SyncAwareContent(
                            sessionKey = "admin_problems_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminProblemsView(
                            problems = allProblems,
                            allUsers = allUsers,
                            viewModel = viewModel,
                            isManualRefreshing = isRefreshing,
                            onDeleteProblem = { viewModel.adminDeleteProblem(it) }
                        )
                        }
                    }
                    6 -> {
                        // Admin Panel Loading fix, সেশন ২.৫ — cold-load skeleton gate (rule ১+২)
                        // শুধু, ঠিক KYC (২.৩)-এর মতো। ভেতরের list-pulse (re-entry/pull-to-refresh/
                        // data-change) নিজে AdminCategoriesView-এর ভেতরেই (rememberFieldChangePulse
                        // + PulsingValue দিয়ে, শুধু ক্যাটাগরি-কার্ডে) হ্যান্ডেল করা হয় — toggle/Add
                        // বাটন এই কম্পোনেন্টের বাইরে, তাই কখনো pulse করবে না।
                        val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
                        SyncAwareContent(
                            sessionKey = "admin_categories_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminCategoriesView(
                            categories = allCategories,
                            radiusKm = physicalCategoryRadiusKm,
                            isPhysicalWorkEnabled = isPhysicalWorkEnabled,
                            isVirtualWorkEnabled = isVirtualWorkEnabled,
                            onSetPhysicalWorkEnabled = { viewModel.adminSetPhysicalWorkEnabled(it) },
                            onSetVirtualWorkEnabled = { viewModel.adminSetVirtualWorkEnabled(it) },
                            onAddClick = { showAddCategoryDialog = true },
                            onEditCategory = { categoryForEditDialog = it },
                            onDeleteCategory = { viewModel.adminDeleteCategory(it) },
                            onToggleActive = { categoryId, isActive ->
                                viewModel.adminToggleCategoryActive(categoryId, isActive)
                            },
                            viewModel = viewModel,
                            isManualRefreshing = isRefreshing
                        )
                        }
                    }
                    7 -> {
                        val allReleasedEscrows by viewModel.allReleasedEscrows.collectAsStateWithLifecycle()
                        val allRefundedEscrows by viewModel.allRefundedEscrows.collectAsStateWithLifecycle()
                        SyncAwareContent(
                            sessionKey = "admin_escrow_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminEscrowView(
                            heldEscrows = allHeldEscrows,
                            releasedEscrows = allReleasedEscrows,
                            refundedEscrows = allRefundedEscrows,
                            allUsers = allUsers,
                            isManualRefreshing = isRefreshing,
                            viewModel = viewModel
                        )
                        }
                    }
                    8 -> {
                        // Admin Panel Loading fix, সেশন ২.৬ — cold-load skeleton gate (rule ১+২)
                        // শুধু, ঠিক KYC (২.৩)/ক্যাটাগরি (২.৫)-এর মতো। ভেতরের list-pulse (re-entry/
                        // pull-to-refresh/data-change) নিজে AdminAdditionalChargesView-এর ভেতরেই
                        // (rememberFieldChangePulse + PulsingValue দিয়ে, শুধু চার্জ-কার্ডে) হ্যান্ডেল
                        // করা হয় — হেডার/সামারি কার্ড, সার্চ/ফিল্টার কার্ড, pagination bar এই
                        // কম্পোনেন্টের বাইরে, তাই কখনো pulse করবে না।
                        SyncAwareContent(
                            sessionKey = "admin_additional_charges_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminAdditionalChargesView(
                            charges = allAdditionalCharges,
                            viewModel = viewModel,
                            isManualRefreshing = isRefreshing
                        )
                        }
                    }
                    9 -> {
                        // Admin Panel Loading fix, সেশন ২.২০ — Ground Rule ১৮ retrofit (whole-list
                        // flash থেকে per-item pulse-এ)। আগে পুরো content (transactions +
                        // gatewayPayments + allUsers) `SyncAwareRefreshableContent`-এ `data` হিসেবে
                        // পাস হতো বলে re-entry/pull-to-refresh-এ পুরো তালিকা + সামারি/স্ট্যাট কার্ড
                        // একসাথে flash করতো। এখন প্লেইন `SyncAwareContent` (rule ১+২ cold-load শুধু)
                        // — pull-to-refresh/re-entry/নতুন-Insert pulse এখন AdminTransactionsView-এর
                        // ভেতরেই (rememberFieldChangePulse + PulsingValue, শুধু তালিকার
                        // WorkTransaction/WalletRecharge কার্ডে — ব্যবহারকারীর কনফার্মড উত্তর,
                        // Ground Rule ১৬) হ্যান্ডেল হয় — সামারি/স্ট্যাট কার্ড, ফিল্টার/সার্চ বার,
                        // pagination bar এর বাইরে, কখনো pulse করবে না।
                        val allGatewayPayments by viewModel.allGatewayPayments.collectAsStateWithLifecycle()
                        SyncAwareContent(
                            sessionKey = "admin_transactions_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminTransactionsView(
                            transactions = allTransactions,
                            gatewayPayments = allGatewayPayments,
                            allUsers = allUsers,
                            viewModel = viewModel,
                            isManualRefreshing = isRefreshing
                        )
                        }
                    }
                    10 -> {
                        // Admin Panel Loading fix, সেশন ২.৭ — cold-load skeleton gate (rule ১+২)
                        // শুধু, ঠিক KYC (২.৩)/ক্যাটাগরি (২.৫)/অতিরিক্ত চার্জ (২.৬)-এর মতো। ভেতরের
                        // list-pulse (re-entry/pull-to-refresh/ফিল্টার-পেজ-change/delete) নিজে
                        // AdminRatingsView-এর ভেতরেই (rememberFieldChangePulse + PulsingValue
                        // দিয়ে, শুধু রিভিউ-কার্ডে) হ্যান্ডেল করা হয় — হেডার/সার্চ-ফিল্টার-সর্ট
                        // কন্ট্রোল, pagination bar এই কম্পোনেন্টের বাইরে, তাই কখনো pulse করবে না।
                        val allRatings by viewModel.allRatings.collectAsStateWithLifecycle()
                        SyncAwareContent(
                            sessionKey = "admin_ratings_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminRatingsView(
                            ratings = allRatings,
                            onDelete = { viewModel.adminDeleteRating(it) },
                            viewModel = viewModel,
                            isManualRefreshing = isRefreshing
                        )
                        }
                    }
                    11 -> {
                        // Admin Panel Loading fix, সেশন ২.৮ — ব্যবহারকারীর সিদ্ধান্ত: ক্যাটেগরি E
                        // (SOMADHAN_LOADING_PATTERN_MASTER_PROMPT.md-এর এডিট-ফর্ম ব্যতিক্রম)।
                        // শুধু rule ১ (cold-load skeleton gate) — কোনো pulse/diff-শিমার (rule ২)
                        // ইচ্ছাকৃতভাবে বসানো হয়নি, কারণ AdminSettingsView-এর প্রতিটা সেকশন
                        // active edit form (পাসওয়ার্ড/রেডিয়াস/টগল ইত্যাদি) — background data
                        // বদলে diff-শিমার flash করলে মাঝপথের ইনপুট বিঘ্নিত হওয়ার ঝুঁকি আছে।
                        SyncAwareContent(
                            sessionKey = "admin_settings_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminSettingsView(
                            viewModel = viewModel,
                            onLogout = onLogout
                        )
                        }
                    }
                    12 -> {
                        // Admin Panel Loading fix, সেশন ২.৯ — cold-load skeleton gate (rule ১+২)
                        // শুধু, ঠিক KYC (২.৩)/ক্যাটাগরি (২.৫)/অতিরিক্ত চার্জ (২.৬)/রিভিউ (২.৭)-এর
                        // মতো। ভেতরের list-pulse (re-entry/pull-to-refresh/ফিল্টার-সার্চ-scroll-
                        // load) নিজে AdminAuditLogView-এর ভেতরেই (rememberFieldChangePulse +
                        // PulsingValue দিয়ে, শুধু লগ-কার্ডে) হ্যান্ডেল করা হয় — হেডার/সার্চ-ফিল্টার
                        // চিপ এই কম্পোনেন্টের বাইরে, তাই কখনো pulse করবে না।
                        val recentAuditLogs by viewModel.recentAuditLogs.collectAsStateWithLifecycle()
                        SyncAwareContent(
                            sessionKey = "admin_audit_log_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminAuditLogView(
                            auditLogs = recentAuditLogs,
                            viewModel = viewModel,
                            isManualRefreshing = isRefreshing
                        )
                        }
                    }
                    13 -> {
                        // Admin Panel Loading fix, সেশন ২.১০ — cold-load skeleton gate (rule ১+২)
                        // শুধু, ঠিক KYC (২.৩)/ক্যাটাগরি (২.৫)-এর মতো। ভেতরের list-pulse (re-entry/
                        // pull-to-refresh/ট্যাব-সার্চ-change/data-change) নিজে
                        // AdminFaqManagementView-এর ভেতরেই (rememberFieldChangePulse +
                        // PulsingValue দিয়ে, শুধু FAQ-কার্ডে) হ্যান্ডেল করা হয় — ট্যাব হেডার, সার্চ
                        // বার, Add বাটন এই কম্পোনেন্টের বাইরে, তাই কখনো pulse করবে না।
                        val allFaqs by viewModel.allFaqsForAdmin.collectAsStateWithLifecycle()
                        SyncAwareContent(
                            sessionKey = "admin_faq_management_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminFaqManagementView(
                            faqs = allFaqs,
                            onAddClick = { audience ->
                                addFaqTargetAudience = audience
                                showAddFaqDialog = true
                            },
                            onEditFaq = { faqForEditDialog = it },
                            onDeleteFaq = { viewModel.adminDeleteFaq(it) },
                            viewModel = viewModel,
                            isManualRefreshing = isRefreshing
                        )
                        }
                    }
                    14 -> {
                        // Admin Panel Loading fix, সেশন ২.১১ — cold-load skeleton gate (rule ১+২)
                        // শুধু, ঠিক KYC (২.৩)/ক্যাটাগরি (২.৫)/অতিরিক্ত চার্জ (২.৬)-এর মতো। ভেতরের
                        // list-pulse (re-entry/pull-to-refresh/সার্চ-ফিল্টার-পেজ-change) নিজে
                        // AdminCancelledBidsView-এর ভেতরেই (rememberFieldChangePulse + PulsingValue
                        // দিয়ে, শুধু বিড-কার্ডে) হ্যান্ডেল করা হয় — সার্চ/ফিল্টার/সামারি কার্ড,
                        // pagination bar এই কম্পোনেন্টের বাইরে, তাই কখনো pulse করবে না।
                        val allCancelledBids by viewModel.allCancelledBids.collectAsStateWithLifecycle()
                        SyncAwareContent(
                            sessionKey = "admin_cancelled_bids_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminCancelledBidsView(
                            cancelledBids = allCancelledBids,
                            allProblems = allProblems,
                            allUsers = allUsers,
                            viewModel = viewModel,
                            isManualRefreshing = isRefreshing
                        )
                        }
                    }
                    15 -> {
                        // Admin Panel Loading fix, সেশন ২.২১ — ব্যবহারকারীর সিদ্ধান্ত: ReputationEngine
                        // (২.১৩)-এর মতো ক্যাটেগরি E। GR19 pulse স্কোপ = কোনোটাই না (ব্যবহারকারীর
                        // কনফার্মড উত্তর), GR21 = এই ট্যাবে দরকার নেই। তাই শুধু GR18 (whole-tab
                        // re-entry/pull-to-refresh flash বন্ধ) — SyncAwareRefreshableContent থেকে
                        // প্লেইন SyncAwareContent-এ বদলানো হলো। যেহেতু কোনো pulse নেই, তাই
                        // AdminChatMonitoringView-এ isManualRefreshing ওয়্যারিং দরকার নেই (global
                        // SomadhanPullToRefresh wrapper আগে থেকেই pull-gesture সব ট্যাবে দেয়)।
                        SyncAwareContent(
                            sessionKey = "admin_chat_monitoring_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminChatMonitoringView(
                            viewModel = viewModel,
                            allProblems = allProblems,
                            allUsers = allUsers
                        )
                        }
                    }
                    16 -> {
                        // Admin Panel Loading fix, সেশন ২.২২ — ব্যবহারকারীর সিদ্ধান্ত: GR19 pulse
                        // স্কোপ = শুধু ডাইরেক্ট কন্ট্রাক্ট তালিকা, GR18 pull-to-refresh = দরকার,
                        // GR21 = শুধু genuine Insert। SyncAwareRefreshableContent (whole-data flash)
                        // থেকে প্লেইন SyncAwareContent-এ বদলানো হলো — pull-to-refresh সম্পন্ন হলে আর
                        // পুরো ট্যাব ফ্ল্যাশ করবে না, শুধু AdminDirectContractsView.kt-এর ভেতরের
                        // per-item pulse (নিজে viewModel.isRefreshing কালেক্ট করে) কাজ করবে।
                        SyncAwareContent(
                            sessionKey = "admin_direct_contracts_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminDirectContractsView(
                            problems = allProblems,
                            allUsers = allUsers,
                            viewModel = viewModel
                        )
                        }
                    }
                    17 -> AdminSupabaseExplorerView(
                        viewModel = viewModel,
                        onLogout = onLogout
                    )
                    18 -> {
                        val allWithdrawals by viewModel.allWithdrawals.collectAsStateWithLifecycle()
                        val allRefundedEscrows by viewModel.allRefundedEscrows.collectAsStateWithLifecycle()
                        val allCancelledBids by viewModel.allCancelledBids.collectAsStateWithLifecycle()
                        val allRatings by viewModel.allRatings.collectAsStateWithLifecycle()
                        SyncAwareContent(
                            sessionKey = "admin_user_lookup_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminUserLookupView(
                            allUsers = allUsers,
                            allProblems = allProblems,
                            allWithdrawals = allWithdrawals,
                            allHeldEscrows = allHeldEscrows,
                            allRefundedEscrows = allRefundedEscrows,
                            allCancelledBids = allCancelledBids,
                            allAdditionalCharges = allAdditionalCharges,
                            allTransactions = allTransactions,
                            allRatings = allRatings,
                            viewModel = viewModel,
                            onNavigate = onNavigate
                        )
                        }
                    }
                    19 -> {
                        SyncAwareRefreshableContent(
                            sessionKey = "admin_solver_quota_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            data = listOf(allUsers, allProblems, allTransactions),
                            isManualRefreshing = isRefreshing,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminSolverQuotaView(
                            allUsers = allUsers,
                            allProblems = allProblems,
                            allTransactions = allTransactions,
                            viewModel = viewModel,
                            onNavigate = onNavigate
                        )
                        }
                    }
                    20 -> {
                        // Admin Panel Loading fix, সেশন ২.১৩ — ব্যবহারকারীর সিদ্ধান্ত: সেটিংস
                        // (২.৮)-এর মতো ক্যাটেগরি E (এডিট-ফর্ম ব্যতিক্রম)। শুধু rule ১ (cold-load
                        // skeleton gate) — কোনো pulse/diff-শিমার (rule ২) ইচ্ছাকৃতভাবে বসানো হয়নি,
                        // কারণ AdminReputationEngineView-এর মূল অংশ স্কোর/ক্যাপ active edit-form
                        // ইনপুট ফিল্ড (ভেতরের সাজেস্টেড/কাস্টম ইভেন্ট তালিকা দুটোও ফর্ম-ইনপুট-চালিত)
                        // — background data বদলে diff-শিমার flash করলে মাঝপথের ইনপুট বিঘ্নিত হওয়ার
                        // ঝুঁকি আছে। pull-to-refresh (গ্লোবাল `SomadhanPullToRefresh` wrapper, যা
                        // আগে থেকেই পুরো `when` ব্লক ঢেকে রাখে) এই ট্যাবে বাদ দেওয়া হয়নি (ব্যবহারকারীর
                        // কনফার্মড সিদ্ধান্ত) — কিন্তু যেহেতু কোনো pulse-ই নেই, তাই কোনো
                        // `isManualRefreshing` ওয়্যারিং এই স্ক্রিনে দরকার হয়নি (Settings/২.৮-এর মতোই)।
                        SyncAwareContent(
                            sessionKey = "admin_reputation_engine_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminReputationEngineView(
                            viewModel = viewModel
                        )
                        }
                    }
                    21 -> {
                        val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
                        val allReleasedEscrows by viewModel.allReleasedEscrows.collectAsStateWithLifecycle()
                        val allRefundedEscrows by viewModel.allRefundedEscrows.collectAsStateWithLifecycle()
                        // Ground Rule ১৮, সেশন ২.২৪ — পুরো ট্যাব whole-content flash আর করবে না;
                        // per-item pulse (Admin[X]View.kt-এর ভেতরে) নিজে সামলায়।
                        SyncAwareContent(
                            sessionKey = "admin_instant_jobs_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminInstantJobsView(
                            allProblems = allProblems,
                            allUsers = allUsers,
                            allCategories = allCategories,
                            allHeldEscrows = allHeldEscrows,
                            allReleasedEscrows = allReleasedEscrows,
                            allRefundedEscrows = allRefundedEscrows,
                            viewModel = viewModel,
                            onNavigate = onNavigate
                        )
                        }
                    }
                    22 -> {
                        // Ground Rule ১৮, সেশন ২.২৫ — পুরো ট্যাব whole-content flash আর করবে না;
                        // per-item pulse (AdminDisputeCenterView.kt-এর ভেতরে) নিজে সামলায়।
                        SyncAwareContent(
                            sessionKey = "admin_dispute_center_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminDisputeCenterView(
                            viewModel = viewModel,
                            allProblems = allProblems,
                            allUsers = allUsers
                        )
                        }
                    }
                    23 -> {
                        // Ground Rule ১৮, সেশন ২.২৬ — পুরো ট্যাব whole-content flash আর করবে না;
                        // per-item + সামারি-ভ্যালু pulse (AdminGatewayPaymentsView.kt-এর ভেতরে)
                        // নিজে সামলায়।
                        val allGatewayPayments by viewModel.allGatewayPayments.collectAsStateWithLifecycle()
                        SyncAwareContent(
                            sessionKey = "admin_gateway_payments_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            onRetry = { viewModel.retryInitialSync() },
                            skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                        ) {
                        AdminGatewayPaymentsView(
                            gatewayPayments = allGatewayPayments,
                            allUsers = allUsers,
                            viewModel = viewModel,
                            isManualRefreshing = isRefreshing
                        )
                        }
                    }
                    24 -> AdminRefundDebugView()
                    AdminPermissionCatalog.ROLE_MGMT_TAB_INDEX -> {
                        // [ADMIN_ROLE_PROFILE সেশন ৩ — অংশ ২.২] রোল ম্যানেজমেন্ট (সুপার-অনলি)।
                        // ড্রয়ারে নন-সুপারের কাছে আইটেমটাই আসে না; এখানকার চেকটা দ্বিতীয় স্তর — যেমন সুপার
                        // এই ট্যাবে থাকা অবস্থায় heartbeat-এ রোল হারালে (rememberSaveable ইনডেক্স রয়ে যায়)।
                        // আসল গেট সার্ভারে (`_admin_require_super`), এটা শুধু UI।
                        if (!isSuperAdmin) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "এই মেনুটি শুধু সুপার অ্যাডমিনের জন্য।",
                                    color = SomadhanTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            val adminRoles by viewModel.adminRoles.collectAsStateWithLifecycle()
                            val adminRolesPhase by viewModel.adminRolesSyncPhase.collectAsStateWithLifecycle()
                            // ট্যাবে ঢুকলেই একবার রিফ্রেশ (LOADED অবস্থায় চুপচাপ — skeleton/flash ছাড়া; নতুন ডেটা
                            // এলে শুধু কার্ডগুলো per-item pulse করে)। ERROR অবস্থায় এটা আবার চেষ্টা চালু করে।
                            LaunchedEffect(Unit) { viewModel.loadAdminRoles() }
                            // ERROR-কে ইচ্ছাকৃতভাবে LOADED হিসেবে গেটে পাঠানো — SyncAwareContent ERROR-এ
                            // "cached কনটেন্ট" দেখায় (bulk-pull-এর ইতিহাস ঠিক থাকলে), যা এই ডেটার ক্ষেত্রে
                            // ভুল ("কোনো রোল নেই" দেখাতো)। এরর আর retry AdminRoleManagementView নিজে দেখায়।
                            SyncAwareContent(
                                sessionKey = "admin_role_mgmt_sync",
                                viewModel = viewModel,
                                syncPhase = if (adminRolesPhase == SupabaseRealtimeManager.SyncPhase.ERROR) {
                                    SupabaseRealtimeManager.SyncPhase.LOADED
                                } else {
                                    adminRolesPhase
                                },
                                onRetry = { viewModel.loadAdminRoles() },
                                skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                            ) {
                                AdminRoleManagementView(
                                    roles = adminRoles,
                                    onSaveRole = { id, name, perms, onDone ->
                                        viewModel.adminSaveRole(id, name, perms, onDone)
                                    },
                                    onDeleteRole = { viewModel.adminDeleteRole(it) },
                                    viewModel = viewModel,
                                    isManualRefreshing = isRefreshing,
                                    loadFailed = adminRolesPhase == SupabaseRealtimeManager.SyncPhase.ERROR,
                                    onRetry = { viewModel.loadAdminRoles() }
                                )
                            }
                        }
                    }
                    AdminPermissionCatalog.ADMIN_ACCOUNTS_TAB_INDEX -> {
                        // [ADMIN_ROLE_PROFILE সেশন ৪] এডমিন অ্যাকাউন্ট (সুপার-অনলি) — রোল-ট্যাবের মতোই দুই-স্তরের গেট
                        // (ড্রয়ারে আইটেমই আসে না + এখানের চেক); আসল গেট সার্ভারে।
                        if (!isSuperAdmin) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "এই মেনুটি শুধু সুপার অ্যাডমিনের জন্য।",
                                    color = SomadhanTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            val adminAccounts by viewModel.adminAccounts.collectAsStateWithLifecycle()
                            val adminAccountsPhase by viewModel.adminAccountsSyncPhase.collectAsStateWithLifecycle()
                            // রোল-ড্রপডাউন/account_count-এর জন্য রোল-তালিকাও লাগে (ট্যাব খুলতে আলাদা রোল-ট্যাবে যেতে হবে না)।
                            val adminRoles by viewModel.adminRoles.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) { viewModel.loadAdminRoles() }

                            // অনলাইন-ডট লাইভ রাখতে ~২০সে পরপর নিঃশব্দ পোলিং (silent = true: ERROR অবস্থায় skeleton ঝলকায় না)।
                            // শুধু স্ক্রিন RESUMED থাকলে চলে — ব্যাকগ্রাউন্ডে গেলে থামে, ফিরলে সাথে সাথে একবার রিফ্রেশ করে আবার শুরু।
                            val pollLifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
                            var isScreenResumed by remember { mutableStateOf(true) }
                            DisposableEffect(pollLifecycleOwner) {
                                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                                    when (event) {
                                        androidx.lifecycle.Lifecycle.Event.ON_RESUME -> isScreenResumed = true
                                        androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> isScreenResumed = false
                                        else -> Unit
                                    }
                                }
                                pollLifecycleOwner.lifecycle.addObserver(observer)
                                onDispose { pollLifecycleOwner.lifecycle.removeObserver(observer) }
                            }
                            LaunchedEffect(isScreenResumed) {
                                if (isScreenResumed) {
                                    viewModel.loadAdminAccounts()
                                    while (true) {
                                        kotlinx.coroutines.delay(20_000L)
                                        viewModel.loadAdminAccounts(silent = true)
                                    }
                                }
                            }

                            // ERROR → LOADED গেটে পাঠানো (রোল-ট্যাবের মতোই কারণ: cached-কনটেন্ট শাখা এই ডেটায় ভুল);
                            // এরর আর retry AdminAccountsView নিজে দেখায়।
                            SyncAwareContent(
                                sessionKey = "admin_accounts_sync",
                                viewModel = viewModel,
                                syncPhase = if (adminAccountsPhase == SupabaseRealtimeManager.SyncPhase.ERROR) {
                                    SupabaseRealtimeManager.SyncPhase.LOADED
                                } else {
                                    adminAccountsPhase
                                },
                                onRetry = { viewModel.loadAdminAccounts() },
                                skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                            ) {
                                AdminAccountsView(
                                    accounts = adminAccounts,
                                    roles = adminRoles,
                                    currentAdminId = adminSessionState?.account?.id,
                                    onCreate = { name, phone, password, roleId, designation, email, onDone ->
                                        viewModel.adminCreateAccount(name, phone, password, roleId, designation, email, onDone)
                                    },
                                    onChangeRole = { account, roleId, onDone ->
                                        viewModel.adminChangeAccountRole(account, roleId, onDone)
                                    },
                                    onSetActive = { account, active, reason, onDone ->
                                        viewModel.adminSetAccountActive(account, active, reason, onDone)
                                    },
                                    onSetFlagged = { account, flagged, reason, onDone ->
                                        viewModel.adminSetAccountFlagged(account, flagged, reason, onDone)
                                    },
                                    onResetPassword = { account, newPassword, onDone ->
                                        viewModel.adminResetAccountPassword(account, newPassword, onDone)
                                    },
                                    viewModel = viewModel,
                                    isManualRefreshing = isRefreshing,
                                    loadFailed = adminAccountsPhase == SupabaseRealtimeManager.SyncPhase.ERROR,
                                    onRetry = { viewModel.loadAdminAccounts() }
                                )
                            }
                        }
                    }
                    AdminPermissionCatalog.ACTIVITY_LOG_TAB_INDEX -> {
                        // [ADMIN_ROLE_PROFILE সেশন ৫] অ্যাক্টিভিটি লগ (সুপার-অনলি, ক্লাউড থেকে সব এডমিনের লগ) —
                        // বিদ্যমান লিগ্যাসি লগ-ট্যাব (১২, ডিভাইস-লোকাল) থেকে আলাদা। রোল/অ্যাকাউন্ট-ট্যাবের মতোই
                        // দুই-স্তরের গেট; আসল গেট সার্ভারে (`_admin_require_super`)।
                        if (!isSuperAdmin) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "এই মেনুটি শুধু সুপার অ্যাডমিনের জন্য।",
                                    color = SomadhanTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            val activityLogs by viewModel.adminActivityLogs.collectAsStateWithLifecycle()
                            val activityLogsPhase by viewModel.adminActivityLogsSyncPhase.collectAsStateWithLifecycle()
                            val activityLogsHasMore by viewModel.adminActivityLogsHasMore.collectAsStateWithLifecycle()
                            val activityLogsLoadingMore by viewModel.adminActivityLogsLoadingMore.collectAsStateWithLifecycle()
                            val activityLogsFiltering by viewModel.adminActivityLogsFiltering.collectAsStateWithLifecycle()
                            val activityLogFilter by viewModel.adminActivityLogFilter.collectAsStateWithLifecycle()
                            // ফিল্টার-ড্রপডাউনের এডমিন-তালিকা (এডমিন অ্যাকাউন্ট ট্যাবের একই ডেটা)
                            val filterAdmins by viewModel.adminAccounts.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) {
                                viewModel.loadAdminActivityLogs()
                                viewModel.loadAdminAccounts(silent = true)
                            }
                            // ERROR → LOADED গেটে পাঠানো (রোল/অ্যাকাউন্ট-ট্যাবের মতোই কারণ); এরর + retry ভিউ নিজে দেখায়।
                            SyncAwareContent(
                                sessionKey = "admin_activity_log_sync",
                                viewModel = viewModel,
                                syncPhase = if (activityLogsPhase == SupabaseRealtimeManager.SyncPhase.ERROR) {
                                    SupabaseRealtimeManager.SyncPhase.LOADED
                                } else {
                                    activityLogsPhase
                                },
                                onRetry = { viewModel.loadAdminActivityLogs() },
                                skeleton = { ListScreenSkeleton(tint = SomadhanAdminSlate) }
                            ) {
                                AdminActivityLogView(
                                    logs = activityLogs,
                                    hasMore = activityLogsHasMore,
                                    isLoadingMore = activityLogsLoadingMore,
                                    isFiltering = activityLogsFiltering,
                                    appliedFilter = activityLogFilter,
                                    admins = filterAdmins,
                                    onApplyFilter = { viewModel.applyAdminActivityLogFilter(it) },
                                    onLoadMore = { viewModel.loadMoreAdminActivityLogs() },
                                    viewModel = viewModel,
                                    isManualRefreshing = isRefreshing,
                                    loadFailed = activityLogsPhase == SupabaseRealtimeManager.SyncPhase.ERROR,
                                    onRetry = { viewModel.loadAdminActivityLogs() }
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
