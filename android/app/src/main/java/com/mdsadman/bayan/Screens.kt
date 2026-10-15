package com.mdsadman.bayan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image

/* ---------------------------------------------------------------- shared bits */

@Composable
fun PrimaryButton(text: String, enabled: Boolean, accent: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (accent) Accent else Ink,
            contentColor = Color(0xFFFFF6EF),
            disabledContainerColor = (if (accent) Accent else Ink).copy(alpha = 0.35f),
            disabledContentColor = Color(0xFFFFF6EF)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun ArabicText(text: String, size: Int, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Center) {
    Text(
        text,
        modifier = modifier.fillMaxWidth(),
        fontFamily = FontFamily.Serif,
        fontSize = size.sp,
        lineHeight = (size * 1.9).sp,
        textAlign = align,
        color = Ink
    )
}

@Composable
fun QLabel(text: String) {
    Text(
        text.uppercase(),
        color = InkFaint,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
fun TagChip(text: String, bg: Color, fg: Color) {
    Text(
        text.uppercase(),
        color = fg,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 11.dp, vertical = 4.dp)
    )
}

@Composable
fun CardSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(1.dp, LineC, RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) { content() }
}

/* ---------------------------------------------------------------- onboarding */

@Composable
fun OnboardingScreen(c: AppController) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))
        Image(
            painter = painterResource(R.drawable.ic_launcher),
            contentDescription = null,
            modifier = Modifier.size(92.dp).clip(RoundedCornerShape(22.dp))
        )
        Spacer(Modifier.height(16.dp))
        Text("Bayan", fontFamily = FontFamily.Serif, fontSize = 42.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(
            if (c.langPref == "bn") "কুরআন বুঝুন, আরবি থেকেই।" else "Understand the Quran, in Arabic.",
            color = InkSoft, fontSize = 15.5.sp
        )
        Spacer(Modifier.height(26.dp))
        listOf("onb.p1", "onb.p2", "onb.p3").forEach { key ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Box(Modifier.padding(top = 7.dp, end = 10.dp).size(7.dp).clip(CircleShape).background(Accent))
                Text(Strings.get(key, c.langPref), color = InkSoft, fontSize = 14.5.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            Strings.get("onb.choose", c.langPref).uppercase(),
            fontSize = 11.5.sp, letterSpacing = 1.5.sp, color = InkFaint, fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LangButton("English", c.langPref == "en") { c.langPref = "en" }
            LangButton("বাংলা", c.langPref == "bn") { c.langPref = "bn" }
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(Strings.get("onb.start", c.langPref), true, true) { c.onboard(c.langPref) }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun LangButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 15.5.sp,
        fontWeight = FontWeight.Bold,
        color = if (selected) Accent else InkSoft,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) AccentTint else CardBg)
            .border(2.dp, if (selected) Accent else LineC, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 28.dp, vertical = 13.dp)
    )
}

/* ---------------------------------------------------------------- chrome */

@Composable
fun TopBar(c: AppController) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Chip("🔥 ${c.st.streak}")
        Spacer(Modifier.weight(1f))
        Chip("💎 ${c.st.gems}")
        Spacer(Modifier.width(8.dp))
        Chip("XP ${c.st.xp}")
    }
}

@Composable
fun Chip(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = Ink,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(CardBg)
            .border(1.dp, LineC, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

@Composable
fun TabBar(c: AppController, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = LineC, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().height(58.dp).background(CardBg)) {
            TabItem(c.t("nav.learn"), Icons.Filled.Home, c.route == "home", Modifier.weight(1f)) { c.go("home") }
            TabItem(c.t("nav.practice"), Icons.Filled.Refresh, c.route == "practice", Modifier.weight(1f)) { c.go("practice") }
            TabItem(c.t("nav.profile"), Icons.Filled.Person, c.route == "profile", Modifier.weight(1f)) { c.go("profile") }
        }
    }
}

@Composable
fun TabItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clickable { onClick() }.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (active) Accent else InkFaint, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(2.dp))
        Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (active) Accent else InkFaint)
    }
}

/* ---------------------------------------------------------------- home */

@Composable
fun HomeScreen(c: AppController) {
    @Suppress("UNUSED_VARIABLE")
    val inPlace = c.tick
    val today = Engine.dayKey(System.currentTimeMillis())
    val daily = if (c.st.day == today) c.st.dailyXp else 0
    val due = Engine.dueIds(c.st).size
    val seen = Engine.seenCount(c.st)
    val nextId = Engine.nextLessonId(c.st, c.cur)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TopBar(c) }
        item { GoalCard(c, daily) }
        item { ReviewCard(c, due, seen) }
        c.cur.units.forEach { u ->
            item { Spacer(Modifier.height(4.dp)); UnitHeader(c, u) }
            items(u.lessons.size) { idx -> NodeRow(c, u.lessons[idx], idx, nextId) }
        }
    }
}

@Composable
fun GoalCard(c: AppController, daily: Int) {
    val goal = c.st.dailyGoal
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(1.dp, LineC, RoundedCornerShape(18.dp))
            .clickable { c.cycleGoal() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(66.dp)) {
                val stroke = 6.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)
                drawArc(LineC, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(
                    Accent, -90f, 360f * (daily.toFloat() / goal).coerceIn(0f, 1f), false, topLeft, arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$daily", fontFamily = FontFamily.Serif, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("/$goal", fontSize = 10.sp, color = InkFaint)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(c.t("home.goal"), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
            Text("$daily XP", color = InkSoft, fontSize = 13.sp)
        }
    }
}

@Composable
fun ReviewCard(c: AppController, due: Int, seen: Int) {
    val sub = when {
        seen == 0 -> c.t("practice.emptyHint")
        due > 0 -> c.t("home.due", "n" to due)
        else -> c.t("home.allDone")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (seen == 0) PaperDeep else Ink)
            .clickable { if (seen == 0) c.toast = c.t("practice.emptyHint") else c.go("practice") }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Refresh, null,
            tint = if (seen == 0) InkFaint else Accent,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(c.t("home.review"), color = if (seen == 0) InkSoft else Paper, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(sub, color = if (seen == 0) InkFaint else Paper.copy(alpha = 0.75f), fontSize = 12.5.sp)
        }
        if (due > 0) {
            Text(
                "$due",
                color = Color(0xFFFFF6EF),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Accent).padding(horizontal = 11.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun UnitHeader(c: AppController, u: Unit) {
    val (done, total) = Engine.unitProgress(c.st, u)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(1.dp, LineC, RoundedCornerShape(18.dp))
            .padding(15.dp)
    ) {
        TagChip(c.t("home.unit", "n" to u.num), AccentTint, Accent)
        Spacer(Modifier.height(9.dp))
        Text(u.title.get(c.st.lang), fontFamily = FontFamily.Serif, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(u.subtitle.get(c.st.lang), color = InkSoft, fontSize = 12.5.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$done/$total", fontSize = 13.sp, color = InkFaint)
            if (done == total) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.Check, null, tint = Ok, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
fun NodeRow(c: AppController, lessonId: String, idx: Int, nextId: String?) {
    val done = Engine.lessonDone(c.st, lessonId)
    val unlocked = Engine.isUnlocked(c.st, lessonId, c.cur)
    val current = unlocked && !done && lessonId == nextId
    val stars = c.st.lessons[lessonId]?.stars ?: 0
    val lesson = c.cur.lessons[lessonId]
    Column(
        Modifier.fillMaxWidth().offset(x = if (idx % 2 == 1) 42.dp else (-42).dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (current) {
                Text(
                    c.t("home.start"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Accent,
                    modifier = Modifier
                        .offset(y = (-42).dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(CardBg)
                        .border(1.dp, LineC, RoundedCornerShape(999.dp))
                        .padding(horizontal = 11.dp, vertical = 4.dp)
                )
            }
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (done) Ink else if (current) Accent else PaperDeep)
                    .border(if (current) 3.dp else 0.dp, if (current) AccentTint else Color.Transparent, CircleShape)
                    .clickable { if (unlocked) c.startLesson(lessonId) else c.toast = c.t("home.locked") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when {
                        done -> Icons.Filled.Check
                        unlocked -> Icons.Filled.PlayArrow
                        else -> Icons.Filled.Lock
                    },
                    contentDescription = null,
                    tint = when {
                        done -> Paper
                        current -> Color(0xFFFFF6EF)
                        unlocked -> InkSoft
                        else -> InkFaint
                    },
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            lesson?.title?.get(c.st.lang) ?: lessonId,
            fontSize = 12.sp,
            color = InkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
        if (done) Text("★".repeat(stars) + "☆".repeat(3 - stars), color = Brass, fontSize = 12.sp)
    }
}

/* ---------------------------------------------------------------- practice */

@Composable
fun PracticeScreen(c: AppController) {
    @Suppress("UNUSED_VARIABLE")
    val inPlace = c.tick
    val due = Engine.dueIds(c.st).size
    val seen = Engine.seenCount(c.st)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Spacer(Modifier.height(8.dp))
        Text(c.t("practice.title"), fontFamily = FontFamily.Serif, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(14.dp))
        if (seen == 0) {
            CardSurface {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔁", fontSize = 34.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(c.t("practice.empty"), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(c.t("practice.emptyHint"), color = InkSoft, fontSize = 13.5.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            CardSurface {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔁", fontSize = 34.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (due > 0) c.t("practice.due", "n" to due) else c.t("home.practiceWeak"),
                        fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(c.t("practice.note"), color = InkSoft, fontSize = 13.5.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(18.dp))
                    PrimaryButton(c.t("practice.start"), true, true) { c.startReview() }
                }
            }
        }
    }
}

/* ---------------------------------------------------------------- profile */

@Composable
fun ProfileScreen(c: AppController) {
    @Suppress("UNUSED_VARIABLE")
    val inPlace = c.tick
    var showReset by remember { mutableStateOf(false) }
    val s = c.st
    val acc = if (s.stats.answered > 0) (100f * s.stats.correct / s.stats.answered).toInt() else 0
    val cov = Engine.coverage(s, c.cur)
    val stats = listOf(
        c.t("profile.xp") to "${s.xp}",
        c.t("profile.streak") to "${s.streak}",
        c.t("profile.gems") to "${s.gems}",
        c.t("profile.words") to "${Engine.learnedCount(s)}",
        c.t("profile.mastered") to "${Engine.masteredCount(s)}",
        c.t("profile.accuracy") to "$acc%",
        c.t("profile.lessons") to "${s.stats.lessons}",
        c.t("profile.perfects") to "${s.stats.perfects}"
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Spacer(Modifier.height(8.dp))
        Text(c.t("profile.title"), fontFamily = FontFamily.Serif, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(14.dp))
        stats.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    StatTile(label, value, Modifier.weight(1f))
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
        CardSurface {
            Column {
                Box(
                    Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)).background(PaperDeep)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((cov / 100f).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(Accent)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(c.t("profile.coverage", "p" to "%.1f".format(cov)), color = InkSoft, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(c.t("profile.achievements"), fontFamily = FontFamily.Serif, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(10.dp))
        Engine.ACHIEVEMENTS.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { a ->
                    AchievementTile(c, a, Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(c.t("profile.settings"), fontFamily = FontFamily.Serif, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(10.dp))
        CardSurface {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.t("profile.language"), fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
                    LangButtonSmall("English", s.lang == "en") { c.setLang("en") }
                    Spacer(Modifier.width(8.dp))
                    LangButtonSmall("বাংলা", s.lang == "bn") { c.setLang("bn") }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.t("profile.sound"), fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
                    Switch(
                        checked = s.sound,
                        onCheckedChange = { c.setSound(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Ok, checkedThumbColor = Color.White)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (c.ttsAvailable) c.t("profile.ttsOn") else c.t("profile.ttsOff"),
                    color = InkFaint, fontSize = 12.5.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(c.t("profile.aboutText"), color = InkSoft, fontSize = 12.5.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = { showReset = true }) {
                    Text(c.t("profile.reset"), color = Bad, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(c.t("profile.reset")) },
            text = { Text(c.t("profile.resetAsk")) },
            confirmButton = {
                TextButton(onClick = { showReset = false; c.resetAll() }) {
                    Text(c.t("common.erase"), color = Bad, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text(c.t("common.cancel")) }
            }
        )
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, LineC, RoundedCornerShape(14.dp))
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontFamily = FontFamily.Serif, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(label.uppercase(), fontSize = 9.5.sp, color = InkFaint, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun AchievementTile(c: AppController, a: Achievement, modifier: Modifier = Modifier) {
    val got = c.st.achievements.containsKey(a.id)
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (got) Color(0xFFFFFBEF) else CardBg)
            .border(1.dp, if (got) Brass else LineC, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(a.icon, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            a.name.get(c.st.lang),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (got) Ink else InkFaint,
            textAlign = TextAlign.Center
        )
        Text(
            a.desc.get(c.st.lang),
            fontSize = 9.5.sp,
            color = InkFaint,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LangButtonSmall(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (selected) Color(0xFFFFF6EF) else InkSoft,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Ink else Color.Transparent)
            .border(1.dp, LineC, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
}
