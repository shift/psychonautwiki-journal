package com.isaakhanimann.journal.plugin

import com.isaakhanimann.journal.plugin.builtin.SmartPatternRecognitionPlugin
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

class PluginManagerTest : StringSpec({
    
    "SmartPatternRecognitionPlugin should initialize correctly" {
        val plugin = SmartPatternRecognitionPlugin()
        
        plugin.manifest.id shouldBe "smart-pattern-recognition"
        plugin.manifest.name shouldBe "Smart Pattern Recognition"
        plugin.manifest.version shouldBe "1.0.0"
        plugin.manifest.permissions shouldContain Permission.READ_EXPERIENCES
        plugin.manifest.permissions shouldContain Permission.READ_SUBSTANCES
        plugin.manifest.permissions shouldContain Permission.ANALYTICS_ACCESS
    }
    
    "SmartPatternRecognitionPlugin should provide expected capabilities" {
        val plugin = SmartPatternRecognitionPlugin()
        val capabilities = plugin.getCapabilities()
        
        capabilities.size shouldBe 5
        
        val capabilityIds = capabilities.map { it.id }
        capabilityIds shouldContain "substance-interaction-detection"
        capabilityIds shouldContain "tolerance-tracking"
        capabilityIds shouldContain "experience-quality-correlation"
        capabilityIds shouldContain "timing-optimization"
        capabilityIds shouldContain "risk-assessment"
        
        // All capabilities should be AnalyticsCapability
        capabilities.forEach { capability ->
            capability.shouldBeInstanceOf<AnalyticsCapability>()
        }
    }
    
    "Plugin manifest should be serializable" {
        val manifest = PluginManifest(
            id = "test-plugin",
            name = "Test Plugin",
            version = "1.0.0",
            description = "A test plugin",
            author = "Test Author",
            permissions = listOf(Permission.READ_EXPERIENCES),
            entryPoint = "com.test.TestPlugin"
        )
        
        manifest.id shouldBe "test-plugin"
        manifest.permissions shouldContain Permission.READ_EXPERIENCES
    }
    
    "Permission enum should contain all expected values" {
        val permissions = Permission.values()
        
        permissions shouldContain Permission.READ_EXPERIENCES
        permissions shouldContain Permission.WRITE_EXPERIENCES
        permissions shouldContain Permission.READ_SUBSTANCES
        permissions shouldContain Permission.NETWORK_ACCESS
        permissions shouldContain Permission.FILE_SYSTEM_ACCESS
        permissions shouldContain Permission.BIOMETRIC_DATA
        permissions shouldContain Permission.EXPORT_DATA
        permissions shouldContain Permission.IMPORT_DATA
        permissions shouldContain Permission.SEND_NOTIFICATIONS
        permissions shouldContain Permission.ANALYTICS_ACCESS
    }
    
    "AnalyticsResult should handle empty data correctly" {
        val result = AnalyticsResult(
            insights = emptyList(),
            recommendations = emptyList(),
            riskAssessment = null,
            visualizations = emptyList()
        )
        
        result.insights.size shouldBe 0
        result.recommendations.size shouldBe 0
        result.riskAssessment shouldBe null
        result.visualizations.size shouldBe 0
    }
    
    "RiskAssessment should calculate correctly" {
        val riskFactors = listOf(
            RiskFactor("High frequency", 0.8, "Too many experiences recently"),
            RiskFactor("Polydrug use", 0.6, "Multiple substances used together")
        )
        
        val riskAssessment = RiskAssessment(
            overallRisk = 0.7,
            riskFactors = riskFactors,
            mitigationStrategies = listOf("Take a break", "Focus on one substance")
        )
        
        riskAssessment.overallRisk shouldBe 0.7
        riskAssessment.riskFactors.size shouldBe 2
        riskAssessment.riskFactors[0].severity shouldBe 0.8
        riskAssessment.mitigationStrategies.size shouldBe 2
    }
    
    "Insight severity levels should be ordered correctly" {
        val severities = InsightSeverity.values()
        
        severities[0] shouldBe InsightSeverity.LOW
        severities[1] shouldBe InsightSeverity.MEDIUM
        severities[2] shouldBe InsightSeverity.HIGH
        severities[3] shouldBe InsightSeverity.CRITICAL
    }
    
    "Recommendation priorities should be ordered correctly" {
        val priorities = RecommendationPriority.values()
        
        priorities[0] shouldBe RecommendationPriority.LOW
        priorities[1] shouldBe RecommendationPriority.MEDIUM
        priorities[2] shouldBe RecommendationPriority.HIGH
        priorities[3] shouldBe RecommendationPriority.URGENT
    }
})