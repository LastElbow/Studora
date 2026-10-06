package com.bustedelbow.studora.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Inlined Material icons.
 *
 * Material3 no longer ships `material-icons-core`, and the app deliberately adds no dependency for
 * a handful of glyphs, so each icon is a 24dp [ImageVector] built from SVG path data.
 */
internal fun materialIcon(name: String, pathData: String): ImageVector =
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
internal val HomeIcon: ImageVector =
    materialIcon(name = "Home", pathData = "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z")

/** Filled "date_range" (calendar) glyph, used for the History tab. */
internal val DateRangeIcon: ImageVector =
    materialIcon(
        name = "DateRange",
        pathData =
            "M9 11H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2zm2-7h-1V2h-2v2H8V2H6v2H5c-1.11 " +
                "0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 " +
                "16H5V9h14v11z",
    )

/** Filled "settings" (gear) glyph, used for the Settings tab. */
internal val SettingsIcon: ImageVector =
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

/** Filled "chevron_left" glyph, used to step the heatmap to an earlier window. */
internal val ChevronLeftIcon: ImageVector =
    materialIcon(
        name = "ChevronLeft",
        pathData = "M15.41 7.41 14 6l-6 6 6 6 1.41-1.41L10.83 12z",
    )

/** Filled "chevron_right" glyph, used to step the heatmap back toward the current window. */
internal val ChevronRightIcon: ImageVector =
    materialIcon(
        name = "ChevronRight",
        pathData = "M10 6 8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z",
    )
