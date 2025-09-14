package com.isaakhanimann.journal.ui.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

interface FileDialogHandler {
    suspend fun saveFile(
        title: String = "Save File",
        defaultName: String = "",
        extension: String = "*"
    ): String?
    
    suspend fun openFile(
        title: String = "Open File",
        extension: String = "*"
    ): String?
}

class DesktopFileDialogHandler : FileDialogHandler {
    
    override suspend fun saveFile(
        title: String,
        defaultName: String,
        extension: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE).apply {
                if (defaultName.isNotBlank()) {
                    file = defaultName
                }
                
                // Set file filter based on extension
                when (extension.lowercase()) {
                    "json" -> {
                        setFilenameFilter { _, name -> 
                            name.lowercase().endsWith(".json") 
                        }
                    }
                    "csv" -> {
                        setFilenameFilter { _, name -> 
                            name.lowercase().endsWith(".csv") 
                        }
                    }
                }
                
                isVisible = true
            }
            
            val selectedFile = dialog.file
            val selectedDirectory = dialog.directory
            
            if (selectedFile != null && selectedDirectory != null) {
                val fullPath = "$selectedDirectory$selectedFile"
                
                // Ensure the file has the correct extension
                val finalPath = when {
                    extension == "json" && !fullPath.lowercase().endsWith(".json") -> "$fullPath.json"
                    extension == "csv" && !fullPath.lowercase().endsWith(".csv") -> "$fullPath.csv"
                    else -> fullPath
                }
                
                finalPath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun openFile(
        title: String,
        extension: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD).apply {
                // Set file filter based on extension
                when (extension.lowercase()) {
                    "json" -> {
                        setFilenameFilter { _, name -> 
                            name.lowercase().endsWith(".json") 
                        }
                    }
                    "csv" -> {
                        setFilenameFilter { _, name -> 
                            name.lowercase().endsWith(".csv") 
                        }
                    }
                }
                
                isVisible = true
            }
            
            val selectedFile = dialog.file
            val selectedDirectory = dialog.directory
            
            if (selectedFile != null && selectedDirectory != null) {
                val fullPath = "$selectedDirectory$selectedFile"
                
                // Verify the file exists
                if (File(fullPath).exists()) {
                    fullPath
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}