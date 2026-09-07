package com.example.focuspets.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.focuspets.db.entity.PetEntity

@Dao
interface PetDao {

    /** 查询所有图鉴（按解锁价格升序：普通 → 传说） */
    @Query("SELECT * FROM pets ORDER BY unlock_cost ASC")
    suspend fun getAllPets(): List<PetEntity>

    @Query("SELECT * FROM pets WHERE id = :id")
    suspend fun getPetById(id: Int): PetEntity?

    /** 预置数据写入（重复插入忽略，防止重复 seed） */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(pets: List<PetEntity>)

    /** 图鉴页数据源：宠物 + 解锁状态，数据库一变 LiveData 自动刷新 */
    @Query(
        """
        SELECT pets.*,
               (user_collection.pet_id IS NOT NULL) AS is_unlocked
        FROM pets
        LEFT JOIN user_collection ON pets.id = user_collection.pet_id
        ORDER BY pets.unlock_cost ASC
        """
    )
    fun getPetsWithState(): LiveData<List<PetWithState>>

    /** 积分已达标但尚未解锁的宠物（专注完成后的庆祝提示用） */
    @Query(
        """
        SELECT pets.* FROM pets
        WHERE pets.unlock_cost > 0
          AND pets.unlock_cost <= :points
          AND pets.id NOT IN (SELECT pet_id FROM user_collection)
        ORDER BY pets.unlock_cost ASC
        """
    )
    suspend fun getAffordableLockedPets(points: Int): List<PetEntity>
}
