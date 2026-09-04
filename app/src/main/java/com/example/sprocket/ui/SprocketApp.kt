package com.example.sprocket.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprocket.data.model.SortMode
import com.example.sprocket.data.repository.SprocketRepository
import com.example.sprocket.domain.WearEngine
import com.example.sprocket.theme.SprocketBg
import com.example.sprocket.theme.SprocketDarkBg
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.ui.components.AddPartModal
import com.example.sprocket.ui.components.EditIntervalModal
import com.example.sprocket.ui.components.LockPreviewModal
import com.example.sprocket.ui.components.LogReplacementModal
import com.example.sprocket.ui.components.OdometerPadModal
import com.example.sprocket.ui.components.SprocketBottomNav
import com.example.sprocket.ui.components.SprocketHeader
import com.example.sprocket.ui.components.SprocketTab
import com.example.sprocket.ui.screens.CostsScreen
import com.example.sprocket.ui.screens.GarageScreen
import com.example.sprocket.ui.screens.PartDetailScreen
import com.example.sprocket.ui.screens.SetupScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SprocketApp(
    repository: SprocketRepository,
    modifier: Modifier = Modifier
) {
    val parts by repository.parts.collectAsState()
    val history by repository.history.collectAsState()
    val readings by repository.readings.collectAsState()
    val vehicleState by repository.vehicleState.collectAsState()
    val sortMode by repository.sortMode.collectAsState()

    val monthlyAvgKm = remember(readings, vehicleState) { repository.monthlyAverageKm() }

    val tabSaver = Saver<SprocketTab, String>(
        save = { it.name },
        restore = { name -> runCatching { SprocketTab.valueOf(name) }.getOrDefault(SprocketTab.GARAGE) }
    )
    var currentTab by rememberSaveable(stateSaver = tabSaver) { mutableStateOf(SprocketTab.GARAGE) }
    var selectedPartId by rememberSaveable { mutableStateOf<String?>(null) }
    var showOdometerPad by rememberSaveable { mutableStateOf(false) }
    var showLogReplacementPartId by rememberSaveable { mutableStateOf<String?>(null) }
    var showEditIntervalPartId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddPart by rememberSaveable { mutableStateOf(false) }
    var showLockPreview by rememberSaveable { mutableStateOf(false) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun showToast(msg: String) {
        scope.launch {
            toastMessage = msg
            delay(2400)
            if (toastMessage == msg) {
                toastMessage = null
            }
        }
    }

    // Recalculate wear for all parts
    val calculations = remember(parts, vehicleState, monthlyAvgKm) {
        parts.map { part ->
            WearEngine.calculate(
                part = part,
                currentOdometerKm = vehicleState.odometerKm,
                monthlyAvgKm = monthlyAvgKm,
                unit = vehicleState.unit,
                soonThreshold = vehicleState.soonThreshold
            )
        }
    }

    // Sort calculations
    val sortedCalculations = remember(calculations, sortMode) {
        when (sortMode) {
            SortMode.WEAR -> calculations.sortedByDescending { it.wearPercentage }
            SortMode.NAME -> calculations.sortedBy { it.part.name }
            SortMode.INTERVAL -> calculations.sortedBy { it.part.intervalKm ?: Int.MAX_VALUE }
        }
    }

    // Calculate dynamic bottom tab subtitles
    val garageSub = "${parts.size} parts"
    val recordsLast24 = history.filter { WearEngine.isWithinMonths(it.year, it.month, 24) }
    val costsSub = WearEngine.formatCurrency(recordsLast24.sumOf { it.cost }, vehicleState.currencyCode)
    val setupSub = if (vehicleState.remindersEnabled) "${vehicleState.reminderDay} monthly" else "reminders off"

    val selectedCalc = calculations.find { it.part.id == selectedPartId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SprocketBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Top Header
            SprocketHeader(
                vehicleTag = vehicleState.vehicleName,
                onTitleClick = {
                    selectedPartId = null
                    currentTab = SprocketTab.GARAGE
                }
            )

            // Screen Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    SprocketTab.GARAGE -> {
                        GarageScreen(
                            vehicleState = vehicleState,
                            partsCalculations = sortedCalculations,
                            sortMode = sortMode,
                            onCycleSort = { repository.cycleSort() },
                            onOpenOdometer = { showOdometerPad = true },
                            onSelectPart = { partId -> selectedPartId = partId },
                            onLogPart = { partId -> showLogReplacementPartId = partId },
                            onSnoozePart = { partId ->
                                val wakeDate = java.time.LocalDate.now().plusDays(14)
                                val wakeStr = wakeDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMM")).uppercase()
                                repository.snoozePart(partId)
                                showToast("SNOOZED UNTIL $wakeStr")
                            },
                            onUnsnoozePart = { partId ->
                                repository.unsnoozePart(partId)
                                showToast("SNOOZE CANCELLED")
                            },
                            onOpenAddPart = { showAddPart = true },
                            monthlyAvgKm = monthlyAvgKm
                        )
                    }
                    SprocketTab.COSTS -> {
                        CostsScreen(
                            parts = parts,
                            history = history,
                            monthlyAvgKm = monthlyAvgKm,
                            unit = vehicleState.unit,
                            readings = readings,
                            currencyCode = vehicleState.currencyCode
                        )
                    }
                    SprocketTab.SETUP -> {
                        SetupScreen(
                            vehicleState = vehicleState,
                            onToggleReminders = { repository.toggleReminders() },
                            onSetReminderDay = { repository.setReminderDay(it) },
                            onSetReminderDayOfMonth = { repository.setReminderDayOfMonth(it) },
                            onToggleAlert = { repository.toggleAlert(it) },
                            onSetUnit = { repository.setUnit(it) },
                            onSetCurrencyCode = { repository.setCurrencyCode(it) },
                            onSetThemePreference = { repository.setThemePreference(it) },
                            onPreviewNotification = { showLockPreview = true },
                            onResetData = {
                                repository.resetToDefaults()
                                showToast("DATA RESET TO DEFAULTS")
                            }
                        )
                    }
                }
            }

            // Bottom Navigation Segmented Bar
            SprocketBottomNav(
                currentTab = currentTab,
                garageSub = garageSub,
                costsSub = costsSub,
                setupSub = setupSub,
                onTabSelected = { tab ->
                    selectedPartId = null
                    currentTab = tab
                }
            )
        }

        // Full-screen Part Detail Overlay
        if (selectedCalc != null) {
            val partHistory = history.filter { it.partId == selectedCalc.part.id }
            PartDetailScreen(
                calc = selectedCalc,
                history = partHistory,
                unit = vehicleState.unit,
                currencyCode = vehicleState.currencyCode,
                onBack = { selectedPartId = null },
                onOpenEditInterval = { showEditIntervalPartId = selectedCalc.part.id },
                onOpenLogReplacement = { showLogReplacementPartId = selectedCalc.part.id },
                onSnooze = {
                    val wakeDate = java.time.LocalDate.now().plusDays(14)
                    val wakeStr = wakeDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMM")).uppercase()
                    repository.snoozePart(selectedCalc.part.id)
                    selectedPartId = null
                    showToast("SNOOZED UNTIL $wakeStr")
                },
                onUnsnooze = {
                    repository.unsnoozePart(selectedCalc.part.id)
                    selectedPartId = null
                    showToast("SNOOZE CANCELLED")
                },

                onDeletePart = {
                    val partName = selectedCalc.part.name.uppercase()
                    repository.deletePart(selectedCalc.part.id)
                    selectedPartId = null
                    showToast("DELETED $partName")
                }
            )
        }

        // Odometer Pad Modal Overlay
        if (showOdometerPad) {
            OdometerPadModal(
                currentOdoKm = vehicleState.odometerKm,
                monthlyAvgKm = monthlyAvgKm,
                unit = vehicleState.unit,
                lastReadYear = vehicleState.lastReadYear,
                lastReadMonth = vehicleState.lastReadMonth,
                recalculatedParts = sortedCalculations,
                onSaveOdometer = { newKm ->
                    repository.updateOdometer(newKm)
                    showToast("ODOMETER UPDATED TO ${WearEngine.formatDistance(newKm, vehicleState.unit)} ${vehicleState.unit.label.uppercase()}")
                },
                onDismiss = { showOdometerPad = false }
            )
        }

        // Log Replacement Modal Overlay
        if (showLogReplacementPartId != null) {
            val partToLog = parts.find { it.id == showLogReplacementPartId }
            if (partToLog != null) {
                LogReplacementModal(
                    part = partToLog,
                    currentOdoKm = vehicleState.odometerKm,
                    unit = vehicleState.unit,
                    currencyCode = vehicleState.currencyCode,
                    onConfirmLog = { cost, who, odoKm, note, advanceOdometer ->
                        repository.logReplacement(partToLog.id, cost, who, odoKm, note)
                        if (advanceOdometer) {
                            repository.updateOdometer(odoKm)
                        }
                        showToast("${partToLog.name.uppercase()} RESET AT ${WearEngine.formatDistance(odoKm, vehicleState.unit)} ${vehicleState.unit.label.uppercase()}")
                        showLogReplacementPartId = null
                    },
                    onDismiss = { showLogReplacementPartId = null }
                )
            }
        }

        // Edit Interval Modal Overlay
        if (showEditIntervalPartId != null) {
            val partToEdit = parts.find { it.id == showEditIntervalPartId }
            if (partToEdit != null) {
                EditIntervalModal(
                    part = partToEdit,
                    currentOdoKm = vehicleState.odometerKm,
                    unit = vehicleState.unit,
                    onSaveInterval = { newKm, newMo ->
                        repository.updateInterval(partToEdit.id, newKm, newMo)
                        showToast("INTERVAL UPDATED")
                        showEditIntervalPartId = null
                    },
                    onDismiss = { showEditIntervalPartId = null }
                )
            }
        }

        // Add Part Modal Overlay
        if (showAddPart) {
            AddPartModal(
                unit = vehicleState.unit,
                currencyCode = vehicleState.currencyCode,
                onAddPart = { name, km, mo, cost ->
                    repository.addPart(name, km, mo, cost)
                    showToast("ADDED $name TO GARAGE")
                },
                onDismiss = { showAddPart = false }
            )
        }

        // Lock Screen Notification Preview
        if (showLockPreview) {
            LockPreviewModal(
                partsCalculations = sortedCalculations,
                onTapNotification = {
                    showLockPreview = false
                    showOdometerPad = true
                },
                onDismiss = { showLockPreview = false }
            )
        }

        // Toast Notification Bar
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
                .fillMaxWidth()
        ) {
            if (toastMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SprocketInk)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = toastMessage!!,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = SprocketBg
                    )
                }
            }
        }
    }
}
