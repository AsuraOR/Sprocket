package com.example.sprocket

import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.data.model.Part
import com.example.sprocket.data.model.ServiceRecord
import com.example.sprocket.data.model.VehicleState
import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleStateTest {

    @Test
    fun testDefaultThemePreferenceIsSystem() {
        val state = VehicleState()
        assertEquals("SYSTEM", state.themePreference)
    }

    @Test
    fun testServiceRecordBengkelPerformerAndCustomFields() {
        val record = ServiceRecord(
            id = "rec-custom-01",
            partId = "belt",
            year = 2026,
            month = 8,
            odometerKm = 39850,
            cost = 485000L,
            performer = "Bengkel",
            note = "Replaced at Bengkel Ahass - Gates Powerlink"
        )

        assertEquals("belt", record.partId)
        assertEquals("Bengkel", record.performer)
        assertEquals(485000L, record.cost)
        assertEquals(39850, record.odometerKm)
        assertEquals("Replaced at Bengkel Ahass - Gates Powerlink", record.note)
    }

    @Test
    fun testVehicleStateThemePreferencePersistence() {
        val stateDark = VehicleState(themePreference = "DARK")
        assertEquals("DARK", stateDark.themePreference)

        val stateLight = VehicleState(themePreference = "LIGHT")
        assertEquals("LIGHT", stateLight.themePreference)
    }

    @Test
    fun testPartLogReplacementStateUpdate() {
        val part = Part(
            id = "spark",
            name = "Spark plug",
            intervalKm = 8000,
            intervalMonths = 12,
            lastKm = 32000,
            lastYear = 2025,
            lastMonth = 1,
            standardCost = 35000L
        )

        val updated = part.copy(
            lastKm = 40000,
            lastYear = 2026,
            lastMonth = 9
        )

        assertEquals(40000, updated.lastKm)
        assertEquals(2026, updated.lastYear)
        assertEquals(9, updated.lastMonth)
    }
}
