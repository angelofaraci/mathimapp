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
    val pill: Dp
)

val DefaultAppShapeTokens = AppShapeTokens(
    card = 28.dp,
    button = 20.dp,
    field = 18.dp,
    pill = 999.dp
)

internal val LocalAppShapeTokens = staticCompositionLocalOf { DefaultAppShapeTokens }

object AppThemeDefaults {
    val shapes: AppShapeTokens
        @Composable get() = LocalAppShapeTokens.current
}
