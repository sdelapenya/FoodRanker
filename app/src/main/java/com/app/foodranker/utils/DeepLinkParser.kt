package com.app.foodranker.utils

import android.net.Uri

fun Uri.parseFoodRankerProfileUserId(): String? = when {
    scheme == "https" && host == "foodranker.app" ->
        pathSegments.takeIf { it.size >= 2 && it[0] == "user" }?.get(1)?.takeIf { it.isNotBlank() }
    scheme == "foodranker" && host == "user" && pathSegments.isNotEmpty() ->
        pathSegments[0].takeIf { it.isNotBlank() }
    else -> null
}

fun Uri.parseFoodRankerPlateId(): String? = when {
    scheme == "https" && host == "foodranker.app" ->
        pathSegments.takeIf { it.size >= 2 && it[0] == "plate" }?.get(1)?.takeIf { it.isNotBlank() }
    scheme == "foodranker" && host == "plate" && pathSegments.isNotEmpty() ->
        pathSegments[0].takeIf { it.isNotBlank() }
    else -> null
}

fun Uri.parseFoodRankerInviteCode(): String? = when {
    scheme == "https" && host == "foodranker.app" ->
        pathSegments.takeIf { it.size >= 2 && it[0] == "invite" }?.get(1)?.takeIf { it.isNotBlank() }
    else -> null
}
