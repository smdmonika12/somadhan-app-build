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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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

@Composable
fun AdminAuditLogView(
    auditLogs: List<AdminAuditLogEntity>,
    viewModel: SomadhanViewModel? = null,
    isManualRefreshing: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("ALL") }

    // Scroll-to-load pagination state: initial 10 items, load 10 more as scrolled
    var displayedCount by rememberSaveable { mutableIntStateOf(10) }
    val initialPageSize = 10
    val loadMoreStep = 10

    val alertCount = auditLogs.count { it.actionType.endsWith("_AUTO_REPAIRED") }

    val filterCategories = listOf(
        "ALL" to "সব (${DistanceUtil.toBengaliDigits(auditLogs.size.toString())})",
        "ALERT" to "⚠️ সিস্টেম অ্যালার্ট (${DistanceUtil.toBengaliDigits(alertCount.toString())})",
        "USER" to "ইউজার / রোল",
        "DELETE" to "মুছে ফেলা (Delete)",
        "KYC" to "কেওয়াইসি",
        "FINANCE" to "অর্থ / Escrow",
        "NOTIF" to "বিজ্ঞপ্তি"
    )

    val filteredLogs = remember(auditLogs, searchQuery, selectedFilterCategory) {
        val query = searchQuery.trim().lowercase()
        auditLogs.filter { log ->
            val matchesCategory = when (selectedFilterCategory) {
                // System self-heal alerts: anything the system corrected on its own, rather than
                // an admin taking an action. Kept as its own category so these don't get lost in
                // the FINANCE tab's normal admin-action volume. Convention: any future self-heal
                // feature's actionType should end in "_AUTO_REPAIRED" to automatically land here
                // without touching this filter logic.
                "ALERT" -> log.actionType.endsWith("_AUTO_REPAIRED")
                "USER" -> log.actionType.contains("USER") || log.actionType.contains("ROLE") || log.actionType.contains("PASSWORD") || log.actionType.contains("REPUTATION") || log.actionType.contains("BADGE")
                "DELETE" -> log.actionType.startsWith("DELETE")
                "KYC" -> log.actionType.contains("KYC")
                "FINANCE" -> log.actionType.contains("BALANCE") || log.actionType.contains("ESCROW") || log.actionType.contains("WITHDRAWAL")
                "NOTIF" -> log.actionType.contains("NOTIFICATION")
                else -> true
            }

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                log.actionType.lowercase().contains(query) ||
                log.targetName.lowercase().contains(query) ||
                log.targetId.lowercase().contains(query) ||
                log.details.lowercase().contains(query) ||
                getBengaliActionName(log.actionType).lowercase().contains(query)
            }

            matchesCategory && matchesQuery
        }
    }

    // Reset displayed count when query or filter changes
    LaunchedEffect(searchQuery, selectedFilterCategory) {
        displayedCount = initialPageSize
    }

    val visibleLogs = remember(filteredLogs, displayedCount) {
        filteredLogs.take(displayedCount)
    }

    val listState = rememberLazyListState()

    val shouldLoadMore by remember(filteredLogs.size, displayedCount) {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 2 && displayedCount < filteredLogs.size
        }
    }

    // Automatically increase displayedCount on reaching bottom
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            displayedCount = minOf(displayedCount + loadMoreStep, filteredLogs.size)
        }
    }

    // Admin Panel Loading fix, সেশন ২.৯ — KYC (২.৩)/ক্যাটাগরি (২.৫)/অতিরিক্ত চার্জ (২.৬)/রিভিউ
    // (২.৭)-এর মতো, শুধু লগ-তালিকার কার্ডগুলো pulse করবে (হেডার/সার্চ-ফিল্টার, filterCategories
    // ট্যাব-চিপ এই value-র অংশ না, তাই কখনো pulse করবে না)। value = visibleLogs, তাই ফিল্টার/সার্চ
    // পাল্টানো/scroll-to-load-এ আরও লগ যোগ হওয়া/আসল ডেটা বদলানো — সবই ট্রিগার করে। sessionKey
    // "admin_audit_log_sync" — AdminPanelScreen.kt-এর ইনডেক্স ১২-এর SyncAwareContent cold-load
    // gate-এর সাথে একই key শেয়ার করে, আর pull-to-refresh সম্পন্ন হলেও (isManualRefreshing
    // সত্যি→মিথ্যা) আলাদাভাবে pulse হয়।
    val auditLogListPulse = rememberFieldChangePulse(
        value = visibleLogs,
        isManualRefreshing = isManualRefreshing,
        sessionKey = "admin_audit_log_sync",
        viewModel = viewModel
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header info card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SomadhanOrangeLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = SomadhanOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "অ্যাডমিন অ্যাক্টিভিটি অডিট লগ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = SomadhanTextPrimary
                                )
                                Text(
                                    text = "প্ল্যাটফর্মে সম্পাদিত সকল প্রশাসনিক পদক্ষেপের ট্র্যাকিং রেকর্ড",
                                    fontSize = 12.sp,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_audit_log_search_input"),
                        placeholder = {
                            Text(
                                "অ্যাকশন, ব্যবহারকারীর নাম বা বিবরণ খুঁজুন...",
                                fontSize = 13.sp,
                                color = SomadhanTextHint
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "অনুসন্ধান",
                                tint = SomadhanTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "মুছুন",
                                        tint = SomadhanTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanBg,
                            focusedTextColor = SomadhanTextPrimary,
                            unfocusedTextColor = SomadhanTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter chips row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filterCategories.take(3).forEach { (key, label) ->
                            val isSelected = selectedFilterCategory == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SomadhanOrange else SomadhanBg)
                                    .border(1.dp, if (isSelected) SomadhanOrange else SomadhanBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedFilterCategory = key }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else SomadhanTextSecondary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filterCategories.drop(3).forEach { (key, label) ->
                            val isSelected = selectedFilterCategory == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SomadhanOrange else SomadhanBg)
                                    .border(1.dp, if (isSelected) SomadhanOrange else SomadhanBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedFilterCategory = key }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else SomadhanTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "প্রদর্শিত: ${DistanceUtil.toBengaliDigits(visibleLogs.size.toString())} / ${DistanceUtil.toBengaliDigits(filteredLogs.size.toString())} টি লগ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SomadhanTextSecondary
                )
                if (searchQuery.isNotBlank() || selectedFilterCategory != "ALL") {
                    Text(
                        text = "ফিল্টার সক্রিয়",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SomadhanOrange
                    )
                }
            }
        }

        // Empty state
        if (filteredLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SomadhanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = SomadhanTextHint,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "কোনো অডিট লগ পাওয়া যায়নি" else "এখনো কোনো অ্যাডমিন অ্যাকশন রেকর্ড নেই",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SomadhanTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "অনুসন্ধান পরিবর্তন করে আবার চেষ্টা করুন।" else "অ্যাডমিন প্যানেল থেকে কোনো পদক্ষেপ নেওয়া হলে এখানে স্বয়ংক্রিয়ভাবে সংরক্ষিত হবে।",
                            fontSize = 12.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }
            }
        } else {
            items(visibleLogs, key = { it.id }) { log ->
                PulsingValue(isUpdating = auditLogListPulse) {
                    AuditLogItemCard(log = log)
                }
            }

            // Scroll to load indicator / Manual Load More button
            if (displayedCount < filteredLogs.size) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OutlinedButton(
                            onClick = {
                                displayedCount = minOf(displayedCount + loadMoreStep, filteredLogs.size)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SomadhanOrange),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanOrange)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "আরও ${DistanceUtil.toBengaliDigits(minOf(loadMoreStep, filteredLogs.size - displayedCount).toString())}টি লগ লোড করুন (স্ক্রল করুন)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogItemCard(log: AdminAuditLogEntity) {
    val (icon, tintColor, bgTint) = getAuditActionVisuals(log.actionType)
    val banglaActionName = getBengaliActionName(log.actionType)

    Card(
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SomadhanBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_audit_log_item_${log.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Icon + Action Title + Timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(bgTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = banglaActionName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SomadhanTextPrimary
                    )
                    Text(
                        text = log.actionType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tintColor
                    )
                }

                // Time ago badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SomadhanBg)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Formatters.formatTimeAgo(log.timestamp),
                        fontSize = 11.sp,
                        color = SomadhanTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Exact Date & Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SomadhanBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = SomadhanOrange,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "সম্পন্নের সঠিক সময়: ${Formatters.formatDateTimeBengali(log.timestamp)}",
                    fontSize = 11.sp,
                    color = SomadhanTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Target information
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SomadhanBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "টার্গেট:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = log.targetName.ifBlank { log.targetId },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SomadhanTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Details section if available
            if (log.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = log.details,
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

internal fun getBengaliActionName(actionType: String): String {
    return when (actionType) {
        "BAN_USER" -> "ব্যবহারকারী নিষিদ্ধ (Ban)"
        "UNBAN_USER" -> "নিষেধাজ্ঞা প্রত্যাহার (Unban)"
        "RESTRICT_USER" -> "অ্যাকাউন্ট সীমাবদ্ধ (Restricted)"
        "UNRESTRICT_USER" -> "সীমাবদ্ধতা প্রত্যাহার"
        "ADD_VERIFIED_BADGE" -> "ভেরিফাইড ব্যাজ প্রদান"
        "REMOVE_VERIFIED_BADGE" -> "ভেরিফাইড ব্যাজ অপসারণ"
        "DELETE_USER" -> "ইউজার অ্যাকাউন্ট ডিলিট"
        "DELETE_PROBLEM" -> "সমস্যা পোস্ট ডিলিট"
        "DELETE_MESSAGE" -> "মেসেজ ডিলিট"
        "DELETE_RATING" -> "রিভিউ মুছে ফেলা"
        "DELETE_NOTIFICATION" -> "বিজ্ঞপ্তি মুছে ফেলা"
        "APPROVE_KYC" -> "KYC আবেদন অনুমোদন"
        "REJECT_KYC" -> "KYC আবেদন বাতিল"
        "UPDATE_WITHDRAWAL_STATUS" -> "উইথড্র স্ট্যাটাস আপডেট"
        "ADD_BALANCE" -> "ব্যালেন্স যোগ"
        "DEDUCT_BALANCE" -> "ব্যালেন্স কর্তন"
        "CHANGE_ROLE" -> "অ্যাকাউন্ট রোল পরিবর্তন"
        "RESET_PASSWORD" -> "পাসওয়ার্ড রিসেট"
        "ADJUST_REPUTATION" -> "রেপুটেশন স্কোর সমন্বয়"
        "UPDATE_PROBLEM_STATUS" -> "সমস্যার স্ট্যাটাস আপডেট"
        "UPDATE_PROBLEM_BUDGET" -> "বাজেট পুনর্নির্ধারণ"
        "REASSIGN_SOLVER" -> "সমাধানকারী পরিবর্তন"
        "REJECT_BID" -> "বিড বাতিল (Reject)"
        "RELEASE_ESCROW" -> "Escrow পেমেন্ট রিলিজ"
        "REFUND_ESCROW" -> "Escrow টাকা রিফান্ড"
        "ADD_CATEGORY" -> "নতুন ক্যাটাগরি যুক্ত"
        "UPDATE_CATEGORY" -> "ক্যাটাগরি তথ্য আপডেট"
        "ENABLE_CATEGORY" -> "ক্যাটাগরি সক্রিয়"
        "DISABLE_CATEGORY" -> "ক্যাটাগরি নিষ্ক্রিয়"
        "DELETE_CATEGORY" -> "ক্যাটাগরি ডিলিট"
        "SEND_NOTIFICATION" -> "ম্যানুয়াল বিজ্ঞপ্তি প্রেরণ"
        "UPDATE_SETTING" -> "প্ল্যাটফর্ম সেটিংস পরিবর্তন"
        "ESCROW_PAYOUT_AUTO_REPAIRED" -> "⚠️ Escrow পেআউট স্বয়ংক্রিয়ভাবে মেরামত করা হয়েছে"
        else -> actionType
    }
}

@Composable
internal fun getAuditActionVisuals(actionType: String): Triple<androidx.compose.ui.graphics.vector.ImageVector, Color, Color> {
    return when {
        // System self-heal alerts get their own distinct red/orange treatment so they stand out
        // from ordinary admin-initiated actions -- checked first since these are the entries most
        // in need of an admin's attention. Any future self-heal actionType ending in
        // "_AUTO_REPAIRED" automatically gets this treatment too.
        actionType.endsWith("_AUTO_REPAIRED") -> {
            Triple(Icons.Default.Warning, SomadhanError, SomadhanErrorLight)
        }
        actionType.startsWith("DELETE") || actionType == "BAN_USER" || actionType == "RESTRICT_USER" || actionType == "REJECT_KYC" || actionType == "REJECT_BID" -> {
            Triple(Icons.Default.Delete, SomadhanError, SomadhanErrorLight)
        }
        actionType == "APPROVE_KYC" || actionType == "UNBAN_USER" || actionType == "UNRESTRICT_USER" || actionType == "ADD_VERIFIED_BADGE" || actionType == "RELEASE_ESCROW" || actionType == "ADD_BALANCE" || actionType == "ENABLE_CATEGORY" -> {
            Triple(Icons.Default.CheckCircle, SomadhanSuccess, SomadhanSuccessLight)
        }
        actionType == "DEDUCT_BALANCE" || actionType == "REFUND_ESCROW" || actionType == "UPDATE_WITHDRAWAL_STATUS" -> {
            Triple(Icons.Default.AccountBalanceWallet, SomadhanOrange, SomadhanOrangeLight)
        }
        actionType == "SEND_NOTIFICATION" -> {
            Triple(Icons.Default.NotificationsActive, SomadhanInfo, Color(0xFFE8F4FD))
        }
        actionType == "CHANGE_ROLE" || actionType == "RESET_PASSWORD" || actionType == "ADJUST_REPUTATION" -> {
            Triple(Icons.Default.Shield, SomadhanInfo, Color(0xFFE8F4FD))
        }
        else -> {
            Triple(Icons.Default.Info, SomadhanInfo, Color(0xFFE8F4FD))
        }
    }
}
