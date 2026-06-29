package com.app.foodranker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.data.model.Comment
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.PlateCategory
import com.app.foodranker.data.model.Rating
import com.app.foodranker.data.repository.PlateRepository
import com.app.foodranker.utils.InputLimits
import com.google.firebase.functions.FirebaseFunctions
import com.app.foodranker.utils.InputLimits.sanitized
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

data class PlateDetailUiState(
    val plate: Plate? = null,
    val ratings: List<Rating> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val collections: List<com.app.foodranker.data.model.PlateCollection> = emptyList(),
    val isLoading: Boolean = false,
    val hasUserRated: Boolean = false,
    val currentUserId: String = "",
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isSubmittingRating: Boolean = false,
    val isSubmittingComment: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val cityRank: Int = 0
) {
    // Derivado de ratings para evitar desincronización con el campo separado
    val userRating: Rating? get() = ratings.find { it.userId == currentUserId }
}

@HiltViewModel
class PlateDetailViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val plateRepository: PlateRepository,
    private val functions: FirebaseFunctions
) : ViewModel() {

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(PlateDetailUiState())
    val uiState: StateFlow<PlateDetailUiState> = _uiState
    @Volatile private var lastLoadTime = 0L

    fun loadPlateIfStale(plateId: String) {
        if (System.currentTimeMillis() - lastLoadTime < 60_000L) return
        loadPlate(plateId)
    }

    fun loadPlate(plateId: String) {
        if (plateId.isBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Plato no válido")
            return
        }
        if (com.app.foodranker.BuildConfig.DEBUG) android.util.Log.d("PlateDetail", "Cargando plato con ID: $plateId")
        lastLoadTime = System.currentTimeMillis()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val userId = auth.currentUser?.uid ?: ""
                coroutineScope {
                    val plateDeferred = async {
                        firestore.collection("plates").document(plateId).get().await()
                    }
                    val ratingsDeferred = async {
                        firestore.collection("ratings")
                            .whereEqualTo("plateId", plateId)
                            .limit(50).get().await()
                    }
                    val commentsDeferred = async {
                        firestore.collection("comments")
                            .whereEqualTo("plateId", plateId)
                            .limit(50).get().await()
                    }
                    val isSavedDeferred = async {
                        if (userId.isNotEmpty()) {
                            try {
                                firestore.collection("saves").document("${userId}_${plateId}")
                                    .get().await().exists()
                            } catch (e: Exception) { false }
                        } else false
                    }

                    val plateDoc = plateDeferred.await()
                    if (com.app.foodranker.BuildConfig.DEBUG) android.util.Log.d("PlateDetail", "Documento existe: ${plateDoc.exists()}")

                    val plate = if (plateDoc.exists()) {
                        plateDoc.toObject(Plate::class.java)?.copy(id = plateDoc.id)
                            ?: plateDoc.data?.let { data ->
                                @Suppress("UNCHECKED_CAST")
                                Plate(
                                    id = plateDoc.id,
                                    name = data["name"] as? String ?: "",
                                    description = data["description"] as? String ?: "",
                                    category = try {
                                        PlateCategory.valueOf(data["category"] as? String ?: "OTHER")
                                    } catch (e: Exception) { PlateCategory.OTHER },
                                    restaurantName = data["restaurantName"] as? String ?: "",
                                    restaurantAddress = data["restaurantAddress"] as? String ?: "",
                                    city = data["city"] as? String ?: "",
                                    country = data["country"] as? String ?: "",
                                    imageUrl = data["imageUrl"] as? String ?: "",
                                    addedByUserId = data["addedByUserId"] as? String ?: "",
                                    addedByUserName = data["addedByUserName"] as? String ?: "",
                                    averageScore = (data["averageScore"] as? Double) ?: 0.0,
                                    totalRatings = (data["totalRatings"] as? Long)?.toInt() ?: 0,
                                    createdAt = (data["createdAt"] as? Long) ?: 0L,
                                    likes = (data["likes"] as? Long)?.toInt() ?: 0,
                                    likedByUsers = (data["likedByUsers"] as? List<*>)
                                        ?.filterIsInstance<String>() ?: emptyList()
                                )
                            }
                    } else null

                    // City rank: plates in same city with higher score, run in parallel
                    val cityRankDeferred = if (plate != null && plate.city.isNotEmpty()) {
                        async {
                            try {
                                firestore.collection("plates")
                                    .whereEqualTo("status", com.app.foodranker.data.model.PlateStatus.APPROVED)
                                    .whereEqualTo("city", plate.city)
                                    .whereGreaterThan("averageScore", plate.averageScore)
                                    .limit(100).get().await().size() + 1
                            } catch (e: Exception) { 0 }
                        }
                    } else null

                    val ratings = ratingsDeferred.await().documents
                        .mapNotNull { it.toObject(Rating::class.java) }
                        .sortedByDescending { it.createdAt }

                    val comments = commentsDeferred.await().documents
                        .mapNotNull { it.toObject(Comment::class.java) }
                        .sortedByDescending { it.createdAt }

                    val isSaved = isSavedDeferred.await()
                    val cityRank = cityRankDeferred?.await() ?: 0

                    _uiState.value = _uiState.value.copy(
                        plate = plate,
                        ratings = ratings,
                        comments = comments,
                        hasUserRated = ratings.any { it.userId == userId },
                        currentUserId = userId,
                        isLiked = plate?.likedByUsers?.contains(userId) == true,
                        isSaved = isSaved,
                        cityRank = cityRank,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("PlateDetail", "Error: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = com.app.foodranker.utils.ErrorMapper.toUserMessage(e))
            }
        }
    }

    fun toggleLike(plateId: String) {
        val userId = auth.currentUser?.uid ?: return
        val plate = _uiState.value.plate ?: return
        val isCurrentlyLiked = _uiState.value.isLiked

        // Actualización optimista
        _uiState.value = _uiState.value.copy(
            isLiked = !isCurrentlyLiked,
            plate = plate.copy(likes = if (isCurrentlyLiked) plate.likes - 1 else plate.likes + 1)
        )

        viewModelScope.launch {
            val result = plateRepository.toggleLike(plateId, userId)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(isLiked = isCurrentlyLiked, plate = plate)
                android.util.Log.e("PlateDetail", "Error toggleLike: ${result.exceptionOrNull()?.message}")
            } else {
                val nowLiked = result.getOrDefault(false)
                if (nowLiked) sendLikeNotification(plateId, plate, userId)
            }
        }
    }

    private suspend fun sendLikeNotification(plateId: String, plate: Plate, fromUserId: String) {
        val ownerUserId = plate.addedByUserId
        if (ownerUserId.isEmpty() || ownerUserId == fromUserId) return
        try {
            val notifId = UUID.randomUUID().toString()
            firestore.collection("notifications")
                .document(ownerUserId)
                .collection("items")
                .document(notifId)
                .set(mapOf(
                    "id" to notifId,
                    "type" to "like",
                    "fromUserId" to (auth.currentUser?.uid ?: ""),
                    "fromUserName" to (auth.currentUser?.displayName ?: "Alguien"),
                    "plateId" to plateId,
                    "plateName" to plate.name,
                    "isRead" to false,
                    "createdAt" to System.currentTimeMillis()
                )).await()
        } catch (e: Exception) {
            android.util.Log.e("PlateDetail", "Error enviando notif like: ${e.message}")
        }
    }

    fun loadUserCollections() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snap = firestore.collection("collections")
                    .whereEqualTo("userId", userId).limit(50).get().await()
                val cols = snap.documents.mapNotNull {
                    it.toObject(com.app.foodranker.data.model.PlateCollection::class.java)
                }
                _uiState.value = _uiState.value.copy(collections = cols)
            } catch (e: Exception) {
                android.util.Log.e("PlateDetail", "loadUserCollections: ${e.message}")
            }
        }
    }

    fun addToCollection(collectionId: String, plateId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("collections").document(collectionId)
                    .update("plateIds", com.google.firebase.firestore.FieldValue.arrayUnion(plateId)).await()
                _uiState.value = _uiState.value.copy(
                    collections = _uiState.value.collections.map { col ->
                        if (col.id == collectionId) col.copy(plateIds = col.plateIds + plateId) else col
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = com.app.foodranker.utils.ErrorMapper.toUserMessage(e))
            }
        }
    }

    fun toggleSave(plateId: String) {
        val userId = auth.currentUser?.uid ?: return
        val saveId = "${userId}_${plateId}"
        val isSaved = _uiState.value.isSaved
        _uiState.value = _uiState.value.copy(isSaved = !isSaved)
        viewModelScope.launch {
            try {
                val ref = firestore.collection("saves").document(saveId)
                if (isSaved) ref.delete().await()
                else ref.set(mapOf(
                    "userId" to userId, "plateId" to plateId,
                    "createdAt" to System.currentTimeMillis()
                )).await()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaved = isSaved)
            }
        }
    }

    fun addComment(plateId: String, text: String, onSuccess: () -> Unit = {}) {
        val user = auth.currentUser ?: return
        val clean = text.sanitized(InputLimits.COMMENT_TEXT)
        if (clean.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingComment = true)
            try {
                val commentId = UUID.randomUUID().toString()
                val comment = Comment(
                    id = commentId, plateId = plateId,
                    userId = user.uid,
                    userName = (user.displayName ?: "Usuario").sanitized(InputLimits.USER_NAME),
                    userPhotoUrl = user.photoUrl?.toString() ?: "",
                    text = clean,
                    createdAt = System.currentTimeMillis()
                )
                firestore.collection("comments").document(commentId).set(comment).await()
                _uiState.value = _uiState.value.copy(
                    comments = listOf(comment) + _uiState.value.comments,
                    isSubmittingComment = false
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmittingComment = false)
            }
        }
    }

    fun deleteComment(commentId: String) {
        if (auth.currentUser == null) return
        viewModelScope.launch {
            try {
                firestore.collection("comments").document(commentId).delete().await()
                _uiState.value = _uiState.value.copy(
                    comments = _uiState.value.comments.filter { it.id != commentId }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = com.app.foodranker.utils.ErrorMapper.toUserMessage(e))
            }
        }
    }

    fun submitRating(
        plateId: String,
        flavorScore: Float,
        presentationScore: Float,
        valueScore: Float,
        comment: String
    ) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingRating = true)
            try {
                val existingSnap = firestore.collection("ratings")
                    .document("${plateId}_${user.uid}")
                    .get().await()
                if (existingSnap.exists()) {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingRating = false,
                        error = "Ya valoraste este plato"
                    )
                    return@launch
                }

                val safeFlavor = flavorScore.coerceIn(1f, 10f)
                val safePresentation = presentationScore.coerceIn(1f, 10f)
                val safeValue = valueScore.coerceIn(1f, 10f)
                val avgScore = ((safeFlavor + safePresentation + safeValue) / 3.0)
                val ratingId = "${plateId}_${user.uid}"
                val rating = Rating(
                    id = ratingId,
                    plateId = plateId,
                    userId = user.uid,
                    userName = (user.displayName ?: "Usuario").sanitized(InputLimits.USER_NAME),
                    userPhotoUrl = user.photoUrl?.toString() ?: "",
                    flavorScore = safeFlavor,
                    presentationScore = safePresentation,
                    valueScore = safeValue,
                    averageScore = avgScore,
                    comment = comment.sanitized(InputLimits.RATING_COMMENT),
                    createdAt = System.currentTimeMillis()
                )

                firestore.collection("ratings").document(ratingId).set(rating).await()

                // Score, XP, badges y notificación los actualiza onRatingCreated (Cloud Function).
                // Calculamos el score localmente solo para el optimistic update de la UI.
                val plate = _uiState.value.plate ?: return@launch
                val oldCount = plate.totalRatings
                val newCount = oldCount + 1
                val newAvg = (plate.averageScore * oldCount + avgScore) / newCount

                _uiState.value = _uiState.value.copy(
                    ratings = listOf(rating) + _uiState.value.ratings,
                    hasUserRated = true,
                    isSubmittingRating = false,
                    plate = plate.copy(averageScore = newAvg, totalRatings = newCount)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmittingRating = false, error = com.app.foodranker.utils.ErrorMapper.toUserMessage(e))
            }
        }
    }

    fun editRating(
        plateId: String,
        flavorScore: Float,
        presentationScore: Float,
        valueScore: Float,
        comment: String
    ) {
        val user = auth.currentUser ?: return
        val ratingId = "${plateId}_${user.uid}"
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingRating = true)
            try {
                val safeFlavor = flavorScore.coerceIn(1f, 10f)
                val safePresentation = presentationScore.coerceIn(1f, 10f)
                val safeValue = valueScore.coerceIn(1f, 10f)
                val avgScore = (safeFlavor + safePresentation + safeValue) / 3.0
                val cleanComment = comment.sanitized(InputLimits.RATING_COMMENT)
                firestore.collection("ratings").document(ratingId).update(
                    mapOf(
                        "flavorScore" to safeFlavor,
                        "presentationScore" to safePresentation,
                        "valueScore" to safeValue,
                        "averageScore" to avgScore,
                        "comment" to cleanComment
                    )
                ).await()
                val updatedRating = _uiState.value.userRating?.copy(
                    flavorScore = safeFlavor,
                    presentationScore = safePresentation,
                    valueScore = safeValue,
                    averageScore = avgScore,
                    comment = cleanComment
                )
                _uiState.value = _uiState.value.copy(
                    isSubmittingRating = false,
                    // userRating se deriva automáticamente de ratings (no campo separado)
                    ratings = _uiState.value.ratings.map { if (it.id == ratingId) updatedRating ?: it else it },
                    successMessage = "¡Valoración actualizada!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingRating = false,
                    error = com.app.foodranker.utils.ErrorMapper.toUserMessage(e)
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun awardXpFromAd() {
        if (auth.currentUser == null) return
        viewModelScope.launch {
            try {
                functions.getHttpsCallable("awardAdXp").call().await()
                _uiState.value = _uiState.value.copy(successMessage = "¡+50 XP ganados por ver el anuncio! ⭐")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = com.app.foodranker.utils.ErrorMapper.toUserMessage(e))
            }
        }
    }
}
