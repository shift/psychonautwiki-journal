package com.isaakhanimann.journal.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.navigation.DesktopNavigationController
import com.isaakhanimann.journal.ui.compose.LocalConfiguration
import com.isaakhanimann.journal.ui.layout.calculateWindowSizeClass
import com.isaakhanimann.journal.ui.theme.ThemeMode
import com.isaakhanimann.journal.ui.viewmodel.SettingsViewModel
import com.isaakhanimann.journal.data.export.ExportManager
import com.isaakhanimann.journal.data.import.ImportManager
import com.isaakhanimann.journal.data.import.ImportFormat
import com.isaakhanimann.journal.ui.utils.FileDialogHandler
import kotlinx.coroutines.launch
import org.koin.compose.getKoin

@Composable
fun SettingsScreen(navController: DesktopNavigationController) {
    val configuration = LocalConfiguration.current
    val responsiveConfig = calculateWindowSizeClass(
        width = configuration.screenWidthDp.dp,
        height = configuration.screenHeightDp.dp
    )
    
    val koin = getKoin()
    val settingsViewModel = remember { koin.get<SettingsViewModel>() }
    val themeMode by settingsViewModel.themeMode.collectAsState()
    
    var enableNotifications by remember { mutableStateOf(true) }
    var dataRetention by remember { mutableStateOf("1 year") }
    var showAboutDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(responsiveConfig.contentPadding),
        verticalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.navigateToDashboard() }
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Home")
                        }
                        Column {
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Configure your journal preferences",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DataExportSection() {
    val koin = getKoin()
    val exportManager = remember { koin.get<ExportManager>() }
    val fileDialogHandler = remember { koin.get<FileDialogHandler>() }
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<String?>(null) }
    
    SettingsSection(title = "Data Export") {
        SettingItem(
            title = "Export to JSON",
            description = "Export all experiences and data in JSON format",
            icon = Icons.Default.DataObject,
            onClick = {
                scope.launch {
                    isExporting = true
                    try {
                        val filePath = fileDialogHandler.saveFile(
                            title = "Export to JSON",
                            defaultName = "experiences_export.json",
                            extension = "json"
                        )
                        
                        if (filePath != null) {
                            val success = exportManager.exportToJsonFile(filePath)
                            exportResult = if (success) {
                                "Successfully exported to $filePath"
                            } else {
                                "Failed to export data"
                            }
                        }
                    } catch (e: Exception) {
                        exportResult = "Export error: ${e.message}"
                    } finally {
                        isExporting = false
                    }
                }
            },
            trailing = {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        )
        
        SettingItem(
            title = "Export to CSV",
            description = "Export experiences in spreadsheet-compatible format",
            icon = Icons.Default.TableChart,
            onClick = {
                scope.launch {
                    isExporting = true
                    try {
                        val filePath = fileDialogHandler.saveFile(
                            title = "Export to CSV",
                            defaultName = "experiences_export.csv",
                            extension = "csv"
                        )
                        
                        if (filePath != null) {
                            val success = exportManager.exportToCsvFile(filePath)
                            exportResult = if (success) {
                                "Successfully exported to $filePath"
                            } else {
                                "Failed to export data"
                            }
                        }
                    } catch (e: Exception) {
                        exportResult = "Export error: ${e.message}"
                    } finally {
                        isExporting = false
                    }
                }
            },
            trailing = {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        )
    }
    
    // Show export result snackbar or dialog
    exportResult?.let { result ->
        LaunchedEffect(result) {
            // You could show a snackbar here or create a simple dialog
            // For now, we'll just clear the result after showing it
            kotlinx.coroutines.delay(3000)
            exportResult = null
        }
    }
}

@Composable
private fun DataImportSection() {
    val koin = getKoin()
    val importManager = remember { koin.get<ImportManager>() }
    val fileDialogHandler = remember { koin.get<FileDialogHandler>() }
    val scope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportFile by remember { mutableStateOf<String?>(null) }
    var pendingImportFormat by remember { mutableStateOf<ImportFormat?>(null) }
    
    SettingsSection(title = "Data Import") {
        SettingItem(
            title = "Import from JSON",
            description = "Import experiences from a JSON backup file",
            icon = Icons.Default.FileUpload,
            onClick = {
                scope.launch {
                    val filePath = fileDialogHandler.openFile(
                        title = "Import from JSON",
                        extension = "json"
                    )
                    
                    if (filePath != null) {
                        pendingImportFile = filePath
                        pendingImportFormat = ImportFormat.JSON
                        showConfirmDialog = true
                    }
                }
            },
            trailing = {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        )
        
        SettingItem(
            title = "Import from CSV",
            description = "Import experiences from a CSV spreadsheet file",
            icon = Icons.Default.TableView,
            onClick = {
                scope.launch {
                    val filePath = fileDialogHandler.openFile(
                        title = "Import from CSV",
                        extension = "csv"
                    )
                    
                    if (filePath != null) {
                        pendingImportFile = filePath
                        pendingImportFormat = ImportFormat.CSV
                        showConfirmDialog = true
                    }
                }
            },
            trailing = {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        )
    }
    
    // Import confirmation dialog
    if (showConfirmDialog && pendingImportFile != null && pendingImportFormat != null) {
        ImportConfirmationDialog(
            filePath = pendingImportFile!!,
            format = pendingImportFormat!!,
            onConfirm = {
                scope.launch {
                    isImporting = true
                    try {
                        val result = when (pendingImportFormat) {
                            ImportFormat.JSON -> importManager.importFromJsonFile(pendingImportFile!!)
                            ImportFormat.CSV -> importManager.importFromCsvFile(pendingImportFile!!)
                            else -> null
                        }
                        
                        importResult = when (result) {
                            is com.isaakhanimann.journal.data.import.ImportResult.Success -> {
                                "Successfully imported ${result.imported} experiences"
                            }
                            is com.isaakhanimann.journal.data.import.ImportResult.Error -> {
                                "Import failed: ${result.message}"
                            }
                            else -> "Import failed: Unknown error"
                        }
                    } catch (e: Exception) {
                        importResult = "Import error: ${e.message}"
                    } finally {
                        isImporting = false
                        showConfirmDialog = false
                        pendingImportFile = null
                        pendingImportFormat = null
                    }
                }
            },
            onCancel = {
                showConfirmDialog = false
                pendingImportFile = null
                pendingImportFormat = null
            }
        )
    }
    
    // Show import result
    importResult?.let { result ->
        LaunchedEffect(result) {
            kotlinx.coroutines.delay(3000)
            importResult = null
        }
    }
}

@Composable
private fun ImportConfirmationDialog(
    filePath: String,
    format: ImportFormat,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Confirm Import")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Are you sure you want to import data from this file?",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "File: ${filePath.substringAfterLast("/")}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Format: ${format.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "⚠️ This will add new experiences to your journal. Existing data will not be modified.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "About PsychonautWiki Journal",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "A safer way to track substance experiences",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Text(
                    text = "This application helps you document and analyze your substance experiences with safety and harm reduction in mind.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider()
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Built with:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("• Kotlin Multiplatform", style = MaterialTheme.typography.bodyMedium)
                    Text("• Compose Desktop", style = MaterialTheme.typography.bodyMedium)
                    Text("• Material 3 Design", style = MaterialTheme.typography.bodyMedium)
                    Text("• SQLDelight", style = MaterialTheme.typography.bodyMedium)
                }
                
                HorizontalDivider()
                
                Text(
                    text = "Data from PsychonautWiki.org",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "© 2024 PsychonautWiki Journal Contributors",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { 
                    // Open PsychonautWiki website - placeholder for future implementation
                }
            ) {
                Text("Visit PsychonautWiki")
            }
        }
    )
}
            }
        }
        
        // Appearance Section
        item {
            SettingsSection(title = "Appearance") {
                SettingItem(
                    title = "Theme",
                    description = "Choose your preferred theme",
                    icon = Icons.Default.DarkMode,
                    trailing = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.values().forEach { mode ->
                                FilterChip(
                                    onClick = { settingsViewModel.setThemeMode(mode) },
                                    label = { 
                                        Text(
                                            when (mode) {
                                                ThemeMode.SYSTEM -> "System"
                                                ThemeMode.LIGHT -> "Light"
                                                ThemeMode.DARK -> "Dark"
                                            }
                                        ) 
                                    },
                                    selected = themeMode == mode
                                )
                            }
                        }
                    }
                )
            }
        }
        
        // Data Export Section
        item {
            DataExportSection()
        }
        
        // Data Import Section
        item {
            DataImportSection()
        }
        
        // Privacy & Data Section
        item {
            SettingsSection(title = "Privacy & Data") {
                SettingItem(
                    title = "Data Retention",
                    description = "How long to keep your journal data",
                    icon = Icons.Default.Info,
                    trailing = {
                        AssistChip(
                            onClick = { /* Show picker */ },
                            label = { Text(dataRetention) }
                        )
                    }
                )
                
                HorizontalDivider()
                
                SettingItem(
                    title = "Export Data",
                    description = "Export your journal data as JSON",
                    icon = Icons.Default.Info,
                    onClick = { /* Handle export */ }
                )
                
                HorizontalDivider()
                
                SettingItem(
                    title = "Import Data",
                    description = "Import journal data from file",
                    icon = Icons.Default.Info,
                    onClick = { /* Handle import */ }
                )
            }
        }
        
        // Notifications Section
        item {
            SettingsSection(title = "Notifications") {
                SettingItem(
                    title = "Enable Notifications",
                    description = "Show reminders and alerts",
                    icon = Icons.Default.Notifications,
                    trailing = {
                        Switch(
                            checked = enableNotifications,
                            onCheckedChange = { enableNotifications = it }
                        )
                    }
                )
            }
        }
        
        // Safety Section
        item {
            SettingsSection(title = "Safety") {
                SettingItem(
                    title = "Harm Reduction Guidelines",
                    description = "View safety information and best practices",
                    icon = Icons.Default.Info,
                    onClick = { /* Show guidelines */ }
                )
                
                HorizontalDivider()
                
                SettingItem(
                    title = "Emergency Contacts",
                    description = "Manage emergency contact information",
                    icon = Icons.Default.Phone,
                    onClick = { /* Manage contacts */ }
                )
            }
        }
        
        // About Section
        item {
            SettingsSection(title = "About") {
                SettingItem(
                    title = "Version",
                    description = "PsychonautWiki Journal Desktop v1.0.0",
                    icon = Icons.Default.Info,
                    onClick = { showAboutDialog = true }
                )
                
                HorizontalDivider()
                
                SettingItem(
                    title = "Open Source Licenses",
                    description = "View third-party software licenses",
                    icon = Icons.Default.Info,
                    onClick = { /* Show licenses */ }
                )
                
                HorizontalDivider()
                
                SettingItem(
                    title = "Privacy Policy",
                    description = "Read our privacy policy",
                    icon = Icons.Default.Info,
                    onClick = { /* Show privacy policy */ }
                )
                
                HorizontalDivider()
                
                SettingItem(
                    title = "PsychonautWiki",
                    description = "Visit the PsychonautWiki website",
                    icon = Icons.Default.Info,
                    onClick = { /* Open website */ }
                )
            }
        }
        
        // Danger Zone
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Danger Zone",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    SettingItem(
                        title = "Clear All Data",
                        description = "Permanently delete all journal entries and settings",
                        icon = Icons.Default.Delete,
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = { /* Show confirmation dialog */ }
                    )
                }
            }
        }
    }
    
    // About Dialog
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            content()
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    description: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable { onClick() }
    } else {
        Modifier.fillMaxWidth()
    }
    
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (trailing != null) {
            Spacer(modifier = Modifier.width(16.dp))
            trailing()
        } else if (onClick != null) {
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}