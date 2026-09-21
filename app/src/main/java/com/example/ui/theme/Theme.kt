package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = ModernNeonLime,
    onPrimary = ModernMatteBlack,
    primaryContainer = Color(0xFF2A2B33),
    onPrimaryContainer = ModernNeonLime,
    secondary = ModernBrandBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF24252E),
    onSecondaryContainer = Color(0xFFE2E4EC),
    tertiary = ModernNeonLime,
    onTertiary = ModernMatteBlack,
    tertiaryContainer = Color(0xFF1E2810),
    onTertiaryContainer = ModernNeonLime,
    background = Color(0xFF101114),
    onBackground = Color(0xFFF8F9FA),
    surface = Color(0xFF18191E),
    onSurface = Color(0xFFF8F9FA),
    surfaceVariant = Color(0xFF22242B),
    onSurfaceVariant = Color(0xFFA0A3B1),
    outline = Color(0xFF2E313B),
    error = ModernErrorRed,
    errorContainer = ModernErrorContainer,
    onErrorContainer = ModernOnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = ModernMatteBlack,
    onPrimary = Color.White,
    primaryContainer = ModernNeonLime,
    onPrimaryContainer = ModernMatteBlack,
    secondary = ModernBrandBlue,
    onSecondary = Color.White,
    secondaryContainer = ModernBrandBlueLight,
    onSecondaryContainer = ModernBrandBlue,
    tertiary = ModernNeonLime,
    onTertiary = ModernMatteBlack,
    tertiaryContainer = Color(0xFFEBFDC0),
    onTertiaryContainer = ModernMatteBlack,
    background = Color(0xFFF4F5F7),
    onBackground = ModernMatteBlack,
    surface = Color.White,
    onSurface = ModernMatteBlack,
    surfaceVariant = Color(0xFFECEEF2),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE5E7EB),
    error = ModernErrorRed,
    errorContainer = ModernErrorContainer,
    onErrorContainer = ModernOnErrorContainer
)

object AppTheme {
    val isDark: Boolean
        @Composable get() = LocalIsDarkTheme.current

    val canvasBackground: Color
        @Composable get() = if (isDark) Color(0xFF101114) else Color(0xFFF4F5F7)

    val cardBackground: Color
        @Composable get() = if (isDark) Color(0xFF1A1B22) else Color(0xFFFFFFFF)

    val cardBorder: Color
        @Composable get() = if (isDark) Color(0xFF2A2C37) else Color(0xFFE6E9EB)

    val textPrimary: Color
        @Composable get() = if (isDark) Color(0xFFF8F9FA) else Color(0xFF131412)

    val textSecondary: Color
        @Composable get() = if (isDark) Color(0xFFA0A3B1) else Color(0xFF6F7274)

    val textMuted: Color
        @Composable get() = if (isDark) Color(0xFF6B7280) else Color(0xFFA3A6AE)

    val pillBackground: Color
        @Composable get() = if (isDark) Color(0xFF24252E) else Color(0xFFF4F5F7)

    val pillBorder: Color
        @Composable get() = if (isDark) Color(0xFF2E313B) else Color(0xFFE6E9EB)

    val accentLime: Color
        get() = ModernNeonLime

    val accentLimeText: Color
        @Composable get() = if (isDark) ModernNeonLime else Color(0xFF2E7D32)

    val accentGreen: Color
        @Composable get() = if (isDark) ModernNeonLime else Color(0xFF16A34A)

    val brandBlue: Color
        @Composable get() = if (isDark) Color(0xFF7FA2E8) else ModernBrandBlue

    val brandBlueSecondary: Color
        @Composable get() = if (isDark) Color(0xFF5275B8) else ModernBrandBlueSecondary

    val accentPurple: Color
        get() = ModernSoftPurple
}

@Composable
fun MyApplicationTheme(
    themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
