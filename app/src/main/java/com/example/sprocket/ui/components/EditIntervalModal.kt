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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.data.model.Part
import com.example.sprocket.domain.WearEngine
import com.example.sprocket.theme.SprocketAccent
import com.example.sprocket.theme.SprocketBg
import com.example.sprocket.theme.SprocketDivider
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.theme.SprocketMuted
import com.example.sprocket.theme.SprocketOnAccent
import com.example.sprocket.theme.SprocketSurface
import com.example.sprocket.theme.sprocketTopBorder
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun EditIntervalModal(
    part: Part,
    currentOdoKm: Int,
    unit: DistanceUnit,
    onSaveInterval: (newKm: Int?, newMo: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var draftKm by remember { mutableStateOf(part.intervalKm) }
    var draftMo by remember { mutableStateOf(part.intervalMonths) }

    val usedKm = max(0, currentOdoKm - part.lastKm)
    val usedMo = max(0, WearEngine.calculateMonths(part.lastYear, part.lastMonth))

    val npk = if (draftKm != null && draftKm!! > 0) usedKm.toFloat() / draftKm!! else 0f
    val npm = if (draftMo != null && draftMo!! > 0) usedMo.toFloat() / draftMo!! else 0f
    val np = max(npk, npm)

    val effectText = if (np >= 1.0f) {
        val driver = if (npm > npk) "age" else "distance"
        "With this interval, ${part.name.lowercase()} reads as overdue right now — on $driver."
    } else {
        "With this interval, ${part.name.lowercase()} sits at ${(np * 100).roundToInt()}% consumed."
    }

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
                    text = "EDIT INTERVAL",
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
                text = "${part.name} · whichever runs out first raises the flag",
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = SprocketMuted
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Distance Interval Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sprocketTopBorder(SprocketDivider, 1.dp)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EVERY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp,
                        color = SprocketMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (draftKm != null) "${WearEngine.formatDistance(draftKm!!, unit)} ${unit.label}" else "no distance limit",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = SprocketInk
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .border(1.dp, SprocketDivider)
                            .clickable {
                                draftKm = max(1000, (draftKm ?: 1000) - 1000)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "−", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .border(1.dp, SprocketDivider)
                            .clickable {
                                draftKm = (draftKm ?: 0) + 1000
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }

            // Age Interval Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sprocketTopBorder(SprocketDivider, 1.dp)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OR EVERY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp,
                        color = SprocketMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (draftMo != null) "$draftMo months" else "no age limit",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = SprocketInk
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .border(1.dp, SprocketDivider)
                            .clickable {
                                draftMo = max(6, (draftMo ?: 6) - 6)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "−", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .border(1.dp, SprocketDivider)
                            .clickable {
                                draftMo = (draftMo ?: 0) + 6
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Impact preview box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SprocketSurface)
                    .padding(12.dp)
            ) {
                Text(
                    text = effectText,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = SprocketInk
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SprocketAccent)
                    .clickable {
                        onSaveInterval(draftKm, draftMo)
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SAVE INTERVAL",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = SprocketOnAccent
                )
            }
        }
    }
}
