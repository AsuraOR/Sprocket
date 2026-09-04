package com.example.sprocket.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class SprocketColorPalette(
    val bg: Color,
    val ink: Color,
    val surface: Color,
    val accent: Color,
    val onAccent: Color,
    val muted: Color,
    val neutral300: Color,
    val neutral400: Color,
    val divider: Color,
    val lightDivider: Color,
    val overdueRowBg: Color,
    val isDark: Boolean
)

// Light Palette (Default) — High WCAG AA Contrast (>6.0:1)
val SprocketLightColors = SprocketColorPalette(
    bg = Color(0xFFF3F2F2),
    ink = Color(0xFF201E1D),
    surface = Color(0xFFEAE9E9),
    accent = Color(0xFFEC3013),
    onAccent = Color(0xFFF3F2F2),
    muted = Color(0xFF595555), // Enhanced from #7D7979 -> 6.4:1 contrast on #F3F2F2
    neutral300 = Color(0xFFD7D3D3),
    neutral400 = Color(0xFF595555), // Enhanced for healthy counters and secondary labels
    divider = Color(0x50201E1D),
    lightDivider = Color(0x38201E1D),
    overdueRowBg = Color(0x14EC3013),
    isDark = false
)

// Dark Palette ("Inverted Instrument" Option 1c)
val SprocketDarkColors = SprocketColorPalette(
    bg = Color(0xFF1B1918),
    ink = Color(0xFFF3F2F2),
    surface = Color(0xFF282524),
    accent = Color(0xFFFF3B1F),
    onAccent = Color(0xFFF3F2F2),
    muted = Color(0xFFA5A0A0), // High contrast on dark ground (>6.5:1)
    neutral300 = Color(0xFF423E3D),
    neutral400 = Color(0xFFA5A0A0),
    divider = Color(0x50F3F2F2),
    lightDivider = Color(0x30F3F2F2),
    overdueRowBg = Color(0x28EC3013),
    isDark = true
)

// Static constants preserved for non-theme direct references
val SprocketDarkBg = Color(0xFF1B1918)
val SprocketDarkSurface = Color(0xFF282524)
val SprocketDarkDivider = Color(0x50F3F2F2)

val LocalSprocketColors = staticCompositionLocalOf { SprocketLightColors }

val SprocketBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSprocketColors.current.bg

val SprocketInk: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSprocketColors.current.ink

val SprocketSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSprocketColors.current.surface

val SprocketAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSprocketColors.current.accent

val SprocketOnAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSprocketColors.current.onAccent

val SprocketMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSprocketColors.current.muted

val SprocketNeutral300: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSprocketColors.current.neutral300

val SprocketNeutral400: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSprocketColors.current.neutral400

val SprocketDivider: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSprocketColors.current.divider

val SprocketLightDivider: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSprocketColors.current.lightDivider

val SprocketOverdueRowBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSprocketColors.current.overdueRowBg
