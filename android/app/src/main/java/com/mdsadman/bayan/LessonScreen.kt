package com.mdsadman.bayan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/* ---------------------------------------------------------------- lesson screen */

@Composable
fun LessonScreen(c: AppController) {
    val r = c.run ?: return
    CompleteTone(c, r)
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { c.quitLesson() }) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = InkFaint)
            }
            Box(Modifier.weight(1f).height(11.dp).clip(RoundedCornerShape(999.dp)).background(PaperDeep)) {
                val frac = if (r.cards.size > 1) r.i.toFloat() / (r.cards.size - 1) else 0f
                Box(
                    Modifier
                        .fillMaxWidth(frac.coerceIn(0.03f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(Accent)
                )
            }
            Row(Modifier.padding(start = 10.dp)) {
                repeat(Engine.MAX_HEARTS) { i ->
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = if (i < r.hearts) Bad else LineC,
                        modifier = Modifier.size(15.dp).padding(start = 2.dp)
                    )
                }
            }
        }
        if (r.flow.multiplier > 1) {
            Text(
                "${r.flow.multiplier}× " + c.t("lesson.flow"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Brass,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            if (r.phase == "failed" && r.cur !is Card.Complete) {
                FailView(c, r)
            } else {
                CardView(c, r)
            }
            Spacer(Modifier.height(14.dp))
        }

        Footer(c, r)
    }
}

@Composable
fun FailView(c: AppController, r: LessonRun) {
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("❤️", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(c.t("lesson.outHearts"), fontFamily = FontFamily.Serif, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(20.dp))
        PrimaryButton(c.t("lesson.retry"), true, true) { c.retry() }
        Spacer(Modifier.height(10.dp))
        PrimaryButton(c.t("lesson.quit"), true, false) { c.quitLesson() }
    }
}

/* ---------------------------------------------------------------- card dispatch */

@Composable
fun CardView(c: AppController, r: LessonRun) {
    when (val card = r.cur) {
        is Card.Teach -> TeachView(c, card)
        is Card.Context -> ContextView(c, card)
        is Card.Read -> ReadView(c, r, card)
        is Card.Mc, is Card.Mca, is Card.Root, is Card.Listen, is Card.Blank -> ChoiceView(c, r, card)
        is Card.Build -> BuildView(c, r, card)
        is Card.Match -> MatchView(c, r, card)
        is Card.Forge -> ForgeView(c, r, card)
        is Card.Complete -> CompleteView(c, r)
        null -> {}
    }
}

/** Sarf forge (feature 3): pick the word the mould produces from the root. */
@Composable
fun ForgeView(c: AppController, r: LessonRun, card: Card.Forge) {
    val round = card.round
    CardSurface {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            TagChip(c.t("forge.tag"), Color(0xFFFBF4E0), Brass)
            Spacer(Modifier.height(12.dp))
            ArabicText(round.mould.pattern, 34)
            Text(round.mould.role.get(c.st.lang), fontSize = 12.sp, color = InkFaint)
            Spacer(Modifier.height(8.dp))
            ArabicText(round.root, 26)
            Spacer(Modifier.height(10.dp))
            Text(c.t("forge.ask"), fontSize = 12.sp, color = InkFaint)
            Spacer(Modifier.height(10.dp))
            round.options.forEachIndexed { index, option ->
                val chosen = r.selected == index
                val frozen = r.phase == "feedback"
                val borderColor = when {
                    frozen && index == round.answer -> Ok
                    frozen && chosen && index != round.answer -> Bad
                    chosen -> Accent
                    else -> LineC
                }
                Text(
                    option,
                    fontSize = 25.sp,
                    fontFamily = FontFamily.Serif,
                    color = Ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (chosen && !frozen) Accent.copy(alpha = 0.08f) else CardBg)
                        .border(2.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable { if (r.phase == "answer") { r.selected = index; c.check() } }
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
fun TeachView(c: AppController, card: Card.Teach) {
    CardSurface {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            TagChip(
                if (card.t.kind == "concept") c.t("lesson.concept") else c.t("lesson.newWord"),
                PaperDeep, InkSoft
            )
            Spacer(Modifier.height(14.dp))
            ArabicText(card.t.ar, 42)
            Spacer(Modifier.height(2.dp))
            Text(card.t.bi.get(c.st.lang), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            card.t.note?.let { note ->
                Spacer(Modifier.height(14.dp))
                Text(
                    note.get(c.st.lang),
                    color = InkSoft, fontSize = 13.5.sp, lineHeight = 19.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Paper)
                        .border(1.dp, LineC, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun ContextView(c: AppController, card: Card.Context) {
    val cx = card.v.context ?: return
    CardSurface {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            TagChip(
                c.t("lesson.context"),
                when (cx.kind) {
                    "asbab" -> AccentTint
                    "story" -> Color(0xFFFBF4E0)
                    else -> PaperDeep
                },
                when (cx.kind) {
                    "asbab" -> Accent
                    "story" -> Brass
                    else -> InkSoft
                }
            )
            Spacer(Modifier.height(14.dp))
            Text(
                cx.title.get(c.st.lang),
                fontFamily = FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = Ink, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(cx.body.get(c.st.lang), color = InkSoft, fontSize = 14.5.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(12.dp))
            Text(card.v.ref.uppercase(), color = InkFaint, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ReadView(c: AppController, r: LessonRun, card: Card.Read) {
    CardSurface {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            TagChip(c.t("lesson.quran"), PaperDeep, InkSoft)
            Spacer(Modifier.height(10.dp))
            ArabicText(card.v.ar, 29)
            if (c.ttsAvailable) {
                Text(
                    "▶ " + c.t("lesson.play"),
                    color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.5.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .border(2.dp, LineC, RoundedCornerShape(11.dp))
                        .clickable { c.speak(card.v.ar) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
            val revealed = card.v.id in r.readRevealed
            if (revealed) {
                Spacer(Modifier.height(10.dp))
                Text(card.v.bi.get(c.st.lang), fontSize = 15.sp, lineHeight = 22.sp, color = Ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(card.v.ref.uppercase(), color = InkFaint, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            } else {
                Spacer(Modifier.height(10.dp))
                Text(
                    c.t("lesson.reveal"),
                    color = Accent, fontSize = 13.5.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { c.revealVerse() }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun ChoiceView(c: AppController, r: LessonRun, card: Card) {
    val question = when (card) {
        is Card.Mc -> c.t("lesson.whatMean")
        is Card.Mca -> c.t("lesson.chooseAr")
        is Card.Root -> c.t("lesson.rootQ")
        is Card.Listen -> c.t("lesson.listenQ")
        else -> c.t("lesson.blankQ")
    }
    val promptArabic = when (card) {
        is Card.Mc -> card.prompt
        is Card.Root -> card.item.ar
        else -> null
    }
    val promptText = when (card) {
        is Card.Mca -> card.prompt
        else -> null
    }
    val hint = when (card) {
        is Card.Root -> card.item.bi.get(c.st.lang)
        else -> null
    }
    val options = when (card) {
        is Card.Mc -> card.options
        is Card.Mca -> card.options
        is Card.Root -> card.options
        is Card.Listen -> card.options
        is Card.Blank -> card.options
        else -> emptyList()
    }
    val answer = when (card) {
        is Card.Mc -> card.answer
        is Card.Mca -> card.answer
        is Card.Root -> card.answer
        is Card.Listen -> card.answer
        is Card.Blank -> card.answer
        else -> 0
    }
    val optionsArabic = card !is Card.Mc
    val listenText = (card as? Card.Listen)?.item?.ar
    val blankVerse = (card as? Card.Blank)?.let { cv ->
        cv.v.tokens.mapIndexed { i, t -> if (i == cv.blank) "____" else t }.joinToString(" ")
    }
    val blankHint = (card as? Card.Blank)?.v?.bi?.get(c.st.lang)

    CardSurface {
        Column(Modifier.fillMaxWidth()) {
            QLabel(question)
            promptArabic?.let { ArabicText(it, 44) }
            promptText?.let {
                Text(it, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            hint?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, color = InkSoft, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            if (card is Card.Listen) {
                Spacer(Modifier.height(6.dp))
                PrimaryButton("▶ " + c.t("lesson.play"), true, true) { listenText?.let { c.speak(it) } }
            }
            blankVerse?.let {
                Spacer(Modifier.height(4.dp))
                ArabicText(it, 27)
                blankHint?.let { h ->
                    Spacer(Modifier.height(4.dp))
                    Text(h, color = InkSoft, fontSize = 13.5.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(16.dp))
            options.forEachIndexed { i, opt ->
                val state = when {
                    r.phase != "feedback" -> if (r.selected == i) "sel" else ""
                    i == answer -> "correct"
                    i == r.selected -> "wrong"
                    else -> "frozen"
                }
                OptionButton(opt, optionsArabic, state, r.phase == "answer") { c.select(i) }
                if (i < options.size - 1) Spacer(Modifier.height(9.dp))
            }
        }
    }
}

@Composable
fun OptionButton(text: String, isArabic: Boolean, state: String, enabled: Boolean, onClick: () -> Unit) {
    val border = when (state) {
        "sel" -> Accent
        "correct" -> Ok
        "wrong" -> Bad
        else -> LineC
    }
    val bg = when (state) {
        "sel" -> AccentTint
        "correct" -> OkBg
        "wrong" -> BadBg
        else -> CardBg
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(2.dp, border, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled && state != "frozen") { onClick() }
            .padding(horizontal = 15.dp, vertical = if (isArabic) 8.dp else 13.dp)
    ) {
        Text(
            text,
            fontFamily = if (isArabic) FontFamily.Serif else FontFamily.Default,
            fontSize = if (isArabic) 24.sp else 15.sp,
            fontWeight = if (isArabic) FontWeight.Normal else FontWeight.SemiBold,
            color = Ink,
            textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BuildView(c: AppController, r: LessonRun, card: Card.Build) {
    val sel = r.buildSel
    val graded = r.phase == "feedback"
    CardSurface {
        Column(Modifier.fillMaxWidth()) {
            QLabel(c.t("lesson.buildQ"))
            Text(
                card.v.bi.get(c.st.lang),
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(when {
                        graded && r.gradedOk == true -> OkBg
                        graded -> BadBg
                        else -> Paper
                    })
                    .border(2.dp, when {
                        graded && r.gradedOk == true -> Ok
                        graded -> Bad
                        else -> LineC
                    }, RoundedCornerShape(14.dp))
                    .padding(8.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column {
                        sel.chunked(5).forEach { chunk ->
                            Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                chunk.forEach { ix ->
                                    TileButton(card.tiles[ix], selected = true) { c.buildUntap(ix) }
                                }
                            }
                        }
                    }
                }
            }
            if (graded && r.gradedOk == false) {
                Spacer(Modifier.height(10.dp))
                ArabicText(r.gradedText, 26)
            }
            Spacer(Modifier.height(16.dp))
            val remaining = card.tiles.indices.filter { it !in sel }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    remaining.chunked(5).forEach { chunk ->
                        Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            chunk.forEach { ix ->
                                TileButton(card.tiles[ix], selected = false) { c.buildTap(ix) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TileButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = FontFamily.Serif,
        fontSize = 22.sp,
        color = Ink,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AccentTint else CardBg)
            .border(2.dp, if (selected) Accent else LineC, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

@Composable
fun MatchView(c: AppController, r: LessonRun, card: Card.Match) {
    LaunchedEffect(r.matchFlash) {
        if (r.matchFlash != null) {
            delay(380)
            c.matchClearFlash()
        }
    }
    CardSurface {
        Column(Modifier.fillMaxWidth()) {
            QLabel(c.t("lesson.matchTitle"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    card.pairs.forEach { p ->
                        MatchTile(
                            text = p.ar,
                            arabic = true,
                            state = when {
                                p.id in r.matchDone -> "matched"
                                r.matchSelLeft == p.id -> "sel"
                                else -> ""
                            }
                        ) { c.matchLeft(p.id) }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    card.rights.forEach { idx ->
                        val p = card.pairs[idx]
                        MatchTile(
                            text = p.meaning,
                            arabic = false,
                            state = when {
                                p.id in r.matchDone -> "matched"
                                r.matchFlash == p.id -> "flash"
                                else -> ""
                            }
                        ) { c.matchRight(p.id) }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchTile(text: String, arabic: Boolean, state: String, onClick: () -> Unit) {
    val border = when (state) {
        "sel" -> Accent
        "matched" -> Ok
        "flash" -> Bad
        else -> LineC
    }
    val bg = when (state) {
        "sel" -> AccentTint
        "matched" -> OkBg
        "flash" -> BadBg
        else -> CardBg
    }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .border(2.dp, border, RoundedCornerShape(13.dp))
            .clickable(enabled = state != "matched") { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontFamily = if (arabic) FontFamily.Serif else FontFamily.Default,
            fontSize = if (arabic) 23.sp else 14.sp,
            fontWeight = if (arabic) FontWeight.Normal else FontWeight.SemiBold,
            color = Ink,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CompleteView(c: AppController, r: LessonRun) {
    val res = c.lastResult
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
            Text("★", color = Color(0xFFFFF6EF), fontSize = 36.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (res?.perfect == true) c.t("lesson.perfect") else c.t("lesson.again"),
            fontFamily = FontFamily.Serif, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink
        )
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResTile("+${res?.xp ?: 0}", c.t("lesson.xpEarned"), Modifier.weight(1f))
            ResTile("${(((res?.acc ?: 0f) * 100).toInt())}%", c.t("lesson.accuracy"), Modifier.weight(1f))
            ResTile("${r.comboMax}", c.t("lesson.combo"), Modifier.weight(1f))
        }
        if (res?.unitComplete != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                c.t("lesson.unitDone"),
                color = Brass, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFBF4E0))
                    .padding(11.dp)
            )
        }
        res?.newAch?.forEach { id ->
            val a = Engine.ACHIEVEMENTS.firstOrNull { it.id == id } ?: return@forEach
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentTint)
                    .padding(11.dp)
            ) {
                BayanGlyph(achGlyph(a.id), 18.dp, AccentDeep)
                Spacer(Modifier.width(9.dp))
                Text(
                    "${a.name.get(c.st.lang)} — ${a.desc.get(c.st.lang)}",
                    color = AccentDeep, fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun ResTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Paper)
            .border(1.dp, LineC, RoundedCornerShape(13.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontFamily = FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(label.uppercase(), fontSize = 9.5.sp, color = InkFaint, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

/* ---------------------------------------------------------------- footer */

/** Completion chirp (feature 1: the ladder's top figure), played once per lesson. */
@Composable
fun CompleteTone(c: AppController, r: LessonRun) {
    LaunchedEffect(r.finalized) {
        if (r.finalized) ToneFeedback.play("complete", 0, c.st.sound)
    }
}

@Composable
fun Footer(c: AppController, r: LessonRun) {
    val card = r.cur ?: return
    val graded = card is Card.Mc || card is Card.Mca || card is Card.Root ||
        card is Card.Listen || card is Card.Blank || card is Card.Build || card is Card.Forge
    val feedback = r.phase == "feedback"
    val bg = when {
        feedback -> if (r.gradedOk == true) OkBg else BadBg
        else -> Paper
    }

    Column(Modifier.fillMaxWidth().background(bg).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp)) {
        if (feedback) {
            Text(
                if (r.gradedOk == true) c.t("lesson.correct") else c.t("lesson.wrong") + " " + r.gradedText,
                color = if (r.gradedOk == true) OkDeep else BadDeep,
                fontWeight = FontWeight.Bold, fontSize = 14.sp
            )
            Spacer(Modifier.height(10.dp))
        }
        when {
            card is Card.Match && !feedback -> Unit
            card is Card.Forge && !feedback -> Unit // options answer immediately
            card is Card.Complete -> PrimaryButton(c.t("lesson.continue"), true, true) { c.continueFromComplete() }
            feedback -> PrimaryButton(c.t("lesson.continue"), true, true) { c.continueCard() }
            graded -> PrimaryButton(
                c.t("lesson.check"),
                enabled = if (card is Card.Build) r.buildSel.isNotEmpty() else r.selected != null,
                accent = false
            ) { c.check() }
            else -> PrimaryButton(c.t("lesson.continue"), true, true) { c.continueCard() }
        }
    }
}
