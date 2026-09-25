package com.example.ui.screens

import androidx.activity.compose.BackHandler
import com.example.ui.components.SummaryTransitionContainer
import com.example.ui.components.SyncBlockedRetryState
import com.example.ui.components.UserAvatar
import android.widget.Toast
import com.example.data.remote.SupabaseRealtimeManager
import com.example.data.repository.SomadhanRepository
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.EscrowEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.UserEntity
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanOrangePressed
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.InstantJobReceiptUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DisputeResultScreen(
    problemId: String,
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit
) {
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val problem = allProblems.find { it.id == problemId }
    // Loading/Sync Fix Roadmap v2, ধাপ ৪ (Group B) — problem null থাকা অবস্থায় bulk-pull
    // (initialSyncPhase) ERROR-এ চলে গেলে চিরকাল স্পিনারে আটকে থাকার বদলে এরর+রিট্রাই UI।
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    if (problem == null) {
        if (initialSyncPhase == SupabaseRealtimeManager.SyncPhase.ERROR) {
            Box(
                modifier = Modifier.fillMaxSize().background(SomadhanBg),
                contentAlignment = Alignment.Center
            ) {
                SyncBlockedRetryState(onRetry = { viewModel.retryInitialSync() })
            }
            return
        }
        Box(
            modifier = Modifier.fillMaxSize().background(SomadhanBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SomadhanOrange)
        }
        return
    }

    val currentUserId = currentUser?.id ?: ""
    val isOwner = currentUserId.isNotBlank() && currentUserId == problem.userId
    val isAcceptedSolver = currentUserId.isNotBlank() && currentUserId == problem.acceptedSolverId
    val isAdmin = currentUser?.role?.equals("ADMIN", ignoreCase = true) == true

    if (!isOwner && !isAcceptedSolver && !isAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SomadhanBg),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .border(1.dp, SomadhanBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SomadhanTextSecondary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "এই তথ্যের গোপনীয়তা সংরক্ষিত",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "বিরোধ নিষ্পত্তির ফলাফল কেবল সংশ্লিষ্ট গ্রাহক এবং সমাধানকারীর জন্য সীমাবদ্ধ।",
                        fontSize = 12.5.sp,
                        color = SomadhanTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ফিরে যান", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
        return
    }

    val isUserRole = isOwner ||
            (currentUser?.role?.equals("USER", ignoreCase = true) == true && currentUserId != problem.acceptedSolverId)
    val otherUserId = if (isUserRole) problem.acceptedSolverId else problem.userId

    var otherUser by remember { mutableStateOf<UserEntity?>(null) }
    var escrow by remember { mutableStateOf<EscrowEntity?>(null) }

    LaunchedEffect(otherUserId) {
        if (!otherUserId.isNullOrBlank()) {
            otherUser = viewModel.getPublicUserById(otherUserId)
        }
    }

    val escrowFlow = remember(problemId) { viewModel.getEscrowForProblem(problemId) }
    val escrowState by escrowFlow.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(escrowState) {
        if (escrowState != null) {
            escrow = escrowState
        }
    }

    SummaryTransitionContainer {
        DisputeResultContent(
            problem = problem,
            otherUser = otherUser,
            escrow = escrow,
            isUserRole = isUserRole,
            viewModel = viewModel,
            onDismiss = {
                viewModel.markDisputeResultSeen(problem.id, isUserRole)
                onNavigateBack()
            }
        )
    }
}

@Composable
fun DisputeResultContent(
    problem: ProblemEntity,
    otherUser: UserEntity?,
    escrow: EscrowEntity?,
    isUserRole: Boolean,
    viewModel: SomadhanViewModel? = null,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val (baseAmount, extraAmount) = SomadhanRepository.resolveSettlementAmounts(problem, escrow)
    val totalEscrow = baseAmount + extraAmount

    val decision = problem.disputeResolutionType ?: problem.disputeResolutionDecision ?: "REFUND_TO_USER"
    val isSplit = decision == "SPLIT_SETTLEMENT" || decision == "SPLIT_50_50" || decision == "CUSTOM_SPLIT" || decision == "SETTLE"
    val solverPct = problem.disputeSplitSolverPercent ?: 50.0
    val clientPct = (100.0 - solverPct).coerceAtLeast(0.0)
    val solverGrossBase = Math.round(baseAmount * (solverPct / 100.0)).toDouble()
    val solverGrossExtra = Math.round(extraAmount * (solverPct / 100.0)).toDouble()
    val solverGrossShare = solverGrossBase + solverGrossExtra
    val clientRefund = Math.round(totalEscrow - solverGrossShare).toDouble()

    val reachedStep = problem.disputeProgressAtSettlement ?: problem.disputeProgressAtRaise ?: problem.calculateProgressStep()

    var commissionBreakdown by remember(problem.id, decision, baseAmount, extraAmount) {
        mutableStateOf<SomadhanRepository.CommissionBreakdown?>(null)
    }

    LaunchedEffect(problem.id, decision, baseAmount, extraAmount, isUserRole) {
        if (viewModel != null) {
            val sId = problem.acceptedSolverId ?: ""
            if (isSplit) {
                commissionBreakdown = viewModel.previewCommissionBreakdown(problem, sId, solverGrossBase, solverGrossExtra)
            } else if (decision == "RELEASE_TO_SOLVER") {
                commissionBreakdown = viewModel.previewCommissionBreakdown(problem, sId, baseAmount, extraAmount)
            }
        }
    }

    // Role-adaptive verdict titles and styling
    val (decisionHeader, decisionSub, decisionColor, decisionIcon) = when (decision) {
        "RELEASE_TO_SOLVER" -> {
            if (!isUserRole) {
                Quadruple(
                    "আপনি বিজয়ী! অর্থ রিলিজ করা হয়েছে 🎉",
                    "অ্যাডমিন আপনার পক্ষে সিদ্ধান্ত দিয়েছেন। সম্পূর্ণ অর্থ আপনার ব্যালেন্সে জমা হয়েছে।",
                    SomadhanSuccess,
                    Icons.Default.CheckCircle
                )
            } else {
                Quadruple(
                    "বিরোধ নিষ্পত্তি: অর্থ রিলিজ ⚖️",
                    "কাজের অগ্রগতি ও পর্যালোচনার ভিত্তিতে অ্যাডমিন সমাধানকারীকে অর্থ রিলিজের রায় দিয়েছেন।",
                    Color(0xFFE65100),
                    Icons.Default.Gavel
                )
            }
        }
        "REFUND_TO_USER" -> {
            if (isUserRole) {
                Quadruple(
                    "রিফান্ড সফল! আপনি বিজয়ী 💰",
                    "অ্যাডমিন আপনার পক্ষে সিদ্ধান্ত দিয়েছেন। সম্পূর্ণ অর্থ আপনার ওয়ালেটে ফেরত দেওয়া হয়েছে।",
                    SomadhanSuccess,
                    Icons.Default.CheckCircle
                )
            } else {
                Quadruple(
                    "বিরোধ নিষ্পত্তি: অর্থ রিফান্ড ⚖️",
                    "অ্যাডমিন পর্যালোচনার ভিত্তিতে ক্লায়েন্টকে সম্পূর্ণ অর্থ রিফান্ডের রায় দিয়েছেন।",
                    SomadhanError,
                    Icons.AutoMirrored.Filled.Undo
                )
            }
        }
        "SPLIT_SETTLEMENT", "CUSTOM_SPLIT" -> {
            val myAmt = if (isUserRole) clientRefund.toInt() else solverGrossShare.toInt()
            val myPct = if (isUserRole) clientPct.toInt() else solverPct.toInt()

            Quadruple(
                "উভয় পক্ষের সমঝোতা সম্পন্ন ⚖️",
                "অ্যাডমিনের মধ্যস্থতায় কাজের অগ্রগতি অনুযায়ী অর্থ ভাগাভাগি করে বিরোধ নিষ্পত্তি করা হয়েছে। আপনার প্রাপ্ত অংশ: ৳ ${DistanceUtil.toBengaliDigits(myAmt.toString())} (${DistanceUtil.toBengaliDigits(myPct.toString())}%)।",
                SomadhanInfo,
                Icons.Default.Balance
            )
        }
        else -> {
            Quadruple(
                "বিরোধ নিষ্পত্তি সম্পন্ন ⚖️",
                "অ্যাডমিন প্যানেল থেকে এই সমস্যার বিরোধ নিষ্পত্তি করা হয়েছে।",
                SomadhanTextPrimary,
                Icons.Default.Info
            )
        }
    }

    val resolutionLabel = when (decision) {
        "RELEASE_TO_SOLVER" -> "৭. অ্যাডমিন কর্তৃক অর্থ রিলিজ করা হয়েছে"
        "REFUND_TO_USER" -> "৭. অ্যাডমিন কর্তৃক অর্থ রিফান্ড করা হয়েছে"
        "SPLIT_SETTLEMENT", "CUSTOM_SPLIT" -> "৭. অ্যাডমিন কর্তৃক অর্থ ভাগাভাগি (স্প্লিট) করা হয়েছে"
        else -> "৭. বিরোধ নিষ্পত্তি সম্পন্ন"
    }

    val disputeProgressSteps = remember(problem, reachedStep, decision, decisionColor, decisionIcon) {
        fun formatStepTime(ts: Long?): String {
            if (ts == null || ts <= 0L) return "সম্পন্ন হয়নি"
            val sdf = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault())
            return DistanceUtil.toBengaliDigits(sdf.format(Date(ts)))
        }

        val baseSteps = listOf(
            Triple("১. জরুরি ব্রডকাস্ট ও সংযোগ", 1, problem.createdAt),
            Triple("২. সমাধানকারী রওয়ানা হয়েছেন", 2, problem.onWayAt ?: problem.acceptedAt2),
            Triple("৩. লোকেশনে পৌঁছেছেন", 3, problem.arrivedAt),
            Triple("৪. কাজ শুরু হয়েছে", 4, problem.jobStartedAt),
            Triple("৫. কাজ সম্পন্ন", 5, problem.completedAt)
        ).map { (label, stepNum, ts) ->
            val done = stepNum <= reachedStep
            DisputeProgressStepItem(
                label = label,
                timeText = if (done && ts != null && ts > 0L) formatStepTime(ts) else if (done) "সম্পন্ন" else "সম্পন্ন হয়নি",
                isDone = done,
                icon = if (done) Icons.Default.CheckCircle else Icons.Default.Cancel,
                iconTint = if (done) SomadhanSuccess else SomadhanError.copy(alpha = 0.5f)
            )
        }

        val disputeTime = problem.disputedAt ?: problem.lastActivityAt ?: problem.createdAt
        val disputeStep = DisputeProgressStepItem(
            label = "৬. বিরোধ উত্থাপন (Dispute Raised)",
            timeText = formatStepTime(disputeTime),
            isDone = true,
            icon = Icons.Default.Warning,
            iconTint = Color(0xFFF59E0B)
        )

        val verdictTime = problem.disputeResolvedAt ?: problem.disputeSettledAt
        val verdictStep = DisputeProgressStepItem(
            label = resolutionLabel,
            timeText = if (verdictTime != null && verdictTime > 0L) formatStepTime(verdictTime) else "মীমাংসিত",
            isDone = true,
            icon = decisionIcon,
            iconTint = decisionColor,
            isVerdict = true
        )

        baseSteps + disputeStep + verdictStep
    }

    val resolvedTimeFormatted = remember(problem.disputeResolvedAt) {
        val ts = problem.disputeResolvedAt ?: System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault())
        DistanceUtil.toBengaliDigits(sdf.format(Date(ts)))
    }

    Scaffold(
        containerColor = SomadhanBg,
        topBar = {
            Surface(
                color = SomadhanCardBg,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusBarPadding + 6.dp, bottom = 12.dp, start = 12.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dispute_result_top_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = SomadhanTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Image(
                        painter = painterResource(id = R.drawable.somadhan_app_icon_1786820904533),
                        contentDescription = "সমাধান লোগো",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "বিরোধ নিষ্পত্তির রেজাল্ট",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "সমাধান (Somadhan) অফিসিয়াল রিসিট",
                            fontSize = 11.5.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = SomadhanCardBg,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Phase U: Receipt Download Button (Above 'বুঝেছি ও ফিরে যান' button)
                    Button(
                        onClick = {
                            val receiptFile = InstantJobReceiptUtil.generateInstantJobReceipt(
                                context = context,
                                problem = problem,
                                otherUser = otherUser,
                                selfUser = viewModel?.currentUser?.value,
                                isUserRole = isUserRole,
                                commissionBreakdown = commissionBreakdown,
                                escrow = escrow
                            )
                            if (receiptFile != null) {
                                Toast.makeText(context, "রিসিট ডাউনলোড হয়েছে (${receiptFile.name})", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "রিসিট তৈরিতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SomadhanOrangeLight,
                            contentColor = SomadhanOrange
                        ),
                        border = BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dispute_result_receipt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "রিসিট ডাউনলোড করুন (PDF)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanOrange
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dispute_result_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "বুঝেছি ও ফিরে যান",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ১. রেজাল্ট ও রায় ব্যানার
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = decisionColor.copy(alpha = 0.08f)),
                border = BorderStroke(1.5.dp, decisionColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().testTag("dispute_result_decision_banner")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(decisionColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = decisionIcon,
                                contentDescription = null,
                                tint = decisionColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "চূড়ান্ত অ্যাডমিন রায়",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = decisionColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = decisionHeader,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = decisionColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = decisionSub,
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = decisionColor.copy(alpha = 0.2f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = problem.title.ifBlank { "জরুরি সমস্যা সমাধান" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ক্যাটাগরি: ${problem.categoryName.ifBlank { "সাধারণ" }}",
                            fontSize = 11.5.sp,
                            color = SomadhanTextSecondary
                        )

                        // Post ID Chip
                        val cleanPostId = problem.id
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                                .border(0.6.dp, Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(cleanPostId))
                                    Toast.makeText(context, "পোস্ট ID কপি করা হয়েছে: $cleanPostId", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                tint = SomadhanTextHint,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "ID: $cleanPostId",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanTextSecondary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "কপি করুন",
                                tint = SomadhanTextHint,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }

            // ২. কাজের অগ্রগতি ও চেকলিস্ট (Partial Progress)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth().testTag("dispute_result_progress_checklist")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "কাজের অগ্রগতি ও বিরোধকালীন স্টেজ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = SomadhanTextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SomadhanOrangeLight
                        ) {
                            Text(
                                text = "ধাপ ${DistanceUtil.toBengaliDigits(reachedStep.toString())}/৫",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrangePressed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    disputeProgressSteps.forEach { step ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = step.iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.label,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (step.isVerdict) FontWeight.SemiBold else if (step.isDone) FontWeight.Medium else FontWeight.Normal,
                                    color = if (step.isDone) SomadhanTextPrimary else SomadhanTextHint
                                )
                                Text(
                                    text = step.timeText,
                                    fontSize = 11.5.sp,
                                    color = if (step.isDone) SomadhanTextSecondary else SomadhanError.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // ৩. অ্যাডমিন রায় ও বিস্তারিত নোট
            if (!problem.disputeResolutionNote.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    border = BorderStroke(1.dp, SomadhanBorder),
                    modifier = Modifier.fillMaxWidth().testTag("dispute_result_admin_note_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "অ্যাডমিনের পর্যবেক্ষণ ও মীমাংসা নোট",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = SomadhanTextPrimary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = problem.disputeResolutionNote ?: "",
                            fontSize = 13.sp,
                            color = SomadhanTextSecondary,
                            lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "মীমাংসার সময়: $resolvedTimeFormatted",
                            fontSize = 11.5.sp,
                            color = SomadhanTextHint
                        )
                    }
                }
            }

            // ৪. আর্থিক ও এসক্রো সারসংক্ষেপ কার্ড
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth().testTag("dispute_result_escrow_summary")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "আর্থিক লেনদেন ও এসক্রো নিষ্পত্তি",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = SomadhanTextPrimary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "চুক্তিভিত্তিক মূল বাজেট:", fontSize = 13.sp, color = SomadhanTextSecondary)
                        Text(text = "৳ ${DistanceUtil.toBengaliDigits(baseAmount.toInt().toString())}", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                    }
                    if (extraAmount > 0.0) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "অতিরিক্ত চার্জ / পার্টস বিল:", fontSize = 13.sp, color = SomadhanTextSecondary)
                            Text(text = "+ ৳ ${DistanceUtil.toBengaliDigits(extraAmount.toInt().toString())}", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = SomadhanOrangePressed)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = SomadhanBorder, thickness = 0.8.dp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "মোট নিষ্পত্তিকৃত এসক্রো ফান্ড:",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "৳ ${DistanceUtil.toBengaliDigits(totalEscrow.toInt().toString())}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }

                    if (decision == "RELEASE_TO_SOLVER") {
                        if (!isUserRole) {
                            // Solver View: Commission & Net Payout
                            val rate = commissionBreakdown?.rate ?: (problem.appliedCommissionRate ?: 10.0)
                            val wasFree = commissionBreakdown?.wasFreeQuotaJob ?: false
                            val baseComm = commissionBreakdown?.baseCommission ?: if (wasFree) 0.0 else Math.round(baseAmount * (rate / 100.0)).toDouble()
                            val extraComm = commissionBreakdown?.extraCommission ?: 0.0
                            val extraApplied = commissionBreakdown?.extraCommissionApplied ?: false
                            val totalComm = commissionBreakdown?.totalCommission ?: (baseComm + extraComm)
                            val netEarnings = commissionBreakdown?.netAmount ?: (totalEscrow - totalComm).coerceAtLeast(0.0)

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = SomadhanBorder, thickness = 0.8.dp)
                            Spacer(Modifier.height(8.dp))

                            if (wasFree) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "প্ল্যাটফর্ম ফি (ফ্রি কোটা ০%):", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                                    Text(text = "৳ ০ (ফ্রি)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SomadhanSuccess)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "প্ল্যাটফর্ম ফি (${DistanceUtil.toBengaliDigits(rate.toInt().toString())}%):", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                                    Text(text = "- ৳ ${DistanceUtil.toBengaliDigits(baseComm.toInt().toString())}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SomadhanError)
                                }
                                if (extraAmount > 0.0) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = if (extraApplied) "অতিরিক্ত বিলের ফি (${DistanceUtil.toBengaliDigits(rate.toInt().toString())}%):" else "অতিরিক্ত বিলের ফি (কমিশন ছাড় ০%):", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                                        Text(
                                            text = if (extraApplied) "- ৳ ${DistanceUtil.toBengaliDigits(extraComm.toInt().toString())}" else "৳ ০ (ছাড়)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (extraApplied) SomadhanError else SomadhanSuccess
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = SomadhanBorder, thickness = 0.8.dp)
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "ওয়ালেটে মোট জমা (নিট আয়):", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = SomadhanSuccess)
                                Text(text = "৳ ${DistanceUtil.toBengaliDigits(netEarnings.toInt().toString())}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SomadhanSuccess)
                            }

                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SomadhanSuccess.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "আপনার ব্যালেন্সে নিট অর্থ জমা হয়েছে: ৳ ${DistanceUtil.toBengaliDigits(netEarnings.toInt().toString())}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanSuccess,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        } else {
                            // User View for RELEASE_TO_SOLVER
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFF7ED),
                                border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "এসক্রো থেকে সমাধানকারীকে পরিশোধ: ৳ ${DistanceUtil.toBengaliDigits(totalEscrow.toInt().toString())}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC2410C)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "অ্যাডমিন পর্যালোচনার প্রেক্ষিতে সম্পূর্ণ অর্থ সমাধানকারীকে রিলিজ করা হয়েছে। গ্রাহকের কোনো রিফান্ড প্রযোজ্য নয়।",
                                        fontSize = 11.5.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                            }
                        }
                    } else if (decision == "REFUND_TO_USER") {
                        if (isUserRole) {
                            // User View for REFUND_TO_USER
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SomadhanSuccess.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "আপনার ওয়ালেটে রিফান্ড জমা হয়েছে: ৳ ${DistanceUtil.toBengaliDigits(totalEscrow.toInt().toString())}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "১০০% অর্থ কোনো কর্তন ছাড়া সফলভাবে আপনার মূল ব্যালেন্সে ফেরত দেওয়া হয়েছে।",
                                        fontSize = 11.5.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                            }
                        } else {
                            // Solver View for REFUND_TO_USER
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SomadhanError.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "গ্রাহককে সম্পূর্ণ অর্থ রিফান্ড করা হয়েছে",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanError
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "অ্যাডমিন সিদ্ধান্তের প্রেক্ষিতে কাজটি বাতিল ও গ্রাহককে সম্পূর্ণ অর্থ ফেরত দেওয়া হয়েছে।",
                                        fontSize = 11.5.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                            }
                        }
                    } else if (isSplit) {
                        // Split Settlement
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = SomadhanBorder, thickness = 0.8.dp)
                        Spacer(Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "সমঝোতা বণ্টন হার:", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                            Text(
                                text = "${DistanceUtil.toBengaliDigits(solverPct.toInt().toString())}% সলভার / ${DistanceUtil.toBengaliDigits(clientPct.toInt().toString())}% ক্লায়েন্ট",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanTextPrimary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "সমাধানকারীর মোট অংশ:", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(solverGrossShare.toInt().toString())}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrange
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "ক্লায়েন্ট ফেরত / রিফান্ড:", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(clientRefund.toInt().toString())}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanSuccess
                            )
                        }

                        if (!isUserRole) {
                            // Solver View: Fee breakdown on solver's split portion
                            val rate = commissionBreakdown?.rate ?: (problem.appliedCommissionRate ?: 10.0)
                            val wasFree = commissionBreakdown?.wasFreeQuotaJob ?: false
                            val baseComm = commissionBreakdown?.baseCommission ?: if (wasFree) 0.0 else Math.round(solverGrossBase * (rate / 100.0)).toDouble()
                            val extraComm = commissionBreakdown?.extraCommission ?: 0.0
                            val extraApplied = commissionBreakdown?.extraCommissionApplied ?: false
                            val totalComm = commissionBreakdown?.totalCommission ?: (baseComm + extraComm)
                            val netEarnings = commissionBreakdown?.netAmount ?: (solverGrossShare - totalComm).coerceAtLeast(0.0)

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = SomadhanBorder, thickness = 0.8.dp)
                            Spacer(Modifier.height(8.dp))

                            if (wasFree) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "প্ল্যাটফর্ম ফি (ফ্রি কোটা ০%):", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                                    Text(text = "৳ ০ (ফ্রি)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SomadhanSuccess)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "প্ল্যাটফর্ম ফি (${DistanceUtil.toBengaliDigits(rate.toInt().toString())}%):", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                                    Text(text = "- ৳ ${DistanceUtil.toBengaliDigits(baseComm.toInt().toString())}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SomadhanError)
                                }
                                if (solverGrossExtra > 0.0) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = if (extraApplied) "অতিরিক্ত বিলের ফি (${DistanceUtil.toBengaliDigits(rate.toInt().toString())}%):" else "অতিরিক্ত বিলের ফি (কমিশন ছাড় ০%):", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                                        Text(
                                            text = if (extraApplied) "- ৳ ${DistanceUtil.toBengaliDigits(extraComm.toInt().toString())}" else "৳ ০ (ছাড়)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (extraApplied) SomadhanError else SomadhanSuccess
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = SomadhanBorder, thickness = 0.8.dp)
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "ওয়ালেটে নিট জমা:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = SomadhanSuccess)
                                Text(text = "৳ ${DistanceUtil.toBengaliDigits(netEarnings.toInt().toString())}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SomadhanSuccess)
                            }

                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SomadhanSuccess.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "আপনার ব্যালেন্সে নিট অর্থ জমা হয়েছে: ৳ ${DistanceUtil.toBengaliDigits(netEarnings.toInt().toString())}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanSuccess,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        } else {
                            // User View for Split Settlement
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SomadhanSuccess.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "আপনার ওয়ালেটে রিফান্ড জমা হয়েছে: ৳ ${DistanceUtil.toBengaliDigits(clientRefund.toInt().toString())}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanSuccess,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ৫. অপর পক্ষের প্রোফাইল কার্ড
            if (otherUser != null) {
                val otherRoleBadge = if (isUserRole) "সমাধানকারী" else "গ্রাহক"
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    border = BorderStroke(1.dp, SomadhanBorder),
                    modifier = Modifier.fillMaxWidth().testTag("dispute_result_other_user_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            photoUri = otherUser.profileImageUri,
                            name = otherUser.name.ifBlank { "ব্যবহারকারী" },
                            size = 44.dp,
                            backgroundColor = SomadhanOrangeLight,
                            textColor = SomadhanOrange,
                            fontSize = 16.sp,
                            borderColor = SomadhanOrange.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = otherUser.name.ifBlank { "ব্যবহারকারী" },
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SomadhanOrangeLight
                                ) {
                                    Text(
                                        text = otherRoleBadge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanOrangePressed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "আইডি: ${otherUser.id}",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private data class DisputeProgressStepItem(
    val label: String,
    val timeText: String = "",
    val isDone: Boolean,
    val icon: ImageVector,
    val iconTint: Color,
    val isVerdict: Boolean = false
)
