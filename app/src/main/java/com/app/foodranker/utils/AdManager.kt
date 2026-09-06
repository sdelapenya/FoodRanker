package com.app.foodranker.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicInteger
import com.app.foodranker.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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
        // En debug, marcar este dispositivo como de prueba para que AdMob sirva
        // anuncios de test. Sin esto el móvil de desarrollo recibe anuncios reales
        // y cualquier pulsación cuenta como tráfico inválido, motivo habitual de
        // suspensión de cuenta. Los IDs salen de local.properties (fuera de git);
        // si está vacío no se toca la configuración.
        if (BuildConfig.DEBUG) {
            val ids = BuildConfig.ADMOB_TEST_DEVICE_IDS
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (ids.isNotEmpty()) {
                MobileAds.setRequestConfiguration(
                    RequestConfiguration.Builder().setTestDeviceIds(ids).build()
                )
            }
        }
        // MobileAds.initialize() hace E/S de disco y red. Llamarlo en el hilo principal
        // desde Application.onCreate() retrasa el arranque y puede acabar en ANR
        // ("failed to complete startup", reproducido en el emulador de Android 15); la
        // propia documentación de Google pide lanzarlo en segundo plano.
        //
        // Las precargas van dentro del callback y no antes: lanzarlas mientras el SDK
        // aún se inicializa era pedir anuncios sin nada configurado. Se publican al
        // hilo principal a mano porque la API de anuncios exige que se llamen ahí.
        val appContext = context.applicationContext
        Thread {
            MobileAds.initialize(appContext) {
                Handler(Looper.getMainLooper()).post {
                    loadInterstitial(appContext)
                    loadRewarded(appContext)
                }
            }
        }.start()
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
        // Captura local para evitar la race entre el null-check y el .show():
        // si otro hilo pone interstitialAd=null entre ambas líneas, onDismiss
        // nunca se llamaría y la navegación quedaría bloqueada.
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback =
                object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        loadInterstitial(activity)
                        onDismiss()
                    }
                    override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                        interstitialAd = null
                        onDismiss()
                    }
                }
            ad.show(activity)
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
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback =
                object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        rewardedAd = null
                        loadRewarded(activity)
                        onDismiss()
                    }
                    override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                        rewardedAd = null
                        onDismiss()
                    }
                }
            ad.show(activity) {
                onRewarded()
            }
        } else {
            onDismiss()
        }
    }
}