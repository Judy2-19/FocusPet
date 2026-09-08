package com.example.focuspets.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.room.withTransaction
import com.example.focuspets.db.dao.ContentMinutes
import com.example.focuspets.db.dao.DayMinutes
import com.example.focuspets.db.dao.PetWithState
import com.example.focuspets.db.dao.RecordRaw
import com.example.focuspets.db.entity.FocusRecordEntity
import com.example.focuspets.db.entity.PetEntity
import com.example.focuspets.db.entity.UserCollectionEntity
import com.example.focuspets.cloud.CloudSyncManager
import com.example.focuspets.db.entity.WardrobePurchaseEntity
import com.example.focuspets.model.AchievementBadge
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PetRepository(private val db: AppDatabase) {

    fun getPetsWithState(): LiveData<List<PetWithState>> = db.petDao().getPetsWithState()

    fun getAvailablePoints(): LiveData<Int> = db.userCollectionDao().getAvailablePoints()

    /**
     * 解锁宠物：事务内做三重校验，防止并发/重复点击导致超扣。
     * 「扣积分」不需要 UPDATE 任何表——插入收藏记录后，
     * 可用积分 = 总专注分钟 - 已解锁 unlock_cost 之和，自然减少。
     */
    suspend fun unlockPet(pet: PetEntity): Boolean = db.withTransaction {
        val unlockedIds = db.userCollectionDao().getUnlockedPetIds()
        if (pet.id in unlockedIds) return@withTransaction false          // 已解锁
        if (db.userCollectionDao().getAvailablePointsOnce() < pet.unlockCost) {
            return@withTransaction false                                 // 积分不够
        }
        db.userCollectionDao().unlockPet(UserCollectionEntity(petId = pet.id))
        CloudSyncManager.notifyChanged()   // 解锁后增量同步到云端
        true
    }

    /** 专注完成结算后：查询「积分已达标但尚未解锁」的宠物，用于庆祝弹窗 */
    suspend fun getAffordableLockedPets(): List<PetEntity> {
        val points = db.userCollectionDao().getAvailablePointsOnce()
        return db.petDao().getAffordableLockedPets(points)
    }

    /** 已购买的猫咪妆扮 id 列表（LiveData） */
    fun getWardrobeOwnedIds(): LiveData<List<String>> = db.wardrobeDao().getOwnedItemIds()

    /**
     * 购买猫咪妆扮：事务内校验积分并写入购买记录。
     * 可用积分已包含 wardrobe_purchases 总消费，所以直接比较即可。
     */
    suspend fun buyWardrobeItem(itemId: String, itemType: String, cost: Int): Boolean =
        db.withTransaction {
            if (db.userCollectionDao().getAvailablePointsOnce() < cost) return@withTransaction false
            val ok = db.wardrobeDao()
                .insert(WardrobePurchaseEntity(itemId, itemType, cost)) > 0
            if (ok) CloudSyncManager.notifyChanged()   // 购买妆扮后增量同步到云端
            ok
        }

    // ---------------- 专注统计 ----------------

    /** 今日专注分钟数 */
    fun getTodayMinutes(): LiveData<Int> = liveData(Dispatchers.IO) {
        emit(db.focusRecordDao().getMinutesOnDate(todayStr()))
    }

    /** 累计专注分钟数（真实专注时长，不含测试注入的纯积分） */
    fun getTotalMinutes(): LiveData<Int> = liveData(Dispatchers.IO) {
        emit(db.focusRecordDao().getTotalDuration())
    }

    /** 专注总次数 */
    fun getSessionCount(): LiveData<Int> = liveData(Dispatchers.IO) {
        emit(db.focusRecordDao().getSessionCount())
    }

    // ---------------- 专注记录页（内容维度） ----------------

    /** 时间区间内的全部专注记录（含内容），记录页列表用 */
    suspend fun getRecordsBetween(startMs: Long, endMs: Long): List<FocusRecordEntity> =
        db.focusRecordDao().getRecordsBetween(startMs, endMs)

    /** 时间区间内各专注内容的分钟数聚合，饼图用 */
    suspend fun getContentMinutesBetween(startMs: Long, endMs: Long): List<ContentMinutes> =
        db.focusRecordDao().getContentMinutesBetween(startMs, endMs)

    /** 高频专注内容（复用建议） */
    suspend fun getFrequentContents(limit: Int): List<String> =
        db.focusRecordDao().getFrequentContents(limit)

    /** 最近一次使用的专注内容（下次默认填充） */
    suspend fun getLatestContent(): String? = db.focusRecordDao().getLatestContent()

    /** 删除指定专注内容的全部记录（用户手动清理「最近用过」历史） */
    suspend fun deleteRecordsByContent(content: String): Int =
        db.focusRecordDao().deleteByContent(content)

    /** 连续打卡天数（今天没专注也不算断签，从昨天往前数） */
    fun getStreak(): LiveData<Int> = liveData(Dispatchers.IO) {
        emit(computeStreak())
    }

    /** 近 7 天专注分钟数（含今天），用于柱状图 */
    fun getLast7Days(): LiveData<List<DayBar>> = liveData(Dispatchers.IO) {
        emit(buildLast7())
    }

    /** 专注时段热力图数据：7(周一..周日) × 24(时) 的分钟数矩阵，按完成时刻分桶 */
    fun getHeatmap(): LiveData<Array<IntArray>> = liveData(Dispatchers.IO) {
        val records = db.focusRecordDao().getAllRecordsRaw()
        val matrix = Array(7) { IntArray(24) }
        val cal = Calendar.getInstance()
        for (r in records) {
            cal.timeInMillis = r.createdAt
            val dow = (cal.get(Calendar.DAY_OF_WEEK) + 6) % 7 // 0=周一 .. 6=周日
            val hour = cal.get(Calendar.HOUR_OF_DAY)          // 0..23
            matrix[dow][hour] += r.minutes
        }
        emit(matrix)
    }

    /** 连续打卡成就徽章列表（LiveData，供统计页实时展示） */
    fun getAchievements(): LiveData<List<AchievementBadge>> = liveData(Dispatchers.IO) {
        emit(computeAchievements())
    }

    /** 同步快照版本：供「成就达成弹海报」检测使用，不走 LiveData */
    suspend fun getAchievementsSnapshot(): List<AchievementBadge> = computeAchievements()

    /** 依据当前连续天数 / 总专注次数计算成就徽章列表 */
    private suspend fun computeAchievements(): List<AchievementBadge> {
        val streak = computeStreak()
        val sessions = db.focusRecordDao().getSessionCount()
        val defs = listOf(
            BadgeDef("first", "\uD83C\uDF31", "初次打卡", "完成第一次专注", 1, sessions),
            BadgeDef("s3", "\uD83D\uDD25", "三日之约", "连续专注 3 天", 3, streak),
            BadgeDef("s7", "\u2B50", "一周坚持", "连续专注 7 天", 7, streak),
            BadgeDef("s14", "\uD83D\uDCE0", "双周达人", "连续专注 14 天", 14, streak),
            BadgeDef("s30", "\uD83D\uDC51", "月度王者", "连续专注 30 天", 30, streak),
            BadgeDef("s100", "\uD83C\uDFC6", "百日传奇", "连续专注 100 天", 100, streak),
        )
        return defs.map { d ->
            AchievementBadge(
                id = d.id, emoji = d.emoji, title = d.title, desc = d.desc,
                target = d.target,
                progress = d.progress.coerceAtMost(d.target),
                unlocked = d.progress >= d.target
            )
        }
    }

    // ---- 统计计算辅助（日期用 Calendar 兼容 minSdk 24） ----

    private fun todayStr(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

    private suspend fun computeStreak(): Int {
        val dates = db.focusRecordDao().getDailyMinutes().map { it.date }.toSet()
        if (dates.isEmpty()) return 0
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        // 今天尚未专注不算断签：从今天或昨天开始往前数
        if (sdf.format(cal.time) !in dates) cal.add(Calendar.DAY_OF_MONTH, -1)
        var streak = 0
        while (sdf.format(cal.time) in dates) {
            streak++
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return streak
    }

    private suspend fun buildLast7(): List<DayBar> {
        val map = db.focusRecordDao().getDailyMinutes().associate { it.date to it.minutes }
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val weekLabels = arrayOf("日", "一", "二", "三", "四", "五", "六")
        cal.add(Calendar.DAY_OF_MONTH, -6)
        return (0..6).map {
            val key = sdf.format(cal.time)
            val label = "周" + weekLabels[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val bar = DayBar(label, map[key] ?: 0)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            bar
        }
    }

    /** 指定月份（yyyy-MM 格式）内有专注记录的日期 → 当天分钟数的映射，供日历红点标记 */
    suspend fun getDailyMinutesInMonth(
        year: Int,
        month: Int   // Calendar.JANUARY=0 .. DECEMBER=11
    ): Map<String, Int> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            zeroTime()
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val start = sdf.format(cal.time)
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        val end = sdf.format(cal.time)
        return db.focusRecordDao()
            .getDailyMinutesBetweenDates(start, end)
            .filter { it.minutes > 0 }
            .associate { it.date to it.minutes }
    }

    private fun Calendar.zeroTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

/** 近 7 天柱状图的一项：星期标签 + 当天专注分钟数 */
data class DayBar(val label: String, val minutes: Int)

/** 成就徽章定义（仓库内部使用）：target 为解锁阈值，progress 为当前进度 */
private data class BadgeDef(
    val id: String,
    val emoji: String,
    val title: String,
    val desc: String,
    val target: Int,
    val progress: Int
)
