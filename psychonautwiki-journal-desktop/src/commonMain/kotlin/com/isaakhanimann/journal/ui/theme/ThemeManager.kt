package com.isaakhanimann.journal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.isaakhanimann.journal.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;
    
    companion object {
        fun fromString(value: String): ThemeMode = when (value.lowercase()) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }
    
    override fun toString(): String = when (this) {
        SYSTEM -> "system"
        LIGHT -> "light"
        DARK -> "dark"
    }
}

class ThemeManager(private val preferencesRepository: PreferencesRepository) {
    
    val themeMode: Flow<ThemeMode> = preferencesRepository.getThemeMode()
        .map { ThemeMode.fromString(it) }
    
    suspend fun setThemeMode(mode: ThemeMode) {
        preferencesRepository.setThemeMode(mode.toString())
    }
    
    @Composable
    fun shouldUseDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}