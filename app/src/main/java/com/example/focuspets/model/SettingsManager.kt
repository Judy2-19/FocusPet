package com.example.focuspets.model

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import com.example.focuspets.util.isNightMode
import com.example.focuspets.util.toNightSurface

/**
 * 全局用户偏好（SharedPreferences）：
 * - 背景色：用户首次进入时选择，之后全局生效（番茄 ToDo 风格柔和色板）
 * - 是否已选过背景色：用于首次进入弹出色板
 */
object SettingsManager {

    private const val PREFS = "app_settings"
    private const val KEY_BG = "bg_color"
    private const val KEY_BG_CHOSEN = "bg_chosen"
    private const val KEY_ONBOARDING_SEEN = "onboarding_seen"
    private const val KEY_CURRENT_PET = "current_pet_id"

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

    /** 新手引导是否已看过（看过则不再自动弹） */
    fun isOnboardingSeen(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_SEEN, false)

    fun setOnboardingSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply()
    }

    /** 重置引导标记，让下次启动重新展示（设置页「重新查看新手引导」用） */
    fun resetOnboarding(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_SEEN, false).apply()
    }

    /** 当前首页展示的宠物 id（用于云端排行榜头像映射；默认 1 号） */
    fun getCurrentPetId(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_CURRENT_PET, 1)

    fun setCurrentPetId(context: Context, id: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_CURRENT_PET, id).apply()
    }

    // ---------------- 锁机模式：每日强制退出 ----------------

    private const val KEY_LOCK_FORCE_EXIT_DATE = "lock_force_exit_date"

    /** 最近一次使用「强制退出」的日期（yyyy-MM-dd），空串表示从未使用过 */
    fun getLockForceExitDate(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOCK_FORCE_EXIT_DATE, "") ?: ""

    /** 记录今天已使用过强制退出（每日限一次，次日自动恢复） */
    fun setLockForceExitDate(context: Context, date: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LOCK_FORCE_EXIT_DATE, date).apply()
    }

    // ---------------- 锁机模式：系统屏幕固定开关 ----------------

    private const val KEY_SCREEN_PIN = "screen_pin_enabled"

    /**
     * 锁机时是否使用系统「屏幕固定」（startLockTask）。默认开启。
     * 关闭后锁机不调用 startLockTask，因此不会弹出系统「应用已固定」提示，
     * 代价是锁机变为普通全屏（用户可按 Home 退出）。
     */
    fun isScreenPinEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SCREEN_PIN, true)

    fun setScreenPinEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SCREEN_PIN, enabled).apply()
    }

    // ---------------- 锁机模式：白名单应用（软监控放行）----------------

    private const val KEY_LOCK_WHITELIST = "lock_whitelist_apps"

    /**
     * 锁机白名单应用包名集合。锁机开始时若此集合非空，
     * 则改用「软监控」模式（不再调用 startLockTask 硬锁死），
     * 允许白名单内的 app 在锁机期间正常使用；打开白名单外的 app 会被自动拉回锁机页。
     */
    fun getLockWhitelist(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_LOCK_WHITELIST, emptySet()) ?: emptySet()

    fun setLockWhitelist(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_LOCK_WHITELIST, packages).apply()
    }

    /** 是否启用白名单软监控（集合非空即启用） */
    fun isLockWhitelistEnabled(context: Context): Boolean =
        getLockWhitelist(context).isNotEmpty()

    // ---------------- 结束提醒：铃声 / 震动 / 自定义音频 ----------------

    private const val KEY_ALERT_MODE = "alert_mode"
    private const val KEY_ALERT_RINGTONE = "alert_ringtone_uri"

    /** 专注 / 锁机完成时的提醒方式（二者择其一） */
    const val ALERT_NONE = "none"           // 不提醒
    const val ALERT_VIBRATE = "vibrate"     // 仅震动
    const val ALERT_SOUND_DEFAULT = "sound_default"   // 系统默认提示音
    const val ALERT_SOUND_CUSTOM = "sound_custom"     // 用户自定义音频

    /** 默认用「仅震动」，避免突然响铃打扰 */
    fun getAlertMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ALERT_MODE, ALERT_VIBRATE) ?: ALERT_VIBRATE

    fun setAlertMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ALERT_MODE, mode).apply()
    }

    /** 用户上传的自定义提醒音频 URI（持久授权），仅 ALERT_SOUND_CUSTOM 时生效 */
    fun getAlertRingtoneUri(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ALERT_RINGTONE, null)

    fun setAlertRingtoneUri(context: Context, uri: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ALERT_RINGTONE, uri).apply()
    }

    // ---------------- 主题模式（浅色 / 深色 / 跟随系统）----------------

    private const val KEY_THEME_MODE = "theme_mode"

    /** 主题模式：system=跟随系统，light=浅色，dark=深色 */
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    fun getThemeMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME_MODE, mode).apply()
    }

    /** 把保存的主题模式字符串映射成 AppCompatDelegate 的夜间模式常量 */
    fun themeModeToNightMode(mode: String): Int = when (mode) {
        THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}

/** 背景色应用的工具方法 */
object Backgrounds {

    /** 把用户选中的背景色应用到某个根视图上（深色模式下压暗为深色表层） */
    fun apply(context: Context, view: View) {
        try {
            val base = Color.parseColor(SettingsManager.getBgColor(context))
            view.setBackgroundColor(if (context.isNightMode()) base.toNightSurface() else base)
        } catch (_: Exception) {
            // 颜色解析失败则保持原样
        }
    }

    /** 取当前背景色的 int 值（供 Activity 窗口等使用，深色模式返回压暗后的深色） */
    fun colorInt(context: Context): Int = try {
        val base = Color.parseColor(SettingsManager.getBgColor(context))
        if (context.isNightMode()) base.toNightSurface() else base
    } catch (_: Exception) {
        if (context.isNightMode()) Color.parseColor("#1C1916")
        else Color.parseColor(SettingsManager.DEFAULT_BG)
    }
}
