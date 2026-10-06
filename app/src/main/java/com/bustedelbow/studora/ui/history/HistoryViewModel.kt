package com.bustedelbow.studora.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bustedelbow.studora.domain.SessionRecord
import com.bustedelbow.studora.domain.SessionRepository
import com.bustedelbow.studora.domain.ShadeLevel
import com.bustedelbow.studora.domain.countByDay
import com.bustedelbow.studora.domain.shadeForCount
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * UI state for the History heatmap.
 *
 * [Loading] is the state before the repository's first emission; [Empty] means there are no
 * completed sessions at all, so the screen shows the "no sessions yet" copy instead of a grid;
 * [Content] carries the per-day summaries the grid renders.
 */
sealed interface HistoryUiState {

    /** Waiting for the first repository emission. */
    data object Loading : HistoryUiState

    /** Repository emitted, but no completed sessions exist yet. */
    data object Empty : HistoryUiState

    /**
     * Completed sessions bucketed by the local day they started.
     *
     * [shadesByDay] drives cell colour; [countsByDay] lets each cell describe how many sessions
     * it holds. Days with no sessions are absent from both maps.
     */
    data class Content(
        val shadesByDay: Map<LocalDate, ShadeLevel>,
        val countsByDay: Map<LocalDate, Int>,
    ) : HistoryUiState
}

/**
 * Projects the completed-session history onto the History heatmap state.
 *
 * The repository flow is mapped with the pure `StudyHeat` functions; `ZoneId.systemDefault()` is
 * supplied here, at the seam between the framework-free domain and the app, so the domain keeps
 * taking an explicit zone. This is the only line in the mapping the tests cannot control.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: SessionRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> =
        repository.completedSessions()
            .map { sessions -> sessions.toHistoryUiState(ZoneId.systemDefault()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = HistoryUiState.Loading,
            )

    companion object {
        /** Grace period before the repository flow is torn down when the screen stops observing. */
        const val STOP_TIMEOUT_MILLIS: Long = 5_000L
    }
}

/**
 * Maps completed [sessions] to [HistoryUiState], bucketing them by their start day in [zone].
 *
 * Kept pure and top-level so the day-bucketing contract is independent of Android/Hilt wiring.
 */
internal fun List<SessionRecord>.toHistoryUiState(zone: ZoneId): HistoryUiState {
    if (isEmpty()) return HistoryUiState.Empty
    val counts = countByDay(this, zone)
    return HistoryUiState.Content(
        shadesByDay = counts.mapValues { (_, count) -> shadeForCount(count) },
        countsByDay = counts,
    )
}
