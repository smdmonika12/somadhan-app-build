package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.WithdrawalEntity
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalDetailBottomSheet(
    withdrawal: WithdrawalEntity,
    onDismiss: () -> Unit,
    onNavigateToSupport: (() -> Unit)? = null,
    accentColor: Color = Color(0xFF1D4ED8)
) {
    val context = LocalContext.current
    val isRejected = withdrawal.status == "REJECTED"
    val isCompleted = withdrawal.status == "COMPLETED"
    val isPending = !isRejected && !isCompleted

    val statusTitle = when {
        isRejected -> "উইথড্র বাতিলের বিবরণ"
        isCompleted -> "উইথড্র সম্পন্নের বিবরণ"
        else -> "উইথড্র প্রক্রিয়ার বিবরণ"
    }

    val statusBadgeText = when {
        isRejected -> "বাতিলকৃত"
        isCompleted -> "সফল"
        else -> "প্রক্রিয়াধীন"
    }

    val statusBgColor = when {
        isRejected -> Color(0xFFFEE2E2)
        isCompleted -> Color(0xFFDCFCE7)
        else -> Color(0xFFFEF3C7)
    }

    val statusTextColor = when {
        isRejected -> SomadhanError
        isCompleted -> SomadhanSuccess
        else -> Color(0xFFD97706)
    }

    val withdrawSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = withdrawSheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row with Title, Badge, and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(statusBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isRejected -> Icons.Default.ErrorOutline
                                isCompleted -> Icons.Default.CheckCircle
                                else -> Icons.Default.HourglassTop
                            },
                            contentDescription = null,
                            tint = statusTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusTitle,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusBgColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusBadgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusTextColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "বন্ধ করুন",
                            tint = SomadhanTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary Amount Card
            val summaryContainerColor = when {
                isRejected -> Color(0xFFFEF2F2)
                isCompleted -> Color(0xFFF0FDF4)
                else -> Color(0xFFFFFBEB)
            }
            val summaryBorderColor = when {
                isRejected -> Color(0xFFFECACA)
                isCompleted -> Color(0xFFBBF7D0)
                else -> Color(0xFFFDE68A)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = summaryContainerColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, summaryBorderColor, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "উইথড্র উত্তোলিত অর্থ",
                            fontSize = 12.sp,
                            color = SomadhanTextSecondary
                        )
                        Text(
                            text = when {
                                isRejected -> "টাকা ওয়ালেটে ফেরত দেওয়া হয়েছে"
                                isCompleted -> "সফলভাবে পাঠানো হয়েছে"
                                else -> "অনুরোধটি বর্তমানে প্রক্রিয়াধীন রয়েছে"
                            },
                            fontSize = 10.5.sp,
                            color = statusTextColor
                        )
                    }
                    Text(
                        text = Formatters.formatTaka(withdrawal.amount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Withdraw ID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("উইথড্র আইডি:", fontSize = 12.5.sp, color = SomadhanTextSecondary)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Withdraw ID", withdrawal.id))
                                Toast.makeText(context, "উইথড্র আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = withdrawal.id,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Withdraw ID",
                                modifier = Modifier.size(11.dp),
                                tint = accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isRejected) {
                        Text(
                            text = "বাতিলের কারণ:",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = withdrawal.rejectionReason?.ifBlank { "কোনো নির্দিষ্ট কারণ উল্লেখ করা হয়নি" }
                                ?: "কোনো নির্দিষ্ট কারণ উল্লেখ করা হয়নি",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanError
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "উইথড্রটি বাতিল হওয়ায় টাকাটি পুনরায় আপনার ওয়ালেটে ফেরত দেওয়া হয়েছে। কোনো জিজ্ঞাসা থাকলে আমাদের কাস্টমার সাপোর্টে যোগাযোগ করুন।",
                            fontSize = 11.5.sp,
                            color = SomadhanTextSecondary,
                            lineHeight = 16.sp
                        )
                    } else {
                        // COMPLETED or PENDING
                        Text(
                            text = "মাধ্যম (অপারেটর): ${withdrawal.method}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = SomadhanTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "অ্যাকাউন্ট নম্বর: ${withdrawal.accountNumber}",
                            fontSize = 12.5.sp,
                            color = SomadhanTextSecondary
                        )

                        if (!withdrawal.bankName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ব্যাংক: ${withdrawal.bankName}${if (!withdrawal.branchName.isNullOrBlank()) ", শাখা: ${withdrawal.branchName}" else ""}",
                                fontSize = 12.sp,
                                color = SomadhanTextHint
                            )
                        }
                        if (!withdrawal.accountHolderName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "হোল্ডার: ${withdrawal.accountHolderName}",
                                fontSize = 12.sp,
                                color = SomadhanTextHint
                            )
                        }

                        if (isCompleted) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            val trxIdVal = withdrawal.trxId?.ifBlank { "N/A" } ?: "N/A"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(enabled = trxIdVal != "N/A") {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("TrxID", trxIdVal))
                                    Toast.makeText(context, "ট্রানজেকশন আইডি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = "ট্রানজেকশন আইডি: $trxIdVal",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SomadhanSuccess
                                )
                                if (trxIdVal != "N/A") {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "কপি করুন",
                                        tint = SomadhanSuccess,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        } else if (isPending) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "স্ট্যাটাস: অনুরোধটি অ্যাডমিন পর্যালোচনার জন্য জমা রয়েছে। অনুমোদন সম্পন্ন হলে টাকা আপনার অ্যাকাউন্টে পাঠিয়ে দেওয়া হবে।",
                                fontSize = 11.5.sp,
                                color = Color(0xFFD97706),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "তারিখ: ${Formatters.formatDateTimeBengali(withdrawal.createdAt)}",
                        fontSize = 11.sp,
                        color = SomadhanTextHint
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (isRejected && onNavigateToSupport != null) {
                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToSupport()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "সাপোর্টে যোগাযোগ করুন",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bottom Close button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRejected) Color(0xFFF1F5F9) else accentColor
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "বন্ধ করুন",
                    color = if (isRejected) SomadhanTextPrimary else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
