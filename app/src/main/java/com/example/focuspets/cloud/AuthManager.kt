package com.example.focuspets.cloud

import android.content.Context
import android.content.SharedPreferences
import com.example.focuspets.cloud.model.AnonResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Anonymous authentication: register at /api/auth/anon on first use, then cache the UID in
 * SharedPreferences on this device.
 */
class AuthManager(private val context: Context, private val api: CloudApi) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focuspets_cloud", Context.MODE_PRIVATE)

    /** Return the current UID, registering anonymously first if needed. */
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
     * Sign out by clearing the cached UID and nickname. The next ensureLogin registers a new anonymous
     * identity. Local progress remains, but no longer syncs with the old identity.
     */
    fun logout() {
        prefs.edit().remove(KEY_UID).remove(KEY_NAME).apply()
    }

    companion object {
        private const val KEY_UID = "uid"
        private const val KEY_NAME = "display_name"
    }
}
