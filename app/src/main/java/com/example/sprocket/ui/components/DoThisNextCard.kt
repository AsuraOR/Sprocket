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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprocket.domain.PartWearCalculation
import com.example.sprocket.domain.WearDriver
import com.example.sprocket.theme.SprocketAccent
import com.example.sprocket.theme.SprocketBg
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.theme.SprocketOnAccent

@Composable
fun DoThisNextCard(
    calculation: PartWearCalculation,
    onLogClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val part = calculation.part
    val headline = if (calculation.driver == WearDriver.AGE) {
        "${part.name}\nis ${calculation.remainLabel}\npast due"
    } else {
        "${part.name}\nis ${calculation.remainLabel.replace("−", "")}\npast due"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SprocketAccent)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DO THIS NEXT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                        color = SprocketOnAccent.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = headline,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        lineHeight = 34.sp,
                        letterSpacing = (-0.03).sp,
                        color = SprocketOnAccent
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(SprocketOnAccent.copy(alpha = 0.18f))
                        .border(1.dp, SprocketOnAccent.copy(alpha = 0.45f))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(
                            com.example.sprocket.ui.components.PartIconHelper.getIconResId(part.id, part.name)
                        ),
                        contentDescription = part.name,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(SprocketOnAccent),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = calculation.forecastText,
                fontWeight = FontWeight.Normal,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = SprocketOnAccent.copy(alpha = 0.92f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row {
                Box(
                    modifier = Modifier
                        .background(SprocketOnAccent)
                        .clickable { onLogClick() }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "LOG REPLACEMENT",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFF201E1D)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .border(1.dp, SprocketOnAccent.copy(alpha = 0.5f))
                        .clickable { onSnoozeClick() }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "REMIND IN 2 WEEKS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp,
                        color = SprocketOnAccent
                    )
                }
            }
        }
    }
}
