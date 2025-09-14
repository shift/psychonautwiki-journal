package com.isaakhanimann.journal.ui.viewmodel

import com.isaakhanimann.journal.ui.theme.ThemeManager
import com.isaakhanimann.journal.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val themeManager: ThemeManager
) : BaseViewModel() {
    
    val themeMode: StateFlow<ThemeMode> = themeManager.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )
    
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeManager.setThemeMode(mode)
        }
    }
}