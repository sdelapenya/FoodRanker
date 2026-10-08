package com.app.foodranker.utils

import com.app.foodranker.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorMapperTest {

    @Test
    fun `maps unknown host exception to network error message`() {
        val e = Exception("java.net.UnknownHostException: Unable to resolve host")
        assertEquals(
            R.string.err_no_internet,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps PERMISSION_DENIED to permission message`() {
        val e = Exception("PERMISSION_DENIED: Missing or insufficient permissions")
        assertEquals(
            R.string.err_permission,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps UNAVAILABLE to service unavailable message`() {
        val e = Exception("UNAVAILABLE: The service is currently unavailable")
        assertEquals(
            R.string.err_unavailable,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps DEADLINE_EXCEEDED to timeout message`() {
        val e = Exception("DEADLINE_EXCEEDED: deadline exceeded")
        assertEquals(
            R.string.err_timeout,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps NOT_FOUND to content no longer exists message`() {
        val e = Exception("NOT_FOUND: document does not exist")
        assertEquals(
            R.string.err_not_found,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps Cloudinary upload error to storage message`() {
        val e = Exception("Cloudinary upload failed")
        assertEquals(
            R.string.err_upload,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps CANCELLED to cancelled message`() {
        val e = Exception("CANCELLED: the operation was cancelled")
        assertEquals(
            R.string.err_cancelled,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps unrecognized exception to generic message`() {
        val e = Exception("something totally unexpected")
        assertEquals(
            R.string.err_generic2,
            ErrorMapper.toUserMessageRes(e)
        )
    }

    @Test
    fun `maps exception without message to generic message`() {
        val e = Exception()
        assertEquals(
            R.string.err_generic2,
            ErrorMapper.toUserMessageRes(e)
        )
    }
}
