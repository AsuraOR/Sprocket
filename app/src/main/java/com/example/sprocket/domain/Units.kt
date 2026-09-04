package com.example.sprocket.domain

import com.example.sprocket.data.model.DistanceUnit
import kotlin.math.roundToInt

object Units {
    fun toDisplay(km: Int, unit: DistanceUnit): Int {
        return when (unit) {
            DistanceUnit.KM -> km
            DistanceUnit.MI -> (km * unit.toKmFactor).roundToInt()
        }
    }

    fun toKm(display: Int, unit: DistanceUnit): Int {
        return when (unit) {
            DistanceUnit.KM -> display
            DistanceUnit.MI -> (display / unit.toKmFactor).roundToInt()
        }
    }
}
