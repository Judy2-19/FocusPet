package com.example.focuspets.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One record per completed timer. duration_minutes stores actual focus time for statistics; points
 * stores the reward independently (normally one per minute). Test builds may inject points with zero
 * duration to avoid inflating focus time. content stores the session task; empty means unspecified.
 */
@Entity(tableName = "focus_records")
data class FocusRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "focus_date") val focusDate: String,              // Date such as 2026-09-07 for daily aggregation.
    @ColumnInfo(name = "content") val content: String = "",             // Session content; defaults to an empty string.
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,     // Focus duration in minutes.
    @ColumnInfo(name = "points") val points: Int = 0,                   // Earned points; defaults to zero, normally equals duration, but can be injected independently.
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
