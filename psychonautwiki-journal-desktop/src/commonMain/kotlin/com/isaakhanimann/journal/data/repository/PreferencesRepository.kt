package com.isaakhanimann.journal.data.repository

import com.isaakhanimann.journal.data.model.UserPreference
import com.isaakhanimann.journal.database.Database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

interface PreferencesRepository {
    fun getAllPreferences(): Flow<List<UserPreference>>
    fun getPreferenceByKey(key: String): Flow<UserPreference?>
    suspend fun setPreference(key: String, value: String)
    suspend fun deletePreference(key: String)
    
    // Convenience methods for plugin system
    suspend fun getString(key: String, default: String = ""): String
    suspend fun setString(key: String, value: String)
    suspend fun getBoolean(key: String, default: Boolean = false): Boolean
    suspend fun setBoolean(key: String, value: Boolean)
    suspend fun getInt(key: String, default: Int = 0): Int
    suspend fun setInt(key: String, value: Int)
    suspend fun getDouble(key: String, default: Double = 0.0): Double
    suspend fun setDouble(key: String, value: Double)
}

class PreferencesRepositoryImpl(
    private val database: Database
) : PreferencesRepository {
    
    private val queries = database.journalQueries
    
    override fun getAllPreferences(): Flow<List<UserPreference>> {
        return queries.selectAllPreferences()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { prefList ->
                prefList.map { pref ->
                    UserPreference(
                        id = pref.id.toInt(),
                        key = pref.key,
                        value = pref.value_,
                        createdAt = Instant.fromEpochSeconds(pref.created_at),
                        updatedAt = Instant.fromEpochSeconds(pref.updated_at)
                    )
                }
            }
    }
    
    override fun getPreferenceByKey(key: String): Flow<UserPreference?> {
        return queries.selectPreferenceByKey(key)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { pref ->
                pref?.let {
                    UserPreference(
                        id = it.id.toInt(),
                        key = it.key,
                        value = it.value_,
                        createdAt = Instant.fromEpochSeconds(it.created_at),
                        updatedAt = Instant.fromEpochSeconds(it.updated_at)
                    )
                }
            }
    }
    
    override suspend fun setPreference(key: String, value: String) {
        queries.insertOrReplacePreference(key = key, value_ = value)
    }
    
    override suspend fun deletePreference(key: String) {
        queries.deletePreference(key)
    }
    
    // Convenience methods for plugin system
    override suspend fun getString(key: String, default: String): String {
        return try {
            getPreferenceByKey(key).first()?.value ?: default
        } catch (e: Exception) {
            default
        }
    }
    
    override suspend fun setString(key: String, value: String) {
        setPreference(key, value)
    }
    
    override suspend fun getBoolean(key: String, default: Boolean): Boolean {
        return getString(key, default.toString()).toBooleanStrictOrNull() ?: default
    }
    
    override suspend fun setBoolean(key: String, value: Boolean) {
        setString(key, value.toString())
    }
    
    override suspend fun getInt(key: String, default: Int): Int {
        return getString(key, default.toString()).toIntOrNull() ?: default
    }
    
    override suspend fun setInt(key: String, value: Int) {
        setString(key, value.toString())
    }
    
    override suspend fun getDouble(key: String, default: Double): Double {
        return getString(key, default.toString()).toDoubleOrNull() ?: default
    }
    
    override suspend fun setDouble(key: String, value: Double) {
        setString(key, value.toString())
    }
}