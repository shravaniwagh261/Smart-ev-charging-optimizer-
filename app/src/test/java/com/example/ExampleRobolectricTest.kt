package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.simulation.IoTSimulator
import com.example.domain.optimizer.EVOptimizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("EV Optimizer", appName)
  }

  @Test
  fun `optimizer strictly respects station capacity constraint`() {
    val evs = IoTSimulator.generateEVs(numEVs = 10, maxChargerPower = 100.0f)
    val gridState = IoTSimulator.computeGridState(
      stationCapacityKw = 200.0f,
      baseGridLoadPct = 75.0f,
      totalChargers = 6
    )

    val (scheduled, summary) = EVOptimizer.optimizeSchedule(evs, gridState)

    // Verify allocated power never exceeds available grid power limit
    assertTrue(
      "Allocated power (${summary.allocatedPowerKw} kW) exceeded available grid cap (${gridState.availableGridPowerKw} kW)",
      summary.allocatedPowerKw <= gridState.availableGridPowerKw
    )

    // Verify active chargers never exceed physical bay count
    assertTrue(
      "Active chargers (${summary.totalActiveCharging}) exceeded bay capacity (${gridState.totalChargers})",
      summary.totalActiveCharging <= gridState.totalChargers
    )
  }
}
