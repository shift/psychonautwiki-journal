package com.isaakhanimann.journal.data.repository

import com.isaakhanimann.journal.database.Database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers

interface PreferencesRepository {
    fun getThemeMode(): Flow<String>
    suspend fun setThemeMode(mode: String)
    fun getWindowWidth(): Flow<Int>
    fun getWindowHeight(): Flow<Int>
    fun getWindowX(): Flow<Int>
    fun getWindowY(): Flow<Int>
    suspend fun setWindowSize(width: Int, height: Int)
    suspend fun setWindowPosition(x: Int, y: Int)
    suspend fun setPreference(key: String, value: String)
    fun getPreference(key: String, defaultValue: String = ""): Flow<String>
}

class PreferencesRepositoryImpl(private val database: Database) : PreferencesRepository {
    
    // Theme preferences
    override fun getThemeMode(): Flow<String> = 
        database.journalQueries.selectPreferenceByKey("theme_mode")
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { query ->
                query?.value ?: "system"
            }
    
    override suspend fun setThemeMode(mode: String) {
        database.journalQueries.insertOrReplacePreference("theme_mode", mode)
    }
    
    // Window preferences
    override fun getWindowWidth(): Flow<Int> = 
        database.journalQueries.selectPreferenceByKey("window_width")
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { query ->
                query?.value?.toIntOrNull() ?: 1200
            }
    
    override fun getWindowHeight(): Flow<Int> = 
        database.journalQueries.selectPreferenceByKey("window_height")
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { query ->
                query?.value?.toIntOrNull() ?: 800
            }
    
    override fun getWindowX(): Flow<Int> = 
        database.journalQueries.selectPreferenceByKey("window_x")
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { query ->
                query?.value?.toIntOrNull() ?: 100
            }
    
    override fun getWindowY(): Flow<Int> = 
        database.journalQueries.selectPreferenceByKey("window_y")
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { query ->
                query?.value?.toIntOrNull() ?: 100
            }
    
    override suspend fun setWindowSize(width: Int, height: Int) {
        database.journalQueries.insertOrReplacePreference("window_width", width.toString())
        database.journalQueries.insertOrReplacePreference("window_height", height.toString())
    }
    
    override suspend fun setWindowPosition(x: Int, y: Int) {
        database.journalQueries.insertOrReplacePreference("window_x", x.toString())
        database.journalQueries.insertOrReplacePreference("window_y", y.toString())
    }
    
    // Other preferences can be added here
    override suspend fun setPreference(key: String, value: String) {
        database.journalQueries.insertOrReplacePreference(key, value)
    }
    
    override fun getPreference(key: String, defaultValue: String): Flow<String> = 
        database.journalQueries.selectPreferenceByKey(key)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { query ->
                query?.value ?: defaultValue
            }
}