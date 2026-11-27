package com.app.foodranker.data.repository

import com.app.foodranker.utils.Fallos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quién está bloqueado, en los dos sentidos, y siempre en memoria.
 *
 * ## Por qué un único conjunto y no dos
 *
 * A la hora de pintar da igual quién bloqueó a quién: si hay bloqueo de por medio, ese
 * usuario no sale. Las pantallas no deberían tener que preguntarse en qué dirección va,
 * porque esa es justo la pregunta que alguien contesta mal una vez. Así que [ocultos] es
 * la unión de las dos listas y lo único que las pantallas consultan.
 *
 * ## De dónde sale cada mitad
 *
 *  - `users/{yo}/bloqueados` — a quién he bloqueado yo. La escribe la app.
 *  - `users/{yo}/bloqueadoPor` — quién me ha bloqueado a mí. La escribe SOLO el servidor
 *    (trigger `onBloqueoEscrito`), porque las reglas no me dejan leer la lista del otro:
 *    la lista de a quién has bloqueado es tuya y nadie puede enumerarla.
 *
 * ## Por qué es un Singleton con escuchas vivas
 *
 * El filtro se aplica en una docena larga de sitios. Si cada uno tuviera que leer la
 * lista, serían una docena de consultas por pantalla y, lo que es peor, una docena de
 * sitios donde olvidarse de esperar a que llegue. Con una escucha viva el conjunto ya
 * está en memoria cuando la pantalla lo pide, y una llamada a [bloquear] se refleja en
 * toda la app sin recargar nada.
 *
 * ⚠️ El conjunto empieza VACÍO mientras llega el primer snapshot. Eso quiere decir que
 * en el primer instante tras abrir la app puede colarse contenido de alguien bloqueado.
 * Se asume a propósito: la alternativa es bloquear el arranque de todas las pantallas
 * contra una lectura de red, y el servidor ya impide cualquier interacción. Es un
 * parpadeo, no un agujero.
 */
@Singleton
class BloqueoRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val _ocultos = MutableStateFlow<Set<String>>(emptySet())

    /** Uids que no deben aparecer en ninguna pantalla. Unión de las dos direcciones. */
    val ocultos: StateFlow<Set<String>> = _ocultos.asStateFlow()

    /** A quién he bloqueado YO: lo único que se puede deshacer desde la app. */
    private val _bloqueadosPorMi = MutableStateFlow<Set<String>>(emptySet())
    val bloqueadosPorMi: StateFlow<Set<String>> = _bloqueadosPorMi.asStateFlow()

    private val porMi = MutableStateFlow<Set<String>>(emptySet())
    private val contraMi = MutableStateFlow<Set<String>>(emptySet())

    private var escuchas = mutableListOf<ListenerRegistration>()

    init {
        // Al cambiar de cuenta hay que volver a empezar: las listas son de quien entra.
        // Sin esto, cerrar sesión dejaría los bloqueos del anterior aplicados al
        // siguiente, que es el fallo clásico de un singleton con datos de usuario.
        // `addAuthStateListener` ya dispara al registrarse, así que esto cubre también
        // el arranque: no hace falta una llamada suelta aquí.
        auth.addAuthStateListener { enganchar() }
    }

    private fun enganchar() {
        escuchas.forEach { it.remove() }
        escuchas = mutableListOf()
        porMi.value = emptySet()
        contraMi.value = emptySet()
        recomputar()

        val uid = auth.currentUser?.uid ?: return
        escuchas += firestore.collection("users").document(uid).collection("bloqueados")
            .addSnapshotListener { snap, e ->
                if (e != null) { Fallos.reportar(e, "bloqueo: escuchar bloqueados"); return@addSnapshotListener }
                porMi.value = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
                recomputar()
            }
        escuchas += firestore.collection("users").document(uid).collection("bloqueadoPor")
            .addSnapshotListener { snap, e ->
                if (e != null) { Fallos.reportar(e, "bloqueo: escuchar bloqueadoPor"); return@addSnapshotListener }
                contraMi.value = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
                recomputar()
            }
    }

    private fun recomputar() {
        _bloqueadosPorMi.value = porMi.value
        _ocultos.value = porMi.value + contraMi.value
    }

    /** ¿Hay bloqueo con este usuario, en cualquier dirección? */
    fun estaOculto(uid: String): Boolean = uid.isNotBlank() && uid in _ocultos.value

    /**
     * Bloquear a alguien.
     *
     * Solo escribe el documento. Deshacer los seguimientos —los DOS, también el que esa
     * persona tiene hacia mí, que desde aquí no se puede borrar— y escribir el espejo
     * `bloqueadoPor` es cosa del trigger `onBloqueoEscrito`.
     */
    suspend fun bloquear(otroUid: String): Result<Unit> = try {
        val yo = auth.currentUser?.uid ?: error("sin sesión")
        require(otroUid.isNotBlank() && otroUid != yo) { "uid inválido" }
        firestore.collection("users").document(yo)
            .collection("bloqueados").document(otroUid)
            .set(mapOf("creadoEn" to System.currentTimeMillis())).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Fallos.reportar(e, "bloqueo: bloquear")
        Fallos.fallo(e)
    }

    /**
     * Desbloquear. No restaura los seguimientos que se deshicieron: volver a seguir es
     * una decisión, no algo que deba pasar solo.
     */
    suspend fun desbloquear(otroUid: String): Result<Unit> = try {
        val yo = auth.currentUser?.uid ?: error("sin sesión")
        firestore.collection("users").document(yo)
            .collection("bloqueados").document(otroUid).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Fallos.reportar(e, "bloqueo: desbloquear")
        Fallos.fallo(e)
    }
}
