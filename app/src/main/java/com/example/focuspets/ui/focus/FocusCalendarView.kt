package com.example.focuspets.ui.focus

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.focuspets.R
import java.util.Calendar

/**
 * 简易月历视图（无第三方依赖，纯布局拼装）：
 * - 顶部星期表头（日一二三四五六），下方 6×7 网格。
 * - 有专注记录的日期，在数字下方显示红点。
 * - 点击某天触发 onDayClickListener（year/month/day，month 为 Calendar.JANUARY=0..）。
 * - 当前选中的日期文字加粗高亮。
 *
 * 用法：showMonth(y,m) 切换月份；setMarkedDays(daySet) 标记有记录的日期；
 * setSelectedDay(day) 高亮选中；setOnDayClickListener 接收点击。
 */
class FocusCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val weekLabels = listOf("日", "一", "二", "三", "四", "五", "六")
    private val density = context.resources.displayMetrics.density

    private var year = 0
    private var month = 0   // Calendar.JANUARY = 0 .. DECEMBER = 11
    private var markedDays: Set<Int> = emptySet()
    private var selectedDay: Int = -1
    private var listener: ((year: Int, month: Int, day: Int) -> Unit)? = null

    private val header = LinearLayout(context).apply {
        orientation = HORIZONTAL
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        layoutParams = lp
    }
    private val grid = LinearLayout(context).apply {
        orientation = VERTICAL
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        layoutParams = lp
    }

    init {
        orientation = VERTICAL
        for (w in weekLabels) {
            val tv = TextView(context).apply {
                text = w
                gravity = Gravity.CENTER
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_hint))
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }
            header.addView(tv)
        }
        addView(header)
        addView(grid)

        val cal = Calendar.getInstance()
        showMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun setOnDayClickListener(l: (year: Int, month: Int, day: Int) -> Unit) {
        listener = l
    }

    fun showMonth(y: Int, m: Int) {
        year = y
        month = m
        rebuild()
    }

    fun getYear(): Int = year
    fun getMonth(): Int = month

    fun setMarkedDays(days: Set<Int>) {
        markedDays = days
        rebuild()
    }

    fun setSelectedDay(day: Int) {
        selectedDay = day
        rebuild()
    }

    private fun rebuild() {
        grid.removeAllViews()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val firstDow = cal.get(Calendar.DAY_OF_WEEK)            // 1=Sun .. 7=Sat
        val offset = (firstDow - Calendar.SUNDAY + 7) % 7
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = ArrayList<Int?>()
        repeat(offset) { cells.add(null) }
        for (d in 1..daysInMonth) cells.add(d)
        while (cells.size % 7 != 0) cells.add(null)

        var i = 0
        while (i < cells.size) {
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            for (c in 0 until 7) {
                val day = cells[i]
                row.addView(makeCell(day))
                i++
            }
            grid.addView(row)
        }
    }

    private fun makeCell(day: Int?): View {
        val cell = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply {
                setMargins((2 * density).toInt(), (4 * density).toInt(),
                    (2 * density).toInt(), (4 * density).toInt())
            }
            setPadding(0, (8 * density).toInt(), 0, (8 * density).toInt())
        }
        if (day == null) {
            cell.addView(View(context))
            return cell
        }
        val num = TextView(context).apply {
            text = day.toString()
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            if (day == selectedDay) {
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.cat_accent))
            }
        }
        val dot = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                (6 * density).toInt(), (6 * density).toInt()
            ).apply { topMargin = (3 * density).toInt() }
            setBackgroundResource(R.drawable.dot_red)
            visibility = if (day in markedDays) View.VISIBLE else View.GONE
        }
        cell.addView(num)
        cell.addView(dot)
        cell.setOnClickListener { listener?.invoke(year, month, day) }
        return cell
    }
}
