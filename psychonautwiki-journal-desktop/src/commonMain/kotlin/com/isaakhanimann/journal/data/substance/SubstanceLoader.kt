package com.isaakhanimann.journal.data.substance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads substance data from the bundled JSON file
 */
object SubstanceLoader {
    
    suspend fun loadSubstances(): List<SubstanceInfo> {
        return withContext(Dispatchers.IO) {
            PsychonautWikiDatabase.getAllSubstances()
        }
    }
    
    suspend fun findSubstanceByName(name: String): SubstanceInfo? {
        return withContext(Dispatchers.IO) {
            PsychonautWikiDatabase.getSubstanceByName(name)
        }
    }
    
    suspend fun searchSubstances(query: String): List<SubstanceInfo> {
        return withContext(Dispatchers.IO) {
            PsychonautWikiDatabase.searchSubstances(query)
        }
    }
}