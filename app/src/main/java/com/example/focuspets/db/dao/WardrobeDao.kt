package com.example.focuspets.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.focuspets.db.entity.WardrobePurchaseEntity

@Dao
interface WardrobeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(purchase: WardrobePurchaseEntity): Long

    @Query("SELECT item_id FROM wardrobe_purchases")
    fun getOwnedItemIds(): LiveData<List<String>>

    @Query("SELECT item_id FROM wardrobe_purchases")
    suspend fun getOwnedItemIdsOnce(): List<String>

    @Query("SELECT COALESCE(SUM(cost), 0) FROM wardrobe_purchases")
    fun getTotalSpent(): LiveData<Int>

    @Query("SELECT COALESCE(SUM(cost), 0) FROM wardrobe_purchases")
    suspend fun getTotalSpentOnce(): Int
}
