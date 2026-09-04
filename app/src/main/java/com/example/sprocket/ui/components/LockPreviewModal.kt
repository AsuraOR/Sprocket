package com.example.sprocket.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprocket.domain.PartWearCalculation
import com.example.sprocket.domain.WearStatus
import com.example.sprocket.theme.SprocketAccent
import com.example.sprocket.theme.SprocketDarkBg
import com.example.sprocket.theme.sprocketLeftBorder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LockPreviewModal(
    partsCalculations: List<PartWearCalculation> = emptyList(),
    onTapNotification: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val now = remember { LocalDateTime.now() }
    val dateText = remember(now) {
        now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())).uppercase()
    }
    val timeText = remember(now) {
        now.format(DateTimeFormatter.ofPattern("H:mm"))
    }
    val monthName = remember(now) {
        now.month.name.lowercase().replaceFirstChar { it.uppercase() }
    }

    val worstOverdue = remember(partsCalculations) {
        partsCalculations
            .filter { it.status == WearStatus.OVERDUE && !it.part.isSnoozed }
            .maxByOrNull { it.wearPercentage }
            ?: partsCalculations.filter { it.status == WearStatus.OVERDUE }.maxByOrNull { it.wearPercentage }
    }

    val worstSoon = remember(partsCalculations) {
        partsCalculations
            .filter { it.status == WearStatus.DUE_SOON && !it.part.isSnoozed }
            .maxByOrNull { it.wearPercentage }
            ?: partsCalculations.filter { it.status == WearStatus.DUE_SOON }.maxByOrNull { it.wearPercentage }
    }

    val notificationBody = when {
        worstOverdue != null ->
            "Tap to enter $monthName's odometer. The ${worstOverdue.part.name} is already past due — the reading tells you how far."
        worstSoon != null ->
            "Tap to enter $monthName's odometer. The ${worstSoon.part.name} is at ${(worstSoon.wearPercentage * 100).toInt()}% wear."
        partsCalculations.isNotEmpty() ->
            "Tap to enter $monthName's odometer. All parts healthy — the reading keeps your wear clock accurate."
        else ->
            "Tap to enter $monthName's odometer reading off your dash."
    }

    // Fixed light chrome colors for the dark lockscreen simulation (prevents dark mode collision)
    val chromeLight = Color(0xFFF7F5F0)
    val cardBackground = Color(0xFFF7F5F0)
    val cardInk = Color(0xFF201E1D)
    val cardMuted = Color(0xFF75706D)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SprocketDarkBg)
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            Text(
                text = dateText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                color = chromeLight.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = timeText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 80.sp,
                lineHeight = 76.sp,
                letterSpacing = (-0.045).sp,
                color = chromeLight
            )

            Spacer(modifier = Modifier.weight(1f))

            // Notification Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBackground)
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
                            color = cardInk
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "now",
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = cardMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "What's on the clock?",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = cardInk
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = notificationBody,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = cardInk.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "TAP THE NOTIFICATION TO LOG · TAP OUTSIDE TO CLOSE",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = chromeLight.copy(alpha = 0.45f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
