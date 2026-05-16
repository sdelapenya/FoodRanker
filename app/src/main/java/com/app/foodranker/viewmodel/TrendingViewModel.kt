package com.app.foodranker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.data.model.Plate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class TrendingUiState(
    val mostLiked: List<Plate> = emptyList(),
    val topRated: List<Plate> = emptyList(),
    val mostActive: List<Plate> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendingUiState())
    val uiState: StateFlow<TrendingUiState> = _uiState

    init { loadTrending() }

    fun loadTrending() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val snapshot = firestore.collection("plates").limit(100).get().await()
                val plates = snapshot.documents.mapNotNull {
                    it.toObject(Plate::class.java)?.copy(id = it.id)
                }

                val mostLiked = plates
                    .filter { it.likes > 0 }
                    .sortedByDescending { it.likes }
                    .take(10)
                    .ifEmpty { plates.sortedByDescending { it.averageScore }.take(10) }

                val topRated = plates
                    .filter { it.totalRatings >= 1 }
                    .sortedByDescending { it.averageScore }
                    .take(10)

                // Puntuación de engagement: likes valen doble que valoraciones
                val mostActive = plates
                    .sortedByDescending { it.likes * 2 + it.totalRatings }
                    .take(10)

                _uiState.value = _uiState.value.copy(
                    mostLiked = mostLiked,
                    topRated = topRated,
                    mostActive = mostActive,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
