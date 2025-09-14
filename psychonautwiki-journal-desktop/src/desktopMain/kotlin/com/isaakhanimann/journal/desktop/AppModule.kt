package com.isaakhanimann.journal.desktop

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.isaakhanimann.journal.database.Database
import com.isaakhanimann.journal.data.repository.*
import com.isaakhanimann.journal.data.experience.ExperienceTracker
import com.isaakhanimann.journal.data.export.ExportManager
import com.isaakhanimann.journal.data.export.ExportManagerImpl
import com.isaakhanimann.journal.data.import.ImportManager
import com.isaakhanimann.journal.data.import.ImportManagerImpl
import com.isaakhanimann.journal.plugin.PluginManager
import com.isaakhanimann.journal.plugin.PluginManagerImpl
import com.isaakhanimann.journal.ai.AIAssistant
import com.isaakhanimann.journal.ai.AIAssistantImpl
import com.isaakhanimann.journal.ui.theme.ThemeManager
import com.isaakhanimann.journal.ui.utils.FileDialogHandler
import com.isaakhanimann.journal.ui.utils.DesktopFileDialogHandler
import com.isaakhanimann.journal.ui.viewmodel.*
import org.koin.dsl.module
import java.io.File
import java.sql.DriverManager

val appModule = module {
    
    single<SqlDriver> {
        val databasePath = System.getProperty("user.home") + "/.psychonautwiki-journal/database.db"
        val databaseFile = File(databasePath)
        databaseFile.parentFile?.mkdirs()
        
        val driver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")
        
        // Check if Experience table exists using JDBC
        try {
            val connection = DriverManager.getConnection("jdbc:sqlite:$databasePath")
            val resultSet = connection.metaData.getTables(null, null, "Experience", null)
            val tableExists = resultSet.next()
            resultSet.close()
            connection.close()
            
            if (!tableExists) {
                Database.Schema.create(driver)
            }
        } catch (e: Exception) {
            // If check fails, assume tables don't exist and create schema
            Database.Schema.create(driver)
        }
        
        driver
    }
    
    single<Database> {
        Database(get())
    }
    
    // Repositories
    single<ExperienceRepository> {
        ExperienceRepositoryImpl(get())
    }
    
    single<SubstanceRepository> {
        SubstanceRepositoryImpl(get())
    }
    
    single<PreferencesRepository> {
        PreferencesRepositoryImpl(get())
    }
    
    single<DraftManager> {
        DraftManagerImpl(get())
    }
    
    single<ExportManager> {
        ExportManagerImpl(get())
    }
    
    single<ImportManager> {
        ImportManagerImpl(get())
    }
    
    single<FileDialogHandler> {
        DesktopFileDialogHandler()
    }
    
    // Plugin System
    single<PluginManager> {
        PluginManagerImpl()
    }
    
    // AI Assistant
    single<AIAssistant> {
        AIAssistantImpl()
    }
    
    // Theme Management
    single<ThemeManager> {
        ThemeManager(get())
    }
    
    // Business Logic
    single<ExperienceTracker> {
        ExperienceTracker(get())
    }
    
    // ViewModels
    factory<DashboardViewModel> {
        DashboardViewModel(get())
    }
    
    factory<ExperiencesViewModel> {
        ExperiencesViewModel(get())
    }
    
    factory<ExperienceEditorViewModel> {
        ExperienceEditorViewModel(get(), get())
    }
    
    factory<IngestionEditorViewModel> {
        IngestionEditorViewModel(get(), get(), get())
    }
    
    factory<ExperienceTimelineViewModel> {
        ExperienceTimelineViewModel(get())
    }
    
    factory<SubstancesViewModel> {
        SubstancesViewModel(get())
    }
    
    factory<SettingsViewModel> {
        SettingsViewModel(get())
    }
    
    factory<AnalyticsViewModel> {
        AnalyticsViewModel(get(), get())
    }
    
    factory<AIAssistantViewModel> {
        AIAssistantViewModel(get())
    }
}