package com.muhex.mumu

import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A sophisticated glass-style border modifier where the gradient highlights
 * rotate around the perimeter while the border shape remains perfectly static.
 *
 * @param cornerRadius The radius of the border corners (Default: 32dp).
 * @param strokeWidth The thickness of the border line.
 * @param baseAlpha The maximum opacity of the white segments.
 * @param durationMillis The time for one full rotation of the highlights.
 */
fun Modifier.holoBorder(
    tiltX: Float = 0f, // Signature compatibility
    tiltY: Float = 0f, // Signature compatibility
    cornerRadius: Dp = 32.dp,
    strokeWidth: Dp = 1.5.dp,
    baseAlpha: Float = 0.35f,
    durationMillis: Int = 6000,
    color: Color = Color.White
): Modifier = this.composed {
    
    val infiniteTransition = rememberInfiniteTransition(label = "HoloBorderRotation")
    
    // Smoothly animate the rotation of the gradient colors
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GradientRotation"
    )

    this.drawBehind {
        val strokePx = strokeWidth.toPx()
        val radiusPx = cornerRadius.toPx()
        val halfStroke = strokePx / 2f

        // ── Color Definitions ────────────────────────────────────────────────
        val highlight = color.copy(alpha = baseAlpha).toArgb()
        val trans = Color.Transparent.toArgb()

        // Quadrants setup: Smoothly interpolating between white and transparent segments
        val colors = intArrayOf(trans, highlight, trans, highlight, trans)
        val stops = floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)

        // ── Shader Logic ─────────────────────────────────────────────────────
        // We use a native SweepGradient and a LocalMatrix to rotate ONLY the 
        // colors. This ensures the rectangular border shape doesn't move.
        val shader = SweepGradient(size.width / 2f, size.height / 2f, colors, stops)
        val matrix = Matrix()
        matrix.setRotate(rotation, size.width / 2f, size.height / 2f)
        shader.setLocalMatrix(matrix)

        val brush = ShaderBrush(shader)

        // ── Path Construction ────────────────────────────────────────────────
        val borderPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = halfStroke,
                    top = halfStroke,
                    right = size.width - halfStroke,
                    bottom = size.height - halfStroke,
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
            )
        }

        // ── Rendering ────────────────────────────────────────────────────────
        drawPath(
            path = borderPath,
            brush = brush,
            style = Stroke(
                width = strokePx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
