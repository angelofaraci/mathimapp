package com.example.proyectofinal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val shapeTokens = DefaultAppShapeTokens

    CompositionLocalProvider(LocalAppShapeTokens provides shapeTokens) {
        MaterialTheme(
            colorScheme = AppLightColorScheme,
            typography = AppTypography,
            shapes = Shapes(
                small = RoundedCornerShape(shapeTokens.field),
                medium = RoundedCornerShape(shapeTokens.button),
                large = RoundedCornerShape(shapeTokens.card)
            ),
            content = content
        )
    }
}
