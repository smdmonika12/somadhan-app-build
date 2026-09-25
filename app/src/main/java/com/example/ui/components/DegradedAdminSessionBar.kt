package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight

/**
 * [Somadhan Bug-Fix Step 6 — গ্রুপ ৩.১c] `SomadhanViewModel.loginAsAdmin()`-এ real Supabase Auth
 * সাইন-ইন ব্যর্থ হয়ে admin একটা degraded local-only সেশনে ঢুকলে, আগে শুধু একবার ২-৩ সেকেন্ডের toast
 * দেখানো হতো -- এই bar-টা তার বদলে/পাশাপাশি **persistent** থাকে (AdminPanelScreen-এর topBar-এ
 * RealtimeLocationBar-এর ঠিক নিচে বসানো, তাই সব admin ট্যাবেই দৃশ্যমান) যতক্ষণ না admin নিজে "✕"-এ
 * ট্যাপ করে dismiss করেন বা আবার সফলভাবে লগইন/লগআউট করেন।
 *
 * এই অবস্থায় থাকাকালীন RLS-গেটেড কোনো cloud write (is_admin(auth.uid()) দরকার এমন) নীরবে ব্যর্থ
 * হতে পারে -- এই bar admin-কে সেটা মনে করিয়ে দেওয়ার জন্যই।
 */
@Composable
fun DegradedAdminSessionBar(
    visible: Boolean,
    reason: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier.testTag("degraded_admin_session_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SomadhanErrorLight)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "সতর্কতা",
                    tint = SomadhanError,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "সীমিত সেশন: cloud-এ পরিবর্তন সেভ নাও হতে পারে" +
                        (if (!reason.isNullOrBlank()) " ($reason)" else ""),
                    fontSize = 12.sp,
                    color = SomadhanError,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("degraded_admin_session_bar_dismiss")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "সতর্কবার্তা বন্ধ করুন",
                    tint = SomadhanError,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
