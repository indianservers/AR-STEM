package com.indianservers.ai_stem.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6EDBFF),
    onPrimary = Color(0xFF002A36),
    secondary = Color(0xFF62D6C7),
    tertiary = Color(0xFFFFC857),
    background = Color(0xFF07111F),
    surface = Color(0xFF0E1B2B),
    surfaceVariant = Color(0xFF1B2B3E),
    onBackground = Color(0xFFF3F8FF),
    onSurface = Color(0xFFF3F8FF),
    error = Color(0xFFFF6B6B)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF006B89),
    secondary = Color(0xFF006C62),
    tertiary = Color(0xFF7A5A00),
    background = Color(0xFFF7FBFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE3EDF7),
    error = Color(0xFFB3261E)
)

@Composable
fun AiStemTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
