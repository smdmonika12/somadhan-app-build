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
import androidx.compose.material.icons.filled.AdminPanelSettings
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
import com.example.data.entity.activeRoleBalance
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

// [MONEY_FLOW_AND_ADMIN_BUGS ধাপ ৮ — বাগ D/D২/D৩] dual-role ইউজারের জন্য admin panel-এ ২টা
// সম্পূর্ণ স্বাধীন "কার্ড" (identity anchor বাদে) বানানোর wrapper। একই `UserEntity` (একটাই root
// row) থেকে role অনুযায়ী ১টা বা ২টা কার্ড তৈরি হয় — প্রতিটা কার্ডের নিজস্ব balance/reputation/
// ban/restrict/verified-badge, একটার অ্যাকশন অন্যটাকে স্পর্শ করে না।
private data class AdminUserRoleCard(
    val user: com.example.data.entity.UserEntity,
    val cardRole: String // "USER" | "SOLVER" | "ADMIN"
) {
    val cardBalance: Double
        get() = if (cardRole == "SOLVER") user.balanceSolver else user.balanceUser
    val cardReputationScore: Double
        get() = if (cardRole == "SOLVER") user.reputationScoreSolver else user.reputationScoreUser
    val cardBanned: Boolean
        get() = if (cardRole == "SOLVER") user.isBannedSolver else user.isBannedUser
    val cardRestricted: Boolean
        get() = if (cardRole == "SOLVER") user.isRestrictedSolver else user.isRestrictedUser
    val cardVerifiedBadge: Boolean
        get() = if (cardRole == "SOLVER") user.verifiedBadgeSolver else user.verifiedBadgeUser
}

// একজন ইউজারকে তার `hasUserRole`/`hasSolverRole` অনুযায়ী ১টা বা ২টা কার্ডে ভাঙে। ADMIN
// অ্যাকাউন্ট (dual-role মডেলের বাইরে) সবসময় ১টাই "ADMIN" কার্ড পায়। কোনো কারণে দুটো flag-ই
// false হলে (তাত্ত্বিক edge case) অন্তত ১টা কার্ড দেখানো নিশ্চিত করতে বর্তমান `role` ব্যবহার করা
// হয় — কোনো ইউজারই কার্ডবিহীন হয়ে "হারিয়ে" যাবে না।
private fun com.example.data.entity.UserEntity.toAdminRoleCards(): List<AdminUserRoleCard> {
    if (role == "ADMIN") return listOf(AdminUserRoleCard(this, "ADMIN"))
    val roles = buildList {
        if (hasUserRole) add("USER")
        if (hasSolverRole) add("SOLVER")
    }
    return if (roles.isEmpty()) listOf(AdminUserRoleCard(this, role)) else roles.map { AdminUserRoleCard(this, it) }
}

@Composable
fun AdminUsersView(
    users: List<com.example.data.entity.UserEntity>,
    viewModel: SomadhanViewModel,
    onDeleteUser: (String) -> Unit,
    onNavigateToReputation: (String, String) -> Unit = { _, _ -> },
    isManualRefreshing: Boolean = false
) {
    // [ADMIN_ROLE_PROFILE সেশন ৭.১] view-গেট — AdminSolverQuotaView.kt-এর ঠিক একই প্যাটার্নে, সবার আগে
    // early-return। ৮টা অ্যাকশন (ban/restrict/verified_badge/balance_adjust/change_role/score_adjust/
    // reset_password/delete) নিচের More-Actions ড্রপডাউনে প্রতিটা DropdownMenuItem-এ আলাদা
    // AdminSession.canAct(...) দিয়ে গেট হয়েছে।
    if (!AdminSession.canView("users", "users")) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            AdminAccessLockedState()
        }
        return
    }
    var searchQuery by remember { mutableStateOf("") }
    var sortByLowestReputation by remember { mutableStateOf(false) }
    var selectedRoleFilter by remember { mutableStateOf("ALL") }
    var selectedUserForReputation by remember { mutableStateOf<AdminUserRoleCard?>(null) }

    // Dialog & Menu states
    // [ধাপ ৮] expandedMenuUserId এখন "${userId}_${cardRole}" কী ধরে (শুধু userId না) — নাহলে
    // dual-role ইউজারের দুইটা কার্ডই একই userId শেয়ার করে বলে একটা কার্ডের মেনু খুললে অন্য
    // কার্ডেরটাও (ভুলভাবে) খোলা দেখাত।
    var expandedMenuUserId by remember { mutableStateOf<String?>(null) }
    var userForBanToggle by remember { mutableStateOf<AdminUserRoleCard?>(null) }
    var userForRestrictToggle by remember { mutableStateOf<AdminUserRoleCard?>(null) }
    // [ধাপ ৭ ফিক্স, ROLE_SEPARATION_AUDIT.md] আগে এখানে plain `UserEntity?` ছিল — dual-role
    // ইউজারের ক্ষেত্রে কোন কার্ড (User/Solver) থেকে ডায়ালগ খোলা হলো তার তথ্য হারিয়ে যেত, তাই
    // ডায়ালগ সবসময় generic `user.balance` দেখাত আর adjust করার সময়ও অ্যাকাউন্টের বর্তমান
    // সক্রিয় role অনুমান করা হতো (admin কোন কার্ডে ক্লিক করেছে সেটা না)। এখন ban/restrict
    // toggle-এর মতোই `AdminUserRoleCard?` — cardRole (ক্লিকের মুহূর্তেই জানা) সংরক্ষিত থাকে।
    var userForBalanceAdjust by remember { mutableStateOf<AdminUserRoleCard?>(null) }
    var userForRoleChange by remember { mutableStateOf<com.example.data.entity.UserEntity?>(null) }
    // [ADMIN_ROLE_PROFILE সেশন ৭.১.২, ২০২৬-০৯-২৫ — আংশিক, দেখুন ADMIN_ROLE_PROFILE_PROGRESS.md-এর
    // "৭.১.২ হ্যান্ডঅফ" এন্ট্রি] "বানাও অ্যাডমিন" bottom sheet-এর জন্য টার্গেট-স্টেট — শুধু মেনু-আইটেম
    // থেকে সেট হয় (নিচে দেখুন), কিন্তু আসল BottomSlideAlertDialog/ফর্ম এখনো ওয়্যার করা হয়নি (পরের সাব-ধাপ)।
    var userForCreateAdmin by remember { mutableStateOf<com.example.data.entity.UserEntity?>(null) }
    // [BALANCE_REPUTATION_ROLE_SEPARATION ধাপ ৭খ, ROLE_SEPARATION_AUDIT.md] balance-adjust
    // dialog-এর userForBalanceAdjust ফিক্সের ঠিক একই বাগ ও একই সমাধান — আগে এখানে plain
    // `UserEntity?` ছিল, dual-role ইউজারের ক্ষেত্রে admin কোন কার্ড (User/Solver) থেকে ডায়ালগ
    // খুলেছে সেটা হারিয়ে যেত, তাই ডায়ালগ সবসময় generic shared `reputationScore` দেখাত আর
    // adjust করার সময়ও applyReputationChange() অ্যাকাউন্টের বর্তমান সক্রিয় role অনুমান করত
    // (admin কোন কার্ডে ক্লিক করেছে সেটা না)। এখন `AdminUserRoleCard?` — cardRole ক্লিকের
    // মুহূর্তেই সংরক্ষিত থাকে।
    var userForReputationAdjust by remember { mutableStateOf<AdminUserRoleCard?>(null) }
    var userForPasswordReset by remember { mutableStateOf<com.example.data.entity.UserEntity?>(null) }
    var userForDeleteConfirm by remember { mutableStateOf<com.example.data.entity.UserEntity?>(null) }

    // Balance form inputs
    var balanceAmountInput by remember { mutableStateOf("") }
    var isBalanceAddition by remember { mutableStateOf(true) }
    var balanceReasonInput by remember { mutableStateOf("") }

    // Role form inputs
    var selectedNewRole by remember { mutableStateOf("USER") }

    // Reputation form inputs
    var repScoreChangeInput by remember { mutableStateOf("") }
    var isRepAddition by remember { mutableStateOf(true) }
    var repNoteInput by remember { mutableStateOf("") }

    var newPasswordInput by remember { mutableStateOf("") }

    val filteredAndSortedUsers = remember(users, sortByLowestReputation, selectedRoleFilter, searchQuery) {
        val query = searchQuery.trim().lowercase()
        val filtered = users.filter { user ->
            // [বাগফিক্স, ব্যবহারকারীর রিপোর্ট: "প্রথমবার role switch করলে solver হিসেবে কোনো
            // account admin panel-এ তৈরি হচ্ছে না, শুধু user account-এই দেখাচ্ছে"]
            // আগে এখানে বর্তমানে-সক্রিয় `user.role` দিয়ে ফিল্টার করা হতো। পুরনো dual-row
            // আর্কিটেকচারে এটা ঠিকই কাজ করত, কারণ Solver-এ সুইচ করলে একটা আলাদা, স্থায়ী
            // SOLVER_xxxx row তৈরি হতো যেটা ব্যবহারকারী পরে User-এ ফিরে গেলেও admin panel-এর
            // Solver তালিকায় থেকেই যেত। single-row মডেলে (switchRoleInPlace(), ROLE_UID ফিক্স)
            // আলাদা কোনো row তৈরি হয় না — একই row-এর `role` ফিল্ড শুধু টগল হয়। ফলে ব্যবহারকারী
            // এখন User-এ থাকা অবস্থায় admin panel থেকে তাকে "SOLVER" ফিল্টারে খোঁজা হলে
            // `user.role == "SOLVER"` false হওয়ায় দেখা যেত না — অথচ সে একবার solver setup
            // সম্পন্ন করেছে। ফিক্স: বর্তমান সক্রিয় role না দেখে, root row-এর স্থায়ী
            // `hasUserRole`/`hasSolverRole` ফ্ল্যাগ দিয়ে ফিল্টার করা হচ্ছে — এই ফ্ল্যাগ দুটো
            // role switch করলেও রিসেট হয় না (দেখুন switchRoleInPlace()), তাই যে অ্যাকাউন্ট
            // একবার solver হয়েছে, সে সবসময় Solver ফিল্টারে দেখাবে — ঠিক পুরনো আচরণের মতোই।
            val matchesRole = when (selectedRoleFilter) {
                "USER" -> user.hasUserRole
                "SOLVER" -> user.hasSolverRole
                else -> true
            }
            val matchesQuery = query.isEmpty() ||
                user.name.lowercase().contains(query) ||
                user.phone.lowercase().contains(query) ||
                user.email.lowercase().contains(query) ||
                user.displayUid.lowercase().contains(query) ||
                user.id.lowercase().contains(query)

            matchesRole && matchesQuery
        }
        if (sortByLowestReputation) {
            // [ধাপ ৭খ ফিক্স, ROLE_SEPARATION_AUDIT.md] এই sort একটা UserEntity লিস্টের ওপর হয়
            // (পেজিনেশনের আগে, role-card-এ ভাঙার আগে) — selectedRoleFilter দিয়ে একটা নির্দিষ্ট
            // role বাছাই করা থাকলে সেই role-scoped কলাম দিয়েই সাজানো হচ্ছে, যাতে dual-role
            // ইউজারের অন্য role-এর স্কোর দিয়ে ভুল ক্রম না তৈরি হয়। "ALL" ফিল্টারে (একই ইউজারের
            // দুইটা role-ই সম্ভাব্য প্রাসঙ্গিক, কোনটা "আসল" তা অস্পষ্ট) পুরনো shared
            // `reputationScore` fallback হিসেবে অপরিবর্তিত রাখা হলো।
            when (selectedRoleFilter) {
                "USER" -> filtered.sortedBy { it.reputationScoreUser }
                "SOLVER" -> filtered.sortedBy { it.reputationScoreSolver }
                else -> filtered.sortedBy { it.reputationScore }
            }
        } else {
            filtered.sortedByDescending { it.createdAt }
        }
    }

    val pageSize = 10
    var currentPage by remember { mutableIntStateOf(1) }

    LaunchedEffect(searchQuery, selectedRoleFilter, sortByLowestReputation) {
        currentPage = 1
    }

    // Real DB-backed pagination (see ENGINEERING_NOTES.md §11) only applies to the plain,
    // unfiltered/unsorted browse case -- search and reputation-sort both need the full,
    // already-loaded `users` list to work correctly (Room can't full-text-search or
    // globally sort pages it hasn't fetched yet), so those fall back to the old
    // fully-in-memory behaviour below, unchanged.
    val isBrowsingUnfiltered = searchQuery.isBlank() && !sortByLowestReputation

    LaunchedEffect(selectedRoleFilter, isBrowsingUnfiltered) {
        if (isBrowsingUnfiltered) {
            viewModel.resetAdminUsersPagination(if (selectedRoleFilter == "ALL") null else selectedRoleFilter)
        }
    }

    // As the admin pages forward in browse mode, fetch more from the DB (which itself is kept
    // fresh by the one-shot pullUsers()/pull-to-refresh -- see §9/§10) instead of having
    // already downloaded everyone up front.
    LaunchedEffect(currentPage, isBrowsingUnfiltered) {
        if (isBrowsingUnfiltered) {
            val neededCount = currentPage * pageSize
            while (viewModel.adminUsersPaged.size < neededCount && viewModel.adminUsersHasMore && !viewModel.adminUsersLoadingMore) {
                viewModel.loadNextAdminUsersPage()
            }
        }
    }

    val totalPages = if (isBrowsingUnfiltered) {
        // No COUNT query -- keep the "next" button enabled while there's more to load, and
        // settle on the real page count once the DB says there's nothing left.
        if (viewModel.adminUsersHasMore) currentPage + 1
        else maxOf(1, (viewModel.adminUsersPaged.size + pageSize - 1) / pageSize)
    } else {
        maxOf(1, (filteredAndSortedUsers.size + pageSize - 1) / pageSize)
    }
    val safePage = currentPage.coerceIn(1, totalPages)
    val paginatedUsers: List<com.example.data.entity.UserEntity> = run {
        val source: List<com.example.data.entity.UserEntity> =
            if (isBrowsingUnfiltered) viewModel.adminUsersPaged else filteredAndSortedUsers
        val fromIndex = (safePage - 1) * pageSize
        if (fromIndex >= source.size) {
            emptyList()
        } else {
            // Bug fix: source.subList(...) previously returned Compose's SnapshotStateList
            // SubList view, which throws ConcurrentModificationException if the underlying
            // adminUsersPaged list is mutated (e.g. by loadNextAdminUsersPage()) while this
            // view is later read/iterated by LazyColumn. drop()/take() instead go through the
            // list's own snapshot-aware iterator and copy into a plain, safe List.
            source.drop(fromIndex).take(pageSize)
        }
    }

    // Ground Rule ২১, সেশন ২.১৯.৩ — realtime-এ সম্পূর্ণ নতুন insert হওয়া user id-গুলোর সংক্ষিপ্ত-
    // সময়ের সেট (viewModel থেকে সরাসরি, দেখো SomadhanViewModel.recentlyInsertedUserIds-এর কমেন্ট)।
    // নিচে items(...)-এর ভেতরে per-item membership-check-এ ব্যবহার হয়।
    val recentlyInsertedUserIds by viewModel.recentlyInsertedUserIds.collectAsStateWithLifecycle()

    // [অফলাইন Action Gating ধাপ ১২, গ্রুপ A ভেরিফিকেশন] outbox pending sync ইন্ডিকেটর
    // (adminSetBanned/adminSetRestricted/adminSetVerifiedBadge/adminChangeRole -সহ এই স্ক্রিনের
    // Group A ফাংশনগুলোর জন্য) — AdminWithdrawalsView/UserWalletScreen/WithdrawalHistoryScreen-এর
    // মতোই একই প্যাটার্ন, viewModel এখানে non-null বলে fallback দরকার নেই।
    val outboxPendingCount by viewModel.outboxPendingCount.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    // Ground Rule ২০ রেট্রোফিট, সেশন ২.২৯ — স্ক্রল-জাম্প ফিক্স: আগে এই effect coerced/clamped
    // `safePage`-এ key করা ছিল, যা কোনো item action-এ (ban/restrict/role change ইত্যাদি) সেই
    // ইউজার তালিকা থেকে বাদ পড়ে totalPages কমে গেলে নিজে থেকে বদলে যেত (ইউজার pagination না ছুঁলেও)
    // এবং ভুলভাবে টপে scroll করিয়ে দিত। raw `currentPage` state শুধু দুই জায়গায় বদলায় (filter/
    // search/sort reset, এই ফাইলের নিচের explicit next/prev ক্লিক) — action-এর side-effect-এ না।
    // pagination bounds-check-এর অন্য জায়গায় (paginatedUsers গণনায়) `safePage` অপরিবর্তিত থাকবে।
    LaunchedEffect(currentPage) {
        listState.scrollToItem(0)
    }

    // Ground Rule ২০ রেট্রোফিট, সেশন ২.২৯ — filter/search/sort/pagination বদলে দৃশ্যমান সব কার্ড
    // pulse করবে (২০২৬-০৯-১৫ সিদ্ধান্ত অনুযায়ী pagination বদলও filter/search-এর সমান)। try/finally
    // বাধ্যতামূলক (stuck-shimmer বাগ-১ ক্লাস এড়াতে) — কোনো কারণে effect cancel হলেও flag আটকে না থাকে।
    var isFilterRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(searchQuery, selectedRoleFilter, sortByLowestReputation, currentPage) {
        isFilterRefreshing = true
        try {
            delay(350L)
        } finally {
            isFilterRefreshing = false
        }
    }

    // Admin Panel Loading fix, সেশন ২.১৬ রেট্রোফিট (per-item pulse, ব্যবহারকারীর নতুন স্ট্যান্ডিং
    // সিদ্ধান্ত) — আগে এখানে একটা single whole-list rememberFieldChangePulse(value = paginatedUsers)
    // ছিল, যেটা যেকোনো একটা ইউজারের action (ব্যান/রেস্ট্রিক্ট/ব্যালেন্স/রোল/...) চাপলেই paginatedUsers
    // রেফারেন্স বদলে যেত বলে পুরো পাতার *সব* কার্ড একসাথে pulse করতো। এখন সেই কলটা নিচে
    // items(paginatedUsers) { user -> ... }-এর ভেতরে per-item সরানো হয়েছে (value = user, পুরো লিস্ট
    // না) — UserEntity data class বলে equals-ভিত্তিক তুলনায় শুধু যে ইউজারের ডেটা সত্যিই বদলেছে তার
    // কার্ডই pulse করবে। flashOnReentry = false per-item কলে (এখানে না, নিচে) — কারণ per-item
    // sessionKey/viewModel দিলে LazyColumn-এ স্ক্রল করে নতুন কার্ড প্রথমবার compose হওয়ার মুহূর্তেই
    // সেটা "re-entry" ধরে নিয়ে নিজে থেকে pulse দেখাতো (ব্যবহারকারীর অনিচ্ছাকৃত স্ক্রল-pulse) —
    // flashOnReentry বন্ধ রাখায় শুধু genuine data-change আর isManualRefreshing সম্পন্ন হওয়াতেই
    // pulse হবে, স্ক্রলে না।

    // 1. Ban Toggle Confirmation Dialog
    if (userForBanToggle != null) {
        val target = userForBanToggle!!
        val isCurrentlyBanned = target.cardBanned
        val cardRoleLabel = if (target.cardRole == "SOLVER") "সমাধানকারী" else "গ্রাহক"
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
                        "আপনি কি নিশ্চিত যে '${target.user.name}'-এর $cardRoleLabel পরিচয়ের ওপর থেকে ব্যান নিষেধাজ্ঞা প্রত্যাহার করতে চান? এতে শুধু তার $cardRoleLabel ভূমিকা প্রভাবিত হবে, অন্য ভূমিকা (যদি থাকে) অপরিবর্তিত থাকবে।"
                    else
                        "আপনি কি নিশ্চিত যে '${target.user.name}'-এর $cardRoleLabel পরিচয়কে ব্যান করতে চান? এতে শুধু তার $cardRoleLabel ভূমিকা প্রভাবিত হবে, অন্য ভূমিকা (যদি থাকে) অপরিবর্তিত থাকবে।",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminSetBanned(target.user.id, !isCurrentlyBanned, target.cardRole)
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

    // 2. Restrict Toggle Confirmation Dialog
    if (userForRestrictToggle != null) {
        val target = userForRestrictToggle!!
        val isCurrentlyRestricted = target.cardRestricted
        val cardRoleLabel = if (target.cardRole == "SOLVER") "সমাধানকারী" else "গ্রাহক"
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
                        "আপনি কি নিশ্চিত যে '${target.user.name}'-এর $cardRoleLabel পরিচয়ের সীমাবদ্ধতা (Restriction) প্রত্যাহার করতে চান? এতে শুধু তার $cardRoleLabel ভূমিকা প্রভাবিত হবে।"
                    else
                        "আপনি কি নিশ্চিত যে '${target.user.name}'-এর $cardRoleLabel পরিচয় রেস্ট্রিক্ট করতে চান? এতে শুধু তার $cardRoleLabel ভূমিকার কার্যকলাপ সীমিত হবে, অন্য ভূমিকা (যদি থাকে) অপরিবর্তিত থাকবে।",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminSetRestricted(target.user.id, !isCurrentlyRestricted, target.cardRole)
                        userForRestrictToggle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isCurrentlyRestricted) "সীমাবদ্ধতা তুলুন" else "হ্যাঁ, রেস্ট্রিক্ট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { userForRestrictToggle = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // 3. Balance Adjustment Dialog
    if (userForBalanceAdjust != null) {
        val target = userForBalanceAdjust!!
        val balanceRoleLabel = if (target.cardRole == "SOLVER") "সলভার" else "গ্রাহক"
        BottomSlideAlertDialog(
            onDismissRequest = { userForBalanceAdjust = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SomadhanOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ব্যালেন্স সমন্বয় - ${target.user.name}", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    // [ধাপ ৭ ফিক্স] generic `target.balance`-এর বদলে এই কার্ডের role-scoped
                    // ব্যালেন্স (cardBalance) — এবং কোন role সেটা স্পষ্ট করে লেবেল-সহ দেখানো হচ্ছে,
                    // যাতে dual-role ইউজারের ক্ষেত্রে admin ভুল করে অন্য role-এর ব্যালেন্স মনে
                    // না করে।
                    Text(
                        text = "বর্তমান ব্যালেন্স ($balanceRoleLabel): ৳ ${target.cardBalance.toInt()}",
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
                            val reason = balanceReasonInput.trim().ifBlank { if (isBalanceAddition) "অ্যাডমিন কর্তৃক ব্যালেন্স ক্রেডিট" else "অ্যাডমিন কর্তৃক ব্যালেন্স ডেবিট" }
                            // [ধাপ ৭ ফিক্স] target.cardRole এখন এক্সপ্লিসিটলি পাস করা হচ্ছে, যাতে
                            // এই কার্ডের role-টাই adjust হয় — অ্যাকাউন্টের বর্তমান সক্রিয় role যা-ই
                            // হোক না কেন।
                            viewModel.adminAdjustBalance(target.user.id, amount, isBalanceAddition, reason, role = target.cardRole)
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
        // [ADMIN_ROLE_PROFILE সেশন ৭.১.২, ২০২৬-০৯-২৫] "ADMIN" রেডিও-অপশন বাদ (বাগ-ফিক্স) — এটা বাছলে
        // শুধু legacy public.users.role='ADMIN' সেট হতো, কোনো admin_accounts রো তৈরি হতো না, তাই
        // ব্যক্তি নতুন মাল্টি-অ্যাডমিন প্যানেলে ঢুকতেই পারত না। এই ডায়ালগ এখন শুধু USER↔SOLVER টগলের
        // জন্যই (legacy, ঠিকই কাজ করে) — অ্যাডমিন বানাতে নিচের নতুন "বানাও অ্যাডমিন" অ্যাকশন ব্যবহার হবে।
        val roles = listOf(
            Triple("USER", "গ্রাহক (Customer / Problem Poster)", "সমস্যা পোস্ট ও সমাধান গ্রহণ করতে পারেন"),
            Triple("SOLVER", "সমাধানকারী (Solver / Service Provider)", "সমস্যা সমাধান ও বিড পেশ করতে পারেন")
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
                    Text(
                        text = "ব্যবহারকারীর নতুন ভূমিকা নির্বাচন করুন:",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    roles.forEach { (roleKey, roleTitle, roleDesc) ->
                        val isSelected = selectedNewRole == roleKey
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SomadhanOrangeLight else SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(
                                    1.dp,
                                    if (isSelected) SomadhanOrange else SomadhanDivider,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedNewRole = roleKey }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedNewRole = roleKey },
                                    colors = RadioButtonDefaults.colors(selectedColor = SomadhanOrange)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = roleTitle,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) SomadhanOrange else SomadhanTextPrimary
                                    )
                                    Text(text = roleDesc, fontSize = 11.sp, color = SomadhanTextSecondary)
                                }
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
                    Text("পরিবর্তন নিশ্চিত করুন")
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
        val repRoleLabel = if (target.cardRole == "SOLVER") "সলভার" else "গ্রাহক"
        BottomSlideAlertDialog(
            onDismissRequest = { userForReputationAdjust = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SomadhanOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("রেপুটেশন সমন্বয় - ${target.user.name}", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    // [ধাপ ৭খ ফিক্স] generic `target.reputationScore`-এর বদলে এই কার্ডের
                    // role-scoped স্কোর (cardReputationScore) — এবং কোন role সেটা স্পষ্ট করে
                    // লেবেল-সহ দেখানো হচ্ছে, যাতে dual-role ইউজারের ক্ষেত্রে admin ভুল করে অন্য
                    // role-এর স্কোর মনে না করে (balance-adjust dialog-এর ঠিক একই ফিক্স)।
                    Text(
                        text = "বর্তমান রেপুটেশন স্কোর ($repRoleLabel): ${target.cardReputationScore.toInt()} / ১০০",
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
                            // [ধাপ ৭খ ফিক্স] target.cardRole এখন এক্সপ্লিসিটলি পাস করা হচ্ছে,
                            // যাতে এই কার্ডের role-টাই adjust হয় — অ্যাকাউন্টের বর্তমান সক্রিয়
                            // role যা-ই হোক না কেন (balance-adjust-এর ঠিক একই ফিক্স)।
                            viewModel.adminAdjustReputation(target.user.id, change, note, role = target.cardRole)
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

    // 7. Delete User Confirmation Dialog
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
                        onDeleteUser(target.id)
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

    if (selectedUserForReputation != null) {
        val targetCard = selectedUserForReputation!!
        val targetUser = targetCard.user
        val reputationEvents by viewModel.getRecentReputationEvents(targetUser.id, limit = 20)
            .collectAsStateWithLifecycle(initialValue = emptyList())
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { selectedUserForReputation = null },
            sheetState = sheetState,
            containerColor = SomadhanBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = targetUser.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            RoleBadge(role = targetCard.cardRole)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "রেপুটেশন হিস্ট্রি (সাম্প্রতিক ২০টি)",
                            fontSize = 12.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                    ReputationBadge(score = targetCard.cardReputationScore)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = SomadhanDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                if (reputationEvents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "কোনো রেপুটেশন ইভেন্ট হিস্ট্রি পাওয়া যায়নি।",
                            fontSize = 13.sp,
                            color = SomadhanTextHint
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(reputationEvents, key = { it.id }) { event ->
                            val isPositive = event.scoreChange >= 0
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isPositive) SomadhanSuccess.copy(alpha = 0.3f) else SomadhanError.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (event.eventType) {
                                                "KYC_APPROVED" -> "KYC অনুমোদন"
                                                "PROBLEM_COMPLETED" -> "কাজ সম্পন্ন"
                                                "RATING_5_STAR" -> "৫ স্টার রেটিং"
                                                "RATING_LOW" -> "কম রেটিং"
                                                "EXTRA_CHARGE_ACCEPTED" -> "অতিরিক্ত চার্জ গৃহীত"
                                                "EXTRA_CHARGE_REJECTED" -> "অতিরিক্ত চার্জ প্রত্যাখ্যাত"
                                                "ADMIN_ADJUSTMENT" -> "অ্যাডমিন অ্যাডজাস্টমেন্ট"
                                                else -> event.eventType
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary
                                        )

                                        Text(
                                            text = if (event.scoreChange > 0) "+${event.scoreChange}" else "${event.scoreChange}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPositive) SomadhanSuccess else SomadhanError
                                        )
                                    }

                                    if (event.note.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = event.note,
                                            fontSize = 11.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${Formatters.formatDateBengali(event.createdAt)} (${Formatters.formatTimeAgo(event.createdAt)})",
                                            fontSize = 10.sp,
                                            color = SomadhanTextHint
                                        )
                                        Text(
                                            text = "পরিবর্তন পরবর্তী স্কোর: ${DistanceUtil.toBengaliDigits(event.scoreAfter.toInt().toString())}",
                                            fontSize = 10.sp,
                                            color = SomadhanTextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        val uid = targetUser.id
                        val roleForNav = targetCard.cardRole
                        selectedUserForReputation = null
                        onNavigateToReputation(uid, roleForNav)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanOrange),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanOrange)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = SomadhanOrange
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "সম্পূর্ণ রেপুটেশন বিস্তারিত পেজ দেখুন ↗",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanOrange
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // [অফলাইন Action Gating ধাপ ১২] outbox pending sync ইন্ডিকেটর (non-blocking, খালি থাকলে
        // কিছুই দেখায় না) — AdminWithdrawalsView-এর মতোই মূল লিস্ট-কার্ডের ঠিক ওপরে বসানো হলো।
        OutboxPendingIndicator(
            pendingCount = outboxPendingCount,
            onRetryClick = { viewModel.retryOutboxSyncNow() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ব্যবহারকারী তালিকা (${DistanceUtil.toBengaliDigits(filteredAndSortedUsers.size.toString())} জন)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sortByLowestReputation) SomadhanOrangeLight else SomadhanCardBg)
                            .border(1.dp, if (sortByLowestReputation) SomadhanOrange else SomadhanDivider, RoundedCornerShape(8.dp))
                            .clickable { sortByLowestReputation = !sortByLowestReputation }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = if (sortByLowestReputation) SomadhanOrange else SomadhanTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (sortByLowestReputation) "কম রেপুটেশন আগে ✓" else "সবচেয়ে কম রেপুটেশন আগে",
                                fontSize = 11.sp,
                                fontWeight = if (sortByLowestReputation) FontWeight.Bold else FontWeight.Normal,
                                color = if (sortByLowestReputation) SomadhanOrange else SomadhanTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("UID, নাম, ফোন বা ইমেইল দিয়ে সার্চ করুন...", fontSize = 12.sp, color = SomadhanTextHint) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SomadhanTextSecondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "মুছুন", tint = SomadhanTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        unfocusedBorderColor = SomadhanDivider,
                        focusedContainerColor = SomadhanCardBg,
                        unfocusedContainerColor = SomadhanCardBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALL" to "সকল", "USER" to "গ্রাহক", "SOLVER" to "সমাধানকারী").forEach { (roleKey, roleLabel) ->
                        val isSelected = selectedRoleFilter == roleKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) SomadhanOrange else SomadhanCardBg)
                                .border(1.dp, if (isSelected) SomadhanOrange else SomadhanDivider, RoundedCornerShape(16.dp))
                                .clickable { selectedRoleFilter = roleKey }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = roleLabel,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else SomadhanTextSecondary
                            )
                        }
                    }
                }
            }
        }

        if (filteredAndSortedUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("কোনো ইউজার পাওয়া যায়নি", fontSize = 13.sp, color = SomadhanTextHint)
            }
        } else {
            // [ধাপ ৮] প্রতিটা UserEntity-কে ১টা বা ২টা role-card-এ ভাঙা হচ্ছে। selectedRoleFilter
            // এখানেও আবার প্রয়োগ করা হচ্ছে (শুধু user-লেভেলে না) — নাহলে "SOLVER" ফিল্টারে একজন
            // dual-role ইউজারের USER কার্ডও (ভুলভাবে) দেখা যেত, কারণ user-লেভেল ফিল্টার শুধু
            // hasSolverRole=true কিনা দেখে, dual-role হলে hasUserRole-ও true থাকে।
            val paginatedCards: List<AdminUserRoleCard> = remember(paginatedUsers, selectedRoleFilter) {
                paginatedUsers.flatMap { it.toAdminRoleCards() }
                    .filter { card ->
                        when (selectedRoleFilter) {
                            "USER" -> card.cardRole == "USER"
                            "SOLVER" -> card.cardRole == "SOLVER"
                            else -> true
                        }
                    }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(paginatedCards, key = { "${it.user.id}_${it.cardRole}" }) { card ->
                val user = card.user
                val menuKey = "${user.id}_${card.cardRole}"
                // per-item pulse (উপরের কমেন্ট দেখো) — শুধু এই user-টার ডেটা সত্যিই বদলালে
                // (বা isManualRefreshing সম্পন্ন হলে) এই একটা কার্ডই pulse করবে; flashOnReentry =
                // false বলে স্ক্রল করে এই কার্ড প্রথমবার viewport-এ আসলে pulse হবে না।
                val userCardPulse = rememberFieldChangePulse(
                    value = user,
                    isManualRefreshing = isManualRefreshing,
                    sessionKey = "admin_users_sync",
                    viewModel = viewModel,
                    flashOnReentry = false
                )
                // Ground Rule ২১, সেশন ২.১৯.৩ — realtime-এ এই user সম্পূর্ণ **নতুন** insert
                // হয়ে থাকলে (recentlyInsertedUserIds-এ id মেম্বার থাকলে) এই কার্ডটাও কিছুক্ষণের
                // জন্য pulse করবে, যদিও userCardPulse-এর তুলনা এখানে কিছু ধরতে পারবে না (নতুন
                // composition-এর কোনো "আগের value" নেই, উপরের rememberFieldChangePulse ডকুমেন্ট
                // দেখো)। এই membership-check প্রতিটা recomposition-এ হয় বলে TTL শেষে সেট থেকে id
                // সরে গেলে pulse এমনিতেই থেমে যাবে — আলাদা কোনো LaunchedEffect/delay এখানে লাগেনি।
                val isNewFromRealtime = recentlyInsertedUserIds.contains(user.id)
                PulsingValue(isUpdating = userCardPulse || isFilterRefreshing || isNewFromRealtime) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                        .clickable { selectedUserForReputation = card }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(user.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                                RoleBadge(role = card.cardRole)

                                if (card.cardBanned) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SomadhanErrorLight)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "ব্যানড",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanError
                                        )
                                    }
                                }

                                if (card.cardRestricted) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFEF3C7))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "রেস্ট্রিক্টেড",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }

                                if (card.cardVerifiedBadge) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "ভেরিফাইড ব্যবহারকারী",
                                        tint = SomadhanInfo,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier.clickable {
                                        onNavigateToReputation(user.id, card.cardRole)
                                    }
                                ) {
                                    ReputationBadge(score = card.cardReputationScore)
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            val displayUidText = if (user.displayUid.isNotBlank()) user.displayUid else user.id
                            Text("UID: $displayUidText | ফোন: ${Formatters.toLocalDisplayFormat(user.phone)}", fontSize = 11.sp, color = SomadhanOrange, fontWeight = FontWeight.SemiBold)
                            Text("ইমেইল: ${user.email} | ঠিকানা: ${user.address}", fontSize = 11.sp, color = SomadhanTextSecondary)
                            // [ব্যালেন্স ফিক্স — ধাপ ১, dual-card split — ধাপ ৮] এই নির্দিষ্ট
                            // কার্ডের role-scoped balance (`balanceUser`/`balanceSolver`,
                            // cardRole অনুযায়ী) — active role বা শেয়ার্ড `balance` না।
                            Text("ব্যালেন্স: ৳ ${card.cardBalance.toInt()}", fontSize = 11.sp, color = SomadhanTextHint)
                            
                            // Account creation exact date and time
                            if (user.createdAt > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = SomadhanTextHint,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "অ্যাকাউন্ট তৈরি: ${Formatters.formatDateTimeBengali(user.createdAt)} (${Formatters.formatTimeAgo(user.createdAt)})",
                                        fontSize = 10.sp,
                                        color = SomadhanTextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("রেপুটেশন হিস্ট্রি দেখতে ট্যাপ করুন ℹ️", fontSize = 10.sp, color = SomadhanInfo)
                                Text(
                                    text = "• বিস্তারিত পেজ ↗",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanOrange,
                                    modifier = Modifier.clickable { onNavigateToReputation(user.id, card.cardRole) }
                                )
                            }
                        }

                        // More Actions Dropdown Menu
                        Box {
                            IconButton(onClick = { expandedMenuUserId = menuKey }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "অ্যাকশন অপশন", tint = SomadhanTextSecondary)
                            }

                            DropdownMenu(
                                expanded = expandedMenuUserId == menuKey,
                                onDismissRequest = { expandedMenuUserId = null },
                                modifier = Modifier.background(SomadhanCardBg)
                            ) {
                                // 1. ব্যান / আনব্যান (এই কার্ডের role-scoped, বাগ D২)
                                // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:users:ban" গেট
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (card.cardBanned) "আনব্যান করুন" else "ব্যান করুন",
                                            color = if (card.cardBanned) SomadhanSuccess else SomadhanError,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (card.cardBanned) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (card.cardBanned) SomadhanSuccess else SomadhanError,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    enabled = AdminSession.canAct("users:users:ban"),
                                    onClick = {
                                        expandedMenuUserId = null
                                        userForBanToggle = card
                                    }
                                )

                                // 2. রেস্ট্রিক্ট / রেস্ট্রিকশন প্রত্যাহার (এই কার্ডের role-scoped, বাগ D২)
                                // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:users:restrict" গেট
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (card.cardRestricted) "রেস্ট্রিকশন তুলুন" else "রেস্ট্রিক্ট করুন",
                                            color = SomadhanTextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (card.cardRestricted) Icons.Default.Check else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (card.cardRestricted) SomadhanSuccess else Color(0xFFD97706),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    enabled = AdminSession.canAct("users:users:restrict"),
                                    onClick = {
                                        expandedMenuUserId = null
                                        userForRestrictToggle = card
                                    }
                                )

                                // 3. ভেরিফাইড ব্যাজ (এই কার্ডের role-scoped, বাগ D৩)
                                // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:users:verified_badge" গেট
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (card.cardVerifiedBadge) "ভেরিফাইড ব্যাজ সরান" else "ভেরিফাইড ব্যাজ দিন",
                                            color = SomadhanTextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = if (card.cardVerifiedBadge) SomadhanTextHint else SomadhanInfo,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    enabled = AdminSession.canAct("users:users:verified_badge"),
                                    onClick = {
                                        expandedMenuUserId = null
                                        viewModel.adminSetVerifiedBadge(user.id, !card.cardVerifiedBadge, card.cardRole)
                                    }
                                )

                                // 4. ব্যালেন্স সমন্বয়
                                // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:users:balance_adjust" গেট
                                DropdownMenuItem(
                                    text = { Text("ব্যালেন্স সমন্বয় করুন", color = SomadhanTextPrimary, fontWeight = FontWeight.Medium) },
                                    leadingIcon = {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(18.dp))
                                    },
                                    enabled = AdminSession.canAct("users:users:balance_adjust"),
                                    onClick = {
                                        expandedMenuUserId = null
                                        // [ধাপ ৭ ফিক্স] `user` (plain UserEntity) না, এই `card`
                                        // (cardRole-সহ) — নিচের ডায়ালগে জানার জন্য কোন role-এর
                                        // ব্যালেন্স adjust হচ্ছে।
                                        userForBalanceAdjust = card
                                        balanceAmountInput = ""
                                        isBalanceAddition = true
                                        balanceReasonInput = ""
                                    }
                                )

                                // 5. রোল পরিবর্তন
                                // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:users:change_role" গেট
                                DropdownMenuItem(
                                    text = { Text("রোল পরিবর্তন করুন", color = SomadhanTextPrimary, fontWeight = FontWeight.Medium) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = SomadhanInfo, modifier = Modifier.size(18.dp))
                                    },
                                    enabled = AdminSession.canAct("users:users:change_role"),
                                    onClick = {
                                        expandedMenuUserId = null
                                        userForRoleChange = user
                                        selectedNewRole = user.role
                                    }
                                )

                                // 6. রেপুটেশন সমন্বয়
                                // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:users:score_adjust" গেট
                                DropdownMenuItem(
                                    text = { Text("রেপুটেশন সমন্বয় করুন", color = SomadhanTextPrimary, fontWeight = FontWeight.Medium) },
                                    leadingIcon = {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(18.dp))
                                    },
                                    enabled = AdminSession.canAct("users:users:score_adjust"),
                                    onClick = {
                                        expandedMenuUserId = null
                                        // [ধাপ ৭খ ফিক্স] `user` (plain UserEntity) না, এই `card`
                                        // (cardRole-সহ) — নিচের ডায়ালগে জানার জন্য কোন role-এর
                                        // স্কোর adjust হচ্ছে (balance-adjust-এর ঠিক একই ফিক্স)।
                                        userForReputationAdjust = card
                                        repScoreChangeInput = ""
                                        isRepAddition = true
                                        repNoteInput = ""
                                    }
                                )

                                // 7. পাসওয়ার্ড রিসেট
                                // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:users:reset_password" গেট
                                DropdownMenuItem(
                                    text = { Text("পাসওয়ার্ড রিসেট করুন", color = SomadhanTextPrimary, fontWeight = FontWeight.Medium) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(18.dp))
                                    },
                                    enabled = AdminSession.canAct("users:users:reset_password"),
                                    onClick = {
                                        expandedMenuUserId = null
                                        userForPasswordReset = user
                                        newPasswordInput = ""
                                    }
                                )

                                if (user.role != "ADMIN") {
                                    Divider(color = SomadhanDivider, thickness = 0.5.dp)

                                    // 7-খ. বানাও অ্যাডমিন
                                    // [ADMIN_ROLE_PROFILE সেশন ৭.১.২, ২০২৬-০৯-২৫] "users:users:create_admin" গেট
                                    // (hard-super-only, AdminSession.kt দ্রষ্টব্য)। ⚠️ আংশিক বাস্তবায়ন — শুধু
                                    // এন্ট্রি-পয়েন্ট + state সেট, আসল bottom sheet ফর্ম এখনো নেই (হ্যান্ডঅফ দ্রষ্টব্য)।
                                    DropdownMenuItem(
                                        text = { Text("বানাও অ্যাডমিন", color = SomadhanTextPrimary, fontWeight = FontWeight.Medium) },
                                        leadingIcon = {
                                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = SomadhanInfo, modifier = Modifier.size(18.dp))
                                        },
                                        enabled = AdminSession.canAct("users:users:create_admin"),
                                        onClick = {
                                            expandedMenuUserId = null
                                            userForCreateAdmin = user
                                        }
                                    )

                                    // 8. মুছে ফেলুন
                                    // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:users:delete" গেট
                                    DropdownMenuItem(
                                        text = { Text("মুছে ফেলুন", color = SomadhanError, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = SomadhanError, modifier = Modifier.size(18.dp))
                                        },
                                        enabled = AdminSession.canAct("users:users:delete"),
                                        onClick = {
                                            expandedMenuUserId = null
                                            userForDeleteConfirm = user
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
    }

        // Pagination Controls at bottom (Fixed)
        if (filteredAndSortedUsers.isNotEmpty()) {
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
                        onClick = { if (safePage > 1) currentPage = safePage - 1 },
                        enabled = safePage > 1,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পূর্ববর্তী", modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পূর্ববর্তী", fontSize = 11.sp)
                    }

                    Text(
                        text = "পেজ ${DistanceUtil.toBengaliDigits(safePage.toString())} / ${DistanceUtil.toBengaliDigits(totalPages.toString())}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    OutlinedButton(
                        onClick = { if (safePage < totalPages) currentPage = safePage + 1 },
                        enabled = safePage < totalPages,
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
