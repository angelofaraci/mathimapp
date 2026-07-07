package com.example.proyectofinal.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AppThemeTokensTest {

    @Test
    fun lightColorSchemeMatchesBrandedFoundationPalette() {
        assertEquals(Color(0xFFF2654B), AppLightColorScheme.primary)
        assertEquals(Color(0xFF0E9E8E), AppLightColorScheme.secondary)
        assertEquals(Color(0xFFF0526A), AppLightColorScheme.error)
        assertEquals(Color(0xFFFBF6EF), AppLightColorScheme.background)
        assertEquals(Color(0xFFFFFFFF), AppLightColorScheme.surface)
        assertEquals(Color(0xFF26333B), AppLightColorScheme.onSurface)
    }

    @Test
    fun shapeTokensExposeReviewableFoundationValues() {
        assertEquals(28.dp, DefaultAppShapeTokens.card)
        assertEquals(20.dp, DefaultAppShapeTokens.button)
        assertEquals(18.dp, DefaultAppShapeTokens.field)
        assertEquals(999.dp, DefaultAppShapeTokens.pill)
    }

    @Test
    fun typographyUsesSansSerifPlaceholdersUntilSoraLands() {
        assertEquals(FontFamily.SansSerif, AppTypography.headlineSmall.fontFamily)
        assertEquals(FontFamily.SansSerif, AppTypography.bodyLarge.fontFamily)
        assertEquals(FontFamily.SansSerif, AppTypography.labelLarge.fontFamily)
    }
}
