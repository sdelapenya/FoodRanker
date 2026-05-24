package com.app.foodranker.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.PlateCategory
import com.app.foodranker.data.model.Rating
import com.app.foodranker.utils.CloudinaryManager
import com.app.foodranker.utils.InputLimits
import com.app.foodranker.utils.InputLimits.sanitized
import com.app.foodranker.utils.RewardManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class AddPlateState {
    object Idle    : AddPlateState()
    object Loading : AddPlateState()
    object Success : AddPlateState()
    data class Error(val message: String) : AddPlateState()
}

@HiltViewModel
class AddPlateViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow<AddPlateState>(AddPlateState.Idle)
    val state: StateFlow<AddPlateState> = _state

    fun submitPlate(
        context: Context,
        name: String,
        description: String,
        category: PlateCategory,
        restaurantName: String,
        restaurantAddress: String,
        city: String,
        country: String,
        flavorScore: Float,
        presentationScore: Float,
        valueScore: Float,
        comment: String,
        imageUri: Uri?
    ) {
        val user = auth.currentUser ?: run {
            _state.value = AddPlateState.Error("Debes iniciar sesión")
            return
        }

        val cleanName = name.sanitized(InputLimits.PLATE_NAME)
        val cleanDescription = description.sanitized(InputLimits.PLATE_DESCRIPTION)
        val cleanRestaurant = restaurantName.sanitized(InputLimits.RESTAURANT_NAME)
        val cleanAddress = restaurantAddress.sanitized(InputLimits.RESTAURANT_NAME)
        val cleanCity = city.sanitized(InputLimits.CITY)
        val cleanCountry = country.sanitized(InputLimits.COUNTRY)
        val cleanComment = comment.sanitized(InputLimits.RATING_COMMENT)

        if (cleanName.isBlank()) {
            _state.value = AddPlateState.Error("El nombre del plato es obligatorio")
            return
        }

        viewModelScope.launch {
            _state.value = AddPlateState.Loading
            try {
                // Subir imagen a Cloudinary (obligatoria)
                val imageUrl = if (imageUri != null) {
                    uploadImageToCloudinary(context, imageUri)
                } else {
                    _state.value = AddPlateState.Error("La foto es obligatoria")
                    return@launch
                }

                val safeFlavor = flavorScore.coerceIn(1f, 10f)
                val safePresentation = presentationScore.coerceIn(1f, 10f)
                val safeValue = valueScore.coerceIn(1f, 10f)
                val avgScore = (safeFlavor + safePresentation + safeValue) / 3.0
                val plateId  = UUID.randomUUID().toString()
                val ratingId = "${plateId}_${user.uid}"

                val safeUserName = (user.displayName ?: "Usuario").sanitized(InputLimits.USER_NAME)
                val plate = Plate(
                    id = plateId,
                    name = cleanName,
                    description = cleanDescription,
                    category = category,
                    restaurantName = cleanRestaurant,
                    restaurantAddress = cleanAddress,
                    city = cleanCity,
                    country = cleanCountry,
                    imageUrl = imageUrl,
                    addedByUserId = user.uid,
                    addedByUserName = safeUserName,
                    averageScore = 0.0,
                    totalRatings = 0
                )
                val rating = Rating(
                    id = ratingId,
                    plateId = plateId,
                    userId = user.uid,
                    userName = safeUserName,
                    userPhotoUrl = user.photoUrl?.toString() ?: "",
                    flavorScore = safeFlavor,
                    presentationScore = safePresentation,
                    valueScore = safeValue,
                    averageScore = avgScore,
                    comment = cleanComment
                )

                // Plato + rating se crean atómicamente con un batch: o se crean
                // los dos o no se crea ninguno. El batch debe respetar la regla
                // "create plate con averageScore == 0", por eso el plate se crea
                // con score a cero y luego actualizamos el score con un update
                // independiente (que sí permite las reglas).
                val plateRef = firestore.collection("plates").document(plateId)
                val ratingRef = firestore.collection("ratings").document(ratingId)
                firestore.batch().apply {
                    set(plateRef, plate)
                    set(ratingRef, rating)
                }.commit().await()

                // Resumen del plato a partir de la rating recién creada.
                // Si esto fallara, el plato quedaría con score 0 hasta la
                // siguiente valoración, que recalculará el agregado.
                plateRef.update(mapOf(
                    "averageScore" to avgScore,
                    "totalRatings" to 1
                )).await()

                // Otorgar XP y comprobar badges (subir + valorar cuentan)
                RewardManager.awardXP(user.uid, RewardManager.XP_PLATE_WITH_PHOTO, firestore)
                RewardManager.awardXP(user.uid, RewardManager.XP_GIVE_RATING, firestore)
                RewardManager.checkAndAwardBadges(user.uid, firestore)
                com.app.foodranker.utils.AnalyticsManager.logPlatePublished(category.name)

                // Verificar challenge semanal activo
                checkAndCompleteChallenge(user.uid, category.name, plateId)

                _state.value = AddPlateState.Success
            } catch (e: Exception) {
                _state.value = AddPlateState.Error(com.app.foodranker.utils.ErrorMapper.toUserMessage(e))
            }
        }
    }

    private suspend fun uploadImageToCloudinary(context: Context, uri: Uri): String {
        return suspendCancellableCoroutine { continuation ->
            CloudinaryManager.uploadImage(
                context = context,
                imageUri = uri,
                onSuccess  = { url   -> continuation.resume(url) },
                onError    = { error -> continuation.resumeWithException(Exception(error)) }
            )
        }
    }

    private suspend fun checkAndCompleteChallenge(userId: String, categoryName: String, plateId: String) {
        try {
            val now = System.currentTimeMillis()
            val snap = firestore.collection("challenges")
                .whereLessThanOrEqualTo("startDate", now)
                .orderBy("startDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5).get().await()
            val active = snap.documents
                .mapNotNull { it.toObject(com.app.foodranker.data.model.WeeklyChallenge::class.java)?.copy(id = it.id) }
                .firstOrNull { it.endDate >= now } ?: return

            val categoryMatches = active.category.isBlank() || active.category.equals(categoryName, ignoreCase = true)
            val alreadyParticipating = userId in active.participantIds
            if (categoryMatches && !alreadyParticipating) {
                firestore.collection("challenges").document(active.id).update(
                    mapOf(
                        "participantIds" to com.google.firebase.firestore.FieldValue.arrayUnion(userId),
                        "participantCount" to com.google.firebase.firestore.FieldValue.increment(1)
                    )
                ).await()
                RewardManager.awardXP(userId, active.xpReward, firestore)
                android.util.Log.d("Challenge", "✅ Challenge completado: +${active.xpReward} XP")
            }
        } catch (e: Exception) {
            android.util.Log.w("Challenge", "No se pudo verificar challenge: ${e.message}")
        }
    }

    fun resetState() { _state.value = AddPlateState.Idle }
}
