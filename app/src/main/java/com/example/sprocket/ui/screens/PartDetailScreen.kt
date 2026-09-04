package com.example.sprocket.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.data.model.ServiceRecord
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
import com.example.sprocket.theme.SprocketOnAccent
import com.example.sprocket.theme.SprocketSurface
import com.example.sprocket.theme.sprocketBottomBorder
import com.example.sprocket.theme.sprocketLeftBorder
import com.example.sprocket.theme.sprocketRightBorder
import com.example.sprocket.theme.sprocketTopBorder
import com.example.sprocket.ui.components.WearProgressBar
import kotlin.math.roundToInt

@Composable
fun PartDetailScreen(
    calc: PartWearCalculation,
    history: List<ServiceRecord>,
    unit: DistanceUnit,
    currencyCode: String = "IDR",
    onBack: () -> Unit,
    onOpenEditInterval: () -> Unit,
    onOpenLogReplacement: () -> Unit,
    onSnooze: () -> Unit,
    onUnsnooze: () -> Unit = {},
    onDeletePart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val part = calc.part
    val wakeDate = calc.part.snoozedUntilEpochDay?.let { java.time.LocalDate.ofEpochDay(it) }
    val wakeStr = wakeDate?.format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))?.uppercase() ?: ""
    val statusTitle = when {
        calc.part.isSnoozed -> "SNOOZED UNTIL $wakeStr"
        calc.status == WearStatus.OVERDUE -> if (calc.driver == WearDriver.AGE) "OVERDUE — ON AGE, NOT DISTANCE" else "OVERDUE — ON DISTANCE"
        calc.status == WearStatus.DUE_SOON -> if (calc.driver == WearDriver.AGE) "DUE SOON — ON AGE" else "DUE SOON — ON DISTANCE"
        else -> "HEALTHY"
    }

    val headBg = when (calc.status) {
        WearStatus.OVERDUE -> SprocketAccent
        WearStatus.DUE_SOON -> SprocketInk
        WearStatus.HEALTHY -> SprocketSurface
    }

    val headFg = when (calc.status) {
        WearStatus.OVERDUE -> SprocketOnAccent
        WearStatus.DUE_SOON -> SprocketBg
        WearStatus.HEALTHY -> SprocketInk
    }

    val remainSub = if (calc.driver == WearDriver.AGE) {
        if (calc.status == WearStatus.OVERDUE) "past the age limit" else "of the age limit left"
    } else {
        if (calc.status == WearStatus.OVERDUE) "${unit.label} past the interval" else "${unit.label} of the interval left"
    }

    val intervalLabel = (part.intervalKm?.let { "${WearEngine.formatDistance(it, unit)} ${unit.label}" } ?: "no distance") +
            (part.intervalMonths?.let { " or $it months" } ?: "")

    BackHandler { onBack() }

    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SprocketBg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Back Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .sprocketBottomBorder(SprocketDivider, 2.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "← GARAGE",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.5.sp,
                letterSpacing = 1.2.sp,
                color = SprocketAccent,
                modifier = Modifier.clickable { onBack() }
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Hero Status Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headBg)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = statusTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                letterSpacing = 1.6.sp,
                                color = headFg.copy(alpha = 0.75f)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = part.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp,
                                lineHeight = 32.sp,
                                letterSpacing = (-0.02).sp,
                                color = headFg
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(headFg.copy(alpha = 0.15f))
                                .border(1.dp, headFg.copy(alpha = 0.4f))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(
                                    com.example.sprocket.ui.components.PartIconHelper.getIconResId(part.id, part.name)
                                ),
                                contentDescription = part.name,
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(headFg),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = calc.remainLabel,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 44.sp,
                                    lineHeight = 40.sp,
                                    letterSpacing = (-0.035).sp,
                                    color = headFg
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = remainSub,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = headFg.copy(alpha = 0.8f)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${(calc.wearPercentage * 100).roundToInt()}%",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = headFg
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "CONSUMED",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 1.4.sp,
                                    color = headFg.copy(alpha = 0.75f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        WearProgressBar(
                            progress = calc.wearPercentage,
                            color = headFg,
                            isHatched = calc.driver == WearDriver.AGE,
                            trackColor = headFg.copy(alpha = 0.25f),
                            height = 9.dp
                        )
                    }
                }
            }

            // Distance vs Age Split Cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sprocketBottomBorder(SprocketDivider, 2.dp)
                ) {
                    val kmActive = calc.driver == WearDriver.KM && calc.status != WearStatus.HEALTHY
                    val moActive = calc.driver == WearDriver.AGE && calc.status != WearStatus.HEALTHY

                    // Distance Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (kmActive) Color(0x18EC3013) else Color.Transparent)
                            .sprocketRightBorder(SprocketDivider, 2.dp)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "DISTANCE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                letterSpacing = 1.4.sp,
                                color = SprocketMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = calc.distanceSummary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = SprocketInk
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            val kmPct = part.intervalKm?.let { "${(calc.distanceRatio * 100).roundToInt()}% used" } ?: "—"
                            Text(
                                text = kmPct,
                                fontWeight = FontWeight.Normal,
                                fontSize = 10.5.sp,
                                color = SprocketMuted
                            )
                        }
                    }

                    // Age Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (moActive) Color(0x18EC3013) else Color.Transparent)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "AGE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                letterSpacing = 1.4.sp,
                                color = SprocketMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = calc.ageSummary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = SprocketInk
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            val moPct = part.intervalMonths?.let { "${(calc.ageRatio * 100).roundToInt()}% used" } ?: "—"
                            Text(
                                text = moPct,
                                fontWeight = FontWeight.Normal,
                                fontSize = 10.5.sp,
                                color = SprocketMuted
                            )
                        }
                    }
                }
            }

            // Mileage Forecast text
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sprocketBottomBorder(SprocketLightDivider, 1.dp)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = calc.forecastText,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = SprocketInk
                    )
                }
            }

            // Interval Row with Edit button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sprocketBottomBorder(SprocketLightDivider, 1.dp)
                        .clickable { onOpenEditInterval() }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Interval — $intervalLabel",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = SprocketInk
                    )

                    Text(
                        text = "EDIT",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = SprocketAccent
                    )
                }
            }

            // History Section Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)
                ) {
                    Text(
                        text = "HISTORY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.6.sp,
                        color = SprocketMuted
                    )
                }
            }

            // History Records
            if (history.isEmpty()) {
                item {
                    Text(
                        text = "No service history logged yet.",
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        color = SprocketMuted,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }
            }

            items(history) { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sprocketTopBorder(SprocketLightDivider, 1.dp)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = WearEngine.formatDateLabel(record.year, record.month),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = SprocketMuted,
                        modifier = Modifier.width(60.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = record.note,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = SprocketInk
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "at ${WearEngine.formatDistance(record.odometerKm, unit)} ${unit.label}",
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = SprocketMuted
                        )
                    }

                    Text(
                        text = WearEngine.formatCurrency(record.cost, currencyCode),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = SprocketInk
                    )
                }
            }

            // Delete Component Action
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SprocketDivider)
                            .clickable { showDeleteConfirm = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DELETE THIS COMPONENT",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            letterSpacing = 1.sp,
                            color = SprocketAccent
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Bottom Actions (Snooze + Log a Replacement)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SprocketSurface)
                .sprocketTopBorder(SprocketDivider, 2.dp)
                .navigationBarsPadding()
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                val isSnoozed = calc.part.isSnoozed
                Box(
                    modifier = Modifier
                        .clickable { if (isSnoozed) onUnsnooze() else onSnooze() }
                        .sprocketRightBorder(SprocketDivider, 2.dp)
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSnoozed) "CANCEL SNOOZE" else "SNOOZE (14 DAYS)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = if (isSnoozed) SprocketAccent else SprocketInk
                    )
                }


                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SprocketAccent)
                        .clickable { onOpenLogReplacement() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LOG A REPLACEMENT",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = SprocketOnAccent
                    )
                }
            }
        }
    }

    // Modernist Delete Confirmation Dialog
    if (showDeleteConfirm) {
        Dialog(
            onDismissRequest = { showDeleteConfirm = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(SprocketBg)
                    .border(2.dp, SprocketAccent)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "DELETE ${part.name.uppercase()}?",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp,
                        color = SprocketAccent
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "This will permanently remove ${part.name} and all associated service history from your garage.\n\nThis action cannot be undone.",
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = SprocketInk
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cancel gets filled treatment
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(SprocketInk)
                                .clickable { showDeleteConfirm = false }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CANCEL",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 0.8.sp,
                                color = SprocketBg
                            )
                        }

                        // Destructive action gets outline treatment
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, SprocketAccent)
                                .clickable {
                                    showDeleteConfirm = false
                                    onDeletePart()
                                }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "DELETE PART",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 0.8.sp,
                                color = SprocketAccent
                            )
                        }
                    }
                }
            }
        }
    }
}
}
