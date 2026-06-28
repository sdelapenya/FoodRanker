package com.app.foodranker.utils

import com.app.foodranker.utils.InputLimits.sanitized
import org.junit.Assert.assertEquals
import org.junit.Test

class InputLimitsTest {

    @Test
    fun `sanitized trims surrounding whitespace`() {
        assertEquals("Pizza", "  Pizza  ".sanitized(InputLimits.PLATE_NAME))
    }

    @Test
    fun `sanitized truncates strings longer than the limit`() {
        val tooLong = "a".repeat(InputLimits.USER_NAME + 10)
        val result = tooLong.sanitized(InputLimits.USER_NAME)
        assertEquals(InputLimits.USER_NAME, result.length)
    }

    @Test
    fun `sanitized leaves short strings untouched`() {
        assertEquals("Sushi", "Sushi".sanitized(InputLimits.PLATE_NAME))
    }

    @Test
    fun `sanitized trims before truncating`() {
        val padded = "  " + "b".repeat(InputLimits.BIO) + "  "
        val result = padded.sanitized(InputLimits.BIO)
        assertEquals(InputLimits.BIO, result.length)
        assertEquals("b".repeat(InputLimits.BIO), result)
    }

    @Test
    fun `sanitized of blank string returns empty string`() {
        assertEquals("", "   ".sanitized(InputLimits.COMMENT_TEXT))
    }
}
