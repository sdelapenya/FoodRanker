package com.app.foodranker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.PlateCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

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

enum class SortOption(val label: String) {
    SCORE("Mejor puntuación"),
    RECENT("Más recientes"),
    RATINGS("Más valorados"),
    LIKES("Más likes")
}

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState

    private var searchJob: Job? = null
    private var searchUsersJob: Job? = null

    init { search() }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (_uiState.value.searchMode == SearchMode.USERS) searchUsers() else search()
    }

    fun onCategoryChange(category: PlateCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        search()
    }

    fun onCityChange(city: String) {
        _uiState.value = _uiState.value.copy(selectedCity = city)
        search()
    }

    fun onSortChange(sort: SortOption) {
        _uiState.value = _uiState.value.copy(sortBy = sort)
        search()
    }

    fun setSearchMode(mode: SearchMode) {
        _uiState.value = _uiState.value.copy(searchMode = mode)
        if (mode == SearchMode.USERS) searchUsers()
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
                        .startAt(q).endAt("$q")
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
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun search() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            delay(300)
            try {
                val state = _uiState.value

                // Cargamos los platos sin filtros en Firestore y filtramos en local
                // para evitar índices compuestos. Excluimos platos con 3+ reportes.
                val snapshot = firestore.collection("plates")
                    .limit(100)
                    .get()
                    .await()

                var plates = snapshot.documents
                    .mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                    .filter { it.reportCount < 3 }

                // Filtro por categoría (local)
                if (state.selectedCategory != null) {
                    plates = plates.filter { it.category == state.selectedCategory }
                }

                // Filtro por ciudad (local)
                if (state.selectedCity.isNotBlank()) {
                    plates = plates.filter {
                        it.city.contains(state.selectedCity, ignoreCase = true)
                    }
                }

                // Filtro por nombre/restaurante/ciudad (local)
                if (state.query.isNotBlank()) {
                    plates = plates.filter {
                        it.name.contains(state.query, ignoreCase = true) ||
                                it.restaurantName.contains(state.query, ignoreCase = true) ||
                                it.city.contains(state.query, ignoreCase = true)
                    }
                }

                // Ordenación (local)
                plates = when (state.sortBy) {
                    SortOption.SCORE   -> plates.sortedByDescending { it.averageScore }
                    SortOption.RECENT  -> plates.sortedByDescending { it.createdAt }
                    SortOption.RATINGS -> plates.sortedByDescending { it.totalRatings }
                    SortOption.LIKES   -> plates.sortedByDescending { it.likes }
                }

                _uiState.value = _uiState.value.copy(
                    results = plates,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = com.app.foodranker.utils.ErrorMapper.toUserMessage(e)
                )
            }
        }
    }
}