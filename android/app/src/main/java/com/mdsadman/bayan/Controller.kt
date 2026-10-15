package com.mdsadman.bayan

import android.app.Activity
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Central app controller: navigation, lesson session, TTS, persistence. */
class AppController(private val ctx: Context) {

    val cur: Curriculum = Curriculum.load(ctx)
    /** Game/wisdom/rite payload loaded from assets/features.json; falls back to defaults. */
    val features: Features = try {
        FeaturesEngine.load(ctx.assets.open("features.json").bufferedReader().use { it.readText() })
    } catch (ignored: java.io.IOException) {
        FeaturesEngine.load(null)
    }
    private val store = Store(ctx, cur)

    var st: AppState = store.load()
        private set

    /** bumped on in-place state changes so screens recompose (they read c.tick). */
    var tick by mutableStateOf(0)
        private set

    var ttsAvailable by mutableStateOf(false)
        private set

    var route by mutableStateOf(if (st.onboarded) "home" else "onboarding")
        private set
    /** Ṣalāh simulator position (feature 13), remembered while the app runs. */
    var salahIndex by mutableStateOf(0)
    /** Whisper boss session (feature 4); null outside a battle. */
    var bossSession by mutableStateOf<BossSession?>(null)

    var langPref by mutableStateOf(st.lang)
    var run: LessonRun? by mutableStateOf(null)
        private set
    var lastResult: ResultSummary? by mutableStateOf(null)
        private set
    var toast: String? by mutableStateOf(null)

    var pendingLesson: String? = null
    var pendingAuto: Boolean = false

    private var tts: TextToSpeech? = null
    private var pitchPresets: Map<Int, Pair<Int, Int>> = mapOf() // semitones -> low/high tone pairs

    fun t(key: String, vararg vars: Pair<String, Any>): String =
        Strings.get(key, st.lang, vars.toMap())

    fun refresh() {
        persist()
        tick += 1
    }

    /** Same as refresh() without an extra save, for callers that already persisted. */
    fun bumpTick() {
        tick += 1
    }

    fun persist() = store.save(st)

    /* ------------------------------------------------ tts */

    fun initTts(activity: Activity) {
        tts = TextToSpeech(activity) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts?.setLanguage(Locale("ar"))
                ttsAvailable = res != TextToSpeech.LANG_MISSING_DATA &&
                    res != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun shutdownTts() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    fun speak(text: String) {
        if (ttsAvailable) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bayan")
    }

    /* ------------------------------------------------ navigation */

    fun go(r: String) {
        route = r
    }

    fun processPendingIntent() {
        val lid = pendingLesson ?: return
        pendingLesson = null
        if (cur.lessons.containsKey(lid)) {
            startLesson(lid)
            if (pendingAuto) {
                pendingAuto = false
                autoRun()
            }
        }
    }

    fun onboard(lang: String) {
        st.onboarded = true
        st.lang = lang
        refresh()
        go("home")
    }

    fun setLang(lang: String) {
        st.lang = lang
        langPref = lang
        refresh()
    }

    fun setSound(on: Boolean) {
        st.sound = on
        refresh()
    }

    fun cycleGoal() {
        val goals = listOf(20, 30, 50, 100)
        st.dailyGoal = goals[(goals.indexOf(st.dailyGoal) + 1) % goals.size]
        refresh()
    }

    fun resetAll() {
        store.clear()
        st = AppState()
        run = null
        lastResult = null
        tick += 1
        go("onboarding")
    }

    /* ------------------------------------------------ lesson session */

    fun startLesson(id: String) {
        run = LessonRun("lesson", id, Engine.compileLesson(cur, id, st, ttsAvailable))
        lastResult = null
        route = "lesson"
    }

    fun startReview() {
        val q = Engine.reviewQueue(st)
        if (q.size < 4) {
            toast = t("practice.empty")
            return
        }
        run = LessonRun("review", null, Engine.compileReview(cur, st, q, ttsAvailable))
        lastResult = null
        route = "lesson"
    }

    fun retry() {
        val r = run ?: return
        if (r.mode == "review") startReview() else r.lessonId?.let { startLesson(it) }
    }

    fun quitLesson() {
        run = null
        go("home")
    }

    /* ------------------------------------------------ whisper boss (feature 4) */

    fun startBoss() {
        val waves = FeaturesEngine.bossQueue(kotlin.random.Random, FeaturesEngine.F.bossWaves.size)
        bossSession = BossSession(waves)
        go("boss")
    }

    fun bossAnswer(index: Int) {
        val session = bossSession ?: return
        if (session.done) return
        val correct = index == session.wave.answer
        if (correct) session.hits += 1 else session.wrong += 1
        ToneFeedback.play(if (correct) "ok" else "bad", 0, st.sound)
        session.index += 1
        if (session.index >= session.waves.size || session.expired()) {
            finishBoss()
        }
        tick += 1
    }

    private fun finishBoss() {
        val session = bossSession ?: return
        session.done = true
        val secondsLeft = maxOf(0, ((session.endsAt - System.currentTimeMillis()) / 1000).toInt())
        val score = FeaturesEngine.bossScore(session.waves, session.hits, secondsLeft)
        st.xp += score.xp
        if (score.shields > 0) st.featureState.shields += score.shields
        persist()
        tick += 1
    }

    fun tickBoss() {
        val session = bossSession ?: return
        if (!session.done && session.expired()) finishBoss()
    }

    fun salahStep(delta: Int) {
        salahIndex = maxOf(0, minOf(FeaturesEngine.F.salahSteps.size - 1, salahIndex + delta))
    }

    fun circleAdd(name: String) {
        st.featureState.circle.add(name)
        refresh()
    }

    fun circleShield() {
        if (st.featureState.circle.lendShield()) {
            st.featureState.shields += 1
            toast = t("circle.shieldCast")
        }
        refresh()
    }

    /* ------------------------------------------------ interaction */

    fun select(i: Int) {
        val r = run ?: return
        if (r.phase != "answer") return
        r.selected = i
    }

    fun check() {
        val r = run ?: return
        val c = r.cur ?: return
        if (r.phase != "answer") return
        when (c) {
            is Card.Mc -> finishAnswer(r, c.id, r.selected == c.answer, c.options[c.answer])
            is Card.Mca -> finishAnswer(r, c.id, r.selected == c.answer, c.options[c.answer])
            is Card.Root -> finishAnswer(r, c.id, r.selected == c.answer, c.options[c.answer])
            is Card.Listen -> finishAnswer(r, c.id, r.selected == c.answer, c.options[c.answer])
            is Card.Blank -> finishAnswer(r, c.id, r.selected == c.answer, c.options[c.answer])
            is Card.Build -> {
                if (r.buildSel.isEmpty()) return
                val joined = r.buildSel.map { c.tiles[it] }.joinToString(" ")
                finishAnswer(r, c.v.id, joined == c.v.ar, c.v.ar)
            }
            is Card.Forge -> {
                val word = c.round.options.getOrNull(r.selected ?: -1) ?: return
                finishAnswer(r, "forge:" + c.round.mould.id, word == c.round.word, c.round.word)
            }
            else -> {}
        }
    }

    private fun finishAnswer(r: LessonRun, id: String?, ok: Boolean, correctText: String) {
        r.record(id, ok)
        r.gradedOk = ok
        r.gradedText = correctText
        if (r.phase != "failed") r.phase = "feedback"
        ToneFeedback.playFor(if (ok) "ok" else "bad", r.flow, st.sound)
    }

    fun continueCard() {
        val r = run ?: return
        if (r.phase == "failed") return
        r.phase = "answer"
        r.selected = null
        r.gradedOk = null
        r.gradedText = ""
        r.buildSel = emptyList()
        r.matchSelLeft = null
        r.matchDone = emptySet()
        r.matchFlash = null
        r.i += 1
        if (r.cur is Card.Complete) finalize()
    }

    fun continueFromComplete() {
        run = null
        go("home")
    }

    private fun finalize() {
        val r = run ?: return
        if (r.finalized) return
        r.finalized = true
        r.secs = ((System.currentTimeMillis() - r.startedAt) / 1000).toInt()
        lastResult = Engine.applyLessonResult(cur, st, r)
        persist()
    }

    /* match */

    fun matchLeft(id: String) {
        val r = run ?: return
        if (r.phase != "answer") return
        r.matchSelLeft = id
    }

    fun matchRight(pairId: String) {
        val r = run ?: return
        if (r.phase != "answer") return
        val left = r.matchSelLeft ?: return
        if (left == pairId) {
            r.matchDone = r.matchDone + pairId
            r.matchSelLeft = null
            r.record(pairId, true)
            val card = r.cur as? Card.Match
            if (card != null && r.phase != "failed" && r.matchDone.size == card.pairs.size) {
                r.gradedOk = true
                r.gradedText = ""
                r.phase = "feedback"
            }
        } else {
            r.record(pairId, false)
            r.matchFlash = pairId
        }
    }

    fun matchClearFlash() {
        run?.matchFlash = null
    }

    /* build */

    fun buildTap(poolIndex: Int) {
        val r = run ?: return
        if (r.phase != "answer") return
        r.buildSel = r.buildSel + poolIndex
    }

    fun buildUntap(selIndex: Int) {
        val r = run ?: return
        if (r.phase != "answer") return
        r.buildSel = r.buildSel.filter { it != selIndex }
    }

    fun revealVerse() {
        val r = run ?: return
        val c = r.cur as? Card.Read ?: return
        r.readRevealed = r.readRevealed + c.v.id
    }

    /* ------------------------------------------------ scripted run (deep-link verification) */

    fun autoRun() {
        val r = run ?: return
        var guard = 0
        while (!r.finalized && guard++ < 500) {
            val c = r.cur ?: break
            when (c) {
                is Card.Teach, is Card.Context -> continueCard()
                is Card.Read -> {
                    r.readRevealed = r.readRevealed + c.v.id
                    continueCard()
                }
                is Card.Match -> {
                    for (p in c.pairs) {
                        r.matchDone = r.matchDone + p.id
                        r.record(p.id, true)
                    }
                    r.gradedOk = true
                    r.gradedText = ""
                    if (r.phase != "failed") r.phase = "feedback"
                    continueCard()
                }
                is Card.Build -> {
                    val target = c.v.ar.split(" ")
                    val sel = ArrayList<Int>()
                    val used = HashSet<Int>()
                    for (tk in target) {
                        for ((ix, tile) in c.tiles.withIndex()) {
                            if (ix !in used && tile == tk) {
                                used.add(ix); sel.add(ix); break
                            }
                        }
                    }
                    r.buildSel = sel
                    check()
                    continueCard()
                }
                is Card.Complete -> break
                else -> {
                    r.selected = answerOf(c)
                    check()
                    continueCard()
                }
            }
        }
    }

    private fun answerOf(c: Card): Int = when (c) {
        is Card.Mc -> c.answer
        is Card.Mca -> c.answer
        is Card.Root -> c.answer
        is Card.Listen -> c.answer
        is Card.Blank -> c.answer
        else -> 0
    }
}
