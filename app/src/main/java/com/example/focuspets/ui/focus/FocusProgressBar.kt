package com.example.focuspets.ui.focus

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Custom focus progress bar: elapsed orange (#FF7043), remaining white (#F5F5F5), separated by a black
 * vertical line without rounded corners.
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

    /** Progress from 0 to 100. */
    fun setProgress(p: Int) {
        progress = p.coerceIn(0, 100)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val fillW = w * progress / 100f

        // Remaining-time background.
        canvas.drawRect(0f, 0f, w, h, emptyPaint)
        // Elapsed portion.
        canvas.drawRect(0f, 0f, fillW, h, fillPaint)
        // Black divider, shown only away from the endpoints.
        if (progress in 1..99) {
            canvas.drawLine(fillW, 0f, fillW, h, dividerPaint)
        }
    }
}
