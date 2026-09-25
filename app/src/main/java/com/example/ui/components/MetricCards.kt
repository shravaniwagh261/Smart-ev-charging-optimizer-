package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GridState
import com.example.data.model.ScheduleSummary
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.EnergyAmber

@Composable
fun TopMetricDashboardCards(
    gridState: GridState,
    scheduleSummary: ScheduleSummary,
    totalEnergyRequiredKwh: Float,
    predictedPeakLoadKw: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Power & Grid Load
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCardItem(
                title = "Available Power",
                value = "${gridState.availableGridPowerKw.toInt()} kW",
                subtext = "Cap: ${gridState.stationPowerCapacityKw.toInt()} kW",
                icon = Icons.Default.ElectricBolt,
                accentColor = ElectricEmerald,
                testTag = "metric_available_power",
                modifier = Modifier.weight(1f)
            )

            val gridColor = if (gridState.currentGridLoadPct >= 80f) AlertRed else if (gridState.currentGridLoadPct >= 65f) EnergyAmber else ElectricCyan
            MetricCardItem(
                title = "Current Grid Load",
                value = "${gridState.currentGridLoadPct.toInt()}%",
                subtext = if (gridState.currentGridLoadPct >= 80f) "Severe Stress" else "Normal Flow",
                icon = Icons.Default.Speed,
                accentColor = gridColor,
                testTag = "metric_grid_load",
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Active EVs & Available Chargers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCardItem(
                title = "Active EVs",
                value = "${scheduleSummary.totalActiveCharging}",
                subtext = "Queued: ${scheduleSummary.totalQueued}",
                icon = Icons.Default.EvStation,
                accentColor = ElectricCyan,
                testTag = "metric_active_evs",
                modifier = Modifier.weight(1f)
            )

            MetricCardItem(
                title = "Available Chargers",
                value = "${scheduleSummary.availableChargersRemaining} / ${gridState.totalChargers}",
                subtext = "Bays Ready",
                icon = Icons.Default.BatteryChargingFull,
                accentColor = ElectricEmerald,
                testTag = "metric_available_chargers",
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Total Energy & Predicted Peak
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCardItem(
                title = "Total Energy Req.",
                value = "${totalEnergyRequiredKwh.toInt()} kWh",
                subtext = "All Vehicles",
                icon = Icons.Default.Timeline,
                accentColor = EnergyAmber,
                testTag = "metric_total_energy",
                modifier = Modifier.weight(1f)
            )

            val peakOver = predictedPeakLoadKw > gridState.availableGridPowerKw
            MetricCardItem(
                title = "Predicted Peak Load",
                value = "${predictedPeakLoadKw.toInt()} kW",
                subtext = if (peakOver) "Constrained by AI" else "Within Limit",
                icon = Icons.Default.Warning,
                accentColor = if (peakOver) EnergyAmber else ElectricEmerald,
                testTag = "metric_predicted_peak",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCardItem(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
