package com.app.foodranker.viewmodel

import com.app.foodranker.data.model.soloPublicos
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
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
import com.app.foodranker.utils.Fallos
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.app.foodranker.R
import com.app.foodranker.data.model.sinBloqueados
import com.app.foodranker.data.repository.BloqueoRepository

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
    /** Ligas semanales ganadas. Va suelto, no dentro de User — ver el comentario en load(). */
    val leagueWins: Int = 0,
    val ratingsGiven: Int = 0,
    val likesGiven: Int = 0,
    val error: String? = null,
    val collectionPlates: List<Plate> = emptyList(),
    val isLoadingCollectionPlates: Boolean = false,
    val nearbyRival: User? = null,
    val nearbyRivalGap: Int = 0,
    val cityRank: Int = 0,
    /** Hay bloqueo con este perfil, en cualquier dirección: no se enseña nada de él. */
    val hayBloqueo: Boolean = false,
    /** Y además lo bloqueé yo, así que puedo deshacerlo desde aquí. */
    val loBloqueeYo: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val bloqueos: BloqueoRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    @ApplicationContext private val appContext: Context
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
            _uiState.value = _uiState.value.copy(isLoading = false, error = appContext.getString(R.string.vm_user_invalid))
            return
        }
        // Con bloqueo de por medio el perfil no se carga siquiera. Podría filtrarse
        // campo a campo, pero entonces cada cosa que se añadiera al perfil más adelante
        // sería una fuga nueva: es más seguro no traerlo.
        if (userId != auth.currentUser?.uid && userId in bloqueos.ocultos.value) {
            _uiState.value = ProfileUiState(
                isLoading = false,
                hayBloqueo = true,
                loBloqueeYo = userId in bloqueos.bloqueadosPorMi.value
            )
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
                    // Devuelve también las ligas ganadas, que se leen sueltas del documento y
                    // no desde User: la regla de creación de usuarios usa hasOnly() con los
                    // campos del modelo, así que meter uno nuevo ahí sin tocar firestore.rules
                    // haría fallar el alta de cualquier usuario nuevo.
                    val userDeferred = async {
                        if (isOwnProfile && firebaseUser != null) {
                            val userDoc = firestore.collection("users").document(userId).get().await()
                            val wins = (userDoc.getLong("leagueWins") ?: 0L).toInt()
                            if (userDoc.exists()) {
                                // Si toObject() falla (doc con forma inesperada), el nombre
                                // en bruto del propio doc sigue siendo mejor dato que
                                // displayName, que puede venir vacío del login por navegador.
                                // bloqueo-ok: es MI perfil.
                                (userDoc.toObject(User::class.java) ?: User(
                                    id = firebaseUser.uid,
                                    name = userDoc.getString("name") ?: firebaseUser.displayName ?: "Usuario",
                                    photoUrl = userDoc.getString("photoUrl") ?: firebaseUser.photoUrl?.toString() ?: ""
                                )) to wins
                            } else {
                                val newUser = User(
                                    id = firebaseUser.uid,
                                    name = firebaseUser.displayName ?: "Usuario",
                                    photoUrl = firebaseUser.photoUrl?.toString() ?: ""
                                )
                                firestore.collection("users").document(firebaseUser.uid).set(newUser).await()
                                newUser to 0
                            }
                        } else {
                            val doc = firestore.collection("users").document(userId).get().await()
                            // bloqueo-ok: con bloqueo no se llega hasta aquí, loadProfile corta antes.
                            doc.toObject(User::class.java) to (doc.getLong("leagueWins") ?: 0L).toInt()
                        }
                    }
                    val platesDeferred = async {
                        val suyos = firestore.collection("plates")
                            .whereEqualTo("addedByUserId", userId).limit(100).get().await()
                            // bloqueo-ok: son los platos del perfil que se está viendo, y
                            // loadProfile ya corta antes si hay bloqueo con su dueño.
                            .documents.mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                        // En TU perfil se ven todos, incluido el que acabas de subir y
                        // sigue en moderación: filtrarlo aquí haría parecer que la
                        // publicación se ha perdido. En el de otro, solo los públicos
                        // — si no, el perfil del autor sería la puerta de atrás por la
                        // que ver justo lo que los listados ya no enseñan.
                        (if (isOwnProfile) suyos else suyos.soloPublicos())
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

                    val (user, leagueWins) = userDeferred.await()

                    // Rival query: closest user above in XP within same city (own profile only)
                    val rivalDeferred = if (isOwnProfile && user != null && user.city.isNotEmpty() && user.xp > 0) {
                        async {
                            try {
                                firestore.collection("users")
                                    .whereEqualTo("city", user.city)
                                    .whereGreaterThan("xp", user.xp)
                                    .orderBy("xp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                                    // Se piden varios y no uno: si el primero es alguien
                                    // con bloqueo de por medio, el rival pasa a ser el
                                    // siguiente, no desaparece la tarjeta.
                                    .limit(5).get().await()
                                    .documents.mapNotNull { it.toObject(User::class.java)?.copy(id = it.id) }
                                    .sinBloqueados(bloqueos.ocultos.value)
                                    .firstOrNull()
                            } catch (e: Exception) {
                                // Devolver null deja la tarjeta de rival sin pintar, que es
                                // un apaño razonable. Pero esta consulta lleva whereEqualTo
                                // + whereGreaterThan + orderBy, o sea que es de las que
                                // fallan enteras si falta el índice compuesto, y así no se
                                // notaría nunca (ya pasó con un índice roto desde siempre).
                                Fallos.reportar(e, "perfil: consulta de rival")
                                null
                            }
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
                            } catch (e: Exception) {
                                // Igual que la de rival: índice compuesto, y un 0 silencioso
                                // se confunde con "eres el primero de tu ciudad".
                                Fallos.reportar(e, "perfil: rango en la ciudad")
                                0
                            }
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
                        user = user, plates = plates, leagueWins = leagueWins,
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
                Fallos.reportar(e, "perfil: carga principal")
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
                // bloqueo-ok: son MIS colecciones (whereEqualTo userId == el mío).
                val cols = snap.documents.mapNotNull {
                    it.toObject(com.app.foodranker.data.model.PlateCollection::class.java)
                }.sortedByDescending { it.createdAt }
                _uiState.value = _uiState.value.copy(collections = cols)
            } catch (e: Exception) {
                Fallos.reportar(e, "perfil: cargar colecciones")
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
            } catch (e: Exception) {
                Fallos.reportar(e, "perfil: crear colección")
                android.util.Log.e("Profile", "Error creando colección: ${e.message}")
            }
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
                Fallos.reportar(e, "perfil: añadir plato a colección")
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
                Fallos.reportar(e, "perfil: borrar colección")
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
                }.sinBloqueados(bloqueos.ocultos.value)
                _uiState.value = _uiState.value.copy(
                    savedPlates = saved.sortedByDescending { it.createdAt }
                )
            } catch (e: Exception) {
                Fallos.reportar(e, "perfil: cargar guardados")
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
            } catch (e: Exception) {
                // Al usuario se le sigue fallando en silencio (no hay sitio en la UI donde
                // contárselo), pero al menos ahora nos enteramos nosotros.
                Fallos.reportar(e, "perfil: guardar descripción de plato")
            }
        }
    }

    /** Cómo acabó un intento de corregir el nombre de un plato. */
    enum class ResultadoRenombrar { OK, SOLO_COSMETICO, ERROR }

    /**
     * Corrige CÓMO SE ESCRIBE el nombre de un plato: tildes, mayúsculas, puntuación.
     *
     * No es un renombrado: el id del documento es `{venueId}__{dishSlug}` y en Firestore
     * los ids no se pueden cambiar, así que un nombre que normalice a otro slug exigiría
     * crear otro documento y mover ratings, comments y saves. Eso se devuelve como
     * [ResultadoRenombrar.SOLO_COSMETICO] y no se hace nada.
     *
     * Lo decide la Cloud Function, no esto: las reglas de Firestore no tienen
     * normalización Unicode y no pueden saber que "Ñoquis" y "noquis" colapsan. Aquí no
     * se comprueba el slug ni por adelantado, para que no haya dos criterios — manda el
     * servidor, igual que con `getLeagueId`.
     */
    fun renamePlate(plateId: String, name: String, onResult: (ResultadoRenombrar) -> Unit = {}) {
        if (auth.currentUser?.uid == null || plateId.isBlank()) {
            onResult(ResultadoRenombrar.ERROR); return
        }
        val limpio = name.sanitized(InputLimits.PLATE_NAME)
        if (limpio.isBlank()) { onResult(ResultadoRenombrar.ERROR); return }

        viewModelScope.launch {
            try {
                functions.getHttpsCallable("renamePlate")
                    .call(mapOf("plateId" to plateId, "name" to limpio))
                    .await()
                // La lista se actualiza aquí en vez de recargarla entera: el único campo
                // que ha cambiado es este, y recargar haría parpadear el perfil.
                _uiState.value = _uiState.value.copy(
                    plates = _uiState.value.plates.map {
                        if (it.id == plateId) it.copy(name = limpio) else it
                    }
                )
                onResult(ResultadoRenombrar.OK)
            } catch (e: Exception) {
                // La función devuelve FAILED_PRECONDITION con el mensaje "slug-changed"
                // cuando el nombre nuevo normalizaría a otro slug. Es el caso normal y
                // esperable —el usuario intentó corregir una errata, no una tilde—, no un
                // fallo, así que se distingue para poder explicárselo.
                val esSlug = (e as? FirebaseFunctionsException)
                    ?.let { it.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION &&
                            it.message?.contains("slug-changed") == true } ?: false
                // Solo se reporta lo que NO es el caso esperable: "slug-changed" es la
                // respuesta normal de la función, no un fallo, y reportarlo convertiría
                // cada errata corregida en un no fatal.
                if (!esSlug) Fallos.reportar(e, "perfil: renombrar plato")
                onResult(if (esSlug) ResultadoRenombrar.SOLO_COSMETICO else ResultadoRenombrar.ERROR)
            }
        }
    }

    /**
     * Borra un plato del usuario. La limpieza de todo lo que cuelga de él (imagen en
     * Cloudinary, valoraciones, XP concedido, entradas de liga) la hace la Cloud
     * Function `onPlateDeleted`; aquí solo se borra el documento.
     *
     * Se comprueba la propiedad antes de borrar aunque las reglas de Firestore ya la
     * exigen: así el fallo es un no-op silencioso en vez de una excepción de permisos.
     */
    fun deletePlate(plateId: String, onDeleted: () -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return
        if (plateId.isBlank()) return
        viewModelScope.launch {
            try {
                val plateRef = firestore.collection("plates").document(plateId)
                val snap = plateRef.get().await()
                val ownerId = snap.getString("addedByUserId") ?: ""
                if (ownerId != uid) {
                    // No debería pasar nunca desde la UI (el botón solo aparece en tus
                    // propios platos), pero si "plates" está desincronizado de Firestore
                    // por lo que sea, salir en silencio dejaría creer que sí se borró.
                    _uiState.value = _uiState.value.copy(error = appContext.getString(R.string.vm_no_permission_delete))
                    return@launch
                }

                plateRef.delete().await()
                _uiState.value = _uiState.value.copy(
                    plates = _uiState.value.plates.filterNot { it.id == plateId }
                )
                onDeleted()
            } catch (e: Exception) {
                // El diálogo de confirmación avisa de que borrar es irreversible, así que
                // un fallo silencioso aquí dejaría al usuario creyendo que sí se borró.
                Fallos.reportar(e, "perfil: borrar plato")
                android.util.Log.e("Profile", "Error borrando plato: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = com.app.foodranker.utils.ErrorMapper.toUserMessage(e)
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
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
                Fallos.reportar(e, "perfil: guardar bio/ciudad/web")
                _uiState.value = _uiState.value.copy(isSavingProfile = false)
                onError(ErrorMapper.toUserMessage(e))
            }
        }
    }

    @Volatile private var isTogglingFollow = false

    /**
     * Bloquear a alguien desde su perfil.
     *
     * Al volver se recarga el perfil, que con el bloqueo ya puesto se corta solo y deja
     * la pantalla en "perfil no disponible". No se navega hacia atrás a propósito: el
     * usuario acaba de pulsar algo y tiene que ver que ha surtido efecto.
     *
     * ⚠️ `lastLoadTime = 0` antes de recargar. Sin eso, `loadProfileIfStale` se traga la
     * recarga durante un minuto y la pantalla se queda enseñando a quien acabas de
     * bloquear — que es justo el momento en el que peor sienta.
     */
    fun bloquear(targetUserId: String, alTerminar: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = bloqueos.bloquear(targetUserId).isSuccess
            if (ok) {
                com.app.foodranker.utils.AnalyticsManager.logUserBlocked()
                lastLoadTime = 0L
                loadProfile(targetUserId)
            }
            alTerminar(ok)
        }
    }

    /** Deshacerlo. Los seguimientos NO vuelven: volver a seguir es una decisión. */
    fun desbloquear(targetUserId: String, alTerminar: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = bloqueos.desbloquear(targetUserId).isSuccess
            if (ok) {
                lastLoadTime = 0L
                loadProfile(targetUserId)
            }
            alTerminar(ok)
        }
    }

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
                // Importa más que la media: el contador local ya se puede haber movido,
                // así que un fallo aquí deja al usuario viendo un "siguiendo" que el
                // servidor no tiene.
                Fallos.reportar(e, "perfil: seguir/dejar de seguir")
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
                        .sinBloqueados(bloqueos.ocultos.value)
                        .let { plates.addAll(it) }
                }
                _uiState.value = _uiState.value.copy(
                    collectionPlates = plates.sortedByDescending { it.createdAt },
                    isLoadingCollectionPlates = false
                )
            } catch (e: Exception) {
                // Sin esto la colección se queda vacía sin explicación: el spinner se apaga
                // y parece que no tiene platos.
                Fallos.reportar(e, "perfil: cargar platos de colección")
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
                // Borrar la cuenta es irreversible y obligatorio por política de Play: si
                // falla, el usuario se queda sin poder irse y hay que saberlo ya.
                Fallos.reportar(e, "perfil: borrar cuenta")
                _uiState.value = _uiState.value.copy(isDeletingAccount = false)
                onError(e.message ?: appContext.getString(R.string.vm_delete_account_fail))
            }
        }
    }
}
