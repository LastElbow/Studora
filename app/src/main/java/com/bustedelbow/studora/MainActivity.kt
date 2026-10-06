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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bustedelbow.studora.ui.navigation.Route
import com.bustedelbow.studora.ui.navigation.StudoraNavHost
import com.bustedelbow.studora.ui.theme.StudoraTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single entry point. Hosts the Timer | History bottom-tab scaffold (ADR-0003). */
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

/**
 * Builds a 24dp [ImageVector] from SVG path data.
 *
 * The icons are inlined here rather than pulled from `material-icons-core`: Material3 no longer
 * ships that artifact, and this slice deliberately adds no dependency beyond navigation-compose.
 */
private fun materialIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Black),
    ).build()

/** Filled "home" glyph, used for the Timer tab. */
private val HomeIcon: ImageVector =
    materialIcon(name = "Home", pathData = "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z")

/** Filled "date_range" (calendar) glyph, used for the History tab. */
private val DateRangeIcon: ImageVector =
    materialIcon(
        name = "DateRange",
        pathData =
            "M9 11H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2zm2-7h-1V2h-2v2H8V2H6v2H5c-1.11 " +
                "0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 " +
                "16H5V9h14v11z",
    )

/** Filled "settings" (gear) glyph, used for the Settings tab. */
private val SettingsIcon: ImageVector =
    materialIcon(
        name = "Settings",
        pathData =
            "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41" +
                ".12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94" +
                "l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59" +
                ".24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47" +
                ".12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41" +
                "-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c" +
                ".05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62" +
                "-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58z" +
                "M12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6" +
                "-3.6 3.6z",
    )
