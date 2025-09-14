package com.isaakhanimann.journal.gamification

import com.isaakhanimann.journal.data.repository.ExperienceRepository
import com.isaakhanimann.journal.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GamificationServiceImpl : GamificationService, KoinComponent {
    private val experienceRepository: ExperienceRepository by inject()
    private val preferencesRepository: PreferencesRepository by inject()
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.Default)
    
    // StateFlow for reactive updates
    private val _userLevel = MutableStateFlow(UserLevel())
    override val userLevel: StateFlow<UserLevel> = _userLevel.asStateFlow()
    
    private val _achievements = MutableStateFlow<List<UserAchievement>>(emptyList())
    override val achievements: StateFlow<List<UserAchievement>> = _achievements.asStateFlow()
    
    private val _streaks = MutableStateFlow<Map<StreakType, Streak>>(emptyMap())
    override val streaks: StateFlow<Map<StreakType, Streak>> = _streaks.asStateFlow()
    
    private val _questProgress = MutableStateFlow<List<UserQuestProgress>>(emptyList())
    override val questProgress: StateFlow<List<UserQuestProgress>> = _questProgress.asStateFlow()
    
    private val _safetyScore = MutableStateFlow<SafetyScore?>(null)
    override val safetyScore: StateFlow<SafetyScore?> = _safetyScore.asStateFlow()
    
    private val _gamificationStats = MutableStateFlow(GamificationStats(
        totalXP = 0,
        currentLevel = UserLevel(),
        achievementsUnlocked = 0,
        totalAchievements = 0,
        longestStreak = 0,
        questsCompleted = 0,
        safetyScore = null
    ))
    override val gamificationStats: StateFlow<GamificationStats> = _gamificationStats.asStateFlow()
    
    // Predefined achievements
    private val allAchievements = listOf(
        // Safety First Category
        Achievement(
            id = "first_experience",
            name = "First Steps",
            description = "Document your first experience",
            category = AchievementCategory.SAFETY_FIRST,
            tier = AchievementTier.BRONZE,
            xpReward = 50,
            iconResource = "achievement_first_experience",
            requirements = listOf(
                AchievementRequirement(RequirementType.EXPERIENCES_LOGGED, 1)
            )
        ),
        Achievement(
            id = "safety_conscious",
            name = "Safety Conscious",
            description = "Document 10 experiences with detailed safety practices",
            category = AchievementCategory.SAFETY_FIRST,
            tier = AchievementTier.SILVER,
            xpReward = 200,
            iconResource = "achievement_safety",
            requirements = listOf(
                AchievementRequirement(RequirementType.SAFETY_PRACTICES_USED, 10)
            )
        ),
        Achievement(
            id = "harm_reduction_expert",
            name = "Harm Reduction Expert",
            description = "Master all safety practices with 50 documented uses",
            category = AchievementCategory.SAFETY_FIRST,
            tier = AchievementTier.GOLD,
            xpReward = 500,
            iconResource = "achievement_expert",
            requirements = listOf(
                AchievementRequirement(RequirementType.HARM_REDUCTION_TOOLS, 50)
            )
        ),
        
        // Knowledge Seeker Category
        Achievement(
            id = "curious_mind",
            name = "Curious Mind",
            description = "Research and document 5 different substances",
            category = AchievementCategory.KNOWLEDGE_SEEKER,
            tier = AchievementTier.BRONZE,
            xpReward = 100,
            iconResource = "achievement_research",
            requirements = listOf(
                AchievementRequirement(RequirementType.SUBSTANCES_RESEARCHED, 5)
            )
        ),
        Achievement(
            id = "knowledge_seeker",
            name = "Knowledge Seeker",
            description = "Complete 10 knowledge quests",
            category = AchievementCategory.KNOWLEDGE_SEEKER,
            tier = AchievementTier.SILVER,
            xpReward = 300,
            iconResource = "achievement_quests",
            requirements = listOf(
                AchievementRequirement(RequirementType.KNOWLEDGE_QUESTS_COMPLETED, 10)
            )
        ),
        
        // Consistency Category
        Achievement(
            id = "daily_logger",
            name = "Daily Logger",
            description = "Maintain a 7-day logging streak",
            category = AchievementCategory.CONSISTENCY,
            tier = AchievementTier.BRONZE,
            xpReward = 150,
            iconResource = "achievement_streak",
            requirements = listOf(
                AchievementRequirement(RequirementType.CONSECUTIVE_DAYS_LOGGING, 7)
            )
        ),
        Achievement(
            id = "dedicated_journalist",
            name = "Dedicated Journalist",
            description = "Maintain a 30-day logging streak",
            category = AchievementCategory.CONSISTENCY,
            tier = AchievementTier.SILVER,
            xpReward = 400,
            iconResource = "achievement_dedication",
            requirements = listOf(
                AchievementRequirement(RequirementType.CONSECUTIVE_DAYS_LOGGING, 30)
            )
        ),
        Achievement(
            id = "consistency_master",
            name = "Consistency Master",
            description = "Maintain a 100-day logging streak",
            category = AchievementCategory.CONSISTENCY,
            tier = AchievementTier.GOLD,
            xpReward = 1000,
            iconResource = "achievement_master",
            requirements = listOf(
                AchievementRequirement(RequirementType.CONSECUTIVE_DAYS_LOGGING, 100)
            )
        ),
        
        // Integration Category
        Achievement(
            id = "reflective_practitioner",
            name = "Reflective Practitioner",
            description = "Complete 10 integration sessions",
            category = AchievementCategory.INTEGRATION,
            tier = AchievementTier.BRONZE,
            xpReward = 200,
            iconResource = "achievement_integration",
            requirements = listOf(
                AchievementRequirement(RequirementType.INTEGRATION_SESSIONS, 10)
            )
        ),
        Achievement(
            id = "integration_expert",
            name = "Integration Expert",
            description = "Complete 50 integration sessions with detailed insights",
            category = AchievementCategory.INTEGRATION,
            tier = AchievementTier.GOLD,
            xpReward = 750,
            iconResource = "achievement_insight",
            requirements = listOf(
                AchievementRequirement(RequirementType.INTEGRATION_SESSIONS, 50)
            )
        ),
        
        // Milestone Category
        Achievement(
            id = "level_5",
            name = "Rising Star",
            description = "Reach level 5",
            category = AchievementCategory.MILESTONE,
            tier = AchievementTier.BRONZE,
            xpReward = 100,
            iconResource = "achievement_level5",
            requirements = listOf(
                AchievementRequirement(RequirementType.APP_DAYS_ACTIVE, 5)
            )
        ),
        Achievement(
            id = "level_10",
            name = "Experienced User",
            description = "Reach level 10",
            category = AchievementCategory.MILESTONE,
            tier = AchievementTier.SILVER,
            xpReward = 250,
            iconResource = "achievement_level10",
            requirements = listOf(
                AchievementRequirement(RequirementType.APP_DAYS_ACTIVE, 10)
            )
        ),
        Achievement(
            id = "level_25",
            name = "Veteran Psychonaut",
            description = "Reach level 25",
            category = AchievementCategory.MILESTONE,
            tier = AchievementTier.GOLD,
            xpReward = 500,
            iconResource = "achievement_level25",
            requirements = listOf(
                AchievementRequirement(RequirementType.APP_DAYS_ACTIVE, 25)
            )
        )
    )
    
    // Predefined knowledge quests
    private val allQuests = listOf(
        KnowledgeQuest(
            id = "dosage_basics",
            title = "Dosage Calculation Basics",
            description = "Learn how to accurately calculate and measure dosages",
            category = QuestCategory.DOSAGE_CALCULATION,
            difficulty = QuestDifficulty.BEGINNER,
            xpReward = 75,
            estimatedTimeMinutes = 15,
            isUnlocked = true,
            steps = listOf(
                QuestStep(
                    id = "intro",
                    title = "Introduction to Dosage",
                    description = "Understanding the importance of accurate dosing",
                    type = QuestStepType.INFORMATION,
                    content = "Accurate dosing is fundamental to harm reduction..."
                ),
                QuestStep(
                    id = "quiz1",
                    title = "Basic Dosage Quiz",
                    description = "Test your understanding of dosage principles",
                    type = QuestStepType.QUIZ,
                    content = "What is the most important factor in determining dosage?",
                    requiredAnswer = "body weight and tolerance"
                )
            )
        ),
        KnowledgeQuest(
            id = "set_setting_mastery",
            title = "Set & Setting Mastery",
            description = "Master the art of creating optimal environments",
            category = QuestCategory.SET_SETTING,
            difficulty = QuestDifficulty.INTERMEDIATE,
            xpReward = 100,
            estimatedTimeMinutes = 20,
            prerequisites = listOf("dosage_basics"),
            steps = listOf(
                QuestStep(
                    id = "environment",
                    title = "Environment Preparation",
                    description = "Creating a safe physical space",
                    type = QuestStepType.INFORMATION,
                    content = "Setting refers to your physical environment..."
                ),
                QuestStep(
                    id = "mindset",
                    title = "Mindset Preparation",
                    description = "Mental preparation and intention setting",
                    type = QuestStepType.REFLECTION,
                    content = "Reflect on your current mental state and intentions"
                )
            )
        ),
        KnowledgeQuest(
            id = "interaction_safety",
            title = "Drug Interaction Safety",
            description = "Understanding dangerous combinations and interactions",
            category = QuestCategory.INTERACTION_SAFETY,
            difficulty = QuestDifficulty.ADVANCED,
            xpReward = 150,
            estimatedTimeMinutes = 30,
            prerequisites = listOf("dosage_basics", "set_setting_mastery"),
            steps = listOf(
                QuestStep(
                    id = "common_interactions",
                    title = "Common Dangerous Interactions",
                    description = "Learn about the most dangerous drug combinations",
                    type = QuestStepType.INFORMATION,
                    content = "Some combinations can be life-threatening..."
                ),
                QuestStep(
                    id = "interaction_quiz",
                    title = "Interaction Safety Quiz",
                    description = "Test your knowledge of drug interactions",
                    type = QuestStepType.QUIZ,
                    content = "Which combination should NEVER be mixed?",
                    requiredAnswer = "MAOI and MDMA"
                )
            )
        )
    )
    
    init {
        // Initialize gamification data in a coroutine
        scope.launch {
            loadGamificationData()
        }
    }
    
    private suspend fun loadGamificationData() {
        // Load user level
        val levelData = preferencesRepository.getString("gamification_level", "")
        if (levelData.isNotEmpty()) {
            try {
                _userLevel.value = json.decodeFromString<UserLevel>(levelData)
            } catch (e: Exception) {
                // Use default if parsing fails
            }
        }
        
        // Load achievements
        val achievementData = preferencesRepository.getString("gamification_achievements", "")
        if (achievementData.isNotEmpty()) {
            try {
                _achievements.value = json.decodeFromString<List<UserAchievement>>(achievementData)
            } catch (e: Exception) {
                _achievements.value = emptyList()
            }
        }
        
        // Load streaks
        val streakData = preferencesRepository.getString("gamification_streaks", "")
        if (streakData.isNotEmpty()) {
            try {
                _streaks.value = json.decodeFromString<Map<StreakType, Streak>>(streakData)
            } catch (e: Exception) {
                _streaks.value = initializeStreaks()
            }
        } else {
            _streaks.value = initializeStreaks()
        }
        
        // Load quest progress
        val questData = preferencesRepository.getString("gamification_quests", "")
        if (questData.isNotEmpty()) {
            try {
                _questProgress.value = json.decodeFromString<List<UserQuestProgress>>(questData)
            } catch (e: Exception) {
                _questProgress.value = emptyList()
            }
        }
        
        // Load safety score
        val safetyData = preferencesRepository.getString("gamification_safety", "")
        if (safetyData.isNotEmpty()) {
            try {
                _safetyScore.value = json.decodeFromString<SafetyScore>(safetyData)
            } catch (e: Exception) {
                // Use null if parsing fails
            }
        }
        
        updateGamificationStats()
    }
    
    private fun initializeStreaks(): Map<StreakType, Streak> {
        return StreakType.values().associateWith { type ->
            Streak(type = type)
        }
    }
    
    private suspend fun saveGamificationData() {
        preferencesRepository.setString("gamification_level", json.encodeToString(_userLevel.value))
        preferencesRepository.setString("gamification_achievements", json.encodeToString(_achievements.value))
        preferencesRepository.setString("gamification_streaks", json.encodeToString(_streaks.value))
        preferencesRepository.setString("gamification_quests", json.encodeToString(_questProgress.value))
        _safetyScore.value?.let { 
            preferencesRepository.setString("gamification_safety", json.encodeToString(it))
        }
    }
    
    override suspend fun checkAchievements(event: GamificationEvent): List<Achievement> {
        val newAchievements = mutableListOf<Achievement>()
        val currentAchievements = _achievements.value.map { it.achievementId }.toSet()
        
        allAchievements.forEach { achievement ->
            if (achievement.id !in currentAchievements) {
                if (isAchievementCompleted(achievement, event)) {
                    val userAchievement = UserAchievement(
                        achievementId = achievement.id,
                        unlockedAt = Clock.System.now(),
                        isCompleted = true
                    )
                    _achievements.value = _achievements.value + userAchievement
                    newAchievements.add(achievement)
                    
                    // Award XP for achievement
                    awardXP(achievement.xpReward, "Achievement: ${achievement.name}")
                }
            }
        }
        
        if (newAchievements.isNotEmpty()) {
            saveGamificationData()
            updateGamificationStats()
        }
        
        return newAchievements
    }
    
    private suspend fun isAchievementCompleted(achievement: Achievement, event: GamificationEvent): Boolean {
        // This is a simplified implementation - in practice, you'd check against actual user data
        return achievement.requirements.all { requirement ->
            when (requirement.type) {
                RequirementType.EXPERIENCES_LOGGED -> {
                    // Check actual experience count from repository
                    true // Simplified for now
                }
                RequirementType.CONSECUTIVE_DAYS_LOGGING -> {
                    val streak = _streaks.value[StreakType.DAILY_LOGGING]
                    streak?.currentCount ?: 0 >= requirement.target
                }
                RequirementType.SAFETY_PRACTICES_USED -> {
                    // Check safety practice usage
                    true // Simplified for now
                }
                else -> false
            }
        }
    }
    
    override suspend fun awardXP(amount: Long, reason: String): UserLevel {
        val currentLevel = _userLevel.value
        val newTotalXP = currentLevel.totalXP + amount
        val newLevel = UserLevel.calculateLevelFromTotalXP(newTotalXP)
        
        _userLevel.value = newLevel
        saveGamificationData()
        updateGamificationStats()
        
        return newLevel
    }
    
    override suspend fun updateStreak(type: StreakType, date: Instant) {
        val currentStreaks = _streaks.value.toMutableMap()
        val streak = currentStreaks[type] ?: Streak(type = type)
        
        val updatedStreak = if (streak.shouldResetStreak(date)) {
            // Reset streak if too much time has passed
            streak.copy(
                currentCount = 1,
                lastActivityDate = date,
                isActive = true
            )
        } else {
            // Continue or start streak
            val newCount = streak.currentCount + 1
            streak.copy(
                currentCount = newCount,
                bestCount = maxOf(streak.bestCount, newCount),
                lastActivityDate = date,
                isActive = true
            )
        }
        
        currentStreaks[type] = updatedStreak
        _streaks.value = currentStreaks
        saveGamificationData()
        updateGamificationStats()
    }
    
    override suspend fun calculateExperienceXP(experienceId: String): Long {
        // Base XP for logging an experience
        var xp = 25L
        
        // TODO: Add bonus XP based on:
        // - Completeness of documentation
        // - Safety practices used
        // - Detail quality
        // - Integration notes
        
        return xp
    }
    
    override suspend fun processEvent(event: GamificationEvent): GamificationResult {
        val initialLevel = _userLevel.value
        
        // Award base XP for the event
        val baseXP = when (event.type) {
            GamificationEventType.EXPERIENCE_CREATED -> 25L
            GamificationEventType.EXPERIENCE_DETAILED -> 50L
            GamificationEventType.SAFETY_PRACTICE_USED -> 10L
            GamificationEventType.INTEGRATION_COMPLETED -> 30L
            GamificationEventType.QUEST_COMPLETED -> 75L
            GamificationEventType.KNOWLEDGE_GAINED -> 15L
            GamificationEventType.APP_LAUNCHED -> 5L
            else -> 0L
        }
        
        val newLevel = awardXP(baseXP, event.type.name)
        val levelUp = newLevel.currentLevel > initialLevel.currentLevel
        
        // Check for new achievements
        val newAchievements = checkAchievements(event)
        
        // Update relevant streaks
        val streakUpdates = mutableListOf<Streak>()
        when (event.type) {
            GamificationEventType.EXPERIENCE_CREATED -> {
                updateStreak(StreakType.DAILY_LOGGING, event.timestamp)
                streakUpdates.add(_streaks.value[StreakType.DAILY_LOGGING]!!)
            }
            GamificationEventType.INTEGRATION_COMPLETED -> {
                updateStreak(StreakType.INTEGRATION_PRACTICE, event.timestamp)
                streakUpdates.add(_streaks.value[StreakType.INTEGRATION_PRACTICE]!!)
            }
            GamificationEventType.APP_LAUNCHED -> {
                updateStreak(StreakType.APP_USAGE, event.timestamp)
                streakUpdates.add(_streaks.value[StreakType.APP_USAGE]!!)
            }
            else -> {}
        }
        
        // Generate notifications
        val notifications = mutableListOf<GamificationNotification>()
        
        if (levelUp) {
            notifications.add(
                GamificationNotification(
                    type = NotificationType.LEVEL_UP,
                    title = "Level Up!",
                    message = "Congratulations! You've reached level ${newLevel.currentLevel}",
                    iconResource = "level_up"
                )
            )
        }
        
        newAchievements.forEach { achievement ->
            notifications.add(
                GamificationNotification(
                    type = NotificationType.ACHIEVEMENT_UNLOCKED,
                    title = "Achievement Unlocked!",
                    message = achievement.name,
                    iconResource = achievement.iconResource
                )
            )
        }
        
        return GamificationResult(
            xpAwarded = baseXP,
            newAchievements = newAchievements,
            streakUpdates = streakUpdates,
            levelUp = levelUp,
            newLevel = if (levelUp) newLevel else null,
            notifications = notifications
        )
    }
    
    private suspend fun updateGamificationStats() {
        val achievements = _achievements.value
        val streaks = _streaks.value
        val level = _userLevel.value
        
        _gamificationStats.value = GamificationStats(
            totalXP = level.totalXP,
            currentLevel = level,
            achievementsUnlocked = achievements.size,
            totalAchievements = allAchievements.size,
            longestStreak = streaks.values.maxOfOrNull { it.bestCount } ?: 0,
            questsCompleted = _questProgress.value.count { it.isCompleted },
            safetyScore = _safetyScore.value,
            levelProgress = level.getProgressPercentage()
        )
    }
    
    // Implementation stubs for other interface methods
    override suspend fun getAvailableAchievements(): List<Achievement> = allAchievements
    override suspend fun getUnlockedAchievements(): List<UserAchievement> = _achievements.value
    override suspend fun getAchievementProgress(achievementId: String): Map<String, Long> = emptyMap()
    override suspend fun getXPBreakdown(): Map<String, Long> = emptyMap()
    override suspend fun getActiveStreaks(): List<Streak> = _streaks.value.values.filter { it.isActive }
    override suspend fun getStreakHistory(type: StreakType): List<Pair<Instant, Int>> = emptyList()
    override suspend fun getAvailableQuests(): List<KnowledgeQuest> = allQuests.filter { it.isUnlocked }
    override suspend fun startQuest(questId: String): Result<UserQuestProgress> = Result.failure(NotImplementedError())
    override suspend fun completeQuestStep(questId: String, stepId: String, answer: String?): Result<Boolean> = Result.failure(NotImplementedError())
    override suspend fun getQuestProgress(questId: String): UserQuestProgress? = null
    override suspend fun getCompletedQuests(): List<UserQuestProgress> = _questProgress.value.filter { it.isCompleted }
    override suspend fun getCurrentChallenges(): List<WeeklyChallenge> = emptyList()
    override suspend fun getChallengeProgress(challengeId: String): UserChallengeProgress? = null
    override suspend fun updateChallengeProgress(challengeId: String, event: GamificationEvent) {}
    override suspend fun updateSafetyScore(experienceId: String?) {}
    override suspend fun getSafetyTrends(): List<Pair<Instant, Double>> = emptyList()
    override suspend fun getSafetyInsights(): List<String> = emptyList()
    override suspend fun getRecentEvents(limit: Int): List<GamificationEvent> = emptyList()
    override suspend fun getProgressInsights(): List<ProgressInsight> = emptyList()
    override suspend fun getPersonalizedChallenges(): List<PersonalizedChallenge> = emptyList()
    override suspend fun getGamificationAnalytics(): GamificationAnalytics {
        return GamificationAnalytics(
            totalEngagementDays = 0,
            averageXPPerWeek = 0.0,
            mostActiveCategory = AchievementCategory.SAFETY_FIRST,
            streakData = emptyMap(),
            safetyTrend = ScoreTrend.INSUFFICIENT_DATA,
            completionRates = emptyMap(),
            timeToNextLevel = EstimatedTime(0, 0.0),
            nextMilestone = null
        )
    }
}