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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

data class AdPlacementConfig(
    val id: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val rewardAmount: Int = 10,
    val cooldownSeconds: Long = 30L
)

/**
 * Centralized AdMob management service for PuzzlePop.
 * Supports development test ad units as well as user-provided live ad units with automatic fallback.
 */
object AdMobManager {

    private const val TAG = "AdMobManager"

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

    // Google Official Test Ad Unit IDs (used during testing to prevent policy violations)
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379"
    const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

    // Flag indicating whether to use live ad units (true for release / user builds)
    var useLiveAdUnits: Boolean = false

    // Active Ad Unit IDs resolved dynamically
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

    // Supported Placements
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

    private var isInitialized = false
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null

    private var isRewardedAdLoading = false
    private var isInterstitialAdLoading = false
    private var isRewardedInterstitialLoading = false
    private var isAppOpenLoading = false

    private val placementCooldowns = mutableMapOf<String, Long>()

    // Telemetry stats
    private val _adStats = MutableStateFlow(
        AdStats(
            requestsCount = 0,
            impressionsCount = 0,
            rewardedCompletions = 0,
            failedLoads = 0
        )
    )
    val adStats: StateFlow<AdStats> = _adStats.asStateFlow()

    data class AdStats(
        val requestsCount: Int,
        val impressionsCount: Int,
        val rewardedCompletions: Int,
        val failedLoads: Int
    )

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            MobileAds.initialize(context) {
                isInitialized = true
                Log.d(TAG, "Google AdMob SDK initialized successfully. Mode: ${if (useLiveAdUnits) "LIVE" else "TEST"}")
                loadRewardedAd(context)
                loadInterstitialAd(context)
                loadRewardedInterstitialAd(context)
                loadAppOpenAd(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MobileAds", e)
        }
    }

    fun loadRewardedAd(context: Context) {
        if (isRewardedAdLoading || rewardedAd != null) return
        isRewardedAdLoading = true
        _adStats.value = _adStats.value.copy(requestsCount = _adStats.value.requestsCount + 1)

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            activeRewardedAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedAdLoading = false
                    Log.d(TAG, "Rewarded ad loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedAdLoading = false
                    _adStats.value = _adStats.value.copy(failedLoads = _adStats.value.failedLoads + 1)
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun loadInterstitialAd(context: Context) {
        if (isInterstitialAdLoading || interstitialAd != null) return
        isInterstitialAdLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            activeInterstitialAdUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialAdLoading = false
                    Log.d(TAG, "Interstitial ad loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialAdLoading = false
                    Log.w(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun loadRewardedInterstitialAd(context: Context) {
        if (isRewardedInterstitialLoading || rewardedInterstitialAd != null) return
        isRewardedInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedInterstitialAd.load(
            context,
            activeRewardedInterstitialAdUnitId,
            adRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    isRewardedInterstitialLoading = false
                    Log.d(TAG, "Rewarded Interstitial ad loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedInterstitialAd = null
                    isRewardedInterstitialLoading = false
                    Log.w(TAG, "Rewarded Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun loadAppOpenAd(context: Context) {
        if (isAppOpenLoading || appOpenAd != null) return
        isAppOpenLoading = true

        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            activeAppOpenAdUnitId,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isAppOpenLoading = false
                    Log.d(TAG, "App Open ad loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    isAppOpenLoading = false
                    Log.w(TAG, "App Open ad failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Shows a rewarded ad for a specific placement.
     * Prevents duplicate rewards via an AtomicBoolean single-use flag.
     */
    fun showRewardedAd(
        activity: Activity,
        placementId: String,
        onRewardEarned: (placementId: String, rewardAmount: Int) -> Unit,
        onDismissedOrFailed: (reason: String?) -> Unit
    ) {
        val currentAd = rewardedAd
        if (currentAd == null) {
            // Ad not ready, load for next time
            loadRewardedAd(activity)
            onDismissedOrFailed("Ad currently unavailable. Loading for next session.")
            return
        }

        val hasRewarded = AtomicBoolean(false)

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                _adStats.value = _adStats.value.copy(impressionsCount = _adStats.value.impressionsCount + 1)
                rewardedAd = null // Consume ad instance
            }

            override fun onAdDismissedFullScreenContent() {
                loadRewardedAd(activity)
                if (!hasRewarded.get()) {
                    onDismissedOrFailed("Ad closed before earning reward.")
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadRewardedAd(activity)
                onDismissedOrFailed(adError.message)
            }
        }

        currentAd.show(activity) { rewardItem ->
            if (hasRewarded.compareAndSet(false, true)) {
                _adStats.value = _adStats.value.copy(
                    rewardedCompletions = _adStats.value.rewardedCompletions + 1
                )
                placementCooldowns[placementId] = System.currentTimeMillis()
                val amount = if (rewardItem.amount > 0) rewardItem.amount else 10
                onRewardEarned(placementId, amount)
            }
        }
    }

    /**
     * Shows an interstitial ad at natural break points.
     */
    fun showInterstitialAd(
        activity: Activity,
        onFinished: () -> Unit
    ) {
        val currentAd = interstitialAd
        if (currentAd == null) {
            loadInterstitialAd(activity)
            onFinished()
            return
        }

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
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

        currentAd.show(activity)
    }

    /**
     * Shows an App Open ad when the app comes into foreground.
     */
    fun showAppOpenAd(
        activity: Activity,
        onDismissed: () -> Unit = {}
    ) {
        val currentAd = appOpenAd ?: run {
            loadAppOpenAd(activity)
            onDismissed()
            return
        }

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
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

        currentAd.show(activity)
    }
}

/**
 * Composable Banner Ad component powered by Google AdMob.
 */
@Composable
fun AdMobBannerView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                    adUnitId = AdMobManager.activeBannerAdUnitId
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
