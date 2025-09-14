package com.isaakhanimann.journal.ui.viewmodel

import com.isaakhanimann.journal.gamification.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class GamificationUiState(
    val userLevel: UserLevel = UserLevel(),
    val stats: GamificationStats = GamificationStats(
        totalXP = 0,
        currentLevel = UserLevel(),
        achievementsUnlocked = 0,
        totalAchievements = 0,
        longestStreak = 0,
        questsCompleted = 0,
        safetyScore = null
    ),
    val unlockedAchievements: List<UserAchievement> = emptyList(),
    val availableAchievements: List<Achievement> = emptyList(),
    val recentAchievements: List<UserAchievement> = emptyList(),
    val streaks: Map<StreakType, Streak> = emptyMap(),
    val availableQuests: List<KnowledgeQuest> = emptyList(),
    val questProgress: List<UserQuestProgress> = emptyList(),
    val completedQuests: List<UserQuestProgress> = emptyList(),
    val safetyScore: SafetyScore? = null,
    val progressInsights: List<ProgressInsight> = emptyList(),
    val personalizedChallenges: List<PersonalizedChallenge> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class GamificationViewModel : BaseViewModel(), KoinComponent {
    private val gamificationService: GamificationService by inject()
    
    private val _uiState = MutableStateFlow(GamificationUiState())
    val uiState: StateFlow<GamificationUiState> = _uiState.asStateFlow()
    
    init {
        observeGamificationData()
        loadInitialData()
    }
    
    private fun observeGamificationData() {
        viewModelScope.launch {
            gamificationService.userLevel
                .combine(gamificationService.gamificationStats) { userLevel, stats -> userLevel to stats }
                .combine(gamificationService.achievements) { (userLevel, stats), achievements -> Triple(userLevel, stats, achievements) }
                .combine(gamificationService.streaks) { (userLevel, stats, achievements), streaks -> 
                    listOf(userLevel, stats, achievements, streaks) 
                }
                .combine(gamificationService.questProgress) { list, questProgress -> 
                    list + questProgress
                }
                .combine(gamificationService.safetyScore) { list, safetyScore ->
                    val userLevel = list[0] as UserLevel
                    val stats = list[1] as GamificationStats
                    val achievements = list[2] as List<UserAchievement>
                    val streaks = list[3] as Map<StreakType, Streak>
                    val questProgress = list[4] as List<UserQuestProgress>
                    
                    GamificationUiState(
                        userLevel = userLevel,
                        stats = stats,
                        unlockedAchievements = achievements,
                        recentAchievements = achievements.sortedByDescending { it.unlockedAt }.take(5),
                        streaks = streaks,
                        questProgress = questProgress,
                        completedQuests = questProgress.filter { it.isCompleted },
                        safetyScore = safetyScore,
                        isLoading = false
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
        }
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                val availableAchievements = gamificationService.getAvailableAchievements()
                val availableQuests = gamificationService.getAvailableQuests()
                val progressInsights = gamificationService.getProgressInsights()
                val personalizedChallenges = gamificationService.getPersonalizedChallenges()
                
                _uiState.value = _uiState.value.copy(
                    availableAchievements = availableAchievements,
                    availableQuests = availableQuests,
                    progressInsights = progressInsights,
                    personalizedChallenges = personalizedChallenges,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun processExperienceCreated(experienceId: String) {
        viewModelScope.launch {
            val event = GamificationEvent(
                type = GamificationEventType.EXPERIENCE_CREATED,
                timestamp = kotlinx.datetime.Clock.System.now(),
                experienceId = experienceId
            )
            
            try {
                val result = gamificationService.processEvent(event)
                handleGamificationResult(result)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun processIntegrationCompleted(experienceId: String) {
        viewModelScope.launch {
            val event = GamificationEvent(
                type = GamificationEventType.INTEGRATION_COMPLETED,
                timestamp = kotlinx.datetime.Clock.System.now(),
                experienceId = experienceId
            )
            
            try {
                val result = gamificationService.processEvent(event)
                handleGamificationResult(result)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun processSafetyPracticeUsed(practiceType: String) {
        viewModelScope.launch {
            val event = GamificationEvent(
                type = GamificationEventType.SAFETY_PRACTICE_USED,
                timestamp = kotlinx.datetime.Clock.System.now(),
                metadata = mapOf("practice_type" to practiceType)
            )
            
            try {
                val result = gamificationService.processEvent(event)
                handleGamificationResult(result)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun processAppLaunched() {
        viewModelScope.launch {
            val event = GamificationEvent(
                type = GamificationEventType.APP_LAUNCHED,
                timestamp = kotlinx.datetime.Clock.System.now()
            )
            
            try {
                gamificationService.processEvent(event)
            } catch (e: Exception) {
                // App launch events are low priority, don't show errors
            }
        }
    }
    
    fun startQuest(questId: String) {
        viewModelScope.launch {
            try {
                gamificationService.startQuest(questId)
                // Refresh quest progress
                loadInitialData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun completeQuestStep(questId: String, stepId: String, answer: String? = null) {
        viewModelScope.launch {
            try {
                val isStepCompleted = gamificationService.completeQuestStep(questId, stepId, answer)
                if (isStepCompleted.isSuccess && isStepCompleted.getOrNull() == true) {
                    // Check if quest is now complete
                    val questProgress = gamificationService.getQuestProgress(questId)
                    if (questProgress?.isCompleted == true) {
                        val event = GamificationEvent(
                            type = GamificationEventType.QUEST_COMPLETED,
                            timestamp = kotlinx.datetime.Clock.System.now(),
                            metadata = mapOf("quest_id" to questId)
                        )
                        val result = gamificationService.processEvent(event)
                        handleGamificationResult(result)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun updateSafetyScore() {
        viewModelScope.launch {
            try {
                gamificationService.updateSafetyScore()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun handleGamificationResult(result: GamificationResult) {
        // Handle notifications - these could trigger UI animations or toast messages
        result.notifications.forEach { notification ->
            when (notification.type) {
                NotificationType.LEVEL_UP -> {
                    // Could trigger a level up animation
                }
                NotificationType.ACHIEVEMENT_UNLOCKED -> {
                    // Could trigger achievement unlock animation
                }
                NotificationType.STREAK_MILESTONE -> {
                    // Could trigger streak celebration
                }
                else -> {
                    // Handle other notification types
                }
            }
        }
        
        // Refresh data to show latest changes
        if (result.xpAwarded > 0 || result.newAchievements.isNotEmpty() || result.levelUp) {
            loadInitialData()
        }
    }
    
    // Utility functions for UI
    fun getNextLevelRequirement(): Long {
        return _uiState.value.userLevel.xpToNextLevel - _uiState.value.userLevel.currentXP
    }
    
    fun getProgressToNextAchievement(): String? {
        val availableAchievements = _uiState.value.availableAchievements
        val unlockedIds = _uiState.value.unlockedAchievements.map { it.achievementId }.toSet()
        
        val nextAchievement = availableAchievements
            .filter { it.id !in unlockedIds }
            .minByOrNull { 
                // Simple priority based on XP reward (lower = easier to get)
                it.xpReward
            }
        
        return nextAchievement?.let { 
            "Next: ${it.name} (+${it.xpReward} XP)"
        }
    }
    
    fun getActiveStreakCount(): Int {
        return _uiState.value.streaks.values.count { it.isActive && it.currentCount > 0 }
    }
    
    fun getTotalXPThisWeek(): Long {
        // This would calculate XP earned in the current week
        // For now, return a simplified calculation
        return _uiState.value.stats.totalXP % 500 // Simplified
    }
    
    fun getSafetyScoreImprovement(): String? {
        val safetyScore = _uiState.value.safetyScore ?: return null
        
        return when (safetyScore.trend) {
            ScoreTrend.IMPROVING -> "↗️ Safety practices improving"
            ScoreTrend.DECLINING -> "↘️ Review safety practices"
            ScoreTrend.STABLE -> "→ Consistent safety practices"
            ScoreTrend.INSUFFICIENT_DATA -> "📊 Need more data"
        }
    }
}