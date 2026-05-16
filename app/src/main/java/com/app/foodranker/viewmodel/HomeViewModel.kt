package com.app.foodranker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.PlateCategory
import com.app.foodranker.data.repository.PlateRepository
import com.app.foodranker.utils.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class HomeUiState(
    val topPlates: List<Plate> = emptyList(),
    val recentPlates: List<Plate> = emptyList(),
    val allPlates: List<Plate> = emptyList(),
    val selectedCategory: PlateCategory? = null,
    val unreadNotificationCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val plateRepository: PlateRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    private var notifListener: ListenerRegistration? = null
    private var isFirstNotifLoad = true

    init { loadHomeData() }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val topResult = plateRepository.getTopPlates(20)
            val recentResult = plateRepository.getRecentPlates(20)
            val allPlates = topResult.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(
                topPlates = allPlates,
                recentPlates = recentResult.getOrDefault(emptyList()),
                allPlates = allPlates,
                isLoading = false
            )
        }
    }

    fun selectCategory(category: PlateCategory?) {
        val allPlates = _uiState.value.allPlates
        val filtered = if (category == null) allPlates else allPlates.filter { it.category == category }
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            topPlates = filtered,
            recentPlates = filtered.sortedByDescending { it.createdAt }
        )
    }

    fun toggleLike(plateId: String) {
        val userId = currentUserId.ifEmpty { return }
        val plate = _uiState.value.allPlates.find { it.id == plateId } ?: return
        val isCurrentlyLiked = userId in plate.likedByUsers

        val updatedPlate = plate.copy(
            likes = if (isCurrentlyLiked) plate.likes - 1 else plate.likes + 1,
            likedByUsers = if (isCurrentlyLiked) plate.likedByUsers - userId
                           else plate.likedByUsers + userId
        )
        applyPlateUpdate(plateId, updatedPlate)

        viewModelScope.launch {
            val result = plateRepository.toggleLike(plateId, userId)
            if (result.isFailure) {
                applyPlateUpdate(plateId, plate)
                android.util.Log.e("HomeViewModel", "Error toggleLike: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private fun applyPlateUpdate(plateId: String, updatedPlate: Plate) {
        val allPlates = _uiState.value.allPlates.map { if (it.id == plateId) updatedPlate else it }
        val category = _uiState.value.selectedCategory
        val filtered = if (category == null) allPlates else allPlates.filter { it.category == category }
        _uiState.value = _uiState.value.copy(
            allPlates = allPlates,
            topPlates = filtered,
            recentPlates = filtered.sortedByDescending { it.createdAt }
        )
    }

    fun startListeningForNotifications(userId: String) {
        notifListener?.remove()
        isFirstNotifLoad = true
        notifListener = firestore.collection("notifications")
            .document(userId)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val unread = snapshot.documents.count { doc -> doc.get("isRead") != true }
                _uiState.value = _uiState.value.copy(unreadNotificationCount = unread)

                if (!isFirstNotifLoad) {
                    snapshot.documentChanges
                        .filter { it.type == DocumentChange.Type.ADDED }
                        .forEach { change ->
                            val data = change.document.data
                            val type = data["type"] as? String ?: return@forEach
                            val fromUser = data["fromUserName"] as? String ?: "Alguien"
                            val plateName = data["plateName"] as? String ?: "tu plato"
                            val plateId = data["plateId"] as? String
                            val (title, body) = when (type) {
                                "like" -> "❤️ Nuevo me gusta" to
                                        "$fromUser le ha dado like a \"$plateName\""
                                "rating" -> {
                                    val score = data["score"] as? Double ?: 0.0
                                    "⭐ Nueva valoración" to
                                            "$fromUser ha valorado \"$plateName\" con ${"%.1f".format(score)}"
                                }
                                else -> return@forEach
                            }
                            NotificationHelper.show(context, title, body, plateId)
                        }
                }
                isFirstNotifLoad = false
            }
    }

    fun markNotificationsRead(userId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("notifications")
                    .document(userId)
                    .collection("items")
                    .get()
                    .await()
                if (snapshot.documents.isEmpty()) return@launch
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.update(it.reference, "isRead", true) }
                batch.commit().await()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error markRead: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        notifListener?.remove()
    }
}
