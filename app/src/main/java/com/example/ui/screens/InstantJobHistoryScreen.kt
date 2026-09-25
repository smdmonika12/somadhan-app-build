package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.BidEntity
import com.example.data.entity.EscrowEntity
import com.example.data.entity.PlatformSettingEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import com.example.data.repository.SomadhanRepository
import com.example.ui.components.ShimmerBlock
import com.example.ui.components.SyncAwareRefreshableContent
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanInfoLight
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.InstantJobReceiptUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase T & AC: Instant Job History Screen.
 * Displays completed, cancelled, and disputed instant jobs for the current user/solver.
 *
 * Requirements:
 * 1. Bottom Sheet popup summary experience standardized for ALL tabs (Completed, Cancelled, Disputed)
 *    for both User and Solver roles.
 * 2. Strict separation of jobs: Completed tab strictly excludes cancelled jobs, and Cancelled tab
 *    strictly excludes completed/disputed jobs.
 * 3. Dynamic progress timeline with green checkmarks up to point of cancellation and red crosses for cancelled/subsequent steps.
 * 4. Fully dynamic solver/user names, ratings, review counts, and financials.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstantJobHistoryScreen(
    viewModel: SomadhanViewModel,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val allBids by viewModel.allBids.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allPlatformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
    val userTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    val userEscrows by viewModel.userEscrows.collectAsStateWithLifecycle()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Popup Bottom Sheet States for all tabs
    var selectedCompletedProblem by remember { mutableStateOf<ProblemEntity?>(null) }
    var selectedUserCancelledProblem by remember { mutableStateOf<ProblemEntity?>(null) }
    var selectedSolverCancelledDetailItem by remember { mutableStateOf<SolverCancelledHistoryItem?>(null) }
    var selectedDisputedProblem by remember { mutableStateOf<ProblemEntity?>(null) }

    val isSolver = currentUser?.role == "SOLVER"
    val uid = currentUser?.id ?: ""

    // Loading/Sync Fix Roadmap v2, dhap 4 -- initialSyncPhase (bulk-pull), separate sessionKey "instant_job_history_sync"
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()
    // Loading/Sync Fix Roadmap v2, ধাপ ৬-এ এই স্ক্রিনে pull-to-refresh (SomadhanPullToRefresh)
    // যোগ করা হয়েছিল, কিন্তু ব্যাচ ৩১ সেশন ২.২-এ ব্যবহারকারীর স্পষ্ট নির্দেশে সেটা পুরোপুরি সরানো
    // হয়েছে (এই ট্যাবে pull-to-refresh থাকার কথা না)।
    // [শিমার ফিক্স — MONEY_FLOW_AND_ADMIN_BUGS_MASTER_PROMPT.md ধাপ ৫, বাগ B] আগে এখানেও
    // ProfileScreen.kt-এর মতোই global viewModel.isRefreshing পড়ে নিচের isManualRefreshing-এ
    // পাস করা হতো — অন্য স্ক্রিনের manual-refresh শেষ হলেই (true->false) এই ট্যাবে থাকা
    // অবস্থায় অপ্রত্যাশিত flash দেখাত, যদিও এই ট্যাবে কোনো pull-gesture-ই নেই তা ট্রিগার করার
    // জন্য। ফিক্স: global read সম্পূর্ণ বাদ, নিচে সরাসরি local সমতুল্য মান false।

    val myInstantJobs = remember(allProblems, currentUser) {
        allProblems.filter { prob ->
            prob.isInstantJob && (prob.userId == uid || prob.acceptedSolverId == uid)
        }
    }

    // Bug fix (per-cycle history): which bid.resolutionType values represent a plain
    // cancellation vs. an admin dispute resolution. Used below instead of the shared,
    // per-cycle-overwritten Problem-level dispute fields, so an earlier cycle's plain
    // cancellation still shows in "Cancelled" even if a LATER cycle on the same problem
    // went to dispute (and vice versa).
    val cancelOutcomeTypes = remember { setOf("SOLVER_CANCEL", "USER_CANCEL", "EXPIRED", "ADMIN_MANUAL_REFUND") }
    val disputeOutcomeTypes = remember { setOf("ADMIN_SPLIT", "ADMIN_REFUND_TO_USER", "ADMIN_RELEASE_TO_SOLVER") }

    // 1. Completed Tab (Strictly excludes any cancelled or disputed jobs)
    val completedJobs = remember(myInstantJobs, uid, isSolver) {
        myInstantJobs.filter { prob ->
            val isCompleted = (prob.jobStatus == "JOB_COMPLETED" || prob.status == "COMPLETED")
            val isNotCancelled = prob.jobStatus != "CANCELLED" && prob.status != "CANCELLED" && prob.solverCancelledNotice.isNullOrBlank()
            val isNotDisputed = !prob.isDisputed && prob.disputeResolvedAt == null && prob.disputeResolutionDecision.isNullOrBlank() && prob.disputeResolutionType.isNullOrBlank()
            val roleMatches = if (isSolver) prob.acceptedSolverId == uid else prob.userId == uid
            roleMatches && isCompleted && isNotCancelled && isNotDisputed
        }.sortedByDescending { it.completedAt ?: it.createdAt }
    }

    // 2. Cancelled Tab for User (Strictly cancelled, not completed, not disputed)
    val userCancelledJobs = remember(myInstantJobs, allBids, uid) {
        myInstantJobs.filter { prob ->
            if (prob.userId != uid) return@filter false
            // Bug fix: check the job's CURRENT completion status before trusting a stamped
            // cancel bid. A job can have an earlier cycle's SOLVER_CANCEL/USER_CANCEL/EXPIRED
            // stamped bid from a first solver, then go on to be successfully completed by a
            // second solver -- without this check first, such a job showed up in BOTH the
            // "সম্পন্ন" and "বাতিল" tabs at once.
            val isCompletedNow = prob.jobStatus == "JOB_COMPLETED" || prob.status == "COMPLETED"
            if (isCompletedNow) return@filter false
            val stampedCancelBid = allBids.any { it.problemId == prob.id && it.resolutionType in cancelOutcomeTypes }
            if (stampedCancelBid) return@filter true
            // Legacy fallback: only bids saved before resolutionType existed have a null value
            // here. Only fall back to the old (problem-wide) heuristic when NOTHING on this
            // problem has been stamped yet, so a properly-stamped cycle never gets re-evaluated
            // by the less accurate legacy path.
            val anyStamped = allBids.any { it.problemId == prob.id && it.resolutionType != null }
            if (anyStamped) return@filter false
            val isCancelled = (prob.jobStatus == "CANCELLED" || prob.status == "CANCELLED" || !prob.solverCancelledNotice.isNullOrBlank())
            val isNotDisputed = !prob.isDisputed && prob.disputeResolvedAt == null && prob.disputeResolutionDecision.isNullOrBlank() && prob.disputeResolutionType.isNullOrBlank()
            val isNotCompleted = prob.jobStatus != "JOB_COMPLETED" && prob.status != "COMPLETED"
            isCancelled && isNotDisputed && isNotCompleted
        }.sortedByDescending { it.createdAt }
    }

    // 2. Cancelled Tab for Solver (Bid-level tracking + Option A note, strictly not completed as active unless re-completed)
    val solverCancelledItems: List<SolverCancelledHistoryItem> = remember(allProblems, allBids, uid, isSolver) {
        if (!isSolver || uid.isBlank()) emptyList()
        else {
            val list = mutableListOf<SolverCancelledHistoryItem>()
            val seenProblemIds = mutableSetOf<String>()

            // Find all cancelled bids by this solver on instant jobs (excluding ones that were
            // actually resolved via a dispute outcome — e.g. ADMIN_REFUND_TO_USER also sets
            // bid.status = "CANCELLED", but that cycle belongs in the Disputed tab, not here).
            val myCancelledBids = allBids.filter { it.solverId == uid && it.status == "CANCELLED" && it.resolutionType !in disputeOutcomeTypes }
            for (bid in myCancelledBids) {
                val prob = allProblems.find { it.id == bid.problemId && it.isInstantJob }
                if (prob != null) {
                    seenProblemIds.add(prob.id)
                    val reCompleted = prob.acceptedSolverId == uid && (prob.jobStatus == "JOB_COMPLETED" || prob.status == "COMPLETED") && prob.jobStatus != "CANCELLED"
                    val prog = bid.progressAtCancel ?: prob.calculateProgressStep().coerceAtLeast(1)
                    list.add(
                        SolverCancelledHistoryItem(
                            id = bid.id,
                            problem = prob,
                            bid = bid,
                            progressAtCancel = prog,
                            reCompletedBySameSolver = reCompleted,
                            timestamp = bid.createdAt
                        )
                    )
                }
            }

            // Also check any instant jobs where problem itself was cancelled and acceptedSolverId was this solver
            val cancelledProblemsForSolver = allProblems.filter { prob ->
                prob.isInstantJob && prob.acceptedSolverId == uid &&
                    (prob.jobStatus == "CANCELLED" || prob.status == "CANCELLED" || !prob.solverCancelledNotice.isNullOrBlank()) &&
                    !prob.isDisputed && prob.disputeResolvedAt == null && prob.disputeResolutionDecision.isNullOrBlank() &&
                    prob.jobStatus != "JOB_COMPLETED" && prob.status != "COMPLETED" &&
                    !seenProblemIds.contains(prob.id)
            }
            for (prob in cancelledProblemsForSolver) {
                val prog = prob.calculateProgressStep().coerceAtLeast(1)
                list.add(
                    SolverCancelledHistoryItem(
                        id = "prob_${prob.id}",
                        problem = prob,
                        bid = null,
                        progressAtCancel = prog,
                        reCompletedBySameSolver = false,
                        timestamp = prob.createdAt
                    )
                )
            }

            list.sortedByDescending { it.timestamp }
        }
    }

    // 3. Disputed Tab (Resolved or Unresolved)
    val disputedJobs = remember(myInstantJobs, uid, isSolver, allBids) {
        myInstantJobs.filter { prob ->
            val roleMatches = if (isSolver) (prob.acceptedSolverId == uid || allBids.any { it.problemId == prob.id && it.solverId == uid }) else prob.userId == uid
            if (!roleMatches) return@filter false
            val stampedDisputeBid = allBids.any { b ->
                b.problemId == prob.id && b.resolutionType in disputeOutcomeTypes && (!isSolver || b.solverId == uid)
            }
            if (stampedDisputeBid) return@filter true
            // Legacy fallback: only when nothing on this problem has been stamped yet.
            val anyStamped = allBids.any { it.problemId == prob.id && it.resolutionType != null }
            if (anyStamped) return@filter false
            prob.isDisputed || prob.disputeResolvedAt != null || !prob.disputeResolutionDecision.isNullOrBlank() || !prob.disputeResolutionType.isNullOrBlank()
        }.sortedByDescending { it.disputeResolvedAt ?: it.createdAt }
    }

    val cancelledCount = if (isSolver) solverCancelledItems.size else userCancelledJobs.size

    val tabs = listOf(
        HistoryTabItem("সম্পন্ন", completedJobs.size, Icons.Default.CheckCircle, SomadhanSuccess),
        HistoryTabItem("বাতিল", cancelledCount, Icons.Default.Block, SomadhanError),
        HistoryTabItem("ডিসপিউট", disputedJobs.size, Icons.Default.Gavel, SomadhanOrange)
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = SomadhanBg,
        topBar = {
            Surface(
                color = SomadhanCardBg,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusBarPadding)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("history_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = SomadhanTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSolver) "সমাধানকারীর কাজের ইতিহাস" else "জরুরি কাজের ইতিহাস",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }

                    // 3-Tab Row
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = SomadhanCardBg,
                        contentColor = SomadhanOrange,
                        edgePadding = 16.dp,
                        indicator = { tabPositions ->
                            if (pagerState.currentPage < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    color = SomadhanOrange,
                                    height = 3.dp
                                )
                            }
                        },
                        divider = {
                            HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.6f))
                        }
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = pagerState.currentPage == index
                            Tab(
                                selected = isSelected,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                modifier = Modifier.testTag("history_tab_$index"),
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) tab.color else SomadhanTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = tab.title,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) SomadhanTextPrimary else SomadhanTextSecondary
                                        )
                                        // Count Badge
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) tab.color.copy(alpha = 0.15f) else Color(0xFFF3F4F6)
                                        ) {
                                            Text(
                                                text = DistanceUtil.toBengaliDigits(tab.count.toString()),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) tab.color else SomadhanTextSecondary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // ব্যাচ ৩১, সেশন ২.২ — এই ট্যাবে pull-to-refresh থাকা উচিত না (মূল প্রম্পটের স্পষ্ট
        // নির্দেশ), তাই বাইরের SomadhanPullToRefresh wrapper পুরোপুরি সরানো হলো। বাকি সব আচরণ
        // (initial sync/skeleton/tab-switch) অপরিবর্তিত রাখা হয়েছে।
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { pageIndex ->
            // Loading/Sync Fix Roadmap v2, dhap 7 (batch 2, pagination-screen migration) --
            // এতদিন এখানে data-বিহীন SyncAwareContent ছিল (rule ১ কভার করতো, rule ২/৪ ডেটা-ডিফ
            // শিমার ছিল না)। এই স্ক্রিনে pagination-reset ফাংশন নেই (উপরের কমেন্ট দ্রষ্টব্য) --
            // সব ট্যাবের ডেটা সরাসরি allProblems/allBids-এর plain filter (remember block),
            // page-ভিত্তিক বাফার না -- তাই BidManagementScreen/SolverCompletedJobsScreen-এর
            // মতোই কোনো suppression জটিলতা ছাড়াই সরাসরি migrate করা যায়। দুই ব্রাঞ্চের টাইপ
            // ভিন্ন (List<SolverCancelledHistoryItem> বনাম List<ProblemEntity>) বলে diffing-এর
            // জন্য একটা কমন List<Any> snapshot বানানো হলো; রেন্ডারিং আগের মতোই নিচে টাইপ-নির্দিষ্ট
            // solverCancelledItems/currentList ভ্যারিয়েবল থেকেই হয় (লজিক অপরিবর্তিত)।
            val displayedHistoryItems: List<Any> = if (pageIndex == 1 && isSolver) {
                solverCancelledItems
            } else {
                when (pageIndex) {
                    0 -> completedJobs
                    1 -> userCancelledJobs
                    else -> disputedJobs
                }
            }

            // ব্যাচ ৩১, সেশন ২.২ — আগে কোনো কাস্টম skeleton না দেওয়ায় ডিফল্ট ListScreenSkeleton()
            // (ProblemCardSkeleton-শেপ) দেখাতো, যেটা history-card-এর real layout-এর (chip+badge
            // row, title, id+date row, divider, avatar+name+amount row) সাথে মিলতো না — এখন
            // নিচের HistoryCardSkeleton() (এই ফাইলেই নতুন সংজ্ঞায়িত, উভয় ট্যাবের কার্ড-শেপের সাথেই
            // মিলে যায়) দিয়ে ফিক্স করা হলো, একই contentPadding/spacing আসল LazyColumn-এর মতোই।
            SyncAwareRefreshableContent(
                sessionKey = "instant_job_history_sync",
                viewModel = viewModel,
                syncPhase = initialSyncPhase,
                data = displayedHistoryItems,
                onRetry = { viewModel.retryInitialSync() },
                isManualRefreshing = false,
                skeleton = {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(4) { HistoryCardSkeleton() }
                    }
                }
            ) {
            if (pageIndex == 1 && isSolver) {
                // Cancelled Tab for Solver
                if (solverCancelledItems.isEmpty()) {
                    HistoryEmptyState(
                        tabIndex = 1,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("history_list_tab_1"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = solverCancelledItems,
                            key = { it.id }
                        ) { item ->
                            SolverCancelledHistoryCard(
                                item = item,
                                transactions = userTransactions,
                                onClick = {
                                    selectedSolverCancelledDetailItem = item
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
                        }
                    }
                }
            } else {
                // Standard Tab for User / Completed & Disputed Tabs for Solver
                val currentList = when (pageIndex) {
                    0 -> completedJobs
                    1 -> userCancelledJobs
                    else -> disputedJobs
                }

                if (currentList.isEmpty()) {
                    HistoryEmptyState(
                        tabIndex = pageIndex,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("history_list_tab_$pageIndex"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = currentList,
                            key = { it.id }
                        ) { problem ->
                            InstantJobHistoryCard(
                                problem = problem,
                                currentUserId = uid,
                                tabIndex = pageIndex,
                                transactions = userTransactions,
                                allUsers = allUsers,
                                allBids = allBids,
                                onClick = {
                                    when (pageIndex) {
                                        0 -> selectedCompletedProblem = problem
                                        1 -> selectedUserCancelledProblem = problem
                                        else -> selectedDisputedProblem = problem
                                    }
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
                        }
                    }
                }
            }
            }
        }
    }

    // --- POPUP BOTTOM SHEETS FOR ALL TABS ---

    // 1. Completed Tab Popup Bottom Sheet (User & Solver)
    selectedCompletedProblem?.let { problem ->
        CompletedSummaryBottomSheet(
            problem = problem,
            currentUser = currentUser,
            allUsers = allUsers,
            allPlatformSettings = allPlatformSettings,
            viewModel = viewModel,
            onDismiss = { selectedCompletedProblem = null }
        )
    }

    // 2. User Cancelled Tab Popup Bottom Sheet
    selectedUserCancelledProblem?.let { problem ->
        UserCancelledSummaryBottomSheet(
            problem = problem,
            currentUser = currentUser,
            allBids = allBids,
            allUsers = allUsers,
            transactions = userTransactions,
            escrows = userEscrows,
            onDismiss = { selectedUserCancelledProblem = null }
        )
    }

    // 3. Solver Cancelled Tab Popup Bottom Sheet
    selectedSolverCancelledDetailItem?.let { item ->
        SolverCancelledDetailBottomSheet(
            item = item,
            currentUser = currentUser,
            allUsers = allUsers,
            transactions = userTransactions,
            onDismiss = { selectedSolverCancelledDetailItem = null }
        )
    }

    // 4. Disputed Tab Popup Bottom Sheet (User & Solver)
    selectedDisputedProblem?.let { problem ->
        DisputeSummaryBottomSheet(
            problem = problem,
            currentUser = currentUser,
            allUsers = allUsers,
            viewModel = viewModel,
            onDismiss = { selectedDisputedProblem = null }
        )
    }
}

/**
 * Data holder for Solver Cancelled History Items.
 */
data class SolverCancelledHistoryItem(
    val id: String,
    val problem: ProblemEntity,
    val bid: BidEntity? = null,
    val progressAtCancel: Int = 1,
    val reCompletedBySameSolver: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

private data class HistoryTabItem(
    val title: String,
    val count: Int,
    val icon: ImageVector,
    val color: Color
)

// ==========================================
// 1. COMPLETED SUMMARY BOTTOM SHEET
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompletedSummaryBottomSheet(
    problem: ProblemEntity,
    currentUser: UserEntity?,
    allUsers: List<UserEntity>,
    allPlatformSettings: List<PlatformSettingEntity>,
    viewModel: SomadhanViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isUserRole = problem.userId == currentUser?.id

    val otherUserId = if (isUserRole) problem.acceptedSolverId else problem.userId
    val otherUser = remember(otherUserId, allUsers) {
        allUsers.find { it.id == otherUserId }
    }

    val baseAmount = problem.acceptedAmount ?: problem.minBudget ?: 0.0
    val extraAmount = problem.confirmedExtraAmountTotal
    val totalGross = baseAmount + extraAmount

    var commissionBreakdown by remember(problem.id, baseAmount, extraAmount) {
        mutableStateOf<SomadhanRepository.CommissionBreakdown?>(null)
    }

    LaunchedEffect(problem.id, baseAmount, extraAmount) {
        val sId = problem.acceptedSolverId ?: ""
        commissionBreakdown = viewModel.previewCommissionBreakdown(problem, sId, baseAmount, extraAmount)
    }

    val defaultCommissionRate = allPlatformSettings.find { it.key == "commission_percent" }?.value?.toDoubleOrNull() ?: 10.0
    val solverCommissionRate = problem.appliedCommissionRate ?: defaultCommissionRate
    val fallbackPlatformFee = (totalGross * solverCommissionRate) / 100.0
    val fallbackNetSolverAmount = (totalGross - fallbackPlatformFee).coerceAtLeast(0.0)

    val finalTotalCommission = commissionBreakdown?.totalCommission ?: fallbackPlatformFee
    val netSolverEarnings = commissionBreakdown?.netAmount ?: fallbackNetSolverAmount
    val wasFreeQuota = commissionBreakdown?.wasFreeQuotaJob ?: (solverCommissionRate == 0.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SomadhanCardBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.somadhan_app_icon_1786820904533),
                        contentDescription = "সমাধান লোগো",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column {
                        Text(
                            text = "কাজ সম্পন্ন হওয়ার সামারি",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "সমাধান (Somadhan) • আর্থিক ও টাইমলাইন বিবরণ",
                            fontSize = 11.5.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_completed_summary")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = SomadhanTextSecondary
                    )
                }
            }

            // 1. Hero Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanSuccessLight.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, SomadhanSuccess.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SomadhanSuccess),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "কাজটি সফলভাবে সম্পন্ন হয়েছে! 🎉",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanSuccess
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = problem.title.ifBlank { "জরুরি সমস্যা সমাধান" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = SomadhanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ক্যাটাগরি: ${problem.categoryName.ifBlank { "সাধারণ" }}",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                            // ID Chip
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White)
                                    .border(0.6.dp, Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(problem.id))
                                        Toast.makeText(context, "পোস্ট ID কপি করা হয়েছে: ${problem.id}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tag,
                                    contentDescription = null,
                                    tint = SomadhanTextHint,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "ID: ${problem.id.take(8)}...",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Stepper (All 4 Steps Done ✓)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "কাজের অগ্রগতি ধাপসমূহ",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    val steps = listOf("১. আসছেন", "২. পৌঁছে গেছেন", "৩. কাজ চলছে", "৪. সম্পন্ন")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        steps.forEachIndexed { index, stepName ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SomadhanSuccess),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            if (index < steps.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(2.dp)
                                        .background(SomadhanSuccess)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        steps.forEach { stepName ->
                            Text(
                                text = stepName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanSuccess
                            )
                        }
                    }
                }
            }

            // 3. Dynamic Timeline Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "কাজের টাইমলাইন বিবরণ",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }

                    HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.5f))

                    val timelineItems = listOf(
                        Triple("পোস্ট হয়েছে", problem.createdAt, Icons.Default.Bolt),
                        Triple("বিড গৃহীত", problem.acceptedAt2 ?: problem.createdAt, Icons.Default.Check),
                        Triple("রওনা দিয়েছেন", problem.onWayAt ?: problem.acceptedAt2 ?: problem.createdAt, Icons.Default.Navigation),
                        Triple("পৌঁছেছেন", problem.arrivedAt ?: problem.onWayAt ?: problem.createdAt, Icons.Default.Place),
                        Triple("কাজ শুরু", problem.jobStartedAt ?: problem.arrivedAt ?: problem.createdAt, Icons.Default.PlayArrow),
                        Triple("কাজ সম্পন্ন হয়েছে", problem.completedAt ?: problem.lastActivityAt ?: System.currentTimeMillis(), Icons.Default.CheckCircle)
                    )

                    timelineItems.forEachIndexed { index, (label, timestamp, icon) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(SomadhanSuccessLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = SomadhanSuccess,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                if (index < timelineItems.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(16.dp)
                                            .background(SomadhanSuccess.copy(alpha = 0.5f))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = if (index < timelineItems.size - 1) 8.dp else 0.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanTextPrimary
                                )
                                Text(
                                    text = formatTimelineBengali(timestamp),
                                    fontSize = 11.5.sp,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 4. Other Party Profile Card (Dynamic Name, Rating, Reviews)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isUserRole) "সমাধানকারীর তথ্য" else "সেবাগ্রহীতার তথ্য",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.5f))

                    val name = if (isUserRole) {
                        problem.acceptedSolverName ?: otherUser?.name ?: "দক্ষ সমাধানকারী"
                    } else {
                        problem.userName ?: otherUser?.name ?: "সম্মানিত গ্রাহক"
                    }

                    val roleBadge = if (isUserRole) "সমাধানকারী" else "সেবাগ্রহীতা"
                    val ratingValue = ((otherUser?.reputationScore ?: 50.0) / 10.0).coerceIn(1.0, 5.0)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UserAvatar(
                            photoUri = otherUser?.profileImageUri,
                            name = name,
                            size = 44.dp,
                            backgroundColor = if (isUserRole) SomadhanOrangeLight else SomadhanSuccessLight,
                            textColor = if (isUserRole) SomadhanOrange else SomadhanSuccess,
                            fontSize = 16.sp,
                            borderColor = if (isUserRole) SomadhanOrange.copy(alpha = 0.4f) else SomadhanSuccess.copy(alpha = 0.4f)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isUserRole) SomadhanOrangeLight.copy(alpha = 0.5f) else SomadhanSuccessLight
                                ) {
                                    Text(
                                        text = roleBadge,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isUserRole) SomadhanOrange else SomadhanSuccess,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (isUserRole) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = DistanceUtil.toBengaliDigits(String.format(Locale.ENGLISH, "%.1f", ratingValue)),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary
                                        )
                                    }
                                }
                            }
                            if (!otherUserId.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "আইডি: ${otherUserId.take(12)}...",
                                    fontSize = 10.5.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        }
                    }
                }
            }

            // 5. Accurate Financial Breakdown Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "আর্থিক হিসাব বিবরণী",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("চুক্তিভিত্তিক মূল বিল:", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                        Text(
                            text = "৳ ${DistanceUtil.toBengaliDigits(baseAmount.toInt().toString())}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanTextPrimary
                        )
                    }

                    if (extraAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("অতিরিক্ত চার্জ / পার্টস বিল:", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                            Text(
                                text = "+ ৳ ${DistanceUtil.toBengaliDigits(extraAmount.toInt().toString())}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanOrange
                            )
                        }
                    }

                    if (!isUserRole) {
                        // Solver View: Commission & Net Earnings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("প্ল্যাটফর্ম কমিশন (${DistanceUtil.toBengaliDigits(solverCommissionRate.toInt().toString())}%):", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                            Text(
                                text = if (wasFreeQuota) "৳ ০ (ফ্রি কোটা)" else "- ৳ ${DistanceUtil.toBengaliDigits(finalTotalCommission.toInt().toString())}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (wasFreeQuota) SomadhanSuccess else SomadhanError
                            )
                        }

                        HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("আপনার মোট আয়:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(netSolverEarnings.toInt().toString())}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanSuccess
                            )
                        }
                    } else {
                        // Client View: Total Paid
                        HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("পরিশোধকৃত সর্বমোট বিল:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(totalGross.toInt().toString())}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrange
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Download Receipt Button
            Button(
                onClick = {
                    val receiptFile = InstantJobReceiptUtil.generateInstantJobReceipt(
                        context = context,
                        problem = problem,
                        otherUser = otherUser,
                        selfUser = currentUser,
                        isUserRole = isUserRole,
                        commissionBreakdown = commissionBreakdown
                    )
                    if (receiptFile != null) {
                        Toast.makeText(context, "রিসিট ডাউনলোড হয়েছে (${receiptFile.name})", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "রিসিট তৈরিতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SomadhanOrangeLight,
                    contentColor = SomadhanOrange
                ),
                border = BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("completed_summary_receipt_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = SomadhanOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "রিসিট ডাউনলোড করুন (PDF)",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanOrange
                )
            }

            // Dismiss Button
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .navigationBarsPadding()
                    .testTag("completed_summary_dismiss_button")
            ) {
                Text(
                    text = "ফিরে যান",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ==========================================
// 2. USER CANCELLED SUMMARY BOTTOM SHEET
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserCancelledSummaryBottomSheet(
    problem: ProblemEntity,
    currentUser: UserEntity?,
    allBids: List<BidEntity>,
    allUsers: List<UserEntity>,
    transactions: List<TransactionEntity> = emptyList(),
    escrows: List<EscrowEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val isUserRole = problem.userId == currentUser?.id

    // Dynamically look up the solver who was assigned or cancelled
    val relevantCancelledBid = remember(problem.id, allBids) {
        allBids.filter { it.problemId == problem.id && (it.status == "CANCELLED" || it.status == "ACCEPTED") }
            .maxByOrNull { it.createdAt }
    }

    val solverId = problem.acceptedSolverId ?: relevantCancelledBid?.solverId ?: ""
    val solverUser = remember(solverId, allUsers) {
        allUsers.find { it.id == solverId }
    }

    val solverName = solverUser?.name
        ?: problem.acceptedSolverName?.takeIf { it.isNotBlank() }
        ?: relevantCancelledBid?.solverName?.takeIf { it.isNotBlank() }
        ?: "কোনো সমাধানকারী যুক্ত হননি"

    val solverRating = if (solverUser != null) {
        ((solverUser.reputationScore) / 10.0).coerceIn(1.0, 5.0)
    } else {
        relevantCancelledBid?.solverRating ?: 5.0
    }

    // Dynamic derivation of reached progress step at cancellation
    val reachedStep: Int = (relevantCancelledBid?.progressAtCancel)
        ?: (if (problem.jobStartedAt != null) 4
            else if (problem.arrivedAt != null) 3
            else if (problem.onWayAt != null) 2
            else if (problem.acceptedAt2 != null || !problem.acceptedSolverId.isNullOrBlank()) 1
            else 1)

    val formattedDate = remember(problem.createdAt, problem.lastActivityAt) {
        val ts = problem.lastActivityAt ?: problem.createdAt
        val sdf = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault())
        DistanceUtil.toBengaliDigits(sdf.format(Date(ts)))
    }

    // Look up refund transaction for accurate refunded amount
    val refundTx = remember(problem.id, transactions) {
        transactions.firstOrNull { it.problemId == problem.id && it.type == "REFUND" }
    }

    val totalRefundAmount = refundTx?.grossAmount?.takeIf { it > 0.0 } ?: 0.0

    // Defensive escrow check: Check if an escrow record exists for this problem
    val problemEscrow = remember(problem.id, escrows) {
        escrows.firstOrNull { it.problemId == problem.id }
    }
    val escrowTotalAmount = remember(problemEscrow, problem) {
        val fromEscrow = if (problemEscrow != null) {
            problemEscrow.baseAmount + problemEscrow.extraAmount
        } else 0.0
        if (fromEscrow > 0.0) fromEscrow
        else if (problem.acceptedSolverId != null) {
            (problem.acceptedAmount ?: problem.minBudget ?: 0.0) + problem.confirmedExtraAmountTotal
        } else 0.0
    }
    val hasLockedEscrow = escrowTotalAmount > 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SomadhanCardBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.somadhan_app_icon_1786820904533),
                        contentDescription = "সমাধান লোগো",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column {
                        Text(
                            text = "বাতিল সারাংশ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "সমাধান (Somadhan) • বাতিল কাজের অগ্রগতি ও বিবরণ",
                            fontSize = 11.5.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_user_cancelled_summary")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = SomadhanTextSecondary
                    )
                }
            }

            // 1. Hero Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanErrorLight.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, SomadhanError.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SomadhanError),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "কাজটি বাতিল করা হয়েছে ❌",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanError
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = problem.solverCancelledNotice?.takeIf { it.isNotBlank() }
                                ?: problem.disputeReason?.takeIf { it.isNotBlank() }
                                ?: "সমাধানকারী বা গ্রাহক কর্তৃক এই কাজটি বাতিল করা হয়েছিল।",
                            fontSize = 12.5.sp,
                            color = SomadhanTextPrimary,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "বাতিলের সময়: $formattedDate",
                            fontSize = 11.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }
            }

            // 2. Dynamic Progress Step Line Bar (Green checkmark for completed, Red cross for cancel point and beyond)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "কাজের অগ্রগতি ও বাতিলকালীন স্টেজ",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }

                    // Stepper Line Bar
                    val stepLabels = listOf("১. ব্রডকাস্ট", "২. রওনা", "৩. পৌঁছেছেন", "৪. কাজ শুরু", "৫. সম্পন্ন")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        stepLabels.forEachIndexed { index, _ ->
                            val stepNum = index + 1
                            val isDone = stepNum < reachedStep || (stepNum == 1 && reachedStep >= 1 && stepNum != reachedStep)
                            val isCancelPoint = stepNum == reachedStep

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isDone -> SomadhanSuccess
                                            isCancelPoint -> SomadhanError
                                            else -> Color(0xFFE5E7EB)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDone) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (isDone || isCancelPoint) Color.White else SomadhanTextHint,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            if (index < stepLabels.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(2.dp)
                                        .background(if (isDone) SomadhanSuccess else SomadhanError.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.5f))

                    // Detailed Checklist
                    val checklistSteps = listOf(
                        "১. জরুরি ব্রডকাস্ট ও সংযোগ" to 1,
                        "২. সমাধানকারী রওয়ানা হয়েছেন" to 2,
                        "৩. লোকেশনে পৌঁছেছেন" to 3,
                        "৪. কাজ শুরু হয়েছে" to 4,
                        "৫. কাজ সম্পন্ন" to 5
                    )

                    checklistSteps.forEach { (label, stepNum) ->
                        val isDone = stepNum < reachedStep || (stepNum == 1 && reachedStep >= 1 && stepNum != reachedStep)
                        val isCancelPoint = stepNum == reachedStep

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isCancelPoint -> SomadhanErrorLight.copy(alpha = 0.4f)
                                        isDone -> SomadhanSuccessLight.copy(alpha = 0.4f)
                                        else -> Color.Transparent
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isDone) SomadhanSuccess else if (isCancelPoint) SomadhanError else SomadhanError.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isDone || isCancelPoint) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isDone) SomadhanTextPrimary else if (isCancelPoint) SomadhanError else SomadhanTextSecondary
                                )
                                if (isCancelPoint) {
                                    Text(
                                        text = "এই ধাপে পৌঁছানোর পর কাজটি বাতিল হয়",
                                        fontSize = 10.5.sp,
                                        color = SomadhanError,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Dynamic Solver Information Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "সমাধানকারীর তথ্য",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UserAvatar(
                            photoUri = solverUser?.profileImageUri,
                            name = solverName,
                            size = 44.dp,
                            backgroundColor = SomadhanOrangeLight,
                            textColor = SomadhanOrange,
                            fontSize = 16.sp,
                            borderColor = SomadhanOrange.copy(alpha = 0.4f)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = solverName,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SomadhanOrangeLight.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "সমাধানকারী",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanOrange,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (solverUser != null || relevantCancelledBid != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = DistanceUtil.toBengaliDigits(String.format(Locale.ENGLISH, "%.1f", solverRating)),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary
                                        )
                                    }
                                }
                            }
                            if (solverId.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "আইডি: ${solverId.take(12)}...",
                                    fontSize = 10.5.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        }
                    }
                }
            }

            // 4. Financial & Escrow Refund Guarantee Card
            val isFundRefunded = refundTx != null && totalRefundAmount > 0.0
            val isRefundPending = !isFundRefunded && hasLockedEscrow
            val refundAmountToDisplay = if (isFundRefunded) totalRefundAmount else escrowTotalAmount

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isFundRefunded -> SomadhanSuccessLight.copy(alpha = 0.35f)
                        isRefundPending -> SomadhanOrangeLight.copy(alpha = 0.35f)
                        else -> Color(0xFFF3F4F6)
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    when {
                        isFundRefunded -> SomadhanSuccess.copy(alpha = 0.3f)
                        isRefundPending -> SomadhanOrange.copy(alpha = 0.4f)
                        else -> SomadhanBorder
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isRefundPending) Icons.Default.Timer else Icons.Default.Shield,
                        contentDescription = null,
                        tint = when {
                            isFundRefunded -> SomadhanSuccess
                            isRefundPending -> SomadhanOrange
                            else -> SomadhanTextSecondary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = when {
                                isFundRefunded -> "১০০% অর্থ আপনার ওয়ালেটে রিফান্ড করা হয়েছে"
                                isRefundPending -> "রিফান্ড প্রক্রিয়া চলমান • এসক্রো সুরক্ষিত"
                                else -> "এসক্রো পেমেন্ট কর্তন হয়নি • সম্পূর্ণ সুরক্ষিত"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isFundRefunded -> SomadhanSuccess
                                isRefundPending -> SomadhanOrange
                                else -> SomadhanTextPrimary
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                isFundRefunded -> {
                                    "মোট রিফান্ড: ৳ ${DistanceUtil.toBengaliDigits(totalRefundAmount.toInt().toString())}। এসক্রো সিকিউরিটি সিস্টেমের মাধ্যমে সম্পূর্ণ অর্থ ফেরত সুরক্ষিত রয়েছে।"
                                }
                                isRefundPending -> {
                                    "নির্ধারিত ৳ ${DistanceUtil.toBengaliDigits(refundAmountToDisplay.toInt().toString())} রিফান্ড প্রক্রিয়া চলমান রয়েছে। এসক্রো সিস্টেমের মাধ্যমে আপনার ওয়ালেটে খুব শীঘ্রই অর্থ ক্রেডিট হবে।"
                                }
                                else -> {
                                    "সমাধানকারী যুক্ত হওয়ার পূর্বেই কাজটি বাতিল হওয়ায় কোনো অর্থ কর্তন করা হয়নি এবং আপনার ওয়ালেট ব্যালেন্স সম্পূর্ণ সুরক্ষিত রয়েছে।"
                                }
                            },
                            fontSize = 11.5.sp,
                            color = SomadhanTextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Download Receipt Button
            Button(
                onClick = {
                    val receiptFile = InstantJobReceiptUtil.generateInstantJobReceipt(
                        context = context,
                        problem = problem,
                        otherUser = solverUser,
                        selfUser = currentUser,
                        isUserRole = isUserRole
                    )
                    if (receiptFile != null) {
                        Toast.makeText(context, "রিসিট ডাউনলোড হয়েছে (${receiptFile.name})", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "রিসিট তৈরিতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SomadhanOrangeLight,
                    contentColor = SomadhanOrange
                ),
                border = BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("user_cancelled_receipt_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = SomadhanOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "রিসিট ডাউনলোড করুন (PDF)",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanOrange
                )
            }

            // Dismiss Button
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .navigationBarsPadding()
                    .testTag("user_cancelled_dismiss_button")
            ) {
                Text(
                    text = "ফিরে যান",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ==========================================
// 3. SOLVER CANCELLED DETAIL BOTTOM SHEET
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolverCancelledDetailBottomSheet(
    item: SolverCancelledHistoryItem,
    currentUser: UserEntity?,
    allUsers: List<UserEntity>,
    transactions: List<TransactionEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val problem = item.problem

    // Look up refund transaction if available
    val refundTx = remember(problem.id, transactions) {
        transactions.firstOrNull { it.problemId == problem.id && it.type == "REFUND" }
    }

    val amount = item.bid?.amount
        ?: refundTx?.baseAmount?.takeIf { it > 0.0 }
        ?: refundTx?.grossAmount?.takeIf { it > 0.0 }
        ?: problem.acceptedAmount?.takeIf { it > 0.0 }
        ?: problem.minBudget.takeIf { it > 0.0 }
        ?: problem.maxBudget.takeIf { it > 0.0 }
        ?: 0.0
    val formattedDate = formatHistoryTimestamp(item.timestamp)

    val clientUser = remember(problem.userId, allUsers) {
        allUsers.find { it.id == problem.userId }
    }
    val clientName = problem.userName ?: clientUser?.name ?: "সম্মানিত গ্রাহক"
    val isUserRole = problem.userId == currentUser?.id

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SomadhanCardBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.somadhan_app_icon_1786820904533),
                        contentDescription = "সমাধান লোগো",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column {
                        Text(
                            text = "বাতিলকৃত কাজের বিবরণ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "সমাধান (Somadhan) • বাতিলকৃত চেষ্টার টাইমলাইন",
                            fontSize = 11.5.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_solver_cancelled_detail")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = SomadhanTextSecondary
                    )
                }
            }

            // Headline & Category Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SomadhanBg,
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SomadhanOrangeLight.copy(alpha = 0.5f),
                            border = BorderStroke(0.6.dp, SomadhanOrange.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = problem.categoryName.ifBlank { "জরুরি সেবা" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanOrange,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = formattedDate,
                            fontSize = 11.5.sp,
                            color = SomadhanTextSecondary
                        )
                    }

                    Text(
                        text = problem.title.ifBlank { "জরুরি সেবা অনুরোধ" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    if (problem.description.isNotBlank()) {
                        Text(
                            text = problem.description,
                            fontSize = 12.5.sp,
                            color = SomadhanTextSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF3F4F6))
                            .border(0.6.dp, Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(problem.id))
                                Toast.makeText(context, "পোস্ট ID কপি করা হয়েছে: ${problem.id}", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            tint = SomadhanTextHint,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "পোস্ট ID: ${problem.id.take(8)}...",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
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

            // Cancellation Notice Banner / Option A Note
            if (item.reCompletedBySameSolver) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SomadhanSuccessLight,
                    border = BorderStroke(1.dp, SomadhanSuccess.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SomadhanSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "পুনরায় সফলভাবে সম্পন্ন হয়েছে",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanSuccess
                            )
                            Text(
                                text = "আপনি একবার এই কাজ বাতিল করেছিলেন, পরে আবার বিড দিয়ে সম্পন্ন করেছেন।",
                                fontSize = 11.5.sp,
                                color = SomadhanTextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SomadhanErrorLight,
                    border = BorderStroke(1.dp, SomadhanError.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = SomadhanError,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "আপনি এই কাজটি বাতিল করেছিলেন",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanError
                            )
                            Text(
                                text = "নির্ধারিত সময়ের পর বা অপারগতার কারণে কাজটি বাতিল করা হয়েছিল।",
                                fontSize = 11.5.sp,
                                color = SomadhanTextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Bid Amount & Client Summary Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SomadhanBg,
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "আপনার প্রস্তাবিত পারিশ্রমিক",
                            fontSize = 11.5.sp,
                            color = SomadhanTextSecondary
                        )
                        Text(
                            text = "৳ ${DistanceUtil.toBengaliDigits(amount.toInt().toString())}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "সেবাগ্রহীতা",
                                fontSize = 11.5.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = clientName,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanTextPrimary
                            )
                            if (!problem.userId.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        clipboardManager.setText(AnnotatedString(problem.userId))
                                        Toast.makeText(context, "গ্রাহক ID কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(
                                        text = "আইডি: ${problem.userId.take(12)}...",
                                        fontSize = 10.5.sp,
                                        color = SomadhanTextHint
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
                        UserAvatar(
                            photoUri = clientUser?.profileImageUri,
                            name = clientName,
                            size = 38.dp,
                            backgroundColor = SomadhanSuccessLight,
                            textColor = SomadhanSuccess,
                            fontSize = 14.sp,
                            borderColor = SomadhanSuccess.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Timeline Breakdown with Checkmarks up to progressAtCancel
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "বাতিলের সময় কাজের অগ্রগতি ধাপ",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                val timelineSteps = listOf(
                    1 to "বিড গ্রহণ / কাজ বরাদ্দ",
                    2 to "রওনা দিয়েছেন",
                    3 to "পৌঁছেছেন",
                    4 to "কাজ শুরু করেছেন",
                    5 to "সম্পন্ন"
                )

                timelineSteps.forEach { (stepNumber, stepLabel) ->
                    val isCompleted = stepNumber < item.progressAtCancel
                    val isCancelledAtThisStep = stepNumber == item.progressAtCancel

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isCancelledAtThisStep -> Color(0xFFFEE2E2)
                                    isCompleted -> SomadhanSuccessLight.copy(alpha = 0.5f)
                                    else -> Color.Transparent
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCancelledAtThisStep -> SomadhanError
                                        isCompleted -> SomadhanSuccess
                                        else -> Color(0xFFE5E7EB)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isCancelledAtThisStep -> Icons.Default.Close
                                    isCompleted -> Icons.Default.Check
                                    else -> Icons.Default.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = if (isCancelledAtThisStep || isCompleted) Color.White else SomadhanTextHint,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ধাপ ${DistanceUtil.toBengaliDigits(stepNumber.toString())}: $stepLabel",
                                fontSize = 13.sp,
                                fontWeight = if (isCancelledAtThisStep || isCompleted) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isCancelledAtThisStep -> SomadhanError
                                    isCompleted -> SomadhanTextPrimary
                                    else -> SomadhanTextSecondary
                                }
                            )
                            if (isCancelledAtThisStep) {
                                Text(
                                    text = "এই ধাপে পৌঁছানোর পর কাজটি বাতিল করা হয়েছিল",
                                    fontSize = 11.sp,
                                    color = SomadhanError,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Download Receipt Button
            Button(
                onClick = {
                    val receiptFile = InstantJobReceiptUtil.generateInstantJobReceipt(
                        context = context,
                        problem = problem,
                        otherUser = clientUser,
                        selfUser = currentUser,
                        isUserRole = isUserRole
                    )
                    if (receiptFile != null) {
                        Toast.makeText(context, "রিসিট ডাউনলোড হয়েছে (${receiptFile.name})", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "রিসিট তৈরিতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SomadhanOrangeLight,
                    contentColor = SomadhanOrange
                ),
                border = BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("solver_cancelled_receipt_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = SomadhanOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "রিসিট ডাউনলোড করুন (PDF)",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanOrange
                )
            }

            // Dismiss Button
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .navigationBarsPadding()
                    .testTag("solver_cancelled_detail_dismiss_button")
            ) {
                Text(
                    text = "ফিরে যান",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ==========================================
// 4. DISPUTE SUMMARY BOTTOM SHEET
// ==========================================

private data class HistoryDisputeStepItem(
    val label: String,
    val isDone: Boolean,
    val icon: ImageVector,
    val iconTint: Color,
    val isVerdict: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisputeSummaryBottomSheet(
    problem: ProblemEntity,
    currentUser: UserEntity?,
    allUsers: List<UserEntity>,
    viewModel: SomadhanViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentUserId = currentUser?.id ?: ""
    val isUserRole = (currentUserId.isNotBlank() && currentUserId == problem.userId) ||
            (currentUser?.role?.equals("USER", ignoreCase = true) == true && currentUserId != problem.acceptedSolverId)

    val otherUserId = if (isUserRole) problem.acceptedSolverId else problem.userId
    val otherUser = remember(otherUserId, allUsers) {
        allUsers.find { it.id == otherUserId }
    }

    val baseAmount = problem.acceptedAmount ?: problem.minBudget ?: 0.0
    val extraAmount = if (problem.releaseRequestExtraAmount > 0.0) {
        problem.releaseRequestExtraAmount
    } else {
        problem.confirmedExtraAmountTotal
    }
    val totalEscrow = baseAmount + extraAmount

    val decision = problem.disputeResolutionType ?: problem.disputeResolutionDecision ?: "REFUND_TO_USER"
    val isSplit = decision == "SPLIT_SETTLEMENT" || decision == "SPLIT_50_50" || decision == "CUSTOM_SPLIT" || decision == "SETTLE" || decision == "SPLIT"

    val solverPct = problem.disputeSplitSolverPercent ?: 50.0
    val clientPct = (100.0 - solverPct).coerceAtLeast(0.0)
    val solverGrossBase = Math.round(baseAmount * (solverPct / 100.0)).toDouble()
    val solverGrossExtra = Math.round(extraAmount * (solverPct / 100.0)).toDouble()
    val solverGrossShare = solverGrossBase + solverGrossExtra
    val clientRefund = Math.round(totalEscrow - solverGrossShare).toDouble()

    val reachedStep = problem.disputeProgressAtSettlement ?: problem.disputeProgressAtRaise ?: problem.calculateProgressStep()

    var commissionBreakdown by remember(problem.id, decision, baseAmount, extraAmount) {
        mutableStateOf<SomadhanRepository.CommissionBreakdown?>(null)
    }

    LaunchedEffect(problem.id, decision, baseAmount, extraAmount, isUserRole) {
        val sId = problem.acceptedSolverId ?: ""
        if (isSplit) {
            commissionBreakdown = viewModel.previewCommissionBreakdown(problem, sId, solverGrossBase, solverGrossExtra)
        } else if (decision == "RELEASE_TO_SOLVER" || decision == "RELEASE") {
            commissionBreakdown = viewModel.previewCommissionBreakdown(problem, sId, baseAmount, extraAmount)
        }
    }

    val (decisionTitle, decisionDesc, decisionColor, decisionIcon) = when (decision) {
        "RELEASE_TO_SOLVER", "RELEASE" -> {
            if (!isUserRole) {
                HistoryDisputeQuadruple(
                    "আপনি বিজয়ী! অর্থ রিলিজ করা হয়েছে 🎉",
                    "অ্যাডমিন আপনার পক্ষে রায় দিয়েছেন। প্ল্যাটফর্ম ফি সমন্বয় শেষে সম্পূর্ণ অর্থ আপনার অ্যাকাউন্টে যোগ করা হয়েছে।",
                    SomadhanSuccess,
                    Icons.Default.CheckCircle
                )
            } else {
                HistoryDisputeQuadruple(
                    "বিরোধ নিষ্পত্তি: অর্থ রিলিজ ⚖️",
                    "কাজের অগ্রগতি পর্যালোচনার ভিত্তিতে সমাধানকারীকে অর্থ রিলিজের সিদ্ধান্ত দেওয়া হয়েছে।",
                    Color(0xFFE65100),
                    Icons.Default.Gavel
                )
            }
        }
        "REFUND_TO_USER", "REFUND" -> {
            if (isUserRole) {
                HistoryDisputeQuadruple(
                    "রিফান্ড সফল! আপনি বিজয়ী 💰",
                    "অ্যাডমিন আপনার পক্ষে সিদ্ধান্ত দিয়েছেন। সম্পূর্ণ অর্থ আপনার ওয়ালেটে ফেরত দেওয়া হয়েছে।",
                    SomadhanSuccess,
                    Icons.Default.CheckCircle
                )
            } else {
                HistoryDisputeQuadruple(
                    "বিরোধ নিষ্পত্তি: অর্থ রিফান্ড ⚖️",
                    "অ্যাডমিন পর্যালোচনার ভিত্তিতে ক্লায়েন্টকে সম্পূর্ণ অর্থ রিফান্ডের রায় দিয়েছেন।",
                    SomadhanError,
                    Icons.AutoMirrored.Filled.Undo
                )
            }
        }
        "SPLIT_SETTLEMENT", "CUSTOM_SPLIT", "SPLIT_50_50", "SETTLE", "SPLIT" -> {
            val myAmt = if (isUserRole) clientRefund.toInt() else solverGrossShare.toInt()
            val myPct = if (isUserRole) clientPct.toInt() else solverPct.toInt()
            HistoryDisputeQuadruple(
                "উভয় পক্ষের সমঝোতা সম্পন্ন ⚖️",
                "অ্যাডমিনের মধ্যস্থতায় কাজের অগ্রগতি অনুযায়ী অর্থ ভাগাভাগি করা হয়েছে। আপনার প্রাপ্ত অংশ: ৳ ${DistanceUtil.toBengaliDigits(myAmt.toString())} (${DistanceUtil.toBengaliDigits(myPct.toString())}%)।",
                SomadhanInfo,
                Icons.Default.Balance
            )
        }
        else -> {
            HistoryDisputeQuadruple(
                "বিরোধ নিষ্পত্তি সম্পন্ন ⚖️",
                "অ্যাডমিন প্যানেল থেকে এই সমস্যার বিরোধ নিষ্পত্তি করা হয়েছে।",
                SomadhanTextPrimary,
                Icons.Default.Info
            )
        }
    }

    val resolutionLabel = when (decision) {
        "RELEASE_TO_SOLVER", "RELEASE" -> "৭. অ্যাডমিন কর্তৃক অর্থ রিলিজ করা হয়েছে"
        "REFUND_TO_USER", "REFUND" -> "৭. অ্যাডমিন কর্তৃক অর্থ রিফান্ড করা হয়েছে"
        "SPLIT_SETTLEMENT", "CUSTOM_SPLIT", "SPLIT_50_50", "SETTLE", "SPLIT" -> "৭. অ্যাডমিন কর্তৃক অর্থ ভাগাভাগি (স্প্লিট) করা হয়েছে"
        else -> "৭. বিরোধ নিষ্পত্তি সম্পন্ন"
    }

    val disputeProgressSteps = remember(reachedStep, decision, decisionColor, decisionIcon) {
        val baseSteps = listOf(
            "১. জরুরি ব্রডকাস্ট ও সংযোগ" to 1,
            "২. সমাধানকারী রওয়ানা হয়েছেন" to 2,
            "৩. লোকেশনে পৌঁছেছেন" to 3,
            "৪. কাজ শুরু হয়েছে" to 4,
            "৫. কাজ সম্পন্ন" to 5
        ).map { (label, stepNum) ->
            val done = stepNum <= reachedStep
            HistoryDisputeStepItem(
                label = label,
                isDone = done,
                icon = if (done) Icons.Default.CheckCircle else Icons.Default.Cancel,
                iconTint = if (done) SomadhanSuccess else SomadhanError.copy(alpha = 0.5f)
            )
        }

        val disputeStep = HistoryDisputeStepItem(
            label = "৬. বিরোধ উত্থাপিত হয়েছে",
            isDone = true,
            icon = Icons.Default.Warning,
            iconTint = Color(0xFFF59E0B)
        )

        val verdictStep = HistoryDisputeStepItem(
            label = resolutionLabel,
            isDone = true,
            icon = decisionIcon,
            iconTint = decisionColor,
            isVerdict = true
        )

        baseSteps + disputeStep + verdictStep
    }

    val resolvedTimeFormatted = remember(problem.disputeResolvedAt) {
        val ts = problem.disputeResolvedAt ?: System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault())
        DistanceUtil.toBengaliDigits(sdf.format(Date(ts)))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SomadhanCardBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.somadhan_app_icon_1786820904533),
                        contentDescription = "সমাধান লোগো",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column {
                        Text(
                            text = "বিরোধ নিষ্পত্তির বিবরণ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "সমাধান (Somadhan) • অ্যাডমিন মীমাংসা ও হিসাব",
                            fontSize = 11.5.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_dispute_summary")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = SomadhanTextSecondary
                    )
                }
            }

            // 1. Hero Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = decisionColor.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, decisionColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().testTag("dispute_summary_decision_banner")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(decisionColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = decisionIcon,
                                contentDescription = null,
                                tint = decisionColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "চূড়ান্ত অ্যাডমিন রায়",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = decisionColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = decisionTitle,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = decisionColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = decisionDesc,
                        fontSize = 12.5.sp,
                        color = SomadhanTextPrimary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = decisionColor.copy(alpha = 0.2f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = problem.title.ifBlank { "জরুরি সমস্যা সমাধান" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ক্যাটাগরি: ${problem.categoryName.ifBlank { "সাধারণ" }}",
                            fontSize = 11.5.sp,
                            color = SomadhanTextSecondary
                        )

                        // Post ID Chip
                        val cleanPostId = problem.id
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                                .border(0.6.dp, Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(cleanPostId))
                                    Toast.makeText(context, "পোস্ট ID কপি করা হয়েছে: $cleanPostId", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                tint = SomadhanTextHint,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "ID: ${cleanPostId.take(8)}...",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
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

            // 2. Partial Progress & Stage Checklist
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth().testTag("dispute_summary_progress_checklist")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "কাজের অগ্রগতি ও বিরোধকালীন স্টেজ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SomadhanTextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SomadhanOrangeLight
                        ) {
                            Text(
                                text = "ধাপ ${DistanceUtil.toBengaliDigits(reachedStep.toString())}/৫",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrange,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    disputeProgressSteps.forEach { step ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.5.dp)
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = step.iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = step.label,
                                fontSize = 13.sp,
                                fontWeight = if (step.isVerdict) FontWeight.SemiBold else if (step.isDone) FontWeight.Medium else FontWeight.Normal,
                                color = if (step.isDone) SomadhanTextPrimary else SomadhanTextHint
                            )
                        }
                    }
                }
            }

            // 3. Dispute Reason Card
            if (!problem.disputeReason.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SomadhanErrorLight.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, SomadhanError.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().testTag("dispute_summary_reason_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = SomadhanError,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "উত্থাপিত বিরোধের কারণ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = SomadhanError
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = problem.disputeReason ?: "",
                            fontSize = 12.5.sp,
                            color = SomadhanTextPrimary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // 4. Admin Verdict & Resolution Notes
            if (!problem.disputeResolutionNote.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                    border = BorderStroke(1.dp, SomadhanBorder),
                    modifier = Modifier.fillMaxWidth().testTag("dispute_summary_admin_note_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "অ্যাডমিনের পর্যবেক্ষণ ও মীমাংসা নোট",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = SomadhanTextPrimary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = problem.disputeResolutionNote ?: "",
                            fontSize = 12.5.sp,
                            color = SomadhanTextSecondary,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "মীমাংসার সময়: $resolvedTimeFormatted",
                            fontSize = 11.sp,
                            color = SomadhanTextHint
                        )
                    }
                }
            }

            // 5. Opposite Party Profile Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isUserRole) "বিবাদী/সমাধানকারীর তথ্য" else "সেবাগ্রহীতার তথ্য",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.5f))

                    val name = if (isUserRole) {
                        problem.acceptedSolverName ?: otherUser?.name ?: "দক্ষ সমাধানকারী"
                    } else {
                        problem.userName ?: otherUser?.name ?: "সম্মানিত গ্রাহক"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UserAvatar(
                            photoUri = otherUser?.profileImageUri,
                            name = name,
                            size = 44.dp,
                            backgroundColor = SomadhanInfoLight,
                            textColor = SomadhanInfo,
                            fontSize = 16.sp,
                            borderColor = SomadhanInfo.copy(alpha = 0.4f)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isUserRole) "সমাধানকারী" else "সেবাগ্রহীতা",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                            if (!otherUserId.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        clipboardManager.setText(AnnotatedString(otherUserId))
                                        Toast.makeText(context, "ব্যবহারকারী ID কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(
                                        text = "আইডি: ${otherUserId.take(12)}...",
                                        fontSize = 10.5.sp,
                                        color = SomadhanTextHint
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

            // 6. Comprehensive Financial Distribution & Calculation Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth().testTag("dispute_summary_financial_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "নিষ্পত্তিকৃত অর্থ ও এসক্রো হিসাব বিবরণ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = SomadhanTextPrimary
                    )

                    HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.6f))

                    // Base Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "মূল সার্ভিস বাজেট:",
                            fontSize = 13.sp,
                            color = SomadhanTextSecondary
                        )
                        Text(
                            text = "৳ ${DistanceUtil.toBengaliDigits(baseAmount.toInt().toString())}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanTextPrimary
                        )
                    }

                    // Extra Amount (if applicable)
                    if (extraAmount > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "অতিরিক্ত পার্টস/কাজের চার্জ:",
                                fontSize = 13.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = "+ ৳ ${DistanceUtil.toBengaliDigits(extraAmount.toInt().toString())}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanOrange
                            )
                        }
                    }

                    // Total Escrow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "মোট এসক্রো ফান্ড:",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "৳ ${DistanceUtil.toBengaliDigits(totalEscrow.toInt().toString())}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }

                    HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.4f))

                    // Dynamic Verdict Financial Distribution
                    when (decision) {
                        "RELEASE_TO_SOLVER", "RELEASE" -> {
                            if (!isUserRole) {
                                // Solver View
                                val breakdown = commissionBreakdown
                                val totalGross = totalEscrow
                                val wasFree = breakdown?.wasFreeQuotaJob == true || (breakdown?.totalCommission ?: 0.0) == 0.0
                                val baseComm = breakdown?.baseCommission ?: ((baseAmount * 10.0) / 100.0)
                                val extraComm = breakdown?.extraCommission ?: 0.0
                                val totalComm = breakdown?.totalCommission ?: (baseComm + extraComm)
                                val netAmt = breakdown?.netAmount ?: (totalGross - totalComm)
                                val effRate = breakdown?.rate ?: 10.0

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("গ্রস রিলিজকৃত অর্থ:", fontSize = 13.sp, color = SomadhanTextSecondary)
                                    Text(
                                        text = "৳ ${DistanceUtil.toBengaliDigits(totalGross.toInt().toString())}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanTextPrimary
                                    )
                                }

                                if (wasFree) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("প্ল্যাটফর্ম ফি (ফ্রি কোটা ০%):", fontSize = 13.sp, color = SomadhanSuccess)
                                        Text("৳ ০ (ফ্রি)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanSuccess)
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("প্ল্যাটফর্ম ফি (${DistanceUtil.toBengaliDigits(effRate.toInt().toString())}%):", fontSize = 13.sp, color = SomadhanError)
                                        Text(
                                            text = "- ৳ ${DistanceUtil.toBengaliDigits(baseComm.toInt().toString())}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SomadhanError
                                        )
                                    }

                                    if (extraAmount > 0.0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "অতিরিক্ত বিলের ফি (${if (extraComm > 0.0) "${DistanceUtil.toBengaliDigits(effRate.toInt().toString())}%" else "০% ছাড়"}):",
                                                fontSize = 13.sp,
                                                color = if (extraComm > 0.0) SomadhanError else SomadhanSuccess
                                            )
                                            Text(
                                                text = if (extraComm > 0.0) "- ৳ ${DistanceUtil.toBengaliDigits(extraComm.toInt().toString())}" else "৳ ০ (ছাড়)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (extraComm > 0.0) SomadhanError else SomadhanSuccess
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("মোট প্ল্যাটফর্ম ফি:", fontSize = 13.sp, color = SomadhanError)
                                        Text(
                                            text = "- ৳ ${DistanceUtil.toBengaliDigits(totalComm.toInt().toString())}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanError
                                        )
                                    }
                                }

                                HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "ওয়ালেটে প্রাপ্ত নিট আয়:",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                    Text(
                                        text = "৳ ${DistanceUtil.toBengaliDigits(netAmt.toInt().toString())}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                }
                            } else {
                                // User View
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("সমাধানকারীকে রিলিজকৃত অর্থ:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                    Text(
                                        text = "৳ ${DistanceUtil.toBengaliDigits(totalEscrow.toInt().toString())}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                        }

                        "REFUND_TO_USER", "REFUND" -> {
                            if (isUserRole) {
                                // User View
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "ওয়ালেটে রিফান্ডকৃত অর্থ (১০০%):",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                    Text(
                                        text = "৳ ${DistanceUtil.toBengaliDigits(totalEscrow.toInt().toString())}",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                }
                            } else {
                                // Solver View
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("গ্রাহককে সম্পূর্ণ রিফান্ড:", fontSize = 13.sp, color = SomadhanTextSecondary)
                                    Text(
                                        text = "৳ ${DistanceUtil.toBengaliDigits(totalEscrow.toInt().toString())}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanTextPrimary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("সমাধানকারীর নিট আয়:", fontSize = 13.sp, color = SomadhanError)
                                    Text(
                                        text = "৳ ০",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanError
                                    )
                                }
                            }
                        }

                        else -> {
                            // Split Settlement
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("সিদ্ধান্ত:", fontSize = 13.sp, color = SomadhanTextSecondary)
                                Text(
                                    text = "সমঝোতা (${DistanceUtil.toBengaliDigits(solverPct.toInt().toString())}% / ${DistanceUtil.toBengaliDigits(clientPct.toInt().toString())}%)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanInfo
                                )
                            }

                            if (!isUserRole) {
                                // Solver View
                                val breakdown = commissionBreakdown
                                val wasFree = breakdown?.wasFreeQuotaJob == true || (breakdown?.totalCommission ?: 0.0) == 0.0
                                val baseComm = breakdown?.baseCommission ?: ((solverGrossBase * 10.0) / 100.0)
                                val extraComm = breakdown?.extraCommission ?: 0.0
                                val totalComm = breakdown?.totalCommission ?: (baseComm + extraComm)
                                val netAmt = breakdown?.netAmount ?: (solverGrossShare - totalComm)
                                val effRate = breakdown?.rate ?: 10.0

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("সমাধানকারীর গ্রস ভাগ (${DistanceUtil.toBengaliDigits(solverPct.toInt().toString())}%):", fontSize = 13.sp, color = SomadhanTextSecondary)
                                    Text(
                                        text = "৳ ${DistanceUtil.toBengaliDigits(solverGrossShare.toInt().toString())}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanTextPrimary
                                    )
                                }

                                if (wasFree) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("প্ল্যাটফর্ম ফি (ফ্রি কোটা ০%):", fontSize = 13.sp, color = SomadhanSuccess)
                                        Text("৳ ০ (ফ্রি)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanSuccess)
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("প্ল্যাটফর্ম ফি (${DistanceUtil.toBengaliDigits(effRate.toInt().toString())}%):", fontSize = 13.sp, color = SomadhanError)
                                        Text(
                                            text = "- ৳ ${DistanceUtil.toBengaliDigits(baseComm.toInt().toString())}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SomadhanError
                                        )
                                    }

                                    if (solverGrossExtra > 0.0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "অতিরিক্ত বিলের ফি (${if (extraComm > 0.0) "${DistanceUtil.toBengaliDigits(effRate.toInt().toString())}%" else "০% ছাড়"}):",
                                                fontSize = 13.sp,
                                                color = if (extraComm > 0.0) SomadhanError else SomadhanSuccess
                                            )
                                            Text(
                                                text = if (extraComm > 0.0) "- ৳ ${DistanceUtil.toBengaliDigits(extraComm.toInt().toString())}" else "৳ ০ (ছাড়)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (extraComm > 0.0) SomadhanError else SomadhanSuccess
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("মোট প্ল্যাটফর্ম ফি:", fontSize = 13.sp, color = SomadhanError)
                                        Text(
                                            text = "- ৳ ${DistanceUtil.toBengaliDigits(totalComm.toInt().toString())}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanError
                                        )
                                    }
                                }

                                HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "ওয়ালেটে প্রাপ্ত নিট অর্থ:",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                    Text(
                                        text = "৳ ${DistanceUtil.toBengaliDigits(netAmt.toInt().toString())}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("গ্রাহকের রিফান্ড অংশ (${DistanceUtil.toBengaliDigits(clientPct.toInt().toString())}%):", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                                    Text(
                                        text = "৳ ${DistanceUtil.toBengaliDigits(clientRefund.toInt().toString())}",
                                        fontSize = 12.5.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                            } else {
                                // User View
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "গ্রাহকের রিফান্ড অংশ (${DistanceUtil.toBengaliDigits(clientPct.toInt().toString())}%):",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                    Text(
                                        text = "৳ ${DistanceUtil.toBengaliDigits(clientRefund.toInt().toString())}",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("সমাধানকারীর অংশ (${DistanceUtil.toBengaliDigits(solverPct.toInt().toString())}%):", fontSize = 13.sp, color = SomadhanTextSecondary)
                                    Text(
                                        text = "৳ ${DistanceUtil.toBengaliDigits(solverGrossShare.toInt().toString())}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanOrange
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Download Receipt Button
            Button(
                onClick = {
                    val receiptFile = InstantJobReceiptUtil.generateInstantJobReceipt(
                        context = context,
                        problem = problem,
                        otherUser = otherUser,
                        selfUser = currentUser,
                        isUserRole = isUserRole,
                        commissionBreakdown = commissionBreakdown
                    )
                    if (receiptFile != null) {
                        Toast.makeText(context, "রিসিট ডাউনলোড হয়েছে (${receiptFile.name})", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "রিসিট তৈরিতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SomadhanOrangeLight,
                    contentColor = SomadhanOrange
                ),
                border = BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("dispute_summary_receipt_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = SomadhanOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "রিসিট ডাউনলোড করুন (PDF)",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanOrange
                )
            }

            // Dismiss Button
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .navigationBarsPadding()
                    .testTag("dispute_summary_dismiss_button")
            ) {
                Text(
                    text = "ফিরে যান",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ==========================================
// CARD COMPONENTS
// ==========================================

/**
 * Specialized Card for Solver Cancelled History.
 */
@Composable
private fun SolverCancelledHistoryCard(
    item: SolverCancelledHistoryItem,
    transactions: List<TransactionEntity> = emptyList(),
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val problem = item.problem
    val refundTx = transactions.firstOrNull { it.problemId == problem.id && it.type == "REFUND" }
    val amount = item.bid?.amount
        ?: refundTx?.baseAmount?.takeIf { it > 0.0 }
        ?: refundTx?.grossAmount?.takeIf { it > 0.0 }
        ?: problem.acceptedAmount?.takeIf { it > 0.0 }
        ?: problem.minBudget.takeIf { it > 0.0 }
        ?: problem.maxBudget.takeIf { it > 0.0 }
        ?: 0.0
    val formattedDate = formatHistoryTimestamp(item.timestamp)
    val clientName = problem.userName.ifBlank { "সম্মানিত গ্রাহক" }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        border = BorderStroke(1.dp, SomadhanBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("solver_cancelled_card_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Category Chip + Cancelled Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SomadhanOrangeLight.copy(alpha = 0.5f),
                    border = BorderStroke(0.6.dp, SomadhanOrange.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = problem.categoryName.ifBlank { "জরুরি সমাধান" },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanOrange
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SomadhanErrorLight,
                    border = BorderStroke(0.6.dp, SomadhanError.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = SomadhanError,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "বাতিল",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanError
                        )
                    }
                }
            }

            // Title
            Text(
                text = problem.title.ifBlank { "জরুরি সেবা অনুরোধ" },
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Option A Clarifying Note if re-completed by the same solver
            if (item.reCompletedBySameSolver) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SomadhanSuccessLight.copy(alpha = 0.5f),
                    border = BorderStroke(0.8.dp, SomadhanSuccess.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SomadhanSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "আপনি একবার এই কাজ বাতিল করেছিলেন, পরে আবার বিড দিয়ে সম্পন্ন করেছেন",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = SomadhanSuccess,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Progress At Cancel Step Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val progressStepText = when (item.progressAtCancel) {
                    1 -> "ধাপ ১: বিড গ্রহণ"
                    2 -> "ধাপ ২: রওনা দিয়েছেন"
                    3 -> "ধাপ ৩: পৌঁছেছেন"
                    4 -> "ধাপ ৪: কাজ শুরু"
                    5 -> "ধাপ ৫: সম্পন্ন"
                    else -> "ধাপ ${DistanceUtil.toBengaliDigits(item.progressAtCancel.toString())}"
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEE2E2),
                    border = BorderStroke(0.6.dp, Color(0xFFFCA5A5))
                ) {
                    Text(
                        text = "বাতিলের সময় কাজের অগ্রগতি: $progressStepText",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanError,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // ID Chip + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Copyable ID
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF3F4F6))
                        .border(0.6.dp, Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(problem.id))
                            Toast.makeText(context, "পোস্ট ID কপি করা হয়েছে: ${problem.id}", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("copy_id_${problem.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "ID: ${problem.id.take(8)}...",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
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

                // Date Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = SomadhanTextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(SomadhanDivider)
            )

            // Bottom Row: Client Name & Bid Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Client Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SomadhanSuccessLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = SomadhanSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = clientName,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "সেবাগ্রহীতা",
                            fontSize = 10.5.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }

                // Amount & Action indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "৳ ${DistanceUtil.toBengaliDigits(amount.toInt().toString())}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextSecondary
                        )
                        Text(
                            text = "আপনার বিড",
                            fontSize = 9.5.sp,
                            color = SomadhanTextHint
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "বিস্তারিত দেখুন",
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual Card in History List for Standard Problem entries.
 */
@Composable
private fun InstantJobHistoryCard(
    problem: ProblemEntity,
    currentUserId: String,
    tabIndex: Int,
    transactions: List<TransactionEntity> = emptyList(),
    allUsers: List<UserEntity> = emptyList(),
    allBids: List<BidEntity> = emptyList(),
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val isUserRole = problem.userId == currentUserId
    val otherPartyUser = remember(problem, allUsers, allBids, isUserRole, tabIndex) {
        if (isUserRole) {
            val relevantCancelledBid = if (tabIndex == 1) {
                allBids.filter { it.problemId == problem.id && (it.status == "CANCELLED" || it.status == "ACCEPTED") }
                    .maxByOrNull { it.createdAt }
            } else null
            val solverId = problem.acceptedSolverId?.takeIf { it.isNotBlank() }
                ?: relevantCancelledBid?.solverId?.takeIf { it.isNotBlank() }
                ?: allBids.find { it.id == problem.acceptedBidId || it.status == "ACCEPTED" }?.solverId
            allUsers.find { it.id == solverId }
        } else {
            allUsers.find { it.id == problem.userId }
        }
    }
    val otherPartyName = if (isUserRole) {
        if (tabIndex == 1) {
            val relevantCancelledBid = remember(problem.id, allBids) {
                allBids.filter { it.problemId == problem.id && (it.status == "CANCELLED" || it.status == "ACCEPTED") }
                    .maxByOrNull { it.createdAt }
            }
            val solverId = problem.acceptedSolverId?.takeIf { it.isNotBlank() }
                ?: relevantCancelledBid?.solverId?.takeIf { it.isNotBlank() }
                ?: ""
            if (solverId.isBlank()) {
                "কোনো সমাধানকারী যুক্ত হননি"
            } else {
                val solverUser = remember(solverId, allUsers) {
                    allUsers.find { it.id == solverId }
                }
                solverUser?.name
                    ?: problem.acceptedSolverName?.takeIf { it.isNotBlank() }
                    ?: relevantCancelledBid?.solverName?.takeIf { it.isNotBlank() }
                    ?: "কোনো সমাধানকারী যুক্ত হননি"
            }
        } else {
            problem.acceptedSolverName ?: "নির্ধারিত সমাধানকারী"
        }
    } else {
        problem.userName ?: "সম্মানিত গ্রাহক"
    }
    val otherPartyRoleLabel = if (isUserRole) "সমাধানকারী" else "সেবাগ্রহীতা"

    val refundTx = if (tabIndex == 1) {
        transactions.firstOrNull { it.problemId == problem.id && it.type == "REFUND" }
    } else null

    val baseAmount = if (tabIndex == 1) {
        refundTx?.baseAmount?.takeIf { it > 0.0 }
            ?: refundTx?.grossAmount?.takeIf { it > 0.0 }
            ?: 0.0
    } else {
        problem.acceptedAmount?.takeIf { it > 0.0 }
            ?: problem.minBudget.takeIf { it > 0.0 }
            ?: problem.maxBudget.takeIf { it > 0.0 }
            ?: 0.0
    }
    val totalAmount = if (tabIndex == 1) {
        refundTx?.grossAmount?.takeIf { it > 0.0 } ?: 0.0
    } else {
        baseAmount + problem.confirmedExtraAmountTotal
    }

    // Format Date / Time
    val timestamp = when (tabIndex) {
        0 -> problem.completedAt ?: problem.createdAt
        1 -> problem.createdAt
        else -> problem.disputeResolvedAt ?: problem.createdAt
    }
    val formattedDate = formatHistoryTimestamp(timestamp)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        border = BorderStroke(1.dp, SomadhanBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_card_${problem.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Category Chip + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip with Bolt
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SomadhanOrangeLight.copy(alpha = 0.5f),
                    border = BorderStroke(0.6.dp, SomadhanOrange.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = problem.categoryName.ifBlank { "জরুরি সমাধান" },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanOrange
                        )
                    }
                }

                // Status Badge
                HistoryStatusBadge(problem = problem, tabIndex = tabIndex)
            }

            // Title
            Text(
                text = problem.title.ifBlank { "জরুরি সেবা অনুরোধ" },
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // ID Chip + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Copyable ID
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF3F4F6))
                        .border(0.6.dp, Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(problem.id))
                            Toast.makeText(context, "পোস্ট ID কপি করা হয়েছে: ${problem.id}", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("copy_id_${problem.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "ID: ${problem.id.take(8)}...",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
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

                // Date Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = SomadhanTextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(SomadhanDivider)
            )

            // Bottom Row: Other Party & Price Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Other Party Name + Role
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    UserAvatar(
                        photoUri = otherPartyUser?.profileImageUri,
                        name = otherPartyName,
                        size = 32.dp,
                        backgroundColor = if (isUserRole) SomadhanInfoLight else SomadhanSuccessLight,
                        textColor = if (isUserRole) SomadhanInfo else SomadhanSuccess,
                        fontSize = 12.sp,
                        borderColor = if (isUserRole) SomadhanInfo.copy(alpha = 0.3f) else SomadhanSuccess.copy(alpha = 0.3f)
                    )
                    Column {
                        Text(
                            text = otherPartyName,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = otherPartyRoleLabel,
                            fontSize = 10.5.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }

                // Amount & Forward Arrow
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (tabIndex == 1) {
                            if (refundTx != null && totalAmount > 0.0) {
                                Text(
                                    text = "৳ ${DistanceUtil.toBengaliDigits(totalAmount.toInt().toString())}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextSecondary
                                )
                                Text(
                                    text = "রিফান্ড সম্পন্ন",
                                    fontSize = 9.5.sp,
                                    color = SomadhanSuccess
                                )
                            } else {
                                Text(
                                    text = "কোনো অর্থ কাটা হয়নি",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanTextHint
                                )
                            }
                        } else {
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(totalAmount.toInt().toString())}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tabIndex == 2) SomadhanOrange else SomadhanSuccess
                            )
                            if (problem.confirmedExtraAmountTotal > 0) {
                                Text(
                                    text = "+${DistanceUtil.toBengaliDigits(problem.confirmedExtraAmountTotal.toInt().toString())} অতিরিক্ত",
                                    fontSize = 9.5.sp,
                                    color = SomadhanOrange
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "বিস্তারিত দেখুন",
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * ব্যাচ ৩১, সেশন ২.২ — [InstantJobHistoryCard]/[SolverCancelledHistoryCard] দুটোরই real
 * layout-এর সাথে মেলানো skeleton (উভয় কার্ডের কাঠামো কার্যত একই: chip+badge হেডার-রো, title,
 * id-chip+date রো, ডিভাইডার, তারপর অ্যাভাটার+নাম+amount বটম-রো)। শুধু এই একটা shape দিয়েই
 * দুই ট্যাবের skeleton কভার হয়।
 */
@Composable
private fun HistoryCardSkeleton(tint: Color = SomadhanOrange) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        border = BorderStroke(1.dp, SomadhanBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: category chip + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBlock(modifier = Modifier.width(92.dp).height(20.dp), cornerRadius = 8.dp, tint = tint)
                ShimmerBlock(modifier = Modifier.width(64.dp).height(20.dp), cornerRadius = 8.dp, tint = tint)
            }
            // Title
            ShimmerBlock(modifier = Modifier.fillMaxWidth(0.75f).height(16.dp), tint = tint)
            // ID chip + date row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBlock(modifier = Modifier.width(100.dp).height(16.dp), cornerRadius = 6.dp, tint = tint)
                ShimmerBlock(modifier = Modifier.width(72.dp).height(14.dp), cornerRadius = 6.dp, tint = tint)
            }
            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(SomadhanDivider)
            )
            // Bottom row: avatar + name/role + amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ShimmerBlock(
                        modifier = Modifier.size(32.dp),
                        cornerRadius = 16.dp,
                        tint = tint
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ShimmerBlock(modifier = Modifier.width(90.dp).height(13.dp), cornerRadius = 6.dp, tint = tint)
                        ShimmerBlock(modifier = Modifier.width(60.dp).height(11.dp), cornerRadius = 6.dp, tint = tint)
                    }
                }
                ShimmerBlock(modifier = Modifier.width(56.dp).height(18.dp), cornerRadius = 6.dp, tint = tint)
            }
        }
    }
}

/**
 * Status Badge for History Cards.
 */
@Composable
private fun HistoryStatusBadge(
    problem: ProblemEntity,
    tabIndex: Int
) {
    val (label, bg, fg, icon) = when (tabIndex) {
        0 -> QuadrupleBadge("সম্পন্ন", SomadhanSuccessLight, SomadhanSuccess, Icons.Default.CheckCircle)
        1 -> QuadrupleBadge("বাতিল", SomadhanErrorLight, SomadhanError, Icons.Default.Block)
        else -> {
            if (problem.disputeResolvedAt != null || !problem.disputeResolutionDecision.isNullOrBlank()) {
                val decisionText = when (problem.disputeResolutionDecision) {
                    "RELEASE" -> "মীমাংসিত (রিলিজ)"
                    "REFUND" -> "মীমাংসিত (রিফান্ড)"
                    "SPLIT" -> "মীমাংসিত (স্প্লিট)"
                    else -> "মীমাংসিত"
                }
                QuadrupleBadge(decisionText, SomadhanInfoLight, SomadhanInfo, Icons.Default.Gavel)
            } else {
                QuadrupleBadge("বিরোধ চলমান", SomadhanOrangeLight, SomadhanOrange, Icons.Default.Warning)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(0.6.dp, fg.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}

private data class QuadrupleBadge(
    val label: String,
    val bg: Color,
    val fg: Color,
    val icon: ImageVector
)

private data class HistoryDisputeQuadruple(
    val title: String,
    val desc: String,
    val color: Color,
    val icon: ImageVector
)

/**
 * Empty State for when no records exist in the selected tab.
 */
@Composable
private fun HistoryEmptyState(
    tabIndex: Int,
    modifier: Modifier = Modifier
) {
    val (title, desc, icon, color) = when (tabIndex) {
        0 -> QuadrupleEmpty(
            "কোনো সম্পন্ন কাজের রেকর্ড নেই",
            "আপনার সম্পন্নকৃত সকল জরুরি কাজের হিসাব ও সামারি এখানে দেখতে পাবেন।",
            Icons.Default.CheckCircle,
            SomadhanSuccess
        )
        1 -> QuadrupleEmpty(
            "কোনো বাতিল কাজের রেকর্ড নেই",
            "বাতিলকৃত অথবা সময়োত্তীর্ণ জরুরি কাজের বিবরণ এখানে সংরক্ষিত থাকে।",
            Icons.Default.Block,
            SomadhanError
        )
        else -> QuadrupleEmpty(
            "কোনো বিরোধ সংক্রান্ত রেকর্ড নেই",
            "ডিসপিউট ও অ্যাডমিন নিষ্পত্তিকৃত কাজের সকল তথ্য এখানে তালিকাভুক্ত হবে।",
            Icons.Default.Gavel,
            SomadhanOrange
        )
    }

    Column(
        modifier = modifier.testTag("history_empty_state_$tabIndex"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = SomadhanTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = desc,
            fontSize = 13.sp,
            color = SomadhanTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

private data class QuadrupleEmpty(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val color: Color
)

private fun formatHistoryTimestamp(ts: Long?): String {
    if (ts == null || ts <= 0L) return "—"
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
    val formatted = sdf.format(Date(ts))
    val englishMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val banglaMonths = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
    var result = formatted
    for (i in englishMonths.indices) {
        result = result.replace(englishMonths[i], banglaMonths[i])
    }
    return DistanceUtil.toBengaliDigits(result)
}

private fun formatTimelineBengali(ts: Long?): String {
    if (ts == null || ts <= 0L) return "—"
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
    val formatted = sdf.format(Date(ts))
    val englishMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val banglaMonths = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
    var result = formatted
    for (i in englishMonths.indices) {
        result = result.replace(englishMonths[i], banglaMonths[i])
    }
    return DistanceUtil.toBengaliDigits(result)
}
