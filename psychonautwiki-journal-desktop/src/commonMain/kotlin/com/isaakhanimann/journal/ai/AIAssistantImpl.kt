package com.isaakhanimann.journal.ai

import com.isaakhanimann.journal.data.repository.ExperienceRepository
import com.isaakhanimann.journal.data.repository.PreferencesRepository
import com.isaakhanimann.journal.plugin.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class AIAssistantImpl : AIAssistant, KoinComponent {
    private val experienceRepository: ExperienceRepository by inject()
    private val preferencesRepository: PreferencesRepository by inject()
    private val pluginManager: PluginManager by inject()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    override val config = AIConfig(
        personality = AIPersonality(
            name = "Lucy",
            description = "Your personal harm reduction assistant for psychedelic experiences",
            tone = ConversationTone.EMPATHETIC,
            expertise = listOf(
                ExpertiseArea.HARM_REDUCTION,
                ExpertiseArea.SAFETY,
                ExpertiseArea.INTEGRATION,
                ExpertiseArea.PSYCHOLOGY
            ),
            disclaimers = listOf(
                "I provide harm reduction information, not medical advice",
                "Always consult healthcare professionals for medical concerns",
                "My suggestions are based on general safety principles",
                "Individual responses to substances can vary significantly"
            )
        ),
        capabilities = AICapabilities(
            canAnalyzeExperiences = true,
            canProvideRecommendations = true,
            canAnswerQuestions = true,
            canGenerateInsights = true,
            canProcessNaturalLanguage = true,
            supportedLanguages = listOf("en")
        ),
        maxTokens = 2048,
        temperature = 0.7,
        systemPrompt = buildSystemPrompt()
    )
    
    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    override val currentConversation: StateFlow<Conversation?> = _currentConversation.asStateFlow()
    
    private val _conversationHistory = MutableStateFlow<List<Conversation>>(emptyList())
    override val conversationHistory: StateFlow<List<Conversation>> = _conversationHistory.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    override val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val aiProvider: AIProvider = LocalAIProvider()
    
    init {
        scope.launch {
            loadConversationHistory()
            initializeAIProvider()
        }
    }
    
    private suspend fun initializeAIProvider() {
        aiProvider.initialize(emptyMap())
    }
    
    private suspend fun loadConversationHistory() {
        val conversationsJson = preferencesRepository.getString("ai_conversations", "[]")
        try {
            val conversations = json.decodeFromString<List<Conversation>>(conversationsJson)
            _conversationHistory.value = conversations.sortedByDescending { it.updatedAt }
            
            // Load the most recent conversation as current
            conversations.maxByOrNull { it.updatedAt }?.let { recent ->
                _currentConversation.value = recent
            }
        } catch (e: Exception) {
            _conversationHistory.value = emptyList()
        }
    }
    
    private suspend fun saveConversationHistory() {
        val conversationsJson = json.encodeToString(_conversationHistory.value)
        preferencesRepository.setString("ai_conversations", conversationsJson)
    }
    
    override suspend fun startNewConversation(title: String?): Conversation {
        val now = Clock.System.now()
        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            title = title ?: "New Conversation",
            messages = listOf(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = buildWelcomeMessage(),
                    role = MessageRole.ASSISTANT,
                    timestamp = now
                )
            ),
            createdAt = now,
            updatedAt = now
        )
        
        _currentConversation.value = conversation
        _conversationHistory.value = listOf(conversation) + _conversationHistory.value
        saveConversationHistory()
        
        return conversation
    }
    
    override suspend fun sendMessage(content: String, conversationId: String?): ChatMessage {
        _isProcessing.value = true
        
        try {
            val conversation = conversationId?.let { id ->
                _conversationHistory.value.find { it.id == id }
            } ?: _currentConversation.value ?: startNewConversation()
            
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = content,
                role = MessageRole.USER,
                timestamp = Clock.System.now()
            )
            
            val updatedMessages = conversation.messages + userMessage
            
            // Generate AI response
            val context = buildContextMap(conversation.messages)
            val query = AIQuery(
                content = content,
                context = context,
                conversationHistory = updatedMessages,
                maxTokens = config.maxTokens,
                temperature = config.temperature,
                systemPrompt = config.systemPrompt
            )
            
            val response = aiProvider.processQuery(query).getOrThrow()
            
            val assistantMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = response.content,
                role = MessageRole.ASSISTANT,
                timestamp = Clock.System.now(),
                metadata = mapOf(
                    "confidence" to response.confidence.toString(),
                    "suggestions" to response.suggestions.joinToString(","),
                    "followUpQuestions" to response.followUpQuestions.joinToString(",")
                )
            )
            
            val finalMessages = updatedMessages + assistantMessage
            val updatedConversation = conversation.copy(
                messages = finalMessages,
                updatedAt = Clock.System.now(),
                title = if (conversation.title == "New Conversation" && finalMessages.size == 3) {
                    generateConversationTitle(content)
                } else conversation.title
            )
            
            _currentConversation.value = updatedConversation
            _conversationHistory.value = _conversationHistory.value.map { conv ->
                if (conv.id == conversation.id) updatedConversation else conv
            }
            saveConversationHistory()
            
            return assistantMessage
            
        } finally {
            _isProcessing.value = false
        }
    }
    
    override suspend fun analyzeExperiences(experienceIds: List<String>): AIResult {
        val experiences = experienceRepository.getAllExperiences().first()
            .filter { experienceIds.contains(it.id) }
        
        if (experiences.isEmpty()) {
            return AIResult(
                response = "No experiences found to analyze.",
                confidence = 0.0
            )
        }
        
        val analysisPrompt = buildExperienceAnalysisPrompt(experiences)
        val context = mapOf(
            "experiences" to experiences,
            "analysis_type" to "experience_review"
        )
        
        val query = AIQuery(
            content = analysisPrompt,
            context = context,
            conversationHistory = emptyList(),
            maxTokens = config.maxTokens,
            temperature = config.temperature,
            systemPrompt = config.systemPrompt
        )
        
        val response = aiProvider.processQuery(query).getOrThrow()
        return AIResult(
            response = response.content,
            confidence = response.confidence,
            suggestions = response.suggestions,
            followUpQuestions = response.followUpQuestions
        )
    }
    
    override suspend fun getRecommendations(context: String): List<Recommendation> {
        val prompt = "Based on the following context, provide specific harm reduction recommendations: $context"
        
        val query = AIQuery(
            content = prompt,
            context = mapOf("request_type" to "recommendations"),
            conversationHistory = emptyList(),
            maxTokens = config.maxTokens,
            temperature = config.temperature,
            systemPrompt = config.systemPrompt
        )
        
        val response = aiProvider.processQuery(query).getOrThrow()
        
        // Parse response into recommendations
        return parseRecommendationsFromResponse(response.content)
    }
    
    override suspend fun askQuestion(question: String, context: Map<String, Any>): AIResult {
        val query = AIQuery(
            content = question,
            context = context,
            conversationHistory = emptyList(),
            maxTokens = config.maxTokens,
            temperature = config.temperature,
            systemPrompt = config.systemPrompt
        )
        
        val response = aiProvider.processQuery(query).getOrThrow()
        return AIResult(
            response = response.content,
            confidence = response.confidence,
            suggestions = response.suggestions,
            followUpQuestions = response.followUpQuestions
        )
    }
    
    override suspend fun generateInsights(dataContext: AnalyticsContext): List<Insight> {
        val prompt = buildInsightGenerationPrompt(dataContext)
        
        val query = AIQuery(
            content = prompt,
            context = mapOf(
                "experiences" to dataContext.experiences,
                "substances" to dataContext.substances,
                "analysis_type" to "insight_generation"
            ),
            conversationHistory = emptyList(),
            maxTokens = config.maxTokens,
            temperature = config.temperature,
            systemPrompt = config.systemPrompt
        )
        
        val response = aiProvider.processQuery(query).getOrThrow()
        return parseInsightsFromResponse(response.content)
    }
    
    override fun getConversation(id: String): Flow<Conversation?> {
        return _conversationHistory.map { conversations ->
            conversations.find { it.id == id }
        }
    }
    
    override suspend fun deleteConversation(id: String): Boolean {
        _conversationHistory.value = _conversationHistory.value.filter { it.id != id }
        if (_currentConversation.value?.id == id) {
            _currentConversation.value = _conversationHistory.value.firstOrNull()
        }
        saveConversationHistory()
        return true
    }
    
    override suspend fun updateConversationTitle(id: String, title: String): Boolean {
        _conversationHistory.value = _conversationHistory.value.map { conversation ->
            if (conversation.id == id) {
                conversation.copy(title = title, updatedAt = Clock.System.now())
            } else conversation
        }
        
        if (_currentConversation.value?.id == id) {
            _currentConversation.value = _currentConversation.value?.copy(title = title)
        }
        
        saveConversationHistory()
        return true
    }
    
    override suspend fun exportConversation(id: String): String {
        val conversation = _conversationHistory.value.find { it.id == id }
            ?: return ""
        
        return buildString {
            appendLine("# ${conversation.title}")
            appendLine("Created: ${conversation.createdAt}")
            appendLine("Updated: ${conversation.updatedAt}")
            appendLine()
            
            conversation.messages.forEach { message ->
                appendLine("## ${message.role.name}")
                appendLine("**Time:** ${message.timestamp}")
                appendLine()
                appendLine(message.content)
                appendLine()
                
                if (message.metadata.isNotEmpty()) {
                    appendLine("**Metadata:**")
                    message.metadata.forEach { (key, value) ->
                        appendLine("- $key: $value")
                    }
                    appendLine()
                }
            }
        }
    }
    
    private fun buildSystemPrompt(): String {
        return """
            You are ${config.personality.name}, a ${config.personality.description}.
            
            Your expertise includes: ${config.personality.expertise.joinToString(", ")}
            Your conversational tone is: ${config.personality.tone.name.lowercase()}
            
            IMPORTANT DISCLAIMERS:
            ${config.personality.disclaimers.joinToString("\n") { "- $it" }}
            
            GUIDELINES:
            - Always prioritize safety and harm reduction
            - Provide evidence-based information when possible
            - Be empathetic and non-judgmental
            - Encourage integration and reflection
            - Never provide specific dosage recommendations
            - Always suggest consulting healthcare professionals for medical concerns
            - Support users in making informed decisions
            
            Respond helpfully while maintaining these principles.
        """.trimIndent()
    }
    
    private fun buildWelcomeMessage(): String {
        return """
            Hello! I'm ${config.personality.name}, your personal harm reduction assistant.
            
            I'm here to help you:
            • Analyze your experience patterns and identify insights
            • Provide harm reduction guidance and safety information
            • Support integration and reflection processes
            • Answer questions about substances and practices
            
            ${config.personality.disclaimers.joinToString("\n") { "⚠️ $it" }}
            
            How can I assist you today?
        """.trimIndent()
    }
    
    private fun buildContextMap(messages: List<ChatMessage>): Map<String, Any> {
        return mapOf(
            "conversation_length" to messages.size,
            "recent_topics" to extractTopics(messages.takeLast(5)),
            "user_preferences" to getUserPreferences()
        )
    }
    
    private fun extractTopics(messages: List<ChatMessage>): List<String> {
        val keywords = listOf(
            "substance", "dose", "dosage", "experience", "trip", "integration",
            "safety", "harm", "reduction", "risk", "set", "setting", "tolerance"
        )
        
        return messages.flatMap { message ->
            keywords.filter { keyword ->
                message.content.contains(keyword, ignoreCase = true)
            }
        }.distinct()
    }
    
    private suspend fun getUserPreferences(): Map<String, Any> {
        return mapOf(
            "tone_preference" to preferencesRepository.getString("ai_tone_preference", "empathetic"),
            "detail_level" to preferencesRepository.getString("ai_detail_level", "medium"),
            "focus_areas" to preferencesRepository.getString("ai_focus_areas", "safety,integration")
        )
    }
    
    private fun generateConversationTitle(firstMessage: String): String {
        val words = firstMessage.split(" ").take(5)
        return words.joinToString(" ").take(50) + if (words.size > 5) "..." else ""
    }
    
    private fun buildExperienceAnalysisPrompt(experiences: List<Experience>): String {
        return """
            Please analyze the following experiences and provide insights:
            
            ${experiences.mapIndexed { index, exp ->
                """
                Experience ${index + 1}:
                - Date: ${exp.date}
                - Substances: ${exp.ingestions?.joinToString(", ") { "${it.substanceName} (${it.dose})" }}
                - Setting: ${exp.location ?: "Not specified"}
                - Overall Rating: ${exp.overallRating ?: "Not rated"}
                - Notes: ${exp.notes ?: "No notes"}
                """.trimIndent()
            }.joinToString("\n\n")}
            
            Please provide:
            1. Patterns you notice across these experiences
            2. Safety considerations or concerns
            3. Suggestions for optimization or improvement
            4. Integration opportunities
        """.trimIndent()
    }
    
    private fun buildInsightGenerationPrompt(context: AnalyticsContext): String {
        return """
            Based on the user's experience data, generate insights about their patterns:
            
            - Total experiences: ${context.experiences.size}
            - Unique substances: ${context.substances.size}
            - Time range: ${context.timeRange?.let { "${it.start} to ${it.end}" } ?: "All time"}
            
            Focus on:
            1. Usage patterns and frequency
            2. Substance preferences and combinations
            3. Setting and timing preferences
            4. Experience quality trends
            5. Potential areas for improvement
        """.trimIndent()
    }
    
    private fun parseRecommendationsFromResponse(response: String): List<Recommendation> {
        // Simple parsing - in a real implementation, this would be more sophisticated
        val lines = response.split("\n").filter { it.trim().isNotEmpty() }
        
        return lines.mapIndexedNotNull { index, line ->
            if (line.trim().startsWith("-") || line.trim().startsWith("•")) {
                Recommendation(
                    id = "ai-rec-${UUID.randomUUID()}",
                    title = "AI Recommendation ${index + 1}",
                    description = line.trim().removePrefix("-").removePrefix("•").trim(),
                    actionable = true,
                    priority = RecommendationPriority.MEDIUM,
                    category = RecommendationCategory.SAFETY
                )
            } else null
        }
    }
    
    private fun parseInsightsFromResponse(response: String): List<Insight> {
        // Simple parsing - in a real implementation, this would be more sophisticated
        val lines = response.split("\n").filter { it.trim().isNotEmpty() }
        
        return lines.mapIndexedNotNull { index, line ->
            if (line.trim().startsWith("-") || line.trim().startsWith("•")) {
                Insight(
                    id = "ai-insight-${UUID.randomUUID()}",
                    title = "AI Insight ${index + 1}",
                    description = line.trim().removePrefix("-").removePrefix("•").trim(),
                    confidence = 0.7,
                    severity = InsightSeverity.MEDIUM
                )
            } else null
        }
    }
}