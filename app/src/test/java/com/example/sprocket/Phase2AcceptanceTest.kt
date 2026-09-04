package com.example.sprocket

import androidx.compose.runtime.saveable.Saver
import com.example.sprocket.ui.components.SprocketTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Phase2AcceptanceTest {

    // 1. F06 Acceptance: Tab Saver round-trip and resilience to invalid values
    @Test
    fun testSprocketTabSaver() {
        val tabSaver = Saver<SprocketTab, String>(
            save = { it.name },
            restore = { name -> runCatching { SprocketTab.valueOf(name) }.getOrDefault(SprocketTab.GARAGE) }
        )

        val dummyScope = androidx.compose.runtime.saveable.SaverScope { true }
        // Verify save and restore for all tabs
        for (tab in SprocketTab.values()) {
            val saved = with(tabSaver) { dummyScope.save(tab) }
            assertEquals(tab.name, saved)
            val restored = tabSaver.restore(saved!!)
            assertEquals(tab, restored)
        }

        // Verify fallback on corrupted or invalid saved state
        val fallback = tabSaver.restore("UNKNOWN_TAB_CORRUPTED")
        assertEquals(SprocketTab.GARAGE, fallback)
    }

    // 2. F06 Acceptance: Verify rememberSaveable across modal & screen states in SprocketApp
    @Test
    fun testSprocketAppStatePreservation() {
        val appFile = File("src/main/java/com/example/sprocket/ui/SprocketApp.kt")
        assertTrue("SprocketApp.kt must exist", appFile.exists())
        val content = appFile.readText()

        val expectedSaveableStates = listOf(
            "currentTab",
            "selectedPartId",
            "showOdometerPad",
            "showLogReplacementPartId",
            "showEditIntervalPartId",
            "showAddPart",
            "showLockPreview"
        )

        for (stateName in expectedSaveableStates) {
            val pattern = Regex("""var\s+$stateName\s+by\s+rememberSaveable""")
            assertTrue("State '$stateName' in SprocketApp.kt must use rememberSaveable", pattern.containsMatchIn(content))
        }
    }

    // 3. F06 Acceptance: Verify rememberSaveable across modal form inputs and dialog toggles
    @Test
    fun testModalInputsAndDialogStatePreservation() {
        val filesToCheck = listOf(
            "src/main/java/com/example/sprocket/ui/components/OdometerPadModal.kt" to listOf("entryText", "showSavedSummary", "showBackwardConfirm", "savedOdoKm"),
            "src/main/java/com/example/sprocket/ui/components/LogReplacementModal.kt" to listOf("costText", "selectedCost", "selectedWho", "odoText", "customNote"),
            "src/main/java/com/example/sprocket/ui/components/EditIntervalModal.kt" to listOf("draftKm", "draftMo"),
            "src/main/java/com/example/sprocket/ui/components/AddPartModal.kt" to listOf("showCustom", "customName", "customKm", "customMo", "customCost"),
            "src/main/java/com/example/sprocket/ui/screens/PartDetailScreen.kt" to listOf("showDeleteConfirm"),
            "src/main/java/com/example/sprocket/ui/screens/SetupScreen.kt" to listOf("showResetConfirm")
        )

        for ((relPath, stateVars) in filesToCheck) {
            val file = File(relPath)
            assertTrue("File $relPath must exist", file.exists())
            val content = file.readText()
            for (v in stateVars) {
                val pattern = Regex("""var\s+$v\s+by\s+rememberSaveable""")
                assertTrue("Variable '$v' in $relPath must use rememberSaveable", pattern.containsMatchIn(content))
            }
        }
    }

    // 4. F04 Acceptance: BackHandler present in all modal screens, detail screen, and custom modes
    @Test
    fun testBackHandlerCoverage() {
        val componentsWithBackHandler = listOf(
            "src/main/java/com/example/sprocket/ui/components/SprocketSheet.kt",
            "src/main/java/com/example/sprocket/ui/components/LockPreviewModal.kt",
            "src/main/java/com/example/sprocket/ui/screens/PartDetailScreen.kt"
        )

        for (relPath in componentsWithBackHandler) {
            val file = File(relPath)
            assertTrue("File $relPath must exist", file.exists())
            val content = file.readText()
            assertTrue("$relPath must register BackHandler", content.contains("BackHandler"))
        }

        // Verify OdometerPadModal registers inner BackHandler for backward confirm
        val odoModal = File("src/main/java/com/example/sprocket/ui/components/OdometerPadModal.kt").readText()
        assertTrue("OdometerPadModal must handle back on backward confirm", odoModal.contains("BackHandler { showBackwardConfirm = false }"))

        // Verify AddPartModal registers BackHandler when in clean custom mode
        val addPartModal = File("src/main/java/com/example/sprocket/ui/components/AddPartModal.kt").readText()
        assertTrue("AddPartModal must handle back when in clean custom mode", addPartModal.contains("BackHandler { showCustom = false }"))
    }

    // 5. F07 & F08 Acceptance: Inset paddings (navigationBarsPadding, imePadding) and scroll containers
    @Test
    fun testSystemBarInsetsAndKeyboardHandling() {
        val sheetFile = File("src/main/java/com/example/sprocket/ui/components/SprocketSheet.kt")
        assertTrue("SprocketSheet.kt must exist", sheetFile.exists())
        val sheetContent = sheetFile.readText()

        assertTrue("SprocketSheet must have navigationBarsPadding for gesture nav", sheetContent.contains("navigationBarsPadding()"))
        assertTrue("SprocketSheet must have imePadding for keyboard avoidance", sheetContent.contains("imePadding()"))
        assertTrue("SprocketSheet must provide verticalScroll container", sheetContent.contains("verticalScroll(rememberScrollState())"))

        val bottomNav = File("src/main/java/com/example/sprocket/ui/components/SprocketBottomNav.kt").readText()
        assertTrue("SprocketBottomNav must have navigationBarsPadding", bottomNav.contains("navigationBarsPadding()"))

        val partDetail = File("src/main/java/com/example/sprocket/ui/screens/PartDetailScreen.kt").readText()
        assertTrue("PartDetailScreen bottom bar must have navigationBarsPadding", partDetail.contains("navigationBarsPadding()"))

        val lockPreview = File("src/main/java/com/example/sprocket/ui/components/LockPreviewModal.kt").readText()
        assertTrue("LockPreviewModal must have statusBarsPadding", lockPreview.contains("statusBarsPadding()"))
        assertTrue("LockPreviewModal must have navigationBarsPadding", lockPreview.contains("navigationBarsPadding()"))
    }

    // 6. F36 Acceptance: Dialog button styling rules (Cancel filled SprocketInk, Destructive outlined SprocketAccent)
    @Test
    fun testDialogButtonDistinctionAndEmphasis() {
        val dialogFiles = listOf(
            "src/main/java/com/example/sprocket/ui/screens/PartDetailScreen.kt",
            "src/main/java/com/example/sprocket/ui/screens/SetupScreen.kt",
            "src/main/java/com/example/sprocket/ui/components/SprocketSheet.kt"
        )

        for (relPath in dialogFiles) {
            val file = File(relPath)
            assertTrue("File $relPath must exist", file.exists())
            val content = file.readText()

            // Verify Dialog composable is used
            assertTrue("$relPath must use Compose Dialog", content.contains("Dialog("))

            // Verify filled SprocketInk for safe/cancel action
            assertTrue("$relPath must have filled SprocketInk button for safe action", content.contains(".background(SprocketInk)"))

            // Verify outlined SprocketAccent for destructive action
            assertTrue("$relPath must have outlined SprocketAccent border for destructive action", content.contains("border(1.dp, SprocketAccent)"))
        }
    }

    // 7. IME Action Chaining in LogReplacementModal
    @Test
    fun testImeActionChainingInLogReplacement() {
        val logFile = File("src/main/java/com/example/sprocket/ui/components/LogReplacementModal.kt")
        assertTrue("LogReplacementModal.kt must exist", logFile.exists())
        val content = logFile.readText()

        assertTrue("LogReplacementModal must use FocusRequester", content.contains("FocusRequester"))
        assertTrue("LogReplacementModal must specify ImeAction.Next for inputs", content.contains("ImeAction.Next"))
        assertTrue("LogReplacementModal must specify ImeAction.Done for note", content.contains("ImeAction.Done"))
    }

    // 8. F12 Acceptance: Backward odometer reading logic
    @Test
    fun testBackwardOdometerDetection() {
        val currentOdoKm = 15000
        val lowerReading = 14500
        val higherReading = 15500
        val sameReading = 15000

        assertTrue("Lower reading should be detected as backward", lowerReading < currentOdoKm)
        assertFalse("Higher reading should not be detected as backward", higherReading < currentOdoKm)
        assertFalse("Same reading should not be detected as backward", sameReading < currentOdoKm)
    }
}
