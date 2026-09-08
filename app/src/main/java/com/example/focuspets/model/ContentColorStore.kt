package com.example.focuspets.model

import android.content.Context

/**
 * 专注内容 → 饼图颜色 的持久化映射。
 * 每个专注内容可以在专注记录页点击图例自由改成 粉/紫/黄/蓝/绿/橘/灰/棕 之一，
 * 设置后跨时段、跨重启保持稳定。未设置时由页面按调色板顺序分配默认色。
 */
object ContentColorStore {

    private const val PREFS = "focus_content_colors"
    private const val PREFIX = "c_"
    private const val ABSENT = Int.MIN_VALUE

    /** 返回该内容已保存的颜色；未保存返回 null（调用方应回退到默认调色板） */
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
