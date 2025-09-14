package com.isaakhanimann.journal.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Experience(
    val id: Int = 0,
    val title: String,
    val text: String,
    val creationDate: Instant,
    val sortDate: Instant,
    val isFavorite: Boolean = false,
    val location: String? = null,
    val date: Instant? = sortDate,
    val overallRating: Int? = null,
    val ingestions: List<Ingestion>? = null
)

@Serializable
data class Substance(
    val name: String,
    val commonNames: List<String> = emptyList(),
    val tolerance: ToleranceInfo? = null,
    val roas: List<RouteOfAdministration> = emptyList(),
    val interactions: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val psychoactiveClass: List<String> = emptyList(),
    val chemicalClass: List<String> = emptyList()
)

@Serializable
data class ToleranceInfo(
    val full: String? = null,
    val half: String? = null,
    val zero: String? = null
)

@Serializable
data class RouteOfAdministration(
    val route: AdministrationRoute,
    val dosage: DosageInfo? = null,
    val duration: DurationInfo? = null,
    val bioavailability: String? = null
)

@Serializable
data class DosageInfo(
    val threshold: Double? = null,
    val light: DoubleRange? = null,
    val common: DoubleRange? = null,
    val strong: DoubleRange? = null,
    val heavy: Double? = null,
    val units: String = "mg"
)

@Serializable
data class DurationInfo(
    val onset: String? = null,
    val comeup: String? = null,
    val peak: String? = null,
    val offset: String? = null,
    val total: String? = null
)

@Serializable
data class DoubleRange(
    val min: Double,
    val max: Double
)

@Serializable
data class Location(
    val name: String,
    val longitude: Double? = null,
    val latitude: Double? = null
)

@Serializable
data class Ingestion(
    val id: Int = 0,
    val substanceName: String,
    val time: Instant,
    val endTime: Instant? = null,
    val creationDate: Instant? = null,
    val administrationRoute: AdministrationRoute,
    val dose: Double? = null,
    val isDoseAnEstimate: Boolean,
    val estimatedDoseStandardDeviation: Double? = null,
    val units: String? = null,
    val experienceId: Int,
    val notes: String? = null,
    val stomachFullness: StomachFullness? = null,
    val consumerName: String? = null,
    val customUnitId: Int? = null
)

@Serializable
data class SubstanceCompanion(
    val substanceName: String,
    val color: AdaptiveColor
)

@Serializable
data class CustomSubstance(
    val id: Int = 0,
    val name: String,
    val units: String,
    val description: String
)

@Serializable
data class CustomUnit(
    val id: Int = 0,
    val substanceName: String,
    val name: String,
    val creationDate: Instant,
    val administrationRoute: AdministrationRoute,
    val dose: Double? = null,
    val estimatedDoseStandardDeviation: Double? = null,
    val isEstimate: Boolean,
    val isArchived: Boolean,
    val unit: String,
    val unitPlural: String? = null,
    val originalUnit: String,
    val note: String
)

@Serializable
data class ShulginRating(
    val id: Int = 0,
    val time: Instant? = null,
    val creationDate: Instant? = null,
    val option: ShulginRatingOption,
    val experienceId: Int
)

@Serializable
data class TimedNote(
    val id: Int = 0,
    val creationDate: Instant,
    val time: Instant,
    val note: String,
    val color: AdaptiveColor,
    val experienceId: Int,
    val isPartOfTimeline: Boolean
)

@Serializable
data class UserPreference(
    val id: Int = 0,
    val key: String,
    val value: String,
    val createdAt: Instant,
    val updatedAt: Instant
)