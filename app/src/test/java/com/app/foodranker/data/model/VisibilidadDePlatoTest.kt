package com.app.foodranker.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Qué platos se le pueden enseñar a cualquiera.
 *
 * Merece test propio porque los dos fallos que cubre **compilan igual de bien**: un plato
 * en moderación y un plato denunciado tienen exactamente el mismo tipo que uno normal, así
 * que si alguien quita el filtro de un listado nada se queja — solo se ve usando la app en
 * el momento justo, que es como estuvo sin filtrar hasta el 2026-11-18.
 */
class VisibilidadDePlatoTest {

    private fun plato(status: String, reportCount: Int = 0) =
        Plate(id = "v__p", name = "Plato", status = status, reportCount = reportCount)

    @Test
    fun `un plato aprobado y sin denuncias se ensena`() {
        assertTrue(plato(PlateStatus.APPROVED).esPublico)
    }

    @Test
    fun `un plato en moderacion no se ensena`() {
        // El caso que importa: nace así y lo aprueba un trigger. Si la función no llega a
        // correr, se queda en pending para siempre.
        assertFalse(plato(PlateStatus.PENDING).esPublico)
    }

    @Test
    fun `un plato rechazado no se ensena`() {
        assertFalse(plato(PlateStatus.REJECTED).esPublico)
    }

    @Test
    fun `un plato sin el campo status no se ensena`() {
        // `status` es String con "" por defecto: un documento sin el campo, o con un valor
        // que no conocemos, cae del lado seguro en vez de colarse por no coincidir con
        // ningún estado conocido.
        assertFalse(plato("").esPublico)
        assertFalse(plato("loquesea").esPublico)
    }

    @Test
    fun `al llegar al umbral de denuncias deja de ensenarse`() {
        assertTrue(plato(PlateStatus.APPROVED, reportCount = UMBRAL_DENUNCIAS - 1).esPublico)
        assertFalse(plato(PlateStatus.APPROVED, reportCount = UMBRAL_DENUNCIAS).esPublico)
        assertFalse(plato(PlateStatus.APPROVED, reportCount = UMBRAL_DENUNCIAS + 1).esPublico)
    }

    @Test
    fun `hacen falta las dos condiciones, no una`() {
        // Denunciado pero aprobado, y limpio pero sin aprobar: ninguno de los dos pasa.
        assertFalse(plato(PlateStatus.APPROVED, reportCount = UMBRAL_DENUNCIAS).esPublico)
        assertFalse(plato(PlateStatus.PENDING, reportCount = 0).esPublico)
    }

    @Test
    fun `soloPublicos conserva el orden de los que deja pasar`() {
        // El orden importa: las pantallas ordenan ANTES de filtrar en varios sitios, así
        // que si el filtro barajase, cambiaría el ranking que ve el usuario.
        val lista = listOf(
            plato(PlateStatus.APPROVED).copy(id = "a"),
            plato(PlateStatus.PENDING).copy(id = "b"),
            plato(PlateStatus.APPROVED).copy(id = "c"),
            plato(PlateStatus.APPROVED, reportCount = UMBRAL_DENUNCIAS).copy(id = "d"),
            plato(PlateStatus.APPROVED).copy(id = "e"),
        )
        assertEquals(listOf("a", "c", "e"), lista.soloPublicos().map { it.id })
    }

    @Test
    fun `soloPublicos con la lista vacia devuelve vacia`() {
        assertTrue(emptyList<Plate>().soloPublicos().isEmpty())
    }
}
