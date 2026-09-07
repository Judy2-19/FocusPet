package com.example.focuspets.db.dao

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.example.focuspets.db.entity.PetEntity

/** 宠物 + 是否已解锁的查询结果（LEFT JOIN user_collection） */
data class PetWithState(
    @Embedded val pet: PetEntity,
    @ColumnInfo(name = "is_unlocked") val isUnlocked: Boolean
)
