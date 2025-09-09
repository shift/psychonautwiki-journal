package com.isaakhanimann.journal.ui.screens

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.navigation.DesktopNavigationController
import com.isaakhanimann.journal.data.substance.SubstanceInfo
import com.isaakhanimann.journal.ui.viewmodel.SubstancesViewModel
import org.koin.compose.koinInject

@Composable
fun SubstancesScreen(navController: DesktopNavigationController) {
    val viewModel: SubstancesViewModel = koinInject()
    
    val substances by viewModel.filteredSubstances
    val isLoading by viewModel.isLoading
    val searchQuery by viewModel.searchQuery
    val selectedCategory by viewModel.selectedCategory
    val categories by viewModel.categories
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Substance Database",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading substance database...")
                }
            }
        } else {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Search substances...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            // Category filter
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        onClick = { viewModel.updateSelectedCategory(category) },
                        label = { Text(category) },
                        selected = category == selectedCategory
                    )
                }
            }
            
            // Results count
            Text(
                text = "${substances.size} substances found",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Substance list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(substances) { substance ->
                    SubstanceListItem(
                        substance = substance,
                        onClick = { /* TODO: Navigate to substance detail */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubstanceListItem(
    substance: SubstanceInfo,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on primary category - using only basic icons
            val primaryCategory = substance.categories.firstOrNull() ?: "unknown"
            val icon = when (primaryCategory.lowercase()) {
                "psychedelic" -> Icons.Default.Star
                "stimulant" -> Icons.Default.PlayArrow
                "depressant" -> Icons.Default.KeyboardArrowDown
                "entactogen" -> Icons.Default.Favorite
                "dissociative" -> Icons.Default.MoreVert
                else -> Icons.Default.Info
            }
            
            Icon(
                icon,
                contentDescription = primaryCategory,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = substance.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (substance.categories.isNotEmpty()) {
                    Text(
                        text = substance.categories.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                substance.summary?.let { summary ->
                    if (summary.isNotBlank()) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Show bioavailability info if available
                val bioavailabilityInfo = substance.roas
                    .filter { it.bioavailability != null }
                    .take(2) // Show max 2 routes to avoid clutter
                    .map { roa ->
                        val bioav = roa.bioavailability!!
                        val range = when {
                            bioav.min != null && bioav.max != null -> "${bioav.min?.toInt()}-${bioav.max?.toInt()}%"
                            bioav.max != null -> "up to ${bioav.max?.toInt()}%"
                            bioav.min != null -> "from ${bioav.min?.toInt()}%"
                            else -> "unknown"
                        }
                        "${roa.name}: $range"
                    }
                
                if (bioavailabilityInfo.isNotEmpty()) {
                    Text(
                        text = "Bioavailability: ${bioavailabilityInfo.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Show common names if available
                if (substance.commonNames.isNotEmpty()) {
                    Text(
                        text = "Also known as: ${substance.commonNames.take(3).joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}