package com.example.focuspets.debug

import android.content.Context
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.InitialDataProvider
import com.example.focuspets.db.entity.FocusRecordEntity
import com.example.focuspets.db.entity.UserCollectionEntity
import com.example.focuspets.model.PetCareState
import com.example.focuspets.model.PetMood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 测试分支（test）专用调试工具。
 *
 * 用途：开发阶段直接把图鉴调到「满配」+ 大额积分，方便浏览全部 9 只宠物、
 * 验证稀有度渲染、心情三态、进度条与妆扮商城，而不必真去专注攒积分。
 *
 * 积分原理：可用积分 = 总专注分钟 − 已解锁宠物 cost − 妆扮消费。
 * 所以「发积分」就是往 focus_records 插一条超大 duration_minutes 的记录，
 * 并用特殊 focus_date 打标记，方便一键清掉。
 *
 * ⚠️ 本文件及所有 debug 调用仅存在于 test 分支，不进 main。
 */
object DebugHelper {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private const val PREFS = "debug_prefs"
    // 换过 key：旧版已把 full_unlock_done 置为 true，用新 key 才能让「补 100 万积分」重新执行一次
    private const val KEY_SETUP_DONE = "test_setup_v2"

    /** 测试目标积分：100 万 */
    const val TEST_POINTS = 1_000_000

    /** 测试注入记录的日期标记，用于精准清除 */
    private const val TEST_GRANT_MARKER = "test-grant"

    /**
     * 测试版首次启动：一次性解锁全部宠物并把积分补到 100 万。
     * SharedPreferences 守卫保证只做一次，不会覆盖你后续手动「重置进度」。
     */
    fun ensureTestSetup(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_SETUP_DONE, false)) {
            scope.launch {
                unlockAllSync(context)
                topUpPointsSync(context)
            }
            prefs.edit().putBoolean(KEY_SETUP_DONE, true).apply()
        }
    }

    /** 解锁全部 9 只宠物（幂等） */
    fun unlockAll(context: Context) {
        scope.launch { unlockAllSync(context) }
    }

    /** 重置进度：仅保留首只赠送宠物，并清掉测试注入的积分 */
    fun resetToInitial(context: Context) {
        scope.launch { resetToInitialSync(context) }
    }

    /** 把可用积分补到 100 万（只补差额，可重复点） */
    fun grantPoints(context: Context) {
        scope.launch { topUpPointsSync(context) }
    }

    /**
     * 循环切换宠物心情三态：NORMAL → SICK → GLOW → NORMAL。
     * 返回切换后的状态，便于调试按钮回显。
     */
    fun cycleMood(context: Context): PetMood {
        val next = when (PetCareState.getMood(context)) {
            PetMood.NORMAL -> PetMood.SICK
            PetMood.SICK -> PetMood.GLOW
            PetMood.GLOW -> PetMood.NORMAL
        }
        PetCareState.debugForceMood(context, next)
        return next
    }

    // ---------------- 内部实现 ----------------

    private suspend fun unlockAllSync(context: Context) {
        val db = AppDatabase.getInstance(context.applicationContext)
        db.userCollectionDao().unlockPets(
            InitialDataProvider.PRESET_PETS.map { UserCollectionEntity(petId = it.id) }
        )
    }

    private suspend fun resetToInitialSync(context: Context) {
        val db = AppDatabase.getInstance(context.applicationContext)
        db.userCollectionDao().deleteExcept(InitialDataProvider.FREE_PET_ID)
        db.focusRecordDao().deleteTestGrants(TEST_GRANT_MARKER)
    }

    /**
     * 补积分到 TEST_POINTS：先读当前可用积分，只补差额。
     * 这样无论是否已解锁花钱，最终可用积分都正好是 100 万。
     */
    private suspend fun topUpPointsSync(context: Context) {
        val db = AppDatabase.getInstance(context.applicationContext)
        val current = db.userCollectionDao().getAvailablePointsOnce()
        if (current < TEST_POINTS) {
            db.focusRecordDao().insert(
                FocusRecordEntity(
                    focusDate = TEST_GRANT_MARKER,
                    durationMinutes = TEST_POINTS - current
                )
            )
        }
    }
}
