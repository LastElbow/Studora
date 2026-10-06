package com.bustedelbow.studora.ui.timer

import com.bustedelbow.studora.domain.Clock
import com.bustedelbow.studora.domain.InProgress
import com.bustedelbow.studora.domain.SessionRecord
import com.bustedelbow.studora.domain.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Behaviour tests for [TimerViewModel].
 *
 * The countdown is driven by an injected [Clock]; the view-model's one-second ticker runs on the
 * [StandardTestDispatcher] installed as `Dispatchers.Main`, so virtual time and the fake clock are
 * advanced together and the tests never touch the wall clock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private class FakeClock(var currentMillis: Long = 0L) : Clock {
        override fun nowMillis(): Long = currentMillis
    }

    private val dispatcher = StandardTestDispatcher()
    private val clock = FakeClock()
    private val repository = FakeSessionRepository()
    private lateinit var viewModel: TimerViewModel

    /** In-memory [SessionRepository] so the view-model never touches Room under test. */
    private class FakeSessionRepository : SessionRepository {
        val completed = mutableListOf<SessionRecord>()
        val inProgressFlow = MutableStateFlow<InProgress?>(null)
        private val clearsFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        override suspend fun recordCompleted(record: SessionRecord) {
            completed += record
        }

        override fun completedSessions(): Flow<List<SessionRecord>> = flowOf(completed.toList())

        override suspend fun saveInProgress(startMillis: Long, durationMinutes: Int) {
            inProgressFlow.value = InProgress(startMillis = startMillis, durationMinutes = durationMinutes)
        }

        override fun inProgress(): Flow<InProgress?> = inProgressFlow

        override suspend fun clearInProgress() {
            inProgressFlow.value = null
        }

        override suspend fun clearAll() {
            completed.clear()
            inProgressFlow.value = null
            clearsFlow.emit(Unit)
        }

        override val clears: Flow<Unit> = clearsFlow.asSharedFlow()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = TimerViewModel(clock, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Runs [body] and always cancels the ticker before the test scheduler is torn down. */
    private fun runTimerTest(body: suspend TestScope.() -> Unit) {
        runTest(dispatcher) {
            try {
                body()
            } finally {
                viewModel.reset()
                testScheduler.advanceUntilIdle()
            }
        }
    }

    /** Advances the fake clock and the ticker scheduler one whole second at a time, in lockstep. */
    private fun TestScope.tickSeconds(seconds: Int) {
        testScheduler.runCurrent()
        repeat(seconds) {
            clock.currentMillis += 1000L
            testScheduler.advanceTimeBy(1000L)
            testScheduler.runCurrent()
        }
    }

    @Test
    fun `initial state is idle with default 25 minutes`() = runTimerTest {
        val state = viewModel.uiState.value
        assertTrue(state is TimerUiState.Idle)
        assertEquals(25, state.durationMinutes)
        assertEquals(25L * 60L, state.remainingSeconds)
        assertFalse(viewModel.customDurationError.value)
    }

    @Test
    fun `start moves to running with the full countdown`() = runTimerTest {
        viewModel.start()

        val state = viewModel.uiState.value
        assertTrue(state is TimerUiState.Running)
        assertEquals(25, state.durationMinutes)
        assertEquals(25L * 60L, state.remainingSeconds)
    }

    @Test
    fun `preset selection updates the idle duration`() = runTimerTest {
        viewModel.selectPreset(45)

        val state = viewModel.uiState.value
        assertTrue(state is TimerUiState.Idle)
        assertEquals(45, state.durationMinutes)
        assertEquals(45L * 60L, state.remainingSeconds)
    }

    @Test
    fun `ticker advances remaining by elapsed seconds`() = runTimerTest {
        viewModel.start()
        tickSeconds(5)

        assertEquals(25L * 60L - 5L, viewModel.uiState.value.remainingSeconds)
    }

    @Test
    fun `pause freezes remaining and resume continues the countdown`() = runTimerTest {
        viewModel.start()
        tickSeconds(10)

        viewModel.pause()
        val paused = viewModel.uiState.value
        assertTrue(paused is TimerUiState.Paused)
        assertEquals(25L * 60L - 10L, paused.remainingSeconds)

        // Wall-clock time passes while paused but the countdown must not move.
        tickSeconds(30)
        assertEquals(25L * 60L - 10L, viewModel.uiState.value.remainingSeconds)

        viewModel.resume()
        tickSeconds(20)

        val running = viewModel.uiState.value
        assertTrue(running is TimerUiState.Running)
        assertEquals(25L * 60L - 30L, running.remainingSeconds)
    }

    @Test
    fun `discard from running moves to discarded with zero remaining`() = runTimerTest {
        viewModel.start()
        tickSeconds(3)

        viewModel.discard()

        val state = viewModel.uiState.value
        assertTrue(state is TimerUiState.Discarded)
        assertEquals(25, state.durationMinutes)
        assertEquals(0L, state.remainingSeconds)
    }

    @Test
    fun `discard from paused moves to discarded`() = runTimerTest {
        viewModel.start()
        viewModel.pause()

        viewModel.discard()

        assertTrue(viewModel.uiState.value is TimerUiState.Discarded)
    }

    @Test
    fun `countdown reaching zero moves to completed`() = runTimerTest {
        viewModel.setCustomDuration("5")
        viewModel.start()
        tickSeconds(5 * 60)

        val state = viewModel.uiState.value
        assertTrue(state is TimerUiState.Completed)
        assertEquals(5, state.durationMinutes)
        assertEquals(0L, state.remainingSeconds)
    }

    @Test
    fun `reset after a discarded session keeps the configured duration`() = runTimerTest {
        viewModel.selectPreset(45)
        viewModel.start()
        tickSeconds(5)
        viewModel.discard()

        viewModel.reset()

        val state = viewModel.uiState.value
        assertTrue(state is TimerUiState.Idle)
        assertEquals(45, state.durationMinutes)
        assertEquals(45L * 60L, state.remainingSeconds)
    }

    @Test
    fun `reset after completion keeps the configured duration`() = runTimerTest {
        viewModel.setCustomDuration("5")
        viewModel.start()
        tickSeconds(5 * 60)

        viewModel.reset()

        val state = viewModel.uiState.value
        assertTrue(state is TimerUiState.Idle)
        assertEquals(5, state.durationMinutes)
    }

    @Test
    fun `custom durations outside the allowed bounds are rejected`() = runTimerTest {
        viewModel.setCustomDuration("4")
        assertTrue(viewModel.customDurationError.value)
        assertEquals(25, viewModel.uiState.value.durationMinutes)

        viewModel.setCustomDuration("181")
        assertTrue(viewModel.customDurationError.value)
        assertEquals(25, viewModel.uiState.value.durationMinutes)
    }

    @Test
    fun `custom duration accepts a valid in-range value`() = runTimerTest {
        viewModel.setCustomDuration("45")

        assertFalse(viewModel.customDurationError.value)
        assertEquals(45, viewModel.uiState.value.durationMinutes)
    }

    @Test
    fun `custom duration rejects non-numeric text`() = runTimerTest {
        viewModel.setCustomDuration("abc")

        assertTrue(viewModel.customDurationError.value)
        assertEquals(25, viewModel.uiState.value.durationMinutes)
    }

    @Test
    fun `start is ignored while a custom duration is invalid`() = runTimerTest {
        viewModel.setCustomDuration("181")

        viewModel.start()

        assertTrue(viewModel.uiState.value is TimerUiState.Idle)
    }

    @Test
    fun `restore resumes countdown from persisted values`() = runTimerTest {
        // Persisted ten minutes ago with a 25-minute duration: nine hundred seconds must remain.
        clock.currentMillis = 600_000L
        repository.inProgressFlow.value = InProgress(startMillis = 0L, durationMinutes = 25)

        val restored = TimerViewModel(clock, repository)
        testScheduler.runCurrent()

        val state = restored.uiState.value
        assertTrue(state is TimerUiState.Running)
        assertEquals(25L * 60L - 600L, state.remainingSeconds)
        restored.reset()
    }

    @Test
    fun `restore continues the resumed countdown on the next ticks`() = runTimerTest {
        clock.currentMillis = 600_000L
        repository.inProgressFlow.value = InProgress(startMillis = 0L, durationMinutes = 25)

        val restored = TimerViewModel(clock, repository)
        testScheduler.runCurrent()

        // Drive the shared fake clock + scheduler forward one second at a time.
        clock.currentMillis += 1000L
        testScheduler.advanceTimeBy(1000L)
        testScheduler.runCurrent()

        assertEquals(25L * 60L - 601L, restored.uiState.value.remainingSeconds)
        restored.reset()
    }

    @Test
    fun `start persists an in-progress snapshot`() = runTimerTest {
        viewModel.selectPreset(45)

        viewModel.start()
        testScheduler.runCurrent()

        assertEquals(
            InProgress(startMillis = clock.currentMillis, durationMinutes = 45),
            repository.inProgressFlow.value,
        )
    }

    @Test
    fun `discard clears the in-progress snapshot`() = runTimerTest {
        viewModel.start()
        testScheduler.runCurrent()
        assertNotNull(repository.inProgressFlow.value)

        viewModel.discard()
        testScheduler.runCurrent()

        assertNull(repository.inProgressFlow.value)
    }

    @Test
    fun `completion records the session and clears in-progress`() = runTimerTest {
        viewModel.setCustomDuration("5")
        viewModel.start()
        tickSeconds(5 * 60)

        assertTrue(viewModel.uiState.value is TimerUiState.Completed)
        assertEquals(listOf(SessionRecord(0L, 5L * 60L * 1000L)), repository.completed)
        assertNull(repository.inProgressFlow.value)
    }

    @Test
    fun `clearing all data resets a running session to idle and keeps the duration`() = runTimerTest {
        viewModel.selectPreset(45)
        viewModel.start()
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value is TimerUiState.Running)

        repository.clearAll()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is TimerUiState.Idle)
        assertEquals(45, state.durationMinutes)
    }

    @Test
    fun `a cleared session cannot re-record an entry when its countdown finishes`() = runTimerTest {
        viewModel.setCustomDuration("5")
        viewModel.start()
        tickSeconds(5 * 60 - 1)

        repository.clearAll()
        testScheduler.runCurrent()

        // Drive past the original end instant; nothing must be recorded after the clear.
        tickSeconds(5)

        assertTrue(repository.completed.isEmpty())
    }
}
