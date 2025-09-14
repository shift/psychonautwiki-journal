package com.isaakhanimann.journal.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.isaakhanimann.journal.navigation.AppNavigation
import com.isaakhanimann.journal.navigation.DesktopNavigationController
import com.isaakhanimann.journal.ui.theme.JournalTheme
import com.isaakhanimann.journal.ui.theme.ThemeManager
import com.isaakhanimann.journal.data.repository.PreferencesRepository
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.compose.getKoin

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
        onKeyEvent = { keyEvent ->
            handleGlobalKeyEvents(keyEvent, ::exitApplication)
        }
    ) {
        val koin = getKoin()
        val themeManager = remember { koin.get<ThemeManager>() }
        val themeMode by themeManager.themeMode.collectAsState(initial = com.isaakhanimann.journal.ui.theme.ThemeMode.SYSTEM)
        val isDarkTheme = themeManager.shouldUseDarkTheme(themeMode)
        
        JournalTheme(darkTheme = isDarkTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val focusRequester = remember { FocusRequester() }
                val navController = remember { DesktopNavigationController() }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .onKeyEvent { keyEvent ->
                            handleAppKeyEvents(keyEvent, navController)
                        }
                ) {
                    AppNavigation(navController = navController)
                }
                
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }
    }
}

private fun handleGlobalKeyEvents(keyEvent: KeyEvent, exitApplication: () -> Unit): Boolean {
    if (keyEvent.type == KeyEventType.KeyDown) {
        val isCtrlPressed = keyEvent.isCtrlPressed
        
        when (keyEvent.key) {
            Key.Q -> {
                if (isCtrlPressed) {
                    exitApplication()
                    return true
                }
            }
        }
    }
    return false
}

private fun handleAppKeyEvents(keyEvent: KeyEvent, navController: DesktopNavigationController): Boolean {
    if (keyEvent.type == KeyEventType.KeyDown) {
        val isCtrlPressed = keyEvent.isCtrlPressed
        
        when (keyEvent.key) {
            Key.N -> {
                if (isCtrlPressed) {
                    // Handle Ctrl+N for new experience
                    navController.navigateToExperienceEditor()
                    return true
                }
            }
            Key.S -> {
                if (isCtrlPressed) {
                    // Handle Ctrl+S for save
                    // This will be handled by individual forms
                    return true
                }
            }
        }
    }
    return false
}