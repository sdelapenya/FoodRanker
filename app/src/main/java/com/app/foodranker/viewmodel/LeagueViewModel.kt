package com.app.foodranker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.app.foodranker.data.model.LeagueEntry
import com.app.foodranker.utils.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject

data class LeagueUiState(
    val entries: List<LeagueEntry> = emptyList(),
    val currentUserEntry: LeagueEntry? = null,
    val currentUserRank: Int = 0,
    // true cuando el usuario no aparece en el top cargado (limit 20)
    val userOutsideTop: Boolean = false,
    val city: String = "",
    val weekKey: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LeagueViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeagueUiState())
    val uiState: StateFlow<LeagueUiState> = _uiState

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    fun load() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val userSnap = firestore.collection("users").document(userId).get().await()
                val city = userSnap.getString("city") ?: ""

                if (city.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        city = "",
                        weekKey = currentWeekKey(),
                        error = "Añade tu ciudad en el perfil para participar en la liga"
                    )
                    return@launch
                }

                // El leagueId (ciudad normalizada + semana ISO) lo calcula siempre el
                // servidor: evita que Kotlin y Cloud Functions reimplementen la misma
                // lógica por separado y diverjan (causa raíz del bug de "liga vacía").
                val result = functions.getHttpsCallable("getLeagueId")
                    .call(mapOf("city" to city))
                    .await()
                @Suppress("UNCHECKED_CAST")
                val data = result.data as Map<String, Any>
                val leagueId = data["leagueId"] as String
                val weekKey = data["weekKey"] as String

                val snap = firestore.collection("leagues")
                    .document(leagueId)
                    .collection("entries")
                    .orderBy("xp", Query.Direction.DESCENDING)
                    .limit(20)
                    .get().await()

                val entries = snap.documents.mapNotNull { it.toObject(LeagueEntry::class.java) }
                val rankIndex = entries.indexOfFirst { it.userId == userId }
                val userRank = if (rankIndex >= 0) rankIndex + 1 else 0
                val userEntry = entries.firstOrNull { it.userId == userId }

                _uiState.value = _uiState.value.copy(
                    entries = entries,
                    currentUserEntry = userEntry,
                    currentUserRank = userRank,
                    userOutsideTop = userEntry == null && entries.isNotEmpty(),
                    city = city,
                    weekKey = weekKey,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorMapper.toUserMessage(e)
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    companion object {
        fun currentWeekKey(): String {
            // Solo para mostrar la semana en la pantalla "añade tu ciudad" (sin liga
            // todavía que consultar). El leagueId real siempre lo calcula el servidor
            // vía la CF getLeagueId — ver LeagueViewModel.load().
            val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"), java.util.Locale.GERMANY)
            val year = cal.getWeekYear()
            val week = cal.get(Calendar.WEEK_OF_YEAR)
            return "$year-W${week.toString().padStart(2, '0')}"
        }

        fun millisUntilNextMonday(): Long {
            // UTC obligatorio: debe ser coherente con currentWeekKey(), que también usa UTC.
            val utc = java.util.TimeZone.getTimeZone("UTC")
            val now = Calendar.getInstance(utc)
            val next = Calendar.getInstance(utc).apply {
                val today = get(Calendar.DAY_OF_WEEK)
                val daysUntil = ((Calendar.MONDAY - today + 7) % 7).let { if (it == 0) 7 else it }
                add(Calendar.DAY_OF_YEAR, daysUntil)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return maxOf(0L, next.timeInMillis - now.timeInMillis)
        }
    }
}
