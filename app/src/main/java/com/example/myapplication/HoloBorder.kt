package com.example.myapplication

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * HoloBorder — a dynamic light-reflection border that responds to phone tilt.
 *
 * The highlight always appears on the side the phone tilts toward,
 * exactly like a holographic card catching light.
 *
 * @param tiltX     normalised horizontal tilt  -1f (left) to +1f (right)
 * @param tiltY     normalised vertical tilt    -1f (forward) to +1f (back)
 * @param cornerRadius  corner radius of the border
 * @param strokeWidth   border thickness
 * @param baseAlpha     opacity when phone is flat (0 = invisible, 1 = fully visible)
 */
fun Modifier.holoBorder(
    tiltX: Float,
    tiltY: Float,
    cornerRadius: Dp = 20.dp,
    strokeWidth: Dp = 1.5.dp,
    baseAlpha: Float = 0.55f
): Modifier = this.drawBehind {

    val strokePx = strokeWidth.toPx()
    val radiusPx = cornerRadius.toPx()
    val halfStroke = strokePx / 2f

    // ── Calculate light angle from tilt ──────────────────────────────────────
    // tiltX: -1 = left, +1 = right
    // tiltY: -1 = forward (top of phone toward you), +1 = back
    // We map tilt to an angle in radians where 0 = light from right
    // and π/2 = light from top
    val lightAngle = atan2(-tiltY, tiltX)   // atan2 gives angle in -π..+π

    // Convert to 0..360 degrees for the sweep gradient rotation
    val lightDegrees = Math.toDegrees(lightAngle.toDouble()).toFloat()

    // Tilt magnitude — how much the phone is tilted (0 = flat, 1 = fully tilted)
    val tiltMagnitude = sqrt(tiltX * tiltX + tiltY * tiltY).coerceIn(0f, 1f)

    // Highlight intensity — brighter when tilted more
    val highlightAlpha = (baseAlpha + tiltMagnitude * 0.45f).coerceIn(0f, 1f)

    // ── Build the aurora color stops ──────────────────────────────────────────
    // The highlight is a tight bright arc on the light-source side.
    // The opposite side fades to near-transparent.
    // Colors cycle: purple → cyan → blue → purple (aurora palette)
    val colors = listOf(
        Color(0xFF9B59FF).copy(alpha = highlightAlpha * 0.3f),   // dim purple (shadow side)
        Color(0xFF6C63FF).copy(alpha = highlightAlpha * 0.15f),  // very dim
        Color(0xFF00E5FF).copy(alpha = highlightAlpha * 0.1f),   // almost invisible
        Color(0xFFFFFFFF).copy(alpha = highlightAlpha),           // bright white highlight
        Color(0xFF7C6DFF).copy(alpha = highlightAlpha * 0.9f),   // purple near highlight
        Color(0xFF00BFFF).copy(alpha = highlightAlpha * 0.5f),   // cyan trailing edge
        Color(0xFF9B59FF).copy(alpha = highlightAlpha * 0.3f),   // back to dim
    )

    // ── Draw the border path ─────────────────────────────────────────────────
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left   = halfStroke,
                top    = halfStroke,
                right  = size.width  - halfStroke,
                bottom = size.height - halfStroke,
                cornerRadius = CornerRadius(radiusPx, radiusPx)
            )
        )
    }

    // ── Create sweep gradient centered on the view ────────────────────────────
    // The rotation is driven by lightDegrees so the highlight follows the tilt
    val sweepBrush = Brush.sweepGradient(
        colors = colors,
        center = Offset(size.width / 2f, size.height / 2f)
    )

    // Rotate the canvas so the highlight arc sits on the correct side
    // We offset by -90 because sweepGradient starts at 3 o'clock (0°)
    // but we want 12 o'clock to be "up" (matching phone tilt coordinates)
    with(drawContext.canvas) {
        save()
        rotate(
            degrees = lightDegrees - 90f,
            pivotX  = size.width  / 2f,
            pivotY  = size.height / 2f
        )
        drawPath(
            path  = path,
            brush = sweepBrush,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokePx,
                cap   = StrokeCap.Round,
                join  = StrokeJoin.Round
            )
        )
        restore()
    }

    // ── Secondary glow layer — outer soft halo ────────────────────────────────
    // Only visible when tilted, adds depth
    if (tiltMagnitude > 0.15f) {
        val glowAlpha = (tiltMagnitude * 0.25f).coerceIn(0f, 0.3f)
        val glowPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left   = halfStroke - 3f,
                    top    = halfStroke - 3f,
                    right  = size.width  - halfStroke + 3f,
                    bottom = size.height - halfStroke + 3f,
                    cornerRadius = CornerRadius(radiusPx + 3f, radiusPx + 3f)
                )
            )
        }
        with(drawContext.canvas) {
            save()
            rotate(
                degrees = lightDegrees - 90f,
                pivotX  = size.width  / 2f,
                pivotY  = size.height / 2f
            )
            drawPath(
                path  = glowPath,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color(0xFF7C6DFF).copy(alpha = glowAlpha),
                        Color(0xFFFFFFFF).copy(alpha = glowAlpha * 1.5f),
                        Color(0xFF00E5FF).copy(alpha = glowAlpha),
                        Color.Transparent,
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, size.height / 2f)
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokePx * 3f,
                    cap   = StrokeCap.Round
                )
            )
            restore()
        }
    }
}