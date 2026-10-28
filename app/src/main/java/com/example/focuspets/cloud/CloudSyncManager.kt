package com.example.focuspets.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.focuspets.cloud.model.CloudUser
import com.example.focuspets.cloud.model.CloudUserPayload
import com.example.focuspets.cloud.model.LeaderboardResponse
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.entity.UserCollectionEntity
import com.example.focuspets.db.entity.WardrobePurchaseEntity
import com.example.focuspets.model.CatWardrobe
import com.example.focuspets.model.DogWardrobe
import com.example.focuspets.model.PetCareState
import com.example.focuspets.model.PetMood
import com.example.focuspets.model.SettingsManager
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Singleton cloud synchronization: push incremental local state and fetch/merge after login or
 * reconnection. Room remains the local source of truth; the cloud is a mirror and cross-device
 * replica. Offline changes set pending, and FocusPetsApp's NetworkCallback retries later.
 */
object CloudSyncManager {

    private lateinit var db: AppDatabase
    private lateinit var api: CloudApi
    private lateinit var auth: AuthManager
    private lateinit var appContext: Context
    // Guard background synchronization: log network/database errors without killing the app when no backend is available.
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, t ->
            Log.w("CloudSyncManager", "sync failed (non-fatal, backend likely offline): ${t.message}")
        }
    )

    fun init(context: Context, database: AppDatabase, cloudApi: CloudApi, authManager: AuthManager) {
        appContext = context.applicationContext
        db = database
        api = cloudApi
        auth = authManager
    }

    /** Call after local changes: push immediately online or mark pending offline. */
    fun notifyChanged() {
        scope.launch { syncNow() }
    }

    /** Call after login or reconnection: fetch/merge cloud state, then push local state. */
    fun retryPending() {
        scope.launch { pullIfOnline(); syncNow() }
    }

    suspend fun pullIfOnline() {
        val uid = auth.uid() ?: return
        if (!isOnline()) return
        try {
            val remote = api.getUser(uid)
            mergeIntoDb(remote)
        } catch (_: Exception) {
            // Ignore and retry next time.
        }
    }

    fun currentUid(): String? = if (::auth.isInitialized) auth.uid() else null
    fun currentName(): String = if (::auth.isInitialized) auth.displayName() else "专注者"
    fun rename(name: String) {
        if (::auth.isInitialized) auth.setDisplayName(name)
        notifyChanged()
    }

    private suspend fun syncNow() {
        if (!isOnline()) { markPending(true); return }
        // Ensure an anonymous account exists online, even if startup could not reach the backend and left the UID empty.
        // Retry registration here so a missing UID cannot permanently prevent pushes or leaderboard participation.
        val uid = auth.ensureLogin()
        try {
            api.updateUser(uid, buildPayload())
            markPending(false)
        } catch (_: Exception) {
            markPending(true)
        }
    }

    private fun isOnline(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun buildPayload(): CloudUserPayload {
        val total = db.focusRecordDao().getTotalPoints()
        val collection = db.userCollectionDao().getUnlockedPetIds()
        val wardrobe = db.wardrobeDao().getOwnedItemIdsOnce()
        val mood = PetCareState.getMood(appContext).name
        val name = auth.displayName()
        // Currently displayed pet: Home selectedPet ID, persisted in SettingsManager.
        val currentPetId = SettingsManager.getCurrentPetId(appContext)
        // Artwork path includes the cat/dog outfit; other pets leave it empty for the emoji fallback.
        val petAvatar = when (currentPetId) {
            CatWardrobe.CAT_PET_ID -> CatWardrobe.assetPath(appContext)
            DogWardrobe.DOG_PET_ID -> DogWardrobe.assetPath(appContext)
            else -> ""
        }
        return CloudUserPayload(name, total, collection, wardrobe, mood, currentPetId, petAvatar)
    }

    /** Cloud-to-local merge: simple last-write-wins replacement, sufficient for the demo. */
    private suspend fun mergeIntoDb(remote: CloudUser) {
        // Insert cloud collection entries missing locally; unlockPets uses IGNORE for idempotence.
        val localOwned = db.userCollectionDao().getUnlockedPetIds().toSet()
        val toInsert = remote.collection.filter { it !in localOwned }
        if (toInsert.isNotEmpty()) {
            db.userCollectionDao().unlockPets(toInsert.map { UserCollectionEntity(petId = it) })
        }
        // Insert missing cloud wardrobe entries with placeholder cost 0 for the cross-device recovery demo.
        val localWardrobe = db.wardrobeDao().getOwnedItemIdsOnce().toSet()
        val toInsertW = remote.wardrobe.filter { it !in localWardrobe }
        for (id in toInsertW) {
            db.wardrobeDao().insert(WardrobePurchaseEntity(itemId = id, itemType = "synced", cost = 0))
        }
        // Use cloud mood state.
        val mood = runCatching { PetMood.valueOf(remote.mood) }.getOrDefault(PetMood.NORMAL)
        PetCareState.setMood(appContext, mood)
        // Cache nickname.
        auth.setDisplayName(remote.displayName)
    }

    private suspend fun markPending(pending: Boolean) {
        db.syncDao().setMeta(
            com.example.focuspets.db.entity.SyncMetaEntity("pending", if (pending) "1" else "0")
        )
    }

    // ---------------- Offline leaderboard cache ----------------
    // When the backend is unavailable, show the last successful leaderboard instead of only a loading error.
    private val lbPrefs by lazy {
        appContext.getSharedPreferences("lb_cache", Context.MODE_PRIVATE)
    }
    private val gson = Gson()

    fun cacheLeaderboard(resp: LeaderboardResponse) {
        try {
            lbPrefs.edit()
                .putString("last", gson.toJson(resp))
                .putLong("ts", System.currentTimeMillis())
                .apply()
        } catch (_: Exception) {
        }
    }

    fun loadCachedLeaderboard(): LeaderboardResponse? = try {
        lbPrefs.getString("last", null)
            ?.let { gson.fromJson(it, LeaderboardResponse::class.java) }
    } catch (_: Exception) {
        null
    }
}
