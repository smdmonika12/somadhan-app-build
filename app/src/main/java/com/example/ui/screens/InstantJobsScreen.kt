package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.BidEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ProblemEntity
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.PulsingValue
import com.example.ui.components.SyncAwareContent
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.components.NotificationBottomSheet
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.SomadhanBottomNav
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.SomadhanTopBar
import com.example.ui.components.handleSomadhanNotification
import com.example.ui.navigation.Screen
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanInfoLight
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.NearbyInstantJobItem
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstantJobsScreen(
    viewModel: SomadhanViewModel,
    onNavigate: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onProblemClick: ((String) -> Unit)? = null,
    initialBidProblemId: String? = null
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val isLocationUpdating by viewModel.isLocationUpdating.collectAsStateWithLifecycle()
    val activeJob by viewModel.activeInstantJobForSolver.collectAsStateWithLifecycle()
    val nearbyJobs by viewModel.broadcastingInstantJobsForSolver.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    var showNotificationsSheet by remember { mutableStateOf(false) }
    val isToggleOn = currentUser?.instantJobNotificationsEnabled ?: true
    var biddingJob by remember { mutableStateOf<ProblemEntity?>(null) }
    var dismissedJobIds by remember { mutableStateOf(setOf<String>()) }
    val displayNearbyJobs = remember(nearbyJobs, dismissedJobIds) {
        nearbyJobs.filter { it.problem.id !in dismissedJobIds }
    }
    val allBids by viewModel.allBids.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()

    // Loading/Sync Fix Roadmap v2, dhap 4 -- initialSyncPhase (bulk-pull), separate sessionKey "instant_jobs_sync"
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    // যদি কোনো ম্যাচিং (ক্যাটাগরি + রাডার শর্ত মিলে যাওয়া) সলভার সরাসরি কোনো নির্দিষ্ট জরুরি
    // পোস্টে ক্লিক করে এই হাব পেজে আসে (initialBidProblemId দিয়ে), তাহলে "বিড দিন" বাটনে ক্লিক
    // করলে যে বিড পপআপ খোলে, সেটা এখানে সরাসরি স্বয়ংক্রিয়ভাবে খুলে দেওয়া হচ্ছে।
    var autoOpenedBidTargetId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(initialBidProblemId, displayNearbyJobs) {
        if (!initialBidProblemId.isNullOrBlank() && autoOpenedBidTargetId != initialBidProblemId) {
            val targetJob = displayNearbyJobs.find { it.problem.id == initialBidProblemId }?.problem
            if (targetJob != null) {
                biddingJob = targetJob
                autoOpenedBidTargetId = initialBidProblemId
            }
        }
    }

    if (showNotificationsSheet) {
        NotificationBottomSheet(
            notifications = notifications,
            onDismiss = { showNotificationsSheet = false },
            onMarkAllAsRead = { viewModel.markAllNotificationsRead() },
            onMarkAsRead = { notifId -> viewModel.markNotificationRead(notifId) },
            onNotificationItemClick = { notif ->
                showNotificationsSheet = false
                handleSomadhanNotification(
                    notif = notif,
                    onNavigate = onNavigate,
                    onProblemClick = onProblemClick ?: {},
                    isSolver = true
                )
            },
            isSolver = true
        )
    }

    Scaffold(
        topBar = {
            SomadhanTopBar(
                title = "জরুরি জব হাব",
                currentUser = currentUser,
                unreadCount = unreadCount,
                onNotificationClick = { showNotificationsSheet = true },
                onAdminClick = null,
                onReputationClick = {
                    currentUser?.id?.let { uid ->
                        onNavigate(Screen.ReputationDetail.createRoute(uid))
                    }
                },
                onHistoryClick = {
                    onNavigate(Screen.InstantJobHistory.route)
                },
                locationAddress = liveLocation.address,
                onLocationRefresh = { viewModel.refreshLiveLocation() },
                isLocationUpdating = isLocationUpdating
            )
        },
        bottomBar = {
            val platformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
            val isInstantJobEnabled = platformSettings.find { it.key == "instant_job_feature_enabled" }?.value?.toBooleanStrictOrNull() ?: true
            SomadhanBottomNav(
                currentRoute = "instant_jobs",
                isSolver = true,
                isWalletEnabled = true,
                isInstantJobEnabled = isInstantJobEnabled,
                onNavigate = onNavigate
            )
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        SomadhanPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ব্যাচ ৩১, সেশন ২.২ — এই সেশন প্রম্পটে "শুধু location-সংক্রান্ত section" বলা থাকলেও এই
            // স্ক্রিনে আলাদা কোনো location-card নেই বলে ব্যবহারকারীর কাছে স্পষ্টীকরণ করা হয়েছে
            // (ধাপ ০.১৬): re-entry/pull-to-refresh-এ পুরো পেজ শিমার না হয়ে শুধু নিচের
            // "আশেপাশের লাইভ জরুরি পোস্ট" (active-bid) লিস্ট অংশটুকুই শিমার হবে — active job
            // banner/toggle card স্থির থাকবে। তাই বাইরের wrapper আগের কম্পোজিট-`data`-ভিত্তিক
            // SyncAwareRefreshableContent থেকে SyncAwareContent (শুধু cold-load-এর জন্য)-এ
            // পাল্টানো হলো, আর নিচে শুধু জব-লিস্ট সেকশনের জন্য একটা আলাদা rememberFieldChangePulse
            // যোগ করা হলো (Wallet/Dashboard/ProblemDetail স্ক্রিনে যেমন করা হয়েছে সেই একই প্যাটার্ন)।
            SyncAwareContent(
                sessionKey = "instant_jobs_sync",
                viewModel = viewModel,
                syncPhase = initialSyncPhase,
                onRetry = { viewModel.retryInitialSync() }
            ) {
            val nearbyJobsPulse = rememberFieldChangePulse(
                value = displayNearbyJobs,
                isManualRefreshing = isRefreshing,
                sessionKey = "instant_jobs_sync",
                viewModel = viewModel
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("instant_jobs_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Active Job Banner (If solver already accepted an instant job)
                if (activeJob != null) {
                    item(key = "active_job_card") {
                        ActiveInstantJobCard(
                            job = activeJob!!,
                            onOpenTracking = {
                                onNavigate(Screen.JobTracking.createRoute(activeJob!!.id))
                            }
                        )
                    }
                }

                // 3. Toggle Card (Always present or when no active job)
                item(key = "toggle_card") {
                    InstantJobToggleCard(
                        enabled = isToggleOn,
                        onToggleChanged = { viewModel.updateInstantJobToggle(it) }
                    )
                }

                // 4. Content based on Toggle State & Active Job State
                if (activeJob == null) {
                    if (!isToggleOn) {
                        // Toggle is OFF
                        item(key = "toggle_off_state") {
                            ToggleOffEmptyCard(
                                onTurnOnClick = { viewModel.updateInstantJobToggle(true) }
                            )
                        }
                    } else {
                        // Toggle is ON -> Show Broadcasting Jobs
                        // এই সেকশনের হেডার + লিস্ট/empty-state — এই পুরোটুকুই nearbyJobsPulse দিয়ে
                        // pulse করে (ব্যাচ ৩১, সেশন ২.২), বাকি বডি (active job banner/toggle card)
                        // স্থির।
                        item(key = "section_header") {
                            PulsingValue(isUpdating = nearbyJobsPulse) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        PulsingLiveDot()
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "আশেপাশের লাইভ জরুরি পোস্ট",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = SomadhanTextPrimary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SomadhanOrangeLight
                                    ) {
                                        Text(
                                            text = "${DistanceUtil.toBengaliDigits(displayNearbyJobs.size)} টি প্রাপ্ত",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = SomadhanOrange,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (displayNearbyJobs.isEmpty()) {
                            item(key = "empty_jobs_state") {
                                PulsingValue(isUpdating = nearbyJobsPulse) {
                                    RadarSearchingEmptyCard()
                                }
                            }
                        } else {
                            items(displayNearbyJobs, key = { it.problem.id }) { jobItem ->
                                val category = allCategories.find { it.id == jobItem.problem.categoryId }
                                val myBid = allBids.find {
                                    it.problemId == jobItem.problem.id &&
                                    it.solverId == currentUser?.id &&
                                    it.status == "PENDING"
                                }
                                val hasBid = myBid != null

                                PulsingValue(isUpdating = nearbyJobsPulse) {
                                    NearbyJobCard(
                                        jobItem = jobItem,
                                        iconName = category?.iconName ?: "Bolt",
                                        hasAlreadyBid = hasBid,
                                        onBidClick = {
                                            biddingJob = jobItem.problem
                                        },
                                        onCardClick = {
                                            if (onProblemClick != null) {
                                                onProblemClick(jobItem.problem.id)
                                            } else {
                                                onNavigate(Screen.ProblemDetail.createRoute(jobItem.problem.id))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            } // SyncAwareContent (ব্যাচ ৩১, সেশন ২.২ migration) বন্ধ
        }
    }

    // Bid Submission Bottom Sheet
    biddingJob?.let { job ->
        val category = allCategories.find { it.id == job.categoryId }
        InstantJobBidBottomSheet(
            problem = job,
            category = category,
            viewModel = viewModel,
            onDismiss = { biddingJob = null },
            onBidSuccess = {
                biddingJob = null
            }
        )
    }
}

@Composable
fun InstantJobToggleCard(
    enabled: Boolean,
    onToggleChanged: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (enabled) SomadhanOrange.copy(alpha = 0.4f) else SomadhanDivider,
                RoundedCornerShape(16.dp)
            )
            .testTag("instant_job_toggle_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (enabled) SomadhanOrangeLight else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (enabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = if (enabled) SomadhanOrange else SomadhanTextHint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "জরুরি জব নোটিফিকেশন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = if (enabled) "সক্রিয় রয়েছে (ON)" else "বন্ধ রয়েছে (OFF)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (enabled) SomadhanOrange else SomadhanTextHint
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SomadhanOrange,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = SomadhanBorder
                    ),
                    modifier = Modifier.testTag("instant_job_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (enabled) {
                    "আপনার ক্যাটাগরির আশেপাশের জরুরি জব দেখতে ও notification পেতে থাকবেন"
                } else {
                    "বন্ধ থাকলে আপনি কোনো জরুরি জব দেখবেন না বা notification পাবেন না"
                },
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = if (enabled) SomadhanTextSecondary else SomadhanTextHint
            )
        }
    }
}

@Composable
fun ToggleOffEmptyCard(
    onTurnOnClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp)
            .testTag("toggle_off_empty_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "নোটিফিকেশন বন্ধ আছে",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF92400E)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "নোটিফিকেশন বন্ধ থাকায় আপনি কোনো জরুরি জব দেখতে পাচ্ছেন না। চালু করলে আশেপাশের তাৎক্ষণিক জরুরি জবগুলো দেখতে পাবেন।",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFF78350F),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onTurnOnClick,
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("turn_on_instant_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "নোটিফিকেশন চালু করুন ⚡",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun RadarSearchingEmptyCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SomadhanDivider, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp)
            .testTag("radar_searching_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SomadhanOrangeLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = SomadhanOrange,
                    modifier = Modifier
                        .size(36.dp)
                        .alpha(alpha)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "আশেপাশে বর্তমানে কোনো জরুরি জব নেই",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = SomadhanTextPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "আপনার স্কিল ও এলাকার আশেপাশে কোনো নতুন জরুরি কাজ পোস্ট হলে সাথে সাথে এখানে ভেসে উঠবে ও আপনার ফোনে নোটিফিকেশন পৌঁছে যাবে।",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = SomadhanTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun ActiveInstantJobCard(
    job: ProblemEntity,
    onOpenTracking: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFF3B82F6), RoundedCornerShape(16.dp))
            .testTag("solver_active_instant_job_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .background(Color(0xFFDBEAFE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "আপনার একটি কাজ চলমান রয়েছে",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E3A8A)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDBEAFE)
                ) {
                    val statusText = when (job.jobStatus) {
                        "ACCEPTED", "SOLVER_ACCEPTED" -> "গৃহীত হয়েছে"
                        "SOLVER_EN_ROUTE", "ON_THE_WAY" -> "পথে রয়েছেন"
                        "ARRIVED" -> "পৌঁছেছেন"
                        "WORK_IN_PROGRESS", "IN_PROGRESS" -> "কাজ চলছে"
                        else -> job.jobStatus ?: "চলমান"
                    }
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = job.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = SomadhanTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (job.userAddress.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SomadhanTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = job.userAddress,
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onOpenTracking,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("open_live_tracking_button")
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "লাইভ ট্র্যাকিং-এ যান ⚡",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun NearbyJobCard(
    jobItem: NearbyInstantJobItem,
    iconName: String,
    hasAlreadyBid: Boolean = false,
    onBidClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val problem = jobItem.problem
    val (catBg, catTint) = CategoryIconHelper.getCategoryColors(problem.categoryName, iconName)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (hasAlreadyBid) SomadhanSuccess.copy(alpha = 0.5f) else SomadhanDivider,
                RoundedCornerShape(16.dp)
            )
            .clickable { onCardClick() }
            .testTag("instant_job_card_${problem.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category & Distance Row + Post ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = catBg
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = CategoryIconHelper.getIcon(iconName),
                                contentDescription = null,
                                tint = catTint,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = problem.categoryName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = catTint
                            )
                        }
                    }

                    // Post ID Chip (1-click copyable)
                    val cleanPostId = problem.id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SomadhanBg)
                            .border(0.6.dp, SomadhanBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(cleanPostId))
                                Toast.makeText(context, "পোস্ট ID কপি করা হয়েছে: $cleanPostId", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .testTag("nearby_job_id_${problem.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            tint = SomadhanTextHint,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "ID: $cleanPostId",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanTextSecondary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "কপি করুন",
                            tint = SomadhanTextHint,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SomadhanSuccessLight
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = SomadhanSuccess,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = jobItem.formattedDistance,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanSuccess
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = problem.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = SomadhanTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (problem.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = problem.description,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = SomadhanTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (problem.userAddress.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = problem.userAddress,
                        fontSize = 12.sp,
                        color = SomadhanTextHint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom action & budget row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "গ্রাহকের প্রস্তাবিত বাজেট",
                        fontSize = 11.sp,
                        color = SomadhanTextHint
                    )
                    val budgetText = if (problem.maxBudget > problem.minBudget) {
                        "৳ ${DistanceUtil.toBengaliDigits(problem.minBudget)} - ${DistanceUtil.toBengaliDigits(problem.maxBudget)}"
                    } else {
                        "৳ ${DistanceUtil.toBengaliDigits(problem.minBudget)}"
                    }
                    Text(
                        text = budgetText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SomadhanOrange
                    )
                }

                if (hasAlreadyBid) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SomadhanSuccessLight,
                        modifier = Modifier.height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SomadhanSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "বিড দেওয়া হয়েছে ✅",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SomadhanSuccess
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onBidClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SomadhanOrange
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("bid_job_${problem.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "বিড দিন ⚡",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstantJobBidBottomSheet(
    problem: ProblemEntity,
    category: CategoryEntity?,
    viewModel: SomadhanViewModel,
    onDismiss: () -> Unit,
    onBidSuccess: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var bidAmountText by remember { mutableStateOf(problem.minBudget.toInt().toString()) }
    var arrivalTimeMinutes by remember { mutableStateOf("15") }
    var noteText by remember { mutableStateOf("আমি প্রয়োজনীয় যন্ত্রপাতি সহ প্রস্তুত আছি, অবিলম্বে রওনা হব।") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var estimatedCommissionRate by remember { mutableStateOf(10.0) }

    androidx.compose.runtime.LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let { solverId ->
            estimatedCommissionRate = viewModel.estimateCommissionRate(solverId)
        }
    }

    val arrivalOptions = listOf("10", "15", "20", "30", "45")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SomadhanCardBg,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp),
                shape = RoundedCornerShape(2.dp),
                color = SomadhanDivider
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("instant_job_bid_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SomadhanOrangeLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "জরুরি কাজে প্রস্তাব (বিড) দিন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = problem.title,
                            fontSize = 12.sp,
                            color = SomadhanTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = SomadhanTextHint
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Job Summary Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SomadhanBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "গ্রাহকের প্রস্তাবিত বাজেট",
                            fontSize = 12.sp,
                            color = SomadhanTextHint
                        )
                        val sheetBudgetText = if (problem.maxBudget > problem.minBudget) {
                            "৳ ${DistanceUtil.toBengaliDigits(problem.minBudget)} - ${DistanceUtil.toBengaliDigits(problem.maxBudget)}"
                        } else {
                            "৳ ${DistanceUtil.toBengaliDigits(problem.minBudget)}"
                        }
                        Text(
                            text = sheetBudgetText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SomadhanOrange
                        )
                    }

                    if (problem.userAddress.isNotBlank()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "কাজের এলাকা",
                                fontSize = 12.sp,
                                color = SomadhanTextHint
                            )
                            Text(
                                text = problem.userAddress,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SomadhanTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Bid Amount Input
            Text(
                text = "আপনার অফার মূল্য (টাকা)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SomadhanTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = bidAmountText,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) {
                        bidAmountText = it
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                leadingIcon = {
                    Text(
                        text = "৳",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SomadhanOrange,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SomadhanOrange,
                    unfocusedBorderColor = SomadhanBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bid_amount_input")
            )

            // Dynamic Commission Estimate (Phase M.2)
            val parsedBidAmount = bidAmountText.toDoubleOrNull() ?: 0.0
            if (parsedBidAmount > 0.0) {
                val estimatedCommission = (parsedBidAmount * estimatedCommissionRate) / 100.0
                val estimatedEarnings = (parsedBidAmount - estimatedCommission).coerceAtLeast(0.0)

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (estimatedCommissionRate == 0.0) SomadhanSuccessLight else SomadhanInfoLight.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (estimatedCommissionRate == 0.0) SomadhanSuccess.copy(alpha = 0.35f) else SomadhanInfo.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("estimated_commission_box")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "কমিশন (আনুমানিক):",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = if (estimatedCommissionRate == 0.0) {
                                    "৳ ০ (০% ফ্রি কোটা)"
                                } else {
                                    "৳ ${DistanceUtil.toBengaliDigits(estimatedCommission)} (${DistanceUtil.toBengaliDigits(estimatedCommissionRate)}%)"
                                },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (estimatedCommissionRate == 0.0) SomadhanSuccess else SomadhanTextPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "আপনি পাবেন (আনুমানিক):",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanSuccess
                            )
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(estimatedEarnings)}",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SomadhanSuccess
                            )
                        }

                        Text(
                            text = "ℹ️ চূড়ান্ত রেট গ্রাহক কাজ গ্রহণের সময় লক হবে (রেপুটেশন ও ফ্রি-কোটা অনুযায়ী)",
                            fontSize = 10.5.sp,
                            color = SomadhanTextHint,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Arrival Time Selector
            Text(
                text = "কত মিনিটে পৌঁছাতে পারবেন?",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SomadhanTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(arrivalOptions, key = { it }) { minutes ->
                    val isSelected = arrivalTimeMinutes == minutes
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) SomadhanOrangeLight else SomadhanBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) SomadhanOrange else SomadhanBorder
                        ),
                        modifier = Modifier
                            .clickable { arrivalTimeMinutes = minutes }
                            .testTag("arrival_time_$minutes")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = if (isSelected) SomadhanOrange else SomadhanTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${DistanceUtil.toBengaliDigits(minutes.toInt())} মিনিট",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SomadhanOrange else SomadhanTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Message / Note
            Text(
                text = "গ্রাহকের জন্য বার্তা / নোট (ঐচ্ছিক)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SomadhanTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                maxLines = 2,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SomadhanOrange,
                    unfocusedBorderColor = SomadhanBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bid_message_input")
            )

            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    fontSize = 12.sp,
                    color = SomadhanError
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Submit Bid Button
            Button(
                onClick = {
                    val amount = bidAmountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "অনুগ্রহ করে সঠিক মূল্য লিখুন"
                        return@Button
                    }
                    val estTimeStr = "${arrivalTimeMinutes} মিনিটে পৌঁছাব"
                    val fullMessage = if (noteText.isNotBlank()) {
                        "${estTimeStr} • ${noteText.trim()}"
                    } else {
                        estTimeStr
                    }

                    isSubmitting = true
                    errorMessage = null
                    viewModel.placeBid(
                        problem = problem,
                        amount = amount,
                        message = fullMessage,
                        estimatedTime = "${arrivalTimeMinutes} মিনিট",
                        onSuccess = {
                            isSubmitting = false
                            onBidSuccess()
                        },
                        onError = { err ->
                            isSubmitting = false
                            errorMessage = err
                        }
                    )
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SomadhanOrange,
                    disabledContainerColor = SomadhanOrange.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_instant_bid_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "বিড জমা হচ্ছে...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "বিড নিশ্চিত করুন ⚡",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PulsingLiveDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(Color(0xFFEF4444).copy(alpha = alpha))
    )
}
