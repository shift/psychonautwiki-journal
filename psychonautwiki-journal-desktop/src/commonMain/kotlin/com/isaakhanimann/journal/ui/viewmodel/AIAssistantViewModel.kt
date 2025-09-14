package com.isaakhanimann.journal.ui.viewmodel

import com.isaakhanimann.journal.ai.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AIAssistantViewModel(
    private val aiAssistant: AIAssistant
) : BaseViewModel() {
    
    val currentConversation = aiAssistant.currentConversation
    val conversationHistory = aiAssistant.conversationHistory
    val isProcessing = aiAssistant.isProcessing
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        // Start with a default conversation if none exists
        viewModelScope.launch {
            if (currentConversation.value == null) {
                startNewConversation()
            }
        }
    }
    
    fun startNewConversation(title: String? = null) {
        viewModelScope.launch {
            try {
                aiAssistant.startNewConversation(title)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to start new conversation"
            }
        }
    }
    
    fun sendMessage(content: String, conversationId: String? = null) {
        viewModelScope.launch {
            try {
                aiAssistant.sendMessage(content, conversationId)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to send message"
            }
        }
    }
    
    fun switchToConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                val conversation = conversationHistory.value.find { it.id == conversationId }
                if (conversation != null) {
                    // The conversation switching is handled internally by the assistant
                    // We could add a method to AIAssistant for this if needed
                    _error.value = null
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to switch conversation"
            }
        }
    }
    
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                aiAssistant.deleteConversation(conversationId)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete conversation"
            }
        }
    }
    
    fun updateConversationTitle(conversationId: String, title: String) {
        viewModelScope.launch {
            try {
                aiAssistant.updateConversationTitle(conversationId, title)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update conversation title"
            }
        }
    }
    
    fun exportConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                val exportContent = aiAssistant.exportConversation(conversationId)
                // TODO: Handle file saving through FileDialogHandler
                // For now, we could copy to clipboard or show in a dialog
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to export conversation"
            }
        }
    }
    
    fun analyzeExperiences(experienceIds: List<String>) {
        viewModelScope.launch {
            try {
                val result = aiAssistant.analyzeExperiences(experienceIds)
                // The analysis result could be sent as a message to the current conversation
                sendMessage("Please analyze my selected experiences: ${experienceIds.joinToString(", ")}")
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to analyze experiences"
            }
        }
    }
    
    fun askQuickQuestion(question: String, context: Map<String, Any> = emptyMap()) {
        viewModelScope.launch {
            try {
                val result = aiAssistant.askQuestion(question, context)
                // Send the question as a regular message
                sendMessage(question)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to process question"
            }
        }
    }
    
    fun getIntegrationPrompt(
        category: IntegrationCategory? = null,
        difficulty: PromptDifficulty? = null
    ): IntegrationPrompt {
        return IntegrationPrompts.getRandomPrompt(category, difficulty)
    }
    
    fun startIntegrationSession(prompt: IntegrationPrompt) {
        val integrationMessage = """
            Let's work on integration using this prompt:
            
            **${prompt.title}**
            Category: ${prompt.category.name}
            Difficulty: ${prompt.difficulty.name}
            
            ${prompt.content}
            
            Take your time to reflect on this. I'm here to help guide you through the process.
        """.trimIndent()
        
        sendMessage(integrationMessage)
    }
    
    fun dismissError() {
        _error.value = null
    }
}