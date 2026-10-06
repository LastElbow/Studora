package com.bustedelbow.studora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bustedelbow.studora.ui.components.DateRangeIcon
import com.bustedelbow.studora.ui.components.HomeIcon
import com.bustedelbow.studora.ui.components.SettingsIcon
import com.bustedelbow.studora.ui.navigation.Route
import com.bustedelbow.studora.ui.navigation.StudoraNavHost
import com.bustedelbow.studora.ui.theme.StudoraTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single entry point. Hosts the Timer | History | Settings bottom-tab scaffold (ADR-0003, ADR-0004). */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudoraTheme {
                StudoraRoot()
            }
        }
    }
}

/**
 * Root scaffold: a bottom [NavigationBar] drives a [StudoraNavHost].
 *
 * Tapping a tab uses the canonical bottom-navigation options so the selected destination is
 * restored from, and saved into, the back stack (`saveState` / `restoreState`), keeping each tab's
 * state alive across switches. [NavigationBarItem] derives its tab semantics — including the
 * selected state — from the `selected` flag computed against the current destination.
 */
@Composable
private fun StudoraRoot() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                TabDestination.entries.forEach { tab ->
                    val label = stringResource(tab.labelRes)
                    NavigationBarItem(
                        selected =
                            currentDestination?.hierarchy?.any { it.route == tab.route.route } == true,
                        onClick = { navController.navigateToTab(tab.route) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        StudoraNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/** The three top-level tabs, pairing each [Route] with its label resource and icon. */
private enum class TabDestination(
    val route: Route,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    TIMER(Route.Timer, R.string.nav_timer, HomeIcon),
    HISTORY(Route.History, R.string.nav_history, DateRangeIcon),
    SETTINGS(Route.Settings, R.string.nav_settings, SettingsIcon),
}

/**
 * Selects [route] as a top-level tab: pops back to the graph start, reuses an existing entry
 * instead of stacking duplicates, and restores the tab's saved UI state.
 */
private fun NavHostController.navigateToTab(route: Route) {
    navigate(route.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
