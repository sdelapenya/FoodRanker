package com.app.foodranker.utils

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

/**
 * Valores configurables remotamente desde Firebase Console sin publicar una nueva versión.
 * Uso: RemoteConfigManager.maxDailyPlates, RemoteConfigManager.feedPageSize, etc.
 */
object RemoteConfigManager {

    private val config = Firebase.remoteConfig

    fun initialize() {
        config.setConfigSettingsAsync(remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (android.os.Build.TYPE == "user") 3600L else 0L
        })
        // Valores por defecto (se usan si no hay conexión o no se ha hecho fetch)
        config.setDefaultsAsync(mapOf(
            // Era 10 para todos y 20 para Premium. Al retirar Premium (2026-11-22)
            // se queda el valor alto, que es el que ya tenían ocho usuarios.
            // ⚠️ La plantilla de Remote Config del servidor está VACÍA (comprobado
            // con `remoteconfig:get`), así que manda este valor por defecto. Si algún
            // día se publica una plantilla, el valor de allí gana al de aquí.
            "max_daily_plates"       to 20L,
            "feed_page_size"         to 20L,
            "show_thefork_button"    to true,
            "enable_following_feed"  to true,
            "onboarding_enabled"     to true,
            "vision_api_enabled"     to true,
            "min_image_score"        to 0.5f,
            "daily_mission_goal"     to 3L
        ))
        config.fetchAndActivate()
            .addOnSuccessListener { updated ->
                android.util.Log.d("RemoteConfig", "fetch OK — valores actualizados: $updated")
            }
            .addOnFailureListener { e ->
                android.util.Log.w("RemoteConfig", "fetch fallido, usando defaults: ${e.message}")
            }
    }

    val maxDailyPlates: Int         get() = config.getLong("max_daily_plates").toInt()
    val feedPageSize: Int           get() = config.getLong("feed_page_size").toInt()
    val showTheForkButton: Boolean  get() = config.getBoolean("show_thefork_button")
    val enableFollowingFeed: Boolean get() = config.getBoolean("enable_following_feed")
    val onboardingEnabled: Boolean  get() = config.getBoolean("onboarding_enabled")
    val visionApiEnabled: Boolean   get() = config.getBoolean("vision_api_enabled")
    val minImageScore: Float        get() = config.getDouble("min_image_score").toFloat().coerceIn(0f, 1f)
    val dailyMissionGoal: Int       get() = config.getLong("daily_mission_goal").toInt().coerceAtLeast(1)
}
