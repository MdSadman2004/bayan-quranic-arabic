package com.mdsadman.bayan

import org.junit.Assert.*
import org.junit.Test

class StateTest {
    @Test fun validV1RoundTrip() {
        val s = AppState().apply { onboarded = true; lang = "bn"; xp = 31 }
        assertEquals(31, AppState.fromJson(s.toJson().toString()).xp)
        assertEquals("bn", AppState.fromJson(s.toJson().toString()).lang)
    }
    @Test(expected = IllegalArgumentException::class) fun unsupportedVersionRejected() {
        AppState.fromJson("{\"v\":2}")
    }
    @Test(expected = IllegalArgumentException::class) fun negativeXpRejected() {
        AppState.fromJson("{\"v\":1,\"xp\":-1}")
    }
    @Test(expected = IllegalArgumentException::class) fun invalidSrsLevelRejected() {
        AppState.fromJson("{\"v\":1,\"items\":{\"w_1\":{\"lvl\":12}}}")
    }
    @Test(expected = IllegalArgumentException::class) fun truncatedJsonRejectedRatherThanPartialState() {
        AppState.fromJson("{\"v\":1,\"xp\":30,")
    }
}
