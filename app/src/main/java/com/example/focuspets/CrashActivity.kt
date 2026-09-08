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
 * 崩溃展示页：由 FocusPetsApp 的全局未捕获异常处理器在崩溃时拉起，
 * 把完整堆栈显示在屏幕上（不依赖 MainActivity 能正常启动），
 * 并把文字设为可选中 + 提供「复制」按钮，方便把异常贴给开发者。
 *
 * 注意：本页刻意不引用任何数据库 / 云端 / 自定义主题，避免二次崩溃。
 */
class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val trace = (intent.getStringExtra("trace")
            ?: runCatching { File(filesDir, "last_crash.txt").readText() }.getOrNull()
            ?: "（无堆栈信息）")

        val tv = TextView(this).apply {
            text = "😵 应用崩溃了\n\n下面这段可直接选中复制（或点「复制崩溃信息」按钮）：\n\n$trace"
            setTextColor(0xFF212121.toInt())
            textSize = 12f
            setPadding(48, 48, 48, 48)
            // 关键：允许长按选中文字进行复制
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
            text = "复制崩溃信息"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("focuspets_crash", trace))
                Toast.makeText(this@CrashActivity, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }
        }

        val close = Button(this).apply {
            text = "关闭应用"
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
