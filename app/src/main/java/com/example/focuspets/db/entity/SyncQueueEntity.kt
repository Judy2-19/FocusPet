package com.example.focuspets.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Offline write queue for local changes not yet uploaded successfully; added in v4. */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "op_type") val opType: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
