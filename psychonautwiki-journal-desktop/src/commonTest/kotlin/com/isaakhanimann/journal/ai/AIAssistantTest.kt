package com.isaakhanimann.journal.ai

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldNotBeBlank

class AIAssistantTest : StringSpec({
    
    "AICapabilities should be properly defined" {
        val capabilities = AICapabilities(
            canAnalyzeExperiences = true,
            canProvideRecommendations = true,
            canAnswerQuestions = true,
            canGenerateInsights = true,
            canProcessNaturalLanguage = true,
            supportedLanguages = listOf("en", "es")
        )
        
        capabilities.canAnalyzeExperiences shouldBe true
        capabilities.canProvideRecommendations shouldBe true
        capabilities.canAnswerQuestions shouldBe true
        capabilities.canGenerateInsights shouldBe true
        capabilities.canProcessNaturalLanguage shouldBe true
        capabilities.supportedLanguages shouldContain "en"
        capabilities.supportedLanguages shouldContain "es"
    }
    
    "AIPersonality should have correct configuration" {
        val personality = AIPersonality(
            name = "Lucy",
            description = "Harm reduction assistant",
            tone = ConversationTone.EMPATHETIC,
            expertise = listOf(ExpertiseArea.HARM_REDUCTION, ExpertiseArea.SAFETY),
            disclaimers = listOf("Not medical advice", "Consult professionals")
        )
        
        personality.name shouldBe "Lucy"
        personality.description shouldBe "Harm reduction assistant"
        personality.tone shouldBe ConversationTone.EMPATHETIC
        personality.expertise shouldContain ExpertiseArea.HARM_REDUCTION
        personality.expertise shouldContain ExpertiseArea.SAFETY
        personality.disclaimers.size shouldBe 2
    }
    
    "ChatMessage should be properly structured" {
        val message = ChatMessage(
            id = "test-id",
            content = "Hello, this is a test message",
            role = MessageRole.USER,
            timestamp = kotlinx.datetime.Clock.System.now(),
            metadata = mapOf("test" to "value")
        )
        
        message.id shouldBe "test-id"
        message.content shouldBe "Hello, this is a test message"
        message.role shouldBe MessageRole.USER
        message.timestamp shouldNotBe null
        message.metadata["test"] shouldBe "value"
    }
    
    "Conversation should manage messages correctly" {
        val now = kotlinx.datetime.Clock.System.now()
        val messages = listOf(
            ChatMessage("1", "Hello", MessageRole.USER, now),
            ChatMessage("2", "Hi there!", MessageRole.ASSISTANT, now)
        )
        
        val conversation = Conversation(
            id = "conv-1",
            title = "Test Conversation",
            messages = messages,
            createdAt = now,
            updatedAt = now
        )
        
        conversation.id shouldBe "conv-1"
        conversation.title shouldBe "Test Conversation"
        conversation.messages.size shouldBe 2
        conversation.messages[0].role shouldBe MessageRole.USER
        conversation.messages[1].role shouldBe MessageRole.ASSISTANT
    }
    
    "LocalAIProvider should have correct capabilities" {
        val provider = LocalAIProvider()
        
        provider.name shouldBe "Local AI Assistant"
        provider.description.shouldNotBeBlank()
        provider.capabilities.canAnalyzeExperiences shouldBe true
        provider.capabilities.canProvideRecommendations shouldBe true
        provider.capabilities.canAnswerQuestions shouldBe true
        provider.capabilities.canGenerateInsights shouldBe true
        provider.capabilities.canProcessNaturalLanguage shouldBe true
        provider.capabilities.supportedLanguages shouldContain "en"
    }
    
    "OpenAIProvider should have extended capabilities" {
        val provider = OpenAIProvider()
        
        provider.name shouldBe "OpenAI GPT"
        provider.description.shouldNotBeBlank()
        provider.capabilities.supportedLanguages.shouldNotBeEmpty()
        provider.capabilities.supportedLanguages shouldContain "en"
        provider.capabilities.supportedLanguages shouldContain "es"
        provider.capabilities.supportedLanguages shouldContain "fr"
    }
    
    "AIQuery should contain all necessary fields" {
        val query = AIQuery(
            content = "What is harm reduction?",
            context = mapOf("topic" to "safety"),
            conversationHistory = emptyList(),
            maxTokens = 2048,
            temperature = 0.7,
            systemPrompt = "You are a helpful assistant"
        )
        
        query.content shouldBe "What is harm reduction?"
        query.context["topic"] shouldBe "safety"
        query.maxTokens shouldBe 2048
        query.temperature shouldBe 0.7
        query.systemPrompt shouldBe "You are a helpful assistant"
    }
    
    "AIResponse should handle suggestions and follow-ups" {
        val response = AIResponse(
            content = "Harm reduction is a set of practical strategies...",
            confidence = 0.9,
            suggestions = listOf("Learn more about set and setting", "Research specific substances"),
            followUpQuestions = listOf("What substances are you interested in?"),
            citations = listOf("PsychonautWiki"),
            metadata = mapOf("source" to "local")
        )
        
        response.content.shouldNotBeBlank()
        response.confidence shouldBe 0.9
        response.suggestions.size shouldBe 2
        response.followUpQuestions.size shouldBe 1
        response.citations shouldContain "PsychonautWiki"
        response.metadata["source"] shouldBe "local"
    }
    
    "IntegrationPrompts should provide different categories" {
        val categories = IntegrationCategory.values()
        
        categories shouldContain IntegrationCategory.REFLECTION
        categories shouldContain IntegrationCategory.GOAL_SETTING
        categories shouldContain IntegrationCategory.BEHAVIOR_CHANGE
        categories shouldContain IntegrationCategory.EMOTIONAL_PROCESSING
        categories shouldContain IntegrationCategory.CREATIVE_EXPRESSION
        categories shouldContain IntegrationCategory.RELATIONSHIP_WORK
        categories shouldContain IntegrationCategory.SPIRITUAL_EXPLORATION
        
        // Test that we can get prompts by category
        val reflectionPrompts = IntegrationPrompts.getPromptsByCategory(IntegrationCategory.REFLECTION)
        reflectionPrompts.shouldNotBeEmpty()
        
        val randomPrompt = IntegrationPrompts.getRandomPrompt()
        randomPrompt.content.shouldNotBeBlank()
        randomPrompt.title.shouldNotBeBlank()
    }
    
    "PromptDifficulty should have correct levels" {
        val difficulties = PromptDifficulty.values()
        
        difficulties[0] shouldBe PromptDifficulty.BEGINNER
        difficulties[1] shouldBe PromptDifficulty.INTERMEDIATE
        difficulties[2] shouldBe PromptDifficulty.ADVANCED
        
        // Test filtering by difficulty
        val beginnerPrompts = IntegrationPrompts.getPromptsByDifficulty(PromptDifficulty.BEGINNER)
        beginnerPrompts.shouldNotBeEmpty()
        beginnerPrompts.forEach { prompt ->
            prompt.difficulty shouldBe PromptDifficulty.BEGINNER
        }
    }
})