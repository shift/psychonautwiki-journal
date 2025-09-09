package com.isaakhanimann.journal.navigation

import androidx.compose.runtime.*
import com.isaakhanimann.journal.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = remember { DesktopNavigationController() }
    
    when (val screen = navController.currentScreen) {
        is Screen.Dashboard -> DashboardScreen(navController = navController)
        is Screen.Experiences -> ExperiencesScreen(navController = navController) 
        is Screen.Substances -> SubstancesScreen(navController = navController)
        is Screen.Settings -> SettingsScreen(navController = navController)
        is Screen.ExperienceEditor -> {
            val experienceId = screen.experienceId?.toIntOrNull()
            ExperienceEditorScreen(
                navigationController = navController,
                experienceId = experienceId
            )
        }
        is Screen.IngestionEditor -> {
            val experienceId = screen.experienceId.toIntOrNull() ?: 0
            val ingestionId = screen.ingestionId?.toIntOrNull()
            IngestionEditorScreen(
                navigationController = navController,
                experienceId = experienceId,
                ingestionId = ingestionId
            )
        }
        is Screen.ExperienceTimeline -> {
            val experienceId = screen.experienceId.toIntOrNull() ?: 0
            ExperienceTimelineScreen(
                navigationController = navController,
                experienceId = experienceId
            )
        }
    }
}