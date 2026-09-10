package com.androkiosk.ui.theme

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

private val Light = lightColorScheme(
    primary = Color(0xFF17171A),
    onPrimary = Color(0xFFFAFAF8),
    secondaryContainer = Color(0xFFE8E8E3),
    onSecondaryContainer = Color(0xFF17171A),
    background = Color(0xFFF7F7F5),
    onBackground = Color(0xFF17171A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17171A),
    surfaceVariant = Color(0xFFF0F0EC),
    onSurfaceVariant = Color(0xFF6B6B66),
    outline = Color(0xFFD6D6D0),
    outlineVariant = Color(0xFFE7E7E2),
    error = Color(0xFFA8322A),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFF2F2EF),
    onPrimary = Color(0xFF17171A),
    secondaryContainer = Color(0xFF2A2A2E),
    onSecondaryContainer = Color(0xFFF2F2EF),
    background = Color(0xFF0E0E0E),
    onBackground = Color(0xFFF2F2EF),
    surface = Color(0xFF181819),
    onSurface = Color(0xFFF2F2EF),
    surfaceVariant = Color(0xFF232326),
    onSurfaceVariant = Color(0xFF9C9C97),
    outline = Color(0xFF3A3A3E),
    outlineVariant = Color(0xFF2A2A2E),
    error = Color(0xFFE2857D),
)

private val typography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.tighten(FontWeight.SemiBold),
        titleLarge = base.titleLarge.tighten(FontWeight.SemiBold),
        titleMedium = base.titleMedium.tighten(FontWeight.Medium),
        labelLarge = base.labelLarge.copy(letterSpacing = 0.sp),
    )
}

private fun TextStyle.tighten(weight: FontWeight) = copy(fontWeight = weight, letterSpacing = (-0.2).sp)

@Composable
fun AndrokioskTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = typography,
        content = content,
    )
}
