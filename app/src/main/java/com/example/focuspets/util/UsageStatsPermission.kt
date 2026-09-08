package com.example.focuspets.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * 「使用情况访问」权限（PACKAGE_USAGE_STATS）检查与引导。
 * 该权限属于特殊权限，无法用 requestPermissions 申请，必须引导用户到系统设置页手动开启。
 */
object UsageStatsPermission {

    /** 是否已授予「使用情况访问」权限 */
    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** 打开系统「使用情况访问」设置页 */
    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
