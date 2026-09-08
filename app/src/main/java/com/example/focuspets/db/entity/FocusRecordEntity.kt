package com.example.focuspets.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 专注记录表：每完成一次番茄钟插入一条。
 * - duration_minutes：真实专注时长（分钟），用于「累计专注时间」统计与时段分析。
 * - points：本次获得的积分。正常 1 分钟 = 1 分，但可独立于时长注入
 *   （测试分支用 duration=0、points=大额 的方式发积分，避免污染「累计专注时间」）。
 * content 为「本次专注内容」（如「写代码」「看书」），空串表示未填写。
 */
@Entity(tableName = "focus_records")
data class FocusRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "focus_date") val focusDate: String,              // "2026-09-07"，按日统计用
    @ColumnInfo(name = "content") val content: String = "",             // 本次专注内容（可空，默认空串）
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,     // 专注时长（分钟）
    @ColumnInfo(name = "points") val points: Int = 0,                   // 获得积分（默认 0；正常 = 时长，可独立注入）
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
