package com.isaakhanimann.journal.test

import com.isaakhanimann.journal.data.substance.PsychonautWikiDatabase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.shouldBe

class SubstanceDatabaseTest : StringSpec({
    "should load substances from JSON database" {
        val substances = PsychonautWikiDatabase.getAllSubstances()
        substances.size shouldBeGreaterThan 5
        println("✅ Loaded ${substances.size} substances from database")
    }
    
    "should handle missing max fields in duration ranges" {
        val substances = PsychonautWikiDatabase.getAllSubstances()
        // Find substance with missing max field (should now load successfully)
        val substanceWithIssue = substances.find { it.name == "4-AcO-DiPT" }
        substanceWithIssue shouldNotBe null
        println("✅ Successfully loaded substance with problematic duration range")
    }
    
    "should be able to search substances" {
        val results = PsychonautWikiDatabase.searchSubstances("lsd")
        (results.size > 0) shouldBe true
        println("✅ Search functionality works with ${results.size} results")
    }
})