package com.isaakhanimann.journal.plugin.builtin

import com.isaakhanimann.journal.gamification.*
import com.isaakhanimann.journal.plugin.*
import com.isaakhanimann.journal.data.model.Experience
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * Integration between Smart Pattern Recognition and Gamification systems
 * Analyzes patterns to award XP and unlock achievements based on harm reduction practices
 */
class PatternGamificationIntegration(
    private val gamificationService: GamificationService,
    private val smartPatternPlugin: SmartPatternRecognitionPlugin
) {
    
    /**
     * Analyze experience patterns and award appropriate gamification rewards
     */
    suspend fun processExperienceForGamification(experience: Experience): GamificationResult {
        val events = mutableListOf<GamificationEvent>()
        
        // Base event for creating experience
        events.add(
            GamificationEvent(
                type = GamificationEventType.EXPERIENCE_CREATED,
                timestamp = Clock.System.now(),
                experienceId = experience.id.toString(),
                xpAwarded = calculateBaseExperienceXP(experience)
            )
        )
        
        // Analyze experience quality and safety practices
        val qualityAnalysis = analyzeExperienceQuality(experience)
        
        // Award bonus XP for detailed documentation
        if (qualityAnalysis.isDetailedExperience) {
            events.add(
                GamificationEvent(
                    type = GamificationEventType.EXPERIENCE_DETAILED,
                    timestamp = Clock.System.now(),
                    experienceId = experience.id.toString(),
                    metadata = mapOf(
                        "completeness_score" to qualityAnalysis.completenessScore.toString(),
                        "has_notes" to qualityAnalysis.hasNotes.toString(),
                        "has_set_setting" to qualityAnalysis.hasSetSetting.toString()
                    ),
                    xpAwarded = calculateDetailedExperienceBonus(qualityAnalysis)
                )
            )
        }
        
        // Award XP for safety practices
        qualityAnalysis.safetyPracticesUsed.forEach { practice ->
            events.add(
                GamificationEvent(
                    type = GamificationEventType.SAFETY_PRACTICE_USED,
                    timestamp = Clock.System.now(),
                    experienceId = experience.id.toString(),
                    metadata = mapOf("practice_type" to practice),
                    xpAwarded = calculateSafetyPracticeXP(practice)
                )
            )
        }
        
        // Process integration if notes indicate reflection
        if (qualityAnalysis.hasIntegrationNotes) {
            events.add(
                GamificationEvent(
                    type = GamificationEventType.INTEGRATION_COMPLETED,
                    timestamp = Clock.System.now(),
                    experienceId = experience.id.toString(),
                    metadata = mapOf("integration_quality" to qualityAnalysis.integrationQuality.toString()),
                    xpAwarded = calculateIntegrationXP(qualityAnalysis.integrationQuality)
                )
            )
        }
        
        // Process all events and aggregate results
        var totalResult = GamificationResult(
            xpAwarded = 0,
            newAchievements = emptyList(),
            streakUpdates = emptyList(),
            levelUp = false,
            newLevel = null,
            notifications = emptyList()
        )
        
        events.forEach { event ->
            val result = gamificationService.processEvent(event)
            totalResult = aggregateResults(totalResult, result)
        }
        
        return totalResult
    }
    
    /**
     * Check patterns across multiple experiences for streak and consistency achievements
     */
    suspend fun analyzeProgressPatterns(experiences: Flow<List<Experience>>): Flow<List<ProgressInsight>> {
        return experiences.map { experienceList ->
            val insights = mutableListOf<ProgressInsight>()
            
            // Analyze consistency patterns
            val consistencyInsights = analyzeConsistencyPatterns(experienceList)
            insights.addAll(consistencyInsights)
            
            // Analyze safety improvement patterns
            val safetyInsights = analyzeSafetyPatterns(experienceList)
            insights.addAll(safetyInsights)
            
            // Analyze learning progression
            val learningInsights = analyzeLearningProgression(experienceList)
            insights.addAll(learningInsights)
            
            insights
        }
    }
    
    /**
     * Generate personalized challenges based on user patterns
     */
    suspend fun generatePersonalizedChallenges(experiences: List<Experience>): List<PersonalizedChallenge> {
        val challenges = mutableListOf<PersonalizedChallenge>()
        
        // Analyze current patterns to identify improvement areas
        val patterns = analyzeUserPatterns(experiences)
        
        // Generate challenges based on identified patterns
        if (patterns.lacksSafetyPractices) {
            challenges.add(
                PersonalizedChallenge(
                    id = "safety_improvement_${Clock.System.now().epochSeconds}",
                    title = "Safety First Challenge",
                    description = "Document 3 safety practices in your next experience",
                    difficulty = ChallengeDifficulty.BEGINNER,
                    xpReward = 150L,
                    estimatedDays = 7,
                    category = AchievementCategory.SAFETY_FIRST
                )
            )
        }
        
        if (patterns.inconsistentLogging) {
            challenges.add(
                PersonalizedChallenge(
                    id = "consistency_${Clock.System.now().epochSeconds}",
                    title = "Daily Logger Challenge", 
                    description = "Log experiences for 7 consecutive days",
                    difficulty = ChallengeDifficulty.INTERMEDIATE,
                    xpReward = 300L,
                    estimatedDays = 7,
                    category = AchievementCategory.CONSISTENCY
                )
            )
        }
        
        if (patterns.lacksIntegration) {
            challenges.add(
                PersonalizedChallenge(
                    id = "integration_${Clock.System.now().epochSeconds}",
                    title = "Integration Master",
                    description = "Complete detailed integration notes for 5 experiences",
                    difficulty = ChallengeDifficulty.ADVANCED,
                    xpReward = 500L,
                    estimatedDays = 14,
                    category = AchievementCategory.INTEGRATION
                )
            )
        }
        
        return challenges
    }
    
    private suspend fun analyzeExperienceQuality(experience: Experience): ExperienceQualityAnalysis {
        var completenessScore = 0.0
        val safetyPractices = mutableListOf<String>()
        
        // Analyze completeness based on filled fields
        if (experience.title.isNotBlank()) completenessScore += 0.1
        if (experience.text.isNotBlank()) completenessScore += 0.3
        
        // Check for safety-related keywords in notes
        val safetyKeywords = listOf(
            "test", "testing", "reagent", "dosage", "scale", "measured",
            "set", "setting", "preparation", "sitter", "safe", "safety",
            "supplement", "vitamin", "magnesium", "antioxidant"
        )
        
        val experienceText = "${experience.title} ${experience.text}".lowercase()
        safetyKeywords.forEach { keyword ->
            if (keyword in experienceText) {
                safetyPractices.add(keyword)
                completenessScore += 0.05
            }
        }
        
        // Check for integration-related content
        val integrationKeywords = listOf(
            "insight", "learn", "reflect", "understand", "realize",
            "integration", "meaning", "takeaway", "lesson"
        )
        
        val hasIntegrationNotes = integrationKeywords.any { it in experienceText }
        val integrationQuality = if (hasIntegrationNotes) {
            when {
                experience.text.length > 500 -> IntegrationQuality.DEEP
                experience.text.length > 200 -> IntegrationQuality.MODERATE
                else -> IntegrationQuality.BASIC
            }
        } else {
            IntegrationQuality.NONE
        }
        
        return ExperienceQualityAnalysis(
            completenessScore = completenessScore.coerceAtMost(1.0),
            isDetailedExperience = completenessScore > 0.6,
            hasNotes = experience.text.isNotBlank(),
            hasSetSetting = "set" in experienceText || "setting" in experienceText,
            safetyPracticesUsed = safetyPractices.distinct(),
            hasIntegrationNotes = hasIntegrationNotes,
            integrationQuality = integrationQuality
        )
    }
    
    private fun calculateBaseExperienceXP(experience: Experience): Long {
        return 25L // Base XP for any experience entry
    }
    
    private fun calculateDetailedExperienceBonus(analysis: ExperienceQualityAnalysis): Long {
        val baseBonus = 25L
        val qualityMultiplier = (analysis.completenessScore * 2).coerceAtMost(2.0)
        return (baseBonus * qualityMultiplier).toLong()
    }
    
    private fun calculateSafetyPracticeXP(practice: String): Long {
        return when (practice) {
            "test", "testing", "reagent" -> 15L // Substance testing
            "dosage", "scale", "measured" -> 10L // Dosage precision
            "set", "setting", "preparation" -> 8L // Environment prep
            "sitter", "safe", "safety" -> 12L // Safety measures
            "supplement", "vitamin", "magnesium", "antioxidant" -> 5L // Harm reduction supplements
            else -> 3L // Other safety-related practices
        }
    }
    
    private fun calculateIntegrationXP(quality: IntegrationQuality): Long {
        return when (quality) {
            IntegrationQuality.NONE -> 0L
            IntegrationQuality.BASIC -> 15L
            IntegrationQuality.MODERATE -> 25L
            IntegrationQuality.DEEP -> 40L
        }
    }
    
    private fun aggregateResults(result1: GamificationResult, result2: GamificationResult): GamificationResult {
        return GamificationResult(
            xpAwarded = result1.xpAwarded + result2.xpAwarded,
            newAchievements = result1.newAchievements + result2.newAchievements,
            streakUpdates = result1.streakUpdates + result2.streakUpdates,
            levelUp = result1.levelUp || result2.levelUp,
            newLevel = result2.newLevel ?: result1.newLevel,
            notifications = result1.notifications + result2.notifications
        )
    }
    
    private fun analyzeConsistencyPatterns(experiences: List<Experience>): List<ProgressInsight> {
        // Analyze logging frequency and consistency
        // Return insights about streak potential and consistency improvements
        return emptyList() // Simplified for now
    }
    
    private fun analyzeSafetyPatterns(experiences: List<Experience>): List<ProgressInsight> {
        // Analyze safety practice adoption over time
        // Return insights about safety score improvements
        return emptyList() // Simplified for now
    }
    
    private fun analyzeLearningProgression(experiences: List<Experience>): List<ProgressInsight> {
        // Analyze knowledge growth and learning patterns
        // Return insights about quest opportunities
        return emptyList() // Simplified for now
    }
    
    private fun analyzeUserPatterns(experiences: List<Experience>): UserPatternAnalysis {
        val recentExperiences = experiences.takeLast(10)
        
        val safetyPracticeCount = recentExperiences.sumOf { experience ->
            val text = "${experience.title} ${experience.text}".lowercase()
            listOf("test", "dosage", "safe", "preparation").count { it in text }
        }
        
        val hasConsistentLogging = experiences.size >= 5 // Simplified check
        
        val integrationNoteCount = recentExperiences.count { experience ->
            val text = experience.text.lowercase()
            listOf("insight", "learn", "reflect").any { it in text }
        }
        
        return UserPatternAnalysis(
            lacksSafetyPractices = safetyPracticeCount < 3,
            inconsistentLogging = !hasConsistentLogging,
            lacksIntegration = integrationNoteCount < 2
        )
    }
    
    private data class ExperienceQualityAnalysis(
        val completenessScore: Double,
        val isDetailedExperience: Boolean,
        val hasNotes: Boolean,
        val hasSetSetting: Boolean,
        val safetyPracticesUsed: List<String>,
        val hasIntegrationNotes: Boolean,
        val integrationQuality: IntegrationQuality
    )
    
    private enum class IntegrationQuality {
        NONE, BASIC, MODERATE, DEEP
    }
    
    private data class UserPatternAnalysis(
        val lacksSafetyPractices: Boolean,
        val inconsistentLogging: Boolean,
        val lacksIntegration: Boolean
    )
}