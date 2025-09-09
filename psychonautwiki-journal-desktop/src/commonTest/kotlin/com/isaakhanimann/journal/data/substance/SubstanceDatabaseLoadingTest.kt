package com.isaakhanimann.journal.data.substance

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan

class SubstanceDatabaseLoadingTest : StringSpec({

    "should load complete substance database from JSON" {
        val substances = PsychonautWikiDatabase.getAllSubstances()
        
        // Should load significantly more than 5 fallback substances
        substances.size shouldBeGreaterThan 100
        println("✅ Loaded ${substances.size} substances")
        
        // Should contain known substances
        val substanceNames = substances.map { it.name.lowercase() }
        substanceNames.contains("bromantane") shouldBe true
        substanceNames.contains("lsd") shouldBe true
        substanceNames.contains("mdma") shouldBe true
    }
    
    "should handle bioavailability data correctly" {
        val substances = PsychonautWikiDatabase.getAllSubstances()
        
        // Find substances with bioavailability data
        val substancesWithBioav = substances.filter { substance ->
            substance.roas.any { it.bioavailability != null }
        }
        
        substancesWithBioav.shouldNotBeEmpty()
        println("Found ${substancesWithBioav.size} substances with bioavailability data")
        
        // Check that bioavailability parsing works correctly
        val bromantane = substances.find { it.name.equals("Bromantane", ignoreCase = true) }
        bromantane shouldNotBe null
        
        if (bromantane != null) {
            val oralRoute = bromantane.roas.find { it.name.equals("oral", ignoreCase = true) }
            oralRoute shouldNotBe null
            oralRoute?.bioavailability shouldNotBe null
            oralRoute?.bioavailability?.max shouldBe 42.0
            println("✅ Bromantane oral bioavailability: max=${oralRoute?.bioavailability?.max}%")
        }
    }
    
    "should provide search functionality" {
        val results = PsychonautWikiDatabase.searchSubstances("mdma")
        results.shouldNotBeEmpty()
        
        val mdma = results.find { it.name.equals("MDMA", ignoreCase = true) }
        mdma shouldNotBe null
        mdma?.commonNames?.contains("Ecstasy") shouldBe true
    }
})