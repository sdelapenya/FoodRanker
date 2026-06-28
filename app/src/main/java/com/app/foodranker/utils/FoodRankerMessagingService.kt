package com.app.foodranker.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FoodRankerMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return
        val plateId = message.data["plateId"]
        NotificationHelper.show(this, title, body, plateId)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveToken(token)
    }

    companion object {
        private const val TAG = "FCM"

        fun saveCurrentToken() {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                saveTokenForUser(userId, token)
            }.addOnFailureListener { e ->
                Log.w(TAG, "Error obteniendo token FCM: ${e.message}")
            }
        }

        private fun saveToken(token: String) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            saveTokenForUser(userId, token)
        }

        private fun saveTokenForUser(userId: String, token: String) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error guardando token FCM: ${e.message}")
                }
        }
    }
}
