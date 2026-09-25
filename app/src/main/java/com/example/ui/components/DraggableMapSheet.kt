package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SomadhanBorder
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class MapSheetStateValue {
    COLLAPSED,  // Minimized sticky mini-bar
    DEFAULT,    // Standard half-card height (~42-45% of screen)
    EXPANDED    // Max up to 78% screen height
}

/**
 * Reusable Google Maps-style 3-state draggable bottom sheet.
 * Fluidly draggable between Collapsed (sticky bottom bar), Default (~44%), and Expanded (78% max of map screen).
 * Smooth transition where the compact bar is shown prominently at the bottom when collapsed, and clean detailed content is shown when expanded.
 */
@Composable
fun DraggableMapSheet(
    modifier: Modifier = Modifier,
    initialState: MapSheetStateValue = MapSheetStateValue.DEFAULT,
    collapsedBarHeight: Dp = 108.dp,
    maxHeightFraction: Float = 0.80f,
    defaultHeightFraction: Float = 0.46f,
    collapsedBar: @Composable (sheetState: MapSheetStateValue, onExpandClick: () -> Unit) -> Unit,
    expandedContent: @Composable (sheetState: MapSheetStateValue) -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    val totalScreenHeightDp = configuration.screenHeightDp.dp
    val totalHeightPx = with(density) { totalScreenHeightDp.toPx() }
    val collapsedPx = with(density) { collapsedBarHeight.toPx() }
    val expandedPx = totalHeightPx * maxHeightFraction
    val defaultPx = (totalHeightPx * defaultHeightFraction).coerceIn(
        with(density) { 280.dp.toPx() },
        expandedPx
    )

    val initialHeightPx = when (initialState) {
        MapSheetStateValue.COLLAPSED -> collapsedPx
        MapSheetStateValue.DEFAULT -> defaultPx
        MapSheetStateValue.EXPANDED -> expandedPx
    }

    val heightAnim = remember { Animatable(initialHeightPx) }
    var currentSheetState by remember { mutableStateOf(initialState) }

    // Keep currentSheetState synced with anim target or position
    val updateStateFromPosition = { targetPos: Float ->
        val distToCollapsed = abs(targetPos - collapsedPx)
        val distToDefault = abs(targetPos - defaultPx)
        val distToExpanded = abs(targetPos - expandedPx)
        if (distToCollapsed <= distToDefault && distToCollapsed <= distToExpanded) {
            MapSheetStateValue.COLLAPSED
        } else if (distToExpanded <= distToDefault && distToExpanded <= distToCollapsed) {
            MapSheetStateValue.EXPANDED
        } else {
            MapSheetStateValue.DEFAULT
        }
    }

    val snapToState: (MapSheetStateValue) -> Unit = { targetState ->
        currentSheetState = targetState
        val targetPx = when (targetState) {
            MapSheetStateValue.COLLAPSED -> collapsedPx
            MapSheetStateValue.DEFAULT -> defaultPx
            MapSheetStateValue.EXPANDED -> expandedPx
        }
        coroutineScope.launch {
            heightAnim.animateTo(
                targetValue = targetPx,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        }
    }

    val onDragStoppedAction: (Float) -> Unit = { velocity ->
        val targetState = if (velocity < -600f) {
            // Dragged/flung upwards
            if (heightAnim.value < defaultPx) MapSheetStateValue.DEFAULT else MapSheetStateValue.EXPANDED
        } else if (velocity > 600f) {
            // Dragged/flung downwards
            if (heightAnim.value > defaultPx) MapSheetStateValue.DEFAULT else MapSheetStateValue.COLLAPSED
        } else {
            updateStateFromPosition(heightAnim.value)
        }
        snapToState(targetState)
    }

    // Draggable gesture state
    val draggableState = rememberDraggableState { delta ->
        val newHeight = (heightAnim.value - delta).coerceIn(collapsedPx, expandedPx)
        coroutineScope.launch {
            heightAnim.snapTo(newHeight)
        }
    }

    val isCollapsed = heightAnim.value <= collapsedPx + with(density) { 16.dp.toPx() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(with(density) { heightAnim.value.toDp() })
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStopped = { v -> onDragStoppedAction(v) }
            )
            .testTag("draggable_map_sheet"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { v -> onDragStoppedAction(v) }
                )
        ) {
            // Top Grabber Handle Bar (Always draggable & clickable to cycle states)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SomadhanBorder.copy(alpha = 0.9f))
                        .clickable {
                            val next = when (currentSheetState) {
                                MapSheetStateValue.COLLAPSED -> MapSheetStateValue.DEFAULT
                                MapSheetStateValue.DEFAULT -> MapSheetStateValue.EXPANDED
                                MapSheetStateValue.EXPANDED -> MapSheetStateValue.COLLAPSED
                            }
                            snapToState(next)
                        }
                )
            }

            if (isCollapsed) {
                // COLLAPSED STATE: Draggable bottom bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    collapsedBar(currentSheetState) {
                        snapToState(MapSheetStateValue.DEFAULT)
                    }
                }
            } else {
                // EXPANDED / DEFAULT STATE: Full detailed content (draggable from anywhere)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    expandedContent(currentSheetState)
                }
            }
        }
    }
}
