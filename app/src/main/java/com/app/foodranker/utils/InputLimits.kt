package com.app.foodranker.utils

object InputLimits {
    const val USER_NAME = 50
    const val PLATE_NAME = 80
    const val PLATE_DESCRIPTION = 280
    const val RESTAURANT_NAME = 80
    const val CITY = 60
    const val COUNTRY = 60
    const val RATING_COMMENT = 500
    const val COMMENT_TEXT = 500
    const val BIO = 280
    const val WEBSITE = 200
    const val REPORT_REASON = 200

    fun String.sanitized(max: Int): String = trim().take(max)
}
