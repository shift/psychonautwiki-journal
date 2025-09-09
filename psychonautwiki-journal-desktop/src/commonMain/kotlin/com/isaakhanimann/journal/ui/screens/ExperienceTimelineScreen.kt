package com.isaakhanimann.journal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.navigation.DesktopNavigationController
import com.isaakhanimann.journal.data.experience.TimelineEvent
import com.isaakhanimann.journal.data.model.*
import com.isaakhanimann.journal.ui.viewmodel.ExperienceTimelineViewModel
import com.isaakhanimann.journal.ui.viewmodel.ProcessedTimelineEvent
import com.isaakhanimann.journal.ui.viewmodel.TimelinePhase
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperienceTimelineScreen(
    navigationController: DesktopNavigationController,
    experienceId: Int
) {
    val viewModel: ExperienceTimelineViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val ratingFormState by viewModel.ratingFormState.collectAsState()
    val noteFormState by viewModel.noteFormState.collectAsState()
    
    // Load timeline data
    LaunchedEffect(experienceId) {
        viewModel.loadTimeline(experienceId)
    }
    
    // Clear messages after a delay
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }
    
    Scaffold(
        topBar = {
            TimelineTopBar(
                title = uiState.experienceWithDetails?.experience?.title ?: "Timeline",
                onNavigateBack = { navigationController.navigateToExperiences() },
                onAddRating = { viewModel.showRatingDialog() },
                onAddNote = { viewModel.showNoteDialog() },
                onAddIngestion = { 
                    navigationController.navigateToIngestionEditor(experienceId)
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() }
            ) {
                uiState.successMessage?.let { message ->
                    Snackbar(
                        action = {
                            TextButton(onClick = { viewModel.clearMessages() }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(message)
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                val error = uiState.error
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            TextButton(onClick = { viewModel.clearMessages() }) {
                                Text("Dismiss")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { viewModel.loadTimeline(experienceId) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
            
            else -> {
                TimelineContent(
                    uiState = uiState,
                    onEditExperience = { 
                        navigationController.navigateToExperienceEditor(experienceId)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        
        // Rating Dialog
        if (uiState.showRatingDialog) {
            RatingDialog(
                formState = ratingFormState,
                onDismiss = { viewModel.hideRatingDialog() },
                onConfirm = { viewModel.addRating() },
                onSelectOption = { viewModel.selectRatingOption(it) }
            )
        }
        
        // Note Dialog
        if (uiState.showNoteDialog) {
            NoteDialog(
                formState = noteFormState,
                onDismiss = { viewModel.hideNoteDialog() },
                onConfirm = { viewModel.addNote() },
                onTextChange = { viewModel.updateNoteText(it) },
                onColorChange = { viewModel.updateNoteColor(it) },
                onToggleTimeline = { viewModel.toggleNoteInTimeline() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    onAddRating: () -> Unit,
    onAddNote: () -> Unit,
    onAddIngestion: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add"
                )
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Add Rating") },
                    onClick = {
                        showMenu = false
                        onAddRating()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Star, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add Note") },
                    onClick = {
                        showMenu = false
                        onAddNote()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add Ingestion") },
                    onClick = {
                        showMenu = false
                        onAddIngestion()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Warning, contentDescription = null)
                    }
                )
            }
        }
    )
}

@Composable
private fun TimelineContent(
    uiState: com.isaakhanimann.journal.ui.viewmodel.TimelineUiState,
    onEditExperience: () -> Unit,
    modifier: Modifier = Modifier
) {
    val experienceWithDetails = uiState.experienceWithDetails
    val timelineData = uiState.timelineData
    
    if (experienceWithDetails == null) {
        EmptyTimelineState()
        return
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Experience Header
        item {
            ExperienceHeaderCard(
                experience = experienceWithDetails.experience,
                onEdit = onEditExperience
            )
        }
        
        // Timeline Overview
        if (timelineData != null && timelineData.hasEvents) {
            item {
                TimelineOverviewCard(timelineData = timelineData)
            }
            
            // Timeline Phases
            items(timelineData.phases.filter { it.hasEvents }) { phase ->
                TimelinePhaseCard(phase = phase)
            }
            
            // All Events (chronological)
            item {
                AllEventsCard(events = timelineData.events)
            }
        } else {
            item {
                EmptyTimelineCard()
            }
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ExperienceHeaderCard(
    experience: Experience,
    onEdit: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = experience.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                
                if (experience.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit experience"
                    )
                }
            }
            
            if (experience.text.isNotBlank()) {
                Text(
                    text = experience.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = experience.creationDate.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (experience.location != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = experience.location.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineOverviewCard(
    timelineData: com.isaakhanimann.journal.ui.viewmodel.TimelineData
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Timeline Overview",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimelineStatItem(
                    label = "Total Duration",
                    value = timelineData.formattedDuration,
                    icon = Icons.Default.Info
                )
                
                TimelineStatItem(
                    label = "Events",
                    value = timelineData.events.size.toString(),
                    icon = Icons.Default.List
                )
                
                TimelineStatItem(
                    label = "Active Phases",
                    value = timelineData.phases.count { it.hasEvents }.toString(),
                    icon = Icons.Default.List
                )
            }
        }
    }
}

@Composable
private fun TimelineStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimelinePhaseCard(phase: TimelinePhase) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = phase.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${phase.eventCount} events",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            phase.events.forEach { event ->
                TimelineEventItem(event = event)
            }
        }
    }
}

@Composable
private fun AllEventsCard(events: List<ProcessedTimelineEvent>) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Complete Timeline",
                style = MaterialTheme.typography.titleMedium
            )
            
            events.forEach { event ->
                TimelineEventItem(
                    event = event,
                    showTime = true
                )
            }
        }
    }
}

@Composable
private fun TimelineEventItem(
    event: ProcessedTimelineEvent,
    showTime: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Event type indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(getEventColor(event.event)),
            contentAlignment = Alignment.Center
        ) {
            // Empty content - just a colored indicator
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getEventDescription(event.event),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                
                if (showTime) {
                    Text(
                        text = event.formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            getEventDetails(event.event)?.let { details ->
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getEventColor(event: TimelineEvent): Color {
    return when (event) {
        is TimelineEvent.IngestionEvent -> MaterialTheme.colorScheme.primary
        is TimelineEvent.IngestionEndEvent -> MaterialTheme.colorScheme.secondary
        is TimelineEvent.RatingEvent -> MaterialTheme.colorScheme.tertiary
        is TimelineEvent.NoteEvent -> getAdaptiveColor(event.note.color)
    }
}

@Composable
private fun getAdaptiveColor(color: AdaptiveColor): Color {
    return when (color) {
        AdaptiveColor.RED -> MaterialTheme.colorScheme.error
        AdaptiveColor.ORANGE -> MaterialTheme.colorScheme.error
        AdaptiveColor.YELLOW -> MaterialTheme.colorScheme.tertiary
        AdaptiveColor.GREEN -> MaterialTheme.colorScheme.tertiary
        AdaptiveColor.MINT -> MaterialTheme.colorScheme.tertiary
        AdaptiveColor.TEAL -> MaterialTheme.colorScheme.primary
        AdaptiveColor.CYAN -> MaterialTheme.colorScheme.primary
        AdaptiveColor.BLUE -> MaterialTheme.colorScheme.primary
        AdaptiveColor.INDIGO -> MaterialTheme.colorScheme.secondary
        AdaptiveColor.PURPLE -> MaterialTheme.colorScheme.secondary
        AdaptiveColor.PINK -> MaterialTheme.colorScheme.primary
        AdaptiveColor.BROWN -> MaterialTheme.colorScheme.outline
        AdaptiveColor.WHITE -> MaterialTheme.colorScheme.surface
        AdaptiveColor.GRAY -> MaterialTheme.colorScheme.onSurfaceVariant
        AdaptiveColor.BLACK -> MaterialTheme.colorScheme.onSurface
        AdaptiveColor.BLACK -> MaterialTheme.colorScheme.onSurface
    }
}

private fun getEventDescription(event: TimelineEvent): String {
    return when (event) {
        is TimelineEvent.IngestionEvent -> "${event.ingestion.substanceName} (${event.ingestion.administrationRoute.displayName})"
        is TimelineEvent.IngestionEndEvent -> "End of ${event.ingestion.substanceName}"
        is TimelineEvent.RatingEvent -> "Rating: ${event.rating.option.displayName}"
        is TimelineEvent.NoteEvent -> event.note.note
    }
}

private fun getEventDetails(event: TimelineEvent): String? {
    return when (event) {
        is TimelineEvent.IngestionEvent -> {
            val dose = event.ingestion.dose?.let { "${it} ${event.ingestion.units ?: ""}" }
            val estimate = if (event.ingestion.isDoseAnEstimate) " (estimated)" else ""
            dose?.let { "$it$estimate" }
        }
        is TimelineEvent.RatingEvent -> event.rating.option.description
        else -> null
    }
}

@Composable
private fun EmptyTimelineCard() {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No timeline events yet",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Add ingestions, ratings, and notes to build your experience timeline.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyTimelineState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Experience not found",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RatingDialog(
    formState: com.isaakhanimann.journal.ui.viewmodel.RatingFormState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onSelectOption: (ShulginRatingOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Rating") },
        text = {
            Column {
                Text("Select your current intensity level:")
                Spacer(modifier = Modifier.height(16.dp))
                
                ShulginRatingOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = formState.selectedOption == option,
                            onClick = { onSelectOption(option) }
                        )
                        Column {
                            Text(
                                text = "${option.displayName} - ${option.description}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = formState.isValid
            ) {
                Text("Add Rating")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NoteDialog(
    formState: com.isaakhanimann.journal.ui.viewmodel.NoteFormState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onTextChange: (String) -> Unit,
    onColorChange: (AdaptiveColor) -> Unit,
    onToggleTimeline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Note") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = formState.text,
                    onValueChange = onTextChange,
                    label = { Text("Note") },
                    placeholder = { Text("What's happening...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Include in timeline")
                    Switch(
                        checked = formState.isPartOfTimeline,
                        onCheckedChange = { onToggleTimeline() }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = formState.isValid
            ) {
                Text("Add Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ErrorContent(
    error: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Dismiss")
                    }
                    
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}