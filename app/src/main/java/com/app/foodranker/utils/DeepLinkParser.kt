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

fun Uri.parseFoodRankerInviteCode(): String? = when {
    isFoodRankerWeb() -> webPathId("invite")
    else -> null
}
