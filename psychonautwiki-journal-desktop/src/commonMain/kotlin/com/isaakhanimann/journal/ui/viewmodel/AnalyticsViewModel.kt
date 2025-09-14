package com.isaakhanimann.journal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isaakhanimann.journal.data.repository.ExperienceRepository
import com.isaakhanimann.journal.plugin.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

enum class AnalyticsTimeRange(val displayName: String, val days: Int?) {
    LAST_WEEK("Last Week", 7),
    LAST_MONTH("Last Month", 30),
    LAST_3_MONTHS("Last 3 Months", 90),
    LAST_6_MONTHS("Last 6 Months", 180),
    LAST_YEAR("Last Year", 365),
    ALL_TIME("All Time", null)
}

class AnalyticsViewModel(
    private val experienceRepository: ExperienceRepository,
    private val pluginManager: PluginManager
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedTimeRange = MutableStateFlow(AnalyticsTimeRange.LAST_3_MONTHS)
    val selectedTimeRange: StateFlow<AnalyticsTimeRange> = _selectedTimeRange.asStateFlow()
    
    private val _analyticsResults = MutableStateFlow<List<AnalyticsResult>>(emptyList())
    val analyticsResults: StateFlow<List<AnalyticsResult>> = _analyticsResults.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        refreshAnalytics()
    }
    
    fun setTimeRange(range: AnalyticsTimeRange) {
        _selectedTimeRange.value = range
        refreshAnalytics()
    }
    
    fun refreshAnalytics() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val experiences = experienceRepository.getAllExperiences().first()
                val filteredExperiences = filterExperiencesByTimeRange(experiences, _selectedTimeRange.value)
                val substances = emptyList() // Simplified for now
                
                val analyticsContext = AnalyticsContext(
                    experiences = filteredExperiences,
                    substances = substances,
                    timeRange = getTimeRange(_selectedTimeRange.value)
                )
                
                val results = pluginManager.executeAnalytics(analyticsContext)
                _analyticsResults.value = results
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
                _analyticsResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun filterExperiencesByTimeRange(
        experiences: List<Experience>, 
        timeRange: AnalyticsTimeRange
    ): List<Experience> {
        if (timeRange.days == null) return experiences
        
        val cutoffDate = Clock.System.now().minus(timeRange.days, DateTimeUnit.DAY)
        return experiences.filter { experience ->
            experience.date?.let { date -> date >= cutoffDate } ?: false
        }
    }
    
    private fun getTimeRange(range: AnalyticsTimeRange): TimeRange? {
        return range.days?.let { days ->
            val end = Clock.System.now()
            val start = end.minus(days, DateTimeUnit.DAY)
            TimeRange(start, end)
        }
    }
    
    fun dismissError() {
        _error.value = null
    }
}