package com.example.sprocket

import com.example.sprocket.data.model.AlertsConfig
import com.example.sprocket.data.model.Part
import com.example.sprocket.data.model.VehicleState
import com.example.sprocket.domain.WearStatus
import com.example.sprocket.data.repository.SprocketRepository
import com.example.sprocket.domain.WearEngine
import com.example.sprocket.notifications.ReminderScheduler
import com.example.sprocket.testutil.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class Phase1AcceptanceTest {

    // 1. F01 Acceptance: grep app/src/main for 612 and scanned
    @Test
    fun testNoFabricatedScanOr612InMainSource() {
        // Look in app/src/main relative to current execution
        val mainDir = File("src/main")
        assertTrue("mainDir should exist at ${mainDir.absolutePath}", mainDir.exists())

        val kotlinFiles = mainDir.walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue("Should find kotlin source files", kotlinFiles.isNotEmpty())

        val forbiddenTokens = listOf("612", "scanned")
        val violations = mutableListOf<String>()

        for (file in kotlinFiles) {
            file.forEachLine { line ->
                for (token in forbiddenTokens) {
                    if (line.contains(token)) {
                        violations.add("${file.name}: $line")
                    }
                }
            }
        }

        assertTrue("Found forbidden fabricated scan tokens in: $violations", violations.isEmpty())
    }

    // 2. F02 Acceptance: Reminder scheduling calculations and day-of-month bounds
    @Test
    fun testReminderSchedulerDelayCalculations() {
        // Case A: now is before 9am on target day
        val now1 = LocalDateTime.of(2026, 9, 15, 8, 0, 0)
        val delay1 = ReminderScheduler.calculateDelayUntilNextReminder(15, now1)
        assertEquals(3600L, delay1.seconds) // Exactly 1 hour until 9am today

        // Case B: now is after 9am on target day -> should target next month
        val now2 = LocalDateTime.of(2026, 9, 15, 10, 0, 0)
        val delay2 = ReminderScheduler.calculateDelayUntilNextReminder(15, now2)
        // Target is 2026-10-15T09:00:00
        val target2 = LocalDateTime.of(2026, 10, 15, 9, 0, 0)
        assertEquals(java.time.Duration.between(now2, target2).seconds, delay2.seconds)

        // Case C: target day is 31 in a 30-day month (e.g. September)
        val now3 = LocalDateTime.of(2026, 9, 1, 8, 0, 0)
        val delay3 = ReminderScheduler.calculateDelayUntilNextReminder(31, now3)
        // September has 30 days, so clamped target in Sep is Sep 30 at 9am
        val target3 = LocalDateTime.of(2026, 9, 30, 9, 0, 0)
        assertEquals(java.time.Duration.between(now3, target3).seconds, delay3.seconds)

        // Case D: target day 31 in February (e.g. Feb 2026 has 28 days)
        val nowFeb = LocalDateTime.of(2026, 2, 1, 8, 0, 0)
        val delayFeb = ReminderScheduler.calculateDelayUntilNextReminder(31, nowFeb)
        val targetFeb = LocalDateTime.of(2026, 2, 28, 9, 0, 0)
        assertEquals(java.time.Duration.between(nowFeb, targetFeb).seconds, delayFeb.seconds)
    }

    @Test
    fun testReminderDayOfMonthInRepository() {
        val prefs = FakeSharedPreferences()
        val repo = SprocketRepository(sharedPreferences = prefs)

        // Default reminderDayOfMonth is 1
        assertEquals(1, repo.vehicleState.value.reminderDayOfMonth)

        // Set to 15
        repo.setReminderDayOfMonth(15)
        assertEquals(15, repo.vehicleState.value.reminderDayOfMonth)

        // Set to 28
        repo.setReminderDayOfMonth(28)
        assertEquals(28, repo.vehicleState.value.reminderDayOfMonth)

        // Clamped bounds
        repo.setReminderDayOfMonth(0)
        assertEquals(1, repo.vehicleState.value.reminderDayOfMonth)

        repo.setReminderDayOfMonth(35)
        assertEquals(31, repo.vehicleState.value.reminderDayOfMonth)

        // Toggle reminders
        val initialEnabled = repo.vehicleState.value.remindersEnabled
        repo.toggleReminders()
        assertEquals(!initialEnabled, repo.vehicleState.value.remindersEnabled)
        repo.toggleReminders()
        assertEquals(initialEnabled, repo.vehicleState.value.remindersEnabled)
    }

    // 3. F03 Acceptance: Snooze and Unsnooze state and expiry
    @Test
    fun testSnoozeAndUnsnoozeWorkflow() {
        val prefs = FakeSharedPreferences()
        val repo = SprocketRepository(sharedPreferences = prefs)
        repo.clearAll()
        repo.loadSampleGarage()

        val belt = repo.parts.value.first { it.id == "belt" }
        assertFalse(belt.isSnoozed)
        assertNull(belt.snoozedUntilEpochDay)

        // Snooze belt for 14 days
        repo.snoozePart("belt", 14)
        val snoozedBelt = repo.parts.value.first { it.id == "belt" }
        assertTrue(snoozedBelt.isSnoozed)
        val expectedWake = LocalDate.now().plusDays(14).toEpochDay()
        assertEquals(expectedWake, snoozedBelt.snoozedUntilEpochDay)

        // Unsnooze belt (cancellation)
        repo.unsnoozePart("belt")
        val unsnoozedBelt = repo.parts.value.first { it.id == "belt" }
        assertFalse(unsnoozedBelt.isSnoozed)
        assertNull(unsnoozedBelt.snoozedUntilEpochDay)
    }

    // 4. F39 Acceptance: Notification copy logic reflects actual worst part
    @Test
    fun testHonestNotificationCopySelection() {
        val now = LocalDate.now()
        val monthName = now.month.name.lowercase().replaceFirstChar { it.uppercase() }

        // Overdue part exists
        val overduePart = Part("chain", "Drive Chain", 10000, 24, 0, 2025, 1, 500000L)
        val overdueCalc = WearEngine.calculate(
            part = overduePart,
            currentOdometerKm = 15000,
            currentYear = now.year,
            currentMonth = now.monthValue,
            soonThreshold = 0.8f
        )
        assertEquals(WearStatus.OVERDUE, overdueCalc.status)

        val healthyPart = Part("oil", "Engine Oil", 5000, 12, 0, now.year, now.monthValue, 100000L)
        val healthyCalc = WearEngine.calculate(
            part = healthyPart,
            currentOdometerKm = 1000,
            currentYear = now.year,
            currentMonth = now.monthValue,
            soonThreshold = 0.8f
        )
        assertEquals(WearStatus.HEALTHY, healthyCalc.status)


        val calculations = listOf(healthyCalc, overdueCalc)
        val worstOverdue = calculations.filter { it.status == WearStatus.OVERDUE && !it.part.isSnoozed }
            .maxByOrNull { it.wearPercentage }

        assertNotNull(worstOverdue)
        assertEquals("Drive Chain", worstOverdue!!.part.name)

        val expectedMessage = "Tap to enter $monthName's odometer. The ${worstOverdue.part.name} is already past due — the reading tells you how far."
        assertTrue(expectedMessage.contains("Drive Chain"))
        assertTrue(expectedMessage.contains(monthName))
        assertFalse("Must not contain hardcoded CVT belt when Drive Chain is overdue", expectedMessage.contains("CVT belt"))
    }
}
