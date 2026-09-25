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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BottomSlideDialog
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.SyncAwareRefreshableContent
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.ActivePostWithActivity
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.Formatters

/**
 * Shared Active Jobs Popup Dialog
 */
@Composable
fun ActiveJobsPopup(
    role: String,
    viewModel: SomadhanViewModel,
    onDismiss: () -> Unit,
    onProblemClick: (String) -> Unit
) {
    val isSolver = role.equals("SOLVER", ignoreCase = true)
    val activePosts by (if (isSolver) viewModel.solverActiveWinningPosts else viewModel.userActiveAcceptedPosts)
        .collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    // Loading/Sync Fix Roadmap v2, ধাপ ৪ (Group B) — activePosts problems/bids টেবিল থেকে
    // আসে, যা bulk-pull-এর অংশ, তাই initialSyncPhase প্রাসঙ্গিক। sessionKey isSolver-ভিত্তিক
    // আলাদা রাখা হলো কারণ একই composable solver/user দুই রোলের জন্যই ব্যবহৃত হয়।
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    BottomSlideDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, SomadhanBorder, RoundedCornerShape(20.dp))
                .testTag(if (isSolver) "solver_active_jobs_popup" else "user_active_jobs_popup"),
            color = SomadhanBg,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSolver) SomadhanOrangeLight else Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = "সক্রিয় কাজ",
                                tint = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "সক্রিয় কাজ",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSolver) SomadhanOrange.copy(alpha = 0.12f) else Color(0xFF1D4ED8).copy(alpha = 0.12f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = DistanceUtil.toBengaliDigits(activePosts.size.toString()),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8)
                                    )
                                }
                            }
                            Text(
                                text = if (isSolver) "আপনার বর্তমান উইনিং ও চলমান কাজগুলো" else "আপনার পোস্টকৃত চলমান ও গৃহীত কাজগুলো",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("active_jobs_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "বন্ধ করুন",
                            tint = SomadhanTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SomadhanDivider))
                Spacer(modifier = Modifier.height(12.dp))

                // List of Active Jobs
                // Realtime-Aware, Structure-Preserving Refresh, ধাপ ৩ — এই একটাই স্ক্রিন (দুই
                // সুবিধায় ব্যবহৃত: solver/user popup) roadmap-এর প্রথম পাইলট হিসেবে বেছে নেওয়া
                // হয়েছে (কোনো pagination/filter নেই, একটাই কল-সাইট, `ActivePostWithActivity`
                // data class হওয়ায় `==` অর্থপূর্ণ)। পুরনো `SyncAwareContent`-এর বদলে
                // `SyncAwareRefreshableContent` ব্যবহার করে `activePosts`-কে `data` হিসেবে পাস
                // করা হচ্ছে — dialog-এর হেডার/ক্লোজ-বাটন (এই কম্পোনেন্টের বাইরে) অপরিবর্তিত থাকবে,
                // popup খোলা অবস্থায় realtime-এ কোনো active job যোগ/বাদ/আপডেট হলে শুধু নিচের
                // তালিকা-অংশটুকু সংক্ষিপ্ত সময়ের জন্য shimmer করবে। তালিকা সত্যিই অপরিবর্তিত থাকলে
                // কোনো flash হবে না। প্রথম-ভিজিট cold-load আচরণ (পুরো এই ব্লক প্রথমবার skeleton)
                // আগের মতোই অক্ষত, কারণ এই sessionKey-গুলো একই থাকছে।
                SyncAwareRefreshableContent(
                    sessionKey = if (isSolver) "solver_active_jobs_popup_sync" else "user_active_jobs_popup_sync",
                    viewModel = viewModel,
                    syncPhase = initialSyncPhase,
                    data = activePosts,
                    onRetry = { viewModel.retryInitialSync() }
                ) { posts ->
                    if (posts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = SomadhanTextHint,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "বর্তমানে কোনো সক্রিয় কাজ নেই",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 480.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(posts, key = { it.problem.id }) { item ->
                                val otherPartyName = remember(item.problem, allUsers, isSolver) {
                                    if (isSolver) {
                                        item.problem.userName.ifBlank {
                                            allUsers.find { it.id == item.problem.userId }?.name ?: "গ্রাহক"
                                        }
                                    } else {
                                        item.problem.acceptedSolverName ?: allUsers.find { it.id == item.problem.acceptedSolverId }?.name ?: "সমাধানকারী"
                                    }
                                }

                                ActiveJobCard(
                                    item = item,
                                    otherPartyName = otherPartyName,
                                    isSolver = isSolver,
                                    onClick = {
                                        onDismiss()
                                        onProblemClick(item.problem.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Active Job Card with unseen activity red dot
 */
@Composable
private fun ActiveJobCard(
    item: ActivePostWithActivity,
    otherPartyName: String,
    isSolver: Boolean,
    onClick: () -> Unit
) {
    val problem = item.problem
    val (catBg, catTint) = CategoryIconHelper.getCategoryColors(problem.categoryName)
    val budgetText = if (problem.acceptedAmount != null) {
        Formatters.formatTaka(problem.acceptedAmount)
    } else {
        Formatters.formatTakaRange(problem.minBudget, problem.maxBudget)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (item.hasUnseenActivity) Color(0xFFEF4444).copy(alpha = 0.4f) else SomadhanBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag("active_job_card_${problem.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Category + Unseen Red Dot + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(catBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = problem.categoryName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = catTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (item.hasUnseenActivity) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("active_job_unseen_badge_${problem.id}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "নতুন আপডেট",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }

                // Status Badge
                val statusText = when (problem.status) {
                    "IN_PROGRESS" -> "চলমান"
                    "ACCEPTED" -> "গৃহীত"
                    "OPEN" -> "উন্মুক্ত"
                    else -> problem.status
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SomadhanSuccessLight)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Problem Title
            Text(
                text = problem.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Row: Budget, Other Party Name, and Navigation Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = budgetText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = SomadhanTextHint,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = otherPartyName,
                            fontSize = 11.sp,
                            color = SomadhanTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lastTime = problem.lastActivityAt ?: problem.createdAt
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = SomadhanTextHint,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = Formatters.formatTimeAgo(lastTime),
                            fontSize = 10.sp,
                            color = SomadhanTextHint
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "বিস্তারিত দেখুন",
                        tint = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Solver Active Jobs Popup Convenience Composable
 */
@Composable
fun SolverActiveJobsPopup(
    viewModel: SomadhanViewModel,
    onDismiss: () -> Unit,
    onProblemClick: (String) -> Unit
) {
    ActiveJobsPopup(
        role = "SOLVER",
        viewModel = viewModel,
        onDismiss = onDismiss,
        onProblemClick = onProblemClick
    )
}

/**
 * User Active Jobs Popup Convenience Composable
 */
@Composable
fun UserActiveJobsPopup(
    viewModel: SomadhanViewModel,
    onDismiss: () -> Unit,
    onProblemClick: (String) -> Unit
) {
    ActiveJobsPopup(
        role = "USER",
        viewModel = viewModel,
        onDismiss = onDismiss,
        onProblemClick = onProblemClick
    )
}
