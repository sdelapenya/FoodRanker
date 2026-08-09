package com.app.foodranker.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.app.foodranker.data.model.Venue
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Candidato de local, antes de resolverse contra la CF. */
data class VenueSuggestion(
    val placeId: String,
    val name: String,
    val address: String
)

/**
 * Búsqueda de locales (Places, desde el cliente) y resolución del local canónico
 * (Cloud Function `resolveVenue`, en servidor).
 *
 * El reparto es deliberado: Places desde el cliente solo devuelve *candidatos*
 * (place_id + nombre). Los datos que acaban guardados en `venues` los escribe la CF
 * con una clave de servidor, porque ese documento lo ven todos los usuarios.
 * Ver docs/VENUES.md.
 */
@Singleton
class VenueRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val placesClient: PlacesClient,
    private val functions: FirebaseFunctions
) {

    private val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS)

    /**
     * Locales en los que probablemente está el usuario ahora mismo.
     * Requiere permiso de localización ya concedido — quien llama debe comprobarlo.
     */
    @SuppressLint("MissingPermission")
    suspend fun nearbyVenues(): Result<List<VenueSuggestion>> = runCatching {
        val response = placesClient
            .findCurrentPlace(FindCurrentPlaceRequest.newInstance(fields))
            .await()
        response.placeLikelihoods
            .sortedByDescending { it.likelihood }
            .take(8)
            .mapNotNull { it.place.toSuggestion() }
    }.onFailure { Log.w(TAG, "nearbyVenues falló: ${it.message}") }

    /** Búsqueda por texto, para quien no da permiso de ubicación o publica más tarde. */
    suspend fun searchVenues(query: String): Result<List<VenueSuggestion>> {
        if (query.isBlank()) return Result.success(emptyList())
        return runCatching {
            val response = placesClient
                .searchByText(SearchByTextRequest.builder(query, fields).setMaxResultCount(10).build())
                .await()
            response.places.mapNotNull { it.toSuggestion() }
        }.onFailure { Log.w(TAG, "searchVenues falló: ${it.message}") }
    }

    /**
     * Da de alta el local canónico y lo devuelve. Idempotente: si el venue ya existe
     * la CF lo devuelve sin volver a consultar Places.
     */
    suspend fun resolveVenue(placeId: String): Result<Venue> = runCatching {
        val result = functions
            .getHttpsCallable("resolveVenue")
            .call(mapOf("placeId" to placeId))
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any>
            ?: error("respuesta vacía de resolveVenue")
        @Suppress("UNCHECKED_CAST")
        val v = data["venue"] as? Map<String, Any>
            ?: error("resolveVenue no devolvió venue")

        Venue(
            id = v["id"] as? String ?: placeId,
            name = v["name"] as? String ?: "",
            address = v["address"] as? String ?: "",
            city = v["city"] as? String ?: "",
            cityNormalized = v["cityNormalized"] as? String ?: "",
            country = v["country"] as? String ?: "",
            lat = (v["lat"] as? Number)?.toDouble() ?: 0.0,
            lng = (v["lng"] as? Number)?.toDouble() ?: 0.0,
            plateCount = (v["plateCount"] as? Number)?.toInt() ?: 0,
            createdAt = (v["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }.onFailure { Log.w(TAG, "resolveVenue falló: ${it.message}") }

    private fun Place.toSuggestion(): VenueSuggestion? {
        val pid = id ?: return null
        return VenueSuggestion(
            placeId = pid,
            name = name ?: "",
            address = address ?: ""
        )
    }

    private companion object { const val TAG = "VenueRepository" }
}
