package com.isaakhanimann.journal.data.repository

import com.isaakhanimann.journal.data.model.*
import com.isaakhanimann.journal.data.substance.SubstanceInfo
import com.isaakhanimann.journal.data.substance.SubstanceLoader
import com.isaakhanimann.journal.database.Database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers

interface SubstanceRepository {
    fun getAllSubstanceCompanions(): Flow<List<SubstanceCompanion>>
    fun getSubstanceCompanionByName(name: String): Flow<SubstanceCompanion?>
    suspend fun insertOrUpdateSubstanceCompanion(companion: SubstanceCompanion)
    suspend fun deleteSubstanceCompanion(name: String)
    
    fun getAllCustomSubstances(): Flow<List<CustomSubstance>>
    fun getCustomSubstanceById(id: Int): Flow<CustomSubstance?>
    suspend fun insertCustomSubstance(substance: CustomSubstance): Long
    suspend fun updateCustomSubstance(substance: CustomSubstance)
    suspend fun deleteCustomSubstance(id: Int)
    
    fun getCustomUnitsBySubstance(substanceName: String): Flow<List<CustomUnit>>
    fun getAllCustomUnits(): Flow<List<CustomUnit>>
    fun getCustomUnitById(id: Int): Flow<CustomUnit?>
    suspend fun insertCustomUnit(unit: CustomUnit): Long
    suspend fun updateCustomUnit(unit: CustomUnit)
    suspend fun deleteCustomUnit(id: Int)
    
    // PsychonautWiki substance database methods
    suspend fun getAllSubstances(): List<SubstanceInfo>
    suspend fun findSubstanceByName(name: String): SubstanceInfo?
    suspend fun searchSubstances(query: String): List<SubstanceInfo>
}

class SubstanceRepositoryImpl(
    private val database: Database
) : SubstanceRepository {
    
    private val queries = database.journalQueries
    
    override fun getAllSubstanceCompanions(): Flow<List<SubstanceCompanion>> {
        return queries.selectAllSubstanceCompanions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { companionList ->
                companionList.map { companion ->
                    SubstanceCompanion(
                        substanceName = companion.substanceName,
                        color = AdaptiveColor.valueOf(companion.color)
                    )
                }
            }
    }
    
    override fun getSubstanceCompanionByName(name: String): Flow<SubstanceCompanion?> {
        return queries.selectSubstanceCompanionByName(name)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { companion ->
                companion?.let {
                    SubstanceCompanion(
                        substanceName = it.substanceName,
                        color = AdaptiveColor.valueOf(it.color)
                    )
                }
            }
    }
    
    override suspend fun insertOrUpdateSubstanceCompanion(companion: SubstanceCompanion) {
        queries.insertOrReplaceSubstanceCompanion(
            substanceName = companion.substanceName,
            color = companion.color.name
        )
    }
    
    override suspend fun deleteSubstanceCompanion(name: String) {
        queries.deleteSubstanceCompanion(name)
    }
    
    override fun getAllCustomSubstances(): Flow<List<CustomSubstance>> {
        return queries.selectAllCustomSubstances()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { substanceList ->
                substanceList.map { substance ->
                    CustomSubstance(
                        id = substance.id.toInt(),
                        name = substance.name,
                        units = substance.units,
                        description = substance.description
                    )
                }
            }
    }
    
    override fun getCustomSubstanceById(id: Int): Flow<CustomSubstance?> {
        return queries.selectCustomSubstanceById(id.toLong())
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { substance ->
                substance?.let {
                    CustomSubstance(
                        id = it.id.toInt(),
                        name = it.name,
                        units = it.units,
                        description = it.description
                    )
                }
            }
    }
    
    override suspend fun insertCustomSubstance(substance: CustomSubstance): Long {
        return queries.transactionWithResult {
            queries.insertCustomSubstance(
                name = substance.name,
                units = substance.units,
                description = substance.description
            )
            queries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun updateCustomSubstance(substance: CustomSubstance) {
        queries.updateCustomSubstance(
            name = substance.name,
            units = substance.units,
            description = substance.description,
            id = substance.id.toLong()
        )
    }
    
    override suspend fun deleteCustomSubstance(id: Int) {
        queries.deleteCustomSubstance(id.toLong())
    }
    
    override fun getCustomUnitsBySubstance(substanceName: String): Flow<List<CustomUnit>> {
        return queries.selectCustomUnitsBySubstance(substanceName)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { unitList ->
                unitList.map { unit ->
                    CustomUnit(
                        id = unit.id.toInt(),
                        substanceName = unit.substanceName,
                        name = unit.name,
                        creationDate = Instant.fromEpochSeconds(unit.creationDate),
                        administrationRoute = AdministrationRoute.valueOf(unit.administrationRoute),
                        dose = unit.dose,
                        estimatedDoseStandardDeviation = unit.estimatedDoseStandardDeviation,
                        isEstimate = unit.isEstimate == 1L,
                        isArchived = unit.isArchived == 1L,
                        unit = unit.unit,
                        unitPlural = unit.unitPlural,
                        originalUnit = unit.originalUnit,
                        note = unit.note
                    )
                }
            }
    }
    
    override fun getAllCustomUnits(): Flow<List<CustomUnit>> {
        return queries.selectAllCustomUnits()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { unitList ->
                unitList.map { unit ->
                    CustomUnit(
                        id = unit.id.toInt(),
                        substanceName = unit.substanceName,
                        name = unit.name,
                        creationDate = Instant.fromEpochSeconds(unit.creationDate),
                        administrationRoute = AdministrationRoute.valueOf(unit.administrationRoute),
                        dose = unit.dose,
                        estimatedDoseStandardDeviation = unit.estimatedDoseStandardDeviation,
                        isEstimate = unit.isEstimate == 1L,
                        isArchived = unit.isArchived == 1L,
                        unit = unit.unit,
                        unitPlural = unit.unitPlural,
                        originalUnit = unit.originalUnit,
                        note = unit.note
                    )
                }
            }
    }
    
    override fun getCustomUnitById(id: Int): Flow<CustomUnit?> {
        return queries.selectCustomUnitById(id.toLong())
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { unit ->
                unit?.let {
                    CustomUnit(
                        id = it.id.toInt(),
                        substanceName = it.substanceName,
                        name = it.name,
                        creationDate = Instant.fromEpochSeconds(it.creationDate),
                        administrationRoute = AdministrationRoute.valueOf(it.administrationRoute),
                        dose = it.dose,
                        estimatedDoseStandardDeviation = it.estimatedDoseStandardDeviation,
                        isEstimate = it.isEstimate == 1L,
                        isArchived = it.isArchived == 1L,
                        unit = it.unit,
                        unitPlural = it.unitPlural,
                        originalUnit = it.originalUnit,
                        note = it.note
                    )
                }
            }
    }
    
    override suspend fun insertCustomUnit(unit: CustomUnit): Long {
        return queries.transactionWithResult {
            queries.insertCustomUnit(
                substanceName = unit.substanceName,
                name = unit.name,
                creationDate = unit.creationDate.epochSeconds,
                administrationRoute = unit.administrationRoute.name,
                dose = unit.dose,
                estimatedDoseStandardDeviation = unit.estimatedDoseStandardDeviation,
                isEstimate = if (unit.isEstimate) 1L else 0L,
                isArchived = if (unit.isArchived) 1L else 0L,
                unit = unit.unit,
                unitPlural = unit.unitPlural,
                originalUnit = unit.originalUnit,
                note = unit.note
            )
            queries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun updateCustomUnit(unit: CustomUnit) {
        queries.updateCustomUnit(
            name = unit.name,
            administrationRoute = unit.administrationRoute.name,
            dose = unit.dose,
            estimatedDoseStandardDeviation = unit.estimatedDoseStandardDeviation,
            isEstimate = if (unit.isEstimate) 1L else 0L,
            isArchived = if (unit.isArchived) 1L else 0L,
            unit = unit.unit,
            unitPlural = unit.unitPlural,
            originalUnit = unit.originalUnit,
            note = unit.note,
            id = unit.id.toLong()
        )
    }
    
    override suspend fun deleteCustomUnit(id: Int) {
        queries.deleteCustomUnit(id.toLong())
    }
    
    // PsychonautWiki substance database methods
    override suspend fun getAllSubstances(): List<SubstanceInfo> {
        return SubstanceLoader.loadSubstances()
    }
    
    override suspend fun findSubstanceByName(name: String): SubstanceInfo? {
        return SubstanceLoader.findSubstanceByName(name)
    }
    
    override suspend fun searchSubstances(query: String): List<SubstanceInfo> {
        return SubstanceLoader.searchSubstances(query)
    }
}

interface PreferencesRepository {
    fun getAllPreferences(): Flow<List<UserPreference>>
    fun getPreferenceByKey(key: String): Flow<UserPreference?>
    suspend fun setPreference(key: String, value: String)
    suspend fun deletePreference(key: String)
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
}