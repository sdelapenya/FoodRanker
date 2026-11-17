package com.app.foodranker.utils

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlinx.coroutines.CancellationException

/**
 * Fija el contrato de [Fallos.reportar] sin Firebase delante.
 *
 * Se puede probar en un test de JVM puro gracias a una propiedad del diseño: los dos casos
 * que se filtran (cancelación y fallo del entorno) deciden **antes** de tocar
 * `FirebaseCrashlytics.getInstance()`. Eso convierte "no ha reventado" en una aserción de
 * verdad: si alguien reordena las comprobaciones y el filtro deja de ser lo primero, el
 * `getInstance()` sin Firebase inicializado lanza y estos tests se ponen rojos.
 *
 * El caso que sí reporta no se prueba aquí a propósito: haría falta Robolectric y un
 * FirebaseApp de mentira para comprobar algo que ya se ve en la consola de Crashlytics.
 */
class FallosTest {

    @Test
    fun `la cancelacion se relanza, no se traga`() {
        // Esto es lo que arregla el bug: un `catch (e: Exception)` captura también la
        // cancelación, y hasta ahora reportar() volvía sin más — la corrutina cancelada
        // seguía ejecutando el resto del catch como si nada hubiera pasado.
        val cancelacion = CancellationException("Job was cancelled")
        try {
            Fallos.reportar(cancelacion, "prueba: cancelación")
            fail("tenía que haber relanzado la cancelación")
        } catch (e: CancellationException) {
            assertTrue(e === cancelacion)
        }
    }

    @Test
    fun `relanza la MISMA excepcion, no una copia`() {
        // No es un detalle: la maquinaria de corrutinas compara la instancia para saber si
        // la cancelación es la suya. Envolverla en otra la convertiría en un fallo de
        // verdad y la corrutina padre caería con ella.
        val cancelacion = CancellationException()
        val relanzada = try {
            Fallos.reportar(cancelacion, "prueba: identidad")
            null
        } catch (e: CancellationException) {
            e
        }
        assertTrue(relanzada === cancelacion)
    }

    @Test
    fun `un fallo del entorno se descarta en silencio`() {
        // Sin red no hay bug que arreglar. Y como se descarta antes de pedir el
        // FirebaseCrashlytics, esto no lanza aunque Firebase no exista en el test.
        Fallos.reportar(
            Exception("Unable to resolve host \"firestore.googleapis.com\""),
            "prueba: sin red"
        )
        Fallos.reportar(Exception("DEADLINE_EXCEEDED"), "prueba: timeout")
    }

    @Test
    fun `un fallo del entorno NO se relanza`() {
        // La cancelación se relanza y el fallo de red no: son caminos distintos y es fácil
        // confundirlos al tocar el filtro, porque ErrorMapper.esDelEntorno también
        // considera "del entorno" los mensajes con CANCELLED.
        try {
            Fallos.reportar(Exception("CANCELLED: la llamada se canceló"), "prueba: mensaje")
        } catch (e: Throwable) {
            fail("un Exception normal no se relanza, aunque su mensaje hable de cancelación: ${e}")
        }
    }
}
