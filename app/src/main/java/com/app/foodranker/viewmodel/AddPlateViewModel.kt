package com.app.foodranker.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.PlateCategory
import com.app.foodranker.data.model.Rating
import com.app.foodranker.utils.CloudinaryManager
import com.app.foodranker.utils.FoodImageValidator
import com.app.foodranker.utils.InputLimits
import com.app.foodranker.utils.InputLimits.sanitized
import com.app.foodranker.utils.RemoteConfigManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    // uploadProgress: 0-100 (-1 = indeterminado / preparando)
    data class Loading(val uploadProgress: Int = -1) : AddPlateState()
    object Success : AddPlateState()
    data class Error(val message: String) : AddPlateState()
}

@HiltViewModel
class AddPlateViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow<AddPlateState>(AddPlateState.Idle)
    val state: StateFlow<AddPlateState> = _state

    // ── Estado del formulario (persiste al navegar hacia atrás) ───────────────
    var formName by mutableStateOf("")
    var formDescription by mutableStateOf("")
    var formCategory by mutableStateOf(PlateCategory.OTHER)
    var formRestaurantName by mutableStateOf("")
    var formRestaurantAddress by mutableStateOf("")
    var formCity by mutableStateOf("")
    var formCountry by mutableStateOf("")
    var formFlavorScore by mutableFloatStateOf(7f)
    var formPresentationScore by mutableFloatStateOf(7f)
    var formValueScore by mutableFloatStateOf(7f)
    var formComment by mutableStateOf("")
    var formImageUri by mutableStateOf<Uri?>(null)
    var formImageValidating by mutableStateOf(false)
    var formImageValidationError by mutableStateOf<String?>(null)
    var formCurrentStep by mutableIntStateOf(1)

    fun onImageSelected(uri: Uri?) {
        if (uri == null) return
        formImageUri = uri
        formImageValidating = true
        formImageValidationError = null
        viewModelScope.launch {
            val (ok, error) = FoodImageValidator.validate(appContext, uri)
            formImageValidating = false
            if (!ok) {
                formImageValidationError = error
                formImageUri = null
            }
        }
    }

    private fun resetForm() {
        formName = ""
        formDescription = ""
        formCategory = PlateCategory.OTHER
        formRestaurantName = ""
        formRestaurantAddress = ""
        formCity = ""
        formCountry = ""
        formFlavorScore = 7f
        formPresentationScore = 7f
        formValueScore = 7f
        formComment = ""
        formImageUri = null
        formImageValidating = false
        formImageValidationError = null
        formCurrentStep = 1
    }

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
            _state.value = AddPlateState.Loading(uploadProgress = -1)
            var uploadedImageUrl: String? = null
            try {
                val maxDaily = RemoteConfigManager.maxDailyPlates
                if (maxDaily > 0) {
                    // Medianoche local de hoy, no "hace 24h" (que permitiría más de
                    // maxDaily platos si se publican a horas distintas cada día).
                    val todayStart = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    // Requiere índice compuesto: addedByUserId ASC + createdAt ASC.
                    // FAILED_PRECONDITION = índice faltante → fail-open para no bloquear.
                    // Otros errores (red, auth) se relancen al catch exterior → error visible.
                    val todayCount = try {
                        firestore.collection("plates")
                            .whereEqualTo("addedByUserId", user.uid)
                            .whereGreaterThanOrEqualTo("createdAt", todayStart)
                            .limit((maxDaily + 1).toLong())
                            .get().await().size()
                    } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                        if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) 0
                        else throw e
                    }
                    if (todayCount >= maxDaily) {
                        _state.value = AddPlateState.Error("Has alcanzado el límite de $maxDaily platos por día")
                        return@launch
                    }
                }

                if (imageUri == null) {
                    _state.value = AddPlateState.Error("La foto es obligatoria")
                    return@launch
                }
                val imageUrl = uploadImageToCloudinary(context, imageUri).also { uploadedImageUrl = it }

                val safeFlavor = flavorScore.coerceIn(1f, 10f)
                val safePresentation = presentationScore.coerceIn(1f, 10f)
                val safeValue = valueScore.coerceIn(1f, 10f)
                val avgScore = (safeFlavor + safePresentation + safeValue) / 3.0
                val plateId  = UUID.randomUUID().toString()
                val ratingId = "${plateId}_${user.uid}"

                val safeUserName = (user.displayName ?: "Usuario").sanitized(InputLimits.USER_NAME)
                val now = System.currentTimeMillis()
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
                    totalRatings = 0,
                    createdAt = now,
                    status = com.app.foodranker.data.model.PlateStatus.PENDING
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
                    comment = cleanComment,
                    createdAt = now
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

                // averageScore, totalRatings, XP y badges los actualiza moderatePlateImage
                // (Cloud Function) cuando aprueba la imagen via Vision API.
                com.app.foodranker.utils.AnalyticsManager.logPlatePublished(category.name)

                // Verificar challenge semanal activo
                checkAndCompleteChallenge(user.uid, category.name, plateId)

                _state.value = AddPlateState.Success
            } catch (e: Exception) {
                uploadedImageUrl?.let { android.util.Log.w("AddPlateVM", "Imagen Cloudinary huérfana: $it — ${e.message}") }
                _state.value = AddPlateState.Error(com.app.foodranker.utils.ErrorMapper.toUserMessage(e))
            }
        }
    }

    private suspend fun uploadImageToCloudinary(context: Context, uri: Uri): String {
        return suspendCancellableCoroutine { continuation ->
            CloudinaryManager.uploadImage(
                context    = context,
                imageUri   = uri,
                onSuccess  = { url   -> continuation.resume(url) },
                onError    = { error -> continuation.resumeWithException(Exception(error)) },
                onProgress = { pct   -> _state.value = AddPlateState.Loading(uploadProgress = pct) }
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
            if (categoryMatches) {
                val challengeRef = firestore.collection("challenges").document(active.id)
                val alreadyIn = firestore.runTransaction { tx ->
                    @Suppress("UNCHECKED_CAST")
                    val ids = (tx.get(challengeRef).get("participantIds") as? List<String>) ?: emptyList()
                    if (userId in ids) return@runTransaction true
                    tx.update(challengeRef, mapOf(
                        "participantIds" to com.google.firebase.firestore.FieldValue.arrayUnion(userId),
                        "participantCount" to com.google.firebase.firestore.FieldValue.increment(1)
                    ))
                    false
                }.await()
                if (!alreadyIn) {
                    android.util.Log.d("Challenge", "✅ Challenge completado: ${active.id}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("Challenge", "No se pudo verificar challenge: ${e.message}")
        }
    }

    fun resetState() {
        _state.value = AddPlateState.Idle
        resetForm()
    }
}
