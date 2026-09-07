package com.example.focuspets.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.focuspets.db.Rarity
import kotlinx.parcelize.Parcelize

/** 宠物图鉴表：9 条预置数据，只读不增删 */
@Parcelize
@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey val id: Int,                                // 1~9，固定编号
    val name: String,                                       // 名称
    val emoji: String,                                     // emoji 图标
    val rarity: Rarity,                                    // 稀有度
    @ColumnInfo(name = "unlock_cost") val unlockCost: Int,  // 解锁所需积分
    val description: String                                // 描述文案
) : Parcelable
