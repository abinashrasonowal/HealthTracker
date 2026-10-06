package com.healthtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// One calm accent (teal) on soft neutral surfaces.
private val LightColors = lightColorScheme(
    primary = Color(0xFF00696E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9ECEE),
    onPrimaryContainer = Color(0xFF002022),
    secondaryContainer = Color(0xFFDCE6E6),
    onSecondaryContainer = Color(0xFF151F1F),
    background = Color(0xFFF8FAF9),
    onBackground = Color(0xFF181C1C),
    surface = Color(0xFFF8FAF9),
    onSurface = Color(0xFF181C1C),
    surfaceContainerLow = Color(0xFFF1F4F3),
    surfaceContainer = Color(0xFFEBEFEE),
    surfaceContainerHigh = Color(0xFFE5E9E8),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7979),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D4D8),
    onPrimary = Color(0xFF003739),
    primaryContainer = Color(0xFF004F53),
    onPrimaryContainer = Color(0xFFB9ECEE),
    secondaryContainer = Color(0xFF3F4948),
    onSecondaryContainer = Color(0xFFDAE5E4),
    background = Color(0xFF101414),
    onBackground = Color(0xFFE0E3E2),
    surface = Color(0xFF101414),
    onSurface = Color(0xFFE0E3E2),
    surfaceContainerLow = Color(0xFF181C1C),
    surfaceContainer = Color(0xFF1C2020),
    surfaceContainerHigh = Color(0xFF262B2A),
    onSurfaceVariant = Color(0xFFBEC8C8),
    outline = Color(0xFF889392),
    error = Color(0xFFFFB4AB),
)

// Slightly larger than Material defaults so older family members can read it comfortably.
private val base = Typography()
private val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = base.labelLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
    labelMedium = base.labelMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
fun HealthTrackerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
