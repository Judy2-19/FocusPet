package com.example.focuspets.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 同步状态表：存 pending 标记、lastSyncAt 等键值对（v4 新增） */
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String
)
