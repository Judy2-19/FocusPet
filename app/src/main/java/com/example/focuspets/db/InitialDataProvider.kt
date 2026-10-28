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
        PetEntity(1, "Kitten", "🐱", Rarity.COMMON, 0, "A dress-up kitten with a gray coat"),
        PetEntity(2, "Dog", "🐶", Rarity.COMMON, 50, "A loyal little companion"),
        PetEntity(3, "Chinchilla", "🐹", Rarity.COMMON, 100, "A soft and fluffy companion"),
        PetEntity(4, "Starlight Beast", "⭐", Rarity.RARE, 300, "Appears only late at night"),
        PetEntity(5, "Thunder Hound", "⚡", Rarity.RARE, 500, "Moves as fast as lightning"),
        PetEntity(6, "Ice Fox", "❄️", Rarity.RARE, 700, "A noble spirit of ice and snow"),
        PetEntity(7, "Shadow Dragon", "🌑", Rarity.LEGENDARY, 1200, "Wields tremendous power"),
        PetEntity(8, "Sacred Deer", "🦌", Rarity.LEGENDARY, 1800, "Guardian of the forest"),
        PetEntity(9, "Creator", "🌌", Rarity.LEGENDARY, 2500, "The ultimate collection reward")
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
