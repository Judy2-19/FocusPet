package com.example.focuspets.ui.stats

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.focuspets.MainActivity
import com.example.focuspets.R
import com.example.focuspets.databinding.FragmentStatsBinding
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.PetRepository
import com.example.focuspets.model.Backgrounds
import com.example.focuspets.ui.stats.BadgeAdapter

class FragmentStats : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatsViewModel by viewModels {
        val db = AppDatabase.getInstance(requireActivity().applicationContext)
        StatsViewModelFactory(PetRepository(db))
    }

    private val badgeAdapter = BadgeAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply the selected background color.
        Backgrounds.apply(requireContext(), binding.root)

        // Global leaderboard entry moved to Statistics because bottom navigation supports at most five items.
        binding.btnLeaderboard.setOnClickListener {
            (requireActivity() as MainActivity).showLeaderboard()
        }

        // Three-column achievement grid.
        binding.badges.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.badges.adapter = badgeAdapter

        viewModel.today.observe(viewLifecycleOwner) {
            binding.tvToday.text = formatMinutes(it)
        }
        viewModel.total.observe(viewLifecycleOwner) {
            binding.tvTotal.text = formatMinutes(it)
        }
        viewModel.sessions.observe(viewLifecycleOwner) {
            binding.tvSessions.text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_002 ,(it).toString())
        }
        viewModel.streak.observe(viewLifecycleOwner) {
            binding.tvStreak.text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_001 ,(it).toString())
        }
        viewModel.heatmap.observe(viewLifecycleOwner) {
            binding.heatmap.setData(it)
        }
        viewModel.achievements.observe(viewLifecycleOwner) {
            badgeAdapter.submit(it)
        }
        viewModel.last7.observe(viewLifecycleOwner) {
            buildBarChart(it)
        }
    }

    private fun formatMinutes(min: Int): String = when {
        min >= 60 -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_019 ,(min / 60).toString(), (min % 60).toString())
        else -> com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_007 ,(min).toString())
    }

    /** Seven-day bars share a bottom baseline and scale to the maximum; reserve weekday-label space to prevent clipping. */
    private fun buildBarChart(bars: List<com.example.focuspets.db.DayBar>) {
        if (_binding == null || bars.isEmpty()) return
        binding.barChart.removeAllViews()
        val max = bars.maxOfOrNull { it.minutes } ?: 0
        val density = resources.displayMetrics.density
        // 180dp container with 14dp padding leaves about 152dp content; reserve 24dp for weekday labels.
        val contentH = (180 * density).toInt() - 2 * (14 * density).toInt()
        val labelArea = (24 * density).toInt()
        val maxBarH = (contentH - labelArea).coerceAtLeast((40 * density).toInt())
        val barW = (24 * density).toInt()
        val minBar = (6 * density).toInt()
        val accent = ContextCompat.getColor(requireContext(), R.color.cat_accent)

        for (bar in bars) {
            val col = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                // Bottom-align columns so all bars share a baseline and weekday labels remain at the bottom.
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            }
            val h = if (max == 0) minBar
            else ((bar.minutes.toFloat() / max) * maxBarH).toInt().coerceAtLeast(minBar)

            // Values above bars; tapping also reveals exact minutes.
            val valueLabel = TextView(requireContext()).apply {
                text = if (bar.minutes > 0) "${bar.minutes}" else ""
                textSize = 11f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.cat_accent))
                gravity = Gravity.CENTER
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = (3 * density).toInt()
                layoutParams = lp
            }

            val barView = View(requireContext()).apply {
                setBackgroundColor(accent)
                val lp = LinearLayout.LayoutParams(barW, h)
                lp.bottomMargin = (6 * density).toInt()
                layoutParams = lp
            }
            val label = TextView(requireContext()).apply {
                text = bar.label
                textSize = 11f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
                gravity = Gravity.CENTER
            }
            col.addView(valueLabel)
            col.addView(barView)
            col.addView(label)

            // Tap a bar to show that day's minutes.
            col.setOnClickListener {
                val msg = if (bar.minutes > 0) com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_013 ,(bar.label).toString(), (bar.minutes).toString())
                else com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_014 ,(bar.label).toString())
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }

            binding.barChart.addView(col)
        }
        binding.tvChartHint.text =
            if (max == 0) com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_116)
            else com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_024 ,(max).toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
