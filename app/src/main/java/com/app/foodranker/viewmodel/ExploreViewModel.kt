package com.app.foodranker.viewmodel

import com.app.foodranker.data.model.soloPublicos
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.categoryType
import com.app.foodranker.data.model.PlateCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import androidx.annotation.StringRes
import com.app.foodranker.R
import com.app.foodranker.utils.Fallos

data class UserResult(
    val id: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val xp: Int = 0
)

data class ExploreUiState(
    val results: List<Plate> = emptyList(),
    val userResults: List<UserResult> = emptyList(),
    val isLoading: Boolean = false,
    val query: String = "",
    val selectedCategory: PlateCategory? = null,
    val selectedCity: String = "",
    val sortBy: SortOption = SortOption.SCORE,
    val searchMode: SearchMode = SearchMode.PLATES,
    val error: String? = null
)

enum class SearchMode { PLATES, USERS }

enum class SortOption(@StringRes val label: Int) {
    SCORE(R.string.sort_score),
    RECENT(R.string.sort_recent),
    RATINGS(R.string.sort_ratings),
    LIKES(R.string.sort_likes)
}

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState

    // Caché local de platos sin filtrar. Se recarga si está vacía o han pasado >60s.
    private var allPlatesCache: List<Plate> = emptyList()
    @Volatile private var cacheLoadedAt: Long = 0L

    private var searchJob: Job? = null
    private var searchUsersJob: Job? = null

    init { search() }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (_uiState.value.searchMode == SearchMode.USERS) searchUsers() else applyFilters()
    }

    fun onCategoryChange(category: PlateCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters()
    }

    fun onCityChange(city: String) {
        _uiState.value = _uiState.value.copy(selectedCity = city)
        applyFilters()
    }

    fun onSortChange(sort: SortOption) {
        _uiState.value = _uiState.value.copy(sortBy = sort)
        applyFilters()
    }

    // Aplica filtros. Si el caché está obsoleto, hace fetch de Firestore primero.
    // Usa su propio job para evitar la auto-cancelación que ocurría al llamar a search().
    private fun applyFilters() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val cacheStale = System.currentTimeMillis() - cacheLoadedAt > CACHE_TTL_MS
            if (allPlatesCache.isEmpty() || cacheStale) {
                _uiState.value = _uiState.value.copy(isLoading = true)
                delay(300)
                performFetch()
            } else {
                delay(150)
                applyLocalFilters()
            }
        }
    }

    fun setSearchMode(mode: SearchMode) {
        _uiState.value = _uiState.value.copy(searchMode = mode)
        if (mode == SearchMode.USERS) {
            searchJob?.cancel() // cancelar búsqueda de platos en curso
            searchUsers()
        }
    }

    fun searchUsers() {
        searchUsersJob?.cancel()
        searchUsersJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            delay(300)
            val q = _uiState.value.query.trim()
            try {
                val snapshot = if (q.isBlank()) {
                    firestore.collection("users").limit(20).get().await()
                } else {
                    firestore.collection("users")
                        .orderBy("name")
                        .startAt(q).endAt(q + "")
                        .limit(20).get().await()
                }
                val users = snapshot.documents.mapNotNull { doc ->
                    UserResult(
                        id       = doc.id,
                        name     = doc.getString("name") ?: "",
                        photoUrl = doc.getString("photoUrl") ?: "",
                        bio      = doc.getString("bio") ?: "",
                        xp       = (doc.getLong("xp") ?: 0L).toInt()
                    )
                }.filter { it.name.isNotEmpty() }
                _uiState.value = _uiState.value.copy(userResults = users, isLoading = false)
            } catch (e: Exception) {
                Fallos.reportar(e, "explorar: buscar usuarios")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = com.app.foodranker.utils.ErrorMapper.toUserMessage(e)
                )
            }
        }
    }

    fun search() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            delay(300)
            performFetch()
        }
    }

    private suspend fun performFetch() {
        try {
            val snapshot = firestore.collection("plates")
                .orderBy("rankingScore", Query.Direction.DESCENDING)
                .limit(TOPE_CATALOGO)
                .get()
                .await()

            allPlatesCache = snapshot.documents
                .mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                .soloPublicos()
            cacheLoadedAt = System.currentTimeMillis()

            applyLocalFilters()
        } catch (e: Exception) {
            Fallos.reportar(e, "explorar: buscar platos")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = com.app.foodranker.utils.ErrorMapper.toUserMessage(e)
            )
        }
    }

    private fun applyLocalFilters() {
        val state = _uiState.value
        var plates = allPlatesCache
        if (state.selectedCategory != null) plates = plates.filter { it.categoryType == state.selectedCategory }
        if (state.selectedCity.isNotBlank()) plates = plates.filter { it.city.contains(state.selectedCity, ignoreCase = true) }
        if (state.query.isNotBlank()) plates = plates.filter {
            it.name.contains(state.query, ignoreCase = true) ||
            it.restaurantName.contains(state.query, ignoreCase = true) ||
            it.city.contains(state.query, ignoreCase = true)
        }
        plates = when (state.sortBy) {
            SortOption.SCORE   -> plates.sortedByDescending { it.rankingScore }
            SortOption.RECENT  -> plates.sortedByDescending { it.createdAt }
            SortOption.RATINGS -> plates.sortedByDescending { it.totalRatings }
            SortOption.LIKES   -> plates.sortedByDescending { it.likes }
        }
        _uiState.value = _uiState.value.copy(results = plates, isLoading = false)
    }

    companion object {
        /**
         * Cuántos platos se bajan para buscar dentro de ellos.
         *
         * ⚠️ **Esto es un parche, no el arreglo.** Buscar aquí funciona al revés de como
         * debería: primero se recorta el catálogo a los mejores por `rankingScore` y
         * *luego* se filtra por nombre, local o ciudad. O sea, un plato que existe pero
         * no está entre los [TOPE_CATALOGO] mejores **no aparece aunque lo escribas
         * entero**. Subir el tope no lo arregla, solo mueve el punto donde se rompe.
         *
         * Está así porque Firestore no sabe buscar subcadenas: lo que sabe hacer es
         * prefijo sobre un campo indexado, y eso cambiaría lo que encuentra la búsqueda
         * (hoy "pizza" encuentra "La Pizzería"; por prefijo, no). Esa es una decisión de
         * producto, no un detalle de implementación, así que no se toma de pasada.
         *
         * Mientras tanto: el 2026-11-18 había 59 platos en producción, así que con 500 el
         * fallo queda lejos. **Cuando el catálogo se acerque a esta cifra hay que hacer la
         * búsqueda de verdad** — campo normalizado en el plato e índice, o un servicio de
         * búsqueda. Coste de subirlo: son lecturas de Firestore por cada carga de
         * Explorar, y hay caché ([CACHE_TTL_MS]) para no repetirlas en cada tecla.
         */
        private const val TOPE_CATALOGO = 500L

        /**
         * Cuánto vale la copia local del catálogo antes de volver a pedirla.
         *
         * Importa más desde que [TOPE_CATALOGO] es 500: cada recarga son hasta 500
         * lecturas de Firestore, y sin esto se pagarían al teclear en el buscador.
         */
        private const val CACHE_TTL_MS = 60_000L
    }
}
