package com.isaakhanimann.journal.ui.viewmodel

import com.isaakhanimann.journal.data.experience.ExperienceTracker
import com.isaakhanimann.journal.data.experience.ExperienceWithDetails
import com.isaakhanimann.journal.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class ExperienceEditorViewModel(
    private val experienceTracker: ExperienceTracker
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(ExperienceEditorUiState())
    val uiState: StateFlow<ExperienceEditorUiState> = _uiState.asStateFlow()
    
    private val _formState = MutableStateFlow(ExperienceFormState())
    val formState: StateFlow<ExperienceFormState> = _formState.asStateFlow()
    
    private var editingExperienceId: Int? = null
    
    fun initializeForEditing(experienceId: Int) {
        editingExperienceId = experienceId
        loadExperience(experienceId)
    }
    
    fun initializeForCreation() {
        editingExperienceId = null
        _formState.value = ExperienceFormState()
        _uiState.value = ExperienceEditorUiState(mode = EditorMode.CREATE)
    }
    
    private fun loadExperience(experienceId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                experienceTracker.getExperienceWithDetails(experienceId).collect { experienceWithDetails ->
                    if (experienceWithDetails != null) {
                        val experience = experienceWithDetails.experience
                        _formState.value = ExperienceFormState(
                            title = experience.title,
                            text = experience.text,
                            isFavorite = experience.isFavorite,
                            locationName = experience.location?.name ?: "",
                            locationLatitude = experience.location?.latitude?.toString() ?: "",
                            locationLongitude = experience.location?.longitude?.toString() ?: "",
                            sortDate = experience.sortDate
                        )
                        _uiState.value = ExperienceEditorUiState(
                            mode = EditorMode.EDIT,
                            experienceWithDetails = experienceWithDetails,
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Experience not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load experience"
                )
            }
        }
    }
    
    fun updateTitle(title: String) {
        _formState.value = _formState.value.copy(title = title)
        validateForm()
    }
    
    fun updateText(text: String) {
        _formState.value = _formState.value.copy(text = text)
    }
    
    fun toggleFavorite() {
        _formState.value = _formState.value.copy(isFavorite = !_formState.value.isFavorite)
    }
    
    fun updateLocationName(name: String) {
        _formState.value = _formState.value.copy(locationName = name)
    }
    
    fun updateLocationLatitude(latitude: String) {
        _formState.value = _formState.value.copy(locationLatitude = latitude)
    }
    
    fun updateLocationLongitude(longitude: String) {
        _formState.value = _formState.value.copy(locationLongitude = longitude)
    }
    
    fun updateSortDate(date: Instant) {
        _formState.value = _formState.value.copy(sortDate = date)
    }
    
    private fun validateForm() {
        val form = _formState.value
        val isValid = form.title.isNotBlank()
        
        _formState.value = form.copy(
            isValid = isValid,
            titleError = if (form.title.isBlank()) "Title is required" else null
        )
    }
    
    fun saveExperience() {
        validateForm()
        
        if (!_formState.value.isValid) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            
            try {
                val form = _formState.value
                val location = if (form.locationName.isNotBlank()) {
                    Location(
                        name = form.locationName,
                        latitude = form.locationLatitude.toDoubleOrNull(),
                        longitude = form.locationLongitude.toDoubleOrNull()
                    )
                } else null
                
                if (editingExperienceId != null) {
                    // Update existing experience
                    val currentExperience = _uiState.value.experienceWithDetails?.experience
                    if (currentExperience != null) {
                        val updatedExperience = currentExperience.copy(
                            title = form.title,
                            text = form.text,
                            isFavorite = form.isFavorite,
                            location = location
                        )
                        experienceTracker.updateExperience(updatedExperience)
                    }
                } else {
                    // Create new experience
                    val experienceId = experienceTracker.createNewExperience(
                        title = form.title,
                        text = form.text,
                        location = location,
                        sortDate = form.sortDate
                    )
                    editingExperienceId = experienceId.toInt()
                }
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save experience: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}

data class ExperienceEditorUiState(
    val mode: EditorMode = EditorMode.CREATE,
    val experienceWithDetails: ExperienceWithDetails? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
) {
    val isSuccess: Boolean get() = !isLoading && error == null
    val canSave: Boolean get() = !isLoading && !isSaving
}

data class ExperienceFormState(
    val title: String = "",
    val text: String = "",
    val isFavorite: Boolean = false,
    val locationName: String = "",
    val locationLatitude: String = "",
    val locationLongitude: String = "",
    val sortDate: Instant = Clock.System.now(),
    val isValid: Boolean = false,
    val titleError: String? = null
) {
    val hasLocation: Boolean get() = locationName.isNotBlank()
    val hasValidCoordinates: Boolean get() = 
        locationLatitude.toDoubleOrNull() != null && 
        locationLongitude.toDoubleOrNull() != null
}

enum class EditorMode {
    CREATE, EDIT
}