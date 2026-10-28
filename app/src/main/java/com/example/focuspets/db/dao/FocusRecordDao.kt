package com.example.focuspets.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.focuspets.db.entity.FocusRecordEntity

@Dao
interface FocusRecordDao {

    /** Insert a session record when a timer completes. */
    @Insert
    suspend fun insert(record: FocusRecordEntity): Long

    /** Sum points across all records; return 0 for an empty table. */
    @Query("SELECT COALESCE(SUM(points), 0) FROM focus_records")
    suspend fun getTotalPoints(): Int

    /** Sum actual focus duration in minutes, excluding point-only test grants. */
    @Query("SELECT COALESCE(SUM(duration_minutes), 0) FROM focus_records")
    suspend fun getTotalDuration(): Int

    /** Focus minutes on a date for daily statistics. */
    @Query(
        "SELECT COALESCE(SUM(duration_minutes), 0) FROM focus_records WHERE focus_date = :date"
    )
    suspend fun getMinutesOnDate(date: String): Int

    /** Clear all session records when resetting progress. */
    @Query("DELETE FROM focus_records")
    suspend fun deleteAll()

    /** Completed session count: one record per completed timer. */
    @Query("SELECT COUNT(*) FROM focus_records")
    suspend fun getSessionCount(): Int

    /** Daily focus minutes for streaks and seven-day charts. */
    @Query(
        "SELECT focus_date AS date, COALESCE(SUM(duration_minutes), 0) AS minutes " +
            "FROM focus_records GROUP BY focus_date"
    )
    suspend fun getDailyMinutes(): List<DayMinutes>

    /** Daily minutes in an inclusive yyyy-MM-dd range for calendar dots. */
    @Query(
        "SELECT focus_date AS date, COALESCE(SUM(duration_minutes), 0) AS minutes " +
            "FROM focus_records WHERE focus_date BETWEEN :startDate AND :endDate " +
            "GROUP BY focus_date"
    )
    suspend fun getDailyMinutesBetweenDates(
        startDate: String,
        endDate: String
    ): List<DayMinutes>

    /** Debug/test branch: delete injected point grants when clearing large test balances. */
    @Query("DELETE FROM focus_records WHERE focus_date = :marker")
    suspend fun deleteTestGrants(marker: String)

    /** Delete all records with the specified content when clearing recent history. */
    @Query("DELETE FROM focus_records WHERE content = :content AND content != ''")
    suspend fun deleteByContent(content: String): Int

    /** Lightweight records for weekday-by-hour heatmap bucketing using created_at. */
    @Query("SELECT focus_date, duration_minutes, created_at FROM focus_records")
    suspend fun getAllRecordsRaw(): List<RecordRaw>

    /** Records including content in an interval, newest completion first, for the records list. */
    @Query(
        "SELECT * FROM focus_records WHERE created_at BETWEEN :startMs AND :endMs " +
            "ORDER BY created_at DESC"
    )
    suspend fun getRecordsBetween(startMs: Long, endMs: Long): List<FocusRecordEntity>

    /**
     * Aggregate points by content for the pie chart. Use points rather than duration_minutes because test
     * builds may grant points with zero duration; duration would yield an empty chart. Exclude test-grant
     * records so a million-point grant cannot dominate the chart.
     */
    @Query(
        "SELECT COALESCE(content, '') AS content, COALESCE(SUM(points), 0) AS minutes " +
            "FROM focus_records WHERE created_at BETWEEN :startMs AND :endMs " +
            "AND focus_date != 'test-grant' " +
            "GROUP BY content ORDER BY minutes DESC"
    )
    suspend fun getContentMinutesBetween(startMs: Long, endMs: Long): List<ContentMinutes>

    /** Return at most limit frequently used content values for reuse suggestions. */
    @Query(
        "SELECT content FROM focus_records WHERE content IS NOT NULL AND content != '' " +
            "GROUP BY content ORDER BY COUNT(*) DESC, MAX(created_at) DESC LIMIT :limit"
    )
    suspend fun getFrequentContents(limit: Int): List<String>

    /** Most recently used content for the next default. */
    @Query(
        "SELECT content FROM focus_records WHERE content IS NOT NULL AND content != '' " +
            "ORDER BY created_at DESC LIMIT 1"
    )
    suspend fun getLatestContent(): String?
}

/** DAO aggregation result: focus minutes for a content value within an interval. */
data class ContentMinutes(
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "minutes") val minutes: Int
)

/** DAO aggregation result: focus minutes on a date. */
data class DayMinutes(
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "minutes") val minutes: Int
)

/** Lightweight session projection: date, minutes, and insertion/completion timestamp for time bucketing. */
data class RecordRaw(
    @ColumnInfo(name = "focus_date") val focusDate: String,
    @ColumnInfo(name = "duration_minutes") val minutes: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
