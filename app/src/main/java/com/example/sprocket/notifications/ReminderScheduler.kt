package com.example.sprocket.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val WORK_NAME = "sprocket_monthly_reminder_work"

    /**
     * Calculates the duration until the next target day of month at 09:00 AM.
     * If the day exceeds the days in that month, it clamps to the last day of the month.
     */
    fun calculateDelayUntilNextReminder(
        targetDayOfMonth: Int,
        now: LocalDateTime = LocalDateTime.now()
    ): Duration {
        val clampedTarget = targetDayOfMonth.coerceIn(1, 31)

        // Check if this month's reminder time is still in the future
        val daysInThisMonth = now.toLocalDate().lengthOfMonth()
        val thisMonthDay = clampedTarget.coerceAtMost(daysInThisMonth)
        val thisMonthTarget = now.withDayOfMonth(thisMonthDay)
            .withHour(9)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val nextTarget = if (thisMonthTarget.isAfter(now)) {
            thisMonthTarget
        } else {
            val nextMonth = now.plusMonths(1)
            val daysInNextMonth = nextMonth.toLocalDate().lengthOfMonth()
            val nextMonthDay = clampedTarget.coerceAtMost(daysInNextMonth)
            nextMonth.withDayOfMonth(nextMonthDay)
                .withHour(9)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
        }

        return Duration.between(now, nextTarget)
    }

    /**
     * Schedules or reschedules the monthly reminder work.
     */
    fun schedule(context: Context, targetDayOfMonth: Int) {
        val delay = calculateDelayUntilNextReminder(targetDayOfMonth)
        val workRequest = OneTimeWorkRequestBuilder<MonthlyReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .addTag("sprocket_monthly_reminder")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancels any active monthly reminder work.
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
