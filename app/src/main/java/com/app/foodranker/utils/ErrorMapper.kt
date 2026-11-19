package com.app.foodranker.utils

import android.content.Context
import androidx.annotation.StringRes
import com.app.foodranker.R

object ErrorMapper {

    /**
     * Contexto de aplicación, para poder leer los textos en el idioma activo.
     *
     * Se guarda aquí en vez de pasarlo en cada llamada porque a `toUserMessage` la llaman 21
     * sitios desde nueve ViewModels, que no pueden leer recursos. Es el mismo patrón que ya
     * usan AdManager, CloudinaryManager y compañía: se inicializa en FoodRankerApp.
     */
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Qué clase de error es, como id de recurso.
     *
     * Va aparte de [toUserMessage] para que la decisión se pueda probar sin Android: los
     * tests comparaban el texto en castellano y dejaron de valer en cuanto el mensaje pasó a
     * salir de los recursos.
     */
    @StringRes
    fun toUserMessageRes(e: Exception): Int {
        // Los códigos de FirebaseAuth se miran ANTES que el resto, porque el resto
        // decide por el texto del mensaje y puede coincidir por casualidad: un
        // ERROR_USER_NOT_FOUND acabaría en "no encontrado", que no le dice nada a nadie.
        // El de cuenta suspendida va el primero porque también es de Auth y tiene
        // mensaje propio desde antes.
        if (isDisabledAccountError(e)) return R.string.err_account_disabled
        authCodeRes(e)?.let { return it }
        return when {
            isNetworkError(e)     -> R.string.err_no_internet
            isPermissionError(e)  -> R.string.err_permission
            isUnavailableError(e) -> R.string.err_unavailable
            isTimeoutError(e)     -> R.string.err_timeout
            isNotFoundError(e)    -> R.string.err_not_found
            isStorageError(e)     -> R.string.err_upload
            isCancelledError(e)   -> R.string.err_cancelled
            else                  -> R.string.err_generic2
        }
    }

    /**
     * El mensaje ya traducido. Sin contexto (no debería pasar: se inicializa al arrancar) se
     * devuelve algo legible en vez de reventar justo cuando ya ha fallado otra cosa.
     */
    fun toUserMessage(e: Exception): String =
        appContext?.getString(toUserMessageRes(e)) ?: "Error"

    /**
     * Si el fallo es del entorno del usuario y no un bug nuestro.
     *
     * Lo usa [Fallos] para no llenar Crashlytics de no fatales que no se pueden arreglar
     * desde el código: quien se meta en el metro sin cobertura genera uno por cada carga.
     * Cuentan como del entorno la red, los timeouts —que en la práctica son red lenta: un
     * DEADLINE_EXCEEDED por una consulta pesada de verdad acabaría saliendo igual en las
     * métricas de Firestore— y las cancelaciones.
     *
     * Lo que NO es del entorno sí se reporta: permisos (una regla mal puesta), no
     * encontrado (datos incoherentes), subidas fallidas y cualquier cosa inesperada.
     */
    fun esDelEntorno(e: Exception): Boolean =
        isNetworkError(e) || isTimeoutError(e) || isCancelledError(e) ||
        // Escribir mal la contraseña es del usuario, no un bug: si no, cada dedazo en
        // el login de correo entraría en Crashlytics como no fatal.
        authCodeRes(e) != null

    /**
     * Los fallos del login con correo y contraseña, por `errorCode`.
     *
     * Devuelve null si no es uno de esos, para que [toUserMessageRes] siga mirando.
     *
     * ⚠️ Con la protección contra enumeración de correos activada (lo normal hoy en
     * Firebase), una contraseña incorrecta y un usuario que no existe devuelven **el
     * mismo** `ERROR_INVALID_CREDENTIAL`. Por eso los dos casos comparten mensaje: no es
     * pereza, es que distinguirlos le diría a cualquiera qué correos están registrados.
     */
    @StringRes
    private fun authCodeRes(e: Exception): Int? =
        when ((e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode) {
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_WRONG_PASSWORD",
            "ERROR_USER_NOT_FOUND"      -> R.string.err_auth_credentials
            "ERROR_EMAIL_ALREADY_IN_USE" -> R.string.err_auth_email_in_use
            // Este sale por el camino CONTRARIO: alguien que ya se registró con correo
            // y ahora pulsa Google con esa misma dirección. Firebase trae activada por
            // defecto "una cuenta por dirección", así que no entra — y decirle "ya hay
            // una cuenta con ese correo" le dejaría igual de atascado que antes.
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> R.string.err_auth_use_email_login
            "ERROR_WEAK_PASSWORD"       -> R.string.err_auth_weak_password
            "ERROR_INVALID_EMAIL"       -> R.string.err_auth_invalid_email
            "ERROR_TOO_MANY_REQUESTS"   -> R.string.err_auth_too_many
            else -> null
        }

    // Comprueba el código, no el mensaje (a diferencia del resto de este mapper): el
    // mensaje de FirebaseAuthException varía por locale, el errorCode no.
    private fun isDisabledAccountError(e: Exception) =
        (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode == "ERROR_USER_DISABLED"

    private fun isNetworkError(e: Exception) = e.message?.let {
        it.contains("Unable to resolve host", ignoreCase = true) ||
        it.contains("NETWORK_ERROR", ignoreCase = true) ||
        it.contains("No address associated", ignoreCase = true) ||
        it.contains("Failed to connect", ignoreCase = true) ||
        it.contains("SocketException", ignoreCase = true) ||
        it.contains("UnknownHostException", ignoreCase = true)
    } == true

    private fun isPermissionError(e: Exception) =
        e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true

    private fun isUnavailableError(e: Exception) =
        e.message?.contains("UNAVAILABLE", ignoreCase = true) == true

    private fun isTimeoutError(e: Exception) = e.message?.let {
        it.contains("DEADLINE_EXCEEDED", ignoreCase = true) ||
        it.contains("timeout", ignoreCase = true)
    } == true

    private fun isNotFoundError(e: Exception) =
        e.message?.contains("NOT_FOUND", ignoreCase = true) == true

    private fun isStorageError(e: Exception) = e.message?.let {
        it.contains("Cloudinary", ignoreCase = true) ||
        it.contains("upload", ignoreCase = true)
    } == true

    private fun isCancelledError(e: Exception) =
        e.message?.contains("CANCELLED", ignoreCase = true) == true
}
