package com.example.proyectofinal.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AppShapeTokens(
    val card: Dp,
    val button: Dp,
    val field: Dp,
    val pill: Dp,
    val checkbox: Dp,
    val iconBox: Dp,
    val socialButton: Dp,
    val stepSegment: Dp
)

val DefaultAppShapeTokens = AppShapeTokens(
    card = 18.dp,
    button = 16.dp,
    field = 15.dp,
    pill = 999.dp,
    checkbox = 7.dp,
    iconBox = 13.dp,
    socialButton = 14.dp,
    stepSegment = 999.dp
)

internal val LocalAppShapeTokens = staticCompositionLocalOf { DefaultAppShapeTokens }

object AppThemeDefaults {
    val shapes: AppShapeTokens
        @Composable get() = LocalAppShapeTokens.current
}
