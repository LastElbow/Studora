package com.bustedelbow.studora.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bustedelbow.studora.R
import com.bustedelbow.studora.domain.ShadeLevel
import com.bustedelbow.studora.ui.components.ChevronLeftIcon
import com.bustedelbow.studora.ui.components.ChevronRightIcon
import com.bustedelbow.studora.ui.theme.StudoraTheme
import com.bustedelbow.studora.ui.theme.heatmapRamp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/** One call per day of the week, Monday first. */
private const val DAYS_IN_WEEK = 7

/** Heatmap cell geometry, in dp. */
private val CELL_SIZE = 14.dp
private val CELL_GAP = 3.dp
private val CELL_SHAPE = RoundedCornerShape(2.dp)
private val LEGEND_SWATCH_SIZE = 12.dp

/** Axis gutter: the weekday label column plus the gap before the day columns. */
private val WEEKDAY_LABEL_WIDTH = 28.dp
private val LABEL_GAP = 8.dp

/**
 * Stateful history route: observes [HistoryViewModel] and delegates rendering to the stateless
 * [HistoryContent], forwarding the year-navigation intents.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryContent(
        state = state,
        onPreviousYear = viewModel::previousYear,
        onNextYear = viewModel::nextYear,
        modifier = modifier,
    )
}

/**
 * Stateless history screen: renders [state]'s 52-column, Monday-first window, with back/forward
 * controls that step one year at a time. Cell colours come from the theme's `heatmapRamp()`; every
 * non-future cell exposes a per-day content description. Days after the window's `today` render as
 * empty placeholders.
 *
 * The grid is wider than a phone, so it scrolls horizontally and starts pinned to the latest week.
 * Month, weekday, legend, and cell labels all come from string resources; axis month/weekday names
 * are locale-formatted at runtime.
 */
@Composable
fun HistoryContent(
    state: HistoryUiState,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
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
            is HistoryUiState.Content ->
                ContentState(
                    state = state,
                    onPreviousYear = onPreviousYear,
                    onNextYear = onNextYear,
                )
        }
    }
}

@Composable
private fun ColumnScope.LoadingState() {
    Box(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        val loadingDescription = stringResource(R.string.history_loading)
        CircularProgressIndicator(
            modifier =
                Modifier
                    .testTag(TAG_HISTORY_LOADING)
                    .semantics { contentDescription = loadingDescription },
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
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
) {
    val ramp = heatmapRamp()
    WindowNavigation(
        window = state.window,
        canGoBack = state.canGoBack,
        canGoForward = state.canGoForward,
        onPreviousYear = onPreviousYear,
        onNextYear = onNextYear,
    )
    Box(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        Heatmap(content = state, ramp = ramp)
    }
    HeatLegend(ramp = ramp)
}

@Composable
private fun WindowNavigation(
    window: HistoryWindow,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
) {
    val previousDescription = stringResource(R.string.history_previous_year)
    val nextDescription = stringResource(R.string.history_next_year)
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("MMM yyyy", locale) }
    val label =
        stringResource(
            R.string.history_period,
            window.startDate.format(formatter),
            window.endDate.format(formatter),
        )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick = onPreviousYear,
            enabled = canGoBack,
            modifier = Modifier.testTag(TAG_HISTORY_PREVIOUS),
        ) {
            Icon(imageVector = ChevronLeftIcon, contentDescription = previousDescription)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TAG_HISTORY_PERIOD),
        )
        IconButton(
            onClick = onNextYear,
            enabled = canGoForward,
            modifier = Modifier.testTag(TAG_HISTORY_NEXT),
        ) {
            Icon(imageVector = ChevronRightIcon, contentDescription = nextDescription)
        }
    }
}

/**
 * The full-year heatmap: month labels above, weekday labels to the left, and a Monday-first grid of
 * day columns. Horizontally scrollable; pinned to the latest week whenever the window changes.
 */
@Composable
private fun Heatmap(
    content: HistoryUiState.Content,
    ramp: List<Color>,
) {
    val windowStart = content.window.startDate
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.maxValue, content.window.offsetYears) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Column(modifier = Modifier.horizontalScroll(scrollState)) {
        MonthLabels(windowStart = windowStart)
        Row {
            WeekdayLabels()
            Spacer(Modifier.width(LABEL_GAP))
            HeatmapGrid(content = content, windowStart = windowStart, ramp = ramp)
        }
    }
}

/** Month labels, one per week column, shown where a new month begins (GitHub behaviour). */
@Composable
private fun MonthLabels(windowStart: LocalDate) {
    val locale = LocalConfiguration.current.locales[0]
    Row(modifier = Modifier.testTag(TAG_HISTORY_MONTH_LABELS)) {
        Spacer(Modifier.width(WEEKDAY_LABEL_WIDTH + LABEL_GAP))
        Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
            repeat(WEEKS_IN_WINDOW) { week ->
                val date = windowStart.plusWeeks(week.toLong())
                val startsMonth =
                    week == 0 || date.month != windowStart.plusWeeks((week - 1).toLong()).month
                Box(Modifier.width(CELL_SIZE)) {
                    if (startsMonth) {
                        Text(
                            text = date.month.getDisplayName(TextStyle.SHORT, locale),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            // Draw beyond the 14dp slot without widening it, so columns stay aligned.
                            modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
                        )
                    }
                }
            }
        }
    }
}

/** Monday-first weekday labels; only Mon / Wed / Fri are shown, as on GitHub. */
@Composable
private fun WeekdayLabels() {
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = Modifier.testTag(TAG_HISTORY_WEEKDAY_LABELS),
        verticalArrangement = Arrangement.spacedBy(CELL_GAP),
    ) {
        repeat(DAYS_IN_WEEK) { day ->
            val dayOfWeek = DayOfWeek.MONDAY.plus(day.toLong())
            val show =
                dayOfWeek == DayOfWeek.MONDAY ||
                    dayOfWeek == DayOfWeek.WEDNESDAY ||
                    dayOfWeek == DayOfWeek.FRIDAY
            Box(
                modifier = Modifier.size(width = WEEKDAY_LABEL_WIDTH, height = CELL_SIZE),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (show) {
                    Text(
                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** The Monday-first week columns; cell (week, day) maps to its calendar date. */
@Composable
private fun HeatmapGrid(
    content: HistoryUiState.Content,
    windowStart: LocalDate,
    ramp: List<Color>,
) {
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
                        isFuture = date.isAfter(content.today),
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
    val description = stringResource(R.string.history_cell_description, count, date.toString())
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
                base.semantics { contentDescription = description }
            },
    )
}

@Composable
private fun HeatLegend(ramp: List<Color>) {
    val legendDescription = stringResource(R.string.history_legend_description)
    val lessLabel = stringResource(R.string.history_legend_less)
    val moreLabel = stringResource(R.string.history_legend_more)
    Row(
        modifier =
            Modifier
                .testTag(TAG_HISTORY_LEGEND)
                .semantics { contentDescription = legendDescription },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = lessLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ramp.forEach { color ->
            Box(Modifier.size(LEGEND_SWATCH_SIZE).background(color = color, shape = CELL_SHAPE))
        }
        Text(
            text = moreLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal const val TAG_HISTORY_LOADING = "history_loading"
internal const val TAG_HISTORY_EMPTY = "history_empty"
internal const val TAG_HISTORY_GRID = "history_grid"
internal const val TAG_HISTORY_LEGEND = "history_legend"
internal const val TAG_HISTORY_MONTH_LABELS = "history_month_labels"
internal const val TAG_HISTORY_WEEKDAY_LABELS = "history_weekday_labels"
internal const val TAG_HISTORY_PREVIOUS = "history_previous"
internal const val TAG_HISTORY_NEXT = "history_next"
internal const val TAG_HISTORY_PERIOD = "history_period"
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
                    window = historyWindow(today, offsetYears = 0),
                    today = today,
                    canGoForward = false,
                    canGoBack = true,
                ),
            onPreviousYear = {},
            onNextYear = {},
        )
    }
}

@Preview(name = "History empty", showBackground = true)
@Composable
private fun HistoryEmptyPreview() {
    StudoraTheme {
        HistoryContent(
            state = HistoryUiState.Empty,
            onPreviousYear = {},
            onNextYear = {},
        )
    }
}

@Preview(name = "History loading", showBackground = true)
@Composable
private fun HistoryLoadingPreview() {
    StudoraTheme {
        HistoryContent(
            state = HistoryUiState.Loading,
            onPreviousYear = {},
            onNextYear = {},
        )
    }
}
