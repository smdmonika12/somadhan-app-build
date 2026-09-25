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
import com.example.ui.components.PulsingValue
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.ReputationBadge
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.example.ui.components.BottomSlideAlertDialog
import com.example.ui.components.AdminAccessLockedState
import com.example.data.security.AdminSession

@Composable
fun AdminSolverQuotaView(
    allUsers: List<UserEntity>,
    allProblems: List<ProblemEntity>,
    allTransactions: List<TransactionEntity>,
    viewModel: SomadhanViewModel,
    onNavigate: (String) -> Unit = {}
) {
    // [ADMIN_ROLE_PROFILE সেশন ৭.১] view-গেট — সবার আগে, কোনো composable কল হওয়ার আগেই early-return
    // (AdminAccessLockedState.kt-এর হেডার কমেন্টে বর্ণিত প্যাটার্ন অনুযায়ী)। "reset_quota"/
    // "reset_miss_cycle" অ্যাকশন-বাটন দুটো নিচে AdminSession.canAct(...) দিয়ে আলাদাভাবে গেট হয়েছে।
    if (!AdminSession.canView("users", "solver_quota")) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            AdminAccessLockedState()
        }
        return
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val platformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
    val isFreeQuotaGlobalEnabled = platformSettings.find { it.key == "free_quota_enabled" }?.value != "false"
    val isMissRuleGlobalEnabled = platformSettings.find { it.key == "extra_payment_miss_rule_enabled" }?.value != "false"

    val quotaThreshold = platformSettings.find { it.key == "free_quota_reputation_threshold" }?.value?.toDoubleOrNull() ?: 80.0
    val quotaLimit = platformSettings.find { it.key == "free_quota_job_count" }?.value?.toIntOrNull() ?: 10
    val cycleSize = platformSettings.find { it.key == "extra_payment_miss_cycle_size" }?.value?.toIntOrNull() ?: 10
    val missThreshold = platformSettings.find { it.key == "extra_payment_miss_threshold" }?.value?.toIntOrNull() ?: 3

    val currentMonthKey = remember {
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchingCloud by remember { mutableStateOf(false) }
    var selectedSolver by remember { mutableStateOf<UserEntity?>(null) }
    var searchExecuted by remember { mutableStateOf(false) }

    // Dialog state for quota reset
    var showQuotaResetDialog by remember { mutableStateOf(false) }
    // Dialog state for cycle reset
    var showCycleResetDialog by remember { mutableStateOf(false) }

    // Sync selected solver with latest live allUsers state
    // [বাগফিক্স — AdminUsersView.kt-এর একই single-row প্যাটার্নের বাগ] `role == "SOLVER"`
    // শুধু বর্তমান সক্রিয় role চেক করে — ব্যবহারকারী এখন User-মোডে থাকলে miss হয়ে যেত।
    // `hasSolverRole` (স্থায়ী ফ্ল্যাগ, role switch করলেও থাকে) যোগ করা হলো।
    val liveSolver = remember(selectedSolver, allUsers) {
        selectedSolver?.let { target ->
            allUsers.find { it.id == target.id }
                ?: allUsers.find { (it.role.equals("SOLVER", ignoreCase = true) || it.hasSolverRole) && (it.phone == target.phone || it.displayUid == target.displayUid) }
                ?: target
        }
    }

    // Admin Panel Loading fix, সেশন ২.২৩ — Ground Rule ১৯ (ব্যবহারকারীর কনফার্মড সিদ্ধান্ত:
    // GR18 এই ট্যাবে দরকার নেই, GR21 প্রযোজ্য না, শুধু সিলেক্টেড সলভারের ডিটেইল কার্ড pulse
    // করবে)। `LazyColumn`-এর trailing lambda (`LazyListScope.() -> Unit`) নিজে composable না
    // বলে `rememberFieldChangePulse` এখানে, `LazyColumn` কল করার আগেই, একবার কল করা হয়েছে —
    // `liveSolver` (nullable) সরাসরি value হিসেবে দিয়ে, যাতে conditional composable-call এড়ানো
    // যায়। নিচের তিনটা item{} (Profile/Quota/Cycle card) একই বুলিয়ান রিইউজ করে একসাথে pulse
    // করবে — অন্য কোনো ডিভাইস থেকে রিসেট হলে বা এই অ্যাডমিন নিজেই রিসেট করলে দুটোতেই।
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val solverDetailPulse = rememberFieldChangePulse(
        value = liveSolver,
        isManualRefreshing = isRefreshing
    )

    // Quick match list from local cache while typing - STRICTLY SOLVER ONLY
    val localMatches = remember(searchQuery, allUsers) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            emptyList()
        } else {
            allUsers.filter { u ->
                val isSolver = u.role.equals("SOLVER", ignoreCase = true) || u.hasSolverRole
                val matches = u.id.lowercase().contains(q) ||
                        u.displayUid.lowercase().contains(q) ||
                        u.phone.lowercase().contains(q) ||
                        u.name.lowercase().contains(q)
                isSolver && matches
            }.distinctBy { it.id }.take(5)
        }
    }

    fun performSearch(queryToSearch: String) {
        val queryClean = queryToSearch.trim()
        if (queryClean.isBlank()) {
            Toast.makeText(context, "অনুগ্রহ করে UID অথবা মোবাইল নম্বর লিখুন", Toast.LENGTH_SHORT).show()
            return
        }
        keyboardController?.hide()
        isSearchingCloud = true
        searchExecuted = true

        coroutineScope.launch {
            try {
                val user = viewModel.searchSolverForQuota(queryClean)
                if (user != null && user.role.equals("SOLVER", ignoreCase = true)) {
                    selectedSolver = user
                } else if (user != null && user.hasSolverRole) {
                    val matchingSolver = allUsers.find { (it.role.equals("SOLVER", ignoreCase = true) || it.hasSolverRole) && (it.phone == user.phone || it.displayUid == user.displayUid || it.id == user.linkedAccountId || it.linkedAccountId == user.id) }
                    selectedSolver = matchingSolver ?: user
                } else {
                    selectedSolver = user?.takeIf { it.role.equals("SOLVER", ignoreCase = true) || it.hasSolverRole }
                }
                if (selectedSolver == null) {
                    Toast.makeText(context, "উক্ত তথ্যের কোনো সলভার পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "অনুসন্ধান ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
            } finally {
                isSearchingCloud = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SomadhanBg)
    ) {
        // Top Search Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SomadhanCardBg,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SomadhanOrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "সলভার কোটা ও সাইকেল",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = "UID বা ফোন নম্বর দিয়ে লাইভ অনুসন্ধান",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }

                    // Global Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isFreeQuotaGlobalEnabled) SomadhanSuccessLight else SomadhanErrorLight)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isFreeQuotaGlobalEnabled) "কোটা সক্রিয়" else "কোটা নিষ্ক্রিয়",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFreeQuotaGlobalEnabled) SomadhanSuccess else SomadhanError
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("সলভারের UID, ফোন নম্বর বা নাম লিখুন...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performSearch(searchQuery) }),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "সার্চ",
                            tint = SomadhanOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSearchingCloud) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(end = 4.dp),
                                    strokeWidth = 2.dp,
                                    color = SomadhanOrange
                                )
                            } else if (searchQuery.isNotBlank()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    selectedSolver = null
                                    searchExecuted = false
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "মুছুন",
                                        tint = SomadhanTextHint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Button(
                                onClick = { performSearch(searchQuery) },
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("খুঁজুন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        unfocusedBorderColor = SomadhanBorder,
                        focusedContainerColor = SomadhanBg,
                        unfocusedContainerColor = SomadhanBg
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                // Live suggestion dropdown if typing
                AnimatedVisibility(visible = localMatches.isNotEmpty() && selectedSolver == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SomadhanBg)
                            .border(0.5.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                    ) {
                        localMatches.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSolver = user
                                        searchQuery = user.phone.ifBlank { user.displayUid }
                                        searchExecuted = true
                                        keyboardController?.hide()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(SomadhanOrangeLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = SomadhanOrange
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanTextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(SomadhanOrangeLight)
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "সলভার",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SomadhanOrange
                                                )
                                            }
                                        }
                                        Text(
                                            text = "UID: ${user.displayUid} • ${Formatters.toLocalDisplayFormat(user.phone)}",
                                            fontSize = 10.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = SomadhanTextHint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Divider(color = SomadhanDivider, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        // Content Body
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Selected Solver Details Card
            if (liveSolver != null) {
                val solver = liveSolver
                val userFreeQuotaTrx = allTransactions.count { trx ->
                    trx.solverId == solver.id &&
                            (trx.wasFreeQuotaJob || (trx.baseCommissionAmount == 0.0 && trx.commissionPercent == 0.0 && trx.grossAmount > 0.0)) &&
                            SimpleDateFormat("yyyy-MM", Locale.US).format(Date(trx.timestamp)) == currentMonthKey
                }
                val userActiveFreeProblems = allProblems.count { prob ->
                    prob.acceptedSolverId == solver.id &&
                            prob.status == "IN_PROGRESS" &&
                            prob.appliedCommissionRate == 0.0 &&
                            SimpleDateFormat("yyyy-MM", Locale.US).format(Date(prob.lastActivityAt ?: prob.createdAt)) == currentMonthKey
                }
                val storedUsed = if (solver.freeJobsMonthKey == currentMonthKey) solver.freeJobsUsedThisMonth else 0
                val totalUsedQuota = maxOf(storedUsed, userFreeQuotaTrx + userActiveFreeProblems)
                // [BALANCE_REPUTATION_ROLE_SEPARATION ধাপ ৭খ, ROLE_SEPARATION_AUDIT.md] পুরো
                // স্ক্রিনটাই সলভার-কোটা সংক্রান্ত (এই `solver` নিশ্চিতভাবে solver-role প্রেক্ষাপটে
                // ব্যবহৃত হচ্ছে) — তাই শেয়ার্ড `reputationScore`-এর বদলে role-scoped
                // `reputationScoreSolver` ব্যবহার করা হচ্ছে, dual-role ইউজারের User-role স্কোর
                // দিয়ে ভুল eligibility গণনা এড়াতে।
                val isEligible = solver.reputationScoreSolver >= quotaThreshold
                val remainingSlots = (quotaLimit - totalUsedQuota).coerceAtLeast(0)
                val isQuotaFull = totalUsedQuota >= quotaLimit
                val isNearPenalty = solver.cycleMissCount >= (missThreshold - 1) && solver.cycleMissCount > 0

                item {
                    // Profile Header Card
                    PulsingValue(isUpdating = solverDetailPulse) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        border = BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(SomadhanOrangeLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = solver.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrange,
                                        fontSize = 22.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = solver.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (solver.isKycVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "KYC Verified",
                                                tint = SomadhanSuccess,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "UID: ${solver.displayUid}  •  ফোন: ${Formatters.toLocalDisplayFormat(solver.phone)}",
                                        fontSize = 12.sp,
                                        color = SomadhanTextSecondary
                                    )
                                    if (solver.email.isNotBlank()) {
                                        Text(
                                            text = solver.email,
                                            fontSize = 11.sp,
                                            color = SomadhanTextHint
                                        )
                                    }
                                }

                                // Reputation Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isEligible) SomadhanSuccessLight else SomadhanOrangeLight)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (isEligible) SomadhanSuccess else SomadhanOrange,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                // [ধাপ ৭খ ফিক্স] উপরের isEligible-এর সাথে সামঞ্জস্যপূর্ণ
                                                // role-scoped স্কোর
                                                text = DistanceUtil.toBengaliDigits(String.format(Locale.US, "%.1f", solver.reputationScoreSolver)),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isEligible) SomadhanSuccess else SomadhanOrange
                                            )
                                        }
                                        Text(
                                            text = if (isEligible) "যোগ্য" else "অযোগ্য",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isEligible) SomadhanSuccess else SomadhanOrange
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = SomadhanDivider, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "সিস্টেম আইডি: ${solver.id.take(12)}...",
                                    fontSize = 10.sp,
                                    color = SomadhanTextHint
                                )
                                TextButton(
                                    onClick = { onNavigate(Screen.ReputationDetail.createRoute(solver.id, "SOLVER")) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("রেপুটেশন প্রোফাইল দেখুন ➔", fontSize = 11.sp, color = SomadhanOrange, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    } // close PulsingValue (Profile Header Card, GR19)
                }

                // 1. Free Quota Card
                item {
                    PulsingValue(isUpdating = solverDetailPulse) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        border = BorderStroke(1.dp, if (isEligible) SomadhanSuccess.copy(alpha = 0.5f) else SomadhanBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = if (isEligible) SomadhanSuccess else SomadhanTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "মাসিক ফ্রি-কমিশন কোটা ($currentMonthKey)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SomadhanTextPrimary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (!isEligible) SomadhanBorder.copy(alpha = 0.5f) else if (isQuotaFull) SomadhanErrorLight else SomadhanSuccessLight)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (!isEligible) "রেপুটেশন কম (<${DistanceUtil.toBengaliDigits(quotaThreshold.toInt().toString())})" else if (isQuotaFull) "কোটা শেষ" else "সক্রিয় কোটা",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isEligible) SomadhanTextHint else if (isQuotaFull) SomadhanError else SomadhanSuccess
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ব্যবহৃত স্লট: ${DistanceUtil.toBengaliDigits(totalUsedQuota.toString())} / ${DistanceUtil.toBengaliDigits(quotaLimit.toString())} কাজ",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanTextPrimary
                                )
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(((totalUsedQuota.toFloat() / quotaLimit.coerceAtLeast(1)) * 100).toInt().toString())}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isQuotaFull) SomadhanError else SomadhanSuccess
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (totalUsedQuota.toFloat() / quotaLimit.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (isQuotaFull) SomadhanError else SomadhanSuccess,
                                trackColor = if (isQuotaFull) SomadhanErrorLight else SomadhanSuccessLight
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (!isEligible)
                                    "সলভারের রেপুটেশন স্কোর ${DistanceUtil.toBengaliDigits(quotaThreshold.toInt().toString())}+ না থাকায় বর্তমানে ফ্রি কোটা সুবিধা প্রযোজ্য নয়।"
                                else if (remainingSlots > 0)
                                    "চলতি মাসে আরও ${DistanceUtil.toBengaliDigits(remainingSlots.toString())}টি কাজ ০% কমিশনে সম্পন্ন করতে পারবেন।"
                                else
                                    "এই মাসের সমস্ত ফ্রি কোটা ব্যবহৃত হয়েছে। পরবর্তী কাজগুলোতে স্বাভাবিক কমিশন প্রযোজ্য হবে।",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = SomadhanDivider, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Button: Reset Quota to 0
                            // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:solver_quota:reset_quota" গেট
                            Button(
                                onClick = { showQuotaResetDialog = true },
                                enabled = AdminSession.canAct("users:solver_quota:reset_quota"),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("এই সলভারের কোটা ০ করুন", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    } // close PulsingValue (Free Quota Card, GR19)
                }

                // 2. Extra Payment Miss Cycle Card
                item {
                    PulsingValue(isUpdating = solverDetailPulse) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        border = BorderStroke(1.dp, if (isNearPenalty) SomadhanError.copy(alpha = 0.5f) else SomadhanBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = if (isNearPenalty) SomadhanError else SomadhanOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Extra Payment মিস সাইকেল ট্র্যাকিং",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SomadhanTextPrimary
                                    )
                                }

                                if (isNearPenalty) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SomadhanErrorLight)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = SomadhanError,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "পেনাল্টি ঝুঁকি",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanError
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "সাইকেল অগ্রগতি: ${DistanceUtil.toBengaliDigits(solver.cycleJobCount.toString())} / ${DistanceUtil.toBengaliDigits(cycleSize.toString())} কাজ",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanTextPrimary
                                )
                                Text(
                                    text = "মিস: ${DistanceUtil.toBengaliDigits(solver.cycleMissCount.toString())} / ${DistanceUtil.toBengaliDigits(missThreshold.toString())}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNearPenalty) SomadhanError else SomadhanTextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (solver.cycleJobCount.toFloat() / cycleSize.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = SomadhanOrange,
                                trackColor = SomadhanOrangeLight
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "প্রতি ${DistanceUtil.toBengaliDigits(cycleSize.toString())}টি কাজের সাইকেলে সর্বোচ্চ ${DistanceUtil.toBengaliDigits(missThreshold.toString())}টি মিস অনুমোদিত। সাইকেল পূর্ণ হলে কাউন্টার স্বয়ংক্রিয়ভাবে নতুন সাইকেলে প্রবেশ করে।",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = SomadhanDivider, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Button: Reset Miss Cycle
                            // [ADMIN_ROLE_PROFILE সেশন ৭.১] "users:solver_quota:reset_miss_cycle" গেট
                            OutlinedButton(
                                onClick = { showCycleResetDialog = true },
                                enabled = AdminSession.canAct("users:solver_quota:reset_miss_cycle"),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanTextPrimary),
                                border = BorderStroke(1.dp, SomadhanBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = SomadhanTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("এই সলভারের সাইকেল রিসেট করুন", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    } // close PulsingValue (Extra Payment Miss Cycle Card, GR19)
                }
            } else {
                // Empty / Instructional State
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        border = BorderStroke(1.dp, SomadhanBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(SomadhanOrangeLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = SomadhanOrange,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (searchExecuted) "সলভার পাওয়া যায়নি" else "সলভার অনুসন্ধান করুন",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (searchExecuted)
                                    "উক্ত UID বা মোবাইল নম্বরের সাথে মিলে এমন কোনো সলভার পাওয়া যায়নি। সঠিক তথ্য দিয়ে পুনরায় চেষ্টা করুন।"
                                else
                                    "নির্দিষ্ট সলভারের ফ্রি কোটা স্ট্যাটাস পর্যবেক্ষণ ও রিসেট করতে উপরে সলভারের UID অথবা ফোন নম্বর লিখে খুঁজুন।",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(18.dp))
                            Divider(color = SomadhanDivider, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            // Global Rules Overview
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SomadhanBg)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "বর্তমান প্ল্যাটফর্ম গ্লোবাল কনফিগারেশন:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )
                                Text(
                                    text = "• ন্যূনতম রেপুটেশন যোগ্যতা: ${DistanceUtil.toBengaliDigits(String.format(Locale.US, "%.0f", quotaThreshold))} পয়েন্ট",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary
                                )
                                Text(
                                    text = "• মাসিক ফ্রি কমিশন কোটা: প্রতি মাসে ${DistanceUtil.toBengaliDigits(quotaLimit.toString())}টি কাজ",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary
                                )
                                Text(
                                    text = "• Extra Payment সাইকেল সাইজ: ${DistanceUtil.toBengaliDigits(cycleSize.toString())}টি কাজে সর্বোচ্চ ${DistanceUtil.toBengaliDigits(missThreshold.toString())}টি মিস",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Quota Reset
    if (showQuotaResetDialog && liveSolver != null) {
        BottomSlideAlertDialog(
            onDismissRequest = { showQuotaResetDialog = false },
            title = {
                Text("কোটা রিসেট নিশ্চিতকরণ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    "আপনি কি নিশ্চিত যে সলভার '${liveSolver.name}'-এর চলতি মাসের ($currentMonthKey) ব্যবহৃত ফ্রি কোটা ০-এ রিসেট করতে চান?",
                    fontSize = 13.sp,
                    color = SomadhanTextPrimary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminResetSolverFreeQuota(liveSolver.id)
                        showQuotaResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange)
                ) {
                    Text("হ্যাঁ, রিসেট করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuotaResetDialog = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            },
            containerColor = SomadhanCardBg,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Confirmation Dialog for Cycle Reset
    if (showCycleResetDialog && liveSolver != null) {
        BottomSlideAlertDialog(
            onDismissRequest = { showCycleResetDialog = false },
            title = {
                Text("সাইকেল রিসেট নিশ্চিতকরণ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    "আপনি কি নিশ্চিত যে সলভার '${liveSolver.name}'-এর Extra Payment সাইকেল কাউন্টার ও মিস কাউন্ট রিসেট করতে চান?",
                    fontSize = 13.sp,
                    color = SomadhanTextPrimary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminResetSolverMissCycle(liveSolver.id)
                        showCycleResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange)
                ) {
                    Text("হ্যাঁ, রিসেট করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCycleResetDialog = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            },
            containerColor = SomadhanCardBg,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
