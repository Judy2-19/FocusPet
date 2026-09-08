package com.example.focuspets.cloud

import android.content.Context
import android.content.SharedPreferences
import com.example.focuspets.cloud.model.AnonResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 匿名登录管理：首次调用向后端 /api/auth/anon 注册并拿到 uid，
 * 之后把 uid 缓存在 SharedPreferences（匿名即绑定本机）。
 */
class AuthManager(private val context: Context, private val api: CloudApi) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focuspets_cloud", Context.MODE_PRIVATE)

    /** 确保已登录，返回当前 uid；未登录则先匿名注册 */
    suspend fun ensureLogin(): String = withContext(Dispatchers.IO) {
        val existing = prefs.getString(KEY_UID, null)
        if (existing != null) return@withContext existing
        val resp: AnonResponse = api.anonLogin()
        prefs.edit()
            .putString(KEY_UID, resp.uid)
            .putString(KEY_NAME, resp.displayName)
            .apply()
        resp.uid
    }

    fun uid(): String? = prefs.getString(KEY_UID, null)
    fun displayName(): String = prefs.getString(KEY_NAME, "专注者") ?: "专注者"

    fun setDisplayName(name: String) {
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    /**
     * 注销（退出当前匿名账号）：清除本地缓存的 uid / 昵称。
     * 下次启动 ensureLogin() 会向后端重新匿名注册一个新 uid。
     * 注意：匿名模型下「注销」= 换新匿名身份，本地进度数据保留、仅不再与旧身份同步。
     */
    fun logout() {
        prefs.edit().remove(KEY_UID).remove(KEY_NAME).apply()
    }

    companion object {
        private const val KEY_UID = "uid"
        private const val KEY_NAME = "display_name"
    }
}
