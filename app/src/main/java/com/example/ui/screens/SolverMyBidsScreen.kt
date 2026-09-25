package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.BidEntity
import com.example.data.entity.ProblemEntity
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.ListScreenSkeleton
import com.example.ui.components.LoadingAwareContent
import com.example.ui.components.SyncAwareRefreshableContent
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.SomadhanPullToRefresh
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
import kotlin.math.ceil
import kotlin.math.min
import com.example.ui.components.BottomSlideAlertDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverMyBidsScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onProblemClick: (String) -> Unit,
    initialTab: Int = 0
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val allBids by viewModel.allBids.collectAsStateWithLifecycle()
    val solverBidsState by viewModel.solverBids.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val isLocationUpdating by viewModel.isLocationUpdating.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Loading/Sync Fix Roadmap v2, dhap 4 -- initialSyncPhase (bulk-pull), separate sessionKey "solver_my_bids_sync"
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialTab.coerceIn(0, 3), pageCount = { 4 })
    var selectedCategoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showCategoryFilterDialog by remember { mutableStateOf(false) }

    val currentUserId = currentUser?.id ?: ""

    // Reset pagination on entry or solver change
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            viewModel.resetSolverMyBidsPagination(currentUserId)
        }
    }

    // Bids for this solver from live reactive state
    val myBids = remember(allBids, solverBidsState, currentUserId) {
        if (currentUserId.isBlank()) emptyList()
        else {
            val bids = if (solverBidsState.isNotEmpty()) solverBidsState else allBids.filter { it.solverId == currentUserId }
            bids.sortedByDescending { it.createdAt }
        }
    }

    // 1. অপেক্ষমান (Pending & Problem is still OPEN with no accepted bids)
    val pendingBids = remember(myBids, allProblems) {
        myBids.filter { bid ->
            val prob = allProblems.find { it.id == bid.problemId }
            val isBidPending = bid.status == "PENDING"
            val isPostOpen = prob != null && prob.status == "OPEN"
            val noBidAcceptedYet = prob != null && prob.acceptedBidId.isNullOrBlank() && prob.acceptedSolverId.isNullOrBlank()
            isBidPending && isPostOpen && noBidAcceptedYet
        }
    }

    // 2. গৃহীত (Accepted Bids) - Ongoing/Incomplete jobs appear first
    val acceptedBids = remember(myBids, allProblems, currentUserId) {
        myBids.filter { bid ->
            val prob = allProblems.find { it.id == bid.problemId }
            bid.status == "ACCEPTED" && bid.status != "CANCELLED" && bid.status != "WITHDRAWN" &&
            (prob != null && prob.status != "CANCELLED" && (prob.acceptedBidId == bid.id || prob.acceptedSolverId == currentUserId))
        }.sortedWith(
            compareByDescending<BidEntity> { bid ->
                val prob = allProblems.find { it.id == bid.problemId }
                prob?.status != "COMPLETED" && prob?.status != "CANCELLED"
            }.thenByDescending { it.createdAt }
        )
    }

    // 3. অন্য সলভার নির্বাচিত (Bids where client accepted another solver or another bid)
    val otherSolverBids = remember(myBids, allProblems, currentUserId) {
        myBids.filter { bid ->
            val prob = allProblems.find { it.id == bid.problemId }
            val isThisBidAccepted = bid.status == "ACCEPTED" && bid.status != "CANCELLED" && bid.status != "WITHDRAWN"
            if (isThisBidAccepted) {
                false
            } else {
                // Post has another solver or bid accepted
                val anotherBidAccepted = prob != null && !prob.acceptedBidId.isNullOrBlank() && prob.acceptedBidId != bid.id
                val anotherSolverAccepted = prob != null && !prob.acceptedSolverId.isNullOrBlank() && prob.acceptedSolverId != currentUserId
                val inProgressByOther = prob != null && prob.status == "IN_PROGRESS" && prob.acceptedSolverId != currentUserId
                val completedByOther = prob != null && prob.status == "COMPLETED" && prob.acceptedSolverId != currentUserId
                anotherBidAccepted || anotherSolverAccepted || inProgressByOther || completedByOther
            }
        }
    }

    // 4. বাতিল (Cancelled / Withdrawn by solver, explicitly rejected, or problem cancelled without solver)
    val cancelledBids = remember(myBids, allProblems, currentUserId) {
        myBids.filter { bid ->
            val prob = allProblems.find { it.id == bid.problemId }
            val isThisBidAccepted = bid.status == "ACCEPTED" && bid.status != "CANCELLED" && bid.status != "WITHDRAWN"
            if (isThisBidAccepted) {
                false
            } else {
                val anotherSolverChosen = prob != null && (
                    (!prob.acceptedBidId.isNullOrBlank() && prob.acceptedBidId != bid.id) ||
                    (!prob.acceptedSolverId.isNullOrBlank() && prob.acceptedSolverId != currentUserId) ||
                    (prob.status == "IN_PROGRESS" && prob.acceptedSolverId != currentUserId) ||
                    (prob.status == "COMPLETED" && prob.acceptedSolverId != currentUserId)
                )
                if (anotherSolverChosen) {
                    false
                } else if (bid.status == "CANCELLED" || bid.status == "WITHDRAWN" || bid.status == "REJECTED") {
                    true
                } else {
                    val isPostCancelled = prob?.status == "CANCELLED"
                    val isProblemMissing = prob == null
                    isPostCancelled || isProblemMissing
                }
            }
        }
    }

    val currentTabBids = when (pagerState.currentPage) {
        0 -> pendingBids
        1 -> acceptedBids
        2 -> otherSolverBids
        else -> cancelledBids
    }

    // Filter by Category and Search query
    val filteredBids = remember(currentTabBids, selectedCategoryFilter, searchQuery, allProblems) {
        var list = currentTabBids

        if (!selectedCategoryFilter.isNullOrBlank()) {
            list = list.filter { bid ->
                val prob = allProblems.find { it.id == bid.problemId }
                prob?.categoryId == selectedCategoryFilter
            }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter { bid ->
                val prob = allProblems.find { it.id == bid.problemId }
                val titleMatch = prob?.title?.lowercase()?.contains(q) == true
                val catMatch = prob?.categoryName?.lowercase()?.contains(q) == true
                val msgMatch = bid.message.lowercase().contains(q)
                val amountMatch = bid.amount.toString().contains(q) || DistanceUtil.toBengaliDigits(bid.amount.toInt().toString()).contains(q)
                titleMatch || catMatch || msgMatch || amountMatch
            }
        }
        list
    }

    // Category Filter Dialog
    if (showCategoryFilterDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showCategoryFilterDialog = false },
            title = {
                Text(
                    text = "ক্যাটাগরি ফিল্টার",
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
                                text = "সব ক্যাটাগরি",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.5.sp,
                                color = if (isSelected) SomadhanOrange else SomadhanTextPrimary
                            )
                            Text(
                                text = "(${DistanceUtil.toBengaliDigits(currentTabBids.size.toString())}টি)",
                                fontSize = 12.sp,
                                color = if (isSelected) SomadhanOrange else SomadhanTextSecondary
                            )
                        }
                    }

                    items(allCategories, key = { it.id }) { cat ->
                        val isSelected = selectedCategoryFilter == cat.id
                        val count = currentTabBids.count { bid ->
                            val prob = allProblems.find { it.id == bid.problemId }
                            prob?.categoryId == cat.id
                        }
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
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "আমার বিড",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = "সকল প্রস্তাবিত বিডের বিবরণ ও অবস্থা",
                                fontSize = 11.5.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("back_button")
                        ) {
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
                            modifier = Modifier.testTag("filter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "ক্যাটাগরি ফিল্টার",
                                tint = if (selectedCategoryFilter != null) SomadhanOrange else SomadhanTextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SomadhanBg
                    )
                )

                // Tabs: 0 -> গৃহীত, 1 -> অপেক্ষমান, 2 -> বাতিল, 3 -> অন্য সলভার নির্বাচিত
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = SomadhanBg,
                    contentColor = SomadhanOrange,
                    edgePadding = 12.dp,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            val indicatorColor = when (pagerState.currentPage) {
                                0 -> SomadhanSuccess
                                1 -> SomadhanOrange
                                2 -> Color(0xFFDC2626)
                                else -> Color(0xFF4F46E5)
                            }
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = indicatorColor,
                                height = 3.dp
                            )
                        }
                    },
                    divider = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SomadhanDivider)
                        )
                    }
                ) {
                    // Tab 0: গৃহীত
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        modifier = Modifier.testTag("tab_accepted_bids"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "গৃহীত",
                                    fontSize = 13.sp,
                                    fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (pagerState.currentPage == 0) SomadhanSuccess else SomadhanTextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (pagerState.currentPage == 0) SomadhanSuccess.copy(alpha = 0.15f)
                                            else Color(0xFFF3F4F6)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = DistanceUtil.toBengaliDigits(acceptedBids.size.toString()),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pagerState.currentPage == 0) SomadhanSuccess else SomadhanTextSecondary
                                    )
                                }
                            }
                        }
                    )

                    // Tab 1: অপেক্ষমান
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        modifier = Modifier.testTag("tab_pending_bids"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "অপেক্ষমান",
                                    fontSize = 13.sp,
                                    fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (pagerState.currentPage == 1) SomadhanOrange else SomadhanTextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (pagerState.currentPage == 1) SomadhanOrange.copy(alpha = 0.15f)
                                            else Color(0xFFF3F4F6)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = DistanceUtil.toBengaliDigits(pendingBids.size.toString()),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pagerState.currentPage == 1) SomadhanOrange else SomadhanTextSecondary
                                    )
                                }
                            }
                        }
                    )

                    // Tab 2: বাতিল
                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        },
                        modifier = Modifier.testTag("tab_cancelled_bids"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "বাতিল",
                                    fontSize = 13.sp,
                                    fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Medium,
                                    color = if (pagerState.currentPage == 2) Color(0xFFDC2626) else SomadhanTextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (pagerState.currentPage == 2) Color(0xFFFEE2E2)
                                            else Color(0xFFF3F4F6)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = DistanceUtil.toBengaliDigits(cancelledBids.size.toString()),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pagerState.currentPage == 2) Color(0xFFDC2626) else SomadhanTextSecondary
                                    )
                                }
                            }
                        }
                    )

                    // Tab 3: অন্য সলভার নির্বাচিত
                    Tab(
                        selected = pagerState.currentPage == 3,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        },
                        modifier = Modifier.testTag("tab_other_solver_bids"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "অন্য সলভার নির্বাচিত",
                                    fontSize = 13.sp,
                                    fontWeight = if (pagerState.currentPage == 3) FontWeight.Bold else FontWeight.Medium,
                                    color = if (pagerState.currentPage == 3) Color(0xFF4F46E5) else SomadhanTextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (pagerState.currentPage == 3) Color(0xFFEEF2FF)
                                            else Color(0xFFF3F4F6)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = DistanceUtil.toBengaliDigits(otherSolverBids.size.toString()),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pagerState.currentPage == 3) Color(0xFF4F46E5) else SomadhanTextSecondary
                                    )
                                }
                            }
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
            val pageRawBids = when (pageIndex) {
                0 -> acceptedBids
                1 -> pendingBids
                2 -> cancelledBids
                else -> otherSolverBids
            }

            val pageFilteredBids = remember(pageRawBids, selectedCategoryFilter, searchQuery, allProblems) {
                var list = pageRawBids

                if (!selectedCategoryFilter.isNullOrBlank()) {
                    list = list.filter { bid ->
                        val prob = allProblems.find { it.id == bid.problemId }
                        prob?.categoryId == selectedCategoryFilter
                    }
                }

                if (searchQuery.isNotBlank()) {
                    val q = searchQuery.trim().lowercase()
                    list = list.filter { bid ->
                        val prob = allProblems.find { it.id == bid.problemId }
                        val titleMatch = prob?.title?.lowercase()?.contains(q) == true
                        val catMatch = prob?.categoryName?.lowercase()?.contains(q) == true
                        val msgMatch = bid.message.lowercase().contains(q)
                        val amountMatch = bid.amount.toString().contains(q) || DistanceUtil.toBengaliDigits(bid.amount.toInt().toString()).contains(q)
                        titleMatch || catMatch || msgMatch || amountMatch
                    }
                }
                list
            }

            SomadhanPullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshData() },
                modifier = Modifier.fillMaxSize()
            ) {
                // Loading/Sync Fix Roadmap v2, dhap 7 (batch 3, pagination-screen migration) --
                // এতদিন data-বিহীন SyncAwareContent ছিল (rule ১ কভার করতো, rule ২/৪ ডেটা-ডিফ
                // শিমার ছিল না)। এই স্ক্রিনে (SolverAllPostsScreen-এর মতোই) pagination আসলে
                // dead/stub -- `pageFilteredBids` সরাসরি `myBids`-এর plain `remember`-filter,
                // page-buffer না -- তাই কোনো suppression জটিলতা ছাড়াই সরাসরি standard
                // SyncAwareRefreshableContent প্যাটার্ন প্রযোজ্য। header/search/filter এই
                // স্ক্রিনেও আগে থেকেই LazyColumn-এর ভেতরের item হিসেবে ছিল (আলাদা topBar নয়) --
                // সেই বিদ্যমান লে-আউট অপরিবর্তিত রাখা হয়েছে, শুধু কম্পোনেন্ট বদলানো হলো।
                SyncAwareRefreshableContent(
                    sessionKey = "solver_my_bids_sync",
                    viewModel = viewModel,
                    syncPhase = initialSyncPhase,
                    data = pageFilteredBids,
                    onRetry = { viewModel.retryInitialSync() },
                    isManualRefreshing = isRefreshing
                ) { bidsList ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("solver_my_bids_list"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Real-time location & tracking ticker
                    item {
                        RealtimeLocationBar(
                            locationAddress = liveLocation.address,
                            isSolver = true,
                            onRefresh = { viewModel.refreshLiveLocation(showToast = true) },
                            isUpdating = isLocationUpdating
                        )
                    }

                    // Search & Filter Header
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text("সমস্যার শিরোনাম, বার্তা বা অ্যামাউন্ট দিয়ে খুঁজুন...", fontSize = 12.5.sp, color = SomadhanTextHint)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = SomadhanTextSecondary, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SomadhanTextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SomadhanOrange,
                                    unfocusedBorderColor = SomadhanDivider,
                                    focusedContainerColor = SomadhanCardBg,
                                    unfocusedContainerColor = SomadhanCardBg
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_bids_input")
                            )

                            // Active filter chips
                            if (selectedCategoryFilter != null) {
                                val cat = allCategories.find { it.id == selectedCategoryFilter }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SomadhanOrangeLight)
                                        .border(1.dp, SomadhanOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "ক্যাটাগরি: ${cat?.nameBangla ?: "নির্বাচিত"}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SomadhanOrange
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "ফিল্টার মুছুন",
                                        tint = SomadhanOrange,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { selectedCategoryFilter = null }
                                    )
                                }
                            }
                        }
                    }

                    // Summary Row
                    item {
                        val statusText = when (pageIndex) {
                            0 -> "গৃহীত বিড (${DistanceUtil.toBengaliDigits(bidsList.size.toString())}টি)"
                            1 -> "অপেক্ষমান বিড (${DistanceUtil.toBengaliDigits(bidsList.size.toString())}টি)"
                            2 -> "বাতিলকৃত বিড (${DistanceUtil.toBengaliDigits(bidsList.size.toString())}টি)"
                            else -> "অন্য সলভার নির্বাচিত (${DistanceUtil.toBengaliDigits(bidsList.size.toString())}টি)"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }
                    }

                    // Bid Cards
                    if (bidsList.isEmpty()) {
                        item {
                            SolverMyBidEmptyState(
                                tabIndex = pageIndex,
                                hasFilter = selectedCategoryFilter != null || searchQuery.isNotBlank(),
                                onClearFilter = {
                                    selectedCategoryFilter = null
                                    searchQuery = ""
                                }
                            )
                        }
                    } else {
                        items(bidsList, key = { it.id }) { bid ->
                            val matchingProblem = allProblems.find { it.id == bid.problemId }
                            SolverBidItemCard(
                                bid = bid,
                                problem = matchingProblem,
                                currentUserId = currentUserId,
                                tabIndex = pageIndex,
                                onProblemClick = { onProblemClick(bid.problemId) },
                                onWithdrawBid = { b, onResult ->
                                    viewModel.withdrawBid(
                                        b,
                                        onSuccess = { onResult() },
                                        onError = { err ->
                                            onResult()
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            )
                        }
                    }

                    // Infinite Scroll Footer
                    if (viewModel.solverMyBidsHasMore) {
                        item {
                            LaunchedEffect(Unit) {
                                viewModel.loadNextSolverMyBidsPage(currentUserId)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (viewModel.solverMyBidsLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = SomadhanOrange
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                }
            }
        }
    }
}

@Composable
fun SolverBidItemCard(
    bid: BidEntity,
    problem: ProblemEntity?,
    currentUserId: String = "",
    tabIndex: Int = 0,
    onProblemClick: () -> Unit,
    onWithdrawBid: ((BidEntity, onResult: () -> Unit) -> Unit)? = null
) {
    var showWithdrawConfirmDialog by remember { mutableStateOf(false) }
    var isWithdrawing by remember { mutableStateOf(false) }

    val isExplicitlyAccepted = bid.status == "ACCEPTED" && bid.status != "CANCELLED" && bid.status != "WITHDRAWN" &&
        (problem != null && (problem.acceptedBidId == bid.id || (problem.acceptedSolverId == currentUserId && currentUserId.isNotBlank())))
    val isPendingOpen = bid.status == "PENDING" && problem != null && problem.status == "OPEN" && problem.acceptedBidId.isNullOrBlank() && problem.acceptedSolverId.isNullOrBlank()
    val isOtherSolverChosen = !isExplicitlyAccepted && problem != null && (
        (!problem.acceptedBidId.isNullOrBlank() && problem.acceptedBidId != bid.id) ||
        (!problem.acceptedSolverId.isNullOrBlank() && problem.acceptedSolverId != currentUserId) ||
        (problem.status == "IN_PROGRESS" && problem.acceptedBidId != bid.id) ||
        (problem.status == "COMPLETED" && problem.acceptedBidId != bid.id)
    )

    // Accepted and ongoing (incomplete)
    val isAcceptedRunning = isExplicitlyAccepted && (problem?.status != "COMPLETED" && problem?.status != "CANCELLED")

    val statusColor = when {
        isExplicitlyAccepted -> SomadhanSuccess
        isPendingOpen -> SomadhanOrange
        isOtherSolverChosen -> Color(0xFF4F46E5)
        else -> Color(0xFFDC2626)
    }

    val statusBg = when {
        isExplicitlyAccepted -> Color(0xFFECFDF5)
        isPendingOpen -> SomadhanOrangeLight
        isOtherSolverChosen -> Color(0xFFEEF2FF)
        else -> Color(0xFFFEF2F2)
    }

    val statusLabel = when {
        isAcceptedRunning -> "গৃহীত"
        isExplicitlyAccepted -> "সম্পন্ন"
        isPendingOpen -> "অপেক্ষমান"
        isOtherSolverChosen -> "অন্য সলভার নির্বাচিত"
        bid.status == "CANCELLED" -> "প্রত্যাহারকৃত"
        bid.status == "REJECTED" -> "প্রত্যাখ্যাত"
        problem?.status == "CANCELLED" -> "পোস্ট বাতিল"
        else -> "বাতিলকৃত"
    }

    val statusNote = when {
        isAcceptedRunning -> "ক্লায়েন্ট আপনার প্রস্তাব গ্রহণ করেছেন। কাজটি বর্তমানে চলমান।"
        isExplicitlyAccepted -> "কাজটি সফলভাবে সম্পন্ন হয়েছে।"
        isPendingOpen -> "ক্লায়েন্টের সিদ্ধান্তের অপেক্ষায় রয়েছে।"
        isOtherSolverChosen -> "ক্লায়েন্ট অন্য সমাধানকারীর প্রস্তাব গ্রহণ করেছেন।"
        bid.status == "CANCELLED" -> "আপনি এই বিডটি প্রত্যাহার করেছেন।"
        bid.status == "REJECTED" -> "ক্লায়েন্ট আপনার প্রস্তাবটি প্রত্যাখ্যান করেছেন।"
        problem?.status == "CANCELLED" -> "ক্লায়েন্ট এই সমস্যা পোস্টটি বাতিল করেছেন।"
        else -> null
    }

    val statusIcon = when {
        isExplicitlyAccepted -> Icons.Default.CheckCircle
        isPendingOpen -> Icons.Default.LocalOffer
        isOtherSolverChosen -> Icons.Default.Person
        else -> Icons.Default.Cancel
    }

    if (showWithdrawConfirmDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showWithdrawConfirmDialog = false },
            title = {
                Text(
                    text = "বিড প্রত্যাহার করতে চান?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে আপনি এই সমস্যার জন্য করা আপনার বিডটি বাতিল বা প্রত্যাহার করতে চান? প্রত্যাহার করলে এটি বাতিল ট্যাবে চলে যাবে।",
                    fontSize = 13.5.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isWithdrawing,
                    onClick = {
                        showWithdrawConfirmDialog = false
                        isWithdrawing = true
                        onWithdrawBid?.invoke(bid) { isWithdrawing = false }
                    }
                ) {
                    Text("হ্যাঁ, প্রত্যাহার করুন", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawConfirmDialog = false }) {
                    Text("না", color = SomadhanTextPrimary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Card border and container highlighting for accepted ongoing bids
    val cardBorder = when {
        isAcceptedRunning -> BorderStroke(1.8.dp, Color(0xFFF59E0B))
        isExplicitlyAccepted -> BorderStroke(1.dp, SomadhanSuccess.copy(alpha = 0.5f))
        isOtherSolverChosen -> BorderStroke(1.dp, Color(0xFFE0E7FF))
        else -> BorderStroke(1.dp, SomadhanDivider)
    }

    val cardContainerColor = when {
        isAcceptedRunning -> Color(0xFFFFFDF5)
        else -> SomadhanCardBg
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProblemClick() }
            .testTag("bid_card_${bid.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAcceptedRunning) 3.dp else 1.5.dp),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Pinned Banner for ongoing accepted jobs
            if (isAcceptedRunning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "পিনকৃত চলমান কাজ",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "চলমান",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            // Header: Category, Urgency & Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val categoryName = problem?.categoryName?.ifBlank { "সাধারণ সমস্যা" } ?: "সাধারণ কাজ"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEFF6FF))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = categoryName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (problem?.urgency != null && problem.urgency != "সাধারণ") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF2F2))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = problem.urgency,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }

                // Badges on Right: If accepted & running, show Star Mark + "চলমান"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (isAcceptedRunning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 3.5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "চলমান",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    // Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusLabel,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            // Problem Title
            val titleText = problem?.title?.ifBlank { "সমস্যার শিরোনাম নেই" } ?: "সমস্যার বিস্তারিত জানতে চাপ দিন"
            Text(
                text = titleText,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Problem Location if available
            val locationText = if (problem != null) {
                if (problem.isPhysical) {
                    problem.userAddress.ifBlank { "নির্দিষ্ট ঠিকানা দেওয়া হয়নি" }
                } else {
                    "অনলাইন / রিমোট সমাধান"
                }
            } else {
                null
            }

            if (!locationText.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SomadhanTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = locationText,
                        fontSize = 11.5.sp,
                        color = SomadhanTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Key details container: Bid Amount vs Post Budget & Delivery Time
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF9FAFB))
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bid Amount
                    Column {
                        Text(
                            text = "আপনার বিড মূল্য",
                            fontSize = 11.sp,
                            color = SomadhanTextSecondary
                        )
                        Text(
                            text = Formatters.formatTaka(bid.amount),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SomadhanOrange
                        )
                    }

                    // Estimated Duration / Time
                    if (bid.estimatedTime.isNotBlank()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "সময়সীমা",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = SomadhanTextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = DistanceUtil.toBengaliDigits(bid.estimatedTime),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanTextPrimary
                                )
                            }
                        }
                    }

                    // Post Budget Range
                    if (problem != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "পোস্টের বাজেট",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = Formatters.formatTakaRange(problem.minBudget, problem.maxBudget),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SomadhanTextPrimary
                            )
                        }
                    }
                }
            }

            // Bid Message Note
            if (bid.message.isNotBlank()) {
                Text(
                    text = "প্রস্তাবনা: \"${bid.message}\"",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status explanatory sub-note if present
            if (statusNote != null) {
                val subBg = when {
                    isAcceptedRunning -> Color(0xFFFEF3C7).copy(alpha = 0.5f)
                    isExplicitlyAccepted -> Color(0xFFF0FDF4)
                    isPendingOpen -> Color(0xFFFFFBEB)
                    isOtherSolverChosen -> Color(0xFFEEF2FF)
                    else -> Color(0xFFFEF2F2)
                }
                val subTint = when {
                    isAcceptedRunning -> Color(0xFFB45309)
                    isExplicitlyAccepted -> SomadhanSuccess
                    isPendingOpen -> SomadhanOrange
                    isOtherSolverChosen -> Color(0xFF4F46E5)
                    else -> Color(0xFFDC2626)
                }
                val subIcon = when {
                    isAcceptedRunning -> Icons.Default.Star
                    isExplicitlyAccepted -> Icons.Default.CheckCircle
                    isPendingOpen -> Icons.Default.Info
                    isOtherSolverChosen -> Icons.Default.Person
                    else -> Icons.Default.Cancel
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(subBg)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = subIcon,
                        contentDescription = null,
                        tint = subTint,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = statusNote,
                        fontSize = 11.5.sp,
                        color = subTint
                    )
                }
            }

            // Footer: Submission Date & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "বিডের তারিখ: ${Formatters.formatDateBengali(bid.createdAt)}",
                    fontSize = 11.sp,
                    color = SomadhanTextHint
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // In pending tab, allow withdrawing the bid
                    if (isPendingOpen && onWithdrawBid != null) {
                        TextButton(
                            enabled = !isWithdrawing,
                            onClick = { showWithdrawConfirmDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            if (isWithdrawing) {
                                CircularProgressIndicator(color = Color(0xFFDC2626), modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "প্রত্যাহার হচ্ছে...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFDC2626)
                                )
                            } else {
                                Text(
                                    text = "প্রত্যাহার করুন",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onProblemClick() }
                    ) {
                        Text(
                            text = "বিস্তারিত দেখুন",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanOrange
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun SolverMyBidEmptyState(
    tabIndex: Int,
    hasFilter: Boolean,
    onClearFilter: () -> Unit
) {
    val title = when {
        hasFilter -> "কোনো ফলাফল পাওয়া যায়নি"
        tabIndex == 0 -> "কোনো গৃহীত বিড নেই"
        tabIndex == 1 -> "কোনো অপেক্ষমান বিড নেই"
        tabIndex == 2 -> "কোনো বাতিলকৃত বিড নেই"
        else -> "অন্য সলভার নির্বাচিত এমন কোনো বিড নেই"
    }

    val subtitle = when {
        hasFilter -> "আপনার অনুসন্ধান বা ফিল্টারের সাথে মিলে এমন কোনো বিড নেই। ফিল্টার রিসেট করে দেখুন।"
        tabIndex == 0 -> "আপনার দেওয়া বিড ক্লায়েন্ট কর্তৃক গৃহীত হলে তা এখানে দৃশ্যমান হবে এবং আপনি কাজ শুরু করতে পারবেন।"
        tabIndex == 1 -> "আপনি যে সকল সমস্যার জন্য বিড করেছেন কিন্তু ক্লায়েন্ট এখনও সিদ্ধান্ত নেয়নি, সেগুলো এখানে দেখা যাবে।"
        tabIndex == 2 -> "আপনার প্রত্যাহারকৃত বা বাতিল হওয়া বিডগুলো এখানে সংরক্ষিত থাকে।"
        else -> "আপনি যে সকল সমস্যায় বিড করেছিলেন কিন্তু ক্লায়েন্ট অন্য কোনো সমাধানকারীকে বেছে নিয়েছেন, সেগুলো এখানে থাকবে।"
    }

    val icon = when {
        hasFilter -> Icons.Default.Search
        tabIndex == 0 -> Icons.Default.CheckCircle
        tabIndex == 1 -> Icons.Default.LocalOffer
        tabIndex == 2 -> Icons.Default.Cancel
        else -> Icons.Default.Person
    }

    val iconColor = when {
        hasFilter -> SomadhanOrange
        tabIndex == 0 -> SomadhanSuccess
        tabIndex == 1 -> SomadhanOrange
        tabIndex == 2 -> SomadhanTextSecondary
        else -> Color(0xFF4F46E5)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = SomadhanTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )

            if (hasFilter) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onClearFilter,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("সব ফিল্টার মুছুন", fontSize = 12.sp, color = SomadhanOrange)
                }
            }
        }
    }
}
