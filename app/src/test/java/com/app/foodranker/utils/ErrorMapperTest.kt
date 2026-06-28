package com.app.foodranker.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorMapperTest {

    @Test
    fun `maps unknown host exception to network error message`() {
        val e = Exception("java.net.UnknownHostException: Unable to resolve host")
        assertEquals(
            "Sin conexión a internet. Comprueba tu red e inténtalo de nuevo",
            ErrorMapper.toUserMessage(e)
        )
    }

    @Test
    fun `maps PERMISSION_DENIED to permission message`() {
        val e = Exception("PERMISSION_DENIED: Missing or insufficient permissions")
        assertEquals(
            "No tienes permisos para realizar esta acción",
            ErrorMapper.toUserMessage(e)
        )
    }

    @Test
    fun `maps UNAVAILABLE to service unavailable message`() {
        val e = Exception("UNAVAILABLE: The service is currently unavailable")
        assertEquals(
            "Servicio no disponible. Inténtalo en unos minutos",
            ErrorMapper.toUserMessage(e)
        )
    }

    @Test
    fun `maps DEADLINE_EXCEEDED to timeout message`() {
        val e = Exception("DEADLINE_EXCEEDED: deadline exceeded")
        assertEquals(
            "La operación tardó demasiado. Comprueba tu conexión",
            ErrorMapper.toUserMessage(e)
        )
    }

    @Test
    fun `maps NOT_FOUND to content no longer exists message`() {
        val e = Exception("NOT_FOUND: document does not exist")
        assertEquals(
            "El contenido ya no existe",
            ErrorMapper.toUserMessage(e)
        )
    }

    @Test
    fun `maps Cloudinary upload error to storage message`() {
        val e = Exception("Cloudinary upload failed")
        assertEquals(
            "Error al subir la imagen. Inténtalo de nuevo",
            ErrorMapper.toUserMessage(e)
        )
    }

    @Test
    fun `maps CANCELLED to cancelled message`() {
        val e = Exception("CANCELLED: the operation was cancelled")
        assertEquals(
            "Operación cancelada",
            ErrorMapper.toUserMessage(e)
        )
    }

    @Test
    fun `maps unrecognized exception to generic message`() {
        val e = Exception("something totally unexpected")
        assertEquals(
            "Algo salió mal. Inténtalo de nuevo",
            ErrorMapper.toUserMessage(e)
        )
    }

    @Test
    fun `maps exception without message to generic message`() {
        val e = Exception()
        assertEquals(
            "Algo salió mal. Inténtalo de nuevo",
            ErrorMapper.toUserMessage(e)
        )
    }
}
