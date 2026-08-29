package com.app.foodranker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.Venue
import com.app.foodranker.data.repository.VenueRepository
import com.app.foodranker.data.repository.VenueSuggestion
import com.app.foodranker.utils.BillingManager
import com.app.foodranker.utils.GeoUtils
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Un local cercano ya en la app, con sus platos mejor puntuados. */
data class NearbyVenueDishes(
    val venue: Venue,
    val distanceMeters: Double,
    val dishes: List<Plate>
)

data class NearbyDishesUiState(
    val isLoading: Boolean = false,
    val results: List<NearbyVenueDishes> = emptyList(),
    val noLocationPermission: Boolean = false,
    val error: String? = null,
    // Fallback a Places, solo si el usuario lo pide (ver docs/HANDOFF.md sobre cuota).
    val isSearchingPlaces: Boolean = false,
    val placesSuggestions: List<VenueSuggestion> = emptyList(),
    val searchedPlaces: Boolean = false
)

/**
 * "Qué pido aquí": platos ya puntuados en la app cerca de tu posición real. Primero
 * consulta SOLO Firestore (gratis, sin tocar la cuota de Places): un filtro de rango en
 * `lat` acota candidatos baratos, y GeoUtils.haversineMeters calcula la distancia exacta en
 * cliente para filtrar y ordenar. Solo si no hay nada propio cerca se ofrece, a petición del
 * usuario, buscar locales por Places (searchNearby) — nunca automático.
 */
@HiltViewModel
class NearbyDishesViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val venueRepository: VenueRepository,
    private val billingManager: BillingManager
) : ViewModel() {

    companion object {
        private const val FREE_RADIUS_METERS = 550.0
        private const val PREMIUM_RADIUS_METERS = 1_600.0
        private const val PLACES_FALLBACK_RADIUS_METERS = 1_000.0
    }

    private val _uiState = MutableStateFlow(NearbyDishesUiState())
    val uiState: StateFlow<NearbyDishesUiState> = _uiState

    fun load() {
        _uiState.value = NearbyDishesUiState(isLoading = true)
        viewModelScope.launch {
            val location = venueRepository.currentLocation()
            if (location == null) {
                _uiState.value = NearbyDishesUiState(noLocationPermission = true)
                return@launch
            }

            try {
                val radiusMeters = if (billingManager.isPremium.value) PREMIUM_RADIUS_METERS else FREE_RADIUS_METERS
                val deltaLat = GeoUtils.metersToLatDegrees(radiusMeters)

                val venueSnap = firestore.collection("venues")
                    .whereGreaterThan("lat", location.latitude - deltaLat)
                    .whereLessThan("lat", location.latitude + deltaLat)
                    .limit(50)
                    .get().await()

                val nearby = venueSnap.documents
                    .mapNotNull { it.toObject(Venue::class.java)?.copy(id = it.id) }
                    .map { venue ->
                        venue to GeoUtils.haversineMeters(location.latitude, location.longitude, venue.lat, venue.lng)
                    }
                    .filter { (_, distance) -> distance <= radiusMeters }
                    .sortedBy { (_, distance) -> distance }

                if (nearby.isEmpty()) {
                    _uiState.value = NearbyDishesUiState(results = emptyList())
                    return@launch
                }

                val venueIds = nearby.map { (venue, _) -> venue.id }.take(30)
                val platesSnap = firestore.collection("plates")
                    .whereIn("venueId", venueIds)
                    .get().await()
                val platesByVenue = platesSnap.documents
                    .mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                    .filter { it.reportCount < 3 }
                    .groupBy { it.venueId }

                val results = nearby.mapNotNull { (venue, distance) ->
                    val dishes = platesByVenue[venue.id]?.sortedByDescending { it.averageScore } ?: return@mapNotNull null
                    if (dishes.isEmpty()) return@mapNotNull null
                    NearbyVenueDishes(venue = venue, distanceMeters = distance, dishes = dishes)
                }

                _uiState.value = NearbyDishesUiState(results = results)
            } catch (e: Exception) {
                _uiState.value = NearbyDishesUiState(error = com.app.foodranker.utils.ErrorMapper.toUserMessage(e))
            }
        }
    }

    /** Fallback explícito a Places, solo si el usuario lo pide tras no encontrar nada propio. */
    fun searchNearbyViaPlaces() {
        _uiState.value = _uiState.value.copy(isSearchingPlaces = true)
        viewModelScope.launch {
            venueRepository.nearbyVenues(radiusMeters = PLACES_FALLBACK_RADIUS_METERS)
                .onSuccess { suggestions ->
                    _uiState.value = _uiState.value.copy(
                        isSearchingPlaces = false,
                        placesSuggestions = suggestions,
                        searchedPlaces = true
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSearchingPlaces = false,
                        searchedPlaces = true,
                        error = "No se pudieron buscar locales cerca."
                    )
                }
        }
    }
}
