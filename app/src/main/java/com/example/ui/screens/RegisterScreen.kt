package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CategoryEntity
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanOrangePressed
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil
import com.example.util.LocationFetchResult
import com.example.util.LocationHelper
import com.example.util.LocationResult
import kotlinx.coroutines.launch
import com.example.ui.components.BottomSlideAlertDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(
    viewModel: SomadhanViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val visibleCategories by viewModel.visibleCategories.collectAsStateWithLifecycle()

    var role by remember { mutableStateOf("USER") } // "USER" or "SOLVER"
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var selectedLocation by remember { mutableStateOf<LocationResult?>(null) }
    var customAddress by remember { mutableStateOf("") }
    var isLocating by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showLocationPickerDropdown by remember { mutableStateOf(false) }

    var selectedCategoryIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isVerifyingOtp by remember { mutableStateOf(false) }

    // Live OTP Verification State with 5-minute expiry & rate limit (Section 23.1)
    var showOtpDialog by remember { mutableStateOf(false) }
    var enteredOtp by remember { mutableStateOf("") }
    var otpTimerSeconds by remember { mutableIntStateOf(300) } // 5 minutes expiry (300 seconds)
    var isOtpTimerRunning by remember { mutableStateOf(false) }
    var otpError by remember { mutableStateOf<String?>(null) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var otpTargetType by remember { mutableStateOf("PHONE") } // "PHONE" or "EMAIL"

    LaunchedEffect(isOtpTimerRunning, otpTimerSeconds) {
        if (isOtpTimerRunning && otpTimerSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            otpTimerSeconds -= 1
        } else if (otpTimerSeconds == 0) {
            isOtpTimerRunning = false
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
                        customAddress = result.location.address
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

    LaunchedEffect(Unit) {
        // Check permission initial state - only auto-fetch if permission is already granted
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            isLocating = true
            when (val result = LocationHelper.getCurrentLocation(context)) {
                is LocationFetchResult.Success -> {
                    selectedLocation = result.location
                    customAddress = result.location.address
                }
                else -> {}
            }
            isLocating = false
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

    if (showOtpDialog) {
        val targetDestination = if (otpTargetType == "PHONE") phone.trim() else email.trim()
        BottomSlideAlertDialog(
            onDismissRequest = {
                showOtpDialog = false
                viewModel.clearOtp(phone.trim())
            },
            title = {
                Text(
                    text = if (otpTargetType == "PHONE") "মোবাইল নম্বর যাচাই (OTP)" else "ইমেইল ঠিকানা যাচাই (OTP)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "$targetDestination ঠিকানায় পাঠানো ৬ ডিজিটের ওটিপি যাচাই কোডটি প্রবেশ করান। কোডটির মেয়াদ ৫ মিনিট।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = {
                            if (it.length <= 6) {
                                enteredOtp = it
                                otpError = null
                            }
                        },
                        label = { Text("৬ ডিজিটের OTP") },
                        placeholder = { Text("যেমন: 123456", color = SomadhanTextHint) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("register_otp_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "টেস্টিং কোড: 123456",
                            fontSize = 11.sp,
                            color = SomadhanTextHint
                        )
                        Text(
                            text = "অটো-ফিল (123456)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanOrange,
                            modifier = Modifier
                                .clickable {
                                    enteredOtp = "123456"
                                    otpError = null
                                }
                                .testTag("register_otp_autofill")
                        )
                    }

                    if (otpError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(otpError ?: "", color = SomadhanError, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val minutesLeft = otpTimerSeconds / 60
                        val secondsLeft = otpTimerSeconds % 60
                        val timeStr = "${DistanceUtil.toBengaliDigits(minutesLeft.toString())}:${if (secondsLeft < 10) "০" else ""}${DistanceUtil.toBengaliDigits(secondsLeft.toString())}"
                        Text(
                            text = if (isOtpTimerRunning) "মেয়াদ বাকি: $timeStr" else "মেয়াদ শেষ",
                            fontSize = 12.sp,
                            color = if (isOtpTimerRunning) SomadhanTextSecondary else SomadhanError
                        )

                        TextButton(
                            onClick = {
                                isSendingOtp = true
                                otpError = null
                                viewModel.sendOtp(
                                    target = targetDestination,
                                    purpose = "registration",
                                    onSuccess = {
                                        isSendingOtp = false
                                        otpTimerSeconds = 300
                                        isOtpTimerRunning = true
                                    },
                                    onError = { err ->
                                        isSendingOtp = false
                                        otpError = err
                                    }
                                )
                            },
                            enabled = !isOtpTimerRunning && !isSendingOtp
                        ) {
                            if (isSendingOtp) {
                                CircularProgressIndicator(color = SomadhanOrange, modifier = Modifier.size(12.dp))
                            } else {
                                Text(
                                    "কোড পুনরায় পাঠান",
                                    color = if (!isOtpTimerRunning) SomadhanOrange else SomadhanTextHint,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isVerifyingOtp) return@Button
                        if (enteredOtp.trim().length < 6) {
                            otpError = "৬ ডিজিটের ওটিপি প্রদান করুন।"
                            return@Button
                        }

                        isVerifyingOtp = true
                        viewModel.verifyOtp(
                            target = targetDestination,
                            enteredOtp = enteredOtp.trim(),
                            onSuccess = {
                                showOtpDialog = false
                                isVerifyingOtp = false
                                isLoading = true
                                viewModel.register(
                                    name = name,
                                    phone = phone,
                                    email = email,
                                    pass = password,
                                    role = role,
                                    lat = selectedLocation?.latitude ?: 0.0,
                                    lon = selectedLocation?.longitude ?: 0.0,
                                    address = customAddress,
                                    solverCategories = selectedCategoryIds.toList(),
                                    onSuccess = {
                                        isLoading = false
                                        onRegisterSuccess()
                                    },
                                    onError = {
                                        isLoading = false
                                        errorMessage = it
                                    }
                                )
                            },
                            onError = { err ->
                                isVerifyingOtp = false
                                otpError = err
                            }
                        )
                    },
                    enabled = !isVerifyingOtp,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    modifier = Modifier.testTag("register_otp_confirm_button")
                ) {
                    if (isVerifyingOtp) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("যাচাই হচ্ছে...")
                    } else {
                        Text("যাচাই ও সম্পন্ন করুন")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isVerifyingOtp,
                    onClick = {
                        showOtpDialog = false
                        viewModel.clearOtp(targetDestination)
                    }
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(SomadhanBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "নতুন অ্যাকাউন্ট রেজিস্ট্রেশন",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "সঠিক তথ্য প্রদান করে অ্যাকাউন্ট তৈরি করুন",
                fontSize = 13.sp,
                color = SomadhanTextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Role Toggle (ইউজার vs সমাধানকারী)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SomadhanCardBg)
                    .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (role == "USER") SomadhanOrange else Color.Transparent)
                        .clickable { role = "USER" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ইউজার (সমস্যা পোস্টকারী)",
                        fontSize = 13.sp,
                        fontWeight = if (role == "USER") FontWeight.Bold else FontWeight.Medium,
                        color = if (role == "USER") Color.White else SomadhanTextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (role == "SOLVER") SomadhanOrange else Color.Transparent)
                        .clickable { role = "SOLVER" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "সলভার (সমাধানকারী)",
                        fontSize = 13.sp,
                        fontWeight = if (role == "SOLVER") FontWeight.Bold else FontWeight.Medium,
                        color = if (role == "SOLVER") Color.White else SomadhanTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SomadhanDivider, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Name
                    Text("পূর্ণ নাম *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMessage = null },
                        placeholder = { Text("যেমন: শামীম হোসেন", color = SomadhanTextHint, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SomadhanTextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("reg_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone
                    Text("ফোন নম্বর *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; errorMessage = null },
                        placeholder = { Text("যেমন: 01712345678", color = SomadhanTextHint, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SomadhanTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("reg_phone_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email
                    Text("ইমেইল অ্যাড্রেস *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        placeholder = { Text("যেমন: shamim@example.com", color = SomadhanTextHint, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SomadhanTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("reg_email_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password
                    Text("পাসওয়ার্ড *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        placeholder = { Text("গোপন পাসওয়ার্ড তৈরি করুন", color = SomadhanTextHint, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SomadhanTextSecondary) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = SomadhanTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("reg_password_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Location section with GPS Auto-detect
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("লোকেশন (ম্যাপ/GPS) *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SomadhanOrangeLight)
                                .clickable {
                                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (hasFine || hasCoarse) {
                                        isLocating = true
                                        scope.launch {
                                            when (val result = LocationHelper.getCurrentLocation(context)) {
                                                is LocationFetchResult.Success -> {
                                                    selectedLocation = result.location
                                                    customAddress = result.location.address
                                                    errorMessage = null
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
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (isLocating) {
                                CircularProgressIndicator(color = SomadhanOrange, modifier = Modifier.size(12.dp))
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = SomadhanOrange, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("অটো-ডিটেক্ট", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SomadhanOrange)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = customAddress,
                        onValueChange = { customAddress = it; errorMessage = null },
                        placeholder = { Text("এলাকার নাম লিখুন বা লিস্ট থেকে বেছে নিন", color = SomadhanTextHint, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = SomadhanOrange) },
                        trailingIcon = {
                            IconButton(onClick = { showLocationPickerDropdown = !showLocationPickerDropdown }) {
                                Icon(Icons.Default.MyLocation, contentDescription = "বাছাই করুন", tint = SomadhanTextSecondary)
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("reg_location_input")
                    )

                    // Location Preset Selector Dropdown
                    AnimatedVisibility(visible = showLocationPickerDropdown) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .border(1.dp, SomadhanDivider, RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("প্রধান এলাকাগুলো থেকে বেছে নিন:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SomadhanTextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                LocationHelper.BD_DEFAULT_LOCATIONS.take(6).forEach { loc ->
                                    Text(
                                        text = loc.address,
                                        fontSize = 12.sp,
                                        color = SomadhanTextPrimary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedLocation = loc
                                                customAddress = loc.address
                                                showLocationPickerDropdown = false
                                            }
                                            .padding(vertical = 6.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // SOLVER EXTRA: Category Selection (Min 1, Max 3)
                    AnimatedVisibility(visible = role == "SOLVER") {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ক্যাটাগরি বাছাই করুন *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanTextPrimary
                                )
                                Text(
                                    text = "${DistanceUtil.toBengaliDigits(selectedCategoryIds.size.toString())} / ৩ টি সিলেক্টেড",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedCategoryIds.size in 1..3) SomadhanSuccess else SomadhanOrange
                                )
                            }

                            Text(
                                text = "সর্বনিম্ন ১টি এবং সর্বোচ্চ ৩টি ক্যাটাগরি বেছে নিন",
                                fontSize = 11.sp,
                                color = SomadhanTextHint
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Selected Chips display (Mandatory requirement!)
                            if (selectedCategoryIds.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    selectedCategoryIds.forEach { id ->
                                        val cat = allCategories.find { it.id == id }
                                        if (cat != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(SomadhanOrange)
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = cat.nameBangla,
                                                    fontSize = 11.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "রিমুভ",
                                                    tint = Color.White,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            selectedCategoryIds = selectedCategoryIds - id
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Category Select List (Click-to-select chips/cards)
                            if (visibleCategories.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "বর্তমানে কোনো ক্যাটাগরি সেবা সক্রিয় নেই।",
                                        fontSize = 12.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    visibleCategories.forEach { cat ->
                                        val isSelected = selectedCategoryIds.contains(cat.id)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) SomadhanOrangeLight else SomadhanCardBg)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) SomadhanOrange else SomadhanDivider,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    if (isSelected) {
                                                        selectedCategoryIds = selectedCategoryIds - cat.id
                                                        errorMessage = null
                                                    } else {
                                                        if (selectedCategoryIds.size < 3) {
                                                            selectedCategoryIds = selectedCategoryIds + cat.id
                                                            errorMessage = null
                                                        } else {
                                                            errorMessage = "সর্বোচ্চ ৩টি ক্যাটাগরি বাছাই করা যাবে।"
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                                contentDescription = null,
                                                tint = if (isSelected) SomadhanOrange else SomadhanTextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = cat.nameBangla,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = SomadhanTextPrimary
                                                )
                                                Text(
                                                    text = if (cat.isPhysical) "ফিজিক্যাল" else "ভার্চুয়াল",
                                                    fontSize = 10.sp,
                                                    color = SomadhanTextHint
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = SomadhanOrange,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (name.isBlank() || phone.isBlank() || email.isBlank() || password.isBlank() || customAddress.isBlank()) {
                                errorMessage = "অনুগ্রহ করে সকল তথ্য পূরণ করুন।"
                                return@Button
                            }
                            if (role == "SOLVER" && (selectedCategoryIds.isEmpty() || selectedCategoryIds.size > 3)) {
                                errorMessage = "সমাধানকারী হিসেবে ১ থেকে ৩টি ক্যাটাগরি বাছাই করুন।"
                                return@Button
                            }

                            // Request Real OTP via SMS / Email with 5-minute expiry & rate limit
                            val target = phone.trim()
                            otpTargetType = "PHONE"
                            enteredOtp = ""
                            otpError = null
                            isLoading = true

                            viewModel.sendOtp(
                                target = target,
                                purpose = "registration",
                                onSuccess = {
                                    isLoading = false
                                    otpTimerSeconds = 300
                                    isOtpTimerRunning = true
                                    showOtpDialog = true
                                },
                                onError = { err ->
                                    isLoading = false
                                    errorMessage = err
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("register_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text(
                                text = "রেজিস্ট্রেশন সম্পন্ন করুন",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("ইতিমধ্যে অ্যাকাউন্ট আছে? ", fontSize = 13.sp, color = SomadhanTextSecondary)
                Text(
                    text = "লগইন করুন",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanOrange,
                    modifier = Modifier.clickable { onNavigateToLogin() }.testTag("login_link")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
