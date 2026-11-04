package com.app.foodranker.utils

import android.net.Uri

// Los enlaces https se aceptan de cualquier dominio de DeepLinks.ACCEPTED_HOSTS, no solo
// del actual: un enlace compartido sigue vivo mucho despues de que cambie el dominio.
private fun Uri.isFoodRankerWeb(): Boolean =
    scheme == "https" && host in DeepLinks.ACCEPTED_HOSTS

private fun Uri.webPathId(prefix: String): String? =
    pathSegments.takeIf { it.size >= 2 && it[0] == prefix }?.get(1)?.takeIf { it.isNotBlank() }

fun Uri.parseFoodRankerProfileUserId(): String? = when {
    isFoodRankerWeb() -> webPathId("user")
    scheme == "foodranker" && host == "user" && pathSegments.isNotEmpty() ->
        pathSegments[0].takeIf { it.isNotBlank() }
    else -> null
}

fun Uri.parseFoodRankerPlateId(): String? = when {
    isFoodRankerWeb() -> webPathId("plate")
    scheme == "foodranker" && host == "plate" && pathSegments.isNotEmpty() ->
        pathSegments[0].takeIf { it.isNotBlank() }
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
        pathSegments[0].takeIf { it.isNotBlank() }
    else -> null
}
