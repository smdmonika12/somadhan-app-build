package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CategoryEntity
import com.example.ui.components.AccountStatusIndicator
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.PulsingValue
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.SomadhanPullToRefresh
import com.example.ui.components.SyncAwareContent
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.components.worstSyncPhase
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
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.AiMatcherUtil
import com.example.util.LocationFetchResult
import com.example.util.LocationHelper
import com.example.util.LocationResult
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.ui.components.BottomSlideAlertDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostProblemScreen(
    viewModel: SomadhanViewModel,
    isInstantMode: Boolean = false,
    onNavigateBack: () -> Unit,
    onProblemCreated: (String?) -> Unit = {},
    onHistoryClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val platformUrgencyLevels by viewModel.platformUrgencyLevels.collectAsStateWithLifecycle()
    val isInstantJobFeatureEnabled by viewModel.isInstantJobFeatureEnabled.collectAsStateWithLifecycle()
    val effectiveInstantMode = isInstantMode && isInstantJobFeatureEnabled
    // Loading/Sync Fix Roadmap v2, ধাপ ৪ (Group B) — currentUser (initialSyncPhase) আর
    // activeCategories (categoriesSyncPhase) দুটোর উপরই নির্ভরশীল।
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()
    val categoriesSyncPhase by viewModel.categoriesSyncPhase.collectAsStateWithLifecycle()

    val displayedCategories = remember(activeCategories, effectiveInstantMode) {
        if (effectiveInstantMode) {
            val instantCats = activeCategories.filter { it.instantJobEnabled && it.isPhysical }
            if (instantCats.isNotEmpty()) {
                instantCats
            } else {
                val physicalCats = activeCategories.filter { it.isPhysical }
                if (physicalCats.isNotEmpty()) physicalCats else activeCategories
            }
        } else {
            activeCategories
        }
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val titleWordCount = remember(title) {
        if (title.isBlank()) 0
        else title.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }
    val descWordCount = remember(description) {
        if (description.isBlank()) 0
        else description.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }

    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var isCategoryDropdownOpen by remember { mutableStateOf(false) }

    // Location is AUTO-DETECT ONLY (Rule 2.4 - locked from manual typing, no silent hardcoded fallback)
    var selectedLocation by remember {
        mutableStateOf<LocationResult?>(
            if (currentUser != null && !currentUser?.address.isNullOrBlank() && (currentUser?.latitude ?: 0.0) != 0.0) {
                LocationResult(
                    currentUser!!.latitude!!,
                    currentUser!!.longitude!!,
                    currentUser!!.address
                )
            } else if (liveLocation.address.isNotBlank() && (liveLocation.latitude != 0.0 || liveLocation.longitude != 0.0)) {
                liveLocation
            } else null
        )
    }

    LaunchedEffect(liveLocation) {
        if (selectedLocation == null && liveLocation.address.isNotBlank() && (liveLocation.latitude != 0.0 || liveLocation.longitude != 0.0)) {
            selectedLocation = liveLocation
        }
    }
    var isLocating by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    var minBudgetStr by remember { mutableStateOf("") }
    var maxBudgetStr by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf(if (effectiveInstantMode) "খুব জরুরি" else "সাধারণ") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Live AI category detection (Rule 2.1)
    var aiSuggestedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showForceCategoryHint by remember { mutableStateOf(false) }

    LaunchedEffect(title, displayedCategories) {
        if (title.length >= 3) {
            val suggested = AiMatcherUtil.suggestCategory(title, description, displayedCategories)
            aiSuggestedCategory = suggested
            if (suggested != null) {
                selectedCategory = suggested
                showForceCategoryHint = false
                if (minBudgetStr.isBlank()) minBudgetStr = suggested.minBudget.toInt().toString()
                if (maxBudgetStr.isBlank()) maxBudgetStr = suggested.maxBudget.toInt().toString()
            } else {
                if (title.length >= 20 && selectedCategory == null) {
                    showForceCategoryHint = true
                } else {
                    showForceCategoryHint = false
                }
            }
        } else {
            aiSuggestedCategory = null
            showForceCategoryHint = false
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if ((hasFine || hasCoarse) && selectedLocation == null) {
            isLocating = true
            when (val result = LocationHelper.getCurrentLocation(context)) {
                is LocationFetchResult.Success -> {
                    selectedLocation = result.location
                }
                else -> {}
            }
            isLocating = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            isLocating = true
            scope.launch {
                when (val result = LocationHelper.getCurrentLocation(context)) {
                    is LocationFetchResult.Success -> {
                        selectedLocation = result.location
                        errorMessage = null
                    }
                    is LocationFetchResult.PermissionDenied -> {
                        showPermissionDeniedDialog = true
                        errorMessage = "লোকেশন পারমিশন ছাড়া আপনার বর্তমান অবস্থান শনাক্ত করা সম্ভব নয়। অনুগ্রহ করে অ্যাপ সেটিংস থেকে লোকেশন পারমিশন চালু করুন।"
                    }
                    is LocationFetchResult.Error -> {
                        errorMessage = result.message
                    }
                }
                isLocating = false
            }
        } else {
            showPermissionDeniedDialog = true
            errorMessage = "লোকেশন পারমিশন ছাড়া আপনার বর্তমান অবস্থান শনাক্ত করা সম্ভব নয়। অনুগ্রহ করে অ্যাপ সেটিংস থেকে লোকেশন পারমিশন চালু করুন।"
        }
    }

    if (showPermissionDeniedDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = SomadhanError,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "লোকেশন পারমিশন প্রয়োজন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Text(
                    text = "লোকেশন পারমিশন ছাড়া আপনার বর্তমান অবস্থান শনাক্ত করা সম্ভব নয়। অনুগ্রহ করে অ্যাপ সেটিংস থেকে লোকেশন পারমিশন চালু করুন।",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDeniedDialog = false
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange)
                ) {
                    Text("অ্যাপ সেটিংস খুলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (effectiveInstantMode) "জরুরি সমস্যা পোস্ট করুন ⚡" else "নতুন সমস্যা পোস্ট করুন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AccountStatusIndicator(
                                isBanned = currentUser?.isBannedUser == true,
                                isRestricted = currentUser?.isRestrictedUser == true
                            )
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
                        if (effectiveInstantMode && onHistoryClick != null) {
                            IconButton(
                                onClick = onHistoryClick,
                                modifier = Modifier.testTag("instant_job_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "জরুরি হিস্ট্রি",
                                    tint = SomadhanTextPrimary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SomadhanBg),
                    modifier = Modifier.border(1.dp, SomadhanDivider)
                )
                RealtimeLocationBar(
                    locationAddress = liveLocation.address,
                    onRefresh = { viewModel.refreshLiveLocation() },
                    isSolver = false
                )
            }
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        // সেশন (batch31, PostProblemScreen fix) — শুধু location card-এর জন্য pull-to-refresh।
        // cold-load (প্রথম visit) পুরো-পেজ skeleton নিচের SyncAwareContent-ই হ্যান্ডল করে
        // (অপরিবর্তিত)। re-entry-তে flashOnReentry (ডিফল্ট true) দিয়ে আর pull-to-refresh
        // সম্পন্ন হলে isManualRefreshing দিয়ে — দুই ক্ষেত্রেই শুধু locationPulse true হয়ে
        // নিচের PulsingValue-এ মোড়ানো লোকেশন কার্ডটাই সংক্ষিপ্ত pulse করে, বাকি ফর্ম কাঠামো
        // (টাইটেল, ক্যাটাগরি, বাজেট, ইত্যাদি) কখনো re-animate হয় না।
        // Bug fix: আগে এখানে সরাসরি viewModel.isLocationUpdating (app-wide ব্যাকগ্রাউন্ড ১০-সেকেন্ড
        // GPS টিকারের সাথে শেয়ার্ড ফ্ল্যাগ) ব্যবহার হতো pull-to-refresh স্পিনার দেখানোর জন্য — যার
        // ফলে ব্যবহারকারী কিছু না টানলেও প্রতি ~১০ সেকেন্ডে (ব্যাকগ্রাউন্ড লোকেশন আপডেটের সাথে সাথে)
        // স্পিনারটা নিজে থেকেই দেখা যেত। এখন একটা আলাদা, স্ক্রিন-লোকাল ম্যানুয়াল-রিফ্রেশ state
        // ব্যবহার করা হচ্ছে যেটা শুধু ব্যবহারকারীর নিজের pull gesture-এই true হয়।
        var isManualLocationRefreshing by remember { mutableStateOf(false) }
        val locationPulse = rememberFieldChangePulse(
            value = selectedLocation,
            isManualRefreshing = isManualLocationRefreshing,
            sessionKey = "post_problem_sync",
            viewModel = viewModel
        )
        SomadhanPullToRefresh(
            isRefreshing = isManualLocationRefreshing,
            onRefresh = {
                scope.launch {
                    isManualLocationRefreshing = true
                    try {
                        viewModel.refreshLiveLocationAwait()
                    } finally {
                        isManualLocationRefreshing = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        SyncAwareContent(
            sessionKey = "post_problem_sync",
            viewModel = viewModel,
            syncPhase = worstSyncPhase(initialSyncPhase, categoriesSyncPhase),
            onRetry = {
                viewModel.retryInitialSync()
                viewModel.retryCategoriesSync()
            }
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Instant Mode Announcement Banner
            if (effectiveInstantMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "⚡ জরুরি ব্রডকাস্ট সিস্টেম: সমস্যাটি পোস্ট করার সাথে সাথে আশেপাশের টেকনিশিয়ানদের রাডারে লাইভ নোটিফিকেশন যাবে এবং তারা দ্রুত বিড (অফার) পাঠাবে।",
                            fontSize = 12.sp,
                            color = SomadhanTextPrimary,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Title (Headline)
            Text(
                text = if (effectiveInstantMode) "জরুরি শিরোনাম (Headline) *" else "সমস্যার শিরোনাম *",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SomadhanTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                placeholder = {
                    Text(
                        text = if (effectiveInstantMode) "যেমন: বাথরুমের পানির পাইপ ফেটে গেছে, জরুরি প্লাম্বার দরকার" else "যেমন: সিলিং ফ্যানের রেগুলেটর কাজ করছে না",
                        color = SomadhanTextHint,
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                supportingText = {
                    val minWords = if (effectiveInstantMode) 2 else 5
                    val maxWords = 20
                    val isValid = titleWordCount in minWords..maxWords
                    Text(
                        text = "$titleWordCount / সর্বনিম্ন $minWords, সর্বোচ্চ $maxWords শব্দ",
                        color = if (isValid) SomadhanTextSecondary else SomadhanError,
                        fontSize = 11.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8),
                    unfocusedBorderColor = SomadhanBorder,
                    focusedContainerColor = SomadhanBg,
                    unfocusedContainerColor = SomadhanCardBg
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("post_title_input")
            )

            // AI Suggestion Banner
            if (aiSuggestedCategory != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (effectiveInstantMode) SomadhanOrangeLight.copy(alpha = 0.3f) else Color(0xFFEFF6FF))
                        .border(1.dp, if (effectiveInstantMode) SomadhanOrange.copy(alpha = 0.4f) else Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI শনাক্তকৃত ক্যাটাগরি: ${aiSuggestedCategory?.nameBangla}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Description
            Text(
                text = if (effectiveInstantMode) "সংক্ষিপ্ত বিবরণ (Short Description) *" else "বিস্তারিত বিবরণ *",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SomadhanTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it; errorMessage = null },
                placeholder = {
                    Text(
                        text = if (effectiveInstantMode) "জরুরি সমস্যার বিস্তারিত বা কারণ সংক্ষেপে লিখুন..." else "সমস্যাটি বিস্তারিত ব্যাখ্যা করুন যাতে সমাধানকারী বুঝতে পারে...",
                        color = SomadhanTextHint,
                        fontSize = 13.sp
                    )
                },
                minLines = 3,
                maxLines = 5,
                supportingText = {
                    val minWords = if (effectiveInstantMode) 5 else 50
                    val maxWords = if (effectiveInstantMode) 300 else 500
                    val isValid = descWordCount in minWords..maxWords
                    Text(
                        text = "$descWordCount / সর্বনিম্ন $minWords, সর্বোচ্চ $maxWords শব্দ",
                        color = if (isValid) SomadhanTextSecondary else SomadhanError,
                        fontSize = 11.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8),
                    unfocusedBorderColor = SomadhanBorder,
                    focusedContainerColor = SomadhanBg,
                    unfocusedContainerColor = SomadhanCardBg
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("post_desc_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selection (Rule 2.3: Clean Dropdown/Selector + Quick Selection Chips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("সার্ভিস ক্যাটাগরি *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                if (effectiveInstantMode) {
                    Text(
                        text = "⚡ জরুরি ফিজিক্যাল সার্ভিস",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanOrange
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Quick Category Chips
            if (displayedCategories.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    displayedCategories.forEach { cat ->
                        val isSelected = selectedCategory?.id == cat.id
                        val chipBg = if (isSelected) {
                            if (effectiveInstantMode) SomadhanOrangeLight else Color(0xFFEFF6FF)
                        } else SomadhanCardBg
                        val chipBorder = if (isSelected) {
                            if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8)
                        } else SomadhanBorder
                        val chipText = if (isSelected) {
                            if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8)
                        } else SomadhanTextPrimary

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(chipBg)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = chipBorder,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    selectedCategory = cat
                                    showForceCategoryHint = false
                                    if (minBudgetStr.isBlank()) minBudgetStr = cat.minBudget.toInt().toString()
                                    if (maxBudgetStr.isBlank()) maxBudgetStr = cat.maxBudget.toInt().toString()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("cat_chip_${cat.id}")
                        ) {
                            Icon(
                                imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                contentDescription = null,
                                tint = chipText,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = cat.nameBangla,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = chipText
                            )
                        }
                    }
                }
            }

            // Dropdown Selector Button
            val isForceCategoryWarning = showForceCategoryHint && selectedCategory == null
            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCategoryDropdownOpen = true }
                        .testTag("category_dropdown_card"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isForceCategoryWarning) SomadhanError else if (selectedCategory != null) (if (effectiveInstantMode) SomadhanOrange.copy(alpha = 0.5f) else Color(0xFFBFDBFE)) else SomadhanBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedCategory != null) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(selectedCategory!!.iconName),
                                    contentDescription = null,
                                    tint = if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedCategory!!.nameBangla,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanTextPrimary
                                )
                            } else {
                                Text("ক্যাটাগরি ড্রপডাউন থেকে বাছাই করুন", fontSize = 13.sp, color = SomadhanTextHint)
                            }
                        }
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = SomadhanTextHint)
                    }
                }

                DropdownMenu(
                    expanded = isCategoryDropdownOpen,
                    onDismissRequest = { isCategoryDropdownOpen = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(SomadhanCardBg)
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                ) {
                    if (displayedCategories.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (effectiveInstantMode) "কোনো সক্রিয় জরুরি ক্যাটাগরি পাওয়া যায়নি" else "কোনো সক্রিয় ক্যাটাগরি পাওয়া যায়নি",
                                    color = SomadhanTextHint,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = { isCategoryDropdownOpen = false }
                        )
                    } else {
                        displayedCategories.forEach { cat ->
                            val isCurrentSelected = selectedCategory?.id == cat.id
                            val activeTint = if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                            contentDescription = null,
                                            tint = if (isCurrentSelected) activeTint else SomadhanTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = cat.nameBangla,
                                            color = if (isCurrentSelected) activeTint else SomadhanTextPrimary,
                                            fontWeight = if (isCurrentSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCategory = cat
                                    showForceCategoryHint = false
                                    if (minBudgetStr.isBlank()) minBudgetStr = cat.minBudget.toInt().toString()
                                    if (maxBudgetStr.isBlank()) maxBudgetStr = cat.maxBudget.toInt().toString()
                                    isCategoryDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            if (displayedCategories.isEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (effectiveInstantMode) "⚠️ বর্তমানে কোনো জরুরি সার্ভিস ক্যাটাগরি চালু নেই।" else "⚠️ প্ল্যাটফর্মে বর্তমানে সব ক্যাটাগরি সাময়িকভাবে বন্ধ রয়েছে। অনুগ্রহ করে পরবর্তীতে আবার চেষ্টা করুন।",
                    color = SomadhanError,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            // Warning if AI could not match category
            if (isForceCategoryWarning && displayedCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚠️ আপনার লেখা থেকে কোনো ক্যাটাগরি স্বয়ংক্রিয়ভাবে সনাক্ত করা যায়নি। অনুগ্রহ করে নিচের তালিকা থেকে সবচেয়ে কাছের ক্যাটাগরিটি নিজে বেছে নিন।",
                    color = SomadhanError,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Location (Rule 2.4: Auto-detected GPS ONLY, User CANNOT manually type)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("লোকেশন (GPS অটো-ডিটেক্ট) *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Lock, contentDescription = "Locked to GPS", tint = SomadhanTextHint, modifier = Modifier.size(13.dp))
                }

                Button(
                    onClick = {
                        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) {
                            isLocating = true
                            scope.launch {
                                when (val result = LocationHelper.getCurrentLocation(context)) {
                                    is LocationFetchResult.Success -> {
                                        selectedLocation = result.location
                                    }
                                    is LocationFetchResult.PermissionDenied -> {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                    is LocationFetchResult.Error -> {
                                        errorMessage = result.message
                                    }
                                }
                                isLocating = false
                            }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (effectiveInstantMode) SomadhanOrangeLight.copy(alpha = 0.3f) else Color(0xFFEFF6FF)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (effectiveInstantMode) SomadhanOrange.copy(alpha = 0.4f) else Color(0xFFBFDBFE)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(color = if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8), modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8), modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("অটো ডিটেক্ট", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Read-Only Location Display (Enforcing Rule 2.4 - No hardcoded fallback)
            // batch31 PostProblemScreen fix — এই কার্ডটাই একমাত্র অংশ যা re-entry mount-এ
            // (flashOnReentry) এবং pull-to-refresh সম্পন্ন হলে (isManualRefreshing) সংক্ষিপ্ত
            // pulse দেখায় (locationPulse, উপরে declared); বাকি ফর্ম কখনো re-animate হয় না।
            PulsingValue(isUpdating = locationPulse) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = if (selectedLocation != null) SomadhanCardBg else SomadhanErrorLight),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (selectedLocation != null) (if (effectiveInstantMode) SomadhanOrange.copy(alpha = 0.4f) else Color(0xFFBFDBFE)) else SomadhanError.copy(alpha = 0.5f)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selectedLocation != null) Icons.Default.LocationOn else Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = if (selectedLocation != null) (if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8)) else SomadhanError,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        val currentLoc = selectedLocation
                        if (currentLoc != null) {
                            Text(
                                text = currentLoc.address,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = "GPS স্থানাঙ্ক: ${String.format("%.4f", currentLoc.latitude)}, ${String.format("%.4f", currentLoc.longitude)}",
                                fontSize = 11.sp,
                                color = SomadhanTextHint
                            )
                        } else {
                            Text(
                                text = "লোকেশন এখনো শনাক্ত করা হয়নি",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanError
                            )
                            Text(
                                text = "বর্তমান অবস্থান পেতে উপরের 'অটো ডিটেক্ট' বাটনে ট্যাপ করুন।",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Budget Section
            Text(
                text = if (effectiveInstantMode) "প্রস্তাবিত বাজেট রেঞ্জ (টাকা) *" else "বাজেট রেঞ্জ (টাকা) *",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SomadhanTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = minBudgetStr,
                    onValueChange = { minBudgetStr = it; errorMessage = null },
                    label = { Text("সর্বনিম্ন (Min ৳)") },
                    placeholder = { Text("যেমন: 300") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8),
                        unfocusedBorderColor = SomadhanBorder,
                        focusedContainerColor = SomadhanBg,
                        unfocusedContainerColor = SomadhanCardBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("post_min_budget_input")
                )

                OutlinedTextField(
                    value = maxBudgetStr,
                    onValueChange = { maxBudgetStr = it; errorMessage = null },
                    label = { Text("সর্বোচ্চ (Max ৳)") },
                    placeholder = { Text("যেমন: 800") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8),
                        unfocusedBorderColor = SomadhanBorder,
                        focusedContainerColor = SomadhanBg,
                        unfocusedContainerColor = SomadhanCardBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("post_max_budget_input")
                )
            }

            if (effectiveInstantMode) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "💡 এটি আপনার আনুমানিক বাজেট ধারণা। সমাধানকারীরা এর চেয়ে কম বা বেশি মূল্যেও কাজের অফার (বিড) দিতে পারবেন।",
                    fontSize = 11.5.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 16.sp
                )
            } else {
                Spacer(modifier = Modifier.height(18.dp))

                // Urgency selection
                Text("জরুরি অবস্থা (Urgency) *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))

                val urgencyOptions = if (platformUrgencyLevels.isNotEmpty()) platformUrgencyLevels else listOf("সাধারণ", "জরুরি", "খুব জরুরি")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    urgencyOptions.forEach { level ->
                        val isSelected = urgency == level
                        val selectedBg = if (level.contains("খুব") || level.contains("জরুরি")) {
                            if (level.contains("খুব")) SomadhanError else Color(0xFFD97706)
                        } else {
                            Color(0xFF1D4ED8)
                        }
                        val selectedBorder = selectedBg
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) selectedBg else SomadhanCardBg)
                                .border(1.dp, if (isSelected) selectedBorder else SomadhanDivider, RoundedCornerShape(8.dp))
                                .clickable { urgency = level }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = level,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else SomadhanTextPrimary
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = errorMessage ?: "",
                    fontSize = 12.sp,
                    color = SomadhanError,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    if (currentUser?.isRestrictedUser == true) {
                        errorMessage = "আপনার অ্যাকাউন্টটি সাময়িকভাবে রেস্ট্রিক্ট করা হয়েছে। নতুন কোনো সমস্যা পোস্ট করতে পারবেন না।"
                        return@Button
                    }
                    val titleWords = title.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                    val descWords = description.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size

                    val minTitleW = if (effectiveInstantMode) 2 else 5
                    val maxTitleW = 20
                    if (titleWords < minTitleW || titleWords > maxTitleW) {
                        errorMessage = "শিরোনাম কমপক্ষে $minTitleW শব্দ এবং সর্বোচ্চ $maxTitleW শব্দের মধ্যে হতে হবে (বর্তমানে: $titleWords শব্দ)"
                        return@Button
                    }

                    val minDescW = if (effectiveInstantMode) 5 else 50
                    val maxDescW = if (effectiveInstantMode) 300 else 500
                    if (descWords < minDescW || descWords > maxDescW) {
                        val labelDesc = if (effectiveInstantMode) "সংক্ষিপ্ত বিবরণ" else "বিস্তারিত বিবরণ"
                        errorMessage = "$labelDesc কমপক্ষে $minDescW শব্দ এবং সর্বোচ্চ $maxDescW শব্দের মধ্যে হতে হবে (বর্তমানে: $descWords শব্দ)"
                        return@Button
                    }

                    if (title.isBlank()) {
                        errorMessage = "সমস্যার শিরোনাম প্রদান করা আবশ্যক।"
                        return@Button
                    }
                    if (description.isBlank()) {
                        errorMessage = "বিবরণ প্রদান করুন।"
                        return@Button
                    }

                    if (selectedCategory == null) {
                        errorMessage = "অনুগ্রহ করে একটি ক্যাটাগরি বাছাই করুন।"
                        return@Button
                    }

                    val loc = selectedLocation
                    if (loc == null || loc.address.isBlank()) {
                        errorMessage = "অনুগ্রহ করে 'অটো ডিটেক্ট' বাটনে ক্লিক করে বর্তমান লোকেশন নির্বাচন করুন।"
                        return@Button
                    }

                    val minB = minBudgetStr.toDoubleOrNull() ?: 0.0
                    val maxB = maxBudgetStr.toDoubleOrNull() ?: 0.0
                    if (minB <= 0 || maxB < minB) {
                        errorMessage = "সঠিক বাজেট রেঞ্জ প্রদান করুন।"
                        return@Button
                    }

                    if (effectiveInstantMode) {
                        isSubmitting = true
                        viewModel.createInstantJob(
                            title = title,
                            description = description,
                            category = selectedCategory!!,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            address = loc.address,
                            minBudget = minB,
                            maxBudget = maxB,
                            onSuccess = { created ->
                                isSubmitting = false
                                onProblemCreated(created.id)
                            },
                            onError = {
                                isSubmitting = false
                                errorMessage = it
                            }
                        )
                    } else {
                        isSubmitting = true
                        viewModel.createProblem(
                            title = title,
                            description = description,
                            category = selectedCategory!!,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            address = loc.address,
                            minBudget = minB,
                            maxBudget = maxB,
                            urgency = urgency,
                            onSuccess = {
                                isSubmitting = false
                                onProblemCreated(null)
                            },
                            onError = {
                                isSubmitting = false
                                errorMessage = it
                            }
                        )
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = if (effectiveInstantMode) SomadhanOrange else Color(0xFF1D4ED8)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("post_submit_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "পোস্ট হচ্ছে...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = if (effectiveInstantMode) "জরুরি ব্রডকাস্টিং শুরু করুন ⚡" else "সমস্যা পোস্ট নিশ্চিত করুন",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
        }
        }
    }
}
