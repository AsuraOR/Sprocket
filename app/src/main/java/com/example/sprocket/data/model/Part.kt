package com.example.sprocket.data.model

import java.time.LocalDate

data class Part(
    val id: String,
    val name: String,
    val intervalKm: Int?,          // ALWAYS kilometres, whatever the display unit
    val intervalMonths: Int?,
    val lastKm: Int,
    val lastYear: Int,
    val lastMonth: Int,            // 1 to 12
    val standardCost: Long,
    val snoozedUntilEpochDay: Long? = null,  // replaces isSnoozed
    val isArchived: Boolean = false          // replaces destructive delete
) {
    fun isSnoozed(today: LocalDate = LocalDate.now()): Boolean {
        return snoozedUntilEpochDay != null && today.toEpochDay() < snoozedUntilEpochDay
    }

    val isSnoozed: Boolean
        get() = isSnoozed()

    fun copyWithUpdatedService(currentKm: Int, year: Int, month: Int): Part {
        return copy(
            lastKm = currentKm,
            lastYear = year,
            lastMonth = month,
            snoozedUntilEpochDay = null
        )
    }

    fun copyWithInterval(newKm: Int?, newMonths: Int?): Part {
        return copy(
            intervalKm = newKm,
            intervalMonths = newMonths
        )
    }
}
