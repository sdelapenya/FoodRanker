package com.app.foodranker.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Fija a qué canal va cada tipo de aviso.
 *
 * No es un test de adorno: **este reparto tiene que coincidir con el del servidor**
 * (`onNotificationCreated` en `functions/src/index.ts`, que pone `channelId` en cada `case`).
 * El mismo aviso lo pinta el servidor cuando la app está cerrada y el cliente cuando está
 * abierta, así que si los dos lados dejan de coincidir el usuario ve el mismo hecho en un
 * canal distinto según por dónde entró — y eso no lo reproduce nadie a mano.
 *
 * Hasta el 2026-11-17 todo esto caía en un único canal llamado "Likes y valoraciones", de
 * modo que silenciar los likes te dejaba también sin saber que alguien a quien sigues había
 * publicado. Si algún día alguien vuelve a meter un tipo nuevo en el `else`, que sea
 * queriendo y con este test delante.
 */
class NotificationChannelsTest {

    @Test
    fun `los platos de gente que sigues van a su propio canal`() {
        assertEquals(NotificationHelper.CHANNEL_FOLLOWING, NotificationHelper.canalDe("new_plate"))
    }

    @Test
    fun `los logros y la liga van juntos y aparte`() {
        assertEquals(NotificationHelper.CHANNEL_ACHIEVEMENTS, NotificationHelper.canalDe("level_up"))
        assertEquals(NotificationHelper.CHANNEL_ACHIEVEMENTS, NotificationHelper.canalDe("badge"))
        assertEquals(NotificationHelper.CHANNEL_ACHIEVEMENTS, NotificationHelper.canalDe("league_result"))
    }

    @Test
    fun `la moderacion sigue en el suyo, que es el de maxima importancia`() {
        assertEquals(NotificationHelper.CHANNEL_MODERATION, NotificationHelper.canalDe("moderation_approved"))
        assertEquals(NotificationHelper.CHANNEL_MODERATION, NotificationHelper.canalDe("moderation_rejected"))
    }

    @Test
    fun `las interacciones se quedan en el canal de siempre`() {
        // Y el id tiene que seguir siendo el viejo: es lo que conserva el ajuste de quien
        // ya lo tuviera silenciado (ver el comentario de CHANNEL_SOCIAL).
        assertEquals("foodranker_social", NotificationHelper.CHANNEL_SOCIAL)
        for (tipo in listOf("like", "rating", "comment", "follow")) {
            assertEquals(NotificationHelper.CHANNEL_SOCIAL, NotificationHelper.canalDe(tipo))
        }
    }

    @Test
    fun `un tipo desconocido o nulo no revienta y cae en interacciones`() {
        assertEquals(NotificationHelper.CHANNEL_SOCIAL, NotificationHelper.canalDe(null))
        assertEquals(NotificationHelper.CHANNEL_SOCIAL, NotificationHelper.canalDe("algo_que_no_existe"))
    }
}
