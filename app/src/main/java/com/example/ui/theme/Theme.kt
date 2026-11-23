package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.preferences.ThemeMode

private val BrowserXDarkColorScheme = darkColorScheme(
    primary = OperaCrimson,
    onPrimary = Color.White,
    primaryContainer = OperaCrimsonContainer,
    onPrimaryContainer = OnOperaCrimsonContainer,
    secondary = ElectricCyan,
    onSecondary = OnElectricCyan,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = OnCyanContainer,
    tertiary = VerifiedEmerald,
    onTertiary = Color(0xFF022C22),
    background = ObsidianVoid,
    onBackground = CrispSnow,
    surface = ObsidianSurface,
    onSurface = CrispSnow,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = CyberSilver,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF1F273B),
    error = DangerCoral,
    onError = Color.White,
    errorContainer = DangerCoralBg,
    onErrorContainer = Color(0xFFFECACA)
)

private val BrowserXLightColorScheme = lightColorScheme(
    primary = OperaCrimsonDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4E7),
    onPrimaryContainer = Color(0xFF4A0A14),
    secondary = DeepTealPrimary,
    onSecondary = Color.White,
    secondaryContainer = DeepTealContainer,
    onSecondaryContainer = OnDeepTealContainer,
    tertiary = Color(0xFF047857),
    onTertiary = Color.White,
    background = AlabasterBg,
    onBackground = InkCharcoal,
    surface = AlabasterSurface,
    onSurface = InkCharcoal,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = AlabasterOutline,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val BrowserXShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BrowserXTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (isDark) BrowserXDarkColorScheme else BrowserXLightColorScheme,
        typography = BrowserXTypography,
        shapes = BrowserXShapes,
        content = content
    )
}
