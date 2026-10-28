package com.example.focuspets.model

import android.content.Context

/**
 * Persistent content-to-pie-color mapping. The records legend lets users choose pink, purple, yellow,
 * blue, green, orange, gray, or brown. Choices persist across ranges and restarts; unset content uses
 * the screen's default palette order.
 */
object ContentColorStore {

    private const val PREFS = "focus_content_colors"
    private const val PREFIX = "c_"
    private const val ABSENT = Int.MIN_VALUE

    /** Return the saved color or null so callers can use the default palette. */
    fun getColor(context: Context, content: String): Int? {
        if (content.isEmpty()) return null
        val v = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(PREFIX + content, ABSENT)
        return if (v == ABSENT) null else v
    }

    fun setColor(context: Context, content: String, color: Int) {
        if (content.isEmpty()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(PREFIX + content, color).apply()
    }
}
