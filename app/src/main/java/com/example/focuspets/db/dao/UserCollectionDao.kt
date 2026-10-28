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

    /** Join unlocked entries with full pet data, sorted by unlock time. */
    @Query(
        """
        SELECT pets.* FROM pets
        INNER JOIN user_collection ON pets.id = user_collection.pet_id
        ORDER BY user_collection.unlocked_at ASC
        """
    )
    suspend fun getUnlockedPets(): List<PetEntity>

    /** Unlocked pet IDs for duplicate checks inside unlock transactions. */
    @Query("SELECT pet_id FROM user_collection")
    suspend fun getUnlockedPetIds(): List<Int>

    /**
     * Unlock by inserting a collection record; points are derived, so no deduction UPDATE is needed.
     * IGNORE pet_id uniqueness conflicts to keep concurrent seed/fallback calls idempotent.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlockPet(collection: UserCollectionEntity)

    /** Debug/test branch: batch unlock using IGNORE for idempotence. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlockPets(collections: List<UserCollectionEntity>)

    /** Debug/test branch: clear collection entries except keepId, restoring the initial gift-only state. */
    @Query("DELETE FROM user_collection WHERE pet_id != :keepId")
    suspend fun deleteExcept(keepId: Int)

    /** Spent points: sum of unlocked pets' unlock_cost values. */
    @Query(
        """
        SELECT COALESCE(SUM(pets.unlock_cost), 0) FROM pets
        INNER JOIN user_collection ON pets.id = user_collection.pet_id
        """
    )
    suspend fun getSpentPoints(): Int

    /** Available points as LiveData: total points minus pet unlock and wardrobe costs. */
    @Query(
        """
        SELECT (SELECT COALESCE(SUM(points), 0) FROM focus_records)
             - (SELECT COALESCE(SUM(pets.unlock_cost), 0)
                FROM pets
                INNER JOIN user_collection ON pets.id = user_collection.pet_id)
             - (SELECT COALESCE(SUM(cost), 0) FROM wardrobe_purchases)
        """
    )
    fun getAvailablePoints(): LiveData<Int>

    /** Available points as a suspend query for unlock/purchase transaction checks. */
    @Query(
        """
        SELECT (SELECT COALESCE(SUM(points), 0) FROM focus_records)
             - (SELECT COALESCE(SUM(pets.unlock_cost), 0)
                FROM pets
                INNER JOIN user_collection ON pets.id = user_collection.pet_id)
             - (SELECT COALESCE(SUM(cost), 0) FROM wardrobe_purchases)
        """
    )
    suspend fun getAvailablePointsOnce(): Int
}
