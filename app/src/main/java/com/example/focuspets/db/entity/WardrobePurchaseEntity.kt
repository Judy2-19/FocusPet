package com.example.focuspets.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cat wardrobe purchase records. Use Room rather than SharedPreferences because purchases spend points
 * and must participate in the balance calculation alongside focus_records and user_collection.
 */
@Entity(tableName = "wardrobe_purchases")
data class WardrobePurchaseEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: String,     // e.g. "color_blue", "dress", "crown_blue_crown"

    @ColumnInfo(name = "item_type")
    val itemType: String,   // "color" | "dress" | "crown"

    val cost: Int
)
