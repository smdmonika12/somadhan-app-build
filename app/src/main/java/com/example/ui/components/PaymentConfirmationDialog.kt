package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.util.DistanceUtil

/**
 * PaymentConfirmationDialog (Phase I.1)
 *
 * Parameters:
 * - walletBalance: Double
 * - bidAmount: Double
 * - onProceed: () -> Unit
 * - onDismiss: () -> Unit
 *
 * Content:
 * - Current Wallet Balance
 * - If walletBalance >= bidAmount: "ওয়ালেট থেকে কাটা হবে: ৳{bidAmount}"
 * - If walletBalance < bidAmount: "ওয়ালেট থেকে কাটা হবে: ৳{walletBalance}, বাকি ৳{bidAmount - walletBalance} পেমেন্ট গেটওয়ে দিয়ে দিতে হবে"
 * - "এগিয়ে যান" CTA -> onProceed()
 * - "বাতিল" -> onDismiss()
 */
@Composable
fun PaymentConfirmationDialog(
    walletBalance: Double,
    bidAmount: Double,
    onProceed: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val isFullWallet = walletBalance >= bidAmount
    val walletDeduction = if (isFullWallet) bidAmount else walletBalance
    val gatewayAmount = if (isFullWallet) 0.0 else (bidAmount - walletBalance).coerceAtLeast(0.0)

    BottomSlideDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("payment_confirmation_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SomadhanOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = SomadhanOrange,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "পেমেন্ট নিশ্চিতকরণ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "বিড গ্রহণের জন্য পেমেন্টের বিবরণ নিচে দেখুন",
                    fontSize = 12.5.sp,
                    color = SomadhanTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Detail Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SomadhanBg,
                    border = BorderStroke(1.dp, SomadhanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total Bid Amount
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "মোট বিড মূল্য:",
                                fontSize = 13.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(bidAmount)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                        }

                        HorizontalDivider(color = SomadhanDivider, thickness = 0.8.dp)

                        // Current Wallet Balance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = SomadhanOrange,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "আপনার ওয়ালেট ব্যালেন্স:",
                                    fontSize = 13.sp,
                                    color = SomadhanTextSecondary
                                )
                            }
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(walletBalance)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (walletBalance > 0) SomadhanSuccess else SomadhanTextSecondary
                            )
                        }

                        HorizontalDivider(color = SomadhanDivider, thickness = 0.8.dp)

                        // Deduction Info
                        if (isFullWallet) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ওয়ালেট থেকে কাটা হবে:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanSuccess
                                )
                                Text(
                                    text = "৳ ${DistanceUtil.toBengaliDigits(walletDeduction)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanSuccess
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ওয়ালেট থেকে কাটা হবে:",
                                        fontSize = 13.sp,
                                        color = SomadhanSuccess
                                    )
                                    Text(
                                        text = "৳ ${DistanceUtil.toBengaliDigits(walletDeduction)}",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanSuccess
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "বাকি পেমেন্ট গেটওয়ে দিয়ে:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanOrangePressed
                                    )
                                    Text(
                                        text = "৳ ${DistanceUtil.toBengaliDigits(gatewayAmount)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrangePressed
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Informational badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isFullWallet) SomadhanSuccessLight.copy(alpha = 0.5f) else SomadhanOrangeLight.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFullWallet) Icons.Default.AccountBalanceWallet else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isFullWallet) SomadhanSuccess else SomadhanOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFullWallet) {
                                "ওয়ালেট থেকে কাটা হবে: ৳${DistanceUtil.toBengaliDigits(bidAmount)}"
                            } else {
                                "ওয়ালেট থেকে কাটা হবে: ৳${DistanceUtil.toBengaliDigits(walletBalance)}, বাকি ৳${DistanceUtil.toBengaliDigits(gatewayAmount)} পেমেন্ট গেটওয়ে দিয়ে দিতে হবে"
                            },
                            fontSize = 11.5.sp,
                            color = SomadhanTextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = SomadhanError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SomadhanBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("payment_confirm_cancel_button")
                    ) {
                        Text(
                            text = "বাতিল",
                            color = SomadhanTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }

                    Button(
                        onClick = onProceed,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                            .testTag("payment_confirm_proceed_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "কনফার্ম হচ্ছে...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        } else {
                            Text(
                                text = "এগিয়ে যান",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ExtraAmountPaymentConfirmationDialog (Phase K.1)
 *
 * Parameters:
 * - extraAmount: Double
 * - note: String?
 * - walletBalance: Double
 * - onProceed: () -> Unit
 * - onDismiss: () -> Unit
 */
@Composable
fun ExtraAmountPaymentConfirmationDialog(
    extraAmount: Double,
    note: String?,
    walletBalance: Double,
    onProceed: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val isFullWallet = walletBalance >= extraAmount
    val walletDeduction = if (isFullWallet) extraAmount else walletBalance
    val gatewayAmount = if (isFullWallet) 0.0 else (extraAmount - walletBalance).coerceAtLeast(0.0)
    var isProceeding by remember { mutableStateOf(false) }
    // isProceeding guards the single frame between tap and this composable actually leaving
    // composition; isLoading is the caller's real backend-call status for callers that now keep
    // this dialog open (instead of closing it immediately) until the request truly finishes --
    // both are respected so the lock holds for the whole duration either way.
    val isBusy = isProceeding || isLoading

    BottomSlideDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("extra_amount_payment_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SomadhanOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = SomadhanOrange,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "অতিরিক্ত বিল অনুমোদন ও পেমেন্ট",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "অনুরোধ অনুমোদন করার পূর্বে পেমেন্টের বিবরণ নিচে দেখুন",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Detail Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SomadhanBg,
                    border = BorderStroke(1.dp, SomadhanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Extra Amount
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "অতিরিক্ত বিলের পরিমাণ:",
                                fontSize = 13.sp,
                                color = SomadhanTextSecondary
                            )
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(extraAmount)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanOrangePressed
                            )
                        }

                        if (!note.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "কারণ / বিবরণ:",
                                    fontSize = 12.sp,
                                    color = SomadhanTextSecondary
                                )
                                Text(
                                    text = note,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SomadhanTextPrimary,
                                    modifier = Modifier.padding(start = 12.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        HorizontalDivider(color = SomadhanDivider, thickness = 0.8.dp)

                        // Current Wallet Balance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = SomadhanOrange,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "আপনার ওয়ালেট ব্যালেন্স:",
                                    fontSize = 13.sp,
                                    color = SomadhanTextSecondary
                                )
                            }
                            Text(
                                text = "৳ ${DistanceUtil.toBengaliDigits(walletBalance)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (walletBalance > 0) SomadhanSuccess else SomadhanTextSecondary
                            )
                        }

                        HorizontalDivider(color = SomadhanDivider, thickness = 0.8.dp)

                        // Deduction Info
                        if (isFullWallet) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ওয়ালেট থেকে কাটা হবে:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanSuccess
                                )
                                Text(
                                    text = "৳ ${DistanceUtil.toBengaliDigits(walletDeduction)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanSuccess
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (walletDeduction > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ওয়ালেট থেকে কাটা হবে:",
                                            fontSize = 13.sp,
                                            color = SomadhanSuccess
                                        )
                                        Text(
                                            text = "৳ ${DistanceUtil.toBengaliDigits(walletDeduction)}",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SomadhanSuccess
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "বাকি পেমেন্ট গেটওয়ে দিয়ে:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SomadhanOrangePressed
                                    )
                                    Text(
                                        text = "৳ ${DistanceUtil.toBengaliDigits(gatewayAmount)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrangePressed
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Informational badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isFullWallet) SomadhanSuccessLight.copy(alpha = 0.5f) else SomadhanOrangeLight.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFullWallet) Icons.Default.AccountBalanceWallet else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isFullWallet) SomadhanSuccess else SomadhanOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFullWallet) {
                                "অনুমোদন করলে সম্পূর্ণ ৳${DistanceUtil.toBengaliDigits(extraAmount)} আপনার ওয়ালেট থেকে কেটে এসক্রোতে জমা হবে।"
                            } else {
                                "ওয়ালেট থেকে কাটা হবে ৳${DistanceUtil.toBengaliDigits(walletDeduction)}, বাকি ৳${DistanceUtil.toBengaliDigits(gatewayAmount)} পেমেন্ট সম্পন্ন হলে এসক্রোতে জমা হবে।"
                            },
                            fontSize = 11.5.sp,
                            color = SomadhanTextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = SomadhanError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SomadhanBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("extra_amount_confirm_cancel_button")
                    ) {
                        Text(
                            text = "বাতিল",
                            color = SomadhanTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }

                    Button(
                        onClick = {
                            // Bug fix (loading-lock): this dialog is shared by every "extra bill
                            // accept" entry point in the app (wallet-only and gateway-remainder
                            // flows, on both ProblemDetailScreen and JobTrackingScreen). None of
                            // its callers had a way to stop a second tap here before the dialog
                            // closed on the next recomposition -- a real risk on a slow network,
                            // which is exactly when people re-tap. Guarding it once, inside the
                            // shared dialog itself, covers all three call sites at once.
                            if (!isBusy) {
                                isProceeding = true
                                onProceed()
                            }
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                            .testTag("extra_amount_confirm_proceed_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "কনফার্ম হচ্ছে...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        } else {
                            Text(
                                text = "এগিয়ে যান",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
