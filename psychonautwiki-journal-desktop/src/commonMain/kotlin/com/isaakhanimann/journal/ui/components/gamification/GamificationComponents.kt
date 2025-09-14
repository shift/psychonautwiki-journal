package com.isaakhanimann.journal.ui.components.gamification

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.gamification.*

@Composable
fun LevelProgressCard(
    userLevel: UserLevel,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Level ${userLevel.currentLevel}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                LevelBadge(level = userLevel.currentLevel)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            XPProgressBar(
                currentXP = userLevel.currentXP,
                totalXP = userLevel.xpToNextLevel,
                modifier = Modifier.fillMaxWidth(),
                animated = animated
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${userLevel.currentXP} XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "${userLevel.xpToNextLevel} XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Total: ${userLevel.totalXP} XP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LevelBadge(
    level: Int,
    modifier: Modifier = Modifier
) {
    val badgeColor = when {
        level >= 25 -> Color(0xFFFFD700) // Gold
        level >= 10 -> Color(0xFFC0C0C0) // Silver
        level >= 5 -> Color(0xFFCD7F32)  // Bronze
        else -> MaterialTheme.colorScheme.primary
    }
    
    Surface(
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = badgeColor,
        shadowElevation = 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = level.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun XPProgressBar(
    currentXP: Long,
    totalXP: Long,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val progress = (currentXP.toFloat() / totalXP.toFloat()).coerceIn(0f, 1f)
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) progress else progress,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "xp_progress"
    )
    
    Column {
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = modifier.height(12.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun AchievementGrid(
    achievements: List<UserAchievement>,
    allAchievements: List<Achievement>,
    modifier: Modifier = Modifier,
    onAchievementClick: (Achievement) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val achievementsByCategory = allAchievements.groupBy { it.category }
        
        achievementsByCategory.forEach { (category, categoryAchievements) ->
            item {
                AchievementCategorySection(
                    category = category,
                    achievements = categoryAchievements,
                    unlockedAchievements = achievements,
                    onAchievementClick = onAchievementClick
                )
            }
        }
    }
}

@Composable
fun AchievementCategorySection(
    category: AchievementCategory,
    achievements: List<Achievement>,
    unlockedAchievements: List<UserAchievement>,
    onAchievementClick: (Achievement) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getCategoryIcon(category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = category.name.replace("_", " ").lowercase()
                        .split(" ").joinToString(" ") { word ->
                            word.replaceFirstChar { it.uppercase() }
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                val unlockedCount = achievements.count { achievement ->
                    unlockedAchievements.any { it.achievementId == achievement.id }
                }
                
                Text(
                    text = "$unlockedCount/${achievements.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(achievements) { achievement ->
                    val isUnlocked = unlockedAchievements.any { it.achievementId == achievement.id }
                    
                    AchievementBadge(
                        achievement = achievement,
                        isUnlocked = isUnlocked,
                        onClick = { onAchievementClick(achievement) }
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementBadge(
    achievement: Achievement,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val badgeColor = if (isUnlocked) {
        when (achievement.tier) {
            AchievementTier.BRONZE -> Color(0xFFCD7F32)
            AchievementTier.SILVER -> Color(0xFFC0C0C0)
            AchievementTier.GOLD -> Color(0xFFFFD700)
            AchievementTier.PLATINUM -> Color(0xFFE5E4E2)
        }
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    
    Card(
        modifier = modifier
            .size(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) badgeColor else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isUnlocked) 4.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getCategoryIcon(achievement.category),
                contentDescription = achievement.name,
                tint = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = achievement.tier.name.take(1),
                style = MaterialTheme.typography.bodySmall,
                color = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StreakDisplay(
    streaks: Map<StreakType, Streak>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = "Streaks",
                    tint = Color(0xFFFF6B35) // Fire color
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Current Streaks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            streaks.values.filter { it.isActive && it.currentCount > 0 }.forEach { streak ->
                StreakItem(
                    streak = streak,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (streaks.values.none { it.isActive && it.currentCount > 0 }) {
                Text(
                    text = "No active streaks. Start logging experiences to build your first streak!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StreakItem(
    streak: Streak,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getStreakIcon(streak.type),
            contentDescription = null,
            tint = if (streak.isActive) Color(0xFFFF6B35) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = streak.type.name.replace("_", " ").lowercase()
                .split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${streak.currentCount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (streak.isActive) Color(0xFFFF6B35) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            if (streak.bestCount > streak.currentCount) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(best: ${streak.bestCount})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun SafetyScoreCard(
    safetyScore: SafetyScore?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                safetyScore == null -> MaterialTheme.colorScheme.surfaceVariant
                safetyScore.overallScore >= 80 -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                safetyScore.overallScore >= 60 -> Color(0xFFFF9800).copy(alpha = 0.1f)
                else -> Color(0xFFF44336).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Safety Score",
                    tint = when {
                        safetyScore == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        safetyScore.overallScore >= 80 -> Color(0xFF4CAF50)
                        safetyScore.overallScore >= 60 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Safety Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (safetyScore != null) {
                    Text(
                        text = "${safetyScore.overallScore.toInt()}/100",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            safetyScore.overallScore >= 80 -> Color(0xFF4CAF50)
                            safetyScore.overallScore >= 60 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (safetyScore != null) {
                LinearProgressIndicator(
                    progress = (safetyScore.overallScore / 100.0).toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = when {
                        safetyScore.overallScore >= 80 -> Color(0xFF4CAF50)
                        safetyScore.overallScore >= 60 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (safetyScore.trend) {
                            ScoreTrend.IMPROVING -> Icons.Filled.TrendingUp
                            ScoreTrend.DECLINING -> Icons.Filled.TrendingDown
                            ScoreTrend.STABLE -> Icons.Filled.TrendingFlat
                            ScoreTrend.INSUFFICIENT_DATA -> Icons.Filled.Help
                        },
                        contentDescription = null,
                        tint = when (safetyScore.trend) {
                            ScoreTrend.IMPROVING -> Color(0xFF4CAF50)
                            ScoreTrend.DECLINING -> Color(0xFFF44336)
                            ScoreTrend.STABLE -> Color(0xFFFF9800)
                            ScoreTrend.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = when (safetyScore.trend) {
                            ScoreTrend.IMPROVING -> "Improving"
                            ScoreTrend.DECLINING -> "Declining"
                            ScoreTrend.STABLE -> "Stable"
                            ScoreTrend.INSUFFICIENT_DATA -> "Insufficient Data"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                Text(
                    text = "Log a few experiences with safety details to get your safety score!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun QuickStatsRow(
    stats: GamificationStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickStatCard(
            icon = Icons.Filled.EmojiEvents,
            label = "Achievements",
            value = "${stats.achievementsUnlocked}/${stats.totalAchievements}",
            modifier = Modifier.weight(1f)
        )
        
        QuickStatCard(
            icon = Icons.Filled.LocalFireDepartment,
            label = "Best Streak",
            value = "${stats.longestStreak} days",
            modifier = Modifier.weight(1f)
        )
        
        QuickStatCard(
            icon = Icons.Filled.School,
            label = "Quests Done",
            value = stats.questsCompleted.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper functions for icons
private fun getCategoryIcon(category: AchievementCategory): ImageVector {
    return when (category) {
        AchievementCategory.SAFETY_FIRST -> Icons.Filled.Security
        AchievementCategory.KNOWLEDGE_SEEKER -> Icons.Filled.School
        AchievementCategory.CONSISTENCY -> Icons.Filled.EventRepeat
        AchievementCategory.INTEGRATION -> Icons.Filled.Psychology
        AchievementCategory.COMMUNITY_CARE -> Icons.Filled.Group
        AchievementCategory.HARM_REDUCTION -> Icons.Filled.HealthAndSafety
        AchievementCategory.MILESTONE -> Icons.Filled.EmojiEvents
    }
}

private fun getStreakIcon(streakType: StreakType): ImageVector {
    return when (streakType) {
        StreakType.DAILY_LOGGING -> Icons.Filled.EditNote
        StreakType.INTEGRATION_PRACTICE -> Icons.Filled.Psychology
        StreakType.SAFETY_HABITS -> Icons.Filled.Security
        StreakType.LEARNING_STREAK -> Icons.Filled.School
        StreakType.APP_USAGE -> Icons.Filled.PhoneAndroid
    }
}