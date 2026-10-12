package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.entity.CoinTransactionEntity
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
 * DatabaseService manages Cloud Firestore interactions for user profiles,
 * progression tracking, and virtual coin storage.
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
                trySend(null)
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
     * Reads the user profile once from Firestore.
     */
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

    /**
     * Saves or updates a user profile in Firestore.
     */
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

    /**
     * Updates level progression and coin awards atomically in the cloud.
     */
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

    /**
     * Updates virtual coin balance in Firestore.
     */
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

    /**
     * Records a coin transaction in the user's Firestore transaction subcollection.
     */
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
                trySend(emptyList())
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
     * Synchronizes a local Room account to Firestore.
     */
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
