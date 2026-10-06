package com.bustedelbow.studora.ui.navigation

/**
 * The app's top-level destinations (ADR-0003).
 *
 * Route objects are sealed so the set of tabs is closed and each destination owns exactly one
 * stable route string; typos become compile errors instead of silent runtime crashes (CSV rule 17).
 */
sealed class Route(val route: String) {
    data object Timer : Route("timer")

    data object History : Route("history")

    data object Settings : Route("settings")
}
