package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.UserEntity
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanInfoLight
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.components.BottomSlideAlertDialog

@Composable
fun LoginScreen(
    viewModel: SomadhanViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    onAdminLoginSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isAdminPhoneDetected by remember { mutableStateOf(false) }

    // Check if the entered phone matches configured Admin phone
    LaunchedEffect(phone) {
        val trimmed = phone.trim()
        if (trimmed.length >= 8) {
            delay(200L)
            // [ADMIN_ROLE_PROFILE সেশন ২] একটাই বিদ্যমান admin phone-এর বদলে সার্ভারের এডমিন-অ্যাকাউন্ট তালিকা
            // (anon `admin_login_check` RPC) — যেকোনো সক্রিয়/নিষ্ক্রিয় এডমিনের ফোনে true।
            isAdminPhoneDetected = viewModel.isAdminPhone(trimmed)
        } else {
            isAdminPhoneDetected = false
        }
    }

    // Dual-Role Dialog State
    var showRoleChoiceDialog by remember { mutableStateOf(false) }

    // Login 2FA OTP Dialog State
    var showLoginOtpDialog by remember { mutableStateOf(false) }
    var pendingLoginUser by remember { mutableStateOf<UserEntity?>(null) }
    var loginOtpInput by remember { mutableStateOf("") }
    var loginOtpError by remember { mutableStateOf<String?>(null) }
    var isVerifyingLoginOtp by remember { mutableStateOf(false) }
    var loginOtpTimerSeconds by remember { mutableIntStateOf(300) } // 5 minutes (300s)
    var isLoginOtpTimerRunning by remember { mutableStateOf(false) }
    var isSendingLoginOtp by remember { mutableStateOf(false) }

    // Timer effect for Login OTP (5 minutes countdown)
    LaunchedEffect(isLoginOtpTimerRunning, loginOtpTimerSeconds) {
        if (isLoginOtpTimerRunning && loginOtpTimerSeconds > 0) {
            delay(1000L)
            loginOtpTimerSeconds -= 1
        } else if (loginOtpTimerSeconds == 0) {
            isLoginOtpTimerRunning = false
        }
    }

    // Forgot Password Flow State
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotStep by remember { mutableIntStateOf(1) } // 1: Input Phone/Email, 2: OTP, 3: New Password
    var forgotIdentifier by remember { mutableStateOf("") }
    var forgotOtpInput by remember { mutableStateOf("") }
    var forgotNewPassword by remember { mutableStateOf("") }
    var forgotConfirmPassword by remember { mutableStateOf("") }
    var forgotError by remember { mutableStateOf<String?>(null) }
    var timerSeconds by remember { mutableIntStateOf(300) } // 5 minutes (300s)
    var isTimerRunning by remember { mutableStateOf(false) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifyingForgotOtp by remember { mutableStateOf(false) }
    var isResettingPassword by remember { mutableStateOf(false) }
    var isSelectingLoginRole by remember { mutableStateOf(false) }

    // Timer effect for OTP (5 minutes countdown)
    LaunchedEffect(isTimerRunning, timerSeconds) {
        if (isTimerRunning && timerSeconds > 0) {
            delay(1000L)
            timerSeconds -= 1
        } else if (timerSeconds == 0) {
            isTimerRunning = false
        }
    }

    // Dual Role Selection Dialog
    if (showRoleChoiceDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = "ভূমিকা নির্বাচন করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SomadhanTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "আপনার অ্যাকাউন্টে উভয় রোলের সুবিধা রয়েছে। আপনি এখন কোন ভূমিকায় প্রবেশ করতে চান?",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 1: User
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isSelectingLoginRole) {
                                isSelectingLoginRole = true
                                // [Offline Action Gating ধাপ ১১] অফলাইনে গার্ড ব্লক করলে spinner/dialog যেন আটকে না থাকে।
                                viewModel.switchRoleToUser(onError = { isSelectingLoginRole = false }) {
                                    isSelectingLoginRole = false
                                    showRoleChoiceDialog = false
                                    onLoginSuccess()
                                }
                            }
                            .testTag("login_choose_user_role")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SomadhanOrangeLight),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelectingLoginRole) {
                                    CircularProgressIndicator(color = SomadhanOrange, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = SomadhanOrange)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("ইউজার (User)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SomadhanTextPrimary)
                                Text("সমস্যা পোস্ট ও সমাধান নিতে", fontSize = 11.sp, color = SomadhanTextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 2: Solver
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isSelectingLoginRole) {
                                isSelectingLoginRole = true
                                viewModel.switchRoleToSolver(
                                    newCategories = emptyList(),
                                    onSuccess = {
                                        isSelectingLoginRole = false
                                        showRoleChoiceDialog = false
                                        onLoginSuccess()
                                    },
                                    onError = {
                                        isSelectingLoginRole = false
                                        showRoleChoiceDialog = false
                                        onLoginSuccess()
                                    }
                                )
                            }
                            .testTag("login_choose_solver_role")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SomadhanSuccessLight),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelectingLoginRole) {
                                    CircularProgressIndicator(color = SomadhanSuccess, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Engineering, contentDescription = null, tint = SomadhanSuccess)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("সমাধানকারী (Solver)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SomadhanTextPrimary)
                                Text("সমস্যার সমাধান দিয়ে আয় করতে", fontSize = 11.sp, color = SomadhanTextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Login 2FA Verification Dialog
    if (showLoginOtpDialog && pendingLoginUser != null) {
        val userToLogin = pendingLoginUser!!
        BottomSlideAlertDialog(
            onDismissRequest = {
                if (!isVerifyingLoginOtp) {
                    showLoginOtpDialog = false
                    loginOtpInput = ""
                    loginOtpError = null
                    viewModel.clearOtp(userToLogin.phone)
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "লগইন OTP যাচাইকরণ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SomadhanTextPrimary
                    )
                    IconButton(onClick = {
                        showLoginOtpDialog = false
                        loginOtpInput = ""
                        loginOtpError = null
                        viewModel.clearOtp(userToLogin.phone)
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = SomadhanTextSecondary)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "নিরাপত্তার স্বার্থে আপনার নিবন্ধিত মোবাইল নম্বরে (${Formatters.toLocalDisplayFormat(userToLogin.phone)}) একটি ৬-ডিজিটের গোপন OTP যাচাইকরণ কোড পাঠানো হয়েছে।",
                        fontSize = 13.sp,
                        color = SomadhanTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = loginOtpInput,
                        onValueChange = {
                            if (it.length <= 6) {
                                loginOtpInput = it
                                loginOtpError = null
                            }
                        },
                        placeholder = { Text("৬-ডিজিট ওটিপি কোড (যেমন: 123456)", color = SomadhanTextHint, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("login_otp_input")
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
                                    loginOtpInput = "123456"
                                    loginOtpError = null
                                }
                                .testTag("login_otp_autofill")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val minutesLeft = loginOtpTimerSeconds / 60
                        val secondsLeft = loginOtpTimerSeconds % 60
                        val timeStr = "${DistanceUtil.toBengaliDigits(minutesLeft.toString())}:${if (secondsLeft < 10) "০" else ""}${DistanceUtil.toBengaliDigits(secondsLeft.toString())}"
                        Text(
                            text = if (loginOtpTimerSeconds > 0) "মেয়াদ বাকি: $timeStr" else "ওটিপির মেয়াদ শেষ",
                            fontSize = 12.sp,
                            color = if (loginOtpTimerSeconds > 0) SomadhanTextSecondary else SomadhanError
                        )
                        if (loginOtpTimerSeconds == 0) {
                            Text(
                                text = if (isSendingLoginOtp) "পাঠানো হচ্ছে..." else "পুনরায় পাঠান",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrange,
                                modifier = Modifier.clickable(enabled = !isSendingLoginOtp) {
                                    isSendingLoginOtp = true
                                    loginOtpError = null
                                    viewModel.sendOtp(
                                        target = userToLogin.phone,
                                        purpose = "login",
                                        onSuccess = {
                                            isSendingLoginOtp = false
                                            loginOtpTimerSeconds = 300
                                            isLoginOtpTimerRunning = true
                                        },
                                        onError = { err ->
                                            isSendingLoginOtp = false
                                            loginOtpError = err
                                        }
                                    )
                                }
                            )
                        }
                    }

                    if (loginOtpError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = loginOtpError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (loginOtpInput.trim().length < 6) {
                            loginOtpError = "সঠিক ৬ ডিজিটের ওটিপি কোড প্রবেশ করান।"
                            return@Button
                        }
                        isVerifyingLoginOtp = true
                        loginOtpError = null
                        viewModel.verifyOtp(
                            target = userToLogin.phone,
                            enteredOtp = loginOtpInput.trim(),
                            onSuccess = {
                                // [Somadhan Bug-Fix — গ্রুপ ১, নতুন সাব-বাগ ১.১০] আগে এখানেই
                                // isVerifyingLoginOtp/showLoginOtpDialog false করে dialog বন্ধ করে
                                // দেওয়া হতো, তারপর completeLoginAfterOtp() (network calls +
                                // realtime resubscribe) শেষ না হওয়া পর্যন্ত কোনো loading UI-ই থাকত
                                // না -- real-device-এ এই ফাঁকা gap-টাই "OTP দেওয়ার পর app আটকে
                                // গেছে/লগইন হচ্ছে না" মনে হওয়ার কারণ ছিল। এখন completeLoginAfterOtp()
                                // শেষ না হওয়া পর্যন্ত dialog "যাচাই হচ্ছে..." spinner-সহ খোলা থাকবে।
                                loginOtpError = null
                                viewModel.completeLoginAfterOtp(
                                    user = userToLogin,
                                    onSuccess = {
                                        isVerifyingLoginOtp = false
                                        showLoginOtpDialog = false
                                        if (userToLogin.hasUserRole && userToLogin.hasSolverRole) {
                                            showRoleChoiceDialog = true
                                        } else {
                                            onLoginSuccess()
                                        }
                                    }
                                )
                            },
                            onError = { err ->
                                isVerifyingLoginOtp = false
                                loginOtpError = err
                            }
                        )
                    },
                    enabled = !isVerifyingLoginOtp,
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("login_otp_verify_button")
                ) {
                    if (isVerifyingLoginOtp) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "যাচাই হচ্ছে...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "যাচাই ও লগইন",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isVerifyingLoginOtp,
                    onClick = {
                        showLoginOtpDialog = false
                        loginOtpInput = ""
                        loginOtpError = null
                        viewModel.clearOtp(userToLogin.phone)
                    }
                ) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    // Forgot Password Flow Dialog
    if (showForgotPasswordDialog) {
        val isForgotBusy = isSendingOtp || isVerifyingForgotOtp || isResettingPassword
        BottomSlideAlertDialog(
            onDismissRequest = {
                if (!isForgotBusy) {
                    showForgotPasswordDialog = false
                    forgotStep = 1
                    forgotError = null
                    viewModel.clearOtp(forgotIdentifier.trim())
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (forgotStep) {
                            1 -> "পাসওয়ার্ড পুনরুদ্ধার"
                            2 -> "OTP যাচাইকরণ"
                            else -> "নতুন পাসওয়ার্ড নির্ধারণ"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SomadhanTextPrimary
                    )
                    IconButton(
                        enabled = !isForgotBusy,
                        onClick = {
                            showForgotPasswordDialog = false
                            forgotStep = 1
                            forgotError = null
                            viewModel.clearOtp(forgotIdentifier.trim())
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = SomadhanTextSecondary)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    when (forgotStep) {
                        1 -> {
                            Text(
                                text = "আপনার নিবন্ধিত মোবাইল নম্বর বা ইমেইল লিখুন। আমরা একটি ৬-ডিজিটের ওটিপি যাচাইকরণ কোড পাঠাব।",
                                fontSize = 13.sp,
                                color = SomadhanTextSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = forgotIdentifier,
                                onValueChange = {
                                    forgotIdentifier = it
                                    forgotError = null
                                },
                                placeholder = { Text("মোবাইল নম্বর বা ইমেইল", color = SomadhanTextHint, fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = SomadhanTextSecondary)
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SomadhanOrange,
                                    unfocusedBorderColor = SomadhanBorder,
                                    focusedContainerColor = SomadhanBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("forgot_identifier_input")
                            )
                        }
                        2 -> {
                            Text(
                                text = "${forgotIdentifier.trim()} ঠিকানায় ৬ ডিজিটের গোপন ওটিপি যাচাই কোড পাঠানো হয়েছে। কোডটির মেয়াদ ৫ মিনিট।",
                                fontSize = 13.sp,
                                color = SomadhanTextSecondary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = forgotOtpInput,
                                onValueChange = {
                                    if (it.length <= 6) {
                                        forgotOtpInput = it
                                        forgotError = null
                                    }
                                },
                                placeholder = { Text("৬-ডিজিট ওটিপি কোড (যেমন: 123456)", color = SomadhanTextHint, fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SomadhanOrange,
                                    unfocusedBorderColor = SomadhanBorder,
                                    focusedContainerColor = SomadhanBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("forgot_otp_input")
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
                                            forgotOtpInput = "123456"
                                            forgotError = null
                                        }
                                        .testTag("forgot_otp_autofill")
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val minutesLeft = timerSeconds / 60
                                val secondsLeft = timerSeconds % 60
                                val timeStr = "${DistanceUtil.toBengaliDigits(minutesLeft.toString())}:${if (secondsLeft < 10) "০" else ""}${DistanceUtil.toBengaliDigits(secondsLeft.toString())}"
                                Text(
                                    text = if (timerSeconds > 0) "মেয়াদ বাকি: $timeStr" else "ওটিপির মেয়াদ শেষ",
                                    fontSize = 12.sp,
                                    color = if (timerSeconds > 0) SomadhanTextSecondary else SomadhanError
                                )
                                if (timerSeconds == 0) {
                                    Text(
                                        text = if (isSendingOtp) "পাঠানো হচ্ছে..." else "পুনরায় পাঠান",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrange,
                                        modifier = Modifier.clickable(enabled = !isSendingOtp) {
                                            isSendingOtp = true
                                            forgotError = null
                                            viewModel.sendForgotPasswordOtp(
                                                phoneOrEmail = forgotIdentifier.trim(),
                                                onSuccess = {
                                                    isSendingOtp = false
                                                    timerSeconds = 300
                                                    isTimerRunning = true
                                                },
                                                onError = { err ->
                                                    isSendingOtp = false
                                                    forgotError = err
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        3 -> {
                            Text(
                                text = "আপনার অ্যাকাউন্টের জন্য একটি শক্তিশালী নতুন পাসওয়ার্ড সেট করুন।",
                                fontSize = 13.sp,
                                color = SomadhanTextSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = forgotNewPassword,
                                onValueChange = {
                                    forgotNewPassword = it
                                    forgotError = null
                                },
                                placeholder = { Text("নতুন পাসওয়ার্ড", color = SomadhanTextHint, fontSize = 13.sp) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SomadhanOrange,
                                    unfocusedBorderColor = SomadhanBorder,
                                    focusedContainerColor = SomadhanBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("forgot_new_pass_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = forgotConfirmPassword,
                                onValueChange = {
                                    forgotConfirmPassword = it
                                    forgotError = null
                                },
                                placeholder = { Text("পাসওয়ার্ড নিশ্চিত করুন", color = SomadhanTextHint, fontSize = 13.sp) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SomadhanOrange,
                                    unfocusedBorderColor = SomadhanBorder,
                                    focusedContainerColor = SomadhanBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("forgot_confirm_pass_input")
                            )
                        }
                    }

                    if (forgotError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = forgotError ?: "",
                            fontSize = 12.sp,
                            color = SomadhanError,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                val isForgotBusy = isSendingOtp || isVerifyingForgotOtp || isResettingPassword
                Button(
                    enabled = !isForgotBusy,
                    onClick = {
                        when (forgotStep) {
                            1 -> {
                                if (forgotIdentifier.isBlank()) {
                                    forgotError = "মোবাইল নম্বর বা ইমেইল প্রদান করুন।"
                                    return@Button
                                }
                                isSendingOtp = true
                                forgotError = null
                                viewModel.sendForgotPasswordOtp(
                                    phoneOrEmail = forgotIdentifier.trim(),
                                    onSuccess = {
                                        isSendingOtp = false
                                        forgotStep = 2
                                        timerSeconds = 300
                                        isTimerRunning = true
                                    },
                                    onError = { err ->
                                        isSendingOtp = false
                                        forgotError = err
                                    }
                                )
                            }
                            2 -> {
                                if (forgotOtpInput.trim().length < 6) {
                                    forgotError = "সঠিক ৬ ডিজিটের ওটিপি কোড প্রবেশ করান।"
                                    return@Button
                                }
                                isVerifyingForgotOtp = true
                                viewModel.verifyOtp(
                                    target = forgotIdentifier.trim(),
                                    enteredOtp = forgotOtpInput.trim(),
                                    onSuccess = {
                                        isVerifyingForgotOtp = false
                                        forgotError = null
                                        forgotStep = 3
                                    },
                                    onError = { err ->
                                        isVerifyingForgotOtp = false
                                        forgotError = err
                                    }
                                )
                            }
                            3 -> {
                                if (forgotNewPassword.length < 6) {
                                    forgotError = "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"
                                    return@Button
                                }
                                if (forgotNewPassword != forgotConfirmPassword) {
                                    forgotError = "পাসওয়ার্ড দুটি মিলছে না।"
                                    return@Button
                                }
                                isResettingPassword = true
                                viewModel.resetPassword(
                                    phoneOrEmail = forgotIdentifier.trim(),
                                    newPass = forgotNewPassword,
                                    onSuccess = {
                                        isResettingPassword = false
                                        showForgotPasswordDialog = false
                                        forgotStep = 1
                                        forgotError = null
                                        phone = forgotIdentifier
                                        password = forgotNewPassword
                                    },
                                    onError = {
                                        isResettingPassword = false
                                        forgotError = it
                                    }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isForgotBusy) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isSendingOtp -> "পাঠানো হচ্ছে..."
                                isVerifyingForgotOtp -> "যাচাই হচ্ছে..."
                                else -> "পরিবর্তন হচ্ছে..."
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = when (forgotStep) {
                                1 -> "ওটিপি কোড পাঠান"
                                2 -> "যাচাই করুন"
                                else -> "পাসওয়ার্ড পরিবর্তন করুন"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                val isForgotBusy = isSendingOtp || isVerifyingForgotOtp || isResettingPassword
                TextButton(
                    enabled = !isForgotBusy,
                    onClick = {
                        showForgotPasswordDialog = false
                        forgotStep = 1
                        forgotError = null
                        viewModel.clearOtp(forgotIdentifier.trim())
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // App Logo & Header
            Image(
                painter = painterResource(id = R.drawable.somadhan_app_icon_1786820904533),
                contentDescription = "লোগো",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "সমাধানে স্বাগতম",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "আপনার অ্যাকাউন্ট দিয়ে লগইন করুন",
                fontSize = 13.sp,
                color = SomadhanTextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SomadhanDivider, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "ফোন নম্বর",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            errorMessage = null
                        },
                        placeholder = { Text("যেমন: 01712345678", color = SomadhanTextHint, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = SomadhanTextSecondary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_phone_input")
                    )

                    if (isAdminPhoneDetected) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text(
                                text = "🟢 Admin অ্যাকাউন্ট শনাক্ত হয়েছে",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "পাসওয়ার্ড",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        placeholder = { Text("আপনার পাসওয়ার্ড লিখুন", color = SomadhanTextHint, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SomadhanTextSecondary)
                        },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    // Forgot Password Link
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "পাসওয়ার্ড ভুলে গেছেন?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanOrange,
                            modifier = Modifier
                                .clickable {
                                    forgotIdentifier = phone
                                    showForgotPasswordDialog = true
                                }
                                .testTag("forgot_password_link")
                        )
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (phone.isBlank() || password.isBlank()) {
                                errorMessage = "ফোন নম্বর এবং পাসওয়ার্ড প্রদান করুন।"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            val trimmedPhone = phone.trim()

                            coroutineScope.launch {
                                // [ADMIN_ROLE_PROFILE সেশন ২] মাল্টি-এডমিন: ফোনটা কোনো এডমিন অ্যাকাউন্টের হলে
                                // পাসওয়ার্ড যাচাই real Supabase Auth sign-in দিয়ে (loginAsAdmin-এর ভেতরে) — আগের
                                // AdminCredentials একক-গেট আর নেই। sign-in/সেশন-স্টার্ট ব্যর্থ হলে onError-এ বার্তা।
                                if (viewModel.isAdminPhone(trimmedPhone)) {
                                    viewModel.loginAsAdmin(
                                        adminPhone = trimmedPhone,
                                        rawPassword = password,
                                        onReady = {
                                            isLoading = false
                                            onAdminLoginSuccess()
                                        },
                                        onError = { msg ->
                                            isLoading = false
                                            errorMessage = msg
                                        }
                                    )
                                    return@launch
                                }

                                // Normal user / solver flow
                                viewModel.validateLoginCredentials(
                                    phone = trimmedPhone,
                                    pass = password,
                                    onSuccess = { user ->
                                        // Credentials valid -> trigger OTP to registered phone number
                                        viewModel.sendOtp(
                                            target = user.phone,
                                            purpose = "login",
                                            onSuccess = {
                                                isLoading = false
                                                pendingLoginUser = user
                                                loginOtpInput = ""
                                                loginOtpError = null
                                                loginOtpTimerSeconds = 300
                                                isLoginOtpTimerRunning = true
                                                showLoginOtpDialog = true
                                            },
                                            onError = { sendErr ->
                                                isLoading = false
                                                errorMessage = sendErr
                                            }
                                        )
                                    },
                                    onError = { authErr ->
                                        isLoading = false
                                        errorMessage = authErr
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text(
                                text = "লগইন করুন",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // [ধাপ ১৪ ফলো-আপ] ব্যবহারকারীর স্পষ্ট সিদ্ধান্তে (rule #4 exception) এখানে
                    // আগে থাকা "1-Tap Quick Demo Login" সেকশন (ডেমো ইউজার/সলভার/অ্যাডমিন বাটন,
                    // যেগুলো quickLoginForDemo()/loginAsAdmin() no-real-auth-session দিয়ে সরাসরি
                    // ঢুকিয়ে দিত) সম্পূর্ণ সরিয়ে ফেলা হয়েছে। এখন থেকে লগইন শুধুমাত্র real
                    // Supabase phone+password (validateLoginCredentials/OTP flow) দিয়ে হবে, আর
                    // অ্যাডমিন লগইন হবে উপরের ফর্মেই (সেশন ২ থেকে: মাল্টি-এডমিন, real Supabase Auth phone+password)
                    // (সেটা অক্ষত রাখা হয়েছে -- এটা "ডেমো" না, real admin credential flow)।
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Register navigation footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "অ্যাকাউন্ট নেই? ",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
                Text(
                    text = "নতুন অ্যাকাউন্ট তৈরি করুন",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanOrange,
                    modifier = Modifier
                        .clickable { onNavigateToRegister() }
                        .testTag("register_link")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
