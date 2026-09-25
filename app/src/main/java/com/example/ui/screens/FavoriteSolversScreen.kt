package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.components.DirectContractDialog
import com.example.ui.components.ShimmerBlock
import com.example.ui.components.SyncAwareRefreshableContent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.util.ImageStorageUtil
import com.example.data.entity.CategoryEntity
import com.example.data.entity.UserEntity
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.ReputationBadge
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.theme.SomadhanYellowVerified
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import java.util.Locale

data class FavoriteSolverItem(
    val solver: UserEntity,
    val completedJobsTogether: Int,
    val averageRating: Double,
    val reviewsCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteSolversScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onOpenChat: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val allRatings by viewModel.allRatings.collectAsStateWithLifecycle()
    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()

    // Loading/Sync Fix Roadmap v2, dhap 4 -- initialSyncPhase (bulk-pull), separate sessionKey "favorite_solvers_sync"
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    // Loading Pattern Master Prompt, ধাপ ৫ পয়েন্ট ৩ ফিক্স — আগে এখানে একটা local
    // `remember { mutableStateOf(false) }` ছিল যেটা কখনো `true` হতো না (onRefresh শুধু
    // viewModel.refreshData() কল করতো, এই লোকাল ফ্ল্যাগ সেট করতো না) — ফলে rule ৩-এর
    // pulse কখনো trigger হতো না, যদিও pull-gesture/স্পিনার ঠিকই দেখাত। অন্য সব migrated
    // স্ক্রিনের মতোই এখন সরাসরি viewModel-এর shared `isRefreshing` StateFlow ব্যবহার করা
    // হলো, যেটা refreshData()-ই true/false করে।
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Direct Contract Dialog State
    var directContractSolver by remember { mutableStateOf<UserEntity?>(null) }
    var isSubmittingDirectContract by remember { mutableStateOf(false) }
    var directContractError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser?.favoriteSolverIds) {
        viewModel.resetFavoriteSolversPagination()
    }

    val favoriteSolversPaged = viewModel.favoriteSolversPaged
    val currentUserId = currentUser?.id ?: ""
    val favoriteItems = remember(favoriteSolversPaged.toList(), allProblems, allRatings) {
        favoriteSolversPaged.map { solver ->
            val completedCount = allProblems.count {
                it.userId == currentUserId && it.acceptedSolverId == solver.id && it.status == "COMPLETED"
            }
            val solverRatings = allRatings.filter { it.solverId == solver.id && it.raterRole == "USER" }
            val avg = if (solverRatings.isNotEmpty()) solverRatings.map { it.stars }.average() else 5.0

            FavoriteSolverItem(
                solver = solver,
                completedJobsTogether = completedCount,
                averageRating = avg,
                reviewsCount = solverRatings.size
            )
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "পছন্দের সমাধানকারী",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            if (favoriteItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.25f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = DistanceUtil.toBengaliDigits(favoriteItems.size.toString()),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("favorite_solvers_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFE11D48)
                    )
                )
                RealtimeLocationBar(
                    locationAddress = liveLocation.address,
                    onRefresh = { viewModel.refreshLiveLocation() },
                    isSolver = currentUser?.role == "SOLVER"
                )
            }
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        SomadhanPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                viewModel.refreshData()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Realtime-Aware, Structure-Preserving Refresh, ধাপ ৭ (ব্যাচ ২) —
            // `favoriteItems` ইতিমধ্যেই `remember(favoriteSolversPaged.toList(), ...)`
            // দিয়ে হিসাব করা একটা plain immutable List (উপরে দেখুন), তাই আলাদা করে
            // `.toList()` লাগছে না — প্রতিটা মিউটেশনে নতুন instance এমনিতেই তৈরি হয়,
            // `!=` diff সঠিকভাবে কাজ করবে। "load more" এ নতুন পেজ যোগ হলে তালিকা বদলে
            // যাওয়ায় সংক্ষিপ্ত শিমার হবে — ইচ্ছাকৃত (ব্যবহারকারীর সিদ্ধান্ত)।
            SyncAwareRefreshableContent(
                sessionKey = "favorite_solvers_sync",
                viewModel = viewModel,
                syncPhase = initialSyncPhase,
                data = favoriteItems,
                onRetry = { viewModel.retryInitialSync() },
                modifier = Modifier.fillMaxSize(),
                isManualRefreshing = isRefreshing,
                skeleton = {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(4) { FavoriteSolverCardSkeleton() }
                    }
                }
            ) { favs ->
            if (favs.isEmpty() && !viewModel.favoriteSolversLoadingMore) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.testTag("favorite_solvers_empty_state")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF1F2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "কোনো পছন্দের সমাধানকারী নেই",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ভার্চুয়াল বা অনলাইন কাজের সমাধান সম্পন্ন হলে আপনি দক্ষ সমাধানকারীকে পছন্দের তালিকায় যোগ করতে পারবেন এবং সরাসরি কাজ অফার করতে পারবেন।",
                            fontSize = 13.sp,
                            color = SomadhanTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("favorite_solvers_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "মোট সংরক্ষিত: ${DistanceUtil.toBengaliDigits(favs.size.toString())} জন",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = "সরাসরি কাজ অফার করুন",
                                fontSize = 12.sp,
                                color = Color(0xFFE11D48),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    items(favs, key = { it.solver.id }) { item ->
                        FavoriteSolverCard(
                            item = item,
                            onViewProfile = { onNavigateToProfile(item.solver.id) },
                            onToggleFavorite = {
                                viewModel.toggleFavoriteSolver(item.solver.id)
                            },
                            onDirectAssign = {
                                directContractSolver = item.solver
                            }
                        )
                    }

                    // Infinite Scroll Footer
                    if (viewModel.favoriteSolversHasMore) {
                        item {
                            LaunchedEffect(Unit) {
                                viewModel.loadNextFavoriteSolversPage()
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (viewModel.favoriteSolversLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFFE11D48)
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

    // Direct Contract Assignment Dialog
    directContractSolver?.let { solver ->
        val virtualCategories = remember(activeCategories) {
            activeCategories.filter { !it.isPhysical }
        }
        DirectContractDialog(
            solver = solver,
            categories = virtualCategories,
            onDismiss = { directContractSolver = null; directContractError = null },
            onSubmit = { title, description, category, budget, durationDays ->
                isSubmittingDirectContract = true
                directContractError = null
                viewModel.createDirectContractProject(
                    solver = solver,
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
                        directContractSolver = null
                        Toast.makeText(context, "সরাসরি প্রজেক্ট অফার সফলভাবে পাঠানো হয়েছে!", Toast.LENGTH_LONG).show()
                        if (onOpenChat != null) {
                            onOpenChat(problemId)
                        }
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
}

/**
 * ধাপ ২.৮ (ব্যাচ ৩১): [FavoriteSolverCard]-এর real layout-এর সাথে মেলানো skeleton (ধাপ ০.১৫ —
 * shimmer shape পেজ-নির্দিষ্ট হতে হবে, জেনেরিক ডিফল্ট [com.example.ui.components.ListScreenSkeleton]
 * copy-paste করা যাবে না)। কাঠামো: avatar circle + name/badge/rating লাইন + heart-আইকন স্পট,
 * তারপর "সম্পন্ন কাজ" ব্যাজ-রো, তারপর দুই বাটনের রো — হুবহু আসল কার্ডের চারটা সেকশনের মতোই।
 */
@Composable
private fun FavoriteSolverCardSkeleton(tint: Color = Color(0xFFE11D48)) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        border = BorderStroke(1.dp, SomadhanDivider),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                ShimmerBlock(
                    modifier = Modifier.size(52.dp),
                    cornerRadius = 26.dp,
                    tint = tint
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBlock(modifier = Modifier.width(120.dp).height(15.dp), tint = tint)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShimmerBlock(modifier = Modifier.width(60.dp).height(14.dp), tint = tint)
                        ShimmerBlock(modifier = Modifier.width(50.dp).height(14.dp), tint = tint)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                ShimmerBlock(
                    modifier = Modifier.size(22.dp),
                    cornerRadius = 11.dp,
                    tint = tint
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerBlock(
                modifier = Modifier.fillMaxWidth().height(28.dp),
                cornerRadius = 8.dp,
                tint = tint
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBlock(
                    modifier = Modifier.weight(1f).height(34.dp),
                    cornerRadius = 8.dp,
                    tint = tint
                )
                ShimmerBlock(
                    modifier = Modifier.weight(1f).height(34.dp),
                    cornerRadius = 8.dp,
                    tint = tint
                )
            }
        }
    }
}

@Composable
private fun FavoriteSolverCard(
    item: FavoriteSolverItem,
    onViewProfile: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDirectAssign: () -> Unit
) {
    val solver = item.solver

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("favorite_solver_card_${solver.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        border = BorderStroke(1.dp, SomadhanDivider),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Avatar, Name, Reputation, Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF1F2))
                        .border(1.5.dp, Color(0xFFE11D48).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val solverPhoto = solver.profileImageUri
                    val isPhotoValid = ImageStorageUtil.isValidDisplayUri(solverPhoto)
                    if (isPhotoValid) {
                        var loadFailed by remember(solverPhoto) { mutableStateOf(false) }
                        if (!loadFailed) {
                            AsyncImage(
                                model = solverPhoto,
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
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = solver.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (solver.verifiedBadgeSolver || solver.isKycVerified) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "ভেরিফায়েড",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReputationBadge(score = solver.reputationScore)

                        // Rating info
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = SomadhanYellowVerified,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", item.averageRating),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanTextPrimary
                            )
                            if (item.reviewsCount > 0) {
                                Text(
                                    text = " (${DistanceUtil.toBengaliDigits(item.reviewsCount.toString())})",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    }
                }

                // Heart Icon (Remove/Toggle)
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("remove_favorite_btn_${solver.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "পছন্দের তালিকা থেকে সরান",
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Completed Together badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SomadhanSuccessLight)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TaskAlt,
                        contentDescription = null,
                        tint = SomadhanSuccess,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "আপনার সাথে সম্পন্ন ভার্চুয়াল কাজ:",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary
                    )
                }
                Text(
                    text = "${DistanceUtil.toBengaliDigits(item.completedJobsTogether.toString())} টি",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanSuccess
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Profile & Direct Contract
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewProfile,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("view_profile_btn_${solver.id}"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE11D48)),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text(
                        text = "প্রোফাইল দেখুন",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onDirectAssign,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("direct_assign_btn_${solver.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "সরাসরি কাজ দিন",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

