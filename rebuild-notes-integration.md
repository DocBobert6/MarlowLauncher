# Marlow Launcher — INTEGRATION notes (Phase 3, wave 1.5)

- Branch: `rebuild/merge` (worktree `~/workspace/launcher`)
- Date: 2026-11-26
- Scope: apply every cross-stream patch request and LauncherModels
  change the four rebuild streams filed, so the merged tree works as
  one app. No push made. Version untouched (0.1.0-dev, code 1).
- Verification: `tools/kotlinc-check.sh` **green, 47 source files,
  0 errors, 0 warnings** (kotlinc 2.2.20; CI with Kotlin 2.4.20 is
  the authority, as usual). The Drawer `else`-redundant warning at
  `Drawer.kt:391` is GONE — the GRID branch now exists.
- No credentials, employment history, or education claims anywhere
  here or in the code. Kept intact: name Marlow, Stock theme as
  fallback, P22 seed, warm shading, halo only on page dot + resize
  handle, icon C v3. Dex guard, startup gate, Manifest guard
  untouched. Held items untouched: `FirstRunWizard.kt`, dock
  pagination / labels / seed order / `dockPage` /
  `dockIndicatorStyle` / `wizardCompleted`, the Settings Dock screen
  "Page indicators" row, `.github/`, the manifest, the version.

## A. LauncherModels changes — applied / already done

1. **Applied — `launcherTheme` default STOCK → MATERIAL_YOU**
   (`LauncherModels.kt`). The stale "which should be the default is
   still open for Robert" comment is replaced with the decision:
   rule 3, dynamic colour on by default on Android 12+; Stock is
   the fallback below API 31 and when the user picks Stock. The
   theme row in Settings now reads Material You by default, and the
   `LauncherTheme` composable default already agreed.
2. **Applied — `AccentColour` comment only.** The light/dark ARGB
   pairs are now documented as SEEDS (each regenerates the whole
   scheme through the HCT pipeline, rule 4/5). No value change.
3. **Applied — `DrawerStyle.GRID("Grid")` added; default
   `drawerStyle` LIST → GRID.** This activates the rebuilt drawer.
   `Drawer.kt`'s `when` now has an explicit `DrawerStyle.GRID`
   branch (the `else` is gone, and with it the kotlinc warning).
   The choice screen picks GRID up automatically via
   `DrawerStyle.values()`.
4. **Applied — new `swipeUpToSearch: Boolean = false`.**
   Persisted in LauncherStore (`swipe_up_to_search`) and in
   backups. The gesture side is wired too: swipe-up on the home
   sets `drawerFocusSearch = settings.swipeUpToSearch` (the drawer
   note left the gesture side to the home stream; it is done here).
5. **Applied — new `drawerShowCategories: Boolean = false`,
   `drawerShowDrawerFolders: Boolean = false`.** Persisted in
   LauncherStore and in backups. Both have SwitchRows in the drawer
   settings section (see B5 below) and are wired in `Drawer.kt`:
   category tabs filter the list; drawer-folder chips + a
   "+ Folder" create dialog appear above the list. Both are
   labelled as Marlow extensions and are off by default.
6. **Already done by the home stream — VERIFIED, no change.**
   `badgeStyle` is DOT, `badgePosition` is TOP_END, `badgeSizeDp`
   is 6 (`LauncherModels.kt:632-634`). The numeric count lives only
   in the long-press menu; the dot is the only on-icon badge.
7. **Applied — badge position options restricted to top corners
   only.** `BadgePosition.BOTTOM_END` / `BOTTOM_START` removed from
   the enum. The dot is pinned top-trailing per rule 50, so bottom
   corners were never reachable on screen. Old stored bottom values
   fall back safely to the default (TOP_END) through the
   `enumValue`/`enum` helpers in LauncherStore/BackupManager — no
   migration needed, no crash on old data. The choice screen picks
   the restriction up automatically via `BadgePosition.values()`.
8. **Already done by the home stream — VERIFIED, no change.** Grid
   options are the Pixel set 2×2 / 3×3 / 4×4 / 4×5 / 5×5, default
   5×5, PLUS 5×6 and Fluid kept as explicit non-default
   "(Marlow)" choices (`isPixelOption = false`), with `folderCols`
   4 / `folderRows` 3 / `drawerColumns` / `derivedIconSizeDp` on the
   single grid record.
9. **Applied — `dockShadow` REMOVED, all three places together.**
   Field removed from `LauncherSettings`; the "Shadow" SwitchRow
   removed from the Settings Dock screen (now 5 rows, KDoc
   updated); `KEY_DOCK_SHADOW` + save/restore removed from
   LauncherStore; `dock_shadow` put/restore removed from
   BackupManager; the "Dock shadow" row removed from
   `debugToggleStates` (DebugScreen — it would not have compiled
   otherwise). Verified: zero `dockShadow` references remain in
   Kotlin. The home stream had already removed the `shadow(8.dp)`
   application, so the toggle did nothing; depth is tonal now
   (rule 17).
10. **Applied — `badgeOffPackages` persistence.** New
    LauncherStore string-set key `KEY_BADGE_OFF_PACKAGES`
    (`badge_off_packages`, comma-joined like the hidden-apps key)
    with save + restore, and `badge_off_packages` put/restore in
    BackupManager (`list(...).toSet()`). The stale "In-memory for
    now" comments in `LauncherModels.kt` and `HomeViewModel.kt`
    are updated. The per-app switch no longer resets on restart.

## B. Cross-file wiring — applied

1. **Settings P1 — BackHandler for every HomeScreen overlay:
   APPLIED.** The full block sits in `HomeScreen.kt` right after
   the overlay state declarations (after `showContactDockPicker`,
   so every referenced state is declared). One callback per
   overlay, `enabled` follows its open state, close actions mirror
   the existing `onDismiss`/`onClose` exactly. One deliberate
   deviation from the literal P1 snippet: the drawer Back also
   clears universal-search state (`viewModel.clearSearch()`),
   because that is what the existing `closeDrawer()` onClose does
   — the handler mirrors onClose, not a subset of it. The
   settings screen's own BackHandler (composed later) still wins
   while settings is open; the Debug screen keeps its own.
2. **Settings P2 — `onOpenWidgets` wiring: APPLIED.** The
   `LongPressSheet` call in `HomeScreen.kt` passes
   `onOpenWidgets = { longPressApp = null; showWidgetPicker = true;
   widgetPickerPage = PAGE_WIDGET_PAGE }`. The Widgets entry in the
   sheet now opens the picker.
3. **Settings P3 — TalkBack: APPLIED, with one recorded deviation
   and one recorded non-fake.**
   - (a) HomeGrid app-cell semantics: applied on the outer
     (clickable) Box of each APP cell —
     `appIconDescription(name, pageIndex + 1, position among the
     page's APP items)`.
   - (b) FolderTile semantics: applied, but on the outer grid-cell
     Box (the actionable node) rather than on the tile's inner
     Column as the P3 note sketched — so the tap action and the
     announcement ("Social, folder, 6 apps") are one TalkBack
     node instead of two stops. The 2×2 preview minis inside
     `FolderTile` are hidden with `Modifier.decorative()`.
   - (c) `AppIconTile` customActions Remove / Rename / Uninstall:
     applied, with new optional `onRemove` / `onRename` /
     `onUninstall` params (defaults no-op, so existing call sites
     keep compiling). Wired at Dock (`DockBar` gains
     `onRemoveItem` / `onRenameApp` / `onUninstallApp` params,
     passed from HomeScreen to `removeHomeItem`, the long-press
     sheet for rename, `uninstallApp`) and at FolderView
     (`FolderDialog` gains `onRenameApp`; Remove maps to
     `removeAppFromFolder`, Uninstall to `uninstallApp`). Rename
     opens the long-press sheet, where the rename dialog lives —
     that is the existing rename entry point; no second dialog was
     invented. **No Move action: there is no programmatic drag
     entry point, so it is not faked — recorded as a remaining
     gap, same as the drag-from-sheet gap in the settings notes.**
   - (d) Drawer search field: the
     `.semantics { contentDescription = "Drawer search" }` line
     is deleted; the "Search apps" placeholder is the hint.
4. **Settings P5 — area-based layout variant: APPLIED.**
   `HomeScreen.kt` now calls `AdaptiveLayout.variantForWindow(
   smallestScreenWidthDp, screenWidthDp, screenHeightDp)` instead
   of `variantFor(..., configuration.orientation)`. `variantFor`
   is still called only by `variantForWindow` internally and kept
   for source compatibility.
5. **Drawer P3 — "Swipe up to start search" SwitchRow: APPLIED.**
   In the drawer settings section of `SettingsScreen.kt`, wired
   to `settings.swipeUpToSearch`. The gesture side is wired too
   (see A4). Two further SwitchRows were added in the same
   section for the A5 fields ("Show categories", "Drawer
   folders"), with search-index entries for all three; the drawer
   screen is now 15 rows (rule 61 limit) and its KDoc says so.
6. **Drawer P5 — picker wiring: APPLIED.** The
   `WidgetPickerDialog` call in `HomeScreen.kt` passes
   `gridColumns = settings.gridSize.effectiveCols(
   LocalConfiguration.current.screenWidthDp)` and
   `canPlace = { spanX, spanY -> viewModel.firstFreeRect(
   homeItems, widgetPickerPage, spanX, spanY) != null }`, so the
   picker's visible refusal is real.
7. **Drawer P6 — widget span formula + visible refusal: APPLIED.**
   `HomeViewModel.addThirdPartyWidget` uses
   `WidgetHost.spanForMinSizeDp(minWidth, minHeight)` (whole
   cells, coerced to the grid) instead of `(minWidth + 2) / 74`.
   The three silent `?: return@launch` drops
   (`addBuiltinWidget`, `addThirdPartyWidget`, `createStack`) now
   set a `widgetAddError` StateFlow ("No free space on this page
   — remove something first."), shown once as a snackbar in
   `HomeScreen` (reusing the grid-notice snackbar host) and
   consumed; `addThirdPartyWidget` also frees the allocated host
   id on refusal, so nothing is placed and nothing leaks.
8. **Drawer P7 — anchored resize: APPLIED.**
   `HomeViewModel.resizeWidgetAt(itemId, spanX, spanY, subX,
   subY)` added (same clamping + provider-size update as
   `resizeWidget`, plus origin clamping so the widget cannot be
   pushed off-grid), passed as `onResizeAt` at both
   `WidgetResizeOverlay` call sites in `HomeGrid.kt`. Left/top
   handles now move the origin instead of resizing span-only
   anchored top-left.
9. **Drawer P8 — widget configuration flow: APPLIED.** A
   `configureLauncher` (`ACTION_APPWIDGET_CONFIGURE`) lives in
   `HomeScreen.kt` next to the bind launcher. On pick of a
   provider with a configure activity, the bind runs first and
   the configure flow follows (chained via
   `pendingBindNeedsConfigure` when the system wants bind
   confirmation); the widget is placed only on RESULT_OK and the
   host id is freed otherwise. `WidgetItemSheet` gets
   `onReconfigure` (launches the configure intent for the placed
   widget's id; on RESULT_OK nothing is re-added — it
   reconfigures in place).
10. **Drawer P9 — stack same-footprint enforcement: APPLIED.**
    `addWidgetToStack` refuses (visible `widgetAddError`:
    "Stacks hold widgets of the same size only.") when the
    widget's `(spanX, spanY)` differs from the stack item's; the
    widget stays where it was. The stack HomeItems are passed as
    `stackItems` to `WidgetItemSheet`, so the sheet filters its
    offer list to the same footprint too. (`addWidgetToNewStack`
    needs no guard — a new stack takes the widget's own
    footprint.)
11. **Drawer P10 — stack reset signal: APPLIED.** HomeScreen keeps
    `stackResetSignal`, bumped when the home regains focus
    (tracked open→closed transition over every overlay state),
    threaded through `HomePageGrid` to
    `WidgetStackSurface.resetSignal`, and through
    `WidgetPageScreen` / `DesktopWidgetPageScreen` as well, so
    stacks on the widget page reset too.
12. **Drawer P4 — folder focus return on close: APPLIED.**
    HomeScreen keeps `folderFocusRequest: Pair<String, Int>?`
    (folder id + bump, so the same folder closing twice still
    re-requests); `FolderDialog.onDismiss` sets it. It threads
    through `HomePageGrid` to `FolderTile`, which owns a
    `FocusRequester` and requests focus on the matching tile in a
    `LaunchedEffect` (guarded with `runCatching`). This needed no
    larger refactor — the requester lives in the tile, the home
    only passes the signal.
13. **Home P1 — FolderView grid record: APPLIED.** `FolderView.kt`
    uses `settings.gridSize.folderCols` / `folderRows` for both
    `GridCells.Fixed(...)` and `perPage` (4×3 from the record),
    instead of the hard-coded 4 / 12. The 280dp pager height is
    unchanged (three rows of home-sized cells).
14. **Home P5 — permission priming for calendar + contacts:
    APPLIED.** Contacts was already wired (Settings → Search row
    primes before the contacts request). Calendar is now wired the
    same way: new `PrimingKind.CALENDAR`, a
    `calendarGranted` live check in `SettingsScope`, an
    `onRequestCalendarPermission` param on `SettingsScreen` (wired
    in HomeScreen to the existing `calendarPermission` launcher),
    and the At a Glance "Calendar events" row primes when the user
    enables events without the grant. A denial leaves the strip on
    date + weather only. **No auto-fire at launch was reintroduced
    — both permissions are asked only on user action.**
15. **Per-app badge off switch UI: APPLIED in the long-press
    sheet.** The settings stream had NOT added it anywhere
    (verified: no `setBadgeEnabledForPackage` call site existed
    outside `HomeViewModel`). Added as a "Notification badges"
    Switch in `LongPressSheet.kt`, wired to
    `HomeViewModel.setBadgeEnabledForPackage(app.packageName,
    enabled)`, shown only when badges are enabled globally. The
    Notifications screen has no per-app context, so the sheet —
    where the numeric count already lives — is where it belongs;
    the Notifications screen keeps the global Badges rows.
16. **Theme P3 — accent picker Stock-only: APPLIED.** The
    Appearance "Accent colour" row is now disabled unless the
    theme is Stock, with disabledReason "Stock theme only —
    Material You follows your wallpaper" (rule 63
    disabled-with-reason pattern, no parenthetical in the label).
    The theme row default reads Material You via A1.

## Extra wiring done to make the new settings work (not in the
## filed patch requests, but needed for the tree to work as one app)

- **Drawer extension affordances** (`Drawer.kt`): the A5 toggles
  gate real UI. When "Show categories" is on, a chip row (All +
  categories present among the apps, via
  `viewModel.categorize`) filters the grid/list/pages. When
  "Drawer folders" is on, a chip row lists the drawer folders
  (tap opens `DrawerFolderView`, the retained implementation) plus
  a "+ Folder" chip opening a name dialog that calls
  `viewModel.createDrawerFolder`. Both rows are absent in the
  default view (toggles off).
- **Swipe-up gesture side** (see A4): the home swipe-up sets
  `drawerFocusSearch` from the toggle; the mapped OPEN_DRAWER
  gesture action still opens unfocused.

## Decisions with reasoning

1. **Folder description placement (B3b):** the description sits
   on the grid cell's actionable Box, not on `FolderTile`'s inner
   Column as the P3 note sketched — one TalkBack node carrying
   both the tap action and the announcement, matching the app-cell
   pattern in P3(a). The minis are still hidden with
   `Modifier.decorative()` inside `FolderTile`, as the note asked.
2. **Rename from TalkBack (B3c):** opens the long-press sheet,
   where the rename dialog already lives — no second rename UI
   was built. The sheet opening over the folder dialog (folder
   case) is accepted: the folder stays open behind it.
3. **Drawer Back mirrors `closeDrawer()` exactly (B1):** the
   handler clears the drawer query AND universal-search state,
   because that is what the existing onClose does.
4. **Configure-before-place chains bind → configure (B8):** a
   provider that needs both user bind confirmation and
   configuration goes bind first, then configure, then place —
   never placed bare, never placed twice.
5. **Badge position restriction is safe for old data (A7):** the
   store/backup enum helpers fall back to the default on unknown
   names, so a stored `BOTTOM_END` becomes TOP_END instead of
   crashing.
6. **`dockShadow` removal is all-or-nothing (A9):** field +
   settings row + LauncherStore + BackupManager + the DebugScreen
   row were removed together; verified zero references remain.
7. **No Move custom action (B3c):** TalkBack cannot start a drag
   without a programmatic drag entry point, and faking one as a
   no-op action would lie to the user. Recorded as a remaining
   gap, not silently dropped.

## What remains device-gated, and why

- **TalkBack end-to-end** (rule 69): the announcements,
  customActions, folder focus return, and the drawer search hint
  are code-verified only. Still owed on the Pixel 10a: navigate
  home / drawer / settings with TalkBack on; confirm app
  positions, folder counts, that the customActions appear and do
  what they say, and that focus actually lands back on the folder
  icon after closing it.
- **Drawer grid default + extensions:** the GRID default, the
  category tabs, the folder chips / create dialog, and swipe-up-
  to-search (keyboard up with the field focused) need a device
  pass with a real app set.
- **Widget flows:** configuration end-to-end (Android 14+
  exactly-once landing), the visible no-space refusal, anchored
  left/top resize, stack same-footprint refusal, and stack reset
  on returning Home — all code-verified only; a device pass with
  real providers is still owed.
- **Material You default + accent gating:** the launcher must
  visibly re-theme from the wallpaper on API 31+, and the accent
  picker must read disabled under Material You; Stock fallback
  below API 31 needs a device check.
- **Per-app badge switch:** dot off for one app, on for the rest,
  surviving a restart (persistence is new) — needs a real
  notification on device.
- **Calendar priming:** confirm no system dialog appears before
  Allow, and that a denial leaves only the strip degraded.
- CI is the authority for the build (kotlinc 2.2.20 here vs Kotlin
  2.4.20 on CI). Nothing here was verified by a Gradle build or
  on a device.

## Files changed (this integration only)

- `app/src/main/java/app/launcher/model/LauncherModels.kt`
- `app/src/main/java/app/launcher/data/LauncherStore.kt`
- `app/src/main/java/app/launcher/data/BackupManager.kt`
- `app/src/main/java/app/launcher/ui/DebugScreen.kt`
- `app/src/main/java/app/launcher/ui/SettingsScreen.kt`
- `app/src/main/java/app/launcher/ui/Drawer.kt`
- `app/src/main/java/app/launcher/ui/HomeScreen.kt`
- `app/src/main/java/app/launcher/ui/HomeViewModel.kt`
- `app/src/main/java/app/launcher/ui/HomeGrid.kt`
- `app/src/main/java/app/launcher/ui/WidgetPage.kt`
- `app/src/main/java/app/launcher/ui/FolderView.kt`
- `app/src/main/java/app/launcher/ui/AppIcon.kt`
- `app/src/main/java/app/launcher/ui/Dock.kt`
- `app/src/main/java/app/launcher/ui/LongPressSheet.kt`
- `rebuild-notes-integration.md` — this file.

Not touched, per the brief: `FirstRunWizard.kt`, `.github/`, the
manifest, the version, `BUILD_LOG.md` (the coordinator merges
notes; its pre-existing uncommitted modification was left alone).
