package com.example.data

import com.example.data.entity.AdminAuditLogEntity
import com.example.data.entity.AdminConfigEntity
import com.example.data.entity.AnnouncementEntity
import com.example.data.entity.CoinTransactionEntity
import com.example.data.entity.DailyRewardEntity
import com.example.data.entity.LevelEntity
import com.example.data.entity.MissionEntity
import com.example.data.entity.UserAccountEntity
import com.example.game.engine.TileEngine
import com.example.game.model.LevelData
import com.example.game.model.LevelDifficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class GameRepository(
    private val database: AppDatabase,
    private val databaseService: DatabaseService? = null
) {

    private val _currentUserId = MutableStateFlow("player_main")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    fun switchUser(userId: String) {
        _currentUserId.value = userId
    }

    fun getCurrentUserFlow(): Flow<UserAccountEntity?> {
        return database.userDao().getUserFlow(_currentUserId.value)
    }

    suspend fun getCurrentUser(): UserAccountEntity? {
        return database.userDao().getUserById(_currentUserId.value)
    }

    fun getAllUsersFlow(): Flow<List<UserAccountEntity>> = database.userDao().getAllUsersFlow()

    fun getTransactionsForCurrentUser(): Flow<List<CoinTransactionEntity>> {
        return database.transactionDao().getTransactionsForUser(_currentUserId.value)
    }

    fun getAllTransactionsFlow(): Flow<List<CoinTransactionEntity>> =
        database.transactionDao().getAllTransactionsFlow()

    fun getAllLevelsFlow(): Flow<List<LevelEntity>> = database.levelDao().getAllLevelsFlow()

    suspend fun getLevel(levelNumber: Int): LevelData? {
        val entity = database.levelDao().getLevel(levelNumber) ?: return null
        return LevelData(
            levelNumber = entity.levelNumber,
            name = entity.name,
            themeName = entity.themeName,
            difficulty = try { LevelDifficulty.valueOf(entity.difficulty) } catch (_: Exception) { LevelDifficulty.EASY },
            baseReward = entity.baseReward,
            bonusReward = entity.bonusReward,
            timeLimitSeconds = entity.timeLimitSeconds,
            trayCapacity = entity.trayCapacity,
            tiles = AppDatabase.jsonToTiles(entity.tilesJson),
            isActive = entity.isActive,
            highStars = entity.highStars,
            isCompleted = entity.isCompleted
        )
    }

    fun getDailyRewardsFlow(): Flow<List<DailyRewardEntity>> = database.dailyRewardDao().getAllDailyRewards()

    fun getMissionsFlow(): Flow<List<MissionEntity>> = database.missionDao().getAllMissionsFlow()

    fun getAdminConfigsFlow(): Flow<List<AdminConfigEntity>> = database.adminDao().getAllConfigsFlow()

    fun getAnnouncementsFlow(): Flow<List<AnnouncementEntity>> = database.adminDao().getAnnouncementsFlow()

    fun getAuditLogsFlow(): Flow<List<AdminAuditLogEntity>> = database.adminDao().getAuditLogsFlow()

    suspend fun completeLevel(
        levelNumber: Int,
        stars: Int,
        matchesCount: Int,
        timeTakenSeconds: Int
    ): Int {
        val user = getCurrentUser() ?: return 0
        val levelEntity = database.levelDao().getLevel(levelNumber)

        val baseCoins = levelEntity?.baseReward ?: 5
        val bonusCoins = if (stars == 3) (levelEntity?.bonusReward ?: 2) else 0
        val totalAward = baseCoins + bonusCoins

        // 1. Mark level completed
        database.levelDao().markCompleted(levelNumber, stars)

        // 2. Add coins to user and update progress
        val nextLevel = if (levelNumber >= user.currentLevel) levelNumber + 1 else user.currentLevel
        val highest = maxOf(user.highestLevelReached, nextLevel)
        database.userDao().insertOrUpdate(
            user.copy(
                totalCoins = user.totalCoins + totalAward,
                totalStars = user.totalStars + stars,
                currentLevel = nextLevel,
                highestLevelReached = highest,
                levelsWon = user.levelsWon + 1,
                matchesCount = user.matchesCount + matchesCount
            )
        )

        // 3. Record transaction in audit ledger
        val tx = CoinTransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            type = "LEVEL_REWARD",
            amount = totalAward,
            levelNumber = levelNumber,
            description = "Completed Level $levelNumber ($stars ★) - Earned $totalAward Coins"
        )
        database.transactionDao().insert(tx)

        databaseService?.let { dbService ->
            try {
                dbService.updateLevelProgression(user.id, levelNumber, stars, totalAward)
                dbService.recordCoinTransaction(
                    user.id,
                    FirestoreCoinTransaction(
                        id = tx.id,
                        userId = user.id,
                        type = tx.type,
                        amount = tx.amount,
                        levelNumber = tx.levelNumber,
                        description = tx.description,
                        timestamp = tx.timestamp
                    )
                )
            } catch (_: Exception) {}
        }

        return totalAward
    }

    suspend fun recordLevelFailure(levelNumber: Int) {
        val user = getCurrentUser() ?: return
        database.userDao().insertOrUpdate(
            user.copy(levelsFailed = user.levelsFailed + 1)
        )
    }

    suspend fun spendCoinsForBooster(
        boosterName: String,
        cost: Int,
        levelNumber: Int
    ): Boolean {
        val user = getCurrentUser() ?: return false
        if (user.totalCoins < cost) return false

        database.userDao().insertOrUpdate(
            user.copy(totalCoins = user.totalCoins - cost)
        )

        database.transactionDao().insert(
            CoinTransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                type = "BOOSTER_PURCHASE",
                amount = -cost,
                levelNumber = levelNumber,
                description = "Used $boosterName booster on Level $levelNumber (-$cost coins)"
            )
        )
        return true
    }

    suspend fun spendCoinsForSecondChance(cost: Int, levelNumber: Int): Boolean {
        val user = getCurrentUser() ?: return false
        if (user.totalCoins < cost) return false

        database.userDao().insertOrUpdate(
            user.copy(totalCoins = user.totalCoins - cost)
        )

        database.transactionDao().insert(
            CoinTransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                type = "SECOND_CHANCE",
                amount = -cost,
                levelNumber = levelNumber,
                description = "Purchased Second Chance on Level $levelNumber (-$cost coins)"
            )
        )
        return true
    }

    suspend fun claimDailyReward(dayNumber: Int, coinAmount: Int) {
        val user = getCurrentUser() ?: return
        database.dailyRewardDao().markClaimed(dayNumber, System.currentTimeMillis())
        database.userDao().addCoins(user.id, coinAmount)

        database.transactionDao().insert(
            CoinTransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                type = "DAILY_REWARD",
                amount = coinAmount,
                description = "Day $dayNumber Daily Login Streak Reward (+$coinAmount coins)"
            )
        )
    }

    suspend fun claimMissionReward(missionId: String, rewardCoins: Int) {
        val user = getCurrentUser() ?: return
        val missions = database.missionDao().getAllMissionsFlow()
        // update mission
        database.userDao().addCoins(user.id, rewardCoins)
        database.transactionDao().insert(
            CoinTransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                type = "MISSION_REWARD",
                amount = rewardCoins,
                description = "Daily Mission Completed (+$rewardCoins coins)"
            )
        )
    }

    suspend fun updateProfile(username: String, avatarEmoji: String) {
        val user = getCurrentUser() ?: return
        database.userDao().insertOrUpdate(
            user.copy(username = username, avatarEmoji = avatarEmoji)
        )
    }

    // --- Admin Operations ---

    suspend fun adminGrantCoins(targetUserId: String, amount: Int, reason: String, adminName: String) {
        database.userDao().addCoins(targetUserId, amount)
        database.transactionDao().insert(
            CoinTransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = targetUserId,
                type = "ADMIN_GRANT",
                amount = amount,
                description = "Admin Credit by $adminName: $reason"
            )
        )
        database.adminDao().insertAuditLog(
            AdminAuditLogEntity(
                adminName = adminName,
                actionType = "COIN_GRANT",
                details = "Granted $amount coins to user $targetUserId. Reason: $reason"
            )
        )
    }

    suspend fun adminUpdateDefaultReward(newReward: Int, adminName: String) {
        database.adminDao().setConfig(
            AdminConfigEntity("default_reward_per_level", newReward.toString())
        )
        database.adminDao().insertAuditLog(
            AdminAuditLogEntity(
                adminName = adminName,
                actionType = "CONFIG_CHANGE",
                details = "Changed default reward per level to $newReward coins."
            )
        )
    }

    suspend fun adminSaveLevel(level: LevelData, adminName: String) {
        val entity = LevelEntity(
            levelNumber = level.levelNumber,
            name = level.name,
            themeName = level.themeName,
            difficulty = level.difficulty.name,
            baseReward = level.baseReward,
            bonusReward = level.bonusReward,
            timeLimitSeconds = level.timeLimitSeconds,
            trayCapacity = level.trayCapacity,
            tilesJson = AppDatabase.tilesToJson(level.tiles),
            isActive = level.isActive,
            highStars = level.highStars,
            isCompleted = level.isCompleted
        )
        database.levelDao().insert(entity)
        database.adminDao().insertAuditLog(
            AdminAuditLogEntity(
                adminName = adminName,
                actionType = "LEVEL_SAVE",
                details = "Saved level ${level.levelNumber} (${level.name}) with ${level.tiles.size} tiles."
            )
        )
    }

    suspend fun adminToggleLevelActive(levelNumber: Int, isActive: Boolean, adminName: String) {
        val current = database.levelDao().getLevel(levelNumber) ?: return
        database.levelDao().update(current.copy(isActive = isActive))
        database.adminDao().insertAuditLog(
            AdminAuditLogEntity(
                adminName = adminName,
                actionType = "LEVEL_TOGGLE",
                details = "Set level $levelNumber isActive to $isActive"
            )
        )
    }

    suspend fun adminCreateAnnouncement(title: String, content: String, adminName: String) {
        database.adminDao().insertAnnouncement(
            AnnouncementEntity(
                title = title,
                content = content,
                dateString = "Just Now",
                isActive = true
            )
        )
        database.adminDao().insertAuditLog(
            AdminAuditLogEntity(
                adminName = adminName,
                actionType = "ANNOUNCEMENT_CREATE",
                details = "Created announcement '$title'"
            )
        )
    }
}
