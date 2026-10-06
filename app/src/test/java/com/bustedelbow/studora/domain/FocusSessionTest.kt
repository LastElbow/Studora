package com.bustedelbow.studora.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour tests for the [FocusSession] state machine.
 *
 * All time is driven through a [FakeClock] so the tests never touch the wall clock.
 */
class FocusSessionTest {

    private class FakeClock(var currentMillis: Long = 0L) : Clock {
        override fun nowMillis(): Long = currentMillis

        fun advanceSeconds(seconds: Long) {
            currentMillis += seconds * 1000L
        }
    }

    @Test
    fun `start with default gives 25 minutes remaining`() {
        val clock = FakeClock()
        val session = FocusSession(clock = clock)

        session.start()

        assertEquals(FocusSession.State.RUNNING, session.state)
        assertEquals(25L * 60L, session.remainingSeconds())
    }

    @Test
    fun `start rejects below 5 and above 180`() {
        val clock = FakeClock()

        assertThrows(IllegalArgumentException::class.java) {
            FocusSession(clock = clock, durationMinutes = 4)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FocusSession(clock = clock, durationMinutes = 181)
        }

        // The bounds themselves are accepted.
        val minimum = FocusSession(clock = clock, durationMinutes = 5)
        minimum.start()
        assertEquals(5L * 60L, minimum.remainingSeconds())

        val maximum = FocusSession(clock = clock, durationMinutes = 180)
        maximum.start()
        assertEquals(180L * 60L, maximum.remainingSeconds())
    }

    @Test
    fun `pause freezes remaining across clock advance, resume continues countdown`() {
        val clock = FakeClock()
        val session = FocusSession(clock = clock)
        session.start()

        clock.advanceSeconds(60)
        session.pause()
        assertEquals(FocusSession.State.PAUSED, session.state)
        assertEquals(25L * 60L - 60L, session.remainingSeconds())

        clock.advanceSeconds(120)
        assertEquals(25L * 60L - 60L, session.remainingSeconds())

        session.resume()
        assertEquals(FocusSession.State.RUNNING, session.state)
        clock.advanceSeconds(30)
        session.tick()
        assertEquals(25L * 60L - 90L, session.remainingSeconds())
    }

    @Test
    fun `unlimited pause resume cycles keep correct remaining`() {
        val clock = FakeClock()
        val session = FocusSession(clock = clock)
        session.start()

        clock.advanceSeconds(10)
        session.pause()
        clock.advanceSeconds(100)
        session.resume()
        clock.advanceSeconds(20)
        session.pause()
        clock.advanceSeconds(50)
        session.resume()
        clock.advanceSeconds(30)
        session.tick()

        assertEquals(25L * 60L - (10L + 20L + 30L), session.remainingSeconds())
    }

    @Test
    fun `session completes only when countdown reaches zero`() {
        val clock = FakeClock()
        val session = FocusSession(clock = clock, durationMinutes = 5)
        session.start()

        clock.advanceSeconds(5 * 60 - 1)
        session.tick()
        assertEquals(FocusSession.State.RUNNING, session.state)
        assertEquals(1L, session.remainingSeconds())

        clock.advanceSeconds(1)
        session.tick()
        assertEquals(FocusSession.State.COMPLETED, session.state)
        assertEquals(0L, session.remainingSeconds())
    }

    @Test
    fun `cannot complete early — remaining time still positive means not completed`() {
        val clock = FakeClock()
        val session = FocusSession(clock = clock, durationMinutes = 25)
        session.start()

        clock.advanceSeconds(25 * 60 - 1)
        session.tick()

        assertNotEquals(FocusSession.State.COMPLETED, session.state)
        assertTrue(session.remainingSeconds() > 0L)
    }

    @Test
    fun `discard from running leaves discarded state and never completes`() {
        val clock = FakeClock()
        val session = FocusSession(clock = clock, durationMinutes = 5)
        session.start()

        clock.advanceSeconds(5 * 60)
        session.discard()
        assertEquals(FocusSession.State.DISCARDED, session.state)

        clock.advanceSeconds(600)
        session.tick()
        assertEquals(FocusSession.State.DISCARDED, session.state)
    }

    @Test
    fun `discard from paused leaves discarded state and never completes`() {
        val clock = FakeClock()
        val session = FocusSession(clock = clock, durationMinutes = 5)
        session.start()

        clock.advanceSeconds(60)
        session.pause()
        clock.advanceSeconds(600)
        session.discard()
        assertEquals(FocusSession.State.DISCARDED, session.state)

        clock.advanceSeconds(600)
        session.tick()
        assertEquals(FocusSession.State.DISCARDED, session.state)
    }

    @Test
    fun `invalid transitions throw IllegalStateException`() {
        val clock = FakeClock()

        val idle = FocusSession(clock = clock)
        assertThrows(IllegalStateException::class.java) { idle.pause() }
        assertThrows(IllegalStateException::class.java) { idle.resume() }
        assertThrows(IllegalStateException::class.java) { idle.discard() }

        val running = FocusSession(clock = clock)
        running.start()
        assertThrows(IllegalStateException::class.java) { running.start() }
        assertThrows(IllegalStateException::class.java) { running.resume() }

        val paused = FocusSession(clock = clock)
        paused.start()
        paused.pause()
        assertThrows(IllegalStateException::class.java) { paused.pause() }
        assertThrows(IllegalStateException::class.java) { paused.start() }

        val completed = FocusSession(clock = clock, durationMinutes = 5)
        completed.start()
        clock.advanceSeconds(5 * 60)
        completed.tick()
        assertEquals(FocusSession.State.COMPLETED, completed.state)
        assertThrows(IllegalStateException::class.java) { completed.start() }
        assertThrows(IllegalStateException::class.java) { completed.pause() }
        assertThrows(IllegalStateException::class.java) { completed.resume() }
        assertThrows(IllegalStateException::class.java) { completed.discard() }

        val discarded = FocusSession(clock = clock)
        discarded.start()
        discarded.discard()
        assertThrows(IllegalStateException::class.java) { discarded.start() }
        assertThrows(IllegalStateException::class.java) { discarded.pause() }
        assertThrows(IllegalStateException::class.java) { discarded.resume() }
        assertThrows(IllegalStateException::class.java) { discarded.discard() }
    }

    @Test
    fun `paused duration does not count toward elapsed focus time`() {
        val clock = FakeClock()
        val session = FocusSession(clock = clock, durationMinutes = 25)
        session.start()

        clock.advanceSeconds(60)
        session.pause()
        clock.advanceSeconds(300)
        session.resume()
        clock.advanceSeconds(60)
        session.tick()

        assertEquals(120L, session.elapsedFocusSeconds())
        assertEquals(25L * 60L - 120L, session.remainingSeconds())
    }
}
