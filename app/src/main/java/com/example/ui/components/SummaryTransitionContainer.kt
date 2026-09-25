package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SummaryTransitionContainer
 *
 * Provides a brief smooth app logo loading transition on a clean background,
 * and then smoothly animates the summary view up from the bottom (Slide Up Animation).
 */
@Composable
fun SummaryTransitionContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isReady by remember { mutableStateOf(false) }
    val logoScale = remember { Animatable(0.85f) }
    val logoAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(280, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1.06f, animationSpec = tween(380, easing = FastOutSlowInEasing))
            logoScale.animateTo(1f, animationSpec = tween(220, easing = FastOutSlowInEasing))
        }
        delay(650)
        isReady = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SomadhanBg)
    ) {
        if (!isReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SomadhanBg),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.somadhan_app_icon_1786820904533),
                        contentDescription = "সমাধান লোগো",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "সমাধান",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CircularProgressIndicator(
                        color = SomadhanOrange,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isReady,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 350)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            content()
        }
    }
}
