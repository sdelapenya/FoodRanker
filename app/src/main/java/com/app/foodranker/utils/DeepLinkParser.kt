package com.app.foodranker.utils

import android.net.Uri

// Los enlaces https se aceptan de cualquier dominio de DeepLinks.ACCEPTED_HOSTS, no solo
// del actual: un enlace compartido sigue vivo mucho despues de que cambie el dominio.
private fun Uri.isFoodRankerWeb(): Boolean =
    scheme == "https" && host in DeepLinks.ACCEPTED_HOSTS

/**
 * Un id que llega de un enlace solo se acepta si puede vivir tal cual en UN segmento de
 * ruta de Navigation. `Screen.PlateDetail.createRoute` interpola sin codificar
 * (`"plate/$plateId"`), así que un id con caracteres raros produce una ruta que no casa con
 * `plate/{plateId}` y `navigate()` lanza IllegalArgumentException, llevándose la app.
 *
 * Parece que `Uri.pathSegments` ya protege de eso porque parte por `/`, pero **no**:
 * devuelve los segmentos **decodificados**, así que `…/plate/a%2Fb` da el segmento `a/b`.
 * Lo mismo con espacios (`%20`), `?`, `#` o saltos de línea. Y los App Links están
 * verificados, de modo que cualquiera puede repartir un enlace así y cerrarle la app a
 * quien lo toque. Es la misma familia del fallo de los avisos con `plateId` vacío
 * (2026-11-16, ver docs/HANDOFF.md), por el otro camino de entrada.
 *
 * Es lista blanca y no lista negra a propósito: enumerar lo que rompe una ruta es una
 * carrera que se pierde. Los tres identificadores que viajan en enlaces caben de sobra:
 *   - plato:      `{placeId}__{dishSlug}`, y ambos son `[A-Za-z0-9_-]` (ver DishSlug.kt)
 *   - perfil:     uid de Firebase, alfanumérico
 *   - invitación: `UUID` sin guiones en mayúsculas, `[A-F0-9]{10}` (ReferralManager)
 *
 * Si alguna vez un id legítimo no pasa el filtro, el enlace abre la app por su pantalla
 * normal en vez de ir al contenido. Es un fallo molesto, no un cierre: justo el modo de
 * fallo que queremos.
 */
private val ID_ACEPTABLE = Regex("^[A-Za-z0-9_-]{1,256}$")

private fun String?.comoIdDeEnlace(): String? = this?.takeIf { ID_ACEPTABLE.matches(it) }

private fun Uri.webPathId(prefix: String): String? =
    pathSegments.takeIf { it.size >= 2 && it[0] == prefix }?.get(1).comoIdDeEnlace()

fun Uri.parseFoodRankerProfileUserId(): String? = when {
    isFoodRankerWeb() -> webPathId("user")
    scheme == "foodranker" && host == "user" && pathSegments.isNotEmpty() ->
        pathSegments[0].comoIdDeEnlace()
    else -> null
}

fun Uri.parseFoodRankerPlateId(): String? = when {
    isFoodRankerWeb() -> webPathId("plate")
    scheme == "foodranker" && host == "plate" && pathSegments.isNotEmpty() ->
        pathSegments[0].comoIdDeEnlace()
    else -> null
}

// El esquema propio tambien vale para las invitaciones. No lo aceptaba, y el boton "Ya
// tengo la app" de hosting/public/open.html manda precisamente a foodranker://invite/<code>
// cuando los App Links no estan verificados todavia: con plate y user abria la app, y con
// una invitacion se quedaba muerto. Aceptarlo no abre ningun agujero — un enlace https de
// invitacion lo puede fabricar cualquiera igual, y applyReferralCode ya rechaza
// autoinvitarse y aplicar un codigo dos veces.
fun Uri.parseFoodRankerInviteCode(): String? = when {
    isFoodRankerWeb() -> webPathId("invite")
    scheme == "foodranker" && host == "invite" && pathSegments.isNotEmpty() ->
        pathSegments[0].comoIdDeEnlace()
    else -> null
}
