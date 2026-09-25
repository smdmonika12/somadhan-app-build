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
import com.example.ui.components.PulsingValue
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.ReputationBadge
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.rememberFieldChangePulse
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
fun FaqFormDialog(
    faqToEdit: FaqEntity? = null,
    defaultAudience: String = "USER",
    onDismiss: () -> Unit,
    onSave: (FaqEntity) -> Unit
) {
    var question by remember { mutableStateOf(faqToEdit?.question ?: "") }
    var answer by remember { mutableStateOf(faqToEdit?.answer ?: "") }
    var targetAudience by remember { mutableStateOf(faqToEdit?.targetAudience ?: defaultAudience) }
    var displayOrder by remember { mutableStateOf(faqToEdit?.displayOrder?.toString() ?: "0") }
    var isActive by remember { mutableStateOf(faqToEdit?.isActive ?: true) }

    BottomSlideAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (faqToEdit == null) Icons.Default.AddCircleOutline else Icons.Default.Edit,
                    contentDescription = null,
                    tint = SomadhanOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (faqToEdit == null) "নতুন FAQ যুক্ত করুন" else "FAQ এডিট করুন",
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )
            }
        },
        text = {
            Column {
                // Target Audience Selector
                Text(
                    text = "FAQ এর লক্ষ্য গ্রুপ (Target Audience)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (targetAudience == "USER") SomadhanOrange else SomadhanCardBg)
                            .border(1.dp, if (targetAudience == "USER") SomadhanOrange else SomadhanDivider, RoundedCornerShape(8.dp))
                            .clickable { targetAudience = "USER" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ব্যবহারকারী (User)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (targetAudience == "USER") Color.White else SomadhanTextPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (targetAudience == "SOLVER") SomadhanOrange else SomadhanCardBg)
                            .border(1.dp, if (targetAudience == "SOLVER") SomadhanOrange else SomadhanDivider, RoundedCornerShape(8.dp))
                            .clickable { targetAudience = "SOLVER" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "সমাধানকারী (Solver)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (targetAudience == "SOLVER") Color.White else SomadhanTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("প্রশ্ন") },
                    placeholder = { Text("যেমন: কিভাবে সমস্যা সমাধান পোস্ট করব?") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("faq_question_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("উত্তর") },
                    placeholder = { Text("বিস্তারিত উত্তর লিখুন...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("faq_answer_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = displayOrder,
                    onValueChange = { displayOrder = it },
                    label = { Text("প্রদর্শনের ক্রম (Display Order)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("faq_order_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("FAQ সক্রিয় অবস্থা", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                        Text(if (isActive) "ব্যবহারকারীরা FAQ পেজে দেখতে পাবেন" else "নিষ্ক্রিয় (লুকানো থাকবে)", fontSize = 11.sp, color = SomadhanTextHint)
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SomadhanOrange,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = SomadhanDivider
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (question.isNotBlank() && answer.isNotBlank()) {
                        val parsedOrder = displayOrder.toIntOrNull() ?: 0
                        val faq = faqToEdit?.copy(
                            question = question.trim(),
                            answer = answer.trim(),
                            targetAudience = targetAudience,
                            displayOrder = parsedOrder,
                            isActive = isActive
                        ) ?: FaqEntity(
                            id = "FAQ_" + System.currentTimeMillis(),
                            question = question.trim(),
                            answer = answer.trim(),
                            targetAudience = targetAudience,
                            displayOrder = parsedOrder,
                            isActive = isActive
                        )
                        onSave(faq)
                    }
                },
                enabled = question.isNotBlank() && answer.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("faq_save_button")
            ) {
                Text(if (faqToEdit == null) "সংরক্ষণ" else "আপডেট করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = SomadhanTextSecondary)
            }
        }
    )
}

@Composable
fun AdminFaqManagementView(
    faqs: List<FaqEntity>,
    onAddClick: (String) -> Unit,
    onEditFaq: (FaqEntity) -> Unit,
    onDeleteFaq: (String) -> Unit,
    viewModel: SomadhanViewModel? = null,
    isManualRefreshing: Boolean = false
) {
    var selectedFaqTab by remember { mutableIntStateOf(0) } // 0: User FAQ, 1: Solver FAQ
    var faqToDelete by remember { mutableStateOf<FaqEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    if (faqToDelete != null) {
        val f = faqToDelete!!
        BottomSlideAlertDialog(
            onDismissRequest = { faqToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = SomadhanError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("FAQ মুছে ফেলা", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে '${f.question}' FAQ-টি মুছে ফেলতে চান?",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFaq(f.id)
                        faqToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { faqToDelete = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    val currentAudience = if (selectedFaqTab == 0) "USER" else "SOLVER"

    val tabFaqs = remember(faqs, selectedFaqTab) {
        faqs.filter {
            if (selectedFaqTab == 0) {
                it.targetAudience == "USER" || it.targetAudience == "BOTH"
            } else {
                it.targetAudience == "SOLVER" || it.targetAudience == "BOTH"
            }
        }
    }

    val filteredFaqs = remember(tabFaqs, searchQuery) {
        val list = if (searchQuery.isBlank()) {
            tabFaqs
        } else {
            tabFaqs.filter {
                it.question.contains(searchQuery, ignoreCase = true) ||
                it.answer.contains(searchQuery, ignoreCase = true)
            }
        }
        list.sortedWith(compareBy({ !it.isActive }, { it.displayOrder }))
    }

    // Admin Panel Loading fix, সেশন ২.১০ — KYC (২.৩)/ক্যাটাগরি (২.৫)-এর মতো, শুধু FAQ-লিস্টের
    // কার্ডগুলো pulse করবে (ট্যাব হেডার, সার্চ বার, Add বাটন এই value-র অংশ না, তাই কখনো pulse
    // করবে না)। value = filteredFaqs, যেটা ইতিমধ্যে selectedFaqTab-এর উপর নির্ভরশীল (tabFaqs-এর
    // মাধ্যমে) — তাই ট্যাব পাল্টানো/সার্চ পাল্টানো/আসল ডেটা বদলানো (add/edit/delete-সহ), সবই
    // ট্রিগার করে। sessionKey "admin_faq_management_sync" — AdminPanelScreen.kt-এর ইনডেক্স
    // ১৩-এর SyncAwareContent cold-load gate-এর সাথে একই key শেয়ার করে, আর pull-to-refresh
    // সম্পন্ন হলেও (isManualRefreshing সত্যি→মিথ্যা) আলাদাভাবে pulse হয়।
    val faqListPulse = rememberFieldChangePulse(
        value = filteredFaqs,
        isManualRefreshing = isManualRefreshing,
        sessionKey = "admin_faq_management_sync",
        viewModel = viewModel
    )

    val userFaqCount = faqs.count { it.targetAudience == "USER" || it.targetAudience == "BOTH" }
    val solverFaqCount = faqs.count { it.targetAudience == "SOLVER" || it.targetAudience == "BOTH" }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Tab Row for User vs Solver FAQ
        TabRow(
            selectedTabIndex = selectedFaqTab,
            containerColor = SomadhanCardBg,
            contentColor = SomadhanOrange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedFaqTab == 0,
                onClick = { selectedFaqTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "ইউজার FAQ (${DistanceUtil.toBengaliDigits(userFaqCount.toString())})",
                            fontWeight = if (selectedFaqTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            )
            Tab(
                selected = selectedFaqTab == 1,
                onClick = { selectedFaqTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "সলভার FAQ (${DistanceUtil.toBengaliDigits(solverFaqCount.toString())})",
                            fontWeight = if (selectedFaqTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Add Button
        Button(
            onClick = { onAddClick(currentAudience) },
            colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().testTag("admin_add_faq_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (selectedFaqTab == 0) "নতুন ইউজার FAQ যোগ করুন" else "নতুন সলভার FAQ যোগ করুন",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (selectedFaqTab == 0) "ইউজার FAQ খুঁজুন..." else "সলভার FAQ খুঁজুন...",
                    fontSize = 13.sp,
                    color = SomadhanTextHint
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = SomadhanTextHint, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SomadhanTextHint, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SomadhanOrange,
                unfocusedBorderColor = SomadhanDivider
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Count Summary
        val activeCount = tabFaqs.count { it.isActive }
        val inactiveCount = tabFaqs.size - activeCount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${if (selectedFaqTab == 0) "ইউজার" else "সলভার"} মোট FAQ: ${DistanceUtil.toBengaliDigits(tabFaqs.size.toString())}টি (সক্রিয়: ${DistanceUtil.toBengaliDigits(activeCount.toString())}, নিষ্ক্রিয়: ${DistanceUtil.toBengaliDigits(inactiveCount.toString())})",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SomadhanTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredFaqs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "এই ট্যাবে কোনো FAQ পাওয়া যায়নি" else "অনুসন্ধানের সাথে মিলে এমন কোনো FAQ পাওয়া যায়নি",
                    fontSize = 13.sp,
                    color = SomadhanTextHint
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("admin_faq_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFaqs, key = { it.id }) { faq ->
                    val isInactive = !faq.isActive

                    PulsingValue(isUpdating = faqListPulse) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isInactive) SomadhanCardBg.copy(alpha = 0.5f) else SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isInactive) SomadhanDivider.copy(alpha = 0.5f) else SomadhanDivider,
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = faq.question,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isInactive) SomadhanTextHint else SomadhanTextPrimary,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        // Audience Badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (faq.targetAudience == "SOLVER") Color(0xFFEDE9FE) else Color(0xFFE0F2FE)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (faq.targetAudience == "SOLVER") "সলভার" else "ইউজার",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (faq.targetAudience == "SOLVER") Color(0xFF7C3AED) else Color(0xFF0284C7)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        if (isInactive) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(SomadhanErrorLight)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "নিষ্ক্রিয়",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SomadhanError
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
                                                    text = "সক্রিয়",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SomadhanSuccess
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ক্রম: ${DistanceUtil.toBengaliDigits(faq.displayOrder.toString())}",
                                        fontSize = 11.sp,
                                        color = SomadhanTextHint
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onEditFaq(faq) },
                                        modifier = Modifier.size(32.dp).testTag("admin_edit_faq_${faq.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "এডিট করুন",
                                            tint = SomadhanOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { faqToDelete = faq },
                                        modifier = Modifier.size(32.dp).testTag("admin_delete_faq_${faq.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "মুছুন",
                                            tint = SomadhanError,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = SomadhanDivider.copy(alpha = 0.5f), thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = faq.answer,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = if (isInactive) SomadhanTextHint else SomadhanTextSecondary
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

data class NotificationTemplatePreset(
    val label: String,
    val title: String,
    val message: String,
    val type: String,
    val role: String
)

data class AdminNotificationHistoryGroup(
    val title: String,
    val message: String,
    val targetType: String,
    val scheduledFor: Long?,
    val timestamp: Long,
    val recipientCount: Int,
    val sampleNotificationId: String
)
