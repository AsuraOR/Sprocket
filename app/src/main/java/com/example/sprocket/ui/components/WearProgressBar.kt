package com.example.sprocket.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sprocket.domain.WearEngine
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.theme.SprocketNeutral300

@Composable
fun WearProgressBar(
    progress: Float,
    color: Color,
    isHatched: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 7.dp,
    trackColor: Color = SprocketNeutral300
) {
    val metrics = WearEngine.calculateWearBar(progress)
    val inkColor = SprocketInk

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth = size.width
        val barHeight = size.height
        val fillWidth = totalWidth * metrics.baseFill

        // Draw track
        drawRect(
            color = trackColor,
            size = Size(totalWidth, barHeight)
        )

        if (fillWidth > 0f) {
            if (!isHatched) {
                // Solid bar for distance wear
                drawRect(
                    color = color,
                    size = Size(fillWidth, barHeight)
                )
            } else {
                // Hatched diagonal stripe pattern for age wear
                val clipPath = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, fillWidth, barHeight))
                }

                clipPath(clipPath) {
                    val stripeWidth = 4f
                    val stripeGap = 6f
                    val step = stripeWidth + stripeGap
                    val lineCount = ((fillWidth + barHeight * 2) / step).toInt() + 2

                    for (i in -2..lineCount) {
                        val startX = i * step
                        drawLine(
                            color = color,
                            start = Offset(startX, barHeight),
                            end = Offset(startX + barHeight, 0f),
                            strokeWidth = stripeWidth
                        )
                    }
                }
            }

            // Overflow pass overlay when progress > 100% (F22)
            if (metrics.isOverflowing) {
                val overflowWidth = totalWidth * metrics.overflowFill

                if (metrics.overflowPasses == 1) {
                    // First overflow pass (100% - 200%): semi-transparent dark tint + edge tick
                    drawRect(
                        color = inkColor.copy(alpha = 0.5f),
                        size = Size(overflowWidth, barHeight)
                    )
                    // Mark the overflow edge line
                    drawLine(
                        color = inkColor,
                        start = Offset(overflowWidth, 0f),
                        end = Offset(overflowWidth, barHeight),
                        strokeWidth = 2.5f
                    )
                } else {
                    // Multi-pass overflow (>= 200%): full dark layer for 100-200%, plus second lap overlay
                    drawRect(
                        color = inkColor.copy(alpha = 0.5f),
                        size = Size(totalWidth, barHeight)
                    )
                    // Secondary intense overlay for second lap (e.g. 250%)
                    drawRect(
                        color = Color.White.copy(alpha = 0.45f),
                        size = Size(overflowWidth, barHeight)
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(overflowWidth, 0f),
                        end = Offset(overflowWidth, barHeight),
                        strokeWidth = 2.5f
                    )
                }
            }
        }
    }
}
