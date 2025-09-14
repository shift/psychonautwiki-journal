package com.isaakhanimann.journal.gamification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant

interface GamificationService {
    // Core progression tracking
    val userLevel: StateFlow<UserLevel>
    val achievements: StateFlow<List<UserAchievement>>
    val streaks: StateFlow<Map<StreakType, Streak>>
    val questProgress: StateFlow<List<UserQuestProgress>>
    val safetyScore: StateFlow<SafetyScore?>
    val gamificationStats: StateFlow<GamificationStats>
    
    // Achievement system
    suspend fun checkAchievements(event: GamificationEvent): List<Achievement>
    suspend fun getAvailableAchievements(): List<Achievement>
    suspend fun getUnlockedAchievements(): List<UserAchievement>
    suspend fun getAchievementProgress(achievementId: String): Map<String, Long>
    
    // Experience point system
    suspend fun awardXP(amount: Long, reason: String): UserLevel
    suspend fun calculateExperienceXP(experienceId: String): Long
    suspend fun getXPBreakdown(): Map<String, Long>
    
    // Streak management
    suspend fun updateStreak(type: StreakType, date: Instant = kotlinx.datetime.Clock.System.now())
    suspend fun getActiveStreaks(): List<Streak>
    suspend fun getStreakHistory(type: StreakType): List<Pair<Instant, Int>>
    
    // Knowledge quest system
    suspend fun getAvailableQuests(): List<KnowledgeQuest>
    suspend fun startQuest(questId: String): Result<UserQuestProgress>
    suspend fun completeQuestStep(questId: String, stepId: String, answer: String? = null): Result<Boolean>
    suspend fun getQuestProgress(questId: String): UserQuestProgress?
    suspend fun getCompletedQuests(): List<UserQuestProgress>
    
    // Weekly challenges
    suspend fun getCurrentChallenges(): List<WeeklyChallenge>
    suspend fun getChallengeProgress(challengeId: String): UserChallengeProgress?
    suspend fun updateChallengeProgress(challengeId: String, event: GamificationEvent)
    
    // Safety scoring
    suspend fun updateSafetyScore(experienceId: String? = null)
    suspend fun getSafetyTrends(): List<Pair<Instant, Double>>
    suspend fun getSafetyInsights(): List<String>
    
    // Event processing
    suspend fun processEvent(event: GamificationEvent): GamificationResult
    suspend fun getRecentEvents(limit: Int = 50): List<GamificationEvent>
    
    // Analytics and insights
    suspend fun getProgressInsights(): List<ProgressInsight>
    suspend fun getPersonalizedChallenges(): List<PersonalizedChallenge>
    suspend fun getGamificationAnalytics(): GamificationAnalytics
}

data class GamificationResult(
    val xpAwarded: Long,
    val newAchievements: List<Achievement>,
    val streakUpdates: List<Streak>,
    val levelUp: Boolean,
    val newLevel: UserLevel?,
    val notifications: List<GamificationNotification>
)

data class GamificationNotification(
    val type: NotificationType,
    val title: String,
    val message: String,
    val iconResource: String? = null,
    val actionLabel: String? = null,
    val actionData: Map<String, String> = emptyMap()
)

enum class NotificationType {
    XP_GAINED,
    LEVEL_UP,
    ACHIEVEMENT_UNLOCKED,
    STREAK_MILESTONE,
    QUEST_AVAILABLE,
    CHALLENGE_COMPLETED,
    SAFETY_IMPROVEMENT,
    WEEKLY_SUMMARY
}

data class ProgressInsight(
    val type: InsightType,
    val title: String,
    val description: String,
    val actionable: Boolean,
    val metadata: Map<String, Any> = emptyMap()
)

enum class InsightType {
    LEVEL_PROGRESS,
    ACHIEVEMENT_OPPORTUNITY,
    STREAK_ENCOURAGEMENT,
    SAFETY_IMPROVEMENT,
    LEARNING_SUGGESTION,
    CONSISTENCY_FEEDBACK
}

data class PersonalizedChallenge(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: ChallengeDifficulty,
    val xpReward: Long,
    val estimatedDays: Int,
    val category: AchievementCategory
)

// Note: ChallengeDifficulty is defined in GamificationModels.kt

data class GamificationAnalytics(
    val totalEngagementDays: Int,
    val averageXPPerWeek: Double,
    val mostActiveCategory: AchievementCategory,
    val streakData: Map<StreakType, StreakAnalytics>,
    val safetyTrend: ScoreTrend,
    val completionRates: Map<String, Double>,
    val timeToNextLevel: EstimatedTime,
    val nextMilestone: Milestone?
)

data class StreakAnalytics(
    val currentStreak: Int,
    val longestStreak: Int,
    val averageStreakLength: Double,
    val totalStreaks: Int,
    val consistency: Double // Percentage of possible days
)

data class EstimatedTime(
    val days: Int,
    val confidence: Double // 0.0 to 1.0
)

data class Milestone(
    val type: MilestoneType,
    val target: Long,
    val progress: Long,
    val description: String
)

enum class MilestoneType {
    LEVEL_UP,
    ACHIEVEMENT,
    STREAK_GOAL,
    XP_MILESTONE,
    QUEST_COMPLETION
}