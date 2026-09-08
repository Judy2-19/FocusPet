package com.example.focuspets.user

import android.content.Context
import androidx.room.withTransaction
import com.example.focuspets.cloud.ApiClient
import com.example.focuspets.cloud.AuthManager
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.model.CatWardrobe
import com.example.focuspets.model.DogWardrobe
import com.example.focuspets.db.InitialDataProvider
import com.example.focuspets.model.PetCareState
import com.example.focuspets.model.PetMood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineExceptionHandler
import android.util.Log

/**
 * 用户数据管理：负责「重置进度」与「注销」两类敏感操作。
 *
 * 为什么重置必须同时注销云端账号：
 * 当前架构 Room 是本地真源、云端只是镜像。如果只清本地而不清云端 uid，
 * 下次启动 ensureLogin 用同一 uid 拉取云端会把旧数据合并回本地，重置形同失效。
 * 因此 resetProgress 在清空本地表之后，会调用 AuthManager.logout() 让下次以新匿名身份同步。
 *
 * 所有写库操作都在 IO 协程执行，UI 层用 onDone 回调收尾（弹 Toast / 关闭页面）。
 */
object UserDataManager {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, t ->
            Log.e("UserDataManager", "user op failed (non-fatal): ${t.message}", t)
        }
    )

    /**
     * 重置本地进度到初始状态：
     * 清空专注记录 / 妆扮 / 图鉴（保留首只赠送宠物）/ 同步队列与状态，
     * 宠物心情回到 NORMAL，并注销云端账号（见类注释）。
     * 不动：背景色选择、引导已看标记等纯 UI 偏好。
     */
    fun resetProgress(context: Context, onDone: () -> Unit = {}) {
        scope.launch {
            runCatching {
                val db = AppDatabase.getInstance(context.applicationContext)
                db.withTransaction {
                    db.focusRecordDao().deleteAll()
                    db.wardrobeDao().deleteAll()
                    db.userCollectionDao().deleteExcept(InitialDataProvider.FREE_PET_ID)
                    db.syncDao().clearQueue()
                    db.syncDao().clearMeta()
                }
                PetCareState.setMood(context.applicationContext, PetMood.NORMAL)
                // 清空「穿在身上」的装扮状态（SharedPreferences）：购买记录清了，
                // 但穿戴状态若不清，会出现"已重置却还穿着已不存在的装扮"的脏状态。
                DogWardrobe.clearEquipped(context.applicationContext)
                CatWardrobe.clearEquipped(context.applicationContext)
                // 注销云端：断开与旧匿名身份的绑定，避免云端把旧数据拉回
                AuthManager(context.applicationContext, ApiClient.api).logout()
            }
            // 切回主线程回调，调用方可直接弹 Toast / finish 页面
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    /**
     * 仅注销（退出当前匿名账号）：保留全部本地进度，
     * 下次启动重新匿名注册一个新 uid，本地数据会以新身份继续同步。
     */
    fun logout(context: Context, onDone: () -> Unit = {}) {
        scope.launch {
            runCatching {
                AuthManager(context.applicationContext, ApiClient.api).logout()
            }
            // 切回主线程回调，调用方可直接弹 Toast / finish 页面
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}
