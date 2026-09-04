package com.example.sprocket.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.sprocket.domain.Units
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

    var entryText by rememberSaveable { mutableStateOf("") }
    var showSavedSummary by rememberSaveable { mutableStateOf(false) }
    var showBackwardConfirm by rememberSaveable { mutableStateOf(false) }
    var savedOdoKm by rememberSaveable { mutableStateOf(0) }
    var savedPrevOdoKm by rememberSaveable { mutableStateOf(0) }
    var savedPrevYear by rememberSaveable { mutableStateOf(0) }
    var savedPrevMonth by rememberSaveable { mutableStateOf(0) }

    val enteredValue = entryText.toIntOrNull() ?: 0
    val isCorrection = enteredValue in 1 until displayCurrent
    val isValid = enteredValue > 0

    val hintText = when {
        entryText.isEmpty() -> "Last reading ${WearEngine.formatNumber(displayCurrent)} ${unit.label} — start typing, or take the estimate."
        enteredValue <= 0 -> "Enter an odometer reading greater than 0 to save."
        isCorrection -> "Lower than last reading (${WearEngine.formatNumber(displayCurrent)} ${unit.label}). Tapping save will ask to confirm correction."
        enteredValue == displayCurrent -> "Same as last reading (${WearEngine.formatNumber(displayCurrent)} ${unit.label}) — advances date and age clocks without adding distance."
        else -> "+${WearEngine.formatNumber(enteredValue - displayCurrent)} ${unit.label} since ${WearEngine.formatDateLabel(lastReadYear, lastReadMonth)}."
    }

    val hintColor = if (isCorrection) SprocketAccent else SprocketMuted
    val borderColor = if (isCorrection) SprocketAccent else SprocketInk

    val isDirty = entryText.isNotEmpty() && !showSavedSummary && !showBackwardConfirm

    SprocketSheet(
        onDismissRequest = onDismiss,
        isDirty = isDirty
    ) {
        if (showSavedSummary) {
            // Saved Summary Sheet
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

                val delta = savedOdoKm - savedPrevOdoKm
                val deltaStr = if (delta >= 0) "+${WearEngine.formatNumber(delta)}" else "−${WearEngine.formatNumber(-delta)}"
                Text(
                    text = "$deltaStr ${unit.label} since ${WearEngine.formatDateLabel(savedPrevYear, savedPrevMonth)} · ${recalculatedParts.size} parts recalculated",
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
        } else if (showBackwardConfirm) {
            BackHandler { showBackwardConfirm = false }
            // Backward Correction Confirmation Sheet
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
                                val finalKm = Units.toKm(enteredValue, unit)
                                savedPrevOdoKm = displayCurrent
                                savedPrevYear = lastReadYear
                                savedPrevMonth = lastReadMonth
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
        } else {
            // Pad input view
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

                // Input box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
                                                }
                                                "⌫" -> {
                                                    if (entryText.isNotEmpty()) {
                                                        entryText = entryText.dropLast(1)
                                                    }
                                                }
                                                else -> {
                                                    if (entryText.length < 7) {
                                                        entryText += key
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
                                val finalKm = Units.toKm(enteredValue, unit)
                                savedPrevOdoKm = displayCurrent
                                savedPrevYear = lastReadYear
                                savedPrevMonth = lastReadMonth
                                savedOdoKm = enteredValue
                                onSaveOdometer(finalKm)
                                showSavedSummary = true
                            }
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val saveButtonText = when {
                        entryText.isEmpty() || enteredValue <= 0 -> "ENTER A READING"
                        isCorrection -> "CORRECT BACKWARDS →"
                        else -> "SAVE READING"
                    }
                    Text(
                        text = saveButtonText,
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
