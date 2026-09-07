package com.example.focuspets.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.focuspets.db.entity.FocusRecordEntity

@Dao
interface FocusRecordDao {

    /** 插入一条专注记录（番茄钟完成时调用） */
    @Insert
    suspend fun insert(record: FocusRecordEntity): Long

    /** 累计总积分（总专注分钟数，空表返回 0） */
    @Query("SELECT COALESCE(SUM(duration_minutes), 0) FROM focus_records")
    suspend fun getTotalPoints(): Int

    /** 某一天的专注总分钟数，日统计页用 */
    @Query(
        "SELECT COALESCE(SUM(duration_minutes), 0) FROM focus_records WHERE focus_date = :date"
    )
    suspend fun getMinutesOnDate(date: String): Int

    /** 调试（仅 test 分支）：删除测试注入的积分记录，方便清掉大额积分 */
    @Query("DELETE FROM focus_records WHERE focus_date = :marker")
    suspend fun deleteTestGrants(marker: String)
}
