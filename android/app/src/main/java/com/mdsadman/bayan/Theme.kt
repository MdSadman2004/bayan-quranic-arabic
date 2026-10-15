package com.mdsadman.bayan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Paper = Color(0xFFF7F2E8)
val PaperDeep = Color(0xFFEFE7D7)
val CardBg = Color(0xFFFFFCF6)
val Ink = Color(0xFF26231D)
val InkSoft = Color(0xFF6E6558)
val InkFaint = Color(0xFFA2957E)
val LineC = Color(0xFFE7DDC9)
val Accent = Color(0xFFC2542B)
val AccentDeep = Color(0xFF96401D)
val AccentTint = Color(0xFFFBF0E7)
val Ok = Color(0xFF56794A)
val OkDeep = Color(0xFF3F6136)
val OkBg = Color(0xFFEFF4E8)
val Bad = Color(0xFFA8432F)
val BadDeep = Color(0xFF8A3120)
val BadBg = Color(0xFFF9EBE5)
val Brass = Color(0xFFB98A2F)

private val scheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color(0xFFFFF6EF),
    background = Paper,
    onBackground = Ink,
    surface = CardBg,
    onSurface = Ink,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = InkSoft,
    outline = LineC,
    error = Bad
)

@Composable
fun BayanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
