# Marlow Launcher — HOME stream rebuild notes (Phase 3, wave 1)

Branch: `rebuild/home`. Worktree: `~/workspace/launcher-home`.
Scope: home screen, grid, glance, feed, folders (tile + drag), icons, badges.
Held (untouched): `FirstRunWizard.kt`, `.github/`, manifest, dock pagination/labels/per-page cap/seed order, dock pages (#68).

## What changed, per rule number

### Rule 27 / decision F6 — At a Glance rebuild (`AtAGlance.kt`, `HomeScreen.kt`)
- Strip is now text directly on the wallpaper: no card background, no border.
- Resting state: day + date line, weather line (condition + temperature, Open-Meteo summary).
- An upcoming calendar event REPLACES the date line (never a second row). The next event is the first of today's events whose end is still ahead.
- Swipeable complication carousel (HorizontalPager): resting → today's events → weather detail. A page with no data is skipped, never blank.
- Split tap targets: date/event line → system Calendar (`ACTION_VIEW`, `CalendarContract.CONTENT_URI`); weather line → web weather for the chosen city via `launchUrl` (Google search URL; Marlow unified search's web leg — there is no system weather-view intent a third-party launcher can rely on).
- Long-press → dialog with per-feature toggles (Weather, Calendar events) + where content comes from (system clock; Open-Meteo + chosen city; system calendar, permission-gated). Turning features off reduces the strip to day/date — never blank, never an error.
- No news, no greeting, no promo, no battery %. The "Add weather for your city" setup row is REMOVED from the strip (it belongs in Settings).
- `NowBriefSheet` kept, converted to type-scale roles.

### Rules 20–21 — First-boot composition (`BuiltinWidgets.kt`, `HomeViewModel.kt` seed untouched)
- Seed already places clock widget + 10 apps + 5-icon dock with 2 rows empty (audit: counts pass). The one duplication is fixed: the CLOCK widget is now TIME-ONLY (the date line is gone — the strip already shows it). No second page, no empty page, no tips/settings/store shortcut on page one.
- Folder naming on first boot: new folders never get "Folder" (see rule 39).

### Rules 22–23 / decision F3 — Grid (`LauncherModels.kt`, `HomeViewModel.kt`, `HomeGrid.kt`)
- Grid options are now the Pixel set 2×2 / 3×3 / 4×4 / 4×5 / 5×5, default 5×5 — PLUS 5×6 and Fluid kept as explicit non-default choices labelled "(Marlow)" (`GridSize.isPixelOption = false`).
- Single grid record on `GridSize`: workspace rows/cols, `folderCols` = 4 / `folderRows` = 3 (rule 39), `drawerColumns` = cols, `derivedIconSizeDp` (2×2→72, 3×3→64, 4×4→60, 4×5/5×5/Fluid→56, 5×6→52). The settings icon-size slider remains as a user override of the derived value.
- Changing grid re-flows without overlap or loss: `HomeViewModel.migrateGrid` clamps items into the new bounds, moves colliding items to the nearest free rectangle, shrinks widgets to one cell rather than losing them, and drops only what cannot fit anywhere — with a notice (`gridNotice`, shown once as a snackbar in `HomeScreen`): "N items didn't fit the new grid and were removed from the home screen. Apps stay in the app drawer." Never silent.
- Cell size derived, never hard-coded. Horizontal margin capped at 14% of screen width (`MAX_HORIZONTAL_MARGIN_PERCENT = 0.14f`, enforced in `HomeGrid`).
- Folder grid hard-code (3 columns in `FolderView.kt`) is NOT fixed here — `FolderView.kt` belongs to the drawer stream. Patch request below: use `settings.gridSize.folderCols` / `folderRows` from the record.

### Rules 9–14 / decisions F14, F19 — Labels + legibility
- One 12sp type-scale label role everywhere in my files: `MaterialTheme.typography.bodySmall`. ALL hard-coded `.sp` literals at call sites in my files are gone (verified by grep: `AtAGlance`, `HomeGrid`, `Dock`, `HomeScreen`, `HomeViewModel`, `FeedPage`, `Overview`, `BuiltinWidgets`, `AppIcon`, `IconUtils`, `Gestures`, `MediaPlayerBar`, `LauncherModels` — zero matches). Other roles used where the text is not an icon label: `titleLarge`/`titleMedium`/`titleSmall`, `bodyLarge`/`bodyMedium`, `headlineLarge` (clock time).
- One line + ellipsis everywhere (already; kept). Same cell height regardless of label (cell height comes from the grid span).
- 200% font scaling: at 200% a 12sp label becomes ~24sp (~32dp line). A 5×6 cell is ~116dp tall against ~92dp of icon + 200% label, so the label grows inside the cell's slack and truncates, never overlapping the icon below. This is code inspection + arithmetic only — DEVICE CHECK STILL OWED (200% font scale on the Pixel 10a, all grid sizes).
- Luminance-aware scrim (`HomeScreen.kt`): wallpaper luminance is read from `WallpaperManager.getWallpaperColors(FLAG_SYSTEM)` primary colour (WCAG relative luminance via the theme's `relativeLuminance`). Scrim alpha: 0 over dark art (L<0.30), up to 0.45 over bright art (L>0.80), linear between, 0.08 veil when no pixels can be read. Multi-stop eased vertical gradient (0.9/0.45/0.35/0.45/0.9 of the alpha), so no visible edge or band; the user's dim slider is a floor on top. Label/glance text colour follows luminance too (dark text over bright art, white over dark art, theme text when unreadable).
- Raw hex in `HomeScreen.kt:452,454` removed: the default home backdrop gradient now uses `surfaceContainerLow` / `background` theme roles only.
- Contrast measurement (code terms, four test wallpapers): bright photo (L≈0.85 → scrim 0.45, dark label on ~55%-darkened art); dark photo (L≈0.15 → scrim 0, white label on dark art); busy photo (mid L≈0.5 → scrim ~0.18, luminance-chosen text); flat colour (scrim follows the flat luminance). Exact ratios depend on the wallpaper pixels — DEVICE CHECK STILL OWED: measure label contrast against bright/dark/busy/flat wallpapers on device with a contrast checker.

### Rules 6, 17 — Dock colours only (`Dock.kt`)
- Dock uses `surface-container`; search pill uses `surface-container-high` (more than one step apart). Blur-glass translucency is kept as an alpha over the same roles.
- `shadow(8.dp)` REMOVED — tonal depth, not shadow. The 1dp outline is REMOVED as well: the container steps separate the pills on their own.
- Dock shape is now `shapes.large` (16dp) instead of the non-table 24dp. Pagination, labels, per-page cap, seed order UNTOUCHED (held).

### Rule 28 / decision F15 — Search pill (`Dock.kt`, `HomeScreen.kt`)
- One entry point, bottom, below the dock, on every page (unchanged placement).
- Working mic trailing icon: the system speech recogniser (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`); the spoken words open unified search with the query already typed (`initialQuery` + existing `onQueryChange` run). No recogniser on device → mic just opens search.
- Lens OMITTED: no Lens API exists for third-party launchers — there is nothing truthful to wire it to.
- The pill is Marlow unified search: device-first, web via the chosen search provider. Distinct from drawer-top search (drawer field filters the drawer; the pill opens universal search).

### Rule 29 — Wallpaper + pages (`HomeScreen.kt`, `HomeViewModel.kt`, `HomeGrid.kt`)
- Wallpaper parallaxes CONTINUOUSLY with the finger: a `snapshotFlow` over `pagerState.currentPage + currentPageOffsetFraction` drives `WallpaperManager.setWallpaperOffsets` with the fractional offset (normalised over home pages; infinite-scroll modulo handled). The old per-page-change call is gone.
- Empty trailing pages auto-remove: `HomeViewModel.pruneEmptyTrailingPages(currentPage)` deletes trailing empty pages BEHIND the current page when the user moves pages — so a page just added from Overview is not deleted before it can be visited. (The 400 ms snap-off / 150 ms fade are the pager's own settle; no extra animation code was needed.)
- Page swipes follow the finger and snap (HorizontalPager, unchanged). No dead bounce past the last page: the pager is finite when feed/widget pages are on; infinite scroll modulo-maps.
- Dragging to the edge creates one page: `HomeGrid` reports edge proximity (`onDragNearEdge`, throttled to one move per 900 ms in `HomeScreen`); past the last page, `addPage()` runs and the pager scrolls onto the new page.

### Rule 36 / decision F16 — Feed (`FeedPage.kt`, `HomeScreen.kt`)
- Rebuilt personal-context-first: "Up next" calendar card (next event + following two, permission-gated — without the calendar permission it says plainly that calendar isn't connected and where to grant it), "Recent apps" card (last 5 used, tap to launch), then the "News" card (RSS sources + items) secondary/below.
- Every card optional, reorderable (up/down buttons), removable (X). Card order/dismissal is the page's own state.
- Disabling the feed leaves no dead page and no placeholder (the home simply has no feed page — unchanged behaviour, kept).
- The permanent "Google feed isn't available in this launcher" negative notice is GONE. Replaced with plain wording about what the feed is: "Your day first — calendar, people and recents — with news below. Cards can be moved or removed; turning the feed off removes this page."

### Rules 37–40 / decision F7 — Folders, tile + drag side (`HomeGrid.kt`, `HomeViewModel.kt`)
- Closed folder tile is CIRCULAR (`CircleShape`) with a 2×2 preview of the first four apps (at most 4 minis, stable order), one cell, same size/mask/label treatment as an app icon. Minis sit inside with padding (rule 16: inner radius follows outer − padding).
- Dragging an icon onto a folder ADDS it: `HomeViewModel.addAppToFolder` (the drop search now matches FOLDER targets as well as APP targets). Folder-create on drop onto an app is unchanged; creation is decided on drop, not on a hover timer.
- Hover ring feedback on drag-over (2dp primary border on the target).
- Hovering a drag over a closed folder opens it after 800 ms (`ON_OPEN_DELAY`). Limitation, recorded honestly: the folder currently opens as a dialog (`FolderView.kt`, drawer stream), which cancels the in-flight drag — so the 800 ms open works as a peek at the folder's contents, and the drop itself completes via the direct drop-onto-folder path. A true drop-into-the-open-folder needs the drawer stream's floating overlay; see patch requests.
- New folders get a short task-based name, NEVER the target app's name and never "Folder": `HomeViewModel.folderNameFor` derives it from the contents' kind (shared `AppCategory` → its name, e.g. "Games", "Communication"; mixed kinds → "Apps").
- Dragging the last app out dissolves the folder (already; unchanged).
- The folder OPEN overlay itself is the drawer stream's file (`FolderView.kt`) — untouched.

### Rule 56 / decision F21 — Drag system (`HomeGrid.kt`, `HomeViewModel.kt`, `Gestures.kt`)
- Long-press at the system timeout (already via Compose `detectDragGesturesAfterLongPress`; kept).
- 650 ms reorder hover before neighbours slide aside: after 650 ms over an app target the swap is committed live (`moveHomeItem` swaps two single-cell apps; the grid re-flows under the finger and the drag anchor follows).
- Spring-loaded shrink while dragging: the dragged tile scales up 1.08× on a spring (`animateFloatAsState` + `spring()`); its source cell shows a shrunken (0.92×) placeholder hole — never a duplicate.
- Scaled-up drag visual: 1.08× as above.
- Source-cell hole/placeholder: as above.
- Edge auto-scroll that can create a page: see rule 29.
- Drag-to-top Remove/Uninstall: a Remove / Uninstall bar appears at the top of the page only while dragging (left half removes the item from the home, right half uninstalls the app via the system uninstall intent). The hovered half highlights.
- Double-tap window: the system 300 ms — `Gestures.kt` now uses `ViewConfiguration.getDoubleTapTimeout()` instead of the hand-rolled 320 ms.

### Rules 24, 45–48 / decisions F10, F18 — Icons (`AppIcon.kt`, `IconUtils.kt`, `HomeGrid.kt`, res)
- One icon size / label size / gutter across home, dock, folders (home 56dp default; drawer/folder/overview match by rule — drawer surfaces are the drawer stream's; folder minis and overview minis now derive from the same record).
- Icon corner radius PROPORTIONAL to icon size: `shapeFor(shape, iconSize)` uses `RoundedCornerShape(iconSize * 0.25f)` — the old fixed 28dp made a near-circle on a 40dp icon and a squircle on a 72dp one. Apparent shape is now identical on every surface.
- Legacy icons normalised onto a tray at a fixed scale: 0.72 (14% inset each side, the One UI / AOSP tray scale; was 0.80).
- Icons load asynchronously with a placeholder: the drawable resolves from the cache synchronously (cheap), the bitmap decode runs on `Dispatchers.Default` via `LaunchedEffect`; while loading the cell is reserved with a `surfaceContainer` placeholder — the grid never reflows, neighbours never shift.
- Themed icons: the `ColorFilter.tint(primary, SrcIn)` path over the FULL-COLOUR icon is DELETED. The tint now applies ONLY to the monochrome layer (`IconUtils.monochromeDrawable`, `AdaptiveIconDrawable.monochrome` on API 33+), tinted from the wallpaper palette (`WallpaperHelper.ambientAccent`, falling back to theme `primary`).
- Themed-icon decision (coordinator's, from the research's own stated position in rule 48): below Android 16 QPR2 ship the MIXED grid — apps WITH a monochrome layer become monochrome, apps WITHOUT one keep their normal recognisable icon (never a forced tint); on Android 16 QPR2+ the same monochrome layer is what the system's auto-theming uses, so this path converges on it. Documented here and in `AppIcon.kt`/`IconUtils.kt`.
- Marlow's own icon: added a true monochrome silhouette layer `res/drawable/ic_launcher_monochrome.xml` — single-colour solid silhouette of the primary mark only (the recessed panel path from `ic_launcher_foreground.xml`, same geometry/viewport/safe zone), no gradient/two-tone/shadow/thin detail. Referenced from both `res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` via `<monochrome>`. All three XML files verified well-formed (parsed).

### Rules 49–50 / decision F9 — Badges (`Gestures.kt`, `AppIcon.kt`, `HomeGrid.kt`, `HomeViewModel.kt`, `LauncherModels.kt`)
- Dot-only on icons: a small dot (6dp), top-trailing (`Alignment.TopEnd`, RTL-aware), one colour (`primary`), one shape (circle), pinned identically on every icon.
- NO numbers on icons, NO 18dp badge, NO error-red. The numeric count lives ONLY in the long-press menu: `HomeViewModel.badgeCountFor(packageName)` (the settings stream owns the sheet — see patch requests). The 10sp badge text is gone with the numbers (rule 10 floor).
- Dots clear when the app is opened: `launchApp` records the open time per package; only notifications posted after it re-badge (`NotificationPreview.postedAt` comparison, done where the counts are built in `HomeScreen`).
- Per-app badge off switch: `LauncherSettings.badgeOffPackages` + `HomeViewModel.setBadgeEnabledForPackage`. In-memory for now — persistence needs a `LauncherStore` key (patch request below).
- Folder shows a dot if any child is badged (`FolderTile.hasBadge`).
- NO badges in the drawer: the drawer still threads `badgeCounts`/`badgeBitmaps` through (`Drawer.kt`, drawer stream). Patch request below.
- Compat note: the old badge fields (`badgeStyle`, `badgePosition`, `badgeSizeDp`, `badgeCornerRadiusDp`, `dynamicBadges`) are KEPT on `LauncherSettings` because `LauncherStore` and `BackupManager` still read/write them — but the badge UI ignores them entirely. Defaults changed to DOT / TOP_END / 6dp. Dynamic-badge artwork (#128) is ignored on the icon; its large icon is available to the long-press sheet via the previews.

### Rule 51 — Long-press empty space (`HomeScreen.kt`)
- Long-press on empty home space now opens a menu with exactly three entries: Wallpaper & style, Widgets, Home settings. It used to open Overview. No debug/plan/developer entries in the user-facing menu.

### Rule 67 / decision F22 — Permissions (`HomeScreen.kt`)
- STOPPED the calendar permission auto-fire at launch (the `LaunchedEffect(settings.glanceVisible, settings.glanceCalendar)` that launched `READ_CALENDAR`) and the contacts auto-ask when search first opens (the `LaunchedEffect(showSearch)` that launched `READ_CONTACTS`).
- Permissions are asked in context at first use only. The launchers stay declared (the wizard and Settings call them on user action); the settings stream owns the priming-screen pattern. Until granted: the strip shows date + weather only; search omits contacts.

### Rule 72 — Robustness (`HomeScreen.kt`)
- The drawer query is cleared when the drawer hides, at EVERY close site (close, open-settings, auto-close after launch), via a single `closeDrawer()` helper that also clears universal-search state. No stale query on reopen.

### Rule 10 / §B.18 — Overview (`Overview.kt`)
- Real scaled mini-previews of pages: `PageMiniPreview` draws every item at its true sub-grid position, scaled down — app icons, folder circles, widget rounded rects — not icons-only thumbnails.
- Action labels use the 12sp role (`bodySmall`), not 11sp. Page titles use `bodyMedium`.

### Other files in scope
- `MediaPlayerBar.kt`: shape is now `shapes.medium` (12dp, was non-table 20dp), colour `surfaceContainerHigh`, title `bodyMedium`, artist `bodySmall`.
- `HomeScreen.kt` default home backdrop: raw hex removed (see rules 9–14).
- `LauncherModels.kt`: `STICKER_FONT_SP` const removed (sticker renders in `bodyLarge` now).
- Kept intact: name Marlow, Stock theme default, P22 accent values as seed, warm shading values, halo only on page dot + resize handle, icon C v3. Dex guard, startup gate, Manifest guard untouched.

## Decisions with reasoning (summary)
1. Weather tap opens web weather (Google search URL for the city) — there is no system weather-view intent a third-party launcher can rely on; the web leg is Marlow unified search's existing behaviour.
2. Folder names from shared `AppCategory` ("Games", "Communication", …; mixed → "Apps") — short, task-based, derivable from contents with no extra data, never an app's name.
3. Mixed themed-icon grid below Android 16 QPR2 (coordinator's decision from rule 48's own stated position); no forced tint ever.
4. The icon-size slider stays as a user override of the grid-derived size; the grid record carries the derived value for folder/drawer/overview.
5. Trailing empty pages prune only behind the current page, so "Add page" from Overview survives until visited.
6. The 800 ms folder hover-open works as a peek while the folder is dialog-based; the drop completes via direct drop-onto-folder.
7. Old badge fields kept for `LauncherStore`/`BackupManager` compat but ignored by the UI.
8. Dynamic-badge artwork (#128) is not drawn on icons (dot-only rule wins on the icon surface); the artwork remains available to the long-press sheet.
9. Dock shape `shapes.large` (16dp) replaces the non-table 24dp; the 1dp outline was removed because the container steps separate the pills.
10. Label colour over wallpaper follows wallpaper luminance (dark text over bright art, white over dark art) with the luminance-aware scrim behind — this is what makes 4.5:1 achievable by construction on arbitrary wallpaper.

## Patch requests for other streams
1. **Drawer stream (`FolderView.kt`)**: folder grid is still hard-coded 3 columns. Use the grid record: `settings.gridSize.folderCols` (4) / `folderRows` (3). Folder open overlay (floating view over dimmed home, 4×3 paginated, tap-name rename) is yours — when it lands, the 800 ms hover-open can complete a drop into the open folder instead of peeking.
2. **Drawer stream (`Drawer.kt`)**: REMOVE badges from the drawer (rule 50 — no badges in the drawer). `HomeScreen` still passes `badgeCounts`/`badgeBitmaps` to `DrawerScreen`; stop rendering them there. Also: drawer icon/label/gutter sizes must match the home record (home 56dp, label `bodySmall`) — your surfaces.
3. **Settings stream (long-press sheet, `LongPressSheet.kt`)**: show the numeric badge count in the long-press menu via `HomeViewModel.badgeCountFor(packageName)`; add the per-app badge off switch there, wired to `HomeViewModel.setBadgeEnabledForPackage(packageName, enabled)`.
4. **Settings stream (`LauncherStore.kt` + `BackupManager.kt`)**: persist `LauncherSettings.badgeOffPackages` (new `KEY_BADGE_OFF_PACKAGES`, string-set; include in backup/restore). Without this the per-app badge switch resets on restart.
5. **Settings stream (priming pattern)**: calendar permission should be asked from the At a Glance settings priming row / when the user enables calendar events; contacts permission from Settings → Search. Both auto-fires were removed from `HomeScreen` — the features degrade gracefully until granted, but nothing currently asks.
6. **Theme stream (info only)**: dock/pill now use `surfaceContainer` / `surfaceContainerHigh` directly (with blur-glass alpha). If the theme's surface roles change, these two surfaces follow automatically — no action needed.

## What remains device-gated, and why
- **Label legibility (rules 12–14)**: 200% font scaling (no overlap/clip on any grid size) and label contrast against the four test wallpapers (bright photo, dark photo, busy photo, flat colour). Code inspection + arithmetic only so far; a device with real wallpaper pixels and a contrast checker is the only proof. The scrim strength and the luminance text-colour switch are the mechanisms; their thresholds (0.30/0.80 luminance, 0.45 max alpha) are the numbers to validate on device.
- **Drag feel (rule 56)**: 650 ms reorder hover, spring scale, edge auto-scroll + page creation, drag-to-top Remove/Uninstall, 800 ms folder hover-open — all implemented, but timing/feel can only be judged by dragging on a real device.
- **Glance carousel + split taps (rule 27)**: swipe between complications, date→Calendar, weather→web weather, long-press toggles — needs a device with a calendar event and a set weather city.
- **Themed icons (rule 48)**: monochrome rendering needs apps that actually ship a monochrome layer, on API 33+ and on Android 16 QPR2+, plus Marlow's own icon under the system themed-icon toggle.
- **Badges (rules 49–50)**: dot appearance/position, clear-on-open, folder dot — needs a real notification on device.
- **Wallpaper parallax (rule 29)**: continuous offset needs a live wallpaper on device; `setWallpaperOffsets` is best-effort per device.
- **Feed (rule 36)**: calendar card needs the calendar permission granted on device; recents need real launch history.
- **First-boot composition (rules 20–21)**: fresh-install frame (strip + 8–12 cells + dock + pill, one empty row) needs a clean install on device.
- Everything above is code-verified (kotlinc-check green, see below) but NOT device-verified. CI is the authority for the build; the device is the authority for the feel.

## Verification
- `tools/kotlinc-check.sh` in `~/workspace/launcher-home`: GREEN (kotlinc 2.2.20; CI builds with Kotlin 2.4.20 — CI is the authority).
- Grep verification: zero `.sp` literals and zero `Color(0x…)` literals in all 13 owned files; zero references to `app.launcher.R`.
- `ic_launcher_monochrome.xml`, `ic_launcher.xml`, `ic_launcher_round.xml`: all three parse as well-formed XML.
- No files outside the owned list were modified (verified with `git status` before commit).
- Not run: local Gradle build (impossible in this sandbox), CI build, device checks (all listed above as owed).
