package com.isaakhanimann.journal.ui.viewmodel

import com.isaakhanimann.journal.data.experience.ExperienceTracker
import com.isaakhanimann.journal.data.experience.ExperienceWithDetails
import com.isaakhanimann.journal.data.experience.TimelineEvent
import com.isaakhanimann.journal.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours

class ExperienceTimelineViewModel(
    private val experienceTracker: ExperienceTracker
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()
    
    private val _ratingFormState = MutableStateFlow(RatingFormState())
    val ratingFormState: StateFlow<RatingFormState> = _ratingFormState.asStateFlow()
    
    private val _noteFormState = MutableStateFlow(NoteFormState())
    val noteFormState: StateFlow<NoteFormState> = _noteFormState.asStateFlow()
    
    private var experienceId: Int = 0
    
    fun loadTimeline(experienceId: Int) {
        this.experienceId = experienceId
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                experienceTracker.getExperienceWithDetails(experienceId).collect { experienceWithDetails ->
                    if (experienceWithDetails != null) {
                        val timelineData = processTimelineData(experienceWithDetails)
                        _uiState.value = TimelineUiState(
                            experienceWithDetails = experienceWithDetails,
                            timelineData = timelineData,
                            isLoading = false,
                            error = null
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
                    error = e.message ?: "Failed to load timeline"
                )
            }
        }
    }
    
    private fun processTimelineData(experienceWithDetails: ExperienceWithDetails): TimelineData {
        val events = experienceWithDetails.timelineEvents
        
        // Calculate onset times relative to first ingestion
        val firstIngestionTime = experienceWithDetails.ingestions.minOfOrNull { it.time }
        
        val processedEvents = if (firstIngestionTime != null) {
            events.map { event ->
                val relativeTime = event.time - firstIngestionTime
                ProcessedTimelineEvent(
                    event = event,
                    relativeTime = relativeTime,
                    formattedTime = formatRelativeTime(relativeTime)
                )
            }
        } else {
            events.map { event ->
                ProcessedTimelineEvent(
                    event = event,
                    relativeTime = kotlin.time.Duration.ZERO,
                    formattedTime = "T+0:00"
                )
            }
        }
        
        // Group events by phase
        val phases = groupEventsByPhase(processedEvents)
        
        return TimelineData(
            events = processedEvents,
            phases = phases,
            firstIngestionTime = firstIngestionTime,
            lastEventTime = events.maxOfOrNull { it.time },
            totalDuration = if (firstIngestionTime != null && events.isNotEmpty()) {
                events.maxOf { it.time } - firstIngestionTime
            } else {
                kotlin.time.Duration.ZERO
            }
        )
    }
    
    private fun formatRelativeTime(duration: kotlin.time.Duration): String {
        val hours = duration.inWholeHours
        val minutes = (duration.inWholeMinutes % 60)
        val sign = if (duration.isNegative()) "-" else "+"
        return "T${sign}${hours.toString().padStart(1, '0')}:${minutes.toString().padStart(2, '0')}"
    }
    
    private fun groupEventsByPhase(events: List<ProcessedTimelineEvent>): List<TimelinePhase> {
        val phases = mutableListOf<TimelinePhase>()
        
        // Define standard psychedelic phases
        phases.add(TimelinePhase("Onset", kotlin.time.Duration.ZERO, 1.hours))
        phases.add(TimelinePhase("Come-up", 1.hours, 2.hours))
        phases.add(TimelinePhase("Peak", 2.hours, 4.hours))
        phases.add(TimelinePhase("Plateau", 4.hours, 6.hours))
        phases.add(TimelinePhase("Come-down", 6.hours, 8.hours))
        phases.add(TimelinePhase("Afterglow", 8.hours, 12.hours))
        
        // Assign events to phases
        return phases.map { phase ->
            val phaseEvents = events.filter { event ->
                event.relativeTime >= phase.startTime && event.relativeTime < phase.endTime
            }
            phase.copy(events = phaseEvents)
        }
    }
    
    // Rating functionality
    fun updateRatingTime(time: Instant) {
        _ratingFormState.value = _ratingFormState.value.copy(time = time)
    }
    
    fun selectRatingOption(option: ShulginRatingOption) {
        _ratingFormState.value = _ratingFormState.value.copy(selectedOption = option)
    }
    
    fun addRating() {
        val form = _ratingFormState.value
        if (form.selectedOption == null) return
        
        viewModelScope.launch {
            try {
                experienceTracker.addRatingToExperience(
                    experienceId = experienceId,
                    rating = form.selectedOption,
                    time = form.time
                )
                
                // Reset form
                _ratingFormState.value = RatingFormState()
                
                _uiState.value = _uiState.value.copy(
                    showRatingDialog = false,
                    successMessage = "Rating added successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to add rating: ${e.message}"
                )
            }
        }
    }
    
    // Note functionality
    fun updateNoteTime(time: Instant) {
        _noteFormState.value = _noteFormState.value.copy(time = time)
    }
    
    fun updateNoteText(text: String) {
        _noteFormState.value = _noteFormState.value.copy(text = text)
    }
    
    fun updateNoteColor(color: AdaptiveColor) {
        _noteFormState.value = _noteFormState.value.copy(color = color)
    }
    
    fun toggleNoteInTimeline() {
        _noteFormState.value = _noteFormState.value.copy(
            isPartOfTimeline = !_noteFormState.value.isPartOfTimeline
        )
    }
    
    fun addNote() {
        val form = _noteFormState.value
        if (form.text.isBlank()) return
        
        viewModelScope.launch {
            try {
                experienceTracker.addNoteToExperience(
                    experienceId = experienceId,
                    noteText = form.text,
                    time = form.time,
                    color = form.color,
                    isPartOfTimeline = form.isPartOfTimeline
                )
                
                // Reset form
                _noteFormState.value = NoteFormState()
                
                _uiState.value = _uiState.value.copy(
                    showNoteDialog = false,
                    successMessage = "Note added successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to add note: ${e.message}"
                )
            }
        }
    }
    
    // UI state management
    fun showRatingDialog() {
        _uiState.value = _uiState.value.copy(showRatingDialog = true)
        _ratingFormState.value = RatingFormState(time = Clock.System.now())
    }
    
    fun hideRatingDialog() {
        _uiState.value = _uiState.value.copy(showRatingDialog = false)
        _ratingFormState.value = RatingFormState()
    }
    
    fun showNoteDialog() {
        _uiState.value = _uiState.value.copy(showNoteDialog = true)
        _noteFormState.value = NoteFormState(time = Clock.System.now())
    }
    
    fun hideNoteDialog() {
        _uiState.value = _uiState.value.copy(showNoteDialog = false)
        _noteFormState.value = NoteFormState()
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}

data class TimelineUiState(
    val experienceWithDetails: ExperienceWithDetails? = null,
    val timelineData: TimelineData? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showRatingDialog: Boolean = false,
    val showNoteDialog: Boolean = false
) {
    val hasTimeline: Boolean get() = timelineData?.events?.isNotEmpty() == true
}

data class TimelineData(
    val events: List<ProcessedTimelineEvent>,
    val phases: List<TimelinePhase>,
    val firstIngestionTime: Instant?,
    val lastEventTime: Instant?,
    val totalDuration: kotlin.time.Duration
) {
    val hasEvents: Boolean get() = events.isNotEmpty()
    val formattedDuration: String get() = formatDuration(totalDuration)
    
    private fun formatDuration(duration: kotlin.time.Duration): String {
        val hours = duration.inWholeHours
        val minutes = (duration.inWholeMinutes % 60)
        return "${hours}h ${minutes}m"
    }
}

data class ProcessedTimelineEvent(
    val event: TimelineEvent,
    val relativeTime: kotlin.time.Duration,
    val formattedTime: String
) {
    val eventType: String get() = when (event) {
        is TimelineEvent.IngestionEvent -> "Ingestion"
        is TimelineEvent.IngestionEndEvent -> "Ingestion End"
        is TimelineEvent.RatingEvent -> "Rating"
        is TimelineEvent.NoteEvent -> "Note"
    }
}

data class TimelinePhase(
    val name: String,
    val startTime: kotlin.time.Duration,
    val endTime: kotlin.time.Duration,
    val events: List<ProcessedTimelineEvent> = emptyList()
) {
    val hasEvents: Boolean get() = events.isNotEmpty()
    val eventCount: Int get() = events.size
}

data class RatingFormState(
    val time: Instant = Clock.System.now(),
    val selectedOption: ShulginRatingOption? = null
) {
    val isValid: Boolean get() = selectedOption != null
}

data class NoteFormState(
    val time: Instant = Clock.System.now(),
    val text: String = "",
    val color: AdaptiveColor = AdaptiveColor.BLUE,
    val isPartOfTimeline: Boolean = true
) {
    val isValid: Boolean get() = text.isNotBlank()
}