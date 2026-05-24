package com.app.foodranker.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.data.model.User
import com.app.foodranker.utils.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class FollowListRow(
    val userId: String,
    val name: String,
    val photoUrl: String
)

data class FollowListUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val users: List<FollowListRow> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class FollowListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val profileUserId: String =
        savedStateHandle.get<String>("userId").orEmpty()
    private val listType: String =
        savedStateHandle.get<String>("listType") ?: "followers"

    private val _uiState = MutableStateFlow(
        FollowListUiState(title = titleFor(listType))
    )
    val uiState: StateFlow<FollowListUiState> = _uiState

    init {
        load()
    }

    fun load() {
        if (profileUserId.isBlank()) {
            _uiState.value = FollowListUiState(isLoading = false, error = "Usuario no válido")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val ids = if (listType == "following") {
                    firestore.collection("follows")
                        .whereEqualTo("followerId", profileUserId)
                        .limit(200).get().await()
                        .documents.mapNotNull { it.getString("followingId") }
                } else {
                    firestore.collection("follows")
                        .whereEqualTo("followingId", profileUserId)
                        .limit(200).get().await()
                        .documents.mapNotNull { it.getString("followerId") }
                }.distinct()

                val userMap = mutableMapOf<String, User>()
                ids.chunked(10).forEach { chunk ->
                    firestore.collection("users")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                        .get().await()
                        .documents.forEach { doc ->
                            doc.toObject(User::class.java)?.let { userMap[doc.id] = it }
                        }
                }
                val rows = ids.mapNotNull { uid ->
                    val user = userMap[uid] ?: return@mapNotNull null
                    FollowListRow(
                        userId = uid,
                        name = user.name.ifBlank { "Usuario" },
                        photoUrl = user.photoUrl
                    )
                }.sortedBy { it.name.lowercase() }

                _uiState.value = FollowListUiState(
                    isLoading = false,
                    title = titleFor(listType),
                    users = rows
                )
            } catch (e: Exception) {
                _uiState.value = FollowListUiState(
                    isLoading = false,
                    title = titleFor(listType),
                    error = ErrorMapper.toUserMessage(e)
                )
            }
        }
    }

    companion object {
        fun titleFor(type: String) =
            if (type == "following") "Siguiendo" else "Seguidores"
    }
}
