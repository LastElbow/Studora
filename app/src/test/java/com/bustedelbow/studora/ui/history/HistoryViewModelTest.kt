package com.bustedelbow.studora.ui.history

import com.bustedelbow.studora.domain.Clock
import com.bustedelbow.studora.domain.InProgress
import com.bustedelbow.studora.domain.SessionRecord
import com.bustedelbow.studora.domain.SessionRepository
import com.bustedelbow.studora.domain.ShadeLevel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Behaviour tests for [HistoryViewModel].
 *
 * The view-model maps the repository's completed-session flow onto per-day [ShadeLevel]s for a
 * selected window. The production bucketing zone is `ZoneId.systemDefault()`; these tests build
 * their instants from the same default zone, so they stay deterministic without controlling the
 * device zone. The countdown of days itself is covered by `StudyHeatTest`; the window mapping is
 * covered here through the view-model.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val zone: ZoneId = ZoneId.systemDefault()
    private val clock = FakeClock()

    private class FakeClock(var currentMillis: Long = 0L) : Clock {
        override fun nowMillis(): Long = currentMillis
    }

    /** In-memory [SessionRepository] so the view-model never touches Room under test. */
    private class FakeSessionRepository(
        completed: List<SessionRecord> = emptyList(),
    ) : SessionRepository {
        private val completedFlow = MutableStateFlow(completed)
        private val clearsFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        override suspend fun recordCompleted(record: SessionRecord) {
            completedFlow.value += record
        }

        override fun completedSessions(): Flow<List<SessionRecord>> = completedFlow

        override suspend fun saveInProgress(startMillis: Long, durationMinutes: Int) {
            // Not exercised by the history mapping.
        }

        override fun inProgress(): Flow<InProgress?> = MutableStateFlow(null)

        override suspend fun clearInProgress() {
            // Not exercised by the history mapping.
        }

        override suspend fun clearAll() {
            completedFlow.value = emptyList()
        }

        override val clears: Flow<Unit> = clearsFlow.asSharedFlow()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        clock.currentMillis = at(2026, 1, 20, 12, 0)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private fun session(startMillis: Long, durationMinutes: Long): SessionRecord =
        SessionRecord(startMillis, startMillis + durationMinutes * 60_000L)

    private fun TestScope.viewModelWith(completed: List<SessionRecord>): HistoryViewModel =
        HistoryViewModel(FakeSessionRepository(completed), clock)

    private suspend fun HistoryViewModel.contentWithOffset(offset: Int): HistoryUiState.Content =
        uiState.first { it is HistoryUiState.Content && it.window.offsetYears == offset }
            as HistoryUiState.Content

    @Test
    fun `initial state is loading before history emits`() = runTest(dispatcher) {
        val viewModel = viewModelWith(emptyList())

        assertTrue(viewModel.uiState.value is HistoryUiState.Loading)
    }

    @Test
    fun `empty history maps to Empty`() = runTest(dispatcher) {
        val viewModel = viewModelWith(emptyList())

        val state = viewModel.uiState.first { it !is HistoryUiState.Loading }

        assertTrue(state is HistoryUiState.Empty)
    }

    @Test
    fun `sessions map to their start-day shades and counts`() = runTest(dispatcher) {
        val dayOne = at(2026, 1, 15, 9, 0)
        val dayTwo = at(2026, 1, 16, 9, 0)
        val records = listOf(
            session(dayOne, durationMinutes = 25),
            session(dayOne + 3_600_000L, durationMinutes = 25),
            session(dayOne + 7_200_000L, durationMinutes = 25),
            session(dayTwo, durationMinutes = 25),
        )
        val viewModel = viewModelWith(records)

        val state = viewModel.contentWithOffset(0)

        assertEquals(
            mapOf(
                LocalDate.of(2026, 1, 15) to ShadeLevel.MEDIUM,
                LocalDate.of(2026, 1, 16) to ShadeLevel.LIGHT,
            ),
            state.shadesByDay,
        )
        assertEquals(
            mapOf(LocalDate.of(2026, 1, 15) to 3, LocalDate.of(2026, 1, 16) to 1),
            state.countsByDay,
        )
    }

    @Test
    fun `midnight-straddling session shades its start day only`() = runTest(dispatcher) {
        val start = at(2026, 1, 15, 23, 50)
        val end = at(2026, 1, 16, 0, 15)
        val viewModel = viewModelWith(listOf(SessionRecord(startMillis = start, endMillis = end)))

        val state = viewModel.contentWithOffset(0)

        assertEquals(mapOf(LocalDate.of(2026, 1, 15) to ShadeLevel.LIGHT), state.shadesByDay)
        assertEquals(mapOf(LocalDate.of(2026, 1, 15) to 1), state.countsByDay)
    }

    @Test
    fun `current window disables forward navigation`() = runTest(dispatcher) {
        val viewModel = viewModelWith(listOf(session(at(2026, 1, 15, 9, 0), 25)))

        val state = viewModel.contentWithOffset(0)

        assertFalse(state.canGoForward)
    }

    @Test
    fun `back navigation is disabled when all data is in the current window`() = runTest(dispatcher) {
        val viewModel = viewModelWith(listOf(session(at(2026, 1, 15, 9, 0), 25)))

        val state = viewModel.contentWithOffset(0)

        assertFalse(state.canGoBack)
    }

    @Test
    fun `previous year shows the shifted window with only its own sessions`() = runTest(dispatcher) {
        clock.currentMillis = at(2026, 6, 15, 12, 0)
        val records = listOf(
            session(at(2026, 1, 15, 9, 0), 25),
            session(at(2025, 1, 15, 9, 0), 25),
        )
        val viewModel = viewModelWith(records)

        val current = viewModel.contentWithOffset(0)
        assertTrue(current.canGoBack)
        assertFalse(current.canGoForward)
        assertEquals(mapOf(LocalDate.of(2026, 1, 15) to 1), current.countsByDay)

        viewModel.previousYear()

        val past = viewModel.contentWithOffset(1)
        assertTrue(past.canGoForward)
        assertFalse(past.canGoBack)
        assertEquals(mapOf(LocalDate.of(2025, 1, 15) to 1), past.countsByDay)
    }

    @Test
    fun `next year returns to the current window`() = runTest(dispatcher) {
        clock.currentMillis = at(2026, 6, 15, 12, 0)
        val records = listOf(
            session(at(2026, 1, 15, 9, 0), 25),
            session(at(2025, 1, 15, 9, 0), 25),
        )
        val viewModel = viewModelWith(records)

        viewModel.contentWithOffset(0)
        viewModel.previousYear()
        viewModel.contentWithOffset(1)

        viewModel.nextYear()

        val current = viewModel.contentWithOffset(0)
        assertFalse(current.canGoForward)
    }
}
