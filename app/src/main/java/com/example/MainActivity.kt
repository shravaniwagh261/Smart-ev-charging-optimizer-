package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AlertsView
import com.example.ui.components.ControlBottomSheet
import com.example.ui.components.GridAnalyticsView
import com.example.ui.components.InternshipVivaView
import com.example.ui.components.IoTSimulatorView
import com.example.ui.components.LiveTariffSearchView
import com.example.ui.components.MLPredictionView
import com.example.ui.components.PriorityChartView
import com.example.ui.components.ScheduleTableView
import com.example.ui.components.StationHeroBanner
import com.example.ui.components.TopMetricDashboardCards
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.EnergyAmber
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.EVStationViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                EVStationApp()
            }
        }
    }
}

data class NavTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EVStationApp(
    viewModel: EVStationViewModel = viewModel()
) {
    val numEVs by viewModel.numEVs.collectAsStateWithLifecycle()
    val stationCapacityKw by viewModel.stationCapacityKw.collectAsStateWithLifecycle()
    val numChargers by viewModel.numChargers.collectAsStateWithLifecycle()
    val gridLoadPct by viewModel.gridLoadPct.collectAsStateWithLifecycle()
    val maxChargerPower by viewModel.maxChargerPower.collectAsStateWithLifecycle()

    val evList by viewModel.evList.collectAsStateWithLifecycle()
    val gridState by viewModel.gridState.collectAsStateWithLifecycle()
    val scheduledEVs by viewModel.scheduledEVs.collectAsStateWithLifecycle()
    val scheduleSummary by viewModel.scheduleSummary.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val mlMetrics by viewModel.mlMetrics.collectAsStateWithLifecycle()
    val isModelRetrained by viewModel.isModelRetrained.collectAsStateWithLifecycle()

    val isSpeakingAudio by viewModel.isSpeakingAudio.collectAsStateWithLifecycle()
    val ttsStatusText by viewModel.ttsMessage.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchResult by viewModel.groundingResult.collectAsStateWithLifecycle()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    var showControlSheet by remember { mutableStateOf(false) }

    val tabs = listOf(
        NavTab("Overview", Icons.Default.Dashboard, "tab_overview"),
        NavTab("IoT Stream", Icons.Default.Sensors, "tab_iot"),
        NavTab("AI Priority", Icons.Default.BarChart, "tab_priority"),
        NavTab("ML Predict", Icons.Default.Psychology, "tab_ml"),
        NavTab("Analytics", Icons.Default.Insights, "tab_analytics"),
        NavTab("Tariff AI", Icons.Default.Language, "tab_tariff"),
        NavTab("Viva Prep", Icons.Default.School, "tab_viva")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(ElectricEmerald.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = ElectricEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "EV Optimizer",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Edge AI • Smart Charging Grid",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                    }
                },
                actions = {
                    // Audio Briefing Action
                    IconButton(
                        onClick = {
                            if (isSpeakingAudio) viewModel.stopSpeaking() else viewModel.speakStationStatusBriefing()
                        },
                        modifier = Modifier.testTag("appbar_audio_briefing_btn")
                    ) {
                        Icon(
                            imageVector = if (isSpeakingAudio) Icons.Default.Stop else Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Broadcast",
                            tint = if (isSpeakingAudio) AlertRed else ElectricEmerald
                        )
                    }

                    // Control settings button
                    IconButton(
                        onClick = { showControlSheet = true },
                        modifier = Modifier.testTag("appbar_controls_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Station Controls",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showControlSheet = true },
                containerColor = ElectricEmerald,
                contentColor = Color(0xFF00381E),
                modifier = Modifier.testTag("fab_open_controls")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Open Station Configuration"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Audio status notification bar
            AnimatedVisibility(visible = isSpeakingAudio || ttsStatusText != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkNavyBackground)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ttsStatusText ?: "Broadcasting station audio...",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.stopSpeaking() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = AlertRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Scrollable Tab Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.setSelectedTab(index) },
                        modifier = Modifier.testTag(tab.testTag),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == index) ElectricEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) ElectricEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }

            // Content Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> { // Overview Tab
                        item {
                            StationHeroBanner(
                                isSpeakingAudio = isSpeakingAudio,
                                ttsStatusText = ttsStatusText,
                                onPlayAudioBriefing = { viewModel.speakStationStatusBriefing() },
                                onStopAudio = { viewModel.stopSpeaking() },
                                onOpenControls = { showControlSheet = true }
                            )
                        }

                        item {
                            val totalEnergy = evList.sumOf { it.energyRequiredKwh.toDouble() }.toFloat()
                            val peakLoad = minOf(gridState.stationPowerCapacityKw, scheduleSummary.allocatedPowerKw * 1.08f)
                            TopMetricDashboardCards(
                                gridState = gridState,
                                scheduleSummary = scheduleSummary,
                                totalEnergyRequiredKwh = totalEnergy,
                                predictedPeakLoadKw = peakLoad
                            )
                        }

                        item {
                            AlertsView(alerts = alerts)
                        }

                        item {
                            ScheduleTableView(
                                scheduledEVs = scheduledEVs,
                                gridState = gridState,
                                scheduleSummary = scheduleSummary,
                                onRunOptimization = { viewModel.recomputeGridAndSchedule() }
                            )
                        }
                    }

                    1 -> { // Live IoT Stream Tab
                        item {
                            IoTSimulatorView(
                                evList = evList,
                                onGenerateData = { viewModel.generateEVData() }
                            )
                        }
                    }

                    2 -> { // AI Priority Tab
                        item {
                            PriorityChartView(scheduledEVs = scheduledEVs)
                        }
                    }

                    3 -> { // ML Duration Prediction Tab
                        item {
                            MLPredictionView(
                                metrics = mlMetrics,
                                isRetrained = isModelRetrained,
                                onRetrainModel = { viewModel.retrainAIModel() }
                            )
                        }
                    }

                    4 -> { // Grid Load Analytics Tab
                        item {
                            GridAnalyticsView(
                                gridState = gridState,
                                scheduledEVs = scheduledEVs
                            )
                        }
                    }

                    5 -> { // Live Search Grounding Tab
                        item {
                            LiveTariffSearchView(
                                isSearching = isSearching,
                                searchResult = searchResult,
                                onSearchQuery = { query ->
                                    viewModel.searchGridTariffsOrEVData(query)
                                }
                            )
                        }
                    }

                    6 -> { // Viva Exam Defense Tab
                        item {
                            InternshipVivaView()
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp)) // padding for FAB
                }
            }
        }
    }

    if (showControlSheet) {
        ControlBottomSheet(
            numEVs = numEVs,
            stationCapacityKw = stationCapacityKw,
            numChargers = numChargers,
            gridLoadPct = gridLoadPct,
            maxChargerPower = maxChargerPower,
            onNumEVsChange = { viewModel.updateNumEVs(it) },
            onStationCapacityChange = { viewModel.updateStationCapacity(it) },
            onNumChargersChange = { viewModel.updateNumChargers(it) },
            onGridLoadChange = { viewModel.updateGridLoad(it) },
            onMaxChargerPowerChange = { viewModel.updateMaxChargerPower(it) },
            onGenerateData = { viewModel.generateEVData() },
            onRetrainModel = { viewModel.retrainAIModel() },
            onOptimizeSchedule = { viewModel.recomputeGridAndSchedule() },
            onReset = { viewModel.resetSimulation() },
            onDismiss = { showControlSheet = false }
        )
    }
}
