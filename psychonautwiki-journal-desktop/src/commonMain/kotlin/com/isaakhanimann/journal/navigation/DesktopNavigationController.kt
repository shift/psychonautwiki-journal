package com.isaakhanimann.journal.navigation

import androidx.compose.runtime.*

class DesktopNavigationController {
    private var _currentScreen by mutableStateOf<Screen>(Screen.Dashboard)
    val currentScreen: Screen get() = _currentScreen
    
    fun navigate(screen: Screen) {
        _currentScreen = screen
    }
    
    fun navigateToDashboard() {
        _currentScreen = Screen.Dashboard
    }
    
    fun navigateToExperiences() {
        _currentScreen = Screen.Experiences
    }
    
    fun navigateToSubstances() {
        _currentScreen = Screen.Substances
    }
    
    fun navigateToSettings() {
        _currentScreen = Screen.Settings
    }
    
    fun navigateToAnalytics() {
        _currentScreen = Screen.Analytics
    }
    
    fun navigateToAIAssistant() {
        _currentScreen = Screen.AIAssistant
    }
    
    fun navigateToExperienceEditor(experienceId: Int? = null) {
        _currentScreen = Screen.ExperienceEditor(experienceId?.toString())
    }
    
    fun navigateToIngestionEditor(experienceId: Int, ingestionId: Int? = null) {
        _currentScreen = Screen.IngestionEditor(experienceId.toString(), ingestionId?.toString())
    }
    
    fun navigateToExperienceTimeline(experienceId: Int) {
        _currentScreen = Screen.ExperienceTimeline(experienceId.toString())
    }
    
    fun popBackStack() {
        _currentScreen = Screen.Dashboard
    }
}