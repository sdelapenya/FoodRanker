package com.app.foodranker.utils

import android.app.Activity
import android.content.Context
import com.app.foodranker.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consentimiento de anuncios para el Espacio Económico Europeo y el Reino Unido.
 *
 * Desde enero de 2024 Google exige una CMP (plataforma de gestión del consentimiento)
 * certificada para servir anuncios a usuarios del EEE y de Reino Unido. Sin ella AdMob
 * limita o corta el servicio de anuncios justo en el territorio donde está prácticamente
 * todo el público de la app. El SDK UMP de Google es gratuito y es CMP certificada.
 *
 * El SDK decide solo si hay que pedir consentimiento, según dónde esté el usuario: fuera
 * del EEE y Reino Unido no enseña nada y `canRequestAds` es true desde el primer momento.
 *
 * ⚠️ El mensaje hay que crearlo ADEMÁS en la consola de AdMob (Privacidad y mensajes →
 * RGPD). Sin él el SDK no tiene nada que enseñar, y aquí no aparece ningún error: el
 * formulario simplemente no sale nunca y los anuncios no arrancan en Europa.
 */
object ConsentManager {

    // Los anuncios se tapan hasta que el SDK dice que se pueden pedir. Empieza en false a
    // propósito: pedir un anuncio antes de tener el consentimiento es justo lo que la
    // política prohíbe, y en el peor caso el banner tarda un instante de más en salir.
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds

    private val adsStarted = AtomicBoolean(false)

    /**
     * Pide el consentimiento si hace falta y arranca los anuncios cuando se puede.
     *
     * Hay que llamarlo desde una Activity porque el formulario es un diálogo. Es seguro
     * llamarlo más de una vez (MainActivity.onCreate se repite al girar la pantalla):
     * el arranque del SDK de anuncios está protegido con un flag.
     */
    fun gather(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)

        // Si el usuario ya decidió en una sesión anterior, su respuesta está guardada y
        // los anuncios pueden empezar sin esperar a la red.
        startAdsIfAllowed(activity, info)

        info.requestConsentInfoUpdate(
            activity,
            requestParameters(activity),
            {
                // Enseña el formulario solo si toca; si no toca, llama al callback y ya.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) {
                        android.util.Log.w(
                            "ConsentManager",
                            "Formulario de consentimiento: ${error.errorCode} ${error.message}"
                        )
                    }
                    startAdsIfAllowed(activity, info)
                }
            },
            { error ->
                // Sin red no se puede consultar, pero puede haber un consentimiento ya
                // guardado de antes: se vuelve a mirar en vez de dejar los anuncios muertos.
                android.util.Log.w(
                    "ConsentManager",
                    "Consulta de consentimiento: ${error.errorCode} ${error.message}"
                )
                startAdsIfAllowed(activity, info)
            }
        )
    }

    private fun requestParameters(context: Context): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        // En debug se fuerza la geografía del EEE para poder ver el formulario desde
        // España o desde donde sea; si no, probarlo exigiría estar físicamente en la UE.
        // Los IDs son los mismos de AdMob, salen de local.properties (fuera de git).
        if (BuildConfig.DEBUG) {
            val ids = BuildConfig.ADMOB_TEST_DEVICE_IDS
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (ids.isNotEmpty()) {
                val debug = ConsentDebugSettings.Builder(context)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                ids.forEach { debug.addTestDeviceHashedId(it) }
                builder.setConsentDebugSettings(debug.build())
            }
        }
        return builder.build()
    }

    private fun startAdsIfAllowed(context: Context, info: ConsentInformation) {
        if (!info.canRequestAds()) return
        _canRequestAds.value = true
        if (adsStarted.compareAndSet(false, true)) {
            AdManager.initialize(context.applicationContext)
        }
    }

    /**
     * El RGPD exige poder cambiar de opinión, así que hace falta una entrada permanente
     * para reabrir el formulario. Solo se enseña cuando el SDK dice que hace falta: fuera
     * del EEE y Reino Unido nadie la pide y sería una opción muerta que no abre nada.
     */
    fun isPrivacyOptionsRequired(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context)
            .privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptionsForm(activity: Activity, onError: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) {
                android.util.Log.w(
                    "ConsentManager",
                    "Opciones de privacidad: ${error.errorCode} ${error.message}"
                )
                onError()
            }
        }
    }
}
