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
import androidx.compose.foundation.layout.imePadding
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
import com.example.data.entity.ProblemEntity
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
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.ui.components.BottomSlideAlertDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProblemsScreen(
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

    // Tracks whether core reference data has arrived at least once - distinct
    // from currentFilteredList being empty, which can be a genuine "no problems" state.
    // Loading/Sync Fix Roadmap v2, dhap 4 -- initialSyncPhase (bulk-pull), separate sessionKey "user_problems_sync"
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialTab.coerceIn(0, 2), pageCount = { 3 })
    var selectedCategoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var showCategoryFilterDialog by remember { mutableStateOf(false) }
    var problemToDelete by remember { mutableStateOf<ProblemEntity?>(null) }
    var isDeletingProblem by remember { mutableStateOf(false) }
    var deleteProblemError by remember { mutableStateOf<String?>(null) }

    val currentUserId = currentUser?.id ?: ""

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            viewModel.resetUserProblemsPagination(currentUserId)
        }
    }

    // User's own problems
    // ১. চলমান সমস্যা: এখনও বিড গ্রহণ করা হয়নি এমন সমস্যা (OPEN) + যেগুলোতে বিড গ্রহণ করা হয়েছে (IN_PROGRESS)
    val activeProblems = remember(allProblems, currentUserId) {
        if (currentUserId.isBlank()) emptyList()
        else allProblems.filter { problem ->
            problem.userId == currentUserId &&
            (problem.status == "OPEN" || problem.status == "IN_PROGRESS") &&
            problem.status != "COMPLETED" &&
            problem.status != "CANCELLED" &&
            !problem.isUserDeleted
        }.sortedByDescending { it.createdAt }
    }

    // ২. বাতিল সমস্যা: ব্যবহারকারীর বাতিলকৃত সমস্যা সমূহ (সাধারণত জরুরি জব সহ অন্যান্য বাতিলকৃত পোস্ট)
    val cancelledProblems = remember(allProblems, currentUserId) {
        if (currentUserId.isBlank()) emptyList()
        else allProblems.filter { problem ->
            problem.userId == currentUserId &&
            problem.status == "CANCELLED" &&
            !problem.isUserDeleted
        }.sortedByDescending { it.createdAt }
    }

    // ৩. সম্পন্ন সমস্যা:
    val completedProblems = remember(allProblems, currentUserId) {
        if (currentUserId.isBlank()) emptyList()
        else allProblems.filter { problem ->
            problem.userId == currentUserId &&
            problem.status == "COMPLETED" &&
            !problem.isUserDeleted
        }.sortedByDescending { it.createdAt }
    }

    val rawTabList = when (pagerState.currentPage) {
        0 -> activeProblems
        1 -> cancelledProblems
        else -> completedProblems
    }
    val filteredList = if (!selectedCategoryFilter.isNullOrBlank()) {
        rawTabList.filter { it.categoryId == selectedCategoryFilter }
    } else {
        rawTabList
    }

    // Delete Confirmation Dialog
    if (problemToDelete != null) {
        val prob = problemToDelete!!
        BottomSlideAlertDialog(
            onDismissRequest = { if (!isDeletingProblem) problemToDelete = null },
            title = {
                Text(
                    text = "সমস্যাটি ডিলিট করতে চান?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                Text(
                    text = "\"${prob.title}\" পোস্টটি স্থায়ীভাবে মুছে ফেলা হবে এবং এর সকল বিড বাতিল হবে। আপনি কি নিশ্চিত?",
                    fontSize = 14.sp,
                    color = SomadhanTextSecondary
                )
                if (deleteProblemError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = deleteProblemError ?: "", fontSize = 12.sp, color = SomadhanError)
                }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeletingProblem,
                    onClick = {
                        val target = prob
                        isDeletingProblem = true
                        deleteProblemError = null
                        viewModel.userDeleteProblem(
                            problem = target,
                            onSuccess = {
                                isDeletingProblem = false
                                problemToDelete = null
                            },
                            onError = { err ->
                                isDeletingProblem = false
                                deleteProblemError = err
                            }
                        )
                    },
                    modifier = Modifier.testTag("confirm_user_delete_problem_btn")
                ) {
                    if (isDeletingProblem) {
                        CircularProgressIndicator(color = SomadhanError, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ডিলিট হচ্ছে...", color = SomadhanError, fontWeight = FontWeight.Bold)
                    } else {
                        Text("হ্যাঁ, ডিলিট করুন", color = SomadhanError, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !isDeletingProblem, onClick = { problemToDelete = null; deleteProblemError = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
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
                                .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF9FAFB))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF2563EB) else Color(0xFFE5E7EB),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedCategoryFilter = null
                                    showCategoryFilterDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "সব ক্যাটাগরি (সব সমস্যা)",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.5.sp,
                                color = if (isSelected) Color(0xFF2563EB) else SomadhanTextPrimary
                            )
                            Text(
                                text = "(${DistanceUtil.toBengaliDigits(rawTabList.size.toString())}টি)",
                                fontSize = 12.sp,
                                color = if (isSelected) Color(0xFF2563EB) else SomadhanTextSecondary
                            )
                        }
                    }

                    items(allCategories.filter { it.isActive }, key = { it.id }) { cat ->
                        val isSelected = selectedCategoryFilter == cat.id
                        val count = rawTabList.count { it.categoryId == cat.id }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF9FAFB))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF2563EB) else Color(0xFFE5E7EB),
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
                                    tint = if (isSelected) Color(0xFF2563EB) else SomadhanTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat.nameBangla,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.5.sp,
                                    color = if (isSelected) Color(0xFF2563EB) else SomadhanTextPrimary
                                )
                            }
                            Text(
                                text = "(${DistanceUtil.toBengaliDigits(count.toString())}টি)",
                                fontSize = 12.sp,
                                color = if (isSelected) Color(0xFF2563EB) else SomadhanTextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryFilterDialog = false }) {
                    Text("বন্ধ করুন", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = when (pagerState.currentPage) {
                                0 -> "চলমান সমস্যা সমূহ (${DistanceUtil.toBengaliDigits(activeProblems.size.toString())})"
                                1 -> "বাতিলকৃত সমস্যা সমূহ (${DistanceUtil.toBengaliDigits(cancelledProblems.size.toString())})"
                                else -> "সম্পন্ন সমস্যা সমূহ (${DistanceUtil.toBengaliDigits(completedProblems.size.toString())})"
                            },
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
                                    tint = if (selectedCategoryFilter != null) Color(0xFF2563EB) else SomadhanTextPrimary
                                )
                                if (selectedCategoryFilter != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2563EB))
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
                    isSolver = false,
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
            // Tabs: চলমান সমস্যা (Active), বাতিল সমস্যা (Cancelled) & সম্পন্ন সমস্যা (Completed)
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = SomadhanBg,
                contentColor = Color(0xFF2563EB),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = Color(0xFF2563EB),
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
                            text = "চলমান (${DistanceUtil.toBengaliDigits(activeProblems.size.toString())})",
                            fontSize = 13.5.sp,
                            fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (pagerState.currentPage == 0) Color(0xFF2563EB) else SomadhanTextSecondary
                        )
                    },
                    modifier = Modifier.testTag("user_active_problems_tab")
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
                            text = "বাতিল (${DistanceUtil.toBengaliDigits(cancelledProblems.size.toString())})",
                            fontSize = 13.5.sp,
                            fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (pagerState.currentPage == 1) SomadhanError else SomadhanTextSecondary
                        )
                    },
                    modifier = Modifier.testTag("user_cancelled_problems_tab")
                )

                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    },
                    text = {
                        Text(
                            text = "সম্পন্ন (${DistanceUtil.toBengaliDigits(completedProblems.size.toString())})",
                            fontSize = 13.5.sp,
                            fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Medium,
                            color = if (pagerState.currentPage == 2) SomadhanSuccess else SomadhanTextSecondary
                        )
                    },
                    modifier = Modifier.testTag("user_completed_problems_tab")
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val currentRawList = when (pageIndex) {
                    0 -> activeProblems
                    1 -> cancelledProblems
                    else -> completedProblems
                }
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
                    // Header Row with Problem Count and Right-Aligned Filter Button
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
                                text = "মোট সমস্যা: ${DistanceUtil.toBengaliDigits(currentFilteredList.size.toString())}টি",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )

                            // Right Side Filter Action Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedCategoryFilter != null) Color(0xFF2563EB) else Color(0xFFF3F4F6))
                                    .border(
                                        1.dp,
                                        if (selectedCategoryFilter != null) Color(0xFF2563EB) else Color(0xFFE5E7EB),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { showCategoryFilterDialog = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("user_problems_category_filter_btn")
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
                                    .background(Color(0xFFEFF6FF))
                                    .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                    .clickable {
                                        selectedCategoryFilter = null
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "ফিল্টার: $selectedCatName",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2563EB)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "ফিল্টার সরান",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    SomadhanPullToRefresh(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            if (currentUserId.isNotBlank()) {
                                viewModel.resetUserProblemsPagination(currentUserId)
                            }
                            viewModel.refreshData()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Loading/Sync Fix Roadmap v2, dhap 12 (batch4, pagination-screen
                        // migration) -- SolverProblemsScreen-এর প্যাটার্নের মতোই -- এই স্ক্রিনেও
                        // pagination আসলে dead/stub, `currentFilteredList` সরাসরি
                        // activeProblems/cancelledProblems/completedProblems-এর plain
                        // `remember`-filter, page-buffer না -- তাই সরাসরি standard
                        // SyncAwareRefreshableContent প্যাটার্ন প্রযোজ্য।
                        SyncAwareRefreshableContent(
                            sessionKey = "user_problems_sync",
                            viewModel = viewModel,
                            syncPhase = initialSyncPhase,
                            data = currentFilteredList,
                            onRetry = { viewModel.retryInitialSync() },
                            isManualRefreshing = isRefreshing,
                            // সেশন ২.৭ (ধাপ ০.১৫) -- ডিফল্ট ListScreenSkeleton()-এ একটা অতিরিক্ত
                            // ৬৪dp "header" শিমার-ব্লক থাকে যেটার কোনো প্রতিরূপ আসল UI-তে নেই
                            // (এই স্ক্রিনের ট্যাব/ফিল্টার হেডার এই কম্পোনেন্টের বাইরে, সবসময়ই
                            // স্থির/visible থাকে) -- তাই re-entry/pull-to-refresh flash-এ ওই
                            // ভুয়া হেডার-ব্লকটা দেখাত, যেটা আসল পোস্ট-কার্ড লিস্ট layout-এর সাথে
                            // মেলে না। এখানে সরাসরি নিচের real body-র (LazyColumn + ProblemCard)
                            // হুবহু কাঠামোতে (একই contentPadding) শুধু ProblemCardSkeleton-এর
                            // লিস্ট দেখানো হচ্ছে, কোনো অতিরিক্ত header-ব্লক ছাড়া।
                            skeleton = {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    items(4) { ProblemCardSkeleton(tint = Color(0xFF1D4ED8)) }
                                }
                            }
                        ) { problems ->
                        if (problems.isEmpty() && !viewModel.userProblemsLoadingMore) {
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
                                                "এই ক্যাটাগরিতে কোনো সমস্যা পাওয়া যায়নি।"
                                            } else when (pageIndex) {
                                                0 -> "কোনো চলমান সমস্যা পাওয়া যায়নি।"
                                                1 -> "কোনো বাতিলকৃত সমস্যা পাওয়া যায়নি।"
                                                else -> "কোনো সম্পন্ন সমস্যা পাওয়া যায়নি।"
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
                                    val isDeletable = problem.userId == currentUser?.id &&
                                        problem.status == "OPEN" &&
                                        problem.acceptedBidId == null &&
                                        (problem.acceptedSolverId == null || problem.acceptedSolverId.isBlank())

                                    ProblemCard(
                                        problem = problem,
                                        accentColor = Color(0xFF1D4ED8),
                                        onDeleteClick = if (isDeletable) {
                                            { problemToDelete = problem }
                                        } else null,
                                        onClick = { onProblemClick(problem.id) }
                                    )
                                }

                                // Infinite Scroll Footer
                                item {
                                    if (viewModel.userProblemsHasMore) {
                                        LaunchedEffect(Unit) {
                                            if (currentUserId.isNotBlank()) {
                                                viewModel.loadNextUserProblemsPage(currentUserId)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (viewModel.userProblemsLoadingMore) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = Color(0xFF2563EB)
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
