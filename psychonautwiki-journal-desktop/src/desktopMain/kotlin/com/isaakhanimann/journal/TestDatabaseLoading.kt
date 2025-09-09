package com.isaakhanimann.journal.data.substance

import kotlinx.serialization.json.Json

fun main() {
    println("Testing PsychonautWiki database loading...")
    
    try {
        val substances = PsychonautWikiDatabase.getAllSubstances()
        println("✅ Successfully loaded ${substances.size} substances")
        
        // Test search functionality
        val lsdResults = PsychonautWikiDatabase.searchSubstances("lsd")
        println("✅ Search for 'lsd' found ${lsdResults.size} results")
        
        // Test substance with problematic duration range
        val problematicSubstance = substances.find { it.name == "4-AcO-DiPT" }
        if (problematicSubstance != null) {
            println("✅ Successfully loaded substance with missing max field: ${problematicSubstance.name}")
        } else {
            println("⚠️ Could not find substance with missing max field")
        }
        
        // Show first few substances
        println("\nFirst 10 substances:")
        substances.take(10).forEach { substance ->
            println("- ${substance.name} (${substance.categories.joinToString(", ")})")
        }
        
    } catch (e: Exception) {
        println("❌ Failed to load database: ${e.message}")
        e.printStackTrace()
    }
}