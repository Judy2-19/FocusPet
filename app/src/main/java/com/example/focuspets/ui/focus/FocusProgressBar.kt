package com.example.focuspets.ui.focus

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * 自定义专注进度条：
 * - 已过去时间为橘色（#FF7043）
 * - 未走完时间为白色（#F5F5F5）
 * - 中间以黑色竖线分割，无圆角
 */
class FocusProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF7043") }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F5F5F5") }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 4f
    }

    private var progress: Int = 0

    /** 进度 0~100 */
    fun setProgress(p: Int) {
        progress = p.coerceIn(0, 100)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val fillW = w * progress / 100f

        // 背景（未走完）
        canvas.drawRect(0f, 0f, w, h, emptyPaint)
        // 已走完
        canvas.drawRect(0f, 0f, fillW, h, fillPaint)
        // 中间黑色竖线（仅在两端之外显示）
        if (progress in 1..99) {
            canvas.drawLine(fillW, 0f, fillW, h, dividerPaint)
        }
    }
}
