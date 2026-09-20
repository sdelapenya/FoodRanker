package com.app.foodranker.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recuerda por qué locales ha pasado el usuario, para poder marcar como "valorado en el local"
 * un voto que se escribe más tarde — al salir del restaurante o ya en casa. Sin esto solo se
 * verificaba a quien valoraba con el plato delante, que es la minoría. Ver docs/RATINGS.md §5.
 *
 * Los check-ins se registran **de lo que ya se sabe**: "Qué pido aquí" pide la ubicación y
 * resuelve los locales cercanos de todas formas, así que anotarlos no cuesta ni una llamada
 * más (ni a Places, ni a Firestore, ni al GPS).
 *
 * Es almacenamiento **local del dispositivo** a propósito: el dato solo sirve para un
 * distintivo que no pondera nada, y guardarlo en el servidor sería registrar por dónde se
 * mueve la gente. Se pierde al cambiar de móvil, y no pasa nada.
 */
@Singleton
class VenueCheckInStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Cuánto vale un paso por el local para dar por bueno un voto posterior. */
        const val VALID_WINDOW_MS = 24L * 60 * 60 * 1000
        private const val PREFS = "venue_checkins"
    }

    private fun prefs() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Anota que el usuario está ahora en estos locales, y limpia los caducados de paso. */
    fun record(venueIds: List<String>) {
        if (venueIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val store = prefs()
        val editor = store.edit()
        store.all.forEach { (key, value) ->
            val stamp = value as? Long ?: 0L
            if (now - stamp > VALID_WINDOW_MS) editor.remove(key)
        }
        venueIds.forEach { editor.putLong(it, now) }
        editor.apply()
    }

    /** ¿Pasó por ese local dentro de la ventana válida? */
    fun wasRecentlyAt(venueId: String?): Boolean {
        if (venueId.isNullOrEmpty()) return false
        val stamp = prefs().getLong(venueId, 0L)
        if (stamp == 0L) return false
        return System.currentTimeMillis() - stamp <= VALID_WINDOW_MS
    }
}
