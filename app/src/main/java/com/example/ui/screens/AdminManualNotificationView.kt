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
fun AdminManualNotificationView(
    allUsers: List<com.example.data.entity.UserEntity>,
    allNotifications: List<com.example.data.entity.NotificationEntity>,
    onSendNotification: (
        role: String,
        title: String,
        message: String,
        type: String,
        scheduledFor: Long?,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onCancelScheduledNotification: (title: String, scheduledFor: Long?, timestamp: Long) -> Unit = { _, _, _ -> },
    onDeleteNotification: (title: String, timestamp: Long) -> Unit = { _, _ -> }
) {
    var selectedRole by remember { mutableStateOf("USER") } // "ALL", "USER", "SOLVER"
    var selectedType by remember { mutableStateOf("general") } // "general", "offer", "alert", "role"
    var titleText by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successNotice by remember { mutableStateOf<String?>(null) }
    var notifCurrentPage by rememberSaveable { mutableIntStateOf(1) }

    // Schedule States
    var isScheduled by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val cal = remember { Calendar.getInstance() }
    var selectedHour by remember { mutableStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(cal.get(Calendar.MINUTE)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    fun computeScheduledTimestamp(): Long {
        val dateCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = selectedDateMillis
        }
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
            set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return targetCal.timeInMillis
    }

    fun formatScheduleDisplay(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM, yyyy  hh:mm a", Locale("bn", "BD"))
        return DistanceUtil.toBengaliDigits(sdf.format(Date(timestamp)))
    }

    // [বাগফিক্স — AdminUsersView.kt-এর একই single-row প্যাটার্নের বাগ] বর্তমান সক্রিয় role-এর
    // বদলে স্থায়ী hasUserRole/hasSolverRole দিয়ে গণনা — যাতে যে অ্যাকাউন্ট একবার Solver হয়েছে
    // সে বর্তমানে User-মোডে থাকলেও "মোট সমাধানকারী" সংখ্যায় ধরা পড়ে।
    val totalAll = allUsers.size
    val totalUsers = allUsers.count { it.hasUserRole }
    val totalSolvers = allUsers.count { it.hasSolverRole }

    val targetCount = when (selectedRole) {
        "USER" -> totalUsers
        "SOLVER" -> totalSolvers
        else -> totalAll
    }

    val templates = remember {
        listOf(
            NotificationTemplatePreset(
                label = "📢 সিস্টেম আপডেট",
                title = "সিস্টেম আপডেট নোটিশ",
                message = "সমাধান প্ল্যাটফর্মে নিয়মিত সেবা বজায় রাখার জন্য সিস্টেম আপডেট চলমান রয়েছে। অ্যাপের সকল ফিচার স্বাভাবিকভাবে ব্যবহার করতে পারবেন।",
                type = "general",
                role = "ALL"
            ),
            NotificationTemplatePreset(
                label = "🎁 গ্রাহক অফার",
                title = "গ্রাহকদের জন্য আকর্ষণীয় অফার!",
                message = "আপনার যেকোনো সমস্যার সহজ সমাধানের জন্য এখনই সমস্যা পোস্ট করুন এবং সেরা দক্ষ সমাধানকারীদের নিকট থেকে দ্রুত অফার গ্রহণ করুন।",
                type = "offer",
                role = "USER"
            ),
            NotificationTemplatePreset(
                label = "🛠️ নতুন কাজ পোস্ট",
                title = "আপনার এলাকায় নতুন কাজ উপলব্ধ!",
                message = "আপনার কাজের ক্যাটাগরিতে নতুন সমস্যার পোস্ট যুক্ত হয়েছে। দ্রুত বিড করে কাজ শুরু করুন এবং আকর্ষণীয় আয় করুন।",
                type = "general",
                role = "SOLVER"
            ),
            NotificationTemplatePreset(
                label = "⚡ KYC তাগিদ",
                title = "আপনার KYC ভেরিফিকেশন সম্পন্ন করুন",
                message = "বিশ্বস্ত সমাধানকারী ব্যাজ পেতে এবং আনলিমিটেড উইথড্র সুবিধার জন্য আপনার জাতীয় পরিচয়পত্র ও প্রয়োজনীয় তথ্য দিয়ে দ্রুত KYC আবেদন করুন।",
                type = "role",
                role = "SOLVER"
            ),
            NotificationTemplatePreset(
                label = "⚠️ নিরাপত্তা সতর্কতা",
                title = "নিরাপত্তা ও লেনদেন সতর্কতা",
                message = "সমাধান অ্যাপ প্ল্যাটফর্মের বাইরে কোনো অগ্রিম নগদ অর্থ লেনদেন করবেন না। যেকোনো সহযোগিতায় আমাদের সাপোর্ট টিমে যোগাযোগ করুন।",
                type = "alert",
                role = "ALL"
            )
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("ঠিক আছে", color = SomadhanOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = SomadhanOrange,
                    todayDateBorderColor = SomadhanOrange,
                    todayContentColor = SomadhanOrange
                )
            )
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = false
        )
        BottomSlideAlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text("বিজ্ঞপ্তি পাঠানোর সময় নির্ধারণ করুন", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SomadhanTextPrimary)
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = SomadhanTextPrimary,
                            selectorColor = SomadhanOrange,
                            periodSelectorSelectedContainerColor = SomadhanOrangeLight,
                            periodSelectorSelectedContentColor = SomadhanOrange,
                            timeSelectorSelectedContainerColor = SomadhanOrangeLight,
                            timeSelectorSelectedContentColor = SomadhanOrange
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text("ঠিক আছে", color = SomadhanOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    if (showConfirmDialog) {
        val roleLabel = when (selectedRole) {
            "USER" -> "সকল সাধারণ গ্রাহক ($totalUsers জন)"
            "SOLVER" -> "সকল সমাধানকারী ($totalSolvers জন)"
            else -> "সকল ব্যবহারকারী ও সমাধানকারী ($totalAll জন)"
        }
        val scheduledTs = if (isScheduled) computeScheduledTimestamp() else null

        BottomSlideAlertDialog(
            onDismissRequest = { if (!isSending) showConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = if (isScheduled) Icons.Default.Timer else Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = SomadhanOrange,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isScheduled) "বিজ্ঞপ্তি শিডিউল নিশ্চিত করুন" else "বিজ্ঞপ্তি প্রেরণ নিশ্চিত করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isScheduled && scheduledTs != null) {
                            "আপনি কি নিশ্চিতভাবে নিম্নের বিজ্ঞপ্তিটি $roleLabel-এর নিকট ${formatScheduleDisplay(scheduledTs)} সময়ে স্বয়ংক্রিয়ভাবে প্রেরণের জন্য শিডিউল করতে চান?"
                        } else {
                            "আপনি কি নিশ্চিতভাবে নিম্নের বিজ্ঞপ্তিটি $roleLabel-এর নিকট পাঠাতে চান?"
                        },
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = titleText.trim(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = messageText.trim(),
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary,
                                maxLines = 4
                            )
                            if (isScheduled && scheduledTs != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "শিডিউল: ${formatScheduleDisplay(scheduledTs)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanOrange
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSending = true
                        errorMessage = null
                        onSendNotification(
                            selectedRole,
                            titleText,
                            messageText,
                            selectedType,
                            scheduledTs,
                            { sentCount ->
                                isSending = false
                                showConfirmDialog = false
                                successNotice = if (isScheduled && scheduledTs != null) {
                                    "সফলভাবে $sentCount জনের জন্য বিজ্ঞপ্তি শিডিউল করা হয়েছে!"
                                } else {
                                    "সফলভাবে $sentCount জনের কাছে বিজ্ঞপ্তি পাঠানো হয়েছে!"
                                }
                                titleText = ""
                                messageText = ""
                                isScheduled = false
                                notifCurrentPage = 1
                            },
                            { err ->
                                isSending = false
                                showConfirmDialog = false
                                errorMessage = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(6.dp),
                    enabled = !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        if (isSending) {
                            if (isScheduled) "শিডিউল করা হচ্ছে..." else "পাঠানো হচ্ছে..."
                        } else {
                            if (isScheduled) "হ্যাঁ, শিডিউল করুন" else "হ্যাঁ, পাঠান"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false },
                    enabled = !isSending
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    val adminNotifGroups = remember(allNotifications) {
        allNotifications
            .filter { it.id.startsWith("NOTIF_ADMIN") }
            .groupBy { "${it.title}_${it.message}_${it.scheduledFor}_${it.timestamp / 10000}" }
            .values
            .map { group ->
                val first = group.first()
                AdminNotificationHistoryGroup(
                    title = first.title,
                    message = first.message,
                    targetType = first.targetType,
                    scheduledFor = first.scheduledFor,
                    timestamp = first.timestamp,
                    recipientCount = group.size,
                    sampleNotificationId = first.id
                )
            }
            .sortedWith(
                compareByDescending<AdminNotificationHistoryGroup> {
                    val isFuture = it.scheduledFor != null && it.scheduledFor > System.currentTimeMillis()
                    if (isFuture) 1 else 0
                }.thenByDescending { it.scheduledFor ?: it.timestamp }
            )
    }

    val notifsPerPage = 5
    val notifTotalPages = maxOf(1, (adminNotifGroups.size + notifsPerPage - 1) / notifsPerPage)
    val safeNotifPage = notifCurrentPage.coerceIn(1, notifTotalPages)
    val paginatedNotifGroups = remember(adminNotifGroups, safeNotifPage) {
        adminNotifGroups.drop((safeNotifPage - 1) * notifsPerPage).take(notifsPerPage)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. Header Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SomadhanOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(SomadhanOrange, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ম্যানুয়াল পুশ বিজ্ঞপ্তি প্রেরণ ও শিডিউলিং",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SomadhanOrange
                        )
                        Text(
                            text = "গ্রাহক (User) এবং সমাধানকারী (Solver) অ্যাকাউন্টে তাৎক্ষণিক বা ভবিষ্যতে স্বয়ংক্রিয়ভাবে পাঠানোর জন্য বিজ্ঞপ্তি শিডিউল করুন।",
                            fontSize = 11.sp,
                            color = SomadhanTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Success / Error alerts
        if (successNotice != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanSuccessLight),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, SomadhanSuccess.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SomadhanSuccess, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = successNotice ?: "",
                            color = SomadhanSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { successNotice = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Close", tint = SomadhanSuccess, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        if (errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanErrorLight),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, SomadhanError.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = SomadhanError, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = SomadhanError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { errorMessage = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Close", tint = SomadhanError, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // 2. Role Selector (Target Audience)
        item {
            Text(
                text = "১. প্রাপক একাউন্ট নির্বাচন করুন (Target Audience)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SomadhanTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // USER Card
                AdminRoleSelectorCard(
                    title = "গ্রাহক (User)",
                    subtitle = "${DistanceUtil.toBengaliDigits(totalUsers.toString())} জন",
                    icon = Icons.Default.Person,
                    isSelected = selectedRole == "USER",
                    onClick = { selectedRole = "USER" },
                    modifier = Modifier.weight(1f)
                )

                // SOLVER Card
                AdminRoleSelectorCard(
                    title = "সমাধানকারী",
                    subtitle = "${DistanceUtil.toBengaliDigits(totalSolvers.toString())} জন",
                    icon = Icons.Default.Engineering,
                    isSelected = selectedRole == "SOLVER",
                    onClick = { selectedRole = "SOLVER" },
                    modifier = Modifier.weight(1f)
                )

                // ALL Card
                AdminRoleSelectorCard(
                    title = "উভয়ই (All)",
                    subtitle = "${DistanceUtil.toBengaliDigits(totalAll.toString())} জন",
                    icon = Icons.Default.People,
                    isSelected = selectedRole == "ALL",
                    onClick = { selectedRole = "ALL" },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 3. Category Selector
        item {
            Text(
                text = "২. বিজ্ঞপ্তির ধরন (Type / Category)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SomadhanTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("general", "📢 সাধারণ", Icons.Default.NotificationsActive),
                    Triple("offer", "🎁 অফার", Icons.Default.ReceiptLong),
                    Triple("alert", "⚠️ জরুরী", Icons.Default.Warning),
                    Triple("role", "👤 একাউন্ট", Icons.Default.Person)
                ).forEach { (typeKey, typeLabel, _) ->
                    val isSelected = selectedType == typeKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) SomadhanOrange else SomadhanCardBg)
                            .border(1.dp, if (isSelected) SomadhanOrange else SomadhanDivider, RoundedCornerShape(20.dp))
                            .clickable { selectedType = typeKey }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else SomadhanTextPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 4. Quick Template Presets
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💡 দ্রুত টেমপ্লেট নির্বাচন করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SomadhanTextPrimary
                )
                Text(
                    text = "ক্লিক করে অটো-ফিল করুন",
                    fontSize = 11.sp,
                    color = SomadhanTextHint
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                templates.take(3).forEach { tmpl ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SomadhanBg)
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(6.dp))
                            .clickable {
                                titleText = tmpl.title
                                messageText = tmpl.message
                                selectedType = tmpl.type
                                selectedRole = tmpl.role
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = tmpl.label,
                            fontSize = 10.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                templates.drop(3).forEach { tmpl ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SomadhanBg)
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(6.dp))
                            .clickable {
                                titleText = tmpl.title
                                messageText = tmpl.message
                                selectedType = tmpl.type
                                selectedRole = tmpl.role
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = tmpl.label,
                            fontSize = 10.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // 5. Message Input Form
        item {
            Text(
                text = "৩. বিজ্ঞপ্তির বিষয়বস্তু",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SomadhanTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text("বিজ্ঞপ্তির শিরোনাম (Title)") },
                placeholder = { Text("যেমন: বিশেষ বোনাস অফার অথবা জরুরি নোটিশ") },
                leadingIcon = {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (titleText.isNotBlank()) {
                        IconButton(onClick = { titleText = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SomadhanTextHint, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_notif_title_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SomadhanOrange,
                    unfocusedBorderColor = SomadhanBorder,
                    focusedContainerColor = SomadhanBg,
                    unfocusedContainerColor = SomadhanBg
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("বিস্তারিত বার্তা (Message)") },
                placeholder = { Text("ব্যবহারকারী বা সমাধানকারীদের উদ্দেশ্যে আপনার বিস্তারিত বিজ্ঞপ্তি বার্তা লিখুন...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_notif_message_input"),
                minLines = 3,
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SomadhanOrange,
                    unfocusedBorderColor = SomadhanBorder,
                    focusedContainerColor = SomadhanBg,
                    unfocusedContainerColor = SomadhanBg
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        // 6. Scheduling Options (Feature 2)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isScheduled) SomadhanOrangeLight.copy(alpha = 0.5f) else SomadhanBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isScheduled) SomadhanOrange else SomadhanDivider, RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (isScheduled) SomadhanOrange else SomadhanTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "বিজ্ঞপ্তি শিডিউল করুন (Schedule)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SomadhanTextPrimary
                                )
                                Text(
                                    text = if (isScheduled) "নির্ধারিত সময়ে স্বয়ংক্রিয়ভাবে পাঠানো হবে" else "তাৎক্ষণিকভাবে পাঠাতে অফ রাখুন",
                                    fontSize = 11.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        }
                        Switch(
                            checked = isScheduled,
                            onCheckedChange = { isScheduled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SomadhanOrange,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = SomadhanDivider
                            )
                        )
                    }

                    if (isScheduled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = SomadhanDivider, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        val scheduledTs = computeScheduledTimestamp()
                        val isFuture = scheduledTs > System.currentTimeMillis()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Date Button
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = SomadhanBg),
                                border = BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.7f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                val dateStr = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD")).format(Date(scheduledTs))
                                Text(DistanceUtil.toBengaliDigits(dateStr), fontSize = 12.sp, color = SomadhanTextPrimary)
                            }

                            // Time Button
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = SomadhanBg),
                                border = BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.7f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                val timeStr = SimpleDateFormat("hh:mm a", Locale("bn", "BD")).format(Date(scheduledTs))
                                Text(DistanceUtil.toBengaliDigits(timeStr), fontSize = 12.sp, color = SomadhanTextPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isFuture) "নির্ধারিত সময়: ${formatScheduleDisplay(scheduledTs)}" else "⚠️ অনুগ্রহ করে ভবিষ্যতের কোনো সময় নির্বাচন করুন",
                            fontSize = 11.sp,
                            fontWeight = if (isFuture) FontWeight.Medium else FontWeight.Bold,
                            color = if (isFuture) SomadhanOrange else SomadhanError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // 7. Live Preview
        item {
            Text(
                text = "৪. ব্যবহারকারীর স্ক্রিনে যেমন দেখাবে (Live Preview)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SomadhanTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SomadhanOrange.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(SomadhanOrange, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (titleText.isNotBlank()) titleText else "বিজ্ঞপ্তির শিরোনাম নমুনা",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SomadhanTextPrimary
                            )
                        }

                        // Tag
                        val roleBadgeText = when (selectedRole) {
                            "USER" -> "গ্রাহক"
                            "SOLVER" -> "সমাধানকারী"
                            else -> "সকল"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SomadhanOrangeLight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = roleBadgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (messageText.isNotBlank()) messageText else "এখানে আপনার প্রেরিত বিস্তারিত বিজ্ঞপ্তি বার্তা প্রদর্শিত হবে...",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ধরণ: ${
                                when (selectedType) {
                                    "offer" -> "অফার ও বোনাস"
                                    "alert" -> "জরুরী অ্যালার্ট"
                                    "role" -> "একাউন্ট / রোল"
                                    else -> "সাধারণ নোটিশ"
                                }
                            }",
                            fontSize = 10.sp,
                            color = SomadhanTextHint
                        )
                        Text(
                            text = if (isScheduled) {
                                "শিডিউল: ${formatScheduleDisplay(computeScheduledTimestamp())}"
                            } else {
                                "এখনই"
                            },
                            fontSize = 10.sp,
                            color = if (isScheduled) SomadhanOrange else SomadhanTextHint,
                            fontWeight = if (isScheduled) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 8. Send / Schedule Button
        item {
            Button(
                onClick = {
                    if (titleText.isBlank()) {
                        errorMessage = "বিজ্ঞপ্তির শিরোনাম লিখুন।"
                    } else if (messageText.isBlank()) {
                        errorMessage = "বিজ্ঞপ্তির বিস্তারিত বার্তা লিখুন।"
                    } else if (isScheduled && computeScheduledTimestamp() <= System.currentTimeMillis()) {
                        errorMessage = "অনুগ্রহ করে ভবিষ্যতের কোনো সময় নির্বাচন করুন।"
                    } else {
                        errorMessage = null
                        showConfirmDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_send_notification_btn"),
                enabled = !isSending
            ) {
                Icon(
                    imageVector = if (isScheduled) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isScheduled) {
                        "বিজ্ঞপ্তি শিডিউল করুন (${DistanceUtil.toBengaliDigits(targetCount.toString())} জন প্রাপক)"
                    } else {
                        "বিজ্ঞপ্তি প্রেরণ করুন (${DistanceUtil.toBengaliDigits(targetCount.toString())} জন প্রাপক)"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 9. Notification History Section
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "পাঠানো নোটিফিকেশন হিস্ট্রি (${DistanceUtil.toBengaliDigits(adminNotifGroups.size.toString())} টি)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = SomadhanTextPrimary
                    )
                }

                if (adminNotifGroups.isNotEmpty()) {
                    Text(
                        text = "পেজ ${DistanceUtil.toBengaliDigits(safeNotifPage.toString())}/${DistanceUtil.toBengaliDigits(notifTotalPages.toString())}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SomadhanTextHint
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (adminNotifGroups.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
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
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "এখনও কোনো বিজ্ঞপ্তি পাঠানো বা শিডিউল করা হয়নি",
                            fontSize = 13.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            items(paginatedNotifGroups, key = { "${it.title}_${it.timestamp}_${it.scheduledFor}" }) { notifGroup ->
                val isPendingScheduled = notifGroup.scheduledFor != null && notifGroup.scheduledFor > System.currentTimeMillis()

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPendingScheduled) SomadhanOrangeLight.copy(alpha = 0.15f) else SomadhanBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .border(
                            1.dp,
                            if (isPendingScheduled) SomadhanOrange.copy(alpha = 0.6f) else SomadhanDivider,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPendingScheduled) Icons.Default.Timer else Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (isPendingScheduled) SomadhanOrange else SomadhanTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = notifGroup.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                            }

                            // Scheduled / Sent Badge
                            if (isPendingScheduled) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SomadhanOrangeLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "⏰ শিডিউলড",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrange
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SomadhanSuccessLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "✅ প্রেরিত",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = notifGroup.message,
                            fontSize = 12.sp,
                            color = SomadhanTextSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = SomadhanDivider, thickness = 0.6.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val timeText = if (isPendingScheduled && notifGroup.scheduledFor != null) {
                                    "শিডিউল সময়: ${formatScheduleDisplay(notifGroup.scheduledFor)}"
                                } else {
                                    "পাঠানোর তারিখ ও সময়: ${Formatters.formatDateTimeBengali(notifGroup.timestamp)} (${Formatters.formatTimeAgo(notifGroup.timestamp)})"
                                }
                                Text(
                                    text = timeText,
                                    fontSize = 11.sp,
                                    fontWeight = if (isPendingScheduled) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isPendingScheduled) SomadhanOrange else SomadhanTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "প্রাপক সংখ্যা: ${DistanceUtil.toBengaliDigits(notifGroup.recipientCount.toString())} জন | ধরণ: ${notifGroup.targetType}",
                                    fontSize = 10.sp,
                                    color = SomadhanTextHint
                                )
                            }

                            if (isPendingScheduled) {
                                OutlinedButton(
                                    onClick = {
                                        onCancelScheduledNotification(notifGroup.title, notifGroup.scheduledFor, notifGroup.timestamp)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                                    border = BorderStroke(1.dp, SomadhanError.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        text = "বাতিল",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pagination Controls at bottom
            if (notifTotalPages > 1) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 16.dp)
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (safeNotifPage > 1) notifCurrentPage = safeNotifPage - 1 },
                                enabled = safeNotifPage > 1,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পূর্ববর্তী", modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("পূর্ববর্তী", fontSize = 11.sp)
                            }

                            Text(
                                text = "পেজ ${DistanceUtil.toBengaliDigits(safeNotifPage.toString())} / ${DistanceUtil.toBengaliDigits(notifTotalPages.toString())}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )

                            OutlinedButton(
                                onClick = { if (safeNotifPage < notifTotalPages) notifCurrentPage = safeNotifPage + 1 },
                                enabled = safeNotifPage < notifTotalPages,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("পরবর্তী", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "পরবর্তী", modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminRoleSelectorCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) SomadhanOrange.copy(alpha = 0.12f) else SomadhanCardBg,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) SomadhanOrange else SomadhanBorder
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) SomadhanOrange else SomadhanTextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) SomadhanOrange else SomadhanTextPrimary,
                maxLines = 1
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = SomadhanTextHint,
                maxLines = 1
            )
        }
    }
}
