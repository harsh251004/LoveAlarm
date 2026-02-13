package com.secure.lovealarm.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ClockBlue80,
    secondary = ClockBlueGrey80,
    tertiary = ClockTeal80
)

private val LightColorScheme = lightColorScheme(
    primary = ClockBlue40,
    secondary = ClockBlueGrey40,
    tertiary = ClockTeal40
)

@Composable
fun LoveAlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledDark: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        darkTheme && amoledDark -> {
            val base = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    dynamicDarkColorScheme(context)
                } catch (_: Throwable) {
                    DarkColorScheme
                }
            } else {
                DarkColorScheme
            }
            base.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceVariant = Color(0xFF1A1A1A),
                surfaceContainerHighest = Color(0xFF1F1F1F),
                surfaceContainerHigh = Color(0xFF1C1C1C),
                surfaceContainer = Color(0xFF181818),
                surfaceContainerLow = Color(0xFF141414),
                surfaceDim = Color(0xFF0D0D0D),
                surfaceBright = Color(0xFF141414),
                outlineVariant = Color(0xFF2A2A2A),
                outline = Color(0xFF444444)
            )
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            try {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (_: Throwable) {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}