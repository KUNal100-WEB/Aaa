package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = IdePrimary,
    onPrimary = Color(0xFF041026),
    primaryContainer = IdePrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = IdeSecondary,
    onSecondary = Color(0xFF041026),
    tertiary = IdeAccentGreen,
    background = IdeBackgroundDark,
    surface = IdeSurface,
    surfaceVariant = IdeSurfaceVariant,
    onBackground = IdeTextPrimary,
    onSurface = IdeTextPrimary,
    onSurfaceVariant = IdeTextSecondary,
    outline = IdeBorder
  )

private val LightColorScheme = DarkColorScheme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  // Keep dark blue theme enabled consistently across devices
  dynamicColor: Boolean = false,
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

