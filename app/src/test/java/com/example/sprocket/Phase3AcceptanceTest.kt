package com.example.sprocket

import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.data.model.OdometerReading
import com.example.sprocket.data.model.Part
import com.example.sprocket.data.model.ServiceRecord
import com.example.sprocket.data.repository.SprocketRepository
import com.example.sprocket.domain.Units
import com.example.sprocket.domain.WearEngine
import com.example.sprocket.domain.WearStatus
import com.example.sprocket.testutil.FakeSharedPreferences
import com.example.sprocket.ui.screens.PartSpendSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class Phase3AcceptanceTest {

    // 1. Spend Window with 1 / 7 / 30 months of history (F21)
    @Test
    fun testSpendWindowWith1MonthHistory() {
        val currentYear = 2026
        val currentMonth = 9

        // 1 month ago: August 2026
        val history = listOf(
            ServiceRecord(
                id = "rec1",
                partId = "p1",
                year = 2026,
                month = 8,
                odometerKm = 10000,
                cost = 100000L,
                performer = "Workshop",
                note = "Test"
            )
        )

        val window = WearEngine.calculateSpendWindowMonths(history, currentYear, currentMonth)
        assertEquals(1, window)
    }

    @Test
    fun testSpendWindowWith7MonthsHistory() {
        val currentYear = 2026
        val currentMonth = 9

        // 7 months ago: February 2026
        val history = listOf(
            ServiceRecord(
                id = "rec1",
                partId = "p1",
                year = 2026,
                month = 2,
                odometerKm = 10000,
                cost = 100000L,
                performer = "Workshop",
                note = "Test"
            ),
            ServiceRecord(
                id = "rec2",
                partId = "p2",
                year = 2026,
                month = 6,
                odometerKm = 12000,
                cost = 150000L,
                performer = "Workshop",
                note = "Test"
            )
        )

        val window = WearEngine.calculateSpendWindowMonths(history, currentYear, currentMonth)
        assertEquals(7, window)
    }

    @Test
    fun testSpendWindowWith30MonthsHistory() {
        val currentYear = 2026
        val currentMonth = 9

        // 30 months ago: March 2024 -> must be capped at 24
        val history = listOf(
            ServiceRecord(
                id = "rec1",
                partId = "p1",
                year = 2024,
                month = 3,
                odometerKm = 5000,
                cost = 200000L,
                performer = "Workshop",
                note = "Old"
            ),
            ServiceRecord(
                id = "rec2",
                partId = "p2",
                year = 2026,
                month = 5,
                odometerKm = 14000,
                cost = 150000L,
                performer = "Workshop",
                note = "Recent"
            )
        )

        val window = WearEngine.calculateSpendWindowMonths(history, currentYear, currentMonth)
        assertEquals(24, window)
    }

    @Test
    fun testSpendWindowWithEmptyAndZeroMonths() {
        val currentYear = 2026
        val currentMonth = 9

        // Empty history defaults to 24
        assertEquals(24, WearEngine.calculateSpendWindowMonths(emptyList(), currentYear, currentMonth))

        // Record logged in current month (0 months elapsed) floors at 1 month window
        val currentMonthRecord = listOf(
            ServiceRecord(
                id = "rec0",
                partId = "p0",
                year = 2026,
                month = 9,
                odometerKm = 15000,
                cost = 50000L,
                performer = "DIY",
                note = "Now"
            )
        )
        assertEquals(1, WearEngine.calculateSpendWindowMonths(currentMonthRecord, currentYear, currentMonth))
    }

    // 2. Per-1,000 spend against real odometer deltas (F21)
    @Test
    fun testSpendPer1kAgainstRealOdometerDeltas() {
        val currentYear = 2026
        val currentMonth = 9

        val readings = listOf(
            OdometerReading(id = "r1", year = 2026, month = 1, odometerKm = 10000),
            OdometerReading(id = "r2", year = 2026, month = 5, odometerKm = 12000),
            OdometerReading(id = "r3", year = 2026, month = 9, odometerKm = 14000)
        )

        // Real delta from Jan (10,000) to Sep (14,000) is 4,000 km
        val deltaKm = WearEngine.calculateRealOdometerDeltaKm(readings, windowMonths = 24, currentYear, currentMonth)
        assertEquals(4000, deltaKm)

        val totalSpend = 2000000L // 2,000,000 IDR
        val distKm = WearEngine.toDisplayDistance(deltaKm, DistanceUnit.KM)
        val spendPer1kKm = (totalSpend.toDouble() / distKm * 1000).toLong()
        assertEquals(500000L, spendPer1kKm)

        // In miles:
        val distMi = WearEngine.toDisplayDistance(deltaKm, DistanceUnit.MI)
        val spendPer1kMi = (totalSpend.toDouble() / distMi * 1000).toLong()
        assertTrue("Distance in miles should be less than km", distMi < distKm)
        assertTrue("Spend per 1k miles should be greater than per 1k km", spendPer1kMi > spendPer1kKm)
    }

    // 3. Overflow bar metrics at 100 / 150 / 250% (F22)
    @Test
    fun testOverflowBarMetrics() {
        // At 100% wear
        val m100 = WearEngine.calculateWearBar(1.0f)
        assertEquals(1.0f, m100.baseFill, 0.001f)
        assertEquals(0.0f, m100.overflowFill, 0.001f)
        assertEquals(0, m100.overflowPasses)
        assertFalse("100% should not flag overflow", m100.isOverflowing)

        // At 150% wear
        val m150 = WearEngine.calculateWearBar(1.5f)
        assertEquals(1.0f, m150.baseFill, 0.001f)
        assertEquals(0.5f, m150.overflowFill, 0.001f)
        assertEquals(1, m150.overflowPasses)
        assertTrue("150% should flag overflow", m150.isOverflowing)

        // At 250% wear
        val m250 = WearEngine.calculateWearBar(2.5f)
        assertEquals(1.0f, m250.baseFill, 0.001f)
        assertEquals(0.5f, m250.overflowFill, 0.001f)
        assertEquals(2, m250.overflowPasses)
        assertTrue("250% should flag overflow", m250.isOverflowing)

        // Verify distinct visual metrics so a 250% part never looks like a 100% part
        assertFalse("150% must differ from 100%", m100 == m150)
        assertFalse("250% must differ from 150%", m150 == m250)
        assertFalse("250% must differ from 100%", m100 == m250)
        assertEquals(2, m250.overflowPasses)
    }

    // 4. Forecast with each interval combination including both-null (F27)
    @Test
    fun testForecastWithEachIntervalCombination() {
        val currentYear = 2026
        val currentMonth = 9

        // Combination 1: Both intervals null
        val partBothNull = Part(
            id = "p1",
            name = "Accessory Mount",
            intervalKm = null,
            intervalMonths = null,
            lastKm = 10000,
            lastYear = 2026,
            lastMonth = 1,
            standardCost = 50000L
        )
        val calcBothNull = WearEngine.calculate(
            part = partBothNull,
            currentOdometerKm = 12000,
            currentYear = currentYear,
            currentMonth = currentMonth
        )
        assertFalse("Forecast should not print 'null'", calcBothNull.forecastText.contains("null"))
        assertTrue("Forecast should indicate no interval set", calcBothNull.forecastText.contains("No replacement interval set"))

        // Combination 2: Distance interval only
        val partDistanceOnly = Part(
            id = "p2",
            name = "Chain Lubing",
            intervalKm = 1000,
            intervalMonths = null,
            lastKm = 10000,
            lastYear = 2026,
            lastMonth = 1,
            standardCost = 25000L
        )
        val calcDistanceOnly = WearEngine.calculate(
            part = partDistanceOnly,
            currentOdometerKm = 10500,
            monthlyAvgKm = 500,
            currentYear = currentYear,
            currentMonth = currentMonth
        )
        assertFalse("Forecast should not print 'null'", calcDistanceOnly.forecastText.contains("null"))
        assertTrue("Forecast should mention reaching interval", calcDistanceOnly.forecastText.contains("reach the interval in about 1 month"))

        // Combination 3: Age interval only
        val partAgeOnly = Part(
            id = "p3",
            name = "Brake Fluid",
            intervalKm = null,
            intervalMonths = 24,
            lastKm = 10000,
            lastYear = 2025,
            lastMonth = 9,
            standardCost = 60000L
        )
        val calcAgeOnly = WearEngine.calculate(
            part = partAgeOnly,
            currentOdometerKm = 20000,
            currentYear = currentYear,
            currentMonth = currentMonth
        )
        assertFalse("Forecast should not print 'null'", calcAgeOnly.forecastText.contains("null"))
        assertTrue("Forecast should mention age limit", calcAgeOnly.forecastText.contains("12 months left"))

        // Combination 4: Both intervals present
        val partBothPresent = Part(
            id = "p4",
            name = "Engine Oil",
            intervalKm = 4000,
            intervalMonths = 6,
            lastKm = 10000,
            lastYear = 2026,
            lastMonth = 8,
            standardCost = 90000L
        )
        val calcBothPresent = WearEngine.calculate(
            part = partBothPresent,
            currentOdometerKm = 11000,
            monthlyAvgKm = 500,
            currentYear = currentYear,
            currentMonth = currentMonth
        )
        assertFalse("Forecast should not print 'null'", calcBothPresent.forecastText.contains("null"))
        assertTrue("Forecast should not be empty", calcBothPresent.forecastText.isNotEmpty())
    }

    // 5. Currency formatting for at least two locales (F25)
    @Test
    fun testCurrencyFormattingMultipleLocales() {
        val amount = 150000L

        // IDR formatting
        val formattedIdr = WearEngine.formatCurrency(amount, "IDR")
        assertTrue("IDR should contain Rp: $formattedIdr", formattedIdr.contains("Rp"))
        assertTrue("IDR should contain 150.000: $formattedIdr", formattedIdr.contains("150.000"))

        // USD formatting
        val formattedUsd = WearEngine.formatCurrency(amount, "USD")
        assertTrue("USD should contain $: $formattedUsd", formattedUsd.contains("$"))
        assertTrue("USD should contain 150,000: $formattedUsd", formattedUsd.contains("150,000"))

        // EUR formatting
        val formattedEur = WearEngine.formatCurrency(amount, "EUR")
        assertTrue("EUR should contain €: $formattedEur", formattedEur.contains("€"))
    }

    // 6. Custom interval unit conversion through Units.toKm (F05)
    @Test
    fun testF05CustomIntervalUnitConversion() {
        // When user types 5000 miles in custom part interval
        val enteredMi = 5000
        val storedKm = Units.toKm(enteredMi, DistanceUnit.MI)

        // 5000 miles is ~8046.72 km -> 8047 km
        assertEquals(8047, storedKm)

        // Displaying back in miles returns exactly 5000 miles
        val displayMi = Units.toDisplay(storedKm, DistanceUnit.MI)
        assertEquals(enteredMi, displayMi)
    }

    // 7. Hero card selection from unsorted set by wear percentage (F13)
    @Test
    fun testF13HeroCardSelectionByWearPercentage() {
        val currentYear = 2026
        val currentMonth = 9

        // Belt is 140% worn
        val belt = Part(
            id = "belt",
            name = "CVT belt",
            intervalKm = 10000,
            intervalMonths = 24,
            lastKm = 0,
            lastYear = 2026,
            lastMonth = 1,
            standardCost = 300000L
        )

        // Air filter is 105% worn (alphabetically before CVT belt)
        val airFilter = Part(
            id = "air",
            name = "Air filter",
            intervalKm = 10000,
            intervalMonths = 24,
            lastKm = 3500,
            lastYear = 2026,
            lastMonth = 1,
            standardCost = 60000L
        )

        val odoKm = 14000 // Belt used = 14000 (140%), Air filter used = 10500 (105%)

        val beltCalc = WearEngine.calculate(belt, odoKm, currentYear = currentYear, currentMonth = currentMonth)
        val airCalc = WearEngine.calculate(airFilter, odoKm, currentYear = currentYear, currentMonth = currentMonth)

        // List sorted alphabetically A-Z puts Air filter first
        val sortedAlphabetical = listOf(airCalc, beltCalc)
        assertEquals("Air filter", sortedAlphabetical.first().part.name)

        // Hero card selection must pick the most overdue part (CVT belt, 140%)
        val hero = sortedAlphabetical
            .filter { it.status == WearStatus.OVERDUE && !it.part.isSnoozed }
            .maxByOrNull { it.wearPercentage }

        assertNotNull(hero)
        assertEquals("CVT belt", hero!!.part.name)
        assertEquals(1.4f, hero.wearPercentage, 0.01f)
    }

    // 8. Overdue distance label includes unit and matching over phrasing (F24)
    @Test
    fun testF24OverdueDistanceLabelWithUnit() {
        val part = Part(
            id = "p",
            name = "Brake pads",
            intervalKm = 10000,
            intervalMonths = 48,
            lastKm = 0,
            lastYear = 2026,
            lastMonth = 9,
            standardCost = 100000L
        )

        val calcKm = WearEngine.calculate(part, currentOdometerKm = 13100, unit = DistanceUnit.KM)
        assertEquals("3.100 km over", calcKm.remainLabel)

        val calcMi = WearEngine.calculate(part, currentOdometerKm = 13100, unit = DistanceUnit.MI)
        assertTrue(calcMi.remainLabel.endsWith("mi over"))
    }

    // 9. Part spend summary carries partId (F26)
    @Test
    fun testF26PartSpendSummaryCarriesPartId() {
        val parts = listOf(
            Part("id-belt", "CVT belt", 20000, 24, 0, 2026, 1, 400000L),
            Part("id-plug", "Spark plug", 8000, 12, 0, 2026, 1, 50000L)
        )

        val history = listOf(
            ServiceRecord("h1", "id-belt", 2026, 2, 20000, 450000L, "Workshop", "Belt 1"),
            ServiceRecord("h2", "id-plug", 2026, 3, 22000, 50000L, "Workshop", "Plug 1")
        )

        val spendByPart = history.groupBy { it.partId }.map { (partId, records) ->
            val partName = parts.find { it.id == partId }?.name ?: partId
            PartSpendSummary(
                partId = partId,
                name = partName,
                count = records.size,
                totalCost = records.sumOf { it.cost }
            )
        }.sortedByDescending { it.totalCost }

        val topExpense = spendByPart.first()
        assertEquals("id-belt", topExpense.partId)

        // Matching by ID rather than display name
        val topPart = parts.find { it.id == topExpense.partId }
        assertNotNull(topPart)
        assertEquals("CVT belt", topPart!!.name)
    }

    // 10. Bounded 24-month filter (F29)
    @Test
    fun testF29Bounded24MonthFilter() {
        val currentYear = 2026
        val currentMonth = 9

        // Within range: 0 months ago (this month)
        assertTrue(WearEngine.isWithinMonths(2026, 9, 24, currentYear, currentMonth))
        // Within range: 12 months ago
        assertTrue(WearEngine.isWithinMonths(2025, 9, 24, currentYear, currentMonth))
        // Within range: 24 months ago
        assertTrue(WearEngine.isWithinMonths(2024, 9, 24, currentYear, currentMonth))

        // Out of range: 25 months ago (too old)
        assertFalse(WearEngine.isWithinMonths(2024, 8, 24, currentYear, currentMonth))

        // Out of range: Future date (negative months elapsed)
        assertFalse(WearEngine.isWithinMonths(2026, 10, 24, currentYear, currentMonth))
        assertFalse(WearEngine.isWithinMonths(2027, 1, 24, currentYear, currentMonth))
    }

    // 11. Odometer accept unchanged reading and repo currency code (F11, F25)
    @Test
    fun testF11UnchangedOdometerReadingAndCurrency() {
        val prefs = FakeSharedPreferences()
        val repo = SprocketRepository(sharedPreferences = prefs)
        repo.clearAll()

        // Setting currency code
        repo.setCurrencyCode("USD")
        assertEquals("USD", repo.vehicleState.value.currencyCode)

        // First reading
        repo.updateOdometer(12000, year = 2026, month = 5)
        assertEquals(12000, repo.vehicleState.value.odometerKm)

        // Unchanged reading in next month: still accepted and updates lastReadMonth
        repo.updateOdometer(12000, year = 2026, month = 6)
        assertEquals(12000, repo.vehicleState.value.odometerKm)
        assertEquals(6, repo.vehicleState.value.lastReadMonth)
    }
}
