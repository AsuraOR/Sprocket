package com.example.sprocket.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.sprocket.R

val ArchivoFontFamily = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_semibold, FontWeight.Bold),
    Font(R.font.archivo_extrabold, FontWeight.ExtraBold)
)

val SprocketTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 52.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.035).sp
    ),
    displayMedium = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 42.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.03).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.02).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).sp
    ),
    titleLarge = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.02.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        lineHeight = 10.sp,
        letterSpacing = 0.12.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)
