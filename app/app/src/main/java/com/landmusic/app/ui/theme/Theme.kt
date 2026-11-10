package com.landmusic.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val LandRed = Color(0xFFFA2D48)
val LandTextPrimary = Color(0xFF1D1D1F)
val LandTextSecondary = Color(0xFF6E6E73)
val LandBackground = Color(0xFFFFFFFF)
val LandSurfaceVariant = Color(0xFFF5F5F7)

private val LandColorScheme = lightColorScheme(
    primary = LandRed,
    onPrimary = Color.White,
    primaryContainer = LandRed.copy(alpha = 0.12f),
    onPrimaryContainer = LandRed,
    secondary = LandTextSecondary,
    onSecondary = Color.White,
    background = LandBackground,
    onBackground = LandTextPrimary,
    surface = LandBackground,
    onSurface = LandTextPrimary,
    surfaceVariant = LandSurfaceVariant,
    onSurfaceVariant = LandTextSecondary,
    outline = Color(0xFFD2D2D7),
)

val LandTypography = Typography(
    // Apple Music 风格大标题
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
)

@Composable
fun LandMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LandColorScheme,
        typography = LandTypography,
        content = content,
    )
}
