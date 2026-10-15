package com.mdsadman.bayan

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDate

/** Portable v1 progress. Parse a fresh object completely before any live state is replaced. */
object ProgressJson {
    const val MAX_BYTES = 2 * 1024 * 1024
    private const val MAX_COUNT = 1_000_000_000L
    private const val MAX_TIME = 8_640_000_000_000_000L // JavaScript Date range, milliseconds.

    fun parse(raw: String, cur: Curriculum? = null): AppState {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds 2 MB" }
        try {
            val tokener = JSONTokener(raw.removePrefix("\uFEFF"))
            val o = tokener.nextValue() as? JSONObject ?: error("Expected a progress object")
            require(tokener.nextClean() == '\u0000') { "Unexpected text after progress" }
            require(o.has("v") && number(o, "v", 0, 1, 1) == 1L) { "Only progress version 1 is supported" }
            val s = AppState()
            s.onboarded = bool(o, "onboarded", false)
            s.lang = string(o, "lang", "en").also { require(it in setOf("en", "bn")) { "Unsupported language" } }
            s.sound = bool(o, "sound", true)
            s.xp = count(o, "xp"); s.gems = count(o, "gems"); s.streak = count(o, "streak")
            s.day = if (!o.has("day") || o.isNull("day")) null else string(o, "day", "").also {
                require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(it)) { "Invalid activity date" }
                require(LocalDate.parse(it).toString() == it) { "Invalid activity date" }
            }
            s.dailyXp = count(o, "dailyXp")
            s.dailyGoal = number(o, "dailyGoal", 30, 1, 1000).toInt()
            objectOrEmpty(o, "lessons").let { entries ->
                for (id in entries.keys()) {
                    if (cur != null && id !in cur.lessons) continue
                    val e = entries.getJSONObject(id)
                    s.lessons[id] = LessonRec().apply {
                        done = bool(e, "done", false); attempts = count(e, "attempts")
                        stars = number(e, "stars", 0, 0, 3).toInt(); lastAt = time(e, "lastAt")
                        placed = bool(e, "placed", false)
                    }
                }
            }
            s.placedTier = number(o, "placedTier", 0, 0, 200).toInt()
            s.placed = bool(o, "placed", false) && s.placedTier > 0
            s.theme = string(o, "theme", "auto").also {
                require(it in setOf("auto", "light", "dark")) { "Unsupported appearance setting" }
            }
            val units = if (o.has("unitsDone")) o.getJSONArray("unitsDone") else JSONArray()
            for (i in 0 until units.length()) {
                val id = units.get(i) as? String ?: error("Invalid unit id")
                if (cur == null || cur.units.any { it.id == id }) s.unitsDone.add(id)
            }
            objectOrEmpty(o, "items").let { entries ->
                for (id in entries.keys()) {
                    if (cur != null && id !in cur.items) continue
                    val e = entries.getJSONObject(id)
                    s.items[id] = SrsRec().apply {
                        lvl = number(e, "lvl", 0, 0, 5).toInt(); seen = count(e, "seen")
                        ok = count(e, "ok"); bad = count(e, "bad"); dueAt = time(e, "dueAt")
                        flag = bool(e, "flag", false)
                    }
                }
            }
            objectOrEmpty(o, "achievements").let { entries ->
                val known = Engine.ACHIEVEMENTS.map { it.id }.toSet()
                for (id in entries.keys()) if (id in known) s.achievements[id] = time(entries, id)
            }
            objectOrEmpty(o, "stats").let { e ->
                s.stats.answered = count(e, "answered"); s.stats.correct = count(e, "correct")
                require(s.stats.correct <= s.stats.answered) { "Correct answers exceed total answers" }
                s.stats.lessons = count(e, "lessons"); s.stats.reviews = count(e, "reviews")
                s.stats.perfects = count(e, "perfects"); s.stats.maxComboEver = count(e, "maxComboEver")
                s.stats.timeSpentSec = count(e, "timeSpentSec")
            }
            return s
        } catch (e: Exception) {
            throw IllegalArgumentException(e.message ?: "Invalid progress JSON", e)
        }
    }

    private fun objectOrEmpty(o: JSONObject, key: String): JSONObject =
        if (o.has(key)) o.getJSONObject(key) else JSONObject()

    private fun string(o: JSONObject, key: String, default: String): String =
        if (!o.has(key)) default else o.get(key) as? String ?: error("$key must be text")

    private fun bool(o: JSONObject, key: String, default: Boolean): Boolean =
        if (!o.has(key)) default else o.get(key) as? Boolean ?: error("$key must be true or false")

    private fun count(o: JSONObject, key: String) = number(o, key, 0, 0, MAX_COUNT).toInt()
    private fun time(o: JSONObject, key: String) = number(o, key, 0, 0, MAX_TIME)
    private fun number(o: JSONObject, key: String, default: Long, min: Long, max: Long): Long {
        if (!o.has(key)) return default
        val n = o.get(key) as? Number ?: error("$key must be a number")
        val d = n.toDouble()
        require(d.isFinite() && d >= min.toDouble() && d <= max.toDouble() && d == kotlin.math.floor(d)) { "Invalid $key range" }
        return d.toLong()
    }
}
