package com.nothing.vault.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

private val BackgroundGradientColors = listOf(
    Color(0xFF0D0D0D),
    Color(0xFF161616),
    Color(0xFF0D0D0D)
)

private val BackgroundBrush = Brush.verticalGradient(BackgroundGradientColors)

@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF6C63FF)
) {
    val accentColorOrb = remember { accentColor.copy(alpha = 0.06f) }
    val tealColor = remember { Color(0xFF4ECDC4).copy(alpha = 0.04f) }
    val accentColorLine = remember { accentColor.copy(alpha = 0.03f) }

    Canvas(modifier = modifier) {
        drawRect(brush = BackgroundBrush, size = size)

        drawGlassOrb(size, 0.3f, 0.4f, 40f, accentColorOrb)
        drawGlassOrb(size, 0.7f, 0.6f, 50f, tealColor)
        drawGlassOrb(size, 0.5f, 0.8f, 35f, accentColorLine)
    }
}

private fun DrawScope.drawGlassOrb(
    size: androidx.compose.ui.geometry.Size,
    centerX: Float,
    centerY: Float,
    baseRadius: Float,
    color: Color
) {
    val cx = size.width * centerX
    val cy = size.height * centerY
    val radius = baseRadius * density

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = 0f)),
            center = Offset(cx, cy),
            radius = radius
        ),
        radius = radius,
        center = Offset(cx, cy)
    )
}
