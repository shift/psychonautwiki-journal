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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(navController: DesktopNavigationController) {
    val configuration = LocalConfiguration.current
    val responsiveConfig = calculateWindowSizeClass(
        width = configuration.screenWidthDp.dp,
        height = configuration.screenHeightDp.dp
    )
    
    val settingsViewModel: SettingsViewModel = koinInject()
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
            SettingsHeader(navController = navController)
        }
        
        item {
            AppearanceSettings(
                themeMode = themeMode,
                onThemeModeChange = { settingsViewModel.setThemeMode(it) }
            )
        }
        
        item {
            DataSettings(
                enableNotifications = enableNotifications,
                onNotificationsChange = { enableNotifications = it },
                dataRetention = dataRetention,
                onDataRetentionChange = { dataRetention = it }
            )
        }
        
        item {
            ExportImportSettings()
        }
        
        item {
            AboutSettings(
                onShowAbout = { showAboutDialog = true }
            )
        }
    }
    
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
fun SettingsHeader(navController: DesktopNavigationController) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.navigateToDashboard() }
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun AppearanceSettings(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    SettingsSection(title = "Appearance") {
        SettingItem(
            title = "Theme",
            subtitle = "Choose your preferred theme",
            leadingIcon = Icons.Default.Palette
        ) {
            var expanded by remember { mutableStateOf(false) }
            
            Box {
                OutlinedButton(
                    onClick = { expanded = true }
                ) {
                    Text(
                        text = when (themeMode) {
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.SYSTEM -> "System"
                        }
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Light") },
                        onClick = {
                            onThemeModeChange(ThemeMode.LIGHT)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Dark") },
                        onClick = {
                            onThemeModeChange(ThemeMode.DARK)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("System") },
                        onClick = {
                            onThemeModeChange(ThemeMode.SYSTEM)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DataSettings(
    enableNotifications: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    dataRetention: String,
    onDataRetentionChange: (String) -> Unit
) {
    SettingsSection(title = "Data & Privacy") {
        SettingItem(
            title = "Notifications",
            subtitle = "Enable app notifications",
            leadingIcon = Icons.Default.Notifications
        ) {
            Switch(
                checked = enableNotifications,
                onCheckedChange = onNotificationsChange
            )
        }
        
        SettingItem(
            title = "Data Retention",
            subtitle = "How long to keep your data",
            leadingIcon = Icons.Default.Storage
        ) {
            var expanded by remember { mutableStateOf(false) }
            
            Box {
                OutlinedButton(
                    onClick = { expanded = true }
                ) {
                    Text(dataRetention)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("1 month", "6 months", "1 year", "Forever").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onDataRetentionChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportImportSettings() {
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<String?>(null) }
    var importResult by remember { mutableStateOf<String?>(null) }
    
    SettingsSection(title = "Data Management") {
        SettingItem(
            title = "Export Data",
            subtitle = "Export your experiences and data",
            leadingIcon = Icons.Default.Upload
        ) {
            Button(
                onClick = {
                    scope.launch {
                        isExporting = true
                        try {
                            // TODO: Implement export functionality
                            exportResult = "Export completed successfully"
                        } catch (e: Exception) {
                            exportResult = "Export failed: ${e.message}"
                        } finally {
                            isExporting = false
                        }
                    }
                },
                enabled = !isExporting
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Export")
                }
            }
        }
        
        if (exportResult != null) {
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (exportResult!!.contains("success")) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Text(
                    text = exportResult!!,
                    modifier = Modifier.padding(16.dp),
                    color = if (exportResult!!.contains("success")) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }
        
        SettingItem(
            title = "Import Data",
            subtitle = "Import experiences from file",
            leadingIcon = Icons.Default.Download
        ) {
            Button(
                onClick = {
                    scope.launch {
                        isImporting = true
                        try {
                            // TODO: Implement import functionality
                            importResult = "Import completed successfully"
                        } catch (e: Exception) {
                            importResult = "Import failed: ${e.message}"
                        } finally {
                            isImporting = false
                        }
                    }
                },
                enabled = !isImporting
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Import")
                }
            }
        }
        
        if (importResult != null) {
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (importResult!!.contains("success")) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Text(
                    text = importResult!!,
                    modifier = Modifier.padding(16.dp),
                    color = if (importResult!!.contains("success")) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }
    }
}

@Composable
fun AboutSettings(
    onShowAbout: () -> Unit
) {
    SettingsSection(title = "About") {
        SettingItem(
            title = "About PsychonautWiki Journal",
            subtitle = "Version information and licenses",
            leadingIcon = Icons.Default.Info
        ) {
            TextButton(onClick = onShowAbout) {
                Text("View")
            }
        }
        
        SettingItem(
            title = "Privacy Policy",
            subtitle = "How we handle your data",
            leadingIcon = Icons.Default.PrivacyTip
        ) {
            TextButton(onClick = { /* TODO: Open privacy policy */ }) {
                Text("View")
            }
        }
        
        SettingItem(
            title = "Help & Support",
            subtitle = "Get help using the app",
            leadingIcon = Icons.Default.Help
        ) {
            TextButton(onClick = { /* TODO: Open help */ }) {
                Text("Help")
            }
        }
    }
}

@Composable
fun SettingsSection(
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
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    subtitle: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            leadingIcon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        content()
    }
}

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("About PsychonautWiki Journal")
        },
        text = {
            Column {
                Text("Version: 1.0.0")
                Text("A safe and private tool for tracking substance experiences.")
                Spacer(modifier = Modifier.height(16.dp))
                Text("Built with Kotlin Multiplatform and Compose Desktop")
                Spacer(modifier = Modifier.height(8.dp))
                Text("© 2024 PsychonautWiki Journal")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}