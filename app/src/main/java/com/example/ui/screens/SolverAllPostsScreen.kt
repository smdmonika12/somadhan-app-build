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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.ui.components.ProblemCard
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.SomadhanBottomNav
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.navigation.Screen
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverAllPostsScreen(
    viewModel: SomadhanViewModel,
    onNavigate: ((String) -> Unit)? = null,
    onNavigateBack: () -> Unit,
    onProblemClick: (String) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val openProblems by viewModel.openProblems.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val selectedFilterCat by viewModel.solverFeedFilterCategory.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Loading/Sync Fix Roadmap v2, dhap 4 -- initialSyncPhase (bulk-pull), separate sessionKey "solver_all_posts_sync"
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()
    var searchInput by rememberSaveable { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.resetSolverAllPostsPagination()
    }

    // Search / category filtering needs to see the whole locally-synced scoped
    // set (openProblems, cheap in-memory filter) so results aren't limited to
    // whatever pages happen to be loaded. With no active filter, render the
    // incrementally-loaded page buffer instead of dumping the full set at once.
    val hasActiveFilter = searchInput.isNotBlank() || selectedFilterCat != null
    val pagedPostsSize = viewModel.solverAllPostsPaged.size

    val displayedPosts = remember(openProblems, searchInput, selectedFilterCat, hasActiveFilter, pagedPostsSize) {
        val source = if (hasActiveFilter) openProblems else viewModel.solverAllPostsPaged
        source.filter { problem ->
            val matchesCategory = selectedFilterCat == null || problem.categoryId == selectedFilterCat
            val matchesSearch = searchInput.isBlank() ||
                problem.title.contains(searchInput, ignoreCase = true) ||
                problem.description.contains(searchInput, ignoreCase = true)
            matchesCategory && matchesSearch
        }.sortedByDescending { it.createdAt }
    }

    val selectedCategoryObj = allCategories.find { it.id == selectedFilterCat }
    val selectedCategoryName = selectedCategoryObj?.nameBangla ?: "সকল ক্যাটাগরি"

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "সকল পোস্টের তালিকা (${DistanceUtil.toBengaliDigits(displayedPosts.size.toString())})",
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
                    isSolver = true,
                    onRefresh = { viewModel.refreshLiveLocation() }
                )
            }
        },
        bottomBar = {
            if (onNavigate != null) {
                val platformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
                val isInstantJobEnabled = platformSettings.find { it.key == "instant_job_feature_enabled" }?.value?.toBooleanStrictOrNull() ?: true
                SomadhanBottomNav(
                    currentRoute = "solver_all_posts",
                    isSolver = true,
                    isInstantJobEnabled = isInstantJobEnabled,
                    onNavigate = onNavigate
                )
            }
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        SomadhanPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                viewModel.resetSolverAllPostsPagination()
                viewModel.refreshData()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SomadhanBg)
        ) {
            // Loading/Sync Fix Roadmap v2, dhap 7 (batch 2, pagination-screen migration) --
            // এতদিন এখানে data-বিহীন SyncAwareContent ছিল (rule ১ কভার করতো, কিন্তু rule ২/৪-এর
            // ডেটা-ডিফ শিমার ছিল না)। এখন SyncAwareRefreshableContent-এ migrate করা হলো, data =
            // displayedPosts (search/category filter + pagination-loaded বাফার একসাথে
            // reflect করে) — যাতে realtime আপডেট/re-entry/pull-to-refresh/load-more সবগুলোতেই
            // structure-preserving শিমার (rule ১+২+৩+৪) কাজ করে। header/search/filter এই
            // স্ক্রিনে আগে থেকেই LazyColumn-এর ভেতরের item হিসেবে ছিল (আলাদা Scaffold topBar
            // নয়) -- সেই বিদ্যমান লে-আউট অপরিবর্তিত রাখা হয়েছে, শুধু কম্পোনেন্ট বদলানো হলো।
            SyncAwareRefreshableContent(
                sessionKey = "solver_all_posts_sync",
                viewModel = viewModel,
                syncPhase = initialSyncPhase,
                data = displayedPosts,
                onRetry = { viewModel.retryInitialSync() },
                isManualRefreshing = isRefreshing
            ) { posts ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
            // Search Input
            item {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = {
                        searchInput = it
                        viewModel.setSolverSearchQuery(it)
                    },
                    placeholder = {
                        Text(
                            text = "সব কাজের মধ্য থেকে খুঁজুন...",
                            color = SomadhanTextHint,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "সার্চ", tint = SomadhanTextSecondary)
                    },
                    trailingIcon = {
                        if (searchInput.isNotEmpty()) {
                            IconButton(onClick = {
                                searchInput = ""
                                viewModel.setSolverSearchQuery("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "মুছুন", tint = SomadhanTextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        unfocusedBorderColor = SomadhanBorder,
                        focusedContainerColor = SomadhanBg,
                        unfocusedContainerColor = SomadhanCardBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("solver_all_posts_search_input")
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Category Dropdown Filter (Requirement 11.2)
            item {
                Text(
                    text = "ক্যাটাগরি নির্বাচন করুন:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SomadhanTextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = SomadhanOrange) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("সকল ক্যাটাগরি (সবগুলো)", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            onClick = {
                                viewModel.setSolverFeedFilterCategory(null)
                                categoryDropdownExpanded = false
                            }
                        )

                        val activeCategories = remember(allCategories) {
                            allCategories.filter { it.isActive }
                        }
                        activeCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(category.iconName),
                                            contentDescription = null,
                                            tint = SomadhanOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(category.nameBangla, fontSize = 13.sp)
                                    }
                                },
                                onClick = {
                                    viewModel.setSolverFeedFilterCategory(category.id)
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            if (posts.isEmpty() && !viewModel.solverAllPostsLoadingMore) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
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
                                text = "কোনো পোস্ট পাওয়া যায়নি।",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }
                    }
                }
            } else {
                items(posts, key = { it.id }) { problem ->
                    val posterUser = allUsers.find { it.id == problem.userId }
                    ProblemCard(
                        problem = problem,
                        posterUser = posterUser,
                        solverLat = if (liveLocation.latitude != 0.0) liveLocation.latitude else currentUser?.latitude,
                        solverLon = if (liveLocation.longitude != 0.0) liveLocation.longitude else currentUser?.longitude,
                        onUserClick = if (onNavigate != null && currentUser?.id != problem.userId) {
                            { onNavigate(Screen.PublicProfile.createRoute(problem.userId, "USER")) }
                        } else null,
                        onClick = { onProblemClick(problem.id) }
                    )
                }

                // Infinite Scroll Footer (only while browsing the unfiltered feed —
                // an active search/category filter already shows full matching results)
                item {
                    if (!hasActiveFilter && viewModel.solverAllPostsHasMore) {
                        LaunchedEffect(Unit) {
                            viewModel.loadNextSolverAllPostsPage()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.solverAllPostsLoadingMore) {
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
