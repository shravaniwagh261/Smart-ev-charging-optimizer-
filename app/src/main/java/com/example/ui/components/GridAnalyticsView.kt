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
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GridState
import com.example.data.model.ScheduledEV
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.EnergyAmber
import kotlin.math.max

@Composable
fun GridAnalyticsView(
    gridState: GridState,
    scheduledEVs: List<ScheduledEV>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section F: 24-Hour Grid Load Curve
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("grid_load_analytics_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = null,
                        tint = EnergyAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "F. 24-Hour Regional Grid Demand Profile",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "High stress peak occurs 17:00-21:00. The Edge AI throttles station capacity automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                GridLoadCurveCanvas(currentGridLoad = gridState.currentGridLoadPct)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("00:00 (Night Valley)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("12:00 (Midday)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("19:00 (Peak)", style = MaterialTheme.typography.labelSmall, color = AlertRed)
                    Text("23:59", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Section G: Energy Consumption Analytics
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("energy_consumption_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricMeter,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "G. Energy Consumption Distribution",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                EnergyBarDistributionCanvas(scheduledEVs = scheduledEVs)
            }
        }
    }
}

@Composable
fun GridLoadCurveCanvas(currentGridLoad: Float) {
    val curvePoints = listOf(
        35f, 30f, 28f, 25f, 27f, 34f, 48f, 62f, 75f, 78f, 82f, 85f,
        80f, 77f, 73f, 76f, 88f, 92f, 86f, 78f, 68f, 55f, 45f, 38f
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            val w = size.width
            val h = size.height
            val stepX = w / (curvePoints.size - 1)

            // Threshold line at 80%
            val thresholdY = h - (0.80f * h)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            drawLine(
                color = AlertRed.copy(alpha = 0.5f),
                start = Offset(0f, thresholdY),
                end = Offset(w, thresholdY),
                strokeWidth = 2f,
                pathEffect = dashEffect
            )

            // Curve Path
            val path = Path()
            curvePoints.forEachIndexed { index, pct ->
                val x = index * stepX
                val y = h - ((pct / 100f) * h)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = ElectricCyan,
                style = Stroke(width = 3.dp.toPx())
            )

            // Current Grid Load Indicator Line
            val currentY = h - ((currentGridLoad.coerceIn(0f, 100f) / 100f) * h)
            drawLine(
                color = EnergyAmber,
                start = Offset(0f, currentY),
                end = Offset(w, currentY),
                strokeWidth = 2.5f
            )
        }
    }
}

@Composable
fun EnergyBarDistributionCanvas(scheduledEVs: List<ScheduledEV>) {
    if (scheduledEVs.isEmpty()) return

    val maxEnergy = max(20f, scheduledEVs.maxOfOrNull { it.ev.energyRequiredKwh } ?: 50f) * 1.15f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            val w = size.width
            val h = size.height
            val n = scheduledEVs.size
            val barWidth = (w / (n * 1.6f)).coerceAtLeast(10f)
            val slotWidth = w / n

            scheduledEVs.forEachIndexed { i, item ->
                val energy = item.ev.energyRequiredKwh
                val barHeight = (energy / maxEnergy) * h
                val x = (i * slotWidth) + (slotWidth - barWidth) / 2
                val y = h - barHeight

                val isCharging = item.scheduleStatus == "Charging"
                drawRoundRect(
                    color = if (isCharging) ElectricEmerald else EnergyAmber,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}
