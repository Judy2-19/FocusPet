package com.example.focuspets.model

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {
    fun selected(context: Context): String {
        val preferences = context.getSharedPreferences("app_language", Context.MODE_PRIVATE)
        // Older versions saved Chinese automatically, even without a user selection.
        // Start the English-default version in English once, then retain future choices.
        if (!preferences.getBoolean("english_default_initialized", false)) {
            preferences.edit()
                .putString("language", "en")
                .putBoolean("english_default_initialized", true)
                .apply()
        }
        return preferences.getString("language", "en") ?: "en"
    }

    fun apply(context: Context, language: String = selected(context)) {
        require(language == "zh-CN" || language == "en")
        context.getSharedPreferences("app_language", Context.MODE_PRIVATE)
            .edit().putString("language", language).apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }
}
