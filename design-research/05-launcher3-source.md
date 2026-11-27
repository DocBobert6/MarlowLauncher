# Track 5 — Reading the AOSP Launcher3 Source Itself

**Project:** Marlow Launcher (`app.launcher`, Compose + Material 3, Pixel-style)
**Date:** 2026-11-26
**Scope:** Research only. No code was touched. All findings are grounded in the
actual AOSP Launcher3 tree (`platform/packages/apps/Launcher3`) as published on
android.googlesource.com, plus the source-citing secondary survey
*AOSP Internals* ch. 49, which quotes the same files. Where two AOSP revisions
disagree (e.g. a timeout constant that changed), both values are recorded and
flagged rather than silently picking one.
**Version note:** Launcher3 is a moving target. File paths below are the
current (2025–2026, Android 16 QPR2 / main) layout unless marked "older".
Older forks (Trebuchet / CM-14.1 mirrors on javatips.net) were used only to
confirm long-stable behaviour, never as the sole source for a current claim.

## Primary sources consulted

| # | Source | What it established |
|---|--------|---------------------|
| S1 | `android.googlesource.com/platform/packages/apps/Launcher3` — `src/com/android/launcher3/DeviceProfile.java` | Runtime profile: margins, border space, cell size, `MAX_HORIZONTAL_PADDING_PERCENT = 0.14f`, scalable vs non-scalable grid |
| S2 | Same tree — `src/com/android/launcher3/InvariantDeviceProfile.java` | IDP: `numRows/numColumns`, folder rows/cols, hotseat counts, `numAllAppsColumns`, `TYPE_PHONE/TYPE_MULTI_DISPLAY/TYPE_TABLET`, `ICON_SIZE_DEFINED_IN_APP_DP = 48`, partner overrides, per-grid `dbFile` |
| S3 | Same tree — `src/com/android/launcher3/CellLayout.java` (via search excerpts + AOSP Internals §49.2.2) | `mCountX/mCountY = inv.numColumns/numRows`, `GridOccupancy`, `ShortcutAndWidgetContainer`, `ContainerType {WORKSPACE, HOTSEAT, FOLDER}` |
| S4 | Same tree — `src/com/android/launcher3/Workspace.java` | `Workspace extends PagedView implements DropTarget, DragSource…`, `REORDER_TIMEOUT` (650 current / 350 older), folder-creation flags, `acceptDrop` order, `SNAP_OFF_EMPTY_SCREEN_DURATION = 400`, `FADE_EMPTY_SCREEN_DURATION = 150`, `ADJACENT_SCREEN_DROP_DURATION = 300` |
| S5 | Same tree — `src/com/android/launcher3/dragndrop/DragController.java` | Abstract drag controller, `DragListener`, `DropTarget` list, `DEEP_PRESS_DISTANCE_FACTOR = 3`, spring-loaded mode on drag start |
| S6 | Same tree — `src/com/android/launcher3/folder/Folder.java`, `folder/FolderIcon.java`, `folder/PreviewItemManager.java`, `folder/ClippedFolderIconLayoutRule.java` | Folder as `AbstractFloatingView` + `DragSource` + `DropTarget`; preview = max 4 items; `DROP_IN_ANIMATION_DURATION = 400`, `INITIAL_ITEM_ANIMATION_DURATION = 350`, `FINAL_ITEM_ANIMATION_DURATION = 200`; spring-load open delay 800 ms (`ON_OPEN_DELAY`) |
| S7 | Same tree — `src/com/android/launcher3/AppWidgetResizeFrame.java` | 4 handles, `SNAP_DURATION = 150`, `RESIZE_THRESHOLD = 0.66f`, `HANDLE_COUNT = 4`, min/max spans, `getDefaultPaddingForWidget` |
| S8 | Same tree — `src/com/android/launcher3/LauncherSettings.java` | `Favorites` table: `ITEM_TYPE_*`, `CONTAINER_DESKTOP = -100`, `CONTAINER_HOTSEAT = -101`, `CONTAINER_PREDICTION = -102`, `CONTAINER_HOTSEAT_PREDICTION = -103`, `CONTAINER_ALL_APPS = -104` (per AOSP Internals §49.2.1) |
| S9 | Same tree — `src/com/android/launcher3/LauncherProvider.java` / `src/com/android/launcher3/model/ModelDbController.java` | Default-layout priority: (1) app restrictions, (2) Play-store package, (3) partner APK, (4) built-in default; `loadDefaultFavoritesIfNecessary()` |
| S10 | Same tree — `src/com/android/launcher3/allapps/ActivityAllAppsContainerView.java` | Modern drawer: `AdapterHolder {MAIN, SEARCH, WORK}`, `AlphabeticalAppsList` per tab, `AllAppsSearchUiDelegate`, floating header, 300 ms search transition |
| S11 | Same tree — `quickstep/src/com/android/launcher3/uioverrides/QuickstepLauncher.java`, `quickstep/src/com/android/launcher3/BaseQuickstepLauncher.java`, `quickstep/src/com/android/quickstep/LauncherActivityInterface.java`, `quickstep/src/com/android/quickstep/AbsSwipeUpHandler.java` | Quickstep: `QuickstepLauncher extends Launcher`, `TouchInteractionService` → `OverviewCommandHelper` / `AbsSwipeUpHandler` → `RecentsView`/`TaskView`, taskbar via `TaskbarController` |
| S12 | Same tree — `src/com/android/launcher3/Utilities.java`, `src/com/android/launcher3/icons/BitmapInfo.java`, `src/com/android/launcher3/icons/LauncherIcons.java` (via excerpts) | `getFullDrawable(…, useTheme)`, `AdaptiveIconDrawable.getMonochrome()` + `Themes.isThemedIconEnabled()`, `BitmapInfo.TYPE_DEFAULT/TYPE_THEMED`, `LOW_RES_ICON` 1×1 placeholder |
| S13 | Same tree — `src/com/android/launcher3/BubbleTextView.java`, `badge/BadgeRenderer.java`, `dot/` | Badge/dot drawing: `badge_size = 24dp`, `badge_text_size = 12dp` (older dimens), count > 0 → number, count == 0 + icon → icon, else dot; folders show a dot if any child is badged |
| S14 | `aospbooks.github.io/aosp-internal-book/49-launcher3/` — *AOSP Internals*, ch. 49 | Architecture survey quoting the files above: layout tree, `LauncherState` ordinals, model/view split, responsive specs, predictions (§49.7.7), taskbar |
| S15 | `android.googlesource.com/.../Launcher3/+/1c883d861e^!/` (diff) + `res/xml/device_profiles.xml` discussion | Grid options `3_by_3 / 4_by_4 / 5_by_5 / 6_by_5 / desktop_6_by_5`; tablet `6_by_5` added 2021; each grid-option carries its own `dbFile` and `defaultLayoutId` |
| S16 | `src/com/android/launcher3/settings/SettingsActivity.java` + `res/xml/launcher_preferences.xml` (via a Launcher3-fork settings doc — treat as fork evidence, flagged where AOSP differs) | Settings entry via long-press → `ACTION_APPLICATION_PREFERENCES`; `LauncherSettingsFragment extends PreferenceFragmentCompat` |

**AOSP vs Pixel Launcher caveat (important for Marlow):** the AOSP tree is
*not* the Pixel Launcher. Pixel-only behaviour — At a Glance / Smartspace,
the Google search bar, some prediction surfaces, "suggested apps" polish —
lives in closed Google overlays or the `NexusLauncherPrebuilt` APK, not in
`platform/packages/apps/Launcher3`. Where this document says "Launcher3 does
X", it means the open-source base. Where Pixel differs, it is flagged
explicitly. Marlow should copy AOSP mechanics and Pixel *feel*, and must not
assume a Pixel-only feature exists in the AOSP source.

---

## 1. Architecture overview

### 1.1 Top-level layout

```
platform/packages/apps/Launcher3/
  src/                    # core launcher (com.android.launcher3.*)
  quickstep/              # gesture nav, recents, taskbar (only when built with Quickstep)
  src_no_quickstep/       # stubs for builds without quickstep
  src_plugins/            # plugin interfaces
  shared/                 # shared code incl. TestProtocol
  res/                    # layouts, dimens, res/xml/device_profiles.xml,
                          #   res/xml/default_workspace_*.xml
  modules/                # Compose modules, e.g. modules/widgetpicker
  aconfig/  dagger/  go/  tests/  protos/
  AndroidManifest.xml  Android.bp
```

Core packages under `src/com/android/launcher3/` (S14 §49.1.1): `allapps/`,
`dragndrop/`, `folder/`, `widget/`, `model/`, `icons/`, `graphics/`,
`statemanager/`, `celllayout/`, `search/`, `responsive/`, `touch/`, `anim/`,
`config/`, `deviceprofile/`, `views/`, `popup/`, `shortcuts/`, `logging/`.

### 1.2 The singleton hub and the model/view split

- **`Launcher`** (`src/com/android/launcher3/Launcher.java`) extends
  `StatefulActivity<LauncherState>` and implements
  `InvariantDeviceProfile.OnIDPChangeListener`. It owns: `Workspace`,
  `DragLayer`, `Hotseat`, `ActivityAllAppsContainerView`, `ScrimView`,
  `LauncherDragController`, `AllAppsTransitionController` (S14 §49.1.2).
- **`LauncherAppState`** is the (now `@Deprecated`, migrating to Dagger)
  singleton aggregating `iconProvider`, `iconCache`, `model`,
  `invariantDeviceProfile`, `isSafeModeEnabled` (S14 §49.1.4). New code
  should inject the individual singletons rather than reach through the hub.
- **`LauncherModel`** (`@LauncherAppSingleton`, Kotlin) holds `BgDataModel`
  (workspace items, folders, widgets, screen order) and `AllAppsList`
  (complete launchable activities). Loading runs on `MODEL_EXECUTOR`, a
  dedicated background thread; the UI is bound via `Callbacks` /
  `ModelCallbacks` (`bindWorkspace`, `bindAllApps`, `bindWidgets`,
  `bindItemsAdded/Updated`, `bindCompleteModel`) (S14 §49.1.5–49.1.6).
  **All DB mutations go through `ModelWriter` on the model thread.**
- **`Launcher.onCreate()` order** (S14 §49.1.3): trace → `LauncherAppState`
  → `initDeviceProfile(idp)` → `initDragController()` + `StateManager(NORMAL)`
  → widget holder (`LauncherWidgetHolder.newInstance`, `startListening()`)
  → `setupViews()` → `model.addCallbacksAndLoad(...)` / `activate()`.
  Lesson for Marlow: device profile and drag controller exist *before* views
  are inflated; widget listening starts before the model loads.

### 1.3 The state machine

`StateManager` drives `LauncherState` (S14 §49.1.7). Stable ordinals
(shared with the test harness in `TestProtocol`):

| State | Ordinal | Meaning |
|---|---|---|
| `NORMAL` | 0 | Default workspace |
| `SPRING_LOADED` | 1 | Workspace shrunk during drag |
| `OVERVIEW` | 2 | Recents (Quickstep) |
| `OVERVIEW_MODAL_TASK` | 3 | Task menu open |
| `QUICK_SWITCH` | 4 | Quick-switch gesture |
| `ALL_APPS` | 5 | Drawer open |
| `BACKGROUND_APP` | 6 | App in foreground |
| `HINT_STATE` / `HINT_STATE_TWO_BUTTON` | 7 / 8 | Swipe-up hint |
| `OVERVIEW_SPLIT_SELECT` | 9 | Split-screen selection |
| `EDIT_MODE` | 10 | Workspace customisation |
| `DESKTOP_DRAG_MODE` | 11 | Drag into a desktop window |

Each state is a set of flags (`HOTSEAT_ICONS`, `ALL_APPS_CONTENT`,
`WORKSPACE_PAGE_INDICATOR`, `FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED`,
`FLAG_RECENTS_VIEW_VISIBLE`, …) applied by `StateHandler`s — e.g.
`AllAppsTransitionController` moves/fades the drawer. Marlow's Compose
equivalent should be a single state machine with the same mutually-exclusive
states, not a pile of independent booleans.

### 1.4 The data model — `ItemInfo`

Every home-screen element is an `ItemInfo` subclass (S14 §49.2.1, S8):

```java
// src/com/android/launcher3/model/data/ItemInfo.java
public int id, itemType, container, screenId;
public int cellX = -1, cellY = -1;
public int spanX = 1, spanY = 1, minSpanX = 1, minSpanY = 1;
public int rank = 0;
public CharSequence title;
```

`itemType` (S8): `APPLICATION = 0`, `SHORTCUT = 1` (deprecated, use
`DEEP_SHORTCUT = 6`), `FOLDER = 2`, `APPWIDGET = 4`, `CUSTOM_APPWIDGET = 5`,
`TASK = 7`, `QSB = 8`, `SEARCH_ACTION = 9`, `APP_PAIR = 10`.
`container`: `CONTAINER_DESKTOP = -100`, `CONTAINER_HOTSEAT = -101`,
`CONTAINER_PREDICTION = -102`, `CONTAINER_HOTSEAT_PREDICTION = -103`,
`CONTAINER_ALL_APPS = -104`; a folder's own id is the container for its
children. Subclasses: `ItemInfoWithIcon` (adds `BitmapInfo bitmap`) →
`WorkspaceItemInfo` / `AppInfo` / `PackageItemInfo`; `CollectionInfo` →
`FolderInfo` (holds `contents` + `suggestedFolderNames`) and `AppPairInfo`;
`LauncherAppWidgetInfo` (holds `appWidgetId`, `providerName`).

**Marlow translation:** one sealed `ItemInfo`-equivalent with
`(id, type, container, screenId, cellX, cellY, spanX, spanY, minSpanX,
minSpanY, rank, title)` is the right persistence shape — it is literally the
`favorites` table schema, and it makes grid migration, backup/restore and
folder membership fall out naturally. Do not invent a second, divergent
workspace model.

---

## 2. Workspace and grid

### 2.1 Two-tier profiles: invariant vs runtime

- **`InvariantDeviceProfile` (IDP)** — device-independent, loaded from
  `res/xml/device_profiles.xml`. Fields (S2): `numRows`, `numColumns`,
  `numSearchContainerColumns`, `numFolderRows[]`, `numFolderColumns[]`,
  `iconSize[]` (per orientation/type index), hotseat counts
  (`numShownHotseatIcons`, `numDatabaseHotseatIcons`,
  `numShrunkenHotseatIcons`), `numAllAppsColumns`,
  `numDatabaseAllAppsColumns`, `dbFile`, `defaultLayoutId`, plus
  `horizontalMargin[]` and `borderSpaces[]` per type index. Type indices:
  `INDEX_DEFAULT`, `INDEX_LANDSCAPE`, `INDEX_TWO_PANEL_LANDSCAPE`,
  `INDEX_TWO_PANEL_PORTRAIT` (S1). Device types: `TYPE_PHONE = 0`,
  `TYPE_MULTI_DISPLAY = 1`, `TYPE_TABLET = 2` (and `TYPE_DESKTOP = 3` in
  newer code, S14 §49.2.6).
- **`DeviceProfile`** — runtime profile computed for the current display
  configuration from the IDP + `DisplayMetrics` + window size + orientation.
  It delegates to sub-profiles in current code: `WorkspaceProfile`,
  `HotseatProfile`, `FolderProfile`, `AllAppsProfile`, `OverviewProfile`,
  `TaskbarProfile`, `DropTargetProfile`, `BottomSheetProfile` (S14 §49.2.6).
  Older code kept the fields flat on `DeviceProfile` (`cellWidthPx`,
  `cellHeightPx`, `cellLayoutBorderSpacePx`, `cellLayoutPaddingPx`,
  `desiredWorkspaceHorizontalMarginPx`, `iconSizePx`, `allAppsIconSizePx`,
  `hotseatIconSizePx`, …) — same numbers, different packaging.

### 2.2 Grid options in `device_profiles.xml`

Each `<grid-option>` names a grid and binds it to a DB file and a default
layout (S14 §49.2.6, S15):

```xml
<grid-option launcher:name="4_by_4"
    launcher:numRows="4" launcher:numColumns="4"
    launcher:numFolderRows="3" launcher:numFolderColumns="4"
    launcher:numHotseatIcons="4" launcher:numExtendedHotseatIcons="6"
    launcher:dbFile="launcher_4_by_4.db"
    launcher:defaultLayoutId="@xml/default_workspace_4x4"
    launcher:deviceCategory="phone|multi_display">
    <display-option launcher:name="…" launcher:minWidthDps="…"
        launcher:minHeightDps="…" launcher:iconSize="…" … />
</grid-option>
```

Known grid names: `3_by_3`, `4_by_4`, `5_by_5`, `6_by_5`,
`desktop_6_by_5`, plus a `fixed_landscape_mode` profile. The 2021 tablet
change (S15) added `6_by_5` (`deviceCategory="tablet"`,
`dbFile="launcher_6_by_5.db"`, `defaultLayoutId="@xml/default_workspace_6x5"`).
Pixel Launcher's user-facing grid picker (2×2 / 3×3 / 4×4 / 5×5, 5×5
default on Pixel 5-era devices) is this same mechanism surfaced in
Settings; the *closest* `display-option` to the real display size wins
(`InvariantDeviceProfile` picks the closest profile by min-width/height in
dp — e.g. `5_by_5` "Large Phone" at 406 dp × 694 dp). **Changing grid =
changing DB file**: each grid has its own `launcher_<grid>.db`, which is why
switching grids in Pixel Launcher re-lays-out (and can lose) placements
rather than re-flowing them in place. Grid choice is persisted as
`GRID_NAME` in `LauncherPrefs` (S2).

### 2.3 `CellLayout` — the grid container

- `CellLayout extends ViewGroup`; every workspace page **and** the hotseat
  are `CellLayout`s (S3, S14 §49.2.2). Constructor takes
  `mCountX = grid.inv.numColumns`, `mCountY = grid.inv.numRows` and creates
  `mOccupied = new GridOccupancy(mCountX, mCountY)`.
- `GridOccupancy` (`src/com/android/launcher3/util/GridOccupancy.java`) is
  a `boolean[][] cells` — the single source of truth for "is this cell
  free". All placement, reorder and drop-acceptance consult it.
- Children are laid out by an inner `ShortcutAndWidgetContainer`, which
  applies `CellLayoutLayoutParams` (`cellX/cellY`, `cellHSpan/cellVSpan`,
  `useTmpCoords` + `tmpCellX/tmpCellY` for in-flight reorder previews).
  `CellLayout` itself draws the drop-target highlight rectangle
  (`setWillNotDraw(false)` — "A ViewGroup usually does not draw, but
  CellLayout needs to draw a rectangle to show the user where a dragged
  item will land").
- `ContainerType` annotation: `WORKSPACE`, `HOTSEAT`, `FOLDER` (S3).
- Cell size: `DeviceProfile.calculateCellWidth(childWidth, mCountX)` /
  `calculateCellHeight(...)` in `onMeasure`; `cellWidthPx/cellHeightPx`
  are derived from available width/height minus padding and border space,
  **not** hard-coded. Border space comes from the IDP:
  `getCellLayoutBorderSpace()` returns `(0,0)` when not a scalable grid,
  otherwise `pxFromDp(idp.borderSpaces[typeIndex])` (S1).
- Horizontal margin: capped — `MAX_HORIZONTAL_PADDING_PERCENT = 0.14f`
  (14% of screen width max, S1). Scalable grids use
  `pxFromDp(idp.horizontalMargin[typeIndex])`; non-scalable grids use
  `R.dimen.dynamic_grid_left_right_margin`.

### 2.4 `Workspace` — the paging container

`Workspace extends PagedView` and implements `DropTarget, DragSource,
DragScroller, …` (S4). Behaviour that defines the stock feel:

- Horizontal paging between a finite number of pages; pages are created on
  demand when dragging to the edge (`mAddNewPageOnDrag = true`) and empty
  trailing pages are removed (snap-off 400 ms, fade 150 ms).
- Wallpaper scrolling via `WallpaperOffsetInterpolator` — the wallpaper
  parallaxes with page scroll; Marlow must scroll wallpaper with page
  offset, not keep it static.
- **Spring-loaded mode:** while dragging, the workspace shrinks (pages
  scale down, `SPRING_LOADED` state) so the user can see more pages and
  drop targets; `SpringLoadedDragController` manages entry/exit, with
  `SPRING_LOADED_EXIT_DELAY` on exit.
- Drop is only allowed once the state transition is far enough along:
  `transitionStateShouldAllowDrop()` requires
  `mTransitionProgress > ALLOW_DROP_TRANSITION_PROGRESS (0.25f)` (S4).
- Adjacent-page drop animation: `ADJACENT_SCREEN_DROP_DURATION = 300` ms.

### 2.5 Hotseat

`Hotseat extends CellLayout` (S14 §49.2.5): a single-row grid
(`mCountY = 1`), items not bound to a screen id, and it participates in
predictions (see §7). The QSB can be inline in the hotseat on some
profiles (`mHotseatProfile.isQsbInline()`); on phones it is usually a
separate bar above the hotseat. Hotseat icon count is grid-dependent
(`numHotseatIcons`, `numExtendedHotseatIcons`, `numShownHotseatIcons` vs
`numDatabaseHotseatIcons` — the DB can hold more than are shown, e.g. 6
stored / 5 shown, so predictions can rotate without losing user pins).

### 2.6 Icon sizing

- Base icon size constant: `ICON_SIZE_DEFINED_IN_APP_DP = 48` (S2) — the
  Android adaptive-icon spec size. Actual `iconSizePx` is computed per
  display-option and scaled by the grid; Pixel-era grids used ~57 dp for
  5×5, ~66 dp for 4×4/4×5, ~89 dp for 3×3 (fork/secondary evidence — treat
  as indicative, not AOSP-canonical).
- `DeviceProfile` exposes separate sizes: workspace `iconSizePx`,
  `allAppsIconSizePx`, `hotseatIconSizePx`, `folderChildIconSizePx`,
  plus text sizes. Labels are part of the cell: the icon drawable sits
  above the label inside `BubbleTextView`; cell height must fit
  icon + label + padding, and hiding labels does not change the grid.
- Responsive grid (`src/com/android/launcher3/responsive/`,
  S14 §49.2.8): newer AOSP selects a `ResponsiveSpec` from XML
  (`spec_col_count_3_row.xml`, `spec_handheld_all_apps_3_row.xml`, …)
  based on available dimensions at runtime, adjusting cell sizes/spacing
  continuously rather than only at the coarse `display-option` steps.
  Marlow on Compose should treat the responsive-spec idea — continuous
  scaling with breakpoints — as the modern target, with the
  `device_profiles.xml` grid-options as the coarse fallback.

---

## 3. Drag and drop

### 3.1 The cast

| Role | Class | Notes |
|---|---|---|
| Controller | `dragndrop/DragController.java` (abstract; `LauncherDragController` in Launcher) | Owns the drag; implements `DragDriver.EventListener`, `TouchController` |
| Layer | `dragndrop/DragLayer.java` (`BaseDragLayer`) | Full-screen overlay `ViewGroup` that hosts the `DragView` and floating views (folders, popups, resize frame) above everything |
| Visual | `dragndrop/DragView.java` | The floating bitmap/scaled copy of the dragged item |
| Contract (target) | `DropTarget.java` | `isDropEnabled()`, `acceptDrop(DragObject)`, `onDrop`, `onDragEnter/Exit/Over`, `getDropLocation` |
| Contract (source) | `DragSource.java` | `onDropCompleted`, `supportsDrop`, `onFlingToDelete` etc. |
| Payload | `DropTarget.DragObject` | `dragInfo` (the `ItemInfo`), `dragSource`, `dragView`, `x/y`, `dragComplete` |
| Options | `dragndrop/DragOptions.java` | `isAccessibleDrag`, `PreDragCondition`, `intrinsicIconScaleFactor` |
| Driver | `DragDriver` | Turns touch motion into drag events; deep-press needs `DEEP_PRESS_DISTANCE_FACTOR = 3`× the normal distance to start (S5) |

`DragController` keeps `mDropTargets: ArrayList<DropTarget>` and
`mLastDropTarget`; every `DropTarget` view registers itself
(`mDragController.addDropTarget(...)` — Workspace registers each child
that is a `DropTarget` when it is added, S4). `DragListener`s get
`onDragStart(dragObject, options)` / `onDragEnd()`.

### 3.2 Drag lifecycle

1. Long-press (or accessible-drag) on a `BubbleTextView` / widget →
   `Workspace.beginDragShared(...)` → `DragController.startDrag(...)`
   creates the `DragView` (a bitmap of the item, scaled up — the dragged
   icon is drawn *larger* than its cell icon; `scale * iconScale` in
   `Workspace`, S4) and puts the UI into spring-loaded mode.
2. While dragging, the controller hit-tests drop targets in order and calls
   `onDragOver`; the `Workspace` continuously computes the target cell from
   the drag view's visual centre (`mDragViewVisualCenter`) and runs
   `CellLayout.performReorder(…, MODE_ACCEPT_DROP)` as a *preview* —
   neighbours slide aside live, using `useTmpCoords` so the real layout is
   untouched until drop.
3. **Reorder timing:** `REORDER_TIMEOUT` — **650 ms in current AOSP**
   (S4, current `Workspace.java`), **350 ms in older revisions** (older
   `Workspace.java` excerpt). The alarm fires only after the drag view has
   hovered over a cell for that long; this is the "hover to push items
   aside" delay, and it is why stock dragging feels deliberate rather than
   twitchy. Marlow should pick 650 ms as the current value and note the
   older 350 ms as a historical variant.
4. Drop: `Workspace.acceptDrop()` decides in this exact order (S4):
   a. if `mCreateUserFolderOnDrop && willCreateUserFolder(…)` → create folder;
   b. else if `mAddToExistingFolderOnDrop && willAddToExistingUserFolder(…)` → add to folder;
   c. else `performReorder(…, MODE_ACCEPT_DROP)` → place at `mTargetCell`.
   The drop target then exits spring-loaded mode; a cancelled drop exits it
   automatically (`DragController.startDrag` javadoc, S5).
5. Dragging to the screen edge auto-scrolls pages (`DragScroller`) and can
   create a new page (`mAddNewPageOnDrag`); dragging to the top shows the
   uninstall/remove drop targets (`UninstallDropTarget`,
   `DeleteDropTarget` — the "Remove / Uninstall" bar that appears at the
   top during a drag).

### 3.3 Folder creation by drag

- Hovering one shortcut over another sets `mCreateUserFolderOnDrop`;
  the distance test is `CellLayout.getDistanceFromCell(visualCentre,
  targetCell)` against `mMaxDistanceForFolderCreation` (S4).
  `FOLDER_CREATION_TIMEOUT = 0` in the older code — creation is decided
  *on drop*, not on a hover timer (the ring animation is the only hover
  feedback). Do not implement a hover-timer folder creation; the stock
  behaviour is drop-gated.
- Visual feedback while hovering: `FolderRingAnimator` draws a ring around
  the target `FolderIcon` (`mDragFolderRingAnimator`, S4).
- On drop, the two icons are replaced by a `FolderIcon` and a create
  animation runs (`performCreateAnimation`, 350 ms initial-item +
  400 ms drop-in — see §4).
- A folder left with one item is *destroyed* on close and replaced by that
  item (`replaceFolderWithFinalItem()`, S6 older `Folder.java`); a folder
  left empty is deleted. Marlow must implement this collapse rule — it is
  the reason stock folders never linger with a single icon.

### 3.4 Widget drag and resize

- Widgets are dragged like icons but with their span (`spanX/spanY`,
  `minSpanX/minSpanY`). `performReorder` must find a free rectangle of
  that span; if none fits, the drop is rejected (no auto-shrink in stock
  Launcher3 — the widget simply will not land).
- Long-press on a placed widget (or the widget's popup) opens
  **`AppWidgetResizeFrame`** (S7), an `AbstractFloatingView` added to the
  `DragLayer`. It has exactly **4 handles** (left/top/right/bottom),
  `SNAP_DURATION = 150` ms, `RESIZE_THRESHOLD = 0.66f` (the drag must
  cross 66% of a cell before the span changes — this is the anti-jitter
  rule), `mMinHSpan/mMinVSpan/mMaxHSpan/mMaxVSpan` taken from the
  provider's `resizeMode` (`RESIZE_HORIZONTAL`, `RESIZE_VERTICAL`,
  `RESIZE_NONE`, `RESIZE_BOTH` — handles for disallowed axes are `GONE`).
  Resize padding comes from
  `AppWidgetHostView.getDefaultPaddingForWidget(...)`. The frame dims
  non-active handles (`DIMMED_HANDLE_ALPHA = 0f`) and, on two-panel
  layouts, fades the sibling `CellLayout` to
  `MIN_OPACITY_FOR_CELL_LAYOUT_DURING_INVALID_RESIZE = 0.5f` when the
  resize would cross the panel boundary.
- **No widget stacking in AOSP Launcher3.** There is no stacked-widget /
  Smart Stack concept in the tree: one `LauncherAppWidgetInfo` occupies one
  cell rectangle; overlapping widgets are impossible by `GridOccupancy`.
  (Pixel's At a Glance is a single custom widget, not a stack.) Marlow
  must not present stacking as stock behaviour; if it adds stacking, it is
  a deliberate Marlow extension, not AOSP fidelity.

---

## 4. Folders

### 4.1 Model — `FolderInfo`

`FolderInfo extends CollectionInfo` (S14 §49.2.1): an `ArrayList<ItemInfo>
contents` plus `FolderNameInfos suggestedFolderNames` (system-suggested
names, e.g. from app categories). A folder is a row in `favorites` with
`itemType = ITEM_TYPE_FOLDER (2)`; its children are rows whose
`container` is the folder's id. `FolderListener` notifies the icon when
contents change.

### 4.2 The closed folder — `FolderIcon` + preview

- `FolderIcon extends FrameLayout implements FolderListener` (older) /
  `FloatingIconViewCompanion, DraggableView, Reorderable, Poppable`
  (current, S14 §49.8.2). It contains a `PreviewBackground` (the rounded
  folder tile), a `BubbleTextView` name label, and the preview drawing.
- **Preview shows at most 4 items:**
  `ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW = 4`
  (`FolderIcon.NUM_ITEMS_IN_PREVIEW = 4` in older code). The four are the
  first-page items, drawn clipped in a 2×2-ish arrangement by
  `PreviewItemManager` (`mFirstPageParams` / `mCurrentPageParams`,
  `PreviewItemDrawingParams`), with `ICON_OVERLAP_FACTOR = 1.125`
  (`MAX_RADIUS_DILATION = 0.25f`) — the mini icons overlap slightly and
  are clipped to a circle-ish region. `PreviewItemManager` also owns the
  create/destroy animations (`createFirstItemAnimation`,
  `prepareCreateAnimation`).
- Animation constants (S6): `DROP_IN_ANIMATION_DURATION = 400`,
  `INITIAL_ITEM_ANIMATION_DURATION = 350`,
  `FINAL_ITEM_ANIMATION_DURATION = 200`,
  `CONSUMPTION_ANIMATION_DURATION = 100`. `HAS_OUTER_RING = true`
  (the ring shown during drag-over).
- **Spring-load open:** dragging an item over a closed folder opens it
  after `ON_OPEN_DELAY = 800` ms (S14 §49.8.2) — distinct from the
  drop-to-create path; this is how you drop *into* an existing folder
  without first opening it.
- Folder icon is itself an `AdaptiveIconDrawable` in drag contexts
  (`FolderAdaptiveIcon` in `dragndrop/`) so the folder tile participates
  in the same icon-shape system as apps.

### 4.3 The open folder — `Folder`

`Folder extends AbstractFloatingView implements ClipPathView, DragSource,
DropTarget, FolderListener, …` (S6). Key facts:

- It is a **floating view on the `DragLayer`**, not a new Activity and not
  a page — it is added to the drag layer (`dragLayer.addView(this)`) and
  registers itself as a drop target (`mDragController.addDropTarget(this)`).
  Back / tap-outside / `AbstractFloatingView.close()` dismisses it.
- Content is a **`FolderPagedView`** (paged grid) using
  `FolderGridOrganizer` (columns/rows from
  `profile.getFolderProfile()`, S14 §49.8.6) — folders paginate when they
  exceed one page; the grid inside a folder is *not* the workspace grid
  (`numFolderRows/numFolderColumns` are separate IDP fields, typically
  3 rows × 4 cols on phones).
- Drag inside a folder reorders by rank (`mTargetRank`, `mPrevTargetRank`,
  `mEmptyCellRank`); dragging an item out of the folder to the workspace
  is supported, and dragging the last-but-one item out triggers the
  collapse rule (§3.3).
- The folder name is an editable `ExtendedEditText` at the top; renaming
  logs `LAUNCHER_FOLDER_LABEL_UPDATED`. Empty-name / suggested-name
  behaviour comes from `FolderNameInfos`.
- **Open/close animation** (`FolderAnimationManager`,
  `FolderSpringAnimatorSet`, S14 §49.8.8): (1) preview icons scale up
  from the `FolderIcon` position to their folder positions;
  (2) the folder background expands (circular reveal from the icon);
  (3) items fade in with a stagger; (4) a scrim darkens the workspace
  behind. Spring animations give the "bouncy" feel. `Folder` also
  implements `ClipPathView` so the reveal can be clipped to the expanding
  shape. Closing reverses it, and `onCloseComplete()` removes the view
  from the drag layer and unregisters the drop target.

---

## 5. Widgets

### 5.1 Hosting — `LauncherAppWidgetHostView` / `LauncherWidgetHolder`

- Launcher3 is a standard `AppWidgetHost` client: `LauncherWidgetHolder`
  (newer name; older: `LauncherAppWidgetHost`) owns host ids, and each
  placed widget is a `LauncherAppWidgetHostView` — a subclass of
  `AppWidgetHostView` that adds launcher-specific behaviour (long-press
  handling, resize affordance, drag support). `Launcher.onCreate` creates
  the holder and calls `startListening()` before the model loads (§1.2).
- Widget placement is grid placement: `LauncherAppWidgetInfo` carries
  `appWidgetId`, `providerName`, `spanX/spanY`, `minSpanX/minSpanY`
  (from the provider's `minWidth/minHeight` converted to cells), and the
  usual `cellX/cellY/screenId/container`. The provider's declared
  `minResizeWidth/Height` bound the resize frame's min spans (§3.4).
- **Sizing rule (framework, applied by Launcher3):** widget size in dp
  maps to cells via the standard formula — a widget reports
  `minWidth/minHeight` (and `targetCellWidth/Height` on S+); Launcher3
  converts dp → cells using cell size + border space, then pads with
  `AppWidgetHostView.getDefaultPaddingForWidget(...)`. Widgets do *not*
  get arbitrary pixel sizes; they snap to whole cells. The resize frame
  enforces this by snapping at the 0.66-cell threshold (§3.4).
- **Widget picker:** historically a `WidgetsContainerView` / bottom sheet
  listing `LauncherAppWidgetProviderInfo` grouped by package, with a
  live preview. Current AOSP has moved the picker toward Compose
  (`modules/widgetpicker`). Long-press on a widget shows the resize /
  settings / remove popup; reconfigurable widgets show an education tip
  once (`KEY_RECONFIGURABLE_WIDGET_EDUCATION_TIP_SEEN`, S7).
- **Custom widgets:** `ITEM_TYPE_CUSTOM_APPWIDGET (5)` — launcher-provided
  widgets (historically the clock and search widgets,
  `ITEM_TYPE_WIDGET_CLOCK = 1000`, `ITEM_TYPE_WIDGET_SEARCH = 1001` in
  very old code; now At a Glance / Smartspace is the Pixel-side custom
  widget). In AOSP, custom widgets are just widgets whose provider is the
  launcher itself.
- **No stacking** — see §3.4. One widget, one cell rectangle, enforced by
  `GridOccupancy`.

---

## 6. App drawer (All Apps)

### 6.1 Container — `ActivityAllAppsContainerView`

The modern drawer (S10) is
`ActivityAllAppsContainerView<T extends Context & ActivityContext>
extends SpringRelativeLayout implements DragSource, Insettable,
PersonalWorkSlidingTabStrip.OnActivePageChangedListener,
ScrimView.ScrimDrawingController`. Older code called it
`AllAppsContainerView extends BaseContainerView`. It owns:

- **`AlphabeticalAppsList`** — the sorted model. One instance per tab:
  `AdapterHolder.MAIN` (personal), `AdapterHolder.WORK` (work profile),
  `AdapterHolder.SEARCH` (a separate list instance for search results).
  `mAllAppsStore` is the shared `AllAppsStore` behind them.
- **`AllAppsGridAdapter` / `BaseAllAppsAdapter`** — a `RecyclerView`
  adapter with view types including `ICON_VIEW_TYPE`, section headers,
  prediction rows, dividers, and (newer) private-space headers.
- **`FloatingHeaderView`** — the sticky header holding the search bar,
  the prediction row (`PredictionRowView`) and the apps divider
  (`AppsDividerView`), plus the Personal/Work tab strip
  (`PersonalWorkSlidingTabStrip`).
- **`AllAppsRecyclerView`** — `RecyclerView` with a fast scroller
  (`RecyclerViewFastScroller`, `AllAppsFastScrollHelper`); the scroller
  position is `ALL_APPS_SCROLLER`.
- Search transition: `DEFAULT_SEARCH_TRANSITION_DURATION_MS = 300`.

### 6.2 Ordering — alphabetical, with section merging

- Primary order is **alphabetical by label** (locale-aware collation via
  `AlphabeticalAppsList`), grouped into letter sections (A, B, C…).
  There is **no usage/recency ordering** in the main list — recency lives
  only in the prediction row.
- **Section merging (ragged grid):** `AllAppsContainerView` historically
  used `SECTION_STRATEGY_RAGGED` with `SimpleSectionMergeAlgorithm`:
  sections whose final row is short are merged with the next section
  (minimum apps per row / minimum rows in a merged section / maximum
  number of merges — phone defaults `MIN_ROWS_IN_MERGED_SECTION_PHONE = 3`,
  `MAX_NUM_MERGES_PHONE = 2`). The `FullMergeAlgorithm` variant merges
  indiscriminately *except* it never merges the prediction row
  ("Don't merge the predicted apps"). The visual effect: no lonely
  single-icon rows under a letter header; small letters flow together.
  `SECTION_STRATEGY_GRID` is the alternative (strict per-letter blocks).
- **Fast scroll:** an alphabet scrubber on the edge; the list is a grid
  (`numAllAppsColumns` from the IDP — typically the workspace column
  count) with `all_apps_list_top_bottom_padding` around it.
- **Work profile:** a second tab ("Work") with its own `AlphabeticalAppsList`
  and a work-profile badge on icons; personal/work are separate lists,
  not a merged list with a filter.
- **Drag from drawer:** the drawer is a `DragSource` — long-press an icon
  in All Apps and drag it to the workspace; the workspace accepts it as
  an external drop (`acceptDrop` checks "if it's an external drop (e.g.
  from All Apps)", S4). The drawer stays open behind the drag; dropping
  pins the icon, and the model writes a new `favorites` row.

---

## 7. App search and predictions

### 7.1 Predictions are a system service, not launcher logic

This is the single most commonly misunderstood part of Launcher3
(S14 §49.7.7):

> "Launcher3 does not supply the suggested apps that fill the prediction
> row at the top of All Apps. It also does not supply the predicted slots
> in the Hotseat. Launcher3 is the client of a system
> `AppPredictionService`; a separate app supplies the predictions."

- `QuickstepModelDelegate.recreatePredictors()` opens one `AppPredictor`
  session **per surface**, tagged with a UI-surface string, via the
  framework `AppPredictionService` API. The All Apps session requests
  `setPredictedTargetCount(mIDP.numDatabaseAllAppsColumns)` targets.
- Every launch is reported back as an `AppTargetEvent`; the service
  pushes a fresh `List<AppTarget>`. Launcher3 renders them through
  `appprediction/PredictionRowView.java` (All Apps) and
  `hybridhotseat/HotseatPredictionController.java` (hotseat).
- Binding: `QuickstepLauncher.bindExtraContainerItems(FixedContainerItems)`
  — `CONTAINER_PREDICTION (-102)` → `PredictionRowView.setPredictedApps(...)`;
  `CONTAINER_HOTSEAT_PREDICTION (-103)` →
  `HotseatPredictionController.setPredictedItems(...)`;
  `CONTAINER_WIDGETS_PREDICTION` → recommended widgets in the popup
  (S11, `QuickstepLauncher.java`).
- **Consequence for Marlow:** Marlow cannot "implement predictions" by
  sorting by launch count alone and call it stock. The stock behaviour is
  a *client* of a system ranking service. On a device without that
  service (or without the permission/role), the prediction row is empty
  and the hotseat shows only user pins. A local frequency-based fallback
  is a reasonable Marlow feature, but it must be labelled as a fallback,
  and the row must be designed to be empty without looking broken.
  The prediction row sits in the floating header *above* the divider and
  the "All apps" label (`AppsDividerView.setShowAllAppsLabel(...)`, gated
  by an onboarding counter `ALL_APPS_VISITED_COUNT`).

### 7.2 Search

- **In-drawer search** is delegated: `AllAppsSearchUiDelegate` /
  `SearchUiManager` (`src/com/android/launcher3/allapps/SearchUiManager.java`)
  control the search input layout, the result adapter and the search
  algorithm. OEMs / Google Search plug in their own implementation; the
  AOSP default is a local app-title search (`search/` package:
  `SearchAlgorithm` interfaces). Results replace the alphabetical list
  (the `SEARCH` `AdapterHolder`); the container type for logging becomes
  `SEARCHRESULT`.
- **The QSB (Quick Search Bar)** on the workspace (`qsb/` package) is a
  *separate* entry point: tapping it typically fires an intent to Google
  Search (or the configured search provider) rather than opening the
  in-drawer search. Its item type is `ITEM_TYPE_QSB (8)` — it can be a
  row in `favorites`, which is why it survives grid migration like an
  item. On some profiles it is inline in the hotseat; on phones it is
  usually a standalone bar above the hotseat.
- **Zero-state:** the taskbar search controller keeps
  `setZeroStatePredictedItems(predictedApps)` — before the user types,
  the search UI shows predictions, not an empty list (S11,
  `TaskbarAllAppsController`).

---

## 8. Overview / recents integration (Quickstep)

**This entire section is conditional on the Quickstep build.** Without
`quickstep/`, Launcher3 is just a home screen; with it, it is also the
system recents provider. Marlow, as a third-party launcher, **cannot**
be the system recents provider (that requires being the privileged
`ROLE_HOME` / system `RecentsComponent` — on Pixel devices that is the
system Launcher3/Quickstep build). Marlow must therefore treat Overview
as *out of scope* for parity and design its own graceful behaviour
(swipe-up goes to the system recents, owned by the system launcher).
What follows is what the system launcher does, so Marlow knows what it
is *not* replicating.

- **Entry:** `QuickstepLauncher extends Launcher`
  (`quickstep/src/com/android/launcher3/uioverrides/QuickstepLauncher.java`);
  `BaseQuickstepLauncher extends Launcher` adds `DepthController`,
  `QuickstepTransitionManager`, `TaskbarController`, `RecentsViewStateController`.
- **Gesture interception:** `TouchInteractionService` (system service,
  in the launcher process when it is the recents provider) intercepts
  gesture-navigation events and routes them to `OverviewCommandHelper`
  (show/toggle recents) or `AbsSwipeUpHandler` (swipe-up / quick-switch).
  `AbsSwipeUpHandler` drives the animation via `TaskAnimationManager`
  and `RemoteAnimationProvider`, animating the foreground app's window
  into a `TaskView` (S14 §49.5).
- **Views:** `RecentsView` (the horizontal carousel of `TaskView`s) +
  `OverviewActionsView` (Screenshot / Select / Share / etc. actions under
  the carousel) + `SplitPlaceholderView` / `SplitSelectStateController`
  for split-screen. `LauncherActivityInterface` is the
  `BaseActivityInterface<LauncherState, QuickstepLauncher>` that maps
  gesture end-targets (`HOME`, `RECENTS`, `NEW_TASK`, …) to launcher
  states (`OVERVIEW`, `BACKGROUND_APP`, …) and computes the swipe-up
  destination/length from the `DeviceProfile`
  (`getSwipeUpDestinationAndLength(...)`).
- **States:** swipe-up lands in `OVERVIEW` (ordinal 2); the foreground
  app is `BACKGROUND_APP` (6); quick-switch is `QUICK_SWITCH` (4).
- **Taskbar (large screens):** `quickstep/src/com/android/launcher3/taskbar/`
  — `TaskbarActivityContext`, `TaskbarController`, `TaskbarView`,
  `LauncherTaskbarUIController`, plus `taskbar/allapps/` (the taskbar's
  own All Apps overlay, `TaskbarAllAppsController`, which opens All Apps
  in an *application overlay window*, not a nav-bar panel, so it sits
  below system UI that must stay above it). The taskbar appears on
  tablets / large screens and in desktop windowing; on phones it is
  absent and the hotseat is the only persistent row. Taskbar All Apps
  reuses the same `PredictionRowView` and search-session controller as
  the main drawer.
- **What Marlow should take from this:** the *state names* and the
  *gesture-to-state mapping* are the contract with the system. Marlow's
  own swipe-up handling must not fight the system gesture: on gesture-nav
  devices the system owns swipe-up-from-bottom, and a third-party
  launcher that tries to intercept it will lose or jank. Design Marlow's
  drawer-open gesture (swipe up on the workspace) to coexist with the
  system gesture, and never promise a custom recents UI.

---

## 9. Icon handling

### 9.1 `BubbleTextView` — the icon view

`BubbleTextView extends TextView implements ItemInfoUpdateReceiver,
DraggableView, Poppable` (S14 §49.2.4). The icon is the compound drawable
on top; the label is the text below. Display contexts:
`DISPLAY_WORKSPACE = 0`, `DISPLAY_ALL_APPS = 1`, `DISPLAY_FOLDER = 2`,
`DISPLAY_TASKBAR = 5`, `DISPLAY_SEARCH_RESULT = 6`,
`DISPLAY_SEARCH_RESULT_SMALL = 7`. It renders notification dots, download
progress rings, themed icons, and (taskbar) running-app indicators.
Labels are ellipsised, not wrapped, in the stock view; the label colour
comes from the wallpaper theme (white with shadow on the workspace).

### 9.2 Adaptive icons

- Stock icons are `AdaptiveIconDrawable`s (foreground + background layers
  + optional monochrome layer). Launcher3 never draws the raw drawable
  directly at cell size: it goes through `LauncherIcons` /
  `BaseIconFactory`, which normalises legacy icons into the adaptive
  shape (mask + scale + safe-zone) so a non-adaptive icon does not look
  smaller than its neighbours. The normalisation scale is part of
  `BitmapInfo` (`normalizationScale`).
- **Icon shape** is a user setting (Squircle / Square / Rounded Square /
  Circle / Teardrop in Pixel builds; the shape is applied by the
  `IconShape` / `AdaptiveIconDrawable` mask at draw time, not by
  re-rasterising the bitmap). Marlow must apply shape as a mask at draw
  time so toggling shape is instant and lossless.

### 9.3 `IconCache` — the cache

- `IconCache` (`icons/cache/BaseIconCache` + `icons/IconCache`) loads
  icons **asynchronously on a background thread** and caches them in a
  SQLite database (`app_icons.db` by default) keyed by
  `(componentName, user)` (S14 §49.2.7). Pipeline:
  `getTitleAndIcon(info)` → DB lookup → hit: return cached `BitmapInfo`;
  miss: `PackageManager.getActivityIcon()` → normalise + theme →
  `addOrUpdate(entry)` → return.
- `BitmapInfo` holds the rasterised `Bitmap icon` + dominant `color`
  (used for badge/popup tinting), plus a `TYPE_DEFAULT (1)` /
  `TYPE_THEMED (2)` tag so themed and unthemed variants are stored
  separately (`ThemedBitmapInfo.decode(...)` for the themed form).
  Serialisation is `toByteArray()` / `fromByteArray(...)`.
- **Low-res placeholder:** `BitmapInfo.LOW_RES_ICON` is a 1×1
  `ALPHA_8` bitmap; `LOW_RES_INFO` is shown while the real icon loads
  (`PlaceHolderIconDrawable`), so the grid never shifts layout while
  icons pop in. Marlow should do the same — reserve the cell, draw a
  placeholder, swap in the bitmap — rather than showing an empty cell
  or reflowing.
- Cache invalidation: package add/remove/update and icon-shape/theme
  changes invalidate entries; the themed variant is re-derived, not
  re-fetched from the APK.

### 9.4 Themed icons

- `Utilities.getFullDrawable(context, info, width, height, useTheme)`:
  `useTheme &= Themes.isThemedIconEnabled(context)`; if the icon is an
  `AdaptiveIconDrawable` and `shouldThemeIcon`, take `aid.getMonochrome()`
  (mutated copy), tint it with the wallpaper-derived palette, and use it
  as the icon (S12). Apps without a monochrome layer get a fallback
  treatment (full-colour icon, or a generated monochrome from the
  foreground — the exact fallback has varied by release; the invariant is
  "no monochrome layer ⇒ no forced theming that destroys the mark").
- `FLAG_THEMED` on `BitmapInfo` marks the themed variant; the theme
  toggle is a `ThemePreference` / `ThemeManager` value
  (`isMonoThemeEnabled`, `MONO_THEME_VALUE`), and toggling it invalidates
  the icon cache so every icon re-renders.
- **Marlow rule:** themed icons are opt-in per user, applied from the
  monochrome layer only, tinted from the wallpaper palette — never by
  desaturating the full-colour icon in the UI layer.

### 9.5 Badges / notification dots

- Data: `PopupDataProvider` + `NotificationListener`
  (`NotificationListenerService`) map posted notifications to
  `(package, user)` and produce a `BadgeInfo` per item (notification
  count + notification icon). `BubbleTextView.applyBadgeState(info,
  animate)` / `FastBitmapDrawable.applyIconBadge(badgeInfo,
  badgeRenderer, animate)` apply it, animating the badge scale 0→1 on
  add and 1→0 on remove (`BADGE_SCALE_PROPERTY`).
- Drawing: `BadgeRenderer` (owned by `DeviceProfile` as `mBadgeRenderer`,
  constructed from `R.dimen.badge_size` / `R.dimen.badge_text_size` —
  **24 dp / 12 dp in the older dimens**) draws in the top-right corner
  of the icon bounds: a circle (dot) or a rounded rect with the count.
  Rules (from the 2017 badge CLs, still the stock semantics):
  count > 0 → show the number; count == 0 and the notification supplied
  an icon → show that icon; otherwise → show a plain dot. Multiple
  notifications stack a second badge behind the first. The badge is
  hidden while dragging and while the long-press popup is open.
- **Folders:** `FolderDotInfo` — a folder always shows a dot if *any*
  app inside has a badge (no count on folders).
- **Setting:** notification dots are gated by the system secure setting
  `Settings.Secure.notification_badging` (the "Notification dots"
  toggle in Home Settings reflects this system setting, it is not a
  launcher-local pref) and by the launcher holding notification-listener
  access. Without notification access there are no dots at all — Marlow
  must not fake dots from launch counts.
- Badge colour: the badge background is derived from the icon's dominant
  colour (`IconPalette.fromDominantColor(...)` / `badge_color`), with
  contrasting text — not a fixed red.

---

## 10. First run / default layout

### 10.1 The priority chain

`ModelDbController.loadDefaultFavoritesIfNecessary()` (current) /
`LauncherProvider.loadDefaultFavoritesIfNecessary()` (older) — same
priority, documented in the code comment (S9):

1. **App restrictions** (`createWorkspaceLoaderFromAppRestriction`) —
   an enterprise / managed-profile XML layout.
2. **A package provided by Play Store** (`AutoInstallsLayout.get(...)`) —
   the "launcher layout" content provider
   (`content://<authority>/launcher_layout?version=1&gridWidth=…&
   gridHeight=…&hotseatSize=…`, per the GrapheneOS write-up of the same
   mechanism) — this is how a carrier/OEM can seed the layout.
3. **Partner configuration APK** already in the system image
   (`Partner.get(...)`, `RES_PARTNER_DEFAULT_LAYOUT` / `RES_DEFAULT_LAYOUT`,
   `partner.getXmlResId(...)`) — the classic `default_workspace.xml`
   override.
4. **The built-in default for the device** (`getDefaultLayoutParser()`) —
   `res/xml/default_workspace_<grid>.xml` selected by the grid-option's
   `defaultLayoutId` (e.g. `@xml/default_workspace_4x4`,
   `@xml/default_workspace_5x5`, `@xml/default_workspace_6x5`).

The loader only runs when the DB was just created empty
(`EMPTY_DATABASE_CREATED` / `getEmptyDbCreatedKey()`); if the parser
yields zero rows, the built-in default is used as a fallback. The grid
is chosen at DB-creation time from the closest display-option, and the
grid name is written into prefs on that first start — later display-size
changes do not retroactively change the grid.

### 10.2 What a default layout XML contains

A `default_workspace_*.xml` is a `<favorites>` document with
`<app>`, `<folder>`, `<widget>` / `<search>` / `<shortcut>` entries
carrying `screen`, `x`, `y`, `spanX`, `spanY`, `packageName` /
`className` / `container` attributes, parsed by `DefaultLayoutParser`
(via `AutoInstallsLayout`). The stock AOSP 5×5 default is deliberately
sparse — a handful of apps (dialer, messages, browser, camera, Play
Store in the hotseat on Pixel-flavoured builds; AOSP's own default is
closer to empty + the search bar) and the QSB, with the first workspace
page as the landing page. There is **no first-run tutorial overlay in
AOSP Launcher3** — the "fresh boot" experience is just the seeded grid
plus, on Pixel, the onboarding tips owned by the Pixel overlay. Marlow
should likewise seed sparsely and not build a multi-screen onboarding
flow and call it stock.

### 10.3 Grid migration

Changing grid (or restoring a backup onto a different grid) goes through
a migration that re-maps `(cellX, cellY, spanX, spanY)` into the new
grid, dropping items that no longer fit and writing a backup table
(`Favorites` backup tables for grid migration and for hybrid-hotseat
moves are named in `LauncherSettings`). Items are never silently
re-flowed into a different visual order if they can be kept; overflow
items are dropped, not wrapped. Marlow's grid-change behaviour should
match: migrate, drop overflow, and say so — do not silently reflow.

---

## 11. Settings / preferences

### 11.1 Two different "settings" — do not conflate them

- **`LauncherSettings`** (`src/com/android/launcher3/LauncherSettings.java`)
  is **not** the settings UI. It is the *database schema*: the `Favorites`
  and `WorkspaceScreens` tables (columns, `ITEM_TYPE_*`,
  `CONTAINER_*`, content URIs). See §1.4 / §10.
- **User-facing preferences** live in `LauncherPrefs` (typed
  `ConstantItem` prefs, Kotlin) and in `res/xml/launcher_preferences.xml`,
  shown by **`SettingsActivity`**
  (`src/com/android/launcher3/settings/SettingsActivity.java`).

### 11.2 `SettingsActivity` structure

- Entry: long-press on empty workspace → options popup → "Home settings"
  (`OptionsPopupView::startSettings`), which fires
  `ACTION_APPLICATION_PREFERENCES` scoped to the launcher's own package
  (fork evidence, S16 — the intent action is the standard Android
  mechanism; AOSP's own entry point is the same long-press path).
- `SettingsActivity extends AppCompatActivity` (older: `FragmentActivity`)
  and hosts `LauncherSettingsFragment extends PreferenceFragmentCompat`
  (inner static class). The fragment class is read from a string
  resource, not hard-coded. Theme is a `Theme.Material3.DayNight.NoActionBar`
  descendant.
- The fragment implements `SettingsCache.OnChangeListener` so system
  settings (e.g. `notification_badging`) update the UI live.
- Preference XML is declarative (`res/xml/launcher_preferences.xml`);
  custom preference types exist for launcher-specific rows
  (`NotificationDotsPreference` reflects the *system* secure setting,
  not a local pref; slider/icon-shape preferences are custom layouts).

### 11.3 What stock Launcher3 actually exposes

AOSP's own settings surface is **small** — this restraint is itself a
finding. The stable AOSP/Pixel Home Settings items are approximately:

| Setting | Source of truth | Notes |
|---|---|---|
| Notification dots (on/off) | `Settings.Secure.notification_badging` (system) | Not a launcher-local pref |
| Add app icons to Home screen (on/off) | Launcher pref | New installs get a home-screen icon |
| App icon badges / dots vs number (Pixel/Motorola) | System + launcher | OEM variation; AOSP base is dots |
| Themed icons (on/off) | `ThemePreference` / `ThemeManager` | Needs monochrome layer (§9.4) |
| Grid size (2×2 / 3×3 / 4×4 / 5×5) | `GRID_NAME` in `LauncherPrefs` → IDP grid-option | Pixel-added; AOSP base has no UI for it, only the grid-options |
| Home screen style: App tray vs Open (OEM) | Launcher pref | Motorola-style; not AOSP base |
| Icon size / label rows (OEM) | Launcher pref | Not AOSP base |
| At a Glance / Smartspace toggle (Pixel) | Pixel overlay | Not AOSP base |

Icon packs, per-app hiding, custom gestures, and double-tap-to-lock are
**not** AOSP Launcher3 features — they are Lawnchair / third-party
additions. Marlow should decide explicitly which of those it adds as
Marlow extensions and not present them as stock.

### 11.4 `LauncherPrefs` mechanics

Typed prefs (`ConstantItem<T>`) with a `LauncherPrefs` singleton;
changes are observed (IDP change → `OnIDPChangeListener` → activity
recreates its `DeviceProfile`; icon-theme change → icon-cache
invalidation; grid change → DB-file switch + migration, §10.3).
Preferences are the *only* sanctioned cross-component communication for
user choices — components do not read each other's UI state.

---

## 12. What Marlow should NOT copy literally

1. **The View-system architecture.** `CellLayout` / `PagedView` /
   `DragLayer` / `BubbleTextView` are Views. Marlow is Compose. Copy the
   *invariants* (grid occupancy, drop order, timeouts, span snapping),
   not the class hierarchy. A Compose `CellLayout` should be a layout
   + a `GridOccupancy` model, not a `ViewGroup` port.
2. **The Quickstep recents provider role.** Marlow cannot be it (§8).
   Do not build a `RecentsView` clone and expect the system to use it.
3. **The `LauncherAppState` singleton.** It is `@Deprecated` in AOSP
   itself; Marlow should use DI / explicit singletons from the start.
4. **SQLite `favorites` as the only store.** The schema is worth copying
   (§1.4); the `ContentProvider` + `LoaderTask` + `MODEL_EXECUTOR`
   machinery is an AOSP artefact. Marlow can use Room/DataStore with the
   same columns.
5. **Pixel-only features presented as AOSP.** At a Glance, the Google
   search bar, system predictions, and the Pixel grid-picker UI are not
   in the AOSP tree. Marlow must source or reimplement them deliberately,
   not assume they come free with "Launcher3".
6. **The old `REORDER_TIMEOUT = 350` / `FOLDER_CREATION_TIMEOUT = 0`
   values** without noting they changed — current is 650 ms; the folder
   decision is drop-gated in both eras.

---

## 13. CHECKABLE RULES

Each rule is testable against Marlow, and cites the source file that
justifies it. "Check" describes the concrete test.

### Grid & workspace

1. **Grid is data, not code.** Workspace columns/rows, folder rows/cols,
   hotseat count, all-apps columns and icon sizes come from a single
   grid-option record (name + `numRows/numColumns/numFolderRows/
   numFolderColumns/numHotseatIcons/numAllAppsColumns/iconSize/dbFile/
   defaultLayoutId`), and changing the record changes all of them.
   *Source:* `res/xml/device_profiles.xml` + `InvariantDeviceProfile.java`.
   *Check:* switch the grid record from 5×5 to 4×4 in a test fixture;
   workspace, folder and drawer column counts all change together.
2. **Default phone grid is 5 columns.** The stock default on a modern
   Pixel-size phone is 5×5 (Pixel grid picker default 5×5).
   *Source:* `device_profiles.xml` (`5_by_5`, "Large Phone" 406×694 dp);
   Pixel grid-picker reporting. *Check:* fresh install on a ~400 dp-wide
   phone shows 5 columns, not 4.
3. **One occupancy table per page.** Each page has a `boolean[][]`
   occupancy grid; no two items (icon, folder, widget span) overlap,
   and a widget span is an all-or-nothing rectangle.
   *Source:* `CellLayout.java` (`GridOccupancy`), `util/GridOccupancy.java`.
   *Check:* place a 2×2 widget, then attempt to drop a 1×1 icon on any of
   its four cells — all four reject.
4. **Cell size is derived, never hard-coded.** Cell width/height =
   (available width/height − padding − border space) ÷ column/row count.
   *Source:* `DeviceProfile.java` (`calculateCellWidth/Height`),
   `CellLayout.onMeasure`. *Check:* rotate to landscape; cell size
   recomputes from the new available size, icons do not clip or overlap.
5. **Horizontal margin is capped at 14% of screen width.**
   *Source:* `DeviceProfile.java` (`MAX_HORIZONTAL_PADDING_PERCENT = 0.14f`).
   *Check:* on a very wide display the workspace side margin never
   exceeds 14% per side.
6. **Hotseat is a one-row `CellLayout`, not a special view.**
   *Source:* `Hotseat.java extends CellLayout` (`mCountY = 1`).
   *Check:* hotseat items obey the same drag/drop/reorder rules as
   workspace items, and the hotseat survives page swipes unchanged.
7. **Wallpaper parallaxes with page scroll.**
   *Source:* `Workspace.java` (`WallpaperOffsetInterpolator`).
   *Check:* swipe between pages; the wallpaper translates by a fraction
   of the page offset, in the same direction.
8. **Empty trailing pages are removed; dragging to the edge creates one.**
   *Source:* `Workspace.java` (`mAddNewPageOnDrag`, snap-off 400 ms /
   fade 150 ms). *Check:* drag an icon to the right edge on the last
   page → a new blank page appears; remove the only icon from the last
   page → the page disappears.

### Drag & drop

9. **Drag starts from long-press; the drag visual is scaled up.**
   *Source:* `Workspace.beginDragShared` / `DragController.startDrag`.
   *Check:* long-press an icon → it lifts and grows; the source cell
   shows a hole/placeholder, not a duplicate.
10. **Dragging shrinks the workspace (spring-loaded mode).**
    *Source:* `DragController.startDrag` javadoc; `LauncherState.SPRING_LOADED`
    (ordinal 1). *Check:* during a drag, pages scale down and more of the
    workspace is visible; releasing/cancelling restores full size.
11. **Reorder preview is live but provisional; it commits only after a
    650 ms hover.** Neighbours slide aside under the drag view using
    temporary coordinates; the real layout is untouched until drop.
    *Source:* `Workspace.java` (`REORDER_TIMEOUT = 650` current,
    `useTmpCoords`, `performReorder(…, MODE_ACCEPT_DROP)`).
    *Check:* hover a dragged icon over an occupied cell for < 650 ms and
    move away — nothing moves. Hold ≥ 650 ms — neighbours slide aside.
12. **Drop order is fixed: folder-create → add-to-folder → place.**
    *Source:* `Workspace.acceptDrop()`. *Check:* drop icon A exactly on
    icon B → a folder is created (not a swap); drop A on a folder icon →
    A joins the folder; drop A on empty space → A lands there.
13. **Folder creation is decided on drop, not on a hover timer.**
    *Source:* `Workspace.java` (`FOLDER_CREATION_TIMEOUT = 0`).
    *Check:* hover A over B for 5 s without releasing — no folder exists
    until the finger lifts.
14. **A folder with one item left collapses back to that item; an empty
    folder is deleted.** *Source:* `Folder.java`
    (`replaceFolderWithFinalItem()`). *Check:* drag all but one icon out
    of a folder and close it — the folder is gone and the last icon sits
    on the workspace.
15. **Dragging to the top during a drag reveals Remove/Uninstall.**
    *Source:* `Workspace.java` (`UninstallDropTarget`, `DeleteDropTarget`).
    *Check:* start a drag — a remove/uninstall affordance appears at the
    top; dropping a user app on Uninstall prompts uninstall, dropping on
    Remove just removes the icon.
16. **Widget resize snaps at 66% of a cell and takes 150 ms to snap.**
    *Source:* `AppWidgetResizeFrame.java`
    (`RESIZE_THRESHOLD = 0.66f`, `SNAP_DURATION = 150`).
    *Check:* drag a resize handle 50% across a cell boundary — span does
    not change; drag past ~66% — span changes with a 150 ms snap.
17. **Resize handles obey the provider's `resizeMode`.**
    *Source:* `AppWidgetResizeFrame.java` (`mResizeMode`, handles `GONE`
    for disallowed axes). *Check:* a horizontal-only widget shows only
    left/right handles; a non-resizable widget shows no resize frame.
18. **No widget stacking.** One widget occupies one cell rectangle.
    *Source:* absence in the tree + `GridOccupancy` enforcement (§3.4).
    *Check:* attempt to stack two widgets in the same cells — the second
    is rejected (or, if Marlow adds stacking, it is documented as a
    Marlow extension, not stock).

### Folders

19. **Closed-folder preview shows at most 4 items.**
    *Source:* `folder/ClippedFolderIconLayoutRule.java`
    (`MAX_NUM_ITEMS_IN_PREVIEW = 4`); `FolderIcon.java`
    (`NUM_ITEMS_IN_PREVIEW = 4`). *Check:* a folder with 9 apps shows
    exactly 4 mini icons in its preview.
20. **Hovering a drag over a closed folder opens it after 800 ms.**
    *Source:* `folder/` (`ON_OPEN_DELAY`, via AOSP Internals §49.8.2).
    *Check:* drag an icon over a folder icon and hold — the folder opens
    at ~800 ms and the icon can be dropped inside.
21. **An open folder is a floating view over a dimmed workspace, not a
    new screen.** *Source:* `folder/Folder.java` (`AbstractFloatingView`,
    added to `DragLayer`). *Check:* open a folder — the workspace is
    still visible (dimmed) behind it; Back closes the folder, not the
    launcher.
22. **Folder open animation: preview icons scale up, background expands,
    items stagger in.** *Source:* `folder/FolderAnimationManager.java`,
    `FolderSpringAnimatorSet`. *Check:* open a folder in slow motion —
    the 4 preview icons grow into their grid positions rather than
    fading in from nowhere.
23. **Folder grid is independent of the workspace grid.**
    *Source:* `InvariantDeviceProfile` (`numFolderRows/numFolderColumns`).
    *Check:* on a 5×5 workspace the open folder shows its own grid
    (typically 4 cols × 3 rows per page) and paginates beyond it.

### Widgets

24. **Widgets snap to whole cells; size comes from the provider's
    minWidth/minHeight converted through cell size.**
    *Source:* `LauncherAppWidgetInfo`, `AppWidgetResizeFrame`
    (`getDefaultPaddingForWidget`). *Check:* add a 2×1 widget — it
    occupies exactly 2 columns × 1 row, never 1.5 columns.
25. **Widget host starts listening before the model loads.**
    *Source:* `Launcher.onCreate()` order (§1.2). *Check:* on a cold
    start, widgets render without a second "loading" flash after icons
    appear.

### Drawer (All Apps)

26. **Drawer order is alphabetical by label, grouped by letter.**
    *Source:* `allapps/AlphabeticalAppsList.java`. *Check:* install apps
    named Zebra, Apple, Mango — drawer order is Apple, Mango, Zebra
    regardless of install or launch order.
27. **Short letter sections merge (ragged grid); the prediction row never
    merges.** *Source:* `AllAppsContainerView.java`
    (`SimpleSectionMergeAlgorithm`, `FullMergeAlgorithm`).
    *Check:* a letter with a single app does not get a lonely one-icon
    row with a large header gap; its icon flows into the neighbouring
    section's rows.
28. **Drawer is a grid with the workspace column count, plus a fast
    scroller.** *Source:* `InvariantDeviceProfile.numAllAppsColumns`;
    `AllAppsRecyclerView` / `RecyclerViewFastScroller`. *Check:* the
    drawer shows the same number of columns as the workspace, and an
    alphabet scrubber appears on the edge.
29. **Work profile is a separate tab with its own list, not a filter.**
    *Source:* `ActivityAllAppsContainerView` (`AdapterHolder.WORK`).
    *Check:* with a work profile, Personal and Work tabs each show only
    their own apps; work icons carry the work badge.
30. **Long-press in the drawer starts a drag to the workspace.**
    *Source:* `ActivityAllAppsContainerView implements DragSource`;
    `Workspace.acceptDrop` (external drop). *Check:* drag an app from
    the drawer to the workspace — it pins there and stays in the drawer.

### Search & predictions

31. **The prediction row is a client of the system `AppPredictionService`,
    not a launcher-computed ranking.** *Source:*
    `quickstep/.../QuickstepModelDelegate.java`
    (`recreatePredictors()`, `AppPredictionContext`, `AppTargetEvent`);
    AOSP Internals §49.7.7. *Check:* on a device with no prediction
    service, the prediction row is empty (not filled with
    most-recently-used apps) and the layout does not break.
32. **Prediction count in All Apps equals the all-apps column count.**
    *Source:* `QuickstepModelDelegate`
    (`setPredictedTargetCount(mIDP.numDatabaseAllAppsColumns)`).
    *Check:* the prediction row shows exactly one full row of apps.
33. **Hotseat predictions are a separate surface with their own session.**
    *Source:* `QuickstepLauncher.bindExtraContainerItems`
    (`CONTAINER_HOTSEAT_PREDICTION = -103`,
    `HotseatPredictionController`). *Check:* hotseat suggested apps and
    drawer predictions can differ; clearing one does not clear the other.
34. **Search shows predictions at zero-state, results after typing.**
    *Source:* `TaskbarAllAppsController.setZeroStatePredictedItems`;
    `AllAppsSearchUiDelegate`. *Check:* open drawer search with an empty
    query — predictions are shown; type two letters — only matching apps
    (and provider results, if any) remain.
35. **The QSB is a separate search entry point, not the drawer search.**
    *Source:* `qsb/` package; `ITEM_TYPE_QSB = 8`. *Check:* tapping the
    workspace search bar opens the search provider/activity; it does not
    open the drawer in search mode.

### Overview / recents

36. **Marlow does not provide system recents.** The system launcher owns
    Overview; Marlow's swipe-up must not fight the system gesture.
    *Source:* `quickstep/` (`TouchInteractionService`,
    `AbsSwipeUpHandler`, `RecentsView`) — system-privileged role.
    *Check:* with Marlow as default on a gesture-nav device, swipe-up
    from an app still reaches the *system* recents, and Marlow does not
    show a second, competing recents UI.
37. **Taskbar exists only on large screens; phones have only the hotseat.**
    *Source:* `quickstep/.../taskbar/` (`TaskbarController`,
    `TaskbarView`); `DeviceProfile` tablet/large-screen gating.
    *Check:* on a phone, no taskbar is shown; on a tablet, the taskbar
    appears and its All Apps opens in an overlay window.

### Icons

38. **Icons are normalised into the adaptive shape; legacy icons do not
    render smaller.** *Source:* `icons/LauncherIcons.java` /
    `BaseIconFactory` (normalisation scale in `BitmapInfo`).
    *Check:* place a legacy (non-adaptive) app next to an adaptive app —
    both icons fill the same visual size.
39. **Icon shape is a mask at draw time.** *Source:* `AdaptiveIconDrawable`
    + shape setting (§9.2). *Check:* change shape Squircle → Circle —
    icons change instantly with no reload or quality loss.
40. **Icons load async with a 1×1 placeholder; the grid never reflows.**
    *Source:* `icons/BitmapInfo.java` (`LOW_RES_ICON`,
    `PlaceHolderIconDrawable`); `IconCache` pipeline (§9.3). *Check:*
    cold start — cells are reserved immediately; icons pop in without
    moving neighbours.
41. **Icon cache is keyed by (component, user) in a DB, and theme/shape
    changes invalidate it.** *Source:* `IconCache` (§9.3),
    `BitmapInfo.TYPE_DEFAULT/TYPE_THEMED`. *Check:* toggle themed icons —
    every icon re-renders once, then the new rendering is cached (second
    launch is instant).
42. **Themed icons use the monochrome layer only, tinted from the
    wallpaper palette.** *Source:* `Utilities.getFullDrawable`
    (`getMonochrome()`, `Themes.isThemedIconEnabled()`).
    *Check:* enable themed icons — apps *with* a monochrome layer become
    monochrome; apps *without* one keep a recognisable (fallback) mark
    and are not just desaturated.
43. **Notification dots: count > 0 → number; count == 0 + icon → icon;
    else a dot; folders show a dot if any child is badged.**
    *Source:* `badge/BadgeRenderer.java`, `dot/FolderDotInfo.java`
    (§9.5). *Check:* an app with 3 notifications shows "3"; an app with
    a 0-count notification shows a dot; its folder shows a dot.
44. **Dots require notification-listener access and follow the system
    `notification_badging` setting.** *Source:* `PopupDataProvider` /
    `NotificationListener`; `Settings.Secure.notification_badging`.
    *Check:* revoke notification access — all dots disappear; toggle the
    system dots setting — Marlow's dots follow it without a Marlow-side
    restart.
45. **Badge size is proportional to icon size (24 dp badge / 12 dp text
    at the reference icon size).** *Source:* `res/values/dimens.xml`
    (`badge_size = 24dp`, `badge_text_size = 12dp`, older dimens).
    *Check:* at a larger grid icon size the badge scales with the icon
    rather than staying a fixed pixel size.

### First run & settings

46. **First run seeds from the default-layout chain, in priority order:
    app restrictions → Play-store layout provider → partner APK →
    built-in `default_workspace_<grid>.xml`.** *Source:*
    `model/ModelDbController.loadDefaultFavoritesIfNecessary()`.
    *Check:* fresh install with an empty DB produces exactly the
    built-in default layout for the detected grid — no extra pages, no
    tutorial overlay.
47. **Grid choice is made once, at DB creation, from the closest
    display-option — and is then persisted.** *Source:*
    `InvariantDeviceProfile` + `LauncherPrefs.GRID_NAME`.
    *Check:* change the display size / density after first run — the
    grid does not silently change.
48. **Changing grid migrates and drops overflow; it never silently
    reflows.** *Source:* `LauncherSettings` backup tables; grid-migration
    design (§10.3). *Check:* switch 5×5 → 4×4 — items that no longer fit
    are dropped (and the user is told), surviving items keep their
    relative positions.
49. **Home Settings is reached by long-press on empty workspace →
    "Home settings", and is a `PreferenceFragmentCompat` over a
    declarative XML.** *Source:* `settings/SettingsActivity.java`,
    `res/xml/launcher_preferences.xml`. *Check:* long-press empty space —
    the popup offers Home settings; the settings screen is a standard
    Material 3 preferences list, not a custom full-screen UI.
50. **Stock settings are few: notification dots, add-to-home on install,
    themed icons, grid size.** Icon packs, hidden apps and gestures are
    *not* stock. *Source:* §11.3. *Check:* Marlow's settings screen
    labels every non-stock item as a Marlow addition (or groups it
    separately) so "stock-feeling" is not diluted by unexplained extras.

---

## 14. The 10 findings most likely to matter for a stock-feeling launcher

1. **The grid is a data record, not a layout file (§2.2, Rules 1–2).**
   One grid-option — columns, rows, folder grid, hotseat count, drawer
   columns, icon size, DB file, default layout — drives everything.
   Getting this single source of truth right is what makes grid switching,
   rotation and tablets behave instead of each being a special case.
2. **Predictions are a system service Marlow does not have (§7.1, Rule 31).**
   The prediction row and hotseat suggestions are rendered by Launcher3
   but *ranked* by `AppPredictionService`. Marlow needs an honest
   empty-state and a clearly-labelled local fallback, not a fake
   "most used" row presented as stock.
3. **Drop order is folder-create → add-to-folder → place (§3.2, Rule 12),
   and folder creation is drop-gated, not hover-timed (Rule 13).**
   This ordering is the difference between "it just makes a folder" and
   the fiddly drag behaviour users complain about in third-party launchers.
4. **Reorder needs a 650 ms hover (Rule 11).** The deliberate pause before
   neighbours slide aside is a large part of why stock dragging feels
   calm. A launcher that reorders instantly feels nervous.
5. **Widget resize snaps at 66% of a cell (Rule 16) and widgets snap to
   whole cells (Rule 24).** Fractional widget sizes and hair-trigger
   resizing are the fastest way to feel non-stock.
6. **A folder preview is exactly 4 mini icons, and an open folder is a
   floating view over a dimmed workspace (Rules 19, 21).** The 4-item
   preview and the floating (not full-screen) folder are instantly
   recognisable Pixel/AOSP signatures.
7. **Icons are normalised, cached by (component, user), and load behind a
   placeholder (Rules 38, 40–41).** Equal visual size for legacy and
   adaptive icons, no layout shift on cold start, and instant second
   launch are all consequences of the `IconCache`/`BitmapInfo` pipeline —
   not of the icon draw code alone.
8. **Themed icons come from the monochrome layer only (Rule 42).**
   Desaturating full-colour icons in the UI layer looks wrong in a way
   users notice immediately; the stock path is monochrome-layer +
   wallpaper tint, with a real fallback for apps that lack one.
9. **Badges are dots/numbers from the notification listener, gated by the
   system setting (Rules 43–44).** No notification access ⇒ no dots,
   ever. Faking badges from launch counts would be visible the first
   time a user clears a notification and the dot stays.
10. **Marlow cannot be the recents provider (Rule 36).** The whole
    Quickstep stack — `TouchInteractionService`, `AbsSwipeUpHandler`,
    `RecentsView`, taskbar — is a system-privileged role. Planning Marlow
    as if it could own swipe-up recents would waste the most expensive
    part of the project; the stock-feeling move is to coexist cleanly
    with the system gesture and invest that effort in the workspace,
    drawer and drag feel instead.

---

## 15. Open questions / things to verify against a Pixel device

- Exact current `REORDER_TIMEOUT` on the shipping Pixel build (650 ms in
  current AOSP main; 350 ms in older trees — confirm which the Pixel
  build carries).
- Exact `badge_size` / `badge_text_size` on current Pixel dimens (24 dp /
  12 dp is the older value; the ratio, not the absolute, is the invariant).
- Whether the shipping Pixel drawer still uses the ragged
  section-merge algorithm or has moved to strict grid sections.
- The precise fallback rendering for themed icons of apps without a
  monochrome layer on the current release.
- The current `default_workspace_5x5.xml` contents on Pixel (hotseat
  order and first-page apps) — AOSP's default and Pixel's shipped default
  differ, and Marlow should seed from the Pixel one if it claims
  Pixel-style.

## 16. Sources (full list)

- AOSP Launcher3 tree, `platform/packages/apps/Launcher3` —
  `https://android.googlesource.com/platform/packages/apps/Launcher3`
  (files cited inline: `DeviceProfile.java`, `InvariantDeviceProfile.java`,
  `CellLayout.java`, `Workspace.java`, `dragndrop/DragController.java`,
  `folder/Folder.java`, `folder/FolderIcon.java`,
  `folder/PreviewItemManager.java`,
  `folder/ClippedFolderIconLayoutRule.java`,
  `AppWidgetResizeFrame.java`, `LauncherSettings.java`,
  `LauncherProvider.java`, `model/ModelDbController.java`,
  `allapps/ActivityAllAppsContainerView.java`,
  `quickstep/.../QuickstepLauncher.java`,
  `quickstep/.../BaseQuickstepLauncher.java`,
  `quickstep/.../LauncherActivityInterface.java`,
  `quickstep/.../AbsSwipeUpHandler.java`,
  `Utilities.java`, `icons/BitmapInfo.java`, `BubbleTextView.java`,
  `settings/SettingsActivity.java`, `res/xml/device_profiles.xml`,
  `res/xml/default_workspace_*.xml`).
- *AOSP Internals*, ch. 49 "Launcher3 — The Android Home Screen" —
  `https://aospbooks.github.io/aosp-internal-book/49-launcher3/`
  (secondary survey quoting the files above; used for the architecture
  overview, state ordinals, and §49.7.7 on predictions).
- Tablet grid diff `1c883d861e` —
  `https://android.googlesource.com/platform/packages/apps/Launcher3/+/1c883d861e%5E%21/`
  (addition of the `6_by_5` tablet grid-option).
- GrapheneOS home-screen restore write-up (layout-provider mechanism) —
  `https://aioue.net/2026/10/01/grapheneos-home-screen-layout/`
  (corroborates the `launcher_layout` content-provider step in the
  default-layout priority chain).
- Pixel Launcher grid-size reporting (9to5Google, 2020) —
  `https://9to5google.com/2020/10/15/pixel-launcher-grid-size/`
  (2×2 / 3×3 / 4×4 / 5×5 picker, 5×5 default).

*No credentials, employment history, or education claims appear in this
document. The research stands on the cited sources above.*
