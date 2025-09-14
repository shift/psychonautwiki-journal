package com.isaakhanimann.journal.ui.viewmodel

import com.isaakhanimann.journal.data.experience.ExperienceTracker
import com.isaakhanimann.journal.data.experience.ExperienceWithDetails
import com.isaakhanimann.journal.data.model.*
import com.isaakhanimann.journal.data.repository.DraftManager
import com.isaakhanimann.journal.data.repository.ExperienceDraft
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class ExperienceEditorViewModel(
    private val experienceTracker: ExperienceTracker,
    private val draftManager: DraftManager
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(ExperienceEditorUiState())
    val uiState: StateFlow<ExperienceEditorUiState> = _uiState.asStateFlow()
    
    private val _formState = MutableStateFlow(ExperienceFormState())
    val formState: StateFlow<ExperienceFormState> = _formState.asStateFlow()
    
    private var editingExperienceId: Int? = null
    private var autoSaveJob: Job? = null
    private var formId: String = ""
    
    companion object {
        private const val AUTO_SAVE_DELAY_MS = 30_000L // 30 seconds
    }
    
    init {
        // Start monitoring form changes for auto-save
        startAutoSaveMonitoring()
    }
    
    private fun startAutoSaveMonitoring() {
        viewModelScope.launch {
            formState
                .debounce(5000) // Wait 5 seconds after user stops typing
                .collect { form ->
                    if (formId.isNotBlank() && form.hasSubstantialContent()) {
                        autoSaveDraft()
                    }
                }
        }
    }
    
    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MS)
            autoSaveDraft()
        }
    }
    
    private suspend fun autoSaveDraft() {
        val form = _formState.value
        if (form.hasSubstantialContent()) {
            val draft = ExperienceDraft(
                title = form.title,
                text = form.text,
                isFavorite = form.isFavorite,
                locationName = form.locationName,
                locationLatitude = form.locationLatitude,
                locationLongitude = form.locationLongitude,
                sortDate = form.sortDate.toEpochMilliseconds()
            )
            
            try {
                draftManager.saveExperienceDraft(formId, draft)
                _uiState.value = _uiState.value.copy(
                    autoSaveStatus = AutoSaveStatus.SAVED,
                    lastAutoSave = Clock.System.now()
                )
                
                // Clear the saved status after 3 seconds
                viewModelScope.launch {
                    delay(3000)
                    if (_uiState.value.autoSaveStatus == AutoSaveStatus.SAVED) {
                        _uiState.value = _uiState.value.copy(autoSaveStatus = AutoSaveStatus.NONE)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(autoSaveStatus = AutoSaveStatus.ERROR)
            }
        }
    }
    
    private suspend fun loadDraftIfExists() {
        if (formId.isNotBlank() && editingExperienceId == null) {
            val draft = draftManager.getExperienceDraft(formId)
            if (draft != null && draft.hasSubstantialContent()) {
                _formState.value = ExperienceFormState(
                    title = draft.title,
                    text = draft.text,
                    isFavorite = draft.isFavorite,
                    locationName = draft.locationName,
                    locationLatitude = draft.locationLatitude,
                    locationLongitude = draft.locationLongitude,
                    sortDate = Instant.fromEpochMilliseconds(draft.sortDate)
                )
                _uiState.value = _uiState.value.copy(
                    hasDraft = true,
                    draftLastSaved = Instant.fromEpochMilliseconds(draft.lastSaved)
                )
                validateForm()
            }
        }
    }
    
    fun acceptDraft() {
        _uiState.value = _uiState.value.copy(hasDraft = false)
    }
    
    fun discardDraft() {
        viewModelScope.launch {
            draftManager.clearExperienceDraft(formId)
            _uiState.value = _uiState.value.copy(hasDraft = false)
            initializeForCreation() // Reset form
        }
    }
    
    fun initializeForEditing(experienceId: Int) {
        editingExperienceId = experienceId
        formId = "experience_$experienceId"
        loadExperience(experienceId)
    }
    
    fun initializeForCreation() {
        editingExperienceId = null
        formId = "experience_new_${Clock.System.now().toEpochMilliseconds()}"
        _formState.value = ExperienceFormState()
        _uiState.value = ExperienceEditorUiState(mode = EditorMode.CREATE)
        validateForm()
        
        // Load any existing draft
        viewModelScope.launch {
            loadDraftIfExists()
        }
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
                        validateForm()
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
        scheduleAutoSave()
    }
    
    fun updateText(text: String) {
        _formState.value = _formState.value.copy(text = text)
        scheduleAutoSave()
    }
    
    fun toggleFavorite() {
        _formState.value = _formState.value.copy(isFavorite = !_formState.value.isFavorite)
    }
    
    fun updateLocationName(name: String) {
        _formState.value = _formState.value.copy(locationName = name)
        scheduleAutoSave()
    }
    
    fun updateLocationLatitude(latitude: String) {
        _formState.value = _formState.value.copy(locationLatitude = latitude)
        scheduleAutoSave()
    }
    
    fun updateLocationLongitude(longitude: String) {
        _formState.value = _formState.value.copy(locationLongitude = longitude)
        scheduleAutoSave()
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
                
                // Clear draft after successful save
                if (formId.isNotBlank()) {
                    draftManager.clearExperienceDraft(formId)
                    _uiState.value = _uiState.value.copy(
                        hasDraft = false,
                        autoSaveStatus = AutoSaveStatus.NONE
                    )
                }
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
    val error: String? = null,
    val hasDraft: Boolean = false,
    val draftLastSaved: Instant? = null,
    val autoSaveStatus: AutoSaveStatus = AutoSaveStatus.NONE,
    val lastAutoSave: Instant? = null
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
    val hasSubstantialContent: Boolean get() = title.trim().length > 3 || text.trim().length > 10
}

enum class EditorMode {
    CREATE, EDIT
}

enum class AutoSaveStatus {
    NONE, SAVING, SAVED, ERROR
}