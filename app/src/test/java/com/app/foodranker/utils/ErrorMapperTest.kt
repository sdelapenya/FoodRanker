package com.app.foodranker.utils

import com.app.foodranker.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorMapperTest {

    @Test
    fun `maps unknown host exception to network error message`() {
        val e = Exception("java.net.UnknownHostException: Unable to resolve host")
        assertEquals(
            R.string.err_no_internet,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps PERMISSION_DENIED to permission message`() {
        val e = Exception("PERMISSION_DENIED: Missing or insufficient permissions")
        assertEquals(
            R.string.err_permission,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps UNAVAILABLE to service unavailable message`() {
        val e = Exception("UNAVAILABLE: The service is currently unavailable")
        assertEquals(
            R.string.err_unavailable,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps DEADLINE_EXCEEDED to timeout message`() {
        val e = Exception("DEADLINE_EXCEEDED: deadline exceeded")
        assertEquals(
            R.string.err_timeout,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps NOT_FOUND to content no longer exists message`() {
        val e = Exception("NOT_FOUND: document does not exist")
        assertEquals(
            R.string.err_not_found,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps Cloudinary upload error to storage message`() {
        val e = Exception("Cloudinary upload failed")
        assertEquals(
            R.string.err_upload,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps CANCELLED to cancelled message`() {
        val e = Exception("CANCELLED: the operation was cancelled")
        assertEquals(
            R.string.err_cancelled,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps unrecognized exception to generic message`() {
        val e = Exception("something totally unexpected")
        assertEquals(
            R.string.err_generic2,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps exception without message to generic message`() {
        val e = Exception()
        assertEquals(
            R.string.err_generic2,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    // ── esDelEntorno: qué llega a Crashlytics y qué no ────────────────────────
    //
    // Este filtro es el que decide si un fallo se reporta (ver Fallos.reportar), así que
    // equivocarse tiene dos formas y las dos son malas: de menos, y la alerta de no
    // fatales suena cada mañana por gente sin cobertura hasta que se apaga; de más, y nos
    // quedamos otra vez sin saber nada, que es el punto de partida.

    @Test
    fun `los fallos de red no se reportan`() {
        assertTrue(ErrorMapper.esDelEntorno(Exception("java.net.UnknownHostException: Unable to resolve host")))
        assertTrue(ErrorMapper.esDelEntorno(Exception("Failed to connect to firestore.googleapis.com")))
    }

    @Test
    fun `los timeouts y las cancelaciones no se reportan`() {
        assertTrue(ErrorMapper.esDelEntorno(Exception("DEADLINE_EXCEEDED: deadline exceeded")))
        assertTrue(ErrorMapper.esDelEntorno(Exception("CANCELLED: The operation was cancelled")))
    }

    @Test
    fun `un permiso denegado si se reporta`() {
        // El caso que más interesa de todos: casi siempre es una regla de Firestore mal
        // puesta, y el usuario nunca la va a reportar porque no sabe que ha pasado nada.
        assertFalse(ErrorMapper.esDelEntorno(Exception("PERMISSION_DENIED: Missing or insufficient permissions")))
    }

    @Test
    fun `un documento que no existe si se reporta`() {
        assertFalse(ErrorMapper.esDelEntorno(Exception("NOT_FOUND: No document to update")))
    }

    @Test
    fun `un fallo de subida si se reporta`() {
        assertFalse(ErrorMapper.esDelEntorno(Exception("Cloudinary upload failed")))
    }

    @Test
    fun `lo inesperado si se reporta`() {
        // Lo que no se sabe clasificar es justo lo que hay que ver.
        assertFalse(ErrorMapper.esDelEntorno(Exception("algo rarísimo")))
        assertFalse(ErrorMapper.esDelEntorno(Exception()))
    }
}
