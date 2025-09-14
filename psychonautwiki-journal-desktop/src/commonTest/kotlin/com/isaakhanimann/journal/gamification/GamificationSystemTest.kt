package com.isaakhanimann.journal.gamification

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class GamificationSystemTest : StringSpec({
    
    "UserLevel should calculate correct XP requirements" {
        val level1XP = UserLevel.calculateXPRequiredForLevel(1)
        val level5XP = UserLevel.calculateXPRequiredForLevel(5)
        val level10XP = UserLevel.calculateXPRequiredForLevel(10)
        
        level1XP shouldBe 100L
        level5XP shouldBe 2500L // 5^2 * 100
        level10XP shouldBe 10000L // 10^2 * 100
    }
    
    "UserLevel should correctly calculate level from total XP" {
        val level1 = UserLevel.calculateLevelFromTotalXP(50L)
        level1.currentLevel shouldBe 1
        level1.currentXP shouldBe 50L
        level1.xpToNextLevel shouldBe 100L
        
        val level2 = UserLevel.calculateLevelFromTotalXP(150L)
        level2.currentLevel shouldBe 2
        level2.currentXP shouldBe 50L // 150 - 100 (level 1 XP)
        level2.xpToNextLevel shouldBe 400L // Level 2 requires 400 XP
        
        val level5 = UserLevel.calculateLevelFromTotalXP(5000L)
        level5.currentLevel shouldBeGreaterThanOrEqual 3
    }
    
    "UserLevel progress percentage should be calculated correctly" {
        val userLevel = UserLevel(
            currentLevel = 2,
            currentXP = 200L,
            xpToNextLevel = 400L,
            totalXP = 300L
        )
        
        userLevel.getProgressPercentage() shouldBe 0.5f
    }
    
    "Achievement requirements should be properly defined" {
        val achievement = Achievement(
            id = "test_achievement",
            name = "Test Achievement",
            description = "Test description",
            category = AchievementCategory.SAFETY_FIRST,
            tier = AchievementTier.BRONZE,
            xpReward = 100L,
            iconResource = "test_icon",
            requirements = listOf(
                AchievementRequirement(
                    type = RequirementType.EXPERIENCES_LOGGED,
                    target = 10L
                )
            )
        )
        
        achievement.requirements shouldHaveSize 1
        achievement.requirements.first().type shouldBe RequirementType.EXPERIENCES_LOGGED
        achievement.requirements.first().target shouldBe 10L
    }
    
    "Streak should correctly identify when reset is needed" {
        val now = Clock.System.now()
        val yesterday = Instant.fromEpochSeconds(now.epochSeconds - 86400) // 1 day ago
        val twoDaysAgo = Instant.fromEpochSeconds(now.epochSeconds - 172800) // 2 days ago
        
        val activeStreak = Streak(
            type = StreakType.DAILY_LOGGING,
            currentCount = 5,
            lastActivityDate = yesterday
        )
        
        val inactiveStreak = Streak(
            type = StreakType.DAILY_LOGGING,
            currentCount = 5,
            lastActivityDate = twoDaysAgo
        )
        
        activeStreak.shouldResetStreak(now) shouldBe false
        inactiveStreak.shouldResetStreak(now) shouldBe true
    }
    
    "Knowledge quest should have proper structure" {
        val quest = KnowledgeQuest(
            id = "test_quest",
            title = "Test Quest",
            description = "A test knowledge quest",
            category = QuestCategory.DOSAGE_CALCULATION,
            difficulty = QuestDifficulty.BEGINNER,
            xpReward = 75L,
            estimatedTimeMinutes = 15,
            isUnlocked = true,
            steps = listOf(
                QuestStep(
                    id = "step1",
                    title = "Step 1",
                    description = "First step",
                    type = QuestStepType.INFORMATION,
                    content = "Educational content"
                ),
                QuestStep(
                    id = "step2",
                    title = "Step 2",
                    description = "Quiz step",
                    type = QuestStepType.QUIZ,
                    content = "Quiz question",
                    requiredAnswer = "correct answer"
                )
            )
        )
        
        quest.steps shouldHaveSize 2
        quest.steps.first().type shouldBe QuestStepType.INFORMATION
        quest.steps.last().type shouldBe QuestStepType.QUIZ
        quest.steps.last().requiredAnswer shouldBe "correct answer"
    }
    
    "Safety score should have valid components" {
        val safetyScore = SafetyScore(
            overallScore = 85.5,
            components = mapOf(
                SafetyComponent.DOSAGE_PRECISION to 90.0,
                SafetyComponent.TESTING_FREQUENCY to 80.0,
                SafetyComponent.SET_SETTING_PREP to 85.0
            ),
            trend = ScoreTrend.IMPROVING,
            lastUpdated = Clock.System.now(),
            improvementAreas = listOf("Consider more frequent substance testing")
        )
        
        safetyScore.overallScore shouldBeGreaterThan 80.0
        safetyScore.components shouldContain (SafetyComponent.DOSAGE_PRECISION to 90.0)
        safetyScore.trend shouldBe ScoreTrend.IMPROVING
        safetyScore.improvementAreas shouldHaveSize 1
    }
    
    "Gamification event should be properly structured" {
        val event = GamificationEvent(
            type = GamificationEventType.EXPERIENCE_CREATED,
            timestamp = Clock.System.now(),
            experienceId = "123",
            metadata = mapOf("quality" to "high"),
            xpAwarded = 50L
        )
        
        event.type shouldBe GamificationEventType.EXPERIENCE_CREATED
        event.experienceId shouldBe "123"
        event.metadata shouldContain ("quality" to "high")
        event.xpAwarded shouldBe 50L
    }
    
    "Weekly challenge should have proper time bounds" {
        val now = Clock.System.now()
        val oneWeekLater = Instant.fromEpochSeconds(now.epochSeconds + (7 * 24 * 60 * 60))
        
        val challenge = WeeklyChallenge(
            id = "weekly_safety",
            title = "Weekly Safety Challenge",
            description = "Use 3 different safety practices",
            category = AchievementCategory.SAFETY_FIRST,
            xpReward = 200L,
            startDate = now,
            endDate = oneWeekLater,
            requirements = listOf(
                AchievementRequirement(
                    type = RequirementType.SAFETY_PRACTICES_USED,
                    target = 3L,
                    timeFrame = TimeFrame.WEEKLY
                )
            )
        )
        
        challenge.endDate.epochSeconds shouldBeGreaterThan challenge.startDate.epochSeconds
        challenge.requirements.first().timeFrame shouldBe TimeFrame.WEEKLY
    }
    
    "Gamification stats should aggregate correctly" {
        val stats = GamificationStats(
            totalXP = 1500L,
            currentLevel = UserLevel(currentLevel = 3, currentXP = 200L, xpToNextLevel = 900L, totalXP = 1500L),
            achievementsUnlocked = 8,
            totalAchievements = 15,
            longestStreak = 21,
            questsCompleted = 5,
            safetyScore = SafetyScore(
                overallScore = 82.0,
                components = emptyMap(),
                trend = ScoreTrend.STABLE,
                lastUpdated = Clock.System.now()
            ),
            levelProgress = 0.22f
        )
        
        stats.totalXP shouldBe 1500L
        stats.currentLevel.currentLevel shouldBe 3
        stats.achievementsUnlocked shouldBe 8
        stats.totalAchievements shouldBe 15
        stats.longestStreak shouldBe 21
        stats.questsCompleted shouldBe 5
        stats.safetyScore shouldNotBe null
        stats.levelProgress shouldBe 0.22f
    }
})