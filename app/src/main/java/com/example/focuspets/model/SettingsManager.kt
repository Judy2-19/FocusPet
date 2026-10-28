package com.example.focuspets.model

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import com.example.focuspets.util.isNightMode
import com.example.focuspets.util.toNightSurface

/**
 * Global SharedPreferences: a soft background palette selected on first entry and applied throughout
 * the app, plus a flag controlling the first-run palette prompt.
 */
object SettingsManager {

    private const val PREFS = "app_settings"
    private const val KEY_BG = "bg_color"
    private const val KEY_BG_CHOSEN = "bg_chosen"
    private const val KEY_ONBOARDING_SEEN = "onboarding_seen"
    private const val KEY_CURRENT_PET = "current_pet_id"

    /** Soft background palette inspired by Tomato ToDo. */
    val PALETTE = listOf(
        "#FEF8EA", // Warm beige, the default cat-home color.
        "#FFE9E3", // Peach pink.
        "#E8F3FF", // Sky blue.
        "#E9F8EF", // Mint green.
        "#F3ECFF", // Lavender.
        "#FFF4D6", // Cream yellow.
        "#FDE8F0", // Cherry blossom pink.
        "#EAF6F1"  // Celadon green.
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

    /** Whether onboarding has been viewed; do not show it automatically again. */
    fun isOnboardingSeen(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_SEEN, false)

    fun setOnboardingSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply()
    }

    /** Reset the onboarding flag so Settings can show it again on next launch. */
    fun resetOnboarding(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_SEEN, false).apply()
    }

    /** Home pet ID for the cloud leaderboard avatar; defaults to 1. */
    fun getCurrentPetId(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_CURRENT_PET, 1)

    fun setCurrentPetId(context: Context, id: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_CURRENT_PET, id).apply()
    }

    // ---------------- Lock mode: daily forced exit ----------------

    private const val KEY_LOCK_FORCE_EXIT_DATE = "lock_force_exit_date"

    /** Last forced-exit date (yyyy-MM-dd); empty means never used. */
    fun getLockForceExitDate(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOCK_FORCE_EXIT_DATE, "") ?: ""

    /** Record today's forced exit; limited to once daily and available again tomorrow. */
    fun setLockForceExitDate(context: Context, date: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LOCK_FORCE_EXIT_DATE, date).apply()
    }

    // ---------------- Lock mode: system screen-pinning switch ----------------

    private const val KEY_SCREEN_PIN = "screen_pin_enabled"

    /**
     * Whether to use startLockTask screen pinning; enabled by default. Disabling it avoids the system
     * pinning prompt but leaves a normal full-screen Activity that users can exit with Home.
     */
    fun isScreenPinEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SCREEN_PIN, true)

    fun setScreenPinEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SCREEN_PIN, enabled).apply()
    }

    // ---------------- Lock mode: allowlisted apps with soft monitoring ----------------

    private const val KEY_LOCK_WHITELIST = "lock_whitelist_apps"

    /**
     * Allowlisted package names. A nonempty set enables soft monitoring instead of startLockTask, allowing
     * listed apps and returning to the lock screen when other apps open.
     */
    fun getLockWhitelist(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_LOCK_WHITELIST, emptySet()) ?: emptySet()

    fun setLockWhitelist(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_LOCK_WHITELIST, packages).apply()
    }

    /** Enable allowlist soft monitoring whenever the set is nonempty. */
    fun isLockWhitelistEnabled(context: Context): Boolean =
        getLockWhitelist(context).isNotEmpty()

    // ---------------- Completion alerts: sound, vibration, or custom audio ----------------

    private const val KEY_ALERT_MODE = "alert_mode"
    private const val KEY_ALERT_RINGTONE = "alert_ringtone_uri"

    /** Alert choice for focus/lock completion. */
    const val ALERT_NONE = "none"           // No alert.
    const val ALERT_VIBRATE = "vibrate"     // Vibration only.
    const val ALERT_SOUND_DEFAULT = "sound_default"   // System default notification sound.
    const val ALERT_SOUND_CUSTOM = "sound_custom"     // User-selected audio.

    /** Default to vibration only to avoid disruptive sounds. */
    fun getAlertMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ALERT_MODE, ALERT_VIBRATE) ?: ALERT_VIBRATE

    fun setAlertMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ALERT_MODE, mode).apply()
    }

    /** Persistently granted custom audio URI, used only for ALERT_SOUND_CUSTOM. */
    fun getAlertRingtoneUri(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ALERT_RINGTONE, null)

    fun setAlertRingtoneUri(context: Context, uri: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ALERT_RINGTONE, uri).apply()
    }

    // ---------------- Theme: light, dark, or system ----------------

    private const val KEY_THEME_MODE = "theme_mode"

    /** Theme values: system follows the system, light forces light, dark forces dark. */
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

    /** Map the saved theme string to an AppCompatDelegate night-mode constant. */
    fun themeModeToNightMode(mode: String): Int = when (mode) {
        THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}

/** Background color helpers. */
object Backgrounds {

    /** Apply the selected color to a root view, darkening it into a dark surface in night mode. */
    fun apply(context: Context, view: View) {
        try {
            val base = Color.parseColor(SettingsManager.getBgColor(context))
            view.setBackgroundColor(if (context.isNightMode()) base.toNightSurface() else base)
        } catch (_: Exception) {
            // Leave unchanged if color parsing fails.
        }
    }

    /** Get the background color as an int for Activity windows, darkened in night mode. */
    fun colorInt(context: Context): Int = try {
        val base = Color.parseColor(SettingsManager.getBgColor(context))
        if (context.isNightMode()) base.toNightSurface() else base
    } catch (_: Exception) {
        if (context.isNightMode()) Color.parseColor("#1C1916")
        else Color.parseColor(SettingsManager.DEFAULT_BG)
    }
}
