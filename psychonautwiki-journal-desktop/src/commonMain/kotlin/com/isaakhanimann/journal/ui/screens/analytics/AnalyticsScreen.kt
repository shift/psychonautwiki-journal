package com.isaakhanimann.journal.ui.screens.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.plugin.*
import com.isaakhanimann.journal.ui.viewmodel.AnalyticsViewModel
import com.isaakhanimann.journal.ui.viewmodel.AnalyticsTimeRange
import com.isaakhanimann.journal.ui.components.visualization.AdvancedVisualization
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = koinInject()
) {
    val analyticsResults by viewModel.analyticsResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.refreshAnalytics()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Analytics & Insights",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeRangeSelector(
                    selectedRange = selectedTimeRange,
                    onRangeSelected = viewModel::setTimeRange
                )
                
                IconButton(
                    onClick = { viewModel.refreshAnalytics() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Risk Assessment Dashboard
                analyticsResults.firstOrNull()?.riskAssessment?.let { riskAssessment ->
                    item {
                        RiskAssessmentCard(riskAssessment = riskAssessment)
                    }
                }
                
                // Critical Insights
                val criticalInsights = analyticsResults.flatMap { it.insights }
                    .filter { it.severity == InsightSeverity.CRITICAL }
                
                if (criticalInsights.isNotEmpty()) {
                    item {
                        CriticalInsightsCard(insights = criticalInsights)
                    }
                }
                
                // High Priority Recommendations
                val highPriorityRecommendations = analyticsResults.flatMap { it.recommendations }
                    .filter { it.priority == RecommendationPriority.HIGH || it.priority == RecommendationPriority.URGENT }
                
                if (highPriorityRecommendations.isNotEmpty()) {
                    item {
                        RecommendationsCard(
                            title = "High Priority Recommendations",
                            recommendations = highPriorityRecommendations
                        )
                    }
                }
                
                // All Insights by Category
                val allInsights = analyticsResults.flatMap { it.insights }
                    .filterNot { it.severity == InsightSeverity.CRITICAL }
                    .groupBy { it.severity }
                
                allInsights.forEach { (severity, insights) ->
                    item {
                        InsightsCard(
                            title = "${severity.name.lowercase().replaceFirstChar { it.uppercase() }} Insights",
                            insights = insights,
                            severity = severity
                        )
                    }
                }
                
                // All Recommendations by Category
                val allRecommendations = analyticsResults.flatMap { it.recommendations }
                    .filterNot { it.priority == RecommendationPriority.HIGH || it.priority == RecommendationPriority.URGENT }
                    .groupBy { it.category }
                
                allRecommendations.forEach { (category, recommendations) ->
                    item {
                        RecommendationsCard(
                            title = "${category.name.lowercase().replaceFirstChar { it.uppercase() }} Recommendations",
                            recommendations = recommendations
                        )
                    }
                }
                
                // Visualizations
                val visualizations = analyticsResults.flatMap { it.visualizations }
                
                visualizations.forEach { visualization ->
                    item {
                        VisualizationCard(visualization = visualization)
                    }
                }
            }
        }
    }
}

@Composable
fun TimeRangeSelector(
    selectedRange: AnalyticsTimeRange,
    onRangeSelected: (AnalyticsTimeRange) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedRange.displayName,
            onValueChange = { },
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AnalyticsTimeRange.values().forEach { range ->
                DropdownMenuItem(
                    text = { Text(range.displayName) },
                    onClick = {
                        onRangeSelected(range)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RiskAssessmentCard(riskAssessment: RiskAssessment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                riskAssessment.overallRisk >= 0.7 -> MaterialTheme.colorScheme.errorContainer
                riskAssessment.overallRisk >= 0.4 -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        riskAssessment.overallRisk >= 0.7 -> Icons.Default.Warning
                        riskAssessment.overallRisk >= 0.4 -> Icons.Default.Info
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = when {
                        riskAssessment.overallRisk >= 0.7 -> MaterialTheme.colorScheme.error
                        riskAssessment.overallRisk >= 0.4 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Risk Assessment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "${(riskAssessment.overallRisk * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { riskAssessment.overallRisk.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    riskAssessment.overallRisk >= 0.7 -> MaterialTheme.colorScheme.error
                    riskAssessment.overallRisk >= 0.4 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
            
            if (riskAssessment.riskFactors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Risk Factors:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                
                riskAssessment.riskFactors.forEach { factor ->
                    Text(
                        text = "• ${factor.factor}: ${factor.description}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CriticalInsightsCard(insights: List<Insight>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Critical Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            insights.forEach { insight ->
                InsightItem(insight = insight)
                if (insight != insights.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun InsightsCard(
    title: String,
    insights: List<Insight>,
    severity: InsightSeverity
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
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            insights.forEach { insight ->
                InsightItem(insight = insight)
                if (insight != insights.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun InsightItem(insight: Insight) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = when (insight.severity) {
                InsightSeverity.CRITICAL -> Icons.Default.Warning
                InsightSeverity.HIGH -> Icons.Default.Info
                InsightSeverity.MEDIUM -> Icons.Default.Lightbulb
                InsightSeverity.LOW -> Icons.Default.TipsAndUpdates
            },
            contentDescription = null,
            tint = when (insight.severity) {
                InsightSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                InsightSeverity.HIGH -> MaterialTheme.colorScheme.tertiary
                InsightSeverity.MEDIUM -> MaterialTheme.colorScheme.primary
                InsightSeverity.LOW -> MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = insight.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Confidence: ${(insight.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun RecommendationsCard(
    title: String,
    recommendations: List<Recommendation>
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
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            recommendations.forEach { recommendation ->
                RecommendationItem(recommendation = recommendation)
                if (recommendation != recommendations.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun RecommendationItem(recommendation: Recommendation) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = when (recommendation.priority) {
                RecommendationPriority.URGENT -> Icons.Default.PriorityHigh
                RecommendationPriority.HIGH -> Icons.Default.ArrowUpward
                RecommendationPriority.MEDIUM -> Icons.Default.ArrowForward
                RecommendationPriority.LOW -> Icons.Default.ArrowDownward
            },
            contentDescription = null,
            tint = when (recommendation.priority) {
                RecommendationPriority.URGENT -> MaterialTheme.colorScheme.error
                RecommendationPriority.HIGH -> MaterialTheme.colorScheme.tertiary
                RecommendationPriority.MEDIUM -> MaterialTheme.colorScheme.primary
                RecommendationPriority.LOW -> MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (recommendation.actionable) {
                Text(
                    text = "• Actionable",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun VisualizationCard(visualization: VisualizationData) {
    AdvancedVisualization(
        data = visualization,
        modifier = Modifier.fillMaxWidth()
    )
}