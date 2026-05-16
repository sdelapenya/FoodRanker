package com.app.foodranker.utils

import android.net.Uri

/**
 * Enlaces públicos tipo https://foodranker.app/user/{userId}
 * y prueba local foodranker://user/{userId}
 */
fun Uri.parseFoodRankerProfileUserId(): String? = when {
    scheme == "https" && host == "foodranker.app" ->
        pathSegments.takeIf { it.size >= 2 && it[0] == "user" }?.get(1)?.takeIf { it.isNotBlank() }
    scheme == "foodranker" && host == "user" && pathSegments.isNotEmpty() ->
        pathSegments[0].takeIf { it.isNotBlank() }
    else -> null
}
