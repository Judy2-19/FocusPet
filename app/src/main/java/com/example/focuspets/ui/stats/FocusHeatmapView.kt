package com.example.focuspets.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.focuspets.R

/**
 * Canvas heatmap with seven weekday rows (Monday=0) and 24 hour columns. Darker cells represent more
 * accumulated focus minutes. setData accepts matrix[day][hour]; draw everything in one Canvas without
 * extra View nodes.
 */
class FocusHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val days = 7
    private val hours = 24
    private var matrix: Array<IntArray> = Array(days) { IntArray(hours) }
    private var maxValue = 1

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accent = ContextCompat.getColor(context, R.color.cat_accent)
    private val zeroColor = Color.parseColor("#EFE6C8")

    private val padLeft = dp(24f)
    private val padTop = dp(16f)
    private val cellGap = dp(2f)
    private val weekdayLabels = arrayOf(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_102), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_156), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_176), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_152), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_079), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_134), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_146))

    init {
        labelPaint.color = 0xFF9E9E9E.toInt()
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.textSize = sp(10f)
        cellPaint.style = Paint.Style.FILL
    }

    /** Set heatmap data; ignore matrices without seven rows to avoid bounds errors. */
    fun setData(data: Array<IntArray>) {
        if (data.size != days) return
        matrix = data
        maxValue = data.fold(0) { m, row -> maxOf(m, row.maxOrNull() ?: 0) }.coerceAtLeast(1)
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val width = if (MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED)
            dp(320f).toInt() else MeasureSpec.getSize(widthSpec)
        val gridW = width - padLeft - dp(4f)
        val cell = (gridW - cellGap * (hours - 1)) / hours
        val gridH = cell * days + cellGap * (days - 1)
        val totalH = (padTop + gridH + dp(2f)).toInt()
        setMeasuredDimension(width, totalH)
    }

    override fun onDraw(canvas: Canvas) {
        val width = measuredWidth
        val gridW = width - padLeft - dp(4f)
        val cell = (gridW - cellGap * (hours - 1)) / hours
        val corner = (cell * 0.22f).coerceAtMost(cell / 2f)

        // Top hour labels every three hours.
        labelPaint.textAlign = Paint.Align.CENTER
        for (h in 0 until hours step 3) {
            val cx = padLeft + h * (cell + cellGap) + cell / 2f
            canvas.drawText(h.toString(), cx, padTop - dp(5f), labelPaint)
        }

        for (d in 0 until days) {
            val y = padTop + d * (cell + cellGap)
            // Weekday labels on the left.
            labelPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(weekdayLabels[d], padLeft - dp(6f), y + cell / 2f + sp(3f), labelPaint)
            labelPaint.textAlign = Paint.Align.CENTER
            for (h in 0 until hours) {
                val x = padLeft + h * (cell + cellGap)
                val v = matrix[d][h]
                cellPaint.color = if (v <= 0) zeroColor else heatColor(v)
                canvas.drawRoundRect(RectF(x, y, x + cell, y + cell), corner, corner, cellPaint)
            }
        }
    }

    /** Map minutes to accent opacity: longer focus means darker color. */
    private fun heatColor(v: Int): Int {
        val ratio = (v.toFloat() / maxValue).coerceIn(0f, 1f)
        val alpha = (0.22f + 0.78f * ratio) * 255f
        return Color.argb(
            alpha.toInt().coerceIn(0, 255),
            Color.red(accent), Color.green(accent), Color.blue(accent)
        )
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun sp(v: Float) = v * resources.displayMetrics.scaledDensity
}
