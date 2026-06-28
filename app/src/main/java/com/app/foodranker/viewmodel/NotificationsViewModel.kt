package com.app.foodranker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.app.foodranker.data.model.FoodNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<FoodNotification> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState

    init { loadAndMarkRead() }

    fun clearAll() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("notifications")
                    .document(userId).collection("items")
                    .limit(500).get().await()
                if (snapshot.documents.isEmpty()) return@launch
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
                _uiState.value = NotificationsUiState(notifications = emptyList(), isLoading = false)
            } catch (e: Exception) {
                android.util.Log.e("NotificationsVM", "Error clearAll: ${e.message}")
            }
        }
    }

    fun loadAndMarkRead() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val snapshot = firestore.collection("notifications")
                    .document(userId).collection("items")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(100)
                    .get().await()

                val notifications = snapshot.documents.mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val reasonsList = (doc.get("reasons") as? List<String>) ?: emptyList()
                    FoodNotification(
                        id = doc.id,
                        type = doc.getString("type") ?: "",
                        fromUserName = doc.getString("fromUserName") ?: "",
                        plateId = doc.getString("plateId") ?: "",
                        plateName = doc.getString("plateName") ?: "",
                        score = doc.getDouble("score") ?: 0.0,
                        reasons = reasonsList,
                        isRead = doc.get("isRead") == true,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }.sortedByDescending { it.createdAt }

                // Mostrar primero, marcar leídas después (el usuario ya las ve)
                _uiState.value = NotificationsUiState(notifications = notifications, isLoading = false)

                val unread = snapshot.documents.filter { it.get("isRead") != true }
                if (unread.isNotEmpty()) {
                    val batch = firestore.batch()
                    unread.forEach { batch.update(it.reference, "isRead", true) }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
