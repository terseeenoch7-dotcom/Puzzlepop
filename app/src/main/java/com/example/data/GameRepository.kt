package com.example.data

import android.util.Log
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

data class SyncStatus(
    val isCloudConnected: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncMessage: String = "Cloud synchronization active",
    val lastSyncTime: Long = System.currentTimeMillis()
)

class GameRepository(
    private val database: AppDatabase,
    val databaseService: DatabaseService? = null
) {
    private val tag = "GameRepository"
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentUserId = MutableStateFlow("player_main")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        // Initialize real-time cross-device listeners if DatabaseService is present
        startCloudSyncListeners()
    }

    private fun startCloudSyncListeners() {
        val service = databaseService ?: return

        // 1. Synchronize Levels from Firestore in real-time
        repositoryScope.launch {
            service.observeLevels()
                .catch { e ->
                    Log.w(tag, "Levels listener issue: ${e.message}")
                    _syncStatus.value = _syncStatus.value.copy(isCloudConnected = false)
                }
                .collect { cloudLevels ->
                    if (cloudLevels.isNotEmpty()) {
                        val entities = cloudLevels.map { cl ->
                            LevelEntity(
                                levelNumber = cl.levelNumber,
                                name = cl.name,
                                themeName = cl.themeName,
                                difficulty = cl.difficulty,
                                baseReward = cl.baseReward,
                                bonusReward = cl.bonusReward,
                                timeLimitSeconds = cl.timeLimitSeconds,
                                trayCapacity = cl.trayCapacity,
                                tilesJson = cl.tilesJson,
                                isActive = cl.isActive,
                                highStars = cl.highStars,
                                isCompleted = cl.isCompleted
                            )
                        }
                        database.levelDao().insertAll(entities)
                        _syncStatus.value = _syncStatus.value.copy(
                            isCloudConnected = true,
                            lastSyncMessage = "Synced ${entities.size} levels from cloud",
                            lastSyncTime = System.currentTimeMillis()
                        )
                    }
                }
        }

        // 2. Synchronize Announcements from Firestore in real-time
        repositoryScope.launch {
            service.observeAnnouncements()
                .catch { e -> Log.w(tag, "Announcements listener issue: ${e.message}") }
                .collect { cloudAnnouncements ->
                    if (cloudAnnouncements.isNotEmpty()) {
                        cloudAnnouncements.forEach { ca ->
                            val entity = AnnouncementEntity(
                                title = ca.title,
                                content = ca.content,
                                dateString = ca.dateString,
                                isActive = ca.isActive
                            )
                            database.adminDao().insertAnnouncement(entity)
                        }
                        _syncStatus.value = _syncStatus.value.copy(
                            isCloudConnected = true,
                            lastSyncMessage = "Announcements updated",
                            lastSyncTime = System.currentTimeMillis()
                        )
                    }
                }
        }

        // 3. Synchronize Tasks/Missions from Firestore in real-time
        repositoryScope.launch {
            service.observeTasks()
                .catch { e -> Log.w(tag, "Tasks listener issue: ${e.message}") }
                .collect { cloudTasks ->
                    if (cloudTasks.isNotEmpty()) {
                        val entities = cloudTasks.map { ct ->
                            MissionEntity(
                                id = ct.id,
                                title = ct.title,
                                description = ct.description,
                                currentProgress = 0,
                                targetProgress = ct.targetProgress,
                                rewardCoins = ct.rewardCoins,
                                isClaimed = false
                            )
                        }
                        database.missionDao().insertAll(entities)
                        _syncStatus.value = _syncStatus.value.copy(
                            isCloudConnected = true,
                            lastSyncMessage = "Synced ${entities.size} tasks from cloud",
                            lastSyncTime = System.currentTimeMillis()
                        )
                    }
                }
        }

        // 4. Synchronize Admin Configs from Firestore in real-time
        repositoryScope.launch {
            service.observeAdminConfigs()
                .catch { e -> Log.w(tag, "Configs listener issue: ${e.message}") }
                .collect { configs ->
                    configs.forEach { cfg ->
                        database.adminDao().setConfig(
                            AdminConfigEntity(cfg.configKey, cfg.configValue)
                        )
                    }
                }
        }

        // 5. Synchronize Audit Logs from Firestore in real-time
        repositoryScope.launch {
            service.observeAuditLogs()
                .catch { e -> Log.w(tag, "Audit logs listener issue: ${e.message}") }
                .collect { logs ->
                    logs.forEach { log ->
                        database.adminDao().insertAuditLog(
                            AdminAuditLogEntity(
                                timestamp = log.timestamp,
                                adminName = log.adminName,
                                actionType = log.actionType,
                                details = log.details
                            )
                        )
                    }
                }
        }
    }

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

        // 1. Mark level completed in local cache
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

        // 4. Synchronize to Firestore cloud
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
            } catch (e: Exception) {
                Log.w(tag, "Deferred cloud sync: ${e.message}")
            }
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

        val updatedCoins = user.totalCoins - cost
        database.userDao().insertOrUpdate(user.copy(totalCoins = updatedCoins))

        val tx = CoinTransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            type = "BOOSTER_PURCHASE",
            amount = -cost,
            levelNumber = levelNumber,
            description = "Used $boosterName booster on Level $levelNumber (-$cost coins)"
        )
        database.transactionDao().insert(tx)

        databaseService?.let { dbService ->
            try {
                dbService.updateCoinBalance(user.id, updatedCoins)
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
            } catch (e: Exception) {
                Log.w(tag, "Deferred booster spend sync: ${e.message}")
            }
        }
        return true
    }

    suspend fun spendCoinsForSecondChance(cost: Int, levelNumber: Int): Boolean {
        val user = getCurrentUser() ?: return false
        if (user.totalCoins < cost) return false

        val updatedCoins = user.totalCoins - cost
        database.userDao().insertOrUpdate(user.copy(totalCoins = updatedCoins))

        val tx = CoinTransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            type = "SECOND_CHANCE",
            amount = -cost,
            levelNumber = levelNumber,
            description = "Purchased Second Chance on Level $levelNumber (-$cost coins)"
        )
        database.transactionDao().insert(tx)

        databaseService?.let { dbService ->
            try {
                dbService.updateCoinBalance(user.id, updatedCoins)
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
            } catch (e: Exception) {
                Log.w(tag, "Deferred second chance sync: ${e.message}")
            }
        }
        return true
    }

    suspend fun claimDailyReward(dayNumber: Int, coinAmount: Int) {
        val user = getCurrentUser() ?: return
        database.dailyRewardDao().markClaimed(dayNumber, System.currentTimeMillis())
        database.userDao().addCoins(user.id, coinAmount)

        val tx = CoinTransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            type = "DAILY_REWARD",
            amount = coinAmount,
            description = "Day $dayNumber Daily Login Streak Reward (+$coinAmount coins)"
        )
        database.transactionDao().insert(tx)

        databaseService?.let { dbService ->
            try {
                val newBal = user.totalCoins + coinAmount
                dbService.updateCoinBalance(user.id, newBal)
                dbService.recordCoinTransaction(
                    user.id,
                    FirestoreCoinTransaction(
                        id = tx.id,
                        userId = user.id,
                        type = tx.type,
                        amount = tx.amount,
                        description = tx.description,
                        timestamp = tx.timestamp
                    )
                )
            } catch (e: Exception) {
                Log.w(tag, "Deferred daily reward sync: ${e.message}")
            }
        }
    }

    suspend fun claimMissionReward(missionId: String, rewardCoins: Int) {
        val user = getCurrentUser() ?: return
        database.userDao().addCoins(user.id, rewardCoins)
        val tx = CoinTransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            type = "MISSION_REWARD",
            amount = rewardCoins,
            description = "Daily Mission Completed (+$rewardCoins coins)"
        )
        database.transactionDao().insert(tx)

        databaseService?.let { dbService ->
            try {
                val newBal = user.totalCoins + rewardCoins
                dbService.updateCoinBalance(user.id, newBal)
                dbService.recordCoinTransaction(
                    user.id,
                    FirestoreCoinTransaction(
                        id = tx.id,
                        userId = user.id,
                        type = tx.type,
                        amount = tx.amount,
                        description = tx.description,
                        timestamp = tx.timestamp
                    )
                )
            } catch (e: Exception) {
                Log.w(tag, "Deferred mission reward sync: ${e.message}")
            }
        }
    }

    suspend fun updateProfile(username: String, avatarEmoji: String) {
        val user = getCurrentUser() ?: return
        val updated = user.copy(username = username, avatarEmoji = avatarEmoji)
        database.userDao().insertOrUpdate(updated)

        databaseService?.let { dbService ->
            try {
                dbService.syncLocalUserToCloud(updated)
            } catch (e: Exception) {
                Log.w(tag, "Deferred profile sync: ${e.message}")
            }
        }
    }

    // =========================================================================
    // Admin Operations with Real-Time Firestore Cloud Synchronization
    // =========================================================================

    suspend fun adminSaveLevel(level: LevelData, adminName: String): Result<String> {
        val tilesJson = AppDatabase.tilesToJson(level.tiles)
        val entity = LevelEntity(
            levelNumber = level.levelNumber,
            name = level.name,
            themeName = level.themeName,
            difficulty = level.difficulty.name,
            baseReward = level.baseReward,
            bonusReward = level.bonusReward,
            timeLimitSeconds = level.timeLimitSeconds,
            trayCapacity = level.trayCapacity,
            tilesJson = tilesJson,
            isActive = level.isActive,
            highStars = level.highStars,
            isCompleted = level.isCompleted
        )

        // 1. Update local Room database
        database.levelDao().insert(entity)

        val audit = AdminAuditLogEntity(
            adminName = adminName,
            actionType = "LEVEL_SAVE",
            details = "Saved level ${level.levelNumber} (${level.name}) with ${level.tiles.size} tiles."
        )
        database.adminDao().insertAuditLog(audit)

        // 2. Synchronize to Firestore for cross-device broadcast
        var cloudMessage = "Saved locally"
        databaseService?.let { service ->
            val cloudLevel = FirestoreLevel(
                levelNumber = level.levelNumber,
                name = level.name,
                themeName = level.themeName,
                difficulty = level.difficulty.name,
                baseReward = level.baseReward,
                bonusReward = level.bonusReward,
                timeLimitSeconds = level.timeLimitSeconds,
                trayCapacity = level.trayCapacity,
                tilesJson = tilesJson,
                isActive = level.isActive,
                highStars = level.highStars,
                isCompleted = level.isCompleted
            )
            val result = service.saveLevel(cloudLevel)
            if (result.isSuccess) {
                cloudMessage = "Level ${level.levelNumber} saved and synchronized to Cloud ☁️"
                service.saveAuditLog(
                    FirestoreAuditLog(
                        id = UUID.randomUUID().toString(),
                        timestamp = audit.timestamp,
                        adminName = adminName,
                        actionType = audit.actionType,
                        details = audit.details
                    )
                )
            } else {
                cloudMessage = "Saved locally; Cloud sync queued: ${result.exceptionOrNull()?.message}"
            }
        }

        return Result.success(cloudMessage)
    }

    suspend fun adminToggleLevelActive(levelNumber: Int, isActive: Boolean, adminName: String): Result<String> {
        val current = database.levelDao().getLevel(levelNumber)
            ?: return Result.failure(IllegalArgumentException("Level not found"))

        database.levelDao().update(current.copy(isActive = isActive))
        val audit = AdminAuditLogEntity(
            adminName = adminName,
            actionType = "LEVEL_TOGGLE",
            details = "Set level $levelNumber isActive to $isActive"
        )
        database.adminDao().insertAuditLog(audit)

        var cloudMsg = "Updated locally"
        databaseService?.let { service ->
            val result = service.toggleLevelActive(levelNumber, isActive)
            if (result.isSuccess) {
                cloudMsg = "Level $levelNumber status synced to Cloud ☁️"
                service.saveAuditLog(
                    FirestoreAuditLog(
                        id = UUID.randomUUID().toString(),
                        timestamp = audit.timestamp,
                        adminName = adminName,
                        actionType = audit.actionType,
                        details = audit.details
                    )
                )
            } else {
                cloudMsg = "Updated locally; Cloud sync queued: ${result.exceptionOrNull()?.message}"
            }
        }
        return Result.success(cloudMsg)
    }

    suspend fun adminCreateAnnouncement(title: String, content: String, adminName: String): Result<String> {
        val cloudId = "ann_${UUID.randomUUID().toString().take(8)}"
        val entity = AnnouncementEntity(
            title = title,
            content = content,
            dateString = "Just Now",
            isActive = true
        )
        database.adminDao().insertAnnouncement(entity)

        val audit = AdminAuditLogEntity(
            adminName = adminName,
            actionType = "ANNOUNCEMENT_CREATE",
            details = "Created announcement '$title'"
        )
        database.adminDao().insertAuditLog(audit)

        var cloudMsg = "Announcement published locally"
        databaseService?.let { service ->
            val cloudAnnouncement = FirestoreAnnouncement(
                id = cloudId,
                title = title,
                content = content,
                dateString = "Just Now",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                createdBy = adminName
            )
            val result = service.saveAnnouncement(cloudAnnouncement)
            if (result.isSuccess) {
                cloudMsg = "Announcement published and broadcast across all devices ☁️"
                service.saveAuditLog(
                    FirestoreAuditLog(
                        id = UUID.randomUUID().toString(),
                        timestamp = audit.timestamp,
                        adminName = adminName,
                        actionType = audit.actionType,
                        details = audit.details
                    )
                )
            } else {
                cloudMsg = "Saved locally; Cloud sync pending: ${result.exceptionOrNull()?.message}"
            }
        }
        return Result.success(cloudMsg)
    }

    suspend fun adminDeleteAnnouncement(title: String, adminName: String): Result<String> {
        database.adminDao().deleteAnnouncementByTitle(title)
        val audit = AdminAuditLogEntity(
            adminName = adminName,
            actionType = "ANNOUNCEMENT_DELETE",
            details = "Deleted announcement '$title'"
        )
        database.adminDao().insertAuditLog(audit)
        return Result.success("Announcement deleted")
    }

    // --- Admin Tasks / Quests Synchronization ---

    suspend fun adminCreateTask(
        title: String,
        description: String,
        targetProgress: Int,
        rewardCoins: Int,
        adminName: String
    ): Result<String> {
        val taskId = "task_${UUID.randomUUID().toString().take(8)}"
        val entity = MissionEntity(
            id = taskId,
            title = title,
            description = description,
            currentProgress = 0,
            targetProgress = targetProgress,
            rewardCoins = rewardCoins,
            isClaimed = false
        )
        database.missionDao().insert(entity)

        val audit = AdminAuditLogEntity(
            adminName = adminName,
            actionType = "TASK_CREATE",
            details = "Created task '$title' with target $targetProgress and $rewardCoins coins."
        )
        database.adminDao().insertAuditLog(audit)

        var cloudMsg = "Task created locally"
        databaseService?.let { service ->
            val cloudTask = FirestoreTask(
                id = taskId,
                title = title,
                description = description,
                targetProgress = targetProgress,
                rewardCoins = rewardCoins,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                createdBy = adminName
            )
            val result = service.saveTask(cloudTask)
            if (result.isSuccess) {
                cloudMsg = "Task '$title' created and synchronized across all player devices ☁️"
                service.saveAuditLog(
                    FirestoreAuditLog(
                        id = UUID.randomUUID().toString(),
                        timestamp = audit.timestamp,
                        adminName = adminName,
                        actionType = audit.actionType,
                        details = audit.details
                    )
                )
            } else {
                cloudMsg = "Task saved locally; Cloud sync queued: ${result.exceptionOrNull()?.message}"
            }
        }
        return Result.success(cloudMsg)
    }

    suspend fun adminDeleteTask(taskId: String, adminName: String): Result<String> {
        database.missionDao().deleteById(taskId)
        val audit = AdminAuditLogEntity(
            adminName = adminName,
            actionType = "TASK_DELETE",
            details = "Deleted task with ID $taskId"
        )
        database.adminDao().insertAuditLog(audit)

        databaseService?.let { service ->
            service.deleteTask(taskId)
            service.saveAuditLog(
                FirestoreAuditLog(
                    id = UUID.randomUUID().toString(),
                    timestamp = audit.timestamp,
                    adminName = adminName,
                    actionType = audit.actionType,
                    details = audit.details
                )
            )
        }
        return Result.success("Task deleted and removed from all devices ☁️")
    }

    suspend fun adminGrantCoins(
        targetUserId: String,
        amount: Int,
        reason: String,
        adminName: String
    ): Result<String> {
        database.userDao().addCoins(targetUserId, amount)
        val tx = CoinTransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = targetUserId,
            type = "ADMIN_GRANT",
            amount = amount,
            description = "Admin Credit by $adminName: $reason"
        )
        database.transactionDao().insert(tx)

        val audit = AdminAuditLogEntity(
            adminName = adminName,
            actionType = "COIN_GRANT",
            details = "Granted $amount coins to user $targetUserId. Reason: $reason"
        )
        database.adminDao().insertAuditLog(audit)

        databaseService?.let { service ->
            try {
                service.recordCoinTransaction(
                    targetUserId,
                    FirestoreCoinTransaction(
                        id = tx.id,
                        userId = targetUserId,
                        type = tx.type,
                        amount = tx.amount,
                        description = tx.description,
                        timestamp = tx.timestamp
                    )
                )
                val target = database.userDao().getUserById(targetUserId)
                if (target != null) {
                    service.updateCoinBalance(targetUserId, target.totalCoins)
                }
            } catch (e: Exception) {
                Log.w(tag, "Deferred admin grant cloud sync: ${e.message}")
            }
        }
        return Result.success("Granted $amount coins to user $targetUserId")
    }

    suspend fun adminUpdateDefaultReward(newReward: Int, adminName: String): Result<String> {
        database.adminDao().setConfig(
            AdminConfigEntity("default_reward_per_level", newReward.toString())
        )
        val audit = AdminAuditLogEntity(
            adminName = adminName,
            actionType = "CONFIG_CHANGE",
            details = "Changed default reward per level to $newReward coins."
        )
        database.adminDao().insertAuditLog(audit)

        var cloudMsg = "Updated locally"
        databaseService?.let { service ->
            val result = service.saveAdminConfig("default_reward_per_level", newReward.toString(), adminName)
            if (result.isSuccess) {
                cloudMsg = "Default reward updated to $newReward coins across all devices ☁️"
            } else {
                cloudMsg = "Updated locally; Cloud sync queued: ${result.exceptionOrNull()?.message}"
            }
        }
        return Result.success(cloudMsg)
    }

    /**
     * Seeds initial catalog of levels and tasks to Firestore if they do not yet exist in the cloud.
     */
    suspend fun seedInitialDataToCloud(adminName: String): Result<String> {
        val service = databaseService ?: return Result.failure(IllegalStateException("No Cloud Service available"))
        val localLevels = database.levelDao().getAllLevelsFlow()
        val localMissions = database.missionDao().getAllMissionsFlow()

        // Read snapshot of levels
        val count = database.levelDao().getCount()
        if (count == 0) {
            return Result.failure(IllegalStateException("No local levels to seed"))
        }

        var uploaded = 0
        for (i in 1..count) {
            val lvl = database.levelDao().getLevel(i) ?: continue
            val cloudLvl = FirestoreLevel(
                levelNumber = lvl.levelNumber,
                name = lvl.name,
                themeName = lvl.themeName,
                difficulty = lvl.difficulty,
                baseReward = lvl.baseReward,
                bonusReward = lvl.bonusReward,
                timeLimitSeconds = lvl.timeLimitSeconds,
                trayCapacity = lvl.trayCapacity,
                tilesJson = lvl.tilesJson,
                isActive = lvl.isActive,
                highStars = lvl.highStars,
                isCompleted = lvl.isCompleted
            )
            service.saveLevel(cloudLvl)
            uploaded++
        }

        service.saveAuditLog(
            FirestoreAuditLog(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                adminName = adminName,
                actionType = "CLOUD_SEED",
                details = "Seeded $uploaded catalog levels to Firestore central database."
            )
        )

        return Result.success("Successfully seeded $uploaded levels to Firestore cloud database!")
    }
}
