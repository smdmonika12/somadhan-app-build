package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.util.ImageStorageUtil
import com.example.data.entity.RatingEntity
import com.example.ui.components.DetailScreenSkeleton
import com.example.ui.components.PulsingValue
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.SyncAwareContent
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReputationDetailScreen(
    viewModel: SomadhanViewModel,
    userId: String,
    isOwnProfile: Boolean,
    targetRole: String? = null,
    onNavigateBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val targetUser by viewModel.getUserByIdFlow(userId).collectAsStateWithLifecycle(initialValue = null)
    val effectiveUser = targetUser ?: (if (isOwnProfile || userId == currentUser?.id) currentUser else null)
    // [MONEY_FLOW_AND_ADMIN_BUGS ধাপ ৮] dual-role ইউজারের ক্ষেত্রে ban/restrict status
    // role-scoped কলামে থাকে। `targetRole` (nav argument, AdminUsersView-এর কার্ড থেকে এলে
    // সেই কার্ডের role) না থাকলে effectiveUser-এর নিজের বর্তমান active role-এ fallback করে —
    // পুরনো caller-দের (self-view ইত্যাদি) আচরণ অপরিবর্তিত থাকে।
    val effectiveRole = targetRole ?: effectiveUser?.role
    val recentEvents by viewModel.getReputationHistory(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val platformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
    val extraBillRepCap = platformSettings.find { it.key == "extra_bill_reputation_cap_per_problem" }?.value?.toDoubleOrNull() ?: 10.0

    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()
    // Loading Pattern Master Prompt, ব্যাচ ২০ (ক্যাটেগরি C full migrate) — আগে এখানে একটা
    // ম্যানুয়াল rememberSessionAwareSkeletonGate + early-return + custom SyncBlockedRetryState
    // ছিল। এরপর ADMIN_PANEL_LOADING_MASTER_PROMPT.md-এর Ground Rule ১৮ অনুযায়ী (সেশন ২.২৭)
    // whole-content diff-শিমার (SyncAwareRefreshableContent, data=[...]) সরিয়ে প্লেইন
    // SyncAwareContent (rule ১, cold-load) করা হয়েছে — pull-to-refresh (rule ৩, নিচের
    // SomadhanPullToRefresh) অপরিবর্তিত আছে, শুধু "পুরো কনটেন্ট flash" আচরণটাই সরানো হয়েছে।
    // rule ২-এর re-entry/data-change pulse এখন GR19 স্কোপ অনুযায়ী শুধু হিস্ট্রি ইভেন্ট
    // কার্ডে (per-item) প্রয়োগ করা হয়েছে, নিচে দেখো।
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isSolverViewer = currentUser?.role.equals("SOLVER", ignoreCase = true)

    var ratings by remember { mutableStateOf<List<RatingEntity>>(emptyList()) }
    LaunchedEffect(userId) {
        viewModel.getRatingsForUserFlow(userId).collect { list ->
            ratings = list
        }
    }

    var problemGainedMap by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    LaunchedEffect(recentEvents, userId) {
        val extraBillProblemIds = recentEvents.filter {
            it.eventType.contains("EXTRA_CHARGE", ignoreCase = true) || it.eventType.contains("EXTRA_BILL", ignoreCase = true)
        }.mapNotNull { it.problemId }.distinct()

        val map = mutableMapOf<String, Double>()
        for (probId in extraBillProblemIds) {
            val sum = viewModel.getSumScoreChangeForProblem(
                userId,
                listOf("EXTRA_CHARGE_VIA_APP", "EXTRA_CHARGE_ACCEPTED"),
                probId
            )
            map[probId] = sum
        }
        problemGainedMap = map
    }

    val isSolver = effectiveUser?.role == "SOLVER"

    val userBlue = Color(0xFF1D4ED8)
    val userBlueLight = Color(0xFFEFF6FF)
    val userBlueBorder = Color(0xFFBFDBFE)

    val accentColor = if (isSolver) SomadhanOrange else userBlue
    val topGradientColor = if (isSolver) SomadhanOrangeLight.copy(alpha = 0.5f) else userBlueLight.copy(alpha = 0.6f)
    val avatarBorderColor = if (isSolver) SomadhanOrange.copy(alpha = 0.4f) else userBlue.copy(alpha = 0.35f)

    // Stats calculations
    val completedJobsCount = if (isSolver) {
        allProblems.count { it.acceptedSolverId == userId && it.status == "COMPLETED" }
    } else {
        allProblems.count { it.userId == userId && it.status == "COMPLETED" }
    }
    val totalPostedProblems = allProblems.count { it.userId == userId }

    val averageRating = if (ratings.isNotEmpty()) {
        ratings.map { it.stars }.average()
    } else {
        5.0
    }

    var animatedScore by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(effectiveUser?.reputationScore) {
        val target = (effectiveUser?.reputationScore ?: 0.0).toFloat()
        animate(
            initialValue = 0f,
            targetValue = target,
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        ) { value, _ -> animatedScore = value }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isOwnProfile) "আপনার রেপুটেশন" else "প্রোফাইল রেপুটেশন",
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
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
                    isSolver = isSolver,
                    onRefresh = { viewModel.refreshLiveLocation() }
                )
            }
        },
        containerColor = SomadhanBg
    ) { padding ->
        // rule ৩ — pull-to-refresh এই স্ক্রিনে আগে ছিলই না, এখন নতুন যোগ হলো
        // (ব্যবহারকারীর সিদ্ধান্ত)। onRefresh-এ বাকি সব migrate-হওয়া স্ক্রিনের মতোই সাধারণ
        // viewModel.refreshData() — bulk-pull ERROR-এ থাকলে এটাই retryInitialSync()-ও কল করে।
        SomadhanPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Ground Rule ১৮, সেশন ২.২৭ — পুরো কনটেন্ট (স্কোর/স্ট্যাট/হিস্ট্রি একসাথে) আর
            // whole-content diff-শিমার flash করবে না (ব্যবহারকারীর কনফার্মড সিদ্ধান্ত)। rule ১
            // (cold-load) শুধু প্লেইন SyncAwareContent দিয়ে কভার করা হচ্ছে; rule ২-এর
            // re-entry/data-change pulse এখন নিচে শুধু হিস্ট্রি ইভেন্ট কার্ডে (per-item
            // rememberFieldChangePulse + PulsingValue) প্রয়োগ করা হয়েছে (GR19 স্কোপ অনুযায়ী)।
            SyncAwareContent(
                sessionKey = "reputation_detail_$userId",
                viewModel = viewModel,
                syncPhase = initialSyncPhase,
                onRetry = { viewModel.retryInitialSync() },
                skeleton = {
                    DetailScreenSkeleton(
                        modifier = Modifier.fillMaxSize(),
                        tint = if (isSolverViewer) SomadhanOrange else SomadhanInfo
                    )
                }
            ) {
        if (effectiveUser == null) {
            // Loading/Sync Fix Roadmap v2, ধাপ ৪-এর সমতুল্য — বাল্ক-পুল LOADED হয়ে গেছে কিন্তু
            // এই userId-র জন্য কোনো ইউজার পাওয়া যায়নি (মুছে ফেলা অ্যাকাউন্ট ইত্যাদি)। bulk-pull
            // নিজেই ব্যর্থ (ERROR) হলে উপরের SyncAwareContent স্বয়ংক্রিয়ভাবে
            // SyncErrorState(retry) দেখাবে — এটা শুধু "সত্যিই খুঁজে পাওয়া যায়নি" কেসের জন্য।
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ব্যবহারকারী পাওয়া যায়নি",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                }
            }
        } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            topGradientColor,
                            SomadhanBg,
                            SomadhanBg
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Score Gauge Arc with Full Profile Photo inside + Attached Score Badge on Top-Right
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(190.dp)
                ) {
                    // Gauge Track & Progress Arc around the circle
                    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        // Background track
                        drawArc(
                            color = SomadhanDivider,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        // Animated progress arc
                        val sweep = (animatedScore / 100f) * 360f
                        val arcColor = if (isSolver) {
                            when {
                                animatedScore >= 80f -> SomadhanSuccess
                                animatedScore >= 50f -> SomadhanInfo
                                else -> SomadhanOrange
                            }
                        } else {
                            userBlue
                        }
                        drawArc(
                            color = arcColor,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Centered Profile Image filling the circle
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(SomadhanCardBg)
                            .border(2.5.dp, avatarBorderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageUri = effectiveUser?.profileImageUri
                        val isPhotoValid = ImageStorageUtil.isValidDisplayUri(imageUri)
                        if (isPhotoValid) {
                            var loadFailed by remember(imageUri) { mutableStateOf(false) }
                            if (!loadFailed) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    onError = { loadFailed = true }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(76.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(76.dp)
                            )
                        }
                    }

                    // Attached Score Badge on Top Right
                    val badgeBg = if (isSolver) {
                        when {
                            animatedScore >= 80f -> SomadhanSuccess
                            animatedScore >= 50f -> SomadhanInfo
                            else -> SomadhanOrange
                        }
                    } else {
                        userBlue
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = badgeBg,
                        shadowElevation = 4.dp,
                        border = BorderStroke(2.dp, Color.White),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🛡️", fontSize = 12.sp)
                            Text(
                                text = "${DistanceUtil.toBengaliDigits(animatedScore.toInt().toString())} / ১০০",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = effectiveUser?.name ?: "",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ---------------- STATS CARDS ----------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isSolver) {
                        // SOLVER CARD 1: সম্পন্ন কাজ
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("সম্পন্ন কাজ", fontSize = 11.sp, color = SomadhanTextSecondary, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(completedJobsCount.toString())} টি",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // SOLVER CARD 2: গড় রেটিং
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("গড় রেটিং", fontSize = 11.sp, color = SomadhanTextSecondary, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⭐", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = DistanceUtil.toBengaliDigits(String.format(Locale.US, "%.1f", averageRating)),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                }
                            }
                        }

                        // SOLVER CARD 3: KYC স্ট্যাটাস
                        val isKycVerified = targetUser?.isKycVerified == true
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isKycVerified) SomadhanSuccessLight else SomadhanOrangeLight
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("KYC স্ট্যাটাস", fontSize = 11.sp, color = SomadhanTextSecondary, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isKycVerified) "✅ ভেরিফায়েড" else "⏳ পেন্ডিং",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isKycVerified) SomadhanSuccess else SomadhanOrange,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // USER CARD 1: সম্পন্ন কাজ
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("সম্পন্ন কাজ", fontSize = 11.sp, color = SomadhanTextSecondary, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(completedJobsCount.toString())} টি",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = userBlue,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // USER CARD 2: গড় রেটিং
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("গড় রেটিং", fontSize = 11.sp, color = SomadhanTextSecondary, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⭐", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = DistanceUtil.toBengaliDigits(String.format(Locale.US, "%.1f", averageRating)),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                }
                            }
                        }

                        // USER CARD 3: অ্যাকাউন্ট স্ট্যাটাস
                        val isBannedForRole = if (effectiveRole == "SOLVER") effectiveUser?.isBannedSolver == true else effectiveUser?.isBannedUser == true
                        val isRestrictedForRole = if (effectiveRole == "SOLVER") effectiveUser?.isRestrictedSolver == true else effectiveUser?.isRestrictedUser == true
                        val isAccountActive = !isBannedForRole && !isRestrictedForRole
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isAccountActive) userBlueLight else SomadhanCardBg),
                            border = if (isAccountActive) BorderStroke(1.dp, userBlueBorder) else CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("অ্যাকাউন্ট", fontSize = 11.sp, color = SomadhanTextSecondary, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isAccountActive) "সক্রিয়" else "স্থগিত",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAccountActive) userBlue else SomadhanError,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---------------- SOLVER PRIVATE FINANCIAL & HEALTH CARDS (STEP 13) ----------------
                if (isSolver && isOwnProfile) {
                    // ৩A: ফ্রি-কমিশন কোটা কার্ড
                    val isFreeQuotaEnabled = platformSettings.find { it.key == "free_quota_enabled" }?.value != "false"
                    val currentMonthKey = remember {
                        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
                    }
                    val quotaThreshold = platformSettings.find { it.key == "free_quota_reputation_threshold" }?.value?.toDoubleOrNull() ?: 80.0
                    val quotaLimit = platformSettings.find { it.key == "free_quota_job_count" }?.value?.toIntOrNull() ?: 10
                    
                    val freeQuotaTransactionsThisMonth = remember(allTransactions, userId, currentMonthKey) {
                        allTransactions.count { trx ->
                            trx.solverId == userId &&
                            (trx.wasFreeQuotaJob || (trx.baseCommissionAmount == 0.0 && trx.commissionPercent == 0.0 && trx.grossAmount > 0.0)) &&
                            SimpleDateFormat("yyyy-MM", Locale.US).format(Date(trx.timestamp)) == currentMonthKey
                        }
                    }
                    val activeFreeQuotaProblemsThisMonth = remember(allProblems, userId, currentMonthKey) {
                        allProblems.count { prob ->
                            prob.acceptedSolverId == userId &&
                            prob.status == "IN_PROGRESS" &&
                            prob.appliedCommissionRate == 0.0 &&
                            SimpleDateFormat("yyyy-MM", Locale.US).format(Date(prob.lastActivityAt ?: prob.createdAt)) == currentMonthKey
                        }
                    }
                    val storedUsed = if (effectiveUser?.freeJobsMonthKey == currentMonthKey) (effectiveUser?.freeJobsUsedThisMonth ?: 0) else 0
                    val used = maxOf(storedUsed, freeQuotaTransactionsThisMonth + activeFreeQuotaProblemsThisMonth)
                    
                    // Sync UserEntity with real used quota if difference detected
                    LaunchedEffect(used, effectiveUser?.id, currentMonthKey) {
                        val u = effectiveUser ?: return@LaunchedEffect
                        val curStored = if (u.freeJobsMonthKey == currentMonthKey) u.freeJobsUsedThisMonth else 0
                        if (used != curStored || u.freeJobsMonthKey != currentMonthKey) {
                            val updated = u.copy(
                                freeJobsUsedThisMonth = used,
                                freeJobsMonthKey = currentMonthKey
                            )
                            viewModel.updateUser(updated)
                        }
                    }

                    val eligible = (effectiveUser?.reputationScore ?: 0.0) >= quotaThreshold
                    val remainingSlots = (quotaLimit - used).coerceAtLeast(0)

                    if (isFreeQuotaEnabled) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            border = BorderStroke(
                                if (eligible) 1.5.dp else 1.dp,
                                if (eligible) SomadhanSuccess.copy(alpha = 0.6f) else SomadhanBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🎁", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ফ্রি কমিশন কোটা",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = SomadhanTextPrimary
                                        )
                                    }
                                    if (eligible) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (remainingSlots > 0) SomadhanSuccessLight else SomadhanErrorLight
                                        ) {
                                            Text(
                                                text = if (remainingSlots > 0) "সক্রিয় কোটা" else "কোটা পূর্ণ",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (remainingSlots > 0) SomadhanSuccess else SomadhanError
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (eligible) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ব্যবহৃত: ${DistanceUtil.toBengaliDigits(used.toString())} / ${DistanceUtil.toBengaliDigits(quotaLimit.toString())} কাজ",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SomadhanTextPrimary
                                        )
                                        Text(
                                            text = "${DistanceUtil.toBengaliDigits(((used.toFloat() / quotaLimit.coerceAtLeast(1)) * 100).toInt().toString())}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanSuccess
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { (used.toFloat() / quotaLimit.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = SomadhanSuccess,
                                        trackColor = SomadhanSuccessLight
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (remainingSlots > 0)
                                            "বাকি স্লট: ${DistanceUtil.toBengaliDigits(remainingSlots.toString())}টা কাজ কমিশন-ফ্রি হবে এই মাসে"
                                        else
                                            "এই মাসের সমস্ত ফ্রি স্লট ব্যবহার করা হয়েছে। পরবর্তী কাজগুলোতে স্বাভাবিক কমিশন প্রযোজ্য হবে।",
                                        fontSize = 12.sp,
                                        color = if (remainingSlots > 0) SomadhanTextSecondary else SomadhanOrange
                                    )
                                } else {
                                    Text(
                                        text = "Reputation ${DistanceUtil.toBengaliDigits(quotaThreshold.toInt().toString())}+ হলে প্রতি মাসে ${DistanceUtil.toBengaliDigits(quotaLimit.toString())}টা কাজ কমিশন-ফ্রি পাবেন। আপনার বর্তমান স্কোর: ${DistanceUtil.toBengaliDigits((effectiveUser?.reputationScore ?: 0.0).toInt().toString())}",
                                        fontSize = 12.sp,
                                        color = SomadhanTextSecondary,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // ৩B: Extra Payment Miss-Cycle হেলথ কার্ড
                    val missRuleEnabled = platformSettings.find { it.key == "extra_payment_miss_rule_enabled" }?.value != "false"
                    if (missRuleEnabled) {
                        val cycleSize = platformSettings.find { it.key == "extra_payment_miss_cycle_size" }?.value?.toIntOrNull() ?: 10
                        val threshold = platformSettings.find { it.key == "extra_payment_miss_threshold" }?.value?.toIntOrNull() ?: 3
                        val cycleJobs = effectiveUser?.cycleJobCount ?: 0
                        val cycleMiss = effectiveUser?.cycleMissCount ?: 0
                        val warningLevel = when {
                            cycleMiss >= threshold -> SomadhanError
                            cycleMiss == threshold - 1 && cycleMiss > 0 -> SomadhanOrange
                            else -> SomadhanSuccess
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            border = BorderStroke(1.2.dp, warningLevel.copy(alpha = 0.7f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🔄", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "এক্সট্রা বিল হেলথ (বর্তমান চক্র)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = SomadhanTextPrimary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when {
                                            cycleMiss >= threshold -> SomadhanErrorLight
                                            cycleMiss == threshold - 1 && cycleMiss > 0 -> SomadhanOrangeLight
                                            else -> SomadhanSuccessLight
                                        }
                                    ) {
                                        Text(
                                            text = when {
                                                cycleMiss >= threshold -> "পেনাল্টি ঝুঁকিতে"
                                                cycleMiss == threshold - 1 && cycleMiss > 0 -> "সতর্কতা"
                                                else -> "সুস্থ চক্র"
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = warningLevel
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "চক্র: ${DistanceUtil.toBengaliDigits(cycleJobs.toString())} / ${DistanceUtil.toBengaliDigits(cycleSize.toString())} কাজ সম্পন্ন",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SomadhanTextPrimary
                                    )
                                    Text(
                                        text = "মিস: ${DistanceUtil.toBengaliDigits(cycleMiss.toString())} / ${DistanceUtil.toBengaliDigits(threshold.toString())}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = warningLevel
                                    )
                                }

                                if (cycleMiss >= threshold - 1 && cycleMiss > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "⚠️ সতর্কতা: এই চক্রে ${DistanceUtil.toBengaliDigits(threshold.toString())} বার অতিরিক্ত বিল মিস হলে রেপুটেশন থেকে পয়েন্ট কাটা হবে।",
                                        fontSize = 12.sp,
                                        color = warningLevel,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // ৩D: মাসের কমিশন সামারি
                    val solverMonthlyTransactions = remember(allTransactions, userId, currentMonthKey) {
                        allTransactions.filter { trx ->
                            trx.solverId == userId &&
                            SimpleDateFormat("yyyy-MM", Locale.US).format(Date(trx.timestamp)) == currentMonthKey
                        }
                    }
                    val monthlyNetSum = solverMonthlyTransactions.sumOf { it.netAmount }
                    val monthlyBaseCommSum = solverMonthlyTransactions.sumOf { it.baseCommissionAmount }
                    val monthlyExtraCommSum = solverMonthlyTransactions.sumOf { it.extraCommissionAmount }
                    val monthlyTotalCommSum = monthlyBaseCommSum + monthlyExtraCommSum

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        border = BorderStroke(1.dp, SomadhanBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📊", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "চলতি মাসের কমিশন ও আয় সামারি",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = SomadhanTextPrimary
                                    )
                                }
                                Text(
                                    text = "মাস: $currentMonthKey",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("মোট আয় (নেট)", fontSize = 11.sp, color = SomadhanTextSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "৳${DistanceUtil.toBengaliDigits(DistanceUtil.roundTaka(monthlyNetSum))}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("মোট কমিশন দেওয়া", fontSize = 11.sp, color = SomadhanTextSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "৳${DistanceUtil.toBengaliDigits(DistanceUtil.roundTaka(monthlyTotalCommSum))}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrange
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = SomadhanDivider, thickness = 0.6.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "বেস কমিশন: ৳${DistanceUtil.toBengaliDigits(DistanceUtil.roundTaka(monthlyBaseCommSum))}",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary
                                )
                                Text(
                                    text = "এক্সট্রা বিল কমিশন: ৳${DistanceUtil.toBengaliDigits(DistanceUtil.roundTaka(monthlyExtraCommSum))}",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // ---------------- RECENT REPUTATION EVENTS ----------------
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "সাম্প্রতিক Reputation ইভেন্ট",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val displayEvents = recentEvents.take(10)
                    if (displayEvents.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "এখনও কোনো রেপুটেশন ইভেন্ট রেকর্ড হয়নি",
                                    fontSize = 13.sp,
                                    color = SomadhanTextHint,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            displayEvents.forEach { event ->
                                val isPositive = event.scoreChange >= 0
                                val (eventIcon, iconTint, iconBg) = when (event.eventType) {
                                    "EXTRA_CHARGE_VIA_APP", "EXTRA_CHARGE_ACCEPTED" -> Triple("💰", SomadhanOrange, SomadhanOrangeLight)
                                    "JOB_COMPLETED" -> Triple("✅", SomadhanSuccess, SomadhanSuccessLight)
                                    "EXTRA_PAYMENT_MISS_CYCLE_PENALTY" -> Triple("⚠️", SomadhanError, SomadhanErrorLight)
                                    "ADMIN_BANNED", "ADMIN_RESTRICTED" -> Triple("🚫", SomadhanError, SomadhanErrorLight)
                                    else -> when {
                                        event.eventType.contains("COMPLETED", ignoreCase = true) || event.eventType.contains("JOB", ignoreCase = true) -> Triple("✅", SomadhanSuccess, SomadhanSuccessLight)
                                        event.eventType.contains("KYC", ignoreCase = true) || event.eventType.contains("VERIF", ignoreCase = true) -> Triple("🛡️", SomadhanInfo, SomadhanInfo.copy(alpha = 0.15f))
                                        event.eventType.contains("RATING", ignoreCase = true) || event.eventType.contains("STAR", ignoreCase = true) -> Triple("⭐", SomadhanOrange, SomadhanOrangeLight)
                                        event.eventType.contains("CHARGE", ignoreCase = true) || event.eventType.contains("EXTRA_BILL", ignoreCase = true) -> Triple("💰", SomadhanOrange, SomadhanOrangeLight)
                                        else -> if (isPositive) Triple("•", SomadhanSuccess, SomadhanSuccessLight) else Triple("•", SomadhanError, SomadhanErrorLight)
                                    }
                                }

                                val isWasFreeQuotaJob = remember(event.problemId, allTransactions) {
                                    event.problemId?.let { pId -> allTransactions.find { it.problemId == pId }?.wasFreeQuotaJob } ?: false
                                }

                                // Ground Rule ১৮/১৯ — শুধু হিস্ট্রি ইভেন্ট কার্ডই pulse করবে
                                // (ব্যবহারকারীর কনফার্মড scope), স্কোর/স্ট্যাট কার্ড/হেডার
                                // স্পর্শ করা হয়নি। GR20 (ফিল্টার/পেজিনেশন) প্রযোজ্য না — এই
                                // তালিকায় কোনো ফিল্টার/সার্চ/পেজিনেশন নেই (শুধু take(10))।
                                // GR21 (realtime নতুন-ইভেন্ট আলাদা tracking) ইচ্ছাকৃতভাবে বাদ
                                // দেওয়া হয়েছে (ব্যবহারকারীর সিদ্ধান্ত, DisputeCenter-এর মতোই)।
                                val eventCardPulse = rememberFieldChangePulse(
                                    value = event,
                                    isManualRefreshing = isRefreshing,
                                    sessionKey = "reputation_detail_$userId",
                                    viewModel = viewModel,
                                    flashOnReentry = false
                                )
                                PulsingValue(isUpdating = eventCardPulse) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(iconBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = eventIcon, fontSize = 18.sp)
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        val isExtraBillEvent = event.eventType.contains("CHARGE", ignoreCase = true) ||
                                                event.eventType.contains("EXTRA_BILL", ignoreCase = true)

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = event.note.ifBlank { event.eventType },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SomadhanTextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = Formatters.formatTimeAgo(event.createdAt),
                                                    fontSize = 11.sp,
                                                    color = SomadhanTextHint
                                                )
                                                if (isExtraBillEvent) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(SomadhanOrange.copy(alpha = 0.12f))
                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "💰 Extra Bill",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SomadhanOrange
                                                        )
                                                    }
                                                }
                                                if ((event.eventType == "JOB_COMPLETED" || event.eventType.contains("COMPLETED", ignoreCase = true)) && isWasFreeQuotaJob) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(SomadhanSuccessLight)
                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "🆓 কমিশন-ফ্রি জব",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SomadhanSuccess
                                                        )
                                                    }
                                                }
                                            }
                                            if (isExtraBillEvent && event.problemId != null) {
                                                val sumForProb = problemGainedMap[event.problemId] ?: event.scoreChange
                                                val probUsageText = "${DistanceUtil.toBengaliDigits(String.format(Locale.US, "%.1f", sumForProb))}/${DistanceUtil.toBengaliDigits(String.format(Locale.US, "%.0f", extraBillRepCap))} পয়েন্ট ব্যবহৃত এই পোস্টে"
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = probUsageText,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = SomadhanTextSecondary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isPositive) SomadhanSuccessLight else SomadhanErrorLight
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            val changeText = if (isPositive) {
                                                "+${DistanceUtil.toBengaliDigits(String.format(Locale.US, "%.1f", event.scoreChange))}"
                                            } else {
                                                "-${DistanceUtil.toBengaliDigits(String.format(Locale.US, "%.1f", -event.scoreChange))}"
                                            }
                                            Text(
                                                text = changeText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPositive) SomadhanSuccess else SomadhanError
                                            )
                                        }
                                    }
                                }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        } // effectiveUser == null / else বন্ধ
            } // SyncAwareContent বন্ধ
        } // SomadhanPullToRefresh বন্ধ
    }
}
