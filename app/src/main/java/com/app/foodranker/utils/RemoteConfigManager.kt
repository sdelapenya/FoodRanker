package com.app.foodranker.utils

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

/**
 * Valores configurables remotamente desde Firebase Console sin publicar una nueva versión.
 * Uso: RemoteConfigManager.premiumPrice, RemoteConfigManager.maxDailyPlates, etc.
 */
object RemoteConfigManager {

    private val config = Firebase.remoteConfig

    fun initialize() {
        config.setConfigSettingsAsync(remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (android.os.Build.TYPE == "user") 3600L else 0L
        })
        // Valores por defecto (se usan si no hay conexión o no se ha hecho fetch)
        config.setDefaultsAsync(mapOf(
            "premium_price_monthly"  to "2,99 €/mes",
            "premium_price_yearly"   to "19,99 €/año",
            "max_daily_plates"       to 10L,
            "feed_page_size"         to 20L,
            "show_thefork_button"    to true,
            "enable_following_feed"  to true,
            "onboarding_enabled"     to true,
            "vision_api_enabled"     to true,
            "min_image_score"        to 0.5f
        ))
        config.fetchAndActivate()
    }

    val premiumPriceMonthly: String get() = config.getString("premium_price_monthly")
    val premiumPriceYearly: String  get() = config.getString("premium_price_yearly")
    val maxDailyPlates: Int         get() = config.getLong("max_daily_plates").toInt()
    val feedPageSize: Int           get() = config.getLong("feed_page_size").toInt()
    val showTheForkButton: Boolean  get() = config.getBoolean("show_thefork_button")
    val enableFollowingFeed: Boolean get() = config.getBoolean("enable_following_feed")
    val onboardingEnabled: Boolean  get() = config.getBoolean("onboarding_enabled")
    val visionApiEnabled: Boolean   get() = config.getBoolean("vision_api_enabled")
}
