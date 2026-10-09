package com.app.foodranker.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.data.model.User
import com.app.foodranker.ui.Screen
import com.app.foodranker.utils.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.app.foodranker.R
import androidx.annotation.StringRes

data class FollowListRow(
    val userId: String,
    val name: String,
    val photoUrl: String
)

data class FollowListUiState(
    val isLoading: Boolean = true,
    // Id de recurso, no texto: el titulo lo ponia el ViewModel con "Seguidores" /
    // "Siguiendo" clavados, y salian en castellano con la app en ingles. Asi ademas
    // sigue a un cambio de idioma sin tener que recrear nada.
    @StringRes val titleRes: Int = R.string.fl_followers_title,
    val listType: String = "followers",
    val users: List<FollowListRow> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class FollowListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val profileUserId: String =
        savedStateHandle.get<String>("userId").orEmpty()
    private val listType: String =
        savedStateHandle.get<String>("listType") ?: Screen.FollowList.LIST_FOLLOWERS

    private val _uiState = MutableStateFlow(
        FollowListUiState(titleRes = titleFor(listType), listType = listType)
    )
    val uiState: StateFlow<FollowListUiState> = _uiState

    init {
        load()
    }

    fun load() {
        if (profileUserId.isBlank()) {
            _uiState.value = FollowListUiState(isLoading = false, error = appContext.getString(R.string.vm_user_invalid))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val ids = if (listType == Screen.FollowList.LIST_FOLLOWING) {
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
                        name = user.name.ifBlank {
                            appContext.getString(R.string.user_fallback_name)
                        },
                        photoUrl = user.photoUrl
                    )
                }.sortedBy { it.name.lowercase() }

                _uiState.value = FollowListUiState(
                    isLoading = false,
                    titleRes = titleFor(listType),
                    listType = listType,
                    users = rows
                )
            } catch (e: Exception) {
                _uiState.value = FollowListUiState(
                    isLoading = false,
                    titleRes = titleFor(listType),
                    listType = listType,
                    error = ErrorMapper.toUserMessage(e)
                )
            }
        }
    }

    companion object {
        @StringRes
        fun titleFor(type: String) =
            if (type == Screen.FollowList.LIST_FOLLOWING) R.string.fl_following_title
            else R.string.fl_followers_title
    }
}
