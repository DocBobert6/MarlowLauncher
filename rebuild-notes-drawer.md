# Rebuild notes — DRAWER / WIDGETS / FOLDERS stream (Phase 3, wave 1)

Branch `rebuild/drawer`, worktree `~/workspace/launcher-drawer`.
Files owned and rewritten: `ui/Drawer.kt`, `ui/UniversalSearch.kt`,
`ui/WidgetPage.kt`, `ui/Widgets.kt`, `data/WidgetHost.kt`,
`ui/FolderView.kt`, `ui/HeadphoneMusicSheet.kt`, `ui/WallpaperSheet.kt`,
`ui/NotificationPanel.kt`, `ui/VaultSheet.kt`. No other file touched.
`kotlinc-check` green (47 source files; one expected warning, see rule 32).

## What changed, per rule number

### Rule 32 — drawer is a vertical scrolling grid, alphabetical by default
- `Drawer.kt`: new `DrawerGrid` — `LazyVerticalGrid` with the workspace
  column count (`gridSize.effectiveCols`, doubled to max 8 on wide screens
  when the two-column drawer toggle is on), sorted with
  `java.text.Collator` for the device locale. Every installed app exactly
  once; home order untouched.
- Letter sections use ragged section-merge (`buildDrawerGridEntries`):
  a header is emitted only when its section starts a row, so a short
  section shares its row with the next one and no lonely single-icon row
  sits under a letter header (except the unavoidable final row).
- Alphabet fast scroller is now ONE continuous 48dp-wide touch strip
  (`AlphabetScroller`, one `pointerInput` handling finger-down and drag),
  with a scrub-letter bubble and TalkBack previous/next-letter custom
  actions — not 26 tiny targets. The old `AlphabetIndex` (11sp letters,
  ~20dp targets) and `FastScrollbar` are gone; the strip carries the
  thumb as well.
- Category tabs and the "+ Folder" button are removed from the default
  view; the new-folder dialog is gone. No work tab (plan-removed #145/#149).
- Categories / custom order / drawer folders are KEPT as opt-in
  non-default extensions: the CUSTOM sort branch and `DrawerFolderView`
  stay in the file. They are not surfaced in the default view; the
  settings fields that gate them are in the patch requests below.
- List mode (`DrawerList`) and pages mode (`DrawerPages`) stay as explicit
  non-default options. Both lost package names in rows (rule 32 audit
  finding) and badges (rule 50).
- **Default-flip caveat (important):** the grid is selected by the
  `else` branch of the `when (settings.drawerStyle)` in `DrawerScreen`,
  because `DrawerStyle` today has only LIST/PAGES and `LauncherModels.kt`
  is outside this stream. kotlinc-check flags the `else` as redundant
  ("'when' is exhaustive") — that warning disappears the moment the
  model gains `GRID` and the default flips (patch request 1). Until
  that lands, the shipped default is still LIST even though the grid
  code is complete and verified. The coordinator must apply patch
  request 1 for the rebuild to take effect.

### Rule 31 — drawer close
- Swipe-down on the drawer header (handle + title row) closes the
  drawer: a `detectVerticalDragGestures` accumulates downward drag and
  calls `onClose` past 120px. The detector lives on the header only, so
  it never fights the grid's scrolling. The X button stays as well.
- Back handling is NOT in this file — patch request 2 (HomeScreen).
- No drawer button anywhere (already the case).
- Pull-to-search note: the old pull-down-on-handle gesture revealed the
  search field; the same gesture now closes the drawer (rule 31 wins —
  the gesture cannot do both). The search field is visible by placement
  (TOP by default), so nothing is lost in the default view. The
  `pullToSearch` setting is untouched; the settings stream may retire it.

### Rule 33 — drawer search
- Drawer-top search still filters the alphabetical grid as you type;
  the list is apps-only so apps-first holds. Offline search is local
  and cannot hang (unchanged).
- Zero-state (empty query) shows the frequent/recent rows (see rule 34).
- `focusSearch` now also shows the keyboard
  (`LocalSoftwareKeyboardController.show()`), so the "swipe up to start
  search" flow opens the drawer with the field focused and keyboard up.
  The toggle itself lives in drawer settings — patch request 3.

### Rule 34 — predictions honesty
- No prediction row, no fake predictions. The frequent row stays as the
  launcher's own launch history, off by default (`frequentRowVisible`
  unchanged), now labelled "Frequently used" PLUS a visible caption,
  "Based on apps opened on this device" (same for "Recently used"). No
  string anywhere in these files calls it predictions or system
  suggestions. ("Predictions" appears only in code comments explaining
  the absence; `SUGGESTION` survives only as the pre-existing
  `SearchResultKind` enum value in UniversalSearch, which is a search
  provider result kind, not a drawer claim.)

### Rules 9–10, 24 — labels and type
- One 12sp label role for all drawer labels: `MaterialTheme.typography.bodySmall`
  everywhere an app label appears (grid cells, frequent/recent rows, list
  rows, pages, drawer folders). Section headers use `labelMedium`
  (also 12sp). The drawer title uses `titleLarge`.
- ALL hard-coded `.sp` literals at call sites in the ten owned files are
  gone (verified by grep: no `fontSize` / `N.sp` remains). Sheets and
  UniversalSearch now use `titleLarge` / `titleMedium` / `bodyLarge` /
  `bodyMedium` / `bodySmall` only. (The 11sp timestamp in
  NotificationPanel is now `bodySmall`.)
- Icon sizes match home: drawer cells and rows use `AppIconImage` with
  no drawer-only size, so they render at the settings icon size
  (default 56dp), the same as home. Frequent-row icons were 48dp,
  list rows 40dp — both now the home size.

### Rule 50 — badges in drawer
- Badge threading removed from every drawer surface: grid cells, list
  rows, pages, frequent/recent rows, drawer folders. `DrawerScreen`
  still accepts `badgeCounts` / `badgeBitmaps` parameters (defaults) so
  the HomeScreen call site keeps compiling — they are ignored, and the
  comment says so. Home-screen badge rendering is the home stream's.

### Rules 38–39 — folders, open side
- `FolderDialog` keeps its name and signature (HomeScreen call site
  untouched) but is no longer an `AlertDialog`: it is a floating overlay
  — full-screen 32% black scrim (tap-outside closes), centred
  `surfaceContainerHigh` surface with 28dp corners, basic fade/scale
  entrance. The home stays visible, dimmed, behind it. `BackHandler`
  closes it.
- Folder grid is 4 columns × 3 rows per page (`HorizontalPager`,
  `PageIndicator` when there is more than one page), independent of the
  workspace grid (was 3 columns × fixed 300dp).
- Renaming is tap-the-name → type → confirm (checkmark / IME Done) or
  cancel; the name spans the overlay width and does not truncate at the
  folder's width. The always-visible text field + "Done" are gone.
- No "Remove" button under every app: long-press an app (or contact)
  to take it out of the folder.
- The folder TILE (circular 2×2 preview) and drag-to-folder / hover-open
  stay with the home stream (HomeGrid/HomeViewModel).
- Focus return to the folder icon on close: the overlay clears the open
  folder and the home restores; the focus half lives in HomeScreen —
  patch request 4.

### Rule 41 — widget sizing
- `WidgetHost` now carries the platform table: `dpForCells(n) = 70n − 30`,
  `cellsForDp(dp) = (dp + 30) / 70`, `spanForMinSizeDp`. `updateWidgetSize`
  uses the table (was `cells * 74 − 2`).
- The host rounds and pads third-party widgets on every OS version:
  `WidgetMemberContent` wraps the `AppWidgetHostView` in a
  `surfaceContainerHigh` box with 12dp corners and 8dp padding (was
  "no frame"). `WidgetUnavailable` corners 16dp → 12dp (shape table).
- Visible refusal before release: `WidgetPickerDialog` takes an optional
  `canPlace: (spanXHalfCells, spanYHalfCells) -> Boolean` (default
  always-true). A pick whose span does not fit shows an inline refusal
  ("No free space for this widget on this page — remove something
  first.") and picks nothing. The home wires it to the placement math —
  patch request 5. The silent `return@launch` drops in
  `HomeViewModel.addBuiltinWidget` / `addThirdPartyWidget` / `createStack`
  are the same refusal's other half — patch request 6.

### Rule 42 — widget resize
- `WidgetResizeOverlay` now has exactly FOUR handles (left/top/right/
  bottom), each a 48dp hit area with a 32dp visual pill (the halo stays
  only on the handle, per the locked decision). One corner handle with
  ~50% rounding is gone.
- Handles honour the provider's `resizeMode` (looked up in the overlay
  for third-party widgets; built-ins and stacks resize both axes):
  handles for disallowed axes are gone; a non-resizable widget
  (`RESIZE_NONE`) shows no resize frame, only Done.
- Snap threshold is 66% of a cell (`thresholdCells`: a drag must cross
  66% before the span changes; 50% changes nothing). The 150 ms snap
  animation is a later motion pass — the threshold logic is implemented
  now, as instructed.
- Left/top handles need an anchored resize (span + origin move). The
  overlay takes an optional `onResizeAt(spanX, spanY, subX, subY)`
  callback; until HomeViewModel gains it (patch request 7) the left/top
  handles resize span-only, anchored top-left, and the code says so.

### Rule 43 — widget picker
- `WidgetPickerDialog` is rebuilt as a full-screen surface (was a
  text-only `AlertDialog` with labels only): searchable, alphabetical
  per-app groups (collator-sorted), no "recommended" row.
- Truthful previews: the provider's own preview image via
  `AppWidgetProviderInfo.loadPreviewImage` (aspect preserved, rendered
  at 320px wide); a provider with no preview shows label + description
  + size and no image — never a stale screenshot or generic placeholder.
  Descriptions come from `loadDescription`; each row also shows its
  cell span from the platform table.
- Configuration: `WidgetHost.hasConfigureActivity` /
  `configureIntent` are new helpers. A provider with a configure
  activity is never placed bare — the caller runs the configuration
  flow and places only on its result, so a configurable widget lands
  exactly once (Android 14+ behaviour). The launcher side of that flow
  lives in HomeScreen — patch request 8.
- Reconfigure in place: `WidgetItemSheet` gains an optional
  `onReconfigure: ((HomeItem) -> Unit)?` — the "Reconfigure" entry
  shows only for a placed third-party widget whose provider has a
  configure activity AND when the home wires the callback (patch
  request 8).

### Rule 44 — widget stacking
- Stacking is labelled as a Marlow extension wherever it is offered:
  picker ("A Marlow extra, not standard Android behaviour"), the
  long-press sheet (same wording, plus "Stacks hold widgets of the same
  size only."), and the stack surface's TalkBack description.
- Same-footprint membership: the sheet filters offered stacks to the
  widget's footprint when `stackItems` is supplied (new optional param;
  empty by default, in which case the placement layer enforces it) —
  patch request 9 for the ViewModel enforcement.
- Reset to the default member on returning Home: `WidgetStackSurface`
  resets to index 0 on first composition and whenever its new optional
  `resetSignal` changes (the home bumps a counter when the home regains
  focus — patch request 10). Auto-rotate behaviour is otherwise unchanged.

### Rules 15, 18 — sheets
- All owned sheets use type-scale roles only (see rules 9–10 above).
  Dialogs/sheets keep the M3 defaults that already match the shape
  table: 28dp corners, 32% scrim, drag handle on `ModalBottomSheet`.
  The folder overlay explicitly uses 28dp corners and a 32% scrim.
- No user-visible string in the owned files contains a plan/feature
  number or a developer aside (verified by grep).

## LauncherModels defaults changes needed (this stream may not edit that file)
1. `DrawerStyle`: add `GRID("Grid")`; change the default
   `drawerStyle` from `DrawerStyle.LIST` to `DrawerStyle.GRID`.
   (This is what activates the rebuilt default drawer; without it the
   grid code ships but LIST stays the default.)
2. New setting `swipeUpToSearch: Boolean = false` — one toggle, in
   drawer settings, that opens the drawer with the field focused and
   the keyboard up (rule 33). Default off.
3. New settings for the drawer extensions (all off by default, clearly
   labelled as Marlow extensions, not stock behaviour):
   `drawerShowCategories: Boolean = false`,
   `drawerShowDrawerFolders: Boolean = false`. When on, DrawerScreen
   shows the category tabs / folder affordances again (the extension
   code is retained in Drawer.kt; the affordance wiring is a small
   follow-up once these fields exist).

## Patch requests for other files (exact)
1. **LauncherModels.kt** — see "defaults changes" 1–3 above.
2. **HomeScreen.kt — drawer Back handling (rule 31/58).** In the
   `if (showDrawer) { DrawerScreen(...) }` block (currently ~line 767),
   add before/around it:
   `androidx.activity.compose.BackHandler(enabled = showDrawer) { showDrawer = false }`
   so system Back closes the drawer.
3. **SettingsScreen.kt — "Swipe up to start search" toggle (rule 33).**
   In `drawerSettingsSection` (~line 758), add one `SwitchRow`
   ("Swipe up to start search", `settings.swipeUpToSearch`) that updates
   the new setting. The gesture side (setting `drawerFocusSearch = true`
   when opening the drawer from a swipe-up) lives in HomeScreen/Gestures
   and is the home stream's call.
4. **HomeScreen.kt — folder focus return (rule 39).** After the open
   folder closes (`openFolder = null` in the `FolderDialog` `onDismiss`,
   ~line 1060), focus should return to the folder icon. The home stream
   owns focus restoration; nothing is needed from this stream's files.
5. **HomeScreen.kt — widget picker wiring (rules 41/43).** In the
   `WidgetPickerDialog` call (~line 1106), pass:
   `gridColumns = settings.gridSize.effectiveCols(LocalConfiguration.current.screenWidthDp)`
   and
   `canPlace = { spanX, spanY -> viewModel.firstFreeRect(viewModel.homeItems.value, widgetPickerPage, spanX, spanY) != null }`
   (adapt to the actual home-items accessor; `firstFreeRect` is public).
   This makes the picker's visible refusal real.
6. **HomeViewModel.kt — widget span formula + visible refusal (rule 41).**
   In `addThirdPartyWidget` (~line 843), replace
   `((info.minWidth + 2) / 74).coerceIn(1, effectiveCols())` with
   `WidgetHost.spanForMinSizeDp(info.minWidth, info.minHeight).first.coerceIn(1, effectiveCols())`
   (and `.second` for cellsY, `coerceIn(1, grid.rows)`). Replace the
   silent `?: return@launch` in `addBuiltinWidget` (~line 792),
   `addThirdPartyWidget` (~line 848) and `createStack` (~line 936)
   with a visible refusal: expose the failure (e.g. a
   `StateFlow<String?> widgetAddError` the home shows as a snackbar /
   toast: "No free space on this page — remove something first.")
   and free any allocated host id.
7. **HomeViewModel.kt — anchored widget resize (rule 42).** Add
   `fun resizeWidgetAt(itemId: String, spanX: Int, spanY: Int, subX: Int, subY: Int)`
   (same clamping and `updateWidgetSize` call as `resizeWidget`, but
   also writing `subX`/`subY`), and pass it as `onResizeAt` in the two
   `WidgetResizeOverlay` call sites in HomeGrid.kt (~lines 286, 324).
8. **HomeScreen.kt — widget configuration + reconfigure (rule 43).**
   Add an `ActivityResultLauncher` for `ACTION_APPWIDGET_CONFIGURE`:
   on pick of a provider where `WidgetHost.hasConfigureActivity(info)`
   is true, launch `WidgetHost.configureIntent(context, appWidgetId, info)`
   and call `viewModel.addThirdPartyWidget(...)` only on RESULT_OK
   (free the id otherwise — same pattern as the existing bind flow).
   For reconfigure, pass `onReconfigure = { item -> <launch configure intent for item.appWidgetId> }`
   to `WidgetItemSheet` (~line 1139); on RESULT_OK nothing needs
   re-adding — the widget reconfigures in place.
9. **HomeViewModel.kt — stack same-footprint enforcement (rule 44).**
   In `addWidgetToStack` (~line 949), refuse when the widget's
   `(spanX, spanY)` differs from the stack item's `(spanX, spanY)`
   (look up the `WIDGET_STACK` HomeItem by `stackId`); same guard in
   `addWidgetToNewStack` is unnecessary (a new stack takes the widget's
   own footprint). Also pass the stack HomeItems as `stackItems` to
   `WidgetItemSheet` in HomeScreen (~line 1139) so the sheet can filter
   its offer list.
10. **HomeScreen.kt — stack reset signal (rule 44).** Keep a
    `var stackResetSignal by remember { mutableStateOf(0) }`, bump it
    when the home regains focus (drawer/search/sheets close), and pass
    `resetSignal = stackResetSignal` to `WidgetStackSurface` — that
    requires threading the parameter through `HomePageGrid` (HomeGrid.kt),
    where `WidgetStackSurface` is called (~line 313).

## Decisions with reasoning
- The grid default is implemented behind the `else` branch rather than
  by editing LauncherModels: the task forbids touching that file, and
  an `else` keeps the branch compiling (with one explicit kotlinc
  warning) until the model patch lands. The alternative — keying the
  grid off LIST — would have destroyed list mode as an option.
- Pull-to-search yields to swipe-down-to-close on the same header
  gesture: rule 31 is a pass/fail rule; pull-to-search is an
  extension, and the search field is visible by placement anyway.
- Badge parameters are kept (ignored) on `DrawerScreen` rather than
  removed: the HomeScreen call site is another stream's file, and
  removing them would break compilation of the merged tree.
- The folder overlay uses a fixed 280dp pager height: the grid inside
  the pager needs a bounded height, and three rows of home-sized cells
  fit in 280dp. The overlay itself wraps content up to 560dp.
- The picker's refusal check defaults to always-allow: the placement
  truth lives in the ViewModel, and inventing a second free-space
  computation in the picker would drift from the real one.
- Contacts in a folder paginate after the apps in the same 4×3 grid;
  the offset math is in FolderView.kt and covered by the two worked
  examples in the file's reasoning (14 apps + 3 contacts; 5 apps + 20
  contacts).

## What remains device-gated, and why
- Drawer grid feel (scroll physics, scroller thumb travel, ragged
  headers on a real app set), swipe-down-to-close, folder overlay
  dim/fade on a real wallpaper, widget picker previews for real
  providers, resize-handle hit areas, stack reset on returning Home:
  all are code-verified only. No device proof exists — local Gradle
  builds are impossible in this sandbox and CI is the authority; a
  Pixel 10a pass is still owed for each.
- The 150 ms resize snap animation is a later motion pass (rule 42
  explicitly defers it); the 66% threshold logic is in place.
- Circular-reveal folder-open animation is a later motion pass (basic
  fade/scale is in place).
- Widget configuration end-to-end (Android 14+ exactly-once landing)
  cannot be proven until patch request 8 is wired and run on device.
- The default drawer flip itself is gated on patch request 1.

## Verification
- `tools/kotlinc-check.sh`: PASS, 47 source files. One warning:
  `Drawer.kt:387: 'when' is exhaustive so 'else' is redundant here` —
  expected; it is the future-GRID branch (see rule 32 caveat) and
  disappears once the model gains GRID.
- Grep: no `fontSize` / `N.sp` literals and no plan/feature numbers in
  user-visible strings in the ten owned files.
- `git status`: only the ten owned files modified, plus this note.
