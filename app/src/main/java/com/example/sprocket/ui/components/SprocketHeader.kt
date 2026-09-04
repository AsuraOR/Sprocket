package com.example.sprocket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import com.example.sprocket.theme.SprocketAccent
import com.example.sprocket.theme.SprocketDivider
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.theme.SprocketMuted

import com.example.sprocket.theme.sprocketBottomBorder

@Composable
fun SprocketHeader(
    vehicleTag: String,
    modifier: Modifier = Modifier,
    onTitleClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .sprocketBottomBorder(SprocketDivider, 2.dp)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .then(if (onTitleClick != null) Modifier.clickable { onTitleClick() } else Modifier)
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .background(SprocketAccent)
                )
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = "SPROCKET",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    color = SprocketInk
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = vehicleTag.uppercase(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                letterSpacing = 1.2.sp,
                color = SprocketMuted
            )
        }
    }
}
