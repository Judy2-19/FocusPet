package com.example.focuspets.ui.settings

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.fragment.app.DialogFragment
import com.example.focuspets.R
import com.example.focuspets.databinding.DialogBgColorBinding
import com.example.focuspets.model.Backgrounds
import com.example.focuspets.model.SettingsManager

/**
 * First-entry soft background palette picker. Persist and apply the selected color, or defer using the
 * default.
 */
class BgColorPickerDialog : DialogFragment() {

    private var _binding: DialogBgColorBinding? = null
    private val binding get() = _binding!!

    /** Callback after selection/application to refresh the page background immediately. */
    var onApplied: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogBgColorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buildSwatches()

        binding.btnLater.setOnClickListener {
            // Keep the default and mark color chosen to avoid prompting on every Home entry.
            SettingsManager.setBgColor(requireContext(), SettingsManager.DEFAULT_BG)
            SettingsManager.setBgChosen(requireContext())
            onApplied?.invoke()
            dismiss()
        }
    }

    private fun buildSwatches() {
        // Chinese color names matching SettingsManager.PALETTE order for screen readers.
        val labels = listOf(
            "暖米色", "蜜桃粉", "天空蓝", "薄荷绿",
            "薰衣草紫", "奶油黄", "樱花粉", "青瓷绿"
        )
        val size = (resources.displayMetrics.density * 52).toInt()
        SettingsManager.PALETTE.forEachIndexed { index, color ->
            val label = labels.getOrElse(index) { "背景" }
            val swatch = View(requireContext()).apply {
                val d = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(color))
                    setStroke((resources.displayMetrics.density * 4).toInt(), Color.WHITE)
                }
                background = d
                contentDescription = "$label 背景色"
                val lp = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(size / 6, size / 6, size / 6, size / 6)
                }
                layoutParams = lp
                setOnClickListener {
                    SettingsManager.setBgColor(requireContext(), color)
                    SettingsManager.setBgChosen(requireContext())
                    onApplied?.invoke()
                    dismiss()
                }
            }
            binding.gridBg.addView(swatch)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
