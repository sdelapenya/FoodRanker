package com.app.foodranker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.data.model.Plate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class TrendingUiState(
    val mostLiked: List<Plate> = emptyList(),
    val topRated: List<Plate> = emptyList(),
    val mostActive: List<Plate> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendingUiState())
    val uiState: StateFlow<TrendingUiState> = _uiState

    init { loadTrending() }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    fun loadTrending() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val (likedDocs, ratedDocs) = coroutineScope {
                    val likedJob = async {
                        firestore.collection("plates")
                            .orderBy("likes", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(20).get().await()
                            .documents.mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                            .filter { it.reportCount < 3 }
                    }
                    val ratedJob = async {
                        firestore.collection("plates")
                            .orderBy("averageScore", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(30).get().await()
                            .documents.mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                            .filter { it.reportCount < 3 }
                    }
                    Pair(likedJob.await(), ratedJob.await())
                }

                val mostLiked = likedDocs
                    .filter { it.likes > 0 }
                    .take(10)
                    .ifEmpty { likedDocs.sortedByDescending { it.averageScore }.take(10) }

                val topRated = ratedDocs
                    .filter { it.totalRatings >= 3 }
                    .take(10)

                // Solo platos recientes (30 días) para que "tendencias" no quede
                // dominado para siempre por platos antiguos con muchos likes acumulados.
                val recentCutoff = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
                val activePool = (likedDocs + ratedDocs).distinctBy { it.id }
                val recentPool = activePool.filter { it.createdAt >= recentCutoff }

                // Puntuación de engagement: likes valen doble que valoraciones
                val mostActive = recentPool.ifEmpty { activePool }
                    .sortedByDescending { it.likes * 2 + it.totalRatings }
                    .take(10)

                _uiState.value = _uiState.value.copy(
                    mostLiked = mostLiked,
                    topRated = topRated,
                    mostActive = mostActive,
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
