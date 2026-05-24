package com.app.foodranker.utils

import android.app.Activity
import android.content.Context
import java.util.concurrent.atomic.AtomicInteger
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {

    const val BANNER_AD_UNIT_ID       = "ca-app-pub-6291919286572988/7366691023"
    const val REWARDED_AD_UNIT_ID     = "ca-app-pub-6291919286572988/3409088549"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6291919286572988/9166857850"

    @Volatile private var interstitialAd: InterstitialAd? = null
    @Volatile private var rewardedAd: RewardedAd? = null

    private val plateDetailViewCount = AtomicInteger(0)

    fun recordPlateDetailView(): Boolean {
        val count = plateDetailViewCount.incrementAndGet()
        return if (count >= 3) {
            plateDetailViewCount.set(0)
            true
        } else false
    }

    fun initialize(context: Context) {
        MobileAds.initialize(context)
    }

    // Precargar intersticial
    fun loadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    // Mostrar intersticial (al añadir un plato)
    fun showInterstitial(activity: Activity, onDismiss: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback =
                object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        loadInterstitial(activity) // precargar el siguiente
                        onDismiss()
                    }
                }
            interstitialAd?.show(activity)
        } else {
            onDismiss()
        }
    }

    // Precargar recompensado
    fun loadRewarded(context: Context) {
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    // Mostrar recompensado (para desbloquear funciones premium)
    fun showRewarded(activity: Activity, onRewarded: () -> Unit, onDismiss: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback =
                object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        rewardedAd = null
                        loadRewarded(activity)
                        onDismiss()
                    }
                }
            rewardedAd?.show(activity) {
                onRewarded()
            }
        } else {
            onDismiss()
        }
    }
}