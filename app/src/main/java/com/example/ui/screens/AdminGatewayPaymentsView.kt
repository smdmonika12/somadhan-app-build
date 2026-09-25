@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.GatewayPaymentEntity
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
import com.example.util.CsvExportUtil
import com.example.util.DistanceUtil
import com.example.util.Formatters
import com.example.ui.components.BottomSlideAlertDialog

@Composable
fun AdminGatewayPaymentsView(
    gatewayPayments: List<GatewayPaymentEntity>,
    allUsers: List<UserEntity>,
    viewModel: SomadhanViewModel? = null,
    isManualRefreshing: Boolean = false
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedGatewayFilter by rememberSaveable { mutableStateOf("ALL") }
    var selectedStatusFilter by rememberSaveable { mutableStateOf("ALL") }
    var selectedPurposeFilter by rememberSaveable { mutableStateOf("ALL") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedDetailPayment by remember { mutableStateOf<GatewayPaymentEntity?>(null) }
    var paymentForStatusUpdate by remember { mutableStateOf<GatewayPaymentEntity?>(null) }

    val pageSize = 12
    var currentPage by rememberSaveable { mutableIntStateOf(1) }

    LaunchedEffect(selectedGatewayFilter, selectedStatusFilter, selectedPurposeFilter, searchQuery) {
        currentPage = 1
    }

    // Ground Rule ২০ — সার্চ/ফিল্টার(গেটওয়ে/স্ট্যাটাস/পারপজ)/পেজ বদলে দৃশ্যমান সব কার্ড একসাথে
    // pulse করাবে (Transactions/InstantJobs-এর একই প্যাটার্ন, pagination-key সহ)।
    var isFilterRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(selectedGatewayFilter, selectedStatusFilter, selectedPurposeFilter, searchQuery, currentPage) {
        isFilterRefreshing = true
        try {
            kotlinx.coroutines.delay(350L)
        } finally {
            isFilterRefreshing = false
        }
    }

    // Ground Rule ২১, সেশন ২.২০-এ Transactions-এর জন্য বানানো
    // SupabaseRealtimeManager.recentlyChangedGatewayPaymentIds পুনর্ব্যবহার (Insert-only) — genuine
    // নতুন gateway payment এলে শুধু সেই কার্ডটাই pulse করবে। viewModel nullable বলে না থাকলে খালি
    // সেট ধরে নেওয়া হয়, তৃতীয় pulse-কারণটা নিষ্ক্রিয় থাকে।
    val recentlyChangedGatewayPaymentIds by remember(viewModel) {
        viewModel?.recentlyChangedGatewayPaymentIds ?: kotlinx.coroutines.flow.MutableStateFlow(emptySet<String>())
    }.collectAsStateWithLifecycle()

    val filteredPayments = remember(gatewayPayments, selectedGatewayFilter, selectedStatusFilter, selectedPurposeFilter, searchQuery) {
        val q = searchQuery.trim().lowercase()
        gatewayPayments.filter { p ->
            val matchesGateway = when (selectedGatewayFilter) {
                "BKASH" -> p.gateway.equals("BKASH", ignoreCase = true)
                "NAGAD" -> p.gateway.equals("NAGAD", ignoreCase = true)
                "ROCKET" -> p.gateway.equals("ROCKET", ignoreCase = true)
                "BANK" -> p.gateway.equals("BANK", ignoreCase = true)
                else -> true
            }

            val matchesStatus = when (selectedStatusFilter) {
                "SUCCESS" -> p.status.equals("SUCCESS", ignoreCase = true)
                "PENDING" -> p.status.equals("PENDING", ignoreCase = true)
                "FAILED" -> p.status.equals("FAILED", ignoreCase = true)
                "REFUNDED" -> p.status.equals("REFUNDED", ignoreCase = true)
                else -> true
            }

            val matchesPurpose = when (selectedPurposeFilter) {
                "WALLET_TOPUP" -> p.purpose.equals("WALLET_TOPUP", ignoreCase = true) || p.purpose.contains("TOPUP", ignoreCase = true)
                "ESCROW_PAYMENT" -> p.purpose.equals("ESCROW_PAYMENT", ignoreCase = true) || p.purpose.contains("ESCROW", ignoreCase = true)
                "EXTRA_BILL" -> p.purpose.equals("EXTRA_BILL", ignoreCase = true)
                else -> true
            }

            val matchesSearch = q.isEmpty() ||
                    p.id.lowercase().contains(q) ||
                    p.gatewayTrxId.lowercase().contains(q) ||
                    p.userId.lowercase().contains(q) ||
                    p.userPhone.lowercase().contains(q) ||
                    p.problemId.lowercase().contains(q) ||
                    p.problemTitle.lowercase().contains(q) ||
                    p.note.lowercase().contains(q)

            matchesGateway && matchesStatus && matchesPurpose && matchesSearch
        }.sortedByDescending { it.timestamp }
    }

    val totalPages = maxOf(1, (filteredPayments.size + pageSize - 1) / pageSize)
    val safePage = currentPage.coerceIn(1, totalPages)
    val paginatedList = remember(filteredPayments, safePage, pageSize) {
        val fromIndex = (safePage - 1) * pageSize
        if (fromIndex >= filteredPayments.size) {
            emptyList()
        } else {
            filteredPayments.subList(fromIndex, minOf(fromIndex + pageSize, filteredPayments.size))
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(safePage) {
        listState.scrollToItem(0)
    }

    // Totals
    val totalAmount = remember(gatewayPayments) { gatewayPayments.sumOf { it.amount } }
    val bkashTotal = remember(gatewayPayments) { gatewayPayments.filter { it.gateway.equals("BKASH", ignoreCase = true) }.sumOf { it.amount } }
    val nagadTotal = remember(gatewayPayments) { gatewayPayments.filter { it.gateway.equals("NAGAD", ignoreCase = true) }.sumOf { it.amount } }
    val rocketTotal = remember(gatewayPayments) { gatewayPayments.filter { it.gateway.equals("ROCKET", ignoreCase = true) }.sumOf { it.amount } }
    val bankTotal = remember(gatewayPayments) { gatewayPayments.filter { it.gateway.equals("BANK", ignoreCase = true) }.sumOf { it.amount } }

    val userMap = remember(allUsers) { allUsers.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("admin_gateway_payments_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Overview Cards
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SomadhanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                        .background(SomadhanOrange.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payment,
                                        contentDescription = null,
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "অনলাইন গেটওয়ে লেনদেন",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                    Text(
                                        text = "bKash, Nagad, Rocket ও ব্যাংক পেমেন্ট মনিটরিং",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    CsvExportUtil.exportToCsv(
                                        context = context,
                                        fileName = "gateway_payments_${System.currentTimeMillis()}",
                                        headers = listOf(
                                            "Payment ID",
                                            "Gateway",
                                            "Gateway TrxID",
                                            "Amount (BDT)",
                                            "Status",
                                            "Purpose",
                                            "User ID",
                                            "User Phone",
                                            "Sender Phone",
                                            "Problem Title",
                                            "Timestamp"
                                        ),
                                        rows = filteredPayments.map { p ->
                                            val u = userMap[p.userId]
                                            listOf(
                                                p.id,
                                                p.gateway,
                                                p.gatewayTrxId,
                                                p.amount.toString(),
                                                p.status,
                                                p.purpose,
                                                p.userId,
                                                u?.phone ?: "",
                                                p.userPhone,
                                                p.problemTitle,
                                                Formatters.formatDateTimeBengali(p.timestamp)
                                            )
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "CSV", modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV রিপোর্ট", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = SomadhanDivider, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GatewayStatCard(
                                title = "মোট ট্রানজেকশন",
                                count = gatewayPayments.size,
                                amount = totalAmount,
                                color = SomadhanOrange,
                                icon = Icons.Default.Payment,
                                isManualRefreshing = isManualRefreshing
                            )
                            GatewayStatCard(
                                title = "বিকাশ (bKash)",
                                count = gatewayPayments.count { it.gateway.equals("BKASH", ignoreCase = true) },
                                amount = bkashTotal,
                                color = Color(0xFFD12053),
                                icon = Icons.Default.PhoneAndroid,
                                isManualRefreshing = isManualRefreshing
                            )
                            GatewayStatCard(
                                title = "নগদ (Nagad)",
                                count = gatewayPayments.count { it.gateway.equals("NAGAD", ignoreCase = true) },
                                amount = nagadTotal,
                                color = Color(0xFFE31B23),
                                icon = Icons.Default.PhoneAndroid,
                                isManualRefreshing = isManualRefreshing
                            )
                            GatewayStatCard(
                                title = "রকেট (Rocket)",
                                count = gatewayPayments.count { it.gateway.equals("ROCKET", ignoreCase = true) },
                                amount = rocketTotal,
                                color = Color(0xFF8C3494),
                                icon = Icons.Default.PhoneAndroid,
                                isManualRefreshing = isManualRefreshing
                            )
                            GatewayStatCard(
                                title = "ব্যাংক (Bank)",
                                count = gatewayPayments.count { it.gateway.equals("BANK", ignoreCase = true) },
                                amount = bankTotal,
                                color = Color(0xFF1D4ED8),
                                icon = Icons.Default.AccountBalance,
                                isManualRefreshing = isManualRefreshing
                            )
                        }
                    }
                }
            }

            // 2. Search and Filter Bar
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SomadhanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("TrxID, ইউজার আইডি, মোবাইল নম্বর বা কাজের নাম দিয়ে খুঁজুন...", fontSize = 12.sp, color = SomadhanTextHint) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SomadhanTextSecondary, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SomadhanOrange,
                                unfocusedBorderColor = SomadhanBorder,
                                focusedContainerColor = SomadhanBg,
                                unfocusedContainerColor = SomadhanBg
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_gateway_search_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Gateway Provider Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedGatewayFilter == "ALL",
                                onClick = { selectedGatewayFilter = "ALL" },
                                label = { Text("সকল গেটওয়ে", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SomadhanOrange, selectedLabelColor = Color.White)
                            )
                            FilterChip(
                                selected = selectedGatewayFilter == "BKASH",
                                onClick = { selectedGatewayFilter = "BKASH" },
                                label = { Text("বিকাশ", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFD12053), selectedLabelColor = Color.White)
                            )
                            FilterChip(
                                selected = selectedGatewayFilter == "NAGAD",
                                onClick = { selectedGatewayFilter = "NAGAD" },
                                label = { Text("নগদ", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE31B23), selectedLabelColor = Color.White)
                            )
                            FilterChip(
                                selected = selectedGatewayFilter == "ROCKET",
                                onClick = { selectedGatewayFilter = "ROCKET" },
                                label = { Text("রকেট", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF8C3494), selectedLabelColor = Color.White)
                            )
                            FilterChip(
                                selected = selectedGatewayFilter == "BANK",
                                onClick = { selectedGatewayFilter = "BANK" },
                                label = { Text("ব্যাংক", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1D4ED8), selectedLabelColor = Color.White)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Purpose & Status Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedPurposeFilter == "ALL",
                                onClick = { selectedPurposeFilter = "ALL" },
                                label = { Text("সকল উদ্দেশ্য", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SomadhanOrange, selectedLabelColor = Color.White)
                            )
                            FilterChip(
                                selected = selectedPurposeFilter == "WALLET_TOPUP",
                                onClick = { selectedPurposeFilter = "WALLET_TOPUP" },
                                label = { Text("ওয়ালেট রিচার্জ", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SomadhanSuccess, selectedLabelColor = Color.White)
                            )
                            FilterChip(
                                selected = selectedPurposeFilter == "ESCROW_PAYMENT",
                                onClick = { selectedPurposeFilter = "ESCROW_PAYMENT" },
                                label = { Text("এসক্রো পেমেন্ট", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1D4ED8), selectedLabelColor = Color.White)
                            )
                            FilterChip(
                                selected = selectedStatusFilter == "SUCCESS",
                                onClick = { selectedStatusFilter = if (selectedStatusFilter == "SUCCESS") "ALL" else "SUCCESS" },
                                label = { Text("সফল (SUCCESS)", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SomadhanSuccess, selectedLabelColor = Color.White)
                            )
                            FilterChip(
                                selected = selectedStatusFilter == "PENDING",
                                onClick = { selectedStatusFilter = if (selectedStatusFilter == "PENDING") "ALL" else "PENDING" },
                                label = { Text("বিচারাধীন (PENDING)", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SomadhanOrange, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }
            }

            // 3. Transactions Count Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "গেটওয়ে লেনদেনের তালিকা (${DistanceUtil.toBengaliDigits(filteredPayments.size.toString())}টি)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                    if (totalPages > 1) {
                        Text(
                            text = "পৃষ্ঠা ${DistanceUtil.toBengaliDigits(safePage.toString())} / ${DistanceUtil.toBengaliDigits(totalPages.toString())}",
                            fontSize = 12.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }
            }

            // 4. Payment Items List
            if (filteredPayments.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        border = BorderStroke(1.dp, SomadhanBorder)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Payment, contentDescription = null, tint = SomadhanTextHint, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("কোনো গেটওয়ে লেনদেন পাওয়া যায়নি", fontSize = 13.sp, color = SomadhanTextSecondary)
                            }
                        }
                    }
                }
            } else {
                items(paginatedList, key = { it.id }) { payment ->
                    val user = userMap[payment.userId]
                    val gatewayColor = when (payment.gateway.uppercase()) {
                        "BKASH" -> Color(0xFFD12053)
                        "NAGAD" -> Color(0xFFE31B23)
                        "ROCKET" -> Color(0xFF8C3494)
                        "BANK" -> Color(0xFF1D4ED8)
                        else -> SomadhanOrange
                    }
                    val gatewayBengaliName = when (payment.gateway.uppercase()) {
                        "BKASH" -> "বিকাশ (bKash)"
                        "NAGAD" -> "নগদ (Nagad)"
                        "ROCKET" -> "রকেট (Rocket)"
                        "BANK" -> "ব্যাংক (Bank)"
                        else -> payment.gateway
                    }

                    // Ground Rule ১৮/১৯/২০/২১ — pull-to-refresh/re-entry (rememberFieldChangePulse),
                    // ফিল্টার/সার্চ/পেজ বদল (isFilterRefreshing), আর genuine নতুন Insert
                    // (isNewFromRealtime) — তিনটা স্বাধীন কারণেই শুধু এই কার্ডটাই pulse করবে।
                    val paymentCardPulse = rememberFieldChangePulse(
                        value = payment,
                        isManualRefreshing = isManualRefreshing,
                        sessionKey = "admin_gateway_payments_sync",
                        viewModel = viewModel,
                        flashOnReentry = false
                    )
                    val isNewFromRealtime = recentlyChangedGatewayPaymentIds.contains(payment.id)
                    PulsingValue(isUpdating = paymentCardPulse || isFilterRefreshing || isNewFromRealtime) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SomadhanBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_gateway_payment_card_${payment.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Gateway & Amount
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(gatewayColor.copy(alpha = 0.12f))
                                            .border(1.dp, gatewayColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = gatewayBengaliName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = gatewayColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when (payment.status.uppercase()) {
                                                    "SUCCESS" -> SomadhanSuccessLight
                                                    "PENDING" -> SomadhanOrangeLight
                                                    "FAILED" -> SomadhanErrorLight
                                                    else -> Color(0xFFF3F4F6)
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when (payment.status.uppercase()) {
                                                "SUCCESS" -> "সফল (SUCCESS)"
                                                "PENDING" -> "বিচারাধীন (PENDING)"
                                                "FAILED" -> "ব্যর্থ (FAILED)"
                                                "REFUNDED" -> "রিফান্ড (REFUNDED)"
                                                else -> payment.status
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (payment.status.uppercase()) {
                                                "SUCCESS" -> SomadhanSuccess
                                                "PENDING" -> SomadhanOrange
                                                "FAILED" -> SomadhanError
                                                else -> SomadhanTextSecondary
                                            }
                                        )
                                    }
                                }

                                Text(
                                    text = Formatters.formatTaka(payment.amount),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (payment.status.uppercase() == "SUCCESS") SomadhanSuccess else SomadhanTextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = SomadhanDivider, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Details Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "উদ্দেশ্য: ${
                                            when (payment.purpose.uppercase()) {
                                                "WALLET_TOPUP" -> "ওয়ালেট রিচার্জ / ব্যালেন্স ডিপোজিট"
                                                "ESCROW_PAYMENT" -> "কাজের এসক্রো পেমেন্ট"
                                                "EXTRA_BILL" -> "অতিরিক্ত বিল পেমেন্ট"
                                                else -> payment.purpose
                                            }
                                        }",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanTextPrimary
                                    )
                                    if (payment.problemId.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                clipboardManager.setText(AnnotatedString(payment.problemId))
                                                Toast.makeText(context, "পোস্ট আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text(
                                                text = "পোস্ট আইডি: #${payment.problemId.take(8).uppercase()}",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanOrange
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "কপি",
                                                tint = SomadhanOrange,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                    if (payment.problemTitle.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "কাজ: ${payment.problemTitle}",
                                            fontSize = 11.sp,
                                            color = SomadhanTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ইউজার: ${user?.name ?: "User #${payment.userId.take(8)}"}${if (!user?.phone.isNullOrBlank()) " (${user?.phone?.let { Formatters.toLocalDisplayFormat(it) }})" else ""}",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary
                                    )
                                    if (payment.userPhone.isNotBlank()) {
                                        Text(
                                            text = "প্রেরকের নম্বর: ${payment.userPhone}",
                                            fontSize = 11.sp,
                                            color = SomadhanTextHint
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = Formatters.formatDateTimeBengali(payment.timestamp),
                                        fontSize = 10.5.sp,
                                        color = SomadhanTextHint
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom Bar with TrxID & Action Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SomadhanBg)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(payment.gatewayTrxId))
                                            Toast.makeText(context, "TrxID কপি করা হয়েছে: ${payment.gatewayTrxId}", Toast.LENGTH_SHORT).show()
                                        }
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = "TrxID: ${payment.gatewayTrxId}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrange,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "কপি করুন",
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = { selectedDetailPayment = payment },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("রসিদ দেখুন", fontSize = 11.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                                    }

                                    if (viewModel != null) {
                                        var showStatusMenu by remember { mutableStateOf(false) }
                                        // ⚠️ dropdown menu item-এ inline error/text দেখানোর জায়গা নেই, তাই এখানে
                                        // row-level lock (মেনু বাটন disable + ছোট spinner) ব্যবহার করা হয়েছে;
                                        // VM-এর auto-toast (success/error দুটোতেই) ব্যবহারকারীর ফিডব্যাক হিসেবে যথেষ্ট।
                                        var isUpdatingGatewayStatus by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(
                                                onClick = { if (!isUpdatingGatewayStatus) showStatusMenu = true },
                                                enabled = !isUpdatingGatewayStatus,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                if (isUpdatingGatewayStatus) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(14.dp),
                                                        color = SomadhanTextSecondary,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Icon(Icons.Default.MoreVert, contentDescription = "মেনু", tint = SomadhanTextSecondary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = showStatusMenu,
                                                onDismissRequest = { showStatusMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("স্ট্যাটাস: SUCCESS করুন", fontSize = 12.sp, color = SomadhanSuccess) },
                                                    onClick = {
                                                        showStatusMenu = false
                                                        isUpdatingGatewayStatus = true
                                                        viewModel.adminUpdateGatewayPaymentStatus(
                                                            payment.id, "SUCCESS",
                                                            onSuccess = { isUpdatingGatewayStatus = false },
                                                            onError = { isUpdatingGatewayStatus = false }
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("স্ট্যাটাস: PENDING করুন", fontSize = 12.sp, color = SomadhanOrange) },
                                                    onClick = {
                                                        showStatusMenu = false
                                                        isUpdatingGatewayStatus = true
                                                        viewModel.adminUpdateGatewayPaymentStatus(
                                                            payment.id, "PENDING",
                                                            onSuccess = { isUpdatingGatewayStatus = false },
                                                            onError = { isUpdatingGatewayStatus = false }
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("স্ট্যাটাস: FAILED করুন", fontSize = 12.sp, color = SomadhanError) },
                                                    onClick = {
                                                        showStatusMenu = false
                                                        isUpdatingGatewayStatus = true
                                                        viewModel.adminUpdateGatewayPaymentStatus(
                                                            payment.id, "FAILED",
                                                            onSuccess = { isUpdatingGatewayStatus = false },
                                                            onError = { isUpdatingGatewayStatus = false }
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("স্ট্যাটাস: REFUNDED করুন", fontSize = 12.sp, color = Color(0xFF1D4ED8)) },
                                                    onClick = {
                                                        showStatusMenu = false
                                                        isUpdatingGatewayStatus = true
                                                        viewModel.adminUpdateGatewayPaymentStatus(
                                                            payment.id, "REFUNDED",
                                                            onSuccess = { isUpdatingGatewayStatus = false },
                                                            onError = { isUpdatingGatewayStatus = false }
                                                        )
                                                    }
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

            // 5. Pagination Buttons
            if (totalPages > 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = currentPage > 1,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("পূর্ববর্তী", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${DistanceUtil.toBengaliDigits(safePage.toString())} / ${DistanceUtil.toBengaliDigits(totalPages.toString())}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = currentPage < totalPages,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("পরবর্তী", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Detail Receipt Dialog
    selectedDetailPayment?.let { payment ->
        val user = userMap[payment.userId]
        BottomSlideAlertDialog(
            onDismissRequest = { selectedDetailPayment = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = SomadhanBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("অনলাইন গেটওয়ে পেমেন্ট রসিদ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReceiptRow("পেমেন্ট আইডি", payment.id)
                    ReceiptRow("গেটওয়ে মাধ্যম", payment.gateway)
                    ReceiptRow("গেটওয়ে TrxID", payment.gatewayTrxId)
                    ReceiptRow("টাকার পরিমাণ", Formatters.formatTaka(payment.amount))
                    ReceiptRow("বর্তমান স্ট্যাটাস", payment.status)
                    ReceiptRow("উদ্দেশ্য (Purpose)", payment.purpose)
                    ReceiptRow("গ্রাহক আইডি", payment.userId)
                    ReceiptRow("গ্রাহকের নাম", user?.name ?: "N/A")
                    ReceiptRow("গ্রাহকের মোবাইল", user?.phone?.let { Formatters.toLocalDisplayFormat(it) } ?: "N/A")
                    if (payment.userPhone.isNotBlank()) {
                        ReceiptRow("প্রেরকের ফোন নম্বর", payment.userPhone)
                    }
                    if (payment.problemId.isNotBlank()) {
                        ReceiptRow("সমস্যা আইডি", payment.problemId)
                    }
                    if (payment.problemTitle.isNotBlank()) {
                        ReceiptRow("কাজের শিরোনাম", payment.problemTitle)
                    }
                    if (payment.note.isNotBlank()) {
                        ReceiptRow("মন্তব্য / নোট", payment.note)
                    }
                    ReceiptRow("তারিখ ও সময়", Formatters.formatDateTimeBengali(payment.timestamp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val textToCopy = buildString {
                            appendLine("--- সমাধান গেটওয়ে লেনদেন রসিদ ---")
                            appendLine("পেমেন্ট আইডি: ${payment.id}")
                            appendLine("মাধ্যম: ${payment.gateway}")
                            appendLine("TrxID: ${payment.gatewayTrxId}")
                            appendLine("পরিমাণ: ৳${payment.amount}")
                            appendLine("স্ট্যাটাস: ${payment.status}")
                            appendLine("উদ্দেশ্য: ${payment.purpose}")
                            if (payment.problemId.isNotBlank()) {
                                appendLine("পোস্ট আইডি: ${payment.problemId}")
                            }
                            if (payment.problemTitle.isNotBlank()) {
                                appendLine("কাজের শিরোনাম: ${payment.problemTitle}")
                            }
                            appendLine("ইউজার: ${user?.name ?: ""} (${payment.userId})")
                            appendLine("তারিখ: ${Formatters.formatDateTimeBengali(payment.timestamp)}")
                        }.trim()
                        clipboardManager.setText(AnnotatedString(textToCopy))
                        Toast.makeText(context, "রসিদ কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("রসিদ কপি করুন", fontSize = 12.sp, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDetailPayment = null }) {
                    Text("বন্ধ করুন", color = SomadhanTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun GatewayStatCard(
    title: String,
    count: Int,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isManualRefreshing: Boolean = false
) {
    // Ground Rule ১৮/১৯ — পুরো সামারি কার্ড না, শুধু ভ্যালু (টাকার অংক) pulse করবে; টাইটেল/আইকন/
    // কাউন্ট টেক্সট structural, অপরিবর্তিত থাকবে (ব্যবহারকারীর কনফার্মড সিদ্ধান্ত)।
    val amountPulse = rememberFieldChangePulse(
        value = amount,
        isManualRefreshing = isManualRefreshing
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.width(150.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextSecondary, maxLines = 1)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            PulsingValue(isUpdating = amountPulse) {
                Text(text = Formatters.formatTaka(amount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
            }
            Text(text = "${DistanceUtil.toBengaliDigits(count.toString())}টি লেনদেন", fontSize = 10.sp, color = SomadhanTextHint)
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.5.sp, color = SomadhanTextSecondary, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary, modifier = Modifier.weight(1.3f))
    }
}
