package com.example.focuspets.ui.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.TypedValue
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
import com.example.focuspets.BuildConfig
import com.example.focuspets.debug.DebugHelper
import com.example.focuspets.model.CatWardrobe
import com.example.focuspets.model.DogWardrobe
import com.example.focuspets.model.PetCareState
import com.example.focuspets.model.PetMood
import com.example.focuspets.model.Backgrounds
import com.example.focuspets.cloud.CloudSyncManager
import com.example.focuspets.model.SettingsManager
import com.example.focuspets.ui.settings.BgColorPickerDialog
import com.example.focuspets.ui.settings.SettingsActivity
import com.example.focuspets.service.FocusService
import com.example.focuspets.ui.wardrobe.CatWardrobeBottomSheet
import com.example.focuspets.ui.wardrobe.DogWardrobeBottomSheet
import com.example.focuspets.ui.wardrobe.WARDROBE_CHANGED_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    /** Current pet view, artwork or emoji; all pet animations target it. */
    private val activePetView: View
        get() = if (showingArt) binding.ivPetCat else binding.tvPetEmoji

    private val viewModel: HomeViewModel by viewModels {
        val db = AppDatabase.getInstance(requireActivity().applicationContext)
        HomeViewModelFactory(PetRepository(db))
    }

    // Animation references are cancelled on leaving the page to prevent leaks.
    private var breathingAnimator: AnimatorSet? = null
    private var glowPulseAnimator: ObjectAnimator? = null
    private var glowScaleAnimator: AnimatorSet? = null
    /** Breathing mode: normal/glow/sick. Restart only on mode changes to avoid render jitter. */
    private var breathMode = "normal"
    /** Sick state disables lively actions such as jumping and uses a subdued appearance. */
    private var isSick = false

    // Cat actions: meow, wall jumps, and sniffing toward the screen.
    private var soundPool: SoundPool? = null
    private var meowSoundId = 0
    private var barkSoundId = 0
    private val behaviorHandler = Handler(Looper.getMainLooper())
    private var behaviorRunnable: Runnable? = null
    /** Whether the current pet uses an artwork ImageView: cat/dog do, others use emoji. */
    private var showingArt = false
    /** Whether the pet is a cat, controlling wardrobe access and behaviors. */
    private var isCat = false
    /** Last artwork path shared by cat/dog so switching between them refreshes correctly. */
    private var lastArtPath: String? = null
    /** Last pet ID uploaded; sync only on changes instead of every render. */
    private var lastSyncedPetId = -1

    /** Completion/failure broadcasts refresh mood and trigger GLOW/SICK transitions. */
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

        // Fill Home with the selected background color.
        Backgrounds.apply(requireContext(), binding.root)

        // Use the leaderboard nickname in the companion title instead of the first-person placeholder.
        binding.tvTitle.text = getString(com.example.focuspets.R.string.core_message_29, CloudSyncManager.currentName())

        // Show the soft background palette on first entry if no color has been selected.
        if (!SettingsManager.isBgChosen(requireContext())) {
            showBgPicker()
        }

        // Top-right Collection entry.
        binding.btnPokedex.setOnClickListener {
            (activity as? MainActivity)?.navigateToCollection()
        }

        // Top-right Settings entry: account, deletion, progress reset, and onboarding.
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        setupPetGestures()
        startBreathing()

        // Open the matching cat/dog wardrobe and reload artwork after outfit changes.
        binding.btnWardrobe.setOnClickListener {
            if (isCat) CatWardrobeBottomSheet().show(parentFragmentManager, "cat_wardrobe")
            else DogWardrobeBottomSheet().show(parentFragmentManager, "dog_wardrobe")
        }

        // Allow changing the global background at any time.
        binding.btnBg.setOnClickListener { showBgPicker() }
        parentFragmentManager.setFragmentResultListener(
            WARDROBE_CHANGED_KEY, viewLifecycleOwner
        ) { _, _ -> if (isCat) loadCatImage() else loadDogImage() }

        initMeowSound()

        // Mood debug controls are visible only with -PisTestBuild=true to avoid accidental use on main.
        val showDebugMood = BuildConfig.DEBUG && BuildConfig.IS_TEST_BUILD
        binding.btnDebugMood.visibility = if (showDebugMood) View.VISIBLE else View.GONE
        if (showDebugMood) {
            // Cycle SICK/NORMAL/GLOW to verify rendering.
            fun moodLabel(m: PetMood) = when (m) {
                PetMood.SICK -> getString(com.example.focuspets.R.string.core_message_30)
                PetMood.GLOW -> getString(com.example.focuspets.R.string.core_message_31)
                PetMood.NORMAL -> getString(com.example.focuspets.R.string.core_message_32)
            }
            binding.btnDebugMood.text = getString(com.example.focuspets.R.string.core_message_33) + moodLabel(PetCareState.getMood(requireContext()))
            binding.btnDebugMood.setOnClickListener {
                val mood = DebugHelper.cycleMood(requireContext())
                viewModel.refreshMood(requireContext())
                binding.btnDebugMood.text = getString(com.example.focuspets.R.string.core_message_33) + moodLabel(mood)
            }
        }

        // Restore the last Home pet on reopening; cat/dog outfits persist independently.
        viewModel.selectPetById(SettingsManager.getCurrentPetId(requireContext()))

        viewModel.uiState.observe(viewLifecycleOwner) { render(it) }
    }

    /** Show the background picker on first entry or manual background changes. */
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
        // Refresh mood when returning from Collection or Focus.
        viewModel.refreshMood(requireContext())
        if (showingArt) {   // Refresh artwork in case it changed elsewhere.
            if (isCat) loadCatImage() else loadDogImage()
        }
        scheduleNextBehavior()
    }

    override fun onPause() {
        super.onPause()
        stopBehaviors()
    }

    // ---------------- Tap/swipe to switch pets ----------------

    private fun setupPetGestures() {
        val detector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    viewModel.selectNext()   // Tap selects the next pet.
                    if (!isSick) playJump()  // Do not jump when sick.
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
                ): Boolean {
                    return if (abs(velocityX) > abs(velocityY)) {
                        if (velocityX < 0) viewModel.selectNext()   // Swipe left selects the next pet.
                        else viewModel.selectPrev()                 // Swipe right selects the previous pet.
                        if (!isSick) playJump()                     // Do not jump when sick.
                        true
                    } else false
                }
            }
        )
        binding.petTouchArea.setOnTouchListener { _, event ->
            // Return true to keep consuming events so the gesture detector receives the full gesture.
            detector.onTouchEvent(event)
            true
        }
    }

    // ---------------- Property animations ----------------

    /** Breathing loops scale between 1.0 and amp; amplitude/speed vary for normal, glowing, and sick states. */
    private fun startBreathing(amp: Float = 1.06f, dur: Long = 1200L) {
        // repeatMode/repeatCount belong to ObjectAnimator, not AnimatorSet.
        val target = activePetView
        breathingAnimator?.cancel()
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

    /** When switching cat/dog view targets, restart the current breathing mode instead of defaulting to normal. */
    private fun restartBreathing() {
        when (breathMode) {
            "glow" -> startBreathing(1.10f, 600L)
            "sick" -> startBreathing(1.02f, 2800L)
            else -> startBreathing(1.06f, 1200L)
        }
    }

    /** Change breathing only when mood changes to avoid jitter on every render. */
    private fun updateBreathing(mood: PetMood) {
        val mode = when (mood) {
            PetMood.GLOW -> "glow"
            PetMood.SICK -> "sick"
            else -> "normal"
        }
        if (mode == breathMode) return
        breathMode = mode
        when (mode) {
            "glow" -> startBreathing(1.10f, 600L)
            "sick" -> startBreathing(1.02f, 2800L)
            else -> startBreathing(1.06f, 1200L)
        }
    }

    /** Jump animation when switching pets. */
    private fun playJump() {
        ObjectAnimator.ofFloat(activePetView, View.TRANSLATION_Y, 0f, -56f, 0f).apply {
            duration = 420
            interpolator = OvershootInterpolator()
            start()
        }
    }

    // ---------------- Cat behaviors ----------------

    /** Load equipped artwork from assets and decode on IO to avoid blocking the main thread. */
    private fun loadCatImage() {
        // Capture applicationContext before launching; requireContext may throw if the fragment detaches.
        val appContext = context?.applicationContext ?: return
        val path = CatWardrobe.assetPath(appContext)
        // Avoid decoding unchanged artwork on point/mood-triggered renders.
        if (path == lastArtPath) return
        lastArtPath = path
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

    /** Load dog artwork using the shared lastArtPath so cat/dog switches refresh correctly. */
    private fun loadDogImage() {
        val appContext = context?.applicationContext ?: return
        val path = DogWardrobe.assetPath(appContext)
        if (path == lastArtPath) return
        lastArtPath = path
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
        barkSoundId = soundPool?.load(requireContext(), R.raw.bark, 1) ?: 0
    }

    /** Schedule random actions: cat meow/wall-jump/sniff; dog bark/wall-jump. */
    private fun scheduleNextBehavior() {
        if (isSick) {       // Do not schedule lively actions while sick; remain quiet and subdued.
            behaviorRunnable = null
            return
        }
        behaviorRunnable?.let { behaviorHandler.removeCallbacks(it) }
        behaviorRunnable = Runnable {
            if (isResumed && _binding != null) {
                if (isCat) {
                    when ((0..2).random()) {
                        0 -> playMeow()
                        1 -> playEdgeJump()
                        else -> playSniff()
                    }
                } else { // Dog.
                    when ((0..1).random()) {
                        0 -> playWoof()
                        else -> playEdgeJump()
                    }
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

    /** Play a quiet meow and show a meow bubble. */
    private fun playMeow() {
        soundPool?.play(meowSoundId, 0.25f, 0.25f, 1, 0, 1f)
        binding.tvMeowBubble.text = getString(com.example.focuspets.R.string.ui_053)
        binding.tvMeowBubble.visibility = View.VISIBLE
        binding.tvMeowBubble.alpha = 0f
        binding.tvMeowBubble.animate().alpha(1f).setDuration(180).start()
        behaviorHandler.postDelayed({
            if (_binding != null) binding.tvMeowBubble.visibility = View.GONE
        }, 1100)
    }

    /** Play a quiet bark and reuse the speech bubble for barking. */
    private fun playWoof() {
        soundPool?.play(barkSoundId, 0.25f, 0.25f, 1, 0, 1f)
        binding.tvMeowBubble.text = getString(com.example.focuspets.R.string.core_message_34)
        binding.tvMeowBubble.visibility = View.VISIBLE
        binding.tvMeowBubble.alpha = 0f
        binding.tvMeowBubble.animate().alpha(1f).setDuration(180).start()
        behaviorHandler.postDelayed({
            if (_binding != null) binding.tvMeowBubble.visibility = View.GONE
        }, 1100)
    }

    /** Jump to both screen walls, then return to the center. */
    private fun playEdgeJump() {
        val target = activePetView
        val distance = resources.displayMetrics.widthPixels * 0.30f
        val dir = if ((0..1).random() == 0) -1f else 1f
        AnimatorSet().apply {
            playSequentially(
                // Jump to one side.
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(target, View.TRANSLATION_X, 0f, distance * dir),
                        ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, 0f, -120f, 0f)
                    )
                    duration = 520
                },
                // Jump to the opposite side.
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(
                            target, View.TRANSLATION_X, distance * dir, distance * -dir
                        ),
                        ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, 0f, -120f, 0f)
                    )
                    duration = 620
                },
                // Return to center.
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

    /** Sniff toward the screen: enlarge and gently sway, then restore and restart breathing. */
    private fun playSniff() {
        val target = activePetView
        breathingAnimator?.cancel()   // Pause breathing because it also changes scale and would conflict.
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
            restartBreathing()
        }, 1000)
    }

    /** GLOW uses soft halo alpha/scale pulses for a lively glowing appearance. */
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
        // Reset halo scale/alpha when leaving GLOW to avoid stale transforms.
        binding.vGlow.scaleX = 1f
        binding.vGlow.scaleY = 1f
        binding.vGlow.alpha = 1f
    }

    /** Match GLOW halo to pet skin color; yellow keeps the original gold. */
    private fun applyGlowColor(context: Context) {
        val color = if (isCat) CatWardrobe.getEquippedColor(context)
                    else DogWardrobe.getEquippedColor(context)
        val tint = glowTintColor(color)
        val center = Color.argb(0x99, Color.red(tint), Color.green(tint), Color.blue(tint))
        val d = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 150f, resources.displayMetrics
            )
            colors = intArrayOf(center, Color.TRANSPARENT)
            setGradientCenter(0.5f, 0.5f)
        }
        binding.vGlow.background = d
    }

    /** Map skin colors to halo colors; yellow uses gold and others use matching hues. */
    private fun glowTintColor(color: String): Int = when (color) {
        "yellow" -> 0xFFD54F.toInt()      // Gold, preserving the original color.
        "gray"   -> 0x90A4AE.toInt()      // Gray blue.
        "blue"   -> 0x42A5F5.toInt()      // Blue.
        "pink"   -> 0xF48FB1.toInt()      // Pink.
        "white"  -> 0xCFD8DC.toInt()      // Silver white.
        "black"  -> 0xBA68C8.toInt()      // Purple.
        "colorful" -> 0x66BB6A.toInt()    // Green for the multicolor dog.
        else     -> 0xFFD54F.toInt()      // Gold fallback.
    }

    // ---------------- Rendering ----------------

    private fun render(state: HomeUiState) {
        val context = requireContext()

        // Cats (1) and dogs (2) use asset artwork; other pets use emoji.
        val petIsCat = state.selectedPet.id == CatWardrobe.CAT_PET_ID
        val petIsDog = state.selectedPet.id == DogWardrobe.DOG_PET_ID
        val showing = petIsCat || petIsDog
        if (showing != showingArt) {
            showingArt = showing
            restartBreathing()      // The animation target changed; restart the current breathing mode.
        }
        // Refresh cat/dog identity on every render: both use artwork, so showing does not change.
        // Updating isCat only when showing changes could display a dog but open the cat wardrobe.
        isCat = petIsCat
        if (showing) {
            binding.ivPetCat.visibility = View.VISIBLE
            binding.tvPetEmoji.visibility = View.GONE
            binding.btnWardrobe.visibility = if (showing) View.VISIBLE else View.GONE
            binding.btnWardrobe.text = if (petIsCat) getString(com.example.focuspets.R.string.ui_146) else getString(com.example.focuspets.R.string.ui_148)
            if (petIsCat) loadCatImage() else loadDogImage()
        } else {
            binding.ivPetCat.visibility = View.GONE
            binding.tvPetEmoji.visibility = View.VISIBLE
            binding.btnWardrobe.visibility = View.GONE
            binding.tvPetEmoji.text = state.selectedPet.emoji
        }

        // Collection progress.
        binding.tvPetName.text = com.example.focuspets.model.UiStrings.petName(state.selectedPet.id, state.selectedPet.name)
        binding.tvCollectionStatus.text = getString(com.example.focuspets.R.string.core_message_35, state.unlockedCount)

        // Persist the current Home pet for the cloud leaderboard avatar. Sync only when the pet changes,
        // including the first display, so the cloud receives the latest artwork path.
        val curPet = state.selectedPet.id
        SettingsManager.setCurrentPetId(context, curPet)
        if (curPet != lastSyncedPetId) {
            lastSyncedPetId = curPet
            CloudSyncManager.notifyChanged()
        }

        // Point progress targets the lowest-cost locked pet in the catalog.
        if (state.nextLockedPet == null) {
            binding.progressNext.setProgressCompat(100, true)
            binding.tvProgressHint.text = getString(com.example.focuspets.R.string.core_message_36)
        } else {
            binding.progressNext.setProgressCompat(state.progressPercent, true)
            binding.tvProgressHint.text =
                getString(com.example.focuspets.R.string.core_message_37, state.nextLockedPet.emoji, com.example.focuspets.model.UiStrings.petName(state.nextLockedPet.id, state.nextLockedPet.name), state.remainingPoints)
        }

        // Render the three mood states.
        when (state.mood) {
            PetMood.SICK -> {
                binding.tvMoodTag.text = getString(com.example.focuspets.R.string.core_message_38)
                binding.tvMoodTag.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.rarity_common)
                )
                binding.tvPetSick.visibility = View.VISIBLE
                binding.vGlow.visibility = View.GONE
                activePetView.alpha = 0.55f      // Subdued appearance.
                activePetView.rotation = 8f      // Tilt the head slightly to look unwell.
                if (!isSick) {                   // Entering sick state: stop jumps and other lively actions.
                    isSick = true
                    stopBehaviors()
                }
                stopGlowPulse()
                updateBreathing(PetMood.SICK)
            }

            PetMood.GLOW -> {
                binding.tvMoodTag.text = getString(com.example.focuspets.R.string.core_message_39)
                binding.tvMoodTag.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.rarity_legend)
                )
                binding.tvPetSick.visibility = View.GONE
                binding.vGlow.visibility = View.VISIBLE
                activePetView.alpha = 1f
                if (isSick) {                    // Leaving sick state: reset head tilt and resume behaviors.
                    isSick = false
                    activePetView.rotation = 0f
                    scheduleNextBehavior()
                }
                applyGlowColor(context)          // Match halo color to pet color.
                startGlowPulse()
                updateBreathing(PetMood.GLOW)
            }

            PetMood.NORMAL -> {
                binding.tvMoodTag.text = getString(com.example.focuspets.R.string.ui_158)
                binding.tvMoodTag.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.rarity_rare)
                )
                binding.tvPetSick.visibility = View.GONE
                binding.vGlow.visibility = View.GONE
                activePetView.alpha = 1f
                if (isSick) {                    // Leaving sick state: reset head tilt and resume behaviors.
                    isSick = false
                    activePetView.rotation = 0f
                    scheduleNextBehavior()
                }
                stopGlowPulse()
                updateBreathing(PetMood.NORMAL)
            }
        }

        // Accessibility: update TalkBack descriptions for the current pet and mood when switching.
        val moodText = when (state.mood) {
            PetMood.SICK -> getString(com.example.focuspets.R.string.core_message_40)
            PetMood.GLOW -> getString(com.example.focuspets.R.string.core_message_41)
            PetMood.NORMAL -> getString(com.example.focuspets.R.string.core_message_42)
        }
        binding.petTouchArea.contentDescription =
            getString(com.example.focuspets.R.string.core_message_43, com.example.focuspets.model.UiStrings.petName(state.selectedPet.id, state.selectedPet.name), state.selectedPet.emoji, moodText)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        breathingAnimator?.cancel()
        stopGlowPulse()
        stopBehaviors()
        soundPool?.release()
        soundPool = null
        lastArtPath = null   // Decode artwork again after view recreation.
        _binding = null
    }
}
