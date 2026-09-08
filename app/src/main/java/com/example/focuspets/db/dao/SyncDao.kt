package com.example.focuspets.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.focuspets.db.entity.SyncMetaEntity
import com.example.focuspets.db.entity.SyncQueueEntity

/** 同步相关表访问（v4 新增） */
@Dao
interface SyncDao {

    @Insert
    suspend fun enqueue(op: SyncQueueEntity)

    @Query("DELETE FROM sync_queue")
    suspend fun clearQueue()

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun pendingCount(): Int

    @Query("SELECT value FROM sync_meta WHERE `key` = :k")
    suspend fun getMeta(k: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMeta(meta: SyncMetaEntity)

    @Query("DELETE FROM sync_meta")
    suspend fun clearMeta()
}
