package com.app.foodranker.ui.screens.league

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * El caso que importa es el primero: `weekKey` empieza vacía y la cabecera de la liga se
 * pinta antes de que cargue nada, así que indexar sin comprobar el tamaño reventaba la
 * pantalla al abrirla.
 */
class NumeroDeSemanaTest {

    @Test
    fun `una clave vacia no revienta y no da numero`() {
        assertNull(numeroDeSemana(""))
    }

    @Test
    fun `saca el numero de una clave normal`() {
        assertEquals("41", numeroDeSemana("2026-W41"))
    }

    @Test
    fun `una clave sin el separador no da numero`() {
        assertNull(numeroDeSemana("2026"))
        assertNull(numeroDeSemana("sin separador"))
    }

    @Test
    fun `una clave con separador de mas no da numero`() {
        assertNull(numeroDeSemana("2026-W41-W2"))
    }

    @Test
    fun `un numero en blanco cuenta como ausente`() {
        assertNull(numeroDeSemana("2026-W"))
    }
}
