package com.bustedelbow.studora.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bustedelbow.studora.R
import com.bustedelbow.studora.domain.ShadeLevel
import com.bustedelbow.studora.ui.theme.StudoraTheme
import com.bustedelbow.studora.ui.theme.heatmapRamp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Monday-first week columns in the heatmap window: the last 16 weeks including the current one. */
private const val WEEKS_IN_WINDOW = 16

/** One call per day of the week, Monday first. */
private const val DAYS_IN_WEEK = 7

/** Heatmap cell geometry, in dp. */
private val CELL_SIZE = 14.dp
private val CELL_GAP = 3.dp
private val CELL_SHAPE = RoundedCornerShape(2.dp)
private val LEGEND_SWATCH_SIZE = 12.dp

/**
 * Stateful history route: observes [HistoryViewModel] and delegates rendering to the stateless
 * [HistoryContent]. The window anchor (`today`) is read once here so the content function stays a
 * pure projection of its arguments and the previews are deterministic.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }

    HistoryContent(state = state, today = today, modifier = modifier)
}

/**
 * Stateless history screen: renders [state] and builds the Monday-first 16-week grid anchored on
 * [today]. Cell colours come from the theme's `heatmapRamp()`; every non-future cell exposes a
 * per-day content description. Days after [today] render as empty placeholders.
 *
 * The visible "Less"/"More" legend labels are hard-coded because this slice is limited to these
 * Kotlin files and deliberately does not touch `strings.xml`.
 */
@Composable
fun HistoryContent(
    state: HistoryUiState,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        when (state) {
            HistoryUiState.Loading -> LoadingState()
            HistoryUiState.Empty -> EmptyState()
            is HistoryUiState.Content -> ContentState(state = state, today = today)
        }
    }
}

@Composable
private fun ColumnScope.LoadingState() {
    Box(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier =
                Modifier
                    .testTag(TAG_HISTORY_LOADING)
                    .semantics { contentDescription = LOADING_DESCRIPTION },
        )
    }
}

@Composable
private fun ColumnScope.EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TAG_HISTORY_EMPTY),
        )
    }
}

@Composable
private fun ColumnScope.ContentState(
    state: HistoryUiState.Content,
    today: LocalDate,
) {
    val ramp = heatmapRamp()
    Box(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        HeatmapGrid(content = state, today = today, ramp = ramp)
    }
    HeatLegend(ramp = ramp)
}

/** The Monday-first week columns; cell (week, day) maps to its calendar date. */
@Composable
private fun HeatmapGrid(
    content: HistoryUiState.Content,
    today: LocalDate,
    ramp: List<Color>,
) {
    val windowStart =
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .minusWeeks((WEEKS_IN_WINDOW - 1).toLong())

    Row(
        modifier = Modifier.testTag(TAG_HISTORY_GRID),
        horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
    ) {
        repeat(WEEKS_IN_WINDOW) { week ->
            Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                repeat(DAYS_IN_WEEK) { day ->
                    val date = windowStart.plusDays((week * DAYS_IN_WEEK + day).toLong())
                    HeatCell(
                        date = date,
                        count = content.countsByDay[date] ?: 0,
                        shade = content.shadesByDay[date] ?: ShadeLevel.NONE,
                        isFuture = date.isAfter(today),
                        ramp = ramp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatCell(
    date: LocalDate,
    count: Int,
    shade: ShadeLevel,
    isFuture: Boolean,
    ramp: List<Color>,
) {
    val color = if (isFuture) Color.Transparent else ramp[shade.ordinal]
    val base =
        Modifier
            .size(CELL_SIZE)
            .background(color = color, shape = CELL_SHAPE)
            .testTag("$TAG_HISTORY_CELL_PREFIX$date")
    Box(
        modifier =
            if (isFuture) {
                base
            } else {
                base.semantics { contentDescription = cellDescription(count, date) }
            },
    )
}

@Composable
private fun HeatLegend(ramp: List<Color>) {
    Row(
        modifier =
            Modifier
                .testTag(TAG_HISTORY_LEGEND)
                .semantics { contentDescription = LEGEND_DESCRIPTION },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = LEGEND_LESS,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ramp.forEach { color ->
            Box(Modifier.size(LEGEND_SWATCH_SIZE).background(color = color, shape = CELL_SHAPE))
        }
        Text(
            text = LEGEND_MORE,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Per-cell accessibility text, e.g. "2 sessions on 2026-01-15". */
private fun cellDescription(count: Int, date: LocalDate): String =
    "$count sessions on $date"

private const val LOADING_DESCRIPTION = "Loading study history"
private const val LEGEND_LESS = "Less"
private const val LEGEND_MORE = "More"
private const val LEGEND_DESCRIPTION = "Shade legend, fewer to more sessions"

internal const val TAG_HISTORY_LOADING = "history_loading"
internal const val TAG_HISTORY_EMPTY = "history_empty"
internal const val TAG_HISTORY_GRID = "history_grid"
internal const val TAG_HISTORY_LEGEND = "history_legend"
internal const val TAG_HISTORY_CELL_PREFIX = "history_cell_"

@Preview(name = "History content", showBackground = true)
@Composable
private fun HistoryContentPreview() {
    val today = LocalDate.of(2026, 1, 18)
    StudoraTheme {
        HistoryContent(
            state =
                HistoryUiState.Content(
                    shadesByDay =
                        mapOf(
                            today to ShadeLevel.INTENSE,
                            today.minusDays(1) to ShadeLevel.DARK,
                            today.minusDays(2) to ShadeLevel.MEDIUM,
                            today.minusDays(3) to ShadeLevel.LIGHT,
                            today.minusDays(10) to ShadeLevel.MEDIUM,
                        ),
                    countsByDay =
                        mapOf(
                            today to 6,
                            today.minusDays(1) to 4,
                            today.minusDays(2) to 2,
                            today.minusDays(3) to 1,
                            today.minusDays(10) to 3,
                        ),
                ),
            today = today,
        )
    }
}

@Preview(name = "History empty", showBackground = true)
@Composable
private fun HistoryEmptyPreview() {
    StudoraTheme {
        HistoryContent(state = HistoryUiState.Empty, today = LocalDate.of(2026, 1, 18))
    }
}

@Preview(name = "History loading", showBackground = true)
@Composable
private fun HistoryLoadingPreview() {
    StudoraTheme {
        HistoryContent(state = HistoryUiState.Loading, today = LocalDate.of(2026, 1, 18))
    }
}
