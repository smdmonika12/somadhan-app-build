package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.util.Formatters
import com.example.util.ImageStorageUtil
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.RoleBadge
import com.example.ui.components.UserVerificationBadge
import com.example.ui.components.YellowVerifiedBadge
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.SyncAwareContent
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.LocationFetchResult
import com.example.util.LocationHelper
import kotlinx.coroutines.launch
import com.example.ui.components.BottomSlideAlertDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    // Loading/Sync Fix Roadmap v2, ধাপ ৪ (Group B) — currentUser bulk-pull-এর অংশ, তাই
    // initialSyncPhase প্রাসঙ্গিক।
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    val isUser = currentUser?.role != "SOLVER"
    val brandColor = if (isUser) Color(0xFF1D4ED8) else SomadhanOrange
    val brandLight = if (isUser) Color(0xFF1D4ED8).copy(alpha = 0.12f) else SomadhanOrangeLight
    val brandBorder = if (isUser) Color(0xFF1D4ED8).copy(alpha = 0.35f) else SomadhanOrange.copy(alpha = 0.4f)

    var name by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var email by remember(currentUser) { mutableStateOf(currentUser?.email ?: "") }
    var address by remember(currentUser) { mutableStateOf(currentUser?.address ?: "") }
    var currentLat by remember(currentUser) { mutableStateOf(currentUser?.latitude) }
    var currentLon by remember(currentUser) { mutableStateOf(currentUser?.longitude) }
    var isLocating by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var selectedPhotoUri by remember(currentUser) { mutableStateOf(currentUser?.profileImageUri ?: "") }
    var showChangePhoneFlow by remember { mutableStateOf(false) }
    var phoneChangeStep by remember { mutableStateOf(1) }
    var changePhoneOtpInput by remember { mutableStateOf("") }
    var newPhoneNumberInput by remember { mutableStateOf("") }
    var isSendingChangeOtp by remember { mutableStateOf(false) }
    var isUpdatingPhone by remember { mutableStateOf(false) }
    var phoneChangeError by remember { mutableStateOf<String?>(null) }

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
                        address = result.location.address
                        currentLat = result.location.latitude
                        currentLon = result.location.longitude
                    }
                    is LocationFetchResult.PermissionDenied -> {
                        viewModel.showToast("লোকেশন পারমিশন পাওয়া যায়নি")
                    }
                    is LocationFetchResult.Error -> {
                        viewModel.showToast(result.message)
                    }
                }
                isLocating = false
            }
        } else {
            viewModel.showToast("লোকেশন পারমিশন দেওয়া হয়নি")
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val userId = currentUser?.id ?: "user_${System.currentTimeMillis()}"
            isUploadingPhoto = true
            scope.launch {
                val result = ImageStorageUtil.uploadProfilePhoto(context, userId, uri)
                isUploadingPhoto = false
                result.onSuccess { downloadUrl ->
                    selectedPhotoUri = downloadUrl
                    if (currentUser != null) {
                        viewModel.updateProfileImage(downloadUrl)
                    }
                }.onFailure { error ->
                    viewModel.showToast("ছবি আপলোড ব্যর্থ হয়েছে, আবার চেষ্টা করুন")
                }
            }
        }
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
                            text = "ব্যবহারকারীর তথ্য সম্পাদনা",
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
                    isSolver = !isUser,
                    onRefresh = { viewModel.refreshLiveLocation() }
                )
            }
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        SyncAwareContent(
            sessionKey = "user_info_sync",
            viewModel = viewModel,
            syncPhase = initialSyncPhase,
            onRetry = { viewModel.retryInitialSync() },
            modifier = Modifier.padding(paddingValues)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SomadhanBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header card with photo on LEFT and User Details on RIGHT
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SomadhanDivider, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Profile Photo with camera picker badge
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(brandLight)
                                .border(2.dp, brandColor, CircleShape)
                                .clickable(enabled = !isUploadingPhoto) { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            val isPhotoValid = ImageStorageUtil.isValidDisplayUri(selectedPhotoUri)
                            if (isUploadingPhoto) {
                                CircularProgressIndicator(
                                    color = brandColor,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            } else if (isPhotoValid) {
                                var loadFailed by remember(selectedPhotoUri) { mutableStateOf(false) }
                                if (!loadFailed) {
                                    AsyncImage(
                                        model = selectedPhotoUri,
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
                                        tint = brandColor,
                                        modifier = Modifier.size(46.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = brandColor,
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }

                        // Camera icon badge
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(brandColor)
                                .border(1.5.dp, Color.White, CircleShape)
                                .clickable(enabled = !isUploadingPhoto) { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "ছবি বদলান",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right Side: Info & Photo upload button
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = name.ifBlank { "ব্যবহারকারী" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            UserVerificationBadge(
                                role = currentUser?.role ?: "USER",
                                isKycVerified = currentUser?.isKycVerified == true
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        RoleBadge(role = currentUser?.role ?: "USER")

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            enabled = !isUploadingPhoto,
                            colors = ButtonDefaults.buttonColors(containerColor = brandLight),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            if (isUploadingPhoto) {
                                CircularProgressIndicator(
                                    color = brandColor,
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("আপলোড হচ্ছে...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = brandColor)
                            } else {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = brandColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ছবি আপলোড করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = brandColor)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SomadhanDivider, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ফোন নম্বর (অপরিবর্তনযোগ্য)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextHint)
                        TextButton(
                            onClick = { showChangePhoneFlow = true },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp).testTag("change_phone_number_btn")
                        ) {
                            Text("পরিবর্তন করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = brandColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = currentUser?.phone?.let { Formatters.toLocalDisplayFormat(it) } ?: "",
                        onValueChange = {},
                        enabled = false,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SomadhanTextHint) },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = SomadhanDivider,
                            disabledContainerColor = SomadhanCardBg,
                            disabledTextColor = SomadhanTextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("user_phone_display_field")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("পূর্ণ নাম *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = brandColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandColor,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("ইমেইল অ্যাড্রেস *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = brandColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandColor,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_email_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ঠিকানা (GPS অটো-ডিটেক্ট) *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
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
                                                address = result.location.address
                                                currentLat = result.location.latitude
                                                currentLon = result.location.longitude
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
                                                viewModel.showToast(result.message)
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
                            colors = ButtonDefaults.buttonColors(containerColor = brandLight),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("auto_detect_location_btn")
                        ) {
                            if (isLocating) {
                                CircularProgressIndicator(color = brandColor, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = brandColor, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("অটো-ডিটেক্ট", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = brandColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = address.ifBlank { "লোকেশন পাওয়া যায়নি (বাটনে ক্লিক করুন)" },
                        onValueChange = {},
                        enabled = false,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (address.isNotBlank()) brandColor else SomadhanTextHint) },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = SomadhanDivider,
                            disabledContainerColor = SomadhanCardBg,
                            disabledTextColor = if (address.isNotBlank()) SomadhanTextPrimary else SomadhanTextHint
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_address_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                isLocating = true
                                scope.launch {
                                    when (val result = LocationHelper.getCurrentLocation(context)) {
                                        is LocationFetchResult.Success -> {
                                            address = result.location.address
                                            currentLat = result.location.latitude
                                            currentLon = result.location.longitude
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
                                            viewModel.showToast(result.message)
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
                        colors = ButtonDefaults.buttonColors(containerColor = brandLight),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("detect_current_location_full_btn")
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(color = brandColor, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("লোকেশন খোঁজা হচ্ছে...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = brandColor)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = brandColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("বর্তমান লোকেশন অটো-ডিটেক্ট করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = brandColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (name.isNotBlank() && email.isNotBlank() && address.isNotBlank()) {
                                viewModel.updateProfile(
                                    name = name,
                                    email = email,
                                    address = address,
                                    lat = currentLat,
                                    lon = currentLon,
                                    profileImage = selectedPhotoUri.ifBlank { null },
                                    onSuccess = { onNavigateBack() }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_user_info_btn")
                    ) {
                        Text("তথ্য সংরক্ষণ করুন", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        }

        if (showChangePhoneFlow) {
            BottomSlideAlertDialog(
                onDismissRequest = {
                    if (!isSendingChangeOtp && !isUpdatingPhone) {
                        showChangePhoneFlow = false
                        phoneChangeStep = 1
                        changePhoneOtpInput = ""
                        newPhoneNumberInput = ""
                        phoneChangeError = null
                        viewModel.clearOtp(currentUser?.phone)
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (phoneChangeStep == 1) "ফোন নম্বর পরিবর্তন (ধাপ ১/২)" else "OTP যাচাই ও নতুন নম্বর (ধাপ ২/২)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SomadhanTextPrimary
                        )
                        IconButton(
                            enabled = !isSendingChangeOtp && !isUpdatingPhone,
                            onClick = {
                                showChangePhoneFlow = false
                                phoneChangeStep = 1
                                changePhoneOtpInput = ""
                                newPhoneNumberInput = ""
                                phoneChangeError = null
                                viewModel.clearOtp(currentUser?.phone)
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = SomadhanTextSecondary)
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (phoneChangeStep == 1) {
                            Text(
                                text = "নিরাপত্তার জন্য, আপনার বর্তমান নম্বর ${currentUser?.phone?.let { Formatters.toLocalDisplayFormat(it) } ?: ""}-এ একটা OTP পাঠানো হবে",
                                fontSize = 13.sp,
                                color = SomadhanTextSecondary,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "OTP যাচাইয়ের পর আপনি নতুন ফোন নম্বর সেট করতে পারবেন।",
                                fontSize = 12.sp,
                                color = SomadhanTextHint
                            )
                        } else {
                            Text(
                                text = "বর্তমান নম্বরে (${currentUser?.phone?.let { Formatters.toLocalDisplayFormat(it) } ?: ""}) পাঠানো ৬-ডিজিটের OTP কোড এবং আপনার নতুন ফোন নম্বর প্রবেশ করান:",
                                fontSize = 13.sp,
                                color = SomadhanTextSecondary,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("৬-ডিজিট OTP কোড", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextHint)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = changePhoneOtpInput,
                                onValueChange = {
                                    if (it.length <= 6) changePhoneOtpInput = it
                                },
                                placeholder = { Text("৬-ডিজিট OTP (যেমন: 123456)", color = SomadhanTextHint, fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = brandColor, modifier = Modifier.size(18.dp))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = brandColor,
                                    unfocusedBorderColor = SomadhanBorder,
                                    focusedContainerColor = SomadhanBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("change_phone_otp_input")
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("টেস্টিং কোড: 123456", fontSize = 11.sp, color = SomadhanTextHint)
                                Text(
                                    text = "অটো-ফিল (123456)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = brandColor,
                                    modifier = Modifier
                                        .clickable { changePhoneOtpInput = "123456" }
                                        .testTag("change_phone_otp_autofill")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("নতুন ফোন নম্বর", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextHint)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = newPhoneNumberInput,
                                onValueChange = { newPhoneNumberInput = it },
                                placeholder = { Text("নতুন নম্বর (যেমন: 017XXXXXXXX)", color = SomadhanTextHint, fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = brandColor, modifier = Modifier.size(18.dp))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = brandColor,
                                    unfocusedBorderColor = SomadhanBorder,
                                    focusedContainerColor = SomadhanBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("new_phone_number_input")
                            )
                        }
                        if (phoneChangeError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = phoneChangeError ?: "",
                                fontSize = 12.sp,
                                color = SomadhanError,
                                modifier = Modifier.testTag("phone_change_error")
                            )
                        }
                    }
                },
                confirmButton = {
                    if (phoneChangeStep == 1) {
                        Button(
                            onClick = {
                                val currentPhone = currentUser?.phone ?: ""
                                if (currentPhone.isBlank()) {
                                    viewModel.showToast("বর্তমান ফোন নম্বর পাওয়া যায়নি।")
                                    return@Button
                                }
                                isSendingChangeOtp = true
                                phoneChangeError = null
                                viewModel.sendOtp(
                                    target = currentPhone,
                                    purpose = "phone_change",
                                    onSuccess = {
                                        isSendingChangeOtp = false
                                        phoneChangeStep = 2
                                    },
                                    onError = { err ->
                                        isSendingChangeOtp = false
                                        phoneChangeError = err
                                    }
                                )
                            },
                            enabled = !isSendingChangeOtp,
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("send_phone_change_otp_btn")
                        ) {
                            if (isSendingChangeOtp) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("পাঠানো হচ্ছে...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("OTP পাঠান", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                val currentPhone = currentUser?.phone ?: ""
                                val newPhone = newPhoneNumberInput.trim()
                                val otp = changePhoneOtpInput.trim()

                                if (otp.length < 6) {
                                    viewModel.showToast("৬ ডিজিটের সঠিক OTP প্রদান করুন।")
                                    return@Button
                                }
                                if (newPhone.isBlank() || newPhone.length < 11) {
                                    viewModel.showToast("সঠিক ১১ ডিজিটের নতুন মোবাইল নম্বর দিন।")
                                    return@Button
                                }
                                if (newPhone == currentPhone) {
                                    viewModel.showToast("নতুন নম্বর বর্তমান নম্বরের চেয়ে ভিন্ন হতে হবে।")
                                    return@Button
                                }

                                isUpdatingPhone = true
                                phoneChangeError = null
                                viewModel.verifyOtp(
                                    target = currentPhone,
                                    enteredOtp = otp,
                                    onSuccess = {
                                        viewModel.updatePhoneNumber(
                                            newPhone = newPhone,
                                            onSuccess = {
                                                isUpdatingPhone = false
                                                showChangePhoneFlow = false
                                                phoneChangeStep = 1
                                                changePhoneOtpInput = ""
                                                newPhoneNumberInput = ""
                                                phoneChangeError = null
                                            },
                                            onError = { err ->
                                                isUpdatingPhone = false
                                                phoneChangeError = err
                                            }
                                        )
                                    },
                                    onError = { err ->
                                        isUpdatingPhone = false
                                        phoneChangeError = err
                                    }
                                )
                            },
                            enabled = !isUpdatingPhone,
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("confirm_change_phone_btn")
                        ) {
                            if (isUpdatingPhone) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("নিশ্চিত হচ্ছে...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("নিশ্চিত করুন", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isSendingChangeOtp && !isUpdatingPhone,
                        onClick = {
                            showChangePhoneFlow = false
                            phoneChangeStep = 1
                            changePhoneOtpInput = ""
                            newPhoneNumberInput = ""
                            phoneChangeError = null
                            viewModel.clearOtp(currentUser?.phone)
                        }
                    ) {
                        Text("বাতিল", color = SomadhanTextSecondary, fontSize = 13.sp)
                    }
                },
                containerColor = SomadhanCardBg,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}
