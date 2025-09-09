package com.isaakhanimann.journal.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ResponsiveContent(
    config: ResponsiveConfig,
    modifier: Modifier = Modifier,
    content: @Composable (ResponsiveConfig) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(config.contentPadding)
    ) {
        content(config)
    }
}

@Composable
fun ResponsiveGrid(
    config: ResponsiveConfig,
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(config.columns),
        modifier = modifier,
        contentPadding = PaddingValues(config.cardSpacing),
        horizontalArrangement = Arrangement.spacedBy(config.cardSpacing),
        verticalArrangement = Arrangement.spacedBy(config.cardSpacing),
        content = content
    )
}

@Composable
fun ResponsiveCard(
    config: ResponsiveConfig,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(
                when (config.windowSizeClass) {
                    WindowSizeClass.Compact -> 12.dp
                    WindowSizeClass.Medium -> 16.dp
                    WindowSizeClass.Expanded -> 20.dp
                }
            ),
            content = content
        )
    }
}

@Composable
fun ResponsiveTwoPane(
    config: ResponsiveConfig,
    leftPane: @Composable () -> Unit,
    rightPane: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    when (config.windowSizeClass) {
        WindowSizeClass.Compact -> {
            Column(modifier = modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) { leftPane() }
                Spacer(modifier = Modifier.height(config.cardSpacing))
                Box(modifier = Modifier.weight(1f)) { rightPane() }
            }
        }
        WindowSizeClass.Medium, WindowSizeClass.Expanded -> {
            Row(modifier = modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) { leftPane() }
                Spacer(modifier = Modifier.width(config.cardSpacing))
                Box(modifier = Modifier.weight(1f)) { rightPane() }
            }
        }
    }
}