package com.bustedelbow.studora.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bustedelbow.studora.domain.Clock
import com.bustedelbow.studora.domain.SessionRecord
import com.bustedelbow.studora.domain.SessionRepository
import com.bustedelbow.studora.domain.ShadeLevel
import com.bustedelbow.studora.domain.countByDay
import com.bustedelbow.studora.domain.shadeForCount
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Monday-first week columns in one heatmap window: 52 whole weeks (364 days), matching the rolling
 * year GitHub shows. The MVP slice used 16.
 */
internal const val WEEKS_IN_WINDOW = 52

/**
 * One rendered heatmap window: the [WEEKS_IN_WINDOW] Monday-first weeks ending in the week that
 * contains the anchor `today.minusYears(offsetYears)`.
 *
 * @param startDate Monday of the first column.
 * @param endDate Sunday of the last column.
 * @param offsetYears whole years back from today; `0` is the current window.
 */
data class HistoryWindow(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val offsetYears: Int,
)

/**
 * UI state for the History heatmap.
 *
 * [Loading] is the state before the repository's first emission; [Empty] means there are no
 * completed sessions at all, so the screen shows the "no sessions yet" copy instead of a grid;
 * [Content] carries the per-day summaries for one [HistoryWindow] plus the navigation bounds.
 */
sealed interface HistoryUiState {

    /** Waiting for the first repository emission. */
    data object Loading : HistoryUiState

    /** Repository emitted, but no completed sessions exist yet. */
    data object Empty : HistoryUiState

    /**
     * Completed sessions bucketed by the local day they started, for the selected [window].
     *
     * [shadesByDay] drives cell colour; [countsByDay] lets each cell describe how many sessions it
     * holds. Only days inside [window] are present; days with no sessions are absent from both maps.
     * [today] marks the last real day (cells after it are future placeholders). [canGoForward] and
     * [canGoBack] bound year navigation.
     */
    data class Content(
        val shadesByDay: Map<LocalDate, ShadeLevel>,
        val countsByDay: Map<LocalDate, Int>,
        val window: HistoryWindow,
        val today: LocalDate,
        val canGoForward: Boolean,
        val canGoBack: Boolean,
    ) : HistoryUiState
}

/**
 * Projects the completed-session history onto the History heatmap state for the selected window.
 *
 * The repository flow is combined with the year offset, then mapped with the pure `StudyHeat`
 * functions plus [historyWindow]. `ZoneId.systemDefault()` and the injected [Clock] are read here,
 * at the seam between the framework-free domain and the app, so the domain keeps taking explicit
 * time. This is the only part of the mapping the tests cannot control independently.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val clock: Clock,
) : ViewModel() {

    private val offsetYears = MutableStateFlow(0)

    init {
        // A clear erases all data; snap back to the current window so the user is never stranded.
        viewModelScope.launch { repository.clears.collect { offsetYears.value = 0 } }
    }

    val uiState: StateFlow<HistoryUiState> =
        combine(repository.completedSessions(), offsetYears) { sessions, offset -> sessions to offset }
            .map { (sessions, offset) ->
                val zone = ZoneId.systemDefault()
                val today = Instant.ofEpochMilli(clock.nowMillis()).atZone(zone).toLocalDate()
                sessions.toHistoryUiState(zone, today, offset)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = HistoryUiState.Loading,
            )

    /** Moves to the previous year's window. The UI disables this once [HistoryUiState.Content.canGoBack] is false. */
    fun previousYear() {
        offsetYears.update { it + 1 }
    }

    /** Moves forward, never past the current window. */
    fun nextYear() {
        offsetYears.update { (it - 1).coerceAtLeast(0) }
    }

    companion object {
        /** Grace period before the repository flow is torn down when the screen stops observing. */
        const val STOP_TIMEOUT_MILLIS: Long = 5_000L
    }
}

/**
 * The window for [offsetYears] whole years before [today]: the Monday-first block ending in the week
 * containing the anchor date.
 */
internal fun historyWindow(today: LocalDate, offsetYears: Int): HistoryWindow {
    val anchorMonday =
        today.minusYears(offsetYears.toLong())
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return HistoryWindow(
        startDate = anchorMonday.minusWeeks((WEEKS_IN_WINDOW - 1).toLong()),
        endDate = anchorMonday.plusDays(6),
        offsetYears = offsetYears,
    )
}

/**
 * Maps completed [sessions] to [HistoryUiState] for the window [offsetYears] years before [today],
 * bucketing them by their start day in [zone].
 *
 * Kept pure and top-level so the window and day-bucketing contract is independent of Android/Hilt
 * wiring. A window contains only the days inside it; navigation is bounded by the earliest year
 * that has data.
 */
internal fun List<SessionRecord>.toHistoryUiState(
    zone: ZoneId,
    today: LocalDate,
    offsetYears: Int,
): HistoryUiState {
    if (isEmpty()) return HistoryUiState.Empty
    val window = historyWindow(today, offsetYears)
    val allCounts = countByDay(this, zone)
    val counts = allCounts.filterKeys { it in window.startDate..window.endDate }
    val earliestYear = allCounts.keys.minOf { it.year }
    val maxOffset = (today.year - earliestYear).coerceAtLeast(0)
    return HistoryUiState.Content(
        shadesByDay = counts.mapValues { (_, count) -> shadeForCount(count) },
        countsByDay = counts,
        window = window,
        today = today,
        canGoForward = offsetYears > 0,
        canGoBack = offsetYears < maxOffset,
    )
}
