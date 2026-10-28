package com.example.focuspets.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Synchronization metadata key/value table for pending, lastSyncAt, etc.; added in v4. */
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String
)
