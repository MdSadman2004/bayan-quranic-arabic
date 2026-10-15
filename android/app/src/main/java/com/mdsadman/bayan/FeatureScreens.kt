package com.mdsadman.bayan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar

/** Feature screens: ṣalāh simulator (13), primer (14), wuḍūʾ/adhan (15), Jumuʿah (16),
 * ṣawm drills (17), zakāh/ḥajj (18), whisper boss (4), ḥalaqah + illumination (5, 6)
 * and the relic collection. Compose port of web/js/features-ui.js. */

@Composable
fun RitesScreen(c: AppController) {
    @Suppress("UNUSED_VARIABLE")
    val inPlace = c.tick
    val calendar = Calendar.getInstance()
    val waqt = FeaturesEngine.waqtWindow(calendar.get(Calendar.HOUR_OF_DAY))
    val jumuah = java.util.Calendar.FRIDAY == calendar.get(Calendar.DAY_OF_WEEK)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(c.t("rites.title"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink) }
        item { Text(c.t("rites.note"), fontSize = 13.5.sp, color = InkSoft) }
        item { SalahSimulator(c) }
        item { PrimerSection(c, waqt) }
        item { WuduSection(c) }
        item { JumuahSection(c, jumuah) }
        item { SawmSection(c) }
        item { ZakatHajjSection(c) }
    }
}

@Composable
private fun SectionTitle(title: String, note: String? = null) {
    Column {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        if (note != null) Text(note, fontSize = 13.sp, color = InkSoft, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(1.dp, LineC, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) { content() }
}

@Composable
private fun SectionHeader(c: AppController, titleKey: String, noteKey: String?) {
    Column {
        Text(c.t(titleKey), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        if (noteKey != null) Text(c.t(noteKey), fontSize = 13.sp, color = InkSoft, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun SalahSimulator(c: AppController) {
    val steps = c.features.salahSteps
    if (steps.isEmpty()) return
    val step = steps[c.salahIndex.coerceIn(0, steps.size - 1)]
    Panel {
        SectionHeader(c, "salah.title", "salah.note")
        Spacer(Modifier.height(8.dp))
        Text(step.posture.get(c.st.lang), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Accent)
        Spacer(Modifier.height(6.dp))
        ArabicText(step.ar, 30)
        Spacer(Modifier.height(6.dp))
        Text(step.bi.get(c.st.lang), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Ink)
        Spacer(Modifier.height(10.dp))
        Text(step.grammar.get(c.st.lang), fontSize = 13.sp, color = InkSoft, textAlign = TextAlign.Start)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            steps.indices.forEach { index ->
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (index == c.salahIndex) Accent else LineC)
                        .clickable { c.salahIndex = index }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { c.salahStep(-1) }) { Text(c.t("rite.prev"), color = Ink) }
            Button(
                onClick = { c.salahStep(1) },
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)
            ) { Text(c.t("rite.next")) }
        }
    }
}

private fun verseFor(c: AppController, verseId: String): Verse? =
    c.cur.lessons.values.firstOrNull { lesson -> lesson.verses.any { it.id == verseId } }
        ?.verses?.firstOrNull { it.id == verseId }

@Composable
private fun PrimerSection(c: AppController, waqt: WaqtWindow?) {
    if (waqt == null) return
    Panel {
        SectionHeader(c, "waqt.title", "waqt.approx")
        Spacer(Modifier.height(8.dp))
        Text(waqt.name.get(c.st.lang), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Accent)
        Spacer(Modifier.height(4.dp))
        Text(waqt.focus.get(c.st.lang), fontSize = 14.sp, color = InkSoft, textAlign = TextAlign.Start)
        Spacer(Modifier.height(8.dp))
        val verse = verseFor(c, waqt.verse)
        if (verse != null) {
            ArabicText(verse.ar, 26)
            Spacer(Modifier.height(6.dp))
            Text(verse.bi.get(c.st.lang), fontSize = 13.5.sp, color = InkSoft, textAlign = TextAlign.Center)
            Text(
                verse.ref, fontSize = 11.sp, color = InkFaint, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (c.ttsAvailable) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { c.speak(verse.ar) }) { Text(c.t("lesson.play"), color = Ink) }
            }
        }
    }
}

@Composable
private fun TermCard(c: AppController, entry: RiteTerm) {
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            ArabicText(entry.term, 24)
            ArabicText(entry.root, 15)
        }
        Spacer(Modifier.height(6.dp))
        Text(entry.title.get(c.st.lang), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(2.dp))
        Text(entry.body.get(c.st.lang), fontSize = 13.sp, color = InkSoft)
    }
}

@Composable
private fun WuduSection(c: AppController) {
    Column {
        SectionHeader(c, "wudu.title", "wudu.note")
        c.features.wuduAdhan.forEach { entry ->
            Spacer(Modifier.height(8.dp))
            TermCard(c, entry)
        }
    }
}

@Composable
private fun JumuahSection(c: AppController, open: Boolean) {
    Column {
        SectionHeader(c, "jumuah.title", null)
        Text(c.t(if (open) "jumuah.open" else "jumuah.closed"), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = if (open) OkDeep else InkFaint)
        if (open) Text(c.t("jumuah.surah"), fontSize = 11.5.sp, color = InkFaint, modifier = Modifier.padding(top = 2.dp))
        c.features.jumuahPhrases.forEach { phrase ->
            Spacer(Modifier.height(8.dp))
            Panel {
                ArabicText(phrase.ar, 22)
                Spacer(Modifier.height(6.dp))
                Text(phrase.bi.get(c.st.lang), fontSize = 13.5.sp, color = InkSoft)
                if (c.ttsAvailable) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { c.speak(phrase.ar) }) { Text(c.t("lesson.play"), color = Ink) }
                }
            }
        }
    }
}

@Composable
private fun SawmSection(c: AppController) {
    Column {
        SectionHeader(c, "sawm.title", "sawm.note")
        if (!c.ttsAvailable) {
            Spacer(Modifier.height(6.dp))
            Text(c.t("sawm.needsVoice"), fontSize = 13.sp, color = InkSoft)
        } else {
            c.features.sawmDrills.forEach { drill -> SawmDrillCard(c, drill) }
        }
    }
}

@Composable
private fun SawmDrillCard(c: AppController, drill: SawmDrill) {
    var answered by remember(drill.id) { mutableStateOf<Boolean?>(null) }
    Panel {
        Text(drill.ask.get(c.st.lang), fontSize = 14.sp, color = Ink)
        Spacer(Modifier.height(8.dp))
        val verse = verseFor(c, drill.verse)
        if (verse != null) OutlinedButton(onClick = { c.speak(verse.ar) }) { Text(c.t("sawm.drill"), color = Ink) }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            drill.options.forEachIndexed { index, option ->
                OutlinedButton(
                    onClick = {
                        if (answered == null) {
                            answered = index == drill.answer
                            val ok = answered == true
                            c.recordRite("sawm:" + drill.id, ok, 3)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { ArabicText(option, 18) }
            }
        }
        answered?.let { ok ->
            Spacer(Modifier.height(6.dp))
            Text(c.t(if (ok) "sawm.correct" else "sawm.wrong"), fontSize = 13.sp, color = if (ok) OkDeep else BadDeep)
        }
    }
}

@Composable
private fun ZakatHajjSection(c: AppController) {
    Column {
        SectionHeader(c, "zakat.title", "zakat.note")
        c.features.zakatHajj.forEach { entry ->
            Spacer(Modifier.height(8.dp))
            TermCard(c, entry)
        }
    }
}

@Composable
fun BossScreen(c: AppController) {
    val session = c.bossSession
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                session == null -> {
                    Text(c.t("boss.title"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(c.t("boss.invite", "n" to FeaturesEngine.F.bossSeconds), fontSize = 13.5.sp, color = InkSoft)
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { c.startBoss() },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Paper),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(c.t("boss.start")) }
                }
                session.done -> {
                    Spacer(Modifier.height(30.dp))
                    val score = FeaturesEngine.bossScore(session.waves, session.hits, session.secondsLeft())
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("${score.hits}/${score.total}", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Accent)
                        Text(c.t("boss.hits"), fontSize = 12.sp, color = InkFaint)
                        Spacer(Modifier.height(4.dp))
                        Text(c.t("boss.resultXp", "n" to score.xp), fontSize = 14.sp, color = InkSoft)
                        if (score.shields > 0) Text(c.t("boss.shieldWon"), fontSize = 13.sp, color = Brass)
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { c.startBoss() },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Paper),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(c.t("boss.again")) }
                        OutlinedButton(onClick = { c.bossSession = null; c.go("home") }, modifier = Modifier.fillMaxWidth()) {
                            Text(c.t("common.continue"), color = Ink)
                        }
                    }
                }
                else -> {
                    val wave = session.wave
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${session.secondsLeft()}s", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Accent)
                        Text("${session.index + 1}/${session.waves.size}", fontSize = 12.5.sp, color = InkFaint)
                    }
                    Panel {
                        Text(c.t("boss.trap"), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = InkFaint)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            wave.trap.get(c.st.lang),
                            fontSize = 16.sp, color = InkSoft, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    wave.options.forEachIndexed { index, option ->
                        OutlinedButton(onClick = { c.bossAnswer(index) }, modifier = Modifier.fillMaxWidth()) {
                            Text(option.get(c.st.lang), color = Ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CircleScreen(c: AppController) {
    @Suppress("UNUSED_VARIABLE")
    val inPlace = c.tick
    val circle = c.st.featureState.circle
    var nameDraft by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(c.t("circle.title"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink) }
        item { Text(c.t("circle.note"), fontSize = 13.5.sp, color = InkSoft) }
        item { LanternCard(c, circle) }
        item {
            Column {
                Text(c.t("circle.members"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(vertical = 6.dp))
                circle.members.forEach { member ->
                    Panel { Text(member, fontSize = 14.sp, color = Ink) }
                    Spacer(Modifier.height(8.dp))
                }
                if (circle.members.isEmpty()) Text(c.t("circle.empty"), fontSize = 13.sp, color = InkFaint)
            }
        }
        item {
            Panel {
                TextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it.take(24) },
                    placeholder = { Text(c.t("circle.name"), color = InkFaint) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Paper,
                        unfocusedContainerColor = Paper,
                        focusedIndicatorColor = Accent,
                        unfocusedIndicatorColor = LineC
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { c.circleAdd(nameDraft); nameDraft = "" },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Paper)
                ) { Text(c.t("circle.add")) }
            }
        }
        item { MapSection(c) }
        item { CollectionSection(c) }
    }
}

@Composable
private fun LanternCard(c: AppController, circle: HalaqahCircle) {
    val percent = (circle.lantern.coerceIn(0f, 1f) * 100).toInt()
    Panel {
        SectionHeader(c, "circle.light", "circle.shieldsNote")
        Spacer(Modifier.height(10.dp))
        Text(
            "$percent%",
            fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Accent,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { c.circleShield() },
            colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
            modifier = Modifier.fillMaxWidth()
        ) { Text(c.t("circle.shield")) }
    }
}

@Composable
private fun MapSection(c: AppController) {
    val map = FeaturesEngine.illumination(c.st, c.cur)
    Column {
        Text(c.t("map.title"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(vertical = 6.dp))
        Panel {
            Text(c.t("map.note"), fontSize = 13.sp, color = InkSoft)
            Spacer(Modifier.height(8.dp))
            Text(
                c.t("map.summary", "lit" to map.litVerses, "total" to map.totalVerses, "roots" to map.roots.size),
                fontSize = 13.5.sp, color = Ink
            )
            Spacer(Modifier.height(10.dp))
            map.verses.chunked(9).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                    row.forEach { verse ->
                        Box(
                            Modifier
                                .size(17.dp)
                                .clip(CircleShape)
                                .background(if (verse.lit) Accent.copy(alpha = 0.85f) else PaperDeep)
                                .border(1.dp, if (verse.lit) Brass else LineC, CircleShape)
                        )
                    }
                }
            }
            if (map.roots.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    map.roots.take(6).forEach { root -> ArabicText(root, 15) }
                }
            }
        }
    }
}

@Composable
private fun CollectionSection(c: AppController) {
    val state = c.st.featureState
    Column {
        Text(c.t("collection.title"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(vertical = 6.dp))
        if (state.collection.isEmpty()) {
            Text(c.t("collection.empty"), fontSize = 13.sp, color = InkFaint)
        } else {
            state.collection
                .mapNotNull { FeaturesEngine.F.loot.firstOrNull { entry -> entry.id == it } }
                .forEach { entry ->
                    Spacer(Modifier.height(8.dp))
                    Panel {
                        Text(entry.title.get(c.st.lang), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text(
                            c.t("chest." + entry.kind) + " × " + (state.chests[entry.id] ?: 1),
                            fontSize = 11.5.sp, color = InkFaint
                        )
                        Text(entry.body.get(c.st.lang), fontSize = 13.sp, color = InkSoft)
                    }
                }
        }
    }
}

/** XP awarded by rite micro-drills (features 15–18); kept device-local, no heart cost. */
fun AppController.recordRite(id: String, ok: Boolean, xp: Int) {
    if (ok) st.xp += xp
    persist()
    bumpTick()
}

/** Boss clock: recomposes once per second while a battle is live, so the timer
 * stays honest even if no tap arrives. Renders the boss surface itself. */
@Composable
fun PlacementScreen(c: AppController) {
    @Suppress("UNUSED_VARIABLE")
    val inPlace = c.tick
    val session = c.placementSession
    if (session == null) { PlacementIntro(c); return }
    if (session.done) { PlacementResultView(c, session); return }
    val question = session.current
    if (question == null) { PlacementResultView(c, session); return }
    val graded = c.placementGraded
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(c.t("placement.title"), fontSize = 13.sp, color = InkFaint)
            Text(c.t("placement.progress", "n" to (session.total + 1)), fontSize = 13.sp, color = InkFaint)
        }
        Panel {
            Text(c.t("placement.question"), fontSize = 12.sp, color = InkSoft)
            Spacer(Modifier.height(10.dp))
            ArabicText(c.itemAr(question.itemId), 30)
            Spacer(Modifier.height(12.dp))
            question.options.forEachIndexed { index, id ->
                val truthShown = graded != null && index == question.answer
                OutlinedButton(
                    onClick = { c.answerPlacement(index) },
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(if (truthShown) 2.dp else 1.dp, if (truthShown) Ok else LineC)
                ) {
                    Text(c.itemMeaning(id), color = Ink)
                }
            }
            if (graded != null) {
                Spacer(Modifier.height(10.dp))
                Text(c.itemMeaning(question.itemId) + "  " + c.t(if (graded == true) "placement.correct" else "placement.wrong"),
                    fontSize = 13.sp, color = if (graded == true) OkDeep else BadDeep)
            }
        }
        if (graded != null) {
            Button(
                onClick = { c.advancePlacement() },
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                modifier = Modifier.fillMaxWidth()
            ) { Text(c.t("common.continue")) }
        } else {
            OutlinedButton(onClick = { c.closePlacement() }, modifier = Modifier.fillMaxWidth()) {
                Text(c.t("placement.keep"), color = Ink)
            }
        }
    }
}

@Composable
private fun PlacementIntro(c: AppController) {
    val placed = c.st.placed
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(c.t("placement.title"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink) }
        item { Text(c.t("placement.note"), fontSize = 13.5.sp, color = InkSoft) }
        item {
            Panel {
                Text(c.t("placement.why"), fontSize = 13.sp, color = InkSoft)
                if (placed) {
                    Spacer(Modifier.height(8.dp))
                    Text(c.t("placement.existing", "n" to (c.st.placedTier + 1)), fontSize = 13.sp, color = Accent)
                }
            }
        }
        item {
            Button(
                onClick = { c.startPlacement() },
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Paper),
                modifier = Modifier.fillMaxWidth()
            ) { Text(c.t("placement.start")) }
        }
        if (placed) {
            item {
                OutlinedButton(onClick = { c.undoPlacementNow() }, modifier = Modifier.fillMaxWidth()) {
                    Text(c.t("placement.undo"), color = Ink)
                }
            }
        }
        item {
            OutlinedButton(onClick = { c.closePlacement() }, modifier = Modifier.fillMaxWidth()) {
                Text(c.t("placement.keep"), color = Ink)
            }
        }
    }
}

@Composable
private fun PlacementResultView(c: AppController, session: FeaturesEngine.PlacementSession) {
    val result = session.result ?: FeaturesEngine.placementResult(session)
    val unit = c.cur.units.getOrNull(result.tier)
    var below = 0
    for (i in 0 until result.tier.coerceIn(0, c.cur.units.size)) below += c.cur.units[i].lessons.size
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(c.t("placement.result"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink) }
        item {
            Panel {
                Text(c.t("placement.tierLabel", "n" to (result.tier + 1)), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Accent)
                Text(unit?.title?.get(c.st.lang) ?: "", fontSize = 14.sp, color = InkSoft)
                Spacer(Modifier.height(8.dp))
                Text(c.t("placement.score", "ok" to result.correct, "n" to result.total), fontSize = 13.5.sp, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (below > 0) c.t("placement.placed", "n" to below) else c.t("placement.nothing"),
                    fontSize = 13.sp, color = InkSoft
                )
                if (!result.confident) {
                    Spacer(Modifier.height(4.dp))
                    Text(c.t("placement.unsure"), fontSize = 12.5.sp, color = InkFaint)
                }
                Spacer(Modifier.height(10.dp))
                Text(c.t("placement.why"), fontSize = 12.5.sp, color = InkFaint)
            }
        }
        item {
            Button(
                onClick = { c.applyPlacementNow() },
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Paper),
                modifier = Modifier.fillMaxWidth()
            ) { Text(c.t("placement.apply")) }
        }
        item {
            OutlinedButton(onClick = { c.startPlacement() }, modifier = Modifier.fillMaxWidth()) {
                Text(c.t("placement.retake"), color = Ink)
            }
        }
        if (c.st.placed) {
            item {
                OutlinedButton(onClick = { c.undoPlacementNow() }, modifier = Modifier.fillMaxWidth()) {
                    Text(c.t("placement.undo"), color = Ink)
                }
            }
        }
        item {
            OutlinedButton(onClick = { c.closePlacement() }, modifier = Modifier.fillMaxWidth()) {
                Text(c.t("placement.keep"), color = Ink)
            }
        }
    }
}

@Composable
fun BossClock(c: AppController) {
    LaunchedEffect(c.bossSession?.startedAt) {
        val session = c.bossSession ?: return@LaunchedEffect
        while (!session.done) {
            delay(1000)
            c.tickBoss()
            c.bumpTick()
        }
    }
    BossScreen(c)
}
