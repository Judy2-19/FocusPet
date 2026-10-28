package com.example.focuspets.db.dao

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.example.focuspets.db.entity.PetEntity

/** Query result combining a pet with its unlock state through LEFT JOIN user_collection. */
data class PetWithState(
    @Embedded val pet: PetEntity,
    @ColumnInfo(name = "is_unlocked") val isUnlocked: Boolean
)
