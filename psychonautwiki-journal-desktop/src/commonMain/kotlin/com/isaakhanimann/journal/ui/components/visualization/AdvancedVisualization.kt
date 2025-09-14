package com.isaakhanimann.journal.ui.components.visualization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.plugin.VisualizationData
import com.isaakhanimann.journal.plugin.VisualizationType
import kotlin.math.*

@Composable
fun AdvancedVisualization(
    data: VisualizationData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            data.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (data.type) {
                VisualizationType.LINE_CHART -> LineChart(data = data.data)
                VisualizationType.BAR_CHART -> BarChart(data = data.data)
                VisualizationType.GAUGE -> GaugeChart(data = data.data)
                else -> PlaceholderVisualization(data.type.name)
            }
        }
    }
}

@Composable
fun LineChart(
    data: Map<String, Any>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    // Simple sample data
    val dataPoints = listOf(
        0f to 10f, 1f to 15f, 2f to 12f, 3f to 20f, 4f to 18f, 5f to 25f
    )
    
    val maxX = 5f
    val maxY = 25f
    val minY = 0f
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(surfaceColor)
    ) {
        val padding = 40.dp.toPx()
        val chartWidth = size.width - 2 * padding
        val chartHeight = size.height - 2 * padding
        
        // Draw grid lines
        for (i in 0..5) {
            val y = padding + (chartHeight / 5) * i
            drawLine(
                color = onSurfaceColor.copy(alpha = 0.1f),
                start = Offset(padding, y),
                end = Offset(size.width - padding, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Convert data points to screen coordinates
        val screenPoints = dataPoints.map { (x, y) ->
            val screenX = padding + (x / maxX) * chartWidth
            val screenY = padding + chartHeight - ((y - minY) / (maxY - minY)) * chartHeight
            Offset(screenX, screenY)
        }
        
        // Draw line segments
        for (i in 0 until screenPoints.size - 1) {
            drawLine(
                color = primaryColor,
                start = screenPoints[i],
                end = screenPoints[i + 1],
                strokeWidth = 3.dp.toPx()
            )
        }
        
        // Draw data points
        screenPoints.forEach { point ->
            drawCircle(
                color = primaryColor,
                radius = 4.dp.toPx(),
                center = point
            )
        }
    }
}

@Composable
fun BarChart(
    data: Map<String, Any>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // Simple sample data
    val barData = mapOf(
        "LSD" to 15f,
        "Psilocybin" to 25f,
        "MDMA" to 10f,
        "DMT" to 5f,
        "Cannabis" to 30f
    )
    
    val maxValue = 30f
    
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val padding = 20.dp.toPx()
            val chartWidth = size.width - 2 * padding
            val chartHeight = size.height - 2 * padding
            val barWidth = chartWidth / barData.size
            
            barData.entries.forEachIndexed { index, (_, value) ->
                val barHeight = (value / maxValue) * chartHeight
                val x = padding + index * barWidth + barWidth * 0.1f
                val width = barWidth * 0.8f
                val y = size.height - padding - barHeight
                
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(x, y),
                    size = Size(width, barHeight)
                )
            }
        }
        
        // Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            barData.keys.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GaugeChart(
    data: Map<String, Any>,
    modifier: Modifier = Modifier
) {
    val value = data["value"] as? Float ?: 0.7f
    val maxValue = data["maxValue"] as? Float ?: 1f
    val label = data["label"] as? String ?: "Risk Level"
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val warningColor = Color(0xFFFF9800) // Orange
    
    val gaugeColor = when {
        value >= 0.7f -> errorColor
        value >= 0.4f -> warningColor
        else -> primaryColor
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        val colorScheme = MaterialTheme.colorScheme
        Canvas(
            modifier = Modifier.size(120.dp)
        ) {
            val strokeWidth = 12.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            // Background arc
            drawArc(
                color = colorScheme.surfaceVariant,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )
            
            // Value arc
            val sweepAngle = (value / maxValue) * 270f
            drawArc(
                color = gaugeColor,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = gaugeColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlaceholderVisualization(
    typeName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$typeName Visualization\n(Implementation pending)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}