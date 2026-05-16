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
import com.app.foodranker.utils.InputLimits
import com.app.foodranker.utils.InputLimits.sanitized
import com.app.foodranker.utils.RewardManager
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isSubmittingRating: Boolean = false,
    val isSubmittingComment: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PlateDetailViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(PlateDetailUiState())
    val uiState: StateFlow<PlateDetailUiState> = _uiState

    fun loadPlate(plateId: String) {
        if (plateId.isBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Plato no válido")
            return
        }
        android.util.Log.d("PlateDetail", "Cargando plato con ID: $plateId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val plateDoc = firestore.collection("plates")
                    .document(plateId)
                    .get()
                    .await()

                android.util.Log.d("PlateDetail", "Documento existe: ${plateDoc.exists()}")

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

                val ratingsSnapshot = firestore.collection("ratings")
                    .whereEqualTo("plateId", plateId)
                    .get()
                    .await()
                val ratings = ratingsSnapshot.documents
                    .mapNotNull { it.toObject(Rating::class.java) }
                    .sortedByDescending { it.createdAt }

                val commentsSnapshot = firestore.collection("comments")
                    .whereEqualTo("plateId", plateId).get().await()
                val comments = commentsSnapshot.documents
                    .mapNotNull { it.toObject(Comment::class.java) }
                    .sortedByDescending { it.createdAt }

                val userId = auth.currentUser?.uid ?: ""
                val isSaved = if (userId.isNotEmpty()) {
                    try {
                        firestore.collection("saves").document("${userId}_${plateId}")
                            .get().await().exists()
                    } catch (e: Exception) { false }
                } else false

                _uiState.value = _uiState.value.copy(
                    plate = plate,
                    ratings = ratings,
                    comments = comments,
                    hasUserRated = ratings.any { it.userId == userId },
                    isLiked = plate?.likedByUsers?.contains(userId) == true,
                    isSaved = isSaved,
                    isLoading = false
                )
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
            try {
                val plateRef = firestore.collection("plates").document(plateId)
                if (isCurrentlyLiked) {
                    plateRef.update(
                        "likedByUsers", FieldValue.arrayRemove(userId),
                        "likes", FieldValue.increment(-1)
                    ).await()
                } else {
                    plateRef.update(
                        "likedByUsers", FieldValue.arrayUnion(userId),
                        "likes", FieldValue.increment(1)
                    ).await()
                    // Notificar al dueño del plato si no es el propio usuario
                    sendLikeNotification(plateId, plate, userId)
                }
            } catch (e: Exception) {
                // Revertir en caso de error
                _uiState.value = _uiState.value.copy(isLiked = isCurrentlyLiked, plate = plate)
                android.util.Log.e("PlateDetail", "Error toggleLike: ${e.message}")
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
                    .whereEqualTo("userId", userId).get().await()
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
                android.util.Log.e("PlateDetail", "addToCollection: ${e.message}")
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

    fun addComment(plateId: String, text: String) {
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
                    userName = user.displayName ?: "Usuario",
                    userPhotoUrl = user.photoUrl?.toString() ?: "",
                    text = clean
                )
                firestore.collection("comments").document(commentId).set(comment).await()
                _uiState.value = _uiState.value.copy(
                    comments = listOf(comment) + _uiState.value.comments,
                    isSubmittingComment = false
                )
                RewardManager.awardXP(user.uid, 5, firestore)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmittingComment = false)
            }
        }
    }

    fun deleteComment(commentId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val ref = firestore.collection("comments").document(commentId)
                val snap = ref.get().await()
                val authorId = snap.getString("userId") ?: return@launch
                if (authorId != uid) return@launch

                ref.delete().await()
                _uiState.value = _uiState.value.copy(
                    comments = _uiState.value.comments.filter { it.id != commentId }
                )
            } catch (e: Exception) { /* silencioso */ }
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

                val avgScore = ((flavorScore + presentationScore + valueScore) / 3.0)
                val ratingId = "${plateId}_${user.uid}"
                val rating = Rating(
                    id = ratingId,
                    plateId = plateId,
                    userId = user.uid,
                    userName = user.displayName ?: "Usuario",
                    userPhotoUrl = user.photoUrl?.toString() ?: "",
                    flavorScore = flavorScore.coerceIn(1f, 10f),
                    presentationScore = presentationScore.coerceIn(1f, 10f),
                    valueScore = valueScore.coerceIn(1f, 10f),
                    averageScore = avgScore,
                    comment = comment.sanitized(InputLimits.RATING_COMMENT)
                )

                firestore.collection("ratings").document(ratingId).set(rating).await()

                val plate = _uiState.value.plate ?: return@launch
                val allRatings = _uiState.value.ratings + rating
                val newAvg = allRatings.map { it.averageScore }.average()

                firestore.collection("plates").document(plateId).update(
                    mapOf("averageScore" to newAvg, "totalRatings" to allRatings.size)
                ).await()

                // Notificar al dueño del plato
                val ownerUserId = plate.addedByUserId
                if (ownerUserId.isNotEmpty() && ownerUserId != user.uid) {
                    val notifId = UUID.randomUUID().toString()
                    firestore.collection("notifications")
                        .document(ownerUserId)
                        .collection("items")
                        .document(notifId)
                        .set(mapOf(
                            "id" to notifId,
                            "type" to "rating",
                            "fromUserId" to user.uid,
                            "fromUserName" to (user.displayName ?: "Alguien"),
                            "plateId" to plateId,
                            "plateName" to plate.name,
                            "score" to avgScore,
                            "isRead" to false,
                            "createdAt" to System.currentTimeMillis()
                        )).await()
                }

                // XP para quien valora y para el dueño del plato
                RewardManager.awardXP(user.uid, RewardManager.XP_GIVE_RATING, firestore)
                if (plate.addedByUserId.isNotEmpty() && plate.addedByUserId != user.uid) {
                    RewardManager.awardXP(plate.addedByUserId, RewardManager.XP_RECEIVE_RATING, firestore)
                }
                RewardManager.checkAndAwardBadges(user.uid, firestore)

                _uiState.value = _uiState.value.copy(
                    ratings = allRatings,
                    hasUserRated = true,
                    isSubmittingRating = false,
                    plate = plate.copy(averageScore = newAvg, totalRatings = allRatings.size)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmittingRating = false, error = com.app.foodranker.utils.ErrorMapper.toUserMessage(e))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun awardXpFromAd() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                RewardManager.awardXP(uid, 50, firestore)
                _uiState.value = _uiState.value.copy(error = "+50 XP añadidos a tu perfil!")
            } catch (e: Exception) { /* silencioso */ }
        }
    }
}
