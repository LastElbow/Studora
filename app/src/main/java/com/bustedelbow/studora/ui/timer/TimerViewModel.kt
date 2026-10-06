package com.bustedelbow.studora.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bustedelbow.studora.domain.Clock
import com.bustedelbow.studora.domain.FocusSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the single [FocusSession] and projects it into [TimerUiState].
 *
 * The one-second [TICK_INTERVAL_MILLIS] ticker runs in [viewModelScope], so it survives activity
 * recreation (rotation) and keeps counting while the app is backgrounded for as long as the
 * ViewModel is alive. Process-death recovery is deliberately out of scope for this slice.
 *
 * The production [Clock] arrives via Hilt (see `StudoraApp`); tests construct this class directly
 * with a deterministic fake.
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TimerUiState>(TimerUiState.Idle())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    /** `true` when the last custom-duration input was non-empty and outside 5..180. */
    private val _customDurationError = MutableStateFlow(false)
    val customDurationError: StateFlow<Boolean> = _customDurationError.asStateFlow()

    private var session: FocusSession? = null
    private var ticker: Job? = null

    /** Stores a preset only while idle. */
    fun selectPreset(minutes: Int) {
        if (_uiState.value !is TimerUiState.Idle) return
        _customDurationError.value = false
        _uiState.value = TimerUiState.Idle(durationMinutes = minutes)
    }

    /**
     * Validates free-form custom-duration input. Empty input clears the error without changing the
     * duration; a whole number inside [FocusSession.MIN_DURATION_MINUTES]..[FocusSession.MAX_DURATION_MINUTES]
     * updates it; anything else is rejected and flagged.
     */
    fun setCustomDuration(input: String) {
        if (_uiState.value !is TimerUiState.Idle) return
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            _customDurationError.value = false
            return
        }
        val minutes = trimmed.toIntOrNull()
        val valid = minutes != null &&
            minutes in FocusSession.MIN_DURATION_MINUTES..FocusSession.MAX_DURATION_MINUTES
        if (!valid) {
            _customDurationError.value = true
            return
        }
        _customDurationError.value = false
        _uiState.value = TimerUiState.Idle(durationMinutes = minutes)
    }

    /** Starts the configured session from [TimerUiState.Idle]; a no-op in any other state. */
    fun start() {
        val idle = _uiState.value as? TimerUiState.Idle ?: return
        if (_customDurationError.value) return
        val started = FocusSession(clock = clock, durationMinutes = idle.durationMinutes).apply {
            start()
        }
        session = started
        _uiState.value = TimerUiState.Running(
            remainingSeconds = started.remainingSeconds(),
            durationMinutes = idle.durationMinutes,
        )
        launchTicker()
    }

    /** Freezes a running countdown; a no-op in any other state. */
    fun pause() {
        val active = session ?: return
        val running = _uiState.value as? TimerUiState.Running ?: return
        active.pause()
        stopTicker()
        _uiState.value = TimerUiState.Paused(
            remainingSeconds = active.remainingSeconds(),
            durationMinutes = running.durationMinutes,
        )
    }

    /** Resumes a paused countdown; a no-op in any other state. */
    fun resume() {
        val active = session ?: return
        val paused = _uiState.value as? TimerUiState.Paused ?: return
        active.resume()
        _uiState.value = TimerUiState.Running(
            remainingSeconds = active.remainingSeconds(),
            durationMinutes = paused.durationMinutes,
        )
        launchTicker()
    }

    /** Abandons a running or paused session; a no-op in any other state. */
    fun discard() {
        val active = session ?: return
        val current = _uiState.value
        if (current !is TimerUiState.Running && current !is TimerUiState.Paused) return
        active.discard()
        stopTicker()
        _uiState.value = TimerUiState.Discarded(durationMinutes = current.durationMinutes)
    }

    /**
     * Returns to a fresh idle session after completion or discard, keeping the configured
     * duration so the next session can be started without re-picking a length.
     */
    fun reset() {
        val durationMinutes = _uiState.value.durationMinutes
        stopTicker()
        session = null
        _customDurationError.value = false
        _uiState.value = TimerUiState.Idle(durationMinutes = durationMinutes)
    }

    private fun launchTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MILLIS)
                val active = session ?: break
                active.tick()
                val duration = _uiState.value.durationMinutes
                when (active.state) {
                    FocusSession.State.RUNNING -> {
                        _uiState.value = TimerUiState.Running(
                            remainingSeconds = active.remainingSeconds(),
                            durationMinutes = duration,
                        )
                    }

                    FocusSession.State.COMPLETED -> {
                        _uiState.value = TimerUiState.Completed(durationMinutes = duration)
                        break
                    }

                    else -> break
                }
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    companion object {
        /** Cadence of the countdown ticker, in milliseconds. */
        const val TICK_INTERVAL_MILLIS: Long = 1_000L
    }
}
