package com.example.focuspets.ui.focus

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Lightweight Canvas donut chart with proportional slices and a central hole. Static drawPie shares
 * rendering with high-resolution posters. White dividers separate slices, including a single-slice
 * ring. Two-line total-duration text sits at the lower right, leaving the center empty.
 */
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var slices: List<Slice> = emptyList()
    private var cornerText: String = ""        // Two lines separated by \n: label first, value second.

    /** Slice label is for the external legend; drawing uses only value and color. */
    data class Slice(val label: String, val value: Int, val color: Int)

    fun setData(slices: List<Slice>, cornerText: String = "") {
        this.slices = slices
        this.cornerText = cornerText
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = (if (w < h) w else h) / 2f - 6f
        drawPie(canvas, cx, cy, r, slices, 0.52f, Color.WHITE, cornerText)
    }

    companion object {
        /**
         * Draw a donut chart on any Canvas, including shared posters. hole is the radius-relative cutout ratio
         * (0-1); holeColor usually matches the card background; cornerText has two newline-separated lines,
         * light label then dark value.
         */
        fun drawPie(
            canvas: Canvas,
            cx: Float, cy: Float, r: Float,
            slices: List<Slice>,
            hole: Float = 0.52f,
            holeColor: Int = Color.WHITE,
            cornerText: String = ""
        ) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

            if (slices.isEmpty()) {
                p.color = Color.parseColor("#EEEEEE")
                canvas.drawCircle(cx, cy, r, p)
                p.color = Color.parseColor("#9E9E9E")
                p.textAlign = Paint.Align.CENTER
                p.textSize = r * 0.2f
                val fm = p.fontMetrics
                canvas.drawText(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_115), cx, cy - (fm.ascent + fm.descent) / 2f, p)
                return
            }

            val total = slices.sumOf { it.value }.toFloat().coerceAtLeast(1f)
            val rect = RectF(cx - r, cy - r, cx + r, cy + r)
            val separator = r * 0.022f         // Thin white dividers avoid covering colored slices.
            var start = -90f
            for (s in slices) {
                val sweep = (s.value / total) * 360f
                p.color = s.color
                // useCenter=true draws sectors; overlap slightly to avoid pixel gaps.
                canvas.drawArc(rect, start, sweep + 0.5f, true, p)
                start += sweep
            }

            // Cut out the center to form a donut.
            p.color = holeColor
            canvas.drawCircle(cx, cy, r * hole, p)

            // Draw short radial lines at slice boundaries for clearer separation.
            start = -90f
            p.color = holeColor
            p.strokeWidth = separator
            p.style = Paint.Style.STROKE
            for (s in slices) {
                val sweep = (s.value / total) * 360f
                val angle = Math.toRadians((start).toDouble())
                val x1 = cx + (r * hole) * Math.cos(angle).toFloat()
                val y1 = cy + (r * hole) * Math.sin(angle).toFloat()
                val x2 = cx + (r) * Math.cos(angle).toFloat()
                val y2 = cy + (r) * Math.sin(angle).toFloat()
                canvas.drawLine(x1, y1, x2, y2, p)
                start += sweep
            }
            p.style = Paint.Style.FILL

            // Lower-right total duration: light-gray label and bold dark value.
            if (cornerText.isNotEmpty()) {
                val parts = cornerText.split("\n", limit = 2)
                val label = if (parts.size == 2) parts[0] else ""
                val value = parts.last()

                val labelSize = r * 0.13f
                val valueSize = (r * 0.2f)
                val right = cx + r - 4f
                val bottom = cy + r - 4f

                p.textAlign = Paint.Align.RIGHT
                // Value.
                p.isFakeBoldText = true
                p.color = Color.parseColor("#212121")
                var vSize = valueSize
                p.textSize = vSize
                while (p.measureText(value) > r * 0.9f && vSize > r * 0.1f) {
                    vSize -= r * 0.01f
                    p.textSize = vSize
                }
                val vFm = p.fontMetrics
                canvas.drawText(value, right, bottom, p)
                // Label above the value.
                p.isFakeBoldText = false
                p.color = Color.parseColor("#757575")
                p.textSize = labelSize
                val lFm = p.fontMetrics
                canvas.drawText(label, right, bottom - (vFm.descent - vFm.ascent) - 2f, p)
            }
        }
    }
}
