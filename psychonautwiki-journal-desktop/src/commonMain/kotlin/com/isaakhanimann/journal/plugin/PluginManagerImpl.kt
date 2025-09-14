package com.isaakhanimann.journal.plugin

import com.isaakhanimann.journal.data.repository.ExperienceRepository
import com.isaakhanimann.journal.data.repository.SubstanceRepository
import com.isaakhanimann.journal.data.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile
import kotlin.reflect.KClass

class PluginManagerImpl : PluginManager, KoinComponent {
    private val experienceRepository: ExperienceRepository by inject()
    private val substanceRepository: SubstanceRepository by inject()
    private val preferencesRepository: PreferencesRepository by inject()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _installedPlugins = MutableStateFlow<List<PluginInfo>>(emptyList())
    override val installedPlugins: StateFlow<List<PluginInfo>> = _installedPlugins.asStateFlow()
    
    private val _enabledPlugins = MutableStateFlow<List<Plugin>>(emptyList())
    override val enabledPlugins: StateFlow<List<Plugin>> = _enabledPlugins.asStateFlow()
    
    private val loadedPlugins = mutableMapOf<String, Plugin>()
    private val pluginClassLoaders = mutableMapOf<String, URLClassLoader>()
    
    init {
        scope.launch {
            loadInstalledPlugins()
        }
    }
    
    private suspend fun loadInstalledPlugins() {
        val pluginDir = File(getPluginDirectory())
        if (!pluginDir.exists()) {
            pluginDir.mkdirs()
            return
        }
        
        val pluginInfos = mutableListOf<PluginInfo>()
        
        pluginDir.listFiles { file -> file.extension == "jar" }?.forEach { jarFile ->
            try {
                val manifest = readPluginManifest(jarFile)
                val isEnabled = isPluginEnabled(manifest.id)
                val pluginInfo = PluginInfo(
                    manifest = manifest,
                    isEnabled = isEnabled,
                    isLoaded = loadedPlugins.containsKey(manifest.id)
                )
                pluginInfos.add(pluginInfo)
                
                if (isEnabled) {
                    loadPlugin(jarFile.absolutePath)
                }
            } catch (e: Exception) {
                val errorInfo = PluginInfo(
                    manifest = PluginManifest(
                        id = jarFile.nameWithoutExtension,
                        name = "Unknown",
                        version = "Unknown",
                        description = "Failed to load",
                        author = "Unknown",
                        permissions = emptyList(),
                        entryPoint = ""
                    ),
                    isEnabled = false,
                    isLoaded = false,
                    error = e.message
                )
                pluginInfos.add(errorInfo)
            }
        }
        
        _installedPlugins.value = pluginInfos
    }
    
    override suspend fun loadPlugin(pluginPath: String): Result<Plugin> {
        return try {
            val jarFile = File(pluginPath)
            val manifest = readPluginManifest(jarFile)
            
            if (loadedPlugins.containsKey(manifest.id)) {
                return Result.failure(Exception("Plugin already loaded: ${manifest.id}"))
            }
            
            val classLoader = URLClassLoader(arrayOf(jarFile.toURI().toURL()))
            val pluginClass = classLoader.loadClass(manifest.entryPoint)
            val plugin = pluginClass.getDeclaredConstructor().newInstance() as Plugin
            
            val context = PluginContextImpl()
            plugin.initialize(context).getOrThrow()
            
            loadedPlugins[manifest.id] = plugin
            pluginClassLoaders[manifest.id] = classLoader
            
            updateEnabledPlugins()
            updatePluginInfo(manifest.id, isLoaded = true)
            
            Result.success(plugin)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun unloadPlugin(pluginId: String): Result<Unit> {
        return try {
            val plugin = loadedPlugins[pluginId] 
                ?: return Result.failure(Exception("Plugin not loaded: $pluginId"))
            
            plugin.shutdown().getOrThrow()
            
            loadedPlugins.remove(pluginId)
            pluginClassLoaders[pluginId]?.close()
            pluginClassLoaders.remove(pluginId)
            
            updateEnabledPlugins()
            updatePluginInfo(pluginId, isLoaded = false)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun enablePlugin(pluginId: String): Result<Unit> {
        return try {
            setPluginEnabled(pluginId, true)
            
            val pluginInfo = _installedPlugins.value.find { it.manifest.id == pluginId }
                ?: return Result.failure(Exception("Plugin not found: $pluginId"))
            
            if (!loadedPlugins.containsKey(pluginId)) {
                val pluginPath = File(getPluginDirectory(), "$pluginId.jar").absolutePath
                loadPlugin(pluginPath).getOrThrow()
            }
            
            updatePluginInfo(pluginId, isEnabled = true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun disablePlugin(pluginId: String): Result<Unit> {
        return try {
            setPluginEnabled(pluginId, false)
            unloadPlugin(pluginId).getOrThrow()
            updatePluginInfo(pluginId, isEnabled = false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun installPlugin(pluginPackage: ByteArray): Result<Plugin> {
        return try {
            // Create temporary file to read manifest
            val tempFile = File.createTempFile("plugin", ".jar")
            tempFile.writeBytes(pluginPackage)
            
            val manifest = readPluginManifest(tempFile)
            validatePluginManifest(manifest)
            
            // Copy to plugin directory
            val pluginFile = File(getPluginDirectory(), "${manifest.id}.jar")
            tempFile.copyTo(pluginFile, overwrite = true)
            tempFile.delete()
            
            // Add to installed plugins
            val pluginInfo = PluginInfo(
                manifest = manifest,
                isEnabled = false,
                isLoaded = false
            )
            
            _installedPlugins.value = _installedPlugins.value + pluginInfo
            
            // Load if auto-enable is set
            val plugin = loadPlugin(pluginFile.absolutePath).getOrThrow()
            Result.success(plugin)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun uninstallPlugin(pluginId: String): Result<Unit> {
        return try {
            disablePlugin(pluginId)
            
            val pluginFile = File(getPluginDirectory(), "$pluginId.jar")
            if (pluginFile.exists()) {
                pluginFile.delete()
            }
            
            _installedPlugins.value = _installedPlugins.value.filter { 
                it.manifest.id != pluginId 
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun getPluginCapabilities(type: KClass<out PluginCapability>): List<PluginCapability> {
        return _enabledPlugins.value.flatMap { plugin ->
            plugin.getCapabilities().filter { capability ->
                type.isInstance(capability)
            }
        }
    }
    
    override suspend fun executeAnalytics(context: AnalyticsContext): List<AnalyticsResult> {
        val analyticsCapabilities = getPluginCapabilities(AnalyticsCapability::class)
        return analyticsCapabilities.mapNotNull { capability ->
            try {
                (capability as AnalyticsCapability).analyzeFunction(context)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    override suspend fun queryAI(context: AIContext): List<AIResult> {
        val aiCapabilities = getPluginCapabilities(AICapability::class)
        return aiCapabilities.mapNotNull { capability ->
            try {
                (capability as AICapability).processFunction(context)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun readPluginManifest(jarFile: File): PluginManifest {
        JarFile(jarFile).use { jar ->
            val manifestEntry = jar.getJarEntry("plugin.json")
                ?: throw Exception("Plugin manifest not found in ${jarFile.name}")
            
            val manifestContent = jar.getInputStream(manifestEntry).readBytes().decodeToString()
            return json.decodeFromString(PluginManifest.serializer(), manifestContent)
        }
    }
    
    private fun validatePluginManifest(manifest: PluginManifest) {
        require(manifest.id.isNotBlank()) { "Plugin ID cannot be blank" }
        require(manifest.name.isNotBlank()) { "Plugin name cannot be blank" }
        require(manifest.version.isNotBlank()) { "Plugin version cannot be blank" }
        require(manifest.entryPoint.isNotBlank()) { "Plugin entry point cannot be blank" }
    }
    
    private suspend fun isPluginEnabled(pluginId: String): Boolean {
        return preferencesRepository.getBoolean("plugin_enabled_$pluginId", false)
    }
    
    private suspend fun setPluginEnabled(pluginId: String, enabled: Boolean) {
        preferencesRepository.setBoolean("plugin_enabled_$pluginId", enabled)
    }
    
    private fun updateEnabledPlugins() {
        _enabledPlugins.value = loadedPlugins.values.toList()
    }
    
    private fun updatePluginInfo(pluginId: String, isEnabled: Boolean? = null, isLoaded: Boolean? = null) {
        _installedPlugins.value = _installedPlugins.value.map { info ->
            if (info.manifest.id == pluginId) {
                info.copy(
                    isEnabled = isEnabled ?: info.isEnabled,
                    isLoaded = isLoaded ?: info.isLoaded
                )
            } else {
                info
            }
        }
    }
    
    private fun getPluginDirectory(): String {
        val userHome = System.getProperty("user.home")
        return "$userHome/.psychonautwiki-journal/plugins"
    }
    
    private inner class PluginContextImpl : PluginContext {
        override val experienceRepository = this@PluginManagerImpl.experienceRepository
        override val substanceRepository = this@PluginManagerImpl.substanceRepository
        override val dataAccess = PluginDataAccessImpl()
        override val notifications = PluginNotificationServiceImpl()
        override val preferences = PluginPreferencesImpl()
    }
    
    private inner class PluginDataAccessImpl : PluginDataAccess {
        override suspend fun readExperiences() = experienceRepository.getAllExperiences()
        override suspend fun readSubstances(): Flow<List<com.isaakhanimann.journal.data.model.Substance>> = flow { 
            emit(emptyList<com.isaakhanimann.journal.data.model.Substance>()) // Simplified implementation
        }
        override suspend fun hasPermission(permission: Permission): Boolean {
            // TODO: Implement permission checking based on loaded plugin manifest
            return true
        }
    }
    
    private inner class PluginNotificationServiceImpl : PluginNotificationService {
        override suspend fun showNotification(title: String, message: String, severity: NotificationSeverity) {
            // TODO: Integrate with system notification service
        }
        
        override suspend fun showDialog(title: String, message: String, actions: List<DialogAction>): DialogResult {
            // TODO: Integrate with UI dialog system
            return DialogResult("", true)
        }
    }
    
    private inner class PluginPreferencesImpl : PluginPreferences {
        override suspend fun getString(key: String, default: String): String {
            return preferencesRepository.getString("plugin_pref_$key", default)
        }
        
        override suspend fun setString(key: String, value: String) {
            preferencesRepository.setString("plugin_pref_$key", value)
        }
        
        override suspend fun getBoolean(key: String, default: Boolean): Boolean {
            return preferencesRepository.getBoolean("plugin_pref_$key", default)
        }
        
        override suspend fun setBoolean(key: String, value: Boolean) {
            preferencesRepository.setBoolean("plugin_pref_$key", value)
        }
    }
}