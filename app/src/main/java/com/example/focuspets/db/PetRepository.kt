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
     * Unlock within a transaction with checks against concurrent/repeated clicks and overspending. No
     * UPDATE is needed to deduct points: inserting the collection entry reduces the derived balance (total
     * focus minutes minus unlocked pet costs).
     */
    suspend fun unlockPet(pet: PetEntity): Boolean = db.withTransaction {
        val unlockedIds = db.userCollectionDao().getUnlockedPetIds()
        if (pet.id in unlockedIds) return@withTransaction false          // Already unlocked.
        if (db.userCollectionDao().getAvailablePointsOnce() < pet.unlockCost) {
            return@withTransaction false                                 // Insufficient points.
        }
        db.userCollectionDao().unlockPet(UserCollectionEntity(petId = pet.id))
        CloudSyncManager.notifyChanged()   // Push incremental cloud state after unlocking.
        true
    }

    /** After session settlement, find affordable locked pets for the celebration dialog. */
    suspend fun getAffordableLockedPets(): List<PetEntity> {
        val points = db.userCollectionDao().getAvailablePointsOnce()
        return db.petDao().getAffordableLockedPets(points)
    }

    /** Purchased cat outfit IDs as LiveData. */
    fun getWardrobeOwnedIds(): LiveData<List<String>> = db.wardrobeDao().getOwnedItemIds()

    /**
     * Purchase a cat outfit by checking points and inserting within a transaction. The derived balance
     * already subtracts wardrobe_purchases spending.
     */
    suspend fun buyWardrobeItem(itemId: String, itemType: String, cost: Int): Boolean =
        db.withTransaction {
            if (db.userCollectionDao().getAvailablePointsOnce() < cost) return@withTransaction false
            val ok = db.wardrobeDao()
                .insert(WardrobePurchaseEntity(itemId, itemType, cost)) > 0
            if (ok) CloudSyncManager.notifyChanged()   // Push incremental cloud state after wardrobe purchases.
            ok
        }

    // ---------------- Focus statistics ----------------

    /** Today's focus minutes. */
    fun getTodayMinutes(): LiveData<Int> = liveData(Dispatchers.IO) {
        emit(db.focusRecordDao().getMinutesOnDate(todayStr()))
    }

    /** Total actual focus minutes, excluding point-only test grants. */
    fun getTotalMinutes(): LiveData<Int> = liveData(Dispatchers.IO) {
        emit(db.focusRecordDao().getTotalDuration())
    }

    /** Total completed session count. */
    fun getSessionCount(): LiveData<Int> = liveData(Dispatchers.IO) {
        emit(db.focusRecordDao().getSessionCount())
    }

    // ---------------- Focus records by content ----------------

    /** Records including content in a time interval, for the records list. */
    suspend fun getRecordsBetween(startMs: Long, endMs: Long): List<FocusRecordEntity> =
        db.focusRecordDao().getRecordsBetween(startMs, endMs)

    /** Aggregate minutes per content in an interval for the pie chart. */
    suspend fun getContentMinutesBetween(startMs: Long, endMs: Long): List<ContentMinutes> =
        db.focusRecordDao().getContentMinutesBetween(startMs, endMs)

    /** Frequently used content for reuse suggestions. */
    suspend fun getFrequentContents(limit: Int): List<String> =
        db.focusRecordDao().getFrequentContents(limit)

    /** Most recent content for the next session's default. */
    suspend fun getLatestContent(): String? = db.focusRecordDao().getLatestContent()

    /** Delete all records with the specified content when clearing recent history. */
    suspend fun deleteRecordsByContent(content: String): Int =
        db.focusRecordDao().deleteByContent(content)

    /** Consecutive active days; no session today does not break the streak, which may count backward from yesterday. */
    fun getStreak(): LiveData<Int> = liveData(Dispatchers.IO) {
        emit(computeStreak())
    }

    /** Focus minutes for the last seven days, including today, for the bar chart. */
    fun getLast7Days(): LiveData<List<DayBar>> = liveData(Dispatchers.IO) {
        emit(buildLast7())
    }

    /** Heatmap: a 7 (Monday-Sunday) by 24 (hour) minute matrix bucketed by completion time. */
    fun getHeatmap(): LiveData<Array<IntArray>> = liveData(Dispatchers.IO) {
        val records = db.focusRecordDao().getAllRecordsRaw()
        val matrix = Array(7) { IntArray(24) }
        val cal = Calendar.getInstance()
        for (r in records) {
            cal.timeInMillis = r.createdAt
            val dow = (cal.get(Calendar.DAY_OF_WEEK) + 6) % 7 // 0=Monday through 6=Sunday.
            val hour = cal.get(Calendar.HOUR_OF_DAY)          // 0..23
            matrix[dow][hour] += r.minutes
        }
        emit(matrix)
    }

    /** Achievement badges as LiveData for reactive statistics display. */
    fun getAchievements(): LiveData<List<AchievementBadge>> = liveData(Dispatchers.IO) {
        emit(computeAchievements())
    }

    /** Non-LiveData snapshot for achievement poster detection. */
    suspend fun getAchievementsSnapshot(): List<AchievementBadge> = computeAchievements()

    /** Compute badges from the current streak and total session count. */
    private suspend fun computeAchievements(): List<AchievementBadge> {
        val streak = computeStreak()
        val sessions = db.focusRecordDao().getSessionCount()
        val defs = listOf(
            BadgeDef("first", "\uD83C\uDF31", com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_061), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_051), 1, sessions),
            BadgeDef("s3", "\uD83D\uDD25", com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_151), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_067), 3, streak),
            BadgeDef("s7", "\u2B50", com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_122), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_069), 7, streak),
            BadgeDef("s14", "\uD83D\uDCE0", com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_157), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_066), 14, streak),
            BadgeDef("s30", "\uD83D\uDC51", com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_103), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_068), 30, streak),
            BadgeDef("s100", "\uD83C\uDFC6", com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_087), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_065), 100, streak),
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

    // ---- Statistics helpers: Calendar supports minSdk 24 ----

    private fun todayStr(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

    private suspend fun computeStreak(): Int {
        val dates = db.focusRecordDao().getDailyMinutes().map { it.date }.toSet()
        if (dates.isEmpty()) return 0
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        // No session today does not break the streak: count backward from today or yesterday.
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
        val weekLabels = arrayOf(com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_146), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_102), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_156), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_176), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_152), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_079), com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_134))
        cal.add(Calendar.DAY_OF_MONTH, -6)
        return (0..6).map {
            val key = sdf.format(cal.time)
            val label = "" + weekLabels[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val bar = DayBar(label, map[key] ?: 0)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            bar
        }
    }

    /** Map active dates to minutes within a yyyy-MM month for calendar dots. */
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

/** Seven-day bar chart entry: weekday label and daily minutes. */
data class DayBar(val label: String, val minutes: Int)

/** Internal badge definition: target is the unlock threshold; progress is the current value. */
private data class BadgeDef(
    val id: String,
    val emoji: String,
    val title: String,
    val desc: String,
    val target: Int,
    val progress: Int
)
