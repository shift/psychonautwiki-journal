package com.isaakhanimann.journal.ui.viewmodel

import com.isaakhanimann.journal.data.experience.ExperienceTracker
import com.isaakhanimann.journal.data.experience.ExperienceSummary
import com.isaakhanimann.journal.data.model.Experience
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val experienceTracker: ExperienceTracker
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDashboardData()
    }
    
    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                combine(
                    experienceTracker.getAllExperiencesWithSummary(),
                    experienceTracker.getFavoriteExperiences()
                ) { allExperiences, favoriteExperiences ->
                    DashboardData(
                        recentExperiences = allExperiences.take(5),
                        favoriteExperiences = favoriteExperiences.take(3),
                        totalExperiences = allExperiences.size,
                        experiencesThisMonth = allExperiences.filter { it.isRecent }.size
                    )
                }.collect { data ->
                    _uiState.value = DashboardUiState(
                        data = data,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    fun refresh() {
        loadDashboardData()
    }
    
    fun toggleExperienceFavorite(experienceId: Int) {
        viewModelScope.launch {
            try {
                experienceTracker.toggleExperienceFavorite(experienceId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update favorite: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class DashboardUiState(
    val data: DashboardData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isSuccess: Boolean get() = data != null && error == null
    val isEmpty: Boolean get() = data?.totalExperiences == 0
}

data class DashboardData(
    val recentExperiences: List<ExperienceSummary> = emptyList(),
    val favoriteExperiences: List<Experience> = emptyList(),
    val totalExperiences: Int = 0,
    val experiencesThisMonth: Int = 0
) {
    val hasExperiences: Boolean get() = totalExperiences > 0
    val hasRecentActivity: Boolean get() = experiencesThisMonth > 0
}