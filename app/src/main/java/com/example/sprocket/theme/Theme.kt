package com.example.sprocket.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SprocketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) SprocketDarkColors else SprocketLightColors
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.surface,
            onPrimaryContainer = colors.ink,
            secondary = colors.ink,
            onSecondary = colors.bg,
            background = colors.bg,
            onBackground = colors.ink,
            surface = colors.bg,
            onSurface = colors.ink,
            surfaceVariant = colors.surface,
            onSurfaceVariant = colors.muted,
            outline = colors.divider
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.surface,
            onPrimaryContainer = colors.ink,
            secondary = colors.ink,
            onSecondary = colors.bg,
            background = colors.bg,
            onBackground = colors.ink,
            surface = colors.bg,
            onSurface = colors.ink,
            surfaceVariant = colors.surface,
            onSurfaceVariant = colors.muted,
            outline = colors.divider
        )
    }

    CompositionLocalProvider(LocalSprocketColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SprocketTypography,
            content = content
        )
    }
}

fun Modifier.sprocketTopBorder(color: Color, strokeWidth: Dp = 1.dp): Modifier = drawBehind {
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = strokeWidth.toPx()
    )
}

fun Modifier.sprocketBottomBorder(color: Color, strokeWidth: Dp = 1.dp): Modifier = drawBehind {
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = strokeWidth.toPx()
    )
}

fun Modifier.sprocketLeftBorder(color: Color, strokeWidth: Dp = 1.dp): Modifier = drawBehind {
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(0f, size.height),
        strokeWidth = strokeWidth.toPx()
    )
}

fun Modifier.sprocketRightBorder(color: Color, strokeWidth: Dp = 1.dp): Modifier = drawBehind {
    drawLine(
        color = color,
        start = Offset(size.width, 0f),
        end = Offset(size.width, size.height),
        strokeWidth = strokeWidth.toPx()
    )
}
