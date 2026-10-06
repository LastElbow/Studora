package com.bustedelbow.studora.ui.timer

import com.bustedelbow.studora.domain.FocusSession

/**
 * Immutable UI state for the timer screen, mirroring the domain [FocusSession] lifecycle.
 *
 * [remainingSeconds] is the frozen countdown value; the UI derives both the formatted digits and
 * the progress fraction from it. [durationMinutes] is the configured session length, kept so the
 * progress bar can compute elapsed/total in every state (including the terminal ones).
 */
sealed interface TimerUiState {

    /** Seconds left on the countdown. `0` for [Completed] and [Discarded]. */
    val remainingSeconds: Long

    /** Configured session length, in minutes. */
    val durationMinutes: Int

    /** Before a session starts; the duration can still be changed. */
    data class Idle(
        override val durationMinutes: Int = FocusSession.DEFAULT_DURATION_MINUTES,
        override val remainingSeconds: Long = durationMinutes.toLong() * 60L,
    ) : TimerUiState

    /** Countdown in progress. */
    data class Running(
        override val remainingSeconds: Long,
        override val durationMinutes: Int,
    ) : TimerUiState

    /** Countdown frozen; can be resumed or discarded. */
    data class Paused(
        override val remainingSeconds: Long,
        override val durationMinutes: Int,
    ) : TimerUiState

    /** The countdown reached zero. */
    data class Completed(
        override val durationMinutes: Int,
        override val remainingSeconds: Long = 0L,
    ) : TimerUiState

    /** The session was abandoned before completing. */
    data class Discarded(
        override val durationMinutes: Int,
        override val remainingSeconds: Long = 0L,
    ) : TimerUiState
}
