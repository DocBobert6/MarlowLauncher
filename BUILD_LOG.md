# Launcher — Build Log

Working title "Launcher", placeholder package `app.launcher`.
Plan: `~/workspace/your_files/android-launcher-plan/android-launcher-plan.pdf`
(93 approved / 77 denied or removed, of 170 — audited 2026-11-26).
Decisions: `~/workspace/launcher-plan/feature-decisions.md`.

## Local-build verdict (2026-11-26, Phase 0)

**Local Gradle builds are IMPOSSIBLE in this sandbox — proven, twice.**
The standing 2026-11-15 note said the `other_tcp: Deny` permission stops
Java processes reaching the network. Verified again today:

1. A raw Java socket to the egress proxy is refused with "Other TCP
   connections is turned off for this assistant"; a Java HTTPS fetch
   fails the same way, while curl reaches the same hosts fine.
2. A Java loopback test (ServerSocket + Socket on 127.0.0.1) connected,
   but the bytes were rewritten in transit — the client read "muse:"
   where the server wrote "hello". The sandbox's TCP interception
   corrupts Java's loopback streams.
3. Gradle 9.9.1 (even `--no-daemon`, which still forks a single-use
   daemon) fails with "Could not connect to the Gradle daemon /
   Peer did not prove it holds the expected connection token" — the
   daemon handshake bytes are corrupted exactly as (2) predicts.

No bypass was attempted and none will be: no renamed binaries, no
permission changes, no new downloads. Only Robert can flip that
setting.

**What works locally instead — the kotlinc check.** The workspace
holds a complete, proven Gradle module cache from the Automap project
(`~/workspace/automap/.gradle-home`, 2.4 GB, AGP 9.4.1 / Kotlin 2.4.20
/ Compose BOM 2025.01.00); a copy lives at
`~/workspace/.gradle-launcher` (213 jars, count verified identical).
`tools/kotlinc-check.sh` runs kotlinc 2.2.20 in-process (no daemon, no
network) and type-checks every main Kotlin source against android.jar
plus the cached androidx classes. It proves Kotlin syntax, types, and
Compose usage. It does NOT prove resource/manifest merging, the
Robolectric startup gate, or the dex guard — those run on CI
(`.github/workflows/android.yml`), which is the authority. Note the
version skew: kotlinc here is 2.2.20, the project builds with Kotlin
2.4.20 on CI.

Consequences:
- Every phase must end with a passing `./tools/kotlinc-check.sh` and
  the result recorded in this log. Never claim a Gradle build.
- The Robolectric startup-gate test is **not** in the offline cache,
  so it runs on CI only. The gate test file exists in the repo.
- Kotlin code must not reference `app.launcher.R` (no generated R
  class exists for kotlinc). Strings/constants live in Kotlin;
  resources live in XML and the manifest.
- If a phase ever needs a dependency that is not in the cache, that
  phase is code-complete but type-check-unverified for the affected
  files until CI runs. Record it per phase, never paper over it.

## Phase order (defined here — NOT Robert's decision; he never set a build order)

Bundled where sensible, each phase independently reviewable.

- **Phase 0 — Scaffold (this phase).** Project layout, Gradle setup pinned
  to the cached toolchain, local git repo, README with GitHub migration
  steps, GitHub Actions workflow (not run — no GitHub yet), dex guard +
  Robolectric startup gate from the start, placeholder MainActivity,
  `tools/kotlinc-check.sh` local type-check.
  Status: COMPLETE 2026-11-26 (kotlinc-check PASS, 2 files; Gradle build
  test failed as proven — local Gradle impossible, CI is the authority).
- **Phase 1 — Core home.** Grid (#1, #62, #64), home pages (#3, #75, #76,
  #66, #67), dock (#4, #68, #71, #112 search pill), drawer (#11, #12, #13,
  #17, #78–#84, #86, #87, #80, #81), folders (#14), icons (#19, #20, #22,
  #98, #99, #100, #101), one-handed layout (#5), desktop lock (#65),
  auto-add apps (#172), pinned shortcuts/contacts (#94, #95), App Pairs
  (#96), long-press sheet (#30, #97, #173).
- **Phase 2 — Search, glance, themes, wallpaper.** Universal search (#18,
  #104, #106, #107 voice only), At a Glance (#10, #121 Smartspacer off by
  default), Now Brief (#37), themes (#23, #50, #51 Opaque/blur glass,
  #24), wallpaper picker local-only (#53 note, #77), blur glass surfaces,
  hide status bar (#57), stickers (#58), headphone music (#61).
  Status: COMPLETE 2026-11-26 (kotlinc-check PASS, 24 files; no Gradle
  build — local Gradle impossible, CI is the authority).
- **Phase 3 — Widgets + feed.** Resizable widgets (#6), stacking (#7),
  widget page (#122), built-in widget suite (#113, #115, #119, #126),
  productivity panels as widgets (#118, no Quick Tiles), auto-hiding
  media player (#120), RSS/Discover feed + Google feed bridge (#38,
  fragility noted), widget overlap (#63).
  Status: COMPLETE 2026-11-26 (kotlinc-check PASS, 33 files; no Gradle
  build — local Gradle impossible, CI is the authority).
- **Phase 4 — Gestures, badges, vault, backup.** Gestures (#26, #28, #29,
  #88, #89, #90, #91), badges (#31, #128, #129, #132), inline previews
  + direct reply (#127), hide apps + PIN vault (#34), backup/restore
  manual local file only (#47), per-app language (#155), status/nav bar
  control (#154), landscape (#151), foldable/tablet layouts (#150, #152).
  Status: COMPLETE 2026-11-26 (kotlinc-check PASS, 41 files; no Gradle
  build — local Gradle impossible, CI is the authority).
- **Phase 5 — Settings, wizard, polish.** Settings + settings search
  (#168), first-run wizard (#169, theme choice = the two release themes
  only), reset & debug tools (#171), accessibility conformance (#175:
  TalkBack, keyboard/D-pad, 48dp targets, reduced motion, contrast),
  lightweight budget as a goal (#176), final pass over every toggle
  default (extra/Pro-style features off by default).
  Status: COMPLETE 2026-11-26 (kotlinc-check PASS, 45 files, no errors
  or warnings; no Gradle build — local Gradle impossible, CI is the
  authority).

## Open questions for Robert (never guessed — recorded, not decided)

1. **App name — DECIDED 2026-11-26: Marlow Launcher.** Robert picked
   it from a 15-name shortlist cleared against live app stores
   (10 names survived the check: Marlow, Alder, Glimmer, Fern, Cove,
   Summit, North, Clove, Birch, Rowan). Applied to `app_name` in
   strings.xml. Package `app.launcher` stays as-is (a label change,
   not a package change).
2. **App icon — DECIDED 2026-11-26: C "Recessed screen".** Robert
   picked it from four depth variants of a miniature home screen
   (the v1 generic glyphs were rightly rejected). Wired in as the
   adaptive icon: foreground vector drawable
   (ic_launcher_foreground.xml — recessed panel, 2x2 tiles, glowing
   tile, light spill as 15 evenOdd annuli, dock bar; geometry from
   icon-options/draw_icons_v2.py, render-verified against the design
   PNG with icon-options/verify_icon.py) + background warm charcoal
   #262019 (ic_launcher_background.xml).
   **REVISION v3 (2026-11-26, finish job — design UNCHANGED, still
   C).** Robert installed the run-14 APK and the icon was wrong:
   his installer screenshot
   (`~/workspace/user/files/Screenshot_20261126-130825_0_ik4f.png`)
   shows four flat tiles in a dark circle under Android's circular
   mask — the recessed depth is invisible, the light spill is gone,
   and the dock pill is cropped/invisible. He re-picked C from the
   four v2 options, so the DESIGN is confirmed; the failure was
   small-size legibility, not the design and not the wiring (the
   run-14 APK carries exactly the C vector). Root cause: the v2
   panel (#1B1713) is barely darker than the background (#262019),
   its 0.8dp top highlight is sub-pixel at 48px, the spill alphas
   are too faint to survive downsampling, and the dock pill
   (#332E28 on #1B1713) has almost no contrast. V3
   (`icon-options/draw_icons_v3.py`, design `icon-3c-recessed.png`):
   panel deepened to near-black #0E0B08; a clearly lighter warm rim
   (#57493B, 3dp) around the panel so the recess edge reads; tiles
   enlarged (92px vs 84px at 512) and moved up; spill strengthened
   (blurred tile copy alpha 200, haze alpha 140) and rebuilt in the
   vector as nine concentric discs (radii 21–4dp, alphas 04–62);
   dock pill made paper #F8F1E7, centred, well inside the circle.
   **LESSON (standing): never approve an icon on the 512px square
   alone. Icons must be verified at real launcher size (48px and
   96px) under the circular mask (both 66dp and 72dp diameters).**
   `icon-options/verify_icon.py` now enforces it: 512px agreement
   with the design (mean abs diff 3.66/channel, limit 6), 48px
   masked spill tint measurable (delta 26, limit 20 — v2 measured
   ~10), dock pill paper-bright at 48px masked, and aapt2 compiles
   the vector clean. The 48px masked render reproduces Robert's
   screenshot for v2 (four flat tiles) and shows the fixed v3
   (panel edge, spill tint on the neighbours, paper dock bar).
   Known cosmetic remainder: the vector's stepped discs leave a
   faint ring around the glowing tile visible only at 512px on
   close inspection; invisible at launcher size — accepted.
3. **Accent/shading defaults — DECIDED 2026-11-26.** Accent: P22 CRT
   phosphor blue (ZnS:Ag, x 0.15, y 0.06), lifted in lightness so it
   reads as glowing — #3333CC light, #5555FF dark (replacing the
   Google-blue #1A73E8 / #8AB4F8). Applied to BLUE in AccentColour.
   Shading: warmer and softer across the whole warm family (Robert:
   "a little more subtle and maybe slightly warmer, whatever needs
   to be done for the CRT color look"). Light: background #FAF7F2 →
   #F8F1E7, surface #FFFFFF → #FDF9F1 (never pure white on a CRT),
   surfaceVariant #EFE9E0 → #EEE5D6. Dark: background #1E1B18 →
   #262019 (a CRT black is a soft dark grey), surface #2A2622 →
   #2E2820, surfaceVariant #3A352F → #40382C, text #F2EDE6 →
   #F5EEE1. AMOLED variants untouched (true black by definition).
   Glow treatment DECIDED 2026-11-26: soft subtle halo — a
   `phosphorGlow` modifier in Theme.kt (6.dp elevation, ambient
   0.35 / spot 0.25 in the accent colour), applied only to the
   accent's hero shapes: the current-page indicator dot (HomeGrid)
   and the widget resize handle (Widgets). Never on text, icons,
   or the scrollbar. Item 3 is now fully decided.
4. **At a Glance data sources — weather DECIDED + BUILT 2026-11-26
   (finish job).** Provider: Open-Meteo — free, no API key, no
   account, no location permission. The user types a city in
   Settings → At a Glance; the free geocoding API resolves it, and
   the forecast API returns the current temperature + WMO weather
   code (30-minute cache). The strip renders the weather slot like
   any other slot, and the weather widget shows the same data (or
   plainly says no location is set yet). Commute / finance / sports
   still have NO provider chosen — no free no-key equivalent exists
   for them; they stay placeholder slots and show nothing, and are
   recorded as awaiting provider picks. Calendar is real (system
   calendar). Reminders for Now Brief still have no source.
5. **Google feed bridge fragility.** #38 stays as planned, but the bridge
   uses unofficial Google-app internals and may break when Google
   changes them. Flagged in the plan; no action unless Robert says so.
6. **Robolectric locally.** The startup gate runs on CI only until the
   offline cache gains Robolectric. Acceptable, or should the cache be
   extended (needs a machine with network)?
7. **Hidden Desktop widget page (#122) — how is it reached?** The plan
   approves "a hidden Desktop widget page" but does not say how the
   user gets to it. Phase 3's choice: it is a second widgets-only
   surface opened from the widget page ("Desktop widgets" button),
   never in the swipe sequence; its items live on page -3. If Robert
   wants it reached another way (overview, a gesture, a settings
   entry), say so — the page itself is built, only the entry point
   is a guess.
8. **Google feed bridge — not implemented, by design.** There is no
   public Google feed API for a third-party launcher; the bridge
   would have to use the Google app's unofficial internals. Phase 3
   ships the bridge as a clearly-marked optional toggle (OFF by
   default) whose status function reports the gap, and the feed
   degrades to RSS-only. No Google feed items are shown or invented.
   Whether to attempt a real integration against the Google app's
   internals (and accept that it may break) is Robert's call.
9. **Weather provider (still open, now also for #126).** The weather
   widget shows the same placeholder state as At a Glance — no
   provider chosen, no fake data. Same open question 4, extended to
   the widget.
10. **Settings-search jump behaviour (Phase 5 choice).** Tapping a
    search match shows only that setting's area, with a "Show all
    settings" row to get back — it does not scroll to the row inside
    the full list. If Robert would rather the tap scrolled to the row
    in place, say so; the index already knows each setting's area.
11. **Reset keeps the vault; clear-all wipes it (Phase 5, by design).**
    Reset layout to defaults keeps hidden apps and the vault PIN
    (it resets the layout, not private data) and keeps the wizard
    completed. Clear all launcher data wipes everything including
    the PIN and the wizard flag, so the next launch is a first
    launch again. If either boundary is wrong, say so.

## Phase log

### Phase 5 — Settings, wizard, polish (2026-11-26, COMPLETE — kotlinc-check PASS, 45 files)
Extended Phases 1–4 rather than rewriting them. New files:
`data/Accessibility.kt` (system reduced-motion check, app version
for the debug screen, 48dp constant), `ui/SettingsControls.kt`
(shared switch/slider/chip/reorder rows, moved out of the old
sheet), `ui/SettingsScreen.kt` (the one settings surface + search
index + per-area sections), `ui/FirstRunWizard.kt` (#169),
`ui/DebugScreen.kt` (#171 reset & debugging). Deleted:
`ui/SettingsSheet.kt` — its bottom-sheet copy of every setting row
is gone; the full-screen SettingsScreen is the only settings
surface now. Extended: `model/LauncherModels.kt`
(`wizardCompleted` on LauncherSettings), `data/LauncherStore.kt`
(`wizard_completed` key + `clearAll()`), `ui/HomeViewModel.kt`
(`completeWizard`, `resetLayoutToDefaults`, `clearAllData`,
`iconCacheSize`, bounded icon cache), `ui/HomeScreen.kt` (settings
+ debug overlays, wizard overlay), `ui/Widgets.kt` (reduced-motion
auto-rotate, 48dp resize-handle hit area), `ui/AppIcon.kt`
(explicit TalkBack label on the tile), `ui/BuiltinWidgets.kt`
(48dp delete buttons), `app/build.gradle` (removed unused
`ui-tooling-preview`), `MainActivityStartupTest.kt` (wizard-aware
gate — see below).

Built, per feature number:
- #168 settings search inside launcher settings: one full-screen
  settings surface, organised by area — Home, Drawer, Dock,
  Search, Themes, Gestures, Notifications, Privacy, Backup,
  About — with a search box at the top. The search matches a
  85-entry index (every setting by name + keywords; corrected from
  74 by the 2026-11-26 audit — the code has 85, the log said 74);
  tapping a match jumps to that setting's area (the list then shows only
  that area, with a "Show all settings" row to get back — see
  open question 10). Every setting from Phases 1–4 is reachable
  here: grid, icons, behaviour, At a Glance, wallpaper/display,
  widgets & feed, long-press menu order, overview actions (Home);
  drawer style/sort/search/frequent/position/columns (Drawer);
  dock visibility/height/background/shadow/pill (Dock); provider,
  suggestions, custom engines (Search); mode/theme/accent/font/
  surface/AMOLED (Themes); gesture master + all seven mappings
  (Gestures); badges style/position/size/radius, dynamic badges,
  inline previews, long-press preview (Notifications); vault
  (Privacy); backup export/import (Backup); version + reset &
  debugging entry (About). The old bottom sheet held a second
  copy of these rows; it is deleted, not kept alongside.
- #169 first-run setup wizard — shown on first launch only
  (`wizardCompleted` in DataStore, false until finished or
  skipped). Five steps: Welcome, Theme, Grid, Search provider,
  Optional grants. Theme choice inside it is ONLY between the
  two release themes (Material You and curated stock) — no theme
  maker, no custom themes. Grid choice is the #1 sizes; search
  provider choice is the #104 list (DuckDuckGo default). The
  grants step explains both in plain words — notification
  access for badges/previews/media player, calendar access for
  At a Glance events — with "Open notification settings" /
  "Allow calendar access" buttons and a Continue past them;
  skipping either leaves the launcher working (badges stay
  empty, the strip shows the date only) and both can be granted
  later from Settings. "Skip setup" on the first step exits at
  once; choices apply as they are picked, so a mid-way skip
  keeps what was already chosen. Back steps backwards; it never
  dismisses the wizard. The wizard never shows again unless
  all launcher data is cleared (#171).
- #171 reset & debugging tools + clear-all: a debug screen
  (Settings → About → "Reset & debugging…") showing the app
  version + package (from the PackageManager), counts (installed
  apps, home items, home pages, hidden apps, notes/tasks/habits/
  timers, feed sources, icon-cache entries), and every toggle
  state, built live from LauncherSettings so it cannot drift.
  Two confirmed actions: "Reset layout to defaults" (settings
  to defaults, one home page, home re-seeded; hidden apps and
  the vault PIN are kept, wizard stays completed) and "Clear
  all launcher data" (wipes every DataStore key + in-memory
  state; the next launch is a first launch again, wizard
  included). Each sits behind its own confirmation dialog
  spelling out what is kept or lost.
- #175 accessibility conformance in the launcher itself (audit
  notes below — every Phase 1–4 screen was checked).
- #176 lightweight budget as a goal (pass notes below — APK/RAM
  cannot be measured here; CI/device own those numbers).

Final toggle-default table (verified against the Phase 1–4 logs;
no drift found — nothing needed fixing):
ON by default — dock visible/background/shadow (#4/#71), search
pill (#112), pull-to-search (#82), remember drawer position +
auto-close (#81), two-column drawer (#84), show labels (#22),
Smartsize icons (#100), At a Glance strip + calendar slot (#10),
search suggestions (#104), widget page (#122), auto-hiding media
player (#120, only ever shows while playing), Discover feed
(#38), gestures master (#26), badges (#31, numbers, top right,
18dp, corner 9dp), inline previews (#127) + long-press preview
(#132), landscape (#151), separate screen layouts (#150),
two-pane tablet (#152). OFF by default — infinite scroll (#66),
frequent row (#80), themed icons (#20), live clock (#101),
one-handed (#5), desktop lock (#65), auto-add apps (#172),
headphone music (#61), AMOLED black (#24), glance weather /
commute / finance / sports (#10, no provider), Smartspacer
(#121), Now Brief (#37), hide status bar (#57), scrolling
wallpaper + gradient (#77), hidden Desktop widget page (#122),
widget overlap (#63), Smart Stack auto-rotate (#7), Google feed
bridge (#38), dynamic badges (#128). Triple-tap maps to NONE
(#91). Non-boolean defaults unchanged from the phases: grid
5x5, margins 8dp/8dp, indicators dots/accent, default page 0,
dock height 72dp, drawer list/sort-by-name/search-top, icon
rounded/56dp, theme Auto + Stock + Blue accent + System font,
surface Opaque, search provider DuckDuckGo, double-tap → Lock
device, two-finger swipe → Notifications, two-finger
double-tap → Overview, swipe-down → Notifications, pinch-in →
Overview, pinch-out → Open drawer, bar control System default.

Accessibility audit (#175) — what was checked, per screen:
- TalkBack labels: every IconButton in the app was grepped.
  All icon-only buttons carry a contentDescription (Drawer,
  Overview, UniversalSearch, BuiltinWidgets, FeedPage,
  MediaPlayerBar, reorder rows, the dock search pill's "Search"
  semantics, the drawer affordance's "Open app drawer", the
  At a Glance strip's "At a Glance"); decorative icons pass
  null. Fixed this phase: AppIconTile now names the app on its
  clickable node instead of relying on merged child text, and
  the widget resize handle gained a "Resize widget" label.
  SwitchRow merges its label + switch into one TalkBack node
  (label + on/off read together).
- Keyboard / D-pad: every control is a focusable Compose
  control (Switch, FilterChip, Slider, TextButton,
  OutlinedTextField) — nothing is pointer-only. Settings and
  the debug screen close on system back (BackHandler); the
  wizard's back steps backwards through its steps. Known gap:
  widget resize/move and the long-press sheets are reached by
  long-press/drag gestures with no keyboard equivalent — a
  keyboard user can add/remove widgets through the picker and
  the widget sheet's buttons once open, but cannot long-press
  a widget open from the keyboard. Recorded, not rebuilt.
- 48dp touch targets: SwitchRow and ReorderRow now enforce a
  48dp minimum height; IconButton is 48dp by default.
  Fixed this phase: the four delete buttons in the notes/tasks/
  timers/habits widgets were forced to 32dp (now the default
  48dp), and the widget resize handle's hit area went 32dp →
  48dp (the 32dp visual circle is unchanged). Known exception:
  the drawer's alphabet edge-index letters are ~20dp tall each
  — 26 letters at 48dp cannot fit a phone screen; this is the
  industry-standard pattern, each letter stays focusable and
  labelled.
- Reduced motion: `isSystemReducedMotion` (Accessibility.kt)
  reads the system animator-duration and transition-animation
  scales; either at 0 means no motion. The Smart Stack
  auto-rotate now jumps instantly instead of animating when
  the system has animations off; the home pager already
  navigates with scrollToPage, never an animation. There is no
  launcher-level reduced-motion toggle — the system setting is
  the source of truth, and it is read fresh when the stack
  composes.
- Contrast (stock theme, WCAG ratios computed from the exact
  theme values in Theme.kt — all pass AA, >= 4.5:1): light
  onBackground on background 15.42:1, light onSurface on
  surface 16.48:1, white on primary blue 4.51:1 (the tightest
  pair, just above AA), light secondary on background 5.66:1;
  dark onBackground on background 14.72:1, dark onSurface on
  surface 12.89:1, dark primary on background 8.13:1, dark
  onPrimary on primary 7.41:1, dark secondary on background
  6.49:1. The Material You dynamic scheme is system-generated
  per wallpaper and was not computed — its contrast is the
  system's responsibility.

Lightweight pass (#176 — a GOAL, not a requirement; nothing
was cut for it):
- Removed the unused `androidx.compose.ui:ui-tooling-preview`
  dependency — no @Preview exists anywhere in the app.
- Kept the `lifecycle-runtime-compose` /
  `lifecycle-viewmodel-compose` declarations: no direct imports
  of them exist in main code, but activity-compose pulls them
  in transitively, so deleting the lines would not shrink the
  APK. Recorded, not removed.
- Icon drawable cache is now bounded at 256 entries (it was an
  unbounded HashMap); clearing it just re-reads icons from the
  package manager. No other unbounded cache was found:
  launchStats is keyed by installed apps, DataStore records by
  user data, and icon work goes through the single drawableFor
  cache (no duplicate icon loading).
- 13 implementation dependencies, no new ones added in this
  phase; no duplicate icon work, no unbounded caches remain.
- NOT measured here (stated plainly): APK size and RAM cannot
  be measured without a build or a device — local Gradle is
  impossible in this sandbox and there is no device. APK size
  is for CI (the workflow uploads the debug APK artifact) and
  RAM is for Robert's Pixel 10a. The <30 MB / <100 MB targets
  stand as goals.

Verification: `./tools/kotlinc-check.sh` PASS, 45 source files,
no errors or warnings (2026-11-26). Two deprecation warnings
(Icon ArrowBack) appeared on the first run and were fixed to
the AutoMirrored version before the final run. No Gradle build
attempted (local Gradle impossible — see verdict above); CI is
the authority. Robolectric startup gate UPDATED and still
truthful: a fresh install lands on the first-run wizard, so the
gate now asserts that state — `firstLaunchShowsWizard` checks
the wizard's welcome step, and `launchesAndShowsHome` /
`survivesRecreation` tap the same "Skip setup" button a user
would, then assert the dock search pill ("Search") and the
drawer affordance ("Open app drawer"). The wizard is NOT
skipped under test by any test-only flag. The gate still runs
on CI only (Robolectric not in the offline cache). Dex-guard
discipline kept: the settings surface is split per area (one
small LazyListScope function each) and the wizard/debug screens
are separate files; no new composable approaches 255 registers.

Limitations / open items (recorded, not guessed):
- No device test: the wizard flow, the settings search jump,
  reset/clear-all, the debug screen, reduced-motion behaviour
  and the 48dp fixes are all code-complete but unproven on
  Robert's Pixel 10a.
- The gate test changes are CI-only verification — the test
  file is not covered by kotlinc-check (main sources only), so
  a typo there would surface on CI, not locally. The APIs used
  (onAllNodesWithText / fetchSemanticsNodes / performClick)
  are standard ui-test-junit4 APIs.
- `survivesRecreation` dismisses the wizard after recreation
  if it is showing: the DataStore load after a recreate is
  async, so the wizard may or may not still be there when the
  assertion runs — the helper handles both.
- Keyboard users cannot long-press a widget open (see audit
  above); the alphabet edge index stays below 48dp per letter
  (see audit above).
- APK size / RAM against the #176 targets are unmeasured
  (see lightweight pass above).

### Phase 4 — Gestures, badges, vault, backup (2026-11-26, COMPLETE — kotlinc-check PASS, 41 files)
Extended Phases 1–3 rather than rewriting them. New files:
`data/NotificationMonitor.kt` (NotificationBus: live notification
previews + direct reply via RemoteInput), `data/VaultStore.kt`
(PinCrypto: salted SHA-256 PIN hashing, pure), `data/DeviceLock.kt`
(device-admin lock + receiver), `data/AdaptiveLayout.kt` (layout
variant from smallest width + orientation), `data/BackupManager.kt`
(hand-rolled JSON backup export/import + minimal JSON parser),
`ui/Gestures.kt` (double/triple-tap, multi-finger gestures, badge
composable), `ui/NotificationPanel.kt` (#127 previews + reply sheet),
`ui/VaultSheet.kt` (#34 vault sheet + #155 language dialog).
Extended: `model/LauncherModels.kt` (GestureAction, ShortcutSecondary-
Action, BadgeStyle/Position, BarVisibility, LayoutVariant,
AppLanguageOverride, SwipeDirection, Phase 4 settings),
`data/LauncherStore.kt` (Phase 4 keys + PIN salt/hash, hidden apps,
per-app languages, swipe-up actions), `data/MediaMonitor.kt` (the
existing listener now also feeds NotificationBus),
`ui/HomeViewModel.kt` (hidden/visible apps, vault, backup, gestures,
reply, languages), `ui/HomeScreen.kt` (gesture wiring, vault +
notification overlays, SAF pickers, landscape + two-pane),
`ui/HomeGrid.kt` + `ui/Dock.kt` + `ui/Drawer.kt` (badges on icons),
`ui/AppIcon.kt` (badge overlay on every icon), `ui/LongPressSheet.kt`
(#132 preview, hide, swipe-up picker, language), `ui/SettingsSheet.kt`
(Phase 4 section), `MainActivity.kt` (#154 bar control),
`AndroidManifest.xml` (device-admin receiver), `res/xml/device_admin.xml`
(force-lock policy only).

Built, per feature number:
- #26 standard gestures: swipe up for the drawer (unchanged Phase 1
  behaviour), swipe down for the notifications panel (the launcher's
  own #127 panel — there is no public API for a third-party app to
  expand the system shade), double-tap to lock. Lock route used:
  **device-admin lock** — DevicePolicyManager.lockNow() once the user
  has enabled the app as a device admin; when the admin is not yet
  enabled, the lock action opens the system admin-enrollment screen
  instead of doing nothing silently. The admin policy lists
  force-lock only (no wipe, no password rules).
- #28 swipe-up secondary action per shortcut: mapped per home item in
  its long-press sheet (None / App info / Long-press sheet / First
  shortcut). The detector is attached only when an action is mapped;
  unmapped icons behave exactly as before (swipe-up still opens the
  drawer from them).
- #29 multi-finger gestures: one awaitEachGesture loop tracking two
  pointers (pinch ratio, shared displacement, quick two-tap). Nothing
  is consumed by the loop, so a two-finger swipe also scrolls a pager
  page; the mapped action fires as well (documented trade-off).
- #88 pinch in/out on home screen: pinch-in opens the overview
  (Phase 1 #76), pinch-out opens the drawer — both mappable in
  settings.
- #89 double-tap empty space: mappable, toggleable (gestures master
  switch), defaults to Lock device.
- #90 two-finger swipe / two-finger double-tap: mappable; defaults to
  notifications / overview. A single two-finger tap does nothing.
- #91 triple-tap home-screen action: mappable, off by default (NONE).
  Double- vs triple-tap is decided with a 320 ms window: the second
  tap starts a pending double-tap job, a third tap inside the window
  cancels it and fires the triple-tap instead.
- #31 notification badges: dots or numbers, from the live
  notification set via the existing NotificationListenerService
  (the same service that already fed the media player — no second
  service, no second grant). Ongoing/system notifications are
  skipped; the launcher never cancels, swallows or re-posts
  anything. Badges are simply absent until the listener is granted.
  Shown on home icons, dock icons, the drawer's frequent row and
  drawer pages grid. Drawer list rows and drawer-folder contents do
  not show badges (not threaded through those two composables — see
  limitations).
- #128 dynamic badges: when on, the badge shows the notification's
  large icon (avatar / artwork) instead of the dot/number; falls
  back to the styled badge when the app posted no large icon.
- #129 numeric badge style + position, size (8–28dp slider), corner
  radius (0–14dp slider). Counts above 99 show as "99+".
- #132 notification preview in long-press popup: up to 3 newest
  previews (title + text) for that app at the top of the sheet.
- #127 inline notification text previews + direct reply on the home
  screen: a bottom sheet listing live notifications grouped by app
  with title/text/time; reply goes through the notification's own
  RemoteInput action exactly as the system shade does, only where
  the app supports it (rows without a reply action show no field).
  Reached from swipe-down (#26) and from the sheet itself. The
  toggle is ON by default — it is what swipe-down opens, so leaving
  it off would make the standard swipe-down gesture dead out of the
  box; it only shows notifications the user already sees in the
  shade, and it can be turned off.
- #34 hide apps + PIN vault: hidden apps are excluded from the
  drawer, universal search and home (visibleApps); they appear only
  in the vault after the PIN. The PIN is 4–8 digits, stored only as
  a random 16-byte salt + SHA-256(salt + PIN) in DataStore — never
  in plaintext — and compared in constant time. Hiding an app before
  any PIN exists opens the vault to set one, then completes the
  hide. Removing the PIN leaves apps hidden until a new PIN is set
  and entered. The vault locks again when dismissed.
- #47 backup & restore home layout — MANUAL LOCAL FILE ONLY:
  export writes one plain-JSON document (version 1: settings +
  home items, folders, pages, drawer folders, pairs, contacts,
  renames, icon overrides, hidden apps, languages, swipe-up
  actions, widget stacks, feed sources, notes/tasks/habits/timers)
  to a file the user picks in the system picker (SAF CreateDocument,
  suggested name "launcher-backup.json"); import reads it back and
  replaces layout + settings, validating the version field and
  dropping unparseable records rather than failing the whole
  restore. No schedule, no selective backup, no cloud destinations,
  no named portable format, no backup-honesty extras — none of those
  were built. No serialisation library is a dependency, so the JSON
  writer and parser are hand-rolled in BackupManager.kt.
- #155 per-app language: a BCP 47 tag stored per app (dialog in the
  long-press sheet, common tags offered); the system owns applying
  another app's locale, so saving also opens the system per-app
  language screen for that app (ACTION_APP_LOCALES_SETTINGS, with
  app-details fallback). For the launcher's own package the tag is
  applied directly through LocaleManager (API 33+).
- #154 status-bar / navigation-bar control: System default / Hide
  navigation bar / Hide both bars, applied in MainActivity. #57
  hide-status-bar (immersive) still applies on top: SYSTEM_DEFAULT
  defers to #57 for the status bar; HIDE_BOTH wins over #57.
- #151 landscape home screen: in landscape the At a Glance strip is
  hidden (the date stays on the lock screen and in Now Brief) and
  the dock shortens to 56dp; the grid itself is unchanged so item
  positions never break.
- #150 separate layouts per screen + #152 two-pane tablet layout:
  layout variants are computed from smallest screen width +
  orientation (AdaptiveLayout.kt): >= 600dp smallest width is the
  wide variant (two-pane: home + feed page, else widget page);
  below that is the phone layout, portrait or landscape. On the
  Pixel 10a (smallest width about 412dp) only the two phone
  variants ever occur — the foldable/tablet variants are present
  but dormant there, recorded as such.

Toggle defaults chosen (extra/Pro-style OFF by default):
ON — gestures master switch, double-tap → Lock device, pinch-in →
Overview, pinch-out → Open drawer, two-finger swipe → Notifications,
two-finger double-tap → Overview, swipe-down → Notifications,
badges (numbers, top right, 18dp, corner 9dp), long-press preview,
inline previews (see #127 note above), landscape, separate screen
layouts, two-pane tablet. OFF — triple-tap (NONE), dynamic badges,
swipe-up secondary actions (NONE per item until mapped). Bar control:
System default.

Verification: `./tools/kotlinc-check.sh` PASS, 41 source files, no
errors or warnings (2026-11-26). No Gradle build attempted (local
Gradle impossible — see verdict above); CI is the authority.
Robolectric startup gate UNCHANGED and still truthful: it asserts
the dock search pill ("Search") and the drawer affordance ("Open
app drawer"), both still present with the same descriptions — the
gesture layer and the new overlays do not move them. The gate still
runs on CI only (Robolectric not in the offline cache). Dex-guard
discipline kept: the new composables were split per surface
(gesture modifiers, badge, notification panel, vault sheet,
language dialog); no new composable approaches 255 registers.

kotlinc-check fix (2026-11-26, Phase 4): BackupManager's local
`enum` helper used `enumValueOf<T>` with a non-reified type
parameter — replaced with `Enum.valueOf(fallback.javaClass, name)`.
The drawer pages-grid badge call landed in DrawerPages, which had
no badge values in scope — badge counts/bitmaps are now threaded
through DrawerPages from DrawerScreen.

Limitations / open items (recorded, not guessed):
- A NotificationListenerService needs no XML service description —
  the manifest entry (permission + intent-filter, already present
  since Phase 3) is the whole declaration. The only new XML is the
  device-admin policy (res/xml/device_admin.xml).
- Two-finger swipes are not consumed by the gesture loop, so they
  also scroll pager pages; the mapped action fires as well.
- Swipe-up secondary actions shadow swipe-up-for-drawer on the
  specific icons that have one mapped (unmapped icons unaffected).
- Drawer list rows and drawer-folder contents do not show badges
  (only home, dock, frequent row and drawer pages do).
- The notification panel opens only when inline previews are on;
  with the toggle off, swipe-down (mapped to Notifications) does
  nothing — the toggle is ON by default, so this only bites if the
  user turns it off.
- Per-app language for other apps is system-owned: the launcher
  stores the tag and deep-links to the system screen; only the
  launcher's own locale is applied directly.
- No device test: gestures (especially pinch / two-finger /
  double-tap timing), badges, direct reply, the device-admin lock
  flow, backup export/import round-trip, landscape and two-pane
  layouts are all code-complete but unproven on Robert's Pixel 10a.
  Two-pane / foldable variants cannot be exercised on the 10a at
  all (smallest width about 412dp).

### Phase 3 — Widgets + feed (2026-11-26, COMPLETE — kotlinc-check PASS, 33 files)
Extended Phases 1–2 rather than rewriting them. New files:
`data/WidgetHost.kt` (AppWidgetHost wrapper: allocate/bind/size/
release), `data/FeedRepository.kt` (RSS 2.0 + Atom fetch/parse over
plain HTTP, no feed library), `data/GoogleFeedBridge.kt` (bridge
status only — no Google API exists, see below),
`data/MediaMonitor.kt` (NotificationListenerService feeding a
process-local media-session bus), `ui/Widgets.kt` (host view,
resize overlay, Smart Stack surface), `ui/BuiltinWidgets.kt`
(first-party suite surfaces), `ui/WidgetPage.kt` (widget page,
hidden Desktop page, picker, widget sheet), `ui/FeedPage.kt`
(Discover page), `ui/MediaPlayerBar.kt` (auto-hiding bar + setup
hint).
Extended: `model/LauncherModels.kt` (widget kinds, page codes,
suite/notes/tasks/habits/timers/stack/feed/media models, Phase 3
settings), `data/LauncherStore.kt` (Phase 3 keys + notes/tasks/
habits/timers/stacks/feed-sources persistence; HomeItem record
extended with four appended fields, old records still load),
`ui/HomeViewModel.kt` (widget/feed/media state + mutations),
`ui/HomeScreen.kt` (pager with feed + widget pages, host
lifecycle, bind flow, media bar), `ui/HomeGrid.kt` (widget/stack
rendering + resize overlay), `ui/Dock.kt` (widget kinds render
nothing — #69 removed), `ui/SettingsSheet.kt` (Phase 3 section),
`AndroidManifest.xml` (INTERNET for RSS, media listener service).

Built, per feature number:
- #6 resizable standard widgets: AppWidgetHost/AppWidgetManager,
  picker over installed providers, bind flow with the system
  ACTION_APPWIDGET_BIND confirmation when the system asks for it,
  span from the provider's minimum size, corner-handle resize in
  edit mode (long-press → Resize), sizes persisted and reported to
  the provider via updateAppWidgetOptions. Host ids are released
  when a widget is removed.
- #7 widget stacking / Smart Stack: stacks are placed items whose
  members live on page -2; swipe through them with a pager + dots,
  auto-rotate every 10 s (toggleable, OFF by default), add to an
  existing stack or a new one from the widget sheet, remove the
  current member, dissolve (members return as plain widgets).
- #122 dedicated widget page (swipe-left) + hidden Desktop widget
  page: the widget page is the last pager page; the Desktop page is
  a second widgets-only surface opened from the widget page, never
  in the swipe sequence (see open question 7). Widgets move between
  the widget page and home page 0 from the widget sheet.
- #113 built-in widget suite: Notes, Tasks, Timers, Habits,
  Calendar, Weather, System — first-party Compose surfaces fed by
  the same data layer as the rest of the launcher.
- #115 system-monitor panel: battery (live %), memory used/total,
  storage used/total — all read live from the system, no service.
- #119 notes page: notes as a suite widget (add/delete, DataStore).
- #126 calendar + weather widgets: calendar shows today's system
  calendar events (same source as At a Glance); weather shows the
  placeholder-provider state — "No weather provider chosen yet" —
  no fake data, same gap as open question 4.
- #118 productivity panels as widgets: notes, tasks (check off),
  timers (1/5/10/25 min presets, start/stop, survives as an end
  time), habit tracking (daily check-off + streak, resets each
  day). Quick Tiles are NOT included (quick settings in the
  launcher = denied #43).
- #120 auto-hiding media player: appears only while something is
  playing; drives the active session's transport controls
  (play/pause/next/previous) via MediaSessionManager. Sessions are
  visible only through a NotificationListenerService, granted by
  the user in system settings; until then a one-line setup hint
  shows. The launcher plays nothing itself.
- #38 RSS feed / Discover page + Google feed bridge: swipe-right
  page, user-added RSS sources (HTTP(S) only, duplicates ignored),
  fetched newest-first, tap opens the entry in the browser, fetch
  failures show the error text and never invented entries. The
  Google feed bridge is an optional toggle (OFF by default) whose
  status is reported at the top of the page; there is no public
  Google feed API, so the bridge contributes no items and the feed
  degrades gracefully to RSS-only (see open question 8).
- #63 widget overlap: toggleable, OFF by default. When off, a
  dragged item that would overlap another snaps back; when on,
  items may overlap, still on the grid. Note: this changed
  moveHomeItem — Phase 1/2 drags previously allowed overlap
  silently; now the default (overlap off) refuses overlapping
  drops.

Toggle defaults chosen (extra/Pro-style OFF by default):
ON — widget page (#122), auto-hiding media player (#120, it only
ever shows while playing), Discover feed (#38). OFF — hidden
Desktop widget page (#122), widget overlap (#63), Smart Stack
auto-rotate (#7), Google feed bridge (#38).

Verification: `./tools/kotlinc-check.sh` PASS, 33 source files, no
errors or warnings (2026-11-26). No Gradle build attempted (local
Gradle impossible — see verdict above); CI is the authority.
Robolectric startup gate UNCHANGED and still truthful: it asserts
the dock search pill ("Search") and the drawer affordance ("Open
app drawer"), both still present with the same descriptions — the
dock sits outside the pager, so the new feed/widget pages do not
move it. The gate still runs on CI only (Robolectric not in the
offline cache). Dex-guard discipline kept: the new composables were
split per surface (host view, resize overlay, stack surface, one
surface per suite widget, feed rows, media bar); no new composable
approaches 255 registers. AppWidgetHost calls are wrapped in
try/catch so the gate cannot crash on a missing AppWidget service
under Robolectric.

kotlinc-check fix (2026-11-26, Phase 3): `LocalContext.current`
inside a `remember { }` lambda is not a composable context —
hoisted the context out of the lambda (`remember(localContext)`).

Limitations / open items (recorded, not guessed):
- Infinite scroll (#66) + extra pages do not combine: when the
  feed and/or widget page is on, the pager is finite with the
  extras at fixed positions; infinite scroll spans the home pages
  only when both extras are off. The page indicator counts home
  pages only.
- Long-press-drag on a third-party widget shares the gesture with
  the widget's own touch handling (the host view also receives
  touches); moving a widget by drag is code-complete but unproven
  on device. Resize / stack / remove all go through the widget
  sheet, which does not depend on the drag.
- The widget bind flow, RSS fetching, media-session mirroring,
  notification-listener grant, and the host-view rendering are all
  code-complete but unproven on Robert's Pixel 10a.
- The Google feed bridge is status-only (open question 8); the
  weather widget is placeholder-only (open question 9); the
  Desktop widget page's entry point is the Phase 3 choice (open
  question 7).
- Timers keep only an end time: a timer that expires while the app
  is closed shows its full length again (not a notification — no
  alarm/notification work was decided).
- No device test: widget hosting, feed fetching, media controls and
  stack auto-rotate are all unproven on device.

### Phase 2 — Search, glance, themes, wallpaper (2026-11-26, COMPLETE — kotlinc-check PASS, 24 files)
Extended Phase 1 rather than rewriting it. New files:
`data/SearchEngine.kt` (universal search + calculator/timer/dial +
suggestions), `data/GlanceProvider.kt` (At a Glance provider
interface: real date + calendar, placeholder weather/commute/
finance/sports, Smartspacer stub), `data/WallpaperHelper.kt`
(apply local wallpaper + scrolling offsets), `data/HeadphoneMonitor.kt`
(wired + Bluetooth headphone-connect receiver),
`ui/UniversalSearch.kt`, `ui/AtAGlance.kt` (strip + Now Brief sheet),
`ui/WallpaperSheet.kt` (wallpaper controls + sticker dialog),
`ui/HeadphoneMusicSheet.kt`, `ui/theme/Theme.kt` (rewritten: modes,
two themes, accent, fonts, AMOLED, surface styles).
Extended: `model/LauncherModels.kt` (new enums/models + Phase 2
settings), `data/LauncherStore.kt` (Phase 2 keys + custom providers),
`ui/HomeViewModel.kt` (glance/search/wallpaper/sticker/headphone
state), `ui/HomeScreen.kt` (wiring), `ui/Dock.kt` (surface style +
pill now opens universal search), `ui/HomeGrid.kt` (sticker tile),
`ui/SettingsSheet.kt` (Phase 2 sections), `MainActivity.kt` (theme
from settings, status-bar control, headphone monitor),
`AndroidManifest.xml` (READ_CALENDAR, BLUETOOTH_CONNECT).

Built, per feature number:
- Universal search: #18 apps, contacts (system contacts, READ_CONTACTS),
  settings (32-entry launcher index, tapping one opens settings),
  files (MediaStore display-name match), web (chosen provider), plus
  #106 in-search calculator (pure-expression evaluator: + - * / % ^
  parentheses, decimals; division by zero / non-expressions return no
  row), timer ("timer 5 min" / "1h 30m" / "90s" → system clock intent),
  direct-dial (number-shaped query → dialler). #104 custom search
  engines (add/remove, name + query URL with %s) + suggestions toggle:
  suggestions are fetched live from the active provider's suggestion
  endpoint (DuckDuckGo / Google / Brave JSON; Bing endpoint returns a
  different shape, so Bing yields no suggestions — see limitations).
  Default provider is DuckDuckGo (Robert's AutoSpace choice; a choice
  for review, not a plan decision). #107 voice search ONLY, via the
  system recogniser — visual/camera search was explicitly NOT built.
- #112 the dock search pill now opens the real universal search.
- At a Glance: #10 strip (date line + today's calendar events, up to
  3 shown) above the grid, toggleable. Date = system clock; calendar =
  system calendar (READ_CALENDAR, asked at runtime when the strip is
  on; denial leaves the slot empty). Weather/commute/finance/sports
  are placeholder providers returning nothing — no fake data anywhere.
  #121 Smartspacer: toggle + provider slot, OFF by default; the plugin
  protocol is NOT bound in this phase, so it contributes nothing yet.
- Now Brief: #37 morning summary sheet (tap the strip when enabled):
  today's schedule from the same calendar data + the enabled slots.
  No health data (no Health Connect dependency in the plan). Reminders
  have no source chosen, so the sheet says so plainly.
- Themes: #23 modes Auto/Light/Dark + exactly the two release themes
  (Material You = system dynamic colour on Android 12+, stock =
  curated warm paper/charcoal) + accent picker (6 curated colours) +
  font choice (System/Serif/Monospace, platform families only). #50
  Ambient is launcher-local: in the stock theme the accent comes from
  the picker, NOT the system wallpaper colours. #51 surfaces: Opaque
  vs custom blur glass (translucent surfaces — true backdrop blur via
  a RenderEffect pass is deferred, see limitations). No Liquid Glass,
  no Frosted as a separate style, no theme maker (custom themes are
  way later). #24 AMOLED black (pure-black dark background).
- Wallpaper: #53 note — picker is the system picker (ACTION_GET_CONTENT,
  image mime type) only: photos / wallpapers / other apps on the phone;
  NO built-in sources. Picking sets the system wallpaper and turns the
  home wallpaper layer on (transparent home surface + dim overlay).
  #77 scrolling wallpaper (pan via WallpaperManager offsets on page
  change) + gradient / dimming (0–70%), all toggleable.
- #57 hide status bar / ultra-immersive (WindowInsetsController, from
  settings). #58 stickers / free text: home items (tap to edit, delete
  from the edit dialog, drag like any item); a sticker in the dock
  just shows its text. #61 headphone music: wired plug + Bluetooth
  A2DP connect → sheet offering installed music apps (MEDIA apps with
  music keywords); nothing auto-plays.

Toggle defaults chosen (extra/Pro-style OFF by default):
ON — At a Glance strip (#10), calendar slot, search suggestions
(#104). OFF — weather/commute/finance/sports slots (no provider),
Smartspacer (#121), Now Brief (#37), AMOLED black (#24), hide status
bar (#57), scrolling wallpaper / gradient (#77), headphone music
(#61). Surface style Opaque (#51); theme mode Auto, theme Stock,
accent Blue, font System. Theme default DECIDED 2026-11-26: Stock
(Robert's pick; Material You stays one tap away in Settings and the
first-run wizard). Wallpaper dim 0%.

Verification: `./tools/kotlinc-check.sh` PASS, 24 source files, no
errors or warnings (2026-11-26). No Gradle build attempted (local
Gradle impossible — see verdict above); CI is the authority.
Robolectric startup gate UNCHANGED and still truthful: it asserts the
dock search pill ("Search" content description) and the drawer
affordance ("Open app drawer"), both still present with the same
descriptions; the glance strip is an additional element above the
grid. The gate still runs on CI only (Robolectric not in the offline
cache). Dex-guard discipline kept: the largest new composables
(UniversalSearchScreen, SettingsSheet additions) were split into
small functions; no new composable approaches 255 registers.

kotlinc-check fix (2026-11-26, Phase 2): Kotlin block comments NEST —
`image/*` inside a KDoc comment in WallpaperHelper.kt and
WallpaperSheet.kt opened a nested comment and swallowed the file
(unclosed-comment error + phantom unresolved references). Reworded to
"image mime type". Also: this Material 3 version has no
`Typography(defaultFontFamily = …)` parameter — fonts are applied by
copying every text style (Theme.kt `withFont`).

Limitations / open items (recorded, not guessed):
- Bing suggestions return no rows: the Bing suggestion endpoint's
  response shape is not the [query, [suggestions]] JSON the other
  providers return, so fetchSuggestions drops it. DuckDuckGo, Google
  and Brave suggestions work.
- Blur glass is translucent surfaces, not true backdrop blur — a
  RenderEffect pass over the wallpaper behind the panel is deferred.
- Smartspacer (#121) is a toggle + provider slot only; the plugin
  protocol is not bound, so turning it on changes nothing visible.
- Now Brief reminders: no reminders source exists; the sheet says so.
- Calendar permission is requested on first composition when the
  glance strip + calendar slot are on (both defaults) — a runtime
  prompt at first launch.
- The first-run home seed and the Phase 1 default-page edge case are
  unchanged by Phase 2.
- No device test: the calendar query, wallpaper offsets, voice
  recogniser, headphone receiver and status-bar hiding are all
  code-complete but unproven on Robert's Pixel 10a.

### Phase 1 — Core home (2026-11-26, COMPLETE — kotlinc-check PASS, 16 files)
Replaced the Phase 0 placeholder home with the real home. New files:
`model/LauncherModels.kt` (all settings + layout models),
`data/LauncherStore.kt` (DataStore Preferences persistence),
`data/AppRepository.kt` (LauncherApps, shortcuts, icon packs, categories),
`data/IconUtils.kt` (#100 Smartsize normalisation),
`ui/HomeViewModel.kt` (state + all mutations),
`ui/HomeScreen.kt`, `ui/HomeGrid.kt`, `ui/Dock.kt`, `ui/Drawer.kt`,
`ui/FolderView.kt`, `ui/LongPressSheet.kt`, `ui/Overview.kt`,
`ui/SettingsSheet.kt`, `ui/AppIcon.kt` (incl. #101 live clock).
MainActivity now hosts HomeScreen with the ViewModel.

Built, per feature number:
- Grid: #1 grid size 4x5/5x5/5x6/fluid; #62 sub-grid half-cell
  positioning (drag snaps to half-cells); #64 margin + spacing sliders.
- Pages: #3 multiple pages, reorder/hide/add/delete in overview;
  #75 default home page; #76 overview via pinch or long-press on empty
  space; #66 infinite scroll (virtual 200x pager, modulo-mapped);
  #67 indicators dots/numbers/lines/none + accent/white/black colour.
- Dock: #4 hide/resize/add/remove; #68 scrollable dock pages (up to 5);
  #71 background/shadow/indicator styling; #112 search pill — opens
  the drawer with search focused (in-drawer search is #17; full
  universal search is Phase 2, so the pill is the drawer-search entry
  point, not a no-op placeholder).
- Drawer: #11 list + horizontal pages; #12 auto-categories (keyword
  heuristic over label + package); #13 drawer folders + tabs;
  #17 in-drawer app-name search; #78 draggable fast scrollbar;
  #79 alphabet edge index; #80 frequent row (own launch history);
  #81 remember position + auto-close; #82 search-bar top/bottom +
  pull-to-search (drag down on the drawer handle); #83 styles + sort
  by name/install date/most-used/size/last used (size = APK file
  size, approximate); #84 two-column on wide screens; #86 red-X
  uninstall mode; #87 reorder inside drawer folders (up/down).
- Folders: #14 drag app onto app creates a folder; rename, remove,
  dissolve at one app.
- Icons: #19 icon packs (Nova/ADW theme intent + appfilter.xml
  parsing; packs without appfilter are listed but unmapped);
  #20 themed icons, basic: single-colour accent silhouette;
  #22 shapes/sizes/hide labels; #98 rename + per-app icon swap;
  #99 Quickedit suggestions from the active pack; #100 Smartsize
  normalisation; #101 live clock icon (real hands, 1s tick).
- #5 one-handed layout (content pushed to the bottom third);
  #65 desktop lock (disables moves); #172 auto-add new apps;
  #94 pinned shortcuts (via the long-press sheet); #95 pinned
  contacts (system contact picker, from the overview);
  #96 App Pairs (see limitation below);
  #30 long-press shortcuts; #97 per-app sheet (App info / Screen
  time / Uninstall / Rename / Edit icon); #173 reorderable
  long-press menu + overview-action customisation (both in settings).
- Settings sheet: every Phase 1 toggle is adjustable on device
  (the full settings UI with search is Phase 5, #168).

Toggle defaults chosen (extra/Pro-style OFF by default):
OFF — infinite scroll (#66), frequent row (#80), live clock (#101),
themed icons (#20, basic silhouette; tinting every icon one colour
hurts recognition), auto-add apps (#172), desktop lock (#65),
one-handed (#5). ON — search pill (#112), remember drawer position
+ auto-close (#81), two-column drawer (#84), Smartsize (#100),
show labels (#22), dock visible/background/shadow (#4/#71),
pull-to-search (#82).

Verification: `./tools/kotlinc-check.sh` PASS, 16 source files, no
errors or warnings (2026-11-26). No Gradle build attempted (local
Gradle impossible — see verdict above); CI is the authority.
Robolectric startup gate updated: it asserted the placeholder
"Launcher" text; it now asserts the dock search pill ("Search"
content description) and the drawer affordance ("Open app drawer"),
keeps the config-changes + icon tests and survivesRecreation.
The gate still runs on CI only (Robolectric not in the offline cache).

kotlinc-check.sh fix (2026-11-26, Phase 1): the classpath glob
`! -path '*org.jetbrains.kotlin*'` also excluded
`org.jetbrains.kotlinx` (coroutines — a declared dependency), so
every coroutines import failed. Fixed to `*org.jetbrains.kotlin/*`
so only the exact Kotlin group is excluded; kotlinx stays on the
classpath. Recorded here because the script is a shared tool.

Limitations / open items (recorded, not guessed):
- #96 App Pairs use the public API only: first app launched
  normally, second with FLAG_ACTIVITY_LAUNCH_ADJACENT. Forcing a
  windowing mode needs ActivityOptions.setLaunchWindowingMode, a
  hidden API absent from the public SDK (verified: no such method in
  android.jar API 37, WindowConfiguration is not in android.jar at
  all). Whether the Pixel actually splits the pair is a device-test
  question; if the system declines, both apps still launch.
- #97 Screen time opens the system usage-access settings — there is
  no per-app screen-time API for a launcher.
- #94 pinned shortcuts are placed via the long-press sheet ("Add to
  Home screen" on a shortcut); dragging a shortcut out of the sheet
  onto the home is not implemented.
- #95 pinned contacts land on the home only; placing a contact in
  the dock or a folder is not implemented.
- #80/#83 usage data is the launcher's own launch history (no
  UsageStats permission); counts start at zero on first install.
- Default-page index is into the full page list; hiding pages
  before the default page shifts the initial page by one per hidden
  page. Edge case, not yet handled.
- The home gesture stack (pinch-to-overview + swipe-up-for-drawer +
  long-press-empty-space + horizontal pager on one surface) is
  code-complete but unproven on device — needs Robert's device test.
- First-run home seeding puts the first apps on page 0 and the
  first five in the dock; the seed set is whatever LauncherApps
  returns first, not curated.

### Phase 0 — Scaffold (2026-11-26, COMPLETE)
- Scaffold written: Gradle 9.9.1, AGP 9.4.1, Kotlin 2.4.20, Compose BOM
  2025.01.00, compileSdk 37, minSdk 29, targetSdk 35.
- Gradle cache copied from Automap (2.4 GB, 213 jars verified identical).
- CI workflow added (unit tests incl. Robolectric gate, assembleDebug,
  dex guard with `Lapp/launcher/` prefix, APK artifact upload).
- Gradle build test: FAILED as proven above (daemon handshake corrupted
  by the sandbox's TCP interception; `--no-daemon` fails identically).
  Local Gradle builds impossible; no bypass attempted.
- `tools/kotlinc-check.sh` built and verified: extracts 87 artifacts
  from the offline cache and type-checks main sources with kotlinc
  2.2.20 + Compose plugin. Phase 0 result: PASS, 2 source files.
- Local git repo initialised; Phase 0 committed.

## Whole-plan completion (2026-11-26 — all five phases)

The plan as decided (93 approved / 77 denied or removed, of 170)
is now built in full, in the local workspace only — no GitHub yet,
no Gradle build run anywhere, no device test run anywhere. CI is
the build authority for all of it.

- Phase 0 — Scaffold: project layout, Gradle pinned to the cached
  toolchain, CI workflow (tests + dex guard + APK artifact),
  kotlinc-check, local git repo. 2 files at the time.
- Phase 1 — Core home: grid, pages, dock, drawer, folders, icons,
  one-handed, desktop lock, auto-add, pinned shortcuts/contacts,
  App Pairs, long-press sheet. 16 files.
- Phase 2 — Search, glance, themes, wallpaper: universal search,
  At a Glance / Now Brief, the two release themes, wallpaper
  picker, stickers, headphone music. 24 files.
- Phase 3 — Widgets + feed: resizable widgets, stacking, widget
  page, built-in suite, RSS/Discover feed, auto-hiding media
  player. 33 files.
- Phase 4 — Gestures, badges, vault, backup: gestures, badges,
  inline previews + reply, hide apps + PIN vault, manual local
  backup, per-app language, bar control, landscape / foldable /
  tablet layouts. 41 files.
- Phase 5 — Settings, wizard, polish: one settings surface with
  search, first-run wizard, reset & debug tools, accessibility
  conformance, lightweight pass, final toggle-default pass.
  45 files.

Total: 45 main Kotlin source files + 1 Robolectric startup-gate
test, under `app/src/main/java/app/launcher/` (package
`app.launcher`, placeholder name "Launcher"). Every phase ended
with a passing `./tools/kotlinc-check.sh`; the final run is PASS,
45 files, no errors or warnings. What kotlinc-check does NOT
prove for any phase: resource/manifest merging, the Robolectric
gate, the dex guard, the APK itself — all CI-only. What no phase
has: a Gradle build or a device test (both impossible in this
sandbox; see the local-build verdict at the top of this log).

Deliberately NOT built (decided, not forgotten): Labs (#170),
per-part dark mode (#177), accessibility-first mode (#174),
custom themes / Theme Park (#49/#160), one-tap theme bundle
(#49), and every denied/removed item in feature-decisions.md.

Open questions carried forward for Robert's review (the full
list is in "Open questions for Robert" above — 1–11): the app
name and icon; accent/shading defaults; At a Glance data
providers (weather/commute/finance/sports + reminders); the
Google feed bridge fragility; Robolectric locally; the hidden
Desktop widget page's entry point; the settings-search jump
behaviour (#10); the reset-keeps-vault / clear-all-wipes-it
boundary (#11); and the still-unmeasured #176 APK/RAM targets
(CI for the APK, his Pixel 10a for RAM). Plus the per-phase
limitations recorded in each phase section — the device test
is the outstanding gate for the whole plan.

## GitHub (2026-11-26)

- Public repo **DocBobert6/MarlowLauncher** created 2026-11-26
  (https://github.com/DocBobert6/MarlowLauncher). Public is the point:
  public repos get free unlimited Actions minutes; the 2,000 monthly
  minutes only count against private repos.
- First push: commit 80d39ac (full app, phases 0–5 + all review
  decisions), pushed via the git-database API from a `git archive`
  staging copy, so the remote tree is exactly the committed tree
  (68 files). The local repo keeps the full 8-commit history; the
  remote starts from this one squashed commit.
- First CI run 45858275866 queued on push — the billing hold did NOT
  kill it on sight (the Mirage private-repo runs died in seconds with
  no runner). Run status is the hold test: if it goes green, public
  builds work; if it fails for billing, the hold blocks public too.

## Scaffold rebuild for GitHub (2026-11-26, Robert's call)

Runs 1–5 all failed before the app itself was ever tested — each one
a different build-scaffold incompatibility with AGP 9.4.1, found only
because local Gradle builds are impossible here (see the verdict at
the top) and CI is the first real Gradle run:
- Run 1 (45858275866): setup-android's interactive `sdkmanager
  --licenses` prompt died.
- Run 2 (45859125814): the same action tried to install the removed
  `tools` package. Fix: drop setup-android, use the runner SDK +
  license hash files.
- Run 3 (45859468986): `./gradlew: No such file or directory` — the
  repo had wrapper properties but no wrapper scripts or jar.
- Run 4 (45860112547): settings.gradle sets FAIL_ON_PROJECT_REPOS
  and the root build.gradle's `allprojects` block re-declared the
  same repositories. Fix: drop the redundant block.
- Run 5 (45860764902): AGP 9.0+ rejects the `kotlin-android` plugin
  (built-in Kotlin since AGP 9.0) and, as a knock-on, reported
  `compileSdk` missing.

The scaffold was written in the old style (buildscript classpath +
legacy `apply plugin:`) for the workspace's offline Gradle cache,
which cannot resolve modern plugin markers — a constraint that no
longer matters, since local Gradle cannot run here at all and CI is
online. Robert asked whether to rebuild the scaffold for GitHub and
start fresh; Pip recommended yes, Robert approved. The rebuild
(committed as one change):
- Root build.gradle: plugins DSL (`com.android.application` 9.4.1,
  `org.jetbrains.kotlin.plugin.compose` 2.4.20, both `apply false`);
  no buildscript block; no kotlin-android plugin anywhere.
- settings.gradle: added pluginManagement (gradlePluginPortal +
  google + mavenCentral); dependencyResolutionManagement unchanged
  (FAIL_ON_PROJECT_REPOS, google + mavenCentral).
- app/build.gradle: plugins DSL (`com.android.application`,
  `org.jetbrains.kotlin.plugin.compose`); kotlin-android removed;
  everything else unchanged (namespace, compileSdk 37, minSdk 29,
  targetSdk 35, version 0.1.0-dev, all dependencies, compose,
  jvmToolchain 17 + compilerOptions jvmTarget JVM_17).
- Wrapper completed: gradlew + gradlew.bat (Gradle 9.9.1, from the
  official repo) + gradle-wrapper.jar (49,154 bytes, verified Zip
  with GradleWrapperMain); gradle-wrapper.properties already
  pointed at 9.9.1. The workflow calls ./gradlew again.
- No Kotlin source, resource, manifest, icon, or decision changed.
  The app itself is untouched; only the build files around it.

## The Robolectric startup-gate saga — runs 6–14 (2026-11-26)

The rebuilt scaffold compiled and ran tests from run 6 on. Every
failure after that was the startup gate's test harness, never the
app code or the production manifest:
- Run 6 (45861592235): scaffold proven — Gradle configured,
  compiled, ran the tests. All 5 gate tests failed at launch:
  `Unable to resolve activity` for the MAIN + LAUNCHER intent.
- Run 7 (45862499765): HOME-intent fix failed test compilation
  (type inference on `ActivityScenarioRule(Intent)`).
- Run 8 (45863003824): explicit type args failed test compilation
  (the rule has no public `activity` property).
- Run 9 (45863671100): hand launch compiled and ran, but all 5
  failed identically at `resolveActivityInfo` — disproving the
  categories theory. Reading Robolectric 4.14.1's source: for an
  explicit intent, resolution is purely a `getActivityInfo`
  lookup; filters play no part.
- Run 10 (45864442139): naming the component with the app package
  (`app.launcher/app.launcher.MainActivity`) failed identically —
  disproving the package theory too.
- Run 11 (45865202047): `Robolectric.buildActivity(...).setup()`
  bypass worked — 2 of 5 passed (`launchesAndShowsHome`,
  `survivesRecreation`, the actual gate). 3 failed: the two
  PackageManager manifest checks returned zeroed stubs
  (configChanges 0, icon 0) and the wizard title was composed but
  reported not displayed.
- Run 12 (45866501972): the two manifest checks moved to a new
  workflow "Manifest guard" step that parses
  AndroidManifest.xml directly (icon, roundIcon, exported,
  singleTask, the five config changes, MAIN+HOME+DEFAULT filter)
  — the guard PASSED. The tests failed to compile:
  `assertExists` is not a top-level function in this Compose
  version. Fix: existence via `fetchSemanticsNodes` + assertTrue.
- Run 13 (45867014982): 2 of 3 passed; `firstLaunchShowsWizard`
  failed with NO "Welcome" node at all. Root cause is cross-test
  state: the settings DataStore file persists across test methods
  in the same JVM, so a Skip tap in an earlier test had already
  completed the wizard. Fix: @Before runs the app's own #171
  `LauncherStore.clearAll`, so every test starts from a fresh
  install regardless of execution order.
- Run 14 (45867746263): **GREEN.** All 3 gate tests, the Manifest
  guard, and the 255-register dex guard pass. First debug APK
  produced and verified from Pip's own download (see below).

## First debug APK — run 14 (2026-11-26, verified)

- Artifact `launcher-debug-apk` (16,310,469 bytes, exact match to
  GitHub's recorded size), APK inside 61,835,829 bytes.
- SHA-256: f4cff223cf983e1aabbf5acd2ad16b11b4b81230f4ee70ffd515a1a6bbaa0876
- Package app.launcher, version 0.1.0-dev (code 1), minSdk 29,
  targetSdk 35, label "Marlow Launcher". v2-signed with the Android
  Debug key (expected for a debug build). MainActivity present
  (`get_main_activity` returns None only because the filter is
  HOME, not LAUNCHER — correct for a launcher).
- Size note: 61.8 MB is large for a launcher — mostly the Compose /
  material payload (classes.dex 44.6 MB). The #176 lightweight
  target was a goal, not a requirement; size is unmeasured against
  it and worth a look before any release.
- Delivered to Robert 2026-11-26 with a direct download link
  (Muse storage, expires 2026-11-28). His device test is the gate:
  this build has never run on a device.

## Finish job — 2026-11-26 (Robert: "Go ahead and finish the entire app")

From "0.1.0-dev builds and passes its gate" to complete per the final
plan. Two audit passes (home core; search/glance/themes/widgets/feed/
media) plus the rough edges and open questions in this log. No Robert
decision was overturned; no version bump (stays 0.1.0-dev until his
device test passes). One bundled commit; CI is the build authority.

### Batch 1 — home core (kotlinc-check passed)

- **B1 page indices (critical).** HomeScreen builds a
  visibleFullIndices map; the pager, default page, overview
  selection and sticker placement all map through it. movePage /
  deletePage remap HomeItem.page (deleted page → page 0, later pages
  shift −1) and adjust defaultPage.
- **#94 pinned shortcuts reachable.** Long-press sheet gains "Add
  shortcut to Home screen…" per shortcut → addShortcutToHome. No
  drag-out path exists; the sheet row is the fix.
- **Dock add + dock pages reachable.** Long-press sheet "Add to
  dock…" with a dock-page picker (5 pages with counts) →
  moveItemToDock (home items) / addAppToDock (drawer apps); pinned
  contacts can go to a dock page too. #95 contacts in folders is
  NOT implemented by design (HomeFolder.apps is component names; a
  contact has no component).
- **B4 drawer long-press.** DrawerRow uses combinedClickable, so the
  long-press sheet opens from the default drawer list, not only
  from home icons / pages mode.
- **B8 desktop lock.** Guards added to renameFolder,
  removeAppFromFolder, addPage, deletePage, movePage, setPageHidden,
  setDefaultPage.
- **B6 grid spacing.** gridSpacingDp now insets each tile
  (spacing/2 per side) in HomeGrid — the slider does what it
  promises.
- **B5 #86 in pages mode.** DrawerPages renders the red-X uninstall
  affordance when uninstallMode is on.
- **B7 dock indicators.** Settings gains a "Dock page indicators"
  picker (dockIndicatorStyle was persisted but stuck at DOTS).
- **#5 one-handed.** Spacer weight 0.35f → 0.5f; recorded
  interpretation: the dock + bottom rows sit in the bottom third —
  shrinking the whole 5-row grid into one third would make the
  icons unusable.
- **#80 recency.** "Recently used" section (lastUsed desc, top 6)
  under the same toggle; launchShortcut now records usage (B12).
- **B11 TOOLS category.** categorize() gains a TOOLS keyword branch.
- **B9/B10 fast scrollbar + wide drawer.** Thumb travel from the
  real track height (was hardcoded 300dp); scrollbar now in the
  wide two-column branch; only the active layout's scroll state is
  saved.
- **#76 overview thumbnails.** Real mini-previews (first 8 app icons
  per page) instead of schematic boxes.
- **#112 search pill.** Rendered outside the dockVisible early
  return — hiding the dock no longer hides the pill.
- **#1 FLUID grid.** Real: columns derive from screen width
  ((widthDp / 64) clamped 4–8) via GridSize.effectiveCols.
- **#13 drawer custom order.** DrawerSort.CUSTOM + moveDrawerApp
  (up/down rows in the long-press sheet when CUSTOM is active);
  order persists in DataStore.
- **#99 icon suggestions.** Long-press icon picker renders each
  drawable's icon beside its name.
- **First-run seeding curated.** Dialer, messaging, browser, camera,
  photos, clock, Gmail, settings, Play Store first when installed,
  then label order (was raw LauncherApps order).
- **B13 gestures.** Double/triple-tap and long-press on empty space
  fire ONLY on empty space (hit-tested against item rects + the FAB
  bounds — the default double-tap action is Lock device, so a false
  positive is severe). Two-finger swipe Move changes are consumed so
  the pager doesn't also scroll; two-finger tap is suppressed when
  a pinch already fired. Triple-tap fixed properly (was unreachable:
  detectTapGestures' onDoubleTap fires per tap pair). Device check
  still owed for the remaining suspected conflicts.
- **#18 contacts (part).** READ_CONTACTS asked once on first search
  open + in the wizard GRANTS step + a Settings → Search row;
  rerunSearch re-runs the last query after the grant.

### Batch 2 — glance/weather/widgets/feed/media + theme + brief

- **#10 weather — Open-Meteo (DECIDED, see open question 4).**
  New data/WeatherProvider.kt (pure parsers + suspend network,
  10s timeouts, no key, no location permission) + WeatherGlanceProvider
  (slot WEATHER, reads the ViewModel's cached value) + ViewModel
  state (weatherNow, weatherCities, 30-min cache, search/set/clear/
  refresh) + settings city picker with geocoded matches. The strip
  is generalised: DATE header + any slot's items in slot order
  (WEATHER → CALENDAR → COMMUTE → FINANCE → SPORTS). Now Brief
  gains a Weather section when data exists. Commute/finance/sports
  stay placeholder (no free no-key provider). New Robolectric test
  WeatherProviderTest (parsing only, canned JSON, offline).
- **#126 weather widget.** Shows the live temperature + condition +
  city, or plainly "No location set yet" / "Fetching…"; tap
  refreshes. The old copy promised a provider picker that did not
  exist — the picker now exists in Settings → At a Glance.
- **#115 system monitor live.** 5-second LaunchedEffect refresh loop
  (was computed once per composition and went stale).
- **#61 Bluetooth trigger.** Listens to the A2DP profile broadcast
  (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) instead of the
  adapter-wide one — a watch, keyboard or non-A2DP car kit no longer
  pops the music sheet. **Found during this job:** BLUETOOTH_CONNECT
  was declared in the manifest but never requested at runtime, so on
  API 31+ the broadcast may never have been delivered at all. The
  #61 toggle now requests it when turned on (Settings).
- **#57 ultra-immersive.** systemBarsBehavior is now
  BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE when bars are hidden — a
  swipe brings them back transiently instead of the hidden state
  sticking.
- **#120 media session.** Prefers a STATE_PLAYING controller over
  the first controller with any playback state (a paused session no
  longer shadows the playing one).
- **#66 infinite scroll.** Settings now explains it under the toggle
  when the feed or widget page is on: the pager can't wrap past
  them. No pager redesign.
- **#24 transparency.** Surface-transparency slider for blur glass
  (30–90%, default 55% — the old fixed value), shown only when blur
  glass is active; Opaque ignores it. surfaceColorFor takes the
  alpha; dock + glance strip pass it.
- **#50 Ambient theme (DECIDED).** A third theme choice, Settings
  only (NOT the first-run wizard, which still offers only the two
  release themes). The accent comes from the system wallpaper's
  primary colour (WallpaperManager.getWallpaperColors, fallback to
  the stock accent); legibility-adjusted by luminance, re-derived
  when the theme is (re)selected or a wallpaper is applied. The
  warm stock backgrounds and surfaces stay stock — Ambient is a
  tint, not a new theme.
- **#37 Now Brief morning delivery.** Daily inexact alarm
  (RTC_WAKEUP, INTERVAL_DAY — no exact-alarm permission needed) +
  NowBriefReceiver posting a notification that opens the launcher;
  the brief is one tap away. OFF by default; hour/minute sliders
  in Settings (default 07:00); POST_NOTIFICATIONS requested when
  the summary is turned on. Known limits: the alarm does not
  survive a reboot on its own (re-scheduled whenever the launcher
  starts); the notification opens the app, it does not auto-open
  the sheet.
- **#25 animations / #97 screen time / #96 App Pairs — recorded
  as-is.** #25: framework defaults (pager swipe, sheets, stack
  auto-rotate); no custom transitions invented. #97: the row opens
  the system usage-access settings — there is no per-app screen-time
  API for a launcher. #96: split failure is undetectable from public
  APIs (the second app just launches fullscreen on top) — device-gated.
- **Settings-search jump, reset-keeps-vault / clear-all-wipes
  boundary, hidden Desktop widget page entry point — kept as
  implemented (coherent, documented behaviours); recorded as decided.**
- **Google feed bridge (#38) — kept status-only / RSS-only.**
  Unofficial Google-app internals are fragile by design; the brief
  marks it Robert's call.
- **Smartspacer (#121) — stub kept, deferred.** Binding the plugin
  protocol needs the Smartspacer SDK with no safe offline path; not
  faked.

### Bugs found in Batch 1 code during this job's verification

- The #18 contacts launcher (`contactsPermission`) and its
  `contactsPermissionAsked` flag were declared AFTER the
  LaunchedEffect that uses them (and the launcher declaration was
  missing from its first location) — kotlinc-check failed on
  unresolved references. Fixed: both declared before the effect.
  (The pre-compaction "kotlinc-check passed" note for Batch 1 does
  not match the tree as found — the fix is in this job's commit.)
- `settingsSections()` did not thread `onRequestContactsPermission`
  through to `searchSettingsSection()` — same unresolved-reference
  failure. Fixed by threading the param.

### Icon — C v3 (see review decision 2 above)

Same design, revised for real launcher size under the circular mask
and re-verified at 48px/96px masked. `verify_icon.py` now enforces
the small-size checks; `draw_icons_v3.py` is the design source.

### README

Rewritten: name Marlow Launcher, repo exists, CI green, local Gradle
impossible (kotlinc-check is the local gate), icon verification rule,
push method, standing rules. The stale "Name: undecided" and
"GitHub migration (later)" sections are gone.

### Still device-gated (Robert's test)

Everything in "Still device-gated" from the run-14 section, plus:
the icon v3 at real launcher size (the 48px masked render is the
acceptance proof, his eye is the gate); the weather fetch on a real
network; the A2DP trigger on a real Bluetooth device; the Now Brief
alarm firing overnight; the gesture hit-testing (double-tap on an
app icon must not lock the device); App Pairs split behaviour on
his Pixel. Nothing in this job was faked or silently dropped.

## Finish-job push — run 15 (45877165590, 2026-11-26, GREEN)

- Push: local commit d3c7d8f ("Finish the app: audit gaps, weather,
  Ambient, Now Brief, icon v3", 35 files, +2410/−313) → remote
  5396b4cfe84097e62f77e83f9be6afdcc3184ffb via git archive + push-tree.
- CI run 15 (45877165590): **GREEN on the first try** — all 3 gate
  tests (including the new WeatherProviderTest parsing tests), the
  Manifest guard, and the 255-register dex guard pass. No fix loop
  was needed. Bounded waiter: completed in ~2 minutes (3 polls).

## Second debug APK — run 15 (2026-11-26, verified)

- Artifact `launcher-debug-apk` (16,390,651 bytes, exact match to
  GitHub's recorded size), APK inside 62,048,681 bytes.
- SHA-256: e1cb731943d7f06a2af4b76dce41acbc4455a3e117a6c0aaa26cf71aa50f47c7
- Package app.launcher, version 0.1.0-dev (code 1), minSdk 29,
  targetSdk 35, compileSdk 37, label "Marlow Launcher". v2-signed
  with the Android Debug key (1 signer, CN=Android Debug, cert
  SHA-256 5214b884cffe4e570a4c6729411ffb3c88031293f4d338c08ad02567c7b84cd6).
- Verified in the APK from Pip's own download: POST_NOTIFICATIONS
  permission present; NowBriefReceiver declared in the merged
  manifest and present in dex; WeatherProvider +
  WeatherGlanceProvider present in dex; v3 icon vector present
  (res/drawable/ic_launcher_foreground.xml, 3,472 bytes).
- Size note: 62.0 MB (was 61.8 MB at run 14) — still mostly the
  Compose / material payload. The #176 lightweight target stays a
  goal, not a requirement; release minification is the future lever.
- Delivered with a direct download link (Muse storage, expires
  2026-11-28T18:39:37Z). Robert's device test is the gate: the
  finish job is complete per plan, green in CI, APK verified — what
  remains is his eye on the picture, the icon at launcher size,
  the gestures, the weather fetch, the A2DP trigger and the
  overnight alarm.

## Audit — Robert's verdict on the finish build (2026-11-26 13:46 EST)

Robert, on the run-15 build: "The whole thing kind of sucks. There's
no real default look. It doesn't look anything like a launcher that
would actually come stock on a phone. We really need to audit all of
these photos, all of the code, and everything else. Look back at the
plan and make sure everything we've had is included." Audit + fix
authorised. This section is the audit record — honest, unsoftened.
Screenshots: `~/workspace/user/files/146905_0_aove.png` (home),
`146906_1_svpx.png` (Home pages), `146907_2_3hlu.png` (Settings:
home grid), `146908_3_tyo7.png` (Settings: icons/behaviour),
`146909_4_nfeq.png` (Settings: At a Glance),
`146910_5_gthy.png` (Settings: wallpaper/widgets). All 912x2048.

### A. Systemic — no system-bar inset handling anywhere (affects all 6 shots)

- **A1 — App content draws under the status bar on every screen.**
  The system clock, notification icons and battery sit ON TOP of the
  app's own first row in all six shots. Root cause: zero uses of
  `statusBarsPadding`, `navigationBarsPadding`, `systemBarsPadding`
  or `safeDrawing` anywhere in `app/src/main/java/`; MainActivity
  only shows/hides bars via WindowInsetsControllerCompat and never
  calls `setDecorFitsSystemWindows`. On Android 15 edge-to-edge
  (targetSdk 35), content draws underneath the system bars unless
  the app pads for them. A stock launcher always clears the
  status-bar height.
- **A2 — Status-bar icons are white on pale cream, nearly
  invisible.** Bluetooth, "5G" and signal bars are white-on-cream
  (stockLight background #F8F1E7); only the red 18% battery reads.
  Cause: no `isAppearanceLightStatusBars` / light-status-bar flag
  anywhere (zero hits in code and res/). A stock launcher on a light
  background uses dark status icons.

### B. Shot 1 — home (146905_0_aove.png)

1. **At a Glance strip collides with the status bar (top).** The
   status time "1:46" is struck through by the black calendar icon,
   and a small circular icon sits on the date text so it reads
   "hursday, November 26" — the "T" is hidden. Cause: A1 + the strip
   placed at the very top of HomeScreen with no top inset.
2. **No clock anywhere on the home screen.** The only time visible
   is the status-bar "1:46". At a Glance shows date only
   (GlanceProvider formats the DATE slot as "EEEE, MMMM d", no time
   component). `liveClockIcon = false` by default; the built-in
   widget suite (NOTES/TASKS/TIMERS/HABITS/CALENDAR/WEATHER/SYSTEM)
   has NO clock widget; nothing seeds any widget by default. A stock
   home always shows the time prominently.
3. **No weather anywhere.** The strip's second line is only "Black
   Friday · All day" (calendar). Cause: `glanceWeather = false` and
   `weatherCityName = ""` by default, so the weather slot renders
   nothing.
4. **Home is barren — two icons, ~60% empty cream.** Only
   "Settings" and "Play Store" in the top-left of a 5x5 grid; the
   middle of the screen is blank cream from ~y450 to ~y1450. No
   wallpaper (`wallpaperVisible = false` by default), no widget, no
   clock, no weather. **Root cause — package visibility (see §E).**
5. **The same two apps are duplicated in the dock.** Settings and
   Play Store appear both in the grid and in the dock pill. Cause:
   `seedHome` puts `ordered.take(5)` in the dock from the SAME
   ordered list it already placed on page 0, with no exclusion. On
   a normally populated phone the first five essentials would all
   appear twice.
6. **Dock labels are sliced off at the dock pill's bottom edge.**
   "Settings" with the descenders cut ("Settin gs" — the g's are
   half-missing) and "Play Store" with the "y" descender cut; the
   text sits exactly on the pill's lower edge. Cause: Dock.kt fixes
   the dock Row to `.height(settings.dockHeightDp.dp)` = 72dp, while
   each tile is the 56dp icon PLUS a 12sp label (~73–76dp tall) —
   taller than its container, so the label is clipped by the pill
   Surface.
7. **A permission nag sits on the home screen as a permanent row.**
   "Media player needs notification access to see what is playing."
   with a blue "Grant" button, wrapping to two lines, above the
   dock. Cause: `mediaPlayerEnabled = true` by default + HomeScreen
   showing MediaSetupHint whenever the notification listener isn't
   granted. A stock launcher never puts a setup banner on the home
   screen.
8. **Orphan floating drawer button.** A lavender circle with a 3x3
   grid icon floats alone in the empty middle-right, with no label.
   Cause: the FloatingActionButton ("Open app drawer", BottomEnd +
   16dp) inside the grid Box. Stock launchers open the drawer by
   swipe-up, with no FAB; here it reads as a stray bubble in the
   void.
9. **No page indicator, even though the pager has 3 pages.** No dots
   between grid and dock, although the feed page (swipe right) and
   widget page (swipe left) are both ON by default. Cause:
   HomeScreen passes only the HOME page count (1) to PageIndicator,
   and HomeGrid returns early when `pageCount <= 1`. The indicator
   never reflects the real pager.
10. **Dock and search pill barely distinguishable from the
    background.** Both are surfaceVariant #EEE5D6 on background
    #F8F1E7 — a beige-on-cream difference of almost nothing, so the
    "pill" look collapses into flat cream. (Robert's warm shading
    values themselves are NOT the problem and stay — the fix is an
    outline/shadow on the pills, not a recolour.)

### C. Shot 2 — Home pages overview (146906_1_svpx.png)

11. **Status bar overlaps the title and the close button.** "1:45"
    over "Ho" of "Home pages"; the circular icon covers the "m"; the
    black X sits on top of the battery "18". Cause: A1 + Overview
    (16dp padding only, no inset).
12. **One tiny page card, ~65% blank screen.** A single lavender
    card ("Page 1 • Home", two 28dp mini icons) in the top-left of a
    2-column grid; everything below ~y450 is blank cream. Cause:
    the overview grid with one page, plus defect 4 (only one page
    exists because only two apps were ever seeded). Pixel's overview
    shows full-width page previews.
13. **Bottom actions crammed on one row.** "Add page" centred, then
    "Add contact / Contact to dock / Launcher settings" as three
    blue text buttons nearly touching on a single row at the very
    bottom. No "+ Add page" tile in the grid itself. Cause:
    Overview's bottom TextButton row, no spacing/padding between
    the three buttons.
14. **Page action icons are unexplained.** Under the card: a filled
    home icon, a filled eye icon, two greyed-out arrows — no labels,
    no indication of what they do or that the arrows are disabled
    (single page). Cause: OverviewActionsRow uses icon-only
    IconButtons; the move arrows are disabled on one page but still
    shown.

### D. Shot 3 — Settings: home grid (146907_2_3hlu.png)

15. **"Settings" title overlapped by the status bar.** Same cause
    as A1 (SettingsScreen header row, 8dp top padding only).
16. **Slider labels expose developer units.** "Grid margin (8dp)",
    "Icon spacing (8dp)", "Icon size (56dp)" — raw dp values in
    user-facing strings. Cause: SettingsScreen interpolates the dp
    values directly into the label. (Correction to the original
    suspicion: the slider VALUES are consistent with their labels —
    8dp on a 0–24 range sits at 1/3 and the thumbs sit at 1/3;
    56dp on a 40–72 range sits at 1/2 and its thumb sits at 1/2.
    The defect is the units in the words, not the positions.)
17. **Slider thumbs render as tall thin vertical bars, not round
    thumbs.** On all three sliders the thumb is a dark-blue vertical
    line ~3x the track height, crossing above and below the track.
    It reads as broken/unfinished vs. any stock slider. Cause NOT
    found in code: SliderRow uses a stock Material 3 Slider with no
    thumb customisation — needs a device/M3-version check rather
    than a code guess. (Material 3 1.4.0-alpha06.)

### E. Shot 4 — Settings: icons/behaviour (146908_3_tyo7.png)

18. **Plan/issue numbers in user-facing strings.** "Show music apps
    on headphone connect (#61)" (wrapping so "(#61)" sits alone on
    line 2) and "Add a home-screen sticker… (#58)". Cause: literal
    strings in SettingsScreen. A stock launcher never shows
    internal ticket numbers.
19. **Parenthetical developer jargon in labels.** "Themed icons
    (single-colour)", "Smartsize icons (normalised)", "One-handed
    layout (items in bottom third)". Cause: literal strings in
    SettingsScreen. The parentheticals read as implementation notes,
    not user language.
20. **"Add a home-screen sticker… (#58)" is a stray blue link, not
    a row.** Indented blue text with an ellipsis, no icon, no
    chevron, no row background — it looks like a hyperlink left in
    a settings list. Cause: SettingsScreen plain Text inside a
    TextButton-less row.

### F. Shot 5 — Settings: At a Glance (146909_4_nfeq.png)

21. **The "Behaviour" section header is clipped to a dotted
    remnant.** Just below the search box only the bottom edge of blue
    text is visible — a dashed blue line where a heading should be.
    Partly the scroll position (mid-scroll, no sticky headers), but
    it shows section context is lost the moment you scroll, and a
    half-visible heading reads as a rendering glitch. Cause:
    SettingsScreen is a plain LazyColumn with no sticky headers.
22. **Section title carries plan numbers.** "At a Glance (#10,
    #121)" in blue. Cause: SettingsScreen.
23. **Vendor sales-pitch in a toggle label.** "Weather (Open-Meteo —
    free, no key, no account)" — provider name plus a marketing
    aside, wrapping to two lines. Cause: SettingsScreen. Stock
    wording would be just "Weather".
24. **Weather is OFF with an empty city field, so At a Glance can
    never show weather.** Toggle grey/OFF; below it an empty
    outlined field reading "Weather city (e.g. Crittenden)" — the
    example is Robert's own local town baked into the UI. Cause:
    defaults `glanceWeather = false`, `weatherCityName = ""` + the
    field only shows a "Weather location:" line when a city is
    already set; otherwise a bare empty box.
25. **Three dead toggles plus a dead integration.** "Commute (no
    provider chosen yet)", "Finance (no provider chosen yet)",
    "Sports (no provider chosen yet)", "Smartspacer integration
    (protocol not bound yet)" — all OFF, all toggling features that
    do not exist yet. Cause: literal strings in SettingsScreen; the
    At a Glance header comment confirms slots with no provider
    "show NOTHING". A stock launcher hides unimplemented features
    instead of shipping their toggles with status reports.

### G. Shot 6 — Settings: wallpaper/widgets (146910_5_gthy.png)

26. **More plan numbers in section titles.** "Wallpaper & display
    (#53, #57, #77, #150–#154)" and "Widgets & feed (#6, #7, #38,
    #63, #120, #122)". Cause: SettingsScreen. (Repo-wide count: 20
    literal "…(#…)" strings in SettingsScreen.kt alone — lines 494,
    506, 509, 576, 613, 626, 635, 640, 645, 649, 685, 706, 1165,
    1170, 1188, 1193, 1209, 1217, 1222, 1245 — plus "(no provider
    chosen yet)" x3 and "(protocol not bound yet)" without numbers.)
27. **"Hide both bars" chip wraps to three lines and looks broken.**
    In the "Status / navigation bar control (#154)" row, "System
    default" and "Hide navigation bar" are single-line pills, but
    "Hide both bars" is a tall narrow box reading "Hide / both /
    bars" on three lines, ~3x the height of its siblings. Cause:
    ChipRow lays chips in a plain Row (no FlowRow, no wrapping, no
    horizontal scroll), so the third chip is squeezed into the
    remaining ~1/4-screen width and its label wraps.
28. **Phone-irrelevant foldable/tablet toggles are ON by default.**
    "Landscape home screen (#151)", "Separate layouts per screen
    (#150, foldable)" and "Two-pane tablet layout (#152)" all show
    blue/ON on a Pixel 10a — a phone with no foldable inner screen
    or tablet pane. Cause: defaults all `true` in LauncherModels;
    HomeScreen itself comments that the two-pane branch "never runs"
    on the Pixel 10a (smallest width ~412dp). Stock settings don't
    offer, let alone pre-enable, hardware the device doesn't have.
29. **More parenthetical feature-lists in labels.** "Wallpaper…
    (pick, scrolling, gradient, dimming)", "Hidden Desktop widget
    page (from the widget page)", "Widget overlap (items may overlap
    on the grid)". Cause: SettingsScreen. "Hidden" as an adjective
    in a user-facing label is itself a tell that the surface is a
    dev concept.
30. **Bottom row is sliced by the gesture navigation bar.** "Smart
    Stack auto-rotate" at the very bottom has its text struck
    through by the dark gesture pill and its toggle half cut off at
    the screen edge. Cause: A1's other half — the LazyColumn has no
    bottom contentPadding / navigationBarsPadding, so the last item
    scrolls underneath the gesture bar. (Same flaw will bite every
    screen's last row.)
31. **(Whole-app) There is no "default look" at all — every screen
    is the same flat cream.** Background #F8F1E7 + surfaces #EEE5D6
    on all six shots, no wallpaper, no colour, no imagery. Cause:
    stockLight ("warm paper — soft CRT cream") is the default theme,
    `wallpaperVisible=false` by default, and no default wallpaper or
    widget is ever placed. This is the direct visual basis for
    Robert's "doesn't look anything like a launcher that would
    actually come stock on a phone."

### H. Things that look RIGHT (do not break these in the fix)

- App icons render correctly: Settings (blue gear) and Play Store
  (correct colours, not tinted) draw crisply in the rounded-square
  shape, consistently on home, dock and overview thumbnails. The
  AppIcon pipeline is fine.
- Home-grid labels are fully readable ("Settings", "Play Store",
  12sp, not truncated) — only the DOCK copies clip.
- Calendar data is real and correct: "Thursday, November 26"
  matches the date, and "Black Friday · All day" is a genuine event
  line (the DATE + CALENDAR slots work).
- Search pill is correct: "Search apps" with magnifier, legible,
  sensibly placed at the bottom.
- Settings controls are internally consistent: toggles show ON=blue
  / OFF=grey reliably; selected chips are lavender; the slider
  VALUES match their labels (see defect 16 correction).
- Overview thumbnail uses real mini icons ("Page 1 • Home" is
  accurate), and the single page is correctly marked default/Home.
- No crashes or blank screens in any shot; every screen renders.

### I. Root cause of the barren home — package visibility (verified)

The manifest declares NO `QUERY_ALL_PACKAGES`, and its comment even
claims "no QUERY_ALL_PACKAGES needed for the default home app".
That is wrong for how Robert actually installed it: he opened Marlow
from his existing launcher without setting it as the default home,
so at seed time Marlow was an ordinary app. Since Android 11 (API
30), package visibility filtering hides every installed app except
the caller's own app and a small set of system-forced-visible
packages — `LauncherApps.getActivityList()` silently returns almost
nothing instead of failing. Verified against Android's own docs and
two independent launcher write-ups: a launcher must declare
`QUERY_ALL_PACKAGES` (a Play-approved core-functionality use for a
launcher) or the list is filtered. The two visible apps are exactly
the system-image launchable apps on a Pixel — Settings and Play
Store. Dialer, Messages, Chrome, Camera, Photos, Clock and Gmail
are user-updated Play apps, not system-image apps, so they were
invisible when `seedHome` ran. The seed itself is sound (it would
place up to 10 on page 0 and 5 in the dock from a full list) — it ran
on a 2-app list and never re-runs, so the home stayed barren even
after Marlow could see more. Fix: declare `QUERY_ALL_PACKAGES` AND
make the seed self-healing (one-time repair that tops the home up
from the full list without moving or removing anything the user
placed).

### J. 93-feature plan-vs-code audit

Read-only audit of the code as it stands (no device proof anywhere —
PRESENT means wired and reachable in code). Count check: the 93-number
list is exactly right against the audit section of
feature-decisions.md (the "Final batch — reasoning" section still
lists removed items as approved; the audit overruled them).
**Totals: 82 PRESENT · 11 HALF-DONE · 0 MISSING · 0 denied/removed
features built anyway.** (A sweep of 30 denied/removed identifiers
found zero code hits, even in comments — the removals were honoured.)

HALF-DONE (11), ranked by user impact:
1. **#96 App Pairs** — pair model + launch exist (second app with
   FLAG_ACTIVITY_LAUNCH_ADJACENT), but forcing a split needs the
   hidden setLaunchWindowingMode API (absent from android.jar API
   37); whether the Pixel splits is untestable and a failure is
   undetectable — both apps may just launch fullscreen, silently.
2. **#10 At a Glance** — commute/finance/sports are
   PlaceholderGlanceProvider returning emptyList; their Settings
   toggles exist and can be switched on, yet can never display
   anything.
3. **#38 Google feed** — RSS is real; the Google feed half is a
   status line only (GoogleFeedBridge contributes zero items).
4. **#121 Smartspacer** — toggle + slot exist; load() returns
   emptyList ("protocol not bound yet"). Turning it on changes
   nothing visible.
5. **#37 Now Brief** — sheet works (calendar + weather); health
   absent by design, reminders have no source (sheet says so),
   the morning alarm dies on reboot until the launcher next starts,
   and the notification opens the app, not the brief itself.
6. **#51 Blur glass** — "custom blur glass" is translucent surfaces
   only; true backdrop blur via RenderEffect is deferred, so panels
   over wallpaper don't look frosted.
7. **#5 One-handed** — only a 0.5-weight spacer above the grid;
   content shifts down, nothing is actually placed in the bottom
   third and most-used items are not reordered there. The code
   comment admits this is an interpretation.
8. **#95 Pinned contacts in folders** — home + dock work; folders
   do not (HomeFolder.apps is component names only, a contact has
   no component) — not implemented by design.
9. **#94 Pinned shortcuts** — works, but only via the long-press
   sheet row; drag-out of the sheet onto the home is not
   implemented (feature text says "dragged to home screen").
10. **#25 Animations** — no animation setting exists anywhere; the
    only transitions are Compose framework defaults.
11. **#176 Lightweight budget** — goal, not requirement, and NOT
    met: run-15 debug APK is 62,048,681 bytes vs the <30 MB target
    (classes.dex 44.6 MB); RAM unmeasured. Done so far: one unused
    dependency removed, icon cache bounded at 256. No minification
    lever pulled (that is a release-build lever, not a debug one).

Rough edges (a)–(o) re-checked: (a) App Pairs public-API-only,
confirmed; (b) #97 screen time opens system usage-access settings,
confirmed; (c) #95 contacts in folders not implemented by design,
confirmed; (d) #80/#83 usage data is the launcher's own launch
history, confirmed; (e) default-page edge case FIXED in the finish
job (visibleFullIndices + delete/move remap); (f) first-run seeding
curated (dialer, messaging, browser, camera, photos, clock, Gmail,
settings, Play Store first), confirmed; (g) Google feed status-only,
confirmed; (h) Smartspacer stub, confirmed; (i) commute/finance/
sports placeholders, confirmed (weather now real via Open-Meteo);
(j) Now Brief reminders no source, confirmed (sheet says so);
(k) settings-search jump shows only that area + "Show all
settings", confirmed; (l) reset keeps vault PIN + hidden apps +
wizard-completed, clear-all wipes everything, confirmed;
(m) hidden Desktop widget page entry is the "Desktop widgets"
button on the widget page behind the OFF-by-default toggle,
confirmed; (n) #25 framework defaults only, confirmed; (o) #96
split failure undetectable, confirmed.

Log drift found by this audit (not a code defect): BUILD_LOG says
the settings-search index has 74 entries; the code has 85
(SettingsScreen.kt:97-182). Corrected in the fix record below.

---
# AUDIT FIX — 2026-11-26 (merged, kotlinc-check PASS)

Three fix branches, all from main 7533331, all kotlinc-check PASS in
their worktrees, merged in order home -> settings -> features:

- auditfix/home 19f6653 — default home, app visibility, insets, dock,
  clock, weather, glance (18 files, +554/-134)
- auditfix/settings c5438eb — settings strings, chips, overview,
  device-gated rows (7 files, +355/-126)
- auditfix/features 0ba05f8 — App Pairs honesty, Now Brief boot +
  deep link, blur glass, one-handed, contacts in folders, Google
  feed wording (13 files, +493/-110)

Merge conflicts: exactly one, in LauncherModels.kt (the glance-weather
comment — home said ON by default since the audit fix, settings said
OFF by default). Resolved to the home wording, which matches the
actual default (glanceWeather = true), keeping the settings branch's
note that the commute/finance/sports rows are hidden until a provider
exists. Everything else merged cleanly. FeedPage.kt (home added a
topInset param, features removed the bridge params), HomeScreen.kt,
MainActivity.kt, HomeViewModel.kt, LauncherStore.kt and
LauncherModels.kt all landed without further conflict.

Coordinator's merge-time addition: the weather-row jump-to-area. The
settings branch added an `initialArea` param to SettingsScreen but
could not wire the HomeScreen call site (the other builder's file).
Wired at merge: HomeScreen keeps a `settingsInitialArea` state, the
At a Glance "Add weather" row sets it to SettingsArea.HOME before
opening Settings, and dismiss clears it. Other Settings openings pass
null and land on the full list, as before.

Log drift corrected: the settings-search index is 85 entries, not 74
(line ~252 above fixed).

## What changed, by defect

Default look (the headline):
- QUERY_ALL_PACKAGES declared; the manifest comment claiming it was
  not needed was wrong — it was wrong exactly for how Robert installed
  it (opened from his existing launcher without setting Marlow as
  default, so at seed time Marlow was an ordinary app and Android 11
  package-visibility filtering hid everything but Settings and Play
  Store). Seed is self-healing: repairHomeSeedIfNeeded tops page 0 +
  dock up to 5 from the shared curated order, adds the clock, never
  moves anything placed, runs at most once (seed_repaired flag).
- Dock no longer duplicates the grid: page 0 = ordered.drop(5); the
  repair removes any page-0 copy of a dock app, keeping the dock copy.
- Real CLOCK builtin widget seeded at the top of page 0; liveClockIcon
  ON by default; clock widget is big live time (32sp, ticks every
  second) + date.
- Weather ON by default for fresh installs + one-time migration for
  existing installs (glance_weather_merged flag; a user who deliberately
  turned weather off pre-fix gets it back on once and can turn it off
  again). "Add weather for your city" row on the strip when weather is
  on and no city is set; it jumps straight to the At a Glance area.
- Default home background is a code-drawn subtle warm gradient when no
  wallpaper is set and the theme is Stock or Ambient (light #EFE4CF /
  #F8F1E7, dark #1C1610 / #262019). Material You keeps its flat dynamic
  background. This is the theme's default backdrop, not a wallpaper
  source — no asset, no collection, no picker entry (plan #53).
- Dock clipping fixed: row height is now max(slider value, icon +
  label + padding) — 82dp at defaults vs the old fixed 72dp — and the
  dock + search pills got a 1dp outlineVariant border so they read
  against the cream. No background/surface colours changed.
- Page indicator now counts the real pager (feed + home + widget).
- Drawer FAB removed (swipe-up is the stock gesture, already
  implemented); startup gate test now asserts the At a Glance strip.
- Media-player permission hint removed from home (the bar shows only
  while something is playing); the grant stays reachable via the
  notification panel, the wizard, and the new Settings -> Notifications
  listener row.
- First-run wizard: "Set Marlow as your home app" is now the first row
  of the Optional grants step (RoleManager ROLE_HOME on API 29+,
  ACTION_HOME_SETTINGS fallback); intro corrected from three items to
  four.

Insets / status bar (defects A1, A2, 11, 15, 30):
- statusBarsPadding on the home Column, Drawer, UniversalSearch,
  DesktopWidgetPageScreen, SettingsScreen top-level Surface and
  Overview; navigationBarsPadding on the dock/search-pill column,
  NotificationPanel, VaultSheet, the Settings LazyColumn (with 24dp
  bottom contentPadding) and Overview. FeedPage/WidgetPage take a
  topInset param; the home pager's copies pass false (they sit inside
  the already-padded home Column — an unconditional inset would have
  doubled the gap), two-pane copies keep it.
- Light/dark bar icons follow the theme (isAppearanceLightStatusBars /
  NavigationBars = !darkTheme in MainActivity), so status-bar icons no
  longer sit white on pale cream.

Settings strings (defects 16, 18, 19, 20, 22, 23, 24, 25, 26, 29):
- All 20 "(#...)" literals gone from SettingsScreen.kt; units stripped
  from slider labels ("Grid margin (8dp)" -> "Grid margin", etc.);
  vendor pitch gone ("Weather (Open-Meteo — free, no key, no account)"
  -> "Weather"); city field is "Weather city", placeholder "Search
  for your city" (no town name baked in). Plan numbers survive only in
  code comments.
- Dead features hidden: Commute, Finance, Sports and Smartspacer rows
  removed (approved-but-unprovided, no provider; slot machinery and
  fields stay, rows come back when a provider lands). Their search
  entries removed too.
- Foldable/tablet rows device-gated: defaults OFF in LauncherModels,
  rows hidden on phones (smallestScreenWidthDp < 600), and
  LauncherStore.gateForDevice forces both flags OFF in the returned
  settings on a narrow device without overwriting stored values.
  "Landscape home screen" stays ungated.
- ChipRow is now a FlowRow (the "Hide both bars" chip no longer wraps
  to three lines); also benefits FirstRunWizard, which shares ChipRow.
- Sticky section headers (18) with a background fill so scrolled rows
  do not show through; DebugScreen text left alone.
- Overview: every page-action icon now has a small visible label
  (Home / Hide-or-Show / Move left / Move right / Delete); bottom
  action row spaced 16dp; "+ Add page" is a card in the grid itself;
  a single page spans full width (GridCells.Fixed(1)) instead of a
  tiny 2-column card.
- Slider thumb: M3 1.4.0-alpha06's default thumb is the Expressive
  vertical bar (verified with javap on the classes.jar); SliderRow now
  supplies a 20dp primary-coloured circle via the thumb slot. No M3
  downgrade. Code-verified only — the look on a real device is
  Robert's check.
- "Add a home-screen sticker..." is a proper row with a trailing
  chevron, same action.
- Judgment calls (settings builder, accepted at merge): parentheticals
  that are plain user explanations were kept — "Themed icons
  (single-colour)", "Smartsize icons (normalised)", "One-handed layout
  (items in bottom third)", "Hide status bar (ultra-immersive)",
  "AMOLED black (pure-black dark background)", "Notification badges
  (dots / numbers)", "Google feed bridge (unofficial, may break —
  RSS-only when unavailable)" (a real caveat), "Search URL, with %s
  for the query" (the functional instruction). "Hidden apps vault" kept
  — "Hidden apps" is the feature's own name. The (07:00) time in the
  morning-summary slider labels kept — a user-meaningful current
  value, not a unit.

Feature gaps (half-done list):
- #96 App Pairs: honest labelling only. The picker now says "If the
  system doesn't split them, both apps just open normally." No split
  detection was invented — no public API can force or detect it.
- #37 Now Brief: alarm setup moved into shared
  NowBriefReceiver.schedule(); a new NowBriefBootReceiver
  (RECEIVE_BOOT_COMPLETED) re-schedules on reboot. The notification
  carries EXTRA_OPEN_NOW_BRIEF; MainActivity reads it in onCreate and
  onNewIntent and HomeScreen opens the brief sheet once. Reminders
  still have no source and health is still absent — neither added.
- #51 Blur glass: the prescribed RenderEffect was NOT landed — the
  wallpaper layer in HomeScreen is only the dim/gradient overlay; the
  real wallpaper is the system wallpaper behind the transparent window,
  which a layer effect cannot touch (blurring the overlay would be a
  placebo). What landed: Window.setBackgroundBlurRadius(32) when the
  surface style is BLUR_GLASS and a wallpaper is visible, else 0 —
  genuinely blurs the system wallpaper. Guarded three ways (SDK >= S,
  try/catch, only called when the desired radius changes); a plain
  launch never calls it. Radius 32 (range 0-150) is a judgment call,
  untested on a real device — Robert's eye is the real check.
- #5 One-handed: the grid now shifts every item down so the lowest
  occupied row sits on the pager's bottom edge when one-handed is on
  (applied to rendered positions and hit-test rects); the 0.5-weight
  spacer stays. Shift is 0 when off.
- #95 Pinned contacts in folders: done fully, including backup.
  HomeFolder gains contacts (FolderContact uri+name); the store record
  gains a 4th field (flat uri+name pairs, old 3-field records load
  with empty contacts); backup carries the same field, BACKUP_VERSION
  stays 1. Dissolve now counts apps + contacts together (>=2 members
  just loses the member; 1 left dissolves into that member's tile).
  This also fixes a latent bug: a folder with 1 app + 1 contact losing
  the app used to dissolve into the app and silently drop the contact.
  Folder dialog renders contacts in the same grid; long-press sheet
  gains "Add to folder..." for contacts. One deliberate behaviour
  change: a folder emptied of apps used to become an app tile with a
  null component; it now drops the tile.
- #38 Google feed: status line now says plainly "Google feed isn't
  available in this launcher — add RSS sources below." No integration
  attempted (fragile unofficial internals — Robert's call). The bridge
  object and toggle stay in place as the documented record of why.
- Left as-is, confirmed: #94 no drag-out (long-press sheet row is the
  pinned-shortcut path); #25 no animation work (framework-default
  transitions stand — tuning was audit-removed); #176 no build-config
  or size work (the budget is a release-build lever with minification,
  not this debug APK).

Verification on the merged tree: ./tools/kotlinc-check.sh PASS
(47 source files). Push + CI + APK verification follow below.

## Coordinator decisions (recorded here with reasoning)

- Weather ON by default + one-time migration: leaving Robert with no
  weather after he complained about it is worse than the one-time
  re-enable for a user who deliberately turned it off pre-fix; that
  user can turn it off again.
- Strip all (#NN)/asides/dp-units/vendor text from user-facing strings;
  numbers stay in code comments only. (The settings builder's kept
  explanatory parentheticals above were accepted at merge.)
- Hide unimplemented commute/finance/sports/Smartspacer rows: the
  features stay approved-but-unprovided; the UI stops advertising them
  until a provider exists.
- Foldable/tablet rows hidden on phones + defaults OFF + behaviour
  gated on device width.
- Drawer FAB removed; media hint removed from home (grant reachable
  via notification panel, wizard, Settings -> Notifications row).
- Dock labels fixed by content-driven height + 1dp outlineVariant
  border; Robert's background/surface/surfaceVariant colours NOT
  changed.
- Page indicator reflects the real pager; default home background is
  a code-drawn subtle warm gradient (theme backdrop, not a wallpaper
  source, per plan #53).
- Clock = liveClockIcon ON + a real CLOCK builtin widget seeded at the
  top of page 0; dock apps excluded from page 0.
- #94 drag-out NOT built; #25 animation surface NOT built; #176 NOT
  chased in the debug APK; #96 split failure handled by honest
  labelling only; #38 Google integration NOT attempted.

## Verification — audit-fix build (run 45882611479, GREEN)

- Push: local HEAD 1101216 -> remote commit
  95ae93095757c9ea4b9073ecefc5dc19cae6cd12, message verified on the
  remote ("Audit fix: default home, insets, settings strings, feature
  gaps (no version bump)") — not 'Phosphor source'.
- CI run 45882611479: completed success, first try (bounded waiter,
  ~2 min in progress). Dex guard, Robolectric startup gate and
  Manifest guard all intact and passing; none was touched by the fix.
- Artifact launcher-debug-apk id 15148101305: zip 16,429,018 bytes
  (exact match with the GitHub listing), real Zip, contains
  app-debug.apk.
- APK verified from Pip's own download:
  marlow-launcher-0.1.0-dev-auditfix.apk, 62,147,097 bytes, SHA-256
  76ffc03644b1944761b8a8514034eb37a153704fab4b7ddf1004edfb2f96580f.
  Package app.launcher, version_name 0.1.0-dev, version_code 1,
  minSdk 29, targetSdk 35, compileSdk 37, label "Marlow Launcher".
  v2-signed (v3 false, as with the earlier debug builds), signer
  CN=Android Debug, O=Android, C=US, serial 1, cert SHA-256
  AF488418554E1AB69E51CCF74FA0952AEBD4CA686AAE7164D7D2746DEDACEEAB.
  get_main_activity returns None, as expected — the filter is HOME,
  not LAUNCHER; MainActivity is present.
- Audit-fix markers present in the merged manifest/dex:
  QUERY_ALL_PACKAGES, RECEIVE_BOOT_COMPLETED, POST_NOTIFICATIONS;
  NowBriefBootReceiver declared; dex strings repairHomeSeedIfNeeded
  (16), seed_repaired (1), glance_weather_migrated (1),
  setBackgroundBlurRadius (1), openNowBrief (5), FolderContact (7),
  NowBriefBootReceiver (4).
- Icon v3 confirmed as the shipped icon: the compiled
  ic_launcher_foreground.xml contains all four distinctive v3
  colours (panel #0E0B08, warm rim #57493B, dock pill paper #F8F1E7,
  phosphor blue #5555FF).
- IMPORTANT install note: this build's debug signer cert
  (AF488418...) differs from the finish build's (5214b884...) —
  each CI runner generates its own debug keystore, so the signature
  changes per run. Robert must UNINSTALL the finish build before
  installing this one; an install-over will fail on the signature
  mismatch. (Same caveat applies to every future CI debug build.)
- Size: 62,147,097 bytes vs 62,048,681 for the finish build (+98 KB).
  The #176 <30 MB lightweight budget is still not met — it is a
  release-build lever (minification), not a debug-APK target.
- Worktrees auditfix/home, auditfix/settings, auditfix/features
  removed after the green push; branches deleted. Local main is the
  single source of truth again.
- Delivered to Robert via Muse storage (expires 2026-11-28T19:25:12Z):
  https://muse.ai/files/706211062566888/4665780427042882/0yzp6wjpjtug3av38qw60r10/marlow-launcher-0.1.0-dev-auditfix.apk
  plus the GitHub run link
  https://github.com/DocBobert6/MarlowLauncher/actions/runs/45882611479
- Still device-gated (his eye, not verifiable here): the default
  home actually looks stock on his Pixel 10a (seed repair fills page
  0 + dock once the launcher can see all apps — set Marlow as
  default home, or open it once as default so the repair runs);
  clock widget + weather strip rendering; slider thumb roundness;
  blur-glass radius 32 on the real wallpaper; dock/search pill
  contrast; the warm gradient backdrop; the At a Glance strip no
  longer colliding with the status bar.

## REBUILD TO RESEARCH STANDARD (Phase 3, 2026-11-26)
Rebuilt the app against DESIGN-RESEARCH.md (72 rules, 12 principles,
six research tracks in design-research/; no fabricated credentials
anywhere) and the re-audit's 25 rebuild decisions (section F), plus
the parent's four rulings: grid default 5x5 (5x6 + Fluid kept as
non-default choices); drawer grid + alphabetical default (categories,
custom order, drawer folders kept as opt-in extensions); badges
dot-only on icons (numeric counts kept in the long-press menu);
long-press sheet fixed order by default (user-reordering kept as a
setting, so #173 still exists). Robert's decisions kept intact
throughout: name Marlow, P22 accent values (#3333CC light / #5555FF
dark), warm shading values, halo only on the page dot + resize
handle, icon C v3. Version stays 0.1.0-dev (code 1). Dex guard,
Robolectric startup gate and Manifest guard untouched and still
enforced by CI. Per-rule record: rebuild-notes-theme/home/drawer/
settings/integration/motion/held-items.md (7 files, in the repo).

Wave 1 — four parallel streams, each on its own branch, merged
clean into rebuild/merge (zero conflicts, disjoint file sets):
- Theme (84ae2f3): Stock scheme rebuilt from the P22 seed via HCT
  (tools/generate_stock_scheme.py, official Python
  material-color-utilities, tonal-spot, 12 schemes, idempotent);
  dynamic colour on by default in the stream; accent picker
  redefined as seed choice; warm neutrals from locked backgrounds;
  container ladder (light 2 steps, dark 5 apart); worst text pair
  5.19:1 (light primary/on-primary 8.96:1, up from 4.51:1). Known
  deviation: Ambient still hand-pairs primary/onPrimary (needs a
  runtime material-color-utilities dep). Dark Blue primary is now
  pipeline tone-80 0xFFC1C1FF, not painted #5555FF — device check
  owed.
- Home (c54fd08, 17 files +1938/-494): At a Glance text-on-wallpaper
  + carousel + split taps; Pixel grid set + migration with notice;
  luminance-aware scrim; clock widget time-only; dock surface
  colours; mic in search pill, Lens omitted (no API); continuous
  parallax; feed personal-context-first; circular folder tiles,
  drag-to-folder, 800 ms hover-open, task-based names; drag system
  (650 ms hover, 1.08x spring, source hole, edge scroll,
  drag-to-top Remove/Uninstall, system 300 ms double-tap);
  proportional icon radii; monochrome-only themed icons + Marlow's
  own monochrome layer; dot-only badges (6dp, top-trailing, clear
  on open, per-app off, folder dots); empty-space long-press menu;
  calendar/contacts auto-fires removed; drawer query cleared on
  close; real Overview previews.
- Drawer/widgets/folders (0d6c828, 11 files +1734/-491):
  alphabetical grid drawer with ragged section-merge + 48dp
  drag-strip fast scroller; swipe-down close; frequent row labelled
  "Based on apps opened on this device"; floating folder overlay
  4x3 paginated, tap-to-rename; platform widget sizing table
  (70n-30), host rounding/padding, four resize handles honouring
  resizeMode, 66% snap threshold; picker rebuilt (real previews,
  search, alphabetical, in-place reconfigure); stacking labelled a
  Marlow extension, same-footprint, reset on Home.
- Settings/back/a11y (530a100 + c0bc4c0, 8 files +3473/-1207):
  seven-section IA (<=15 rows/screen), value subtext on every row,
  parentheticals removed, radio-on-sub-screen (25 choice keys),
  deep-link search landing on the row with highlight, fixed
  long-press order (notifications with count header, <=4 circular
  48dp shortcuts, App info + Widgets entry, reordering kept as a
  setting, drag-from-sheet NOT built — recorded, picker kept),
  BackHandler in Settings/Debug, PermissionPrimingDialog, 840dp
  breakpoint, area-based orientation, TalkBack helpers, backup
  restore-scope docs (third-party widgets unrestorable, Nova import
  NOT supported).

Integration (fa4930f, 15 files +955/-83): LauncherModels defaults
(theme MATERIAL_YOU at that point, DrawerStyle GRID, swipe-up-to-
search + drawer extensions off, badge positions restricted to top
corners, dockShadow removed entirely incl. persistence, badgeOff-
Packages persistence); BackHandler per overlay; onOpenWidgets
wiring; TalkBack semantics + customActions (no Move action — no
drag entry point exists, recorded as a gap, not faked); widget
picker/configure/resize/stack wiring with visible refusal errors;
folder focus return; calendar priming; per-app badge switch in the
long-press sheet; accent picker gated to Stock. kotlinc-check green
on the merged tree (47 files, 0 errors, 0 warnings).

Held items — Robert's ruling 2026-11-26 17:06 EST ("Recommendations
learned from audit"): follow the audit on both.
- Wizard home-first (F2) + dock exactly five (F5), commit 698aa96
  (16 files +758/-717), merged as c58f613: FirstRunWizard.kt
  deleted; ChipRow deleted (wizard-only); completeWizard() and
  wizardCompleted deleted; startup gate test is now
  firstLaunchShowsHome (Search pill + At a Glance shown, no
  "Welcome"/"Skip setup" node). Only first-run prompt is the single
  system default-home prompt (existing RoleManager /
  ACTION_HOME_SETTINGS flow, one-time flag
  default_home_prompt_shown, set before firing, never repeats on
  decline/reset/recreation, not in backups). Dock: DOCK_MAX_ITEMS
  = 5, single row, no pager/indicator, labels off on every tile
  kind; seed by kind Phone / Messages / browser / mail / Camera,
  resolving each device default via resolveActivity with curated
  package fallbacks; a sixth add is refused with a visible error
  (dockAddError snackbar, inline sheet notice, up-front overview
  refusal), never silently dropped; normalizeDock caps stored /
  restored lists (extras to free page-0 cells); search pill stays a
  separate element below the dock and does not count.
  dockPage / dockIndicatorStyle persistence removed everywhere
  (Dock "Page indicators" row gone, DockPagePickerDialog deleted).
  HomeItem.dockPage record slot removed (19-field records now;
  both loaders accept pre-rebuild 20-field records via size-based
  offset).

Motion + haptics (5d4ed89, 12 files / 916 insertions, merged as
80618a6): new ui/Motion.kt — single source of truth, emphasized
easing (400 ms decelerate in / 200 ms accelerate out), springs for
drags/sheets, every §4.5 duration; grep-verified no tween(/spring(
literals remain outside it. New ui/Haptics.kt — rule 59 map
(MarlowHaptics + LocalMarlowHaptics). Hero moments: (1) app launch
— system icon-anchored growth, Marlow layers nothing on it
(rule 57); (2) folder open — container transform, 400 ms drop-in
+ 350 ms contents with 45 ms stagger. Predictive back:
PredictiveBackHandler for drawer and folder (surface scales to 90%
with the swipe, cancelling springs back with no state change);
manifest enableOnBackInvokedCallback="true" added explicitly;
sheets keep M3's own preview. Reduced motion central in Motion.kt
(transforms instant, fades 100 ms crossfade, parallax off,
haptics kept). Deviations: no haptics setting existed, so
settings.hapticsEnabled was created, OFF by default, persisted,
with a row in Home settings -> Gestures. Compose UI 1.7.6 exposes
only two haptic constants (LongPress click, TextHandleMove tick) —
the seven-effect vocabulary needs Compose UI 1.8.0; the map
preserves the click/tick distinction with what exists, upgrade path
recorded in rebuild-notes-motion.md. Specified but not wired:
empty-page snap-off/fade (pager exposes no removal hook),
begin-and-end/utility/fade-through specs, settings x-axis
transitions — all in Motion.kt for the next wave, not invented.

Theme-default tension, resolved (ada59ac): the research doc's
header lock says default theme Stock (Material You one tap away),
while §4.1 + rule 3 say dynamic colour on by default; the theme
stream followed rule 3 (default MATERIAL_YOU). Robert's original
decision was Stock default, and the coordinator brief lists Stock
as a decision to keep intact — so the one-line LauncherModels
default was reverted to STOCK (ada59ac). The HCT Stock scheme and
all theme-stream work stand; only the default flipped. Material
You remains one tap away in Settings.

Final merged tree (rebuild/merge @ ada59ac): kotlinc-check PASS,
48 source files, 0 errors, 0 warnings. Icon v3 re-verified in the
merged tree: the compiled ic_launcher_foreground.xml contains all
four distinctive v3 colours (panel #0E0B08, warm rim #57493B,
dock pill paper #F8F1E7, phosphor blue #5555FF). Version still
0.1.0-dev (code 1). The two builders shared one working copy, so
each committed only its own hunks (hunk-splitter + content
filtering); both branches verified clean of foreign hunks before
merging. Merges into rebuild/merge were conflict-free.
