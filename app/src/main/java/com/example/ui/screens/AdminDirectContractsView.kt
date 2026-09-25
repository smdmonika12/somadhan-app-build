@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.example.util.FileAttachmentUtil
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.MessageEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.UserEntity
import com.example.ui.components.PulsingValue
import com.example.ui.components.rememberFieldChangePulse
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
import com.example.util.DistanceUtil
import com.example.util.Formatters
import com.example.ui.components.BottomSlideAlertDialog
import com.example.ui.components.BottomSlideDialog

@Composable
fun AdminDirectContractsView(
    problems: List<ProblemEntity>,
    allUsers: List<UserEntity>,
    viewModel: SomadhanViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Collect fresh real-time data directly from ViewModel flows
    val liveProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val heldEscrows by viewModel.allHeldEscrows.collectAsStateWithLifecycle()
    val allAdminMessages by viewModel.allAdminMessages.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // [বাগফিক্স] আগে এখানে LaunchedEffect(Unit) { viewModel.triggerCloudSync() } ছিল —
    // এটাই এই স্ক্রিনে ঢোকা মাত্র endless reload/shimmer বাগের root cause ছিল। অ্যাপের আর
    // কোনো admin ট্যাবেই এই auto-trigger নেই। triggerCloudSync() একটা ভারী, পুরো-অ্যাপ-জোড়া
    // অপারেশন (local DB → Supabase push, পুরো bulk re-pull, **সব realtime চ্যানেল restart**,
    // admin metrics refresh) — এটা টপ-বারের সিঙ্ক বাটনের জন্য বানানো (ম্যানুয়াল ফোর্স-সিঙ্ক),
    // প্রতিবার এই ট্যাবে ঢোকার সাথে সাথে না। এই স্ক্রিনের cold-load ইতিমধ্যেই SyncAwareContent
    // (AdminPanelScreen-এর sessionKey="admin_direct_contracts_sync") হ্যান্ডেল করে বলে এই
    // এক্সট্রা কলটা অপ্রয়োজনীয়ও ছিল। ম্যানুয়াল সিঙ্ক বাটন (নিচে) অক্ষত আছে, দরকার হলে অ্যাডমিন
    // নিজেই চাপতে পারবেন।

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatusFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, PENDING, IN_PROGRESS, COMPLETED, CANCELLED, ESCROW_HELD
    var currentPage by rememberSaveable { mutableIntStateOf(1) }
    val pageSize = 10

    // Reset pagination on search or filter change
    LaunchedEffect(searchQuery, selectedStatusFilter) {
        currentPage = 1
    }

    // Ground Rule ২০, সেশন ২.২২ — ফিল্টার/সার্চ/পেজ বদলে দৃশ্যমান সব ডাইরেক্ট-কন্ট্রাক্ট কার্ড একসাথে
    // ছোট্ট করে pulse করবে (Withdrawal/Problems/Transactions-এর একই প্যাটার্ন)। currentPage
    // ইচ্ছাকৃতভাবে key-তে আছে।
    var isFilterRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(searchQuery, selectedStatusFilter, currentPage) {
        isFilterRefreshing = true
        try {
            kotlinx.coroutines.delay(350L)
        } finally {
            isFilterRefreshing = false
        }
    }

    // Ground Rule ২১, সেশন ২.২২ — সরাসরি চুক্তিও `problems` টেবিলেরই সারি বলে বিদ্যমান
    // recentlyChangedProblemIds (Insert-only, Problems ট্যাবের সাথে শেয়ার্ড) পুনর্ব্যবহার করা
    // হয়েছে, নতুন কোনো ব্যাকএন্ড state লাগেনি।
    val recentlyChangedProblemIds by viewModel.recentlyChangedProblemIds.collectAsStateWithLifecycle()

    // Dialog States
    var selectedChatProblemId by remember { mutableStateOf<String?>(null) }
    var contractForDetails by remember { mutableStateOf<ProblemEntity?>(null) }
    var contractForStatusChange by remember { mutableStateOf<ProblemEntity?>(null) }
    var contractForRefund by remember { mutableStateOf<ProblemEntity?>(null) }
    var refundReason by remember { mutableStateOf("") }
    var contractForDelete by remember { mutableStateOf<ProblemEntity?>(null) }
    var contractForReleasePayment by remember { mutableStateOf<ProblemEntity?>(null) }
    var contractForEscalate by remember { mutableStateOf<ProblemEntity?>(null) }
    var escalateNote by remember { mutableStateOf("") }
    var adminMessageText by remember { mutableStateOf("") }
    var isSendingAdminMessage by remember { mutableStateOf(false) }
    var adminMessageError by remember { mutableStateOf<String?>(null) }
    var messageToDelete by remember { mutableStateOf<MessageEntity?>(null) }
    var chatMessageLimit by remember { mutableIntStateOf(30) }

    // Direct contracts list (merging props and live flow to always ensure instant update)
    val effectiveProblems = if (liveProblems.isNotEmpty()) liveProblems else problems
    val directContracts = remember(effectiveProblems) {
        effectiveProblems.filter { it.isDirectContract }
    }

    val filteredContracts = remember(directContracts, searchQuery, selectedStatusFilter, allUsers, heldEscrows, allAdminMessages) {
        val query = searchQuery.trim().lowercase()
        directContracts.filter { contract ->
            val client = allUsers.find { it.id == contract.userId }
            val solver = allUsers.find { it.id == contract.acceptedSolverId }
            val contractEscrows = heldEscrows.filter { it.problemId == contract.id }
            val contractMessages = allAdminMessages.filter { it.problemId == contract.id }

            val matchesSearch = if (query.isBlank()) true else {
                contract.id.lowercase().contains(query) ||
                        contract.title.lowercase().contains(query) ||
                        contract.userId.lowercase().contains(query) ||
                        contract.userName.lowercase().contains(query) ||
                        (client?.phone?.contains(query) == true) ||
                        (contract.acceptedSolverId?.lowercase()?.contains(query) == true) ||
                        (contract.acceptedSolverName?.lowercase()?.contains(query) == true) ||
                        (solver?.phone?.contains(query) == true) ||
                        contract.categoryName.lowercase().contains(query) ||
                        contract.description.lowercase().contains(query) ||
                        contractMessages.any { it.content.lowercase().contains(query) }
            }

            val matchesStatus = when (selectedStatusFilter) {
                "PENDING" -> contract.directContractStatus == "PENDING_ACCEPTANCE" || (contract.status == "OPEN" && contract.directContractStatus != "ACCEPTED")
                "IN_PROGRESS" -> contract.directContractStatus == "ACCEPTED" && contract.status != "COMPLETED" && contract.status != "CANCELLED"
                "COMPLETED" -> contract.status == "COMPLETED"
                "CANCELLED" -> contract.status == "CANCELLED" || contract.directContractStatus == "DECLINED"
                "ESCROW_HELD" -> contractEscrows.isNotEmpty()
                else -> true
            }

            matchesSearch && matchesStatus
        }.sortedByDescending { it.createdAt }
    }

    val totalCount = directContracts.size
    val pendingCount = directContracts.count { it.directContractStatus == "PENDING_ACCEPTANCE" || (it.status == "OPEN" && it.directContractStatus != "ACCEPTED") }
    val inProgressCount = directContracts.count { it.directContractStatus == "ACCEPTED" && it.status != "COMPLETED" && it.status != "CANCELLED" }
    val completedCount = directContracts.count { it.status == "COMPLETED" }
    val cancelledCount = directContracts.count { it.status == "CANCELLED" || it.directContractStatus == "DECLINED" }
    val escrowHeldCount = directContracts.count { c -> heldEscrows.any { it.problemId == c.id } }
    val totalVolume = directContracts.filter { it.status == "COMPLETED" }.sumOf { it.acceptedAmount ?: it.minBudget }

    // Pagination calculations
    val totalPages = if (filteredContracts.isEmpty()) 1 else ((filteredContracts.size - 1) / pageSize) + 1
    val safePage = currentPage.coerceIn(1, totalPages)
    val startIndex = (safePage - 1) * pageSize
    val displayedContracts = remember(filteredContracts, safePage) {
        filteredContracts.drop(startIndex).take(pageSize)
    }

    val listState = rememberLazyListState()
    // Ground Rule ২০, সেশন ২.২২ — scroll-jump ফিক্স: coerced safePage-এর বদলে raw currentPage-এ key
    // করা হয়েছে, যাতে ডিলিট-এ totalPages কমে গিয়ে safePage automatically ক্ল্যাম্প হলে ভুলভাবে
    // scroll-to-top ট্রিগার না হয় (Withdrawal/Users/KYC-এর একই ফিক্স)।
    LaunchedEffect(currentPage) {
        listState.scrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header Title & Sync Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🤝 সরাসরি চুক্তি (Direct Contracts)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SomadhanOrangeLight)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${DistanceUtil.toBengaliDigits(totalCount.toString())} টি প্রজেক্ট",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanOrange
                        )
                    }
                }
                Text(
                    text = "P2P সরাসরি নিয়োগকৃত কাজের পূর্ণ মনিটরিং, এস্ক্রো ও অ্যাডমিন কন্ট্রোল",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary
                )
            }

            IconButton(
                onClick = { viewModel.triggerCloudSync() },
                enabled = !isRefreshing,
                modifier = Modifier.size(36.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SomadhanOrange, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = "রিয়েল-টাইম সিঙ্ক", tint = SomadhanOrange)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SomadhanCardBg)
                    .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(text = "চলমান চুক্তি", fontSize = 11.sp, color = SomadhanTextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${DistanceUtil.toBengaliDigits(inProgressCount.toString())}টি",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SomadhanCardBg)
                    .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(text = "এস্ক্রো সুরক্ষিত", fontSize = 11.sp, color = SomadhanTextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${DistanceUtil.toBengaliDigits(escrowHeldCount.toString())}টি",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanSuccess
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SomadhanCardBg)
                    .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(text = "সম্পন্ন ভলিউম", fontSize = 11.sp, color = SomadhanTextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "৳${DistanceUtil.toBengaliDigits(totalVolume.toInt().toString())}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanSuccess
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar with Multi-field support
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("চুক্তি আইডি (#PROB...), ক্লায়েন্ট, সলভার বা বিষয়বস্তু খুঁজুন...", fontSize = 12.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = SomadhanTextHint, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = SomadhanTextHint, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SomadhanOrange,
                unfocusedBorderColor = SomadhanBorder,
                focusedContainerColor = SomadhanCardBg,
                unfocusedContainerColor = SomadhanCardBg
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status Filter Chips with exact counts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedStatusFilter == "ALL",
                onClick = { selectedStatusFilter = "ALL" },
                label = { Text("সব (${totalCount})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SomadhanOrange,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedStatusFilter == "IN_PROGRESS",
                onClick = { selectedStatusFilter = "IN_PROGRESS" },
                label = { Text("চলমান (${inProgressCount})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF2563EB),
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedStatusFilter == "PENDING",
                onClick = { selectedStatusFilter = "PENDING" },
                label = { Text("অপেক্ষারত (${pendingCount})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SomadhanOrange,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedStatusFilter == "ESCROW_HELD",
                onClick = { selectedStatusFilter = "ESCROW_HELD" },
                label = { Text("🛡️ এস্ক্রো জমা (${escrowHeldCount})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SomadhanSuccess,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedStatusFilter == "COMPLETED",
                onClick = { selectedStatusFilter = "COMPLETED" },
                label = { Text("সম্পন্ন (${completedCount})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SomadhanSuccess,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedStatusFilter == "CANCELLED",
                onClick = { selectedStatusFilter = "CANCELLED" },
                label = { Text("বাতিল (${cancelledCount})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SomadhanError,
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Direct Contracts List
        if (filteredContracts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "কোনো ফলাফল পাওয়া যায়নি" else "কোনো সরাসরি চুক্তি পাওয়া যায়নি",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SomadhanTextHint
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedContracts, key = { it.id }) { contract ->
                    val client = allUsers.find { it.id == contract.userId }
                    val solver = allUsers.find { it.id == contract.acceptedSolverId }
                    val contractEscrows = heldEscrows.filter { it.problemId == contract.id }
                    val hasEscrow = contractEscrows.isNotEmpty()
                    val escrowTotal = contractEscrows.sumOf { it.baseAmount + it.extraAmount }
                    val amount = contract.acceptedAmount ?: contract.minBudget

                    // Ground Rule ১৯ — per-item pulse (শুধু যে কন্ট্রাক্টের ডেটা সত্যিই বদলেছে তার
                    // কার্ডই action-এ pulse করবে), flashOnReentry = false। Ground Rule ২০ —
                    // isFilterRefreshing OR করা। Ground Rule ২১ — recentlyChangedProblemIds
                    // (genuine নতুন Insert)।
                    val contractCardPulse = rememberFieldChangePulse(
                        value = contract,
                        isManualRefreshing = isRefreshing,
                        sessionKey = "admin_direct_contracts_sync",
                        viewModel = viewModel,
                        flashOnReentry = false
                    )
                    val isNewFromRealtime = recentlyChangedProblemIds.contains(contract.id)
                    PulsingValue(isUpdating = contractCardPulse || isFilterRefreshing || isNewFromRealtime) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = SomadhanDivider,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Top Post/Contract ID Banner & Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SomadhanBg)
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(contract.id))
                                            Toast.makeText(context, "চুক্তি আইডি কপি হয়েছে: ${contract.id}", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "🆔 পোস্ট/চুক্তি আইডি: #${contract.id}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrange
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "কপি করুন",
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }

                                // Status Badge
                                val (statusBg, statusFg, statusText) = when {
                                    contract.status == "COMPLETED" -> Triple(SomadhanSuccessLight, SomadhanSuccess, "সম্পন্ন")
                                    contract.status == "CANCELLED" || contract.directContractStatus == "DECLINED" -> Triple(SomadhanErrorLight, SomadhanError, "বাতিলকৃত")
                                    contract.directContractStatus == "ACCEPTED" -> Triple(Color(0xFFDBEAFE), Color(0xFF1E40AF), "গৃহীত ও চলমান")
                                    else -> Triple(SomadhanOrangeLight, SomadhanOrange, "অপেক্ষারত")
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusBg)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusFg
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Contract Title & Category
                            Text(
                                text = contract.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SomadhanTextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ক্যাটাগরি: ${contract.categoryName}",
                                    fontSize = 11.sp,
                                    color = SomadhanTextHint
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• তৈরি: ${Formatters.formatDateTimeBengali(contract.createdAt)} (${Formatters.formatTimeAgo(contract.createdAt)})",
                                    fontSize = 10.sp,
                                    color = SomadhanTextHint
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Parties: Client <-> Solver Cards
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SomadhanBg)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Client Info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "👤 ক্লায়েন্ট (গ্রাহক):",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanTextHint
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = client?.name ?: contract.userName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                    val clientPhone = client?.phone?.let { Formatters.toLocalDisplayFormat(it) } ?: ""
                                    if (clientPhone.isNotBlank()) {
                                        Text(
                                            text = clientPhone,
                                            fontSize = 11.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }
                                    Text(
                                        text = "ID: ${contract.userId.take(8)}...",
                                        fontSize = 9.sp,
                                        color = SomadhanTextHint
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Solver Info
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "🛠️ মনোনীত সমাধানকারী:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanTextHint
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = solver?.name ?: contract.acceptedSolverName ?: "অনির্ধারিত / অপেক্ষারত",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrange
                                    )
                                    val solverPhone = solver?.phone?.let { Formatters.toLocalDisplayFormat(it) } ?: ""
                                    if (solverPhone.isNotBlank()) {
                                        Text(
                                            text = solverPhone,
                                            fontSize = 11.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }
                                    if (!contract.acceptedSolverId.isNullOrBlank()) {
                                        Text(
                                            text = "ID: ${contract.acceptedSolverId.take(8)}...",
                                            fontSize = 9.sp,
                                            color = SomadhanTextHint
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Financials, Duration & Escrow Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = SomadhanSuccess,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "বাজেট: ৳${DistanceUtil.toBengaliDigits(amount.toInt().toString())}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                }

                                if (contract.deadline.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = SomadhanTextSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "সময়: ${contract.deadline}",
                                            fontSize = 11.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }
                                }

                                if (hasEscrow) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SomadhanSuccessLight)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "🛡️ Escrow: ৳${DistanceUtil.toBengaliDigits(escrowTotal.toInt().toString())}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanSuccess
                                        )
                                    }
                                }
                            }

                            if (contract.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "বিবরণ: ${contract.description}",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = SomadhanDivider, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Details Button
                                OutlinedButton(
                                    onClick = { contractForDetails = contract },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanTextSecondary),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("বিস্তারিত", fontSize = 10.sp)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Chat Monitor Button (Support Manager View)
                                    OutlinedButton(
                                        onClick = {
                                            selectedChatProblemId = contract.id
                                            chatMessageLimit = 30
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4F46E5)),
                                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("চ্যাট মনিটর", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Change Status Button
                                    OutlinedButton(
                                        onClick = { contractForStatusChange = contract },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanTextPrimary),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("স্ট্যাটাস", fontSize = 10.sp)
                                    }

                                    // Complete & Release Payment to Solver
                                    if (contract.status != "CANCELLED" && contract.status != "COMPLETED") {
                                        Button(
                                            onClick = { contractForReleasePayment = contract },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SomadhanSuccessLight,
                                                contentColor = SomadhanSuccess
                                            ),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("রিলিজ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Cancel & Refund
                                    if (contract.status != "CANCELLED" && contract.status != "COMPLETED") {
                                        Button(
                                            onClick = {
                                                contractForRefund = contract
                                                refundReason = ""
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SomadhanErrorLight,
                                                contentColor = SomadhanError
                                            ),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("রিফান্ড", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Escalate for Investigation (অ্যাডমিন Action bug-fix master prompt, ধাপ ৯)
                                    IconButton(
                                        onClick = {
                                            contractForEscalate = contract
                                            escalateNote = ""
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Flag, contentDescription = "এসকেলেট করুন", tint = SomadhanOrange, modifier = Modifier.size(14.dp))
                                    }

                                    // Delete Contract
                                    IconButton(
                                        onClick = { contractForDelete = contract },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "মুছে ফেলুন", tint = SomadhanError, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                    } // close PulsingValue (Ground Rule ১৯/২০/২১, সেশন ২.২২)
                }
            }

            // Pagination Controls
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { if (safePage > 1) currentPage = safePage - 1 },
                    enabled = safePage > 1,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পূর্ববর্তী", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("পূর্ববর্তী", fontSize = 11.sp)
                }

                Text(
                    text = "পৃষ্ঠা ${DistanceUtil.toBengaliDigits(safePage.toString())} / ${DistanceUtil.toBengaliDigits(totalPages.toString())} (মোট ${DistanceUtil.toBengaliDigits(filteredContracts.size.toString())} টি)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SomadhanTextSecondary
                )

                OutlinedButton(
                    onClick = { if (safePage < totalPages) currentPage = safePage + 1 },
                    enabled = safePage < totalPages,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("পরবর্তী", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "পরবর্তী", modifier = Modifier.size(14.dp))
                }
            }
        }
    }

    // Modal Dialog: Chat Monitor & Support Manager Intervention
    selectedChatProblemId?.let { problemId ->
        val problem = effectiveProblems.find { it.id == problemId }
        val allMsgs = allAdminMessages.filter { it.problemId == problemId }.sortedBy { it.timestamp }
        val displayedMsgs = allMsgs.takeLast(chatMessageLimit)

        BottomSlideDialog(onDismissRequest = { selectedChatProblemId = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = SomadhanBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "💬 চুক্তি চ্যাট মনিটর",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = SomadhanTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF312E81))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Support Manager 🛡️",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFBBF24)
                                    )
                                }
                            }
                            Text(
                                text = "${problem?.userName ?: "ক্লায়েন্ট"} ↔ ${problem?.acceptedSolverName ?: "সমাধানকারী"} • মোট বার্তা: ${DistanceUtil.toBengaliDigits(allMsgs.size.toString())}টি",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { selectedChatProblemId = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "বন্ধ করুন")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Divider(color = SomadhanDivider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Messages List
                    if (allMsgs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("এই চুক্তির কোনো চ্যাট বার্তা পাওয়া যায়নি", fontSize = 13.sp, color = SomadhanTextHint)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (allMsgs.size > chatMessageLimit) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TextButton(onClick = { chatMessageLimit += 40 }) {
                                            Text(
                                                text = "⬆️ আরও পুরোনো বার্তা লোড করুন (${allMsgs.size - chatMessageLimit} টি বাকি)",
                                                fontSize = 11.sp,
                                                color = SomadhanOrange,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            items(displayedMsgs, key = { it.id }) { msg ->
                                val isAdmin = msg.isAdminMessage ||
                                        msg.senderName.contains("Support Manager", ignoreCase = true) ||
                                        msg.senderName.contains("সাপোর্ট ম্যানেজার", ignoreCase = true) ||
                                        msg.senderName.contains("Admin", ignoreCase = true) ||
                                        msg.senderName.contains("অ্যাডমিন", ignoreCase = true) ||
                                        msg.senderId.startsWith("ADMIN", ignoreCase = true)

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
                                                        contentDescription = "মুছুন",
                                                        tint = if (isAdmin) Color(0xFFF87171) else SomadhanError,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
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
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = null,
                                                    tint = if (isAdmin) Color(0xFFFBBF24) else SomadhanOrange,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "সংযুক্ত: ${msg.fileName ?: "ফাইল খুলুন"}",
                                                    fontSize = 10.sp,
                                                    color = if (isAdmin) Color.White else SomadhanOrange,
                                                    fontWeight = FontWeight.SemiBold,
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
                    }

                    // Admin Chat Message Intervention input as Support Manager
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = adminMessageText,
                            onValueChange = { adminMessageText = it },
                            placeholder = { Text("সাপোর্ট ম্যানেজার হিসেবে বার্তা দিন...", fontSize = 11.sp) },
                            singleLine = true,
                            enabled = !isSendingAdminMessage,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = SomadhanBorder,
                                focusedContainerColor = SomadhanCardBg,
                                unfocusedContainerColor = SomadhanCardBg
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
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
                            modifier = Modifier.height(46.dp)
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

    // Modal Dialog: Full Contract Details
    contractForDetails?.let { contract ->
        val client = allUsers.find { it.id == contract.userId }
        val solver = allUsers.find { it.id == contract.acceptedSolverId }
        val contractEscrows = heldEscrows.filter { it.problemId == contract.id }

        BottomSlideAlertDialog(
            onDismissRequest = { contractForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = SomadhanOrange)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("চুক্তির পূর্ণ বিবরণ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SomadhanBg)
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("প্রজেক্ট শিরোনাম:", fontSize = 10.sp, color = SomadhanTextHint)
                            Text(contract.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("পোস্ট আইডি: ${contract.id}", fontSize = 11.sp, color = SomadhanOrange, fontWeight = FontWeight.SemiBold)
                            Text("ক্যাটাগরি: ${contract.categoryName}", fontSize = 11.sp, color = SomadhanTextSecondary)
                            Text("তৈরির সময়: ${Formatters.formatDateTimeBengali(contract.createdAt)}", fontSize = 11.sp, color = SomadhanTextSecondary)
                            Text("স্ট্যাটাস: ${contract.status} (${contract.directContractStatus})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SomadhanOrange)
                        }
                    }

                    // Financials
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SomadhanBg)
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("আর্থিক তথ্য ও এস্ক্রো:", fontSize = 10.sp, color = SomadhanTextHint)
                            Text("বাজেট: ৳${DistanceUtil.toBengaliDigits((contract.acceptedAmount ?: contract.minBudget).toInt().toString())}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanSuccess)
                            if (contractEscrows.isNotEmpty()) {
                                Text("জমা এস্ক্রো: ৳${DistanceUtil.toBengaliDigits(contractEscrows.sumOf { it.baseAmount + it.extraAmount }.toInt().toString())} (সুরক্ষিত)", fontSize = 11.sp, color = SomadhanSuccess)
                            } else {
                                Text("এস্ক্রো স্ট্যাটাস: কোনো সক্রিয় হোল্ড এস্ক্রো নেই", fontSize = 11.sp, color = SomadhanTextHint)
                            }
                        }
                    }

                    // Parties
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SomadhanBg)
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("ক্লায়েন্ট: ${client?.name ?: contract.userName} (${client?.phone?.let { Formatters.toLocalDisplayFormat(it) } ?: "ফোন নেই"})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                            Text("ক্লায়েন্ট ইউজার আইডি: ${contract.userId}", fontSize = 10.sp, color = SomadhanTextHint)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("সমাধানকারী: ${solver?.name ?: contract.acceptedSolverName ?: "অনির্ধারিত"} (${solver?.phone?.let { Formatters.toLocalDisplayFormat(it) } ?: "ফোন নেই"})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SomadhanOrange)
                            Text("সমাধানকারী আইডি: ${contract.acceptedSolverId ?: "নেই"}", fontSize = 10.sp, color = SomadhanTextHint)
                        }
                    }

                    if (contract.description.isNotBlank()) {
                        Text("কাজের পূর্ণ বর্ণনা:", fontSize = 10.sp, color = SomadhanTextHint)
                        Text(contract.description, fontSize = 12.sp, color = SomadhanTextSecondary)
                    }

                    if (contract.userAddress.isNotBlank()) {
                        Text("লোকেশন / ঠিকানা:", fontSize = 10.sp, color = SomadhanTextHint)
                        Text(contract.userAddress, fontSize = 11.sp, color = SomadhanTextSecondary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { contractForDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange)
                ) {
                    Text("ঠিক আছে", color = Color.White)
                }
            }
        )
    }

    // Modal Dialog: Change Status
    contractForStatusChange?.let { contract ->
        var selectedStatus by remember { mutableStateOf(contract.status) }
        var isSubmittingStatusChange by remember { mutableStateOf(false) }
        var statusChangeError by remember { mutableStateOf<String?>(null) }

        BottomSlideAlertDialog(
            onDismissRequest = { if (!isSubmittingStatusChange) contractForStatusChange = null },
            title = { Text("চুক্তির স্ট্যাটাস পরিবর্তন", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column {
                    Text("বর্তমান স্ট্যাটাস: ${contract.status}", fontSize = 12.sp, color = SomadhanTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))

                    val statuses = listOf(
                        "IN_PROGRESS" to "গৃহীত ও চলমান (IN_PROGRESS)",
                        "COMPLETED" to "সম্পন্ন ও পেমেন্ট রিলিজ (COMPLETED)",
                        "PENDING" to "অপেক্ষারত (PENDING)",
                        "CANCELLED" to "বাতিলকৃত ও রিফান্ড (CANCELLED)"
                    )
                    statuses.forEach { (statKey, statLabel) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = !isSubmittingStatusChange) { selectedStatus = statKey }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = selectedStatus == statKey,
                                enabled = !isSubmittingStatusChange,
                                onClick = { selectedStatus = statKey }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = statLabel, fontSize = 12.sp, color = SomadhanTextPrimary)
                        }
                    }
                    if (statusChangeError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = statusChangeError ?: "", fontSize = 12.sp, color = SomadhanError)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val directStatus = if (selectedStatus == "COMPLETED" || selectedStatus == "IN_PROGRESS") "ACCEPTED" else if (selectedStatus == "PENDING") "PENDING_ACCEPTANCE" else "DECLINED"
                        isSubmittingStatusChange = true
                        statusChangeError = null
                        viewModel.adminUpdateDirectContractStatus(
                            problemId = contract.id,
                            status = selectedStatus,
                            directContractStatus = directStatus,
                            onSuccess = {
                                isSubmittingStatusChange = false
                                contractForStatusChange = null
                            },
                            onError = { err ->
                                isSubmittingStatusChange = false
                                statusChangeError = err
                            }
                        )
                    },
                    enabled = !isSubmittingStatusChange,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange)
                ) {
                    if (isSubmittingStatusChange) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সংরক্ষণ হচ্ছে...", color = Color.White)
                    } else {
                        Text("সংরক্ষণ করুন", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !isSubmittingStatusChange, onClick = { contractForStatusChange = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Modal Dialog: Complete & Release Payment Confirmation
    contractForReleasePayment?.let { contract ->
        var isSubmittingReleasePayment by remember { mutableStateOf(false) }
        var releasePaymentError by remember { mutableStateOf<String?>(null) }

        BottomSlideAlertDialog(
            onDismissRequest = { if (!isSubmittingReleasePayment) contractForReleasePayment = null },
            title = { Text("পেমেন্ট রিলিজ ও চুক্তি সম্পন্ন করবেন?", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column {
                    Text(
                        text = "আপনি কি নিশ্চিত যে \"${contract.title}\" চুক্তিটি সম্পন্ন করে সমাধানকারীর ওয়ালেটে এস্ক্রো অর্থ রিলিজ করতে চান?",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary
                    )
                    if (releasePaymentError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = releasePaymentError ?: "", fontSize = 12.sp, color = SomadhanError)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingReleasePayment = true
                        releasePaymentError = null
                        viewModel.adminUpdateDirectContractStatus(
                            problemId = contract.id,
                            status = "COMPLETED",
                            directContractStatus = "COMPLETED",
                            onSuccess = {
                                isSubmittingReleasePayment = false
                                contractForReleasePayment = null
                            },
                            onError = { err ->
                                isSubmittingReleasePayment = false
                                releasePaymentError = err
                            }
                        )
                    },
                    enabled = !isSubmittingReleasePayment,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess)
                ) {
                    if (isSubmittingReleasePayment) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("রিলিজ হচ্ছে...", color = Color.White)
                    } else {
                        Text("হ্যাঁ, রিলিজ ও সম্পন্ন করুন", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !isSubmittingReleasePayment, onClick = { contractForReleasePayment = null }) {
                    Text("না")
                }
            }
        )
    }

    // Modal Dialog: Cancel & Refund
    contractForRefund?.let { contract ->
        var isSubmittingRefund by remember { mutableStateOf(false) }
        var refundError by remember { mutableStateOf<String?>(null) }

        BottomSlideAlertDialog(
            onDismissRequest = { if (!isSubmittingRefund) contractForRefund = null },
            title = { Text("ডাইরেক্ট চুক্তি বাতিল ও ক্লায়েন্ট রিফান্ড", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column {
                    Text("আপনি কি \"${contract.title}\" চুক্তিটি বাতিল এবং ক্লায়েন্টকে সম্পূর্ণ অর্থ রিফান্ড করতে চান?", fontSize = 12.sp, color = SomadhanTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = refundReason,
                        onValueChange = { refundReason = it },
                        enabled = !isSubmittingRefund,
                        placeholder = { Text("বাতিল ও রিফান্ডের কারণ লিখুন...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (refundError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = refundError ?: "", fontSize = 12.sp, color = SomadhanError)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingRefund = true
                        refundError = null
                        viewModel.adminCancelAndRefundDirectContract(
                            problemId = contract.id,
                            reason = refundReason.ifBlank { "অ্যাডমিন কর্তৃক চুক্তি বাতিল ও রিফান্ড" },
                            onSuccess = {
                                isSubmittingRefund = false
                                contractForRefund = null
                            },
                            onError = { err ->
                                isSubmittingRefund = false
                                refundError = err
                            }
                        )
                    },
                    enabled = !isSubmittingRefund,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError)
                ) {
                    if (isSubmittingRefund) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("বাতিল হচ্ছে...", color = Color.White)
                    } else {
                        Text("বাতিল ও রিফান্ড করুন", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !isSubmittingRefund, onClick = { contractForRefund = null }) {
                    Text("না")
                }
            }
        )
    }

    // Modal Dialog: Escalate for Investigation (অ্যাডমিন Action bug-fix master prompt, ধাপ ৯)
    contractForEscalate?.let { contract ->
        var isSubmittingEscalate by remember { mutableStateOf(false) }
        var escalateError by remember { mutableStateOf<String?>(null) }

        BottomSlideAlertDialog(
            onDismissRequest = { if (!isSubmittingEscalate) contractForEscalate = null },
            title = { Text("তদন্তের জন্য এসকেলেট করুন", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column {
                    Text(
                        "\"${contract.title}\" চুক্তিটি ভবিষ্যতে তদন্তের জন্য ফ্ল্যাগ করা হবে (audit log-এ রেকর্ড থাকবে)। এতে চুক্তির কোনো status/টাকা পরিবর্তন হয় না।",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = escalateNote,
                        onValueChange = { escalateNote = it },
                        enabled = !isSubmittingEscalate,
                        placeholder = { Text("এসকেলেশনের কারণ/নোট লিখুন...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (escalateError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = escalateError ?: "", fontSize = 12.sp, color = SomadhanError)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingEscalate = true
                        escalateError = null
                        viewModel.adminEscalateDirectContract(
                            problemId = contract.id,
                            note = escalateNote.ifBlank { "কোনো অতিরিক্ত নোট দেওয়া হয়নি" },
                            onSuccess = {
                                isSubmittingEscalate = false
                                contractForEscalate = null
                            },
                            onError = { err ->
                                isSubmittingEscalate = false
                                escalateError = err
                            }
                        )
                    },
                    enabled = !isSubmittingEscalate,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange)
                ) {
                    if (isSubmittingEscalate) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সংরক্ষণ হচ্ছে...", color = Color.White)
                    } else {
                        Text("এসকেলেট করুন", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !isSubmittingEscalate, onClick = { contractForEscalate = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Modal Dialog: Delete Problem Confirmation
    contractForDelete?.let { contract ->
        BottomSlideAlertDialog(
            onDismissRequest = { contractForDelete = null },
            title = { Text("ডাইরেক্ট চুক্তি স্থায়ীভাবে মুছবেন?", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Text("এটি স্থায়ীভাবে ডাটাবেস ও ক্লাউড ফায়ারস্টোর থেকে মুছে যাবে।", fontSize = 12.sp, color = SomadhanTextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminDeleteProblem(contract.id)
                        contractForDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError)
                ) {
                    Text("স্থায়ীভাবে মুছুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { contractForDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Modal Dialog: Message Delete Confirmation in Chat
    messageToDelete?.let { msg ->
        BottomSlideAlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("মেসেজ মুছে ফেলার নিশ্চিতকরণ", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary) },
            text = { Text("আপনি কি নিশ্চিত যে এই বার্তাটি স্থায়ীভাবে মুছে ফেলতে চান?", fontSize = 12.sp, color = SomadhanTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminDeleteMessage(msg.id)
                        messageToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError)
                ) {
                    Text("মুছুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) {
                    Text("না")
                }
            }
        )
    }
}
