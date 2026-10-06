package com.bustedelbow.studora.ui.navigation

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bustedelbow.studora.R
import com.bustedelbow.studora.ui.timer.TimerScreen

/**
 * Top-level navigation graph for the Timer | History tabs (ADR-0003).
 *
 * Each destination keeps its own state: navigation-compose saves the back-stack entry's
 * `SaveableStateHolder` state, so Timer's input and History's (future) grid survive tab switches.
 * The Timer destination owns its own Hilt ViewModel; History owns its own in slice 6b, which keeps
 * the ticker and the heatmap independent.
 */
@Composable
fun StudoraNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Timer.route,
        modifier = modifier,
    ) {
        composable(Route.Timer.route) {
            TimerScreen(viewModel = hiltViewModel())
        }
        composable(Route.History.route) {
            HistoryPlaceholder()
        }
    }
}

/**
 * Slice 6a placeholder for the History destination: title plus empty-state copy, no grid yet.
 *
 * Slice 6b replaces this with the History screen and its ViewModel.
 */
@Composable
private fun HistoryPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
