package com.isaakhanimann.journal.ui.viewmodel

import com.isaakhanimann.journal.data.experience.ExperienceTracker
import com.isaakhanimann.journal.data.experience.ExperienceSummary
import com.isaakhanimann.journal.data.model.Experience
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExperiencesViewModel(
    private val experienceTracker: ExperienceTracker
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(ExperiencesUiState())
    val uiState: StateFlow<ExperiencesUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()
    
    init {
        loadExperiences()
    }
    
    private fun loadExperiences() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                combine(
                    experienceTracker.getAllExperiencesWithSummary(),
                    _searchQuery,
                    _showFavoritesOnly
                ) { experiences, query, favoritesOnly ->
                    var filteredExperiences = experiences
                    
                    // Filter by favorites if enabled
                    if (favoritesOnly) {
                        filteredExperiences = filteredExperiences.filter { 
                            it.experience.isFavorite 
                        }
                    }
                    
                    // Filter by search query
                    if (query.isNotBlank()) {
                        filteredExperiences = filteredExperiences.filter { summary ->
                            summary.experience.title.contains(query, ignoreCase = true) ||
                            summary.experience.text.contains(query, ignoreCase = true)
                        }
                    }
                    
                    filteredExperiences
                }.collect { experiences ->
                    _uiState.value = ExperiencesUiState(
                        data = experiences,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load experiences"
                )
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun toggleFavoritesFilter() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
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
    
    fun deleteExperience(experienceId: Int) {
        viewModelScope.launch {
            try {
                experienceTracker.deleteExperience(experienceId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete experience: ${e.message}"
                )
            }
        }
    }
    
    fun refresh() {
        loadExperiences()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ExperiencesUiState(
    val data: List<ExperienceSummary>? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isSuccess: Boolean get() = data != null && error == null
    val isEmpty: Boolean get() = data?.isEmpty() == true
    val experienceCount: Int get() = data?.size ?: 0
}