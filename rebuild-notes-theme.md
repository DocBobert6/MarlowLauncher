# Rebuild notes — THEME stream (Phase 3, wave 1)

- Branch: `rebuild/theme` (worktree `~/workspace/launcher-theme`)
- Date: 2026-11-26
- Scope: `app/src/main/java/app/launcher/ui/theme/Theme.kt`,
  `tools/generate_stock_scheme.py` (new), this file. No other file touched.
  No push made. Version untouched (0.1.0-dev, code 1).
- Verification: `tools/kotlinc-check.sh` green (47 source files).
  No credentials, employment history, or education claims anywhere here
  or in the code; the work stands on DESIGN-RESEARCH.md only.

## What changed, per rule

### Rule 4 — Stock scheme generated from one seed via HCT (was FAIL)
- Deleted the hand-written `stockLight` / `stockDark` (`lightColorScheme` /
  `darkColorScheme` with ~8 roles each, `surface-container-*` never set).
- New `tools/generate_stock_scheme.py` regenerates the Stock scheme from
  one seed per accent through the official HCT pipeline
  (`material-color-utilities`, Python package, tonal-spot variant,
  standard contrast), light + dark exported, all 36 `ColorScheme` roles.
- `material-color-utilities` is NOT in the offline Gradle cache
  (`~/workspace/.gradle-launcher/caches/` has no such artifact), so the
  offline-generation path was used, per the brief: the generated Kotlin
  is checked in (Theme.kt, between `// BEGIN/END GENERATED STOCK SCHEMES`
  markers) and the generation step is documented and re-runnable —
  change a seed in the script, re-run it, the whole scheme regenerates.
  Verified idempotent: re-running the script produces zero diff.
- The generator also measures WCAG contrast for every key pair and fails
  loudly on any text pair < 4.5:1 or boundary pair < 3:1 (see ratios below).

### Rule 3 — Dynamic colour on by default, Stock fallback (was FAIL)
- `LauncherTheme`'s default `theme` parameter is now
  `LauncherThemeChoice.MATERIAL_YOU` (was `STOCK`).
- The `LauncherModels.kt` defaults change this needs is listed below
  (not edited here — another file). Behaviour after it: API 31+ gets the
  system dynamic scheme by default; below API 31 the Stock scheme (from
  the same accent seed) is the fallback, as the MATERIAL_YOU branch
  already falls back that way.
- TENSION FLAGGED for the coordinator: DESIGN-RESEARCH.md's header lock
  says "default theme Stock (Material You one tap away)", while §4.1 and
  rule 3 say dynamic colour is on by default. This stream followed rule 3
  per the brief. Robert / coordinator to confirm the header lock is
  superseded; if not, the one-line LauncherModels default below is the
  only thing to revert.

### Rule 5 — Swap the scheme, never individual colours (was FAIL)
- Deleted the MATERIAL_YOU accent override (old Theme.kt:202-216, which
  overwrote the dynamic primary with the accent picker's fixed colour
  and forced `onPrimary = White`). The MATERIAL_YOU branch now uses the
  dynamic scheme exactly as the system derives it from the wallpaper,
  with no override of any kind. The wallpaper-derived primary now
  reaches the UI.
- Accent picker redefined as a SEED choice: each `AccentColour` is a
  seed; the generator emits a full light+dark scheme per accent (6
  accents × 2 modes = 12 generated schemes), and `stockScheme(accent,
  darkTheme)` selects the whole scheme. Under Material You the accent
  picker does nothing by design — the wallpaper is the seed (Settings
  follow-up listed under patch requests).

### Rule 2 — Every colour paired with its `on-` role (was PARTIAL)
- Fixed by construction: every `on-` role comes from the HCT pipeline
  (fixed tones of the same palette), so no pairing is hand-picked.
  Gone: dark stock `onPrimary = 0xFF10243E`, the AMBIENT luminance
  threshold (see known deviation below), the MATERIAL_YOU forced white.
- Verified after the rebuild: outside the generated region, the only raw
  hex literals left in Theme.kt are the two home-backdrop edge constants
  (rule 1, documented) and the AMBIENT `onPrimary` threshold (deviation).
  All 435 `Color(0xFF…)` literals in the file sit inside the generated
  region, i.e. they are pipeline output, not hand-set values.

### Rule 6 — Container steps respected (was FAIL)
- All `surface-container-*` roles now exist with warm values. Default
  accent (Blue): light container `0xFFF5EDDF` (tone 94), container-high
  `0xFFEFE7DA` (tone 92) — two tone steps apart, more than one step as
  the rule requires; dark container `0xFF29231B` (tone 14),
  container-high `0xFF342D25` (tone 19) — five tones apart. Full ladder:
  light lowest `0xFFFFFFFF` / low `0xFFFBF3E5` / container / high /
  highest `0xFFE9E2D4`; dark lowest `0xFF130D07` / low `0xFF211B13` /
  container / high / highest `0xFF3F3830`.
- Dock (`surface-container`) vs search bar (`surface-container-high`)
  application is in Dock.kt — patch request below; the theme side (the
  roles existing) is done.

### Rule 7 — Contrast ≥ 4.5:1 text / ≥ 3:1 non-text (was PARTIAL)
- Measured by the generator for all 12 schemes (full table for the
  default Blue accent below; all accents pass). Worst text pair across
  all 12 schemes is 5.19:1 (`on_error_container` on `error_container`,
  dark) — above 4.5:1. `outline` on `surface` is 4.02:1 light /
  5.11:1 dark (≥ 3:1). `outline_variant` is 1.52:1 light / 1.72:1 dark:
  that is M3's own baseline for a decorative divider and carries no
  3:1 requirement — the generator reports it as info, never fails on it.
- Not measured here (device-gated): label-over-wallpaper contrast
  (rule 13) and the dynamic scheme's own pairs (system-derived, varies
  with wallpaper) — both need a device.

### Rule 8 — Error only for errors; halo never the sole carrier
- Halo: untouched. `phosphorGlow` is still used ONLY on the
  page-indicator dot (`HomeGrid.kt:472`) and the widget resize handle
  (`Widgets.kt:187`); its definition in Theme.kt is unchanged. No halo
  added anywhere, none removed.
- Error-role-as-badge-colour (`Gestures.kt:261,267`) is untouched — it
  is the rule-49 contradiction the audit already flagged, not this
  stream's file.

### Rule 15 — Shape discipline (was FAIL on the theme side)
- Theme `Shapes` are now stated explicitly as the M3 defaults:
  extra-small 4dp, small 8dp, medium 12dp, large 16dp, extra-large 28dp
  (`MarlowShapes`, passed to `MaterialTheme`). Pills are `full` at the
  component level (M3 button/chip default) — no theme-level pill
  override exists or is needed.
- Shape overrides recorded: NONE. The non-table radii the audit found
  (At a Glance 16dp, media bar 20dp, dock 24dp, search pill 28dp,
  icon 28dp) live in other streams' files; this stream records no
  exception for any of them.

### Rule 17 — Depth is tonal, not shadow (was FAIL on the theme side)
- Depth now comes from the container ladder (rule 6 above). No
  shadow-based depth is defined in this theme: `phosphorGlow` is the
  halo (rule 8), not depth, and the theme defines no elevation.
- The dock `shadow(8.dp)` and the `dockShadow` toggle live in Dock.kt /
  LauncherModels.kt — patch request below; the toggle itself does not
  live in Theme.kt.

### Rule 1 — No raw hex outside the theme (was FAIL)
- Theme side done: all hex lives in Theme.kt. Two additions for the
  other stream's file: `HomeBackdropEdgeLight = 0xFFEFE4CF` and
  `HomeBackdropEdgeDark = 0xFF1C1610` are defined here (the audit-fix
  warm gradient edge values) so HomeScreen.kt can stop inlining them —
  patch request below.
- `surfaceColorFor` is unchanged (returns `surfaceVariant`, a role).
  Note for the Dock stream: it currently maps dock + search pill to the
  same `surfaceVariant`; the role-based fix is the patch request below.

### Rule 9 / 10 — Type scale; 12sp label role
- Confirmed, not changed: `MaterialTheme.typography` already exposes
  the M3 type scale, and the 12sp label roles exist — verified
  empirically against the actual material3 1.4.0-alpha06 classes with a
  kotlinc spot-check: `labelMedium = 12.0.sp`, `bodySmall = 12.0.sp`,
  `labelSmall = 11.0.sp` (the 11sp floor). No custom sizes added.
  Font choice still applies one family to every style.
- The hard-coded `.sp` literals at call sites (audit rule 9 FAIL) are
  in other streams' files, not the theme's.

## Decisions with reasoning

1. **Warm neutrals: per-mode palettes from the locked backgrounds.**
   Each mode's neutral / neutral-variant palettes are built from that
   mode's locked background colour (light hue 93.41, dark hue 73.04 —
   the two locked colours do not share a hue, so one shared hue would
   make one mode's surfaces mismatch its own background), with the
   pipeline's own chroma convention (neutral 6, neutral-variant 8,
   read off the tonal-spot pipeline's output for the P22 seed).
   Background and surface are pinned to the locked values
   (`#F8F1E7` / `#262019`); every other neutral role follows the M3
   tone mapping from those palettes. This is a project decision
   overriding the seed-generated neutrals, applied in the generator —
   not a hand-tweak in Theme.kt.
2. **Dark container tones shifted +2 (12/17/22 → 14/19/24).** The
   locked dark background is tone ~12.7, so the M3 baseline container
   tone 12 would sit (invisibly) *below* the surface and the dock
   would not read as a step above it. The +2 shift keeps the ladder
   monotonic above the pinned surface while preserving the 5-tone
   container→container-high gap. Systematic shift, documented in the
   script header — not a per-role tweak.
3. **AMOLED black overrides background only.** #24 defines AMOLED black
   as a pure-black *background*; the old code also overrode surface /
   surfaceVariant with hand-picked near-blacks. The new code copies
   only `background = Black`, leaving the generated dark surfaces to
   stand above it. This keeps the warm dark surfaces and the container
   ladder intact in AMOLED mode.
4. **All six accents get full generated schemes, not just Blue.**
   The accent picker is a seed choice, so every option must regenerate
   the whole scheme — generating only the default would have left the
   other five accents on the old hand-written path.
5. **AccentColour enum values unchanged.** The enum's light/dark ARGB
   pairs already ARE the per-mode seeds; no value change was needed.
   The generator's ACCENTS table mirrors the enum and the script header
   says the two must change together. (The enum fields are now only
   read by the AMBIENT fallback in Theme.kt; that is fine — they
   document the seeds.)
6. **LauncherTheme's own default `theme` parameter → MATERIAL_YOU.**
   MainActivity passes `settings.launcherTheme` explicitly, so no
   caller relies on the composable default; aligning it with the
   rule-3 default keeps the two defaults from contradicting each other.

## KNOWN DEVIATION — Ambient theme (rules 2, 5)
The AMBIENT branch still replaces `primary` with the wallpaper colour
and derives `onPrimary` from a luminance threshold. That is a
hand-picked pairing, not a pipeline derivation. The correct fix is
generating the scheme from the wallpaper colour as a seed **at
runtime**, which needs the `material-color-utilities` artifact as a
runtime dependency — it is not in the offline Gradle cache, which is
why the Stock scheme is generated offline instead. Until that
dependency lands (CI network) or Ambient is re-scoped, the branch
keeps its previous behaviour, is commented as a deviation in Theme.kt,
and is recorded here. It is the only hand-tuned pairing left in the
theme file. (Ambient is #50, launcher-local; Material You is the
system dynamic theme and is unaffected.)

## LauncherModels.kt changes needed (NOT edited — exact spec)
File: `app/src/main/java/app/launcher/model/LauncherModels.kt`
1. `LauncherSettings.launcherTheme` (line ~488):
   - Old: `val launcherTheme: LauncherThemeChoice = LauncherThemeChoice.STOCK`
   - New: `val launcherTheme: LauncherThemeChoice = LauncherThemeChoice.MATERIAL_YOU`
   - Why: rule 3 — dynamic colour on by default on Android 12+; Stock
     is the fallback below API 31 (the MATERIAL_YOU branch already
     falls back to the generated Stock scheme there) and when the user
     picks Stock. Also update the stale comment at lines ~486-489
     ("Which should be the default is still open for Robert") to state
     the decision.
2. `AccentColour` (lines ~240-248): NO value change. Optional comment
   update only: the light/dark ARGB pairs are now SEEDS (each regenerates
   the whole scheme through the HCT pipeline), not primary colours.
   Suggested comment: "Each entry is a seed: the light value seeds the
   light scheme, the dark value seeds the dark scheme (rule 4/5)."
3. No other LauncherModels field needs to change for this stream.
   (`dockShadow`, `surfaceStyle`, `surfaceAlpha`, `amoledBlack` are
   untouched by this stream; see patch requests.)

## Patch requests for other files (exact anchors)
1. **Dock.kt:155-157, 163, 193-198 + LauncherModels.kt:433**
   (`dockShadow`): dock background should be `surface-container` and
   the search pill `surface-container-high` (rules 6, 17) instead of
   both using `surfaceColorFor` → `surfaceVariant`; remove the
   `Modifier.shadow(8.dp, RoundedCornerShape(24.dp))` application when
   `settings.dockShadow` is on (depth is tonal now). The `dockShadow`
   setting itself becomes dead once the shadow is gone — settings-model
   decision for the coordinator (remove the field + its settings row,
   or keep it dormant; this stream recommends removal so a dead toggle
   does not ship).
2. **HomeScreen.kt:452,454** (home backdrop gradient): replace the
   inline `Color(0xFF1C1610)` / `Color(0xFFEFE4CF)` with the theme roles
   defined for exactly this in Theme.kt:
   `if (darkTheme) HomeBackdropEdgeDark else HomeBackdropEdgeLight`
   (both occurrences; the middle stop already reads
   `MaterialTheme.colorScheme.background`). This closes audit rule 1.
3. **SettingsScreen (Appearance / theme rows)** — not anchored here
   (settings UI is another stream): the accent picker must be presented
   as applying to Stock only (under Material You the wallpaper is the
   seed — disable it or label it "Stock theme only"), and the theme
   row's default display should read Material You.
4. **FirstRunWizard.kt:131-134** (HELD file — do not touch): the
   theme choice offered there should default to Material You per rule 3;
   coordinator to route when the wizard stream opens.
5. **Runtime MCU follow-up (removes the Ambient deviation):** add
   `com.google.android.material:material-color-utilities` as a runtime
   dependency (CI network; not in the offline cache) so Ambient can
   generate its scheme from the wallpaper colour as a seed at runtime
   instead of hand-pairing `primary`/`onPrimary`. Until then, Ambient
   stays as documented above. (Long-term this could also replace the
   checked-in generated tables with runtime generation; not proposed
   now — the offline tables are the brief's prescribed path.)

## Contrast ratios measured (WCAG, generator output)
Default accent Blue — every text pair ≥ 4.5:1, boundary ≥ 3:1:

| Pair (fg on bg) | Light | Dark |
|---|---|---|
| onPrimary on primary | 8.96:1 | 9.12:1 |
| onPrimaryContainer on primaryContainer | 5.82:1 | 5.22:1 |
| onSecondary on secondary | 9.04:1 | 9.08:1 |
| onSecondaryContainer on secondaryContainer | 5.82:1 | 5.20:1 |
| onTertiary on tertiary | 8.99:1 | 9.07:1 |
| onTertiaryContainer on tertiaryContainer | 5.76:1 | 5.21:1 |
| onError on error | 8.98:1 | 9.09:1 |
| onErrorContainer on errorContainer | 5.80:1 | 5.19:1 |
| onBackground on background | 15.33:1 | 12.44:1 |
| onSurface on surface | 15.33:1 | 12.44:1 |
| onSurfaceVariant on surfaceVariant | 7.27:1 | 5.50:1 |
| onSurface on surfaceContainer | 14.79:1 | 12.01:1 |
| onSurface on surfaceContainerHigh | 14.01:1 | 10.47:1 |
| inverseOnSurface on inverseSurface | 11.61:1 | 10.18:1 |
| outline on surface | 4.02:1 | 5.11:1 |
| outlineVariant on surface (info only) | 1.52:1 | 1.72:1 |

All 12 schemes (6 accents × light/dark) pass the same gates; worst
text pair anywhere is 5.19:1. Ratios are identical across accents for
the neutral pairs by construction (warm override is accent-independent).
Note the old stock light white-on-primary was 4.51:1 — just above AA;
the pipeline-derived light primary now reads 8.96:1. The old dark
`onPrimary = 0xFF10243E` pairing is gone, replaced by the pipeline's
tone-20 onPrimary (dark Blue primary is now the tone-80 `0xFFC1C1FF`
family — verify on device that the lighter dark primary still reads as
"P22 blue" to Robert's eye; see device-gated notes).

## Verification
- `tools/kotlinc-check.sh` in the worktree: **passed, 47 source files**
  (kotlinc 2.2.20; CI with Kotlin 2.4.20 is the authority, as usual).
  Local Gradle builds remain impossible in this sandbox.
- Generator re-run is idempotent (second run: zero diff to Theme.kt).
- `material-color-utilities` Python package was installed into a venv
  (`/tmp/mcu-venv`, ephemeral) — system pip is PEP 668-blocked; the
  script header documents the venv install for re-runs.
- Type scale spot-check against the real material3 1.4.0-alpha06
  classes: `labelMedium = 12.0.sp`, `bodySmall = 12.0.sp`,
  `labelSmall = 11.0.sp`.

## Device-gated (still needs Robert's phone / a device)
- Dynamic colour visual check on API 31+ with a colourful wallpaper:
  the launcher must visibly re-theme; Stock fallback below API 31.
- Container-step readability: dock vs search bar vs surface must read
  as distinct steps in both themes (light steps are 2 tones apart;
  dark 5 tones) — screenshot judgement, not measurable in code.
- Dark Blue primary is lighter than the old hand-picked dark accent
  (`#5555FF` was painted directly; the pipeline's dark primary is a
  tone-80 blue): confirm the dark theme still reads as P22 blue.
- Label-over-wallpaper contrast (rule 13) and the dynamic scheme's own
  pairs: need the four test wallpapers on a device; not measurable here.
- 200% font scaling (rule 12) and the `.sp` call-site cleanup (rule 9)
  belong to other streams; the theme exposes the roles they need.
