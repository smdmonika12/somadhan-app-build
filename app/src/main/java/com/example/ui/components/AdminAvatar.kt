package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.screens.adminInitials
import com.example.ui.theme.SomadhanAdminSlateLight
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeContainer
import com.example.ui.theme.SomadhanOrangePressed
import com.example.ui.theme.SomadhanTextPrimary

/**
 * [ADMIN_ROLE_PROFILE সেশন ৬] এডমিনের গোল অবতার — নিচের স্তরে আদ্যক্ষর, উপরের স্তরে ছবি (থাকলে)।
 *
 * ছবি লোড না হলে (নেটওয়ার্ক/ভাঙা URL) `AsyncImage` কিছু আঁকে না, তাই নিচের আদ্যক্ষর দৃশ্যমান থাকে — আলাদা
 * error-স্টেট লাগে না। [photo] যেকোনো Coil-মডেল: সার্ভারের URL (`String`) বা ব্যবহারকারীর সদ্য-বাছা ছবির
 * `Uri` (সংরক্ষণের আগের প্রিভিউ); ফাঁকা স্ট্রিং/null = ছবি নেই।
 * [ring] true হলে ব্র্যান্ড-অরেঞ্জ রিং (টপ-বারের অবতার ও সুপারের জন্য)।
 */
@Composable
fun AdminAvatar(
    name: String,
    photo: Any?,
    isSuper: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    ring: Boolean = false
) {
    val model: Any? = if (photo is String && photo.isBlank()) null else photo
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isSuper) SomadhanOrangeContainer else SomadhanAdminSlateLight)
            .then(if (ring) Modifier.border(BorderStroke(2.dp, SomadhanOrange), CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = adminInitials(name),
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Bold,
            color = if (isSuper) SomadhanOrangePressed else SomadhanTextPrimary
        )
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = "প্রোফাইল ছবি",
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}
