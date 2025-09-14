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

@Composable
fun SettingsScreen(navController: DesktopNavigationController) {
    val configuration = LocalConfiguration.current
    val responsiveConfig = calculateWindowSizeClass(
        width = configuration.screenWidthDp.dp,
        height = configuration.screenHeightDp.dp
    )
    
    var darkMode by remember { mutableStateOf(false) }
    var enableNotifications by remember { mutableStateOf(true) }
    var dataRetention by remember { mutableStateOf("1 year") }
    
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
            }
        }
        
        // Appearance Section
        item {
            SettingsSection(title = "Appearance") {
                SettingItem(
                    title = "Dark Mode",
                    description = "Use dark theme throughout the app",
                    icon = Icons.Default.Settings,
                    trailing = {
                        Switch(
                            checked = darkMode,
                            onCheckedChange = { darkMode = it }
                        )
                    }
                )
            }
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
                    icon = Icons.Default.Info
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