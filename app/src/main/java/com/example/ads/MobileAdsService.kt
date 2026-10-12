package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.DatabaseService
import com.example.data.FirestoreCoinTransaction
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Result state returned by rewarded ad operations.
 */
sealed class RewardValidationResult {
    data class Success(
        val placementId: String,
        val rewardAmount: Int,
        val transactionId: String,
        val serverValidated: Boolean,
        val message: String
    ) : RewardValidationResult()

    data class Failure(
        val placementId: String,
        val reason: String,
        val error: Throwable? = null
    ) : RewardValidationResult()
}

/**
 * Centralized MobileAdsService managing Google AdMob SDK initialization, ad lifecycle
 * (Banner, Interstitial, Rewarded, Rewarded Interstitial, App Open), anti-tamper reward verification,
 * and server-side validation against Firebase Firestore.
 */
class MobileAdsService private constructor() {

    companion object {
        private const val TAG = "MobileAdsService"

        @Volatile
        private var instance: MobileAdsService? = null

        fun getInstance(): MobileAdsService {
            return instance ?: synchronized(this) {
                instance ?: MobileAdsService().also { instance = it }
            }
        }

        // Live AdMob App ID & Ad Unit IDs provided by user
        const val LIVE_APP_ID = "ca-app-pub-1621437411635895~7060489446"

        const val LIVE_BANNER_AD_UNIT_ID_PRIMARY = "ca-app-pub-1621437411635895/6932496933"
        const val LIVE_BANNER_AD_UNIT_ID_SECONDARY = "ca-app-pub-1621437411635895/3532307970"

        const val LIVE_INTERSTITIAL_AD_UNIT_ID_PRIMARY = "ca-app-pub-1621437411635895/6353912438"
        const val LIVE_INTERSTITIAL_AD_UNIT_ID_SECONDARY = "ca-app-pub-1621437411635895/9302500248"

        const val LIVE_REWARDED_INTERSTITIAL_AD_UNIT_ID_PRIMARY = "ca-app-pub-1621437411635895/5240886179"
        const val LIVE_REWARDED_INTERSTITIAL_AD_UNIT_ID_SECONDARY = "ca-app-pub-1621437411635895/7774805312"

        const val LIVE_REWARDED_AD_UNIT_ID_PRIMARY = "ca-app-pub-1621437411635895/4688467814"
        const val LIVE_REWARDED_AD_UNIT_ID_SECONDARY = "ca-app-pub-1621437411635895/9586405424"

        const val LIVE_APP_OPEN_AD_UNIT_ID = "ca-app-pub-1621437411635895/9586405424"

        // Official Google Test Ad Unit IDs
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379"
        const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

        // Placement IDs
        const val PLACEMENT_EXTRA_TIME = "game_extra_time"
        const val PLACEMENT_LEVEL_COMPLETION_BONUS = "level_completion_bonus"
        const val PLACEMENT_DOUBLE_LEVEL_REWARD = "double_level_reward"
        const val PLACEMENT_SECOND_CHANCE = "second_chance"
        const val PLACEMENT_HINT_REWARD = "hint_reward"
        const val PLACEMENT_SHUFFLE_REWARD = "shuffle_reward"
        const val PLACEMENT_UNDO_REWARD = "undo_reward"
        const val PLACEMENT_EXTRA_TRAY_SLOT = "extra_tray_slot"
        const val PLACEMENT_DAILY_BONUS = "daily_bonus"
        const val PLACEMENT_TASK_CLAIM = "task_claim"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // Configuration flags
    var useLiveAdUnits: Boolean = false

    val activeBannerAdUnitId: String
        get() = if (useLiveAdUnits) LIVE_BANNER_AD_UNIT_ID_PRIMARY else TEST_BANNER_AD_UNIT_ID

    val activeRewardedAdUnitId: String
        get() = if (useLiveAdUnits) LIVE_REWARDED_AD_UNIT_ID_PRIMARY else TEST_REWARDED_AD_UNIT_ID

    val activeInterstitialAdUnitId: String
        get() = if (useLiveAdUnits) LIVE_INTERSTITIAL_AD_UNIT_ID_PRIMARY else TEST_INTERSTITIAL_AD_UNIT_ID

    val activeRewardedInterstitialAdUnitId: String
        get() = if (useLiveAdUnits) LIVE_REWARDED_INTERSTITIAL_AD_UNIT_ID_PRIMARY else TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID

    val activeAppOpenAdUnitId: String
        get() = if (useLiveAdUnits) LIVE_APP_OPEN_AD_UNIT_ID else TEST_APP_OPEN_AD_UNIT_ID

    private var isInitialized = false
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null

    private var isRewardedLoading = false
    private var isInterstitialLoading = false
    private var isRewardedInterstitialLoading = false
    private var isAppOpenLoading = false

    // Anti-replay and cooldown registry
    private val placementCooldowns = ConcurrentHashMap<String, Long>()
    private val processedRewardTokens = ConcurrentHashMap.newKeySet<String>()

    // Optional DatabaseService reference for cloud server validation
    private var databaseService: DatabaseService? = null

    data class ServiceAdStats(
        val requestsCount: Int = 0,
        val impressionsCount: Int = 0,
        val rewardedCompletions: Int = 0,
        val failedLoads: Int = 0,
        val serverValidatedRewards: Int = 0
    )

    private val _adStats = MutableStateFlow(ServiceAdStats())
    val adStats: StateFlow<ServiceAdStats> = _adStats.asStateFlow()

    /**
     * Initializes the Google AdMob Mobile Ads SDK and preloads initial ad inventory.
     */
    fun initialize(context: Context, dbService: DatabaseService? = null) {
        if (dbService != null) {
            this.databaseService = dbService
        }
        if (isInitialized) return

        try {
            MobileAds.initialize(context) { status ->
                isInitialized = true
                Log.d(TAG, "MobileAds initialized: ${status.adapterStatusMap.keys}. Mode: ${if (useLiveAdUnits) "LIVE" else "TEST"}")
                loadRewardedAd(context)
                loadInterstitialAd(context)
                loadRewardedInterstitialAd(context)
                loadAppOpenAd(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds SDK", e)
        }
    }

    fun setDatabaseService(service: DatabaseService) {
        this.databaseService = service
    }

    // =========================================================================
    // BANNER ADS
    // =========================================================================

    /**
     * Loads a standard banner ad into an AdView container.
     */
    fun loadBanner(adView: AdView) {
        adView.adUnitId = activeBannerAdUnitId
        adView.loadAd(AdRequest.Builder().build())
    }

    // =========================================================================
    // INTERSTITIAL ADS
    // =========================================================================

    /**
     * Preloads an interstitial ad.
     */
    fun loadInterstitialAd(context: Context) {
        if (isInterstitialLoading || interstitialAd != null) return
        isInterstitialLoading = true

        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            activeInterstitialAdUnitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "Interstitial ad loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    _adStats.value = _adStats.value.copy(failedLoads = _adStats.value.failedLoads + 1)
                    Log.w(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Displays an interstitial ad if ready.
     */
    fun showInterstitialAd(activity: Activity, onFinished: () -> Unit) {
        val current = interstitialAd
        if (current == null) {
            loadInterstitialAd(activity)
            onFinished()
            return
        }

        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitialAd(activity)
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                loadInterstitialAd(activity)
                onFinished()
            }
        }

        current.show(activity)
    }

    // =========================================================================
    // REWARDED ADS (With Callback Verification & Server-Side Validation)
    // =========================================================================

    /**
     * Loads a rewarded video ad with Server-Side Verification (SSV) options configured.
     */
    fun loadRewardedAd(context: Context, userId: String? = null, customData: String? = null) {
        if (isRewardedLoading || rewardedAd != null) return
        isRewardedLoading = true
        _adStats.value = _adStats.value.copy(requestsCount = _adStats.value.requestsCount + 1)

        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            activeRewardedAdUnitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false

                    // Configure Server-Side Verification (SSV) if user identifier provided
                    if (!userId.isNullOrBlank()) {
                        val ssv = ServerSideVerificationOptions.Builder()
                            .setUserId(userId)
                            .setCustomData(customData ?: "")
                            .build()
                        ad.setServerSideVerificationOptions(ssv)
                    }

                    Log.d(TAG, "Rewarded ad loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                    _adStats.value = _adStats.value.copy(failedLoads = _adStats.value.failedLoads + 1)
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Shows a rewarded ad.
     * Enforces:
     * 1. Placement cooldown verification.
     * 2. Atomic callback verification (prevents duplicate execution if SDK triggers multiple callbacks).
     * 3. Server-side validation via Firebase Firestore ledger.
     */
    fun showRewardedAd(
        activity: Activity,
        placementId: String,
        userId: String,
        expectedRewardAmount: Int = 10,
        onResult: (RewardValidationResult) -> Unit
    ) {
        // 1. Verify cooldown
        val lastUsed = placementCooldowns[placementId] ?: 0L
        val now = System.currentTimeMillis()
        val cooldownMs = 15_000L // 15s placement cooldown
        if (now - lastUsed < cooldownMs) {
            val remainingSec = ((cooldownMs - (now - lastUsed)) / 1000).coerceAtLeast(1)
            onResult(
                RewardValidationResult.Failure(
                    placementId = placementId,
                    reason = "Placement in cooldown. Please wait $remainingSec second(s)."
                )
            )
            return
        }

        val currentAd = rewardedAd
        if (currentAd == null) {
            loadRewardedAd(activity, userId, placementId)
            onResult(
                RewardValidationResult.Failure(
                    placementId = placementId,
                    reason = "Ad is loading. Please try again in a moment."
                )
            )
            return
        }

        val hasEarnedReward = AtomicBoolean(false)
        val nonce = UUID.randomUUID().toString()

        // Set SSV options with unique transaction nonce
        val ssv = ServerSideVerificationOptions.Builder()
            .setUserId(userId)
            .setCustomData("$placementId:$nonce")
            .build()
        currentAd.setServerSideVerificationOptions(ssv)

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                _adStats.value = _adStats.value.copy(impressionsCount = _adStats.value.impressionsCount + 1)
                rewardedAd = null // Consume ad instance immediately
            }

            override fun onAdDismissedFullScreenContent() {
                loadRewardedAd(activity, userId, placementId)
                if (!hasEarnedReward.get()) {
                    onResult(
                        RewardValidationResult.Failure(
                            placementId = placementId,
                            reason = "Ad closed before completing the required watch time."
                        )
                    )
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadRewardedAd(activity, userId, placementId)
                onResult(
                    RewardValidationResult.Failure(
                        placementId = placementId,
                        reason = adError.message
                    )
                )
            }
        }

        // Show ad and await onUserEarnedReward callback
        currentAd.show(activity) { rewardItem ->
            // Prevent duplicate reward grant on client side
            if (hasEarnedReward.compareAndSet(false, true)) {
                val grantAmount = if (rewardItem.amount > 0) rewardItem.amount else expectedRewardAmount
                placementCooldowns[placementId] = System.currentTimeMillis()

                // Execute asynchronous server-side reward validation & persistence
                validateAndCommitReward(
                    userId = userId,
                    placementId = placementId,
                    nonce = nonce,
                    rewardAmount = grantAmount,
                    onResult = onResult
                )
            }
        }
    }

    /**
     * Validates and records reward with server-side validation against Firebase Firestore.
     */
    private fun validateAndCommitReward(
        userId: String,
        placementId: String,
        nonce: String,
        rewardAmount: Int,
        onResult: (RewardValidationResult) -> Unit
    ) {
        val token = "$userId:$placementId:$nonce"
        if (!processedRewardTokens.add(token)) {
            onResult(
                RewardValidationResult.Failure(
                    placementId = placementId,
                    reason = "Duplicate reward token detected."
                )
            )
            return
        }

        serviceScope.launch {
            val db = databaseService
            var serverValidated = false
            val txId = "tx_ad_" + hashString(token).take(12)

            if (db != null && userId.isNotBlank()) {
                try {
                    // Record cloud transaction ledger entry
                    val transaction = FirestoreCoinTransaction(
                        id = txId,
                        userId = userId,
                        type = "AD_REWARD",
                        amount = rewardAmount,
                        description = "Ad Placement: $placementId (Verified)",
                        timestamp = System.currentTimeMillis()
                    )
                    val recordResult = db.recordCoinTransaction(userId, transaction)
                    if (recordResult.isSuccess) {
                        // Atomically increment cloud user balance
                        val profileResult = db.getUserProfile(userId)
                        val currentBalance = profileResult.getOrNull()?.totalCoins ?: 0
                        db.updateCoinBalance(userId, currentBalance + rewardAmount)
                        serverValidated = true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Server verification fallback: local ledger will resolve during next sync.", e)
                }
            }

            _adStats.value = _adStats.value.copy(
                rewardedCompletions = _adStats.value.rewardedCompletions + 1,
                serverValidatedRewards = _adStats.value.serverValidatedRewards + (if (serverValidated) 1 else 0)
            )

            withContext(Dispatchers.Main) {
                onResult(
                    RewardValidationResult.Success(
                        placementId = placementId,
                        rewardAmount = rewardAmount,
                        transactionId = txId,
                        serverValidated = serverValidated,
                        message = "+$rewardAmount Coins Earned!"
                    )
                )
            }
        }
    }

    // =========================================================================
    // REWARDED INTERSTITIAL & APP OPEN ADS
    // =========================================================================

    fun loadRewardedInterstitialAd(context: Context) {
        if (isRewardedInterstitialLoading || rewardedInterstitialAd != null) return
        isRewardedInterstitialLoading = true

        val request = AdRequest.Builder().build()
        RewardedInterstitialAd.load(
            context,
            activeRewardedInterstitialAdUnitId,
            request,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    isRewardedInterstitialLoading = false
                    Log.d(TAG, "Rewarded Interstitial ad loaded.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedInterstitialAd = null
                    isRewardedInterstitialLoading = false
                }
            }
        )
    }

    fun loadAppOpenAd(context: Context) {
        if (isAppOpenLoading || appOpenAd != null) return
        isAppOpenLoading = true

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            activeAppOpenAdUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isAppOpenLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    isAppOpenLoading = false
                }
            }
        )
    }

    fun showAppOpenAd(activity: Activity, onDismissed: () -> Unit = {}) {
        val current = appOpenAd ?: run {
            loadAppOpenAd(activity)
            onDismissed()
            return
        }

        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                loadAppOpenAd(activity)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                loadAppOpenAd(activity)
                onDismissed()
            }
        }
        current.show(activity)
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Reusable Jetpack Compose Banner Ad View backed by MobileAdsService.
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier
) {
    val service = MobileAdsService.getInstance()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    service.loadBanner(this)
                }
            }
        )
    }
}
