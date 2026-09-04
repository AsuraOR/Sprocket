package com.example.sprocket

import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.data.model.OdometerReading
import com.example.sprocket.data.model.Part
import com.example.sprocket.data.model.ServiceRecord
import com.example.sprocket.data.model.VehicleState
import com.example.sprocket.data.repository.SprocketRepository
import com.example.sprocket.domain.Units
import com.example.sprocket.testutil.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class Phase0AcceptanceTest {

    // 1. Unit Round-Trip Test
    @Test
    fun testUnitRoundTripStability() {
        // Round trip test for KM: toDisplay(toKm(x, KM), KM) == x
        for (display in 0..50000) {
            val km = Units.toKm(display, DistanceUnit.KM)
            val roundTripDisplay = Units.toDisplay(km, DistanceUnit.KM)
            assertEquals(display, roundTripDisplay)
        }

        // Round trip test for MI: toDisplay(toKm(x, MI), MI) == x
        for (display in 0..50000) {
            val km = Units.toKm(display, DistanceUnit.MI)
            val roundTripDisplay = Units.toDisplay(km, DistanceUnit.MI)
            assertEquals("Mismatch at display mi = $display", display, roundTripDisplay)
        }

        // Test boundary and specific values
        val samples = listOf(0, 1, 2, 5, 10, 100, 1000, 3107, 5000, 12000, 40000, 150000, 300000)
        for (x in samples) {
            assertEquals(x, Units.toDisplay(Units.toKm(x, DistanceUnit.KM), DistanceUnit.KM))
            assertEquals(x, Units.toDisplay(Units.toKm(x, DistanceUnit.MI), DistanceUnit.MI))
        }
    }

    // 2. Learned Monthly Average Tests (1, 2, 12 readings and override)
    @Test
    fun testLearnedAverageWithOneReading() {
        val prefs = FakeSharedPreferences()
        val repo = SprocketRepository(sharedPreferences = prefs)
        repo.clearAll()

        // 1 reading: fallback to 560
        repo.updateOdometer(10000, year = 2026, month = 1)
        assertEquals(1, repo.readings.value.size)
        assertEquals(560, repo.monthlyAverageKm())
        assertEquals(1, repo.monthlyAverageReadingsCount())
    }

    @Test
    fun testLearnedAverageWithTwoReadings() {
        val prefs = FakeSharedPreferences()
        val repo = SprocketRepository(sharedPreferences = prefs)
        repo.clearAll()

        // Reading 1: Jan 2026 -> 10,000 km
        repo.updateOdometer(10000, year = 2026, month = 1)
        // Reading 2: Feb 2026 -> 10,600 km (1 month spanned, delta 600 km)
        repo.updateOdometer(10600, year = 2026, month = 2)

        assertEquals(2, repo.readings.value.size)
        assertEquals(600, repo.monthlyAverageKm())
        assertEquals(2, repo.monthlyAverageReadingsCount())
    }

    @Test
    fun testLearnedAverageWithTwelveReadings() {
        val prefs = FakeSharedPreferences()
        val repo = SprocketRepository(sharedPreferences = prefs)
        repo.clearAll()

        // 12 months spanned (Jan 2025 to Jan 2026): 6,000 km delta -> 500 km/month
        for (m in 1..12) {
            repo.updateOdometer(10000 + (m - 1) * 500, year = 2025, month = m)
        }
        repo.updateOdometer(16000, year = 2026, month = 1)

        assertEquals(13, repo.readings.value.size)
        assertEquals(500, repo.monthlyAverageKm())
        assertEquals(13, repo.monthlyAverageReadingsCount())
    }

    @Test
    fun testLearnedAverageOverridePrecedence() {
        val prefs = FakeSharedPreferences()
        val repo = SprocketRepository(sharedPreferences = prefs)
        repo.clearAll()

        // Add 2 readings that would yield 600 km/mo
        repo.updateOdometer(10000, year = 2026, month = 1)
        repo.updateOdometer(10600, year = 2026, month = 2)

        // Set user override
        val state = repo.vehicleState.value.copy(monthlyAverageOverrideKm = 850)
        prefs.edit().putString("state", repo.exportJson()).apply() // or update via import/state
        val updatedRepo = SprocketRepository(
            sharedPreferences = FakeSharedPreferences(
                mapOf(
                    "data_version" to 3,
                    "state" to """{"vehicleName":"MyBike","odometerKm":10600,"monthlyAverageOverrideKm":850,"unit":"KM"}""",
                    "readings" to """[{"id":"1","year":2026,"month":1,"odometerKm":10000},{"id":"2","year":2026,"month":2,"odometerKm":10600}]"""
                )
            )
        )
        assertEquals(850, updatedRepo.monthlyAverageKm())
        assertEquals(0, updatedRepo.monthlyAverageReadingsCount()) // 0 indicates override in effect
    }

    // 3. Snooze Expiry at the Boundary
    @Test
    fun testSnoozeExpiryAtBoundary() {
        val baseDate = LocalDate.of(2026, 9, 4)
        val wakeDate = baseDate.plusDays(14) // 2026-09-18

        val part = Part(
            id = "belt",
            name = "CVT belt",
            intervalKm = 20000,
            intervalMonths = 48,
            lastKm = 16900,
            lastYear = 2024,
            lastMonth = 1,
            standardCost = 450000L,
            snoozedUntilEpochDay = wakeDate.toEpochDay()
        )

        // On day 0: snoozed
        assertTrue(part.isSnoozed(baseDate))
        // On day 13: still snoozed
        assertTrue(part.isSnoozed(baseDate.plusDays(13)))
        // On day 14 (wake day): expired/woken up!
        assertFalse("Should not be snoozed on wake date", part.isSnoozed(wakeDate))
        // On day 15+: expired
        assertFalse(part.isSnoozed(baseDate.plusDays(15)))
    }

    @Test
    fun testSnoozeAndUnsnoozeInRepository() {
        val prefs = FakeSharedPreferences()
        val repo = SprocketRepository(sharedPreferences = prefs)
        val belt = repo.parts.value.first { it.id == "belt" }

        assertNull(belt.snoozedUntilEpochDay)
        assertFalse(belt.isSnoozed)

        repo.snoozePart("belt", 14)
        val snoozedBelt = repo.parts.value.first { it.id == "belt" }
        assertNotNull(snoozedBelt.snoozedUntilEpochDay)
        assertTrue(snoozedBelt.isSnoozed)

        repo.unsnoozePart("belt")
        val unsnoozedBelt = repo.parts.value.first { it.id == "belt" }
        assertNull(unsnoozedBelt.snoozedUntilEpochDay)
        assertFalse(unsnoozedBelt.isSnoozed)
    }

    // 4. v2 -> v3 Migration Test
    @Test
    fun testV2ToV3MigrationFromCapturedPayload() {
        val v2PartsJson = """
            [
              {
                "id": "belt",
                "name": "CVT belt",
                "intervalKm": 20000,
                "intervalMonths": 48,
                "lastKm": 16900,
                "lastYear": 2024,
                "lastMonth": 6,
                "standardCost": 450000,
                "isSnoozed": true
              },
              {
                "id": "oil",
                "name": "Engine oil",
                "intervalKm": 3000,
                "intervalMonths": 12,
                "lastKm": 38000,
                "lastYear": 2026,
                "lastMonth": 8,
                "standardCost": 95000,
                "isSnoozed": false
              }
            ]
        """.trimIndent()

        val v2HistoryJson = """
            [
              {
                "id": "rec-1",
                "partId": "belt",
                "year": 2024,
                "month": 6,
                "odometerKm": 16900,
                "cost": 450000,
                "performer": "Bengkel",
                "note": "Initial belt replacement"
              }
            ]
        """.trimIndent()

        val v2StateJson = """
            {
              "vehicleName": "NMAX '16",
              "odometerKm": 39850,
              "prevOdometerKm": 39000,
              "lastReadYear": 2026,
              "lastReadMonth": 9,
              "monthlyAverageKm": 560,
              "unit": "KM",
              "soonThreshold": 0.8,
              "reminderDay": "1st",
              "remindersEnabled": true,
              "alerts": {
                "overdue": true,
                "soon": true,
                "recap": false
              },
              "themePreference": "DARK"
            }
        """.trimIndent()

        val prefs = FakeSharedPreferences(
            mapOf(
                SprocketRepository.KEY_DATA_VERSION to 2,
                "parts" to v2PartsJson,
                "history" to v2HistoryJson,
                "state" to v2StateJson
            )
        )

        val repo = SprocketRepository(sharedPreferences = prefs)

        // Version upgraded to 3
        assertEquals(3, prefs.getInt(SprocketRepository.KEY_DATA_VERSION, 0))

        // Existing user has onboardingComplete = true
        assertTrue("Existing user should have onboardingComplete = true", repo.vehicleState.value.onboardingComplete)
        assertEquals("NMAX '16", repo.vehicleState.value.vehicleName)
        assertEquals(39850, repo.vehicleState.value.odometerKm)

        // Readings seeded from current odometer
        assertEquals(1, repo.readings.value.size)
        val seededReading = repo.readings.value.first()
        assertEquals(39850, seededReading.odometerKm)
        assertEquals(2026, seededReading.year)
        assertEquals(9, seededReading.month)

        // Parts check: isSnoozed=true was converted to an expired snooze
        val belt = repo.parts.value.first { it.id == "belt" }
        assertNotNull(belt.snoozedUntilEpochDay)
        assertFalse("Migrated snooze should be expired", belt.isSnoozed)

        val oil = repo.parts.value.first { it.id == "oil" }
        assertNull(oil.snoozedUntilEpochDay)
        assertFalse(oil.isSnoozed)

        // History preserved
        assertEquals(1, repo.history.value.size)
        assertEquals("rec-1", repo.history.value.first().id)
    }

    @Test
    fun testFutureVersionLoadsReadOnly() {
        val prefs = FakeSharedPreferences(
            mapOf(
                SprocketRepository.KEY_DATA_VERSION to 99,
                "state" to """{"vehicleName":"Future Bike","odometerKm":50000,"unit":"KM"}"""
            )
        )

        val repo = SprocketRepository(sharedPreferences = prefs)
        assertEquals("Future Bike", repo.vehicleState.value.vehicleName)

        // Updating odometer in read-only mode should not write to prefs
        repo.updateOdometer(60000, 2027, 1)
        val storedStateJson = prefs.getString("state", "")!!
        assertFalse("Future version prefs should not be overwritten", storedStateJson.contains("60000"))
    }

    // 5. Export -> Import Round-Trip Equality Test
    @Test
    fun testExportImportRoundTripEquality() {
        val prefs1 = FakeSharedPreferences()
        val repo1 = SprocketRepository(sharedPreferences = prefs1)
        repo1.clearAll()

        repo1.addPart("Chain", 15000, 24, 250000L)
        repo1.addPart("Sprocket", 30000, 48, 350000L)
        val chainId = repo1.parts.value.first { it.name == "Chain" }.id
        repo1.snoozePart(chainId, 7)
        repo1.logReplacement(chainId, 260000L, "DIY", odoKm = 15000, customNote = "DID Chain")
        repo1.updateOdometer(15000, year = 2026, month = 5)
        repo1.updateOdometer(16500, year = 2026, month = 6)

        val exportedJson = repo1.exportJson()
        assertTrue("Exported JSON should contain version", exportedJson.contains("\"version\":3"))

        val prefs2 = FakeSharedPreferences()
        val repo2 = SprocketRepository(sharedPreferences = prefs2)
        val importResult = repo2.importJson(exportedJson)

        assertTrue(importResult.isSuccess)
        assertEquals(repo1.vehicleState.value, repo2.vehicleState.value)
        assertEquals(repo1.allParts.value, repo2.allParts.value)
        assertEquals(repo1.history.value, repo2.history.value)
        assertEquals(repo1.readings.value, repo2.readings.value)
    }

    @Test
    fun testImportValidationFailureNeverMutatesState() {
        val prefs = FakeSharedPreferences()
        val repo = SprocketRepository(sharedPreferences = prefs)
        val originalVehicle = repo.vehicleState.value

        // Corrupted JSON
        val invalidResult = repo.importJson("{ not valid json }")
        assertTrue(invalidResult.isFailure)
        assertEquals(originalVehicle, repo.vehicleState.value)

        // Unsupported version
        val badVersionResult = repo.importJson("""{"version": 999, "vehicle": {}}""")
        assertTrue(badVersionResult.isFailure)
        assertEquals(originalVehicle, repo.vehicleState.value)
    }

    // 6. Same-Month Reading Replacement Test
    @Test
    fun testSameMonthReadingReplacement() {
        val prefs = FakeSharedPreferences()
        val repo = SprocketRepository(sharedPreferences = prefs)
        repo.clearAll()

        // First reading in Sept 2026
        repo.updateOdometer(40000, year = 2026, month = 9)
        assertEquals(1, repo.readings.value.size)
        assertEquals(40000, repo.readings.value.first().odometerKm)

        // Second reading in same month Sept 2026 replaces it
        repo.updateOdometer(40500, year = 2026, month = 9)
        assertEquals("Second reading in same month must replace existing", 1, repo.readings.value.size)
        assertEquals(40500, repo.readings.value.first().odometerKm)

        // Reading in new month Oct 2026 appends
        repo.updateOdometer(41200, year = 2026, month = 10)
        assertEquals(2, repo.readings.value.size)
        assertEquals(40500, repo.readings.value[0].odometerKm)
        assertEquals(41200, repo.readings.value[1].odometerKm)
    }

    // 7. Archive instead of Delete Test
    @Test
    fun testArchivePreservesHistoryAndSpend() {
        val prefs = FakeSharedPreferences()
        val repo = SprocketRepository(sharedPreferences = prefs)
        repo.clearAll()

        repo.addPart("Brake Pads", 10000, 12, 100000L)
        val partId = repo.parts.value.first().id
        repo.logReplacement(partId, cost = 100000L, performer = "Workshop")

        assertEquals(1, repo.parts.value.size)
        assertEquals(1, repo.history.value.size)

        // archivePart should hide from active parts but keep history
        repo.archivePart(partId)
        assertEquals(0, repo.parts.value.size) // active parts is empty
        assertEquals(1, repo.archivedParts.value.size)
        assertEquals(1, repo.allParts.value.size)
        assertEquals("History must be preserved when archived", 1, repo.history.value.size)

        // Unarchive
        repo.unarchivePart(partId)
        assertEquals(1, repo.parts.value.size)
        assertEquals(0, repo.archivedParts.value.size)
    }
}
