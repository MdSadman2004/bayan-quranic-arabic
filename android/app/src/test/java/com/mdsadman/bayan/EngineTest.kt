package com.mdsadman.bayan

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

internal fun fixture(): Curriculum {
    val items = (1..5).associate { n -> "w_$n" to Item("w_$n", "كلمة$n", Bi("word $n", "শব্দ $n"), "ك-ل-م", n, false, null) }
    val lessons = (1..3).associate { n -> "u1l$n" to Lesson("u1l$n", Bi("Lesson $n", "পাঠ $n"), emptyList(), items.values.toList(), emptyList(), "u1") }
    return Curriculum(listOf(CourseUnit("u1", 1, Bi("Unit", "ইউনিট"), Bi("", ""), lessons.keys.toList())), lessons, items, items.keys.toList(), emptyList(), 77429)
}

internal fun reviewRun(vararg touches: Pair<String, Boolean>): LessonRun = LessonRun("review", null, listOf(Card.Complete("review"))).also { r -> touches.forEach { r.record(it.first, it.second) } }

class EngineTest {
    private val cur = fixture()

    @Test fun missedDaysResetStreak() {
        val s = AppState().apply { day = LocalDate.now().minusDays(3).toString(); streak = 9 }
        Engine.applyLessonResult(cur, s, reviewRun("w_1" to true))
        assertEquals(1, s.streak)
    }
    @Test fun consecutiveDayIncrementsStreak() {
        val s = AppState().apply { day = LocalDate.now().minusDays(1).toString(); streak = 9 }
        Engine.applyLessonResult(cur, s, reviewRun("w_1" to true))
        assertEquals(10, s.streak)
    }
    @Test fun sameDayDoesNotIncrementStreak() {
        val s = AppState().apply { day = Engine.dayKey(System.currentTimeMillis()); streak = 9 }
        Engine.applyLessonResult(cur, s, reviewRun("w_1" to true))
        assertEquals(9, s.streak)
    }
    @Test fun earlyPracticePreservesFutureDueAndLevel() {
        val due = System.currentTimeMillis() + 86400000L
        val s = AppState().apply { items["w_1"] = SrsRec().apply { seen = 3; lvl = 3; dueAt = due } }
        Engine.applyLessonResult(cur, s, reviewRun("w_1" to true))
        assertEquals(3, s.items.getValue("w_1").lvl)
        assertEquals(due, s.items.getValue("w_1").dueAt)
    }
    @Test fun wrongAnswerIsDueImmediatelyAndCountsAllTouches() {
        val now = System.currentTimeMillis()
        val s = AppState().apply { items["w_1"] = SrsRec().apply { seen = 3; lvl = 4; dueAt = now + 86400000L } }
        Engine.applyLessonResult(cur, s, reviewRun("w_1" to true, "w_1" to false))
        val r = s.items.getValue("w_1")
        assertEquals(3, r.lvl)
        assertEquals(1, r.ok)
        assertEquals(1, r.bad)
        assertTrue(r.dueAt <= System.currentTimeMillis())
        assertTrue(r.flag)
    }
    @Test fun verseAndUnknownIdsNeverEnterVocabSrs() {
        val s = AppState()
        Engine.applyLessonResult(cur, s, reviewRun("v_112_1" to true, "missing" to false, "w_1" to true))
        assertEquals(setOf("w_1"), s.items.keys)
    }
    @Test fun repeatSameDayDoesNotPromoteTwice() {
        val s = AppState()
        Engine.applyLessonResult(cur, s, reviewRun("w_1" to true))
        Engine.applyLessonResult(cur, s, reviewRun("w_1" to true))
        assertEquals(1, s.items.getValue("w_1").lvl)
        assertEquals(2, s.items.getValue("w_1").seen)
    }
    @Test fun unknownAndLockedLessonsAreNotUnlocked() {
        assertFalse(Engine.isUnlocked(AppState(), "missing", cur))
        assertFalse(Engine.isUnlocked(AppState(), "u1l2", cur))
        assertTrue(Engine.isUnlocked(AppState(), "u1l1", cur))
    }
}
