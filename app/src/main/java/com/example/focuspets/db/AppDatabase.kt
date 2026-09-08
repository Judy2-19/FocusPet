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
    version = 6,            // v5 → v6：focus_records 新增 points 列，把「积分」从「专注分钟」中拆出，支持测试注入纯积分
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
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 种子数据写入放到后台协程；用异常处理器兜底，避免 seed 失败（如数据异常）杀进程
        private val seedScope = CoroutineScope(
            SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, t ->
                Log.e("AppDatabase", "seed failed (non-fatal): ${t.message}", t)
            }
        )

        /**
         * v2 → v3 结构迁移：新增 wardrobe_purchases 表，记录猫咪妆扮消费。
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
         * v1 → v2 数据迁移：把宠物图鉴数据替换为新的 9 只。
         *
         * 为什么用 UPDATE 而不是 INSERT OR REPLACE：
         * SQLite 的 REPLACE 语义是"先 DELETE 旧行再 INSERT 新行"，
         * 而 user_collection 对 pet_id 声明了 ON DELETE CASCADE——
         * REPLACE 会连带删掉用户已有的解锁记录！UPDATE 不触发级联，用户数据安全。
         */
        /**
         * v3 → v4 结构迁移：新增 sync_queue（离线写队列）与 sync_meta（同步状态）两张表。
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
         * v4 → v5 结构迁移：focus_records 新增 content 列（TEXT，默认空串），
         * 老记录原样保留、content 统一为空串，不影响积分与已有统计。
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE focus_records ADD COLUMN content TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /**
         * v5 → v6 结构迁移：focus_records 新增 points 列（本次获得积分，独立于时长）。
         * 回填：老记录 points = 原 duration_minutes，保证历史「积分」不丢；
         * 新记录由 FocusService / 调试工具显式写入 points。
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
                // ---- 普通 ----
                db.execSQL("UPDATE pets SET name='小水滴', emoji='💧', rarity='COMMON', unlock_cost=0, description='最基础的元素精灵' WHERE id=1")
                db.execSQL("UPDATE pets SET name='火苗崽', emoji='🔥', rarity='COMMON', unlock_cost=50, description='充满热情的小家伙' WHERE id=2")
                db.execSQL("UPDATE pets SET name='木灵', emoji='🌿', rarity='COMMON', unlock_cost=100, description='喜欢安静地睡觉' WHERE id=3")
                // ---- 稀有 ----
                db.execSQL("UPDATE pets SET name='星光兽', emoji='⭐', rarity='RARE', unlock_cost=300, description='只在深夜出现' WHERE id=4")
                db.execSQL("UPDATE pets SET name='雷电犬', emoji='⚡', rarity='RARE', unlock_cost=500, description='行动迅捷如闪电' WHERE id=5")
                db.execSQL("UPDATE pets SET name='冰晶狐', emoji='❄️', rarity='RARE', unlock_cost=700, description='高傲的冰雪贵族' WHERE id=6")
                // ---- 传说 ----
                db.execSQL("UPDATE pets SET name='暗影龙', emoji='🌑', rarity='LEGENDARY', unlock_cost=1200, description='拥有毁灭力量' WHERE id=7")
                db.execSQL("UPDATE pets SET name='神圣鹿', emoji='🦌', rarity='LEGENDARY', unlock_cost=1800, description='森林的守护神' WHERE id=8")
                db.execSQL("UPDATE pets SET name='创世神', emoji='🌌', rarity='LEGENDARY', unlock_cost=2500, description='集齐所有图鉴后的终极奖励' WHERE id=9")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focus_pets.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)   // 老版本设备：原地更新宠物数据，保留专注记录
                    .fallbackToDestructiveMigration()   // 兜底：真机上残留的旧版本库与当前 schema 不一致时，直接重建而非崩溃（演示数据可丢弃）
                    .addCallback(SeedCallback)      // 首次建库：写入预置数据
                    .build()
                    .also { INSTANCE = it }
                    .also {
                        // 启动兜底检查：App 启动时检查 pets 表是否为空，为空则插入预置数据。
                        // Room 的 suspend DAO 会等待数据库打开（含 onCreate 执行完）后才真正执行，
                        // 与 SeedCallback 里的 ensureSeeded 并发调用也是幂等的（IGNORE + 已解锁判断）。
                        seedScope.launch { InitialDataProvider.ensureSeeded(it) }
                    }
            }

        /**
         * 首次建库回调：此时 Room 已自动执行完 CREATE TABLE（onCreate 之前的建表阶段），
         * 我们只需做数据 Insert。seed 逻辑统一委托给 InitialDataProvider，保证
         * "首次建库"与"启动兜底检查"两条路径走的是同一份代码。
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
