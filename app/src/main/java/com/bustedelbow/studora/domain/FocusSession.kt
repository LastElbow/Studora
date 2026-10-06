package com.bustedelbow.studora.domain

/**
 * Source of wall-clock time for [FocusSession].
 *
 * The domain never calls `Instant.now()` / `System.currentTimeMillis()` directly; production
 * code supplies a real clock and tests supply a fake. Kept deliberately tiny so the state
 * machine stays pure and deterministic under test.
 */
interface Clock {
    /** Current time in milliseconds since the Unix epoch. */
    fun nowMillis(): Long
}

/**
 * Pure focus-session state machine.
 *
 * Lifecycle:
 *
 * ```
 * IDLE -> RUNNING <-> PAUSED -> COMPLETED | DISCARDED
 * ```
 *
 * A session is only [State.COMPLETED] when the countdown actually reaches zero; there is no
 * finish-early path. [discard] abandons a running or paused session and leaves no history.
 * Paused wall-clock time is never counted as focus time, and the remaining countdown is frozen
 * while paused.
 *
 * @param clock the time source; injected so tests are deterministic.
 * @param durationMinutes whole-minute session length, within
 *   [MIN_DURATION_MINUTES]..[MAX_DURATION_MINUTES]. Defaults to [DEFAULT_DURATION_MINUTES].
 * @throws IllegalArgumentException if [durationMinutes] is outside the allowed bounds.
 */
class FocusSession(
    private val clock: Clock,
    durationMinutes: Int = DEFAULT_DURATION_MINUTES,
) {

    /** The states a [FocusSession] can be in. */
    enum class State {
        IDLE,
        RUNNING,
        PAUSED,
        COMPLETED,
        DISCARDED,
    }

    companion object {
        /** Session length used when the caller does not choose one. */
        const val DEFAULT_DURATION_MINUTES: Int = 25

        /** Smallest allowed custom session length, in minutes. */
        const val MIN_DURATION_MINUTES: Int = 5

        /** Largest allowed custom session length, in minutes. */
        const val MAX_DURATION_MINUTES: Int = 180
    }

    private val totalSeconds: Long

    /** Current lifecycle state. Read-only to callers; changed only through the transitions. */
    var state: State = State.IDLE
        private set

    private var remaining: Long = 0L
    private var elapsedFocus: Long = 0L
    private var runningSinceMillis: Long = 0L

    init {
        require(durationMinutes in MIN_DURATION_MINUTES..MAX_DURATION_MINUTES) {
            "durationMinutes must be between $MIN_DURATION_MINUTES and " +
                "$MAX_DURATION_MINUTES inclusive, was $durationMinutes"
        }
        totalSeconds = durationMinutes.toLong() * 60L
    }

    /**
     * Starts the session and the countdown.
     *
     * @throws IllegalStateException if the session has already been started.
     */
    fun start(): Unit {
        check(state == State.IDLE) { "start() is only valid from IDLE, was $state" }
        remaining = totalSeconds
        elapsedFocus = 0L
        runningSinceMillis = clock.nowMillis()
        state = State.RUNNING
    }

    /**
     * Freezes the countdown. Allowed from [State.RUNNING] only and any number of times.
     *
     * @throws IllegalStateException if the session is not running.
     */
    fun pause(): Unit {
        check(state == State.RUNNING) { "pause() is only valid from RUNNING, was $state" }
        accumulateRunningTime()
        state = State.PAUSED
    }

    /**
     * Resumes a paused session; the remaining countdown continues from where it froze. Allowed
     * from [State.PAUSED] only.
     *
     * @throws IllegalStateException if the session is not paused.
     */
    fun resume(): Unit {
        check(state == State.PAUSED) { "resume() is only valid from PAUSED, was $state" }
        runningSinceMillis = clock.nowMillis()
        state = State.RUNNING
    }

    /**
     * Recomputes the countdown from the clock. A no-op unless the session is running; moves to
     * [State.COMPLETED] when the remaining time reaches zero. Safe to call repeatedly.
     */
    fun tick(): Unit {
        if (state != State.RUNNING) {
            return
        }
        accumulateRunningTime()
        if (remaining == 0L) {
            state = State.COMPLETED
        }
    }

    /**
     * Abandons the session. Allowed from [State.RUNNING] or [State.PAUSED]; the session never
     * completes afterwards. This slice holds state only, so nothing is written to history.
     *
     * @throws IllegalStateException if the session is not running or paused.
     */
    fun discard(): Unit {
        check(state == State.RUNNING || state == State.PAUSED) {
            "discard() is only valid from RUNNING or PAUSED, was $state"
        }
        state = State.DISCARDED
    }

    /** Seconds left on the countdown, frozen while paused. `0` once completed. */
    fun remainingSeconds(): Long = remaining

    /** Focus seconds actually spent running; paused wall-clock time is excluded. */
    fun elapsedFocusSeconds(): Long = elapsedFocus

    private fun accumulateRunningTime(): Unit {
        val now = clock.nowMillis()
        val elapsedMillis = now - runningSinceMillis
        if (elapsedMillis <= 0L) {
            return
        }
        val elapsedSeconds = elapsedMillis / 1000L
        if (elapsedSeconds == 0L) {
            return
        }
        elapsedFocus += elapsedSeconds
        remaining = maxOf(0L, remaining - elapsedSeconds)
        // Consume only the whole seconds accounted for, so leftover millis carry over.
        runningSinceMillis += elapsedSeconds * 1000L
    }
}
