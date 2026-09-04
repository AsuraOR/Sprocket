package com.example.sprocket.data.model

data class Part(
    val id: String,
    val name: String,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val lastKm: Int,
    val lastYear: Int,
    val lastMonth: Int, // 1 to 12
    val standardCost: Long,
    val isSnoozed: Boolean = false
) {
    fun copyWithUpdatedService(currentKm: Int, year: Int, month: Int): Part {
        return copy(
            lastKm = currentKm,
            lastYear = year,
            lastMonth = month,
            isSnoozed = false
        )
    }

    fun copyWithInterval(newKm: Int?, newMonths: Int?): Part {
        return copy(
            intervalKm = newKm,
            intervalMonths = newMonths
        )
    }
}
