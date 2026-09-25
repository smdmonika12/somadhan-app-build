package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.util.DistanceUtil
import com.example.util.Formatters
import kotlinx.coroutines.delay

enum class PaymentGatewayProvider(
    val title: String,
    val brandColor: Color,
    val lightBg: Color,
    val shortName: String
) {
    BKASH("bKash (বিকাশ)", Color(0xFFE2136E), Color(0xFFFDF2F8), "বিকাশ"),
    NAGAD("Nagad (নগদ)", Color(0xFFF7941D), Color(0xFFFFF7ED), "নগদ"),
    ROCKET("Rocket (রকেট)", Color(0xFF8C3494), Color(0xFFFAF5FF), "রকেট")
}

private enum class PaymentDialogState {
    INPUT,
    PROCESSING,
    SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantPaymentDialog(
    amount: Double,
    problemTitle: String? = null,
    solverName: String? = null,
    onPaymentSuccess: () -> Unit,
    onDismissRequest: () -> Unit,
    onPaymentCompleteWithDetails: ((gateway: String, trxId: String, phone: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedProvider by remember { mutableStateOf(PaymentGatewayProvider.BKASH) }
    var phoneNumber by remember { mutableStateOf("") }
    var pinCode by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }
    var dialogState by remember { mutableStateOf(PaymentDialogState.INPUT) }
    val generatedTrxId = remember { "TRX" + System.currentTimeMillis().toString().takeLast(8).uppercase() }

    val amountBengali = Formatters.formatTaka(amount)
    val checkmarkScale = remember { Animatable(0f) }

    LaunchedEffect(dialogState) {
        when (dialogState) {
            PaymentDialogState.PROCESSING -> {
                // 1.5 seconds loading simulation
                delay(1500)
                dialogState = PaymentDialogState.SUCCESS
            }
            PaymentDialogState.SUCCESS -> {
                checkmarkScale.animateTo(
                    targetValue = 1.15f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
                checkmarkScale.animateTo(1.0f, animationSpec = tween(150))
                // Wait briefly for the user to view the success checkmark
                delay(1000)
                onPaymentCompleteWithDetails?.invoke(selectedProvider.name, generatedTrxId, phoneNumber)
                onPaymentSuccess()
                onDismissRequest()
            }
            PaymentDialogState.INPUT -> {
                checkmarkScale.snapTo(0f)
            }
        }
    }

    Dialog(
        onDismissRequest = {
            // Intentionally empty: outside taps or back gestures will not dismiss
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 48.dp)
                        .testTag("merchant_payment_dialog")
                ) {
                    AnimatedContent(
                        targetState = dialogState,
                        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                        label = "PaymentDialogAnimation"
                    ) { state ->
                when (state) {
                    PaymentDialogState.INPUT -> {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Dialog Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(selectedProvider.lightBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Payment,
                                            contentDescription = null,
                                            tint = selectedProvider.brandColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "এসক্রো পেমেন্ট গেটওয়ে",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanTextPrimary
                                        )
                                        Text(
                                            text = "মার্চেন্ট পেমেন্ট চেকআউট",
                                            fontSize = 12.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onDismissRequest,
                                    modifier = Modifier.size(34.dp).testTag("close_payment_dialog_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "বন্ধ করুন",
                                        tint = SomadhanTextHint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Amount & Summary Card
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = selectedProvider.lightBg),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, selectedProvider.brandColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "মোট প্রদেয় অর্থ",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SomadhanTextSecondary
                                        )
                                        Text(
                                            text = amountBengali,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = selectedProvider.brandColor
                                        )
                                    }

                                    if (!solverName.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "সমাধানকারী: $solverName",
                                            fontSize = 12.5.sp,
                                            color = SomadhanTextSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "কাজ সম্পন্ন না হওয়া পর্যন্ত টাকা এসক্রোতে নিরাপদ থাকবে",
                                            fontSize = 11.sp,
                                            color = Color(0xFF15803D),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Provider Selector Buttons
                            Text(
                                text = "পেমেন্ট মাধ্যম বেছে নিন",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PaymentGatewayProvider.values().forEach { provider ->
                                    val isSelected = selectedProvider == provider
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) provider.lightBg else SomadhanCardBg)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) provider.brandColor else SomadhanDivider,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                selectedProvider = provider
                                                inputError = null
                                            }
                                            .padding(vertical = 12.dp)
                                            .testTag("provider_btn_${provider.name.lowercase()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = provider.shortName,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) provider.brandColor else SomadhanTextPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Phone Number Field
                            Text(
                                text = "${selectedProvider.shortName} মোবাইল নম্বর *",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = {
                                    phoneNumber = it
                                    inputError = null
                                },
                                placeholder = { Text("01XXXXXXXXX", fontSize = 13.5.sp, color = SomadhanTextHint) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = selectedProvider.brandColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = selectedProvider.brandColor,
                                    unfocusedBorderColor = SomadhanBorder,
                                    focusedContainerColor = SomadhanBg,
                                    unfocusedContainerColor = SomadhanCardBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("payment_phone_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // PIN Code Field
                            Text(
                                text = "${selectedProvider.shortName} পিন কোড (PIN) *",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = pinCode,
                                onValueChange = {
                                    if (it.length <= 6) {
                                        pinCode = it
                                        inputError = null
                                    }
                                },
                                placeholder = { Text("••••", fontSize = 13.5.sp, color = SomadhanTextHint) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = selectedProvider.brandColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = selectedProvider.brandColor,
                                    unfocusedBorderColor = SomadhanBorder,
                                    focusedContainerColor = SomadhanBg,
                                    unfocusedContainerColor = SomadhanCardBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("payment_pin_input")
                            )

                            if (inputError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = inputError ?: "",
                                    fontSize = 12.sp,
                                    color = SomadhanError
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Demo notice disclaimer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFB45309),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "এটি একটি ডেমো পেমেন্ট সিস্টেম — কোনো প্রকৃত টাকা কাটা হচ্ছে না।",
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismissRequest,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text("বাতিল", fontSize = 13.5.sp, color = SomadhanTextSecondary)
                                }

                                Button(
                                    onClick = {
                                        if (phoneNumber.trim().isEmpty()) {
                                            inputError = "মোবাইল নম্বর প্রদান করুন।"
                                            return@Button
                                        }
                                        if (pinCode.trim().isEmpty()) {
                                            inputError = "পিন কোড (PIN) প্রদান করুন।"
                                            return@Button
                                        }
                                        dialogState = PaymentDialogState.PROCESSING
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = selectedProvider.brandColor),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.5f).height(48.dp).testTag("confirm_payment_btn")
                                ) {
                                    Text(
                                        text = "পে করুন ৳${DistanceUtil.toBengaliDigits(amount.toInt().toString())}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    PaymentDialogState.PROCESSING -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = selectedProvider.brandColor,
                                strokeWidth = 3.5.dp,
                                modifier = Modifier.size(52.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "পেমেন্ট প্রসেসিং হচ্ছে...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${selectedProvider.shortName} গেটওয়ের মাধ্যমে এসক্রো তহবিলে যুক্ত হচ্ছে",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    PaymentDialogState.SUCCESS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .scale(checkmarkScale.value)
                                    .clip(CircleShape)
                                    .background(SomadhanSuccess.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "পেমেন্ট সফল",
                                    tint = SomadhanSuccess,
                                    modifier = Modifier.size(46.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "পেমেন্ট সফল হয়েছে ✅",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanSuccess,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "$amountBengali টাকা এসক্রো অ্যাকাউন্টে সুরক্ষিতভাবে জমা হয়েছে এবং বিডটি সফলভাবে গৃহীত হয়েছে।",
                                fontSize = 12.5.sp,
                                color = SomadhanTextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SomadhanBg)
                                    .border(1.dp, SomadhanBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(generatedTrxId))
                                        Toast.makeText(context, "ট্রানজেকশন আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "TrxID: $generatedTrxId",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = selectedProvider.brandColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "কপি করুন",
                                    tint = selectedProvider.brandColor,
                                    modifier = Modifier.size(13.dp)
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
}
