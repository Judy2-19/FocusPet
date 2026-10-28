package com.example.focuspets.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.focuspets.db.Rarity
import kotlinx.parcelize.Parcelize

/** Read-only pet catalog with nine preset entries. */
@Parcelize
@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey val id: Int,                                // Fixed IDs 1-9.
    val name: String,                                       // Name.
    val emoji: String,                                     // Emoji icon.
    val rarity: Rarity,                                    // Rarity.
    @ColumnInfo(name = "unlock_cost") val unlockCost: Int,  // Points required to unlock.
    val description: String                                // Description text.
) : Parcelable
