package com.app.foodranker.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FoodRankerMessagingService : FirebaseMessagingService() {

    @Inject lateinit var firestore: FirebaseFirestore

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return
        val plateId = message.data["plateId"]
        // El canal hay que resolverlo aquí: en segundo plano lo aplica Android desde
        // android.notification.channelId del push, pero en primer plano pasa por aquí y
        // antes caía siempre en el social — un rechazo o una aprobación de moderación
        // acababa mezclado con los likes y con su misma prioridad.
        val channelId = when (message.data["type"]) {
            "moderation_approved", "moderation_rejected" -> NotificationHelper.CHANNEL_MODERATION
            else -> NotificationHelper.CHANNEL_SOCIAL
        }
        NotificationHelper.show(this, title, body, plateId, channelId)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        saveTokenForUser(firestore, userId, token)
    }

    companion object {
        private const val TAG = "FCM"

        fun saveCurrentToken() {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                // Llamado tras auth: Hilt ya inicializó provideFirestore() con los settings,
                // por lo que getInstance() devuelve el mismo singleton ya configurado.
                saveTokenForUser(FirebaseFirestore.getInstance(), userId, token)
            }.addOnFailureListener { e ->
                Log.w(TAG, "Error obteniendo token FCM: ${e.message}")
            }
        }

        /**
         * Guarda el idioma del móvil en el perfil.
         *
         * Lo necesita el servidor: los push los redactan las Cloud Functions, que no tienen
         * forma de saber en qué idioma habla cada persona. Se refresca junto al token, al
         * entrar, para que siga a quien cambie el idioma del teléfono o el de la app.
         */
        fun saveCurrentLanguage() {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val idioma = java.util.Locale.getDefault().language.ifBlank { "es" }
            FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .update("language", idioma)
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error guardando el idioma: ${e.message}")
                }
        }

        private fun saveTokenForUser(firestore: FirebaseFirestore, userId: String, token: String) {
            firestore
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error guardando token FCM: ${e.message}")
                }
        }
    }
}
