package com.example.focuspets.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

/**
 * 深色模式判断与配色工具。
 * 用户可选的背景色是浅色柔和色板，直接套到深色模式会破坏对比度，
 * 因此在夜间模式下把任意背景色压暗成深色表层，保留色相。
 */

/** 当前是否处于系统深色模式 */
fun Context.isNightMode(): Boolean {
    val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return uiMode == Configuration.UI_MODE_NIGHT_YES
}

/** 把任意（多为浅色）背景色压暗为深色表层，ratio 越大越接近 darkBase */
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
