package com.app.foodranker.viewmodel

import com.app.foodranker.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.Rating
import com.app.foodranker.data.repository.NotificationRepository
import com.app.foodranker.data.repository.PlateRepository
import com.app.foodranker.utils.InputLimits
import com.app.foodranker.utils.InputLimits.sanitized
import com.app.foodranker.utils.ConnectivityObserver
import com.app.foodranker.utils.DailyMissionManager
import com.app.foodranker.utils.MealDBSeeder
import com.app.foodranker.utils.ReferralManager
import com.app.foodranker.utils.RemoteConfigManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class DiscoverUiState(
    val plates: List<Plate> = emptyList(),
    val nearbyPlates: List<Plate> = emptyList(),
    val followingPlates: List<Plate> = emptyList(),
    val selectedTab: Int = 0,
    val userCity: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingFollowing: Boolean = false,
    val hasMorePlates: Boolean = true,
    val unreadNotificationCount: Int = 0,
    val seedingProgress: Pair<Int, Int>? = null,
    val reportFeedback: String? = null,
    val savedPlateIds: Set<String> = emptySet(),
    val saveFeedback: String? = null,
    val dailyMissionProgress: Int = 0,
    val dailyMissionGoal: Int = 3,
    val voteStreak: Int = 0
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val connectivityObserver: ConnectivityObserver,
    private val plateRepository: PlateRepository,
    private val referralManager: ReferralManager,
    private val dailyMissionManager: DailyMissionManager,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    // auth.currentUser?.displayName no es de fiar tras el login por navegador (ver
    // AuthRepository.signInWithGoogle) — Firestore users/{uid}.name es la fuente de verdad
    // ya corregida en el login. Mismo fallo y mismo arreglo que en
    // PlateDetailViewModel.resolveCurrentUserNameAndPhoto / AddPlateViewModel.
    private suspend fun resolveCurrentUserNameAndPhoto(): Pair<String, String> {
        val user = auth.currentUser
        val fallbackPhoto = user?.photoUrl?.toString() ?: ""
        return try {
            val snap = firestore.collection("users").document(user?.uid ?: "").get().await()
            val name = snap.getString("name")?.takeIf { it.isNotBlank() } ?: "Usuario"
            val photo = snap.getString("photoUrl")?.takeIf { it.isNotBlank() } ?: fallbackPhoto
            name.sanitized(InputLimits.USER_NAME) to photo
        } catch (e: Exception) {
            (user?.displayName?.takeIf { it.isNotBlank() } ?: "Usuario").sanitized(InputLimits.USER_NAME) to fallbackPhoto
        }
    }

    @Volatile private var lastDocument: DocumentSnapshot? = null
    private val PAGE_SIZE = 20L
    @Volatile private var lastLoadTime = 0L

    init {
        viewModelScope.launch {
            notificationRepository.unreadCount.collect { count ->
                _uiState.value = _uiState.value.copy(unreadNotificationCount = count)
            }
        }
    }

    fun applyInviteCode(code: String) {
        viewModelScope.launch {
            try { referralManager.applyReferralCode(code) }
            catch (e: Exception) {
                android.util.Log.w("DiscoverVM", "applyInviteCode error: ${e.message}")
            }
        }
    }

    fun setTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
        when (index) {
            1 -> if (_uiState.value.nearbyPlates.isEmpty()) loadNearbyPlates()
            2 -> if (_uiState.value.followingPlates.isEmpty()) loadFollowingPlates()
        }
    }

    fun loadNearbyPlates() {
        val city = _uiState.value.userCity.ifEmpty { return }
        viewModelScope.launch {
            try {
                // Requiere índice compuesto en Firebase Console:
                // Collection: plates | Fields: city ASC, rankingScore DESC
                val plates = firestore.collection("plates")
                    .whereEqualTo("city", city)
                    .orderBy("rankingScore", Query.Direction.DESCENDING)
                    .limit(30).get().await()
                    .documents.mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                    .filter { it.reportCount < 3 }
                _uiState.value = _uiState.value.copy(nearbyPlates = plates)
            } catch (e: Exception) {
                android.util.Log.e("DiscoverVM", "Error loadNearbyPlates: ${e.message}")
            }
        }
    }

    fun loadFollowingPlates() {
        val userId = currentUserId.ifEmpty { return }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingFollowing = true)
            try {
                // Obtener IDs de usuarios seguidos (hasta 90)
                val followsSnap = firestore.collection("follows")
                    .whereEqualTo("followerId", userId).limit(90).get().await()
                val followingIds = followsSnap.documents.mapNotNull { it.getString("followingId") }

                if (followingIds.isEmpty()) {
                    _uiState.value = _uiState.value.copy(followingPlates = emptyList(), isLoadingFollowing = false)
                    return@launch
                }

                // whereIn acepta máximo 30 IDs — dividir en batches paralelos
                val plates = coroutineScope {
                    followingIds.chunked(30).map { batch ->
                        async {
                            firestore.collection("plates")
                                .whereIn("addedByUserId", batch)
                                .orderBy("createdAt", Query.Direction.DESCENDING)
                                .limit(30).get().await()
                                .documents.mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                        }
                    }.flatMap { it.await() }
                }.filter { it.reportCount < 3 }
                    .sortedByDescending { it.createdAt }
                    .take(90)
                _uiState.value = _uiState.value.copy(followingPlates = plates, isLoadingFollowing = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingFollowing = false)
            }
        }
    }

    fun loadMorePlates() {
        val cursor = lastDocument ?: return
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMorePlates) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            try {
                val snapshot = firestore.collection("plates")
                    .orderBy("rankingScore", Query.Direction.DESCENDING)
                    .startAfter(cursor)
                    .limit(PAGE_SIZE)
                    .get().await()
                val docs = snapshot.documents
                lastDocument = docs.lastOrNull() ?: cursor
                val more = docs.mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                    .filter { it.reportCount < 3 }
                // Seguimos paginando mientras Firestore devuelva páginas completas,
                // aunque todos los docs de esta página estén filtrados por reportCount.
                val hasMore = docs.size >= PAGE_SIZE
                _uiState.value = _uiState.value.copy(
                    plates = _uiState.value.plates + more,
                    isLoadingMore = false,
                    hasMorePlates = hasMore
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun seedFromMealDB() {
        if (!BuildConfig.DEBUG) return
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(seedingProgress = Pair(0, 112))
            MealDBSeeder.seed { current, total ->
                _uiState.value = _uiState.value.copy(seedingProgress = Pair(current, total))
            }
            _uiState.value = _uiState.value.copy(seedingProgress = null)
            loadPlates()
        }
    }

    fun loadPlatesIfStale() {
        if (System.currentTimeMillis() - lastLoadTime < 60_000L) return
        loadPlates()
    }

    fun loadPlates() {
        lastDocument = null
        lastLoadTime = System.currentTimeMillis()
        viewModelScope.launch {
            // getProgress()/getStreak() leen SharedPreferences (I/O de disco) — fuera del
            // dispatcher Main para no bloquearlo en cada carga (cold start, refresh, etc.)
            val (missionProgress, streak) = withContext(Dispatchers.IO) {
                dailyMissionManager.getProgress() to dailyMissionManager.getStreak()
            }
            val missionGoal = RemoteConfigManager.dailyMissionGoal
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                hasMorePlates = true,
                dailyMissionProgress = missionProgress,
                dailyMissionGoal = missionGoal,
                voteStreak = streak
            )
            try {
                val snapshot = firestore.collection("plates")
                    .orderBy("rankingScore", Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE)
                    .get()
                    .await()
                val docs = snapshot.documents
                lastDocument = docs.lastOrNull()
                val plates = docs
                    .mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                    .filter { it.reportCount < 3 }

                _uiState.value = _uiState.value.copy(
                    plates = plates,
                    isLoading = false,
                    hasMorePlates = docs.size >= PAGE_SIZE
                )

                // Load saves + user city in parallel
                val userId = currentUserId
                if (userId.isNotEmpty()) {
                    coroutineScope {
                        val savesDeferred = async {
                            try {
                                firestore.collection("saves")
                                    .whereEqualTo("userId", userId).limit(500).get().await()
                                    .documents.mapNotNull { it.getString("plateId") }.toSet()
                            } catch (e: Exception) {
                                android.util.Log.w("DiscoverVM", "No se pudieron cargar saves: ${e.message}")
                                null
                            }
                        }
                        val cityDeferred = async {
                            try {
                                firestore.collection("users").document(userId)
                                    .get().await().getString("city") ?: ""
                            } catch (e: Exception) {
                                android.util.Log.w("DiscoverVM", "No se pudo cargar ciudad: ${e.message}")
                                ""
                            }
                        }
                        savesDeferred.await()?.let { savedIds ->
                            _uiState.value = _uiState.value.copy(savedPlateIds = savedIds)
                        }
                        val city = cityDeferred.await()
                        if (city.isNotEmpty()) {
                            _uiState.value = _uiState.value.copy(userCity = city)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DiscoverVM", "Error cargando platos: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun toggleSave(plateId: String) {
        val userId = currentUserId.ifEmpty { return }
        val saveId = "${userId}_${plateId}"
        val isSaved = plateId in _uiState.value.savedPlateIds
        // Update optimista: icono y mensaje van juntos y de inmediato. Firestore
        // tiene caché persistente, así que sin red el write queda encolado y su
        // await() no resuelve hasta que el servidor confirma — si el mensaje
        // esperase a eso, offline el icono se marcaría y el usuario no vería
        // ninguna confirmación. Si el write acaba fallando, se revierte abajo.
        _uiState.value = _uiState.value.copy(
            savedPlateIds = if (isSaved)
                _uiState.value.savedPlateIds - plateId
            else
                _uiState.value.savedPlateIds + plateId,
            saveFeedback = if (isSaved) "Eliminado de guardados"
                           else "🔖 Guardado en tu colección"
        )
        viewModelScope.launch {
            try {
                val ref = firestore.collection("saves").document(saveId)
                if (isSaved) ref.delete().await()
                else ref.set(mapOf(
                    "userId" to userId,
                    "plateId" to plateId,
                    "createdAt" to System.currentTimeMillis()
                )).await()
            } catch (e: Exception) {
                // Revertir
                _uiState.value = _uiState.value.copy(
                    savedPlateIds = if (!isSaved)
                        _uiState.value.savedPlateIds - plateId
                    else
                        _uiState.value.savedPlateIds + plateId,
                    saveFeedback = "No se pudo actualizar tus guardados"
                )
            }
        }
    }

    fun clearSaveFeedback() {
        _uiState.value = _uiState.value.copy(saveFeedback = null)
    }

    fun toggleLike(plateId: String) {
        val userId = currentUserId.ifEmpty { return }
        val plate = _uiState.value.plates.find { it.id == plateId }
            ?: _uiState.value.followingPlates.find { it.id == plateId }
            ?: return
        val isCurrentlyLiked = userId in plate.likedByUsers

        applyPlateUpdate(plateId, plate.copy(
            likes = if (isCurrentlyLiked) plate.likes - 1 else plate.likes + 1,
            likedByUsers = if (isCurrentlyLiked) plate.likedByUsers - userId
                           else plate.likedByUsers + userId
        ))

        viewModelScope.launch {
            val result = plateRepository.toggleLike(plateId, userId)
            if (result.isFailure) {
                applyPlateUpdate(plateId, plate)
            } else {
                val nowLiked = result.getOrDefault(false)
                if (nowLiked) {
                    com.app.foodranker.utils.AnalyticsManager.logPlateLiked(plateId)
                    sendLikeNotification(plateId, plate, userId)
                } else {
                    com.app.foodranker.utils.AnalyticsManager.logPlateUnliked(plateId)
                }
            }
        }
    }

    private suspend fun sendLikeNotification(plateId: String, plate: Plate, fromUserId: String) {
        val ownerUserId = plate.addedByUserId
        if (ownerUserId.isEmpty() || ownerUserId == fromUserId) return
        try {
            val notifId = UUID.randomUUID().toString()
            firestore.collection("notifications").document(ownerUserId).collection("items")
                .document(notifId).set(mapOf(
                    "id" to notifId, "type" to "like",
                    "fromUserId" to (auth.currentUser?.uid ?: ""),
                    "fromUserName" to (auth.currentUser?.displayName ?: "Alguien"),
                    "plateId" to plateId, "plateName" to plate.name,
                    "isRead" to false, "createdAt" to System.currentTimeMillis()
                )).await()
        } catch (e: Exception) { /* silencioso */ }
    }

    private fun applyPlateUpdate(plateId: String, updated: Plate) {
        _uiState.value = _uiState.value.copy(
            plates = _uiState.value.plates.map { if (it.id == plateId) updated else it },
            nearbyPlates = _uiState.value.nearbyPlates.map { if (it.id == plateId) updated else it },
            followingPlates = _uiState.value.followingPlates.map { if (it.id == plateId) updated else it }
        )
    }

    fun startListeningForNotifications(userId: String) {
        notificationRepository.startListening(userId)
    }

    fun reportPlate(plateId: String, reason: String) {
        val userId = currentUserId.ifEmpty { return }
        if (plateId.isBlank()) return
        val cleanReason = reason.sanitized(InputLimits.REPORT_REASON)
        viewModelScope.launch {
            try {
                // ID determinístico: un usuario solo puede tener un reporte por plato.
                // La transacción lee el doc y el contador en el mismo bloque atómico,
                // eliminando el TOCTOU del patrón check-then-increment previo.
                val reportId = "${userId}_${plateId}"
                val reportRef = firestore.collection("reports").document(reportId)
                val plateRef = firestore.collection("plates").document(plateId)
                val alreadyReported = firestore.runTransaction { tx ->
                    if (tx.get(reportRef).exists()) return@runTransaction true
                    tx.set(reportRef, mapOf(
                        "id" to reportId,
                        "plateId" to plateId,
                        "reportedByUserId" to userId,
                        "reason" to cleanReason,
                        "createdAt" to System.currentTimeMillis()
                    ))
                    tx.update(plateRef, "reportCount", FieldValue.increment(1))
                    false
                }.await()
                _uiState.value = _uiState.value.copy(
                    reportFeedback = if (alreadyReported)
                        "Ya has reportado este plato anteriormente"
                    else
                        "Reporte enviado. Gracias por ayudarnos a mantener la comunidad."
                )
            } catch (e: Exception) {
                android.util.Log.e("DiscoverVM", "Error al reportar: ${e.message}")
            }
        }
    }

    fun clearReportFeedback() {
        _uiState.value = _uiState.value.copy(reportFeedback = null)
    }

}
