package com.isaakhanimann.journal.test

import com.isaakhanimann.journal.data.substance.PsychonautWikiDatabase

fun main() {
    println("Testing substance database loading...")
    try {
        val substances = PsychonautWikiDatabase.getAllSubstances()
        println("✅ Successfully loaded ${substances.size} substances")
        
        if (substances.size >= 700) {
            println("✅ Full database loaded (${substances.size} substances)")
            
            // Show some examples
            substances.take(5).forEach { substance ->
                println("- ${substance.name} (${substance.categories.joinToString(", ")})")
            }
            
            // Check for some specific substances
            val lsd = substances.find { it.name.equals("LSD", ignoreCase = true) }
            val mdma = substances.find { it.name.equals("MDMA", ignoreCase = true) }
            val cannabis = substances.find { it.name.equals("Cannabis", ignoreCase = true) }
            
            println("\nSpecific substance checks:")
            println("LSD found: ${lsd != null}")
            println("MDMA found: ${mdma != null}")
            println("Cannabis found: ${cannabis != null}")
            
        } else {
            println("❌ Only ${substances.size} substances loaded - appears to be fallback data")
        }
    } catch (e: Exception) {
        println("❌ Failed to load substances: ${e.message}")
        e.printStackTrace()
    }
}