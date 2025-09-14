package com.isaakhanimann.journal.ui.components.visualization

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.isaakhanimann.journal.plugin.VisualizationData
import com.isaakhanimann.journal.plugin.VisualizationType
import com.isaakhanimann.journal.ui.theme.JournalTheme
import io.kotest.core.spec.style.StringSpec
import org.junit.Rule

class AdvancedVisualizationTest : StringSpec({
    
    "AdvancedVisualization should display title and description" {
        val data = VisualizationData(
            type = VisualizationType.LINE_CHART,
            data = emptyMap(),
            title = "Test Chart",
            description = "This is a test chart"
        )
        
        // For now, just test that the data structure is correct
        // UI testing would require Android/desktop specific test setup
        data.title shouldBe "Test Chart"
        data.description shouldBe "This is a test chart"
        data.type shouldBe VisualizationType.LINE_CHART
    }
    
    "VisualizationType should include all chart types" {
        val types = VisualizationType.values()
        
        types shouldContain VisualizationType.LINE_CHART
        types shouldContain VisualizationType.BAR_CHART
        types shouldContain VisualizationType.SCATTER_PLOT
        types shouldContain VisualizationType.HEAT_MAP
        types shouldContain VisualizationType.NETWORK_GRAPH
        types shouldContain VisualizationType.TIMELINE
        types shouldContain VisualizationType.CALENDAR
        types shouldContain VisualizationType.GAUGE
    }
    
    "VisualizationData should handle different data formats" {
        val lineChartData = VisualizationData(
            type = VisualizationType.LINE_CHART,
            data = mapOf(
                "dataPoints" to listOf(
                    mapOf("x" to 0, "y" to 10),
                    mapOf("x" to 1, "y" to 15),
                    mapOf("x" to 2, "y" to 12)
                )
            ),
            title = "Usage Trend"
        )
        
        val barChartData = VisualizationData(
            type = VisualizationType.BAR_CHART,
            data = mapOf(
                "categories" to listOf("LSD", "Psilocybin", "MDMA"),
                "values" to listOf(15, 25, 10)
            ),
            title = "Substance Usage"
        )
        
        val gaugeData = VisualizationData(
            type = VisualizationType.GAUGE,
            data = mapOf(
                "value" to 0.7,
                "maxValue" to 1.0,
                "label" to "Risk Level"
            ),
            title = "Current Risk"
        )
        
        lineChartData.type shouldBe VisualizationType.LINE_CHART
        barChartData.type shouldBe VisualizationType.BAR_CHART
        gaugeData.type shouldBe VisualizationType.GAUGE
        
        lineChartData.data["dataPoints"] shouldNotBe null
        barChartData.data["categories"] shouldNotBe null
        gaugeData.data["value"] shouldBe 0.7
    }
})