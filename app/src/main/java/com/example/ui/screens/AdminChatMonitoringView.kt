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
import com.example.ui.components.BottomSlideDialog

@Composable
fun AdminChatMonitoringView(
    viewModel: SomadhanViewModel,
    allProblems: List<ProblemEntity>,
    allUsers: List<UserEntity>
) {
    val context = LocalContext.current
    val allAdminMessages by viewModel.allAdminMessages.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ATTACHMENTS, PROPOSALS
    var selectedThreadProblemId by remember { mutableStateOf<String?>(null) }
    var messageToDelete by remember { mutableStateOf<MessageEntity?>(null) }
    var adminMessageText by remember { mutableStateOf("") }
    var isSendingAdminMessage by remember { mutableStateOf(false) }
    var adminMessageError by remember { mutableStateOf<String?>(null) }
    var currentPage by rememberSaveable { mutableIntStateOf(1) }
    val pageSize = 10

    // Group messages by problemId
    val threads = remember(allAdminMessages) {
        allAdminMessages.groupBy { it.problemId }
    }

    // Reset pagination when search query or filter changes
    LaunchedEffect(searchQuery, selectedFilter) {
        currentPage = 1
    }

    val filteredProblemIds = remember(threads, searchQuery, selectedFilter, allProblems) {
        val query = searchQuery.trim()
        threads.keys.filter { problemId ->
            val msgs = threads[problemId] ?: emptyList()
            val problem = allProblems.find { it.id == problemId }

            val matchesSearch = if (query.isBlank()) true else {
                val problemIdMatch = problemId.contains(query, ignoreCase = true) ||
                        (problem?.id?.contains(query, ignoreCase = true) == true)
                val titleMatch = problem?.title?.contains(query, ignoreCase = true) == true
                val userIdMatch = (problem?.userId?.contains(query, ignoreCase = true) == true) ||
                        (problem?.acceptedSolverId?.contains(query, ignoreCase = true) == true) ||
                        msgs.any { it.senderId.contains(query, ignoreCase = true) || it.receiverId.contains(query, ignoreCase = true) }
                val userNameMatch = (problem?.userName?.contains(query, ignoreCase = true) == true) ||
                        (problem?.acceptedSolverName?.contains(query, ignoreCase = true) == true)
                val msgMatch = msgs.any {
                    it.content.contains(query, ignoreCase = true) ||
                            it.senderName.contains(query, ignoreCase = true)
                }
                problemIdMatch || titleMatch || userIdMatch || userNameMatch || msgMatch
            }

            val matchesFilter = when (selectedFilter) {
                "ATTACHMENTS" -> msgs.any { !it.fileUrl.isNullOrBlank() }
                "PROPOSALS" -> msgs.any { it.isDirectContractProposal }
                else -> true
            }

            matchesSearch && matchesFilter
        }.sortedByDescending { pid ->
            threads[pid]?.maxOfOrNull { it.timestamp } ?: 0L
        }
    }

    val totalPages = remember(filteredProblemIds.size) {
        maxOf(1, kotlin.math.ceil(filteredProblemIds.size.toDouble() / pageSize).toInt())
    }
    val safePage = currentPage.coerceIn(1, totalPages)

    val pagedProblemIds = remember(filteredProblemIds, safePage) {
        val startIndex = (safePage - 1) * pageSize
        filteredProblemIds.drop(startIndex).take(pageSize)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(safePage) {
        listState.scrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "💬 চ্যাট ও মেসেজ মনিটরিং",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SomadhanTextPrimary
        )
        Text(
            text = "মোট থ্রেড: ${DistanceUtil.toBengaliDigits(threads.size.toString())} টি | মোট বার্তা: ${DistanceUtil.toBengaliDigits(allAdminMessages.size.toString())} টি",
            fontSize = 12.sp,
            color = SomadhanTextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar with Multi-field support
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("পোস্ট আইডি, ইউজার আইডি, নাম বা সমস্যা খুঁজুন...", fontSize = 12.5.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = SomadhanTextHint)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "মুছুন", tint = SomadhanTextHint)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SomadhanOrange,
                unfocusedBorderColor = SomadhanBorder,
                focusedContainerColor = SomadhanCardBg,
                unfocusedContainerColor = SomadhanCardBg
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("সব (${DistanceUtil.toBengaliDigits(threads.size.toString())})", fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SomadhanOrange,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedFilter == "ATTACHMENTS",
                onClick = { selectedFilter = "ATTACHMENTS" },
                label = { Text("ফাইলযুক্ত", fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SomadhanOrange,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedFilter == "PROPOSALS",
                onClick = { selectedFilter = "PROPOSALS" },
                label = { Text("প্রস্তাব", fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SomadhanOrange,
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredProblemIds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "কোনো চ্যাট থ্রেড খুঁজে পাওয়া যায়নি",
                    fontSize = 14.sp,
                    color = SomadhanTextHint
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pagedProblemIds, key = { it }) { problemId ->
                    val msgs = threads[problemId] ?: emptyList()
                    val latestMsg = msgs.maxByOrNull { it.timestamp }
                    val problem = allProblems.find { it.id == problemId }
                    val attachmentCount = msgs.count { !it.fileUrl.isNullOrBlank() }
                    val proposalCount = msgs.count { it.isDirectContractProposal }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = SomadhanDivider,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedThreadProblemId = problemId
                                val threadProblem = allProblems.find { it.id == problemId }
                                val pTitle = threadProblem?.title ?: "সমস্যা আইডি: $problemId"
                                val clientName = threadProblem?.userName ?: "অজানা"
                                val solverName = threadProblem?.acceptedSolverName ?: "অনির্ধারিত"
                                viewModel.adminLogChatView(
                                    problemId = problemId,
                                    problemTitle = pTitle,
                                    details = "সাপোর্ট ম্যানেজার কর্তৃক চ্যাট থ্রেড পর্যবেক্ষণ (পোস্ট আইডি: $problemId, ক্লায়েন্ট: $clientName, সমাধানকারী: $solverName)"
                                )
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Top Row: Problem Title & Post ID Badge & Timestamp
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
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = problem?.title ?: "সমস্যার চ্যাট থ্রেড",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SomadhanTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Post ID & Exact Time Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFF4F46E5).copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, Color(0xFF4F46E5).copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "🆔 পোস্ট আইডি: #${problem?.id ?: problemId}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF4338CA),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (latestMsg != null) {
                                    Text(
                                        text = "${Formatters.formatTimeAgo(latestMsg.timestamp)} (${Formatters.formatDateTimeBengali(latestMsg.timestamp)})",
                                        fontSize = 9.5.sp,
                                        color = SomadhanTextHint
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "গ্রাহক: ${problem?.userName ?: (msgs.firstOrNull()?.senderName ?: "অজানা")} (ID: ${problem?.userId ?: msgs.firstOrNull()?.senderId ?: ""})",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(text = "↔", fontSize = 11.sp, color = SomadhanTextHint)
                                Text(
                                    text = "সমাধানকারী: ${problem?.acceptedSolverName ?: "অনির্ধারিত"}",
                                    fontSize = 11.sp,
                                    color = SomadhanOrange,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = latestMsg?.let {
                                    val prefix = if (it.isAdminMessage) "🛡️ Support Manager" else it.senderName
                                    "$prefix: ${it.content}"
                                } ?: "কোনো বার্তা নেই",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SomadhanBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "মোট বার্তা: ${DistanceUtil.toBengaliDigits(msgs.size.toString())}",
                                        fontSize = 10.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                                if (attachmentCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SomadhanOrange.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "📎 ফাইল: ${DistanceUtil.toBengaliDigits(attachmentCount.toString())}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanOrange
                                        )
                                    }
                                }
                                if (proposalCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SomadhanSuccess.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "🎯 সরাসরি চুক্তি",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanSuccess
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pagination Controls
            if (filteredProblemIds.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { if (safePage > 1) currentPage = safePage - 1 },
                            enabled = safePage > 1,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পূর্ববর্তী", modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পূর্ববর্তী", fontSize = 11.sp)
                        }

                        Text(
                            text = "পেজ ${DistanceUtil.toBengaliDigits(safePage.toString())} / ${DistanceUtil.toBengaliDigits(totalPages.toString())}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )

                        OutlinedButton(
                            onClick = { if (safePage < totalPages) currentPage = safePage + 1 },
                            enabled = safePage < totalPages,
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

    // Modal to view messages inside thread
    selectedThreadProblemId?.let { problemId ->
        val msgs = (threads[problemId] ?: emptyList()).sortedBy { it.timestamp }
        val problem = allProblems.find { it.id == problemId }

        var visibleMessageCount by remember(problemId) { mutableIntStateOf(10) }
        val totalMsgsCount = msgs.size
        val displayedMsgs = remember(msgs, visibleMessageCount) {
            if (totalMsgsCount <= visibleMessageCount) msgs else msgs.takeLast(visibleMessageCount)
        }

        BottomSlideDialog(onDismissRequest = { selectedThreadProblemId = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(640.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = SomadhanBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = problem?.title ?: "চ্যাট বার্তা লগ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SomadhanTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "পোস্ট আইডি: #${problem?.id ?: problemId} | ${problem?.userName ?: "গ্রাহক"} ↔ ${problem?.acceptedSolverName ?: "সমাধানকারী"}",
                                fontSize = 10.5.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                        IconButton(onClick = { selectedThreadProblemId = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "বন্ধ করুন")
                        }
                    }

                    // Lazy / Infinite loading banner if there are more older messages
                    if (totalMsgsCount > visibleMessageCount) {
                        val remaining = totalMsgsCount - visibleMessageCount
                        OutlinedButton(
                            onClick = { visibleMessageCount += 10 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4F46E5)),
                            border = BorderStroke(1.dp, Color(0xFF4F46E5).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "⬆️ আরও পুরোনো বার্তা লোড করুন (${DistanceUtil.toBengaliDigits(remaining.toString())} টি বাকি)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedMsgs, key = { it.id }) { msg ->
                            val isAdmin = msg.isAdminMessage ||
                                    msg.senderName.contains("Support Manager", ignoreCase = true) ||
                                    msg.senderName.contains("সাপোর্ট ম্যানেজার", ignoreCase = true) ||
                                    msg.senderName.contains("অ্যাডমিন", ignoreCase = true) ||
                                    msg.senderName.contains("Admin", ignoreCase = true) ||
                                    msg.senderId.startsWith("ADMIN", ignoreCase = true) ||
                                    msg.senderId.equals("ADMIN_SYSTEM", ignoreCase = true) ||
                                    msg.senderId.equals("admin", ignoreCase = true)

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAdmin) Color(0xFF1E1B4B) else if (msg.isDisputeNotice) Color(0xFFFEF3C7) else SomadhanCardBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isAdmin) 1.5.dp else 1.dp,
                                        color = if (isAdmin) Color(0xFF6366F1) else SomadhanDivider,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    // Header: Sender & Action / Timestamp
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            if (isAdmin) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF4F46E5)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Shield,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFBBF24),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Support Manager",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFBBF24),
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Surface(
                                                    color = Color(0xFF312E81),
                                                    shape = RoundedCornerShape(3.dp)
                                                ) {
                                                    Text(
                                                        text = "ADMIN",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFC7D2FE),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = msg.senderName,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SomadhanOrange,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(
                                                text = Formatters.formatTimeAgo(msg.timestamp),
                                                fontSize = 9.sp,
                                                color = if (isAdmin) Color(0xFFA5B4FC) else SomadhanTextHint,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            IconButton(
                                                onClick = { messageToDelete = msg },
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "মেসেজ মুছুন",
                                                    tint = if (isAdmin) Color(0xFFF87171) else SomadhanError,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (msg.isDirectContractProposal) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = SomadhanSuccess.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.5.dp, SomadhanSuccess.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "🎯 সরাসরি চুক্তি প্রস্তাব (বাজেট: ৳${DistanceUtil.toBengaliDigits((msg.directContractBudget ?: 0.0).toInt().toString())}, মেয়াদ: ${msg.directContractDuration ?: ""})",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SomadhanSuccess,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    if (!msg.fileUrl.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isAdmin) Color(0xFF312E81) else SomadhanBg)
                                                .clickable {
                                                    FileAttachmentUtil.openFile(context, msg.fileUrl, msg.fileName)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Assignment,
                                                contentDescription = null,
                                                tint = if (isAdmin) Color(0xFFFBBF24) else SomadhanOrange,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "সংযুক্ত: ${msg.fileName ?: "ফাইল খুলুন"}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isAdmin) Color.White else SomadhanOrange,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (msg.content.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = msg.content,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            color = if (isAdmin) Color(0xFFF8FAFC) else SomadhanTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Admin Chat Message Intervention input as Support Manager
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = adminMessageText,
                            onValueChange = { adminMessageText = it },
                            placeholder = { Text("সাপোর্ট ম্যানেজার হিসেবে বার্তা দিন...", fontSize = 12.sp) },
                            singleLine = true,
                            enabled = !isSendingAdminMessage,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = SomadhanBorder,
                                focusedContainerColor = SomadhanCardBg,
                                unfocusedContainerColor = SomadhanCardBg
                            ),
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (adminMessageText.isNotBlank()) {
                                    val text = adminMessageText.trim()
                                    isSendingAdminMessage = true
                                    adminMessageError = null
                                    viewModel.adminSendMessageToProblemChat(
                                        problemId,
                                        text,
                                        onSuccess = {
                                            isSendingAdminMessage = false
                                            adminMessageText = ""
                                        },
                                        onError = { err ->
                                            isSendingAdminMessage = false
                                            adminMessageError = err
                                        }
                                    )
                                }
                            },
                            enabled = !isSendingAdminMessage,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            if (isSendingAdminMessage) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "পাঠান", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (adminMessageError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = adminMessageError ?: "", fontSize = 11.sp, color = SomadhanError)
                    }
                }
            }
        }
    }

    messageToDelete?.let { msg ->
        BottomSlideAlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = {
                Text(
                    text = "মেসেজ মুছে ফেলার নিশ্চিতকরণ",
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিতভাবে এই বার্তাটি মুছে ফেলতে চান?\n\n\"${msg.content.take(80)}\"",
                    color = SomadhanTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val msgId = msg.id
                        messageToDelete = null
                        viewModel.adminDeleteMessage(msgId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError)
                ) {
                    Text("মুছে ফেলুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }
}
