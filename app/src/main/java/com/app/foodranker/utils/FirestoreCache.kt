package com.app.foodranker.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.persistentCacheSettings

/**
 * Enciende la caché persistente de Firestore, y lo hace **antes que nadie**.
 *
 * ## Por qué existe este fichero y no se configura donde se configuraba
 *
 * Los ajustes vivían en `FirebaseModule.provideFirestore`, el proveedor de Hilt. El
 * problema es que `setFirestoreSettings` solo se puede llamar **antes de la primera
 * operación** contra Firestore: en cuanto el cliente arranca, lanza
 * `IllegalStateException` y los ajustes se pierden.
 *
 * Y algo arrancaba Firestore antes de que Hilt llegara a construir ese singleton:
 * `FoodRankerApp.onCreate()` llama a `FoodRankerMessagingService.saveCurrentToken()`,
 * que accede a `FirebaseFirestore.getInstance()` por su cuenta para escribir el token
 * del dispositivo.
 *
 * ⚠️ **El comentario que había decía que era "una carrera" que "no falla siempre". Era
 * falso.** Esa llamada está dentro de `if (FirebaseAuth.getInstance().currentUser != null)`,
 * así que ocurría **en todos los arranques con sesión iniciada, que son casi todos**. El
 * resultado: la app funcionaba **sin caché persistente para el cien por cien de los
 * usuarios**, es decir, sin modo offline. Se descubrió el 2026-11-23 porque el
 * `Fallos.reportar` del `catch` llegó a Crashlytics, y se reprodujo en el Redmi al primer
 * arranque.
 *
 * ## Por qué aquí es seguro
 *
 * [aplicar] se llama como primera sentencia de `FoodRankerApp.onCreate()`. Firebase se
 * inicializa antes, en el `FirebaseInitProvider`, pero eso solo construye el `FirebaseApp`:
 * no abre el cliente de Firestore ni ejecuta ninguna operación, que es lo que cierra la
 * puerta a los ajustes.
 *
 * ⚠️ **Si algún día se añade algo a `onCreate()` que toque Firestore, tiene que ir después
 * de esta llamada.** Es la única regla que hay que respetar aquí.
 *
 * Se deja el `try/catch` como red de seguridad —y reportando— para que, si alguien vuelve a
 * colar un acceso por delante, no se pierda en silencio otra vez.
 */
object FirestoreCache {

    private const val TAM_CACHE_BYTES = 50L * 1024 * 1024 // 50 MB

    @Volatile
    private var aplicada = false

    /** Idempotente: la segunda llamada no hace nada. */
    fun aplicar() {
        if (aplicada) return
        aplicada = true
        try {
            FirebaseFirestore.getInstance().firestoreSettings =
                FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        persistentCacheSettings { setSizeBytes(TAM_CACHE_BYTES) }
                    )
                    .build()
        } catch (e: IllegalStateException) {
            // Alguien tocó Firestore antes que nosotros. La app sigue funcionando, pero sin
            // caché offline, así que interesa enterarse.
            Fallos.reportar(e, "firestore: algo arrancó Firestore antes de aplicar la caché")
            Log.w("FirestoreCache", "Firestore ya estaba iniciado; sin caché persistente", e)
        }
    }
}
