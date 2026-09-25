package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary

/**
 * ===== Somadhan Bottom-Slide Dialog Motion =====
 *
 * Drop-in replacements for Compose's stock `Dialog` and Material3 `AlertDialog`.
 * The app previously had ~117+ popups across ~39 files that opened with a
 * center fade/scale (the stock Compose/Material3 default). Product decision:
 * every such popup across user/solver/admin should instead read as a bottom
 * sheet - sliding up smoothly from the bottom edge of the screen - to match
 * the popups that already used ModalBottomSheet (notifications, photo
 * upload, withdrawal details, merchant payment, etc.).
 *
 * [BottomSlideDialog] and [BottomSlideAlertDialog] mirror the exact named
 * parameters of the composables they replace, so existing call sites switch
 * over by only renaming the function call - no other logic changes.
 *
 * This file is additive only - it does not modify any existing component.
 */

private const val SLIDE_ENTER_MS = 320
private const val SLIDE_EXIT_MS = 220

/**
 * Drop-in replacement for [androidx.compose.ui.window.Dialog].
 * Same signature; anchors [content] to the bottom of the screen and slides
 * it up on entry instead of the stock center-appear behavior.
 */
@Composable
fun BottomSlideDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(SLIDE_ENTER_MS, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(SLIDE_ENTER_MS)),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(SLIDE_EXIT_MS)
                ) + fadeOut(tween(SLIDE_EXIT_MS))
            ) {
                content()
            }
        }
    }
}

/**
 * Drop-in replacement for Material3 `AlertDialog`. Same named parameters
 * (title, text, icon, confirmButton, dismissButton, properties, colors...),
 * but renders as a bottom-anchored sheet-style card with a grabber handle,
 * rounded top corners, and a slide-up-from-bottom entrance - instead of the
 * stock centered fade/scale card.
 */
@Composable
fun BottomSlideAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    containerColor: Color = SomadhanCardBg,
    iconContentColor: Color = SomadhanTextPrimary,
    titleContentColor: Color = SomadhanTextPrimary,
    textContentColor: Color = SomadhanTextSecondary,
    tonalElevation: Dp = 0.dp,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false)
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(SLIDE_ENTER_MS, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(SLIDE_ENTER_MS)),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(SLIDE_EXIT_MS)
                ) + fadeOut(tween(SLIDE_EXIT_MS))
            ) {
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    shape = shape,
                    color = containerColor,
                    tonalElevation = tonalElevation,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        // Grabber handle so the card visually reads as a sheet
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 16.dp)
                                .size(width = 36.dp, height = 4.dp)
                                .background(SomadhanDivider, RoundedCornerShape(2.dp))
                        )

                        if (icon != null) {
                            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                                    icon()
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (title != null) {
                            CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                                ProvideTextStyle(value = MaterialTheme.typography.headlineSmall) {
                                    title()
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (text != null) {
                            CompositionLocalProvider(LocalContentColor provides textContentColor) {
                                ProvideTextStyle(value = MaterialTheme.typography.bodyMedium) {
                                    text()
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (dismissButton != null) {
                                dismissButton()
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            confirmButton()
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
