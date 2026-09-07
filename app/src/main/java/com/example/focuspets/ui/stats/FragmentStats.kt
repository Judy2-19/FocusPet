package com.example.focuspets.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.focuspets.R
import com.example.focuspets.databinding.FragmentStatsBinding
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.PetRepository
import com.example.focuspets.model.Backgrounds

class FragmentStats : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatsViewModel by viewModels {
        val db = AppDatabase.getInstance(requireActivity().applicationContext)
        StatsViewModelFactory(PetRepository(db))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 应用用户选中的背景色
        Backgrounds.apply(requireContext(), binding.root)

        viewModel.today.observe(viewLifecycleOwner) {
            binding.tvToday.text = formatMinutes(it)
        }
        viewModel.total.observe(viewLifecycleOwner) {
            binding.tvTotal.text = formatMinutes(it)
        }
        viewModel.sessions.observe(viewLifecycleOwner) {
            binding.tvSessions.text = "$it 次"
        }
        viewModel.streak.observe(viewLifecycleOwner) {
            binding.tvStreak.text = "$it 天"
        }
        viewModel.last7.observe(viewLifecycleOwner) {
            buildBarChart(it)
        }
    }

    private fun formatMinutes(min: Int): String = when {
        min >= 60 -> "${min / 60} 小时 ${min % 60} 分"
        else -> "$min 分钟"
    }

    /** 近 7 天柱状图：每根柱子底部对齐，高度按最大值等比缩放 */
    private fun buildBarChart(bars: List<com.example.focuspets.db.DayBar>) {
        if (_binding == null || bars.isEmpty()) return
        binding.barChart.removeAllViews()
        val max = bars.maxOfOrNull { it.minutes } ?: 0
        val density = resources.displayMetrics.density
        val availH = (140 * density).toInt()      // 柱子可用最大高度
        val barW = (26 * density).toInt()
        val accent = ContextCompat.getColor(requireContext(), R.color.cat_accent)

        for (bar in bars) {
            val col = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                layoutParams = lp
            }
            val h = if (max == 0) (8 * density).toInt()
            else ((bar.minutes.toFloat() / max) * availH).toInt().coerceAtLeast((8 * density).toInt())

            val barView = View(requireContext()).apply {
                setBackgroundColor(accent)
                val lp = LinearLayout.LayoutParams(barW, h)
                lp.bottomMargin = (6 * density).toInt()
                layoutParams = lp
            }
            val label = TextView(requireContext()).apply {
                text = bar.label
                textSize = 11f
                setTextColor(0xFF757575.toInt())
                gravity = android.view.Gravity.CENTER
            }
            col.addView(barView)
            col.addView(label)
            binding.barChart.addView(col)
        }
        binding.tvChartHint.text =
            if (max == 0) "还没有专注记录，开始第一个番茄钟吧～"
            else "近 7 天最高 ${max} 分钟 · 数据来自专注记录"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
