package com.app.foodranker.data.repository

import android.content.Context
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.app.foodranker.utils.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    private var listener: ListenerRegistration? = null
    private var isFirstLoad = true
    @Volatile private var activeUserId = ""

    fun startListening(userId: String) {
        if (userId.isEmpty()) return
        if (userId == activeUserId && listener != null) return
        activeUserId = userId
        listener?.remove()
        isFirstLoad = true
        listener = firestore.collection("notifications")
            .document(userId).collection("items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                _unreadCount.value = snapshot.documents.count { it.get("isRead") != true }
                if (!isFirstLoad) {
                    snapshot.documentChanges
                        .filter { it.type == DocumentChange.Type.ADDED }
                        .forEach { change ->
                            val data = change.document.data
                            val type = data["type"] as? String ?: return@forEach
                            val fromUser = data["fromUserName"] as? String ?: "Alguien"
                            val plateName = data["plateName"] as? String ?: "tu plato"
                            val plateId = data["plateId"] as? String
                            // Esta lista tiene que cubrir TODOS los tipos que crea el
                            // servidor. Es un camino paralelo al push de FCM, y lo que no
                            // esté aquí se descarta sin dejar rastro: así los comentarios
                            // aparecían en la campana pero no avisaban de nada (reportado
                            // por un tester). Al añadir un tipo nuevo, añadirlo también aquí.
                            val (title, body) = when (type) {
                                "like" -> "❤️ Nuevo me gusta" to
                                        "$fromUser le ha dado like a \"$plateName\""
                                "rating" -> {
                                    val score = data["score"] as? Double ?: 0.0
                                    "⭐ Nueva valoración" to
                                            "$fromUser ha valorado \"$plateName\" con ${"%.1f".format(score)}"
                                }
                                "comment" -> {
                                    val texto = data["commentText"] as? String ?: ""
                                    "💬 Nuevo comentario" to
                                            if (texto.isNotBlank()) "$fromUser en \"$plateName\": $texto"
                                            else "$fromUser ha comentado \"$plateName\""
                                }
                                "follow" -> "✨ Nuevo seguidor" to "$fromUser ha empezado a seguirte"
                                "moderation_approved" -> "✅ Tu plato ya está publicado" to
                                        "\"$plateName\" ya aparece en el ranking."
                                "moderation_rejected" -> "Plato no aprobado" to
                                        "\"$plateName\" no cumple las normas de la comunidad."
                                "level_up" -> "🎉 Has subido de nivel" to
                                        "Ya eres $plateName"
                                "badge" -> "🏅 Logro desbloqueado" to
                                        "Has conseguido $plateName"
                                "league_result" -> {
                                    val puesto = (data["position"] as? Long)?.toInt() ?: 0
                                    val medalla = if (puesto == 1) "🥇" else "🏅"
                                    "$medalla Has quedado ${puesto}º en la liga" to
                                            if (puesto == 1) "Ganaste la liga semanal."
                                            else "La liga vuelve a empezar hoy."
                                }
                                else -> return@forEach
                            }
                            val channelId = when (type) {
                                "moderation_approved", "moderation_rejected" ->
                                    NotificationHelper.CHANNEL_MODERATION
                                else -> NotificationHelper.CHANNEL_SOCIAL
                            }
                            NotificationHelper.show(context, title, body, plateId, channelId)
                        }
                }
                isFirstLoad = false
            }
    }

    suspend fun markAllRead(userId: String) {
        try {
            val snapshot = firestore.collection("notifications")
                .document(userId).collection("items")
                .whereEqualTo("isRead", false)
                .limit(100).get().await()
            if (snapshot.documents.isEmpty()) return
            val batch = firestore.batch()
            snapshot.documents.forEach { batch.update(it.reference, "isRead", true) }
            batch.commit().await()
        } catch (e: Exception) {
            android.util.Log.e("NotifRepo", "Error markAllRead: ${e.message}")
        }
    }
}
