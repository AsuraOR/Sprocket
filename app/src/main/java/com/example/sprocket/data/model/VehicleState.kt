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
    val vehicleName: String = "NMAX '16",
    val odometerKm: Int = 0,
    val prevOdometerKm: Int = 0,
    val lastReadYear: Int = 2026,
    val lastReadMonth: Int = 9,
    val monthlyAverageKm: Int = 560,
    val unit: DistanceUnit = DistanceUnit.KM,
    val soonThreshold: Float = 0.80f,
    val reminderDay: String = "1st",
    val remindersEnabled: Boolean = true,
    val alerts: AlertsConfig = AlertsConfig(),
    val themePreference: String = "SYSTEM" // "SYSTEM", "LIGHT", "DARK"
)
