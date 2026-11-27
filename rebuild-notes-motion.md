# Motion + haptics — rebuild notes (wave 2)

Branch: `rebuild/motion` (from `rebuild/merge`).
Rulebook: DESIGN-RESEARCH.md §4.5, rules 53–60. Audit offence C.1
(no motion system, no haptics) and decision F.11.

## What was built

Two new files are the system; everything else is small wiring:

- `ui/Motion.kt` — the single source of truth. Every duration,
  easing curve, spring, and transition pattern in the app lives
  here and nowhere else. Verified by grep: no `tween(`/`spring(`
  literal remains outside this file.
- `ui/Haptics.kt` — the rule 59 haptic map (`MarlowHaptics`),
  plus `LocalMarlowHaptics`, provided at the HomeScreen root and
  read by HomeGrid, FolderDialog, and SwitchRow. No raw
  `Vibrator.vibrate(` anywhere (grep-verified).

## 1. Motion table as implemented (§4.5)

No duration was invented — every value is from the table. Where
the table gives a range (stagger 30–60 ms) the midpoint, 45 ms,
is used and named.

| Transition | Implemented as | Where |
|---|---|---|
| Drawer enter / exit | Shared-axis z: fade + scale 80%→100% / 100%→80%, 400 ms Emphasized decelerate in / 200 ms Emphasized accelerate out | `Motion.drawerEnter/drawerExit`, `HomeScreen` (`AnimatedVisibility`, not a bare `if`, so the exit plays) |
| Screen transition, begin-and-end on screen | Emphasized 500 ms (`beginEndSpec`) | `Motion.kt` (spec available; no begin-and-end screen transition exists yet to wire it to) |
| Small utility transition | Standard 300 ms; enter 250 ms decelerate, exit 200 ms accelerate (`utilitySpec/utilityEnterSpec/utilityExitSpec`) | `Motion.kt` (specs available; see §5 for unwired items) |
| Folder drop-in / initial-item / final-item | 400 ms / 350 ms / 200 ms | `FolderView` (`folderDropInSpec`, `folderContentsSpec`, `folderExitSpec`) |
| Folder contents stagger | Container first, contents 45 ms after | `FolderView` (`contentAlpha`, 45 ms delay inside `folderContentsSpec`) |
| Page move (programmatic, incl. adjacent-page drop) | 300 ms Emphasized decelerate | `HomeScreen.scrollPagerTo` (used by `scrollHomeBy`) |
| Widget resize snap | 66% of a cell (existing threshold logic, unchanged), 150 ms snap | `HomeGrid` (`animateContentSize` with `Motion.widgetResizeSnapSpec`, widget kinds only) |
| Drawer search transition | 300 ms fade + expand/shrink vertically | `Drawer.kt` (`searchField`, both TOP and BOTTOM placements) |
| Drag lift | Spring, `StiffnessMediumLow` (the stiffness the grid already used) | `HomeGrid` (`Motion.dragLiftSpring`) |
| Reorder hover | 650 ms before neighbours slide aside (existing timer, unchanged) | `HomeGrid` |
| Folder hover-open | 800 ms (existing timer, unchanged) | `HomeGrid` |
| Empty-page snap-off / fade | 400 ms / 150 ms | Specified in `Motion.kt` only — NOT wired (see §5) |
| Easing curves | Enter Emphasized decelerate `cubic-bezier(0.05, 0.7, 0.1, 1.0)`; exit Emphasized accelerate `cubic-bezier(0.3, 0, 0.8, 0.15)`; Emphasized `cubic-bezier(0.2, 0, 0, 1.0)`; never linear, never reversed | `Motion.kt` |
| Single-step / micro ceilings | 500 ms / 200 ms | Enforced by construction: no spec exceeds them |

## 2. Hero moments (rule 55 — at most two)

1. **App launch** — the system icon-anchored window growth
   (rule 57). Marlow contributes nothing layered on top of it;
   the system owns the animation and Marlow must not duplicate
   it. Recorded here as the hero moment, not built as one.
2. **Folder open** — the container transform: preview icons scale
   up from the folder icon, the background expands, items fade in
   with a 45 ms stagger (400 ms drop-in + 350 ms contents).
   No other screen has a hero animation.

## 3. Haptic map (rule 59)

One crisp impulse each, at the event moment, on the same frame
as the visual. Fired from:

| Event | Effect (as implemented) | Call site |
|---|---|---|
| Long-press pickup | `LongPress` (click) — fires AT the long-press moment, drag starts only after it | `HomeGrid.onDragStart` |
| Drag snap (reorder) | `TextHandleMove` (tick) — when neighbours slide aside after the 650 ms hover | `HomeGrid` hover timer |
| Drop | `LongPress` (click) — move, folder create/add, remove/uninstall zone | `HomeGrid.onDragEnd` (all four commit sites) |
| Page snap | `TextHandleMove` (tick) — pager settles on a page (first emission skipped: the initial page is not a snap) | `HomeScreen` `LaunchedEffect(currentPage)` |
| Folder/drawer threshold | `TextHandleMove` (tick) — 800 ms drag hover opens a folder | `HomeGrid` hover timer |
| Toggle flip | `LongPress` on / `TextHandleMove` off | `SettingsControls.SwitchRow` (every settings switch) |
| Successful confirm | `LongPress` (click) — folder rename confirmed (keyboard Done + check button) | `FolderView.confirmRename` |

Never fires: icon tap, page swipe, scrolling, typing in search,
opening an app, errors (visual only). No `Vibrator.vibrate(`
anywhere in the app.

**Vocabulary limit, recorded honestly:** this project builds on
Compose UI 1.7.6 (BOM 2025.01.00), whose `HapticFeedbackType`
exposes only TWO constants — `LongPress` (click class) and
`TextHandleMove` (tick class). The richer vocabulary the research
names (`Confirm`, `SegmentTick`, `ToggleOn`/`ToggleOff`,
`GestureThresholdActivate`) arrives in Compose UI 1.8.0 and does
not exist in this project's classpath (verified against the
cached `ui-android-1.7.6` classes — the first version of this
wave used them and kotlinc-check rejected them). The map above
therefore expresses the click/tick distinction with the two
constants that exist. When the BOM moves to a Compose UI with the
full set, the mapping — and only the mapping — should be widened
to the named constants; the event list does not change. On
Android 16+, any custom haptic would be a PWLE envelope; Marlow
defines no custom haptic, so there is nothing to port.

**Gating:** the whole map sits behind a NEW setting,
`settings.hapticsEnabled`, OFF by default (Home settings →
Gestures → "Haptics", searchable in settings search). There was
NO pre-existing haptics setting to keep the map behind — the
brief's "existing haptics setting" did not exist, so the setting
was created with the map, defaulting OFF per the toggle rule, and
is persisted in `LauncherStore` (`haptics_enabled`). Marlow never
overrides the system haptics setting and never forces haptics on.
The flip that turns haptics off still gives its feedback, since
the old instance is still enabled at that moment.

## 4. Predictive back (rule 58)

- Manifest: `android:enableOnBackInvokedCallback="true"` added
  explicitly on `<application>` (targetSdk 35 already defaults it
  on; the attribute states it so support does not depend on
  remembering the default). The Manifest guard does not check this
  attribute — it still passes on the items it does check.
- **Drawer:** `PredictiveBackHandler` in `HomeScreen` (replaces
  the drawer's plain `BackHandler` — one callback for the drawer).
  The drawer scales toward 90% as the swipe progresses
  (`Animatable`, `snapTo` on progress); completing closes the
  drawer and clears the query; cancelling springs back to 100%
  with `Motion.predictiveBackCancelSpring()` and no state change.
- **Folder:** `PredictiveBackHandler` in `FolderDialog` (replaces
  its `BackHandler`; the redundant HomeScreen-level
  `BackHandler(enabled = openFolder != null)` was removed — one
  callback per overlay). Same 90% preview; completing runs the
  200 ms exit animation, then dismisses; cancelling springs back.
- **Sheets (ModalBottomSheet — At a Glance settings, headphone
  music, notification panel, vault, wallpaper):** NO Marlow
  preview layered on. Verified in the M3 1.4.0-alpha06 classes in
  the local cache: `ModalBottomSheetDialogLayout$Api34Impl` has
  its own back callback (`onBackStarted` / `onBackProgressed` /
  `onBackCancelled`) — the sheet owns its predictive-back preview
  on API 34+. Layering a second preview on top would fight it
  (rule 57).
- **Other overlays** (search, vault, notifications, wallpaper,
  widget picker, long-press sheets, overview, settings
  sub-screens): keep their existing `BackHandler`s — they commit
  on back with no preview. A preview for each is future work, not
  faked here.
- **Home root:** no handler — back belongs to the system (unchanged).
- **Below API 33:** `PredictiveBackHandler` degrades to a plain
  back handler (the gesture commits immediately, no progress
  events) — graceful, no special-casing in Marlow code.
- No screen uses `KeyEvent.KEYCODE_BACK` or `onBackPressed`
  (grep-verified).

## 5. Reduced motion (rule 60)

Central, in `Motion.kt`: every spec function takes
`reducedMotion`, read from the platform by
`rememberReducedMotion()` → `isSystemReducedMotion()` (animator
duration scale or transition animation scale at 0). There is no
launcher-level toggle — the system setting is the source of
truth, as before.

When reduced motion is on:
- Transform motion collapses to instant (`snap()`): drawer
  enter/exit, folder drop-in/exit, drag lift, page moves (via
  `scrollToPage`), widget resize snap.
- Fades collapse to a near-instant 100 ms crossfade (opacity-only
  changes are kept, per rule 60): drawer/search crossfades,
  folder contents.
- Wallpaper parallax is off entirely (the `LaunchedEffect` in
  `HomeScreen` returns early).
- **Haptics are kept** — reduced motion collapses motion, not
  feedback.

Wired in this wave: drawer, folder, home pager, drag lift,
widget resize snap, drawer search, wallpaper parallax.

**Specified in `Motion.kt` but NOT wired (recorded, not hidden):**
- Empty-page snap-off 400 ms / fade 150 ms (`emptyPageSnapOffSpec`
  / `emptyPageFadeSpec`): page removal is a pager `pageCount`
  change and the pager exposes no removal-animation hook, so the
  page vanishes with the pager's own settle.
- Begin-and-end 500 ms (`beginEndSpec`), small-utility 300/250/200
  ms (`utilitySpec` family), fade-through (`fadeThroughSpec`,
  e.g. for At a Glance content swaps), and the fade spec
  (`fadeSpec`): no existing transition uses them yet. They are the
  vocabulary the next wave wires (settings forward/back shared
  axis, At a Glance fade-through) — no transition was invented to
  consume them.
- Settings forward/back x-axis (rule 54): the settings screen
  swaps content without a transition today; a shared-axis wrap was
  out of this wave's scope (home/drawer/folder/sheet only).

## 6. Files changed

- NEW `app/src/main/java/app/launcher/ui/Motion.kt`
- NEW `app/src/main/java/app/launcher/ui/Haptics.kt`
- `app/src/main/java/app/launcher/model/LauncherModels.kt` —
  `hapticsEnabled: Boolean = false`
- `app/src/main/java/app/launcher/data/LauncherStore.kt` —
  `KEY_HAPTICS_ENABLED` + save/load
- `app/src/main/java/app/launcher/ui/HomeScreen.kt` — haptics
  provider wrapper (`HomeScreen` → `HomeScreenContent`),
  drawer `PredictiveBackHandler` + `AnimatedVisibility` enter/exit,
  folder handler removed (FolderDialog owns it), page-snap haptic,
  `scrollPagerTo` (300 ms / `scrollToPage` when reduced), parallax
  off under reduced motion
- `app/src/main/java/app/launcher/ui/HomeGrid.kt` — drag spring
  via Motion, long-press/drop/snap/threshold haptics, widget
  resize snap (`animateContentSize`, 150 ms)
- `app/src/main/java/app/launcher/ui/FolderView.kt` — Motion
  enter/exit + staggered contents, animated exit before dismiss,
  `PredictiveBackHandler`, rename-confirm haptic
- `app/src/main/java/app/launcher/ui/Drawer.kt` — drawer search
  field 300 ms transition
- `app/src/main/java/app/launcher/ui/SettingsControls.kt` —
  `SwitchRow` fires the toggle-flip haptic
- `app/src/main/java/app/launcher/ui/SettingsScreen.kt` —
  "Haptics" row in Gestures + settings-search index entry
- `app/src/main/AndroidManifest.xml` —
  `android:enableOnBackInvokedCallback="true"`

NOT touched: the wizard, the dock (structure, pagination,
labels, seeds), the dex guard, the Robolectric startup gate, the
Manifest guard's checked items, the theme (Stock default, P22
accents, warm shading, halo placement), icon C v3, version
(0.1.0-dev, code 1).

## 7. Verification

- `tools/kotlinc-check.sh`: PASSED, 48 source files, 0 errors
  (final run after all edits; an intermediate run caught three
  real mistakes of this wave — the Compose 1.7.6 haptic
  vocabulary, the `animateContentSize` import package, and a
  CompositionLocal read outside composable context — all fixed
  and re-verified).
- Greps: no `Vibrator.vibrate(`; no `tween(`/`spring(` outside
  `Motion.kt`; no `KEYCODE_BACK`/`onBackPressed`.
- Manifest XML parses; `enableOnBackInvokedCallback` does not
  touch anything the Manifest guard checks.
- CI is the authority for the Robolectric startup gate, the dex
  guard, and the Manifest guard — none of those files were
  touched, but this wave was NOT run through CI (no push).

## 8. Device-gated (the real gate)

Feel and timing are a Pixel 10a job — nothing here is proven by
feel yet:
- Each haptic event, named click / tick (rule 59 asks for the
  hand review naming each effect; with the two-constant
  vocabulary the names are click = `LongPress`, tick =
  `TextHandleMove`).
- Drawer enter/exit timing and the 90% back preview on a real
  back swipe; folder open stagger; page snap tick; widget resize
  snap feel.
- Predictive-back previews on API 33 vs 34+ (the Api34 sheet
  behaviour is from the library's classes, not observed).
- Reduced-motion behaviour with "Remove animations" on.
- The haptics setting default (OFF) on a fresh install.
