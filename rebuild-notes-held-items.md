# Held items — wizard deleted, dock fixed (2026-11-26)

Branch `rebuild/held-items`, from `rebuild/merge` (fa4930f). Audit section F
items 2 (first run) and 5 (dock), per Robert's ruling: follow the audit's
recommendations on both. Stock theme default, P22 accent values, warm
shading, halo only on page dot + resize handle, icon C v3 — all untouched.
Dex guard, Robolectric startup gate, Manifest guard — untouched.
Version stays 0.1.0-dev code 1.

kotlinc-check: PASS, 0 errors — verified two ways: (a) the combined
shared tree (this work plus the parallel motion/haptics builder's files)
passes (48 files), and (b) an isolated tree of fa4930f + only this
commit's hunks — exactly this branch's content — passes (46 files).
The parallel builder's motion/haptics work lives on branch
`rebuild/motion` (commit 5d4ed89) and shares the working tree
(untracked `Haptics.kt`/`Motion.kt`, modified `AndroidManifest.xml`,
`Drawer.kt`, `FolderView.kt`, plus hunks in `HomeGrid.kt`,
`HomeScreen.kt`, `LauncherStore.kt`, `LauncherModels.kt`,
`SettingsScreen.kt` and `SettingsControls.kt`); this commit stages ONLY
the held-items hunks, so none of their work is in it.
`HomeViewModel.kt` is entirely this commit's — no foreign hunks there.
Where one hunk mixed both builders' lines (HomeScreen BackHandler/
pickContact region), it was split by hand and only the held-items lines
were staged.

## 1. Wizard — home first (F2)

Deleted:
- `ui/FirstRunWizard.kt` — the whole five-step wizard (welcome, theme,
  grid, search, grants) and its `ChipRow` usage.
- `ui/SettingsControls.kt` — `ChipRow` (existed only for the wizard) and
  its now-unused imports (`ExperimentalLayoutApi`, `FlowRow`, `FilterChip`).
  `ExperimentalMaterial3Api` stays (used elsewhere in the file).
- `ui/HomeViewModel.kt` — `completeWizard()`.
- `model/LauncherModels.kt` — `LauncherSettings.wizardCompleted`.

Changed:
- `ui/HomeScreen.kt` — the `if (!settings.wizardCompleted) { FirstRunWizard(...) }`
  block at the end is gone; the home is what first launch shows. The
  calendar/contacts permission launchers stay (Settings still calls them
  on user action). Comments retargeted ("the wizard and Settings call
  them" → "Settings calls them"; media-player grant-path comment).
- `ui/HomeViewModel.kt` — `resetLayoutToDefaults()` no longer preserves a
  wizard flag (there is none); `clearAllData()` KDoc now says the next
  launch is home-first with the single default-home prompt.
- `ui/DebugScreen.kt` — KDoc, the clear-all description, the "All launcher
  data cleared…" action message, and the "First-run wizard completed"
  toggle row (deleted) all retargeted to home-first.
- `ui/SettingsScreen.kt` — the Access-row comment "(Also reachable from
  the notification panel and the first-run wizard.)" drops the wizard.
- `ui/MediaPlayerBar.kt` — KDoc grant-path comment drops the wizard.
- `data/BackupManager.kt` — KDoc silent-gap list: wizard state was the
  fourth gap; now three (surfaceAlpha, weather city, nowBriefHour/Minute).
- `app/src/test/.../MainActivityStartupTest.kt` — `firstLaunchShowsWizard`
  is now `firstLaunchShowsHome` (asserts the Search pill + At a Glance
  strip are displayed and that no "Welcome"/"Skip setup" node exists);
  the `dismissWizardIfShown()` helper is gone; header/`@Before` comments
  updated. No test-only flags.

### Default-home prompt flow (the ONLY first-run prompt)

- `HomeViewModel.defaultHomeIntent()` is unchanged in behaviour — the
  standard system flow already in the codebase: RoleManager `ROLE_HOME`
  request on API 29+, `Settings.ACTION_HOME_SETTINGS` below. Its KDoc no
  longer calls it the wizard's intent.
- New one-time decision in `HomeViewModel.maybePromptDefaultHome()`,
  called once from HomeScreen (`LaunchedEffect(Unit)`): if the prompt
  flag is already set, or Marlow is already the default home
  (`isDefaultHome()` — the same `resolveActivity` HOME-intent check
  Settings uses for rule 66), nothing is owed. Otherwise the flag is set
  and `defaultHomePromptPending` fires; HomeScreen launches the intent
  through the existing `defaultHomeLauncher` (result needs no handling)
  and consumes the pending flag. The flag is set BEFORE firing, so the
  prompt never repeats — not on a decline, not on a layout reset
  (`resetLayoutToDefaults()` does not re-prompt: a layout reset is not a
  first run), not on recreation.
- New one-time flag in `LauncherStore`: `default_home_prompt_shown`
  (`loadDefaultHomePromptShown` / `saveDefaultHomePromptShown`). It is a
  flag like `seed_repaired`, NOT a `LauncherSettings` field, and it is
  not carried in backups — a restored install prompts once. Clearing all
  data wipes it (via `clearAll`), so the next launch prompts again.
- Settings → About → "Change the default home app" is unchanged: the
  user-action route to the same system flow.

## 2. Dock — exactly five icons, labels off, one page (F5)

- `model/LauncherModels.kt` — new `const val DOCK_MAX_ITEMS = 5` (kind
  order documented there); `HomeItem.dockPage` deleted; `LauncherSettings.
  dockIndicatorStyle` deleted. `PageIndicatorStyle` stays for home pages.
- `ui/Dock.kt` — `DockBar` rewritten: no `HorizontalPager`, no page
  computation, no `PageIndicator`. One `Row` of at most five items
  (`take(DOCK_MAX_ITEMS)` as a render guard). Labels off for every tile
  kind: `AppIconTile(..., showLabel = false)`; `ContactTile` /
  `ShortcutTile` / `PairTile` likewise. Row-height floor drops the old
  +18dp label allowance (`iconSizeDp + 8`). The search pill is untouched
  and stays a separate element below the dock — it is NOT a dock icon
  and does not count toward the five.
- `ui/AppIcon.kt` — `AppIconTile` gains `showLabel: Boolean = true`;
  the label renders only when `showLabel && settings.showLabels`, so
  home-grid behaviour is unchanged.
- `ui/HomeGrid.kt` — `ContactTile` / `ShortcutTile` / `PairTile` gain
  `showLabel: Boolean = true`, gating their text the same way. (The
  motion/haptics hunks in this file are the parallel builder's, not
  this commit's.)
- `ui/HomeViewModel.kt` — `moveItemToDock(itemId)` and
  `addAppToDock(component)` drop the `dockPage` parameter and return
  `Boolean`; `addPinnedContact(uri, name, page, toDock)` replaces the
  `dockPage: Int?` parameter with `toDock: Boolean` and also returns
  `Boolean`. `canAddToDock()` is the cap test. `refuseDockAdd()` sets
  the refusal for the overview's up-front check.

### Five-icon seed order and cap enforcement

Seed (`dockSeedApps`, in `HomeViewModel`): Phone / Messages / browser /
mail / Camera, in that order. Each kind resolves to the device's default
app for the kind via `PackageManager.resolveActivity` with
`MATCH_DEFAULT_ONLY` (honours the user's chosen default; the manifest
already declares `QUERY_ALL_PACKAGES`, so resolution sees all apps),
falling back to the curated seed the home stream used:
- Phone: `ACTION_DIAL` → `com.google.android.dialer`, `com.android.dialer`
- Messages: `ACTION_MAIN` + `CATEGORY_APP_MESSAGING` →
  `com.google.android.apps.messaging`, `com.android.mms`,
  `com.samsung.android.messaging`
- Browser: `ACTION_VIEW` `https://example.com` → `com.android.chrome`,
  `org.mozilla.firefox`, `com.brave.browser`
- Mail: `ACTION_MAIN` + `CATEGORY_APP_EMAIL` → `com.google.android.gm`
- Camera: `MediaStore.ACTION_IMAGE_CAPTURE` →
  `com.google.android.GoogleCamera`, `com.android.camera`

A kind with no installed app is skipped (never a dead slot); duplicates
are deduped by component. `seedHome` uses this seed for the dock and
excludes the dock's components from page 0 (replaces the old
`ordered.drop(5)` / `ordered.take(5)`). `repairHomeSeedIfNeeded` step (d)
tops the dock up from the same kind seed instead of the generic curated
order.

Cap enforcement — a sixth icon is REFUSED, never silently dropped:
- Mutation paths: `addAppToDock`, `moveItemToDock`, `addPinnedContact`
  (toDock) return false and set `dockAddError` ("Dock is full — remove
  an icon from the dock first.") when the dock already holds five.
  Already-in-dock is a no-op success (true). Desktop-lock still refuses
  (false, as before).
- Long-press sheet (`ui/LongPressSheet.kt`): the "Add to dock…" row calls
  the add/move directly — no page picker any more — and on refusal shows
  the message inline in the sheet and stays open. `DockPagePickerDialog`
  is deleted (both call sites gone).
- Overview "Contact to dock" (`ui/HomeScreen.kt` + `ui/Overview.kt`):
  the cap is checked BEFORE the contact picker opens; a full dock
  closes the overview and fires the refusal, so the snackbar is visible
  (it would be hidden behind the overview). A picked contact closes the
  overview too, so the result — or the refusal — is visible on the home.
- HomeScreen shows `dockAddError` as a snackbar (mirrors the existing
  `widgetAddError` pattern).
- Load/restore normalisation (`normalizeDock` in `HomeViewModel`): a
  list loaded from storage or a backup with more than five dock items
  (possible only from the old multi-page dock) keeps the first five in
  the dock, in order; extras move to free cells on page 0 where one
  exists and are dropped only when page 0 is full. Applied in `init`
  (store rewritten when it changes anything) and in `applyBackup`.
  `DockBar`'s `take(5)` is the final render guard.

### Dock pages / indicators removed

- `ui/SettingsScreen.kt` — Dock screen: the "Page indicators" row is
  gone, `rowKeys` drops `"dock_indicators"`, KDoc "5 rows" → "4 rows".
  `ChoiceKey.DOCK_INDICATORS`, its search-index entry, and its
  choice-options / selected-id / apply branches are all deleted.
  `ChoiceKey.INDICATOR_STYLE` (home pages) stays.
- `ui/HomeScreen.kt` — the overview's `DockPagePickerDialog` call site
  is gone (see above); `showContactDockPicker` state, its `BackHandler`,
  and its `anyOverlayOpen` entry are gone.
- `ui/LongPressSheet.kt` — `dockCounts`, `showDockPicker`, and
  `DockPagePickerDialog` are gone (see above).

## Persistence keys removed

- `wizard_completed` (`LauncherStore.KEY_WIZARD_COMPLETED`) — field,
  key, save and read lines all deleted. Old stored values are inert:
  DataStore ignores unknown keys, and nothing reads the field.
- `dock_indicator_style` (`LauncherStore.KEY_DOCK_INDICATOR`) — key,
  save and read lines deleted; `BackupManager` `dock_indicator_style`
  put/restore entries deleted. Old stored values are inert the same way.
- `HomeItem.dockPage` record slot — deleted from the model and from
  both serialisers (`LauncherStore.saveHomeItems` / `loadHomeItems`,
  `BackupManager.homeItemToJson` / `fromJson`). The record is now
  19 fields instead of 20.

### Old-record compatibility (both loaders)

Records written before this rebuild carry the retired dockPage at index
3 (13–20 fields); new records omit it (exactly 19 fields). Both loaders
detect the shape by size (`f.size == 19` → no retired slot, else offset
1 for every field after index 3) so pre-rebuild layouts load with
positions intact. Residual edge: a record written by a transient
intermediate dev build with exactly 19 fields INCLUDING dockPage would
mis-parse positions (not crash — the loader is defensive); the current
build always wrote 20 fields, and every save rewrites the whole list,
so no such record is expected to exist.

## Device-gated (not verifiable here)

- First-run flow: home-first landing, single default-home prompt firing
  once, no prompt after a decline, no prompt after a layout reset.
- Dock seed on a real device: the five kinds resolve to the device's
  defaults (Pixel 10a: Google Dialer / Messages / Chrome / Gmail /
  Google Camera expected) in that order, labels off, one page.
- Default-home prompt behaviour under the Robolectric startup gate: the
  prompt decision runs on launch there too; `defaultHomeIntent()` is
  try/catch-guarded and the launch is wrapped, so a missing RoleManager
  or unresolvable intent skips the prompt instead of breaking the gate
  — CI is the authority.
- Dock-full refusal on a real device (sheet inline message, overview
  snackbar, contact-to-dock refusal).
- Old multi-page dock migration (`normalizeDock` moving extras to page 0)
  on an install that had one.
