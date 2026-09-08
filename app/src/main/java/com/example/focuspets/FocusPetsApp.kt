package com.example.focuspets

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import java.io.File
import com.example.focuspets.cloud.ApiClient
import com.example.focuspets.cloud.AuthManager
import com.example.focuspets.cloud.CloudSyncManager
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.badge.BadgeCelebration
import com.example.focuspets.model.SettingsManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用入口：初始化云端模块（Retrofit / 匿名登录 / 同步管理器），
 * 并在网络恢复时触发补传。
 *
 * 重要：云端是「演示增强」功能，后端（server/leaderboard_server.py）不一定在运行。
 * 所有初始化与网络调用都必须「非致命」——即使后端不可用 / 网络失败，
 * 也绝不能让整个 App 崩溃。因此统一用 try/catch + CoroutineExceptionHandler 兜底。
 * 否则在真机（非模拟器）上访问 10.0.2.2 这类模拟器回环地址会直接抛异常杀进程。
 */
class FocusPetsApp : Application() {

    companion object {
        /** 读取并清除上次崩溃堆栈（由 MainActivity 在启动时弹窗展示） */
        fun consumeLastCrash(context: Context): String? {
            val f = File(context.filesDir, "last_crash.txt")
            if (!f.exists()) return null
            val text = runCatching { f.readText() }.getOrNull()
            runCatching { f.delete() }
            return text
        }
    }

    private val cloudExceptionHandler = CoroutineExceptionHandler { _, t ->
        Log.w("FocusPetsApp", "cloud coroutine failed (non-fatal, backend likely offline): ${t.message}")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + cloudExceptionHandler)

    override fun onCreate() {
        super.onCreate()

        // 应用用户保存的主题模式（浅色 / 深色 / 跟随系统），在首屏绘制前设置以免闪屏
        AppCompatDelegate.setDefaultNightMode(
            SettingsManager.themeModeToNightMode(SettingsManager.getThemeMode(this))
        )

        // 全局兜底：任何未被协程/作用域捕获的异常，都把完整堆栈打到 Logcat
        // （tag FocusPetsCrash），并弹 Toast，便于真机复现时定位——而不是只给系统级崩溃提示。
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val trace = Log.getStackTraceString(throwable)
            Log.e("FocusPetsCrash", "UNCAUGHT on ${thread.name}", throwable)
            // 把完整堆栈落盘，下次启动由 MainActivity 弹窗展示，方便无 Logcat 时定位
            try {
                File(filesDir, "last_crash.txt").writeText("thread=${thread.name}\n$trace")
            } catch (_: Throwable) {
            }
            // 尽量直接拉起崩溃展示页（后台线程崩溃时可靠；主线程崩溃可能来不及，但下次启动仍可读文件）
            try {
                val intent = Intent(this, CrashActivity::class.java).apply {
                    putExtra("trace", "thread=${thread.name}\n$trace")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            } catch (_: Throwable) {
            }
        }

        // 整体兜底：任何初始化异常都不要扩散成进程崩溃
        try {
            val db = AppDatabase.getInstance(this)
            val auth = AuthManager(this, ApiClient.api)
            CloudSyncManager.init(this, db, ApiClient.api, auth)

            // 启动即匿名登录 + 拉取云端状态合并到本地（后端不可达时静默失败）
            scope.launch {
                try {
                    auth.ensureLogin()
                    // 启动即拉取云端并回推本地进度（账号已在上面 ensureLogin 建立；
                    // 后端不可达时 retryPending 内部会安全失败，不影响启动）
                    CloudSyncManager.retryPending()
                    // 把当前已解锁徽章标记为已庆祝，避免老用户升级后一次性弹出一堆历史成就
                    BadgeCelebration.migrateExisting(this@FocusPetsApp)
                } catch (e: Exception) {
                    Log.w("FocusPetsApp", "login/pull failed (offline?): ${e.message}")
                }
            }

            // 网络恢复时补传待同步数据
            val cm = getSystemService(ConnectivityManager::class.java)
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        try {
                            CloudSyncManager.retryPending()
                        } catch (e: Exception) {
                            Log.w("FocusPetsApp", "retryPending failed: ${e.message}")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            Log.e("FocusPetsApp", "cloud init error (non-fatal): ${e.message}", e)
        }
    }
}
