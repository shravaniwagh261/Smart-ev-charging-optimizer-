package com.example.domain.optimizer

import com.example.data.model.AlertLevel
import com.example.data.model.EVTransaction
import com.example.data.model.GridState
import com.example.data.model.ScheduleSummary
import com.example.data.model.ScheduledEV
import com.example.data.model.StationAlert
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object EVOptimizer {

    fun calculatePriorityScore(ev: EVTransaction, gridLoadPct: Float): Float {
        val batteryUrgency = ((100.0f - ev.batteryPct) / 100.0f).coerceIn(0.0f, 1.0f)
        val stayHours = max(0.2f, ev.stayDurationHours)
        val departureUrgency = (1.2f / (stayHours + 0.2f)).coerceIn(0.0f, 1.0f)
        val energyRatio = (ev.energyRequiredKwh / max(20.0f, ev.batteryCapacityKwh)).coerceIn(0.0f, 1.0f)

        val wBattery = 45.0f
        val wDeparture = 35.0f
        val wEnergy = 20.0f

        var score = (batteryUrgency * wBattery) + (departureUrgency * wDeparture) + (energyRatio * wEnergy)

        if (gridLoadPct > 80.0f) {
            score *= 0.95f
        }

        return (score * 10.0f).roundToInt() / 10.0f
    }

    fun optimizeSchedule(
        evList: List<EVTransaction>,
        gridState: GridState,
        baseTime: Date = Date()
    ): Pair<List<ScheduledEV>, ScheduleSummary> {
        val availableCap = gridState.availableGridPowerKw
        val totalChargers = gridState.totalChargers
        val gridLoad = gridState.currentGridLoadPct

        // Calculate priorities and sort descending
        val evWithPriority = evList.map { ev ->
            val priority = calculatePriorityScore(ev, gridLoad)
            ev to priority
        }.sortedByDescending { it.second }

        var allocatedPowerSum = 0.0f
        var assignedChargers = 0
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val scheduled = mutableListOf<ScheduledEV>()

        for ((ev, priority) in evWithPriority) {
            val requestedPower = ev.maxChargingPowerKw
            val hasChargerSlot = assignedChargers < totalChargers
            val hasPowerHeadroom = (allocatedPowerSum + 10.0f) <= availableCap

            if (hasChargerSlot && hasPowerHeadroom) {
                assignedChargers++
                val powerToGive = min(requestedPower, availableCap - allocatedPowerSum)
                allocatedPowerSum += powerToGive

                // Physics duration: Energy / (Power * efficiency ~92%)
                // Non-linear taper overhead for high target SOC
                val taper = 1.0f + (max(0.0f, ev.targetBatteryPct - 80.0f) / 40.0f) * 0.35f
                val durationHours = (ev.energyRequiredKwh / (max(5.0f, powerToGive) * 0.92f)) * taper
                val durationMinutes = max(15, (durationHours * 60.0f).toInt())

                val startCal = Calendar.getInstance().apply { time = baseTime }
                val startStr = timeFormat.format(startCal.time)
                startCal.add(Calendar.MINUTE, durationMinutes)
                val compStr = timeFormat.format(startCal.time)

                scheduled.add(
                    ScheduledEV(
                        ev = ev.copy(chargingStatus = "Charging"),
                        assignedCharger = "Bay #$assignedChargers",
                        allocatedPowerKw = ((powerToGive * 10.0f).roundToInt() / 10.0f),
                        startTime = startStr,
                        estCompletionTime = compStr,
                        estDurationMin = durationMinutes,
                        priorityScore = priority,
                        scheduleStatus = "Charging"
                    )
                )
            } else {
                // Queued
                val taper = 1.0f + (max(0.0f, ev.targetBatteryPct - 80.0f) / 40.0f) * 0.35f
                val durationHours = (ev.energyRequiredKwh / (requestedPower * 0.92f)) * taper
                val durationMinutes = max(15, (durationHours * 60.0f).toInt())

                scheduled.add(
                    ScheduledEV(
                        ev = ev.copy(chargingStatus = "Queued"),
                        assignedCharger = "Waiting Bay",
                        allocatedPowerKw = 0.0f,
                        startTime = "Deferred",
                        estCompletionTime = "+${durationMinutes}m upon slot",
                        estDurationMin = durationMinutes,
                        priorityScore = priority,
                        scheduleStatus = "Queued"
                    )
                )
            }
        }

        val powerUtilization = ((allocatedPowerSum / max(1.0f, availableCap)) * 100.0f).coerceIn(0.0f, 100.0f)
        val headroom = max(0.0f, availableCap - allocatedPowerSum)

        val summary = ScheduleSummary(
            totalActiveCharging = assignedChargers,
            totalQueued = evList.size - assignedChargers,
            allocatedPowerKw = (allocatedPowerSum * 10.0f).roundToInt() / 10.0f,
            stationHeadroomKw = (headroom * 10.0f).roundToInt() / 10.0f,
            powerUtilizationPct = (powerUtilization * 10.0f).roundToInt() / 10.0f,
            availableChargersRemaining = totalChargers - assignedChargers
        )

        return Pair(scheduled, summary)
    }

    fun generateAlerts(
        scheduled: List<ScheduledEV>,
        gridState: GridState,
        summary: ScheduleSummary
    ): List<StationAlert> {
        val alerts = mutableListOf<StationAlert>()

        // 1. Grid stress
        if (gridState.currentGridLoadPct >= 85.0f) {
            alerts.add(
                StationAlert(
                    level = AlertLevel.CRITICAL,
                    title = "Severe Grid Overload Alert",
                    message = "Grid load is ${gridState.currentGridLoadPct}%. Station capacity throttled by ${(100.0f - gridState.gridThrottleFactor * 100.0f).roundToInt()}% to prevent blackout penalties."
                )
            )
        } else if (gridState.currentGridLoadPct >= 75.0f) {
            alerts.add(
                StationAlert(
                    level = AlertLevel.WARNING,
                    title = "High Regional Grid Demand",
                    message = "Grid load at ${gridState.currentGridLoadPct}%. Peak time-of-use tariffs active. Priority dispatch enforced."
                )
            )
        }

        // 2. Insufficient station headroom
        if (summary.stationHeadroomKw < 15.0f && summary.totalActiveCharging > 0) {
            alerts.add(
                StationAlert(
                    level = AlertLevel.WARNING,
                    title = "Station Power Cap Saturated",
                    message = "Operating at ${summary.allocatedPowerKw} kW out of ${gridState.availableGridPowerKw} kW available headroom."
                )
            )
        }

        // 3. Imminent departure for queued EVs
        for (item in scheduled) {
            if (item.scheduleStatus == "Queued" && item.ev.stayDurationHours <= 1.0f) {
                alerts.add(
                    StationAlert(
                        level = AlertLevel.CRITICAL,
                        title = "Departure Warning: ${item.ev.evId}",
                        message = "${item.ev.evId} (${item.ev.modelName}) leaves in ${(item.ev.stayDurationHours * 60).toInt()}m but is waiting in queue."
                    )
                )
            }
        }

        // 4. Charger Queue Congestion
        if (summary.totalQueued >= 3) {
            alerts.add(
                StationAlert(
                    level = AlertLevel.INFO,
                    title = "Charging Queue Backlog",
                    message = "${summary.totalQueued} electric vehicles waiting for available charging ports."
                )
            )
        }

        if (alerts.isEmpty()) {
            alerts.add(
                StationAlert(
                    level = AlertLevel.NORMAL,
                    title = "Grid & Station Operations Nominal",
                    message = "All active bays charging within optimal thermal and power limits. Headroom is safe."
                )
            )
        }

        return alerts
    }
}
