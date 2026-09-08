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
 * 锁机专注（仿番茄 ToDo 学霸模式）：
 * - 全屏沉浸 + 锁机（两种模式见 beginLock 注释）
 * - 复用 FocusService 计时，接收其广播刷新计时器 / 进度
 * - 锁机期间置 FocusService.lockActive=true，让底层专注页的「切后台 5 秒失败」逻辑失效
 * - 放弃需二次确认；完成弹出遮罩，点「解锁离开」才解除并退出
 * - 白名单应用：锁机界面可进入「白名单应用」页勾选，锁机期间允许这些 app 正常使用（软监控模式）
 */
class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private var selectedMinutes = 25
    private var totalMillisAtStart = 0L
    private var pinned = false
    /** 是否正在「合法退出」流程中（每日强制退出 / 完成解锁 / 失败退出）。true 时不再重新固定 */
    private var exiting = false

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private fun todayStr(): String = dateFmt.format(Date())

    /** 今日是否还有「强制退出」机会（每天一次，次日自动恢复） */
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

        // 重置锁定状态（该 Activity 以 singleTask 复用，避免上一会话的标记残留）
        exiting = false
        pinned = false

        Backgrounds.apply(this, binding.lockRoot)

        // 接管会话：屏蔽底层专注页的「切后台失败」判定
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
        // 每日一次强制退出（锁机进行中可用，不删）
        binding.btnForceExit.setOnClickListener { confirmForceExit() }
        // 空闲态：随时可退出（此时尚未真正锁机）
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
        // 锁机进行中：若屏幕仍亮着（说明是用户在划 Home / 做解除固定的手势），
        // 就把锁机页重新拉回前台；屏幕熄灭时不拉起，避免息屏后反复唤醒。
        if (!exiting && FocusService.isRunning) {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm == null || pm.isInteractive) {
                pinned = false // 离开前台即视为已解除固定，下次 onResume 会重新固定
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
        // 锁机进行中：若尚未被系统屏幕固定（例如用户刚才用系统手势解除了固定），
        // 重新调用 startLockTask 把手机再次锁死；屏幕固定开关关闭时则只靠 onStop 拉回兜底。
        if (!exiting && FocusService.isRunning && SettingsManager.isScreenPinEnabled(this) && !pinned) {
            try { startLockTask(); pinned = true } catch (_: Exception) { }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FocusService.lockActive = false
        try { if (pinned) stopLockTask() } catch (_: Exception) { }
    }

    // 返回键处理：
    // - 空闲态（尚未真正锁机）：直接退出
    // - 真锁机中（专注进行中）：不可退出，忽略返回键，直到专注完成点「解锁离开」
    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!FocusService.isRunning) {
                finish()
                return
            }
            // 锁机专注进行中：强制不可退出（白名单软监控仍会把切走的应用拉回）
        }
    }

    // ---------------- 沉浸 / 锁屏 ----------------

    private fun enterImmersive() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /** 主动、合法地解除锁定（每日强制退出 / 完成解锁 / 失败退出时调用） */
    private fun releaseLock() {
        exiting = true
        try { stopLockTask() } catch (_: Exception) { }
        pinned = false
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
        if (FocusService.isRunning) return
        // 二次确认：点「开始锁机」才真正锁定；点「不用了」什么都不做（不启动倒计时）
        MaterialAlertDialogBuilder(this)
            .setTitle("开始锁机专注？")
            .setMessage(
                "开始后手机将被锁定在本页面，无法主动退出；专注结束自动解锁。\n" +
                if (SettingsManager.getLockWhitelist(this).isNotEmpty())
                    "已设置白名单：锁机期间可正常使用白名单内的应用，其余应用会被自动拉回。"
                else
                    "未设置白名单：将使用系统级屏幕固定，把手机完全锁死在当前页面。"
            )
            .setPositiveButton("开始锁机") { _, _ -> beginLock() }
            .setNegativeButton("不用了", null)
            .show()
    }

    /**
     * 用户确认开始，按白名单情况选择锁机模式：
     * - 白名单非空 → 软监控模式：不调用 startLockTask（不再硬锁死手机），而是在 FocusService
     *   中开启「前台应用监控」，放行白名单内的 app，打开白名单外的 app 会被自动拉回锁机页。
     *   软监控需要「使用情况访问」权限，未授权则引导授权、不启动。
     * - 白名单为空 → 硬锁模式：仍走系统屏幕固定（学霸模式），把手机锁在当前页面。
     * 两种模式都会立即进入锁机界面并启动倒计时。
     */
    private fun beginLock() {
        if (FocusService.isRunning) return
        exiting = false
        pinned = false
        val whitelist = SettingsManager.getLockWhitelist(this)
        val useMonitor = whitelist.isNotEmpty()

        if (useMonitor) {
            // 软监控必须有权限，否则无法判断前台应用 → 引导授权后由用户重新点开始
            if (!UsageStatsPermission.hasPermission(this)) {
                promptUsagePermission()
                return
            }
            pinned = false
        } else {
            // 硬锁模式：番茄 ToDo 学霸模式同款，把手机锁在当前页面（真锁机，无常规退出）。
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
            // 锁机事件内容默认为「锁机」，专注记录里归属到该内容
            putExtra(FocusService.EXTRA_CONTENT, "锁机")
            // 是否开启白名单软监控
            putExtra(FocusService.EXTRA_LOCK_MONITOR, useMonitor)
        }
        ContextCompat.startForegroundService(this, intent)
        totalMillisAtStart = selectedMinutes * 60_000L
        renderRunning(null)
        // 进入锁机瞬间才隐藏系统栏（沉浸），避免误入时无法一次返回退出
        enterImmersive()
    }

    /** 引导用户授予「使用情况访问」权限（特殊权限，需到系统设置手动开启） */
    private fun promptUsagePermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle("需要「使用情况访问」权限")
            .setMessage("锁机白名单需要读取前台应用来判断是否放行。请在接下来的设置页中，找到「专注养宠」并开启「使用情况访问」权限，然后返回重新点击「开始锁机专注」。")
            .setPositiveButton("去授权") { _, _ -> UsageStatsPermission.openSettings(this) }
            .setNegativeButton("取消", null)
            .show()
    }

    // ---------------- 每日一次强制退出 ----------------

    private fun confirmForceExit() {
        if (!forceExitAvailable()) return
        MaterialAlertDialogBuilder(this)
            .setTitle("强制退出锁机？")
            .setMessage("这是今天的最后一次强制退出机会，确定要解锁离开吗？")
            .setPositiveButton("强制退出") { _, _ -> doForceExit() }
            .setNegativeButton("继续专注", null)
            .show()
    }

    private fun doForceExit() {
        // 记录今天已用掉强制退出机会（次日自动恢复）
        SettingsManager.setLockForceExitDate(this, todayStr())
        // 停止底层专注计时（服务内部会顺带停止监控）
        startService(
            Intent(this, FocusService::class.java).setAction(FocusService.ACTION_STOP)
        )
        releaseLock()
        finish()
    }

    private fun onFinished(minutes: Int) {
        binding.tvDoneSub.text = "+$minutes 积分"
        binding.overlayDone.visibility = View.VISIBLE
        binding.btnForceExit.visibility = View.GONE
        com.example.focuspets.util.AlertPlayer.play(this)
        // 完成即解除固定，等用户点「解锁离开」退出
        releaseLock()
    }

    private fun onFailed() {
        MaterialAlertDialogBuilder(this)
            .setTitle("😿 专注失败")
            .setMessage("本次专注没有完成。")
            .setPositiveButton("知道了") { _, _ ->
                releaseLock()
                finish()
            }
            .show()
    }

    private fun unlockAndFinish() {
        releaseLock()
        finish()
    }

    // ---------------- 渲染 ----------------

    private fun renderRunning(remaining: Long?) {
        binding.overlayDone.visibility = View.GONE
        // 真锁机：隐藏时长选择等其它入口，且默认无常规退出键
        binding.toggleDuration.visibility = View.GONE
        binding.layoutCustom.visibility = View.GONE
        binding.btnStart.visibility = View.GONE
        binding.btnWhitelist.visibility = View.GONE
        binding.btnExit.visibility = View.GONE
        // 每日一次强制退出：当日还有机会就显示按钮（用完则隐藏，次日自动恢复）
        if (forceExitAvailable()) {
            binding.btnForceExit.visibility = View.VISIBLE
            binding.btnForceExit.text = "强制退出（今日剩余 1 次）"
        } else {
            binding.btnForceExit.visibility = View.GONE
        }
        binding.tvHint.visibility = View.GONE
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
        // 空闲态：可退出、可配置时长、可设置白名单，尚未真正锁机
        binding.toggleDuration.visibility = View.VISIBLE
        binding.layoutCustom.visibility = View.VISIBLE
        binding.btnStart.visibility = View.VISIBLE
        binding.btnStart.text = "开始锁机专注"
        binding.btnStart.isEnabled = true
        binding.btnWhitelist.visibility = View.VISIBLE
        binding.btnExit.visibility = View.VISIBLE
        binding.btnForceExit.visibility = View.GONE
        binding.tvHint.visibility = View.VISIBLE
        binding.tvSubtitle.text = "选择时长，开始锁机专注"
        binding.tvRemaining.text = formatMillis(selectedMinutes * 60_000L)
        binding.progressFocus.setProgress(0)
    }

    /** 锁屏宠物 = 首页当前宠物（猫/狗），并显示其当前装扮，而不是永远显示猫 */
    private fun loadCurrentPetImage() {
        val ctx = applicationContext
        val petId = SettingsManager.getCurrentPetId(ctx)
        val path = if (petId == DogWardrobe.DOG_PET_ID) DogWardrobe.assetPath(ctx)
                   else CatWardrobe.assetPath(ctx)
        binding.ivCat.contentDescription =
            if (petId == DogWardrobe.DOG_PET_ID) "专注中的小狗" else "专注中的小猫"
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
