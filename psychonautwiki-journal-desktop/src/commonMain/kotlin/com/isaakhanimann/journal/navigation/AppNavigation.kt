package com.isaakhanimann.journal.navigation

import androidx.compose.runtime.*
import com.isaakhanimann.journal.ui.screens.*
import com.isaakhanimann.journal.ui.screens.analytics.AnalyticsScreen
import com.isaakhanimann.journal.ui.screens.ai.AIAssistantScreen
import com.isaakhanimann.journal.ui.screens.gamification.GamificationScreen

@Composable
fun AppNavigation(navController: DesktopNavigationController? = null) {
    val actualNavController = navController ?: remember { DesktopNavigationController() }
    
    when (val screen = actualNavController.currentScreen) {
        is Screen.Dashboard -> DashboardScreen(navController = actualNavController)
        is Screen.Experiences -> ExperiencesScreen(navController = actualNavController) 
        is Screen.Substances -> SubstancesScreen(navController = actualNavController)
        is Screen.Settings -> SettingsScreen(navController = actualNavController)
        is Screen.Analytics -> AnalyticsScreen()
        is Screen.AIAssistant -> AIAssistantScreen()
        is Screen.Gamification -> GamificationScreen()
        is Screen.ExperienceEditor -> {
            val experienceId = screen.experienceId?.toIntOrNull()
            ExperienceEditorScreen(
                navigationController = actualNavController,
                experienceId = experienceId
            )
        }
        is Screen.IngestionEditor -> {
            val experienceId = screen.experienceId.toIntOrNull() ?: 0
            val ingestionId = screen.ingestionId?.toIntOrNull()
            IngestionEditorScreen(
                navigationController = actualNavController,
                experienceId = experienceId,
                ingestionId = ingestionId
            )
        }
        is Screen.ExperienceTimeline -> {
            val experienceId = screen.experienceId.toIntOrNull() ?: 0
            ExperienceTimelineScreen(
                navigationController = actualNavController,
                experienceId = experienceId
            )
        }
    }
}