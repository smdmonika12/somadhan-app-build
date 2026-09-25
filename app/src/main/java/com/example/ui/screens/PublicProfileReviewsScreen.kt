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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.ProblemEntity
import com.example.data.entity.RatingEntity
import com.example.data.entity.UserEntity
import com.example.ui.components.ListScreenSkeleton
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.StatusBadge
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.SyncAwareRefreshableContent
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.theme.SomadhanYellowVerified
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.Formatters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileReviewsScreen(
    userId: String,
    profileRole: String? = null,
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onProblemClick: (String) -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()

    val isViewerSolver = currentUser?.role == "SOLVER"
    val brandPrimary = if (isViewerSolver) SomadhanOrange else Color(0xFF1D4ED8)
    val brandPrimaryLight = if (isViewerSolver) SomadhanOrangeLight else Color(0xFFEFF6FF)
    val brandBorder = if (isViewerSolver) SomadhanOrange.copy(alpha = 0.3f) else Color(0xFFBFDBFE)

    var userProfile by remember { mutableStateOf<UserEntity?>(null) }
    var ratings by remember { mutableStateOf<List<RatingEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    // Fetch user
    val foundUser = allUsers.find { it.id == userId }
    LaunchedEffect(userId, foundUser) {
        if (foundUser != null) {
            userProfile = foundUser
            isLoading = false
        } else {
            val dbUser = viewModel.getPublicUserById(userId)
            userProfile = dbUser
            isLoading = false
        }
    }

    // Collect ratings for summary & counts
    LaunchedEffect(userId) {
        viewModel.getRatingsForUserFlow(userId).collect { list ->
            ratings = list
        }
    }

    val targetUser = userProfile
    val isSolver = when (profileRole?.uppercase()) {
        "SOLVER" -> true
        "USER" -> false
        else -> targetUser?.role == "SOLVER"
    }

    // Reset pagination on entry/user change
    LaunchedEffect(userId) {
        viewModel.resetPublicProfileReviewsPagination(userId)
    }

    LaunchedEffect(userId, isSolver) {
        if (isSolver) {
            viewModel.resetSolverCompletedJobsPagination(userId)
        } else {
            viewModel.resetUserProblemsPagination(userId)
        }
    }

    val publicProfileReviewsPaged = viewModel.publicProfileReviewsPaged
    val jobsPaged = if (isSolver) viewModel.solverCompletedJobsPaged else viewModel.userProblemsPaged

    // All problems associated with user (completed or posted)
    val userJobs = remember(allProblems, userId, isSolver) {
        if (isSolver) {
            allProblems.filter { it.acceptedSolverId == userId }.sortedByDescending { it.createdAt }
        } else {
            allProblems.filter { it.userId == userId }.sortedByDescending { it.createdAt }
        }
    }

    // Sorted reviews descending
    val sortedReviews = remember(ratings) {
        ratings.sortedByDescending { it.createdAt }
    }

    // Problem lookup map for displaying problem title and category in reviews
    val problemsMap = remember(allProblems) {
        allProblems.associateBy { it.id }
    }

    val averageRating = if (sortedReviews.isNotEmpty()) {
        sortedReviews.map { it.stars }.average()
    } else {
        5.0
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (targetUser != null) "${targetUser.name}-এর রিভিউ ও পোস্ট" else "সকল রিভিউ ও পোস্ট",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "মোট ${DistanceUtil.toBengaliDigits(sortedReviews.size.toString())} টি রিভিউ | ${DistanceUtil.toBengaliDigits(userJobs.size.toString())} টি পোস্ট",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("public_profile_reviews_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = brandPrimary)
                )
                RealtimeLocationBar(
                    locationAddress = liveLocation.address,
                    onRefresh = { viewModel.refreshLiveLocation() },
                    isSolver = isViewerSolver
                )
            }
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()
        val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
        // Loading Pattern Master Prompt, ব্যাচ ১০-এ চূড়ান্ত target design অনুযায়ী migrate —
        // আগে এখানে দুই-স্তরের গেট ছিল (বাইরে network-level sync-এর জন্য SyncAwareContent, ভেতরে
        // এই স্ক্রিনের নিজস্ব per-user fetch-এর জন্য rememberSessionAwareSkeletonGate)।
        // ReputationDetailScreen/ProblemDetailScreen-এর প্রমাণিত standard SyncAwareRefreshableContent
        // + SomadhanPullToRefresh প্যাটার্নে আনা হলো, একই sessionKey reuse করে rule ১ (cold-load)
        // + rule ২ (re-entry/realtime diff-শিমার, data = রিভিউ ও জব পেজড লিস্ট দুটোর স্ন্যাপশট)
        // একসাথে কভার করছে, আর নতুন rule ৩ (pull-to-refresh, এই স্ক্রিনে আগে ছিলই না) যোগ হলো।
        // এই স্ক্রিনের নিজস্ব per-user fetch (isLoading — allUsers-এ ক্যাশ-মিস হলে
        // viewModel.getPublicUserById() কল করা, global bulk-sync থেকে স্বতন্ত্র একটা per-instance
        // concern, ঠিক ProblemDetailScreen-এর isCheckingProblem-এর মতোই) ইচ্ছাকৃতভাবে অক্ষত রাখা
        // হয়েছে — content lambda-র ভেতরে isLoading সাব-কেস হিসেবে রয়ে গেছে, কারণ
        // SyncAwareRefreshableContent-এর নিজস্ব LOADED phase বাল্ক-পুল শেষেই আসতে পারে, তখনো এই
        // নির্দিষ্ট userId-র fetch শেষ না হতে পারে।
        SomadhanPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        SyncAwareRefreshableContent(
            sessionKey = "public_profile_reviews_$userId",
            viewModel = viewModel,
            syncPhase = initialSyncPhase,
            data = listOf(publicProfileReviewsPaged.toList(), jobsPaged.toList()),
            onRetry = { viewModel.retryInitialSync() },
            isManualRefreshing = isRefreshing,
            skeleton = {
                ListScreenSkeleton(
                    modifier = Modifier.fillMaxSize(),
                    tint = brandPrimary
                )
            }
        ) { _ ->
        if (isLoading) {
            ListScreenSkeleton(
                modifier = Modifier.fillMaxSize(),
                tint = brandPrimary
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Tabs: Reviews & Posts
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = SomadhanBg,
                    contentColor = brandPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = brandPrimary
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
                                text = "সকল রিভিউ (${DistanceUtil.toBengaliDigits(sortedReviews.size.toString())})",
                                fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
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
                                text = if (isSolver) "সম্পন্ন/যুক্ত কাজ (${DistanceUtil.toBengaliDigits(userJobs.size.toString())})"
                                       else "পোস্টসমূহ (${DistanceUtil.toBengaliDigits(userJobs.size.toString())})",
                                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }

                HorizontalDivider(color = SomadhanDivider, thickness = 1.dp)

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    if (pageIndex == 0) {
                        // TAB 0: REVIEWS LIST WITH INFINITE SCROLLING
                        if (publicProfileReviewsPaged.isEmpty() && !viewModel.publicProfileReviewsLoadingMore) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = SomadhanTextHint,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "এখনো কোনো রিভিউ পাওয়া যায়নি",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("public_reviews_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Rating Overview Summary Card
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = brandPrimaryLight),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, brandBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "সামগ্রিক গড় রেটিং",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = SomadhanTextSecondary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = String.format(Locale.US, "%.1f", averageRating),
                                                        fontSize = 26.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = brandPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    for (i in 1..5) {
                                                        val isFilled = i <= averageRating.toInt()
                                                        Icon(
                                                            imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.Star,
                                                            contentDescription = null,
                                                            tint = if (isFilled) SomadhanYellowVerified else SomadhanTextHint,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${DistanceUtil.toBengaliDigits(sortedReviews.size.toString())} টি রিভিউ",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SomadhanTextPrimary
                                                )
                                                Text(
                                                    text = "১০০% গ্রাহক সন্তুষ্টি",
                                                    fontSize = 11.sp,
                                                    color = SomadhanTextSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                // Review Items
                                items(publicProfileReviewsPaged, key = { it.id }) { review ->
                                    val relatedProblem = problemsMap[review.problemId]
                                    val problemTitle = if (review.problemTitle.isNotBlank()) review.problemTitle
                                                       else relatedProblem?.title ?: "সমাধান পোস্ট"
                                    val categoryName = relatedProblem?.categoryName ?: ""

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("review_card_${review.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp)
                                        ) {
                                            // User & Stars Header
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(brandPrimaryLight),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = brandPrimary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = review.userName.ifBlank { "ক্লায়েন্ট" },
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SomadhanTextPrimary
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    for (i in 1..5) {
                                                        val isFilled = i <= review.stars
                                                        Icon(
                                                            imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.Star,
                                                            contentDescription = null,
                                                            tint = if (isFilled) SomadhanYellowVerified else SomadhanTextHint,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Associated Problem Title & Category Tag
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(SomadhanBg)
                                                    .border(0.5.dp, SomadhanDivider, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Work,
                                                    contentDescription = null,
                                                    tint = SomadhanTextSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = problemTitle,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SomadhanTextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (categoryName.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "• $categoryName",
                                                        fontSize = 11.sp,
                                                        color = brandPrimary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }

                                            // Review Comment
                                            if (review.comment.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = review.comment,
                                                    fontSize = 13.sp,
                                                    color = SomadhanTextSecondary,
                                                    lineHeight = 18.sp
                                                )
                                            }

                                            // Timestamp
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = Formatters.formatTimeAgo(review.createdAt),
                                                fontSize = 11.sp,
                                                color = SomadhanTextHint
                                            )
                                        }
                                    }
                                }

                                // Reviews Infinite Scroll Footer
                                if (viewModel.publicProfileReviewsHasMore) {
                                    item {
                                        LaunchedEffect(Unit) {
                                            viewModel.loadNextPublicProfileReviewsPage(userId)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (viewModel.publicProfileReviewsLoadingMore) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = brandPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // TAB 1: POSTS & JOBS LIST WITH INFINITE SCROLLING
                        val jobsLoadingMore = if (isSolver) viewModel.solverCompletedJobsLoadingMore else viewModel.userProblemsLoadingMore
                        val jobsHasMore = if (isSolver) viewModel.solverCompletedJobsHasMore else viewModel.userProblemsHasMore

                        if (jobsPaged.isEmpty() && !jobsLoadingMore) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Work,
                                        contentDescription = null,
                                        tint = SomadhanTextHint,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "এখনো কোনো পোস্ট বা কাজের রেকর্ড নেই",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("public_posts_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(jobsPaged, key = { it.id }) { problem ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onProblemClick(problem.id) }
                                            .testTag("post_card_${problem.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(brandPrimaryLight)
                                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Category,
                                                        contentDescription = null,
                                                        tint = brandPrimary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = problem.categoryName.ifBlank { "সাধারণ" },
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = brandPrimary
                                                    )
                                                }

                                                StatusBadge(status = problem.status)
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = problem.title,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanTextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (problem.description.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = problem.description,
                                                    fontSize = 12.sp,
                                                    color = SomadhanTextSecondary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = Formatters.formatTakaRange(problem.minBudget, problem.maxBudget),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = brandPrimary
                                                )
                                                Text(
                                                    text = Formatters.formatTimeAgo(problem.createdAt),
                                                    fontSize = 11.sp,
                                                    color = SomadhanTextHint
                                                )
                                            }
                                        }
                                    }
                                }

                                // Jobs Infinite Scroll Footer
                                if (jobsHasMore) {
                                    item {
                                        LaunchedEffect(Unit) {
                                            if (isSolver) {
                                                viewModel.loadNextSolverCompletedJobsPage(userId)
                                            } else {
                                                viewModel.loadNextUserProblemsPage(userId)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (jobsLoadingMore) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = brandPrimary
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
            } // SyncAwareRefreshableContent বন্ধ
        } // SomadhanPullToRefresh বন্ধ
    }
}
