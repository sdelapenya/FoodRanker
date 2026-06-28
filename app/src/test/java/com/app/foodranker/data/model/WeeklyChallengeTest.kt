package com.app.foodranker.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyChallengeTest {

    private val now = System.currentTimeMillis()
    private val day = 24L * 3600 * 1000

    @Test
    fun `isActive is true when now is within range`() {
        val challenge = WeeklyChallenge(startDate = now - day, endDate = now + day)
        assertTrue(challenge.isActive)
    }

    @Test
    fun `isActive is false when challenge has not started`() {
        val challenge = WeeklyChallenge(startDate = now + day, endDate = now + 2 * day)
        assertFalse(challenge.isActive)
    }

    @Test
    fun `isActive is false when challenge has ended`() {
        val challenge = WeeklyChallenge(startDate = now - 2 * day, endDate = now - day)
        assertFalse(challenge.isActive)
    }

    @Test
    fun `daysLeft rounds up partial days`() {
        // Quedan 1.5 dias -> debe redondear a 2, no truncar a 1
        val challenge = WeeklyChallenge(endDate = now + (day + day / 2))
        assertEquals(2, challenge.daysLeft)
    }

    @Test
    fun `daysLeft is exactly the number of full days when aligned`() {
        val challenge = WeeklyChallenge(endDate = now + 3 * day)
        assertEquals(3, challenge.daysLeft)
    }

    @Test
    fun `daysLeft is zero when the challenge already ended`() {
        val challenge = WeeklyChallenge(endDate = now - day)
        assertEquals(0, challenge.daysLeft)
    }
}
