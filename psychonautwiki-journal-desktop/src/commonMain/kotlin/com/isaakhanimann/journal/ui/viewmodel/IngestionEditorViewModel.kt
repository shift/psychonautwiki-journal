package com.isaakhanimann.journal.ui.viewmodel

import com.isaakhanimann.journal.data.experience.ExperienceTracker
import com.isaakhanimann.journal.data.model.*
import com.isaakhanimann.journal.data.repository.SubstanceRepository
import com.isaakhanimann.journal.data.substance.PsychonautWikiDatabase
import com.isaakhanimann.journal.data.substance.SubstanceInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class IngestionEditorViewModel(
    private val experienceTracker: ExperienceTracker,
    private val substanceRepository: SubstanceRepository
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(IngestionEditorUiState())
    val uiState: StateFlow<IngestionEditorUiState> = _uiState.asStateFlow()
    
    private val _formState = MutableStateFlow(IngestionFormState())
    val formState: StateFlow<IngestionFormState> = _formState.asStateFlow()
    
    private val _substanceSearchQuery = MutableStateFlow("")
    val substanceSearchQuery: StateFlow<String> = _substanceSearchQuery.asStateFlow()
    
    private var experienceId: Int = 0
    private var editingIngestionId: Int? = null
    
    init {
        loadSubstanceSearchResults()
    }
    
    fun initializeForCreation(experienceId: Int) {
        this.experienceId = experienceId
        editingIngestionId = null
        _formState.value = IngestionFormState()
        _uiState.value = IngestionEditorUiState(mode = IngestionEditorMode.CREATE)
        validateForm()
    }
    
    fun initializeForEditing(experienceId: Int, ingestionId: Int) {
        this.experienceId = experienceId
        editingIngestionId = ingestionId
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
    }
    
    fun updateDose(dose: String) {
        _formState.value = _formState.value.copy(dose = dose)
        calculateDoseClassification()
        validateForm()
    }
    
    fun updateUnits(units: String) {
        _formState.value = _formState.value.copy(units = units)
        calculateDoseClassification()
    }
    
    fun updateAdministrationRoute(route: AdministrationRoute) {
        _formState.value = _formState.value.copy(administrationRoute = route)
        calculateDoseClassification()
    }
    
    fun updateTime(time: Instant) {
        _formState.value = _formState.value.copy(time = time)
    }
    
    fun updateEndTime(endTime: Instant?) {
        _formState.value = _formState.value.copy(endTime = endTime)
    }
    
    fun updateNotes(notes: String) {
        _formState.value = _formState.value.copy(notes = notes)
    }
    
    fun toggleDoseEstimate() {
        _formState.value = _formState.value.copy(isDoseAnEstimate = !_formState.value.isDoseAnEstimate)
    }
    
    fun updateStomachFullness(fullness: StomachFullness?) {
        _formState.value = _formState.value.copy(stomachFullness = fullness)
    }
    
    fun updateConsumerName(name: String) {
        _formState.value = _formState.value.copy(consumerName = name)
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
    val doseClassification: DoseClassification? = null
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
    val needsCaution: Boolean get() = this in listOf(STRONG, HEAVY)
}