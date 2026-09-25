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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ListScreenSkeleton
import com.example.ui.components.LoadingAwareContent
import com.example.ui.components.NotificationBottomSheet
import com.example.ui.components.handleSomadhanNotification
import com.example.ui.components.SomadhanBottomNav
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.SomadhanTopBar
import com.example.ui.components.SyncAwareContent
import com.example.ui.navigation.Screen
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.Formatters

@Composable
fun MessagesScreen(
    viewModel: SomadhanViewModel,
    onNavigate: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onAdminClick: () -> Unit,
    onProblemClick: ((String) -> Unit)? = null
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val conversations by viewModel.userConversations.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    // Loading/Sync Fix Roadmap v2, ধাপ ৪ — messages টেবিল initialSyncPhase-এর (bulk-pull)
    // অংশ, তাই এখানে সেই phase ব্যবহার হচ্ছে (sessionKey "messages_sync")।
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()


    var showNotificationsSheet by remember { mutableStateOf(false) }

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
                    onProblemClick = onProblemClick,
                    onChatClick = onOpenChat,
                    isSolver = (currentUser?.role == "SOLVER")
                )
            },
            isSolver = (currentUser?.role == "SOLVER")
        )
    }

    Scaffold(
        topBar = {
            SomadhanTopBar(
                title = "বার্তা ও চ্যাট",
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
                onLocationRefresh = { viewModel.refreshLiveLocation() }
            )
        },
        bottomBar = {
            val platformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
            val isWalletEnabled = platformSettings.find { it.key == "menu_wallet_enabled" }?.value != "false"
            val isInstantJobEnabled = platformSettings.find { it.key == "instant_job_feature_enabled" }?.value?.toBooleanStrictOrNull() ?: true
            val unreadChatCount by viewModel.unreadMessagesCount.collectAsStateWithLifecycle()
            SomadhanBottomNav(
                currentRoute = "messages",
                isSolver = currentUser?.role == "SOLVER",
                isWalletEnabled = isWalletEnabled,
                isInstantJobEnabled = isInstantJobEnabled,
                unreadChatCount = unreadChatCount,
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
                .background(SomadhanBg)
        ) {
            // Loading/Sync Fix Roadmap v2, ধাপ ৪ — SyncAwareContent বাইরে (network-level
            // sync error/retry), SessionAwareLoadingContent ভেতরে (local Room-এর
            // data-ready-কিনা skeleton, অপরিবর্তিত) -- দুটো ভিন্ন উদ্বেগ, একে অপরকে
            // প্রতিস্থাপন করে না।
            // MessagesScreen-specific ব্যতিক্রম (ব্যবহারকারীর সিদ্ধান্ত) — ধাপ ৭-এ এখানে
            // SyncAwareRefreshableContent ব্যবহার করা হয়েছিল, কিন্তু এই স্ক্রিনে সেটা ফেরত
            // SyncAwareContent-এ আনা হলো: এই স্ক্রিনে কোনো realtime-diff-flash/re-entry-flash
            // চাওয়া হচ্ছে না — শুধু app session-এ প্রথমবার ঢোকার সময় একবার cold-load skeleton
            // দেখাবে, তারপর realtime-এ conversations যতবারই বদলাক/re-entry হোক, আর কখনো
            // skeleton/shimmer দেখাবে না। এই ব্যতিক্রম শুধু MessagesScreen-এর জন্য, অন্য কোনো
            // স্ক্রিনের SyncAwareRefreshableContent ব্যবহার এতে বদলায়নি।
            SyncAwareContent(
                sessionKey = "messages_sync",
                viewModel = viewModel,
                syncPhase = initialSyncPhase,
                onRetry = { viewModel.retryInitialSync() },
                modifier = Modifier.fillMaxSize()
            ) {
            val threads = conversations.groupBy { it.problemId }
            if (threads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = SomadhanTextHint,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "কোনো চ্যাট বা বার্তা নেই",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "বিড গ্রহণের পর এখানে সরাসরি চ্যাট শুরু হবে।",
                            fontSize = 13.sp,
                            color = SomadhanTextHint
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Text(
                            text = "কথোপকথনের তালিকা",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    items(threads.keys.toList()) { problemId ->
                        val msgs = threads[problemId] ?: emptyList()
                        val latestMsg = msgs.maxByOrNull { it.timestamp }
                        val relatedProblem = allProblems.find { it.id == problemId }
                        val unreadInThread = msgs.count { !it.isRead && it.receiverId == currentUser?.id }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .border(
                                    1.dp,
                                    if (unreadInThread > 0) SomadhanOrange.copy(alpha = 0.5f) else SomadhanDivider,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onOpenChat(problemId) }
                                .testTag("chat_thread_$problemId")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (relatedProblem?.isDirectContract == true) Color(0xFFEFF6FF) else SomadhanOrangeLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (relatedProblem?.isDirectContract == true) Color(0xFF1D4ED8) else SomadhanOrange,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = relatedProblem?.title ?: "সমস্যার সমাধান চ্যাট",
                                                fontSize = 14.sp,
                                                fontWeight = if (unreadInThread > 0) FontWeight.Bold else FontWeight.SemiBold,
                                                color = SomadhanTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (relatedProblem?.isDirectContract == true) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFDBEAFE))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = "সরাসরি চুক্তি",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1E40AF)
                                                    )
                                                }
                                            }
                                        }
                                        if (latestMsg != null) {
                                            Text(
                                                text = Formatters.formatTimeAgo(latestMsg.timestamp),
                                                fontSize = 10.sp,
                                                color = if (unreadInThread > 0) SomadhanOrange else SomadhanTextHint,
                                                fontWeight = if (unreadInThread > 0) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val displaySnippet = when {
                                            latestMsg == null -> "কথোপকথন শুরু করুন"
                                            latestMsg.isDirectContractProposal -> "🎯 প্রজেক্ট প্রস্তাব: ${latestMsg.content}"
                                            !latestMsg.fileUrl.isNullOrBlank() -> {
                                                val prefix = if (latestMsg.fileType?.startsWith("image") == true) "📷 ছবি" else "📎 ফাইল"
                                                "${latestMsg.senderName}: $prefix (${latestMsg.fileName ?: ""})"
                                            }
                                            else -> "${latestMsg.senderName}: ${latestMsg.content}"
                                        }

                                        Text(
                                            text = displaySnippet,
                                            fontSize = 12.sp,
                                            color = if (unreadInThread > 0) SomadhanTextPrimary else SomadhanTextSecondary,
                                            fontWeight = if (unreadInThread > 0) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (unreadInThread > 0) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(SomadhanOrange),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (unreadInThread > 9) "9+" else unreadInThread.toString(),
                                                    color = androidx.compose.ui.graphics.Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
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
