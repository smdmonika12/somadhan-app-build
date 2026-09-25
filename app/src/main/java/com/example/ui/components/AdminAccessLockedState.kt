package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanSurfaceVariant
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextSecondary

/**
 * [ADMIN_ROLE_PROFILE সেশন ৭.০] "এই মেনুতে অ্যাক্সেস নেই" শেয়ার্ড খালি-স্টেট — `admin-role-management.html`
 * মকআপের `.sim-locked` স্টাইলের প্রোডাকশন সংস্করণ (dashed→solid muted border card, কেন্দ্রীভূত)।
 *
 * সেশন ৭.১-৭.৮-এ প্রতিটা `AdminXxxView.kt`-এর ট্যাব/স্ক্রিন খোলার সময় ব্যবহৃত হবে যখন
 * `AdminSession.canView(groupId, itemId)` (বা সমতুল্য `canAct(".../view")`) false — অর্থাৎ বর্তমান
 * লগইন-করা এডমিনের রোলে এই মেনুর 'দেখুন' পারমিশনই নেই। সাধারণ স্ক্রিন-কন্টেন্টের বদলে এই একটা
 * কম্পোজেবল বসিয়ে দিলেই হবে, স্ক্রিনের বাকি কোনো কোড রান হবে না (ডেটা-লোড/ViewModel কল এড়িয়ে যাওয়া
 * caller-এর দায়িত্ব — `if (AdminSession.canView(...)) { ...আসল কন্টেন্ট... } else { AdminAccessLockedState() }`)।
 *
 * এটা কোনো ফ্ল্যাগড-অ্যাকশন-লকের জন্য না (ফ্ল্যাগড অ্যাডমিনের view অক্ষত থাকে বলে এই স্ক্রিনই দেখানো হয়
 * না) — প্রতিটা অ্যাকশন-বাটনের নিজস্ব `enabled = AdminSession.canAct(...)` দিয়ে গেট হবে, আলাদা কোনো
 * শেয়ার্ড "লকড-অ্যাকশন" কম্পোজেবলের দরকার নেই (মকআপেও বাটন শুধু disabled দেখায়, আলাদা বার্তা-বক্স না)।
 */
@Composable
fun AdminAccessLockedState(
    modifier: Modifier = Modifier,
    title: String = "এই মেনুতে অ্যাক্সেস নেই",
    message: String = "আপনার বর্তমান রোলে এই মেনু দেখার অনুমতি নেই। প্রয়োজন মনে করলে সুপার অ্যাডমিনের সাথে যোগাযোগ করুন।"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)
            .border(BorderStroke(1.dp, SomadhanBorder), RoundedCornerShape(12.dp))
            .background(SomadhanSurfaceVariant, RoundedCornerShape(12.dp))
            .padding(vertical = 30.dp, horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = SomadhanTextHint,
            modifier = Modifier.size(30.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = SomadhanTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            fontSize = 11.5.sp,
            color = SomadhanTextHint,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}
