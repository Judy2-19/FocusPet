package com.example.focuspets.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.focuspets.db.entity.PetEntity
import com.example.focuspets.db.entity.UserCollectionEntity

@Dao
interface UserCollectionDao {

    /** 查询已解锁列表（连表查出宠物完整信息，按解锁时间排序） */
    @Query(
        """
        SELECT pets.* FROM pets
        INNER JOIN user_collection ON pets.id = user_collection.pet_id
        ORDER BY user_collection.unlocked_at ASC
        """
    )
    suspend fun getUnlockedPets(): List<PetEntity>

    /** 已解锁的宠物 id 集合，解锁事务校验重复用 */
    @Query("SELECT pet_id FROM user_collection")
    suspend fun getUnlockedPetIds(): List<Int>

    /**
     * 解锁新宠物 = 插入一条收藏记录（积分是派生值，无需 UPDATE 扣分）。
     * IGNORE：pet_id 唯一索引冲突时跳过而非崩溃，保证 seed/兜底检查并发调用时幂等。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlockPet(collection: UserCollectionEntity)

    /** 已消耗积分（已解锁宠物的 unlock_cost 之和） */
    @Query(
        """
        SELECT COALESCE(SUM(pets.unlock_cost), 0) FROM pets
        INNER JOIN user_collection ON pets.id = user_collection.pet_id
        """
    )
    suspend fun getSpentPoints(): Int

    /** 可用积分（LiveData 版）：总专注分钟 - 已消耗，随数据库变化自动刷新 */
    @Query(
        """
        SELECT (SELECT COALESCE(SUM(duration_minutes), 0) FROM focus_records)
             - (SELECT COALESCE(SUM(pets.unlock_cost), 0)
                FROM pets
                INNER JOIN user_collection ON pets.id = user_collection.pet_id)
        """
    )
    fun getAvailablePoints(): LiveData<Int>

    /** 可用积分（suspend 版）：解锁事务内校验用 */
    @Query(
        """
        SELECT (SELECT COALESCE(SUM(duration_minutes), 0) FROM focus_records)
             - (SELECT COALESCE(SUM(pets.unlock_cost), 0)
                FROM pets
                INNER JOIN user_collection ON pets.id = user_collection.pet_id)
        """
    )
    suspend fun getAvailablePointsOnce(): Int
}
