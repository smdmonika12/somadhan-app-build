@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.entity.RatingEntity
import com.example.ui.components.PulsingValue
import com.example.ui.components.RoleBadge
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
import com.example.ui.components.BottomSlideAlertDialog

@Composable
fun AdminRatingsView(
    ratings: List<RatingEntity>,
    onDelete: (String) -> Unit,
    viewModel: SomadhanViewModel? = null,
    isManualRefreshing: Boolean = false
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedRatingFilter by rememberSaveable { mutableStateOf("ALL") }
    var selectedRoleFilter by rememberSaveable { mutableStateOf("ALL") } // "ALL", "USER_TO_SOLVER", "SOLVER_TO_USER", "WITH_COMMENT"
    var sortBy by rememberSaveable { mutableStateOf("NEWEST") } // "NEWEST", "OLDEST", "LOWEST_RATING", "HIGHEST_RATING"

    var currentPage by rememberSaveable { mutableIntStateOf(1) }
    val itemsPerPage = 5

    var ratingToDelete by remember { mutableStateOf<RatingEntity?>(null) }
    var selectedRatingForDetail by remember { mutableStateOf<RatingEntity?>(null) }

    // Delete Confirmation Dialog
    if (ratingToDelete != null) {
        val target = ratingToDelete!!
        BottomSlideAlertDialog(
            onDismissRequest = { ratingToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = SomadhanError,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "রিভিউ মুছে ফেলা নিশ্চিতকরণ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "আপনি কি নিশ্চিতভাবে এই রিভিউটি প্ল্যাটফর্ম থেকে স্থায়ীভাবে মুছে ফেলতে চান?",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${target.userName} ➔ ${target.solverName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SomadhanTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${DistanceUtil.toBengaliDigits(target.stars.toString())}★)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFB800)
                                )
                            }
                            if (target.comment.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${target.comment}\"",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(target.id)
                        ratingToDelete = null
                        if (selectedRatingForDetail?.id == target.id) {
                            selectedRatingForDetail = null
                        }
                        Toast.makeText(context, "রিভিউ সফলভাবে মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("হ্যাঁ, মুছে ফেলুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { ratingToDelete = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // Detail Dialog (Admin Control)
    if (selectedRatingForDetail != null) {
        val detailRating = selectedRatingForDetail!!
        val isLow = detailRating.stars <= 2

        BottomSlideAlertDialog(
            onDismissRequest = { selectedRatingForDetail = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.RateReview,
                    contentDescription = null,
                    tint = SomadhanOrange,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "রিভিউ বিস্তারিত ও অ্যাডমিন কন্ট্রোল",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Rating Stars Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLow) SomadhanErrorLight else SomadhanOrangeLight.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(5) { idx ->
                                    val filled = idx < detailRating.stars
                                    Icon(
                                        imageVector = if (filled) Icons.Default.Star else Icons.Outlined.Star,
                                        contentDescription = null,
                                        tint = if (filled) Color(0xFFFFB800) else SomadhanTextHint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(detailRating.stars.toString())}/৫",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isLow) SomadhanError else Color(0xFFFFB800)
                                )
                            }

                            if (isLow) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SomadhanError)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("⚠️ কম রেটিং", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Review Participants
                    Text("রেটিং বিবরণ:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("রেটিং দিয়েছেন (Reviewer):", fontSize = 10.sp, color = SomadhanTextHint)
                                    Text(detailRating.userName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                                }
                                RoleBadge(role = detailRating.raterRole)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(color = SomadhanDivider, thickness = 0.6.dp)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("রেটিং পেয়েছেন (Reviewee):", fontSize = 10.sp, color = SomadhanTextHint)
                                    Text(detailRating.solverName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                                }
                                RoleBadge(role = if (detailRating.raterRole == "USER") "SOLVER" else "USER")
                            }

                            if (detailRating.problemTitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Divider(color = SomadhanDivider, thickness = 0.6.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("সম্পর্কিত কাজ / সমস্যা:", fontSize = 10.sp, color = SomadhanTextHint)
                                Text(detailRating.problemTitle, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SomadhanTextSecondary)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(color = SomadhanDivider, thickness = 0.6.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "প্রদানের তারিখ ও সময়: ${Formatters.formatDateTimeBengali(detailRating.createdAt)} (${Formatters.formatTimeAgo(detailRating.createdAt)})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SomadhanTextSecondary
                            )
                        }
                    }

                    // Comment section
                    if (detailRating.comment.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ব্যবহারকারীর মন্তব্য:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(detailRating.comment))
                                    Toast.makeText(context, "মন্তব্য কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "কপি", modifier = Modifier.size(12.dp), tint = SomadhanOrange)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("কপি", fontSize = 11.sp, color = SomadhanOrange)
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "\"${detailRating.comment}\"",
                                fontSize = 12.sp,
                                color = SomadhanTextPrimary,
                                modifier = Modifier.padding(10.dp),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ratingToDelete = detailRating
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("রিভিউ মুছুন", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRatingForDetail = null }) {
                    Text("বন্ধ করুন", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // Filter & Sort Logic
    val filteredRatings = remember(ratings, searchQuery, selectedRatingFilter, selectedRoleFilter, sortBy) {
        val query = searchQuery.trim().lowercase()
        val filtered = ratings.filter { rating ->
            val matchesStar = when (selectedRatingFilter) {
                "ALL" -> true
                "LOW" -> rating.stars <= 2
                "5" -> rating.stars == 5
                "4" -> rating.stars == 4
                "3" -> rating.stars == 3
                "2" -> rating.stars == 2
                "1" -> rating.stars == 1
                else -> true
            }

            val matchesRole = when (selectedRoleFilter) {
                "ALL" -> true
                "USER_TO_SOLVER" -> rating.raterRole == "USER"
                "SOLVER_TO_USER" -> rating.raterRole == "SOLVER"
                "WITH_COMMENT" -> rating.comment.isNotBlank()
                else -> true
            }

            val matchesQuery = if (query.isBlank()) true else {
                rating.userName.lowercase().contains(query) ||
                    rating.solverName.lowercase().contains(query) ||
                    rating.comment.lowercase().contains(query) ||
                    rating.problemTitle.lowercase().contains(query) ||
                    rating.id.lowercase().contains(query)
            }

            matchesStar && matchesRole && matchesQuery
        }

        when (sortBy) {
            "NEWEST" -> filtered.sortedByDescending { it.createdAt }
            "OLDEST" -> filtered.sortedBy { it.createdAt }
            "LOWEST_RATING" -> filtered.sortedWith(compareBy<RatingEntity> { it.stars }.thenByDescending { it.createdAt })
            "HIGHEST_RATING" -> filtered.sortedWith(compareByDescending<RatingEntity> { it.stars }.thenByDescending { it.createdAt })
            else -> filtered.sortedByDescending { it.createdAt }
        }
    }

    val totalPages = maxOf(1, (filteredRatings.size + itemsPerPage - 1) / itemsPerPage)
    val safePage = currentPage.coerceIn(1, totalPages)
    val paginatedRatings = remember(filteredRatings, safePage) {
        filteredRatings.drop((safePage - 1) * itemsPerPage).take(itemsPerPage)
    }

    val listState = rememberLazyListState()
    // Admin Panel Loading fix, সেশন ২.৩৪ — Users/Withdrawal/KYC/AdditionalCharges/CancelledBids-এর
    // মতোই scroll-jump ফিক্স: key `safePage`-এর বদলে `currentPage` — কোনো রেটিং ডিলিট হয়ে
    // totalPages কমে গিয়ে coerced `safePage` স্বয়ংক্রিয়ভাবে বদলালে জোর করে টপে scroll হওয়া বন্ধ
    // করা (ইউজার/অ্যাডমিন নিজে next/prev ক্লিক করলেই শুধু scroll হবে, passive coercion-এ না)।
    LaunchedEffect(currentPage) {
        listState.scrollToItem(0)
    }

    // Admin Panel Loading fix, সেশন ২.৭ — KYC (২.৩)/ক্যাটাগরি (২.৫)/অতিরিক্ত চার্জ (২.৬)-এর মতো,
    // শুধু রিভিউ-লিস্টের কার্ডগুলো pulse করবে (হেডার/সার্চ-ফিল্টার/সর্ট কন্ট্রোল, pagination bar এই
    // value-র অংশ না, তাই কখনো pulse করবে না)। value = paginatedRatings, তাই ফিল্টার/সর্ট/পেজ
    // পাল্টানো/আসল ডেটা বদলানো (delete-সহ) — সবই ট্রিগার করে। sessionKey
    // "admin_ratings_sync" — AdminPanelScreen.kt-এর ইনডেক্স ১০-এর SyncAwareContent cold-load
    // gate-এর সাথে একই key শেয়ার করে, আর pull-to-refresh সম্পন্ন হলেও (isManualRefreshing
    // সত্যি→মিথ্যা) আলাদাভাবে pulse হয়।
    val ratingsListPulse = rememberFieldChangePulse(
        value = paginatedRatings,
        isManualRefreshing = isManualRefreshing,
        sessionKey = "admin_ratings_sync",
        viewModel = viewModel
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        // 1. KPI Stats Summary Cards
        item {
            val totalCount = ratings.size
            val lowRatingCount = ratings.count { it.stars <= 2 }
            val fiveStarCount = ratings.count { it.stars == 5 }
            val avgRating = if (ratings.isNotEmpty()) {
                String.format(java.util.Locale.US, "%.1f", ratings.map { it.stars }.average())
            } else "0.0"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Total
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(DistanceUtil.toBengaliDigits(totalCount.toString()), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                        Text("মোট রিভিউ", fontSize = 10.sp, color = SomadhanTextHint)
                    }
                }

                // Card 2: Average
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SomadhanOrange.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(DistanceUtil.toBengaliDigits(avgRating), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SomadhanOrange)
                            Text("★", fontSize = 12.sp, color = Color(0xFFFFB800))
                        }
                        Text("গড় রেটিং", fontSize = 10.sp, color = SomadhanTextSecondary)
                    }
                }

                // Card 3: 5 Star
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanSuccessLight.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SomadhanSuccess.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(DistanceUtil.toBengaliDigits(fiveStarCount.toString()), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SomadhanSuccess)
                        Text("৫★ রেটিং", fontSize = 10.sp, color = SomadhanTextSecondary)
                    }
                }

                // Card 4: Low Rating Alert
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (lowRatingCount > 0) SomadhanErrorLight else SomadhanBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (lowRatingCount > 0) SomadhanError.copy(alpha = 0.6f) else SomadhanDivider,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            DistanceUtil.toBengaliDigits(lowRatingCount.toString()),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lowRatingCount > 0) SomadhanError else SomadhanTextPrimary
                        )
                        Text("কম রেটিং (১-২★)", fontSize = 9.sp, color = if (lowRatingCount > 0) SomadhanError else SomadhanTextHint, maxLines = 1)
                    }
                }
            }
        }

        // 2. Search & Filter Bar Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "রিভিউ মডারেশন (${DistanceUtil.toBengaliDigits(filteredRatings.size.toString())} টি)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )

                        Text(
                            text = "পেজ ${DistanceUtil.toBengaliDigits(safePage.toString())}/${DistanceUtil.toBengaliDigits(totalPages.toString())}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SomadhanTextHint
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            currentPage = 1
                        },
                        placeholder = { Text("ব্যবহারকারী, সলভার, সমস্যা বা মন্তব্য খুঁজুন...", fontSize = 12.sp, color = SomadhanTextHint) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = SomadhanTextSecondary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    currentPage = 1
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "মুছুন", tint = SomadhanTextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanDivider,
                            focusedContainerColor = SomadhanCardBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Star Rating Filter Chips
                    Text("রেটিং ফিল্টার:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))

                    val starFilterOptions = listOf(
                        "ALL" to "সকল (${ratings.size})",
                        "LOW" to "⚠️ কম রেটিং (${ratings.count { it.stars <= 2 }})",
                        "5" to "৫★ (${ratings.count { it.stars == 5 }})",
                        "4" to "৪★ (${ratings.count { it.stars == 4 }})",
                        "3" to "৩★ (${ratings.count { it.stars == 3 }})",
                        "2" to "২★ (${ratings.count { it.stars == 2 }})",
                        "1" to "১★ (${ratings.count { it.stars == 1 }})"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        starFilterOptions.take(3).forEach { (filterKey, filterLabel) ->
                            val isSelected = selectedRatingFilter == filterKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) (if (filterKey == "LOW") SomadhanError else SomadhanOrange) else SomadhanCardBg)
                                    .border(1.dp, if (isSelected) (if (filterKey == "LOW") SomadhanError else SomadhanOrange) else SomadhanDivider, RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedRatingFilter = filterKey
                                        currentPage = 1
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = filterLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else SomadhanTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        starFilterOptions.drop(3).forEach { (filterKey, filterLabel) ->
                            val isSelected = selectedRatingFilter == filterKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) SomadhanOrange else SomadhanCardBg)
                                    .border(1.dp, if (isSelected) SomadhanOrange else SomadhanDivider, RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedRatingFilter = filterKey
                                        currentPage = 1
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = filterLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else SomadhanTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Filters & Sort
                    Text("অ্যাডমিন ধরণ ও সর্টিং:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))

                    val roleFilters = listOf(
                        "ALL" to "সব ধরণ",
                        "USER_TO_SOLVER" to "👤 গ্রাহক ➔ সলভার",
                        "SOLVER_TO_USER" to "🛠️ সলভার ➔ গ্রাহক",
                        "WITH_COMMENT" to "💬 মন্তব্য আছে"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        roleFilters.forEach { (roleKey, roleLabel) ->
                            val isSelected = selectedRoleFilter == roleKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) SomadhanOrange.copy(alpha = 0.15f) else SomadhanCardBg)
                                    .border(1.dp, if (isSelected) SomadhanOrange else SomadhanDivider, RoundedCornerShape(6.dp))
                                    .clickable {
                                        selectedRoleFilter = roleKey
                                        currentPage = 1
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = roleLabel,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SomadhanOrange else SomadhanTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Sort Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = null, tint = SomadhanTextHint, modifier = Modifier.size(14.dp))
                        listOf(
                            "NEWEST" to "সর্বশেষ আগে",
                            "OLDEST" to "পুরাতন আগে",
                            "LOWEST_RATING" to "কম রেটিং আগে",
                            "HIGHEST_RATING" to "উচ্চ রেটিং আগে"
                        ).forEach { (sortKey, sortLabel) ->
                            val isSelected = sortBy == sortKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) SomadhanTextPrimary else SomadhanCardBg)
                                    .clickable {
                                        sortBy = sortKey
                                        currentPage = 1
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = sortLabel,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SomadhanBg else SomadhanTextHint
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Review Items or Empty State
        if (filteredRatings.isEmpty()) {
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
                            .padding(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = SomadhanTextHint, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedRatingFilter != "ALL" || selectedRoleFilter != "ALL") {
                                    "ফিল্টারের সাথে কোনো রিভিউ পাওয়া যায়নি।"
                                } else {
                                    "প্ল্যাটফর্মে এখনো কোনো রিভিউ জমা পড়েনি।"
                                },
                                fontSize = 13.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }
                }
            }
        } else {
            items(paginatedRatings, key = { it.id }) { rating ->
                val isLowRating = rating.stars <= 2
                PulsingValue(isUpdating = ratingsListPulse) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLowRating) SomadhanErrorLight.copy(alpha = 0.2f) else SomadhanBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isLowRating) 1.5.dp else 1.dp,
                            color = if (isLowRating) SomadhanError.copy(alpha = 0.6f) else SomadhanDivider,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Top Row: Stars, Low rating badge, Actions (Info, Delete)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Stars Row
                                repeat(5) { index ->
                                    val isFilled = index < rating.stars
                                    Icon(
                                        imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.Star,
                                        contentDescription = null,
                                        tint = if (isFilled) Color(0xFFFFB800) else SomadhanTextHint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(rating.stars.toString())}/৫",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isLowRating) SomadhanError else Color(0xFFFFB800)
                                )

                                if (isLowRating) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SomadhanErrorLight)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("⚠️ কম রেটিং", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SomadhanError)
                                    }
                                }
                            }

                            // Action buttons
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { selectedRatingForDetail = rating },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "বিস্তারিত দেখুন",
                                        tint = SomadhanInfo,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { ratingToDelete = rating },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "রিভিউ মুছুন",
                                        tint = SomadhanError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // User to Solver Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rating.userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            RoleBadge(role = rating.raterRole)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("➔", fontSize = 12.sp, color = SomadhanOrange)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = rating.solverName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            RoleBadge(role = if (rating.raterRole == "USER") "SOLVER" else "USER")
                        }

                        if (rating.problemTitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "সমস্যা: ${rating.problemTitle}",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Review Comment
                        if (rating.comment.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "\"${rating.comment}\"",
                                        fontSize = 12.sp,
                                        color = SomadhanTextPrimary,
                                        modifier = Modifier.weight(1f),
                                        lineHeight = 16.sp
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(rating.comment))
                                            Toast.makeText(context, "মন্তব্য কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "কপি",
                                            tint = SomadhanTextHint,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bottom row: Date & Quick Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "তারিখ ও সময়: ${Formatters.formatDateTimeBengali(rating.createdAt)} (${Formatters.formatTimeAgo(rating.createdAt)})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SomadhanTextSecondary
                            )

                            TextButton(
                                onClick = { selectedRatingForDetail = rating },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("অ্যাডমিন কন্ট্রোল", fontSize = 11.sp, color = SomadhanOrange, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                }
            }
        }
    }

        // 4. Pagination Controls (Bottom, Fixed)
        if (filteredRatings.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (safePage > 1) currentPage = safePage - 1 },
                        enabled = safePage > 1,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পূর্ববর্তী", modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পূর্ববর্তী", fontSize = 11.sp)
                    }

                    Text(
                        text = "পেজ ${DistanceUtil.toBengaliDigits(safePage.toString())} / ${DistanceUtil.toBengaliDigits(totalPages.toString())}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )

                    OutlinedButton(
                        onClick = { if (safePage < totalPages) currentPage = safePage + 1 },
                        enabled = safePage < totalPages,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("পরবর্তী", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "পরবর্তী", modifier = Modifier.size(13.dp))
                    }
                }
            }
        }
    }
}
