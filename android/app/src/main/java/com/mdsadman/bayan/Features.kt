package com.mdsadman.bayan

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random

/** Parsed copy of curriculum/features.json — one source for the game, wisdom and rite payloads. */
data class LootEntry(val id: String, val kind: String, val rarity: String, val title: Bi, val body: Bi, val root: String?, val verse: String?)
data class Rarity(val id: String, val weight: Int, val label: Bi)
data class MouldExample(val root: String, val word: String, val meaning: Bi)
data class Mould(val id: String, val pattern: String, val role: Bi, val examples: List<MouldExample>)
data class BossWave(val id: String, val trap: Bi, val options: List<Bi>, val answer: Int, val note: Bi)
data class SeedEntry(val id: String, val kind: String, val root: String?, val word: String, val title: Bi, val body: Bi)
data class SalahStep(val id: String, val posture: Bi, val ar: String, val bi: Bi, val grammar: Bi)
data class WaqtWindow(val id: String, val name: Bi, val window: String, val verse: String, val focus: Bi)
data class RiteTerm(val id: String, val term: String, val root: String, val title: Bi, val body: Bi)
data class JumuahPhrase(val id: String, val ar: String, val bi: Bi)
data class SawmDrill(val id: String, val verse: String, val ask: Bi, val options: List<String>, val answer: Int)
data class WhisperLine(val id: String, val root: String, val line: Bi)

data class Features(
    val comboSteps: List<Int>,
    val comboSemitones: List<Int>,
    val comboWindowSec: Int,
    val chestChance: Float,
    val rarities: List<Rarity>,
    val loot: List<LootEntry>,
    val forgeMoulds: List<Mould>,
    val bossSeconds: Int,
    val bossWaves: List<BossWave>,
    val seeds: List<SeedEntry>,
    val salahSteps: List<SalahStep>,
    val waqtWindows: List<WaqtWindow>,
    val wuduAdhan: List<RiteTerm>,
    val zakatHajj: List<RiteTerm>,
    val jumuahPhrases: List<JumuahPhrase>,
    val sawmDrills: List<SawmDrill>,
    val whispers: List<WhisperLine>,
    val placement: PlacementCfg = PlacementCfg()
)

/** Placement-test tuning (features.json -> placement). Units are the tiers. */
data class PlacementCfg(
    val perTier: Int = 4,
    val sampleSize: Int = 16,
    val passRate: Float = 0.75f
)

/** Device-local feature state (loot, shields, ḥalaqah circle) — never part of the portable v1 backup. */
class FeatureState {
    val chests = LinkedHashMap<String, Int>()
    val collection = LinkedHashSet<String>()
    var shields = 0
    val circle = HalaqahCircle()

    companion object {
        fun fromJson(o: JSONObject?): FeatureState {
            val f = FeatureState()
            if (o == null) return f
            val cj = o.optJSONObject("chests")
            if (cj != null) for (id in cj.keys()) f.chests[id] = min(cj.optInt(id, 0), 1_000)
            val col = o.optJSONArray("collection")
            if (col != null) for (i in 0 until col.length()) f.collection.add(col.optString(i))
            f.shields = min(o.optInt("shields", 0), 1_000)
            f.circle.fromJson(o.optJSONObject("circle"))
            return f
        }
    }

    fun toJson(): JSONObject {
        val o = JSONObject()
        val cj = JSONObject()
        for ((id, n) in chests) cj.put(id, n)
        o.put("chests", cj)
        o.put("collection", JSONArray(collection.toList()))
        o.put("shields", shields)
        o.put("circle", circle.toJson())
        return o
    }
}

class HalaqahCircle {
    var cap = 5
    val members = ArrayList<String>()
    var lantern = 0f // 0..1
    var lastHelped = 0L

    fun add(name: String?): Boolean {
        val clean = (name ?: "").trim().take(24)
        if (clean.isEmpty() || members.size >= cap || clean in members) return false
        members.add(clean)
        return true
    }

    fun tick(practicedToday: Boolean) {
        val active = 1f / maxOf(2f, (members.size + 1).toFloat())
        lantern = if (practicedToday) min(1f, lantern + active) else max(0f, lantern - active / 2f)
    }

    fun lendShield(): Boolean {
        if (lantern >= 1f) return false
        lantern = min(1f, lantern + 0.25f)
        lastHelped = System.currentTimeMillis()
        return true
    }

    fun fromJson(o: JSONObject?) {
        if (o == null) return
        cap = min(maxOf(2, o.optInt("cap", 5)), 12)
        members.clear()
        val ms = o.optJSONArray("members")
        if (ms != null) for (i in 0 until ms.length()) {
            val name = ms.optString(i)
            if (name.isNotEmpty() && name !in members) members.add(name)
        }
        lantern = min(maxOf(0f, o.optDouble("lantern", 0.0).toFloat()), 1f)
        lastHelped = o.optLong("lastHelped", 0L)
    }

    fun toJson(): JSONObject = JSONObject()
        .put("cap", cap)
        .put("members", JSONArray(members))
        .put("lantern", lantern.toDouble())
        .put("lastHelped", lastHelped)
}

/** Feature engine — a 1:1 port of web/js/features.js, pure logic. */
object FeaturesEngine {
    private const val ARABIC_START = '\u0621'
    private const val ARABIC_END = '\u064A'

    var F: Features = Features(
        listOf(2, 3, 4, 5, 6, 8), listOf(0, 2, 4, 5, 7, 9), 8, 0.55f,
        emptyList(), emptyList(), emptyList(), 45, emptyList(), emptyList(),
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
    )

    fun load(json: String?): Features {
        if (json.isNullOrBlank()) return F
        val root = JSONObject(json)
        val combo = root.optJSONObject("combo") ?: JSONObject()
        val steps = combo.optJSONArray("steps") ?: JSONArray()
        val semis = combo.optJSONArray("semitones") ?: JSONArray()
        val comboSteps = ArrayList<Int>()
        val comboSemitones = ArrayList<Int>()
        for (i in 0 until steps.length()) comboSteps.add(steps.getInt(i))
        for (i in 0 until semis.length()) comboSemitones.add(semis.getInt(i))

        val chests = root.optJSONObject("chests") ?: JSONObject()
        val rarities = ArrayList<Rarity>()
        chests.optJSONArray("rarities")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                rarities.add(Rarity(o.getString("id"), o.optInt("weight", 1), Bi.from(o.optJSONObject("label"))))
            }
        }
        val loot = ArrayList<LootEntry>()
        chests.optJSONArray("loot")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                loot.add(
                    LootEntry(
                        o.getString("id"), o.optString("kind", "relic"), o.optString("rarity", "common"),
                        Bi.from(o.optJSONObject("title")), Bi.from(o.optJSONObject("body")),
                        optStr(o, "root"), optStr(o, "verse")
                    )
                )
            }
        }

        val forge = root.optJSONObject("forge") ?: JSONObject()
        val moulds = ArrayList<Mould>()
        forge.optJSONArray("moulds")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val examples = ArrayList<MouldExample>()
                o.optJSONArray("examples")?.let { ex ->
                    for (j in 0 until ex.length()) {
                        val e = ex.getJSONObject(j)
                        examples.add(MouldExample(e.getString("root"), e.getString("word"), Bi.from(e.optJSONObject("meaning"))))
                    }
                }
                moulds.add(Mould(o.getString("id"), o.getString("pattern"), Bi.from(o.optJSONObject("role")), examples))
            }
        }

        val boss = root.optJSONObject("boss") ?: JSONObject()
        val waves = ArrayList<BossWave>()
        boss.optJSONArray("waves")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val options = ArrayList<Bi>()
                o.optJSONArray("options")?.let { op -> for (j in 0 until op.length()) options.add(Bi.from(op.getJSONObject(j))) }
                waves.add(
                    BossWave(
                        o.getString("id"), Bi.from(o.optJSONObject("trap")), options, o.optInt("answer", 0),
                        Bi.from(o.optJSONObject("note"))
                    )
                )
            }
        }

        val seedsRoot = root.optJSONObject("seeds") ?: JSONObject()
        val seeds = ArrayList<SeedEntry>()
        seedsRoot.optJSONArray("items")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                seeds.add(
                    SeedEntry(
                        o.getString("id"), o.optString("kind", "etymology"), optStr(o, "root"),
                        o.optString("word", ""), Bi.from(o.optJSONObject("title")), Bi.from(o.optJSONObject("body"))
                    )
                )
            }
        }

        val rites = root.optJSONObject("rites") ?: JSONObject()
        val salah = ArrayList<SalahStep>()
        rites.optJSONObject("salah")?.optJSONArray("steps")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                salah.add(SalahStep(o.getString("id"), Bi.from(o.optJSONObject("posture")), o.getString("ar"),
                    Bi(o.optString("en"), o.optString("bn")), Bi.from(o.optJSONObject("grammar"))))
            }
        }
        val waqt = ArrayList<WaqtWindow>()
        rites.optJSONObject("waqt")?.optJSONArray("windows")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                waqt.add(WaqtWindow(o.getString("id"), Bi.from(o.optJSONObject("name")), o.optString("window", ""),
                    o.getString("verse"), Bi.from(o.optJSONObject("focus"))))
            }
        }
        fun terms(key: String): List<RiteTerm> {
            val out = ArrayList<RiteTerm>()
            rites.optJSONObject(key)?.optJSONArray("items")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out.add(RiteTerm(o.getString("id"), o.getString("term"), o.optString("root", ""),
                        Bi.from(o.optJSONObject("title")), Bi.from(o.optJSONObject("body"))))
                }
            }
            return out
        }
        val jumuah = ArrayList<JumuahPhrase>()
        rites.optJSONObject("jumuah")?.optJSONArray("phrases")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                jumuah.add(JumuahPhrase(o.getString("id"), o.getString("ar"), Bi(o.optString("en"), o.optString("bn"))))
            }
        }
        val drills = ArrayList<SawmDrill>()
        rites.optJSONObject("sawm")?.optJSONArray("drills")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val options = ArrayList<String>()
                o.optJSONArray("options")?.let { op -> for (j in 0 until op.length()) options.add(op.getString(j)) }
                drills.add(SawmDrill(o.getString("id"), o.getString("verse"), Bi.from(o.optJSONObject("ask")), options, o.optInt("answer", 0)))
            }
        }

        val whispers = ArrayList<WhisperLine>()
        root.optJSONArray("whispers")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                whispers.add(WhisperLine(o.getString("id"), o.optString("root", ""), Bi.from(o.optJSONObject("line"))))
            }
        }

        F = Features(
            comboSteps, comboSemitones, combo.optInt("windowSec", 8),
            chests.optDouble("chance", 0.55).toFloat(), rarities, loot, moulds,
            boss.optInt("seconds", 45), waves, seeds, salah, waqt,
            terms("wuduAdhan"), terms("zakatHajj"), jumuah, drills, whispers,
            root.optJSONObject("placement")?.let { pl ->
                PlacementCfg(
                    pl.optInt("perTier", 4).coerceIn(1, 6),
                    pl.optInt("sampleSize", 16).coerceIn(4, 40),
                    pl.optDouble("passRate", 0.75).toFloat().coerceIn(0.5f, 1f)
                )
            } ?: PlacementCfg()
        )
        return F
    }

    /* ---------------------------------------------- 1. Noor flow combo */
    fun flowMultiplier(streak: Int): Int {
        if (streak <= 0) return 1
        val steps = F.comboSteps
        return steps[min(streak - 1, steps.size - 1)]
    }

    fun flowSemitones(streak: Int): Int {
        if (streak <= 0) return 0
        val semis = F.comboSemitones
        return semis[min(streak - 1, semis.size - 1)]
    }

    fun flowXp(baseXp: Int, streak: Int): Int = Math.round(baseXp.toFloat() * flowMultiplier(streak))
    fun pitchShift(semitones: Int): Float = 2f.pow(semitones / 12f)

    /* ------------------------------------------------ 2. Barakah chests */
    fun rollChest(runTotal: Int, runMistakes: Int, rng: Random): LootEntry? {
        if (runTotal <= 0) return null
        val chance = F.chestChance + if (runMistakes == 0) 0.2f else 0f
        if (rng.nextDouble() > min(1f, chance).toDouble()) return null
        val pool = F.loot.filter { it.rarity == pickRarity(rng) }
        val source = pool.ifEmpty { F.loot }
        if (source.isEmpty()) return null
        return source[rng.nextInt(source.size)]
    }

    private fun pickRarity(rng: Random): String {
        val total = F.rarities.sumOf { it.weight }.toFloat()
        if (total <= 0f) return F.rarities.firstOrNull()?.id ?: return ""
        var roll = rng.nextFloat() * total
        for (rarity in F.rarities) {
            roll -= rarity.weight
            if (roll <= 0f) return rarity.id
        }
        return F.rarities.last().id
    }

    fun applyLoot(state: FeatureState, entry: LootEntry) {
        if (entry.kind == "shield") state.shields += 1
        else state.chests[entry.id] = (state.chests[entry.id] ?: 0) + 1
        state.collection.add(entry.id)
    }

    /* A missed day spends a shield before it can break a streak. */
    /** A missed day (not yesterday) is covered by a shield on the WEB side where
     * streak recomputation lives; the Android engine already forbids streak
     * inflation, so this helper is only needed by callers that reset streaks.
     * Kept as a pure function so both sides stay testable. */
    fun resolveStreakGap(shields: Int, dayIsRecent: Boolean): Pair<Boolean, Int> {
        if (dayIsRecent) return false to shields
        if (shields > 0) return true to shields - 1
        return false to shields
    }

    /* ---------------------------------------------------- 3. Sarf forge */
    data class ForgeRound(val mould: Mould, val root: String, val word: String, val meaning: Bi, val options: List<String>, val answer: Int)

    fun forgeRound(rng: kotlin.random.Random): ForgeRound? {
        val moulds = F.forgeMoulds
        if (moulds.isEmpty()) return null
        val mould = moulds[rng.nextInt(moulds.size)]
        if (mould.examples.isEmpty()) return null
        val example = mould.examples[rng.nextInt(mould.examples.size)]
        val options = LinkedHashSet<String>()
        options.add(example.word)
        for (other in moulds) {
            if (other.id == mould.id) continue
            for (alt in other.examples) if (options.size < 3) options.add(alt.word)
        }
        for (alt in mould.examples) if (options.size < 3) options.add(alt.word)
        val order = options.toList().shuffled(rng)
        return ForgeRound(mould, example.root, example.word, example.meaning, order, order.indexOf(example.word))
    }

    /* ------------------------------------------------ 4. Whisper boss */
    fun bossQueue(rng: Random, count: Int): List<BossWave> =
        F.bossWaves.shuffled(rng).take(maxOf(1, min(count, F.bossWaves.size)))

    data class BossScore(val hits: Int, val total: Int, val accuracy: Float, val xp: Int, val shattered: Boolean, val shields: Int)

    fun bossScore(waves: List<BossWave>, hits: Int, secondsLeft: Int): BossScore {
        val total = waves.size
        val safe = maxOf(0, min(hits, total))
        val accuracy = if (total == 0) 0f else safe.toFloat() / total
        val speedBonus = maxOf(0, secondsLeft) / 5
        return BossScore(safe, total, accuracy, Math.round(6f * safe * accuracy) + speedBonus, total > 0 && safe == total, if (total > 0 && safe == total) 1 else 0)
    }

    /* ---------------------------------------------------- seeds (7-12) */
    fun seedsForRoots(roots: Set<String>): List<SeedEntry> =
        F.seeds.filter { it.root != null && it.root in roots }

    fun normalize(text: String): String {
        var out = ""
        for (c in text) {
            if (c in '\u064B'..'\u065F' || c == '\u0640' || c == '\u0670') continue // diacritics, tatweel, dagger alif
            when (c) {
                'أ', 'إ', 'آ' -> out += 'ا'
                in ARABIC_START..ARABIC_END -> out += c
                ' ' -> {}
                else -> out += c
            }
        }
        return out.lowercase()
    }

    fun carriesRoot(root: String?, word: String): Boolean {
        if (root.isNullOrBlank()) return false
        val letters = normalize(root).filter { it in ARABIC_START..ARABIC_END }
        if (letters.isEmpty()) return false
        val target = normalize(word)
        var cursor = 0
        for (c in target) {
            if (cursor < letters.length && c == letters[cursor]) cursor += 1
            if (cursor == letters.length) return true
        }
        return cursor == letters.length
    }

    /* ---------------------------------------------- 6. illumination map */
    data class LitVerse(val id: String, val ref: String, val lit: Boolean, val fraction: Float)
    data class Illumination(val roots: List<String>, val totalVerses: Int, val litVerses: Int, val percent: Int, val verses: List<LitVerse>)

    fun knownRoots(state: AppState, cur: Curriculum): Set<String> {
        val roots = LinkedHashSet<String>()
        for ((id, rec) in state.items) {
            if (rec.lvl >= 1) cur.items[id]?.root?.let { roots.add(it) }
        }
        return roots
    }

    fun illumination(state: AppState, cur: Curriculum): Illumination {
        val roots = knownRoots(state, cur)
        val verses = ArrayList<LitVerse>()
        for (unit in cur.units) for (lessonId in unit.lessons) {
            val lesson = cur.lessons[lessonId] ?: continue
            for (verse in lesson.verses) {
                var lit = 0
                for (token in verse.tokens) {
                    if (roots.any { carriesRoot(it, token) }) lit += 1
                }
                verses.add(LitVerse(verse.id, verse.ref, lit > 0, if (verse.tokens.isEmpty()) 0f else lit.toFloat() / verse.tokens.size))
            }
        }
        val litCount = verses.count { it.lit }
        return Illumination(roots.toList().sorted(), verses.size, litCount, if (verses.isEmpty()) 0 else Math.round(100f * litCount / verses.size), verses)
    }

    fun whisperFor(now: Long): WhisperLine? {
        if (F.whispers.isEmpty()) return null
        val day = now / 86400000L
        return F.whispers[((day % F.whispers.size) + F.whispers.size).toInt() % F.whispers.size]
    }

    /* ---------------------------------------------- 13-15, 23. rites */
    fun waqtWindow(hour: Int): WaqtWindow? {
        val windows = F.waqtWindows
        if (windows.isEmpty()) return null
        val id = when {
            hour < 8 -> "fajr"
            hour < 15 -> "dhuhr"
            hour < 18 -> "asr"
            hour < 20 -> "maghrib"
            else -> "isha"
        }
        return windows.firstOrNull { it.id == id } ?: windows.first()
    }

    fun jumuahOpen(dayOfWeek: Int): Boolean = dayOfWeek == 5 // java.util.Calendar.FRIDAY
    /* --------------------------------------- placement test (adaptive tiers) */

    /** Adaptive tier search: units are the ladder, 0 = the first unit.
     * A tier is judged once `perTier` questions are seen at it, passing at
     * `passRate`. Binary search converges in ~log2(units) rounds. */
    class PlacementSession(val tiers: Int) {
        var lo = -1
        var hi = tiers
        var probe = if (tiers > 1) (tiers - 1) / 2 else 0
        var tierCorrect = 0
        var tierWrong = 0
        var total = 0
        var correct = 0
        var done = false
        var current: PlacementQuestion? = null
        val asked = HashSet<String>()
        var result: PlacementResult? = null
    }

    class PlacementQuestion(val tier: Int, val itemId: String, val options: List<String>, val answer: Int)

    data class PlacementResult(val tier: Int, val correct: Int, val total: Int, val confident: Boolean) {
        val accuracy: Int get() = if (total == 0) 0 else Math.round(100f * correct / total)
    }

    private fun paramPerTier() = F.placement.perTier
    private fun paramBudget() = F.placement.sampleSize
    private fun paramRate() = F.placement.passRate

    /** Item ids taught in one unit, unseen ones first. */
    fun tierItems(cur: Curriculum, tier: Int, state: AppState?): List<String> {
        val unit = cur.units.getOrNull(tier) ?: return emptyList()
        val ids = ArrayList<String>()
        for (lessonId in unit.lessons) {
            val lesson = cur.lessons[lessonId] ?: continue
            for (item in lesson.items) if (ids.indexOf(item.id) < 0) ids.add(item.id)
        }
        val seen = state?.items ?: emptyMap()
        val unseen = ids.filter { id -> (seen[id]?.seen ?: 0) == 0 }
        return if (unseen.size >= 2) unseen else ids
    }

    fun placementStart(cur: Curriculum, state: AppState?): PlacementSession {
        val session = PlacementSession(cur.units.size.coerceAtLeast(1))
        return placementAdvance(session) ?: session
    }

    private fun placementAdvance(session: PlacementSession): PlacementSession? {
        if (session.done) return null
        if (session.total >= paramBudget() || session.hi - session.lo <= 1) {
            session.done = true
            session.result = placementResult(session)
            return null
        }
        val probe = ((session.lo + session.hi) / 2).coerceIn(0, session.tiers - 1)
        if (probe != session.probe) {
            session.probe = probe
            session.tierCorrect = 0
            session.tierWrong = 0
        }
        return session
    }

    fun placementQuestion(session: PlacementSession, cur: Curriculum, state: AppState?): PlacementQuestion? {
        if (session.done) return null
        val pool = tierItems(cur, session.probe, state)
        val itemId = pool.firstOrNull { !session.asked.contains(session.probe.toString() + ":" + it) }
            ?: pool.firstOrNull()
        if (itemId == null) {
            session.done = true
            session.result = placementResult(session)
            return null
        }
        session.asked.add(session.probe.toString() + ":" + itemId)
        val others = ArrayList<String>()
        for (t in 0 until session.tiers) {
            if (t == session.probe) continue
            for (id in tierItems(cur, t, state)) if (others.indexOf(id) < 0) others.add(id)
        }
        val rand = kotlin.random.Random(seedOf("placement:" + session.probe + ":" + itemId))
        val options = ArrayList<String>()
        options.add(itemId)
        val shuffled = others.shuffled(rand).take(3)
        options.addAll(shuffled)
        options.shuffle(rand)
        val question = PlacementQuestion(session.probe, itemId, options, options.indexOf(itemId))
        session.current = question
        return question
    }

    fun placementAnswer(session: PlacementSession, ok: Boolean, cur: Curriculum, state: AppState?): PlacementSession {
        if (session.done) return session
        session.total += 1
        if (ok) { session.correct += 1; session.tierCorrect += 1 } else session.tierWrong += 1
        val need = Math.max(1, Math.ceil((paramPerTier() * paramRate()).toDouble()).toInt())
        val miss = paramPerTier() - need + 1
        if (session.tierCorrect >= need) session.lo = Math.max(session.lo, session.probe)
        else if (session.tierWrong >= miss) session.hi = Math.min(session.hi, session.probe)
        placementAdvance(session)
        if (!session.done) placementQuestion(session, cur, state)
        return session
    }

    fun placementResult(session: PlacementSession): PlacementResult =
        PlacementResult(
            Math.max(0, session.lo), session.correct, session.total,
            session.total >= Math.min(6, paramBudget())
        )

    /** Mark the units below `tier` as placed: unlocks the path, grants no XP or
     * stars, and never rewrites a lesson the learner finished themselves. */
    fun applyPlacement(state: AppState, tier: Int, cur: Curriculum): Int {
        val target = tier.coerceIn(0, cur.units.size)
        var skipped = 0
        for (i in 0 until target) {
            val unit = cur.units[i]
            for (lessonId in unit.lessons) {
                val rec = state.lessons[lessonId]
                if (rec != null && rec.done && !rec.placed) continue
                if (rec == null || !rec.done) skipped += 1
                val entry = rec ?: LessonRec()
                entry.done = true; entry.placed = true; entry.attempts = 0; entry.stars = 0; entry.lastAt = 0L
                state.lessons[lessonId] = entry
            }
            state.unitsDone.add(unit.id)
        }
        state.placedTier = target
        state.placed = target > 0
        return skipped
    }

    /** Remove exactly the lessons this feature placed. */
    fun undoPlacement(state: AppState, cur: Curriculum): Int {
        var removed = 0
        for (unit in cur.units) {
            for (lessonId in unit.lessons) {
                val rec = state.lessons[lessonId] ?: continue
                if (rec.placed) { state.lessons.remove(lessonId); removed += 1 }
            }
            val stillDone = unit.lessons.all { state.lessons[it]?.done == true }
            if (!stillDone) state.unitsDone.remove(unit.id)
        }
        state.placedTier = 0
        state.placed = false
        return removed
    }

    private fun seedOf(seed: String): Int {
        var h = -0x7ee3623b
        for (c in seed) h = h * 31 + c.code
        return h
    }
}

/** optString() only accepts non-null defaults in Kotlin; this keeps "absent" as null. */
internal fun optStr(o: org.json.JSONObject, key: String): String? =
    if (o.has(key) && !o.isNull(key)) o.getString(key) else null
