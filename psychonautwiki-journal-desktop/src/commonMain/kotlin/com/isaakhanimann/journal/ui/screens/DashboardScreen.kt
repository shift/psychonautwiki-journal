package com.isaakhanimann.journal.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.navigation.DesktopNavigationController
import com.isaakhanimann.journal.navigation.Screen
import com.isaakhanimann.journal.data.experience.ExperienceSummary
import com.isaakhanimann.journal.data.model.Experience
import com.isaakhanimann.journal.ui.compose.LocalConfiguration
import com.isaakhanimann.journal.ui.layout.calculateWindowSizeClass
import com.isaakhanimann.journal.ui.viewmodel.DashboardViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

@Composable
fun DashboardScreen(navController: DesktopNavigationController) {
    val viewModel: DashboardViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val responsiveConfig = calculateWindowSizeClass(
        width = configuration.screenWidthDp.dp,
        height = configuration.screenHeightDp.dp
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(responsiveConfig.contentPadding),
            verticalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing)
        ) {
            item {
                DashboardHeader(
                    onRefresh = { viewModel.refresh() },
                    isLoading = uiState.isLoading
                )
            }
            
            // Show loading or error states
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                item {
                    DashboardStats(
                        totalExperiences = uiState.data?.totalExperiences ?: 0,
                        totalSubstances = uiState.data?.totalSubstances ?: 0,
                        experiencesThisWeek = uiState.data?.experiencesThisWeek ?: 0
                    )
                }
                
                item {
                    DashboardQuickActions(
                        onNewExperience = { navController.navigate(Screen.ExperienceEditor()) },
                        onViewExperiences = { navController.navigate(Screen.Experiences) },
                        onViewSubstances = { navController.navigate(Screen.Substances) },
                        onViewAnalytics = { navController.navigate(Screen.Analytics) },
                        onOpenAIAssistant = { /* TODO: AI Assistant */ },
                        onOpenGamification = { navController.navigate(Screen.Gamification) }
                    )
                }
                
                item {
                    DashboardRecentExperiences(
                        recentExperiences = uiState.data?.recentExperiences ?: emptyList(),
                        onExperienceClick = { experience ->
                            navController.navigate(Screen.ExperienceEditor(experience.experience.id.toString()))
                        }
                    )
                }
            }
        }
        
        // Floating Action Button for quick experience creation
        FloatingActionButton(
            onClick = { navController.navigate(Screen.ExperienceEditor()) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "New Experience")
        }
    }
}

@Composable
fun DashboardHeader(
    onRefresh: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Track your substance experiences safely",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = onRefresh,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
    }
}

@Composable
fun DashboardStats(
    totalExperiences: Int,
    totalSubstances: Int,
    experiencesThisWeek: Int
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        item {
            StatCard(
                title = "Total Experiences", 
                value = totalExperiences.toString(),
                icon = Icons.Default.List
            )
        }
        item {
            StatCard(
                title = "Substances Tried", 
                value = totalSubstances.toString(),
                icon = Icons.Default.Star
            )
        }
        item {
            StatCard(
                title = "This Week", 
                value = experiencesThisWeek.toString(),
                icon = Icons.Default.DateRange
            )
        }
        item {
            StatCard(
                title = "Avg. Rating", 
                value = "4.2", // TODO: Calculate real average
                icon = Icons.Default.TrendingUp
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.width(140.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DashboardQuickActions(
    onNewExperience: () -> Unit,
    onViewExperiences: () -> Unit,
    onViewSubstances: () -> Unit,
    onViewAnalytics: () -> Unit,
    onOpenAIAssistant: () -> Unit,
    onOpenGamification: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNewExperience,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Experience")
                }
                
                OutlinedButton(
                    onClick = onViewExperiences,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.List, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View All")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewSubstances,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Science, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Substances")
                }
                
                OutlinedButton(
                    onClick = onViewAnalytics,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analytics")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenAIAssistant,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Assistant")
                }
                
                OutlinedButton(
                    onClick = onOpenGamification,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Progress")
                }
            }
        }
    }
}

@Composable
fun DashboardRecentExperiences(
    recentExperiences: List<ExperienceSummary>,
    onExperienceClick: (ExperienceSummary) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recent Experiences",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            if (recentExperiences.isEmpty()) {
                Text(
                    text = "No experiences yet. Create your first one!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp)
                )
            } else {
                recentExperiences.take(3).forEach { experience ->
                    ExperienceCard(
                        experience = experience,
                        onClick = { onExperienceClick(experience) }
                    )
                    if (experience != recentExperiences.take(3).last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ExperienceCard(
    experience: ExperienceSummary,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
                        text = experience.experience.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = experience.experience.sortDate.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (experience.experience.isFavorite) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            if (experience.experience.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = experience.experience.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}