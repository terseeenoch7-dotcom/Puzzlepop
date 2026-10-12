package com.example.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow

/**
 * AdPlacementConfig data model for managing ad placement rules and rewards.
 */
data class AdPlacementConfig(
    val id: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val rewardAmount: Int = 10,
    val cooldownSeconds: Long = 30L
)

/**
 * AdMobManager provides unified singleton access, delegating directly to MobileAdsService.
 * Maintains full backward compatibility across the application.
 */
object AdMobManager {

    private val service = MobileAdsService.getInstance()

    // Live AdMob App ID & Ad Unit IDs provided by user
    const val LIVE_APP_ID = MobileAdsService.LIVE_APP_ID
    const val LIVE_BANNER_AD_UNIT_ID_PRIMARY = MobileAdsService.LIVE_BANNER_AD_UNIT_ID_PRIMARY
    const val LIVE_BANNER_AD_UNIT_ID_SECONDARY = MobileAdsService.LIVE_BANNER_AD_UNIT_ID_SECONDARY
    const val LIVE_INTERSTITIAL_AD_UNIT_ID_PRIMARY = MobileAdsService.LIVE_INTERSTITIAL_AD_UNIT_ID_PRIMARY
    const val LIVE_INTERSTITIAL_AD_UNIT_ID_SECONDARY = MobileAdsService.LIVE_INTERSTITIAL_AD_UNIT_ID_SECONDARY
    const val LIVE_REWARDED_INTERSTITIAL_AD_UNIT_ID_PRIMARY = MobileAdsService.LIVE_REWARDED_INTERSTITIAL_AD_UNIT_ID_PRIMARY
    const val LIVE_REWARDED_INTERSTITIAL_AD_UNIT_ID_SECONDARY = MobileAdsService.LIVE_REWARDED_INTERSTITIAL_AD_UNIT_ID_SECONDARY
    const val LIVE_REWARDED_AD_UNIT_ID_PRIMARY = MobileAdsService.LIVE_REWARDED_AD_UNIT_ID_PRIMARY
    const val LIVE_REWARDED_AD_UNIT_ID_SECONDARY = MobileAdsService.LIVE_REWARDED_AD_UNIT_ID_SECONDARY
    const val LIVE_APP_OPEN_AD_UNIT_ID = MobileAdsService.LIVE_APP_OPEN_AD_UNIT_ID

    // Test Ad Unit IDs
    const val TEST_REWARDED_AD_UNIT_ID = MobileAdsService.TEST_REWARDED_AD_UNIT_ID
    const val TEST_INTERSTITIAL_AD_UNIT_ID = MobileAdsService.TEST_INTERSTITIAL_AD_UNIT_ID
    const val TEST_BANNER_AD_UNIT_ID = MobileAdsService.TEST_BANNER_AD_UNIT_ID
    const val TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID = MobileAdsService.TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID
    const val TEST_APP_OPEN_AD_UNIT_ID = MobileAdsService.TEST_APP_OPEN_AD_UNIT_ID

    // Placements
    const val PLACEMENT_EXTRA_TIME = MobileAdsService.PLACEMENT_EXTRA_TIME
    const val PLACEMENT_LEVEL_COMPLETION_BONUS = MobileAdsService.PLACEMENT_LEVEL_COMPLETION_BONUS
    const val PLACEMENT_DOUBLE_LEVEL_REWARD = MobileAdsService.PLACEMENT_DOUBLE_LEVEL_REWARD
    const val PLACEMENT_SECOND_CHANCE = MobileAdsService.PLACEMENT_SECOND_CHANCE
    const val PLACEMENT_HINT_REWARD = MobileAdsService.PLACEMENT_HINT_REWARD
    const val PLACEMENT_SHUFFLE_REWARD = MobileAdsService.PLACEMENT_SHUFFLE_REWARD
    const val PLACEMENT_UNDO_REWARD = MobileAdsService.PLACEMENT_UNDO_REWARD
    const val PLACEMENT_EXTRA_TRAY_SLOT = MobileAdsService.PLACEMENT_EXTRA_TRAY_SLOT
    const val PLACEMENT_DAILY_BONUS = MobileAdsService.PLACEMENT_DAILY_BONUS
    const val PLACEMENT_TASK_CLAIM = MobileAdsService.PLACEMENT_TASK_CLAIM

    var useLiveAdUnits: Boolean
        get() = service.useLiveAdUnits
        set(value) {
            service.useLiveAdUnits = value
        }

    val activeBannerAdUnitId: String get() = service.activeBannerAdUnitId
    val activeRewardedAdUnitId: String get() = service.activeRewardedAdUnitId
    val activeInterstitialAdUnitId: String get() = service.activeInterstitialAdUnitId
    val activeRewardedInterstitialAdUnitId: String get() = service.activeRewardedInterstitialAdUnitId
    val activeAppOpenAdUnitId: String get() = service.activeAppOpenAdUnitId

    data class AdStats(
        val requestsCount: Int,
        val impressionsCount: Int,
        val rewardedCompletions: Int,
        val failedLoads: Int
    )

    fun initialize(context: Context) {
        service.initialize(context)
    }

    fun loadRewardedAd(context: Context) {
        service.loadRewardedAd(context)
    }

    fun loadInterstitialAd(context: Context) {
        service.loadInterstitialAd(context)
    }

    fun loadRewardedInterstitialAd(context: Context) {
        service.loadRewardedInterstitialAd(context)
    }

    fun loadAppOpenAd(context: Context) {
        service.loadAppOpenAd(context)
    }

    fun showRewardedAd(
        activity: Activity,
        placementId: String,
        userId: String = "",
        onRewardEarned: (placementId: String, rewardAmount: Int) -> Unit,
        onDismissedOrFailed: (reason: String?) -> Unit
    ) {
        service.showRewardedAd(
            activity = activity,
            placementId = placementId,
            userId = userId,
            expectedRewardAmount = 10
        ) { result ->
            when (result) {
                is RewardValidationResult.Success -> {
                    onRewardEarned(result.placementId, result.rewardAmount)
                }
                is RewardValidationResult.Failure -> {
                    onDismissedOrFailed(result.reason)
                }
            }
        }
    }

    fun showInterstitialAd(
        activity: Activity,
        onFinished: () -> Unit
    ) {
        service.showInterstitialAd(activity, onFinished)
    }

    fun showAppOpenAd(
        activity: Activity,
        onDismissed: () -> Unit = {}
    ) {
        service.showAppOpenAd(activity, onDismissed)
    }
}

/**
 * Backward compatible Composable Banner Ad component powered by MobileAdsService.
 */
@Composable
fun AdMobBannerView(
    modifier: Modifier = Modifier
) {
    AdMobBanner(modifier = modifier)
}
