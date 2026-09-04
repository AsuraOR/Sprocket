package com.example.sprocket.domain

import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.data.model.OdometerReading
import com.example.sprocket.data.model.Part
import com.example.sprocket.data.model.ServiceRecord
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class WearStatus {
    OVERDUE,
    DUE_SOON,
    HEALTHY
}

enum class WearDriver {
    KM,
    AGE
}

data class WearBarMetrics(
    val baseFill: Float,
    val overflowFill: Float,
    val overflowPasses: Int,
    val isOverflowing: Boolean
)

data class PartWearCalculation(
    val part: Part,
    val usedKm: Int,
    val usedMonths: Int,
    val distanceRatio: Float,
    val ageRatio: Float,
    val wearPercentage: Float, // 0.0 to 1.0+
    val driver: WearDriver,
    val status: WearStatus,
    val remainLabel: String,
    val subLabel: String,
    val forecastText: String,
    val distanceSummary: String,
    val ageSummary: String
)

object WearEngine {
    val currentYear: Int
        get() = java.time.LocalDate.now().year

    val currentMonth: Int
        get() = java.time.LocalDate.now().monthValue

    // Backwards-compatible properties
    val CURRENT_YEAR: Int get() = currentYear
    val CURRENT_MONTH: Int get() = currentMonth

    private val MONTH_NAMES = arrayOf(
        "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
        "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
    )

    private val FULL_MONTH_NAMES = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun getMonthName(month: Int = currentMonth): String {
        val m = (month - 1).coerceIn(0, 11)
        return MONTH_NAMES[m]
    }

    fun currentMonthName(): String = getMonthName(currentMonth)

    fun getFullMonthName(month: Int = currentMonth): String {
        val m = (month - 1).coerceIn(0, 11)
        return FULL_MONTH_NAMES[m]
    }

    fun formatNumber(number: Number): String {
        val symbols = DecimalFormatSymbols(Locale.GERMAN).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)
        return formatter.format(number.toLong())
    }

    fun toDisplayDistance(km: Int, unit: DistanceUnit): Int {
        return Units.toDisplay(km, unit)
    }

    fun formatDistance(km: Int, unit: DistanceUnit): String {
        return formatNumber(toDisplayDistance(km, unit))
    }

    fun formatCurrency(amount: Long, currencyCode: String = "IDR", locale: Locale? = null): String {
        return try {
            val resolvedLocale = locale ?: when (currencyCode.uppercase()) {
                "IDR" -> Locale.forLanguageTag("id-ID")
                "USD" -> Locale.US
                "EUR" -> Locale.GERMANY
                "GBP" -> Locale.UK
                "JPY" -> Locale.JAPAN
                else -> Locale.getDefault()
            }
            val currency = Currency.getInstance(currencyCode.uppercase())
            val format = NumberFormat.getCurrencyInstance(resolvedLocale).apply {
                this.currency = currency
                maximumFractionDigits = 0
            }
            format.format(amount)
        } catch (e: Exception) {
            "$currencyCode ${formatNumber(amount)}"
        }
    }

    fun getCurrencySymbol(currencyCode: String): String {
        return try {
            Currency.getInstance(currencyCode.uppercase()).symbol
        } catch (e: Exception) {
            currencyCode
        }
    }

    fun formatDateLabel(year: Int, month: Int): String {
        val m = (month - 1).coerceIn(0, 11)
        val shortYear = year.toString().takeLast(2)
        return "${MONTH_NAMES[m]} '$shortYear"
    }

    fun calculateMonths(fromYear: Int, fromMonth: Int, toYear: Int = currentYear, toMonth: Int = currentMonth): Int {
        return (toYear - fromYear) * 12 + (toMonth - fromMonth)
    }

    fun isWithinMonths(
        year: Int,
        month: Int,
        maxMonths: Int = 24,
        currentYear: Int = WearEngine.currentYear,
        currentMonth: Int = WearEngine.currentMonth
    ): Boolean {
        val diff = calculateMonths(year, month, currentYear, currentMonth)
        return diff in 0..maxMonths
    }

    fun calculateSpendWindowMonths(
        history: List<ServiceRecord>,
        currentYear: Int = WearEngine.currentYear,
        currentMonth: Int = WearEngine.currentMonth
    ): Int {
        val validRecords = history.filter { calculateMonths(it.year, it.month, currentYear, currentMonth) >= 0 }
        if (validRecords.isEmpty()) return 24
        val earliest = validRecords.minWithOrNull(compareBy({ it.year }, { it.month })) ?: return 24
        val elapsed = calculateMonths(earliest.year, earliest.month, currentYear, currentMonth)
        return max(1, elapsed).coerceAtMost(24)
    }

    fun calculateRealOdometerDeltaKm(
        readings: List<OdometerReading>,
        windowMonths: Int? = null,
        currentYear: Int = WearEngine.currentYear,
        currentMonth: Int = WearEngine.currentMonth
    ): Int {
        val eligibleReadings = if (windowMonths != null) {
            readings.filter { isWithinMonths(it.year, it.month, windowMonths, currentYear, currentMonth) }
        } else {
            readings.filter { calculateMonths(it.year, it.month, currentYear, currentMonth) >= 0 }
        }

        if (eligibleReadings.size < 2) return 0
        val sorted = eligibleReadings.sortedWith(compareBy({ it.year }, { it.month }))
        val earliest = sorted.first()
        val latest = sorted.last()
        return max(0, latest.odometerKm - earliest.odometerKm)
    }

    fun calculateWearBar(progress: Float): WearBarMetrics {
        val nonNegative = max(0f, progress)
        val baseFill = nonNegative.coerceIn(0f, 1f)
        return if (nonNegative <= 1.0f) {
            WearBarMetrics(
                baseFill = baseFill,
                overflowFill = 0f,
                overflowPasses = 0,
                isOverflowing = false
            )
        } else {
            val excess = nonNegative - 1.0f
            val passes = excess.toInt() + 1
            val currentPassRatio = excess - excess.toInt()
            val overflowFill = if (currentPassRatio == 0f && excess > 0f) 1.0f else currentPassRatio
            WearBarMetrics(
                baseFill = 1.0f,
                overflowFill = overflowFill.coerceIn(0f, 1f),
                overflowPasses = passes,
                isOverflowing = true
            )
        }
    }

    fun calculate(
        part: Part,
        currentOdometerKm: Int,
        monthlyAvgKm: Int = 560,
        unit: DistanceUnit = DistanceUnit.KM,
        soonThreshold: Float = 0.80f,
        currentYear: Int = WearEngine.currentYear,
        currentMonth: Int = WearEngine.currentMonth
    ): PartWearCalculation {
        val usedKm = max(0, currentOdometerKm - part.lastKm)
        val usedMonths = max(0, calculateMonths(part.lastYear, part.lastMonth, currentYear, currentMonth))

        val pk = if (part.intervalKm != null && part.intervalKm > 0) {
            usedKm.toFloat() / part.intervalKm
        } else {
            0f
        }

        val pm = if (part.intervalMonths != null && part.intervalMonths > 0) {
            usedMonths.toFloat() / part.intervalMonths
        } else {
            0f
        }

        val pct = max(pk, pm)
        val byTime = pm > pk
        val driver = if (byTime) WearDriver.AGE else WearDriver.KM

        val status = if (pct >= 1.0f) {
            WearStatus.OVERDUE
        } else if (pct >= soonThreshold) {
            WearStatus.DUE_SOON
        } else {
            WearStatus.HEALTHY
        }

        val remainLabel = if (byTime) {
            val leftMonths = (part.intervalMonths ?: 0) - usedMonths
            if (leftMonths < 0) {
                "${abs(leftMonths)} mo over"
            } else {
                "$leftMonths mo"
            }
        } else {
            val leftKm = (part.intervalKm ?: 0) - usedKm
            if (leftKm < 0) {
                "${formatDistance(-leftKm, unit)} ${unit.label} over"
            } else {
                formatDistance(leftKm, unit)
            }
        }

        val intervalKmPart = if (part.intervalKm != null) "Every ${formatDistance(part.intervalKm, unit)} ${unit.label}" else "—"
        val intervalMoPart = if (part.intervalMonths != null) " or ${part.intervalMonths} months" else ""
        val lastDonePart = "last done ${formatDateLabel(part.lastYear, part.lastMonth)} at ${formatDistance(part.lastKm, unit)}"
        val subLabel = "$intervalKmPart$intervalMoPart · $lastDonePart"

        val leftKm = part.intervalKm?.let { it - usedKm }
        val leftMonths = part.intervalMonths?.let { it - usedMonths }

        val hasKmInterval = part.intervalKm != null && part.intervalKm > 0
        val hasMoInterval = part.intervalMonths != null && part.intervalMonths > 0

        val forecastText = when {
            !hasKmInterval && !hasMoInterval -> {
                "No replacement interval set. Track this component manually or edit its interval to get wear forecasts."
            }
            status == WearStatus.OVERDUE -> {
                if (byTime) {
                    "Flagged on age, not wear. The ${part.intervalMonths}-month limit ran out ${abs(leftMonths ?: 0)} months ago while distance is only ${(pk * 100).roundToInt()}% used — rubber and fluids go off on the shelf as well as on the road."
                } else {
                    val pastKm = abs(leftKm ?: 0)
                    "You are ${formatDistance(pastKm, unit)} ${unit.label} past the distance interval. At ${formatDistance(monthlyAvgKm, unit)} ${unit.label} a month that gap widens by a month every month."
                }
            }
            hasKmInterval && !hasMoInterval -> {
                val validLeftKm = max(0, leftKm ?: 0)
                val monthsToInterval = max(1, (validLeftKm.toDouble() / monthlyAvgKm).roundToInt())
                val monthWord = if (monthsToInterval == 1) "month" else "months"
                "At ${formatDistance(monthlyAvgKm, unit)} ${unit.label} a month you reach the interval in about $monthsToInterval $monthWord."
            }
            !hasKmInterval && hasMoInterval -> {
                val validLeftMonths = max(0, leftMonths ?: 0)
                "Age is the binding limit here — $validLeftMonths months left regardless of how much you ride."
            }
            else -> {
                if (leftKm != null && leftKm > 0 && !byTime) {
                    val monthsToInterval = max(1, (leftKm.toDouble() / monthlyAvgKm).roundToInt())
                    val monthWord = if (monthsToInterval == 1) "month" else "months"
                    "At ${formatDistance(monthlyAvgKm, unit)} ${unit.label} a month you reach the interval in about $monthsToInterval $monthWord."
                } else {
                    val validLeftMonths = max(0, leftMonths ?: 0)
                    "Age is the binding limit here — $validLeftMonths months left regardless of how much you ride."
                }
            }
        }

        val distSummary = if (part.intervalKm != null) {
            "${formatDistance(usedKm, unit)} / ${formatDistance(part.intervalKm, unit)}"
        } else {
            "not tracked"
        }

        val ageSummary = if (part.intervalMonths != null) {
            "$usedMonths / ${part.intervalMonths} mo"
        } else {
            "not tracked"
        }

        return PartWearCalculation(
            part = part,
            usedKm = usedKm,
            usedMonths = usedMonths,
            distanceRatio = pk,
            ageRatio = pm,
            wearPercentage = pct,
            driver = driver,
            status = status,
            remainLabel = remainLabel,
            subLabel = subLabel,
            forecastText = forecastText,
            distanceSummary = distSummary,
            ageSummary = ageSummary
        )
    }
}
