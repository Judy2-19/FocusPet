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
 * 云端同步管理器（单例）：负责把本地状态增量推到后端、并在登录/网络恢复时拉取合并。
 * 设计原则：Room 仍是本地真源，这里只是"镜像 + 跨设备副本"。
 * 离线时只标记 pending，等网络恢复由 FocusPetsApp 的 NetworkCallback 触发补传。
 */
object CloudSyncManager {

    private lateinit var db: AppDatabase
    private lateinit var api: CloudApi
    private lateinit var auth: AuthManager
    private lateinit var appContext: Context
    // 同步在后台跑，必须兜底异常：真机无后端时网络/DB 错误都只记日志，绝不杀进程
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

    /** 本地数据变化后调用：联网立即上报；离线标记待同步 */
    fun notifyChanged() {
        scope.launch { syncNow() }
    }

    /** 登录成功 / 网络恢复时调用：先拉云端合并，再推本地 */
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
            // 忽略，下次重试
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
        // 联网即确保匿名账号已建立：若 App 启动时后端不可用导致 uid 一直为空，
        // 这里兜底补建，避免「uid 没建好 → 永远推不上去 → 排行榜永远未上榜」。
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
        // 当前展示宠物：首页 selectedPet 的 id（持久化在 SettingsManager）
        val currentPetId = SettingsManager.getCurrentPetId(appContext)
        // 立绘路径：猫/狗带当前装扮，其余宠物无立绘留空（走 emoji 兜底）
        val petAvatar = when (currentPetId) {
            CatWardrobe.CAT_PET_ID -> CatWardrobe.assetPath(appContext)
            DogWardrobe.DOG_PET_ID -> DogWardrobe.assetPath(appContext)
            else -> ""
        }
        return CloudUserPayload(name, total, collection, wardrobe, mood, currentPetId, petAvatar)
    }

    /** 云端 → 本地合并（last-write-wins，简单覆盖；演示足够） */
    private suspend fun mergeIntoDb(remote: CloudUser) {
        // 收藏：云端有、本地缺 → 补插入（unlockPets 用 IGNORE，幂等）
        val localOwned = db.userCollectionDao().getUnlockedPetIds().toSet()
        val toInsert = remote.collection.filter { it !in localOwned }
        if (toInsert.isNotEmpty()) {
            db.userCollectionDao().unlockPets(toInsert.map { UserCollectionEntity(petId = it) })
        }
        // 妆扮：云端有、本地缺 → 补插入（cost 占位 0，仅用于演示跨设备恢复）
        val localWardrobe = db.wardrobeDao().getOwnedItemIdsOnce().toSet()
        val toInsertW = remote.wardrobe.filter { it !in localWardrobe }
        for (id in toInsertW) {
            db.wardrobeDao().insert(WardrobePurchaseEntity(itemId = id, itemType = "synced", cost = 0))
        }
        // 心情：取云端
        val mood = runCatching { PetMood.valueOf(remote.mood) }.getOrDefault(PetMood.NORMAL)
        PetCareState.setMood(appContext, mood)
        // 昵称缓存
        auth.setDisplayName(remote.displayName)
    }

    private suspend fun markPending(pending: Boolean) {
        db.syncDao().setMeta(
            com.example.focuspets.db.entity.SyncMetaEntity("pending", if (pending) "1" else "0")
        )
    }

    // ---------------- 排行榜离线缓存 ----------------
    // 后端断开时，排行榜页回退显示上一次成功拉取的数据，而不是一片"加载失败"。
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
