package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.util.ImageStorageUtil
import com.example.data.entity.CategoryEntity
import com.example.data.entity.RatingEntity
import com.example.data.entity.UserEntity
import com.example.ui.components.AccountStatusIndicator
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.DetailScreenSkeleton
import com.example.ui.components.DirectContractDialog
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.ReputationBadge
import com.example.ui.components.SyncBlockedRetryState
import com.example.ui.components.rememberSessionAwareSkeletonGate
import com.example.data.remote.SupabaseRealtimeManager
import com.example.ui.navigation.Screen
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
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
import com.example.ui.theme.SomadhanYellowVerifiedBg
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.Formatters
import java.util.Locale
import com.example.ui.components.BottomSlideAlertDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PublicProfileScreen(
    userId: String,
    profileRole: String? = null,
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onSeeAllReviewsClick: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val allBids by viewModel.allBids.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    val isViewerSolver = currentUser?.role == "SOLVER"
    val brandPrimary = if (isViewerSolver) SomadhanOrange else Color(0xFF1D4ED8)
    val brandPrimaryLight = if (isViewerSolver) SomadhanOrangeLight else Color(0xFFEFF6FF)
    val brandBorder = if (isViewerSolver) SomadhanOrange.copy(alpha = 0.3f) else Color(0xFFBFDBFE)

    // Rule ২ (re-entry, flash-বিহীন target — PublicProfileScreen): userId-এর ডেটা যদি ইতিমধ্যেই
    // allUsers Flow-তে ক্যাশ থাকে (সাধারণ কেস, বিশেষত re-entry-তে), তাহলে userProfile/isLoading
    // শুরু থেকেই সেই ভ্যালু নিয়ে composition হয় — আগের মতো null → LaunchedEffect → non-null এই
    // এক-ফ্রেমের "null window" আর থাকে না, যেটার কারণে re-entry-তে full-page skeleton flash হতো।
    // key = userId, যাতে ভিন্ন প্রোফাইলে গেলে ঠিকমতো নতুন cold-load state শুরু হয়।
    val initialFoundUser = remember(userId) { allUsers.find { it.id == userId } }
    var userProfile by remember(userId) { mutableStateOf(initialFoundUser) }
    var ratings by remember(userId) { mutableStateOf<List<RatingEntity>>(emptyList()) }
    var isLoading by remember(userId) { mutableStateOf(initialFoundUser == null) }
    var canFavoriteState by remember(currentUser?.id, userId) { mutableStateOf(false) }

    var showDirectContractDialog by remember { mutableStateOf(false) }
    var isSubmittingDirectContract by remember { mutableStateOf(false) }
    var directContractError by remember { mutableStateOf<String?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }

    LaunchedEffect(currentUser?.id, userId) {
        val cur = currentUser
        if (cur != null && userId.isNotBlank() && cur.id != userId) {
            canFavoriteState = viewModel.canFavoriteSolver(userId)
        } else {
            canFavoriteState = false
        }
    }

    // User lookup from Flow or DB
    val foundUser = allUsers.find { it.id == userId }
    LaunchedEffect(userId, foundUser) {
        if (foundUser != null) {
            userProfile = foundUser
            isLoading = false
        } else {
            val dbUser = viewModel.getPublicUserById(userId)
            userProfile = dbUser
            isLoading = false
        }
    }

    // Collect ratings for this user (both as solver or client)
    LaunchedEffect(userId) {
        viewModel.getRatingsForUserFlow(userId).collect { list ->
            ratings = list
        }
    }

    // Calculations based on role and activities
    val targetUser = userProfile
    val isTargetSolver = when (profileRole?.uppercase()) {
        "SOLVER" -> true
        "USER" -> false
        else -> targetUser?.role == "SOLVER"
    }
    val isViewerAdmin = currentUser?.role == "ADMIN" || currentUser?.role == "SUPER_ADMIN"
    val shouldShowCategoriesSection = isTargetSolver && (currentUser?.role == "USER" || isViewerAdmin)

    val solverCategoryIds = remember(targetUser?.solverCategories) {
        targetUser?.solverCategories?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    }
    val solverCategoriesList = remember(solverCategoryIds, allCategories) {
        solverCategoryIds.mapNotNull { catId ->
            allCategories.find { it.id == catId }
        }
    }

    // Rating calculations
    val totalReviewsCount = ratings.size
    val averageRating = if (totalReviewsCount > 0) {
        ratings.map { it.stars }.average()
    } else {
        5.0
    }

    // Completed jobs calculation (status == "COMPLETED")
    val completedJobsCount = if (isTargetSolver) {
        allProblems.count { it.acceptedSolverId == userId && it.status == "COMPLETED" }
    } else {
        allProblems.count { it.userId == userId && it.status == "COMPLETED" }
    }

    // Accepted / Total engaged jobs
    val totalAcceptedJobsCount = if (isTargetSolver) {
        allProblems.count { it.acceptedSolverId == userId }
    } else {
        allProblems.count { it.userId == userId && (it.status == "IN_PROGRESS" || it.status == "COMPLETED" || it.acceptedSolverId != null) }
    }

    // Completion rate percentage: (সম্পন্ন কাজ ÷ মোট গৃহীত কাজ) × ১০০
    val completionRate = if (totalAcceptedJobsCount > 0) {
        ((completedJobsCount.toDouble() / totalAcceptedJobsCount.toDouble()) * 100.0).coerceIn(0.0, 100.0)
    } else if (completedJobsCount > 0) {
        100.0
    } else {
        100.0
    }

    // Sorted descending by createdAt
    val sortedReviews = remember(ratings) {
        ratings.sortedByDescending { it.createdAt }
    }
    // Recent 3 reviews preview
    val recentReviews = remember(sortedReviews) {
        sortedReviews.take(3)
    }

    // Problems map for lookup
    val problemsMap = remember(allProblems) {
        allProblems.associateBy { it.id }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "পাবলিক প্রোফাইল",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("public_profile_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = brandPrimary)
                )
                RealtimeLocationBar(
                    locationAddress = liveLocation.address,
                    onRefresh = { viewModel.refreshLiveLocation() },
                    isSolver = isViewerSolver
                )
            }
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        // সংশোধন: এই স্ক্রিনে re-entry-তে শিমার/flash *চাওয়া হচ্ছে না* — এটাই ইচ্ছাকৃত/কাঙ্ক্ষিত
        // আচরণ। flashOnReentry ডিফল্ট false-ই থাকছে (আগের মতো), তাই re-entry-তে কোনো
        // flash হবে না — শুধু প্রকৃত প্রথম cold visit-এ skeleton দেখাবে।
        val minimumSkeletonActive = rememberSessionAwareSkeletonGate(
            sessionKey = "public_profile_$userId",
            viewModel = viewModel
        )
        LaunchedEffect(isLoading, minimumSkeletonActive) {
            if (!isLoading && !minimumSkeletonActive) {
                viewModel.markLoadedOnce("public_profile_$userId")
            }
        }
        if (isLoading || minimumSkeletonActive) {
            DetailScreenSkeleton(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                tint = brandPrimary
            )
        } else if (targetUser == null && initialSyncPhase == SupabaseRealtimeManager.SyncPhase.ERROR) {
            // Loading/Sync Fix Roadmap v2, ধাপ ৪ — বাল্ক-পুল (users) ব্যর্থ হয়ে থাকলে এই
            // userId হয়তো Room-এ আসেইনি, তাই নিচের "পাওয়া যায়নি" মেসেজ ভুল/বিভ্রান্তিকর হবে —
            // এই ক্ষেত্রে সঠিক এরর+রিট্রাই UI দেখাও।
            SyncBlockedRetryState(
                onRetry = { viewModel.retryInitialSync() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else if (targetUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = SomadhanTextHint,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ব্যবহারকারীর তথ্য পাওয়া যায়নি",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ইউজার আইডি: $userId",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("public_profile_content"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Profile Info (Read-Only)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("public_profile_header_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(brandPrimaryLight)
                                    .border(2.5.dp, brandPrimary, CircleShape)
                                    .testTag("public_profile_avatar"),
                                contentAlignment = Alignment.Center
                            ) {
                                val imageUri = targetUser.profileImageUri
                                val isPhotoValid = ImageStorageUtil.isValidDisplayUri(imageUri)
                                if (isPhotoValid) {
                                    var loadFailed by remember(imageUri) { mutableStateOf(false) }
                                    if (!loadFailed) {
                                        AsyncImage(
                                            model = imageUri,
                                            contentDescription = "প্রোফাইল ছবি",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                            onError = { loadFailed = true }
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "প্রোফাইল ছবি",
                                            tint = brandPrimary,
                                            modifier = Modifier.size(54.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "প্রোফাইল ছবি",
                                        tint = brandPrimary,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Name with Verification Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = targetUser.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false).testTag("public_profile_name")
                                )
                                if ((if (isTargetSolver) targetUser.verifiedBadgeSolver else targetUser.verifiedBadgeUser) || targetUser.isKycVerified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "ভেরিফায়েড ব্যাজ",
                                        tint = brandPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                ReputationBadge(
                                    score = targetUser.reputationScore,
                                    modifier = Modifier.clickable {
                                        onNavigate(Screen.ReputationDetail.createRoute(targetUser.id, if (isTargetSolver) "SOLVER" else "USER"))
                                    }
                                )
                                val targetBanned = if (isTargetSolver) targetUser.isBannedSolver else targetUser.isBannedUser
                                val targetRestricted = if (isTargetSolver) targetUser.isRestrictedSolver else targetUser.isRestrictedUser
                                if (targetBanned || targetRestricted) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    AccountStatusIndicator(
                                        isBanned = targetBanned,
                                        isRestricted = targetRestricted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Role Pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isTargetSolver) SomadhanOrangeLight else Color(0xFFEFF6FF))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isTargetSolver) Icons.Default.Handyman else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isTargetSolver) SomadhanOrange else Color(0xFF1D4ED8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTargetSolver) "সমাধানকারী (Solver)" else "ক্লায়েন্ট (User)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isTargetSolver) SomadhanOrange else Color(0xFF1D4ED8)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Average Rating Stars Bar
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.testTag("public_profile_rating_row")
                            ) {
                                for (i in 1..5) {
                                    val isFilled = i <= averageRating.toInt()
                                    Icon(
                                        imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.Star,
                                        contentDescription = null,
                                        tint = if (isFilled) SomadhanYellowVerified else SomadhanTextHint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f", averageRating),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                                Text(
                                    text = " (${DistanceUtil.toBengaliDigits(totalReviewsCount.toString())} টি রিভিউ)",
                                    fontSize = 13.sp,
                                    color = SomadhanTextSecondary
                                )
                            }

                            if (canFavoriteState) {
                                val isFav = currentUser?.favoriteSolverIds?.split(",")?.map { it.trim() }?.contains(userId) == true
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (isFav) {
                                            viewModel.showToast("এই সমাধানকারী ইতোমধ্যেই আপনার পছন্দের তালিকায় যুক্ত আছেন।")
                                        } else {
                                            viewModel.addFavoriteSolver(userId)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isFav) Color(0xFFF0FDF4) else Color.Transparent,
                                        contentColor = if (isFav) Color(0xFF166534) else Color(0xFFE11D48)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isFav) Color(0xFF86EFAC) else Color(0xFFFDA4AF)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("public_profile_toggle_favorite_button")
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
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Direct Contract Offer button (Clients to Solvers when viewing another user)
                            if (currentUser != null && currentUser?.id != userId) {
                                if (isTargetSolver && currentUser?.role != "SOLVER") {
                                    val isFav = currentUser?.favoriteSolverIds?.split(",")?.map { it.trim() }?.contains(userId) == true
                                    val solverCats = targetUser?.solverCategories?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                                    val matchingVirtualCount = activeCategories.count { !it.isPhysical && solverCats.contains(it.id) }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (isFav) {
                                        if (matchingVirtualCount > 0) {
                                            Button(
                                                onClick = { showDirectContractDialog = true },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("public_profile_direct_assign_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Assignment,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "সরাসরি কাজের প্রস্তাব দিন",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        } else {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFFFFFBEB),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Info,
                                                        contentDescription = null,
                                                        tint = Color(0xFFD97706),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "সমাধানকারীর প্রোফাইলে কোনো সরাসরি অ্যাসাইনযোগ্য ভার্চুয়াল ক্যাটাগরি নেই।",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF92400E),
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                if (canFavoriteState) {
                                                    viewModel.toggleFavoriteSolver(userId)
                                                } else {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "সরাসরি কাজের প্রস্তাব পাঠাতে সমাধানকারীকে আগে পছন্দের তালিকায় যুক্ত করতে হবে।",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color(0xFFF8FAFC),
                                                contentColor = SomadhanTextSecondary
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("public_profile_direct_assign_disabled_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = SomadhanTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "🔒 সরাসরি প্রস্তাব (পছন্দের তালিকায় থাকা আবশ্যক)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SomadhanTextSecondary
                                            )
                                        }
                                    }
                                }

                                // Report User affordance
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    TextButton(
                                        onClick = { showReportDialog = true },
                                        modifier = Modifier.testTag("public_profile_report_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = SomadhanTextHint,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "রিপোর্ট / অভিযোগ করুন",
                                            fontSize = 11.sp,
                                            color = SomadhanTextHint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Service Categories Section (Only visible when visitor is USER or ADMIN viewing a Solver)
                if (shouldShowCategoriesSection) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("public_profile_services_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "সেবার ক্যাটাগরি",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                    if (solverCategoriesList.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${DistanceUtil.toBengaliDigits(solverCategoriesList.size.toString())} টি)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SomadhanTextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (solverCategoriesList.isEmpty()) {
                                    Text(
                                        text = "কোনো সেবার ক্যাটাগরি যুক্ত করা হয়নি",
                                        fontSize = 13.sp,
                                        color = SomadhanTextHint,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    androidx.compose.foundation.layout.BoxWithConstraints(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val isCompact = maxWidth < 360.dp
                                        val chipSpacing = if (isCompact) 6.dp else 8.dp

                                        if (solverCategoriesList.size == 1) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                PublicProfileCategoryChip(
                                                    category = solverCategoriesList[0],
                                                    isCompact = isCompact
                                                )
                                            }
                                        } else if (solverCategoriesList.size == 2) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(chipSpacing, Alignment.CenterHorizontally),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                PublicProfileCategoryChip(
                                                    category = solverCategoriesList[0],
                                                    modifier = Modifier.weight(1f, fill = false),
                                                    isCompact = isCompact
                                                )
                                                PublicProfileCategoryChip(
                                                    category = solverCategoriesList[1],
                                                    modifier = Modifier.weight(1f, fill = false),
                                                    isCompact = isCompact
                                                )
                                            }
                                        } else if (solverCategoriesList.size == 3) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(chipSpacing),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(chipSpacing, Alignment.CenterHorizontally),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    PublicProfileCategoryChip(
                                                        category = solverCategoriesList[0],
                                                        modifier = Modifier.weight(1f, fill = false),
                                                        isCompact = isCompact
                                                    )
                                                    PublicProfileCategoryChip(
                                                        category = solverCategoriesList[1],
                                                        modifier = Modifier.weight(1f, fill = false),
                                                        isCompact = isCompact
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    PublicProfileCategoryChip(
                                                        category = solverCategoriesList[2],
                                                        modifier = Modifier.weight(1f, fill = false),
                                                        isCompact = isCompact
                                                    )
                                                }
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(chipSpacing),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                solverCategoriesList.chunked(2).forEach { rowCategories ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(chipSpacing, Alignment.CenterHorizontally),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        rowCategories.forEach { cat ->
                                                            PublicProfileCategoryChip(
                                                                category = cat,
                                                                modifier = if (rowCategories.size > 1) Modifier.weight(1f, fill = false) else Modifier,
                                                                isCompact = isCompact
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
                }

                // Stats Cards: Completed Jobs & Completion Rate
                item {
                    Text(
                        text = "কাজের পরিসংখ্যান",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Completed Jobs
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("public_profile_completed_jobs_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SomadhanSuccessLight),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanSuccess.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(SomadhanSuccess.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TaskAlt,
                                        contentDescription = null,
                                        tint = SomadhanSuccess,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(completedJobsCount.toString())} টি",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanSuccess
                                )
                                Text(
                                    text = "মোট সম্পন্ন কাজ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }

                        // Card 2: Completion Rate %
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("public_profile_completion_rate_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = brandPrimaryLight),
                            border = androidx.compose.foundation.BorderStroke(1.dp, brandBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(brandPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = brandPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(completionRate.toInt().toString())}%",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = brandPrimary
                                )
                                Text(
                                    text = "সম্পন্ন হওয়ার হার",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    }
                }

                // Reviews Section Header with "See More" / "আরও দেখুন" affordance
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RateReview,
                                contentDescription = null,
                                tint = brandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "সাম্প্রতিক রিভিউ (${DistanceUtil.toBengaliDigits(totalReviewsCount.toString())})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }

                        if (totalReviewsCount > 3) {
                            TextButton(
                                onClick = onSeeAllReviewsClick,
                                modifier = Modifier.testTag("public_profile_see_all_reviews_btn")
                            ) {
                                Text(
                                    text = "আরও দেখুন",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = brandPrimary
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = brandPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Recent Reviews List (Preview up to 3 with problem title & category)
                if (recentReviews.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = SomadhanTextHint,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "এখনো কোনো রিভিউ পাওয়া যায়নি",
                                    fontSize = 13.sp,
                                    color = SomadhanTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(recentReviews, key = { it.id }) { review ->
                        val relatedProblem = problemsMap[review.problemId]
                        val problemTitle = if (review.problemTitle.isNotBlank()) review.problemTitle
                                           else relatedProblem?.title ?: "সমাধান কাজ"
                        val categoryName = relatedProblem?.categoryName ?: ""

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("public_profile_review_item_${review.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = review.userName.ifBlank { "ক্লায়েন্ট" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        for (i in 1..5) {
                                            val isFilled = i <= review.stars
                                              Icon(
                                                imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.Star,
                                                contentDescription = null,
                                                tint = if (isFilled) SomadhanYellowVerified else SomadhanTextHint,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                // Associated Problem Title & Category
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SomadhanBg)
                                        .border(0.5.dp, SomadhanDivider, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Work,
                                        contentDescription = null,
                                        tint = SomadhanTextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = problemTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (categoryName.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• $categoryName",
                                            fontSize = 11.sp,
                                            color = brandPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (review.comment.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = review.comment,
                                        fontSize = 13.sp,
                                        color = SomadhanTextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = Formatters.formatTimeAgo(review.createdAt),
                                    fontSize = 11.sp,
                                    color = SomadhanTextHint
                                )
                            }
                        }
                    }

                    // Bottom "See More" Button if reviews > 3
                    if (totalReviewsCount > 3) {
                        item {
                            OutlinedButton(
                                onClick = onSeeAllReviewsClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("public_profile_see_more_bottom_btn"),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, brandPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = brandPrimary)
                            ) {
                                Text(
                                    text = "সকল রিভিউ ও কাজের তালিকা দেখুন (${DistanceUtil.toBengaliDigits(totalReviewsCount.toString())})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Direct Contract Dialog
        if (showDirectContractDialog && userProfile != null) {
            val targetSolver = userProfile!!
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
                        onSuccess = { problemId ->
                            isSubmittingDirectContract = false
                            showDirectContractDialog = false
                            android.widget.Toast.makeText(context, "সরাসরি প্রজেক্ট অফার সফলভাবে পাঠানো হয়েছে!", android.widget.Toast.LENGTH_LONG).show()
                            onNavigate(Screen.Chat.createRoute(problemId))
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

        // Report User Dialog
        if (showReportDialog && userProfile != null) {
            BottomSlideAlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = {
                    Text(
                        text = "ব্যবহারকারীকে রিপোর্ট করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "আপনি কি নিশ্চিত যে ${userProfile?.name}-এর বিরুদ্ধে অভিযোগ করতে চান? সমস্যা বা আপত্তিকর আচরণের কারণ উল্লেখ করুন:",
                            fontSize = 13.sp,
                            color = SomadhanTextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = reportReason,
                            onValueChange = { reportReason = it },
                            placeholder = { Text("রিপোর্টের কারণ লিখুন...") },
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
                                    reportedUserId = userId,
                                    reason = reportReason,
                                    contextType = "PROFILE"
                                )
                                showReportDialog = false
                                reportReason = ""
                                android.widget.Toast.makeText(context, "আপনার অভিযোগটি অ্যাডমিনের নিকট জমা দেওয়া হয়েছে।", android.widget.Toast.LENGTH_LONG).show()
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
    }
}

@Composable
private fun PublicProfileCategoryChip(
    category: CategoryEntity,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val (bgColor, iconColor) = CategoryIconHelper.getCategoryColors(
        category.nameBangla,
        category.iconName
    )
    val horizontalPadding = if (isCompact) 8.dp else 12.dp
    val verticalPadding = if (isCompact) 5.dp else 7.dp
    val iconSize = if (isCompact) 14.dp else 16.dp
    val titleFontSize = if (isCompact) 11.sp else 12.5.sp
    val badgeFontSize = if (isCompact) 9.5.sp else 10.5.sp

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.3f)),
        modifier = modifier.testTag("public_profile_category_chip_${category.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            Icon(
                imageVector = CategoryIconHelper.getIcon(category.iconName),
                contentDescription = category.nameBangla,
                tint = iconColor,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(if (isCompact) 4.dp else 6.dp))
            Text(
                text = category.nameBangla,
                fontSize = titleFontSize,
                fontWeight = FontWeight.SemiBold,
                color = SomadhanTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (category.isPhysical) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "• ফিল্ড",
                    fontSize = badgeFontSize,
                    fontWeight = FontWeight.Normal,
                    color = SomadhanTextSecondary,
                    maxLines = 1
                )
            } else {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "• অনলাইন",
                    fontSize = badgeFontSize,
                    fontWeight = FontWeight.Normal,
                    color = SomadhanInfo,
                    maxLines = 1
                )
            }
        }
    }
}
