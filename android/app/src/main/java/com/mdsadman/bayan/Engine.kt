package com.mdsadman.bayan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
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

class LessonRec { var done = false; var attempts = 0; var stars = 0; var lastAt = 0L }
class SrsRec { var lvl = 0; var seen = 0; var ok = 0; var bad = 0; var dueAt = 0L; var flag = false }
class Stats {
    var answered = 0; var correct = 0; var lessons = 0; var reviews = 0
    var perfects = 0; var maxComboEver = 0; var timeSpentSec = 0
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
    val items = LinkedHashMap<String, SrsRec>()
    val achievements = LinkedHashMap<String, Long>()
    val stats = Stats()

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("v", v); o.put("onboarded", onboarded); o.put("lang", lang); o.put("sound", sound)
        o.put("xp", xp); o.put("gems", gems); o.put("streak", streak)
        o.put("day", day ?: JSONObject.NULL)
        o.put("dailyXp", dailyXp); o.put("dailyGoal", dailyGoal)
        val lj = JSONObject()
        for ((k, r) in lessons) {
            lj.put(k, JSONObject().put("done", r.done).put("attempts", r.attempts).put("stars", r.stars).put("lastAt", r.lastAt))
        }
        o.put("lessons", lj)
        o.put("unitsDone", JSONArray(unitsDone.toList()))
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
        return o
    }

    companion object {
        fun fromJson(raw: String): AppState {
            val s = AppState()
            runCatching {
                val o = JSONObject(raw)
                s.v = o.optInt("v", 1)
                s.onboarded = o.optBoolean("onboarded", false)
                s.lang = o.optString("lang", "en")
                s.sound = o.optBoolean("sound", true)
                s.xp = o.optInt("xp", 0); s.gems = o.optInt("gems", 0); s.streak = o.optInt("streak", 0)
                s.day = if (o.isNull("day")) null else o.optString("day")
                s.dailyXp = o.optInt("dailyXp", 0); s.dailyGoal = o.optInt("dailyGoal", 30)
                o.optJSONObject("lessons")?.let { lj ->
                    for (k in lj.keys()) {
                        val e = lj.optJSONObject(k) ?: continue
                        s.lessons[k] = LessonRec().apply {
                            done = e.optBoolean("done"); attempts = e.optInt("attempts")
                            stars = e.optInt("stars"); lastAt = e.optLong("lastAt")
                        }
                    }
                }
                o.optJSONArray("unitsDone")?.let { arr ->
                    for (i in 0 until arr.length()) s.unitsDone.add(arr.optString(i))
                }
                o.optJSONObject("items")?.let { ij ->
                    for (k in ij.keys()) {
                        val e = ij.optJSONObject(k) ?: continue
                        s.items[k] = SrsRec().apply {
                            lvl = e.optInt("lvl"); seen = e.optInt("seen"); ok = e.optInt("ok")
                            bad = e.optInt("bad"); dueAt = e.optLong("dueAt"); flag = e.optBoolean("flag")
                        }
                    }
                }
                o.optJSONObject("achievements")?.let { aj ->
                    for (k in aj.keys()) s.achievements[k] = aj.optLong(k)
                }
                o.optJSONObject("stats")?.let { e ->
                    s.stats.answered = e.optInt("answered"); s.stats.correct = e.optInt("correct")
                    s.stats.lessons = e.optInt("lessons"); s.stats.reviews = e.optInt("reviews")
                    s.stats.perfects = e.optInt("perfects"); s.stats.maxComboEver = e.optInt("maxComboEver")
                    s.stats.timeSpentSec = e.optInt("timeSpentSec")
                }
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
    val newAch: List<String>
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

    fun dayKey(ts: Long): String {
        val c = Calendar.getInstance()
        c.timeInMillis = ts
        return "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }

    /* ------------------------- progress ------------------------- */

    fun lessonDone(s: AppState, id: String) = s.lessons[id]?.done == true

    fun isUnlocked(s: AppState, lessonId: String, cur: Curriculum): Boolean {
        val u = cur.unitOf(lessonId) ?: return false
        val idx = u.lessons.indexOf(lessonId)
        if (idx == 0) {
            val ui = cur.units.indexOfFirst { it.id == u.id }
            if (ui <= 0) return true
            val prev = cur.units[ui - 1]
            return prev.lessons.all { lessonDone(s, it) }
        }
        return lessonDone(s, u.lessons[idx - 1])
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
            else maxOf(1, v.tokens.size - 1)
            val correct = v.tokens[idx]
            val opts = mutableListOf(correct)
            for (c in shuffled(cur.tokens, rng)) {
                if (opts.size >= 4) break
                if (c != correct && c !in v.tokens) opts.add(c)
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
        cards.add(Card.Complete(lessonId))
        return cards
    }

    fun compileReview(cur: Curriculum, s: AppState, ids: List<String>, canListen: Boolean): List<Card> {
        val rng = Rng("review:" + dayKey(System.currentTimeMillis()) + ":" + ids.joinToString(","))
        val pools = listOf(cur.allItems(), cur.allItems())
        val cards = ArrayList<Card>()
        val text: (Item) -> String = { if (s.lang == "bn") it.bi.bn else it.bi.en }
        val pool = ids.mapNotNull { cur.items[it] }

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

    fun applyLessonResult(cur: Curriculum, s: AppState, run: LessonRun): ResultSummary {
        val now = System.currentTimeMillis()
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
            s.streak = if (s.day == null) 1 else s.streak + 1
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
            val r = s.items.getOrPut(id) { SrsRec() }
            r.seen += 1
            if (t[1] > 0) {
                r.bad += t[1]
                r.lvl = maxOf(0, r.lvl - 1)
                r.flag = true
            } else {
                r.ok += t[0]
                r.lvl = min(5, r.lvl + 1)
                r.flag = false
            }
            r.dueAt = now + INTERVALS[r.lvl] * DAY_MS
        }

        val newAch = evaluateAchievements(s)
        return ResultSummary(xp, acc, perfect, gems, unitComplete, newAch)
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
