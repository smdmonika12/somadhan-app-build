package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.example.BuildConfig
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.BidEntity
import com.example.data.entity.PlatformSettingEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.UserEntity
import com.example.data.repository.SomadhanRepository
import com.example.ui.viewmodel.SomadhanViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.JobTrackingSkeleton
import com.example.ui.components.rememberMinimumSkeletonGate
import com.example.ui.components.rememberSessionAwareSkeletonGate
import com.example.ui.components.SyncBlockedRetryState
import com.example.ui.components.PulsingValue
import com.example.ui.components.rememberFieldChangePulse
import com.example.data.remote.SupabaseRealtimeManager
import com.example.ui.components.DraggableMapSheet
import com.example.ui.components.MapSheetStateValue
import com.example.ui.components.ExtraAmountPaymentConfirmationDialog
import com.example.ui.components.MerchantPaymentDialog
import com.example.ui.components.PaymentConfirmationDialog
import com.example.ui.navigation.Screen
import com.example.util.DistanceUtil
import com.example.util.Formatters
import com.example.util.InstantJobReceiptUtil
import com.example.util.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import com.example.ui.components.SummaryTransitionContainer
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanInfoLight
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeContainer
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanOrangePressed
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanSurfaceVariant
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.ui.components.BottomSlideAlertDialog

/**
 * JobTrackingScreen: Live map tracking interface for both User and Solver.
 * Implements the exact UX/UI specified in mockup:
 * - Top Bar: Back button + ETA pill card
 * - Map: Markers for User and Solver, connected by route line, auto-fit camera
 * - Bottom Sheet: Status badge, 4-step progress stepper, other party profile + Call/Chat,
 *   job summary card, and role-driven CTA action button.
 */
@Composable
fun JobTrackingScreen(
    problemId: String,
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allPlatformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
    val allBids by viewModel.allBids.collectAsStateWithLifecycle()
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    // Find problem from stream or observe by ID
    val problem = allProblems.find { it.id == problemId }

    val minimumSkeletonActive = rememberSessionAwareSkeletonGate(
        sessionKey = "job_tracking_$problemId",
        viewModel = viewModel
    )
    if (problem == null || minimumSkeletonActive) {
        // Loading/Sync Fix Roadmap v2, ধাপ ৪ — বাল্ক-পুল (problems) সম্পূর্ণ ব্যর্থ হয়ে থাকলে
        // এই problemId কখনোই allProblems-এ আসবে না, তাই চিরকাল স্কেলিটনে আটকে থাকার বদলে
        // এরর+রিট্রাই দেখাও (এই স্ক্রিন টাকা-সংক্রান্ত escrow/payment ফ্লো-র অংশ বলে বিশেষভাবে
        // গুরুত্বপূর্ণ যে এটা চিরকাল স্পিন না করে)।
        if (problem == null && initialSyncPhase == SupabaseRealtimeManager.SyncPhase.ERROR) {
            val isSolverViewer = currentUser?.role.equals("SOLVER", ignoreCase = true)
            SyncBlockedRetryState(
                onRetry = { viewModel.retryInitialSync() },
                modifier = Modifier.fillMaxSize()
            )
            return
        }
        // currentUser is already known even before the specific problem loads,
        // so we can still tint the skeleton by role (Solver = orange, User/other = blue).
        val isSolverViewer = currentUser?.role.equals("SOLVER", ignoreCase = true)
        JobTrackingSkeleton(
            modifier = Modifier.fillMaxSize(),
            tint = if (isSolverViewer) SomadhanOrange else SomadhanInfo
        )
        return
    }
    LaunchedEffect(problemId) { viewModel.markLoadedOnce("job_tracking_$problemId") }

    // Normal (non-instant) posts do not use live tracking; redirect to normal problem detail screen
    if (!problem.isInstantJob) {
        LaunchedEffect(problem.id) {
            onNavigate(com.example.ui.navigation.Screen.ProblemDetail.createRoute(problem.id))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SomadhanBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SomadhanOrange)
        }
        return
    }

    // Top-level Phase P / Phase I.3: Auto-redirect & diagnostic logging for cancellation/expiry
    var isNavigatingAway by remember { mutableStateOf(false) }
    val safeNavigateBack: () -> Unit = remember(onNavigateBack) {
        {
            if (!isNavigatingAway) {
                isNavigatingAway = true
                onNavigateBack()
            }
        }
    }

    val currentUid = currentUser?.id ?: ""
    val isOwner = (currentUser?.id == problem.userId)
    val isUserRole = isOwner
    val role = if (isUserRole) "USER" else "SOLVER"
    val isAcceptedSolver = (
        currentUser?.id == problem.acceptedSolverId
    )
    val isSolverWithCancelledBid = remember(allBids, problem.id, currentUid) {
        !isUserRole && allBids.any { it.problemId == problem.id && it.solverId == currentUid && it.status == "CANCELLED" }
    }
    val isAdmin = currentUser?.role.equals("ADMIN", ignoreCase = true)
    val isAuthorized = isOwner || isAcceptedSolver || isAdmin || isSolverWithCancelledBid || (!problem.solverCancelledNotice.isNullOrBlank() && !isUserRole)

    if (!isAuthorized) {
        LaunchedEffect(Unit) {
            if (currentUser?.role.equals("SOLVER", ignoreCase = true)) {
                safeNavigateBack()
            } else {
                Toast.makeText(context, "আপনার এই কাজের ট্র্যাকিং দেখার অনুমতি নেই।", Toast.LENGTH_SHORT).show()
                onNavigate(com.example.ui.navigation.Screen.ProblemDetail.createRoute(problem.id))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SomadhanBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SomadhanOrange)
        }
        return
    }

    val unreadJobChatCount by remember(problem.id, currentUid) {
        viewModel.getMessagesForProblem(problem.id).map { messages ->
            messages.count { msg ->
                !msg.isRead && (msg.receiverId == currentUid || (msg.senderId != currentUid && msg.receiverId.isEmpty()))
            }
        }
    }.collectAsStateWithLifecycle(initialValue = 0)

    val solverCancelledBid = remember(allBids, problem.id, currentUid) {
        allBids.filter { it.problemId == problem.id && it.solverId == currentUid && it.status == "CANCELLED" }
            .maxByOrNull { it.createdAt }
    }

    var viewingSolverCancellationSummary by remember(problem.id) {
        mutableStateOf(!isUserRole && (!problem.solverCancelledNotice.isNullOrBlank() || solverCancelledBid != null))
    }

    LaunchedEffect(problem.solverCancelledNotice, solverCancelledBid) {
        if (!isUserRole && (!problem.solverCancelledNotice.isNullOrBlank() || solverCancelledBid != null)) {
            viewingSolverCancellationSummary = true
        }
    }

    var wasAlreadyCancelledOnEntry by remember { mutableStateOf(problem.jobStatus == "CANCELLED" || problem.status == "CANCELLED") }

    val isDisputeRelated = problem.isDisputed || problem.disputeResolvedAt != null || !problem.disputeResolutionDecision.isNullOrBlank() || !problem.disputeResolutionType.isNullOrBlank()

    LaunchedEffect(problem.jobStatus, problem.status, isNavigatingAway) {
        if (BuildConfig.DEBUG) {
            Log.d("InstantJobDebug", "JobTrackingScreen LaunchedEffect triggered: problemId=$problemId, jobStatus=${problem.jobStatus}, status=${problem.status}")
        }
        if (!isNavigatingAway && !wasAlreadyCancelledOnEntry && !isDisputeRelated && (problem.jobStatus == "CANCELLED" || problem.status == "CANCELLED")) {
            if (!viewingSolverCancellationSummary && isUserRole) {
                Toast.makeText(context, "নির্ধারিত সময়ের মধ্যে কোনো বিড গ্রহণ না হওয়ায় পোস্টটি বাতিল হয়ে গেছে", Toast.LENGTH_LONG).show()
                safeNavigateBack()
            }
        }
    }

    // Identify other party
    val otherUser = remember(problem, allUsers, isUserRole) {
        if (isUserRole) {
            val solverId = problem.acceptedSolverId
            allUsers.find { it.id == solverId }
        } else {
            allUsers.find { it.id == problem.userId }
        }
    }

    val escrow by viewModel.getEscrowForProblem(problemId).collectAsStateWithLifecycle(initialValue = null)

    // Default Coordinates from ProblemEntity
    val userLat = remember(problem) { if (problem.latitude != 0.0) problem.latitude else 23.7557 }
    val userLng = remember(problem) { if (problem.longitude != 0.0) problem.longitude else 90.3644 }

    // Solver Live Location (falls back to solver's profile location or offset coordinate)
    var solverLat by remember(problem, otherUser) {
        val sLat = problem.solverLiveLat ?: if (isUserRole) {
            otherUser?.latitude?.takeIf { it != 0.0 } ?: (userLat + 0.0095)
        } else {
            currentUser?.latitude?.takeIf { it != 0.0 } ?: (userLat + 0.0095)
        }
        mutableDoubleStateOf(sLat)
    }
    var solverLng by remember(problem, otherUser) {
        val sLng = problem.solverLiveLng ?: if (isUserRole) {
            otherUser?.longitude?.takeIf { it != 0.0 } ?: (userLng + 0.0075)
        } else {
            currentUser?.longitude?.takeIf { it != 0.0 } ?: (userLng + 0.0075)
        }
        mutableDoubleStateOf(sLng)
    }

    // Distance calculation
    val distanceKm = remember(userLat, userLng, solverLat, solverLng) {
        DistanceUtil.calculateDistanceKm(solverLat, solverLng, userLat, userLng)
    }

    // Estimated travel time (Assuming ~15 km/h in city traffic, or roughly 4 mins per km)
    val etaMinutes = remember(distanceKm) {
        val mins = (distanceKm * 4.5).toInt().coerceAtLeast(2)
        mins
    }

    val status = problem.jobStatus ?: if (problem.status == "COMPLETED") "JOB_COMPLETED" else "ACCEPTED"
    val isJobActive = status != "BROADCASTING" && status != "JOB_COMPLETED" && status != "CANCELLED" && problem.status != "COMPLETED" && problem.status != "CANCELLED"

    // 6.1 Solver Real-time GPS Location Broadcasting:
    // If role == "SOLVER" and job is active, collect LocationHelper.observeRealtimeLocation(context)
    // and periodically push lightweight partial coordinates to Room & Supabase (originally
    // written as "& Firestore" when this spec comment was authored -- updated ধাপ ৩৩.৫, Firebase
    // has since been fully removed and this now goes to Supabase via updateSolverLiveLocation()).
    // 6.4 Cleanup: When job becomes completed/cancelled or screen disposes, LaunchedEffect automatically cancels the Flow.
    LaunchedEffect(problemId, isJobActive, isUserRole) {
        if (!isUserRole && isJobActive) {
            LocationHelper.observeRealtimeLocation(context).collect { locationResult ->
                solverLat = locationResult.latitude
                solverLng = locationResult.longitude
                viewModel.updateSolverLiveLocation(
                    problemId = problemId,
                    lat = locationResult.latitude,
                    lng = locationResult.longitude
                )
            }
        }
    }

    // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগে এখানে FirebaseFirestore.addSnapshotListener() দিয়ে
    // সরাসরি "problems" ডকুমেন্ট শোনা হতো solverLiveLat/solverLiveLng পাওয়ার জন্য। এখন সেটা সরানো
    // হলো -- ধাপ ২০-এ বানানো SupabaseRealtimeManager-এর "problems" টেবিল realtime channel
    // ইতিমধ্যে প্রতিটা UPDATE-এ solverLiveLat/solverLiveLng সহ পুরো Problem row Room-এ upsert করে
    // (ProblemBidMappers.kt-এর toProblemEntity(), attachDatabase()-এ ধাপ ২২ থেকে সক্রিয়) --
    // নিচের LaunchedEffect(problem.solverLiveLat, problem.solverLiveLng) সেই Room পরিবর্তনই observe
    // করছে, তাই এই ডুপ্লিকেট Firestore listener ছাড়াই একই আচরণ বজায় থাকে (verified bridge, কোনো
    // নতুন কোড লাগেনি)।

    // Update solverLat / solverLng whenever ProblemEntity from Room emits new live coordinates
    LaunchedEffect(problem.solverLiveLat, problem.solverLiveLng) {
        val sLat = problem.solverLiveLat
        val sLng = problem.solverLiveLng
        if (sLat != null && sLng != null && sLat != 0.0 && sLng != 0.0) {
            solverLat = sLat
            solverLng = sLng
        }
    }

    // Dialog States
    var showSolverCancelDialog by remember { mutableStateOf(false) }
    var solverCancelReason by remember { mutableStateOf("") }
    var isSubmittingSolverJobCancel by remember { mutableStateOf(false) }
    var solverJobCancelError by remember { mutableStateOf<String?>(null) }
    // Bug fix (loading-lock): "পৌঁছে গেছি" and "কাজ শুরু করুন" had no isSubmitting lock at all
    // (unlike every other action button in this file), so a fast double-tap could fire
    // markSolverArrived()/markJobStarted() more than once before the first call's toast/UI
    // update landed. Same pattern as isSubmittingSolverJobCancel/isRequestingRelease above/below.
    var isSubmittingSolverArrived by remember { mutableStateOf(false) }
    var isSubmittingSolverStartJob by remember { mutableStateOf(false) }
    var showDisputeDialog by remember { mutableStateOf(false) }
    var disputeReasonInput by remember { mutableStateOf("") }
    var isSubmittingDispute by remember { mutableStateOf(false) }
    var disputeError by remember { mutableStateOf<String?>(null) }
    var showExtraAmountDialog by remember { mutableStateOf(false) }
    var extraAmountInput by remember { mutableStateOf("") }
    var extraAmountNoteInput by remember { mutableStateOf("") }
    var isSubmittingExtraAmount by remember { mutableStateOf(false) }
    var showExtraAmountConfirmDialog by remember { mutableStateOf(false) }
    var extraAmountForGatewayPayment by remember { mutableStateOf<Double?>(null) }
    // Loading-lock for the wallet-only extra-bill confirm path: true from tap until the
    // viewmodel's onSuccess/onError actually returns. Passed to the dialog's isLoading param
    // (combined internally with its own isProceeding) so the button shows a spinner and the
    // dialog cannot be dismissed/closed early — and, critically, so this composable is not torn
    // down (showExtraAmountConfirmDialog = false) before the backend call resolves.
    var isConfirmingExtraAmountPaid by remember { mutableStateOf(false) }
    var confirmExtraAmountError by remember { mutableStateOf<String?>(null) }
    var extraAmountGatewayPending by remember { mutableStateOf(false) }
    var extraAmountGatewayError by remember { mutableStateOf<String?>(null) }
    var isRequestingJobRelease by remember { mutableStateOf(false) }
    var jobReleaseError by remember { mutableStateOf<String?>(null) }
    var isRejectingExtraAmountTop by remember { mutableStateOf(false) }
    var rejectExtraAmountErrorTop by remember { mutableStateOf<String?>(null) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showCompletionSummary by remember { mutableStateOf(false) }
    var ratingStars by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    var isSubmittingRating by remember { mutableStateOf(false) }

    // SOLVER CANCELLATION SUMMARY VIEW:
    // If the solver cancelled or is viewing cancellation summary, stay on CancellationSummaryScreen until solver explicitly clicks "ফিরে যান" (Back).
    // Poster rebroadcasting or cancelling the post must NOT auto-redirect the solver away from this summary page.
    if (!isUserRole && viewingSolverCancellationSummary) {
        val relevantCancelledBid = solverCancelledBid
            ?: allBids.filter { it.problemId == problem.id && it.status == "CANCELLED" }.maxByOrNull { it.createdAt }

        SummaryTransitionContainer {
            CancellationSummaryScreen(
                problem = problem,
                cancelledBid = relevantCancelledBid,
                isUserRole = false,
                onNavigateBack = {
                    viewingSolverCancellationSummary = false
                    safeNavigateBack()
                },
                onRebroadcast = {},
                onFullyCancel = {}
            )
        }
        return
    }

    // FEATURE 3: DISPUTE RESOLUTION SUMMARY SCREEN (PHASE S & W MANDATORY RESULT - ONLY ON OFFICIAL ADMIN RESOLUTION/REFUND)
    val isDisputeResolved = problem.disputeResolvedAt != null || !problem.disputeResolutionDecision.isNullOrBlank() || !problem.disputeResolutionType.isNullOrBlank()
    val hasSeenDisputeResult = if (isUserRole) problem.disputeResultSeenByUser else problem.disputeResultSeenBySolver

    if (isDisputeResolved) {
        if (!hasSeenDisputeResult) {
            SummaryTransitionContainer {
                DisputeResultContent(
                    problem = problem,
                    otherUser = otherUser,
                    escrow = escrow,
                    isUserRole = isUserRole,
                    viewModel = viewModel,
                    onDismiss = {
                        viewModel.markDisputeResultSeen(problem.id, isUserRole)
                        safeNavigateBack()
                    }
                )
            }
            return
        } else {
            // Already seen dispute result, pop back to previous screen
            LaunchedEffect(Unit) {
                safeNavigateBack()
            }
            return
        }
    }

    // If the job is still in BROADCASTING stage
    if (status == "BROADCASTING") {
        if (!problem.solverCancelledNotice.isNullOrBlank()) {
            val relevantCancelledBid = allBids
                .filter { it.problemId == problem.id && it.status == "CANCELLED" }
                .maxByOrNull { it.createdAt }

            SummaryTransitionContainer {
                CancellationSummaryScreen(
                    problem = problem,
                    cancelledBid = relevantCancelledBid,
                    isUserRole = isUserRole,
                    onNavigateBack = {
                        viewingSolverCancellationSummary = false
                        safeNavigateBack()
                    },
                    onRebroadcast = {
                        viewModel.clearSolverCancelledNotice(problem.id)
                    },
                    onFullyCancel = {
                        safeNavigateBack()
                        viewModel.cancelInstantJob(problem.id)
                    }
                )
            }
            return
        } else if (isUserRole) {
            UserJobBroadcastingScreen(
                problem = problem,
                viewModel = viewModel,
                onNavigateBack = safeNavigateBack
            )
            return
        } else {
            LaunchedEffect(status) {
                safeNavigateBack()
            }
            return
        }
    }

    // PHASE L: JOB COMPLETION & CANCELLATION SUMMARY SCREEN (Shown after rating confirm or when job is completed/cancelled)
    if (showCompletionSummary || status == "JOB_COMPLETED" || status == "COMPLETED" || status == "CANCELLED" || problem.jobStatus == "CANCELLED") {
        SummaryTransitionContainer {
            JobCompletionSummaryScreen(
                problem = problem,
                otherUser = otherUser,
                isUserRole = isUserRole,
                allPlatformSettings = allPlatformSettings,
                viewModel = viewModel,
                isCancelled = (status == "CANCELLED" || problem.jobStatus == "CANCELLED"),
                onNavigateBack = {
                    viewModel.markCompletionResultSeen(problem.id, isUserRole)
                    safeNavigateBack()
                }
            )
        }
        return
    }

    val arrivalRadiusMeters = allPlatformSettings.find { it.key == "instant_job_arrival_radius_meters" }?.value?.toDoubleOrNull() ?: 500.0
    val currentDistanceMeters = distanceKm * 1000.0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SomadhanBg)
            .testTag("job_tracking_screen")
    ) {
        // 1. CENTER MAP VIEW
        JobTrackingMapView(
            userLat = userLat,
            userLng = userLng,
            solverLat = solverLat,
            solverLng = solverLng,
            status = status,
            isUserRole = isUserRole,
            modifier = Modifier.fillMaxSize()
        )

        // 2. TOP FLOATING BAR (Back Button + ETA Pill Card)
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding + 10.dp, start = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Circular Back Button
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clickable { safeNavigateBack() }
                    .testTag("job_tracking_back_button"),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "ফিরে যান",
                        tint = SomadhanTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ETA Pill Card
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    val (etaTitle, etaSubtitle) = when (status) {
                        "ACCEPTED", "ON_WAY", "ON_THE_WAY", "SOLVER_ACCEPTED", "SOLVER_EN_ROUTE" -> {
                            Pair(
                                "পৌঁছাতে আনুমানিক ${DistanceUtil.toBengaliDigits(etaMinutes)} মিনিট",
                                "দূরত্ব ${DistanceUtil.toBengaliDigits(String.format("%.1f", distanceKm))} কিমি • ${problem.userAddress.ifBlank { "বসিলা মেইন রোড, ঢাকা" }}"
                            )
                        }
                        "ARRIVED" -> {
                            val solverName = problem.acceptedSolverName ?: otherUser?.name ?: "সলভার"
                            Pair(
                                "$solverName লোকেশনে পৌঁছে গেছেন",
                                "এখন — ${problem.userAddress.ifBlank { "বসিলা মেইন রোড, ঢাকা" }}"
                            )
                        }
                        "WORK_IN_PROGRESS", "IN_PROGRESS" -> {
                            Pair(
                                "কাজ চলছে",
                                "সমস্যা সমাধান প্রক্রিয়াধীন রয়েছে"
                            )
                        }
                        "JOB_COMPLETED", "COMPLETED" -> {
                            Pair(
                                "কাজ সম্পন্ন হয়েছে",
                                "পরিশোধ ও হিসাব নিষ্পত্তি সম্পন্ন"
                            )
                        }
                        else -> {
                            Pair(
                                "জরুরি কাজ ট্র্যাকিং",
                                problem.userAddress.ifBlank { "ঢাকা" }
                            )
                        }
                    }
                    Text(
                        text = etaTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = SomadhanTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = etaSubtitle,
                        fontSize = 11.sp,
                        color = SomadhanTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Circular History Button
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clickable { onNavigate(Screen.InstantJobHistory.route) }
                    .testTag("job_tracking_history_button"),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "জরুরি হিস্ট্রি",
                        tint = SomadhanTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 3. RECENTER (🧭) FLOATING BUTTON
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp, bottom = 320.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clickable {
                        Toast.makeText(context, "ম্যাপ সেন্টারে রিসেট করা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                    .testTag("recenter_map_button"),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "রিসেন্টার ম্যাপ",
                        tint = SomadhanOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 4. BOTTOM SHEET ACTION PANEL
        JobTrackingBottomSheet(
            problem = problem,
            otherUser = otherUser,
            isUserRole = isUserRole,
            status = status,
            allPlatformSettings = allPlatformSettings,
            viewModel = viewModel,
            modifier = Modifier.align(Alignment.BottomCenter),
            arrivalRadiusMeters = arrivalRadiusMeters,
            currentDistanceMeters = currentDistanceMeters,
            unreadChatCount = unreadJobChatCount,
            onChatClick = {
                onNavigate(Screen.Chat.createRoute(problem.id))
            },
            onDisputeClick = {
                val isDisputeActive = problem.isDisputed && problem.disputeSettledAt == null
                if (isDisputeActive) {
                    Toast.makeText(context, "এই কাজে ইতিমধ্যে একটি বিরোধ (Dispute) চলমান রয়েছে। চ্যাটে আলোচনা করুন।", Toast.LENGTH_SHORT).show()
                    onNavigate(Screen.Chat.createRoute(problem.id))
                } else {
                    disputeReasonInput = ""
                    showDisputeDialog = true
                }
            },
            onSolverArrived = {
                if (!isSubmittingSolverArrived) {
                    if (currentDistanceMeters <= arrivalRadiusMeters || solverLat == 0.0 || userLat == 0.0) {
                        isSubmittingSolverArrived = true
                        viewModel.markSolverArrived(problem.id) {
                            isSubmittingSolverArrived = false
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "আপনি এখনো ক্লায়েন্টের লোকেশনের নির্দিষ্ট সীমার (${DistanceUtil.toBengaliDigits(arrivalRadiusMeters.toInt())} মি.) মধ্যে পৌঁছাননি (বর্তমান দূরত্ব: ${DistanceUtil.toBengaliDigits(String.format("%.0f", currentDistanceMeters))} মি.)।",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            onSolverStartJob = {
                if (!isSubmittingSolverStartJob) {
                    isSubmittingSolverStartJob = true
                    viewModel.markJobStarted(problem.id) {
                        isSubmittingSolverStartJob = false
                    }
                }
            },
            onSolverCancelJob = {
                showSolverCancelDialog = true
            },
            onSolverRequestExtraAmount = {
                extraAmountInput = ""
                extraAmountNoteInput = ""
                showExtraAmountDialog = true
            },
            onSolverRequestRelease = {
                // Bug fix (loading-lock): আগে এখানে সাথে সাথে একটা "সফল হয়েছে" toast দেখাত --
                // আসল রেজাল্ট আসার আগেই। এখন lock করা থাকে এবং toast শুধু আসল onSuccess-এ
                // ফায়ার হয় (VM-এর নিজস্ব auto-toast আছে, তাই এখানে আলাদা toast লাগবে না)।
                if (!isRequestingJobRelease) {
                    isRequestingJobRelease = true
                    jobReleaseError = null
                    viewModel.requestJobRelease(
                        problem = problem,
                        extraAmount = problem.confirmedExtraAmountTotal,
                        note = "",
                        onSuccess = {
                            isRequestingJobRelease = false
                        },
                        onError = { err ->
                            isRequestingJobRelease = false
                            jobReleaseError = err
                        }
                    )
                }
            },
            isRequestingRelease = isRequestingJobRelease,
            isSubmittingSolverArrived = isSubmittingSolverArrived,
            isSubmittingSolverStartJob = isSubmittingSolverStartJob,
            releaseError = jobReleaseError,
            onSolverCompleteJob = {
                showCompleteDialog = true
            },
            onUserConfirmExtraAmount = {
                showExtraAmountConfirmDialog = true
            },
            onUserRejectExtraAmount = {
                if (!isRejectingExtraAmountTop) {
                    isRejectingExtraAmountTop = true
                    rejectExtraAmountErrorTop = null
                    viewModel.userRejectExtraAmount(
                        problem,
                        onSuccess = { isRejectingExtraAmountTop = false },
                        onError = { err ->
                            isRejectingExtraAmountTop = false
                            rejectExtraAmountErrorTop = err
                        }
                    )
                }
            },
            isRejectingExtraAmount = isRejectingExtraAmountTop,
            rejectExtraAmountError = rejectExtraAmountErrorTop,
            onUserReleaseJob = {
                showCompleteDialog = true
            },
            onUserReviewDone = {
                showCompleteDialog = true
            },
            onReturnHome = {
                safeNavigateBack()
            }
        )
    }

    // SOLVER EXTRA AMOUNT REQUEST DIALOG
    if (showExtraAmountDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { if (!isSubmittingExtraAmount) showExtraAmountDialog = false },
            title = {
                Text(
                    text = "➕ অতিরিক্ত এমাউন্ট রিকোয়েস্ট",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "কাজের অতিরিক্ত কোনো খরচ বা নতুন কাজের জন্য ক্লায়েন্টের কাছে অতিরিক্ত অর্থ অনুমোদন চান। ক্লায়েন্ট অনুমোদন করলে তা সর্বমোট বিলে যুক্ত হবে।",
                        fontSize = 12.5.sp,
                        color = SomadhanTextSecondary,
                        lineHeight = 17.sp
                    )

                    OutlinedTextField(
                        value = extraAmountInput,
                        onValueChange = { extraAmountInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("অতিরিক্ত টাকার পরিমাণ (৳)", fontSize = 12.5.sp) },
                        placeholder = { Text("যেমন: ৫০০", fontSize = 12.5.sp, color = SomadhanTextHint) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("extra_amount_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )

                    OutlinedTextField(
                        value = extraAmountNoteInput,
                        onValueChange = { extraAmountNoteInput = it },
                        label = { Text("কারণ / বিবরণ (ঐচ্ছিক)", fontSize = 12.5.sp) },
                        placeholder = { Text("যেমন: অতিরিক্ত স্পেয়ার পার্টস কেনা হয়েছে", fontSize = 12.5.sp, color = SomadhanTextHint) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("extra_amount_note_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        ),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = extraAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) {
                            Toast.makeText(context, "সঠিক অতিরিক্ত টাকার পরিমাণ লিখুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmittingExtraAmount = true
                        viewModel.requestExtraAmount(
                            problem = problem,
                            amount = amt,
                            note = extraAmountNoteInput,
                            onSuccess = {
                                isSubmittingExtraAmount = false
                                showExtraAmountDialog = false
                                extraAmountInput = ""
                                extraAmountNoteInput = ""
                            },
                            onError = { err ->
                                isSubmittingExtraAmount = false
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    enabled = !isSubmittingExtraAmount,
                    modifier = Modifier.testTag("submit_extra_amount_button")
                ) {
                    if (isSubmittingExtraAmount) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("অনুরোধ পাঠানো হচ্ছে...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Text("অনুরোধ পাঠান ➕", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExtraAmountDialog = false },
                    enabled = !isSubmittingExtraAmount
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // USER CONFIRM EXTRA AMOUNT PAYMENT DIALOG (PHASE K.1)
    val pendingExtraAmt = problem.pendingExtraAmount ?: 0.0
    if (showExtraAmountConfirmDialog && pendingExtraAmt > 0.0) {
        // [ব্যালেন্স ফিক্স — ধাপ ১] এই স্ক্রিনের সব ব্যালেন্স-চেক owner/USER-role bid/extra-charge
        // ফ্লো-র অংশ (এই ব্যক্তি এই মুহূর্তে সবসময় USER role-এ), তাই role-scoped balanceUser
        // পড়া হচ্ছে, শেয়ার্ড `balance` না (MONEY_FLOW_AND_ADMIN_BUGS_MASTER_PROMPT.md, বাগ A)।
        val userBalance = currentUser?.balanceUser ?: 0.0
        ExtraAmountPaymentConfirmationDialog(
            extraAmount = pendingExtraAmt,
            note = problem.pendingExtraAmountNote,
            walletBalance = userBalance,
            isLoading = isConfirmingExtraAmountPaid,
            errorMessage = confirmExtraAmountError,
            onProceed = {
                if (userBalance >= pendingExtraAmt) {
                    // Bug fix (loading-lock): এই dialog আগে showExtraAmountConfirmDialog = false
                    // করে সাথে সাথে বন্ধ হয়ে যেত, userConfirmExtraAmountPaid() শুরু হওয়ার আগেই --
                    // এখন callback (success/error) না আসা পর্যন্ত dialog খোলাই থাকবে (isLoading
                    // দিয়ে লক অবস্থায়), যাতে slow network-এ re-tap ডাবল-কল ফায়ার না করতে পারে।
                    // Bug fix: error হলে dialog আগে বন্ধ হয়ে যেত (showExtraAmountConfirmDialog =
                    // false ছিল onError-এও) -- এখন শুধু success-এ বন্ধ হয়, error হলে dialog খোলা
                    // থেকে inline error দেখায়।
                    isConfirmingExtraAmountPaid = true
                    confirmExtraAmountError = null
                    viewModel.userConfirmExtraAmountPaid(
                        problem = problem,
                        walletDeducted = pendingExtraAmt,
                        onSuccess = {
                            isConfirmingExtraAmountPaid = false
                            showExtraAmountConfirmDialog = false
                            confirmExtraAmountError = null
                            Toast.makeText(context, "অতিরিক্ত বিল সফলভাবে ওয়ালেট থেকে কনফার্ম করা হয়েছে! ✅", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            isConfirmingExtraAmountPaid = false
                            confirmExtraAmountError = err
                        }
                    )
                } else {
                    // কোনো network call এখানে নেই (শুধু dialog সুইচ), তাই সাথে সাথে বন্ধ করা নিরাপদ
                    showExtraAmountConfirmDialog = false
                    confirmExtraAmountError = null
                    extraAmountForGatewayPayment = (pendingExtraAmt - userBalance).coerceAtLeast(0.0)
                }
            },
            onDismiss = {
                if (!isConfirmingExtraAmountPaid) {
                    showExtraAmountConfirmDialog = false
                    confirmExtraAmountError = null
                }
            }
        )
    }

    // MERCHANT PAYMENT GATEWAY DIALOG FOR EXTRA AMOUNT (PHASE K.1)
    if (extraAmountForGatewayPayment != null) {
        val payableAmount = extraAmountForGatewayPayment!!
        val userBalance = currentUser?.balanceUser ?: 0.0
        val walletDeduction = if (userBalance > 0) userBalance.coerceAtMost(pendingExtraAmt) else 0.0
        MerchantPaymentDialog(
            amount = payableAmount,
            problemTitle = "অতিরিক্ত বিল: ${problem.title.ifBlank { "জরুরি সমাধান" }}",
            solverName = problem.acceptedSolverName ?: otherUser?.name ?: "দক্ষ সমাধানকারী",
            onPaymentSuccess = {
                extraAmountForGatewayPayment = null
                extraAmountGatewayPending = true
                extraAmountGatewayError = null
                viewModel.userConfirmExtraAmountPaid(
                    problem = problem,
                    walletDeducted = walletDeduction,
                    onSuccess = {
                        extraAmountGatewayPending = false
                        Toast.makeText(context, "পেমেন্ট সম্পন্ন ও অতিরিক্ত বিল কনফার্ম করা হয়েছে! ✅", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        extraAmountGatewayError = err
                    }
                )
            },
            onDismissRequest = {
                extraAmountForGatewayPayment = null
            },
            onPaymentCompleteWithDetails = { gateway, trxId, phone ->
                viewModel.recordGatewayPayment(
                    gateway = gateway,
                    gatewayTrxId = trxId,
                    amount = payableAmount,
                    senderPhone = phone,
                    purpose = "ADDITIONAL_CHARGE",
                    problemId = problem.id,
                    problemTitle = problem.title,
                    note = "Problem #${problem.id.take(8)}"
                )
            }
        )
    }

    // Post-gateway-payment verification overlay for the extra-amount confirm flow
    // (section 3 pattern) -- MerchantPaymentDialog already closed by the time this shows.
    if (extraAmountGatewayPending) {
        val userBalanceForRetry = currentUser?.balanceUser ?: 0.0
        val walletDeductionForRetry = if (userBalanceForRetry > 0) userBalanceForRetry.coerceAtMost(pendingExtraAmt) else 0.0
        BottomSlideAlertDialog(
            onDismissRequest = { /* locked until success or explicit dismiss-on-error below */ },
            title = { Text("লেনদেন যাচাই হচ্ছে", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary) },
            text = {
                Column {
                    if (extraAmountGatewayError == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = SomadhanOrange, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("লেনদেন যাচাই হচ্ছে...", fontSize = 13.sp, color = SomadhanTextPrimary)
                        }
                    } else {
                        Text(
                            text = extraAmountGatewayError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError
                        )
                    }
                }
            },
            confirmButton = {
                if (extraAmountGatewayError != null) {
                    Button(onClick = {
                        extraAmountGatewayError = null
                        viewModel.userConfirmExtraAmountPaid(
                            problem = problem,
                            walletDeducted = walletDeductionForRetry,
                            onSuccess = {
                                extraAmountGatewayPending = false
                                Toast.makeText(context, "পেমেন্ট সম্পন্ন ও অতিরিক্ত বিল কনফার্ম করা হয়েছে! ✅", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err -> extraAmountGatewayError = err }
                        )
                    }) { Text("আবার চেষ্টা করুন") }
                }
            },
            dismissButton = {
                if (extraAmountGatewayError != null) {
                    TextButton(onClick = {
                        extraAmountGatewayPending = false
                        extraAmountGatewayError = null
                    }) { Text("বাতিল", color = SomadhanTextSecondary) }
                }
            }
        )
    }

    // USER RAISE DISPUTE DIALOG
    if (showDisputeDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = {
                if (!isSubmittingDispute) {
                    showDisputeDialog = false
                    disputeReasonInput = ""
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "বিরোধ (Dispute) উত্থাপন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = SomadhanTextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "কাজে কোনো অসঙ্গতি থাকলে আপনি বিরোধ উত্থাপন করতে পারেন। এর মাধ্যমে সমস্যাটির ওপর একটি বিরোধ (Dispute) প্রক্রিয়া শুরু হবে এবং সমাধানকারীর সাথে বিরোধ নিয়ে চ্যাট শুরু হবে। প্রয়োজনে চ্যাটে অ্যাডমিনও সহায়তা করতে পারবেন।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = disputeReasonInput,
                        onValueChange = { disputeReasonInput = it },
                        label = { Text("বিরোধের কারণ ও বিস্তারিত বিবরণ *") },
                        placeholder = { Text("যেমন: কাজটি সম্পূর্ণ হয়নি বা চুক্তিবহির্ভূত কাজ করা হয়েছে...") },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dispute_reason_input")
                    )
                    if (disputeError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = disputeError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (disputeReasonInput.isNotBlank()) {
                            isSubmittingDispute = true
                            disputeError = null
                            viewModel.raiseDispute(
                                problem = problem,
                                reason = disputeReasonInput.trim(),
                                onSuccess = {
                                    isSubmittingDispute = false
                                    showDisputeDialog = false
                                    disputeReasonInput = ""
                                    disputeError = null
                                    onNavigate(Screen.Chat.createRoute(problem.id))
                                },
                                onError = { err ->
                                    isSubmittingDispute = false
                                    disputeError = err
                                }
                            )
                        }
                    },
                    enabled = !isSubmittingDispute && disputeReasonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    modifier = Modifier.testTag("submit_dispute_button")
                ) {
                    if (isSubmittingDispute) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পাঠানো হচ্ছে...")
                    } else {
                        Text("বিরোধ চালু ও চ্যাটে যান")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDisputeDialog = false
                        disputeReasonInput = ""
                        disputeError = null
                    },
                    enabled = !isSubmittingDispute
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // CANCEL CONFIRMATION DIALOG (FOR SOLVER)
    if (showSolverCancelDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { if (!isSubmittingSolverJobCancel) showSolverCancelDialog = false },
            title = {
                Text(
                    text = "কাজটি বাতিল করতে চান?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "গৃহীত কাজ বাতিল করলে গ্রাহকের ওয়ালেটে অর্থ রিফান্ড হবে এবং পোস্টটি পুনরায় ওপেন হবে। বাতিলের কারণ উল্লেখ করুন:",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )
                    OutlinedTextField(
                        value = solverCancelReason,
                        onValueChange = { solverCancelReason = it },
                        placeholder = { Text("বাতিলের কারণ (যেমন: যানজট বা জরুরি সমস্যা)", fontSize = 12.5.sp, color = SomadhanTextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        ),
                        maxLines = 2,
                        enabled = !isSubmittingSolverJobCancel
                    )
                    if (solverJobCancelError != null) {
                        Text(
                            text = solverJobCancelError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isSubmittingSolverJobCancel) {
                            isSubmittingSolverJobCancel = true
                            solverJobCancelError = null
                            // Bug fix (cancel-summary page appearing twice -- case 3): this used
                            // to set showSolverCancelDialog=false and
                            // viewingSolverCancellationSummary=true immediately on click, BEFORE
                            // the cancel had actually gone through. The LaunchedEffect above
                            // (keyed on problem.solverCancelledNotice/solverCancelledBid) already
                            // flips viewingSolverCancellationSummary to true on its own once the
                            // real, confirmed cancellation data arrives -- so the optimistic set
                            // here just fired the same transition a second time a moment later,
                            // which is what made the summary page visibly appear twice. Now the
                            // dialog stays open (with a spinner) until the ViewModel callback
                            // confirms the cancel actually happened, and only then closes it;
                            // showing the summary itself is left entirely to that existing
                            // LaunchedEffect.
                            viewModel.solverCancelAcceptedJob(
                                problem = problem,
                                reason = solverCancelReason.ifBlank { "সমাধানকারী কর্তৃক জরুরি বাতিল" },
                                onSuccess = {
                                    isSubmittingSolverJobCancel = false
                                    showSolverCancelDialog = false
                                    solverJobCancelError = null
                                },
                                onError = { err ->
                                    isSubmittingSolverJobCancel = false
                                    solverJobCancelError = err
                                }
                            )
                        }
                    },
                    enabled = !isSubmittingSolverJobCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError)
                ) {
                    if (isSubmittingSolverJobCancel) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("বাতিল হচ্ছে...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Text("কাজ বাতিল করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSolverCancelDialog = false
                        solverJobCancelError = null
                    },
                    enabled = !isSubmittingSolverJobCancel
                ) {
                    Text("না", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // COMPLETION & RATING DIALOG
    if (showCompleteDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { if (!isSubmittingRating) showCompleteDialog = false },
            title = {
                Text(
                    text = if (isUserRole) "কাজের রেটিং ও রিভিউ দিন" else "কাজ সম্পন্ন নিশ্চিতকরণ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isUserRole)
                            "সলভারের সেবার মান কেমন ছিল? আপনার মতামত দিন:"
                        else
                            "কাজটি কি সম্পূর্ণ শেষ হয়েছে? নিশ্চিত করলে পেমেন্ট ও রেপুটেশন নিষ্পন্ন হবে।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary,
                        textAlign = TextAlign.Center
                    )

                    // 5-Star Rating Selector
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        (1..5).forEach { star ->
                            IconButton(
                                onClick = { ratingStars = star },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (star <= ratingStars) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = "$star তারা",
                                    tint = SomadhanOrange,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    // Optional Review Comment Field
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        placeholder = { Text("মতামত লিখুন (ঐচ্ছিক)", fontSize = 12.5.sp, color = SomadhanTextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        ),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingRating = true
                        viewModel.completeInstantJob(
                            problem = problem,
                            stars = ratingStars,
                            reviewComment = reviewComment.ifBlank { "চমৎকার ও দ্রুত জরুরি সমাধান।" }
                        ) {
                            isSubmittingRating = false
                            showCompleteDialog = false
                            showCompletionSummary = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                    enabled = !isSubmittingRating,
                    modifier = Modifier.testTag("confirm_completion_button")
                ) {
                    if (isSubmittingRating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("নিশ্চিত করুন ✅", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCompleteDialog = false },
                    enabled = !isSubmittingRating
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }
}

/**
 * JobTrackingMapView: Renders GoogleMap with fallback Custom Map Graphics.
 */
@Composable
fun JobTrackingMapView(
    userLat: Double,
    userLng: Double,
    solverLat: Double,
    solverLng: Double,
    status: String,
    isUserRole: Boolean,
    modifier: Modifier = Modifier
) {
    val userLatLng = remember(userLat, userLng) { LatLng(userLat, userLng) }
    val solverLatLng = remember(solverLat, solverLng) { LatLng(solverLat, solverLng) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng((userLat + solverLat) / 2.0, (userLng + solverLng) / 2.0),
            14.5f
        )
    }

    LaunchedEffect(userLatLng, solverLatLng) {
        try {
            val bounds = LatLngBounds.builder()
                .include(userLatLng)
                .include(solverLatLng)
                .build()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        } catch (_: Exception) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng((userLat + solverLat) / 2.0, (userLng + solverLng) / 2.0),
                14.5f
            )
        }
    }

    val isMapsApiKeyConfigured = remember {
        val key = com.example.BuildConfig.MAPS_API_KEY
        key.isNotBlank() &&
            !key.equals("YOUR_MAPS_API_KEY", ignoreCase = true) &&
            !key.startsWith("YOUR_", ignoreCase = true) &&
            !key.startsWith("AIzaSyDummy", ignoreCase = true) &&
            !key.startsWith("DEMO_", ignoreCase = true) &&
            key.startsWith("AIza")
    }

    Box(modifier = modifier) {
        // Fallback Canvas Street Grid & Interactive Map Canvas
        JobTrackingCanvasFallback(
            status = status,
            modifier = Modifier.fillMaxSize()
        )

        // Google Map Composable Layer (Rendered only when valid Maps API key is configured)
        if (isMapsApiKeyConfigured) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = false,
                    mapToolbarEnabled = false
                )
            ) {
                // User Pin (🏠)
                Marker(
                    state = MarkerState(position = userLatLng),
                    title = "ইউজারের লোকেশন (গন্তব্য)",
                    snippet = "কাজের অবস্থান"
                )

                // Solver Pin (🔧)
                Marker(
                    state = MarkerState(position = solverLatLng),
                    title = "সলভারের লাইভ অবস্থান",
                    snippet = if (status == "ARRIVED" || status == "WORK_IN_PROGRESS" || status == "JOB_COMPLETED")
                        "পৌঁছে গেছেন" else "আসছেন"
                )

                // Polyline between them
                if (status != "WORK_IN_PROGRESS" && status != "JOB_COMPLETED") {
                    Polyline(
                        points = listOf(solverLatLng, userLatLng),
                        color = SomadhanOrange,
                        width = 8f
                    )
                }
            }
        }
    }
}

/**
 * JobTrackingCanvasFallback: Beautiful illustrated street grid matching the mockup design.
 */
@Composable
fun JobTrackingCanvasFallback(
    status: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background subtle map hue
        drawRect(color = Color(0xFFE7EBE5))

        // Grid lines
        val gridSpacing = 70f
        var x = 0f
        while (x < w) {
            drawLine(
                color = Color(0xFFDFE4DD),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 2f
            )
            x += gridSpacing
        }

        var y = 0f
        while (y < h) {
            drawLine(
                color = Color(0xFFDFE4DD),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 2f
            )
            y += gridSpacing
        }

        // Main arterial road
        drawLine(
            color = Color(0xFFFFFFFF),
            start = Offset(w * 0.2f, h * 0.1f),
            end = Offset(w * 0.7f, h * 0.85f),
            strokeWidth = 24f,
            cap = StrokeCap.Round
        )

        // Route Line from Solver (top right) to User (bottom left)
        val userX = w * 0.30f
        val userY = h * 0.42f
        val solverX = if (status == "ARRIVED" || status == "WORK_IN_PROGRESS" || status == "JOB_COMPLETED")
            userX + 20f else w * 0.65f
        val solverY = if (status == "ARRIVED" || status == "WORK_IN_PROGRESS" || status == "JOB_COMPLETED")
            userY - 15f else h * 0.25f

        // Pulsing Circle at User Pin
        drawCircle(
            color = SomadhanOrange.copy(alpha = 0.12f),
            radius = pulseRadius * 1.5f,
            center = Offset(userX, userY)
        )
        drawCircle(
            color = SomadhanOrange.copy(alpha = 0.35f),
            radius = pulseRadius * 1.5f,
            center = Offset(userX, userY),
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
        )

        // Route Polyline
        if (status != "WORK_IN_PROGRESS" && status != "JOB_COMPLETED") {
            drawLine(
                color = SomadhanOrange,
                start = Offset(solverX, solverY),
                end = Offset(userX, userY),
                strokeWidth = 6f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 12f))
            )
        }
    }
}

/**
 * JobTrackingBottomSheet: Exactly matches the mockup specification.
 */
@Composable
fun JobTrackingBottomSheet(
    problem: ProblemEntity,
    otherUser: UserEntity?,
    isUserRole: Boolean,
    status: String,
    allPlatformSettings: List<PlatformSettingEntity> = emptyList(),
    viewModel: SomadhanViewModel? = null,
    modifier: Modifier = Modifier,
    arrivalRadiusMeters: Double = 500.0,
    currentDistanceMeters: Double = 0.0,
    unreadChatCount: Int = 0,
    onChatClick: () -> Unit,
    onDisputeClick: () -> Unit,
    onSolverArrived: () -> Unit,
    onSolverStartJob: () -> Unit,
    isSubmittingSolverArrived: Boolean = false,
    isSubmittingSolverStartJob: Boolean = false,
    onSolverCancelJob: () -> Unit,
    onSolverRequestExtraAmount: () -> Unit,
    onSolverRequestRelease: () -> Unit,
    isRequestingRelease: Boolean = false,
    releaseError: String? = null,
    onSolverCompleteJob: () -> Unit,
    onUserConfirmExtraAmount: () -> Unit,
    onUserRejectExtraAmount: () -> Unit,
    isRejectingExtraAmount: Boolean = false,
    rejectExtraAmountError: String? = null,
    onUserReleaseJob: () -> Unit,
    onUserReviewDone: () -> Unit,
    onReturnHome: () -> Unit
) {
    val context = LocalContext.current
    val (badgeBg, badgeTextColor, badgeText) = when (status) {
        "ACCEPTED", "ON_WAY", "ON_THE_WAY", "SOLVER_ACCEPTED", "SOLVER_EN_ROUTE" ->
            Triple(SomadhanInfoLight, SomadhanInfo, "১. আসছেন")
        "ARRIVED" ->
            Triple(SomadhanOrangeContainer, SomadhanOrangePressed, "২. পৌঁছে গেছেন")
        "WORK_IN_PROGRESS", "IN_PROGRESS" ->
            Triple(SomadhanInfoLight, SomadhanInfo, "৩. কাজ চলছে")
        "JOB_COMPLETED", "COMPLETED" ->
            Triple(SomadhanSuccessLight, SomadhanSuccess, "৪. সম্পন্ন")
        else ->
            Triple(SomadhanInfoLight, SomadhanInfo, "চলমান")
    }

    val otherPartyName = if (isUserRole) {
        problem.acceptedSolverName ?: otherUser?.name ?: "দক্ষ সমাধানকারী"
    } else {
        problem.userName ?: otherUser?.name ?: "সম্মানিত গ্রাহক"
    }

    val otherPartyShortStatus = when (status) {
        "ACCEPTED", "ON_WAY", "ON_THE_WAY", "SOLVER_ACCEPTED", "SOLVER_EN_ROUTE" ->
            if (currentDistanceMeters > 0) "দূরত্ব: ${DistanceUtil.toBengaliDigits(String.format("%.1f", currentDistanceMeters / 1000.0))} কিমি" else "লোকেশন অভিমুখে রওয়ানা"
        "ARRIVED" -> "লোকেশন: ${problem.userAddress.ifBlank { "গন্তব্যে পৌঁছেছেন" }}"
        "WORK_IN_PROGRESS", "IN_PROGRESS" -> "কাজ চলমান রয়েছে"
        "JOB_COMPLETED", "COMPLETED" -> "কাজ সম্পন্ন হয়েছে"
        else -> problem.userAddress.ifBlank { "ট্র্যাকিং সক্রিয়" }
    }

    DraggableMapSheet(
        modifier = modifier,
        initialState = MapSheetStateValue.DEFAULT,
        collapsedBar = { sheetState, onToggleExpand ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = badgeBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(badgeTextColor)
                            )
                            Text(
                                text = badgeText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = otherPartyName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SomadhanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = otherPartyShortStatus,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = SomadhanTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quick Chat Button
                    if (status != "JOB_COMPLETED" && status != "COMPLETED") {
                        Surface(
                            modifier = Modifier
                                .size(38.dp)
                                .clickable { onChatClick() }
                                .testTag("sticky_chat_quick_button"),
                            shape = CircleShape,
                            color = SomadhanOrangeLight
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                BadgedBox(
                                    badge = {
                                        if (unreadChatCount > 0) {
                                            Badge(
                                                containerColor = SomadhanOrange,
                                                contentColor = Color.White
                                            ) {
                                                Text(
                                                    text = if (unreadChatCount > 99) "99+" else DistanceUtil.toBengaliDigits(unreadChatCount.toString()),
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = "চ্যাট",
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { onToggleExpand() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = when (sheetState) {
                                MapSheetStateValue.COLLAPSED -> Icons.Default.KeyboardArrowUp
                                MapSheetStateValue.DEFAULT -> Icons.Default.UnfoldMore
                                MapSheetStateValue.EXPANDED -> Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = "টগল করুন",
                            tint = SomadhanTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        expandedContent = { sheetState ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                // 1. Status Badge & Dispute Link — Loading Pattern ধাপ খ (JobTrackingScreen zone ২/৬):
                // status/isDisputed/disputeSettledAt বদলালে (স্টেজ পরিবর্তন বা ডিসপিউট শুরু/মীমাংসা)
                // শুধু এই badge+dispute-বাটন zone-টা হালকা pulse করবে — নিচের stepper/other-party/
                // money-card/action-CTA zone সম্পূর্ণ স্বাধীন, এখানে ছোঁয়া হয়নি। zone ১/৬ (bid list,
                // UserJobBroadcastingScreen)-এর মতোই rememberFieldChangePulse + sessionKey বাইরের
                // JobTrackingScreen-এর "job_tracking_${problem.id}" cold-load gate-এর সাথে মিলিয়ে
                // দেওয়া হয়েছে (আগে থেকেই markLoadedOnce), তাই কোনো নতুন/আলাদা cold-skeleton flash হবে না।
                PulsingValue(
                    isUpdating = rememberFieldChangePulse(
                        value = Triple(status, problem.isDisputed, problem.disputeSettledAt),
                        sessionKey = "job_tracking_${problem.id}",
                        viewModel = viewModel
                    )
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = badgeBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(badgeTextColor)
                            )
                            Text(
                                text = badgeText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor
                            )
                        }
                    }

                    // Post ID Chip (1-click copyable)
                    val cleanPostId = problem.id
                    val context = LocalContext.current
                    val clipboardManager = LocalClipboardManager.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SomadhanBg)
                            .border(0.6.dp, SomadhanBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(cleanPostId))
                                Toast.makeText(context, "পোস্ট ID কপি করা হয়েছে: $cleanPostId", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .testTag("tracking_sheet_job_id_${problem.id}")
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

                // Dispute button in top right of bottom sheet - navigates to Chat where dispute flow exists
                if (status != "JOB_COMPLETED" && status != "COMPLETED") {
                    val isDisputeActive = problem.isDisputed && problem.disputeSettledAt == null
                    TextButton(
                        onClick = onDisputeClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("dispute_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isDisputeActive) SomadhanOrange else SomadhanError,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isDisputeActive) "ডিসপিউট চলছে" else "ডিসপিউট",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDisputeActive) SomadhanOrange else SomadhanError
                        )
                    }
                }
            }
            } // PulsingValue content lambda (ধাপ খ, স্ট্যাটাস-ব্যাজ zone ২/৬) শেষ

            Spacer(modifier = Modifier.height(14.dp))

            // 2. 4-Step Horizontal Stepper Timeline (Onway -> Arrived -> In Progress -> Done)
            val currentStepIdx = when (status) {
                "ACCEPTED", "ON_WAY", "ON_THE_WAY", "SOLVER_ACCEPTED", "SOLVER_EN_ROUTE" -> 1
                "ARRIVED" -> 2
                "WORK_IN_PROGRESS", "IN_PROGRESS" -> 3
                "JOB_COMPLETED", "COMPLETED" -> 4
                else -> 1
            }
            // Loading Pattern ধাপ খ (JobTrackingScreen zone ৩/৬): currentStepIdx/isDisputed/
            // disputeSettledAt বদলালে শুধু stepper-টা pulse করবে, সেশনকি বাকি zone-গুলোর মতোই
            // বাইরের cold-load gate-এর সাথে মেলানো — নতুন flash নেই।
            PulsingValue(
                isUpdating = rememberFieldChangePulse(
                    value = Triple(currentStepIdx, problem.isDisputed, problem.disputeSettledAt),
                    sessionKey = "job_tracking_${problem.id}",
                    viewModel = viewModel
                )
            ) {
            TrackingProgressStepper(
                currentStepIdx = currentStepIdx,
                isDisputed = problem.isDisputed,
                disputeSettledAt = problem.disputeSettledAt,
                problem = problem
            )
            } // PulsingValue content lambda (ধাপ খ, stepper zone ৩/৬) শেষ

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Other Party Profile Row (Chat Button Only)
            val displayName = if (isUserRole) {
                problem.acceptedSolverName ?: otherUser?.name ?: "দক্ষ সমাধানকারী"
            } else {
                problem.userName.ifBlank { otherUser?.name ?: "সম্মানিত সেবাগ্রহীতা" }
            }

            val metaText = if (isUserRole) {
                "${problem.categoryName.ifBlank { "বিশেষজ্ঞ" }} • ${otherUser?.reputationScore?.let { String.format("%.1f", it) } ?: "৫.০"} ⭐ রেটিং"
            } else {
                problem.userAddress.ifBlank { "বসিলা মেইন রোড, মোহাম্মদপুর, ঢাকা" }
            }

            val initialChar = displayName.trim().take(1).ifBlank { "স" }

            // Loading Pattern ধাপ খ (JobTrackingScreen zone ৪/৬): displayName/metaText
            // (otherUser-এর নাম/রেটিং বদলালে) বা unreadChatCount বদলালে শুধু এই profile-row zone-টা
            // pulse করবে; sessionKey আগের zone-গুলোর মতোই বাইরের cold-load gate-এর সাথে মেলানো।
            PulsingValue(
                isUpdating = rememberFieldChangePulse(
                    value = Triple(displayName, metaText, unreadChatCount),
                    sessionKey = "job_tracking_${problem.id}",
                    viewModel = viewModel
                )
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SomadhanOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialChar,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SomadhanOrangePressed
                    )
                }

                // Name & Meta
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metaText,
                        fontSize = 11.5.sp,
                        color = SomadhanTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Chat Action Button (💬) -> navigates to Chat screen
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.dp, SomadhanBorder, CircleShape)
                        .clickable { onChatClick() }
                        .testTag("chat_action_button"),
                    color = SomadhanSurfaceVariant,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BadgedBox(
                            badge = {
                                if (unreadChatCount > 0) {
                                    Badge(
                                        containerColor = SomadhanOrange,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (unreadChatCount > 99) "99+" else DistanceUtil.toBengaliDigits(unreadChatCount.toString()),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "মেসেজ পাঠান",
                                tint = SomadhanInfo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            } // PulsingValue content lambda (ধাপ খ, other-party-row zone ৪/৬) শেষ

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Problem Brief Card
            val baseAmount = (problem.acceptedAmount ?: problem.maxBudget ?: problem.minBudget ?: 0.0).toInt()
            val confirmedExtra = problem.confirmedExtraAmountTotal.toInt()
            val totalGross = baseAmount + confirmedExtra
            val pendingExtra = problem.pendingExtraAmount ?: 0.0
            val hasPendingExtra = pendingExtra > 0.0

            var commissionBreakdown by remember(problem.id, baseAmount, confirmedExtra, problem.appliedCommissionRate) {
                mutableStateOf<SomadhanRepository.CommissionBreakdown?>(null)
            }

            LaunchedEffect(problem.id, baseAmount, confirmedExtra, problem.appliedCommissionRate) {
                val sId = problem.acceptedSolverId ?: ""
                if (viewModel != null) {
                    commissionBreakdown = viewModel.previewCommissionBreakdown(
                        problem = problem,
                        solverId = sId,
                        baseAmount = baseAmount.toDouble(),
                        extraAmount = confirmedExtra.toDouble()
                    )
                }
            }

            val defaultCommissionRate = allPlatformSettings.find { it.key == "commission_percent" }?.value?.toDoubleOrNull() ?: 10.0
            val solverCommissionRate = problem.appliedCommissionRate ?: defaultCommissionRate
            val fallbackPlatformFee = (totalGross * solverCommissionRate) / 100.0
            val fallbackNetSolverAmount = (totalGross - fallbackPlatformFee).coerceAtLeast(0.0)

            val netSolverAmount = commissionBreakdown?.netAmount ?: fallbackNetSolverAmount
            val baseCommission = commissionBreakdown?.baseCommission ?: ((baseAmount * solverCommissionRate) / 100.0)
            val extraCommission = commissionBreakdown?.extraCommission ?: ((confirmedExtra * solverCommissionRate) / 100.0)
            val totalCommission = commissionBreakdown?.totalCommission ?: fallbackPlatformFee
            val wasFreeQuota = commissionBreakdown?.wasFreeQuotaJob ?: (solverCommissionRate == 0.0)

            // Loading Pattern ধাপ খ (JobTrackingScreen zone ৫/৬): টাকার অঙ্ক (totalGross/
            // netSolverAmount/confirmedExtra/pendingExtra/wasFreeQuota) বদলালে শুধু এই money-card +
            // pending-extra-নোটিশ zone pulse করবে — commissionBreakdown fetch করা LaunchedEffect
            // ইচ্ছাকৃতভাবে এই wrapper-এর বাইরে রাখা হয়েছে (সেটা business-logic side-effect, visual না)।
            // sessionKey আগের zone-গুলোর মতোই বাইরের cold-load gate-এর সাথে মেলানো।
            PulsingValue(
                isUpdating = rememberFieldChangePulse(
                    value = listOf(totalGross, netSolverAmount, confirmedExtra, pendingExtra, wasFreeQuota),
                    sessionKey = "job_tracking_${problem.id}",
                    viewModel = viewModel
                )
            ) {
            Column {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SomadhanCardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = problem.title.ifBlank { "জরুরি সমস্যা সমাধান" },
                            fontSize = 12.5.sp,
                            color = SomadhanTextPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (!isUserRole) {
                            // Solver View: Show Net Earnings as Primary Figure (Phase M.3 & N.2)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "৳ ${DistanceUtil.toBengaliDigits(netSolverAmount.toInt())}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SomadhanSuccess
                                )
                                Text(
                                    text = if (confirmedExtra > 0) {
                                        if (wasFreeQuota) {
                                            "ফ্রি কোটা মূল বিড + অতিরিক্তের কমিশন ৳${DistanceUtil.toBengaliDigits(extraCommission.toInt())} (ছাড়সহ)"
                                        } else {
                                            "মূল বিডের কমিশন ৳${DistanceUtil.toBengaliDigits(baseCommission.toInt())} + অতিরিক্তের কমিশন ৳${DistanceUtil.toBengaliDigits(extraCommission.toInt())} (ছাড়সহ)"
                                        }
                                    } else if (wasFreeQuota) {
                                        "ফ্রি কোটা (০% কমিশন)"
                                    } else {
                                        "কমিশন ৳${DistanceUtil.toBengaliDigits(baseCommission.toInt())} বাদে"
                                    },
                                    fontSize = 10.5.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        } else {
                            // User View: Total gross amount
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(totalGross)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = SomadhanOrangePressed
                            )
                        }
                    }

                    if (!isUserRole) {
                        if (confirmedExtra > 0) {
                            Text(
                                text = "মূল চুক্তি: ৳${DistanceUtil.toBengaliDigits(baseAmount)} + অতিরিক্ত: ৳${DistanceUtil.toBengaliDigits(confirmedExtra)} (মোট বিল ৳${DistanceUtil.toBengaliDigits(totalGross)})",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        } else {
                            Text(
                                text = "গ্রাহক পরিশোধিত মোট বিল: ৳${DistanceUtil.toBengaliDigits(totalGross)}",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    } else if (confirmedExtra > 0) {
                        Text(
                            text = "মূল চুক্তি: ৳${DistanceUtil.toBengaliDigits(baseAmount)} + অনুমোদিত অতিরিক্ত: ৳${DistanceUtil.toBengaliDigits(confirmedExtra)}",
                            fontSize = 11.sp,
                            color = SomadhanTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "সুরক্ষিত এসক্রো ID: #ESC_${problem.id.takeLast(8).uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD97706)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "এসক্রো সুরক্ষিত",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }

            // Notice for Solver when extra amount is waiting for client approval
            if (!isUserRole && hasPendingExtra) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SomadhanOrangeContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanOrangeLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "অতিরিক্ত বিল অনুরোধ পাঠানো হয়েছে: ৳${DistanceUtil.toBengaliDigits(pendingExtra.toInt())} (ক্লায়েন্ট অনুমোদনের অপেক্ষায়)",
                            fontSize = 11.5.sp,
                            color = SomadhanOrangePressed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            } // Column (ধাপ খ, money-card zone ৫/৬ wrapper) শেষ
            } // PulsingValue content lambda (ধাপ খ, money-card zone ৫/৬) শেষ

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Role-Specific Action CTA Buttons — Loading Pattern ধাপ খ (JobTrackingScreen zone
            // ৬/৬, crossfade upgrade কনফার্মড ব্যাচ ২৮, বাস্তবায়ন এই ব্যাচে)। এই zone অন্য ৫টার
            // মতো "একই structure-এ শুধু ভ্যালু বদলায়" না — role × status × sub-state কম্বিনেশনে
            // সম্পূর্ণ আলাদা বাটন-সেট/কার্ড দেখায় (leaf-value pulse যথেষ্ট না, আর PulsingValue-এর
            // opacity-pulse-only আচরণে পুরনো থেকে নতুন বাটন-সেটে সাথে সাথেই swap হয়ে যেত, কোনো
            // true crossfade ছাড়া) — তাই এই একটা zone-এর জন্য PulsingValue-এর বদলে AnimatedContent
            // ব্যবহার হচ্ছে, যাতে old→new সত্যিকারের fade crossfade হয় (height-ভিন্ন branch-গুলোর
            // জন্য default SizeTransform-ও smooth animate করে)। ctaHasReleaseReq/ctaIsDisputeActive
            // এখানে আলাদাভাবে গণনা করা হলো শুধু এই targetState key-এর জন্য — নিচের if/when ব্লকের
            // ভেতরের local val hasReleaseReq/isDisputeActive (solver/user শাখায় আলাদা আলাদাভাবে,
            // আগে থেকেই বিদ্যমান) ইচ্ছাকৃতভাবে অপরিবর্তিত রাখা হলো (Ground Rule ১ — বিদ্যমান লজিক
            // স্পর্শ না করে শুধু visual crossfade যোগ করা)। isSubmittingSolverArrived/
            // isSubmittingSolverStartJob/isRequestingRelease/isRejectingExtraAmount-এর মতো
            // submit-লোডিং ফ্ল্যাগ ইচ্ছাকৃতভাবে key-তে নেই — ওগুলো নিজেরাই spinner দেখায়, ওগুলো
            // দিলে ডাবল-ফিডব্যাক হতো। Compose gotcha এড়াতে (exit-হওয়া content বাইরের live var পড়ে
            // ফেললে crossfade ভেঙে যায়) নিচে status/hasPendingExtra-কে content lambda-র প্যারামিটার
            // থেকে shadow করা হয়েছে — if/when ব্লকের ভেতরের বাকি সব কোড অপরিবর্তিত।
            val ctaHasReleaseReq = problem.hasReleaseRequest
            val ctaIsDisputeActive = problem.isDisputed && problem.disputeSettledAt == null
            val ctaKey = JobTrackingCtaKey(status, hasPendingExtra, ctaHasReleaseReq, ctaIsDisputeActive)
            AnimatedContent(
                targetState = ctaKey,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
                    // SizeTransform override করা লাগবে না — AnimatedContent-এর default SizeTransform
                    // bounds-টা spring দিয়ে smooth animate করে, যেটা এই height-ভিন্ন branch-গুলোর
                    // (completed-summary vs solver-buttons vs user-approval-card) জন্যই দরকার।
                },
                contentAlignment = Alignment.TopStart,
                label = "job_tracking_cta_crossfade"
            ) { ctaTargetKey ->
            // exit-content freeze রাখতে status/hasPendingExtra এখন বাইরের live var থেকে না, শুধু
            // targetState প্যারামিটার থেকে shadow করা (Compose gotcha, উপরে ব্যাখ্যা করা)।
            val status = ctaTargetKey.status
            val hasPendingExtra = ctaTargetKey.hasPendingExtra
            if (status == "JOB_COMPLETED" || status == "COMPLETED") {
                // Completed Stage Summary & Action
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isUserRole) "৳${DistanceUtil.toBengaliDigits(totalGross)}" else "৳${DistanceUtil.toBengaliDigits(netSolverAmount.toInt())}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isUserRole) SomadhanTextPrimary else SomadhanSuccess
                    )
                    Text(
                        text = if (isUserRole) "পরিশোধ সম্পন্ন" else if (wasFreeQuota && confirmedExtra == 0) "আপনার নিট আয় (০% ফ্রি কোটা)" else if (confirmedExtra > 0) "আপনার নিট আয় (মূল ও অতিরিক্তের কমিশন বাদে)" else "আপনার নিট আয় (কমিশন ৳${DistanceUtil.toBengaliDigits(totalCommission.toInt())} বাদে)",
                        fontSize = 11.5.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isUserRole) {
                        Button(
                            onClick = onUserReviewDone,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("user_review_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SomadhanInfo)
                        ) {
                            Text("রিভিউ জমা দিন", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = onReturnHome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("solver_home_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SomadhanInfo)
                        ) {
                            Text("হোমে ফিরে যান", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            } else if (!isUserRole) {
                // SOLVER CTA BUTTONS
                val isDisputeActive = problem.isDisputed && problem.disputeSettledAt == null
                when (status) {
                    "ACCEPTED", "ON_WAY", "ON_THE_WAY", "SOLVER_ACCEPTED", "SOLVER_EN_ROUTE" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onSolverCancelJob,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("solver_cancel_job_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder)
                            ) {
                                Text("বাতিল", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    if (isDisputeActive) {
                                        Toast.makeText(context, "ডিসপিউট চলমান থাকায় পরবর্তী ধাপে যাওয়া সম্ভব নয়। চ্যাটে সমঝোতা করুন।", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onSolverArrived()
                                    }
                                },
                                enabled = !isDisputeActive && !isSubmittingSolverArrived,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(50.dp)
                                    .testTag("solver_arrived_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SomadhanOrange,
                                    disabledContainerColor = Color(0xFFF1F5F9),
                                    disabledContentColor = SomadhanTextHint
                                )
                            ) {
                                if (isSubmittingSolverArrived) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("পাঠানো হচ্ছে...", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                } else {
                                Text(
                                    text = if (isDisputeActive) "ডিসপিউট চলছে ⚠️" else "পৌঁছে গেছি 📍",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDisputeActive) SomadhanTextHint else Color.White
                                )
                                }
                            }
                        }
                    }
                    "ARRIVED" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onSolverCancelJob,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("solver_cancel_job_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder)
                            ) {
                                Text("বাতিল", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    if (isDisputeActive) {
                                        Toast.makeText(context, "ডিসপিউট চলমান থাকায় কাজ শুরু করা সম্ভব নয়। চ্যাটে সমঝোতা করুন।", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onSolverStartJob()
                                    }
                                },
                                enabled = !isDisputeActive && !isSubmittingSolverStartJob,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(50.dp)
                                    .testTag("solver_start_job_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SomadhanOrange,
                                    disabledContainerColor = Color(0xFFF1F5F9),
                                    disabledContentColor = SomadhanTextHint
                                )
                            ) {
                                if (isSubmittingSolverStartJob) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("পাঠানো হচ্ছে...", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                } else {
                                Text(
                                    text = if (isDisputeActive) "ডিসপিউট চলছে ⚠️" else "কাজ শুরু করুন ⚡",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDisputeActive) SomadhanTextHint else Color.White
                                )
                                }
                            }
                        }
                    }
                    "WORK_IN_PROGRESS", "IN_PROGRESS" -> {
                        val hasReleaseReq = problem.hasReleaseRequest

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Top Row: [বাতিল] (if release requested, hide extra amount button and show cancel full width)
                            if (hasReleaseReq) {
                                OutlinedButton(
                                    onClick = onSolverCancelJob,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("solver_cancel_job_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder)
                                ) {
                                    Text("বাতিল", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onSolverCancelJob,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("solver_cancel_job_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder)
                                    ) {
                                        Text("বাতিল", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = onSolverRequestExtraAmount,
                                        enabled = !hasPendingExtra && !isDisputeActive,
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(48.dp)
                                            .testTag("solver_extra_amount_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SomadhanOrange,
                                            disabledContainerColor = SomadhanOrangeLight
                                        )
                                    ) {
                                        Text(
                                            text = if (hasPendingExtra) "অপেক্ষমান…" else "➕ অতিরিক্ত এমাউন্ট",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasPendingExtra) SomadhanOrangePressed else Color.White
                                        )
                                    }
                                }
                            }

                            // Bottom Full-Width Button: [রিলিজ রিকোয়েস্ট]
                            Button(
                                onClick = onSolverRequestRelease,
                                enabled = !hasReleaseReq && !isDisputeActive && !isRequestingRelease,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("solver_release_request_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SomadhanSuccess,
                                    disabledContainerColor = Color(0xFFDCFCE7),
                                    disabledContentColor = SomadhanTextHint
                                )
                            ) {
                                if (isRequestingRelease) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("পাঠানো হচ্ছে...", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                } else {
                                    Text(
                                        text = if (isDisputeActive) "ডিসপিউট চলছে (রিলিজ স্থগিত) ⚠️" else if (hasReleaseReq) "রিলিজ অনুরোধ পাঠানো হয়েছে (অপেক্ষমান...)" else "রিলিজ রিকোয়েস্ট 🚀",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDisputeActive) SomadhanTextHint else if (hasReleaseReq) SomadhanSuccess else Color.White
                                    )
                                }
                            }
                            if (releaseError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = releaseError,
                                    fontSize = 11.5.sp,
                                    color = SomadhanError
                                )
                            }
                        }
                    }
                }
            } else {
                // USER CTA BUTTONS
                val isDisputeActive = problem.isDisputed && problem.disputeSettledAt == null
                when (status) {
                    "ACCEPTED", "ON_WAY", "ON_THE_WAY", "SOLVER_ACCEPTED", "SOLVER_EN_ROUTE" -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onDisputeClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("user_dispute_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDisputeActive) SomadhanOrange else SomadhanError
                                )
                            ) {
                                Text(
                                    text = if (isDisputeActive) "ডিসপিউট চলছে ⚠️" else "ডিসপিউট",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    "ARRIVED" -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SomadhanSuccessLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "সলভার লোকেশনে পৌঁছে গেছেন 📍",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanSuccess,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                            Button(
                                onClick = onDisputeClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("user_dispute_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDisputeActive) SomadhanOrange else SomadhanError
                                )
                            ) {
                                Text(
                                    text = if (isDisputeActive) "ডিসপিউট চলছে ⚠️" else "ডিসপিউট",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    "WORK_IN_PROGRESS", "IN_PROGRESS" -> {
                        val hasReleaseReq = problem.hasReleaseRequest

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // If solver requested extra amount, show Approval Card
                            if (hasPendingExtra) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SomadhanOrangeContainer.copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanOrangeLight),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "অতিরিক্ত বিল অনুরোধ",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = SomadhanTextPrimary
                                            )
                                            Text(
                                                text = "৳${DistanceUtil.toBengaliDigits(pendingExtra.toInt())}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = SomadhanOrangePressed
                                            )
                                        }

                                        if (!problem.pendingExtraAmountNote.isNullOrBlank()) {
                                            Text(
                                                text = "নোট: ${problem.pendingExtraAmountNote}",
                                                fontSize = 11.5.sp,
                                                color = SomadhanTextSecondary
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = onUserRejectExtraAmount,
                                                enabled = !isRejectingExtraAmount,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .testTag("reject_extra_amount_button"),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder)
                                            ) {
                                                if (isRejectingExtraAmount) {
                                                    CircularProgressIndicator(color = SomadhanError, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                } else {
                                                    Text("বাতিল", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Button(
                                                onClick = onUserConfirmExtraAmount,
                                                enabled = !isDisputeActive && !isRejectingExtraAmount,
                                                modifier = Modifier
                                                    .weight(1.2f)
                                                    .height(40.dp)
                                                    .testTag("confirm_extra_amount_button"),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess)
                                            ) {
                                                Text("কনফার্ম ✅", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                        if (rejectExtraAmountError != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = rejectExtraAmountError,
                                                fontSize = 11.sp,
                                                color = SomadhanError
                                            )
                                        }
                                    }
                                }
                            }

                            // Main Action Row (Dispute on Left, and Right action: Release or In Progress badge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = onDisputeClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("user_dispute_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDisputeActive) SomadhanOrange else SomadhanError
                                    )
                                ) {
                                    Text(
                                        text = if (isDisputeActive) "ডিসপিউট চলছে ⚠️" else "ডিসপিউট",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                if (isDisputeActive) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(48.dp)
                                            .testTag("user_release_blocked_surface"),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFFEF3C7),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        ) {
                                            Text(
                                                text = "ডিসপিউট চলছে (রিলিজ স্থগিত)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB45309),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else if (hasReleaseReq && !hasPendingExtra) {
                                    Button(
                                        onClick = onUserReleaseJob,
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(48.dp)
                                            .testTag("user_confirm_release_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess)
                                    ) {
                                        Text("রিলিজ ও সম্পন্ন করুন ✅", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else if (hasReleaseReq && hasPendingExtra) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(48.dp)
                                            .testTag("user_release_blocked_surface"),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        ) {
                                            Text(
                                                text = "অতিরিক্ত বিল অনুমোদন করার পর রিলিজ করতে পারবেন",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SomadhanTextHint,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = SomadhanInfoLight
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("কাজ চলছে...", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = SomadhanInfo)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            } // AnimatedContent crossfade content lambda (ধাপ খ, action-CTA zone ৬/৬, crossfade upgrade ব্যাচ ২৮) শেষ
        }
    }
    )
}

// Loading Pattern ধাপ খ, action-CTA zone ৬/৬ crossfade upgrade (ব্যাচ ২৮) — শুধু এই zone-এর
// AnimatedContent targetState-এর জন্য key। PulsingValue-এর rememberFieldChangePulse-এ আগে যে
// listOf(status, hasPendingExtra, ctaHasReleaseReq, ctaIsDisputeActive) ব্যবহার হতো, এখন সেই
// একই ৪টা ভ্যালু এই data class-এ (equals/hashCode auto-generated বলে AnimatedContent ঠিকভাবে
// old vs new targetState তুলনা করতে পারবে)।
private data class JobTrackingCtaKey(
    val status: String,
    val hasPendingExtra: Boolean,
    val hasReleaseReq: Boolean,
    val isDisputeActive: Boolean
)

/**
 * Progress Stepper Node representation for dynamic derivation.
 */
data class TrackingStepNode(
    val title: String,
    val isDone: Boolean,
    val isCurrent: Boolean,
    val isDispute: Boolean = false,
    val isSettled: Boolean = false
)

/**
 * Phase V: Dynamic Progress Stepper with inline "বিরোধ" & "সমঝোতা" steps.
 * Stages dynamically derive: ১. আসছেন -> [২. পৌঁছে গেছেন] -> [বিরোধ] -> [সমঝোতা] -> [কাজ চলছে] -> [সম্পন্ন]
 */
@Composable
fun TrackingProgressStepper(
    currentStepIdx: Int,
    isDisputed: Boolean = false,
    disputeSettledAt: Long? = null,
    problem: ProblemEntity? = null
) {
    val hasDispute = isDisputed || (disputeSettledAt != null && disputeSettledAt > 0L)
    val isDisputeActive = isDisputed && (disputeSettledAt == null || disputeSettledAt <= 0L)
    val isDisputeSettled = disputeSettledAt != null && disputeSettledAt > 0L

    // Determine the exact progress level when dispute was raised (2: On Way, 3: Arrived, 4: Started/In Progress, 5: Completed)
    val raiseLevel = problem?.disputeProgressAtRaise?.takeIf { it > 0 } ?: problem?.calculateProgressStep() ?: 2

    // Helper lambda to insert dispute & settlement nodes
    fun MutableList<TrackingStepNode>.addDisputeNodes() {
        if (!hasDispute) return
        add(
            TrackingStepNode(
                title = "বিরোধ",
                isDone = isDisputeSettled,
                isCurrent = isDisputeActive,
                isDispute = true
            )
        )
        if (isDisputeSettled) {
            add(
                TrackingStepNode(
                    title = "সমঝোতা",
                    isDone = true,
                    isCurrent = false,
                    isSettled = true
                )
            )
        }
    }

    // Build the dynamic list of steps freshly on each recomposition (pure derivation, no accumulation)
    val steps = buildList {
        // Base Step 1: আসছেন (level 2)
        val isStep1Done = if (hasDispute) {
            if (raiseLevel >= 2) true else currentStepIdx > 1
        } else {
            currentStepIdx > 1
        }
        val isStep1Current = !isDisputeActive && currentStepIdx == 1
        add(
            TrackingStepNode(
                title = "আসছেন",
                isDone = isStep1Done,
                isCurrent = isStep1Current
            )
        )

        // Insert dispute if raised at or before level 2 (on the way)
        if (hasDispute && raiseLevel <= 2) {
            addDisputeNodes()
        }

        // Base Step 2: পৌঁছে গেছেন (level 3)
        val isStep2Done = if (hasDispute) {
            if (isDisputeActive) {
                raiseLevel >= 3
            } else {
                raiseLevel >= 3 || currentStepIdx > 2
            }
        } else {
            currentStepIdx > 2
        }
        val isStep2Current = !isDisputeActive && (currentStepIdx == 2 || (isDisputeSettled && currentStepIdx == 2 && raiseLevel < 3))
        add(
            TrackingStepNode(
                title = "পৌঁছে গেছেন",
                isDone = isStep2Done,
                isCurrent = isStep2Current
            )
        )

        // Insert dispute if raised at level 3 (after arrival, before start)
        if (hasDispute && raiseLevel == 3) {
            addDisputeNodes()
        }

        // Base Step 3: কাজ চলছে (level 4)
        val isStep3Done = if (hasDispute) {
            if (isDisputeActive) {
                raiseLevel >= 4
            } else {
                raiseLevel >= 4 || currentStepIdx > 3
            }
        } else {
            currentStepIdx > 3
        }
        val isStep3Current = !isDisputeActive && (currentStepIdx == 3 || (isDisputeSettled && currentStepIdx == 3 && raiseLevel < 4))
        add(
            TrackingStepNode(
                title = "কাজ চলছে",
                isDone = isStep3Done,
                isCurrent = isStep3Current
            )
        )

        // Insert dispute if raised at level 4 (during job)
        if (hasDispute && raiseLevel == 4) {
            addDisputeNodes()
        }

        // Base Step 4: সম্পন্ন (level 5)
        val isStep4Done = if (hasDispute) {
            if (isDisputeActive) {
                raiseLevel >= 5
            } else {
                raiseLevel >= 5 || currentStepIdx >= 4
            }
        } else {
            currentStepIdx >= 4
        }
        val isStep4Current = !isDisputeActive && (currentStepIdx == 4 || (isDisputeSettled && currentStepIdx == 4 && raiseLevel < 5))
        add(
            TrackingStepNode(
                title = "সম্পন্ন",
                isDone = isStep4Done,
                isCurrent = isStep4Current
            )
        )

        // Insert dispute if raised at level 5 (completed / release)
        if (hasDispute && raiseLevel >= 5) {
            addDisputeNodes()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isDisputeActive || isDisputeSettled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDisputeActive) SomadhanErrorLight else SomadhanSuccessLight,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDisputeActive) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isDisputeActive) "⚠️ বিরোধ চলমান (চ্যাটে সমঝোতা করুন)" else "🤝 বিরোধের সমঝোতা সম্পন্ন",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDisputeActive) SomadhanError else SomadhanSuccess
                        )
                    }
                }
            }
        }

        // Stepper circles & connecting lines
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, node ->
                val stepNumber = index + 1
                val circleColor = when {
                    node.isDispute && node.isCurrent -> SomadhanError
                    node.isDispute && node.isDone -> Color(0xFFF59E0B)
                    node.isSettled -> SomadhanSuccess
                    node.isDone -> SomadhanSuccess
                    node.isCurrent -> SomadhanOrange
                    else -> SomadhanDivider
                }

                Box(
                    modifier = Modifier
                        .size(if (steps.size > 4) 22.dp else 24.dp)
                        .clip(CircleShape)
                        .background(circleColor),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        node.isDispute -> {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "বিরোধ",
                                tint = Color.White,
                                modifier = Modifier.size(if (steps.size > 4) 12.dp else 14.dp)
                            )
                        }
                        node.isSettled || node.isDone -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (steps.size > 4) 12.dp else 14.dp)
                            )
                        }
                        else -> {
                            Text(
                                text = DistanceUtil.toBengaliDigits(stepNumber),
                                color = if (node.isCurrent) Color.White else SomadhanTextSecondary,
                                fontSize = if (steps.size > 4) 10.sp else 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (index < steps.size - 1) {
                    val nextNode = steps[index + 1]
                    val lineColor = when {
                        node.isDone && nextNode.isDispute && nextNode.isCurrent -> SomadhanError
                        node.isDone -> SomadhanSuccess
                        else -> SomadhanDivider
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.5.dp)
                            .background(lineColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Stepper labels row with proportional distribution
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, node ->
                val fullTitle = "${DistanceUtil.toBengaliDigits(index + 1)}. ${node.title}"
                val textColor = when {
                    node.isDispute && node.isCurrent -> SomadhanError
                    node.isDispute && node.isDone -> Color(0xFFD97706)
                    node.isSettled -> SomadhanSuccess
                    node.isDone -> SomadhanSuccess
                    node.isCurrent -> SomadhanOrange
                    else -> SomadhanTextSecondary
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = when (index) {
                        0 -> Alignment.TopStart
                        steps.size - 1 -> Alignment.TopEnd
                        else -> Alignment.TopCenter
                    }
                ) {
                    Text(
                        text = fullTitle,
                        fontSize = if (steps.size > 4) 9.sp else 10.5.sp,
                        fontWeight = if (node.isCurrent || node.isDone || node.isDispute || node.isSettled) FontWeight.Bold else FontWeight.Normal,
                        color = textColor,
                        textAlign = when (index) {
                            0 -> TextAlign.Start
                            steps.size - 1 -> TextAlign.End
                            else -> TextAlign.Center
                        },
                        lineHeight = if (steps.size > 4) 11.sp else 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * UserJobBroadcastingScreen: Phase B.1 Screen for User when job is BROADCASTING.
 * Displays:
 * 1. Live animated Radar map scanning around user location.
 * 2. Floating Top Bar with back button and broadcast indicator.
 * 3. Bottom panel with countdown timer from platform settings, problem summary,
 *    live mini-bids list from nearby solvers, accept bid CTA, and cancel job option.
 */
@Composable
fun UserJobBroadcastingScreen(
    problem: ProblemEntity,
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val allBids by viewModel.allBids.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val allPlatformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showCancelDialog by remember { mutableStateOf(false) }
    var selectedBidId by remember { mutableStateOf<String?>(null) }
    var bidForGatewayPayment by remember { mutableStateOf<Pair<BidEntity, Double>?>(null) }
    var showPaymentConfirmationDialog by remember { mutableStateOf(false) }
    var pendingConfirmBid by remember { mutableStateOf<BidEntity?>(null) }
    var isAccepting by remember { mutableStateOf(false) }
    var acceptInstantBidError by remember { mutableStateOf<String?>(null) }
    var acceptInstantBidGatewayPending by remember { mutableStateOf(false) }
    var acceptInstantBidGatewayError by remember { mutableStateOf<String?>(null) }
    // Bug fix: previously the gateway-retry path referenced `pendingConfirmBid`, which is only
    // ever set by the wallet-confirmation dialog flow -- if the gateway dialog was entered via
    // the other call site (direct bid-card -> bidForGatewayPayment, without going through
    // pendingConfirmBid first), `pendingConfirmBid` could be null or stale, making "আবার চেষ্টা
    // করুন" retry the wrong bid or silently no-op. This dedicated var is set at the moment the
    // gateway payment succeeds, from the exact bid that was actually paid for, so retry is
    // always correct regardless of which entry point was used.
    var acceptInstantBidGatewayBid by remember { mutableStateOf<BidEntity?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }

    // The poster is actively watching this screen's live radar/countdown for new bids to arrive.
    // The background sync's bids listener deliberately doesn't cover "bids on my own posts" in
    // real time (see ENGINEERING_NOTES.md §2 and the comment above listenToBidsForProblem), so
    // this screen opts into its own temporary live listener for just this one problem while it's
    // open, and removes it the moment the poster navigates away.
    //
    // [SUPABASE-MIGRATED - ধাপ ২৩, কোনো নতুন কোড লাগেনি] Firebase-এর ভার্সনটা উপরে যা বলছে,
    // তার Supabase-সমতুল্য সীমাবদ্ধতাই নেই -- verify করা হয়েছে (এই session-এ Supabase MCP
    // দিয়ে সরাসরি DB-তে গিয়ে): `bids` টেবিলের RLS SELECT policy (`bids_select`) হলো
    // `auth.uid() = solver_id OR is_admin(auth.uid()) OR EXISTS(problems WHERE problem_id
    // matches AND user_id = auth.uid())` -- অর্থাৎ পোস্টদাতা (এই স্ক্রিনের ব্যবহারকারী)
    // ইতিমধ্যেই RLS-এর আওতায় নিজের পোস্টের সব বিড দেখতে পান। `SupabaseRealtimeManager`-এর
    // গ্লোবাল bids channel (ধাপ ২০-এ তৈরি, ধাপ ২২-এ session-শুরুতেই সাবস্ক্রাইব হয়) তাই এই
    // পোস্টদাতার জন্য তার নিজের পোস্টে আসা প্রতিটা বিড ইতিমধ্যেই লাইভ Room-এ (bidDao) upsert
    // করে দিচ্ছে -- আর এই স্ক্রিনের `allBids` (উপরে `viewModel.allBids`) সেই একই Room টেবিল
    // থেকেই আসে। তাই এখানে আলাদা কোনো per-problem Supabase subscription (dual-run) যোগ করার
    // দরকার নেই -- Firebase কলটা (dual-run নীতি অনুযায়ী) অপরিবর্তিত রাখা হলো, আর Supabase
    // পাশ ইতিমধ্যেই (কোনো নতুন কোড ছাড়াই) সমতুল্য কভারেজ দিচ্ছে। বিস্তারিত বিশ্লেষণ:
    // `MIGRATION_PROGRESS.md`-এর "ধাপ ২৩" এন্ট্রি।
    // Phase I.3: Auto-redirect when job is cancelled in background is managed by parent JobTrackingScreen

    // Read broadcast timeout setting (default 300s = 5 mins)
    val timeoutSeconds = remember(allPlatformSettings) {
        allPlatformSettings.find { it.key == "instant_job_broadcast_timeout_seconds" }?.value?.toLongOrNull() ?: 300L
    }

    // Read max active instant jobs per solver setting (default 1)
    val maxActivePerSolver = remember(allPlatformSettings) {
        allPlatformSettings.find { it.key == "instant_job_max_active_per_solver" }?.value?.toIntOrNull() ?: 1
    }

    // Dynamic ticking countdown
    val timerStart = problem.broadcastTimerStartedAt ?: problem.createdAt
    var remainingSeconds by remember(timerStart, timeoutSeconds) {
        val elapsed = (System.currentTimeMillis() - timerStart) / 1000
        mutableLongStateOf((timeoutSeconds - elapsed).coerceAtLeast(0L))
    }

    LaunchedEffect(timerStart, timeoutSeconds) {
        while (remainingSeconds > 0) {
            delay(1000)
            val elapsed = (System.currentTimeMillis() - timerStart) / 1000
            remainingSeconds = (timeoutSeconds - elapsed).coerceAtLeast(0L)
        }
        if (remainingSeconds <= 0) {
            if (BuildConfig.DEBUG) {
                Log.d("InstantJobDebug", "UserJobBroadcastingScreen countdown hit 0 for problemId=${problem.id}, triggering instant expiry check")
            }
            viewModel.triggerInstantJobExpiryCheck()
        }
    }

    val userLat = if (problem.latitude != 0.0) problem.latitude else 23.7557
    val userLng = if (problem.longitude != 0.0) problem.longitude else 90.3644

    // Live bids for this problem (Phase A.3 / Phase C)
    val problemBids = remember(allBids, problem.id) {
        val cancelledSolverIds = allBids
            .filter { it.problemId == problem.id && (it.status == "CANCELLED" || it.status == "WITHDRAWN" || it.status == "REJECTED") }
            .map { it.solverId }
            .toSet()

        allBids.filter { bid ->
            bid.problemId == problem.id &&
            bid.status == "PENDING" &&
            bid.solverId !in cancelledSolverIds &&
            bid.solverId != problem.acceptedSolverId
        }
    }

    // Sort bids by solver rating / reputation descending
    val sortedBids = remember(problemBids, allUsers) {
        problemBids.sortedByDescending { bid ->
            val solver = allUsers.find { it.id == bid.solverId }
            solver?.reputationScore?.takeIf { it > 0.0 } ?: bid.solverRating
        }
    }

    // Auto-select first available bid if nothing is selected or if previous selected bid was cancelled
    LaunchedEffect(sortedBids) {
        if (selectedBidId != null && sortedBids.none { it.id == selectedBidId }) {
            selectedBidId = null
        }
        if (selectedBidId == null && sortedBids.isNotEmpty()) {
            val firstAvailable = sortedBids.firstOrNull { bid ->
                val activeJobs = allProblems.count { prob ->
                    prob.isInstantJob &&
                    prob.acceptedSolverId == bid.solverId &&
                    prob.jobStatus in listOf("ON_WAY", "ON_THE_WAY", "ARRIVED", "IN_PROGRESS", "WORK_IN_PROGRESS", "ACCEPTED") &&
                    prob.status != "COMPLETED" && prob.status != "CANCELLED"
                }
                activeJobs < maxActivePerSolver
            }
            if (firstAvailable != null) {
                selectedBidId = firstAvailable.id
            }
        }
    }

    val selectedBid = remember(sortedBids, selectedBidId) {
        sortedBids.find { it.id == selectedBidId }
    }

    val isSelectedBidBusy = remember(selectedBid, allProblems, maxActivePerSolver) {
        if (selectedBid == null) false
        else {
            val activeJobs = allProblems.count { prob ->
                prob.isInstantJob &&
                prob.acceptedSolverId == selectedBid.solverId &&
                prob.jobStatus in listOf("ON_WAY", "ON_THE_WAY", "ARRIVED", "IN_PROGRESS", "WORK_IN_PROGRESS", "ACCEPTED") &&
                prob.status != "COMPLETED" && prob.status != "CANCELLED"
            }
            activeJobs >= maxActivePerSolver
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SomadhanBg)
            .testTag("user_job_broadcasting_screen")
    ) {
        // 1. RADAR SCAN MAP BACKGROUND
        RadarMapScanOverlay(
            userLat = userLat,
            userLng = userLng,
            modifier = Modifier.fillMaxSize()
        )

        // 2. TOP BAR
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding + 10.dp, start = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clickable { onNavigateBack() }
                    .testTag("broadcasting_back_button"),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "ফিরে যান",
                        tint = SomadhanTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulsingLiveDot()
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "জরুরি জব সম্প্রচার 📡",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = if (problemBids.isEmpty()) "আশেপাশের সলভারদের কাছে পৌঁছানো হচ্ছে..."
                            else "${DistanceUtil.toBengaliDigits(problemBids.size)}টি প্রস্তাব পাওয়া গেছে",
                            fontSize = 11.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }
            }
        }

        // 3. DRAGGABLE BOTTOM PANEL WITH COUNTDOWN, SUMMARY, AND BIDS (PHASE Q)
        DraggableMapSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            initialState = MapSheetStateValue.DEFAULT,
            collapsedBar = { sheetState, onToggleExpand ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = SomadhanOrangeLight
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                PulsingLiveDot()
                                Text(
                                    text = "সম্প্রচার 📡",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrangePressed
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = problem.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SomadhanTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val minutes = remainingSeconds / 60
                            val secs = remainingSeconds % 60
                            Text(
                                text = if (problemBids.isEmpty()) "আশেপাশে খোঁজা হচ্ছে • ${DistanceUtil.toBengaliDigits(String.format("%02d:%02d", minutes, secs))} বাকি"
                                else "${DistanceUtil.toBengaliDigits(problemBids.size)}টি প্রস্তাব • ${DistanceUtil.toBengaliDigits(String.format("%02d:%02d", minutes, secs))} বাকি",
                                fontSize = 11.5.sp,
                                color = if (problemBids.isNotEmpty()) SomadhanOrangePressed else SomadhanTextSecondary,
                                fontWeight = if (problemBids.isNotEmpty()) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { onToggleExpand() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = when (sheetState) {
                                MapSheetStateValue.COLLAPSED -> Icons.Default.KeyboardArrowUp
                                MapSheetStateValue.DEFAULT -> Icons.Default.UnfoldMore
                                MapSheetStateValue.EXPANDED -> Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = "টগল করুন",
                            tint = SomadhanTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            expandedContent = { sheetState ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                // Countdown Timer Header Card
                val progress = if (timeoutSeconds > 0) {
                    (remainingSeconds.toFloat() / timeoutSeconds.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SomadhanOrangeLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = SomadhanOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (remainingSeconds > 0) "প্রস্তাব গ্রহণের সময় বাকি" else "সময় শেষ হয়েছে",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrangePressed
                                )
                            }

                            val minutes = remainingSeconds / 60
                            val secs = remainingSeconds % 60
                            Text(
                                text = "${DistanceUtil.toBengaliDigits(String.format("%02d:%02d", minutes, secs))} মি.",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = SomadhanOrangePressed
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SomadhanOrange,
                            trackColor = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Job Summary Header Card + Post ID
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SomadhanCardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = problem.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SomadhanTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                // Post ID Chip (1-click copyable)
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
                                        .testTag("broadcasting_job_id_${problem.id}")
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
                            if (problem.userAddress.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = problem.userAddress,
                                    fontSize = 11.5.sp,
                                    color = SomadhanTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "আপনার বাজেট",
                                fontSize = 10.5.sp,
                                color = SomadhanTextHint
                            )
                            val userBudgetText = if (problem.maxBudget > problem.minBudget) {
                                "৳ ${DistanceUtil.toBengaliDigits(problem.minBudget)} - ${DistanceUtil.toBengaliDigits(problem.maxBudget)}"
                            } else {
                                "৳ ${DistanceUtil.toBengaliDigits(problem.minBudget)}"
                            }
                            Text(
                                text = userBudgetText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SomadhanOrange
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // BIDS SECTION (PHASE C) — Loading Pattern ধাপ খ (JobTrackingScreen zone ১/৬):
                // sortedBids বদলালে (নতুন বিড আসা/সরে যাওয়া, empty↔non-empty state-বদলসহ) পুরো
                // এই zone-টা হালকা pulse দিয়ে আপডেট দেখাবে — বাকি স্ক্রিন (countdown timer কার্ড,
                // job summary/বাজেট কার্ড, উপরের top bar, radar background) সম্পূর্ণ স্থির থাকবে।
                // rememberFieldChangePulse (SyncAwareRefreshableContent না) ইচ্ছাকৃতভাবে বেছে
                // নেওয়া হয়েছে — এটা শুধু value-diff-এ pulse করে, নিজের কোনো cold-skeleton গেট নেই,
                // তাই বাইরের JobTrackingScreen-এর rule ১ গেট (যেটা এতক্ষণে already pass হয়ে গেছে)-এর
                // উপরে বসে আরেকবার আলাদা skeleton flash করবে না। sessionKey ইচ্ছাকৃতভাবে বাইরের
                // JobTrackingScreen-এর "job_tracking_${problem.id}"-এর সাথে মিলিয়ে দেওয়া হয়েছে
                // (এই key আগে থেকেই markLoadedOnce হয়ে আছে), তাই re-entry-flash ঘটলেও সেটা
                // consistent থাকবে outer গেটের সাথে।
                PulsingValue(
                    isUpdating = rememberFieldChangePulse(
                        value = sortedBids,
                        sessionKey = "job_tracking_${problem.id}",
                        viewModel = viewModel
                    )
                ) {
                Column {
                if (sortedBids.isEmpty()) {
                    // Empty state with searching indicator
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = SomadhanOrange,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "সলভারদের প্রস্তাবের অপেক্ষা করা হচ্ছে...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "আশেপাশের সলভাররা বিড দিলে সাথে সাথে এখানে দেখতে পাবেন।",
                                fontSize = 11.5.sp,
                                color = SomadhanTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cancel Job Button
                    Button(
                        onClick = { showCancelDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SomadhanErrorLight,
                            contentColor = SomadhanError
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("cancel_broadcasting_job_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = SomadhanError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "পোস্টটি বাতিল করুন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = SomadhanError
                        )
                    }
                } else {
                    // Stage 0.5: Active Bids Selection Header
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = SomadhanOrangeLight,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(SomadhanOrange)
                            )
                            Text(
                                text = "${DistanceUtil.toBengaliDigits(sortedBids.size)}টি ইনস্ট্যান্ট বিড এসেছে",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrangePressed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "যেকোনো ১টি বিড বেছে নিন",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = SomadhanTextPrimary
                    )
                    Text(
                        text = "যেই সমাধানকারীকে বেছে নেবেন তিনিই কাজটি পাবেন",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bids List
                    sortedBids.forEach { bid ->
                        val solver = allUsers.find { it.id == bid.solverId }
                        val activeJobs = allProblems.count { prob ->
                            prob.isInstantJob &&
                            prob.acceptedSolverId == bid.solverId &&
                            prob.jobStatus in listOf("ON_WAY", "ON_THE_WAY", "ARRIVED", "IN_PROGRESS", "WORK_IN_PROGRESS", "ACCEPTED") &&
                            prob.status != "COMPLETED" && prob.status != "CANCELLED"
                        }
                        val isSolverBusy = activeJobs >= maxActivePerSolver
                        val isSelected = selectedBidId == bid.id

                        val completedJobsCount = allProblems.count { it.acceptedSolverId == bid.solverId && it.status == "COMPLETED" }

                        BroadcastBidCard(
                            bid = bid,
                            solver = solver,
                            completedJobsCount = completedJobsCount,
                            isSelected = isSelected,
                            isBusy = isSolverBusy,
                            onSelect = {
                                if (!isSolverBusy) {
                                    selectedBidId = bid.id
                                    actionError = null
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    actionError?.let { err ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = err,
                            fontSize = 12.sp,
                            color = SomadhanError,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons (বাতিল & কনফার্ম করুন)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel Button (Phase I.4: Direct cancel dialog)
                        Button(
                            onClick = {
                                showCancelDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SomadhanBg,
                                contentColor = SomadhanTextSecondary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("cancel_bid_selection_button")
                        ) {
                            Text(
                                text = "বাতিল",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = SomadhanTextSecondary
                            )
                        }

                        // Confirm Button (Phase I.2)
                        Button(
                            onClick = {
                                val currentBid = selectedBid
                                if (currentBid == null) {
                                    actionError = "অনুগ্রহ করে একটি বিড নির্বাচন করুন"
                                    return@Button
                                }
                                if (isSelectedBidBusy) {
                                    actionError = "এই সমাধানকারী বর্তমানে অন্য কাজে ব্যস্ত আছেন।"
                                    return@Button
                                }

                                actionError = null
                                val userBalance = currentUser?.balanceUser ?: 0.0
                                val bidAmount = currentBid.amount

                                if (userBalance <= 0.0) {
                                    // কেস গ — আগের মতোই, সরাসরি গেটওয়ে
                                    bidForGatewayPayment = Pair(currentBid, bidAmount)
                                } else {
                                    // কেস ক/খ — নতুন popup দেখাও
                                    pendingConfirmBid = currentBid
                                    showPaymentConfirmationDialog = true
                                }
                            },
                            enabled = selectedBid != null && !isSelectedBidBusy && !isAccepting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SomadhanOrange,
                                disabledContainerColor = SomadhanOrange.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.6f)
                                .height(46.dp)
                                .testTag("confirm_bid_selection_button")
                        ) {
                            if (isAccepting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "কনফার্ম হচ্ছে...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "কনফার্ম করুন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } // bids-section if/else শেষ
                } // Column (ধাপ খ pulse-zone wrapper) শেষ
                } // PulsingValue content lambda (ধাপ খ, বিড-জোন) শেষ
            }
        }
    )
}

    // Phase I.1 & I.2: Payment Confirmation Dialog
    if (showPaymentConfirmationDialog && pendingConfirmBid != null) {
        val targetBid = pendingConfirmBid!!
        val userBalance = currentUser?.balanceUser ?: 0.0
        val bidAmount = targetBid.amount
        PaymentConfirmationDialog(
            walletBalance = userBalance,
            bidAmount = bidAmount,
            isLoading = isAccepting,
            errorMessage = acceptInstantBidError,
            onProceed = {
                if (userBalance >= bidAmount) {
                    isAccepting = true
                    acceptInstantBidError = null
                    viewModel.acceptInstantJobBid(
                        problem = problem,
                        bid = targetBid,
                        userLiveLat = userLat,
                        userLiveLng = userLng,
                        onSuccess = {
                            isAccepting = false
                            showPaymentConfirmationDialog = false
                            acceptInstantBidError = null
                            Toast.makeText(context, "বিড সফলভাবে গৃহীত হয়েছে! 🚀", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            isAccepting = false
                            acceptInstantBidError = err
                        }
                    )
                } else {
                    showPaymentConfirmationDialog = false
                    acceptInstantBidError = null
                    bidForGatewayPayment = Pair(targetBid, bidAmount - userBalance)
                }
            },
            onDismiss = {
                if (!isAccepting) {
                    showPaymentConfirmationDialog = false
                    pendingConfirmBid = null
                    acceptInstantBidError = null
                }
            }
        )
    }

    // Escrow Merchant Payment Dialog for Bid Selection
    if (bidForGatewayPayment != null) {
        val (targetBid, payableAmount) = bidForGatewayPayment!!
        MerchantPaymentDialog(
            amount = payableAmount,
            problemTitle = problem.title,
            solverName = targetBid.solverName ?: "দক্ষ সমাধানকারী",
            onPaymentSuccess = {
                val b = targetBid
                bidForGatewayPayment = null
                acceptInstantBidGatewayPending = true
                acceptInstantBidGatewayError = null
                acceptInstantBidGatewayBid = b
                viewModel.acceptInstantJobBid(
                    problem = problem,
                    bid = b,
                    userLiveLat = userLat,
                    userLiveLng = userLng,
                    onSuccess = {
                        acceptInstantBidGatewayPending = false
                        acceptInstantBidGatewayBid = null
                        Toast.makeText(context, "পেমেন্ট সম্পন্ন ও বিড গৃহীত হয়েছে! 🚀", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        acceptInstantBidGatewayError = err
                    }
                )
            },
            onDismissRequest = {
                bidForGatewayPayment = null
            },
            onPaymentCompleteWithDetails = { gateway, trxId, phone ->
                viewModel.recordGatewayPayment(
                    gateway = gateway,
                    gatewayTrxId = trxId,
                    amount = payableAmount,
                    senderPhone = phone,
                    purpose = "ESCROW_PAYMENT",
                    problemId = problem.id,
                    problemTitle = problem.title,
                    note = "Problem #${problem.id.take(8)}"
                )
            }
        )
    }

    // Post-gateway-payment verification overlay for acceptInstantJobBid (section 3 pattern)
    // -- MerchantPaymentDialog already closed itself by the time this shows.
    if (acceptInstantBidGatewayPending) {
        BottomSlideAlertDialog(
            onDismissRequest = { /* locked until success or explicit dismiss-on-error below */ },
            title = { Text("লেনদেন যাচাই হচ্ছে", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary) },
            text = {
                Column {
                    if (acceptInstantBidGatewayError == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = SomadhanOrange, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("লেনদেন যাচাই হচ্ছে...", fontSize = 13.sp, color = SomadhanTextPrimary)
                        }
                    } else {
                        Text(
                            text = acceptInstantBidGatewayError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError
                        )
                    }
                }
            },
            confirmButton = {
                if (acceptInstantBidGatewayError != null) {
                    val retryBid = acceptInstantBidGatewayBid
                    Button(onClick = {
                        if (retryBid != null) {
                            acceptInstantBidGatewayError = null
                            viewModel.acceptInstantJobBid(
                                problem = problem,
                                bid = retryBid,
                                userLiveLat = userLat,
                                userLiveLng = userLng,
                                onSuccess = {
                                    acceptInstantBidGatewayPending = false
                                    acceptInstantBidGatewayBid = null
                                    Toast.makeText(context, "পেমেন্ট সম্পন্ন ও বিড গৃহীত হয়েছে! 🚀", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err -> acceptInstantBidGatewayError = err }
                            )
                        } else {
                            acceptInstantBidGatewayPending = false
                        }
                    }) { Text("আবার চেষ্টা করুন") }
                }
            },
            dismissButton = {
                if (acceptInstantBidGatewayError != null) {
                    TextButton(onClick = {
                        acceptInstantBidGatewayPending = false
                        acceptInstantBidGatewayError = null
                        acceptInstantBidGatewayBid = null
                    }) { Text("বাতিল", color = SomadhanTextSecondary) }
                }
            }
        )
    }


    // Cancel Job Confirmation Dialog
    if (showCancelDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(
                    text = "পোস্ট বাতিল করতে চান?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে সম্প্রচারটি বন্ধ করে পোস্টটি বাতিল করতে চান?",
                    fontSize = 13.5.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onNavigateBack()
                        viewModel.cancelInstantJob(problem.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError)
                ) {
                    Text("হ্যাঁ, বাতিল করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("না", color = SomadhanTextSecondary)
                }
            }
        )
    }
}

/**
 * BroadcastBidCard: Selectable card displaying an individual solver's bid in the broadcasting sheet.
 */
@Composable
fun BroadcastBidCard(
    bid: BidEntity,
    solver: UserEntity?,
    completedJobsCount: Int,
    isSelected: Boolean,
    isBusy: Boolean,
    onSelect: () -> Unit
) {
    val solverName = solver?.name ?: bid.solverName ?: "দক্ষ সমাধানকারী"
    val ratingScore = solver?.reputationScore?.takeIf { it > 0.0 } ?: bid.solverRating.takeIf { it > 0.0 } ?: 4.9
    val initialChar = solverName.trim().take(1).ifBlank { "স" }

    val elapsedMs = (System.currentTimeMillis() - bid.createdAt).coerceAtLeast(0L)
    val elapsedMins = (elapsedMs / (60 * 1000)).toInt()
    val relativeTimeText = when {
        elapsedMins <= 0 -> "এখন"
        elapsedMins < 60 -> "${DistanceUtil.toBengaliDigits(elapsedMins)} মিনিট আগে"
        else -> "${DistanceUtil.toBengaliDigits(elapsedMins / 60)} ঘণ্টা আগে"
    }

    val cardBg = if (isSelected) SomadhanOrangeLight.copy(alpha = 0.35f) else SomadhanCardBg
    val cardBorder = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, SomadhanOrange)
    else androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isBusy) { onSelect() }
            .testTag("bid_card_${bid.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .alpha(if (isBusy) 0.55f else 1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Radio/Checkmark + Solver info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Selection indicator
                    if (isBusy) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "ব্যস্ত",
                            tint = SomadhanTextHint,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "নির্বাচিত",
                            tint = SomadhanOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = "নির্বাচন করুন",
                            tint = SomadhanBorder,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) SomadhanOrange.copy(alpha = 0.15f) else SomadhanOrangeLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initialChar,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SomadhanOrangePressed
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = solverName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (isBusy) SomadhanTextSecondary else SomadhanTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "•",
                                color = SomadhanTextHint,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format("%.1f", ratingScore),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${DistanceUtil.toBengaliDigits(completedJobsCount)}+ কাজ সম্পন্ন • $relativeTimeText",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }

                        if (bid.estimatedTime.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "পৌঁছাতে: ${bid.estimatedTime}",
                                fontSize = 11.sp,
                                color = SomadhanSuccess,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (isBusy) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SomadhanErrorLight
                            ) {
                                Text(
                                    text = "এই মুহূর্তে ব্যস্ত",
                                    color = SomadhanError,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Bid price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "৳ ${DistanceUtil.toBengaliDigits(bid.amount.toInt())}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.5.sp,
                        color = if (isBusy) SomadhanTextSecondary else SomadhanOrange
                    )
                }
            }

            if (bid.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = bid.message,
                    fontSize = 11.5.sp,
                    color = SomadhanTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * RadarMapScanOverlay: Animated radar map canvas with animated concentric ripples.
 */
@Composable
fun RadarMapScanOverlay(
    userLat: Double,
    userLng: Double,
    modifier: Modifier = Modifier
) {
    val userLatLng = remember(userLat, userLng) { LatLng(userLat, userLng) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLatLng, 15f)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radar_waves")
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3"
    )

    val isMapsApiKeyConfigured = remember {
        val key = com.example.BuildConfig.MAPS_API_KEY
        key.isNotBlank() &&
            !key.equals("YOUR_MAPS_API_KEY", ignoreCase = true) &&
            !key.startsWith("YOUR_", ignoreCase = true) &&
            !key.startsWith("AIzaSyDummy", ignoreCase = true) &&
            !key.startsWith("DEMO_", ignoreCase = true) &&
            key.startsWith("AIza")
    }

    Box(modifier = modifier) {
        // Fallback canvas grid
        JobTrackingCanvasFallback(
            status = "BROADCASTING",
            modifier = Modifier.fillMaxSize()
        )

        // Google Map Composable Layer (Rendered only when valid Maps API key is configured)
        if (isMapsApiKeyConfigured) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = false,
                    mapToolbarEnabled = false
                )
            ) {
                Marker(
                    state = MarkerState(position = userLatLng),
                    title = "আপনার অবস্থান",
                    snippet = "জরুরি জব সম্প্রচার কেন্দ্র"
                )
            }
        }

        // Radar Ripple Wave Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.38f
            val maxRadius = size.width * 0.6f

            listOf(wave1, wave2, wave3).forEach { progress ->
                val radius = maxRadius * progress
                val alpha = (1f - progress).coerceIn(0f, 1f) * 0.45f
                drawCircle(
                    color = SomadhanOrange.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = SomadhanOrange.copy(alpha = alpha * 0.8f),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

/**
 * JobCompletionSummaryScreen (Phase L)
 *
 * Full-screen completion summary shown when an instant job is completed.
 * Features:
 * - Stepper with all 4 stages completed (✓)
 * - Dynamic Timeline from existing timestamps (createdAt, acceptedAt2, onWayAt, arrivedAt, jobStartedAt, completedAt)
 * - Other party's profile card (photo/avatar, name, role badge, category/rating)
 * - Final payment calculation (Base bid + Confirmed Extra - Commission for Solver only)
 * - Single "ফিরে যান" button that calls onNavigateBack()
 */
@Composable
fun JobCompletionSummaryScreen(
    problem: ProblemEntity,
    otherUser: UserEntity?,
    isUserRole: Boolean,
    allPlatformSettings: List<PlatformSettingEntity>,
    viewModel: SomadhanViewModel? = null,
    isCancelled: Boolean = (problem.jobStatus == "CANCELLED" || problem.status == "CANCELLED"),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val escrow by viewModel?.getEscrowForProblem(problem.id)?.collectAsStateWithLifecycle(initialValue = null) ?: remember { mutableStateOf(null) }

    // Pricing calculation
    val (baseAmount, extraAmount) = SomadhanRepository.resolveSettlementAmounts(problem, escrow)
    val totalGross = baseAmount + extraAmount

    // Solver-only commission calculation with centralized breakdown (Phase N.3)
    var commissionBreakdown by remember(problem.id, baseAmount, extraAmount, problem.appliedCommissionRate) {
        mutableStateOf<SomadhanRepository.CommissionBreakdown?>(null)
    }

    LaunchedEffect(problem.id, baseAmount, extraAmount, problem.appliedCommissionRate) {
        val sId = problem.acceptedSolverId ?: ""
        if (viewModel != null) {
            commissionBreakdown = viewModel.previewCommissionBreakdown(problem, sId, baseAmount, extraAmount)
        }
    }

    val defaultCommissionRate = allPlatformSettings.find { it.key == "commission_percent" }?.value?.toDoubleOrNull() ?: 10.0
    val solverCommissionRate = problem.appliedCommissionRate ?: defaultCommissionRate
    val fallbackPlatformFee = (totalGross * solverCommissionRate) / 100.0
    val fallbackNetSolverAmount = (totalGross - fallbackPlatformFee).coerceAtLeast(0.0)

    val finalBaseCommission = commissionBreakdown?.baseCommission ?: ((baseAmount * solverCommissionRate) / 100.0)
    val finalExtraCommission = commissionBreakdown?.extraCommission ?: ((extraAmount * solverCommissionRate) / 100.0)
    val finalTotalCommission = commissionBreakdown?.totalCommission ?: fallbackPlatformFee
    val netSolverEarnings = commissionBreakdown?.netAmount ?: fallbackNetSolverAmount
    val wasFreeQuota = commissionBreakdown?.wasFreeQuotaJob ?: (solverCommissionRate == 0.0)

    fun formatTimelineBengali(ts: Long?): String {
        if (ts == null || ts <= 0L) return "—"
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
        val formatted = sdf.format(Date(ts))
        val englishMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val banglaMonths = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
        var result = formatted
        for (i in englishMonths.indices) {
            result = result.replace(englishMonths[i], banglaMonths[i])
        }
        return DistanceUtil.toBengaliDigits(result)
    }

    BackHandler(onBack = onNavigateBack)

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
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("completion_summary_top_back_button")
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
                            text = if (isCancelled) "কাজ বাতিলের সামারি" else "কাজ সম্পন্ন হওয়ার সামারি",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "সমাধান (Somadhan) সার্ভিস রিসিট",
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
                    // Phase U: Receipt Download Button (Above 'ফিরে যান' button)
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
                            .testTag("completion_summary_receipt_button")
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
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isCancelled) SomadhanTextSecondary else SomadhanOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("completion_summary_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ফিরে যান",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Banner (Success or Cancelled)
            val bannerContainerColor = if (isCancelled) SomadhanErrorLight.copy(alpha = 0.45f) else SomadhanSuccessLight.copy(alpha = 0.45f)
            val bannerBorderColor = if (isCancelled) SomadhanError.copy(alpha = 0.3f) else SomadhanSuccess.copy(alpha = 0.3f)
            val bannerIconColor = if (isCancelled) SomadhanError else SomadhanSuccess
            val bannerIconVector = if (isCancelled) Icons.Default.Cancel else Icons.Default.Check
            val bannerTitleText = if (isCancelled) "কাজটি বাতিল করা হয়েছে ❌" else "কাজটি সফলভাবে সম্পন্ন হয়েছে! 🎉"

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = bannerContainerColor),
                border = BorderStroke(1.dp, bannerBorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("completion_success_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(bannerIconColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = bannerIconVector,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bannerTitleText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = bannerIconColor
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = problem.title.ifBlank { "জরুরি সমস্যা সমাধান" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = SomadhanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ক্যাটাগরি: ${problem.categoryName.ifBlank { "সাধারণ" }}",
                                fontSize = 11.5.sp,
                                color = SomadhanTextSecondary
                            )

                            // Post ID Chip (1-click copyable)
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
                                    .testTag("completion_summary_job_id_${problem.id}")
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
            }

            // 2. Stepper — All 4 Steps Done (✓)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "কাজের অগ্রগতি ধাপসমূহ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    TrackingProgressStepper(currentStepIdx = 5) // Step 5 means all 1..4 are complete
                }
            }

            // 3. Dynamic Timeline Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("completion_timeline_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "কাজের টাইমলাইন বিবরণ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val timelineItems = listOf(
                        Triple("পোস্ট হয়েছে", problem.createdAt, Icons.Default.Add),
                        Triple("বিড গৃহীত", problem.acceptedAt2 ?: problem.createdAt, Icons.Default.Check),
                        Triple("রওনা দিয়েছেন", problem.onWayAt ?: problem.acceptedAt2 ?: problem.createdAt, Icons.Default.LocationOn),
                        Triple("পৌঁছেছেন", problem.arrivedAt ?: problem.onWayAt ?: problem.createdAt, Icons.Default.Flag),
                        Triple("কাজ শুরু", problem.jobStartedAt ?: problem.arrivedAt ?: problem.createdAt, Icons.Default.Bolt),
                        Triple("কাজ সম্পন্ন হয়েছে", problem.completedAt ?: System.currentTimeMillis(), Icons.Default.CheckCircle)
                    )

                    timelineItems.forEachIndexed { index, (label, timestamp, icon) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(SomadhanSuccessLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = SomadhanSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                if (index < timelineItems.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(20.dp)
                                            .background(SomadhanSuccess.copy(alpha = 0.5f))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = if (index < timelineItems.size - 1) 12.dp else 0.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanTextPrimary
                                )
                                Text(
                                    text = formatTimelineBengali(timestamp),
                                    fontSize = 12.sp,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 4. Other Party's Profile Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("completion_profile_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isUserRole) "সমাধানকারীর তথ্য" else "সেবাগ্রহীতার তথ্য",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val name = if (isUserRole) {
                        problem.acceptedSolverName ?: otherUser?.name ?: "দক্ষ সমাধানকারী"
                    } else {
                        problem.userName.ifBlank { otherUser?.name ?: "সম্মানিত সেবাগ্রহীতা" }
                    }

                    val roleBadge = if (isUserRole) "দক্ষ সমাধানকারী" else "সেবাগ্রহীতা"
                    val rating = otherUser?.reputationScore?.let { String.format("%.1f", it) } ?: "৫.০"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isUserRole) SomadhanOrangeLight else SomadhanInfoLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(1).uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUserRole) SomadhanOrangePressed else SomadhanInfo
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SomadhanOrangeLight
                                ) {
                                    Text(
                                        text = roleBadge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanOrangePressed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isUserRole) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${DistanceUtil.toBengaliDigits(rating)} রেটিং",
                                        fontSize = 12.sp,
                                        color = SomadhanTextSecondary
                                    )
                                } else {
                                    Text(
                                        text = problem.userAddress.ifBlank { "বসিলা মেইন রোড, ঢাকা" },
                                        fontSize = 12.sp,
                                        color = SomadhanTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Final Payment Breakdown Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("completion_payment_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "চূড়ান্ত হিসাব ও পেমেন্ট বিবরণী",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }

                    HorizontalDivider(color = SomadhanDivider, thickness = 0.8.dp)

                    // Escrow ID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "এসক্রো ট্র্যাকিং ID:", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                        Text(
                            text = "#ESC_${problem.id.takeLast(8).uppercase()}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }

                    // Base Bid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "মূল বিড অ্যামাউন্ট:", fontSize = 13.sp, color = SomadhanTextSecondary)
                        Text(
                            text = "৳ ${DistanceUtil.toBengaliDigits(baseAmount)}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanTextPrimary
                        )
                    }

                    // Extra Amount (if any)
                    if (extraAmount > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "অতিরিক্ত বিল (অনুমোদিত):", fontSize = 13.sp, color = SomadhanTextSecondary)
                            Text(
                                text = "+ ৳ ${DistanceUtil.toBengaliDigits(extraAmount)}",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanOrangePressed
                            )
                        }
                    }

                    // Total Gross
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "মোট বিল:",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "৳ ${DistanceUtil.toBengaliDigits(totalGross)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }

                    // Solver View: Commission & Net Payout (Phase M & N)
                    if (!isUserRole) {
                        HorizontalDivider(color = SomadhanDivider, thickness = 0.8.dp)

                        if (extraAmount > 0.0) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (wasFreeQuota) "মূল বিডের কমিশন (ফ্রি কোটা):" else "মূল বিডের কমিশন (${DistanceUtil.toBengaliDigits(solverCommissionRate)}%):",
                                        fontSize = 13.sp,
                                        color = SomadhanTextSecondary
                                    )
                                    Text(
                                        text = if (wasFreeQuota) "৳ ০ (ফ্রি)" else "- ৳ ${DistanceUtil.toBengaliDigits(finalBaseCommission.toInt())}",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (wasFreeQuota) SomadhanSuccess else SomadhanError
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "অতিরিক্ত বিলের কমিশন (ছাড়সহ):",
                                        fontSize = 13.sp,
                                        color = SomadhanTextSecondary
                                    )
                                    Text(
                                        text = "- ৳ ${DistanceUtil.toBengaliDigits(finalExtraCommission.toInt())}",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanError
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "মোট প্ল্যাটফর্ম ফি:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanTextSecondary
                                    )
                                    Text(
                                        text = "- ৳ ${DistanceUtil.toBengaliDigits(finalTotalCommission.toInt())}",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanError
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (wasFreeQuota) "প্ল্যাটফর্ম ফি (ফ্রি কোটা ০%):" else "প্ল্যাটফর্ম ফি (${DistanceUtil.toBengaliDigits(solverCommissionRate)}%):",
                                    fontSize = 13.sp,
                                    color = SomadhanTextSecondary
                                )
                                Text(
                                    text = if (wasFreeQuota) "৳ ০ (ফ্রি)" else "- ৳ ${DistanceUtil.toBengaliDigits(finalTotalCommission.toInt())}",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (wasFreeQuota) SomadhanSuccess else SomadhanError
                                )
                            }
                        }

                        HorizontalDivider(color = SomadhanDivider, thickness = 0.8.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ওয়ালেটে মোট জমা (নিট আয়):",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanSuccess
                            )
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(netSolverEarnings.toInt())}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanSuccess
                            )
                        }
                    } else {
                        // User View: Only Escrow Released Total (NO COMMISSION SHOWN)
                        HorizontalDivider(color = SomadhanDivider, thickness = 0.8.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "মোট পরিশোধিত অর্থ (এসক্রো থেকে নিষ্পন্ন):",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanSuccess
                            )
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(totalGross)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanSuccess
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * DisputeResolutionSummaryScreen (Feature 3)
 *
 * Full-screen dispute settlement summary with progress checklist and admin verdict details.
 */
@Composable
fun DisputeResolutionSummaryScreen(
    problem: ProblemEntity,
    otherUser: UserEntity?,
    isUserRole: Boolean,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // ধাপগুলোর তালিকা — এগুলো disputeProgressAtSettlement অনুযায়ী ✅/❌ দেখাবে
    val steps = listOf(
        "ব্রডকাস্টিং / বিড গ্রহণ" to 1,
        "সমাধানকারী রওয়ানা হয়েছেন" to 2,
        "লোকেশনে পৌঁছেছেন" to 3,
        "কাজ শুরু হয়েছে" to 4,
        "কাজ সম্পন্ন" to 5
    )
    val reachedStep = problem.disputeProgressAtSettlement ?: problem.disputeProgressAtRaise ?: problem.calculateProgressStep()

    val (decisionTitle, decisionColor, decisionIcon) = when (problem.disputeResolutionDecision) {
        "RELEASE_TO_SOLVER" -> Triple("সমাধানকারীকে সম্পূর্ণ অর্থ রিলিজ করা হয়েছে", SomadhanSuccess, Icons.Default.CheckCircle)
        "REFUND_TO_USER" -> Triple("গ্রাহককে সম্পূর্ণ রিফান্ড করা হয়েছে", SomadhanError, Icons.AutoMirrored.Filled.Undo)
        "SPLIT_SETTLEMENT" -> Triple("৫০/৫০ হারে সমঝোতা করা হয়েছে", SomadhanInfo, Icons.Default.Balance)
        else -> Triple("বিরোধ নিষ্পত্তি হয়েছে", SomadhanTextSecondary, Icons.Default.Info)
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
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("dispute_summary_top_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = SomadhanTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "বিরোধ নিষ্পত্তির সামারি",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("dispute_summary_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ফিরে যান",
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
            // ১. সিদ্ধান্তের ব্যাজ ও পোস্ট টাইটেল কার্ড
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = decisionColor.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, decisionColor.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth().testTag("dispute_decision_banner")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(decisionColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = decisionIcon,
                                contentDescription = null,
                                tint = decisionColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "অ্যাডমিন সিদ্ধান্ত",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = decisionColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = decisionTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = decisionColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = decisionColor.copy(alpha = 0.18f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = problem.title.ifBlank { "জরুরি সমস্যা সমাধান" },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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

            // ২. ✅/❌ চেকলিস্ট
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                border = BorderStroke(1.dp, SomadhanBorder),
                modifier = Modifier.fillMaxWidth().testTag("dispute_progress_checklist")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "কাজের অগ্রগতি ও নিষ্পত্তিকালীন স্টেজ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = SomadhanTextPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    steps.forEach { (label, stepNum) ->
                        val done = stepNum <= reachedStep
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (done) SomadhanSuccess else SomadhanError,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = label,
                                fontSize = 13.5.sp,
                                fontWeight = if (done) FontWeight.Medium else FontWeight.Normal,
                                color = if (done) SomadhanTextPrimary else SomadhanTextHint
                            )
                        }
                    }
                }
            }

            // ৩. অ্যাডমিন নোট
            if (!problem.disputeResolutionNote.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    border = BorderStroke(1.dp, SomadhanBorder),
                    modifier = Modifier.fillMaxWidth().testTag("dispute_admin_note_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "অ্যাডমিনের মন্তব্য ও নিষ্পত্তি বিবরণ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = SomadhanTextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = problem.disputeResolutionNote ?: "",
                            fontSize = 13.sp,
                            color = SomadhanTextSecondary,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            // ৪. অপর পক্ষের প্রোফাইল সামারি
            if (otherUser != null) {
                val otherRoleBadge = if (isUserRole) "সমাধানকারী" else "গ্রাহক"
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    border = BorderStroke(1.dp, SomadhanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SomadhanOrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }
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
                                text = if (otherUser.phone.isBlank()) "ফোন নম্বর উপলব্ধ নেই" else Formatters.toLocalDisplayFormat(otherUser.phone),
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Phase AG: Dynamic Cancellation Summary Screen for both Solver and User
 */
@Composable
fun CancellationSummaryScreen(
    problem: ProblemEntity,
    cancelledBid: BidEntity?,
    isUserRole: Boolean,
    onNavigateBack: () -> Unit,
    onRebroadcast: () -> Unit,
    onFullyCancel: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val reachedStep = (cancelledBid?.progressAtCancel?.takeIf { it > 0 })
        ?: (problem.calculateProgressStep().takeIf { it > 1 })
        ?: 2

    val formattedTime = remember(problem.lastActivityAt) {
        val ts = problem.lastActivityAt ?: System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault())
        DistanceUtil.toBengaliDigits(sdf.format(Date(ts)))
    }

    val baseSteps = listOf(
        "১. জরুরি ব্রডকাস্ট ও সংযোগ" to 1,
        "২. সমাধানকারী রওয়ানা হয়েছেন" to 2,
        "৩. লোকেশনে পৌঁছেছেন" to 3,
        "৪. কাজ শুরু হয়েছে" to 4,
        "৫. কাজ সম্পন্ন" to 5
    ).map { (label, stepNum) ->
        val done = stepNum <= reachedStep
        CancellationStepItem(
            label = label,
            isDone = done,
            icon = if (done) Icons.Default.CheckCircle else Icons.Default.Cancel,
            iconTint = if (done) SomadhanSuccess else SomadhanError.copy(alpha = 0.5f)
        )
    }

    val hasDispute = problem.isDisputed || (problem.disputeSettledAt != null && problem.disputeSettledAt > 0L)
    val disputeStep = if (hasDispute) {
        listOf(
            CancellationStepItem(
                label = "৬. বিরোধ ও সমঝোতা ⚠️",
                isDone = true,
                icon = Icons.Default.Warning,
                iconTint = Color(0xFFF59E0B)
            )
        )
    } else {
        emptyList()
    }

    val finalStepNum = if (hasDispute) "৭" else "৬"
    val finalStep = CancellationStepItem(
        label = if (isUserRole) "$finalStepNum. সমাধানকারী কর্তৃক কাজ বাতিল ❌" else "$finalStepNum. আপনি বিডটি বাতিল করেছেন ❌",
        isDone = true,
        icon = Icons.Default.Cancel,
        iconTint = SomadhanError,
        isFinal = true
    )

    val allSteps = baseSteps + disputeStep + listOf(finalStep)

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
                    if (!isUserRole) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("cancellation_summary_top_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = SomadhanTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "বাতিল সারাংশ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
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
                    if (!isUserRole) {
                        // Solver: Only "ফিরে যান" button
                        Button(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("solver_cancel_summary_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ফিরে যান",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        // User: "🔄 আবার ব্রডকাস্টিং করুন" and "❌ বাতিল করুন"
                        Button(
                            onClick = onRebroadcast,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("user_rebroadcast_button")
                        ) {
                            Text(
                                text = "🔄 আবার ব্রডকাস্টিং করুন",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = onFullyCancel,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SomadhanError.copy(alpha = 0.1f),
                                contentColor = SomadhanError
                            ),
                            border = BorderStroke(1.dp, SomadhanError.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("user_fully_cancel_button")
                        ) {
                            Text(
                                text = "❌ সম্পূর্ণ বাতিল করুন",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanError
                            )
                        }
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
            // ১. হেডার ব্যানার কার্ড
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanError.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, SomadhanError.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SomadhanError.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            tint = SomadhanError,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUserRole) "সলভার কাজটি বাতিল করেছেন" else "আপনি কাজটি বাতিল করেছেন",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanError
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isUserRole) {
                                problem.solverCancelledNotice?.takeIf { it.isNotBlank() }
                                    ?: "সমাধানকারী আপনার পোস্টটির বিড বাতিল করেছেন। আপনি পুনরায় বিড নির্বাচন বা বাতিল করতে পারেন।"
                            } else {
                                "আপনি এই কাজটি বাতিল করেছেন। আপনি চাইলে জরুরি হাব থেকে পুনরায় বিড করতে পারেন।"
                            },
                            fontSize = 13.sp,
                            color = SomadhanTextPrimary,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "বাতিলের সময়: $formattedTime",
                            fontSize = 11.5.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                }
            }

            // ২. কাজের তথ্য কার্ড
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "কাজের বিবরণ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                    HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("কাজের শিরোনাম:", fontSize = 13.sp, color = SomadhanTextSecondary)
                        Text(
                            text = problem.title,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanTextPrimary,
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ক্যাটেগরি:", fontSize = 13.sp, color = SomadhanTextSecondary)
                        Text(
                            text = problem.categoryName,
                            fontSize = 13.sp,
                            color = SomadhanTextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("নির্ধারিত বাজেট:", fontSize = 13.sp, color = SomadhanTextSecondary)
                        Text(
                            text = "৳ ${DistanceUtil.toBengaliDigits(problem.maxBudget.toInt().toString())}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanTextPrimary
                        )
                    }

                    val contractAmount = cancelledBid?.amount ?: problem.acceptedAmount
                    if (contractAmount != null && contractAmount > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("চুক্তি মূল্য / বিড অ্যামাউন্ট:", fontSize = 13.sp, color = SomadhanTextSecondary)
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(contractAmount.toInt().toString())}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrange
                            )
                        }
                    }

                    if (cancelledBid != null) {
                        HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("বাতিলকারী সমাধানকারী:", fontSize = 13.sp, color = SomadhanTextSecondary)
                            Text(
                                text = cancelledBid.solverName.ifBlank { "সলভার" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanTextPrimary
                            )
                        }
                        if (cancelledBid.message.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("বিড মেসেজ / নোট:", fontSize = 13.sp, color = SomadhanTextSecondary)
                                Text(
                                    text = cancelledBid.message,
                                    fontSize = 12.5.sp,
                                    color = SomadhanTextPrimary,
                                    modifier = Modifier.widthIn(max = 200.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ৩. ডাইনামিক স্টেপ চেকলিস্ট কার্ড
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "কাজের অগ্রগতি ও বাতিলকালীন স্টেজ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }
                    Text(
                        text = "বাতিল হওয়ার পূর্বমুহূর্ত পর্যন্ত সম্পন্ন ধাপসমূহ:",
                        fontSize = 12.5.sp,
                        color = SomadhanTextSecondary
                    )
                    HorizontalDivider(color = SomadhanBorder.copy(alpha = 0.5f))

                    allSteps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = step.iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step.label,
                                fontSize = if (step.isFinal) 14.sp else 13.sp,
                                fontWeight = if (step.isDone || step.isFinal) FontWeight.Bold else FontWeight.Normal,
                                color = if (step.isFinal) SomadhanError else if (step.isDone) SomadhanTextPrimary else SomadhanError.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // ৪. সহায়ক বার্তা কার্ড
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUserRole) SomadhanOrangeLight else SomadhanCardBg
                ),
                border = BorderStroke(
                    1.dp,
                    if (isUserRole) SomadhanOrange.copy(alpha = 0.3f) else SomadhanBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isUserRole) SomadhanOrange else SomadhanTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isUserRole)
                            "আপনার এসক্রো অর্থ সম্পূর্ণ সুরক্ষিত রয়েছে এবং আপনার ওয়ালেটে ফেরত দেওয়া হয়েছে। আপনি নিচে থেকে পুনরায় ব্রডকাস্টিং চালু করে অন্য সমাধানকারীর বিড গ্রহণ করতে পারেন অথবা পোস্টটি সম্পূর্ণ বাতিল করতে পারেন।"
                        else
                            "পোস্টটি পুনরায় উন্মুক্ত করা হয়েছে। সমাধানকারী হিসেবে আপনি জরুরি হাব থেকে অন্য কোনো নতুন কাজে অংশ নিতে পারবেন।",
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = if (isUserRole) SomadhanOrangePressed else SomadhanTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private data class CancellationStepItem(
    val label: String,
    val isDone: Boolean,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconTint: Color,
    val isFinal: Boolean = false
)
