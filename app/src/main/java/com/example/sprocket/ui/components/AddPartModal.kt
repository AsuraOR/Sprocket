package com.example.sprocket.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.domain.WearEngine
import com.example.sprocket.theme.SprocketAccent
import com.example.sprocket.theme.SprocketBg
import com.example.sprocket.theme.SprocketDivider
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.theme.SprocketMuted
import com.example.sprocket.theme.sprocketBottomBorder
import com.example.sprocket.theme.sprocketTopBorder

data class PartPreset(
    val name: String,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val cost: Long
)

@Composable
fun AddPartModal(
    unit: DistanceUnit,
    onAddPart: (name: String, intervalKm: Int?, intervalMonths: Int?, cost: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = remember {
        listOf(
            PartPreset("Front brake pads", 15000, 24, 120000L),
            PartPreset("Rear brake pads", 12000, 24, 95000L),
            PartPreset("Spark plug", 8000, 18, 45000L),
            PartPreset("Final drive / gear oil", 12000, 24, 35000L),
            PartPreset("Engine coolant", 24000, 36, 80000L),
            PartPreset("12V Battery", null, 36, 320000L),
            PartPreset("Brake fluid (DOT 4)", null, 24, 55000L)
        )
    }

    var showCustom by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customKm by remember { mutableStateOf("") }
    var customMo by remember { mutableStateOf("") }
    var customCost by remember { mutableStateOf("") }

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
                    text = "PART LIBRARY",
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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Track another component on this vehicle",
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = SprocketMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!showCustom) {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(presets) { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .sprocketBottomBorder(SprocketDivider, 1.dp)
                                .clickable {
                                    onAddPart(preset.name, preset.intervalKm, preset.intervalMonths, preset.cost)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Preset Icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(com.example.sprocket.theme.SprocketSurface)
                                    .border(1.dp, SprocketDivider)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(
                                        com.example.sprocket.ui.components.PartIconHelper.getIconResId(preset.name, preset.name)
                                    ),
                                    contentDescription = preset.name,
                                    colorFilter = ColorFilter.tint(SprocketInk),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.5.sp,
                                    color = SprocketInk
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val kmLabel = preset.intervalKm?.let { "${WearEngine.formatDistance(it, unit)} ${unit.label}" } ?: "no distance"
                                val moLabel = preset.intervalMonths?.let { "$it mo" } ?: "no age"
                                Text(
                                    text = "Every $kmLabel / $moLabel · ~${WearEngine.formatCurrency(preset.cost)}",
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.5.sp,
                                    color = SprocketMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(SprocketInk)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "+ TRACK",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.8.sp,
                                    color = SprocketBg
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SprocketInk)
                        .clickable { showCustom = true }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ CUSTOM COMPONENT",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        letterSpacing = 1.sp,
                        color = SprocketInk
                    )
                }
            } else {
                // Custom component form
                Column {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Part Name (e.g. Brake Caliper Pin)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SprocketInk,
                            unfocusedBorderColor = SprocketDivider
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customKm,
                            onValueChange = { customKm = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Interval (${unit.label})") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SprocketInk,
                                unfocusedBorderColor = SprocketDivider
                            )
                        )

                        OutlinedTextField(
                            value = customMo,
                            onValueChange = { customMo = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Interval (Months)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SprocketInk,
                                unfocusedBorderColor = SprocketDivider
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customCost,
                        onValueChange = { customCost = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Estimated Cost (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SprocketInk,
                            unfocusedBorderColor = SprocketDivider
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val canSave = customName.isNotBlank() && (customKm.isNotBlank() || customMo.isNotBlank())
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (canSave) SprocketAccent else SprocketDivider)
                            .clickable(enabled = canSave) {
                                val km = customKm.toIntOrNull()
                                val mo = customMo.toIntOrNull()
                                val cost = customCost.toLongOrNull() ?: 100000L
                                onAddPart(customName, km, mo, cost)
                                onDismiss()
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ADD TO GARAGE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp,
                            color = SprocketBg
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCustom = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Back to presets",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = SprocketMuted
                        )
                    }
                }
            }
        }
    }
}
