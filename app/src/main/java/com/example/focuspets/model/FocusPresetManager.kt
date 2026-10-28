package com.example.focuspets.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reusable focus presets pair content and custom duration, e.g. coding/50 minutes or vocabulary/25
 * minutes. Store at most eight in SharedPreferences as JSON without extra dependencies, newest first.
 */
object FocusPresetManager {

    private const val PREFS = "focus_presets"
    private const val KEY = "presets"
    private const val MAX = 8

    data class Preset(val content: String, val minutes: Int)

    fun getPresets(context: Context): List<Preset> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        if (raw.isNullOrEmpty()) return emptyList()
        val arr = JSONArray(raw)
        val list = mutableListOf<Preset>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(Preset(o.getString("c"), o.getInt("m")))
        }
        list
    }.getOrDefault(emptyList())

    /** Save a preset after trimming content; matching content updates duration and moves to the front. Return the full saved list. */
    fun savePreset(context: Context, content: String, minutes: Int): List<Preset> {
        val c = content.trim()
        val m = minutes.coerceIn(1, 180)
        if (c.isEmpty()) return getPresets(context)
        val existing = getPresets(context).toMutableList()
        existing.removeAll { it.content == c }
        existing.add(0, Preset(c, m))
        val trimmed = existing.take(MAX)
        persist(context, trimmed)
        return trimmed
    }

    /** Delete a preset. */
    fun removePreset(context: Context, content: String) {
        val remaining = getPresets(context).filter { it.content != content }
        persist(context, remaining)
    }

    private fun persist(context: Context, list: List<Preset>) {
        val arr = JSONArray()
        for (p in list) {
            arr.put(JSONObject().apply {
                put("c", p.content)
                put("m", p.minutes)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
