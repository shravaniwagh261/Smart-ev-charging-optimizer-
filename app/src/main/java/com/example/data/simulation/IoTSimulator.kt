package com.example.data.simulation

import com.example.data.model.EVTransaction
import com.example.data.model.GridState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object IoTSimulator {

    data class EVPreset(
        val model: String,
        val capacity: Float,
        val maxDcKw: Float
    )

    private val PRESETS = listOf(
        EVPreset("Tesla Model 3", 60.0f, 170.0f),
        EVPreset("Tesla Model Y", 75.0f, 210.0f),
        EVPreset("Hyundai Ioniq 5", 77.4f, 220.0f),
        EVPreset("Kia EV6", 77.4f, 230.0f),
        EVPreset("Ford Mustang Mach-E", 88.0f, 150.0f),
        EVPreset("Porsche Taycan", 93.4f, 270.0f),
        EVPreset("Volkswagen ID.4", 77.0f, 135.0f),
        EVPreset("Nissan Leaf", 40.0f, 50.0f),
        EVPreset("BYD Atto 3", 60.5f, 88.0f),
        EVPreset("BMW i4 eDrive40", 83.9f, 205.0f)
    )

    fun generateEVs(
        numEVs: Int = 8,
        maxChargerPower: Float = 100.0f,
        baseDate: Date = Date()
    ): List<EVTransaction> {
        val list = mutableListOf<EVTransaction>()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        for (i in 1..numEVs) {
            val preset = PRESETS[Random.nextInt(PRESETS.size)]
            val initialSoc = (Random.nextFloat() * 45.0f + 12.0f).roundTo(1)
            val targetSoc = listOf(80.0f, 85.0f, 90.0f, 95.0f, 100.0f).random()
            val effectiveTarget = if (targetSoc <= initialSoc) min(100.0f, initialSoc + 30.0f) else targetSoc

            val energyRequired = (((effectiveTarget - initialSoc) / 100.0f) * preset.capacity).roundTo(2)

            // Arrival and departure times
            val cal = Calendar.getInstance().apply { time = baseDate }
            cal.add(Calendar.MINUTE, Random.nextInt(-45, 15))
            val arrivalStr = timeFormat.format(cal.time)

            val stayHours = (Random.nextFloat() * 3.5f + 1.2f).roundTo(2)
            cal.add(Calendar.MINUTE, (stayHours * 60).toInt())
            val departureStr = timeFormat.format(cal.time)

            val supportedPower = min(maxChargerPower, preset.maxDcKw)
            val nominalVoltage = (Random.nextFloat() * 25.0f + 390.0f).roundTo(1)
            val nominalCurrent = ((supportedPower * 1000.0f) / nominalVoltage).roundTo(1)

            list.add(
                EVTransaction(
                    evId = "EV-${100 + i}",
                    modelName = preset.model,
                    batteryPct = initialSoc,
                    targetBatteryPct = effectiveTarget,
                    batteryCapacityKwh = preset.capacity,
                    arrivalTime = arrivalStr,
                    departureTime = departureStr,
                    stayDurationHours = stayHours,
                    energyRequiredKwh = energyRequired,
                    maxChargingPowerKw = supportedPower,
                    voltageV = nominalVoltage,
                    currentA = nominalCurrent,
                    chargingStatus = "Queued"
                )
            )
        }
        return list
    }

    fun computeGridState(
        stationCapacityKw: Float,
        baseGridLoadPct: Float,
        totalChargers: Int
    ): GridState {
        // Grid throttle: above 60% load, grid operator limits peak station intake
        val throttlePct = max(0.0f, baseGridLoadPct - 60.0f) / 100.0f
        val throttleFactor = (1.0f - (throttlePct * 0.75f)).coerceIn(0.25f, 1.0f).roundTo(2)
        val availablePower = (stationCapacityKw * throttleFactor).roundTo(1)

        return GridState(
            stationPowerCapacityKw = stationCapacityKw,
            currentGridLoadPct = baseGridLoadPct.roundTo(1),
            gridThrottleFactor = throttleFactor,
            availableGridPowerKw = availablePower,
            totalChargers = totalChargers
        )
    }

    private fun Float.roundTo(decimals: Int): Float {
        var multiplier = 1.0f
        repeat(decimals) { multiplier *= 10.0f }
        return Math.round(this * multiplier) / multiplier
    }
}
