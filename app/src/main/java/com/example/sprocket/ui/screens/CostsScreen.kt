package com.example.sprocket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.data.model.OdometerReading
import com.example.sprocket.data.model.Part
import com.example.sprocket.data.model.ServiceRecord
import com.example.sprocket.domain.WearEngine
import com.example.sprocket.theme.SprocketAccent
import com.example.sprocket.theme.SprocketBg
import com.example.sprocket.theme.SprocketDivider
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.theme.SprocketLightDivider
import com.example.sprocket.theme.SprocketMuted
import com.example.sprocket.theme.SprocketNeutral300
import com.example.sprocket.theme.SprocketSurface
import com.example.sprocket.theme.sprocketBottomBorder
import com.example.sprocket.theme.sprocketTopBorder
import kotlin.math.max
import kotlin.math.roundToInt

data class RunningCostItem(
    val name: String,
    val costPer1000: Long,
    val barRatio: Float
)

data class PartSpendSummary(
    val partId: String,
    val name: String,
    val count: Int,
    val totalCost: Long
)

@Composable
fun CostsScreen(
    parts: List<Part>,
    history: List<ServiceRecord>,
    monthlyAvgKm: Int,
    unit: DistanceUnit,
    readings: List<OdometerReading> = emptyList(),
    currencyCode: String = "IDR",
    modifier: Modifier = Modifier
) {
    // Dynamic spend window: months elapsed since first record, capped at 24 (F21)
    val windowMonths = WearEngine.calculateSpendWindowMonths(history)
    val recordsInWindow = history.filter {
        WearEngine.isWithinMonths(it.year, it.month, windowMonths)
    }
    val totalSpend = recordsInWindow.sumOf { it.cost }

    // Distance from real odometer reading delta across the window (F21)
    val realDeltaKm = WearEngine.calculateRealOdometerDeltaKm(readings, windowMonths)
    val totalDistance = if (realDeltaKm > 0) {
        WearEngine.toDisplayDistance(realDeltaKm, unit)
    } else if (readings.size < 2) {
        // Fallback for new bikes with fewer than 2 recorded readings
        val displayAvg = WearEngine.toDisplayDistance(monthlyAvgKm, unit)
        displayAvg * windowMonths
    } else {
        0
    }

    val spendPer1k = if (totalDistance > 0) (totalSpend.toDouble() / totalDistance * 1000).toLong() else 0L
    val spendPerMonth = totalSpend / windowMonths

    // Running cost per 1,000 by part: (standardCost / intervalKm) * 1000
    val runningCosts = parts.filter { (it.intervalKm ?: 0) > 0 }.map { p ->
        val dist = WearEngine.toDisplayDistance(p.intervalKm ?: 1, unit)
        val per1k = if (dist > 0) (p.standardCost.toDouble() / dist * 1000).toLong() else 0L
        Pair(p.name, per1k)
    }.sortedByDescending { it.second }

    val maxPer1k = max(1L, runningCosts.maxOfOrNull { it.second } ?: 1L)
    val runningItems = runningCosts.map {
        RunningCostItem(
            name = it.first,
            costPer1000 = it.second,
            barRatio = (it.second.toFloat() / maxPer1k).coerceIn(0.03f, 1f)
        )
    }
    val totalRunningPer1k = runningCosts.sumOf { it.second }

    // All-time spend grouped by part with partId carried through (F26)
    val spendByPart = history.groupBy { it.partId }.map { (partId, records) ->
        val partName = parts.find { it.id == partId }?.name ?: partId
        PartSpendSummary(
            partId = partId,
            name = partName,
            count = records.size,
            totalCost = records.sumOf { it.cost }
        )
    }.sortedByDescending { it.totalCost }

    val topExpense = spendByPart.firstOrNull()
    val topPart = parts.find { it.id == topExpense?.partId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SprocketBg)
    ) {
        // Spent Last N Months Block
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .sprocketBottomBorder(SprocketDivider, 2.dp)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column {
                    Text(
                        text = "SPENT ON PARTS · LAST $windowMonths ${if (windowMonths == 1) "MONTH" else "MONTHS"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.6.sp,
                        color = SprocketMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = WearEngine.getCurrencySymbol(currencyCode),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = SprocketMuted,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = WearEngine.formatNumber(totalSpend),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 44.sp,
                            lineHeight = 40.sp,
                            letterSpacing = (-0.035).sp,
                            color = SprocketInk
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${WearEngine.formatCurrency(spendPer1k, currencyCode)} per ${WearEngine.formatNumber(1000)} ${unit.label} ridden · ${WearEngine.formatCurrency(spendPerMonth, currencyCode)} a month",
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        color = SprocketMuted
                    )
                }
            }
        }

        // Running Cost Per 1,000 Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RUNNING COST PER ${WearEngine.formatNumber(1000)} ${unit.label.uppercase()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.6.sp,
                    color = SprocketMuted
                )
                Text(
                    text = WearEngine.formatCurrency(totalRunningPer1k, currencyCode),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.5.sp,
                    color = SprocketInk
                )
            }
        }

        // Running Cost Bars
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .sprocketBottomBorder(SprocketDivider, 2.dp)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                runningItems.forEachIndexed { index, item ->
                    val barColor = if (index == 0) SprocketAccent else SprocketInk
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = SprocketInk,
                            maxLines = 1,
                            modifier = Modifier.width(110.dp)
                        )

                        // Bar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(14.dp)
                                .background(SprocketNeutral300)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(item.barRatio)
                                    .height(14.dp)
                                    .background(barColor)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = WearEngine.formatNumber(item.costPer1000),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            color = SprocketInk,
                            modifier = Modifier.width(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Part price ÷ its interval — what each one actually costs you to keep riding.",
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = SprocketMuted
                )
            }
        }

        // By Part All Time Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)
            ) {
                Text(
                    text = "BY PART · ALL TIME",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.6.sp,
                    color = SprocketMuted
                )
            }
        }

        // Spend Rows
        if (spendByPart.isEmpty()) {
            item {
                Text(
                    text = "No service expenses logged yet.",
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.5.sp,
                    color = SprocketMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
        }

        items(spendByPart) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sprocketTopBorder(SprocketLightDivider, 1.dp)
                    .padding(horizontal = 20.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = SprocketInk,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${item.count}×",
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                    color = SprocketMuted
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = WearEngine.formatCurrency(item.totalCost, currencyCode),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    color = SprocketInk
                )
            }
        }

        // Budget Insight Callout
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .background(SprocketSurface)
                    .padding(14.dp)
            ) {
                val line1 = if (topExpense != null) {
                    "${topExpense.name} is the single biggest line at ${WearEngine.formatCurrency(topExpense.totalCost, currencyCode)}."
                } else {
                    "No service history logged yet."
                }
                val line2 = if (topPart != null) {
                    " Budget ${WearEngine.formatCurrency(topPart.standardCost, currencyCode)} for the next one."
                } else {
                    ""
                }

                Text(
                    text = "$line1$line2",
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = SprocketInk
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
