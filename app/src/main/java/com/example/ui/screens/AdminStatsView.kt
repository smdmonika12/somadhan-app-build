@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.AdditionalChargeEntity
import com.example.data.entity.AdminAuditLogEntity
import com.example.data.entity.BidEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.EscrowEntity
import com.example.data.entity.FaqEntity
import com.example.data.entity.MessageEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.RatingEntity
import com.example.data.entity.ReputationEventEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.WithdrawalEntity
import com.example.data.repository.AdminDashboardMetrics
import com.example.ui.components.PulsingValue
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.ReputationBadge
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.components.ScrollRevealShimmer
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.navigation.Screen
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.theme.SomadhanYellowVerified
import com.example.ui.theme.SomadhanYellowVerifiedBg
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.CsvExportUtil
import com.example.util.DistanceUtil
import com.example.util.FileAttachmentUtil
import com.example.util.Formatters
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.text.SimpleDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun AdminStatsView(
    metrics: AdminDashboardMetrics,
    commissionPercent: Double = 10.0,
    heldEscrows: List<EscrowEntity> = emptyList(),
    additionalCharges: List<AdditionalChargeEntity> = emptyList(),
    allTransactions: List<TransactionEntity> = emptyList(),
    allProblems: List<ProblemEntity> = emptyList(),
    allUsers: List<UserEntity> = emptyList(),
    isRefreshing: Boolean,
    onSyncClick: () -> Unit,
    onOpenCloudConfig: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val context = LocalContext.current
    val listState = rememberLazyListState()
    // Loading Pattern Master Prompt, batch 15 — প্রতিটা independent shimmer-zone প্রথমবার
    // scroll করে viewport-এ দেখা দিলে (এই AdminStatsView composition instance-এর মধ্যেই) সেই
    // zoneKey এখানে যোগ হয় — একবার যোগ হলে আর দ্বিতীয়বার শিমার-reveal হয় না (উপরে-নিচে
    // আবার scroll করলেও), কিন্তু ট্যাব থেকে বেরিয়ে আবার ঢুকলে (নতুন composition instance)
    // ফাঁকা সেট দিয়েই আবার শুরু হয় — এটাই "শুধু প্রথমবার (per-visit)" রুল।
    val revealedZoneKeys = remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
        // --- 1. Real-time Supabase Sync Status Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = if (metrics.isConnected) Color(0xFF0F2418) else Color(0xFF261D15)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (metrics.isConnected) SomadhanSuccess.copy(alpha = 0.4f) else SomadhanOrange.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .alpha(if (metrics.isConnected) pulseAlpha else 1f)
                            .background(
                                color = if (metrics.isConnected) SomadhanSuccess else SomadhanOrange,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "সিঙ্ক স্ট্যাটাস: ",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = if (metrics.isConnected) "লাইভ সংযুক্ত (Supabase Live)" else "লোকাল সিঙ্ক মোড",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (metrics.isConnected) SomadhanSuccess else SomadhanOrange
                            )
                        }
                        Text(
                            text = metrics.syncStatusMessage,
                            fontSize = 11.sp,
                            color = SomadhanTextHint
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onOpenCloudConfig,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanTextSecondary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("admin_open_cloud_config_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(12.dp), tint = SomadhanOrange)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("কনফিগ", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SomadhanTextPrimary)
                    }

                    OutlinedButton(
                        onClick = onSyncClick,
                        enabled = !isRefreshing,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanOrange),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("admin_sync_now_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = SomadhanOrange, strokeWidth = 1.5.dp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(12.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("সিঙ্ক", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        }
        item {
        // --- TOP HERO CARD: মোট সম্পন্ন লেনদেন (Total Completed Transactions Volume) ---
        val completedProblemsList = remember(allProblems) {
            allProblems.filter { it.status == "COMPLETED" }
        }
        val completedCount = completedProblemsList.size
        val totalCompletedGrossAmount = remember(completedProblemsList, allTransactions) {
            if (allTransactions.isNotEmpty()) {
                val compProblemIds = completedProblemsList.map { it.id }.toSet()
                val matchingTrxSum = allTransactions.filter { compProblemIds.contains(it.problemId) }.sumOf { it.grossAmount }
                if (matchingTrxSum > 0) matchingTrxSum
                else completedProblemsList.sumOf { it.acceptedAmount ?: it.minBudget }
            } else {
                completedProblemsList.sumOf { it.acceptedAmount ?: it.minBudget }
            }
        }
        val completedCommissionAmount = remember(completedProblemsList, allTransactions, commissionPercent) {
            if (allTransactions.isNotEmpty()) {
                val compProblemIds = completedProblemsList.map { it.id }.toSet()
                val matchingTrxs = allTransactions.filter { compProblemIds.contains(it.problemId) }
                if (matchingTrxs.isNotEmpty()) {
                    val matchingProblemIds = matchingTrxs.map { it.problemId }.toSet()
                    val trxCommissionSum = matchingTrxs.sumOf { it.grossAmount * (it.commissionPercent / 100.0) }
                    val unmatchedProblems = completedProblemsList.filter { !matchingProblemIds.contains(it.id) }
                    val unmatchedCommissionSum = unmatchedProblems.sumOf { (it.acceptedAmount ?: it.minBudget) * (commissionPercent / 100.0) }
                    trxCommissionSum + unmatchedCommissionSum
                } else {
                    completedProblemsList.sumOf { (it.acceptedAmount ?: it.minBudget) * (commissionPercent / 100.0) }
                }
            } else {
                totalCompletedGrossAmount * (commissionPercent / 100.0)
            }
        }
        val netSolverCompletedPayout = totalCompletedGrossAmount - completedCommissionAmount

        // Loading Pattern Master Prompt, ব্যাচ ১১ (Overview ট্যাব, independent shimmer-zone
        // পাইলট #১) — এই হিরো কার্ডের ৪টা derived value (completedCount/
        // totalCompletedGrossAmount/completedCommissionAmount/netSolverCompletedPayout) একসাথে
        // একটা composite key হিসেবে rememberFieldChangePulse-এ পাঠানো হলো, ঠিক পুরনো
        // UserWalletScreen-এর pre-batch-৭ rememberFieldChangePulse প্যাটার্নের মতোই। এই ৪টার
        // যেকোনোটা realtime-এ বদলালে (বা isManualRefreshing সত্যি থেকে মিথ্যায় ফিরলে, মানে
        // pull-to-refresh সবেমাত্র শেষ হলো) শুধু এই কার্ডটাই সংক্ষিপ্ত পালস শিমার করবে — বাকি
        // Overview ট্যাবের কাঠামো/অন্য কার্ড/চার্ট সম্পূর্ণ অপরিবর্তিত/স্থির থাকবে। sessionKey/
        // viewModel ইচ্ছাকৃতভাবে দেওয়া হয়নি (তাই re-entry mount pulse-টা নিষ্ক্রিয়) — শুধু
        // প্রকৃত value-পরিবর্তন আর ম্যানুয়াল-রিফ্রেশ-সমাপ্তি এই দুটো কারণেই পালস হবে, যা এই
        // ট্যাবের target design অনুযায়ী যথেষ্ট (rule ১-এর পূর্ণ cold-load skeleton আগে থেকেই
        // বাইরের `SyncAwareContent`("admin_overview_sync") দিয়ে কভার করা আছে, এখানে দ্বিতীয়বার
        // দরকার নেই)। বাকি কার্ড/চার্টগুলোর নিজস্ব independent zone পরের ব্যাচে যোগ হবে।
        val heroCardPulsing = rememberFieldChangePulse(
            value = listOf(completedCount, totalCompletedGrossAmount, completedCommissionAmount, netSolverCompletedPayout),
            isManualRefreshing = isRefreshing
        )

        ScrollRevealShimmer(zoneKey = "admin_overview_hero_card", revealedKeys = revealedZoneKeys) {
        PulsingValue(isUpdating = heroCardPulsing) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, SomadhanOrange, RoundedCornerShape(14.dp))
                .testTag("admin_completed_transactions_hero_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SomadhanOrange.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "মোট সম্পন্ন কাজের লেনদেন",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = "অ্যাপের মাধ্যমে সফলভাবে সম্পন্ন হওয়া কাজের মোট হিসাব",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SomadhanSuccess.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${DistanceUtil.toBengaliDigits(completedCount.toString())}টি সম্পন্ন কাজ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanSuccess
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Total Amount Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    SomadhanOrange.copy(alpha = 0.08f),
                                    Color(0xFF10B981).copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(1.dp, SomadhanOrange.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "সর্বমোট লেনদেন (কমিশন সহ মোট অংক)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SomadhanTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(totalCompletedGrossAmount.toInt().toString())}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SomadhanTextPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SomadhanSuccess,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detailed Split: Commission + Net Solver Payout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SomadhanBg)
                            .border(0.5.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "প্ল্যাটফর্ম কমিশন (${DistanceUtil.toBengaliDigits(commissionPercent.toInt().toString())}%)",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(completedCommissionAmount.toInt().toString())}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrange
                            )
                            Text(
                                text = "অ্যাডমিন রাজস্ব",
                                fontSize = 9.5.sp,
                                color = SomadhanTextHint
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SomadhanBg)
                            .border(0.5.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "সমাধানকারী মোট প্রাপ্য",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(netSolverCompletedPayout.toInt().toString())}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanSuccess
                            )
                            Text(
                                text = "নেট প্রদেয় আয়",
                                fontSize = 9.5.sp,
                                color = SomadhanTextHint
                            )
                        }
                    }
                }
            }
        }
        } // PulsingValue (হিরো কার্ড independent shimmer-zone) বন্ধ
        } // ScrollRevealShimmer (হিরো কার্ড scroll-reveal) বন্ধ

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "রিয়েল-টাইম মেট্রিক্স (Supabase Live)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = SomadhanTextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        }
        item {
        // --- Row 1: Total Bids & Platform Revenue ---
        // ব্যাচ ১২ — independent shimmer-zone পাইলট #২: এই Row-টার ৫টা মেট্রিক (totalBids/
        // acceptedBids/pendingBids/platformRevenue/totalTransactionVolume) একসাথে composite
        // key, commissionPercent-ও যোগ হলো যেহেতু সাবটাইটেলে দেখানো হয়। ব্যাচ ১১-এর হিরো
        // কার্ডের মতোই sessionKey/viewModel ছাড়া (শুধু real value-change + manual-refresh-শেষ)।
        val row1Pulsing = rememberFieldChangePulse(
            value = listOf(metrics.totalBids, metrics.acceptedBids, metrics.pendingBids, metrics.platformRevenue, metrics.totalTransactionVolume, commissionPercent),
            isManualRefreshing = isRefreshing
        )
        ScrollRevealShimmer(zoneKey = "admin_overview_row1", revealedKeys = revealedZoneKeys) {
        PulsingValue(isUpdating = row1Pulsing) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "মোট বিড (Total Bids)",
                value = DistanceUtil.toBengaliDigits(metrics.totalBids.toString()),
                subtitle = "${DistanceUtil.toBengaliDigits(metrics.acceptedBids.toString())} গৃহীত | ${DistanceUtil.toBengaliDigits(metrics.pendingBids.toString())} অপেক্ষমাণ",
                icon = Icons.Default.Shield,
                iconColor = SomadhanOrange,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "প্ল্যাটফর্ম রাজস্ব (Revenue)",
                value = Formatters.formatTaka(metrics.platformRevenue),
                subtitle = "মোট ৳ ${DistanceUtil.toBengaliDigits(metrics.totalTransactionVolume.toInt().toString())} (বর্তমান ফি: ${DistanceUtil.toBengaliDigits(commissionPercent.toInt().toString())}%)",
                icon = Icons.Default.AccountBalanceWallet,
                iconColor = SomadhanSuccess,
                modifier = Modifier.weight(1f)
            )
        }
        } // PulsingValue (Row ১: মোট বিড + প্ল্যাটফর্ম রাজস্ব) বন্ধ
        } // ScrollRevealShimmer (Row ১ scroll-reveal) বন্ধ

        Spacer(modifier = Modifier.height(10.dp))

        }
        item {
        // --- Escrow & Additional Charge Metrics Card ---
        val totalHeldEscrowAmount = heldEscrows.sumOf { it.baseAmount + it.extraAmount }
        val acceptedCharges = additionalCharges.filter { it.status == "ACCEPTED" }
        val acceptedChargesAmount = acceptedCharges.sumOf { it.amount }
        val rejectedChargesCount = additionalCharges.count { it.status == "REJECTED" }
        val pendingChargesCount = additionalCharges.count { it.status == "PENDING" }

        // ব্যাচ ১২ — independent shimmer-zone পাইলট #৩: Escrow/অতিরিক্ত-চার্জ কার্ডের ৫টা মান।
        val escrowChargesPulsing = rememberFieldChangePulse(
            value = listOf(totalHeldEscrowAmount, heldEscrows.size, additionalCharges.size, acceptedChargesAmount, rejectedChargesCount),
            isManualRefreshing = isRefreshing
        )
        ScrollRevealShimmer(zoneKey = "admin_overview_escrow_charges", revealedKeys = revealedZoneKeys) {
        PulsingValue(isUpdating = escrowChargesPulsing) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SomadhanOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Escrow ও অতিরিক্ত চার্জের মেট্রিক্স",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }
                    StatusBadge(status = "HELD")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Escrow Held summary box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SomadhanBg)
                            .border(0.5.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "মোট Escrow-এ HELD",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(totalHeldEscrowAmount.toInt().toString())}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanSuccess
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${DistanceUtil.toBengaliDigits(heldEscrows.size.toString())}টি চলমান কাজে সুরক্ষিত",
                                fontSize = 10.sp,
                                color = SomadhanTextHint
                            )
                        }
                    }

                    // Additional Charges summary box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SomadhanBg)
                            .border(0.5.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "অতিরিক্ত চার্জ আবেদন",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "মোট ${DistanceUtil.toBengaliDigits(additionalCharges.size.toString())}টি",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrange
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "গৃহীত: ${DistanceUtil.toBengaliDigits(acceptedCharges.size.toString())}টি (৳ ${DistanceUtil.toBengaliDigits(acceptedChargesAmount.toInt().toString())}) | প্রত্যাখ্যাত: ${DistanceUtil.toBengaliDigits(rejectedChargesCount.toString())}টি",
                                fontSize = 10.sp,
                                color = SomadhanTextHint,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }
        } // PulsingValue (Escrow ও অতিরিক্ত চার্জের মেট্রিক্স কার্ড) বন্ধ
        } // ScrollRevealShimmer (Escrow ও অতিরিক্ত চার্জ scroll-reveal) বন্ধ

        Spacer(modifier = Modifier.height(10.dp))

        }
        item {
        // --- Row 2: Total Problems & Total Users ---
        // ব্যাচ ১২ — independent shimmer-zone পাইলট #৪।
        val row2Pulsing = rememberFieldChangePulse(
            value = listOf(metrics.totalProblems, metrics.completedProblems, metrics.openProblems, metrics.totalUsers, metrics.totalSolvers, metrics.totalClients),
            isManualRefreshing = isRefreshing
        )
        ScrollRevealShimmer(zoneKey = "admin_overview_row2", revealedKeys = revealedZoneKeys) {
        PulsingValue(isUpdating = row2Pulsing) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "মোট সমস্যা পোস্ট",
                value = DistanceUtil.toBengaliDigits(metrics.totalProblems.toString()),
                subtitle = "${DistanceUtil.toBengaliDigits(metrics.completedProblems.toString())} সম্পন্ন | ${DistanceUtil.toBengaliDigits(metrics.openProblems.toString())} উন্মুক্ত",
                icon = Icons.Default.PostAdd,
                iconColor = SomadhanInfo,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "মোট ইউজার ও সলভার",
                value = DistanceUtil.toBengaliDigits(metrics.totalUsers.toString()),
                subtitle = "${DistanceUtil.toBengaliDigits(metrics.totalSolvers.toString())} সলভার | ${DistanceUtil.toBengaliDigits(metrics.totalClients.toString())} ক্লায়েন্ট",
                icon = Icons.Default.People,
                iconColor = SomadhanOrange,
                modifier = Modifier.weight(1f)
            )
        }
        } // PulsingValue (Row ২: মোট সমস্যা + মোট ইউজার) বন্ধ
        } // ScrollRevealShimmer (Row ২ scroll-reveal) বন্ধ

        Spacer(modifier = Modifier.height(10.dp))

        }
        item {
        // --- Row 3: Pending Withdrawals & Completed Payouts ---
        // ব্যাচ ১২ — independent shimmer-zone পাইলট #৫।
        val row3Pulsing = rememberFieldChangePulse(
            value = listOf(metrics.pendingWithdrawals, metrics.completedWithdrawals),
            isManualRefreshing = isRefreshing
        )
        ScrollRevealShimmer(zoneKey = "admin_overview_row3", revealedKeys = revealedZoneKeys) {
        PulsingValue(isUpdating = row3Pulsing) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "পেন্ডিং উইথড্রয়াল",
                value = DistanceUtil.toBengaliDigits(metrics.pendingWithdrawals.toString()),
                subtitle = "অনুমোদন অপেক্ষমাণ",
                icon = Icons.Default.AccountBalanceWallet,
                iconColor = SomadhanError,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "পরিশোধিত পে-আউট",
                value = Formatters.formatTaka(metrics.completedWithdrawals),
                subtitle = "সলভারদের পরিশোধ",
                icon = Icons.Default.CheckCircle,
                iconColor = SomadhanSuccess,
                modifier = Modifier.weight(1f)
            )
        }
        } // PulsingValue (Row ৩: পেন্ডিং উইথড্রয়াল + পরিশোধিত পে-আউট) বন্ধ
        } // ScrollRevealShimmer (Row ৩ scroll-reveal) বন্ধ

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "ডায়নামিক অ্যানালিটিক্স ও চার্ট",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = SomadhanTextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        }
        item {
        // --- Dynamic Chart 1: Category Problem & Bid Distribution Bar Chart ---
        // ব্যাচ ১৩ — independent shimmer-zone পাইলট #৬: এই চার্টের ২টা Map (categoryProblemCounts/
        // categoryBidCounts) সরাসরি key হিসেবে ব্যবহার করা হলো (Kotlin Map-এর structural `==`
        // ইতিমধ্যে content-ভিত্তিক তুলনা করে, তাই এই দুটো বদলালেই যথেষ্ট)।
        val chart1Pulsing = rememberFieldChangePulse(
            value = listOf(metrics.categoryProblemCounts, metrics.categoryBidCounts),
            isManualRefreshing = isRefreshing
        )
        ScrollRevealShimmer(zoneKey = "admin_overview_chart1", revealedKeys = revealedZoneKeys) {
        PulsingValue(isUpdating = chart1Pulsing) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ক্যাটাগরি অনুযায়ী সমস্যা ও বিড ডিস্ট্রিবিউশন",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chart Legend
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(SomadhanOrange, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পোস্টকৃত সমস্যা", fontSize = 11.sp, color = SomadhanTextSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(SomadhanSuccess, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("মোট বিড", fontSize = 11.sp, color = SomadhanTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val categories = if (metrics.categoryProblemCounts.isNotEmpty()) {
                    metrics.categoryProblemCounts.keys.toList()
                } else {
                    listOf("হোম অ্যাপ্লায়েন্স", "ইলেকট্রনিক্স", "প্লাম্বিং", "এয়ার কন্ডিশনার", "আইটি সাপোর্ট")
                }

                val maxVal = maxOf(
                    1,
                    metrics.categoryProblemCounts.values.maxOrNull() ?: 1,
                    metrics.categoryBidCounts.values.maxOrNull() ?: 1
                )

                categories.forEach { categoryName ->
                    val probCount = metrics.categoryProblemCounts[categoryName] ?: 0
                    val bidCount = metrics.categoryBidCounts[categoryName] ?: 0
                    val probProgress = (probCount.toFloat() / maxVal.toFloat()).coerceIn(0.05f, 1f)
                    val bidProgress = (bidCount.toFloat() / maxVal.toFloat()).coerceIn(0.05f, 1f)

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(categoryName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SomadhanTextPrimary)
                            Text(
                                "${DistanceUtil.toBengaliDigits(probCount.toString())} পোস্ট • ${DistanceUtil.toBengaliDigits(bidCount.toString())} বিড",
                                fontSize = 11.sp,
                                color = SomadhanTextHint
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Problem Bar
                        LinearProgressIndicator(
                            progress = { if (probCount == 0) 0f else probProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = SomadhanOrange,
                            trackColor = SomadhanOrange.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        // Bid Bar
                        LinearProgressIndicator(
                            progress = { if (bidCount == 0) 0f else bidProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = SomadhanSuccess,
                            trackColor = SomadhanSuccess.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
        } // PulsingValue (চার্ট ১: ক্যাটাগরি ডিস্ট্রিবিউশন) বন্ধ
        } // ScrollRevealShimmer (চার্ট ১ scroll-reveal) বন্ধ

        Spacer(modifier = Modifier.height(14.dp))

        }
        item {
        // --- Dynamic Chart 2: Problem Lifecycle Status Breakdown ---
        // ব্যাচ ১৩ — independent shimmer-zone পাইলট #৭।
        val chart2Pulsing = rememberFieldChangePulse(
            value = listOf(metrics.openProblems, metrics.inProgressProblems, metrics.completedProblems, metrics.totalProblems),
            isManualRefreshing = isRefreshing
        )
        ScrollRevealShimmer(zoneKey = "admin_overview_chart2", revealedKeys = revealedZoneKeys) {
        PulsingValue(isUpdating = chart2Pulsing) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = SomadhanInfo, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "সমস্যা সমাধান লাইফসাইকেল অনুপাত",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val total = maxOf(1, metrics.totalProblems)
                val openPercent = (metrics.openProblems * 100) / total
                val inProgressPercent = (metrics.inProgressProblems * 100) / total
                val completedPercent = (metrics.completedProblems * 100) / total

                // Segmented Proportional Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(SomadhanDivider)
                ) {
                    if (openPercent > 0) {
                        Box(
                            modifier = Modifier
                                .weight(openPercent.toFloat().coerceAtLeast(1f))
                                .fillMaxSize()
                                .background(SomadhanInfo)
                        )
                    }
                    if (inProgressPercent > 0) {
                        Box(
                            modifier = Modifier
                                .weight(inProgressPercent.toFloat().coerceAtLeast(1f))
                                .fillMaxSize()
                                .background(SomadhanOrange)
                        )
                    }
                    if (completedPercent > 0) {
                        Box(
                            modifier = Modifier
                                .weight(completedPercent.toFloat().coerceAtLeast(1f))
                                .fillMaxSize()
                                .background(SomadhanSuccess)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusLegendItem(
                        label = "উন্মুক্ত (Open)",
                        count = metrics.openProblems,
                        percent = openPercent,
                        color = SomadhanInfo
                    )
                    StatusLegendItem(
                        label = "চলমান (In Progress)",
                        count = metrics.inProgressProblems,
                        percent = inProgressPercent,
                        color = SomadhanOrange
                    )
                    StatusLegendItem(
                        label = "সম্পন্ন (Completed)",
                        count = metrics.completedProblems,
                        percent = completedPercent,
                        color = SomadhanSuccess
                    )
                }
            }
        }
        } // PulsingValue (চার্ট ২: সমস্যা লাইফসাইকেল) বন্ধ
        } // ScrollRevealShimmer (চার্ট ২ scroll-reveal) বন্ধ

        Spacer(modifier = Modifier.height(14.dp))

        }
        item {
        // --- Dynamic Chart 3: Financial Revenue & Volume Breakdown ---
        // ব্যাচ ১৩ — independent shimmer-zone পাইলট #৮।
        val chart3Pulsing = rememberFieldChangePulse(
            value = listOf(metrics.totalTransactionVolume, metrics.platformRevenue, commissionPercent),
            isManualRefreshing = isRefreshing
        )
        ScrollRevealShimmer(zoneKey = "admin_overview_chart3", revealedKeys = revealedZoneKeys) {
        PulsingValue(isUpdating = chart3Pulsing) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SomadhanSuccess, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "আর্থিক প্রবাহ ও প্ল্যাটফর্ম রাজস্ব বিশ্লেষণ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val grossVolume = metrics.totalTransactionVolume
                val platformRevenue = metrics.platformRevenue
                val solverNet = grossVolume - platformRevenue

                val commPct = commissionPercent
                val solverPct = (100.0 - commPct).coerceAtLeast(0.0)
                val commPctStr = DistanceUtil.toBengaliDigits(commPct.toInt().toString())
                val solverPctStr = DistanceUtil.toBengaliDigits(solverPct.toInt().toString())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("মোট সম্পন্ন চুক্তি", fontSize = 11.sp, color = SomadhanTextSecondary)
                        Text(Formatters.formatTaka(grossVolume), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("প্ল্যাটফর্ম আয় ($commPctStr%)", fontSize = 11.sp, color = SomadhanTextSecondary)
                        Text(Formatters.formatTaka(platformRevenue), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SomadhanSuccess)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("সলভার পে-আউট ($solverPctStr%)", fontSize = 11.sp, color = SomadhanTextSecondary)
                        Text(Formatters.formatTaka(solverNet), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SomadhanOrange)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { (commPct.toFloat() / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = SomadhanSuccess,
                    trackColor = SomadhanOrange
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🟩 প্ল্যাটফর্ম কমিশন $commPctStr%", fontSize = 10.sp, color = SomadhanSuccess)
                    Text("🟧 সলভারদের নেট আয় $solverPctStr%", fontSize = 10.sp, color = SomadhanOrange)
                }
            }
        }
        } // PulsingValue (চার্ট ৩: আর্থিক প্রবাহ) বন্ধ
        } // ScrollRevealShimmer (চার্ট ৩ scroll-reveal) বন্ধ

        Spacer(modifier = Modifier.height(14.dp))

        }
        item {
        // --- Dynamic Chart 4: User Demographics Ratio Visual ---
        // ব্যাচ ১৩ — independent shimmer-zone পাইলট #৯।
        val chart4Pulsing = rememberFieldChangePulse(
            value = listOf(metrics.totalUsers, metrics.totalSolvers, metrics.totalClients),
            isManualRefreshing = isRefreshing
        )
        ScrollRevealShimmer(zoneKey = "admin_overview_chart4", revealedKeys = revealedZoneKeys) {
        PulsingValue(isUpdating = chart4Pulsing) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Engineering, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ব্যবহারকারী ডেমোগ্রাফিক্স অনুপাত",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val totalUsers = maxOf(1, metrics.totalUsers)
                val solverPercent = (metrics.totalSolvers * 100) / totalUsers
                val clientPercent = (metrics.totalClients * 100) / totalUsers

                LinearProgressIndicator(
                    progress = { (metrics.totalSolvers.toFloat() / totalUsers.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = SomadhanOrange,
                    trackColor = SomadhanInfo
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(SomadhanOrange, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("দক্ষ সমাধানকারী: ${DistanceUtil.toBengaliDigits(metrics.totalSolvers.toString())} জন ($solverPercent%)", fontSize = 11.sp, color = SomadhanTextPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(SomadhanInfo, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("সাধারণ ক্লায়েন্ট: ${DistanceUtil.toBengaliDigits(metrics.totalClients.toString())} জন ($clientPercent%)", fontSize = 11.sp, color = SomadhanTextPrimary)
                    }
                }
            }
        }
        } // PulsingValue (চার্ট ৪: ইউজার ডেমোগ্রাফিক্স) বন্ধ
        } // ScrollRevealShimmer (চার্ট ৪ scroll-reveal) বন্ধ

        }
        item {
        // ==========================================
        // ১. রেভিনিউ ট্রেন্ড (গত ৭ দিনের প্ল্যাটফর্ম কমিশন)
        // ==========================================
        Spacer(modifier = Modifier.height(14.dp))

        data class DailyRevenue(
            val dayLabel: String,
            val dateLabel: String,
            val revenue: Double
        )

        val last7DaysRevenue = remember(allTransactions) {
            val list = mutableListOf<DailyRevenue>()
            val cal = Calendar.getInstance()
            val dayFormat = SimpleDateFormat("EEE", Locale("bn", "BD"))
            val dateFormat = SimpleDateFormat("dd/MM", Locale("bn", "BD"))

            for (i in 6 downTo 0) {
                cal.timeInMillis = System.currentTimeMillis()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val endOfDay = cal.timeInMillis

                val dayRev = allTransactions
                    .filter { it.timestamp in startOfDay..endOfDay }
                    .sumOf { it.commissionAmount }

                val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.SATURDAY -> "শনি"
                    Calendar.SUNDAY -> "রবি"
                    Calendar.MONDAY -> "সোম"
                    Calendar.TUESDAY -> "মঙ্গল"
                    Calendar.WEDNESDAY -> "বুধ"
                    Calendar.THURSDAY -> "বৃহ"
                    Calendar.FRIDAY -> "শুক্র"
                    else -> dayFormat.format(Date(startOfDay))
                }
                val dateStr = DistanceUtil.toBengaliDigits(dateFormat.format(Date(startOfDay)))
                list.add(DailyRevenue(dayName, dateStr, dayRev))
            }
            list
        }

        val total7DaysRevenue = last7DaysRevenue.sumOf { it.revenue }
        val maxDayRevenue = (last7DaysRevenue.maxOfOrNull { it.revenue } ?: 1.0).coerceAtLeast(1.0)

        // ব্যাচ ১৪ — independent shimmer-zone পাইলট #১০: `last7DaysRevenue` একটা data class-এর
        // List (DailyRevenue), তাই সরাসরি structural key হিসেবে ব্যবহার করা হলো।
        val revenueTrendPulsing = rememberFieldChangePulse(
            value = last7DaysRevenue,
            isManualRefreshing = isRefreshing
        )
        ScrollRevealShimmer(zoneKey = "admin_overview_revenue_trend", revealedKeys = revealedZoneKeys) {
        PulsingValue(isUpdating = revenueTrendPulsing) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SomadhanSuccess, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "রেভিনিউ ট্রেন্ড (গত ৭ দিন)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }
                    Text(
                        text = "মোট: ৳ ${DistanceUtil.toBengaliDigits(total7DaysRevenue.toInt().toString())}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanSuccess
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    last7DaysRevenue.forEach { day ->
                        val ratio = if (maxDayRevenue > 0) (day.revenue / maxDayRevenue).toFloat() else 0f
                        val barHeightFraction = ratio.coerceIn(0.06f, 1f)

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = if (day.revenue > 0) "৳${DistanceUtil.toBengaliDigits(day.revenue.toInt().toString())}" else "০",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (day.revenue > 0) SomadhanSuccess else SomadhanTextHint,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .fillMaxHeight(barHeightFraction * 0.72f)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (day.revenue > 0) {
                                            Brush.verticalGradient(
                                                listOf(SomadhanSuccess, SomadhanSuccess.copy(alpha = 0.6f))
                                            )
                                        } else {
                                            SolidColor(SomadhanDivider.copy(alpha = 0.5f))
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = day.dayLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = day.dateLabel,
                                fontSize = 9.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }
                }
            }
        }
        } // PulsingValue (রেভিনিউ ট্রেন্ড চার্ট) বন্ধ
        } // ScrollRevealShimmer (রেভিনিউ ট্রেন্ড scroll-reveal) বন্ধ

        }
        item {
        // ==========================================
        // ২. টপ ক্যাটাগরি কার্ড
        // ==========================================
        Spacer(modifier = Modifier.height(14.dp))
        val topCategories = remember(allProblems) {
            val total = allProblems.size.coerceAtLeast(1)
            allProblems
                .groupBy { it.categoryName.ifBlank { "অন্যান্য" } }
                .map { (catName, problems) ->
                    Triple(catName, problems.size, (problems.size * 100f) / total)
                }
                .sortedByDescending { it.second }
                .take(5)
        }

        // ব্যাচ ১৪ — independent shimmer-zone পাইলট #১১: `topCategories` (List<Triple<...>>)
        // সরাসরি structural key হিসেবে ব্যবহার করা হলো।
        val topCategoriesPulsing = rememberFieldChangePulse(
            value = topCategories,
            isManualRefreshing = isRefreshing
        )
        ScrollRevealShimmer(zoneKey = "admin_overview_top_categories", revealedKeys = revealedZoneKeys) {
        PulsingValue(isUpdating = topCategoriesPulsing) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "টপ ক্যাটাগরি (জনপ্রিয় সেবাসমূহ)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (topCategories.isEmpty()) {
                    Text(
                        text = "এখনও কোনো ক্যাটাগরির সমস্যা পোস্ট করা হয়নি।",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary
                    )
                } else {
                    val maxCategoryCount = (topCategories.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                    topCategories.forEachIndexed { index, (catName, count, percent) ->
                        val rankBn = DistanceUtil.toBengaliDigits((index + 1).toString())
                        val countBn = DistanceUtil.toBengaliDigits(count.toString())
                        val percentBn = DistanceUtil.toBengaliDigits(percent.toInt().toString())

                        Column(modifier = Modifier.padding(vertical = 5.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (index) {
                                                    0 -> SomadhanOrange
                                                    1 -> SomadhanOrange.copy(alpha = 0.8f)
                                                    2 -> SomadhanOrange.copy(alpha = 0.6f)
                                                    else -> SomadhanDivider
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = rankBn,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = catName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanTextPrimary,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$countBn টি সমস্যা ($percentBn%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (count.toFloat() / maxCategoryCount.toFloat()).coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SomadhanOrange,
                                trackColor = SomadhanDivider
                            )
                        }
                    }
                }
            }
        }
        } // PulsingValue (টপ ক্যাটাগরি কার্ড) বন্ধ
        } // ScrollRevealShimmer (টপ ক্যাটাগরি scroll-reveal) বন্ধ

        }
        item {
        // ==========================================
        // ৩. টপ সলভার কার্ড
        // ==========================================
        Spacer(modifier = Modifier.height(14.dp))
        val topSolvers = remember(allUsers, allProblems) {
            allUsers
                .filter { it.role.equals("SOLVER", ignoreCase = true) || it.hasSolverRole }
                .map { solver ->
                    val completedCount = allProblems.count {
                        it.acceptedSolverId == solver.id && it.status == "COMPLETED"
                    }
                    Pair(solver, completedCount)
                }
                .sortedWith(
                    // [BALANCE_REPUTATION_ROLE_SEPARATION ধাপ ৭খ, ROLE_SEPARATION_AUDIT.md] এই
                    // তালিকা ইতিমধ্যেই role="SOLVER"/hasSolverRole দিয়ে ফিল্টার করা — তাই
                    // অস্পষ্ট শেয়ার্ড `reputationScore`-এর বদলে সরাসরি role-scoped
                    // `reputationScoreSolver` দিয়ে র‍্যাংক করা উচিত, নাহলে dual-role ইউজারের
                    // (User role-এ সক্রিয় থাকা অবস্থায়) User-role স্কোর দিয়ে ভুলভাবে সলভার
                    // র‍্যাংক হতে পারত।
                    compareByDescending<Pair<UserEntity, Int>> { it.first.reputationScoreSolver }
                        .thenByDescending { it.second }
                )
                .take(5)
        }

        // ব্যাচ ১৪ — independent shimmer-zone পাইলট #১২: `topSolvers` (List<Pair<UserEntity,
        // Int>>) সরাসরি structural key হিসেবে ব্যবহার করা হলো (UserEntity data class ধরে নেওয়া
        // হয়েছে, ঠিক ProfileScreen-এর ব্যাচ ৬ migration-এর মতোই যেখানে currentUser data class
        // হিসেবে ব্যবহৃত হয়েছিল)।
        val topSolversPulsing = rememberFieldChangePulse(
            value = topSolvers,
            isManualRefreshing = isRefreshing
        )
        ScrollRevealShimmer(zoneKey = "admin_overview_top_solvers", revealedKeys = revealedZoneKeys) {
        PulsingValue(isUpdating = topSolversPulsing) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "টপ সলভার (সেরা সমাধানকারী)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (topSolvers.isEmpty()) {
                    Text(
                        text = "কোনো সমাধানকারী অ্যাকাউন্ট পাওয়া যায়নি।",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary
                    )
                } else {
                    topSolvers.forEachIndexed { index, (solver, completedCount) ->
                        val rankBn = DistanceUtil.toBengaliDigits((index + 1).toString())
                        val completedBn = DistanceUtil.toBengaliDigits(completedCount.toString())
                        // [ধাপ ৭খ ফিক্স] উপরের র‍্যাংকিং-এর সাথে সামঞ্জস্যপূর্ণ role-scoped স্কোর
                        val scoreBn = DistanceUtil.toBengaliDigits(String.format(Locale.US, "%.1f", solver.reputationScoreSolver))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SomadhanBg)
                                .border(0.5.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (index) {
                                                0 -> Color(0xFFFFD700)
                                                1 -> Color(0xFFC0C0C0)
                                                2 -> Color(0xFFCD7F32)
                                                else -> SomadhanDivider
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rankBn,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index < 3) Color(0xFF1E1E1E) else SomadhanTextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = solver.name.ifBlank { "সমাধানকারী" },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary
                                        )
                                        if (solver.isKycVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.Verified,
                                                contentDescription = "KYC Verified",
                                                tint = SomadhanSuccess,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$completedBn টি কাজ সম্পন্ন",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SomadhanOrangeLight)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "$scoreBn স্কোর",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrange
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        } // PulsingValue (টপ সলভার কার্ড) বন্ধ
        } // ScrollRevealShimmer (টপ সলভার scroll-reveal) বন্ধ

        }
        item {
        // ==========================================
        // ৪. রিপোর্ট এক্সপোর্ট বাটন সেকশন
        // ==========================================
        Spacer(modifier = Modifier.height(14.dp))

        fun exportUsersCsv() {
            // [ধাপ ৭ ফিক্স, ROLE_SEPARATION_AUDIT.md] এই এক্সপোর্ট-এ প্রতি ইউজারে একটাই সারি
            // (কোনো per-role কার্ড/perspective প্রেক্ষাপট নেই, তাই AdminUsersView/
            // AdminUserLookupView-এর মতো role selector context এখানে ধরার সুযোগ নেই)। তাই
            // dual-role ইউজারের জন্য একটা অস্পষ্ট generic "Balance" কলামের বদলে দুইটা আলাদা
            // role-scoped কলাম ("Balance (User)"/"Balance (Solver)") — এতে কোনো নতুন UI/role
            // selector লাগে না, শুধু কলাম বিভাজন।
            // [BALANCE_REPUTATION_ROLE_SEPARATION ধাপ ৭খ] উপরের একই যুক্তিতে `Reputation`
            // কলামও এখন "Reputation (User)"/"Reputation (Solver)" — Balance কলামের ঠিক একই
            // প্যাটার্ন, কোনো নতুন UI/selector ছাড়াই।
            val headers = listOf("User ID", "Name", "Phone", "Email", "Role", "Reputation (User)", "Reputation (Solver)", "Balance (User)", "Balance (Solver)", "KYC Status", "Created At")
            val rows = allUsers.map { u ->
                listOf(
                    u.id,
                    u.name,
                    u.phone,
                    u.email,
                    u.role,
                    u.reputationScoreUser.toString(),
                    u.reputationScoreSolver.toString(),
                    u.balanceUser.toString(),
                    u.balanceSolver.toString(),
                    u.kycStatus,
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(u.createdAt))
                )
            }
            val fileName = "somadhan_users_${System.currentTimeMillis()}.csv"
            val success = CsvExportUtil.exportToCsv(context, fileName, headers, rows)
            Toast.makeText(
                context,
                if (success) "ইউজার রিপোর্ট সংরক্ষিত হয়েছে: $fileName" else "ইউজার রিপোর্ট এক্সপোর্ট ব্যর্থ হয়েছে",
                Toast.LENGTH_SHORT
            ).show()
        }

        fun exportProblemsCsv() {
            val headers = listOf("Problem ID", "Title", "Category", "User", "User Phone", "Min Budget", "Max Budget", "Solver", "Status", "Urgency", "Created At")
            val rows = allProblems.map { p ->
                listOf(
                    p.id,
                    p.title,
                    p.categoryName,
                    p.userName,
                    p.userPhone,
                    p.minBudget.toString(),
                    p.maxBudget.toString(),
                    p.acceptedSolverName ?: "N/A",
                    p.status,
                    p.urgency,
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(p.createdAt))
                )
            }
            val fileName = "somadhan_problems_${System.currentTimeMillis()}.csv"
            val success = CsvExportUtil.exportToCsv(context, fileName, headers, rows)
            Toast.makeText(
                context,
                if (success) "সমস্যা রিপোর্ট সংরক্ষিত হয়েছে: $fileName" else "সমস্যা রিপোর্ট এক্সপোর্ট ব্যর্থ হয়েছে",
                Toast.LENGTH_SHORT
            ).show()
        }

        fun exportTransactionsCsv() {
            val headers = listOf("Transaction ID", "Problem ID", "Problem Title", "User ID", "Solver ID", "Gross Amount", "Commission Amount", "Net Amount", "Timestamp")
            val rows = allTransactions.map { t ->
                listOf(
                    t.id,
                    t.problemId,
                    t.problemTitle,
                    t.userId,
                    t.solverId,
                    t.grossAmount.toString(),
                    t.commissionAmount.toString(),
                    t.netAmount.toString(),
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(t.timestamp))
                )
            }
            val fileName = "somadhan_transactions_${System.currentTimeMillis()}.csv"
            val success = CsvExportUtil.exportToCsv(context, fileName, headers, rows)
            Toast.makeText(
                context,
                if (success) "লেনদেন রিপোর্ট সংরক্ষিত হয়েছে: $fileName" else "লেনদেন রিপোর্ট এক্সপোর্ট ব্যর্থ হয়েছে",
                Toast.LENGTH_SHORT
            ).show()
        }

        fun exportAllReports() {
            exportUsersCsv()
            exportProblemsCsv()
            exportTransactionsCsv()
            Toast.makeText(context, "সকল রিপোর্ট সফলভাবে এক্সপোর্ট সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "রিপোর্ট এক্সপোর্ট করুন",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "প্ল্যাটফর্মের সকল ইউজার, সমস্যা এবং লেনদেনের বিস্তারিত ডাটা CSV ফাইল আকারে ডাউনলোড করুন।",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { exportAllReports() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "একসাথে সকল রিপোর্ট এক্সপোর্ট করুন (All CSV)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { exportUsersCsv() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text("ইউজার CSV", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { exportProblemsCsv() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text("সমস্যা CSV", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { exportTransactionsCsv() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text("লেনদেন CSV", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary, maxLines = 1)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
