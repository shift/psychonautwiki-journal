package com.isaakhanimann.journal.data.import

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.datetime.Instant
import com.isaakhanimann.journal.data.experience.ExperienceTracker
import com.isaakhanimann.journal.data.export.*
import com.isaakhanimann.journal.data.model.*
import java.io.File

interface ImportManager {
    suspend fun importFromJson(jsonData: String): ImportResult
    suspend fun importFromCsv(csvData: String): ImportResult
    suspend fun importFromJsonFile(filePath: String): ImportResult
    suspend fun importFromCsvFile(filePath: String): ImportResult
    suspend fun validateImportData(data: String, format: ImportFormat): ValidationResult
    fun importProgress(): Flow<ImportProgress>
}

class ImportManagerImpl(
    private val experienceTracker: ExperienceTracker
) : ImportManager {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    override suspend fun importFromJson(jsonData: String): ImportResult {
        return try {
            val exportData = json.decodeFromString<ExportData>(jsonData)
            importExportData(exportData)
        } catch (e: Exception) {
            ImportResult.Error("Failed to parse JSON: ${e.message}")
        }
    }
    
    override suspend fun importFromCsv(csvData: String): ImportResult {
        return try {
            val experiences = parseCsvData(csvData)
            importExperiences(experiences)
        } catch (e: Exception) {
            ImportResult.Error("Failed to parse CSV: ${e.message}")
        }
    }
    
    override suspend fun importFromJsonFile(filePath: String): ImportResult {
        return try {
            val jsonData = File(filePath).readText()
            importFromJson(jsonData)
        } catch (e: Exception) {
            ImportResult.Error("Failed to read file: ${e.message}")
        }
    }
    
    override suspend fun importFromCsvFile(filePath: String): ImportResult {
        return try {
            val csvData = File(filePath).readText()
            importFromCsv(csvData)
        } catch (e: Exception) {
            ImportResult.Error("Failed to read file: ${e.message}")
        }
    }
    
    override suspend fun validateImportData(data: String, format: ImportFormat): ValidationResult {
        return try {
            when (format) {
                ImportFormat.JSON -> {
                    val exportData = json.decodeFromString<ExportData>(data)
                    ValidationResult.Valid(
                        experienceCount = exportData.experiences.size,
                        ingestionCount = exportData.experiences.sumOf { it.ingestions.size },
                        warnings = validateExportData(exportData)
                    )
                }
                ImportFormat.CSV -> {
                    val lines = data.lines().filter { it.isNotBlank() }
                    if (lines.size < 2) {
                        ValidationResult.Invalid("CSV file must contain at least a header and one data row")
                    } else {
                        ValidationResult.Valid(
                            experienceCount = lines.size - 1, // Subtract header
                            ingestionCount = lines.size - 1,
                            warnings = emptyList()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            ValidationResult.Invalid("Invalid format: ${e.message}")
        }
    }
    
    override fun importProgress(): Flow<ImportProgress> {
        // Simple implementation - in a real app, this would track actual progress
        return flowOf(ImportProgress.Completed)
    }
    
    private suspend fun importExportData(exportData: ExportData): ImportResult {
        return try {
            var imported = 0
            var duplicates = 0
            val errors = mutableListOf<String>()
            
            exportData.experiences.forEach { exportExperience ->
                try {
                    // Check for duplicates (simple title + date matching)
                    val existingExperiences = experienceTracker.getAllExperiencesWithDetails()
                    // For now, we'll skip duplicate checking and just import
                    
                    // Create experience
                    val experienceId = experienceTracker.createNewExperience(
                        title = exportExperience.title,
                        text = exportExperience.text,
                        location = exportExperience.location?.let { loc ->
                            Location(
                                name = loc.name,
                                latitude = loc.latitude,
                                longitude = loc.longitude
                            )
                        },
                        sortDate = Instant.fromEpochMilliseconds(exportExperience.sortDate)
                    )
                    
                    // Import ingestions
                    exportExperience.ingestions.forEach { exportIngestion ->
                        experienceTracker.addIngestionToExperience(
                            experienceId = experienceId.toInt(),
                            substanceName = exportIngestion.substanceName,
                            dose = exportIngestion.dose,
                            units = exportIngestion.units,
                            administrationRoute = AdministrationRoute.values().find { 
                                it.name == exportIngestion.administrationRoute 
                            } ?: AdministrationRoute.ORAL,
                            time = Instant.fromEpochMilliseconds(exportIngestion.time),
                            notes = exportIngestion.notes,
                            isDoseAnEstimate = exportIngestion.isDoseAnEstimate,
                            stomachFullness = exportIngestion.stomachFullness?.let { sf ->
                                StomachFullness.values().find { it.name == sf }
                            },
                            consumerName = exportIngestion.consumerName
                        )
                    }
                    
                    imported++
                } catch (e: Exception) {
                    errors.add("Failed to import experience '${exportExperience.title}': ${e.message}")
                }
            }
            
            ImportResult.Success(
                imported = imported,
                duplicates = duplicates,
                errors = errors
            )
        } catch (e: Exception) {
            ImportResult.Error("Import failed: ${e.message}")
        }
    }
    
    private suspend fun importExperiences(experiences: List<CsvExperience>): ImportResult {
        return try {
            var imported = 0
            val errors = mutableListOf<String>()
            
            // Group by experience ID to handle multiple ingestions per experience
            val groupedExperiences = experiences.groupBy { it.experienceId }
            
            groupedExperiences.forEach { (expId, csvExperiences) ->
                try {
                    val firstExp = csvExperiences.first()
                    
                    // Create experience
                    val experienceId = experienceTracker.createNewExperience(
                        title = firstExp.title,
                        text = firstExp.text,
                        location = if (firstExp.locationName.isNotBlank()) {
                            Location(
                                name = firstExp.locationName,
                                latitude = firstExp.locationLatitude,
                                longitude = firstExp.locationLongitude
                            )
                        } else null,
                        sortDate = Instant.fromEpochMilliseconds(firstExp.date)
                    )
                    
                    // Import ingestions
                    csvExperiences.forEach { csvExp ->
                        if (csvExp.substanceName.isNotBlank()) {
                            experienceTracker.addIngestionToExperience(
                                experienceId = experienceId.toInt(),
                                substanceName = csvExp.substanceName,
                                dose = csvExp.dose,
                                units = csvExp.units.ifBlank { null },
                                administrationRoute = AdministrationRoute.values().find { 
                                    it.displayName == csvExp.administrationRoute 
                                } ?: AdministrationRoute.ORAL,
                                time = Instant.fromEpochMilliseconds(csvExp.ingestionTime),
                                notes = csvExp.notes.ifBlank { null },
                                isDoseAnEstimate = csvExp.isDoseEstimate,
                                stomachFullness = csvExp.stomachFullness?.let { sf ->
                                    StomachFullness.values().find { it.name == sf }
                                },
                                consumerName = csvExp.consumerName.ifBlank { null }
                            )
                        }
                    }
                    
                    imported++
                } catch (e: Exception) {
                    errors.add("Failed to import experience ID $expId: ${e.message}")
                }
            }
            
            ImportResult.Success(
                imported = imported,
                duplicates = 0,
                errors = errors
            )
        } catch (e: Exception) {
            ImportResult.Error("CSV import failed: ${e.message}")
        }
    }
    
    private fun parseCsvData(csvData: String): List<CsvExperience> {
        val lines = csvData.lines().filter { it.isNotBlank() }
        if (lines.size < 2) throw IllegalArgumentException("CSV must contain header and data")
        
        val header = lines.first().split(",")
        val experiences = mutableListOf<CsvExperience>()
        
        lines.drop(1).forEach { line ->
            val fields = parseCsvLine(line)
            
            experiences.add(CsvExperience(
                experienceId = fields.getOrNull(0)?.toIntOrNull() ?: 0,
                title = fields.getOrNull(1)?.unescapeCsv() ?: "",
                date = fields.getOrNull(2)?.toLongOrNull() ?: 0L,
                text = fields.getOrNull(3)?.unescapeCsv() ?: "",
                isFavorite = fields.getOrNull(4)?.toBooleanStrictOrNull() ?: false,
                locationName = fields.getOrNull(5)?.unescapeCsv() ?: "",
                locationLatitude = fields.getOrNull(6)?.toDoubleOrNull(),
                locationLongitude = fields.getOrNull(7)?.toDoubleOrNull(),
                substanceName = fields.getOrNull(8)?.unescapeCsv() ?: "",
                dose = fields.getOrNull(9)?.toDoubleOrNull(),
                units = fields.getOrNull(10)?.unescapeCsv() ?: "",
                administrationRoute = fields.getOrNull(11)?.unescapeCsv() ?: "",
                ingestionTime = fields.getOrNull(12)?.toLongOrNull() ?: 0L,
                endTime = fields.getOrNull(13)?.toLongOrNull(),
                notes = fields.getOrNull(14)?.unescapeCsv() ?: "",
                isDoseEstimate = fields.getOrNull(15)?.toBooleanStrictOrNull() ?: false,
                stomachFullness = fields.getOrNull(16)?.unescapeCsv(),
                consumerName = fields.getOrNull(17)?.unescapeCsv() ?: ""
            ))
        }
        
        return experiences
    }
    
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++ // Skip next quote
                }
                char == '"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
            i++
        }
        fields.add(current.toString())
        
        return fields
    }
    
    private fun String.unescapeCsv(): String {
        return if (startsWith("\"") && endsWith("\"")) {
            substring(1, length - 1).replace("\"\"", "\"")
        } else {
            this
        }
    }
    
    private fun validateExportData(exportData: ExportData): List<String> {
        val warnings = mutableListOf<String>()
        
        if (exportData.experiences.isEmpty()) {
            warnings.add("No experiences found in import data")
        }
        
        exportData.experiences.forEach { exp ->
            if (exp.title.isBlank()) {
                warnings.add("Experience with empty title found")
            }
        }
        
        return warnings
    }
}

data class CsvExperience(
    val experienceId: Int,
    val title: String,
    val date: Long,
    val text: String,
    val isFavorite: Boolean,
    val locationName: String,
    val locationLatitude: Double?,
    val locationLongitude: Double?,
    val substanceName: String,
    val dose: Double?,
    val units: String,
    val administrationRoute: String,
    val ingestionTime: Long,
    val endTime: Long?,
    val notes: String,
    val isDoseEstimate: Boolean,
    val stomachFullness: String?,
    val consumerName: String
)

enum class ImportFormat {
    JSON, CSV
}

sealed class ImportResult {
    data class Success(
        val imported: Int,
        val duplicates: Int,
        val errors: List<String>
    ) : ImportResult()
    
    data class Error(val message: String) : ImportResult()
}

sealed class ValidationResult {
    data class Valid(
        val experienceCount: Int,
        val ingestionCount: Int,
        val warnings: List<String>
    ) : ValidationResult()
    
    data class Invalid(val reason: String) : ValidationResult()
}

sealed class ImportProgress {
    object Starting : ImportProgress()
    data class InProgress(val current: Int, val total: Int) : ImportProgress()
    object Completed : ImportProgress()
    data class Error(val message: String) : ImportProgress()
}