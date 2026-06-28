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

    @Test
    fun `custom scheme does not match invite parser`() {
        val uri = Uri.parse("foodranker://invite/REF42")
        assertNull(uri.parseFoodRankerInviteCode())
    }

    @Test
    fun `unrelated host returns null for all parsers`() {
        val uri = Uri.parse("https://example.com/user/abc123")
        assertNull(uri.parseFoodRankerProfileUserId())
        assertNull(uri.parseFoodRankerPlateId())
        assertNull(uri.parseFoodRankerInviteCode())
    }
}
