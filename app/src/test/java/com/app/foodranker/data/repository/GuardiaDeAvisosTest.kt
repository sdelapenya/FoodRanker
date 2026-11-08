package com.app.foodranker.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El fallo que cubren estos tests se reportó así: "la app notifica las cosas cuando estás
 * fuera de la app y de nuevo vuelven a saltar cuando entras a la app". Pasaba en cada
 * arranque en frío, y no se veía compilando: hacía falta usar la app para notarlo.
 */
class GuardiaDeAvisosTest {

    private val cache = true
    private val servidor = false

    @Test
    fun `un snapshot de cache no avisa nunca`() {
        val guardia = GuardiaDeAvisos()
        assertFalse(guardia.debeNotificar(cache))
    }

    @Test
    fun `el primer snapshot del servidor es la linea base y no avisa`() {
        val guardia = GuardiaDeAvisos()
        assertFalse(guardia.debeNotificar(servidor))
    }

    @Test
    fun `a partir del segundo snapshot del servidor si avisa`() {
        val guardia = GuardiaDeAvisos()
        guardia.debeNotificar(servidor)
        assertTrue(guardia.debeNotificar(servidor))
        assertTrue(guardia.debeNotificar(servidor))
    }

    /**
     * El test del fallo de verdad. Con el `isFirstLoad` de antes, la llamada de la caché
     * gastaba el flag y el **primer** snapshot del servidor ya notificaba: justo el que
     * trae todo lo ocurrido mientras la app estaba cerrada, que el usuario ya había
     * recibido por push. Si alguien vuelve a poner un flag que se apague con la caché,
     * esto se pone rojo.
     */
    @Test
    fun `la cache no gasta la linea base del servidor`() {
        val guardia = GuardiaDeAvisos()
        guardia.debeNotificar(cache)
        guardia.debeNotificar(cache)
        guardia.debeNotificar(cache)
        assertFalse(
            "el primer snapshot del servidor trae lo ocurrido con la app cerrada: no avisa",
            guardia.debeNotificar(servidor)
        )
        assertTrue(guardia.debeNotificar(servidor))
    }

    @Test
    fun `la cache sigue sin avisar despues de la linea base`() {
        val guardia = GuardiaDeAvisos()
        guardia.debeNotificar(servidor)
        assertFalse(guardia.debeNotificar(cache))
        assertTrue(guardia.debeNotificar(servidor))
    }

    /**
     * `startListening` reinicia al cambiar de usuario (o al reengancharse). Sin esto, el
     * primer snapshot del servidor del usuario nuevo notificaría su historial entero.
     */
    @Test
    fun `reiniciar vuelve a exigir una linea base`() {
        val guardia = GuardiaDeAvisos()
        guardia.debeNotificar(servidor)
        assertTrue(guardia.debeNotificar(servidor))

        guardia.reiniciar()
        assertFalse(guardia.debeNotificar(servidor))
        assertTrue(guardia.debeNotificar(servidor))
    }
}
