package com.example.sprocket.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sprocket.theme.SprocketAccent
import com.example.sprocket.theme.SprocketBg
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.theme.SprocketNeutral300
import com.example.sprocket.theme.sprocketTopBorder

@Composable
fun SprocketSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    isDirty: Boolean = false,
    backgroundColor: Color = SprocketBg,
    content: @Composable ColumnScope.() -> Unit
) {
    var showDiscardConfirm by rememberSaveable { mutableStateOf(false) }

    fun handleDismissAttempt() {
        if (isDirty) {
            showDiscardConfirm = true
        } else {
            onDismissRequest()
        }
    }

    // Innermost BackHandler
    BackHandler {
        if (showDiscardConfirm) {
            showDiscardConfirm = false
        } else {
            handleDismissAttempt()
        }
    }

    // Scrim
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x8C201E1D))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                handleDismissAttempt()
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Sheet Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = false
                ) {} // block clicks from reaching scrim
                .background(backgroundColor)
                .sprocketTopBorder(SprocketInk, 2.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 10.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .background(SprocketNeutral300)
                )

                content()
            }
        }
    }

    // Discard Confirmation Dialog
    if (showDiscardConfirm) {
        Dialog(
            onDismissRequest = { showDiscardConfirm = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(SprocketBg)
                    .border(2.dp, SprocketAccent)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "DISCARD CHANGES?",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp,
                        color = SprocketAccent
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "You have unsaved changes. Are you sure you want to discard them?",
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = SprocketInk
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cancel gets filled treatment (emphasis on keeping user's work safe)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(SprocketInk)
                                .clickable { showDiscardConfirm = false }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "KEEP EDITING",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 0.8.sp,
                                color = SprocketBg
                            )
                        }

                        // Destructive action gets outline treatment
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, SprocketAccent)
                                .clickable {
                                    showDiscardConfirm = false
                                    onDismissRequest()
                                }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "DISCARD",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 0.8.sp,
                                color = SprocketAccent
                            )
                        }
                    }
                }
            }
        }
    }
}
