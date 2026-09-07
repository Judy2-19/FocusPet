package com.example.focuspets.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 我的收藏表：记录用户已解锁的宠物。
 * 单用户 App 中「宠物 <-> 收藏记录」是 1:0..1 关系，
 * 用 pet_id 唯一索引保证每只宠物只能解锁一次。
 */
@Entity(
    tableName = "user_collection",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["pet_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["pet_id"], unique = true)]
)
data class UserCollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pet_id") val petId: Int,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Long = System.currentTimeMillis()
)
