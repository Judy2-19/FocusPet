package com.example.focuspets.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User collection of unlocked pets. Each pet has zero or one collection entry in this single-user app;
 * a unique pet_id index prevents duplicate unlocks.
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
