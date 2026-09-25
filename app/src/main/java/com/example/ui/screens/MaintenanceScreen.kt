package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.UserEntity
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary

@Composable
fun MaintenanceScreen(
    maintenanceMessage: String,
    currentUser: UserEntity?,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SomadhanBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated/Glow Icon Container
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                SomadhanOrange.copy(alpha = 0.25f),
                                SomadhanOrange.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(2.dp, SomadhanOrange.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "মেইনটেন্যান্স",
                    tint = SomadhanOrange,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "অ্যাপ রক্ষণাবেক্ষণ চলছে",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SomadhanOrange.copy(alpha = 0.15f))
                    .border(1.dp, SomadhanOrange.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SomadhanOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MAINTENANCE MODE ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Admin Custom Maintenance Message Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "অ্যাডমিন নোটিশ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (maintenanceMessage.isNotBlank()) {
                            maintenanceMessage
                        } else {
                            "অ্যাপটির রক্ষণাবেক্ষণ ও সার্ভার আপগ্রেডের কাজ চলছে। শীঘ্রই স্বাভাবিক সেবা চালু হবে। আপনার সহযোগিতার জন্য ধন্যবাদ।"
                        },
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = SomadhanTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Info Banner
            if (currentUser != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanDivider)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "লগইন করা অ্যাকাউন্ট:",
                                fontSize = 11.sp,
                                color = SomadhanTextHint
                            )
                            Text(
                                text = "${currentUser.name} (${if (currentUser.role.equals("SOLVER", true)) "সমাধানকারী" else "গ্রাহক"})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanTextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = SomadhanTextHint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Logout / Switch Account Button
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SomadhanCardBg,
                    contentColor = SomadhanError
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanError.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "লগআউট",
                    tint = SomadhanError,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "অ্যাকাউন্ট থেকে লগআউট করুন",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanError
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "অ্যাডমিন ক্রেডেনশিয়াল থাকলে লগআউট করে অ্যাডমিন হিসেবে প্রবেশ করতে পারেন।",
                fontSize = 11.sp,
                color = SomadhanTextHint,
                textAlign = TextAlign.Center
            )
        }
    }
}
