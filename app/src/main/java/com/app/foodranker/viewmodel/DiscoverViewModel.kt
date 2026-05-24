package com.app.foodranker.viewmodel

import android.content.Context
import com.app.foodranker.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.Rating
import com.app.foodranker.utils.InputLimits
import com.app.foodranker.utils.InputLimits.sanitized
import com.app.foodranker.utils.ConnectivityObserver
import com.app.foodranker.utils.MealDBSeeder
import com.app.foodranker.utils.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

data class DiscoverUiState(
    val plates: List<Plate> = emptyList(),
    val isLoading: Boolean = false,
    val unreadNotificationCount: Int = 0,
    val seedingProgress: Pair<Int, Int>? = null,
    val reportFeedback: String? = null,
    val ratingFeedback: String? = null,
    val savedPlateIds: Set<String> = emptySet(),
    val isLoadingMore: Boolean = false,
    val hasMorePlates: Boolean = true,
    val feedMode: FeedMode = FeedMode.FOR_YOU,
    val followingPlates: List<Plate> = emptyList(),
    val isLoadingFollowing: Boolean = false
)

enum class FeedMode { FOR_YOU, FOLLOWING }

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    private var notifListener: ListenerRegistration? = null
    @Volatile private var lastDocument: DocumentSnapshot? = null
    private val PAGE_SIZE = 20L
    @Volatile private var isFirstNotifLoad = true
    @Volatile private var lastLoadTime = 0L

    fun setFeedMode(mode: FeedMode) {
        _uiState.value = _uiState.value.copy(feedMode = mode)
        if (mode == FeedMode.FOLLOWING && _uiState.value.followingPlates.isEmpty()) {
            loadFollowingPlates()
        }
    }

    fun loadFollowingPlates() {
        val userId = currentUserId.ifEmpty { return }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingFollowing = true)
            try {
                // Obtener IDs de usuarios seguidos
                val followsSnap = firestore.collection("follows")
                    .whereEqualTo("followerId", userId).limit(30).get().await()
                val followingIds = followsSnap.documents.mapNotNull { it.getString("followingId") }

                if (followingIds.isEmpty()) {
                    _uiState.value = _uiState.value.copy(followingPlates = emptyList(), isLoadingFollowing = false)
                    return@launch
                }

                // Cargar platos de esos usuarios
                val platesSnap = firestore.collection("plates")
                    .whereIn("addedByUserId", followingIds.take(30))
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(30).get().await()
                val plates = platesSnap.documents
                    .mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                    .filter { it.reportCount < 3 }
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
                    .orderBy("averageScore", Query.Direction.DESCENDING)
                    .startAfter(cursor)
                    .limit(PAGE_SIZE)
                    .get().await()
                val docs = snapshot.documents
                lastDocument = docs.lastOrNull() ?: cursor
                val more = docs.mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                    .filter { it.reportCount < 3 }
                _uiState.value = _uiState.value.copy(
                    plates = _uiState.value.plates + more,
                    isLoadingMore = false,
                    hasMorePlates = docs.size >= PAGE_SIZE
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun seedFromMealDB() {
        val userId = currentUserId.ifEmpty { return }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(seedingProgress = Pair(0, 112))
            var inserted = 0

            val mealDbCategories = listOf(
                "Seafood" to com.app.foodranker.data.model.PlateCategory.SEAFOOD,
                "Pasta"   to com.app.foodranker.data.model.PlateCategory.PASTA,
                "Beef"    to com.app.foodranker.data.model.PlateCategory.STEAK,
                "Dessert" to com.app.foodranker.data.model.PlateCategory.DESSERT,
                "Breakfast" to com.app.foodranker.data.model.PlateCategory.BREAKFAST,
                "Chicken" to com.app.foodranker.data.model.PlateCategory.OTHER,
                "Starter" to com.app.foodranker.data.model.PlateCategory.TAPAS,
                "Lamb"    to com.app.foodranker.data.model.PlateCategory.STEAK,
                "Miscellaneous" to com.app.foodranker.data.model.PlateCategory.OTHER,
                "Pork"    to com.app.foodranker.data.model.PlateCategory.STEAK,
                "Vegan"   to com.app.foodranker.data.model.PlateCategory.SALAD,
                "Vegetarian" to com.app.foodranker.data.model.PlateCategory.SALAD,
                "Side"    to com.app.foodranker.data.model.PlateCategory.TAPAS,
                "Goat"    to com.app.foodranker.data.model.PlateCategory.STEAK
            )
            val total = mealDbCategories.size * 8
            _uiState.value = _uiState.value.copy(seedingProgress = Pair(0, total))

            // Prefetch Pexels images por categoría (alta resolución 1880px)
            val pexelsPool = mutableMapOf<com.app.foodranker.data.model.PlateCategory, MutableList<String>>()
            val pexelsQueries = mapOf(
                com.app.foodranker.data.model.PlateCategory.SEAFOOD   to "seafood dish restaurant",
                com.app.foodranker.data.model.PlateCategory.PASTA     to "pasta italian dish",
                com.app.foodranker.data.model.PlateCategory.STEAK     to "grilled meat steak",
                com.app.foodranker.data.model.PlateCategory.DESSERT   to "dessert cake sweet",
                com.app.foodranker.data.model.PlateCategory.BREAKFAST to "breakfast food plate",
                com.app.foodranker.data.model.PlateCategory.TAPAS     to "appetizer starter food",
                com.app.foodranker.data.model.PlateCategory.SALAD     to "salad vegetarian bowl",
                com.app.foodranker.data.model.PlateCategory.OTHER     to "gourmet food dish"
            )
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                pexelsQueries.forEach { (cat, q) ->
                    try {
                        val encoded = java.net.URLEncoder.encode(q, "UTF-8")
                        val conn = java.net.URL("https://api.pexels.com/v1/search?query=$encoded&per_page=15&orientation=portrait")
                            .openConnection() as java.net.HttpURLConnection
                        conn.setRequestProperty("Authorization", com.app.foodranker.BuildConfig.PEXELS_API_KEY)
                        conn.connectTimeout = 8_000; conn.readTimeout = 8_000
                        if (conn.responseCode == 200) {
                            val photos = org.json.JSONObject(conn.inputStream.bufferedReader().readText())
                                .optJSONArray("photos") ?: return@forEach
                            val urls = mutableListOf<String>()
                            for (i in 0 until photos.length()) {
                                val src = photos.getJSONObject(i).optJSONObject("src")
                                val url = src?.optString("large2x") ?: src?.optString("large") ?: continue
                                if (url.isNotBlank()) urls.add(url)
                            }
                            if (urls.isNotEmpty()) pexelsPool[cat] = urls.toMutableList()
                        }
                        conn.disconnect()
                    } catch (e: Exception) { android.util.Log.w("Seeder", "Pexels $cat: ${e.message}") }
                }
            }

            for ((catName, plateCat) in mealDbCategories) {
                try {
                    val listJson = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        java.net.URL("https://www.themealdb.com/api/json/v1/1/filter.php?c=$catName").readText()
                    }
                    val mealIds = org.json.JSONObject(listJson).optJSONArray("meals")?.let { arr ->
                        (0 until arr.length()).map { arr.getJSONObject(it).getString("idMeal") }
                    }?.shuffled()?.take(8) ?: continue

                    for (mealId in mealIds) {
                        try {
                            val detailJson = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                java.net.URL("https://www.themealdb.com/api/json/v1/1/lookup.php?i=$mealId").readText()
                            }
                            val meal = org.json.JSONObject(detailJson).optJSONArray("meals")?.getJSONObject(0) ?: continue
                            val area = meal.optString("strArea", "International")
                            val desc = meal.optString("strInstructions", "")
                                .replace("\r\n", " ").replace("\n", " ").trim()
                                .take(220).let { if (it.length == 220) "$it..." else it }
                            val imgUrl = pexelsPool[plateCat]?.removeFirstOrNull()
                                ?: meal.optString("strMealThumb", "")
                            val ratings = kotlin.random.Random.nextInt(20, 380)
                            val score   = (kotlin.random.Random.nextDouble(7.3, 9.85) * 10).toInt() / 10.0
                            val city = when(area) {
                                "Italian"->"Roma";"French"->"París";"Japanese"->"Tokio"
                                "Mexican"->"Ciudad de México";"Indian"->"Mumbai";"Chinese"->"Shanghái"
                                "British"->"Londres";"American"->"Nueva York";"Spanish"->"Madrid"
                                "Greek"->"Atenas";"Thai"->"Bangkok";"Moroccan"->"Marrakech"
                                "Turkish"->"Estambul";"Portuguese"->"Lisboa";"Croatian"->"Dubrovnik"
                                else-> listOf("Roma","Londres","París","Tokio","Nueva York").random()
                            }
                            val country = when(area) {
                                "Italian"->"Italia";"French"->"Francia";"Japanese"->"Japón"
                                "Mexican"->"México";"Indian"->"India";"Chinese"->"China"
                                "British"->"Reino Unido";"American"->"EE.UU.";"Spanish"->"España"
                                "Greek"->"Grecia";"Thai"->"Tailandia";"Moroccan"->"Marruecos"
                                "Turkish"->"Turquía";"Portuguese"->"Portugal";"Croatian"->"Croacia"
                                else-> listOf("Italia","Francia","Japón","España","EE.UU.").random()
                            }
                            val restaurant = when(area){
                                "Italian"->"Trattoria del Centro";"French"->"Brasserie Centrale"
                                "Japanese"->"Izakaya Yuki";"Mexican"->"Cantina El Pueblo"
                                "Indian"->"Spice Garden";"Chinese"->"Golden Dragon"
                                "British"->"The Crown Pub";"American"->"Downtown Diner"
                                "Spanish"->"Taberna El Rincón";"Greek"->"Taverna Kyria"
                                "Thai"->"Bangkok Kitchen";"Moroccan"->"Riad Zahra"
                                else->"Local Restaurant"
                            }
                            // Reglas Firestore requieren: averageScore=0, totalRatings=0, likes=0, reportCount=0 en create
                            val plate = com.app.foodranker.data.model.Plate(
                                id="mdb_$mealId", name=meal.optString("strMeal"),
                                description=desc, category=plateCat,
                                restaurantName=restaurant,
                                city=city, country=country, imageUrl=imgUrl,
                                addedByUserId=userId, addedByUserName="FoodRanker Team",
                                averageScore=0.0, totalRatings=0, likes=0, reportCount=0,
                                createdAt=System.currentTimeMillis() - kotlin.random.Random.nextLong(0, 90L*24*3600*1000)
                            )
                            val plateRef = firestore.collection("plates").document(plate.id)
                            plateRef.set(plate).await()
                            // Las reglas solo permiten update de averageScore+totalRatings juntos
                            plateRef.update(mapOf("averageScore" to score, "totalRatings" to ratings)).await()
                            inserted++
                            _uiState.value = _uiState.value.copy(seedingProgress = Pair(inserted, total))
                        } catch (e: Exception) { android.util.Log.e("Seeder", "meal $mealId: ${e.message}") }
                    }
                } catch (e: Exception) { android.util.Log.e("Seeder", "cat $catName: ${e.message}") }
            }
            android.util.Log.d("Seeder", "Completo: $inserted platos")
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
            _uiState.value = _uiState.value.copy(isLoading = true, hasMorePlates = true)
            try {
                val snapshot = firestore.collection("plates")
                    .orderBy("averageScore", Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE)
                    .get()
                    .await()
                val docs = snapshot.documents
                lastDocument = docs.lastOrNull()
                val plates = docs
                    .mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                    .filter { it.reportCount < 3 }
                    .shuffled()

                _uiState.value = _uiState.value.copy(
                    plates = plates,
                    isLoading = false,
                    hasMorePlates = docs.size >= PAGE_SIZE
                )

                // Cargar saves en segundo plano — si falla no bloquea el feed
                val userId = currentUserId
                if (userId.isNotEmpty()) {
                    try {
                        val savedIds = firestore.collection("saves")
                            .whereEqualTo("userId", userId).get().await()
                            .documents.mapNotNull { it.getString("plateId") }.toSet()
                        _uiState.value = _uiState.value.copy(savedPlateIds = savedIds)
                    } catch (e: Exception) {
                        android.util.Log.w("DiscoverVM", "No se pudieron cargar saves: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DiscoverVM", "Error cargando platos: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun submitRatingAnalytics(score: Float, plateId: String) {
        com.app.foodranker.utils.AnalyticsManager.logPlateRated(score, plateId)
    }

    fun toggleSave(plateId: String) {
        val userId = currentUserId.ifEmpty { return }
        val saveId = "${userId}_${plateId}"
        val isSaved = plateId in _uiState.value.savedPlateIds
        // Optimistic update
        _uiState.value = _uiState.value.copy(
            savedPlateIds = if (isSaved)
                _uiState.value.savedPlateIds - plateId
            else
                _uiState.value.savedPlateIds + plateId
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
                        _uiState.value.savedPlateIds + plateId
                )
            }
        }
    }

    fun toggleLike(plateId: String) {
        val userId = currentUserId.ifEmpty { return }
        val plate = _uiState.value.plates.find { it.id == plateId }
            ?: _uiState.value.followingPlates.find { it.id == plateId }
            ?: return
        val isCurrentlyLiked = userId in plate.likedByUsers

        val updated = plate.copy(
            likes = if (isCurrentlyLiked) plate.likes - 1 else plate.likes + 1,
            likedByUsers = if (isCurrentlyLiked) plate.likedByUsers - userId
                           else plate.likedByUsers + userId
        )
        applyPlateUpdate(plateId, updated)

        viewModelScope.launch {
            try {
                val ref = firestore.collection("plates").document(plateId)
                if (isCurrentlyLiked) {
                    ref.update("likedByUsers", FieldValue.arrayRemove(userId), "likes", FieldValue.increment(-1)).await()
                    com.app.foodranker.utils.AnalyticsManager.logPlateUnliked(plateId)
                } else {
                    ref.update("likedByUsers", FieldValue.arrayUnion(userId), "likes", FieldValue.increment(1)).await()
                    com.app.foodranker.utils.AnalyticsManager.logPlateLiked(plateId)
                    sendLikeNotification(plateId, plate, userId)
                }
            } catch (e: Exception) {
                applyPlateUpdate(plateId, plate)
            }
        }
    }

    fun submitRating(plateId: String, flavorScore: Float, presentationScore: Float, valueScore: Float, comment: String) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val alreadyRated = firestore.collection("ratings")
                    .document("${plateId}_${user.uid}")
                    .get().await().exists()
                if (alreadyRated) {
                    _uiState.value = _uiState.value.copy(
                        ratingFeedback = "Ya valoraste este plato"
                    )
                    return@launch
                }

                val safeFlavor = flavorScore.coerceIn(1f, 10f)
                val safePresentation = presentationScore.coerceIn(1f, 10f)
                val safeValue = valueScore.coerceIn(1f, 10f)
                val avgScore = (safeFlavor + safePresentation + safeValue) / 3.0
                val ratingId = "${plateId}_${user.uid}"
                val rating = Rating(
                    id = ratingId, plateId = plateId, userId = user.uid,
                    userName = user.displayName ?: "Usuario",
                    userPhotoUrl = user.photoUrl?.toString() ?: "",
                    flavorScore = safeFlavor, presentationScore = safePresentation,
                    valueScore = safeValue, averageScore = avgScore,
                    comment = comment.sanitized(InputLimits.RATING_COMMENT)
                )
                firestore.collection("ratings").document(ratingId).set(rating).await()

                val plate = _uiState.value.plates.find { it.id == plateId }
                    ?: _uiState.value.followingPlates.find { it.id == plateId }
                    ?: return@launch
                val allRatings = firestore.collection("ratings")
                    .whereEqualTo("plateId", plateId).get().await()
                    .documents.mapNotNull { it.toObject(Rating::class.java) }
                val newAvg = allRatings.map { it.averageScore }.average()
                firestore.collection("plates").document(plateId)
                    .update("averageScore", newAvg, "totalRatings", allRatings.size).await()

                val updated = plate.copy(averageScore = newAvg, totalRatings = allRatings.size)
                applyPlateUpdate(plateId, updated)

                val ownerUserId = plate.addedByUserId
                if (ownerUserId.isNotEmpty() && ownerUserId != user.uid) {
                    val notifId = UUID.randomUUID().toString()
                    firestore.collection("notifications").document(ownerUserId).collection("items")
                        .document(notifId).set(mapOf(
                            "id" to notifId, "type" to "rating",
                            "fromUserId" to user.uid,
                            "fromUserName" to (user.displayName ?: "Alguien"),
                            "plateId" to plateId, "plateName" to plate.name,
                            "score" to avgScore, "isRead" to false,
                            "createdAt" to System.currentTimeMillis()
                        )).await()
                    com.app.foodranker.utils.RewardManager.awardXP(ownerUserId, com.app.foodranker.utils.RewardManager.XP_RECEIVE_RATING, firestore)
                }
                com.app.foodranker.utils.RewardManager.awardXP(user.uid, com.app.foodranker.utils.RewardManager.XP_GIVE_RATING, firestore)
                com.app.foodranker.utils.RewardManager.checkAndAwardBadges(user.uid, firestore)
            } catch (e: Exception) {
                android.util.Log.e("DiscoverVM", "Error submitRating: ${e.message}")
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
            followingPlates = _uiState.value.followingPlates.map { if (it.id == plateId) updated else it }
        )
    }

    fun startListeningForNotifications(userId: String) {
        notifListener?.remove()
        isFirstNotifLoad = true
        notifListener = firestore.collection("notifications")
            .document(userId).collection("items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val unread = snapshot.documents.count { it.get("isRead") != true }
                _uiState.value = _uiState.value.copy(unreadNotificationCount = unread)
                if (!isFirstNotifLoad) {
                    snapshot.documentChanges.filter { it.type == DocumentChange.Type.ADDED }
                        .forEach { change ->
                            val data = change.document.data
                            val type = data["type"] as? String ?: return@forEach
                            val fromUser = data["fromUserName"] as? String ?: "Alguien"
                            val plateName = data["plateName"] as? String ?: "tu plato"
                            val plateId = data["plateId"] as? String
                            val (title, body) = when (type) {
                                "like" -> "❤️ Nuevo me gusta" to "$fromUser le ha dado like a \"$plateName\""
                                "rating" -> {
                                    val score = data["score"] as? Double ?: 0.0
                                    "⭐ Nueva valoración" to "$fromUser ha valorado \"$plateName\" con ${"%.1f".format(score)}"
                                }
                                else -> return@forEach
                            }
                            NotificationHelper.show(context, title, body, plateId)
                        }
                }
                isFirstNotifLoad = false
            }
    }

    fun markNotificationsRead(userId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("notifications")
                    .document(userId).collection("items")
                    .whereEqualTo("isRead", false)
                    .limit(100).get().await()
                if (snapshot.documents.isEmpty()) return@launch
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.update(it.reference, "isRead", true) }
                batch.commit().await()
            } catch (e: Exception) { /* silencioso */ }
        }
    }

    fun reportPlate(plateId: String, reason: String) {
        val userId = currentUserId.ifEmpty { return }
        if (plateId.isBlank()) return
        val cleanReason = reason.sanitized(InputLimits.REPORT_REASON)
        viewModelScope.launch {
            try {
                // Verificar si el usuario ya reportó este plato
                val existing = firestore.collection("reports")
                    .whereEqualTo("plateId", plateId)
                    .whereEqualTo("reportedByUserId", userId)
                    .limit(1).get().await()

                if (!existing.isEmpty) {
                    _uiState.value = _uiState.value.copy(reportFeedback = "Ya has reportado este plato anteriormente")
                    return@launch
                }

                val reportId = UUID.randomUUID().toString()
                firestore.collection("reports").document(reportId).set(mapOf(
                    "id" to reportId,
                    "plateId" to plateId,
                    "reportedByUserId" to userId,
                    "reason" to cleanReason,
                    "createdAt" to System.currentTimeMillis()
                )).await()

                // Incrementar contador de reportes en el plato
                firestore.collection("plates").document(plateId)
                    .update("reportCount", FieldValue.increment(1)).await()

                _uiState.value = _uiState.value.copy(reportFeedback = "Reporte enviado. Gracias por ayudarnos a mantener la comunidad.")
            } catch (e: Exception) {
                android.util.Log.e("DiscoverVM", "Error al reportar: ${e.message}")
            }
        }
    }

    fun clearReportFeedback() {
        _uiState.value = _uiState.value.copy(reportFeedback = null)
    }

    fun clearRatingFeedback() {
        _uiState.value = _uiState.value.copy(ratingFeedback = null)
    }

    override fun onCleared() {
        super.onCleared()
        notifListener?.remove()
    }
}
