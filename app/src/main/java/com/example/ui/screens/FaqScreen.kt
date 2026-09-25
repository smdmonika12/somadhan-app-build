package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ShimmerBlock
import com.example.ui.components.SyncAwareRefreshableContent
import com.example.data.entity.FaqEntity
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit,
    onSupportClick: (() -> Unit)? = null
) {
    val faqs by viewModel.activeFaqs.collectAsStateWithLifecycle()
    val faqsSyncPhase by viewModel.faqsSyncPhase.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userRole = currentUser?.role ?: "USER"
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = if (userRole == "SOLVER") 1 else 0,
        pageCount = { 2 }
    )
    var searchQuery by remember { mutableStateOf("") }
    val expandedFaqIds = remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "সচরাচর জিজ্ঞাসা (FAQ)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("faq_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = SomadhanTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SomadhanBg)
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("faq_screen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header Info Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE9D5FF), RoundedCornerShape(16.dp))
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
                                .background(Color(0xFF9333EA).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = Color(0xFF9333EA),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "কীভাবে সমাধান অ্যাপ কাজ করে?",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF581C87)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "আপনার প্রশ্নের দ্রুত সমাধান পেতে নিচে উল্লেখিত প্রশ্নোত্তরগুলো দেখুন।",
                                fontSize = 11.5.sp,
                                color = Color(0xFF7E22CE),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Role / Audience Tab Row
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.White,
                    contentColor = Color(0xFF9333EA),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = Color(0xFF9333EA)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, SomadhanBorder, RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (pagerState.currentPage == 0) Color(0xFF9333EA) else Color(0xFF6B7280)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "ইউজার FAQ",
                                    fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (pagerState.currentPage == 0) Color(0xFF9333EA) else Color(0xFF6B7280)
                                )
                            }
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Engineering,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (pagerState.currentPage == 1) Color(0xFF9333EA) else Color(0xFF6B7280)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "সলভার FAQ",
                                    fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (pagerState.currentPage == 1) Color(0xFF9333EA) else Color(0xFF6B7280)
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "প্রশ্ন খুঁজুন (যেমন: পোস্ট, পেমেন্ট, বিড...)",
                            fontSize = 13.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF9333EA),
                        unfocusedBorderColor = SomadhanBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("faq_search_input")
                )
            }

            // Loading/Sync Fix Roadmap v2, ধাপ ৩ — SyncAwareContent-এর পরীক্ষামূলক ওয়্যারিং।
            // faqsSyncPhase LOADING/LOADED/ERROR অনুযায়ী skeleton/আসল কন্টেন্ট/এরর-রিট্রাই UI
            // দেখায়।
            // Loading Pattern Master Prompt, ব্যাচ ৬ (B2 migration) — `data = faqs` পাস করা
            // হলো: `activeFaqs` একটা StateFlow<List<FaqEntity>>, প্রতিটা প্রকৃত DB/realtime
            // পরিবর্তনে নতুন List instance আসে (plain immutable, mutableStateListOf না), তাই
            // Ground Rule ১১ অনুযায়ী `.toList()` লাগছে না। lambda প্যারামিটারের নাম ইচ্ছাকৃতভাবে
            // বাইরের `faqs`-কেই shadow করছে যাতে ভেতরের tab/search filtering (`remember(faqs, ...)`)
            // অপরিবর্তিত থাকে — ট্যাব-সুইচ/সার্চে flash হবে না, শুধু আসল FAQ ডেটা বদলালে হবে।
            SyncAwareRefreshableContent(
                sessionKey = "faq_screen",
                viewModel = viewModel,
                syncPhase = faqsSyncPhase,
                data = faqs,
                onRetry = { viewModel.retryFaqsSync() },
                modifier = Modifier.fillMaxSize(),
                skeleton = {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            ShimmerBlock(modifier = Modifier.width(150.dp).height(16.dp))
                        }
                        items(5) { FaqCardSkeleton() }
                    }
                }
            ) { faqs ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val tabFaqs = remember(faqs, pageIndex) {
                    faqs.filter {
                        if (pageIndex == 0) {
                            it.targetAudience == "USER" || it.targetAudience == "BOTH"
                        } else {
                            it.targetAudience == "SOLVER" || it.targetAudience == "BOTH"
                        }
                    }
                }

                val filteredFaqs = remember(tabFaqs, searchQuery) {
                    if (searchQuery.isBlank()) {
                        tabFaqs
                    } else {
                        val query = searchQuery.trim().lowercase()
                        tabFaqs.filter {
                            it.question.lowercase().contains(query) || it.answer.lowercase().contains(query)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // FAQ Item Count Bar
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "প্রশ্নোত্তর তালিকা (${DistanceUtil.toBengaliDigits(filteredFaqs.size.toString())}টি)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }
                    }

                    // Empty state if no matches
                    if (filteredFaqs.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SomadhanBorder, RoundedCornerShape(14.dp))
                                    .padding(vertical = 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "কোনো প্রশ্নোত্তর খুঁজে পাওয়া যায়নি",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "অন্য কোনো কি-ওয়ার্ড দিয়ে আবার চেষ্টা করুন।",
                                        fontSize = 12.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        // Expandable Accordion List
                        itemsIndexed(filteredFaqs, key = { _, item -> item.id }) { index, faq ->
                            val isExpanded = expandedFaqIds.value.contains(faq.id)
                            val rotationState by animateFloatAsState(
                                targetValue = if (isExpanded) 180f else 0f,
                                label = "accordion_arrow_rotation"
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 2.dp else 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isExpanded) Color(0xFFC084FC) else SomadhanBorder,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clip(RoundedCornerShape(14.dp))
                                    .testTag("faq_card_${faq.id}")
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Header Row (Question + Number Badge + Arrow)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val current = expandedFaqIds.value.toMutableSet()
                                                if (isExpanded) {
                                                    current.remove(faq.id)
                                                } else {
                                                    current.add(faq.id)
                                                }
                                                expandedFaqIds.value = current
                                            }
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (isExpanded) Color(0xFF9333EA) else Color(0xFFF3E8FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = DistanceUtil.toBengaliDigits((index + 1).toString()),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isExpanded) Color.White else Color(0xFF7E22CE)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Text(
                                            text = faq.question,
                                            fontSize = 14.sp,
                                            fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isExpanded) Color(0xFF6B21A8) else SomadhanTextPrimary,
                                            lineHeight = 19.sp,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("faq_question_${faq.id}")
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = if (isExpanded) "সংকোচন করুন" else "প্রসারিত করুন",
                                            tint = if (isExpanded) Color(0xFF9333EA) else Color(0xFF9CA3AF),
                                            modifier = Modifier
                                                .size(22.dp)
                                                .rotate(rotationState)
                                        )
                                    }

                                    // Expanded Answer Section
                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFFAF5FF).copy(alpha = 0.6f))
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            HorizontalDivider(
                                                color = Color(0xFFE9D5FF).copy(alpha = 0.7f),
                                                thickness = 0.8.dp,
                                                modifier = Modifier.padding(bottom = 10.dp)
                                            )

                                            Text(
                                                text = faq.answer,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color(0xFF374151),
                                                lineHeight = 20.sp,
                                                modifier = Modifier.testTag("faq_answer_${faq.id}")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Need More Help Card
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1D4ED8).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HeadsetMic,
                                        contentDescription = null,
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "আপনার প্রশ্নের উত্তর পাননি?",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "আমাদের সাপোর্ট সেন্টারে সরাসরি যোগাযোগ করুন।",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            } // SyncAwareRefreshableContent (ধাপ ৩ + ব্যাচ ৬ B2 migration) বন্ধ
        }
    }
}

/**
 * ধাপ ২.৯ (ব্যাচ ৩১): accordion FAQ কার্ডের (collapsed state) real layout-এর সাথে মেলানো skeleton
 * (ধাপ ০.১৫) — নম্বর-ব্যাজ circle, প্রশ্ন-টেক্সট লাইন, expand-আইকন স্পট, আগের ডিফল্ট
 * [com.example.ui.components.ListScreenSkeleton]-এর (ProblemCard-শেপ) বদলে।
 */
@Composable
private fun FaqCardSkeleton() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SomadhanBorder, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBlock(
                modifier = Modifier.size(28.dp),
                cornerRadius = 14.dp,
                tint = Color(0xFF9333EA)
            )
            Spacer(modifier = Modifier.width(12.dp))
            ShimmerBlock(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp),
                tint = Color(0xFF9333EA)
            )
            Spacer(modifier = Modifier.width(8.dp))
            ShimmerBlock(
                modifier = Modifier.size(22.dp),
                cornerRadius = 11.dp,
                tint = Color(0xFF9333EA)
            )
        }
    }
}
