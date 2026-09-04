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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.domain.Units
import com.example.sprocket.data.model.Part
import com.example.sprocket.domain.WearEngine
import com.example.sprocket.theme.ArchivoFontFamily
import com.example.sprocket.theme.SprocketAccent
import com.example.sprocket.theme.SprocketBg
import com.example.sprocket.theme.SprocketDivider
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.theme.SprocketMuted
import com.example.sprocket.theme.SprocketOnAccent

data class CostPreset(val label: String, val cost: Long)

@Composable
fun LogReplacementModal(
    part: Part,
    currentOdoKm: Int,
    unit: DistanceUnit,
    currencyCode: String = "IDR",
    onConfirmLog: (cost: Long, performer: String, odoKm: Int, customNote: String, advanceOdometer: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val displayCurrentOdo = WearEngine.toDisplayDistance(currentOdoKm, unit)
    var costText by rememberSaveable { mutableStateOf(part.standardCost.toString()) }
    var selectedCost by rememberSaveable { mutableStateOf(part.standardCost) }
    var selectedWho by rememberSaveable { mutableStateOf("Workshop") }
    var odoText by rememberSaveable { mutableStateOf(displayCurrentOdo.toString()) }
    var customNote by rememberSaveable { mutableStateOf("") }
    var advanceOdometer by rememberSaveable { mutableStateOf(true) }

    val focusManager = LocalFocusManager.current
    val odoFocusRequester = remember { FocusRequester() }
    val noteFocusRequester = remember { FocusRequester() }

    val presets = remember(part.standardCost) {
        listOf(
            CostPreset("0.5×", (part.standardCost * 0.5).toLong()),
            CostPreset("1×", part.standardCost),
            CostPreset("1.5×", (part.standardCost * 1.5).toLong())
        ).filter { it.cost > 0 }
    }
    val performers = listOf("DIY", "Workshop", "Dealer")

    val isDirty = customNote.isNotEmpty() ||
            costText != part.standardCost.toString() ||
            selectedCost != part.standardCost ||
            selectedWho != "Workshop" ||
            odoText != displayCurrentOdo.toString()

    SprocketSheet(
        onDismissRequest = onDismiss,
        isDirty = isDirty
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
                Text(
                    text = "LOG A REPLACEMENT",
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
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${part.name} · records maintenance and resets the wear clock",
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = SprocketMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            // WHAT IT COST
            Text(
                text = "WHAT IT COST (${currencyCode.uppercase()})",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = SprocketMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SprocketDivider)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = WearEngine.getCurrencySymbol(currencyCode),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SprocketMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = costText,
                        onValueChange = { newText ->
                            val digits = newText.filter { it.isDigit() }
                            if (digits.length <= 10) {
                                costText = digits
                                selectedCost = digits.toLongOrNull() ?: 0L
                            }
                        },
                        textStyle = TextStyle(
                            fontFamily = ArchivoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = SprocketInk
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { odoFocusRequester.requestFocus() }
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cost Presets (derived from standardCost)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = selectedCost == preset.cost
                    val bg = if (isSelected) SprocketInk else Color.Transparent
                    val fg = if (isSelected) SprocketBg else SprocketInk

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(bg)
                            .border(1.dp, SprocketDivider)
                            .clickable {
                                selectedCost = preset.cost
                                costText = preset.cost.toString()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${preset.label} (${WearEngine.formatCurrency(preset.cost, currencyCode)})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.5.sp,
                            color = fg
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // WHO DID IT
            Text(
                text = "WHO DID IT",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = SprocketMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            // DIY vs Workshop vs Dealer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                performers.forEach { who ->
                    val isSelected = selectedWho == who
                    val bg = if (isSelected) SprocketInk else Color.Transparent
                    val fg = if (isSelected) SprocketBg else SprocketInk

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(bg)
                            .border(1.dp, SprocketDivider)
                            .clickable { selectedWho = who }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = who,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = fg
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ODOMETER AT SERVICE
            Text(
                text = "ODOMETER AT SERVICE (${unit.label.uppercase()})",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = SprocketMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SprocketDivider)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = odoText,
                        onValueChange = { newText ->
                            val digits = newText.filter { it.isDigit() }
                            if (digits.length <= 7) {
                                odoText = digits
                            }
                        },
                        textStyle = TextStyle(
                            fontFamily = ArchivoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = SprocketInk
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { noteFocusRequester.requestFocus() }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(odoFocusRequester)
                    )
                    Text(
                        text = unit.label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SprocketMuted
                    )
                }
            }

            val enteredOdo = odoText.toIntOrNull() ?: displayCurrentOdo
            val isOdoHigher = enteredOdo > displayCurrentOdo
            val isOdoMuchLower = displayCurrentOdo - enteredOdo > 10000

            if (isOdoHigher) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SprocketDivider.copy(alpha = 0.5f))
                        .clickable { advanceOdometer = !advanceOdometer }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.5.dp, SprocketInk)
                            .background(if (advanceOdometer) SprocketInk else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (advanceOdometer) {
                            Text(text = "✓", color = SprocketBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Advance vehicle odometer to ${WearEngine.formatNumber(enteredOdo)} ${unit.label}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SprocketInk
                    )
                }
            } else if (isOdoMuchLower) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Notice: Reading is lower than current odometer (${WearEngine.formatNumber(displayCurrentOdo)} ${unit.label}).",
                    fontSize = 11.sp,
                    color = SprocketAccent
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // CUSTOM SERVICE NOTE / PART BRAND
            Text(
                text = "NOTE / PART BRAND (OPTIONAL)",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = SprocketMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SprocketDivider)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (customNote.isEmpty()) {
                    Text(
                        text = "e.g. Yamalube Matic 10W-40, Motul, Daytona",
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = SprocketMuted
                    )
                }
                BasicTextField(
                    value = customNote,
                    onValueChange = { customNote = it },
                    textStyle = TextStyle(
                        fontFamily = ArchivoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SprocketInk
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(noteFocusRequester)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SprocketAccent)
                    .clickable {
                        val finalOdoKm = Units.toKm(enteredOdo, unit)
                        onConfirmLog(selectedCost, selectedWho, finalOdoKm, customNote.trim(), isOdoHigher && advanceOdometer)
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SAVE TO SERVICE LOG",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = SprocketOnAccent
                )
            }
    }
}
