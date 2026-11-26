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

    // ── Fallos.fallo(): el camino de los repositorios ────────────────────────────

    @Test
    fun `fallo() relanza la cancelacion en vez de empaquetarla`() {
        // El bug que arregla: `catch (e: Exception) { Result.failure(e) }` convertía la
        // cancelación en un Result.failure, el `onFailure` de arriba se ejecutaba y al
        // usuario le salía un error por una carga que él mismo canceló al salir de la
        // pantalla.
        val cancelacion = CancellationException("Job was cancelled")
        try {
            val r: Result<String> = Fallos.fallo(cancelacion)
            fail("tenía que haber relanzado, y devolvió $r")
        } catch (e: CancellationException) {
            assertTrue(e === cancelacion)
        }
    }

    @Test
    fun `fallo() devuelve un Result fallido para todo lo demas`() {
        // Lo normal: un fallo de red o de permisos sí tiene que llegar al que llamó, que
        // es quien sabe qué enseñarle al usuario.
        val e = Exception("PERMISSION_DENIED")
        val r: Result<String> = Fallos.fallo(e)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull() === e)
    }

    @Test
    fun `fallo() NO se confunde con un mensaje que hable de cancelacion`() {
        // Misma trampa que en reportar(): "CANCELLED" en el texto de un Exception normal
        // no es una CancellationException, y tragárselo sería perder el error de verdad.
        val e = Exception("CANCELLED: la llamada se canceló")
        val r: Result<String> = Fallos.fallo(e)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull() === e)
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
