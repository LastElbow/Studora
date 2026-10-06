package com.bustedelbow.studora.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bustedelbow.studora.R
import com.bustedelbow.studora.ui.theme.StudoraTheme
import java.util.Locale

/** The durations offered as one-tap presets, in minutes. */
private val PresetMinutes = listOf(15, 25, 45, 60)

/**
 * Stateful timer route: observes [TimerViewModel] and delegates rendering to the stateless
 * [TimerContent]. Keeping the split means previews and (future) screenshot tests can drive the UI
 * with plain state and callbacks, without a ViewModel or Hilt.
 */
@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val customDurationError by viewModel.customDurationError.collectAsStateWithLifecycle()

    TimerContent(
        state = state,
        customDurationError = customDurationError,
        onCustomDurationChange = viewModel::setCustomDuration,
        onPresetSelected = viewModel::selectPreset,
        onStart = viewModel::start,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onDiscard = viewModel::discard,
        onReset = viewModel::reset,
        modifier = modifier,
    )
}

/**
 * Stateless timer screen: it renders [state] and forwards user intent through the callbacks.
 *
 * The only local state is the custom-duration text field, which is UI-only and survives
 * configuration changes via [rememberSaveable]; every timer decision lives in
 * [TimerViewModel]. Colours, type, and control metrics come from [MaterialTheme] tokens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerContent(
    state: TimerUiState,
    customDurationError: Boolean,
    onCustomDurationChange: (String) -> Unit,
    onPresetSelected: (Int) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var customDurationText by rememberSaveable { mutableStateOf("") }

    val remaining = formatDuration(state.remainingSeconds)
    val stateLabel =
        when (state) {
            is TimerUiState.Idle -> stringResource(R.string.timer_state_idle)
            is TimerUiState.Running -> stringResource(R.string.timer_state_running)
            is TimerUiState.Paused -> stringResource(R.string.timer_state_paused)
            is TimerUiState.Completed -> stringResource(R.string.timer_state_completed)
            is TimerUiState.Discarded -> stringResource(R.string.timer_state_discarded)
        }
    val displayDescription = stringResource(R.string.timer_display_description, remaining)
    val progress = progressFraction(state)

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.timer_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = remaining,
            style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .testTag(TAG_TIMER_DISPLAY)
                    .semantics {
                        contentDescription = displayDescription
                        stateDescription = stateLabel
                    },
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().testTag(TAG_TIMER_PROGRESS),
        )

        when (state) {
            is TimerUiState.Idle -> {
                DurationPicker(
                    selectedMinutes = state.durationMinutes,
                    customDurationText = customDurationText,
                    customDurationError = customDurationError,
                    onCustomDurationChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(3)
                        customDurationText = digits
                        onCustomDurationChange(digits)
                    },
                    onPresetSelected = { minutes ->
                        customDurationText = ""
                        onPresetSelected(minutes)
                    },
                )

                val startLabel = stringResource(R.string.timer_start)
                Button(
                    onClick = onStart,
                    enabled = !customDurationError,
                    modifier =
                        Modifier
                            .testTag(TAG_START)
                            .minimumInteractiveComponentSize()
                            .semantics { contentDescription = startLabel },
                ) {
                    Text(startLabel)
                }
            }

            is TimerUiState.Running -> {
                ActiveControls(
                    primaryLabel = stringResource(R.string.timer_pause),
                    primaryTag = TAG_PAUSE,
                    onPrimary = onPause,
                    onDiscard = onDiscard,
                )
            }

            is TimerUiState.Paused -> {
                ActiveControls(
                    primaryLabel = stringResource(R.string.timer_resume),
                    primaryTag = TAG_RESUME,
                    onPrimary = onResume,
                    onDiscard = onDiscard,
                )
            }

            is TimerUiState.Completed -> {
                TerminalControls(
                    message = stringResource(R.string.timer_completed_message),
                    onReset = onReset,
                )
            }

            is TimerUiState.Discarded -> {
                TerminalControls(
                    message = stringResource(R.string.timer_discarded_message),
                    onReset = onReset,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationPicker(
    selectedMinutes: Int,
    customDurationText: String,
    customDurationError: Boolean,
    onCustomDurationChange: (String) -> Unit,
    onPresetSelected: (Int) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PresetMinutes.forEach { minutes ->
            val label = stringResource(R.string.timer_preset_minutes, minutes)
            FilterChip(
                selected = selectedMinutes == minutes,
                onClick = { onPresetSelected(minutes) },
                label = { Text(label) },
                modifier =
                    Modifier
                        .testTag("$TAG_PRESET_PREFIX$minutes")
                        .minimumInteractiveComponentSize()
                        .semantics { contentDescription = label },
            )
        }
    }

    OutlinedTextField(
        value = customDurationText,
        onValueChange = onCustomDurationChange,
        label = { Text(stringResource(R.string.timer_custom_label)) },
        isError = customDurationError,
        supportingText = {
            if (customDurationError) {
                Text(stringResource(R.string.timer_custom_error))
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().testTag(TAG_CUSTOM_INPUT),
    )
}

@Composable
private fun ActiveControls(
    primaryLabel: String,
    primaryTag: String,
    onPrimary: () -> Unit,
    onDiscard: () -> Unit,
) {
    val discardLabel = stringResource(R.string.timer_discard)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FilledTonalButton(
            onClick = onPrimary,
            modifier =
                Modifier
                    .testTag(primaryTag)
                    .minimumInteractiveComponentSize()
                    .semantics { contentDescription = primaryLabel },
        ) {
            Text(primaryLabel)
        }
        OutlinedButton(
            onClick = onDiscard,
            modifier =
                Modifier
                    .testTag(TAG_DISCARD)
                    .minimumInteractiveComponentSize()
                    .semantics { contentDescription = discardLabel },
        ) {
            Text(discardLabel)
        }
    }
}

@Composable
private fun TerminalControls(
    message: String,
    onReset: () -> Unit,
) {
    val resetLabel = stringResource(R.string.timer_new_session)
    Text(text = message, style = MaterialTheme.typography.titleMedium)
    Button(
        onClick = onReset,
        modifier =
            Modifier
                .testTag(TAG_RESET)
                .minimumInteractiveComponentSize()
                .semantics { contentDescription = resetLabel },
    ) {
        Text(resetLabel)
    }
}

private fun progressFraction(state: TimerUiState): Float {
    val total = state.durationMinutes.toFloat() * 60f
    if (total <= 0f) return 0f
    return (1f - state.remainingSeconds.toFloat() / total).coerceIn(0f, 1f)
}

private fun formatDuration(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val minutes = safe / 60
    val seconds = safe % 60
    return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
}

internal const val TAG_TIMER_DISPLAY = "timer_display"
internal const val TAG_TIMER_PROGRESS = "timer_progress"
internal const val TAG_START = "timer_start"
internal const val TAG_PAUSE = "timer_pause"
internal const val TAG_RESUME = "timer_resume"
internal const val TAG_DISCARD = "timer_discard"
internal const val TAG_RESET = "timer_reset"
internal const val TAG_CUSTOM_INPUT = "timer_custom_input"
internal const val TAG_PRESET_PREFIX = "timer_preset_"

@Preview(name = "Timer idle", showBackground = true)
@Composable
private fun TimerContentIdlePreview() {
    StudoraTheme {
        TimerContent(
            state = TimerUiState.Idle(),
            customDurationError = false,
            onCustomDurationChange = {},
            onPresetSelected = {},
            onStart = {},
            onPause = {},
            onResume = {},
            onDiscard = {},
            onReset = {},
        )
    }
}

@Preview(name = "Timer running", showBackground = true)
@Composable
private fun TimerContentRunningPreview() {
    StudoraTheme {
        TimerContent(
            state = TimerUiState.Running(remainingSeconds = 15L * 60L, durationMinutes = 25),
            customDurationError = false,
            onCustomDurationChange = {},
            onPresetSelected = {},
            onStart = {},
            onPause = {},
            onResume = {},
            onDiscard = {},
            onReset = {},
        )
    }
}
