package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

data class TripColors(
    val background: Color,
    val textDark: Color,
    val primaryPurple: Color,
    val secondaryPurple: Color,
    val tertiaryPink: Color,
    val mintGreen: Color,
    val peachGold: Color,
    val softLavender: Color,
    val borderDark: Color,
    val grayMuted: Color,
    val grayLight: Color,
    val cardBackground: Color = Color.White,
    val isDark: Boolean = false
)

val LocalTripColors = staticCompositionLocalOf {
    TripColors(
        background = Color(0xFFF8F7FC),
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
        cardBackground = Color.White
    )
}

val ArtBackground: Color @Composable get() = LocalTripColors.current.background
val ArtTextDark: Color @Composable get() = LocalTripColors.current.textDark
val ArtPrimaryPurple: Color @Composable get() = LocalTripColors.current.primaryPurple
val ArtSecondaryPurple: Color @Composable get() = LocalTripColors.current.secondaryPurple
val ArtTertiaryPink: Color @Composable get() = LocalTripColors.current.tertiaryPink
val ArtMintGreen: Color @Composable get() = LocalTripColors.current.mintGreen
val ArtPeachGold: Color @Composable get() = LocalTripColors.current.peachGold
val ArtSoftLavender: Color @Composable get() = LocalTripColors.current.softLavender
val ArtBorderDark: Color @Composable get() = LocalTripColors.current.borderDark
val ArtGrayMuted: Color @Composable get() = LocalTripColors.current.grayMuted
val ArtGrayLight: Color @Composable get() = LocalTripColors.current.grayLight
val ArtCardBackground: Color @Composable get() = LocalTripColors.current.cardBackground
