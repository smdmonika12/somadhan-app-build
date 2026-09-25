package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.WithdrawalEntity
import com.example.ui.components.OutboxPendingIndicator
import com.example.ui.components.PulsingValue
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.StatusBadge
import com.example.ui.components.SyncAwareContent
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalHistoryScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSupport: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val withdrawals by viewModel.solverWithdrawals.collectAsStateWithLifecycle()

    // ধাপ ৮ (RPC_SYNC_FIX ট্র্যাক) — outbox pending sync ইন্ডিকেটর (requestWithdrawal সহ
    // withdrawal-সংক্রান্ত outbox-wired ফাংশনগুলোর জন্য)।
    val outboxPendingCount by viewModel.outboxPendingCount.collectAsStateWithLifecycle()
    var selectedWithdrawalDetail by remember { mutableStateOf<WithdrawalEntity?>(null) }
    val accentColor = if (currentUser?.role == "SOLVER") SomadhanOrange else Color(0xFF1D4ED8)

    // Loading/Sync Fix Roadmap v2, ধাপ ৪ — withdrawals টেবিল initialSyncPhase-এর (bulk-pull)
    // অংশ, আলাদা sessionKey "withdrawal_history_sync" (বিদ্যমান "withdrawal_history" key-র
    // সাথে সংঘর্ষ এড়াতে, MessagesScreen-এর ধাপ ৪-এর মতোই কারণ)।
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    // Loading/Sync Fix Roadmap v2, ধাপ ৬ — এই স্ক্রিনে আগে pull-to-refresh ছিল না, তাই বাকি
    // ১৬টা স্ক্রিনের একই প্যাটার্নে (SomadhanPullToRefresh → viewModel.refreshData(), যেটা
    // ধাপ ৬-এই ERROR হলে retryInitialSync() ট্রিগার করে) যোগ করা হলো।
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser?.id) {
        viewModel.resetWithdrawalsPagination(currentUser?.id)
    }

    val withdrawalsPaged = viewModel.withdrawalsPaged
    val totalItems = if (withdrawals.isNotEmpty()) withdrawals.size else withdrawalsPaged.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "সকল উত্তোলনের ইতিহাস",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SomadhanTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("withdrawal_history_back_btn")
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
        // সেশন ২.৫ (batch31) — আগে এখানে SyncAwareRefreshableContent (data = withdrawalsSnapshot)
        // ব্যবহার হতো, যেটা withdrawalsPaged বদলালেই পুরো LazyColumn (summary stats card + লিস্ট,
        // সবটা) একসাথে re-flash করতো — Wallet/TransactionHistory-তে পাওয়া একই ক্লাসের বাগ। এখন
        // cold-load-only SyncAwareContent-এ আনা হলো (একবার LOADED হলে content() আর নিজে থেকে
        // re-animate করে না)। নিচের withdrawalListPulse (summary card + লিস্ট, দুটো একসাথে —
        // TransactionHistoryScreen-এর একই সিদ্ধান্ত, কারণ সামারির "মোট লোড হয়েছে" সংখ্যাও ডেটার
        // সাথেই বদলায়) নিজস্ব rememberFieldChangePulse + PulsingValue দিয়ে আলাদাভাবে pulse করে।
        val withdrawalsList = withdrawalsPaged.toList()
        val withdrawalListPulse = rememberFieldChangePulse(
            value = withdrawalsList,
            isManualRefreshing = isRefreshing,
            sessionKey = "withdrawal_history_sync",
            viewModel = viewModel
        )

        SyncAwareContent(
            sessionKey = "withdrawal_history_sync",
            viewModel = viewModel,
            syncPhase = initialSyncPhase,
            onRetry = { viewModel.retryInitialSync() },
            modifier = Modifier.fillMaxSize()
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))

                // ধাপ ৮ — outbox pending sync ইন্ডিকেটর (non-blocking, খালি থাকলে কিছুই
                // দেখায় না)
                OutboxPendingIndicator(
                    pendingCount = outboxPendingCount,
                    onRetryClick = { viewModel.retryOutboxSyncNow() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Summary Stats Card — সেশন ২.৫: নিচের লিস্টের সাথেই pulse করে
                // (withdrawalListPulse), যেহেতু "মোট লোড হয়েছে" সংখ্যা লিস্টের সাথেই বদলায়।
                PulsingValue(isUpdating = withdrawalListPulse) {
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
                                text = "মোট অনুরোধ",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = "${DistanceUtil.toBengaliDigits(totalItems.toString())} টি",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "মোট লোড হয়েছে",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = "${DistanceUtil.toBengaliDigits(withdrawalsList.size.toString())} টি",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }
                    }
                }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (withdrawalsList.isEmpty() && !viewModel.withdrawalsLoadingMore) {
                item {
                    PulsingValue(isUpdating = withdrawalListPulse) {
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
                                text = "এখনও কোনো উইথড্রয়াল রিকোয়েস্ট নেই।",
                                fontSize = 14.sp,
                                color = SomadhanTextHint
                            )
                        }
                    }
                    }
                }
            } else {
                items(withdrawalsList, key = { it.id }) { item ->
                    PulsingValue(isUpdating = withdrawalListPulse) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedWithdrawalDetail = item
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Formatters.formatTaka(item.amount),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                                StatusBadge(status = item.status)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "উইথড্র আইডি: ${item.id}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Withdraw ID", item.id))
                                        Toast.makeText(context, "উইথড্র আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Withdraw ID",
                                        modifier = Modifier.size(12.dp),
                                        tint = accentColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "মাধ্যম: ${item.method} (${item.accountNumber})",
                                fontSize = 13.sp,
                                color = SomadhanTextSecondary
                            )
                            if (item.bankName != null) {
                                Text(
                                    text = "ব্যাংক: ${item.bankName}, শাখা: ${item.branchName ?: ""}",
                                    fontSize = 11.sp,
                                    color = SomadhanTextHint
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Formatters.formatDateTimeBengali(item.createdAt),
                                    fontSize = 11.sp,
                                    color = SomadhanTextHint
                                )
                                Text(
                                    text = "বিস্তারিত দেখতে ট্যাপ করুন ›",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (item.status == "REJECTED") SomadhanError else accentColor
                                )
                            }
                        }
                    }
                    }
                }

                // Infinite Scroll Footer
                if (viewModel.withdrawalsHasMore) {
                    item {
                        LaunchedEffect(Unit) {
                            viewModel.loadNextWithdrawalsPage()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.withdrawalsLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = accentColor
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

    // Withdrawal Detail Bottom Sheet (Same as User Withdraw Screen)
    if (selectedWithdrawalDetail != null) {
        com.example.ui.components.WithdrawalDetailBottomSheet(
            withdrawal = selectedWithdrawalDetail!!,
            onDismiss = { selectedWithdrawalDetail = null },
            onNavigateToSupport = onNavigateToSupport,
            accentColor = accentColor
        )
    }
}
