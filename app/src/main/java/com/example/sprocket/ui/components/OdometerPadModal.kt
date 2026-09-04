package com.example.sprocket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.domain.PartWearCalculation
import com.example.sprocket.domain.WearEngine
import com.example.sprocket.theme.SprocketAccent
import com.example.sprocket.theme.SprocketBg
import com.example.sprocket.theme.SprocketDivider
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.theme.SprocketMuted
import com.example.sprocket.theme.SprocketNeutral300
import com.example.sprocket.theme.SprocketOnAccent
import com.example.sprocket.theme.SprocketSurface
import com.example.sprocket.theme.sprocketTopBorder

@Composable
fun OdometerPadModal(
    currentOdoKm: Int,
    monthlyAvgKm: Int,
    unit: DistanceUnit,
    lastReadYear: Int,
    lastReadMonth: Int,
    recalculatedParts: List<PartWearCalculation>,
    onSaveOdometer: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val displayCurrent = WearEngine.toDisplayDistance(currentOdoKm, unit)
    val displayEstimate = displayCurrent + WearEngine.toDisplayDistance(monthlyAvgKm, unit)

    var entryText by remember { mutableStateOf("") }
    var scanned by remember { mutableStateOf(false) }
    var showSavedSummary by remember { mutableStateOf(false) }
    var showBackwardConfirm by remember { mutableStateOf(false) }
    var savedOdoKm by remember { mutableStateOf(0) }

    val enteredValue = entryText.toIntOrNull() ?: 0
    val isCorrection = enteredValue in 1 until displayCurrent
    val isValid = enteredValue > 0 && enteredValue != displayCurrent

    val hintText = when {
        entryText.isEmpty() -> "Last reading ${WearEngine.formatNumber(displayCurrent)} ${unit.label} — start typing, or take the estimate."
        isCorrection -> "Lower than last reading (${WearEngine.formatNumber(displayCurrent)} ${unit.label}). Tapping save will ask to confirm correction."
        scanned -> "Read off your photo — check it against the dash."
        else -> "+${WearEngine.formatNumber(enteredValue - displayCurrent)} ${unit.label} since ${WearEngine.formatDateLabel(lastReadYear, lastReadMonth)}."
    }

    val hintColor = if (isCorrection) SprocketAccent else SprocketMuted
    val borderColor = if (isCorrection) SprocketAccent else SprocketInk

    if (showSavedSummary) {
        // Saved Summary Sheet
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SprocketBg)
                .sprocketTopBorder(SprocketInk, 2.dp)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "READING SAVED",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.6.sp,
                    color = SprocketMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = WearEngine.formatNumber(savedOdoKm),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 36.sp,
                        letterSpacing = (-0.03).sp,
                        color = SprocketInk
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = unit.label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = SprocketMuted
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val delta = savedOdoKm - displayCurrent
                Text(
                    text = "+${WearEngine.formatNumber(delta)} ${unit.label} since ${WearEngine.formatDateLabel(lastReadYear, lastReadMonth)} · ${recalculatedParts.size} parts recalculated",
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.5.sp,
                    color = SprocketMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(SprocketDivider)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Top recalculated parts
                recalculatedParts.take(3).forEach { calc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val indicatorColor = when (calc.status) {
                            com.example.sprocket.domain.WearStatus.OVERDUE -> SprocketAccent
                            com.example.sprocket.domain.WearStatus.DUE_SOON -> SprocketInk
                            com.example.sprocket.domain.WearStatus.HEALTHY -> SprocketNeutral300
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(indicatorColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = calc.part.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            color = SprocketInk,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = calc.remainLabel,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = indicatorColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SprocketAccent)
                        .clickable { onDismiss() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BACK TO GARAGE",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        color = SprocketOnAccent
                    )
                }
            }
        }
    } else if (showBackwardConfirm) {
        // Backward Correction Confirmation Sheet
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SprocketBg)
                .sprocketTopBorder(SprocketAccent, 2.dp)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "CORRECT ODOMETER BACKWARDS?",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp,
                    color = SprocketAccent
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "New reading is ${WearEngine.formatNumber(enteredValue)} ${unit.label}, which is lower than the last recorded reading of ${WearEngine.formatNumber(displayCurrent)} ${unit.label}.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = SprocketInk
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "All component wear percentages and remaining intervals will be recalculated backwards to match this new reading. Use this to fix an accidental typo.",
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = SprocketMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, SprocketDivider)
                            .clickable { showBackwardConfirm = false }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CANCEL",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            letterSpacing = 0.8.sp,
                            color = SprocketInk
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(SprocketAccent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showBackwardConfirm = false
                                val finalKm = if (unit == DistanceUnit.MI) {
                                    (enteredValue / unit.toKmFactor).toInt()
                                } else {
                                    enteredValue
                                }
                                savedOdoKm = enteredValue
                                onSaveOdometer(finalKm)
                                showSavedSummary = true
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CONFIRM CORRECTION",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            letterSpacing = 0.8.sp,
                            color = SprocketOnAccent
                        )
                    }
                }
            }
        }
    } else {
        // Pad input view
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SprocketBg)
                .sprocketTopBorder(SprocketInk, 2.dp)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${WearEngine.currentMonthName().uppercase()} ODOMETER",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp,
                        color = SprocketInk
                    )
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SprocketMuted,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input box & scan button
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(2.dp, borderColor)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val textToShow = if (entryText.isEmpty()) {
                                WearEngine.formatNumber(displayEstimate)
                            } else {
                                WearEngine.formatNumber(enteredValue)
                            }
                            val textColor = if (entryText.isEmpty()) SprocketNeutral300 else SprocketInk

                            Text(
                                text = textToShow,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 36.sp,
                                letterSpacing = (-0.035).sp,
                                color = textColor
                            )
                            Text(
                                text = unit.label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = SprocketMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Scan Dash button
                    Box(
                        modifier = Modifier
                            .width(68.dp)
                            .border(1.dp, SprocketDivider)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val scannedVal = displayCurrent + WearEngine.toDisplayDistance(612, unit)
                                entryText = scannedVal.toString()
                                scanned = true
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📷",
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SCAN",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                color = SprocketAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = hintText,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                    color = hintColor
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Estimate Quick Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SprocketDivider)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            entryText = displayEstimate.toString()
                            scanned = false
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "USE ESTIMATE · ${WearEngine.formatNumber(displayEstimate)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.8.sp,
                        color = SprocketInk
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Keypad grid
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SprocketDivider)
                        .padding(1.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(SprocketBg)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            when (key) {
                                                "C" -> {
                                                    entryText = ""
                                                    scanned = false
                                                }
                                                "⌫" -> {
                                                    if (entryText.isNotEmpty()) {
                                                        entryText = entryText.dropLast(1)
                                                        scanned = false
                                                    }
                                                }
                                                else -> {
                                                    if (entryText.length < 7) {
                                                        entryText += key
                                                        scanned = false
                                                    }
                                                }
                                            }
                                        }
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val keyColor = if (key == "C" || key == "⌫") SprocketMuted else SprocketInk
                                    Text(
                                        text = key,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = keyColor
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Confirm button
                val saveBg = if (isValid) SprocketAccent else SprocketNeutral300
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(saveBg)
                        .clickable(enabled = isValid) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (isCorrection) {
                                showBackwardConfirm = true
                            } else {
                                val finalKm = if (unit == DistanceUnit.MI) {
                                    (enteredValue / unit.toKmFactor).toInt()
                                } else {
                                    enteredValue
                                }
                                savedOdoKm = enteredValue
                                onSaveOdometer(finalKm)
                                showSavedSummary = true
                            }
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCorrection) "CORRECT BACKWARDS →" else "SAVE READING",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        color = if (isValid) SprocketOnAccent else SprocketMuted
                    )
                }
            }
        }
    }
}
