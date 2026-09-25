@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.AdditionalChargeEntity
import com.example.data.entity.AdminAuditLogEntity
import com.example.data.entity.BidEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.EscrowEntity
import com.example.data.entity.FaqEntity
import com.example.data.entity.MessageEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.RatingEntity
import com.example.data.entity.ReputationEventEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.WithdrawalEntity
import com.example.data.repository.AdminDashboardMetrics
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.ReputationBadge
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.navigation.Screen
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
import com.example.ui.theme.SomadhanYellowVerified
import com.example.ui.theme.SomadhanYellowVerifiedBg
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.CsvExportUtil
import com.example.util.DistanceUtil
import com.example.util.FileAttachmentUtil
import com.example.util.Formatters
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.text.SimpleDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.example.ui.components.BottomSlideAlertDialog

@Composable
fun AdminSettingsView(
    viewModel: SomadhanViewModel,
    onLogout: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val platformSettings by viewModel.allPlatformSettings.collectAsStateWithLifecycle()
    val commissionSetting = platformSettings.find { it.key == "commission_percent" }?.value ?: "10.0"
    val minWithdrawalSetting = platformSettings.find { it.key == "min_withdrawal" }?.value ?: "100.0"
    val urgencyLevelsSetting = platformSettings.find { it.key == "urgency_levels" }?.value ?: "সাধারণ,জরুরি,খুব জরুরি"
    val maintenanceModeSetting = platformSettings.find { it.key == "maintenance_mode" }?.value ?: "false"
    val maintenanceMsgSetting = platformSettings.find { it.key == "maintenance_message" }?.value ?: "অ্যাপটি বর্তমানে রক্ষণাবেক্ষণের জন্য বন্ধ আছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
    val minAppVersionSetting = platformSettings.find { it.key == "min_app_version" }?.value ?: "1"
    val physicalRadiusSetting = platformSettings.find { it.key == "physical_category_radius_km" }?.value ?: "10"
    // [Offline Action Gating ধাপ ২] `strict_offline_block` -- না থাকলে ডিফল্ট "true" (ব্যাকওয়ার্ড-
    // কম্প্যাটিবল, বর্তমান strict full-block আচরণ অপরিবর্তিত থাকে যতক্ষণ না admin সরাসরি off করেন)।
    val strictOfflineBlockSetting = platformSettings.find { it.key == "strict_offline_block" }?.value ?: "true"

    var commissionInput by remember(commissionSetting) { mutableStateOf(commissionSetting) }
    var minWithdrawalInput by remember(minWithdrawalSetting) { mutableStateOf(minWithdrawalSetting) }
    var newUrgencyInput by remember { mutableStateOf("") }
    var maintenanceMsgInput by remember(maintenanceMsgSetting) { mutableStateOf(maintenanceMsgSetting) }
    var minAppVersionInput by remember(minAppVersionSetting) { mutableStateOf(minAppVersionSetting) }
    var physicalRadiusInput by remember(physicalRadiusSetting) { mutableStateOf(physicalRadiusSetting) }

    var currentSavedPhone by remember { mutableStateOf("") }
    var isDefaultCredentials by remember { mutableStateOf(false) }

    var newPhoneInput by remember { mutableStateOf("") }
    var currentPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }

    var isCurrentPasswordVisible by remember { mutableStateOf(false) }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(false) }
    var showLogoutConfirmationDialog by remember { mutableStateOf(false) }
    var showFactoryResetDialog by remember { mutableStateOf(false) }
    var factoryResetConfirmText by remember { mutableStateOf("") }
    var isFactoryResetting by remember { mutableStateOf(false) }
    var factoryResetError by remember { mutableStateOf<String?>(null) }

    // Load admin current phone and default status
    androidx.compose.runtime.LaunchedEffect(Unit) {
        // [ADMIN_ROLE_PROFILE সেশন ২] বর্তমানে লগইন-করা এডমিনের ফোন (আগে একক AdminCredentials phone);
        // "ডিফল্ট ক্রেডেনশিয়াল" ব্যানার আর প্রযোজ্য না — প্রতিটা এডমিনের নিজস্ব Supabase Auth পাসওয়ার্ড।
        currentSavedPhone = com.example.data.security.AdminSession.current?.account?.phone.orEmpty()
        isDefaultCredentials = false
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmationDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showLogoutConfirmationDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SomadhanErrorLight.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "লগআউট",
                        tint = SomadhanError,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "অ্যাডমিন লগআউট",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিতভাবে অ্যাডমিন কন্ট্রোল প্যানেল থেকে লগআউট করতে চান? পুনরায় প্রবেশ করতে আপনার অ্যাডমিন ক্রেডেনশিয়াল প্রয়োজন হবে।",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmationDialog = false
                        viewModel.showToast("অ্যাডমিন প্যানেল থেকে সফলভাবে লগআউট হয়েছে।")
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("admin_confirm_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("লগআউট করুন", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutConfirmationDialog = false },
                    modifier = Modifier.testTag("admin_cancel_logout_button")
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary, fontWeight = FontWeight.Medium)
                }
            },
            containerColor = SomadhanCardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Factory Reset Confirmation Dialog (Admin Action bug-fix master prompt, ধাপ ৯) --
    // destructive/irreversible অপারেশন বলে সাধারণ Yes/No না, admin-কে একটা নির্দিষ্ট phrase
    // টাইপ করতে হবে, exact match না হলে বাটন disabled থাকে -- ভুল করে ট্রিগার হওয়া আটকাতে।
    if (showFactoryResetDialog) {
        val requiredPhrase = "মুছে ফেলো"
        BottomSlideAlertDialog(
            onDismissRequest = {
                if (!isFactoryResetting) {
                    showFactoryResetDialog = false
                    factoryResetConfirmText = ""
                    factoryResetError = null
                }
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SomadhanErrorLight.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "সতর্কতা",
                        tint = SomadhanError,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "⚠️ সম্পূর্ণ ফ্যাক্টরি রিসেট",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "এই অপারেশন লোকাল এবং cloud (Supabase) উভয় জায়গার সমস্ত ইউজার, সমস্যা, বিড, লেনদেন, escrow -- সবকিছু স্থায়ীভাবে মুছে ফেলবে। এটা ফেরানো সম্ভব না।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "নিশ্চিত করতে নিচে হুবহু লিখুন: \"$requiredPhrase\"",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanError
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = factoryResetConfirmText,
                        onValueChange = { factoryResetConfirmText = it },
                        enabled = !isFactoryResetting,
                        placeholder = { Text(requiredPhrase, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanError,
                            unfocusedBorderColor = SomadhanBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (factoryResetError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = factoryResetError ?: "", fontSize = 12.sp, color = SomadhanError)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isFactoryResetting = true
                        factoryResetError = null
                        viewModel.adminFactoryResetAllData(
                            onSuccess = {
                                isFactoryResetting = false
                                showFactoryResetDialog = false
                                factoryResetConfirmText = ""
                                onLogout()
                            },
                            onError = { err ->
                                isFactoryResetting = false
                                factoryResetError = err
                            }
                        )
                    },
                    enabled = !isFactoryResetting && factoryResetConfirmText.trim() == requiredPhrase,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isFactoryResetting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("রিসেট হচ্ছে...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Text("সব মুছে ফ্যাক্টরি রিসেট করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isFactoryResetting,
                    onClick = {
                        showFactoryResetDialog = false
                        factoryResetConfirmText = ""
                        factoryResetError = null
                    }
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary, fontWeight = FontWeight.Medium)
                }
            },
            containerColor = SomadhanCardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Warning Banner if using default credentials
        if (isDefaultCredentials) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "সতর্কতা",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "⚠️ ডিফল্ট ডেমো ক্রেডেনশিয়াল সক্রিয়",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "আপনি এখনো ডিফল্ট ডেমো অ্যাডমিন তথ্য ব্যবহার করছেন। নিরাপত্তার জন্য এখনই নম্বর ও পাসওয়ার্ড পরিবর্তন করুন।",
                            fontSize = 12.sp,
                            color = Color(0xFFBF360C),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // Credentials Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = SomadhanOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "অ্যাডমিন ক্রেডেনশিয়াল সেটিংস",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Admin Phone Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "বর্তমান অ্যাডমিন ফোন নম্বর",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = if (currentSavedPhone.isNotBlank()) currentSavedPhone else "লোড হচ্ছে...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = SomadhanDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // New Phone Input
                Text(
                    text = "ফোন নম্বর (লগইন আইডি — বদলানো যায় না)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = newPhoneInput,
                    onValueChange = { newPhoneInput = it },
                    // [ADMIN_ROLE_PROFILE সেশন ২] ফোন = প্রতিটা এডমিনের Supabase Auth লগইন-আইডি, তাই নিষ্ক্রিয়।
                    enabled = false,
                    placeholder = { Text("ফোন নম্বর বদলানো যায় না", fontSize = 12.sp, color = SomadhanTextHint) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SomadhanTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        unfocusedBorderColor = SomadhanBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Current Password (Required)
                Text(
                    text = "বর্তমান পাসওয়ার্ড (যাচাইয়ের জন্য আবশ্যক) *",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = currentPasswordInput,
                    onValueChange = { currentPasswordInput = it },
                    placeholder = { Text("বর্তমান অ্যাডমিন পাসওয়ার্ড লিখুন", fontSize = 12.sp, color = SomadhanTextHint) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SomadhanTextSecondary) },
                    trailingIcon = {
                        IconButton(onClick = { isCurrentPasswordVisible = !isCurrentPasswordVisible }) {
                            Icon(
                                imageVector = if (isCurrentPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isCurrentPasswordVisible) "পাসওয়ার্ড লুকান" else "পাসওয়ার্ড দেখুন",
                                tint = SomadhanTextSecondary
                            )
                        }
                    },
                    visualTransformation = if (isCurrentPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        unfocusedBorderColor = SomadhanBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // New Password
                Text(
                    text = "নতুন পাসওয়ার্ড (ঐচ্ছিক)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = newPasswordInput,
                    onValueChange = { newPasswordInput = it },
                    placeholder = { Text("কমপক্ষে ৮ অক্ষরের নতুন পাসওয়ার্ড", fontSize = 12.sp, color = SomadhanTextHint) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SomadhanTextSecondary) },
                    trailingIcon = {
                        IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                            Icon(
                                imageVector = if (isNewPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isNewPasswordVisible) "পাসওয়ার্ড লুকান" else "পাসওয়ার্ড দেখুন",
                                tint = SomadhanTextSecondary
                            )
                        }
                    },
                    visualTransformation = if (isNewPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        unfocusedBorderColor = SomadhanBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Confirm New Password
                Text(
                    text = "নতুন পাসওয়ার্ড নিশ্চিত করুন",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = confirmPasswordInput,
                    onValueChange = { confirmPasswordInput = it },
                    placeholder = { Text("নতুন পাসওয়ার্ডটি পুনরায় লিখুন", fontSize = 12.sp, color = SomadhanTextHint) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SomadhanTextSecondary) },
                    trailingIcon = {
                        IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                            Icon(
                                imageVector = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isConfirmPasswordVisible) "পাসওয়ার্ড লুকান" else "পাসওয়ার্ড দেখুন",
                                tint = SomadhanTextSecondary
                            )
                        }
                    },
                    visualTransformation = if (isConfirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        unfocusedBorderColor = SomadhanBorder
                    )
                )

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = statusMessage ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isErrorMessage) SomadhanError else SomadhanSuccess
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save Button
                Button(
                    onClick = {
                        if (currentPasswordInput.isBlank()) {
                            statusMessage = "বর্তমান পাসওয়ার্ড প্রদান করা বাধ্যতামূলক।"
                            isErrorMessage = true
                            return@Button
                        }

                        if (newPhoneInput.isBlank() && newPasswordInput.isBlank()) {
                            statusMessage = "নতুন ফোন নম্বর বা নতুন পাসওয়ার্ডের অন্তত একটি প্রদান করুন।"
                            isErrorMessage = true
                            return@Button
                        }

                        if (newPasswordInput.isNotBlank()) {
                            if (newPasswordInput.length < 8) {
                                statusMessage = "নতুন পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে।"
                                isErrorMessage = true
                                return@Button
                            }
                            if (newPasswordInput != confirmPasswordInput) {
                                statusMessage = "নতুন পাসওয়ার্ড এবং নিশ্চিতকরণ পাসওয়ার্ড মেলেনি।"
                                isErrorMessage = true
                                return@Button
                            }
                        }

                        isSaving = true
                        statusMessage = null

                        viewModel.adminUpdateCredentials(
                            newPhone = newPhoneInput,
                            currentPassword = currentPasswordInput,
                            newPassword = newPasswordInput
                        ) { success, msg ->
                            isSaving = false
                            statusMessage = msg
                            isErrorMessage = !success
                            if (success) {
                                currentPasswordInput = ""
                                newPasswordInput = ""
                                confirmPasswordInput = ""
                                newPhoneInput = ""
                                currentSavedPhone = com.example.data.security.AdminSession.current?.account?.phone.orEmpty()
                                isDefaultCredentials = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "পরিবর্তন সংরক্ষণ করুন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // a) Commission & Withdrawal Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = SomadhanOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "কমিশন ও উইথড্রয়াল সেটিংস",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Commission Percent Field
                Text(
                    text = "প্ল্যাটফর্ম কমিশন হার (%)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commissionInput,
                        onValueChange = { commissionInput = it },
                        placeholder = { Text("যেমন: 10.0", fontSize = 12.sp, color = SomadhanTextHint) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val value = commissionInput.trim().toDoubleOrNull()
                            if (value != null && value >= 0.0) {
                                viewModel.adminUpdatePlatformSetting("commission_percent", commissionInput.trim())
                            } else {
                                viewModel.showToast("সঠিক সংখ্যা প্রদান করুন।")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("সংরক্ষণ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Min Withdrawal Amount Field
                Text(
                    text = "সর্বনিম্ন উত্তোলনের পরিমাণ (৳)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = minWithdrawalInput,
                        onValueChange = { minWithdrawalInput = it },
                        placeholder = { Text("যেমন: 100", fontSize = 12.sp, color = SomadhanTextHint) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val value = minWithdrawalInput.trim().toDoubleOrNull()
                            if (value != null && value > 0.0) {
                                viewModel.adminUpdatePlatformSetting("min_withdrawal", minWithdrawalInput.trim())
                            } else {
                                viewModel.showToast("সঠিক সংখ্যা প্রদান করুন।")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("সংরক্ষণ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // b) Urgency Levels Card
        val urgencyList = remember(urgencyLevelsSetting) {
            urgencyLevelsSetting.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = SomadhanOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "জরুরি অবস্থা লেভেল",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "সমস্যা পোস্ট করার সময় ব্যবহারকারী এই লেভেলগুলো বেছে নিতে পারবেন:",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // List of current urgency levels
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    urgencyList.forEachIndexed { index, level ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SomadhanBg, RoundedCornerShape(8.dp))
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SomadhanOrange)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = level,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanTextPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    val updated = urgencyList.filterIndexed { i, _ -> i != index }
                                    if (updated.isNotEmpty()) {
                                        viewModel.adminUpdatePlatformSetting("urgency_levels", updated.joinToString(","))
                                    } else {
                                        viewModel.showToast("কমপক্ষে একটি লেভেল থাকা আবশ্যক।")
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "মুছুন",
                                    tint = SomadhanError,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add new urgency level
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newUrgencyInput,
                        onValueChange = { newUrgencyInput = it },
                        placeholder = { Text("নতুন লেভেল নাম (যেমন: অতি জরুরি)", fontSize = 12.sp, color = SomadhanTextHint) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmed = newUrgencyInput.trim()
                            if (trimmed.isNotBlank()) {
                                if (urgencyList.contains(trimmed)) {
                                    viewModel.showToast("এই লেভেলটি ইতিমধ্যে বিদ্যমান।")
                                } else {
                                    val updated = urgencyList + trimmed
                                    viewModel.adminUpdatePlatformSetting("urgency_levels", updated.joinToString(","))
                                    newUrgencyInput = ""
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("যোগ করুন", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // c) Maintenance Mode Card
        val isMaintenanceOn = maintenanceModeSetting == "true"
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(
                if (isMaintenanceOn) 1.5.dp else 1.dp,
                if (isMaintenanceOn) SomadhanError else SomadhanBorder
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isMaintenanceOn) Icons.Default.Warning else Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (isMaintenanceOn) SomadhanError else SomadhanOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "মেইনটেন্যান্স মোড",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isMaintenanceOn) SomadhanError else SomadhanTextPrimary
                        )
                    }
                    Switch(
                        checked = isMaintenanceOn,
                        onCheckedChange = { checked ->
                            viewModel.adminUpdatePlatformSetting("maintenance_mode", if (checked) "true" else "false")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SomadhanError,
                            uncheckedThumbColor = SomadhanTextSecondary,
                            uncheckedTrackColor = SomadhanDivider
                        )
                    )
                }

                if (isMaintenanceOn) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SomadhanErrorLight.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = SomadhanError, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚠️ মেইনটেন্যান্স মোড সক্রিয় আছে! সাধারণ ব্যবহারকারীরা অ্যাপে রক্ষণাবেক্ষণ নোটিশ দেখতে পাবেন।",
                                fontSize = 12.sp,
                                color = SomadhanError,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "মেইনটেন্যান্স বার্তা",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = maintenanceMsgInput,
                    onValueChange = { maintenanceMsgInput = it },
                    placeholder = { Text("ব্যবহারকারীদের প্রদর্শনের বার্তা লিখুন...", fontSize = 12.sp, color = SomadhanTextHint) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        unfocusedBorderColor = SomadhanBorder
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (maintenanceMsgInput.isNotBlank()) {
                            viewModel.adminUpdatePlatformSetting("maintenance_message", maintenanceMsgInput.trim())
                        } else {
                            viewModel.showToast("বার্তা খালি রাখা যাবে না।")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("সংরক্ষণ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // [Offline Action Gating ধাপ ২] Strict Offline Block টগল কার্ড।
        // ON (ডিফল্ট) = পুরনো আচরণ অপরিবর্তিত (network না থাকলে পুরো অ্যাপ full-block, NoInternetOverlay)।
        // OFF হলে (ভবিষ্যৎ ধাপে ওয়্যার হবে) network না থাকলেও cached ডেটা দেখা যাবে, শুধু action ব্লক হবে।
        // ⚠️ এই ধাপে (ধাপ ২) শুধু toggle infra + local cache যোগ হলো -- MainActivity.kt-এর মূল
        // ব্লকিং লজিক এখনো এই মান read করে না (সেটা ধাপ ৩-এ হবে), তাই আপাতত টগল off করলেও অ্যাপের
        // real-world আচরণ এখনই বদলাবে না -- নিচের সতর্কবার্তায় সেটা admin-কে স্পষ্ট করে জানানো হচ্ছে।
        val isStrictOfflineBlockOn = strictOfflineBlockSetting != "false"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_strict_offline_block_card"),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(
                if (isStrictOfflineBlockOn) 1.5.dp else 1.dp,
                if (isStrictOfflineBlockOn) SomadhanOrange else SomadhanBorder
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isStrictOfflineBlockOn) SomadhanOrange else SomadhanTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "স্ট্রিক্ট অফলাইন ব্লক (Strict Offline Block)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SomadhanTextPrimary
                        )
                    }
                    Switch(
                        checked = isStrictOfflineBlockOn,
                        onCheckedChange = { checked ->
                            viewModel.adminUpdatePlatformSetting("strict_offline_block", if (checked) "true" else "false")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SomadhanOrange,
                            uncheckedThumbColor = SomadhanTextSecondary,
                            uncheckedTrackColor = SomadhanDivider
                        ),
                        modifier = Modifier.testTag("admin_strict_offline_block_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "চালু (ডিফল্ট) থাকলে ইন্টারনেট সংযোগ না থাকলে ব্যবহারকারীরা পুরো অ্যাপে ঢুকতে পারবেন না (আগের আচরণ)। বন্ধ করলে সংযোগ না থাকলেও আগের সংগৃহীত তথ্য দেখা যাবে, শুধু নতুন কিছু submit/save করা যাবে না।",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "প্রস্তুতিমূলক ধাপ: এই টগল আপাতত শুধু সংরক্ষিত হচ্ছে, অ্যাপের প্রকৃত ব্লকিং আচরণ এখনো এর সাথে যুক্ত হয়নি -- পরবর্তী আপডেটে সক্রিয় হবে।",
                            fontSize = 11.sp,
                            color = SomadhanTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Extra Bill Commission Discount Toggle Card
        val extraCommissionEnabled = platformSettings.find { it.key == "extra_amount_commission_enabled" }?.value != "false"
        val extraCommissionDiscountSetting = platformSettings.find { it.key == "extra_amount_commission_discount_percent" }?.value
            ?: platformSettings.find { it.key == "extra_amount_commission_percent" }?.value ?: "50.0"
        var extraCommissionDiscountInput by remember(extraCommissionDiscountSetting) { mutableStateOf(extraCommissionDiscountSetting) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(
                if (extraCommissionEnabled) 1.5.dp else 1.dp,
                if (extraCommissionEnabled) SomadhanOrange else SomadhanBorder
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = if (extraCommissionEnabled) SomadhanOrange else SomadhanTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "অতিরিক্ত বিল কমিশন ছাড় (Extra Bill Commission Discount)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SomadhanTextPrimary
                        )
                    }
                    Switch(
                        checked = extraCommissionEnabled,
                        onCheckedChange = { checked ->
                            viewModel.adminUpdatePlatformSetting(
                                "extra_amount_commission_enabled",
                                if (checked) "true" else "false"
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SomadhanOrange,
                            uncheckedThumbColor = SomadhanTextSecondary,
                            uncheckedTrackColor = SomadhanDivider
                        ),
                        modifier = Modifier.testTag("toggle_extra_amount_commission_enabled")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (extraCommissionEnabled)
                        "চালু থাকলে অতিরিক্ত বিলের স্বাভাবিক প্ল্যাটফর্ম কমিশনের উপর নির্ধারিত হারে ছাড় (discount) দেওয়া হবে।"
                    else
                        "বন্ধ আছে — অতিরিক্ত বিলের উপর কোনো ছাড় ছাড়া পুরো স্বাভাবিক কমিশন কাটা হচ্ছে।",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 16.sp
                )

                if (extraCommissionEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "কমিশনে ছাড়ের হার (%)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "যেমন normal commission ৩০ টাকা হলে, ৫০% ছাড়ে সলভার/গ্রাহক থেকে মাত্র ১৫ টাকা কমিশন কাটা হবে।",
                        fontSize = 11.sp,
                        color = SomadhanOrange,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = extraCommissionDiscountInput,
                            onValueChange = { extraCommissionDiscountInput = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SomadhanOrange,
                                unfocusedBorderColor = SomadhanBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val parsed = extraCommissionDiscountInput.toDoubleOrNull()
                                if (parsed != null && parsed in 0.0..100.0) {
                                    viewModel.adminUpdatePlatformSetting(
                                        "extra_amount_commission_discount_percent",
                                        parsed.toString()
                                    )
                                    viewModel.adminUpdatePlatformSetting(
                                        "extra_amount_commission_percent",
                                        parsed.toString()
                                    )
                                    viewModel.showToast("অতিরিক্ত বিলে কমিশন ছাড়ের হার আপডেট হয়েছে।")
                                } else {
                                    viewModel.showToast("০ থেকে ১০০ এর মধ্যে একটা সঠিক সংখ্যা দিন।")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("সংরক্ষণ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Free-Quota & Reputation Settings Card
        val freeQuotaEnabledSetting = platformSettings.find { it.key == "free_quota_enabled" }?.value ?: "true"
        val isFreeQuotaEnabled = freeQuotaEnabledSetting == "true"
        val freeQuotaRepThresholdSetting = platformSettings.find { it.key == "free_quota_reputation_threshold" }?.value ?: "80.0"
        val freeQuotaJobCountSetting = platformSettings.find { it.key == "free_quota_job_count" }?.value ?: "10"
        val extraBillRepCapSetting = platformSettings.find { it.key == "extra_bill_reputation_cap_per_problem" }?.value ?: "10.0"

        var freeQuotaRepThresholdInput by remember(freeQuotaRepThresholdSetting) { mutableStateOf(freeQuotaRepThresholdSetting) }
        var freeQuotaJobCountInput by remember(freeQuotaJobCountSetting) { mutableStateOf(freeQuotaJobCountSetting) }
        var extraBillRepCapInput by remember(extraBillRepCapSetting) { mutableStateOf(extraBillRepCapSetting) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(
                if (isFreeQuotaEnabled) 1.5.dp else 1.dp,
                if (isFreeQuotaEnabled) SomadhanOrange.copy(alpha = 0.6f) else SomadhanBorder
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SomadhanOrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ফ্রি-কোটা ও রেপুটেশন সেটিংস",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = "রেপুটেশন অনুযায়ী মাসিক কমিশন-মুক্ত সুবিধা",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isFreeQuotaEnabled,
                        onCheckedChange = { checked ->
                            viewModel.adminUpdatePlatformSetting("free_quota_enabled", if (checked) "true" else "false")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SomadhanOrange,
                            uncheckedThumbColor = SomadhanTextSecondary,
                            uncheckedTrackColor = SomadhanDivider
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "সলভারদের রেপুটেশন স্কোর অনুযায়ী মাসিক কমিশন-মুক্ত সুবিধা এবং অতিরিক্ত বিল থেকে অর্জিত রেপুটেশন ক্যাপ নির্ধারণ করুন।",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 16.sp
                )

                if (isFreeQuotaEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "ফ্রি কমিশন পাওয়ার জন্য ন্যূনতম Reputation",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = freeQuotaRepThresholdInput,
                        onValueChange = { freeQuotaRepThresholdInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "মাসে কতটা কাজ কমিশন-ফ্রি",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = freeQuotaJobCountInput,
                        onValueChange = { freeQuotaJobCountInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "প্রতি পোস্টে Extra Bill থেকে সর্বোচ্চ Reputation পয়েন্ট",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = extraBillRepCapInput,
                        onValueChange = { extraBillRepCapInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val thresholdParsed = freeQuotaRepThresholdInput.toDoubleOrNull()
                            val countParsed = freeQuotaJobCountInput.toIntOrNull()
                            val capParsed = extraBillRepCapInput.toDoubleOrNull()

                            if (thresholdParsed == null || thresholdParsed < 0.0) {
                                viewModel.showToast("ন্যূনতম রেপুটেশন থ্রেশহোল্ড সঠিক সংখ্যা দিন।")
                                return@Button
                            }
                            if (countParsed == null || countParsed < 0) {
                                viewModel.showToast("কমিশন-ফ্রি কাজের সংখ্যা সঠিক সংখ্যা দিন।")
                                return@Button
                            }
                            if (capParsed == null || capParsed < 0.0) {
                                viewModel.showToast("রেপুটেশন ক্যাপের জন্য সঠিক সংখ্যা দিন।")
                                return@Button
                            }

                            viewModel.adminUpdatePlatformSetting("free_quota_reputation_threshold", thresholdParsed.toString())
                            viewModel.adminUpdatePlatformSetting("free_quota_job_count", countParsed.toString())
                            viewModel.adminUpdatePlatformSetting("extra_bill_reputation_cap_per_problem", capParsed.toString())
                            viewModel.showToast("ফ্রি-কোটা ও রেপুটেশন সেটিংস সংরক্ষিত হয়েছে।")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("সংরক্ষণ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        viewModel.adminCleanupCorruptedCommissionRates()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanOrange),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🧹 ওপেন (OPEN) জব কমিশন রেট ক্লিনআপ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Extra Payment Miss-Cycle Settings Card (STEP 6-B)
        val extraPaymentMissRuleEnabledSetting = platformSettings.find { it.key == "extra_payment_miss_rule_enabled" }?.value ?: "true"
        val isExtraPaymentMissRuleEnabled = extraPaymentMissRuleEnabledSetting == "true"
        val extraPaymentMissCycleSizeSetting = platformSettings.find { it.key == "extra_payment_miss_cycle_size" }?.value ?: "10"
        val extraPaymentMissThresholdSetting = platformSettings.find { it.key == "extra_payment_miss_threshold" }?.value ?: "3"
        val extraPaymentMissPenaltySetting = platformSettings.find { it.key == "extra_payment_miss_penalty" }?.value ?: "5.0"

        var extraPaymentMissCycleSizeInput by remember(extraPaymentMissCycleSizeSetting) { mutableStateOf(extraPaymentMissCycleSizeSetting) }
        var extraPaymentMissThresholdInput by remember(extraPaymentMissThresholdSetting) { mutableStateOf(extraPaymentMissThresholdSetting) }
        var extraPaymentMissPenaltyInput by remember(extraPaymentMissPenaltySetting) { mutableStateOf(extraPaymentMissPenaltySetting) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(
                if (isExtraPaymentMissRuleEnabled) 1.5.dp else 1.dp,
                if (isExtraPaymentMissRuleEnabled) SomadhanOrange.copy(alpha = 0.6f) else SomadhanBorder
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SomadhanOrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Extra Payment মিস-সাইকেল পেনাল্টি",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = "ধারাবাহিক কাজে অতিরিক্ত বিল মিসের উপর পেনাল্টি সাইকেল",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isExtraPaymentMissRuleEnabled,
                        onCheckedChange = { checked ->
                            viewModel.adminUpdatePlatformSetting("extra_payment_miss_rule_enabled", if (checked) "true" else "false")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SomadhanOrange,
                            uncheckedThumbColor = SomadhanTextSecondary,
                            uncheckedTrackColor = SomadhanDivider
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "প্রতিটি সলভারের নির্দিষ্ট সাইকেল কাজের মধ্যে অতিরিক্ত বিল আদায় করতে না পারলে স্বয়ংক্রিয়ভাবে রেপুটেশন পেনাল্টি প্রয়োগ করা হবে।",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 16.sp
                )

                if (isExtraPaymentMissRuleEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "সাইকেল কাজের সংখ্যা (Cycle Size)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = extraPaymentMissCycleSizeInput,
                        onValueChange = { extraPaymentMissCycleSizeInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "সর্বোচ্চ অনুমোদিত মিস সংখ্যা (Threshold)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = extraPaymentMissThresholdInput,
                        onValueChange = { extraPaymentMissThresholdInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "পেনাল্টি পয়েন্ট (Reputation Penalty)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = extraPaymentMissPenaltyInput,
                        onValueChange = { extraPaymentMissPenaltyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val cycleParsed = extraPaymentMissCycleSizeInput.toIntOrNull()
                            val threshParsed = extraPaymentMissThresholdInput.toIntOrNull()
                            val penParsed = extraPaymentMissPenaltyInput.toDoubleOrNull()

                            if (cycleParsed == null || cycleParsed <= 0) {
                                viewModel.showToast("সাইকেল কাজের সংখ্যা ১ বা তার বেশি দিন।")
                                return@Button
                            }
                            if (threshParsed == null || threshParsed <= 0 || threshParsed > cycleParsed) {
                                viewModel.showToast("মিস সীমা ১ থেকে সাইকেল সাইজের সমান বা কম হতে হবে।")
                                return@Button
                            }
                            if (penParsed == null || penParsed < 0.0) {
                                viewModel.showToast("সঠিক পেনাল্টি পয়েন্ট দিন।")
                                return@Button
                            }

                            viewModel.adminUpdatePlatformSetting("extra_payment_miss_cycle_size", cycleParsed.toString())
                            viewModel.adminUpdatePlatformSetting("extra_payment_miss_threshold", threshParsed.toString())
                            viewModel.adminUpdatePlatformSetting("extra_payment_miss_penalty", penParsed.toString())
                            viewModel.showToast("Extra Payment মিস-সাইকেল সেটিংস সংরক্ষিত হয়েছে।")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("সংরক্ষণ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ----------------------------------------------------
        // STEP 10: FREE-QUOTA & CYCLE MONITORING SUMMARY & GLOBAL ACTION
        // ----------------------------------------------------
        val currentThresholdVal = platformSettings.find { it.key == "free_quota_reputation_threshold" }?.value?.toDoubleOrNull() ?: 80.0
        val currentQuotaLimitVal = platformSettings.find { it.key == "free_quota_job_count" }?.value?.toIntOrNull() ?: 10
        val currentCycleSizeVal = platformSettings.find { it.key == "extra_payment_miss_cycle_size" }?.value?.toIntOrNull() ?: 10
        val currentMissThresholdVal = platformSettings.find { it.key == "extra_payment_miss_threshold" }?.value?.toIntOrNull() ?: 3
        val currentMonthKey = remember {
            java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date())
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SomadhanOrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "গ্লোবাল ফ্রি-কোটা ও সাইকেল নিয়ন্ত্রণ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = "সকল সলভারের মাসিক ফ্রি কোটা এক ক্লিকে রিসেট",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Threshold & Quota Info Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SomadhanBg)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ন্যূনতম রেপুটেশন: ${DistanceUtil.toBengaliDigits(String.format(Locale.US, "%.0f", currentThresholdVal))} | মাসিক ফ্রি কোটা: ${DistanceUtil.toBengaliDigits(currentQuotaLimitVal.toString())} টি",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = "চলতি মাস: $currentMonthKey",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrange
                            )
                        }
                        Text(
                            text = "মিস সাইকেল: ${DistanceUtil.toBengaliDigits(currentCycleSizeVal.toString())} কাজে ${DistanceUtil.toBengaliDigits(currentMissThresholdVal.toString())} মিসে পেনাল্টি",
                            fontSize = 10.sp,
                            color = SomadhanTextHint
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                viewModel.adminRunMonthlyFreeQuotaReset()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanOrange),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanOrange),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("🔄 সকল সলভারের মাসিক কোটা রিসেট করুন ($currentMonthKey)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Direct link info to the dedicated "সলভার কোটা" menu
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SomadhanOrangeLight.copy(alpha = 0.5f))
                        .border(0.5.dp, SomadhanOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "কোনো নির্দিষ্ট সলভারের কোটা ও সাইকেল অনুসন্ধান, পর্যবেক্ষণ বা এককভাবে রিসেট করতে ড্রয়ার মেনু থেকে 'সলভার কোটা' অপশনটি ব্যবহার করুন।",
                            fontSize = 11.sp,
                            color = SomadhanTextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null,
                        tint = SomadhanOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "হোম মেনু নিয়ন্ত্রণ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "হোম স্ক্রিনের গ্রিড মেনু এবং বটম ন্যাভিগেশন বার আইটেম সক্রিয় বা নিষ্ক্রিয় করুন:",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                val menuToggles = listOf(
                    Triple("menu_all_problems_enabled", "সকল সমস্যা", "ইউজার হোম গ্রিডের 'সকল সমস্যা' অপশন"),
                    Triple("menu_my_problems_enabled", "আমার সমস্যা", "ইউজার হোম গ্রিডের 'আমার সমস্যা' অপশন"),
                    Triple("menu_bid_management_enabled", "বিড ম্যানেজমেন্ট", "ইউজার ও সলভার হোম গ্রিডের 'বিড ম্যানেজমেন্ট' অপশন"),
                    Triple("menu_favorite_solvers_enabled", "পছন্দের সমাধানকারী", "ইউজার হোম গ্রিডের 'পছন্দের সমাধানকারী' অপশন"),
                    Triple("menu_faq_enabled", "FAQ", "ইউজার ও সলভার হোম গ্রিডের 'FAQ' অপশন"),
                    Triple("menu_support_center_enabled", "সহায়তা কেন্দ্র", "ইউজার ও সলভার হোম গ্রিডের 'সহায়তা কেন্দ্র' অপশন"),
                    Triple("menu_wallet_enabled", "ওয়ালেট", "বটম ন্যাভিগেশন বারের 'ওয়ালেট' ট্যাব")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    menuToggles.forEach { (key, label, desc) ->
                        val isEnabled = platformSettings.find { it.key == key }?.value != "false"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SomadhanBg, RoundedCornerShape(8.dp))
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanTextPrimary
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.adminUpdatePlatformSetting(key, if (checked) "true" else "false")
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SomadhanOrange,
                                    uncheckedThumbColor = SomadhanTextSecondary,
                                    uncheckedTrackColor = SomadhanDivider
                                ),
                                modifier = Modifier.testTag("toggle_$key")
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User Home Notice Bar & Animation Control Card
        val noticeIntervalSetting = platformSettings.find { it.key == "user_home_notice_interval_sec" }?.value ?: "4"
        val rawNoticesStr = platformSettings.find { it.key == "user_home_notices" }?.value
        val currentNoticeList = remember(rawNoticesStr) {
            if (!rawNoticesStr.isNullOrBlank()) {
                rawNoticesStr.split("|||").map { it.trim() }.filter { it.isNotBlank() }
            } else {
                listOf(
                    "যেকোনো সেবা সহজে বুকিং করুন ও নিরাপদ সমাধান নিন।",
                    "যাচাইকৃত দক্ষ সলভারদের মাধ্যমে দ্রুত কাজ সম্পন্ন করুন।",
                    "নিরাপদ এসক্রো পেমেন্টে আপনার টাকা সর্বদা সুরক্ষিত।",
                    "যেকোনো সমস্যায় আমাদের ২৪/৭ সাপোর্ট সেন্টারে যোগাযোগ করুন।"
                )
            }
        }
        var newNoticeInput by remember { mutableStateOf("") }
        var noticeIntervalInput by remember(noticeIntervalSetting) { mutableStateOf(noticeIntervalSetting) }

        val currentIntervalInt = remember(noticeIntervalSetting) {
            noticeIntervalSetting.toIntOrNull()?.coerceIn(1, 60) ?: 4
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = SomadhanOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ইউজার হোম নোটিশ বার ও অ্যানিমেশন সেটিংস",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ইউজার হোমপেজে 'সেবাসমূহ' এর পাশে ঘূর্ণায়মান নোটিশ তালিকা এবং কতো সেকেন্ড পর পর এনিমেশন হবে তা পরিচালনা করুন:",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Live Preview Section
                Text(
                    text = "লাইভ প্রিভিউ (হোম স্ক্রিনে যেমন দেখাবে):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "সেবাসমূহ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (currentNoticeList.isNotEmpty()) {
                            UserHomeNoticeBar(
                                notices = currentNoticeList,
                                intervalSec = currentIntervalInt,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Animation Interval Settings
                Text(
                    text = "অ্যানিমেশন বিরতি সময় (কতো সেকেন্ড পর পর নোটিশ পরিবর্তন হবে):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = noticeIntervalInput,
                        onValueChange = { noticeIntervalInput = it.filter { ch -> ch.isDigit() } },
                        placeholder = { Text("যেমন: ৪", fontSize = 12.sp, color = SomadhanTextHint) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        trailingIcon = { Text("সেকেন্ড", fontSize = 12.sp, color = SomadhanTextSecondary, modifier = Modifier.padding(end = 10.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val intervalVal = noticeIntervalInput.toIntOrNull()
                            if (intervalVal != null && intervalVal >= 1) {
                                viewModel.adminUpdatePlatformSetting("user_home_notice_interval_sec", intervalVal.toString())
                                viewModel.showToast("অ্যানিমেশন বিরতি $intervalVal সেকেন্ড সেট করা হয়েছে।")
                            } else {
                                viewModel.showToast("সঠিক সংখ্যা প্রদান করুন (কমপক্ষে ১ সেকেন্ড)।")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("সেভ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Interval presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(2, 3, 4, 5, 7, 10).forEach { sec ->
                        val isSelected = noticeIntervalInput == sec.toString()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) SomadhanOrange else SomadhanBg)
                                .border(1.dp, if (isSelected) SomadhanOrange else SomadhanDivider, RoundedCornerShape(6.dp))
                                .clickable {
                                    noticeIntervalInput = sec.toString()
                                    viewModel.adminUpdatePlatformSetting("user_home_notice_interval_sec", sec.toString())
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${DistanceUtil.toBengaliDigits(sec.toString())} সে.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else SomadhanTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Active Notice List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সক্রিয় নোটিশ তালিকা (${DistanceUtil.toBengaliDigits(currentNoticeList.size.toString())}টি):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    currentNoticeList.forEachIndexed { index, notice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SomadhanBg, RoundedCornerShape(8.dp))
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(SomadhanOrange.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = DistanceUtil.toBengaliDigits((index + 1).toString()),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrange
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = notice,
                                    fontSize = 12.sp,
                                    color = SomadhanTextPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    val updated = currentNoticeList.filterIndexed { i, _ -> i != index }
                                    if (updated.isNotEmpty()) {
                                        viewModel.adminUpdatePlatformSetting("user_home_notices", updated.joinToString("|||"))
                                        viewModel.showToast("নোটিশ মুছে ফেলা হয়েছে।")
                                    } else {
                                        viewModel.showToast("কমপক্ষে একটি নোটিশ থাকা আবশ্যক।")
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "নোটিশ মুছুন",
                                    tint = SomadhanError,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Add New Notice
                Text(
                    text = "নতুন নোটিশ যোগ করুন:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newNoticeInput,
                        onValueChange = { newNoticeInput = it },
                        placeholder = { Text("যেমন: বিশেষ ছাড়ে সকল ইলেকট্রিশিয়ান সেবা নিন...", fontSize = 12.sp, color = SomadhanTextHint) },
                        modifier = Modifier.weight(1f),
                        singleLine = false,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmed = newNoticeInput.trim()
                            if (trimmed.isNotBlank()) {
                                if (currentNoticeList.contains(trimmed)) {
                                    viewModel.showToast("এই নোটিশটি ইতিমধ্যে তালিকায় বিদ্যমান।")
                                } else {
                                    val updated = currentNoticeList + trimmed
                                    viewModel.adminUpdatePlatformSetting("user_home_notices", updated.joinToString("|||"))
                                    newNoticeInput = ""
                                    viewModel.showToast("নতুন নোটিশ সফলভাবে যোগ করা হয়েছে।")
                                }
                            } else {
                                viewModel.showToast("নোটিশ বার্তা লিখুন।")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("যোগ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // d) App Version Control Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = SomadhanOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "অ্যাপ ভার্সন কন্ট্রোল",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "এই ভার্সনের নিচের অ্যাপে ব্যবহারকারীরা জোরপূর্বক আপডেট মেসেজ দেখবে।",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "সর্বনিম্ন সমর্থিত ভার্সন কোড (Min App Version)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = minAppVersionInput,
                        onValueChange = { minAppVersionInput = it },
                        placeholder = { Text("যেমন: 1", fontSize = 12.sp, color = SomadhanTextHint) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val v = minAppVersionInput.trim().toIntOrNull()
                            if (v != null && v >= 1) {
                                viewModel.adminUpdatePlatformSetting("min_app_version", minAppVersionInput.trim())
                            } else {
                                viewModel.showToast("সঠিক পূর্ণসংখ্যা প্রদান করুন (>= 1)।")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("সংরক্ষণ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // e) Physical Category Radius Range Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = SomadhanOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ফিজিক্যাল ক্যাটাগরি রেঞ্জ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ফিজিক্যাল/লোকাল সার্ভিসের জন্য সলভারের সাথে সমস্যার সর্বোচ্চ দূরত্ব (কিমি)।",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ম্যাচিং রেডিয়াস (কিলোমিটার)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = physicalRadiusInput,
                        onValueChange = { physicalRadiusInput = it },
                        placeholder = { Text("যেমন: 10", fontSize = 12.sp, color = SomadhanTextHint) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val r = physicalRadiusInput.trim().toDoubleOrNull()
                            if (r != null && r > 0.0) {
                                viewModel.adminUpdatePlatformSetting("physical_category_radius_km", physicalRadiusInput.trim())
                            } else {
                                viewModel.showToast("সঠিক সংখ্যা প্রদান করুন (> 0)।")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("সংরক্ষণ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Admin Session & Logout Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_session_logout_card"),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = SomadhanError,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "অ্যাডমিন সেশন ও লগআউট",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SomadhanTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "অ্যাডমিন প্যানেল থেকে বের হতে এবং সেশন সমাপ্ত করতে নিচের বোতামে চাপুন।",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showLogoutConfirmationDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SomadhanError,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_settings_logout_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "লগআউট",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "অ্যাডমিন প্যানেল থেকে লগআউট করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Danger Zone Card — Factory Reset (Admin Action bug-fix master prompt, ধাপ ৯)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_danger_zone_card"),
            colors = CardDefaults.cardColors(containerColor = SomadhanErrorLight.copy(alpha = 0.15f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanError.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = SomadhanError,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚠️ Danger Zone",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SomadhanError
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "নিচের অপারেশন লোকাল ও cloud-এর সমস্ত ডেটা স্থায়ীভাবে মুছে অ্যাপকে ফ্যাক্টরি-ফ্রেশ অবস্থায় ফিরিয়ে দেয়। এটা ফেরানো সম্ভব না -- শুধু চরম প্রয়োজনে ব্যবহার করুন।",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        factoryResetConfirmText = ""
                        factoryResetError = null
                        showFactoryResetDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SomadhanError,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_settings_factory_reset_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "সমস্ত ডেটা ফ্যাক্টরি রিসেট করো",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
