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
 * ## Por qué no relanza la cancelación
 *
 * `catch (e: Exception)` captura también `CancellationException`, así que ahora mismo los
 * ViewModels se tragan las cancelaciones y rompen la concurrencia estructurada. Lo
 * correcto sería relanzarla, pero eso cambia el flujo de control en 78 sitios de una app
 * que ya está en producción y que no se puede probar a mano entera. Aquí solo se evita
 * **reportarla** (una cancelación no es un fallo). Arreglar el tragado va aparte, con su
 * propia prueba; está apuntado en docs/HANDOFF.md.
 */
object Fallos {

    /**
     * @param donde una etiqueta corta y estable que diga qué se estaba intentando
     *   ("cargar perfil", "borrar colección"). Va como breadcrumb del informe: Crashlytics
     *   agrupa los no fatales por traza, no por este texto, así que sirve para leer el
     *   informe, no para separarlos.
     */
    fun reportar(e: Throwable, donde: String) {
        if (e is CancellationException) return
        if (e is Exception && ErrorMapper.esDelEntorno(e)) return
        FirebaseCrashlytics.getInstance().apply {
            log(donde)
            recordException(e)
        }
    }
}
