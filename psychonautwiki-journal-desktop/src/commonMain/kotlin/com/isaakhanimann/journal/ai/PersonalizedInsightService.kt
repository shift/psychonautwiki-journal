package com.isaakhanimann.journal.ai

import com.isaakhanimann.journal.data.model.Experience
import com.isaakhanimann.journal.data.repository.ExperienceRepository
import com.isaakhanimann.journal.data.repository.PreferencesRepository
import com.isaakhanimann.journal.gamification.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.minus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface PersonalizedInsightService {
    val insights: StateFlow<List<AIPersonalizedInsight>>
    val unreadInsightCount: StateFlow<Int>
    
    suspend fun generateDailyInsights(): List<AIPersonalizedInsight>
    suspend fun generateContextualInsight(context: String): AIPersonalizedInsight?
    suspend fun markInsightAsRead(insightId: String): Boolean
    suspend fun dismissInsight(insightId: String): Boolean
    fun getInsightsByCategory(category: InsightCategory): List<AIPersonalizedInsight>
    fun getActionableInsights(): List<AIPersonalizedInsight>
}

@OptIn(ExperimentalUuidApi::class)
class PersonalizedInsightServiceImpl(
    private val experienceRepository: ExperienceRepository,
    private val preferencesRepository: PreferencesRepository,
    private val gamificationService: GamificationService
) : PersonalizedInsightService, KoinComponent {
    
    private val json = Json { prettyPrint = true }
    
    private val _insights = MutableStateFlow<List<AIPersonalizedInsight>>(emptyList())
    override val insights = _insights.asStateFlow()
    
    private val _unreadInsightCount = MutableStateFlow(0)
    override val unreadInsightCount = _unreadInsightCount.asStateFlow()
    
    override suspend fun generateDailyInsights(): List<AIPersonalizedInsight> {
        val newInsights = mutableListOf<AIPersonalizedInsight>()
        
        // Analyze recent experiences (last 30 days)
        val thirtyDaysAgo = Clock.System.now().minus(30.days)
        val recentExperiences = experienceRepository.getAllExperiences().first()
            .filter { it.creationDate >= thirtyDaysAgo }
        
        if (recentExperiences.isEmpty()) {
            return emptyList()
        }
        
        // Generate various types of insights
        newInsights.addAll(generateSafetyPatternInsights(recentExperiences))
        newInsights.addAll(generateDosageOptimizationInsights(recentExperiences))
        newInsights.addAll(generateTimingPatternInsights(recentExperiences))
        newInsights.addAll(generateIntegrationGapInsights(recentExperiences))
        newInsights.addAll(generateProgressFeedbackInsights())
        
        // Save and update state
        val allInsights = (_insights.value + newInsights).distinctBy { it.id }
        _insights.value = allInsights
        updateUnreadCount()
        saveInsights(allInsights)
        
        return newInsights
    }
    
    override suspend fun generateContextualInsight(context: String): AIPersonalizedInsight? {
        // Generate insights based on specific context (e.g., before logging experience)
        return when (context.lowercase()) {
            "pre_experience" -> generatePreExperienceInsight()
            "post_experience" -> generatePostExperienceInsight()
            "weekly_review" -> generateWeeklyReviewInsight()
            else -> null
        }
    }
    
    override suspend fun markInsightAsRead(insightId: String): Boolean {
        val currentInsights = _insights.value.toMutableList()
        val index = currentInsights.indexOfFirst { it.id == insightId }
        
        if (index != -1) {
            currentInsights[index] = currentInsights[index].copy(isRead = true)
            _insights.value = currentInsights
            updateUnreadCount()
            saveInsights(currentInsights)
            return true
        }
        
        return false
    }
    
    override suspend fun dismissInsight(insightId: String): Boolean {
        val currentInsights = _insights.value.toMutableList()
        val removed = currentInsights.removeAll { it.id == insightId }
        
        if (removed) {
            _insights.value = currentInsights
            updateUnreadCount()
            saveInsights(currentInsights)
        }
        
        return removed
    }
    
    override fun getInsightsByCategory(category: InsightCategory): List<AIPersonalizedInsight> {
        return _insights.value.filter { it.category == category }
    }
    
    override fun getActionableInsights(): List<AIPersonalizedInsight> {
        return _insights.value.filter { it.actionableSteps.isNotEmpty() && !it.isRead }
    }
    
    private suspend fun generateSafetyPatternInsights(experiences: List<Experience>): List<AIPersonalizedInsight> {
        val insights = mutableListOf<AIPersonalizedInsight>()
        
        // Analyze dosage consistency
        val dosageVariations = experiences.mapNotNull { exp ->
            exp.ingestions?.mapNotNull { it.dose }
        }.flatten()
        
        if (dosageVariations.size >= 3) {
            val avgDose = dosageVariations.average()
            val variance = dosageVariations.map { (it - avgDose) * (it - avgDose) }.average()
            
            if (variance > avgDose * 0.3) { // High variance threshold
                insights.add(
                    AIPersonalizedInsight(
                        id = Uuid.random().toString(),
                        insight = "Your dosages have been quite variable recently. Consider standardizing your approach for more predictable experiences.",
                        category = InsightCategory.DOSAGE_OPTIMIZATION,
                        priority = InsightPriority.MEDIUM,
                        actionableSteps = listOf(
                            "Use a precision scale for all measurements",
                            "Start with lower doses and work up gradually",
                            "Document your optimal dosage range for each substance"
                        ),
                        basedOnData = listOf("${dosageVariations.size} recent dosage measurements"),
                        generatedAt = Clock.System.now()
                    )
                )
            }
        }
        
        return insights
    }
    
    private suspend fun generateDosageOptimizationInsights(experiences: List<Experience>): List<AIPersonalizedInsight> {
        val insights = mutableListOf<AIPersonalizedInsight>()
        
        // Analyze experiences with ratings
        val ratedExperiences = experiences.filter { it.overallRating != null }
        
        if (ratedExperiences.size >= 3) {
            val avgRating = ratedExperiences.mapNotNull { it.overallRating }.average()
            
            if (avgRating < 3.5) {
                insights.add(
                    AIPersonalizedInsight(
                        id = Uuid.random().toString(),
                        insight = "Your recent experience ratings suggest room for improvement. Consider focusing on set, setting, and preparation.",
                        category = InsightCategory.SAFETY_PATTERN,
                        priority = InsightPriority.HIGH,
                        actionableSteps = listOf(
                            "Review your preparation routine before experiences",
                            "Ensure comfortable, safe environment",
                            "Consider longer breaks between experiences",
                            "Focus on integration practices after experiences"
                        ),
                        basedOnData = listOf("${ratedExperiences.size} recent rated experiences", "Average rating: ${String.format("%.1f", avgRating)}/5"),
                        generatedAt = Clock.System.now()
                    )
                )
            }
        }
        
        return insights
    }
    
    private suspend fun generateTimingPatternInsights(experiences: List<Experience>): List<AIPersonalizedInsight> {
        val insights = mutableListOf<AIPersonalizedInsight>()
        
        if (experiences.size >= 2) {
            val sortedExperiences = experiences.sortedBy { it.date ?: it.creationDate }
            val intervals = sortedExperiences.zipWithNext { a, b ->
                val aTime = a.date ?: a.creationDate
                val bTime = b.date ?: b.creationDate
                bTime.epochSeconds - aTime.epochSeconds
            }
            
            val avgInterval = intervals.average()
            val daysBetween = avgInterval / (24 * 60 * 60)
            
            if (daysBetween < 7) { // Less than a week between experiences
                insights.add(
                    AIPersonalizedInsight(
                        id = Uuid.random().toString(),
                        insight = "You've been having experiences quite frequently. Consider allowing more time for integration between sessions.",
                        category = InsightCategory.TIMING_PATTERNS,
                        priority = InsightPriority.MEDIUM,
                        actionableSteps = listOf(
                            "Space experiences at least 1-2 weeks apart",
                            "Use time between experiences for reflection and integration",
                            "Journal about insights from previous experiences before the next one"
                        ),
                        basedOnData = listOf("Average ${String.format("%.1f", daysBetween)} days between experiences"),
                        generatedAt = Clock.System.now()
                    )
                )
            }
        }
        
        return insights
    }
    
    private suspend fun generateIntegrationGapInsights(experiences: List<Experience>): List<AIPersonalizedInsight> {
        val insights = mutableListOf<AIPersonalizedInsight>()
        
        // Check for experiences with minimal notes/text
        val shortEntries = experiences.filter { it.text.length < 100 }
        
        if (shortEntries.size > experiences.size * 0.5) { // More than half are short
            insights.add(
                AIPersonalizedInsight(
                    id = Uuid.random().toString(),
                    insight = "Many of your recent experiences have brief notes. More detailed journaling can enhance integration and learning.",
                    category = InsightCategory.INTEGRATION_GAPS,
                    priority = InsightPriority.MEDIUM,
                    actionableSteps = listOf(
                        "Set aside 20-30 minutes after each experience for detailed journaling",
                        "Document specific insights, emotions, and lessons learned",
                        "Include preparation details and setting descriptions",
                        "Note any challenging moments and how you handled them"
                    ),
                    basedOnData = listOf("${shortEntries.size} of ${experiences.size} recent experiences have brief notes"),
                    generatedAt = Clock.System.now()
                )
            )
        }
        
        return insights
    }
    
    private suspend fun generateProgressFeedbackInsights(): List<AIPersonalizedInsight> {
        val insights = mutableListOf<AIPersonalizedInsight>()
        
        val stats = gamificationService.gamificationStats.value
        val level = gamificationService.userLevel.value
        
        // Positive reinforcement for progress
        if (stats.achievementsUnlocked > 0) {
            insights.add(
                AIPersonalizedInsight(
                    id = Uuid.random().toString(),
                    insight = "Congratulations on reaching Level ${level.currentLevel}! Your commitment to harm reduction practices is making a real difference.",
                    category = InsightCategory.PROGRESS_FEEDBACK,
                    priority = InsightPriority.LOW,
                    actionableSteps = listOf(
                        "Continue your excellent safety practices",
                        "Consider sharing your knowledge with the community (future feature)",
                        "Set new personal goals for the next level"
                    ),
                    basedOnData = listOf("Current level: ${level.currentLevel}", "Achievements unlocked: ${stats.achievementsUnlocked}"),
                    generatedAt = Clock.System.now()
                )
            )
        }
        
        return insights
    }
    
    private suspend fun generatePreExperienceInsight(): AIPersonalizedInsight? {
        // Insight to show before logging a new experience
        return AIPersonalizedInsight(
            id = Uuid.random().toString(),
            insight = "Remember to document your preparation, set, and setting details. This information is valuable for future reference and safety.",
            category = InsightCategory.SAFETY_PATTERN,
            priority = InsightPriority.MEDIUM,
            actionableSteps = listOf(
                "Include details about your physical and mental state",
                "Document your environment and who you're with",
                "Note any substances consumed in the last 24 hours",
                "Record your intentions for this experience"
            ),
            basedOnData = listOf("Pre-experience preparation reminder"),
            generatedAt = Clock.System.now(),
            expiresAt = Clock.System.now().plus(1.hours)
        )
    }
    
    private suspend fun generatePostExperienceInsight(): AIPersonalizedInsight? {
        // Insight to show after logging an experience
        return AIPersonalizedInsight(
            id = Uuid.random().toString(),
            insight = "Take time to process and integrate your experience. The insights from reflection are often as valuable as the experience itself.",
            category = InsightCategory.INTEGRATION_GAPS,
            priority = InsightPriority.MEDIUM,
            actionableSteps = listOf(
                "Journal about key insights and lessons learned",
                "Identify any challenging moments and how you handled them",
                "Consider how this experience relates to your personal growth",
                "Plan integration practices for the coming days"
            ),
            basedOnData = listOf("Post-experience integration reminder"),
            generatedAt = Clock.System.now(),
            expiresAt = Clock.System.now().plus(24.hours)
        )
    }
    
    private suspend fun generateWeeklyReviewInsight(): AIPersonalizedInsight? {
        val weekAgo = Clock.System.now().minus(7.days)
        val weeklyExperiences = experienceRepository.getAllExperiences().first()
            .filter { it.creationDate >= weekAgo }
        
        return AIPersonalizedInsight(
            id = Uuid.random().toString(),
            insight = "Weekly reflection: You've logged ${weeklyExperiences.size} experiences this week. Consider what patterns or insights you're noticing.",
            category = InsightCategory.PROGRESS_FEEDBACK,
            priority = InsightPriority.LOW,
            actionableSteps = listOf(
                "Review your experiences from this week",
                "Identify common themes or patterns",
                "Consider what you've learned about yourself",
                "Set intentions for the coming week"
            ),
            basedOnData = listOf("${weeklyExperiences.size} experiences logged this week"),
            generatedAt = Clock.System.now()
        )
    }
    
    private fun updateUnreadCount() {
        _unreadInsightCount.value = _insights.value.count { !it.isRead }
    }
    
    private suspend fun saveInsights(insights: List<AIPersonalizedInsight>) {
        val insightsJson = json.encodeToString(insights)
        preferencesRepository.setString("personalized_insights", insightsJson)
    }
}