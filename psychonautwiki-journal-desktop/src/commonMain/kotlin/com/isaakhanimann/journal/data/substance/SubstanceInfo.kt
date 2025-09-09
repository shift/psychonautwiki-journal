package com.isaakhanimann.journal.data.substance

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// JSON structure matches the PsychonautWiki database format
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
    val min: Double? = null,
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

// Substance database singleton that loads from JSON resources
object PsychonautWikiDatabase {
    private var _database: SubstanceDatabaseJson? = null
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    // Database health metrics
    data class DatabaseHealth(
        val totalSubstances: Int,
        val loadedSuccessfully: Int,
        val failedToLoad: Int,
        val loadTimeMs: Long,
        val substancesWithRoas: Int,
        val substancesWithBioavailability: Int,
        val substancesWithInteractions: Int
    )
    
    private var _lastLoadHealth: DatabaseHealth? = null
    val lastLoadHealth: DatabaseHealth? get() = _lastLoadHealth
    
    private val database: SubstanceDatabaseJson
        get() {
            if (_database == null) {
                loadDatabase()
            }
            return _database!!
        }
    
    private fun loadDatabase() {
        val startTime = System.currentTimeMillis()
        try {
            println("🔄 Loading PsychonautWiki substance database...")
            
            // Try multiple resource loading approaches
            val jsonText = try {
                // First try: Compose resources path
                object {}.javaClass.getResourceAsStream("/Substances.json")
                    ?.bufferedReader()?.use { it.readText() }
            } catch (e: Exception) {
                null
            } ?: try {
                // Second try: Assets path
                object {}.javaClass.getResourceAsStream("/assets/Substances.json")
                    ?.bufferedReader()?.use { it.readText() }
            } catch (e: Exception) {
                null
            } ?: try {
                // Third try: Alternative class loader approach
                Thread.currentThread().contextClassLoader
                    .getResourceAsStream("Substances.json")
                    ?.bufferedReader()?.use { it.readText() }
            } catch (e: Exception) {
                null
            } ?: throw IllegalStateException("Substances.json not found in any resource location")
            
            println("📁 JSON file loaded: ${jsonText.length} characters")
            
            _database = json.decodeFromString<SubstanceDatabaseJson>(jsonText)
            val loadTime = System.currentTimeMillis() - startTime
            
            val substances = _database!!.substances
            val categories = _database!!.categories
            
            // Calculate database metrics
            val substancesWithRoas = substances.count { it.roas.isNotEmpty() }
            val substancesWithBioavailability = substances.count { substance ->
                substance.roas.any { it.bioavailability != null }
            }
            val substancesWithInteractions = substances.count { it.interactions != null }
            val totalRoas = substances.sumOf { it.roas.size }
            
            // Store health metrics
            _lastLoadHealth = DatabaseHealth(
                totalSubstances = substances.size,
                loadedSuccessfully = substances.size,
                failedToLoad = 0,
                loadTimeMs = loadTime,
                substancesWithRoas = substancesWithRoas,
                substancesWithBioavailability = substancesWithBioavailability,
                substancesWithInteractions = substancesWithInteractions
            )
            
            println("✅ Successfully loaded PsychonautWiki database in ${loadTime}ms")
            println("📊 Database metrics:")
            println("   • ${substances.size} substances")
            println("   • ${categories.size} categories") 
            println("   • ${substancesWithRoas} substances with dosing/duration data")
            println("   • ${substancesWithBioavailability} substances with bioavailability data")
            println("   • ${substancesWithInteractions} substances with interaction warnings")
            println("   • ${totalRoas} total routes of administration")
        } catch (e: Exception) {
            println("❌ Failed to load substance database: ${e.message}")
            e.printStackTrace()
            // Fallback to minimal hardcoded data
            _database = SubstanceDatabaseJson(
                categories = emptyList(),
                substances = listOf(
                    SubstanceInfo(
                        name = "Alcohol",
                        categories = listOf("depressant"),
                        commonNames = listOf("Ethanol", "EtOH"),
                        summary = "A depressant substance commonly consumed recreationally."
                    ),
                    SubstanceInfo(
                        name = "Cannabis",
                        categories = listOf("psychedelic", "depressant"),
                        commonNames = listOf("Marijuana", "Weed", "THC"),
                        summary = "A psychoactive plant commonly used for recreational and medicinal purposes."
                    ),
                    SubstanceInfo(
                        name = "Caffeine",
                        categories = listOf("stimulant"),
                        commonNames = listOf("Coffee"),
                        summary = "A stimulant substance commonly consumed in coffee and tea."
                    ),
                    SubstanceInfo(
                        name = "LSD",
                        categories = listOf("psychedelic"),
                        commonNames = listOf("Acid", "Lucy"),
                        summary = "A powerful hallucinogenic substance."
                    ),
                    SubstanceInfo(
                        name = "MDMA",
                        categories = listOf("entactogen", "stimulant"),
                        commonNames = listOf("Ecstasy", "Molly"),
                        summary = "An empathogenic stimulant."
                    )
                )
            )
        }
    }
    
    fun getAllSubstances(): List<SubstanceInfo> {
        return database.substances.sortedBy { it.name }
    }
    
    fun getSubstanceByName(name: String): SubstanceInfo? {
        return database.substances.find { 
            it.name.equals(name, ignoreCase = true) ||
            it.commonNames.any { commonName -> commonName.equals(name, ignoreCase = true) }
        }
    }
    
    fun searchSubstances(query: String): List<SubstanceInfo> {
        if (query.isBlank()) return getAllSubstances()
        
        return database.substances.filter { substance ->
            substance.name.contains(query, ignoreCase = true) ||
            substance.commonNames.any { it.contains(query, ignoreCase = true) } ||
            (substance.summary?.contains(query, ignoreCase = true) == true) ||
            substance.categories.any { it.contains(query, ignoreCase = true) }
        }.sortedBy { it.name }
    }
    
    fun getAllSubstanceNames(): List<String> {
        return database.substances.map { it.name }.sorted()
    }
    
    fun getCategories(): List<Category> {
        return database.categories
    }
    
    fun getSubstancesByCategory(category: String): List<SubstanceInfo> {
        return database.substances.filter { substance ->
            substance.categories.any { it.equals(category, ignoreCase = true) }
        }.sortedBy { it.name }
    }
    
    // Get dosing information for a substance and route
    fun getDoseInfo(substanceName: String, route: String): DoseInfo? {
        return getSubstanceByName(substanceName)
            ?.roas
            ?.find { it.name.equals(route, ignoreCase = true) }
            ?.dose
    }
    
    // Get duration information for a substance and route  
    fun getDurationInfo(substanceName: String, route: String): DurationInfo? {
        return getSubstanceByName(substanceName)
            ?.roas
            ?.find { it.name.equals(route, ignoreCase = true) }
            ?.duration
    }
    
    // Check for dangerous interactions
    fun getDangerousInteractions(substanceName: String): List<String> {
        return getSubstanceByName(substanceName)
            ?.interactions
            ?.dangerous
            ?: emptyList()
    }
    
    // Get all available routes for a substance
    fun getAvailableRoutes(substanceName: String): List<String> {
        return getSubstanceByName(substanceName)
            ?.roas
            ?.map { it.name }
            ?: emptyList()
    }
    
    // Get database health information
    fun getDatabaseHealth(): DatabaseHealth? = _lastLoadHealth
    
    // Check if database is loaded and healthy
    fun isDatabaseHealthy(): Boolean {
        return _lastLoadHealth?.let { health ->
            health.loadedSuccessfully > 100 && // At least 100 substances loaded
            health.failedToLoad < health.totalSubstances * 0.1 // Less than 10% failed
        } ?: false
    }
}

// Legacy compatibility - maintain old interface
@Deprecated("Use PsychonautWikiDatabase instead", ReplaceWith("PsychonautWikiDatabase"))
object SubstanceDatabase {
    fun getSubstanceByName(name: String): SubstanceInfo? = PsychonautWikiDatabase.getSubstanceByName(name)
    fun searchSubstances(query: String): List<SubstanceInfo> = PsychonautWikiDatabase.searchSubstances(query)
    fun getAllSubstanceNames(): List<String> = PsychonautWikiDatabase.getAllSubstanceNames()
}