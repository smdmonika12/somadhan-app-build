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

@Composable
fun StatusLegendItem(
    label: String,
    count: Int,
    percent: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, color = SomadhanTextSecondary)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            "${DistanceUtil.toBengaliDigits(count.toString())}টি (${DistanceUtil.toBengaliDigits(percent.toString())}%)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SomadhanTextPrimary
        )
    }
}


@Composable
fun AdminWithdrawalsView(
    withdrawals: List<WithdrawalEntity>,
    allUsers: List<UserEntity> = emptyList(),
    viewModel: SomadhanViewModel? = null,
    isManualRefreshing: Boolean = false,
    onUpdateStatus: (WithdrawalEntity, String, String?) -> Unit = { w, s, t ->
        viewModel?.adminUpdateWithdrawalStatus(w, s, t)
    }
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var isSelectMode by remember { mutableStateOf(false) }
    var selectedWithdrawalIds by remember { mutableStateOf(setOf<String>()) }
    val pageSize = 10
    var currentPage by remember { mutableIntStateOf(1) }

    LaunchedEffect(searchQuery, selectedStatusFilter) {
        currentPage = 1
    }

    // Admin Panel Loading fix, সেশন ২.৩১ (Ground Rule ২০ retrofit) — Ground Rule ১৯-এর per-item
    // pulse (নিচে withdrawalCardPulse) item-level equality দিয়ে শুধু action-এ বদলানো
    // item-ই ধরে, ফিল্টার/সার্চ/pagination বদলে item নিজে বদলায় না বলে কোনো pulse হতো না।
    // Ground Rule ২০ অনুযায়ী এই তিনটা বদলে (আপডেটেড স্কোপ অনুযায়ী pagination-ও) দৃশ্যমান সব কার্ড
    // pulse করা উচিত। currentPage এই key-তে ইচ্ছাকৃতভাবে আছে (২০২৬-০৯-১৫ সিদ্ধান্তে
    // "pagination-only = no pulse" নিয়মটা উল্টে গেছে)। try/finally বাধ্যতামূলক (bug-1 ক্লাস
    // স্টাক-শিমার এড়াতে — খুব দ্রুত পরপর ফিল্টার বদলালে আগের coroutine cancel হতে পারে)।
    var isFilterRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(searchQuery, selectedStatusFilter, currentPage) {
        isFilterRefreshing = true
        try {
            delay(350L)
        } finally {
            isFilterRefreshing = false
        }
    }

    // Ground Rule ২১, সেশন ২.১৯.৫ — নতুন PENDING রিকোয়েস্ট (Insert) অথবা COMPLETED/REJECTED-এ
    // status বদলানো (Update) হওয়া withdrawal-দের id-র সংক্ষিপ্ত-সময়ের সেট (viewModel থেকে সরাসরি,
    // দেখো SomadhanViewModel.recentlyChangedWithdrawalIds-এর কমেন্ট)। viewModel এই ফাইলে ঐচ্ছিক
    // বলে null হলে খালি সেট fallback (AdminKycView-এর প্যাটার্নেই)।
    val recentlyChangedWithdrawalIds by viewModel?.recentlyChangedWithdrawalIds?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(emptySet<String>()) }

    // ধাপ ৮ (RPC_SYNC_FIX ট্র্যাক) — outbox pending sync ইন্ডিকেটর
    // (adminUpdateWithdrawalStatus-সহ withdrawal-সংক্রান্ত outbox-wired ফাংশনগুলোর জন্য)।
    // viewModel এই ফাইলে ঐচ্ছিক বলে null হলে 0 fallback (উপরের একই প্যাটার্ন)।
    val outboxPendingCount by viewModel?.outboxPendingCount?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(0) }

    var showBulkApproveDialog by remember { mutableStateOf(false) }
    var bulkTrxId by remember { mutableStateOf("") }

    var singleWithdrawalToApprove by remember { mutableStateOf<WithdrawalEntity?>(null) }
    var singleWithdrawalToReject by remember { mutableStateOf<WithdrawalEntity?>(null) }
    var singleTrxId by remember { mutableStateOf("") }
    var rejectReasonInput by remember { mutableStateOf("") }

    var withdrawalForEditTrx by remember { mutableStateOf<WithdrawalEntity?>(null) }
    var editTrxInput by remember { mutableStateOf("") }

    var selectedWithdrawalForDetail by remember { mutableStateOf<WithdrawalEntity?>(null) }

    val pendingCount = remember(withdrawals) { withdrawals.count { it.status == "PENDING" } }
    val completedCount = remember(withdrawals) { withdrawals.count { it.status == "COMPLETED" } }
    val rejectedCount = remember(withdrawals) { withdrawals.count { it.status == "REJECTED" } }

    val filteredWithdrawals = remember(withdrawals, selectedStatusFilter, searchQuery) {
        val query = searchQuery.trim().lowercase()
        withdrawals.filter { item ->
            val matchesStatus = when (selectedStatusFilter) {
                "PENDING" -> item.status == "PENDING"
                "COMPLETED" -> item.status == "COMPLETED"
                "REJECTED" -> item.status == "REJECTED"
                else -> true
            }
            val matchesQuery = query.isEmpty() ||
                item.solverId.lowercase().contains(query) ||
                item.solverName.lowercase().contains(query) ||
                item.accountNumber.lowercase().contains(query) ||
                item.method.lowercase().contains(query) ||
                (item.trxId ?: "").lowercase().contains(query) ||
                (item.bankName ?: "").lowercase().contains(query) ||
                (item.rejectionReason ?: "").lowercase().contains(query) ||
                item.id.lowercase().contains(query)

            matchesStatus && matchesQuery
        }
    }

    val totalPages: Int
    val safePage: Int
    val paginatedWithdrawals: List<WithdrawalEntity>
    // Real DB-backed pagination (see ENGINEERING_NOTES.md §11) only for the plain
    // unfiltered/unsearched browse case -- status filter and text search both need the full,
    // already-loaded `withdrawals` list (same trade-off documented for AdminUsersView), so
    // those fall back to the original fully-in-memory behaviour, unchanged.
    val isBrowsingUnfilteredWithdrawals = searchQuery.isBlank() && selectedStatusFilter == "ALL"
    if (viewModel != null) {
        LaunchedEffect(isBrowsingUnfilteredWithdrawals) {
            if (isBrowsingUnfilteredWithdrawals) {
                viewModel.resetAdminWithdrawalsPagination()
            }
        }
        LaunchedEffect(currentPage, isBrowsingUnfilteredWithdrawals) {
            if (isBrowsingUnfilteredWithdrawals) {
                val neededCount = currentPage * pageSize
                while (viewModel.adminWithdrawalsPaged.size < neededCount && viewModel.adminWithdrawalsHasMore && !viewModel.adminWithdrawalsLoadingMore) {
                    viewModel.loadNextAdminWithdrawalsPage()
                }
            }
        }
        totalPages = if (isBrowsingUnfilteredWithdrawals) {
            if (viewModel.adminWithdrawalsHasMore) currentPage + 1
            else maxOf(1, (viewModel.adminWithdrawalsPaged.size + pageSize - 1) / pageSize)
        } else {
            maxOf(1, (filteredWithdrawals.size + pageSize - 1) / pageSize)
        }
        safePage = currentPage.coerceIn(1, totalPages)
        paginatedWithdrawals = run {
            val source: List<WithdrawalEntity> =
                if (isBrowsingUnfilteredWithdrawals) viewModel.adminWithdrawalsPaged else filteredWithdrawals
            val fromIndex = (safePage - 1) * pageSize
            if (fromIndex >= source.size) emptyList()
            else source.drop(fromIndex).take(pageSize) // safe: avoids SnapshotStateList's SubList CME (see AdminUsersView.kt)
        }
    } else {
        // No viewModel provided (e.g. preview) -- old fully-in-memory behaviour.
        totalPages = maxOf(1, (filteredWithdrawals.size + pageSize - 1) / pageSize)
        safePage = currentPage.coerceIn(1, totalPages)
        paginatedWithdrawals = run {
            val fromIndex = (safePage - 1) * pageSize
            if (fromIndex >= filteredWithdrawals.size) emptyList()
            else filteredWithdrawals.drop(fromIndex).take(pageSize) // safe: avoids SnapshotStateList's SubList CME (see AdminUsersView.kt)
        }
    }

    val listState = rememberLazyListState()
    // Admin Panel Loading fix, সেশন ২.৩১ (Ground Rule ২০ scroll-jump ফিক্স) — আগে এই effect
    // coerced `safePage`-এ key করা ছিল, যেটা approve/reject-এর ফলে totalPages কমে গিয়ে
    // স্বয়ংক্রিয়ভাবে ক্ল্যাম্প হয়ে ভুলভাবে টপে scroll-jump ঘটাতো (ইউজার pagination না ছুঁলেও)।
    // raw `currentPage`-এ re-key করা হলো — এটা শুধু filter/search reset আর explicit next/prev
    // ক্লিকে বদলায়, action-এর side-effect-এ না। অন্য কোথাও pagination bounds-check এখনো safePage
    // ব্যবহার করে, এটা শুধু এই scroll-effect-এর key।
    LaunchedEffect(currentPage) {
        listState.scrollToItem(0)
    }

    val selectedPendingWithdrawals = remember(withdrawals, selectedWithdrawalIds) {
        withdrawals.filter { it.id in selectedWithdrawalIds && it.status == "PENDING" }
    }
    val selectedTotalAmount = remember(selectedPendingWithdrawals) {
        selectedPendingWithdrawals.sumOf { it.amount }
    }

    // --- Bulk Approve Dialog ---
    if (showBulkApproveDialog) {
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
                    text = "বাল্ক উইথড্র অনুমোদন নিশ্চিতকরণ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "আপনি ${DistanceUtil.toBengaliDigits(selectedPendingWithdrawals.size.toString())}টি অপেক্ষমাণ উইথড্র রিকোয়েস্ট একবারে অনুমোদন করতে যাচ্ছেন।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "মোট পরিশোধযোগ্য অঙ্ক: ৳ ${DistanceUtil.toBengaliDigits(selectedTotalAmount.toInt().toString())}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanOrange
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = bulkTrxId,
                        onValueChange = { bulkTrxId = it },
                        label = { Text("ব্যাচ / ট্রানজেকশন আইডি (ঐচ্ছিক)") },
                        placeholder = { Text("যেমন: BATCH-PAY-01 বা TrxID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanDivider
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trx = bulkTrxId.trim().ifBlank { null }
                        // [Somadhan Bug-Fix Step 3 — গ্রুপ ২.২] আগে এখানে একটা unconditional "N-টি
                        // সফলভাবে অনুমোদন করা হয়েছে" toast দেখানো হতো, প্রতিটা `onUpdateStatus()`
                        // কল আসলে সফল হলো কিনা তা যাচাই না করেই। এখন `onUpdateStatus` (→
                        // viewModel.adminUpdateWithdrawalStatus) নিজেই প্রতিটা withdrawal-এর জন্য
                        // repository-র real ফলাফল (Updated/GuardBlocked) অনুযায়ী নিজস্ব conditional
                        // toast দেখায় -- তাই এই ব্লকেট toast টা বাদ দেওয়া হলো, প্রতিটা আইটেমের real
                        // status caller-কে জানানো হবে।
                        selectedPendingWithdrawals.forEach { w ->
                            onUpdateStatus(w, "COMPLETED", trx)
                        }
                        selectedWithdrawalIds = emptySet()
                        isSelectMode = false
                        showBulkApproveDialog = false
                        bulkTrxId = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("অনুমোদন সম্পন্ন করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkApproveDialog = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // --- Single Item Approve Dialog with TrxID ---
    if (singleWithdrawalToApprove != null) {
        val target = singleWithdrawalToApprove!!
        BottomSlideAlertDialog(
            onDismissRequest = {
                singleWithdrawalToApprove = null
                singleTrxId = ""
            },
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
                    text = "উইথড্র অনুমোদন করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "${target.solverName} (উইথড্র আইডি: ${target.id})-এর ৳${DistanceUtil.toBengaliDigits(target.amount.toInt().toString())} উইথড্র (${target.method}: ${target.accountNumber}) অনুমোদন করতে চান?",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = singleTrxId,
                        onValueChange = { singleTrxId = it },
                        label = { Text("ট্রানজেকশন / TrxID (ঐচ্ছিক)") },
                        placeholder = { Text("যেমন: 9J3K89LM") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trx = singleTrxId.trim().ifBlank { null }
                        onUpdateStatus(target, "COMPLETED", trx)
                        singleWithdrawalToApprove = null
                        singleTrxId = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("অনুমোদন করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    singleWithdrawalToApprove = null
                    singleTrxId = ""
                }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // --- Single Item Reject Dialog ---
    if (singleWithdrawalToReject != null) {
        val target = singleWithdrawalToReject!!
        BottomSlideAlertDialog(
            onDismissRequest = {
                singleWithdrawalToReject = null
                rejectReasonInput = ""
            },
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
                    text = "উইথড্র বাতিল নিশ্চিতকরণ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "আপনি কি নিশ্চিত যে ${target.solverName} (উইথড্র আইডি: ${target.id})-এর ৳${DistanceUtil.toBengaliDigits(target.amount.toInt().toString())} উইথড্র রিকোয়েস্ট বাতিল করতে চান? অর্থ সলভারের ব্যালেন্সে জমা থাকবে।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rejectReasonInput,
                        onValueChange = { rejectReasonInput = it },
                        label = { Text("বাতিলের কারণ (ঐচ্ছিক)") },
                        placeholder = { Text("যেমন: ভুল অ্যাকাউন্ট নম্বর দেওয়া হয়েছে") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = rejectReasonInput.trim().ifBlank { "কোনো নির্দিষ্ট কারণ উল্লেখ করা হয়নি" }
                        onUpdateStatus(target, "REJECTED", reason)
                        singleWithdrawalToReject = null
                        rejectReasonInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("হ্যাঁ, বাতিল করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    singleWithdrawalToReject = null
                    rejectReasonInput = ""
                }) {
                    Text("ফিরে যান", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // --- Edit TrxID Dialog ---
    if (withdrawalForEditTrx != null) {
        val target = withdrawalForEditTrx!!
        BottomSlideAlertDialog(
            onDismissRequest = { withdrawalForEditTrx = null },
            icon = {
                Icon(Icons.Default.Edit, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(28.dp))
            },
            title = {
                Text("TrxID সম্পাদন / পরিবর্তন", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${target.solverName} (উইথড্র আইডি: ${target.id})-এর উইথড্রয়ালের TrxID আপডেট করুন:",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )
                    OutlinedTextField(
                        value = editTrxInput,
                        onValueChange = { editTrxInput = it },
                        label = { Text("নতুন TrxID / রেফারেন্স") },
                        placeholder = { Text("যেমন: 9TRX72834") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTrx = editTrxInput.trim()
                        if (newTrx.isNotBlank()) {
                            viewModel?.adminUpdateWithdrawalTrxId(target.id, newTrx)
                            withdrawalForEditTrx = null
                            editTrxInput = ""
                        } else {
                            Toast.makeText(context, "অনুগ্রহ করে TrxID লিখুন", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("সংরক্ষণ করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { withdrawalForEditTrx = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // --- Full Details Dialog ---
    if (selectedWithdrawalForDetail != null) {
        val item = selectedWithdrawalForDetail!!
        val matchedUser = allUsers.find { it.id == item.solverId }
        BottomSlideAlertDialog(
            onDismissRequest = { selectedWithdrawalForDetail = null },
            icon = {
                Icon(Icons.Default.Info, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(28.dp))
            },
            title = {
                Text("উইথড্রয়াল বিস্তারিত বিবরণ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ID row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("উইথড্রয়াল ID", fontSize = 10.sp, color = SomadhanTextHint)
                            Text(item.id, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Withdrawal ID", item.id))
                                Toast.makeText(context, "উইথড্রয়াল ID কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = SomadhanOrange)
                        }
                    }

                    // UID row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ব্যবহারকারী UID", fontSize = 10.sp, color = SomadhanTextHint)
                            Text(matchedUser?.displayUid?.takeIf { it.isNotBlank() } ?: item.solverId, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanOrange)
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("User UID", matchedUser?.displayUid?.takeIf { it.isNotBlank() } ?: item.solverId))
                                Toast.makeText(context, "UID কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = SomadhanOrange)
                        }
                    }

                    Divider(color = SomadhanDivider, thickness = 0.5.dp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("নাম ও রোল:", fontSize = 12.sp, color = SomadhanTextSecondary)
                        Text("${item.solverName} (${matchedUser?.role ?: "USER"})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                    }

                    if (!matchedUser?.phone.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("মোবাইল নম্বর:", fontSize = 12.sp, color = SomadhanTextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(matchedUser?.phone?.let { Formatters.toLocalDisplayFormat(it) } ?: "", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SomadhanTextPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("User Phone", matchedUser?.phone ?: ""))
                                        Toast.makeText(context, "ফোন নম্বর কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = SomadhanOrange)
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("পরিমাণ:", fontSize = 12.sp, color = SomadhanTextSecondary)
                        Text("৳ ${DistanceUtil.toBengaliDigits(item.amount.toInt().toString())}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanOrange)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("মাধ্যম:", fontSize = 12.sp, color = SomadhanTextSecondary)
                        Text(item.method, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("অ্যাকাউন্ট নম্বর:", fontSize = 12.sp, color = SomadhanTextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.accountNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Account Number", item.accountNumber))
                                    Toast.makeText(context, "অ্যাকাউন্ট নম্বর কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = SomadhanOrange)
                            }
                        }
                    }

                    if (item.bankName != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ব্যাংক:", fontSize = 12.sp, color = SomadhanTextSecondary)
                            Text(item.bankName, fontSize = 12.sp, color = SomadhanTextPrimary)
                        }
                        if (item.branchName != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("শাখা:", fontSize = 12.sp, color = SomadhanTextSecondary)
                                Text(item.branchName, fontSize = 12.sp, color = SomadhanTextPrimary)
                            }
                        }
                        if (item.accountHolderName != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("হোল্ডারের নাম:", fontSize = 12.sp, color = SomadhanTextSecondary)
                                Text(item.accountHolderName, fontSize = 12.sp, color = SomadhanTextPrimary)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("স্ট্যাটাস:", fontSize = 12.sp, color = SomadhanTextSecondary)
                        StatusBadge(status = item.status)
                    }

                    if (!item.trxId.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TrxID:", fontSize = 12.sp, color = SomadhanTextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.trxId, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanSuccess)
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("TrxID", item.trxId))
                                        Toast.makeText(context, "TrxID কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = SomadhanSuccess)
                                }
                            }
                        }
                    }

                    if (!item.rejectionReason.isNullOrBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("বাতিলের কারণ:", fontSize = 12.sp, color = SomadhanTextSecondary)
                            Text(item.rejectionReason, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanError)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("আবেদনের সময়:", fontSize = 12.sp, color = SomadhanTextSecondary)
                        Text(Formatters.formatDateTimeBengali(item.createdAt), fontSize = 11.sp, color = SomadhanTextHint)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedWithdrawalForDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ঠিক আছে", color = Color.White)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 1. Header Card with Title, CSV Export, & Select Mode Toggle
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "উইথড্রয়াল অনুরোধসমূহ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // CSV Export Button
                        OutlinedButton(
                            onClick = {
                                val headers = listOf(
                                    "id",
                                    "solverId",
                                    "solverName",
                                    "amount",
                                    "method",
                                    "accountNumber",
                                    "status",
                                    "trxId",
                                    "createdAt"
                                )
                                val rows = filteredWithdrawals.map { item ->
                                    listOf(
                                        item.id,
                                        item.solverId,
                                        item.solverName,
                                        item.amount.toInt().toString(),
                                        item.method,
                                        item.accountNumber,
                                        item.status,
                                        item.trxId ?: "",
                                        Formatters.formatDateTimeBengali(item.createdAt)
                                    )
                                }
                                val fileName = "somadhan_withdrawals_${System.currentTimeMillis()}.csv"
                                val success = CsvExportUtil.exportToCsv(context, fileName, headers, rows)
                                if (success) {
                                    Toast.makeText(context, "CSV ফাইল সেভ হয়েছে", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "CSV ফাইল সেভ করা যায়নি", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanOrange),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanOrange),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV এক্সপোর্ট", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Select Mode Toggle Button
                        Button(
                            onClick = {
                                isSelectMode = !isSelectMode
                                if (!isSelectMode) {
                                    selectedWithdrawalIds = emptySet()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelectMode) SomadhanOrange else SomadhanCardBg
                            ),
                            border = if (!isSelectMode) androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider) else null,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Assignment,
                                contentDescription = null,
                                tint = if (isSelectMode) Color.White else SomadhanTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSelectMode) "সিলেক্ট চালু ✓" else "সিলেক্ট মোড",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelectMode) Color.White else SomadhanTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Box with Withdraw ID, UID, Name, Account, TrxID support
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("উইথড্র আইডি, সলভারের নাম, UID, অ্যাকাউন্ট বা TrxID দিয়ে সার্চ করুন...", fontSize = 12.sp, color = SomadhanTextHint) },
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

                // Status Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterChips = listOf(
                        "ALL" to "সকল (${DistanceUtil.toBengaliDigits(withdrawals.size.toString())})",
                        "PENDING" to "অপেক্ষমাণ (${DistanceUtil.toBengaliDigits(pendingCount.toString())})",
                        "COMPLETED" to "সম্পন্ন (${DistanceUtil.toBengaliDigits(completedCount.toString())})",
                        "REJECTED" to "বাতিল (${DistanceUtil.toBengaliDigits(rejectedCount.toString())})"
                    )

                    filterChips.forEach { (key, label) ->
                        val isSelected = selectedStatusFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) SomadhanOrange else SomadhanCardBg)
                                .border(1.dp, if (isSelected) SomadhanOrange else SomadhanDivider, RoundedCornerShape(16.dp))
                                .clickable { selectedStatusFilter = key }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else SomadhanTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // ধাপ ৮ — outbox pending sync ইন্ডিকেটর (non-blocking, খালি থাকলে কিছুই দেখায় না)
        OutboxPendingIndicator(
            pendingCount = outboxPendingCount,
            onRetryClick = { viewModel?.retryOutboxSyncNow() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        // 2. Select Mode Action Bar (When Select Mode is Active)
        if (isSelectMode) {
            val visiblePendingList = remember(filteredWithdrawals) {
                filteredWithdrawals.filter { it.status == "PENDING" }
            }
            val allVisiblePendingSelected = visiblePendingList.isNotEmpty() &&
                visiblePendingList.all { it.id in selectedWithdrawalIds }

            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .border(1.dp, SomadhanOrange.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (allVisiblePendingSelected) {
                                val visibleIds = visiblePendingList.map { it.id }.toSet()
                                selectedWithdrawalIds = selectedWithdrawalIds - visibleIds
                            } else {
                                val visibleIds = visiblePendingList.map { it.id }.toSet()
                                selectedWithdrawalIds = selectedWithdrawalIds + visibleIds
                            }
                        }
                    ) {
                        Checkbox(
                            checked = allVisiblePendingSelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    val visibleIds = visiblePendingList.map { it.id }.toSet()
                                    selectedWithdrawalIds = selectedWithdrawalIds + visibleIds
                                } else {
                                    val visibleIds = visiblePendingList.map { it.id }.toSet()
                                    selectedWithdrawalIds = selectedWithdrawalIds - visibleIds
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = SomadhanOrange,
                                uncheckedColor = SomadhanTextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (allVisiblePendingSelected) "সব আনসিলেক্ট" else "সব সিলেক্ট (${DistanceUtil.toBengaliDigits(visiblePendingList.size.toString())}টি)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SomadhanTextPrimary
                        )
                    }

                    Button(
                        onClick = { showBulkApproveDialog = true },
                        enabled = selectedPendingWithdrawals.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SomadhanSuccess,
                            disabledContainerColor = SomadhanSuccess.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "নির্বাচিত (${DistanceUtil.toBengaliDigits(selectedPendingWithdrawals.size.toString())}টি) বাল্ক অ্যাপ্রুভ করুন",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 3. Withdrawal Items List with Option A Pagination
        if (filteredWithdrawals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "অনুসন্ধানের সাথে কোনো উইথড্রয়াল পাওয়া যায়নি" else "কোনো উইথড্রয়াল রিকোয়েস্ট নেই",
                    fontSize = 13.sp,
                    color = SomadhanTextHint
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(paginatedWithdrawals, key = { it.id }) { item ->
                    val isPending = item.status == "PENDING"
                    val isCompleted = item.status == "COMPLETED"
                    val isRejected = item.status == "REJECTED"
                    val isSelected = item.id in selectedWithdrawalIds

                    // Admin Panel Loading fix, সেশন ২.২৭ (Ground Rule ১৯ retrofit, whole-list →
                    // per-item) — আগে একটাই শেয়ার্ড withdrawalsListPulse (ফাংশনের উপরে, value =
                    // paginatedWithdrawals) ছিল বলে যেকোনো একটা উইথড্রয়ালের status আপডেট করলেই
                    // দৃশ্যমান সব কার্ড একসাথে pulse করতো, আর ফিল্টার/pagination পাল্টালেও পুরো
                    // নতুন পাতা pulse করতো। এখন per-item — value = item (পুরো লিস্ট না), তাই
                    // WithdrawalEntity data class-এর equals-ভিত্তিক তুলনায় শুধু যে উইথড্রয়ালের
                    // ডেটা সত্যিই বদলেছে তার কার্ডই pulse করবে। flashOnReentry = false বাধ্যতামূলক
                    // (Ground Rule ১৯) — না হলে স্ক্রল করে নতুন কার্ড প্রথমবার compose হওয়ার মুহূর্তে
                    // সেটাকে ভুলভাবে "re-entry" ধরে নিয়ে অনিচ্ছাকৃত স্ক্রল-pulse দেখাতো; cold-load-এর
                    // re-entry pulse এমনিতেই বাইরের SyncAwareContent (ইনডেক্স ২) সামলায়। sessionKey/
                    // viewModel এখনো পাস করা হচ্ছে (consistency), isManualRefreshing per-item পাস
                    // করা হচ্ছে যাতে pull-to-refresh সম্পন্ন হলে ইচ্ছাকৃতভাবে দৃশ্যমান সব কার্ড একসাথে
                    // pulse করে (এটা bug না, Users/KYC-এর মতোই ইচ্ছাকৃত "রিফ্রেশ সম্পন্ন" ফিডব্যাক)।
                    val withdrawalCardPulse = rememberFieldChangePulse(
                        value = item,
                        isManualRefreshing = isManualRefreshing,
                        sessionKey = "admin_withdrawals_sync",
                        viewModel = viewModel,
                        flashOnReentry = false
                    )

                    // Ground Rule ২১, সেশন ২.১৯.৫ — নতুন request/status-change (উপরের কমেন্ট
                    // দেখো)।
                    val isNewFromRealtime = recentlyChangedWithdrawalIds.contains(item.id)

                    // সেশন ২.৩১ (Ground Rule ২০) — action-pulse (item নিজে বদলালে) অথবা
                    // filter-pulse (ফিল্টার/সার্চ/pagination বদলে দৃশ্যমান সব কার্ড), যেটাই true।
                    PulsingValue(isUpdating = withdrawalCardPulse || isFilterRefreshing || isNewFromRealtime) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SomadhanOrangeLight.copy(alpha = 0.5f) else SomadhanBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) SomadhanOrange else SomadhanDivider,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .then(
                                if (isSelectMode && isPending) {
                                    Modifier.clickable {
                                        selectedWithdrawalIds = if (isSelected) {
                                            selectedWithdrawalIds - item.id
                                        } else {
                                            selectedWithdrawalIds + item.id
                                        }
                                    }
                                } else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelectMode && isPending) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedWithdrawalIds = if (checked) {
                                            selectedWithdrawalIds + item.id
                                        } else {
                                            selectedWithdrawalIds - item.id
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = SomadhanOrange,
                                        uncheckedColor = SomadhanTextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        val matchedUser = allUsers.find { it.id == item.solverId }
                                        val isUserRole = matchedUser?.role == "USER"
                                        val roleLabel = if (isUserRole) "ইউজার" else "সলভার"

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.solverName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanTextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isUserRole) Color(0xFFE8F4FD) else SomadhanOrangeLight)
                                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                            ) {
                                                Text(
                                                    text = roleLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isUserRole) SomadhanInfo else SomadhanOrange
                                                )
                                            }
                                        }

                                        // Withdraw ID display under user's name with copy button
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = "উইথড্র আইডি: ${item.id}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SomadhanOrange
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Withdraw ID", item.id))
                                                    Toast.makeText(context, "উইথড্র আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = "Copy Withdraw ID",
                                                    modifier = Modifier.size(11.dp),
                                                    tint = SomadhanOrange
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "পরিমাণ: ৳ ${DistanceUtil.toBengaliDigits(item.amount.toInt().toString())}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanOrange
                                        )
                                    }
                                    StatusBadge(status = item.status)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "মাধ্যম: ${item.method} (${item.accountNumber})",
                                    fontSize = 12.sp,
                                    color = SomadhanTextSecondary
                                )

                                if (item.bankName != null) {
                                    Text(
                                        text = "ব্যাংক: ${item.bankName}, শাখা: ${item.branchName ?: ""}, নাম: ${item.accountHolderName ?: ""}",
                                        fontSize = 11.sp,
                                        color = SomadhanTextHint
                                    )
                                }

                                if (!item.trxId.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "TrxID: ${item.trxId}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SomadhanSuccess
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("TrxID", item.trxId))
                                                Toast.makeText(context, "TrxID কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Copy TrxID",
                                                modifier = Modifier.size(11.dp),
                                                tint = SomadhanSuccess
                                            )
                                        }
                                    }
                                }

                                if (!item.rejectionReason.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "বাতিলের কারণ: ${item.rejectionReason}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanError
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${Formatters.formatDateTimeBengali(item.createdAt)} (${Formatters.formatTimeAgo(item.createdAt)})",
                                    fontSize = 10.sp,
                                    color = SomadhanTextHint
                                )

                                // Dynamic & Actionable Admin Controls for ALL statuses
                                if (!isSelectMode) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = SomadhanDivider, thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isPending) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Button(
                                                onClick = { singleWithdrawalToApprove = item },
                                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1.2f).height(34.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("অনুমোদন", fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = { singleWithdrawalToReject = item },
                                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1.1f).height(34.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("বাতিল", fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = { selectedWithdrawalForDetail = item },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanOrange),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanOrange),
                                                modifier = Modifier.weight(1f).height(34.dp)
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("বিস্তারিত", fontSize = 11.sp)
                                            }
                                        }
                                    } else if (isCompleted) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    editTrxInput = item.trxId ?: ""
                                                    withdrawalForEditTrx = item
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanSuccess),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanSuccess),
                                                modifier = Modifier.weight(1.3f).height(34.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("TrxID সম্পাদন", fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = { selectedWithdrawalForDetail = item },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanOrange),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanOrange),
                                                modifier = Modifier.weight(1f).height(34.dp)
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("বিস্তারিত", fontSize = 11.sp)
                                            }
                                        }
                                    } else if (isRejected) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SomadhanSuccess,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "অর্থ রিফান্ড সম্পন্ন (চূড়ান্ত স্টেট)",
                                                    fontSize = 11.sp,
                                                    color = SomadhanTextSecondary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = { selectedWithdrawalForDetail = item },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanOrange),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanOrange),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("বিস্তারিত", fontSize = 11.sp)
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

            // Pagination Controls at bottom
            if (filteredWithdrawals.isNotEmpty()) {
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
}
