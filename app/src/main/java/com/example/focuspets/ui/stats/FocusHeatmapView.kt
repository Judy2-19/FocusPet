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
 * 专注时段热力图：
 *  - 7 行 = 周一..周日
 *  - 24 列 = 0..23 时
 * 每个格子颜色越深，代表该「星期 × 小时」累计专注分钟数越多。
 *
 * 数据通过 [setData] 传入，matrix[day][hour] 为分钟数（day: 0=周一）。
 * 无需布局嵌套，整张图由 Canvas 一次绘制，零额外 View 节点。
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
    private val weekdayLabels = arrayOf("一", "二", "三", "四", "五", "六", "日")

    init {
        labelPaint.color = 0xFF9E9E9E.toInt()
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.textSize = sp(10f)
        cellPaint.style = Paint.Style.FILL
    }

    /** 设置热力数据；行数不为 7 时忽略，避免数组越界 */
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

        // 顶部小时刻度（每 3 小时标注一次）
        labelPaint.textAlign = Paint.Align.CENTER
        for (h in 0 until hours step 3) {
            val cx = padLeft + h * (cell + cellGap) + cell / 2f
            canvas.drawText(h.toString(), cx, padTop - dp(5f), labelPaint)
        }

        for (d in 0 until days) {
            val y = padTop + d * (cell + cellGap)
            // 左侧星期标签
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

    /** 把分钟数映射成 accent 色的透明度：越久越深 */
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
