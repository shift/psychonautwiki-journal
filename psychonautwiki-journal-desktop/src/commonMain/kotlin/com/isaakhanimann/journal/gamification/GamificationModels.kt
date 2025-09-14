package com.isaakhanimann.journal.gamification

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

// Core Gamification Data Models

@Serializable
data class UserLevel(
    val currentLevel: Int = 1,
    val currentXP: Long = 0,
    val xpToNextLevel: Long = 100,
    val totalXP: Long = 0
) {
    fun getProgressPercentage(): Float = (currentXP.toFloat() / xpToNextLevel.toFloat()).coerceIn(0f, 1f)
    
    companion object {
        fun calculateXPRequiredForLevel(level: Int): Long {
            // Exponential progression: level^2 * 100 (like language learning apps)
            return (level * level * 100L).coerceAtLeast(100L)
        }
        
        fun calculateLevelFromTotalXP(totalXP: Long): UserLevel {
            var level = 1
            var accumulatedXP = 0L
            
            while (accumulatedXP + calculateXPRequiredForLevel(level) <= totalXP) {
                accumulatedXP += calculateXPRequiredForLevel(level)
                level++
            }
            
            val currentXP = totalXP - accumulatedXP
            val xpToNextLevel = calculateXPRequiredForLevel(level)
            
            return UserLevel(
                currentLevel = level,
                currentXP = currentXP,
                xpToNextLevel = xpToNextLevel,
                totalXP = totalXP
            )
        }
    }
}

@Serializable
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val category: AchievementCategory,
    val tier: AchievementTier,
    val xpReward: Long,
    val iconResource: String,
    val requirements: List<AchievementRequirement>,
    val isHidden: Boolean = false, // Hidden until requirements are close to completion
    val prerequisites: List<String> = emptyList() // Other achievement IDs required first
)

@Serializable
enum class AchievementCategory {
    SAFETY_FIRST,      // Testing, dosage precision, set/setting preparation
    KNOWLEDGE_SEEKER,  // Learning about substances, interactions, effects
    CONSISTENCY,       // Regular journaling, reflection habits  
    INTEGRATION,       // Post-experience processing and insights
    COMMUNITY_CARE,    // Sharing anonymous safety insights (future feature)
    HARM_REDUCTION,    // Overall harm reduction practices
    MILESTONE          // General app usage milestones
}

@Serializable
enum class AchievementTier {
    BRONZE,   // Basic milestones
    SILVER,   // Consistent practice
    GOLD,     // Excellence in harm reduction
    PLATINUM  // Teaching and mentoring level
}

@Serializable
data class AchievementRequirement(
    val type: RequirementType,
    val target: Long,
    val timeFrame: TimeFrame? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class RequirementType {
    EXPERIENCES_LOGGED,           // Total number of experiences documented
    CONSECUTIVE_DAYS_LOGGING,     // Daily journaling streak
    DETAILED_EXPERIENCES,         // Experiences with high completeness score
    SAFETY_PRACTICES_USED,        // Number of safety practices documented
    SUBSTANCES_RESEARCHED,        // Number of substances with notes/research
    INTEGRATION_SESSIONS,         // Post-experience reflection entries
    DOSAGE_PRECISION,            // Experiences with precise dosage measurements
    SET_SETTING_DOCUMENTED,      // Experiences with set/setting information
    HARM_REDUCTION_TOOLS,        // Use of testing kits, supplements, etc.
    KNOWLEDGE_QUESTS_COMPLETED,  // Interactive learning modules completed
    WEEKLY_GOALS_MET,           // Weekly safety/learning goals achieved
    APP_DAYS_ACTIVE,            // Days with app activity
    RESEARCH_DOCUMENTED,        // Research activities completed
    SAFETY_SCORE_MAINTAINED     // Safety score maintained above threshold
}

@Serializable
enum class TimeFrame {
    DAILY,
    WEEKLY, 
    MONTHLY,
    YEARLY,
    ALL_TIME
}

@Serializable
data class UserAchievement(
    val achievementId: String,
    val unlockedAt: Instant,
    val progress: Map<String, Long> = emptyMap(), // Progress toward requirements
    val isCompleted: Boolean = false
)

@Serializable
data class Streak(
    val type: StreakType,
    val currentCount: Int = 0,
    val bestCount: Int = 0,
    val lastActivityDate: Instant? = null,
    val isActive: Boolean = false
) {
    fun shouldResetStreak(currentDate: Instant): Boolean {
        val lastActivity = lastActivityDate ?: return false
        val daysSinceActivity = (currentDate.epochSeconds - lastActivity.epochSeconds) / 86400
        return daysSinceActivity > 1 // Reset if more than 1 day gap
    }
}

@Serializable
enum class StreakType {
    DAILY_LOGGING,        // Daily experience documentation
    INTEGRATION_PRACTICE, // Regular post-experience reflection
    SAFETY_HABITS,        // Consistent safety practice documentation
    LEARNING_STREAK,      // Daily knowledge quest completion
    APP_USAGE            // Daily app engagement
}

@Serializable
data class KnowledgeQuest(
    val id: String,
    val title: String,
    val description: String,
    val category: QuestCategory,
    val difficulty: QuestDifficulty,
    val xpReward: Long,
    val estimatedTimeMinutes: Int,
    val prerequisites: List<String> = emptyList(),
    val isUnlocked: Boolean = false,
    val steps: List<QuestStep>
)

@Serializable
enum class QuestCategory {
    SUBSTANCE_KNOWLEDGE,   // Learning about specific substances
    INTERACTION_SAFETY,    // Drug interaction education
    DOSAGE_CALCULATION,    // Precise dosage practice
    SET_SETTING,          // Environment and mindset optimization
    HARM_REDUCTION,       // General safety practices
    INTEGRATION_SKILLS,   // Post-experience processing techniques
    RISK_ASSESSMENT      // Identifying and mitigating risks
}

@Serializable
enum class QuestDifficulty {
    BEGINNER,    // Basic harm reduction concepts
    INTERMEDIATE, // Detailed safety protocols
    ADVANCED,    // Complex scenarios and calculations
    EXPERT       // Teaching-level mastery
}

@Serializable
data class QuestStep(
    val id: String,
    val title: String,
    val description: String,
    val type: QuestStepType,
    val content: String, // JSON or markdown content
    val requiredAnswer: String? = null, // For quiz steps
    val isCompleted: Boolean = false
)

@Serializable
enum class QuestStepType {
    INFORMATION,    // Educational content
    QUIZ,          // Multiple choice or input question
    PRACTICAL,     // Hands-on task (e.g., calculate dosage)
    REFLECTION,    // Open-ended reflection prompt
    SIMULATION     // Interactive scenario
}

@Serializable
data class UserQuestProgress(
    val questId: String,
    val startedAt: Instant,
    val completedAt: Instant? = null,
    val currentStepIndex: Int = 0,
    val stepProgress: Map<String, Boolean> = emptyMap(),
    val isCompleted: Boolean = false
)



@Serializable
data class UserChallengeProgress(
    val challengeId: String,
    val startedAt: Instant,
    val progress: Map<String, Long> = emptyMap(),
    val isCompleted: Boolean = false,
    val completedAt: Instant? = null
)

@Serializable
data class SafetyScore(
    val overallScore: Double, // 0.0 to 100.0
    val components: Map<SafetyComponent, Double>,
    val trend: ScoreTrend,
    val lastUpdated: Instant,
    val improvementAreas: List<String> = emptyList()
)

@Serializable
enum class SafetyComponent {
    DOSAGE_PRECISION,      // Accuracy of dosage measurements
    TESTING_FREQUENCY,     // Use of substance testing
    SET_SETTING_PREP,     // Environment and mindset preparation
    INTEGRATION_PRACTICE,  // Post-experience processing
    HARM_REDUCTION_TOOLS,  // Use of supplements, safety tools
    TIMING_AWARENESS,      // Spacing between experiences
    RESEARCH_QUALITY      // Depth of substance research
}

@Serializable
enum class ScoreTrend {
    IMPROVING,
    STABLE,
    DECLINING,
    INSUFFICIENT_DATA
}

@Serializable
data class GamificationStats(
    val totalXP: Long,
    val currentLevel: UserLevel,
    val achievementsUnlocked: Int,
    val totalAchievements: Int,
    val longestStreak: Int,
    val questsCompleted: Int,
    val safetyScore: SafetyScore?,
    val weeklyProgress: Map<String, Long> = emptyMap(),
    val levelProgress: Float = 0f
)

// Events for tracking user actions and calculating rewards
@Serializable
data class GamificationEvent(
    val type: GamificationEventType,
    val timestamp: Instant,
    val experienceId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val xpAwarded: Long = 0
)

@Serializable
enum class GamificationEventType {
    EXPERIENCE_CREATED,
    EXPERIENCE_DETAILED,      // High completeness score
    SAFETY_PRACTICE_USED,
    INTEGRATION_COMPLETED,
    STREAK_MILESTONE,
    ACHIEVEMENT_UNLOCKED,
    QUEST_COMPLETED,
    WEEKLY_GOAL_MET,
    KNOWLEDGE_GAINED,
    APP_LAUNCHED,
    RESEARCH_DOCUMENTED
}

// Weekly Safety Challenges
@Serializable
data class WeeklyChallenge(
    val id: String,
    val title: String,
    val description: String,
    val category: ChallengeCategory,
    val difficulty: ChallengeDifficulty,
    val xpReward: Long,
    val requirements: List<ChallengeRequirement>,
    val startDate: Instant,
    val endDate: Instant,
    val isActive: Boolean = true,
    val completedBy: List<String> = emptyList() // User IDs who completed it
)

@Serializable
enum class ChallengeCategory {
    SAFETY,           // Safety-focused challenges
    KNOWLEDGE,        // Learning and research challenges
    MINDFULNESS,      // Preparation and integration challenges
    DOCUMENTATION,    // Journaling and reflection challenges
    COMMUNITY         // Sharing and helping others (future)
}

@Serializable
enum class ChallengeDifficulty {
    BEGINNER,    // Simple, single-action challenges
    INTERMEDIATE, // Multi-step or consistency challenges
    ADVANCED,     // Complex, long-term challenges
    EXPERT       // Community leadership challenges
}

@Serializable
data class ChallengeRequirement(
    val type: RequirementType,
    val target: Long,
    val description: String,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class ChallengeProgress(
    val challengeId: String,
    val userId: String,
    val startedAt: Instant,
    val completedAt: Instant? = null,
    val progress: Float = 0f, // 0.0 to 1.0
    val currentMetrics: Map<String, Long> = emptyMap(),
    val isCompleted: Boolean = false
)

// AI Assistant Integration Models
@Serializable
data class AIPersonalizedInsight(
    val id: String,
    val insight: String,
    val category: InsightCategory,
    val priority: InsightPriority,
    val actionableSteps: List<String>,
    val basedOnData: List<String>, // What data this insight is based on
    val generatedAt: Instant,
    val expiresAt: Instant? = null,
    val isRead: Boolean = false
)

@Serializable
enum class InsightCategory {
    SAFETY_PATTERN,      // Patterns in safety practices
    DOSAGE_OPTIMIZATION, // Suggestions about dosage consistency
    TIMING_PATTERNS,     // Observations about timing between experiences
    INTEGRATION_GAPS,    // Missing integration practices
    SUBSTANCE_INTERACTIONS, // Insights about substance combinations
    PROGRESS_FEEDBACK    // Progress toward goals
}

@Serializable
enum class InsightPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}