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
 * 首次进入时弹出，让用户从柔和色板里挑一个背景色（番茄 ToDo 风格）。
 * 选完即持久化并在首页应用；也可「稍后再说」用默认色。
 */
class BgColorPickerDialog : DialogFragment() {

    private var _binding: DialogBgColorBinding? = null
    private val binding get() = _binding!!

    /** 选完/应用后回调（用于立即刷新当前页面背景） */
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
            // 用默认色，仅标记「已选过」，避免每次进首页都弹
            SettingsManager.setBgColor(requireContext(), SettingsManager.DEFAULT_BG)
            SettingsManager.setBgChosen(requireContext())
            onApplied?.invoke()
            dismiss()
        }
    }

    private fun buildSwatches() {
        val size = (resources.displayMetrics.density * 52).toInt()
        for (color in SettingsManager.PALETTE) {
            val swatch = View(requireContext()).apply {
                val d = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(color))
                    setStroke((resources.displayMetrics.density * 4).toInt(), Color.WHITE)
                }
                background = d
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
