package com.example.data.model

data class EVTransaction(
    val evId: String,
    val modelName: String,
    val batteryPct: Float,
    val targetBatteryPct: Float,
    val batteryCapacityKwh: Float,
    val arrivalTime: String,
    val departureTime: String,
    val stayDurationHours: Float,
    val energyRequiredKwh: Float,
    val maxChargingPowerKw: Float,
    val voltageV: Float,
    val currentA: Float,
    val chargingStatus: String = "Queued"
)

data class ScheduledEV(
    val ev: EVTransaction,
    val assignedCharger: String,
    val allocatedPowerKw: Float,
    val startTime: String,
    val estCompletionTime: String,
    val estDurationMin: Int,
    val priorityScore: Float,
    val scheduleStatus: String // "Charging" or "Queued"
)

data class GridState(
    val stationPowerCapacityKw: Float,
    val currentGridLoadPct: Float,
    val gridThrottleFactor: Float,
    val availableGridPowerKw: Float,
    val totalChargers: Int
)

enum class AlertLevel {
    CRITICAL,
    WARNING,
    INFO,
    NORMAL
}

data class StationAlert(
    val level: AlertLevel,
    val title: String,
    val message: String
)

data class MLSampleComparison(
    val evId: String,
    val actualMin: Float,
    val predictedMin: Float,
    val errorMin: Float
)

data class MLMetrics(
    val mae: Float,
    val rmse: Float,
    val r2: Float,
    val samples: List<MLSampleComparison>
)

data class ScheduleSummary(
    val totalActiveCharging: Int,
    val totalQueued: Int,
    val allocatedPowerKw: Float,
    val stationHeadroomKw: Float,
    val powerUtilizationPct: Float,
    val availableChargersRemaining: Int
)
