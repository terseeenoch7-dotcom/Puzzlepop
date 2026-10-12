package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserAccountEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val avatarEmoji: String = "🐼",
    val role: String = "PLAYER", // "PLAYER" or "SUPER_ADMIN"
    val currentLevel: Int = 1,
    val highestLevelReached: Int = 1,
    val totalCoins: Int = 50,
    val totalStars: Int = 0,
    val matchesCount: Int = 0,
    val levelsWon: Int = 0,
    val levelsFailed: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class CoinTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // "LEVEL_REWARD", "DAILY_REWARD", "BOOSTER_PURCHASE", "ADMIN_GRANT", "MISSION_REWARD", "SECOND_CHANCE"
    val amount: Int,  // positive for credit, negative for debit
    val timestamp: Long = System.currentTimeMillis(),
    val levelNumber: Int? = null,
    val status: String = "CONFIRMED",
    val description: String
)

@Entity(tableName = "levels")
data class LevelEntity(
    @PrimaryKey val levelNumber: Int,
    val name: String,
    val themeName: String,
    val difficulty: String,
    val baseReward: Int = 5,
    val bonusReward: Int = 2,
    val timeLimitSeconds: Int = 0,
    val trayCapacity: Int = 7,
    val tilesJson: String,
    val isActive: Boolean = true,
    val highStars: Int = 0,
    val isCompleted: Boolean = false
)

@Entity(tableName = "daily_rewards")
data class DailyRewardEntity(
    @PrimaryKey val dayNumber: Int, // 1 to 7
    val coinAmount: Int,
    val boosterType: String? = null,
    val isClaimed: Boolean = false,
    val claimedTimestamp: Long? = null
)

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val currentProgress: Int = 0,
    val targetProgress: Int = 1,
    val rewardCoins: Int = 10,
    val isClaimed: Boolean = false
)

@Entity(tableName = "admin_configs")
data class AdminConfigEntity(
    @PrimaryKey val configKey: String,
    val configValue: String
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val dateString: String,
    val isActive: Boolean = true
)

@Entity(tableName = "audit_logs")
data class AdminAuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val adminName: String,
    val actionType: String,
    val details: String
)
