package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.entity.UserAccountEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Cloud data model for user profiles in Firestore.
 */
data class FirestoreUserProfile(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val avatarEmoji: String = "🐼",
    val role: String = "PLAYER",
    val currentLevel: Int = 1,
    val highestLevelReached: Int = 1,
    val totalCoins: Int = 50,
    val totalStars: Int = 0,
    val matchesCount: Int = 0,
    val levelsWon: Int = 0,
    val levelsFailed: Int = 0,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "email" to email,
            "displayName" to displayName,
            "avatarEmoji" to avatarEmoji,
            "role" to role,
            "currentLevel" to currentLevel,
            "highestLevelReached" to highestLevelReached,
            "totalCoins" to totalCoins,
            "totalStars" to totalStars,
            "matchesCount" to matchesCount,
            "levelsWon" to levelsWon,
            "levelsFailed" to levelsFailed,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

/**
 * Cloud data model for coin transactions in Firestore ledger.
 */
data class FirestoreCoinTransaction(
    val id: String = "",
    val userId: String = "",
    val type: String = "LEVEL_REWARD",
    val amount: Int = 0,
    val levelNumber: Int? = null,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "type" to type,
            "amount" to amount,
            "levelNumber" to levelNumber,
            "description" to description,
            "timestamp" to timestamp
        ).filterValues { it != null }
    }
}

/**
 * Cloud data model for puzzle levels in Firestore.
 */
data class FirestoreLevel(
    val levelNumber: Int = 1,
    val name: String = "",
    val themeName: String = "",
    val difficulty: String = "EASY",
    val baseReward: Int = 5,
    val bonusReward: Int = 2,
    val timeLimitSeconds: Int = 0,
    val trayCapacity: Int = 7,
    val tilesJson: String = "",
    val isActive: Boolean = true,
    val highStars: Int = 0,
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "levelNumber" to levelNumber,
            "name" to name,
            "themeName" to themeName,
            "difficulty" to difficulty,
            "baseReward" to baseReward,
            "bonusReward" to bonusReward,
            "timeLimitSeconds" to timeLimitSeconds,
            "trayCapacity" to trayCapacity,
            "tilesJson" to tilesJson,
            "isActive" to isActive,
            "highStars" to highStars,
            "isCompleted" to isCompleted,
            "updatedAt" to System.currentTimeMillis()
        )
    }
}

/**
 * Cloud data model for in-app broadcast announcements.
 */
data class FirestoreAnnouncement(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val dateString: String = "Just Now",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "Admin"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "content" to content,
            "dateString" to dateString,
            "isActive" to isActive,
            "createdAt" to createdAt,
            "createdBy" to createdBy
        )
    }
}

/**
 * Cloud data model for synchronized admin tasks and daily quests.
 */
data class FirestoreTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val targetProgress: Int = 1,
    val rewardCoins: Int = 10,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "Admin"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "description" to description,
            "targetProgress" to targetProgress,
            "rewardCoins" to rewardCoins,
            "isActive" to isActive,
            "createdAt" to createdAt,
            "createdBy" to createdBy
        )
    }
}

/**
 * Cloud data model for game economy parameters.
 */
data class FirestoreAdminConfig(
    val configKey: String = "",
    val configValue: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "Admin"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "configKey" to configKey,
            "configValue" to configValue,
            "updatedAt" to updatedAt,
            "updatedBy" to updatedBy
        )
    }
}

/**
 * Cloud data model for security and audit trail.
 */
data class FirestoreAuditLog(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val adminName: String = "",
    val actionType: String = "",
    val details: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "timestamp" to timestamp,
            "adminName" to adminName,
            "actionType" to actionType,
            "details" to details
        )
    }
}

/**
 * DatabaseService manages Cloud Firestore interactions with real-time listeners
 * and offline caching for cross-device synchronization.
 */
class DatabaseService(
    val db: FirebaseFirestore
) {
    private val tag = "DatabaseService"

    /**
     * Secondary constructor resolving the custom database ID from strings resource.
     */
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    // ==========================================
    // Real-Time Observers (Snapshot Listeners)
    // ==========================================

    /**
     * Observes real-time user profile data from Firestore.
     */
    fun observeUserProfile(userId: String): Flow<FirestoreUserProfile?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val docRef = db.collection("users").document(userId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Error listening to user profile: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(FirestoreUserProfile::class.java)
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Observes real-time transaction ledger for a user.
     */
    fun observeTransactions(userId: String): Flow<List<FirestoreCoinTransaction>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val query = db.collection("users").document(userId)
            .collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Error listening to transactions: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(FirestoreCoinTransaction::class.java)
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Observes all published levels in Firestore in real-time.
     */
    fun observeLevels(): Flow<List<FirestoreLevel>> = callbackFlow {
        val collection = db.collection("levels")
        val registration = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Error listening to levels: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(FirestoreLevel::class.java)
                    .sortedBy { it.levelNumber }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Observes all in-app announcements in real-time.
     */
    fun observeAnnouncements(): Flow<List<FirestoreAnnouncement>> = callbackFlow {
        val collection = db.collection("announcements")
            .orderBy("createdAt", Query.Direction.DESCENDING)
        val registration = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Error listening to announcements: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(FirestoreAnnouncement::class.java)
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Observes all admin-created tasks in real-time.
     */
    fun observeTasks(): Flow<List<FirestoreTask>> = callbackFlow {
        val collection = db.collection("tasks")
            .orderBy("createdAt", Query.Direction.ASCENDING)
        val registration = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Error listening to tasks: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(FirestoreTask::class.java)
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Observes global game economy configs in real-time.
     */
    fun observeAdminConfigs(): Flow<List<FirestoreAdminConfig>> = callbackFlow {
        val collection = db.collection("admin_configs")
        val registration = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Error listening to admin configs: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(FirestoreAdminConfig::class.java)
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Observes audit log events in real-time (for admin view).
     */
    fun observeAuditLogs(): Flow<List<FirestoreAuditLog>> = callbackFlow {
        val collection = db.collection("audit_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
        val registration = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Error listening to audit logs: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(FirestoreAuditLog::class.java)
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { registration.remove() }
    }

    // ==========================================
    // Cloud Operations & Mutations
    // ==========================================

    suspend fun getUserProfile(userId: String): Result<FirestoreUserProfile?> = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(FirestoreUserProfile::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to get user profile", e)
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(profile: FirestoreUserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            require(profile.userId.isNotBlank()) { "UserId cannot be blank" }
            db.collection("users").document(profile.userId)
                .set(profile.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save user profile", e)
            Result.failure(e)
        }
    }

    suspend fun updateLevelProgression(
        userId: String,
        levelNumber: Int,
        stars: Int,
        coinsEarned: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userRef = db.collection("users").document(userId)
            val current = userRef.get().await().toObject(FirestoreUserProfile::class.java)
                ?: FirestoreUserProfile(userId = userId)

            val nextLvl = if (levelNumber >= current.currentLevel) levelNumber + 1 else current.currentLevel
            val highest = maxOf(current.highestLevelReached, nextLvl)

            val updated = current.copy(
                totalCoins = current.totalCoins + coinsEarned,
                totalStars = current.totalStars + stars,
                currentLevel = nextLvl,
                highestLevelReached = highest,
                levelsWon = current.levelsWon + 1
            )

            userRef.set(updated.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update level progression in cloud", e)
            Result.failure(e)
        }
    }

    suspend fun updateCoinBalance(userId: String, newBalance: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("users").document(userId)
                .update(
                    mapOf(
                        "totalCoins" to newBalance,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update coin balance", e)
            Result.failure(e)
        }
    }

    suspend fun recordCoinTransaction(
        userId: String,
        transaction: FirestoreCoinTransaction
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("users").document(userId)
                .collection("transactions").document(transaction.id)
                .set(transaction.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to record transaction in cloud", e)
            Result.failure(e)
        }
    }

    suspend fun saveLevel(level: FirestoreLevel): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("levels").document(level.levelNumber.toString())
                .set(level.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save level in cloud", e)
            Result.failure(e)
        }
    }

    suspend fun toggleLevelActive(levelNumber: Int, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("levels").document(levelNumber.toString())
                .update(
                    mapOf(
                        "isActive" to isActive,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to toggle level active state in cloud", e)
            Result.failure(e)
        }
    }

    suspend fun saveAnnouncement(announcement: FirestoreAnnouncement): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docId = if (announcement.id.isNotBlank()) announcement.id else "ann_${System.currentTimeMillis()}"
            val toSave = announcement.copy(id = docId)
            db.collection("announcements").document(docId)
                .set(toSave.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save announcement in cloud", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("announcements").document(announcementId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete announcement in cloud", e)
            Result.failure(e)
        }
    }

    suspend fun saveTask(task: FirestoreTask): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docId = if (task.id.isNotBlank()) task.id else "task_${System.currentTimeMillis()}"
            val toSave = task.copy(id = docId)
            db.collection("tasks").document(docId)
                .set(toSave.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save task in cloud", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("tasks").document(taskId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete task in cloud", e)
            Result.failure(e)
        }
    }

    suspend fun saveAdminConfig(key: String, value: String, adminName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val config = FirestoreAdminConfig(
                configKey = key,
                configValue = value,
                updatedAt = System.currentTimeMillis(),
                updatedBy = adminName
            )
            db.collection("admin_configs").document(key)
                .set(config.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save admin config in cloud", e)
            Result.failure(e)
        }
    }

    suspend fun saveAuditLog(log: FirestoreAuditLog): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docId = if (log.id.isNotBlank()) log.id else "audit_${System.currentTimeMillis()}"
            db.collection("audit_logs").document(docId)
                .set(log.copy(id = docId).toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to write audit log in cloud", e)
            Result.failure(e)
        }
    }

    suspend fun syncLocalUserToCloud(localUser: UserAccountEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cloudProfile = FirestoreUserProfile(
                userId = localUser.id,
                email = localUser.email,
                displayName = localUser.username,
                avatarEmoji = localUser.avatarEmoji,
                role = localUser.role,
                currentLevel = localUser.currentLevel,
                highestLevelReached = localUser.highestLevelReached,
                totalCoins = localUser.totalCoins,
                totalStars = localUser.totalStars,
                matchesCount = localUser.matchesCount,
                levelsWon = localUser.levelsWon,
                levelsFailed = localUser.levelsFailed
            )
            saveUserProfile(cloudProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
