import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

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
data class BioavailabilityRange(
    val min: Double? = null,
    val max: Double? = null
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
        println("🔍 Testing PsychonautWiki database loading with fixed bioavailability schema...")
        
        val jsonText = object {}.javaClass.getResourceAsStream("/Substances.json")
            ?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Substances.json not found")
            
        println("📁 JSON file loaded: ${jsonText.length} characters")
        
        val database = json.decodeFromString<SubstanceDatabaseJson>(jsonText)
        println("✅ Successfully parsed JSON into database object")
        println("📊 Loaded ${database.substances.size} substances")
        
        if (database.substances.size > 100) {
            println("🎉 SUCCESS: Full database loaded!")
            
            // Test bioavailability parsing specifically
            val substancesWithBioav = database.substances.filter { substance ->
                substance.roas.any { it.bioavailability != null }
            }
            println("💊 ${substancesWithBioav.size} substances have bioavailability data")
            
            // Test Bromantane specifically (substance #94 that was causing errors)
            val bromantane = database.substances.find { it.name.equals("Bromantane", ignoreCase = true) }
            if (bromantane != null) {
                println("✅ Found Bromantane")
                val oralRoute = bromantane.roas.find { it.name.equals("oral", ignoreCase = true) }
                if (oralRoute?.bioavailability != null) {
                    println("✅ Bromantane oral bioavailability: max=${oralRoute.bioavailability!!.max}%")
                }
            }
            
            // Show first few substances
            println("\n📋 Sample substances:")
            database.substances.take(5).forEach { substance ->
                println("  - ${substance.name} (${substance.categories.joinToString(", ")})")
            }
            
        } else {
            println("❌ Only ${database.substances.size} substances - still in fallback mode")
        }
        
    } catch (e: Exception) {
        println("❌ Error during loading: ${e.message}")
        e.printStackTrace()
    }
}