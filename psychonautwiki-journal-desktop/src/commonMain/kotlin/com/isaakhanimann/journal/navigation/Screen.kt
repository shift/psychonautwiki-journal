package com.isaakhanimann.journal.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Experiences : Screen("experiences")  
    object Substances : Screen("substances")
    object Settings : Screen("settings")
    data class ExperienceEditor(val experienceId: String? = null) : Screen("experience_editor/${experienceId ?: "new"}")
    data class IngestionEditor(val experienceId: String, val ingestionId: String? = null) : Screen("ingestion_editor/$experienceId/${ingestionId ?: "new"}")
    data class ExperienceTimeline(val experienceId: String) : Screen("experience_timeline/$experienceId")
}