package com.isaakhanimann.journal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.navigation.DesktopNavigationController
import com.isaakhanimann.journal.ui.viewmodel.ExperienceEditorViewModel
import com.isaakhanimann.journal.ui.viewmodel.EditorMode
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperienceEditorScreen(
    navigationController: DesktopNavigationController,
    experienceId: Int? = null
) {
    val viewModel: ExperienceEditorViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    
    // Initialize the view model based on whether we're editing or creating
    LaunchedEffect(experienceId) {
        if (experienceId != null) {
            viewModel.initializeForEditing(experienceId)
        } else {
            viewModel.initializeForCreation()
        }
    }
    
    // Handle save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.clearSaveSuccess()
            navigationController.navigateToExperiences()
        }
    }
    
    Scaffold(
        topBar = {
            ExperienceEditorTopBar(
                mode = uiState.mode,
                onNavigateBack = { navigationController.navigateToExperiences() },
                onSave = { viewModel.saveExperience() },
                canSave = formState.isValid && uiState.canSave,
                isSaving = uiState.isSaving
            )
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
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = {
                                if (experienceId != null) {
                                    viewModel.initializeForEditing(experienceId)
                                } else {
                                    viewModel.initializeForCreation()
                                }
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
            
            else -> {
                ExperienceEditorContent(
                    formState = formState,
                    onTitleChange = viewModel::updateTitle,
                    onTextChange = viewModel::updateText,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onLocationNameChange = viewModel::updateLocationName,
                    onLocationLatitudeChange = viewModel::updateLocationLatitude,
                    onLocationLongitudeChange = viewModel::updateLocationLongitude,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperienceEditorTopBar(
    mode: EditorMode,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean
) {
    TopAppBar(
        title = {
            Text(
                text = when (mode) {
                    EditorMode.CREATE -> "New Experience"
                    EditorMode.EDIT -> "Edit Experience"
                }
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save")
                }
            }
        }
    )
}

@Composable
private fun ExperienceEditorContent(
    formState: com.isaakhanimann.journal.ui.viewmodel.ExperienceFormState,
    onTitleChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onLocationNameChange: (String) -> Unit,
    onLocationLatitudeChange: (String) -> Unit,
    onLocationLongitudeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Section
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Experience Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    IconToggleButton(
                        checked = formState.isFavorite,
                        onCheckedChange = { onToggleFavorite() }
                    ) {
                        Icon(
                            imageVector = if (formState.isFavorite) Icons.Filled.Star else Icons.Filled.Add,
                            contentDescription = "Toggle favorite",
                            tint = if (formState.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                OutlinedTextField(
                    value = formState.title,
                    onValueChange = onTitleChange,
                    label = { Text("Title *") },
                    isError = formState.titleError != null,
                    supportingText = formState.titleError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = formState.text,
                    onValueChange = onTextChange,
                    label = { Text("Description") },
                    placeholder = { Text("Describe your experience...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8
                )
            }
        }
        
        // Date and Time Section
        DateTimeSection(
            sortDate = formState.sortDate
        )
        
        // Location Section
        LocationSection(
            formState = formState,
            onLocationNameChange = onLocationNameChange,
            onLocationLatitudeChange = onLocationLatitudeChange,
            onLocationLongitudeChange = onLocationLongitudeChange
        )
        
        // Help Section
        HelpSection()
        
        // Bottom spacing for better scrolling
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DateTimeSection(
    sortDate: kotlinx.datetime.Instant
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val localDateTime = sortDate.toLocalDateTime(TimeZone.currentSystemDefault())
    
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Date & Time",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            OutlinedTextField(
                value = "${localDateTime.date} ${localDateTime.time}",
                onValueChange = { },
                label = { Text("Experience Date") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Pick date"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = "The date when this experience occurred. This helps organize your experiences chronologically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    // Note: Date picker implementation would go here
    // For now, this is a placeholder as date pickers require platform-specific implementation
}

@Composable
private fun LocationSection(
    formState: com.isaakhanimann.journal.ui.viewmodel.ExperienceFormState,
    onLocationNameChange: (String) -> Unit,
    onLocationLatitudeChange: (String) -> Unit,
    onLocationLongitudeChange: (String) -> Unit
) {
    var showCoordinates by remember { mutableStateOf(false) }
    
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                TextButton(
                    onClick = { showCoordinates = !showCoordinates }
                ) {
                    Icon(
                        imageVector = if (showCoordinates) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Coordinates")
                }
            }
            
            OutlinedTextField(
                value = formState.locationName,
                onValueChange = onLocationNameChange,
                label = { Text("Location Name") },
                placeholder = { Text("e.g., Home, Park, Festival...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            if (showCoordinates) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = formState.locationLatitude,
                        onValueChange = onLocationLatitudeChange,
                        label = { Text("Latitude") },
                        placeholder = { Text("0.000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = formState.locationLongitude,
                        onValueChange = onLocationLongitudeChange,
                        label = { Text("Longitude") },
                        placeholder = { Text("0.000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                if (formState.hasValidCoordinates) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Valid coordinates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Text(
                text = "Optional location information for this experience. You can add just a name or include precise coordinates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HelpSection() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Getting Started",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "Start by giving your experience a descriptive title. After saving, you can add substances, dosages, and timeline notes to build a complete record of your journey.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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