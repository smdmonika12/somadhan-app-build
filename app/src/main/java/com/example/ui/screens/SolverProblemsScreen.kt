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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import com.example.ui.components.ProblemCardSkeleton
import com.example.ui.components.LoadingAwareContent
import com.example.ui.components.SyncAwareRefreshableContent
import com.example.ui.components.ProblemCard
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.ui.components.BottomSlideAlertDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverProblemsScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onProblemClick: (String) -> Unit,
    initialTab: Int = 0
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val isLocationUpdating by viewModel.isLocationUpdating.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Loading/Sync Fix Roadmap v2, dhap 4 -- initialSyncPhase (bulk-pull), separate sessionKey "solver_problems_sync"
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialTab.coerceIn(0, 1), pageCount = { 2 })
    var selectedCategoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var showCategoryFilterDialog by remember { mutableStateOf(false) }

    val currentSolverId = currentUser?.id ?: ""

    LaunchedEffect(currentSolverId) {
        if (currentSolverId.isNotBlank()) {
            viewModel.resetSolverProblemsPagination(currentSolverId)
        }
    }

    // Jobs accepted by this solver (acceptedSolverId matches currentUser)
    val activeJobs = remember(allProblems, currentSolverId) {
        if (currentSolverId.isBlank()) emptyList()
        else allProblems.filter { it.acceptedSolverId == currentSolverId && it.status == "IN_PROGRESS" && !it.isUserDeleted }
            .sortedByDescending { it.lastActivityAt ?: it.createdAt }
    }
    val completedJobs = remember(allProblems, currentSolverId) {
        if (currentSolverId.isBlank()) emptyList()
        else allProblems.filter { it.acceptedSolverId == currentSolverId && it.status == "COMPLETED" && !it.isUserDeleted }
            .sortedByDescending { it.lastActivityAt ?: it.createdAt }
    }

    val tabList = if (pagerState.currentPage == 0) activeJobs else completedJobs
    val filteredList = if (!selectedCategoryFilter.isNullOrBlank()) {
        tabList.filter { it.categoryId == selectedCategoryFilter }
    } else {
        tabList
    }

    // Category Filter Dialog (matches Home screen design)
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
                    // All categories option
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
                                text = "(${DistanceUtil.toBengaliDigits(tabList.size.toString())}টি)",
                                fontSize = 12.sp,
                                color = if (isSelected) SomadhanOrange else SomadhanTextSecondary
                            )
                        }
                    }

                    items(allCategories, key = { it.id }) { cat ->
                        val isSelected = selectedCategoryFilter == cat.id
                        val count = tabList.count { it.categoryId == cat.id }
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
                            text = if (pagerState.currentPage == 0) "চলমান কাজগুলো" else "সম্পন্ন কাজগুলো",
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
                    onRefresh = { viewModel.refreshLiveLocation() },
                    isUpdating = isLocationUpdating
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
            // Tabs: চলমান কাজ (Active) & সম্পন্ন কাজ (Completed)
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = SomadhanBg,
                contentColor = SomadhanOrange,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = SomadhanOrange,
                        height = 3.dp
                    )
                },
                divider = {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SomadhanDivider))
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
                            text = "চলমান কাজ (${DistanceUtil.toBengaliDigits(activeJobs.size.toString())})",
                            fontSize = 14.sp,
                            fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (pagerState.currentPage == 0) SomadhanOrange else SomadhanTextSecondary
                        )
                    },
                    modifier = Modifier.testTag("solver_active_jobs_tab")
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
                            text = "সম্পন্ন কাজ (${DistanceUtil.toBengaliDigits(completedJobs.size.toString())})",
                            fontSize = 14.sp,
                            fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (pagerState.currentPage == 1) SomadhanOrange else SomadhanTextSecondary
                        )
                    },
                    modifier = Modifier.testTag("solver_completed_jobs_tab")
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val currentRawList = if (pageIndex == 0) activeJobs else completedJobs
                val currentFilteredList = if (!selectedCategoryFilter.isNullOrBlank()) {
                    currentRawList.filter { it.categoryId == selectedCategoryFilter }
                } else {
                    currentRawList
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SomadhanBg)
                ) {
                    // Clean Header Bar with Right-Aligned Filter Button (matching HomeScreen)
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
                                text = if (pageIndex == 0) {
                                    "মোট চলমান কাজ: ${DistanceUtil.toBengaliDigits(currentFilteredList.size.toString())}টি"
                                } else {
                                    "মোট সম্পন্ন কাজ: ${DistanceUtil.toBengaliDigits(currentFilteredList.size.toString())}টি"
                                },
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
                                    .testTag("solver_category_filter_action_button")
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

                        // Active Filter Removable Pill (if a category is currently filtered)
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
                                    .testTag("clear_solver_category_filter_pill")
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
                                viewModel.resetSolverProblemsPagination(currentSolverId)
                            }
                            viewModel.refreshData()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Loading/Sync Fix Roadmap v2, dhap 7 (batch 3, pagination-screen
                        // migration) -- এতদিন data-বিহীন SyncAwareContent ছিল (rule ১ কভার
                        // করতো, rule ২/৪ ডেটা-ডিফ শিমার ছিল না)। এই স্ক্রিনে (SolverCompletedJobsScreen-এর
                        // মতোই) pagination আসলে dead/stub -- `currentFilteredList` সরাসরি
                        // `activeJobs`/`completedJobs`-এর plain `remember`-filter, page-buffer
                        // না -- তাই কোনো suppression জটিলতা ছাড়াই সরাসরি standard
                        // SyncAwareRefreshableContent প্যাটার্ন প্রযোজ্য।
                        SyncAwareRefreshableContent(
                            sessionKey = "solver_problems_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            data = currentFilteredList,
                            onRetry = { viewModel.retryInitialSync() },
                            isManualRefreshing = isRefreshing,
                            // সেশন ২.৭ (ধাপ ০.১৫, UserProblemsScreen.kt-এর একই ফিক্স) -- ডিফল্ট
                            // ListScreenSkeleton()-এর ভুয়া হেডার-ব্লক (আসল UI-তে প্রতিরূপ নেই)
                            // বাদ দিয়ে সরাসরি real body (LazyColumn + ProblemCard)-এর কাঠামোতেই
                            // ProblemCardSkeleton-এর লিস্ট।
                            skeleton = {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    items(4) { ProblemCardSkeleton(tint = SomadhanOrange) }
                                }
                            }
                        ) { problems ->
                        if (problems.isEmpty() && !viewModel.solverProblemsLoadingMore) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
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
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
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
                                                "এই ক্যাটাগরিতে কোনো কাজ পাওয়া যায়নি।"
                                            } else if (pageIndex == 0) {
                                                "কোনো চলমান কাজ পাওয়া যায়নি।"
                                            } else {
                                                "কোনো সম্পন্ন কাজ পাওয়া যায়নি।"
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
                                items(problems, key = { it.id }) { problem ->
                                    ProblemCard(
                                        problem = problem,
                                        solverLat = currentUser?.latitude,
                                        solverLon = currentUser?.longitude,
                                        onClick = { onProblemClick(problem.id) }
                                    )
                                }

                                // Infinite Scroll Footer
                                item {
                                    if (viewModel.solverProblemsHasMore) {
                                        LaunchedEffect(Unit) {
                                            if (currentSolverId.isNotBlank()) {
                                                viewModel.loadNextSolverProblemsPage(currentSolverId)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (viewModel.solverProblemsLoadingMore) {
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
    }
}

