package com.example.focuspets.ui.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.focuspets.MainActivity
import com.example.focuspets.R
import com.example.focuspets.databinding.FragmentHomeBinding
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.PetRepository
import com.example.focuspets.model.PetMood
import com.example.focuspets.service.FocusService
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val db = AppDatabase.getInstance(requireActivity().applicationContext)
        HomeViewModelFactory(PetRepository(db))
    }

    // 动画引用（离开页面时取消，防止内存泄漏）
    private var breathingAnimator: AnimatorSet? = null
    private var glowPulseAnimator: ObjectAnimator? = null

    /** 专注结果广播：完成/失败后刷新宠物心情（收到 GLOW/ SICK 转换的触发源） */
    private val moodReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == FocusService.BROADCAST_FINISHED ||
                intent.action == FocusService.BROADCAST_FAILED
            ) {
                viewModel.refreshMood(requireContext())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 右上角图鉴入口
        binding.btnPokedex.setOnClickListener {
            (activity as? MainActivity)?.navigateToCollection()
        }

        setupPetGestures()
        startBreathing()

        viewModel.uiState.observe(viewLifecycleOwner) { render(it) }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(FocusService.BROADCAST_FINISHED)
            addAction(FocusService.BROADCAST_FAILED)
        }
        androidx.core.content.ContextCompat.registerReceiver(
            requireContext(), moodReceiver, filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(moodReceiver)
    }

    override fun onResume() {
        super.onResume()
        // 从图鉴/专注页切回来时同步一次心情
        viewModel.refreshMood(requireContext())
    }

    // ---------------- 点击 / 左右滑动切换宠物 ----------------

    private fun setupPetGestures() {
        val detector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    viewModel.selectNext()   // 点击 → 下一只
                    playJump()
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
                ): Boolean {
                    return if (abs(velocityX) > abs(velocityY)) {
                        if (velocityX < 0) viewModel.selectNext()   // 左滑 → 下一只
                        else viewModel.selectPrev()                 // 右滑 → 上一只
                        playJump()
                        true
                    } else false
                }
            }
        )
        binding.petTouchArea.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
    }

    // ---------------- 属性动画 ----------------

    /** 呼吸动画：scale 1.0 ↔ 1.06 无限往返 */
    private fun startBreathing() {
        breathingAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.tvPetEmoji, View.SCALE_X, 1f, 1.06f),
                ObjectAnimator.ofFloat(binding.tvPetEmoji, View.SCALE_Y, 1f, 1.06f)
            )
            duration = 1200
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /** 切换宠物时的跳跃动画 */
    private fun playJump() {
        ObjectAnimator.ofFloat(binding.tvPetEmoji, View.TRANSLATION_Y, 0f, -56f, 0f).apply {
            duration = 420
            interpolator = OvershootInterpolator()
            start()
        }
    }

    /** GLOW 状态：金色光环呼吸脉冲 */
    private fun startGlowPulse() {
        if (glowPulseAnimator?.isRunning == true) return
        glowPulseAnimator = ObjectAnimator.ofFloat(binding.vGlow, View.ALPHA, 0.4f, 1f).apply {
            duration = 800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun stopGlowPulse() {
        glowPulseAnimator?.cancel()
        glowPulseAnimator = null
    }

    // ---------------- 渲染 ----------------

    private fun render(state: HomeUiState) {
        val context = requireContext()

        // 宠物 + 收集进度
        binding.tvPetEmoji.text = state.selectedPet.emoji
        binding.tvPetName.text = state.selectedPet.name
        binding.tvCollectionStatus.text = "已收集 ${state.unlockedCount} / 9"

        // 积分进度条：绑定「图鉴最低解锁积分的未解锁宠物」
        if (state.nextLockedPet == null) {
            binding.progressNext.setProgressCompat(100, true)
            binding.tvProgressHint.text = "🎉 图鉴已全部收集完成！"
        } else {
            binding.progressNext.setProgressCompat(state.progressPercent, true)
            binding.tvProgressHint.text =
                "距解锁 ${state.nextLockedPet.emoji} ${state.nextLockedPet.name} 还差 ${state.remainingPoints} 积分"
        }

        // 心情三态渲染
        when (state.mood) {
            PetMood.SICK -> {
                binding.tvMoodTag.text = "🤒 生病中…完成一次专注恢复"
                binding.tvMoodTag.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.rarity_common)
                )
                binding.tvPetSick.visibility = View.VISIBLE
                binding.vGlow.visibility = View.GONE
                binding.tvPetEmoji.alpha = 0.55f      // 无精打采
                stopGlowPulse()
            }

            PetMood.GLOW -> {
                binding.tvMoodTag.text = "✨ 容光焕发！"
                binding.tvMoodTag.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.rarity_legend)
                )
                binding.tvPetSick.visibility = View.GONE
                binding.vGlow.visibility = View.VISIBLE
                binding.tvPetEmoji.alpha = 1f
                startGlowPulse()
            }

            PetMood.NORMAL -> {
                binding.tvMoodTag.text = "😊 陪伴中"
                binding.tvMoodTag.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.rarity_rare)
                )
                binding.tvPetSick.visibility = View.GONE
                binding.vGlow.visibility = View.GONE
                binding.tvPetEmoji.alpha = 1f
                stopGlowPulse()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        breathingAnimator?.cancel()
        stopGlowPulse()
        _binding = null
    }
}
