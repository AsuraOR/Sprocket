package com.example.sprocket

import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.data.model.Part
import com.example.sprocket.domain.WearDriver
import com.example.sprocket.domain.WearEngine
import com.example.sprocket.domain.WearStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearEngineTest {

    @Test
    fun testDistanceWearOverdue() {
        // CVT belt: 20,000 km interval, fitted at 16,900 km, current odo = 40,000 km -> used = 23,100 km (116% wear)
        val belt = Part(
            id = "belt",
            name = "CVT belt",
            intervalKm = 20000,
            intervalMonths = 48,
            lastKm = 16900,
            lastYear = 2023,
            lastMonth = 6,
            standardCost = 450000L
        )

        val calc = WearEngine.calculate(
            part = belt,
            currentOdometerKm = 40000,
            currentYear = 2026,
            currentMonth = 9
        )

        assertEquals(WearStatus.OVERDUE, calc.status)
        assertEquals(WearDriver.KM, calc.driver)
        assertEquals(23100, calc.usedKm)
        assertEquals("3.100 km over", calc.remainLabel)
    }

    @Test
    fun testAgeWearOverdue() {
        // Front tyre: 20,000 km / 60 mo, last done Jun 2021 (63 months ago), lastKm = 24,800, current = 40,000 (15,200 km = 76%)
        // Age is 63 / 60 = 105% (overdue on age!)
        val tyre = Part(
            id = "front",
            name = "Front tyre",
            intervalKm = 20000,
            intervalMonths = 60,
            lastKm = 24800,
            lastYear = 2021,
            lastMonth = 6,
            standardCost = 340000L
        )

        val calc = WearEngine.calculate(
            part = tyre,
            currentOdometerKm = 40000,
            currentYear = 2026,
            currentMonth = 9
        )

        assertEquals(WearStatus.OVERDUE, calc.status)
        assertEquals(WearDriver.AGE, calc.driver)
        assertEquals("3 mo over", calc.remainLabel)
    }

    @Test
    fun testHealthyPart() {
        // Rear tyre: 12,000 km / 60 mo, last done Jan 2025 at 29,100 km -> used = 10,900 km (90.8%)
        val rear = Part(
            id = "rear",
            name = "Rear tyre",
            intervalKm = 12000,
            intervalMonths = 60,
            lastKm = 29100,
            lastYear = 2025,
            lastMonth = 1,
            standardCost = 385000L
        )

        val calc = WearEngine.calculate(
            part = rear,
            currentOdometerKm = 40000,
            currentYear = 2026,
            currentMonth = 9
        )

        // 10900 / 12000 = 0.908 -> DUE_SOON (> 0.80)
        assertEquals(WearStatus.DUE_SOON, calc.status)
        assertEquals("1.100", calc.remainLabel)
    }

    @Test
    fun testCurrencyAndNumberFormatting() {
        assertEquals("Rp450.000", WearEngine.formatCurrency(450000L))
        assertEquals("40.000", WearEngine.formatNumber(40000))
        assertEquals("0", WearEngine.formatNumber(0))
        assertEquals("0", WearEngine.formatDistance(0, DistanceUnit.KM))
        assertEquals("1.100", WearEngine.formatDistance(1100, DistanceUnit.KM))
    }

    @Test
    fun testFreshZeroData() {
        val part = Part(
            id = "belt",
            name = "CVT belt",
            intervalKm = 20000,
            intervalMonths = 48,
            lastKm = 0,
            lastYear = 2026,
            lastMonth = 9,
            standardCost = 450000L
        )

        val calc = WearEngine.calculate(
            part = part,
            currentOdometerKm = 0,
            currentYear = 2026,
            currentMonth = 9
        )

        assertEquals(WearStatus.HEALTHY, calc.status)
        assertEquals(WearDriver.KM, calc.driver)
        assertEquals(0, calc.usedKm)
        assertEquals(0, calc.usedMonths)
        assertEquals(0f, calc.wearPercentage, 0.0001f)
        assertEquals("20.000", calc.remainLabel)
    }

    @Test
    fun testDynamicDateHelpers() {
        assertTrue("Current year should be realistic (>= 2024)", WearEngine.currentYear >= 2024)
        assertTrue("Current month should be 1..12", WearEngine.currentMonth in 1..12)
        assertEquals("SEP", WearEngine.getMonthName(9))
        assertEquals("JAN", WearEngine.getMonthName(1))
        assertEquals("DEC", WearEngine.getMonthName(12))
        assertTrue("currentMonthName should not be empty", WearEngine.currentMonthName().isNotEmpty())
    }

    @Test
    fun testBackwardOdometerClamping() {
        // If odometer was mistakenly set to 40,000 and later corrected down to 15,000,
        // a part serviced at 16,900 should cleanly clamp to 0 used km instead of negative distance.
        val part = Part(
            id = "belt",
            name = "CVT belt",
            intervalKm = 20000,
            intervalMonths = 48,
            lastKm = 16900,
            lastYear = 2024,
            lastMonth = 1,
            standardCost = 450000L
        )

        val calc = WearEngine.calculate(
            part = part,
            currentOdometerKm = 15000, // corrected lower than lastKm (16,900)
            currentYear = 2024,
            currentMonth = 1
        )

        assertEquals("Used km should clamp to 0", 0, calc.usedKm)
        assertEquals(WearStatus.HEALTHY, calc.status)
        assertTrue("Wear percentage should be >= 0.0", calc.wearPercentage >= 0.0f)
    }
}
