package com.example.focuspets.model

import android.content.Context
import android.graphics.Color
import android.view.View

/**
 * 全局用户偏好（SharedPreferences）：
 * - 背景色：用户首次进入时选择，之后全局生效（番茄 ToDo 风格柔和色板）
 * - 是否已选过背景色：用于首次进入弹出色板
 */
object SettingsManager {

    private const val PREFS = "app_settings"
    private const val KEY_BG = "bg_color"
    private const val KEY_BG_CHOSEN = "bg_chosen"

    /** 番茄 ToDo 风格的柔和背景色板 */
    val PALETTE = listOf(
        "#FEF8EA", // 暖米（猫窝色，默认）
        "#FFE9E3", // 蜜桃粉
        "#E8F3FF", // 天空蓝
        "#E9F8EF", // 薄荷绿
        "#F3ECFF", // 薰衣草紫
        "#FFF4D6", // 奶油黄
        "#FDE8F0", // 樱花粉
        "#EAF6F1"  // 青瓷绿
    )

    val DEFAULT_BG = "#FEF8EA"

    fun getBgColor(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BG, DEFAULT_BG) ?: DEFAULT_BG

    fun setBgColor(context: Context, color: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_BG, color).apply()
    }

    fun isBgChosen(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_BG_CHOSEN, false)

    fun setBgChosen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_BG_CHOSEN, true).apply()
    }
}

/** 背景色应用的工具方法 */
object Backgrounds {

    /** 把用户选中的背景色应用到某个根视图上 */
    fun apply(context: Context, view: View) {
        try {
            view.setBackgroundColor(Color.parseColor(SettingsManager.getBgColor(context)))
        } catch (_: Exception) {
            // 颜色解析失败则保持原样
        }
    }

    /** 取当前背景色的 int 值（供 Activity 窗口等使用） */
    fun colorInt(context: Context): Int = try {
        Color.parseColor(SettingsManager.getBgColor(context))
    } catch (_: Exception) {
        Color.parseColor(SettingsManager.DEFAULT_BG)
    }
}
