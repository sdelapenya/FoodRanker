package com.app.foodranker

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Variante de release: atestigua la app con Play Integrity.
 *
 * Play Integrity solo reconoce builds distribuidas por Google Play, así que un APK
 * de release instalado a mano en el móvil NO obtiene token válido. Por eso el
 * enforcement de App Check se activa en la consola de Firebase solo cuando la app
 * ya está en un track de Play; hasta entonces se deja en monitorización y las
 * llamadas sin token siguen pasando.
 *
 * La variante de debug vive en `src/debug` y usa el proveedor de depuración: son
 * dos ficheros y no un `if (BuildConfig.DEBUG)` porque la dependencia del
 * proveedor de debug es `debugImplementation` y su clase no existe en release.
 */
object AppCheckInstaller {
    fun install() {
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
}
