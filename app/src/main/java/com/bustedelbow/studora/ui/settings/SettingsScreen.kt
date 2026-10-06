package com.bustedelbow.studora.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bustedelbow.studora.R
import com.bustedelbow.studora.ui.theme.StudoraTheme

/**
 * Settings route.
 *
 * Slice #4 renders a placeholder body; the destructive clear-data action lands in slice #5, which
 * will also give this route a ViewModel. The stateful/stateless split mirrors [TimerScreen] and
 * [HistoryScreen] so previews stay ViewModel-free.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    SettingsContent(modifier = modifier)
}

/** Stateless Settings screen. */
@Composable
fun SettingsContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.settings_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(name = "Settings", showBackground = true)
@Composable
private fun SettingsContentPreview() {
    StudoraTheme {
        SettingsContent()
    }
}
