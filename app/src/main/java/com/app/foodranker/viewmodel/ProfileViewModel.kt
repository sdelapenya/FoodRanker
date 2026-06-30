package com.app.foodranker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.User
import com.app.foodranker.utils.InputLimits
import com.app.foodranker.utils.InputLimits.sanitized
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.app.foodranker.utils.ErrorMapper
import javax.inject.Inject

enum class ProfileTab { MY_PLATES, SAVED, COLLECTIONS }

data class ProfileUiState(
    val user: User? = null,
    val plates: List<Plate> = emptyList(),
    val savedPlates: List<Plate> = emptyList(),
    val collections: List<com.app.foodranker.data.model.PlateCollection> = emptyList(),
    val activeTab: ProfileTab = ProfileTab.MY_PLATES,
    val isLoading: Boolean = false,
    val isOwnProfile: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val isSavingProfile: Boolean = false,
    val isFollowing: Boolean = false,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val ratingsGiven: Int = 0,
    val likesGiven: Int = 0,
    val error: String? = null,
    val collectionPlates: List<Plate> = emptyList(),
    val isLoadingCollectionPlates: Boolean = false,
    val nearbyRival: User? = null,
    val nearbyRivalGap: Int = 0,
    val cityRank: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState
    @Volatile private var lastLoadTime = 0L

    fun loadProfileIfStale(userId: String) {
        if (System.currentTimeMillis() - lastLoadTime < 60_000L) return
        loadProfile(userId)
    }

    fun loadProfile(userId: String) {
        if (userId.isBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Usuario no válido")
            return
        }
        lastLoadTime = System.currentTimeMillis()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val firebaseUser = auth.currentUser
                val currentUserId = firebaseUser?.uid ?: ""
                val isOwnProfile = currentUserId == userId

                coroutineScope {
                    val userDeferred = async {
                        if (isOwnProfile && firebaseUser != null) {
                            val userDoc = firestore.collection("users").document(userId).get().await()
                            if (userDoc.exists()) {
                                userDoc.toObject(User::class.java) ?: User(
                                    id = firebaseUser.uid,
                                    name = firebaseUser.displayName ?: "Usuario",
                                    email = firebaseUser.email ?: "",
                                    photoUrl = firebaseUser.photoUrl?.toString() ?: ""
                                )
                            } else {
                                val newUser = User(
                                    id = firebaseUser.uid,
                                    name = firebaseUser.displayName ?: "Usuario",
                                    email = firebaseUser.email ?: "",
                                    photoUrl = firebaseUser.photoUrl?.toString() ?: ""
                                )
                                firestore.collection("users").document(firebaseUser.uid).set(newUser).await()
                                newUser
                            }
                        } else {
                            firestore.collection("users").document(userId).get().await().toObject(User::class.java)
                        }
                    }
                    val platesDeferred = async {
                        firestore.collection("plates")
                            .whereEqualTo("addedByUserId", userId).limit(100).get().await()
                            .documents.mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                            .sortedByDescending { it.createdAt }
                    }
                    val ratingsDeferred = async {
                        firestore.collection("ratings")
                            .whereEqualTo("userId", userId).limit(1000).get().await().size()
                    }
                    val likesDeferred = async {
                        firestore.collection("plates")
                            .whereArrayContains("likedByUsers", userId).limit(500).get().await().size()
                    }
                    val followerDeferred = async {
                        firestore.collection("follows").whereEqualTo("followingId", userId).limit(1000).get().await()
                    }
                    val followingDeferred = async {
                        firestore.collection("follows").whereEqualTo("followerId", userId).limit(1000).get().await()
                    }
                    // Lookup O(1) por document ID determinista en vez de scan lineal
                    val isFollowingDeferred = async {
                        if (!isOwnProfile && currentUserId.isNotEmpty()) {
                            firestore.collection("follows")
                                .document("${currentUserId}_${userId}").get().await().exists()
                        } else false
                    }

                    val user = userDeferred.await()

                    // Rival query: closest user above in XP within same city (own profile only)
                    val rivalDeferred = if (isOwnProfile && user != null && user.city.isNotEmpty() && user.xp > 0) {
                        async {
                            try {
                                firestore.collection("users")
                                    .whereEqualTo("city", user.city)
                                    .whereGreaterThan("xp", user.xp)
                                    .orderBy("xp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                                    .limit(1).get().await()
                                    .documents.firstOrNull()?.toObject(User::class.java)
                            } catch (e: Exception) { null }
                        }
                    } else null

                    // City rank: count users with higher XP in same city + 1
                    val cityRankDeferred = if (isOwnProfile && user != null && user.city.isNotEmpty()) {
                        async {
                            try {
                                firestore.collection("users")
                                    .whereEqualTo("city", user.city)
                                    .orderBy("xp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                    .whereGreaterThan("xp", user.xp)
                                    .limit(200).get().await().size() + 1
                            } catch (e: Exception) { 0 }
                        }
                    } else null

                    val plates = platesDeferred.await()
                    val ratingsGiven = ratingsDeferred.await()
                    val likesGiven = likesDeferred.await()
                    val followerSnap = followerDeferred.await()
                    val followingSnap = followingDeferred.await()
                    val isFollowing = isFollowingDeferred.await()
                    val rival = rivalDeferred?.await()
                    val rivalGap = if (rival != null && user != null) rival.xp - user.xp else 0
                    val cityRank = cityRankDeferred?.await() ?: 0

                    _uiState.value = _uiState.value.copy(
                        user = user, plates = plates,
                        isLoading = false, isOwnProfile = isOwnProfile,
                        isFollowing = isFollowing,
                        followerCount = followerSnap.size(),
                        followingCount = followingSnap.size(),
                        ratingsGiven = ratingsGiven,
                        likesGiven = likesGiven,
                        nearbyRival = rival,
                        nearbyRivalGap = rivalGap,
                        cityRank = cityRank
                    )
                    if (isOwnProfile) {
                        loadSavedPlates()
                        loadCollections()
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = ErrorMapper.toUserMessage(e))
            }
        }
    }

    fun setTab(tab: ProfileTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
        when (tab) {
            ProfileTab.SAVED       -> loadSavedPlates()   // siempre recargar
            ProfileTab.COLLECTIONS -> loadCollections()   // siempre recargar
            else -> {}
        }
    }

    fun loadCollections() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snap = firestore.collection("collections")
                    .whereEqualTo("userId", userId).limit(50).get().await()
                val cols = snap.documents.mapNotNull {
                    it.toObject(com.app.foodranker.data.model.PlateCollection::class.java)
                }.sortedByDescending { it.createdAt }
                _uiState.value = _uiState.value.copy(collections = cols)
            } catch (e: Exception) {
                android.util.Log.e("Profile", "Error cargando colecciones: ${e.message}")
            }
        }
    }

    fun createCollection(name: String, emoji: String) {
        val userId = auth.currentUser?.uid ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val id = java.util.UUID.randomUUID().toString()
                val col = com.app.foodranker.data.model.PlateCollection(
                    id = id, userId = userId, name = name.trim(), emoji = emoji
                )
                firestore.collection("collections").document(id).set(col).await()
                _uiState.value = _uiState.value.copy(
                    collections = listOf(col) + _uiState.value.collections
                )
            } catch (e: Exception) { android.util.Log.e("Profile", "Error creando colección: ${e.message}") }
        }
    }

    fun addPlateToCollection(collectionId: String, plateId: String) {
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
                android.util.Log.e("Profile", "addPlateToCollection: ${e.message}")
            }
        }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("collections").document(collectionId).delete().await()
                _uiState.value = _uiState.value.copy(
                    collections = _uiState.value.collections.filter { it.id != collectionId }
                )
            } catch (e: Exception) {
                android.util.Log.e("Profile", "deleteCollection: ${e.message}")
            }
        }
    }

    fun loadSavedPlates() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val savesSnap = firestore.collection("saves")
                    .whereEqualTo("userId", userId).limit(500).get().await()
                val plateIds = savesSnap.documents.mapNotNull { it.getString("plateId") }
                if (plateIds.isEmpty()) {
                    _uiState.value = _uiState.value.copy(savedPlates = emptyList())
                    return@launch
                }
                // Firestore whereIn admite hasta 30 ids — cargar chunks en paralelo
                val saved = coroutineScope {
                    plateIds.chunked(30).map { chunk ->
                        async {
                            firestore.collection("plates")
                                .whereIn(FieldPath.documentId(), chunk).get().await()
                                .documents.mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                        }
                    }.flatMap { it.await() }
                }
                _uiState.value = _uiState.value.copy(
                    savedPlates = saved.sortedByDescending { it.createdAt }
                )
            } catch (e: Exception) {
                android.util.Log.e("Profile", "Error cargando guardados: ${e.message}")
            }
        }
    }

    fun updatePlateDescription(plateId: String, description: String) {
        val uid = auth.currentUser?.uid ?: return
        if (plateId.isBlank()) return
        val clean = description.sanitized(InputLimits.PLATE_DESCRIPTION)
        viewModelScope.launch {
            try {
                val plateRef = firestore.collection("plates").document(plateId)
                val snap = plateRef.get().await()
                val ownerId = snap.getString("addedByUserId") ?: ""
                if (ownerId != uid) return@launch

                plateRef.update("description", clean).await()
                _uiState.value = _uiState.value.copy(
                    plates = _uiState.value.plates.map {
                        if (it.id == plateId) it.copy(description = clean) else it
                    }
                )
            } catch (e: Exception) { /* silencioso */ }
        }
    }

    fun updateProfile(
        bio: String,
        city: String,
        website: String,
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        val cleanBio = bio.sanitized(InputLimits.BIO)
        // Capitalizamos para que la ciudad se vea consistente en el cliente
        // ("madrid" y "Madrid" se guardan igual) — el servidor sigue normalizando
        // por separado (lowercase) al calcular el leagueId, así que esto no afecta
        // a la lógica de liga, solo a cómo se muestra el texto.
        val cleanCity = city.sanitized(InputLimits.CITY)
            .lowercase()
            .split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        val cleanWebsite = website.sanitized(InputLimits.WEBSITE)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingProfile = true)
            try {
                firestore.collection("users").document(userId).update(
                    mapOf(
                        "bio"     to cleanBio,
                        "city"    to cleanCity,
                        "website" to cleanWebsite
                    )
                ).await()
                _uiState.value = _uiState.value.copy(
                    isSavingProfile = false,
                    user = _uiState.value.user?.copy(
                        bio = cleanBio,
                        city = cleanCity,
                        website = cleanWebsite
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSavingProfile = false)
                onError(ErrorMapper.toUserMessage(e))
            }
        }
    }

    @Volatile private var isTogglingFollow = false

    fun toggleFollow(targetUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        // Evita que un doble-tap rápido dispare dos coroutines que lean isFollowing
        // antes de que la primera haya actualizado el estado, duplicando el +1/-1 local.
        if (isTogglingFollow) return
        isTogglingFollow = true
        viewModelScope.launch {
            try {
                val followId = "${currentUserId}_${targetUserId}"
                val followRef = firestore.collection("follows").document(followId)
                if (_uiState.value.isFollowing) {
                    followRef.delete().await()
                    _uiState.value = _uiState.value.copy(isFollowing = false, followerCount = maxOf(0, _uiState.value.followerCount - 1))
                    com.app.foodranker.utils.AnalyticsManager.logUserUnfollowed()
                } else {
                    followRef.set(mapOf(
                        "followerId" to currentUserId,
                        "followingId" to targetUserId,
                        "createdAt" to System.currentTimeMillis()
                    )).await()
                    _uiState.value = _uiState.value.copy(isFollowing = true, followerCount = _uiState.value.followerCount + 1)
                    com.app.foodranker.utils.AnalyticsManager.logUserFollowed()
                }
            } catch (e: Exception) {
                android.util.Log.e("Profile", "Error toggleFollow: ${e.message}")
            } finally {
                isTogglingFollow = false
            }
        }
    }

    fun loadCollectionPlates(plateIds: List<String>) {
        if (plateIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(collectionPlates = emptyList(), isLoadingCollectionPlates = false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCollectionPlates = true)
            try {
                val plates = mutableListOf<Plate>()
                plateIds.chunked(30).forEach { chunk ->
                    firestore.collection("plates")
                        .whereIn(FieldPath.documentId(), chunk).get().await()
                        .documents.mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                        .let { plates.addAll(it) }
                }
                _uiState.value = _uiState.value.copy(
                    collectionPlates = plates.sortedByDescending { it.createdAt },
                    isLoadingCollectionPlates = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingCollectionPlates = false)
            }
        }
    }

    fun clearCollectionPlates() {
        _uiState.value = _uiState.value.copy(collectionPlates = emptyList())
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (auth.currentUser == null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingAccount = true)
            try {
                // Cloud Function handles all data deletion (including ratings which
                // have allow delete: if false) and deletes the Auth account via Admin SDK.
                functions.getHttpsCallable("deleteUserAccount")
                    .call()
                    .await()
                _uiState.value = _uiState.value.copy(isDeletingAccount = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isDeletingAccount = false)
                onError(e.message ?: "Error al eliminar la cuenta")
            }
        }
    }
}
