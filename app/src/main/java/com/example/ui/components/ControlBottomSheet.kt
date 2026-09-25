package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.EnergyAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlBottomSheet(
    numEVs: Int,
    stationCapacityKw: Float,
    numChargers: Int,
    gridLoadPct: Float,
    maxChargerPower: Float,
    onNumEVsChange: (Int) -> Unit,
    onStationCapacityChange: (Float) -> Unit,
    onNumChargersChange: (Int) -> Unit,
    onGridLoadChange: (Float) -> Unit,
    onMaxChargerPowerChange: (Float) -> Unit,
    onGenerateData: () -> Unit,
    onRetrainModel: () -> Unit,
    onOptimizeSchedule: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = ElectricEmerald,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Station & Grid Simulation Controls",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Slider 1: Number of Connected EVs
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Number of EVs: $numEVs", fontWeight = FontWeight.SemiBold)
                    Text("3 - 16 vehicles", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Slider(
                    value = numEVs.toFloat(),
                    onValueChange = { onNumEVsChange(it.toInt()) },
                    valueRange = 3f..16f,
                    steps = 12,
                    colors = SliderDefaults.colors(thumbColor = ElectricEmerald, activeTrackColor = ElectricEmerald),
                    modifier = Modifier.testTag("slider_num_evs")
                )
            }

            // Slider 2: Station Power Capacity (kW)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Station Power Capacity: ${stationCapacityKw.toInt()} kW", fontWeight = FontWeight.SemiBold)
                    Text("100 - 500 kW", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Slider(
                    value = stationCapacityKw,
                    onValueChange = onStationCapacityChange,
                    valueRange = 100f..500f,
                    steps = 15,
                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                    modifier = Modifier.testTag("slider_station_capacity")
                )
            }

            // Slider 3: Available Charger Bays
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Available Charger Bays: $numChargers", fontWeight = FontWeight.SemiBold)
                    Text("2 - 10 bays", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Slider(
                    value = numChargers.toFloat(),
                    onValueChange = { onNumChargersChange(it.toInt()) },
                    valueRange = 2f..10f,
                    steps = 7,
                    colors = SliderDefaults.colors(thumbColor = ElectricEmerald, activeTrackColor = ElectricEmerald),
                    modifier = Modifier.testTag("slider_num_chargers")
                )
            }

            // Slider 4: Regional Grid Load (%)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current Grid Load: ${gridLoadPct.toInt()}%", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (gridLoadPct >= 80f) "Critical" else if (gridLoadPct >= 65f) "Warning" else "Normal",
                        color = if (gridLoadPct >= 80f) AlertRed else if (gridLoadPct >= 65f) EnergyAmber else ElectricEmerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Slider(
                    value = gridLoadPct,
                    onValueChange = onGridLoadChange,
                    valueRange = 20f..95f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (gridLoadPct >= 80f) AlertRed else EnergyAmber,
                        activeTrackColor = if (gridLoadPct >= 80f) AlertRed else EnergyAmber
                    ),
                    modifier = Modifier.testTag("slider_grid_load")
                )
            }

            // Slider 5: Max Charger Power
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Max Charger Power: ${maxChargerPower.toInt()} kW", fontWeight = FontWeight.SemiBold)
                    Text("Fast DC Charger Cap", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Slider(
                    value = maxChargerPower,
                    onValueChange = onMaxChargerPowerChange,
                    valueRange = 22f..250f,
                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                    modifier = Modifier.testTag("slider_max_charger_power")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onGenerateData()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_generate_ev_data_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate EV Data", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onRetrainModel()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_retrain_ai_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retrain AI Model", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onOptimizeSchedule()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_optimize_schedule_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF00381E), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Optimize Schedule", color = Color(0xFF00381E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onReset()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_reset_simulation_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Simulation", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
