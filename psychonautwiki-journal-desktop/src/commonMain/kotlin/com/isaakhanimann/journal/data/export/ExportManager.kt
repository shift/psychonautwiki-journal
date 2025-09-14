package com.isaakhanimann.journal.data.export

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.datetime.Instant
import com.isaakhanimann.journal.data.experience.ExperienceTracker
import com.isaakhanimann.journal.data.experience.ExperienceWithDetails
import com.isaakhanimann.journal.data.model.*

interface ExportManager {
    suspend fun exportToJson(): String
    suspend fun exportToCsv(): String
    suspend fun exportToJsonFile(filePath: String): Boolean
    suspend fun exportToCsvFile(filePath: String): Boolean
    fun exportProgress(): Flow<ExportProgress>
}

class ExportManagerImpl(
    private val experienceTracker: ExperienceTracker
) : ExportManager {
    
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }
    
    override suspend fun exportToJson(): String {
        val experiences = experienceTracker.getAllExperiencesWithDetails().first()
        val exportData = ExportData(
            exportVersion = "1.0",
            exportDate = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            experiences = experiences.map { it.toExportExperience() }
        )
        return json.encodeToString(exportData)
    }
    
    override suspend fun exportToCsv(): String {
        val experiences = experienceTracker.getAllExperiencesWithDetails().first()
        return buildString {
            // CSV Header
            appendLine("Experience ID,Title,Date,Text,Is Favorite,Location Name,Location Latitude,Location Longitude,Substance Name,Dose,Units,Administration Route,Ingestion Time,End Time,Notes,Is Dose Estimate,Stomach Fullness,Consumer Name")
            
            // Experience rows
            experiences.forEach { experienceWithDetails ->
                val exp = experienceWithDetails.experience
                val ingestions = experienceWithDetails.ingestions
                
                if (ingestions.isEmpty()) {
                    // Experience without ingestions
                    appendLine(buildCsvRow(
                        exp.id.toString(),
                        exp.title.escapeCsv(),
                        exp.sortDate.toEpochMilliseconds().toString(),
                        exp.text.escapeCsv(),
                        exp.isFavorite.toString(),
                        exp.location?.escapeCsv() ?: "",
                        "", // latitude not available in string location
                        "", // longitude not available in string location
                        "", "", "", "", "", "", "", "", "", ""
                    ))
                } else {
                    // Experience with ingestions (one row per ingestion)
                    ingestions.forEach { ingestion ->
                        appendLine(buildCsvRow(
                            exp.id.toString(),
                            exp.title.escapeCsv(),
                            exp.sortDate.toEpochMilliseconds().toString(),
                            exp.text.escapeCsv(),
                            exp.isFavorite.toString(),
                            exp.location?.escapeCsv() ?: "",
                            "", // latitude not available in string location
                            "", // longitude not available in string location
                            ingestion.substanceName.escapeCsv(),
                            ingestion.dose?.toString() ?: "",
                            ingestion.units?.escapeCsv() ?: "",
                            ingestion.administrationRoute.displayName.escapeCsv(),
                            ingestion.time.toEpochMilliseconds().toString(),
                            ingestion.endTime?.toEpochMilliseconds()?.toString() ?: "",
                            ingestion.notes?.escapeCsv() ?: "",
                            ingestion.isDoseAnEstimate.toString(),
                            ingestion.stomachFullness?.name?.escapeCsv() ?: "",
                            ingestion.consumerName?.escapeCsv() ?: ""
                        ))
                    }
                }
            }
        }
    }
    
    override suspend fun exportToJsonFile(filePath: String): Boolean {
        return try {
            val jsonData = exportToJson()
            java.io.File(filePath).writeText(jsonData)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun exportToCsvFile(filePath: String): Boolean {
        return try {
            val csvData = exportToCsv()
            java.io.File(filePath).writeText(csvData)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun exportProgress(): Flow<ExportProgress> {
        // For now, return a simple implementation
        // In a more complex implementation, this could track actual progress
        return kotlinx.coroutines.flow.flowOf(ExportProgress.Completed)
    }
    
    private fun buildCsvRow(vararg fields: String): String {
        return fields.joinToString(",")
    }
    
    private fun String.escapeCsv(): String {
        return if (contains(",") || contains("\"") || contains("\n")) {
            "\"${replace("\"", "\"\"")}\""
        } else {
            this
        }
    }
}

private fun ExperienceWithDetails.toExportExperience(): ExportExperience {
    return ExportExperience(
        id = experience.id.toInt(),
        title = experience.title,
        text = experience.text,
        isFavorite = experience.isFavorite,
        sortDate = experience.sortDate.toEpochMilliseconds(),
        location = experience.location?.let { locationName ->
            ExportLocation(
                name = locationName,
                latitude = null, // not available in string location
                longitude = null // not available in string location
            )
        },
        ingestions = ingestions.map { ingestion ->
            ExportIngestion(
                id = ingestion.id.toInt(),
                substanceName = ingestion.substanceName,
                dose = ingestion.dose,
                units = ingestion.units,
                administrationRoute = ingestion.administrationRoute.name,
                time = ingestion.time.toEpochMilliseconds(),
                endTime = ingestion.endTime?.toEpochMilliseconds(),
                notes = ingestion.notes,
                isDoseAnEstimate = ingestion.isDoseAnEstimate,
                stomachFullness = ingestion.stomachFullness?.name,
                consumerName = ingestion.consumerName
            )
        }
    )
}

@Serializable
data class ExportData(
    val exportVersion: String,
    val exportDate: Long,
    val experiences: List<ExportExperience>
)

@Serializable
data class ExportExperience(
    val id: Int,
    val title: String,
    val text: String,
    val isFavorite: Boolean,
    val sortDate: Long,
    val location: ExportLocation? = null,
    val ingestions: List<ExportIngestion> = emptyList()
)

@Serializable
data class ExportLocation(
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class ExportIngestion(
    val id: Int,
    val substanceName: String,
    val dose: Double? = null,
    val units: String? = null,
    val administrationRoute: String,
    val time: Long,
    val endTime: Long? = null,
    val notes: String? = null,
    val isDoseAnEstimate: Boolean = false,
    val stomachFullness: String? = null,
    val consumerName: String? = null
)

sealed class ExportProgress {
    object Starting : ExportProgress()
    data class InProgress(val current: Int, val total: Int) : ExportProgress()
    object Completed : ExportProgress()
    data class Error(val message: String) : ExportProgress()
}