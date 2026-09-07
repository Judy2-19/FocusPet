package com.example.focuspets.db

import com.example.focuspets.db.entity.PetEntity
import com.example.focuspets.db.entity.UserCollectionEntity

/**
 * 数据初始化与预置提供者。
 *
 * 两种触发路径（二者幂等，可安全重复调用）：
 * 1. 首次安装：Room onCreate 回调 → ensureSeeded()
 * 2. 防御性兜底：每次 App 启动后检查一次 pets 表是否为空（应对 onCreate 未执行/数据被清空的极端情况）
 *
 * 幂等保障：
 * - insertAll 使用 OnConflictStrategy.IGNORE（主键冲突直接跳过，不重复 seed）
 * - 赠送 1 号宠物前先查已解锁列表，避免唯一索引 pet_id 冲突
 */
object InitialDataProvider {

    /** 9 只预置宠物：普通 x3、稀有 x3、传说 x3（id 固定 1~9，与 MIGRATION 保持一致） */
    val PRESET_PETS: List<PetEntity> = listOf(
        PetEntity(1, "小水滴", "💧", Rarity.COMMON, 0, "最基础的元素精灵"),
        PetEntity(2, "火苗崽", "🔥", Rarity.COMMON, 50, "充满热情的小家伙"),
        PetEntity(3, "木灵", "🌿", Rarity.COMMON, 100, "喜欢安静地睡觉"),
        PetEntity(4, "星光兽", "⭐", Rarity.RARE, 300, "只在深夜出现"),
        PetEntity(5, "雷电犬", "⚡", Rarity.RARE, 500, "行动迅捷如闪电"),
        PetEntity(6, "冰晶狐", "❄️", Rarity.RARE, 700, "高傲的冰雪贵族"),
        PetEntity(7, "暗影龙", "🌑", Rarity.LEGENDARY, 1200, "拥有毁灭力量"),
        PetEntity(8, "神圣鹿", "🦌", Rarity.LEGENDARY, 1800, "森林的守护神"),
        PetEntity(9, "创世神", "🌌", Rarity.LEGENDARY, 2500, "集齐所有图鉴后的终极奖励")
    )

    /** 首次赠送的宠物：1 号小水滴（unlock_cost = 0，天然不需要特判） */
    private const val FREE_PET_ID = 1

    /**
     * 检查数据库是否为空；为空则插入预置宠物并默认赠送解锁 1 号。
     * 必须在 IO 协程中调用（suspend DAO）。
     */
    suspend fun ensureSeeded(db: AppDatabase) {
        val existing = db.petDao().getAllPets()
        if (existing.isEmpty()) {
            db.petDao().insertAll(PRESET_PETS)
        }

        // 默认赠送：1 号普通宠物已解锁（已解锁则跳过，防止唯一索引冲突）
        val unlocked = db.userCollectionDao().getUnlockedPetIds()
        if (FREE_PET_ID !in unlocked) {
            db.userCollectionDao()
                .unlockPet(UserCollectionEntity(petId = FREE_PET_ID))
        }
    }
}
