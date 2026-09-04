package com.example.sprocket.ui.components

import com.example.sprocket.R

object PartIconHelper {
    fun getIconResId(partId: String, partName: String): Int {
        val lowerId = partId.lowercase()
        val lowerName = partName.lowercase()
        return when {
            lowerId.contains("belt") || lowerName.contains("belt") -> R.drawable.ic_part_belt
            lowerId.contains("tyre") || lowerId.contains("tire") || lowerName.contains("tyre") || lowerName.contains("tire") -> R.drawable.ic_part_tyre
            lowerId.contains("gear") || lowerName.contains("gear") || lowerName.contains("final drive") -> R.drawable.ic_part_gears
            lowerId.contains("oil") || lowerName.contains("oil") -> R.drawable.ic_part_oil
            lowerId.contains("air") || lowerName.contains("air") || lowerName.contains("filter") && !lowerName.contains("oil") -> R.drawable.ic_part_air
            lowerId.contains("brake") || lowerName.contains("brake") || lowerName.contains("pad") -> R.drawable.ic_part_brake
            lowerId.contains("spark") || lowerName.contains("spark") || lowerName.contains("plug") -> R.drawable.ic_part_spark
            lowerId.contains("battery") || lowerName.contains("battery") || lowerName.contains("12v") -> R.drawable.ic_part_battery
            lowerId.contains("coolant") || lowerName.contains("coolant") || lowerName.contains("radiator") -> R.drawable.ic_part_coolant
            else -> R.drawable.ic_part_belt
        }
    }
}
