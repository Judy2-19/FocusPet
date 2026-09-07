package com.example.focuspets.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.focuspets.MainActivity
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.entity.FocusRecordEntity
import com.example.focuspets.model.PetCareState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 专注计时前台服务：
 * - 持有 CountDownTimer，倒计时期间常驻通知栏（避免进程被杀导致计时中断）
 * - 每秒广播剩余时间给 UI 刷新（包内定向广播，不对外暴露）
 * - 成功完成 → 写入 focus_records（积分结算）→ 广播 FINISHED → 停止服务
 * - 切后台超 5 秒 → UI 层发 ACTION_FAIL → 宠物进入饥饿状态，不结算积分
 */
class FocusService : Service() {

    companion object {
        // 启动指令
        const val ACTION_START = "com.example.focuspets.action.START"
        const val ACTION_FAIL = "com.example.focuspets.action.FAIL"
        const val ACTION_STOP = "com.example.focuspets.action.STOP"

        // 对外广播
        const val BROADCAST_TICK = "com.example.focuspets.broadcast.TICK"
        const val BROADCAST_FINISHED = "com.example.focuspets.broadcast.FINISHED"
        const val BROADCAST_FAILED = "com.example.focuspets.broadcast.FAILED"
        const val BROADCAST_CANCELLED = "com.example.focuspets.broadcast.CANCELLED"

        // Intent extra 键
        const val EXTRA_MINUTES = "extra_minutes"
        const val EXTRA_REMAINING_MILLIS = "extra_remaining"
        const val EXTRA_TOTAL_MILLIS = "extra_total"
        const val EXTRA_MINUTES_DONE = "extra_minutes_done"

        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "focus_timer_channel"

        /** 计时是否进行中（UI 判断能否开始新计时 / 切后台判定用） */
        @Volatile
        var isRunning: Boolean = false
            private set

        /**
         * 锁机模式接管会话时为 true：屏蔽"切后台 5 秒失败"判定。
         * 锁机时底层专注页会被暂停，但用户其实仍在 App 内（被屏幕固定锁住），
         * 不应因此判失败。
         */
        @Volatile
        var lockActive: Boolean = false
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var countdown: CountDownTimer? = null
    private var totalMillis = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer(intent.getIntExtra(EXTRA_MINUTES, 25))
            ACTION_FAIL -> failFocus()
            ACTION_STOP -> cancelByUser()
            else -> stopSelf()
        }
        return START_NOT_STICKY   // 被系统回收后不自动重启（专注连续性应由用户重新发起）
    }

    // ------------------------- 计时核心 -------------------------

    private fun startTimer(minutes: Int) {
        if (isRunning) return
        totalMillis = minutes * 60_000L
        isRunning = true

        // 前台服务：必须在 5 秒内调用 startForeground，否则 ANR + 崩溃
        startForegroundCompat(buildNotification(totalMillis))

        countdown = object : CountDownTimer(totalMillis, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                updateNotification(millisUntilFinished)
                send(BROADCAST_TICK) {
                    putExtra(EXTRA_REMAINING_MILLIS, millisUntilFinished)
                    putExtra(EXTRA_TOTAL_MILLIS, totalMillis)
                }
            }

            override fun onFinish() = completeFocus()
        }.start()
    }

    /** 倒计时成功完成：结算积分（写库）→ 广播结果 → 停服 */
    private fun completeFocus() {
        val minutes = (totalMillis / 60_000L).toInt()
        serviceScope.launch {
            // 1. 积分结算：每专注 1 分钟得 1 分 → 写入一条专注记录
            val dao = AppDatabase.getInstance(applicationContext).focusRecordDao()
            dao.insert(
                FocusRecordEntity(
                    focusDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    durationMinutes = minutes
                )
            )
            // 2. 专注成功：宠物恢复健康 + 连续成功计数 +1（阶段四心情状态机）
            PetCareState.recordSuccess(applicationContext)

            isRunning = false
            send(BROADCAST_FINISHED) { putExtra(EXTRA_MINUTES_DONE, minutes) }
            stopSelf()
        }
    }

    /** 切后台超 5 秒：专注失败，不结算积分，宠物进入饥饿状态 */
    private fun failFocus() {
        if (!isRunning) return
        countdown?.cancel()
        isRunning = false
        // 专注失败：连续成功计数清零，宠物进入生病状态
        PetCareState.recordFailure(applicationContext)
        send(BROADCAST_FAILED)
        stopSelf()
    }

    /** 用户主动放弃：不结算积分，也不惩罚宠物 */
    private fun cancelByUser() {
        if (!isRunning) return
        countdown?.cancel()
        isRunning = false
        send(BROADCAST_CANCELLED)
        stopSelf()
    }

    override fun onDestroy() {
        countdown?.cancel()
        serviceScope.cancel()
        isRunning = false
        super.onDestroy()
    }

    // ------------------------- 通知 -------------------------

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+ 强制要求声明前台服务类型（计时类用 specialUse）
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "专注计时", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "番茄钟倒计时进行中"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(remaining: Long): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("专注中…不要切走哦 🐣")
            .setContentText("剩余 ${formatMillis(remaining)}")
            .setOngoing(true)          // 不可滑动清除
            .setOnlyAlertOnce(true)     // 每秒刷新不重复响铃/震动
            .setContentIntent(contentIntent)
            .build()
    }

    /** 复用同一个 NOTIFICATION_ID 通知，实现"通知栏倒计时"效果 */
    private fun updateNotification(remaining: Long) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(remaining))
    }

    private fun formatMillis(ms: Long): String {
        val totalSeconds = ms / 1000
        return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    /** 包内定向广播（setPackage 保证只在应用内传播，配合 RECEIVER_NOT_EXPORTED） */
    private fun send(action: String, configurator: Intent.() -> Unit = {}) {
        sendBroadcast(Intent(action).setPackage(packageName).apply(configurator))
    }
}
