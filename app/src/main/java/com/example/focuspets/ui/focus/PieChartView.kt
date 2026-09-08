package com.example.focuspets.ui.focus

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 轻量甜甜圈饼图：按各 slice 的 value 占比绘制扇形，中心挖空。
 *
 * 设计要点：
 * - 无第三方图表库，纯 Canvas 绘制。
 * - drawPie 是 companion 静态方法，分享海报的高清 Bitmap 复用同一套绘制逻辑，
 *   保证「屏幕上看到的」和「分享出去的」完全一致。
 * - 各扇形之间留白色分隔线，单个内容占比时也是一个清晰的环。
 * - 「总专注时长」显示在饼图右下角（cornerText，两行：标签 + 数值），
 *   中心留空，使各内容扇形的占比更直观。
 */
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var slices: List<Slice> = emptyList()
    private var cornerText: String = ""        // 两行文本用 \n 分隔：第一行标签、第二行数值

    /** 单个扇形：label 仅用于外部图例，绘制本身只用 value 与 color */
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
         * 绘制甜甜圈饼图（纯静态，可在任意 Canvas 上调用，便于生成分享海报）。
         * @param hole 中心挖空比例（0~1，相对半径）
         * @param holeColor 中心挖空填充色（通常取卡片背景色）
         * @param cornerText 饼图右下角的文字（两行用 \n 分隔；第一行为浅色标签，第二行为深色数值）
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
                canvas.drawText("暂无数据", cx, cy - (fm.ascent + fm.descent) / 2f, p)
                return
            }

            val total = slices.sumOf { it.value }.toFloat().coerceAtLeast(1f)
            val rect = RectF(cx - r, cy - r, cx + r, cy + r)
            val separator = r * 0.022f         // 白色分隔线宽度（细，避免盖过彩色扇形）
            var start = -90f
            for (s in slices) {
                val sweep = (s.value / total) * 360f
                p.color = s.color
                // useCenter = true 画扇形；略微重叠一像素避免缝隙
                canvas.drawArc(rect, start, sweep + 0.5f, true, p)
                start += sweep
            }

            // 中心挖空 → 甜甜圈
            p.color = holeColor
            canvas.drawCircle(cx, cy, r * hole, p)

            // 白色分隔线：在每个扇形边界再画一小段半径线，强化分区感
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

            // 右下角文字（总专注时长）：标签（浅灰）+ 数值（深色加粗）
            if (cornerText.isNotEmpty()) {
                val parts = cornerText.split("\n", limit = 2)
                val label = if (parts.size == 2) parts[0] else ""
                val value = parts.last()

                val labelSize = r * 0.13f
                val valueSize = (r * 0.2f)
                val right = cx + r - 4f
                val bottom = cy + r - 4f

                p.textAlign = Paint.Align.RIGHT
                // 数值
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
                // 标签（数值上方）
                p.isFakeBoldText = false
                p.color = Color.parseColor("#757575")
                p.textSize = labelSize
                val lFm = p.fontMetrics
                canvas.drawText(label, right, bottom - (vFm.descent - vFm.ascent) - 2f, p)
            }
        }
    }
}
