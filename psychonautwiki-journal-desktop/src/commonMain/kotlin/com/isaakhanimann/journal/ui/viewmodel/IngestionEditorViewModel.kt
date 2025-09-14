package com.isaakhanimann.journal.ui.viewmodel

import com.isaakhanimann.journal.data.experience.ExperienceTracker
import com.isaakhanimann.journal.data.model.*
import com.isaakhanimann.journal.data.repository.SubstanceRepository
import com.isaakhanimann.journal.data.repository.DraftManager
import com.isaakhanimann.journal.data.repository.IngestionDraft
import com.isaakhanimann.journal.data.substance.PsychonautWikiDatabase
import com.isaakhanimann.journal.data.substance.SubstanceInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class IngestionEditorViewModel(
    private val experienceTracker: ExperienceTracker,
    private val substanceRepository: SubstanceRepository,
    private val draftManager: DraftManager
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(IngestionEditorUiState())
    val uiState: StateFlow<IngestionEditorUiState> = _uiState.asStateFlow()
    
    private val _formState = MutableStateFlow(IngestionFormState())
    val formState: StateFlow<IngestionFormState> = _formState.asStateFlow()
    
    private val _substanceSearchQuery = MutableStateFlow("")
    val substanceSearchQuery: StateFlow<String> = _substanceSearchQuery.asStateFlow()
    
    private var experienceId: Int = 0
    private var editingIngestionId: Int? = null
    private var autoSaveJob: Job? = null
    private var formId: String = ""
    
    companion object {
        private const val AUTO_SAVE_DELAY_MS = 30_000L // 30 seconds
    }
    
    init {
        loadSubstanceSearchResults()
        startAutoSaveMonitoring()
    }
    
    private fun startAutoSaveMonitoring() {
        viewModelScope.launch {
            formState
                .debounce(5000) // Wait 5 seconds after user stops typing
                .collect { form ->
                    if (formId.isNotBlank() && form.hasSubstantialContent) {
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
        if (form.hasSubstantialContent) {
            val draft = IngestionDraft(
                substanceName = form.substanceName,
                dose = form.dose,
                units = form.units,
                administrationRoute = form.administrationRoute.name,
                time = form.time.toEpochMilliseconds(),
                endTime = form.endTime?.toEpochMilliseconds(),
                notes = form.notes,
                isDoseAnEstimate = form.isDoseAnEstimate,
                stomachFullness = form.stomachFullness?.name,
                consumerName = form.consumerName
            )
            
            try {
                draftManager.saveIngestionDraft(formId, draft)
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
        if (formId.isNotBlank() && editingIngestionId == null) {
            val draft = draftManager.getIngestionDraft(formId)
            if (draft != null && draft.hasSubstantialContent()) {
                _formState.value = IngestionFormState(
                    substanceName = draft.substanceName,
                    dose = draft.dose,
                    units = draft.units,
                    administrationRoute = AdministrationRoute.values().find { it.name == draft.administrationRoute } ?: AdministrationRoute.ORAL,
                    time = Instant.fromEpochMilliseconds(draft.time),
                    endTime = draft.endTime?.let { Instant.fromEpochMilliseconds(it) },
                    notes = draft.notes,
                    isDoseAnEstimate = draft.isDoseAnEstimate,
                    stomachFullness = draft.stomachFullness?.let { StomachFullness.values().find { sf -> sf.name == it } },
                    consumerName = draft.consumerName
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
            draftManager.clearIngestionDraft(formId)
            _uiState.value = _uiState.value.copy(hasDraft = false)
            initializeForCreation(experienceId) // Reset form
        }
    }
    
    fun initializeForCreation(experienceId: Int) {
        this.experienceId = experienceId
        editingIngestionId = null
        formId = "ingestion_new_${experienceId}_${Clock.System.now().toEpochMilliseconds()}"
        _formState.value = IngestionFormState()
        _uiState.value = IngestionEditorUiState(mode = IngestionEditorMode.CREATE)
        validateForm()
        
        // Load any existing draft
        viewModelScope.launch {
            loadDraftIfExists()
        }
    }
    
    fun initializeForEditing(experienceId: Int, ingestionId: Int) {
        this.experienceId = experienceId
        editingIngestionId = ingestionId
        formId = "ingestion_edit_${ingestionId}"
        loadIngestion(ingestionId)
    }
    
    private fun loadIngestion(ingestionId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                experienceTracker.getIngestionsByExperience(experienceId).collect { ingestions ->
                    val ingestion = ingestions.find { it.id == ingestionId }
                    if (ingestion != null) {
                        _formState.value = IngestionFormState(
                            substanceName = ingestion.substanceName,
                            dose = ingestion.dose?.toString() ?: "",
                            units = ingestion.units ?: "",
                            administrationRoute = ingestion.administrationRoute,
                            time = ingestion.time,
                            endTime = ingestion.endTime,
                            notes = ingestion.notes ?: "",
                            isDoseAnEstimate = ingestion.isDoseAnEstimate,
                            stomachFullness = ingestion.stomachFullness,
                            consumerName = ingestion.consumerName ?: ""
                        )
                        _uiState.value = IngestionEditorUiState(
                            mode = IngestionEditorMode.EDIT,
                            isLoading = false
                        )
                        validateForm()
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load ingestion"
                )
            }
        }
    }
    
    private fun loadSubstanceSearchResults() {
        viewModelScope.launch {
            _substanceSearchQuery.collect { query ->
                val substances = PsychonautWikiDatabase.searchSubstances(query)
                _uiState.value = _uiState.value.copy(substanceSearchResults = substances)
            }
        }
    }
    
    fun updateSubstanceSearchQuery(query: String) {
        _substanceSearchQuery.value = query
    }
    
    fun selectSubstance(substanceName: String) {
        _formState.value = _formState.value.copy(substanceName = substanceName)
        val substanceInfo = PsychonautWikiDatabase.getSubstanceByName(substanceName)
        _uiState.value = _uiState.value.copy(selectedSubstanceInfo = substanceInfo)
        validateForm()
        scheduleAutoSave()
    }
    
    fun updateDose(dose: String) {
        _formState.value = _formState.value.copy(dose = dose)
        calculateDoseClassification()
        validateForm()
        scheduleAutoSave()
    }
    
    fun updateUnits(units: String) {
        _formState.value = _formState.value.copy(units = units)
        calculateDoseClassification()
        scheduleAutoSave()
    }
    
    fun updateAdministrationRoute(route: AdministrationRoute) {
        _formState.value = _formState.value.copy(administrationRoute = route)
        calculateDoseClassification()
        scheduleAutoSave()
    }
    
    fun updateTime(time: Instant) {
        _formState.value = _formState.value.copy(time = time)
        scheduleAutoSave()
    }
    
    fun updateEndTime(endTime: Instant?) {
        _formState.value = _formState.value.copy(endTime = endTime)
        scheduleAutoSave()
    }
    
    fun updateNotes(notes: String) {
        _formState.value = _formState.value.copy(notes = notes)
        scheduleAutoSave()
    }
    
    fun toggleDoseEstimate() {
        _formState.value = _formState.value.copy(isDoseAnEstimate = !_formState.value.isDoseAnEstimate)
        scheduleAutoSave()
    }
    
    fun updateStomachFullness(fullness: StomachFullness?) {
        _formState.value = _formState.value.copy(stomachFullness = fullness)
        scheduleAutoSave()
    }
    
    fun updateConsumerName(name: String) {
        _formState.value = _formState.value.copy(consumerName = name)
        scheduleAutoSave()
    }
    
    private fun calculateDoseClassification() {
        val form = _formState.value
        val substanceInfo = _uiState.value.selectedSubstanceInfo
        val doseValue = form.dose.toDoubleOrNull()
        
        if (substanceInfo != null && doseValue != null) {
            val routeInfo = substanceInfo.roas.find { 
                it.name.equals(form.administrationRoute.displayName, ignoreCase = true) 
            }
            
            val doseClassification = routeInfo?.dose?.let { doseInfo ->
                when {
                    doseInfo.lightMin?.let { doseValue <= it } == true -> DoseClassification.LIGHT
                    doseInfo.commonMin?.let { doseValue >= it } == true && doseInfo.strongMin?.let { doseValue < it } == true -> DoseClassification.COMMON
                    doseInfo.strongMin?.let { doseValue >= it } == true && doseInfo.heavyMin?.let { doseValue < it } == true -> DoseClassification.STRONG
                    doseInfo.heavyMin?.let { doseValue >= it } == true -> DoseClassification.HEAVY
                    else -> DoseClassification.LIGHT
                }
            } ?: DoseClassification.UNKNOWN
            
            _uiState.value = _uiState.value.copy(doseClassification = doseClassification)
        } else {
            _uiState.value = _uiState.value.copy(doseClassification = null)
        }
    }
    
    private fun parseRange(rangeStr: String): ClosedFloatingPointRange<Double>? {
        return try {
            when {
                rangeStr.contains("-") -> {
                    val parts = rangeStr.split("-").map { it.trim().toDouble() }
                    if (parts.size == 2) parts[0]..parts[1] else null
                }
                rangeStr.contains("+") -> {
                    val value = rangeStr.replace("+", "").trim().toDouble()
                    value..Double.MAX_VALUE
                }
                else -> {
                    val value = rangeStr.trim().toDouble()
                    value..value
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun validateForm() {
        val form = _formState.value
        val isValid = form.substanceName.isNotBlank() && 
                     form.dose.isNotBlank() && 
                     form.dose.toDoubleOrNull() != null &&
                     form.dose.toDoubleOrNull()!! > 0
        
        _formState.value = form.copy(
            isValid = isValid,
            substanceNameError = if (form.substanceName.isBlank()) "Substance is required" else null,
            doseError = when {
                form.dose.isBlank() -> "Dose is required"
                form.dose.toDoubleOrNull() == null -> "Invalid dose format"
                form.dose.toDoubleOrNull()!! <= 0 -> "Dose must be greater than 0"
                else -> null
            }
        )
    }
    
    fun saveIngestion() {
        validateForm()
        
        if (!_formState.value.isValid) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            
            try {
                val form = _formState.value
                val doseValue = form.dose.toDoubleOrNull()!!
                
                if (editingIngestionId != null) {
                    // Update existing ingestion - would need repository method
                    // For now, this is a placeholder
                } else {
                    // Create new ingestion
                    experienceTracker.addIngestionToExperience(
                        experienceId = experienceId,
                        substanceName = form.substanceName,
                        dose = doseValue,
                        units = form.units.ifBlank { null },
                        administrationRoute = form.administrationRoute,
                        time = form.time,
                        notes = form.notes.ifBlank { null },
                        isDoseAnEstimate = form.isDoseAnEstimate,
                        stomachFullness = form.stomachFullness,
                        consumerName = form.consumerName.ifBlank { null }
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true
                )
                
                // Clear draft after successful save
                if (formId.isNotBlank()) {
                    draftManager.clearIngestionDraft(formId)
                    _uiState.value = _uiState.value.copy(
                        hasDraft = false,
                        autoSaveStatus = AutoSaveStatus.NONE
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save ingestion: ${e.message}"
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

data class IngestionEditorUiState(
    val mode: IngestionEditorMode = IngestionEditorMode.CREATE,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val substanceSearchResults: List<SubstanceInfo> = emptyList(),
    val selectedSubstanceInfo: SubstanceInfo? = null,
    val doseClassification: DoseClassification? = null,
    val hasDraft: Boolean = false,
    val draftLastSaved: Instant? = null,
    val autoSaveStatus: AutoSaveStatus = AutoSaveStatus.NONE,
    val lastAutoSave: Instant? = null
) {
    val canSave: Boolean get() = !isLoading && !isSaving
}

data class IngestionFormState(
    val substanceName: String = "",
    val dose: String = "",
    val units: String = "",
    val administrationRoute: AdministrationRoute = AdministrationRoute.ORAL,
    val time: Instant = Clock.System.now(),
    val endTime: Instant? = null,
    val notes: String = "",
    val isDoseAnEstimate: Boolean = false,
    val stomachFullness: StomachFullness? = null,
    val consumerName: String = "",
    val isValid: Boolean = false,
    val substanceNameError: String? = null,
    val doseError: String? = null
) {
    val hasEndTime: Boolean get() = endTime != null
    val hasNotes: Boolean get() = notes.isNotBlank()
    val isMultipleConsumers: Boolean get() = consumerName.isNotBlank()
    val hasSubstantialContent: Boolean get() = substanceName.trim().length > 2 || dose.isNotBlank()
}

enum class IngestionEditorMode {
    CREATE, EDIT
}

enum class DoseClassification(val displayName: String, val description: String) {
    THRESHOLD("Threshold", "Barely noticeable effects"),
    LIGHT("Light", "Mild effects, still functional"),
    COMMON("Common", "Normal recreational dose"),
    STRONG("Strong", "Intense effects, impaired function"),
    HEAVY("Heavy", "Very strong effects, high risk"),
    UNKNOWN("Unknown", "Dose range not available");
    
    val isRisky: Boolean get() = this in listOf(HEAVY)
}

enum class AutoSaveStatus {
    NONE, SAVING, SAVED, ERROR
}

enum class DoseClassification(val displayName: String, val description: String) {
    THRESHOLD("Threshold", "Barely noticeable effects"),
    LIGHT("Light", "Mild effects, still functional"),
    COMMON("Common", "Normal recreational dose"),
    STRONG("Strong", "Intense effects, impaired function"),
    HEAVY("Heavy", "Very strong effects, high risk"),
    UNKNOWN("Unknown", "Dose range not available");
    
    val isRisky: Boolean get() = this in listOf(HEAVY)
    val needsCaution: Boolean get() = this in listOf(STRONG, HEAVY)
}