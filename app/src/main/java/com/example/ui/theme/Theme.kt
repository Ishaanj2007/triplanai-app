package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFA78BFA),
    secondary = Color(0xFFC084FC),
    tertiary = Color(0xFFF472B6),
    background = Color(0xFF0F0F12),
    surface = Color(0xFF1A1A22),
    onPrimary = Color(0xFF0F0F12),
    onSecondary = Color.White,
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF6A4CD3),
    secondary = Color(0xFF8B5CF6),
    tertiary = Color(0xFFF472B6),
    background = Color(0xFFF8F7FC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827)
  )

private val DarkTripColors = TripColors(
  background = Color(0xFF0F0F12),
  cardBackground = Color(0xFF1A1A22),
  textDark = Color(0xFFF3F4F6),
  primaryPurple = Color(0xFFA78BFA),
  secondaryPurple = Color(0xFF28233C),
  tertiaryPink = Color(0xFF381F2F),
  mintGreen = Color(0xFF1B3527),
  peachGold = Color(0xFF382B1B),
  softLavender = Color(0xFF27213C),
  borderDark = Color(0xFF333344),
  grayMuted = Color(0xFF9CA3AF),
  grayLight = Color(0xFF22222E),
  isDark = true
)

private val LightTripColors = TripColors(
  background = Color(0xFFF8F7FC),
  cardBackground = Color.White,
  textDark = Color(0xFF111827),
  primaryPurple = Color(0xFF6A4CD3),
  secondaryPurple = Color(0xFFEDE9FE),
  tertiaryPink = Color(0xFFFCE7F3),
  mintGreen = Color(0xFFD1FAE5),
  peachGold = Color(0xFFFEF3C7),
  softLavender = Color(0xFFE9D5FF),
  borderDark = Color(0xFFE5E3EF),
  grayMuted = Color(0xFF6B7280),
  grayLight = Color(0xFFF3F2F8),
  isDark = false
)

private val CoralSunsetTripColors = TripColors(
  background = Color(0xFFFFF9F6),
  cardBackground = Color(0xFFFFFDFC),
  textDark = Color(0xFF3E1F1F),
  primaryPurple = Color(0xFFFF5F6D),
  secondaryPurple = Color(0xFFFFC371),
  tertiaryPink = Color(0xFFFFAB91),
  mintGreen = Color(0xFF80CBC4),
  peachGold = Color(0xFFFFD54F),
  softLavender = Color(0xFFFFCC80),
  borderDark = Color(0xFF6E2828),
  grayMuted = Color(0xFF8D5B5B),
  grayLight = Color(0xFFFFECE3),
  isDark = false
)

@Composable
fun MyApplicationTheme(
  themeMode: Int = 0, // 0 = Auto / System, 1 = Dark Mode, 2 = Light Mode, 3 = Coral Sunset
  content: @Composable () -> Unit,
) {
  val isSystemDark = isSystemInDarkTheme()
  val isDark = when (themeMode) {
    1 -> true
    2 -> false
    3 -> false
    else -> isSystemDark
  }

  val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

  val customColors = when (themeMode) {
    1 -> DarkTripColors
    2 -> LightTripColors
    3 -> CoralSunsetTripColors
    else -> if (isSystemDark) DarkTripColors else LightTripColors
  }

  CompositionLocalProvider(LocalTripColors provides customColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}

