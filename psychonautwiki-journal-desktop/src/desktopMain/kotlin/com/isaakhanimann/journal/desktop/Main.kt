package com.isaakhanimann.journal.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.isaakhanimann.journal.navigation.AppNavigation
import com.isaakhanimann.journal.ui.theme.JournalTheme
import org.koin.core.context.startKoin

fun main() = application {
    
    // Initialize Koin dependency injection
    startKoin {
        modules(appModule)
    }
    
    val windowState = rememberWindowState(
        placement = WindowPlacement.Floating,
        width = 1200.dp,
        height = 800.dp
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "PsychonautWiki Journal",
        state = windowState,
        resizable = true,
    ) {
        JournalTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AppNavigation()
            }
        }
    }
}