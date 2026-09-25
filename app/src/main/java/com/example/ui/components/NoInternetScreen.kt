package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanOrangePressed
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen No Internet Error Overlay adhering to Somadhan's design system:
 * - Clean white background
 * - High-contrast black typography
 * - Vibrant Somadhan Orange primary accent button
 * - Real-time auto-dismissal when connectivity returns
 */
@Composable
fun NoInternetOverlay(
    isVisible: Boolean,
    onRetry: () -> Boolean,
    isSolver: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 10 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 10 }),
            modifier = modifier
        ) {
            NoInternetScreenContent(
                onRetry = onRetry,
                isSolver = isSolver
            )
        }
    }
}

@Composable
fun NoInternetScreenContent(
    onRetry: () -> Boolean,
    isSolver: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isChecking by remember { mutableStateOf(false) }
    var showQuickToast by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val accentColor = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8)
    val lightAccentBg = if (isSolver) SomadhanOrangeLight else Color(0xFFEFF6FF)
    val lightAccentBorder = if (isSolver) SomadhanOrange.copy(alpha = 0.3f) else Color(0xFFBFDBFE)
    val toastTextColor = if (isSolver) SomadhanOrangePressed else Color(0xFF1E40AF)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // Intercept clicks so user can't interact with background
            )
            .testTag("no_internet_screen"),
        color = SomadhanBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Outer glow icon container
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(lightAccentBg),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .shadow(elevation = 3.dp, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "কোনো ইন্টারনেট সংযোগ নেই",
                            tint = accentColor,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Title
                Text(
                    text = "ইন্টারনেট সংযোগ নেই",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subtitle / Description
                Text(
                    text = "আপনার ডিভাইসটি ইন্টারনেটের সাথে যুক্ত নেই। অনুগ্রহ করে মোবাইল ডেটা বা ওয়াই-ফাই সংযোগ চালু করুন। সংযোগ ফিরে পেলে অ্যাপটি স্বয়ংক্রিয়ভাবে চালু হবে।",
                    fontSize = 14.sp,
                    color = SomadhanTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Retry Button (Theme Accent)
                Button(
                    onClick = {
                        if (!isChecking) {
                            isChecking = true
                            scope.launch {
                                val connected = onRetry()
                                delay(600) // Brief animation/check visual feedback
                                isChecking = false
                                if (!connected) {
                                    showQuickToast = true
                                    delay(2500)
                                    showQuickToast = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(52.dp)
                        .testTag("retry_internet_button"),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 6.dp
                    )
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "যাচাই করা হচ্ছে...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "পুনরায় চেষ্টা",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "পুনরায় চেষ্টা করুন",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick feedback banner if retry still has no internet
                AnimatedVisibility(
                    visible = showQuickToast,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(lightAccentBg)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "এখনও সংযোগ পাওয়া যায়নি। ডেটা বা ওয়াই-ফাই চেক করুন।",
                            color = toastTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom Brand watermark
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সমাধান • Somadhan",
                    fontSize = 12.sp,
                    color = SomadhanTextHint,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * [Offline Action Gating ধাপ ৩] Non-blocking, edge-to-edge thin status bar — Strict Offline
 * Block টগল OFF অবস্থায় ফুল-স্ক্রিন [NoInternetOverlay]-এর বদলে এটা দেখানো হয়। শুধু একটা চিকন
 * bar, নিচের UI স্বাভাবিকভাবে ব্যবহারযোগ্য/ক্লিকযোগ্য থাকে (কোনো clickable interceptor নেই,
 * fillMaxSize না) — কারণ এই মোডে ইউজার offline-এও cached ডেটা দেখতে ও ঘোরাঘুরি করতে পারবে,
 * শুধু network-writing action-গুলো আলাদাভাবে (ধাপ ৪-এর guard দিয়ে) block হবে।
 */
@Composable
fun OfflineStatusBanner(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .testTag("offline_status_banner"),
            color = SomadhanTextPrimary
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "ইন্টারনেট নেই",
                    tint = SomadhanOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ইন্টারনেট নেই — শুধু আগের ডেটা দেখা যাচ্ছে",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
