package com.secure.lovealarm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.min

private val PinkHeartLight = Color(0x50FFB6C1)
private val PinkHeartDark = Color(0x70FFB6C1)

/**
 * Draws a simple heart shape at the given center with the given size.
 */
private fun DrawScope.drawHeart(centerX: Float, centerY: Float, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(centerX, centerY + size * 0.35f)
        cubicTo(
            centerX - size * 0.5f, centerY - size * 0.1f,
            centerX - size * 0.9f, centerY + size * 0.6f,
            centerX, centerY + size * 1.1f
        )
        cubicTo(
            centerX + size * 0.9f, centerY + size * 0.6f,
            centerX + size * 0.5f, centerY - size * 0.1f,
            centerX, centerY + size * 0.35f
        )
        close()
    }
    drawPath(path, color)
}

/**
 * Subtle pink hearts overlay drawn in Compose (no drawable loading).
 * Use on top of the gradient background.
 */
@Composable
fun HeartsOverlay(modifier: Modifier = Modifier.fillMaxSize()) {
    val isDark = isSystemInDarkTheme()
    val heartColor = if (isDark) PinkHeartDark else PinkHeartLight
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val heartSize = min(w, h) * 0.12f

        drawHeart(w * 0.2f, h * 0.25f, heartSize, heartColor)
        drawHeart(w * 0.8f, h * 0.2f, heartSize * 0.9f, heartColor)
        drawHeart(w * 0.15f, h * 0.7f, heartSize * 0.8f, heartColor)
        drawHeart(w * 0.85f, h * 0.65f, heartSize * 0.85f, heartColor)
        drawHeart(w * 0.5f, h * 0.45f, heartSize * 0.7f, heartColor)
    }
}
