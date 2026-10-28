package com.example.focuspets.model

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Resolves display text for models, services, adapters and custom drawing code. */
object UiStrings {
    private lateinit var app: Context
    fun init(context: Context) { app = context.applicationContext }
    fun locale(): Locale = Locale.forLanguageTag(LanguageManager.selected(app))
    fun petName(id: Int, fallback: String): String = when (id) {
        1 -> text(com.example.focuspets.R.string.dynamic_092)
        2 -> text(com.example.focuspets.R.string.dynamic_056)
        3 -> text(com.example.focuspets.R.string.dynamic_046)
        4 -> text(com.example.focuspets.R.string.dynamic_141)
        5 -> text(com.example.focuspets.R.string.dynamic_153)
        6 -> text(com.example.focuspets.R.string.dynamic_088)
        7 -> text(com.example.focuspets.R.string.dynamic_135)
        8 -> text(com.example.focuspets.R.string.dynamic_133)
        9 -> text(com.example.focuspets.R.string.dynamic_055)
        else -> fallback
    }
    fun petDescription(id: Int, fallback: String): String = when (id) {
        1 -> text(com.example.focuspets.R.string.dynamic_025)
        2 -> text(com.example.focuspets.R.string.dynamic_026)
        3 -> text(com.example.focuspets.R.string.dynamic_028)
        4 -> text(com.example.focuspets.R.string.dynamic_031)
        5 -> text(com.example.focuspets.R.string.dynamic_104)
        6 -> text(com.example.focuspets.R.string.dynamic_027)
        7 -> text(com.example.focuspets.R.string.dynamic_178)
        8 -> text(com.example.focuspets.R.string.dynamic_085)
        9 -> text(com.example.focuspets.R.string.dynamic_148)
        else -> fallback
    }
    fun text(id: Int, vararg args: Any): String {
        val configuration = Configuration(app.resources.configuration)
        configuration.setLocale(locale())
        return app.createConfigurationContext(configuration).getString(id, *args)
    }
}
