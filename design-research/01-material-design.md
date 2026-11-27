# Material Design 3 / Material You — Design Research for Marlow Launcher

**Project:** Marlow Launcher (`app.launcher`) — a Pixel-style Android home launcher, Jetpack Compose + Material 3.
**Purpose:** the design system Marlow must be judged against. This file is the reference the re-audit and rebuild measure the app against.
**Date researched:** 2026-11-26. **Status of the system:** Material 3 baseline plus the Material 3 Expressive update (announced May 2025, shipped on Pixel with Android 16 QPR1, September 2025).

## Method and honesty note

Every load-bearing claim below is cited to a public source that was actually read during this research — the Material Design site (`m3.material.io`), `developer.android.com`, Google's own blogs, or the open-source implementation. Where a claim comes from a secondary source (a write-up of the spec rather than the spec itself) it is flagged as such. Where Material has *no* rule for something a launcher needs (e.g. home-screen app-icon labels), that is stated plainly and the recommendation is labelled as an *application* of the system, not a quote from it.

This document makes no claim about its author's employment, education, or credentials. It stands on its citations, not on authority. If a claim here and a cited source disagree, the source wins — fix this file.

**Marlow decisions already taken that this document interacts with** (from the project record, not from Material): the default theme is **Stock** (Material You one tap away in Settings/the wizard); the default accent is **P22 phosphor blue** (`#3333CC` light / `#5555FF` dark); shading is the warmer/softer CRT warm family (light background `#F8F1E7`, dark `#262019`); glow is a soft subtle halo. Those are project choices. What Material dictates is *how* a chosen seed colour becomes a whole theme, and what the resulting roles, shapes, motion, and spacing must be. The two meet in §1.5: the stock theme is a static M3 scheme generated from the P22-blue seed; Material You is the same pipeline with the wallpaper as the seed.

**Primary sources read in full**

- Colour roles — https://m3.material.io/styles/color/the-color-system/key-colors-tones
- Dynamic colour overview — https://m3.material.io/styles/color/dynamic-color/overview
- Typography overview — https://m3.material.io/styles/typography/overview
- Shape / corner radius scale — https://m3.material.io/styles/shape/corner-radius-scale
- Elevation — https://m3.material.io/styles/elevation
- Easing and duration — https://m3.material.io/styles/motion/easing-and-duration
- State layers — https://m3.material.io/foundations/interaction/states/state-layers
- Search guidelines — https://m3.material.io/components/search/guidelines
- Breakpoints (compact / medium / expanded) — https://m3.material.io/foundations/layout/breakpoints/compact (and `/medium`, `/expanded`)
- Material Design 3 in Compose — https://developer.android.com/develop/ui/compose/designsystems/material3
- Accessibility (mobile design guidance) — https://developer.android.com/design/ui/mobile/guides/foundations/accessibility
- Material motion codelab — https://developer.android.com/codelabs/material-motion-android
- Colour-contrast codelab — https://codelabs.developers.google.com/color-contrast-accessibility

---

# Part I — Principles

## 1. What Material 3 is

Material Design 3 (M3), also called **Material You**, is Google's design system for Android 12 and later. It is the third generation of Material Design (2014 → Material 2 in 2018 → M3 in 2021), and its current evolution is **Material 3 Expressive** (May 2025), which "complements the Android 16 visual style and system UI" (developer.android.com, Material Design 3 in Compose). A launcher that wants to feel stock on a 2026 Pixel is therefore judged against **M3 Expressive**, not the 2021 baseline — though Expressive is an expansion of M3, not a replacement: the colour roles, type scale, shape scale, and elevation levels below are the same system, with Expressive adding emphasized type styles, spring motion, and a larger shape library.

The system is built from **tokens**: small named design decisions (`md.sys.color.primary`, `md.sys.shape.corner.medium`, `md.sys.motion.*`) that resolve to values. In Compose these surface as `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and `MaterialTheme.shapes`. The discipline that matters for Marlow: **never hard-code a hex, a size, or a radius at a call site. Reference a role.** If a value appears in more than one place, it belongs in the theme.

## 2. The five ideas that explain nearly every rule

1. **Roles, not values.** Nothing in the UI is "purple `#6750A4`". It is `primary`, and `primary` resolves differently in light and dark, in static and dynamic schemes, and at different contrast levels. The role is the contract; the hex is an implementation detail. (m3.material.io, Colour roles)
2. **Every colour is a tone.** A scheme is five key colours expanded into tonal palettes of 13 tones each (0–100), and every role is a fixed tone from a palette. Light and dark mode are the same palettes with different tone assignments — which is why dark mode "comes almost for free" and why contrast is a property of the system, not something checked afterwards. (developer.android.com, Material Design 3 in Compose; m3.material.io, Colour roles)
3. **Depth is tonal, not shadow.** M3 communicates elevation primarily through surface colour (a primary-coloured tint that deepens with height), with shadows used only where extra separation is needed. A launcher full of drop shadows looks like Material 2. (m3.material.io, Elevation)
4. **Motion is choreography, not decoration.** Every transition answers "where did this come from, and where is it going?" Entering elements decelerate; exiting elements accelerate; bigger travel takes longer. (m3.material.io, Easing and duration)
5. **State is a layer, not a new colour.** Interaction feedback is a translucent overlay of the content's own colour at a fixed opacity per state — the same numbers everywhere, on every component. (m3.material.io, State layers)

## 3. The M2 → M3 differences that show up in a launcher audit

| Area | Material 2 | Material 3 |
|---|---|---|
| Colour | Fixed palette: 19 hues, 10-step scales, hand-picked | Generative: one seed → HCT tonal palettes → roles; dynamic colour from wallpaper |
| Elevation | Shadows at every level | Tonal surface colour first; shadows only when required |
| Shape | 3-level scale tied to component size | 10-level scale (0–48dp + full) tied to roundedness |
| Typography | Static ramp (H1–H6, subtitle, body, caption…) | 15 baseline roles (display/headline/title/body/label × L/M/S) + 15 emphasized roles |
| Motion | Utilitarian easing, shorter durations | Emphasized easing, snappier takeoffs, softer landings, slightly longer durations |
| Components | Bottom navigation, square-ish search | Navigation bar with pill active indicator; rounded search bar, no shadow by default |

Sources: m3.material.io (Elevation — "Differences from M2"; Search — "Differences from M2 to M3 baseline"; Shape — M2 3-level vs M3 10-level diagrams; Easing and duration — "Compared to the utilitarian style of M2, M3 easing is more expressive"); developer.android.com, Material Design 3 in Compose.

---

# Part II — The foundations, in detail

## 1. Colour

### 1.1 How a scheme is built

- **Five key colours** — primary, secondary, tertiary, neutral, neutral-variant (plus error) — each expand into a **tonal palette of 13 tones**: 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 99, 100 (0 = black, 100 = white; the number is a lightness value). (developer.android.com, Material Design 3 in Compose)
- **Roles map to fixed tones, differently in light and dark.** E.g. `primary` is tone 40 in light, tone 80 in dark; `primary-container` is tone 90 in light, tone 30 in dark; `surface` is neutral tone 98/99 in light, tone 6 in dark. The container/text relationship *inverts* between themes — never just invert a light scheme to get dark. (m3.material.io, Colour roles; corroborated by the tone→role mapping in the Material 3 references consulted)
- **Baseline is the default static scheme.** "Baseline is the default static color scheme for Material products. It includes colors for both light and dark themes." Its seed is the familiar purple `#6750A4`. (m3.material.io, Colour roles)
- **Error is static.** "Error color roles are made static by default with any dynamic color scheme. They still adapt to light and dark theme." Never recolour error roles for decoration. (m3.material.io, Colour roles)

### 1.2 Dynamic colour (Material You)

- On Android 12+, the system extracts a **source colour from the user's wallpaper** (user-generated colour) and generates light and dark schemes from it; content-based colour (from in-app imagery) is the alternative source. (m3.material.io, Dynamic colour overview)
- In Compose: use `dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)` when `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`; **fall back to a custom static `ColorScheme` when dynamic colour is unavailable**. (developer.android.com, Material Design 3 in Compose)
- The system pipeline (Monet) is open source as **Material Color Utilities** (`material-foundation/material-color-utilities`); the same engine powers the Material Theme Builder. The default scheme variant is `tonal-spot`. (See Part III for the full story.)
- **User-controlled contrast:** dynamic colour offers three contrast levels — standard, medium, high. Colour pairings must survive all three; this is exactly what breaks when roles are mixed incorrectly. (m3.material.io, Colour roles)

### 1.3 The roles, and where each belongs

| Role | Meaning / use |
|---|---|
| `primary` | High-emphasis fills, text, icons against surface. Filled buttons, FAB tint, active states. |
| `on-primary` | Text/icons on `primary`. Always paired. |
| `primary-container` | Standout fill against surface, for key components like the FAB. |
| `on-primary-container` | Text/icons on `primary-container`. |
| `secondary` / `on-secondary` | Less prominent fills/text/icons. Filter chips, dismissive buttons, nav icon selection state. |
| `secondary-container` / `on-secondary-container` | Recessive fills: tonal buttons, **navigation-bar active indicator**, selected chips. |
| `tertiary` (+ container / on-) | Contrasting accents to balance primary/secondary or bring heightened attention — e.g. a badge. Use at the designer's discretion. |
| `error` (+ container / on-) | Error states only. |
| `surface` | Default background. |
| `on-surface` | Text/icons on any surface or surface container. |
| `on-surface-variant` | Lower-emphasis text/icons on any surface. |
| `surface-container-lowest` / `-low` / (default) / `-high` / `-highest` | Five container tones, lowest → highest emphasis. Default `surface-container` is the navigation-area colour; `-low` for elevated cards/buttons; `-high` for FAB, basic dialogs, **search bars**; `-highest` for input labels, off switches. |
| `surface-dim` / `surface-bright` | Add-on roles: dimmest / brightest surface in both themes (they keep relative brightness across themes, unlike `surface`). |
| `outline` | Important boundaries: text-field outlines, borders that must meet 3:1. |
| `outline-variant` | Decorative elements: **dividers**. Never use `outline` for dividers; never use `outline-variant` to define a touch target's boundary (unless the contents inside provide 4.5:1). |
| `inverse-surface` / `inverse-on-surface` / `inverse-primary` | Reversed colours, e.g. snackbar background / text / action. |
| `scrim` | Temporary overlay dimming background content behind dialogs/sheets. |
| `primary-fixed` / `primary-fixed-dim` (+ on-fixed) | Add-on roles that keep the **same tone in light and dark**. Useful for a FAB that must not change tone — but "likely to cause contrast issues. Avoid using them where contrast is necessary." |

Sources: m3.material.io, Colour roles (all role definitions, the surface-container component mapping, the outline do/don'ts, and the fixed-colour warning); m3.material.io, Search guidelines (search bar = `surface-container-high`).

### 1.4 The pairing rule (the single most important colour rule)

> "To ensure accessible visual contrast in your app, apply colors only in the intended pairs or layering orders… Combining colors improperly may break contrast necessary for visual accessibility, particularly when colors are adjusted through dynamic color features such as user-controlled contrast." (m3.material.io, Colour roles)

- Every container/accent role has a matching `on-` role **guaranteed to meet contrast**. Always use them together: `primary`↔`on-primary`, `primary-container`↔`on-primary-container`, `surface`↔`on-surface` / `on-surface-variant`, `secondary-container`↔`on-secondary-container`, `error-container`↔`on-error-container`.
- The system's role pairs provide "an accessible minimum 3:1 contrast". (m3.material.io, Colour roles)
- **Do not** put `on-surface` text on `primary-container`, `primary` text on `tertiary-container`, or any other cross-pairing. The developer docs show exactly this as the ❌ example: a button with `tertiaryContainer` + `primaryContainer` content has poor contrast, while `primary` + `onPrimary` is correct. (developer.android.com, Material Design 3 in Compose — Accessibility)
- **Surface-container steps:** "To ensure proper contrast, use surface container roles that are more than one step apart." A `surface-container-high` search bar on a `surface-container` background blends in and fails. (m3.material.io, Search guidelines)
- **Emphasis without new colours:** vary emphasis with `surface` / `surface-variant` / `on-surface` / `on-surface-variant` combinations, and with font weight — not by inventing new greys. (developer.android.com, Material Design 3 in Compose — Emphasis)

### 1.5 How a non-dynamic "stock" theme should pick its roles (Marlow's Stock theme)

1. **Pick one seed colour.** For Marlow that seed is the decided P22 phosphor blue (`#3333CC` light / `#5555FF` dark — project decision). Everything else is derived; no role is hand-picked.
2. **Generate the full scheme from the seed with the HCT pipeline** — in practice, Material Theme Builder (`m3.material.io/theme-builder`) or the `material-color-utilities` library — and export *both* light and dark schemes. Do not hand-tune individual roles afterwards; if a role looks wrong, the seed is wrong.
3. **Keep the warm backgrounds as the neutral palette's expression.** Marlow's warm light background (`#F8F1E7`) and dark background (`#262019`) are choices about the *neutral* tones of the scheme, not overrides of individual surfaces: they should land on `surface` / `surface-container-*` roles so that every component that reads a surface role inherits the warmth consistently.
4. **Dynamic colour is the same pipeline with a different seed.** When the user enables Material You, the wallpaper's extracted colour replaces the P22 seed and the whole scheme regenerates. The app must not contain a second, parallel set of colours for dynamic mode — it swaps the *scheme*, not individual colours.
5. **Contrast levels are the user's, not the app's.** Respect the system's standard/medium/high contrast setting; never hard-code a contrast level, and never "fix" a role pairing by darkening text manually — fix the pairing.
6. **The glow is not a colour role.** Marlow's phosphor glow (soft halo on the page-indicator dot and widget resize handle) is a brand accent effect. It must be drawn *in addition to* the role system (e.g. a glow around a `primary`-coloured dot), never as a substitute for a role, and it must not carry information that colour alone carries (never the only indicator of state).

### 1.6 Contrast requirements (numbers)

| Content | Minimum ratio | Source |
|---|---|---|
| Normal (body) text | **4.5:1** (AA) | WCAG via developer.android.com Accessibility; codelabs colour-contrast |
| Large text (≥18pt regular / 14pt bold) | **3:1** | Same |
| Non-text: icons, UI component boundaries, input outlines, toggle states | **3:1** | developer.android.com Accessibility ("Use a 3:1 ratio between surfaces and non-text elements") |
| Colour-role pairs (e.g. `primary`/`on-primary`) | ≥3:1 by construction | m3.material.io, Colour roles |
| Badge with custom colours | ≥3:1 | m3.material.io, Badges guidelines |
| Body text size floor | **12sp** minimum ("Don't make the body size any smaller than 12 sp") | developer.android.com Accessibility |

- Dynamic colour is "designed to meet accessibility standards for color contrast" and "the system of tonal palettes is critical to making any color scheme accessible by default" — but only if roles are used as paired. Customising a component's colours is where contrast breaks; the docs warn explicitly to "use appropriate color roles and avoid mismatch". (developer.android.com, Material Design 3 in Compose — Accessibility)
- **Text over wallpaper is the launcher's hard case.** M3 has no role for "text on a user's photo". The honest options are the ones M3 itself uses for text over imagery: a **scrim** (the role exists for exactly this — a temporary overlay that makes background content less prominent) or a surface-container chip behind the text. White text with a drop shadow is *not* an M3 pattern; a scrim or a container is. This applies to app-icon labels, the clock widget, and At-a-Glance text.

## 2. Typography

### 2.1 The type scale

M3 has **15 baseline type styles and 15 emphasized styles** (the emphasized set added in the May 2025 M3 Expressive update): five roles — display, headline, title, body, label — each in large, medium, and small. "Type roles describe size — such as small, medium, and large — enabling them to adapt and respond to the device or context." (m3.material.io, Typography overview)

| Style | Size / line-height | Weight | Tracking | In Compose |
|---|---|---|---|---|
| Display Large | 57 / 64 sp | Regular (400) | −0.25 sp | `displayLarge` |
| Display Medium | 45 / 52 sp | Regular | 0 | `displayMedium` |
| Display Small | 36 / 44 sp | Regular | 0 | `displaySmall` |
| Headline Large | 32 / 40 sp | Regular | 0 | `headlineLarge` |
| Headline Medium | 28 / 36 sp | Regular | 0 | `headlineMedium` |
| Headline Small | 24 / 32 sp | Regular | 0 | `headlineSmall` |
| Title Large | 22 / 28 sp | Regular* | 0 | `titleLarge` |
| Title Medium | 16 / 24 sp | Medium (500) | 0.15 sp | `titleMedium` |
| Title Small | 14 / 20 sp | Medium | 0.1 sp | `titleSmall` |
| Body Large | 16 / 24 sp | Regular | 0.5 sp | `bodyLarge` |
| Body Medium | 14 / 20 sp | Regular | 0.25 sp | `bodyMedium` |
| Body Small | 12 / 16 sp | Regular | 0.4 sp | `bodySmall` |
| Label Large | 14 / 20 sp | Medium | 0.1 sp | `labelLarge` |
| Label Medium | 12 / 16 sp | Medium | 0.5 sp | `labelMedium` |
| Label Small | 11 / 16 sp | Medium | 0.5 sp | `labelSmall` |

Sizes and line heights are from developer.android.com (Material Design 3 in Compose — Typography table). Weights are from the same page (title/label styles are "Roboto Medium"; display/headline/body are "Roboto") and the M3 type-scale references. Tracking values are the M3 token defaults as shipped in Compose's `Typography` defaults. *Note: the developer.android.com page shows `titleLarge` as "New- Roboto Medium 22/28" in its table while the Reply sample customises it to SemiBold; the baseline token weight for title-large is Regular (400) with Medium commonly used by apps — if Marlow sets it to Medium, that is a deliberate override to record in the theme, not the default.

- **Typeface:** Roboto is the M3 typeface (Roboto Flex as the variable-font option; Google Sans Flex is the expressive counterpart). "Use variable fonts for more control over expression in editorial treatments." (m3.material.io, Typography overview)
- **Emphasized styles** mirror the baseline scale at higher weight for "highlighted moments" and clearer hierarchies. Use sparingly — a home screen that emphasizes everything emphasizes nothing. (m3.material.io, Typography overview)
- **Sizes in sp, always** — "specify font size in scalable pixels (sp)" so the user's font-size setting works. Never dp for text. (developer.android.com, Accessibility)
- **You will not need all 15 styles.** The Reply sample uses five. Pick a small set, use it everywhere, and omit the rest from the theme. (developer.android.com, Material Design 3 in Compose — Typography)

### 2.2 Hierarchy rules — which role, when

| Role | Job | Launcher examples |
|---|---|---|
| Display | Hero moments, large decorative text | Clock widget's large time (if styled as display type); onboarding splash |
| Headline | Primary content headers, screen titles | Settings screen title; folder name in the open-folder view; "Widgets" picker header |
| Title | Secondary structure, component-level headers | Dialog headline; list-item primary text; section headers in Settings |
| Body | Reading — the majority of content | Settings descriptions and helper text; search-result secondary lines; dialog supporting text |
| Label | Small, functional, utilitarian text | Buttons, chips, tabs, navigation labels, captions, badges |

- **Buttons, chips, and tabs use Label/Title roles at Medium weight** — never Body. Body roles are for reading, with their positive tracking. (M3 token guidance, corroborated across the M3 references consulted)
- **Button text is sentence case, no ALL CAPS.** (M3 component guidance, as reported in the 2021 M3 component coverage)
- **Emphasis comes from weight and colour roles**, not from inventing sizes: "Using different font weights for text" is one of M3's two emphasis mechanisms, the other being surface/on-surface colour combinations. (developer.android.com, Material Design 3 in Compose — Emphasis)
- **One size per job.** If two pieces of text do the same job at different sizes, one of them is wrong. The scale exists so hierarchy is a *choice of role*, not a negotiation in sp.

### 2.3 App-icon labels — the launcher's special case (application, not spec)

Material has **no component spec for home-screen app-icon labels** — they are launcher UI, not an M3 component. Applying the scale honestly gives:

- **Size: 12sp, no smaller.** 12sp is both the floor for body text (developer.android.com, Accessibility) and the size of `body-small` (12/16) and `label-medium` (12/16). Anything smaller is below the system's own readability floor.
- **Role: `body-small` (Regular) or `label-medium` (Medium).** Either is defensible; pick one and use it for *all* icon labels (home grid, dock, drawer, folders). Do not mix. (Note: `body-medium` is 14sp — a rule like "icon labels use body-medium at 12sp" contradicts the scale itself; 12sp means body-small or label-medium.)
- **Colour:** `on-surface` where the label sits on a surface; over wallpaper, white text is only legible with a scrim or a shadow treatment — and M3's own answer to text-over-imagery is a **scrim**, not a shadow (see §1.6). Single line, ellipsis, centred under the icon.
- **Dock labels** follow the same rule. A dock is not an excuse for a second label style.

## 3. Shape

### 3.1 The scale

M3 defines corner radii with a **ten-style, size-based scale**. "Styles are assigned to components based on the desired amount of roundedness." (m3.material.io, Shape)

| Style | Radius | Default components |
|---|---|---|
| None | 0dp | Full-screen sheets; square containers |
| Extra small | 4dp | Text fields, snackbars, menus |
| Small | 8dp | Chips, menus |
| Medium | 12dp | Cards, small FAB |
| Large | 16dp | FAB, extended FAB, navigation drawer |
| Large increased | 20dp | (Expressive addition) |
| Extra large | 28dp | Dialogs, expanded bottom sheets |
| Extra large increased | 32dp | (Expressive addition) |
| Extra extra large | 48dp | (Expressive addition) |
| Full | Fully rounded | Buttons, pills, badges, nav active indicator, circular FAB |

Sources: m3.material.io, Shape (corner radius scale); developer.android.com, Material Design 3 in Compose (Shapes — `extraSmall 4dp, small 8dp, medium 12dp, large 16dp, extraLarge 28dp`; "Medium shape for Card and Large shape for Floating action button").

- **Compose's `Shapes` class** exposes `extraSmall / small / medium / large / extraLarge` (plus `full` via `CircleShape`); the three newer Expressive steps (20/32/48dp) are set per-component where used. The Reply sample customises `extraLarge` to 24dp — that is a *customisation*, not the default; Marlow should use the 28dp default unless it records a deliberate override.
- **Buttons are full by default.** "By default, buttons are mapped to the **full** corner radius shape style. If your product needs a less rounded shape, remap the token to another style in the shape scale, such as **small** or **medium**." (m3.material.io, Shape)
- **Shape families:** rounded (default) or cut (straight-line corners). Cut corners clip content more than rounded ones of the same size — add padding in information-dense components if cut is ever used. Marlow should stay rounded; cut is a brand statement M3 itself treats as an exception. (m3.material.io, Shape)
- **Don't put large or full corners on information-dense components** such as cards. (m3.material.io, Shape — Caution)

### 3.2 Shape rules that catch a launcher out

- **Optical roundness for nesting:** when one rounded object sits inside another, do not reuse the same radius. Inner radius = outer radius − padding (e.g. 48dp − 14dp = 34dp). Same-radius nesting "can make the corners look unbalanced". (m3.material.io, Shape) — This bites in folder previews, widget frames inside cards, and the search bar inside a sheet.
- **Asymmetric shapes** exist for closely-grouped items (menus, split buttons) via inner corners. A launcher's grouped controls (e.g. a segmented control in Settings) should use them rather than uniform radii. (m3.material.io, Shape)
- **Customisation is allowed at style or component level** — changing the `medium` style restyles every card and small FAB; remapping one component restyles only it. "Products should consistently use the Material 3 shape styles. However, customization is sometimes necessary, and even encouraged, for hero moments or custom components." (m3.material.io, Shape) Marlow's warm/CRT identity is expressed through colour and glow, not by redefining the shape scale.
- **Shape is how a launcher signals "this is a button" vs "this is a card" vs "this is a sheet".** If the home screen, the drawer, and the settings all use the same radius everywhere, the user cannot tell containers apart by shape — and M3's answer is that they should be able to.

## 4. Elevation and surfaces

### 4.1 The six levels

| Level | Height | Primary-tint opacity | Typical occupants |
|---|---|---|---|
| 0 | 0dp | 0% | Most resting components, filled buttons, outlined cards, default surfaces |
| 1 | 1dp | 5% | Elevated cards, bottom app bar, search bar |
| 2 | 3dp | 8% | Navigation bar, menus, elevated buttons (hover) |
| 3 | 6dp | 11% | FAB, dialogs, snackbars, navigation drawer |
| 4 | 8dp | 12% | Cards picked up / hovered |
| 5 | 12dp | 14% | Dragged elements |

Heights and level assignments are the M3 token values (m3.material.io, Elevation; corroborated by the M3 elevation references). Tint opacities are the M3 tonal-elevation values as documented in the token references consulted. Note the elevation page itself frames it as: "Elevation can be shown as tonal surface colors or shadows" and "Tokens have no shadows or color; each platform determines the specific shadows and values to use at each elevation level."

### 4.2 Rules

- **Tonal first, shadow second.** "M3: Using color instead of shadows to communicate elevation." Shadows are for "additional protection against a background or to encourage interaction" — not the default. (m3.material.io, Elevation — Differences from M2)
- **Elevation in dark themes is tonal too.** "Elevation overlays in dark themes have also changed to tonal color overlays in Material 3. The overlay color comes from the primary color slot." A dark-theme launcher that relies on shadows for depth is invisible in the dark — the tint *is* the depth cue. (developer.android.com, Material Design 3 in Compose — Elevation)
- **Do not change component defaults.** "All components have a default resting elevation. Avoid changing the default elevation of Material components." and "Stick to using a small amount of elevation levels." (m3.material.io, Elevation)
- **Elevation responds to interaction, consistently.** "Components should change elevation in response to system events or user interaction, like hovering… hovering a FAB temporarily increases the elevation by 1 level, from level 3 to level 4. All Material buttons increase elevation by 1 level when hovered." (m3.material.io, Elevation)
- **Surface containers are the Compose expression of elevation.** Level 0 → `surface`, level 1 → `surface-container-low`, level 2 → `surface-container`, level 3 → `surface-container-high`, levels 4–5 → `surface-container-highest`. In practice Marlow should reach for the container role, not a `tonalElevation` number — the role carries the level with it. (M3 component colour mapping, m3.material.io Colour roles)
- **In Compose**, `Surface` takes both `tonalElevation` and `shadowElevation` — use tonal for depth, shadow sparingly. (developer.android.com, Material Design 3 in Compose — Elevation)
- **Scrim is not elevation.** A scrim is a temporary overlay to focus attention (dialogs, modal sheets). Do not use scrim opacity as a general dimming tool for the home screen.

### 4.3 What this means on the home screen

- The home screen itself is level 0 (`surface`, or the wallpaper). The **dock** is a navigation area → `surface-container`. The **search bar** is level 1 / `surface-container-high`. A **folder open** or **bottom sheet** is level 3+ / `surface-container-high`. A **dragged icon** is level 5. If two of those look identical, the elevation system is not being used.
- **Widgets sit on the wallpaper, not on a surface.** A widget that needs a background should use a surface-container role (or a scrim), not a grey box with a shadow — grey boxes with shadows are the single most reliable tell of a non-Material launcher.

## 5. Motion

### 5.1 Principles

- "In the physical world, objects don't start or stop instantaneously. Instead, they take time to speed up and slow down. Transitions without easing look stiff and mechanical, while a transition with easing appears more natural." (m3.material.io, Easing and duration)
- M3 easing is deliberately more expressive than M2: "Transitions have snappy take offs and very soft landings. Durations are slightly longer compared to M2. This gives transitions time to come to a gentle rest without feeling abrupt." (m3.material.io, Easing and duration)
- "Transitions shouldn't be jarringly fast or so slow that users feel as though they're waiting." (m3.material.io, Easing and duration)

### 5.2 Easing and duration (the table to build to)

**Emphasized is the default for most transitions** — "recommended for most transitions to capture the style of M3". **Standard** is for "small utility focused transitions that need to be quick", and is the fallback on platforms without Emphasized support. (m3.material.io, Easing and duration)

| Easing | Duration | Transition type |
|---|---|---|
| Emphasized | 500 ms | Begin and end on screen |
| Emphasized decelerate | 400 ms | Enter the screen |
| Emphasized accelerate | 200 ms | Exit the screen |
| Standard | 300 ms | Begin and end on screen |
| Standard decelerate | 250 ms | Enter the screen |
| Standard accelerate | 200 ms | Exit the screen |

The cubic-bezier values (M3 token values): Emphasized `cubic-bezier(0.2, 0, 0, 1.0)`; Emphasized decelerate `cubic-bezier(0.05, 0.7, 0.1, 1.0)`; Emphasized accelerate `cubic-bezier(0.3, 0, 0.8, 0.15)`; Standard `cubic-bezier(0.2, 0, 0, 1.0)`; Standard decelerate `cubic-bezier(0, 0, 0, 1.0)`; Standard accelerate `cubic-bezier(0.3, 0, 1.0, 1.0)`.

- **Direction chooses the curve.** "Easing types are chosen based on how a transition moves in relation to the screen." Entering: decelerate — "begins at peak velocity then comes to a gentle rest". Exiting permanently: accelerate — "begins at rest and ends at peak velocity. By ending at peak velocity, it gives the impression the exiting component cannot be retrieved." Exiting *temporarily* (it can come back — a drawer, a sheet): Emphasized, "ending at rest just off screen". (m3.material.io, Easing and duration)
- **Duration scales with distance.** "Transitions that cover small areas of the screen have short durations. Those that traverse large areas have long durations. Scaling duration with the size of a transition area gives a consistent sense of speed." (m3.material.io, Easing and duration) A full-screen open takes ~500 ms; a chip appearing takes ~200 ms. Same speed, different distance.
- **Exits are faster than enters.** "Transitions that exit, dismiss, or collapse an element use shorter durations. Exit transitions are faster because they require less attention than the user's next task." Enter 500 ms vs exit 200 ms is the canonical pair. (m3.material.io, Easing and duration)
- **Duration tokens** (for state changes and smaller work): Short 50/100/150/200 ms, Medium 250/300/350/400 ms, Long 450/500/550/600 ms, Extra-long 700–1000 ms. (M3 duration token scale, as documented in the M3 references consulted)
- **M3 Expressive adds spring physics.** Springs "have no fixed duration — they respond dynamically to input", with standard and expressive schemes; spatial springs for things that move/resize/morph, effects springs for colour/opacity. (M3 Expressive motion guidance, as documented in the M3 references consulted; see also Part III.) In Compose this is the `MotionScheme` / spring APIs. A launcher's drag-and-drop, fling-to-dismiss, and sheet settling should feel springy; its screen transitions should use the easing table above. Do not mix the two jobs.
- **Respect reduced motion.** When the system requests reduced motion, transitions should collapse to fades or instant changes. (Platform accessibility guidance; see developer.android.com, Accessibility.)

### 5.3 Transition patterns — which one, when

The Material motion system defines four patterns (m3.material.io, "Building Beautiful Transitions with Material Motion for Android"; developer.android.com, Material motion codelab):

| Pattern | Use when… | Launcher mapping |
|---|---|---|
| **Container transform** | One thing turns into another; a shared outer container, inner content swaps | App icon → app opening (system handles this, but the launcher's own version is folder icon → open folder; widget → widget config; search bar → search view; FAB → compose/edit surface) |
| **Shared axis** | Elements have a spatial or navigational relationship; motion along x, y, or z reinforces it | Home page → app drawer (z-axis: drawer rises over home); drawer pages; settings screens stepping forward/back (x-axis); search icon → search page (z-axis) |
| **Fade through** | Elements have *no* strong relationship; outgoing fades out, then incoming fades in while scaling 92% → 100% | Switching home-screen pages is *not* this (pages have a spatial relationship — use shared axis / slide). Use fade-through for unrelated content swaps, e.g. At-a-Glance content changing, or switching between unrelated sections |
| **Fade** | Elements enter/exit *within* the screen bounds; entering scales 80% → 100%, exiting just fades | Dialogs, menus, tooltips, badges appearing; the app-drawer scrim |

- **Choreography rule:** run transitions as pairs (exit + enter together) so direction reads. A shared-axis transition "operates on two targets simultaneously… pairs of transitions run together to create a continuous, directional animation". (developer.android.com, Material motion codelab)
- **The home screen's signature motions are the system's, not the launcher's.** App-open/app-close animations, the overview/recents transition, and the predictive-back gesture are owned by the system and the launcher must not fight them: the launcher's job is to hand off cleanly (correct window transitions, no extra animation layered on top) and to make its *own* surfaces (drawer, folders, sheets, search) follow the patterns above.

## 6. State layers

- **A state layer is a semi-transparent overlay on an element that indicates its state**, using opacity only — one layer at a time, sandwiched between container and content, coloured with the content's (`on-`) colour. "The state layer uses a fixed percentage for the opacity for each state. A state layer uses the color used by content (usually the on color)." (m3.material.io, State layers)
- **The numbers** (read directly from the spec page):

| State | Opacity |
|---|---|
| Hover | **+8%** |
| Focus | **+10%** |
| Press | **+10%** |
| Drag | **+16%** |
| Disabled | **38%** |

- **Example from the spec:** a `secondary-container` container with `on-secondary-container` content gets a state layer in `on-secondary-container`; a `surface` container with `primary` content gets a state layer in `primary`. The layer colour follows the *content*, not the container. (m3.material.io, State layers)
- **Sizes:** "The size of state layers is 40dp while the interactive target size is 48dp." The visible feedback circle/rect is smaller than the hit area. (m3.material.io, State layers)
- **Ripple:** M3's ripple is a touch-originated ink reaction — it starts at the touch point, not the element centre. On Android 12+, Compose's ripple "uses a subtle sparkle to illuminate surfaces when pressed" via the platform `RippleDrawable`. (developer.android.com, Material Design 3 in Compose — System UI / Ripple) A launcher that shows a centred fade on press instead of a touch-point ripple reads as wrong to anyone who has used a Pixel.
- **Overscroll:** M3 uses a stretch effect at the edge of scrolling containers (on by default in `LazyColumn`/`LazyRow`/`LazyVerticalGrid` in Compose Foundation 1.1.0+). A launcher's drawer and settings lists should stretch, not glow (the old M2 edge glow is gone). (developer.android.com, Material Design 3 in Compose — System UI / Overscroll)
- **Selected state is not a state layer.** Selection is shown with a container change (e.g. `secondary-container` active indicator, filled icon) *plus* the state layer for press/hover. Do not signal "selected" with opacity alone.
- **Focus must be visible.** Focus is a 10% layer *and* (on non-touch navigation) a visible focus indicator. A launcher driven by keyboard/d-pad must show focus on every focusable element — the home grid included.

## 7. Density, spacing, and layout

### 7.1 Breakpoints (May 2026 revision)

Window size classes were renamed **breakpoints**. The five breakpoints:

| Breakpoint | Width | Panes | Navigation |
|---|---|---|---|
| Compact | under 600dp | 1 | Navigation bar, modal expanded rail |
| Medium | 600–839dp | 1 recommended, 2 possible | Collapsed navigation rail |
| Expanded | 840–1199dp | 2 recommended | Collapsed or standard expanded rail |
| Large | 1200–1599dp | 2 recommended | Standard expanded rail |
| Extra-large | 1600dp+ | up to 3 | Standard expanded rail |

Sources: m3.material.io, Breakpoints (compact/medium/expanded pages read directly — compact <600dp, medium 600–839dp, expanded 840–1199dp); the Large/Extra-large rows and the rename are per the May 2026 revision of the foundations pages as reported in the conformance write-up consulted.

- **A phone is Compact.** Nearly everything Marlow does lives in the compact column: single pane, 16dp margins, navigation at the bottom near the edge ("Place navigation components close to the edge of the screen where they're easier to reach"). (m3.material.io, Breakpoints — Compact)
- **Foldables/tablets change the layout, not just stretch it.** At medium/expanded widths, search switches from full-screen to docked, dialogs stop being full-screen, and a second pane becomes possible. Marlow's foldable/tablet behaviour must be a breakpoint decision, not a stretched phone layout.

### 7.2 Spacing

- **Margins:** compact 16dp; medium and expanded 24dp (and 24dp spacers between panes). (m3.material.io, Breakpoints — Compact/Medium/Expanded)
- **Spacing scale (May 2026 tokens):** an 8dp scale where `space100 = 8dp`, with recommended stops including sub-8 nested units: space0 0, space25 2, space50 4, space75 6, space100 8, space125 10, space150 12, space175 14, space200 16, space250 20, space300 24, space400 32, space450 36, space500 40, space600 48, space700 56, space800 64, space900 72. Spacing has three categories — **padding** (inside an element), **gap** (between elements), **margin** (outside an element) — and margins are a last resort: define padding and gaps on the parent container. (Per the May 2026 revision of the foundations pages, as reported in the conformance write-up consulted.)
- **4dp grid for micro-spacing, 8dp for macro.** Component-internal spacing (icon-to-text, label padding) snaps to 4dp; layout spacing between sections snaps to 8dp. (M3 spacing practice, as documented in the M3 references consulted)
- **Text line length:** "across all breakpoints, adjust margins and type styles to keep text between 40–60 characters per line". (Per the foundations pages, as reported in the conformance write-up consulted.) Mostly a settings/dialog concern for a launcher — but it is why settings descriptions should not run edge-to-edge on a tablet.

### 7.3 Touch targets and density

- **Minimum touch target: 48 × 48dp**, for every interactive element — even when the visual element is smaller (expand the hit area, don't grow the icon). "The size of state layers is 40dp while the interactive target size is 48dp." (m3.material.io, State layers; developer.android.com, Accessibility)
- **Minimum 8dp between targets.** (M3 accessibility guidance, as documented in the M3 references consulted)
- **System icons are 24dp** (20dp for small/secondary icons), 2dp stroke. App-icon cells on the home grid are a different thing — the *cell* must still offer a 48dp target.
- **Home-screen density is a launcher decision M3 does not make for you.** There is no M3 spec for "how many icons per row". What M3 does constrain: 16dp page margins (compact), ≥48dp targets, 12sp labels, 4/8dp spacing rhythm, and legibility over wallpaper (§1.6). A 5-column grid of 48dp targets with 12sp labels fits those constraints on a ~360–400dp-wide phone; a 6-column grid of cramped 40dp targets does not. Density is judged by whether the constraints hold, not by a column count.
- **Ergonomics:** "For horizontal tablets and unfolded foldables, the top 25% of the screen is likely out of reach… limit the amount of interactions that are placed in the upper 25% of the screen." (m3.material.io, Breakpoints — Medium) The same thumb-reach logic is why a launcher's search bar and dock live at the bottom and why the top of the home screen is for glanceable content, not controls.

## 8. Components — as relevant to a launcher

### 8.1 Search bar (and search view)

- **Placement:** "A search bar is typically placed at the top of a screen to remain prominent and accessible" — but for a *launcher*, the Pixel convention is the search bar at the *bottom* (thumb reach, above the dock). M3's rule is about prominence and closeness to the searchable content, not an absolute edge; the launcher's bottom placement is a deliberate, documented deviation for ergonomics, and the bar must still be persistent and prominent. (m3.material.io, Search guidelines)
- **Container:** `surface-container-high` — "ensuring the container has clear contrast" against a `surface` background. Never put it on a `surface-container` background (roles must be more than one step apart). (m3.material.io, Search guidelines)
- **Margins:** unfocused 24dp, focused 12dp; "In the contained style, the search bar container remains the same shape in both the unfocused and focused states. Avoid changing the container behavior." (m3.material.io, Search guidelines)
- **Anatomy:** container, leading icon (navigational icon button *or* a non-functional search icon), supporting/hint text ("Provide a short description of the information people can search, like **Search replies**"), optional avatar/trailing icon, input text, results container. (m3.material.io, Search guidelines)
- **Trailing icons: maximum two**; "Combine an avatar with up to one other trailing icon button". A focused bar may show a clear (×) icon. (m3.material.io, Search guidelines)
- **Focused search:** suggestions can appear before typing; results appear as typing or after executing; results show in a list below the bar. **Full-screen layout is the default for compact** (phones); **docked layout for medium/expanded** (tablets/foldables). The back icon releases focus and dismisses results. (m3.material.io, Search guidelines)
- **Predictive back:** on Android, search "detaches from the screen edge to signal the full-screen layout will minimize" and "the previous screen is revealed in a preview". A launcher's search must support predictive back rather than swallowing the gesture. (m3.material.io, Search guidelines)
- **Accessibility:** "focused search needs a clear status indicator that it's searching content, like a search icon or **Results** label." (m3.material.io, Search guidelines)

### 8.2 Bottom sheet

- **Two types:** standard (co-exists with the main UI; both regions stay interactive; used when the main region is frequently scrolled/panned) and **modal** (appears in front, disables all other functionality until confirmed/dismissed; "Modal bottom sheets are used in mobile apps only"). (m3.material.io, Bottom sheets guidelines)
- **Anatomy:** container (the only required element), drag handle (optional), scrim (modal only). (m3.material.io, Bottom sheets guidelines)
- **Shape:** top corners extra-large (28dp), bottom corners square. Drag handle: 32×4dp pill in `on-surface-variant` at 40% opacity (Compose `BottomSheetDefaults.DragHandle` defaults to `MaterialTheme.shapes.extraLarge` and the sheet's container colour). (m3.material.io, Bottom sheets guidelines; developer.android.com, `BottomSheetDefaults` API reference)
- **Scrim:** 32% black over background content for modal sheets (and dialogs). (M3 scrim value, as documented in the M3 references and the `BottomSheetDefaults.ScrimColor` / Dialog scrim attributes)
- **Behaviour:** preset heights (peek / half / expanded), drag or fling to dismiss, tap-outside/scrim to dismiss a modal sheet; a sheet is for supplementary content and actions, not the main flow. Only one sheet at a time.
- **Launcher mapping:** the long-press app menu, widget picker, and "customise" panels are modal bottom sheets; a persistent mini-player or now-playing strip would be a standard sheet. Do not use a modal sheet where a dialog belongs (a decision that blocks everything) or a dialog where a sheet belongs (a list of choices).

### 8.3 Dialogs

- "A dialog is a modal window that appears in front of app content to provide critical information or ask for a decision. Dialogs disable all app functionality when they appear… Dialogs are purposefully interruptive, so they should be used sparingly." (m3.material.io, Dialogs guidelines)
- **Variants:** basic dialog; full-screen dialog — "Full-screen dialogs are for compact breakpoints only, like mobile devices. For medium and expanded breakpoints, use a basic dialog." (m3.material.io, Dialogs guidelines)
- **Anatomy (basic):** container, optional icon, headline, supporting text, optional divider, buttons, scrim. (m3.material.io, Dialogs guidelines)
- **Container:** `surface-container-high`, shape extra-large (28dp), side insets 24dp, top/bottom insets 80dp. **Scrim 32%.** Headline uses `titleLarge`; supporting text `bodyMedium`; actions are **text buttons**. (M3 Dialog component attributes, as documented in the material-components-android Dialog reference)
- **Launcher mapping:** uninstall confirmations, "remove widget?" confirmations, destructive actions, permission explanations. A dialog must never be used for something the user does routinely — that is a sheet or an inline control.

### 8.4 Chips

- **Four types:** assist, filter, input, suggestion — "represent options in a specific context". (M3 component guidance, as reported in the 2021 M3 component coverage)
- **Shape:** small (8dp). Selected filter chips use `secondary-container` / `on-secondary-container`; unselected chips use `outline` borders (with the outline-variant caution from §1.3 — the contents inside must provide 4.5:1). (M3 shape/component mapping; developer.android.com — secondary roles "used for less prominent components in the UI, such as filter chips")
- **Launcher mapping:** filter chips in search results ("Apps / Widgets / Settings / Contacts"); suggestion chips for recent searches. Chips are *not* buttons and *not* navigation — one tap, one discrete action or filter.

### 8.5 Switches and sliders

- **Switch:** toggles a single setting on/off. Selected state uses `primary` (track) with `on-primary` thumb/icon; unselected track is `surface-container-highest` with an `outline` border. Minimum 48dp touch target (the visual switch is smaller; the hit area is not). (M3 Switch component tokens; the unselected-track role is the one named in the colour-roles page's surface-container mapping — "surface container highest applied to… off switch")
- **Slider:** selects a value from a range. Active track and thumb use `primary`; inactive track uses `surface-container-highest`; value indicator (when shown) uses `inverse-surface` with `inverse-on-surface` text. Minimum 48dp touch height. (M3 Slider component tokens, as documented in the M3 references consulted)
- **Launcher mapping:** switches for every on/off setting (never a checkbox for a setting, never a custom toggle that doesn't read as a switch); sliders for grid size, icon size, animation scale, transparency. A slider must show its value — a bare track with no readout is a guessing game.

### 8.6 FAB

- "A floating action button (FAB) represents the primary action of a screen." Sizes: small, medium (default), large, extended. (M3 component guidance)
- **Shape:** large (16dp) for the default FAB; medium (12dp) for the small FAB. (developer.android.com, Material Design 3 in Compose — Shapes: "Large shape for Floating action button")
- **Colour:** `primary-container` / `on-primary-container` by default (the colour-roles page shows the FAB as the primary-container example); `primary-fixed` is the alternative when the FAB must not change tone between themes.
- **Elevation:** level 3 (6dp); +1 level on hover (→ level 4). (m3.material.io, Elevation)
- **"FAB should persist on the screen when content is scrolling"** and, having no text label, must be "functionally explicit". Extended FAB (icon + label) is "the most visually prominent button" — icon optional. (M3 FAB guidance, as reported in the 2021 M3 component coverage)
- **Launcher mapping:** a launcher rarely needs a FAB on the home screen — the home screen's primary actions are the icons themselves. A FAB belongs in *creation* flows (e.g. "add widget" inside the widget picker, or the edit-mode entry point) — not floating over the wallpaper, where it competes with the icons and has no single primary action to represent. If Marlow has a FAB anywhere, it must pass this test: "what is the one primary action of this screen?"

### 8.7 Navigation bar

- For compact devices, "when you want to target 5 or less destinations". (`NavigationBar` — developer.android.com, Material Design 3 in Compose — Navigation components)
- **Anatomy:** container `surface-container`, elevation level 2 (3dp); height 80dp baseline (64dp in the M3 Expressive "short" configuration); icon 24dp; label `label-medium`; **active indicator 56×32dp pill in `secondary-container`**; active icon filled + `primary`, inactive icon outlined + `on-surface-variant`. (M3 Navigation bar component tokens, as documented in the M3 references consulted; the ≤5 destination limit and the filled/outlined active-state language are from developer.android.com and the M3 component coverage)
- **Badges on nav items:** hide the badge once the destination has been selected. (m3.material.io, Badges guidelines)
- **Launcher mapping:** Marlow has no in-app bottom navigation bar on the home screen — the dock is not a navigation bar (it launches apps, it doesn't switch destinations within Marlow). The navigation-bar rules apply where Marlow *does* switch destinations: the Settings bottom navigation if one exists, and the widget-picker / drawer tabs. Do not style the dock as a navigation bar (no active indicator on a dock icon — a dock icon is not a "selected destination").

### 8.8 Badges

- "Badges are used to indicate a notification, item count, or other information relating to a navigation destination. They are placed on the ending edge of icons, typically within other components." Two variants: **small badge** (a simple circle — unread notification) and **large badge** (contains label text — item count). (m3.material.io, Badges guidelines)
- **Sizes:** small badge 6dp; large badge 16dp high, width grows with the count (one digit 16dp, max four characters 16×34dp); large-badge text padding 4dp. **Maximum four characters including "+"** (e.g. "999+"). (m3.material.io, Badges specs)
- **Placement:** anchored inside the icon bounding box — small badge offset 6×6dp from the top-trailing icon corner, large badge 14×12dp. "Don't change the position of the badge arbitrarily or place the badge over the icon." Change position for right-to-left languages. (m3.material.io, Badges guidelines/specs)
- **Colour:** "Use the default badge color" (error red). "Avoid using custom color roles for the badge container and label text. If custom roles are necessary, make sure they have contrast of at least 3:1." (m3.material.io, Badges guidelines)
- **Launcher mapping:** notification counts on app icons are badges. That means: small dot for "has notifications, count unknown/irrelevant"; large badge with a number (max 4 chars) for counts; error-red by default; anchored to the icon's top-trailing corner, never centred on the icon, never covering the icon's centre; hidden when the app is opened / the destination is selected. A badge count that reads "127" is wrong (cap it: "99+"); a badge that is a different colour per app is wrong; a badge that sits in the middle of the icon is wrong.

---

# Part III — Checkable rules

Every rule below is testable against the app. Each states *what to check*, *how to check it*, and the *source* that justifies it. Rules marked **[APP]** are applications of the system to launcher UI that Material does not specify directly; the rest are direct readings of the cited source.

## Colour

- **R1.** No raw hex colour appears in any composable outside the theme definition. Check: search the UI code for `Color(0xFF` outside `Theme.kt` / `Color.kt`. Source: m3.material.io, Colour roles ("Color roles are tokenized… Roles are implemented in design and code through tokens"); developer.android.com ("Never hardcode `Color(0xFF...)` — always reference a color role" is the standing guidance in the M3 theming docs).
- **R2.** Every text/icon colour is the `on-` role of the background it sits on (`primary`↔`on-primary`, `primary-container`↔`on-primary-container`, `secondary-container`↔`on-secondary-container`, `surface*`↔`on-surface`/`on-surface-variant`, `error-container`↔`on-error-container`). Check: for each screen, list every (background, foreground) pair and verify it is an intended pair. Source: m3.material.io, Colour roles ("apply colors only in the intended pairs").
- **R3.** Dynamic colour is on by default on Android 12+ and the app falls back to the static Stock scheme (P22-blue seed) below API 31 and when dynamic colour is off. Check: run on API 31+ with a colourful wallpaper and confirm the scheme changes; run with dynamic colour disabled and confirm the Stock scheme, not a default purple. Source: developer.android.com, Material Design 3 in Compose (dynamic colour + fallback).
- **R4.** The Stock scheme is generated from a single seed through the HCT pipeline (Theme Builder / material-color-utilities), with light *and* dark schemes exported — no role hand-tuned afterwards. Check: the theme file is an export (or a faithful transcription of one) from one seed; changing the seed regenerates everything. Source: developer.android.com ("Generate color schemes… The Material Theme Builder tool allows you to do this").
- **R5.** The dock uses `surface-container` and the search bar uses `surface-container-high` — and the two are never placed on backgrounds less than two container-steps apart. Check: screenshot the home screen; the search bar must be visibly distinct from the surface behind it. Source: m3.material.io, Search guidelines ("use surface container roles that are more than one step apart").
- **R6.** Dividers use `outline-variant`; input-field borders use `outline`. Check: inspect divider and text-field colours. Source: m3.material.io, Colour roles (outline do/don'ts).
- **R7.** Body text contrast ≥ 4.5:1 and icon/component-boundary contrast ≥ 3:1, in *both* light and dark schemes, and at standard, medium, and high contrast levels. Check: measure with a contrast checker against the actual resolved colours (not the seed). Source: developer.android.com, Accessibility; m3.material.io, Colour roles.
- **R8.** Text over wallpaper (icon labels, clock, At-a-Glance) is legible over the lightest *and* darkest wallpapers, via a scrim or a surface container — not by shadow alone. Check: set a white wallpaper, then a black wallpaper, then a busy photo; read every label. **[APP]** — the scrim mechanism is M3's (see §1.6); the wallpaper case is the launcher's application of it. Source: m3.material.io, Colour roles (scrim); developer.android.com, Accessibility (4.5:1).
- **R9.** Error colour is used only for errors/destructive states, nowhere decorative. Check: search for `error` role usage. Source: m3.material.io, Colour roles.
- **R10.** The phosphor glow is an additive effect on top of role-coloured elements, never the sole carrier of state or meaning, and never a substitute for a role. Check: turn the glow off — every state must still read. **[APP]** Source: project decision; M3's state-layer and colour-role rules.

## Typography

- **R11.** All text uses a type-scale role (`MaterialTheme.typography.*`); no hard-coded `fontSize` at call sites. Check: search for `.sp` literals outside the theme. Source: developer.android.com, Material Design 3 in Compose — Typography ("Your product will likely not need all 15 default styles… five sizes are chosen for a reduced set").
- **R12.** App-icon labels (home, dock, drawer, folders) are 12sp — `body-small` or `label-medium` — one role, used everywhere, single line, ellipsis. Check: measure label size in a screenshot; confirm one style across all four places. **[APP]** Source: developer.android.com, Accessibility (12sp floor); M3 type scale (body-small / label-medium = 12/16).
- **R13.** No text below 12sp anywhere in the app. Check: audit the smallest text (badge labels, captions, footnotes). Source: developer.android.com, Accessibility ("Don't make the body size any smaller than 12 sp").
- **R14.** All text sizes are in sp (font scaling works). Check: enable the system's largest font size; the UI must scale without clipping or overlap. Source: developer.android.com, Accessibility.
- **R15.** Button and chip text is sentence case, never ALL CAPS. Check: read every button label. Source: M3 component guidance.
- **R16.** Settings hierarchy is visible in type alone: screen title (headline), section titles (title), item labels (title-medium/body-large), descriptions (body-medium), captions (body-small/label). No two levels share a role. Check: screenshot Settings and list the roles in use. **[APP]** Source: M3 type-scale role definitions (§2.2).
- **R17.** Dialog headline is `titleLarge`; dialog supporting text is `bodyMedium`; dialog actions are text buttons. Check: open any dialog and verify. Source: M3 Dialog component attributes.

## Shape

- **R18.** Buttons, chips, badges, and nav indicators are `full` (pill); cards are `medium` (12dp); dialogs and expanded bottom sheets are `extra-large` (28dp); text fields, menus, and snackbars are `extra-small` (4dp); FAB is `large` (16dp). Check: measure corner radii in screenshots against this table. Source: m3.material.io, Shape; developer.android.com, Shapes.
- **R19.** Nested rounded containers use optical roundness (inner radius = outer − padding), never the same radius twice. Check: folder previews, widget frames in cards, search bar inside a sheet. Source: m3.material.io, Shape ("Use different corner radii values for nested components").
- **R20.** No large/full corners on information-dense components (cards, lists). Check: card and list screenshots. Source: m3.material.io, Shape (Caution).
- **R21.** The shape scale is not redefined for brand purposes; any custom radius is a recorded, deliberate override of a single component, not a new scale. Check: the theme's `Shapes` definition matches the M3 defaults (4/8/12/16/28) except for listed overrides. Source: m3.material.io, Shape (customisation rules).

## Elevation and surfaces

- **R22.** Depth is expressed by surface-container roles, not shadows: home = `surface`, dock = `surface-container`, search bar = `surface-container-high`, sheet/dialog = `surface-container-high`, dragged icon = highest level. Check: screenshot the home screen with shadows mentally removed — hierarchy must still read. Source: m3.material.io, Elevation; m3.material.io, Colour roles (surface-container mapping).
- **R23.** No component's default elevation is changed. Check: `tonalElevation`/`shadowElevation` overrides in code — each must have a recorded reason. Source: m3.material.io, Elevation ("Avoid changing the default elevation of Material components").
- **R24.** Press/hover raises elevation by exactly one level (FAB 3→4 on hover; buttons +1 on hover). Check: interact and observe. Source: m3.material.io, Elevation.
- **R25.** In dark theme, depth still reads (tonal tint, not shadow). Check: same hierarchy screenshots in dark mode. Source: developer.android.com, Elevation ("Elevation overlays in dark themes have also changed to tonal color overlays").
- **R26.** Modal sheets and dialogs sit over a 32% black scrim; the home screen is never scrimmed except behind a modal surface. Check: open a sheet/dialog and measure the scrim. Source: M3 scrim value (Dialog/BottomSheet attributes).

## Motion

- **R27.** Screen transitions use the easing table: enter = Emphasized decelerate 400 ms; exit (permanent) = Emphasized accelerate 200 ms; begin-and-end-on-screen = Emphasized 500 ms; small utility transitions = Standard 300/250/200 ms. Check: record the app's transitions and compare durations/curves to the table. Source: m3.material.io, Easing and duration.
- **R28.** Exits are faster than enters (200 ms vs 400–500 ms) everywhere. Check: every dismiss/close animation against its open animation. Source: m3.material.io, Easing and duration.
- **R29.** Duration scales with travel distance — a full-screen open takes longer than a chip appearing. Check: compare a folder-open animation with a tooltip/chip animation. Source: m3.material.io, Easing and duration.
- **R30.** The right pattern is used for each transition: container transform for one-thing-becomes-another (folder open, search bar → search view); shared axis for navigational steps (home → drawer, settings forward/back); fade-through for unrelated swaps; fade for in-bounds appear/disappear (dialogs, menus, badges). Check: name the pattern for each transition in the app. Source: developer.android.com, Material motion codelab; m3.material.io, "Building Beautiful Transitions with Material Motion".
- **R31.** Drags, flings, and sheet settling use springs (M3 Expressive motion), not fixed-duration tweens; screen transitions use the easing table, not springs. Check: drag a sheet/icon and feel for velocity-responsive settling; check that screen transitions have fixed durations. Source: M3 Expressive motion guidance.
- **R32.** Reduced-motion setting is honoured (transitions collapse to fades/instant). Check: enable "Remove animations" and repeat R27–R30. Source: platform accessibility guidance.
- **R33.** The launcher does not layer its own animation on top of system transitions (app open/close, overview, predictive back). Check: open/close an app and confirm a single clean handoff. **[APP]** Source: Material motion system (transitions must read as one continuous motion).

## State layers

- **R34.** Every pressable element shows press feedback: a state layer at 10% of the content colour and/or a touch-point ripple. No dead taps anywhere — including app icons, dock icons, drawer rows, settings rows, and chips. Check: press every element type and watch. Source: m3.material.io, State layers (press +10%); developer.android.com (ripple).
- **R35.** Hover (8%) and focus (10%) layers exist and are distinguishable from press. Check: hover with a mouse/trackpad; move focus with a keyboard/d-pad. Source: m3.material.io, State layers.
- **R36.** Dragged elements show the 16% drag layer. Check: drag an icon/widget. Source: m3.material.io, State layers.
- **R37.** Disabled elements read as disabled (38% treatment) and are not merely greyed text. Check: find a disabled control and verify. Source: m3.material.io, State layers.
- **R38.** The state layer is 40dp and the interactive target is 48dp — the feedback is smaller than the hit area, never larger. Check: small icon buttons (e.g. the search bar's trailing icons). Source: m3.material.io, State layers.
- **R39.** Selection is shown by a container/indicator change (e.g. `secondary-container` pill, filled icon), never by opacity alone. Check: selected states in drawer tabs, settings selections, folder contents. Source: M3 component behaviour (navigation-bar active indicator); m3.material.io, State layers (state layers indicate *state*, selection is a container change).

## Density, spacing, layout

- **R40.** Every interactive element has a ≥ 48×48dp touch target, including visually small icons (hit area expanded, icon not enlarged). Check: measure targets with layout inspection; tap-test the smallest icons. Source: m3.material.io, State layers ("the interactive target size is 48dp"); developer.android.com, Accessibility.
- **R41.** At least 8dp between adjacent touch targets. Check: dock icons, drawer rows, settings rows. Source: M3 accessibility guidance.
- **R42.** Screen margins are 16dp on phones (compact breakpoint) and 24dp on medium/expanded widths. Check: measure the home screen and settings margins on a phone and on a tablet/foldable. Source: m3.material.io, Breakpoints.
- **R43.** All spacing snaps to the 4dp/8dp grid — no 3dp, 5dp, 7dp, 10dp, or 13dp gaps. Check: audit padding/gap values in layout code. Source: M3 spacing scale (§7.2).
- **R44.** System icons are 24dp (20dp for secondary). Check: settings icons, search-bar icons, sheet icons. Source: M3 icon guidance.
- **R45.** Layout changes at breakpoints: search is full-screen on phones and docked on tablets/foldables; dialogs are full-screen only on compact; a second pane appears only at expanded widths. Check: rotate / unfold / run on a tablet. Source: m3.material.io, Search guidelines; Dialogs guidelines; Breakpoints.
- **R46.** The top 25% of large screens holds glanceable content, not primary controls. Check: tablet/foldable layouts. Source: m3.material.io, Breakpoints — Medium.

## Components

- **R47.** Search bar: `surface-container-high`, persistent, ≤2 trailing icons (avatar + at most one other), hint text describes what can be searched, focused state is full-screen on phones and docked on tablets, back releases focus, predictive back works. Check: each behaviour in turn. Source: m3.material.io, Search guidelines.
- **R48.** Bottom sheets: top corners 28dp, bottom square, drag handle present (32×4dp), modal sheets have a 32% scrim and block interaction, standard sheets don't. Check: every sheet in the app. Source: m3.material.io, Bottom sheets guidelines; developer.android.com, `BottomSheetDefaults`.
- **R49.** Dialogs: `surface-container-high`, 28dp corners, 24dp side insets, 32% scrim, text-button actions, used sparingly and only for blocking decisions. Check: every dialog; count how many exist — a launcher with dialogs everywhere is misusing them. Source: m3.material.io, Dialogs guidelines; M3 Dialog attributes.
- **R50.** Chips: small (8dp) shape; selected filter chip is `secondary-container`; chips do one discrete thing. Check: search filters. Source: M3 shape/component mapping; developer.android.com (filter chips → secondary roles).
- **R51.** Switches: selected = `primary`, unselected track = `surface-container-highest` + `outline`; 48dp target. Sliders: active track/thumb = `primary`, inactive track = `surface-container-highest`, value shown. Check: Settings. Source: M3 Switch/Slider tokens; m3.material.io, Colour roles.
- **R52.** No FAB floats over the home screen unless the home screen has exactly one primary action it represents. Any FAB elsewhere is `primary-container`, large (16dp) shape, level-3 elevation. Check: justify every FAB or remove it. **[APP]** Source: M3 FAB guidance (§8.6).
- **R53.** The dock is not styled as a navigation bar: no active-indicator pill on dock icons, no "selected destination" state. Check: dock visuals. **[APP]** Source: M3 Navigation bar rules (§8.7).
- **R54.** Notification badges: small dot (6dp) for unread, large badge (16dp high) with a count capped at 4 characters ("99+"), error-red by default, anchored to the icon's top-trailing corner, hidden when the app is opened. Check: set counts of 1, 9, 99, 127 on a test app and screenshot. Source: m3.material.io, Badges guidelines/specs.

---

# The 10 rules most likely to matter for a stock-feeling launcher

These are the ten rules from above that most separate "a launcher that feels stock on a Pixel" from one that doesn't. If the rebuild gets only ten things right, get these:

1. **R2 — Pair every colour with its `on-` role.** Broken pairings are the fastest way to look wrong: illegible text, buttons that fail at high contrast, a theme that falls apart the moment the wallpaper changes.
2. **R3 — Dynamic colour on, Stock scheme as the fallback.** A Pixel launcher that doesn't theme from the wallpaper isn't a Pixel launcher; a launcher with no coherent static fallback isn't finished.
3. **R8 — Labels legible over any wallpaper.** The home screen *is* text over a photo. If the labels only work on the default wallpaper, the home screen is broken for every user who changes it.
4. **R12 — One 12sp label style for all icons.** Mixed label sizes are the most visible amateur tell on a home screen — the eye catches it before anything else.
5. **R40 — 48dp touch targets everywhere.** A launcher is touched hundreds of times a day; a single 40dp target that mis-taps is felt constantly.
6. **R18 — Shape discipline (pills, 12dp cards, 28dp sheets/dialogs).** Shape is how the user tells a button from a card from a sheet at a glance. One wrong radius and the taxonomy collapses.
7. **R22 — Tonal elevation, not shadows.** Heavy drop shadows are the clearest Material 2 tell. Depth must come from surface-container steps.
8. **R34 — Press feedback on everything, from the touch point.** A home screen with dead-feeling icons feels broken even when it works — the ripple is the handshake.
9. **R27/R28 — Emphasized easing, exits faster than enters.** Motion is where "stock" is felt rather than seen: 400 ms in, 200 ms out, decelerate in, accelerate out. Linear or symmetric timing feels immediately off.
10. **R54 — Badges done properly (6dp dot / 16dp count, max "99+", top-trailing corner, error red).** Badges are on screen all day, on every icon; a wrong badge is a wrong home screen.

---

# Part IV — Rationale, evolution, and the talks (the insider-level background)

*This section answers "why is M3 the way it is?" — the reasoning, history, and practitioner writing behind the rules in Parts I–III. Everything here is from public sources, cited. It is background for judgement calls, not a second rulebook: where it conflicts with the spec pages, the spec pages win.*

## 1. Timeline — four generations

| Year | What happened | Why it matters to Marlow |
|---|---|---|
| **2014** | **Material Design** introduced at Google I/O (Matías Duarte). Pixels as 3D material: "thinking of pixels not just as 2D spots of color, but as 3D cubes with depth and weight" — surfaces as digital paper and ink, realistic light, shadow, and movement showing "how objects interact". (Gizmodo, I/O 2014 coverage) | The founding metaphor. Elevation, shadows, and "authentic motion" all descend from this. Marlow's warmth/CRT identity is a *surface* choice inside this metaphor, not a rejection of it. |
| **2018** | **Material Design 2** (Material Theming): the system becomes themeable — fixed palette of 19 hues, 10-step scales, shape theming. | M2 is the baseline M3 defines itself *against* (see the "Differences from M2" notes on the Elevation, Search, and Shape pages). Knowing M2 is how you recognise M2 tells in Marlow (shadows everywhere, square search, bottom navigation). |
| **2021** | **Material 3 / Material You**, announced at Google I/O 2021 (May 18) with Android 12. Duarte: "Instead of form following function, what if form followed feeling?" Creative lead Christian Robertson; Android lead Sameer Samat. First on Pixel that fall. (Engadget, Android Authority, MakeUseOf — I/O 2021 coverage) | The generative turn: colour from the user, not from Google. A launcher is the *primary* surface where this plays out — the home screen is where wallpaper, widgets, and system theme meet. |
| **2025** | **Material 3 Expressive**, announced at The Android Show: I/O Edition (May 13, 2025) and at Google I/O 2025; shipped on Pixel with Android 16 QPR1 (September 2025). Spring motion, emphasized type, shape library, blur, Live Updates. (blog.google, May 13 2025; Android Authority deep dive) | The current bar. "Stock" in 2026 means Expressive: springy, bolder, more emotional — not the restrained 2021 baseline. |

## 2. Why M3 differs from M2 — the reasoning, not just the diff

**Colour: from choosing to generating.** M2 handed the designer a menu (19 hues, fixed scales) and the designer picked. M3 hands the system one seed and derives everything. The reason, in the words of James O'Leary — design engineer on the Android team and one of Material You's architects, interviewed by Android Police (Feb 17, 2022) — is that the real problem was never "pick pretty colours", it was **contrast**: "fewer still can choose those colors knowing that, when it comes to contrast, text and buttons will always be perfectly legible no matter how they're used or in what combination. But that's what Material You does." The generative system exists so that legibility is a *property of the derivation*, not a check run at the end. His analogy for the pre-M3 situation: designers were "bound by this contrast rule, they didn't have the tools to be able to express that rule until it was almost too late. By the time you're doing contrast checking and accessibility, you're getting ready to ship the software… We needed to make contrast and tone part of the system in order to unlock possibilities past this stasis of flat design." And: "fish don't talk about how they're swimming around in water" — tone had to become part of the water. (Android Police, "Material You dynamic theming is Android 12's biggest feature, and here's exactly how it works", interview with James O'Leary)

**Why a whole new colour space.** The team did not set out to invent one. They started wanting "an automated system that could choose colors" and discovered the available spaces couldn't do the job: HSL (1970s, built for RGB) is perceptually wrong — colours at HSL lightness 50 can read as 30 or 90 to a human eye. CIELAB's L* was better but "spatially limited… inconsistent results for designers" (notably stretched-out blues and coverage gaps). CAM16 was close enough to borrow hue and chroma from, with L* bolted on as "tone". Hence **HCT: Hue, Chroma, Tone** — CAM16 hue/chroma + CIELAB L* lightness. The payoff, stated in the HCT documentation and analyses of the open-source implementation: **a tone difference of 40 guarantees contrast ≥ 3.0, and a difference of 50 guarantees ≥ 4.5** — so picking two tones far enough apart *is* the contrast check, no calculation required. (Android Police interview with O'Leary; colors.dotui.org, "Material 3 & HCT"; material-color-utilities source)

**How Monet picks the seed.** The wallpaper pipeline (codenamed **Monet**, running in SystemUI): quantize the wallpaper to 256 representative colours (the same family of algorithms that reduce colours in a GIF), score each candidate twice — once for colourfulness, once for resemblance to the image's overall hue (a population component, so a single vivid pixel can't win) — filter out near-greys, and present the top scorers as the source-colour choices. From the seed, tonal palettes are generated and roles assigned, "all with a contrast ratio that already meets accessibility requirements". (Android Police interview with O'Leary)

**The forcing function was the Pixel 6.** "We felt there was a really powerful moment with the Pixel brand this year between the Tensor CPU, this gorgeous new device, and we wanted to have the software and the design play into this moment… we were going to go all in and make it happen this year." Material You had been explored "for years and years" before that — from "merely dynamic app colors down to the level of system integration" — but the Pixel 6 moment is what shipped it. (Android Police interview with O'Leary)

**Elevation: from shadow to tone.** M2's depth cue was the shadow, which fails in dark themes (shadows are invisible on dark backgrounds) and which every app rendered slightly differently. M3's answer is to make the *surface colour itself* the depth cue, with the primary-colour tint deepening with height — a cue that works identically in light and dark, and that is identical across apps because it is a token, not a rendering choice. Shadows survive only as a secondary cue "when required to create additional protection against a background or to encourage interaction". (m3.material.io, Elevation; developer.android.com, Elevation)

**Motion: from utilitarian to expressive — and then to springs.** M2's easing was utilitarian; M3's is "more expressive… snappy take offs and very soft landings" with slightly longer durations. Expressive (2025) replaces fixed durations with **spring physics** for interactive motion, because springs respond to the user's actual input velocity instead of playing a canned curve. The through-line across all three generations is the same idea from the 2014 launch: motion must be *authentic* — "Tangible surfaces don't just appear out of nowhere like a jump-cut in a movie; they move into place helping to focus attention, establish spatial relationships and maintain continuity" (Banes & Butcher, 2014, below).

**The honest ceiling O'Leary states.** Automation does not remove design: "There's still that place for design, and it's probably the most important part of this… The perfect contrast is just black on white — we can just make our phones black on white and completely optimize the algorithms… But though the computational part is important, that doesn't mean the design part goes away. If anything, it's more important and more of a challenge. Because now these designers have to think in a formula, engaging with engineers type of world, where before you could just go to Figma, pick the red you liked, and you were done." And his favourite thing about Material You after "well over a year" working on it: "It shipped" — and that a phone can now feel "alive… both really elegant and deeply designed, and also… joyful". (Android Police interview with O'Leary)

## 3. Material 3 Expressive — the research behind the current direction

Expressive is the most researched update to the system Google has ever shipped, and its research is the best evidence for *why* the current bar looks the way it does:

- **Origin:** a 2022 conversation sparked by a research intern studying user sentiment toward Material Design in Google apps — "Why did so many apps look alike? Couldn't design evoke more feeling?" — debated, per the account, in a Munich beer hall. Three years of collaborative inquiry across research, design, and engineering followed. (design.google, "Expressive Design: Google's UX Research"; 9to5Google, May 5 2025)
- **Scale:** **46 separate research studies, hundreds of designs, more than 18,000 participants worldwide**, using eye tracking, surveys and focus groups, sentiment experiments, and usability testing. (design.google; 9to5Google; Android Authority)
- **Findings:** expressive designs were preferred by people of all ages — up to **87% among 18-to-24-year-olds**; participants spotted key UI elements **up to 4× faster** in Expressive designs (eye-tracking); and Expressive boosted perceived brand "coolness" — a **32% increase in subculture perception** ("relevant and in-the-know"). The emotional attributes tested were "playful", "energetic", "creative", "friendly", "positive vibe". Notably, even the "totally wild and way-too-playful" outer-limit designs were participants' preferred screens — which is what pushed the team to double down rather than pull back. (design.google; Dezeen, May 28 2025, quoting Google Design VP Vanessa Cho; 9to5Google)
- **What Expressive is, in one line:** "the fundamental parts of expressive design are the use of color, shape, size, motion, and containment" — used boldly to draw attention to key elements and group related ones. (design.google)
- **What shipped:** springy motion ("when you dismiss a notification, the others next to it subtly respond to your drag… you feel a satisfying haptic rumble"), subtle blur in the shade for depth, updated dynamic-colour themes, emphasized typography, customizable Quick Settings, Live Updates for progress notifications, and on Wear OS, motion that "traces the curvature of the display". Announced May 13, 2025 at The Android Show: I/O Edition; on Pixel with Android 16 QPR1, September 2025. (blog.google, May 13 2025; Android Authority deep dive)
- **Implication for Marlow:** the "stock" look is no longer restrained minimalism. A launcher that is technically M3-correct but emotionally flat will read as dated against Expressive — but Expressive's own research says the emotion must *serve* usability (4× faster spotting is a usability result, not a decoration result). Marlow's warmth, glow, and phosphor blue are compatible with this direction; a timid, grey, flat home screen is not.

## 4. The talks — Google I/O and Android developer sessions

These are the sessions that established the rules in this document. Listed chronologically; where a speaker is named in the source, they are named here — where the source does not name one, none is invented.

| Year / event | Session | What it establishes | Speakers (where sourced) |
|---|---|---|---|
| I/O 2014 | Google I/O keynote — Material Design introduction | The founding metaphor: surfaces as digital paper/ink, light, surface, movement conveying how objects interact; a single system across phone, watch, car, TV. | Matías Duarte (VP of Design) |
| I/O 2021 (May 18) | Google I/O keynote — Android 12 / Material You | Material You announced: "form followed feeling"; colour extraction from wallpaper; personalization as co-creation; redesigned widgets; more fluid motion. "Material You is a multi-year journey." | Matías Duarte; Christian Robertson (Creative Lead); Sameer Samat (Android) |
| I/O 2021 | "What's new in material design" (technical session) | The developer-facing detail of Material You: the M3 system, components, and theming for Android 12. Referenced in the Android 12 Beta 2 update as the session to watch. | — (not named in sources consulted) |
| I/O 2022 (May 11–12) | Material Design at I/O '22 — sessions & workshops | "Let's get personal: Designing accessibility for individuals" — how M3 features enable *personalized* accessible design. "Create expressive and readable typography with variable fonts" + "Migrating to Variable Fonts" workshop — variable fonts and the newly launched Material Symbols. "Designing apps for large screens" — form-factor-specific design. Codelabs: "Designing with accessible colors" (contrast checking with Theme Builder) and "Making accessible apps with Material 3 and Jetpack Compose". | Karen Ng, Julia Feldman (accessibility); Ivy Knight (variable fonts); André Labonté (large screens) |
| I/O 2023 (May 10) | "What's new in Material Design" | "An overview of the latest evolution to our design language, Material You, and a look at our vision of the future of design systems… new components and features… motion capabilities, new color customization features, Android and Flutter library updates, and design tooling." | — (not named in sources consulted) |
| I/O 2023 | "What's new in Jetpack Compose" | Compose for TV, Material 3 1.1 stable, first Glance beta — the library surface Marlow builds on. | — |
| I/O 2024 (May 14) | "Faster, more expressive design with Material for Compose" | "The latest from Material Design, including color and typography updates that empower developers to create personalized, accessible products, and a more comprehensive Compose offering" — plus a sneak peek at Google Maps shipping Material for Compose to production. | — (not named in sources consulted) |
| I/O 2024 | "Build Android widgets with Jetpack Glance" | Using Compose skills to build high-quality home-screen widgets (Glance) — directly relevant to Marlow's widgets. | — |
| May 13, 2025 | **The Android Show: I/O Edition** (YouTube) | Material 3 Expressive unveiled to developers; Sameer Samat (President, Android Ecosystem) and the Android team; "a fresh and dynamic look and feel". The blog post promises the I/O talk the following week would "dive into this in more detail". | Sameer Samat and the Android team |
| I/O 2025 | Material 3 Expressive technical sessions ("What's new in Android" and the Expressive deep dive) | The implementation detail of Expressive: new components, motion-physics system, type styles, colours, shapes; Wear OS 6 with Expressive; Live Updates. | — (not named in sources consulted) |

Sources: m3.material.io, "Join Material Design at I/O '22" (https://m3.material.io/blog/material-google-io22); 9to5Google, "Google shares I/O 2023 session list" (https://9to5google.com/2023/04/27/google-i-o-2023-sessions/) and "Google shares I/O 2024 session list" (https://9to5google.com/2024/04/25/google-i-o-2024-sessions-list/); Android Developers Blog, "The Android Show: I/O Edition" (http://android-developers.googleblog.com/2025/05/the-android-show-io-edition.html); Android Developers Blog, "Android 12 Beta 2 Update" (https://android-developers.googleblog.com/2021/06/android-12-beta-2-update.html); Engadget / Android Authority / MakeUseOf I/O 2021 coverage.

**How to use this table:** when a rule in Parts I–III is disputed during the rebuild, the talk that introduced it is the place to check the *intent*. In particular: I/O 2022's accessibility sessions are the source for user-controlled contrast and personalized accessibility (the reason R7 tests three contrast levels); I/O 2024's Compose session is the source for the current component set; and the 2025 Expressive sessions are the source for springs and emphasized type (the reason R31 separates spring motion from eased transitions).

## 5. Design-engineering writing from the people who built it

- **Chris Banes & Nick Butcher, "Implementing Material Design in Your Android app", Android Developers Blog, October 2014** (https://android-developers.googleblog.com/2014/10/implementing-material-design-in-your.html). The original practitioner guide, written by two Android Developer Relations engineers as Lollipop launched: tangible surfaces ("UIs are composed of pieces of digital paper & ink"), the `elevation` property and real-time dynamic shadows, dynamic colour from rich images (the Palette API — the ancestor of Monet), "authentic motion" ("Materials respond to touch to confirm your interaction and all changes radiate outward from your touch point"), shared-element activity/fragment transitions, ripples, `StateListAnimator` ("materials also respond to touch by raising up to meet your finger, like a magnetic attraction… `Z = elevation + translationZ`"), and circular reveal. Nearly every idea in this document is a descendant of something in this post — including the state-layer/ripple rules and the elevation model. Read it to see which 2014 ideas survived (ripples, shared elements, authentic motion) and which were replaced (real-time shadows → tonal elevation).
- **Nick Butcher, "Building Meaningful Motion", Droidcon UK 2015; and "Animatable — Beautiful Meaningful Animations", Droidcon NYC (reported 2016).** Butcher's talks, built around Plaid (his demo app for showcasing Material Design): advanced animation practice, including a bottom-sheet implementation driven by `onTouchEvent` deltas and `ViewDragHelper` with nested-scroll handoff (one gesture expands the sheet, then scrolls the content — the exact interaction Marlow's sheets need), and a text shared-element transition built by recording start/end `StaticLayout`s, splitting text into unbroken "runs", and cross-fading bitmaps per run (because animating font size invalidates the glyph cache). The durable lesson: **never animate font size; animate bitmaps/bounds/opacity instead** — directly relevant to Marlow's folder-open and search-expand transitions. (Reported in https://gnuf.dev/droidcon-nyc-day-2/)
- **Mike Wolfson, "Mastering Material Motion", Droidcon NYC.** The four principles of Material motion as published by Google that year: **responsive** (the user drives motion), **natural** (movement informed by real-world forces; durations tuned to screen size — tablets longer, wearables shorter, relative to phones), **aware** (material is aware of its surroundings), and **intentional** (motion guides focus to the right spot; avoid animating more than two elements at a time, or elements in different directions). The ancestor of the easing/duration rules in §5. (Reported in https://gnuf.dev/droidcon-nyc-day-2/)
- **"Building Beautiful Transitions with Material Motion for Android", m3.material.io blog / Android Developers codelab** (https://m3.material.io/blog/android-material-motion; https://developer.android.com/codelabs/material-motion-android). The canonical statement of the four transition patterns (container transform, shared axis, fade through, fade) worked through the Reply email app: container transform for list→detail and FAB→compose, shared z-axis for search icon→search view, fade-through between mailboxes, container transform for chip→card. The codelab's Reply app is the worked example behind §5.3's launcher mapping.
- **James O'Leary (design engineer, Android team; Material You architect), interviewed by Ryne Hager, Android Police, Feb 17, 2022** (https://www.androidpolice.com/google-material-you-interview-james-oleary/). The fullest public account of *why* HCT exists, how Monet scores wallpaper colours, why the Pixel 6 forced the schedule, and the design-vs-computation tension. Quoted throughout §2 above. The single most useful background read for anyone judging Marlow's colour system.
- **Material Color Utilities — the open-source implementation** (`material-foundation/material-color-utilities`, Apache 2.0; mirrored at chromium.googlesource.com). Read `DynamicScheme.java` / `Hct` to see the system as code rather than prose: a scheme is built from a source colour + a **variant** (`tonal-spot` is the default — the Android Material You variant; others include `content`, `expressive`, `fidelity`, `vibrant`, `monochrome`, `neutral`, `fruit-salad`, `rainbow`), a dark flag, and a **contrast level** (−1 to 1, where 0 is standard, −1 minimum contrast, 1 maximum). The variant list is the menu Marlow's theme picker is secretly choosing from: "Material You" = `tonal-spot`; a "vibrant" or "expressive" option in Marlow's theme settings would be a different variant of the same engine, not a different engine. Source: https://chromium.googlesource.com/external/github.com/material-foundation/material-color-utilities/+/refs/heads/upstream/main/java/dynamiccolor/DynamicScheme.java
- **"Expressive Design: Google's UX Research", design.google** (https://design.google/library/expressive-material-design-google-research). The Material research team's own account of the Expressive programme — the 46 studies, the methods, the emotional attributes, the 4×-faster finding. Read alongside the Dezeen interview with Google Design VP Vanessa Cho (https://www.dezeen.com/2025/05/28/google-ushers-in-age-of-expressive-interfaces-with-material-design-update/) for the "wild and way-too-playful" finding and what it means.
- **"Designing with accessible colors" codelab** (https://codelabs.developers.google.com/color-contrast-accessibility) and **"Adding dynamic color to your app" codelab** (https://codelabs.developers.google.com/codelabs/apply-dynamic-color). The hands-on companions to §1: the first teaches contrast checking with Theme Builder (WCAG AA 4.5:1 / 3:1 tables); the second walks through `DynamicColors.applyToActivitiesIfAvailable`, the colour-role groups, and the five tonal palettes (system_accent1/2/3, system_neutral1/2).

## 6. What this background changes for Marlow's rebuild

1. **The colour system is a contrast machine, not a palette.** Judge Marlow's Stock theme the way O'Leary's team would: pick any two roles that touch, measure the tone gap, and check the ratio. If the warm backgrounds or the P22 accent ever break a pairing, the fix is in the *derivation* (seed/variant), never a one-off hex tweak — because a one-off tweak breaks the moment dynamic colour or a contrast level changes it.
2. **Expressive is the direction; restraint is not the goal.** The research says users — especially younger users — prefer bolder, more emotional interfaces *and* complete tasks faster in them. Marlow's warm CRT identity and phosphor glow are assets in that frame, not deviations to apologise for. What would be a deviation is a flat, grey, cautious home screen.
3. **Motion is the cheapest stock-feel win and the easiest to get wrong.** The easing table (§5.2) and the four patterns (§5.3) are exact numbers, not taste. A launcher that gets them right feels stock even before its colours are judged; a launcher with linear or symmetric timing feels wrong even with perfect colours.
4. **The launcher is where Material You is most visible — and most fragile.** The home screen is the one surface where the wallpaper (the dynamic-colour source) is also the background (the legibility problem). That collision is Marlow's central design problem, and M3's answer to it is scrims and surface containers (§1.6, R8), not shadows or hope.

---

# Sources consulted

**Official — Material Design site (m3.material.io)**

- Colour roles — https://m3.material.io/styles/color/the-color-system/key-colors-tones
- Dynamic colour overview — https://m3.material.io/styles/color/dynamic-color/overview
- Typography overview — https://m3.material.io/styles/typography/overview
- Shape / corner radius scale — https://m3.material.io/styles/shape/corner-radius-scale
- Elevation — https://m3.material.io/styles/elevation
- Easing and duration — https://m3.material.io/styles/motion/easing-and-duration
- State layers — https://m3.material.io/foundations/interaction/states/state-layers
- Search guidelines — https://m3.material.io/components/search/guidelines
- Breakpoints: compact / medium / expanded — https://m3.material.io/foundations/layout/breakpoints/compact (and `/medium`, `/expanded`)
- "Building Beautiful Transitions with Material Motion for Android" — https://m3.material.io/blog/android-material-motion
- "Join Material Design at I/O '22" — https://m3.material.io/blog/material-google-io22

**Official — Android Developers**

- Material Design 3 in Compose — https://developer.android.com/develop/ui/compose/designsystems/material3
- Accessibility (mobile design guidance) — https://developer.android.com/design/ui/mobile/guides/foundations/accessibility
- Material motion codelab — https://developer.android.com/codelabs/material-motion-android
- `BottomSheetDefaults` API reference — https://developer.android.com/reference/kotlin/androidx/compose/material3/BottomSheetDefaults
- `Badge` / `BadgedBox` API reference — https://developer.android.com/reference/kotlin/androidx/compose/material3/BadgedBox.composable
- Android Developers Blog: "Android 12 Beta 2 Update" — https://android-developers.googleblog.com/2021/06/android-12-beta-2-update.html
- Android Developers Blog: "The Android Show: I/O Edition" — http://android-developers.googleblog.com/2025/05/the-android-show-io-edition.html
- Android Developers Blog (2014): Chris Banes & Nick Butcher, "Implementing Material Design in Your Android app" — https://android-developers.googleblog.com/2014/10/implementing-material-design-in-your.html

**Official — Google (blogs, research, code)**

- Google (The Keyword) blog: "Google launches Material 3 Expressive redesign for Android, Wear OS devices", May 13, 2025 — https://blog.google/products-and-platforms/platforms/android/material-3-expressive-android-wearos-launch/
- Google Design: "Expressive Design: Google's UX Research" — https://design.google/library/expressive-material-design-google-research
- Material Color Utilities (open source) — `DynamicScheme.java`, chromium mirror: https://chromium.googlesource.com/external/github.com/material-foundation/material-color-utilities/+/refs/heads/upstream/main/java/dynamiccolor/DynamicScheme.java
- Google codelabs: "Designing with accessible colors" — https://codelabs.developers.google.com/color-contrast-accessibility ; "Adding dynamic color to your app" — https://codelabs.developers.google.com/codelabs/apply-dynamic-color

**Secondary (used only where flagged, or for corroboration)**

- Android Police (Ryne Hager), "Material You dynamic theming is Android 12's biggest feature, and here's exactly how it works" — interview with James O'Leary, Feb 17, 2022 — https://www.androidpolice.com/google-material-you-interview-james-oleary/
- colors.dotui.org, "Material 3 & HCT" — https://colors.dotui.org/material-hct
- xckevin.github.io, "Android Material 3 Dynamic Color: Monet, HCT, and Compose" — https://xckevin.github.io/en/blog/android-material3-dynamic-color-monet/
- 9to5Google, "Google shares I/O 2023 session list" — https://9to5google.com/2023/04/27/google-i-o-2023-sessions/ ; "Google shares I/O 2024 session list" — https://9to5google.com/2024/04/25/google-i-o-2024-sessions-list/ ; "Leak: How and why Google made Material 3 Expressive" — https://9to5google.com/2025/05/05/material-3-expressive-leak/
- Android Authority, "Material 3 Expressive deep dive" — https://www.androidauthority.com/google-material-3-expressive-features-changes-availability-supported-devices-3556392/
- Dezeen, "Google ushers in 'expressive' interfaces with Material Design update", May 28, 2025 — https://www.dezeen.com/2025/05/28/google-ushers-in-age-of-expressive-interfaces-with-material-design-update/
- Gizmodo, "What Material Design Means For the Future of Android" (I/O 2014) — https://gizmodo.com/what-material-design-means-for-the-future-of-android-1595976211
- Engadget, "Material You is a colorful, personalized redesign for Android and more" (I/O 2021) — https://www.engadget.com/google-material-you-android-personalized-redesign-182501466.html
- 9to5Google, "Here's a look at the new Material Design 3 UI components" (Oct 2021) — https://9to5google.com/2021/10/28/google-material-design-3/
- gnuf.dev, "Droidcon NYC: Day 2" (Wolfson / Butcher motion talks) — https://gnuf.dev/droidcon-nyc-day-2/
- Material-components-android Dialog reference (raw GitHub) — https://raw.githubusercontent.com/material-components/material-components-android/master/docs/components/Dialog.md
- M3 badges guidelines / specs (via search) — https://m3.material.io/components/badges/guidelines , https://m3.material.io/components/badges/specs
- M3 dialogs guidelines / bottom-sheets guidelines (via search) — https://m3.material.io/components/dialogs/guidelines , https://m3.material.io/components/bottom-sheets/guidelines
- Conformance write-up reporting the May 2026 foundations revision (breakpoints rename; spacing tokens) — https://code.sigidli.com/mantra/mantra-kmp/src/commit/26fef1f3319c566dfc2acc706d22ffc069e4481e/docs/material-design-conformance.md
- Type-scale weight corroboration — https://astordev.github.io/articles/material-typography/ ; https://extensionbooster.net/blog/android-typography-color-system-material-design-best-practices/

*End of file. No credentials, employment history, or qualifications are claimed anywhere in this document. Every load-bearing claim carries a citation; secondary sources are flagged as such.*
