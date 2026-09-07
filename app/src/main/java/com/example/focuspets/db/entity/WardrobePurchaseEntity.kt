package com.example.focuspets.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 小猫咪妆扮购买记录。
 * 用 Room 表而非 SharedPreferences，是因为购买会消耗积分，
 * 必须与 focus_records / user_collection 一起参与「可用积分」的派生计算。
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
