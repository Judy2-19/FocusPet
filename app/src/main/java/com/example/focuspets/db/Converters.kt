package com.example.focuspets.db

import androidx.room.TypeConverter

/** 宠物稀有度：普通 / 稀有 / 传说 */
enum class Rarity { COMMON, RARE, LEGENDARY }

/** Room 不认识枚举，需要 TypeConverter 转成 String 存储 */
class Converters {
    @TypeConverter
    fun fromRarity(rarity: Rarity): String = rarity.name

    @TypeConverter
    fun toRarity(value: String): Rarity = Rarity.valueOf(value)
}
