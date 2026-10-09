package com.app.foodranker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.utils.ReferralManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.app.foodranker.R

data class ReferralUiState(
    val referralCode: String = "",
    val referralCount: Int = 0,
    val shareText: String = "",
    val isLoading: Boolean = false,
    val copiedFeedback: String? = null
)

@HiltViewModel
class ReferralViewModel @Inject constructor(
    private val referralManager: ReferralManager,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReferralUiState())
    val uiState: StateFlow<ReferralUiState> = _uiState

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val code = referralManager.getOrCreateReferralCode()
                val userId = auth.currentUser?.uid ?: ""
                val userName = auth.currentUser?.displayName ?: appContext.getString(R.string.vm_a_friend)
                val count = if (userId.isNotEmpty()) {
                    firestore.collection("users").document(userId).get().await()
                        .getLong("referralCount")?.toInt() ?: 0
                } else 0
                _uiState.value = _uiState.value.copy(
                    referralCode = code,
                    referralCount = count,
                    shareText = referralManager.buildReferralShareText(appContext, code, userName),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onCodeCopied() {
        _uiState.value = _uiState.value.copy(copiedFeedback = appContext.getString(R.string.vm_code_copied))
    }

    fun clearCopiedFeedback() {
        _uiState.value = _uiState.value.copy(copiedFeedback = null)
    }
}
