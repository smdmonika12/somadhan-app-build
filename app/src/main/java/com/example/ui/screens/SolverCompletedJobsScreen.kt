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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.ListScreenSkeleton
import com.example.ui.components.LoadingAwareContent
import com.example.ui.components.SyncAwareRefreshableContent
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.StatusBadge
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.Formatters
import com.example.util.TransactionHelper
import com.example.ui.components.BottomSlideAlertDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverCompletedJobsScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onProblemClick: ((String) -> Unit)? = null
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val userTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()

    // Loading/Sync Fix Roadmap v2, dhap 4 -- initialSyncPhase (bulk-pull), separate sessionKey "solver_completed_jobs_sync"
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    val currentSolverId = currentUser?.id ?: ""

    LaunchedEffect(currentSolverId) {
        if (currentSolverId.isNotBlank()) {
            viewModel.resetSolverCompletedJobsPagination(currentSolverId)
        }
    }

    val rawCompletedJobs = remember(allProblems, currentSolverId) {
        if (currentSolverId.isBlank()) emptyList()
        else allProblems.filter { it.acceptedSolverId == currentSolverId && it.status == "COMPLETED" && !it.isUserDeleted }
            .sortedByDescending { it.lastActivityAt ?: it.createdAt }
    }

    var selectedCategoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var showCategoryFilterDialog by remember { mutableStateOf(false) }

    val completedJobs = if (!selectedCategoryFilter.isNullOrBlank()) {
        rawCompletedJobs.filter { it.categoryId == selectedCategoryFilter }
    } else {
        rawCompletedJobs
    }

    // Category Filter Dialog
    if (showCategoryFilterDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showCategoryFilterDialog = false },
            title = {
                Text(
                    text = "ক্যাটাগরি অনুযায়ী ফিল্টার করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        val isSelected = selectedCategoryFilter == null
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) SomadhanOrangeLight else Color(0xFFF9FAFB))
                                .border(
                                    1.dp,
                                    if (isSelected) SomadhanOrange else Color(0xFFE5E7EB),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedCategoryFilter = null
                                    showCategoryFilterDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "সব ক্যাটাগরি (সব কাজ)",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.5.sp,
                                color = if (isSelected) SomadhanOrange else SomadhanTextPrimary
                            )
                            Text(
                                text = "(${DistanceUtil.toBengaliDigits(rawCompletedJobs.size.toString())}টি)",
                                fontSize = 12.sp,
                                color = if (isSelected) SomadhanOrange else SomadhanTextSecondary
                            )
                        }
                    }

                    items(allCategories.filter { it.isActive }, key = { it.id }) { cat ->
                        val isSelected = selectedCategoryFilter == cat.id
                        val count = rawCompletedJobs.count { it.categoryId == cat.id }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) SomadhanOrangeLight else Color(0xFFF9FAFB))
                                .border(
                                    1.dp,
                                    if (isSelected) SomadhanOrange else Color(0xFFE5E7EB),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedCategoryFilter = cat.id
                                    showCategoryFilterDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                    contentDescription = null,
                                    tint = if (isSelected) SomadhanOrange else SomadhanTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat.nameBangla,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.5.sp,
                                    color = if (isSelected) SomadhanOrange else SomadhanTextPrimary
                                )
                            }
                            Text(
                                text = "(${DistanceUtil.toBengaliDigits(count.toString())}টি)",
                                fontSize = 12.sp,
                                color = if (isSelected) SomadhanOrange else SomadhanTextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryFilterDialog = false }) {
                    Text("বন্ধ করুন", color = SomadhanOrange, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "সম্পন্ন কাজগুলো (${DistanceUtil.toBengaliDigits(completedJobs.size.toString())})",
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
                    actions = {
                        IconButton(
                            onClick = { showCategoryFilterDialog = true },
                            modifier = Modifier.testTag("topbar_category_filter_btn")
                        ) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "ফিল্টার করুন",
                                    tint = if (selectedCategoryFilter != null) SomadhanOrange else SomadhanTextPrimary
                                )
                                if (selectedCategoryFilter != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(SomadhanOrange)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SomadhanBg),
                    modifier = Modifier.border(1.dp, SomadhanDivider)
                )
                RealtimeLocationBar(
                    locationAddress = liveLocation.address,
                    isSolver = true,
                    onRefresh = { viewModel.refreshLiveLocation() }
                )
            }
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SomadhanBg)
        ) {
            // Clean Header Bar with Right-Aligned Filter Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SomadhanBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "মোট সম্পন্ন কাজ: ${DistanceUtil.toBengaliDigits(completedJobs.size.toString())}টি",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    // Right Side Filter Action Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedCategoryFilter != null) SomadhanOrange else Color(0xFFF3F4F6))
                            .border(
                                1.dp,
                                if (selectedCategoryFilter != null) SomadhanOrange else Color(0xFFE5E7EB),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { showCategoryFilterDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("completed_jobs_category_filter_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "ক্যাটাগরি ফিল্টার",
                                tint = if (selectedCategoryFilter != null) Color.White else SomadhanTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (selectedCategoryFilter != null) "ফিল্টারযুক্ত" else "ফিল্টার",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedCategoryFilter != null) Color.White else SomadhanTextSecondary
                            )
                        }
                    }
                }

                // Active Filter Removable Pill
                if (selectedCategoryFilter != null) {
                    val selectedCatName = allCategories.find { it.id == selectedCategoryFilter }?.nameBangla ?: "নির্বাচিত ক্যাটাগরি"
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SomadhanOrangeLight)
                            .border(1.dp, SomadhanOrange.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .clickable {
                                selectedCategoryFilter = null
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ফিল্টার: $selectedCatName",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = SomadhanOrange
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "ফিল্টার সরান",
                            tint = SomadhanOrange,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            SomadhanPullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = {
                    if (currentSolverId.isNotBlank()) {
                        viewModel.resetSolverCompletedJobsPagination(currentSolverId)
                    }
                    viewModel.refreshData()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                // Loading/Sync Fix Roadmap v2, dhap 7 (batch 2, pagination-screen migration) --
                // এতদিন data-বিহীন SyncAwareContent ছিল (rule ১ কভার করতো, rule ২/৪ ডেটা-ডিফ
                // শিমার ছিল না)। এই স্ক্রিনে (BidManagementScreen-এর মতোই) pagination আসলে
                // dead/stub -- `completedJobs` সরাসরি `allProblems.filter { ... }` থেকে আসা
                // plain immutable list (loadNextSolverCompletedJobsPage শুধু bulk-pull-এর পরের
                // পেজ আনে, কিন্তু displayed list সবসময় পুরো locally-synced set-এর filter), তাই
                // pagination-suppression নিয়ে কোনো জটিলতা ছাড়াই সরাসরি standard
                // SyncAwareRefreshableContent প্যাটার্ন প্রযোজ্য।
                SyncAwareRefreshableContent(
                    sessionKey = "solver_completed_jobs_sync",
                    viewModel = viewModel,
                    syncPhase = initialSyncPhase,
                    data = completedJobs,
                    onRetry = { viewModel.retryInitialSync() },
                    isManualRefreshing = isRefreshing
                ) { jobs ->
                if (jobs.isEmpty() && !viewModel.solverCompletedJobsLoadingMore) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = SomadhanTextHint,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (selectedCategoryFilter != null) {
                                        "এই ক্যাটাগরিতে কোনো সম্পন্ন কাজ পাওয়া যায়নি।"
                                    } else {
                                        "এখনও কোনো সম্পন্ন কাজের রেকর্ড নেই।"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(jobs, key = { it.id }) { job ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .border(1.dp, SomadhanDivider, RoundedCornerShape(14.dp))
                                    .clickable(enabled = onProblemClick != null) {
                                        onProblemClick?.invoke(job.id)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = job.categoryName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanOrange
                                        )
                                        StatusBadge(status = job.status)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = job.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = job.description,
                                        fontSize = 12.sp,
                                        color = SomadhanTextSecondary,
                                        maxLines = 2
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("ক্লায়েন্ট: ${job.userName}", fontSize = 12.sp, color = SomadhanTextSecondary)
                                            Text("ঠিকানা: ${job.userAddress}", fontSize = 11.sp, color = SomadhanTextHint)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ৫]
                                            // আগে "type != REFUND" যেকোনো non-refund টাইপ
                                            // (USER-role deduction-সহ) ম্যাচ করতে পারত, এই
                                            // solver-এর আসল আয়ের (PAYMENT, role=SOLVER) বদলে
                                            // ভুল amount দেখানোর ঝুঁকি ছিল -- DashboardScreen-এর
                                            // একই প্যাটার্নের মতোই ঠিক করা হলো।
                                            val trx = userTransactions.find {
                                                it.problemId == job.id && TransactionHelper.matchesRoleForHistory(it, "SOLVER", currentSolverId)
                                            }
                                            val baseAmount = job.acceptedAmount ?: (job.maxBudget ?: 0.0)
                                            val extraAmount = if (job.confirmedExtraAmountTotal > 0.0) job.confirmedExtraAmountTotal else 0.0
                                            val totalGross = if (trx != null) trx.grossAmount else (baseAmount + extraAmount)
                                            val displayAmount = if (trx != null) trx.netAmount else totalGross

                                            if (displayAmount > 0) {
                                                Text(
                                                    text = "আয়: ৳ ${DistanceUtil.toBengaliDigits(displayAmount.toInt().toString())}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SomadhanSuccess
                                                )
                                                if (trx != null && trx.grossAmount > trx.netAmount) {
                                                    Text(
                                                        text = "মোট চুক্তি: ৳${DistanceUtil.toBengaliDigits(trx.grossAmount.toInt().toString())}",
                                                        fontSize = 10.sp,
                                                        color = SomadhanTextHint
                                                    )
                                                } else if (extraAmount > 0.0) {
                                                    Text(
                                                        text = "অতিরিক্ত বিল সহ",
                                                        fontSize = 10.sp,
                                                        color = SomadhanTextHint
                                                    )
                                                }
                                            }
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

                        // Infinite Scroll Footer
                        item {
                            if (viewModel.solverCompletedJobsHasMore) {
                                LaunchedEffect(Unit) {
                                    if (currentSolverId.isNotBlank()) {
                                        viewModel.loadNextSolverCompletedJobsPage(currentSolverId)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (viewModel.solverCompletedJobsLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = SomadhanOrange
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
}

