package com.app.foodranker

import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Variante de debug: proveedor de depuración.
 *
 * En el primer arranque el SDK escribe en logcat un token con la forma
 * `Enter this debug secret into the allow list...`. Hay que darlo de alta en
 * Firebase Console → App Check → app Android → Administrar tokens de depuración,
 * o el emulador dejará de poder llamar a las callables en cuanto se active el
 * enforcement. El token es por instalación: cada emulador o reinstalación genera
 * uno nuevo.
 *
 * Ver la variante de release en `src/release` para por qué son dos ficheros.
 */
object AppCheckInstaller {
    fun install() {
        Log.d("AppCheck", "Instalando el proveedor de depuración (build de debug)")
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
