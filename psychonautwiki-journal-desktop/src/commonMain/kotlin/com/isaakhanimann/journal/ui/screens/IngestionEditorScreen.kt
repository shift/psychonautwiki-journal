package com.isaakhanimann.journal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.navigation.DesktopNavigationController
import com.isaakhanimann.journal.data.model.AdministrationRoute
import com.isaakhanimann.journal.data.model.StomachFullness
import com.isaakhanimann.journal.data.substance.SubstanceInfo
import com.isaakhanimann.journal.ui.viewmodel.IngestionEditorViewModel
import com.isaakhanimann.journal.ui.viewmodel.IngestionEditorMode
import com.isaakhanimann.journal.ui.viewmodel.DoseClassification
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngestionEditorScreen(
    navigationController: DesktopNavigationController,
    experienceId: Int,
    ingestionId: Int? = null
) {
    val viewModel: IngestionEditorViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val substanceSearchQuery by viewModel.substanceSearchQuery.collectAsState()
    
    // Initialize the view model
    LaunchedEffect(experienceId, ingestionId) {
        if (ingestionId != null) {
            viewModel.initializeForEditing(experienceId, ingestionId)
        } else {
            viewModel.initializeForCreation(experienceId)
        }
    }
    
    // Handle save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.clearSaveSuccess()
            navigationController.navigateToExperienceTimeline(experienceId)
        }
    }
    
    Scaffold(
        topBar = {
            IngestionEditorTopBar(
                mode = uiState.mode,
                onNavigateBack = { navigationController.navigateToExperienceTimeline(experienceId) },
                onSave = { viewModel.saveIngestion() },
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
                val errorMessage = uiState.error
                if (errorMessage != null) {
                    ErrorContent(
                        error = errorMessage,
                        onDismiss = { viewModel.clearError() },
                        onRetry = {
                            if (ingestionId != null) {
                                viewModel.initializeForEditing(experienceId, ingestionId)
                            } else {
                                viewModel.initializeForCreation(experienceId)
                            }
                        }
                    )
                }
            }
            
            else -> {
                IngestionEditorContent(
                    formState = formState,
                    uiState = uiState,
                    substanceSearchQuery = substanceSearchQuery,
                    onSubstanceSearchQueryChange = viewModel::updateSubstanceSearchQuery,
                    onSelectSubstance = viewModel::selectSubstance,
                    onDoseChange = viewModel::updateDose,
                    onUnitsChange = viewModel::updateUnits,
                    onRouteChange = viewModel::updateAdministrationRoute,
                    onTimeChange = viewModel::updateTime,
                    onEndTimeChange = viewModel::updateEndTime,
                    onNotesChange = viewModel::updateNotes,
                    onToggleDoseEstimate = viewModel::toggleDoseEstimate,
                    onStomachFullnessChange = viewModel::updateStomachFullness,
                    onConsumerNameChange = viewModel::updateConsumerName,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngestionEditorTopBar(
    mode: IngestionEditorMode,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean
) {
    TopAppBar(
        title = {
            Text(
                text = when (mode) {
                    IngestionEditorMode.CREATE -> "New Ingestion"
                    IngestionEditorMode.EDIT -> "Edit Ingestion"
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
private fun IngestionEditorContent(
    formState: com.isaakhanimann.journal.ui.viewmodel.IngestionFormState,
    uiState: com.isaakhanimann.journal.ui.viewmodel.IngestionEditorUiState,
    substanceSearchQuery: String,
    onSubstanceSearchQueryChange: (String) -> Unit,
    onSelectSubstance: (String) -> Unit,
    onDoseChange: (String) -> Unit,
    onUnitsChange: (String) -> Unit,
    onRouteChange: (AdministrationRoute) -> Unit,
    onTimeChange: (kotlinx.datetime.Instant) -> Unit,
    onEndTimeChange: (kotlinx.datetime.Instant?) -> Unit,
    onNotesChange: (String) -> Unit,
    onToggleDoseEstimate: () -> Unit,
    onStomachFullnessChange: (StomachFullness?) -> Unit,
    onConsumerNameChange: (String) -> Unit,
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
        // Substance Selection
        SubstanceSelectionSection(
            formState = formState,
            substanceSearchQuery = substanceSearchQuery,
            substanceSearchResults = uiState.substanceSearchResults,
            onSubstanceSearchQueryChange = onSubstanceSearchQueryChange,
            onSelectSubstance = onSelectSubstance
        )
        
        // Dose and Route Section
        DoseRouteSection(
            formState = formState,
            uiState = uiState,
            onDoseChange = onDoseChange,
            onUnitsChange = onUnitsChange,
            onRouteChange = onRouteChange,
            onToggleDoseEstimate = onToggleDoseEstimate
        )
        
        // Timing Section
        TimingSection(
            formState = formState,
            onTimeChange = onTimeChange,
            onEndTimeChange = onEndTimeChange
        )
        
        // Additional Information
        AdditionalInfoSection(
            formState = formState,
            onNotesChange = onNotesChange,
            onStomachFullnessChange = onStomachFullnessChange,
            onConsumerNameChange = onConsumerNameChange
        )
        
        // Safety Information
        if (uiState.selectedSubstanceInfo != null) {
            SafetyInfoSection(
                substanceInfo = uiState.selectedSubstanceInfo,
                doseClassification = uiState.doseClassification
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SubstanceSelectionSection(
    formState: com.isaakhanimann.journal.ui.viewmodel.IngestionFormState,
    substanceSearchQuery: String,
    substanceSearchResults: List<SubstanceInfo>,
    onSubstanceSearchQueryChange: (String) -> Unit,
    onSelectSubstance: (String) -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }
    
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Substance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            ExposedDropdownMenuBox(
                expanded = showDropdown,
                onExpandedChange = { showDropdown = it }
            ) {
                OutlinedTextField(
                    value = if (formState.substanceName.isNotBlank()) formState.substanceName else substanceSearchQuery,
                    onValueChange = onSubstanceSearchQueryChange,
                    label = { Text("Search substance *") },
                    isError = formState.substanceNameError != null,
                    supportingText = formState.substanceNameError?.let { { Text(it) } },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                
                ExposedDropdownMenu(
                    expanded = showDropdown && substanceSearchResults.isNotEmpty(),
                    onDismissRequest = { showDropdown = false }
                ) {
                    substanceSearchResults.take(10).forEach { substance ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = substance.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (substance.commonNames.isNotEmpty()) {
                                        Text(
                                            text = substance.commonNames.joinToString(", "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onSelectSubstance(substance.name)
                                showDropdown = false
                            }
                        )
                    }
                }
            }
            
            if (formState.substanceName.isNotBlank()) {
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
                        text = "Selected: ${formState.substanceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DoseRouteSection(
    formState: com.isaakhanimann.journal.ui.viewmodel.IngestionFormState,
    uiState: com.isaakhanimann.journal.ui.viewmodel.IngestionEditorUiState,
    onDoseChange: (String) -> Unit,
    onUnitsChange: (String) -> Unit,
    onRouteChange: (AdministrationRoute) -> Unit,
    onToggleDoseEstimate: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Dose & Administration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = formState.dose,
                    onValueChange = onDoseChange,
                    label = { Text("Dose *") },
                    isError = formState.doseError != null,
                    supportingText = formState.doseError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = formState.units,
                    onValueChange = onUnitsChange,
                    label = { Text("Units") },
                    placeholder = { Text("mg, g, ml...") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Administration Route Selector
            var showRouteMenu by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = showRouteMenu,
                onExpandedChange = { showRouteMenu = it }
            ) {
                OutlinedTextField(
                    value = formState.administrationRoute.displayName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Administration Route") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRouteMenu)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                
                ExposedDropdownMenu(
                    expanded = showRouteMenu,
                    onDismissRequest = { showRouteMenu = false }
                ) {
                    AdministrationRoute.entries.forEach { route ->
                        DropdownMenuItem(
                            text = { Text(route.displayName) },
                            onClick = {
                                onRouteChange(route)
                                showRouteMenu = false
                            }
                        )
                    }
                }
            }
            
            // Dose Classification Display
            uiState.doseClassification?.let { classification ->
                DoseClassificationCard(classification)
            }
            
            // Dose Estimate Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dose is an estimate",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = formState.isDoseAnEstimate,
                    onCheckedChange = { onToggleDoseEstimate() }
                )
            }
        }
    }
}

@Composable
private fun DoseClassificationCard(classification: DoseClassification) {
    val containerColor = when (classification) {
        DoseClassification.THRESHOLD -> MaterialTheme.colorScheme.surfaceVariant
        DoseClassification.LIGHT -> MaterialTheme.colorScheme.primaryContainer
        DoseClassification.COMMON -> MaterialTheme.colorScheme.secondaryContainer
        DoseClassification.STRONG -> MaterialTheme.colorScheme.tertiaryContainer
        DoseClassification.HEAVY -> MaterialTheme.colorScheme.errorContainer
        DoseClassification.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when (classification) {
        DoseClassification.THRESHOLD -> MaterialTheme.colorScheme.onSurfaceVariant
        DoseClassification.LIGHT -> MaterialTheme.colorScheme.onPrimaryContainer
        DoseClassification.COMMON -> MaterialTheme.colorScheme.onSecondaryContainer
        DoseClassification.STRONG -> MaterialTheme.colorScheme.onTertiaryContainer
        DoseClassification.HEAVY -> MaterialTheme.colorScheme.onErrorContainer
        DoseClassification.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    classification.isRisky -> Icons.Default.Warning
                    classification.needsCaution -> Icons.Default.Info
                    else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "${classification.displayName} Dose",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
                Text(
                    text = classification.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun TimingSection(
    formState: com.isaakhanimann.journal.ui.viewmodel.IngestionFormState,
    onTimeChange: (kotlinx.datetime.Instant) -> Unit,
    onEndTimeChange: (kotlinx.datetime.Instant?) -> Unit
) {
    var showEndTime by remember { mutableStateOf(formState.hasEndTime) }
    val localDateTime = formState.time.toLocalDateTime(TimeZone.currentSystemDefault())
    
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Timing",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            OutlinedTextField(
                value = "${localDateTime.date} ${localDateTime.time}",
                onValueChange = { },
                label = { Text("Ingestion Time") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { /* TODO: Date/time picker */ }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Pick time"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add end time",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = showEndTime,
                    onCheckedChange = { 
                        showEndTime = it
                        if (!it) onEndTimeChange(null)
                    }
                )
            }
            
            if (showEndTime) {
                val endLocalDateTime = formState.endTime?.toLocalDateTime(TimeZone.currentSystemDefault())
                
                OutlinedTextField(
                    value = endLocalDateTime?.let { "${it.date} ${it.time}" } ?: "",
                    onValueChange = { },
                    label = { Text("End Time") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { /* TODO: Date/time picker */ }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Pick end time"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AdditionalInfoSection(
    formState: com.isaakhanimann.journal.ui.viewmodel.IngestionFormState,
    onNotesChange: (String) -> Unit,
    onStomachFullnessChange: (StomachFullness?) -> Unit,
    onConsumerNameChange: (String) -> Unit
) {
    var showStomachFullness by remember { mutableStateOf(formState.stomachFullness != null) }
    
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Additional Information",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            OutlinedTextField(
                value = formState.notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                placeholder = { Text("Additional details about this ingestion...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            
            OutlinedTextField(
                value = formState.consumerName,
                onValueChange = onConsumerNameChange,
                label = { Text("Consumer Name") },
                placeholder = { Text("Optional: who consumed this") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Track stomach fullness",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = showStomachFullness,
                    onCheckedChange = { 
                        showStomachFullness = it
                        if (!it) onStomachFullnessChange(null)
                    }
                )
            }
            
            if (showStomachFullness) {
                var showFullnessMenu by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = showFullnessMenu,
                    onExpandedChange = { showFullnessMenu = it }
                ) {
                    OutlinedTextField(
                        value = formState.stomachFullness?.displayName ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Stomach Fullness") },
                        placeholder = { Text("Select...") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFullnessMenu)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showFullnessMenu,
                        onDismissRequest = { showFullnessMenu = false }
                    ) {
                        StomachFullness.entries.forEach { fullness ->
                            DropdownMenuItem(
                                text = { Text(fullness.displayName) },
                                onClick = {
                                    onStomachFullnessChange(fullness)
                                    showFullnessMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SafetyInfoSection(
    substanceInfo: SubstanceInfo,
    doseClassification: DoseClassification?
) {
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
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Safety Information",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (doseClassification?.needsCaution == true) {
                Text(
                    text = "⚠️ This is a ${doseClassification.displayName.lowercase()} dose. Please exercise caution.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            substanceInfo.summary?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (substanceInfo.categories.isNotEmpty()) {
                Text(
                    text = "Categories: ${substanceInfo.categories.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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