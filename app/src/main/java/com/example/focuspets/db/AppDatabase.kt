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
import com.example.focuspets.db.entity.WardrobePurchaseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        PetEntity::class,
        UserCollectionEntity::class,
        FocusRecordEntity::class,
        WardrobePurchaseEntity::class
    ],
    version = 3,            // v2 → v3：新增 wardrobe_purchases 表，记录猫咪妆扮消费
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun petDao(): PetDao
    abstract fun userCollectionDao(): UserCollectionDao
    abstract fun focusRecordDao(): FocusRecordDao
    abstract fun wardrobeDao(): WardrobeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val seedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)   // 老版本设备：原地更新宠物数据，保留专注记录
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
                    val database = INSTANCE ?: return@launch
                    InitialDataProvider.ensureSeeded(database)
                }
            }
        }
    }
}
