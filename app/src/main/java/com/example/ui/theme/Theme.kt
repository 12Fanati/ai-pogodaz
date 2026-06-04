package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF6366F1),      // Beautiful Indigo
    onPrimary = Color.White,
    secondary = Color(0xFF0EA5E9),    // Sparkling Sky Blue
    onSecondary = Color.White,
    tertiary = Color(0xFFF43F5E),     // Coral Rose
    onTertiary = Color.White,
    background = Color(0xFF080B1A),   // Cosmic SpaceDark
    onBackground = Color(0xFFF9FAFB),
    surface = Color(0xFF0D122E),      // Deep Nebula Blue
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF4F46E5),      // Real Deep Indigo for light mode
    onPrimary = Color.White,
    secondary = Color(0xFF0284C7),    // Real Sky Blue
    onSecondary = Color.White,
    tertiary = Color(0xFFE11D48),     // Real Amber/Coral Rose accent
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9),   // Real slate white/light grey
    onBackground = Color(0xFF0F172A),  // Dark charcoal text
    surface = Color(0xFFFFFFFF),      // Clean white surfaces
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
