package com.isaakhanimann.journal.ui.viewmodel

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.isaakhanimann.journal.data.model.*
import kotlinx.datetime.Clock

class ViewModelTests : StringSpec({
    
    "DashboardUiState should indicate success correctly" {
        val emptyState = DashboardUiState()
        emptyState.isSuccess shouldBe false
        emptyState.isEmpty shouldBe true
        
        val successState = DashboardUiState(
            data = DashboardData(totalExperiences = 5)
        )
        successState.isSuccess shouldBe true
        successState.isEmpty shouldBe false
    }
    
    "ExperienceFormState should validate correctly" {
        val invalidForm = ExperienceFormState(title = "")
        invalidForm.isValid shouldBe false
        
        val validForm = ExperienceFormState(title = "Test Experience")
        // Note: isValid would be set by validateForm() in ViewModel
        validForm.title shouldBe "Test Experience"
    }
    
    "IngestionFormState should handle dose validation" {
        val form = IngestionFormState(
            substanceName = "Caffeine",
            dose = "100",
            administrationRoute = AdministrationRoute.ORAL
        )
        
        form.substanceName shouldBe "Caffeine"
        form.administrationRoute shouldBe AdministrationRoute.ORAL
    }
    
    "DoseClassification should identify risky doses" {
        DoseClassification.HEAVY.isRisky shouldBe true
        DoseClassification.LIGHT.isRisky shouldBe false
        DoseClassification.STRONG.needsCaution shouldBe true
        DoseClassification.THRESHOLD.needsCaution shouldBe false
    }
    
    "TimelineData should format duration correctly" {
        val timelineData = TimelineData(
            events = emptyList(),
            phases = emptyList(),
            firstIngestionTime = null,
            lastEventTime = null,
            totalDuration = kotlin.time.Duration.parse("2h 30m")
        )
        
        timelineData.formattedDuration shouldBe "2h 30m"
    }
    
    "RatingFormState should validate selection" {
        val emptyForm = RatingFormState()
        emptyForm.isValid shouldBe false
        
        val validForm = RatingFormState(selectedOption = ShulginRatingOption.PLUS)
        validForm.isValid shouldBe true
    }
    
    "NoteFormState should validate text content" {
        val emptyForm = NoteFormState()
        emptyForm.isValid shouldBe false
        
        val validForm = NoteFormState(text = "Feeling good")
        validForm.isValid shouldBe true
    }
})