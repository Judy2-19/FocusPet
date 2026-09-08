package com.example.focuspets.ui.onboarding

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.focuspets.MainActivity
import com.example.focuspets.databinding.ActivityOnboardingBinding
import com.example.focuspets.model.Backgrounds
import com.example.focuspets.model.SettingsManager

/**
 * 新手引导（Onboarding）：首次安装启动后展示，三页 - 欢迎 / 怎么玩 / 选背景色。
 *
 * 触发时机：MainActivity 启动时检查 SettingsManager.isOnboardingSeen()，未看过则先来这里。
 * 完成（或跳过）后写入「已看过」+「已选过背景色」两个标记，再进入 MainActivity，
 * 因此首页不会再重复弹背景色选择，也不会重复弹引导。
 * 设置页提供「重新查看新手引导」入口，通过 resetOnboarding() 清标记后再次进入。
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    /** 当前选中的背景色（默认取已保存的，首次则是默认暖米色） */
    private var selectedColor: String = SettingsManager.DEFAULT_BG

    private val swatchViews = mutableListOf<ImageView>()
    private val dots = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedColor = SettingsManager.getBgColor(this)
        applyBackground()

        buildColorGrid()
        buildDots(binding.flipper.childCount)
        updateUi(0)

        binding.btnSkip.setOnClickListener { finishOnboarding() }
        binding.btnNext.setOnClickListener {
            val idx = binding.flipper.displayedChild
            if (idx < binding.flipper.childCount - 1) {
                binding.flipper.showNext()
                updateUi(binding.flipper.displayedChild)
            } else {
                finishOnboarding()
            }
        }
    }

    // ---------------- 背景色 ----------------

    /** 把当前背景色应用到窗口与根布局（深色模式下会自动压暗） */
    private fun applyBackground() {
        val color = Backgrounds.colorInt(this)
        window.decorView.setBackgroundColor(color)
        binding.onboardingRoot.setBackgroundColor(color)
        // 状态栏 / 导航栏跟随用户选中的背景色，避免默认紫色边框；浅底用深色状态栏图标
        window.statusBarColor = color
        window.navigationBarColor = color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    /** 按色板动态生成色块（4 列网格） */
    private fun buildColorGrid() {
        val size = dp(46)
        val gap = dp(10)
        for (color in SettingsManager.PALETTE) {
            val swatch = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(gap, gap, gap, gap)
                }
                background = swatchDrawable(color, color == selectedColor)
                setOnClickListener { selectColor(color) }
                scaleX = if (color == selectedColor) 1.12f else 1f
                scaleY = if (color == selectedColor) 1.12f else 1f
            }
            swatchViews.add(swatch)
            binding.colorGrid.addView(swatch)
        }
    }

    private fun selectColor(color: String) {
        selectedColor = color
        SettingsManager.setBgColor(this, color)
        applyBackground()
        SettingsManager.PALETTE.forEachIndexed { i, c ->
            val on = c == selectedColor
            swatchViews[i].background = swatchDrawable(c, on)
            swatchViews[i].scaleX = if (on) 1.12f else 1f
            swatchViews[i].scaleY = if (on) 1.12f else 1f
        }
    }

    private fun swatchDrawable(colorHex: String, selected: Boolean) =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(colorHex))
            setStroke(
                dp(if (selected) 3 else 1),
                Color.parseColor(if (selected) "#FFB300" else "#BDBDBD")
            )
        }

    // ---------------- 页码指示与按钮 ----------------

    private fun buildDots(count: Int) {
        for (i in 0 until count) {
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                    if (i > 0) marginStart = dp(7)
                }
                background = dotDrawable(i == 0)
            }
            dots.add(dot)
            binding.dots.addView(dot)
        }
    }

    private fun dotDrawable(active: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.parseColor(if (active) "#FFB300" else "#BDBDBD"))
    }

    private fun updateUi(index: Int) {
        dots.forEachIndexed { i, v -> v.background = dotDrawable(i == index) }
        val last = index == binding.flipper.childCount - 1
        binding.btnNext.text = if (last) "开始体验" else "下一步"
        binding.btnSkip.visibility = if (last) View.GONE else View.VISIBLE
    }

    // ---------------- 收尾 ----------------

    /**
     * 引导结束：写入「已看过引导」与「已选背景色」，然后进入主页。
     * 写 bg_chosen 是为了让首页不再重复弹出背景色选择（两者都在首次启动发生）。
     */
    private fun finishOnboarding() {
        SettingsManager.setBgColor(this, selectedColor)
        SettingsManager.setBgChosen(this)
        SettingsManager.setOnboardingSeen(this)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    /**
     * 引导是首启闸门，返回无处可退 —— 视为完成引导直接进入主页。
     * 这里自己消费掉返回事件（不调用 super），避免退出后下次启动又从引导开始。
     */
    @Suppress("MissingSuperCall", "DEPRECATION")
    override fun onBackPressed() {
        finishOnboarding()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
