package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.util.DistanceUtil
import com.example.util.ImageStorageUtil
import com.example.data.entity.activeRoleBanned
import com.example.data.entity.activeRoleRestricted
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.AccountStatusIndicator
import com.example.ui.components.ListScreenSkeleton
import com.example.ui.components.LoadingAwareContent
import com.example.ui.components.SyncAwareRefreshableContent
import com.example.ui.components.NotificationBottomSheet
import com.example.ui.components.handleSomadhanNotification
import com.example.ui.components.ReputationBadge
import com.example.ui.components.RoleBadge
import com.example.ui.components.RoleSwitchCategoryDialog
import com.example.ui.components.SomadhanBottomNav
import com.example.ui.components.SomadhanTopBar
import com.example.ui.components.UserVerificationBadge
import com.example.ui.components.YellowVerifiedBadge
import com.example.ui.navigation.Screen
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight
import com.example.ui.theme.SomadhanInfoLight
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.ui.components.BottomSlideAlertDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: SomadhanViewModel,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onAdminClick: () -> Unit,
    onProblemClick: ((String) -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allProblems by viewModel.allProblems.collectAsStateWithLifecycle()
    val solverBids by viewModel.solverBids.collectAsStateWithLifecycle()
    val allBids by viewModel.allBids.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val isUploadingProfileImage by viewModel.isUploadingProfileImage.collectAsStateWithLifecycle()
    // [শিমার ফিক্স — MONEY_FLOW_AND_ADMIN_BUGS_MASTER_PROMPT.md ধাপ ৫, বাগ B] আগে এখানে
    // viewModel.isRefreshing (app-জোড়া শেয়ার্ড global StateFlow) পড়ে সরাসরি নিচে
    // isManualRefreshing-এ পাস করা হতো। এই পেজে pull-to-refresh নেই (ব্যাচ ৩১-এ সরানো হয়েছে,
    // নিচের comment দ্রষ্টব্য) — তবু global flag observe করার ফলে অন্য যেকোনো স্ক্রিনের
    // action (Force Sync, Wallet Refresh, ইত্যাদি) শেষ হলেই (true->false ট্রানজিশন)
    // ব্যবহারকারী Profile-এ থাকা অবস্থায় একটা অপ্রত্যাশিত ৩৫০ms flash দেখতেন, যদিও তিনি নিজে
    // কিছুই ট্রিগার করেননি। ফিক্স: এই পেজের নিজস্ব কোনো manual-refresh gesture নেই বলে এই
    // local সমতুল্য মান সবসময় false-ই।

    // Loading/Sync Fix Roadmap v2, dhap 4 -- initialSyncPhase (bulk-pull), separate sessionKey "profile_sync"
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    val currentUserId = currentUser?.id ?: ""
    val myProblems = remember(allProblems, currentUserId) {
        if (currentUserId.isBlank()) emptyList()
        else allProblems.filter { it.userId == currentUserId && !it.isUserDeleted }
    }
    // চলমান সমস্যা: এখনও বিড গ্রহণ করা হয়নি এমন পোস্ট (OPEN) + যেসব পোস্টের বিড ইতিমধ্যে গ্রহণ করা হয়েছে (IN_PROGRESS)
    val myActiveProblems = remember(myProblems) {
        myProblems.filter {
            (it.status == "OPEN" || it.status == "IN_PROGRESS") &&
            it.status != "COMPLETED" &&
            it.status != "CANCELLED"
        }
    }
    val myActiveCount = myActiveProblems.size
    val myCancelledProblems = remember(myProblems) {
        myProblems.filter { it.status == "CANCELLED" }
    }
    val myCancelledCount = myCancelledProblems.size
    val myCompletedProblems = remember(myProblems) {
        myProblems.filter { it.status == "COMPLETED" }
    }
    val myCompletedCount = myCompletedProblems.size
    val myFavoriteSolversCount = currentUser?.favoriteSolverIds?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.size ?: 0
    val myBidsCount = remember(currentUser, solverBids, allBids) {
        val uid = currentUser?.id ?: ""
        if (solverBids.isNotEmpty()) solverBids.size
        else if (uid.isNotBlank()) allBids.count { it.solverId == uid }
        else 0
    }

    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showRoleCategoryDialog by remember { mutableStateOf(false) }
    var showRoleConfirmDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showPhotoUploadSheet by remember { mutableStateOf(false) }
    var isSwitchingRole by remember { mutableStateOf(false) }
    var profileImageUploadError by remember { mutableStateOf<String?>(null) }
    var switchRoleError by remember { mutableStateOf<String?>(null) }
    var switchRoleCategoryError by remember { mutableStateOf<String?>(null) }

    val isSolver = currentUser?.role == "SOLVER"
    val switchBoxBg = if (isSolver) SomadhanOrangeLight else Color(0xFFEFF6FF)
    val switchBoxBorder = if (isSolver) SomadhanOrange.copy(alpha = 0.45f) else Color(0xFFBFDBFE)
    val switchBoxSpot = if (isSolver) SomadhanOrange.copy(alpha = 0.25f) else Color(0x201D4ED8)
    val switchBoxAccent = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8)

    // Photo picker from device gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            profileImageUploadError = null
            viewModel.uploadAndSaveProfileImage(
                uri = uri,
                onSuccess = {
                    profileImageUploadError = null
                    showPhotoUploadSheet = false
                },
                onError = { err ->
                    profileImageUploadError = err
                }
            )
        }
    }

    // Preset avatars list for instant choice
    val presetAvatars = remember {
        listOf(
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=200&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=200&auto=format&fit=crop&q=80"
        )
    }

    if (showNotificationsSheet) {
        NotificationBottomSheet(
            notifications = notifications,
            onDismiss = { showNotificationsSheet = false },
            onMarkAllAsRead = { viewModel.markAllNotificationsRead() },
            onMarkAsRead = { notifId -> viewModel.markNotificationRead(notifId) },
            onNotificationItemClick = { notif ->
                showNotificationsSheet = false
                handleSomadhanNotification(
                    notif = notif,
                    onNavigate = onNavigate,
                    onProblemClick = onProblemClick,
                    isSolver = (currentUser?.role == "SOLVER")
                )
            },
            isSolver = (currentUser?.role == "SOLVER")
        )
    }

    if (showPhotoUploadSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoUploadSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SomadhanBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "প্রোফাইল ছবি পরিবর্তন ও আপলোড",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                    IconButton(onClick = { showPhotoUploadSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = SomadhanTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Gallery upload option
                val uploadBoxBg = if (isSolver) SomadhanOrangeLight else Color(0xFFEFF6FF)
                val uploadBoxAccent = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8)
                val uploadBoxBorder = if (isSolver) SomadhanOrange.copy(alpha = 0.3f) else Color(0xFFBFDBFE)

                Card(
                    colors = CardDefaults.cardColors(containerColor = uploadBoxBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, uploadBoxBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isUploadingProfileImage) {
                            galleryLauncher.launch("image/*")
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(uploadBoxAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingProfileImage) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isUploadingProfileImage) "ছবি ক্লাউডে আপলোড হচ্ছে..." else "গ্যালারি / ডিভাইস থেকে ছবি আপলোড করুন",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = uploadBoxAccent
                            )
                            Text(
                                text = if (isUploadingProfileImage) "অনুগ্রহ করে অপেক্ষা করুন, ফায়ারবেসে সেভ হচ্ছে" else "ফোন মেমরি থেকে পছন্দের ছবি বাছাই করুন",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "অথবা প্রিসেট অবতার নির্বাচন করুন:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SomadhanTextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presetAvatars.take(6).forEach { avatarUrl ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (currentUser?.profileImageUri == avatarUrl) 2.5.dp else 1.dp,
                                    color = if (currentUser?.profileImageUri == avatarUrl) uploadBoxAccent else SomadhanDivider,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.updateProfileImage(avatarUrl)
                                    showPhotoUploadSheet = false
                                }
                        ) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "অবতার",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                if (!currentUser?.profileImageUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = SomadhanDivider)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateProfileImage("")
                                showPhotoUploadSheet = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = SomadhanError, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("বর্তমান ছবি মুছে ডিফল্ট করুন", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SomadhanError)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showRoleCategoryDialog) {
        RoleSwitchCategoryDialog(
            allCategories = allCategories,
            isSubmitting = isSwitchingRole,
            errorMessage = switchRoleCategoryError,
            onDismiss = { if (!isSwitchingRole) { showRoleCategoryDialog = false; switchRoleCategoryError = null } },
            onConfirm = { selectedCats ->
                isSwitchingRole = true
                switchRoleCategoryError = null
                viewModel.switchRoleToSolver(
                    newCategories = selectedCats,
                    onSuccess = {
                        isSwitchingRole = false
                        showRoleCategoryDialog = false
                    },
                    onError = {
                        isSwitchingRole = false
                        switchRoleCategoryError = it
                    }
                )
            }
        )
    }

    if (showRoleConfirmDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { if (!isSwitchingRole) { showRoleConfirmDialog = false; switchRoleError = null } },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(switchBoxBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = switchBoxAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (isSolver) "ইউজার রোলে ফিরে যেতে চান?" else "সমাধানকারী (Solver) হতে চান?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = SomadhanTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isSolver)
                            "আপনি এখন থেকে সমস্যা পোস্ট করতে পারবেন, বিড গ্রহণ করতে পারবেন না।"
                        else
                            "আপনি এখন থেকে অন্যদের সমস্যার সমাধান দিয়ে আয় করতে পারবেন।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    if (switchRoleError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = switchRoleError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("switch_role_error")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSwitchingRole,
                    onClick = {
                        isSwitchingRole = true
                        switchRoleError = null
                        viewModel.requestSwitchRole(
                            onShowCategoryPicker = {
                                isSwitchingRole = false
                                showRoleConfirmDialog = false
                                showRoleCategoryDialog = true
                            },
                            onSuccess = {
                                isSwitchingRole = false
                                showRoleConfirmDialog = false
                            },
                            onError = {
                                isSwitchingRole = false
                                switchRoleError = it
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = switchBoxAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSwitchingRole) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("পরিবর্তন হচ্ছে...")
                    } else {
                        Text("হ্যাঁ, পরিবর্তন করুন")
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !isSwitchingRole, onClick = { showRoleConfirmDialog = false; switchRoleError = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = SomadhanBg
        )
    }

    if (showLogoutConfirmDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("লগআউট নিশ্চিতকরণ", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary) },
            text = { Text("আপনি কি নিশ্চিতভাবে আপনার অ্যাকাউন্ট থেকে লগআউট করতে চান?", color = SomadhanTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError)
                ) {
                    Text("লগআউট")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            SomadhanTopBar(
                title = "প্রোফাইল",
                currentUser = currentUser,
                unreadCount = unreadCount,
                onNotificationClick = { showNotificationsSheet = true },
                onAdminClick = null,
                showReputation = false,
                locationAddress = liveLocation.address,
                onLocationRefresh = { viewModel.refreshLiveLocation() }
            )
        },
        bottomBar = {
            val platformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
            val isWalletEnabled = platformSettings.find { it.key == "menu_wallet_enabled" }?.value != "false"
            val isInstantJobEnabled = platformSettings.find { it.key == "instant_job_feature_enabled" }?.value?.toBooleanStrictOrNull() ?: true
            SomadhanBottomNav(
                currentRoute = "profile",
                isSolver = isSolver,
                isWalletEnabled = isWalletEnabled,
                isInstantJobEnabled = isInstantJobEnabled,
                onNavigate = onNavigate
            )
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        // ধাপ ২.৬ (ব্যাচ ৩১): এই পেজে pull-to-refresh সম্পূর্ণ সরানো হয়েছে (ব্যবহারকারীর স্পষ্ট
        // নির্দেশ)। আগে এখানে SomadhanPullToRefresh wrapper ছিল যেটা fillMaxSize/padding/
        // background modifier বহন করত — সেই modifier টা এখন সরাসরি SyncAwareRefreshableContent-এ
        // দেওয়া হলো, বাকি সব (sessionKey/data/ডাটা-লোড লজিক) অপরিবর্তিত।
        // [শিমার ফিক্স — ধাপ ৫] isManualRefreshing = isRefreshing (global) আগে এখানে রাখা হতো —
        // এটাই বাগ B-এর কারণ ছিল (উপরের ভ্যারিয়েবল ডিক্লেয়ারেশনের কমেন্ট দ্রষ্টব্য)। এখন নিচে
        // সরাসরি false, কারণ এই পেজে কোনো pull-gesture-ই নেই এটা ট্রিগার করার জন্য।
        run {
            // Loading Pattern Master Prompt, ব্যাচ ৬ (B2 migration) — এই স্ক্রিনে কন্টেন্ট
            // সরাসরি বাইরের scope-এর currentUser/myActiveCount/myCancelledCount/
            // myCompletedCount/myBidsCount ব্যবহার করে (নিচে অপরিবর্তিত), তাই content lambda-র
            // প্যারামিটার শুধু diff-trigger হিসেবে ব্যবহৃত — `data = listOf(...)` -এ সবগুলো
            // মূল্য একসাথে রাখা হলো যাতে currentUser বদলালে বা allProblems/allBids বদলে
            // derived count বদলালে (List.equals() structural), দুটো ক্ষেত্রেই শিমার হয়।
            SyncAwareRefreshableContent(
                sessionKey = "profile_sync",
                viewModel = viewModel,
                syncPhase = initialSyncPhase,
                data = listOf(currentUser, myActiveCount, myCancelledCount, myCompletedCount, myBidsCount),
                onRetry = { viewModel.retryInitialSync() },
                isManualRefreshing = false,
                flashOnReentry = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(SomadhanBg)
            ) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
            // Profile Card with Photo on LEFT and User Details on RIGHT
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = if (isSolver) SomadhanOrange.copy(alpha = 0.35f) else Color(0xFF1D4ED8).copy(alpha = 0.35f),
                        ambientColor = if (isSolver) SomadhanOrange.copy(alpha = 0.15f) else Color(0xFF0F172A).copy(alpha = 0.15f)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = if (isSolver) {
                                    listOf(
                                        Color(0xFFFFA23E), // lighter warm orange on the left
                                        Color(0xFFFF851A), // medium smooth transition
                                        Color(0xFFFF5900)  // rich, slightly deeper orange on the right
                                    )
                                } else {
                                    listOf(
                                        Color(0xFF3B82F6), // lighter vibrant royal blue on the left
                                        Color(0xFF1D4ED8), // rich royal blue in the middle
                                        Color(0xFF0F172A)  // deep midnight slate on the right
                                    )
                                }
                            )
                        )
                ) {
                    Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Side: Profile Photo with upload action
                        Box(
                            modifier = Modifier.size(86.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .border(2.dp, Color.White, CircleShape)
                                    .clickable(enabled = !isUploadingProfileImage) { showPhotoUploadSheet = true }
                                    .testTag("profile_photo_avatar"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingProfileImage) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(34.dp),
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    val imageUri = currentUser?.profileImageUri
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
                                                tint = Color.White,
                                                modifier = Modifier.size(50.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "প্রোফাইল ছবি",
                                            tint = Color.White,
                                            modifier = Modifier.size(50.dp)
                                        )
                                    }
                                }
                            }

                            // Camera icon overlay badge on bottom right of the photo
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.5.dp, if (isSolver) SomadhanOrange else Color(0xFF1D4ED8), CircleShape)
                                    .clickable(enabled = !isUploadingProfileImage) { showPhotoUploadSheet = true }
                                    .testTag("upload_photo_badge"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "ছবি আপলোড করুন",
                                    tint = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Right Side: User Information
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = currentUser?.name ?: "ব্যবহারকারী",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                UserVerificationBadge(
                                    role = currentUser?.role ?: "USER",
                                    isKycVerified = currentUser?.isKycVerified == true,
                                    onKycClick = { onNavigate("solver_kyc") }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                ReputationBadge(
                                    score = currentUser?.reputationScore ?: 50.0,
                                    modifier = Modifier.clickable {
                                        onNavigate(Screen.ReputationDetail.createRoute(currentUser?.id ?: ""))
                                    }
                                )
                                if (currentUser?.activeRoleBanned == true || currentUser?.activeRoleRestricted == true) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    AccountStatusIndicator(
                                        isBanned = currentUser?.activeRoleBanned == true,
                                        isRestricted = currentUser?.activeRoleRestricted == true
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Account UID Display - 1-click copyable for all account types
                            // [UID ফিক্স] আগে এখানে সরাসরি `currentUser?.id` দেখানো হতো — এটাই
                            // আসল বাগ ছিল: role switch করলে এটা local dual-row id
                            // ("SOLVER_xxxx"/"USER_xxxx") দেখাত, আর কখনো switch না করলে raw
                            // ৩৬-ক্যারেক্টার Supabase UUID দেখাত — দুটোই অনেক লম্বা। এখন আসল
                            // ছোট, সার্ভার-জেনারেটেড `displayUid` (৬-সংখ্যা থেকে শুরু) দেখানো
                            // হচ্ছে, blank হলে (যেমন এখনো cloud-এর সাথে sync হয়নি এমন পুরনো
                            // local-only demo/admin অ্যাকাউন্ট) আগের raw id fallback হিসেবে থাকছে।
                            val accountId = currentUser?.displayUid?.takeIf { it.isNotBlank() }
                                ?: currentUser?.id ?: ""

                            if (accountId.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.22f))
                                        .border(
                                            0.8.dp,
                                            Color.White.copy(alpha = 0.45f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(accountId))
                                            viewModel.showToast("UID কপি করা হয়েছে: $accountId")
                                        }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                        .testTag("profile_display_uid_chip")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "UID",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "UID: $accountId",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "UID কপি করুন",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            if (!currentUser?.address.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = currentUser?.address ?: "",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ছবি পরিবর্তন",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .background(
                                            Color.White.copy(alpha = 0.22f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(0.8.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .clickable { showPhotoUploadSheet = true }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )

                                Text(
                                    text = "তথ্য সম্পাদন",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8),
                                    modifier = Modifier
                                        .background(
                                            Color.White,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onNavigate(Screen.UserInfo.route) }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    if (profileImageUploadError != null) {
                        Text(
                            text = profileImageUploadError ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.22f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("profile_image_upload_error")
                        )
                    }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // USER QUICK ACTIVITY & STATS SUMMARY (When in User Role)
            if (!isSolver) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(14.dp),
                            spotColor = Color(0x1F000000),
                            ambientColor = Color(0x0D000000)
                        )
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Active Problems
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigate("user_active_problems") }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = DistanceUtil.toBengaliDigits(myActiveCount.toString()),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "চলমান সমস্যা",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SomadhanTextSecondary
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFFE2E8F0)))

                        // 2. Solved Problems
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigate("user_completed_problems") }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = DistanceUtil.toBengaliDigits(myCompletedCount.toString()),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanSuccess
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "সমাধান হয়েছে",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SomadhanTextSecondary
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFFE2E8F0)))

                        // 3. User Reputation / Rating Score
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onNavigate(Screen.ReputationDetail.createRoute(currentUser?.id ?: ""))
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            val repScore = currentUser?.reputationScore ?: 50.0
                            Text(
                                text = DistanceUtil.toBengaliDigits(repScore.toInt().toString()),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "রেপুটেশন স্কোর",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SomadhanTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Role Switch Button (User ↔ Solver switch) with Deep Shadow
            Card(
                colors = CardDefaults.cardColors(containerColor = switchBoxBg),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(14.dp),
                        spotColor = switchBoxSpot,
                        ambientColor = Color(0x14000000)
                    )
                    .border(1.dp, switchBoxBorder, RoundedCornerShape(14.dp))
                    .clickable { showRoleConfirmDialog = true }
                    .testTag("role_switch_button")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = switchBoxAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isSolver) "ইউজার রোলে স্যুইচ করুন" else "সমাধানকারী (Solver) রোলে স্যুইচ করুন",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = switchBoxAccent
                            )
                            Text(
                                text = if (isSolver) "সমস্যা পোস্ট ও সমাধান নিতে ইউজার হন" else "সমস্যার সমাধান দিয়ে আয় করতে সলভার হন",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = switchBoxAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "অ্যাকাউন্ট মেনু",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic Menu Items based on USER or SOLVER with Deep Shadow
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color(0x33000000),
                        ambientColor = Color(0x1F000000)
                    )
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
            ) {
                Column {
                    if (!isSolver) {
                        // USER SPECIFIC MENU ITEMS:
                        // 1. ব্যবহারকারীর তথ্য
                        ProfileMenuItem(
                            title = "ব্যবহারকারীর তথ্য",
                            subtitle = "নাম, ইমেইল ও ঠিকানা সম্পাদনা",
                            icon = Icons.Default.Edit,
                            iconColor = Color(0xFF2563EB),
                            onClick = { onNavigate("user_info") }
                        )

                        // 2. চলমান সমস্যা সমূহ
                        ProfileMenuItem(
                            title = "চলমান সমস্যা সমূহ",
                            subtitle = "${DistanceUtil.toBengaliDigits(myActiveCount.toString())}টি সমস্যা চলমান রয়েছে",
                            icon = Icons.Default.ListAlt,
                            iconColor = Color(0xFF2563EB),
                            onClick = { onNavigate("user_active_problems") }
                        )

                        // 3. বাতিল সমস্যা সমূহ
                        ProfileMenuItem(
                            title = "বাতিলকৃত সমস্যা সমূহ",
                            subtitle = "${DistanceUtil.toBengaliDigits(myCancelledCount.toString())}টি সমস্যা বাতিল করা হয়েছে",
                            icon = Icons.Default.Cancel,
                            iconColor = SomadhanError,
                            onClick = { onNavigate("user_cancelled_problems") }
                        )

                        // 4. সম্পন্ন সমস্যা সমূহ
                        ProfileMenuItem(
                            title = "সম্পন্ন সমস্যা সমূহ",
                            subtitle = "${DistanceUtil.toBengaliDigits(myCompletedCount.toString())}টি সমস্যা সমাধান হয়েছে",
                            icon = Icons.Default.CheckCircle,
                            iconColor = SomadhanSuccess,
                            onClick = { onNavigate("user_completed_problems") }
                        )

                        // 5. রিভিউ সমূহ
                        ProfileMenuItem(
                            title = "রিভিউ সমূহ",
                            subtitle = "সকল রেটিং ও রিভিউ",
                            icon = Icons.Default.RateReview,
                            iconColor = Color(0xFFD97706),
                            onClick = { onNavigate("user_reviews") }
                        )

                        // 6. ওয়ালেট ও উইথড্র
                        ProfileMenuItem(
                            title = "আমার ওয়ালেট",
                            subtitle = "ব্যালেন্স, এসক্রো ও অর্থ উত্তোলন",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconColor = Color(0xFF1D4ED8),
                            onClick = { onNavigate("wallet") }
                        )

                        // 6. পছন্দের সমাধানকারী
                        ProfileMenuItem(
                            title = "পছন্দের সমাধানকারী",
                            subtitle = if (myFavoriteSolversCount > 0) "${DistanceUtil.toBengaliDigits(myFavoriteSolversCount.toString())} জন সমাধানকারী সংরক্ষিত" else "কোনো পছন্দের সমাধানকারী নেই",
                            icon = Icons.Default.Favorite,
                            iconColor = Color(0xFFE11D48),
                            onClick = { onNavigate("favorite_solvers") }
                        )
                    } else {
                        // SOLVER SPECIFIC MENU ITEMS:
                        // 1. সমাধানকারী স্কিল
                        ProfileMenuItem(
                            title = "সমাধানকারী স্কিল",
                            subtitle = "আপনার ১-৩টি ক্যাটাগরি পরিবর্তন করুন",
                            icon = Icons.Default.Category,
                            iconColor = SomadhanOrange,
                            onClick = { onNavigate("solver_skills") }
                        )

                        // আমার বিড
                        ProfileMenuItem(
                            title = "আমার বিড",
                            subtitle = if (myBidsCount > 0) "${DistanceUtil.toBengaliDigits(myBidsCount.toString())}টি বিডের বিবরণ ও অবস্থা" else "চলমান, গৃহীত ও বাতিল বিড",
                            icon = Icons.Default.LocalOffer,
                            iconColor = SomadhanOrange,
                            onClick = { onNavigate("solver_my_bids") }
                        )

                        // চলমান কাজগুলো
                        ProfileMenuItem(
                            title = "চলমান কাজগুলো",
                            subtitle = "আপনার চলমান কাজের তালিকা",
                            icon = Icons.Default.ListAlt,
                            iconColor = SomadhanOrange,
                            onClick = { onNavigate("solver_active_jobs") }
                        )

                        // সম্পন্ন কাজগুলো
                        ProfileMenuItem(
                            title = "সম্পন্ন কাজগুলো",
                            subtitle = "আপনার সম্পন্ন করা কাজের তালিকা",
                            icon = Icons.Default.ListAlt,
                            iconColor = SomadhanSuccess,
                            onClick = { onNavigate("solver_own_completed_jobs") }
                        )

                        // 2. KYC ভেরিফিকেশন (Strictly "KYC ভেরিফিকেশন", NO NID wording)
                        ProfileMenuItem(
                            title = "KYC ভেরিফিকেশন",
                            subtitle = if (currentUser?.isKycVerified == true) "যাচাইকৃত (Verified)" else "ভেরিফিকেশন আবশ্যক",
                            icon = Icons.Default.VerifiedUser,
                            iconColor = if (currentUser?.isKycVerified == true) SomadhanSuccess else SomadhanOrange,
                            onClick = { onNavigate("solver_kyc") }
                        )

                        // 3. ব্যালেন্স ও উইথড্র
                        ProfileMenuItem(
                            title = "ব্যালেন্স ও উইথড্র",
                            subtitle = "বিকাশ, নগদ, রকেট বা ব্যাংকে উত্তোলন",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconColor = SomadhanOrange,
                            onClick = { onNavigate("solver_balance_withdraw") }
                        )

                        // 4. প্রাপ্ত রিভিউ সমূহ
                        ProfileMenuItem(
                            title = "প্রাপ্ত রিভিউ সমূহ",
                            subtitle = "ক্লায়েন্টদের থেকে পাওয়া রেটিং ও মতামত",
                            icon = Icons.Default.RateReview,
                            iconColor = SomadhanOrange,
                            onClick = { onNavigate("solver_reviews") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Logout Button with Shadow
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanErrorLight),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(14.dp),
                        spotColor = SomadhanError.copy(alpha = 0.2f),
                        ambientColor = Color(0x14000000)
                    )
                    .clickable { showLogoutConfirmDialog = true }
                    .border(1.dp, SomadhanError.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .testTag("logout_button")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "লগআউট",
                        tint = SomadhanError,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "লগআউট করুন",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanError
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
            } // SyncAwareRefreshableContent (ব্যাচ ৬ B2 migration) বন্ধ
    } // run{} (ধাপ ২.৬, ব্যাচ ৩১ — আগে SomadhanPullToRefresh ছিল, এখন সরানো হয়েছে) বন্ধ
}
}

@Composable
fun ProfileMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = SomadhanOrange,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SomadhanTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = SomadhanTextHint
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = SomadhanTextHint,
            modifier = Modifier.size(16.dp)
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SomadhanDivider.copy(alpha = 0.6f))
    )
}
