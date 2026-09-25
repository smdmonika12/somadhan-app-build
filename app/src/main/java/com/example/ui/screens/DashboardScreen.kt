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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ProblemEntity
import com.example.data.entity.TransactionEntity
import com.example.ui.components.PulsingValue
import com.example.ui.components.SyncAwareContent
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.components.NotificationBottomSheet
import com.example.ui.components.handleSomadhanNotification
import com.example.ui.components.SomadhanBottomNav
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.SomadhanTopBar
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.navigation.Screen
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.Formatters
import com.example.util.TransactionHelper

@Composable
fun DashboardScreen(
    viewModel: SomadhanViewModel,
    onNavigate: (String) -> Unit,
    onNavigateToCompletedJobs: () -> Unit,
    onNavigateToWithdraw: () -> Unit,
    onAdminClick: () -> Unit,
    onProblemClick: ((String) -> Unit)? = null
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val isLocationUpdating by viewModel.isLocationUpdating.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val allPlatformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()

    // Tracks whether core reference data (platform settings) has arrived at
    // least once. Session-aware: skips straight to ready on revisit this session.
    // Loading/Sync Fix Roadmap v2, dhap 4 -- initialSyncPhase (bulk-pull), separate sessionKey "dashboard_sync"
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    var showNotificationsSheet by remember { mutableStateOf(false) }
    val isSolver = currentUser?.role == "SOLVER"
    val currentUserId = currentUser?.id ?: ""
    val notificationsPaged = viewModel.notificationsPaged
    val notificationsLoadingMore = viewModel.notificationsLoadingMore
    val notificationsHasMore = viewModel.notificationsHasMore

    LaunchedEffect(showNotificationsSheet, currentUserId) {
        if (showNotificationsSheet && currentUserId.isNotBlank()) {
            viewModel.resetNotificationsPagination(currentUserId)
        }
    }

    if (showNotificationsSheet) {
        NotificationBottomSheet(
            notifications = notificationsPaged,
            hasMore = notificationsHasMore,
            loadingMore = notificationsLoadingMore,
            onLoadMore = {
                if (currentUserId.isNotBlank()) {
                    viewModel.loadNextNotificationsPage(currentUserId)
                }
            },
            onDismiss = { showNotificationsSheet = false },
            onMarkAllAsRead = { viewModel.markAllNotificationsRead() },
            onMarkAsRead = { notifId -> viewModel.markNotificationRead(notifId) },
            onNotificationItemClick = { notif ->
                showNotificationsSheet = false
                handleSomadhanNotification(
                    notif = notif,
                    onNavigate = onNavigate,
                    onProblemClick = onProblemClick,
                    isSolver = isSolver
                )
            },
            isSolver = isSolver
        )
    }

    Scaffold(
        topBar = {
            SomadhanTopBar(
                title = if (isSolver) "সলভার ড্যাশবোর্ড" else "ইউজার ড্যাশবোর্ড",
                currentUser = currentUser,
                unreadCount = unreadCount,
                onNotificationClick = { showNotificationsSheet = true },
                onAdminClick = null,
                onReputationClick = {
                    currentUser?.id?.let { uid ->
                        onNavigate(Screen.ReputationDetail.createRoute(uid))
                    }
                },
                locationAddress = liveLocation.address,
                onLocationRefresh = { viewModel.refreshLiveLocation() },
                isLocationUpdating = isLocationUpdating
            )
        },
        bottomBar = {
            val isWalletEnabled = allPlatformSettings.find { it.key == "menu_wallet_enabled" }?.value != "false"
            val isInstantJobEnabled = allPlatformSettings.find { it.key == "instant_job_feature_enabled" }?.value?.toBooleanStrictOrNull() ?: true
            SomadhanBottomNav(
                currentRoute = "dashboard",
                isSolver = isSolver,
                isWalletEnabled = isWalletEnabled,
                isInstantJobEnabled = isInstantJobEnabled,
                onNavigate = onNavigate
            )
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        SomadhanPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SomadhanBg)
        ) {
            // ব্যাচ ৩১, সেশন ২.১ — আগে এখানে (বাইরের স্কোপে) diff-key StateFlow collect করে
            // SyncAwareRefreshableContent-এর কম্পোজিট data-তে পাস করা হতো, যেটা Wallet/
            // ProblemDetail-এ পাওয়া একই ধরনের বাগ তৈরি করছিল: ভেতরের যেকোনো ভ্যালু বদলালেই পুরো
            // sub-composable (SolverDashboardContent/UserDashboardContent, পুরো header+layout-সহ)
            // re-flash হতো, যদিও ব্যবহারকারীর কাঙ্ক্ষিত আচরণ শুধু নির্দিষ্ট কিছু value/list অংশ
            // pulse করা (rule ২/৩)। এখন cold-load-only `SyncAwareContent` ব্যবহার করা হচ্ছে —
            // এটা শুধু প্রথমবার (এই app session-এ) পুরো-পেজ স্কেলিটন/এরর দেখায়, তারপর নিজে থেকে
            // আর কখনো re-animate করে না। re-entry/pull-to-refresh-এ কোন কোন section pulse করবে তা
            // এখন sub-composable দুটোর ভেতরেই `rememberFieldChangePulse` + `PulsingValue` দিয়ে
            // নির্দিষ্টভাবে (scoped) হ্যান্ডেল করা হচ্ছে — তাই sub-composable দুটোকে এখন `isRefreshing`
            // প্যারামিটার পাস করা হচ্ছে (pull-to-refresh শেষে pulse ট্রিগার করার জন্য)।
            if (isSolver) {
                SyncAwareContent(
                    sessionKey = "dashboard_sync",
                    viewModel = viewModel,
                    syncPhase = initialSyncPhase,
                    onRetry = { viewModel.retryInitialSync() }
                ) {
                    SolverDashboardContent(
                        viewModel = viewModel,
                        isRefreshing = isRefreshing,
                        onNavigateToCompletedJobs = onNavigateToCompletedJobs,
                        onNavigateToWithdraw = onNavigateToWithdraw,
                        onNavigateToTransactions = { onNavigate(Screen.TransactionHistory.route) },
                        onProblemClick = { problemId ->
                            if (onProblemClick != null) {
                                onProblemClick(problemId)
                            } else {
                                onNavigate(Screen.ProblemDetail.createRoute(problemId))
                            }
                        }
                    )
                }
            } else {
                SyncAwareContent(
                    sessionKey = "dashboard_sync",
                    viewModel = viewModel,
                    syncPhase = initialSyncPhase,
                    onRetry = { viewModel.retryInitialSync() }
                ) {
                    UserDashboardContent(
                        viewModel = viewModel,
                        isRefreshing = isRefreshing
                    )
                }
            }
        }
    }
}

@Composable
fun UserDashboardContent(viewModel: SomadhanViewModel, isRefreshing: Boolean = false) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()

    val userBlue = Color(0xFF1D4ED8)
    val userBlueLight = Color(0xFFEFF6FF)

    val currentUserId = currentUser?.id ?: ""
    val myProblems = remember(allProblems, currentUserId) {
        if (currentUserId.isBlank()) emptyList()
        else allProblems.filter { it.userId == currentUserId && !it.isUserDeleted }
    }
    val totalPosts = myProblems.size
    val completedProblems = remember(myProblems) { myProblems.filter { it.status == "COMPLETED" } }
    val totalSpent = completedProblems.sumOf { it.acceptedAmount ?: 0.0 }
    val activeProblemsCount = remember(myProblems) {
        myProblems.count {
            (it.status == "OPEN" || it.status == "IN_PROGRESS") &&
            it.status != "COMPLETED" &&
            it.status != "CANCELLED"
        }
    }
    val inProgressCount = activeProblemsCount
    val openCount = remember(myProblems) { myProblems.count { it.status == "OPEN" && it.acceptedBidId == null } }

    // Category breakdown
    val categoryCounts = myProblems.groupBy { it.categoryName }.mapValues { it.value.size }

    // ব্যাচ ৩১, সেশন ২.১ — ব্যবহারকারীর কনফার্মেশন অনুযায়ী: ৪টা stat card + নিচের ক্যাটাগরি-লিস্ট
    // — সবগুলোই ডাইনামিক ভ্যালু, তাই সবগুলোই pulse করবে (শুধু মূল প্রম্পটে বলা ৩টা অংশ না)। Wallet-এর
    // escrow-সিদ্ধান্তের মতোই দুটো আলাদা section (stat cards / category list) নিজস্ব
    // rememberFieldChangePulse দিয়ে আলাদাভাবে pulse করে (re-entry, pull-to-refresh, বা realtime
    // ভ্যালু-বদল — তিন কারণেই)। উপরের পেজ-হেডার ("আপনার পরিসংখ্যান ও সারাংশ") pulse-এর বাইরে, সবসময়
    // স্থির।
    val statsPulse = rememberFieldChangePulse(
        value = listOf(totalPosts, completedProblems.size, inProgressCount, totalSpent),
        isManualRefreshing = isRefreshing,
        sessionKey = "dashboard_sync",
        viewModel = viewModel
    )
    val categoryPulse = rememberFieldChangePulse(
        value = categoryCounts,
        isManualRefreshing = isRefreshing,
        sessionKey = "dashboard_sync",
        viewModel = viewModel
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "আপনার পরিসংখ্যান ও সারাংশ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Stat Cards Grid — ব্যাচ ৩১/২.১ (আপডেট): ব্যবহারকারীর সিদ্ধান্ত অনুযায়ী পুরো কার্ড না,
            // শুধু ভেতরের value-অংশটুকুই pulse করবে (label/subtitle/icon স্থির) —
            // `StatCard`-এর নতুন `isValuePulsing` প্যারামিটার দিয়ে, বাইরের `PulsingValue` wrapper
            // সরিয়ে ফেলা হলো।
            Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "মোট পোস্ট",
                            value = DistanceUtil.toBengaliDigits(totalPosts.toString()),
                            subtitle = "পোস্টকৃত সমস্যা",
                            icon = Icons.Default.PostAdd,
                            iconColor = userBlue,
                            modifier = Modifier.weight(1f),
                            isValuePulsing = statsPulse
                        )

                        StatCard(
                            title = "মোট সম্পন্ন",
                            value = DistanceUtil.toBengaliDigits(completedProblems.size.toString()),
                            subtitle = "সফল সমাধান",
                            icon = Icons.Default.CheckCircle,
                            iconColor = SomadhanSuccess,
                            modifier = Modifier.weight(1f),
                            isValuePulsing = statsPulse
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "চলমান কাজ",
                            value = DistanceUtil.toBengaliDigits(inProgressCount.toString()),
                            subtitle = "প্রক্রিয়াধীন",
                            icon = Icons.Default.Equalizer,
                            iconColor = SomadhanInfo,
                            modifier = Modifier.weight(1f),
                            isValuePulsing = statsPulse
                        )

                        StatCard(
                            title = "মোট ব্যয়",
                            value = Formatters.formatTaka(totalSpent),
                            subtitle = "পরিশোধিত অর্থ",
                            icon = Icons.Default.MonetizationOn,
                            iconColor = userBlue,
                            modifier = Modifier.weight(1f),
                            isValuePulsing = statsPulse
                        )
                    }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Category Distribution / Bar Chart — ব্যাচ ৩১/২.১: পুরো কার্ড (হেডার/লিস্ট একসাথে)
            // pulse করবে, Wallet-এর escrow সেকশনের মতোই একটা dynamic ইউনিট হিসেবে।
            PulsingValue(isUpdating = categoryPulse) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ক্যাটাগরি অনুযায়ী পোস্ট বিভাজন",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (categoryCounts.isEmpty()) {
                            Text(
                                text = "এখনো কোনো পোস্ট করেননি।",
                                fontSize = 13.sp,
                                color = SomadhanTextHint
                            )
                        } else {
                            categoryCounts.forEach { (catName, count) ->
                                val progress = if (totalPosts > 0) count.toFloat() / totalPosts.toFloat() else 0f
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = catName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SomadhanTextPrimary
                                        )
                                        Text(
                                            text = "${DistanceUtil.toBengaliDigits(count.toString())} টি (${(progress * 100).toInt()}%)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = userBlue
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        color = userBlue,
                                        trackColor = userBlueLight,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
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

@Composable
fun SolverDashboardContent(
    viewModel: SomadhanViewModel,
    isRefreshing: Boolean = false,
    onNavigateToCompletedJobs: () -> Unit,
    onNavigateToWithdraw: () -> Unit,
    onNavigateToTransactions: () -> Unit = {},
    onProblemClick: (String) -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val solverBids by viewModel.solverBids.collectAsStateWithLifecycle()
    val completedJobs by viewModel.solverCompletedJobs.collectAsStateWithLifecycle()
    val dismissedReminderIds by viewModel.dismissedReminderIds.collectAsStateWithLifecycle()
    val givenRatings by viewModel.solverGivenReviews.collectAsStateWithLifecycle()
    val allAdditionalCharges by viewModel.allAdditionalCharges.collectAsStateWithLifecycle()
    val solverTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val currentSolverId = currentUser?.id ?: ""

    // Map of problemId -> list of approved extra charges
    val approvedChargesByProblem = remember(allAdditionalCharges) {
        allAdditionalCharges.filter { it.status == "APPROVED" }.groupBy { it.problemId }
    }

    val ratedProblemIds = remember(givenRatings) {
        givenRatings.filter { it.raterRole == "SOLVER" }.map { it.problemId }.toSet()
    }
    val pendingRatingJobs = remember(completedJobs, ratedProblemIds, dismissedReminderIds) {
        completedJobs.filter { job ->
            job.id !in ratedProblemIds && job.id !in dismissedReminderIds
        }.sortedBy { it.completedAt }
    }

    val totalBids = solverBids.size
    val wonBids = solverBids.count { it.status == "ACCEPTED" }
    val winRate = if (totalBids > 0) ((wonBids.toDouble() / totalBids.toDouble()) * 100).toInt() else 0
    // [ব্যালেন্স ফিক্স — ধাপ ১] এটা Solver ড্যাশবোর্ডের "মোট আয়", তাই role-scoped
    // balanceSolver পড়া হচ্ছে, শেয়ার্ড `balance` না (MONEY_FLOW_AND_ADMIN_BUGS_MASTER_PROMPT.md, বাগ A)।
    val totalEarnings = currentUser?.balanceSolver ?: 0.0

    // Recent completed jobs (takes first 2 for dashboard card)
    val recentCompleted = completedJobs.take(2)
    val currentPendingJob = pendingRatingJobs.firstOrNull()

    // Transaction History items (max 2 cards)
    // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ৫] আগে "... || it.userId == currentSolverId"
    // অংশটা এই অ্যাকাউন্টের USER-role transaction (deposit, refund, bid-accept deduction
    // ইত্যাদি)-ও এই Solver ড্যাশবোর্ডের "সাম্প্রতিক লেনদেন" কার্ডে ঢুকিয়ে ফেলত -- ধাপ ৫-এর
    // কনফার্ম করা উত্তর অনুযায়ী (দুই role সম্পূর্ণ আলাদা) এটা ভুল। এখন
    // TransactionHelper.matchesRoleForHistory() দিয়ে শুধু এই অ্যাকাউন্টের SOLVER-role
    // transaction-ই দেখানো হয়।
    val recentTransactions = remember(solverTransactions, currentSolverId) {
        solverTransactions
            .filter { TransactionHelper.matchesRoleForHistory(it, "SOLVER", currentSolverId) }
            .distinctBy { it.id }
            .take(2)
    }

    // ব্যাচ ৩১, সেশন ২.১ — ব্যবহারকারীর কনফার্মেশন অনুযায়ী: balance banner-এর ভ্যালু, ৪টা stat
    // card, "সম্প্রতি সম্পন্ন কাজ" লিস্ট, আর "লেনদেন হিস্ট্রি" লিস্ট — এই সবগুলো ডাইনামিক
    // ভ্যালু/লিস্ট pulse করবে; header (সেকশন-টাইটেল), বাটন ("আরও দেখুন"/"টাকা উইথড্র করুন"), আর
    // স্থির লেবেল (ব্যালেন্স-কার্ডের টাইটেল/স্ট্যাটাস ব্যাজ) সবসময় স্থির থাকবে।
    val balancePulse = rememberFieldChangePulse(
        value = totalEarnings,
        isManualRefreshing = isRefreshing,
        sessionKey = "dashboard_sync",
        viewModel = viewModel
    )
    val statsPulse = rememberFieldChangePulse(
        value = listOf(totalBids, winRate, completedJobs.size, totalEarnings),
        isManualRefreshing = isRefreshing,
        sessionKey = "dashboard_sync",
        viewModel = viewModel
    )
    val completedJobsPulse = rememberFieldChangePulse(
        value = recentCompleted,
        isManualRefreshing = isRefreshing,
        sessionKey = "dashboard_sync",
        viewModel = viewModel
    )
    val transactionsPulse = rememberFieldChangePulse(
        value = recentTransactions,
        isManualRefreshing = isRefreshing,
        sessionKey = "dashboard_sync",
        viewModel = viewModel
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (currentPendingJob != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, SomadhanOrange.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.dismissSolverRatingReminder(context, currentPendingJob.id)
                            onProblemClick(currentPendingJob.id)
                        }
                        .testTag("solver_pending_rating_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "গ্রাহককে রেটিং দিন ⭐",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrange
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "\"${currentPendingJob.title}\"",
                                fontSize = 11.sp,
                                color = SomadhanOrange.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        item {
            // Balance & Earnings Banner Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanOrange),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "বর্তমান উত্তোলনযোগ্য ব্যালেন্স",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "সক্রিয়",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    PulsingValue(isUpdating = balancePulse) {
                        Text(
                            text = Formatters.formatTaka(totalEarnings),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onNavigateToWithdraw,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("dashboard_withdraw_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "টাকা উইথড্র করুন",
                            color = SomadhanOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "কাজের পারফরম্যান্স পরিসংখ্যান",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Stats Grid: Total Bids, Win Rate %, Completed Jobs, Total Balance — ব্যাচ ৩১/২.১
            // (আপডেট): শুধু value-অংশ pulse করবে, বাইরের PulsingValue wrapper সরিয়ে
            // `StatCard(isValuePulsing = statsPulse)` ব্যবহার করা হলো (user-section-এর মতোই)।
            Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "মোট বিড",
                            value = DistanceUtil.toBengaliDigits(totalBids.toString()),
                            subtitle = "জমাকৃত প্রস্তাবনা",
                            icon = Icons.Default.Gavel,
                            iconColor = SomadhanOrange,
                            modifier = Modifier.weight(1f),
                            isValuePulsing = statsPulse
                        )

                        StatCard(
                            title = "বিড জয়ের হার",
                            value = "${DistanceUtil.toBengaliDigits(winRate.toString())}%",
                            subtitle = "গ্রহণযোগ্যতার হার",
                            icon = Icons.Default.TrendingUp,
                            iconColor = SomadhanSuccess,
                            modifier = Modifier.weight(1f),
                            isValuePulsing = statsPulse
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "সম্পন্ন কাজ",
                            value = DistanceUtil.toBengaliDigits(completedJobs.size.toString()),
                            subtitle = "সফল সমাপ্তি",
                            icon = Icons.Default.CheckCircle,
                            iconColor = SomadhanSuccess,
                            modifier = Modifier.weight(1f),
                            isValuePulsing = statsPulse
                        )

                        StatCard(
                            title = "আয়ের পরিমাণ",
                            value = Formatters.formatTaka(totalEarnings),
                            subtitle = "ওয়ালেট জমা",
                            icon = Icons.Default.MonetizationOn,
                            iconColor = SomadhanOrange,
                            modifier = Modifier.weight(1f),
                            isValuePulsing = statsPulse
                        )
                    }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Recent Completed Jobs Section + "See More" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সম্প্রতি সম্পন্ন কাজ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                TextButton(
                    onClick = onNavigateToCompletedJobs,
                    modifier = Modifier.testTag("see_more_completed_jobs")
                ) {
                    Text(
                        text = "আরও দেখুন (See More)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        if (recentCompleted.isEmpty()) {
            item {
                PulsingValue(isUpdating = completedJobsPulse) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "এখনও কোনো কাজ সম্পন্ন হয়নি।",
                            fontSize = 13.sp,
                            color = SomadhanTextHint
                        )
                    }
                }
                }
            }
        } else {
            items(recentCompleted, key = { it.id }) { job ->
                // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ৫] আগে "type != REFUND" যেকোনো
                // non-refund টাইপ (BID_ACCEPT_DEDUCTION, RELEASE_DEDUCTION-এর মতো USER-role
                // deduction-ও) ম্যাচ করতে পারত -- .find() প্রথম যেটা পেত সেটাই নিতো, যেটা এই
                // solver-এর আসল আয়ের (PAYMENT, role=SOLVER) বদলে ভুল amount দেখানোর ঝুঁকি
                // তৈরি করতো। এখন সুনির্দিষ্টভাবে এই solver-এর SOLVER-role transaction-ই খোঁজা
                // হয়।
                val trx = solverTransactions.find {
                    it.problemId == job.id && TransactionHelper.matchesRoleForHistory(it, "SOLVER", currentSolverId)
                }
                val baseAmount = job.acceptedAmount ?: (job.maxBudget ?: 0.0)
                val extraFromCharges = approvedChargesByProblem[job.id]?.sumOf { it.amount } ?: 0.0
                val extraCharges = if (job.confirmedExtraAmountTotal > 0.0) job.confirmedExtraAmountTotal else extraFromCharges
                val totalGross = if (trx != null) trx.grossAmount else (baseAmount + extraCharges)
                val displayAmount = if (trx != null) trx.netAmount else totalGross

                PulsingValue(isUpdating = completedJobsPulse) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                        .clickable { onProblemClick(job.id) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = job.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "৳ ${DistanceUtil.toBengaliDigits(displayAmount.toInt().toString())}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanSuccess
                                )
                                if (trx != null && trx.grossAmount > trx.netAmount) {
                                    Text(
                                        text = "(নিট আয় • মোট চুক্তি: ৳${DistanceUtil.toBengaliDigits(trx.grossAmount.toInt().toString())})",
                                        fontSize = 10.sp,
                                        color = SomadhanTextHint
                                    )
                                } else if (extraCharges > 0.0) {
                                    Text(
                                        text = "(মূল: ৳${DistanceUtil.toBengaliDigits(baseAmount.toInt().toString())} + অতিরিক্ত: ৳${DistanceUtil.toBengaliDigits(extraCharges.toInt().toString())})",
                                        fontSize = 10.sp,
                                        color = SomadhanTextHint
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ক্লায়েন্ট: ${job.userName}",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = Formatters.formatDateTimeBengali(job.completedAt ?: job.createdAt),
                                fontSize = 10.sp,
                                color = SomadhanTextHint
                            )
                        }
                    }
                }
                }
            }
        }

        // Section: লেনদেন হিস্ট্রি (Transaction History)
        item {
            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "লেনদেন হিস্ট্রি",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                TextButton(
                    onClick = onNavigateToTransactions,
                    modifier = Modifier.testTag("see_more_transaction_history")
                ) {
                    Text(
                        text = "আরও দেখুন (See More)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        if (recentTransactions.isEmpty()) {
            item {
                PulsingValue(isUpdating = transactionsPulse) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "এখনও কোনো লেনদেনের হিস্ট্রি নেই।",
                            fontSize = 13.sp,
                            color = SomadhanTextHint
                        )
                    }
                }
                }
            }
        } else {
            items(recentTransactions, key = { it.id }) { trx ->
                val context = LocalContext.current
                val clipboardManager = LocalClipboardManager.current
                val isRefund = (trx.type == "REFUND")
                val isEarning = (currentSolverId == trx.solverId && !isRefund)
                val isUserRefund = (currentSolverId == trx.userId && isRefund)
                // [Step 15.3 — CI_TEST_SUITE_MASTER_PROMPT.md] Bug fix: same bug-class as
                // TransactionHistoryScreen.kt (Step 15.2), and even more independent here — this
                // screen doesn't use TransactionHelper at all, `isRefund` only recognizes the
                // literal "REFUND" type (misses DISPUTE_REFUND/SPLIT_REFUND), and there's no
                // isDeposit check at all. `recentTransactions` (above) is already scoped to this
                // solver's SOLVER-role rows only via TransactionHelper.matchesRoleForHistory(), so
                // (as in Step 15.2) net_amount's sign is a reliable, type-agnostic source of truth
                // for this list. Reusing the same pure `transactionDisplaySign()` (declared
                // top-level in TransactionHistoryScreen.kt, same package) instead of the old
                // `isEarning || isUserRefund` enumeration, which silently defaulted to `false`
                // (always "−") for any other type (e.g. ADMIN_ADJUSTMENT, WITHDRAWAL_REFUND).
                // `isRefund`/`isEarning`/`isUserRefund` are kept unchanged — still used below for
                // label/icon/color branching, out of this bug-class's scope.
                val isPositive = transactionDisplaySign(trx, currentSolverId)
                val displayTitle = if (isRefund) {
                    if (trx.problemTitle.isNotBlank()) "রিফান্ড: ${trx.problemTitle}" else "বাতিল কাজের রিফান্ড"
                } else {
                    trx.problemTitle.ifBlank { "সমস্যা সমাধান লেনদেন" }
                }

                PulsingValue(isUpdating = transactionsPulse) {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isRefund -> Color(0xFF2563EB).copy(alpha = 0.12f)
                                                isPositive -> SomadhanSuccess.copy(alpha = 0.12f)
                                                else -> SomadhanOrange.copy(alpha = 0.12f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPositive) Icons.Default.MonetizationOn else Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = when {
                                            isRefund -> Color(0xFF2563EB)
                                            isPositive -> SomadhanSuccess
                                            else -> SomadhanOrange
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = displayTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isEarning && trx.commissionAmount > 0.0) {
                                        Text(
                                            text = "মূল: ৳${DistanceUtil.toBengaliDigits(trx.grossAmount)} • ফি: -৳${DistanceUtil.toBengaliDigits(trx.commissionAmount)}",
                                            fontSize = 11.sp,
                                            color = SomadhanTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                clipboardManager.setText(AnnotatedString(trx.id))
                                                val msg = if (isRefund) "রিফান্ড আইডি কপি করা হয়েছে" else "আইডি কপি করা হয়েছে"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text(
                                                text = if (isRefund) "রিফান্ড আইডি: ${trx.id.take(10)}" else "আইডি: ${trx.id.take(10)}",
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
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (isPositive) "+ " else "- "}৳ ${DistanceUtil.toBengaliDigits((if (isEarning) trx.netAmount else trx.grossAmount).toInt().toString())}",
                                    fontSize = 14.sp,
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
                    }
                }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
