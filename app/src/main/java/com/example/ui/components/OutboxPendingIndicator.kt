package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SomadhanTextPrimary
import com.example.util.DistanceUtil

/**
 * ধাপ ৮ (RPC_SYNC_FIX ট্র্যাক, UI ইন্ডিকেটর) — মাস্টার প্ল্যান অনুযায়ী:
 * "outbox-এ pending entry থাকলে একটা ছোট, non-intrusive indicator ... + একটা 'এখনই আবার
 * চেষ্টা করুন' বাটন"।
 *
 * ডিজাইন সিদ্ধান্ত (কমন রুল মেনে):
 * - `pendingCount == 0` হলে এই কম্পোজেবল কিছুই রেন্ডার করে না (`AnimatedVisibility` দিয়ে,
 *   height/space সহ পুরোপুরি সরে যায় -- কলার সাইডে খালি জায়গা রাখতে হয় না)।
 * - কোনো blocking dialog/modal না -- শুধু একটা ছোট, non-dismissible-না-হওয়া inline card।
 *   ইউজারকে বাকি স্ক্রিন ব্যবহার করা থেকে আটকায় না।
 * - "এখনই আবার চেষ্টা করুন" বাটন শুধু `onRetryClick` কল করে (ViewModel.retryOutboxSyncNow() ->
 *   OutboxSyncWorker.triggerImmediate()) -- outbox-এর কোনো ডেটা এই কম্পোজেবল নিজে বদলায় না,
 *   শুধু read (pendingCount) + একটা ট্রিগার কল।
 */
@Composable
fun OutboxPendingIndicator(
    pendingCount: Int,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = pendingCount > 0,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "কিছু লেনদেন (${DistanceUtil.toBengaliDigits(pendingCount.toString())}টি) cloud-এ sync হতে বাকি আছে, নেটওয়ার্ক এলে স্বয়ংক্রিয়ভাবে হবে",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SomadhanTextPrimary
                    )
                }

                TextButton(onClick = onRetryClick) {
                    Text(
                        text = "এখনই চেষ্টা করুন",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                }
            }
        }
    }
}
