package com.example.focuspets.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.focuspets.MainActivity
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.entity.FocusRecordEntity
import com.example.focuspets.cloud.CloudSyncManager
import com.example.focuspets.model.PetCareState
import com.example.focuspets.model.SettingsManager
import com.example.focuspets.ui.lock.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineExceptionHandler
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground focus timer using CountDownTimer and an ongoing notification. Broadcast remaining time
 * each second within the app. On completion, persist focus_records for points, broadcast FINISHED, and
 * stop. Backgrounding over five seconds triggers ACTION_FAIL with no points and a hungry pet.
 * Allowlist monitoring checks the foreground app and brings the lock screen back for disallowed apps.
 */
class FocusService : Service() {

    companion object {
        // Start/control actions.
        const val ACTION_START = "com.example.focuspets.action.START"
        const val ACTION_FAIL = "com.example.focuspets.action.FAIL"
        const val ACTION_STOP = "com.example.focuspets.action.STOP"
        const val ACTION_LOCK_MONITOR_STOP = "com.example.focuspets.action.LOCK_MONITOR_STOP"

        // Outgoing broadcasts.
        const val BROADCAST_TICK = "com.example.focuspets.broadcast.TICK"
        const val BROADCAST_FINISHED = "com.example.focuspets.broadcast.FINISHED"
        const val BROADCAST_FAILED = "com.example.focuspets.broadcast.FAILED"
        const val BROADCAST_CANCELLED = "com.example.focuspets.broadcast.CANCELLED"

        // Intent extra keys.
        const val EXTRA_MINUTES = "extra_minutes"
        const val EXTRA_CONTENT = "extra_content"          // Session content, such as coding, saved in the focus record.
        const val EXTRA_REMAINING_MILLIS = "extra_remaining"
        const val EXTRA_TOTAL_MILLIS = "extra_total"
        const val EXTRA_MINUTES_DONE = "extra_minutes_done"
        const val EXTRA_LOCK_MONITOR = "extra_lock_monitor"

        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "focus_timer_channel"

        /** Whether a timer is active, used to gate starts and background checks. */
        @Volatile
        var isRunning: Boolean = false
            private set

        /**
         * True while lock mode owns the session: suppress the five-second background failure. The underlying
         * focus page pauses while pinning or soft monitoring still keeps the session inside the app; this must
         * not count as failure.
         */
        @Volatile
        var lockActive: Boolean = false
    }

    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, t ->
            Log.e("FocusService", "service coroutine failed (non-fatal): ${t.message}", t)
        }
    )
    private var countdown: CountDownTimer? = null
    private var totalMillis = 0L
    /** Session content passed by the UI through EXTRA_CONTENT at startup and saved on completion. */
    private var focusContent: String = ""

    // Allowlist monitoring state: detect foreground apps and bring the lock page back for disallowed apps.
    @Volatile
    var lockMonitorActive: Boolean = false
        private set
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer(
                intent.getIntExtra(EXTRA_MINUTES, 25),
                intent.getStringExtra(EXTRA_CONTENT) ?: "",
                intent.getBooleanExtra(EXTRA_LOCK_MONITOR, false)
            )
            ACTION_FAIL -> failFocus()
            ACTION_STOP -> cancelByUser()
            ACTION_LOCK_MONITOR_STOP -> stopMonitor()
            else -> stopSelf()
        }
        return START_NOT_STICKY   // Do not restart automatically after system termination; users must start a new session.
    }

    // ------------------------- Timer core -------------------------

    private fun startTimer(minutes: Int, content: String, enableMonitor: Boolean = false) {
        if (isRunning) return
        totalMillis = minutes * 60_000L
        focusContent = content.trim()
        isRunning = true

        // Call startForeground within five seconds to avoid an ANR/crash.
        startForegroundCompat(buildNotification(totalMillis))

        // Allowlist mode replaces screen pinning with foreground-app detection so listed apps remain usable.
        if (enableMonitor) startMonitor()

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

    /** Successful countdown: persist points, broadcast the result, then stop the service. */
    private fun completeFocus() {
        val minutes = (totalMillis / 60_000L).toInt()
        serviceScope.launch {
            // 1. Award one point per focus minute and insert the record including content.
            val dao = AppDatabase.getInstance(applicationContext).focusRecordDao()
            dao.insert(
                FocusRecordEntity(
                    focusDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    content = focusContent,
                    durationMinutes = minutes,
                    points = minutes
                )
            )
            // 2. Restore pet health and increment the success streak in the mood state machine.
            PetCareState.recordSuccess(applicationContext)

            // 3. Incrementally sync points, collection, wardrobe, and mood when anonymously logged in.
            CloudSyncManager.notifyChanged()

            isRunning = false
            stopMonitor()
            send(BROADCAST_FINISHED) { putExtra(EXTRA_MINUTES_DONE, minutes) }
            stopSelf()
        }
    }

    /** After over five seconds in the background, fail without awarding points and make the pet hungry. */
    private fun failFocus() {
        if (!isRunning) return
        countdown?.cancel()
        isRunning = false
        stopMonitor()
        // Failure clears the success streak and makes the pet sick.
        PetCareState.recordFailure(applicationContext)
        send(BROADCAST_FAILED)
        stopSelf()
    }

    /** Voluntary cancellation awards no points and does not punish the pet. */
    private fun cancelByUser() {
        if (!isRunning) return
        countdown?.cancel()
        isRunning = false
        stopMonitor()
        send(BROADCAST_CANCELLED)
        stopSelf()
    }

    override fun onDestroy() {
        countdown?.cancel()
        stopMonitor()
        serviceScope.cancel()
        isRunning = false
        super.onDestroy()
    }

    // ------------------------- Lock allowlist soft monitoring -------------------------

    /** Periodically detect the foreground app; return to the lock screen unless it is this app or allowlisted. */
    private fun startMonitor() {
        if (monitorJob != null) return
        lockMonitorActive = true
        monitorJob = serviceScope.launch {
            while (isActive && lockMonitorActive) {
                delay(700)
                try {
                    val fg = currentForegroundPackage()
                    if (fg != null && fg != packageName &&
                        !SettingsManager.getLockWhitelist(this@FocusService).contains(fg)
                    ) {
                        bringLockToFront()
                    }
                } catch (_: Exception) { }
            }
        }
    }

    /** Stop foreground-app monitoring. */
    private fun stopMonitor() {
        lockMonitorActive = false
        monitorJob?.cancel()
        monitorJob = null
    }

    /** Read the foreground package through UsageStatsManager; requires Usage Access permission. */
    private fun currentForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 4000, now)
        if (stats.isNullOrEmpty()) return null
        return stats.filter { it.lastTimeUsed > 0 }
            .maxByOrNull { it.lastTimeUsed }?.packageName
    }

    /** Bring the singleTask lock Activity back to the top of its existing stack. */
    private fun bringLockToFront() {
        try {
            val lockIntent = Intent(this, LockActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            startActivity(lockIntent)
        } catch (_: Exception) { }
    }

    // ------------------------- Notifications -------------------------

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+ requires a declared foreground-service type; use specialUse for this timer.
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
            .setOngoing(true)          // Cannot be dismissed by swiping.
            .setOnlyAlertOnce(true)     // Do not repeat sound/vibration on every second's update.
            .setContentIntent(contentIntent)
            .build()
    }

    /** Reuse NOTIFICATION_ID to update the notification countdown in place. */
    private fun updateNotification(remaining: Long) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(remaining))
    }

    private fun formatMillis(ms: Long): String {
        val totalSeconds = ms / 1000
        return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    /** App-targeted broadcast: setPackage limits delivery to this app, paired with RECEIVER_NOT_EXPORTED. */
    private fun send(action: String, configurator: Intent.() -> Unit = {}) {
        sendBroadcast(Intent(action).setPackage(packageName).apply(configurator))
    }
}
