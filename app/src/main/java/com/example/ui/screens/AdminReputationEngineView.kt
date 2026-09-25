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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.ReputationBadge
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
import com.example.ui.components.BottomSlideAlertDialog

@Composable
fun AdminReputationEngineView(
    viewModel: SomadhanViewModel
) {
    val platformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()

    // 1. Positive Events & Per-Event Daily Caps
    val repScoreBidWonSetting = platformSettings.find { it.key == "rep_score_bid_won" }?.value ?: "0.5"
    val repCapBidWonSetting = platformSettings.find { it.key == "rep_cap_daily_bid_won" }?.value ?: "2.0"

    val repScoreJobCompletedSetting = platformSettings.find { it.key == "rep_score_job_completed" }?.value ?: "0.5"
    val repCapJobCompletedSetting = platformSettings.find { it.key == "rep_cap_daily_job_completed" }?.value ?: "2.0"

    val repRateWithdrawalSetting = platformSettings.find { it.key == "rep_rate_withdrawal_per_100" }?.value ?: "0.1"
    val repCapWithdrawalSetting = platformSettings.find { it.key == "rep_cap_daily_withdrawal" }?.value ?: "2.0"

    val repScoreProblemPostedSetting = platformSettings.find { it.key == "rep_score_problem_posted" }?.value ?: "0.2"
    val repCapProblemPostedSetting = platformSettings.find { it.key == "rep_cap_daily_problem_posted" }?.value ?: "2.0"

    val repScoreRating5StarSetting = platformSettings.find { it.key == "rep_score_rating_5_star" }?.value ?: "1.5"
    val repScoreRating4StarSetting = platformSettings.find { it.key == "rep_score_rating_4_star" }?.value ?: "0.5"
    val repCapRatingSetting = platformSettings.find { it.key == "rep_cap_daily_rating" }?.value ?: "2.0"

    val repScoreKycVerifiedSetting = platformSettings.find { it.key == "rep_score_kyc_verified" }?.value ?: "5.0"

    // 2. Immediate Uncapped Penalties
    val repPenaltyRatingBadSetting = platformSettings.find { it.key == "rep_penalty_rating_bad" }?.value ?: "1.0"
    val repPenaltyJobCancelledSetting = platformSettings.find { it.key == "rep_penalty_job_cancelled" }?.value ?: "3.0"
    val repPenaltyReleaseTimeoutSetting = platformSettings.find { it.key == "rep_penalty_release_timeout" }?.value ?: "10.0"
    val repPenaltyInactive7dSetting = platformSettings.find { it.key == "rep_penalty_inactive_7d" }?.value ?: "2.0"
    val repPenaltyInactive30dSetting = platformSettings.find { it.key == "rep_penalty_inactive_30d" }?.value ?: "5.0"
    val repPenaltyRestrictedSetting = platformSettings.find { it.key == "rep_penalty_restricted" }?.value ?: "10.0"
    val repPenaltyBannedSetting = platformSettings.find { it.key == "rep_penalty_banned" }?.value ?: "25.0"

    // 3. Free Quota & Problem Extra Bill Cap
    val freeQuotaEnabledSetting = platformSettings.find { it.key == "free_quota_enabled" }?.value ?: "true"
    val freeQuotaThresholdSetting = platformSettings.find { it.key == "free_quota_reputation_threshold" }?.value ?: "80.0"
    val freeQuotaCountSetting = platformSettings.find { it.key == "free_quota_job_count" }?.value ?: "10"
    val extraBillCapPerProblemSetting = platformSettings.find { it.key == "extra_bill_reputation_cap_per_problem" }?.value ?: "10.0"

    // 4. Miss-Cycle Rules
    val missRuleEnabledSetting = platformSettings.find { it.key == "extra_payment_miss_rule_enabled" }?.value ?: "true"
    val missCycleSizeSetting = platformSettings.find { it.key == "extra_payment_miss_cycle_size" }?.value ?: "10"
    val missThresholdSetting = platformSettings.find { it.key == "extra_payment_miss_threshold" }?.value ?: "3"
    val missPenaltySetting = platformSettings.find { it.key == "extra_payment_miss_penalty" }?.value ?: "5.0"

    // Dynamic Custom Events List from Settings
    val customEventsRaw = platformSettings.find { it.key == "rep_custom_events_list" }?.value ?: ""
    val customEventKeys = remember(customEventsRaw) {
        if (customEventsRaw.isBlank()) emptyList()
        else customEventsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    // Local Mutable State for Inputs
    var bidWonScoreInput by remember(repScoreBidWonSetting) { mutableStateOf(repScoreBidWonSetting) }
    var bidWonCapInput by remember(repCapBidWonSetting) { mutableStateOf(repCapBidWonSetting) }

    var jobCompletedScoreInput by remember(repScoreJobCompletedSetting) { mutableStateOf(repScoreJobCompletedSetting) }
    var jobCompletedCapInput by remember(repCapJobCompletedSetting) { mutableStateOf(repCapJobCompletedSetting) }

    var withdrawalRateInput by remember(repRateWithdrawalSetting) { mutableStateOf(repRateWithdrawalSetting) }
    var withdrawalCapInput by remember(repCapWithdrawalSetting) { mutableStateOf(repCapWithdrawalSetting) }

    var problemPostedScoreInput by remember(repScoreProblemPostedSetting) { mutableStateOf(repScoreProblemPostedSetting) }
    var problemPostedCapInput by remember(repCapProblemPostedSetting) { mutableStateOf(repCapProblemPostedSetting) }

    var rating5StarScoreInput by remember(repScoreRating5StarSetting) { mutableStateOf(repScoreRating5StarSetting) }
    var rating4StarScoreInput by remember(repScoreRating4StarSetting) { mutableStateOf(repScoreRating4StarSetting) }
    var ratingCapInput by remember(repCapRatingSetting) { mutableStateOf(repCapRatingSetting) }

    var kycVerifiedScoreInput by remember(repScoreKycVerifiedSetting) { mutableStateOf(repScoreKycVerifiedSetting) }

    var penaltyRatingBadInput by remember(repPenaltyRatingBadSetting) { mutableStateOf(repPenaltyRatingBadSetting) }
    var penaltyJobCancelledInput by remember(repPenaltyJobCancelledSetting) { mutableStateOf(repPenaltyJobCancelledSetting) }
    var penaltyReleaseTimeoutInput by remember(repPenaltyReleaseTimeoutSetting) { mutableStateOf(repPenaltyReleaseTimeoutSetting) }
    var penaltyInactive7dInput by remember(repPenaltyInactive7dSetting) { mutableStateOf(repPenaltyInactive7dSetting) }
    var penaltyInactive30dInput by remember(repPenaltyInactive30dSetting) { mutableStateOf(repPenaltyInactive30dSetting) }
    var penaltyRestrictedInput by remember(repPenaltyRestrictedSetting) { mutableStateOf(repPenaltyRestrictedSetting) }
    var penaltyBannedInput by remember(repPenaltyBannedSetting) { mutableStateOf(repPenaltyBannedSetting) }

    // Free Quota & Miss-Cycle are strictly managed from Settings menu (Read-Only in Reputation Engine)
    val isFreeQuotaEnabled = (freeQuotaEnabledSetting != "false")
    val freeQuotaThreshold = freeQuotaThresholdSetting
    val freeQuotaCount = freeQuotaCountSetting
    val extraBillCapPerProblem = extraBillCapPerProblemSetting

    val isMissRuleEnabled = (missRuleEnabledSetting != "false")
    val missCycleSize = missCycleSizeSetting
    val missThreshold = missThresholdSetting
    val missPenalty = missPenaltySetting

    var showResetDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Standard, 1: Suggestions Gallery, 2: Active Custom Rules
    var filterType by remember { mutableStateOf("All") } // "All", "Positive", "Negative"

    // Dialog state for adopting/configuring a suggested event or adding a new custom one
    var showConfigureEventDialog by remember { mutableStateOf(false) }
    var configEventKey by remember { mutableStateOf("") }
    var configEventTitle by remember { mutableStateOf("") }
    var configEventDesc by remember { mutableStateOf("") }
    var configEventIsPositive by remember { mutableStateOf(true) }
    var configEventScoreInput by remember { mutableStateOf("1.0") }
    var configEventCapInput by remember { mutableStateOf("2.0") }

    if (showConfigureEventDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showConfigureEventDialog = false },
            icon = {
                Icon(
                    imageVector = if (configEventIsPositive) Icons.Default.TrendingUp else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (configEventIsPositive) SomadhanSuccess else SomadhanError,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (customEventKeys.contains(configEventKey)) "ইভেন্ট কনফিগারেশন আপডেট" else "নতুন রেপুটেশন ইভেন্ট যুক্ত করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ইভেন্ট কি: $configEventKey",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanOrange
                    )

                    OutlinedTextField(
                        value = configEventTitle,
                        onValueChange = { configEventTitle = it },
                        label = { Text("ইভেন্টের নাম (Title)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = configEventDesc,
                        onValueChange = { configEventDesc = it },
                        label = { Text("বিবরণ (Description)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ইভেন্ট প্রকৃতি:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        FilterChip(
                            selected = configEventIsPositive,
                            onClick = { configEventIsPositive = true },
                            label = { Text("বোনাস (+)") }
                        )
                        FilterChip(
                            selected = !configEventIsPositive,
                            onClick = { configEventIsPositive = false },
                            label = { Text("পেনাল্টি (-)") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = configEventScoreInput,
                            onValueChange = { configEventScoreInput = it },
                            label = { Text(if (configEventIsPositive) "স্কোর মান (+)" else "পেনাল্টি মান (-)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = configEventCapInput,
                            onValueChange = { configEventCapInput = it },
                            label = { Text("দৈনিক ক্যাপ (Daily Cap)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val score = configEventScoreInput.toDoubleOrNull() ?: 1.0
                        val cap = configEventCapInput.toDoubleOrNull() ?: 2.0
                        viewModel.adminSaveCustomReputationEvent(
                            eventKey = configEventKey,
                            title = configEventTitle.ifBlank { configEventKey },
                            description = configEventDesc,
                            isPositive = configEventIsPositive,
                            score = score,
                            dailyCap = cap
                        )
                        showConfigureEventDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("সংরক্ষণ ও সক্রিয় করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigureEventDialog = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    if (showResetDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = SomadhanYellowVerified,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Reset to Default (ডিফল্ট রিসেট)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Text(
                    text = "আপনি কি সমস্ত রেপুটেশন ইভেন্ট স্কোর, পার-ইভেন্ট দৈনিক ক্যাপ (2.0), পেনাল্টি ও কোটা মানগুলোকে সিস্টেমের স্ট্যান্ডার্ড ডিফল্ট মানে রিসেট করতে চান?",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        // Instantly reset local UI states
                        bidWonScoreInput = "0.5"
                        bidWonCapInput = "2.0"
                        jobCompletedScoreInput = "0.5"
                        jobCompletedCapInput = "2.0"
                        withdrawalRateInput = "0.1"
                        withdrawalCapInput = "2.0"
                        problemPostedScoreInput = "0.2"
                        problemPostedCapInput = "2.0"
                        rating5StarScoreInput = "1.5"
                        rating4StarScoreInput = "0.5"
                        ratingCapInput = "2.0"
                        kycVerifiedScoreInput = "5.0"
                        penaltyRatingBadInput = "1.0"
                        penaltyJobCancelledInput = "3.0"
                        penaltyReleaseTimeoutInput = "10.0"
                        penaltyInactive7dInput = "2.0"
                        penaltyInactive30dInput = "5.0"
                        penaltyRestrictedInput = "10.0"
                        penaltyBannedInput = "25.0"

                        val defaultSettingsMap = mapOf(
                            "rep_score_bid_won" to "0.5",
                            "rep_cap_daily_bid_won" to "2.0",
                            "rep_score_job_completed" to "0.5",
                            "rep_cap_daily_job_completed" to "2.0",
                            "rep_rate_withdrawal_per_100" to "0.1",
                            "rep_cap_daily_withdrawal" to "2.0",
                            "rep_score_problem_posted" to "0.2",
                            "rep_cap_daily_problem_posted" to "2.0",
                            "rep_score_rating_5_star" to "1.5",
                            "rep_score_rating_4_star" to "0.5",
                            "rep_cap_daily_rating" to "2.0",
                            "rep_score_kyc_verified" to "5.0",
                            "rep_penalty_rating_bad" to "1.0",
                            "rep_penalty_job_cancelled" to "3.0",
                            "rep_penalty_release_timeout" to "10.0",
                            "rep_penalty_inactive_7d" to "2.0",
                            "rep_penalty_inactive_30d" to "5.0",
                            "rep_penalty_restricted" to "10.0",
                            "rep_penalty_banned" to "25.0"
                        )
                        viewModel.adminBatchUpdatePlatformSettings(defaultSettingsMap, "স্ট্যান্ডার্ড রেপুটেশন স্কোর ডিফল্ট মানে সফলভাবে রিসেট করা হয়েছে")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanYellowVerified),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_reset_defaults_button")
                ) {
                    Text("হ্যাঁ, রিসেট করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanOrange.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SomadhanOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ডায়নামিক রেপুটেশন ইঞ্জিন",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = "প্রতি ইভেন্টের স্বতন্ত্র দৈনিক সীমা (Per-Event Daily Cap = 2.0)",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "💡 প্রতিটি আলাদা ইভেন্ট ক্যাটাগরির নিজস্ব সর্বোচ্চ দৈনিক সীমা স্বতন্ত্রভাবে কার্যকর হবে (যেমন: বিড জিতে ২.০, কাজ শেষ করে ২.০, উইথড্র করে ২.০)। নেতিবাচক পেনাল্টি আনক্যাপড ও তাৎক্ষণিক প্রযোজ্য।",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Navigation
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = SomadhanOrange,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("স্ট্যান্ডার্ড রুলস", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("সাজেস্টেড ইভেন্ট (${PREDEFINED_SUGGESTED_REPUTATION_EVENTS.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("কাস্টম ইভেন্ট (${customEventKeys.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        }

        // TAB 1: SUGGESTED REPUTATION EVENTS GALLERY
        if (selectedTab == 1) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SomadhanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ভবিষ্যৎ ইভেন্ট সাজেশন গ্যালারি",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            OutlinedButton(
                                onClick = {
                                    configEventKey = "CUSTOM_EVENT_${System.currentTimeMillis() % 10000}"
                                    configEventTitle = ""
                                    configEventDesc = ""
                                    configEventIsPositive = true
                                    configEventScoreInput = "1.0"
                                    configEventCapInput = "2.0"
                                    showConfigureEventDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("নিজে নতুন বানান", fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = "অ্যাপে ভবিষ্যতে যোগ হতে পারে এমন সম্ভাব্য ইভেন্টসমূহ। যেকোনো ইভেন্ট পছন্দমতো স্কোর ও ক্যাপ সেট করে এক ক্লিকে সক্রিয় করতে পারেন।",
                            fontSize = 12.sp,
                            color = SomadhanTextSecondary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Type Filter Chips (+ / - / All)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = filterType == "All",
                                onClick = { filterType = "All" },
                                label = { Text("সব ইভেন্ট") }
                            )
                            FilterChip(
                                selected = filterType == "Positive",
                                onClick = { filterType = "Positive" },
                                label = { Text("বোনাস (+)") }
                            )
                            FilterChip(
                                selected = filterType == "Negative",
                                onClick = { filterType = "Negative" },
                                label = { Text("পেনাল্টি (-)") }
                            )
                        }
                    }
                }
            }

            val filteredSuggestions = PREDEFINED_SUGGESTED_REPUTATION_EVENTS.filter { event ->
                when (filterType) {
                    "Positive" -> event.isPositive
                    "Negative" -> !event.isPositive
                    else -> true
                }
            }

            items(filteredSuggestions, key = { it.hashCode() }) { blueprint ->
                val isAlreadyActive = customEventKeys.contains(blueprint.eventKey)
                val currentScore = platformSettings.find { it.key == "rep_score_${blueprint.eventKey.lowercase()}" }?.value
                    ?: blueprint.suggestedScore.toString()
                val currentCap = platformSettings.find { it.key == "rep_cap_daily_${blueprint.eventKey.lowercase()}" }?.value
                    ?: blueprint.suggestedCap.toString()

                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isAlreadyActive) SomadhanSuccess.copy(alpha = 0.5f)
                        else if (blueprint.isPositive) SomadhanOrange.copy(alpha = 0.2f)
                        else SomadhanError.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (blueprint.isPositive) SomadhanSuccess.copy(alpha = 0.12f) else SomadhanError.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (blueprint.isPositive) "বোনাস (+)" else "পেনাল্টি (-)",
                                        color = if (blueprint.isPositive) SomadhanSuccess else SomadhanError,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SomadhanTextSecondary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = blueprint.category,
                                        color = SomadhanTextSecondary,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (isAlreadyActive) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SomadhanSuccess.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = SomadhanSuccess,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("সক্রিয় আছে", color = SomadhanSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = blueprint.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = blueprint.description,
                            fontSize = 12.sp,
                            color = SomadhanTextSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "সাজেস্টেড: ${if (blueprint.isPositive) "+" else "-"}${blueprint.suggestedScore} | দৈনিক ক্যাপ: ${blueprint.suggestedCap}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (blueprint.isPositive) SomadhanOrange else SomadhanError
                                )
                                Text(
                                    text = "ইভেন্ট কি: ${blueprint.eventKey}",
                                    fontSize = 10.sp,
                                    color = SomadhanTextSecondary.copy(alpha = 0.7f)
                                )
                            }

                            Button(
                                onClick = {
                                    configEventKey = blueprint.eventKey
                                    configEventTitle = blueprint.title
                                    configEventDesc = blueprint.description
                                    configEventIsPositive = blueprint.isPositive
                                    configEventScoreInput = currentScore
                                    configEventCapInput = currentCap
                                    showConfigureEventDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAlreadyActive) SomadhanCardBg else SomadhanOrange
                                ),
                                border = if (isAlreadyActive) BorderStroke(1.dp, SomadhanSuccess) else null,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isAlreadyActive) "মান পরিবর্তন করুন" else "যুক্ত ও সক্রিয় করুন",
                                    color = if (isAlreadyActive) SomadhanSuccess else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // TAB 2: ACTIVE CUSTOM RULES MANAGER
        if (selectedTab == 2) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SomadhanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "সক্রিয় কাস্টম রেপুটেশন ইভেন্টসমূহ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Button(
                                onClick = {
                                    configEventKey = "CUSTOM_RULE_${System.currentTimeMillis() % 10000}"
                                    configEventTitle = ""
                                    configEventDesc = ""
                                    configEventIsPositive = true
                                    configEventScoreInput = "1.0"
                                    configEventCapInput = "2.0"
                                    showConfigureEventDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("নতুন রুল যোগ করুন", fontSize = 12.sp)
                            }
                        }
                        Text(
                            text = "অ্যাডমিন প্যানেল থেকে সক্রিয় করা ডায়নামিক ইভেন্টগুলো এখানে তালিকাভুক্ত রয়েছে। এগুলো অ্যাপের যে কোনো কার্যক্রমে সরাসরি রেপুটেশন স্কোর ক্যালকুলেশনে প্রয়োগ হয়।",
                            fontSize = 12.sp,
                            color = SomadhanTextSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            if (customEventKeys.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = SomadhanTextSecondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "বর্তমানে কোনো কাস্টম ইভেন্ট যোগ করা নেই",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = "সাজেস্টেড ইভেন্ট ট্যাব থেকে পছন্দমতো ইভেন্ট বেছে নিন অথবা নতুন রুল তৈরি করুন।",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(customEventKeys, key = { it }) { eventKey ->
                    val title = platformSettings.find { it.key == "rep_title_${eventKey.lowercase()}" }?.value ?: eventKey
                    val desc = platformSettings.find { it.key == "rep_desc_${eventKey.lowercase()}" }?.value ?: ""
                    val isPositive = platformSettings.find { it.key == "rep_type_${eventKey.lowercase()}" }?.value != "false"
                    val score = platformSettings.find { it.key == "rep_score_${eventKey.lowercase()}" }?.value ?: "1.0"
                    val cap = platformSettings.find { it.key == "rep_cap_daily_${eventKey.lowercase()}" }?.value ?: "2.0"
                    val isEnabled = platformSettings.find { it.key == "rep_enabled_${eventKey.lowercase()}" }?.value != "false"

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isEnabled) (if (isPositive) SomadhanSuccess.copy(alpha = 0.4f) else SomadhanError.copy(alpha = 0.4f))
                            else SomadhanBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isPositive) SomadhanSuccess.copy(alpha = 0.12f) else SomadhanError.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = if (isPositive) "বোনাস (+)" else "পেনাল্টি (-)",
                                            color = if (isPositive) SomadhanSuccess else SomadhanError,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEnabled) SomadhanTextPrimary else SomadhanTextSecondary
                                    )
                                }

                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        viewModel.adminToggleCustomReputationEventStatus(eventKey, checked)
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = SomadhanOrange, checkedTrackColor = SomadhanOrange.copy(alpha = 0.3f))
                                )
                            }

                            if (desc.isNotBlank()) {
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "স্কোর: ${if (isPositive) "+" else "-"}$score | দৈনিক ক্যাপ: $cap | কি: $eventKey",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPositive) SomadhanSuccess else SomadhanError
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            configEventKey = eventKey
                                            configEventTitle = title
                                            configEventDesc = desc
                                            configEventIsPositive = isPositive
                                            configEventScoreInput = score
                                            configEventCapInput = cap
                                            showConfigureEventDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SomadhanOrange, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.adminDeleteCustomReputationEvent(eventKey)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SomadhanError, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB 0: STANDARD REPUTATION RULES (Positive Events, Penalties, Free Quota, Miss Cycle, Save All)
        if (selectedTab == 0) {
        // Section 1: ধনাত্মক অ্যাক্টিভিটি পয়েন্ট ও দৈনিক সীমা (Independent Daily Caps)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = SomadhanSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "১. ধনাত্মক ইভেন্ট ও দৈনিক সীমা (Independent Daily Caps)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1.1 Bid Won / Direct Contract
                    ReputationEventConfigRow(
                        title = "বিড জিতে কাজ নেওয়া / সরাসরি চুক্তি গ্রহণ (BID_WON)",
                        desc = "প্রতিটি বিড জয় বা ডিরেক্ট কন্ট্রাক্ট সফল অ্যাকসেপ্টে অর্জিত পয়েন্ট ও স্বতন্ত্র দৈনিক সর্বোচ্চ ক্যাপ।",
                        scoreLabel = "প্রতি ইভেন্টে পয়েন্ট",
                        scoreValue = bidWonScoreInput,
                        onScoreChange = { bidWonScoreInput = it },
                        capLabel = "দৈনিক সর্বোচ্চ ক্যাপ (Daily Cap)",
                        capValue = bidWonCapInput,
                        onCapChange = { bidWonCapInput = it },
                        scoreTag = "rep_score_bid_won_input",
                        capTag = "rep_cap_bid_won_input"
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = SomadhanDivider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1.2 Job Completed
                    ReputationEventConfigRow(
                        title = "কাজ সম্পন্ন করা (JOB_COMPLETED)",
                        desc = "কাজ সন্তোষজনকভাবে সম্পন্ন ও রিলিজ হলে সলভার এবং ক্লায়েন্ট উভয়ের প্রাপ্ত পয়েন্ট ও দৈনিক ক্যাপ।",
                        scoreLabel = "প্রতি কাজে পয়েন্ট",
                        scoreValue = jobCompletedScoreInput,
                        onScoreChange = { jobCompletedScoreInput = it },
                        capLabel = "দৈনিক সর্বোচ্চ ক্যাপ (Daily Cap)",
                        capValue = jobCompletedCapInput,
                        onCapChange = { jobCompletedCapInput = it },
                        scoreTag = "rep_score_job_completed_input",
                        capTag = "rep_cap_job_completed_input"
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = SomadhanDivider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1.3 Withdrawal Completed
                    ReputationEventConfigRow(
                        title = "উইথড্র সফল হওয়া (WITHDRAWAL_COMPLETED)",
                        desc = "উইথড্র সম্পন্ন হলে প্রতি ১০০ টাকায় পয়েন্ট (যেমন ১০০ টাকায় +০.১, ৫০০ টাকায় +০.৫) ও দৈনিক সর্বোচ্চ সীমা।",
                        scoreLabel = "প্রতি ১০০ টাকায় পয়েন্ট",
                        scoreValue = withdrawalRateInput,
                        onScoreChange = { withdrawalRateInput = it },
                        capLabel = "দৈনিক সর্বোচ্চ ক্যাপ (Daily Cap)",
                        capValue = withdrawalCapInput,
                        onCapChange = { withdrawalCapInput = it },
                        scoreTag = "rep_rate_withdrawal_input",
                        capTag = "rep_cap_withdrawal_input"
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = SomadhanDivider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1.4 Problem Posted
                    ReputationEventConfigRow(
                        title = "নতুন সমস্যা পোস্ট করা (PROBLEM_POSTED)",
                        desc = "ক্লায়েন্ট কর্তৃক প্ল্যাটফর্মে নতুন সমস্যা পোস্ট করার জন্য অর্জিত পয়েন্ট ও দৈনিক সীমা।",
                        scoreLabel = "প্রতি পোস্টে পয়েন্ট",
                        scoreValue = problemPostedScoreInput,
                        onScoreChange = { problemPostedScoreInput = it },
                        capLabel = "দৈনিক সর্বোচ্চ ক্যাপ (Daily Cap)",
                        capValue = problemPostedCapInput,
                        onCapChange = { problemPostedCapInput = it },
                        scoreTag = "rep_score_problem_posted_input",
                        capTag = "rep_cap_problem_posted_input"
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = SomadhanDivider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1.5 Rating Bonuses
                    Text(
                        text = "ভালো রেটিং বোনাস (RATING_BONUS)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanTextPrimary
                    )
                    Text(
                        text = "ক্লায়েন্ট কর্তৃক ভালো রেটিং প্রাপ্তির ভিত্তিতে প্রাপ্ত পয়েন্ট ও দৈনিক ক্যাপ।",
                        fontSize = 11.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = rating5StarScoreInput,
                            onValueChange = { rating5StarScoreInput = it },
                            label = { Text("৫ স্টার পয়েন্ট", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rep_score_rating_5_star_input")
                        )
                        OutlinedTextField(
                            value = rating4StarScoreInput,
                            onValueChange = { rating4StarScoreInput = it },
                            label = { Text("৪ স্টার পয়েন্ট", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rep_score_rating_4_star_input")
                        )
                        OutlinedTextField(
                            value = ratingCapInput,
                            onValueChange = { ratingCapInput = it },
                            label = { Text("দৈনিক ক্যাপ", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rep_cap_rating_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = SomadhanDivider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1.6 KYC Verified
                    Text(
                        text = "কেওয়াইসি ভেরিফিকেশন (KYC_VERIFIED)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanTextPrimary
                    )
                    Text(
                        text = "অ্যাকাউন্ট ভেরিফিকেশন সম্পন্ন হলে প্রোফাইলে এককালীন বোনাস পয়েন্ট যোগ হবে।",
                        fontSize = 11.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = kycVerifiedScoreInput,
                        onValueChange = { kycVerifiedScoreInput = it },
                        label = { Text("এককালীন বোনাস পয়েন্ট", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rep_score_kyc_verified_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val map = mapOf(
                                "rep_score_bid_won" to (bidWonScoreInput.toDoubleOrNull()?.toString() ?: "0.5"),
                                "rep_cap_daily_bid_won" to (bidWonCapInput.toDoubleOrNull()?.toString() ?: "2.0"),
                                "rep_score_job_completed" to (jobCompletedScoreInput.toDoubleOrNull()?.toString() ?: "0.5"),
                                "rep_cap_daily_job_completed" to (jobCompletedCapInput.toDoubleOrNull()?.toString() ?: "2.0"),
                                "rep_rate_withdrawal_per_100" to (withdrawalRateInput.toDoubleOrNull()?.toString() ?: "0.1"),
                                "rep_cap_daily_withdrawal" to (withdrawalCapInput.toDoubleOrNull()?.toString() ?: "2.0"),
                                "rep_score_problem_posted" to (problemPostedScoreInput.toDoubleOrNull()?.toString() ?: "0.2"),
                                "rep_cap_daily_problem_posted" to (problemPostedCapInput.toDoubleOrNull()?.toString() ?: "2.0"),
                                "rep_score_rating_5_star" to (rating5StarScoreInput.toDoubleOrNull()?.toString() ?: "1.5"),
                                "rep_score_rating_4_star" to (rating4StarScoreInput.toDoubleOrNull()?.toString() ?: "0.5"),
                                "rep_cap_daily_rating" to (ratingCapInput.toDoubleOrNull()?.toString() ?: "2.0"),
                                "rep_score_kyc_verified" to (kycVerifiedScoreInput.toDoubleOrNull()?.toString() ?: "5.0")
                            )
                            viewModel.adminBatchUpdatePlatformSettings(map, "পজিটিভ ইভেন্ট ও ক্যাপ সেটিংস সংরক্ষিত হয়েছে")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_positive_reputation_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পয়েন্ট ও দৈনিক ক্যাপ সংরক্ষণ করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: নেতিবাচক পেনাল্টি রুলস (Immediate Uncapped Penalties)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = SomadhanError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "২. নেতিবাচক পেনাল্টি রুলস (Uncapped Penalties)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "নেতিবাচক আচরণের পেনাল্টি কোনো দৈনিক সীমার আওতাভুক্ত নয় এবং তাৎক্ষণিকভাবে পূর্ণ কর্তন হবে।",
                        fontSize = 11.sp,
                        color = SomadhanTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bad Rating & Cancelled Job
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = penaltyRatingBadInput,
                            onValueChange = { penaltyRatingBadInput = it },
                            label = { Text("খারাপ রেটিং পেনাল্টি (১-৩★)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rep_penalty_rating_bad_input")
                        )
                        OutlinedTextField(
                            value = penaltyJobCancelledInput,
                            onValueChange = { penaltyJobCancelledInput = it },
                            label = { Text("কাজ গ্রহণপর বাতিল পেনাল্টি", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rep_penalty_job_cancelled_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 48h release timeout
                    OutlinedTextField(
                        value = penaltyReleaseTimeoutInput,
                        onValueChange = { penaltyReleaseTimeoutInput = it },
                        label = { Text("৪৮ ঘণ্টার মধ্যে ফান্ড রিলিজ না করা (ক্লায়েন্ট পেনাল্টি)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rep_penalty_release_timeout_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Inactivity penalties (7d & 30d)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = penaltyInactive7dInput,
                            onValueChange = { penaltyInactive7dInput = it },
                            label = { Text("৭ দিন নিষ্ক্রিয়তা পেনাল্টি", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rep_penalty_inactive_7d_input")
                        )
                        OutlinedTextField(
                            value = penaltyInactive30dInput,
                            onValueChange = { penaltyInactive30dInput = it },
                            label = { Text("৩০ দিন নিষ্ক্রিয়তা পেনাল্টি", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rep_penalty_inactive_30d_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Admin restricted & ban penalties
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = penaltyRestrictedInput,
                            onValueChange = { penaltyRestrictedInput = it },
                            label = { Text("রেস্ট্রিকশন পেনাল্টি", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rep_penalty_restricted_input")
                        )
                        OutlinedTextField(
                            value = penaltyBannedInput,
                            onValueChange = { penaltyBannedInput = it },
                            label = { Text("ব্যান পেনাল্টি", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rep_penalty_banned_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val map = mapOf(
                                "rep_penalty_rating_bad" to (penaltyRatingBadInput.toDoubleOrNull()?.toString() ?: "1.0"),
                                "rep_penalty_job_cancelled" to (penaltyJobCancelledInput.toDoubleOrNull()?.toString() ?: "3.0"),
                                "rep_penalty_release_timeout" to (penaltyReleaseTimeoutInput.toDoubleOrNull()?.toString() ?: "10.0"),
                                "rep_penalty_inactive_7d" to (penaltyInactive7dInput.toDoubleOrNull()?.toString() ?: "2.0"),
                                "rep_penalty_inactive_30d" to (penaltyInactive30dInput.toDoubleOrNull()?.toString() ?: "5.0"),
                                "rep_penalty_restricted" to (penaltyRestrictedInput.toDoubleOrNull()?.toString() ?: "10.0"),
                                "rep_penalty_banned" to (penaltyBannedInput.toDoubleOrNull()?.toString() ?: "25.0")
                            )
                            viewModel.adminBatchUpdatePlatformSettings(map, "পেনাল্টি সেটিংস সফলভাবে সংরক্ষিত হয়েছে")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_penalty_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পেনাল্টি সেটিংস সংরক্ষণ করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 3: ফ্রি-কমিশন কোটা ও এক্সট্রা বিল ক্যাপ (Read-Only from Settings)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "৩. ফ্রি-কমিশন কোটা ও পার-পোস্ট ক্যাপ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SomadhanBorder.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, SomadhanBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = SomadhanTextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "সেটিংস থেকে নিয়ন্ত্রিত",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SomadhanCardBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isFreeQuotaEnabled) "ফিচার অবস্থা: সক্রিয় (Active)" else "ফিচার অবস্থা: নিষ্ক্রিয় (Disabled)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isFreeQuotaEnabled) SomadhanSuccess else SomadhanError
                        )
                        Switch(
                            checked = isFreeQuotaEnabled,
                            onCheckedChange = null,
                            enabled = false,
                            modifier = Modifier.testTag("free_quota_toggle_switch_readonly")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = freeQuotaThreshold,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("ন্যূনতম রেপুটেশন থ্রেশহোল্ড", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("free_quota_threshold_input_readonly")
                        )
                        OutlinedTextField(
                            value = "$freeQuotaCount টি",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("প্রতি মাসে ফ্রি কাজের সংখ্যা", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("free_quota_count_input_readonly")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = "$extraBillCapPerProblem পয়েন্ট",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("প্রতি পোস্টে এক্সট্রা বিল রেপুটেশন ক্যাপ (Per Problem Cap)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("extra_bill_cap_per_problem_input_readonly")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SomadhanOrange.copy(alpha = 0.07f),
                        border = BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "💡 বর্তমান কনফিগারেশন বিবরণ:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrange
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• থ্রেশহোল্ড ($freeQuotaThreshold+ রেপুটেশন): রেপুটেশন স্কোর $freeQuotaThreshold বা বেশি হলে সলভার প্রতি মাসে প্রথম $freeQuotaCount টি কাজে ফ্রি কমিশন সুবিধা পান।\n• পোস্ট ক্যাপ: প্রতি প্রবলেমে অতিরিক্ত বিলের জন্য সর্বোচ্চ $extraBillCapPerProblem রেপুটেশন পয়েন্ট অর্জন সম্ভব।",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ℹ️ এই ফাংশনটির মান পরিবর্তন করতে অ্যাডমিন প্যানেলের 'সেটিংস' (Settings) মেনু ব্যবহার করুন। এখানে শুধুমাত্র বর্তমান কনফিগারেশন দৃশ্যমান।",
                        fontSize = 11.sp,
                        color = SomadhanTextHint,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }

        // Section 4: Extra Payment মিস-সাইকেল রুলস (Read-Only from Settings)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PostAdd,
                                contentDescription = null,
                                tint = SomadhanInfo,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "৪. এক্সট্রা বিল মিস-সাইকেল ইঞ্জিন",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SomadhanBorder.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, SomadhanBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = SomadhanTextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "সেটিংস থেকে নিয়ন্ত্রিত",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SomadhanCardBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isMissRuleEnabled) "ইঞ্জিন অবস্থা: সক্রিয় (Active)" else "ইঞ্জিন অবস্থা: নিষ্ক্রিয় (Disabled)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMissRuleEnabled) SomadhanSuccess else SomadhanError
                        )
                        Switch(
                            checked = isMissRuleEnabled,
                            onCheckedChange = null,
                            enabled = false,
                            modifier = Modifier.testTag("miss_rule_toggle_switch_readonly")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = "$missCycleSize টি কাজ",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("সাইকেল সাইজ (কাজের সংখ্যা)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("miss_cycle_size_input_readonly")
                        )
                        OutlinedTextField(
                            value = "$missThreshold টি",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("অনুমোদিত মিস সীমা", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("miss_threshold_input_readonly")
                        )
                        OutlinedTextField(
                            value = "$missPenalty পয়েন্ট",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("পেনাল্টি পয়েন্ট", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("miss_penalty_input_readonly")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SomadhanInfo.copy(alpha = 0.07f),
                        border = BorderStroke(1.dp, SomadhanInfo.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "💡 বর্তমান কনফিগারেশন বিবরণ:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanInfo
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• মিস সাইকেল: প্রতি $missCycleSize টি কাজের সাইকেলে সর্বোচ্চ $missThreshold টি কাজের এক্সট্রা বিল মিস গ্রহণযোগ্য।\n• স্বয়ংক্রিয় পেনাল্টি: সীমা ($missThreshold টি) অতিক্রম করলে অতিরিক্ত $missPenalty পয়েন্ট রেপুটেশন পেনাল্টি কাটা হবে।",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ℹ️ এই ফাংশনটির মান পরিবর্তন করতে অ্যাডমিন প্যানেলের 'সেটিংস' (Settings) মেনু ব্যবহার করুন। এখানে শুধুমাত্র বর্তমান কনফিগারেশন দৃশ্যমান।",
                        fontSize = 11.sp,
                        color = SomadhanTextHint,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }

        // Action Buttons Row (Reset & Save All)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            val allSettingsMap = mapOf(
                                "rep_score_bid_won" to (bidWonScoreInput.toDoubleOrNull()?.toString() ?: "0.5"),
                                "rep_cap_daily_bid_won" to (bidWonCapInput.toDoubleOrNull()?.toString() ?: "2.0"),
                                "rep_score_job_completed" to (jobCompletedScoreInput.toDoubleOrNull()?.toString() ?: "0.5"),
                                "rep_cap_daily_job_completed" to (jobCompletedCapInput.toDoubleOrNull()?.toString() ?: "2.0"),
                                "rep_rate_withdrawal_per_100" to (withdrawalRateInput.toDoubleOrNull()?.toString() ?: "0.1"),
                                "rep_cap_daily_withdrawal" to (withdrawalCapInput.toDoubleOrNull()?.toString() ?: "2.0"),
                                "rep_score_problem_posted" to (problemPostedScoreInput.toDoubleOrNull()?.toString() ?: "0.2"),
                                "rep_cap_daily_problem_posted" to (problemPostedCapInput.toDoubleOrNull()?.toString() ?: "2.0"),
                                "rep_score_rating_5_star" to (rating5StarScoreInput.toDoubleOrNull()?.toString() ?: "1.5"),
                                "rep_score_rating_4_star" to (rating4StarScoreInput.toDoubleOrNull()?.toString() ?: "0.5"),
                                "rep_cap_daily_rating" to (ratingCapInput.toDoubleOrNull()?.toString() ?: "2.0"),
                                "rep_score_kyc_verified" to (kycVerifiedScoreInput.toDoubleOrNull()?.toString() ?: "5.0"),
                                "rep_penalty_rating_bad" to (penaltyRatingBadInput.toDoubleOrNull()?.toString() ?: "1.0"),
                                "rep_penalty_job_cancelled" to (penaltyJobCancelledInput.toDoubleOrNull()?.toString() ?: "3.0"),
                                "rep_penalty_release_timeout" to (penaltyReleaseTimeoutInput.toDoubleOrNull()?.toString() ?: "10.0"),
                                "rep_penalty_inactive_7d" to (penaltyInactive7dInput.toDoubleOrNull()?.toString() ?: "2.0"),
                                "rep_penalty_inactive_30d" to (penaltyInactive30dInput.toDoubleOrNull()?.toString() ?: "5.0"),
                                "rep_penalty_restricted" to (penaltyRestrictedInput.toDoubleOrNull()?.toString() ?: "10.0"),
                                "rep_penalty_banned" to (penaltyBannedInput.toDoubleOrNull()?.toString() ?: "25.0")
                            )
                            viewModel.adminBatchUpdatePlatformSettings(allSettingsMap, "স্ট্যান্ডার্ড রেপুটেশন স্কোর ও পেনাল্টি কনফিগারেশন সংরক্ষিত হয়েছে")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .testTag("save_all_reputation_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "এক ক্লিকে সমস্ত কনফিগারেশন সংরক্ষণ করুন",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showResetDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SomadhanYellowVerified),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanTextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bottom_reset_defaults_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "রিসেট",
                        tint = SomadhanOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reset to Default (ডিফল্ট মানে রিসেট করুন)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanTextPrimary
                    )
                }
            }
        }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ReputationEventConfigRow(
    title: String,
    desc: String,
    scoreLabel: String,
    scoreValue: String,
    onScoreChange: (String) -> Unit,
    capLabel: String,
    capValue: String,
    onCapChange: (String) -> Unit,
    scoreTag: String,
    capTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = SomadhanTextPrimary
        )
        Text(
            text = desc,
            fontSize = 11.sp,
            color = SomadhanTextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = scoreValue,
                onValueChange = onScoreChange,
                label = { Text(scoreLabel, fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag(scoreTag)
            )
            OutlinedTextField(
                value = capValue,
                onValueChange = onCapChange,
                label = { Text(capLabel, fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag(capTag)
            )
        }
    }
}
