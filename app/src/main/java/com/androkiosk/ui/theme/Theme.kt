package com.androkiosk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Accent = Color(0xFF78C257)

private val scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = Color(0xFF1C2A16),
    onPrimaryContainer = Accent,
    secondaryContainer = Color(0xFF1A1A1A),
    onSecondaryContainer = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF0D0D0D),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF171717),
    onSurfaceVariant = Color(0xFF9B9B9B),
    outline = Color(0xFF2E2E2E),
    outlineVariant = Color(0xFF1E1E1E),
    error = Color(0xFFE08A82),
    onError = Color(0xFF0A0A0A),
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
fun AndrokioskTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
