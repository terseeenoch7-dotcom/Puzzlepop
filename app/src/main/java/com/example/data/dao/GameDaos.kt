package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.AdminAuditLogEntity
import com.example.data.entity.AdminConfigEntity
import com.example.data.entity.AnnouncementEntity
import com.example.data.entity.CoinTransactionEntity
import com.example.data.entity.DailyRewardEntity
import com.example.data.entity.LevelEntity
import com.example.data.entity.MissionEntity
import com.example.data.entity.UserAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserFlow(userId: String): Flow<UserAccountEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserAccountEntity?

    @Query("SELECT * FROM users ORDER BY createdAt ASC")
    fun getAllUsersFlow(): Flow<List<UserAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserAccountEntity)

    @Update
    suspend fun update(user: UserAccountEntity)

    @Query("UPDATE users SET totalCoins = totalCoins + :delta WHERE id = :userId")
    suspend fun addCoins(userId: String, delta: Int)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsForUser(userId: String): Flow<List<CoinTransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<CoinTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: CoinTransactionEntity)
}

@Dao
interface LevelDao {
    @Query("SELECT * FROM levels ORDER BY levelNumber ASC")
    fun getAllLevelsFlow(): Flow<List<LevelEntity>>

    @Query("SELECT * FROM levels WHERE levelNumber = :levelNumber LIMIT 1")
    suspend fun getLevel(levelNumber: Int): LevelEntity?

    @Query("SELECT COUNT(*) FROM levels")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(levels: List<LevelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(level: LevelEntity)

    @Update
    suspend fun update(level: LevelEntity)

    @Query("UPDATE levels SET isCompleted = 1, highStars = MAX(highStars, :stars) WHERE levelNumber = :levelNumber")
    suspend fun markCompleted(levelNumber: Int, stars: Int)
}

@Dao
interface DailyRewardDao {
    @Query("SELECT * FROM daily_rewards ORDER BY dayNumber ASC")
    fun getAllDailyRewards(): Flow<List<DailyRewardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rewards: List<DailyRewardEntity>)

    @Query("UPDATE daily_rewards SET isClaimed = 1, claimedTimestamp = :timestamp WHERE dayNumber = :dayNumber")
    suspend fun markClaimed(dayNumber: Int, timestamp: Long)

    @Query("UPDATE daily_rewards SET isClaimed = 0, claimedTimestamp = NULL")
    suspend fun resetAll()
}

@Dao
interface MissionDao {
    @Query("SELECT * FROM missions")
    fun getAllMissionsFlow(): Flow<List<MissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(missions: List<MissionEntity>)

    @Update
    suspend fun update(mission: MissionEntity)
}

@Dao
interface AdminDao {
    @Query("SELECT * FROM admin_configs")
    fun getAllConfigsFlow(): Flow<List<AdminConfigEntity>>

    @Query("SELECT configValue FROM admin_configs WHERE configKey = :key LIMIT 1")
    suspend fun getConfig(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(config: AdminConfigEntity)

    @Query("SELECT * FROM announcements ORDER BY id DESC")
    fun getAnnouncementsFlow(): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAuditLogsFlow(): Flow<List<AdminAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AdminAuditLogEntity)
}
