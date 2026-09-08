package com.example.focuspets.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 离线写队列：本地有变更但暂未上报成功时入队（v4 新增） */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "op_type") val opType: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
