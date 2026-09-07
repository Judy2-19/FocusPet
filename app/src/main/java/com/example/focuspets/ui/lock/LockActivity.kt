package com.example.focuspets.ui.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
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
import com.example.focuspets.service.FocusService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 锁机专注（仿番茄 ToDo 学霸模式）：
 * - 全屏沉浸 + 屏幕固定（startLockTask），把手机锁在当前页面，期间无法切走
 * - 复用 FocusService 计时，接收其广播刷新计时器 / 进度
 * - 锁机期间置 FocusService.lockActive=true，让底层专注页的「切后台 5 秒失败」逻辑失效
 * - 放弃需二次确认；完成弹出遮罩，点「解锁离开」才解除固定并退出
 */
class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private var selectedMinutes = 25
    private var totalMillisAtStart = 0L
    private var pinned = false

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

        Backgrounds.apply(this, binding.lockRoot)
        enterImmersive()

        // 接管会话：屏蔽底层专注页的「切后台失败」判定
        FocusService.lockActive = true

        setupDuration()
        binding.btnStart.setOnClickListener { tryStart() }
        binding.btnCustom.setOnClickListener { applyCustom() }
        binding.etCustomMinutes.setOnEditorActionListener { _, _, _ ->
            applyCustom(); true
        }
        binding.btnUnlock.setOnClickListener { unlockAndFinish() }

        loadCatImage()
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
    }

    override fun onDestroy() {
        super.onDestroy()
        FocusService.lockActive = false
        try { if (pinned) stopLockTask() } catch (_: Exception) { }
    }

    override fun onBackPressed() {
        // 锁机中：返回键不直接退出，必须主动放弃，避免误触解屏
        if (FocusService.isRunning) confirmCancel() else super.onBackPressed()
    }

    // ---------------- 沉浸 / 锁屏 ----------------

    private fun enterImmersive() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // ---------------- 时长 ----------------

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
            Toast.makeText(this, "请输入 1~180 之间的分钟数", Toast.LENGTH_SHORT).show()
            return
        }
        selectedMinutes = m
        binding.tvRemaining.text = formatMillis(selectedMinutes * 60_000L)
        binding.toggleDuration.clearChecked()
    }

    // ---------------- 控制 ----------------

    private fun tryStart() {
        if (FocusService.isRunning) { confirmCancel(); return }
        val intent = Intent(this, FocusService::class.java).apply {
            action = FocusService.ACTION_START
            putExtra(FocusService.EXTRA_MINUTES, selectedMinutes)
        }
        ContextCompat.startForegroundService(this, intent)
        totalMillisAtStart = selectedMinutes * 60_000L
        renderRunning(null)
        // 屏幕固定：番茄 ToDo 学霸模式同款，把手机锁在当前页面
        try {
            startLockTask()
            pinned = true
        } catch (_: Exception) { /* 非设备所有者需手动确认，忽略即可 */ }
    }

    private fun confirmCancel() {
        MaterialAlertDialogBuilder(this)
            .setTitle("放弃本次专注？")
            .setMessage("已专注的时间不会计入积分")
            .setPositiveButton("放弃") { _, _ ->
                startService(
                    Intent(this, FocusService::class.java)
                        .setAction(FocusService.ACTION_STOP)
                )
                try { if (pinned) stopLockTask() } catch (_: Exception) { }
                pinned = false
                finish()
            }
            .setNegativeButton("继续专注", null)
            .show()
    }

    private fun onFinished(minutes: Int) {
        binding.tvDoneSub.text = "+$minutes 积分"
        binding.overlayDone.visibility = View.VISIBLE
        // 完成即解除固定，等用户点「解锁离开」退出
        try { if (pinned) stopLockTask() } catch (_: Exception) { }
        pinned = false
    }

    private fun onFailed() {
        MaterialAlertDialogBuilder(this)
            .setTitle("😿 专注失败")
            .setMessage("本次专注没有完成。")
            .setPositiveButton("知道了") { _, _ ->
                try { if (pinned) stopLockTask() } catch (_: Exception) { }
                pinned = false
                finish()
            }
            .show()
    }

    private fun unlockAndFinish() {
        try { if (pinned) stopLockTask() } catch (_: Exception) { }
        pinned = false
        finish()
    }

    // ---------------- 渲染 ----------------

    private fun renderRunning(remaining: Long?) {
        binding.btnStart.text = "放弃专注"
        binding.toggleDuration.isEnabled = false
        binding.etCustomMinutes.isEnabled = false
        binding.btnCustom.isEnabled = false
        binding.tvSubtitle.text = "锁机专注中…保持手机在本页面"
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
        binding.btnStart.text = "开始锁机专注"
        binding.toggleDuration.isEnabled = true
        binding.etCustomMinutes.isEnabled = true
        binding.btnCustom.isEnabled = true
        binding.tvSubtitle.text = "选择时长，开始锁机专注"
        binding.tvRemaining.text = formatMillis(selectedMinutes * 60_000L)
        binding.progressFocus.setProgress(0)
    }

    private fun loadCatImage() {
        val ctx = applicationContext
        val path = CatWardrobe.assetPath(ctx)
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
