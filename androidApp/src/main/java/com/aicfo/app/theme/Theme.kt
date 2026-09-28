package com.aicfo.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aicfo.app.R

object AiColors {
    val Accent = Color(0xFF635BFF)
    val AccentSoft = Color(0xFFEEF0FF)
    val Text = Color(0xFF0F172A)
    val Muted = Color(0xFF8B93A7)
    val Bg = Color(0xFFF4F5F7)
    val Card = Color.White
    val Nav = Color(0xFF1A1D26)
    val Success = Color(0xFF059669)
    val SuccessSoft = Color(0xFFE7F8F1)
    val Warning = Color(0xFFD97706)
    val WarningSoft = Color(0xFFFFF4E5)
    val Danger = Color(0xFFDC2626)
    val DangerSoft = Color(0xFFFDECEC)
    val Chip = Color(0xFFF1F3F6)
    val DoneCard = Color(0xFFF4FBF7)
    val White = Color.White
}

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp),
)

private val AppColors = lightColorScheme(
    primary = AiColors.Accent,
    onPrimary = Color.White,
    background = AiColors.Bg,
    onBackground = AiColors.Text,
    surface = AiColors.Card,
    onSurface = AiColors.Text,
    secondary = AiColors.AccentSoft,
)

@Composable
fun AiCfoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = AppTypography,
        content = content,
    )
}

fun parseHex(hex: String): Color {
    val rgb = hex.removePrefix("#").toLong(16)
    return Color((0xFF000000L or rgb).toInt())
}
