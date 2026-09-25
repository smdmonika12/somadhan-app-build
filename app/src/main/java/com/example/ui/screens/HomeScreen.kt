package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import com.example.util.ImageStorageUtil
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.UserEntity
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.CategorySkillIndicator
import com.example.ui.components.ExpandablePaginatedSection
import com.example.ui.components.HomeFeedSkeleton
import com.example.ui.components.LoadingAwareContent
import com.example.ui.components.SyncAwareContent
import com.example.ui.components.NotificationBottomSheet
import com.example.ui.components.handleSomadhanNotification
import com.example.ui.components.ProblemCard
import com.example.ui.components.ProblemCardSkeleton
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.components.SomadhanBottomNav
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.SomadhanTopBar
import com.example.ui.navigation.Screen
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
import com.example.ui.theme.SomadhanOrangePressed
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
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

@Composable
fun HomeScreen(
    viewModel: SomadhanViewModel,
    onNavigate: (String) -> Unit,
    onProblemClick: (String) -> Unit,
    onPostProblemClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val isLocationUpdating by viewModel.isLocationUpdating.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val physicalCategoryRadiusKm by viewModel.physicalCategoryRadiusKm.collectAsStateWithLifecycle()
    val unreadChatCount by viewModel.unreadMessagesCount.collectAsStateWithLifecycle()

    // Tracks whether Home's core reference data (categories) has arrived at least once.
    // Session-aware: if Home already loaded earlier this app session, starts ready
    // immediately (no skeleton on revisit) - only a true first-load-this-session
    // shows the skeleton at all.
    // Loading/Sync Fix Roadmap v2, dhap 4 -- initialSyncPhase (bulk-pull), separate sessionKey "home_sync"
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    var showNotificationsSheet by remember { mutableStateOf(false) }

    val isSolver = currentUser?.role == "SOLVER"
    val currentUserId = currentUser?.id ?: ""
    val notificationsPaged = viewModel.notificationsPaged
    val notificationsLoadingMore = viewModel.notificationsLoadingMore
    val notificationsHasMore = viewModel.notificationsHasMore

    LaunchedEffect(showNotificationsSheet, currentUserId) {
        if (showNotificationsSheet && currentUserId.isNotBlank()) {
            viewModel.resetNotificationsPagination(currentUserId)
        }
    }

    if (showNotificationsSheet) {
        NotificationBottomSheet(
            notifications = notificationsPaged,
            hasMore = notificationsHasMore,
            loadingMore = notificationsLoadingMore,
            onLoadMore = {
                if (currentUserId.isNotBlank()) {
                    viewModel.loadNextNotificationsPage(currentUserId)
                }
            },
            onDismiss = { showNotificationsSheet = false },
            onMarkAllAsRead = { viewModel.markAllNotificationsRead() },
            onMarkAsRead = { notifId -> viewModel.markNotificationRead(notifId) },
            onNotificationItemClick = { notif ->
                showNotificationsSheet = false
                handleSomadhanNotification(
                    notif = notif,
                    onNavigate = onNavigate,
                    onProblemClick = onProblemClick,
                    isSolver = isSolver
                )
            },
            isSolver = isSolver
        )
    }

    Scaffold(
        topBar = {
            SomadhanTopBar(
                title = if (isSolver) "সমাধানকারী ফিড" else "সমাধান মার্কেটপ্লেস",
                currentUser = currentUser,
                unreadCount = unreadCount,
                onNotificationClick = { showNotificationsSheet = true },
                onAdminClick = null,
                onReputationClick = {
                    currentUser?.id?.let { uid ->
                        onNavigate(Screen.ReputationDetail.createRoute(uid))
                    }
                },
                locationAddress = liveLocation.address,
                onLocationRefresh = { viewModel.refreshLiveLocation() },
                isLocationUpdating = isLocationUpdating
            )
        },
        bottomBar = {
            val platformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
            val isWalletEnabled = platformSettings.find { it.key == "menu_wallet_enabled" }?.value != "false"
            val isInstantJobEnabled = platformSettings.find { it.key == "instant_job_feature_enabled" }?.value?.toBooleanStrictOrNull() ?: true
            SomadhanBottomNav(
                currentRoute = "home",
                isSolver = isSolver,
                isWalletEnabled = isWalletEnabled,
                isInstantJobEnabled = isInstantJobEnabled,
                unreadChatCount = unreadChatCount,
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            // ONLY User account has "Post Problem" button. Solver account does NOT have it.
            if (!isSolver) {
                FloatingActionButton(
                    onClick = onPostProblemClick,
                    containerColor = Color(0xFF1D4ED8),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("post_problem_fab")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "সমস্যা পোস্ট করুন")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সমস্যা পোস্ট", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        SomadhanPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = { if (isSolver) viewModel.refreshWalletData() else viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SomadhanBg)
        ) {
            SyncAwareContent(
                sessionKey = "home_sync",
                viewModel = viewModel,
                syncPhase = initialSyncPhase,
                onRetry = { viewModel.retryInitialSync() }
            ) {
                if (isSolver) {
                    SolverHomeContent(
                        viewModel = viewModel,
                        allCategories = allCategories,
                        onProblemClick = onProblemClick,
                        onNavigateToKyc = { onNavigate(Screen.SolverKyc.route) },
                        onNavigate = onNavigate
                    )
                } else {
                    UserHomeContent(
                        viewModel = viewModel,
                        allCategories = allCategories,
                        onProblemClick = onProblemClick,
                        onNavigate = onNavigate
                    )
                }
            }
        }
    }
}

@Composable
fun UserHomeNoticeBar(
    notices: List<String>,
    intervalSec: Int,
    modifier: Modifier = Modifier
) {
    if (notices.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, notices.size - 1)
    val currentNotice = notices[safeIndex]

    val scrollOffset = remember { Animatable(0f) }
    var textMeasuredWidthPx by remember(safeIndex, currentNotice) { mutableIntStateOf(0) }
    var containerWidthPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(safeIndex, currentNotice, textMeasuredWidthPx, containerWidthPx, intervalSec) {
        scrollOffset.snapTo(0f)
        val overflow = (textMeasuredWidthPx - containerWidthPx).coerceAtLeast(0)

        if (overflow > 0) {
            // Long notice: pause at start -> scroll left smoothly -> pause at end -> advance
            kotlinx.coroutines.delay(800L)
            val durationMs = ((overflow / 40f) * 1000).toLong().coerceIn(2000L, 15000L)
            scrollOffset.animateTo(
                targetValue = -overflow.toFloat() - 6f,
                animationSpec = tween(
                    durationMillis = durationMs.toInt(),
                    easing = LinearEasing
                )
            )
            kotlinx.coroutines.delay(1200L)
        } else {
            // Short notice: display for configured interval
            kotlinx.coroutines.delay((intervalSec.coerceAtLeast(2) * 1000).toLong())
        }

        if (notices.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % notices.size
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF1F3F5))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Fixed Notice Icon on the left
        Icon(
            imageVector = Icons.Default.Campaign,
            contentDescription = "নোটিশ",
            tint = Color(0xFF2563EB), // Vibrant blue
            modifier = Modifier.size(13.dp)
        )

        Spacer(modifier = Modifier.width(5.dp))

        // 2. Clipped text display window where long text scrolls left towards the fixed icon
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .clipToBounds(),
            contentAlignment = Alignment.CenterStart
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val maxWidthPx = with(density) { maxWidth.roundToPx() }
            containerWidthPx = maxWidthPx

            AnimatedContent(
                targetState = safeIndex,
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                            (slideOutVertically { height -> -height } + fadeOut())
                },
                label = "UserNoticeAnimation"
            ) { targetIdx ->
                val notice = notices.getOrNull(targetIdx) ?: ""
                val isCurrentActive = targetIdx == safeIndex

                Text(
                    text = notice,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4B5563),
                    softWrap = false,
                    maxLines = 1,
                    onTextLayout = { layoutResult ->
                        if (isCurrentActive) {
                            textMeasuredWidthPx = layoutResult.size.width
                        }
                    },
                    modifier = Modifier.offset {
                        if (isCurrentActive) {
                            IntOffset(scrollOffset.value.roundToInt(), 0)
                        } else {
                            IntOffset.Zero
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun UserHomeContent(
    viewModel: SomadhanViewModel,
    allCategories: List<CategoryEntity>,
    onProblemClick: (String) -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val selectedFilterCat by viewModel.userFilterCategory.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val activeCompletedProblems by viewModel.completedProblems.collectAsStateWithLifecycle()
    val filteredProblems by viewModel.userFilteredProblems.collectAsStateWithLifecycle()
    val categoryCounts by viewModel.categoryActivePostCounts.collectAsStateWithLifecycle()
    val unreadProblemCount by viewModel.unreadProblemNotificationCount.collectAsStateWithLifecycle()
    val allBids by viewModel.allBids.collectAsStateWithLifecycle()
    val allAdditionalCharges by viewModel.allAdditionalCharges.collectAsStateWithLifecycle()
    val userConversations by viewModel.userConversations.collectAsStateWithLifecycle()
    val userHasActiveJobs by viewModel.userHasActiveJobs.collectAsStateWithLifecycle()
    val userHasAnyUnseenActivity by viewModel.userHasAnyUnseenActivity.collectAsStateWithLifecycle()
    // rule ৩ (pull-to-refresh শিমার, নিচের completed-problems list-এর জন্য) — SomadhanPullToRefresh-এর
    // isRefreshing state HomeScreen()-এই collect হয় (SomadhanPullToRefresh ওখানেই বসানো), কিন্তু আসল
    // list এই sub-composable-এ থাকায় এখানেও আলাদাভাবে collect করা হচ্ছে (একই shared StateFlow, দুটো
    // collectAsStateWithLifecycle কল একই মান দেখায়)।
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val currentUserId = currentUser?.id ?: ""

    // 1. "সকল সমস্যা" (All Open Problems count on the platform)
    val allOpenProblemsCount = remember(allProblems) {
        allProblems.count { it.status == "OPEN" && !it.isDirectContract && it.isPublic && !it.isUserDeleted }
    }

    // 2. "আমার সমস্যা" (User's active/ongoing problems ONLY - excluding completed & cancelled)
    val userActiveProblemsCount = remember(allProblems, currentUserId) {
        if (currentUserId.isBlank()) 0
        else {
            allProblems.count {
                it.userId == currentUserId && (it.status == "OPEN" || it.status == "IN_PROGRESS" || it.status == "ACCEPTED") && !it.isUserDeleted
            }
        }
    }

    // 3. "বিড ম্যানেজমেন্ট" (Active ongoing problems count for User where bid is accepted):
    val userBidManagementActivityCount = remember(allProblems, currentUserId) {
        if (currentUserId.isBlank()) 0
        else {
            allProblems.count { prob ->
                prob.userId == currentUserId &&
                prob.status == "IN_PROGRESS" &&
                !prob.acceptedSolverId.isNullOrBlank() &&
                prob.status != "COMPLETED" &&
                prob.status != "CANCELLED" &&
                prob.status != "DISPUTED" &&
                !prob.isDisputed &&
                !prob.isUserDeleted
            }
        }
    }

    // 4. "পছন্দের সমাধানকারী" (User's Favorite Solvers Count)
    val userFavoriteSolversCount = remember(currentUser?.favoriteSolverIds) {
        currentUser?.favoriteSolverIds?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.size ?: 0
    }

    val platformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
    val allProblemsEnabled = platformSettings.find { it.key == "menu_all_problems_enabled" }?.value != "false"
    val myProblemsEnabled = platformSettings.find { it.key == "menu_my_problems_enabled" }?.value != "false"
    val bidManagementEnabled = platformSettings.find { it.key == "menu_bid_management_enabled" }?.value != "false"
    val favoriteSolversEnabled = platformSettings.find { it.key == "menu_favorite_solvers_enabled" }?.value != "false"
    val faqEnabled = platformSettings.find { it.key == "menu_faq_enabled" }?.value != "false"
    val supportCenterEnabled = platformSettings.find { it.key == "menu_support_center_enabled" }?.value != "false"

    val noticeIntervalSec = remember(platformSettings) {
        platformSettings.find { it.key == "user_home_notice_interval_sec" }?.value?.toIntOrNull() ?: 4
    }
    val rawNoticesString = platformSettings.find { it.key == "user_home_notices" }?.value
    val userHomeNotices = remember(rawNoticesString) {
        if (!rawNoticesString.isNullOrBlank()) {
            rawNoticesString.split("|||").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            listOf(
                "যেকোনো সেবা সহজে বুকিং করুন ও নিরাপদ সমাধান নিন।",
                "যাচাইকৃত দক্ষ সলভারদের মাধ্যমে দ্রুত কাজ সম্পন্ন করুন।",
                "নিরাপদ এসক্রো পেমেন্টে আপনার টাকা সর্বদা সুরক্ষিত।",
                "যেকোনো সমস্যায় আমাদের ২৪/৭ সাপোর্ট সেন্টারে যোগাযোগ করুন।"
            )
        }
    }

    val userMenuItems = remember(allProblemsEnabled, myProblemsEnabled, bidManagementEnabled, favoriteSolversEnabled, faqEnabled, supportCenterEnabled) {
        val list = mutableListOf<Triple<String, String, Pair<androidx.compose.ui.graphics.vector.ImageVector, Pair<Color, Color>>>>()
        if (allProblemsEnabled) {
            list.add(Triple("সকল সমস্যা", Screen.AllOpenProblems.route, Icons.Default.ListAlt to Pair(Color(0xFFEFF6FF), Color(0xFF1D4ED8))))
        }
        if (myProblemsEnabled) {
            list.add(Triple("আমার সমস্যা", Screen.UserProblems.route, Icons.Default.Assignment to Pair(Color(0xFFF0FDF4), Color(0xFF16A34A))))
        }
        if (bidManagementEnabled) {
            list.add(Triple("বিড ম্যানেজমেন্ট", Screen.BidManagement.route, Icons.Default.Gavel to Pair(Color(0xFFFFF7ED), Color(0xFFEA580C))))
        }
        if (favoriteSolversEnabled) {
            list.add(Triple("পছন্দের সমাধানকারী", Screen.FavoriteSolvers.route, Icons.Default.Favorite to Pair(Color(0xFFFDF2F8), Color(0xFFE11D48))))
        }
        if (faqEnabled) {
            list.add(Triple("FAQ", Screen.FAQ.route, Icons.Default.Help to Pair(Color(0xFFFAF5FF), Color(0xFF9333EA))))
        }
        if (supportCenterEnabled) {
            list.add(Triple("সহায়তা কেন্দ্র", Screen.SupportCenter.route, Icons.Default.SupportAgent to Pair(Color(0xFFECFEFF), Color(0xFF0891B2))))
        }
        list
    }

    val activeCategories = remember(allCategories) {
        allCategories.filter { it.isActive }
    }

    // UI state for User Menu Grid Expansion (show 6 initially if total > 6)
    var isUserMenuExpanded by rememberSaveable { mutableStateOf(false) }

    // Category filter state for completed jobs
    var selectedCompletedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var showCompletedCategoryFilterDialog by remember { mutableStateOf(false) }
    var showUserActiveJobsPopup by remember { mutableStateOf(false) }

    if (showUserActiveJobsPopup) {
        UserActiveJobsPopup(
            viewModel = viewModel,
            onDismiss = { showUserActiveJobsPopup = false },
            onProblemClick = { problemId ->
                showUserActiveJobsPopup = false
                val prob = allProblems.find { it.id == problemId }
                if (prob?.isInstantJob == true) {
                    onNavigate(Screen.JobTracking.createRoute(problemId))
                } else {
                    onNavigate(Screen.ProblemDetail.createRoute(problemId))
                }
            }
        )
    }

    val rawCompletedProblems = remember(activeCompletedProblems) {
        activeCompletedProblems.sortedByDescending { it.completedAt ?: it.createdAt }
    }

    val completedProblems = remember(rawCompletedProblems, selectedCompletedCategoryFilter) {
        if (selectedCompletedCategoryFilter.isNullOrBlank()) {
            rawCompletedProblems
        } else {
            rawCompletedProblems.filter { it.categoryId == selectedCompletedCategoryFilter }
        }
    }

    var completedCurrentPage by rememberSaveable { mutableIntStateOf(0) }
    val completedPageSize = 2

    val totalCompletedPages = if (completedProblems.isEmpty()) 1 else ceil(completedProblems.size.toDouble() / completedPageSize).toInt()
    val safeCompletedPage = completedCurrentPage.coerceIn(0, (totalCompletedPages - 1).coerceAtLeast(0))
    val pagedCompletedProblems = if (completedProblems.isNotEmpty()) {
        val start = safeCompletedPage * completedPageSize
        completedProblems.subList(start, min(start + completedPageSize, completedProblems.size))
    } else {
        emptyList()
    }

    // SOMADHAN_LOADING_PATTERN_MASTER_PROMPT, HomeScreen target design (ব্যাচ ১০, rule ৩+৪) — শুধু
    // এই completed-problems list অংশটুকু শিমার করবে, বাকি পুরো HomeScreen-এর কাঠামো (rule ২ অনুযায়ী)
    // কখনো flash হবে না। তাই ইচ্ছাকৃতভাবে [rememberFieldChangePulse]-এর `value`-তে পুরো তালিকা/আইটেম
    // না দিয়ে শুধু `safeCompletedPage` (পেজ-ইনডেক্স) দেওয়া হলো — এতে realtime-এ কোনো completed-problem
    // এর কোনো field বদলে গেলেও flash হয় না (rule ২), কিন্তু next/prev পেজ-ক্লিকে page index বদলালে
    // flash হয় (rule ৪)। `isManualRefreshing = isRefreshing` দিয়ে pull-to-refresh সাইকেল শেষ হলে flash
    // (rule ৩)। `sessionKey`/`viewModel` ইচ্ছাকৃতভাবে বাদ দেওয়া হয়েছে যাতে re-entry-flash মেকানিজম
    // ("পথ B") এখানে সম্পূর্ণ নিষ্ক্রিয় থাকে (rule ২-এর "আর কখনো flash না" শর্ত)।
    val isCompletedListRefreshing = rememberFieldChangePulse(
        value = safeCompletedPage,
        isManualRefreshing = isRefreshing
    )

    // Completed Category Filter Dialog
    if (showCompletedCategoryFilterDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showCompletedCategoryFilterDialog = false },
            title = {
                Text(
                    text = "সম্পন্ন কাজের ক্যাটাগরি ফিল্টার",
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
                        val isSelected = selectedCompletedCategoryFilter == null
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF9FAFB))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF1D4ED8) else Color(0xFFE5E7EB),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedCompletedCategoryFilter = null
                                    completedCurrentPage = 0
                                    showCompletedCategoryFilterDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "সব ক্যাটাগরি",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.5.sp,
                                color = if (isSelected) Color(0xFF1D4ED8) else SomadhanTextPrimary
                            )
                            Text(
                                text = "(${DistanceUtil.toBengaliDigits(rawCompletedProblems.size.toString())}টি)",
                                fontSize = 12.sp,
                                color = if (isSelected) Color(0xFF1D4ED8) else SomadhanTextSecondary
                            )
                        }
                    }

                    items(activeCategories, key = { it.id }) { cat ->
                        val isSelected = selectedCompletedCategoryFilter == cat.id
                        val count = rawCompletedProblems.count { it.categoryId == cat.id }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF9FAFB))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF1D4ED8) else Color(0xFFE5E7EB),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedCompletedCategoryFilter = cat.id
                                    completedCurrentPage = 0
                                    showCompletedCategoryFilterDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF1D4ED8) else SomadhanTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat.nameBangla,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.5.sp,
                                    color = if (isSelected) Color(0xFF1D4ED8) else SomadhanTextPrimary
                                )
                            }
                            Text(
                                text = "(${DistanceUtil.toBengaliDigits(count.toString())}টি)",
                                fontSize = 12.sp,
                                color = if (isSelected) Color(0xFF1D4ED8) else SomadhanTextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCompletedCategoryFilterDialog = false }) {
                    Text("বন্ধ করুন", color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 0. Active Jobs Button (Visible only when userHasActiveJobs is true)
        if (userHasActiveJobs) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .clickable {
                            showUserActiveJobsPopup = true
                        }
                        .testTag("user_active_jobs_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = "সক্রিয় কাজ",
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "সক্রিয় কাজ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            if (userHasAnyUnseenActivity) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                        .testTag("user_active_jobs_unseen_dot")
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = SomadhanTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // 1. User Activity / Status Card with Royal Blue Sapphire Gradient (matching User Profile)
        item {
            val userActiveCount = remember(allProblems, currentUser) {
                allProblems.count { it.userId == currentUser?.id && (it.status == "OPEN" || it.status == "IN_PROGRESS") && !it.isUserDeleted }
            }
            val userCompletedCount = remember(allProblems, currentUser) {
                allProblems.count { it.userId == currentUser?.id && it.status == "COMPLETED" && !it.isUserDeleted }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D4ED8)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_activity_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6),
                                    Color(0xFF1D4ED8),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 13.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Section: High-contrast White Wallet/List Container + Status Text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "সক্রিয় সমস্যা",
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(23.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "আপনার সমস্যা কার্যক্রম",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(userActiveCount.toString())}টি সমস্যা চলমান",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(1.dp))

                                Text(
                                    text = "মোট সমাধান হয়েছে: ${DistanceUtil.toBengaliDigits(userCompletedCount.toString())}টি",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        // Right Action: Elevated button navigating to User Problems Screen
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = CircleShape,
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable { onNavigate(Screen.UserProblems.route) }
                                .testTag("user_my_problems_btn")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "আমার সমস্যা সমূহ",
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // 2. User Home Services & Quick Access Section (Filtered Menu Grid Boxes)
        if (userMenuItems.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সেবাসমূহ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Animated Notice Bar with fixed notice icon & right-to-left scroll + vertical slide
                    if (userHomeNotices.isNotEmpty()) {
                        UserHomeNoticeBar(
                            notices = userHomeNotices,
                            intervalSec = noticeIntervalSec,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            val displayedMenuItems = if (!isUserMenuExpanded && userMenuItems.size > 6) {
                userMenuItems.take(6)
            } else {
                userMenuItems
            }

            items(displayedMenuItems.chunked(3), key = { rowItems -> rowItems.joinToString("|") { it.second } }) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for ((title, route, iconPair) in rowItems) {
                        val (iconVector, colors) = iconPair
                        val (iconBg, iconTint) = colors
                        val badgeCount = when (title) {
                            "সকল সমস্যা" -> allOpenProblemsCount
                            "আমার সমস্যা" -> userActiveProblemsCount
                            "বিড ম্যানেজমেন্ট" -> userBidManagementActivityCount
                            "পছন্দের সমাধানকারী" -> userFavoriteSolversCount
                            else -> 0
                        }
                        val badgeBg = when (title) {
                            "সকল সমস্যা" -> Color(0xFF1D4ED8)
                            "আমার সমস্যা" -> Color(0xFF16A34A)
                            "বিড ম্যানেজমেন্ট" -> Color(0xFFEA580C)
                            "পছন্দের সমাধানকারী" -> Color(0xFFE11D48)
                            else -> Color(0xFFEF4444)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    Color(0xFFE2E8F0),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    onNavigate(route)
                                }
                                .testTag("user_menu_card_${route}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(iconBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = title,
                                            tint = iconTint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    if (badgeCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 6.dp, y = (-4).dp)
                                                .clip(CircleShape)
                                                .background(badgeBg)
                                                .border(1.5.dp, Color.White, CircleShape)
                                                .padding(horizontal = 5.5.dp, vertical = 1.dp)
                                                .testTag("user_menu_badge_${route}")
                                        ) {
                                            Text(
                                                text = DistanceUtil.toBengaliDigits(
                                                    if (badgeCount > 99) "99+" else badgeCount.toString()
                                                ),
                                                color = Color.White,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = title,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    minLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                    if (rowItems.size < 3) {
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // "আরও দেখুন" / "সংক্ষেপ করুন" Button for User Home Menu (only if userMenuItems.size > 6)
            if (userMenuItems.size > 6) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(22.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier
                                .border(1.dp, Color(0xFF1D4ED8).copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                                .clickable { isUserMenuExpanded = !isUserMenuExpanded }
                                .testTag("toggle_expand_user_menu")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isUserMenuExpanded) "সংক্ষেপ করুন" else "আরও দেখুন",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (isUserMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isUserMenuExpanded) "সংক্ষেপ করুন" else "আরও দেখুন",
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Completed Problems Section Header ("সম্পন্ন হওয়া সমস্যার তালিকা (Xটি)" + Category Filter Button)
        item {
            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সম্পন্ন হওয়া সমস্যার তালিকা (${DistanceUtil.toBengaliDigits(completedProblems.size.toString())}টি)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                // Category Filter Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedCompletedCategoryFilter != null) Color(0xFF1D4ED8) else Color(0xFFF3F4F6))
                        .clickable { showCompletedCategoryFilterDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "ক্যাটাগরি ফিল্টার",
                            tint = if (selectedCompletedCategoryFilter != null) Color.White else SomadhanTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (selectedCompletedCategoryFilter != null) "ফিল্টারযুক্ত" else "ফিল্টার",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedCompletedCategoryFilter != null) Color.White else SomadhanTextSecondary
                        )
                    }
                }
            }

            // Active Filter Chip (if filtered by category)
            if (selectedCompletedCategoryFilter != null) {
                val selectedCatName = activeCategories.find { it.id == selectedCompletedCategoryFilter }?.nameBangla ?: "নির্বাচিত ক্যাটাগরি"
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFEFF6FF))
                        .border(1.dp, Color(0xFF1D4ED8).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .clickable {
                            selectedCompletedCategoryFilter = null
                            completedCurrentPage = 0
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ফিল্টার: $selectedCatName",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1D4ED8)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "রিমুভ ফিল্টার",
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // 8. Completed Problems List (2 items per page with Pagination)
        // rule ৩+৪ — pull-to-refresh শেষ হলে বা page বদলালে (isCompletedListRefreshing) এই অংশটুকু
        // সংক্ষিপ্ত সময়ের জন্য ProblemCardSkeleton() দেখায় (completedPageSize-সংখ্যক, যাতে ওই পেজে
        // যত কার্ড আশা করা হয় ততটাই skeleton দেখা যায়), তারপর আগের মতোই empty-state/real-list।
        // Pagination Controls (নিচে, এই if/else-এর বাইরে) rule ৩+৪-এর "কাঠামো ঠিক থাকবে" শর্ত মেনে
        // শিমার-অবস্থাতেও সবসময় স্থির/দৃশ্যমান থাকে, শুধু বাটন দুটো তখনও আগের মতোই enabled/disabled
        // লজিকে (Ground Rule ১ — বিজনেস লজিক অপরিবর্তিত)।
        if (isCompletedListRefreshing) {
            items(completedPageSize) {
                ProblemCardSkeleton()
            }
        } else if (completedProblems.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF2F2F2), RoundedCornerShape(16.dp))
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = SomadhanTextHint,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedCompletedCategoryFilter != null) {
                                "এই ক্যাটাগরিতে কোনো সম্পন্ন সমস্যা পাওয়া যায়নি।"
                            } else {
                                "এখনো কোনো সমস্যা সমাধান হয়নি।"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = SomadhanTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = pagedCompletedProblems,
                key = { it.id }
            ) { problem ->
                val poster = allUsers.find { it.id == problem.userId }
                CompletedProblemCard(
                    problem = problem,
                    posterUser = poster,
                    badgeTint = Color(0xFF1D4ED8),
                    badgeBg = Color(0xFFEFF6FF),
                    budgetColor = Color(0xFF1D4ED8),
                    onUserClick = if (currentUser?.id != problem.userId) {
                        { onNavigate(Screen.PublicProfile.createRoute(problem.userId, "USER")) }
                    } else null,
                    onClick = { onProblemClick(problem.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Pagination Controls for Completed Problems (কাঠামো ঠিক থাকা অংশ — শিমার-অবস্থাতেও স্থির)
        if (totalCompletedPages > 1) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (safeCompletedPage > 0) completedCurrentPage-- },
                        enabled = safeCompletedPage > 0,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "পূর্ববর্তী",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পূর্ববর্তী", fontSize = 12.sp)
                    }

                    Text(
                        text = "পৃষ্ঠা ${DistanceUtil.toBengaliDigits((safeCompletedPage + 1).toString())} / ${DistanceUtil.toBengaliDigits(totalCompletedPages.toString())}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanTextSecondary
                    )

                    OutlinedButton(
                        onClick = { if (safeCompletedPage < totalCompletedPages - 1) completedCurrentPage++ },
                        enabled = safeCompletedPage < totalCompletedPages - 1,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("পরবর্তী", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "পরবর্তী",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun SolverHomeContent(
    viewModel: SomadhanViewModel,
    allCategories: List<CategoryEntity>,
    onProblemClick: (String) -> Unit,
    onNavigateToKyc: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val solverCatIds by viewModel.solverSelectedCategoryIds.collectAsStateWithLifecycle()
    val categoryCounts by viewModel.categoryActivePostCounts.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val activeCompletedProblems by viewModel.completedProblems.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val allBids by viewModel.allBids.collectAsStateWithLifecycle()
    val userConversations by viewModel.userConversations.collectAsStateWithLifecycle()
    val solverHasActiveJobs by viewModel.solverHasActiveJobs.collectAsStateWithLifecycle()
    val solverHasAnyUnseenActivity by viewModel.solverHasAnyUnseenActivity.collectAsStateWithLifecycle()
    val physicalCategoryRadiusKm by viewModel.physicalCategoryRadiusKm.collectAsStateWithLifecycle()
    // rule ৩ (pull-to-refresh শিমার, নিচের completed-problems list-এর জন্য) — UserHomeContent-এর মতো
    // একই কারণে এখানেও আলাদাভাবে collect করা হচ্ছে (একই shared StateFlow, HomeScreen()-এর
    // SomadhanPullToRefresh-এ ব্যবহৃত মানের সাথে সবসময় সমান)।
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val currentUserId = currentUser?.id ?: ""

    // "বিড ম্যানেজমেন্ট" (Active accepted ongoing posts count for Solver):
    val solverBidManagementActivityCount = remember(allProblems, currentUserId) {
        if (currentUserId.isBlank()) 0
        else {
            allProblems.count { prob ->
                prob.acceptedSolverId == currentUserId &&
                prob.status == "IN_PROGRESS" &&
                !prob.isDisputed &&
                prob.status != "DISPUTED" &&
                !prob.isUserDeleted
            }
        }
    }

    val platformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
    val bidManagementEnabled = platformSettings.find { it.key == "menu_bid_management_enabled" }?.value != "false"
    val faqEnabled = platformSettings.find { it.key == "menu_faq_enabled" }?.value != "false"
    val supportCenterEnabled = platformSettings.find { it.key == "menu_support_center_enabled" }?.value != "false"

    val solverMenuItems = remember(bidManagementEnabled, faqEnabled, supportCenterEnabled) {
        val list = mutableListOf<Triple<String, String, Pair<androidx.compose.ui.graphics.vector.ImageVector, Pair<Color, Color>>>>()
        if (bidManagementEnabled) {
            list.add(Triple("বিড ম্যানেজমেন্ট", Screen.BidManagement.route, Icons.Default.Gavel to Pair(Color(0xFFFFF7ED), Color(0xFFEA580C))))
        }
        if (faqEnabled) {
            list.add(Triple("FAQ", Screen.FAQ.route, Icons.Default.Help to Pair(Color(0xFFFAF5FF), Color(0xFF9333EA))))
        }
        if (supportCenterEnabled) {
            list.add(Triple("সহায়তা কেন্দ্র", Screen.SupportCenter.route, Icons.Default.SupportAgent to Pair(Color(0xFFECFEFF), Color(0xFF0891B2))))
        }
        list
    }

    // Solver selected categories from profile (only active ones)
    val selectedSkillCategories = remember(allCategories, solverCatIds) {
        allCategories.filter { solverCatIds.contains(it.id) && it.isActive }
    }
    // All active categories with solver's selected categories placed first
    val activeCategories = remember(allCategories, solverCatIds) {
        val active = allCategories.filter { it.isActive }
        val (selected, unselected) = active.partition { solverCatIds.contains(it.id) }
        selected + unselected
    }

    // UI state for Balance Visibility Toggle (hidden by default)
    var isBalanceVisible by remember { mutableStateOf(false) }

    // UI state for Category Grid Expansion (show 6 initially if total > 6, expand to all on "আরও দেখুন")
    var isCategoriesExpanded by rememberSaveable { mutableStateOf(false) }

    // Category filter state for completed jobs
    var selectedCompletedCategoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var showCompletedCategoryFilterDialog by remember { mutableStateOf(false) }
    var showSolverActiveJobsPopup by remember { mutableStateOf(false) }

    if (showSolverActiveJobsPopup) {
        SolverActiveJobsPopup(
            viewModel = viewModel,
            onDismiss = { showSolverActiveJobsPopup = false },
            onProblemClick = { problemId ->
                showSolverActiveJobsPopup = false
                val prob = allProblems.find { it.id == problemId }
                if (prob?.isInstantJob == true) {
                    onNavigate(Screen.JobTracking.createRoute(problemId))
                } else {
                    onNavigate(Screen.ProblemDetail.createRoute(problemId))
                }
            }
        )
    }

    // All active completed problems across the platform (filtered by active categories)
    val rawCompletedProblems = remember(activeCompletedProblems) {
        activeCompletedProblems.sortedByDescending { it.completedAt ?: it.createdAt }
    }

    val completedProblems = remember(rawCompletedProblems, selectedCompletedCategoryFilter) {
        if (selectedCompletedCategoryFilter.isNullOrBlank()) {
            rawCompletedProblems
        } else {
            rawCompletedProblems.filter { it.categoryId == selectedCompletedCategoryFilter }
        }
    }

    var completedCurrentPage by rememberSaveable { mutableIntStateOf(0) }
    val completedPageSize = 2

    val totalCompletedPages = if (completedProblems.isEmpty()) 1 else ceil(completedProblems.size.toDouble() / completedPageSize).toInt()
    val safeCompletedPage = completedCurrentPage.coerceIn(0, (totalCompletedPages - 1).coerceAtLeast(0))
    val pagedCompletedProblems = if (completedProblems.isNotEmpty()) {
        val start = safeCompletedPage * completedPageSize
        completedProblems.subList(start, min(start + completedPageSize, completedProblems.size))
    } else {
        emptyList()
    }

    // SOMADHAN_LOADING_PATTERN_MASTER_PROMPT, HomeScreen target design (ব্যাচ ১০, rule ৩+৪) —
    // UserHomeContent-এর সমতুল্য ব্লকের মতোই একই কারণে `value = safeCompletedPage` (শুধু page-index,
    // realtime field-change-এ flash নয়) + `isManualRefreshing = isRefreshing`, `sessionKey`/`viewModel`
    // বাদ (re-entry-flash নিষ্ক্রিয়, rule ২)।
    val isCompletedListRefreshing = rememberFieldChangePulse(
        value = safeCompletedPage,
        isManualRefreshing = isRefreshing
    )

    // Category Filter Dialog for Completed Jobs
    if (showCompletedCategoryFilterDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showCompletedCategoryFilterDialog = false },
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
                        val isSelected = selectedCompletedCategoryFilter == null
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
                                    selectedCompletedCategoryFilter = null
                                    completedCurrentPage = 0
                                    showCompletedCategoryFilterDialog = false
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
                                text = "(${DistanceUtil.toBengaliDigits(rawCompletedProblems.size.toString())}টি)",
                                fontSize = 12.sp,
                                color = if (isSelected) SomadhanOrange else SomadhanTextSecondary
                            )
                        }
                    }

                    items(activeCategories, key = { it.id }) { cat ->
                        val isSelected = selectedCompletedCategoryFilter == cat.id
                        val count = rawCompletedProblems.count { it.categoryId == cat.id }
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
                                    selectedCompletedCategoryFilter = cat.id
                                    completedCurrentPage = 0
                                    showCompletedCategoryFilterDialog = false
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
                TextButton(onClick = { showCompletedCategoryFilterDialog = false }) {
                    Text("বন্ধ করুন", color = SomadhanOrange, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 0. Active Jobs Button (Visible only when solverHasActiveJobs is true)
        if (solverHasActiveJobs) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .clickable {
                            showSolverActiveJobsPopup = true
                        }
                        .testTag("solver_active_jobs_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SomadhanOrangeLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = "সক্রিয় কাজ",
                                    tint = SomadhanOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "সক্রিয় কাজ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            if (solverHasAnyUnseenActivity) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                        .testTag("solver_active_jobs_unseen_dot")
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = SomadhanTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // 1. KYC Warning Banner if not verified
        if (currentUser?.isKycVerified != true) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanErrorLight),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanError.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { onNavigateToKyc() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "KYC সতর্কতা",
                            tint = SomadhanError,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "KYC ভেরিফিকেশন বাকি আছে",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanError
                            )
                            Text(
                                text = "কাজে বিড করতে আপনার KYC সম্পন্ন করুন। এখানে ট্যাপ করুন।",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 2. Dashboard Styled Compact Orange Balance Card ("আপনার ব্যালেন্স")
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanOrange),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("solver_balance_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF8C1A),
                                    SomadhanOrange,
                                    Color(0xFFFF5500)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Section: High-contrast White Wallet Container + Balance Text (Non-clickable)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Rounded Square White Wallet Icon Container with Orange Icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "ব্যালেন্স",
                                    tint = SomadhanOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(11.dp))

                            // Middle Info Column
                            Column {
                                Text(
                                    text = "আপনার ব্যালেন্স",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )

                                Spacer(modifier = Modifier.height(1.dp))

                                if (isBalanceVisible) {
                                    // [ব্যালেন্স ফিক্স — ধাপ ১] active role অনুযায়ী role-scoped
                                    // balance পড়া হচ্ছে, শেয়ার্ড `balance` না (বাগ A)।
                                    val balanceVal = currentUser?.balanceSolver ?: 0.0 // SolverHomeContent শুধু solver role-এ দেখানো হয় (HomeScreen: if (isSolver) শাখা)
                                    Text(
                                        text = Formatters.formatTaka(balanceVal),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        text = "••••••",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 2.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(1.dp))

                                Text(
                                    text = if (isBalanceVisible) "ব্যালেন্স গোপন করতে চোখের আইকনে চাপুন" else "ব্যালেন্স দেখতে চোখের আইকনে চাপুন",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Right Action: Circular Elevated Eye Toggle Button (Only interactive element)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = CircleShape,
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable { isBalanceVisible = !isBalanceVisible }
                                .testTag("toggle_balance_visibility")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isBalanceVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isBalanceVisible) "ব্যালেন্স লুকান" else "ব্যালেন্স দেখুন",
                                    tint = SomadhanOrange,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // 3. Solver Selected Skill Categories Header & Chips ("আপনার স্কিল ক্যাটাগরি" - non-clickable)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "আপনার স্কিল ক্যাটাগরি (${DistanceUtil.toBengaliDigits(selectedSkillCategories.size.toString())}টি)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedSkillCategories.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "আপনার প্রোফাইলে এখনো কোনো স্কিল ক্যাটাগরি যুক্ত করা হয়নি।",
                            fontSize = 12.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(selectedSkillCategories, key = { it.id }) { cat ->
                        val (catBg, catTint) = CategoryIconHelper.getCategoryColors(cat.nameBangla, cat.iconName)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(catBg)
                                .border(1.dp, catTint.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                contentDescription = null,
                                tint = catTint,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.nameBangla,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. All Categories Section Header with Filter Logic Notice side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ক্যাটাগরি সমূহ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Location Bar Style Filter Logic Notice in 1 line beside the title
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F3F5))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "ফিল্টার লজিক",
                        tint = Color(0xFF2563EB), // Vibrant blue
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "ফিজিক্যাল কাজ ${DistanceUtil.toBengaliDigits(physicalCategoryRadiusKm.toInt().toString())} কিমির মধ্যে, ভার্চুয়াল কাজ সারা বাংলাদেশ।",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4B5563), // Soft gray
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // 5. Category Grid (Show 6 categories if > 6 with "আরও দেখুন" button, or all if <= 6 or expanded)
        val displayedCategories = if (!isCategoriesExpanded && activeCategories.size > 6) {
            activeCategories.take(6)
        } else {
            activeCategories
        }

        items(displayedCategories.chunked(3), key = { trio -> trio.joinToString("|") { it.id } }) { trio ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (cat in trio) {
                    val count = categoryCounts[cat.id] ?: 0
                    val isSkillSelected = solverCatIds.contains(cat.id)
                    val (catBg, catTint) = CategoryIconHelper.getCategoryColors(cat.nameBangla, cat.iconName)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSkillSelected) Color(0xFFFFFBF5) else Color(0xFFF8FAFC)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSkillSelected) 4.dp else 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isSkillSelected) 1.5.dp else 1.dp,
                                color = if (isSkillSelected) SomadhanOrange.copy(alpha = 0.7f) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onNavigate(Screen.SolverCategoryPosts.createRoute(cat.id))
                            }
                            .testTag("category_card_${cat.id}")
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Floating tooltip badge icon indicating "আপনার স্কিল"
                            if (isSkillSelected) {
                                CategorySkillIndicator(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(top = 6.dp, start = 6.dp),
                                    testTag = "skill_badge_${cat.id}"
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 12.dp, start = 4.dp, end = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(if (isSkillSelected) Color(0xFFFFEDD5) else catBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                            contentDescription = cat.nameBangla,
                                            tint = if (isSkillSelected) SomadhanOrange else catTint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    if (count > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 6.dp, y = (-4).dp)
                                                .clip(CircleShape)
                                                .background(if (isSkillSelected) SomadhanOrange else Color(0xFFEA580C))
                                                .border(1.5.dp, Color.White, CircleShape)
                                                .padding(horizontal = 5.5.dp, vertical = 1.dp)
                                                .testTag("category_badge_${cat.id}")
                                        ) {
                                            Text(
                                                text = DistanceUtil.toBengaliDigits(
                                                    if (count > 99) "99+" else count.toString()
                                                ),
                                                color = Color.White,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(7.dp))

                                Text(
                                    text = cat.nameBangla,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSkillSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = SomadhanTextPrimary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    minLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Balance space if row has fewer than 3 items
                if (trio.size < 3) {
                    repeat(3 - trio.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // "আরও দেখুন" / "সংক্ষেপ করুন" Button for Solver Categories (only if activeCategories.size > 6)
        if (activeCategories.size > 6) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(22.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .border(1.dp, SomadhanOrange.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                            .clickable { isCategoriesExpanded = !isCategoriesExpanded }
                            .testTag("toggle_expand_categories")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isCategoriesExpanded) "সংক্ষেপ করুন" else "আরও দেখুন",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isCategoriesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isCategoriesExpanded) "সংক্ষেপ করুন" else "আরও দেখুন",
                                tint = SomadhanOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 7. More Options Section (3-column Grid Boxes: Filtered based on settings)
        if (solverMenuItems.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "আরও অপশন",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(solverMenuItems.chunked(3), key = { rowItems -> rowItems.joinToString("|") { it.second } }) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for ((title, route, iconPair) in rowItems) {
                        val (iconVector, colors) = iconPair
                        val (iconBg, iconTint) = colors
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    Color(0xFFE2E8F0),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    onNavigate(route)
                                }
                                .testTag("solver_menu_card_${route}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    val badgeCount = when (title) {
                                        "বিড ম্যানেজমেন্ট" -> solverBidManagementActivityCount
                                        else -> 0
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(iconBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = title,
                                            tint = iconTint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    if (badgeCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 6.dp, y = (-4).dp)
                                                .clip(CircleShape)
                                                .background(SomadhanOrange)
                                                .border(1.5.dp, Color.White, CircleShape)
                                                .padding(horizontal = 5.5.dp, vertical = 1.dp)
                                                .testTag("solver_menu_badge_${route}")
                                        ) {
                                            Text(
                                                text = DistanceUtil.toBengaliDigits(
                                                    if (badgeCount > 99) "99+" else badgeCount.toString()
                                                ),
                                                color = Color.White,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = title,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    minLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                    if (rowItems.size < 3) {
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 8. Completed Work Section Header ("সম্পন্ন কাজের তালিকা (Xটি)" + Category Filter Button)
        item {
            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সম্পন্ন কাজের তালিকা (${DistanceUtil.toBengaliDigits(completedProblems.size.toString())}টি)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                // Category Filter Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedCompletedCategoryFilter != null) SomadhanOrange else Color(0xFFF3F4F6))
                        .clickable { showCompletedCategoryFilterDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "ক্যাটাগরি ফিল্টার",
                            tint = if (selectedCompletedCategoryFilter != null) Color.White else SomadhanTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (selectedCompletedCategoryFilter != null) "ফিল্টারযুক্ত" else "ফিল্টার",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedCompletedCategoryFilter != null) Color.White else SomadhanTextSecondary
                        )
                    }
                }
            }

            // Active Filter Chip (if filtered by category)
            if (selectedCompletedCategoryFilter != null) {
                val selectedCatName = activeCategories.find { it.id == selectedCompletedCategoryFilter }?.nameBangla ?: "নির্বাচিত ক্যাটাগরি"
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SomadhanOrangeLight)
                        .border(1.dp, SomadhanOrange.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .clickable {
                            selectedCompletedCategoryFilter = null
                            completedCurrentPage = 0
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
                        contentDescription = "রিমুভ ফিল্টার",
                        tint = SomadhanOrange,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // 8. Completed Problems List (2 items per page with Pagination)
        // rule ৩+৪ — UserHomeContent-এর সমতুল্য ব্লকের মতোই একই প্যাটার্নে: isCompletedListRefreshing
        // হলে completedPageSize-সংখ্যক ProblemCardSkeleton(), তারপর আগের empty-state/real-list অপরিবর্তিত।
        if (isCompletedListRefreshing) {
            items(completedPageSize) {
                ProblemCardSkeleton()
            }
        } else if (completedProblems.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF2F2F2), RoundedCornerShape(16.dp))
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = SomadhanTextHint,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedCompletedCategoryFilter != null) {
                                "এই ক্যাটাগরিতে কোনো সম্পন্ন কাজ পাওয়া যায়নি।"
                            } else {
                                "এখনো কোনো কাজ সম্পন্ন হয়নি।"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = SomadhanTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = pagedCompletedProblems,
                key = { it.id }
            ) { problem ->
                val poster = allUsers.find { it.id == problem.userId }
                CompletedProblemCard(
                    problem = problem,
                    posterUser = poster,
                    onUserClick = if (currentUser?.id != problem.userId) {
                        { onNavigate(Screen.PublicProfile.createRoute(problem.userId, "USER")) }
                    } else null,
                    onClick = { onProblemClick(problem.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Pagination Controls (Previous / Next / Page Indicator) — কাঠামো ঠিক থাকা অংশ, শিমার-অবস্থাতেও স্থির
        if (totalCompletedPages > 1) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Page Button
                    OutlinedButton(
                        onClick = { if (safeCompletedPage > 0) completedCurrentPage-- },
                        enabled = safeCompletedPage > 0,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "পূর্ববর্তী",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পূর্ববর্তী", fontSize = 12.sp)
                    }

                    // Page indicator
                    Text(
                        text = "পৃষ্ঠা ${DistanceUtil.toBengaliDigits((safeCompletedPage + 1).toString())} / ${DistanceUtil.toBengaliDigits(totalCompletedPages.toString())}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanTextSecondary
                    )

                    // Next Page Button
                    OutlinedButton(
                        onClick = { if (safeCompletedPage < totalCompletedPages - 1) completedCurrentPage++ },
                        enabled = safeCompletedPage < totalCompletedPages - 1,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("পরবর্তী", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "পরবর্তী",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

/**
 * Card for displaying completed problems with uniform orange category badge, physical/virtual badge, user profile image + name, budget, and completion time.
 */
@Composable
fun CompletedProblemCard(
    problem: ProblemEntity,
    posterUser: UserEntity? = null,
    badgeTint: Color = SomadhanOrange,
    badgeBg: Color = SomadhanOrangeLight,
    budgetColor: Color = SomadhanOrange,
    onUserClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFEFEFEF), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("completed_problem_card_${problem.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Uniform Orange Category Badge + Physical/Virtual Badge + Post ID & Completed Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag + Physical/Virtual Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Category badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = problem.categoryName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Physical or Virtual Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (problem.isPhysical) SomadhanSuccessLight else SomadhanInfoLight)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (problem.isPhysical) "ফিজিক্যাল" else "ভার্চুয়াল",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (problem.isPhysical) SomadhanSuccess else SomadhanInfo
                        )
                    }
                }

                // Completed Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFDCFCE7))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "সম্পন্ন",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "সম্পন্ন",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Problem Title
            Text(
                text = problem.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            if (problem.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = problem.description,
                    fontSize = 12.5.sp,
                    color = SomadhanTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFFF3F4F6)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Row: Budget & Poster Info (Photo + Name) + Completed time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Budget
                Column {
                    Text(
                        text = "পারিশ্রমিক / বাজেট",
                        fontSize = 10.sp,
                        color = SomadhanTextHint
                    )
                    Text(
                        text = if (problem.acceptedAmount != null && problem.acceptedAmount > 0) {
                            Formatters.formatTaka(problem.acceptedAmount)
                        } else {
                            Formatters.formatTakaRange(problem.minBudget, problem.maxBudget)
                        },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = budgetColor
                    )
                }

                // Poster & Time (With user profile photo)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = if (onUserClick != null) Modifier.clickable { onUserClick() } else Modifier
                ) {
                    // Profile Photo or Initial Circle
                    val photoUri = posterUser?.profileImageUri
                    val isPhotoValid = ImageStorageUtil.isValidDisplayUri(photoUri)
                    var loadFailed by remember(photoUri) { mutableStateOf(false) }
                    if (isPhotoValid && !loadFailed) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "প্রোফাইল ছবি",
                            contentScale = ContentScale.Crop,
                            onError = { loadFailed = true },
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.dp, badgeTint.copy(alpha = 0.4f), CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(badgeBg)
                                .border(1.dp, badgeTint.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (problem.userName.ifBlank { posterUser?.name ?: "U" }).take(1).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeTint
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = problem.userName.ifBlank { posterUser?.name ?: "গ্রাহক" },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = SomadhanTextHint
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Formatters.formatTimeAgo(problem.completedAt ?: problem.createdAt),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SomadhanTextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Reference-image styled problem card for the Solver Feed
 */
@Composable
fun SolverProblemCard(
    problem: ProblemEntity,
    posterUser: UserEntity? = null,
    solverLat: Double? = null,
    solverLon: Double? = null,
    onUserClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFEFEFEF), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("solver_problem_card_${problem.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Category Type Badge + Post ID + Budget Range & Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge
                if (!problem.isPhysical) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEFF6FF))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ভার্চুয়াল",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanInfo
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFF1EB))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ফিজিক্যাল",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanOrange
                        )
                    }
                }

                // Budget Column
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatTakaRange(problem.minBudget, problem.maxBudget),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                    Text(
                        text = "বাজেট",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = SomadhanTextHint
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Problem Title
            Text(
                text = problem.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Location & Posted Time Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = SomadhanOrange,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = problem.userAddress.ifBlank { "ঢাকা, বাংলাদেশ" },
                    fontSize = 11.sp,
                    color = SomadhanTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = SomadhanTextHint
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Formatters.formatTimeAgo(problem.createdAt),
                    fontSize = 11.sp,
                    color = SomadhanTextHint
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFFF3F4F6)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Row: Poster Info (Avatar, Name, Verified, Star Rating) + Distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.then(
                        if (onUserClick != null) Modifier.clickable { onUserClick() } else Modifier
                    )
                ) {
                    // Profile Photo or Initial Circle
                    val photoUri = posterUser?.profileImageUri
                    val isPhotoValid = ImageStorageUtil.isValidDisplayUri(photoUri)
                    var loadFailed by remember(photoUri) { mutableStateOf(false) }
                    if (isPhotoValid && !loadFailed) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "প্রোফাইল ছবি",
                            contentScale = ContentScale.Crop,
                            onError = { loadFailed = true },
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.dp, SomadhanOrange.copy(alpha = 0.4f), CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(SomadhanOrangeLight)
                                .border(1.dp, SomadhanOrange.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (problem.userName.ifBlank { posterUser?.name ?: "U" }).take(1).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = problem.userName.ifBlank { posterUser?.name ?: "গ্রাহক" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanTextPrimary
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Verified Badge Checkmark
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "ভেরিফাইড",
                        tint = SomadhanInfo,
                        modifier = Modifier.size(13.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Star Rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "4.8",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4B5563)
                        )
                    }
                }

                // Distance Calculation
                if (problem.isPhysical && solverLat != null && solverLon != null && problem.latitude != 0.0 && problem.longitude != 0.0) {
                    val distKm = DistanceUtil.calculateDistanceKm(
                        solverLat,
                        solverLon,
                        problem.latitude,
                        problem.longitude
                    )
                    Text(
                        text = "📍 ${DistanceUtil.formatDistance(distKm)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SomadhanTextSecondary
                    )
                }
            }
        }
    }
}
