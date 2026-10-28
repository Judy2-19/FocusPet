package com.example.focuspets

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

/**
 * Crash screen launched by the global uncaught-exception handler in FocusPetsApp. Displays a
 * selectable stack trace with a Copy button, even when MainActivity fails before rendering. Avoid
 * database, cloud, and custom-library dependencies to prevent secondary crashes.
 */
class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val trace = (intent.getStringExtra("trace")
            ?: runCatching { File(filesDir, "last_crash.txt").readText() }.getOrNull()
            ?: com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_020))

        val tv = TextView(this).apply {
            text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_190 ,(trace).toString())
            setTextColor(0xFF212121.toInt())
            textSize = 12f
            setPadding(48, 48, 48, 48)
            // Allow selecting and copying the error text.
            setTextIsSelectable(true)
        }

        val scroll = ScrollView(this).apply {
            addView(tv)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            layoutParams = lp
        }

        val copy = Button(this).apply {
            text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_053)
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("focuspets_crash", trace))
                Toast.makeText(this@CrashActivity, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_052), Toast.LENGTH_SHORT).show()
            }
        }

        val close = Button(this).apply {
            text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_048)
            setOnClickListener { finishAffinity() }
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(copy, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(close, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFEF8EA.toInt())
            addView(scroll)
            addView(btnRow)
        }
        setContentView(root)
    }
}
