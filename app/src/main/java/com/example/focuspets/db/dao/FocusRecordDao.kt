package com.example.focuspets.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.focuspets.db.entity.FocusRecordEntity

@Dao
interface FocusRecordDao {

    /** 插入一条专注记录（番茄钟完成时调用） */
    @Insert
    suspend fun insert(record: FocusRecordEntity): Long

    /** 累计总积分（所有专注记录 points 之和，空表返回 0） */
    @Query("SELECT COALESCE(SUM(points), 0) FROM focus_records")
    suspend fun getTotalPoints(): Int

    /** 累计专注总时长（分钟），用于「累计专注时间」等真实时长统计（不含测试注入的纯积分） */
    @Query("SELECT COALESCE(SUM(duration_minutes), 0) FROM focus_records")
    suspend fun getTotalDuration(): Int

    /** 某一天的专注总分钟数，日统计页用 */
    @Query(
        "SELECT COALESCE(SUM(duration_minutes), 0) FROM focus_records WHERE focus_date = :date"
    )
    suspend fun getMinutesOnDate(date: String): Int

    /** 清空全部专注记录（重置进度用） */
    @Query("DELETE FROM focus_records")
    suspend fun deleteAll()

    /** 专注总次数（每条记录 = 一次完成的番茄钟） */
    @Query("SELECT COUNT(*) FROM focus_records")
    suspend fun getSessionCount(): Int

    /** 每个有记录的日期的专注总分钟数，用于连续打卡 / 近 7 天图表 */
    @Query(
        "SELECT focus_date AS date, COALESCE(SUM(duration_minutes), 0) AS minutes " +
            "FROM focus_records GROUP BY focus_date"
    )
    suspend fun getDailyMinutes(): List<DayMinutes>

    /** 指定日期区间（含两端，格式 yyyy-MM-dd）内，每天的专注总分钟数，用于日历红点标记 */
    @Query(
        "SELECT focus_date AS date, COALESCE(SUM(duration_minutes), 0) AS minutes " +
            "FROM focus_records WHERE focus_date BETWEEN :startDate AND :endDate " +
            "GROUP BY focus_date"
    )
    suspend fun getDailyMinutesBetweenDates(
        startDate: String,
        endDate: String
    ): List<DayMinutes>

    /** 调试（仅 test 分支）：删除测试注入的积分记录，方便清掉大额积分 */
    @Query("DELETE FROM focus_records WHERE focus_date = :marker")
    suspend fun deleteTestGrants(marker: String)

    /** 删除指定专注内容的全部记录（用户手动清理「最近用过」历史用） */
    @Query("DELETE FROM focus_records WHERE content = :content AND content != ''")
    suspend fun deleteByContent(content: String): Int

    /** 全部专注记录（轻量投影），用于时段热力图按 created_at 分桶到「星期 × 小时」 */
    @Query("SELECT focus_date, duration_minutes, created_at FROM focus_records")
    suspend fun getAllRecordsRaw(): List<RecordRaw>

    /** 某时间区间内的全部专注记录（含内容），按完成时间倒序，记录页列表用 */
    @Query(
        "SELECT * FROM focus_records WHERE created_at BETWEEN :startMs AND :endMs " +
            "ORDER BY created_at DESC"
    )
    suspend fun getRecordsBetween(startMs: Long, endMs: Long): List<FocusRecordEntity>

    /** 某时间区间内，各专注内容所占的「专注值（points）」聚合，饼图用。
     *  注意：这里用 points 而非 duration_minutes——测试分支会把时长置 0 只发积分，
     *  用 duration 会令内容全部归零、饼图空白；同时排除 test-grant 注入记录，
     *  避免「测试积分」这一条（duration=0、points=100 万）独占整张图。 */
    @Query(
        "SELECT COALESCE(content, '') AS content, COALESCE(SUM(points), 0) AS minutes " +
            "FROM focus_records WHERE created_at BETWEEN :startMs AND :endMs " +
            "AND focus_date != 'test-grant' " +
            "GROUP BY content ORDER BY minutes DESC"
    )
    suspend fun getContentMinutesBetween(startMs: Long, endMs: Long): List<ContentMinutes>

    /** 使用频率最高的专注内容（用于「复用历史」建议），最多 limit 条 */
    @Query(
        "SELECT content FROM focus_records WHERE content IS NOT NULL AND content != '' " +
            "GROUP BY content ORDER BY COUNT(*) DESC, MAX(created_at) DESC LIMIT :limit"
    )
    suspend fun getFrequentContents(limit: Int): List<String>

    /** 最近一次使用的专注内容（用于下次默认填充） */
    @Query(
        "SELECT content FROM focus_records WHERE content IS NOT NULL AND content != '' " +
            "ORDER BY created_at DESC LIMIT 1"
    )
    suspend fun getLatestContent(): String?
}

/** 某内容在时段内的专注分钟数（DAO 内部聚合结果） */
data class ContentMinutes(
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "minutes") val minutes: Int
)

/** 某天专注分钟数（DAO 内部聚合结果） */
data class DayMinutes(
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "minutes") val minutes: Int
)

/** 专注记录轻量投影：日期 + 分钟 + 写入时间戳（= 完成时刻，用于时段分桶） */
data class RecordRaw(
    @ColumnInfo(name = "focus_date") val focusDate: String,
    @ColumnInfo(name = "duration_minutes") val minutes: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
