package com.example.proyectofinal.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    fun semanticFoundationColorsMatchRedesign() {
        assertEquals(Color(0xFFEADFD1), BrandTrack)
        assertEquals(Color(0xFFCBBEAE), BrandLock)
        assertEquals(Color(0xFFF2E9DD), BrandStripe)
        assertEquals(Color(0x6BF2654B), BrandCoralShadow)
    }

    @Test
    fun shapeTokensExposeReviewableFoundationValues() {
        assertEquals(18.dp, DefaultAppShapeTokens.card)
        assertEquals(16.dp, DefaultAppShapeTokens.button)
        assertEquals(15.dp, DefaultAppShapeTokens.field)
        assertEquals(999.dp, DefaultAppShapeTokens.pill)
        assertEquals(7.dp, DefaultAppShapeTokens.checkbox)
        assertEquals(13.dp, DefaultAppShapeTokens.iconBox)
        assertEquals(14.dp, DefaultAppShapeTokens.socialButton)
        assertEquals(999.dp, DefaultAppShapeTokens.stepSegment)
    }

    @Test
    fun typographyMatchesSoraScaleWithInjectedFamily() {
        val typography = buildAppTypography(FontFamily.SansSerif)

        assertEquals(FontFamily.SansSerif, typography.headlineLarge.fontFamily)
        assertEquals(FontFamily.SansSerif, typography.bodyLarge.fontFamily)
        assertEquals(FontFamily.SansSerif, typography.labelMedium.fontFamily)

        assertEquals(32.sp, typography.headlineLarge.fontSize)
        assertEquals(FontWeight.ExtraBold, typography.headlineLarge.fontWeight)
        assertEquals(27.sp, typography.headlineMedium.fontSize)
        assertEquals(FontWeight.ExtraBold, typography.headlineMedium.fontWeight)
        assertEquals(21.sp, typography.headlineSmall.fontSize)
        assertEquals(FontWeight.ExtraBold, typography.headlineSmall.fontWeight)
        assertEquals(17.sp, typography.titleLarge.fontSize)
        assertEquals(FontWeight.Bold, typography.titleLarge.fontWeight)
        assertEquals(14.sp, typography.titleMedium.fontSize)
        assertEquals(FontWeight.Bold, typography.titleMedium.fontWeight)
        assertEquals(15.sp, typography.bodyLarge.fontSize)
        assertEquals(FontWeight.SemiBold, typography.bodyLarge.fontWeight)
        assertEquals(13.sp, typography.bodyMedium.fontSize)
        assertEquals(FontWeight.SemiBold, typography.bodyMedium.fontWeight)
        assertEquals(12.sp, typography.bodySmall.fontSize)
        assertEquals(FontWeight.Medium, typography.bodySmall.fontWeight)
        assertEquals(12.sp, typography.labelMedium.fontSize)
        assertEquals(FontWeight.SemiBold, typography.labelMedium.fontWeight)
    }
}
