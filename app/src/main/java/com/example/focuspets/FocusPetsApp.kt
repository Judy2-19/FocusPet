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
 * Application entry point: initialize Retrofit, anonymous authentication, and synchronization; retry
 * when networking returns. Cloud features are optional demo enhancements and the backend may be
 * unavailable. Guard initialization and network calls with try/catch and CoroutineExceptionHandler so
 * failures, including emulator-only addresses on hardware, do not kill the app.
 */
class FocusPetsApp : Application() {

    companion object {
        /** Read and clear the previous crash trace for display during MainActivity startup. */
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

        // Apply the saved light/dark/system theme before drawing the first screen to prevent flashing.
        AppCompatDelegate.setDefaultNightMode(
            SettingsManager.themeModeToNightMode(SettingsManager.getThemeMode(this))
        )

        // Global fallback: write uncaught coroutine and main-thread exceptions to Logcat.
        // Use the FocusPetsCrash tag and a Toast to make hardware startup failures diagnosable.
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val trace = Log.getStackTraceString(throwable)
            Log.e("FocusPetsCrash", "UNCAUGHT on ${thread.name}", throwable)
            // Persist the stack trace for display on the next launch without Logcat.
            try {
                File(filesDir, "last_crash.txt").writeText("thread=${thread.name}\n$trace")
            } catch (_: Throwable) {
            }
            // Try opening the crash screen directly; if a main-thread crash prevents this, the next launch can still read the trace.
            try {
                val intent = Intent(this, CrashActivity::class.java).apply {
                    putExtra("trace", "thread=${thread.name}\n$trace")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            } catch (_: Throwable) {
            }
        }

        // Catch initialization failures so they do not become process crashes.
        try {
            val db = AppDatabase.getInstance(this)
            val auth = AuthManager(this, ApiClient.api)
            CloudSyncManager.init(this, db, ApiClient.api, auth)

            // Register anonymously, fetch cloud state, and merge locally; fail silently when the backend is unreachable.
            scope.launch {
                try {
                    auth.ensureLogin()
                    // Fetch and push progress on startup; retryPending invokes ensureLogin if necessary.
                    // retryPending handles unreachable backends safely without disrupting startup.
                    CloudSyncManager.retryPending()
                    // Mark existing badges as celebrated so upgrades do not display every historical achievement at once.
                    BadgeCelebration.migrateExisting(this@FocusPetsApp)
                } catch (e: Exception) {
                    Log.w("FocusPetsApp", "login/pull failed (offline?): ${e.message}")
                }
            }

            // Retry synchronization when network connectivity returns.
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
