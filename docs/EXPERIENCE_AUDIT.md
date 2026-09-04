# Sprocket — UI / UX / usability audit

Reviewed at commit `74cab79`: 27 Kotlin files, 5,823 lines. Full source read; no device run
(the review environment had no Android SDK).

45 findings. IDs are stable and are referenced by `REMEDIATION_PHASES.md`.

## Summary

| Band | Count | Character |
|---|---|---|
| Critical | 5 | The app states things that are false |
| Blocking | 15 | A real rider hits these in week one |
| Misleading | 9 | The numbers don't mean what they say |
| Accessibility | 7 | Near-unusable without sight or fine motor control |
| Platform | 9 | Platform integration and polish |

Three problems sit above the rest and are the same problem in different clothes: **the app tells
the user things that aren't true.** The camera button reads no camera, the reminder settings
schedule nothing, and snooze claims two weeks it does not implement. Each is cheap to fix by
removing the claim and expensive to leave, because the odometer number seeds every wear
percentage, forecast and cost figure downstream.

Below that is ordinary Android correctness — Back exits the app from every sheet, nothing
survives rotation, sheets render under the gesture bar and behind the keyboard. Below that is
the structural gap: there is no way to record a service that happened in the past, and no way to
tell the app which bike this is or how far you actually ride.

---

## Critical — the app states things that are false

### F01 · The SCAN button invents an odometer reading
`OdometerPadModal.kt:357-366` — `displayCurrent + toDisplayDistance(612, unit)`

Tapping SCAN opens no camera. It writes last reading + 612 km into the field, sets
`scanned = true`, and the hint then reads "Read off your photo — check it against the dash."
The fabricated number becomes the basis of every wear percentage, forecast and cost figure.

**Fix** Remove the button, or wire it to CameraX + ML Kit with recognised digits shown for
confirmation before they enter the field.

### F02 · Reminders and alerts are settings with nothing behind them
`AndroidManifest.xml` — no `POST_NOTIFICATIONS`, no WorkManager / AlarmManager / receiver anywhere

"Remind me", the day selector, three alert switches and "PREVIEW THE NOTIFICATION" persist state
that nothing reads. The product's stated promise — one push a month — is not implemented.
"Payday" is undefined; there is no date behind it.

**Fix** Schedule with WorkManager, request the runtime permission when the user enables
reminders, replace "Payday" with a real day. Until then the section should not accept input.

### F03 · Snooze doesn't snooze, and can't be undone
`SprocketRepository.kt:138` · `GarageScreen.kt:75` — the only read of `isSnoozed` in the app

The toast says "SNOOZED FOR 2 WEEKS"; a boolean with no timestamp is set and the part stops being
eligible for the hero card, permanently, until a replacement is logged. The row still shows red
with no snooze marker, and nothing can un-snooze it.

**Fix** Store `snoozedUntil` as a date, expire it in the wear engine, badge snoozed rows, allow
cancel.

### F04 · System Back exits the app from every screen and every sheet
No `BackHandler` in the codebase; Part Detail and all five modals are in-composition overlays

Back closes Sprocket from the part detail, any sheet, or a delete confirmation — taking typed
input with it.

**Fix** A `BackHandler` per overlay, innermost first; move confirmations into real `Dialog`s.

### F05 · Custom intervals typed in miles are stored as kilometres
`AddPartModal.kt:225` label `"Interval (${unit.label})"` → `:266-269` passed into `intervalKm`

Preset rows convert correctly; the custom form does not. A 5,000 mi interval flags overdue at
3,107 mi. Silent and permanent.

**Fix** Convert on save, and put the conversion in one shared place.

---

## Blocking — a real rider hits these in week one

### F06 · Nothing survives a rotation
All UI state uses `remember`; `rememberSaveable` appears nowhere. Rotation or process death
resets the tab, closes the open sheet, and discards typed input.

### F07 · Sheet buttons sit underneath the gesture bar
No modal calls `navigationBarsPadding()` (only `SprocketBottomNav.kt:55`, `PartDetailScreen.kt:475`).
SAVE READING, SAVE TO SERVICE LOG and ADD TO GARAGE render behind the system bar.

### F08 · The keyboard covers the fields it is typing into
Manifest sets `adjustResize`, but `enableEdgeToEdge()` opts the window out of fitting system
windows and no sheet calls `imePadding()`. Add Part's custom form has no scroll container at all.

### F09 · There is no way to record a service that happened in the past
`LogReplacementModal.kt` has no date field; `SprocketRepository.kt:96-104` stamps the current
month. Every log is dated today and resets the wear clock to today. Combined with F17 (seeded
parts marked serviced today), the first week of data is fiction and the age half of the wear
model is meaningless until a year of real use accrues. **This is the structural gap in the
product.**

### F10 · Changing an interval takes seventeen taps and can't be undone
`EditIntervalModal.kt` — ±1000 km / ±6 mo steppers, no text entry, no press-and-hold. Min clamps
of `max(1000, …)` / `max(6, …)` mean an interval that exists can never be removed. In miles the
step is a ~621 mi jump, so round mile figures are unreachable.

### F11 · An unchanged odometer reading is silently rejected
`OdometerPadModal.kt:68` — `isValid` requires the value to differ from last month. A bike that
sat in the garage can't be logged, and the disabled button explains nothing.

### F12 · One stray tap discards a filled-in form
`SprocketApp.kt:214-316` — every scrim is `clickable { dismiss() }` with no dirty check. The
sheets are also not draggable and have no handle.

### F13 · "Do this next" follows the sort order, not urgency
`GarageScreen.kt:75` takes the first overdue from the already-sorted list. Sort A–Z and the hero
promotes "Air filter" over a belt 40% past due.

### F14 · Deleting a part silently deletes its money
`SprocketRepository.kt:145-149` — `deletePart` also filters `_history`, so all-time spend changes
retroactively, with no undo and no export.

### F15 · A logged service can never be corrected
No edit or delete path for a `ServiceRecord`. A wrong cost or odometer is permanent, and it has
already reset the part's wear clock.

### F16 · "Reset all data" restores someone else's motorcycle
`SprocketRepository.kt:212-217` reinstates `defaultParts` and `VehicleState()`. The dialog
promises erasure; the user gets an NMAX '16 with five parts marked serviced today.

### F17 · There is no onboarding — first launch is a stranger's bike
`VehicleState.kt:21-26` — `"NMAX '16"`, `odometerKm = 0`, and a hard-coded `lastReadYear = 2026` /
`lastReadMonth = 9` that drives a stale "READING DUE" banner in any other month.

### F18 · Every forecast rests on a constant of 560 that is neither learned nor editable
`VehicleState.kt:26`. It drives every "reach the interval in about N months", the odometer
estimate, and both Costs headlines. It is displayed as fact ("averaging 560 km a month") and no
screen edits it. `prevOdometerKm` is stored (`Repository:88`) and never read.

### F19 · The settings the UI refers to don't exist
Vehicle name is printed in the header and an alert row and can't be changed. The soon-threshold
is displayed live ("A part hits 80%") and can't be changed. Currency is hard-coded. A part's name
and standard cost can't be edited after creation.

### F20 · A newly added part lands nowhere the user can see
`AddPartModal.kt:123-126` · `SprocketRepository.kt:151-166` — appended at 0% wear, which under the
default sort puts it off-screen at the bottom. Adding the same preset twice creates a silent
duplicate.

---

## Misleading — the numbers don't mean what they say

### F21 · Monthly spend is divided by 24 regardless of how long the app has been used
`CostsScreen.kt:70-72`. After two months, "per month" understates by 12×. "Per 1,000 km ridden"
divides by a hypothetical distance from the 560 constant rather than distance actually travelled.

### F22 · The wear bar tops out, so 250% looks the same as 100%
`WearProgressBar.kt:26` — `progress.coerceIn(0f, 1f)`.

### F23 · The "reading saved" confirmation always says +0
`OdometerPadModal.kt:119` — the delta is computed against a baseline the save just updated. The
month label is wrong for the same reason.

### F24 · Overdue distance is shown without a unit
`WearEngine.kt:148` renders `"−1.234"` next to age-driven parts reading `"3 mo over"`.

### F25 · Currency, number format and half the vocabulary are hard-coded to one market
`WearEngine.kt:73-90` (`Locale.GERMAN` grouping, literal `"Rp "`), `LogReplacementModal.kt:59,66,186`
(`"Bengkel"`, `"75rb"`), `CostsScreen.kt:150` (`"per 1.000"`). A miles user gets
"Rp 12.500 per 1.000 mi ridden".

### F26 · The Costs insight matches parts by display name
`CostsScreen.kt:102` — `parts.find { it.name == topExpense?.name }`.

### F27 · A part with no intervals prints "null months left"
`WearEngine.kt:175`.

### F28 · The service odometer isn't validated against anything
`LogReplacementModal.kt:337-343`. Logging at 200,000 km on a 40,000 km bike puts `lastKm` above
the odometer; `usedKm` floors at zero and the part reads 0% used indefinitely.

### F29 · Future-dated records count toward "last 24 months"
`CostsScreen.kt:64-66` · `WearEngine.kt:98` — `calculateMonths` returns negatives; the filter is
`<= 24`.

---

## Accessibility

Everything is a hand-built `Box` with a `clickable`. That is a legitimate way to get this look,
but every semantic Material supplies for free has to be added back, and none has been.

### F30 · Nothing is announced to a screen reader
Four `contentDescription`s in the app, all on icons duplicating adjacent text. No `Role`, no
`stateDescription`, no `mergeDescendants`, no `toggleable`/`selectable`. Setup toggles are
unlabelled containers with no on/off state; the bottom nav announces no selection; the keypad's
"C" and "⌫" are silent; the wear bar is an undescribed `Canvas`; each part row reads as six
disconnected fragments with no indication it is one tappable thing.

### F31 · Most touch targets are well under 48 dp
"TAP TO UPDATE ✎" ≈ 30×20 dp (`GarageScreen.kt:104-117`); "← GARAGE" is bare text ≈ 16 dp tall
(`PartDetailScreen.kt:115-122`); "Cancel" in all four sheets; interval steppers 44 dp. Only the
sort control and the Setup toggle set a minimum.

### F32 · The type floor is 9 sp, uppercase, letter-spaced
Status counters 9.5 sp / 1.4 sp tracking, driver tags 9 sp, `Type.kt` labelSmall 9 sp. Sizes are
literal `sp` at every call site rather than routed through the existing typography scale.

### F33 · Two of the three statuses are near-identical greys
`GarageScreen.kt:334-338` — OVERDUE `accent`, DUE SOON `ink` (#201E1D), HEALTHY `neutral400`
(#595555). The only other cue is a 6 dp square. Overdue is red-only with no shape or icon cue.

### F34 · Fixed-width slots clip at larger font scales
`GarageScreen.kt:463` (40 dp), `CostsScreen.kt:206,231` (110/56 dp), `PartDetailScreen.kt:410`
(60 dp); `maxLines = 1` with no `TextOverflow.Ellipsis` so text cuts mid-glyph. Part names in the
garage row have no line limit at all.

### F35 · Sub-settings stay live when their parent switch is off
`SetupScreen.kt:121-196` — the day selector and three alert rows stay enabled at full opacity when
"Remind me" is off.

### F36 · The confirmations aren't dialogs
`PartDetailScreen.kt:515-596` and `SetupScreen.kt:355-436` are `Box` overlays: no Back dismissal,
no focus trap, no alert semantics. Both give the destructive choice the filled accent treatment
while Cancel is a quiet outline.

---

## Platform & polish

### F37 · White flash on cold start in dark mode
`themes.xml` — `android:Theme.Material.Light.NoActionBar`, no `values-night`, no window background.

### F38 · Status bar icons can end up invisible
`MainActivity.kt:22` — `enableEdgeToEdge()` is called once, styled from the system configuration,
before the app's own theme preference is read.

### F39 · The notification preview is invisible in dark mode and fictional in both
`LockPreviewModal.kt:39` paints `SprocketDarkBg` then writes the clock in `SprocketBg` — the same
colour in dark theme. Content is hard-coded: "TUESDAY 1 SEPTEMBER", "9:41", "The CVT belt is
already past due" regardless of state.

### F40 · Four different input paradigms across four sheets
Custom keypad (odometer), hand-rolled `BasicTextField` (log), Material `OutlinedTextField`
(add part), steppers (interval). The keypad blocks paste, hardware keyboards and accessibility
input, and shows the estimate in placeholder grey inside the value box as if it were a value that
will be saved.

### F41 · Nothing moves
One `AnimatedVisibility` in the project, on the toast. Tab switches, the detail overlay, every
sheet, the toggle thumb and the wear bars are instant cuts.

### F42 · Toasts announce destruction instead of offering to undo it
`SprocketApp.kt:72-80,330-355` — custom bar, 2.4 s, no action, padded 90 dp from the bottom
regardless of the nav bar, and not a live region. Four irreversible actions are acknowledged by a
message that can't be acted on.

### F43 · Every string is hard-coded in Kotlin
`strings.xml` contains one entry. No localisation; plurals hand-branched in `WearEngine`;
`supportsRtl="true"` declared against a UI never laid out right-to-left.

### F44 · A data-version bump wipes the user's data
`SprocketRepository.kt:54-63` — any stored version below current resets to defaults and saves. The
migration path is deletion, and there is no export or backup.

### F45 · The instrumented test doesn't compile
`MainScreenTest.kt:16` references a `MainScreen` composable that does not exist.

---

## What is holding up — do not lose these in the rework

- **The dual distance/age wear model.** Tracking both limits, taking the worse, and naming the
  driver is better thinking than most maintenance apps manage.
- **Solid vs. hatched bars.** A non-colour encoding of what drives the wear, applied
  consistently. Extend it to the status system rather than leaving it as the one place it appears.
- **The interval editor's impact preview.** Showing a setting's consequence before it is
  committed; worth spreading to the odometer and unit changes.
- **The backwards-odometer correction flow.** Detects a lower reading, relabels the button, and
  explains what recalculating backwards means. The model for everything in the Blocking band.
- **The palette.** Two full themes with contrast reasoned about in comments and a token layer that
  makes them swappable. The dark-mode bugs are the few places that bypass it.
- **The voice.** "Three seconds. Every part re-dates itself." Specific, confident, never chirpy —
  and the reason the false claims cost so much.
