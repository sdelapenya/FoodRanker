package com.app.foodranker.utils

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeepLinkParserTest {

    // --- parseFoodRankerProfileUserId ---

    @Test
    fun `https profile link returns the user id`() {
        val uri = Uri.parse("https://foodranker.app/user/abc123")
        assertEquals("abc123", uri.parseFoodRankerProfileUserId())
    }

    @Test
    fun `custom scheme profile link returns the user id`() {
        val uri = Uri.parse("foodranker://user/abc123")
        assertEquals("abc123", uri.parseFoodRankerProfileUserId())
    }

    @Test
    fun `plate link does not match profile parser`() {
        val uri = Uri.parse("https://foodranker.app/plate/xyz789")
        assertNull(uri.parseFoodRankerProfileUserId())
    }

    @Test
    fun `https profile link without id returns null`() {
        val uri = Uri.parse("https://foodranker.app/user/")
        assertNull(uri.parseFoodRankerProfileUserId())
    }

    // --- parseFoodRankerPlateId ---

    @Test
    fun `https plate link returns the plate id`() {
        val uri = Uri.parse("https://foodranker.app/plate/xyz789")
        assertEquals("xyz789", uri.parseFoodRankerPlateId())
    }

    @Test
    fun `custom scheme plate link returns the plate id`() {
        val uri = Uri.parse("foodranker://plate/xyz789")
        assertEquals("xyz789", uri.parseFoodRankerPlateId())
    }

    @Test
    fun `profile link does not match plate parser`() {
        val uri = Uri.parse("https://foodranker.app/user/abc123")
        assertNull(uri.parseFoodRankerPlateId())
    }

    // --- parseFoodRankerInviteCode ---

    @Test
    fun `https invite link returns the invite code`() {
        val uri = Uri.parse("https://foodranker.app/invite/REF42")
        assertEquals("REF42", uri.parseFoodRankerInviteCode())
    }

    // Antes este test afirmaba lo contrario: que el esquema propio NO valia para las
    // invitaciones. Era el reflejo de un fallo, no una decision — el boton "Ya tengo la app"
    // de open.html manda aqui, y con invitaciones no hacia nada.
    @Test
    fun `custom scheme invite link returns the invite code`() {
        val uri = Uri.parse("foodranker://invite/REF42")
        assertEquals("REF42", uri.parseFoodRankerInviteCode())
    }

    @Test
    fun `custom scheme invite without code returns null`() {
        val uri = Uri.parse("foodranker://invite/")
        assertNull(uri.parseFoodRankerInviteCode())
    }

    // --- El dominio actual, y el que se acepta por si se registra algun dia ---
    // Los tests de arriba usan foodranker.app a proposito: es el dominio viejo, sigue
    // aceptado, y si alguien lo quita de DeepLinks.ACCEPTED_HOSTS tienen que ponerse
    // rojos. Un enlace ya compartido no se puede apagar.

    @Test
    fun `current host parses all three link types`() {
        assertEquals("abc123", Uri.parse("https://foodranker.web.app/user/abc123").parseFoodRankerProfileUserId())
        assertEquals("xyz789", Uri.parse("https://foodranker.web.app/plate/xyz789").parseFoodRankerPlateId())
        assertEquals("REF42",  Uri.parse("https://foodranker.web.app/invite/REF42").parseFoodRankerInviteCode())
    }

    @Test
    fun `generated links are parsed back`() {
        assertEquals("abc123", Uri.parse(DeepLinks.user("abc123")).parseFoodRankerProfileUserId())
        assertEquals("xyz789", Uri.parse(DeepLinks.plate("xyz789")).parseFoodRankerPlateId())
        assertEquals("REF42",  Uri.parse(DeepLinks.invite("REF42")).parseFoodRankerInviteCode())
    }

    @Test
    fun `http is not accepted, only https`() {
        assertNull(Uri.parse("http://foodranker.web.app/plate/xyz789").parseFoodRankerPlateId())
    }

    @Test
    fun `unrelated host returns null for all parsers`() {
        val uri = Uri.parse("https://example.com/user/abc123")
        assertNull(uri.parseFoodRankerProfileUserId())
        assertNull(uri.parseFoodRankerPlateId())
        assertNull(uri.parseFoodRankerInviteCode())
    }

    // --- ids que romperian la ruta de Navigation (2026-11-16) ---
    //
    // `createRoute` interpola sin codificar ("plate/$plateId"), asi que un id con
    // caracteres raros produce una ruta que no casa con "plate/{plateId}" y navigate()
    // lanza IllegalArgumentException, cerrando la app. Con los App Links verificados,
    // cualquiera puede repartir uno de estos enlaces.

    @Test
    fun `una barra codificada no se cuela como id de plato`() {
        // El caso peligroso: pathSegments DECODIFICA, asi que %2F reaparece como "/".
        val uri = Uri.parse("https://foodranker.app/plate/abc%2Fdef")
        assertEquals("abc/def", uri.pathSegments[1])   // deja constancia de por que hace falta
        assertNull(uri.parseFoodRankerPlateId())
    }

    @Test
    fun `un espacio no se cuela como id de plato`() {
        assertNull(Uri.parse("https://foodranker.app/plate/abc%20def").parseFoodRankerPlateId())
    }

    @Test
    fun `los ids raros tampoco pasan por el esquema propio`() {
        assertNull(Uri.parse("foodranker://plate/abc%2Fdef").parseFoodRankerPlateId())
        assertNull(Uri.parse("foodranker://user/a%20b").parseFoodRankerProfileUserId())
        assertNull(Uri.parse("foodranker://invite/a%23b").parseFoodRankerInviteCode())
    }

    @Test
    fun `un id absurdamente largo se rechaza`() {
        val largo = "a".repeat(300)
        assertNull(Uri.parse("https://foodranker.app/plate/$largo").parseFoodRankerPlateId())
    }

    @Test
    fun `los ids REALES siguen pasando — esto es lo que no se puede romper`() {
        // Plato: {placeId}__{dishSlug}, tal y como lo compone plateDocId().
        assertEquals(
            "ChIJ17p_qV9HoBIRTRSfEBcZuhk__arroz-illa",
            Uri.parse("https://foodranker.app/plate/ChIJ17p_qV9HoBIRTRSfEBcZuhk__arroz-illa")
                .parseFoodRankerPlateId()
        )
        // Perfil: uid de Firebase.
        assertEquals(
            "rNpwrHo3euaQSJjwsGFGX2vHr193",
            Uri.parse("https://foodranker.app/user/rNpwrHo3euaQSJjwsGFGX2vHr193")
                .parseFoodRankerProfileUserId()
        )
        // Invitacion: UUID sin guiones, en mayusculas (ReferralManager.generateCode).
        assertEquals(
            "A1B2C3D4E5",
            Uri.parse("foodranker://invite/A1B2C3D4E5").parseFoodRankerInviteCode()
        )
    }
}
