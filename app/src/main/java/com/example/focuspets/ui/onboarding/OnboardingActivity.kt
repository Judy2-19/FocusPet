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
 * Three-page first-run onboarding: welcome, instructions, and background color. MainActivity checks
 * isOnboardingSeen first. Completion/skipping marks onboarding and color selection as done, preventing
 * duplicate prompts. Settings can reset onboarding and reopen it.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    /** Selected background: saved value or default warm beige on first launch. */
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

    // ---------------- Background color ----------------

    /** Apply the current background to the window and root, automatically darkening it in night mode. */
    private fun applyBackground() {
        val color = Backgrounds.colorInt(this)
        window.decorView.setBackgroundColor(color)
        binding.onboardingRoot.setBackgroundColor(color)
        // Match system bars to the selected background to avoid purple borders; use dark status icons on light surfaces.
        window.statusBarColor = color
        window.navigationBarColor = color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    /** Build palette swatches dynamically in a four-column grid. */
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

    // ---------------- Page indicators and buttons ----------------

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
        binding.btnNext.text = if (last) com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_080) else com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_111)
        binding.btnSkip.visibility = if (last) View.GONE else View.VISIBLE
    }

    // ---------------- Completion ----------------

    /**
     * Mark onboarding seen and background chosen, then open Home. Setting bg_chosen prevents the first-run
     * background prompt from appearing again.
     */
    private fun finishOnboarding() {
        SettingsManager.setBgColor(this, selectedColor)
        SettingsManager.setBgChosen(this)
        SettingsManager.setOnboardingSeen(this)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    /**
     * Onboarding is the first-run gate with no previous page. Consume Back as completion and open Home
     * rather than calling super and showing onboarding again next launch.
     */
    @Suppress("MissingSuperCall", "DEPRECATION")
    override fun onBackPressed() {
        finishOnboarding()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
