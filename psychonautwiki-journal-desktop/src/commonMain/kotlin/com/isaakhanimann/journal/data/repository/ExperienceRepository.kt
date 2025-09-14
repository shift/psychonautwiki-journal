package com.isaakhanimann.journal.data.repository

import com.isaakhanimann.journal.data.model.*
import com.isaakhanimann.journal.database.Database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers

interface ExperienceRepository {
    fun getAllExperiences(): Flow<List<Experience>>
    fun getExperienceById(id: Int): Flow<Experience?>
    fun getFavoriteExperiences(): Flow<List<Experience>>
    fun searchExperiences(query: String): Flow<List<Experience>>
    suspend fun insertExperience(experience: Experience): Long
    suspend fun updateExperience(experience: Experience)
    suspend fun deleteExperience(id: Int)
    
    fun getAllIngestions(): Flow<List<Ingestion>>
    fun getIngestionsByExperience(experienceId: Int): Flow<List<Ingestion>>
    suspend fun insertIngestion(ingestion: Ingestion): Long
    suspend fun updateIngestion(ingestion: Ingestion)
    suspend fun deleteIngestion(id: Int)
    
    fun getShulginRatingsByExperience(experienceId: Int): Flow<List<ShulginRating>>
    suspend fun insertShulginRating(rating: ShulginRating): Long
    suspend fun updateShulginRating(rating: ShulginRating)
    suspend fun deleteShulginRating(id: Int)
    
    fun getTimedNotesByExperience(experienceId: Int): Flow<List<TimedNote>>
    fun getTimelineNotesByExperience(experienceId: Int): Flow<List<TimedNote>>
    suspend fun insertTimedNote(note: TimedNote): Long
    suspend fun updateTimedNote(note: TimedNote)
    suspend fun deleteTimedNote(id: Int)
}

class ExperienceRepositoryImpl(
    private val database: Database
) : ExperienceRepository {
    
    private val queries = database.journalQueries
    
    override fun getAllExperiences(): Flow<List<Experience>> {
        return queries.selectAllExperiences()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { experienceList ->
                experienceList.map { exp ->
                    Experience(
                        id = exp.id.toInt(),
                        title = exp.title,
                        text = exp.text,
                        creationDate = Instant.fromEpochSeconds(exp.creationDate),
                        sortDate = Instant.fromEpochSeconds(exp.sortDate),
                        isFavorite = exp.isFavorite == 1L,
                        location = exp.locationName,
                        date = Instant.fromEpochSeconds(exp.sortDate),
                        overallRating = exp.overallRating?.toInt(),
                        ingestions = null // Will be loaded separately when needed
                    )
                }
            }
    }
    
    override fun getExperienceById(id: Int): Flow<Experience?> {
        return queries.selectExperienceById(id.toLong())
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { exp ->
                exp?.let {
                    Experience(
                        id = it.id.toInt(),
                        title = it.title,
                        text = it.text,
                        creationDate = Instant.fromEpochSeconds(it.creationDate),
                        sortDate = Instant.fromEpochSeconds(it.sortDate),
                        isFavorite = it.isFavorite == 1L,
                        location = it.locationName,
                        date = Instant.fromEpochSeconds(it.sortDate),
                        overallRating = it.overallRating?.toInt(),
                        ingestions = null // Will be loaded separately when needed
                    )
                }
            }
    }
    
    override fun getFavoriteExperiences(): Flow<List<Experience>> {
        return queries.selectFavoriteExperiences()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { experienceList ->
                experienceList.map { exp ->
                    Experience(
                        id = exp.id.toInt(),
                        title = exp.title,
                        text = exp.text,
                        creationDate = Instant.fromEpochSeconds(exp.creationDate),
                        sortDate = Instant.fromEpochSeconds(exp.sortDate),
                        isFavorite = exp.isFavorite == 1L,
                        location = exp.locationName,
                        date = Instant.fromEpochSeconds(exp.sortDate),
                        overallRating = exp.overallRating?.toInt(),
                        ingestions = null // Will be loaded separately when needed
                    )
                }
            }
    }
    
    override suspend fun insertExperience(experience: Experience): Long {
        return queries.transactionWithResult {
            queries.insertExperience(
                title = experience.title,
                text = experience.text,
                creationDate = experience.creationDate.epochSeconds,
                sortDate = experience.sortDate.epochSeconds,
                isFavorite = if (experience.isFavorite) 1L else 0L,
                locationName = experience.location,
                locationLongitude = null,
                locationLatitude = null
            )
            queries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun updateExperience(experience: Experience) {
        queries.updateExperience(
            title = experience.title,
            text = experience.text,
            isFavorite = if (experience.isFavorite) 1L else 0L,
            locationName = experience.location,
            locationLongitude = null,
            locationLatitude = null,
            id = experience.id.toLong()
        )
    }
    
    override suspend fun deleteExperience(id: Int) {
        queries.deleteExperience(id.toLong())
    }
    
    override fun getAllIngestions(): Flow<List<Ingestion>> {
        return queries.selectAllIngestions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { ingestionList ->
                ingestionList.map { ing ->
                    Ingestion(
                        id = ing.id.toInt(),
                        substanceName = ing.substanceName,
                        time = Instant.fromEpochSeconds(ing.time),
                        endTime = ing.endTime?.let { Instant.fromEpochSeconds(it) },
                        creationDate = ing.creationDate?.let { Instant.fromEpochSeconds(it) },
                        administrationRoute = AdministrationRoute.valueOf(ing.administrationRoute),
                        dose = ing.dose,
                        isDoseAnEstimate = ing.isDoseAnEstimate == 1L,
                        estimatedDoseStandardDeviation = ing.estimatedDoseStandardDeviation,
                        units = ing.units,
                        experienceId = ing.experienceId.toInt(),
                        notes = ing.notes,
                        stomachFullness = ing.stomachFullness?.let { StomachFullness.valueOf(it) },
                        consumerName = ing.consumerName,
                        customUnitId = ing.customUnitId?.toInt()
                    )
                }
            }
    }
    
    override fun getIngestionsByExperience(experienceId: Int): Flow<List<Ingestion>> {
        return queries.selectIngestionsByExperience(experienceId.toLong())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { ingestionList ->
                ingestionList.map { ing ->
                    Ingestion(
                        id = ing.id.toInt(),
                        substanceName = ing.substanceName,
                        time = Instant.fromEpochSeconds(ing.time),
                        endTime = ing.endTime?.let { Instant.fromEpochSeconds(it) },
                        creationDate = ing.creationDate?.let { Instant.fromEpochSeconds(it) },
                        administrationRoute = AdministrationRoute.valueOf(ing.administrationRoute),
                        dose = ing.dose,
                        isDoseAnEstimate = ing.isDoseAnEstimate == 1L,
                        estimatedDoseStandardDeviation = ing.estimatedDoseStandardDeviation,
                        units = ing.units,
                        experienceId = ing.experienceId.toInt(),
                        notes = ing.notes,
                        stomachFullness = ing.stomachFullness?.let { StomachFullness.valueOf(it) },
                        consumerName = ing.consumerName,
                        customUnitId = ing.customUnitId?.toInt()
                    )
                }
            }
    }
    
    override suspend fun insertIngestion(ingestion: Ingestion): Long {
        return queries.transactionWithResult {
            queries.insertIngestion(
                substanceName = ingestion.substanceName,
                time = ingestion.time.epochSeconds,
                endTime = ingestion.endTime?.epochSeconds,
                creationDate = ingestion.creationDate?.epochSeconds,
                administrationRoute = ingestion.administrationRoute.name,
                dose = ingestion.dose,
                isDoseAnEstimate = if (ingestion.isDoseAnEstimate) 1L else 0L,
                estimatedDoseStandardDeviation = ingestion.estimatedDoseStandardDeviation,
                units = ingestion.units,
                experienceId = ingestion.experienceId.toLong(),
                notes = ingestion.notes,
                stomachFullness = ingestion.stomachFullness?.name,
                consumerName = ingestion.consumerName,
                customUnitId = ingestion.customUnitId?.toLong()
            )
            queries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun updateIngestion(ingestion: Ingestion) {
        queries.updateIngestion(
            substanceName = ingestion.substanceName,
            time = ingestion.time.epochSeconds,
            endTime = ingestion.endTime?.epochSeconds,
            administrationRoute = ingestion.administrationRoute.name,
            dose = ingestion.dose,
            isDoseAnEstimate = if (ingestion.isDoseAnEstimate) 1L else 0L,
            estimatedDoseStandardDeviation = ingestion.estimatedDoseStandardDeviation,
            units = ingestion.units,
            notes = ingestion.notes,
            stomachFullness = ingestion.stomachFullness?.name,
            consumerName = ingestion.consumerName,
            customUnitId = ingestion.customUnitId?.toLong(),
            id = ingestion.id.toLong()
        )
    }
    
    override suspend fun deleteIngestion(id: Int) {
        queries.deleteIngestion(id.toLong())
    }
    
    override fun getShulginRatingsByExperience(experienceId: Int): Flow<List<ShulginRating>> {
        return queries.selectShulginRatingsByExperience(experienceId.toLong())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { ratingList ->
                ratingList.map { rating ->
                    ShulginRating(
                        id = rating.id.toInt(),
                        time = rating.time?.let { Instant.fromEpochSeconds(it) },
                        creationDate = rating.creationDate?.let { Instant.fromEpochSeconds(it) },
                        option = ShulginRatingOption.valueOf(rating.option),
                        experienceId = rating.experienceId.toInt()
                    )
                }
            }
    }
    
    override suspend fun insertShulginRating(rating: ShulginRating): Long {
        return queries.transactionWithResult {
            queries.insertShulginRating(
                time = rating.time?.epochSeconds,
                creationDate = rating.creationDate?.epochSeconds,
                option = rating.option.name,
                experienceId = rating.experienceId.toLong()
            )
            queries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun updateShulginRating(rating: ShulginRating) {
        queries.updateShulginRating(
            time = rating.time?.epochSeconds,
            option = rating.option.name,
            id = rating.id.toLong()
        )
    }
    
    override suspend fun deleteShulginRating(id: Int) {
        queries.deleteShulginRating(id.toLong())
    }
    
    override fun getTimedNotesByExperience(experienceId: Int): Flow<List<TimedNote>> {
        return queries.selectTimedNotesByExperience(experienceId.toLong())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { noteList ->
                noteList.map { note ->
                    TimedNote(
                        id = note.id.toInt(),
                        creationDate = Instant.fromEpochSeconds(note.creationDate),
                        time = Instant.fromEpochSeconds(note.time),
                        note = note.note,
                        color = AdaptiveColor.valueOf(note.color),
                        experienceId = note.experienceId.toInt(),
                        isPartOfTimeline = note.isPartOfTimeline == 1L
                    )
                }
            }
    }
    
    override fun getTimelineNotesByExperience(experienceId: Int): Flow<List<TimedNote>> {
        return queries.selectTimelineNotesByExperience(experienceId.toLong())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { noteList ->
                noteList.map { note ->
                    TimedNote(
                        id = note.id.toInt(),
                        creationDate = Instant.fromEpochSeconds(note.creationDate),
                        time = Instant.fromEpochSeconds(note.time),
                        note = note.note,
                        color = AdaptiveColor.valueOf(note.color),
                        experienceId = note.experienceId.toInt(),
                        isPartOfTimeline = note.isPartOfTimeline == 1L
                    )
                }
            }
    }
    
    override suspend fun insertTimedNote(note: TimedNote): Long {
        return queries.transactionWithResult {
            queries.insertTimedNote(
                creationDate = note.creationDate.epochSeconds,
                time = note.time.epochSeconds,
                note = note.note,
                color = note.color.name,
                experienceId = note.experienceId.toLong(),
                isPartOfTimeline = if (note.isPartOfTimeline) 1L else 0L
            )
            queries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun updateTimedNote(note: TimedNote) {
        queries.updateTimedNote(
            time = note.time.epochSeconds,
            note = note.note,
            color = note.color.name,
            isPartOfTimeline = if (note.isPartOfTimeline) 1L else 0L,
            id = note.id.toLong()
        )
    }
    
    override suspend fun deleteTimedNote(id: Int) {
        queries.deleteTimedNote(id.toLong())
    }
}