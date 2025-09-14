package com.isaakhanimann.journal.gamification

import com.isaakhanimann.journal.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.plus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

interface WeeklyChallengeService {
    val currentChallenge: StateFlow<WeeklyChallenge?>
    val challengeProgress: StateFlow<ChallengeProgress?>
    val completedChallenges: StateFlow<List<WeeklyChallenge>>
    
    suspend fun generateNewWeeklyChallenge(): WeeklyChallenge?
    suspend fun startChallenge(challengeId: String): Boolean
    suspend fun updateProgress(challengeId: String, actionType: GamificationEventType): Boolean
    suspend fun completeChallenge(challengeId: String): Boolean
    fun getChallengeForWeek(weekStart: Instant): WeeklyChallenge?
}

class WeeklyChallengeServiceImpl(
    private val preferencesRepository: PreferencesRepository,
    private val gamificationService: GamificationService
) : WeeklyChallengeService, KoinComponent {
    
    private val json = Json { prettyPrint = true }
    
    private val _currentChallenge = MutableStateFlow<WeeklyChallenge?>(null)
    override val currentChallenge = _currentChallenge.asStateFlow()
    
    private val _challengeProgress = MutableStateFlow<ChallengeProgress?>(null)
    override val challengeProgress = _challengeProgress.asStateFlow()
    
    private val _completedChallenges = MutableStateFlow<List<WeeklyChallenge>>(emptyList())
    override val completedChallenges = _completedChallenges.asStateFlow()
    
    private val challengeTemplates = listOf(
        // Beginner Safety Challenges
        ChallengeTemplate(
            title = "Safety First Week",
            description = "Complete 3 detailed experience logs with safety information",
            category = ChallengeCategory.SAFETY,
            difficulty = ChallengeDifficulty.BEGINNER,
            xpReward = 150,
            requirements = listOf(
                ChallengeRequirement(
                    type = RequirementType.DETAILED_EXPERIENCES,
                    target = 3,
                    description = "Log 3 experiences with complete safety details"
                )
            )
        ),
        
        // Knowledge Challenges
        ChallengeTemplate(
            title = "Research Scholar",
            description = "Research and document information about 2 new substances",
            category = ChallengeCategory.KNOWLEDGE,
            difficulty = ChallengeDifficulty.INTERMEDIATE,
            xpReward = 200,
            requirements = listOf(
                ChallengeRequirement(
                    type = RequirementType.RESEARCH_DOCUMENTED,
                    target = 2,
                    description = "Research and add notes about 2 substances"
                )
            )
        ),
        
        // Mindfulness Challenges
        ChallengeTemplate(
            title = "Integration Master",
            description = "Complete 5 post-experience reflection sessions",
            category = ChallengeCategory.MINDFULNESS,
            difficulty = ChallengeDifficulty.INTERMEDIATE,
            xpReward = 175,
            requirements = listOf(
                ChallengeRequirement(
                    type = RequirementType.INTEGRATION_SESSIONS,
                    target = 5,
                    description = "Complete 5 integration reflection sessions"
                )
            )
        ),
        
        // Documentation Challenges
        ChallengeTemplate(
            title = "Daily Chronicler",
            description = "Log experiences or insights for 7 consecutive days",
            category = ChallengeCategory.DOCUMENTATION,
            difficulty = ChallengeDifficulty.ADVANCED,
            xpReward = 300,
            requirements = listOf(
                ChallengeRequirement(
                    type = RequirementType.CONSECUTIVE_DAYS_LOGGING,
                    target = 7,
                    description = "Log something meaningful each day for a week"
                )
            )
        ),
        
        // Advanced Safety Challenges
        ChallengeTemplate(
            title = "Harm Reduction Advocate",
            description = "Demonstrate comprehensive safety practices across multiple experiences",
            category = ChallengeCategory.SAFETY,
            difficulty = ChallengeDifficulty.EXPERT,
            xpReward = 500,
            requirements = listOf(
                ChallengeRequirement(
                    type = RequirementType.SAFETY_SCORE_MAINTAINED,
                    target = 90,
                    description = "Maintain safety score above 90 for the week"
                ),
                ChallengeRequirement(
                    type = RequirementType.DETAILED_EXPERIENCES,
                    target = 3,
                    description = "Log 3 comprehensive experiences with all safety details"
                )
            )
        )
    )
    
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun generateNewWeeklyChallenge(): WeeklyChallenge? {
        val now = Clock.System.now()
        val weekStart = getWeekStart(now)
        val weekEnd = weekStart.plus(7.days)
        
        // Check if we already have a challenge for this week
        val existingChallenge = getChallengeForWeek(weekStart)
        if (existingChallenge != null) {
            _currentChallenge.value = existingChallenge
            return existingChallenge
        }
        
        // Get user's level to determine appropriate challenge difficulty
        val userLevel = gamificationService.userLevel.value
        val availableTemplates = challengeTemplates.filter { template ->
            when (template.difficulty) {
                ChallengeDifficulty.BEGINNER -> userLevel.currentLevel <= 5
                ChallengeDifficulty.INTERMEDIATE -> userLevel.currentLevel in 3..15
                ChallengeDifficulty.ADVANCED -> userLevel.currentLevel in 10..25
                ChallengeDifficulty.EXPERT -> userLevel.currentLevel >= 20
            }
        }
        
        if (availableTemplates.isEmpty()) {
            return null
        }
        
        // Select a random appropriate challenge
        val template = availableTemplates.random()
        val challenge = WeeklyChallenge(
            id = Uuid.random().toString(),
            title = template.title,
            description = template.description,
            category = template.category,
            difficulty = template.difficulty,
            xpReward = template.xpReward,
            requirements = template.requirements,
            startDate = weekStart,
            endDate = weekEnd,
            isActive = true
        )
        
        // Save the challenge
        saveChallenge(challenge)
        _currentChallenge.value = challenge
        
        return challenge
    }
    
    override suspend fun startChallenge(challengeId: String): Boolean {
        val challenge = _currentChallenge.value
        if (challenge?.id != challengeId) return false
        
        val progress = ChallengeProgress(
            challengeId = challengeId,
            userId = "current_user", // In a real app, this would be the actual user ID
            startedAt = Clock.System.now()
        )
        
        saveChallengeProgress(progress)
        _challengeProgress.value = progress
        
        return true
    }
    
    override suspend fun updateProgress(challengeId: String, actionType: GamificationEventType): Boolean {
        val progress = _challengeProgress.value
        val challenge = _currentChallenge.value
        
        if (progress?.challengeId != challengeId || challenge?.id != challengeId || progress.isCompleted) {
            return false
        }
        
        // Update progress metrics based on the action type
        val updatedMetrics = progress.currentMetrics.toMutableMap()
        val metricKey = when (actionType) {
            GamificationEventType.EXPERIENCE_CREATED -> "experiences_logged"
            GamificationEventType.EXPERIENCE_DETAILED -> "detailed_experiences"
            GamificationEventType.INTEGRATION_COMPLETED -> "integration_sessions"
            GamificationEventType.RESEARCH_DOCUMENTED -> "research_completed"
            GamificationEventType.SAFETY_PRACTICE_USED -> "safety_checks"
            else -> return false
        }
        
        updatedMetrics[metricKey] = (updatedMetrics[metricKey] ?: 0) + 1
        
        // Calculate overall progress
        val totalProgress = challenge.requirements.sumOf { requirement ->
            val currentValue = updatedMetrics[getMetricKeyForRequirement(requirement.type)] ?: 0
            (currentValue.toDouble() / requirement.target.toDouble()).coerceAtMost(1.0)
        } / challenge.requirements.size.toDouble()
        
        val updatedProgress = progress.copy(
            currentMetrics = updatedMetrics,
            progress = totalProgress.toFloat(),
            isCompleted = totalProgress >= 1.0,
            completedAt = if (totalProgress >= 1.0) Clock.System.now() else null
        )
        
        saveChallengeProgress(updatedProgress)
        _challengeProgress.value = updatedProgress
        
        // If completed, award XP and update achievements
        if (updatedProgress.isCompleted && !progress.isCompleted) {
            gamificationService.awardXP(challenge.xpReward, "Weekly Challenge Completed: ${challenge.title}")
            completeChallenge(challengeId)
        }
        
        return true
    }
    
    override suspend fun completeChallenge(challengeId: String): Boolean {
        val challenge = _currentChallenge.value
        if (challenge?.id != challengeId) return false
        
        // Move to completed challenges
        val completed = _completedChallenges.value.toMutableList()
        completed.add(challenge)
        _completedChallenges.value = completed
        
        // Save completed challenges
        val completedJson = json.encodeToString(completed)
        preferencesRepository.setString("completed_weekly_challenges", completedJson)
        
        // Clear current challenge
        _currentChallenge.value = null
        _challengeProgress.value = null
        
        return true
    }
    
    override fun getChallengeForWeek(weekStart: Instant): WeeklyChallenge? {
        // This would typically query saved challenges
        // For now, return null to indicate no existing challenge
        return null
    }
    
    private suspend fun saveChallenge(challenge: WeeklyChallenge) {
        val challengeJson = json.encodeToString(challenge)
        preferencesRepository.setString("current_weekly_challenge", challengeJson)
    }
    
    private suspend fun saveChallengeProgress(progress: ChallengeProgress) {
        val progressJson = json.encodeToString(progress)
        preferencesRepository.setString("current_challenge_progress", progressJson)
    }
    
    private fun getWeekStart(instant: Instant): Instant {
        // Simplified week calculation - in real implementation, this would be more sophisticated
        return instant
    }
    
    private fun getMetricKeyForRequirement(requirementType: RequirementType): String {
        return when (requirementType) {
            RequirementType.EXPERIENCES_LOGGED -> "experiences_logged"
            RequirementType.DETAILED_EXPERIENCES -> "detailed_experiences"
            RequirementType.INTEGRATION_SESSIONS -> "integration_sessions"
            RequirementType.RESEARCH_DOCUMENTED -> "research_completed"
            RequirementType.CONSECUTIVE_DAYS_LOGGING -> "consecutive_days"
            RequirementType.SAFETY_SCORE_MAINTAINED -> "safety_score"
            else -> "unknown"
        }
    }
}

data class ChallengeTemplate(
    val title: String,
    val description: String,
    val category: ChallengeCategory,
    val difficulty: ChallengeDifficulty,
    val xpReward: Long,
    val requirements: List<ChallengeRequirement>
)