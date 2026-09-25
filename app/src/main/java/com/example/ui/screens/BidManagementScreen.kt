package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import com.example.data.entity.BidEntity
import com.example.data.entity.ProblemEntity
import com.example.ui.components.ListScreenSkeleton
import com.example.ui.components.LoadingAwareContent
import com.example.ui.components.SyncAwareRefreshableContent
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.StatusBadge
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSurfaceVariant
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.Formatters
import kotlin.math.ceil
import kotlin.math.min
import com.example.ui.components.BottomSlideAlertDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BidManagementScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToProblemDetail: ((String) -> Unit)? = null
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val allBids by viewModel.allBids.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Loading/Sync Fix Roadmap v2, dhap 4 -- initialSyncPhase (bulk-pull), separate sessionKey "bid_management_sync"
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    val isSolver = currentUser?.role == "SOLVER"
    val brandColor = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8)
    val brandLight = if (isSolver) SomadhanOrangeLight else Color(0xFFEFF6FF)

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    var selectedProblemForDetail by remember { mutableStateOf<ProblemEntity?>(null) }
    var showCancelJobConfirmDialog by remember { mutableStateOf(false) }
    var isSubmittingSolverCancel by remember { mutableStateOf(false) }
    var solverCancelError by remember { mutableStateOf<String?>(null) }
    var showReportNoticeDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentUserId = currentUser?.id ?: ""

    // Reset pagination on entry or role change
    LaunchedEffect(currentUserId, isSolver) {
        if (currentUserId.isNotBlank()) {
            if (isSolver) {
                viewModel.resetSolverProblemsPagination(currentUserId)
                viewModel.resetSolverCompletedJobsPagination(currentUserId)
            } else {
                viewModel.resetUserProblemsPagination(currentUserId)
                viewModel.resetUserCompletedProblemsPagination(currentUserId)
            }
        }
    }

    LaunchedEffect(selectedProblemForDetail?.id, isSolver, currentUser?.id) {
        selectedProblemForDetail?.let { prob ->
            val role = when {
                currentUser?.id == prob.userId -> "USER"
                currentUser?.id == prob.acceptedSolverId -> "SOLVER"
                isSolver -> "SOLVER"
                else -> "USER"
            }
            viewModel.markProblemSeen(prob.id, role)
        }
    }

    val inProgressProblems = remember(allProblems, currentUserId, isSolver) {
        if (currentUserId.isBlank()) emptyList()
        else if (isSolver) {
            allProblems.filter { prob ->
                prob.acceptedSolverId == currentUserId &&
                prob.status == "IN_PROGRESS" &&
                !prob.isDisputed &&
                prob.status != "DISPUTED" &&
                !prob.isUserDeleted
            }.sortedByDescending { it.lastActivityAt ?: it.createdAt }
        } else {
            allProblems.filter { prob ->
                prob.userId == currentUserId &&
                prob.status == "IN_PROGRESS" &&
                !prob.acceptedSolverId.isNullOrBlank() &&
                prob.status != "COMPLETED" &&
                prob.status != "CANCELLED" &&
                prob.status != "DISPUTED" &&
                !prob.isDisputed &&
                !prob.isUserDeleted
            }.sortedByDescending { it.lastActivityAt ?: it.createdAt }
        }
    }

    val historyProblems = remember(allProblems, currentUserId, isSolver) {
        if (currentUserId.isBlank()) emptyList()
        else if (isSolver) {
            allProblems.filter { prob ->
                prob.acceptedSolverId == currentUserId &&
                (prob.status == "COMPLETED" || prob.status == "CANCELLED" || prob.status == "DISPUTED" || prob.isDisputed)
            }.sortedByDescending { it.lastActivityAt ?: it.createdAt }
        } else {
            allProblems.filter { prob ->
                prob.userId == currentUserId &&
                (prob.status == "COMPLETED" || prob.status == "CANCELLED" || prob.status == "DISPUTED" || prob.isDisputed)
            }.sortedByDescending { it.lastActivityAt ?: it.createdAt }
        }
    }

    val inProgressHasMore = false
    val inProgressLoadingMore = false
    val historyHasMore = false
    val historyLoadingMore = false

    // Cancellation confirmation dialog for Solver
    if (showCancelJobConfirmDialog && selectedProblemForDetail != null) {
        val prob = selectedProblemForDetail!!
        val matchingBid = allBids.find { it.id == prob.acceptedBidId } ?: BidEntity(
            id = prob.acceptedBidId ?: "BID_${prob.id}",
            problemId = prob.id,
            solverId = prob.acceptedSolverId ?: currentUserId,
            solverName = prob.acceptedSolverName ?: (currentUser?.name ?: "সলভার"),
            solverPhone = currentUser?.phone ?: "",
            amount = prob.acceptedAmount ?: 0.0,
            message = "",
            estimatedTime = "",
            status = "ACCEPTED"
        )

        BottomSlideAlertDialog(
            onDismissRequest = { if (!isSubmittingSolverCancel) showCancelJobConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = SomadhanError,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "কাজ বাতিল নিশ্চিতকরণ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SomadhanTextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "আপনি কি নিশ্চিত যে \"${prob.title}\" কাজটি বাতিল করতে চান?",
                        fontSize = 14.sp,
                        color = SomadhanTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SomadhanErrorLight)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "⚠️ কাজ বাতিল করলে আপনার রেপুটেশন স্কোর থেকে ৩.০ পয়েন্ট হ্রাস পাবে এবং এসক্রো থেকে সম্পূর্ণ অর্থ গ্রাহকের ওয়ালেটে রিফান্ড করা হবে।",
                            fontSize = 12.sp,
                            color = SomadhanError,
                            lineHeight = 17.sp
                        )
                    }
                    if (solverCancelError != null) {
                        Text(text = solverCancelError ?: "", fontSize = 12.sp, color = SomadhanError)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Bug fix (loading-lock): this used to close the dialog and clear
                        // selectedProblemForDetail immediately on click, then fire the cancel as
                        // pure fire-and-forget -- no way to stop a second tap before the first
                        // request landed, and no feedback if it failed. The dialog now stays open
                        // with a spinner until the ViewModel callback confirms the cancel actually
                        // went through (same pattern as JobTrackingScreen's solver-cancel dialog).
                        if (!isSubmittingSolverCancel) {
                            isSubmittingSolverCancel = true
                            solverCancelError = null
                            viewModel.solverCancelAcceptedJob(
                                prob,
                                matchingBid,
                                onSuccess = {
                                    isSubmittingSolverCancel = false
                                    showCancelJobConfirmDialog = false
                                    selectedProblemForDetail = null
                                },
                                onError = { err ->
                                    isSubmittingSolverCancel = false
                                    solverCancelError = err
                                }
                            )
                        }
                    },
                    enabled = !isSubmittingSolverCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSubmittingSolverCancel) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("বাতিল হচ্ছে...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("হ্যাঁ, বাতিল করুন", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelJobConfirmDialog = false; solverCancelError = null },
                    enabled = !isSubmittingSolverCancel
                ) {
                    Text("ফিরে যান", color = SomadhanTextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // User Report Notice Dialog
    if (showReportNoticeDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showReportNoticeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("রিপোর্ট করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "শীঘ্রই সরাসরি রিপোর্ট ফিচার যুক্ত হবে — এই মুহূর্তে যেকোনো সহায়তার জন্য সহায়তা কেন্দ্রে যোগাযোগ করুন।",
                    fontSize = 13.5.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showReportNoticeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ঠিক আছে")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Detail Bottom Sheet
    if (selectedProblemForDetail != null) {
        val prob = selectedProblemForDetail!!
        ModalBottomSheet(
            onDismissRequest = { selectedProblemForDetail = null },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "কাজের বিবরণ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                    val displayStatus = if (prob.isDisputed || prob.status == "DISPUTED") "DISPUTED" else prob.status
                    StatusBadge(status = displayStatus)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = prob.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary,
                    lineHeight = 22.sp
                )

                if (prob.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = prob.description,
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = SomadhanDivider)
                Spacer(modifier = Modifier.height(14.dp))

                // Counterparty and Financial Info Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isSolver) "গ্রাহক:" else "নির্বাচিত সমাধানকারী:",
                                fontSize = 13.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = if (isSolver) prob.userName else (prob.acceptedSolverName ?: "নির্ধারিত নয়"),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }

                        if (prob.acceptedAmount != null && prob.acceptedAmount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "নির্ধারিত বাজেট:",
                                    fontSize = 13.sp,
                                    color = SomadhanTextSecondary
                                )
                                Text(
                                    text = "৳ ${DistanceUtil.toBengaliDigits(prob.acceptedAmount.toInt().toString())}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = brandColor
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ক্যাটাগরি:",
                                fontSize = 13.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = prob.categoryName.ifBlank { prob.categoryId },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = SomadhanTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                // 1. Chat button (always available)
                Button(
                    onClick = {
                        val pid = prob.id
                        selectedProblemForDetail = null
                        onNavigateToChat(pid)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("bid_management_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "চ্যাট করুন",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 2. Solver Cancel Job button (only for Solver on "চলমান" tab)
                if (isSolver && prob.status == "IN_PROGRESS") {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showCancelJobConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanError.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("solver_cancel_job_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            tint = SomadhanError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "কাজ বাতিল করুন",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanError
                        )
                    }
                }

                // 3. "বিস্তারিত দেখুন" (View Details) button for both User & Solver
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val pid = prob.id
                        selectedProblemForDetail = null
                        onNavigateToProblemDetail?.invoke(pid)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = brandColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, brandColor.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("view_details_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = brandColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "বিস্তারিত দেখুন",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = brandColor
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isSolver) "আমার গৃহীত কাজসমূহ" else "বিড ও কাজ ব্যবস্থাপনা",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = SomadhanTextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = SomadhanTextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SomadhanBg),
                    modifier = Modifier.border(1.dp, SomadhanDivider)
                )

                RealtimeLocationBar(
                    locationAddress = liveLocation.address,
                    isSolver = isSolver,
                    onRefresh = { viewModel.refreshLiveLocation() }
                )

                // Tab Row: চলমান (In Progress) and ইতিহাস (History)
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = SomadhanBg,
                    contentColor = brandColor,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = brandColor
                        )
                    }
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        text = {
                            Text(
                                text = "চলমান (${DistanceUtil.toBengaliDigits(inProgressProblems.size.toString())})",
                                fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (pagerState.currentPage == 0) brandColor else SomadhanTextSecondary,
                                fontSize = 13.5.sp
                            )
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        text = {
                            Text(
                                text = "ইতিহাস (${DistanceUtil.toBengaliDigits(historyProblems.size.toString())})",
                                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (pagerState.currentPage == 1) brandColor else SomadhanTextSecondary,
                                fontSize = 13.5.sp
                            )
                        }
                    )
                }
            }
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { pageIndex ->
            val pageProblems = if (pageIndex == 0) inProgressProblems else historyProblems
            val pageHasMore = if (pageIndex == 0) inProgressHasMore else historyHasMore
            val pageLoadingMore = if (pageIndex == 0) inProgressLoadingMore else historyLoadingMore

            SomadhanPullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshData() },
                modifier = Modifier.fillMaxSize()
            ) {
                // Realtime-Aware, Structure-Preserving Refresh, ধাপ ৭ (ব্যাচ ২) — এই
                // স্ক্রিনে pagination আসলে dead/stub (inProgressHasMore/historyHasMore
                // উপরে সবসময় false, ডেটা সরাসরি allProblems.filter থেকে আসা plain
                // immutable list), তাই pagination-suppression নিয়ে কোনো জটিলতা নেই —
                // সরাসরি standard SyncAwareRefreshableContent প্যাটার্ন প্রযোজ্য।
                SyncAwareRefreshableContent(
                    sessionKey = "bid_management_sync",
                    viewModel = viewModel,
                    syncPhase = initialSyncPhase,
                    data = pageProblems,
                    onRetry = { viewModel.retryInitialSync() },
                    modifier = Modifier.fillMaxSize(),
                    isManualRefreshing = isRefreshing
                ) { probs ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SomadhanBg)
                ) {
                    if (probs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(brandLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = brandColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (pageIndex == 0) "বর্তমানে কোনো চলমান কাজ নেই।" else "ইতিহাসে কোনো কাজ পাওয়া যায়নি।",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(probs, key = { it.id }) { problem ->
                                BidManagementJobCard(
                                    problem = problem,
                                    isSolver = isSolver,
                                    brandColor = brandColor,
                                    onClick = {
                                        if (pageIndex == 1) {
                                            // ইতিহাস (History) ট্যাব: সরাসরি Post Details পেজে যাবে,
                                            // মাঝখানে "কাজের বিবরণ" সামারি বটম-শীট দেখানো হবে না।
                                            onNavigateToProblemDetail?.invoke(problem.id)
                                        } else {
                                            // চলমান (In Progress) ট্যাব: আগের মতোই সামারি বটম-শীট
                                            // দেখাবে, যেখান থেকে চ্যাট/বাতিল/বিস্তারিত অপশন পাওয়া যায়।
                                            selectedProblemForDetail = problem
                                        }
                                    }
                                )
                            }

                            // Infinite Scroll Footer
                            if (pageHasMore) {
                                item {
                                    LaunchedEffect(Unit) {
                                        if (pageIndex == 0) {
                                            if (isSolver) {
                                                viewModel.loadNextSolverProblemsPage(currentUserId)
                                            } else {
                                                viewModel.loadNextUserProblemsPage(currentUserId)
                                            }
                                        } else {
                                            if (isSolver) {
                                                viewModel.loadNextSolverCompletedJobsPage(currentUserId)
                                            } else {
                                                viewModel.loadNextUserCompletedProblemsPage(currentUserId)
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (pageLoadingMore) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = brandColor
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun BidManagementJobCard(
    problem: ProblemEntity,
    isSolver: Boolean,
    brandColor: Color,
    onClick: () -> Unit
) {
    val counterpartName = if (isSolver) problem.userName else (problem.acceptedSolverName ?: "সমাধানকারী")
    val amount = problem.acceptedAmount ?: problem.maxBudget

    Card(
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SomadhanBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("bid_management_card_${problem.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = problem.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val cardDisplayStatus = if (problem.isDisputed || problem.status == "DISPUTED") "DISPUTED" else problem.status
                StatusBadge(status = cardDisplayStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSolver) "গ্রাহক: $counterpartName" else "সলভার: $counterpartName",
                        fontSize = 12.5.sp,
                        color = SomadhanTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (amount > 0) {
                    Text(
                        text = "৳ ${DistanceUtil.toBengaliDigits(amount.toInt().toString())}",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = brandColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = problem.categoryName.ifBlank { problem.categoryId },
                    fontSize = 11.5.sp,
                    color = SomadhanTextHint
                )

                Text(
                    text = Formatters.formatTimeAgo(problem.completedAt ?: problem.createdAt),
                    fontSize = 10.5.sp,
                    color = SomadhanTextHint
                )
            }
        }
    }
}
