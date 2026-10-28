package com.example.focuspets.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Check and guide PACKAGE_USAGE_STATS Usage Access permission. It is special permission, not
 * requestPermissions; users must enable it manually in system Settings.
 */
object UsageStatsPermission {

    /** Whether Usage Access permission is granted. */
    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                "android:get_usage_stats", android.os.Process.myUid(), context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Open the system Usage Access settings page. */
    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
