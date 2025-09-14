package com.isaakhanimann.journal.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import com.isaakhanimann.journal.data.model.AdministrationRoute
import com.isaakhanimann.journal.data.model.StomachFullness

interface DraftManager {
    suspend fun saveExperienceDraft(formId: String, draft: ExperienceDraft)
    suspend fun getExperienceDraft(formId: String): ExperienceDraft?
    suspend fun clearExperienceDraft(formId: String)
    suspend fun saveIngestionDraft(formId: String, draft: IngestionDraft)
    suspend fun getIngestionDraft(formId: String): IngestionDraft?
    suspend fun clearIngestionDraft(formId: String)
    suspend fun clearAllDrafts()
    fun getAllDraftKeys(): Flow<List<String>>
}

class DraftManagerImpl(
    private val preferencesRepository: PreferencesRepository
) : DraftManager {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    companion object {
        private const val EXPERIENCE_DRAFT_PREFIX = "draft_experience_"
        private const val INGESTION_DRAFT_PREFIX = "draft_ingestion_"
        private const val DRAFT_KEYS_KEY = "draft_keys"
    }
    
    override suspend fun saveExperienceDraft(formId: String, draft: ExperienceDraft) {
        val key = "$EXPERIENCE_DRAFT_PREFIX$formId"
        val jsonString = json.encodeToString(draft)
        preferencesRepository.setPreference(key, jsonString)
        addDraftKey(key)
    }
    
    override suspend fun getExperienceDraft(formId: String): ExperienceDraft? {
        val key = "$EXPERIENCE_DRAFT_PREFIX$formId"
        return try {
            val jsonString = preferencesRepository.getPreference(key).first()
            if (jsonString.isBlank()) null
            else json.decodeFromString<ExperienceDraft>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun clearExperienceDraft(formId: String) {
        val key = "$EXPERIENCE_DRAFT_PREFIX$formId"
        preferencesRepository.setPreference(key, "")
        removeDraftKey(key)
    }
    
    override suspend fun saveIngestionDraft(formId: String, draft: IngestionDraft) {
        val key = "$INGESTION_DRAFT_PREFIX$formId"
        val jsonString = json.encodeToString(draft)
        preferencesRepository.setPreference(key, jsonString)
        addDraftKey(key)
    }
    
    override suspend fun getIngestionDraft(formId: String): IngestionDraft? {
        val key = "$INGESTION_DRAFT_PREFIX$formId"
        return try {
            val jsonString = preferencesRepository.getPreference(key).first()
            if (jsonString.isBlank()) null
            else json.decodeFromString<IngestionDraft>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun clearIngestionDraft(formId: String) {
        val key = "$INGESTION_DRAFT_PREFIX$formId"
        preferencesRepository.setPreference(key, "")
        removeDraftKey(key)
    }
    
    override suspend fun clearAllDrafts() {
        // This would need implementation based on available APIs
        // For now, we'd need to track all draft keys
        preferencesRepository.setPreference(DRAFT_KEYS_KEY, "")
    }
    
    override fun getAllDraftKeys(): Flow<List<String>> {
        return preferencesRepository.getPreference(DRAFT_KEYS_KEY, "")
    }
    
    private suspend fun addDraftKey(key: String) {
        // Track draft keys for cleanup purposes
        val existingKeys = preferencesRepository.getPreference(DRAFT_KEYS_KEY, "").first()
        val keysList = if (existingKeys.isBlank()) emptyList() 
                      else existingKeys.split(",").filter { it.isNotBlank() }
        
        if (key !in keysList) {
            val updatedKeys = (keysList + key).joinToString(",")
            preferencesRepository.setPreference(DRAFT_KEYS_KEY, updatedKeys)
        }
    }
    
    private suspend fun removeDraftKey(key: String) {
        val existingKeys = preferencesRepository.getPreference(DRAFT_KEYS_KEY, "").first()
        val keysList = if (existingKeys.isBlank()) emptyList() 
                      else existingKeys.split(",").filter { it.isNotBlank() }
        
        val updatedKeys = keysList.filter { it != key }.joinToString(",")
        preferencesRepository.setPreference(DRAFT_KEYS_KEY, updatedKeys)
    }
}

@Serializable
data class ExperienceDraft(
    val title: String = "",
    val text: String = "",
    val isFavorite: Boolean = false,
    val locationName: String = "",
    val locationLatitude: String = "",
    val locationLongitude: String = "",
    val sortDate: Long = Clock.System.now().toEpochMilliseconds(),
    val lastSaved: Long = Clock.System.now().toEpochMilliseconds()
) {
    fun isEmpty(): Boolean = title.isBlank() && text.isBlank() && locationName.isBlank()
    
    fun hasSubstantialContent(): Boolean = title.trim().length > 3 || text.trim().length > 10
}

@Serializable
data class IngestionDraft(
    val substanceName: String = "",
    val dose: String = "",
    val units: String = "",
    val administrationRoute: String = AdministrationRoute.ORAL.name,
    val time: Long = Clock.System.now().toEpochMilliseconds(),
    val endTime: Long? = null,
    val notes: String = "",
    val isDoseAnEstimate: Boolean = false,
    val stomachFullness: String? = null,
    val consumerName: String = "",
    val lastSaved: Long = Clock.System.now().toEpochMilliseconds()
) {
    fun isEmpty(): Boolean = substanceName.isBlank() && dose.isBlank() && notes.isBlank()
    
    fun hasSubstantialContent(): Boolean = substanceName.trim().length > 2 || dose.isNotBlank()
}