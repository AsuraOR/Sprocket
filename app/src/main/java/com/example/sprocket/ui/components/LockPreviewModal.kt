package com.example.sprocket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprocket.theme.SprocketAccent
import com.example.sprocket.theme.SprocketBg
import com.example.sprocket.theme.SprocketDarkBg
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.theme.SprocketMuted
import com.example.sprocket.theme.sprocketLeftBorder

@Composable
fun LockPreviewModal(
    onTapNotification: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SprocketDarkBg)
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 60.dp)
        ) {
            Text(
                text = "TUESDAY 1 SEPTEMBER",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                color = SprocketBg.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "9:41",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 80.sp,
                lineHeight = 76.sp,
                letterSpacing = (-0.045).sp,
                color = SprocketBg
            )

            Spacer(modifier = Modifier.weight(1f))

            // Notification Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SprocketBg)
                    .sprocketLeftBorder(SprocketAccent, 6.dp)
                    .clickable { onTapNotification() }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(SprocketAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SPROCKET",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.6.sp,
                            color = SprocketInk
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "now",
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = SprocketMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "What's on the clock?",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = SprocketInk
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Tap to enter September's odometer. The CVT belt is already past due — the reading tells you how far.",
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = SprocketInk.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "TAP THE NOTIFICATION TO LOG · TAP OUTSIDE TO CLOSE",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = SprocketBg.copy(alpha = 0.35f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
