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
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CategoryEntity
import com.example.data.entity.EscrowEntity
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
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.Formatters
import kotlin.math.ceil
import kotlin.math.max
import com.example.ui.components.BottomSlideAlertDialog

@Composable
fun AdminInstantJobsView(
    allProblems: List<ProblemEntity>,
    allUsers: List<UserEntity>,
    allCategories: List<CategoryEntity>,
    allHeldEscrows: List<EscrowEntity> = emptyList(),
    allReleasedEscrows: List<EscrowEntity> = emptyList(),
    allRefundedEscrows: List<EscrowEntity> = emptyList(),
    viewModel: SomadhanViewModel,
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val platformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
    val allBids by viewModel.allBids.collectAsStateWithLifecycle()

    val heldEscrowsFromVm by viewModel.allHeldEscrows.collectAsStateWithLifecycle()
    val releasedEscrowsFromVm by viewModel.allReleasedEscrows.collectAsStateWithLifecycle()
    val refundedEscrowsFromVm by viewModel.allRefundedEscrows.collectAsStateWithLifecycle()

    val allEscrows = remember(
        allHeldEscrows, allReleasedEscrows, allRefundedEscrows,
        heldEscrowsFromVm, releasedEscrowsFromVm, refundedEscrowsFromVm
    ) {
        val combined = (allHeldEscrows + allReleasedEscrows + allRefundedEscrows +
                heldEscrowsFromVm + releasedEscrowsFromVm + refundedEscrowsFromVm)
        combined.distinctBy { it.id }
    }

    val isGlobalInstantEnabled = platformSettings.find { it.key == "instant_jobs_global_enabled" }?.value != "false"
    val defaultRadiusSetting = platformSettings.find { it.key == "instant_job_default_radius_km" || it.key == "instant_jobs_default_radius_km" }?.value ?: "5.0"
    val broadcastTimeoutSecondsSetting = platformSettings.find { it.key == "instant_job_broadcast_timeout_seconds" }?.value ?: "300"
    val arrivalRadiusMetersSetting = platformSettings.find { it.key == "instant_job_arrival_radius_meters" }?.value ?: "200"

    var radiusInput by remember(defaultRadiusSetting) { mutableStateOf(defaultRadiusSetting) }
    var broadcastTimeoutMinutesInput by remember(broadcastTimeoutSecondsSetting) {
        val sec = broadcastTimeoutSecondsSetting.toDoubleOrNull() ?: 300.0
        val mins = (sec / 60.0).let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it) }
        mutableStateOf(mins)
    }
    var arrivalRadiusMetersInput by remember(arrivalRadiusMetersSetting) { mutableStateOf(arrivalRadiusMetersSetting) }

    var showConfigSection by remember { mutableStateOf(false) }
    var showCategoryConfigDialog by remember { mutableStateOf(false) }
    var selectedStatusFilter by rememberSaveable { mutableStateOf("ALL") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("ALL") }

    // Pagination
    val pageSize = 10
    var currentPage by rememberSaveable { mutableIntStateOf(1) }

    var actionTargetProblem by remember { mutableStateOf<ProblemEntity?>(null) }
    var actionType by remember { mutableStateOf("CANCEL") } // CANCEL, REBROADCAST, TO_NORMAL_BIDDING
    var actionReason by remember { mutableStateOf("") }
    var isSubmittingAction by remember { mutableStateOf(false) }

    // Filter problems that are instant jobs or marked as emergency
    val instantProblems = remember(allProblems) {
        allProblems.filter { it.isInstantJob || it.urgency == "জরুরি" || it.urgency == "খুব জরুরি" }
    }

    // Dynamic Counts for Progress Tabs
    val totalCount = instantProblems.size
    val broadcastingCount = instantProblems.count {
        it.jobStatus == "BROADCASTING" || (it.isInstantJob && it.status == "PENDING" && it.acceptedSolverId == null)
    }
    val enRouteCount = instantProblems.count {
        it.jobStatus in listOf("ACCEPTED", "ON_THE_WAY", "ARRIVED")
    }
    val startedCount = instantProblems.count {
        it.jobStatus == "STARTED"
    }
    val completedCount = instantProblems.count {
        it.jobStatus == "JOB_COMPLETED" || it.status == "RESOLVED" || it.status == "COMPLETED"
    }
    val cancelledCount = instantProblems.count {
        it.jobStatus == "CANCELLED" || it.status == "CANCELLED"
    }

    // Filtered list based on status, category, search query (including Post ID and Escrow ID)
    val filteredList = remember(instantProblems, selectedStatusFilter, selectedCategoryFilter, searchQuery, allEscrows) {
        instantProblems.filter { prob ->
            val tiedEscrow = allEscrows.find { it.problemId == prob.id }
            val matchesStatus = when (selectedStatusFilter) {
                "BROADCASTING" -> prob.jobStatus == "BROADCASTING" || (prob.isInstantJob && prob.status == "PENDING" && prob.acceptedSolverId == null)
                "EN_ROUTE" -> prob.jobStatus in listOf("ACCEPTED", "ON_THE_WAY", "ARRIVED")
                "STARTED" -> prob.jobStatus == "STARTED"
                "COMPLETED" -> prob.jobStatus == "JOB_COMPLETED" || prob.status == "RESOLVED" || prob.status == "COMPLETED"
                "CANCELLED" -> prob.jobStatus == "CANCELLED" || prob.status == "CANCELLED"
                else -> true
            }

            val matchesCategory = if (selectedCategoryFilter == "ALL") true else prob.categoryId == selectedCategoryFilter

            val query = searchQuery.trim().lowercase()
            val matchesSearch = if (query.isBlank()) true else {
                prob.id.lowercase().contains(query) ||
                        prob.title.lowercase().contains(query) ||
                        prob.userName.lowercase().contains(query) ||
                        prob.userId.lowercase().contains(query) ||
                        prob.userPhone.lowercase().contains(query) ||
                        (prob.acceptedSolverName?.lowercase()?.contains(query) == true) ||
                        (prob.acceptedSolverId?.lowercase()?.contains(query) == true) ||
                        prob.categoryName.lowercase().contains(query) ||
                        (tiedEscrow != null && tiedEscrow.id.lowercase().contains(query))
            }

            matchesStatus && matchesCategory && matchesSearch
        }.sortedByDescending { it.createdAt }
    }

    // Reset pagination when search query or filter changes
    LaunchedEffect(selectedStatusFilter, selectedCategoryFilter, searchQuery) {
        currentPage = 1
    }

    val totalPages = max(1, ceil(filteredList.size.toDouble() / pageSize).toInt())
    val safePage = currentPage.coerceIn(1, totalPages)
    val startIndex = (safePage - 1) * pageSize
    val endIndex = minOf(startIndex + pageSize, filteredList.size)
    val pagedList = remember(filteredList, safePage) {
        if (startIndex < filteredList.size) filteredList.subList(startIndex, endIndex) else emptyList()
    }

    val listState = rememberLazyListState()
    // Ground Rule ২০ — raw currentPage-এ key করা (coerced safePage না), যাতে action-জনিত passive
    // page-shrink-এ ভুলভাবে scroll-to-top ট্রিগার না হয়।
    LaunchedEffect(currentPage, selectedStatusFilter, selectedCategoryFilter, searchQuery) {
        listState.scrollToItem(0)
    }

    // Ground Rule ২০, সেশন ২.২৪ — ফিল্টার/স্ট্যাটাস/ক্যাটাগরি/সার্চ/পেজ বদলে দৃশ্যমান সব
    // ইনস্ট্যান্ট-জব কার্ড একসাথে ছোট্ট করে pulse করবে (Withdrawal/Problems/Transactions/
    // DirectContracts-এর একই প্যাটার্ন)। currentPage ইচ্ছাকৃতভাবে key-তে আছে।
    var isFilterRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(selectedStatusFilter, selectedCategoryFilter, searchQuery, currentPage) {
        isFilterRefreshing = true
        try {
            kotlinx.coroutines.delay(350L)
        } finally {
            isFilterRefreshing = false
        }
    }

    // Helper: copy to clipboard
    fun copyToClipboard(label: String, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "$label কপি হয়েছে: $text", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "কপি করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
        }
    }

    // Category Instant Settings Dialog
    if (showCategoryConfigDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showCategoryConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = SomadhanOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ক্যাটাগরিভিত্তিক জরুরি জব সেটিংস", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allCategories, key = { it.id }) { cat ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (cat.instantJobEnabled) SomadhanOrangeLight.copy(alpha = 0.35f) else SomadhanBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (cat.instantJobEnabled) SomadhanOrange.copy(alpha = 0.5f) else SomadhanDivider),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.nameBangla,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SomadhanTextPrimary
                                    )
                                    Text(
                                        text = "ডিফল্ট রেডিয়াস: ${DistanceUtil.toBengaliDigits(cat.instantJobRadiusKm.toInt().toString())} কিমি",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }

                                Switch(
                                    checked = cat.instantJobEnabled,
                                    onCheckedChange = { checked ->
                                        viewModel.adminToggleCategoryInstantJob(cat.id, checked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = SomadhanOrange,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = SomadhanDivider
                                    ),
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCategoryConfigDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    // Admin Force Action Dialog
    if (actionTargetProblem != null) {
        val prob = actionTargetProblem!!
        val tiedEscrow = allEscrows.find { it.problemId == prob.id }
        BottomSlideAlertDialog(
            onDismissRequest = {
                if (!isSubmittingAction) {
                    actionTargetProblem = null
                    actionReason = ""
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (actionType) {
                            "CANCEL" -> Icons.Default.Cancel
                            else -> Icons.Default.FlashOn
                        },
                        contentDescription = null,
                        tint = when (actionType) {
                            "CANCEL" -> SomadhanError
                            else -> SomadhanOrange
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "অ্যাডমিন ফোর্স অ্যাকশন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SomadhanTextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "কাজের শিরোনাম: ${prob.title}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SomadhanTextPrimary
                    )
                    Text(
                        text = "পোস্ট আইডি: ${prob.id} | গ্রাহক UID: ${allUsers.find { it.id == prob.userId }?.displayUid?.takeIf { uid -> uid.isNotBlank() } ?: prob.userId}",
                        fontSize = 11.sp,
                        color = SomadhanTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!prob.acceptedSolverId.isNullOrBlank()) {
                        Text(
                            text = "সলভার: ${prob.acceptedSolverName ?: "অজ্ঞাত"} (UID: ${allUsers.find { it.id == prob.acceptedSolverId }?.displayUid?.takeIf { uid -> uid.isNotBlank() } ?: prob.acceptedSolverId})",
                            fontSize = 11.sp,
                            color = SomadhanOrange,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (tiedEscrow != null) {
                        Text(
                            text = "এসক্রো ফান্ড: ৳${DistanceUtil.toBengaliDigits(tiedEscrow.baseAmount.toInt().toString())} (স্ট্যাটাস: ${tiedEscrow.status})",
                            fontSize = 11.sp,
                            color = SomadhanInfo
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "অ্যাকশনের ধরন নির্বাচন করুন:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // 1: Cancel & Refund
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (actionType == "CANCEL") SomadhanErrorLight.copy(alpha = 0.4f) else SomadhanBg)
                            .border(1.dp, if (actionType == "CANCEL") SomadhanError else SomadhanDivider, RoundedCornerShape(8.dp))
                            .clickable { actionType = "CANCEL" }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = actionType == "CANCEL",
                            onClick = { actionType = "CANCEL" },
                            colors = RadioButtonDefaults.colors(selectedColor = SomadhanError)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("কাজ বাতিল ও পূর্ণ রিফান্ড", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = SomadhanError)
                            Text("কাজটি সম্পূর্ণ বাতিল হবে এবং কাস্টমারকে অর্থ রিফান্ড করা হবে।", fontSize = 10.5.sp, color = SomadhanTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2: Rebroadcast
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (actionType == "REBROADCAST") SomadhanOrangeLight.copy(alpha = 0.4f) else SomadhanBg)
                            .border(1.dp, if (actionType == "REBROADCAST") SomadhanOrange else SomadhanDivider, RoundedCornerShape(8.dp))
                            .clickable { actionType = "REBROADCAST" }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = actionType == "REBROADCAST",
                            onClick = { actionType = "REBROADCAST" },
                            colors = RadioButtonDefaults.colors(selectedColor = SomadhanOrange)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("পুনরায় ব্রডকাস্ট করুন 📡", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = SomadhanOrange)
                            Text("বর্তমান সলভারকে রিমুভ করে আশেপাশের অন্য সলভারদের কাছে পাঠানো হবে।", fontSize = 10.5.sp, color = SomadhanTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3: To Normal Bidding
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (actionType == "TO_NORMAL_BIDDING") SomadhanInfo.copy(alpha = 0.1f) else SomadhanBg)
                            .border(1.dp, if (actionType == "TO_NORMAL_BIDDING") SomadhanInfo else SomadhanDivider, RoundedCornerShape(8.dp))
                            .clickable { actionType = "TO_NORMAL_BIDDING" }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = actionType == "TO_NORMAL_BIDDING",
                            onClick = { actionType = "TO_NORMAL_BIDDING" },
                            colors = RadioButtonDefaults.colors(selectedColor = SomadhanInfo)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("সাধারণ বিডিংয়ে রূপান্তর 📋", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = SomadhanInfo)
                            Text("জরুরি ব্রডকাস্ট বন্ধ করে স্বাভাবিক উন্মুক্ত বিডিং তালিকায় দেওয়া হবে।", fontSize = 10.5.sp, color = SomadhanTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = actionReason,
                        onValueChange = { actionReason = it },
                        label = { Text("অ্যাকশনের কারণ (বাধ্যতামূলক)") },
                        placeholder = { Text("যেমন: সলভার অন-টাইমে সাড়া দেননি / কাস্টমারের অভিযোগ") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (actionReason.trim().isNotBlank()) {
                            isSubmittingAction = true
                            viewModel.adminForceCancelInstantJob(
                                problemId = prob.id,
                                reason = actionReason.trim(),
                                targetAction = actionType,
                                onSuccess = {
                                    isSubmittingAction = false
                                    actionTargetProblem = null
                                    actionReason = ""
                                }
                            )
                        } else {
                            viewModel.showToast("অনুগ্রহ করে অ্যাকশনের কারণ উল্লেখ করুন")
                        }
                    },
                    enabled = !isSubmittingAction && actionReason.trim().isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionType == "CANCEL") SomadhanError else SomadhanOrange
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSubmittingAction) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("নিশ্চিত করুন")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        actionTargetProblem = null
                        actionReason = ""
                    },
                    enabled = !isSubmittingAction
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. Top Header Banner & Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SomadhanOrange.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SomadhanOrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ইনস্ট্যান্ট ও জরুরি জবস কন্ট্রোল",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                                if (broadcastingCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SomadhanOrange)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "📡 ${DistanceUtil.toBengaliDigits(broadcastingCount.toString())} লাইভ",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "জরুরি কাজের রিয়েল-টাইম মনিটরিং ও কন্ট্রোল প্যানেল",
                                fontSize = 11.sp,
                                color = SomadhanTextHint
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showConfigSection = !showConfigSection },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (showConfigSection) SomadhanOrange else SomadhanDivider),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (showConfigSection) SomadhanOrange else SomadhanTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showConfigSection) "লুকান" else "সেটিংস",
                            fontSize = 11.sp,
                            color = if (showConfigSection) SomadhanOrange else SomadhanTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Global Status Indicator Banner
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isGlobalInstantEnabled) SomadhanSuccessLight.copy(alpha = 0.4f) else SomadhanErrorLight.copy(alpha = 0.4f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isGlobalInstantEnabled) SomadhanSuccess else SomadhanError)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGlobalInstantEnabled) "ইনস্ট্যান্ট সেবা সারাদেশে সম্পূর্ণ চালু আছে" else "ইনস্ট্যান্ট সেবা সাময়িকভাবে বন্ধ আছে",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGlobalInstantEnabled) SomadhanSuccess else SomadhanError
                        )
                    }

                    Switch(
                        checked = isGlobalInstantEnabled,
                        onCheckedChange = { checked ->
                            viewModel.adminUpdatePlatformSetting("instant_jobs_global_enabled", if (checked) "true" else "false")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SomadhanOrange,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = SomadhanDivider
                        ),
                        modifier = Modifier.scale(0.75f)
                    )
                }

                // Collapsible Settings Configuration Panel
                AnimatedVisibility(
                    visible = showConfigSection,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Divider(color = SomadhanDivider, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "ইনস্ট্যান্ট জব গ্লোবাল প্যারামিটার",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = broadcastTimeoutMinutesInput,
                                onValueChange = { broadcastTimeoutMinutesInput = it },
                                label = { Text("ব্রডকাস্ট টাইমআউট (মিনিট)", fontSize = 10.5.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SomadhanOrange,
                                    unfocusedBorderColor = SomadhanBorder
                                )
                            )

                            OutlinedTextField(
                                value = arrivalRadiusMetersInput,
                                onValueChange = { arrivalRadiusMetersInput = it },
                                label = { Text("পৌঁছানোর রেডিয়াস (মিটার)", fontSize = 10.5.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SomadhanOrange,
                                    unfocusedBorderColor = SomadhanBorder
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = radiusInput,
                                onValueChange = { radiusInput = it },
                                label = { Text("অনুসন্ধান রেডিয়াস (কিমি)", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SomadhanOrange,
                                    unfocusedBorderColor = SomadhanBorder
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showCategoryConfigDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SomadhanOrange),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ক্যাটাগরি ভিত্তিক জরুরি সুইচ", fontSize = 11.sp, color = SomadhanOrange, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    val r = radiusInput.toDoubleOrNull()
                                    val bMins = broadcastTimeoutMinutesInput.toDoubleOrNull()
                                    val aRadius = arrivalRadiusMetersInput.toDoubleOrNull()
                                    if (r != null && r > 0 && bMins != null && bMins > 0 && aRadius != null && aRadius > 0) {
                                        val bSecs = (bMins * 60).toLong()
                                        viewModel.adminUpdatePlatformSetting("instant_job_default_radius_km", r.toString())
                                        viewModel.adminUpdatePlatformSetting("instant_jobs_default_radius_km", r.toString())
                                        viewModel.adminUpdatePlatformSetting("instant_job_broadcast_timeout_seconds", bSecs.toString())
                                        viewModel.adminUpdatePlatformSetting("instant_jobs_timeout_seconds", bSecs.toString())
                                        viewModel.adminUpdatePlatformSetting("instant_job_arrival_radius_meters", aRadius.toInt().toString())
                                        viewModel.showToast("ইনস্ট্যান্ট জব সেটিংস সংরক্ষিত হয়েছে ✅")
                                    } else {
                                        viewModel.showToast("সঠিক সংখ্যা প্রদান করুন")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("সংরক্ষণ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Search Bar (Supports Post ID, Escrow ID, User UID, Solver UID, Title, Phone, Names)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("পোস্ট ID, এসক্রো ID, UID, কাজের নাম বা নাম দিয়ে খুঁজুন...", fontSize = 12.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = SomadhanTextHint, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "মুছুন", tint = SomadhanTextHint, modifier = Modifier.size(16.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SomadhanOrange,
                unfocusedBorderColor = SomadhanDivider
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Dynamic Progress Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                Triple("ALL", "সকল (${DistanceUtil.toBengaliDigits(totalCount.toString())})", Icons.Default.FilterList),
                Triple("BROADCASTING", "ব্রডকাস্টিং (${DistanceUtil.toBengaliDigits(broadcastingCount.toString())})", Icons.Default.FlashOn),
                Triple("EN_ROUTE", "রওয়ানা / অন-সাইট (${DistanceUtil.toBengaliDigits(enRouteCount.toString())})", Icons.Default.DirectionsRun),
                Triple("STARTED", "কাজ চলছে (${DistanceUtil.toBengaliDigits(startedCount.toString())})", Icons.Default.Settings),
                Triple("COMPLETED", "সম্পন্ন (${DistanceUtil.toBengaliDigits(completedCount.toString())})", Icons.Default.CheckCircle),
                Triple("CANCELLED", "বাতিল (${DistanceUtil.toBengaliDigits(cancelledCount.toString())})", Icons.Default.Cancel)
            ).forEach { (filterKey, label, icon) ->
                val isSelected = selectedStatusFilter == filterKey
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) SomadhanOrange else SomadhanCardBg
                        )
                        .border(
                            1.dp,
                            if (isSelected) SomadhanOrange else SomadhanDivider,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedStatusFilter = filterKey }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else SomadhanTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else SomadhanTextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Jobs List (Weighted to allow bottom fixed pagination)
        if (pagedList.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = SomadhanTextHint,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "অনুসন্ধানের সাথে কোনো ইনস্ট্যান্ট জব মেলেনি।" else "এই ক্যাটাগরিতে কোনো ইনস্ট্যান্ট জব নেই।",
                            fontSize = 13.sp,
                            color = SomadhanTextHint
                        )
                    }
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
                items(pagedList, key = { it.id }) { problem ->
                    val poster = allUsers.find { it.id == problem.userId }
                    val solver = allUsers.find { it.id == problem.acceptedSolverId }
                    val tiedEscrow = allEscrows.find { it.problemId == problem.id }

                    val currentJobStatus = problem.jobStatus ?: if (problem.status == "COMPLETED" || problem.status == "RESOLVED") "JOB_COMPLETED"
                    else if (problem.status == "CANCELLED") "CANCELLED"
                    else "BROADCASTING"

                    // Ground Rule ১৯ — per-item pulse (শুধু যে জবের ডেটা সত্যিই বদলেছে তার কার্ডই
                    // action-এ pulse করবে), flashOnReentry = false। Ground Rule ২০ —
                    // isFilterRefreshing OR করা। GR21 এই ট্যাবে ইচ্ছাকৃতভাবে বাদ (ব্যবহারকারীর
                    // সিদ্ধান্ত, সেশন ২.২৪)।
                    val jobCardPulse = rememberFieldChangePulse(
                        value = problem,
                        isManualRefreshing = isRefreshing,
                        sessionKey = "admin_instant_jobs_sync",
                        viewModel = viewModel,
                        flashOnReentry = false
                    )
                    PulsingValue(isUpdating = jobCardPulse || isFilterRefreshing) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (currentJobStatus == "BROADCASTING") 1.5.dp else 1.dp,
                                color = when (currentJobStatus) {
                                    "BROADCASTING" -> SomadhanOrange
                                    "ACCEPTED", "ON_THE_WAY", "ARRIVED", "STARTED" -> SomadhanInfo
                                    "JOB_COMPLETED" -> SomadhanSuccess
                                    "CANCELLED" -> SomadhanError
                                    else -> SomadhanDivider
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header Row: Emergency Badge, Category, Date/Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SomadhanOrangeLight)
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.FlashOn,
                                                contentDescription = null,
                                                tint = SomadhanOrange,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "জরুরি জব ⚡",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanOrange
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${problem.categoryName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanTextSecondary
                                    )
                                }

                                Text(
                                    text = Formatters.formatDateBengali(problem.createdAt),
                                    fontSize = 11.sp,
                                    color = SomadhanTextHint
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Job Title & Description
                            Text(
                                text = problem.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            if (problem.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = problem.description,
                                    fontSize = 12.sp,
                                    color = SomadhanTextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // POST ID & ESCROW ID SECTION (Directly under headline with one-click copy)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SomadhanBg)
                                    .border(1.dp, SomadhanDivider.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Post ID with copy
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        copyToClipboard("Post ID", problem.id)
                                    }
                                ) {
                                    Text(
                                        text = "পোস্ট ID: ",
                                        fontSize = 10.5.sp,
                                        color = SomadhanTextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "#${problem.id.take(12)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = SomadhanOrange
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Post ID",
                                        tint = SomadhanTextHint,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }

                                // Escrow ID with copy
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        if (tiedEscrow != null) {
                                            copyToClipboard("Escrow ID", tiedEscrow.id)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "এসক্রো ID: ",
                                        fontSize = 10.5.sp,
                                        color = SomadhanTextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (tiedEscrow != null) {
                                        Text(
                                            text = "#${tiedEscrow.id.take(10)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = when (tiedEscrow.status) {
                                                "HELD" -> SomadhanInfo
                                                "RELEASED" -> SomadhanSuccess
                                                "REFUNDED" -> SomadhanError
                                                else -> SomadhanTextPrimary
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Escrow ID",
                                            tint = SomadhanTextHint,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "অপেক্ষমাণ",
                                            fontSize = 10.5.sp,
                                            color = SomadhanTextHint
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // DYNAMIC LIVE PROGRESS BAR & STATUS
                            val progressStep = when (currentJobStatus) {
                                "BROADCASTING" -> 1
                                "ACCEPTED" -> 2
                                "ON_THE_WAY" -> 2
                                "ARRIVED" -> 3
                                "STARTED" -> 4
                                "JOB_COMPLETED" -> 5
                                "CANCELLED" -> 0
                                else -> 1
                            }

                            // Visual Multi-segment Progress Bar
                            if (progressStep > 0) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val steps = listOf("ব্রডকাস্টিং", "রওয়ানা", "অন-সাইট", "কাজ চলছে", "সম্পন্ন")
                                        steps.forEachIndexed { index, stepLabel ->
                                            val stepNum = index + 1
                                            val isCurrentOrPassed = progressStep >= stepNum
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(
                                                        if (isCurrentOrPassed) {
                                                            if (progressStep == 5) SomadhanSuccess else SomadhanOrange
                                                        } else SomadhanDivider
                                                    )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }

                            // Live Stage Banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (currentJobStatus) {
                                            "BROADCASTING" -> SomadhanOrangeLight.copy(alpha = 0.5f)
                                            "ACCEPTED" -> SomadhanInfo.copy(alpha = 0.12f)
                                            "ON_THE_WAY" -> Color(0xFFE0E7FF)
                                            "ARRIVED" -> Color(0xFFFEF3C7)
                                            "STARTED" -> Color(0xFFFDE68A)
                                            "JOB_COMPLETED" -> SomadhanSuccessLight.copy(alpha = 0.5f)
                                            "CANCELLED" -> SomadhanErrorLight.copy(alpha = 0.4f)
                                            else -> SomadhanBg
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (currentJobStatus) {
                                            "BROADCASTING" -> Icons.Default.FlashOn
                                            "ACCEPTED" -> Icons.Default.Check
                                            "ON_THE_WAY" -> Icons.Default.DirectionsRun
                                            "ARRIVED" -> Icons.Default.LocationOn
                                            "STARTED" -> Icons.Default.Settings
                                            "JOB_COMPLETED" -> Icons.Default.CheckCircle
                                            "CANCELLED" -> Icons.Default.Cancel
                                            else -> Icons.Default.Check
                                        },
                                        contentDescription = null,
                                        tint = when (currentJobStatus) {
                                            "BROADCASTING" -> SomadhanOrange
                                            "ACCEPTED", "ON_THE_WAY" -> SomadhanInfo
                                            "ARRIVED", "STARTED" -> Color(0xFFD97706)
                                            "JOB_COMPLETED" -> SomadhanSuccess
                                            "CANCELLED" -> SomadhanError
                                            else -> SomadhanTextSecondary
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (currentJobStatus) {
                                            "BROADCASTING" -> "📡 ব্রডকাস্টিং চলছে (সলভারের অপেক্ষা)"
                                            "ACCEPTED" -> "🤝 সলভার গ্রহণ করেছেন"
                                            "ON_THE_WAY" -> "🚀 সলভার লোকেশনের উদ্দেশ্যে রওয়ানা হয়েছেন"
                                            "ARRIVED" -> "📍 সলভার লোকেশনে পৌঁছে গেছেন"
                                            "STARTED" -> "⚡ কাজ চলছে"
                                            "JOB_COMPLETED" -> "✅ কাজ সফলভাবে সম্পন্ন হয়েছে"
                                            "CANCELLED" -> "❌ কাজটি বাতিল করা হয়েছে"
                                            else -> currentJobStatus
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (currentJobStatus) {
                                            "BROADCASTING" -> SomadhanOrange
                                            "ACCEPTED", "ON_THE_WAY" -> SomadhanInfo
                                            "ARRIVED", "STARTED" -> Color(0xFFD97706)
                                            "JOB_COMPLETED" -> SomadhanSuccess
                                            "CANCELLED" -> SomadhanError
                                            else -> SomadhanTextPrimary
                                        }
                                    )
                                }

                                if (problem.broadcastRadiusKm != null) {
                                    Text(
                                        text = "রেডিয়াস: ${DistanceUtil.toBengaliDigits(problem.broadcastRadiusKm.toInt().toString())} কিমি",
                                        fontSize = 10.5.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = SomadhanDivider.copy(alpha = 0.6f), thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Customer & Solver Info Column (UID displayed instead of phone number)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Customer Info Column
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "পোস্টকারী (গ্রাহক)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = problem.userName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanTextPrimary
                                    )
                                    // Customer UID
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            copyToClipboard("Customer UID", poster?.displayUid?.takeIf { it.isNotBlank() } ?: problem.userId)
                                        }
                                    ) {
                                        Text(
                                            text = "UID: ${(poster?.displayUid?.takeIf { it.isNotBlank() } ?: problem.userId).take(12)}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = SomadhanTextSecondary
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Customer UID",
                                            tint = SomadhanTextHint,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                    if (problem.userAddress.isNotBlank()) {
                                        Text(
                                            text = "📍 ${problem.userAddress}",
                                            fontSize = 10.5.sp,
                                            color = SomadhanTextHint,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Solver Info Column
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "সমাধানকারী",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    if (!problem.acceptedSolverName.isNullOrBlank() && !problem.acceptedSolverId.isNullOrBlank()) {
                                        Text(
                                            text = problem.acceptedSolverName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SomadhanOrange
                                        )
                                        // Solver UID
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                copyToClipboard("Solver UID", solver?.displayUid?.takeIf { it.isNotBlank() } ?: problem.acceptedSolverId)
                                            }
                                        ) {
                                            Text(
                                                text = "UID: ${(solver?.displayUid?.takeIf { it.isNotBlank() } ?: problem.acceptedSolverId).take(12)}",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = SomadhanTextSecondary
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Solver UID",
                                                tint = SomadhanTextHint,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                        if (problem.solverLiveLat != null && problem.solverLiveLng != null) {
                                            Text(
                                                text = "🛰️ লাইভ জিপিএস সক্রিয়",
                                                fontSize = 10.5.sp,
                                                color = SomadhanSuccess,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "অপেক্ষমাণ...",
                                            fontSize = 12.sp,
                                            color = SomadhanTextHint,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                        Text(
                                            text = "UID: নির্ধারিত হয়নি",
                                            fontSize = 11.sp,
                                            color = SomadhanTextHint
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Budget & Financial Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "বাজেট: ৳ ${DistanceUtil.toBengaliDigits((problem.acceptedAmount ?: problem.minBudget).toInt().toString())}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                }

                                if (problem.appliedCommissionRate != null) {
                                    Text(
                                        text = "কমিশন: ${DistanceUtil.toBengaliDigits(problem.appliedCommissionRate.toString())}%",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                            }

                            // Extra details & Release Request Status (BUG FIXED: Only shows pending if work is still active and escrow is HELD)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SomadhanBg)
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Bids Count
                                val bidsCount = allBids.count { it.problemId == problem.id }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "বিড: ",
                                        fontSize = 10.5.sp,
                                        color = SomadhanTextSecondary
                                    )
                                    Text(
                                        text = "${DistanceUtil.toBengaliDigits(bidsCount.toString())}টি",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (bidsCount > 0) SomadhanOrange else SomadhanTextHint
                                    )
                                }

                                // Confirmed Extra Amount Total
                                val confirmedExtra = problem.confirmedExtraAmountTotal
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "অতিরিক্ত: ",
                                        fontSize = 10.5.sp,
                                        color = SomadhanTextSecondary
                                    )
                                    Text(
                                        text = if (confirmedExtra > 0.0) "+৳${DistanceUtil.toBengaliDigits(confirmedExtra.toInt().toString())}" else "৳০",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (confirmedExtra > 0.0) SomadhanSuccess else SomadhanTextSecondary
                                    )
                                }

                                // RELEASE REQUEST STATUS LOGIC BUG FIX
                                val isJobFinished = currentJobStatus in listOf("JOB_COMPLETED", "COMPLETED", "RESOLVED")
                                val isJobCancelled = currentJobStatus in listOf("CANCELLED")
                                val isEscrowReleased = tiedEscrow?.status == "RELEASED"
                                val isEscrowRefunded = tiedEscrow?.status == "REFUNDED"

                                val (statusText, statusColor) = when {
                                    isJobFinished || isEscrowReleased -> "পেমেন্ট রিলিজড ✅" to SomadhanSuccess
                                    isJobCancelled || isEscrowRefunded -> "রিফান্ড সম্পন্ন ↩️" to SomadhanError
                                    problem.hasReleaseRequest && (tiedEscrow == null || tiedEscrow.status == "HELD") -> "রিলিজ রিকোয়েস্ট পেন্ডিং" to SomadhanOrange
                                    tiedEscrow?.status == "HELD" -> "ফান্ড জমা আছে 🔒" to SomadhanInfo
                                    else -> "স্বাভাবিক" to SomadhanTextHint
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = statusText,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (problem.hasReleaseRequest && !isJobFinished && !isEscrowReleased) FontWeight.Bold else FontWeight.Normal,
                                        color = statusColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = SomadhanDivider.copy(alpha = 0.6f), thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Action Buttons Row: Direct Call Options & Admin Control
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Call Customer Button
                                if (problem.userPhone.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${problem.userPhone}"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                viewModel.showToast("কল দেওয়া সম্ভব হয়নি: ${e.message}")
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, SomadhanDivider)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = SomadhanTextSecondary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("গ্রাহককে কল", fontSize = 11.sp, color = SomadhanTextSecondary)
                                    }
                                }

                                // Call Solver Button (if assigned)
                                if (solver?.phone?.isNotBlank() == true) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${solver.phone}"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                viewModel.showToast("কল দেওয়া সম্ভব হয়নি: ${e.message}")
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, SomadhanDivider)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("সলভারকে কল", fontSize = 11.sp, color = SomadhanOrange)
                                    }
                                }

                                // Admin Force Action Trigger Button
                                Button(
                                    onClick = {
                                        actionTargetProblem = problem
                                        actionType = "CANCEL"
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (currentJobStatus in listOf("JOB_COMPLETED", "CANCELLED")) SomadhanBg else SomadhanOrange
                                    ),
                                    border = if (currentJobStatus in listOf("JOB_COMPLETED", "CANCELLED")) BorderStroke(1.dp, SomadhanDivider) else null,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (currentJobStatus in listOf("JOB_COMPLETED", "CANCELLED")) SomadhanTextSecondary else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "অ্যাডমিন অ্যাকশন",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentJobStatus in listOf("JOB_COMPLETED", "CANCELLED")) SomadhanTextSecondary else Color.White
                                    )
                                }
                            }
                        }
                    }
                    } // close PulsingValue (Ground Rule ১৯/২০, সেশন ২.২৪)
                }
            }
        }

        // 5. Fixed Bottom Pagination Controls
        if (totalPages > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SomadhanDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "পৃষ্ঠা: ${DistanceUtil.toBengaliDigits(safePage.toString())} / ${DistanceUtil.toBengaliDigits(totalPages.toString())} (মোট ${DistanceUtil.toBengaliDigits(filteredList.size.toString())}টি)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = SomadhanTextSecondary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = safePage > 1,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, if (safePage > 1) SomadhanOrange else SomadhanDivider)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Page", modifier = Modifier.size(14.dp), tint = if (safePage > 1) SomadhanOrange else SomadhanTextHint)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("আগের", fontSize = 11.sp, color = if (safePage > 1) SomadhanOrange else SomadhanTextHint)
                        }

                        OutlinedButton(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = safePage < totalPages,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, if (safePage < totalPages) SomadhanOrange else SomadhanDivider)
                        ) {
                            Text("পরের", fontSize = 11.sp, color = if (safePage < totalPages) SomadhanOrange else SomadhanTextHint)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page", modifier = Modifier.size(14.dp), tint = if (safePage < totalPages) SomadhanOrange else SomadhanTextHint)
                        }
                    }
                }
            }
        }
    }
}
