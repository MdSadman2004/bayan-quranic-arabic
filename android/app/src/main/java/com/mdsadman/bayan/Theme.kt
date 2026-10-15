package com.mdsadman.bayan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** One warm palette in two lights: sand paper and ink by day, lamp-lit ink by
 * night. The colour names stay the same everywhere, so every screen follows the
 * appearance setting without per-screen changes. */
object BayanPalette {
    var dark by mutableStateOf(false)

    private val light = mapOf(
        "paper" to Color(0xFFF7F2E8), "paperDeep" to Color(0xFFEFE7D7), "card" to Color(0xFFFFFCF6),
        "ink" to Color(0xFF26231D), "inkSoft" to Color(0xFF6E6558), "inkFaint" to Color(0xFFA2957E),
        "line" to Color(0xFFE7DDC9), "accent" to Color(0xFFC2542B), "accentDeep" to Color(0xFF96401D),
        "accentTint" to Color(0xFFFBF0E7), "ok" to Color(0xFF56794A), "okDeep" to Color(0xFF3F6136),
        "okBg" to Color(0xFFEFF4E8), "bad" to Color(0xFFA8432F), "badDeep" to Color(0xFF8A3120),
        "badBg" to Color(0xFFF9EBE5), "brass" to Color(0xFFB98A2F)
    )
    private val night = mapOf(
        "paper" to Color(0xFF17140F), "paperDeep" to Color(0xFF120F0A), "card" to Color(0xFF221D16),
        "ink" to Color(0xFFF3ECE0), "inkSoft" to Color(0xFFC3B8A6), "inkFaint" to Color(0xFF8E8371),
        "line" to Color(0xFF372F26), "accent" to Color(0xFFE0703F), "accentDeep" to Color(0xFFF0916A),
        "accentTint" to Color(0xFF2E2019), "ok" to Color(0xFF7FA06A), "okDeep" to Color(0xFFA9C793),
        "okBg" to Color(0xFF1E2418), "bad" to Color(0xFFD8785F), "badDeep" to Color(0xFFEBA08B),
        "badBg" to Color(0xFF2A1B16), "brass" to Color(0xFFD9AE55)
    )

    fun color(name: String): Color = (if (dark) night else light)[name] ?: Color.Unspecified
}

val Paper: Color get() = BayanPalette.color("paper")
val PaperDeep: Color get() = BayanPalette.color("paperDeep")
val CardBg: Color get() = BayanPalette.color("card")
val Ink: Color get() = BayanPalette.color("ink")
val InkSoft: Color get() = BayanPalette.color("inkSoft")
val InkFaint: Color get() = BayanPalette.color("inkFaint")
val LineC: Color get() = BayanPalette.color("line")
val Accent: Color get() = BayanPalette.color("accent")
val AccentDeep: Color get() = BayanPalette.color("accentDeep")
val AccentTint: Color get() = BayanPalette.color("accentTint")
val Ok: Color get() = BayanPalette.color("ok")
val OkDeep: Color get() = BayanPalette.color("okDeep")
val OkBg: Color get() = BayanPalette.color("okBg")
val Bad: Color get() = BayanPalette.color("bad")
val BadDeep: Color get() = BayanPalette.color("badDeep")
val BadBg: Color get() = BayanPalette.color("badBg")
val Brass: Color get() = BayanPalette.color("brass")

@Composable
private fun scheme() = if (BayanPalette.dark) {
    darkColorScheme(
        primary = Accent,
        onPrimary = Color(0xFF2A1A12),
        background = Paper,
        onBackground = Ink,
        surface = CardBg,
        onSurface = Ink,
        surfaceVariant = PaperDeep,
        onSurfaceVariant = InkSoft,
        outline = Accent.copy(alpha = 0.55f),
        error = Bad
    )
} else {
    lightColorScheme(
        primary = Accent,
        onPrimary = Color(0xFFFFF6EF),
        background = Paper,
        onBackground = Ink,
        surface = CardBg,
        onSurface = Ink,
        surfaceVariant = PaperDeep,
        onSurfaceVariant = InkSoft,
        outline = Accent.copy(alpha = 0.55f),
        error = Bad
    )
}

@Composable
fun BayanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme(), typography = Typography(), content = content)
}
