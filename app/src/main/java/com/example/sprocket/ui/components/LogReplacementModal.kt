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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.data.model.Part
import com.example.sprocket.domain.WearEngine
import com.example.sprocket.theme.ArchivoFontFamily
import com.example.sprocket.theme.SprocketAccent
import com.example.sprocket.theme.SprocketBg
import com.example.sprocket.theme.SprocketDivider
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.theme.SprocketMuted
import com.example.sprocket.theme.SprocketOnAccent

import com.example.sprocket.theme.sprocketTopBorder

@Composable
fun LogReplacementModal(
    part: Part,
    currentOdoKm: Int,
    unit: DistanceUnit,
    onConfirmLog: (cost: Long, performer: String, odoKm: Int, customNote: String) -> Unit,
    onDismiss: () -> Unit
) {
    val displayCurrentOdo = WearEngine.toDisplayDistance(currentOdoKm, unit)
    var costText by remember { mutableStateOf(part.standardCost.toString()) }
    var selectedCost by remember { mutableStateOf(part.standardCost) }
    var selectedWho by remember { mutableStateOf("Bengkel") }
    var odoText by remember { mutableStateOf(displayCurrentOdo.toString()) }
    var customNote by remember { mutableStateOf("") }

    val presets = remember(part.standardCost) {
        listOf(75000L, 150000L, 350000L, part.standardCost).distinct().sorted()
    }
    val performers = listOf("DIY", "Bengkel", "Dealer")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SprocketBg)
            .sprocketTopBorder(SprocketInk, 2.dp)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
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
                text = "WHAT IT COST (EDITABLE)",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = SprocketMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Cost readout / editable field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, SprocketInk)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Rp",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = SprocketMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = costText,
                        onValueChange = { newText ->
                            val digits = newText.filter { it.isDigit() }
                            if (digits.length <= 9) {
                                costText = digits
                                selectedCost = digits.toLongOrNull() ?: 0L
                            }
                        },
                        textStyle = TextStyle(
                            fontFamily = ArchivoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            letterSpacing = (-0.02).sp,
                            color = SprocketInk
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cost Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                presets.forEach { cost ->
                    val isSelected = selectedCost == cost
                    val bg = if (isSelected) SprocketInk else Color.Transparent
                    val fg = if (isSelected) SprocketBg else SprocketInk

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(bg)
                            .border(1.dp, SprocketDivider)
                            .clickable {
                                selectedCost = cost
                                costText = cost.toString()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${cost / 1000}rb",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
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

            // DIY vs Bengkel vs Dealer
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = unit.label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SprocketMuted
                    )
                }
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
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SprocketAccent)
                    .clickable {
                        val enteredOdo = odoText.toIntOrNull() ?: displayCurrentOdo
                        val finalOdoKm = if (unit == DistanceUnit.MI) {
                            (enteredOdo / unit.toKmFactor).toInt()
                        } else {
                            enteredOdo
                        }
                        onConfirmLog(selectedCost, selectedWho, finalOdoKm, customNote.trim())
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
}
