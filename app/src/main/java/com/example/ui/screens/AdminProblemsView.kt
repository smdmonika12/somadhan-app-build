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
import kotlinx.coroutines.delay
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
fun AdminProblemsView(
    problems: List<com.example.data.entity.ProblemEntity>,
    allUsers: List<com.example.data.entity.UserEntity> = emptyList(),
    viewModel: SomadhanViewModel,
    isManualRefreshing: Boolean = false,
    onDeleteProblem: (String) -> Unit
) {
    var selectedProblemForDetail by remember { mutableStateOf<com.example.data.entity.ProblemEntity?>(null) }
    var problemForStatusChange by remember { mutableStateOf<com.example.data.entity.ProblemEntity?>(null) }
    var problemForBudgetEdit by remember { mutableStateOf<com.example.data.entity.ProblemEntity?>(null) }
    var problemForBidsView by remember { mutableStateOf<com.example.data.entity.ProblemEntity?>(null) }
    var problemForChatView by remember { mutableStateOf<com.example.data.entity.ProblemEntity?>(null) }
    var problemForSolverReassign by remember { mutableStateOf<com.example.data.entity.ProblemEntity?>(null) }
    var problemForDeleteConfirm by remember { mutableStateOf<com.example.data.entity.ProblemEntity?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    val filteredProblems = remember(problems, searchQuery, selectedStatusFilter) {
        val query = searchQuery.trim().lowercase()
        problems.filter { problem ->
            val matchesFilter = when (selectedStatusFilter) {
                "ALL" -> true
                else -> problem.status.equals(selectedStatusFilter, ignoreCase = true)
            }
            val matchesQuery = if (query.isBlank()) true else {
                problem.title.lowercase().contains(query) ||
                    problem.description.lowercase().contains(query) ||
                    problem.userName.lowercase().contains(query) ||
                    problem.userId.lowercase().contains(query) ||
                    problem.id.lowercase().contains(query) ||
                    problem.categoryName.lowercase().contains(query) ||
                    (problem.acceptedSolverName?.lowercase()?.contains(query) == true) ||
                    (problem.acceptedSolverId?.lowercase()?.contains(query) == true) ||
                    (problem.userPhone.lowercase().contains(query))
            }
            matchesFilter && matchesQuery
        }
    }

    val pageSize = 10
    var currentPage by remember { mutableIntStateOf(1) }

    LaunchedEffect(searchQuery, selectedStatusFilter) {
        currentPage = 1
    }

    val totalPages = maxOf(1, (filteredProblems.size + pageSize - 1) / pageSize)
    val safePage = currentPage.coerceIn(1, totalPages)
    val paginatedProblems = remember(filteredProblems, safePage, pageSize) {
        val fromIndex = (safePage - 1) * pageSize
        if (fromIndex >= filteredProblems.size) {
            emptyList()
        } else {
            filteredProblems.subList(fromIndex, minOf(fromIndex + pageSize, filteredProblems.size))
        }
    }

    val listState = rememberLazyListState()
    // Ground Rule ২০ — safePage-এর বদলে raw currentPage-এ key করা হয়েছে (scroll-jump ফিক্স):
    // কোনো সমস্যা ডিলিট হয়ে totalPages কমে গেলে coerced safePage স্বয়ংক্রিয়ভাবে বদলে যেতে পারে
    // (ইউজার pagination না ছুঁলেও), raw currentPage শুধু সার্চ/ফিল্টার-রিসেট বা explicit next/prev
    // ক্লিকেই বদলায়। bounds-check (paginatedProblems গণনা) এখনো safePage ব্যবহার করে, অপরিবর্তিত।
    LaunchedEffect(currentPage) {
        listState.scrollToItem(0)
    }

    // Ground Rule ২০ — ফিল্টার/সার্চ/pagination বদলালে দৃশ্যমান সব প্রবলেম-কার্ড একসাথে ছোট্ট করে
    // pulse করবে (Ground Rule ১৯-এর per-item action-pulse-এর পাশাপাশি, দুটো OR করা হয়েছে নিচে)।
    var isFilterRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(searchQuery, selectedStatusFilter, currentPage) {
        isFilterRefreshing = true
        try {
            delay(350L)
        } finally {
            isFilterRefreshing = false
        }
    }

    // Ground Rule ২১, সেশন ২.১৯.৬ — নতুন problem post (Insert) অথবা status transition (Update)
    // হলে শুধু সেই কার্ডটাই pulse করবে (Users/Withdrawal-এর প্যাটার্নেই)।
    val recentlyChangedProblemIds by viewModel.recentlyChangedProblemIds.collectAsStateWithLifecycle()

    // 1. Status Update Dialog
    if (problemForStatusChange != null) {
        val targetProb = problemForStatusChange!!
        AdminUpdateStatusDialog(
            problem = targetProb,
            onUpdateStatus = { newStatus ->
                viewModel.adminUpdateProblemStatus(targetProb.id, newStatus)
                problemForStatusChange = null
            },
            onDismiss = { problemForStatusChange = null }
        )
    }

    // 2. Budget Edit Dialog
    if (problemForBudgetEdit != null) {
        val targetProb = problemForBudgetEdit!!
        AdminEditBudgetDialog(
            problem = targetProb,
            onUpdateBudget = { minBudget, maxBudget ->
                viewModel.adminUpdateProblemBudget(targetProb.id, minBudget, maxBudget)
                problemForBudgetEdit = null
            },
            onDismiss = { problemForBudgetEdit = null }
        )
    }

    // 3. View Bids Dialog / Sheet
    if (problemForBidsView != null) {
        val targetProb = problemForBidsView!!
        AdminProblemBidsDialog(
            problem = targetProb,
            viewModel = viewModel,
            onDismiss = { problemForBidsView = null }
        )
    }

    // 4. View Chat BottomSheet
    if (problemForChatView != null) {
        val targetProb = problemForChatView!!
        AdminProblemChatBottomSheet(
            problem = targetProb,
            viewModel = viewModel,
            onDismiss = { problemForChatView = null }
        )
    }

    // 5. Reassign Solver Dialog
    if (problemForSolverReassign != null) {
        val targetProb = problemForSolverReassign!!
        AdminReassignSolverDialog(
            problem = targetProb,
            allUsers = allUsers,
            onReassign = { solverId, solverName ->
                viewModel.adminReassignSolver(targetProb.id, solverId, solverName)
                problemForSolverReassign = null
            },
            onDismiss = { problemForSolverReassign = null }
        )
    }

    // 5. Delete Confirmation Dialog
    if (problemForDeleteConfirm != null) {
        val targetProb = problemForDeleteConfirm!!
        BottomSlideAlertDialog(
            onDismissRequest = { problemForDeleteConfirm = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = SomadhanError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সমস্যা মুছে ফেলা নিশ্চিত করুন", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে '${targetProb.title}' সমস্যাটি সম্পূর্ণরূপে মুছে ফেলতে চান? এই কাজটি ফিরিয়ে নেওয়া যাবে না।",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProblem(targetProb.id)
                        problemForDeleteConfirm = null
                        if (selectedProblemForDetail?.id == targetProb.id) {
                            selectedProblemForDetail = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { problemForDeleteConfirm = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // Detailed Problem Dialog
    if (selectedProblemForDetail != null) {
        val prob = selectedProblemForDetail!!
        val escrow by viewModel.getEscrowForProblem(prob.id).collectAsStateWithLifecycle(initialValue = null)
        val additionalCharges by viewModel.getAllAdditionalCharges(prob.id).collectAsStateWithLifecycle(initialValue = emptyList())

        BottomSlideAlertDialog(
            onDismissRequest = { selectedProblemForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assignment, contentDescription = null, tint = SomadhanOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "সমস্যার বিস্তারিত বিবরণ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SomadhanTextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = prob.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(status = prob.status)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ক্যাটাগরি: ${prob.categoryName}",
                            fontSize = 11.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = prob.description,
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "পোস্টকারী: ${prob.userName} (${prob.userPhone ?: ""})",
                        fontSize = 11.sp,
                        color = SomadhanTextHint
                    )
                    if (!prob.acceptedSolverName.isNullOrBlank()) {
                        Text(
                            text = "নির্বাচিত সমাধানকারী: ${prob.acceptedSolverName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanOrange
                        )
                    }
                    Text(
                        text = "বাজেট রেঞ্জ: ৳ ${prob.minBudget.toInt()} - ৳ ${prob.maxBudget.toInt()}",
                        fontSize = 11.sp,
                        color = SomadhanTextHint
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = SomadhanDivider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Escrow Status Section
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Escrow ফান্ড অবস্থা",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SomadhanTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    if (escrow != null) {
                        val esc = escrow!!
                        val totalEscrow = esc.baseAmount + esc.extraAmount
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SomadhanOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "স্ট্যাটাস: ${if (esc.status == "RELEASED") "রিলিজ হয়েছে (RELEASED)" else "সুরক্ষিত (HELD)"}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (esc.status == "RELEASED") SomadhanSuccess else SomadhanOrange
                                    )
                                    StatusBadge(status = esc.status)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("মূল বিড (Base):", fontSize = 11.sp, color = SomadhanTextSecondary)
                                    Text("৳ ${esc.baseAmount.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                                }
                                if (esc.extraAmount > 0.0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("অতিরিক্ত বিল (Extra):", fontSize = 11.sp, color = SomadhanTextSecondary)
                                        Text("+ ৳ ${esc.extraAmount.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SomadhanOrange)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Divider(color = SomadhanDivider, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("সর্বমোট Escrow:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                                    Text("৳ ${totalEscrow.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanSuccess)
                                }
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "এই সমস্যার জন্য এখনো কোনো Escrow ফান্ড গঠিত হয়নি (কাজের বিড গ্রহণ হলে গঠিত হবে)।",
                                fontSize = 11.sp,
                                color = SomadhanTextHint,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = SomadhanDivider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Additional Charges List
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "অতিরিক্ত বিলের আবেদনসমূহ (${additionalCharges.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SomadhanTextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    if (additionalCharges.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "কোনো অতিরিক্ত বিলের আবেদন নেই।",
                                fontSize = 11.sp,
                                color = SomadhanTextHint,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            additionalCharges.forEach { charge ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            when (charge.status) {
                                                "ACCEPTED" -> SomadhanSuccess.copy(alpha = 0.3f)
                                                "REJECTED" -> SomadhanError.copy(alpha = 0.3f)
                                                else -> SomadhanOrange.copy(alpha = 0.3f)
                                            },
                                            RoundedCornerShape(8.dp)
                                        )
                                 ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "৳ ${charge.amount.toInt()}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = SomadhanOrange
                                            )
                                            StatusBadge(status = charge.status)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "কারণ: ${charge.reason}",
                                            fontSize = 11.sp,
                                            color = SomadhanTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "আবেদনের সময়: ${Formatters.formatDateBengali(charge.createdAt)} (${Formatters.formatTimeAgo(charge.createdAt)})",
                                            fontSize = 10.sp,
                                            color = SomadhanTextHint
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = SomadhanDivider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Admin Management Actions Section
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "অ্যাডমিন নিয়ন্ত্রণ ও অ্যাকশন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SomadhanTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    problemForStatusChange = prob
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(14.dp), tint = SomadhanOrange)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("স্ট্যাটাস পরিবর্তন", fontSize = 11.sp, color = SomadhanTextPrimary)
                            }

                            OutlinedButton(
                                onClick = {
                                    problemForBudgetEdit = prob
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = SomadhanOrange)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("বাজেট এডিট", fontSize = 11.sp, color = SomadhanTextPrimary)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    problemForBidsView = prob
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("বিডসমূহ (${prob.bidsCount})", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    problemForChatView = prob
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = SomadhanOrange)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("চ্যাট দেখুন", fontSize = 11.sp, color = SomadhanTextPrimary)
                            }
                        }

                        if (prob.status == "OPEN" || prob.status == "IN_PROGRESS") {
                            OutlinedButton(
                                onClick = {
                                    problemForSolverReassign = prob
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp), tint = SomadhanOrange)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("সলভার রিঅ্যাসাইন", fontSize = 11.sp, color = SomadhanTextPrimary)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                problemForDeleteConfirm = prob
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = SomadhanError)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("সমস্যা মুছে ফেলুন", fontSize = 11.sp, color = SomadhanError)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedProblemForDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Search & Filter Header
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("সমস্যার নাম, বিবরণ, পোস্ট আইডি, UID বা ব্যবহারকারী দিয়ে খুঁজুন...", fontSize = 12.sp, color = SomadhanTextHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SomadhanTextHint) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = SomadhanTextHint)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips
        val statusFilters = listOf(
            Pair("ALL", "সব (${problems.size})"),
            Pair("OPEN", "উন্মুক্ত (${problems.count { it.status.equals("OPEN", true) }})"),
            Pair("IN_PROGRESS", "চলমান (${problems.count { it.status.equals("IN_PROGRESS", true) }})"),
            Pair("COMPLETED", "সম্পন্ন (${problems.count { it.status.equals("COMPLETED", true) }})"),
            Pair("CANCELLED", "বাতিল (${problems.count { it.status.equals("CANCELLED", true) }})")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            statusFilters.forEach { (filterKey, filterLabel) ->
                val isSelected = selectedStatusFilter == filterKey
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) SomadhanOrange else SomadhanBg
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (isSelected) SomadhanOrange else SomadhanDivider,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedStatusFilter = filterKey }
                ) {
                    Text(
                        text = filterLabel,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) androidx.compose.ui.graphics.Color.White else SomadhanTextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredProblems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank() || selectedStatusFilter != "ALL") "কোনো সমস্যা পাওয়া যায়নি।" else "কোনো সমস্যা পোস্ট করা হয়নি।",
                    fontSize = 14.sp,
                    color = SomadhanTextHint
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(paginatedProblems, key = { it.id }) { problem ->
                    // Ground Rule ১৯ — per-item pulse (শুধু যে প্রবলেমের ডেটা সত্যিই বদলেছে তার
                    // কার্ডই action-এ pulse করবে), flashOnReentry = false (স্ক্রল-pulse এড়াতে)।
                    // Ground Rule ২০ — উপরের isFilterRefreshing OR করা হয়েছে (ফিল্টার/সার্চ/
                    // pagination বদলে সব দৃশ্যমান কার্ড একসাথে pulse করবে)।
                    val problemCardPulse = rememberFieldChangePulse(
                        value = problem,
                        isManualRefreshing = isManualRefreshing,
                        sessionKey = "admin_problems_sync",
                        viewModel = viewModel,
                        flashOnReentry = false
                    )
                    // Ground Rule ২১ — নতুন insert/status-transition হলে (recentlyChangedProblemIds)
                    // এই তৃতীয় স্বাধীন কারণেও pulse করবে, বিদ্যমান দুটো OR-এর পাশে।
                    val isNewFromRealtime = recentlyChangedProblemIds.contains(problem.id)
                    PulsingValue(isUpdating = problemCardPulse || isFilterRefreshing || isNewFromRealtime) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header Row: Title & Status
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedProblemForDetail = problem },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = problem.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ক্যাটাগরি: ${problem.categoryName}",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                                StatusBadge(status = problem.status)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Poster Name & UID (Above Budget)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "পোস্টকারী: ${problem.userName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "UID: ${allUsers.find { it.id == problem.userId }?.displayUid?.takeIf { uid -> uid.isNotBlank() } ?: problem.userId}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanOrange
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("User UID", allUsers.find { it.id == problem.userId }?.displayUid?.takeIf { uid -> uid.isNotBlank() } ?: problem.userId))
                                        Toast.makeText(context, "UID কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy UID",
                                        modifier = Modifier.size(11.dp),
                                        tint = SomadhanOrange
                                    )
                                }
                            }

                            // Post ID (Below UID)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "পোস্ট আইডি: ${problem.id}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1D4ED8)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Post ID", problem.id))
                                        Toast.makeText(context, "পোস্ট আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy Post ID",
                                        modifier = Modifier.size(11.dp),
                                        tint = Color(0xFF1D4ED8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Budget & Exact Date-Time on Right Side of Budget
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "বাজেট: ৳ ${DistanceUtil.toBengaliDigits(problem.minBudget.toInt().toString())} - ৳ ${DistanceUtil.toBengaliDigits(problem.maxBudget.toInt().toString())}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )

                                Text(
                                    text = Formatters.formatDateTimeBengali(problem.createdAt),
                                    fontSize = 10.sp,
                                    color = SomadhanTextHint,
                                    maxLines = 1
                                )
                            }

                            if (!problem.acceptedSolverName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(12.dp), tint = SomadhanInfo)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "সমাধানকারী: ${problem.acceptedSolverName}${if (!problem.acceptedSolverId.isNullOrBlank()) " (UID: ${allUsers.find { it.id == problem.acceptedSolverId }?.displayUid?.takeIf { uid -> uid.isNotBlank() } ?: problem.acceptedSolverId})" else ""}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SomadhanInfo,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = SomadhanDivider, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Action Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // View Bids button
                                    OutlinedButton(
                                        onClick = { problemForBidsView = problem },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(12.dp), tint = SomadhanOrange)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("বিড (${problem.bidsCount})", fontSize = 10.sp, color = SomadhanTextPrimary)
                                    }

                                    // View Chat button
                                    OutlinedButton(
                                        onClick = { problemForChatView = problem },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(12.dp), tint = SomadhanOrange)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("চ্যাট", fontSize = 10.sp, color = SomadhanTextPrimary)
                                    }

                                    // Edit Budget button
                                    OutlinedButton(
                                        onClick = { problemForBudgetEdit = problem },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = SomadhanOrange)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("বাজেট", fontSize = 10.sp, color = SomadhanTextPrimary)
                                    }

                                    // Update Status button
                                    OutlinedButton(
                                        onClick = { problemForStatusChange = problem },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(12.dp), tint = SomadhanOrange)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("স্ট্যাটাস", fontSize = 10.sp, color = SomadhanTextPrimary)
                                    }

                                    // Reassign Solver button (for OPEN or IN_PROGRESS)
                                    if (problem.status == "OPEN" || problem.status == "IN_PROGRESS") {
                                        OutlinedButton(
                                            onClick = { problemForSolverReassign = problem },
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(12.dp), tint = SomadhanOrange)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("সলভার", fontSize = 10.sp, color = SomadhanTextPrimary)
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { selectedProblemForDetail = problem },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = "বিস্তারিত", tint = SomadhanInfo, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { problemForDeleteConfirm = problem },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "মুছে ফেলুন", tint = SomadhanError, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                    } // PulsingValue (প্রবলেম কার্ড) বন্ধ
                }
            }

            // Pagination Controls at bottom
            if (filteredProblems.isNotEmpty()) {
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
}

@Composable
fun AdminUpdateStatusDialog(
    problem: com.example.data.entity.ProblemEntity,
    onUpdateStatus: (newStatus: String) -> Unit,
    onDismiss: () -> Unit
) {
    val statuses = listOf(
        Pair("OPEN", "উন্মুক্ত / বিডিং চলমান (OPEN)"),
        Pair("IN_PROGRESS", "কাজ প্রক্রিয়াধীন (IN_PROGRESS)"),
        Pair("COMPLETED", "কাজ সম্পন্ন (COMPLETED)"),
        Pair("CANCELLED", "বাতিল (CANCELLED)")
    )
    var selectedStatus by remember { mutableStateOf(problem.status) }
    var showConfirmStep by remember { mutableStateOf(false) }

    if (showConfirmStep) {
        BottomSlideAlertDialog(
            onDismissRequest = { showConfirmStep = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = SomadhanOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("স্ট্যাটাস পরিবর্তনের নিশ্চিতকরণ", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে '${problem.title}' সমস্যার স্ট্যাটাস '${selectedStatus}'-এ পরিবর্তন করতে চান?",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateStatus(selectedStatus)
                        showConfirmStep = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("হ্যাঁ, পরিবর্তন করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmStep = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    } else {
        BottomSlideAlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assignment, contentDescription = null, tint = SomadhanOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("স্ট্যাটাস পরিবর্তন করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "সমস্যা: ${problem.title}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanTextSecondary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "নতুন স্ট্যাটাস নির্বাচন করুন:",
                        fontSize = 12.sp,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    statuses.forEach { (statusKey, statusLabel) ->
                        val isSelected = selectedStatus == statusKey
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SomadhanOrangeLight else SomadhanBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .border(
                                    1.dp,
                                    if (isSelected) SomadhanOrange else SomadhanDivider,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedStatus = statusKey }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedStatus = statusKey },
                                        colors = RadioButtonDefaults.colors(selectedColor = SomadhanOrange)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = statusLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) SomadhanOrange else SomadhanTextPrimary
                                    )
                                }
                                StatusBadge(status = statusKey)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedStatus != problem.status) {
                            showConfirmStep = true
                        } else {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("এগিয়ে যান")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }
}

@Composable
fun AdminEditBudgetDialog(
    problem: com.example.data.entity.ProblemEntity,
    onUpdateBudget: (minBudget: Double, maxBudget: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var minBudgetInput by remember { mutableStateOf(problem.minBudget.toInt().toString()) }
    var maxBudgetInput by remember { mutableStateOf(problem.maxBudget.toInt().toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    BottomSlideAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SomadhanOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text("বাজেট এডিট করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary)
            }
        },
        text = {
            Column {
                Text(
                    text = "সমস্যা: ${problem.title}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SomadhanTextSecondary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = minBudgetInput,
                    onValueChange = {
                        minBudgetInput = it
                        errorMsg = null
                    },
                    label = { Text("সর্বনিম্ন বাজেট (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = maxBudgetInput,
                    onValueChange = {
                        maxBudgetInput = it
                        errorMsg = null
                    },
                    label = { Text("সর্বোচ্চ বাজেট (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMsg!!,
                        fontSize = 11.sp,
                        color = SomadhanError
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minVal = minBudgetInput.trim().toDoubleOrNull()
                    val maxVal = maxBudgetInput.trim().toDoubleOrNull()
                    if (minVal == null || maxVal == null || minVal < 0 || maxVal < 0) {
                        errorMsg = "অনুগ্রহ করে সঠিক বাজেট সংখ্যা লিখুন"
                    } else if (minVal > maxVal) {
                        errorMsg = "সর্বনিম্ন বাজেট সর্বোচ্চ বাজেটের চেয়ে বেশি হতে পারে না"
                    } else {
                        onUpdateBudget(minVal, maxVal)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("সংরক্ষণ করুন")
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
fun AdminReassignSolverDialog(
    problem: com.example.data.entity.ProblemEntity,
    allUsers: List<com.example.data.entity.UserEntity>,
    onReassign: (solverId: String, solverName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val solvers = remember(allUsers, searchQuery) {
        val query = searchQuery.trim().lowercase()
        allUsers.filter { (it.role == "SOLVER" || it.hasSolverRole) && !it.isBannedSolver }
            .filter { solver ->
                if (query.isBlank()) true
                else solver.name.lowercase().contains(query) || solver.phone.contains(query) || solver.solverCategories.lowercase().contains(query)
            }
    }

    var selectedSolver by remember { mutableStateOf<com.example.data.entity.UserEntity?>(null) }

    BottomSlideAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Engineering, contentDescription = null, tint = SomadhanOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("সমাধানকারী পুনর্নির্ধারণ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary)
                    Text(problem.title, fontSize = 11.sp, color = SomadhanTextSecondary, maxLines = 1)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                if (!problem.acceptedSolverName.isNullOrBlank()) {
                    Text(
                        text = "বর্তমান সমাধানকারী: ${problem.acceptedSolverName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanOrange,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("সলভারের নাম বা ফোন দিয়ে খুঁজুন...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SomadhanTextHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (solvers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("কোনো সক্রিয় সমাধানকারী পাওয়া যায়নি।", fontSize = 12.sp, color = SomadhanTextHint)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(solvers, key = { it.id }) { solver ->
                            val isSelected = selectedSolver?.id == solver.id || (selectedSolver == null && solver.id == problem.acceptedSolverId)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) SomadhanOrangeLight else SomadhanBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isSelected) SomadhanOrange else SomadhanDivider,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedSolver = solver }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(solver.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                                            if (solver.verifiedBadgeSolver) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Default.Verified, contentDescription = null, tint = SomadhanInfo, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        // [BALANCE_REPUTATION_ROLE_SEPARATION ধাপ ৭খ, ROLE_SEPARATION_AUDIT.md]
                                        // এই কার্ডে ঠিক ওপরেই `solver.verifiedBadgeSolver` (role-scoped) পড়া
                                        // হচ্ছে — এই তালিকার প্রতিটা এন্ট্রি অনস্বীকার্যভাবে "সলভার" প্রেক্ষাপটে
                                        // (bid দেওয়া সলভারদের তালিকা), তাই শেয়ার্ড `reputationScore`-এর বদলে
                                        // role-scoped `reputationScoreSolver` — dual-role ইউজারের User-role
                                        // স্কোর এখানে আর দেখাবে না।
                                        Text("ফোন: ${Formatters.toLocalDisplayFormat(solver.phone)} | স্কোর: ${solver.reputationScoreSolver.toInt()}", fontSize = 11.sp, color = SomadhanTextSecondary)
                                        if (solver.solverCategories.isNotBlank()) {
                                            Text("ক্যাটাগরি: ${solver.solverCategories}", fontSize = 10.sp, color = SomadhanTextHint, maxLines = 1)
                                        }
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedSolver = solver },
                                        colors = RadioButtonDefaults.colors(selectedColor = SomadhanOrange)
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
                    val chosen = selectedSolver
                    if (chosen != null) {
                        onReassign(chosen.id, chosen.name)
                        onDismiss()
                    }
                },
                enabled = selectedSolver != null,
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("অ্যাসাইন নিশ্চিত করুন")
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
fun AdminProblemBidsDialog(
    problem: com.example.data.entity.ProblemEntity,
    viewModel: SomadhanViewModel,
    onDismiss: () -> Unit
) {
    val bids by viewModel.getBidsForProblem(problem.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var bidToRejectConfirm by remember { mutableStateOf<com.example.data.entity.BidEntity?>(null) }

    if (bidToRejectConfirm != null) {
        val targetBid = bidToRejectConfirm!!
        BottomSlideAlertDialog(
            onDismissRequest = { bidToRejectConfirm = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Clear, contentDescription = null, tint = SomadhanError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("বিড বাতিল নিশ্চিতকরণ", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে '${targetBid.solverName}'-এর ৳ ${targetBid.amount.toInt()} মূল্যের বিডটি বাতিল (Reject) করতে চান?",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminRejectBid(targetBid.id)
                        bidToRejectConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("বাতিল করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { bidToRejectConfirm = null }) {
                    Text("ফিরে যান", color = SomadhanTextSecondary)
                }
            }
        )
    }

    BottomSlideAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SomadhanOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("বিডসমূহ (${bids.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary)
                        Text(problem.title, fontSize = 11.sp, color = SomadhanTextSecondary, maxLines = 1)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                if (bids.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "এই সমস্যার জন্য এখনো কোনো বিড জমা পড়েনি।",
                            fontSize = 13.sp,
                            color = SomadhanTextHint
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bids, key = { it.id }) { bid ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        when (bid.status) {
                                            "ACCEPTED" -> SomadhanSuccess.copy(alpha = 0.4f)
                                            "REJECTED" -> SomadhanError.copy(alpha = 0.3f)
                                            else -> SomadhanDivider
                                        },
                                        RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = bid.solverName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = SomadhanTextPrimary
                                            )
                                            if (!bid.solverPhone.isNullOrBlank()) {
                                                Text(
                                                    text = "ফোন: ${bid.solverPhone}",
                                                    fontSize = 10.sp,
                                                    color = SomadhanTextHint
                                                )
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "৳ ${bid.amount.toInt()}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = SomadhanOrange
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            StatusBadge(status = bid.status)
                                        }
                                    }

                                    if (bid.message.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "বার্তা: ${bid.message}",
                                            fontSize = 11.sp,
                                            color = SomadhanTextSecondary,
                                            lineHeight = 15.sp
                                        )
                                    }

                                    if (bid.estimatedTime.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "আনুমানিক সময়: ${bid.estimatedTime}",
                                            fontSize = 10.sp,
                                            color = SomadhanTextHint
                                        )
                                    }

                                    if (bid.status == "PENDING") {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Divider(color = SomadhanDivider, thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { bidToRejectConfirm = bid },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = SomadhanError)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("বিড রিজেক্ট করুন", fontSize = 11.sp, color = SomadhanError)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("বন্ধ করুন")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProblemChatBottomSheet(
    problem: com.example.data.entity.ProblemEntity,
    viewModel: SomadhanViewModel,
    onDismiss: () -> Unit
) {
    val messages by viewModel.getMessagesForProblem(problem.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var messageToDeleteConfirm by remember { mutableStateOf<MessageEntity?>(null) }

    if (messageToDeleteConfirm != null) {
        val msg = messageToDeleteConfirm!!
        BottomSlideAlertDialog(
            onDismissRequest = { messageToDeleteConfirm = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = SomadhanError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("মেসেজ মুছে ফেলা নিশ্চিতকরণ", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে '${msg.senderName}'-এর এই মেসেজটি মুছে ফেলতে চান?",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminDeleteMessage(msg.id)
                        messageToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDeleteConfirm = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SomadhanBg,
        contentColor = SomadhanTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = SomadhanOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "চ্যাট হিস্ট্রি (${DistanceUtil.toBengaliDigits(messages.size.toString())})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = problem.title,
                            fontSize = 11.sp,
                            color = SomadhanTextSecondary,
                            maxLines = 1
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = SomadhanTextHint)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = SomadhanDivider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = SomadhanTextHint, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "এই সমস্যার জন্য কোনো মেসেজ বা চ্যাট কথোপকথন নেই।",
                            fontSize = 13.sp,
                            color = SomadhanTextHint
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isUser = msg.senderId == problem.userId
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = msg.senderName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = SomadhanTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        RoleBadge(role = if (isUser) "USER" else "SOLVER")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = Formatters.formatTimeAgo(msg.timestamp),
                                            fontSize = 10.sp,
                                            color = SomadhanTextHint
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.content,
                                        fontSize = 12.sp,
                                        color = SomadhanTextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                                IconButton(
                                    onClick = { messageToDeleteConfirm = msg },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "মেসেজ মুছুন",
                                        tint = SomadhanError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
