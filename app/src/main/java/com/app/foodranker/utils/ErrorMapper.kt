package com.app.foodranker.utils

object ErrorMapper {

    fun toUserMessage(e: Exception): String = when {
        isNetworkError(e)     -> "Sin conexión a internet. Comprueba tu red e inténtalo de nuevo"
        isPermissionError(e)  -> "No tienes permisos para realizar esta acción"
        isUnavailableError(e) -> "Servicio no disponible. Inténtalo en unos minutos"
        isTimeoutError(e)     -> "La operación tardó demasiado. Comprueba tu conexión"
        isNotFoundError(e)    -> "El contenido ya no existe"
        isStorageError(e)     -> "Error al subir la imagen. Inténtalo de nuevo"
        isCancelledError(e)   -> "Operación cancelada"
        else                  -> "Algo salió mal. Inténtalo de nuevo"
    }

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
