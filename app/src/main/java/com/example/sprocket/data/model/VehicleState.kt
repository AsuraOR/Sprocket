package com.example.sprocket.data.model

enum class DistanceUnit(val label: String, val toKmFactor: Double) {
    KM("km", 1.0),
    MI("mi", 0.621371)
}

enum class SortMode(val label: String) {
    WEAR("BY WEAR"),
    NAME("A–Z"),
    INTERVAL("BY INTERVAL")
}

data class AlertsConfig(
    val overdue: Boolean = true,
    val soon: Boolean = true,
    val recap: Boolean = false
)

data class VehicleState(
    val vehicleName: String = "",
    val odometerKm: Int = 0,
    val lastReadYear: Int = 0,
    val lastReadMonth: Int = 0,
    val monthlyAverageOverrideKm: Int? = null,  // null = learn it from readings
    val unit: DistanceUnit = DistanceUnit.KM,
    val currencyCode: String = "IDR",
    val soonThreshold: Float = 0.80f,
    val reminderDayOfMonth: Int = 1,            // replaces the "1st"/"Payday" strings
    val remindersEnabled: Boolean = true,
    val alerts: AlertsConfig = AlertsConfig(),
    val themePreference: String = "SYSTEM",
    val onboardingComplete: Boolean = false
) {
    // Backward compatibility helper for UI before Phase 1
    val reminderDay: String
        get() = when (reminderDayOfMonth) {
            1 -> "1st"
            15 -> "15th"
            28 -> "Last"
            else -> "${reminderDayOfMonth}th"
        }
}
