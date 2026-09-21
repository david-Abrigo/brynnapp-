package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Exact User-Specified Light Theme Palette
// Verde lima principal: #CFF35C
val ModernNeonLime = Color(0xFFCFF35C)
val ModernLimeVolt = ModernNeonLime
val ModernNeonLimeContainer = Color(0xFFEBFDC0)
val ModernOnNeonLime = Color(0xFF131412) // Near Black

// Azul Tecnológico / Profundo de Marca: #35589A (Combinación con Verde Lima)
val ModernBrandBlue = Color(0xFF35589A)
val ModernBrandBlueLight = Color(0xFFEBF0FA)
val ModernBrandBlueSecondary = Color(0xFF4A6FA5)

// Morado / Lavanda: #B2A4F0
val ModernSoftPurple = Color(0xFFB2A4F0)
val ModernSoftPurpleContainer = Color(0xFFECE7FE)

// Textos: Principal Near Black #131412, Secundario Cool Gray #A3A6AE, Gris Oscuro #6F7274
val ModernMatteBlack = Color(0xFF131412)
val ModernDeepCharcoal = Color(0xFF1C1D21)
val ModernHeroSubtext = Color(0xFFA3A6AE)

// Canvas & Superficies: Fondo #F4F5F7, Tarjetas #FFFFFF, Bordes #E6E9EB
val ModernCanvasBackground = Color(0xFFF4F5F7)
val ModernSurfaceLight = Color(0xFFFFFFFF)
val ModernSurfaceVariantLight = Color(0xFFF1F3F5)
val ModernOnSurfaceLight = Color(0xFF131412)
val ModernOnSurfaceVariantLight = Color(0xFF6F7274)
val ModernSubtleTextLight = Color(0xFFA3A6AE)
val ModernBorderLight = Color(0xFFE6E9EB)
val ModernCardBackground = Color(0xFFFFFFFF)
val ModernPillBackground = Color(0xFFF1F3F5)

// Status & Indicators
val ModernStatusGreen = Color(0xFFC6F432)
val ModernSuccessGreen = Color(0xFF16A34A)
val ModernErrorRed = Color(0xFFFF3B30)
val ModernErrorContainer = Color(0xFFFFE5E5)
val ModernOnErrorContainer = Color(0xFF990000)

// Aliases for seamless design system integration across existing components
val SleekBackgroundLight = ModernCanvasBackground
val SleekSurfaceLight = ModernSurfaceLight
val SleekSurfaceVariantLight = ModernSurfaceVariantLight
val SleekOnSurfaceLight = ModernOnSurfaceLight
val SleekOnSurfaceVariantLight = ModernOnSurfaceVariantLight
val SleekSubtleTextLight = ModernSubtleTextLight
val SleekBorderLight = ModernBorderLight

val SleekMidnightNavy = ModernMatteBlack
val SleekHeroSubtext = ModernHeroSubtext
val SleekPrimaryBlue = ModernMatteBlack
val SleekPrimaryContainer = ModernNeonLime
val SleekOnPrimaryContainer = ModernOnNeonLime
val SleekSecondaryContainer = ModernSurfaceVariantLight
val SleekOnSecondaryContainer = ModernMatteBlack

val SleekStatusGreen = ModernNeonLime
val SleekSuccessGreen = ModernSuccessGreen
val SleekErrorRed = ModernErrorRed
val SleekErrorContainer = ModernErrorContainer
val SleekOnErrorContainer = ModernOnErrorContainer

// Dark Theme Variants
val SleekBackgroundDark = Color(0xFF0F0F11)
val SleekSurfaceDark = Color(0xFF18181B)
val SleekSurfaceVariantDark = Color(0xFF232328)
val SleekOnSurfaceDark = Color(0xFFF4F4F6)
val SleekOnSurfaceVariantDark = Color(0xFFA1A1AA)
val SleekBorderDark = Color(0xFF2C2C32)

// Legacy alias compatibility
val YapePurplePrimary = ModernNeonLime
val YapePurpleDark = ModernMatteBlack
val YapePurpleLight = ModernNeonLime
val YapePurpleContainer = ModernNeonLimeContainer
val YapeOnPurpleContainer = ModernOnNeonLime

val YapeCyanAccent = ModernNeonLime
val YapeCyanDark = ModernMatteBlack
val YapeCyanContainer = ModernSurfaceVariantLight
val YapeOnCyanContainer = ModernMatteBlack

val YapeGold = Color(0xFFFFB800)
val YapeGoldContainer = Color(0xFFFFF4D4)

val BackgroundLight = SleekBackgroundLight
val SurfaceLight = SleekSurfaceLight
val SurfaceVariantLight = SleekSurfaceVariantLight
val OnSurfaceLight = SleekOnSurfaceLight
val OnSurfaceVariantLight = SleekOnSurfaceVariantLight

val BackgroundDark = SleekBackgroundDark
val SurfaceDark = SleekSurfaceDark
val SurfaceVariantDark = SleekSurfaceVariantDark
val OnSurfaceDark = SleekOnSurfaceDark
val OnSurfaceVariantDark = SleekOnSurfaceVariantDark
val SuccessGreen = SleekSuccessGreen
val SuccessGreenContainer = Color(0xFFDCFCE7)
val CardBorderLight = SleekBorderLight
val CardBorderDark = SleekBorderDark

// Paleta Personalizada Guardada: Pasteles & Negro Puro
val PastelPinkLavenderStart = Color(0xFFF3B6E8)
val PastelPinkLavenderEnd = Color(0xFFE39DF5)
val PastelSkyBlueStart = Color(0xFFB3EEF8)
val PastelSkyBlueEnd = Color(0xFFC8D9FA)
val PastelButterYellow = Color(0xFFFFF3A3)
val PastelSoftLime = Color(0xFFF3FDB8)
val PureBlack = Color(0xFF000000)

val PastelGradientColors = listOf(
    PastelSkyBlueEnd,
    PastelSkyBlueStart,
    PastelButterYellow,
    PastelSoftLime
)

