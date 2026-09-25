package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.WithdrawalEntity
import com.example.ui.navigation.Screen
import com.example.ui.components.StatusBadge
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.PulsingValue
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.SyncAwareContent
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverBalanceWithdrawScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onNavigate: ((String) -> Unit)? = null,
    onNavigateToSupport: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val withdrawals by viewModel.solverWithdrawals.collectAsStateWithLifecycle()
    val minWithdrawalAmount by viewModel.minWithdrawalAmount.collectAsStateWithLifecycle()

    var selectedWithdrawalDetail by remember { mutableStateOf<WithdrawalEntity?>(null) }

    val minWithdrawBengali = DistanceUtil.toBengaliDigits(DistanceUtil.roundTaka(minWithdrawalAmount))

    val paymentMethods = listOf("বিকাশ (bKash)", "নগদ (Nagad)", "রকেট (Rocket)", "ব্যাংক একাউন্ট")

    var selectedMethod by remember { mutableStateOf(paymentMethods.first()) }
    var amountStr by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var branchName by remember { mutableStateOf("") }
    var accountHolderName by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // [ব্যালেন্স ফিক্স — ধাপ ১] role-scoped balanceSolver পড়া হচ্ছে, শেয়ার্ড `balance` না
    // (দেখুন MONEY_FLOW_AND_ADMIN_BUGS_MASTER_PROMPT.md, বাগ A)।
    val currentBalance = currentUser?.balanceSolver ?: 0.0
    // Loading/Sync Fix Roadmap v2, ধাপ ৪ (Group B) — currentUser/withdrawals দুটোই bulk-pull-এর
    // অংশ, তাই initialSyncPhase প্রাসঙ্গিক।
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    // সেশন ২.৫ (batch31) — এই স্ক্রিনে আগে pull-to-refresh ছিল না, যোগ করা হলো। শুধু নিচের
    // "উইথড্রয়াল ইতিহাস" সেকশন (টাইটেল-কাউন্ট + লিস্ট, একসাথে — Wallet-এর escrow-সেকশনের মতোই
    // সিদ্ধান্ত, কারণ টাইটেলের কাউন্টও ডেটার সাথেই বদলায়) re-entry/pull-to-refresh-এ pulse করবে,
    // বাকি ফর্ম কাঠামো (balance card, withdraw form, submit বাটন) অপরিবর্তিত থাকবে।
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val withdrawalHistoryPulse = rememberFieldChangePulse(
        value = withdrawals,
        isManualRefreshing = isRefreshing,
        sessionKey = "solver_balance_withdraw_sync",
        viewModel = viewModel
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "ব্যালেন্স ও উইথড্র",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = SomadhanTextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = SomadhanTextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SomadhanBg),
                    modifier = Modifier.border(1.dp, SomadhanDivider)
                )
                RealtimeLocationBar(
                    locationAddress = liveLocation.address,
                    isSolver = true,
                    onRefresh = { viewModel.refreshLiveLocation() }
                )
            }
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        SomadhanPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        SyncAwareContent(
            sessionKey = "solver_balance_withdraw_sync",
            viewModel = viewModel,
            syncPhase = initialSyncPhase,
            onRetry = { viewModel.retryInitialSync() }
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SomadhanBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Balance Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanOrange),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "উত্তোলনযোগ্য ওয়ালেট ব্যালেন্স",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = Formatters.formatTaka(currentBalance),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "সর্বনিম্ন উত্তোলনের পরিমাণ ${minWithdrawBengali} টাকা",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Withdraw Form Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SomadhanDivider, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "অর্থ উত্তোলনের অনুরোধ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("উত্তোলনের মাধ্যম *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paymentMethods.forEach { method ->
                            val isSelected = selectedMethod == method
                            val shortName = when {
                                method.contains("bKash") -> "বিকাশ"
                                method.contains("Nagad") -> "নগদ"
                                method.contains("Rocket") -> "রকেট"
                                else -> "ব্যাংক"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SomadhanOrange else SomadhanCardBg)
                                    .border(1.dp, if (isSelected) SomadhanOrange else SomadhanDivider, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedMethod = method
                                        errorMessage = null
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = shortName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else SomadhanTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("উত্তোলনের পরিমাণ (টাকা) *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it; errorMessage = null },
                        placeholder = { Text("যেমন: 500", color = SomadhanTextHint, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("withdraw_amount_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mobile Banking Account Number
                    if (!selectedMethod.contains("ব্যাংক")) {
                        Text("মোবাইল ব্যাংকিং অ্যাকাউন্ট নম্বর *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it; errorMessage = null },
                            placeholder = { Text("01XXXXXXXXX", color = SomadhanTextHint, fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = SomadhanOrange) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SomadhanOrange,
                                unfocusedBorderColor = SomadhanBorder,
                                focusedContainerColor = SomadhanBg,
                                unfocusedContainerColor = SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("withdraw_acc_input")
                        )
                    } else {
                        // Bank Account Fields
                        Text("ব্যাংকের নাম *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it; errorMessage = null },
                            placeholder = { Text("যেমন: ডাচ বাংলা ব্যাংক / ইসলামী ব্যাংক", color = SomadhanTextHint, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = SomadhanOrange) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SomadhanOrange,
                                unfocusedBorderColor = SomadhanBorder,
                                focusedContainerColor = SomadhanBg,
                                unfocusedContainerColor = SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("শাখার নাম *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = branchName,
                            onValueChange = { branchName = it; errorMessage = null },
                            placeholder = { Text("যেমন: মিরপুর শাখা", color = SomadhanTextHint, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SomadhanOrange,
                                unfocusedBorderColor = SomadhanBorder,
                                focusedContainerColor = SomadhanBg,
                                unfocusedContainerColor = SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("অ্যাকাউন্টধারীর নাম *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = accountHolderName,
                            onValueChange = { accountHolderName = it; errorMessage = null },
                            placeholder = { Text("ব্যাংক একাউন্টের নাম লিখুন", color = SomadhanTextHint, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SomadhanOrange,
                                unfocusedBorderColor = SomadhanBorder,
                                focusedContainerColor = SomadhanBg,
                                unfocusedContainerColor = SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("ব্যাংক অ্যাকাউন্ট নম্বর *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it; errorMessage = null },
                            placeholder = { Text("অ্যাকাউন্ট নম্বর লিখুন", color = SomadhanTextHint, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = SomadhanOrange) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SomadhanOrange,
                                unfocusedBorderColor = SomadhanBorder,
                                focusedContainerColor = SomadhanBg,
                                unfocusedContainerColor = SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = errorMessage ?: "", fontSize = 12.sp, color = SomadhanError)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (amount < minWithdrawalAmount) {
                                errorMessage = "সর্বনিম্ন উত্তোলনের পরিমাণ ${minWithdrawBengali} টাকা।"
                                return@Button
                            }
                            if (amount > currentBalance) {
                                errorMessage = "আপনার ব্যালেন্সে পর্যাপ্ত টাকা নেই।"
                                return@Button
                            }
                            if (accountNumber.isBlank()) {
                                errorMessage = "অ্যাকাউন্ট নম্বর প্রদান করুন।"
                                return@Button
                            }
                            // [Step 12.10e] SOLVER-role withdrawal-এ server-এ KYC বাধ্যতামূলক (KYC_REQUIRED)
                            // — এখানে client-side প্রি-চেক দিয়ে আগেভাগেই জানিয়ে দেওয়া হচ্ছে, যাতে
                            // ব্যবহারকারীকে বৃথা সার্ভার-রাউন্ডট্রিপ/এরর-কোড দেখতে না হয়।
                            if (currentUser?.isKycVerified != true) {
                                errorMessage = "উইথড্র করতে হলে আগে KYC ভেরিফিকেশন সম্পন্ন করুন।"
                                return@Button
                            }

                            isSubmitting = true
                            viewModel.requestWithdrawal(
                                amount = amount,
                                method = selectedMethod,
                                accountNumber = accountNumber,
                                bankName = if (selectedMethod.contains("ব্যাংক")) bankName else null,
                                branchName = if (selectedMethod.contains("ব্যাংক")) branchName else null,
                                accountHolderName = if (selectedMethod.contains("ব্যাংক")) accountHolderName else null,
                                onSuccess = {
                                    isSubmitting = false
                                    amountStr = ""
                                    accountNumber = ""
                                    bankName = ""
                                    branchName = ""
                                    accountHolderName = ""
                                },
                                onError = {
                                    isSubmitting = false
                                    errorMessage = it
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_withdraw_btn")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("জমা হচ্ছে...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Text("উইথড্র রিকোয়েস্ট জমা দিন", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Withdrawal History List
            PulsingValue(isUpdating = withdrawalHistoryPulse) {
            Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "উইথড্রয়াল ইতিহাস (${DistanceUtil.toBengaliDigits(withdrawals.size.toString())})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                if (withdrawals.size > 2) {
                    TextButton(
                        onClick = { onNavigate?.invoke(Screen.WithdrawalHistory.route) },
                        modifier = Modifier.testTag("see_more_solver_withdrawals_btn")
                    ) {
                        Text(
                            text = "আরও দেখুন (See More)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (withdrawals.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("এখনও কোনো উইথড্রয়াল রিকোয়েস্ট নেই।", fontSize = 13.sp, color = SomadhanTextHint)
                    }
                }
            } else {
                withdrawals.take(2).forEach { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedWithdrawalDetail = item
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Formatters.formatTaka(item.amount),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )
                                StatusBadge(status = item.status)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "উইথড্র আইডি: ${item.id}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanOrange
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Withdraw ID", item.id))
                                        Toast.makeText(context, "উইথড্র আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Withdraw ID",
                                        modifier = Modifier.size(12.dp),
                                        tint = SomadhanOrange
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "মাধ্যম: ${item.method} (${item.accountNumber})",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                            if (item.bankName != null) {
                                Text(
                                    text = "ব্যাংক: ${item.bankName}, শাখা: ${item.branchName ?: ""}",
                                    fontSize = 11.sp,
                                    color = SomadhanTextHint
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Formatters.formatDateTimeBengali(item.createdAt),
                                    fontSize = 10.sp,
                                    color = SomadhanTextHint
                                )
                                Text(
                                    text = "বিস্তারিত দেখতে ট্যাপ করুন ›",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (item.status == "REJECTED") SomadhanError else SomadhanOrange
                                )
                            }
                        }
                    }
                }
            }
            }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
        }
        }
    }

    // Withdrawal Detail Bottom Sheet (Same as User Withdraw)
    if (selectedWithdrawalDetail != null) {
        com.example.ui.components.WithdrawalDetailBottomSheet(
            withdrawal = selectedWithdrawalDetail!!,
            onDismiss = { selectedWithdrawalDetail = null },
            onNavigateToSupport = onNavigateToSupport,
            accentColor = SomadhanOrange
        )
    }
}
