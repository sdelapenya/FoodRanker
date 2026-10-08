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
    fun toUserMessageRes(e: Exception): Int = when {
            isDisabledAccountError(e) -> R.string.err_account_disabled
            isNetworkError(e)     -> R.string.err_no_internet
            isPermissionError(e)  -> R.string.err_permission
            isUnavailableError(e) -> R.string.err_unavailable
            isTimeoutError(e)     -> R.string.err_timeout
            isNotFoundError(e)    -> R.string.err_not_found
            isStorageError(e)     -> R.string.err_upload
            isCancelledError(e)   -> R.string.err_cancelled
            else                  -> R.string.err_generic2
    }

    /**
     * El mensaje ya traducido. Sin contexto (no debería pasar: se inicializa al arrancar) se
     * devuelve algo legible en vez de reventar justo cuando ya ha fallado otra cosa.
     */
    fun toUserMessage(e: Exception): String =
        appContext?.getString(toUserMessageRes(e)) ?: "Error"

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
