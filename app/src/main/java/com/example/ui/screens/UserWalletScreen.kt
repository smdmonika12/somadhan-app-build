package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SupportAgent
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.EscrowEntity
import com.example.data.entity.GatewayPaymentEntity
import com.example.data.entity.TransactionEntity
import com.example.util.TransactionHelper
import com.example.data.entity.WithdrawalEntity
import com.example.ui.navigation.Screen
import com.example.ui.components.ListScreenSkeleton
import com.example.ui.components.LoadingAwareContent
import com.example.ui.components.PulsingValue
import com.example.ui.components.SyncAwareContent
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.components.MerchantPaymentDialog
import com.example.ui.components.NotificationBottomSheet
import com.example.ui.components.OutboxPendingIndicator
import com.example.ui.components.SomadhanBottomNav
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.SomadhanTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.components.handleSomadhanNotification
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.Formatters
import com.example.ui.components.BottomSlideAlertDialog

// Section-3 pattern: captures the entity/details needed for the post-gateway-payment
// verification overlay for depositMoneyViaGateway (so a retry can be fired with the same details).
private data class DepositGatewayPendingDetails(
    val amount: Double,
    val gateway: String,
    val gatewayTrxId: String,
    val senderPhone: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserWalletScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onNavigate: ((String) -> Unit)? = null,
    onNavigateToSupport: (() -> Unit)? = null,
    onProblemClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val isLocationUpdating by viewModel.isLocationUpdating.collectAsStateWithLifecycle()
    var showNotificationsSheet by remember { mutableStateOf(false) }

    // ধাপ ৮ (RPC_SYNC_FIX ট্র্যাক) — outbox pending sync ইন্ডিকেটর, escrow/withdrawal outbox
    // wire হওয়া ফাংশনগুলোর (Step 7) জন্য। pendingCount == 0 হলে OutboxPendingIndicator নিজেই
    // কিছু রেন্ডার করে না।
    val outboxPendingCount by viewModel.outboxPendingCount.collectAsStateWithLifecycle()

    val userEscrows by viewModel.userEscrows.collectAsStateWithLifecycle()
    val userTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    val userGatewayPayments by viewModel.userGatewayPayments.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val allAdditionalCharges by viewModel.allAdditionalCharges.collectAsStateWithLifecycle()
    val minWithdrawalAmount by viewModel.minWithdrawalAmount.collectAsStateWithLifecycle()

    // Loading/Sync Fix Roadmap v2, ধাপ ৪ — transactions/escrows/gateway_payments/withdrawals
    // টেবিল initialSyncPhase-এর (bulk-pull) অংশ, আলাদা sessionKey "user_wallet_sync"
    // (বিদ্যমান "user_wallet" key-র সাথে সংঘর্ষ এড়াতে)।
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var addMoneyAmountStr by remember { mutableStateOf("") }
    var addMoneyPayableAmount by remember { mutableStateOf<Double?>(null) }
    // Section-3 pattern: post-gateway-payment verification overlay state for depositMoneyViaGateway
    var depositGatewayPendingDetails by remember { mutableStateOf<DepositGatewayPendingDetails?>(null) }
    var depositGatewayError by remember { mutableStateOf<String?>(null) }
    var selectedGatewayDetail by remember { mutableStateOf<GatewayPaymentEntity?>(null) }
    var selectedProblemGateways by remember { mutableStateOf<List<GatewayPaymentEntity>?>(null) }
    var selectedProblemGatewaysTitle by remember { mutableStateOf("") }
    var selectedProblemGatewaysId by remember { mutableStateOf("") }
    // Wallet+Gateway split support for "অতিরিক্ত বিল" (EXTRA_CHARGE_DEDUCTION) receipts:
    // when this trx's full amount wasn't covered by gateway payments alone, the shortfall
    // came from the wallet balance -- these two hold what's needed to compute that split
    // inside the receipt bottom sheet below.
    var selectedProblemGatewaysTrxAmount by remember { mutableStateOf(0.0) }
    var selectedProblemGatewaysIsExtraCharge by remember { mutableStateOf(false) }

    val minWithdrawBengali = DistanceUtil.toBengaliDigits(DistanceUtil.roundTaka(minWithdrawalAmount))

    LaunchedEffect(Unit) {
        viewModel.reconcileEscrows()
    }

    // [ব্যালেন্স ফিক্স — ধাপ ১] শেয়ার্ড/legacy `balance` কলাম role-switch-এর সময় stale হতে
    // পারে (MONEY_FLOW_AND_ADMIN_BUGS_MASTER_PROMPT.md, বাগ A) — এখন সরাসরি role-scoped
    // `balanceUser` পড়া হচ্ছে, কারণ এই স্ক্রিন সবসময় USER role active থাকা অবস্থায়ই দেখা যায়।
    val currentBalance = currentUser?.balanceUser ?: 0.0

    // Filter held escrows for the current user (guarding against cancelled or completed posts).
    // NOTE: we intentionally do NOT also exclude by "this problem has some REFUND transaction"
    // (problemId-scoped) here. A single problem/post can go through multiple payment cycles
    // (bid A accepted -> cancelled/refunded -> bid B accepted -> new escrow). A problem-wide
    // refund check would wrongly hide bid B's fresh, genuinely HELD escrow just because bid A's
    // earlier cycle was refunded — exactly the same "problemId not escrowId" bug that was fixed
    // on the backend (see, historically, the now-deleted FirebaseSyncManager.resolveIncomingEscrowStatus /
    // the current SomadhanRepository.reconcileEscrowStates). escrow.status == "HELD" is already accurate
    // per-escrow, so it alone is the source of truth here.
    val heldEscrows = remember(userEscrows, allProblems) {
        userEscrows.filter { escrow ->
            if (escrow.status.uppercase() != "HELD") return@filter false
            val prob = allProblems.find { it.id == escrow.problemId }
            if (prob != null && (prob.status == "CANCELLED" || prob.status == "COMPLETED" || (prob.status == "OPEN" && prob.acceptedSolverId.isNullOrBlank()))) {
                return@filter false
            }
            true
        }
    }
    val totalHeldEscrowAmount = remember(heldEscrows) {
        heldEscrows.sumOf { it.baseAmount + it.extraAmount }
    }

    val isSolver = currentUser?.role == "SOLVER"

    // সেশন ২.৩ (batch 31, fix2_3 half1) — আগের "ব্যাচ ৭" মাইগ্রেশন পুরো Column-কেই (header
    // লেবেল, রিচার্জ/টাকা-তোলা বাটন-সহ) একটা একক SyncAwareRefreshableContent-এর diff-শিমারের
    // আওতায় এনেছিল, যার ফলে যেকোনো একটা ফিল্ড (এমনকি অপ্রাসঙ্গিক allProblems/
    // allAdditionalCharges) বদলালেই পুরো পেজ সংক্ষিপ্তভাবে flash করতো — এটাই এই সেশনের
    // রিপোর্ট-করা "মাঝে মাঝে পুরো পেজ auto-reload" বাগ (ধাপ ০.১৪(খ))। এখন সেই কম্পোজিট
    // walletSyncData আর ব্যবহার হয় না — নিচে cold-load-এর জন্য শুধু SyncAwareContent
    // (Refreshable না, তাই কোনো content-লেভেল diff-শিমার নেই), আর balance/escrow/transaction
    // list-এর জন্য আলাদা আলাদা rememberFieldChangePulse+PulsingValue (নিচে দেখো) — যেন শুধু
    // এই নির্দিষ্ট অংশগুলোই re-entry/pull-to-refresh/আসল ডেটা-বদলে সংক্ষিপ্ত pulse দেখায়,
    // header/action বাটন/ট্যাব-সিলেক্টর কখনো re-animate না হয়।
    val balancePulse = rememberFieldChangePulse(
        value = currentBalance,
        isManualRefreshing = isRefreshing,
        sessionKey = "user_wallet_sync",
        viewModel = viewModel
    )
    val escrowPulse = rememberFieldChangePulse(
        value = Pair(totalHeldEscrowAmount, heldEscrows),
        isManualRefreshing = isRefreshing,
        sessionKey = "user_wallet_sync",
        viewModel = viewModel
    )

    if (showNotificationsSheet) {
        NotificationBottomSheet(
            notifications = notifications,
            onDismiss = { showNotificationsSheet = false },
            onMarkAllAsRead = { viewModel.markAllNotificationsRead() },
            onMarkAsRead = { notifId -> viewModel.markNotificationRead(notifId) },
            onNotificationItemClick = { notif ->
                showNotificationsSheet = false
                if (onNavigate != null) {
                    handleSomadhanNotification(
                        notif = notif,
                        onNavigate = onNavigate,
                        onProblemClick = onProblemClick,
                        isSolver = isSolver
                    )
                }
            },
            isSolver = isSolver
        )
    }

    Scaffold(
        topBar = {
            SomadhanTopBar(
                title = "ওয়ালেট",
                currentUser = currentUser,
                unreadCount = unreadCount,
                onNotificationClick = { showNotificationsSheet = true },
                onAdminClick = null,
                showReputation = false,
                locationAddress = liveLocation.address,
                onLocationRefresh = { viewModel.refreshLiveLocation() },
                isLocationUpdating = isLocationUpdating
            )
        },
        bottomBar = {
            if (onNavigate != null) {
                val platformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
                val isWalletEnabled = platformSettings.find { it.key == "menu_wallet_enabled" }?.value != "false"
                val isInstantJobEnabled = platformSettings.find { it.key == "instant_job_feature_enabled" }?.value?.toBooleanStrictOrNull() ?: true
                SomadhanBottomNav(
                    currentRoute = "wallet",
                    isSolver = isSolver,
                    isWalletEnabled = isWalletEnabled,
                    isInstantJobEnabled = isInstantJobEnabled,
                    onNavigate = onNavigate
                )
            }
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        SomadhanPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshWalletData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // সেশন ২.৩ — শুধু cold-load (এই app session-এ প্রথমবার) পুরো-পেজ স্কেলিটন/এরর
            // দেখানোর জন্য SyncAwareContent (Refreshable না)। একবার LOADED হয়ে গেলে এই
            // কম্পোনেন্ট নিজে থেকে আর কখনো re-animate করে না — content() ঠিক যেভাবে দেওয়া
            // হয়েছে তেমনই থাকে, তাই header/action বাটন/ট্যাব-সিলেক্টর সবসময় স্থির। নিচের
            // balancePulse/escrowPulse আর transaction list-এর নিজস্ব rememberFieldChangePulse-ই
            // শুধু re-entry/pull-to-refresh/ডেটা-বদলে সংক্ষিপ্ত pulse দেখায়।
            SyncAwareContent(
                sessionKey = "user_wallet_sync",
                viewModel = viewModel,
                syncPhase = initialSyncPhase,
                onRetry = { viewModel.retryInitialSync() }
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SomadhanBg)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .testTag("user_wallet_screen")
            ) {
            // 1. Current Balance Card (Withdrawable Balance)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D4ED8)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "বর্তমান ওয়ালেট ব্যালেন্স",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            // [ব্যালেন্স UI ফিক্স] USER আর SOLVER role-এর জন্য ব্যালেন্স ইচ্ছাকৃতভাবেই
                            // আলাদা আলাদা পুল (balance_user/balance_solver) — role switch করলে সংখ্যাটা
                            // বদলে যাওয়া স্বাভাবিক, কিন্তু আগে কোনো ইঙ্গিত ছিল না কেন, তাই টাকা "হারিয়ে
                            // যাওয়ার" মতো মনে হতো। এখন কোন role-এর ব্যালেন্স দেখানো হচ্ছে সেটা স্পষ্ট করে
                            // দেখানো হচ্ছে।
                            Text(
                                text = if (currentUser?.role.equals("SOLVER", ignoreCase = true))
                                    "সমাধানকারী হিসেবে আপনার আয়ের ব্যালেন্স"
                                else
                                    "ইউজার হিসেবে আপনার ব্যালেন্স",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // সেশন ২.৩ — শুধু balance amount-টাই pulse করে, উপরের লেবেল/আইকন
                            // আর নিচের রিচার্জ/টাকা-তোলা বাটন (এই Card-এরই অংশ) কখনো pulse
                            // করে না — literal spec অনুযায়ী শুধু "balance amount"।
                            PulsingValue(isUpdating = balancePulse) {
                                Text(
                                    text = Formatters.formatTaka(currentBalance),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "সর্বনিম্ন উত্তোলনের পরিমাণ ${minWithdrawBengali} টাকা",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showAddMoneyDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SomadhanOrange,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wallet_add_money_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "রিচার্জ করুন",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { onNavigate?.invoke(Screen.UserWithdraw.route) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1D4ED8)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("toggle_withdraw_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "টাকা তুলুন",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ধাপ ৮ — outbox pending sync ইন্ডিকেটর (non-blocking, খালি থাকলে কিছুই দেখায় না)
            OutboxPendingIndicator(
                pendingCount = outboxPendingCount,
                onRetryClick = { viewModel.retryOutboxSyncNow() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Escrow Section (Funds Held in Escrow)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, Color(0xFFFDE68A), RoundedCornerShape(16.dp))
            ) {
                // সেশন ২.৩ — ব্যবহারকারীর সিদ্ধান্ত: এসক্রো সেকশন balance-এর মতোই একটা
                // dynamic ভ্যালু, তাই পুরো সেকশন (হেডার/ব্যাজ/টোটাল/লিস্ট একসাথে) pulse করে —
                // কার্ডের ব্যাকগ্রাউন্ড/বর্ডার (উপরে, এই Card()-এর নিজের) কাঠামো হিসেবে স্থির।
                PulsingValue(isUpdating = escrowPulse) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Escrow Header
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7))
                                    .border(1.dp, Color(0xFFFCD34D), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "এসক্রোতে রক্ষিত টাকা",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                    if (heldEscrows.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFD97706))
                                                .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                        ) {
                                            Text(
                                                text = "${DistanceUtil.toBengaliDigits(heldEscrows.size.toString())}টি সক্রিয়",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "কাজ সম্পন্ন ও নিশ্চিত হলে অর্থ ছাড় হবে",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(0.8.dp, Color(0xFFFDE68A), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            // সেশন ২.৩ — এই Text-টা উপরের escrowPulse-চালিত PulsingValue-এর
                            // ভেতরেই আছে (পুরো এসক্রো সেকশন একসাথে pulse করে), তাই এখানে আলাদা
                            // pulse দরকার নেই।
                            Text(
                                text = Formatters.formatTaka(totalHeldEscrowAmount),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }

                    if (heldEscrows.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFFDE68A).copy(alpha = 0.8f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        val escrowInfiniteTransition = rememberInfiniteTransition(label = "escrowSignalPulse")
                        val signalAlpha by escrowInfiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1100, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "signalAlpha"
                        )
                        val signalGlowScale by escrowInfiniteTransition.animateFloat(
                            initialValue = 0.75f,
                            targetValue = 1.4f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1100, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "signalGlowScale"
                        )

                        heldEscrows.forEach { escrow ->
                            val totalAmount = escrow.baseAmount + escrow.extraAmount
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(1.dp, Color(0xFFFDE68A).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (escrow.problemId.isNotBlank()) {
                                            if (onProblemClick != null) {
                                                onProblemClick(escrow.problemId)
                                            } else if (onNavigate != null) {
                                                onNavigate("${Screen.ProblemDetail.route}/${escrow.problemId}")
                                            }
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Row 1: Problem Title (Single Line with Ellipsis) & Amount
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Assignment,
                                                contentDescription = null,
                                                tint = SomadhanOrange,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val displayTitle = if (escrow.problemTitle.isNotBlank()) {
                                                escrow.problemTitle
                                            } else {
                                                val problem = allProblems.find { it.id == escrow.problemId }
                                                problem?.title?.ifBlank { null } ?: "চলমান কাজের পেমেন্ট"
                                            }
                                            Text(
                                                text = displayTitle,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = Formatters.formatTaka(totalAmount),
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Row 2: Escrow ID (Click to copy) & Status Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Escrow ID chip (Click to copy)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFFEF3C7))
                                                .border(0.6.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Escrow ID", escrow.id))
                                                    Toast.makeText(context, "এসক্রো ID কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color(0xFF92400E),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.5.dp))
                                            Text(
                                                text = "এসক্রো ID: #${escrow.id.take(8).uppercase()}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                            Spacer(modifier = Modifier.width(3.5.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "কপি করুন",
                                                tint = Color(0xFF92400E),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }

                                        // Status badge with Pulsing Green Live Signal Light
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFDCFCE7))
                                                .border(0.8.dp, Color(0xFF86EFAC), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Pulsing Green Live Signal Light (Active Escrow Indicator)
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.size(11.dp)
                                                ) {
                                                    // Glowing pulsing outer ring
                                                    Box(
                                                        modifier = Modifier
                                                            .size((11 * signalGlowScale).dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF22C55E).copy(alpha = (signalAlpha * 0.45f).coerceIn(0.1f, 0.45f)))
                                                    )
                                                    // Core live green signal LED
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF16A34A).copy(alpha = signalAlpha.coerceIn(0.35f, 1.0f)))
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.5.dp))
                                                Text(
                                                    text = "সুরক্ষিত (HELD)",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF15803D)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 0.8.dp)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Row 3: Timestamp and View Details Action
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "হোল্ড সময়: ${Formatters.formatDateTimeBengali(escrow.createdAt)}",
                                            fontSize = 10.5.sp,
                                            color = SomadhanTextHint
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "বিস্তারিত দেখুন",
                                                fontSize = 11.sp,
                                                color = Color(0xFFD97706),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "বর্তমানে কোনো টাকা এসক্রোতে আটকে নেই। আপনার সকল পেমেন্ট নিরাপদ।",
                                fontSize = 11.5.sp,
                                color = Color(0xFF92400E).copy(alpha = 0.85f)
                            )
                        }
                    }
                }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Transaction History (2-Tab View: App Transactions & Gateway Transactions)
            var selectedHistoryTab by rememberSaveable { mutableStateOf("APP") } // "APP" or "GATEWAY"
            var selectedAppSubFilter by rememberSaveable { mutableStateOf("ALL") } // "ALL", "PAYMENT", "DEPOSIT", "REFUND"
            val currentUid = currentUser?.id ?: ""
            val isDepositTrx = { trx: com.example.data.entity.TransactionEntity ->
                trx.type.equals("WALLET_DEPOSIT", ignoreCase = true) || trx.type.equals("DEPOSIT", ignoreCase = true) || trx.id.contains("DEP", ignoreCase = true)
            }
            val isRefundTrx = { trx: com.example.data.entity.TransactionEntity ->
                trx.type.equals("REFUND", ignoreCase = true) || trx.id.contains("REFUND", ignoreCase = true)
            }
            val isPaymentTrx = { trx: com.example.data.entity.TransactionEntity ->
                !isDepositTrx(trx) && !isRefundTrx(trx)
            }

            // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ৫] আগে শুধু identity ম্যাচ হতো
            // (userId/solverId == currentUid), বর্তমান active role (isSolver, উপরে লাইন ২১৭-এ
            // ইতিমধ্যেই গণনা করা) দেখা হতো না -- ফলে এই ওয়ালেট স্ক্রিন (User ও Solver দুই
            // মোডেই দেখানো হয়) অন্য role-এর transaction-ও দেখিয়ে ফেলত
            // (TransactionHistoryScreen-এ একই বাগ ছিল, একই নিয়মে ঠিক করা হলো --
            // TransactionHelper.matchesRoleForHistory()-এর doc comment দেখুন)।
            val activeRole = if (isSolver) "SOLVER" else "USER"
            val sortedUserTransactions = remember(userTransactions, currentUid, activeRole) {
                userTransactions
                    .distinctBy { it.id }
                    .filter { trx ->
                        TransactionHelper.matchesRoleForHistory(trx, activeRole, currentUid)
                    }
                    .sortedByDescending { it.timestamp }
            }
            val sortedGatewayPayments = remember(userGatewayPayments) {
                userGatewayPayments.sortedByDescending { it.timestamp }
            }
            // sortedUserTransactions এখন ইতিমধ্যেই active role-এ স্কোপড, তাই এখানে আর আলাদা করে
            // "it.userId == currentUid" চেক লাগে না (TransactionHistoryScreen-এর মতোই)।
            val filteredUserTransactions = remember(sortedUserTransactions, selectedAppSubFilter) {
                when (selectedAppSubFilter) {
                    "PAYMENT" -> sortedUserTransactions.filter { isPaymentTrx(it) }
                    "DEPOSIT" -> sortedUserTransactions.filter { isDepositTrx(it) }
                    "REFUND" -> sortedUserTransactions.filter { isRefundTrx(it) }
                    else -> sortedUserTransactions
                }
            }
            val userPaymentCount = remember(sortedUserTransactions) {
                sortedUserTransactions.count { isPaymentTrx(it) }
            }
            val userDepositCount = remember(sortedUserTransactions) {
                sortedUserTransactions.count { isDepositTrx(it) }
            }
            val userRefundCount = remember(sortedUserTransactions) {
                sortedUserTransactions.count { isRefundTrx(it) }
            }

            val totalCountDisplay = when (selectedHistoryTab) {
                "GATEWAY" -> sortedGatewayPayments.size
                else -> filteredUserTransactions.size
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "লেনদেনের ইতিহাস (${DistanceUtil.toBengaliDigits(totalCountDisplay.toString())})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                val showSeeMore = if (selectedHistoryTab == "APP") sortedUserTransactions.size > 2 else sortedGatewayPayments.size > 2
                if (showSeeMore) {
                    TextButton(
                        onClick = { onNavigate?.invoke(Screen.TransactionHistory.route) },
                        modifier = Modifier.testTag("see_more_user_transactions_btn")
                    ) {
                        Text(
                            text = "আরও দেখুন (See More)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        .background(if (selectedHistoryTab == "APP") Color.White else Color.Transparent)
                        .border(
                            width = if (selectedHistoryTab == "APP") 1.dp else 0.dp,
                            color = if (selectedHistoryTab == "APP") SomadhanOrange.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedHistoryTab = "APP" }
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
                            tint = if (selectedHistoryTab == "APP") SomadhanOrange else SomadhanTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "অ্যাপের লেনদেন (${DistanceUtil.toBengaliDigits(sortedUserTransactions.size.toString())})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedHistoryTab == "APP") FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedHistoryTab == "APP") SomadhanOrange else SomadhanTextSecondary
                        )
                    }
                }

                // Tab 2: Gateway Transactions
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedHistoryTab == "GATEWAY") Color.White else Color.Transparent)
                        .border(
                            width = if (selectedHistoryTab == "GATEWAY") 1.dp else 0.dp,
                            color = if (selectedHistoryTab == "GATEWAY") Color(0xFFE2136E).copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedHistoryTab = "GATEWAY" }
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
                            tint = if (selectedHistoryTab == "GATEWAY") Color(0xFFE2136E) else SomadhanTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "গেটওয়ে লেনদেন (${DistanceUtil.toBengaliDigits(sortedGatewayPayments.size.toString())})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedHistoryTab == "GATEWAY") FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedHistoryTab == "GATEWAY") Color(0xFFE2136E) else SomadhanTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // সেশন ২.৩ (আইটেম ৩) — শুধু নিচের list body pulse করে, উপরের ট্যাব-সিলেক্টর
            // Row (APP/GATEWAY বাটন, count-সহ) সবসময় স্থির থাকে। selectedHistoryTab
            // pulse-key-এর অংশ বলে ট্যাব পাল্টালেই (ডেটা সত্যিই বদলেছে কিনা তা না দেখেই,
            // ঠিক pull-to-refresh-এর মতো) এই body-টুকু সংক্ষিপ্ত pulse দেখায়, তারপর নতুন
            // ট্যাবের লিস্ট দেখায়।
            val transactionListPulse = rememberFieldChangePulse(
                value = if (selectedHistoryTab == "GATEWAY") {
                    Pair(selectedHistoryTab, sortedGatewayPayments)
                } else {
                    Triple(selectedHistoryTab, selectedAppSubFilter, filteredUserTransactions)
                },
                isManualRefreshing = isRefreshing,
                sessionKey = "user_wallet_sync",
                viewModel = viewModel
            )
            PulsingValue(isUpdating = transactionListPulse) {
            // বাগ-ফিক্স: PulsingValue এর ভেতরের Box ডিফল্টভাবে তার একাধিক direct child-কে
            // একে অপরের উপর overlap করে বসায় (Box টপ-স্ট্যাক আচরণ) — এখানে GATEWAY/APP দুই
            // ব্রাঞ্চেই forEach দিয়ে একাধিক Card (২টা করে, .take(2)) সরাসরি এই লাম্বডার child
            // হিসেবে emit হতো, তাই কার্ডগুলো একটার উপর আরেকটা উঠে বসে যাচ্ছিলো (Column ছাড়া
            // vertical stacking হয় না)। একটা Column দিয়ে wrap করে ঠিক করা হলো, যাতে প্রতিটা
            // ট্যাবে ২টা transaction card record ঠিকভাবে আলাদা আলাদা সারিতে দেখায়।
            Column(modifier = Modifier.fillMaxWidth()) {
            if (selectedHistoryTab == "GATEWAY") {
                if (sortedGatewayPayments.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = SomadhanTextHint,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "এখনও কোনো গেটওয়ে লেনদেনের তথ্য নেই।",
                                    fontSize = 13.sp,
                                    color = SomadhanTextHint
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "বিকাশ, নগদ বা রকেট দিয়ে রিচার্জ বা পেমেন্ট করলে এখানে সংরক্ষিত থাকবে।",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    }
                } else {
                    sortedGatewayPayments.take(2).forEach { gw ->
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
                        val gatewayDisplayName = when (gw.gateway.uppercase()) {
                            "BKASH" -> "bKash (বিকাশ)"
                            "NAGAD" -> "Nagad (নগদ)"
                            "ROCKET" -> "Rocket (রকেট)"
                            else -> gw.gateway
                        }
                        val purposeText = when (gw.purpose) {
                            "WALLET_DEPOSIT" -> "ওয়ালেট রিচার্জ (টপ-আপ)"
                            "ESCROW_PAYMENT" -> "বিড পেমেন্ট (এসক্রো)"
                            "ADDITIONAL_CHARGE" -> "অতিরিক্ত বিল পেমেন্ট"
                            "RELEASE_EXTRA" -> "রিলিজ অতিরিক্ত বিল"
                            "DIRECT_PAYMENT" -> "সরাসরি চুক্তি পেমেন্ট"
                            else -> gw.purpose
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, brandColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .testTag("user_gateway_card_${gw.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
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
                                                .border(1.dp, brandColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = gatewayDisplayName,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = brandColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFF1F5F9))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = purposeText,
                                                fontSize = 10.5.sp,
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
                                             val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                             clipboard.setPrimaryClip(ClipData.newPlainText("TrxID", gw.gatewayTrxId))
                                             Toast.makeText(context, "ট্রানজেকশন আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            text = "TrxID: ${gw.gatewayTrxId}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = brandColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "কপি করুন",
                                            tint = brandColor,
                                            modifier = Modifier.size(11.dp)
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
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Post ID", gw.problemId))
                                                    Toast.makeText(context, "পোস্ট আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Assignment, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(12.dp))
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
                                        modifier = Modifier.clickable { selectedGatewayDetail = gw }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SomadhanSuccess,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "সফল • রসিদ দেখুন",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SomadhanSuccess
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Sub-tabs for App transactions: "সব", "পেমেন্ট", "রিচার্জ", "রিফান্ড"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedAppSubFilter == "ALL",
                        onClick = { selectedAppSubFilter = "ALL" },
                        label = {
                            Text(
                                text = "সব (${DistanceUtil.toBengaliDigits(sortedUserTransactions.size.toString())})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedAppSubFilter == "ALL") FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SomadhanOrange,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("user_wallet_tab_all")
                    )
                    FilterChip(
                        selected = selectedAppSubFilter == "PAYMENT",
                        onClick = { selectedAppSubFilter = "PAYMENT" },
                        label = {
                            Text(
                                text = "পেমেন্ট (${DistanceUtil.toBengaliDigits(userPaymentCount.toString())})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedAppSubFilter == "PAYMENT") FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SomadhanOrange,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("user_wallet_tab_payment")
                    )
                    FilterChip(
                        selected = selectedAppSubFilter == "DEPOSIT",
                        onClick = { selectedAppSubFilter = "DEPOSIT" },
                        label = {
                            Text(
                                text = "রিচার্জ (${DistanceUtil.toBengaliDigits(userDepositCount.toString())})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedAppSubFilter == "DEPOSIT") FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SomadhanOrange,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("user_wallet_tab_deposit")
                    )
                    FilterChip(
                        selected = selectedAppSubFilter == "REFUND",
                        onClick = { selectedAppSubFilter = "REFUND" },
                        label = {
                            Text(
                                text = "রিফান্ড (${DistanceUtil.toBengaliDigits(userRefundCount.toString())})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedAppSubFilter == "REFUND") FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SomadhanOrange,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("user_wallet_tab_refund")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredUserTransactions.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = when (selectedAppSubFilter) {
                                    "PAYMENT" -> "কোনো পেমেন্টের তথ্য নেই।"
                                    "DEPOSIT" -> "কোনো রিচার্জের তথ্য নেই।"
                                    "REFUND" -> "কোনো রিফান্ডের তথ্য নেই।"
                                    else -> "এখনও কোনো অ্যাপ লেনদেনের তথ্য নেই।"
                                },
                                fontSize = 13.sp,
                                color = SomadhanTextHint
                            )
                        }
                    }
                } else {
                    filteredUserTransactions.take(2).forEach { trx ->
                    val isDeposit = isDepositTrx(trx)
                    val isRefund = isRefundTrx(trx)
                    val isEarning = (currentUser?.id == trx.solverId && !isRefund && !isDeposit)
                    val isUserRefund = (currentUser?.id == trx.userId && isRefund)
                    val isUserDeposit = (currentUser?.id == trx.userId && isDeposit)
                    // [Step 15.3 — CI_TEST_SUITE_MASTER_PROMPT.md] Bug fix: same bug-class as
                    // TransactionHistoryScreen.kt (Step 15.2) — this was a byte-for-byte duplicate
                    // of that screen's old `isEarning || isUserRefund || isUserDeposit` derivation,
                    // which silently defaulted to `false` (always "−") for ADMIN_ADJUSTMENT and any
                    // other type these three checks don't enumerate. Reusing the same pure,
                    // type-agnostic `transactionDisplaySign()` (declared top-level in
                    // TransactionHistoryScreen.kt, same package) instead of re-deriving it here —
                    // `isEarning`/`isUserRefund`/`isUserDeposit`/`isDeposit`/`isRefund` are kept
                    // unchanged since they're still used below (label/color/copy text).
                    val isPositive = transactionDisplaySign(trx, currentUid)
                    val displayTitle = when {
                        isDeposit -> trx.problemTitle.ifBlank { "ওয়ালেট রিচার্জ (টপ-আপ)" }
                        isRefund -> if (trx.problemTitle.isNotBlank()) "রিফান্ড: ${trx.problemTitle}" else "বাতিল কাজের রিফান্ড"
                        else -> if (trx.problemTitle.isNotBlank()) "সমস্যা: ${trx.problemTitle}" else "সমস্যা সমাধান লেনদেন"
                    }

                    val escrow = userEscrows.find { it.problemId == trx.problemId }
                    val problem = allProblems.find { it.id == trx.problemId }
                    val acceptedExtraCharges = allAdditionalCharges.filter { it.problemId == trx.problemId && it.status == "ACCEPTED" }.sumOf { it.amount }

                    val extraAmt = if (escrow != null && escrow.extraAmount > 0.0) {
                        escrow.extraAmount
                    } else if (acceptedExtraCharges > 0.0) {
                        acceptedExtraCharges
                    } else if (problem != null && problem.releaseRequestExtraAmount > 0.0) {
                        problem.releaseRequestExtraAmount
                    } else {
                        0.0
                    }

                    val baseBidAmt = if (escrow != null && escrow.baseAmount > 0.0) {
                        escrow.baseAmount
                    } else if (problem?.acceptedAmount != null && problem.acceptedAmount > 0.0) {
                        problem.acceptedAmount
                    } else {
                        (trx.grossAmount - extraAmt).coerceAtLeast(0.0)
                    }

                    val finalExtra = if (extraAmt > 0.0 && baseBidAmt + extraAmt == trx.grossAmount) {
                        extraAmt
                    } else if (trx.grossAmount > baseBidAmt && baseBidAmt > 0.0) {
                        (trx.grossAmount - baseBidAmt)
                    } else {
                        extraAmt
                    }
                    val finalBid = (trx.grossAmount - finalExtra).coerceAtLeast(0.0)
                    val effectiveEscrowId = when {
                        trx.escrowId.isNotBlank() -> trx.escrowId
                        escrow != null && escrow.id.isNotBlank() -> escrow.id
                        trx.problemId.isNotBlank() -> "ESC_${trx.problemId.take(8).uppercase()}"
                        else -> "ESC_${trx.id.take(8).uppercase()}"
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
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
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Trx ID", trx.id))
                                            val msg = when {
                                                isDeposit -> "রিচার্জ আইডি কপি করা হয়েছে"
                                                isRefund -> "রিফান্ড আইডি কপি করা হয়েছে"
                                                else -> "লেনদেন আইডি কপি করা হয়েছে"
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            text = when {
                                                isDeposit -> "রিচার্জ: ${trx.id.take(12)}"
                                                isRefund -> "রিফান্ড: ${trx.id.take(12)}"
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
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isPositive) "+${Formatters.formatTaka(if (isEarning) trx.netAmount else trx.grossAmount)}" else "-${Formatters.formatTaka(trx.grossAmount)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPositive) Color(0xFF16A34A) else Color(0xFFDC2626)
                                    )
                                    Text(
                                        text = Formatters.formatDateTimeBengali(trx.timestamp),
                                        fontSize = 10.sp,
                                        color = SomadhanTextHint
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

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
                                            text = "লেনদেনের ধরন: ওয়ালেট ব্যালেন্স রিচার্জ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SomadhanTextPrimary
                                        )
                                        Text(
                                            text = "স্ট্যাটাস: সফলভাবে ওয়ালেটে যোগ হয়েছে ✅",
                                            fontSize = 11.sp,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "জমা: +৳${DistanceUtil.toBengaliDigits(trx.grossAmount)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                }
                            } else {
                                // Breakdown container with Bid Amount, Extra Amount, and Total Amount
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
                                            text = if (isRefund) "মোট রিফান্ড: ৳${DistanceUtil.toBengaliDigits(trx.grossAmount)}" else "সর্বমোট লেনদেন: ৳${DistanceUtil.toBengaliDigits(trx.grossAmount)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPositive) Color(0xFF16A34A) else Color(0xFF1E293B)
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
                                                        selectedProblemGatewaysTrxAmount = trx.grossAmount
                                                        selectedProblemGatewaysIsExtraCharge = trx.type.equals("EXTRA_CHARGE_DEDUCTION", ignoreCase = true)
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
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Escrow ID", effectiveEscrowId))
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
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Post ID", trx.problemId))
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
            }
            }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
        }
    }
    }

    // Add Money BottomSheet (Gateway Top-up)
    if (showAddMoneyDialog) {
        var depositError by remember { mutableStateOf<String?>(null) }
        val addMoneySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

        ModalBottomSheet(
            onDismissRequest = {
                showAddMoneyDialog = false
                depositError = null
            },
            sheetState = addMoneySheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header with icon, title and Close (✕) icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SomadhanOrange.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ওয়ালেট রিচার্জ করুন",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = "বিকাশ, নগদ বা রকেট গেটওয়ে",
                                fontSize = 11.5.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            showAddMoneyDialog = false
                            depositError = null
                        },
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "টপ-আপ পরিমাণ নির্বাচন করুন অথবা নিচে লিখুন:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SomadhanTextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Preset Amount Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100.0, 500.0, 1000.0, 2000.0).forEach { presetAmt ->
                        val isSelected = addMoneyAmountStr == presetAmt.toInt().toString()
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                addMoneyAmountStr = presetAmt.toInt().toString()
                                depositError = null
                            },
                            label = {
                                Text(
                                    text = "৳${DistanceUtil.toBengaliDigits(presetAmt.toInt().toString())}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SomadhanOrange,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = addMoneyAmountStr,
                    onValueChange = {
                        addMoneyAmountStr = it.filter { ch -> ch.isDigit() || ch == '.' }
                        depositError = null
                    },
                    label = { Text("রিচার্জের পরিমাণ (টাকা)") },
                    placeholder = { Text("যেমন: 500") },
                    leadingIcon = {
                        Text(
                            text = "৳",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanOrange,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        focusedLabelColor = SomadhanOrange,
                        cursorColor = SomadhanOrange
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_money_amount_input")
                )

                if (depositError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = depositError ?: "",
                        fontSize = 12.sp,
                        color = SomadhanError
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val amt = addMoneyAmountStr.toDoubleOrNull()
                        if (amt == null || amt < 10) {
                            depositError = "অনুগ্রহ করে সর্বনিম্ন ১০ টাকা বা তার বেশি পরিমাণ লিখুন।"
                            return@Button
                        }
                        showAddMoneyDialog = false
                        depositError = null
                        addMoneyPayableAmount = amt
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_proceed_to_payment_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "পেমেন্ট গেটওয়েতে যান",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Merchant Payment Gateway Dialog for Wallet Recharge
    if (addMoneyPayableAmount != null) {
        val payableAmt = addMoneyPayableAmount!!
        MerchantPaymentDialog(
            amount = payableAmt,
            problemTitle = "ওয়ালেট রিচার্জ (Top-up)",
            solverName = "সমাধান অনলাইন ওয়ালেট",
            onPaymentSuccess = {
                addMoneyPayableAmount = null
                addMoneyAmountStr = ""
            },
            onDismissRequest = {
                addMoneyPayableAmount = null
            },
            onPaymentCompleteWithDetails = { gateway, trxId, phone ->
                val amountToDeposit = payableAmt
                addMoneyPayableAmount = null
                addMoneyAmountStr = ""
                depositGatewayError = null
                depositGatewayPendingDetails = DepositGatewayPendingDetails(
                    amount = amountToDeposit,
                    gateway = gateway,
                    gatewayTrxId = trxId,
                    senderPhone = phone
                )
                viewModel.depositMoneyViaGateway(
                    amount = amountToDeposit,
                    gateway = gateway,
                    gatewayTrxId = trxId,
                    senderPhone = phone,
                    note = "ওয়ালেট রিচার্জ (Top-up)",
                    onSuccess = {
                        // The user balance, transactions, and gateway payments are reactively updated via StateFlow and Room Flow without page reloading
                        depositGatewayPendingDetails = null
                    },
                    onError = { err ->
                        depositGatewayError = err
                    }
                )
            }
        )
    }

    // Post-gateway-payment verification overlay for depositMoneyViaGateway (section 3 pattern):
    // MerchantPaymentDialog already closed itself by the time this shows, but we keep the
    // user informed and locked out until the real viewModel.depositMoneyViaGateway() call resolves.
    if (depositGatewayPendingDetails != null) {
        val pendingDeposit = depositGatewayPendingDetails!!
        BottomSlideAlertDialog(
            onDismissRequest = { /* locked until success or explicit dismiss-on-error below */ },
            title = { Text("লেনদেন যাচাই হচ্ছে", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary) },
            text = {
                Column {
                    if (depositGatewayError == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = SomadhanOrange, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("লেনদেন যাচাই হচ্ছে...", fontSize = 13.sp, color = SomadhanTextPrimary)
                        }
                    } else {
                        Text(
                            text = depositGatewayError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError
                        )
                    }
                }
            },
            confirmButton = {
                if (depositGatewayError != null) {
                    Button(onClick = {
                        depositGatewayError = null
                        viewModel.depositMoneyViaGateway(
                            amount = pendingDeposit.amount,
                            gateway = pendingDeposit.gateway,
                            gatewayTrxId = pendingDeposit.gatewayTrxId,
                            senderPhone = pendingDeposit.senderPhone,
                            note = "ওয়ালেট রিচার্জ (Top-up)",
                            onSuccess = {
                                depositGatewayPendingDetails = null
                            },
                            onError = { err ->
                                depositGatewayError = err
                            }
                        )
                    }) { Text("আবার চেষ্টা করুন") }
                }
            },
            dismissButton = {
                if (depositGatewayError != null) {
                    TextButton(onClick = {
                        depositGatewayPendingDetails = null
                        depositGatewayError = null
                    }) { Text("বাতিল", color = SomadhanTextSecondary) }
                }
            }
        )
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
            "WALLET_DEPOSIT" -> "ওয়ালেট রিচার্জ (ব্যালেন্স যুক্ত)"
            "ESCROW_PAYMENT" -> "কাজের এসক্রো পেমেন্ট"
            "ADDITIONAL_CHARGE" -> "অতিরিক্ত বিল পেমেন্ট"
            "RELEASE_EXTRA" -> "রিলিজ অতিরিক্ত বিল"
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
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Post ID", item.problemId))
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
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("TrxID", item.gatewayTrxId))
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
                                text = "প্রেরক অ্যাকাউন্ট: ${item.userPhone}",
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
        // Wallet+Gateway split: only relevant for an "অতিরিক্ত বিল" deduction whose gateway
        // payments alone don't cover the full trx amount -- the rest came from the wallet.
        val extraGatewayTotal = gateways.filter { it.purpose.equals("ADDITIONAL_CHARGE", ignoreCase = true) }.sumOf { it.amount }
        val walletPortion = (selectedProblemGatewaysTrxAmount - extraGatewayTotal).coerceAtLeast(0.0)
        val showWalletSplit = selectedProblemGatewaysIsExtraCharge && walletPortion > 0.5

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
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Post ID", selectedProblemGatewaysId))
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

                // Summary Card Banner: a compact wallet+gateway breakdown when this "অতিরিক্ত
                // বিল" trx was split across both sources, otherwise the usual gateway-only total.
                if (showWalletSplit) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = "উৎস অনুযায়ী পেমেন্ট বিভাজন",
                                fontSize = 10.5.sp,
                                color = SomadhanTextHint
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(text = "ওয়ালেট থেকে", fontSize = 10.5.sp, color = SomadhanTextSecondary)
                                    Text(
                                        text = Formatters.formatTaka(walletPortion),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(28.dp)
                                        .background(Color(0xFFBBF7D0))
                                )
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(text = "গেটওয়ে থেকে", fontSize = 10.5.sp, color = SomadhanTextSecondary)
                                    Text(
                                        text = Formatters.formatTaka(extraGatewayTotal),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrange
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(28.dp)
                                        .background(Color(0xFFBBF7D0))
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "সর্বমোট", fontSize = 10.5.sp, color = SomadhanTextSecondary)
                                    Text(
                                        text = Formatters.formatTaka(walletPortion + extraGatewayTotal),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                }
                            }
                        }
                    }
                } else {
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
                            else -> SomadhanOrangeLight
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
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Gateway TrxID", gw.gatewayTrxId))
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
