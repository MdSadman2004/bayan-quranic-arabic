package com.mdsadman.bayan

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BackupTest {
    private val cur = fixture()

    @Test fun portableVersionSerialized() {
        val s = AppState().apply { xp = 30; streak = 5 }
        val o = s.toJson()
        assertEquals(1L, o.getLong("v"))
    }

    @Test fun importsRejectUnsupportedVersion() {
        val before = AppState()
        before.day = "2026-10-15"
        before.xp = 30
        val v2 = JSONObject().apply { put("v", 2) }
        assertThrows(IllegalArgumentException::class.java) { AppState.fromJson(v2.toString(), cur) }
    }

    @Test fun importsRejectNegativeXp() {
        assertThrows(IllegalArgumentException::class.java) { AppState.fromJson("{\"v\":1,\"xp\":-1}", cur) }
    }

    @Test fun importsRejectInvalidSrsLevel() {
        assertThrows(IllegalArgumentException::class.java) { AppState.fromJson("{\"v\":1,\"items\":{\"w_1\":{\"lvl\":12}}}", cur) }
    }

    @Test fun importsRejectTruncatedJson() {
        assertThrows(IllegalArgumentException::class.java) { AppState.fromJson("{\"v\":1,\"xp\":30,", cur) }
    }

    @Test fun importsRejectMalformedStructure() {
        assertThrows(IllegalArgumentException::class.java) { AppState.fromJson("not json", cur) }
        assertThrows(IllegalArgumentException::class.java) { AppState.fromJson("[]", cur) }
        assertThrows(IllegalArgumentException::class.java) { AppState.fromJson("{\"v\":1,\"xp\":30,", cur) }
    }

    @Test fun importsCarryForwardKnownItems() {
        val before = AppState()
        before.items["w_1"] = SrsRec().apply { seen = 3; lvl = 2; dueAt = 1000 }
        val after = AppState.fromJson(before.toJson().toString(), cur)
        val r = after.items["w_1"]
        assertNotNull(r)
        assertEquals(3, r!!.seen)
        assertEquals(2, r!!.lvl)
        assertEquals(1000, r!!.dueAt)
    }

    @Test fun importsIgnoreUnknownItemIds() {
        val beforeUnknown = AppState()
        beforeUnknown.items["garbage_999"] = SrsRec()
        val after = AppState.fromJson("{\"v\":1,\"items\":{\"w_1\":{\"seen\":1}}}", cur)
        assertEquals(1, after.items.size)
        assertEquals(1, after.items["w_1"]!!.seen)
    }

    @Test fun importsCarryForwardLessons() {
        val before = AppState()
        before.lessons["u1l1"] = LessonRec().apply { done = true }
        val after = AppState.fromJson(before.toJson().toString(), cur)
        assertTrue(after.lessons["u1l1"]?.done == true)
    }

    @Test fun importsCarryForwardUnitsDone() {
        val before = AppState()
        before.unitsDone.add("u1")
        val after = AppState.fromJson(before.toJson().toString(), cur)
        assertTrue(after.unitsDone.contains("u1"))
    }

    @Test fun importsRejectMalformedActivityDate() {
        assertThrows(IllegalArgumentException::class.java) { AppState.fromJson("{\"v\":1,\"xp\":30,\"day\":\"2026-13-01\"}", cur) }
    }
}
