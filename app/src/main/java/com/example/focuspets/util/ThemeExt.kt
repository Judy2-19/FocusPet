package com.example.focuspets.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

/**
 * Night-mode detection and color helpers. The selectable backgrounds are light pastels, so darken them
 * into night surfaces while preserving hue to maintain contrast.
 */

/** Whether system dark mode is active. */
fun Context.isNightMode(): Boolean {
    val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return uiMode == Configuration.UI_MODE_NIGHT_YES
}

/** Darken a background into a night surface; larger ratio moves it closer to darkBase. */
fun Int.toNightSurface(ratio: Float = 0.80f): Int {
    val darkBase = Color.parseColor("#15120F")
    return mixColor(darkBase, ratio)
}

private fun Int.mixColor(other: Int, ratio: Float): Int {
    val r1 = Color.red(this); val g1 = Color.green(this); val b1 = Color.blue(this)
    val r2 = Color.red(other); val g2 = Color.green(other); val b2 = Color.blue(other)
    val clamp = { v: Int -> v.coerceIn(0, 255) }
    return Color.rgb(
        clamp((r1 + (r2 - r1) * ratio).toInt()),
        clamp((g1 + (g2 - g1) * ratio).toInt()),
        clamp((b1 + (b2 - b1) * ratio).toInt())
    )
}
