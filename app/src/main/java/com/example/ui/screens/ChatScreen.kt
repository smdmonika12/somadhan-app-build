package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.util.ImageStorageUtil
import com.example.data.entity.MessageEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.UserEntity
import com.example.data.remote.SupabaseRealtimeManager
import com.example.ui.components.ChatSkeleton
import com.example.ui.components.DirectContractDialog
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.SyncBlockedRetryState
import com.example.ui.components.rememberMinimumSkeletonGate
import com.example.ui.components.rememberSessionAwareSkeletonGate
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.theme.SomadhanYellowVerified
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.ChatPolicyGuard
import com.example.util.DistanceUtil
import com.example.util.FileAttachmentUtil
import com.example.util.Formatters
import com.example.util.ProcessedAttachment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.components.BottomSlideAlertDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    problemId: String,
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val problem by viewModel.selectedProblem.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val messages by viewModel.problemMessages.collectAsStateWithLifecycle()
    val typingStatusMap by viewModel.typingStatusMap.collectAsStateWithLifecycle()

    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()
    val minimumSkeletonActive = rememberSessionAwareSkeletonGate(
        sessionKey = "chat_$problemId",
        viewModel = viewModel
    )
    // ব্যাচ ৮ migration — ক্যাটেগরি C, SOMADHAN_LOADING_PATTERN_MASTER_PROMPT.md অনুযায়ী
    // ChatScreen-এর target ডিজাইন MessagesScreen-এর প্যাটার্ন হুবহু কপি (ক্যাটেগরি D): app
    // session-এ প্রথমবার ঢোকার সময় একবার loading/skeleton, তারপর realtime-এ নতুন মেসেজ এলে বা
    // re-entry হলে আর কখনো skeleton/shimmer দেখাবে না। আগে শুধু
    // `problem == null || minimumSkeletonActive`-এই early-return হতো — `minimumSkeletonActive`
    // নিজে থেকেই sessionKey একবার markLoadedOnce() হলে চিরকাল false থাকে (rule ১-এর গ্যারান্টি
    // অক্ষত), কিন্তু `problem == null` অংশটা প্রথম-ভিজিটের বাইরেও সত্যি হয়ে যেতে পারতো (যেমন
    // realtime resync-এর সময় `selectedProblem` StateFlow ক্ষণিকের জন্য null হলে), যা এই
    // sessionKey ইতিমধ্যে একবার লোড হয়ে যাওয়ার পরেও আবার ChatSkeleton flash করাতে পারতো — এটাই
    // MessagesScreen-এর "আর কখনো flash না" গ্যারান্টি থেকে ব্যতিক্রম ছিল। নিচের
    // `!viewModel.hasLoadedOnce(...)` শর্তটা যোগ করে এই ব্যতিক্রম বন্ধ করা হলো: sessionKey একবার
    // মার্ক-লোডেড হয়ে গেলে, এই কারণেই আর কখনো early-return/skeleton হবে না।
    if (!viewModel.hasLoadedOnce("chat_$problemId") && (problem == null || minimumSkeletonActive)) {
        // ব্যাচ ৯ (এই সেশন) — MessagesScreen-এ যেমন `SyncAwareContent`-এর `ERROR` ব্র্যাঞ্চে
        // এরর+রিট্রাই UI আছে, `ChatScreen`-এ সেটা এতদিন ছিল না (শুধু null/loading চেক)।
        // `problem` (selectedProblem) `users`/`problems` bulk-pull-এরই অংশ, তাই এখানেও
        // `initialSyncPhase` প্রাসঙ্গিক signal — এটা ইতিমধ্যে `ERROR`-এ থাকলে (মানে শুরুর
        // bulk-pull সম্পূর্ণ ব্যর্থ হয়ে গেছে) `problem` কখনোই আসবে না, তাই চিরকাল ChatSkeleton-এ
        // আটকে না রেখে `ReputationDetailScreen`/`JobTrackingScreen`-এর মতো একই
        // `SyncBlockedRetryState(onRetry = viewModel.retryInitialSync())` প্যাটার্ন এখানেও বসানো
        // হলো। rule ১-এর (`hasLoadedOnce`/`minimumSkeletonActive`) গেট-লজিক অপরিবর্তিত।
        if (problem == null && initialSyncPhase == SupabaseRealtimeManager.SyncPhase.ERROR) {
            SyncBlockedRetryState(
                onRetry = { viewModel.retryInitialSync() },
                modifier = Modifier.fillMaxSize()
            )
            return
        }
        val isSolverViewer = currentUser?.role == "SOLVER"
        ChatSkeleton(
            modifier = Modifier.fillMaxSize(),
            tint = if (isSolverViewer) SomadhanOrange else Color(0xFF1D4ED8)
        )
        return
    }
    LaunchedEffect(problemId) { viewModel.markLoadedOnce("chat_$problemId") }

    val chatMessagesPaged = viewModel.chatMessagesPaged
    val chatMessagesLoadingMore = viewModel.chatMessagesLoadingMore
    val chatMessagesHasMore = viewModel.chatMessagesHasMore

    var inputMessage by remember { mutableStateOf("") }
    var selectedAttachment by remember { mutableStateOf<ProcessedAttachment?>(null) }
    var isProcessingFile by remember { mutableStateOf(false) }
    var previewImageDialogUrl by remember { mutableStateOf<String?>(null) }
    var showDirectContractDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var contractForReleaseConfirmation by remember { mutableStateOf<ProblemEntity?>(null) }
    var contractForDeclineProposalId by remember { mutableStateOf<String?>(null) }
    var declineReasonText by remember { mutableStateOf("") }
    var releaseRatingStars by remember { mutableStateOf(5) }
    var releaseReviewComment by remember { mutableStateOf("") }
    var policyViolationResult by remember { mutableStateOf<ChatPolicyGuard.DetectionResult?>(null) }
    var showPolicyViolationDialog by remember { mutableStateOf(false) }
    var showSettleDisputeDialog by remember { mutableStateOf(false) }
    var processingProposalId by remember { mutableStateOf<String?>(null) }
    var proposalActionError by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isDeclining by remember { mutableStateOf(false) }
    var declineError by remember { mutableStateOf<String?>(null) }
    var isConfirmingRelease by remember { mutableStateOf(false) }
    var confirmReleaseError by remember { mutableStateOf<String?>(null) }
    var isSettlingDispute by remember { mutableStateOf(false) }
    var settleDisputeError by remember { mutableStateOf<String?>(null) }
    var isRequestingAdminAssistance by remember { mutableStateOf(false) }
    var isSubmittingDirectContract by remember { mutableStateOf(false) }
    var directContractError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(problemId) {
        viewModel.selectProblem(problemId)
        viewModel.resetChatMessagesPagination(problemId)
        viewModel.markMessagesAsReadForProblem(problemId)
        // Realtime Scoping ফিক্স, ধাপ ৩ (dual-run) — এই problemId-র `problem:<id>` broadcast
        // channel join করা, পুরনো টেবিল-ওয়াইড messages subscription-এর পাশাপাশি (প্রতিস্থাপন না)।
        viewModel.joinProblemChatBroadcast(problemId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            viewModel.markMessagesAsReadForProblem(problemId)
        }
    }

    // Load older messages when scrolling near the end of the reverse list (which corresponds to older messages at top)
    LaunchedEffect(listState, chatMessagesPaged.size, chatMessagesHasMore, chatMessagesLoadingMore) {
        androidx.compose.runtime.snapshotFlow {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 4
        }.collect { shouldLoadOlder ->
            if (shouldLoadOlder && chatMessagesHasMore && !chatMessagesLoadingMore) {
                viewModel.loadOlderMessages(problemId)
            }
        }
    }

    val isSolver = currentUser?.role == "SOLVER"
    val brandColor = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8)

    // Resolve other party ID and name
    val otherPartyId = remember(problemId, problem, messages, currentUser?.id, allUsers) {
        when {
            problem != null -> {
                if (problem?.userId == currentUser?.id) problem?.acceptedSolverId ?: "" else problem?.userId ?: ""
            }
            problemId.startsWith("DIR_") -> {
                val parts = problemId.removePrefix("DIR_").split("_")
                parts.firstOrNull { it != currentUser?.id } ?: ""
            }
            chatMessagesPaged.isNotEmpty() -> {
                chatMessagesPaged.firstOrNull { it.senderId != currentUser?.id }?.senderId
                    ?: chatMessagesPaged.firstOrNull()?.receiverId ?: ""
            }
            messages.isNotEmpty() -> {
                messages.firstOrNull { it.senderId != currentUser?.id }?.senderId
                    ?: messages.firstOrNull()?.receiverId ?: ""
            }
            else -> ""
        }
    }

    val otherPartyUser = remember(otherPartyId, allUsers) {
        allUsers.find { it.id == otherPartyId }
    }

    val otherPartyName = when {
        problem?.userId == currentUser?.id -> problem?.acceptedSolverName ?: otherPartyUser?.name ?: "সমাধানকারী"
        problem != null -> problem?.userName ?: otherPartyUser?.name ?: "ব্যবহারকারী"
        otherPartyUser != null -> otherPartyUser.name
        else -> "সরাসরি বার্তা"
    }

    // Check if other party is typing
    val otherTypingKey = "${problemId}_${otherPartyId}"
    val otherTypingTimestamp = typingStatusMap[otherTypingKey]
    val isOtherTyping = otherTypingTimestamp != null && (System.currentTimeMillis() - otherTypingTimestamp) < 8000L

    // Typing dispatcher with debounce
    LaunchedEffect(inputMessage) {
        if (inputMessage.isNotBlank()) {
            viewModel.setTypingStatus(problemId, true)
            delay(3500)
            viewModel.setTypingStatus(problemId, false)
        } else {
            viewModel.setTypingStatus(problemId, false)
        }
    }

    DisposableEffect(problemId) {
        onDispose {
            viewModel.setTypingStatus(problemId, false)
            // Realtime Scoping ফিক্স, ধাপ ৩ — screen ছাড়ার সময় broadcast channel unsubscribe
            // (memory/connection leak এড়াতে)।
            viewModel.leaveProblemChatBroadcast(problemId)
        }
    }

    // File Picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingFile = true
            coroutineScope.launch {
                val result = FileAttachmentUtil.processAttachment(context, uri)
                isProcessingFile = false
                if (result.isSuccess) {
                    val attachment = result.getOrNull()
                    selectedAttachment = attachment
                    Toast.makeText(context, "ফাইল যুক্ত করা হয়েছে: ${attachment?.fileName}", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "ফাইল যুক্ত করা যায়নি।"
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(brandColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val otherPartyPhoto = otherPartyUser?.profileImageUri
                                val isPhotoValid = ImageStorageUtil.isValidDisplayUri(otherPartyPhoto)
                                if (isPhotoValid) {
                                    var loadFailed by remember(otherPartyPhoto) { mutableStateOf(false) }
                                    if (!loadFailed) {
                                        AsyncImage(
                                            model = otherPartyPhoto,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                            onError = { loadFailed = true }
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = brandColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = brandColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = otherPartyName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = SomadhanTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isOtherTyping) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• টাইপ করছে...",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = brandColor
                                        )
                                    }
                                }
                                Text(
                                    text = problem?.title ?: "সরাসরি পি২পি যোগাযোগ",
                                    fontSize = 11.sp,
                                    color = SomadhanTextHint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
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
                    actions = {
                        // Create Direct Project Offer Button (for Clients chatting with solvers)
                        if (!isSolver && (otherPartyUser?.role == "SOLVER" || otherPartyUser != null)) {
                            IconButton(
                                onClick = { showDirectContractDialog = true },
                                modifier = Modifier.testTag("chat_create_direct_contract_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = "কাজের প্রস্তাব দিন",
                                    tint = Color(0xFF059669)
                                )
                            }
                        }

                        // Report Abuse
                        IconButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier.testTag("chat_report_abuse_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "রিপোর্ট করুন",
                                tint = SomadhanTextHint,
                                modifier = Modifier.size(20.dp)
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
        bottomBar = {
            if (problem?.status == "COMPLETED" || problem?.status == "CANCELLED") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SomadhanCardBg)
                        .border(1.dp, SomadhanDivider)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "এই কাজ সম্পন্ন/বাতিল হয়ে গেছে বলে চ্যাট বন্ধ রয়েছে",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SomadhanTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SomadhanBg)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    // Attachment preview bar if any
                    selectedAttachment?.let { att ->
                        AttachmentPreviewBar(
                            attachment = att,
                            onRemove = { selectedAttachment = null }
                        )
                    }

                    // Chat Policy Caution Strip (Real-time proactive warning)
                    if (ChatPolicyGuard.hasSuspiciousPatterns(inputMessage)) {
                        Surface(
                            color = SomadhanError.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                            border = BorderStroke(1.dp, SomadhanError.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = SomadhanError,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "সতর্কতা: ফোন নম্বর, সামাজিক মাধ্যম বা অ্যাপের বাইরে লেনদেন প্ল্যাটফর্মের নীতিমালা বিরোধী।",
                                    fontSize = 11.sp,
                                    color = SomadhanError,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // Chat Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider)
                            .background(SomadhanBg)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attachment button
                        IconButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.size(40.dp)
                        ) {
                            if (isProcessingFile) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = brandColor)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "ফাইল বা ছবি যুক্ত করুন",
                                    tint = if (selectedAttachment != null) brandColor else SomadhanTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        OutlinedTextField(
                            value = inputMessage,
                            onValueChange = { inputMessage = it },
                            placeholder = { Text("একটি বার্তা লিখুন...", fontSize = 13.sp, color = SomadhanTextHint) },
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = SomadhanBorder,
                                focusedContainerColor = SomadhanCardBg,
                                unfocusedContainerColor = SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        val canSend = inputMessage.isNotBlank() || selectedAttachment != null

                        IconButton(
                            onClick = {
                                val contentToSend = inputMessage.trim()
                                val attachmentToSend = selectedAttachment
                                if (contentToSend.isNotBlank() || attachmentToSend != null) {
                                    // Scan for off-platform communication / unauthorized payment policy violation
                                    val scanResult = ChatPolicyGuard.scanMessage(contentToSend)
                                    if (scanResult.isViolation) {
                                        policyViolationResult = scanResult
                                        showPolicyViolationDialog = true

                                        // Trigger dynamic reputation penalty engine
                                        currentUser?.let { user ->
                                            viewModel.triggerDynamicReputationEvent(
                                                userId = user.id,
                                                eventType = scanResult.ruleKey,
                                                problemId = problemId,
                                                defaultScore = 15.0,
                                                defaultCap = 30.0,
                                                defaultPenalty = 15.0,
                                                isPositive = false,
                                                customNote = "চ্যাটে অফ-প্ল্যাটফর্ম লেনদেনের চেষ্টা (${scanResult.warningTitle})"
                                            )
                                        }
                                        return@IconButton
                                    }

                                    viewModel.sendMessage(
                                        problemId = problemId,
                                        receiverId = otherPartyId,
                                        content = contentToSend.ifBlank { "সংযুক্ত ফাইল: ${attachmentToSend?.fileName}" },
                                        fileUrl = attachmentToSend?.uriString,
                                        fileName = attachmentToSend?.fileName,
                                        fileType = attachmentToSend?.fileType
                                    )
                                    inputMessage = ""
                                    selectedAttachment = null
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    if (canSend) brandColor else SomadhanBorder
                                )
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "পাঠান",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SomadhanBg)
        ) {
            val currentProb = problem
            // "সমঝোতা" ও "অ্যাডমিন যুক্ত করুন" বাটন শুধু তখনই দেখাবে যখন চ্যাট সক্রিয় (কাজ COMPLETED/CANCELLED
            // হয়ে যায়নি), dispute বর্তমানে চালু আছে (এখনো সমঝোতায় সেটেল হয়নি), এবং এডমিন এখনো কোনো
            // রেজলিউশন সিদ্ধান্ত দেয়নি — এই তিনটার যেকোনো একটা না মিললে বাটন দুটো হাইড থাকবে।
            val isDisputeActive = currentProb?.isDisputed == true &&
                currentProb.disputeSettledAt == null &&
                currentProb.disputeResolutionDecision == null &&
                currentProb.status != "COMPLETED" &&
                currentProb.status != "CANCELLED"
            val isDisputeSettled = currentProb?.isDisputed == true && currentProb.disputeSettledAt != null

            // Dispute Active Banner with Settlement & Admin options
            if (isDisputeActive && currentProb != null) {
                val isDisputeInitiator = currentProb.disputeInitiatorId == currentUser?.id
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "বিরোধ সক্রিয়: ${currentProb.disputeReason?.ifBlank { "আলোচনা চলছে" } ?: "আলোচনা চলছে"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isDisputeInitiator) {
                                    Button(
                                        onClick = {
                                            showSettleDisputeDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp).testTag("settle_dispute_btn")
                                    ) {
                                        Icon(Icons.Default.Handshake, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("সমঝোতা", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                if (currentProb.isAdminInvolvedInChat) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF4F46E5).copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("অ্যাডমিন যুক্ত", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            isRequestingAdminAssistance = true
                                            viewModel.requestAdminAssistance(
                                                currentProb,
                                                onSuccess = {
                                                    isRequestingAdminAssistance = false
                                                    Toast.makeText(context, "অ্যাডমিনকে এই চ্যাটে সহায়তার জন্য যুক্ত করা হয়েছে।", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = {
                                                    isRequestingAdminAssistance = false
                                                }
                                            )
                                        },
                                        enabled = !isRequestingAdminAssistance,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp).testTag("chat_add_admin_btn")
                                    ) {
                                        if (isRequestingAdminAssistance) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("যুক্ত হচ্ছে...", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        } else {
                                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("অ্যাডমিন যুক্ত করুন", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        if (!isDisputeInitiator) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ℹ️ শুধু বিরোধ-উদ্যোক্তাই সমঝোতা নিশ্চিত করতে পারবেন",
                                fontSize = 10.5.sp,
                                color = Color(0xFF991B1B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else if (isDisputeSettled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanSuccess.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SomadhanSuccess, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "✓ বিরোধ পারস্পরিক সমঝোতায় নিষ্পত্তি হয়েছে",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanSuccess
                        )
                    }
                }
            }

            // Messages List (Reverse Layout: bottom is index 0 / newest message)
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (isOtherTyping) {
                    item(key = "typing_indicator") {
                        TypingIndicatorBubble(brandColor = brandColor, otherPartyName = otherPartyName)
                    }
                }

                // If chatMessagesPaged has items use it; fallback to messages sorted descending
                val displayMessages = if (chatMessagesPaged.isNotEmpty()) {
                    chatMessagesPaged
                } else {
                    messages.sortedByDescending { it.timestamp }
                }

                items(displayMessages, key = { it.id }) { msg ->
                    val isMyMsg = msg.senderId == currentUser?.id
                    val msgProblem = allProblems.find { it.id == msg.problemId } ?: problem

                    ChatMessageItem(
                        message = msg,
                        isMyMsg = isMyMsg,
                        brandColor = brandColor,
                        relatedProblem = msgProblem,
                        isSolver = isSolver,
                        currentUserId = currentUser?.id,
                        onOpenImage = { url -> previewImageDialogUrl = url },
                        onOpenFile = { url, fileName ->
                            FileAttachmentUtil.openFile(context, url, fileName)
                        },
                        onAcceptProposal = { pid ->
                            processingProposalId = "accept_$pid"
                            proposalActionError = null
                            viewModel.acceptDirectContractProposal(
                                problemId = pid,
                                onSuccess = {
                                    processingProposalId = null
                                    Toast.makeText(context, "প্রস্তাবটি গৃহীত হয়েছে! কাজ এখন চলমান।", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    processingProposalId = null
                                    proposalActionError = pid to err
                                }
                            )
                        },
                        onDeclineProposal = { pid ->
                            contractForDeclineProposalId = pid
                            declineReasonText = ""
                        },
                        onReleasePayment = { prob ->
                            contractForReleaseConfirmation = prob
                        },
                        onRequestRelease = { prob ->
                            processingProposalId = "release_${prob.id}"
                            proposalActionError = null
                            viewModel.requestJobRelease(
                                prob,
                                extraAmount = 0.0,
                                note = "কাজ সফলভাবে সম্পন্ন করেছি। অনুগ্রহ করে পেমেন্ট রিলিজ করুন।",
                                onSuccess = {
                                    processingProposalId = null
                                },
                                onError = { err ->
                                    processingProposalId = null
                                    proposalActionError = prob.id to err
                                }
                            )
                        },
                        processingProposalId = processingProposalId,
                        proposalActionError = proposalActionError,
                        onRetrySend = { messageId -> viewModel.retryFailedMessage(messageId) }
                    )
                }

                // Loading older messages indicator (appears at the top of scroll / end of reversed list)
                if (chatMessagesLoadingMore) {
                    item(key = "loading_older_indicator") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = brandColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "পুরোনো বার্তা লোড হচ্ছে...",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Direct Contract Dialog
    if (showDirectContractDialog && otherPartyUser != null) {
        val targetSolver = otherPartyUser
        val virtualCategories = remember(activeCategories) {
            activeCategories.filter { !it.isPhysical }
        }
        DirectContractDialog(
            solver = targetSolver,
            categories = virtualCategories,
            onDismiss = { showDirectContractDialog = false; directContractError = null },
            onSubmit = { title, description, category, budget, durationDays ->
                isSubmittingDirectContract = true
                directContractError = null
                viewModel.createDirectContractProject(
                    solver = targetSolver,
                    title = title,
                    description = description,
                    category = category,
                    budget = budget,
                    durationDays = durationDays,
                    address = liveLocation.address,
                    latitude = liveLocation.latitude,
                    longitude = liveLocation.longitude,
                    onSuccess = { createdProblemId ->
                        isSubmittingDirectContract = false
                        showDirectContractDialog = false
                        Toast.makeText(context, "সরাসরি কাজের চুক্তি প্রস্তাব পাঠানো হয়েছে! 🎯", Toast.LENGTH_LONG).show()
                        viewModel.selectProblem(createdProblemId)
                    },
                    onError = { error ->
                        isSubmittingDirectContract = false
                        directContractError = error
                    }
                )
            },
            isSubmitting = isSubmittingDirectContract,
            errorMessage = directContractError
        )
    }

    // Policy Violation Warning Alert Dialog
    if (showPolicyViolationDialog) {
        val violation = policyViolationResult
        BottomSlideAlertDialog(
            onDismissRequest = { showPolicyViolationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SomadhanError,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = violation?.warningTitle ?: "প্ল্যাটফর্ম পলিসি সতর্কতা!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanError,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column {
                    Text(
                        text = violation?.warningMessage ?: "অ্যাপের বাইরে সরাসরি যোগাযোগ বা লেনদেন করা কঠোরভাবে নিষিদ্ধ।",
                        fontSize = 13.sp,
                        color = SomadhanTextPrimary,
                        lineHeight = 18.sp
                    )

                    if (!violation?.detectedSnippet.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = SomadhanError.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SomadhanError.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "শনাক্তকৃত অংশ:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanError
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "\"${violation?.detectedSnippet}\"",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = SomadhanOrange.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "প্ল্যাটফর্মের নিরাপত্তা রক্ষার্থে আপনার রেপুটেশন ইঞ্জিন স্কোরে পেনাল্টি রেকর্ড করা হতে পারে।",
                                fontSize = 11.sp,
                                color = SomadhanOrange,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPolicyViolationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("মেসেজ সংশোধন করুন", color = Color.White, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        inputMessage = ""
                        selectedAttachment = null
                        showPolicyViolationDialog = false
                    }
                ) {
                    Text("মুছে ফেলুন", color = SomadhanTextSecondary, fontSize = 12.sp)
                }
            }
        )
    }

    // Settle Dispute Confirmation Dialog (Phase R & X)
    val probForSettle = problem
    if (showSettleDisputeDialog && probForSettle != null && probForSettle.disputeInitiatorId == currentUser?.id) {
        BottomSlideAlertDialog(
            onDismissRequest = { if (!isSettlingDispute) { showSettleDisputeDialog = false; settleDisputeError = null } },
            title = {
                Text(
                    text = "বিরোধ নিষ্পত্তি ও সমঝোতা 🤝",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "দুই পক্ষ কি সমঝোতায় পৌঁছেছেন? সমঝোতা নিশ্চিত করলে বিরোধ শেষ হবে এবং কাজের পরবর্তী ধাপগুলো পুনরায় সক্রিয় হবে।",
                        fontSize = 13.5.sp,
                        color = SomadhanTextSecondary,
                        lineHeight = 18.sp
                    )
                    if (settleDisputeError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = settleDisputeError ?: "", fontSize = 12.sp, color = SomadhanError)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSettlingDispute = true
                        settleDisputeError = null
                        viewModel.settleDispute(
                            probForSettle,
                            onSuccess = {
                                isSettlingDispute = false
                                showSettleDisputeDialog = false
                            },
                            onError = { err ->
                                isSettlingDispute = false
                                settleDisputeError = err
                            }
                        )
                    },
                    enabled = !isSettlingDispute,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess)
                ) {
                    if (isSettlingDispute) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সমঝোতা হচ্ছে...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Text("হ্যাঁ, সমঝোতা হয়েছে", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettleDisputeDialog = false; settleDisputeError = null }, enabled = !isSettlingDispute) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }


    // Report Abuse Dialog
    if (showReportDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Text(
                    text = "অভিযোগ / রিপোর্ট করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "আপত্তিকর বার্তা বা নীতিমালা লঙ্ঘনের বিবরণ লিখুন:",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("রিপোর্টের কারণ...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportReason.isNotBlank()) {
                            viewModel.reportAbuse(
                                reportedUserId = otherPartyId,
                                reason = reportReason,
                                contextType = "CHAT_$problemId"
                            )
                            showReportDialog = false
                            reportReason = ""
                            Toast.makeText(context, "আপনার অভিযোগটি সফলভাবে জমা নেওয়া হয়েছে।", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("রিপোর্ট পাঠান", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // Full screen image preview dialog
    if (previewImageDialogUrl != null) {
        Dialog(onDismissRequest = { previewImageDialogUrl = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                        IconButton(onClick = { previewImageDialogUrl = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = Color.White)
                        }
                    }
                    AsyncImage(
                        model = previewImageDialogUrl,
                        contentDescription = "ছবির প্রিভিউ",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

    // Decline Proposal Confirmation Dialog
    if (contractForDeclineProposalId != null) {
        val targetPid = contractForDeclineProposalId!!
        BottomSlideAlertDialog(
            onDismissRequest = { if (!isDeclining) { contractForDeclineProposalId = null; declineError = null } },
            title = {
                Text(
                    text = "কাজের প্রস্তাব প্রত্যাখ্যান",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "আপনি কি এই সরাসরি চুক্তি প্রস্তাবটি প্রত্যাখ্যান করতে চান?",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = declineReasonText,
                        onValueChange = { declineReasonText = it },
                        placeholder = { Text("প্রত্যাখ্যানের কারণ (ঐচ্ছিক)...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isDeclining
                    )
                    if (declineError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = declineError ?: "", fontSize = 12.sp, color = SomadhanError)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = declineReasonText.trim().ifBlank { "সমাধানকারী অপারগতা প্রকাশ করেছেন" }
                        isDeclining = true
                        declineError = null
                        viewModel.declineDirectContractProposal(
                            problemId = targetPid,
                            reason = reason,
                            onSuccess = {
                                isDeclining = false
                                contractForDeclineProposalId = null
                                declineReasonText = ""
                                Toast.makeText(context, "প্রস্তাবটি প্রত্যাখ্যান করা হয়েছে।", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                isDeclining = false
                                declineError = err
                            }
                        )
                    },
                    enabled = !isDeclining,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError)
                ) {
                    if (isDeclining) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("প্রত্যাখ্যান হচ্ছে...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Text("প্রত্যাখ্যান করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { contractForDeclineProposalId = null; declineError = null }, enabled = !isDeclining) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // Direct Contract Release & Complete Dialog
    if (contractForReleaseConfirmation != null) {
        val targetContract = contractForReleaseConfirmation!!
        val contractBudget = (targetContract.acceptedAmount ?: targetContract.minBudget).toInt()
        BottomSlideAlertDialog(
            onDismissRequest = { if (!isConfirmingRelease) { contractForReleaseConfirmation = null; confirmReleaseError = null } },
            title = {
                Text(
                    text = "কাজ সম্পন্ন ও পেমেন্ট রিলিজ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "\"${targetContract.title}\" কাজটি সফলভাবে সম্পন্ন হয়েছে?",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "অনুমোদন নিশ্চিত করলে Escrow থেকে ৳${DistanceUtil.toBengaliDigits(contractBudget.toString())} সরাসরি সমাধানকারীর ওয়ালেট ব্যালেন্সে যোগ হবে।",
                        fontSize = 12.5.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "সমাধানকারীকে রেটিং দিন (ঐচ্ছিক):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        (1..5).forEach { star ->
                            IconButton(
                                onClick = { releaseRatingStars = star },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (star <= releaseRatingStars) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = "$star star",
                                    tint = if (star <= releaseRatingStars) SomadhanYellowVerified else SomadhanTextHint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = releaseReviewComment,
                        onValueChange = { releaseReviewComment = it },
                        placeholder = { Text("মতামত বা রিভিউ লিখুন (যেমন: চমৎকার কাজ)...", fontSize = 12.sp) },
                        maxLines = 2,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isConfirmingRelease
                    )
                    if (confirmReleaseError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = confirmReleaseError ?: "", fontSize = 12.sp, color = SomadhanError)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prob = contractForReleaseConfirmation!!
                        isConfirmingRelease = true
                        confirmReleaseError = null
                        viewModel.confirmReleaseAndComplete(
                            problem = prob,
                            includeExtraAmount = false,
                            stars = releaseRatingStars,
                            reviewComment = releaseReviewComment,
                            onSuccess = {
                                isConfirmingRelease = false
                                contractForReleaseConfirmation = null
                                releaseReviewComment = ""
                            },
                            onError = { err ->
                                isConfirmingRelease = false
                                confirmReleaseError = err
                            }
                        )
                    },
                    enabled = !isConfirmingRelease,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess)
                ) {
                    if (isConfirmingRelease) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("নিশ্চিত হচ্ছে...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পেমেন্ট রিলিজ নিশ্চিত করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { contractForReleaseConfirmation = null; confirmReleaseError = null }, enabled = !isConfirmingRelease) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }
}

@Composable
fun ChatMessageItem(
    message: MessageEntity,
    isMyMsg: Boolean,
    brandColor: Color,
    relatedProblem: ProblemEntity? = null,
    isSolver: Boolean = false,
    currentUserId: String? = null,
    onOpenImage: (String) -> Unit,
    onOpenFile: (String, String?) -> Unit,
    onAcceptProposal: ((String) -> Unit)? = null,
    onDeclineProposal: ((String) -> Unit)? = null,
    onReleasePayment: ((ProblemEntity) -> Unit)? = null,
    onRequestRelease: ((ProblemEntity) -> Unit)? = null,
    processingProposalId: String? = null,
    proposalActionError: Pair<String, String>? = null,
    // [Offline Action Gating ধাপ ৯] messenger-স্টাইল "পাঠানো যায়নি" + retry -- শুধু isMyMsg &&
    // message.sendStatus == "FAILED" হলেই নিচে দেখানো হবে, ট্যাপ করলে এই কলব্যাক ফায়ার হবে।
    onRetrySend: ((String) -> Unit)? = null
) {
    if (message.isSystemEvent) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SomadhanTextHint.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = message.content,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = SomadhanTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val isImage = message.fileType == "image" ||
            message.fileType?.startsWith("image", ignoreCase = true) == true ||
            message.fileUrl?.endsWith(".jpg", ignoreCase = true) == true ||
            message.fileUrl?.endsWith(".png", ignoreCase = true) == true ||
            message.fileUrl?.endsWith(".jpeg", ignoreCase = true) == true

    // 1. ADMIN MESSAGE - Prominently highlighted across the whole chat width
    val isActuallyAdmin = message.isAdminMessage ||
            message.senderName.contains("Support Manager", ignoreCase = true) ||
            message.senderName.contains("সাপোর্ট ম্যানেজার", ignoreCase = true) ||
            message.senderName.contains("অ্যাডমিন", ignoreCase = true) ||
            message.senderName.contains("Admin", ignoreCase = true) ||
            message.senderId.startsWith("ADMIN", ignoreCase = true) ||
            message.senderId.equals("ADMIN_SYSTEM", ignoreCase = true) ||
            message.senderId.equals("admin", ignoreCase = true)

    if (isActuallyAdmin) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)), // Rich Deep Indigo
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFF6366F1), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4F46E5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24), // Vibrant gold shield
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Support Manager",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF4338CA))
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "OFFICIAL",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = "সমাধান কাস্টমার সাপোর্ট টিম",
                                fontSize = 10.sp,
                                color = Color(0xFFC7D2FE)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // File Attachment if any
                    if (!message.fileUrl.isNullOrBlank()) {
                        if (isImage) {
                            AsyncImage(
                                model = message.fileUrl,
                                contentDescription = "সংযুক্ত ছবি",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onOpenImage(message.fileUrl) },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF312E81))
                                    .clickable { onOpenFile(message.fileUrl, message.fileName) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.fileName ?: "সংযুক্ত ডকুমেন্ট",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ট্যাপ করে খুলুন",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFFA5B4FC)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Admin Message Content
                    if (message.content.isNotBlank()) {
                        Text(
                            text = message.content,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFF8FAFC),
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${Formatters.formatTimeAgo(message.timestamp)} • ${Formatters.formatDateTimeBengali(message.timestamp)}",
                        fontSize = 9.5.sp,
                        color = Color(0xFFA5B4FC),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
        return
    }

    // Determine sender profile colors
    val isSenderSolver = if (isMyMsg) {
        isSolver
    } else {
        if (relatedProblem != null && message.senderId == relatedProblem.acceptedSolverId) {
            true
        } else {
            !isSolver
        }
    }

    // Dynamic color matching with profiles
    val bubbleBackground: Color
    val bubbleBorder: Color
    val bubbleTextColor: Color
    val senderRoleBadgeText: String?

    val senderDisplayName: String? = if (!isMyMsg) {
        val resolvedName = message.senderName.trim()
        if (resolvedName.isNotBlank() && resolvedName != "ব্যবহারকারী" && resolvedName != "সমাধানকারী" && resolvedName != "সরাসরি বার্তা") {
            resolvedName
        } else {
            if (isSenderSolver) {
                relatedProblem?.acceptedSolverName ?: "সমাধানকারী"
            } else {
                relatedProblem?.userName ?: "গ্রাহক"
            }
        }
    } else {
        null
    }

    if (isMyMsg) {
        if (isSenderSolver) {
            bubbleBackground = SomadhanOrange // Solver profile orange
            bubbleBorder = SomadhanOrange
            bubbleTextColor = Color.White
            senderRoleBadgeText = null
        } else {
            bubbleBackground = Color(0xFF2563EB) // User/client profile royal blue
            bubbleBorder = Color(0xFF2563EB)
            bubbleTextColor = Color.White
            senderRoleBadgeText = null
        }
    } else {
        if (isSenderSolver) {
            bubbleBackground = Color(0xFFFFF7ED) // Warm solver light amber
            bubbleBorder = Color(0xFFFDBA74) // Solver border
            bubbleTextColor = Color(0xFF7C2D12) // Deep solver brown-orange
            senderRoleBadgeText = "🛠️ ${senderDisplayName ?: "সমাধানকারী"}"
        } else {
            bubbleBackground = Color(0xFFEFF6FF) // Customer soft blue
            bubbleBorder = Color(0xFFBFDBFE) // Customer border
            bubbleTextColor = Color(0xFF1E3A8A) // Deep customer blue
            senderRoleBadgeText = "👤 ${senderDisplayName ?: "গ্রাহক"}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isMyMsg) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isMyMsg) 14.dp else 2.dp,
                        bottomEnd = if (isMyMsg) 2.dp else 14.dp
                    )
                )
                .background(bubbleBackground)
                .border(
                    1.dp,
                    bubbleBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                // Role Badge for Other Party
                if (!isMyMsg && senderRoleBadgeText != null) {
                    Text(
                        text = senderRoleBadgeText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSenderSolver) SomadhanOrange else Color(0xFF2563EB)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Dispute Notice inside message
                if (message.isDisputeNotice) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isMyMsg) Color.White.copy(alpha = 0.2f) else Color(0xFFFEF3C7))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = if (isMyMsg) Color.White else Color(0xFFD97706), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("⚖️ বিরোধ নোটিশ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isMyMsg) Color.White else Color(0xFFD97706))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Direct Contract Proposal Box
                if (message.isDirectContractProposal) {
                    val proposalStatus = relatedProblem?.directContractStatus ?: "PENDING_ACCEPTANCE"

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMyMsg) Color.White.copy(alpha = 0.15f) else SomadhanBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Handshake,
                                    contentDescription = null,
                                    tint = if (isMyMsg) Color.White else Color(0xFF1D4ED8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🎯 সরাসরি কাজের চুক্তি প্রস্তাব",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMyMsg) Color.White else SomadhanTextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (message.directContractBudget != null && message.directContractBudget > 0.0) {
                                Text(
                                    text = "বাজেট: ৳${DistanceUtil.toBengaliDigits(message.directContractBudget.toInt().toString())} (Escrow সুরক্ষিত)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMyMsg) Color.White else SomadhanSuccess
                                )
                            }
                            if (!message.directContractDuration.isNullOrBlank()) {
                                Text(
                                    text = "মেয়াদ: ${message.directContractDuration}",
                                    fontSize = 10.5.sp,
                                    color = if (isMyMsg) Color.White.copy(alpha = 0.9f) else SomadhanTextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Interactive Status / Action Buttons
                            when (proposalStatus) {
                                "PENDING_ACCEPTANCE" -> {
                                    if (!isMyMsg) {
                                        // Solver viewing proposal -> Show Accept / Decline buttons!
                                        val acceptId = "accept_${message.problemId}"
                                        val isAccepting = processingProposalId == acceptId
                                        val isProcessingAny = processingProposalId != null
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = { onAcceptProposal?.invoke(message.problemId) },
                                                enabled = !isProcessingAny,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f).testTag("accept_proposal_btn_${message.problemId}")
                                            ) {
                                                if (isAccepting) {
                                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(text = "গ্রহণ হচ্ছে...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                } else {
                                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(text = "গ্রহণ করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = { onDeclineProposal?.invoke(message.problemId) },
                                                enabled = !isProcessingAny,
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE11D48)),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE11D48)),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f).testTag("decline_proposal_btn_${message.problemId}")
                                            ) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = "প্রত্যাখ্যান", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                        if (proposalActionError?.first == message.problemId) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = proposalActionError.second,
                                                fontSize = 10.5.sp,
                                                color = Color(0xFFE11D48)
                                            )
                                        }
                                    } else {
                                        // Client waiting
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "⏳ সমাধানকারীর অনুমোদনের অপেক্ষায়",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                "ACCEPTED" -> {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        if (relatedProblem?.status == "COMPLETED" || proposalStatus == "COMPLETED") {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isMyMsg) Color.White.copy(alpha = 0.2f) else SomadhanSuccessLight)
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "🎉 কাজ সম্পন্ন ও পেমেন্ট রিলিজ সম্পন্ন হয়েছে",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isMyMsg) Color.White else SomadhanSuccess
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isMyMsg) Color.White.copy(alpha = 0.2f) else SomadhanSuccessLight)
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "✅ চুক্তি গৃহীত হয়েছে • কাজ চলমান",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isMyMsg) Color.White else SomadhanSuccess
                                                )
                                            }

                                            val isClientParty = !isSolver || currentUserId == relatedProblem?.userId
                                            if (isClientParty && relatedProblem != null) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Button(
                                                    onClick = { onReleasePayment?.invoke(relatedProblem) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.fillMaxWidth().testTag("chat_release_payment_btn")
                                                ) {
                                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(text = "💰 পেমেন্ট রিলিজ ও সম্পন্ন করুন", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            } else if (isSolver && relatedProblem != null) {
                                                if (relatedProblem.hasReleaseRequest) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "⏳ গ্রাহকের পেমেন্ট রিলিজ অনুমোদনের অপেক্ষায়",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (isMyMsg) Color.White else SomadhanOrange
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    val releaseId = "release_${message.problemId}"
                                                    val isRequestingRelease = processingProposalId == releaseId
                                                    val isProcessingAnyRelease = processingProposalId != null
                                                    OutlinedButton(
                                                        onClick = { onRequestRelease?.invoke(relatedProblem) },
                                                        enabled = !isProcessingAnyRelease,
                                                        shape = RoundedCornerShape(6.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isMyMsg) Color.White else SomadhanOrange),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isMyMsg) Color.White else SomadhanOrange),
                                                        modifier = Modifier.fillMaxWidth().testTag("chat_request_release_btn")
                                                    ) {
                                                        if (isRequestingRelease) {
                                                            CircularProgressIndicator(
                                                                color = if (isMyMsg) Color.White else SomadhanOrange,
                                                                modifier = Modifier.size(12.dp),
                                                                strokeWidth = 2.dp
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(text = "পাঠানো হচ্ছে...", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                                        } else {
                                                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(text = "📢 কাজ সমাপ্তির রিলিজ অনুরোধ", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }
                                                    if (proposalActionError?.first == message.problemId) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = proposalActionError.second,
                                                            fontSize = 10.5.sp,
                                                            color = if (isMyMsg) Color.White else Color(0xFFE11D48)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                "COMPLETED" -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isMyMsg) Color.White.copy(alpha = 0.2f) else SomadhanSuccessLight)
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "🎉 কাজ সম্পন্ন ও পেমেন্ট রিলিজ সম্পন্ন হয়েছে",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMyMsg) Color.White else SomadhanSuccess
                                        )
                                    }
                                }
                                "DECLINED" -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isMyMsg) Color.White.copy(alpha = 0.2f) else Color(0xFFFFF1F2))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "❌ প্রস্তাবটি প্রত্যাখ্যাত হয়েছে",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMyMsg) Color.White else Color(0xFFE11D48)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // File Attachment Rendering
                if (!message.fileUrl.isNullOrBlank()) {
                    if (isImage) {
                        AsyncImage(
                            model = message.fileUrl,
                            contentDescription = "সংযুক্ত ছবি",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenImage(message.fileUrl) },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    } else {
                        // Document / other file preview badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isMyMsg) Color.White.copy(alpha = 0.2f) else SomadhanBg)
                                .clickable { onOpenFile(message.fileUrl, message.fileName) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (message.fileType == "document") Icons.Default.Description else Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = if (isMyMsg) Color.White else (if (isSenderSolver) SomadhanOrange else Color(0xFF2563EB)),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.fileName ?: "সংযুক্ত ডকুমেন্ট",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isMyMsg) Color.White else SomadhanTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "ট্যাপ করে খুলুন",
                                    fontSize = 9.sp,
                                    color = if (isMyMsg) Color.White.copy(alpha = 0.8f) else SomadhanTextHint
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = if (isMyMsg) Color.White else SomadhanTextHint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // Message Text Content
                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        fontSize = 13.sp,
                        color = bubbleTextColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Time and Read Status Tick
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = "${Formatters.formatTimeAgo(message.timestamp)} • ${Formatters.formatTimeExact(message.timestamp)}",
                fontSize = 9.sp,
                color = SomadhanTextHint
            )

            // [Offline Action Gating ধাপ ৯] sendStatus অনুযায়ী tick আইকন -- "FAILED" হলে ওয়ার্নিং
            // আইকন (নিচের retry Row-এ বিস্তারিত), "PENDING" হলে (এইমাত্র পাঠানো হচ্ছে, dual-write
            // এখনো শেষ হয়নি) কোনো tick না -- শুধু "SENT" (স্বাভাবিক, পুরনো ডিফল্টও এটাই) হলে আগের
            // মতোই isRead অনুযায়ী double-check।
            if (isMyMsg) {
                Spacer(modifier = Modifier.width(4.dp))
                when (message.sendStatus) {
                    "FAILED" -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "পাঠানো যায়নি",
                            tint = SomadhanError,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    "PENDING" -> {
                        // পাঠানো হচ্ছে -- কোনো tick না, শুধু ফাঁকা জায়গা (spacer alignment ঠিক রাখতে)
                    }
                    else -> {
                        if (message.isRead) {
                            // Double check colored (Seen / পঠিত)
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "পঠিত",
                                tint = SomadhanSuccess,
                                modifier = Modifier.size(13.dp)
                            )
                        } else {
                            // Double check gray (Delivered)
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "প্রেরিত",
                                tint = SomadhanTextHint,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        // [Offline Action Gating ধাপ ৯] messenger-স্টাইল "পাঠানো যায়নি" + ইনলাইন retry -- শুধু
        // নিজের FAILED মেসেজের নিচেই দেখাবে। ট্যাপ করলে onRetrySend(message.id) কল হয়, যেটা
        // ViewModel-এর retryFailedMessage() (requireOnlineOrWarn() দিয়ে গার্ডেড) কল করে।
        if (isMyMsg && message.sendStatus == "FAILED") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .padding(top = 2.dp)
                    .clickable { onRetrySend?.invoke(message.id) }
            ) {
                Text(
                    text = "পাঠানো যায়নি",
                    fontSize = 9.sp,
                    color = SomadhanError
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "আবার পাঠান",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = brandColor
                )
            }
        }
    }
}

@Composable
fun AttachmentPreviewBar(
    attachment: ProcessedAttachment,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SomadhanCardBg)
            .border(1.dp, SomadhanDivider)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (attachment.fileType == "image") Icons.Default.Image else Icons.Default.Description,
            contentDescription = null,
            tint = SomadhanOrange,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.fileName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SomadhanTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = attachment.formattedSize,
                fontSize = 9.sp,
                color = SomadhanTextHint
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "মুছে ফেলুন",
                tint = SomadhanTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun TypingIndicatorBubble(brandColor: Color, otherPartyName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots_transition")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1_alpha"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2_alpha"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3_alpha"
    )

    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SomadhanCardBg)
            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(brandColor.copy(alpha = dot1Alpha))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(brandColor.copy(alpha = dot2Alpha))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(brandColor.copy(alpha = dot3Alpha))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$otherPartyName টাইপ করছেন...",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = SomadhanTextSecondary
        )
    }
}

