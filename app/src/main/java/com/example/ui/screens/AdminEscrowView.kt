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
import androidx.compose.material.icons.filled.Build
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
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.ReputationBadge
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
import com.example.data.repository.MissingRefundRepairReport
import com.example.data.repository.BalanceReconciliationReport
import com.example.data.repository.MissingRefundRepairItem
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.components.PulsingValue
import com.example.ui.components.rememberFieldChangePulse
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

@Composable
fun AdminEscrowView(
    heldEscrows: List<EscrowEntity>,
    refundedEscrows: List<EscrowEntity> = emptyList(),
    releasedEscrows: List<EscrowEntity> = emptyList(),
    allUsers: List<UserEntity> = emptyList(),
    viewModel: SomadhanViewModel,
    isManualRefreshing: Boolean = false
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var selectedEscrowTab by rememberSaveable { mutableIntStateOf(0) } // 0 = HELD, 1 = RELEASED, 2 = REFUNDED
    var searchQuery by remember { mutableStateOf("") }
    val pageSize = 10
    var currentPage by rememberSaveable { mutableIntStateOf(1) }

    LaunchedEffect(searchQuery, selectedEscrowTab) {
        currentPage = 1
    }

    // Ground Rule ২০: ফিল্টার/সার্চ/ট্যাব/pagination বদলে দৃশ্যমান সব এসক্রো-কার্ড একসাথে pulse করাবে
    var isFilterRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(searchQuery, selectedEscrowTab, currentPage) {
        isFilterRefreshing = true
        try {
            delay(350L)
        } finally {
            isFilterRefreshing = false
        }
    }

    // Ground Rule ২১, সেশন ২.১৯.৬ — নতুন escrow তৈরি (Insert) অথবা HELD→RELEASED/REFUNDED status
    // transition (Update) হলে শুধু সেই কার্ডটাই pulse করবে (তিনটা তালিকাতেই — Held/Released/
    // Refunded — একই সেট reuse করা হয়েছে, Withdrawal-এর প্যাটার্নেই)।
    val recentlyChangedEscrowIds by viewModel.recentlyChangedEscrowIds.collectAsStateWithLifecycle()

    val totalHeldAmount = heldEscrows.sumOf { it.baseAmount + it.extraAmount }
    val stuckEscrowsCount = heldEscrows.count {
        val days = ((System.currentTimeMillis() - it.createdAt) / (1000L * 60 * 60 * 24)).toInt()
        days >= 7
    }

    val totalReleasedAmount = releasedEscrows.sumOf { it.baseAmount + it.extraAmount }
    val totalRefundedAmount = refundedEscrows.sumOf { it.baseAmount + it.extraAmount }

    val filteredHeldEscrows = remember(heldEscrows, searchQuery, allUsers) {
        if (searchQuery.isBlank()) heldEscrows else {
            val q = searchQuery.trim().lowercase()
            heldEscrows.filter { escrow ->
                val customerName = allUsers.find { it.id == escrow.userId }?.name?.lowercase() ?: ""
                val solverName = allUsers.find { it.id == escrow.solverId }?.name?.lowercase() ?: ""
                escrow.id.lowercase().contains(q) ||
                escrow.problemId.lowercase().contains(q) ||
                escrow.problemTitle.lowercase().contains(q) ||
                escrow.userId.lowercase().contains(q) ||
                escrow.solverId.lowercase().contains(q) ||
                customerName.contains(q) ||
                solverName.contains(q)
            }
        }
    }

    val filteredReleasedEscrows = remember(releasedEscrows, searchQuery, allUsers) {
        if (searchQuery.isBlank()) releasedEscrows else {
            val q = searchQuery.trim().lowercase()
            releasedEscrows.filter { escrow ->
                val customerName = allUsers.find { it.id == escrow.userId }?.name?.lowercase() ?: ""
                val solverName = allUsers.find { it.id == escrow.solverId }?.name?.lowercase() ?: ""
                escrow.id.lowercase().contains(q) ||
                escrow.problemId.lowercase().contains(q) ||
                escrow.problemTitle.lowercase().contains(q) ||
                escrow.userId.lowercase().contains(q) ||
                escrow.solverId.lowercase().contains(q) ||
                customerName.contains(q) ||
                solverName.contains(q)
            }
        }
    }

    val filteredRefundedEscrows = remember(refundedEscrows, searchQuery, allUsers) {
        if (searchQuery.isBlank()) refundedEscrows else {
            val q = searchQuery.trim().lowercase()
            refundedEscrows.filter { escrow ->
                val customerName = allUsers.find { it.id == escrow.userId }?.name?.lowercase() ?: ""
                val solverName = allUsers.find { it.id == escrow.solverId }?.name?.lowercase() ?: ""
                escrow.id.lowercase().contains(q) ||
                escrow.problemId.lowercase().contains(q) ||
                escrow.problemTitle.lowercase().contains(q) ||
                escrow.userId.lowercase().contains(q) ||
                escrow.solverId.lowercase().contains(q) ||
                customerName.contains(q) ||
                solverName.contains(q)
            }
        }
    }

    val currentEscrowList = when (selectedEscrowTab) {
        0 -> filteredHeldEscrows
        1 -> filteredReleasedEscrows
        2 -> filteredRefundedEscrows
        else -> filteredHeldEscrows
    }
    val totalPages = maxOf(1, (currentEscrowList.size + pageSize - 1) / pageSize)
    val safePage = currentPage.coerceIn(1, totalPages)
    val paginatedEscrows = remember(currentEscrowList, safePage, pageSize) {
        val fromIndex = (safePage - 1) * pageSize
        if (fromIndex >= currentEscrowList.size) {
            emptyList()
        } else {
            currentEscrowList.subList(fromIndex, minOf(fromIndex + pageSize, currentEscrowList.size))
        }
    }

    val escrowListState = rememberLazyListState()
    // Ground Rule ২০ scroll-jump ফিক্স: coerced safePage-এর বদলে raw currentPage-এ key করা হয়েছে,
    // যাতে release/refund action-এ totalPages কমে safePage প্যাসিভভাবে বদলে গেলে ভুল scroll-to-top
    // ট্রিগার না হয়। selectedEscrowTab আগের মতোই key-তে আছে (ট্যাব বদল একটা genuine navigation change)।
    LaunchedEffect(currentPage, selectedEscrowTab) {
        escrowListState.scrollToItem(0)
    }

    var escrowToRelease by remember { mutableStateOf<EscrowEntity?>(null) }
    var escrowToRefund by remember { mutableStateOf<EscrowEntity?>(null) }

    if (escrowToRelease != null) {
        val target = escrowToRelease!!
        val amount = target.baseAmount + target.extraAmount
        BottomSlideAlertDialog(
            onDismissRequest = { escrowToRelease = null },
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
                    text = "টাকা ছেড়ে দেওয়া নিশ্চিত করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Text(
                    text = "সলভারকে ৳${DistanceUtil.toBengaliDigits(amount.toInt().toString())} প্রদান করা হবে, নিশ্চিত?",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminReleaseEscrow(target)
                        escrowToRelease = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess)
                ) {
                    Text("হ্যাঁ, টাকা ছেড়ে দিন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { escrowToRelease = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    if (escrowToRefund != null) {
        val target = escrowToRefund!!
        val amount = target.baseAmount + target.extraAmount
        BottomSlideAlertDialog(
            onDismissRequest = { escrowToRefund = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = SomadhanError,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "টাকা ফেরত নিশ্চিত করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Text(
                    text = "ইউজারকে ৳${DistanceUtil.toBengaliDigits(amount.toInt().toString())} ফেরত দেওয়া হবে, নিশ্চিত?",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminRefundEscrow(target)
                        escrowToRefund = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError)
                ) {
                    Text("হ্যাঁ, ফেরত দিন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { escrowToRefund = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    var showRepairModal by remember { mutableStateOf(false) }
    var isRepairRunning by remember { mutableStateOf(false) }
    var auditReport by remember { mutableStateOf<MissingRefundRepairReport?>(null) }
    var showLiveConfirmDialog by remember { mutableStateOf(false) }

    var showReconcileModal by remember { mutableStateOf(false) }
    var isReconcileRunning by remember { mutableStateOf(false) }
    var reconcileReport by remember { mutableStateOf<BalanceReconciliationReport?>(null) }
    var showReconcileLiveConfirmDialog by remember { mutableStateOf(false) }

    if (showRepairModal) {
        BottomSlideAlertDialog(
            onDismissRequest = {
                if (!isRepairRunning) {
                    showRepairModal = false
                    auditReport = null
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = SomadhanOrange,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "মিসিং রিফান্ড অডিট ও রিপেয়ার 🛠️",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    Text(
                        text = "বাতিলকৃত কাজের জন্য কোনো ইউজারের অ্যাকাউন্টে রিফান্ড মিসিং থাকলে তা সনাক্ত ও নিরাপদে ওয়ালেটে জমা করার টুল। (Idempotency সুরক্ষিত)",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isRepairRunning) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = SomadhanOrange, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("প্রসেসিং হচ্ছে, অনুগ্রহ করে অপেক্ষা করুন...", fontSize = 12.sp, color = SomadhanTextSecondary)
                            }
                        }
                    } else if (auditReport == null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🔍 ধাপ ১: ড্রাই-রান অডিট (Dry Run)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "প্রথমে একটি নিরাপদ ড্রাই-রান অডিট চালানো হবে যা কোনো ডাটাবেস পরিবর্তন না করেই মিসিং রিফান্ডগুলো স্ক্যান করে তালিকা তৈরি করবে।",
                                    fontSize = 11.sp,
                                    color = SomadhanTextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isRepairRunning = true
                                viewModel.adminRepairMissingRefunds(dryRun = true) { report ->
                                    auditReport = report
                                    isRepairRunning = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_dry_run_audit_btn")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ড্রাই-রান অডিট শুরু করুন", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }
                    } else {
                        val report = auditReport!!
                        // Summary Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (report.missingRefundCount > 0) SomadhanOrangeLight else SomadhanSuccessLight
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (report.dryRun) "📋 ড্রাই-রান অডিট রিপোর্ট" else "✅ লাইভ রিপেয়ার ফলাফল",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (report.missingRefundCount > 0) SomadhanOrange else SomadhanSuccess
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "মোট স্ক্যানকৃত এসক্রো: ${DistanceUtil.toBengaliDigits(report.scannedEscrowsCount.toString())} টি",
                                    fontSize = 11.sp,
                                    color = SomadhanTextPrimary
                                )
                                Text(
                                    text = if (report.dryRun) "মিসিং রিফান্ড শনাক্ত: ${DistanceUtil.toBengaliDigits(report.missingRefundCount.toString())} টি"
                                           else "সফলভাবে পুনরুদ্ধার: ${DistanceUtil.toBengaliDigits(report.repairedCount.toString())} টি",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (report.missingRefundCount > 0) SomadhanOrange else SomadhanSuccess
                                )
                                Text(
                                    text = "মোট অর্থ: ৳ ${DistanceUtil.toBengaliDigits(report.totalAmountRepairedOrAudited.toInt().toString())}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (report.items.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("সব ডাটা সঠিক রয়েছে। কোনো রিফান্ড মিসিং নেই।", fontSize = 12.sp, color = SomadhanTextSecondary)
                                }
                            }
                        } else {
                            Text(
                                text = "সনাক্তকৃত আইটেমসমূহ (${DistanceUtil.toBengaliDigits(report.items.size.toString())}টি):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            report.items.forEach { item ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = item.problemTitle,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanTextPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "৳${DistanceUtil.toBengaliDigits(item.totalAmount.toInt().toString())}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanOrange
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "সমস্যা আইডি: #${item.problemId.take(8).uppercase()} | ইউজার: ${item.userId.take(8)}",
                                            fontSize = 10.sp,
                                            color = SomadhanTextHint
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.message,
                                            fontSize = 10.sp,
                                            color = if (item.status == "REPAIRED") SomadhanSuccess else SomadhanTextSecondary
                                        )
                                    }
                                }
                            }

                            if (report.dryRun && report.missingRefundCount > 0) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { showLiveConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("execute_live_repair_btn")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("২. বাস্তবে ওয়ালেটে রিফান্ড জমা দিন (Live Repair)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (auditReport != null && !isRepairRunning) {
                    TextButton(onClick = { auditReport = null }) {
                        Text("পুনরায় অডিট", color = SomadhanOrange, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRepairModal = false
                        auditReport = null
                    },
                    enabled = !isRepairRunning
                ) {
                    Text("বন্ধ করুন", color = SomadhanTextSecondary, fontSize = 12.sp)
                }
            }
        )
    }

    if (showLiveConfirmDialog) {
        val totalAmount = auditReport?.totalAmountRepairedOrAudited ?: 0.0
        val count = auditReport?.missingRefundCount ?: 0
        BottomSlideAlertDialog(
            onDismissRequest = { showLiveConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = SomadhanOrange,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "লাইভ রিফান্ড নিশ্চিতকরণ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Text(
                    text = "${DistanceUtil.toBengaliDigits(count.toString())}টি মিসিং রিফান্ডের মোট ৳${DistanceUtil.toBengaliDigits(totalAmount.toInt().toString())} টাকা ব্যবহারকারীদের অ্যাকাউন্টে ক্রেডিট করা হবে। আপনি কি নিশ্চিত?",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLiveConfirmDialog = false
                        isRepairRunning = true
                        viewModel.adminRepairMissingRefunds(dryRun = false) { report ->
                            auditReport = report
                            isRepairRunning = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess)
                ) {
                    Text("হ্যাঁ, রিফান্ড সম্পন্ন করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLiveConfirmDialog = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    if (showReconcileModal) {
        BottomSlideAlertDialog(
            onDismissRequest = {
                if (!isReconcileRunning) {
                    showReconcileModal = false
                    reconcileReport = null
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = SomadhanOrange,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "ব্যালেন্স রিকনসিলিয়েশন 🔍",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    Text(
                        text = "প্রতিটি ব্যবহারকারীর সব লেনদেন (transaction ledger) যোগ করে গণনা করা ব্যালেন্সের সাথে তাদের বর্তমান স্টোরড ব্যালেন্স মিলিয়ে দেখা হয় — কোথাও গরমিল থাকলে এখানে দেখাবে। প্রথমে ড্রাই-রান (শুধু দেখাবে, কিছু বদলাবে না), তারপর চাইলে সংশোধন।",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isReconcileRunning) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = SomadhanOrange, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("স্ক্যান করা হচ্ছে...", fontSize = 12.sp, color = SomadhanTextSecondary)
                            }
                        }
                    } else if (reconcileReport == null) {
                        Button(
                            onClick = {
                                isReconcileRunning = true
                                viewModel.adminReconcileBalances(dryRun = true) { report ->
                                    reconcileReport = report
                                    isReconcileRunning = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_balance_reconcile_audit_btn")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("অডিট শুরু করুন", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }
                    } else {
                        val report = reconcileReport!!
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (report.mismatchCount > 0) SomadhanErrorLight else SomadhanSuccessLight
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (report.mismatchCount > 0) "⚠️ ${report.mismatchCount}টি গরমিল পাওয়া গেছে" else "✅ কোনো গরমিল পাওয়া যায়নি",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (report.mismatchCount > 0) SomadhanError else SomadhanSuccess
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "স্ক্যান করা হয়েছে ${DistanceUtil.toBengaliDigits(report.scannedUsersCount.toString())} জন ব্যবহারকারী, মোট পার্থক্য ৳${DistanceUtil.toBengaliDigits(report.totalAbsoluteDifference.toInt().toString())}",
                                    fontSize = 11.sp,
                                    color = SomadhanTextPrimary
                                )
                            }
                        }

                        if (report.mismatchCount > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            report.items.take(20).forEach { item ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ৪ ফলো-আপ] item.role
                                    // (ধাপ ৪-এ যোগ হয়েছে) এখানে দেখানো হচ্ছে -- একই userId-এর জন্য
                                    // এখন USER আর SOLVER role-এ আলাদা আলাদা mismatch item আসতে পারে,
                                    // চিপ ছাড়া কোনটা কোন role-এর তা বোঝা যেত না।
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.userName.ifBlank { item.userId },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SomadhanTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (item.role == "SOLVER") "সলভার" else "ইউজার",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextSecondary,
                                            modifier = Modifier
                                                .background(SomadhanDivider, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "স্টোরড ৳${item.storedBalance.toInt()} → লেজার ৳${item.ledgerBalance.toInt()} (পার্থক্য ৳${item.difference.toInt()})",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                                Divider(color = SomadhanDivider, thickness = 0.5.dp)
                            }
                            if (report.items.size > 20) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "... এবং আরও ${report.items.size - 20}টি (তালিকায় শুধু প্রথম ২০টি দেখানো হচ্ছে)",
                                    fontSize = 11.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (reconcileReport != null && reconcileReport!!.mismatchCount > 0 && !isReconcileRunning) {
                    Button(
                        onClick = { showReconcileLiveConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess)
                    ) {
                        Text("সংশোধন করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (reconcileReport != null && !isReconcileRunning) {
                    TextButton(onClick = { reconcileReport = null }) {
                        Text("আবার চালান", color = SomadhanOrange)
                    }
                }
                OutlinedButton(
                    onClick = {
                        showReconcileModal = false
                        reconcileReport = null
                    },
                    enabled = !isReconcileRunning
                ) {
                    Text("বন্ধ করুন", color = SomadhanTextSecondary)
                }
            }
        )
    }

    if (showReconcileLiveConfirmDialog) {
        val mismatchCount = reconcileReport?.mismatchCount ?: 0
        val totalDiff = reconcileReport?.totalAbsoluteDifference ?: 0.0
        BottomSlideAlertDialog(
            onDismissRequest = { showReconcileLiveConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = SomadhanOrange,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "ব্যালেন্স সংশোধন নিশ্চিতকরণ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Text(
                    text = "${DistanceUtil.toBengaliDigits(mismatchCount.toString())}টি ব্যবহারকারীর ব্যালেন্স তাদের লেজার (transaction history) অনুযায়ী সংশোধন করা হবে (মোট পার্থক্য ৳${DistanceUtil.toBengaliDigits(totalDiff.toInt().toString())})। আপনি কি নিশ্চিত?",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReconcileLiveConfirmDialog = false
                        isReconcileRunning = true
                        viewModel.adminReconcileBalances(dryRun = false) { report ->
                            reconcileReport = report
                            isReconcileRunning = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess)
                ) {
                    Text("হ্যাঁ, সংশোধন করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showReconcileLiveConfirmDialog = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("admin_escrow_search_bar"),
            placeholder = { Text("এসক্রো ID, সমস্যা ID, সমস্যা, ইউজার বা সলভার দিয়ে খুঁজুন...", fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = SomadhanTextHint
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(16.dp),
                            tint = SomadhanTextHint
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SomadhanOrange,
                unfocusedBorderColor = SomadhanBorder
            )
        )

        // Sub-Tab Switcher (3 Tabs: Held, Completed/Released, Refunded/Cancelled)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                0 to "সুরক্ষিত / HELD (${DistanceUtil.toBengaliDigits(heldEscrows.size.toString())})",
                1 to "সম্পন্ন (${DistanceUtil.toBengaliDigits(releasedEscrows.size.toString())})",
                2 to "বাতিল / রিফান্ড (${DistanceUtil.toBengaliDigits(refundedEscrows.size.toString())})"
            )
            tabs.forEach { (index, label) ->
                val isSelected = selectedEscrowTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SomadhanOrange else SomadhanCardBg)
                        .border(1.dp, if (isSelected) SomadhanOrange else SomadhanDivider, RoundedCornerShape(8.dp))
                        .clickable { selectedEscrowTab = index }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else SomadhanTextPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        when (selectedEscrowTab) {
            0 -> {
                // HELD ESCROWS LIST
            LazyColumn(
                state = escrowListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "সুরক্ষিত Escrow ফান্ডসমূহ (${DistanceUtil.toBengaliDigits(filteredHeldEscrows.size.toString())}টি)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                }
                                StatusBadge(status = "HELD")
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "মোট সুরক্ষিত অর্থ: ৳ ${DistanceUtil.toBengaliDigits(totalHeldAmount.toInt().toString())}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanSuccess
                            )

                            if (stuckEscrowsCount > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SomadhanErrorLight)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("admin_escrow_stuck_warning")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = SomadhanError,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${DistanceUtil.toBengaliDigits(stuckEscrowsCount.toString())}টি এসক্রো ৭ দিনের বেশি সময় ধরে আটকে আছে!",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanError
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredHeldEscrows.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (heldEscrows.isEmpty()) "বর্তমানে কোনো HELD Escrow ফান্ড নেই" else "অনুসন্ধানের সাথে মিল রেখে কোনো এসক্রো পাওয়া যায়নি",
                                fontSize = 13.sp,
                                color = SomadhanTextHint
                            )
                        }
                    }
                } else {
                    items(paginatedEscrows, key = { it.id }) { escrow ->
                        val totalAmount = escrow.baseAmount + escrow.extraAmount
                        val daysHeld = ((System.currentTimeMillis() - escrow.createdAt) / (1000L * 60 * 60 * 24)).toInt()
                        val isStuck = daysHeld >= 7

                        val customerUser = allUsers.find { it.id == escrow.userId }
                        val solverUser = allUsers.find { it.id == escrow.solverId }

                        val escrowCardPulse = rememberFieldChangePulse(
                            value = escrow,
                            isManualRefreshing = isManualRefreshing,
                            sessionKey = "admin_escrow_sync",
                            viewModel = viewModel,
                            flashOnReentry = false
                        )
                        // Ground Rule ২১ — নতুন insert/status-transition হলে এই তৃতীয় স্বাধীন
                        // কারণেও pulse করবে (Held তালিকা)।
                        val isNewFromRealtime = recentlyChangedEscrowIds.contains(escrow.id)
                        PulsingValue(isUpdating = escrowCardPulse || isFilterRefreshing || isNewFromRealtime) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isStuck) SomadhanErrorLight.copy(alpha = 0.35f) else SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isStuck) 1.5.dp else 1.dp,
                                    color = if (isStuck) SomadhanError else SomadhanDivider,
                                    shape = RoundedCornerShape(10.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (escrow.problemTitle.isNotBlank()) escrow.problemTitle else "সমস্যা আইডি: ${escrow.problemId}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary
                                        )
                                        if (escrow.problemId.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    clipboardManager.setText(AnnotatedString(escrow.problemId))
                                                    Toast.makeText(context, "পোস্ট আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text(
                                                    text = "পোস্ট আইডি: #${escrow.problemId.take(8).uppercase()}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SomadhanOrange
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "কপি",
                                                    tint = SomadhanOrange,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "এসক্রো আইডি: ${escrow.id}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SomadhanOrange
                                        )
                                    }
                                    if (isStuck) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(SomadhanError)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "আটকে আছে (${DistanceUtil.toBengaliDigits(daysHeld.toString())} দিন)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        StatusBadge(status = "HELD")
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "গ্রাহক: ${customerUser?.name ?: (customerUser?.displayUid?.takeIf { it.isNotBlank() } ?: escrow.userId)}",
                                            fontSize = 11.sp,
                                            color = SomadhanTextSecondary
                                        )
                                        Text(
                                            text = "সলভার: ${solverUser?.name ?: (solverUser?.displayUid?.takeIf { it.isNotBlank() } ?: escrow.solverId)}",
                                            fontSize = 11.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "৳ ${DistanceUtil.toBengaliDigits(totalAmount)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanOrange
                                        )
                                        if (escrow.extraAmount > 0.0) {
                                            Text(
                                                text = "(মূল ৳ ${DistanceUtil.toBengaliDigits(escrow.baseAmount)} + অতিরিক্ত ৳ ${DistanceUtil.toBengaliDigits(escrow.extraAmount)})",
                                                fontSize = 9.sp,
                                                color = SomadhanTextHint
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Divider(color = SomadhanDivider, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "হোল্ড সময়: ${Formatters.formatDateTimeBengali(escrow.createdAt)}",
                                        fontSize = 10.sp,
                                        color = SomadhanTextHint
                                    )
                                    Text(
                                        text = "ধরে রাখা হয়েছে: ${DistanceUtil.toBengaliDigits(daysHeld.toString())} দিন আগে (${Formatters.formatTimeAgo(escrow.createdAt)})",
                                        fontSize = 10.sp,
                                        fontWeight = if (isStuck) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isStuck) SomadhanError else SomadhanTextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = SomadhanDivider, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Action Buttons: Release & Refund
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { escrowToRelease = escrow },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(if (isStuck) 42.dp else 36.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isStuck) SomadhanSuccess else SomadhanSuccess.copy(alpha = 0.9f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        elevation = if (isStuck) ButtonDefaults.buttonElevation(defaultElevation = 4.dp) else ButtonDefaults.buttonElevation()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isStuck) "টাকা ছেড়ে দিন (Release) ⚡" else "টাকা ছেড়ে দিন (Release)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Button(
                                        onClick = { escrowToRefund = escrow },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(if (isStuck) 42.dp else 36.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isStuck) SomadhanError else SomadhanError.copy(alpha = 0.9f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        elevation = if (isStuck) ButtonDefaults.buttonElevation(defaultElevation = 4.dp) else ButtonDefaults.buttonElevation()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isStuck) "ফেরত দিন (Refund) ⚡" else "ফেরত দিন (Refund)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
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
        1 -> {
                // RELEASED / COMPLETED ESCROWS LIST (Success / Finished)
                LazyColumn(
                    state = escrowListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SomadhanSuccess,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "সম্পন্ন / রিলিজড Escrow ফান্ডসমূহ (${DistanceUtil.toBengaliDigits(filteredReleasedEscrows.size.toString())}টি)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SomadhanSuccessLight)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "পেমেন্ট সম্পন্ন",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanSuccess
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "মোট রিলিজকৃত অর্থ: ৳ ${DistanceUtil.toBengaliDigits(totalReleasedAmount.toInt().toString())}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanSuccess
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "সমস্যা সমাধান যাচাইয়ের পর সফলভাবে সলভারকে প্রদানকৃত এসক্রো ফান্ডসমূহ এখানে সংরক্ষিত আছে।",
                                    fontSize = 11.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        }
                    }

                    if (filteredReleasedEscrows.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (releasedEscrows.isEmpty()) "বর্তমানে কোনো সম্পন্ন Escrow ফান্ড নেই" else "অনুসন্ধানের সাথে মিল রেখে কোনো সম্পন্ন রেকর্ড পাওয়া যায়নি",
                                    fontSize = 13.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        }
                    } else {
                        items(paginatedEscrows, key = { it.id }) { escrow ->
                            val totalAmount = escrow.baseAmount + escrow.extraAmount
                            val customerUser = allUsers.find { it.id == escrow.userId }
                            val solverUser = allUsers.find { it.id == escrow.solverId }

                            val escrowCardPulse = rememberFieldChangePulse(
                                value = escrow,
                                isManualRefreshing = isManualRefreshing,
                                sessionKey = "admin_escrow_sync",
                                viewModel = viewModel,
                                flashOnReentry = false
                            )
                            // Ground Rule ২১ — নতুন insert/status-transition হলে এই তৃতীয় স্বাধীন
                            // কারণেও pulse করবে (Released তালিকা)।
                            val isNewFromRealtime = recentlyChangedEscrowIds.contains(escrow.id)
                            PulsingValue(isUpdating = escrowCardPulse || isFilterRefreshing || isNewFromRealtime) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
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
                                            Text(
                                                text = if (escrow.problemTitle.isNotBlank()) escrow.problemTitle else "সমস্যা আইডি: ${escrow.problemId}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanTextPrimary
                                            )
                                            if (escrow.problemId.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable {
                                                        clipboardManager.setText(AnnotatedString(escrow.problemId))
                                                        Toast.makeText(context, "পোস্ট আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Text(
                                                        text = "পোস্ট আইডি: #${escrow.problemId.take(8).uppercase()}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SomadhanOrange
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "কপি",
                                                        tint = SomadhanOrange,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "এসক্রো ID: ${escrow.id}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SomadhanSuccess
                                                )
                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(escrow.id))
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.ContentCopy,
                                                        contentDescription = "Copy Escrow ID",
                                                        modifier = Modifier.size(12.dp),
                                                        tint = SomadhanTextHint
                                                    )
                                                }
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(SomadhanSuccessLight)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "RELEASED",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanSuccess
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "গ্রাহক: ${customerUser?.name ?: (customerUser?.displayUid?.takeIf { it.isNotBlank() } ?: escrow.userId)}",
                                                    fontSize = 11.sp,
                                                    color = SomadhanTextSecondary
                                                )
                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(customerUser?.displayUid?.takeIf { it.isNotBlank() } ?: escrow.userId))
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.ContentCopy,
                                                        contentDescription = "Copy User UID",
                                                        modifier = Modifier.size(11.dp),
                                                        tint = SomadhanTextHint
                                                    )
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "সলভার: ${solverUser?.name ?: (solverUser?.displayUid?.takeIf { it.isNotBlank() } ?: escrow.solverId)}",
                                                    fontSize = 11.sp,
                                                    color = SomadhanTextSecondary
                                                )
                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(solverUser?.displayUid?.takeIf { it.isNotBlank() } ?: escrow.solverId))
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.ContentCopy,
                                                        contentDescription = "Copy Solver UID",
                                                        modifier = Modifier.size(11.dp),
                                                        tint = SomadhanTextHint
                                                    )
                                                }
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "৳ ${DistanceUtil.toBengaliDigits(totalAmount)}",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanSuccess
                                            )
                                            if (escrow.extraAmount > 0.0) {
                                                Text(
                                                    text = "(মূল ৳ ${DistanceUtil.toBengaliDigits(escrow.baseAmount)} + অতিরিক্ত ৳ ${DistanceUtil.toBengaliDigits(escrow.extraAmount)})",
                                                    fontSize = 9.sp,
                                                    color = SomadhanTextHint
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Divider(color = SomadhanDivider, thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "তৈরির সময়: ${Formatters.formatDateTimeBengali(escrow.createdAt)}",
                                            fontSize = 10.sp,
                                            color = SomadhanTextHint
                                        )
                                        Text(
                                            text = if (escrow.releasedAt != null && escrow.releasedAt > 0L) "রিলিজের সময়: ${Formatters.formatDateTimeBengali(escrow.releasedAt)}" else "সফলভাবে সম্পন্ন",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SomadhanSuccess
                                        )
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
            2 -> {
                // REFUNDED ESCROWS LIST (Solver Cancelled / Admin Refunded)
            LazyColumn(
                state = escrowListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = SomadhanError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "রিফান্ডকৃত Escrow ফান্ডসমূহ (${DistanceUtil.toBengaliDigits(filteredRefundedEscrows.size.toString())}টি)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SomadhanErrorLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "রিফান্ড সম্পন্ন",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanError
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "মোট রিফান্ডকৃত অর্থ: ৳ ${DistanceUtil.toBengaliDigits(totalRefundedAmount.toInt().toString())}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanError
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "সলভার নিজে কাজ বাতিল করলে অথবা অ্যাডমিন কর্তৃক রিফান্ডকৃত এসক্রো ফান্ডসমূহ এখানে প্রদর্শিত হচ্ছে।",
                                fontSize = 11.sp,
                                color = SomadhanTextHint
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = SomadhanDivider, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { showRepairModal = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SomadhanOrange
                                ),
                                border = BorderStroke(1.dp, SomadhanOrange),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_repair_missing_refunds_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = SomadhanOrange
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "মিসিং রিফান্ড অডিট ও রিপেয়ার 🛠️",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { showReconcileModal = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SomadhanOrange
                                ),
                                border = BorderStroke(1.dp, SomadhanOrange),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_reconcile_balances_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = SomadhanOrange
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ব্যালেন্স রিকনসিলিয়েশন 🔍",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )
                            }
                        }
                    }
                }

                if (filteredRefundedEscrows.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (refundedEscrows.isEmpty()) "বর্তমানে কোনো রিফান্ডকৃত Escrow ফান্ড নেই" else "অনুসন্ধানের সাথে মিল রেখে কোনো রিফান্ড রেকর্ড পাওয়া যায়নি",
                                fontSize = 13.sp,
                                color = SomadhanTextHint
                            )
                        }
                    }
                } else {
                    items(paginatedEscrows, key = { it.id }) { escrow ->
                        val totalAmount = escrow.baseAmount + escrow.extraAmount
                        val customerUser = allUsers.find { it.id == escrow.userId }
                        val solverUser = allUsers.find { it.id == escrow.solverId }

                        val escrowCardPulse = rememberFieldChangePulse(
                            value = escrow,
                            isManualRefreshing = isManualRefreshing,
                            sessionKey = "admin_escrow_sync",
                            viewModel = viewModel,
                            flashOnReentry = false
                        )
                        // Ground Rule ২১ — নতুন insert/status-transition হলে এই তৃতীয় স্বাধীন
                        // কারণেও pulse করবে (Refunded তালিকা)।
                        val isNewFromRealtime = recentlyChangedEscrowIds.contains(escrow.id)
                        PulsingValue(isUpdating = escrowCardPulse || isFilterRefreshing || isNewFromRealtime) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
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
                                        Text(
                                            text = if (escrow.problemTitle.isNotBlank()) escrow.problemTitle else "সমস্যা আইডি: ${escrow.problemId}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary
                                        )
                                        if (escrow.problemId.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    clipboardManager.setText(AnnotatedString(escrow.problemId))
                                                    Toast.makeText(context, "পোস্ট আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text(
                                                    text = "পোস্ট আইডি: #${escrow.problemId.take(8).uppercase()}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SomadhanOrange
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "কপি",
                                                    tint = SomadhanOrange,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "এসক্রো আইডি: ${escrow.id}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SomadhanOrange
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (escrow.status == "REFUND_PENDING_SYNC") SomadhanOrangeLight else SomadhanErrorLight)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (escrow.status == "REFUND_PENDING_SYNC") "REFUND PENDING SYNC" else "REFUNDED",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (escrow.status == "REFUND_PENDING_SYNC") SomadhanOrange else SomadhanError
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "গ্রাহক: ${customerUser?.name ?: (customerUser?.displayUid?.takeIf { it.isNotBlank() } ?: escrow.userId)}",
                                            fontSize = 11.sp,
                                            color = SomadhanTextSecondary
                                        )
                                        Text(
                                            text = "সলভার: ${solverUser?.name ?: (solverUser?.displayUid?.takeIf { it.isNotBlank() } ?: escrow.solverId)}",
                                            fontSize = 11.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "৳ ${DistanceUtil.toBengaliDigits(totalAmount)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanError
                                        )
                                        if (escrow.extraAmount > 0.0) {
                                            Text(
                                                text = "(মূল ৳ ${DistanceUtil.toBengaliDigits(escrow.baseAmount)} + অতিরিক্ত ৳ ${DistanceUtil.toBengaliDigits(escrow.extraAmount)})",
                                                fontSize = 9.sp,
                                                color = SomadhanTextHint
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Divider(color = SomadhanDivider, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "তৈরির সময়: ${Formatters.formatDateTimeBengali(escrow.createdAt)}",
                                        fontSize = 10.sp,
                                        color = SomadhanTextHint
                                    )
                                    Text(
                                        text = if (escrow.releasedAt != null && escrow.releasedAt > 0L) "রিফান্ডের সময়: ${Formatters.formatDateTimeBengali(escrow.releasedAt)}" else "বাতিলকৃত / রিফান্ডকৃত",
                                        fontSize = 10.sp,
                                        color = SomadhanError
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

        // Pagination Bar (Matches Problem List / Withdrawals)
        if (currentEscrowList.isNotEmpty()) {
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "পূর্ববর্তী",
                            modifier = Modifier.size(13.dp)
                        )
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "পরবর্তী",
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
