package com.app.foodranker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.foodranker.data.model.User
import com.app.foodranker.data.repository.BloqueoRepository
import com.app.foodranker.utils.Fallos
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class BloqueadosUiState(
    val usuarios: List<User> = emptyList(),
    val cargando: Boolean = true
)

/**
 * La lista de a quién has bloqueado, para poder deshacerlo.
 *
 * Hace falta por dos motivos y los dos pesan. El de fuera: la política de contenido de
 * Google Play pide que una app con contenido público deje bloquear usuarios, y un bloqueo
 * que no se puede revisar ni deshacer no cumple. El de dentro: un botón que hace algo
 * irreversible y no enseña dónde quedó lo hecho es un botón que da miedo pulsar.
 *
 * Solo lista `bloqueadosPorMi`, no `ocultos`. Quien te haya bloqueado A TI no sale: no es
 * tuyo, no puedes deshacerlo, y enseñar esa lista sería convertir el espejo privado en un
 * tablón de "estas personas te han bloqueado".
 */
@HiltViewModel
class BloqueadosViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val bloqueos: BloqueoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BloqueadosUiState())
    val uiState: StateFlow<BloqueadosUiState> = _uiState

    init {
        // Se recarga sola al bloquear o desbloquear desde cualquier otra pantalla.
        viewModelScope.launch {
            bloqueos.bloqueadosPorMi.collect { ids -> cargar(ids) }
        }
    }

    private suspend fun cargar(ids: Set<String>) {
        if (ids.isEmpty()) {
            _uiState.value = BloqueadosUiState(usuarios = emptyList(), cargando = false)
            return
        }
        try {
            val encontrados = mutableListOf<User>()
            // whereIn admite 30 por consulta.
            ids.chunked(30).forEach { trozo ->
                firestore.collection("users")
                    .whereIn(FieldPath.documentId(), trozo)
                    .get().await().documents.forEach { doc ->
                        // bloqueo-ok: ESTA es la lista de bloqueados. Filtrarla la dejaría
                        // vacía siempre y no habría forma de deshacer nada.
                        encontrados += doc.toObject(User::class.java)?.copy(id = doc.id)
                            ?: User(id = doc.id)
                    }
            }
            // Un uid cuya cuenta ya no existe sigue en la lista: se enseña igual, con el
            // nombre de respaldo, para que se pueda quitar. Si no, quedaría una entrada
            // fantasma imposible de borrar desde la app.
            val perdidos = ids - encontrados.map { it.id }.toSet()
            _uiState.value = BloqueadosUiState(
                usuarios = (encontrados + perdidos.map { User(id = it) })
                    .sortedBy { it.name.lowercase() },
                cargando = false
            )
        } catch (e: Exception) {
            Fallos.reportar(e, "bloqueados: cargar lista")
            _uiState.value = _uiState.value.copy(cargando = false)
        }
    }

    fun desbloquear(uid: String, alTerminar: (Boolean) -> Unit = {}) {
        viewModelScope.launch { alTerminar(bloqueos.desbloquear(uid).isSuccess) }
    }
}
