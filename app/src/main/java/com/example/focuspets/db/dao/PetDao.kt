package com.example.focuspets.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.focuspets.db.entity.PetEntity

@Dao
interface PetDao {

    /** Query the catalog by increasing unlock cost, from common to legendary. */
    @Query("SELECT * FROM pets ORDER BY unlock_cost ASC")
    suspend fun getAllPets(): List<PetEntity>

    @Query("SELECT * FROM pets WHERE id = :id")
    suspend fun getPetById(id: Int): PetEntity?

    /** Insert seed data, ignoring duplicates to prevent repeated seeding. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(pets: List<PetEntity>)

    /** Collection data source: pet plus unlock state; LiveData updates when the database changes. */
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

    /** Affordable pets still locked, for completion celebrations. */
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
