package com.example.focuspets.db

import androidx.lifecycle.LiveData
import androidx.room.withTransaction
import com.example.focuspets.db.dao.PetWithState
import com.example.focuspets.db.entity.PetEntity
import com.example.focuspets.db.entity.UserCollectionEntity

class PetRepository(private val db: AppDatabase) {

    fun getPetsWithState(): LiveData<List<PetWithState>> = db.petDao().getPetsWithState()

    fun getAvailablePoints(): LiveData<Int> = db.userCollectionDao().getAvailablePoints()

    /**
     * 解锁宠物：事务内做三重校验，防止并发/重复点击导致超扣。
     * 「扣积分」不需要 UPDATE 任何表——插入收藏记录后，
     * 可用积分 = 总专注分钟 - 已解锁 unlock_cost 之和，自然减少。
     */
    suspend fun unlockPet(pet: PetEntity): Boolean = db.withTransaction {
        val unlockedIds = db.userCollectionDao().getUnlockedPetIds()
        if (pet.id in unlockedIds) return@withTransaction false          // 已解锁
        if (db.userCollectionDao().getAvailablePointsOnce() < pet.unlockCost) {
            return@withTransaction false                                 // 积分不够
        }
        db.userCollectionDao().unlockPet(UserCollectionEntity(petId = pet.id))
        true
    }

    /** 专注完成结算后：查询「积分已达标但尚未解锁」的宠物，用于庆祝弹窗 */
    suspend fun getAffordableLockedPets(): List<PetEntity> {
        val points = db.userCollectionDao().getAvailablePointsOnce()
        return db.petDao().getAffordableLockedPets(points)
    }
}
