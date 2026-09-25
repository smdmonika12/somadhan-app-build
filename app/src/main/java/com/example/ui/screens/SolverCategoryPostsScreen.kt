package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.SupabaseRealtimeManager
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.ListScreenSkeleton
import com.example.ui.components.ProblemCard
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.SyncAwareRefreshableContent
import com.example.ui.components.worstSyncPhase
import com.example.ui.navigation.Screen
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSurfaceVariant
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.AiMatcherUtil
import com.example.util.DistanceUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverCategoryPostsScreen(
    categoryId: String,
    viewModel: SomadhanViewModel,
    onNavigate: (String) -> Unit = {},
    onNavigateBack: () -> Unit,
    onProblemClick: (String) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val isLocationUpdating by viewModel.isLocationUpdating.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val physicalRadius by viewModel.physicalCategoryRadiusKm.collectAsStateWithLifecycle()
    // Loading/Sync Fix Roadmap v2, ধাপ ৪ (Group B) — allProblems/allUsers bulk-pull-এর অংশ
    // (initialSyncPhase), allCategories categoriesSyncPhase-এর অধীনে — দুটোর যেকোনো একটা ERROR
    // হলেই এই স্ক্রিনের ডাটা অসম্পূর্ণ থাকতে পারে।
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()
    val categoriesSyncPhase by viewModel.categoriesSyncPhase.collectAsStateWithLifecycle()
    val combinedSyncPhase = worstSyncPhase(initialSyncPhase, categoriesSyncPhase)

    val isSolver = currentUser?.role == "SOLVER"
    val radiusFormattedBengali = DistanceUtil.toBengaliDigits(
        if (physicalRadius % 1.0 == 0.0) physicalRadius.toInt().toString() else physicalRadius.toString()
    )

    // Resolve specific category
    val category = allCategories.find { it.id == categoryId }

    // Category specific palette for solver; For user account, strictly use User Profile Royal Blue theme
    val (catBg, catTint) = category?.let { CategoryIconHelper.getCategoryColors(it.nameBangla, it.iconName) }
        ?: Pair(SomadhanOrangeLight, SomadhanOrange)

    val brandColor = if (isSolver) {
        if (catTint != SomadhanOrange) catTint else SomadhanOrange
    } else {
        Color(0xFF1D4ED8)
    }
    val brandBgLight = if (isSolver) {
        if (catTint != SomadhanOrange) catBg else SomadhanOrangeLight
    } else {
        Color(0xFFEFF6FF)
    }

    // Category-specific Search state (remembered per categoryId, prevents query leakage across categories)
    var searchQuery by rememberSaveable(categoryId) { mutableStateOf("") }

    // Solver / User effective coordinates (Live GPS prioritized -> Saved User DB coordinates fallback)
    val solverLat = if (liveLocation.latitude != 0.0) liveLocation.latitude else (currentUser?.latitude ?: 0.0)
    val solverLon = if (liveLocation.longitude != 0.0) liveLocation.longitude else (currentUser?.longitude ?: 0.0)

    // Reset pagination whenever the category changes. Deliberately NOT keyed on
    // solverLat/solverLon (those tick often with live GPS) — the initial fetch uses
    // whatever location is already known, same as pull-to-refresh does explicitly.
    LaunchedEffect(categoryId, isSolver) {
        viewModel.resetSolverCategoryPostsPagination(categoryId, solverLat, solverLon, physicalRadius, isSolver)
    }

    // Base Category-Eligible Active Posts from live StateFlow — used for the header
    // count (always accurate) and as the search source (below), scoped+distance
    // filtered same as the paginated path.
    val categoryEligibleProblems = remember(allProblems, categoryId, category, solverLat, solverLon, isSolver, physicalRadius) {
        if (category != null && !category.isActive) {
            emptyList()
        } else {
            allProblems.filter { problem ->
                if (problem.categoryId != categoryId) return@filter false
                if (problem.status != "OPEN") return@filter false
                if (problem.isDirectContract || !problem.isPublic || problem.isUserDeleted) return@filter false
                if (isSolver) {
                    viewModel.isProblemVisibleToSolver(problem, solverLat, solverLon, physicalRadius)
                } else {
                    // For regular Users: No distance limitation, see all active posts in active category
                    true
                }
            }.sortedByDescending { it.createdAt }
        }
    }

    // While searching, search needs to see the whole locally-synced scoped set
    // (categoryEligibleProblems, cheap in-memory) rather than just loaded pages.
    // With no active search, render the incrementally-loaded page buffer instead
    // of the full set at once.
    val hasActiveFilter = searchQuery.isNotBlank()
    val pagedPostsSize = viewModel.solverCategoryPostsPaged.size

    // ব্যাচ ৮ migration নোট: pagination-এর data (mutableStateListOf) সরাসরি না পাঠিয়ে
    // .toList() দিয়ে immutable snapshot বানানো হচ্ছে (Ground Rule ১১) — এটাই এখন
    // SyncAwareRefreshableContent-এর data প্যারামিটারে যাবে diff/শিমার সঠিকভাবে ধরার জন্য।
    val displayedProblems = remember(categoryEligibleProblems, searchQuery, allCategories, hasActiveFilter, pagedPostsSize) {
        if (hasActiveFilter) {
            AiMatcherUtil.searchSimilarProblems(searchQuery, categoryEligibleProblems, allCategories)
        } else {
            viewModel.solverCategoryPostsPaged.toList()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (category != null) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(brandBgLight, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(category.iconName),
                                        contentDescription = null,
                                        tint = brandColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Column {
                                Text(
                                    text = category?.nameBangla ?: (if (isSolver) "ক্যাটাগরি কাজ" else "ক্যাটাগরি সমস্যা"),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(categoryEligibleProblems.size.toString())}টি ${if (isSolver) "সক্রিয় কাজ" else "সক্রিয় সমস্যা"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = brandColor
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("category_posts_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = SomadhanTextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SomadhanBg
                    )
                )

                // Realtime Location Bar
                RealtimeLocationBar(
                    locationAddress = liveLocation.address,
                    isSolver = isSolver,
                    onRefresh = { viewModel.refreshLiveLocation(showToast = true) },
                    isUpdating = isLocationUpdating
                )
            }
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        SomadhanPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                viewModel.resetSolverCategoryPostsPagination(categoryId, solverLat, solverLon, physicalRadius, isSolver)
                viewModel.refreshData()
                viewModel.refreshLiveLocation(showToast = false)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ব্যাচ ৮ migration — ক্যাটেগরি C, SOMADHAN_LOADING_PATTERN_MASTER_PROMPT.md অনুযায়ী
            // এই স্ক্রিনটাই সবচেয়ে সরল (ক্যাটেগরি A-এর ১৪টার মতোই standard full migrate,
            // rule ১+২+৩+৪ সব একসাথে)। আগের ম্যানুয়াল minimumSkeletonActive/isInitialLoading
            // গেট এবং সরাসরি ListScreenSkeleton/SyncBlockedRetryState কল সরিয়ে
            // SyncAwareRefreshableContent-এ migrate করা হলো — প্রথম-ভিজিট ন্যূনতম-স্কেলিটন গেট
            // আর LOADED-এ markLoadedOnce() কল এখন এই কম্পোনেন্টের ভেতরেই একই sessionKey দিয়ে
            // হ্যান্ডল হয়।
            //
            // এই স্ক্রিনে দুইটা স্বাধীন loading-উৎস আছে: (১) allProblems/allCategories bulk-pull
            // sync (combinedSyncPhase, আগের মতোই), (২) এই নির্দিষ্ট ক্যাটাগরির নিজস্ব paginated
            // fetch (solverCategoryPostsLoadingMore/solverCategoryPostsPaged, প্রথম পাতা এখনো
            // আসেনি কিনা)। দুটোকে worstSyncPhase দিয়ে একটাই syncPhase-এ মিলিয়ে দেওয়া হচ্ছে যাতে
            // কোনোটাই miss না হয়।
            val paginationFirstPageSyncPhase = if (
                allCategories.isEmpty() &&
                viewModel.solverCategoryPostsLoadingMore &&
                viewModel.solverCategoryPostsPaged.isEmpty()
            ) {
                SupabaseRealtimeManager.SyncPhase.LOADING
            } else {
                SupabaseRealtimeManager.SyncPhase.LOADED
            }
            val screenSyncPhase = worstSyncPhase(combinedSyncPhase, paginationFirstPageSyncPhase)

            SyncAwareRefreshableContent(
                sessionKey = "solver_category_posts_$categoryId",
                viewModel = viewModel,
                syncPhase = screenSyncPhase,
                data = displayedProblems,
                onRetry = {
                    viewModel.retryInitialSync()
                    viewModel.retryCategoriesSync()
                },
                isManualRefreshing = isRefreshing,
                skeleton = { ListScreenSkeleton(modifier = Modifier.fillMaxSize(), tint = brandColor) },
                modifier = Modifier.fillMaxSize()
            ) { displayedProblemsShown ->
            if (category == null && allCategories.isNotEmpty()) {
                // Category not found fallback
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = SomadhanError,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "ক্যাটাগরি পাওয়া যায়নি",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "অনুরোধকৃত ক্যাটাগরিটি খুঁজে পাওয়া যায়নি। অনুগ্রহ করে হোমে ফিরে যান।",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateBack,
                                colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("হোমে ফিরে যান", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category-specific Search Bar Item
                    item {
                        val dynamicPlaceholder = if (category != null) {
                            "${category.nameBangla}-এর ${if (isSolver) "কাজ" else "সমস্যা"} খুঁজুন..."
                        } else {
                            "এই ক্যাটাগরিতে ${if (isSolver) "কাজ" else "সমস্যা"} খুঁজুন..."
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = dynamicPlaceholder,
                                    fontSize = 12.sp,
                                    color = SomadhanTextHint
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "সার্চ",
                                    tint = brandColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.testTag("clear_category_search")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "মুছুন",
                                            tint = SomadhanTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = SomadhanBorder,
                                focusedContainerColor = SomadhanBg,
                                unfocusedContainerColor = SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("category_search_input")
                        )

                        // Search result count badge if search query active
                        if (searchQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "সার্চ ফলাফল: ${DistanceUtil.toBengaliDigits(displayedProblemsShown.size.toString())}টি ${if (isSolver) "কাজ" else "সমস্যা"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = brandColor
                                )
                                TextButton(
                                    onClick = { searchQuery = "" },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = if (isSolver) "সব কাজ দেখুন" else "সব সমস্যা দেখুন",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Empty State: No eligible posts in this category at all
                    if (categoryEligibleProblems.isEmpty() && !viewModel.solverCategoryPostsLoadingMore) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SomadhanBorder, RoundedCornerShape(14.dp))
                                    .padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(SomadhanSurfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkOff,
                                            contentDescription = null,
                                            tint = SomadhanTextSecondary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = if (isSolver && category?.isPhysical == true) {
                                            "আপনার ${radiusFormattedBengali} কিমি এলাকার মধ্যে এই ক্যাটাগরিতে বর্তমানে কোনো সক্রিয় কাজ নেই।"
                                        } else {
                                            "এই ক্যাটাগরিতে বর্তমানে কোনো সক্রিয় ${if (isSolver) "কাজ" else "সমস্যা"} নেই।"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SomadhanTextSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 19.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isSolver) {
                                            "নতুন কাজ পোস্ট হলে এখানে স্বয়ংক্রিয়ভাবে দৃশ্যমান হবে।"
                                        } else {
                                            "নতুন সমস্যা পোস্ট হলে এখানে স্বয়ংক্রিয়ভাবে দৃশ্যমান হবে।"
                                        },
                                        fontSize = 11.sp,
                                        color = SomadhanTextHint,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    // Empty State: Category has posts, but search query returned 0 matches
                    else if (displayedProblemsShown.isEmpty() && !viewModel.solverCategoryPostsLoadingMore) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SomadhanBorder, RoundedCornerShape(14.dp))
                                    .padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(SomadhanSurfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FindInPage,
                                            contentDescription = null,
                                            tint = brandColor,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "আপনার অনুসন্ধানের সাথে মিলে কোনো ${if (isSolver) "কাজ" else "সমস্যা"} পাওয়া যায়নি।",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SomadhanTextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "বানান সঠিক কিনা যাচাই করুন অথবা অন্য শব্দ দিয়ে অনুসন্ধান করুন।",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { searchQuery = "" },
                                        colors = ButtonDefaults.buttonColors(containerColor = brandBgLight),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "সার্চ ক্লিয়ার করুন",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = brandColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Visible Posts List
                    else {
                        items(
                            items = displayedProblemsShown,
                            key = { it.id }
                        ) { problem ->
                            val posterUser = allUsers.find { it.id == problem.userId }
                            ProblemCard(
                                problem = problem,
                                posterUser = posterUser,
                                solverLat = if (solverLat != 0.0) solverLat else null,
                                solverLon = if (solverLon != 0.0) solverLon else null,
                                accentColor = brandColor,
                                onUserClick = if (currentUser?.id != problem.userId) {
                                    { onNavigate(Screen.PublicProfile.createRoute(problem.userId, "USER")) }
                                } else null,
                                onClick = { onProblemClick(problem.id) }
                            )
                        }

                        // Infinite Scroll Footer (only while browsing the unfiltered
                        // paginated feed — an active search already shows the full
                        // matching set from categoryEligibleProblems)
                        item {
                            if (!hasActiveFilter && viewModel.solverCategoryPostsHasMore) {
                                LaunchedEffect(Unit) {
                                    viewModel.loadNextSolverCategoryPostsPage(categoryId, solverLat, solverLon, physicalRadius, isSolver)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (viewModel.solverCategoryPostsLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = brandColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
            }
        }
    }
}
