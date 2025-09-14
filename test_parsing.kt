#!/usr/bin/env kotlin

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class SubstanceDatabaseJson(
    val categories: List<Category>,
    val substances: List<SubstanceInfo>
)

@Serializable
data class Category(
    val name: String,
    val description: String? = null,
    val url: String? = null,
    val color: Long? = null
)

@Serializable
data class SubstanceInfo(
    val name: String,
    val commonNames: List<String> = emptyList(),
    val url: String? = null,
    val isApproved: Boolean = false,
    val tolerance: ToleranceInfo? = null,
    val crossTolerances: List<String> = emptyList(),
    val addictionPotential: String? = null,
    val toxicities: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val summary: String? = null,
    val interactions: InteractionData? = null,
    val roas: List<RouteOfAdministration> = emptyList()
)

@Serializable
data class RouteOfAdministration(
    val name: String,
    val dose: DoseInfo? = null,
    val duration: DurationInfo? = null,
    val bioavailability: BioavailabilityRange? = null
)

@Serializable
data class DurationInfo(
    val onset: DurationRange? = null,
    val comeup: DurationRange? = null,
    val peak: DurationRange? = null,
    val offset: DurationRange? = null,
    val total: DurationRange? = null,
    val afterglow: DurationRange? = null
)

@Serializable
data class DurationRange(
    val min: Double,
    val max: Double? = null,
    val units: String
)

@Serializable
data class BioavailabilityRange(
    val min: Double? = null,
    val max: Double? = null
)

@Serializable
data class DoseInfo(
    val units: String,
    val lightMin: Double? = null,
    val commonMin: Double? = null,
    val strongMin: Double? = null,
    val heavyMin: Double? = null
)

@Serializable
data class ToleranceInfo(
    val full: String? = null,
    val half: String? = null,
    val zero: String? = null
)

@Serializable
data class InteractionData(
    val dangerous: List<String> = emptyList(),
    val unsafe: List<String> = emptyList(),
    val uncertain: List<String> = emptyList()
)

val json = Json { 
    ignoreUnknownKeys = true
    isLenient = true
}

fun main() {
    try {
        val jsonText = File("psychonautwiki-journal-desktop/src/commonMain/resources/Substances.json").readText()
        println("JSON file size: ${jsonText.length} characters")
        
        val database = json.decodeFromString<SubstanceDatabaseJson>(jsonText)
        println("✅ Successfully loaded ${database.substances.size} substances")
        
        // Show first few substances
        database.substances.take(5).forEach { substance ->
            println("  - ${substance.name} (${substance.categories.joinToString(", ")})")
        }
        
        // Check bioavailability parsing specifically
        val substancesWithBioav = database.substances.filter { substance ->
            substance.roas.any { it.bioavailability != null }
        }
        println("\n${substancesWithBioav.size} substances have bioavailability data")
        
        substancesWithBioav.take(3).forEach { substance ->
            substance.roas.filter { it.bioavailability != null }.forEach { roa ->
                println("  ${substance.name} (${roa.name}): ${roa.bioavailability!!.min}-${roa.bioavailability!!.max}%")
            }
        }
        
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}

main()