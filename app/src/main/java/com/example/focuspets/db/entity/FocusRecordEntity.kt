package com.example.focuspets.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 专注记录表：每完成一次番茄钟插入一条，duration_minutes 即获得的积分 */
@Entity(tableName = "focus_records")
data class FocusRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "focus_date") val focusDate: String,              // "2026-09-07"，按日统计用
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,     // 专注时长（分钟）
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
