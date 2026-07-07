package com.example.proyectofinal.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val BrandPrimary = Color(0xFFF2654B)
val BrandSecondary = Color(0xFF0E9E8E)
val BrandError = Color(0xFFF0526A)
val BrandBackground = Color(0xFFFBF6EF)
val BrandSurface = Color(0xFFFFFFFF)
val BrandSurfaceVariant = Color(0xFFF6EFE6)
val BrandOutlineVariant = Color(0xFFEBE3D7)
val BrandOnSurface = Color(0xFF26333B)

val AppLightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandSurface,
    primaryContainer = BrandPrimary.copy(alpha = 0.14f),
    onPrimaryContainer = BrandOnSurface,
    secondary = BrandSecondary,
    onSecondary = BrandSurface,
    secondaryContainer = BrandSecondary.copy(alpha = 0.14f),
    onSecondaryContainer = BrandOnSurface,
    tertiary = BrandSecondary,
    onTertiary = BrandSurface,
    tertiaryContainer = BrandSurfaceVariant,
    onTertiaryContainer = BrandOnSurface,
    error = BrandError,
    onError = BrandSurface,
    errorContainer = BrandError.copy(alpha = 0.14f),
    onErrorContainer = BrandOnSurface,
    background = BrandBackground,
    onBackground = BrandOnSurface,
    surface = BrandSurface,
    onSurface = BrandOnSurface,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandOnSurface.copy(alpha = 0.74f),
    outline = BrandOutlineVariant,
    outlineVariant = BrandOutlineVariant,
    scrim = BrandOnSurface.copy(alpha = 0.32f)
)
