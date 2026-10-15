package com.mdsadman.bayan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Bayan's own glyph family, drawn from one 24x24 geometry so web and Android
 * render the same marks. Replaces stock emoji / Material placeholders: a khatim
 * star for the streak (continuous light), a dīnār seal for gems, a stair path,
 * an echo loop, a mihrab arch, a ḥalaqah ring and a qalam nib.
 *
 * `kind`: star | seal | path | echo | mihrab | ring | nib */
@Composable
fun BayanGlyph(kind: String, size: Dp, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val w = (1.7f * s).coerceAtLeast(1.2f)
        val stroke = Stroke(width = w, cap = StrokeCap.Round)
        when (kind) {
            "star" -> khatim(s, tint, stroke)
            "seal" -> dinar(s, tint, stroke)
            "path" -> stairPath(s, tint)
            "echo" -> echoLoop(s, tint, stroke)
            "mihrab" -> mihrab(s, tint, stroke)
            "ring" -> halaqah(s, tint, stroke)
            "nib" -> qalam(s, tint, stroke)
            "sprout" -> sprout(s, tint, stroke)
            "spark" -> spark(s, tint)
            "lamp" -> lamp(s, tint, stroke)
            "scroll" -> scroll(s, tint, stroke)
        }
    }
}

/** Eight-point khatim star: two squares, one rotated 45°, with a lit centre. */
private fun DrawScope.khatim(s: Float, tint: Color, stroke: Stroke) {
    fun square(rotated: Boolean) {
        rotate(if (rotated) 45f else 0f) {
            val side = 13.2f * s
            val topLeft = Offset((12f * s) - side / 2f, (12f * s) - side / 2f)
            drawRect(
                color = tint,
                topLeft = topLeft,
                size = Size(side, side),
                style = stroke
            )
        }
    }
    square(false)
    square(true)
    drawCircle(tint, radius = 1.5f * s, center = Offset(12f * s, 12f * s))
}

/** Dīnār seal: a coin ring with an engraved lozenge and a strike bar. */
private fun DrawScope.dinar(s: Float, tint: Color, stroke: Stroke) {
    val c = Offset(12f * s, 12f * s)
    drawCircle(tint, radius = 8.4f * s, center = c, style = stroke)
    val d = 4.4f * s
    val lozenge = Path().apply {
        moveTo(c.x, c.y - d)
        lineTo(c.x + d, c.y)
        lineTo(c.x, c.y + d)
        lineTo(c.x - d, c.y)
        close()
    }
    drawPath(lozenge, tint, style = stroke)
    drawLine(tint, Offset(c.x - 5.4f * s, c.y), Offset(c.x + 5.4f * s, c.y), strokeWidth = 1.4f * s, cap = StrokeCap.Round)
}

/** Three rising blocks: the study path. */
private fun DrawScope.stairPath(s: Float, tint: Color) {
    val blocks = listOf(
        Triple(3.6f, 16.4f, 4.6f),
        Triple(9.6f, 11.6f, 9.4f),
        Triple(15.6f, 6.8f, 14.2f)
    )
    blocks.forEachIndexed { index, (x, y, h) ->
        val alpha = 0.55f + 0.15f * index
        drawRoundRect(
            color = tint.copy(alpha = alpha),
            topLeft = Offset(x * s, y * s),
            size = Size(4.8f * s, h * s),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.4f * s, 1.4f * s)
        )
    }
}

/** Open ring with a bead: revision coming back around. */
private fun DrawScope.echoLoop(s: Float, tint: Color, stroke: Stroke) {
    val c = Offset(12f * s, 12f * s)
    drawArc(
        color = tint,
        startAngle = -58f,
        sweepAngle = 268f,
        useCenter = false,
        topLeft = Offset(c.x - 7.6f * s, c.y - 7.6f * s),
        size = Size(15.2f * s, 15.2f * s),
        style = stroke
    )
    val bead = Offset(c.x + 8.6f * s * 0.72f, c.y - 8.6f * s * 0.72f)
    drawCircle(tint, radius = 1.9f * s, center = bead)
}

/** Mihrab arch with the lamp behind it. */
private fun DrawScope.mihrab(s: Float, tint: Color, stroke: Stroke) {
    val arch = Path().apply {
        moveTo(6.2f * s, 20.4f * s)
        lineTo(6.2f * s, 11.6f * s)
        cubicTo(6.2f * s, 7.6f * s, 9f * s, 4.4f * s, 12f * s, 3.4f * s)
        cubicTo(15f * s, 4.4f * s, 17.8f * s, 7.6f * s, 17.8f * s, 11.6f * s)
        lineTo(17.8f * s, 20.4f * s)
    }
    drawPath(arch, tint, style = stroke)
    drawCircle(tint, radius = 1.7f * s, center = Offset(12f * s, 13.4f * s))
}

/** Companions in a ring. */
private fun DrawScope.halaqah(s: Float, tint: Color, stroke: Stroke) {
    val c = Offset(12f * s, 12f * s)
    drawCircle(tint, radius = 8.2f * s, center = c, style = stroke)
    val r = 3.6f * s
    for (i in 0 until 3) {
        val angle = Math.toRadians((i * 120 - 90).toDouble())
        drawCircle(
            tint,
            radius = 1.5f * s,
            center = Offset(c.x + (r * Math.cos(angle)).toFloat(), c.y + (r * Math.sin(angle)).toFloat())
        )
    }
}

/** A seed taking root. */
private fun DrawScope.sprout(s: Float, tint: Color, stroke: Stroke) {
    drawLine(tint, Offset(12f * s, 21f * s), Offset(12f * s, 13f * s), strokeWidth = 1.7f * s, cap = StrokeCap.Round)
    val right = Path().apply {
        moveTo(12f * s, 13.2f * s)
        cubicTo(12f * s, 9.6f * s, 14.6f * s, 7.6f * s, 17.8f * s, 7.4f * s)
        cubicTo(17.8f * s, 11f * s, 15.2f * s, 13.2f * s, 12f * s, 13.2f * s)
        close()
    }
    val left = Path().apply {
        moveTo(12f * s, 15.4f * s)
        cubicTo(12f * s, 12.6f * s, 9.8f * s, 10.8f * s, 7.2f * s, 10.6f * s)
        cubicTo(7.2f * s, 13.4f * s, 9.4f * s, 15.4f * s, 12f * s, 15.4f * s)
        close()
    }
    drawPath(right, tint, style = stroke)
    drawPath(left, tint, style = stroke)
}

/** A drawn spark: one continuous lozenge bolt, not a stock lightning glyph. */
private fun DrawScope.spark(s: Float, tint: Color) {
    val bolt = Path().apply {
        moveTo(13.4f * s, 2.6f * s)
        lineTo(6.2f * s, 13.4f * s)
        lineTo(10.4f * s, 13.4f * s)
        lineTo(9f * s, 21.4f * s)
        lineTo(16.4f * s, 10.4f * s)
        lineTo(12f * s, 10.4f * s)
        close()
    }
    drawPath(bolt, tint)
}

/** The lantern that lights a ḥalaqah. */
private fun DrawScope.lamp(s: Float, tint: Color, stroke: Stroke) {
    val body = Path().apply {
        moveTo(12f * s, 3.4f * s)
        cubicTo(8.6f * s, 3.4f * s, 5.8f * s, 6.2f * s, 5.8f * s, 9.6f * s)
        cubicTo(5.8f * s, 12.2f * s, 7.5f * s, 14.5f * s, 9.8f * s, 15.3f * s)
        lineTo(9.8f * s, 17.7f * s)
        lineTo(14.2f * s, 17.7f * s)
        lineTo(14.2f * s, 15.3f * s)
        cubicTo(16.5f * s, 14.5f * s, 18.2f * s, 12.2f * s, 18.2f * s, 9.6f * s)
        cubicTo(18.2f * s, 6.2f * s, 15.4f * s, 3.4f * s, 12f * s, 3.4f * s)
        close()
    }
    drawPath(body, tint, style = stroke)
    drawCircle(tint, radius = 2.4f * s, center = Offset(12f * s, 9.6f * s))
    drawLine(tint, Offset(9.8f * s, 20.4f * s), Offset(14.2f * s, 20.4f * s), strokeWidth = 1.7f * s, cap = StrokeCap.Round)
}

/** A leaf of the mus'haf. */
private fun DrawScope.scroll(s: Float, tint: Color, stroke: Stroke) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(4.6f * s, 4.2f * s),
        size = Size(14.8f * s, 15.6f * s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.2f * s, 2.2f * s),
        style = stroke
    )
    listOf(9f, 12.4f, 15.8f).forEachIndexed { i, y ->
        val width = if (i == 2) 4.4f else 7.2f
        drawLine(tint, Offset(8.4f * s, y * s), Offset((8.4f + width) * s, y * s), strokeWidth = 1.5f * s, cap = StrokeCap.Round)
    }
}

/** Achievement -> glyph, so no screen falls back to an emoji. */
fun achGlyph(id: String?): String = when (id) {
    "first_lesson" -> "sprout"
    "lessons_10", "halfway" -> "path"
    "streak_3", "streak_7" -> "star"
    "xp_100", "xp_500" -> "spark"
    "perfect" -> "seal"
    "unit1" -> "mihrab"
    "words_25" -> "sprout"
    "words_50" -> "ring"
    "review_1" -> "echo"
    else -> "star"
}

/** Qalam nib: the writer's mark. */
private fun DrawScope.qalam(s: Float, tint: Color, stroke: Stroke) {
    val nib = Path().apply {
        moveTo(12f * s, 3.2f * s)
        lineTo(16.4f * s, 12f * s)
        lineTo(12f * s, 20.8f * s)
        lineTo(7.6f * s, 12f * s)
        close()
    }
    drawPath(nib, tint, style = stroke)
    drawLine(tint, Offset(12f * s, 9.4f * s), Offset(12f * s, 15.4f * s), strokeWidth = 1.6f * s, cap = StrokeCap.Round)
}
