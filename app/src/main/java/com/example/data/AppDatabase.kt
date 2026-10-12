package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AdminDao
import com.example.data.dao.DailyRewardDao
import com.example.data.dao.LevelDao
import com.example.data.dao.MissionDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.UserDao
import com.example.data.entity.AdminAuditLogEntity
import com.example.data.entity.AdminConfigEntity
import com.example.data.entity.AnnouncementEntity
import com.example.data.entity.CoinTransactionEntity
import com.example.data.entity.DailyRewardEntity
import com.example.data.entity.LevelEntity
import com.example.data.entity.MissionEntity
import com.example.data.entity.UserAccountEntity
import com.example.game.engine.TileEngine
import com.example.game.model.LevelDifficulty
import com.example.game.model.TileConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Database(
    entities = [
        UserAccountEntity::class,
        CoinTransactionEntity::class,
        LevelEntity::class,
        DailyRewardEntity::class,
        MissionEntity::class,
        AdminConfigEntity::class,
        AnnouncementEntity::class,
        AdminAuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun levelDao(): LevelDao
    abstract fun dailyRewardDao(): DailyRewardDao
    abstract fun missionDao(): MissionDao
    abstract fun adminDao(): AdminDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "puzzlepop.db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun tilesToJson(tiles: List<TileConfig>): String {
            val array = JSONArray()
            tiles.forEach { tile ->
                val obj = JSONObject()
                obj.put("type", tile.typeName)
                obj.put("layer", tile.layer)
                obj.put("x", tile.x.toDouble())
                obj.put("y", tile.y.toDouble())
                array.put(obj)
            }
            return array.toString()
        }

        fun jsonToTiles(json: String): List<TileConfig> {
            val list = mutableListOf<TileConfig>()
            if (json.isBlank()) return list
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        TileConfig(
                            typeName = obj.getString("type"),
                            layer = obj.getInt("layer"),
                            x = obj.getDouble("x").toFloat(),
                            y = obj.getDouble("y").toFloat()
                        )
                    )
                }
            } catch (_: Exception) {}
            return list
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = INSTANCE ?: return@launch
                seedInitialData(database)
            }
        }

        private suspend fun seedInitialData(database: AppDatabase) {
            // 1. Initial Player & Admin
            val defaultPlayer = UserAccountEntity(
                id = "player_main",
                username = "PopMaster",
                email = "player@puzzlepop.com",
                avatarEmoji = "🐼",
                role = "PLAYER",
                currentLevel = 1,
                highestLevelReached = 1,
                totalCoins = 50,
                totalStars = 0,
                matchesCount = 0,
                levelsWon = 0,
                levelsFailed = 0
            )
            val adminUser = UserAccountEntity(
                id = "admin_super",
                username = "Administrator",
                email = "admin@puzzlepop.com",
                avatarEmoji = "👑",
                role = "SUPER_ADMIN",
                currentLevel = 25,
                highestLevelReached = 25,
                totalCoins = 9999,
                totalStars = 75,
                matchesCount = 350,
                levelsWon = 25,
                levelsFailed = 2
            )
            database.userDao().insertOrUpdate(defaultPlayer)
            database.userDao().insertOrUpdate(adminUser)

            // Initial transaction for starting balance
            database.transactionDao().insert(
                CoinTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    userId = "player_main",
                    type = "WELCOME_BONUS",
                    amount = 50,
                    description = "Welcome to PuzzlePop! Starting Coin Gift 🎉"
                )
            )

            // 2. Initial 30 Predefined Levels
            val levels = mutableListOf<LevelEntity>()
            val themes = listOf(
                "Sunny Orchard", "Berry Meadow", "Sweet Treats", "Candy Wonderland",
                "Starry Garden", "Magic Forest", "Panda's Bamboo", "Gemstone Mine",
                "Tropical Reef", "Cloud Kingdom"
            )

            for (i in 1..30) {
                val theme = themes[(i - 1) % themes.size]
                val diff = when {
                    i <= 3 -> LevelDifficulty.EASY
                    i <= 10 -> LevelDifficulty.MEDIUM
                    i <= 20 -> LevelDifficulty.HARD
                    else -> LevelDifficulty.EXPERT
                }
                val generated = TileEngine.generateLevel(i, theme, diff, baseReward = 5)
                levels.add(
                    LevelEntity(
                        levelNumber = i,
                        name = generated.name,
                        themeName = theme,
                        difficulty = diff.name,
                        baseReward = 5,
                        bonusReward = generated.bonusReward,
                        timeLimitSeconds = generated.timeLimitSeconds,
                        trayCapacity = generated.trayCapacity,
                        tilesJson = tilesToJson(generated.tiles),
                        isActive = true,
                        highStars = 0,
                        isCompleted = false
                    )
                )
            }
            database.levelDao().insertAll(levels)

            // 3. 7-Day Daily Rewards
            val dailyRewards = listOf(
                DailyRewardEntity(1, coinAmount = 10, boosterType = null),
                DailyRewardEntity(2, coinAmount = 15, boosterType = "HINT"),
                DailyRewardEntity(3, coinAmount = 20, boosterType = null),
                DailyRewardEntity(4, coinAmount = 25, boosterType = "SHUFFLE"),
                DailyRewardEntity(5, coinAmount = 30, boosterType = "UNDO"),
                DailyRewardEntity(6, coinAmount = 40, boosterType = "EXTRA_SLOT"),
                DailyRewardEntity(7, coinAmount = 100, boosterType = "MYSTERY_CHEST")
            )
            database.dailyRewardDao().insertAll(dailyRewards)

            // 4. Daily Missions
            val missions = listOf(
                MissionEntity("m1", "First Pop", "Make your first triple tile match", 0, 1, 10),
                MissionEntity("m2", "Fruit Collector", "Match 9 Fruit tiles", 0, 9, 15),
                MissionEntity("m3", "Level Conqueror", "Beat 2 puzzle levels", 0, 2, 20),
                MissionEntity("m4", "Booster Tactician", "Use any booster in a level", 0, 1, 10)
            )
            database.missionDao().insertAll(missions)

            // 5. Admin Configurations (Default 5 coins reward)
            val configs = listOf(
                AdminConfigEntity("default_reward_per_level", "5"),
                AdminConfigEntity("hint_cost", "15"),
                AdminConfigEntity("shuffle_cost", "20"),
                AdminConfigEntity("undo_cost", "10"),
                AdminConfigEntity("extra_slot_cost", "25"),
                AdminConfigEntity("second_chance_cost", "30"),
                AdminConfigEntity("max_tray_slots", "7")
            )
            configs.forEach { database.adminDao().setConfig(it) }

            // 6. Announcements
            database.adminDao().insertAnnouncement(
                AnnouncementEntity(
                    title = "Welcome to PuzzlePop Season 1! 🎈",
                    content = "Enjoy 30 exciting handcrafted levels, daily streak rewards, and triple-matching fun! Check the Admin Dashboard to test custom levels.",
                    dateString = "Season Opening",
                    isActive = true
                )
            )

            // 7. Initial Audit Log
            database.adminDao().insertAuditLog(
                AdminAuditLogEntity(
                    adminName = "System",
                    actionType = "SYSTEM_INITIALIZED",
                    details = "Initialized database with 30 levels, 7-day streak calendar, and default economy."
                )
            )
        }
    }
}
