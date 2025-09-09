package com.isaakhanimann.journal.data.experience

import com.isaakhanimann.journal.data.model.*
import com.isaakhanimann.journal.data.repository.ExperienceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.take
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class ExperienceWithDetails(
    val experience: Experience,
    val ingestions: List<Ingestion> = emptyList(),
    val ratings: List<ShulginRating> = emptyList(),
    val notes: List<TimedNote> = emptyList()
) {
    val hasIngestions: Boolean get() = ingestions.isNotEmpty()
    val hasRatings: Boolean get() = ratings.isNotEmpty()
    val hasNotes: Boolean get() = notes.isNotEmpty()
    
    val timelineEvents: List<TimelineEvent>
        get() {
            val events = mutableListOf<TimelineEvent>()
            
            // Add ingestions as timeline events
            ingestions.forEach { ingestion ->
                events.add(
                    TimelineEvent.IngestionEvent(
                        time = ingestion.time,
                        ingestion = ingestion
                    )
                )
                
                // Add end time if available
                ingestion.endTime?.let { endTime ->
                    events.add(
                        TimelineEvent.IngestionEndEvent(
                            time = endTime,
                            ingestion = ingestion
                        )
                    )
                }
            }
            
            // Add ratings as timeline events
            ratings.forEach { rating ->
                rating.time?.let { time ->
                    events.add(
                        TimelineEvent.RatingEvent(
                            time = time,
                            rating = rating
                        )
                    )
                }
            }
            
            // Add notes marked for timeline
            notes.filter { it.isPartOfTimeline }.forEach { note ->
                events.add(
                    TimelineEvent.NoteEvent(
                        time = note.time,
                        note = note
                    )
                )
            }
            
            return events.sortedBy { it.time }
        }
}

sealed class TimelineEvent {
    abstract val time: Instant
    
    data class IngestionEvent(
        override val time: Instant,
        val ingestion: Ingestion
    ) : TimelineEvent()
    
    data class IngestionEndEvent(
        override val time: Instant,
        val ingestion: Ingestion
    ) : TimelineEvent()
    
    data class RatingEvent(
        override val time: Instant,
        val rating: ShulginRating
    ) : TimelineEvent()
    
    data class NoteEvent(
        override val time: Instant,
        val note: TimedNote
    ) : TimelineEvent()
}

class ExperienceTracker(
    private val experienceRepository: ExperienceRepository
) {
    
    fun getExperienceWithDetails(experienceId: Int): Flow<ExperienceWithDetails?> {
        return combine(
            experienceRepository.getExperienceById(experienceId),
            experienceRepository.getIngestionsByExperience(experienceId),
            experienceRepository.getShulginRatingsByExperience(experienceId),
            experienceRepository.getTimedNotesByExperience(experienceId)
        ) { experience, ingestions, ratings, notes ->
            experience?.let {
                ExperienceWithDetails(
                    experience = it,
                    ingestions = ingestions,
                    ratings = ratings,
                    notes = notes
                )
            }
        }
    }
    
    fun getAllExperiencesWithSummary(): Flow<List<ExperienceSummary>> {
        return experienceRepository.getAllExperiences().combine(
            experienceRepository.getAllIngestions()
        ) { experiences, allIngestions ->
            experiences.map { experience ->
                val experienceIngestions = allIngestions.filter { it.experienceId == experience.id }
                ExperienceSummary(
                    experience = experience,
                    substanceCount = experienceIngestions.map { it.substanceName }.distinct().size,
                    ingestionCount = experienceIngestions.size,
                    lastActivity = experienceIngestions.maxOfOrNull { it.time } ?: experience.creationDate
                )
            }
        }
    }
    
    suspend fun createNewExperience(
        title: String,
        text: String = "",
        location: Location? = null,
        sortDate: Instant = Clock.System.now()
    ): Long {
        val experience = Experience(
            title = title,
            text = text,
            creationDate = Clock.System.now(),
            sortDate = sortDate,
            location = location
        )
        return experienceRepository.insertExperience(experience)
    }
    
    suspend fun addIngestionToExperience(
        experienceId: Int,
        substanceName: String,
        dose: Double?,
        units: String?,
        administrationRoute: AdministrationRoute,
        time: Instant = Clock.System.now(),
        notes: String? = null,
        isDoseAnEstimate: Boolean = false,
        stomachFullness: StomachFullness? = null,
        consumerName: String? = null
    ): Long {
        val ingestion = Ingestion(
            substanceName = substanceName,
            time = time,
            creationDate = Clock.System.now(),
            administrationRoute = administrationRoute,
            dose = dose,
            isDoseAnEstimate = isDoseAnEstimate,
            units = units,
            experienceId = experienceId,
            notes = notes,
            stomachFullness = stomachFullness,
            consumerName = consumerName
        )
        return experienceRepository.insertIngestion(ingestion)
    }
    
    suspend fun addRatingToExperience(
        experienceId: Int,
        rating: ShulginRatingOption,
        time: Instant = Clock.System.now()
    ): Long {
        val shulginRating = ShulginRating(
            time = time,
            creationDate = Clock.System.now(),
            option = rating,
            experienceId = experienceId
        )
        return experienceRepository.insertShulginRating(shulginRating)
    }
    
    suspend fun addNoteToExperience(
        experienceId: Int,
        noteText: String,
        time: Instant = Clock.System.now(),
        color: AdaptiveColor = AdaptiveColor.BLUE,
        isPartOfTimeline: Boolean = true
    ): Long {
        val timedNote = TimedNote(
            creationDate = Clock.System.now(),
            time = time,
            note = noteText,
            color = color,
            experienceId = experienceId,
            isPartOfTimeline = isPartOfTimeline
        )
        return experienceRepository.insertTimedNote(timedNote)
    }
    
    suspend fun toggleExperienceFavorite(experienceId: Int) {
        val experienceFlow = experienceRepository.getExperienceById(experienceId)
        // Note: This is a simplified implementation
        // In production, you'd want proper single-value collection
        experienceFlow.take(1).collect { experience ->
            experience?.let {
                val updated = it.copy(isFavorite = !it.isFavorite)
                experienceRepository.updateExperience(updated)
            }
        }
    }
    
    fun getFavoriteExperiences(): Flow<List<Experience>> {
        return experienceRepository.getFavoriteExperiences()
    }
    
    fun getIngestionsByExperience(experienceId: Int): Flow<List<Ingestion>> {
        return experienceRepository.getIngestionsByExperience(experienceId)
    }
    
    suspend fun updateExperience(experience: Experience) {
        experienceRepository.updateExperience(experience)
    }
    
    suspend fun deleteExperience(experienceId: Int) {
        experienceRepository.deleteExperience(experienceId)
    }
}

data class ExperienceSummary(
    val experience: Experience,
    val substanceCount: Int,
    val ingestionCount: Int,
    val lastActivity: Instant
) {
    val hasSubstances: Boolean get() = substanceCount > 0
    val isRecent: Boolean get() = 
        (Clock.System.now() - lastActivity).inWholeDays < 7
}