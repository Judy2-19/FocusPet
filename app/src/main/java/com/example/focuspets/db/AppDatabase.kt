package com.example.focuspets.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.focuspets.db.dao.FocusRecordDao
import com.example.focuspets.db.dao.PetDao
import com.example.focuspets.db.dao.UserCollectionDao
import com.example.focuspets.db.dao.WardrobeDao
import com.example.focuspets.db.entity.FocusRecordEntity
import com.example.focuspets.db.entity.PetEntity
import com.example.focuspets.db.entity.UserCollectionEntity
import com.example.focuspets.db.dao.SyncDao
import com.example.focuspets.db.entity.SyncMetaEntity
import com.example.focuspets.db.entity.SyncQueueEntity
import com.example.focuspets.db.entity.WardrobePurchaseEntity
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        PetEntity::class,
        UserCollectionEntity::class,
        FocusRecordEntity::class,
        WardrobePurchaseEntity::class,
        SyncQueueEntity::class,
        SyncMetaEntity::class
    ],
    version = 7,            // v6 to v7: translate the built-in catalog without changing progress.
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun petDao(): PetDao
    abstract fun userCollectionDao(): UserCollectionDao
    abstract fun focusRecordDao(): FocusRecordDao
    abstract fun wardrobeDao(): WardrobeDao
    abstract fun syncDao(): SyncDao

    companion object {
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (pet in InitialDataProvider.PRESET_PETS) {
                    db.execSQL(
                        "UPDATE pets SET name=?, description=? WHERE id=?",
                        arrayOf<Any>(pet.name, pet.description, pet.id)
                    )
                }
            }
        }
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Seed data in a background coroutine with an exception handler so seeding errors do not kill the process.
        private val seedScope = CoroutineScope(
            SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, t ->
                Log.e("AppDatabase", "seed failed (non-fatal): ${t.message}", t)
            }
        )

        /**
         * v2 to v3 migration: add wardrobe_purchases to record cat outfit spending.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS wardrobe_purchases (
                        item_id TEXT PRIMARY KEY NOT NULL,
                        item_type TEXT NOT NULL,
                        cost INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * v1 to v2 data migration: replace catalog data with nine pets. Use UPDATE rather than INSERT OR
         * REPLACE: SQLite REPLACE deletes before inserting, so ON DELETE CASCADE on user_collection.pet_id
         * would erase existing unlocks. UPDATE preserves them.
         */
        /**
         * v3 to v4 migration: add sync_queue for offline writes and sync_meta for synchronization state.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        op_type TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_meta (
                        `key` TEXT PRIMARY KEY NOT NULL,
                        value TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * v4 to v5 migration: add content as TEXT defaulting to an empty string. Preserve old records without
         * affecting points or statistics.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE focus_records ADD COLUMN content TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /**
         * v5 to v6 migration: add points independently of duration. Backfill historical points from
         * duration_minutes; FocusService and debug tools explicitly set points on new records.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE focus_records ADD COLUMN points INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("UPDATE focus_records SET points = duration_minutes")
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ---- Common ----
                db.execSQL("UPDATE pets SET name='Droplet', emoji='💧', rarity='COMMON', unlock_cost=0, description='A friendly elemental spirit' WHERE id=1")
                db.execSQL("UPDATE pets SET name='Ember', emoji='🔥', rarity='COMMON', unlock_cost=50, description='A little friend full of enthusiasm' WHERE id=2")
                db.execSQL("UPDATE pets SET name='Wood Sprite', emoji='🌿', rarity='COMMON', unlock_cost=100, description='Loves a quiet nap' WHERE id=3")
                // ---- Rare ----
                db.execSQL("UPDATE pets SET name='Starlight Beast', emoji='⭐', rarity='RARE', unlock_cost=300, description='Appears only late at night' WHERE id=4")
                db.execSQL("UPDATE pets SET name='Thunder Hound', emoji='⚡', rarity='RARE', unlock_cost=500, description='Moves as fast as lightning' WHERE id=5")
                db.execSQL("UPDATE pets SET name='Ice Fox', emoji='❄️', rarity='RARE', unlock_cost=700, description='A noble spirit of ice and snow' WHERE id=6")
                // ---- Legendary ----
                db.execSQL("UPDATE pets SET name='Shadow Dragon', emoji='🌑', rarity='LEGENDARY', unlock_cost=1200, description='Wields tremendous power' WHERE id=7")
                db.execSQL("UPDATE pets SET name='Sacred Deer', emoji='🦌', rarity='LEGENDARY', unlock_cost=1800, description='Guardian of the forest' WHERE id=8")
                db.execSQL("UPDATE pets SET name='Creator', emoji='🌌', rarity='LEGENDARY', unlock_cost=2500, description='The ultimate collection reward' WHERE id=9")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focus_pets.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)   // Upgrade pet data in place on older devices while retaining session records.
                    .fallbackToDestructiveMigration()   // Fallback: rebuild an incompatible leftover database rather than crash; demo data may be discarded.
                    .addCallback(SeedCallback)      // Seed data when the database is first created.
                    .build()
                    .also { INSTANCE = it }
                    .also {
                        // Startup fallback: seed the pets table if it is empty.
                        // Room suspend DAOs wait until opening and onCreate finish before executing.
                        // Concurrent ensureSeeded calls from SeedCallback remain idempotent through IGNORE and unlock checks.
                        seedScope.launch { InitialDataProvider.ensureSeeded(it) }
                    }
            }

        /**
         * Initial database callback: Room has already created tables before onCreate. Delegate inserts to
         * InitialDataProvider so initial creation and startup fallback share the same seeding code.
         */
        private object SeedCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedScope.launch {
                    try {
                        val database = INSTANCE ?: return@launch
                        InitialDataProvider.ensureSeeded(database)
                    } catch (e: Exception) {
                        Log.e("AppDatabase", "seed in callback failed (non-fatal): ${e.message}", e)
                    }
                }
            }
        }
    }
}
