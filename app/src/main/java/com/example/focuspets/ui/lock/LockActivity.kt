package com.example.focuspets.ui.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.focuspets.R
import com.example.focuspets.databinding.ActivityLockBinding
import com.example.focuspets.model.Backgrounds
import com.example.focuspets.model.CatWardrobe
import com.example.focuspets.model.DogWardrobe
import com.example.focuspets.model.SettingsManager
import com.example.focuspets.service.FocusService
import com.example.focuspets.util.UsageStatsPermission
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen pinned focus mode inspired by Tomato ToDo. Reuses FocusService and broadcasts for
 * timer/progress. lockActive suppresses the underlying page's background-failure check. Cancellation
 * requires confirmation; completion waits for Unlock and Leave. Allowlisted apps remain available
 * through soft monitoring.
 */
class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private var selectedMinutes = 25
    private var totalMillisAtStart = 0L
    private var pinned = false
    /** Whether an authorized exit is underway: daily forced exit, completion, or failure. Do not repin while true. */
    private var exiting = false

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private fun todayStr(): String = dateFmt.format(Date())

    /** Whether today's once-daily forced exit remains available; resets the next day. */
    private fun forceExitAvailable(): Boolean =
        SettingsManager.getLockForceExitDate(this) != todayStr()

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                FocusService.BROADCAST_TICK -> {
                    val remaining =
                        intent.getLongExtra(FocusService.EXTRA_REMAINING_MILLIS, 0)
                    totalMillisAtStart =
                        intent.getLongExtra(FocusService.EXTRA_TOTAL_MILLIS, totalMillisAtStart)
                    renderRunning(remaining)
                }
                FocusService.BROADCAST_FINISHED ->
                    onFinished(intent.getIntExtra(FocusService.EXTRA_MINUTES_DONE, 0))
                FocusService.BROADCAST_FAILED -> onFailed()
                FocusService.BROADCAST_CANCELLED -> renderIdle()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Reset locking flags because singleTask reuses this Activity across sessions.
        exiting = false
        pinned = false

        Backgrounds.apply(this, binding.lockRoot)

        // Take ownership of the session and suppress the underlying focus page's background-failure check.
        FocusService.lockActive = true

        setupDuration()
        binding.btnStart.setOnClickListener { tryStart() }
        binding.btnWhitelist.setOnClickListener {
            startActivity(Intent(this, LockWhitelistActivity::class.java))
        }
        binding.btnCustom.setOnClickListener { applyCustom() }
        binding.etCustomMinutes.setOnEditorActionListener { _, _, _ ->
            applyCustom(); true
        }
        binding.btnUnlock.setOnClickListener { unlockAndFinish() }
        // Keep the once-daily forced exit available during locking.
        binding.btnForceExit.setOnClickListener { confirmForceExit() }
        // Idle state can exit freely before locking actually starts.
        binding.btnExit.setOnClickListener { finish() }

        loadCurrentPetImage()
        onBackPressedDispatcher.addCallback(this, backCallback)
        if (FocusService.isRunning) renderRunning(null) else renderIdle()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(FocusService.BROADCAST_TICK)
            addAction(FocusService.BROADCAST_FINISHED)
            addAction(FocusService.BROADCAST_FAILED)
            addAction(FocusService.BROADCAST_CANCELLED)
        }
        ContextCompat.registerReceiver(
            this, timerReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(timerReceiver)
        // While locking, if the screen is still on, the user may be using Home or an unpin gesture;
        // bring the lock screen back. Do not relaunch while the screen is off to avoid repeated wakeups.
        if (!exiting && FocusService.isRunning) {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm == null || pm.isInteractive) {
                pinned = false // Leaving the foreground means pinning may be gone; repin on the next onResume.
                try {
                    val i = Intent(this, LockActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(i)
                } catch (_: Exception) { }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // During locking, if the user has unpinned through a system gesture,
        // call startLockTask again. With pinning disabled, rely on the onStop fallback.
        if (!exiting && FocusService.isRunning && SettingsManager.isScreenPinEnabled(this) && !pinned) {
            try { startLockTask(); pinned = true } catch (_: Exception) { }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FocusService.lockActive = false
        try { if (pinned) stopLockTask() } catch (_: Exception) { }
    }

    // Back handling:
    // Idle before locking: exit immediately.
    // Active lock session: ignore Back until completion and Unlock and Leave.
    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!FocusService.isRunning) {
                finish()
                return
            }
            // Prevent exiting during locked focus; soft monitoring also returns from disallowed apps.
        }
    }

    // ---------------- Immersive mode / locking ----------------

    private fun enterImmersive() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /** Deliberately release the lock for daily forced exit, successful completion, or failure. */
    private fun releaseLock() {
        exiting = true
        try { stopLockTask() } catch (_: Exception) { }
        pinned = false
    }

    // ---------------- Duration ----------------

    private fun setupDuration() {
        binding.toggleDuration.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && !FocusService.isRunning) {
                selectedMinutes = when (checkedId) {
                    R.id.btn_15 -> 15
                    R.id.btn_45 -> 45
                    else -> 25
                }
                binding.tvRemaining.text = formatMillis(selectedMinutes * 60_000L)
                binding.etCustomMinutes.setText(selectedMinutes.toString())
            }
        }
    }

    private fun applyCustom() {
        if (FocusService.isRunning) return
        val m = binding.etCustomMinutes.text.toString().toIntOrNull()
        if (m == null || m < 1 || m > 180) {
            Toast.makeText(this, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_059), Toast.LENGTH_SHORT).show()
            return
        }
        selectedMinutes = m
        binding.tvRemaining.text = formatMillis(selectedMinutes * 60_000L)
        binding.toggleDuration.clearChecked()
    }

    // ---------------- Controls ----------------

    private fun tryStart() {
        if (FocusService.isRunning) return
        // Confirm before starting: Start Lock actually locks; dismissing does not start the countdown.
        MaterialAlertDialogBuilder(this)
            .setTitle(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_144))
            .setMessage(
                com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_181) +
                if (SettingsManager.getLockWhitelist(this).isNotEmpty())
                    com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_029)
                else
                    com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_113)
            )
            .setPositiveButton(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_142)) { _, _ -> beginLock() }
            .setNegativeButton(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_119), null)
            .show()
    }

    /**
     * After confirmation, choose by allowlist: a nonempty list enables FocusService soft monitoring
     * without startLockTask, allowing listed apps and returning from others. Usage Access must be granted
     * first. An empty list uses hard screen pinning. Both modes enter the lock screen and start the
     * countdown immediately.
     */
    private fun beginLock() {
        if (FocusService.isRunning) return
        exiting = false
        pinned = false
        val whitelist = SettingsManager.getLockWhitelist(this)
        val useMonitor = whitelist.isNotEmpty()

        if (useMonitor) {
            // Soft monitoring requires Usage Access; guide the user to grant it, then let them start again.
            if (!UsageStatsPermission.hasPermission(this)) {
                promptUsagePermission()
                return
            }
            pinned = false
        } else {
            // Hard lock uses system screen pinning to keep the device on this page without normal exit.
            try {
                if (SettingsManager.isScreenPinEnabled(this)) {
                    startLockTask()
                    pinned = true
                } else {
                    pinned = false
                }
            } catch (_: Exception) {
                pinned = false
            }
        }

        val intent = Intent(this, FocusService::class.java).apply {
            action = FocusService.ACTION_START
            putExtra(FocusService.EXTRA_MINUTES, selectedMinutes)
            // Locked sessions use the default lock-mode content label in session records.
            putExtra(FocusService.EXTRA_CONTENT, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_096))
            // Whether allowlist soft monitoring is enabled.
            putExtra(FocusService.EXTRA_LOCK_MONITOR, useMonitor)
        }
        ContextCompat.startForegroundService(this, intent)
        totalMillisAtStart = selectedMinutes * 60_000L
        renderRunning(null)
        // Hide system bars only when locking starts so accidental entry can still exit with one Back press.
        enterImmersive()
    }

    /** Guide the user to grant special Usage Access permission manually in Settings. */
    private fun promptUsagePermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_174))
            .setMessage(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_030))
            .setPositiveButton(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_083)) { _, _ -> UsageStatsPermission.openSettings(this) }
            .setNegativeButton(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_042), null)
            .show()
    }

    // ---------------- Once-daily forced exit ----------------

    private fun confirmForceExit() {
        if (!forceExitAvailable()) return
        MaterialAlertDialogBuilder(this)
            .setTitle(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_077))
            .setMessage(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_150))
            .setPositiveButton(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_078)) { _, _ -> doForceExit() }
            .setNegativeButton(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_090), null)
            .show()
    }

    private fun doForceExit() {
        // Record today's forced exit; it becomes available again tomorrow.
        SettingsManager.setLockForceExitDate(this, todayStr())
        // Stop the underlying focus timer; the service also stops monitoring.
        startService(
            Intent(this, FocusService::class.java).setAction(FocusService.ACTION_STOP)
        )
        releaseLock()
        finish()
    }

    private fun onFinished(minutes: Int) {
        binding.tvDoneSub.text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_021 ,(minutes).toString())
        binding.overlayDone.visibility = View.VISIBLE
        binding.btnForceExit.visibility = View.GONE
        com.example.focuspets.util.AlertPlayer.play(this)
        // Unpin on completion, then wait for the user to select Unlock and Leave.
        releaseLock()
    }

    private fun onFailed() {
        MaterialAlertDialogBuilder(this)
            .setTitle(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_191))
            .setMessage(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_149))
            .setPositiveButton(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_121)) { _, _ ->
                releaseLock()
                finish()
            }
            .show()
    }

    private fun unlockAndFinish() {
        releaseLock()
        finish()
    }

    // ---------------- Rendering ----------------

    private fun renderRunning(remaining: Long?) {
        binding.overlayDone.visibility = View.GONE
        // During locking, hide duration settings and other entry points; no ordinary exit button by default.
        binding.toggleDuration.visibility = View.GONE
        binding.layoutCustom.visibility = View.GONE
        binding.btnStart.visibility = View.GONE
        binding.btnWhitelist.visibility = View.GONE
        binding.btnExit.visibility = View.GONE
        // Show forced exit only if today's allowance remains; hide after use and restore tomorrow.
        if (forceExitAvailable()) {
            binding.btnForceExit.visibility = View.VISIBLE
            binding.btnForceExit.text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_076)
        } else {
            binding.btnForceExit.visibility = View.GONE
        }
        binding.tvHint.visibility = View.GONE
        binding.tvSubtitle.text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_097)
        if (remaining != null) {
            binding.tvRemaining.text = formatMillis(remaining)
            val percent = if (totalMillisAtStart > 0)
                (((totalMillisAtStart - remaining) * 100) / totalMillisAtStart).toInt()
            else 0
            binding.progressFocus.setProgress(percent.coerceIn(0, 100))
        } else {
            binding.tvRemaining.text = formatMillis(totalMillisAtStart)
        }
    }

    private fun renderIdle() {
        binding.overlayDone.visibility = View.GONE
        // Idle allows exit, duration configuration, and allowlist setup before locking.
        binding.toggleDuration.visibility = View.VISIBLE
        binding.layoutCustom.visibility = View.VISIBLE
        binding.btnStart.visibility = View.VISIBLE
        binding.btnStart.text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_143)
        binding.btnStart.isEnabled = true
        binding.btnWhitelist.visibility = View.VISIBLE
        binding.btnExit.visibility = View.VISIBLE
        binding.btnForceExit.visibility = View.GONE
        binding.tvHint.visibility = View.VISIBLE
        binding.tvSubtitle.text = com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_047)
        binding.tvRemaining.text = formatMillis(selectedMinutes * 60_000L)
        binding.progressFocus.setProgress(0)
    }

    /** Show the currently selected Home cat/dog and outfit instead of always showing a cat. */
    private fun loadCurrentPetImage() {
        val ctx = applicationContext
        val petId = SettingsManager.getCurrentPetId(ctx)
        val path = if (petId == DogWardrobe.DOG_PET_ID) DogWardrobe.assetPath(ctx)
                   else CatWardrobe.assetPath(ctx)
        binding.ivCat.contentDescription =
            if (petId == DogWardrobe.DOG_PET_ID) com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_074) else com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_073)
        lifecycleScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                try {
                    ctx.assets.open(path).use { BitmapFactory.decodeStream(it) }
                } catch (_: Exception) {
                    null
                }
            }
            if (!isDestroyed && bmp != null) binding.ivCat.setImageBitmap(bmp)
        }
    }

    private fun formatMillis(ms: Long): String {
        val s = ms / 1000
        return "%02d:%02d".format(s / 60, s % 60)
    }
}
