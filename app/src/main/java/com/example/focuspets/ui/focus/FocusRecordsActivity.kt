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
 * Focus records screen: day/week/month/year ranges, previous/next/today navigation, content-minute
 * donut chart and percentage legend, FileProvider sharing, and detailed task/completion/duration
 * records.
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

    /** User-selectable pie palette: pink, purple, yellow, blue, green, orange, gray, brown. */
    private val palette = intArrayOf(
        0xFFE91E63.toInt(), // Pink.
        0xFF9C27B0.toInt(), // Purple.
        0xFFFBC02D.toInt(), // Yellow.
        0xFF2196F3.toInt(), // Blue.
        0xFF4CAF50.toInt(), // Green.
        0xFFFF9800.toInt(), // Orange.
        0xFF9E9E9E.toInt(), // Gray for unnamed content.
        0xFF795548.toInt()  // Brown.
    )

    private enum class Period { DAY, WEEK, MONTH, YEAR }

    private var periodType = Period.DAY
    private var anchorMs = System.currentTimeMillis()

    // Current data snapshot for sharing.
    private var currentSlices: List<PieChartView.Slice> = emptyList()
    private var currentTotal = 0
    private var contentColorMap: Map<String, Int> = emptyMap()

    // Latest aggregate for recoloring without querying the database again.
    private var lastCms: List<ContentMinutes> = emptyList()
    private var lastRecs: List<com.example.focuspets.db.entity.FocusRecordEntity> = emptyList()

    /** Color picker reference so it can close after selection. */
    private var colorPickerDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Let the system inflate and attach the ScrollView, avoiding duplicate addView calls
        // and the child-already-has-a-parent error. Bind using the actual ScrollView
        // root @id/scroll_root rather than casting ContentFrameLayout to ScrollView.
        setContentView(R.layout.activity_focus_records)
        binding = ActivityFocusRecordsBinding.bind(findViewById(R.id.scroll_root))
        Backgrounds.apply(this, binding.root)

        binding.rvLegend.layoutManager = LinearLayoutManager(this)
        binding.rvLegend.adapter = legendAdapter
        binding.rvRecords.layoutManager = LinearLayoutManager(this)
        binding.rvRecords.adapter = recordAdapter

        // Tap a legend entry to change its content's pie color.
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

    // ---------------- Calendar ----------------

    private fun setupCalendar() {
        val now = Calendar.getInstance()
        binding.calendar.showMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
        updateCalendarTitle()
        refreshCalendarMarks()
        binding.calendar.setSelectedDay(now.get(Calendar.DAY_OF_MONTH))

        binding.calendar.setOnDayClickListener { y, m, d ->
            // Tap a date to switch to daily view and load that day's records/chart.
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

    /** Move to the previous/next month and refresh activity dots. */
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
            com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_016 ,(cal.get(Calendar.YEAR)).toString(), (cal.get(Calendar.MONTH) + 1).toString())
    }

    /** Load active dates for the displayed month and mark them with dots. */
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
            // Keep only day numbers belonging to the displayed month.
            val marked = map.keys.mapNotNull { dateStr ->
                dateStr.takeLast(2).toIntOrNull()
            }.toSet()
            binding.calendar.setMarkedDays(marked)
        }
    }

    // ---------------- Period calculations ----------------

    /** Compute [startMs, endMs) and the title from periodType and anchorMs. */
    private fun computeRange(): Triple<Long, Long, String> {
        val cal = Calendar.getInstance().apply { timeInMillis = anchorMs }
        return when (periodType) {
            Period.DAY -> {
                cal.zeroTime()
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                val end = cal.timeInMillis
                Triple(start, end, SimpleDateFormat(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_058), com.example.focuspets.model.UiStrings.locale()).format(Date(start)))
            }
            Period.WEEK -> {
                cal.zeroTime()
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                val offset = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
                cal.add(Calendar.DAY_OF_MONTH, -offset)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 7)
                val end = cal.timeInMillis
                val sf = SimpleDateFormat(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_099), com.example.focuspets.model.UiStrings.locale())
                val s = sf.format(Date(start))
                val e = sf.format(Date(end - 1))
                Triple(start, end, "$s - $e")
            }
            Period.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.zeroTime()
                val start = cal.timeInMillis
                val label = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_016 ,(cal.get(Calendar.YEAR)).toString(), (cal.get(Calendar.MONTH) + 1).toString())
                cal.add(Calendar.MONTH, 1)
                val end = cal.timeInMillis
                Triple(start, end, label)
            }
            Period.YEAR -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.zeroTime()
                val start = cal.timeInMillis
                val label = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_015 ,(cal.get(Calendar.YEAR)).toString())
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

    // ---------------- Data binding ----------------

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
            binding.tvSummarySessions.text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_002 ,(it).toString())
        }
    }

    /**
     * Render the chart, legend, and records from the last aggregate and saved/default colors. Call again
     * after recoloring without another database query.
     */
    private fun renderData() {
        val cms = lastCms
        currentTotal = cms.sumOf { it.minutes }

        // 1. Prefer saved content colors; otherwise assign and persist the next palette color.
        val colorMap = mutableMapOf<String, Int>()
        val slices = cms.mapIndexed { i, c ->
            val key = if (c.content.isBlank()) "" else c.content
            val color = if (key.isEmpty()) palette[6] // Gray means unnamed content.
            else ContentColorStore.getColor(this@FocusRecordsActivity, key)
                ?: palette[i % palette.size].also {
                    ContentColorStore.setColor(this@FocusRecordsActivity, key, it)
                }
            colorMap[key] = color
            PieChartView.Slice(if (key.isEmpty()) com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_173) else key, c.minutes, color)
        }
        currentSlices = slices
        contentColorMap = colorMap
        binding.pieChart.setData(slices, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_154 ,(formatMinutes(currentTotal)).toString()))

        // 2. Legend entries support recoloring on tap.
        val legend = cms.mapIndexed { i, c ->
            val key = if (c.content.isBlank()) "" else c.content
            val color = colorMap[key] ?: palette[i % palette.size]
            val pct = if (currentTotal > 0) c.minutes * 100 / currentTotal else 0
            LegendAdapter.LegendItem(color, if (key.isEmpty()) com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_173) else key, c.minutes, pct)
        }
        legendAdapter.submit(legend)

        // 3. Record list.
        val recs = lastRecs
        val items = recs.map { r ->
            val raw = if (r.content.isBlank()) "" else r.content
            val content = if (raw.isEmpty()) com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_173) else raw
            val color = colorMap[raw] ?: if (raw.isEmpty()) palette[6] else palette[0]
            val time = SimpleDateFormat(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_100), com.example.focuspets.model.UiStrings.locale()).format(Date(r.createdAt))
            RecordAdapter.RecordItem(color, content, time, r.durationMinutes)
        }
        recordAdapter.submit(items)
        binding.tvEventsTitle.text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_062 ,(recs.size).toString())
        binding.tvEmpty.visibility = if (recs.isEmpty()) View.VISIBLE else View.GONE
        binding.rvRecords.visibility = if (recs.isEmpty()) View.GONE else View.VISIBLE
        updateShareEnabled()
    }

    /** Show the palette on legend tap; persist the selection and refresh chart/legend. */
    private fun showColorPicker(content: String) {
        if (content == com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_173)) {
            Toast.makeText(this, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_172), Toast.LENGTH_SHORT).show()
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
            .setTitle(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_049 ,(content).toString()))
            .setView(grid)
            .setNegativeButton(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_042), null)
            .show()
    }

    private fun updateShareEnabled() {
        val hasData = currentSlices.isNotEmpty()
        binding.btnShare.isEnabled = hasData
        binding.btnShare.alpha = if (hasData) 1f else 0.5f
    }

    private fun formatMinutes(min: Int): String = when {
        min >= 60 -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_019 ,(min / 60).toString(), (min % 60).toString())
        else -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_007 ,(min).toString())
    }

    // ---------------- Sharing ----------------

    private fun sharePie() {
        if (currentSlices.isEmpty()) {
            Toast.makeText(this, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_114), Toast.LENGTH_SHORT).show()
            return
        }
        val bmp = buildShareBitmap()
        val uri = saveBitmap(bmp)
        if (uri == null) {
            Toast.makeText(this, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_168), Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_107))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_137)))
    }

    /** Render a 1080-pixel-wide chart/legend poster using the same drawing logic as the screen. */
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
        drawCenter(c, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_063), w / 2f, y + titleSize, titleSize, Color.parseColor("#212121"), true)
        y += titleSize + 24
        drawCenter(c, label, w / 2f, y + subSize, subSize, Color.parseColor("#9E9E9E"), false)
        y += subSize + 40

        val cx = w / 2f
        val cy = y + pieR
        PieChartView.drawPie(c, cx, cy, pieR, slices, 0.52f, Color.WHITE, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_155 ,(formatMinutes(total)).toString()))
        y += pieR * 2 + 48

        var ly = y
        for (s in slices) {
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = s.color }
            c.drawCircle(pad + legendSize / 2f, ly + legendSize / 2f, 13f, dotPaint)
            drawLeft(c, s.label, pad + 44f, ly + legendSize, legendSize, Color.parseColor("#212121"), false)
            val pct = if (total > 0) s.value * 100 / total else 0
            drawRight(c, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_011 ,(pct).toString(), (s.value).toString()), w - pad, ly + legendSize, legendSize, Color.parseColor("#757575"), false)
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
