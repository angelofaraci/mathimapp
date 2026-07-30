package com.example.proyectofinal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.sora_bold
import proyectofinal.composeapp.generated.resources.sora_extrabold
import proyectofinal.composeapp.generated.resources.sora_regular
import proyectofinal.composeapp.generated.resources.sora_semibold

@Composable
fun rememberSoraFontFamily(): FontFamily = FontFamily(
    Font(Res.font.sora_regular, FontWeight.Normal),
    Font(Res.font.sora_semibold, FontWeight.SemiBold),
    Font(Res.font.sora_bold, FontWeight.Bold),
    Font(Res.font.sora_extrabold, FontWeight.ExtraBold)
)

fun buildAppTypography(fontFamily: FontFamily): Typography = Typography(
    displayLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp),
    displayMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 42.sp),
    displaySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp),
    titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

val AppTypography: Typography
    @Composable get() = buildAppTypography(rememberSoraFontFamily())
