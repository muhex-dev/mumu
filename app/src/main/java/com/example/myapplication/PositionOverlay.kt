package com.example.myapplication

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private val AccentColor  = Color(0xFF7C6DFF)
private val HandleColor  = Color(0xEEFFFFFF)
private val OverlayColor = Color(0x44000000)

@Stable
class PositioningState(
    topOffsetPx: Float = 0f,
    bottomOffsetPx: Float = 0f,
    visible: Boolean = false
) {
    var topOffsetPx by mutableStateOf(topOffsetPx)
    var bottomOffsetPx by mutableStateOf(bottomOffsetPx)
    var isVisible by mutableStateOf(visible)
}

@Composable
fun PositionOverlay(
    state: PositioningState,
    onTopDrag: (Float) -> Unit,
    onBottomDrag: (Float) -> Unit,
    onDone: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (state.isVisible) 1f else 0f,
        animationSpec = tween(250),
        label = "alpha"
    )

    if (alpha <= 0f && !state.isVisible) return

    val density = androidx.compose.ui.platform.LocalDensity.current.density

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(OverlayColor)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, _ -> },
                    onDragEnd = { onDone() }
                )
            }
            .noRippleClick { onDone() }
    ) {
        // ── Top handle ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .offset(y = (state.topOffsetPx / density).dp)
                .fillMaxWidth()
                .height(60.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dy -> onTopDrag(dy) }
                },
            contentAlignment = Alignment.Center
        ) {
            DragHandlePill()
        }

        // ── Bottom handle ────────────────────────────────────────────────────
        // Positioning it so the pill is centered exactly at state.bottomOffsetPx
        Box(
            modifier = Modifier
                .offset(y = (state.bottomOffsetPx / density).dp - 30.dp)
                .fillMaxWidth()
                .height(60.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dy -> onBottomDrag(dy) }
                },
            contentAlignment = Alignment.Center
        ) {
            DragHandlePill()
        }
    }
}

@Composable
private fun DragHandlePill() {
    // Sleek "pill-in-pill" tactile handle
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(72.dp)
            .height(10.dp)
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(5.dp))
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .background(Color.White, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun Modifier.noRippleClick(onClick: () -> Unit): Modifier {
    val interactionSource = remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    return this.then(
        Modifier.clickable(
            indication = null,
            interactionSource = interactionSource,
            onClick = onClick
        )
    )
}
