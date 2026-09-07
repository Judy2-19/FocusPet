package com.example.focuspets.ui.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.lifecycle.lifecycleScope
import com.example.focuspets.MainActivity
import com.example.focuspets.R
import com.example.focuspets.databinding.FragmentHomeBinding
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.PetRepository
import com.example.focuspets.debug.DebugHelper
import com.example.focuspets.model.CatWardrobe
import com.example.focuspets.model.PetCareState
import com.example.focuspets.model.PetMood
import com.example.focuspets.model.Backgrounds
import com.example.focuspets.model.SettingsManager
import com.example.focuspets.ui.settings.BgColorPickerDialog
import com.example.focuspets.service.FocusService
import com.example.focuspets.ui.wardrobe.CatWardrobeBottomSheet
import com.example.focuspets.ui.wardrobe.WARDROBE_CHANGED_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    /** 当前展示中的宠物视图（猫咪立绘 or emoji），动画统一作用在它身上 */
    private val activePetView: View
        get() = if (isCatSelected) binding.ivPetCat else binding.tvPetEmoji

    private val viewModel: HomeViewModel by viewModels {
        val db = AppDatabase.getInstance(requireActivity().applicationContext)
        HomeViewModelFactory(PetRepository(db))
    }

    // 动画引用（离开页面时取消，防止内存泄漏）
    private var breathingAnimator: AnimatorSet? = null
    private var glowPulseAnimator: ObjectAnimator? = null
    private var glowScaleAnimator: AnimatorSet? = null
    /** 当前呼吸动画是否处于 GLOW 的「更快更活泼」模式，避免每次 render 都重启 */
    private var glowBreathOn = false

    // 小猫咪活体行为：喵叫 / 侧壁跳跃 / 凑近嗅探
    private var soundPool: SoundPool? = null
    private var meowSoundId = 0
    private val behaviorHandler = Handler(Looper.getMainLooper())
    private var behaviorRunnable: Runnable? = null
    /** 当前选中的是不是小猫咪（决定用立绘还是 emoji） */
    private var isCatSelected = false
    /** 上次加载的立绘路径，用于避免重复解码 */
    private var lastCatAssetPath: String? = null

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

        // 应用用户选中的背景色（首页始终铺满用户色）
        Backgrounds.apply(requireContext(), binding.root)

        // 首次进入还没选过背景色 → 弹出色板选择（番茄 ToDo 风格）
        if (!SettingsManager.isBgChosen(requireContext())) {
            showBgPicker()
        }

        // 右上角图鉴入口
        binding.btnPokedex.setOnClickListener {
            (activity as? MainActivity)?.navigateToCollection()
        }

        setupPetGestures()
        startBreathing()

        // 装扮入口：打开换装面板；妆扮变更后重新加载立绘
        binding.btnWardrobe.setOnClickListener {
            CatWardrobeBottomSheet().show(parentFragmentManager, "cat_wardrobe")
        }

        // 换背景：随时重新选择全局背景色
        binding.btnBg.setOnClickListener { showBgPicker() }
        parentFragmentManager.setFragmentResultListener(
            WARDROBE_CHANGED_KEY, viewLifecycleOwner
        ) { _, _ -> loadCatImage() }

        initMeowSound()

        // 调试：循环切换心情三态，验证 SICK / NORMAL / GLOW 渲染（仅 test 分支）
        fun moodLabel(m: PetMood) = when (m) {
            PetMood.SICK -> "生病"
            PetMood.GLOW -> "发光"
            PetMood.NORMAL -> "普通"
        }
        binding.btnDebugMood.text = "🎭 调试心情：" + moodLabel(PetCareState.getMood(requireContext()))
        binding.btnDebugMood.setOnClickListener {
            val mood = DebugHelper.cycleMood(requireContext())
            viewModel.refreshMood(requireContext())
            binding.btnDebugMood.text = "🎭 调试心情：" + moodLabel(mood)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { render(it) }
    }

    /** 弹出背景色选择（首次进入 or 手动点击「换背景」） */
    private fun showBgPicker() {
        if (_binding == null) return
        val dialog = BgColorPickerDialog()
        dialog.onApplied = { Backgrounds.apply(requireContext(), binding.root) }
        dialog.show(parentFragmentManager, "bg_picker")
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
        if (isCatSelected) loadCatImage()   // 装备可能在别处改过，回来刷新一次
        scheduleNextBehavior()
    }

    override fun onPause() {
        super.onPause()
        stopBehaviors()
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
        binding.petTouchArea.setOnTouchListener { _, event ->
            // 必须返回 true 持续消费事件，否则 gesture detector 可能收不到完整手势
            detector.onTouchEvent(event)
            true
        }
    }

    // ---------------- 属性动画 ----------------

    /** 呼吸动画：scale 1.0 ↔ 1.06 无限往返；glow=true 时更快、幅度更大，显得更活泼 */
    private fun startBreathing(glow: Boolean = false) {
        // repeatMode / repeatCount 是 ObjectAnimator 的属性，不能写在 AnimatorSet 上
        val target = activePetView
        breathingAnimator?.cancel()
        val amp = if (glow) 1.10f else 1.06f
        val dur = if (glow) 600L else 1200L
        val scaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, amp).apply {
            duration = dur
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, amp).apply {
            duration = dur
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        breathingAnimator = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }
    }

    /** 切换宠物时的跳跃动画 */
    private fun playJump() {
        ObjectAnimator.ofFloat(activePetView, View.TRANSLATION_Y, 0f, -56f, 0f).apply {
            duration = 420
            interpolator = OvershootInterpolator()
            start()
        }
    }

    // ---------------- 小猫咪活体行为 ----------------

    /** 按当前装备从 assets 加载立绘（IO 线程解码，避免卡主线程） */
    private fun loadCatImage() {
        // 先取 applicationContext，避免协程执行时 Fragment 已解绑导致 requireContext() 抛异常
        val appContext = context?.applicationContext ?: return
        val path = CatWardrobe.assetPath(appContext)
        // 立绘没变就不重复解码（心情/积分变化也会触发 render）
        if (path == lastCatAssetPath) return
        lastCatAssetPath = path
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    appContext.assets.open(path).use { BitmapFactory.decodeStream(it) }
                } catch (e: Exception) {
                    null
                }
            }
            if (_binding == null) return@launch
            if (bitmap != null) binding.ivPetCat.setImageBitmap(bitmap)
        }
    }

    private fun initMeowSound() {
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        meowSoundId = soundPool?.load(requireContext(), R.raw.meow, 1) ?: 0
    }

    /** 随机调度下一次行为：喵叫 / 侧壁跳跃 / 凑近嗅探 */
    private fun scheduleNextBehavior() {
        behaviorRunnable?.let { behaviorHandler.removeCallbacks(it) }
        behaviorRunnable = Runnable {
            if (isCatSelected && isResumed && _binding != null) {
                when ((0..2).random()) {
                    0 -> playMeow()
                    1 -> playEdgeJump()
                    else -> playSniff()
                }
            }
            scheduleNextBehavior()
        }
        behaviorHandler.postDelayed(behaviorRunnable!!, (4_000L..9_000L).random())
    }

    private fun stopBehaviors() {
        behaviorRunnable?.let { behaviorHandler.removeCallbacks(it) }
        behaviorRunnable = null
    }

    /** 喵喵叫：播放音效（音量小）+ 冒出「喵~」气泡 */
    private fun playMeow() {
        soundPool?.play(meowSoundId, 0.25f, 0.25f, 1, 0, 1f)
        binding.tvMeowBubble.visibility = View.VISIBLE
        binding.tvMeowBubble.alpha = 0f
        binding.tvMeowBubble.animate().alpha(1f).setDuration(180).start()
        behaviorHandler.postDelayed({
            if (_binding != null) binding.tvMeowBubble.visibility = View.GONE
        }, 1100)
    }

    /** 在屏幕两侧壁之间跳来跳去：左右各跳一次再回中间 */
    private fun playEdgeJump() {
        val target = activePetView
        val distance = resources.displayMetrics.widthPixels * 0.30f
        val dir = if ((0..1).random() == 0) -1f else 1f
        AnimatorSet().apply {
            playSequentially(
                // 跳到一侧
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(target, View.TRANSLATION_X, 0f, distance * dir),
                        ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, 0f, -120f, 0f)
                    )
                    duration = 520
                },
                // 再跳到另一侧
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(
                            target, View.TRANSLATION_X, distance * dir, distance * -dir
                        ),
                        ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, 0f, -120f, 0f)
                    )
                    duration = 620
                },
                // 回到中间
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(target, View.TRANSLATION_X, distance * -dir, 0f),
                        ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, 0f, -80f, 0f)
                    )
                    duration = 520
                }
            )
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /** 凑近屏幕嗅嗅：放大 + 左右轻晃，结束后恢复并重启呼吸 */
    private fun playSniff() {
        val target = activePetView
        breathingAnimator?.cancel()   // 呼吸动画也在改 scale，先让位避免冲突
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.22f),
                ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.22f),
                ObjectAnimator.ofFloat(target, View.ROTATION, 0f, -5f, 5f, -3f, 0f)
            )
            duration = 900
            start()
        }
        behaviorHandler.postDelayed({
            if (_binding == null) return@postDelayed
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(target, View.SCALE_X, 1f),
                    ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f)
                )
                duration = 350
                start()
            }
            startBreathing()
        }, 1000)
    }

    /** GLOW 状态：柔光光晕呼吸（alpha + scale 脉冲），营造「周围有光晕」的活泼氛围 */
    private fun startGlowPulse() {
        if (glowPulseAnimator?.isRunning != true) {
            glowPulseAnimator = ObjectAnimator.ofFloat(binding.vGlow, View.ALPHA, 0.5f, 1f).apply {
                duration = 700
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        }
        if (glowScaleAnimator?.isRunning != true) {
            val sx = ObjectAnimator.ofFloat(binding.vGlow, View.SCALE_X, 0.92f, 1.08f).apply {
                duration = 1100
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            val sy = ObjectAnimator.ofFloat(binding.vGlow, View.SCALE_Y, 0.92f, 1.08f).apply {
                duration = 1100
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            glowScaleAnimator = AnimatorSet().apply {
                playTogether(sx, sy)
                start()
            }
        }
    }

    private fun stopGlowPulse() {
        glowPulseAnimator?.cancel()
        glowPulseAnimator = null
        glowScaleAnimator?.cancel()
        glowScaleAnimator = null
        // 复位光晕视图，避免离开 GLOW 后留下缩放/透明度残留
        binding.vGlow.scaleX = 1f
        binding.vGlow.scaleY = 1f
        binding.vGlow.alpha = 1f
    }

    // ---------------- 渲染 ----------------

    private fun render(state: HomeUiState) {
        val context = requireContext()

        // 宠物渲染：1 号小猫咪用 assets 立绘，其余宠物仍用 emoji
        val isCat = state.selectedPet.id == CatWardrobe.CAT_PET_ID
        if (isCat != isCatSelected) {
            isCatSelected = isCat
            startBreathing(glow = glowBreathOn)      // 动画目标视图换了，重启呼吸（保持当前 GLOW 节奏）
        }
        if (isCat) {
            binding.ivPetCat.visibility = View.VISIBLE
            binding.tvPetEmoji.visibility = View.GONE
            binding.btnWardrobe.visibility = View.VISIBLE
            loadCatImage()
        } else {
            binding.ivPetCat.visibility = View.GONE
            binding.tvPetEmoji.visibility = View.VISIBLE
            binding.btnWardrobe.visibility = View.GONE
            binding.tvPetEmoji.text = state.selectedPet.emoji
        }

        // 收集进度
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
                activePetView.alpha = 0.55f      // 无精打采
                stopGlowPulse()
                if (glowBreathOn) {        // 退出 GLOW：呼吸恢复普通节奏
                    glowBreathOn = false
                    startBreathing(glow = false)
                }
            }

            PetMood.GLOW -> {
                binding.tvMoodTag.text = "✨ 容光焕发！"
                binding.tvMoodTag.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.rarity_legend)
                )
                binding.tvPetSick.visibility = View.GONE
                binding.vGlow.visibility = View.VISIBLE
                activePetView.alpha = 1f
                startGlowPulse()
                // 小猫更活泼：切换到更快、幅度更大的呼吸（仅切换一次，避免每次 render 重启）
                if (!glowBreathOn) {
                    glowBreathOn = true
                    startBreathing(glow = true)
                }
            }

            PetMood.NORMAL -> {
                binding.tvMoodTag.text = "😊 陪伴中"
                binding.tvMoodTag.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.rarity_rare)
                )
                binding.tvPetSick.visibility = View.GONE
                binding.vGlow.visibility = View.GONE
                activePetView.alpha = 1f
                stopGlowPulse()
                if (glowBreathOn) {        // 退出 GLOW：呼吸恢复普通节奏
                    glowBreathOn = false
                    startBreathing(glow = false)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        breathingAnimator?.cancel()
        stopGlowPulse()
        stopBehaviors()
        soundPool?.release()
        soundPool = null
        lastCatAssetPath = null   // 视图重建后需重新解码立绘
        _binding = null
    }
}
