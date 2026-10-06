package com.bustedelbow.studora.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bustedelbow.studora.ui.history.HistoryScreen
import com.bustedelbow.studora.ui.timer.TimerScreen

/**
 * Top-level navigation graph for the Timer | History tabs (ADR-0003).
 *
 * Each destination keeps its own state: navigation-compose saves the back-stack entry's
 * `SaveableStateHolder` state, so the Timer's input and the History grid survive tab switches.
 * The Timer destination owns its own Hilt ViewModel; History owns [HistoryScreen]'s, which keeps
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
            HistoryScreen(viewModel = hiltViewModel())
        }
    }
}
