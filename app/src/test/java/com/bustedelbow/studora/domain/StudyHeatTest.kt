package com.bustedelbow.studora.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour tests for the day-bucketing + shade seam ([StudyHeat]).
 *
 * These tests describe completed sessions only: the paused / discarded states live in
 * [FocusSession] and never reach this seam. Time is supplied entirely by [SessionRecord] plus an
 * explicit [ZoneId]; the tests never touch the wall clock or [ZoneId.systemDefault].
 */
class StudyHeatTest {

    private val manila: ZoneId = ZoneId.of("Asia/Manila")
    private val honolulu: ZoneId = ZoneId.of("Pacific/Honolulu")

    /** Builds an instant in [zone] expressed as epoch millis, e.g. local time 23:50. */
    private fun at(
        zone: ZoneId,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = LocalDateTime.of(year, month, day, hour, minute)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    /** A session that starts at [startMillis] and runs for [durationMinutes]. */
    private fun session(startMillis: Long, durationMinutes: Long): SessionRecord =
        SessionRecord(startMillis, startMillis + durationMinutes * 60_000L)

    private fun assertShade(count: Int, expected: ShadeLevel) {
        assertEquals("shade for count=$count", expected, shadeForCount(count))
    }

    @Test
    fun `empty list yields empty maps`() {
        val sessions = emptyList<SessionRecord>()

        assertTrue(countByDay(sessions, manila).isEmpty())
        assertTrue(shadeByDay(sessions, manila).isEmpty())
    }

    @Test
    fun `single session counts once on its start day with LIGHT shade`() {
        val start = at(manila, 2026, 1, 15, 10, 0)
        val sessions = listOf(session(start, durationMinutes = 25))
        val day = LocalDate.of(2026, 1, 15)

        assertEquals(mapOf(day to 1), countByDay(sessions, manila))
        assertEquals(mapOf(day to ShadeLevel.LIGHT), shadeByDay(sessions, manila))
    }

    @Test
    fun `multiple sessions same day aggregate counts`() {
        val start = at(manila, 2026, 1, 15, 10, 0)
        val sessions = listOf(
            session(start, durationMinutes = 25),
            session(start + 30 * 60_000L, durationMinutes = 25),
            session(start + 60 * 60_000L, durationMinutes = 25),
        )
        val day = LocalDate.of(2026, 1, 15)

        assertEquals(mapOf(day to 3), countByDay(sessions, manila))
        assertEquals(mapOf(day to ShadeLevel.MEDIUM), shadeByDay(sessions, manila))
    }

    @Test
    fun `shade boundaries map 0 1 2 3 4 5 6 and 10 correctly`() {
        assertShade(0, ShadeLevel.NONE)
        assertShade(1, ShadeLevel.LIGHT)
        assertShade(2, ShadeLevel.MEDIUM)
        assertShade(3, ShadeLevel.MEDIUM)
        assertShade(4, ShadeLevel.DARK)
        assertShade(5, ShadeLevel.DARK)
        assertShade(6, ShadeLevel.INTENSE)
        assertShade(10, ShadeLevel.INTENSE)
    }

    @Test
    fun `midnight straddling session counts toward start day only`() {
        val start = at(manila, 2026, 1, 15, 23, 50)
        val end = at(manila, 2026, 1, 16, 0, 15)
        val sessions = listOf(SessionRecord(start, end))

        assertEquals(mapOf(LocalDate.of(2026, 1, 15) to 1), countByDay(sessions, manila))
        assertEquals(
            mapOf(LocalDate.of(2026, 1, 15) to ShadeLevel.LIGHT),
            shadeByDay(sessions, manila),
        )
    }

    @Test
    fun `long session crossing midnight still counts once on start day`() {
        // A three-hour session from 22:30 on day A to 01:30 on day B is one unit on day A.
        val start = at(manila, 2026, 1, 15, 22, 30)
        val end = at(manila, 2026, 1, 16, 1, 30)
        val sessions = listOf(SessionRecord(start, end))

        assertEquals(mapOf(LocalDate.of(2026, 1, 15) to 1), countByDay(sessions, manila))
    }

    @Test
    fun `sessions spread across days bucket independently`() {
        val dayOne = at(manila, 2026, 1, 15, 9, 0)
        val dayTwo = at(manila, 2026, 1, 16, 9, 0)
        val dayThree = at(manila, 2026, 1, 17, 9, 0)
        val sessions = listOf(
            session(dayOne, 25),
            session(dayOne + 60 * 60_000L, 25),
            session(dayTwo, 25),
            session(dayThree, 25),
            session(dayThree + 60 * 60_000L, 25),
            session(dayThree + 120 * 60_000L, 25),
        )

        assertEquals(
            mapOf(
                LocalDate.of(2026, 1, 15) to 2,
                LocalDate.of(2026, 1, 16) to 1,
                LocalDate.of(2026, 1, 17) to 3,
            ),
            countByDay(sessions, manila),
        )
        assertEquals(
            mapOf(
                LocalDate.of(2026, 1, 15) to ShadeLevel.MEDIUM,
                LocalDate.of(2026, 1, 16) to ShadeLevel.LIGHT,
                LocalDate.of(2026, 1, 17) to ShadeLevel.MEDIUM,
            ),
            shadeByDay(sessions, manila),
        )
    }

    @Test
    fun `end before start is rejected`() {
        val inconsistent = listOf(SessionRecord(startMillis = 2_000L, endMillis = 1_000L))

        assertThrows(IllegalArgumentException::class.java) {
            countByDay(inconsistent, manila)
        }
        assertThrows(IllegalArgumentException::class.java) {
            shadeByDay(inconsistent, manila)
        }
    }

    @Test
    fun `zone determines the bucket day`() {
        // 2026-01-15T18:00Z is 2026-01-16 02:00 in Manila (UTC+8) but
        // 2026-01-15 08:00 in Honolulu (UTC-10). Same instant, different calendar day.
        val instant = Instant.parse("2026-01-15T18:00:00Z")
        val sessions = listOf(
            SessionRecord(
                startMillis = instant.toEpochMilli(),
                endMillis = instant.plusSeconds(25 * 60).toEpochMilli(),
            ),
        )

        assertEquals(mapOf(LocalDate.of(2026, 1, 16) to 1), countByDay(sessions, manila))
        assertEquals(mapOf(LocalDate.of(2026, 1, 15) to 1), countByDay(sessions, honolulu))
    }

    @Test
    fun `paused or discarded sessions never reach this seam`() {
        // Only completed sessions are ever passed in. There is deliberately no paused or
        // discarded state to model here; this test documents that contract by feeding a list
        // built purely from completed sessions.
        val start = at(manila, 2026, 1, 15, 11, 0)
        val completed = listOf(session(start, durationMinutes = 25))

        assertEquals(mapOf(LocalDate.of(2026, 1, 15) to 1), countByDay(completed, manila))
        assertEquals(1, countByDay(completed, manila).size)
    }
}
