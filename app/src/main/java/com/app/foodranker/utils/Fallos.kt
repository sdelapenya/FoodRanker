package com.app.foodranker.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CancellationException

/**
 * Deja constancia en Crashlytics de un fallo que la app se tragó.
 *
 * Hasta ahora la app no llamaba a `recordException` ni una vez: lo único que hacía con
 * Crashlytics era encender la recogida en [com.app.foodranker.FoodRankerApp]. Eso quiere
 * decir que de los 78 `catch` que hay repartidos **no se enteraba nadie**: ni el usuario,
 * al que se le queda la pantalla a medias, ni nosotros. La alerta de "errores recuperables
 * nuevos" que se configuró el 2026-11-16 estaba conectada a nada, porque nunca se
 * registraba ninguno.
 *
 * La diferencia que introduce esto no es para el usuario —a él se le sigue fallando
 * igual— sino para nosotros: es la diferencia entre "la app va bien" y "la app va bien
 * **que yo sepa**".
 *
 * ## Por qué NO se reporta todo
 *
 * Un no fatal por cada fallo sería peor que ninguno. Quien vaya en el metro sin cobertura
 * genera un fallo en cada carga, y eso no es un bug nuestro: es ruido que entierra la
 * señal y que encima dispararía la alerta por correo todas las mañanas. Así que se filtra
 * por lo que ya sabe clasificar [ErrorMapper] (ver [ErrorMapper.esDelEntorno]).
 *
 * Lo que sí interesa que llegue:
 *  - `PERMISSION_DENIED` → casi siempre una regla de Firestore mal puesta. Es la forma
 *    más rápida de encontrarlas, porque el usuario nunca las va a reportar.
 *  - `NOT_FOUND` → datos incoherentes, algo que apunta a un documento que ya no está.
 *  - fallos de subida (Cloudinary) → problema nuestro, y el usuario pierde su foto.
 *  - cualquier cosa inesperada → justo lo que no se puede prever desde aquí.
 *
 * ## Por qué esta función SÍ puede lanzar (y tiene que hacerlo)
 *
 * `catch (e: Exception)` captura también [CancellationException], que no es un fallo: es
 * la señal de que a la corrutina le han dicho que pare. Tragársela rompe la concurrencia
 * estructurada — el `cancel()` deja de significar nada y el `catch` sigue ejecutando el
 * cuerpo, así que un ViewModel que se está destruyendo todavía escribe su `_uiState`, o
 * pinta un error al usuario por una carga que él mismo canceló al salir de la pantalla.
 *
 * Por eso [reportar] **relanza la cancelación** en vez de simplemente no reportarla. Que
 * una función con este nombre lance es poco intuitivo, y va aquí a propósito: es el único
 * punto por el que pasan los 68 `catch` de la app, y arreglarlo aquí los arregla todos.
 * La alternativa era un `catch (e: CancellationException) { throw e }` repetido 68 veces,
 * que es justo la clase de línea que alguien olvida al añadir el sitio 69.
 *
 * Esto funciona porque en los 68 sitios [reportar] es la **primera** sentencia del
 * `catch`: lo que se salta al relanzar es precisamente lo que no se debe ejecutar cuando
 * ya no hay a quién enseñárselo. Y relanzar dentro de una corrutina no es un fallo: es el
 * camino normal de la cancelación, la maquinaria lo recoge y no revienta la app.
 *
 * ⚠️ Dos cosas que hay que mantener para que siga siendo seguro:
 *  - **Que siga siendo la primera sentencia del `catch`.** Si se llama al final, lo de
 *    arriba ya se ha ejecutado y el relanzado no arregla nada.
 *  - **Nada de `withTimeout`.** `TimeoutCancellationException` **es** una
 *    [CancellationException], así que un `catch` que pretenda enseñar "se ha agotado el
 *    tiempo" dejaría de funcionar. Hoy no hay ni uno en la app (comprobado); si se añade,
 *    ese `catch` tiene que capturar el timeout antes de llamar aquí.
 */
object Fallos {

    /**
     * @param donde una etiqueta corta y estable que diga qué se estaba intentando
     *   ("cargar perfil", "borrar colección"). Va como breadcrumb del informe: Crashlytics
     *   agrupa los no fatales por traza, no por este texto, así que sirve para leer el
     *   informe, no para separarlos.
     * @throws CancellationException si [e] lo es, para no romper la concurrencia
     *   estructurada. Ver la nota de la clase: es intencionado.
     */
    fun reportar(e: Throwable, donde: String) {
        // No es "no reportar la cancelación", es "no tragársela". Ver el KDoc de arriba.
        if (e is CancellationException) throw e
        if (e is Exception && ErrorMapper.esDelEntorno(e)) return
        FirebaseCrashlytics.getInstance().apply {
            log(donde)
            recordException(e)
        }
    }

    /**
     * `Result.failure(e)` que **no se traga la cancelación**.
     *
     * ## Por qué hace falta, teniendo ya [reportar]
     *
     * [reportar] arregla los 64 `catch` que pasan por él, pero los repositorios no lo
     * llaman: devuelven el fallo al que llamó, con `catch (e: Exception) {
     * Result.failure(e) }`, y que un `get()` falle no es un bug que haya que reportar.
     *
     * El problema es que ese `catch` **también captura la cancelación**. Cuando el usuario
     * sale de una pantalla, el scope del ViewModel se cancela, el `await()` de Firestore
     * lanza `CancellationException`… y en vez de propagarse, se empaqueta en un
     * `Result.failure`. El `onFailure` de arriba se ejecuta tan contento y le **pinta un
     * error al usuario por una carga que él mismo canceló al irse**. Es justo lo que el
     * KDoc de esta clase describe, en los sitios que no pasan por [reportar].
     *
     * Se usa igual que `Result.failure`, y por eso vive aquí y no suelto: para que el
     * relanzado esté en el **mismo fichero** que el de [reportar] y se vean los dos juntos.
     *
     * @throws CancellationException si [e] lo es.
     */
    fun <T> fallo(e: Throwable): Result<T> {
        if (e is CancellationException) throw e
        return Result.failure(e)
    }
}
