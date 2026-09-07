package com.example.focuspets.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.room.withTransaction
import com.example.focuspets.db.dao.DayMinutes
import com.example.focuspets.db.dao.PetWithState
import com.example.focuspets.db.entity.PetEntity
import com.example.focuspets.db.entity.UserCollectionEntity
import com.example.focuspets.db.entity.WardrobePurchaseEntity
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
            db.wardrobeDao()
                .insert(WardrobePurchaseEntity(itemId, itemType, cost)) > 0
        }

    // ---------------- 专注统计 ----------------

    /** 今日专注分钟数 */
    fun getTodayMinutes(): LiveData<Int> = liveData(Dispatchers.IO) {
        emit(db.focusRecordDao().getMinutesOnDate(todayStr()))
    }

    /** 累计专注分钟数（= 累计获得积分） */
    fun getTotalMinutes(): LiveData<Int> = liveData(Dispatchers.IO) {
        emit(db.focusRecordDao().getTotalPoints())
    }

    /** 专注总次数 */
    fun getSessionCount(): LiveData<Int> = liveData(Dispatchers.IO) {
        emit(db.focusRecordDao().getSessionCount())
    }

    /** 连续打卡天数（今天没专注也不算断签，从昨天往前数） */
    fun getStreak(): LiveData<Int> = liveData(Dispatchers.IO) {
        emit(computeStreak())
    }

    /** 近 7 天专注分钟数（含今天），用于柱状图 */
    fun getLast7Days(): LiveData<List<DayBar>> = liveData(Dispatchers.IO) {
        emit(buildLast7())
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
}

/** 近 7 天柱状图的一项：星期标签 + 当天专注分钟数 */
data class DayBar(val label: String, val minutes: Int)
