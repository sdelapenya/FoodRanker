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
import com.app.foodranker.R

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
                            val fromUser = data["fromUserName"] as? String
                                ?: context.getString(R.string.notif_from_somebody)
                            val plateName = data["plateName"] as? String
                                ?: context.getString(R.string.notif_your_plate)
                            val plateId = data["plateId"] as? String
                            fun txt(id: Int, vararg args: Any) =
                                context.getString(id, *args)
                            // Esta lista tiene que cubrir TODOS los tipos que crea el
                            // servidor. Es un camino paralelo al push de FCM, y lo que no
                            // esté aquí se descarta sin dejar rastro: así los comentarios
                            // aparecían en la campana pero no avisaban de nada (reportado
                            // por un tester). Al añadir un tipo nuevo, añadirlo también aquí.
                            //
                            // ⚠️ Y el texto tiene que ser IDÉNTICO al de TEXTOS en
                            // functions/src/index.ts: el id del aviso sale del hash del
                            // propio texto (ver NotificationHelper.show), así que si los
                            // dos caminos redactan distinto el mismo hecho se ve DOS veces.
                            // Lo vigila tools/i18n/check_notif_parity.py.
                            val (title, body) = when (type) {
                                "like" -> txt(R.string.notif_like_t) to
                                        txt(R.string.notif_like_b, fromUser, plateName)
                                "rating" -> {
                                    val score = data["score"] as? Double ?: 0.0
                                    txt(R.string.notif_rating_t) to txt(
                                        R.string.notif_rating_b, fromUser, plateName,
                                        "%.1f".format(score)
                                    )
                                }
                                "comment" -> {
                                    val texto = data["commentText"] as? String ?: ""
                                    txt(R.string.notif_comment_t) to
                                            if (texto.isNotBlank())
                                                txt(R.string.notif_comment_b, fromUser, plateName, texto)
                                            else
                                                txt(R.string.notif_comment_b_plain, fromUser, plateName)
                                }
                                "follow" -> txt(R.string.notif_follow_t) to
                                        txt(R.string.notif_follow_b, fromUser)
                                "new_plate" -> txt(R.string.notif_newplate_t) to
                                        txt(R.string.notif_newplate_b, fromUser, plateName)
                                "moderation_approved" -> txt(R.string.notif_approved_t) to
                                        txt(R.string.notif_approved_b, plateName)
                                "moderation_rejected" -> txt(R.string.notif_rejected_t) to
                                        txt(R.string.notif_rejected_b, plateName)
                                "level_up" -> txt(R.string.notif_level_t) to
                                        txt(R.string.notif_level_b, plateName)
                                "badge" -> txt(R.string.notif_badge_t) to
                                        txt(R.string.notif_badge_b, plateName)
                                "league_result" -> {
                                    val puesto = (data["position"] as? Long)?.toInt() ?: 0
                                    val medalla = if (puesto == 1) "🥇" else "🏅"
                                    txt(R.string.notif_league_t, medalla, puesto) to
                                            if (puesto == 1) txt(R.string.notif_league_b_win)
                                            else txt(R.string.notif_league_b_other, puesto)
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
