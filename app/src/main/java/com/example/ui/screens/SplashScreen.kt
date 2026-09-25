package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun SplashScreen(
    isSessionRestored: Boolean = true,
    onNavigateNext: () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    var hasNavigated by remember { androidx.compose.runtime.mutableStateOf(false) }

    val triggerNavigate = remember(onNavigateNext) {
        {
            if (!hasNavigated) {
                hasNavigated = true
                onNavigateNext()
            }
        }
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(350))
        delay(500)
        triggerNavigate()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SomadhanBg)
            .clickable { triggerNavigate() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alpha.value)
        ) {
            Image(
                painter = painterResource(id = R.drawable.somadhan_app_icon_1786820904533),
                contentDescription = "সমাধান লোগো",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "সমাধান",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "বাংলাদেশ-কেন্দ্রিক সমস্যা সমাধান মার্কেটপ্লেস",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = SomadhanTextHint
            )
        }
    }
}
