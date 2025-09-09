package com.isaakhanimann.journal.ui.viewmodel

import androidx.compose.runtime.*
import com.isaakhanimann.journal.data.repository.SubstanceRepository
import com.isaakhanimann.journal.data.substance.SubstanceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SubstancesViewModel(
    private val substanceRepository: SubstanceRepository
) {
    
    private val _substances = mutableStateOf<List<SubstanceInfo>>(emptyList())
    val substances: State<List<SubstanceInfo>> = _substances
    
    private val _filteredSubstances = mutableStateOf<List<SubstanceInfo>>(emptyList())
    val filteredSubstances: State<List<SubstanceInfo>> = _filteredSubstances
    
    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading
    
    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery
    
    private val _selectedCategory = mutableStateOf("All")
    val selectedCategory: State<String> = _selectedCategory
    
    private val _categories = mutableStateOf<List<String>>(emptyList())
    val categories: State<List<String>> = _categories
    
    init {
        loadSubstances()
    }
    
    private fun loadSubstances() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                _isLoading.value = true
                val allSubstances = substanceRepository.getAllSubstances()
                _substances.value = allSubstances
                
                // Extract categories
                val allCategories = mutableSetOf<String>()
                allCategories.add("All")
                allSubstances.forEach { substance ->
                    allCategories.addAll(substance.categories)
                }
                _categories.value = allCategories.sorted()
                
                // Initial filter
                filterSubstances()
                
                println("✅ SubstancesViewModel loaded ${allSubstances.size} substances")
                
                // Log database health information
                val health = com.isaakhanimann.journal.data.substance.PsychonautWikiDatabase.getDatabaseHealth()
                if (health != null) {
                    println("💊 Database health: ${health.loadedSuccessfully}/${health.totalSubstances} substances loaded successfully")
                    println("⚡ Load time: ${health.loadTimeMs}ms")
                    println("📈 Features: ${health.substancesWithRoas} with dosing, ${health.substancesWithBioavailability} with bioavailability")
                }
            } catch (e: Exception) {
                println("❌ SubstancesViewModel failed to load substances: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterSubstances()
    }
    
    fun updateSelectedCategory(category: String) {
        _selectedCategory.value = category
        filterSubstances()
    }
    
    private fun filterSubstances() {
        val query = _searchQuery.value
        val category = _selectedCategory.value
        
        val filtered = _substances.value.filter { substance ->
            val matchesSearch = query.isBlank() || 
                substance.name.contains(query, ignoreCase = true) ||
                substance.commonNames.any { it.contains(query, ignoreCase = true) } ||
                (substance.summary?.contains(query, ignoreCase = true) == true)
            
            val matchesCategory = category == "All" || substance.categories.contains(category)
            
            matchesSearch && matchesCategory
        }
        
        _filteredSubstances.value = filtered
    }
}