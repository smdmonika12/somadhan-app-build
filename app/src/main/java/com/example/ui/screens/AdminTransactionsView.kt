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
import androidx.compose.material3.AlertDialog
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
import com.example.data.entity.GatewayPaymentEntity
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

sealed interface AdminTransactionListItem {
    val id: String
    val timestamp: Long

    data class WorkTransaction(val transaction: TransactionEntity) : AdminTransactionListItem {
        override val id: String get() = transaction.id
        override val timestamp: Long get() = transaction.timestamp
    }

    data class WalletRecharge(val payment: GatewayPaymentEntity) : AdminTransactionListItem {
        override val id: String get() = payment.id
        override val timestamp: Long get() = payment.timestamp
    }
}

@Composable
fun AdminTransactionsView(
    transactions: List<TransactionEntity>,
    gatewayPayments: List<GatewayPaymentEntity> = emptyList(),
    allUsers: List<UserEntity> = emptyList(),
    viewModel: com.example.ui.viewmodel.SomadhanViewModel? = null,
    isManualRefreshing: Boolean = false
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val userMap = remember(allUsers) { allUsers.associateBy { it.id } }
    var selectedTimeFilter by remember { mutableStateOf("ALL") } // "ALL", "TODAY", "WEEK", "MONTH"
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "PAYMENT", "REFUND", "WALLET_RECHARGE", "FREE_QUOTA", "EXTRA_BILL"
    val pageSize = 10
    var currentPage by rememberSaveable { mutableIntStateOf(1) }
    var isCleaningDuplicates by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTimeFilter, searchQuery, selectedTypeFilter) {
        currentPage = 1
    }

    // Ground Rule ২০: ফিল্টার/সার্চ/টাইম-রেঞ্জ/পেজ বদলে দৃশ্যমান সব তালিকার কার্ড একসাথে pulse করাবে
    // (Escrow/Withdrawal-এর একই প্যাটার্ন)।
    var isFilterRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(selectedTimeFilter, searchQuery, selectedTypeFilter, currentPage) {
        isFilterRefreshing = true
        try {
            kotlinx.coroutines.delay(350L)
        } finally {
            isFilterRefreshing = false
        }
    }

    // Ground Rule ২১, সেশন ২.২০ — genuine নতুন WorkTransaction/WalletRecharge (Insert-only, শুধু
    // সেই কার্ডটাই pulse করবে)। দুই আলাদা টেবিল বলে দুই আলাদা id-সেট (দেখো
    // SupabaseRealtimeManager.recentlyChangedTransactionIds/recentlyChangedGatewayPaymentIds-এর
    // কমেন্ট)। `viewModel` nullable (কিছু preview/pre-existing কল-সাইটে না দেওয়া হতে পারে) — না
    // থাকলে খালি সেট ধরে নেওয়া হয়, pulse-এর তৃতীয় কারণটা নিষ্ক্রিয় থাকে।
    val recentlyChangedTransactionIds by remember(viewModel) {
        viewModel?.recentlyChangedTransactionIds ?: kotlinx.coroutines.flow.MutableStateFlow(emptySet<String>())
    }.collectAsStateWithLifecycle()
    val recentlyChangedGatewayPaymentIds by remember(viewModel) {
        viewModel?.recentlyChangedGatewayPaymentIds ?: kotlinx.coroutines.flow.MutableStateFlow(emptySet<String>())
    }.collectAsStateWithLifecycle()

    val now = remember { System.currentTimeMillis() }
    val startOfToday = remember(now) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val startOfWeek = remember(now) { now - (7L * 24 * 60 * 60 * 1000) }
    val startOfMonth = remember(now) { now - (30L * 24 * 60 * 60 * 1000) }

    val filteredListItems = remember(transactions, gatewayPayments, selectedTimeFilter, searchQuery, selectedTypeFilter, userMap) {
        val q = searchQuery.trim().lowercase()

        val matchingTransactions: List<AdminTransactionListItem.WorkTransaction> = if (selectedTypeFilter == "WALLET_RECHARGE") {
            emptyList()
        } else {
            transactions.filter { trx ->
                val isDepositOrRecharge = TransactionHelper.isDepositTrx(trx)
                val isRefund = TransactionHelper.isRefundTrx(trx)

                val matchesTime = when (selectedTimeFilter) {
                    "TODAY" -> trx.timestamp >= startOfToday
                    "WEEK" -> trx.timestamp >= startOfWeek
                    "MONTH" -> trx.timestamp >= startOfMonth
                    else -> true
                }

                val matchesType = when (selectedTypeFilter) {
                    "PAYMENT" -> !isRefund && !isDepositOrRecharge
                    "REFUND" -> isRefund
                    "FREE_QUOTA" -> !isRefund && !isDepositOrRecharge && trx.wasFreeQuotaJob
                    "EXTRA_BILL" -> !isRefund && !isDepositOrRecharge && (trx.extraCommissionAmount > 0.0 || trx.extraAmount > 0.0)
                    else -> true
                }

                val matchesQuery = q.isEmpty() ||
                        trx.id.lowercase().contains(q) ||
                        trx.problemTitle.lowercase().contains(q) ||
                        trx.problemId.lowercase().contains(q) ||
                        trx.userId.lowercase().contains(q) ||
                        trx.solverId.lowercase().contains(q) ||
                        trx.escrowId.lowercase().contains(q)

                matchesTime && matchesType && matchesQuery
            }.map { AdminTransactionListItem.WorkTransaction(it) }
        }

        val matchingGatewayPayments: List<AdminTransactionListItem.WalletRecharge> = if (selectedTypeFilter != "ALL" && selectedTypeFilter != "WALLET_RECHARGE") {
            emptyList()
        } else {
            gatewayPayments.filter { payment ->
                val matchesTime = when (selectedTimeFilter) {
                    "TODAY" -> payment.timestamp >= startOfToday
                    "WEEK" -> payment.timestamp >= startOfWeek
                    "MONTH" -> payment.timestamp >= startOfMonth
                    else -> true
                }

                val userName = userMap[payment.userId]?.name.orEmpty().lowercase()
                val userPhone = userMap[payment.userId]?.phone.orEmpty().lowercase()

                val matchesQuery = q.isEmpty() ||
                        payment.id.lowercase().contains(q) ||
                        payment.gatewayTrxId.lowercase().contains(q) ||
                        payment.gateway.lowercase().contains(q) ||
                        payment.userId.lowercase().contains(q) ||
                        payment.status.lowercase().contains(q) ||
                        payment.problemId.lowercase().contains(q) ||
                        payment.problemTitle.lowercase().contains(q) ||
                        userName.contains(q) ||
                        userPhone.contains(q)

                matchesTime && matchesQuery
            }.map { AdminTransactionListItem.WalletRecharge(it) }
        }

        (matchingTransactions + matchingGatewayPayments).sortedByDescending { it.timestamp }
    }

    val totalPages = maxOf(1, (filteredListItems.size + pageSize - 1) / pageSize)
    val safePage = currentPage.coerceIn(1, totalPages)
    val paginatedList = remember(filteredListItems, safePage, pageSize) {
        val fromIndex = (safePage - 1) * pageSize
        if (fromIndex >= filteredListItems.size) {
            emptyList()
        } else {
            filteredListItems.subList(fromIndex, minOf(fromIndex + pageSize, filteredListItems.size))
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(safePage) {
        listState.scrollToItem(0)
    }

    val filteredTransactions = remember(filteredListItems) {
        filteredListItems.mapNotNull { (it as? AdminTransactionListItem.WorkTransaction)?.transaction }
    }
    val filteredRecharges = remember(filteredListItems) {
        filteredListItems.mapNotNull { (it as? AdminTransactionListItem.WalletRecharge)?.payment }
    }

    val totalGrossWork = remember(filteredTransactions) { filteredTransactions.sumOf { it.grossAmount } }
    val totalRechargeAmount = remember(filteredRecharges) {
        filteredRecharges.filter { it.status.uppercase() == "SUCCESS" }.sumOf { it.amount }
    }
    val totalGross = remember(totalGrossWork, totalRechargeAmount) { totalGrossWork + totalRechargeAmount }

    val totalCommission = remember(filteredTransactions) { filteredTransactions.sumOf { it.commissionAmount } }
    val totalBaseCommission = remember(filteredTransactions) {
        filteredTransactions.sumOf {
            if (it.baseCommissionAmount > 0.0) it.baseCommissionAmount
            else if (!it.wasFreeQuotaJob) (it.grossAmount * (if (it.commissionPercent > 0) it.commissionPercent else 10.0) / 100.0)
            else 0.0
        }
    }
    val totalExtraCommission = remember(filteredTransactions) { filteredTransactions.sumOf { it.extraCommissionAmount } }
    val freeQuotaJobsCount = remember(filteredTransactions) { filteredTransactions.count { it.wasFreeQuotaJob } }
    val waivedCommissionTotal = remember(filteredTransactions) {
        filteredTransactions.filter { it.wasFreeQuotaJob }.sumOf {
            if (it.baseCommissionAmount > 0.0) it.baseCommissionAmount
            else (it.grossAmount * (if (it.commissionPercent > 0) it.commissionPercent else 10.0) / 100.0)
        }
    }
    val totalNet = remember(filteredTransactions) { filteredTransactions.sumOf { it.netAmount } }
    val totalTrxCount = filteredListItems.size

    // 1B. Audit & Adjustment Report Data for Historical Extra Bill Transactions
    val auditItems = remember(transactions) {
        transactions.filter { it.extraAmount > 0.0 && it.extraCommissionAmount > 0.0 }.mapNotNull { tx ->
            val normalRate = if (tx.commissionPercent > 0.0) tx.commissionPercent else 10.0
            val normalExtraComm = tx.extraAmount * (normalRate / 100.0)
            val expectedDiscountedExtraComm = normalExtraComm * 0.50 // 50% discount on normal extra commission
            val excess = tx.extraCommissionAmount - expectedDiscountedExtraComm
            if (excess > 0.5) { // excess deduction detected
                Triple(tx, expectedDiscountedExtraComm, excess)
            } else null
        }
    }
    var showAuditDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("admin_transactions_view"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // 1. Header & Breakdown Summary Dashboard Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SomadhanSuccessLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = SomadhanSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "কমিশন ও রেভিনিউ ড্যাশবোর্ড",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                                Text(
                                    text = "বেস কমিশন, অতিরিক্ত বিল কমিশন ও ফ্রি-কোটার বিস্তারিত বিবরণী",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (viewModel != null) {
                                OutlinedButton(
                                    onClick = {
                                        if (!isCleaningDuplicates) {
                                            isCleaningDuplicates = true
                                            viewModel.adminCleanupDuplicateRefunds {
                                                isCleaningDuplicates = false
                                            }
                                        }
                                    },
                                    enabled = !isCleaningDuplicates,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    border = BorderStroke(1.dp, SomadhanOrange)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "ডুপ্লিকেট ক্লিনআপ",
                                        modifier = Modifier.size(15.dp),
                                        tint = SomadhanOrange
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isCleaningDuplicates) "ক্লিন হচ্ছে..." else "ডুপ্লিকেট রিফান্ড ফিক্স",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrange
                                    )
                                }
                            }

                            // Export CSV Button
                            Button(
                                onClick = {
                                    CsvExportUtil.exportToCsv(
                                        context = context,
                                        fileName = "commission_report_${System.currentTimeMillis()}",
                                        headers = listOf(
                                            "Transaction ID",
                                            "Escrow / TrxID",
                                            "Problem ID",
                                            "Title / Method",
                                            "Type",
                                            "Customer ID",
                                            "Solver ID",
                                            "Gross Amount (BDT)",
                                            "Total Commission (BDT)",
                                            "Base Commission (BDT)",
                                            "Extra Commission (BDT)",
                                            "Free Quota Applied",
                                            "Net Payout/Refund/Deposit (BDT)",
                                            "Date"
                                        ),
                                        rows = filteredListItems.map { item ->
                                            when (item) {
                                                 is AdminTransactionListItem.WorkTransaction -> {
                                                    val tx = item.transaction
                                                    val isDepositOrRecharge = TransactionHelper.isDepositTrx(tx)
                                                    val isRef = TransactionHelper.isRefundTrx(tx)
                                                    val refundMeta = if (isRef) TransactionHelper.getRefundMeta(tx) else null

                                                    val effectiveEscrow = when {
                                                        tx.escrowId.isNotBlank() -> tx.escrowId
                                                        tx.problemId.isNotBlank() -> "ESC_${tx.problemId.take(8).uppercase()}"
                                                        else -> "ESC_${tx.id.take(8).uppercase()}"
                                                    }
                                                    val txType = if (isDepositOrRecharge) {
                                                        "ওয়ালেট রিচার্জ (Wallet Recharge)"
                                                    } else if (isRef) {
                                                        "${refundMeta?.labelBn ?: "রিফান্ড"} (${refundMeta?.labelEn ?: "User Refund"})"
                                                    } else if (tx.wasFreeQuotaJob) {
                                                        "ফ্রি কোটা"
                                                    } else if (tx.extraCommissionAmount > 0.0) {
                                                        "অতিরিক্ত বিল সহ"
                                                    } else {
                                                        "সাধারণ পেআউট"
                                                    }
                                                    listOf(
                                                        tx.id,
                                                        effectiveEscrow,
                                                        tx.problemId,
                                                        TransactionHelper.getDisplayTitle(tx),
                                                        txType,
                                                        tx.userId,
                                                        if (isRef && tx.solverId.isBlank()) "N/A" else tx.solverId,
                                                        tx.grossAmount.toString(),
                                                        (if (isRef || isDepositOrRecharge) 0.0 else tx.commissionAmount).toString(),
                                                        (if (isRef || isDepositOrRecharge) 0.0 else if (tx.baseCommissionAmount > 0.0) tx.baseCommissionAmount else if (!tx.wasFreeQuotaJob) tx.commissionAmount else 0.0).toString(),
                                                        (if (isRef || isDepositOrRecharge) 0.0 else tx.extraCommissionAmount).toString(),
                                                        if (tx.wasFreeQuotaJob && !isRef && !isDepositOrRecharge) "YES (Free Quota)" else "NO",
                                                        tx.netAmount.toString(),
                                                        Formatters.formatDateTimeBengali(tx.timestamp)
                                                    )
                                                }
                                                is AdminTransactionListItem.WalletRecharge -> {
                                                    val payment = item.payment
                                                    listOf(
                                                        payment.id,
                                                        payment.gatewayTrxId.ifBlank { "N/A" },
                                                        payment.problemId.ifBlank { "N/A" },
                                                        "ওয়ালেট রিচার্জ (${payment.gateway.uppercase()})",
                                                        "ওয়ালেট রিচার্জ (Gateway Deposit)",
                                                        payment.userId,
                                                        "N/A",
                                                        payment.amount.toString(),
                                                        "0.0",
                                                        "0.0",
                                                        "0.0",
                                                        "NO",
                                                        payment.amount.toString(),
                                                        Formatters.formatDateTimeBengali(payment.timestamp)
                                                    )
                                                }
                                            }
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "CSV এক্সপোর্ট", modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = SomadhanDivider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Time Filters Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "সব সময়",
                            "TODAY" to "আজ",
                            "WEEK" to "এই সপ্তাহ",
                            "MONTH" to "এই মাস"
                        ).forEach { (key, label) ->
                            val isSelected = selectedTimeFilter == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) SomadhanOrange else SomadhanBg)
                                    .clickable { selectedTimeFilter = key }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Summary Stats: Primary Row (Gross & Commission)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Gross Volume Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "মোট গ্রস লেনদেন",
                                    fontSize = 10.sp,
                                    color = SomadhanTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "৳ ${DistanceUtil.toBengaliDigits(totalGross)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                            }
                        }

                        // Commission Revenue Card (Highlighted)
                        Card(
                            modifier = Modifier.weight(1.2f),
                            colors = CardDefaults.cardColors(containerColor = SomadhanSuccessLight.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, SomadhanSuccess.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(SomadhanSuccess)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "মোট অর্জিত কমিশন",
                                        fontSize = 10.sp,
                                        color = SomadhanSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "৳ ${DistanceUtil.toBengaliDigits(totalCommission)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanSuccess
                                )
                            }
                        }

                        // Total Transactions Count
                        Card(
                            modifier = Modifier.weight(0.9f),
                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "মোট লেনদেন",
                                    fontSize = 10.sp,
                                    color = SomadhanTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(totalTrxCount.toString())} টি",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Breakdown Row: Base Commission vs Extra Bill Commission
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "মূল বেস কমিশন",
                                    fontSize = 10.sp,
                                    color = SomadhanTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "৳ ${DistanceUtil.toBengaliDigits(totalBaseCommission)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "অতিরিক্ত বিল কমিশন",
                                    fontSize = 10.sp,
                                    color = SomadhanTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "৳ ${DistanceUtil.toBengaliDigits(totalExtraCommission)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Free-Quota Card (Lost / Waived Commission Info)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🎁", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "ফ্রি-কোটা সুবিধা প্রাপ্ত কাজ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                    Text(
                                        text = "${DistanceUtil.toBengaliDigits(freeQuotaJobsCount.toString())} টি কাজে ০% কমিশন প্রয়োগ হয়েছে",
                                        fontSize = 10.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "মওকুফকৃত কমিশন",
                                    fontSize = 9.sp,
                                    color = SomadhanTextSecondary
                                )
                                Text(
                                    text = "৳ ${DistanceUtil.toBengaliDigits(waivedCommissionTotal)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "সলভারদের মোট নেট পেআউট: ৳ ${DistanceUtil.toBengaliDigits(totalNet)}",
                        fontSize = 11.sp,
                        color = SomadhanTextHint
                    )
                }
            }
        }

        // 1B. Audit & Adjustment Report Card for Historical Extra Bill Transactions
        if (auditItems.isNotEmpty()) {
            item {
                val totalExcess = auditItems.sumOf { it.third }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_extra_commission_audit_card"),
                    colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(text = "⚠️", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "অতিরিক্ত বিল কমিশন অডিট রিপোর্ট (${DistanceUtil.toBengaliDigits(auditItems.size.toString())}টি লেনদেন)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SomadhanTextPrimary
                                    )
                                    Text(
                                        text = "চিহ্নিত মোট অতিরিক্ত কর্তন: ৳ ${DistanceUtil.toBengaliDigits(totalExcess)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanOrange
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    CsvExportUtil.exportToCsv(
                                        context = context,
                                        fileName = "extra_commission_audit_adjustment_${System.currentTimeMillis()}",
                                        headers = listOf(
                                            "Transaction ID",
                                            "Problem Title",
                                            "Solver ID",
                                            "Customer ID",
                                            "Extra Bill Amount (BDT)",
                                            "Charged Extra Commission (BDT)",
                                            "Correct Expected Commission (BDT)",
                                            "Excess Deducted Amount (BDT)",
                                            "Date"
                                        ),
                                        rows = auditItems.map { (tx, expected, excess) ->
                                            listOf(
                                                tx.id,
                                                tx.problemTitle,
                                                tx.solverId,
                                                tx.userId,
                                                tx.extraAmount.toString(),
                                                tx.extraCommissionAmount.toString(),
                                                expected.toString(),
                                                excess.toString(),
                                                Formatters.formatDateTimeBengali(tx.timestamp)
                                            )
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "CSV", modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("অডিট CSV", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "পূর্বে অতিরিক্ত বিলের উপর সরাসরি ৫০% রেট কেটে নেওয়া কিছু লেনদেন পাওয়া গেছে। অ্যাডমিন ম্যানুয়ালি পর্যালোচনা করে অ্যাডজাস্টমেন্ট বা রিফান্ড সিদ্ধান্ত নিতে পারেন (ব্যালেন্স নিজে থেকে পরিবর্তন করা হয়নি)।",
                            fontSize = 11.sp,
                            color = SomadhanTextSecondary,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showAuditDetails = !showAuditDetails },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (showAuditDetails) "বিবরণ লুকান ▲" else "বিস্তারিত লেনদেনের তালিকা দেখুন ▼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrange
                            )
                        }

                        if (showAuditDetails) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                auditItems.forEach { (tx, expected, excess) ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SomadhanBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = tx.problemTitle.ifBlank { tx.id },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SomadhanTextPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = "+৳ ${DistanceUtil.toBengaliDigits(excess)} বেশি কাটা",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SomadhanError
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "সলভার ID: ${tx.solverId} | অতিরিক্ত বিল: ৳${DistanceUtil.toBengaliDigits(tx.extraAmount)} | কাটা হয়েছিল: ৳${DistanceUtil.toBengaliDigits(tx.extraCommissionAmount)} (সঠিক: ৳${DistanceUtil.toBengaliDigits(expected)})",
                                                fontSize = 10.sp,
                                                color = SomadhanTextSecondary
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

        // 2. Search Box & Type Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("লেনদেন আইডি, এসক্রো আইডি, শিরোনাম বা গ্রাহক/সলভার খুঁজুন...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "সার্চ", tint = SomadhanTextHint, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "মুছুন", tint = SomadhanTextHint, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        unfocusedBorderColor = SomadhanBorder
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Type Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterChips = listOf(
                        "ALL" to "সব লেনদেন",
                        "PAYMENT" to "💼 সলভার পেআউট",
                        "REFUND" to "🔄 ইউজার রিফান্ড",
                        "WALLET_RECHARGE" to "💳 ওয়ালেট রিচার্জ",
                        "FREE_QUOTA" to "🎁 ফ্রি কোটা",
                        "EXTRA_BILL" to "⚡ অতিরিক্ত বিল"
                    )
                    items(filterChips) { (key, label) ->
                        val isSelected = selectedTypeFilter == key
                        val chipBg = when {
                            isSelected && key == "REFUND" -> Color(0xFF2563EB)
                            isSelected && key == "WALLET_RECHARGE" -> Color(0xFF0D9488)
                            isSelected -> SomadhanOrange
                            else -> SomadhanCardBg
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(chipBg)
                                .border(1.dp, if (isSelected) Color.Transparent else SomadhanBorder, RoundedCornerShape(16.dp))
                                .clickable { selectedTypeFilter = key }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else SomadhanTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 3. Transactions List
        if (filteredListItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = SomadhanTextHint,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "উক্ত ফিল্টারে কোনো লেনদেন পাওয়া যায়নি",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = SomadhanTextSecondary
                        )
                    }
                }
            }
        } else {
            items(paginatedList, key = { it.id }) { item ->
                when (item) {
                    is AdminTransactionListItem.WorkTransaction -> {
                        val trx = item.transaction
                        val isDepositOrRecharge = TransactionHelper.isDepositTrx(trx)
                        val isRefund = TransactionHelper.isRefundTrx(trx)
                        val refundMeta = if (isRefund) TransactionHelper.getRefundMeta(trx) else null

                        val effectiveEscrowId = when {
                            trx.escrowId.isNotBlank() -> trx.escrowId
                            trx.problemId.isNotBlank() -> "ESC_${trx.problemId.take(8).uppercase()}"
                            else -> "ESC_${trx.id.take(8).uppercase()}"
                        }
                        val baseAmt = if (trx.baseCommissionAmount > 0.0) trx.baseCommissionAmount
                        else if (!trx.wasFreeQuotaJob) trx.commissionAmount else 0.0
                        val extraAmt = trx.extraCommissionAmount

                        val refundCardBg = when (refundMeta?.category) {
                            RefundCategory.SPLIT_REFUND -> Color(0xFFFAF5FF)
                            RefundCategory.DISPUTE_REFUND -> Color(0xFFFEF2F2)
                            else -> Color(0xFFF8FAFC)
                        }
                        val refundCardBorder = when (refundMeta?.category) {
                            RefundCategory.SPLIT_REFUND -> Color(0xFFE9D5FF)
                            RefundCategory.DISPUTE_REFUND -> Color(0xFFFECACA)
                            else -> Color(0xFFBFDBFE)
                        }
                        val refundColor = when (refundMeta?.category) {
                            RefundCategory.SPLIT_REFUND -> Color(0xFF7E22CE)
                            RefundCategory.DISPUTE_REFUND -> Color(0xFFB91C1C)
                            else -> Color(0xFF1D4ED8)
                        }

                        val cardBg = when {
                            isRefund -> refundCardBg
                            isDepositOrRecharge -> Color(0xFFF0FDFA)
                            else -> SomadhanCardBg
                        }
                        val cardBorder = when {
                            isRefund -> refundCardBorder
                            isDepositOrRecharge -> Color(0xFF99F6E4)
                            else -> SomadhanBorder
                        }

                        // Ground Rule ১৮/২০/২১ — pull-to-refresh/re-entry (rememberFieldChangePulse),
                        // ফিল্টার-বদল (isFilterRefreshing), আর genuine নতুন Insert
                        // (isNewFromRealtime) — তিনটা স্বাধীন কারণেই শুধু এই কার্ডটাই pulse করবে।
                        val trxCardPulse = rememberFieldChangePulse(
                            value = trx,
                            isManualRefreshing = isManualRefreshing,
                            sessionKey = "admin_transactions_sync",
                            viewModel = viewModel,
                            flashOnReentry = false
                        )
                        val isNewFromRealtime = recentlyChangedTransactionIds.contains(trx.id)
                        PulsingValue(isUpdating = trxCardPulse || isFilterRefreshing || isNewFromRealtime) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_trx_card_${trx.id}"),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Header row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = TransactionHelper.getDisplayTitle(trx),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary
                                        )
                                        if (trx.problemId.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    clipboardManager.setText(AnnotatedString(trx.problemId))
                                                    Toast.makeText(context, "পোস্ট আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text(
                                                    text = "পোস্ট আইডি: #${trx.problemId.take(8).uppercase()}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SomadhanOrange
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "কপি",
                                                    tint = SomadhanOrange,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // Badges
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isDepositOrRecharge) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFCCFBF1))
                                                        .border(0.5.dp, Color(0xFF99F6E4), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "💳 ওয়ালেট রিচার্জ (Wallet Recharge)",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F766E)
                                                    )
                                                }
                                            } else if (isRefund && refundMeta != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(refundCardBg)
                                                        .border(0.5.dp, refundCardBorder, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = refundMeta.badgeBn,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = refundColor
                                                    )
                                                }
                                            } else {
                                                if (trx.wasFreeQuotaJob) {
                                                    Box(
                                                        modifier = Modifier
                                                           .clip(RoundedCornerShape(4.dp))
                                                            .background(SomadhanSuccessLight)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "🎁 ফ্রি কোটা (০% কমিশন)",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SomadhanSuccess
                                                        )
                                                    }
                                                } else if (extraAmt > 0.0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(SomadhanOrangeLight)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "⚡ অতিরিক্ত বিল কমিশন সহ",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SomadhanOrange
                                                        )
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFF3F4F6))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "💼 সলভার পেআউট",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = SomadhanTextSecondary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = Formatters.formatDateTimeBengali(trx.timestamp),
                                        fontSize = 10.sp,
                                        color = SomadhanTextHint
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = SomadhanDivider, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Amount breakdown
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.1f)) {
                                        if (isDepositOrRecharge) {
                                            Text(
                                                text = "ইউজার (অ্যাকাউন্ট): ${trx.userId}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SomadhanTextPrimary
                                            )
                                            Text(
                                                text = "ক্যাটাগরি: ওয়ালেট রিচার্জ / টপ-আপ",
                                                fontSize = 11.sp,
                                                color = SomadhanTextSecondary
                                            )
                                            Text(
                                                text = "ধরণ: ব্যালেন্স ডিপোজিট",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0F766E)
                                            )
                                        } else if (isRefund) {
                                            Text(
                                                text = "গ্রাহক (প্রাপক): ${trx.userId}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SomadhanTextPrimary
                                            )
                                            Text(
                                                text = "সমাধানকারী: ${if (trx.solverId.isNotBlank()) trx.solverId else "প্রযোজ্য নয়"}",
                                                fontSize = 11.sp,
                                                color = SomadhanTextSecondary
                                            )
                                            Text(
                                                text = "ধরণ: ${refundMeta?.labelBn ?: "ইউজার রিফান্ড"} (${refundMeta?.labelEn ?: "User Refund"})",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = refundColor
                                            )
                                        } else {
                                            Text(
                                                text = "গ্রাহক (প্রদানকারী): ${trx.userId}",
                                                fontSize = 11.sp,
                                                color = SomadhanTextSecondary
                                            )
                                            Text(
                                                text = "সমাধানকারী (প্রাপক): ${trx.solverId}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SomadhanTextPrimary
                                            )
                                            val badgeDesc = if (trx.wasFreeQuotaJob) "ফ্রি-কোটা সুবিধা" else if (trx.extraCommissionAmount > 0.0) "অতিরিক্ত বিল কমিশন সহ" else "সাধারণ পেআউট"
                                            Text(
                                                text = "ধরণ: $badgeDesc",
                                                fontSize = 10.sp,
                                                color = SomadhanTextHint
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(0.9f)) {
                                        if (isDepositOrRecharge) {
                                            Text(
                                                text = "রিচার্জ অর্থ: ৳ ${DistanceUtil.toBengaliDigits(trx.grossAmount)}",
                                                fontSize = 12.sp,
                                                color = SomadhanTextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "কমিশন: ৳ ০ (প্রযোজ্য নয়)",
                                                fontSize = 11.sp,
                                                color = SomadhanTextHint
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "জমা (Wallet): ৳ ${DistanceUtil.toBengaliDigits(trx.netAmount)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F766E)
                                            )
                                        } else if (isRefund) {
                                            Text(
                                                text = "রিফান্ড অর্থ: ৳ ${DistanceUtil.toBengaliDigits(trx.grossAmount)}",
                                                fontSize = 12.sp,
                                                color = SomadhanTextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "কমিশন: ৳ ০ (প্রযোজ্য নয়)",
                                                fontSize = 11.sp,
                                                color = SomadhanTextHint
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${refundMeta?.labelBn ?: "ইউজার রিফান্ড"} (Net): ৳ ${DistanceUtil.toBengaliDigits(trx.netAmount)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = refundColor
                                            )
                                        } else {
                                            Text(
                                                text = "গ্রস বিল: ৳ ${DistanceUtil.toBengaliDigits(trx.grossAmount)}",
                                                fontSize = 12.sp,
                                                color = SomadhanTextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))

                                            // Base & Extra Breakdown if extra exists
                                            if (extraAmt > 0.0) {
                                                Text(
                                                    text = "বেস: ৳ ${DistanceUtil.toBengaliDigits(baseAmt)} | অতিরিক্ত: ৳ ${DistanceUtil.toBengaliDigits(extraAmt)}",
                                                    fontSize = 10.sp,
                                                    color = SomadhanTextHint
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }

                                            // Highlighted Total Commission Amount
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "কমিশন (${DistanceUtil.toBengaliDigits(trx.commissionPercent)}%): ",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (trx.wasFreeQuotaJob && trx.commissionAmount == 0.0) SomadhanTextHint else SomadhanSuccess
                                                )
                                                Text(
                                                    text = "৳ ${DistanceUtil.toBengaliDigits(trx.commissionAmount)}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (trx.wasFreeQuotaJob && trx.commissionAmount == 0.0) SomadhanTextHint else SomadhanSuccess
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "সলভার পেআউট (Net): ৳ ${DistanceUtil.toBengaliDigits(trx.netAmount)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanOrange
                                            )
                                        }
                                    }
                                }

                                if (trx.problemId.isNotBlank()) {
                                    val linkedGatewayPayments = gatewayPayments.filter { it.problemId == trx.problemId }
                                    if (linkedGatewayPayments.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(SomadhanBg)
                                                .border(0.5.dp, SomadhanBorder, RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = "সংযুক্ত গেটওয়ে পেমেন্ট (${DistanceUtil.toBengaliDigits(linkedGatewayPayments.size.toString())}টি):",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanTextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            linkedGatewayPayments.forEach { gw ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                clipboardManager.setText(AnnotatedString(gw.gatewayTrxId))
                                                                Toast.makeText(context, "TrxID কপি করা হয়েছে: ${gw.gatewayTrxId}", Toast.LENGTH_SHORT).show()
                                                            }
                                                    ) {
                                                        Text(
                                                            text = "${gw.gateway.uppercase()}: ${gw.gatewayTrxId}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = SomadhanOrange,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(10.dp))
                                                    }
                                                    Text(
                                                        text = "৳ ${DistanceUtil.toBengaliDigits(gw.amount)} (${if (gw.purpose == "EXTRA_BILL") "অতিরিক্ত বিল" else "পেমেন্ট"})",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SomadhanSuccess
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Escrow ID and Full Transaction ID with Click to Copy
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!isDepositOrRecharge) {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFFEF3C7))
                                                .border(0.5.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    clipboardManager.setText(AnnotatedString(effectiveEscrowId))
                                                    Toast.makeText(context, "এসক্রো আইডি কপি করা হয়েছে ($effectiveEscrowId)", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 7.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "এসক্রো আইডি",
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "এসক্রো আইডি: $effectiveEscrowId",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "কপি করুন",
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFCCFBF1))
                                                .border(0.5.dp, Color(0xFF99F6E4), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 7.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalanceWallet,
                                                contentDescription = "ওয়ালেট",
                                                tint = Color(0xFF0F766E),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "ওয়ালেট ব্যালেন্স",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F766E)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            clipboardManager.setText(AnnotatedString(trx.id))
                                            val msg = if (isDepositOrRecharge) "রিচার্জ আইডি কপি করা হয়েছে" else if (isRefund) "রিফান্ড আইডি কপি করা হয়েছে" else "লেনদেন আইডি কপি করা হয়েছে"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            text = if (isDepositOrRecharge) "রিচার্জ: ${trx.id}" else if (isRefund) "রিফান্ড: ${trx.id}" else "লেনদেন: ${trx.id}",
                                            fontSize = 10.sp,
                                            color = SomadhanTextSecondary
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "কপি করুন",
                                            tint = SomadhanTextHint,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                        }
                    }
                    is AdminTransactionListItem.WalletRecharge -> {
                        val payment = item.payment
                        val user = userMap[payment.userId]
                        val isSuccess = payment.status.equals("SUCCESS", ignoreCase = true)
                        val isPending = payment.status.equals("PENDING", ignoreCase = true)

                        val statusBg = when {
                            isSuccess -> Color(0xFFD1FAE5)
                            isPending -> Color(0xFFFEF3C7)
                            else -> Color(0xFFFEE2E2)
                        }
                        val statusTextColor = when {
                            isSuccess -> Color(0xFF065F46)
                            isPending -> Color(0xFF92400E)
                            else -> Color(0xFF991B1B)
                        }
                        val statusText = when {
                            isSuccess -> "সফল (SUCCESS)"
                            isPending -> "পেন্ডিং (PENDING)"
                            else -> "ব্যর্থ (${payment.status})"
                        }

                        // Ground Rule ১৮/২০/২১ — WorkTransaction কার্ডের ঠিক একই তিন-কারণ প্যাটার্ন,
                        // শুধু `gateway_payments` টেবিলের id-সেট (recentlyChangedGatewayPaymentIds)
                        // দিয়ে।
                        val rechargeCardPulse = rememberFieldChangePulse(
                            value = payment,
                            isManualRefreshing = isManualRefreshing,
                            sessionKey = "admin_transactions_sync",
                            viewModel = viewModel,
                            flashOnReentry = false
                        )
                        val isNewFromRealtime = recentlyChangedGatewayPaymentIds.contains(payment.id)
                        PulsingValue(isUpdating = rechargeCardPulse || isFilterRefreshing || isNewFromRealtime) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_recharge_card_${payment.id}"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDFA)),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF99F6E4))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Header row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ওয়ালেট রিচার্জ (${payment.gateway.uppercase()})",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFCCFBF1))
                                                    .border(0.5.dp, Color(0xFF99F6E4), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "💳 গেটওয়ে রিচার্জ",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F766E)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(statusBg)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = statusText,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusTextColor
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = Formatters.formatDateTimeBengali(payment.timestamp),
                                        fontSize = 10.sp,
                                        color = SomadhanTextHint
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = SomadhanDivider, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Details & Amount
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.1f)) {
                                        Text(
                                            text = "ইউজার: ${user?.name ?: payment.userName.ifBlank { payment.userId }}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SomadhanTextPrimary
                                        )
                                        val phone = user?.phone ?: payment.userPhone
                                        if (phone.isNotBlank()) {
                                            Text(
                                                text = "মোবাইল: $phone",
                                                fontSize = 10.5.sp,
                                                color = SomadhanTextSecondary
                                            )
                                        }
                                        Text(
                                            text = "গেটওয়ে: ${payment.gateway.uppercase()}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F766E)
                                        )
                                        if (payment.problemId.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    clipboardManager.setText(AnnotatedString(payment.problemId))
                                                    Toast.makeText(context, "পোস্ট আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text(
                                                    text = "পোস্ট আইডি: #${payment.problemId.take(8).uppercase()}",
                                                    fontSize = 10.5.sp,
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
                                        if (payment.problemTitle.isNotBlank()) {
                                            Text(
                                                text = "কাজ: ${payment.problemTitle}",
                                                fontSize = 10.5.sp,
                                                color = SomadhanTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(0.9f)) {
                                        Text(
                                            text = "রিচার্জ অর্থ",
                                            fontSize = 10.sp,
                                            color = SomadhanTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "৳ ${DistanceUtil.toBengaliDigits(payment.amount)}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSuccess) Color(0xFF0F766E) else SomadhanTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "কমিশন: ৳ ০ (ব্যালেন্স টপ-আপ)",
                                            fontSize = 10.sp,
                                            color = SomadhanTextHint
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Gateway TrxID and Order ID
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val displayTrxId = payment.gatewayTrxId.ifBlank { payment.id }
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFCCFBF1))
                                            .border(0.5.dp, Color(0xFF99F6E4), RoundedCornerShape(6.dp))
                                            .clickable {
                                                clipboardManager.setText(AnnotatedString(displayTrxId))
                                                Toast.makeText(context, "গেটওয়ে TrxID কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = "TrxID",
                                            tint = Color(0xFF0F766E),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "TrxID: ${displayTrxId.take(16)}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F766E)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "কপি করুন",
                                            tint = Color(0xFF0F766E),
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            clipboardManager.setText(AnnotatedString(payment.id))
                                            Toast.makeText(context, "পেমেন্ট রেকর্ড আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            text = "আইডি: ${payment.id.take(12)}",
                                            fontSize = 10.sp,
                                            color = SomadhanTextSecondary
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "কপি করুন",
                                            tint = SomadhanTextHint,
                                            modifier = Modifier.size(10.dp)
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
    }

        // Pagination Bar
        if (filteredListItems.isNotEmpty()) {
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
