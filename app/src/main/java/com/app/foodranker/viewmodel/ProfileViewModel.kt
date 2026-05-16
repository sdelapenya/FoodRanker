package com.app.foodranker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.User
import com.app.foodranker.utils.InputLimits
import com.app.foodranker.utils.InputLimits.sanitized
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    val isLoadingCollectionPlates: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    fun loadProfile(userId: String) {
        if (userId.isBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Usuario no válido")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val currentUserId = auth.currentUser?.uid ?: ""
                val isOwnProfile = currentUserId == userId

                val user = if (isOwnProfile && auth.currentUser != null) {
                    val firebaseUser = auth.currentUser!!
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

                val rawPlates = firestore.collection("plates")
                    .whereEqualTo("addedByUserId", userId).get().await()
                    .documents.mapNotNull { it.toObject(Plate::class.java) }
                val plates = rawPlates.sortedByDescending { it.createdAt }

                // Actividad como consumidor
                val ratingsGiven = firestore.collection("ratings")
                    .whereEqualTo("userId", userId).get().await().size()

                // Likes dados (platos donde el usuario aparece en likedByUsers)
                val likesGivenSnap = firestore.collection("plates")
                    .whereArrayContains("likedByUsers", userId).get().await()
                val likesGiven = likesGivenSnap.size()

                // Cargar datos de seguimiento
                val followerSnap = firestore.collection("follows")
                    .whereEqualTo("followingId", userId).get().await()
                val followingSnap = firestore.collection("follows")
                    .whereEqualTo("followerId", userId).get().await()
                val isFollowing = if (!isOwnProfile) {
                    followerSnap.documents.any { it.getString("followerId") == currentUserId }
                } else false

                _uiState.value = _uiState.value.copy(
                    user = user, plates = plates,
                    isLoading = false, isOwnProfile = isOwnProfile,
                    isFollowing = isFollowing,
                    followerCount = followerSnap.size(),
                    followingCount = followingSnap.size(),
                    ratingsGiven = ratingsGiven,
                    likesGiven = likesGiven
                )
                // Siempre refrescar saved y collections en perfil propio
                if (isOwnProfile) {
                    loadSavedPlates()
                    loadCollections()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
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
                    .whereEqualTo("userId", userId).get().await()
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
                    .whereEqualTo("userId", userId).get().await()
                val plateIds = savesSnap.documents.mapNotNull { it.getString("plateId") }
                if (plateIds.isEmpty()) {
                    _uiState.value = _uiState.value.copy(savedPlates = emptyList())
                    return@launch
                }
                // Firestore whereIn admite hasta 30 ids
                val saved = mutableListOf<Plate>()
                plateIds.chunked(30).forEach { chunk ->
                    firestore.collection("plates")
                        .whereIn("id", chunk).get().await()
                        .documents.mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                        .let { saved.addAll(it) }
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

    fun updateProfile(bio: String, city: String, website: String, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val cleanBio = bio.sanitized(InputLimits.BIO)
        val cleanCity = city.sanitized(InputLimits.CITY)
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
            }
        }
    }

    fun toggleFollow(targetUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
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
                        .whereIn("id", chunk).get().await()
                        .documents.mapNotNull { it.toObject(Plate::class.java) }
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
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingAccount = true)
            try {
                val uid = user.uid

                suspend fun deleteQueryInBatches(
                    queryBuilder: com.google.firebase.firestore.Query
                ) {
                    while (true) {
                        val snap = queryBuilder.limit(450).get().await()
                        if (snap.isEmpty) break
                        val batch = firestore.batch()
                        snap.documents.forEach { batch.delete(it.reference) }
                        batch.commit().await()
                    }
                }

                // 1. Borrar platos del usuario
                deleteQueryInBatches(
                    firestore.collection("plates").whereEqualTo("addedByUserId", uid)
                )

                // 2. Borrar valoraciones del usuario
                deleteQueryInBatches(
                    firestore.collection("ratings").whereEqualTo("userId", uid)
                )

                // 3. Comentarios escritos por el usuario
                deleteQueryInBatches(
                    firestore.collection("comments").whereEqualTo("userId", uid)
                )

                // 4. Seguimientos
                deleteQueryInBatches(
                    firestore.collection("follows").whereEqualTo("followerId", uid)
                )
                deleteQueryInBatches(
                    firestore.collection("follows").whereEqualTo("followingId", uid)
                )

                // 5. Platos guardados (saves)
                deleteQueryInBatches(
                    firestore.collection("saves").whereEqualTo("userId", uid)
                )

                // 6. Listas/colecciones del usuario
                deleteQueryInBatches(
                    firestore.collection("collections").whereEqualTo("userId", uid)
                )

                // 7. Items de notificación (subcolección)
                deleteQueryInBatches(
                    firestore.collection("notifications").document(uid).collection("items")
                )

                // 8. Documento de usuario, raíz de notificaciones
                firestore.collection("users").document(uid).delete().await()
                firestore.collection("notifications").document(uid).delete().await()

                // 9. Borrar cuenta de Firebase Auth
                user.delete().await()

                _uiState.value = _uiState.value.copy(isDeletingAccount = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isDeletingAccount = false)
                onError(e.message ?: "Error al eliminar la cuenta")
            }
        }
    }
}
