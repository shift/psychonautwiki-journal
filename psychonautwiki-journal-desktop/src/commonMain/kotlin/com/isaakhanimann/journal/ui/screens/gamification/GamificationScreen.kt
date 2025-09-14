package com.isaakhanimann.journal.ui.screens.gamification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.gamification.*
import com.isaakhanimann.journal.ui.components.gamification.*
import com.isaakhanimann.journal.ui.viewmodel.GamificationViewModel
import com.isaakhanimann.journal.ui.viewmodel.GamificationUiState
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationScreen(
    viewModel: GamificationViewModel = koinInject(),
    onNavigateToQuest: (String) -> Unit = {},
    onNavigateToAchievement: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with tabs
        var selectedTab by remember { mutableStateOf(GamificationTab.OVERVIEW) }
        
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            GamificationTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.displayName) },
                    icon = { Icon(tab.icon, contentDescription = null) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content based on selected tab
        when (selectedTab) {
            GamificationTab.OVERVIEW -> OverviewContent(uiState)
            GamificationTab.ACHIEVEMENTS -> AchievementsContent(
                uiState = uiState,
                onAchievementClick = onNavigateToAchievement
            )
            GamificationTab.QUESTS -> QuestsContent(
                uiState = uiState,
                onQuestClick = onNavigateToQuest
            )
            GamificationTab.CHALLENGES -> ChallengesContent(uiState)
            GamificationTab.PROGRESS -> ProgressContent(uiState)
        }
    }
}

@Composable
private fun OverviewContent(uiState: GamificationUiState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            LevelProgressCard(
                userLevel = uiState.userLevel,
                animated = true
            )
        }
        
        item {
            QuickStatsRow(stats = uiState.stats)
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StreakDisplay(
                    streaks = uiState.streaks,
                    modifier = Modifier.weight(1f)
                )
                
                SafetyScoreCard(
                    safetyScore = uiState.safetyScore,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        if (uiState.recentAchievements.isNotEmpty()) {
            item {
                RecentAchievementsSection(
                    achievements = uiState.recentAchievements,
                    allAchievements = uiState.availableAchievements
                )
            }
        }
        
        if (uiState.availableQuests.isNotEmpty()) {
            item {
                FeaturedQuestsSection(
                    quests = uiState.availableQuests.take(3)
                )
            }
        }
        
        if (uiState.progressInsights.isNotEmpty()) {
            item {
                ProgressInsightsSection(insights = uiState.progressInsights)
            }
        }
    }
}

@Composable
private fun AchievementsContent(
    uiState: GamificationUiState,
    onAchievementClick: (String) -> Unit
) {
    AchievementGrid(
        achievements = uiState.unlockedAchievements,
        allAchievements = uiState.availableAchievements,
        onAchievementClick = { achievement ->
            onAchievementClick(achievement.id)
        }
    )
}

@Composable
private fun QuestsContent(
    uiState: GamificationUiState,
    onQuestClick: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uiState.availableQuests) { quest ->
            QuestCard(
                quest = quest,
                onClick = { onQuestClick(quest.id) }
            )
        }
        
        if (uiState.completedQuests.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Completed Quests",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(uiState.completedQuests) { questProgress ->
                val quest = uiState.availableQuests.find { it.id == questProgress.questId }
                if (quest != null) {
                    CompletedQuestCard(
                        quest = quest,
                        progress = questProgress
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressContent(uiState: GamificationUiState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DetailedLevelProgress(uiState.userLevel)
        }
        
        item {
            StreakAnalytics(uiState.streaks)
        }
        
        if (uiState.safetyScore != null) {
            item {
                DetailedSafetyAnalysis(uiState.safetyScore!!)
            }
        }
        
        item {
            XPBreakdownCard(uiState.stats)
        }
    }
}

@Composable
private fun RecentAchievementsSection(
    achievements: List<UserAchievement>,
    allAchievements: List<Achievement>
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recent Achievements",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(achievements.take(5)) { userAchievement ->
                    val achievement = allAchievements.find { it.id == userAchievement.achievementId }
                    if (achievement != null) {
                        AchievementBadge(
                            achievement = achievement,
                            isUnlocked = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedQuestsSection(quests: List<KnowledgeQuest>) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Available Quests",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            quests.forEach { quest ->
                QuestPreviewItem(quest = quest)
                if (quest != quests.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ProgressInsightsSection(insights: List<ProgressInsight>) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Insights,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Progress Insights",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            insights.take(3).forEach { insight ->
                InsightItem(insight = insight)
                if (insight != insights.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// Additional detailed components
@Composable
private fun QuestCard(
    quest: KnowledgeQuest,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        text = quest.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "+${quest.xpReward} XP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "${quest.estimatedTimeMinutes}min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                DifficultyChip(difficulty = quest.difficulty)
                
                Spacer(modifier = Modifier.width(8.dp))
                
                CategoryChip(category = quest.category)
            }
        }
    }
}

@Composable
private fun QuestPreviewItem(quest: KnowledgeQuest) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.PlayCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quest.title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${quest.estimatedTimeMinutes}min • +${quest.xpReward} XP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun InsightItem(insight: ProgressInsight) {
    Row {
        Icon(
            imageVector = when (insight.type) {
                InsightType.LEVEL_PROGRESS -> Icons.Filled.TrendingUp
                InsightType.ACHIEVEMENT_OPPORTUNITY -> Icons.Filled.EmojiEvents
                InsightType.STREAK_ENCOURAGEMENT -> Icons.Filled.LocalFireDepartment
                InsightType.SAFETY_IMPROVEMENT -> Icons.Filled.Security
                InsightType.LEARNING_SUGGESTION -> Icons.Filled.School
                InsightType.CONSISTENCY_FEEDBACK -> Icons.Filled.EventRepeat
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = insight.title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DifficultyChip(difficulty: QuestDifficulty) {
    val color = when (difficulty) {
        QuestDifficulty.BEGINNER -> MaterialTheme.colorScheme.primary
        QuestDifficulty.INTERMEDIATE -> MaterialTheme.colorScheme.secondary
        QuestDifficulty.ADVANCED -> MaterialTheme.colorScheme.tertiary
        QuestDifficulty.EXPERT -> MaterialTheme.colorScheme.error
    }
    
    AssistChip(
        onClick = { },
        label = { Text(difficulty.name.lowercase().replaceFirstChar { it.uppercase() }) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        )
    )
}

@Composable
private fun CategoryChip(category: QuestCategory) {
    AssistChip(
        onClick = { },
        label = { 
            Text(
                category.name.replace("_", " ").lowercase()
                    .split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { it.uppercase() }
                    }
            ) 
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

// Additional detailed components (stubs for now)
@Composable
private fun DetailedLevelProgress(userLevel: UserLevel) {
    // Detailed level progression with milestones
}

@Composable
private fun StreakAnalytics(streaks: Map<StreakType, Streak>) {
    // Detailed streak analysis and history
}

@Composable
private fun DetailedSafetyAnalysis(safetyScore: SafetyScore) {
    // Component-by-component safety score breakdown
}

@Composable
private fun XPBreakdownCard(stats: GamificationStats) {
    // XP sources and earning breakdown
}

@Composable
private fun CompletedQuestCard(
    quest: KnowledgeQuest,
    progress: UserQuestProgress
) {
    // Card showing completed quest with completion date
}

@Composable
private fun ChallengesContent(uiState: GamificationUiState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Weekly Challenge
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Weekly Challenge",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Mock weekly challenge data
                    val mockChallenge = WeeklyChallenge(
                        id = "challenge-1",
                        title = "Safety First Week",
                        description = "Complete 3 detailed experience logs with safety information",
                        category = AchievementCategory.SAFETY_FIRST,
                        xpReward = 150,
                        requirements = emptyList(),
                        startDate = kotlinx.datetime.Clock.System.now(),
                        endDate = kotlinx.datetime.Clock.System.now()
                    )
                    
                    Text(
                        mockChallenge.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        mockChallenge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Progress bar
                    LinearProgressIndicator(
                        progress = 0.6f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Progress: 3/5 completed")
                        Text("${mockChallenge.xpReward} XP reward")
                    }
                }
            }
        }
        
        // AI Insights Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "AI Insights",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Mock AI insights
                    val mockInsights = listOf(
                        "Your dosages have been consistent lately - great job!",
                        "Consider spacing experiences 1-2 weeks apart for better integration",
                        "You've earned 3 new achievements this week!"
                    )
                    
                    mockInsights.forEach { insight ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    insight,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Challenge History
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Challenge History",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Mock completed challenges
                    repeat(3) { index ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Research Scholar",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        "Completed 2 weeks ago",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("+200 XP")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class GamificationTab(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OVERVIEW("Overview", Icons.Filled.Dashboard),
    ACHIEVEMENTS("Achievements", Icons.Filled.EmojiEvents),
    QUESTS("Quests", Icons.Filled.School),
    CHALLENGES("Challenges", Icons.Filled.Timer),
    PROGRESS("Progress", Icons.Filled.Analytics)
}