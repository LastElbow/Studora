package com.bustedelbow.studora.ui.settings

import com.bustedelbow.studora.domain.InProgress
import com.bustedelbow.studora.domain.SessionRecord
import com.bustedelbow.studora.domain.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Behaviour tests for [SettingsViewModel].
 *
 * The destructive erase is verified through the [SessionRepository] seam: the view-model must ask
 * the repository to clear everything, and must announce completion for the UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeSessionRepository
    private lateinit var viewModel: SettingsViewModel

    /** In-memory [SessionRepository] recording how many times a clear was requested. */
    private class FakeSessionRepository(
        completed: List<SessionRecord> = emptyList(),
    ) : SessionRepository {
        val completedFlow = MutableStateFlow(completed)
        val inProgressFlow = MutableStateFlow<InProgress?>(null)
        var clearAllCalls = 0
        private val clearsFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        override suspend fun recordCompleted(record: SessionRecord) {
            completedFlow.value += record
        }

        override fun completedSessions(): Flow<List<SessionRecord>> = completedFlow

        override suspend fun saveInProgress(startMillis: Long, durationMinutes: Int) {
            inProgressFlow.value = InProgress(startMillis = startMillis, durationMinutes = durationMinutes)
        }

        override fun inProgress(): Flow<InProgress?> = inProgressFlow

        override suspend fun clearInProgress() {
            inProgressFlow.value = null
        }

        override suspend fun clearAll() {
            clearAllCalls++
            completedFlow.value = emptyList()
            inProgressFlow.value = null
            clearsFlow.emit(Unit)
        }

        override val clears: Flow<Unit> = clearsFlow.asSharedFlow()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeSessionRepository(
            completed = listOf(SessionRecord(startMillis = 1_000L, endMillis = 2_000L)),
        )
        viewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `clear all asks the repository to erase everything`() = runTest(dispatcher) {
        viewModel.clearAll()
        testScheduler.advanceUntilIdle()

        assertEquals(1, repository.clearAllCalls)
        assertTrue(repository.completedFlow.value.isEmpty())
        assertEquals(null, repository.inProgressFlow.value)
    }

    @Test
    fun `clear all announces completion so the UI can confirm`() = runTest(dispatcher) {
        val announced = async { viewModel.cleared.first() }

        viewModel.clearAll()
        testScheduler.advanceUntilIdle()

        assertTrue(announced.isCompleted)
        announced.await()
    }
}
