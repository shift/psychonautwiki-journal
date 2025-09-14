package com.isaakhanimann.journal.plugin

import androidx.compose.runtime.Composable
import com.isaakhanimann.journal.data.model.*
import com.isaakhanimann.journal.data.repository.ExperienceRepository
import com.isaakhanimann.journal.data.repository.SubstanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val permissions: List<Permission>,
    val entryPoint: String,
    val dependencies: List<String> = emptyList()
)

@Serializable
enum class Permission {
    READ_EXPERIENCES,
    WRITE_EXPERIENCES,
    READ_SUBSTANCES,
    NETWORK_ACCESS,
    FILE_SYSTEM_ACCESS,
    BIOMETRIC_DATA,
    EXPORT_DATA,
    IMPORT_DATA,
    SEND_NOTIFICATIONS,
    ANALYTICS_ACCESS
}

data class PluginInfo(
    val manifest: PluginManifest,
    val isEnabled: Boolean,
    val isLoaded: Boolean,
    val error: String? = null
)

interface Plugin {
    val manifest: PluginManifest
    suspend fun initialize(context: PluginContext): Result<Unit>
    suspend fun shutdown(): Result<Unit>
    fun getCapabilities(): List<PluginCapability>
}

abstract class PluginCapability(
    val id: String,
    val name: String,
    val description: String
)

class AnalyticsCapability(
    id: String,
    name: String,
    description: String,
    val analyzeFunction: suspend (AnalyticsContext) -> AnalyticsResult
) : PluginCapability(id, name, description)

class VisualizationCapability(
    id: String,
    name: String,
    description: String,
    val visualizationComponent: @Composable (VisualizationContext) -> Unit
) : PluginCapability(id, name, description)

class AICapability(
    id: String,
    name: String,
    description: String,
    val processFunction: suspend (AIContext) -> AIResult
) : PluginCapability(id, name, description)

interface PluginContext {
    val experienceRepository: ExperienceRepository
    val substanceRepository: SubstanceRepository
    val dataAccess: PluginDataAccess
    val notifications: PluginNotificationService
    val preferences: PluginPreferences
}

interface PluginDataAccess {
    suspend fun readExperiences(): Flow<List<Experience>>
    suspend fun readSubstances(): Flow<List<Substance>>
    suspend fun hasPermission(permission: Permission): Boolean
}

interface PluginNotificationService {
    suspend fun showNotification(title: String, message: String, severity: NotificationSeverity)
    suspend fun showDialog(title: String, message: String, actions: List<DialogAction>): DialogResult
}

enum class NotificationSeverity { INFO, WARNING, ERROR, SUCCESS }

data class DialogAction(val label: String, val action: suspend () -> Unit)
data class DialogResult(val selectedAction: String, val cancelled: Boolean)

interface PluginPreferences {
    suspend fun getString(key: String, default: String = ""): String
    suspend fun setString(key: String, value: String)
    suspend fun getBoolean(key: String, default: Boolean = false): Boolean
    suspend fun setBoolean(key: String, value: Boolean)
}

data class AnalyticsContext(
    val experiences: List<Experience>,
    val substances: List<Substance>,
    val timeRange: TimeRange? = null,
    val filters: Map<String, Any> = emptyMap()
)

data class AnalyticsResult(
    val insights: List<Insight>,
    val recommendations: List<Recommendation>,
    val riskAssessment: RiskAssessment? = null,
    val visualizations: List<VisualizationData> = emptyList()
)

data class Insight(
    val id: String,
    val title: String,
    val description: String,
    val confidence: Double,
    val severity: InsightSeverity,
    val metadata: Map<String, Any> = emptyMap()
)

enum class InsightSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class Recommendation(
    val id: String,
    val title: String,
    val description: String,
    val actionable: Boolean,
    val priority: RecommendationPriority,
    val category: RecommendationCategory
)

enum class RecommendationPriority { LOW, MEDIUM, HIGH, URGENT }
enum class RecommendationCategory { SAFETY, OPTIMIZATION, HEALTH, INTEGRATION, TIMING }

data class RiskAssessment(
    val overallRisk: Double,
    val riskFactors: List<RiskFactor>,
    val mitigationStrategies: List<String>
)

data class RiskFactor(
    val factor: String,
    val severity: Double,
    val description: String
)

data class VisualizationData(
    val type: VisualizationType,
    val data: Map<String, Any>,
    val title: String,
    val description: String? = null
)

enum class VisualizationType { 
    LINE_CHART, BAR_CHART, SCATTER_PLOT, HEAT_MAP, 
    NETWORK_GRAPH, TIMELINE, CALENDAR, GAUGE 
}

data class VisualizationContext(
    val data: VisualizationData,
    val interactive: Boolean = true,
    val exportable: Boolean = true
)

data class AIContext(
    val query: String,
    val context: Map<String, Any>,
    val userHistory: List<Experience>,
    val preferences: Map<String, Any>
)

data class AIResult(
    val response: String,
    val confidence: Double,
    val suggestions: List<String> = emptyList(),
    val followUpQuestions: List<String> = emptyList()
)

data class TimeRange(
    val start: kotlinx.datetime.Instant,
    val end: kotlinx.datetime.Instant
)

interface PluginManager {
    val installedPlugins: StateFlow<List<PluginInfo>>
    val enabledPlugins: StateFlow<List<Plugin>>
    
    suspend fun loadPlugin(pluginPath: String): Result<Plugin>
    suspend fun unloadPlugin(pluginId: String): Result<Unit>
    suspend fun enablePlugin(pluginId: String): Result<Unit>
    suspend fun disablePlugin(pluginId: String): Result<Unit>
    suspend fun installPlugin(pluginPackage: ByteArray): Result<Plugin>
    suspend fun uninstallPlugin(pluginId: String): Result<Unit>
    
    fun getPluginCapabilities(type: KClass<out PluginCapability>): List<PluginCapability>
    suspend fun executeAnalytics(context: AnalyticsContext): List<AnalyticsResult>
    suspend fun queryAI(context: AIContext): List<AIResult>
}