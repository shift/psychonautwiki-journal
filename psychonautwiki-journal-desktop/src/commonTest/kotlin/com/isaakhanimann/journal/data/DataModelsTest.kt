package com.isaakhanimann.journal.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.isaakhanimann.journal.data.model.*
import com.isaakhanimann.journal.data.substance.PsychonautWikiDatabase
import kotlinx.datetime.Clock

class DataModelsTest : StringSpec({
    
    "Experience should create with valid data" {
        val experience = Experience(
            id = 1,
            title = "Test Experience",
            text = "This is a test experience",
            creationDate = Clock.System.now(),
            sortDate = Clock.System.now()
        )
        
        experience.title shouldBe "Test Experience"
        experience.isFavorite shouldBe false
    }
    
    "AdministrationRoute should have display names" {
        AdministrationRoute.ORAL.displayName shouldBe "Oral"
        AdministrationRoute.INSUFFLATED.displayName shouldBe "Insufflated"
        AdministrationRoute.INTRAVENOUS.displayName shouldBe "Intravenous"
    }
    
    "ShulginRatingOption should have display names and descriptions" {
        ShulginRatingOption.PLUS.displayName shouldBe "+"
        ShulginRatingOption.THREE_PLUS.displayName shouldBe "+++"
        ShulginRatingOption.PLUS.description shouldBe "Light effect"
        ShulginRatingOption.FOUR_PLUS.description shouldBe "Very strong effect"
    }
    
    "PsychonautWikiDatabase should find substances by name" {
        val alcohol = PsychonautWikiDatabase.getSubstanceByName("Alcohol")
        alcohol shouldNotBe null
        alcohol?.name shouldBe "Alcohol"
        
        val ethanol = PsychonautWikiDatabase.getSubstanceByName("Ethanol")
        ethanol shouldNotBe null
        ethanol?.name shouldBe "Alcohol" // Should find by common name
    }
    
    "PsychonautWikiDatabase should search substances" {
        val results = PsychonautWikiDatabase.searchSubstances("caf")
        results.shouldNotBeEmpty()
        results.any { it.name.contains("Caffeine", ignoreCase = true) } shouldBe true
        
        val emptyResults = PsychonautWikiDatabase.searchSubstances("nonexistent")
        emptyResults.size shouldBe 0
    }
})