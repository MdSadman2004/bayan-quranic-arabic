package com.mdsadman.bayan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/* ---------------------------------------------------------------- cards */

sealed class Card {
    data class Teach(val t: TeachC) : Card()
    data class Context(val v: Verse) : Card()
    data class Read(val v: Verse) : Card()
    data class Mc(val id: String, val item: Item, val prompt: String, val options: List<String>, val answer: Int) : Card()
    data class Mca(val id: String, val item: Item, val prompt: String, val options: List<String>, val answer: Int) : Card()
    data class Root(val id: String, val item: Item, val options: List<String>, val answer: Int) : Card()
    data class Listen(val id: String, val item: Item, val options: List<String>, val answer: Int) : Card()
    data class Blank(val id: String, val v: Verse, val blank: Int, val options: List<String>, val answer: Int) : Card()
    data class Build(val v: Verse, val tiles: List<String>) : Card()
    data class MatchPair(val id: String, val ar: String, val meaning: String)
    data class Match(val pairs: List<MatchPair>, val rights: List<Int>) : Card()
    data class Complete(val lessonId: String) : Card()
    /** Sarf forge: pick the word this mould forges from the root (feature 3). */
    data class Forge(val id: String, val round: FeaturesEngine.ForgeRound) : Card()
}

/* ---------------------------------------------------------------- rng */

class Rng(seed: String) {
    private var a: Int

    init {
        var h = 1779033703 xor seed.length
        for (c in seed) {
            h = (h xor c.code) * -862048943
            h = (h shl 13) or (h ushr 19)
        }
        a = h
    }

    fun next(): Double {
        a += 0x6D2B79F5
        var t = (a xor (a ushr 15)) * (1 or a)
        t += (t xor (t ushr 7)) * (61 or t)
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }
}

fun <T> shuffled(src: List<T>, rng: Rng): List<T> {
    val a = src.toMutableList()
    for (i in a.size - 1 downTo 1) {
        val j = floor(rng.next() * (i + 1)).toInt()
        val t = a[i]; a[i] = a[j]; a[j] = t
    }
    return a
}

/* ---------------------------------------------------------------- state */

class LessonRec { var done = false; var attempts = 0; var stars = 0; var lastAt = 0L; var placed = false }
class SrsRec { var lvl = 0; var seen = 0; var ok = 0; var bad = 0; var dueAt = 0L; var flag = false }
class Stats {
    var answered = 0; var correct = 0; var lessons = 0; var reviews = 0
    var perfects = 0; var maxComboEver = 0; var timeSpentSec = 0
}

/** Lesson-session flow state for the Noor combo (feature 1). Not persisted: it is
 * per-run bookkeeping, and a new run must always restart at 1x. */
class FlowState {
    var streak = 0
    var lastAt = 0L
    var multiplier = 1
    var semitones = 0
}

class AppState {
    var v = 1
    var onboarded = false
    var lang = "en"
    var sound = true
    var xp = 0
    var gems = 0
    var streak = 0
    var day: String? = null
    var dailyXp = 0
    var dailyGoal = 30
    val lessons = LinkedHashMap<String, LessonRec>()
    val unitsDone = LinkedHashSet<String>()
    /** Placement test: the unit index the learner was placed at (0 = none skipped). */
    var placedTier = 0
    var placed = false
    /** Appearance: "auto" follows the device, otherwise "light" / "dark". */
    var theme = "auto"
    val items = LinkedHashMap<String, SrsRec>()
    val achievements = LinkedHashMap<String, Long>()
    val stats = Stats()
    /** Device-local feature extras (loot, shields, ḥalaqah). Kept in a separate
     * SharedPreferences file so the portable v1 progress JSON stays stable. */
    var featureState: FeatureState = FeatureState()

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("v", v); o.put("onboarded", onboarded); o.put("lang", lang); o.put("sound", sound)
        o.put("xp", xp); o.put("gems", gems); o.put("streak", streak)
        o.put("day", day ?: JSONObject.NULL)
        o.put("dailyXp", dailyXp); o.put("dailyGoal", dailyGoal)
        val lj = JSONObject()
        for ((k, r) in lessons) {
            val entry = JSONObject().put("done", r.done).put("attempts", r.attempts)
                .put("stars", r.stars).put("lastAt", r.lastAt)
            if (r.placed) entry.put("placed", true)
            lj.put(k, entry)
        }
        o.put("lessons", lj)
        o.put("unitsDone", JSONArray(unitsDone.toList()))
        o.put("placedTier", placedTier); o.put("placed", placed); o.put("theme", theme)
        val ij = JSONObject()
        for ((k, r) in items) {
            ij.put(k, JSONObject().put("lvl", r.lvl).put("seen", r.seen).put("ok", r.ok).put("bad", r.bad).put("dueAt", r.dueAt).put("flag", r.flag))
        }
        o.put("items", ij)
        val aj = JSONObject()
        for ((k, t) in achievements) aj.put(k, t)
        o.put("achievements", aj)
        o.put(
            "stats", JSONObject()
                .put("answered", stats.answered).put("correct", stats.correct)
                .put("lessons", stats.lessons).put("reviews", stats.reviews)
                .put("perfects", stats.perfects).put("maxComboEver", stats.maxComboEver)
                .put("timeSpentSec", stats.timeSpentSec)
        )
        o.put("features", featureState.toJson())
        return o
    }

    companion object {
        fun fromJson(raw: String, cur: Curriculum? = null): AppState {
            val s = ProgressJson.parse(raw, cur)
            try {
                s.featureState = FeatureState.fromJson(JSONObject(raw).optJSONObject("features"))
            } catch (ignored: org.json.JSONException) {
                s.featureState = FeatureState()
            }
            return s
        }
    }
}

data class ResultSummary(
    val xp: Int,
    val acc: Float,
    val perfect: Boolean,
    val gems: Int,
    val unitComplete: CourseUnit?,
    val newAch: List<String>,
    val flowMultiplier: Int = 1,
    val chest: LootEntry? = null,
    val seeds: List<SeedEntry> = emptyList()
)

data class Achievement(
    val id: String,
    val icon: String,
    val name: Bi,
    val desc: Bi,
    val test: (AppState) -> Boolean
)

/* ---------------------------------------------------------------- engine */

object Engine {
    const val MAX_HEARTS = 5
    const val REVIEW_SIZE = 12
    val INTERVALS = intArrayOf(0, 1, 2, 4, 8, 16)
    private const val DAY_MS = 86400000L

    fun meaning(item: Item, lang: String) = if (lang == "bn") item.bi.bn else item.bi.en

    fun dayKey(ts: Long): String = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    fun todayXp(s: AppState, now: Long = System.currentTimeMillis()): Int =
        if (s.day == dayKey(now)) s.dailyXp else 0

    fun currentStreak(s: AppState, now: Long = System.currentTimeMillis()): Int {
        val today = LocalDate.parse(dayKey(now))
        return if (s.day == today.toString() || s.day == today.minusDays(1).toString()) s.streak else 0
    }

    /* ------------------------- progress ------------------------- */

    fun lessonDone(s: AppState, id: String) = s.lessons[id]?.done == true

    fun isUnlocked(s: AppState, lessonId: String, cur: Curriculum): Boolean {
        val ordered = cur.units.flatMap { it.lessons }
        val index = ordered.indexOf(lessonId)
        return index >= 0 && ordered.take(index).all { lessonDone(s, it) }
    }

    fun nextLessonId(s: AppState, cur: Curriculum): String? {
        for (u in cur.units) for (lid in u.lessons) if (!lessonDone(s, lid)) return lid
        return null
    }

    fun unitProgress(s: AppState, u: CourseUnit): Pair<Int, Int> {
        val done = u.lessons.count { lessonDone(s, it) }
        return done to u.lessons.size
    }

    fun doneLessons(s: AppState) = s.lessons.values.count { it.done }

    /* ------------------------- SRS ------------------------- */

    fun seenCount(s: AppState) = s.items.values.count { it.seen > 0 }

    fun learnedCount(s: AppState) = s.items.count { (id, r) -> r.lvl >= 1 && id.startsWith("w_") }

    fun masteredCount(s: AppState) = s.items.count { (id, r) -> r.lvl >= 3 && id.startsWith("w_") }

    private fun sortByNeed(s: AppState, ids: List<String>): List<String> = ids.sortedWith(
        compareBy(
            { s.items[it]?.flag != true },
            { s.items[it]?.lvl ?: 0 },
            { s.items[it]?.dueAt ?: 0L }
        )
    )

    fun dueIds(s: AppState, now: Long = System.currentTimeMillis()): List<String> {
        val out = s.items.filter { (_, r) -> r.seen > 0 && r.dueAt <= now }.keys.toList()
        return sortByNeed(s, out)
    }

    fun reviewQueue(s: AppState, now: Long = System.currentTimeMillis()): List<String> {
        var q = dueIds(s, now).take(REVIEW_SIZE)
        if (q.size < 6) {
            val seen = s.items.filter { (id, r) -> r.seen > 0 && id !in q }.keys.toList()
            q = q + sortByNeed(s, seen).take(6 - q.size)
        }
        return q
    }

    fun coverage(s: AppState, cur: Curriculum): Float {
        var occ = 0
        for ((id, r) in s.items) {
            if (r.lvl >= 1) occ += cur.items[id]?.occ ?: 0
        }
        return 100f * occ / maxOf(1, cur.totalQuranWords)
    }

    /* ------------------------- compiler ------------------------- */

    private fun pickOptions(
        item: Item,
        correct: String,
        textOf: (Item) -> String,
        pools: List<List<Item>>,
        rng: Rng
    ): Pair<List<String>, Int> {
        val texts = mutableSetOf(correct, item.ar, item.bi.en, item.bi.bn)
        val out = mutableListOf(correct)
        for (pool in pools) {
            for (c in shuffled(pool, rng)) {
                if (out.size >= 4) break
                if (c.id == item.id) continue
                if (!texts.add(textOf(c))) continue
                out.add(textOf(c))
            }
            if (out.size >= 4) break
        }
        val sh = shuffled(out, rng)
        return sh to sh.indexOf(correct)
    }

    fun compileLesson(cur: Curriculum, lessonId: String, s: AppState, canListen: Boolean): List<Card> {
        val lesson = cur.lessons[lessonId] ?: return emptyList()
        val rng = Rng(lessonId + ":" + (s.lessons[lessonId]?.attempts ?: 0))
        val lang = s.lang
        val pools = listOf(cur.unitItems(lesson.unit), cur.allItems())
        val cards = ArrayList<Card>()
        val items = lesson.items
        val verses = lesson.verses
        val teaches = lesson.teaches
        val text: (Item) -> String = { if (lang == "bn") it.bi.bn else it.bi.en }

        fun addMc(item: Item?) {
            item ?: return
            val (opts, ans) = pickOptions(item, text(item), text, pools, rng)
            cards.add(Card.Mc(item.id, item, item.ar, opts, ans))
        }

        fun addMca(item: Item?) {
            item ?: return
            val (opts, ans) = pickOptions(item, item.ar, { it.ar }, pools, rng)
            cards.add(Card.Mca(item.id, item, text(item), opts, ans))
        }

        fun addRoot(item: Item?) {
            item ?: return
            val root = item.root ?: return
            val roots = mutableListOf(root)
            for (c in shuffled(pools[1], rng)) {
                if (roots.size >= 4) break
                val cr = c.root ?: continue
                if (cr !in roots) roots.add(cr)
            }
            val sh = shuffled(roots, rng)
            cards.add(Card.Root(item.id, item, sh, sh.indexOf(root)))
        }

        fun addMatch(group: List<Item>) {
            if (group.size < 3) return
            val chosen = group.take(4)
            val pairs = chosen.map { Card.MatchPair(it.id, it.ar, text(it)) }
            val rights = shuffled(pairs.indices.toList(), rng)
            cards.add(Card.Match(pairs, rights))
        }

        fun addListen(item: Item?) {
            item ?: return
            if (!canListen) return
            val (opts, ans) = pickOptions(item, item.ar, { it.ar }, pools, rng)
            cards.add(Card.Listen(item.id, item, opts, ans))
        }

        fun addBuild(v: Verse) {
            val inVerse = v.tokens.toSet()
            val extras = ArrayList<String>()
            for (c in shuffled(cur.tokens, rng)) {
                if (extras.size >= 2) break
                if (c !in inVerse) extras.add(c)
            }
            cards.add(Card.Build(v, shuffled(v.tokens + extras, rng)))
        }

        fun addBlank(v: Verse) {
            val candidates = v.tokens.indices.filter { i ->
                i >= 1 && v.tokens[i].replace(Regex("[^\u0600-\u06FF]"), "").length >= 4
            }
            val idx = if (candidates.isNotEmpty()) candidates[floor(rng.next() * candidates.size).toInt()]
            else maxOf(0, v.tokens.size - 1)
            val correct = v.tokens[idx]
            val opts = mutableListOf(correct)
            for (c in shuffled(cur.tokens, rng)) {
                if (opts.size >= 4) break
                if (c != correct && c !in v.tokens && c !in opts) opts.add(c)
            }
            val sh = shuffled(opts, rng)
            cards.add(Card.Blank(v.id, v, idx, sh, sh.indexOf(correct)))
        }

        fun addRead(v: Verse) {
            v.context?.let { cards.add(Card.Context(v)) }
            cards.add(Card.Read(v))
        }

        teaches.getOrNull(0)?.let { cards.add(Card.Teach(it)) }
        addMc(items.getOrNull(0))
        addMc(items.getOrNull(1))
        addMatch(items.take(4))
        teaches.getOrNull(1)?.let { cards.add(Card.Teach(it)) }
        addMca(items.getOrNull(2))
        addMc(items.getOrNull(3))
        if (canListen) {
            addListen(items.lastOrNull())
            addListen(items.getOrNull(2))
        }
        items.filter { it.root != null }.take(2).forEach { addRoot(it) }

        verses.getOrNull(0)?.let { v ->
            addRead(v)
            if (v.tokens.size <= 12) addBuild(v)
            addBlank(v)
        }
        teaches.getOrNull(2)?.let { cards.add(Card.Teach(it)) }
        items.drop(4).forEachIndexed { i, x -> if (i % 2 == 0) addMc(x) else addMca(x) }
        verses.getOrNull(1)?.let { v ->
            addRead(v)
            if (v.tokens.size <= 12) addBuild(v)
            addBlank(v)
        }
        teaches.drop(3).forEach { cards.add(Card.Teach(it)) }
        verses.drop(2).forEach { v ->
            addRead(v)
            if (v.tokens.size <= 12) addBuild(v)
            addBlank(v)
        }
        injectForge(cards, items)
        cards.add(Card.Complete(lessonId))
        return cards
    }

    /** One forge card between the last verse and completion when the lesson taught
     * at least two roots. Silently skipped when the payload has no moulds. */
    private fun injectForge(cards: ArrayList<Card>, items: List<Item>) {
        val roots = items.mapNotNull { it.root }.toSet()
        if (roots.size < 2) return
        val round = FeaturesEngine.forgeRound(kotlin.random.Random) ?: return
        val at = cards.indexOfLast { it is Card.Complete }.let { if (it < 0) cards.size else it }
        cards.add(at, Card.Forge("forge:" + round.mould.id, round))
    }

    fun compileReview(cur: Curriculum, s: AppState, ids: List<String>, canListen: Boolean): List<Card> {
        val rng = Rng("review:" + dayKey(System.currentTimeMillis()) + ":" + ids.joinToString(","))
        val pools = listOf(cur.allItems(), cur.allItems())
        val cards = ArrayList<Card>()
        val text: (Item) -> String = { if (s.lang == "bn") it.bi.bn else it.bi.en }
        val pool = ids.distinct().mapNotNull { cur.items[it] }

        fun addMc(item: Item?) {
            item ?: return
            val (opts, ans) = pickOptions(item, text(item), text, pools, rng)
            cards.add(Card.Mc(item.id, item, item.ar, opts, ans))
        }

        fun addMca(item: Item?) {
            item ?: return
            val (opts, ans) = pickOptions(item, item.ar, { it.ar }, pools, rng)
            cards.add(Card.Mca(item.id, item, text(item), opts, ans))
        }

        fun addRoot(item: Item?) {
            item ?: return
            val root = item.root ?: return
            val roots = mutableListOf(root)
            for (c in shuffled(pools[1], rng)) {
                if (roots.size >= 4) break
                val cr = c.root ?: continue
                if (cr !in roots) roots.add(cr)
            }
            val sh = shuffled(roots, rng)
            cards.add(Card.Root(item.id, item, sh, sh.indexOf(root)))
        }

        fun addListen(item: Item?) {
            item ?: return
            if (!canListen) return
            val (opts, ans) = pickOptions(item, item.ar, { it.ar }, pools, rng)
            cards.add(Card.Listen(item.id, item, opts, ans))
        }

        addMc(pool.getOrNull(0))
        addMc(pool.getOrNull(1))
        if (pool.size >= 3) {
            val chosen = pool.take(4)
            val pairs = chosen.map { Card.MatchPair(it.id, it.ar, text(it)) }
            cards.add(Card.Match(pairs, shuffled(pairs.indices.toList(), rng)))
        }
        pool.forEachIndexed { i, item ->
            if (i < 2) return@forEachIndexed
            when (i % 4) {
                2 -> if (item.root != null) addRoot(item) else addMc(item)
                3 -> if (canListen) addListen(item) else addMc(item)
                1 -> addMca(item)
                else -> addMc(item)
            }
        }
        cards.add(Card.Complete("review"))
        return cards
    }

    /* ------------------------- results ------------------------- */

    fun applyLessonResult(cur: Curriculum, s: AppState, run: LessonRun, now: Long = System.currentTimeMillis()): ResultSummary {
        val acc = if (run.total > 0) run.correct.toFloat() / run.total else 1f
        val perfect = run.total > 0 && run.mistakes == 0
        val isReview = run.mode == "review"
        var xp = if (isReview) 5 + (3 * acc).roundToInt() else 10 + (5 * acc).roundToInt()
        if (perfect) xp += 5
        xp += min(3, floor(run.comboMax / 5.0).toInt()) * 2

        var unitComplete: CourseUnit? = null
        if (!isReview && run.lessonId != null) {
            val lid = run.lessonId
            val rec = s.lessons.getOrPut(lid) { LessonRec() }
            rec.done = true
            rec.attempts += 1
            val stars = if (acc >= 1f) 3 else if (acc >= 0.8f) 2 else 1
            rec.stars = maxOf(rec.stars, stars)
            rec.lastAt = now
            val u = cur.unitOf(lid)
            if (u != null && u.lessons.all { lessonDone(s, it) } && s.unitsDone.add(u.id) ) {
                unitComplete = u
                xp += 20
            }
        }
        var gems = if (perfect) 2 else 1
        if (unitComplete != null) gems += 5
        s.xp += xp
        s.gems += gems

        val dk = dayKey(now)
        if (s.day != dk) {
            s.streak = if (s.day == LocalDate.parse(dk).minusDays(1).toString()) s.streak + 1 else 1
            s.day = dk
            s.dailyXp = 0
        }
        s.dailyXp += xp

        if (isReview) s.stats.reviews += 1 else s.stats.lessons += 1
        s.stats.answered += run.total
        s.stats.correct += run.correct
        if (perfect) s.stats.perfects += 1
        s.stats.maxComboEver = maxOf(s.stats.maxComboEver, run.comboMax)
        s.stats.timeSpentSec += run.secs

        for ((id, t) in run.touches) {
            if (id !in cur.items) continue // Verse exercises affect accuracy, never vocabulary SRS.
            val isNew = id !in s.items || s.items.getValue(id).seen == 0
            val r = s.items.getOrPut(id) { SrsRec() }
            r.seen += 1
            r.ok += t[0]
            r.bad += t[1]
            if (t[1] > 0) {
                r.lvl = maxOf(0, r.lvl - 1)
                r.flag = true
                r.dueAt = now
            } else {
                if (isNew || r.dueAt <= now) {
                    r.lvl = min(5, r.lvl + 1)
                    r.dueAt = now + INTERVALS[r.lvl] * DAY_MS
                }
                r.flag = false
            }
        }

        val newAch = evaluateAchievements(s)
        /* ---------------- feature layer (flow multiplier, chest, seeds) -------- */
        val rootsBefore = FeaturesEngine.knownRoots(s, cur)
        val flow = FeaturesEngine.flowMultiplier(run.comboMax)
        val flowBonus = FeaturesEngine.flowXp(xp, run.comboMax) - xp
        val flowTotal = FeaturesEngine.flowXp(xp, run.comboMax)
        if (flowBonus > 0) {
            s.xp += flowBonus
            s.dailyXp += flowBonus
        }
        val chest = FeaturesEngine.rollChest(run.total, run.mistakes, kotlin.random.Random)
        if (chest != null) FeaturesEngine.applyLoot(s.featureState, chest)
        s.featureState.circle.tick(true)
        val freshRoots = FeaturesEngine.knownRoots(s, cur).filter { it !in rootsBefore }.toSet()
        val freshSeeds = FeaturesEngine.seedsForRoots(freshRoots)
        return ResultSummary(flowTotal, acc, perfect, gems, unitComplete, newAch, flow, chest, freshSeeds)
    }

    /* ------------------------- achievements ------------------------- */

    val ACHIEVEMENTS: List<Achievement> = listOf(
        Achievement("first_lesson", "🌱", Bi("First Steps", "প্রথম পদক্ষেপ"), Bi("Complete your first lesson", "প্রথম পাঠ সম্পন্ন করুন")) { it.stats.lessons >= 1 },
        Achievement("lessons_10", "📚", Bi("Ten Lessons", "দশটি পাঠ"), Bi("Complete 10 lessons", "১০টি পাঠ শেষ করুন")) { it.stats.lessons >= 10 },
        Achievement("streak_3", "🔥", Bi("Warming Up", "গতি এসেছে"), Bi("Reach a 3-day streak", "৩ দিনের স্ট্রিক")) { it.streak >= 3 },
        Achievement("streak_7", "☀️", Bi("Consistent", "নিয়মিত"), Bi("Reach a 7-day streak", "৭ দিনের স্ট্রিক")) { it.streak >= 7 },
        Achievement("xp_100", "⚡", Bi("Century", "শতক"), Bi("Earn 100 XP", "১০০ XP অর্জন")) { it.xp >= 100 },
        Achievement("xp_500", "🌟", Bi("Five Hundred", "পাঁচশ"), Bi("Earn 500 XP", "৫০০ XP অর্জন")) { it.xp >= 500 },
        Achievement("perfect", "🎯", Bi("Flawless", "নিখুঁত"), Bi("Finish a lesson with no mistakes", "একটি পাঠ ভুল ছাড়া শেষ করুন")) { it.stats.perfects >= 1 },
        Achievement("unit1", "🏛️", Bi("Foundations Set", "ভিত্তি স্থাপিত"), Bi("Complete Unit 1", "ইউনিট ১ সম্পন্ন করুন")) { lessonDone(it, "u1l3") },
        Achievement("words_25", "📖", Bi("25 Words", "২৫ শব্দ"), Bi("Learn 25 words", "২৫টি শব্দ শিখুন")) { learnedCount(it) >= 25 },
        Achievement("words_50", "🕌", Bi("50 Words", "৫০ শব্দ"), Bi("Learn 50 words", "৫০টি শব্দ শিখুন")) { learnedCount(it) >= 50 },
        Achievement("review_1", "🔁", Bi("Reviewer", "রিভিউয়ার"), Bi("Complete a review session", "একটি রিভিউ সেশন শেষ করুন")) { it.stats.reviews >= 1 },
        Achievement("halfway", "🧭", Bi("Halfway", "অর্ধেক পথ"), Bi("Complete 12 lessons", "১২টি পাঠ শেষ করুন")) { doneLessons(it) >= 12 }
    )

    fun evaluateAchievements(s: AppState): List<String> {
        val got = ArrayList<String>()
        for (a in ACHIEVEMENTS) {
            if (!s.achievements.containsKey(a.id) && a.test(s)) {
                s.achievements[a.id] = System.currentTimeMillis()
                got.add(a.id)
            }
        }
        return got
    }
}

/* ---------------------------------------------------------------- lesson run */

class LessonRun(val mode: String, val lessonId: String?, val cards: List<Card>) {
    /* Flow (feature 1): consecutive fast correct answers raise the multiplier. */
    val flow by mutableStateOf(FlowState())
    var i by mutableStateOf(0)
    var hearts by mutableStateOf(Engine.MAX_HEARTS)
    var combo by mutableStateOf(0)
    var comboMax by mutableStateOf(0)
    var correct by mutableStateOf(0)
    var total by mutableStateOf(0)
    var mistakes by mutableStateOf(0)
    var phase by mutableStateOf("answer") // answer | feedback | failed
    var selected by mutableStateOf<Int?>(null)
    var gradedOk by mutableStateOf<Boolean?>(null)
    var gradedText by mutableStateOf("")
    var buildSel by mutableStateOf<List<Int>>(emptyList())
    var matchSelLeft by mutableStateOf<String?>(null)
    var matchDone by mutableStateOf<Set<String>>(emptySet())
    var matchFlash by mutableStateOf<String?>(null)
    var readRevealed by mutableStateOf<Set<String>>(emptySet())
    val touches = HashMap<String, IntArray>()
    val startedAt = System.currentTimeMillis()
    var finalized = false
    var secs = 0

    val cur: Card? get() = cards.getOrNull(i)

    fun touch(id: String?, ok: Boolean) {
        if (id == null) return
        val r = touches.getOrPut(id) { intArrayOf(0, 0) }
        if (ok) r[0]++ else r[1]++
    }

    fun record(id: String?, ok: Boolean) {
        total++
        val now = System.currentTimeMillis()
        if (ok && flow.lastAt > 0 && now - flow.lastAt > FeaturesEngine.F.comboWindowSec * 1000L) {
            flow.streak = 0
        }
        flow.streak = if (ok) flow.streak + 1 else 0
        flow.lastAt = now
        flow.multiplier = FeaturesEngine.flowMultiplier(flow.streak)
        flow.semitones = FeaturesEngine.flowSemitones(flow.streak)
        if (ok) {
            correct++
            combo++
            comboMax = maxOf(comboMax, combo)
            touch(id, true)
        } else {
            mistakes++
            combo = 0
            hearts--
            touch(id, false)
            if (hearts <= 0) phase = "failed"
        }
    }
}

/** One whisper-boss battle (feature 4). Kept by the controller; only the earned
 * XP and shields are durable. */
class BossSession(val waves: List<BossWave>) {
    var index by mutableStateOf(0)
    var hits by mutableStateOf(0)
    var wrong by mutableStateOf(0)
    var done by mutableStateOf(false)
    val startedAt = System.currentTimeMillis()
    val endsAt = startedAt + FeaturesEngine.F.bossSeconds * 1000L
    val wave: BossWave get() = waves[index]

    fun expired(): Boolean = System.currentTimeMillis() >= endsAt
    fun secondsLeft(): Int = maxOf(0, ((endsAt - System.currentTimeMillis()) / 1000).toInt())
}
