package com.bustedelbow.studora.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bustedelbow.studora.domain.Clock
import com.bustedelbow.studora.domain.FocusSession
import com.bustedelbow.studora.domain.InProgress
import com.bustedelbow.studora.domain.SessionRecord
import com.bustedelbow.studora.domain.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the single [FocusSession] and projects it into [TimerUiState].
 *
 * The one-second [TICK_INTERVAL_MILLIS] ticker runs in [viewModelScope], so it survives activity
 * recreation (rotation) and keeps counting while the app is backgrounded for as long as the
 * ViewModel is alive. Process-death recovery is provided by [SessionRepository]: a session that
 * starts is persisted, and on construction an unfinished snapshot is restored (ADR-0002).
 *
 * The production [Clock] and [SessionRepository] arrive via Hilt (see `StudoraApp` / the data
 * layer); tests construct this class directly with deterministic fakes.
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val clock: Clock,
    private val repository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TimerUiState>(TimerUiState.Idle())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    /** `true` when the last custom-duration input was non-empty and outside 5..180. */
    private val _customDurationError = MutableStateFlow(false)
    val customDurationError: StateFlow<Boolean> = _customDurationError.asStateFlow()

    private var session: FocusSession? = null
    private var ticker: Job? = null

    /** Wall-clock start of the active session, persisted for kill-recovery. */
    private var sessionStartMillis: Long? = null

    /** Last time an in-progress snapshot was written, for [SAVE_INTERVAL_MILLIS] throttling. */
    private var lastPersistedMillis: Long = 0L

    init {
        viewModelScope.launch {
            repository.clears.collect { reset() }
        }
        viewModelScope.launch {
            val persisted = repository.inProgress().firstOrNull() ?: return@launch
            if (session == null) {
                restoreInProgress(persisted)
            }
        }
    }

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
        val startMillis = clock.nowMillis()
        val started = FocusSession(clock = clock, durationMinutes = idle.durationMinutes).apply {
            start()
        }
        session = started
        sessionStartMillis = startMillis
        lastPersistedMillis = startMillis
        _uiState.value = TimerUiState.Running(
            remainingSeconds = started.remainingSeconds(),
            durationMinutes = idle.durationMinutes,
        )
        viewModelScope.launch { repository.saveInProgress(startMillis, idle.durationMinutes) }
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
        sessionStartMillis = null
        _uiState.value = TimerUiState.Discarded(durationMinutes = current.durationMinutes)
        viewModelScope.launch { repository.clearInProgress() }
    }

    /**
     * Returns to a fresh idle session after completion or discard, keeping the configured
     * duration so the next session can be started without re-picking a length.
     */
    fun reset() {
        val durationMinutes = _uiState.value.durationMinutes
        stopTicker()
        session = null
        sessionStartMillis = null
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
                        persistInProgressIfDue()
                    }

                    FocusSession.State.COMPLETED -> {
                        _uiState.value = TimerUiState.Completed(durationMinutes = duration)
                        completeSession()
                        break
                    }

                    else -> break
                }
            }
        }
    }

    /**
     * Rebuilds a running session from a persisted snapshot. Elapsed wall-clock time since the
     * stored start is subtracted from the configured length; if that already reached zero, the
     * session is recorded as completed instead of resumed.
     */
    private fun restoreInProgress(persisted: InProgress) {
        val totalSeconds = persisted.durationMinutes.toLong() * 60L
        val elapsedMillis = (clock.nowMillis() - persisted.startMillis).coerceAtLeast(0L)
        val remaining = totalSeconds - elapsedMillis / 1000L
        if (remaining <= 0L) {
            viewModelScope.launch {
                repository.recordCompleted(
                    SessionRecord(
                        startMillis = persisted.startMillis,
                        endMillis = persisted.startMillis + totalSeconds * 1000L,
                    ),
                )
                repository.clearInProgress()
            }
            return
        }
        val restoringClock = RestoringClock(clock, pendingMillis = persisted.startMillis)
        val active =
            FocusSession(clock = restoringClock, durationMinutes = persisted.durationMinutes).apply {
                start()
                // Reconcile the elapsed-before-restore seconds in one step.
                tick()
            }
        session = active
        sessionStartMillis = persisted.startMillis
        lastPersistedMillis = clock.nowMillis()
        _uiState.value = TimerUiState.Running(
            remainingSeconds = active.remainingSeconds(),
            durationMinutes = persisted.durationMinutes,
        )
        launchTicker()
    }

    /** Writes the in-progress snapshot, throttled to [SAVE_INTERVAL_MILLIS] between writes. */
    private suspend fun persistInProgressIfDue() {
        val startMillis = sessionStartMillis ?: return
        val now = clock.nowMillis()
        if (now - lastPersistedMillis < SAVE_INTERVAL_MILLIS) return
        lastPersistedMillis = now
        repository.saveInProgress(startMillis, _uiState.value.durationMinutes)
    }

    /** Records the just-finished session and drops the now-stale in-progress snapshot. */
    private suspend fun completeSession() {
        val startMillis = sessionStartMillis ?: return
        val endMillis = clock.nowMillis().coerceAtLeast(startMillis)
        repository.recordCompleted(SessionRecord(startMillis = startMillis, endMillis = endMillis))
        repository.clearInProgress()
        sessionStartMillis = null
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    companion object {
        /** Cadence of the countdown ticker, in milliseconds. */
        const val TICK_INTERVAL_MILLIS: Long = 1_000L

        /** Minimum spacing between in-progress snapshot writes, in milliseconds. */
        const val SAVE_INTERVAL_MILLIS: Long = 5_000L
    }
}

/**
 * [Clock] that returns one caller-supplied instant before falling back to [delegate].
 *
 * Used only while restoring: [FocusSession.start] reads the clock once, and feeding it the
 * persisted start makes the reconstructed session resume from the right wall-clock origin.
 */
private class RestoringClock(
    private val delegate: Clock,
    private var pendingMillis: Long?,
) : Clock {
    override fun nowMillis(): Long {
        val pending = pendingMillis
        if (pending != null) {
            pendingMillis = null
            return pending
        }
        return delegate.nowMillis()
    }
}
