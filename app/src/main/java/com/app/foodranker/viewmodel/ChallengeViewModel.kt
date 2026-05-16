package com.app.foodranker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.data.model.WeeklyChallenge
import com.app.foodranker.utils.RewardManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ChallengeUiState(
    val currentChallenge: WeeklyChallenge? = null,
    val isParticipating: Boolean = false,
    val isLoading: Boolean = false,
    val justCompleted: Boolean = false
)

@HiltViewModel
class ChallengeViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengeUiState())
    val uiState: StateFlow<ChallengeUiState> = _uiState

    init { loadCurrentChallenge() }

    fun loadCurrentChallenge() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val now = System.currentTimeMillis()
                val snap = firestore.collection("challenges")
                    .whereLessThanOrEqualTo("startDate", now)
                    .limit(5).get().await()

                val active = snap.documents
                    .mapNotNull { it.toObject(WeeklyChallenge::class.java)?.copy(id = it.id) }
                    .firstOrNull { it.endDate >= now }

                val userId = auth.currentUser?.uid ?: ""
                val isParticipating = active?.participantIds?.contains(userId) == true

                _uiState.value = ChallengeUiState(
                    currentChallenge = active,
                    isParticipating = isParticipating,
                    isLoading = false
                )
            } catch (e: Exception) {
                android.util.Log.e("Challenge", "loadCurrentChallenge: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
                // Los challenges se crean desde Firebase Console, no desde la app
            }
        }
    }

    fun participate() {
        val userId = auth.currentUser?.uid ?: return
        val challenge = _uiState.value.currentChallenge ?: return
        if (_uiState.value.isParticipating) return

        viewModelScope.launch {
            try {
                firestore.collection("challenges").document(challenge.id).update(
                    mapOf(
                        "participantIds" to FieldValue.arrayUnion(userId),
                        "participantCount" to FieldValue.increment(1)
                    )
                ).await()
                RewardManager.awardXP(userId, challenge.xpReward, firestore)
                _uiState.value = _uiState.value.copy(isParticipating = true, justCompleted = true)
            } catch (e: Exception) { }
        }
    }

    fun clearJustCompleted() {
        _uiState.value = _uiState.value.copy(justCompleted = false)
    }


}
