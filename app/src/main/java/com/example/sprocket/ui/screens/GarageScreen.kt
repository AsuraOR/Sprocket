package com.example.sprocket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprocket.data.model.SortMode
import com.example.sprocket.data.model.VehicleState
import com.example.sprocket.domain.PartWearCalculation
import com.example.sprocket.domain.WearDriver
import com.example.sprocket.domain.WearEngine
import com.example.sprocket.domain.WearStatus
import com.example.sprocket.theme.SprocketAccent
import com.example.sprocket.theme.SprocketBg
import com.example.sprocket.theme.SprocketDivider
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.theme.SprocketLightDivider
import com.example.sprocket.theme.SprocketMuted
import com.example.sprocket.theme.SprocketNeutral300
import com.example.sprocket.theme.SprocketNeutral400
import com.example.sprocket.theme.SprocketOnAccent
import com.example.sprocket.theme.SprocketOverdueRowBg
import com.example.sprocket.theme.SprocketSurface
import com.example.sprocket.theme.sprocketBottomBorder
import com.example.sprocket.theme.sprocketRightBorder
import com.example.sprocket.theme.sprocketTopBorder
import com.example.sprocket.ui.components.DoThisNextCard
import com.example.sprocket.ui.components.WearProgressBar
import kotlin.math.roundToInt

@Composable
fun GarageScreen(
    vehicleState: VehicleState,
    partsCalculations: List<PartWearCalculation>,
    sortMode: SortMode,
    onCycleSort: () -> Unit,
    onOpenOdometer: () -> Unit,
    onSelectPart: (String) -> Unit,
    onLogPart: (String) -> Unit,
    onSnoozePart: (String) -> Unit,
    onUnsnoozePart: (String) -> Unit = {},
    onOpenAddPart: () -> Unit,
    modifier: Modifier = Modifier,
    monthlyAvgKm: Int = 560
) {
    val needsReading = vehicleState.lastReadMonth != WearEngine.currentMonth ||
            vehicleState.lastReadYear != WearEngine.currentYear

    val overdueCount = partsCalculations.count { it.status == WearStatus.OVERDUE }
    val soonCount = partsCalculations.count { it.status == WearStatus.DUE_SOON }
    val healthyCount = partsCalculations.count { it.status == WearStatus.HEALTHY }

    val topOverdue = partsCalculations
        .filter { it.status == WearStatus.OVERDUE && !it.part.isSnoozed }
        .maxByOrNull { it.wearPercentage }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SprocketBg)
    ) {
        // ODOMETER Block
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .sprocketBottomBorder(SprocketDivider, 2.dp)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ODOMETER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.6.sp,
                            color = SprocketMuted
                        )

                        Box(
                            modifier = Modifier
                                .border(1.dp, SprocketDivider)
                                .clickable { onOpenOdometer() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "TAP TO UPDATE ✎",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                color = SprocketAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.clickable { onOpenOdometer() }
                    ) {
                        Text(
                            text = WearEngine.formatDistance(vehicleState.odometerKm, vehicleState.unit),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 48.sp,
                            lineHeight = 44.sp,
                            letterSpacing = (-0.035).sp,
                            color = SprocketInk
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = vehicleState.unit.label.uppercase(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = SprocketMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Read ${WearEngine.formatDateLabel(vehicleState.lastReadYear, vehicleState.lastReadMonth)} · averaging ${WearEngine.formatDistance(monthlyAvgKm, vehicleState.unit)} ${vehicleState.unit.label} a month",
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        color = SprocketMuted
                    )
                }
            }
        }

        // Reading Due Banner
        if (needsReading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SprocketAccent)
                        .clickable { onOpenOdometer() }
                        .padding(horizontal = 20.dp, vertical = 15.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${WearEngine.currentMonthName().uppercase()} READING DUE",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                letterSpacing = 0.4.sp,
                                color = SprocketOnAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Three seconds. Every part re-dates itself.",
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = SprocketOnAccent.copy(alpha = 0.85f)
                            )
                        }

                        Text(
                            text = "ENTER →",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp,
                            color = SprocketOnAccent
                        )
                    }
                }
            }
        }

        // Do This Next Overdue Hero Banner
        if (topOverdue != null) {
            item {
                DoThisNextCard(
                    calculation = topOverdue,
                    onLogClick = { onLogPart(topOverdue.part.id) },
                    onSnoozeClick = { onSnoozePart(topOverdue.part.id) }
                )
            }
        }

        // Status Counts Summary (Overdue / Due Soon / Healthy)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sprocketBottomBorder(SprocketDivider, 2.dp)
            ) {
                // OVERDUE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .sprocketRightBorder(SprocketDivider, 2.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column {
                        Text(
                            text = overdueCount.toString(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                            lineHeight = 30.sp,
                            color = SprocketAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "OVERDUE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.4.sp,
                            color = SprocketMuted
                        )
                    }
                }

                // DUE SOON
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .sprocketRightBorder(SprocketDivider, 2.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column {
                        Text(
                            text = soonCount.toString(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                            lineHeight = 30.sp,
                            color = SprocketInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "DUE SOON",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.4.sp,
                            color = SprocketMuted
                        )
                    }
                }

                // HEALTHY
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column {
                        Text(
                            text = healthyCount.toString(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                            lineHeight = 30.sp,
                            color = SprocketNeutral400
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "HEALTHY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.4.sp,
                            color = SprocketMuted
                        )
                    }
                }
            }
        }

        // Parts Header & Sort Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${partsCalculations.size} PARTS TRACKED",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.6.sp,
                    color = SprocketMuted
                )

                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onCycleSort() }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "${sortMode.label} ⇅",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = SprocketAccent
                    )
                }
            }
        }

        // Part List
        items(partsCalculations, key = { it.part.id }) { calc ->
            val statusColor = when (calc.status) {
                WearStatus.OVERDUE -> SprocketAccent
                WearStatus.DUE_SOON -> SprocketInk
                WearStatus.HEALTHY -> SprocketNeutral400
            }

            val rowBg = if (calc.status == WearStatus.OVERDUE) SprocketOverdueRowBg else Color.Transparent

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBg)
                    .sprocketTopBorder(SprocketLightDivider, 1.dp)
                    .clickable { onSelectPart(calc.part.id) }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Part Icon Badge
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SprocketSurface)
                                .border(1.dp, if (calc.status == WearStatus.OVERDUE) SprocketAccent else SprocketDivider)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(
                                    com.example.sprocket.ui.components.PartIconHelper.getIconResId(calc.part.id, calc.part.name)
                                ),
                                contentDescription = calc.part.name,
                                colorFilter = ColorFilter.tint(SprocketInk),
                                modifier = Modifier.fillMaxSize()
                            )
                            // Square status badge on top-left of the icon
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .size(6.dp)
                                    .background(statusColor)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Part Name
                                Text(
                                    text = calc.part.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = SprocketInk,
                                    modifier = Modifier.weight(1f)
                                )

                                // Driver Tag (AGE or KM)
                                val tagBorderColor = if (calc.driver == WearDriver.AGE && calc.status != WearStatus.HEALTHY) statusColor else SprocketDivider
                                val tagTextColor = if (calc.driver == WearDriver.AGE && calc.status != WearStatus.HEALTHY) statusColor else SprocketMuted

                                Box(
                                    modifier = Modifier
                                        .border(1.dp, tagBorderColor)
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = calc.driver.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp,
                                        letterSpacing = 1.2.sp,
                                        color = tagTextColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Remaining / Overdue Value
                                Text(
                                    text = calc.remainLabel,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = statusColor
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            // Subtitle
                            Text(
                                text = calc.subLabel,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = SprocketMuted,
                                maxLines = 1
                            )

                            if (calc.part.isSnoozed) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val wakeDate = calc.part.snoozedUntilEpochDay?.let { java.time.LocalDate.ofEpochDay(it) }
                                val dateStr = wakeDate?.format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))?.uppercase() ?: ""
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, SprocketMuted)
                                        .background(SprocketSurface)
                                        .clickable { onUnsnoozePart(calc.part.id) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "SNOOZED UNTIL $dateStr  ✕",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.8.sp,
                                        color = SprocketMuted
                                    )
                                }
                            }
                        }

                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Wear Progress Bar & Percentage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WearProgressBar(
                            progress = calc.wearPercentage,
                            color = statusColor,
                            isHatched = calc.driver == WearDriver.AGE,
                            modifier = Modifier.weight(1f),
                            height = 7.dp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "${(calc.wearPercentage * 100).roundToInt()}%",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = SprocketMuted,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                }
            }
        }

        // Track Another Part Action
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .sprocketTopBorder(SprocketLightDivider, 1.dp)
                    .sprocketBottomBorder(SprocketDivider, 2.dp)
                    .clickable { onOpenAddPart() }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "+ TRACK ANOTHER PART",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                    letterSpacing = 0.8.sp,
                    color = SprocketAccent
                )
            }
        }

        // Legend explainer
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Solid bar = counting down distance. Hatched bar = counting down age, whatever the mileage.",
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = SprocketMuted
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
