package com.isaakhanimann.journal.plugin.builtin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.isaakhanimann.journal.gamification.*
import com.isaakhanimann.journal.plugin.*
import com.isaakhanimann.journal.data.model.Experience
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GamificationPlugin : Plugin, KoinComponent {
    private val gamificationService: GamificationService by inject()
    
    override val manifest = PluginManifest(
        id = "gamification_system",
        name = "Gamification System",
        version = "1.0.0",
        description = "Transform harm reduction into an engaging, educational experience with levels, achievements, and knowledge quests",
        author = "PsychonautWiki Journal Team",
        permissions = listOf(
            Permission.READ_EXPERIENCES,
            Permission.WRITE_EXPERIENCES,
            Permission.ANALYTICS_ACCESS,
            Permission.SEND_NOTIFICATIONS
        ),
        entryPoint = "com.isaakhanimann.journal.plugin.builtin.GamificationPlugin"
    )
    
    private lateinit var context: PluginContext
    
    override suspend fun initialize(context: PluginContext): Result<Unit> {
        return try {
            this.context = context
            
            // Set up experience tracking
            context.experienceRepository.getAllExperiences().collect { experiences ->
                processExperienceUpdates(experiences)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun shutdown(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override fun getCapabilities(): List<PluginCapability> {
        return listOf(
            GamificationAnalyticsCapability().analyticsCapability,
            GamificationVisualizationCapability().visualizationCapability
            // GamificationUserInterfaceCapability().uiCapability
        )
    }
    
    private suspend fun processExperienceUpdates(experiences: List<Experience>) {
        // Process new experiences for gamification events
        experiences.forEach { experience ->
            val event = GamificationEvent(
                type = GamificationEventType.EXPERIENCE_CREATED,
                timestamp = Clock.System.now(),
                experienceId = experience.id.toString(),
                xpAwarded = gamificationService.calculateExperienceXP(experience.id.toString())
            )
            
            val result = gamificationService.processEvent(event)
            
            // Send notifications for achievements and level ups
            result.notifications.forEach { notification ->
                when (notification.type) {
                    NotificationType.LEVEL_UP -> {
                        context.notifications.showNotification(
                            title = notification.title,
                            message = notification.message,
                            severity = NotificationSeverity.SUCCESS
                        )
                    }
                    NotificationType.ACHIEVEMENT_UNLOCKED -> {
                        context.notifications.showNotification(
                            title = notification.title,
                            message = notification.message,
                            severity = NotificationSeverity.SUCCESS
                        )
                    }
                    else -> {
                        context.notifications.showNotification(
                            title = notification.title,
                            message = notification.message,
                            severity = NotificationSeverity.INFO
                        )
                    }
                }
            }
        }
    }
    
    inner class GamificationAnalyticsCapability {
        val analyticsCapability = AnalyticsCapability(
            id = "gamification_analytics",
            name = "Gamification Analytics",
            description = "Provides user progress analytics and achievement insights",
            analyzeFunction = ::analyzeGamificationProgress
        )
    }
    
    inner class GamificationVisualizationCapability {
        val visualizationCapability = VisualizationCapability(
            id = "gamification_visualization",
            name = "Progress Visualization",
            description = "Visualizes user progress and achievements",
            visualizationComponent = ::generateProgressVisualization
        )
    }
    
    /*
    inner class GamificationUserInterfaceCapability {
        val uiCapability = UserInterfaceCapability(
            id = "gamification_ui",
            name = "Gamification Dashboard",
            description = "Displays progress, achievements, and quests",
            renderComponent = ::renderGamificationDashboard
        )
        
        private fun renderGamificationDashboard(context: UIContext): UIComponent {
            return GamificationDashboardComponent(
                userLevel = context.data["userLevel"] as? UserLevel ?: UserLevel(),
                achievements = context.data["achievements"] as? List<UserAchievement> ?: emptyList(),
                quests = context.data["quests"] as? List<KnowledgeQuest> ?: emptyList(),
                streaks = context.data["streaks"] as? Map<StreakType, Streak> ?: emptyMap()
            )
        }
    }
    */
    
    private suspend fun analyzeGamificationProgress(context: AnalyticsContext): AnalyticsResult {
        val stats = gamificationService.getGamificationStats()
        val insights = gamificationService.getProgressInsights()
        val challenges = gamificationService.getPersonalizedChallenges()
        
        val gamificationInsights = insights.map { insight ->
            Insight(
                id = insight.type.name,
                title = insight.title,
                description = insight.description,
                confidence = 0.9,
                severity = when (insight.type) {
                    InsightType.SAFETY_IMPROVEMENT -> InsightSeverity.HIGH
                    InsightType.ACHIEVEMENT_OPPORTUNITY -> InsightSeverity.MEDIUM
                    else -> InsightSeverity.LOW
                }
            )
        }
        
        val recommendations = challenges.map { challenge ->
            Recommendation(
                id = challenge.id,
                title = challenge.title,
                description = challenge.description,
                actionable = true,
                priority = when (challenge.difficulty) {
                    ChallengeDifficulty.BEGINNER -> RecommendationPriority.LOW
                    ChallengeDifficulty.INTERMEDIATE -> RecommendationPriority.MEDIUM
                    ChallengeDifficulty.ADVANCED -> RecommendationPriority.HIGH
                    ChallengeDifficulty.EXPERT -> RecommendationPriority.URGENT
                },
                category = when (challenge.category) {
                    AchievementCategory.SAFETY_FIRST -> RecommendationCategory.SAFETY
                    AchievementCategory.INTEGRATION -> RecommendationCategory.INTEGRATION
                    AchievementCategory.HARM_REDUCTION -> RecommendationCategory.HEALTH
                    else -> RecommendationCategory.OPTIMIZATION
                }
            )
        }
        
        val progressVisualization = VisualizationData(
            type = VisualizationType.BAR_CHART,
            title = "Progress Overview",
            description = "Your gamification progress across different categories",
            data = mapOf(
                "level" to stats.currentLevel.currentLevel,
                "xp" to stats.totalXP,
                "achievements" to stats.achievementsUnlocked,
                "streaks" to stats.longestStreak
            )
        )
        
        return AnalyticsResult(
            insights = gamificationInsights,
            recommendations = recommendations,
            visualizations = listOf(progressVisualization)
        )
    }
    
    @Composable
    private fun GamificationVisualizationComponent(context: VisualizationContext) {
        val stats by gamificationService.gamificationStats.collectAsState()
        val level by gamificationService.userLevel.collectAsState()
        val achievements by gamificationService.achievements.collectAsState()
        val streaks by gamificationService.streaks.collectAsState()
        
        // This would be implemented with actual Compose UI components
        // For now, this is a placeholder that shows the structure
    }
    
    private suspend fun processGamificationAIQuery(context: AIContext): AIResult {
        val query = context.query.lowercase()
        val stats = gamificationService.gamificationStats.value
        val insights = gamificationService.getProgressInsights()
        
        val response = when {
            "progress" in query || "level" in query -> {
                "You're currently at level ${stats.currentLevel.currentLevel} with ${stats.totalXP} total XP! " +
                "You've unlocked ${stats.achievementsUnlocked} out of ${stats.totalAchievements} achievements. " +
                "Keep up the great work with your harm reduction journey!"
            }
            
            "achievement" in query -> {
                val availableAchievements = gamificationService.getAvailableAchievements()
                val nextAchievement = availableAchievements.firstOrNull { achievement ->
                    gamificationService.getUnlockedAchievements().none { it.achievementId == achievement.id }
                }
                
                if (nextAchievement != null) {
                    "Your next achievement to unlock is '${nextAchievement.name}': ${nextAchievement.description}. " +
                    "This will reward you with ${nextAchievement.xpReward} XP!"
                } else {
                    "Congratulations! You've unlocked all available achievements. More are coming soon!"
                }
            }
            
            "streak" in query -> {
                val activeStreaks = gamificationService.getActiveStreaks()
                if (activeStreaks.isNotEmpty()) {
                    val streakInfo = activeStreaks.joinToString(", ") { 
                        "${it.type.name}: ${it.currentCount} days" 
                    }
                    "Your current active streaks: $streakInfo. Keep it up!"
                } else {
                    "You don't have any active streaks right now. Start by logging an experience today!"
                }
            }
            
            "quest" in query || "learn" in query -> {
                val availableQuests = gamificationService.getAvailableQuests()
                if (availableQuests.isNotEmpty()) {
                    val quest = availableQuests.first()
                    "Try the '${quest.title}' knowledge quest! It covers ${quest.category.name.lowercase()} " +
                    "and takes about ${quest.estimatedTimeMinutes} minutes. You'll earn ${quest.xpReward} XP!"
                } else {
                    "Complete some basic activities to unlock your first knowledge quest!"
                }
            }
            
            "safety" in query -> {
                val safetyScore = stats.safetyScore
                if (safetyScore != null) {
                    "Your safety score is ${safetyScore.overallScore.toInt()}/100. " +
                    when (safetyScore.trend) {
                        ScoreTrend.IMPROVING -> "Great news - your safety practices are improving!"
                        ScoreTrend.STABLE -> "You're maintaining consistent safety practices."
                        ScoreTrend.DECLINING -> "Consider focusing more on harm reduction practices."
                        ScoreTrend.INSUFFICIENT_DATA -> "Log more experiences to get better safety insights."
                    }
                } else {
                    "Log a few experiences with safety details to get your safety score!"
                }
            }
            
            else -> {
                "I can help you with your gamification progress! Ask me about your level, achievements, " +
                "streaks, knowledge quests, or safety score."
            }
        }
        
        val suggestions = listOf(
            "Check your current level and XP",
            "See available achievements",
            "View your streaks",
            "Find knowledge quests to complete",
            "Review your safety score"
        )
        
        return AIResult(
            response = response,
            confidence = 0.95,
            suggestions = suggestions
        )
    }
}