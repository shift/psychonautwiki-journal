package com.isaakhanimann.journal.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSizeClass {
    Compact,    // < 600dp (Mobile portrait)
    Medium,     // 600-839dp (Mobile landscape, tablet portrait)
    Expanded    // >= 840dp (Desktop, tablet landscape)
}

data class ResponsiveConfig(
    val windowSizeClass: WindowSizeClass,
    val windowWidthDp: Dp,
    val windowHeightDp: Dp,
    val isCompactHeight: Boolean,
    val columns: Int,
    val contentPadding: Dp,
    val cardSpacing: Dp
) {
    val isMobile: Boolean = windowSizeClass == WindowSizeClass.Compact
    val isTablet: Boolean = windowSizeClass == WindowSizeClass.Medium
    val isDesktop: Boolean = windowSizeClass == WindowSizeClass.Expanded
}

@Composable
fun calculateWindowSizeClass(width: Dp, height: Dp): ResponsiveConfig {
    val density = LocalDensity.current
    
    val windowSizeClass = when {
        width < 600.dp -> WindowSizeClass.Compact
        width < 840.dp -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }
    
    val isCompactHeight = height < 480.dp
    
    val columns = when (windowSizeClass) {
        WindowSizeClass.Compact -> if (isCompactHeight) 2 else 1
        WindowSizeClass.Medium -> if (isCompactHeight) 3 else 2
        WindowSizeClass.Expanded -> if (width > 1200.dp) 4 else 3
    }
    
    val contentPadding = when (windowSizeClass) {
        WindowSizeClass.Compact -> 16.dp
        WindowSizeClass.Medium -> 24.dp
        WindowSizeClass.Expanded -> 32.dp
    }
    
    val cardSpacing = when (windowSizeClass) {
        WindowSizeClass.Compact -> 8.dp
        WindowSizeClass.Medium -> 12.dp
        WindowSizeClass.Expanded -> 16.dp
    }
    
    return remember(width, height) {
        ResponsiveConfig(
            windowSizeClass = windowSizeClass,
            windowWidthDp = width,
            windowHeightDp = height,
            isCompactHeight = isCompactHeight,
            columns = columns,
            contentPadding = contentPadding,
            cardSpacing = cardSpacing
        )
    }
}