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
fun AdminCancelledBidsView(
    cancelledBids: List<BidEntity>,
    allProblems: List<ProblemEntity> = emptyList(),
    allUsers: List<UserEntity> = emptyList(),
    viewModel: SomadhanViewModel? = null,
    isManualRefreshing: Boolean = false
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTimeFilter by remember { mutableStateOf("ALL") } // ALL, TODAY, THIS_WEEK, FREQUENT
    var selectedSolverFilterId by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableIntStateOf(1) }
    val itemsPerPage = 10

    var showFrequentCancellersModal by remember { mutableStateOf(false) }
    var modalSearchQuery by remember { mutableStateOf("") }
    var modalVisibleCount by remember { mutableIntStateOf(10) }

    // Solver cancellation counts map
    val solverCancellationCounts = remember(cancelledBids) {
        cancelledBids.groupBy { it.solverId }.mapValues { it.value.size }
    }

    // Top frequent cancellers (sorted by cancellations desc)
    val frequentCancellers = remember(solverCancellationCounts, allUsers) {
        solverCancellationCounts.entries
            .filter { it.value > 1 }
            .sortedByDescending { it.value }
    }

    val top3Cancellers = remember(frequentCancellers) {
        frequentCancellers.take(3)
    }

    // Filtered bids based on search, time filter, and solver filter
    val now = System.currentTimeMillis()
    val oneDayMs = 24L * 60 * 60 * 1000
    val oneWeekMs = 7L * oneDayMs

    val filteredBids = remember(
        cancelledBids,
        searchQuery,
        selectedTimeFilter,
        selectedSolverFilterId,
        allProblems,
        allUsers
    ) {
        cancelledBids.filter { bid ->
            // Time filter
            val matchesTime = when (selectedTimeFilter) {
                "TODAY" -> (now - bid.createdAt) <= oneDayMs
                "THIS_WEEK" -> (now - bid.createdAt) <= oneWeekMs
                "FREQUENT" -> (solverCancellationCounts[bid.solverId] ?: 0) > 1
                else -> true
            }
            if (!matchesTime) return@filter false

            // Solver specific filter
            if (selectedSolverFilterId != null && bid.solverId != selectedSolverFilterId) {
                return@filter false
            }

            // Search query filter (Multi-field)
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                val problem = allProblems.find { it.id == bid.problemId }
                val problemTitle = (problem?.title ?: "").lowercase()
                val categoryName = (problem?.categoryName ?: "").lowercase()
                val solver = allUsers.find { it.id == bid.solverId }
                val solverName = (solver?.name ?: bid.solverName).lowercase()
                val solverPhone = (solver?.phone ?: "").lowercase()
                val bidMsg = bid.message.lowercase()

                val matchesQuery = solverName.contains(q) ||
                    solverPhone.contains(q) ||
                    problemTitle.contains(q) ||
                    categoryName.contains(q) ||
                    bid.id.lowercase().contains(q) ||
                    bid.problemId.lowercase().contains(q) ||
                    bid.solverId.lowercase().contains(q) ||
                    bidMsg.contains(q)

                if (!matchesQuery) return@filter false
            }

            true
        }
    }

    // Reset pagination when search query or filter changes
    LaunchedEffect(searchQuery, selectedTimeFilter, selectedSolverFilterId) {
        currentPage = 1
    }

    val totalPages = remember(filteredBids.size, itemsPerPage) {
        if (filteredBids.isEmpty()) 1 else ((filteredBids.size - 1) / itemsPerPage) + 1
    }
    val safeCurrentPage = currentPage.coerceIn(1, totalPages)

    val paginatedBids = remember(filteredBids, safeCurrentPage, itemsPerPage) {
        val startIndex = (safeCurrentPage - 1) * itemsPerPage
        filteredBids.drop(startIndex).take(itemsPerPage)
    }

    // Admin Panel Loading fix, সেশন ২.১১ — re-entry/pull-to-refresh/সার্চ-ফিল্টার-পেজ পাল্টালে
    // শুধু বিড-তালিকার কার্ড pulse করবে (KYC/Categories/অতিরিক্ত-চার্জ-ধরনের); সামারি-সংখ্যা বক্স
    // ("মোট বাতিল"/"বারবার বাতিলকারী" গণনা), সার্চ/ফিল্টার বার, pagination bar (LazyColumn-এর
    // বাইরে/আলাদা item ব্লক) স্থির থাকবে। "টপ-৩ বারবার বাতিলকারী" হাইলাইট আলাদা নিজস্ব পাল্স পায়
    // (নিচে frequentCancellersPulse দেখুন, ব্যবহারকারীর অনুরোধে ২.১১-এর পরে যোগ করা)।
    val cancelledBidsListPulse = rememberFieldChangePulse(
        value = paginatedBids,
        isManualRefreshing = isManualRefreshing,
        sessionKey = "admin_cancelled_bids_sync",
        viewModel = viewModel
    )

    // ব্যবহারকারীর অনুরোধে (২.১১-এর পর) — "ঘন ঘন বাতিলকারী" (top-3 highlight) অংশও নিজস্ব
    // rememberFieldChangePulse দিয়ে আলাদাভাবে pulse করবে, ঠিক Dashboard-এর stat-card/category-list
    // প্যাটার্নের মতো (একই sessionKey, আলাদা value) — re-entry/pull-to-refresh/ডেটা-বদল, সবই।
    val frequentCancellersPulse = rememberFieldChangePulse(
        value = top3Cancellers,
        isManualRefreshing = isManualRefreshing,
        sessionKey = "admin_cancelled_bids_sync",
        viewModel = viewModel
    )

    // ব্যবহারকারীর অনুরোধে — ৩টা সামারি-সংখ্যা বক্সের ভ্যালু (পুরো বক্স না, শুধু সংখ্যাটাই) pulse
    // করবে। Dashboard-এর statsPulse-এর মতো তিনটা সংখ্যা একসাথে একটা list value হিসেবে দেওয়া হলো।
    val summaryCountsPulse = rememberFieldChangePulse(
        value = listOf(cancelledBids.size, solverCancellationCounts.size, frequentCancellers.size),
        isManualRefreshing = isManualRefreshing,
        sessionKey = "admin_cancelled_bids_sync",
        viewModel = viewModel
    )

    // Admin Panel Loading fix, সেশন ২.৩৩ (Ground Rule ২০ রেট্রোফিট) — coerced safeCurrentPage-এর
    // বদলে raw currentPage-এ re-key করা হয়েছে, যাতে realtime ডেটা বদলে totalPages কমে গিয়ে
    // safeCurrentPage নিজে থেকে ক্ল্যাম্প হলে (ব্যবহারকারী পেজ বদলায়নি এমন অবস্থায়ও) জোর করে
    // টপে scroll না হয়ে যায়। paginatedBids-এর bounds-check এখনো safeCurrentPage-ই ব্যবহার করে,
    // অপরিবর্তিত — শুধু এই scroll-effect-এর key বদলেছে।
    LaunchedEffect(currentPage) {
        listState.scrollToItem(0)
    }

    // Modal BottomSheet / Dialog for all frequent cancellers
    if (showFrequentCancellersModal) {
        val filteredModalCancellers = remember(frequentCancellers, modalSearchQuery, allUsers) {
            if (modalSearchQuery.isBlank()) {
                frequentCancellers
            } else {
                val q = modalSearchQuery.trim().lowercase()
                frequentCancellers.filter { (solverId, _) ->
                    val solver = allUsers.find { it.id == solverId }
                    val name = (solver?.name ?: "").lowercase()
                    val phone = (solver?.phone ?: "").lowercase()
                    name.contains(q) || phone.contains(q) || solverId.lowercase().contains(q)
                }
            }
        }

        val displayedModalCancellers = filteredModalCancellers.take(modalVisibleCount)

        // ব্যবহারকারীর অনুরোধে — "সব দেখুন" মোডালের তালিকাও pulse করবে (একই sessionKey, ভিন্ন value)।
        val modalCancellersPulse = rememberFieldChangePulse(
            value = displayedModalCancellers,
            isManualRefreshing = isManualRefreshing,
            sessionKey = "admin_cancelled_bids_sync",
            viewModel = viewModel
        )

        BottomSlideAlertDialog(
            onDismissRequest = {
                showFrequentCancellersModal = false
                modalSearchQuery = ""
                modalVisibleCount = 10
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = SomadhanError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "বারবার বাতিলকারী সমাধানকারী",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            showFrequentCancellersModal = false
                            modalSearchQuery = ""
                            modalVisibleCount = 10
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = SomadhanTextSecondary)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                ) {
                    Text(
                        text = "যে সকল সমাধানকারী ২ বা ততোধিক বার বিড বা কাজ বাতিল করেছেন (${DistanceUtil.toBengaliDigits(filteredModalCancellers.size.toString())} জন):",
                        fontSize = 11.5.sp,
                        color = SomadhanTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search within modal
                    OutlinedTextField(
                        value = modalSearchQuery,
                        onValueChange = {
                            modalSearchQuery = it
                            modalVisibleCount = 10
                        },
                        placeholder = { Text("নাম, ফোন বা আইডি দিয়ে খুঁজুন...", fontSize = 11.sp, color = SomadhanTextHint) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SomadhanTextHint, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (modalSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { modalSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = SomadhanTextHint, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanDivider,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanBg
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredModalCancellers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("কোনো সলভার পাওয়া যায়নি", fontSize = 12.sp, color = SomadhanTextHint)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayedModalCancellers, key = { it.key }) { (solverId, cancelCount) ->
                                val solver = allUsers.find { it.id == solverId }
                                val rankIndex = frequentCancellers.indexOfFirst { it.key == solverId } + 1

                                PulsingValue(isUpdating = modalCancellersPulse) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SomadhanErrorLight, RoundedCornerShape(8.dp))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
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
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(if (rankIndex <= 3) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (rankIndex == 1) "🥇" else if (rankIndex == 2) "🥈" else if (rankIndex == 3) "🥉" else "#${rankIndex}",
                                                        fontSize = if (rankIndex <= 3) 12.sp else 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = solver?.name ?: solverId,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SomadhanTextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (!solver?.phone.isNullOrBlank()) {
                                                        Text(
                                                            text = "ফোন: ${solver?.phone?.let { Formatters.toLocalDisplayFormat(it) }}",
                                                            fontSize = 10.5.sp,
                                                            color = SomadhanTextSecondary
                                                        )
                                                    }
                                                }
                                            }

                                            Surface(
                                                color = SomadhanError,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "${DistanceUtil.toBengaliDigits(cancelCount.toString())} বার বাতিল",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Divider(color = SomadhanDivider, thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!solver?.phone.isNullOrBlank()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${solver?.phone}"))
                                                        context.startActivity(intent)
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(11.dp), tint = SomadhanSuccess)
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("কল", fontSize = 10.sp, color = SomadhanSuccess)
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }

                                            Button(
                                                onClick = {
                                                    selectedSolverFilterId = solverId
                                                    selectedTimeFilter = "ALL"
                                                    searchQuery = ""
                                                    currentPage = 1
                                                    showFrequentCancellersModal = false
                                                    modalSearchQuery = ""
                                                    modalVisibleCount = 10
                                                    Toast.makeText(context, "${solver?.name ?: solverId}-এর বাতিলকৃত বিড ফিল্টার করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                    coroutineScope.launch {
                                                        listState.animateScrollToItem(1) // scroll to search & list section
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color.White)
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("বিডসমূহ দেখুন", fontSize = 10.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                                }
                            }

                            // Load more pagination button inside modal
                            if (displayedModalCancellers.size < filteredModalCancellers.size) {
                                item {
                                    OutlinedButton(
                                        onClick = { modalVisibleCount += 10 },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "আরও ১০ জন লোড করুন (+${filteredModalCancellers.size - displayedModalCancellers.size} জন বাকি)",
                                            fontSize = 11.sp,
                                            color = SomadhanOrange,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFrequentCancellersModal = false
                        modalSearchQuery = ""
                        modalVisibleCount = 10
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        // Summary & Analytics Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = SomadhanError,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "বিড বাতিলের সামগ্রিক পরিসংখ্যান",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }
                        StatusBadge(status = "CANCELLED")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3 Metric Overview Boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SomadhanBg)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "মোট বাতিল বিড",
                                    fontSize = 10.5.sp,
                                    color = SomadhanTextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                PulsingValue(isUpdating = summaryCountsPulse) {
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(cancelledBids.size.toString())}টি",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanError
                                )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SomadhanBg)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "বাতিলকারী",
                                    fontSize = 10.5.sp,
                                    color = SomadhanTextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                PulsingValue(isUpdating = summaryCountsPulse) {
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(solverCancellationCounts.size.toString())}জন",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SomadhanBg)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "বারবার বাতিলকারী",
                                    fontSize = 10.5.sp,
                                    color = SomadhanTextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                PulsingValue(isUpdating = summaryCountsPulse) {
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(frequentCancellers.size.toString())}জন",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanError
                                )
                                }
                            }
                        }
                    }

                    // TOP 3 Frequent Canceller Solvers Highlights Section (Idea 3)
                    if (top3Cancellers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = SomadhanDivider, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🚨 সর্বোচ্চ বাতিলকারী সলভার (টপ ৩)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanError
                                )
                            }

                            if (frequentCancellers.size > 1) {
                                Surface(
                                    color = SomadhanErrorLight,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable {
                                        modalVisibleCount = 10
                                        modalSearchQuery = ""
                                        showFrequentCancellersModal = true
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "সব দেখুন (${DistanceUtil.toBengaliDigits(frequentCancellers.size.toString())} জন)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanError
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = SomadhanError,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Compact Top 3 Highlights Grid / Cards
                        PulsingValue(isUpdating = frequentCancellersPulse) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            top3Cancellers.forEachIndexed { index, (solverId, count) ->
                                val solver = allUsers.find { it.id == solverId }
                                val rankNumber = DistanceUtil.toBengaliDigits((index + 1).toString())

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFF7F7))
                                        .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedSolverFilterId = solverId
                                            selectedTimeFilter = "ALL"
                                            searchQuery = ""
                                            currentPage = 1
                                            Toast.makeText(context, "${solver?.name ?: solverId}-এর বাতিলকৃত বিড ফিল্টার করা হয়েছে", Toast.LENGTH_SHORT).show()
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(1)
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Sleek Rank Badge
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (index == 0) SomadhanError else Color(0xFFFED7D7)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = rankNumber,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (index == 0) Color.White else SomadhanError
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = solver?.name ?: solverId,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SomadhanTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!solver?.phone.isNullOrBlank()) {
                                                Text(
                                                    text = "ফোন: ${solver?.phone?.let { Formatters.toLocalDisplayFormat(it) }}",
                                                    fontSize = 10.sp,
                                                    color = SomadhanTextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        color = SomadhanError,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${DistanceUtil.toBengaliDigits(count.toString())} বার বাতিল",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
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

        // Multi-field Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("বিড আইডি, সমস্যা আইডি, সলভার নাম, ফোন বা বিবরণ...", fontSize = 12.sp, color = SomadhanTextHint) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = SomadhanTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = SomadhanTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SomadhanOrange,
                    unfocusedBorderColor = SomadhanDivider,
                    focusedContainerColor = SomadhanCardBg,
                    unfocusedContainerColor = SomadhanCardBg
                ),
                singleLine = true
            )
        }

        // Filter Chips Row (Time Filters & Active Solver Filter)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If a solver is actively filtered
                if (selectedSolverFilterId != null) {
                    val activeSolver = allUsers.find { it.id == selectedSolverFilterId }
                    Surface(
                        color = SomadhanOrange,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { selectedSolverFilterId = null }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "সলভার: ${activeSolver?.name ?: selectedSolverFilterId}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "ফিল্টার মুছুন",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                val timeFilters = listOf(
                    "ALL" to "সব বিড (${cancelledBids.size})",
                    "TODAY" to "আজকে",
                    "THIS_WEEK" to "এই সপ্তাহে",
                    "FREQUENT" to "বারবার বাতিলকারী"
                )

                timeFilters.forEach { (key, label) ->
                    val isSelected = selectedTimeFilter == key && selectedSolverFilterId == null
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SomadhanOrange else SomadhanBg
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (isSelected) SomadhanOrange else SomadhanDivider,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                selectedTimeFilter = key
                                selectedSolverFilterId = null
                            }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else SomadhanTextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Section Title & Item Count
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "বাতিলকৃত বিডের তালিকা (${DistanceUtil.toBengaliDigits(filteredBids.size.toString())}টি)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                if (filteredBids.isNotEmpty()) {
                    Text(
                        text = "পৃষ্ঠা ${DistanceUtil.toBengaliDigits(safeCurrentPage.toString())} / ${DistanceUtil.toBengaliDigits(totalPages.toString())}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SomadhanTextSecondary
                    )
                }
            }
        }

        // List of Cancelled Bids
        if (filteredBids.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedSolverFilterId != null) "কোনো ফলাফল পাওয়া যায়নি" else "কোনো বাতিলকৃত বিড নেই",
                        fontSize = 13.sp,
                        color = SomadhanTextHint
                    )
                }
            }
        } else {
            items(paginatedBids, key = { it.id }) { bid ->
                val problem = allProblems.find { it.id == bid.problemId }
                val solver = allUsers.find { it.id == bid.solverId }
                val solverTotalCancellations = solverCancellationCounts[bid.solverId] ?: 1

                PulsingValue(isUpdating = cancelledBidsListPulse) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Header: Problem Title & Status Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = problem?.title ?: "সমস্যা: ${bid.problemId}",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Problem ID Chip
                                    Surface(
                                        color = SomadhanBg,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.5.dp, SomadhanDivider),
                                        modifier = Modifier.clickable {
                                            clipboardManager.setText(AnnotatedString(bid.problemId))
                                            Toast.makeText(context, "সমস্যা আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "পোস্ট: #${bid.problemId.takeLast(8)}",
                                                fontSize = 9.5.sp,
                                                color = SomadhanTextSecondary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(Icons.Default.ContentCopy, contentDescription = "কপি", modifier = Modifier.size(9.dp), tint = SomadhanTextHint)
                                        }
                                    }

                                    // Bid ID Chip
                                    Surface(
                                        color = SomadhanBg,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.5.dp, SomadhanDivider),
                                        modifier = Modifier.clickable {
                                            clipboardManager.setText(AnnotatedString(bid.id))
                                            Toast.makeText(context, "বিড আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "বিড: #${bid.id.takeLast(8)}",
                                                fontSize = 9.5.sp,
                                                color = SomadhanTextSecondary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(Icons.Default.ContentCopy, contentDescription = "কপি", modifier = Modifier.size(9.dp), tint = SomadhanTextHint)
                                        }
                                    }
                                }
                            }

                            StatusBadge(status = "CANCELLED")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Solver info and Financials
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = solver?.name ?: bid.solverName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanTextPrimary
                                    )
                                    if (solverTotalCancellations > 1) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(SomadhanErrorLight)
                                                .clickable {
                                                    selectedSolverFilterId = bid.solverId
                                                    Toast.makeText(context, "${solver?.name ?: bid.solverName}-এর বিড ফিল্টার করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        ) {
                                            Text(
                                                text = "🔥 ${DistanceUtil.toBengaliDigits(solverTotalCancellations.toString())} বার বাতিল",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanError
                                            )
                                        }
                                    }
                                }

                                if (!solver?.phone.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ফোন: ${solver?.phone?.let { Formatters.toLocalDisplayFormat(it) }}",
                                        fontSize = 10.5.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "৳ ${DistanceUtil.toBengaliDigits(bid.amount.toInt().toString())}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanError
                                )
                                if (bid.estimatedTime.isNotBlank()) {
                                    Text(
                                        text = "সময়সীমা: ${bid.estimatedTime}",
                                        fontSize = 9.5.sp,
                                        color = SomadhanTextHint
                                    )
                                }
                            }
                        }

                        // Bid proposal message
                        if (bid.message.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SomadhanBg)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "বিড বার্তা: \"${bid.message}\"",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = SomadhanDivider, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Footer: Bengali Date & Exact Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = SomadhanTextHint,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${Formatters.formatDateTimeBengali(bid.createdAt)} (${Formatters.formatTimeAgo(bid.createdAt)})",
                                    fontSize = 10.sp,
                                    color = SomadhanTextHint
                                )
                            }

                            Text(
                                text = "স্ট্যাটাস: বাতিলকৃত",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanError
                            )
                        }
                    }
                }
                }
            }
        }
    }

        // Pagination Controls at Bottom (Fixed)
        if (filteredBids.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
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
                        onClick = { if (safeCurrentPage > 1) currentPage = safeCurrentPage - 1 },
                        enabled = safeCurrentPage > 1,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পূর্ববর্তী", modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পূর্ববর্তী", fontSize = 11.sp)
                    }

                    Text(
                        text = "পৃষ্ঠা ${DistanceUtil.toBengaliDigits(safeCurrentPage.toString())} / ${DistanceUtil.toBengaliDigits(totalPages.toString())} (মোট ${DistanceUtil.toBengaliDigits(filteredBids.size.toString())}টি)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    OutlinedButton(
                        onClick = { if (safeCurrentPage < totalPages) currentPage = safeCurrentPage + 1 },
                        enabled = safeCurrentPage < totalPages,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
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

data class ReputationEventBlueprint(
    val eventKey: String,
    val title: String,
    val description: String,
    val isPositive: Boolean,
    val suggestedScore: Double,
    val suggestedCap: Double,
    val category: String
)

val PREDEFINED_SUGGESTED_REPUTATION_EVENTS = listOf(
    ReputationEventBlueprint(
        eventKey = "FAST_RESPONSE_ACCEPTED",
        title = "দ্রুত রেসপন্স ও বিড গ্রহণ",
        description = "সমস্যা পোস্টের ১৫ মিনিটের মধ্যে প্রথম সাড়া দিয়ে বিড গ্রহণ হলে স্পেশাল বুস্ট।",
        isPositive = true,
        suggestedScore = 0.5,
        suggestedCap = 1.5,
        category = "পারফরম্যান্স"
    ),
    ReputationEventBlueprint(
        eventKey = "FIRST_SOLVER_BADGE",
        title = "কমিউনিটির প্রথম সমাধানকারী",
        description = "নতুন কোনো ক্যাটাগরি বা এলাকায় প্রথম কাজ সম্পন্ন করে মাইলস্টোন অর্জন।",
        isPositive = true,
        suggestedScore = 0.5,
        suggestedCap = 1.0,
        category = "কমিউনিটি"
    ),
    ReputationEventBlueprint(
        eventKey = "DISPUTE_SETTLED_FRIENDLY",
        title = "পারস্পরিক সমঝোতায় বিবাদ নিষ্পত্তি",
        description = "অ্যাডমিন হস্তক্ষেপ ছাড়া双方 ক্লায়েন্ট-সলভার সৌহার্দ্যপূর্ণভাবে ডিসপুট সমাধান করলে।",
        isPositive = true,
        suggestedScore = 1.0,
        suggestedCap = 2.0,
        category = "বিশ্বাস"
    ),
    ReputationEventBlueprint(
        eventKey = "PROFILE_COMPLETED_100",
        title = "প্রোফাইল শতভাগ সম্পূর্ণকরণ",
        description = "এনআইডি, স্কিল ট্যাগ, বায়ো এবং কাজের পোর্টফোলিও শতভাগ পূর্ণ করার বোনাস।",
        isPositive = true,
        suggestedScore = 2.0,
        suggestedCap = 2.0,
        category = "বিশ্বাস"
    ),
    ReputationEventBlueprint(
        eventKey = "REPEAT_CLIENT_HIRE",
        title = "একই ক্লায়েন্টের পুনরাবৃত্তি হায়ার",
        description = "পূর্বে কাজ করিয়ে সন্তুষ্ট ক্লায়েন্ট সরাসরি পুনরায় কাজ দিলে বিশেষ লয়্যালটি স্কোর।",
        isPositive = true,
        suggestedScore = 1.0,
        suggestedCap = 2.0,
        category = "পারফরম্যান্স"
    ),
    ReputationEventBlueprint(
        eventKey = "TIP_BONUS_RECEIVED",
        title = "ক্লায়েন্ট কর্তৃক বকশিশ/টিপ প্রদান",
        description = "কাজে অতিরিক্ত সন্তুষ্ট হয়ে ক্লায়েন্ট টিপ বা বোনাস অর্থ প্রদান করলে।",
        isPositive = true,
        suggestedScore = 0.5,
        suggestedCap = 1.5,
        category = "পারফরম্যান্স"
    ),
    ReputationEventBlueprint(
        eventKey = "URGENT_SOS_HELP",
        title = "জরুরি SOS সাহায্য প্রদান",
        description = "ইমার্জেন্সি/জরুরি ডাক পেয়ে দ্রুততম সময়ে স্পট গিয়ে কাজ সমাধান করা।",
        isPositive = true,
        suggestedScore = 1.5,
        suggestedCap = 3.0,
        category = "পারফরম্যান্স"
    ),
    ReputationEventBlueprint(
        eventKey = "ZERO_DISPUTE_MILESTONE",
        title = "ধারাবাহিক ১০টি কাজে বিবাদহীন মাইলস্টোন",
        description = "টানা ১০টি সফল কাজে কোনো অভিযোগ বা পেনাল্টি না পাওয়ার ধারাবাহিকতা বোনাস।",
        isPositive = true,
        suggestedScore = 2.0,
        suggestedCap = 2.0,
        category = "পারফরম্যান্স"
    ),
    ReputationEventBlueprint(
        eventKey = "BADGE_EXPERT_VERIFIED",
        title = "স্কিল টেস্ট / এক্সপার্ট ব্যাজ অর্জন",
        description = "নির্দিষ্ট টেকনিক্যাল স্কিল টেস্টে ৮০%+ নম্বর পেয়ে স্পেশাল ভেরিফাইড ব্যাজ অর্জন।",
        isPositive = true,
        suggestedScore = 3.0,
        suggestedCap = 3.0,
        category = "বিশ্বাস"
    ),
    // Penalties / Negative Suggestions
    ReputationEventBlueprint(
        eventKey = "LATE_DELIVERY",
        title = "নির্দিষ্ট সময়সীমা পার করা (Late Delivery)",
        description = "নির্ধারিত সময় পেরিয়ে যাওয়ার পরও কোনো যুক্তিসঙ্গত কারণ ছাড়া কাজ শেষ না করা।",
        isPositive = false,
        suggestedScore = 1.5,
        suggestedCap = 3.0,
        category = "পারফরম্যান্স"
    ),
    ReputationEventBlueprint(
        eventKey = "UNRESPONSIVE_CHAT",
        title = "চ্যাটে ২৪ ঘণ্টার বেশি নিষ্ক্রিয় থাকা",
        description = "কাজ রানিং থাকা অবস্থায় অপর পক্ষের মেসেজের উত্তর না দিয়ে উধাও থাকা।",
        isPositive = false,
        suggestedScore = 1.0,
        suggestedCap = 2.0,
        category = "কমিউনিটি"
    ),
    ReputationEventBlueprint(
        eventKey = "DISPUTE_LOST",
        title = "অ্যাডমিন তদন্তে বিবাদে দোষী সাব্যস্ত",
        description = "ডিসপুট কেসে ইচ্ছাকৃত প্রতারণা বা দায়িত্বে চরম অবহেলা প্রমাণিত হলে পেনাল্টি।",
        isPositive = false,
        suggestedScore = 5.0,
        suggestedCap = 10.0,
        category = "নিরাপত্তা"
    ),
    ReputationEventBlueprint(
        eventKey = "OFFLINE_PAYMENT_ATTEMPT",
        title = "অ্যাপের বাইরে সরাসরি লেনদেনের চেষ্টা",
        description = "প্ল্যাটফর্ম ফি এড়াতে অ্যাপের বাইরে ক্যাশ বা ব্যক্তিগত পেমেন্ট নেওয়ার চেষ্টা।",
        isPositive = false,
        suggestedScore = 15.0,
        suggestedCap = 30.0,
        category = "নিরাপত্তা"
    ),
    ReputationEventBlueprint(
        eventKey = "FAKE_PROOF_SUBMISSION",
        title = "কাজের জাল বা ভুয়া প্রুফ সাবমিট",
        description = "কাজ সম্পন্ন না করেই ফিনিশড দাবি করে ভুয়া ফটো বা ফেক ওটিপি সাবমিট করা।",
        isPositive = false,
        suggestedScore = 10.0,
        suggestedCap = 20.0,
        category = "নিরাপত্তা"
    ),
    ReputationEventBlueprint(
        eventKey = "CLIENT_RUDE_BEHAVIOR",
        title = "চ্যাট বা কলে অসদাচরণ ও গালাগালি",
        description = "অফিশিয়াল চ্যাট বা ফোনে অপর পক্ষের সাথে অশোভন আচরণ বা হুমকি দেওয়া।",
        isPositive = false,
        suggestedScore = 3.0,
        suggestedCap = 6.0,
        category = "কমিউনিটি"
    ),
    ReputationEventBlueprint(
        eventKey = "FREQUENT_BID_WITHDRAWAL",
        title = "বিড গ্রহণ করার পর বারবার বাতিল",
        description = "ক্লায়েন্ট বিড গ্রহণ করার পর সলভার কাজ শুরু না করে বারবার কাজ ড্রপ করা।",
        isPositive = false,
        suggestedScore = 2.0,
        suggestedCap = 4.0,
        category = "পারফরম্যান্স"
    )
)
