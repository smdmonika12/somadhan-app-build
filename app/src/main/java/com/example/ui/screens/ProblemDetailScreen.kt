package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Star
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.AdditionalChargeEntity
import com.example.data.entity.BidEntity
import com.example.data.entity.GatewayPaymentEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import com.example.ui.components.AccountStatusIndicator
import com.example.ui.components.DetailScreenSkeleton
import com.example.ui.components.ExtraAmountPaymentConfirmationDialog
import com.example.ui.components.MerchantPaymentDialog
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.StatusBadge
import com.example.ui.components.SyncAwareContent
import com.example.ui.components.PulsingValue
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.components.UrgencyBadge
import com.example.ui.components.UserVerificationBadge
import com.example.ui.components.YellowVerifiedBadge
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.UserAvatar
import com.example.ui.navigation.Screen
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanInfoLight
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.theme.SomadhanYellowVerified
import com.example.ui.viewmodel.BidBlockReason
import com.example.ui.viewmodel.BidEligibility
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.Formatters
import com.example.ui.components.BottomSlideAlertDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemDetailScreen(
    problemId: String,
    readOnly: Boolean = false,
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onNavigateToKyc: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val problem by viewModel.selectedProblem.collectAsStateWithLifecycle()
    val bids by viewModel.problemBids.collectAsStateWithLifecycle()
    val escrow by remember(problemId) { viewModel.getEscrowForProblem(problemId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val pendingCharges by remember(problemId) { viewModel.getPendingAdditionalCharges(problemId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val solverGivenReviews by viewModel.solverGivenReviews.collectAsStateWithLifecycle()
    val userGivenReviews by viewModel.userGivenReviews.collectAsStateWithLifecycle()
    val physicalRadius by viewModel.physicalCategoryRadiusKm.collectAsStateWithLifecycle()
    val platformCommissionPercent by viewModel.platformCommissionPercent.collectAsStateWithLifecycle()
    val userGatewayPayments by viewModel.userGatewayPayments.collectAsStateWithLifecycle()
    val problemGatewayPayments = remember(userGatewayPayments, problemId) {
        userGatewayPayments.filter { it.problemId == problemId }.sortedByDescending { it.timestamp }
    }
    var selectedGatewayReceiptDetail by remember { mutableStateOf<GatewayPaymentEntity?>(null) }
    // Wallet+Gateway split support for "অতিরিক্ত বিল" (ADDITIONAL_CHARGE) gateway payments:
    // when the wallet balance covered part of a confirmed extra-amount request and the gateway
    // covered the rest, pair each such gateway payment with its EXTRA_CHARGE_DEDUCTION trx
    // (nearest timestamp -- there's no direct foreign key between the two records) so the wallet
    // portion (trx amount - gateway amount) can be shown alongside the gateway amount.
    val userTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    val problemExtraChargeTrxs = remember(userTransactions, problemId) {
        userTransactions.filter { it.problemId == problemId && it.type.equals("EXTRA_CHARGE_DEDUCTION", ignoreCase = true) }
    }
    fun walletPortionFor(gw: GatewayPaymentEntity): Double {
        if (!gw.purpose.equals("ADDITIONAL_CHARGE", ignoreCase = true)) return 0.0
        val matchedTrx = problemExtraChargeTrxs.minByOrNull { kotlin.math.abs(it.timestamp - gw.timestamp) } ?: return 0.0
        return (matchedTrx.grossAmount - gw.amount).coerceAtLeast(0.0)
    }

    val unreadDetailChatCount by remember(problemId, currentUser?.id) {
        viewModel.getMessagesForProblem(problemId).map { messages ->
            val uid = currentUser?.id ?: ""
            messages.count { msg ->
                !msg.isRead && (msg.receiverId == uid || (msg.senderId != uid && msg.receiverId.isEmpty()))
            }
        }
    }.collectAsStateWithLifecycle(initialValue = 0)

    var showCompleteDialog by remember { mutableStateOf(false) }
    var bidToAcceptWithWalletConfirm by remember { mutableStateOf<BidEntity?>(null) }
    var additionalChargeToPay by remember { mutableStateOf<AdditionalChargeEntity?>(null) }
    var additionalChargeGatewayPayment by remember { mutableStateOf<Pair<AdditionalChargeEntity, Double>?>(null) }
    var isSubmittingAdditionalCharge by remember { mutableStateOf(false) }
    var additionalChargeError by remember { mutableStateOf<String?>(null) }
    var additionalChargeGatewayPending by remember { mutableStateOf<AdditionalChargeEntity?>(null) }
    var additionalChargeGatewayError by remember { mutableStateOf<String?>(null) }
    var releaseExtraAmountToPay by remember { mutableStateOf<Double?>(null) }
    // Loading-lock for the release-request extra-amount confirm dialog (wallet path) -- true
    // from tap until confirmReleaseAndComplete()'s onSuccess/onError actually returns.
    var isConfirmingReleaseExtraAmount by remember { mutableStateOf(false) }
    var releaseExtraAmountError by remember { mutableStateOf<String?>(null) }
    var releaseExtraAmountGatewayPending by remember { mutableStateOf(false) }
    var releaseExtraAmountGatewayError by remember { mutableStateOf<String?>(null) }
    var extraAmountForGatewayPayment by remember { mutableStateOf<Double?>(null) }

    var userRatingStars by remember { mutableIntStateOf(0) }
    var userReviewComment by remember { mutableStateOf("") }

    var solverRatingStars by remember { mutableIntStateOf(0) }
    var solverReviewComment by remember { mutableStateOf("") }

    var showAddChargeDialog by remember { mutableStateOf(false) }
    var chargeReason by remember { mutableStateOf("") }
    var chargeAmountStr by remember { mutableStateOf("") }
    var chargeError by remember { mutableStateOf<String?>(null) }
    var isSubmittingCharge by remember { mutableStateOf(false) }

    var bidAmountStr by remember { mutableStateOf("") }
    var bidMessage by remember { mutableStateOf("") }
    var bidTimeValue by remember { mutableStateOf("") }
    var bidTimeUnit by remember { mutableStateOf("ঘন্টা") }
    var bidTimeUnitExpanded by remember { mutableStateOf(false) }
    var bidError by remember { mutableStateOf<String?>(null) }
    var isPlacingBid by remember { mutableStateOf(false) }
    var isCheckingProblem by remember(problemId) { mutableStateOf(true) }
    var showUserDeleteProblemDialog by remember { mutableStateOf(false) }
    var isDeletingProblemDetail by remember { mutableStateOf(false) }
    var deleteProblemDetailError by remember { mutableStateOf<String?>(null) }
    var canFavoriteSolverState by remember(problem?.acceptedSolverId) { mutableStateOf(false) }
    var showAddToFavoritesDialog by remember { mutableStateOf(false) }

    // Generic loading-lock for one-shot action buttons on this screen (accept bid, solver
    // cancel, extra-bill accept/reject, dispute raise/withdraw, etc). Each button sets this to
    // its own unique id right before calling the ViewModel, disables itself and shows a spinner
    // while processingActionId == its id, and clears it back to null from the action's
    // onSuccess/onError callback. This exists to close the exact gap the app has been hitting:
    // these actions had no isProcessing state at all, so a double-tap (easy to do on a slow
    // network, which is exactly when people tend to tap twice) could fire the same mutation
    // twice before the first one's result came back and the button visually updated.
    var processingActionId by remember { mutableStateOf<String?>(null) }
    var withdrawDisputeError by remember { mutableStateOf<String?>(null) }
    var cancelReleaseRequestError by remember { mutableStateOf<String?>(null) }
    var directContractProposalError by remember { mutableStateOf<String?>(null) }
    var rejectChargeError by remember { mutableStateOf<Pair<String, String>?>(null) }
    var acceptBidError by remember { mutableStateOf<String?>(null) }
    var acceptBidGatewayPendingBid by remember { mutableStateOf<BidEntity?>(null) }
    var acceptBidGatewayError by remember { mutableStateOf<String?>(null) }
    // Safety-valve: if a click's onSuccess/onError never arrives (e.g. process death, a hung
    // request that isn't wrapped in a timeout further down the stack), don't leave the button
    // locked forever -- release it after 20s so the user can retry.
    LaunchedEffect(processingActionId) {
        if (processingActionId != null) {
            delay(20000)
            processingActionId = null
        }
    }

    LaunchedEffect(problem?.acceptedSolverId, problem?.status, problem?.isPhysical, currentUser?.id) {
        val solverId = problem?.acceptedSolverId
        if (!solverId.isNullOrBlank() && currentUser != null && solverId != currentUser?.id) {
            val canFav = viewModel.canFavoriteSolver(solverId) || (problem?.status == "COMPLETED" && problem?.isPhysical == false)
            canFavoriteSolverState = canFav
        } else {
            canFavoriteSolverState = false
        }
    }

    LaunchedEffect(problemId) {
        isCheckingProblem = true
        viewModel.selectProblem(problemId)
        delay(1200)
        isCheckingProblem = false
        // Realtime Scoping ফিক্স, ধাপ ৪ (dual-run) — এই problemId-র `problem:<id>:bids`
        // broadcast channel join করা, পুরনো টেবিল-ওয়াইড bids subscription-এর পাশাপাশি
        // (প্রতিস্থাপন না)। এই স্ক্রিন যেকোনো ভিজিটর (শুধু owner না, উপরের ✅ [ধাপ ২৩] কমেন্ট
        // দেখুন) দেখতে পারেন বলে RLS policy-ও bids_select-এর broad visibility অনুসরণ করে -- Firebase-
        // parity বজায় রাখা হয়েছে।
        viewModel.joinProblemBidsBroadcast(problemId)
    }

    DisposableEffect(problemId) {
        onDispose {
            // [joinProblemBidsBroadcast]-এর কাউন্টারপার্ট — screen ছাড়ার সময় unsubscribe
            // (memory/connection leak এড়াতে, ChatScreen-এর ধাপ ৩-প্যাটার্নের মতোই)।
            viewModel.leaveProblemBidsBroadcast(problemId)
        }
    }

    LaunchedEffect(problemId, currentUser?.id, currentUser?.role, problem?.userId, problem?.acceptedSolverId) {
        val role = when {
            currentUser?.id == problem?.userId -> "USER"
            currentUser?.id == problem?.acceptedSolverId -> "SOLVER"
            currentUser?.role == "SOLVER" -> "SOLVER"
            else -> "USER"
        }
        viewModel.markProblemSeen(problemId, role)
    }

    val currentProblem = problem
    val isOwner = currentProblem?.userId == currentUser?.id
    val isSolver = currentUser?.role == "SOLVER"
    // এই solver-এর এই পোস্টে করা বিড বাতিল/প্রত্যাখ্যাত হয়ে গেছে কিনা এবং সে বর্তমান accepted_solver
    // না -- হলে নিচে একটা নোটিশ দেখানো হবে (RLS-এ এখন আর এই পোস্টের লাইভ আপডেট এই solver-এর কাছে
    // পৌঁছায় না, তাই এখানে যা দেখাচ্ছে তা পুরনো/স্থবির তথ্য হতে পারে)।
    val myEndedBid = remember(bids, currentUser?.id, currentUser?.role, currentProblem?.acceptedSolverId) {
        if (currentUser?.role == "SOLVER" && currentProblem != null && currentProblem.acceptedSolverId != currentUser?.id) {
            bids.filter { it.solverId == currentUser?.id }
                .firstOrNull { it.status == "CANCELLED" || it.status == "WITHDRAWN" || it.status == "REJECTED" }
        } else null
    }
    val isUser = currentUser?.role != "SOLVER"
    val isDirectContractParty = isOwner || currentProblem?.acceptedSolverId == currentUser?.id || currentUser?.role == "ADMIN"
    val isRestrictedDirectContract = currentProblem?.isDirectContract == true && !isDirectContractParty
    val brandColor = if (isUser) Color(0xFF1D4ED8) else SomadhanOrange
    val brandLight = if (isUser) Color(0xFFEFF6FF) else SomadhanOrangeLight

    // CORRECTED: the bid list ("সকল বিড") below is visible to ANY visitor of this post, not just
    // the owner -- isOwner only gates the accept-bid action button (isProblemOwner passed to
    // BidCard), not the list's visibility (see the plain `if (!currentProblem.isDirectContract)`
    // gate around bidsToDisplay). So this screen-scoped live listener must fire for anyone
    // viewing this problem, not only its owner -- otherwise a solver or public visitor browsing
    // an open post would see bids arrive late instead of instantly, same as the owner does.
    // ✅ [ধাপ ২৩ — RLS gap resolved, ব্যবহারকারীর স্পষ্ট নির্দেশে "Firebase-এর মতোই সম্পূর্ণ
    // ফাংশনালিটি চাই"] এই স্ক্রিনে বিড-লিস্ট যেকোনো visitor-কে (শুধু owner না, কমেন্ট উপরে দেখুন)
    // দেখানো হয় -- প্রথমে `bids_select` RLS policy শুধু নিজের বিড/admin/পোস্টদাতাকে দেখতে দিতো,
    // ফলে তৃতীয়-পক্ষ visitor Supabase পাশ থেকে অন্য সলভারদের বিড দেখতে পেতেন না (Firebase-এর
    // সাথে অসামঞ্জস্যপূর্ণ)। এই session-এ Supabase MCP দিয়ে সরাসরি `bids_select` policy-তে
    // `problems_select`-এর "OPEN + is_public + not is_user_deleted" ভিজিটর-ক্লজের সাথে
    // সঙ্গতিপূর্ণ একটা নতুন OR-শর্ত apply করা হয়েছে (দেখুন `supabase/migrations/
    // step23_bids_select_open_public_visibility.sql`, apply-পরবর্তী verify-ও করা হয়েছে) --
    // ফলে এখন Supabase পাশও Firebase-এর মতোই যেকোনো ভিজিটরকে OPEN+public problem-এর সব বিড
    // দেখায়। এখানে নতুন কোনো Kotlin কোড লাগেনি -- `SupabaseRealtimeManager`-এর গ্লোবাল bids
    // channel (ধাপ ২০-২২, session শুরুতেই চালু) এখন থেকে এই ভিজিটরদের জন্যও লাইভ বিড Room-এ
    // upsert করবে, আর উপরের `viewModel.problemBids` সেই একই Room টেবিল থেকে read করে। বিস্তারিত:
    // `MIGRATION_PROGRESS.md`-এর "ধাপ ২৩" এন্ট্রি।
    val brandBorder = if (isUser) Color(0xFFBFDBFE) else SomadhanOrange.copy(alpha = 0.4f)
    val isKycVerified = currentUser?.isKycVerified == true
    val userBid = bids.find { it.solverId == currentUser?.id && it.status != "CANCELLED" }

    var showReleaseRequestDialog by remember { mutableStateOf(false) }
    var releaseExtraAmountStr by remember { mutableStateOf("") }
    var releaseNote by remember { mutableStateOf("") }
    var releaseError by remember { mutableStateOf<String?>(null) }
    var isSubmittingReleaseRequest by remember { mutableStateOf(false) }

    var showConfirmReleaseDialog by remember { mutableStateOf(false) }
    var includeExtraAmountInRelease by remember { mutableStateOf(true) }
    var isConfirmingRelease by remember { mutableStateOf(false) }
    var confirmReleaseError by remember { mutableStateOf<String?>(null) }

    var showDisputeDialog by remember { mutableStateOf(false) }
    var disputeReasonInput by remember { mutableStateOf("") }
    var isSubmittingDispute by remember { mutableStateOf(false) }
    var disputeError by remember { mutableStateOf<String?>(null) }

    var showSolverCancelJobDialog by remember { mutableStateOf(false) }
    var solverCancelReasonInput by remember { mutableStateOf("") }
    var isSubmittingSolverCancel by remember { mutableStateOf(false) }
    var solverCancelJobError by remember { mutableStateOf<String?>(null) }

    var showSolverCancelAcceptedBidDialog by remember { mutableStateOf(false) }
    var isSubmittingSolverCancelAcceptedBid by remember { mutableStateOf(false) }
    var solverCancelAcceptedBidError by remember { mutableStateOf<String?>(null) }

    // Solver Release Request Dialog
    if (showReleaseRequestDialog && currentProblem != null) {
        val baseAmt = currentProblem.acceptedAmount ?: 0.0
        val extraAmt = releaseExtraAmountStr.toDoubleOrNull() ?: 0.0
        val totalRequested = baseAmt + extraAmt
        val commissionRate = platformCommissionPercent / 100.0
        val commission = totalRequested * commissionRate
        val netEarnings = totalRequested - commission

        BottomSlideAlertDialog(
            onDismissRequest = {
                if (!isSubmittingReleaseRequest) {
                    showReleaseRequestDialog = false
                    releaseError = null
                }
            },
            title = {
                Text(
                    text = "কাজ সম্পন্ন ও অর্থ রিলিজের অনুরোধ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "কাজ সফলভাবে সম্পন্ন করে থাকলে ব্যবহারকারীর নিকট অর্থ রিলিজের জন্য অনুরোধ পাঠান। কাজের অতিরিক্ত কোনো খরচ বা বিল থাকলে তা এখানে উল্লেখ করতে পারেন।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Base Agreed Amount Info
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanBorder, RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("মূল চুক্তি (Bid Amount):", fontSize = 12.sp, color = SomadhanTextSecondary)
                            Text(
                                "৳ ${DistanceUtil.toBengaliDigits(baseAmt.toInt().toString())}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = releaseExtraAmountStr,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                releaseExtraAmountStr = input
                                releaseError = null
                            }
                        },
                        label = { Text("অতিরিক্ত বিল/খরচ (ঐচ্ছিক, ৳)") },
                        placeholder = { Text("যেমন: ২০০ (প্রযোজ্য হলে)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("release_extra_amount_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = releaseNote,
                        onValueChange = { releaseNote = it },
                        label = { Text("কাজের বিবরণ বা নোট (ঐচ্ছিক)") },
                        placeholder = { Text("যেমন: কাজটি সফলভাবে শেষ করেছি...") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("release_note_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Calculation Summary Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("মোট রিলিজের দাবি:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                                Text(
                                    "৳ ${DistanceUtil.toBengaliDigits(totalRequested.toInt().toString())}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val netPctBengali = DistanceUtil.toBengaliDigits(((100.0 - platformCommissionPercent).coerceAtLeast(0.0)).toInt().toString())
                                Text("কমিশন বাদে আপনার আয় (${netPctBengali}%):", fontSize = 11.sp, color = SomadhanTextSecondary)
                                Text(
                                    "৳ ${DistanceUtil.toBengaliDigits(netEarnings.toInt().toString())}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanSuccess
                                )
                            }
                        }
                    }

                    if (releaseError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = releaseError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingReleaseRequest = true
                        viewModel.requestJobRelease(
                            problem = currentProblem,
                            extraAmount = extraAmt,
                            note = releaseNote.trim(),
                            onSuccess = {
                                isSubmittingReleaseRequest = false
                                showReleaseRequestDialog = false
                                releaseExtraAmountStr = ""
                                releaseNote = ""
                                releaseError = null
                            },
                            onError = { err ->
                                isSubmittingReleaseRequest = false
                                releaseError = err
                            }
                        )
                    },
                    enabled = !isSubmittingReleaseRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    modifier = Modifier.testTag("submit_release_request_button")
                ) {
                    if (isSubmittingReleaseRequest) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পাঠানো হচ্ছে...")
                    } else {
                        Text("অনুরোধ পাঠান")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReleaseRequestDialog = false
                        releaseError = null
                    },
                    enabled = !isSubmittingReleaseRequest
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // User Confirm Release & Complete Dialog
    if (showConfirmReleaseDialog && currentProblem != null) {
        val baseAmt = currentProblem.acceptedAmount ?: 0.0
        val extraAmt = currentProblem.releaseRequestExtraAmount
        val finalAmount = baseAmt + (if (includeExtraAmountInRelease) extraAmt else 0.0)

        BottomSlideAlertDialog(
            onDismissRequest = {
                if (!isConfirmingRelease) {
                    showConfirmReleaseDialog = false
                }
            },
            title = {
                Text(
                    text = "অর্থ রিলিজ ও কাজ সম্পন্ন নিশ্চিতকরণ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "সমাধানকারী কাজ সম্পন্ন করে অর্থ রিলিজের অনুরোধ পাঠিয়েছেন। রিলিজ নিশ্চিত করলে সমাধানকারীর ওয়ালেটে অর্থ পৌঁছে যাবে এবং কাজটি সম্পন্ন চিহ্নিত হবে।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Solver & Base Contract Info
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanBorder, RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("সমাধানকারী:", fontSize = 12.sp, color = SomadhanTextSecondary)
                                Text(
                                    currentProblem.acceptedSolverName ?: "সলভার",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("মূল চুক্তির পরিমাণ:", fontSize = 12.sp, color = SomadhanTextSecondary)
                                Text(
                                    "৳ ${DistanceUtil.toBengaliDigits(baseAmt.toInt().toString())}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                            }
                        }
                    }

                    // Extra Amount Option (if solver requested extra)
                    if (extraAmt > 0.0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = SomadhanOrangeLight.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    SomadhanOrange,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "অতিরিক্ত বিল: ৳ ${DistanceUtil.toBengaliDigits(extraAmt.toInt().toString())}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanOrange
                                        )
                                        if (currentProblem.releaseRequestNote.isNotBlank()) {
                                            Text(
                                                text = "নোট: \"${currentProblem.releaseRequestNote}\"",
                                                fontSize = 11.sp,
                                                color = SomadhanTextSecondary
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ℹ️ সমাধানকারী রিলিজের সাথে অতিরিক্ত বিলের অনুরোধ করেছেন। আপনার ওয়ালেট ব্যালেন্স থাকলে সেখান থেকে কেটে অথবা পেমেন্ট গেটওয়ের মাধ্যমে অতিরিক্ত অর্থ এসক্রোতে পরিশোধ করে রিলিজ সম্পন্ন করুন। আপত্তি থাকলে বিরোধ (Dispute) ওপেন করুন।",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanOrange,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Final Total Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanSuccessLight),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanSuccess.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("মোট রিলিজের পরিমাণ:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                            Text(
                                "৳ ${DistanceUtil.toBengaliDigits(finalAmount.toInt().toString())}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Star Rating (1-5)
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
                                onClick = { userRatingStars = star },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (star <= userRatingStars) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = "$star star",
                                    tint = if (star <= userRatingStars) SomadhanYellowVerified else SomadhanTextHint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    if (userRatingStars > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = userReviewComment,
                            onValueChange = { userReviewComment = it },
                            label = { Text("মতামত বা রিভিউ (ঐচ্ছিক)") },
                            placeholder = { Text("যেমন: চমৎকার ও দ্রুত কাজ করেছেন...") },
                            maxLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SomadhanSuccess,
                                unfocusedBorderColor = SomadhanBorder,
                                focusedContainerColor = SomadhanBg,
                                unfocusedContainerColor = SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (confirmReleaseError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = confirmReleaseError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (extraAmt > 0.0) {
                            showConfirmReleaseDialog = false
                            releaseExtraAmountToPay = extraAmt
                        } else {
                            isConfirmingRelease = true
                            confirmReleaseError = null
                            viewModel.confirmReleaseAndComplete(
                                problem = currentProblem,
                                includeExtraAmount = false,
                                stars = userRatingStars,
                                reviewComment = userReviewComment,
                                onSuccess = {
                                    isConfirmingRelease = false
                                    showConfirmReleaseDialog = false
                                    val solverId = currentProblem.acceptedSolverId
                                    val isVirtual = !currentProblem.isPhysical
                                    val isAlreadyFav = currentUser?.favoriteSolverIds?.split(",")?.map { it.trim() }?.contains(solverId) == true
                                    if (isVirtual && !solverId.isNullOrBlank()) {
                                        if (!isAlreadyFav) {
                                            showAddToFavoritesDialog = true
                                        }
                                    }
                                },
                                onError = { err ->
                                    isConfirmingRelease = false
                                    confirmReleaseError = err
                                }
                            )
                        }
                    },
                    enabled = !isConfirmingRelease,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                    modifier = Modifier.testTag("confirm_complete_and_release_button")
                ) {
                    if (isConfirmingRelease) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("প্রসেস হচ্ছে...")
                    } else {
                        Text(if (extraAmt > 0.0) "অতিরিক্ত বিল পরিশোধ ও রিলিজ নিশ্চিত করুন" else "রিলিজ ও সম্পন্ন করুন")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmReleaseDialog = false
                        confirmReleaseError = null
                    },
                    enabled = !isConfirmingRelease
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // User Raise Dispute Dialog
    if (showDisputeDialog && currentProblem != null) {
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
                                problem = currentProblem,
                                reason = disputeReasonInput.trim(),
                                onSuccess = {
                                    isSubmittingDispute = false
                                    showDisputeDialog = false
                                    disputeReasonInput = ""
                                    onOpenChat(currentProblem.id)
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

    // Solver Cancel Job Dialog (Full refund to user)
    if (showSolverCancelJobDialog && currentProblem != null) {
        BottomSlideAlertDialog(
            onDismissRequest = {
                if (!isSubmittingSolverCancel) {
                    showSolverCancelJobDialog = false
                    solverCancelReasonInput = ""
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = SomadhanError, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "কাজটি বাতিল নিশ্চিতকরণ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = SomadhanTextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "আপনি কি নিশ্চিত যে এই কাজটি বাতিল করতে চান? কাজটি বাতিল করলে এসক্রোর সম্পূর্ণ অর্থ সরাসরি গ্রাহকের ওয়ালেটে রিফান্ড হয়ে যাবে এবং সমস্যাটি পুনরায় উন্মুক্ত (OPEN) হবে।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = solverCancelReasonInput,
                        onValueChange = { solverCancelReasonInput = it },
                        label = { Text("বাতিল করার কারণ (ঐচ্ছিক)") },
                        placeholder = { Text("যেমন: ব্যক্তিগত কারণে কাজটি সম্পন্ন করতে পারছি না...") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanError,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("solver_cancel_reason_input")
                    )

                    if (solverCancelJobError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = solverCancelJobError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingSolverCancel = true
                        solverCancelJobError = null
                        viewModel.solverCancelJob(
                            problem = currentProblem,
                            reason = solverCancelReasonInput.trim(),
                            onSuccess = {
                                isSubmittingSolverCancel = false
                                showSolverCancelJobDialog = false
                                solverCancelReasonInput = ""
                                onNavigate(com.example.ui.navigation.Screen.InstantJobs.route)
                            },
                            onError = { err ->
                                isSubmittingSolverCancel = false
                                solverCancelJobError = err
                            }
                        )
                    },
                    enabled = !isSubmittingSolverCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    modifier = Modifier.testTag("confirm_solver_cancel_job_button")
                ) {
                    if (isSubmittingSolverCancel) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("বাতিল হচ্ছে...")
                    } else {
                        Text("কাজ বাতিল করুন")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSolverCancelJobDialog = false
                        solverCancelReasonInput = ""
                        solverCancelJobError = null
                    },
                    enabled = !isSubmittingSolverCancel
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // Solver Cancel Accepted Bid Confirmation Dialog
    if (showSolverCancelAcceptedBidDialog && currentProblem != null) {
        val matchingAcceptedBid = bids.firstOrNull { it.id == currentProblem.acceptedBidId || it.solverId == currentUser?.id } ?: userBid
        BottomSlideAlertDialog(
            onDismissRequest = {
                if (!isSubmittingSolverCancelAcceptedBid) {
                    showSolverCancelAcceptedBidDialog = false
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = SomadhanError,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "বিড বাতিল নিশ্চিতকরণ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SomadhanTextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "আপনি কি নিশ্চিত যে এই কাজের গৃহীত বিডটি বাতিল করতে চান?",
                        fontSize = 14.sp,
                        color = SomadhanTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SomadhanErrorLight)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "⚠️ বিড বাতিল করলে এটি আপনার রেপুটেশন স্কোর কমিয়ে দেবে (-৩.০ পয়েন্ট) এবং পোস্টটি পুনরায় অন্যান্য সমাধানকারীদের জন্য উন্মুক্ত হয়ে যাবে। তবুও কি বাতিল করতে চান?",
                            fontSize = 12.sp,
                            color = SomadhanError,
                            lineHeight = 17.sp
                        )
                    }
                    if (solverCancelAcceptedBidError != null) {
                        Text(
                            text = solverCancelAcceptedBidError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (matchingAcceptedBid != null) {
                            isSubmittingSolverCancelAcceptedBid = true
                            solverCancelAcceptedBidError = null
                            viewModel.solverCancelAcceptedJob(
                                problem = currentProblem,
                                bid = matchingAcceptedBid,
                                onSuccess = {
                                    isSubmittingSolverCancelAcceptedBid = false
                                    showSolverCancelAcceptedBidDialog = false
                                    onNavigate(com.example.ui.navigation.Screen.InstantJobs.route)
                                },
                                onError = { err ->
                                    // Bug fix: this button had no failure path before -- if the
                                    // repository call threw, isSubmittingSolverCancelAcceptedBid
                                    // stayed true forever and the button was permanently disabled
                                    // until the screen was left and re-entered.
                                    isSubmittingSolverCancelAcceptedBid = false
                                    solverCancelAcceptedBidError = err
                                }
                            )
                        } else {
                            showSolverCancelAcceptedBidDialog = false
                        }
                    },
                    enabled = !isSubmittingSolverCancelAcceptedBid,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_solver_cancel_accepted_bid_button")
                ) {
                    if (isSubmittingSolverCancelAcceptedBid) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("বাতিল হচ্ছে...")
                    } else {
                        Text("হ্যাঁ, বাতিল করুন", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSolverCancelAcceptedBidDialog = false
                        solverCancelAcceptedBidError = null
                    },
                    enabled = !isSubmittingSolverCancelAcceptedBid
                ) {
                    Text("ফিরে যান", color = SomadhanTextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    var bidToAcceptGatewayDialog by remember { mutableStateOf<Pair<BidEntity, Double>?>(null) }

    // Wallet + Escrow Confirmation Dialog before Accepting Bid
    if (bidToAcceptWithWalletConfirm != null && currentProblem != null) {
        val selectedBid = bidToAcceptWithWalletConfirm!!
        // [ব্যালেন্স ফিক্স — ধাপ ১] bid-accept সংক্রান্ত সব ব্যালেন্স-চেক owner/USER-role
        // ফ্লো (এই ব্যক্তি এই মুহূর্তে সবসময় USER role-এ), তাই role-scoped balanceUser পড়া
        // হচ্ছে, শেয়ার্ড `balance` না (MONEY_FLOW_AND_ADMIN_BUGS_MASTER_PROMPT.md, বাগ A)।
        val userBalance = currentUser?.balanceUser ?: 0.0
        val walletDeduction = minOf(userBalance, selectedBid.amount)
        val remainingToPay = maxOf(0.0, selectedBid.amount - walletDeduction)
        val isFullWalletPay = remainingToPay <= 0.0

        BottomSlideAlertDialog(
            onDismissRequest = { bidToAcceptWithWalletConfirm = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = brandColor,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "বিড গ্রহণ ও এসক্রো জমা",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "আপনি কি ${selectedBid.solverName}-এর ৳${DistanceUtil.toBengaliDigits(selectedBid.amount.toInt().toString())} মূল্যের বিডটি গ্রহণ করতে চান?",
                        fontSize = 14.sp,
                        color = SomadhanTextPrimary,
                        fontWeight = FontWeight.Medium
                    )

                    // Financial Summary Breakdown Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanBorder, RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("বিড মূল্য:", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                                Text("৳ ${DistanceUtil.toBengaliDigits(selectedBid.amount.toInt().toString())}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("আপনার ওয়ালেট ব্যালেন্স:", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                                Text("৳ ${DistanceUtil.toBengaliDigits(userBalance.toInt().toString())}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (userBalance > 0.0) SomadhanSuccess else SomadhanTextSecondary)
                            }
                            if (walletDeduction > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ওয়ালেট থেকে কর্তন:", fontSize = 12.5.sp, color = SomadhanSuccess)
                                    Text("- ৳ ${DistanceUtil.toBengaliDigits(walletDeduction.toInt().toString())}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanSuccess)
                                }
                            }
                            Divider(color = SomadhanDivider, thickness = 0.8.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isFullWalletPay) "ওয়ালেট অবশিষ্ট ব্যালেন্স:" else "পেমেন্ট গেটওয়েতে প্রদেয়:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFullWalletPay) SomadhanTextPrimary else SomadhanOrange
                                )
                                Text(
                                    if (isFullWalletPay) "৳ ${DistanceUtil.toBengaliDigits((userBalance - walletDeduction).toInt().toString())}" else "৳ ${DistanceUtil.toBengaliDigits(remainingToPay.toInt().toString())}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFullWalletPay) SomadhanSuccess else SomadhanOrange
                                )
                            }
                        }
                    }

                    if (isFullWalletPay) {
                        Text(
                            text = "💡 আপনার ওয়ালেটের রিফান্ড/জমা অর্থ থেকে সম্পূর্ণ ৳${DistanceUtil.toBengaliDigits(selectedBid.amount.toInt().toString())} কেটে এসক্রোতে জমা রাখা হবে। বাকি ৳${DistanceUtil.toBengaliDigits((userBalance - walletDeduction).toInt().toString())} ওয়ালেটেই থাকবে।",
                            fontSize = 11.5.sp,
                            color = SomadhanSuccess,
                            lineHeight = 16.sp
                        )
                    } else if (walletDeduction > 0.0) {
                        Text(
                            text = "💡 আপনার ওয়ালেট থেকে ৳${DistanceUtil.toBengaliDigits(walletDeduction.toInt().toString())} সমন্বয় করা হবে এবং অবশিষ্ট ৳${DistanceUtil.toBengaliDigits(remainingToPay.toInt().toString())} পেমেন্ট গেটওয়ে দিয়ে পরিশোধ করতে হবে।",
                            fontSize = 11.5.sp,
                            color = SomadhanTextSecondary,
                            lineHeight = 16.sp
                        )
                    }

                    if (acceptBidError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = acceptBidError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError
                        )
                    }
                }
            },
            confirmButton = {
                val acceptActionId = "accept_bid_${selectedBid.id}"
                val isAccepting = processingActionId == acceptActionId
                Button(
                    onClick = {
                        val bidToAccept = selectedBid
                        if (isFullWalletPay) {
                            processingActionId = acceptActionId
                            acceptBidError = null
                            viewModel.acceptBid(
                                currentProblem,
                                bidToAccept,
                                onSuccess = {
                                    processingActionId = null
                                    bidToAcceptWithWalletConfirm = null
                                },
                                onError = { err ->
                                    processingActionId = null
                                    acceptBidError = err
                                }
                            )
                        } else {
                            bidToAcceptWithWalletConfirm = null
                            acceptBidError = null
                            bidToAcceptGatewayDialog = Pair(bidToAccept, remainingToPay)
                        }
                    },
                    enabled = !isAccepting,
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_accept_bid_btn")
                ) {
                    if (isAccepting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("নিশ্চিত হচ্ছে...", fontWeight = FontWeight.Bold)
                    } else {
                        Text(if (isFullWalletPay) "গ্রহণ ও নিশ্চিত করুন" else "পেমেন্টে এগিয়ে যান", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { bidToAcceptWithWalletConfirm = null }) {
                    Text("ফিরে যান", color = SomadhanTextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Escrow Merchant Payment Confirmation Dialog for remaining/full amount
    if (bidToAcceptGatewayDialog != null && currentProblem != null) {
        val (selectedBid, payableAmount) = bidToAcceptGatewayDialog!!

        MerchantPaymentDialog(
            amount = payableAmount,
            problemTitle = currentProblem.title,
            solverName = selectedBid.solverName,
            onPaymentSuccess = {
                acceptBidGatewayPendingBid = selectedBid
                acceptBidGatewayError = null
                viewModel.acceptBid(
                    currentProblem,
                    selectedBid,
                    onSuccess = { acceptBidGatewayPendingBid = null },
                    onError = { err -> acceptBidGatewayError = err }
                )
            },
            onDismissRequest = {
                bidToAcceptGatewayDialog = null
            },
            onPaymentCompleteWithDetails = { gateway, trxId, phone ->
                viewModel.recordGatewayPayment(
                    gateway = gateway,
                    gatewayTrxId = trxId,
                    amount = payableAmount,
                    senderPhone = phone,
                    purpose = "ESCROW_PAYMENT",
                    problemId = currentProblem.id,
                    problemTitle = currentProblem.title,
                    note = "Problem #${currentProblem.id.take(8)}"
                )
            }
        )
    }

    // Post-gateway-payment verification overlay for acceptBid (section 3 pattern):
    // MerchantPaymentDialog already closed itself by the time this shows, but we keep the
    // user informed and locked out until the real viewModel.acceptBid() call resolves.
    if (acceptBidGatewayPendingBid != null && currentProblem != null) {
        val pendingBid = acceptBidGatewayPendingBid!!
        BottomSlideAlertDialog(
            onDismissRequest = { /* locked until success or explicit dismiss-on-error below */ },
            title = { Text("লেনদেন যাচাই হচ্ছে", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary) },
            text = {
                Column {
                    if (acceptBidGatewayError == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = SomadhanOrange, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("লেনদেন যাচাই হচ্ছে...", fontSize = 13.sp, color = SomadhanTextPrimary)
                        }
                    } else {
                        Text(
                            text = acceptBidGatewayError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError
                        )
                    }
                }
            },
            confirmButton = {
                if (acceptBidGatewayError != null) {
                    Button(onClick = {
                        acceptBidGatewayError = null
                        viewModel.acceptBid(
                            currentProblem,
                            pendingBid,
                            onSuccess = { acceptBidGatewayPendingBid = null },
                            onError = { err -> acceptBidGatewayError = err }
                        )
                    }) { Text("আবার চেষ্টা করুন") }
                }
            },
            dismissButton = {
                if (acceptBidGatewayError != null) {
                    TextButton(onClick = {
                        acceptBidGatewayPendingBid = null
                        acceptBidGatewayError = null
                    }) { Text("বাতিল", color = SomadhanTextSecondary) }
                }
            }
        )
    }

    // Confirmation Dialog for Additional Charge Request Acceptance (Wallet Deduction + Gateway)
    if (additionalChargeToPay != null && currentProblem != null) {
        val charge = additionalChargeToPay!!
        val userBalance = currentUser?.balanceUser ?: 0.0
        val isFullWallet = userBalance >= charge.amount
        val walletDeduction = if (userBalance > 0.0) minOf(userBalance, charge.amount) else 0.0
        val gatewayPayable = maxOf(0.0, charge.amount - walletDeduction)

        ExtraAmountPaymentConfirmationDialog(
            extraAmount = charge.amount,
            note = charge.reason.ifBlank { "সমাধানকারীর অতিরিক্ত বিল" },
            walletBalance = userBalance,
            isLoading = isSubmittingAdditionalCharge,
            errorMessage = additionalChargeError,
            onProceed = {
                val chargeToProcess = charge
                if (isFullWallet) {
                    // সম্পূর্ণ অর্থ ওয়ালেট ব্যালেন্স থেকে কর্তন হবে -- ডায়ালগ খোলাই থাকবে
                    // (loading spinner সহ) যতক্ষণ না রিকোয়েস্ট সত্যিই শেষ হয়, যাতে ধীর
                    // নেটওয়ার্কে দ্বিতীয়বার ট্যাপ করে ডাবল-চার্জ না হয়।
                    isSubmittingAdditionalCharge = true
                    additionalChargeError = null
                    viewModel.respondToAdditionalCharge(
                        charge = chargeToProcess,
                        accept = true,
                        onDone = {
                            isSubmittingAdditionalCharge = false
                            additionalChargeToPay = null
                        },
                        onError = { err ->
                            isSubmittingAdditionalCharge = false
                            additionalChargeError = err
                        }
                    )
                } else {
                    // আংশিক বা শূন্য ব্যালেন্স থাকলে বাকি টাকার জন্য পেমেন্ট গেটওয়ে ওপেন -- এখানে
                    // ডায়ালগ বন্ধ করা নিরাপদ, পরের ধাপ (MerchantPaymentDialog) নিজেই দায়িত্ব নেয়।
                    additionalChargeToPay = null
                    additionalChargeError = null
                    additionalChargeGatewayPayment = Pair(chargeToProcess, gatewayPayable)
                }
            },
            onDismiss = {
                if (!isSubmittingAdditionalCharge) {
                    additionalChargeToPay = null
                    additionalChargeError = null
                }
            }
        )
    }

    // Payment Gateway Dialog for Additional Charge (remaining amount)
    if (additionalChargeGatewayPayment != null && currentProblem != null) {
        val (charge, payableAmount) = additionalChargeGatewayPayment!!
        MerchantPaymentDialog(
            amount = payableAmount,
            problemTitle = "অতিরিক্ত বিল: ${currentProblem.title}",
            solverName = currentProblem.acceptedSolverName ?: "সমাধানকারী",
            onPaymentSuccess = {
                additionalChargeGatewayPending = charge
                additionalChargeGatewayError = null
                viewModel.respondToAdditionalCharge(
                    charge,
                    accept = true,
                    onDone = { additionalChargeGatewayPending = null },
                    onError = { err -> additionalChargeGatewayError = err }
                )
            },
            onDismissRequest = {
                additionalChargeGatewayPayment = null
            },
            onPaymentCompleteWithDetails = { gateway, trxId, phone ->
                viewModel.recordGatewayPayment(
                    gateway = gateway,
                    gatewayTrxId = trxId,
                    amount = payableAmount,
                    senderPhone = phone,
                    purpose = "ADDITIONAL_CHARGE",
                    problemId = currentProblem.id,
                    problemTitle = currentProblem.title,
                    note = "Problem #${currentProblem.id.take(8)}"
                )
            }
        )
    }

    // Post-gateway-payment verification overlay for the additional-charge accept flow
    // (section 3 pattern) -- MerchantPaymentDialog already closed by the time this shows.
    if (additionalChargeGatewayPending != null && currentProblem != null) {
        val pendingCharge = additionalChargeGatewayPending!!
        BottomSlideAlertDialog(
            onDismissRequest = { /* locked until success or explicit dismiss-on-error below */ },
            title = { Text("লেনদেন যাচাই হচ্ছে", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary) },
            text = {
                Column {
                    if (additionalChargeGatewayError == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = SomadhanOrange, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("লেনদেন যাচাই হচ্ছে...", fontSize = 13.sp, color = SomadhanTextPrimary)
                        }
                    } else {
                        Text(
                            text = additionalChargeGatewayError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError
                        )
                    }
                }
            },
            confirmButton = {
                if (additionalChargeGatewayError != null) {
                    Button(onClick = {
                        additionalChargeGatewayError = null
                        viewModel.respondToAdditionalCharge(
                            pendingCharge,
                            accept = true,
                            onDone = { additionalChargeGatewayPending = null },
                            onError = { err -> additionalChargeGatewayError = err }
                        )
                    }) { Text("আবার চেষ্টা করুন") }
                }
            },
            dismissButton = {
                if (additionalChargeGatewayError != null) {
                    TextButton(onClick = {
                        additionalChargeGatewayPending = null
                        additionalChargeGatewayError = null
                    }) { Text("বাতিল", color = SomadhanTextSecondary) }
                }
            }
        )
    }

    // Payment Dialog for Release Request Extra Amount (Wallet Deduction + Gateway Payment Confirmation)
    if (releaseExtraAmountToPay != null && currentProblem != null) {
        val extraAmt = releaseExtraAmountToPay!!
        val userBalance = currentUser?.balanceUser ?: 0.0
        val isFullWallet = userBalance >= extraAmt
        val walletDeduction = if (userBalance > 0.0) minOf(userBalance, extraAmt) else 0.0
        val gatewayPayable = maxOf(0.0, extraAmt - walletDeduction)

        ExtraAmountPaymentConfirmationDialog(
            extraAmount = extraAmt,
            note = currentProblem.releaseRequestNote.ifBlank { "রিলিজের সাথে অতিরিক্ত বিল" },
            walletBalance = userBalance,
            isLoading = isConfirmingReleaseExtraAmount,
            errorMessage = releaseExtraAmountError,
            onProceed = {
                if (isFullWallet) {
                    // Bug fix (loading-lock): dialog আগে সাথে সাথে বন্ধ হয়ে যেত, এখন
                    // confirmReleaseAndComplete()-এর callback আসা পর্যন্ত dialog খোলা (isLoading
                    // দিয়ে লক) থাকবে। ব্যর্থ হলে এখন inline error-এও জানানো হয় (আগে onError = {} ছিল)।
                    // সরাসরি ওয়ালেট থেকে সম্পূর্ণ অতিরিক্ত বিল কর্তন করে রিলিজ ও সম্পন্ন
                    isConfirmingReleaseExtraAmount = true
                    releaseExtraAmountError = null
                    viewModel.confirmReleaseAndComplete(
                        problem = currentProblem,
                        includeExtraAmount = true,
                        walletDeduction = extraAmt,
                        stars = userRatingStars,
                        reviewComment = userReviewComment,
                        onSuccess = {
                            isConfirmingReleaseExtraAmount = false
                            releaseExtraAmountToPay = null
                            val solverId = currentProblem.acceptedSolverId
                            val isVirtual = !currentProblem.isPhysical
                            val isAlreadyFav = currentUser?.favoriteSolverIds?.split(",")?.map { it.trim() }?.contains(solverId) == true
                            if (isVirtual && !solverId.isNullOrBlank()) {
                                if (!isAlreadyFav) {
                                    showAddToFavoritesDialog = true
                                }
                            }
                        },
                        onError = { err ->
                            isConfirmingReleaseExtraAmount = false
                            releaseExtraAmountError = err
                        }
                    )
                } else {
                    // কোনো network call এখানে নেই (শুধু dialog সুইচ), তাই সাথে সাথে বন্ধ করা নিরাপদ
                    releaseExtraAmountToPay = null
                    releaseExtraAmountError = null
                    extraAmountForGatewayPayment = gatewayPayable
                }
            },
            onDismiss = {
                if (!isConfirmingReleaseExtraAmount) {
                    releaseExtraAmountToPay = null
                    releaseExtraAmountError = null
                }
            }
        )
    }

    // Merchant Payment Gateway Dialog for remaining extra amount
    if (extraAmountForGatewayPayment != null && currentProblem != null) {
        val payableAmount = extraAmountForGatewayPayment!!
        val extraAmt = currentProblem.releaseRequestExtraAmount
        val userBalance = currentUser?.balanceUser ?: 0.0
        val walletDeduction = if (userBalance > 0.0) minOf(userBalance, extraAmt) else 0.0

        MerchantPaymentDialog(
            amount = payableAmount,
            problemTitle = "রিলিজ অতিরিক্ত বিল: ${currentProblem.title}",
            solverName = currentProblem.acceptedSolverName ?: "সমাধানকারী",
            onPaymentSuccess = {
                extraAmountForGatewayPayment = null
                releaseExtraAmountGatewayPending = true
                releaseExtraAmountGatewayError = null
                viewModel.confirmReleaseAndComplete(
                    problem = currentProblem,
                    includeExtraAmount = true,
                    walletDeduction = walletDeduction,
                    stars = userRatingStars,
                    reviewComment = userReviewComment,
                    onSuccess = {
                        releaseExtraAmountGatewayPending = false
                        val solverId = currentProblem.acceptedSolverId
                        val isVirtual = !currentProblem.isPhysical
                        val isAlreadyFav = currentUser?.favoriteSolverIds?.split(",")?.map { it.trim() }?.contains(solverId) == true
                        if (isVirtual && !solverId.isNullOrBlank()) {
                            if (!isAlreadyFav) {
                                showAddToFavoritesDialog = true
                            }
                        }
                    },
                    onError = { err -> releaseExtraAmountGatewayError = err }
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
                    purpose = "RELEASE_EXTRA",
                    problemId = currentProblem.id,
                    problemTitle = currentProblem.title,
                    note = "Problem #${currentProblem.id.take(8)}"
                )
            }
        )
    }

    // Post-gateway-payment verification overlay for the release-with-extra-amount flow
    if (releaseExtraAmountGatewayPending && currentProblem != null) {
        val extraAmt = currentProblem.releaseRequestExtraAmount
        val userBalanceForRetry = currentUser?.balanceUser ?: 0.0
        val walletDeductionForRetry = if (userBalanceForRetry > 0.0) minOf(userBalanceForRetry, extraAmt) else 0.0
        BottomSlideAlertDialog(
            onDismissRequest = { /* locked until success or explicit dismiss-on-error below */ },
            title = { Text("লেনদেন যাচাই হচ্ছে", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary) },
            text = {
                Column {
                    if (releaseExtraAmountGatewayError == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = SomadhanOrange, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("লেনদেন যাচাই হচ্ছে...", fontSize = 13.sp, color = SomadhanTextPrimary)
                        }
                    } else {
                        Text(
                            text = releaseExtraAmountGatewayError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError
                        )
                    }
                }
            },
            confirmButton = {
                if (releaseExtraAmountGatewayError != null) {
                    Button(onClick = {
                        releaseExtraAmountGatewayError = null
                        viewModel.confirmReleaseAndComplete(
                            problem = currentProblem,
                            includeExtraAmount = true,
                            walletDeduction = walletDeductionForRetry,
                            stars = userRatingStars,
                            reviewComment = userReviewComment,
                            onSuccess = {
                                releaseExtraAmountGatewayPending = false
                                val solverId = currentProblem.acceptedSolverId
                                val isVirtual = !currentProblem.isPhysical
                                val isAlreadyFav = currentUser?.favoriteSolverIds?.split(",")?.map { it.trim() }?.contains(solverId) == true
                                if (isVirtual && !solverId.isNullOrBlank()) {
                                    if (!isAlreadyFav) {
                                        showAddToFavoritesDialog = true
                                    }
                                }
                            },
                            onError = { err -> releaseExtraAmountGatewayError = err }
                        )
                    }) { Text("আবার চেষ্টা করুন") }
                }
            },
            dismissButton = {
                if (releaseExtraAmountGatewayError != null) {
                    TextButton(onClick = {
                        releaseExtraAmountGatewayPending = false
                        releaseExtraAmountGatewayError = null
                    }) { Text("বাতিল", color = SomadhanTextSecondary) }
                }
            }
        )
    }

    // Post-Release Add to Favorite Dialog for Virtual Category Jobs
    if (showAddToFavoritesDialog && currentProblem != null) {
        val solverId = currentProblem.acceptedSolverId.orEmpty()
        val solverName = currentProblem.acceptedSolverName ?: "সমাধানকারী"
        val isAlreadyFav = currentUser?.favoriteSolverIds?.split(",")?.map { it.trim() }?.contains(solverId) == true

        BottomSlideAlertDialog(
            onDismissRequest = { showAddToFavoritesDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE11D48),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "পছন্দের তালিকায় যোগ করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SomadhanTextPrimary,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "অভিনন্দন! আপনার কাজটি সফলভাবে সম্পন্ন ও রিলিজ হয়েছে।",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanSuccess,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "যেহেতু এটি একটি ভার্চুয়াল কাজ ছিল, আপনি কি \"$solverName\"-কে আপনার পছন্দের সমাধানকারী তালিকায় যুক্ত করতে চান? এর মাধ্যমে পরবর্তীতে ওনার সাথে সরাসরি চুক্তি (Direct Contract) করতে পারবেন।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isAlreadyFav) {
                            viewModel.addFavoriteSolver(solverId)
                        }
                        showAddToFavoritesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_add_to_favorite_dialog_btn")
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("পছন্দের তালিকায় যোগ করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddToFavoritesDialog = false }
                ) {
                    Text("পরে করব", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // Additional Charge Dialog (for Solver)
    if (showAddChargeDialog && currentProblem != null) {
        BottomSlideAlertDialog(
            onDismissRequest = {
                if (!isSubmittingCharge) {
                    showAddChargeDialog = false
                    chargeReason = ""
                    chargeAmountStr = ""
                    chargeError = null
                }
            },
            title = {
                Text(
                    text = "অতিরিক্ত বিল যোগ করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "কাজের প্রয়োজনে অতিরিক্ত খরচের কারণ ও পরিমাণ লিখুন। ব্যবহারকারী গ্রহণ করলে এটি Escrow-এ যুক্ত হবে।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = chargeReason,
                        onValueChange = {
                            chargeReason = it
                            chargeError = null
                        },
                        label = { Text("খরচের কারণ *") },
                        placeholder = { Text("যেমন: নতুন পার্টস ক্রয়") },
                        isError = chargeError != null && chargeReason.isBlank(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("charge_reason_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = chargeAmountStr,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                chargeAmountStr = input
                                chargeError = null
                            }
                        },
                        label = { Text("টাকার পরিমাণ (৳) *") },
                        placeholder = { Text("যেমন: 500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = chargeError != null && (chargeAmountStr.toDoubleOrNull() ?: 0.0) <= 0,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("charge_amount_input")
                    )

                    if (chargeError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = chargeError ?: "",
                            color = SomadhanError,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = chargeAmountStr.toDoubleOrNull() ?: 0.0
                        if (chargeReason.isBlank()) {
                            chargeError = "অনুগ্রহ করে কারণ লিখুন"
                            return@Button
                        }
                        if (amount <= 0) {
                            chargeError = "সঠিক টাকার পরিমাণ দিন (০-এর বেশি)"
                            return@Button
                        }
                        isSubmittingCharge = true
                        chargeError = null
                        viewModel.requestAdditionalCharge(
                            problem = currentProblem,
                            reason = chargeReason.trim(),
                            amount = amount,
                            onSuccess = {
                                isSubmittingCharge = false
                                showAddChargeDialog = false
                                viewModel.showToast("অতিরিক্ত বিলের অনুরোধ সফলভাবে পাঠানো হয়েছে!")
                                chargeReason = ""
                                chargeAmountStr = ""
                                chargeError = null
                            },
                            onError = { err ->
                                isSubmittingCharge = false
                                chargeError = err
                            }
                        )
                    },
                    enabled = !isSubmittingCharge,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    modifier = Modifier.testTag("submit_charge_button")
                ) {
                    if (isSubmittingCharge) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পাঠানো হচ্ছে...")
                    } else {
                        Text("পাঠান")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddChargeDialog = false
                        chargeReason = ""
                        chargeAmountStr = ""
                        chargeError = null
                    },
                    enabled = !isSubmittingCharge
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // User Delete Problem Dialog
    if (showUserDeleteProblemDialog && currentProblem != null) {
        BottomSlideAlertDialog(
            onDismissRequest = {
                if (!isDeletingProblemDetail) {
                    showUserDeleteProblemDialog = false
                    deleteProblemDetailError = null
                }
            },
            title = {
                Text(
                    text = "সমস্যাটি ডিলিট করতে চান?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "\"${currentProblem.title}\" পোস্টটি স্থায়ীভাবে মুছে ফেলা হবে এবং এর সকল বিড বাতিল হবে। আপনি কি নিশ্চিত?",
                        fontSize = 14.sp,
                        color = SomadhanTextSecondary
                    )
                    if (deleteProblemDetailError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = deleteProblemDetailError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeletingProblemDetail,
                    onClick = {
                        val prob = currentProblem
                        isDeletingProblemDetail = true
                        deleteProblemDetailError = null
                        viewModel.userDeleteProblem(
                            problem = prob,
                            onSuccess = {
                                isDeletingProblemDetail = false
                                showUserDeleteProblemDialog = false
                                onNavigateBack()
                            },
                            onError = { err ->
                                isDeletingProblemDetail = false
                                deleteProblemDetailError = err
                            }
                        )
                    },
                    modifier = Modifier.testTag("confirm_user_delete_problem_detail_btn")
                ) {
                    if (isDeletingProblemDetail) {
                        CircularProgressIndicator(color = SomadhanError, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ডিলিট হচ্ছে...", color = SomadhanError, fontWeight = FontWeight.Bold)
                    } else {
                        Text("হ্যাঁ, ডিলিট করুন", color = SomadhanError, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeletingProblemDetail,
                    onClick = {
                        showUserDeleteProblemDialog = false
                        deleteProblemDetailError = null
                    }
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }


    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "সমস্যার বিস্তারিত",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
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
                    actions = {
                        if (currentProblem != null && isOwner && currentProblem.status == "OPEN" && currentProblem.acceptedBidId == null && currentProblem.acceptedSolverId.isNullOrBlank()) {
                            IconButton(
                                onClick = { showUserDeleteProblemDialog = true },
                                modifier = Modifier.testTag("user_delete_problem_detail_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "পোস্ট ডিলিট করুন",
                                    tint = SomadhanError
                                )
                            }
                        }
                        val canAccessChat = currentProblem != null && !currentProblem.acceptedSolverId.isNullOrBlank() &&
                            (isOwner || currentProblem.acceptedSolverId == currentUser?.id)
                        if (canAccessChat) {
                            IconButton(
                                onClick = { onOpenChat(currentProblem!!.id) },
                                modifier = Modifier.testTag("open_chat_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadDetailChatCount > 0) {
                                            Badge(
                                                containerColor = com.example.ui.theme.SomadhanOrange,
                                                contentColor = Color.White
                                            ) {
                                                Text(
                                                    text = if (unreadDetailChatCount > 99) "99+" else DistanceUtil.toBengaliDigits(unreadDetailChatCount.toString()),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Message,
                                        contentDescription = "চ্যাট",
                                        tint = brandColor
                                    )
                                }
                            }
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
    ) { paddingValues ->
        val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()
        val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
        // Loading Pattern Master Prompt, ব্যাচ ২১ (ক্যাটেগরি C, দ্বিতীয় স্ক্রিন — rule ৪ প্রযোজ্য
        // না, grep করে যাচাই করা হয়েছে এই স্ক্রিনে কোনো pagination নেই) — আগে এখানে একটা ম্যানুয়াল
        // rememberSessionAwareSkeletonGate + early-return গেট ছিল (শুধু rule ১ কভার করতো)।
        // সেশন ২.৭ (batch 31) — আগে এখানে SyncAwareRefreshableContent (data = currentProblem+bids)
        // ব্যবহার হতো, যেটা যেকোনো দুটোর একটা বদলালেই পুরো LazyColumn (problem details + bid list
        // দুটোই একসাথে) সংক্ষিপ্তভাবে re-flash করতো — এটাই মূল প্রম্পটে বলা "re-entry-তে পুরো পেজ
        // আবার শিমার হয়ে যাওয়া/থামে না" বাগের কারণ (problem details অংশ কখনোই স্থির থাকত না)।
        // এখন cold-load-only SyncAwareContent-এ আনা হলো (rule ১, একবার LOADED হয়ে গেলে content()
        // আর নিজে থেকে re-animate করে না) — problem details অংশ (বেশিরভাগ LazyColumn) এখন সবসময়
        // স্থির/visible থাকে rule ২/৩ অনুযায়ী। নিচে শুধু bid-list অংশ (winning bid card + header +
        // empty-state + bid items, "bidsToDisplay" ব্লকের কাছে) নিজস্ব rememberFieldChangePulse +
        // PulsingValue দিয়ে আলাদাভাবে pulse করে (Wallet স্ক্রিনের একই প্যাটার্ন, দেখো
        // UserWalletScreen.kt) — শুধু bids বদলালে/pull-to-refresh শেষে/re-entry-তে সংক্ষিপ্ত pulse,
        // বাকি পেজ (problem details) অস্পর্শিত। পুরনো isCheckingProblem (এই নির্দিষ্ট problemId-র
        // জন্য viewModel.selectProblem() এখনো শেষ হয়নি কিনা, ১২০০ms artificial delay) লজিক
        // ইচ্ছাকৃতভাবে অক্ষত রাখা হয়েছে — global bulk-sync থেকে স্বতন্ত্র একটা per-instance concern,
        // নিচে content-এর ভেতরে আগের মতোই currentProblem == null-এর সাব-কেস হিসেবে রয়ে গেছে
        // (skeleton বনাম "পাওয়া যায়নি" নির্ধারণে)।
        SomadhanPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SyncAwareContent(
                sessionKey = "problem_detail_$problemId",
                viewModel = viewModel,
                syncPhase = initialSyncPhase,
                onRetry = { viewModel.retryInitialSync() },
                skeleton = {
                    DetailScreenSkeleton(
                        modifier = Modifier.fillMaxSize(),
                        tint = brandColor
                    )
                }
            ) {
                val bidsPulse = rememberFieldChangePulse(
                    value = bids,
                    isManualRefreshing = isRefreshing,
                    sessionKey = "problem_detail_$problemId",
                    viewModel = viewModel
                )
        if (currentProblem == null) {
            if (isCheckingProblem) {
                DetailScreenSkeleton(
                    modifier = Modifier.fillMaxSize(),
                    tint = brandColor
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Not Found",
                            tint = SomadhanTextHint,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "সমস্যাটি খুঁজে পাওয়া যায়নি",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "সমস্যাটি সম্পন্ন বা সমাপ্ত হয়ে থাকতে পারে অথবা বিজ্ঞপ্তিটির লিঙ্ক পরিবর্তিত হয়েছে।",
                            fontSize = 13.sp,
                            color = SomadhanTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ফিরে যান", color = Color.White)
                        }
                    }
                }
            }
        } else if (isRestrictedDirectContract) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Restricted",
                        tint = SomadhanOrange,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ব্যক্তিগত সরাসরি চুক্তি (Direct Contract)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "এটি একটি ব্যক্তিগত সরাসরি কাজের চুক্তি। শুধুমাত্র সংশ্লিষ্ট গ্রাহক ও নির্বাচিত সমাধানকারী এই পোস্টটিতে প্রবেশ করতে পারবেন।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ফিরে যান", color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Problem Status & Category Row + Post ID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(brandLight)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = currentProblem.categoryName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = brandColor
                                )
                            }

                            // Post ID Chip (1-click copyable - visible ONLY to the post creator/owner)
                            if (isOwner) {
                                val cleanPostId = currentProblem.id
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
                                        .testTag("detail_screen_id_${currentProblem.id}")
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
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanTextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "কপি করুন",
                                        tint = SomadhanTextHint,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            UrgencyBadge(urgency = currentProblem.urgency)
                        }
                        StatusBadge(status = currentProblem.status)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentProblem.isInstantJob) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "⚡ তাৎক্ষণিক জরুরি সেবা (Instant Emergency Request)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "এই সমস্যাটি দ্রুত সমাধানের জন্য নিকটস্থ দক্ষ সমাধানকারীদের জন্য ব্রডকাস্ট করা হচ্ছে।",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // এই solver-এর বাতিল/প্রত্যাখ্যাত বিড-নোটিশ (বর্তমান accepted_solver না হলে)
                    if (myEndedBid != null) {
                        val endedNoticeTitle = when (myEndedBid.status) {
                            "CANCELLED" -> "আপনি এই পোস্টে আপনার বিড বাতিল/প্রত্যাহার করেছেন"
                            "WITHDRAWN" -> "আপনি এই পোস্টে আপনার বিড প্রত্যাহার করেছেন"
                            "REJECTED" -> "ক্লায়েন্ট এই পোস্টে আপনার বিড গ্রহণ করেননি"
                            else -> "এই পোস্টে আপনার বিডের মেয়াদ শেষ হয়ে গেছে"
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SomadhanTextHint.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = SomadhanTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = endedNoticeTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "এই পোস্টের সাম্প্রতিক তথ্য/আপডেট এখানে নাও দেখাতে পারে।",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Problem Title
                    Text(
                        text = currentProblem.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "বিস্তারিত সমস্যা:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentProblem.description,
                                fontSize = 14.sp,
                                color = SomadhanTextPrimary,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Location & Budget Meta Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("পোস্টকারী", fontSize = 11.sp, color = SomadhanTextHint)
                                    val posterPhoto = allUsers.find { it.id == currentProblem.userId }?.profileImageUri
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable(enabled = currentUser?.id != currentProblem.userId) {
                                                val isPosterInBids = bids.any { it.solverId == currentProblem.userId }
                                                val targetRole = if (isPosterInBids) "SOLVER" else "USER"
                                                onNavigate(Screen.PublicProfile.createRoute(currentProblem.userId, targetRole))
                                            }
                                            .padding(vertical = 2.dp)
                                    ) {
                                        UserAvatar(
                                            photoUri = posterPhoto,
                                            name = currentProblem.userName,
                                            size = 32.dp,
                                            backgroundColor = brandLight,
                                            textColor = brandColor,
                                            fontSize = 13.sp,
                                            borderColor = brandColor.copy(alpha = 0.3f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = currentProblem.userName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        UserVerificationBadge(role = "USER")
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("বাজেট রেঞ্জ", fontSize = 11.sp, color = SomadhanTextHint)
                                    Text(
                                        text = Formatters.formatTakaRange(currentProblem.minBudget, currentProblem.maxBudget),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = brandColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = brandColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = currentProblem.userAddress,
                                        fontSize = 12.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }

                                Text(
                                    text = Formatters.formatTimeAgo(currentProblem.createdAt),
                                    fontSize = 11.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        }
                    }

                    // Direct Contract Status (when OPEN or PENDING_ACCEPTANCE)
                    if (currentProblem.isDirectContract && currentProblem.status == "OPEN") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFF1D4ED8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "সরাসরি কাজের প্রস্তাবনা",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1D4ED8)
                                        )
                                    }
                                    Text(
                                        text = if (currentProblem.directContractStatus == "DECLINED") "প্রত্যাখ্যাত" else "সম্মতির অপেক্ষায়",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentProblem.directContractStatus == "DECLINED") SomadhanError else Color(0xFFD97706)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "প্রস্তাবিত সমাধানকারী: ${currentProblem.acceptedSolverName ?: "সমাধানকারী"} (বাজেট: ৳${currentProblem.acceptedAmount?.toInt() ?: currentProblem.minBudget.toInt()})",
                                    fontSize = 12.sp,
                                    color = SomadhanTextPrimary
                                )

                                if (currentProblem.directContractStatus == "PENDING_ACCEPTANCE") {
                                    if (currentUser?.id == currentProblem.acceptedSolverId) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "আপনি এই কাজের সরাসরি অফার পেয়েছেন। কাজ শুরু করতে প্রস্তাবটি গ্রহণ করুন।",
                                            fontSize = 12.sp,
                                            color = SomadhanTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val acceptDcActionId = "accept_dc_${currentProblem.id}"
                                            val declineDcActionId = "decline_dc_${currentProblem.id}"
                                            val isAcceptingDc = processingActionId == acceptDcActionId
                                            val isDecliningDc = processingActionId == declineDcActionId
                                            Button(
                                                onClick = {
                                                    if (processingActionId == null) {
                                                        processingActionId = acceptDcActionId
                                                        directContractProposalError = null
                                                        viewModel.acceptDirectContractProposal(
                                                            problemId = currentProblem.id,
                                                            onSuccess = { processingActionId = null },
                                                            onError = { err ->
                                                                processingActionId = null
                                                                directContractProposalError = err
                                                            }
                                                        )
                                                    }
                                                },
                                                enabled = !isAcceptingDc && !isDecliningDc,
                                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).testTag("accept_direct_contract_btn")
                                            ) {
                                                if (isAcceptingDc) {
                                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("গ্রহণ হচ্ছে...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                } else {
                                                    Text("✅ গ্রহণ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    if (processingActionId == null) {
                                                        processingActionId = declineDcActionId
                                                        directContractProposalError = null
                                                        viewModel.declineDirectContractProposal(
                                                            problemId = currentProblem.id,
                                                            reason = "সমাধানকারী অপারগতা প্রকাশ করেছেন",
                                                            onSuccess = { processingActionId = null },
                                                            onError = { err ->
                                                                processingActionId = null
                                                                directContractProposalError = err
                                                            }
                                                        )
                                                    }
                                                },
                                                enabled = !isAcceptingDc && !isDecliningDc,
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                                                border = BorderStroke(1.dp, SomadhanError),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).testTag("decline_direct_contract_btn")
                                            ) {
                                                if (isDecliningDc) {
                                                    CircularProgressIndicator(color = SomadhanError, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("প্রত্যাখ্যান হচ্ছে...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                } else {
                                                    Text("❌ প্রত্যাখ্যান", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        if (directContractProposalError != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = directContractProposalError ?: "",
                                                fontSize = 11.sp,
                                                color = SomadhanError
                                            )
                                        }
                                    } else if (isOwner) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "প্রস্তাবটি সমাধানকারীর সম্মতির অপেক্ষায় রয়েছে। সমাধানকারী গ্রহণ করলে কাজটি সক্রিয় হবে।",
                                            fontSize = 12.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Problem Status Specific Actions (In progress / Completed / Accepted Solver Info)
                    if (currentProblem.status == "IN_PROGRESS" && (isOwner || currentUser?.id == currentProblem.acceptedSolverId)) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = brandLight),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, brandBorder, RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "কাজটি বর্তমানে চলমান রয়েছে",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = brandColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "নির্বাচিত সমাধানকারী: ${currentProblem.acceptedSolverName ?: ""} (চুক্তি: ৳ ${currentProblem.acceptedAmount?.toInt() ?: 0})",
                                    fontSize = 12.sp,
                                    color = SomadhanTextPrimary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable(enabled = currentProblem.acceptedSolverId != null && currentUser?.id != currentProblem.acceptedSolverId) {
                                            currentProblem.acceptedSolverId?.let { solverId ->
                                                onNavigate(Screen.PublicProfile.createRoute(solverId, "SOLVER"))
                                            }
                                        }
                                        .padding(vertical = 2.dp)
                                )

                                if (escrow != null && escrow?.status != "RELEASED") {
                                    val totalEscrow = (escrow?.baseAmount ?: 0.0) + (escrow?.extraAmount ?: 0.0)
                                    val extraAmt = escrow?.extraAmount ?: 0.0
                                    val baseAmt = escrow?.baseAmount ?: 0.0

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .border(1.dp, SomadhanBorder, RoundedCornerShape(8.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("মূল বিড মূল্য:", fontSize = 12.sp, color = SomadhanTextSecondary)
                                                Text("৳ ${baseAmt.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                                            }

                                            if (extraAmt > 0.0) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("অতিরিক্ত বিল (গৃহীত):", fontSize = 12.sp, color = SomadhanTextSecondary)
                                                    Text("৳ ${extraAmt.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanOrange)
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Divider(color = SomadhanBorder, thickness = 1.dp)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("মোট প্রদেয় (Escrow):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                                                    Text("৳ ${totalEscrow.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanSuccess)
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "🔒 ৳ ${totalEscrow.toInt()} Escrow-এ সুরক্ষিত আছে",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SomadhanSuccess
                                            )
                                        }
                                    }
                                } else if (isOwner && !currentProblem.isDisputed) {
                                    // Recovery card for the "orphaned accepted bid" dead-end (case
                                    // 3 in the bug report): acceptedSolverId is set, the job shows
                                    // IN_PROGRESS, but there is no escrow row at all -- meaning no
                                    // money actually moved for this accept. Before this fix the
                                    // owner's only option here was the Dispute button, even though
                                    // there was nothing for an admin to actually adjudicate. This
                                    // only appears when escrow is genuinely absent; if any funds
                                    // are locked, ownerResetOrphanedAcceptedBid() itself refuses
                                    // and this button surfaces that error via a toast instead.
                                    var isResettingOrphanedBid by remember(currentProblem.id) { mutableStateOf(false) }
                                    var resetOrphanedBidError by remember(currentProblem.id) { mutableStateOf<String?>(null) }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SomadhanErrorLight),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "⚠️ এই বিডের বিপরীতে কোনো Escrow পাওয়া যায়নি — এটি একটি অসম্পূর্ণ/ত্রুটিপূর্ণ accept হতে পারে।",
                                                fontSize = 11.5.sp,
                                                color = SomadhanError,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    if (!isResettingOrphanedBid) {
                                                        isResettingOrphanedBid = true
                                                        resetOrphanedBidError = null
                                                        viewModel.ownerResetOrphanedAcceptedBid(
                                                            currentProblem,
                                                            onSuccess = { isResettingOrphanedBid = false },
                                                            onError = { err ->
                                                                isResettingOrphanedBid = false
                                                                resetOrphanedBidError = err
                                                            }
                                                        )
                                                    }
                                                },
                                                enabled = !isResettingOrphanedBid,
                                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().height(38.dp)
                                            ) {
                                                if (isResettingOrphanedBid) {
                                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("রিসেট হচ্ছে...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                } else {
                                                    Text("এই accept বাতিল করে পুনরায় ব্রডকাস্ট করুন", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                            if (resetOrphanedBidError != null) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = resetOrphanedBidError ?: "",
                                                    fontSize = 11.sp,
                                                    color = SomadhanError
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isOwner || currentUser?.id == currentProblem.acceptedSolverId) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (currentProblem.isInstantJob) {
                                            Button(
                                                onClick = { onNavigate(Screen.JobTracking.createRoute(currentProblem.id)) },
                                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("লাইভ ট্র্যাকিং", fontSize = 12.sp)
                                            }
                                        }

                                        Button(
                                            onClick = { onOpenChat(currentProblem.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("চ্যাট করুন", fontSize = 12.sp)
                                        }

                                        val isSolver = currentUser?.id == currentProblem.acceptedSolverId
                                        val canShowDispute = !currentProblem.isDisputed && 
                                            (currentProblem.status == "IN_PROGRESS" || currentProblem.jobStatus in listOf("ACCEPTED", "IN_PROGRESS", "WORKING", "SUBMITTED", "PENDING_CONFIRMATION")) && 
                                            (isOwner || isSolver)

                                        if (canShowDispute) {
                                            OutlinedButton(
                                                onClick = {
                                                    disputeReasonInput = ""
                                                    showDisputeDialog = true
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = Color(0xFFFEF2F2),
                                                    contentColor = Color(0xFFDC2626)
                                                ),
                                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).testTag("header_raise_dispute_button")
                                            ) {
                                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFDC2626))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("বিরোধ (Dispute)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                            }
                                        }
                                    }
                                }

                                // 1. SOLVER PERSPECTIVE
                                if (currentUser?.id == currentProblem.acceptedSolverId) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (currentProblem.isDisputed) {
                                        // Dispute is active
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "🚨 বিরোধ (Dispute) চলমান রয়েছে",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFDC2626)
                                                    )
                                                }
                                                if (currentProblem.disputeReason.orEmpty().isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "গ্রাহকের অভিযোগ: ${currentProblem.disputeReason.orEmpty()}",
                                                        fontSize = 12.sp,
                                                        color = SomadhanTextPrimary
                                                    )
                                                }
                                                if (currentProblem.isAdminInvolvedInChat) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "🛡️ চ্যাটে অ্যাডমিন মধ্যস্থতার জন্য যুক্ত আছেন।",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF4F46E5)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = { onOpenChat(currentProblem.id) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f).height(40.dp)
                                                    ) {
                                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("চ্যাটে আলোচনা করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    if (currentProblem.disputeInitiatorId == currentUser?.id) {
                                                        val withdrawId = "withdraw_dispute_${currentProblem.id}"
                                                        val isWithdrawing = processingActionId == withdrawId
                                                        OutlinedButton(
                                                            onClick = {
                                                                if (processingActionId == null) {
                                                                    processingActionId = withdrawId
                                                                    withdrawDisputeError = null
                                                                    viewModel.withdrawDispute(
                                                                        currentProblem,
                                                                        onDone = { processingActionId = null },
                                                                        onError = { err ->
                                                                            processingActionId = null
                                                                            withdrawDisputeError = err
                                                                        }
                                                                    )
                                                                }
                                                            },
                                                            enabled = !isWithdrawing,
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
                                                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier.weight(1f).height(40.dp)
                                                        ) {
                                                            if (isWithdrawing) {
                                                                CircularProgressIndicator(color = Color(0xFF64748B), modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("প্রত্যাহার হচ্ছে...", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                            } else {
                                                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF64748B))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("বিরোধ প্রত্যাহার", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                            }
                                                        }
                                                    } else if (currentProblem.hasReleaseRequest) {
                                                        val cancelReleaseId = "cancel_release_dispute_${currentProblem.id}"
                                                        val isCancellingRelease = processingActionId == cancelReleaseId
                                                        OutlinedButton(
                                                            onClick = {
                                                                if (processingActionId == null) {
                                                                    processingActionId = cancelReleaseId
                                                                    cancelReleaseRequestError = null
                                                                    viewModel.cancelJobReleaseRequest(
                                                                        currentProblem,
                                                                        onSuccess = { processingActionId = null },
                                                                        onError = { err ->
                                                                            processingActionId = null
                                                                            cancelReleaseRequestError = err
                                                                        }
                                                                    )
                                                                }
                                                            },
                                                            enabled = !isCancellingRelease,
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanOrange),
                                                            border = BorderStroke(1.dp, SomadhanOrange),
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier.weight(1f).height(40.dp)
                                                        ) {
                                                            if (isCancellingRelease) {
                                                                CircularProgressIndicator(color = SomadhanOrange, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("প্রত্যাহার হচ্ছে...", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                                            } else {
                                                                Text("অনুরোধ প্রত্যাহার", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                                            }
                                                        }
                                                    }
                                                }
                                                if (withdrawDisputeError != null || cancelReleaseRequestError != null) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = withdrawDisputeError ?: cancelReleaseRequestError ?: "",
                                                        fontSize = 11.sp,
                                                        color = SomadhanError
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                OutlinedButton(
                                                    onClick = { showSolverCancelJobDialog = true },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                                                    border = BorderStroke(1.dp, SomadhanError),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth().height(38.dp).testTag("solver_cancel_job_button")
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp), tint = SomadhanError)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("কাজটি সম্পূর্ণ বাতিল করুন (গ্রাহককে রিফান্ড)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    } else if (currentProblem.hasReleaseRequest) {
                                        // Solver has already sent a release request
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, SomadhanOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "রিলিজের অনুরোধ পাঠানো হয়েছে",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SomadhanOrange
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                val totalClaim = (currentProblem.acceptedAmount ?: 0.0) + currentProblem.releaseRequestExtraAmount
                                                Text(
                                                    text = "মোট দাবিকৃত অর্থ: ৳ ${DistanceUtil.toBengaliDigits(totalClaim.toInt().toString())} (ব্যবহারকারীর অনুমোদনের অপেক্ষায়)",
                                                    fontSize = 12.sp,
                                                    color = SomadhanTextPrimary
                                                )
                                                if (currentProblem.releaseRequestExtraAmount > 0.0) {
                                                    Text(
                                                        text = "• অতিরিক্ত বিল: ৳ ${DistanceUtil.toBengaliDigits(currentProblem.releaseRequestExtraAmount.toInt().toString())}",
                                                        fontSize = 11.sp,
                                                        color = SomadhanOrange,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                if (currentProblem.releaseRequestNote.isNotBlank()) {
                                                    Text(
                                                        text = "• নোট: ${currentProblem.releaseRequestNote}",
                                                        fontSize = 11.sp,
                                                        color = SomadhanTextSecondary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                val cancelReleaseId2 = "cancel_release_solvercard_${currentProblem.id}"
                                                val isCancellingRelease2 = processingActionId == cancelReleaseId2
                                                OutlinedButton(
                                                    onClick = {
                                                        if (processingActionId == null) {
                                                            processingActionId = cancelReleaseId2
                                                            cancelReleaseRequestError = null
                                                            viewModel.cancelJobReleaseRequest(
                                                                currentProblem,
                                                                onSuccess = { processingActionId = null },
                                                                onError = { err ->
                                                                    processingActionId = null
                                                                    cancelReleaseRequestError = err
                                                                }
                                                            )
                                                        }
                                                    },
                                                    enabled = !isCancellingRelease2,
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                                                    border = BorderStroke(1.dp, SomadhanError.copy(alpha = 0.5f)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                                ) {
                                                    if (isCancellingRelease2) {
                                                        CircularProgressIndicator(color = SomadhanError, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("প্রত্যাহার হচ্ছে...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    } else {
                                                        Text("অনুরোধ প্রত্যাহার / সংশোধন করুন", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                                if (cancelReleaseRequestError != null) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = cancelReleaseRequestError ?: "",
                                                        fontSize = 11.sp,
                                                        color = SomadhanError
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // Solver has not requested release yet
                                        val solverPendingCharge = pendingCharges.firstOrNull { it.solverId == currentUser?.id }
                                        val hasPendingExtraCharge = solverPendingCharge != null

                                        // 1. Extra charge button / pending indicator (PLACED ON TOP)
                                        if (hasPendingExtraCharge) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = "অতিরিক্ত বিল পর্যালোচনায় রয়েছে (৳ ${DistanceUtil.toBengaliDigits(solverPendingCharge!!.amount.toInt().toString())})",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF92400E)
                                                        )
                                                        Text(
                                                            text = "গ্রাহক প্রত্যাখ্যান বা অনুমোদন করার পর পুনরায় আবেদন করতে পারবেন।",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFFB45309),
                                                            lineHeight = 15.sp
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            OutlinedButton(
                                                onClick = { showSolverCancelAcceptedBidDialog = true },
                                                border = BorderStroke(1.5.dp, SomadhanError),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = SomadhanErrorLight.copy(alpha = 0.4f),
                                                    contentColor = SomadhanError
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(44.dp)
                                                    .testTag("solver_cancel_accepted_bid_button")
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = SomadhanError)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("বিড বাতিল করুন", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanError)
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { showAddChargeDialog = true },
                                                    border = BorderStroke(1.5.dp, SomadhanOrange),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = SomadhanOrangeLight.copy(alpha = 0.4f),
                                                        contentColor = SomadhanOrange
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                        .testTag("add_extra_charge_button")
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = SomadhanOrange)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("অতিরিক্ত বিল", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = SomadhanOrange, maxLines = 1)
                                                }

                                                OutlinedButton(
                                                    onClick = { showSolverCancelAcceptedBidDialog = true },
                                                    border = BorderStroke(1.5.dp, SomadhanError),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = SomadhanErrorLight.copy(alpha = 0.4f),
                                                        contentColor = SomadhanError
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                        .testTag("solver_cancel_accepted_bid_button")
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = SomadhanError)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("বিড বাতিল করুন", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = SomadhanError, maxLines = 1)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // 2. Release Request Button (PLACED BELOW EXTRA CHARGE, WITH DISTINCTIVE VIBRANT INDIGO BACKGROUND)
                                        Button(
                                            onClick = {
                                                releaseExtraAmountStr = ""
                                                releaseNote = ""
                                                releaseError = null
                                                showReleaseRequestDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF4F46E5), // Distinctive vibrant Royal Indigo
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                                .testTag("request_release_button")
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("কাজ সম্পন্ন ও অর্থ রিলিজের অনুরোধ পাঠান", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }

                                // 2. USER (JOB OWNER) PERSPECTIVE
                                if (isOwner) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (currentProblem.isDisputed) {
                                        // Dispute is active for owner
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "🚨 বিরোধ (Dispute) চলমান রয়েছে",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFDC2626)
                                                    )
                                                }
                                                if (currentProblem.disputeReason.orEmpty().isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "বিরোধের কারণ: ${currentProblem.disputeReason.orEmpty()}",
                                                        fontSize = 12.sp,
                                                        color = SomadhanTextPrimary
                                                    )
                                                }
                                                if (currentProblem.isAdminInvolvedInChat) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "🛡️ চ্যাটে অ্যাডমিন মধ্যস্থতার জন্য যুক্ত আছেন।",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF4F46E5)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = { onOpenChat(currentProblem.id) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f).height(42.dp)
                                                    ) {
                                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("বিরোধ চ্যাট খুলুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Button(
                                                        onClick = {
                                                            includeExtraAmountInRelease = true
                                                            showConfirmReleaseDialog = true
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1.2f).height(42.dp).testTag("dispute_accept_release_button")
                                                    ) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("সমঝোতা ও রিলিজ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                if (currentProblem.disputeInitiatorId == currentUser?.id) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    val withdrawId2 = "withdraw_dispute_${currentProblem.id}"
                                                    val isWithdrawing2 = processingActionId == withdrawId2
                                                    OutlinedButton(
                                                        onClick = {
                                                            if (processingActionId == null) {
                                                                processingActionId = withdrawId2
                                                                withdrawDisputeError = null
                                                                viewModel.withdrawDispute(
                                                                    currentProblem,
                                                                    onDone = { processingActionId = null },
                                                                    onError = { err ->
                                                                        processingActionId = null
                                                                        withdrawDisputeError = err
                                                                    }
                                                                )
                                                            }
                                                        },
                                                        enabled = !isWithdrawing2,
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
                                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.fillMaxWidth().height(38.dp)
                                                    ) {
                                                        if (isWithdrawing2) {
                                                            CircularProgressIndicator(color = Color(0xFF64748B), modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("প্রত্যাহার হচ্ছে...", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                                        } else {
                                                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF64748B))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("বিরোধ প্রত্যাহার করুন (Withdraw Dispute)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }
                                                    if (withdrawDisputeError != null) {
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            text = withdrawDisputeError ?: "",
                                                            fontSize = 11.sp,
                                                            color = SomadhanError
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else if (currentProblem.hasReleaseRequest) {
                                        // Solver has sent a release request - user can confirm & release or dispute or reject
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.5.dp, Color(0xFF10B981), RoundedCornerShape(10.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SomadhanSuccess, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "রিলিজের অনুরোধ এসেছে!",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SomadhanSuccess
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "সমাধানকারী কাজ সম্পন্ন করে অর্থ রিলিজের অনুরোধ পাঠিয়েছেন।",
                                                    fontSize = 12.sp,
                                                    color = SomadhanTextPrimary
                                                )
                                                if (currentProblem.releaseRequestExtraAmount > 0.0) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "• অতিরিক্ত বিল দাবি: ৳ ${DistanceUtil.toBengaliDigits(currentProblem.releaseRequestExtraAmount.toInt().toString())}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SomadhanOrange
                                                    )
                                                }
                                                if (currentProblem.releaseRequestNote.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "• নোট: ${currentProblem.releaseRequestNote}",
                                                        fontSize = 12.sp,
                                                        color = SomadhanTextSecondary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            disputeReasonInput = ""
                                                            showDisputeDialog = true
                                                        },
                                                        colors = ButtonDefaults.outlinedButtonColors(
                                                            containerColor = Color(0xFFFEF2F2),
                                                            contentColor = Color(0xFFDC2626)
                                                        ),
                                                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f).height(46.dp).testTag("release_request_dispute_button")
                                                    ) {
                                                        Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFDC2626))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("বিরোধ (Dispute)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                                    }

                                                    Button(
                                                        onClick = {
                                                            includeExtraAmountInRelease = true
                                                            showConfirmReleaseDialog = true
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier
                                                            .weight(1.3f)
                                                            .height(46.dp)
                                                            .testTag("confirm_release_button")
                                                    ) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("রিলিজ ও সম্পন্ন করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // Solver has not yet requested release
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "কাজটি বর্তমানে চলমান রয়েছে। সমাধানকারী কাজ সম্পন্ন করে রিলিজ অনুরোধ পাঠালে আপনি অর্থ রিলিজ ও কাজ সম্পন্ন নিশ্চিত করতে পারবেন।",
                                                    fontSize = 12.sp,
                                                    color = SomadhanTextSecondary,
                                                    lineHeight = 17.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isOwner && pendingCharges.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "অতিরিক্ত বিলের অনুরোধ:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    pendingCharges.forEach { charge ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .border(1.dp, brandBorder, RoundedCornerShape(8.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = charge.reason,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = SomadhanTextPrimary
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "৳ ${charge.amount.toInt()}",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SomadhanOrange
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(
                                                        onClick = { additionalChargeToPay = charge },
                                                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanSuccess),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .testTag("accept_charge_${charge.id}")
                                                    ) {
                                                        Text("✅ গ্রহণ করুন (পেমেন্ট)", fontSize = 11.sp, color = Color.White)
                                                    }
                                                    run {
                                                        val rejectChargeId = "reject_charge_${charge.id}"
                                                        val isRejectingCharge = processingActionId == rejectChargeId
                                                        Button(
                                                            onClick = {
                                                                if (processingActionId == null) {
                                                                    processingActionId = rejectChargeId
                                                                    rejectChargeError = null
                                                                    viewModel.respondToAdditionalCharge(
                                                                        charge,
                                                                        accept = false,
                                                                        onDone = { processingActionId = null },
                                                                        onError = { err ->
                                                                            processingActionId = null
                                                                            rejectChargeError = charge.id to err
                                                                        }
                                                                    )
                                                                }
                                                            },
                                                            enabled = !isRejectingCharge,
                                                            colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                                                            shape = RoundedCornerShape(6.dp),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .testTag("reject_charge_${charge.id}")
                                                        ) {
                                                            if (isRejectingCharge) {
                                                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("প্রত্যাখ্যান হচ্ছে...", fontSize = 11.sp, color = Color.White)
                                                            } else {
                                                                Text("❌ প্রত্যাখ্যান করুন", fontSize = 11.sp, color = Color.White)
                                                            }
                                                        }
                                                    }
                                                }
                                                if (rejectChargeError?.first == charge.id) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = rejectChargeError?.second ?: "",
                                                        fontSize = 11.sp,
                                                        color = SomadhanError
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val isPartyToDispute = isOwner || 
                        (currentUser?.id == currentProblem.acceptedSolverId) || 
                        (currentUser?.role?.equals("ADMIN", ignoreCase = true) == true)

                    if (currentProblem.disputeResolvedAt != null && isPartyToDispute) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val (disputeDecTitle, disputeDecColor, disputeDecIcon) = when (currentProblem.disputeResolutionDecision) {
                            "RELEASE_TO_SOLVER" -> Triple("সমাধানকারীকে অর্থ রিলিজ করা হয়েছে", SomadhanSuccess, Icons.Default.CheckCircle)
                            "REFUND_TO_USER" -> Triple("গ্রাহককে অর্থ রিফান্ড করা হয়েছে", SomadhanError, Icons.AutoMirrored.Filled.Undo)
                            "SPLIT_SETTLEMENT" -> Triple("৫০/৫০ সমঝোতা নিষ্পত্তি হয়েছে", SomadhanInfo, Icons.Default.Balance)
                            else -> Triple("বিরোধ নিষ্পত্তি সম্পন্ন হয়েছে", SomadhanTextPrimary, Icons.Default.Gavel)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = disputeDecColor.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, disputeDecColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(disputeDecIcon, contentDescription = null, tint = disputeDecColor, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "⚖️ বিরোধ নিষ্পত্তি: $disputeDecTitle",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = disputeDecColor
                                    )
                                }
                                if (!currentProblem.disputeResolutionNote.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "সিদ্ধান্ত নোট: ${currentProblem.disputeResolutionNote}",
                                        fontSize = 12.sp,
                                        color = SomadhanTextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { onNavigate(Screen.DisputeResult.createRoute(currentProblem.id)) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = SomadhanCardBg,
                                        contentColor = disputeDecColor
                                    ),
                                    border = BorderStroke(1.dp, disputeDecColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(38.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(15.dp), tint = disputeDecColor)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("বিরোধ নিষ্পত্তির পূর্ণ বিবরণ দেখুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (currentProblem.status == "COMPLETED" || (currentProblem.disputeResolvedAt != null && !isPartyToDispute)) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanSuccessLight),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SomadhanSuccess.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SomadhanSuccess, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("কাজটি সফলভাবে সম্পন্ন হয়েছে!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanSuccess)
                                    Text("সমাধানকারীকে অর্থ প্রদান করা হয়েছে।", fontSize = 11.sp, color = SomadhanTextSecondary)
                                }
                            }
                        }

                        // User Rating for Solver UI (Rate Solver)
                        if (isOwner) {
                            val hasUserRated = userGivenReviews.any { it.problemId == currentProblem.id && it.raterRole == "USER" }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (!hasUserRated) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, brandBorder, RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = brandColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "এই সমাধানকারীকে রেটিং দিন",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = SomadhanTextPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "সমাধানকারী ${currentProblem.acceptedSolverName ?: "সমাধানকারী"}-এর কাজের অভিজ্ঞতা কেমন ছিল?",
                                            fontSize = 12.sp,
                                            color = SomadhanTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Star Rating Bar
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            (1..5).forEach { star ->
                                                IconButton(onClick = { userRatingStars = star }) {
                                                    Icon(
                                                        imageVector = if (star <= userRatingStars) Icons.Filled.Star else Icons.Outlined.Star,
                                                        contentDescription = "$star স্টার",
                                                        tint = if (star <= userRatingStars) SomadhanYellowVerified else SomadhanTextHint,
                                                        modifier = Modifier.size(30.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = userReviewComment,
                                            onValueChange = { userReviewComment = it },
                                            placeholder = { Text("একটি মন্তব্য লিখুন (ঐচ্ছিক)...", fontSize = 13.sp, color = SomadhanTextHint) },
                                            minLines = 2,
                                            maxLines = 4,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = brandColor,
                                                unfocusedBorderColor = SomadhanBorder,
                                                focusedContainerColor = SomadhanBg,
                                                unfocusedContainerColor = SomadhanCardBg
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                viewModel.submitUserRatingForSolver(
                                                    problem = currentProblem,
                                                    stars = userRatingStars,
                                                    comment = userReviewComment
                                                )
                                            },
                                            enabled = userRatingStars > 0,
                                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("submit_user_rating_button")
                                        ) {
                                            Text("রেটিং জমা দিন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }

                                        if (userRatingStars == 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "রেটিং দিতে অন্তত ১টি স্টার সিলেক্ট করুন",
                                                fontSize = 11.sp,
                                                color = SomadhanTextHint,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        if (canFavoriteSolverState && !currentProblem.acceptedSolverId.isNullOrBlank()) {
                                            val solverId = currentProblem.acceptedSolverId!!
                                            val isFav = currentUser?.favoriteSolverIds?.split(",")?.map { it.trim() }?.contains(solverId) == true
                                            Spacer(modifier = Modifier.height(12.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    if (isFav) {
                                                        viewModel.showToast("এই সমাধানকারী ইতোমধ্যেই আপনার পছন্দের তালিকায় যুক্ত আছেন।")
                                                    } else {
                                                        viewModel.addFavoriteSolver(solverId)
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (isFav) Color(0xFFF0FDF4) else Color.Transparent,
                                                    contentColor = if (isFav) Color(0xFF166534) else Color(0xFFE11D48)
                                                ),
                                                border = BorderStroke(1.dp, if (isFav) Color(0xFF86EFAC) else Color(0xFFFDA4AF)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("toggle_favorite_solver_button")
                                            ) {
                                                Icon(
                                                    imageVector = if (isFav) Icons.Filled.CheckCircle else Icons.Filled.FavoriteBorder,
                                                    contentDescription = null,
                                                    tint = if (isFav) Color(0xFF16A34A) else Color(0xFFE11D48),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isFav) "✓ ইতোমধ্যে পছন্দের তালিকায় যুক্ত আছেন" else "❤️ পছন্দের তালিকায় যোগ করুন",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SomadhanBorder, RoundedCornerShape(10.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SomadhanSuccess, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "আপনি এই সমাধানকারীকে রেটিং প্রদান করেছেন।",
                                                fontSize = 12.sp,
                                                color = SomadhanTextSecondary
                                            )
                                        }

                                        if (canFavoriteSolverState && !currentProblem.acceptedSolverId.isNullOrBlank()) {
                                            val solverId = currentProblem.acceptedSolverId!!
                                            val isFav = currentUser?.favoriteSolverIds?.split(",")?.map { it.trim() }?.contains(solverId) == true
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    if (isFav) {
                                                        viewModel.showToast("এই সমাধানকারী ইতোমধ্যেই আপনার পছন্দের তালিকায় যুক্ত আছেন।")
                                                    } else {
                                                        viewModel.addFavoriteSolver(solverId)
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (isFav) Color(0xFFF0FDF4) else Color.Transparent,
                                                    contentColor = if (isFav) Color(0xFF166534) else Color(0xFFE11D48)
                                                ),
                                                border = BorderStroke(1.dp, if (isFav) Color(0xFF86EFAC) else Color(0xFFFDA4AF)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("toggle_favorite_solver_rated_button")
                                            ) {
                                                Icon(
                                                    imageVector = if (isFav) Icons.Filled.CheckCircle else Icons.Filled.FavoriteBorder,
                                                    contentDescription = null,
                                                    tint = if (isFav) Color(0xFF16A34A) else Color(0xFFE11D48),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isFav) "✓ ইতোমধ্যে পছন্দের তালিকায় যুক্ত আছেন" else "❤️ পছন্দের তালিকায় যোগ করুন",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Solver Rating for User UI
                        if (currentUser?.id == currentProblem.acceptedSolverId) {
                            val hasSolverRated = solverGivenReviews.any { it.problemId == currentProblem.id && it.raterRole == "SOLVER" }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (!hasSolverRated) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SomadhanOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = SomadhanOrange,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "এই ইউজারকে রেটিং দিন",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = SomadhanTextPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ব্যবহারকারী ${currentProblem.userName}-এর সাথে কাজের অভিজ্ঞতা কেমন ছিল?",
                                            fontSize = 12.sp,
                                            color = SomadhanTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Star Rating Bar
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            (1..5).forEach { star ->
                                                IconButton(onClick = { solverRatingStars = star }) {
                                                    Icon(
                                                        imageVector = if (star <= solverRatingStars) Icons.Filled.Star else Icons.Outlined.Star,
                                                        contentDescription = "$star স্টার",
                                                        tint = if (star <= solverRatingStars) SomadhanYellowVerified else SomadhanTextHint,
                                                        modifier = Modifier.size(30.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = solverReviewComment,
                                            onValueChange = { solverReviewComment = it },
                                            placeholder = { Text("একটি মন্তব্য লিখুন (ঐচ্ছিক)...", fontSize = 13.sp, color = SomadhanTextHint) },
                                            minLines = 2,
                                            maxLines = 4,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = SomadhanOrange,
                                                unfocusedBorderColor = SomadhanBorder,
                                                focusedContainerColor = SomadhanBg,
                                                unfocusedContainerColor = SomadhanCardBg
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                viewModel.submitSolverRatingForUser(
                                                    problem = currentProblem,
                                                    stars = solverRatingStars,
                                                    comment = solverReviewComment
                                                )
                                            },
                                            enabled = solverRatingStars > 0,
                                            colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("submit_solver_rating_button")
                                        ) {
                                            Text("রেটিং জমা দিন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }

                                        if (solverRatingStars == 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "রেটিং দিতে অন্তত ১টি স্টার সিলেক্ট করুন",
                                                fontSize = 11.sp,
                                                color = SomadhanTextHint,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SomadhanBorder, RoundedCornerShape(10.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SomadhanSuccess, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "আপনি এই ইউজারকে রেটিং প্রদান করেছেন।",
                                            fontSize = 12.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ONLINE GATEWAY PAYMENTS & RECEIPT CARD FOR THIS JOB
                    if (problemGatewayPayments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .testTag("job_gateway_payments_card")
                        ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = SomadhanSuccess,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "অনলাইন গেটওয়ে পেমেন্ট ইতিহাস",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanTextPrimary
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFDCFCE7))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${DistanceUtil.toBengaliDigits(problemGatewayPayments.size.toString())}টি পেমেন্ট",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanSuccess
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    problemGatewayPayments.forEach { gw ->
                                        val gwBrandColor = when (gw.gateway.uppercase()) {
                                            "BKASH" -> Color(0xFFE2136E)
                                            "NAGAD" -> Color(0xFFF7941D)
                                            "ROCKET" -> Color(0xFF8C3494)
                                            else -> Color(0xFF1D4ED8)
                                        }
                                        val gwLightBg = when (gw.gateway.uppercase()) {
                                            "BKASH" -> Color(0xFFFDF2F8)
                                            "NAGAD" -> Color(0xFFFFF7ED)
                                            "ROCKET" -> Color(0xFFFAF5FF)
                                            else -> Color(0xFFEFF6FF)
                                        }
                                        val purposeText = when (gw.purpose) {
                                            "ESCROW_PAYMENT" -> "বিড পেমেন্ট (এসক্রো)"
                                            "ADDITIONAL_CHARGE" -> "অতিরিক্ত বিল পেমেন্ট"
                                            "RELEASE_EXTRA" -> "রিলিজ অতিরিক্ত বিল"
                                            "DIRECT_PAYMENT" -> "সরাসরি চুক্তি পেমেন্ট"
                                            else -> gw.purpose
                                        }
                                        val gwWalletPortion = walletPortionFor(gw)
                                        val gwShowSplit = gwWalletPortion > 0.5

                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .border(1.dp, gwBrandColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                                .clickable { selectedGatewayReceiptDetail = gw }
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(gwBrandColor)
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = gw.gateway.uppercase(),
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(gwLightBg)
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = purposeText,
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = gwBrandColor
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = Formatters.formatTaka(gw.amount),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SomadhanSuccess
                                                    )
                                                }

                                                // Compact wallet+gateway split line -- only for a mixed-source
                                                // "অতিরিক্ত বিল" payment; keeps to one small line, no extra card.
                                                if (gwShowSplit) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "ওয়ালেট ৳${DistanceUtil.toBengaliDigits(gwWalletPortion)} + গেটওয়ে ৳${DistanceUtil.toBengaliDigits(gw.amount)} = সর্বমোট ৳${DistanceUtil.toBengaliDigits(gwWalletPortion + gw.amount)}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = SomadhanTextSecondary
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(SomadhanCardBg)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.clickable {
                                                            clipboardManager.setText(AnnotatedString(gw.gatewayTrxId))
                                                            Toast.makeText(context, "ট্রানজেকশন আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "TrxID: ${gw.gatewayTrxId}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = gwBrandColor
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.ContentCopy,
                                                            contentDescription = "কপি করুন",
                                                            tint = gwBrandColor,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.clickable { selectedGatewayReceiptDetail = gw }
                                                    ) {
                                                        Text(
                                                            text = "রসিদ দেখুন",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF1D4ED8)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    // ONGOING NOTICE FOR NON-WINNING SOLVERS / VISITORS
                    if (!isOwner && !readOnly && !currentProblem.isDirectContract && currentProblem.status == "IN_PROGRESS" && currentProblem.acceptedSolverId != currentUser?.id) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = SomadhanOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "কাজটি বর্তমানে চলমান রয়েছে",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "এই কাজের জন্য একজন সমাধানকারীকে নির্বাচিত করা হয়েছে এবং কাজটি বর্তমানে সমাধান প্রক্রিয়ার মধ্যে রয়েছে।",
                                        fontSize = 11.5.sp,
                                        color = SomadhanTextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // SOLVER BIDDING SECTION
                    if (isSolver && currentProblem.status == "OPEN" && !isOwner && !readOnly && !currentProblem.isDirectContract) {
                        Spacer(modifier = Modifier.height(18.dp))

                        val solverLat = if (liveLocation.latitude != 0.0) liveLocation.latitude else (currentUser?.latitude ?: 0.0)
                        val solverLon = if (liveLocation.longitude != 0.0) liveLocation.longitude else (currentUser?.longitude ?: 0.0)
                        val effectiveRadius = if (currentProblem.isInstantJob) {
                            val cat = allCategories.find { it.id == currentProblem.categoryId }
                            cat?.instantJobRadiusKm ?: 5.0
                        } else {
                            physicalRadius
                        }
                        val radiusFormattedBengali = DistanceUtil.toBengaliDigits(
                            if (effectiveRadius % 1.0 == 0.0) effectiveRadius.toInt().toString() else effectiveRadius.toString()
                        )
                        val bidEligibility = remember(currentProblem, currentUser, solverLat, solverLon, userBid, effectiveRadius) {
                            viewModel.checkBidEligibility(
                                problem = currentProblem,
                                solver = currentUser,
                                solverLat = solverLat,
                                solverLon = solverLon,
                                userBid = userBid,
                                radiusKm = effectiveRadius
                            )
                        }

                        when (bidEligibility.reason) {
                            BidBlockReason.CATEGORY_MISMATCH -> {
                                // Category not matched
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = SomadhanInfo, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("ক্যাটাগরি ম্যাচ করছে না", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "এই পোস্টটি আপনার নির্বাচিত স্কিল ক্যাটাগরির সাথে মিলে না। আপনি আপনার প্রোফাইলে নির্বাচিত স্কিল ক্যাটাগরির কাজেই বিড করতে পারবেন।",
                                                fontSize = 12.sp,
                                                color = SomadhanTextSecondary,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }
                            BidBlockReason.LOCATION_UNAVAILABLE -> {
                                // Physical post but solver location unavailable
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocationOff, contentDescription = null, tint = SomadhanError, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("আপনার অবস্থান নির্ধারণ করা যাচ্ছে না", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "ফিজিক্যাল কাজে বিড করার জন্য আপনার বর্তমান অবস্থান প্রয়োজন। দয়া করে লোকেশন চালু করে আবার চেষ্টা করুন।",
                                                fontSize = 12.sp,
                                                color = SomadhanTextSecondary,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }
                            BidBlockReason.TOO_FAR -> {
                                // Physical/Instant post exceeds radius limit
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocationOff, contentDescription = null, tint = SomadhanError, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (currentProblem.isInstantJob) "জরুরি সেবার রাডার সীমার বাইরে (${radiusFormattedBengali} কিমি)" else "এই কাজটি আপনার ${radiusFormattedBengali} কিমি এলাকার বাইরে",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SomadhanTextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (currentProblem.isInstantJob) {
                                                    "আপনি এই তাৎক্ষণিক জরুরি কাজের ${radiusFormattedBengali} কিমি রাডার সীমার বাইরে অবস্থান করছেন। বিড করতে হলে এর সীমার মধ্যে থাকতে হবে।"
                                                } else {
                                                    "ফিজিক্যাল কাজের ক্ষেত্রে আপনি আপনার বর্তমান অবস্থান থেকে সর্বোচ্চ ${radiusFormattedBengali} কিমি দূরের কাজেই বিড করতে পারবেন।"
                                                },
                                                fontSize = 12.sp,
                                                color = SomadhanTextSecondary,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }
                            BidBlockReason.KYC_REQUIRED -> {
                                // Locked bidding for unverified solvers
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanErrorLight),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SomadhanError.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .clickable { onNavigateToKyc() }
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = SomadhanError, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("বিড করার অপশন লক রয়েছে", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SomadhanError)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "বিড জমা দিতে KYC ভেরিফিকেশন সম্পন্ন করা আবশ্যক। অনুগ্রহ করে এখানে ট্যাপ করে KYC ফর্ম পূরণ করুন।",
                                            fontSize = 12.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }
                                }
                            }
                            BidBlockReason.CANCELLED_PREVIOUSLY -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanErrorLight),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SomadhanError.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Block, contentDescription = null, tint = SomadhanError, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("আপনি এই কাজ বাতিল করেছেন", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SomadhanError)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "পূর্বে এই কাজটির চুক্তি বাতিল করার কারণে আপনি এই কাজে পুনরায় বিড করতে পারবেন না।",
                                                fontSize = 12.sp,
                                                color = SomadhanTextSecondary,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }
                            BidBlockReason.ALREADY_BID -> {
                                if (userBid != null) {
                                    // Solver has already placed bid
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight),
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
                                                    Text("আপনার জমাকৃত বিড", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SomadhanOrange)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    AccountStatusIndicator(
                                                        isBanned = currentUser?.isBannedSolver == true,
                                                        isRestricted = currentUser?.isRestrictedSolver == true
                                                    )
                                                }
                                                StatusBadge(status = userBid.status)
                                            }
                                            if (userBid.status == "PENDING") {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.AccessTime,
                                                        contentDescription = null,
                                                        tint = SomadhanTextSecondary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "আপনার বিড জমা হয়েছে, ইউজারের সিদ্ধান্তের অপেক্ষায়",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = SomadhanTextSecondary
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("বিড মূল্য: ৳ ${userBid.amount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                                            Text("সময়: ${userBid.estimatedTime}", fontSize = 12.sp, color = SomadhanTextSecondary)
                                            Text("বার্তা: ${userBid.message}", fontSize = 12.sp, color = SomadhanTextSecondary)
                                        }
                                    }
                                }
                            }
                            else -> {
                                if (bidEligibility.canBid) {
                                if (currentProblem.isInstantJob) {
                                    // জরুরি (instant) post: an eligible solver only sees an enabled
                                    // "বিড দিন" button here. Tapping it navigates to the জরুরি পোস্ট
                                    // হাব (InstantJobsScreen) with this post's bid popup opened
                                    // automatically — the actual bid form only appears there.
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, SomadhanOrange, RoundedCornerShape(14.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Bolt, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(22.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("আপনি এই জরুরি কাজে বিড করার জন্য উপযুক্ত", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "নিচের বাটনে ক্লিক করলে আপনাকে জরুরি পোস্ট হাবে নিয়ে যাওয়া হবে এবং এই পোস্টের বিড ফর্ম স্বয়ংক্রিয়ভাবে খুলে যাবে।",
                                                fontSize = 12.sp,
                                                color = SomadhanTextSecondary,
                                                lineHeight = 17.sp
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    onNavigate(Screen.InstantJobsBid.createRoute(currentProblem.id))
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(46.dp)
                                                    .testTag("go_to_instant_bid_button")
                                            ) {
                                                Text("বিড দিন", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                } else {
                                    // Active bidding form for verified eligible solver
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, SomadhanOrange, RoundedCornerShape(14.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("এই সমস্যায় বিড করুন", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                AccountStatusIndicator(
                                                    isBanned = currentUser?.isBannedSolver == true,
                                                    isRestricted = currentUser?.isRestrictedSolver == true
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = bidAmountStr,
                                                    onValueChange = { input ->
                                                        if (input.all { it.isDigit() }) {
                                                            bidAmountStr = input
                                                            bidError = null
                                                        }
                                                    },
                                                    label = { Text("আপনার অফার (৳)") },
                                                    placeholder = { Text("যেমন: 500") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = SomadhanOrange,
                                                        unfocusedBorderColor = SomadhanBorder,
                                                        focusedContainerColor = SomadhanBg,
                                                        unfocusedContainerColor = SomadhanCardBg
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1.1f).testTag("bid_amount_input")
                                                )

                                                OutlinedTextField(
                                                    value = bidTimeValue,
                                                    onValueChange = { input ->
                                                        if (input.all { it.isDigit() }) {
                                                            bidTimeValue = input
                                                            bidError = null
                                                        }
                                                    },
                                                    label = { Text("সময়") },
                                                    placeholder = { Text("যেমন: ২") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = SomadhanOrange,
                                                        unfocusedBorderColor = SomadhanBorder,
                                                        focusedContainerColor = SomadhanBg,
                                                        unfocusedContainerColor = SomadhanCardBg
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(0.9f).testTag("bid_time_input")
                                                )

                                                ExposedDropdownMenuBox(
                                                    expanded = bidTimeUnitExpanded,
                                                    onExpandedChange = { bidTimeUnitExpanded = it },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    OutlinedTextField(
                                                        value = bidTimeUnit,
                                                        onValueChange = {},
                                                        readOnly = true,
                                                        label = { Text("একক") },
                                                        trailingIcon = {
                                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = bidTimeUnitExpanded)
                                                        },
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = SomadhanOrange,
                                                            unfocusedBorderColor = SomadhanBorder,
                                                            focusedContainerColor = SomadhanBg,
                                                            unfocusedContainerColor = SomadhanCardBg
                                                        ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier
                                                            .menuAnchor()
                                                            .fillMaxWidth()
                                                            .testTag("bid_time_unit_dropdown")
                                                    )

                                                    ExposedDropdownMenu(
                                                        expanded = bidTimeUnitExpanded,
                                                        onDismissRequest = { bidTimeUnitExpanded = false }
                                                    ) {
                                                        listOf("ঘন্টা", "দিন").forEach { unit ->
                                                            DropdownMenuItem(
                                                                text = { Text(unit) },
                                                                onClick = {
                                                                    bidTimeUnit = unit
                                                                    bidTimeUnitExpanded = false
                                                                    bidError = null
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            val wordCount = remember(bidMessage) {
                                                if (bidMessage.isBlank()) 0
                                                else bidMessage.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
                                            }
                                            val isWordCountValid = wordCount in 5..20

                                            OutlinedTextField(
                                                value = bidMessage,
                                                onValueChange = { bidMessage = it; bidError = null },
                                                label = { Text("আপনার প্রস্তাব ও কাজের বর্ণনা") },
                                                placeholder = { Text("কীভাবে কাজটি সমাধান করবেন লিখুন (সর্বনিম্ন ৫ শব্দ)...") },
                                                minLines = 2,
                                                maxLines = 3,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = SomadhanOrange,
                                                    unfocusedBorderColor = SomadhanBorder,
                                                    focusedContainerColor = SomadhanBg,
                                                    unfocusedContainerColor = SomadhanCardBg
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().testTag("bid_msg_input")
                                            )

                                            // Live Word Counter Indicator
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (wordCount > 0 && wordCount < 5) {
                                                        "সর্বনিম্ন ৫টি শব্দ প্রয়োজন"
                                                    } else if (wordCount > 20) {
                                                        "সর্বোচ্চ ২০টি শব্দের সীমা অতিক্রম করেছে"
                                                    } else {
                                                        "সর্বনিম্ন ৫ ও সর্বোচ্চ ২০ শব্দ"
                                                    },
                                                    fontSize = 11.sp,
                                                    color = if (wordCount > 0 && !isWordCountValid) SomadhanError else SomadhanTextSecondary
                                                )

                                                val countColor = when {
                                                    wordCount > 20 -> SomadhanError
                                                    wordCount in 5..20 -> SomadhanSuccess
                                                    else -> SomadhanTextSecondary
                                                }

                                                Text(
                                                    text = "${DistanceUtil.toBengaliDigits(wordCount.toString())} / ২০ শব্দ",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isWordCountValid) FontWeight.Bold else FontWeight.Normal,
                                                    color = countColor
                                                )
                                            }

                                            val enteredBidAmount = bidAmountStr.toDoubleOrNull() ?: 0.0
                                            if (enteredBidAmount > 0) {
                                                val commAmount = enteredBidAmount * (platformCommissionPercent / 100.0)
                                                val netTakeHome = enteredBidAmount - commAmount
                                                val commPctStr = DistanceUtil.toBengaliDigits(platformCommissionPercent.toInt().toString())
                                                val commAmtStr = DistanceUtil.toBengaliDigits(commAmount.toInt().toString())
                                                val netAmtStr = DistanceUtil.toBengaliDigits(netTakeHome.toInt().toString())

                                                Spacer(modifier = Modifier.height(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(SomadhanOrange.copy(alpha = 0.08f))
                                                        .border(1.dp, SomadhanOrange.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "প্ল্যাটফর্ম ফি ($commPctStr%): ৳$commAmtStr",
                                                            fontSize = 11.sp,
                                                            color = SomadhanTextSecondary
                                                        )
                                                        Text(
                                                            text = "আপনি পাবেন: ৳$netAmtStr",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SomadhanOrange
                                                        )
                                                    }
                                                }
                                            }

                                            if (bidError != null) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(text = bidError ?: "", fontSize = 11.sp, color = SomadhanError)
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Button(
                                                onClick = {
                                                    if (currentUser?.isRestrictedSolver == true) {
                                                        bidError = "আপনার একাউন্টটি সাময়িকভাবে রেস্ট্রিক্ট করা হয়েছে। বিড জমা দেওয়া যাবে না।"
                                                        return@Button
                                                    }
                                                    val amount = bidAmountStr.toDoubleOrNull() ?: 0.0
                                                    if (amount <= 0) {
                                                        bidError = "সঠিক অফার মূল্য লিখুন (০-এর বেশি)।"
                                                        return@Button
                                                    }
                                                    val timeNum = bidTimeValue.trim().toIntOrNull() ?: 0
                                                    if (timeNum <= 0) {
                                                        bidError = "সঠিক আনুমানিক সময় লিখুন (০-এর বেশি)।"
                                                        return@Button
                                                    }
                                                    if (wordCount < 5 || wordCount > 20) {
                                                        bidError = "প্রস্তাবনায় সর্বনিম্ন ৫টি ও সর্বোচ্চ ২০টি শব্দ থাকতে হবে (বর্তমানে ${DistanceUtil.toBengaliDigits(wordCount.toString())}টি শব্দ)।"
                                                        return@Button
                                                    }
                                                    val formattedEstimatedTime = "${DistanceUtil.toBengaliDigits(bidTimeValue.trim())} $bidTimeUnit"
                                                    isPlacingBid = true
                                                    viewModel.placeBid(
                                                        problem = currentProblem,
                                                        amount = amount,
                                                        message = bidMessage.trim(),
                                                        estimatedTime = formattedEstimatedTime,
                                                        onSuccess = {
                                                            isPlacingBid = false
                                                            bidAmountStr = ""
                                                            bidMessage = ""
                                                            bidTimeValue = ""
                                                            bidTimeUnit = "ঘন্টা"
                                                            bidError = null
                                                        },
                                                        onError = {
                                                            isPlacingBid = false
                                                            bidError = it
                                                        }
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                                                shape = RoundedCornerShape(8.dp),
                                                enabled = !isPlacingBid,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(46.dp)
                                                    .testTag("submit_bid_button")
                                            ) {
                                                if (isPlacingBid) {
                                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                                } else {
                                                    Text("বিড জমা দিন", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

                if (!currentProblem.isDirectContract) {
                    val winningBid = bids.find { it.id == currentProblem.acceptedBidId || it.status == "ACCEPTED" }
                        ?: if (!currentProblem.acceptedSolverId.isNullOrBlank() && (currentProblem.status == "IN_PROGRESS" || currentProblem.status == "COMPLETED")) {
                            BidEntity(
                                id = currentProblem.acceptedBidId ?: "ACCEPTED_BID_${currentProblem.id}",
                                problemId = currentProblem.id,
                                solverId = currentProblem.acceptedSolverId ?: "",
                                solverName = currentProblem.acceptedSolverName ?: "নির্বাচিত সমাধানকারী",
                                solverPhone = "",
                                amount = currentProblem.acceptedAmount ?: 0.0,
                                message = "গৃহীত ও নির্ধারিত সমাধান চুক্তি",
                                estimatedTime = "নির্ধারিত সময়",
                                status = "ACCEPTED",
                                createdAt = currentProblem.lastActivityAt ?: currentProblem.createdAt
                            )
                        } else null

                    val pendingBids = bids.filter { it.id != winningBid?.id && it.status != "ACCEPTED" }

                    if (winningBid != null) {
                        item {
                            PulsingValue(isUpdating = bidsPulse) {
                                Column {
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "বিজয়ী বিড",
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "বিজয়ী বিড (নির্বাচিত সমাধানকারী)",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    WinningBidPinnedCard(
                                        bid = winningBid,
                                        accentColor = brandColor,
                                        onSolverClick = {
                                            if (currentUser?.id != winningBid.solverId && winningBid.solverId.isNotBlank()) {
                                                onNavigate(Screen.PublicProfile.createRoute(winningBid.solverId, "SOLVER"))
                                            }
                                        },
                                        solverPhotoUri = allUsers.find { it.id == winningBid.solverId }?.profileImageUri
                                    )
                                }
                            }
                        }
                    }

                    item {
                        PulsingValue(isUpdating = bidsPulse) {
                            Column {
                                Spacer(modifier = Modifier.height(18.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (winningBid != null) {
                                            "অন্যান্য বিড (অপেক্ষমাণ) (${DistanceUtil.toBengaliDigits(pendingBids.size.toString())})"
                                        } else {
                                            "সকল বিড (${DistanceUtil.toBengaliDigits(bids.size.toString())})"
                                        },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    if (winningBid == null && bids.isEmpty()) {
                        item {
                            PulsingValue(isUpdating = bidsPulse) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "এখনও কোনো বিড জমা পড়েনি।",
                                            fontSize = 13.sp,
                                            color = SomadhanTextHint
                                        )
                                    }
                                }
                            }
                        }
                    } else if (winningBid != null && pendingBids.isEmpty()) {
                        item {
                            PulsingValue(isUpdating = bidsPulse) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "অন্য কোনো অপেক্ষমাণ বিড নেই।",
                                            fontSize = 13.sp,
                                            color = SomadhanTextHint
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val bidsToDisplay = if (winningBid != null) pendingBids else bids
                        items(bidsToDisplay, key = { it.id }) { bid ->
                            PulsingValue(isUpdating = bidsPulse) {
                                BidCard(
                                    bid = bid,
                                    isProblemOwner = isOwner,
                                    isBidOwner = currentUser?.id == bid.solverId,
                                    isProblemOpen = currentProblem.status == "OPEN",
                                    accentColor = brandColor,
                                    onAcceptBid = {
                                        bidToAcceptWithWalletConfirm = bid
                                    },
                                    onWithdrawBid = null,
                                    onSolverClick = {
                                        if (currentUser?.id != bid.solverId) {
                                            onNavigate(Screen.PublicProfile.createRoute(bid.solverId, "SOLVER"))
                                        }
                                    },
                                    solverPhotoUri = allUsers.find { it.id == bid.solverId }?.profileImageUri
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
            } // SyncAwareContent বন্ধ
        } // SomadhanPullToRefresh বন্ধ
    }

    // Gateway Payment Receipt Dialog for Problem Detail
    if (selectedGatewayReceiptDetail != null) {
        val item = selectedGatewayReceiptDetail!!
        val brandColor = when (item.gateway.uppercase()) {
            "BKASH" -> Color(0xFFE2136E)
            "NAGAD" -> Color(0xFFF7941D)
            "ROCKET" -> Color(0xFF8C3494)
            else -> Color(0xFF1D4ED8)
        }
        val lightBg = when (item.gateway.uppercase()) {
            "BKASH" -> Color(0xFFFDF2F8)
            "NAGAD" -> Color(0xFFFFF7ED)
            "ROCKET" -> Color(0xFFFAF5FF)
            else -> Color(0xFFEFF6FF)
        }
        val gatewayDisplayName = when (item.gateway.uppercase()) {
            "BKASH" -> "bKash (বিকাশ)"
            "NAGAD" -> "Nagad (নগদ)"
            "ROCKET" -> "Rocket (রকেট)"
            else -> item.gateway
        }
        val purposeText = when (item.purpose) {
            "WALLET_DEPOSIT" -> "ওয়ালেট রিচার্জ (টপ-আপ)"
            "ESCROW_PAYMENT" -> "কাজের এসক্রো পেমেন্ট (বিড গ্রহণ)"
            "ADDITIONAL_CHARGE" -> "অতিরিক্ত বিল পেমেন্ট"
            "RELEASE_EXTRA" -> "রিলিজ অতিরিক্ত বিল"
            "DIRECT_PAYMENT" -> "সরাসরি চুক্তি পেমেন্ট"
            else -> item.purpose
        }
        val itemWalletPortion = walletPortionFor(item)
        val itemShowSplit = itemWalletPortion > 0.5

        val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

        ModalBottomSheet(
            onDismissRequest = { selectedGatewayReceiptDetail = null },
            sheetState = detailSheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header with title, gateway badge and Close (✕) icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(lightBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = brandColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "অনলাইন পেমেন্ট রসিদ",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(lightBg)
                                .border(0.5.dp, brandColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = gatewayDisplayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = brandColor
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { selectedGatewayReceiptDetail = null },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "বন্ধ করুন",
                                tint = SomadhanTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Status & Total Amount Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (itemShowSplit) "গেটওয়ে থেকে পরিশোধিত" else "মোট পরিশোধিত অর্থ",
                                    fontSize = 12.sp,
                                    color = SomadhanTextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = Formatters.formatTaka(item.amount),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanSuccess
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SomadhanSuccess.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SomadhanSuccess,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "পরিশোধিত ও যাচাইকৃত",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                }
                            }
                        }

                        // Compact wallet+gateway split -- only when this "অতিরিক্ত বিল" was
                        // partly covered from the wallet balance; single slim row, no extra card.
                        if (itemShowSplit) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFBBF7D0))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ওয়ালেট থেকে: ৳${DistanceUtil.toBengaliDigits(itemWalletPortion)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1D4ED8)
                                )
                                Text(
                                    text = "সর্বমোট: ৳${DistanceUtil.toBengaliDigits(itemWalletPortion + item.amount)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Details Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "রসিদ সংক্রান্ত বিস্তারিত তথ্য",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )

                        HorizontalDivider(color = SomadhanDivider)

                        // Purpose
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("উদ্দেশ্য", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                            Text(
                                text = purposeText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanTextPrimary
                            )
                        }

                        // Problem Title
                        if (item.problemTitle.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("কাজের শিরোনাম", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.problemTitle,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanTextPrimary,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }

                        // Gateway TrxID with Copy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ট্রানজেকশন আইডি", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(lightBg)
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(item.gatewayTrxId))
                                        Toast.makeText(context, "ট্রানজেকশন আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.gatewayTrxId,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = brandColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "কপি",
                                    tint = brandColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Sender Phone
                        if (item.userPhone.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("প্রেরক একাউন্ট", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                                Text(
                                    text = item.userPhone,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanTextPrimary
                                )
                            }
                        }

                        // Reference Note
                        if (item.note.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("রেফারেন্স / নোট", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                                Text(
                                    text = item.note,
                                    fontSize = 12.5.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        }

                        // Date & Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("তারিখ ও সময়", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                            Text(
                                text = Formatters.formatDateTimeBengali(item.timestamp),
                                fontSize = 12.sp,
                                color = SomadhanTextHint
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Copy full receipt & Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val receiptText = buildString {
                                appendLine("=== সমাধান ডিজিটাল পেমেন্ট রসিদ ===")
                                appendLine("পদ্ধতি: $gatewayDisplayName")
                                appendLine("পরিমাণ: ${Formatters.formatTaka(item.amount)}")
                                appendLine("উদ্দেশ্য: $purposeText")
                                if (item.problemTitle.isNotBlank()) appendLine("কাজের শিরোনাম: ${item.problemTitle}")
                                appendLine("TrxID: ${item.gatewayTrxId}")
                                if (item.userPhone.isNotBlank()) appendLine("প্রেরক: ${item.userPhone}")
                                appendLine("তারিখ: ${Formatters.formatDateTimeBengali(item.timestamp)}")
                                appendLine("স্ট্যাটাস: সফল ও যাচাইকৃত")
                            }
                            clipboardManager.setText(AnnotatedString(receiptText))
                            Toast.makeText(context, "সম্পূর্ণ রসিদ কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("রসিদ কপি", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { selectedGatewayReceiptDetail = null },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("বন্ধ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun BidCard(
    bid: BidEntity,
    isProblemOwner: Boolean,
    isProblemOpen: Boolean,
    onAcceptBid: () -> Unit,
    accentColor: Color = Color(0xFF1D4ED8),
    onSolverClick: () -> Unit = {},
    solverPhotoUri: String? = null,
    isBidOwner: Boolean = false,
    onWithdrawBid: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                1.dp,
                if (isProblemOwner && bid.status == "PENDING") accentColor.copy(alpha = 0.35f) else SomadhanDivider,
                RoundedCornerShape(12.dp)
            )
            .testTag("bid_card_${bid.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSolverClick() }
                        .padding(4.dp)
                ) {
                    UserAvatar(
                        photoUri = solverPhotoUri,
                        name = bid.solverName,
                        size = 36.dp,
                        backgroundColor = accentColor.copy(alpha = 0.12f),
                        textColor = accentColor,
                        fontSize = 14.sp,
                        borderColor = accentColor.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = bid.solverName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            UserVerificationBadge(role = "SOLVER", isKycVerified = true)
                        }
                        Text(
                            text = Formatters.formatTimeAgo(bid.createdAt),
                            fontSize = 10.sp,
                            color = SomadhanTextHint
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "৳ ${DistanceUtil.toBengaliDigits(bid.amount.toInt().toString())}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    StatusBadge(status = bid.status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = bid.message,
                fontSize = 13.sp,
                color = SomadhanTextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = SomadhanTextHint, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "সময়: ${bid.estimatedTime}",
                    fontSize = 11.sp,
                    color = SomadhanTextHint
                )
            }

            // If user is owner and problem is open, show Accept Bid button
            if (isProblemOwner && isProblemOpen && bid.status == "PENDING") {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onAcceptBid,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("accept_bid_button_${bid.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("বিড গ্রহণ করুন", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // If solver is owner of this pending bid, show Withdraw Bid button
            if (isBidOwner && bid.status == "PENDING" && onWithdrawBid != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onWithdrawBid,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanError),
                    border = BorderStroke(1.dp, SomadhanError.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("withdraw_bid_button_${bid.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = SomadhanError,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("বিড প্রত্যাহার করুন", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = SomadhanError)
                }
            }
        }
    }
}

@Composable
fun WinningBidPinnedCard(
    bid: BidEntity,
    accentColor: Color,
    onSolverClick: () -> Unit = {},
    solverPhotoUri: String? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(14.dp))
            .testTag("pinned_winning_bid_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Pinned top banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "বিজয়ী",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "পিনযুক্ত বিজয়ী বিড",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                }

                StatusBadge(status = "ACCEPTED")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSolverClick() }
                        .padding(4.dp)
                ) {
                    UserAvatar(
                        photoUri = solverPhotoUri,
                        name = bid.solverName,
                        size = 42.dp,
                        backgroundColor = Color(0xFFFDE68A),
                        textColor = Color(0xFFB45309),
                        fontSize = 16.sp,
                        borderColor = Color(0xFFF59E0B).copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = bid.solverName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            UserVerificationBadge(role = "SOLVER", isKycVerified = true)
                        }
                        Text(
                            text = Formatters.formatTimeAgo(bid.createdAt),
                            fontSize = 11.sp,
                            color = SomadhanTextHint
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "৳ ${DistanceUtil.toBengaliDigits(bid.amount.toInt().toString())}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                    Text(
                        text = "গৃহীত দর",
                        fontSize = 11.sp,
                        color = SomadhanTextHint
                    )
                }
            }

            if (bid.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = bid.message,
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 18.sp
                )
            }

            if (bid.estimatedTime.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = SomadhanTextHint, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "সময়: ${bid.estimatedTime}",
                        fontSize = 11.5.sp,
                        color = SomadhanTextHint
                    )
                }
            }
        }
    }
}
