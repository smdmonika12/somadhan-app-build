package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ListScreenSkeleton
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.SyncAwareRefreshableContent
import com.example.ui.components.worstSyncPhase
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.theme.SomadhanYellowVerified
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.Formatters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverReviewsScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val reviews by viewModel.solverReceivedReviews.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()
    val ratingsSyncPhase by viewModel.ratingsSyncPhase.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser?.id) {
        viewModel.resetSolverReviewsPagination(currentUser?.id)
    }

    val solverReviewsPaged = viewModel.solverReviewsPaged
    val totalReviews = if (reviews.isNotEmpty()) reviews.size else solverReviewsPaged.size
    val averageRating = if (reviews.isNotEmpty()) {
        reviews.map { it.stars }.average()
    } else if (solverReviewsPaged.isNotEmpty()) {
        solverReviewsPaged.map { it.stars }.average()
    } else {
        5.0
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "প্রাপ্ত রিভিউ ও রেটিং (${DistanceUtil.toBengaliDigits(totalReviews.toString())})",
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
                .background(SomadhanBg)
        ) {
            // Loading/Sync Fix Roadmap v2, dhap 7 (batch 3, pagination-screen migration) --
            // এতদিন data-বিহীন SyncAwareContent ছিল (rule ১ কভার করতো, rule ২/৪ ডেটা-ডিফ শিমার
            // ছিল না)। এই স্ক্রিনে pagination আসল/live (`solverReviewsPaged`, একটা
            // mutableStateListOf<RatingEntity>()) -- AllOpenProblemsScreen-এর প্যাটার্ন অনুযায়ী
            // `.toList()` দিয়ে immutable snapshot বানিয়ে `data`-তে পাঠানো হলো, যাতে প্রতিটা
            // মিউটেশনে নতুন List instance তৈরি হয় আর SyncAwareRefreshableContent-এর `!=` diff
            // সঠিকভাবে ধরতে পারে (একই mutable reference বারবার পাঠালে diff কাজ করতো না)।
            val reviewsSnapshot = solverReviewsPaged.toList()

            SyncAwareRefreshableContent(
                sessionKey = "solver_reviews_sync",
                viewModel = viewModel,
                syncPhase = worstSyncPhase(initialSyncPhase, ratingsSyncPhase),
                data = reviewsSnapshot,
                onRetry = { viewModel.retryInitialSync() },
                isManualRefreshing = isRefreshing,
                skeleton = { ListScreenSkeleton(tint = SomadhanOrange) }
            ) { reviewsList ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
            item {
                // Average Rating Overview Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanOrange.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "গড় রেটিং স্কোর",
                                fontSize = 13.sp,
                                color = SomadhanTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = DistanceUtil.toBengaliDigits(String.format(Locale.US, "%.1f", averageRating)),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = SomadhanYellowVerified,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "মোট ক্লায়েন্ট রিভিউ",
                                fontSize = 12.sp,
                                color = SomadhanTextHint
                            )
                            Text(
                                text = "${DistanceUtil.toBengaliDigits(totalReviews.toString())} টি",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ক্লায়েন্টদের মতামত",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (reviewsList.isEmpty() && !viewModel.solverReviewsLoadingMore) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "এখনও কোনো ক্লায়েন্ট রিভিউ পাওয়া যায়নি। কাজ সম্পন্ন করলে এখানে রিভিউ যুক্ত হবে।",
                                fontSize = 13.sp,
                                color = SomadhanTextHint,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                items(reviewsList, key = { it.id }) { review ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = review.userName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                                Text(
                                    text = Formatters.formatDateBengali(review.createdAt),
                                    fontSize = 10.sp,
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
                                    text = "সমস্যা: ${review.problemTitle}",
                                    fontSize = 12.sp,
                                    color = SomadhanTextSecondary
                                )

                                if (review.raterRole == "USER") {
                                    Text(
                                        text = "ক্লায়েন্টের থেকে পাওয়া",
                                        fontSize = 10.sp,
                                        color = SomadhanInfo,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "আপনি দিয়েছেন",
                                        fontSize = 10.sp,
                                        color = SomadhanTextHint
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                (1..5).forEach { star ->
                                    Icon(
                                        imageVector = if (star <= review.stars) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = null,
                                        tint = if (star <= review.stars) SomadhanYellowVerified else SomadhanTextHint,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(review.stars.toString())} / ৫",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanYellowVerified
                                )
                            }

                            if (review.comment.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "\"${review.comment}\"",
                                    fontSize = 13.sp,
                                    color = SomadhanTextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // Infinite Scroll Footer
            if (viewModel.solverReviewsHasMore) {
                item {
                    LaunchedEffect(Unit) {
                        viewModel.loadNextSolverReviewsPage()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.solverReviewsLoadingMore) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = SomadhanOrange
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
