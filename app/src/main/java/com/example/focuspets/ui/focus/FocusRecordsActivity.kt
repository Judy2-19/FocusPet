package com.example.focuspets.ui.focus

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focuspets.R
import com.example.focuspets.databinding.ActivityFocusRecordsBinding
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.PetRepository
import com.example.focuspets.db.dao.ContentMinutes
import com.example.focuspets.model.Backgrounds
import com.example.focuspets.model.ContentColorStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 专注记录页：
 * - 顶部按 日 / 周 / 月 / 年 切换，并支持上一/下一时段、回到今天。
 * - 中部用甜甜圈饼图展示「不同专注内容」在本时段的分钟数占比，下方配图例与百分比。
 * - 饼图可一键分享（经 FileProvider 暴露为图片，调起系统分享面板）。
 * - 底部「专注事件」列表详细展示每一次专注的内容、完成时间与时长。
 */
class FocusRecordsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFocusRecordsBinding

    private val repo by lazy {
        PetRepository(AppDatabase.getInstance(applicationContext))
    }
    private val viewModel: FocusRecordsViewModel by lazy {
        ViewModelProvider(
            this,
            FocusRecordsViewModelFactory(repo)
        )[FocusRecordsViewModel::class.java]
    }

    private val legendAdapter = LegendAdapter()
    private val recordAdapter = RecordAdapter()

    /** 饼图调色板（用户可在记录页点击图例改成其中任意一种）：粉/紫/黄/蓝/绿/橘/灰/棕 */
    private val palette = intArrayOf(
        0xFFE91E63.toInt(), // 粉
        0xFF9C27B0.toInt(), // 紫
        0xFFFBC02D.toInt(), // 黄
        0xFF2196F3.toInt(), // 蓝
        0xFF4CAF50.toInt(), // 绿
        0xFFFF9800.toInt(), // 橘
        0xFF9E9E9E.toInt(), // 灰（未命名）
        0xFF795548.toInt()  // 棕
    )

    private enum class Period { DAY, WEEK, MONTH, YEAR }

    private var periodType = Period.DAY
    private var anchorMs = System.currentTimeMillis()

    // 供分享用的当前数据快照
    private var currentSlices: List<PieChartView.Slice> = emptyList()
    private var currentTotal = 0
    private var contentColorMap: Map<String, Int> = emptyMap()

    // 最近一次聚合结果，供改色后局部刷新（避免重新查库）
    private var lastCms: List<ContentMinutes> = emptyList()
    private var lastRecs: List<com.example.focuspets.db.entity.FocusRecordEntity> = emptyList()

    /** 选色弹窗引用，便于选中后关闭 */
    private var colorPickerDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 系统负责 inflate + 把 ScrollView 挂到窗口（不会重复 addView，
        // 规避 "The specified child already has a parent"）；再以真正的 ScrollView
        // 根（@id/scroll_root）做 bind 包装，避免强转 ContentFrameLayout 为 ScrollView。
        setContentView(R.layout.activity_focus_records)
        binding = ActivityFocusRecordsBinding.bind(findViewById(R.id.scroll_root))
        Backgrounds.apply(this, binding.root)

        binding.rvLegend.layoutManager = LinearLayoutManager(this)
        binding.rvLegend.adapter = legendAdapter
        binding.rvRecords.layoutManager = LinearLayoutManager(this)
        binding.rvRecords.adapter = recordAdapter

        // 点击图例 → 修改该内容的饼图颜色
        legendAdapter.onItemClick = { item -> showColorPicker(item.label) }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPrev.setOnClickListener { shiftAnchor(-1) }
        binding.btnNext.setOnClickListener { shiftAnchor(1) }
        binding.tvToday.setOnClickListener {
            anchorMs = System.currentTimeMillis()
            reload()
        }
        binding.btnShare.setOnClickListener { sharePie() }

        binding.togglePeriod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            periodType = when (checkedId) {
                R.id.btn_p_week -> Period.WEEK
                R.id.btn_p_month -> Period.MONTH
                R.id.btn_p_year -> Period.YEAR
                else -> Period.DAY
            }
            reload()
        }

        setupCalendar()

        observe()
        reload()
    }

    // ---------------- 日历 ----------------

    private fun setupCalendar() {
        val now = Calendar.getInstance()
        binding.calendar.showMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
        updateCalendarTitle()
        refreshCalendarMarks()
        binding.calendar.setSelectedDay(now.get(Calendar.DAY_OF_MONTH))

        binding.calendar.setOnDayClickListener { y, m, d ->
            // 点击某天 → 切到「日」视图并加载当天记录 + 饼图
            periodType = Period.DAY
            binding.togglePeriod.check(R.id.btn_p_day)
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, y); set(Calendar.MONTH, m); set(Calendar.DAY_OF_MONTH, d)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            anchorMs = cal.timeInMillis
            binding.calendar.setSelectedDay(d)
            reload()
        }

        binding.btnCalPrev.setOnClickListener {
            shiftCalendar(-1)
        }
        binding.btnCalNext.setOnClickListener {
            shiftCalendar(1)
        }
    }

    /** 上个月 / 下个月翻页，并刷新红点标记 */
    private fun shiftCalendar(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, binding.calendar.getYear())
            set(Calendar.MONTH, binding.calendar.getMonth())
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.add(Calendar.MONTH, delta)
        binding.calendar.showMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        updateCalendarTitle()
        refreshCalendarMarks()
    }

    private fun updateCalendarTitle() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, binding.calendar.getYear())
            set(Calendar.MONTH, binding.calendar.getMonth())
        }
        binding.tvCalTitle.text =
            "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
    }

    /** 拉取当前显示月份内有记录的日期，标记红点 */
    private fun refreshCalendarMarks() {
        lifecycleScope.launchWhenCreated {
            val map = withContext(Dispatchers.IO) {
                runCatching {
                    repo.getDailyMinutesInMonth(
                        binding.calendar.getYear(),
                        binding.calendar.getMonth()
                    )
                }.getOrDefault(emptyMap())
            }
            // 仅保留属于当前显示月份的「日」数字
            val marked = map.keys.mapNotNull { dateStr ->
                dateStr.takeLast(2).toIntOrNull()
            }.toSet()
            binding.calendar.setMarkedDays(marked)
        }
    }

    // ---------------- 时段计算 ----------------

    /** 根据当前 periodType + anchorMs 计算 [startMs, endMs) 与本时段标题 */
    private fun computeRange(): Triple<Long, Long, String> {
        val cal = Calendar.getInstance().apply { timeInMillis = anchorMs }
        return when (periodType) {
            Period.DAY -> {
                cal.zeroTime()
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                val end = cal.timeInMillis
                Triple(start, end, SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA).format(Date(start)))
            }
            Period.WEEK -> {
                cal.zeroTime()
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                val offset = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
                cal.add(Calendar.DAY_OF_MONTH, -offset)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 7)
                val end = cal.timeInMillis
                val sf = SimpleDateFormat("M月d日", Locale.CHINA)
                val s = sf.format(Date(start))
                val e = sf.format(Date(end - 1))
                Triple(start, end, "$s - $e")
            }
            Period.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.zeroTime()
                val start = cal.timeInMillis
                val label = "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
                cal.add(Calendar.MONTH, 1)
                val end = cal.timeInMillis
                Triple(start, end, label)
            }
            Period.YEAR -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.zeroTime()
                val start = cal.timeInMillis
                val label = "${cal.get(Calendar.YEAR)}年"
                cal.add(Calendar.YEAR, 1)
                val end = cal.timeInMillis
                Triple(start, end, label)
            }
        }
    }

    private fun Calendar.zeroTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun shiftAnchor(delta: Int) {
        val cal = Calendar.getInstance().apply { timeInMillis = anchorMs }
        when (periodType) {
            Period.DAY -> cal.add(Calendar.DAY_OF_MONTH, delta)
            Period.WEEK -> cal.add(Calendar.DAY_OF_MONTH, delta * 7)
            Period.MONTH -> cal.add(Calendar.MONTH, delta)
            Period.YEAR -> cal.add(Calendar.YEAR, delta)
        }
        anchorMs = cal.timeInMillis
        reload()
    }

    private fun reload() {
        val (start, end, label) = computeRange()
        binding.tvPeriodLabel.text = label
        viewModel.load(start, end)
    }

    // ---------------- 数据绑定 ----------------

    private fun observe() {
        viewModel.slices.observe(this) { cms ->
            lastCms = cms
            renderData()
        }
        viewModel.records.observe(this) { recs ->
            lastRecs = recs
            renderData()
        }
        viewModel.totalMinutes.observe(this) {
            binding.tvSummaryMinutes.text = formatMinutes(it)
        }
        viewModel.sessionCount.observe(this) {
            binding.tvSummarySessions.text = "$it 次"
        }
    }

    /**
     * 依据最近一次聚合结果 + 用户已设/默认颜色，统一渲染饼图、图例与事件列表。
     * 改色后只调本方法即可，无需重新查库。
     */
    private fun renderData() {
        val cms = lastCms
        currentTotal = cms.sumOf { it.minutes }

        // 1) 为每个内容确定颜色：已保存的优先，否则按调色板顺序分配并持久化
        val colorMap = mutableMapOf<String, Int>()
        val slices = cms.mapIndexed { i, c ->
            val key = if (c.content.isBlank()) "" else c.content
            val color = if (key.isEmpty()) palette[6] // 灰 = 未命名
            else ContentColorStore.getColor(this@FocusRecordsActivity, key)
                ?: palette[i % palette.size].also {
                    ContentColorStore.setColor(this@FocusRecordsActivity, key, it)
                }
            colorMap[key] = color
            PieChartView.Slice(if (key.isEmpty()) "未命名" else key, c.minutes, color)
        }
        currentSlices = slices
        contentColorMap = colorMap
        binding.pieChart.setData(slices, "总专注\n${formatMinutes(currentTotal)}")

        // 2) 图例（点击可改色）
        val legend = cms.mapIndexed { i, c ->
            val key = if (c.content.isBlank()) "" else c.content
            val color = colorMap[key] ?: palette[i % palette.size]
            val pct = if (currentTotal > 0) c.minutes * 100 / currentTotal else 0
            LegendAdapter.LegendItem(color, if (key.isEmpty()) "未命名" else key, c.minutes, pct)
        }
        legendAdapter.submit(legend)

        // 3) 事件列表
        val recs = lastRecs
        val items = recs.map { r ->
            val raw = if (r.content.isBlank()) "" else r.content
            val content = if (raw.isEmpty()) "未命名" else raw
            val color = colorMap[raw] ?: if (raw.isEmpty()) palette[6] else palette[0]
            val time = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(r.createdAt))
            RecordAdapter.RecordItem(color, content, time, r.durationMinutes)
        }
        recordAdapter.submit(items)
        binding.tvEventsTitle.text = "专注事件（${recs.size}）"
        binding.tvEmpty.visibility = if (recs.isEmpty()) View.VISIBLE else View.GONE
        binding.rvRecords.visibility = if (recs.isEmpty()) View.GONE else View.VISIBLE
        updateShareEnabled()
    }

    /** 点击图例 → 弹出色板，选中后写入并刷新饼图/图例 */
    private fun showColorPicker(content: String) {
        if (content == "未命名") {
            Toast.makeText(this, "未命名内容不可改色", Toast.LENGTH_SHORT).show()
            return
        }
        colorPickerDialog?.dismiss()
        val ctx = this
        val grid = GridLayout(ctx).apply {
            columnCount = 4
            setPadding(24, 16, 24, 16)
        }
        val dm = resources.displayMetrics
        val size = (dm.density * 54).toInt()
        val m = (dm.density * 8).toInt()
        for (color in palette) {
            val swatch = View(ctx).apply {
                setBackgroundColor(color)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size; height = size; setMargins(m, m, m, m)
                }
                setOnClickListener {
                    ContentColorStore.setColor(ctx, content, color)
                    renderData()
                    colorPickerDialog?.dismiss()
                }
            }
            grid.addView(swatch)
        }
        colorPickerDialog = MaterialAlertDialogBuilder(ctx)
            .setTitle("「$content」的颜色")
            .setView(grid)
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateShareEnabled() {
        val hasData = currentSlices.isNotEmpty()
        binding.btnShare.isEnabled = hasData
        binding.btnShare.alpha = if (hasData) 1f else 0.5f
    }

    private fun formatMinutes(min: Int): String = when {
        min >= 60 -> "${min / 60} 小时 ${min % 60} 分"
        else -> "$min 分钟"
    }

    // ---------------- 分享 ----------------

    private fun sharePie() {
        if (currentSlices.isEmpty()) {
            Toast.makeText(this, "暂无数据可分享", Toast.LENGTH_SHORT).show()
            return
        }
        val bmp = buildShareBitmap()
        val uri = saveBitmap(bmp)
        if (uri == null) {
            Toast.makeText(this, "分享失败，请重试", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, "我的专注内容占比")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "分享饼图"))
    }

    /** 把饼图 + 图例合成一张分享海报（1080 宽），绘制逻辑与屏幕一致 */
    private fun buildShareBitmap(): Bitmap {
        val w = 1080
        val pad = 64f
        val slices = currentSlices
        val total = currentTotal
        val label = binding.tvPeriodLabel.text.toString()

        val titleSize = 52f
        val subSize = 32f
        val legendSize = 32f
        val pieR = 280f
        val legendGap = 72f
        val legendH = slices.size * legendGap
        val h = (pad + titleSize + 24 + subSize + 40 + pieR * 2 + 48 + legendH + pad).toInt()

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#FFF8EA"))

        var y = pad
        drawCenter(c, "专注内容占比", w / 2f, y + titleSize, titleSize, Color.parseColor("#212121"), true)
        y += titleSize + 24
        drawCenter(c, label, w / 2f, y + subSize, subSize, Color.parseColor("#9E9E9E"), false)
        y += subSize + 40

        val cx = w / 2f
        val cy = y + pieR
        PieChartView.drawPie(c, cx, cy, pieR, slices, 0.52f, Color.WHITE, "总专注\n${formatMinutes(total)}")
        y += pieR * 2 + 48

        var ly = y
        for (s in slices) {
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = s.color }
            c.drawCircle(pad + legendSize / 2f, ly + legendSize / 2f, 13f, dotPaint)
            drawLeft(c, s.label, pad + 44f, ly + legendSize, legendSize, Color.parseColor("#212121"), false)
            val pct = if (total > 0) s.value * 100 / total else 0
            drawRight(c, "$pct%  ·  ${s.value} 分", w - pad, ly + legendSize, legendSize, Color.parseColor("#757575"), false)
            ly += legendGap
        }
        return bmp
    }

    private fun saveBitmap(bmp: Bitmap): Uri? = runCatching {
        val dir = File(cacheDir, "posters")
        dir.mkdirs()
        val file = File(dir, "focus_pie_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }.getOrNull()

    private fun drawCenter(c: android.graphics.Canvas, text: String, cx: Float, baseY: Float, size: Float, color: Int, bold: Boolean) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER; textSize = size; this.color = color
            if (bold) isFakeBoldText = true
        }
        val fm = p.fontMetrics
        c.drawText(text, cx, baseY - (fm.ascent + fm.descent) / 2f, p)
    }

    private fun drawLeft(c: android.graphics.Canvas, text: String, x: Float, baseY: Float, size: Float, color: Int, bold: Boolean) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.LEFT; textSize = size; this.color = color
            if (bold) isFakeBoldText = true
        }
        val fm = p.fontMetrics
        c.drawText(text, x, baseY - (fm.ascent + fm.descent) / 2f, p)
    }

    private fun drawRight(c: android.graphics.Canvas, text: String, x: Float, baseY: Float, size: Float, color: Int, bold: Boolean) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.RIGHT; textSize = size; this.color = color
            if (bold) isFakeBoldText = true
        }
        val fm = p.fontMetrics
        c.drawText(text, x, baseY - (fm.ascent + fm.descent) / 2f, p)
    }
}
