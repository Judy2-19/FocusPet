package com.example.focuspets.db

import com.example.focuspets.db.entity.PetEntity
import com.example.focuspets.db.entity.UserCollectionEntity

/**
 * Seed provider with two idempotent entry points: Room onCreate on first installation, and an empty-
 * table check at each startup for missed callbacks or cleared data. insertAll uses
 * OnConflictStrategy.IGNORE; check existing unlocks before granting pet 1 to avoid pet_id uniqueness
 * conflicts.
 */
object InitialDataProvider {

    /** Nine preset pets: three common, three rare, three legendary; fixed IDs 1-9 match migrations. */
    val PRESET_PETS: List<PetEntity> = listOf(
        PetEntity(1, "小猫咪", "🐱", Rarity.COMMON, 0, "可换装的小猫咪，默认灰色"),
        PetEntity(2, "小狗", "🐶", Rarity.COMMON, 50, "忠诚的小伙伴（暂未实装立绘）"),
        PetEntity(3, "龙猫", "🐹", Rarity.COMMON, 100, "软糯的小毛球（暂未实装立绘）"),
        PetEntity(4, "星光兽", "⭐", Rarity.RARE, 300, "只在深夜出现"),
        PetEntity(5, "雷电犬", "⚡", Rarity.RARE, 500, "行动迅捷如闪电"),
        PetEntity(6, "冰晶狐", "❄️", Rarity.RARE, 700, "高傲的冰雪贵族"),
        PetEntity(7, "暗影龙", "🌑", Rarity.LEGENDARY, 1200, "拥有毁灭力量"),
        PetEntity(8, "神圣鹿", "🦌", Rarity.LEGENDARY, 1800, "森林的守护神"),
        PetEntity(9, "创世神", "🌌", Rarity.LEGENDARY, 2500, "集齐所有图鉴后的终极奖励")
    )

    /** Initially granted pet: water droplet ID 1, with unlock_cost=0 and no special cost handling. */
    const val FREE_PET_ID = 1

    /**
     * If empty, seed the database and grant pet 1. Call from an IO coroutine because DAOs suspend.
     */
    suspend fun ensureSeeded(db: AppDatabase) {
        val existing = db.petDao().getAllPets()
        if (existing.isEmpty()) {
            db.petDao().insertAll(PRESET_PETS)
        }

        // Grant common pet 1 only if not already unlocked, preventing uniqueness conflicts.
        val unlocked = db.userCollectionDao().getUnlockedPetIds()
        if (FREE_PET_ID !in unlocked) {
            db.userCollectionDao()
                .unlockPet(UserCollectionEntity(petId = FREE_PET_ID))
        }
    }
}
