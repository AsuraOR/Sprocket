package com.example.sprocket.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sprocket.MainActivity
import com.example.sprocket.domain.WearStatus
import com.example.sprocket.data.repository.SprocketRepository
import com.example.sprocket.domain.WearEngine
import java.time.LocalDate

class MonthlyReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "sprocket_monthly_reminders"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val repository = SprocketRepository(context)
        val vehicleState = repository.vehicleState.value

        if (!vehicleState.remindersEnabled) {
            return Result.success()
        }

        val currentYear = WearEngine.currentYear
        val currentMonth = WearEngine.currentMonth
        val readingDue = vehicleState.lastReadYear != currentYear || vehicleState.lastReadMonth != currentMonth

        val parts = repository.parts.value
        val calculations = parts.map {
            WearEngine.calculate(
                part = it,
                currentOdometerKm = vehicleState.odometerKm,
                currentYear = currentYear,
                currentMonth = currentMonth,
                soonThreshold = vehicleState.soonThreshold
            )

        }

        val worstOverdue = calculations
            .filter { it.status == WearStatus.OVERDUE && !it.part.isSnoozed }
            .maxByOrNull { it.wearPercentage }

        val worstSoon = calculations
            .filter { it.status == WearStatus.DUE_SOON && !it.part.isSnoozed }
            .maxByOrNull { it.wearPercentage }

        val isQuarterlyRecap = vehicleState.alerts.recap && currentMonth in listOf(3, 6, 9, 12)

        val shouldNotify = readingDue ||
                (vehicleState.alerts.overdue && worstOverdue != null) ||
                (vehicleState.alerts.soon && worstSoon != null) ||
                isQuarterlyRecap

        if (shouldNotify) {
            showNotification(worstOverdue?.part?.name, worstOverdue?.status, worstSoon?.part?.name, worstSoon?.wearPercentage, readingDue)
        }

        // Reschedule for next month
        ReminderScheduler.schedule(context, vehicleState.reminderDayOfMonth)

        return Result.success()
    }

    private fun showNotification(
        overduePartName: String?,
        overdueStatus: WearStatus?,
        soonPartName: String?,
        soonWearPercentage: Float?,
        readingDue: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monthly Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Monthly odometer check-in and maintenance reminders"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val monthName = LocalDate.now().month.name.lowercase().replaceFirstChar { it.uppercase() }

        val message = when {
            overduePartName != null ->
                "Tap to enter $monthName's odometer. The $overduePartName is already past due — the reading tells you how far."
            soonPartName != null && soonWearPercentage != null ->
                "Tap to enter $monthName's odometer. The $soonPartName is at ${(soonWearPercentage * 100).toInt()}% wear."
            readingDue ->
                "Tap to enter $monthName's odometer reading and update all wear calculations."
            else ->
                "Monthly check-in: tap to log $monthName's odometer reading."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("SPROCKET · What's on the clock?")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }
}
