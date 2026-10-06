package com.bustedelbow.studora.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bustedelbow.studora.R
import com.bustedelbow.studora.ui.theme.StudoraTheme

/**
 * Stateful Settings route: wires [SettingsViewModel]'s clear action and completion signal to the
 * stateless [SettingsContent]. The confirmation is owned by the content; the ViewModel only erases.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clearedMessage = stringResource(R.string.settings_clear_done)
    LaunchedEffect(viewModel, clearedMessage) {
        viewModel.cleared.collect { snackbarHostState.showSnackbar(clearedMessage) }
    }

    SettingsContent(
        onClearAll = viewModel::clearAll,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless Settings screen.
 *
 * The destructive action is rendered in the theme's error container colour and guarded by an
 * [AlertDialog]; confirming forwards to [onClearAll]. Completion is announced through
 * [snackbarHostState]. Colours, type, and metrics come from [MaterialTheme] tokens.
 */
@Composable
fun SettingsContent(
    onClearAll: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    val clearLabel = stringResource(R.string.settings_clear_data)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_data_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.settings_clear_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { showConfirm = true },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(TAG_SETTINGS_CLEAR)
                        .minimumInteractiveComponentSize()
                        .semantics { contentDescription = clearLabel },
            ) {
                Text(clearLabel)
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            modifier = Modifier.testTag(TAG_SETTINGS_CONFIRM_DIALOG),
            title = { Text(stringResource(R.string.settings_clear_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        onClearAll()
                    },
                    modifier = Modifier.testTag(TAG_SETTINGS_CONFIRM),
                ) {
                    Text(
                        text = stringResource(R.string.settings_clear_confirm_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirm = false },
                    modifier = Modifier.testTag(TAG_SETTINGS_CANCEL),
                ) {
                    Text(stringResource(R.string.settings_clear_cancel))
                }
            },
        )
    }
}

internal const val TAG_SETTINGS_CLEAR = "settings_clear"
internal const val TAG_SETTINGS_CONFIRM_DIALOG = "settings_confirm_dialog"
internal const val TAG_SETTINGS_CONFIRM = "settings_confirm"
internal const val TAG_SETTINGS_CANCEL = "settings_cancel"

@Preview(name = "Settings", showBackground = true)
@Composable
private fun SettingsContentPreview() {
    StudoraTheme {
        SettingsContent(onClearAll = {}, snackbarHostState = remember { SnackbarHostState() })
    }
}
