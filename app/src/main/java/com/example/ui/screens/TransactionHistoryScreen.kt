package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.data.entity.GatewayPaymentEntity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ListScreenSkeleton
import com.example.ui.components.LoadingAwareContent
import com.example.ui.components.PulsingValue
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.SyncAwareContent
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.Formatters
import com.example.util.RefundCategory
import com.example.util.RefundMeta
import com.example.util.TransactionHelper

// [Step 15.2 — CI_TEST_SUITE_MASTER_PROMPT.md] Bug fix: the old inline isPositive computation
// (`isEarning || isUserRefund || isUserDeposit`) enumerated only three specific transaction
// shapes (solver PAYMENT/DISPUTE_SPLIT earning, user-side REFUND, user-side DEPOSIT) and silently
// defaulted to `false` (always "−") for every other trx.type — including ADMIN_ADJUSTMENT (the
// originally reported bug: an admin *addition* to a user's balance still rendered as "−"), and
// also WITHDRAWAL_REFUND/DUPLICATE_CORRECTION/BALANCE_RECONCILIATION, which had the same silent
// default-to-negative bug (confirmed by reading each RPC's `insert into public.transactions` in
// supabase/migrations/*.sql — see Step 15.2 session notes in CI_TEST_SUITE_PROGRESS.md).
//
// Fix: every transaction row's net_amount is written SIGNED at insert time, for every type,
// relative to whichever ledger (USER or SOLVER) that specific row belongs to — positive means a
// credit to that ledger, negative means a debit (e.g. `-v_wallet_deduction` for
// BID_ACCEPT_DEDUCTION, `case when p_is_addition then p_amount else -p_amount end` for
// ADMIN_ADJUSTMENT). The `trx` values reaching this screen's item list are already filtered via
// `TransactionHelper.matchesRoleForHistory(trx, activeRole, currentUserId)` before rendering, so
// by construction any row shown here belongs to viewerId's own ledger. That makes net_amount's
// sign a single, type-agnostic source of truth for the correct "+"/"−" classification — instead
// of re-deriving it per transaction type (which is exactly what silently broke for any type the
// enumeration forgot). This also makes the fix regression-proof against *future* transaction
// types: any new type will be classified correctly as long as its net_amount is written signed,
// which is already the established convention for every existing type.
//
// A defensive identity check (viewerId must actually match userId or solverId) is kept so the
// function is still meaningful if ever called on a row NOT pre-filtered for viewerId (e.g.
// directly from a unit test) — mirrors the identity half of matchesRoleForHistory().
fun transactionDisplaySign(trx: com.example.data.entity.TransactionEntity, viewerId: String): Boolean {
    if (viewerId.isBlank()) return false
    if (trx.userId != viewerId && trx.solverId != viewerId) return false
    return trx.netAmount >= 0.0
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val transactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val userEscrows by viewModel.userEscrows.collectAsStateWithLifecycle()
    val allAdditionalCharges by viewModel.allAdditionalCharges.collectAsStateWithLifecycle()
    val userGatewayPayments by viewModel.userGatewayPayments.collectAsStateWithLifecycle()

    // Loading/Sync Fix Roadmap v2, ধাপ ৪ — transactions/gateway_payments/escrows টেবিল
    // initialSyncPhase-এর (bulk-pull) অংশ, আলাদা sessionKey "transaction_history_sync"
    // (বিদ্যমান "transaction_history" key-র সাথে সংঘর্ষ এড়াতে)।
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()
    // Loading/Sync Fix Roadmap v2, ধাপ ৬ — এই স্ক্রিনে আগে pull-to-refresh ছিল না, বাকি ১৬টা
    // স্ক্রিনের প্যাটার্নে (SomadhanPullToRefresh → viewModel.refreshData()) যোগ করা হলো।
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val currentUserId = currentUser?.id ?: ""
    val isSolver = currentUser?.role.equals("SOLVER", ignoreCase = true)

    LaunchedEffect(currentUser?.id) {
        viewModel.resetTransactionsPagination(currentUser?.id)
    }

    val transactionsPaged = viewModel.transactionsPaged
    var selectedMainTab by rememberSaveable { mutableStateOf("APP") } // "APP" or "GATEWAY"
    var selectedSubTab by rememberSaveable { mutableStateOf("ALL") } // "ALL", "PAYMENT", "DEPOSIT", "REFUND"

    val isDepositTrx = { trx: com.example.data.entity.TransactionEntity ->
        TransactionHelper.isDepositTrx(trx)
    }
    val isRefundTrx = { trx: com.example.data.entity.TransactionEntity ->
        TransactionHelper.isRefundTrx(trx)
    }
    val isPaymentTrx = { trx: com.example.data.entity.TransactionEntity ->
        TransactionHelper.isPaymentTrx(trx)
    }

    // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ৫] আগে এখানে শুধু identity ম্যাচ করা হতো
    // (userId/solverId == currentUserId), বর্তমান active role কী তা কোনোভাবেই দেখা হতো না --
    // ফলে User মোডে থাকলেও Solver-role-এর PAYMENT ইত্যাদি ঢুকে যেত। এখন
    // TransactionHelper.matchesRoleForHistory() দিয়ে identity + role দুটোই একসাথে যাচাই হয়
    // (ধাপ ৫-এ কনফার্ম করা প্রশ্ন #১-#৩-এর উত্তর অনুযায়ী -- সেই ফাংশনের doc comment দেখুন)।
    val activeRole = if (isSolver) "SOLVER" else "USER"
    val allTransactionsList = remember(transactions, transactionsPaged, currentUserId, activeRole) {
        val raw = if (transactions.isNotEmpty()) transactions else transactionsPaged.toList()
        raw.distinctBy { it.id }.filter { trx ->
            TransactionHelper.matchesRoleForHistory(trx, activeRole, currentUserId)
        }
    }
    val sortedTransactions = remember(allTransactionsList) {
        allTransactionsList.sortedByDescending { it.timestamp }
    }
    val sortedGatewayPayments = remember(userGatewayPayments) {
        userGatewayPayments.sortedByDescending { it.timestamp }
    }
    var selectedGatewayDetail by remember { mutableStateOf<GatewayPaymentEntity?>(null) }
    var selectedProblemGateways by remember { mutableStateOf<List<GatewayPaymentEntity>?>(null) }
    var selectedProblemGatewaysTitle by remember { mutableStateOf("") }
    var selectedProblemGatewaysId by remember { mutableStateOf("") }
    // sortedTransactions এখন ইতিমধ্যেই active role-এ স্কোপড (উপরে), তাই এখানে আর আলাদা করে
    // "it.userId == currentUserId" চেক লাগে না (আগে এই চেকটা শুধু User-মোডে সাব-ট্যাব
    // ফিল্টারে ছিল, Solver-মোডে সাব-ট্যাব UI-ই নেই বলে কোনো ফিল্টারই হতো না -- সেই আসল বাগটা
    // উপরের role ফিল্টারে ঠিক হয়ে গেছে, এখানে শুধু টাইপ-ভিত্তিক সাব-ট্যাব ফিল্টার বাকি)।
    val filteredTransactions = remember(sortedTransactions, selectedSubTab, isSolver) {
        if (isSolver) {
            sortedTransactions
        } else {
            when (selectedSubTab) {
                "PAYMENT" -> sortedTransactions.filter { isPaymentTrx(it) }
                "DEPOSIT" -> sortedTransactions.filter { isDepositTrx(it) }
                "REFUND" -> sortedTransactions.filter { isRefundTrx(it) }
                else -> sortedTransactions
            }
        }
    }

    val totalCount = sortedTransactions.size
    val paymentCount = remember(sortedTransactions) {
        sortedTransactions.count { isPaymentTrx(it) }
    }
    val depositCount = remember(sortedTransactions) {
        sortedTransactions.count { isDepositTrx(it) }
    }
    val refundCount = remember(sortedTransactions) {
        sortedTransactions.count { isRefundTrx(it) }
    }
    val totalFilterCount = if (selectedMainTab == "GATEWAY") sortedGatewayPayments.size else filteredTransactions.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "সকল লেনদেনের ইতিহাস",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SomadhanTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("transaction_history_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SomadhanTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SomadhanCardBg)
            )
        },
        containerColor = SomadhanBg
    ) { innerPadding ->
        SomadhanPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier.fillMaxSize()
        ) {
        // সেশন ২.৪ (batch31 fix8) — আগে এখানে SyncAwareRefreshableContent (data =
        // Pair(filteredTransactions, sortedGatewayPayments)) ব্যবহার হতো, যেটা যেকোনো একটা
        // বদলালেই পুরো LazyColumn (summary stats card + sticky tab/filter header + list — সবটা)
        // একসাথে re-flash করতো, যদিও কাঙ্ক্ষিত আচরণ শুধু list (+ব্যবহারকারীর সিদ্ধান্তে summary
        // stats card-ও) pulse করা, sticky tab/filter header (APP/GATEWAY toggle + filter chip,
        // ভেতরের count-সহ) সবসময় স্থির থাকা (Wallet/ProblemDetail-এ যেমন হয়েছে)।
        // এখন cold-load-only SyncAwareContent-এ আনা হলো (একবার LOADED হলে content() আর নিজে থেকে
        // re-animate করে না) — sticky tab/filter header এখন সবসময় স্থির। নিচের transactionListPulse
        // (summary card + list, দুটো একসাথে) নিজস্ব rememberFieldChangePulse + PulsingValue দিয়ে
        // আলাদাভাবে pulse করে (Wallet/ProblemDetail-এর একই প্যাটার্ন)। selectedMainTab/selectedSubTab
        // pulse-key-এর অংশ, তাই ট্যাব পাল্টালেও (ডেটা সত্যিই বদলেছে কিনা না দেখেই) সংক্ষিপ্ত pulse হয়।
        val transactionListPulse = rememberFieldChangePulse(
            value = Triple(selectedMainTab, selectedSubTab, Pair(filteredTransactions, sortedGatewayPayments)),
            isManualRefreshing = isRefreshing,
            sessionKey = "transaction_history_sync",
            viewModel = viewModel
        )
        SyncAwareContent(
            sessionKey = "transaction_history_sync",
            viewModel = viewModel,
            syncPhase = initialSyncPhase,
            onRetry = { viewModel.retryInitialSync() },
            modifier = Modifier.fillMaxSize()
        ) {
        // বাগ-ফিক্স (রিপোর্ট: এই স্ক্রিনে pull-to-refresh টানলে স্পিনার পুরো না এসে শুধু অর্ধেক
        // আসে, তারপর ছেড়ে দিলে উপরে চলে যায় -- যদিও রিফ্রেশ আসলে ঠিকই হয়, toast/ডেটা লোড হয়)।
        // Root cause: এই স্ক্রিনই একমাত্র জায়গা যেখানে `SomadhanPullToRefresh` (Material3
        // PullToRefreshBox) আর `LazyColumn`-এর `stickyHeader { }` একসাথে ব্যবহার হতো। stickyHeader
        // থাকা অবস্থায় PullToRefreshBox-এর nested-scroll connection লিস্ট সত্যিই scroll-এর
        // একদম উপরে আছে কিনা তা নির্ভরযোগ্যভাবে বুঝতে পারে না, ফলে টানলে অর্ধেক দূরত্বই পায়,
        // থ্রেশহোল্ডে না পৌঁছে ছেড়ে দিলে indicator আবার উপরে fold হয়ে যায়। এখন ট্যাব/ফিল্টার
        // সেকশনটা `stickyHeader { }`-এর বদলে LazyColumn-এর *বাইরে* একটা সাধারণ fixed Composable
        // হিসেবে বসানো হলো (নিচে) -- এতে LazyColumn-এ আর কোনো stickyHeader থাকছে না, তাই
        // PullToRefreshBox পরিষ্কারভাবে "scrolled to top" অবস্থা শনাক্ত করতে পারে। ট্যাব/ফিল্টার
        // আগের মতোই সবসময় স্থির/visible থাকবে (বরং এখন সত্যিকারের fixed, স্ক্রল করে "stick" করা
        // লাগে না) -- শুধু এখন থেকে সামারি কার্ডের উপরে সবসময় দেখা যাবে, আগে যেমন স্ক্রল করলে তবেই
        // উপরে "আটকে" যেত তার বদলে।
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // এখানে আগে এই ট্যাব/ফিল্টার সেকশনটা LazyColumn-এর ভেতরে `stickyHeader { }` হিসেবে
            // ছিল (যাতে স্ক্রল করলেও এটা "আটকে" থাকে)। এখন LazyColumn-এর *বাইরে* একটা সাধারণ fixed
            // Composable হিসেবে বসানো হলো, যাতে নিচের LazyColumn-এ আর কোনো stickyHeader না থাকে —
            // এতে pull-to-refresh (উপরের বাগ-ফিক্স কমেন্ট দেখুন) ঠিকভাবে কাজ করে, আর ট্যাব/ফিল্টার
            // আগের চেয়েও বেশি স্থির থাকে (সবসময় সামারি কার্ডের উপরে fixed, স্ক্রল করে "stick" করা
            // লাগে না)।
            if (!isSolver) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SomadhanBg)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // 2 Primary Tabs: "অ্যাপের লেনদেন" (App Transactions) & "গেটওয়ে লেনদেন" (Gateway Transactions)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Tab 1: App Transactions
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedMainTab == "APP") Color.White else Color.Transparent)
                                .border(
                                    width = if (selectedMainTab == "APP") 1.dp else 0.dp,
                                    color = if (selectedMainTab == "APP") SomadhanOrange.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedMainTab = "APP" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = if (selectedMainTab == "APP") SomadhanOrange else SomadhanTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "অ্যাপের লেনদেন (${DistanceUtil.toBengaliDigits(totalCount.toString())})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedMainTab == "APP") FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedMainTab == "APP") SomadhanOrange else SomadhanTextSecondary
                                )
                            }
                        }

                        // Tab 2: Gateway Transactions
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedMainTab == "GATEWAY") Color.White else Color.Transparent)
                                .border(
                                    width = if (selectedMainTab == "GATEWAY") 1.dp else 0.dp,
                                    color = if (selectedMainTab == "GATEWAY") Color(0xFFE2136E).copy(alpha = 0.5f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedMainTab = "GATEWAY" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (selectedMainTab == "GATEWAY") Color(0xFFE2136E) else SomadhanTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "গেটওয়ে লেনদেন (${DistanceUtil.toBengaliDigits(sortedGatewayPayments.size.toString())})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedMainTab == "GATEWAY") FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedMainTab == "GATEWAY") Color(0xFFE2136E) else SomadhanTextSecondary
                                )
                            }
                        }
                    }

                    if (selectedMainTab == "APP") {
                        Spacer(modifier = Modifier.height(10.dp))
                        // Sub-tabs for App transactions (সব, পেমেন্ট, রিচার্জ, রিফান্ড)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedSubTab == "ALL",
                                onClick = { selectedSubTab = "ALL" },
                                label = {
                                    Text(
                                        text = "সব (${DistanceUtil.toBengaliDigits(totalCount.toString())})",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (selectedSubTab == "ALL") FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SomadhanOrange,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("transaction_history_tab_all")
                            )
                            FilterChip(
                                selected = selectedSubTab == "PAYMENT",
                                onClick = { selectedSubTab = "PAYMENT" },
                                label = {
                                    Text(
                                        text = "পেমেন্ট (${DistanceUtil.toBengaliDigits(paymentCount.toString())})",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (selectedSubTab == "PAYMENT") FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SomadhanOrange,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("transaction_history_tab_payment")
                            )
                            FilterChip(
                                selected = selectedSubTab == "DEPOSIT",
                                onClick = { selectedSubTab = "DEPOSIT" },
                                label = {
                                    Text(
                                        text = "রিচার্জ (${DistanceUtil.toBengaliDigits(depositCount.toString())})",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (selectedSubTab == "DEPOSIT") FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SomadhanOrange,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("transaction_history_tab_deposit")
                            )
                            FilterChip(
                                selected = selectedSubTab == "REFUND",
                                onClick = { selectedSubTab = "REFUND" },
                                label = {
                                    Text(
                                        text = "রিফান্ড (${DistanceUtil.toBengaliDigits(refundCount.toString())})",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (selectedSubTab == "REFUND") FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SomadhanOrange,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("transaction_history_tab_refund")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))

                // Summary Stats Card — সেশন ২.৪: ব্যবহারকারীর সিদ্ধান্ত অনুযায়ী এটাও নিচের
                // list-এর সাথেই pulse করে (transactionListPulse), যেহেতু "ফিল্টার ফলাফল" সংখ্যা
                // লিস্টের সাথেই বদলায়।
                PulsingValue(isUpdating = transactionListPulse) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "মোট লেনদেন",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = "${DistanceUtil.toBengaliDigits(totalCount.toString())} টি",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrange
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ফিল্টার ফলাফল",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = "${DistanceUtil.toBengaliDigits(totalFilterCount.toString())} টি",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }
                    }
                }
                }
            }

            if (selectedMainTab == "GATEWAY") {
                if (sortedGatewayPayments.isEmpty()) {
                    item {
                        PulsingValue(isUpdating = transactionListPulse) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "কোনো গেটওয়ে পেমেন্ট বা রিচার্জের তথ্য পাওয়া যায়নি।",
                                    fontSize = 14.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        }
                        }
                    }
                } else {
                    items(sortedGatewayPayments, key = { it.id }) { gw ->
                        val brandColor = when (gw.gateway.uppercase()) {
                            "BKASH" -> Color(0xFFE2136E)
                            "NAGAD" -> Color(0xFFF7941D)
                            "ROCKET" -> Color(0xFF8C3494)
                            else -> Color(0xFF1D4ED8)
                        }
                        val lightBg = when (gw.gateway.uppercase()) {
                            "BKASH" -> Color(0xFFFDF2F8)
                            "NAGAD" -> Color(0xFFFFF7ED)
                            "ROCKET" -> Color(0xFFFAF5FF)
                            else -> Color(0xFFEFF6FF)
                        }
                        val isDeposit = gw.purpose.uppercase() == "WALLET_DEPOSIT"
                        val purposeText = when (gw.purpose) {
                            "WALLET_DEPOSIT" -> "ওয়ালেট রিচার্জ (টপ-আপ)"
                            "ESCROW_PAYMENT" -> "বিড পেমেন্ট (এসক্রো)"
                            "ADDITIONAL_CHARGE" -> "অতিরিক্ত বিল পেমেন্ট"
                            "RELEASE_EXTRA" -> "রিলিজ অতিরিক্ত বিল"
                            "DIRECT_PAYMENT" -> "সরাসরি চুক্তি পেমেন্ট"
                            else -> if (gw.problemTitle.isNotBlank()) gw.problemTitle else gw.purpose
                        }

                        PulsingValue(isUpdating = transactionListPulse) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, brandColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .clickable { selectedGatewayDetail = gw }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(brandColor)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = gw.gateway.uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(lightBg)
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = purposeText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SomadhanTextPrimary
                                            )
                                        }
                                    }

                                    Text(
                                        text = "+${Formatters.formatTaka(gw.amount)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            clipboardManager.setText(AnnotatedString(gw.gatewayTrxId))
                                            Toast.makeText(context, "ট্রানজেকশন আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            text = "TrxID: ${gw.gatewayTrxId}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = brandColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "কপি করুন",
                                            tint = brandColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    if (gw.userPhone.isNotBlank()) {
                                        Text(
                                            text = "নম্বর: ${gw.userPhone}",
                                            fontSize = 11.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }
                                }

                                if (gw.problemId.isNotBlank() || gw.problemTitle.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFFF7ED))
                                            .border(0.5.dp, Color(0xFFFFEDD5), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                if (gw.problemId.isNotBlank()) {
                                                    clipboardManager.setText(AnnotatedString(gw.problemId))
                                                    Toast.makeText(context, "পোস্ট আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Receipt, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (gw.problemId.isNotBlank()) "পোস্ট ID: #${gw.problemId.take(8).uppercase()}" else "পোস্ট সংযুক্ত",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanOrange
                                            )
                                            if (gw.problemId.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(Icons.Default.ContentCopy, contentDescription = "কপি", tint = SomadhanOrange, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                        if (gw.problemTitle.isNotBlank()) {
                                            Text(
                                                text = gw.problemTitle,
                                                fontSize = 10.5.sp,
                                                color = SomadhanTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(start = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = Formatters.formatDateTimeBengali(gw.timestamp),
                                        fontSize = 10.5.sp,
                                        color = SomadhanTextHint
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(brandColor.copy(alpha = 0.1f))
                                                .clickable { selectedGatewayDetail = gw }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "রসিদ দেখুন",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = brandColor
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFDCFCE7))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF166534),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "গেটওয়ে সম্পন্ন",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF166534)
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
            } else if (filteredTransactions.isEmpty() && !viewModel.transactionsLoadingMore) {
                item {
                    PulsingValue(isUpdating = transactionListPulse) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSolver) {
                                    "এখনও কোনো লেনদেনের তথ্য নেই।"
                                } else {
                                    when (selectedSubTab) {
                                        "PAYMENT" -> "কোনো সম্পন্ন পেমেন্টের লেনদেন পাওয়া যায়নি।"
                                        "DEPOSIT" -> "কোনো ওয়ালেট রিচার্জের লেনদেন পাওয়া যায়নি।"
                                        "REFUND" -> "কোনো রিফান্ডের লেনদেন পাওয়া যায়নি।"
                                        else -> "এখনও কোনো লেনদেনের তথ্য নেই।"
                                    }
                                },
                                fontSize = 14.sp,
                                color = SomadhanTextHint
                            )
                        }
                    }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { trx ->
                    val isDeposit = isDepositTrx(trx)
                    val isRefund = isRefundTrx(trx)
                    val isEarning = (currentUserId == trx.solverId && !isRefund && !isDeposit)
                    val isUserRefund = (currentUserId == trx.userId && isRefund)
                    val isUserDeposit = (currentUserId == trx.userId && isDeposit)
                    // [Step 15.2 bug fix] see transactionDisplaySign() doc-comment above — the old
                    // `isEarning || isUserRefund || isUserDeposit` silently defaulted to false
                    // (always "−") for any type those three didn't enumerate (ADMIN_ADJUSTMENT
                    // etc). isEarning/isUserRefund/isUserDeposit are kept above unchanged — they
                    // still drive other UI (commission breakdown, deposit method label, etc.).
                    val isPositive = transactionDisplaySign(trx, currentUserId)

                    val escrow = userEscrows.find { it.problemId == trx.problemId }
                    val problem = allProblems.find { it.id == trx.problemId }
                    val refundMeta = if (isRefund) TransactionHelper.getRefundMeta(trx, problem) else null
                    val displayTitle = TransactionHelper.getDisplayTitle(trx, problem)

                    val refundIconColor = when (refundMeta?.category) {
                        RefundCategory.SPLIT_REFUND -> Color(0xFF9333EA)
                        RefundCategory.DISPUTE_REFUND -> Color(0xFFDC2626)
                        else -> Color(0xFF2563EB)
                    }

                    val acceptedExtraCharges = allAdditionalCharges.filter { it.problemId == trx.problemId && it.status == "ACCEPTED" }.sumOf { it.amount }

                    val extraAmt = if (trx.extraAmount > 0.0) {
                        trx.extraAmount
                    } else if (escrow != null && escrow.extraAmount > 0.0) {
                        escrow.extraAmount
                    } else if (acceptedExtraCharges > 0.0) {
                        acceptedExtraCharges
                    } else if (problem != null && problem.releaseRequestExtraAmount > 0.0) {
                        problem.releaseRequestExtraAmount
                    } else {
                        0.0
                    }

                    val baseBidAmt = if (trx.baseAmount > 0.0) {
                        trx.baseAmount
                    } else if (escrow != null && escrow.baseAmount > 0.0) {
                        escrow.baseAmount
                    } else if (problem?.acceptedAmount != null && problem.acceptedAmount > 0.0) {
                        problem.acceptedAmount
                    } else {
                        (trx.grossAmount - extraAmt).coerceAtLeast(0.0)
                    }

                    val finalExtra = if (trx.extraAmount > 0.0) {
                        trx.extraAmount
                    } else if (extraAmt > 0.0 && baseBidAmt + extraAmt == trx.grossAmount) {
                        extraAmt
                    } else if (trx.grossAmount > baseBidAmt && baseBidAmt > 0.0) {
                        (trx.grossAmount - baseBidAmt)
                    } else {
                        extraAmt
                    }
                    val finalBid = if (trx.baseAmount > 0.0) {
                        trx.baseAmount
                    } else {
                        (trx.grossAmount - finalExtra).coerceAtLeast(0.0)
                    }
                    val effectiveEscrowId = when {
                        trx.escrowId.isNotBlank() -> trx.escrowId
                        escrow != null && escrow.id.isNotBlank() -> escrow.id
                        trx.problemId.isNotBlank() -> "ESC_${trx.problemId.take(8).uppercase()}"
                        else -> "ESC_${trx.id.take(8).uppercase()}"
                    }

                    PulsingValue(isUpdating = transactionListPulse) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    isDeposit -> SomadhanSuccess.copy(alpha = 0.12f)
                                                    isRefund -> refundIconColor.copy(alpha = 0.12f)
                                                    isEarning -> SomadhanSuccess.copy(alpha = 0.12f)
                                                    else -> SomadhanOrange.copy(alpha = 0.12f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                isDeposit -> Icons.Default.AccountBalanceWallet
                                                isRefund -> Icons.Default.MonetizationOn
                                                isEarning -> Icons.Default.MonetizationOn
                                                else -> Icons.Default.ReceiptLong
                                            },
                                            contentDescription = null,
                                            tint = when {
                                                isDeposit -> SomadhanSuccess
                                                isRefund -> refundIconColor
                                                isEarning -> SomadhanSuccess
                                                else -> SomadhanOrange
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = displayTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                clipboardManager.setText(AnnotatedString(trx.id))
                                                val msg = when {
                                                    isDeposit -> "রিচার্জ আইডি কপি করা হয়েছে"
                                                    isRefund -> "${refundMeta?.labelBn ?: "রিফান্ড"} আইডি কপি করা হয়েছে"
                                                    else -> "লেনদেন আইডি কপি করা হয়েছে"
                                                }
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text(
                                                text = when {
                                                    isDeposit -> "রিচার্জ: ${trx.id.take(12)}"
                                                    isRefund -> "${refundMeta?.labelBn ?: "রিফান্ড"}: ${trx.id.take(12)}"
                                                    else -> "আইডি: ${trx.id.take(12)}"
                                                },
                                                fontSize = 11.sp,
                                                color = SomadhanTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (isPositive) "+ " else "- "}৳ ${DistanceUtil.toBengaliDigits(if (isEarning) trx.netAmount else trx.grossAmount)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPositive) SomadhanSuccess else SomadhanOrange
                                    )
                                    Text(
                                        text = Formatters.formatDateTimeBengali(trx.timestamp),
                                        fontSize = 10.sp,
                                        color = SomadhanTextHint
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isDeposit) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF0FDF4))
                                        .border(0.5.dp, Color(0xFFBBF7D0), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ওয়ালেট ডিপোজিট (অনলাইন রিচার্জ)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF166534)
                                        )
                                        Text(
                                            text = "সরাসরি মূল ওয়ালেটে যোগ হয়েছে",
                                            fontSize = 11.sp,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "+ ৳${DistanceUtil.toBengaliDigits(trx.grossAmount)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534)
                                        )
                                        Text(
                                            text = "সফল রিচার্জ",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanSuccess
                                        )
                                    }
                                }
                            } else {
                                // Breakdown section with Bid Amount, Extra Amount, and Total Amount
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isRefund) "ফেরত মূল অংশ: ৳${DistanceUtil.toBengaliDigits(finalBid)}" else "বিড চুক্তি মূল্য: ৳${DistanceUtil.toBengaliDigits(finalBid)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SomadhanTextPrimary
                                        )
                                        if (finalExtra > 0.0) {
                                            Text(
                                                text = if (isRefund) "ফেরত অতিরিক্ত বিল: +৳${DistanceUtil.toBengaliDigits(finalExtra)}" else "অতিরিক্ত বিল: +৳${DistanceUtil.toBengaliDigits(finalExtra)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isRefund) Color(0xFF2563EB) else SomadhanOrange
                                            )
                                        } else {
                                            Text(
                                                text = if (isRefund) "অতিরিক্ত বিল: ৳০" else "অতিরিক্ত বিল: ৳০",
                                                fontSize = 11.sp,
                                                color = SomadhanTextSecondary
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isRefund) "মোট ${refundMeta?.labelBn ?: "রিফান্ড"}: ৳${DistanceUtil.toBengaliDigits(trx.grossAmount)}" else "সর্বমোট লেনদেন: ৳${DistanceUtil.toBengaliDigits(trx.grossAmount)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPositive) SomadhanSuccess else Color(0xFF1E293B)
                                        )
                                        if (isEarning) {
                                            val effectiveBaseAmt = if (trx.baseAmount > 0.0) trx.baseAmount else (trx.grossAmount - trx.extraAmount).coerceAtLeast(0.0)
                                            val effectiveBaseComm = if (trx.baseCommissionAmount > 0.0 || trx.wasFreeQuotaJob) trx.baseCommissionAmount else (trx.commissionAmount - trx.extraCommissionAmount).coerceAtLeast(0.0)
                                            if (effectiveBaseAmt > 0.0) {
                                                val basePercent = if (effectiveBaseAmt > 0.0) (effectiveBaseComm / effectiveBaseAmt) * 100.0 else 0.0
                                                Text(
                                                    text = "বেস কমিশন (${DistanceUtil.toBengaliDigits(basePercent)}%): " +
                                                        "-৳${DistanceUtil.toBengaliDigits(effectiveBaseComm)}" +
                                                        (if (trx.wasFreeQuotaJob) "  🆓 কমিশন-ফ্রি জব" else ""),
                                                    fontSize = 11.sp,
                                                    color = if (trx.wasFreeQuotaJob) SomadhanSuccess else Color(0xFF64748B)
                                                )
                                            }
                                            if (trx.extraAmount > 0.0) {
                                                val extraPercent = (trx.extraCommissionAmount / trx.extraAmount) * 100.0
                                                Text(
                                                    text = "অতিরিক্ত বিল কমিশন (${DistanceUtil.toBengaliDigits(extraPercent)}%" +
                                                        (if (trx.extraCommissionApplied) ", ছাড় প্রযোজ্য" else "") + "): " +
                                                        "-৳${DistanceUtil.toBengaliDigits(trx.extraCommissionAmount)}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            Text(
                                                text = "মোট কমিশন: -৳${DistanceUtil.toBengaliDigits(trx.commissionAmount)} " +
                                                    "(নিট প্রাপ্তি: ৳${DistanceUtil.toBengaliDigits(trx.netAmount)})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SomadhanTextPrimary
                                            )
                                        }

                                        // Payment Receipt button under Total Transaction if available
                                        val matchingGateways = if (trx.problemId.isNotBlank()) userGatewayPayments.filter { it.problemId == trx.problemId && it.problemId.isNotBlank() } else emptyList()
                                        if (matchingGateways.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFEFF6FF))
                                                    .border(0.6.dp, Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        selectedProblemGateways = matchingGateways
                                                        selectedProblemGatewaysTitle = matchingGateways.firstOrNull()?.problemTitle ?: ""
                                                        selectedProblemGatewaysId = trx.problemId
                                                    }
                                                    .padding(horizontal = 7.dp, vertical = 2.5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Receipt,
                                                    contentDescription = null,
                                                    tint = Color(0xFF1D4ED8),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "পেমেন্ট রসিদ",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1D4ED8)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom metadata row: Escrow ID on Left, Post ID on Right
                            if (isDeposit) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFF0FDF4))
                                            .border(0.5.dp, Color(0xFFBBF7D0), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "সফল",
                                            tint = SomadhanSuccess,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "গেটওয়ে পেমেন্ট",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // বাম দিকে এসক্রো ID
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
                                            text = "এসক্রো ID: $effectiveEscrowId",
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

                                    // ডান দিকে পোস্ট ID
                                    if (trx.problemId.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                clipboardManager.setText(AnnotatedString(trx.problemId))
                                                Toast.makeText(context, "পোস্ট আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Receipt, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "পোস্ট ID: #${trx.problemId.take(8).uppercase()}",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SomadhanOrange
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(Icons.Default.ContentCopy, contentDescription = "কপি", tint = SomadhanOrange, modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }

                // Infinite Scroll Footer
                if (viewModel.transactionsHasMore) {
                    item {
                        LaunchedEffect(Unit) {
                            viewModel.loadNextTransactionsPage()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.transactionsLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = SomadhanOrange
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
        }
        }
        }
    }

    // Gateway Payment Receipt Dialog
    if (selectedGatewayDetail != null) {
        val item = selectedGatewayDetail!!
        val brandColor = when (item.gateway.uppercase()) {
            "BKASH" -> Color(0xFFE2136E)
            "NAGAD" -> Color(0xFFF7941D)
            "ROCKET" -> Color(0xFF8C3494)
            else -> Color(0xFF1D4ED8)
        }
        val lightBg = when (item.gateway.uppercase()) {
            "BKASH" -> Color(0xFFFDF2F8)
            "NAGAD" -> Color(0xFFFFF7ED)
            "ROCKET" -> Color(0xFFFAF5FF)
            else -> Color(0xFFEFF6FF)
        }
        val gatewayDisplayName = when (item.gateway.uppercase()) {
            "BKASH" -> "bKash (বিকাশ)"
            "NAGAD" -> "Nagad (নগদ)"
            "ROCKET" -> "Rocket (রকেট)"
            else -> item.gateway
        }
        val purposeText = when (item.purpose) {
            "WALLET_DEPOSIT" -> "ওয়ালেট রিচার্জ (টপ-আপ)"
            "ESCROW_PAYMENT" -> "কাজের এসক্রো পেমেন্ট (বিড গ্রহণ)"
            "ADDITIONAL_CHARGE" -> "অতিরিক্ত বিল পেমেন্ট"
            "RELEASE_EXTRA" -> "রিলিজ অতিরিক্ত বিল"
            "DIRECT_PAYMENT" -> "সরাসরি চুক্তি পেমেন্ট"
            else -> item.purpose
        }

        val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

        ModalBottomSheet(
            onDismissRequest = { selectedGatewayDetail = null },
            sheetState = detailSheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Top Header Row with Title and Close (✕) Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(lightBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = brandColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "অনলাইন পেমেন্ট রসিদ",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(lightBg)
                                .border(0.5.dp, brandColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = gatewayDisplayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = brandColor
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { selectedGatewayDetail = null },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "বন্ধ করুন",
                                tint = SomadhanTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Amount Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "মোট পরিশোধিত অর্থ",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = "সফল ও স্বয়ংক্রিয়ভাবে যাচাইকৃত",
                                fontSize = 10.5.sp,
                                color = SomadhanTextHint
                            )
                        }
                        Text(
                            text = Formatters.formatTaka(item.amount),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanSuccess
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detail Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "উদ্দেশ্য: $purposeText",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanTextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (item.problemId.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(item.problemId))
                                    Toast.makeText(context, "পোস্ট আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = "পোস্ট আইডি: #${item.problemId.take(8).uppercase()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "কপি",
                                    tint = SomadhanOrange,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (item.problemTitle.isNotBlank()) {
                            Text(
                                text = "কাজের শিরোনাম: ${item.problemTitle}",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(item.gatewayTrxId))
                                Toast.makeText(context, "ট্রানজেকশন আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "ট্রানজেকশন আইডি: ${item.gatewayTrxId}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = brandColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "কপি",
                                tint = brandColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        if (item.userPhone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "প্রেরক একাউন্ট: ${item.userPhone}",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                        }

                        if (item.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "রেফারেন্স / নোট: ${item.note}",
                                fontSize = 12.sp,
                                color = SomadhanTextHint
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "তারিখ ও সময়: ${Formatters.formatDateTimeBengali(item.timestamp)}",
                            fontSize = 11.sp,
                            color = SomadhanTextHint
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SomadhanSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "পেমেন্ট সফল ও যাচাইকৃত",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanSuccess
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Close button
                Button(
                    onClick = { selectedGatewayDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "বন্ধ করুন",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet for Post-wise Multiple Gateway Payments (Draggable with Close Button)
    if (selectedProblemGateways != null) {
        val gateways = selectedProblemGateways!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        val totalAmount = gateways.sumOf { it.amount }
        val countBengali = DistanceUtil.toBengaliDigits(gateways.size.toString())

        ModalBottomSheet(
            onDismissRequest = { selectedProblemGateways = null },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Top Header Row with Title and Close (✕) Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "গেটওয়ে পেমেন্ট রসিদ বিবরণ",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }

                        if (selectedProblemGatewaysId.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(selectedProblemGatewaysId))
                                    Toast.makeText(context, "পোস্ট আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = "পোস্ট ID: #${selectedProblemGatewaysId.take(8).uppercase()}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
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
                    }

                    IconButton(
                        onClick = { selectedProblemGateways = null },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "বন্ধ করুন",
                            tint = SomadhanTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Card Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "মোট অনলাইন পেমেন্ট (${countBengali}টি)",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = "সফল ও যাচাইকৃত লেনদেনসমূহ",
                                fontSize = 10.5.sp,
                                color = SomadhanTextHint
                            )
                        }
                        Text(
                            text = Formatters.formatTaka(totalAmount),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanSuccess
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "প্রতিটি লেনদেনের তালিকা:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // List of individual gateway payments
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gateways.forEachIndexed { index, gw ->
                        val brandColor = when (gw.gateway.uppercase()) {
                            "BKASH" -> Color(0xFFD12053)
                            "NAGAD" -> Color(0xFFE31B23)
                            "ROCKET" -> Color(0xFF8C3494)
                            "BANK" -> Color(0xFF1D4ED8)
                            else -> SomadhanOrange
                        }
                        val lightBg = when (gw.gateway.uppercase()) {
                            "BKASH" -> Color(0xFFFDF2F4)
                            "NAGAD" -> Color(0xFFFEF2F2)
                            "ROCKET" -> Color(0xFFFAF5FF)
                            "BANK" -> Color(0xFFEFF6FF)
                            else -> Color(0xFFFFF7ED)
                        }
                        val purposeText = when (gw.purpose) {
                            "WALLET_DEPOSIT" -> "ওয়ালেট রিচার্জ"
                            "ESCROW_PAYMENT" -> "মূল চুক্তি এসক্রো পেমেন্ট"
                            "ADDITIONAL_CHARGE" -> "অতিরিক্ত বিল পেমেন্ট"
                            "RELEASE_EXTRA" -> "রিলিজ অতিরিক্ত বিল"
                            else -> gw.purpose.ifBlank { "কাজের পেমেন্ট" }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Header: Method badge + Amount
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(lightBg)
                                                .border(0.5.dp, brandColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${DistanceUtil.toBengaliDigits((index + 1).toString())}. ${gw.gateway.uppercase()}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = brandColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = purposeText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SomadhanTextPrimary
                                        )
                                    }

                                    Text(
                                        text = "+৳${DistanceUtil.toBengaliDigits(gw.amount)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Transaction ID click to copy
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            clipboardManager.setText(AnnotatedString(gw.gatewayTrxId))
                                            Toast.makeText(context, "TrxID কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            text = "TrxID: ${gw.gatewayTrxId}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = brandColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "কপি",
                                            tint = brandColor,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }

                                    if (gw.userPhone.isNotBlank()) {
                                        Text(
                                            text = "প্রেরক: ${gw.userPhone}",
                                            fontSize = 11.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }
                                }

                                if (gw.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "নোট: ${gw.note}",
                                        fontSize = 11.sp,
                                        color = SomadhanTextHint
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "সময়: ${Formatters.formatDateTimeBengali(gw.timestamp)}",
                                        fontSize = 10.5.sp,
                                        color = SomadhanTextHint
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SomadhanSuccess,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "সফল",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanSuccess
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Close button
                Button(
                    onClick = { selectedProblemGateways = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "বন্ধ করুন",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
