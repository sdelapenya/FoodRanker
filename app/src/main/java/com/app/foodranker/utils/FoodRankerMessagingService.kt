package com.app.foodranker.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.installations.FirebaseInstallations
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
        // El reparto completo vive en NotificationHelper.canalDe, junto a la definición de
        // los canales: tenerlo aquí hacía fácil olvidarse de un tipo nuevo y que acabara
        // en el canal de interacciones por descarte, que es como "platos de gente que
        // sigues" pasó dos meses mezclado con los likes.
        val channelId = NotificationHelper.canalDe(message.data["type"])
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

        /**
         * Nombre de la subcolección privada donde va el token de cada móvil.
         * El reparto entre este sitio y el campo del perfil está explicado en
         * `firestore.rules`, en la regla de `users/{userId}/dispositivos`.
         */
        const val COLECCION_DISPOSITIVOS = "dispositivos"

        private fun saveTokenForUser(firestore: FirebaseFirestore, userId: String, token: String) {
            // 1) Donde ha estado siempre. El servidor sigue leyendo de aquí, así que esto
            //    NO se puede quitar hasta que la v22 esté repartida y las funciones lean
            //    de la subcolección. Ver firestore.rules.
            firestore
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error guardando token FCM: ${e.message}")
                }

            // 2) El sitio nuevo: privado y uno por móvil. Es puramente aditivo — si esto
            //    falla, los avisos siguen funcionando por (1). Se hace así a propósito:
            //    este cambio no se ha podido probar con una sesión real (el emulador no
            //    tiene cuenta de Google), así que tenía que ser incapaz de romper nada.
            guardarTokenDelDispositivo(firestore, userId, token)
        }

        /**
         * Escribe el token en `users/{uid}/dispositivos/{idInstalacion}`.
         *
         * El id del documento es el **Installation ID** de Firebase, que es estable para
         * esta instalación de la app. Se usa ese y no el propio token porque el token
         * rota (reinstalar, borrar datos, a veces solo con el tiempo): con el token como
         * id se iría acumulando un documento muerto por cada rotación, y el servidor
         * acabaría mandando cada aviso varias veces al mismo móvil. Con el id de
         * instalación, una rotación simplemente reescribe el mismo documento.
         */
        private fun guardarTokenDelDispositivo(
            firestore: FirebaseFirestore,
            userId: String,
            token: String
        ) {
            FirebaseInstallations.getInstance().id
                .addOnSuccessListener { idInstalacion ->
                    firestore
                        .collection("users").document(userId)
                        .collection(COLECCION_DISPOSITIVOS).document(idInstalacion)
                        .set(
                            mapOf(
                                "token" to token,
                                "plataforma" to "android",
                                "actualizadoEn" to System.currentTimeMillis()
                            )
                        )
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error guardando el token del dispositivo: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error obteniendo el id de instalación: ${e.message}")
                }
        }
    }
}
