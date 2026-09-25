@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.EscrowEntity
import com.example.data.entity.MessageEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.UserEntity
import com.example.data.repository.SomadhanRepository
import com.example.ui.components.PulsingValue
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.FileAttachmentUtil
import com.example.util.Formatters
import com.example.ui.components.BottomSlideAlertDialog
import com.example.ui.components.BottomSlideDialog

@Composable
fun AdminDisputeCenterView(
    viewModel: SomadhanViewModel,
    allProblems: List<ProblemEntity>,
    allUsers: List<UserEntity>
) {
    val context = LocalContext.current
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val allAdminMessages by viewModel.allAdminMessages.collectAsStateWithLifecycle()
    val allHeldEscrows by viewModel.allHeldEscrows.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Active Disputes, 1: Resolved Disputes History
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, NORMAL, INSTANT, DIRECT

    // Ground Rule ২০-এর মূল ভাবনা, সেশন ২.২৫ — এই ট্যাবে পেজিনেশন নেই, কিন্তু সার্চ/টাইপ-ফিল্টার/
    // সাব-ট্যাব (Active/Admin-Involved/Resolved) বদলে দৃশ্যমান সব ডিসপিউট-কার্ড একসাথে ছোট্ট করে
    // pulse করবে (Withdrawal/Problems/InstantJobs-এর একই প্যাটার্ন, শুধু pagination-key নেই)।
    var isFilterRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(searchQuery, selectedTypeFilter, selectedTab) {
        isFilterRefreshing = true
        try {
            kotlinx.coroutines.delay(350L)
        } finally {
            isFilterRefreshing = false
        }
    }

    var selectedProblemIdForModal by remember { mutableStateOf<String?>(null) }
    var messageToDelete by remember { mutableStateOf<MessageEntity?>(null) }
    var adminMessageText by remember { mutableStateOf("") }
    var isSendingAdminMessage by remember { mutableStateOf(false) }
    var adminMessageError by remember { mutableStateOf<String?>(null) }
    var customResolutionNote by remember { mutableStateOf("") }
    var showManualFlagDialog by remember { mutableStateOf(false) }

    // Dialog state for Custom Split & Warning Strike & Fullscreen Image Preview
    var showCustomSplitDialogForProblem by remember { mutableStateOf<ProblemEntity?>(null) }
    var showStrikeDialogForProblem by remember { mutableStateOf<ProblemEntity?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    // Settlement Action Confirmation State
    var pendingSettlementConfirmation by remember {
        mutableStateOf<DisputeCenterConfirmationData?>(null)
    }

    // Threads map for quick chat lookup
    val threads = remember(allAdminMessages) {
        allAdminMessages.groupBy { it.problemId }
    }

    // Dispute State Helper lambdas
    val isActiveDispute: (ProblemEntity) -> Boolean = { prob ->
        prob.isDisputed && prob.disputeResolvedAt == null && prob.disputeSettledAt == null && prob.disputeResolutionDecision.isNullOrBlank()
    }

    val isAdminInvolvedDispute: (ProblemEntity) -> Boolean = { prob ->
        isActiveDispute(prob) && (prob.isAdminInvolvedInChat || prob.adminAssistanceRequestedBy != null)
    }

    val isResolvedDispute: (ProblemEntity) -> Boolean = { prob ->
        !isActiveDispute(prob) && (!prob.disputeResolutionDecision.isNullOrBlank() || prob.disputeResolvedAt != null || prob.disputeSettledAt != null)
    }

    // Active vs Admin Involved vs Resolved problems
    val baseProblems = remember(allProblems, selectedTab) {
        when (selectedTab) {
            0 -> allProblems.filter { isActiveDispute(it) }
            1 -> allProblems.filter { isAdminInvolvedDispute(it) }
            else -> allProblems.filter { isResolvedDispute(it) }
        }
    }

    // Filtered by search & type
    val filteredProblems = remember(baseProblems, searchQuery, selectedTypeFilter) {
        val query = searchQuery.trim()
        baseProblems.filter { prob ->
            val matchesType = when (selectedTypeFilter) {
                "NORMAL" -> !prob.isInstantJob && prob.directContractStatus == null
                "INSTANT" -> prob.isInstantJob
                "DIRECT" -> prob.directContractStatus != null
                else -> true
            }

            val matchesSearch = if (query.isBlank()) true else {
                prob.id.contains(query, ignoreCase = true) ||
                        prob.title.contains(query, ignoreCase = true) ||
                        prob.userName.contains(query, ignoreCase = true) ||
                        prob.acceptedSolverName.orEmpty().contains(query, ignoreCase = true) ||
                        prob.disputeReason.orEmpty().contains(query, ignoreCase = true) ||
                        prob.disputeResolutionNote.orEmpty().contains(query, ignoreCase = true)
            }

            matchesType && matchesSearch
        }.sortedByDescending { it.disputedAt ?: it.createdAt }
    }

    val activeCount = remember(allProblems) { allProblems.count { isActiveDispute(it) } }
    val adminInvolvedCount = remember(allProblems) { allProblems.count { isAdminInvolvedDispute(it) } }
    val resolvedCount = remember(allProblems) { allProblems.count { isResolvedDispute(it) } }

    val tabIndicatorColor = when (selectedTab) {
        0 -> Color(0xFFDC2626)
        1 -> Color(0xFF4F46E5)
        else -> SomadhanOrange
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_dispute_center_view")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "বিরোধ (Dispute) কেন্দ্রীয় নিয়ন্ত্রণ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                }
                Text(
                    text = "সক্রিয়: ${DistanceUtil.toBengaliDigits(activeCount.toString())} টি | অ্যাডমিন যুক্ত: ${DistanceUtil.toBengaliDigits(adminInvolvedCount.toString())} টি | নিষ্পত্তি ইতিহাস: ${DistanceUtil.toBengaliDigits(resolvedCount.toString())} টি",
                    fontSize = 11.5.sp,
                    color = SomadhanTextSecondary
                )
            }

            Button(
                onClick = { showManualFlagDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("manual_flag_dispute_btn")
            ) {
                Icon(Icons.Default.AddAlert, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("ম্যানুয়ালি ফ্ল্যাগ করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Tabs (Active vs Admin Involved vs Resolved)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SomadhanCardBg,
            contentColor = tabIndicatorColor,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = tabIndicatorColor
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = if (selectedTab == 0) Color(0xFFDC2626) else SomadhanTextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "সক্রিয় বিরোধ (${DistanceUtil.toBengaliDigits(activeCount.toString())})",
                            fontSize = 11.5.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) Color(0xFFDC2626) else SomadhanTextSecondary
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = if (selectedTab == 1) Color(0xFF4F46E5) else SomadhanTextSecondary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "অ্যাডমিন যুক্ত (${DistanceUtil.toBengaliDigits(adminInvolvedCount.toString())})",
                            fontSize = 11.5.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) Color(0xFF4F46E5) else SomadhanTextSecondary
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = if (selectedTab == 2) SomadhanOrange else SomadhanTextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "সমাধানকৃত (${DistanceUtil.toBengaliDigits(resolvedCount.toString())})",
                            fontSize = 11.5.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) SomadhanOrange else SomadhanTextSecondary
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("আইডি, শিরোনাম, গ্রাহক, সমাধানকারী বা কারণ খুঁজুন...", fontSize = 12.sp) },
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
                focusedBorderColor = tabIndicatorColor,
                unfocusedBorderColor = SomadhanBorder,
                focusedContainerColor = SomadhanCardBg,
                unfocusedContainerColor = SomadhanCardBg
            ),
            modifier = Modifier.fillMaxWidth().testTag("dispute_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sub-filter chips (All / Normal / Instant / Direct)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedTypeFilter == "ALL",
                onClick = { selectedTypeFilter = "ALL" },
                label = { Text("সব (${DistanceUtil.toBengaliDigits(baseProblems.size.toString())})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tabIndicatorColor,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedTypeFilter == "NORMAL",
                onClick = { selectedTypeFilter = "NORMAL" },
                label = {
                    val count = baseProblems.count { !it.isInstantJob && it.directContractStatus == null }
                    Text("সাধারণ (${DistanceUtil.toBengaliDigits(count.toString())})", fontSize = 11.sp)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tabIndicatorColor,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedTypeFilter == "INSTANT",
                onClick = { selectedTypeFilter = "INSTANT" },
                label = {
                    val count = baseProblems.count { it.isInstantJob }
                    Text("ইনস্ট্যান্ট (${DistanceUtil.toBengaliDigits(count.toString())})", fontSize = 11.sp)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tabIndicatorColor,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedTypeFilter == "DIRECT",
                onClick = { selectedTypeFilter = "DIRECT" },
                label = {
                    val count = baseProblems.count { it.directContractStatus != null }
                    Text("সরাসরি চুক্তি (${DistanceUtil.toBengaliDigits(count.toString())})", fontSize = 11.sp)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tabIndicatorColor,
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // List of Dispute Cards
        if (filteredProblems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when (selectedTab) {
                            0 -> Icons.Default.CheckCircle
                            1 -> Icons.Default.SupportAgent
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when (selectedTab) {
                            0 -> SomadhanSuccess
                            1 -> Color(0xFF818CF8)
                            else -> SomadhanTextHint
                        },
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (selectedTab) {
                            0 -> "বর্তমানে কোনো সক্রিয় বিরোধ নেই ✅"
                            1 -> "কোনো বিরোধে এখনও গ্রাহক/সলভার অ্যাডমিন যুক্ত করেননি"
                            else -> "কোনো সমাধানকৃত বিরোধের ইতিহাস পাওয়া যায়নি"
                        },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = SomadhanTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredProblems, key = { it.id }) { problem ->
                    val escrow = allHeldEscrows.find { it.problemId == problem.id }
                    val (baseAmt, extraAmt) = SomadhanRepository.resolveSettlementAmounts(problem, escrow)
                    val escrowAmount = baseAmt + extraAmt
                    val isResolved = !problem.disputeResolutionDecision.isNullOrBlank()

                    // Ground Rule ১৯ — per-item pulse (শুধু যে ডিসপিউটের ডেটা সত্যিই বদলেছে তার
                    // কার্ডই action-এ pulse করবে), flashOnReentry = false। GR20-ভাবনা —
                    // isFilterRefreshing OR করা। GR21 এই ট্যাবে ইচ্ছাকৃতভাবে বাদ (ব্যবহারকারীর
                    // সিদ্ধান্ত, সেশন ২.২৫)।
                    val disputeCardPulse = rememberFieldChangePulse(
                        value = problem,
                        isManualRefreshing = isRefreshing,
                        sessionKey = "admin_dispute_center_sync",
                        viewModel = viewModel,
                        flashOnReentry = false
                    )
                    PulsingValue(isUpdating = disputeCardPulse || isFilterRefreshing) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (problem.isDisputed) Color(0xFFFFF1F2) else SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (problem.isDisputed) 1.5.dp else 1.dp,
                                color = if (problem.isDisputed) Color(0xFFF43F5E) else SomadhanDivider,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedProblemIdForModal = problem.id
                                customResolutionNote = ""
                            }
                            .testTag("dispute_item_${problem.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Badges & Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Type Badge
                                    val typeLabel = when {
                                        problem.isInstantJob -> "⚡ ইনস্ট্যান্ট জব"
                                        problem.directContractStatus != null -> "🤝 সরাসরি চুক্তি"
                                        else -> "📋 সাধারণ সমস্যা"
                                    }
                                    val typeColor = when {
                                        problem.isInstantJob -> Color(0xFFEA580C)
                                        problem.directContractStatus != null -> Color(0xFF0284C7)
                                        else -> Color(0xFF4F46E5)
                                    }
                                    Surface(
                                        color = typeColor.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.5.dp, typeColor.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                             text = typeLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = typeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Initiator Badge
                                    val initiatorRoleText = when (problem.disputeInitiatorRole?.uppercase()) {
                                        "SOLVER" -> "সমাধানকারী কর্তৃক উত্থাপিত"
                                        "ADMIN" -> "🚨 অ্যাডমিন ফ্ল্যাগড"
                                        else -> "গ্রাহক কর্তৃক উত্থাপিত"
                                    }
                                    val initiatorColor = if (problem.disputeInitiatorRole?.uppercase() == "ADMIN") Color(0xFF991B1B) else Color(0xFFDC2626)
                                    Surface(
                                        color = initiatorColor.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = initiatorRoleText,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = initiatorColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = Formatters.formatTimeAgo(problem.disputedAt ?: problem.createdAt),
                                    fontSize = 10.sp,
                                    color = SomadhanTextHint
                                )
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

                            Spacer(modifier = Modifier.height(4.dp))

                            // Problem ID and Parties
                            Text(
                                text = "আইডি: #${problem.id} | গ্রাহক: ${problem.userName} ↔ সমাধানকারী: ${problem.acceptedSolverName ?: "নিযুক্ত নেই"}",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )

                            // Dispute Reason Box
                            if (!problem.disputeReason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, Color(0xFFFCA5A5))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "বিরোধের কারণ:",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF991B1B)
                                            )
                                            Text(
                                                text = problem.disputeReason.orEmpty(),
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF7F1D1D)
                                            )
                                        }
                                    }
                                }
                            }

                            // Resolved Decision summary (if resolved tab)
                            if (isResolved) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val decisionBadgeColor = when {
                                    problem.disputeResolutionDecision == "RELEASE_TO_SOLVER" -> SomadhanSuccess
                                    problem.disputeResolutionDecision == "REFUND_TO_USER" -> SomadhanError
                                    problem.disputeResolutionDecision?.startsWith("CUSTOM_SPLIT") == true -> Color(0xFF7C3AED)
                                    else -> Color(0xFFD97706)
                                }
                                val decisionBadgeText = when {
                                    problem.disputeResolutionDecision == "RELEASE_TO_SOLVER" -> "✅ সমাধানকারীকে রিলিজকৃত"
                                    problem.disputeResolutionDecision == "REFUND_TO_USER" -> "↩️ ক্লায়েন্টকে রিফান্ডকৃত"
                                    problem.disputeResolutionDecision == "SPLIT_SETTLEMENT" -> "⚖️ ৫০/৫০ সমঝোতা নিষ্পন্ন"
                                    problem.disputeResolutionDecision == "CUSTOM_SPLIT" -> "⚙️ কাস্টম ভাগাভাগি নিষ্পন্ন"
                                    else -> problem.disputeResolutionDecision.orEmpty()
                                }

                                Surface(
                                    color = decisionBadgeColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, decisionBadgeColor.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = decisionBadgeText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = decisionBadgeColor
                                            )
                                            if (problem.disputeResolvedAt != null) {
                                                Text(
                                                    text = Formatters.formatTimeAgo(problem.disputeResolvedAt),
                                                    fontSize = 9.5.sp,
                                                    color = SomadhanTextSecondary
                                                )
                                            }
                                        }
                                        if (!problem.disputeResolutionNote.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "নোট: ${problem.disputeResolutionNote}",
                                                fontSize = 10.5.sp,
                                                color = SomadhanTextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom Row: Escrow Amount & Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "এসক্রো ফান্ড: ",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary
                                    )
                                    Text(
                                        text = "৳${DistanceUtil.toBengaliDigits(escrowAmount.toInt().toString())}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (problem.isAdminInvolvedInChat || problem.adminAssistanceRequestedBy != null) {
                                        Surface(
                                            color = Color(0xFF4F46E5).copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "🛡️ অ্যাডমিন যুক্ত",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4F46E5),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (problem.isDisputed && problem.disputeResolvedAt == null && problem.disputeSettledAt == null && problem.disputeResolutionDecision.isNullOrBlank()) {
                                        Surface(
                                            color = Color(0xFFD97706).copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "🔒 অ্যাডমিন যুক্ত নেই (চ্যাট বন্ধ)",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFB45309),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "পূর্ণ বিবরণ ও অ্যাকশন →",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                            }
                        }
                    }
                    } // close PulsingValue (Ground Rule ১৯/২০, সেশন ২.২৫)
                }
            }
        }
    }

    // Full Modal Dialog for Selected Dispute Problem
    selectedProblemIdForModal?.let { problemId ->
        val problem = allProblems.find { it.id == problemId }
        val msgs = (threads[problemId] ?: emptyList()).sortedBy { it.timestamp }
        // BUGFIX: previously this only checked `problem.isDisputed`, which intentionally
        // stays true forever after resolution (it's used as a historical "this job had a
        // dispute" marker -- see adminResolveDisputeLocked()'s comments). That meant opening
        // an ALREADY-RESOLVED dispute from the "সমাধানকৃত" (Resolved) history tab still showed
        // the live Release/Refund/50-50/Custom% action panel, letting an admin attempt to
        // re-resolve a settled dispute. The repository-level idempotency guard stops any actual
        // double payout, but the UI would still show a false "success" toast for a no-op click.
        // Gating on disputeResolvedAt as well (matching the isActiveDispute()/isResolvedDispute()
        // lambdas already used for tab filtering above) keeps the action panel from ever
        // rendering for a dispute that has already been settled.
        val isDisputeActive = problem?.isDisputed == true && problem.disputeResolvedAt == null

        var modalInnerTab by rememberSaveable { mutableIntStateOf(0) } // 0: Live Chat Monitoring, 1: Evidence & Media
        var visibleMessageCount by remember(problemId) { mutableIntStateOf(15) }
        val totalMsgsCount = msgs.size
        val displayedMsgs = remember(msgs, visibleMessageCount) {
            if (totalMsgsCount <= visibleMessageCount) msgs else msgs.takeLast(visibleMessageCount)
        }

        // Attachments / Evidence extracted from chat messages
        val evidenceFiles = remember(msgs) {
            msgs.filter { !it.fileUrl.isNullOrBlank() }
        }

        val escrow = allHeldEscrows.find { it.problemId == problemId }
        val totalEscrow = if (problem != null) {
            val (bAmt, eAmt) = SomadhanRepository.resolveSettlementAmounts(problem, escrow)
            bAmt + eAmt
        } else {
            0.0
        }

        BottomSlideDialog(onDismissRequest = { selectedProblemIdForModal = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(720.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = SomadhanBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Modal Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = problem?.title ?: "বিরোধ নিষ্পত্তি ও পর্যবেক্ষণ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SomadhanTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "পোস্ট: #${problem?.id ?: problemId} | ৳${DistanceUtil.toBengaliDigits(totalEscrow.toInt().toString())} এসক্রো | ${problem?.userName ?: "গ্রাহক"} ↔ ${problem?.acceptedSolverName ?: "সমাধানকারী"}",
                                fontSize = 10.5.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                        IconButton(onClick = { selectedProblemIdForModal = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "বন্ধ করুন")
                        }
                    }

                    // Admin Dispute Control Panel inside modal
                    if (isDisputeActive && problem != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("বিরোধ চলমান", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Issue Warning Strike Button
                                        Button(
                                            onClick = { showStrikeDialogForProblem = problem },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("সতর্কবার্তা/পেনাল্টি", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        if (problem.isAdminInvolvedInChat || problem.adminAssistanceRequestedBy != null) {
                                            Surface(
                                                color = Color(0xFF4F46E5).copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "🛡️ অ্যাডমিন চ্যাট সক্রিয়",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF4F46E5),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        } else {
                                            Surface(
                                                color = Color(0xFFD97706).copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "🔒 অ্যাডমিন যুক্ত নেই (চ্যাট বন্ধ)",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD97706),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!problem.disputeReason.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "কারণ: ${problem.disputeReason}",
                                        fontSize = 11.sp,
                                        color = SomadhanTextPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Decision Note TextField
                                OutlinedTextField(
                                    value = customResolutionNote,
                                    onValueChange = { customResolutionNote = it },
                                    placeholder = { Text("নিষ্পত্তির মন্তব্য/নোট লিখুন (ঐচ্ছিক)...", fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFDC2626),
                                        unfocusedBorderColor = SomadhanBorder,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // 4 Settlement Action Buttons (Release, Refund, 50/50, Custom Split) with Confirmation Popups
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val note = if (customResolutionNote.isNotBlank()) customResolutionNote.trim() else "অ্যাডমিন সিদ্ধান্তক্রমে সমাধানকারীকে সম্পূর্ণ অর্থ রিলিজ করা হয়েছে।"
                                            val sAmt = totalEscrow
                                            val uAmt = 0.0
                                            pendingSettlementConfirmation = DisputeCenterConfirmationData(
                                                problem = problem,
                                                resolutionType = "RELEASE_TO_SOLVER",
                                                title = "রিলিজ নিশ্চিতকরণ (Release to Solver)",
                                                description = "আপনি কি নিশ্চিত যে সম্পূর্ণ অর্থ সমাধানকারীকে হস্তান্তর করতে চান?",
                                                solverAmount = sAmt,
                                                userRefundAmount = uAmt,
                                                solverPercent = 100.0,
                                                decisionNote = note,
                                                accentColor = SomadhanSuccess
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(32.dp).testTag("resolve_release_btn")
                                    ) {
                                        Text("রিলিজ দিন", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            val note = if (customResolutionNote.isNotBlank()) customResolutionNote.trim() else "অ্যাডমিন সিদ্ধান্তক্রমে ক্লায়েন্টকে সম্পূর্ণ অর্থ রিফান্ড করা হয়েছে।"
                                            val sAmt = 0.0
                                            val uAmt = totalEscrow
                                            pendingSettlementConfirmation = DisputeCenterConfirmationData(
                                                problem = problem,
                                                resolutionType = "REFUND_TO_USER",
                                                title = "রিফান্ড নিশ্চিতকরণ (Refund to Client)",
                                                description = "আপনি কি নিশ্চিত যে সম্পূর্ণ অর্থ ক্লায়েন্টকে রিফান্ড ফেরত দিতে চান?",
                                                solverAmount = sAmt,
                                                userRefundAmount = uAmt,
                                                solverPercent = 0.0,
                                                decisionNote = note,
                                                accentColor = SomadhanError
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(32.dp).testTag("resolve_refund_btn")
                                    ) {
                                        Text("রিফান্ড করুন", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            val note = if (customResolutionNote.isNotBlank()) customResolutionNote.trim() else "অ্যাডমিন কর্তৃক বিরোধ সমঝোতা করে ৫০-৫০ হারে রিলিজ ও রিফান্ড করা হয়েছে।"
                                            val sAmt = totalEscrow * 0.5
                                            val uAmt = totalEscrow * 0.5
                                            pendingSettlementConfirmation = DisputeCenterConfirmationData(
                                                problem = problem,
                                                resolutionType = "SPLIT_SETTLEMENT",
                                                title = "৫০/৫০ সমঝোতা নিশ্চিতকরণ",
                                                description = "আপনি কি নিশ্চিত যে উভয় পক্ষকে সমান ৫০% হারে অর্থ বণ্টন করতে চান?",
                                                solverAmount = sAmt,
                                                userRefundAmount = uAmt,
                                                solverPercent = 50.0,
                                                decisionNote = note,
                                                accentColor = Color(0xFFD97706)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1.05f).height(32.dp).testTag("resolve_split_btn")
                                    ) {
                                        Text("৫০/৫০", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { showCustomSplitDialogForProblem = problem },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1.15f).height(32.dp).testTag("resolve_custom_split_btn")
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("কাস্টম %", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else if (problem != null && !problem.disputeResolutionDecision.isNullOrBlank()) {
                        // Resolved Summary Card inside modal
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("✅ এই বিরোধটি ইতিমধ্যে অ্যাডমিন কর্তৃক মীমাংসা করা হয়েছে", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                                Text("সিদ্ধান্ত: ${problem.disputeResolutionDecision}", fontSize = 11.sp, color = SomadhanTextPrimary)
                                if (!problem.disputeResolutionNote.isNullOrBlank()) {
                                    Text("মন্তব্য: ${problem.disputeResolutionNote}", fontSize = 10.5.sp, color = SomadhanTextSecondary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Modal Sub-tabs: 0: Chat Thread, 1: Evidence & Media
                    TabRow(
                        selectedTabIndex = modalInnerTab,
                        containerColor = SomadhanCardBg,
                        contentColor = SomadhanOrange,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[modalInnerTab]),
                                color = SomadhanOrange
                            )
                        }
                    ) {
                        Tab(
                            selected = modalInnerTab == 0,
                            onClick = { modalInnerTab = 0 },
                            text = {
                                Text("💬 চ্যাট পর্যবেক্ষণ (${DistanceUtil.toBengaliDigits(totalMsgsCount.toString())})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        )
                        Tab(
                            selected = modalInnerTab == 1,
                            onClick = { modalInnerTab = 1 },
                            text = {
                                Text("📎 প্রমাণাদি ও ফাইল (${DistanceUtil.toBengaliDigits(evidenceFiles.size.toString())})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (modalInnerTab == 0) {
                        // CHAT MONITORING TAB
                        if (totalMsgsCount > visibleMessageCount) {
                            val remaining = totalMsgsCount - visibleMessageCount
                            OutlinedButton(
                                onClick = { visibleMessageCount += 15 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4F46E5)),
                                border = BorderStroke(1.dp, Color(0xFF4F46E5).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "⬆️ আরও পুরোনো বার্তা লোড করুন (${DistanceUtil.toBengaliDigits(remaining.toString())} টি বাকি)",
                                    fontSize = 11.sp,
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
                            if (displayedMsgs.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("এই কাজের জন্য এখনো কোনো বার্তা আদান-প্রদান হয়নি।", fontSize = 12.sp, color = SomadhanTextHint)
                                    }
                                }
                            }

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

                                        if (!msg.fileUrl.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val isImg = msg.fileType == "image" || msg.fileName?.endsWith(".jpg", ignoreCase = true) == true || msg.fileName?.endsWith(".png", ignoreCase = true) == true

                                            if (isImg) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 160.dp, height = 110.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.Black.copy(alpha = 0.05f))
                                                        .clickable { previewImageUrl = msg.fileUrl }
                                                ) {
                                                    AsyncImage(
                                                        model = msg.fileUrl,
                                                        contentDescription = "ছবি প্রমাণ",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    Surface(
                                                        color = Color.Black.copy(alpha = 0.5f),
                                                        shape = RoundedCornerShape(bottomStart = 8.dp),
                                                        modifier = Modifier.align(Alignment.TopEnd)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.ZoomIn,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp).padding(2.dp)
                                                        )
                                                    }
                                                }
                                            } else {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isAdmin) Color(0xFF312E81) else SomadhanBg)
                                                        .clickable {
                                                            FileAttachmentUtil.openFile(context, msg.fileUrl ?: "", msg.fileName)
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

                        // Admin Chat Message Intervention input (Only enabled if user/solver involved admin)
                        val isAdminInvolved = problem != null && (problem.isAdminInvolvedInChat || problem.adminAssistanceRequestedBy != null)
                        Spacer(modifier = Modifier.height(6.dp))

                        if (isAdminInvolved) {
                            Surface(
                                color = Color(0xFFEEF2FF),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFC7D2FE)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "🛡️ অ্যাডমিন সহায়তা সক্রিয় রয়েছে (যুক্ত করেছেন: ${if (problem?.adminAssistanceRequestedBy == "SOLVER") "সমাধানকারী" else if (problem?.adminAssistanceRequestedBy == "USER") "গ্রাহক" else "উভয় পক্ষ"})।",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF3730A3)
                                    )
                                }
                            }
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
                                    modifier = Modifier.weight(1f).height(46.dp)
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
                        } else {
                            // Chat disabled notice for admin
                            Surface(
                                color = Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "চ্যাট বন্ধ: অ্যাডমিন যুক্ত করা হয়নি",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                        Text(
                                            text = "গ্রাহক বা সমাধানকারী যতক্ষণ না অ্যাডমিন যুক্ত করবেন, ততক্ষণ অ্যাডমিন চ্যাটে বার্তা পাঠাতে পারবেন না।",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // EVIDENCE & MEDIA TAB
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Section 1: Written Evidence & Statements
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("📜 লিখিত বিবরণ ও বিরোধ প্রেক্ষাপট", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = SomadhanTextPrimary)
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Problem description
                                        Text("কাজের বিবরণ:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SomadhanTextSecondary)
                                        Text(problem?.description ?: "কোনো বিবরণ নেই", fontSize = 11.5.sp, color = SomadhanTextPrimary)

                                        if (!problem?.disputeReason.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("বিরোধের কারণ:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                            Text(problem.disputeReason.orEmpty(), fontSize = 11.5.sp, color = Color(0xFF991B1B))
                                        }

                                        if (!problem?.releaseRequestNote.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("রিলিজ রিকোয়েস্ট নোট:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SomadhanOrange)
                                            Text(problem.releaseRequestNote, fontSize = 11.5.sp, color = SomadhanTextPrimary)
                                        }

                                        if (!problem?.solverCancelledNotice.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("বাতিলকরণ সংক্রান্ত তথ্য:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                            Text(problem.solverCancelledNotice.orEmpty(), fontSize = 11.5.sp, color = SomadhanTextPrimary)
                                        }
                                    }
                                }
                            }

                            // Section 2: Uploaded Media & Attachments Gallery
                            item {
                                Text("📸 সংযুক্ত ছবি ও প্রমাণাদি গ্যালারি (${DistanceUtil.toBengaliDigits(evidenceFiles.size.toString())} টি ফাইল)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = SomadhanTextPrimary)
                            }

                            if (evidenceFiles.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("এই বিরোধে এখনো কোনো ফাইল বা ছবি সংযুক্ত করা হয়নি।", fontSize = 11.5.sp, color = SomadhanTextHint)
                                    }
                                }
                            } else {
                                items(evidenceFiles, key = { it.id }) { itemFile ->
                                    val isImg = itemFile.fileType == "image" || itemFile.fileName?.endsWith(".jpg", ignoreCase = true) == true || itemFile.fileName?.endsWith(".png", ignoreCase = true) == true

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isImg) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(70.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.Black.copy(alpha = 0.05f))
                                                        .clickable { previewImageUrl = itemFile.fileUrl }
                                                ) {
                                                    AsyncImage(
                                                        model = itemFile.fileUrl,
                                                        contentDescription = "প্রমাণ ছবি",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(SomadhanOrange.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Description, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(24.dp))
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = itemFile.fileName ?: (if (isImg) "ছবি প্রমাণ" else "সংযুক্ত ফাইল"),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SomadhanTextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "প্রেরক: ${itemFile.senderName} • ${Formatters.formatTimeAgo(itemFile.timestamp)}",
                                                    fontSize = 10.sp,
                                                    color = SomadhanTextSecondary
                                                )
                                                if (itemFile.content.isNotBlank()) {
                                                    Text(
                                                        text = itemFile.content,
                                                        fontSize = 10.5.sp,
                                                        color = SomadhanTextHint,
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    if (isImg) {
                                                        previewImageUrl = itemFile.fileUrl
                                                    } else {
                                                        FileAttachmentUtil.openFile(context, itemFile.fileUrl ?: "", itemFile.fileName)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (isImg) Color(0xFF4F46E5) else SomadhanOrange),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(if (isImg) "দেখুন" else "খুলুন", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
    }

    // 1. Custom Split Settlement Dialog
    showCustomSplitDialogForProblem?.let { prob ->
        val escrow = allHeldEscrows.find { it.problemId == prob.id }
        val (bAmt, eAmt) = SomadhanRepository.resolveSettlementAmounts(prob, escrow)
        val totalAmount = bAmt + eAmt

        var solverPercent by remember { mutableFloatStateOf(60f) }
        var splitNote by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        val solverShare = Math.round(totalAmount * (solverPercent / 100.0)).toDouble()
        val userRefund = Math.round(totalAmount - solverShare).toDouble()

        BottomSlideAlertDialog(
            onDismissRequest = { if (!isSubmitting) showCustomSplitDialogForProblem = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("কাস্টম ভাগাভাগি নিষ্পত্তি", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "মোট এসক্রো ফান্ড: ৳${DistanceUtil.toBengaliDigits(totalAmount.toInt().toString())}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                    Text(
                        text = "সলভার ও ক্লায়েন্টের মধ্যে কাজের অগ্রগতি অনুপাতে অর্থ ভাগ করুন:",
                        fontSize = 11.5.sp,
                        color = SomadhanTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Calculation Display Cards
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).border(1.dp, Color(0xFFC084FC), RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("🔧 সমাধানকারী পাবে", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B21A8))
                                Text("৳${DistanceUtil.toBengaliDigits(solverShare.toInt().toString())}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF581C87))
                                Text("(${DistanceUtil.toBengaliDigits(solverPercent.toInt().toString())}%)", fontSize = 9.5.sp, color = Color(0xFF7E22CE))
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).border(1.dp, Color(0xFF93C5FD), RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("👤 ক্লায়েন্ট রিফান্ড", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                                Text("৳${DistanceUtil.toBengaliDigits(userRefund.toInt().toString())}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                                Text("(${DistanceUtil.toBengaliDigits((100 - solverPercent.toInt()).toString())}%)", fontSize = 9.5.sp, color = Color(0xFF2563EB))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Slider
                    Slider(
                        value = solverPercent,
                        onValueChange = { solverPercent = it },
                        valueRange = 0f..100f,
                        steps = 19, // 5% increments
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF7C3AED),
                            activeTrackColor = Color(0xFF7C3AED),
                            inactiveTrackColor = Color(0xFFDDD6FE)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Ratio Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(20f, 30f, 50f, 70f, 80f).forEach { pct ->
                            Surface(
                                color = if (solverPercent.toInt() == pct.toInt()) Color(0xFF7C3AED) else SomadhanCardBg,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0xFF7C3AED)),
                                modifier = Modifier.clickable { solverPercent = pct }
                            ) {
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(pct.toInt().toString())}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (solverPercent.toInt() == pct.toInt()) Color.White else Color(0xFF7C3AED),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = splitNote,
                        onValueChange = { splitNote = it },
                        placeholder = { Text("ভাগাভাগির সুনির্দিষ্ট কারণ/নোট লিখুন...", fontSize = 11.5.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = SomadhanBorder
                        ),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val note = if (splitNote.isNotBlank()) splitNote.trim() else "অ্যাডমিন সিদ্ধান্ত অনুযায়ী কাজের অগ্রগতির প্রেক্ষিতে কাস্টম ভাগাভাগি মীমাংসা করা হয়েছে।"
                        val sAmt = solverShare
                        val uAmt = userRefund
                        val pct = solverPercent.toDouble()
                        pendingSettlementConfirmation = DisputeCenterConfirmationData(
                            problem = prob,
                            resolutionType = "CUSTOM_SPLIT",
                            title = "কাস্টম ভাগাভাগি নিশ্চিতকরণ",
                            description = "আপনি কি নিশ্চিত যে কাস্টম অনুপাতে (${pct.toInt()}% / ${(100 - pct).toInt()}%) উভয় পক্ষের মধ্যে অর্থ বণ্টন করতে চান?",
                            solverAmount = sAmt,
                            userRefundAmount = uAmt,
                            solverPercent = pct,
                            decisionNote = note,
                            accentColor = Color(0xFF7C3AED)
                        )
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) {
                    Text("মীমাংসা নিশ্চিতকরণ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomSplitDialogForProblem = null }, enabled = !isSubmitting) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // 2. Penalty & Warning Strike Dialog
    showStrikeDialogForProblem?.let { prob ->
        var targetParty by remember { mutableStateOf("SOLVER") } // "USER" or "SOLVER"
        var penaltyPoints by remember { mutableFloatStateOf(5f) }
        var selectedReasonPreset by remember { mutableStateOf("মিথ্যা অভিযোগ ও অযৌক্তিক বিরোধ") }
        var customReason by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }
        var strikeError by remember { mutableStateOf<String?>(null) }

        val targetUserId = if (targetParty == "SOLVER") prob.acceptedSolverId.orEmpty() else prob.userId
        val targetUserName = if (targetParty == "SOLVER") prob.acceptedSolverName ?: "সমাধানকারী" else prob.userName

        BottomSlideAlertDialog(
            onDismissRequest = { if (!isSubmitting) showStrikeDialogForProblem = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সতর্কবার্তা ও পেনাল্টি স্ট্রাইক", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("দোষী পক্ষকে আনুষ্ঠানিক সতর্কবার্তা ও রেপুটেশন পেনাল্টি দিন:", fontSize = 11.5.sp, color = SomadhanTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Select Target Party
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = targetParty == "SOLVER",
                            onClick = { targetParty = "SOLVER" },
                            label = { Text("🔧 সলভার (${prob.acceptedSolverName ?: "নিযুক্ত নেই"})", fontSize = 11.sp) },
                            enabled = !prob.acceptedSolverId.isNullOrBlank(),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFDC2626), selectedLabelColor = Color.White)
                        )
                        FilterChip(
                            selected = targetParty == "USER",
                            onClick = { targetParty = "USER" },
                            label = { Text("👤 গ্রাহক (${prob.userName})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFDC2626), selectedLabelColor = Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Penalty Points Selector
                    Text("পেনাল্টি স্কোর: -${DistanceUtil.toBengaliDigits(penaltyPoints.toInt().toString())} পয়েন্ট", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                    Slider(
                        value = penaltyPoints,
                        onValueChange = { penaltyPoints = it },
                        valueRange = 0f..20f,
                        steps = 3, // 0, 5, 10, 15, 20
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFDC2626),
                            activeTrackColor = Color(0xFFDC2626),
                            inactiveTrackColor = Color(0xFFFCA5A5)
                        )
                    )

                    // Presets
                    Text("লঙ্ঘনের ধরন:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SomadhanTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf(
                        "মিথ্যা অভিযোগ ও অযৌক্তিক বিরোধ",
                        "কাজে চরম অবহেলা ও দীর্ঘসূত্রিতা",
                        "চ্যাটে অশালীন ভাষা বা অসদাচরণ",
                        "প্ল্যাটফর্মের বাইরে লেনদেনের অপচেষ্টা"
                    ).forEach { preset ->
                        Surface(
                            color = if (selectedReasonPreset == preset) Color(0xFFFEE2E2) else SomadhanCardBg,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, if (selectedReasonPreset == preset) Color(0xFFDC2626) else SomadhanDivider),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { selectedReasonPreset = preset }
                        ) {
                            Text(
                                text = preset,
                                fontSize = 10.5.sp,
                                color = if (selectedReasonPreset == preset) Color(0xFF991B1B) else SomadhanTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        placeholder = { Text("অতিরিক্ত বিবরণ (ঐচ্ছিক)...", fontSize = 11.5.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFDC2626),
                            unfocusedBorderColor = SomadhanBorder
                        ),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    )
                    if (strikeError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = strikeError ?: "", fontSize = 12.sp, color = SomadhanError)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetUserId.isNotBlank()) {
                            isSubmitting = true
                            strikeError = null
                            val finalReason = if (customReason.isNotBlank()) "$selectedReasonPreset ($customReason)" else selectedReasonPreset
                            viewModel.adminIssueWarningStrike(
                                targetUserId = targetUserId,
                                problemId = prob.id,
                                reason = finalReason,
                                penaltyReputation = penaltyPoints.toDouble(),
                                targetRole = targetParty,
                                onSuccess = {
                                    isSubmitting = false
                                    showStrikeDialogForProblem = null
                                    Toast.makeText(context, "সতর্কবার্তা ও পেনাল্টি সফলভাবে পাঠানো হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    isSubmitting = false
                                    strikeError = err
                                }
                            )
                        } else {
                            strikeError = "সঠিক ব্যক্তি নির্বাচন করুন"
                        }
                    },
                    enabled = targetUserId.isNotBlank() && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পাঠানো হচ্ছে...", color = Color.White)
                    } else {
                        Text("স্ট্রাইক প্রদান করুন", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showStrikeDialogForProblem = null }, enabled = !isSubmitting) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // 3. Fullscreen / Enlarged Image Preview Dialog
    previewImageUrl?.let { imgUrl ->
        Dialog(onDismissRequest = { previewImageUrl = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = "সম্পূর্ণ ছবি",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                    IconButton(
                        onClick = { previewImageUrl = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = Color.White)
                    }
                }
            }
        }
    }

    // Message Delete Confirmation Dialog
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

    // Manual Flag Dispute Dialog
    if (showManualFlagDialog) {
        var flagProblemSearch by remember { mutableStateOf("") }
        var selectedProblemToFlag by remember { mutableStateOf<ProblemEntity?>(null) }
        var flagReason by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }
        var flagError by remember { mutableStateOf<String?>(null) }

        val eligibleProblems = remember(allProblems, flagProblemSearch) {
            val q = flagProblemSearch.trim()
            allProblems.filter { !it.isDisputed && it.status != "CANCELLED" }.filter { prob ->
                if (q.isBlank()) true else {
                    prob.id.contains(q, ignoreCase = true) ||
                            prob.title.contains(q, ignoreCase = true) ||
                            prob.userName.contains(q, ignoreCase = true) ||
                            prob.acceptedSolverName.orEmpty().contains(q, ignoreCase = true)
                }
            }.take(20)
        }

        BottomSlideAlertDialog(
            onDismissRequest = { if (!isSubmitting) showManualFlagDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddAlert, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ম্যানুয়ালি বিরোধ (Dispute) ফ্ল্যাগ করুন", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "যেকোনো সমস্যা বেছে নিয়ে অ্যাডমিন হিসেবে বিরোধ চিহ্নিত করুন:",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedProblemToFlag == null) {
                        OutlinedTextField(
                            value = flagProblemSearch,
                            onValueChange = { flagProblemSearch = it },
                            placeholder = { Text("সমস্যার আইডি বা নাম দিয়ে খুঁজুন...", fontSize = 11.5.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFDC2626),
                                unfocusedBorderColor = SomadhanBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(eligibleProblems, key = { it.id }) { p ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedProblemToFlag = p }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(p.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Text("#${p.id} | ${p.userName} ↔ ${p.acceptedSolverName ?: "সলভার নেই"}", fontSize = 10.sp, color = SomadhanTextSecondary)
                                        }
                                        Text("নির্বাচন", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                    }
                                }
                            }
                        }
                    } else {
                        // Selected problem card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFF87171), RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedProblemToFlag!!.title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                                    Text("আইডি: #${selectedProblemToFlag!!.id}", fontSize = 10.sp, color = Color(0xFF7F1D1D))
                                }
                                TextButton(onClick = { selectedProblemToFlag = null }) {
                                    Text("বদলান", fontSize = 11.sp, color = Color(0xFFDC2626))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = flagReason,
                            onValueChange = { flagReason = it },
                            placeholder = { Text("বিরোধ ফ্ল্যাগ করার সুনির্দিষ্ট কারণ লিখুন...", fontSize = 12.sp) },
                            minLines = 3,
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFDC2626),
                                unfocusedBorderColor = SomadhanBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (flagError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = flagError ?: "", fontSize = 12.sp, color = SomadhanError)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = selectedProblemToFlag
                        if (p != null && flagReason.isNotBlank()) {
                            isSubmitting = true
                            flagError = null
                            viewModel.adminManuallyFlagDispute(
                                problemId = p.id,
                                reason = flagReason.trim(),
                                onSuccess = {
                                    isSubmitting = false
                                    showManualFlagDialog = false
                                    Toast.makeText(context, "বিরোধ ফ্ল্যাগ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    isSubmitting = false
                                    flagError = err
                                }
                            )
                        } else {
                            flagError = "সমস্যা ও কারণ দুটোই প্রদান করুন"
                        }
                    },
                    enabled = selectedProblemToFlag != null && flagReason.isNotBlank() && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সাবমিট হচ্ছে...", color = Color.White)
                    } else {
                        Text("ফ্ল্যাগ সাবমিট করুন", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showManualFlagDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // Settlement Action Confirmation Popup (Release, Refund, 50/50, Custom Split)
    pendingSettlementConfirmation?.let { conf ->
        var isActionSubmitting by remember { mutableStateOf(false) }
        var resolveDisputeError by remember { mutableStateOf<String?>(null) }
        val prob = conf.problem
        val solverName = prob.acceptedSolverName ?: "সমাধানকারী"
        val clientName = prob.userName

        BottomSlideAlertDialog(
            onDismissRequest = { if (!isActionSubmitting) pendingSettlementConfirmation = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(conf.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (conf.resolutionType) {
                            "RELEASE_TO_SOLVER" -> Icons.Default.CheckCircle
                            "REFUND_TO_USER" -> Icons.Default.Refresh
                            "SPLIT_SETTLEMENT" -> Icons.Default.Gavel
                            else -> Icons.Default.Tune
                        },
                        contentDescription = null,
                        tint = conf.accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = conf.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = conf.description,
                        fontSize = 13.sp,
                        color = SomadhanTextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Breakdown Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "টাকা বিতরণের চূড়ান্ত হিসাব:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔧 সমাধানকারী ($solverName):",
                                    fontSize = 12.sp,
                                    color = SomadhanTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "৳ ${DistanceUtil.toBengaliDigits(conf.solverAmount.toInt().toString())}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (conf.solverAmount > 0) Color(0xFF16A34A) else SomadhanTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "👤 ক্লায়েন্ট ($clientName):",
                                    fontSize = 12.sp,
                                    color = SomadhanTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "৳ ${DistanceUtil.toBengaliDigits(conf.userRefundAmount.toInt().toString())}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (conf.userRefundAmount > 0) Color(0xFF2563EB) else SomadhanTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "⚠️ সতর্কতা: এই অ্যাকশনটি চূড়ান্ত এবং অবিলম্বে সংশ্লিষ্ট ওয়ালেটে টাকা স্থানান্তরিত হবে।",
                        fontSize = 11.5.sp,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Medium
                    )
                    if (resolveDisputeError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = resolveDisputeError ?: "", fontSize = 12.sp, color = SomadhanError)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isActionSubmitting = true
                        resolveDisputeError = null
                        viewModel.adminResolveDispute(
                            problemId = prob.id,
                            resolution = conf.resolutionType,
                            decisionNote = conf.decisionNote,
                            splitSolverPercent = conf.solverPercent,
                            onSuccess = {
                                isActionSubmitting = false
                                pendingSettlementConfirmation = null
                                showCustomSplitDialogForProblem = null
                                selectedProblemIdForModal = null
                                Toast.makeText(context, "ডিসপিউট সফলভাবে মীমাংসা ও ফান্ড বিতরণ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                isActionSubmitting = false
                                resolveDisputeError = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = conf.accentColor),
                    enabled = !isActionSubmitting
                ) {
                    if (isActionSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("মীমাংসা হচ্ছে...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("হ্যাঁ, নিশ্চিত করুন", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingSettlementConfirmation = null },
                    enabled = !isActionSubmitting
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }
}

private data class DisputeCenterConfirmationData(
    val problem: ProblemEntity,
    val resolutionType: String,
    val title: String,
    val description: String,
    val solverAmount: Double,
    val userRefundAmount: Double,
    val solverPercent: Double,
    val decisionNote: String,
    val accentColor: Color
)
