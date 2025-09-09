package com.isaakhanimann.journal.ui.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

// Desktop configuration class to replace Android's LocalConfiguration
data class DesktopConfiguration(
    val screenWidthDp: Int,
    val screenHeightDp: Int
) {
    companion object {
        val Default = DesktopConfiguration(
            screenWidthDp = 1200,
            screenHeightDp = 800
        )
    }
}

val LocalConfiguration = compositionLocalOf { DesktopConfiguration.Default }