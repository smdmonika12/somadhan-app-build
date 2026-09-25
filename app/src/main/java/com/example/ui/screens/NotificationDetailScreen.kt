package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.SyncAwareRefreshableContent
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
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    viewModel: SomadhanViewModel,
    notificationId: String,
    onNavigateBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val notification = notifications.find { it.id == notificationId }

    val isSolver = currentUser?.role == "SOLVER"
    val brandColor = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8)
    val brandLight = if (isSolver) SomadhanOrangeLight else Color(0xFFEFF6FF)
    val brandBorder = if (isSolver) SomadhanOrange.copy(alpha = 0.4f) else Color(0xFFBFDBFE)

    LaunchedEffect(notificationId) {
        if (notification != null && !notification.isRead) {
            viewModel.markNotificationRead(notificationId)
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "বিজ্ঞপ্তির বিস্তারিত",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SomadhanBg
                    ),
                    modifier = Modifier.border(1.dp, SomadhanDivider)
                )
                RealtimeLocationBar(
                    locationAddress = liveLocation.address,
                    isSolver = isSolver,
                    onRefresh = { viewModel.refreshLiveLocation() }
                )
            }
        },
        containerColor = SomadhanBg
    ) { innerPadding ->
        // Loading/Sync Fix Roadmap v2, ধাপ ৪ — notifications বাল্ক-পুলের অংশ, তাই initialSyncPhase।
        // Loading Pattern Master Prompt, ব্যাচ ৬ (B2 migration) — এখানে `data = notification`
        // পাস করা হলো (single nullable object, plain val থেকে derive করা — mutable reference
        // না, তাই `.toList()`-এর দরকার নেই)। এই স্ক্রিনে pull-to-refresh নেই, তাই
        // `isManualRefreshing` ডিফল্ট false-ই থাকল। lambda-এর প্যারামিটারের নাম ইচ্ছাকৃতভাবে
        // বাইরের `notification` val-কেই shadow করছে, যাতে ভেতরের সব `notification.xxx` ব্যবহার
        // অপরিবর্তিত থাকে।
        SyncAwareRefreshableContent(
            sessionKey = "notification_detail_sync",
            viewModel = viewModel,
            syncPhase = initialSyncPhase,
            data = notification,
            onRetry = { viewModel.retryInitialSync() },
            modifier = Modifier.fillMaxSize()
        ) { notification ->
        if (notification != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Badge / Sender info header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(brandLight)
                                    .border(1.dp, brandBorder, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = brandColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "অ্যাডমিন কর্তৃক প্রেরিত",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = brandColor
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = SomadhanTextHint,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = Formatters.formatTimeAgo(notification.timestamp),
                                    fontSize = 12.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Notification Title
                        Text(
                            text = notification.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary,
                            lineHeight = 25.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = SomadhanDivider, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Notification Message Content
                        Text(
                            text = notification.message,
                            fontSize = 15.sp,
                            color = SomadhanTextSecondary,
                            lineHeight = 23.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Full Date-Time footer
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SomadhanBg)
                                .border(0.8.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = SomadhanTextHint,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "প্রেরণের তারিখ: ${Formatters.formatDateBengali(notification.timestamp)}",
                                fontSize = 12.sp,
                                color = SomadhanTextHint
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SomadhanBg)
                                .border(0.8.dp, SomadhanDivider, RoundedCornerShape(6.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(notification.id))
                                    Toast.makeText(context, "বিজ্ঞপ্তি আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "নোটিফিকেশন আইডি: ${notification.id.take(12)}",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "কপি করুন",
                                tint = SomadhanTextHint,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Empty / Not found view
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
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
                            .background(brandLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = brandColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "বিজ্ঞপ্তি পাওয়া যায়নি",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                }
            }
        }
        } // SyncAwareRefreshableContent (ধাপ ৪ + ব্যাচ ৬ B2 migration) বন্ধ
    }
}
