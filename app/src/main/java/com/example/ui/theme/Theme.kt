package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val CyberCyanPrimary = Color(0xFF00E5FF)
val CyberCyanDark = Color(0xFF00838F)
val DarkBackground = Color(0xFF0B0F19)
val DarkSurface = Color(0xFF111827)
val StatusSuccess = Color(0xFF10B981)
val StatusRunning = Color(0xFF06B6D4)
val StatusFailure = Color(0xFFEF4444)
val StatusWarning = Color(0xFFF59E0B)

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyanPrimary,
    secondary = CyberCyanDark,
    tertiary = Color(0xFF6366F1),
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9)
)

@Composable
fun LocalAgentTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
