# Sprocket — remediation phases

Nine phases that close all 45 findings in `EXPERIENCE_AUDIT.md`. Each phase is scoped to one
working session: it lands on its own, leaves the app building and usable, and can be verified
before the next one starts.

**Order matters.** Phase 0 changes the data model that Phases 3–5 depend on. Phases 1 and 2 are
independent of Phase 0 and of each other, so they can be done in parallel or first if you want
early wins. Everything from Phase 3 onward assumes Phase 0 has landed.

| # | Phase | Findings | Depends on |
|---|---|---|---|
| 0 | Data model & repository foundation | F03 F05 F14 F18 F21 F23 F44 (data half) | — |
| 1 | Honesty pass | F01 F02 F03 F39 | — |
| 2 | Android correctness | F04 F06 F07 F08 F12 F36 | — |
| 3 | Arithmetic, units and formatting | F05 F11 F13 F21–F29 | 0 |
| 4 | Onboarding and vehicle settings | F09 F16 F17 F18 F19 F20 | 0 |
| 5 | Recoverability | F10 F14 F15 F42 F44 | 0, 2 |
| 6 | Accessibility and the input system | F30–F35 F40 | 2 |
| 7 | Strings and localisation | F43 | 6 |
| 8 | Platform, motion and tests | F37 F38 F41 F45 | 2 |

Every phase ends with `./gradlew testDebugUnitTest` green and the app installed and exercised by
hand. Phases 0 and 3 are almost entirely pure-JVM logic and should gain real unit tests; the rest
are UI work verified on a device or emulator.

---

## Phase 0 — Data model & repository foundation

Everything downstream needs a data layer that records odometer history, expires snoozes, survives
migrations, and converts units in exactly one place. Do this first and the later phases become
small.

**Findings closed:** the data half of F03, F05, F14, F18, F21, F23, F44.

### Target model

```kotlin
// NEW — the app currently throws away every reading but the latest
data class OdometerReading(
    val id: String,
    val year: Int,
    val month: Int,
    val odometerKm: Int
)

data class Part(
    val id: String,
    val name: String,
    val intervalKm: Int?,          // ALWAYS kilometres, whatever the display unit
    val intervalMonths: Int?,
    val lastKm: Int,
    val lastYear: Int,
    val lastMonth: Int,
    val standardCost: Long,
    val snoozedUntilEpochDay: Long? = null,  // replaces isSnoozed
    val isArchived: Boolean = false          // replaces destructive delete
)

data class VehicleState(
    val vehicleName: String = "",
    val odometerKm: Int = 0,
    val lastReadYear: Int = 0,
    val lastReadMonth: Int = 0,
    val monthlyAverageOverrideKm: Int? = null,  // null = learn it from readings
    val unit: DistanceUnit = DistanceUnit.KM,
    val currencyCode: String = "IDR",
    val soonThreshold: Float = 0.80f,
    val reminderDayOfMonth: Int = 1,            // replaces the "1st"/"Payday" strings
    val remindersEnabled: Boolean = true,
    val alerts: AlertsConfig = AlertsConfig(),
    val themePreference: String = "SYSTEM",
    val onboardingComplete: Boolean = false
)
```

Delete `prevOdometerKm` — the readings list supersedes it.

### Work

1. **One conversion helper, used everywhere.** Add `object Units { fun toDisplay(km: Int, unit): Int;
   fun toKm(display: Int, unit): Int }`. Replace every inline `/ unit.toKmFactor` and
   `* unit.toKmFactor` in `OdometerPadModal`, `LogReplacementModal` and `WearEngine`. This is what
   makes F05 impossible to reintroduce. Round-trip must be stable: `toDisplay(toKm(x)) == x`.
2. **Reading history.** `_readings: StateFlow<List<OdometerReading>>`, appended by
   `updateOdometer`, persisted. One reading per year+month — a second reading in the same month
   replaces it.
3. **Learned monthly average.** `fun monthlyAverageKm(): Int` = total distance across the readings
   window divided by months spanned, falling back to the override, then to 560 when there are
   fewer than two readings. Expose how many readings it is based on so the UI can say so.
4. **Snooze with an expiry.** `snoozePart(id, days: Int = 14)` writes
   `LocalDate.now().plusDays(days).toEpochDay()`; add `unsnoozePart(id)`. Add
   `Part.isSnoozed(today: LocalDate): Boolean`.
5. **Archive instead of delete.** `archivePart(id)` sets the flag and keeps every `ServiceRecord`.
   Keep `deletePartAndHistory(id)` as a separate, explicitly-named call. Parts flows should expose
   active and archived separately.
6. **Real migrations.** Replace the wipe at `SprocketRepository.kt:54-63` with a version chain:
   `v2 → v3` maps `isSnoozed` to an expired snooze, seeds `readings` from the current odometer,
   and sets `onboardingComplete = true` so existing users are never dumped into onboarding.
   A stored version *above* current should load read-only rather than reset.
7. **Export / import.** `exportJson(): String` and `importJson(String): Result<Unit>` over
   `{version, vehicle, parts, history, readings}`. Validate on import; never partially apply.
8. **Seed data is opt-in.** Split `resetToDefaults()` into `clearAll()` (empty garage,
   `onboardingComplete = false`) and `loadSampleGarage()`.

### Acceptance

- Unit tests for: unit round-trip, learned average with 1 / 2 / 12 readings, snooze expiry at the
  boundary, v2→v3 migration from a real captured `SharedPreferences` payload, export→import
  round-trip equality, same-month reading replacement.
- No conversion arithmetic outside `Units`. `grep -rn "toKmFactor" app/src/main` returns only
  `Units.kt` and the enum declaration.

---

## Phase 1 — Honesty pass

The three false claims, plus the fake preview. Independent of everything else; smallest phase and
the highest value per line changed. Ship it alone if you want one quick win.

**Findings closed:** F01, F02, F03 (UI half), F39.

### Work

1. **F01 — SCAN.** Delete the button and the `scanned` state from `OdometerPadModal.kt:356-384`
   and the "Read off your photo" branch of `hintText`. Do not leave a disabled stub. (If OCR is
   actually wanted, that is its own project: CameraX + ML Kit text recognition, camera permission,
   and a confirmation step showing the recognised digits before they enter the field. Do not ship
   the halfway version.)
2. **F02 — reminders.** Either implement or withdraw; do not leave the current state.
   - *Implement:* add WorkManager, a `MonthlyReminderWorker` scheduled for
     `reminderDayOfMonth`, a notification channel, and a runtime `POST_NOTIFICATIONS` request
     triggered by the user enabling the toggle (not at launch). The worker reads the repository and
     honours the three alert switches. Replace the `"1st"/"15th"/"Last"/"Payday"` strings with a
     real day-of-month picker.
   - *Withdraw:* replace the section with a single line saying reminders are coming, and remove the
     controls. Keep the preview only if it is honest (below).
3. **F03 — snooze.** With Phase 0's `snoozedUntilEpochDay` in place: badge snoozed rows in the
   garage list with the date they wake, make the badge tappable to cancel, and make the toast text
   match what actually happened. If Phase 0 has not landed, do the copy fix now — the button and
   toast must not claim two weeks.
4. **F39 — preview.** Fix the dark-mode collision: the mock lock screen is intentionally dark, so
   its chrome must use fixed light literals, not the theme-aware `SprocketBg` which resolves to the
   same colour it is painted on. Use the real date and time and the actual worst part. If reminders
   were withdrawn in step 2, delete this modal instead.

### Acceptance

- `grep -rn "612\|scanned" app/src/main` returns nothing.
- Reminders either fire on a device with the app backgrounded, or no reminder controls accept input.
- The preview is legible in both themes and names a part that is genuinely overdue.

---

## Phase 2 — Android correctness

Mechanical, no design decisions, and it removes the largest class of lost-work moments.

**Findings closed:** F04, F06, F07, F08, F12, F36.

### Work

1. **One sheet container.** Every modal currently repeats its own scrim, its own
   `clickable(enabled = false)` hack, and its own padding. Replace with:

   ```kotlin
   @Composable
   fun SprocketSheet(
       onDismissRequest: () -> Unit,
       isDirty: Boolean = false,
       content: @Composable ColumnScope.() -> Unit
   )
   ```

   which owns: the scrim, `BackHandler`, `navigationBarsPadding()`, `imePadding()`, a scrolling
   body, a drag handle, and a "Discard changes?" confirmation when `isDirty` and the user taps the
   scrim or presses Back. Migrate all five modals onto it. This closes F07, F08 and F12 at once and
   makes the fourth and fifth ones impossible to get wrong.
2. **Back.** `BackHandler` on the part-detail overlay and on each dialog, innermost first.
3. **State.** `rememberSaveable` for `currentTab`, `selectedPartId`, every `show*` flag in
   `SprocketApp.kt:61-67`, and every form field inside the modals. `SprocketTab` needs to be
   saveable — store the name and resolve it back.
4. **Real dialogs.** Move the delete confirmation (`PartDetailScreen.kt:515-596`) and the reset
   confirmation (`SetupScreen.kt:355-436`) into `Dialog`. Swap the button emphasis: Cancel gets the
   filled treatment, the destructive action gets the outline.
5. **IME.** `imeAction = Next` chaining cost → odometer → note in the log sheet, `Done` on the last.

### Acceptance

- Back from every sheet, the detail screen and both dialogs returns one level instead of exiting.
- Rotate with a half-filled service log: every field survives.
- On a gesture-nav device, no save button is under the system bar; with the keyboard open, every
  field and the save button are reachable.
- Tapping the scrim on a dirty form asks before discarding.

---

## Phase 3 — Arithmetic, units and formatting

What makes the numbers on screen defensible. Almost all of it is pure logic and should be
unit-tested.

**Findings closed:** F05, F11, F13, F21, F22, F23, F24, F25, F26, F27, F28, F29.

### Work

1. **F05** Custom interval converts through `Units.toKm` before it reaches `intervalKm`.
2. **F11** Accept a reading equal to the previous one — it still advances the date and every age
   clock. Give the disabled state a reason whenever it does apply.
3. **F13** Choose the hero card from the unsorted set by wear percentage, not from the sorted list.
4. **F21** Spend window = months elapsed since the first record, capped at 24; label the window
   ("last 7 months"). Distance for the per-1,000 figure comes from the real odometer delta across
   the readings, not `monthlyAverage × 24`.
5. **F22** Render overflow past 100%: a marked interval line with a compressed over-range, or a
   second-pass overlay. A 250% part must not look like a 100% part.
6. **F23** Capture the previous reading and date into local state *before* calling save, and show
   those in the summary.
7. **F24** Append the unit to the overdue distance label; use one over/under phrasing for both
   drivers.
8. **F25** Add `currencyCode` to settings; format through `NumberFormat.getCurrencyInstance` for
   the chosen locale. Derive cost presets from the part's standard cost (e.g. 0.5×, 1×, 1.5×)
   instead of hard-coded rupiah. Rename `"Bengkel"` to `"Workshop"`. Remove the `"1.000"` literal
   from copy.
9. **F26** Carry the part id through `PartSpendSummary` instead of matching on name.
10. **F27** Branch the forecast on the both-intervals-null case.
11. **F28** Warn when the service odometer is far from the current reading; when it is higher,
    offer to advance the odometer too.
12. **F29** Bound the 24-month filter at both ends.

### Acceptance

- Unit tests for: the spend window with 1 / 7 / 30 months of history, per-1,000 against real
  odometer deltas, the overflow bar at 100 / 150 / 250%, the forecast with each interval
  combination including both-null, currency formatting for at least two locales.
- Switch to miles and walk every screen: no figure is unlabelled or in the wrong scale.

---

## Phase 4 — Onboarding and vehicle settings

The biggest single jump in real-world usefulness. Until this lands, the app is a demo of a
stranger's motorcycle.

**Findings closed:** F09, F16, F17, F18, F19, F20.

### Work

1. **F17 — first run.** Three steps, shown when `onboardingComplete` is false: what do you ride
   (name, unit) → what does the odometer read → which of these parts do you track, and when was
   each last done. Write the answers and set the flag. Existing users must never see it (Phase 0
   migration sets the flag).
2. **F09 — service dates.** A month/year picker in the log sheet, defaulting to now, and the same
   picker in Add Part ("when was this last done?"). This is what makes it possible to enter a real
   logbook rather than only accrue one. The picker must not allow future dates.
3. **F19 — vehicle settings.** A section in Setup for name, unit, monthly average (with "learned
   from N readings" as the default state and an override), soon-threshold, and currency. Add an
   edit path for a part's name and standard cost next to the interval row on the detail screen.
4. **F18 — the average.** Show where the number comes from wherever it is displayed:
   "averaging 560 km a month (from 6 readings)" or "(you set this)".
5. **F16 — reset.** Wire the reset action to `clearAll()` and re-run onboarding. Offer
   `loadSampleGarage()` as a separate, differently-named action.
6. **F20 — new part lands somewhere.** Scroll to and briefly highlight the new row; warn on a
   duplicate name instead of silently creating one.

### Acceptance

- Fresh install → onboarding → a garage containing only the user's own parts with their own dates.
- Upgrade from a v2 install → no onboarding, no data loss.
- A service can be logged for eight months ago and the part's age clock reflects it.

---

## Phase 5 — Recoverability

A maintenance log is only worth keeping if a mistake in it is not permanent.

**Findings closed:** F10, F14, F15, F42, F44 (UI half).

### Work

1. **F42 — undo.** Replace the custom toast (`SprocketApp.kt:72-80,330-355`) with a `Snackbar`
   carrying an Undo action for every destructive or state-resetting operation: delete/archive,
   log replacement, snooze, reset, interval change. The repository holds the previous state until
   the snackbar expires. Make it a live region so it is announced.
2. **F14 — archive.** Point the part-detail destructive action at `archivePart`; keep the history.
   Offer "delete history too" as a separate explicit choice. Add an archived-parts list in Setup
   with restore.
3. **F15 — editable history.** Make history rows tappable: edit cost, date, odometer, performer and
   note, or delete the record with the part's previous service state restored.
4. **F10 — the interval editor.** Numeric entry with the steppers as fine adjustment, a
   "not tracked" option on each axis so an interval can be removed, and a step size expressed in
   the displayed unit.
5. **F44 — export/import UI.** Surface Phase 0's export and import in Setup, with a share sheet
   and a file picker. This is also the answer to "I got a new phone".

### Acceptance

- Every destructive action can be undone from its snackbar.
- Deleting a part no longer changes historical spend unless the user explicitly asks it to.
- A wrong cost logged last month can be corrected without touching the part.
- Export on one install, import on another, identical state.

---

## Phase 6 — Accessibility and the input system

Everything is a hand-built `Box`. This phase adds back what Material would have supplied and
collapses four input paradigms into one.

**Findings closed:** F30, F31, F32, F33, F34, F35, F40.

### Work

1. **A component layer.** `SprocketButton`, `SprocketToggle`, `SprocketSegmented`,
   `SprocketNumberField`, `SprocketTextField`, `SprocketListRow`. Each carries its own semantics
   and a 48 dp minimum touch target internally, so no call site has to remember. Replace every
   ad-hoc `Box { clickable }` with one of them.
2. **F30 — semantics.** `Role.Button` / `Role.Tab` / `Role.Switch` / `Role.RadioButton` on the
   corresponding controls; `stateDescription` on toggles and segmented items;
   `Modifier.semantics(mergeDescendants = true)` with one composed `contentDescription` per garage
   row ("CVT belt, overdue on distance, 1,234 km past the interval, 116% consumed");
   `progressSemantics` on the wear bar; labels on the keypad keys if the keypad survives Phase 6
   step 6.
3. **F31 — targets.** `minimumInteractiveComponentSize()` on every clickable. The visual box stays
   whatever the design wants; only the touch area grows. Priority: the detail-screen back control
   and every sheet Cancel.
4. **F32 — type floor.** Raise the floor to 11 sp and route every `Text` through
   `MaterialTheme.typography` instead of literal `sp` at the call site, so it is adjustable in one
   file.
5. **F33 — status.** Give the three statuses distinct fill, weight and glyph, not two near-identical
   greys. Add a `warning` token to both palettes for DUE SOON. Reuse the solid/hatched idea so
   status survives greyscale.
6. **F40 — one input paradigm.** Delete the Material `OutlinedTextField`s in `AddPartModal` and the
   hand-rolled `BasicTextField`s in `LogReplacementModal` in favour of `SprocketTextField` /
   `SprocketNumberField`. Keep the custom keypad only if it is demonstrably faster, and never as the
   only input path — the field must also accept the system keyboard. Remove the placeholder-grey
   estimate shown inside the value box; an estimate that will not be saved must not look like a
   value that will.
7. **F34 — layout.** Replace fixed `dp` widths with intrinsics or `weight`; add
   `TextOverflow.Ellipsis` everywhere `maxLines` is set; give part names a line limit.
8. **F35 — dependent settings.** Disable and dim the day selector and alert rows when reminders are
   off, or nest them so the relationship is structural.

### Acceptance

- TalkBack: every control announces name, role and state; each garage row is one node.
- Accessibility Scanner reports no target below 48 dp.
- The app is usable at 200% font scale with nothing clipped.
- Screenshots in greyscale still distinguish the three statuses.

---

## Phase 7 — Strings and localisation

Do this after Phase 6 so the strings are extracted once, from settled markup.

**Findings closed:** F43.

### Work

- Extract every user-facing string to `strings.xml`. There are roughly 300 across twelve files.
- Use `plurals` for the hand-branched cases in `WearEngine` ("month"/"months", "N mo").
- Move the composed sentences (`subLabel`, `forecastText`, `remainLabel`) to parameterised
  resources rather than Kotlin concatenation, so word order can change per language.
- Verify RTL with `android:supportsRtl="true"` actually honoured: run with a pseudo-locale and fix
  any hard-coded start/end assumptions.
- Keep the voice. The copy is one of the app's real assets; the goal is to make it editable, not to
  rewrite it.

### Acceptance

- No user-facing literal remains in a `.kt` file.
- The app runs in a pseudo-locale with no layout breakage and no untranslated string.

---

## Phase 8 — Platform, motion and tests

Last, deliberately.

**Findings closed:** F37, F38, F41, F45.

### Work

1. **F37** A DayNight parent for `Theme.Sprocket`, a `values-night` variant, and a window background
   matching the app ground so there is no white flash on cold start.
2. **F38** Re-apply `enableEdgeToEdge` with a `SystemBarStyle` derived from the *resolved* dark
   flag, inside an effect keyed to it, so an in-app theme choice restyles the system bars.
3. **F41** Motion: slide/fade the part detail and the sheets, crossfade tab content, and animate the
   wear bars from their previous values after a new reading — the monthly check-in is the whole
   product and should feel like something. Honour `prefers-reduced-motion` via
   `LocalAccessibilityManager` / `Settings.Global.ANIMATOR_DURATION_SCALE`.
4. **F45** Delete `MainScreenTest`. Replace with a smoke test of the monthly loop: enter a reading,
   assert a part changes status, log a replacement, assert it resets.

### Acceptance

- Cold start in dark mode shows no light flash; system bar icons are legible under all three theme
  settings on both a light and a dark system.
- Animations respect the system animation scale.
- `./gradlew connectedDebugAndroidTest` passes.

---

## Handing a phase to another session

Each phase is written to be pasted as a brief. A useful prompt shape:

> Work on the Sprocket Android app. Read `docs/EXPERIENCE_AUDIT.md` for context and
> `docs/REMEDIATION_PHASES.md` for the plan. Implement **Phase N** only — do not start later
> phases. The findings it closes are listed in that section, each with the exact file and line
> where the problem lives. Meet the acceptance criteria at the end of the section, run
> `./gradlew testDebugUnitTest`, and install and exercise the app by hand before you report done.

Two constraints worth repeating to whoever picks this up:

- **Do not lose what already works.** The dual distance/age wear model, the solid-vs-hatched bar
  encoding, the interval editor's impact preview, the backwards-odometer correction flow, the
  two-theme palette and the copy voice are the app's real assets. Several phases touch them; none
  should flatten them.
- **The audit's line numbers are from commit `74cab79`.** They will drift as phases land. Trust the
  described symptom over the line number.
