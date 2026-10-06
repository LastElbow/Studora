package com.bustedelbow.studora.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A finished focus session, expressed as an epoch-millisecond interval.
 *
 * Only completed sessions ever reach this seam: paused and discarded sessions live in
 * [FocusSession] and are never handed here.
 *
 * @param startMillis start instant, milliseconds since the Unix epoch.
 * @param endMillis end instant, milliseconds since the Unix epoch; must not precede
 *   [startMillis].
 */
data class SessionRecord(
    val startMillis: Long,
    val endMillis: Long,
)

/** How darkly a calendar day is shaded on the heatmap. */
enum class ShadeLevel {
    NONE,
    LIGHT,
    MEDIUM,
    DARK,
    INTENSE,
}

/**
 * Counts completed sessions by the calendar day they *started* on, in [zone].
 *
 * A session contributes exactly one unit, however long it runs; a session crossing midnight is
 * attributed to its start day. The zone is supplied by the caller so the domain never depends on
 * the device's default zone.
 *
 * @throws IllegalArgumentException if any session ends before it starts.
 */
fun countByDay(sessions: List<SessionRecord>, zone: ZoneId): Map<LocalDate, Int> {
    val counts = LinkedHashMap<LocalDate, Int>()
    for (session in sessions) {
        require(session.endMillis >= session.startMillis) {
            "session end ${session.endMillis} precedes start ${session.startMillis}"
        }
        val day = Instant.ofEpochMilli(session.startMillis).atZone(zone).toLocalDate()
        counts[day] = (counts[day] ?: 0) + 1
    }
    return counts
}

/**
 * Maps a completed-session count for a single day to its [ShadeLevel].
 *
 * 0 -> [ShadeLevel.NONE], 1 -> [ShadeLevel.LIGHT], 2-3 -> [ShadeLevel.MEDIUM],
 * 4-5 -> [ShadeLevel.DARK], 6+ -> [ShadeLevel.INTENSE].
 *
 * @throws IllegalArgumentException if [count] is negative.
 */
fun shadeForCount(count: Int): ShadeLevel {
    require(count >= 0) { "count must not be negative, was $count" }
    return when {
        count == 0 -> ShadeLevel.NONE
        count == 1 -> ShadeLevel.LIGHT
        count <= 3 -> ShadeLevel.MEDIUM
        count <= 5 -> ShadeLevel.DARK
        else -> ShadeLevel.INTENSE
    }
}

/**
 * Per-day [ShadeLevel] for the given completed [sessions], bucketed in [zone].
 *
 * @throws IllegalArgumentException if any session ends before it starts.
 */
fun shadeByDay(sessions: List<SessionRecord>, zone: ZoneId): Map<LocalDate, ShadeLevel> =
    countByDay(sessions, zone).mapValues { (_, count) -> shadeForCount(count) }
