package com.example.focuspets.db

import androidx.room.TypeConverter

/** Pet rarity: common, rare, legendary. */
enum class Rarity { COMMON, RARE, LEGENDARY }

/** Room needs a TypeConverter to store enum values as strings. */
class Converters {
    @TypeConverter
    fun fromRarity(rarity: Rarity): String = rarity.name

    @TypeConverter
    fun toRarity(value: String): Rarity = Rarity.valueOf(value)
}
