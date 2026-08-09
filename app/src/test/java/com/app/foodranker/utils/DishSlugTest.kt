package com.app.foodranker.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DishSlugTest {

    @Test
    fun `minusculas y espacios pasan a guiones`() {
        assertEquals("spaghetti-carbonara", "Spaghetti Carbonara".toDishSlug())
    }

    @Test
    fun `quita acentos`() {
        assertEquals("pure-de-patata", "Puré de patata".toDishSlug())
        assertEquals("jamon-iberico", "Jamón ibérico".toDishSlug())
    }

    @Test
    fun `la enye colapsa con la n — mismo plato mal escrito`() {
        assertEquals("noquis", "Ñoquis".toDishSlug())
        assertEquals("Noquis".toDishSlug(), "Ñoquis".toDishSlug())
    }

    @Test
    fun `la puntuacion no genera slugs distintos`() {
        assertEquals("pizza-4-quesos", "Pizza 4 quesos".toDishSlug())
        assertEquals("pizza-4-quesos", "¡Pizza 4 quesos!".toDishSlug())
        assertEquals("pizza-4-quesos", "Pizza (4 quesos)".toDishSlug())
    }

    @Test
    fun `espacios de sobra y mayusculas no importan`() {
        assertEquals("tortilla-de-patatas", "  TORTILLA   de   Patatas  ".toDishSlug())
    }

    @Test
    fun `un nombre solo de simbolos da slug vacio`() {
        assertEquals("", "!!!".toDishSlug())
        assertEquals("", "   ".toDishSlug())
    }

    @Test
    fun `platos distintos NO colapsan — limitacion conocida y buscada`() {
        // El slug no pretende resolver esto; lo resuelve la UI de sugerencias.
        assertTrue("carbonara".toDishSlug() != "spaghetti-carbonara".toDishSlug())
    }

    @Test
    fun `plateDocId compone venue y slug`() {
        assertEquals("ChIJabc123__carbonara", plateDocId("ChIJabc123", "Carbonara"))
    }

    @Test
    fun `plateDocId devuelve vacio si falta algun componente`() {
        assertEquals("", plateDocId("", "Carbonara"))
        assertEquals("", plateDocId("ChIJabc123", "!!!"))
        assertEquals("", plateDocId("ChIJabc123", ""))
    }

    @Test
    fun `el mismo plato escrito distinto produce el mismo id`() {
        val a = plateDocId("ChIJabc", "Tortilla de Patatas")
        val b = plateDocId("ChIJabc", "  tortilla de patatas ")
        val c = plateDocId("ChIJabc", "Tortilla de patatás")
        assertEquals(a, b)
        assertEquals(a, c)
    }
}
