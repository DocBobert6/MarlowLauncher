# Rebuild notes — Settings / Back / Accessibility stream (Phase 3, wave 1)

Branch: `rebuild/settings`. Worktree: `~/workspace/launcher-settings`.
Scope files only: `ui/SettingsScreen.kt`, `ui/SettingsControls.kt`,
`ui/LongPressSheet.kt`, `ui/DebugScreen.kt`, `data/AdaptiveLayout.kt`,
`data/BackupManager.kt` (documentation only), `data/Accessibility.kt`
(TalkBack helpers). Nothing else touched; no push; version untouched
(0.1.0-dev, code 1).

kotlinc-check: **GREEN** — `./tools/kotlinc-check.sh` exit 0,
"kotlinc-check passed: 47 source file(s)", 0 errors.
(CI is still the authority; local Gradle builds are impossible here.)

## What changed, per rule number

### Rule 61 — Settings IA (5–7 sections, ≤15 rows, exact entry name)
- Top level is now exactly the §4.3 map: Wallpaper & style, Home
  screen, App drawer, Dock, Gestures, Appearance, About — plus four
  verb rows at the bottom (Hidden apps vault, Back up home screen,
  Restore backup, Reset & debugging). The ten old areas and the
  ~40-row Home area are gone.
- Every sub-screen is ≤15 rows (counts: Top 11, Home 12, Grid 6,
  Icons 7, At a Glance 7, Widgets & feed 7, Notifications 9, App
  drawer 12, Dock 6, Gestures 8, Appearance 13, About 4 + scope
  line). Sub-screen titles match their entry labels exactly.
- No "Advanced" (no screen has ≥3 rarely-changed items to hide) and
  no vague titles ("General"/"Misc"/"Other settings" — the old
  "Behaviour" section is gone).
- Entry point is exactly "Settings" (long-press menu: "Home settings"
  — that label lives in HomeScreen/Overview, another stream's file).
- Homes found for settings the map does not name: Search settings
  (provider, suggestions, contacts, custom engine) live under App
  drawer; Notification settings (access, badges, previews) live under
  Home screen as a "Notifications" sub-screen; the vault is a bottom
  verb row on the top level; Reset & debugging is a bottom verb row
  (About is status-only, so it could not stay there).
- `SettingsArea` (10 values) is KEPT for source compatibility —
  HomeScreen passes `SettingsArea.HOME` when it opens Settings for
  one job. `initialNavFor` maps the old areas onto the new map
  (SEARCH → App drawer, NOTIFICATIONS → Notifications screen,
  THEMES → Appearance, PRIVACY/BACKUP → top level).

### Rule 62 — Row values + wording
- EVERY row shows its current value as subtext ("5 × 5", "On",
  "System default", "Not set"). Switch values are "On"/"Off";
  sliders show their value as a plain number with no units (the audit
  fix stripped the dp units and the values with them — values are
  restored here without units; blur-glass transparency shows "55%").
- All ~10 parentheticals removed: "Themed icons (single-colour)" →
  "Themed", "Smartsize icons (normalised)" → "Smartsize",
  "One-handed layout (items in bottom third)" → "One-handed layout",
  "Hide status bar (ultra-immersive)" → "Hide status bar",
  "AMOLED black (pure-black dark background)" → "AMOLED black",
  "Calendar events (system calendar)" → "Calendar events",
  "Auto-hiding media player (only while playing)" →
  "Auto-hiding media player", "Discover feed (swipe right…)" →
  "Discover", "Widget page (swipe left…)" → "Widget page",
  "Widget overlap (items may overlap…)" → "Widget overlap".
  The Google-feed fragility note ("unofficial, may break — RSS-only
  when unavailable") is dropped from the row; the fragility is
  already recorded in feature-decisions.md (F6) and the feed page
  carries its own unavailable notice.
- No row repeats its section title's words (applied literally): the
  Dock screen's rows are "Visible", "Height", "Background", "Shadow",
  "Page indicators", "Search pill" — not "Dock height"; the Grid
  screen's rows are "Size", "Margin", "Spacing"; the Icons screen's
  are "Shape", "Size", "App labels", "Themed", "Smartsize",
  "Live clock", "Pack". Search-index labels keep the full names
  ("Grid size", "Dock height") so search still finds them.
- No generic-verb labels, no negative labels, no first-person labels,
  no user-visible plan/feature numbers anywhere in my files
  (including `debugToggleStates` — the "(#NN)" suffixes are gone from
  the Debug screen).
- Buttons are Verb + Object: "Back up home screen", "Restore backup",
  "Reset layout to defaults", "Clear all launcher data",
  "Add custom search engine", "Remove engine", "Save name"
  (rename dialog), "Change the default home app".
- The "Wallpaper…" row is gone; the top-level "Wallpaper & style"
  row (value "System") opens the SYSTEM flow
  (`Intent.ACTION_SET_WALLPAPER`), falling back to Marlow's own
  wallpaper sheet when the system flow is absent.
- Section headers are LARGER than row labels (rule 9/15 sweep):
  headers `titleMedium` (16sp), row labels `bodyMedium` (14sp), row
  values `bodySmall` (12sp), screen titles `titleLarge` (22sp).

### Rule 63 — Setting / status / action rows
- Three visibly different rows in SettingsControls.kt: settings
  (`SwitchRow`/`SliderRow`/`NavigationRow`, control or chevron),
  `StatusRow` (no switch, no chevron, no ripple, not tappable),
  `ActionRow` (one-off Verb + Object, no on/off state).
- Status lives at the bottom of a screen or on About only: About
  holds "Home app", "Version", "Package" (all status rows) plus the
  backup restore-scope text at the bottom. The backup result message
  is a status line at the bottom of the top level.
- All mid-list status/explanation paragraphs are out of the toggle
  lists: the weather-location status is a status line at the bottom
  of the "Weather city" sub-screen; the infinite-scroll explanation
  is now a disabled-with-reason state (see below); the
  notification-access status row is the "Access" row at the top of
  the Notifications screen; the contacts grant row is a value-carrying
  navigation row ("Off — contacts are skipped in search"); the
  Privacy/Backup intro paragraphs are deleted (the vault PIN-hash
  detail stays in code comments; the backup scope lives in
  BackupManager's docs + the About status line).
- Dependents sit directly below their parent, disabled WITH a reason
  rather than disappearing: At a Glance children ("Turn on At a
  Glance to change this"), Weather city ("Turn on Weather to change
  this"), Now Brief hour/minute ("Turn on the morning summary to
  change this"), Desktop widget page ("Turn on the widget page to
  change this"), Blur-glass transparency ("Choose Blur glass to
  change this"), Dock children ("Turn on the dock to change this"),
  Gesture actions ("Turn on Gestures to change this"), badge
  style/position/size/corner/dynamic ("Turn on notification access…"
  or "Turn on Notification badges…"), Infinite scroll ("Turn off the
  feed page and widget page to use this" — it is ineffective while
  either is on, so it is disabled rather than silently doing nothing).
- Destructive actions are separated, error-coloured, and confirmed:
  Debug screen's two actions are now error-coloured verb rows with
  their own confirmation dialogs (they were filled primary Buttons).

### Rule 64 — Controls
- Every in-list `FilterChip` group is gone. Single-choice settings
  use radio on a sub-screen (`RadioChoiceRow` + `ChoiceSettingsContent`;
  25 choice keys). `ChipRow` is KEPT in SettingsControls.kt ONLY
  because the HELD FirstRunWizard.kt still uses it (3 call sites) —
  delete it with the wizard; settings no longer calls it.
- Switch colours are the M3 tokens, passed explicitly:
  selected track `primary`, unselected track
  `surface-container-highest` + `outline`. Slider: active
  track/thumb `primary`, inactive track `surface-container-highest`,
  value shown. (These are also the M3 defaults; passing them makes
  the conformance checkable.)
- 48dp targets everywhere: every row has `heightIn(min = 48dp)`
  (slider rows included — they had no guarantee before); long-press
  sheet rows and picker rows likewise.
- Toggles apply immediately, no Save button. Grid size has no preview
  in Marlow, so the documented grid-size-with-preview Apply exception
  does not apply — grid size applies on tap like every other choice.
- No device-level preference is duplicated (nothing added; the theme
  mode Auto/Light/Dark is Marlow's own theme, not system scheduling).

### Rule 65 — Settings search deep-link
- Search stays live over a rebuilt index (~90 entries), matching
  title + synonyms + effect words ("dark" → Theme mode, Theme,
  AMOLED black; "dots" → Page indicators, Notification badges, Dock
  page indicators; "grid" → Grid size/margin/spacing).
- Each result shows its breadcrumb ("Home settings › App drawer",
  "Home settings › Home screen › Grid").
- Tapping a result lands DIRECTLY on that setting's sub-screen
  (choice settings land on their radio sub-screen) with the row
  scrolled into view (`LazyListState.scrollToItem`) AND highlighted
  (a `surface-container-high` background on the row until the user
  navigates or taps). The old behaviour (showing only the area) is
  gone.
- No-match shows the honest empty state "No settings match '…'".
- Reachable in ≤2 taps from home (long-press empty space →
  "Home settings"; search is at the top of that screen).

### Rule 51 — Long-press sheet (decision F25, parent rulings 5/6)
- Fixed order by default: active notifications FIRST, then app
  shortcuts, then App info and a Widgets entry, then the rest in the
  stored order. Notifications show title/sender/message with no
  action buttons: the notification's title IS the sender in messaging
  notifications, the text is the message, and the app itself is the
  sheet title. `NotificationPreview` carries no separate sender
  field — see the patch-request note below if the home stream wants
  one from `EXTRA_TITLE` (it would duplicate the title).
- The numeric notification count appears HERE: the notifications
  header reads "3 notifications" (at most 3 shown; the header carries
  the full count). Counts never appear on the icon (rule 49).
- Shortcuts: at most 4 per app (`take(4)`), rendered as CIRCULAR
  48dp icons (48dp circle, 24dp centred icon, no shadows) with the
  label below. The app-published icon is loaded in the sheet via
  `LauncherApps.getShortcutIconDrawable` (AppShortcut itself carries
  no drawable — see the LauncherModels note below); fallback is the
  shortcut's initial letter. Tap launches; long-press opens a small
  menu (Launch / Add to Home screen) — more than remove-only.
- Widgets entry added ("Widgets", fixed directly after App info; it
  is not part of the reorderable list). It calls the new
  `onOpenWidgets: () -> Unit = {}` callback — HomeScreen must pass it
  (patch request below); the default no-op keeps the current call
  site compiling.
- User-reordering is KEPT (#173 still exists): the stored order is
  honoured for the menu items, and the reorder sub-screen remains.
  The stored DEFAULT order already is the fixed order, so the default
  render matches rule 51 exactly.
- Drag-from-sheet: NOT built, and the capability is NOT silently
  dropped — the "Add shortcut to Home screen…" picker is kept.
  Why drag is not possible from this stream: the sheet is a
  `ModalBottomSheet`; the home drag infrastructure (drag controller,
  drop targets, spring-loaded workspace, drop-position math) lives in
  HomeScreen/HomeGrid and exposes no begin-drag entry point, and
  Compose has no drag-out-of-a-sheet gesture. A drag needs the home
  stream to expose one (e.g. a `beginShortcutDrag(shortcut)` the sheet
  can call from a drag handle). Recorded here, not silently dropped.
- Long-pressing a shortcut: Launch + Add to Home screen (see above).
- Sheet title is now `titleLarge`; notification title/body use
  `bodyMedium`/`bodySmall`; all `.sp` literals removed from the file.

### Rule 58 — Back handling
- Settings: ONE `BackHandler` in SettingsScreen.kt for settings
  navigation — it pops one level while a sub-screen is open and
  dismisses settings at the top level. Its behaviour follows the
  observable nav state. The header back button uses the same path.
  Dialogs (priming, rename, pickers) consume Back themselves via
  `onDismissRequest`. No predictive-back preview animation (a later
  motion pass refines it) — handlers only, with `enabled` correct.
- DebugScreen: `BackHandler(enabled = true, onBack = onDismiss)` —
  enabled follows the open state (it is a full screen).
- No `KeyEvent.KEYCODE_BACK` or `onBackPressed` anywhere in my files
  (verified by grep).
- Overlays whose state lives in HomeScreen.kt (drawer, overview,
  universal search, vault sheet, notification panel, and the rest)
  are PATCH REQUESTS below — HomeScreen.kt was not touched.

### Rule 67 — Permissions pattern (decision F22)
- Reusable `PermissionPrimingDialog` in SettingsControls.kt: one
  sentence stating what the user gets; "Not now" cancels WITHOUT the
  system dialog ever firing; the system dialog fires only after
  opt-in on Allow.
- Wired for: notifications (Now Brief — priming before the
  POST_NOTIFICATIONS launcher), Bluetooth (headphone music — priming
  before BLUETOOTH_CONNECT), contacts (search — priming before the
  contacts request), notification-listener (priming before opening
  system settings, which Android requires for the listener grant).
- Deny → only the dependent feature degrades, no blocking error, no
  repeated re-prompt, one-line explanation on the dependent row or as
  a bottom status line ("Notifications are off, so the morning
  summary will not arrive." / "Notification access is off — badges
  and previews are hidden." / "Bluetooth permission is off, so music
  apps will not appear on headphone connect." / contacts row value
  "Off — contacts are skipped in search").
- Storage/photos: no priming needed — Wallpaper & style opens the
  system flow, which handles its own access.
- The calendar auto-fire at launch and contacts auto-ask on search
  open were NOT touched (home stream's job, per the brief).

### Rules 68–70, F24-part — Accessibility
- `AdaptiveLayout.isWideDevice`: threshold moved 600dp → **840dp**
  (expanded breakpoint). This also moves `twoPaneEnabled` and
  `variantFor`'s wide branch to 840dp, and the foldable/tablet
  settings rows are now gated on ≥840dp in Appearance.
- Orientation from the occupied area: new
  `orientationForArea(windowWidthDp, windowHeightDp)` (landscape
  when the window is wider than tall) and
  `variantForWindow(smallestScreenWidthDp, windowWidthDp,
  windowHeightDp)`. `variantFor(smallestScreenWidthDp, orientation)`
  is kept for the existing HomeScreen call, with KDoc stating the
  orientation argument MUST be the window orientation, not
  `configuration.orientation` (device orientation). HomeScreen patch
  request below.
- TalkBack helpers added to `Accessibility.kt` (rule 69):
  `Modifier.actionDescription(action)` (contentDescription naming
  the ACTION, e.g. "Settings" not "gear icon"),
  `Modifier.stateText(state)` (`stateDescription`, never colour
  alone), `Modifier.decorative()` (hides decorative elements from
  the accessibility tree), `appIconDescription(name, page, position)`
  → "Chrome, page 1, position 3", `folderDescription(name, count)` →
  "Social, folder, 6 apps".
- App-icon position announcements, folder announcements, and
  `customActions` for drag-only actions live in the home/drawer
  files — PATCH REQUESTS below with exact strings and locations.
- No TalkBack behaviour was invented that cannot be verified: the
  device check (navigate the launcher with TalkBack on) is still
  owed — see "Device-gated".

### Rule 72, F24 — Robustness docs
- `BackupManager` KDoc now documents EXACTLY what a backup restores
  (settings, home items incl. dock, home folders, home pages, drawer
  folders, app pairs, pinned contacts, renames, icon overrides,
  hidden apps, per-app languages, swipe-up actions, widget stacks,
  feed sources, notes/tasks/habits/timers; built-in widgets restore)
  and the gaps: third-party widgets CANNOT be restored on a new
  device (AppWidgetHost ids are per-device — verified in code:
  `ThirdPartyWidgetView` renders "Widget unavailable" when
  `providerInfo` is null for the stored id, so the spot comes back
  unavailable until the user re-adds the widget); Nova import is NOT
  supported (no Nova reader; a Nova file is rejected as not-a-Marlow-
  backup). The same text is in `BackupManager.RESTORE_SCOPE_TEXT`
  and is shown at the bottom of Settings → About.
- KNOWN SILENT GAP documented (not fixed — BackupManager scope was
  documentation-only): four settings fields are NOT carried in the
  backup file and fall back to defaults on restore — blur-glass
  transparency (`surfaceAlpha`), weather city
  (`weatherCityName/Lat/Lon`), morning-summary time
  (`nowBriefHour/Minute`), first-run wizard state
  (`wizardCompleted`). See "LauncherModels / other follow-ups" — the
  fix is four `put(...)` lines in `settingsToMap` + four restores in
  `settingsFromMap` (BackupManager.kt, not LauncherModels.kt).

### Rules 9, 15 — Type + shape sweep in my files
- ZERO hard-coded `.sp` literals at call sites in SettingsScreen,
  SettingsControls, LongPressSheet, DebugScreen (verified:
  `grep -E "[0-9]+\.sp|fontSize"` returns nothing). All text uses
  type-scale roles only.
- Shapes: no custom radii added; dialogs/sheets keep M3 28dp
  defaults; text fields keep the 4dp default; shortcut icons are
  circular; section headers use the default background. The only
  `RoundedCornerShape` use in my files is the 20dp round slider
  thumb (pre-existing, kept) and the circular shortcut icon
  container.

## Decisions with reasoning (not in a rule above)

1. Row labels drop the section word (rule 62, applied literally).
   "Dock height" under "Dock" became "Height". The search index
   keeps the full names so search still finds them.
2. Choice screens apply + return on tap. Tapping a radio option
   applies it immediately and pops back to the parent screen — the
   standard single-choice settings behaviour. Search deep-links land
   ON the choice screen itself (that IS the setting's sub-screen),
   with the current option selected.
3. The Widgets entry is pinned after App info even when the user
   reorders the menu. Rule 51 fixes its position; #173 reordering
   covers the menu items, not the fixed entries (notifications are
   likewise always first).
4. "Google bridge" and "Discover" are the Widget & feed row labels
   ("Google feed bridge"/"Discover feed" repeat the section word
   "feed"). The feed's fragility note left the row (see rule 62).
5. Theme choice options carry supporting text ("Material You —
   Follows your system colours", "Stock — Marlow's own look",
   "Ambient — Accent from your wallpaper") so the §4.3 "Material You
   one tap away, in plain words" requirement is visible where the
   choice is made.
6. The morning-summary switch value shows "On, 07:00" when on —
   the time is part of the value, and the hour/minute sliders below
   show their own values.
7. `ChipRow` survives ONLY for the held wizard (see rule 64).
8. The About screen shows "Package" as a status line (debug-useful,
   honest). There is no licences screen to link to and no storage
   figure available in my files, so neither is claimed on About.

## LauncherModels defaults changes needed (I may not touch the file)

These are requests for the coordinator / model-owning stream, with
file + line (LauncherModels.kt, as read 2026-11-26):

1. `badgeStyle` default NUMBER → DOT (line ~585). Rule 49: stock
   surfaces use the dot (presence) only; the numeric badge is the
   documented non-stock extension (#31/#129 approve it as an
   option, not the default). Also `badgeSizeDp` default 18 → 6
   (M3 small dot; the 18dp default is oversized against the rule-49
   dot and the audit's rule-50 finding).
2. `drawerStyle` default LIST → the Pixel vertical grid. Rule 32:
   the drawer is a vertical scrolling grid, alphabetical by default.
   (The LIST style stays as an option.)
3. `gridSize` options are 4×5 / 5×5 / 5×6 / Fluid (lines 4–8).
   Rule 22: the set is exactly 2×2 / 3×3 / 4×4 / 4×5 / 5×5,
   default 5×5 (4×5 on smaller phones). 5×6 and Fluid must go;
   2×2 / 3×3 / 4×4 must be added. (Changing grid size must also
   re-flow/migrate items — that behaviour is the home stream's, not
   a defaults change.)
4. `longPressMenuOrder` default already equals the fixed rule-51
   order for the menu items (shortcuts, app_info, …) — NO change
   needed. Confirmed, not assumed.
5. Badge position: the default is fine (top-right), but the setting
   must not offer bottom corners as equal choices under rule 50 —
   consider restricting the options to top corners only. (Left as a
   question for the coordinator; I did not remove the options.)
6. No new fields are needed for this stream's work. The shortcut
   icons are loaded in LongPressSheet via LauncherApps (no model
   change). A cleaner future change would be adding an icon field to
   `AppShortcut` (lines 404–409) so `loadShortcuts` keeps the
   drawable instead of the sheet re-querying — optional, not
   required.

## Patch requests for other files

### P1. BackHandler for every HomeScreen overlay (rule 58) — HomeScreen.kt
HomeScreen.kt currently has NO BackHandler (grep: only Settings,
Debug, and the wizard have one). Add the following in the HomeScreen
body, right after the overlay state declarations (after line 171,
the `showNotifications` declaration), in this order — bottom-to-top
priority, so the topmost overlay's handler (composed last) wins when
several are open (BackHandler resolves to the most recently composed
enabled handler). One callback per overlay; `enabled` follows that
overlay's own open state. Each close action mirrors the overlay's
existing `onDismiss`/`onClose` exactly:

```kotlin
BackHandler(enabled = showDrawer) {
    showDrawer = false
    viewModel.setDrawerQuery("") // rule 72: the drawer query must clear when the drawer hides (see P4)
}
BackHandler(enabled = showOverview) { showOverview = false }
BackHandler(enabled = showSearch) {
    showSearch = false
    viewModel.clearSearch()
}
BackHandler(enabled = showVault) {
    showVault = false
    viewModel.lockVault()
    viewModel.clearPendingHide()
}
BackHandler(enabled = showNotifications) { showNotifications = false }
BackHandler(enabled = showWallpaper) { showWallpaper = false }
BackHandler(enabled = showNowBrief) { showNowBrief = false }
BackHandler(enabled = showWidgetPicker) { showWidgetPicker = false }
BackHandler(enabled = longPressWidget != null) { longPressWidget = null }
BackHandler(enabled = showDesktopWidgets) { showDesktopWidgets = false }
BackHandler(enabled = addingSticker) { addingSticker = false }
BackHandler(enabled = editingSticker != null) { editingSticker = null }
BackHandler(enabled = longPressApp != null) { longPressApp = null }
BackHandler(enabled = longPressNonApp != null) { longPressNonApp = null }
BackHandler(enabled = openFolder != null) { openFolder = null }
BackHandler(enabled = pairFirst != null) { pairFirst = null }
BackHandler(enabled = folderPickApp != null) { folderPickApp = null }
BackHandler(enabled = folderPickContact != null) { folderPickContact = null }
BackHandler(enabled = showContactDockPicker) { showContactDockPicker = false }
```
Verified close actions (mirrored from the existing calls):
`DrawerScreen onClose = { showDrawer = false }` (line ~774);
`OverviewScreen onClose = { showOverview = false }`;
`UniversalSearchScreen onClose = { showSearch = false;
viewModel.clearSearch() }` (line ~943);
`VaultSheet onDismiss = { showVault = false; viewModel.lockVault();
viewModel.clearPendingHide() }` (line ~887);
`NotificationPanelSheet onDismiss = { showNotifications = false }`;
`WallpaperSheet onDismiss = { showWallpaper = false }`;
`NowBriefSheet onDismiss = { showNowBrief = false }`;
`WidgetPickerDialog onDismiss = { showWidgetPicker = false }`;
`WidgetItemSheet onDismiss = { longPressWidget = null }`;
`FolderDialog onDismiss = { openFolder = null }`;
`LongPressSheet onDismiss = { longPressApp = null }`;
`NonAppLongPressSheet onDismiss = { longPressNonApp = null }`.
The Settings screen's own BackHandler (in SettingsScreen.kt) is
composed after these (settings block ~line 838), so it wins while
settings is open. No predictive-back preview animation in this
pass — handlers only.

### P2. Long-press sheet Widgets entry wiring — HomeScreen.kt (~line 1014)
Pass the new `onOpenWidgets` callback into `LongPressSheet` (the
parameter exists with a no-op default; without this the Widgets
entry does nothing):

```kotlin
LongPressSheet(
    // ... existing arguments ...
    onOpenWidgets = {
        longPressApp = null
        showWidgetPicker = true
        widgetPickerPage = PAGE_WIDGET_PAGE
    },
)
```

### P3. TalkBack: app-icon position + folder announcements + customActions (rule 69)
Helpers are in `data/Accessibility.kt` (this stream):
`appIconDescription(name, page, position)` → "Chrome, page 1,
position 3"; `folderDescription(name, count)` → "Social, folder,
6 apps".

(a) Home grid app cells — HomeGrid.kt, the APP branch (~line 222,
`Column(modifier = swipeModifier, ...)` inside the outer `Box` whose
modifier ends `.clickable { onTapItem(item) }`). The outer Box is
the clickable node; add the semantics there. `pageIndex` is in scope
(line 73 parameter); position is the 1-based index of the app among
the page's APP-kind items. Exact change:

```kotlin
// Before:
Box(
    modifier = Modifier
        .offset { ... }
        .size(...)
        .pointerInput(...) { ... }
        .clickable { onTapItem(item) },
    contentAlignment = Alignment.Center,
)
// After (add the semantics; keep the rest identical):
Box(
    modifier = Modifier
        .offset { ... }
        .size(...)
        .semantics {
            contentDescription = appIconDescription(
                app.displayLabel,
                pageIndex + 1,
                items.filter { it.kind == HomeItemKind.APP }.indexOf(item) + 1,
            )
        }
        .pointerInput(...) { ... }
        .clickable { onTapItem(item) },
    contentAlignment = Alignment.Center,
)
```
(Import `app.launcher.data.appIconDescription` and
`androidx.compose.ui.semantics.semantics` in HomeGrid.kt.
`app` is non-null in the APP branch — the `if (app != null)` guard
already wraps it; place the semantics where `app` is known non-null
or use `item.label ?: app?.displayLabel ?: "App"`.)

(b) Folder tiles — HomeGrid.kt, `FolderTile` (~line 346). The
`Column(modifier = modifier, ...)` needs:

```kotlin
Column(
    modifier = modifier.semantics {
        contentDescription = folderDescription(
            folder.name,
            folder.apps.size,
        )
    },
    horizontalAlignment = Alignment.CenterHorizontally,
)
```
(Import `app.launcher.data.folderDescription`. The 2×2 preview
minis inside are decorative — hide them with
`Modifier.decorative()` from Accessibility.kt if their TalkBack
nodes leak through; the folder's own description is the announcement.)

(c) `customActions` for drag-only actions — AppIconTile
(AppIcon.kt:165-183) and the home-grid APP cell above. TalkBack
cannot swipe, so move / remove / rename / uninstall need
`customActions` equivalents (rule 69). Exact addition on the
tile's semantics node (AppIconTile):

```kotlin
.semantics {
    contentDescription = app.displayLabel // + position once page/position params exist (see below)
    customActions = listOf(
        CustomAccessibilityAction("Remove") {
            onRemove()
            true
        },
        CustomAccessibilityAction("Rename") {
            onRename()
            true
        },
        CustomAccessibilityAction("Uninstall") {
            onUninstall()
            true
        },
    )
}
```
with new optional parameters on `AppIconTile`
(`onRemove: () -> Unit = {}`, `onRename: () -> Unit = {}`,
`onUninstall: () -> Unit = {}`), wired at the Dock.kt:100 and
FolderView.kt:73 call sites to the existing ViewModel actions
(`removeHomeItem`, rename dialog, `uninstallIntent`).
"Move" is deliberately NOT in this list: there is no programmatic
move/drag entry point in the home stream today — starting a drag
from a TalkBack action needs the same begin-drag entry point as
drag-from-sheet (see the rule-51 note). Do not fake it with a
no-op action; add it when the drag entry point exists.
Position announcements in AppIconTile need `page`/`position`
parameters added to it (it currently receives neither); the home
stream owns that threading (Dock pages: page = dockPage + 1;
FolderView: position within the folder).

(d) Drawer search field hint — Drawer.kt (~line 156-178,
`searchField`). The field already has the right hint; it is
defeated by the semantics override. Exact change — delete one
line:

```kotlin
// Before:
modifier = Modifier
    .fillMaxWidth()
    .padding(horizontal = 16.dp, vertical = 8.dp)
    .focusRequester(focusRequester)
    .semantics { contentDescription = "Drawer search" },
// After:
modifier = Modifier
    .fillMaxWidth()
    .padding(horizontal = 16.dp, vertical = 8.dp)
    .focusRequester(focusRequester),
```
The `placeholder = { Text("Search apps") }` already on the field IS
the hint (rule 69: the drawer search field has a hint, not a
contentDescription). Remove the now-unused semantics import if it
becomes unused.

### P4. Drawer query must clear when the drawer hides (rule 72)
HomeScreen.kt, the `DrawerScreen` call (~line 766): the current
`onClose = { showDrawer = false }` does not clear the query, so a
reopened drawer still shows the old filter (universal search already
clears on close; the drawer does not — audit finding). Exact change:

```kotlin
onClose = { showDrawer = false; viewModel.setDrawerQuery("") },
```
(`setDrawerQuery` exists on HomeViewModel, line 1869.) The
BackHandler in P1 does the same on Back.

### P5. HomeScreen: use the area-based layout variant (rule 70)
HomeScreen.kt line ~127:

```kotlin
// Before:
val layoutVariant = AdaptiveLayout.variantFor(
    smallestScreenWidthDp,
    configuration.orientation,
)
// After:
val layoutVariant = AdaptiveLayout.variantForWindow(
    smallestScreenWidthDp,
    configuration.screenWidthDp,
    configuration.screenHeightDp,
)
```
(`configuration.orientation` is device orientation; the variant must
come from the occupied area. `variantForWindow` is in
AdaptiveLayout.kt, this stream. `variantFor` is kept for source
compatibility but should no longer be called.)

### P6 (note, not a patch). NotificationPreview sender field
If the home stream wants a dedicated sender line distinct from the
title, `NotificationPreview` (NotificationMonitor.kt:24) needs a
`sender` field populated from the notification extras. The sheet
already renders title (= sender in messaging notifications) +
message, so this is optional polish, not a gap.

## What remains device-gated and why

- TalkBack end-to-end: the helpers and strings are in place, but no
  TalkBack navigation of the launcher has been done in this sandbox
  (no device, no emulator with TalkBack). Still owed on the Pixel
  10a: navigate home/drawer/settings with TalkBack on; confirm app
  positions, folder counts, state descriptions, and that the
  customActions appear. (The patch requests in P3 must land first —
  the announcements live in the home stream's files.)
- Long-press sheet visuals: the 48dp circular shortcut icons,
  notification ordering, and the Widgets entry render from code
  only; the look on a real device (icon legibility at 24dp, sheet
  height with 4 shortcuts) is Robert's check, not this change's.
- Settings search deep-link highlight + scroll: code-verified only
  (`scrollToItem` + highlight background). Needs a device pass:
  search "dark" → tap "Theme" → lands on the Theme choice screen;
  search "dots" → tap "Notification badges" → lands on the
  Notifications screen with the Badges row visible and highlighted.
- Priming dialogs: the flow is code-verified (dialog → Allow fires
  the system dialog; Not now fires nothing). A device pass must
  confirm no system dialog appears before Allow, and that a denial
  leaves only the dependent feature degraded.
- System wallpaper flow (`ACTION_SET_WALLPAPER`): the intent fires
  from code; whether the Pixel 10a's system picker opens (vs.
  ActivityNotFoundException → Marlow sheet fallback) is a device
  check.
- Two-pane at ≥840dp + area-based orientation: code-verified only;
  needs a foldable/tablet (or split-screen at three widths) to
  confirm the threshold and the unfold reflow.
- CI is the authority for the build (kotlinc 2.2.20 here vs Kotlin
  2.4.20 on CI). Nothing here was verified by a Gradle build.

## Files changed (this branch only)

- `app/src/main/java/app/launcher/ui/SettingsScreen.kt` — full
  rebuild: 7-section IA, 16 destinations, radio choice screens,
  deep-linking search, priming wiring, BackHandler.
- `app/src/main/java/app/launcher/ui/SettingsControls.kt` —
  SwitchRow/SliderRow/NavigationRow/StatusRow/ActionRow/
  RadioChoiceRow/ReorderRow/PermissionPrimingDialog; ChipRow kept
  ONLY for the held wizard.
- `app/src/main/java/app/launcher/ui/LongPressSheet.kt` — fixed
  order, notifications with count, circular shortcut icons (≤4),
  Widgets entry, shortcut long-press menu, picker kept as the
  drag fallback.
- `app/src/main/java/app/launcher/ui/DebugScreen.kt` — type roles,
  no feature numbers, destructive actions as confirmed error rows.
- `app/src/main/java/app/launcher/data/AdaptiveLayout.kt` — 840dp
  threshold, area-based orientation.
- `app/src/main/java/app/launcher/data/Accessibility.kt` — TalkBack
  helpers.
- `app/src/main/java/app/launcher/data/BackupManager.kt` — restore-
  scope documentation only (+ `RESTORE_SCOPE_TEXT`).
- `rebuild-notes-settings.md` — this file.

Not touched, per the brief: `LauncherModels.kt` (defaults changes
requested above instead), `FirstRunWizard.kt` (HELD), `.github/`,
the manifest, `HomeScreen.kt`, `Drawer.kt`, `AppIcon.kt`,
`HomeGrid.kt`, `NotificationMonitor.kt`, `BUILD_LOG.md`.

## Ruling update 2026-11-26 17:06 EST — wizard deleted, dock fixed (NOT built here)

Robert's ruling: follow the audit's recommendations on both held
items. (1) Wizard: home-first — the five-step wizard is deleted;
home first, single system default-home prompt. (2) Dock pages:
follow the research — exactly five icons, labels off, one page, by
kind Phone / Messages / browser / mail / Camera.

Per the ruling instruction neither is built in this stream; a
separate builder does them on the merged tree. Everything below is
a FLAG for the merge / that builder. This stream's code is
unchanged and still compiles against the pre-ruling tree
(kotlinc-check green on 530a100). Several spots below will NOT
compile, or will be dead/wrong, once the wizard/dock builder lands
its LauncherModels changes — the merge must land those together.

### A. Wizard deletion — impacts in my files

1. `SettingsControls.kt:353-361` `ChipRow` — kept ONLY for
   `FirstRunWizard.kt` (3 call sites). Wizard deleted means zero
   call sites: delete `ChipRow` AND its now-unused imports
   (lines 8-9 `ExperimentalLayoutApi`/`FlowRow`, line 17
   `FilterChip` — grep confirms only `ChipRow` uses them), KDoc
   included.
2. `DebugScreen.kt` — four wizard references to retarget:
   - KDoc lines 41-46: "The wizard stays completed" / "the
     wizard shows" — rewrite: clear-all means the next launch is
     home-first with the single default-home prompt (if Marlow
     isn't default).
   - Line 135: "The next launch shows the first-run wizard
     again" — same retarget.
   - Line 192 action message: "The first-run wizard will show
     next launch" — same retarget.
   - Line 262: debug toggle "First-run wizard completed" →
     `s.wizardCompleted` — if the wizard builder deletes
     `wizardCompleted` (`LauncherModels.kt:615`, main repo), this
     line fails compile. Either delete the row or the field stays
     as dead state; merge to decide with the wizard builder.
3. `SettingsScreen.kt:1274` comment "(Also reachable from the
   notification panel and the first-run wizard.)" — stale comment
   only; the Access row itself is unaffected (the
   notification-listener grant path is unchanged).
4. `BackupManager.kt:79-80` KDoc silent-gap list includes
   `wizardCompleted` — moot once the field is gone; the other
   three gaps (`surfaceAlpha`, weather city,
   `nowBriefHour/Minute`) stand unchanged.
5. Notes P1 (above): the parenthetical "only Settings, Debug,
   and the wizard have one [BackHandler]" goes stale once
   `FirstRunWizard.kt` is deleted; the P1 patch content itself is
   unaffected (`FirstRunWizard.kt:88` BackHandler disappears with
   the file — nothing to migrate; the home-first prompt is the
   wizard builder's, not a BackHandler concern).

### B. Fixed five-icon dock — impacts in my files

1. `SettingsScreen.kt` Dock screen — the "Page indicators" row
   must go: the row (lines 1554-1558), its `rowKeys` entry
   (line 1503), `ChoiceKey.DOCK_INDICATORS` (line 205), its
   search-index entry (line 330), the choice-options branch
   (line 1906), the selected-id branch (line 1942), and the apply
   branch (line 1973). One page has no indicators. KDoc line 1498
   ("6 rows") becomes 5 rows. NOTE: `ChoiceKey.INDICATOR_STYLE`
   (lines 189, 972, 1898, 1935, 1966) is the HOME-page indicator
   style and STAYS — home pages are still multiple; only the DOCK
   variant goes. The `PageIndicatorStyle` enum itself stays, for
   home.
2. `LongPressSheet.kt` — the dock-page picker is dead:
   `dockCounts` (lines 161-164, `0 until 5`), the "Add to dock…"
   row (lines 316-318), `DockPagePickerDialog` (lines 526-560)
   and its call site (lines 397-408). Deeper: with the dock fixed
   to five icons by kind, "Add to dock" itself needs the dock
   builder's call — an arbitrary app cannot join a five-fixed-kind
   dock. Options for that builder: remove the row, or retarget it
   (e.g. replace / choose a kind slot). Its KDoc says the dialog
   is also shared with the overview (pin a contact there) — that
   caller lives in a home-stream file, so it must be found and
   retargeted too.
3. `LauncherModels` fields the dock builder will touch
   (read-only check of the main repo — NOT changed here):
   `HomeItem.dockPage` (line 325, "dock page (#68, up to 5)") and
   `LauncherSettings.dockIndicatorStyle` (line 434). If either
   field is deleted: `BackupManager.kt` lines 201/494 (dockPage
   serialisation) and 291/390 (`dock_indicator_style`
   put/restore) fail compile, and `LauncherStore.kt` lines
   228/350 (dockIndicatorStyle persist/restore) fail compile —
   the dock builder must remove those together with the field,
   or the coordinator must. If `dockPage` is kept as always-0,
   the BackupManager restore (line 494) should force 0 for old
   backups. `RESTORE_SCOPE_TEXT` wording ("home incl. dock")
   stays true either way.
4. `SettingsScreen.kt` Dock screen — "Search pill" row
   (`searchPillVisible`; `LauncherModels` #112, ON by default):
   OPEN QUESTION for the dock builder, not a decided conflict —
   a pill would be a sixth dock element beside exactly five
   icons. Either the pill goes, or it lives elsewhere. Flagged,
   not resolved.
5. Unaffected dock rows: "Visible", "Height" (56-96dp slider),
   "Background", "Shadow" — a one-page five-icon dock still has
   all four; no change needed. No labels row exists in my Dock
   screen (labels were never a setting in my UI), so "labels off"
   needs no removal here — it is enforced in `Dock.kt`
   rendering by the dock builder.
6. Notes P3 (above): the "Dock pages: page = dockPage + 1"
   formula for threading page/position into `AppIconTile` is
   dead. Under a one-page dock: page is always 1, position 1-5.
   The P3 request itself (customActions + position
   announcements) still stands, with that formula replaced.

### C. What my stream does NOT assume (checked, no action)

- No settings row, search entry, or patch request assumes the
  five-step wizard flow (no wizard step is a settings
  destination; the wizard's Theme/Grid/Search steps were
  chip-based, which is why `ChipRow` existed — item A1 covers
  it).
- The About "Change the default home app" row
  (`SettingsScreen.kt:1826`) and rule-66 default-home handling
  already match the home-first ruling: a single system
  default-home prompt, no wizard step in between.
- The LauncherModels defaults requests in my earlier report
  (badgeStyle, drawerStyle, grid options, longPressMenuOrder,
  badge-position) are unaffected by this ruling. One possible
  addition for the coordinator: no dock-labels field was found in
  `LauncherModels` (grep: none), so there is no labels default
  to change — the dock builder owns "labels off" in rendering.

