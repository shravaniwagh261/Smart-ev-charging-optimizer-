package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MLMetrics
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.EnergyAmber
import kotlin.math.max

@Composable
fun MLPredictionView(
    metrics: MLMetrics,
    isRetrained: Boolean,
    onRetrainModel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ml_prediction_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = ElectricEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "E. ML Charging-Time Prediction",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onRetrainModel,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("train_retrain_ai_model_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retrain Model", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Model: Random Forest Regression (Synthetic Data). Captures CC-CV battery saturation taper curve above 80% SOC.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MLMetricBox(
                    label = "MAE",
                    value = "${metrics.mae} min",
                    accentColor = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
                MLMetricBox(
                    label = "RMSE",
                    value = "${metrics.rmse} min",
                    accentColor = EnergyAmber,
                    modifier = Modifier.weight(1f)
                )
                MLMetricBox(
                    label = "R² Score",
                    value = "${metrics.r2}",
                    accentColor = ElectricEmerald,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Actual vs. Predicted Duration (Minutes)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Actual vs Predicted Scatter Canvas
            ActualVsPredictedChart(samples = metrics.samples)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem(color = ElectricCyan, text = "Predicted Sample Points")
                LegendItem(color = AlertRed, text = "Ideal Linear Correlation (y=x)")
            }
        }
    }
}

@Composable
fun ActualVsPredictedChart(
    samples: List<com.example.data.model.MLSampleComparison>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val maxVal = max(
                80f,
                samples.maxOfOrNull { max(it.actualMin, it.predictedMin) } ?: 80f
            ) * 1.1f

            val w = size.width
            val h = size.height

            // Axes
            drawLine(
                color = Color.Gray.copy(alpha = 0.4f),
                start = Offset(0f, h),
                end = Offset(w, h),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.4f),
                start = Offset(0f, 0f),
                end = Offset(0f, h),
                strokeWidth = 2f
            )

            // Ideal Line (y = x)
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            drawLine(
                color = AlertRed.copy(alpha = 0.8f),
                start = Offset(0f, h),
                end = Offset(w, 0f),
                strokeWidth = 2f,
                pathEffect = pathEffect
            )

            // Scatter points
            samples.forEach { sample ->
                val x = (sample.actualMin / maxVal) * w
                val y = h - ((sample.predictedMin / maxVal) * h)

                drawCircle(
                    color = ElectricCyan,
                    radius = 5.dp.toPx(),
                    center = Offset(x.coerceIn(0f, w), y.coerceIn(0f, h))
                )
            }
        }
    }
}

@Composable
fun MLMetricBox(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }
    }
}
