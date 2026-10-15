package com.mdsadman.bayan

import android.content.Context

/** SharedPreferences persistence for AppState (single JSON blob, same schema family as web). */
class Store(ctx: Context, private val cur: Curriculum = Curriculum.load(ctx)) {
    private val prefs = ctx.getSharedPreferences("bayan", Context.MODE_PRIVATE)

    fun load(): AppState {
        val raw = prefs.getString("state", null) ?: return AppState()
        return AppState.fromJson(raw, cur)
    }

    fun save(s: AppState) {
        prefs.edit().putString("state", s.toJson().toString()).apply()
    }

    fun clear() {
        prefs.edit().remove("state").apply()
    }
}
