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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ProblemEntity
import com.example.ui.components.ListScreenSkeleton
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.SyncAwareRefreshableContent
import com.example.ui.components.UrgencyBadge
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllOpenProblemsScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onProblemClick: (String) -> Unit
) {
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    // Loading/Sync Fix Roadmap v2, dhap 4 -- initialSyncPhase (bulk-pull), separate sessionKey "all_open_problems_sync"
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()
    // Loading/Sync Fix Roadmap v2, ধাপ ৬ — এই স্ক্রিনে আগে pull-to-refresh ছিল না, বাকি ১৬টা
    // স্ক্রিনের প্যাটার্নে (SomadhanPullToRefresh → resetOpenProblemsPagination() +
    // viewModel.refreshData()) যোগ করা হলো, HorizontalPager-কে মুড়িয়ে (ট্যাব-রো'র নিচে) —
    // যাতে ট্যাব-সুইচিং pull-gesture-এর সাথে সংঘর্ষ না করে।
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.resetOpenProblemsPagination()
    }

    // Header/tab badge counts reflect the full locally-synced scoped dataset.
    // This is a cheap filter+count (no card rendering), so it stays accurate
    // even before all pages have been loaded below.
    val openProblemsCount = remember(allProblems) {
        allProblems.count { it.status == "OPEN" && (it.isPublic || !it.isDirectContract) && !it.isUserDeleted }
    }

    // The rendered list now comes from the incrementally-loaded page buffer
    // (see loadNextOpenProblemsPage / resetOpenProblemsPagination above) instead
    // of the full scoped table, so scrolling loads more rather than rendering
    // everything at once.
    val openProblemsList: List<ProblemEntity> = viewModel.openProblemsPaged

    val inProgressProblems = remember(allProblems) {
        allProblems
            .filter { it.status == "IN_PROGRESS" && (it.isPublic || !it.isDirectContract) && !it.isUserDeleted }
            .sortedByDescending { it.lastActivityAt ?: it.createdAt }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "সকল সমস্যা",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "উন্মুক্ত: ${DistanceUtil.toBengaliDigits(openProblemsCount.toString())}টি | চলমান: ${DistanceUtil.toBengaliDigits(inProgressProblems.size.toString())}টি",
                            fontSize = 11.5.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("all_open_problems_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = SomadhanTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SomadhanBg)
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Segmented Tabs: Left (উন্মুক্ত সমস্যা) & Right (চলমান সমস্যা)
            Surface(
                color = SomadhanBg,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = SomadhanBg,
                    contentColor = if (pagerState.currentPage == 0) Color(0xFF1D4ED8) else Color(0xFFD97706),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = if (pagerState.currentPage == 0) Color(0xFF1D4ED8) else Color(0xFFD97706)
                        )
                    },
                    divider = {
                        HorizontalDivider(color = SomadhanBorder, thickness = 1.dp)
                    }
                ) {
                    // Left Tab: উন্মুক্ত সমস্যা
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        modifier = Modifier
                            .testTag("tab_open_problems")
                            .padding(vertical = 4.dp),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "উন্মুক্ত সমস্যা",
                                    fontSize = 14.sp,
                                    fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (pagerState.currentPage == 0) Color(0xFF1D4ED8) else SomadhanTextSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (pagerState.currentPage == 0) Color(0xFFEFF6FF) else Color(0xFFF3F4F6)
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = DistanceUtil.toBengaliDigits(openProblemsCount.toString()),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pagerState.currentPage == 0) Color(0xFF1D4ED8) else SomadhanTextHint
                                    )
                                }
                            }
                        }
                    )

                    // Right Tab: চলমান সমস্যা
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        modifier = Modifier
                            .testTag("tab_in_progress_problems")
                            .padding(vertical = 4.dp),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "চলমান সমস্যা",
                                    fontSize = 14.sp,
                                    fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (pagerState.currentPage == 1) Color(0xFFD97706) else SomadhanTextSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (pagerState.currentPage == 1) Color(0xFFFEF3C7) else Color(0xFFF3F4F6)
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = DistanceUtil.toBengaliDigits(inProgressProblems.size.toString()),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pagerState.currentPage == 1) Color(0xFFD97706) else SomadhanTextHint
                                    )
                                }
                            }
                        }
                    )
                }
            }

            SomadhanPullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = {
                    viewModel.resetOpenProblemsPagination()
                    viewModel.refreshData()
                },
                modifier = Modifier.fillMaxSize()
            ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val isCurrentOpen = pageIndex == 0
                // Realtime-Aware, Structure-Preserving Refresh, ধাপ ৭ (ব্যাচ ২, pagination
                // decision) — ব্যবহারকারীর সিদ্ধান্ত: pagination-যুক্ত স্ক্রিনেও একই standard
                // প্যাটার্ন (cold-load skeleton / re-entry-flash / pull-to-refresh-pulse /
                // data-change-flash) প্রযোজ্য — "load more" (scroll/next/prev/see more) এর
                // ফলে নতুন পেজ যোগ হয়ে তালিকা বদলালে সংক্ষিপ্ত শিমার হওয়াটাই কাঙ্ক্ষিত (আলাদা
                // কোনো suppression/isLoadingMore প্যারামিটার যোগ করা হয়নি)। `openProblemsList`
                // (viewModel.openProblemsPaged, একটা mutableStateListOf) `.toList()` দিয়ে
                // immutable snapshot করা হচ্ছে, যাতে প্রতিটা মিউটেশনে নতুন List instance তৈরি
                // হয় আর SyncAwareRefreshableContent-এর `!=` diff সঠিকভাবে ধরতে পারে (একই
                // mutable reference বারবার পাঠালে diff কাজ করতো না)।
                val displayedProblems = if (isCurrentOpen) openProblemsList.toList() else inProgressProblems

                SyncAwareRefreshableContent(
                    sessionKey = "all_open_problems_sync",
                    viewModel = viewModel,
                    syncPhase = initialSyncPhase,
                    data = displayedProblems,
                    onRetry = { viewModel.retryInitialSync() },
                    modifier = Modifier.fillMaxSize(),
                    isManualRefreshing = isRefreshing
                ) { probs ->

                if (probs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .testTag(if (pageIndex == 0) "all_open_problems_empty_state" else "all_in_progress_problems_empty_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(if (pageIndex == 0) Color(0xFFEFF6FF) else Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (pageIndex == 0) Icons.Default.CheckCircleOutline else Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = if (pageIndex == 0) Color(0xFF1D4ED8) else Color(0xFFD97706),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (pageIndex == 0) "বর্তমানে কোনো উন্মুক্ত সমস্যা নেই" else "বর্তমানে কোনো চলমান সমস্যা নেই",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (pageIndex == 0) {
                                    "নতুন কোনো সমস্যা পোস্ট হলে এখানে দেখতে পাবেন।"
                                } else {
                                    "কোনো সমস্যায় সমাধানকারী নিযুক্ত হয়ে কাজ শুরু হলে এখানে দেখতে পাবেন।"
                                },
                                fontSize = 13.sp,
                                color = SomadhanTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .testTag(if (pageIndex == 0) "all_open_problems_list" else "all_in_progress_problems_list"),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header Information Banner
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (pageIndex == 0) Color(0xFFEFF6FF) else Color(0xFFFFFBEB)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (pageIndex == 0) Color(0xFFBFDBFE) else Color(0xFFFDE68A),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (pageIndex == 0) Icons.Default.NearMe else Icons.Default.PlayCircleOutline,
                                        contentDescription = null,
                                        tint = if (pageIndex == 0) Color(0xFF1D4ED8) else Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (pageIndex == 0) {
                                            "প্ল্যাটফর্মে পোস্ট হওয়া সকল উন্মুক্ত সমস্যা এখানে প্রদর্শিত হচ্ছে। বিস্তারিত দেখতে ট্যাপ করুন।"
                                        } else {
                                            "প্ল্যাটফর্মে সমাধানকারী নিযুক্ত হয়ে বর্তমানে চলমান সকল কাজ এখানে প্রদর্শিত হচ্ছে।"
                                        },
                                        fontSize = 11.5.sp,
                                        color = if (pageIndex == 0) Color(0xFF1E3A8A) else Color(0xFF92400E),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Problem Cards
                        items(
                            items = probs,
                            key = { it.id }
                        ) { problem ->
                            ProblemFeedCard(
                                problem = problem,
                                isInProgress = pageIndex == 1,
                                onClick = { onProblemClick(problem.id) }
                            )
                        }

                        // Infinite Scroll Footer (for Open Problems)
                        item {
                            if (pageIndex == 0 && viewModel.openProblemsHasMore) {
                                LaunchedEffect(Unit) {
                                    viewModel.loadNextOpenProblemsPage()
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (viewModel.openProblemsLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color(0xFF1D4ED8)
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

@Composable
private fun ProblemFeedCard(
    problem: ProblemEntity,
    isInProgress: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    Card(
        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SomadhanBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(if (isInProgress) "in_progress_problem_card_${problem.id}" else "open_problem_card_${problem.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Category Tag, Status/Urgency Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEFF6FF))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = problem.categoryName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1D4ED8)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isInProgress) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 7.dp, vertical = 2.5.dp)
                        ) {
                            Text(
                                text = "কাজ চলছে",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    if (problem.isPhysical) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF3E8FF))
                                .padding(horizontal = 6.dp, vertical = 2.5.dp)
                        ) {
                            Text(
                                text = "ফিজিক্যাল",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF7E22CE)
                            )
                        }
                    }

                    UrgencyBadge(urgency = problem.urgency)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = problem.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Budget or Contract Amount Highlight Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isInProgress && problem.acceptedAmount != null && problem.acceptedAmount > 0) {
                    Text(
                        text = "চুক্তিমূল্য: ",
                        fontSize = 12.5.sp,
                        color = SomadhanTextSecondary
                    )
                    Text(
                        text = Formatters.formatTaka(problem.acceptedAmount),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                } else {
                    Text(
                        text = "বাজেট: ",
                        fontSize = 12.5.sp,
                        color = SomadhanTextSecondary
                    )
                    Text(
                        text = Formatters.formatTakaRange(problem.minBudget, problem.maxBudget),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanOrange
                    )
                }
            }

            // If in progress and solver name is available
            if (isInProgress && !problem.acceptedSolverName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "নিয়োজিত সলভার: ${problem.acceptedSolverName}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E40AF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Row: Address & Time Ago
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location Address
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = problem.userAddress.ifBlank { "লোকেশন নেই" },
                        fontSize = 11.5.sp,
                        color = SomadhanTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Time Ago
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = Formatters.formatTimeAgo(problem.createdAt),
                        fontSize = 11.5.sp,
                        color = SomadhanTextHint
                    )
                }
            }
        }
    }
}
