package com.example.sprocket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.sprocket.notifications.ReminderScheduler
import com.example.sprocket.data.model.DistanceUnit
import com.example.sprocket.data.model.VehicleState
import com.example.sprocket.theme.SprocketAccent
import com.example.sprocket.theme.SprocketBg
import com.example.sprocket.theme.SprocketDivider
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.theme.SprocketLightDivider
import com.example.sprocket.theme.SprocketMuted
import com.example.sprocket.theme.SprocketNeutral300
import com.example.sprocket.theme.SprocketOnAccent
import com.example.sprocket.theme.sprocketBottomBorder
import com.example.sprocket.theme.sprocketLeftBorder
import com.example.sprocket.theme.sprocketTopBorder

@Composable
fun SetupScreen(
    vehicleState: VehicleState,
    onToggleReminders: () -> Unit,
    onSetReminderDay: (String) -> Unit = {},
    onSetReminderDayOfMonth: (Int) -> Unit = {},
    onToggleAlert: (String) -> Unit,
    onSetUnit: (DistanceUnit) -> Unit,
    onSetCurrencyCode: (String) -> Unit = {},
    onSetThemePreference: (String) -> Unit = {},
    onPreviewNotification: () -> Unit,
    onResetData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onToggleReminders()
            ReminderScheduler.schedule(context, vehicleState.reminderDayOfMonth)
        }
    }
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }


    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SprocketBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
        // Monthly Check-in Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .sprocketBottomBorder(SprocketDivider, 2.dp)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Monthly odometer check-in",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.015).sp,
                        color = SprocketInk
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "One push a month. Type the number off your dash and every part re-calculates.",
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = SprocketMuted
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Remind me toggle row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Remind me",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = SprocketInk
                        )

                        ModernistToggle(
                            checked = vehicleState.remindersEnabled,
                            onToggle = {
                                if (vehicleState.remindersEnabled) {
                                    onToggleReminders()
                                    ReminderScheduler.cancel(context)
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onToggleReminders()
                                        ReminderScheduler.schedule(context, vehicleState.reminderDayOfMonth)
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Day selector segmented buttons (Real day of month)
                    val currentDay = vehicleState.reminderDayOfMonth
                    val isPreset = currentDay in listOf(1, 15, 28)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SprocketDivider)
                    ) {
                        listOf(
                            1 to "1st",
                            15 to "15th",
                            28 to "28th",
                            -1 to if (!isPreset) "Day $currentDay" else "Custom"
                        ).forEachIndexed { index, (dayVal, label) ->
                            val isSelected = if (dayVal == -1) !isPreset else currentDay == dayVal
                            val bg = if (isSelected) SprocketInk else Color.Transparent
                            val fg = if (isSelected) SprocketBg else SprocketInk

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(bg)
                                    .then(if (index > 0) Modifier.sprocketLeftBorder(SprocketDivider, 1.dp) else Modifier)
                                    .clickable {
                                        if (dayVal == -1) {
                                            if (isPreset) {
                                                val newDay = 5
                                                onSetReminderDayOfMonth(newDay)
                                                if (vehicleState.remindersEnabled) {
                                                    ReminderScheduler.schedule(context, newDay)
                                                }
                                            }
                                        } else {
                                            onSetReminderDayOfMonth(dayVal)
                                            if (vehicleState.remindersEnabled) {
                                                ReminderScheduler.schedule(context, dayVal)
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.5.sp,
                                    color = fg
                                )
                            }
                        }
                    }

                    if (!isPreset) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SprocketDivider)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DAY OF MONTH",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = SprocketMuted
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .border(1.dp, SprocketDivider)
                                        .clickable {
                                            val newDay = (currentDay - 1).coerceIn(1, 31)
                                            onSetReminderDayOfMonth(newDay)
                                            if (vehicleState.remindersEnabled) {
                                                ReminderScheduler.schedule(context, newDay)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "−", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SprocketInk)
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Text(
                                    text = currentDay.toString(),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = SprocketInk
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .border(1.dp, SprocketDivider)
                                        .clickable {
                                            val newDay = (currentDay + 1).coerceIn(1, 31)
                                            onSetReminderDayOfMonth(newDay)
                                            if (vehicleState.remindersEnabled) {
                                                ReminderScheduler.schedule(context, newDay)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "+", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SprocketInk)
                                }
                            }
                        }
                    }

                }
            }
        }

        // ALSO ALERT ME Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)
            ) {
                Text(
                    text = "ALSO ALERT ME",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.6.sp,
                    color = SprocketMuted
                )
            }
        }

        // Alert Items
        item {
            AlertRow(
                title = "A part goes overdue",
                subtitle = "The moment a reading crosses the line",
                checked = vehicleState.alerts.overdue,
                onToggle = { onToggleAlert("over") }
            )
        }

        item {
            AlertRow(
                title = "A part hits ${(vehicleState.soonThreshold * 100).toInt()}%",
                subtitle = "Early warning so you can order it first",
                checked = vehicleState.alerts.soon,
                onToggle = { onToggleAlert("soon") }
            )
        }

        item {
            AlertRow(
                title = "Quarterly spend recap",
                subtitle = "What the ${vehicleState.vehicleName} cost you in parts",
                checked = vehicleState.alerts.recap,
                onToggle = { onToggleAlert("recap") }
            )
        }

        // UNITS Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp)
            ) {
                Text(
                    text = "MEASUREMENT UNIT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.6.sp,
                    color = SprocketMuted
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(1.dp, SprocketDivider)
            ) {
                listOf(DistanceUnit.KM, DistanceUnit.MI).forEachIndexed { index, u ->
                    val isSelected = vehicleState.unit == u
                    val bg = if (isSelected) SprocketInk else Color.Transparent
                    val fg = if (isSelected) SprocketBg else SprocketInk

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(bg)
                            .then(if (index > 0) Modifier.sprocketLeftBorder(SprocketDivider, 1.dp) else Modifier)
                            .clickable { onSetUnit(u) }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (u == DistanceUnit.KM) "KILOMETRES (KM)" else "MILES (MI)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.5.sp,
                            color = fg
                        )
                    }
                }
            }
        }

        // CURRENCY Section (F25)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp)
            ) {
                Text(
                    text = "CURRENCY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.6.sp,
                    color = SprocketMuted
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(1.dp, SprocketDivider)
            ) {
                listOf("IDR", "USD", "EUR", "GBP").forEachIndexed { index, curr ->
                    val isSelected = vehicleState.currencyCode == curr
                    val bg = if (isSelected) SprocketInk else Color.Transparent
                    val fg = if (isSelected) SprocketBg else SprocketInk

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(bg)
                            .then(if (index > 0) Modifier.sprocketLeftBorder(SprocketDivider, 1.dp) else Modifier)
                            .clickable { onSetCurrencyCode(curr) }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = curr,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.5.sp,
                            color = fg
                        )
                    }
                }
            }
        }

        // APPEARANCE THEME Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp)
            ) {
                Text(
                    text = "APPEARANCE THEME",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.6.sp,
                    color = SprocketMuted
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(1.dp, SprocketDivider)
            ) {
                listOf("SYSTEM", "LIGHT", "DARK").forEachIndexed { index, theme ->
                    val isSelected = vehicleState.themePreference == theme
                    val bg = if (isSelected) SprocketInk else Color.Transparent
                    val fg = if (isSelected) SprocketBg else SprocketInk

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(bg)
                            .then(if (index > 0) Modifier.sprocketLeftBorder(SprocketDivider, 1.dp) else Modifier)
                            .clickable { onSetThemePreference(theme) }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = theme,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.5.sp,
                            color = fg
                        )
                    }
                }
            }
        }

        // Preview Notification Button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SprocketInk)
                        .clickable { onPreviewNotification() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PREVIEW THE NOTIFICATION",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        letterSpacing = 0.8.sp,
                        color = SprocketInk
                    )
                }
            }
        }

        // Reset Data Button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showResetConfirm = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RESET ALL DATA TO DEFAULTS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = SprocketMuted
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Modernist Reset Confirmation Dialog
    if (showResetConfirm) {
        Dialog(
            onDismissRequest = { showResetConfirm = false },
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
                        text = "RESET ALL DATA TO DEFAULTS?",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp,
                        color = SprocketAccent
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "This will permanently erase all custom tracked components, clear your entire maintenance history, and reset your odometer.\n\nThis action cannot be undone.",
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
                        // Cancel gets filled treatment
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(SprocketInk)
                                .clickable { showResetConfirm = false }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CANCEL",
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
                                    showResetConfirm = false
                                    onResetData()
                                }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "RESET EVERYTHING",
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
}

@Composable
private fun AlertRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sprocketTopBorder(SprocketLightDivider, 1.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = SprocketInk
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontWeight = FontWeight.Normal,
                fontSize = 11.5.sp,
                color = SprocketMuted
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        ModernistToggle(
            checked = checked,
            onToggle = onToggle
        )
    }
}

@Composable
private fun ModernistToggle(
    checked: Boolean,
    onToggle: () -> Unit
) {
    val bg = if (checked) SprocketAccent else SprocketNeutral300
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(26.dp)
                .background(bg)
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(SprocketBg)
            )
        }
    }
}
