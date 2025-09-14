package com.isaakhanimann.journal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.navigation.DesktopNavigationController
import com.isaakhanimann.journal.navigation.Screen
import com.isaakhanimann.journal.data.experience.ExperienceSummary
import com.isaakhanimann.journal.ui.compose.LocalConfiguration
import com.isaakhanimann.journal.ui.layout.calculateWindowSizeClass
import com.isaakhanimann.journal.ui.viewmodel.ExperiencesViewModel
import com.isaakhanimann.journal.ui.components.ExperienceCalendarView
import com.isaakhanimann.journal.ui.components.ExperienceTimelineView
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject

@Composable
fun ExperiencesScreen(navController: DesktopNavigationController) {
    val viewModel: ExperiencesViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val configuration = LocalConfiguration.current
    val responsiveConfig = calculateWindowSizeClass(
        width = configuration.screenWidthDp.dp,
        height = configuration.screenHeightDp.dp
    )
    
    var viewMode by remember { mutableStateOf(ExperienceViewMode.LIST) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        // Header with search and filters
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.navigateToDashboard() }
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Home")
                        }
                        Text(
                            text = "Experiences",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // View mode toggle
                        FilterChip(
                            onClick = { viewMode = ExperienceViewMode.LIST },
                            label = { Text("List") },
                            selected = viewMode == ExperienceViewMode.LIST,
                            leadingIcon = {
                                Icon(Icons.Default.ViewList, contentDescription = null)
                            }
                        )
                        
                        FilterChip(
                            onClick = { viewMode = ExperienceViewMode.CALENDAR },
                            label = { Text("Calendar") },
                            selected = viewMode == ExperienceViewMode.CALENDAR,
                            leadingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            }
                        )
                        
                        FilterChip(
                            onClick = { viewModel.toggleFavoritesFilter() },
                            label = { Text("Favorites") },
                            selected = showFavoritesOnly,
                            leadingIcon = if (showFavoritesOnly) {
                                { Icon(Icons.Default.Favorite, contentDescription = null) }
                            } else {
                                { Icon(Icons.Default.FavoriteBorder, contentDescription = null) }
                            }
                        )
                        
                        IconButton(
                            onClick = { /* Add more filter actions */ }
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "More Filters")
                        }
                        
                        FilledTonalButton(
                            onClick = { navController.navigate(Screen.ExperienceEditor()) }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("New")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search experiences...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
        
        // Content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(responsiveConfig.contentPadding),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error loading experiences: ${uiState.error}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val experiences = uiState.data ?: emptyList()
            if (experiences.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No experiences yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Create your first experience to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Button(
                            onClick = { navController.navigate(Screen.ExperienceEditor()) }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Experience")
                        }
                    }
                }
            } else {
                // Content based on view mode
                when (viewMode) {
                    ExperienceViewMode.LIST -> {
                        // Experience list
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(responsiveConfig.contentPadding),
                            verticalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing)
                        ) {
                            items(experiences) { experience ->
                                ExperienceListItem(
                                    experience = experience,
                                    onClick = { 
                                        navController.navigate(Screen.ExperienceTimeline(experience.experience.id.toString()))
                                    },
                                    onEdit = {
                                        navController.navigate(Screen.ExperienceEditor(experience.experience.id.toString()))
                                    }
                                )
                            }
                        }
                    }
                    
                    ExperienceViewMode.CALENDAR -> {
                        // Calendar view with timeline
                        Row(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Calendar on the left
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(responsiveConfig.contentPadding)
                            ) {
                                ExperienceCalendarView(
                                    experiences = experiences,
                                    selectedDate = selectedDate,
                                    onDateSelected = { date -> selectedDate = date },
                                    onMonthChanged = { /* Handle month change if needed */ }
                                )
                            }
                            
                            // Timeline on the right
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(responsiveConfig.contentPadding)
                            ) {
                                ExperienceTimelineView(
                                    experiences = experiences,
                                    selectedDate = selectedDate
                                )
                            }
                        }
                    }
                }
            }
                        )
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
    }
}

@Composable
private fun ExperienceListItem(
    experience: ExperienceSummary,
    onClick: () -> Unit,
    onEdit: () -> Unit
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
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (experience.experience.isFavorite) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(16.dp)
            )
        }
    }
}

enum class ExperienceViewMode {
    LIST, CALENDAR
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
            
            // Add substance count if available
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "View Timeline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}