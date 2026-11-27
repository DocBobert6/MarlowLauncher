# 06 — Google I/O and Android developer talks, plus design-engineering writing from people who built launchers

**Project:** Marlow Launcher — a Pixel-style Android home launcher, package `app.launcher`, Compose + Material 3.
**Purpose:** What Google's own talks/docs and working launcher builders actually established, stated as concrete takeaways a launcher builder can test against Marlow. Research only — no code was touched.
**Researched:** 2026-11-26. Sources were found with web search and read as fetched page text. Where a talk's exact video ID or speaker list could not be verified from a returned source, this file says so instead of guessing it — do not treat a title/year below as a video link unless a URL is given.
**Guardrail honoured:** No fabricated credentials, employment history, or education claims appear in this document. No claim is made here that anyone on the Marlow project worked at Meta, Google, Apple, or on the original Android team, and no invented qualifications are stated. People are identified only as their sources identify them (e.g. a blog byline, a Play Store developer name, a GitHub release author).

---

## How to read this file

- **Part A — Talks (chronological):** Google I/O, Android Dev Summit, and the Android Developers Blog talks/posts that bear directly on a launcher. Each entry gives year/title, what it establishes, and a concrete takeaway for Marlow.
- **Part B — Developer documentation and blog posts:** the standing docs a launcher host must follow (widget design, shortcuts, icons, dynamic colour, gesture/back, large screens).
- **Part C — Design-engineering writing from people who built launchers:** Nova, Niagara, Lawnchair, Smart Launcher, Microsoft Launcher — blog posts, changelogs, and release notes, by source.
- **Part D — Checkable rules:** numbered, testable against Marlow, each tied to the talk/post that justifies it.
- **Part E — Top 10 takeaways** most likely to matter for a stock-feeling launcher.

A note on scope: Google publishes very few talks *about launchers as such*. Most launcher-relevant guidance arrives sideways — in widget talks, gesture-navigation talks, Material You talks, and large-screen talks. That is itself a finding: the launcher is the host for other people's widgets, icons, shortcuts, and gestures, so "launcher quality" is mostly "host quality" (see Rule R1).

---

# Part A — Talks (chronological)

## 2017 — "Designing Adaptive Icons" (Android Developers Blog, Nick Butcher)

- **Source:** Android Developers Blog post "Designing Adaptive Icons", linked from the current adaptive-icon documentation. The documentation itself cites it as the design reference.
- **URL (docs page that cites it):** https://developer.android.com/develop/ui/compose/system/icon_design_adaptive?authuser=4
- **What it establishes:** An app icon is not one bitmap. It is two layers — foreground and background, each 108×108 dp — plus a mask the system/OEM supplies, plus (from Android 13) an optional monochrome layer for themed icons. Only the inner 66×66 dp "safe zone" is guaranteed visible under every mask; the outer 18 dp on each side is reserved for masking and for parallax/pulsing effects. Layers must have clean edges, no baked-in mask, and no background shadow around the outline.
- **Takeaway for Marlow:** Marlow is a *renderer* of adaptive icons, not just a consumer of them. It must apply one consistent mask to every icon, never clip the safe zone, never add its own shadow inside the mask, and must honour the monochrome layer when themed icons are on. If Marlow invents its own per-icon shape, it breaks the contract every icon designer was told to rely on.

## 2019 — "Dark Theme & Gesture Navigation" (Google I/O '19)

- **Source:** Google I/O 2019 talk, referenced in the Android Developers Blog post releasing the Google I/O 2019 app source code.
- **URL (blog post referencing it):** https://android-developers.googleblog.com/2019/08/google-releases-source-code-for-google.html?m=0
- **What it establishes:** Android 10 (Q) introduced fully gestural navigation. App developers must do two things: (1) extend content edge-to-edge, drawing behind the status and navigation bars; (2) handle conflicting app gestures. The I/O 2019 app was one of the first apps to do both, and its source was published as the worked example.
- **Takeaway for Marlow:** A launcher cannot opt out of gesture navigation — it *is* the surface gestures land on. Home, back, and recents all start or end here. Marlow must draw edge-to-edge, keep tappable content out of the system-gesture zones unless it deliberately claims them, and never assume a three-button bar exists.

## 2019 — "Going Edge-to-Edge with Gesture Navigation" (Android Dev Summit 2019, Chris Banes and Rohan Shah)

- **Source:** Android Dev Summit 2019 schedule, 23 October 2019, 14:25–14:45, Main Stage, UI track.
- **URL (schedule):** https://developer.android.com/dev-summit/schedule/day1?nav=true
- **What it establishes (from the schedule abstract):** "To aid developers with creating immersive experiences, Android 10 adds new system navigation models that allow apps to take over more of the user's screen. This talk looks at how apps should work with the new gesture navigation, and ways to mitigate gesture conflicts. We also look at how to update apps to go edge to edge, drawing behind the system bars to create an immersive experience for users."
- **Takeaway for Marlow:** This is the canonical launcher-relevant gesture talk. Its two mitigations map directly onto launcher surfaces: (a) edge-to-edge drawing with insets applied to content, not to the wallpaper; (b) gesture-exclusion rects (`View.setSystemGestureExclusionRects()`) only where a horizontal swipe is genuinely the launcher's own gesture — e.g. a home-screen page swipe starting at the very edge — and never for the home gesture, which is the user's only exit from an app. Over-claiming exclusion rects is a stock-feeling bug, not a feature.

## 2021 — Google I/O 2021: Android 12, Material You, and the widget overhaul (keynote + "What's new in Android" sessions)

- **Sources:**
  - Google blog, "Widgets just got better on Android 12", 28 Oct 2021: https://blog.google/products-and-platforms/platforms/android/widgets-just-got-better-android-12/
  - XDA Developers, "Google I/O 2021: New improvements to widgets in Android 12 announced": https://www.XDA-Developers.com/google-android-12-widgets-overhaul/
- **What it establishes:** Android 12 was the biggest widget change in years, announced at I/O 2021 and shipped that autumn:
  - Widgets became resizable with responsive layouts — "as you resize, your widgets will change. If you make your widgets bigger, you'll get more functionality."
  - Widgets gained dynamic controls — checkboxes, radio buttons, switches usable *without opening the app* (previously widgets were largely informational; tapping opened the app).
  - The widget picker gained responsive previews for widgets of different sizes.
  - Widgets adopted rounded corners with padding, dynamic colour from the wallpaper (Material You colour extraction), and the Google Sans / flat-icon / cohesive-palette visual language.
  - The required configuration step when placing a widget was removed, and a new API was added to build backward-compatible widgets.
  - Google Photos Memories was the first widget to use the new freeform widget frames in Material You.
  - A reconfigure button lets users edit a widget's content/style without deleting it and starting over.
  - Launchers can provide a more uniform look for *all* widgets — including ones whose developers never updated — by masking them consistently (with the noted trade-off that transparent widgets may be masked automatically).
- **Takeaway for Marlow:** This is the single most important I/O for a Pixel-style launcher. The stock feeling *is* the Android 12 widget contract: rounded corners + padding enforced by the host, responsive resize that actually reflows content, a picker that shows truthful previews at multiple sizes, and dynamic colour applied uniformly. A launcher that lets widgets draw square-cornered to the cell edge, or that shows a static preview that does not match the placed widget, feels pre-2021 no matter how modern its own UI is.

## 2021 — Material You (Google I/O 2021, Android 12)

- **Sources:**
  - AOSP, "Material You design": https://source.android.com/docs/core/display/material?hl=en
  - Codelab, "Adding dynamic color to your app": https://codelabs.developers.google.com/codelabs/apply-dynamic-color
- **What it establishes:** Material You (Android 12) centres on dynamic colour: one source colour extracted from the wallpaper expands into 5 tonal palettes × 13 tonal variants = 65 colour attributes, applied consistently across System UI and apps. The AOSP page is explicit that partners should use the AOSP colour-extraction logic so the palette is consistent ecosystem-wide, and use the same palette for System UI and OEM apps. The codelab teaches the app-side mechanics: move to a `Theme.Material3.*` parent, use colour *roles* (primary / secondary / tertiary / error / background / surface, each with `on*` and `*Container` variants) rather than hardcoded colours, and let `DynamicColors` override the roles at runtime.
- **Takeaway for Marlow:** Do not hand-pick accent colours. Derive every launcher surface (drawer, folders, search, settings, widget frames, long-press menus) from the Material 3 colour roles, so a wallpaper change re-themes the whole launcher at once. Hardcoded brand colours in any launcher chrome are the fastest way to look non-stock. See Rules R10–R12.

## 2022 — "Prepare your app to support predictive back gestures" (Android Developers Blog, July 2022, Jason Tang, Diego Zuluaga, Michael Mauzy)

- **URL:** https://android-developers.googleblog.com/2022/07/prepare-your-app-to-support-predictive-back-gestures.html?hl=th
- **What it establishes:** Since gesture navigation arrived in Android 10, users wanted to know where a back gesture would take them *before* completing it. Predictive back answers that: starting a back swipe shows an animated preview of the destination UI; completing the gesture navigates there. The early version shipped as a developer option in Android 13 Beta 4, with the UI intended for a future release. Supporting it means moving to an ahead-of-time back model: new platform APIs (`OnBackInvokedCallback`, `OnBackInvokedDispatcher`) and AndroidX Activity 1.6+ (`OnBackPressedCallback` / `OnBackPressedDispatcher`), and deprecating `KeyEvent.KEYCODE_BACK` and `onBackPressed`.
- **Takeaway for Marlow:** Back is a *preview*, not an event. Marlow's settings, drawer, folders, and widget picker must all declare their back handling ahead of time through the dispatcher, so the system can animate the preview. Any screen that intercepts back with the old key-event path breaks the preview and will feel broken on Android 13+ — and on Android 15 the system animations are on by default for opted-in apps, so there is no developer-option hiding place left.

## 2022 — Google I/O 2022: "What's New in Android 13" (incl. predictive back)

- **Source:** I/O 2022 coverage of Android 13; predictive-back summary corroborated by the Android 13 feature page linked in that coverage (developer.android.com/about/versions/13/features/predictive-back-gesture, cited in the Appcircle I/O 2022 round-up).
- **What it establishes:** Android 13 adds the preview display while the user keeps dragging from the edge; apps must provide the destination data to Android ahead of time. This is the same ahead-of-time model as the July 2022 blog post above.
- **Takeaway for Marlow:** Same as above, plus: the launcher is the *destination* of the most common predictive-back animation (back-to-home). Marlow must not consume the back gesture at the home-screen root — at root, back belongs to the system so the back-to-home preview can play. Intercepting it to do nothing, or to open a menu, breaks the platform's most visible animation.

## 2023 — Google I/O 2023: "What's new in Android" and Modern Android Development (Glance beta, large screens)

- **Sources:**
  - Android Developers Blog, "14 Things to know for Android developers at Google I/O!" (May 2023): https://android-developers.googleblog.com/2023/05/14-things-to-know-for-android-developers-google-io.html?m=1
  - Android Developers Blog, "Top 3 things to know in Modern Android Development at Google I/O '23": https://android-developers.googleblog.com/2023/05/modern-android-development-at-google-io.html?hl=fr
- **What it establishes:**
  - Jetpack Glance — Compose-style widgets — reached beta at I/O 2023 ("You can now also use Jetpack Compose to build home screen widgets with the Glance library"). The I/O 2023 summary explicitly frames this as widgets becoming a Compose surface.
  - Large screens/foldables were a headline commitment: "You can start optimizing for these and other large screen devices by reading the do's and don'ts of optimizing your Android app for large screens and watching the Developing high quality apps for large screens and foldables session" (video `mSejMEIbAzU` embedded in the 14-things post).
  - Compose adoption: 24% of the top 1,000 Play apps used Compose, double the prior year — the ecosystem signal that Compose-first UI (including launcher UI) was now the mainstream path, not an experiment.
- **Takeaway for Marlow:** Two linked decisions. First, Marlow's *own* widgets (At a Glance / clock / search) should be built with Glance, not hand-rolled RemoteViews, so they inherit responsive layout and dynamic colour. Second, Marlow's grid must be a window-size decision, not a device-type decision — a foldable unfolding mid-session is a recomposition, not a restart (see Part B, large screens).

## 2024 — Google I/O 2024 / Android 15 Beta 2: predictive back leaves developer options; generated widget previews

- **Sources:**
  - Android Developers Blog, "The Second Beta of Android 15" (May 2024): https://android-developers.googleblog.com/2024/05/the-second-beta-of-android-15.html
  - Android 15 features overview: https://developer.android.com/about/versions/15/features?source=post_page-----26ee709b66b4--------------------------------
- **What it establishes:**
  - "In Android 15, predictive back will no longer be behind a developer option, so system animations such as back-to-home, cross-task, and cross-activity will appear for apps that have properly migrated." The grace period ended at I/O 2024.
  - Android 15 added *generated widget previews*: before Android 15, picker previews were a static image or layout resource, which "often differ significantly from the look of the actual widget" and could not be created with Jetpack Glance at all (developers had to screenshot their widget or build an XML layout). Android 15 lets providers generate `RemoteViews` as the picker preview via a push API (`AppWidgetManager.setWidgetPreview(...)`), persisted in `AppWidgetService`, so the host can request them on demand and the preview matches what the user will actually see.
  - The same I/O pointed developers at "Building adaptive Android apps" and "Building UI with the Material 3 adaptive library" for large screens.
- **Takeaway for Marlow:** The picker is now a truthfulness test. Marlow (API 35+) should prefer generated previews when a provider supplies them and fall back to the static preview only when none exists — never substitute its own screenshot or a stale cached image. And predictive back is no longer opt-in polish; an unmigrated launcher is visibly behind on Android 15.

## 2025 — Widgets Spotlight Week (Android Developers Blog, 3–6 March 2025, Summers Pitman and colleagues)

- **Sources:**
  - Hub post, "Spotlight Week: Design and develop widgets" (Summers Pitman, Developer Relations Engineer), 3 March 2025 — via the Android Developers Blog index: https://android-developers.googleblog.com/search?updated-max=2025-03-12T09:00:00-07:00&max-results=10&reverse-paginate=true&start=7&by-date=false&hl=fr
  - "Design with Widget Canonical Layouts" (Summers Pitman and Ivy Knight, Senior Design Advocate), 4 March 2025 — same index.
  - 9to5Google summary, "Google making Android widgets push with new Play Store filter", 3 March 2025: https://9to5google.com/2025/03/03/android-widgets-play-store/?extended-comments=1
- **What it establishes:** A full week devoted to widgets, structured as: *Why Widgets?* (Mon 3 Mar) — customisation, efficiency, quick info; *Design great widgets with Figma and Canonical Layouts* (Tue 4 Mar) — a step-by-step guide to visualising content in standard widget layouts; *Develop best practice widgets with Glance* (Wed 5 Mar); *#AskAndroid* (Thu 6 Mar). The week's high-level guidance, as summarised by 9to5Google: focus on value (do something useful without opening the app); keep it simple; make it adaptable (test on phones, tablets, foldables); match the look (system colours, fonts, corner shapes); make it easy to find (use the widget-pinning API from inside the app). Google Play simultaneously added a "Widgets" search filter, a widget badge on listings, and a curated widgets editorial page — discoverability became a platform feature, not a launcher feature.
- **Takeaway for Marlow:** Canonical layouts are the shared vocabulary between widget developers and launcher hosts. Marlow should size, pad, and round its widget cells to the canonical-layout grid so a widget designed in Figma against those layouts lands exactly as designed — and should expose the pinning API path (long-press → Widgets, and in-app pin requests) rather than hiding widget discovery behind settings. The Play-side filter also means users now arrive *expecting* widgets to be findable; a launcher whose picker is slow or unsearchable is now conspicuously behind the store itself.

## 2026 — Google I/O 2026: Remote Compose widgets; Android Dev Summit 2026: Glance + Remote Compose

- **Sources:**
  - Android Authority, "Android home screen widgets are getting a massive upgrade" (I/O 2026): https://www.androidauthority.com/android-widgets-overhaul-remote-compose-3670122/
  - Android Developers Blog, "17 Things to know for Android developers at Google I/O" (2026): https://android-developers.googleblog.com/2026/05/17-things-android-developers-google-io.html
  - Android Dev Summit 2026 session, "Evolving Tiles to Wear Widgets using Jetpack Glance and Remote Compose" (published 30 Oct 2026): https://developer.android.com/events/dev-summit/talks/evolving-tiles-to-wear-widgets-using-jetpack-glance-and-remote-compose
  - Android Dev Summit 2026 sessions index: https://developer.android.com/events/dev-summit?authuser=7
- **What it establishes:**
  - At I/O 2026 Google announced a new rendering engine for Jetpack Glance, **Remote Compose**: logic, smooth resizing transitions, dynamic theming, and particle effects handled in the system layer, so widgets no longer wake the host app for animation. New capabilities: Snap Scroll (snap-to-page vertical scrolling in widgets), Expressive Components (custom shapes, morphing/tactile interaction states), Particle Effects, smooth fade-and-morph widget resizing by default, universal dynamic theming (one widget re-themes across phone, watch, and car), and a new canonical layout, "Streak". Native support lands on Android 16+; Android 15 and below get safe static fallbacks.
  - The I/O 2026 recap states the direction plainly: "Android 17 marks a shift toward a single, Compose-based development model for all widgets. By unifying the experience across mobile, Wear OS, and cars through Jetpack Glance, you can soon scale UI components across the ecosystem with a familiar workflow" — and "The Android ecosystem is now Adaptive by Default, moving fluidly across phones, foldables, tablets, cars, XR, and expanding usages."
  - The Dev Summit 2026 session traces the Wear-side evolution "from full-screen Tiles to dynamic, partial-height Wear Widgets", covering "the technical benefits of the unified Jetpack Glance API and the new Remote Compose engine" and "the latest UX guidance" plus migration strategies — i.e. glanceable UI is now one API across surfaces, not a per-surface rewrite.
- **Takeaway for Marlow:** Do not build a bespoke widget-animation or resize system. The platform is converging on Glance + Remote Compose as the widget runtime; Marlow's job is to host it faithfully — honour the resize morph, do not re-animate resizes itself, and degrade gracefully (static fallback) on older devices rather than crashing or blanking a Remote Compose widget. Also: "Adaptive by Default" is now the stated baseline, not an aspiration — a phone-only grid is a dated launcher.
- **Caution:** The I/O 2026 / Android 17 material above is very recent (May–Oct 2026). Treat Remote Compose capabilities as the direction of travel for Marlow's *host* support, not as APIs Marlow must adopt on day one; verify the exact host-side requirements against the current Glance/Remote Compose docs before implementing.

---

# Part B — Developer documentation and blog posts (the standing rules)

## B1. Widget design — the host's contract

- **App Widget Design Guidelines (developer.android.com):** http://developer.android.com/guide/practices/ui_guidelines/widget_design.html
  - "A widget can display an application's most timely or otherwise relevant information at a glance, on a user's Home screen." The page defines the standard anatomy — bounding box, frame, graphical controls — and requires margins between the bounding box and the frame and padding between the frame and the controls. It gives the cell-size table (1 cell = 40 dp, 2 = 110 dp, 3 = 180 dp, 4 = 250 dp; *n* cells = 70×*n* − 30 dp) and advises being conservative with `minWidth`/`minHeight`: specify the minimum size that renders the widget in a good default state, and use nine-patch backgrounds and flexible layouts so the widget adapts to the host's grid.
  - **Host reading:** the launcher owns the bounding box and the margins. If Marlow's cells do not follow the 70×*n* − 30 sizing, widgets designed to the guideline will misalign with icons — the most visible "not stock" tell there is.
- **Adapt widgets across form factors (developer.android.com, updated 20 Oct 2026):** https://developer.android.com/design/ui/mobile/guides/widgets/adapt
  - Build with Glance; make widgets responsive; include light *and* dark themes; implement dynamic theming; ensure the widget completely fills the grid bounds (WL-1 widget quality guideline). For glanceability: design so the key information is visible without manual scrolling — cars do not support it, and a scrollable `LazyColumn` must have a non-scrolling alternative that delivers all necessary information on one surface, with the most valuable/personalised content first.
  - **Host reading:** Marlow should never *require* scrolling inside a widget to reach its point, and should test its own widgets at every size the grid offers, light and dark.
- **Create a widget with Glance (codelab):** https://developer.android.com/codelabs/glance
  - Glance reuses the Compose framework/coding style (not Compose composables themselves), removes much of the need for XML RemoteViews, and pairs with `GlanceTheme` for dynamic colour and responsive layout. The codelab's example widget shows a favourite contact, unread state, and a tap that deep-links to the chat.
  - **Host reading:** Marlow's own widgets should be Glance widgets; the codelab is the fastest correct reference for one.

## B2. Shortcut design

- **App Shortcuts — Icon Design Guidelines v2.0 (PDF, developers.android.com):** http://developers.android.com/static/shareables/design/app-shortcuts-design-guidelines.pdf
  - Shortcuts give quick access to up to four of an app's actions. Shortcut icons are circular: all content inside a 44 dp live area, on a solid Material Grey 100 (#F5F5F5) fill, with 2 dp padding making a 48 dp total; no shadows. Standard icons are a 24×24 dp Material system icon, centred, in the app's primary colour or app-icon colour, with enough contrast against the grey. Avatars fill the whole live-area circle. Vector drawables only — non-vector drawables do not scale in this context.
  - **Host reading:** the long-press menu is where this guideline lives or dies. Marlow must render shortcut icons as circles at 48 dp with the specified padding, must not square them off, add shadows, or re-tint them, and must cap the list at four actions per app as the platform does.

## B3. Icon design

- **Adaptive icons (developer.android.com, current):** https://developer.android.com/develop/ui/compose/system/icon_design_adaptive?authuser=4 — see Part A, 2017, for the full requirements. Two additions verified on the current page:
  - Themed icons: from Android 13, if the user enables themed app icons *and the launcher supports it*, the system tints icons using the wallpaper/theme colours for apps with a `monochrome` layer. "If the launcher doesn't support themed app icons", the home screen shows the unthemed icon instead.
  - From Android 16 QPR 2, Android automatically themes app icons for apps that don't provide their own monochrome layer.
  - **Host reading:** themed-icon support is a launcher capability flag in practice. Marlow must implement it (or explicitly document that it does not) — silently ignoring the system setting is a stock-feeling failure users *will* notice, because the rest of the system re-themes around them.
- **"Designing Adaptive Icons" / "Implementing Adaptive Icons" / "Understanding Android Adaptive Icons" (Medium, Android Developers)** — linked from the docs page above. The XDA walkthrough of the same material ("Graphic design for Android developers: Creating the ideal app icon", 2 Dec 2019) confirms the working vocabulary: 108×108 dp layers, 72×72 dp visible area, 66 dp safe-zone circle ("no mask can clip away"), vector preferred: https://www.xda-developers.com/graphic-design-for-android-developers-creating-the-ideal-app-icon/

## B4. Dynamic colour / Material You

- **AOSP, "Material You design":** https://source.android.com/docs/core/display/material?hl=en — one source colour → 5 tonal palettes × 13 variants = 65 colour attributes; use the AOSP extraction logic; use the same palette for System UI and apps.
- **Codelab, "Adding dynamic color to your app":** https://codelabs.developers.google.com/codelabs/apply-dynamic-color — `Theme.Material3.*`, colour roles, `DynamicColors.applyIfAvailable()`.
- **Host reading:** see Rules R10–R12. One extra launcher-specific point from the AOSP page: dynamic colour is described as covering *widgets* as a first-class area alongside colour and motion. Marlow's widget frames and its own widgets must re-theme with the wallpaper, not just its settings screens.

## B5. Gesture navigation, edge-to-edge, predictive back

- **Ensure compatibility with gesture navigation (developer.android.com):** https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav?hl=en&authuser=002
  - Android 10+: two duties — extend content edge-to-edge, and handle conflicting gestures. Conflicting gestures are handled by `View.setSystemGestureExclusionRects()` (a `List<Rect>` of regions that need touch input), and the doc notes that DrawerLayout and SeekBar already opt out automatically. Android 13+ adds predictive back as part of a multiyear release.
  - **Host reading:** Marlow should lean on the components that already handle exclusion (do not hand-roll it), and should claim exclusion rects narrowly — only the rect that is actually a launcher drag handle.
- **Add support for the predictive back gesture (developer.android.com, current):** https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture?authuser=5
  - Predictive back is enabled by default for apps using default navigation (upgrade to AndroidX Activity 1.6.0+ if using Fragments/Navigation). Custom back goes through `OnBackPressedCallback` / `OnBackInvokedCallback`; Compose uses `PredictiveBackHandler` (activity-compose 1.8.0+) with a `Flow<BackEventCompat>` for custom animations, or plain `BackHandler` when only interception is needed. Callbacks must be single-responsibility, enabled/disabled from observable UI state (StateFlow / Compose State) — *not* from conditional statements that react after the event. Never use back callbacks for business logic or logging (log in `ViewModel.onCleared()`; on Android 16+, use `OnBackInvokedCallback` with `PRIORITY_SYSTEM_NAVIGATION_OBSERVER`, which observes without consuming). Opt-out is `android:enableOnBackInvokedCallback="false"` (app or per-activity). From Android 15 the system animations are on by default for opted-in apps.
  - **Host reading:** Marlow is Compose-first, so `PredictiveBackHandler` / `BackHandler` with state-driven `enabled` is the correct pattern for every overlay: drawer, folder open state, widget picker, long-press menu, settings sub-screens. One callback per responsibility; the innermost enabled callback wins and the system handles the rest, including back-to-home at root.

## B6. Large screens, foldables, and adaptive layouts

- **Support different display sizes (developer.android.com):** https://developer.android.com/develop/ui/compose/layouts/adaptive/support-different-display-sizes?u=http%3A%2F%2Fdeveloper.android.com%2F%2Fcloud.google.com
  - "Avoid using physical hardware values for making layout decisions" — do not branch on "is this a tablet?" or a fixed aspect ratio. Decide from the actual window metrics (Jetpack WindowManager) and convert them to a window size class. A tablet app in multi-window, a foldable mid-unfold, and a resizable desktop window all change the available space *while running*.
- **Support trifolds and landscape foldables (developer.android.com):** https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/trifolds-and-landscape-foldables?hl=en&authuser=5
  - Determine orientation from the actual screen area the app occupies, not the device's physical orientation (the original Pixel Fold opens in landscape; rotation_0 = landscape). Maintain continuity across fold/unfold via ViewModel state — avoid restarts or state loss. If already at the "Optimized" tier (Tier 2) of the adaptive quality guidelines, the app should function well on foldables.
  - **Host reading:** Marlow's grid, dock, drawer columns, and widget placement must all be functions of the current window size class (Compact <600 dp, Medium 600–840 dp, Expanded ≥840 dp, plus Large/XLarge in WindowManager 1.5 BREAKPOINTS_V2), recomputed on every configuration change. A foldable that unfolds must reflow the home screen in place, preserving which page the user was on and every widget's position — never restart the launcher activity to "apply" the new size. Per-screen layouts (different grid on cover vs inner screen) are a known user request in the launcher community (see Part C, Niagara/Smart Launcher); at minimum, Marlow must not *break* when the two screens differ.

## B7. The launcher's role in the system (synthesis of the docs above)

No single doc is titled "the launcher's role". The role is defined by the sum of the host duties above: the launcher is the only app that hosts *other apps'* widgets, icons, shortcuts, and wallpaper; it is the destination of back-to-home; and it is the surface gesture navigation starts from. The docs treat the launcher as infrastructure, not as a showcase — its quality is measured by how faithfully it renders other people's content. That is the design principle behind Rules R1–R4.

---

# Part C — Design-engineering writing from people who built launchers

## C1. Lawnchair (open source, Launcher3-based; GitHub: LawnchairLauncher/lawnchair)

Primary sources: GitHub Releases page (verified by reading it, 2026-11-26), Telegram changelog channel, and the Lawnchair blog.

- **Releases:** https://github.com/LawnchairLauncher/lawnchair/releases
- **Blog (15 Beta 1 announcement, linked from Telegram):** https://lawnchair.app/blog/lawnchair-15-beta-1
- **Telegram news channel:** https://telegram.me/s/lawnchairci

What the changelogs establish, release by release:

- **Lawnchair 14 (Apr 2024, based on Launcher3 from Android 14):** GestureNavContract for no-root app-closing animations; no-root Global Search (contacts, files, settings, web); Smartspacer support (an At a Glance alternative); custom icon shapes alongside new predefined ones; improved icon-pack / themed-icons picker; custom Home Feed support; Hide Dock support. Lesson: Lawnchair's identity is "Pixel Launcher plus customisability" — it tracks the AOSP rebase first, then adds options. The rebase *is* the feature; customisation rides on top of it.
- **Lawnchair 14 Beta 2:** force-resizing widgets / unlimited resizing options (a community PR); package name reverted to `app.lawnchair` after Play Store update conflicts. Lesson: widget resize limits are a user-visible pain the community fixes first; and package identity is load-bearing — renaming it breaks updates.
- **Lawnchair 14 Beta 3:** the most instructive single changelog for Marlow. UI text refresh — "all text is now Sentence case to match Material You guidelines (#4209)"; "Proper tablet support: the launcher and settings UI now adjust accordingly to the larger width"; "Color scheme improvements: Lawnchair now matches the Android 14 and Pixel Launcher colors"; "Added support for predictive back when navigating in Settings"; per-app language (Android 14); new fonts; search improvements (permission handling, performance, switchable algorithms, Google & DuckDuckGo providers); recents improvements (reorderable Overview quick actions, recents-background transparency); crash fixes on One UI, ColorOS, and other OEM skins; Smartspacer left-padding fix; nightly releases. Lesson: "matching Pixel Launcher colours" and "Sentence case to match Material You guidelines" were called out as *features* — stock-feeling is achieved by conforming to the platform's text and colour rules, not by inventing a house style. And OEM-skin crashes are a permanent tax on launcher work: Lawnchair fixes One UI/ColorOS crashes in almost every release.
- **Lawnchair 15 Beta 1 (based on Launcher3 from Android 15):** Private Space and App Archiving support; App Drawer Folders (manual folders + experimental automatic categorisation, "Caddy"); dock background (colour + corner radius), widgets directly in the dock, dock icon labels; Wallpaper Carousel (quick-switch between current and recent wallpapers, "similar to the Pixel Launcher"); App Pausing for rooted QuickSwitch users; expanded search (custom engines, Ecosia/Kagi/Firefox/Iceraven/Mull); experimental "Deck" (no-app-drawer mode, with an explicit "create a backup first" warning); settings reorganised and search settings centralised; settings rewritten with type-safe Compose Navigation; nightly build system. Regressions honestly listed: work-profile icon badges temporarily non-functional; "Customize Icon" bottom sheet not updating state; drawer-folder edits not saving. Lesson: a rebase release is a good moment to reorganise settings and navigation, but badge/state regressions are the predictable cost — and Lawnchair lists them openly rather than shipping silently broken.
- **Lawnchair 15 Beta 2 (stability-focused):** redesigned search backend; infinite scrolling in home screen; re-ordering apps inside drawer folders; "clear all items from home screen" option; fixes for the search widget failing to launch the Google app on Android 14+, a race condition when customising an app being uninstalled, wrong icon scaling in the drawer when home icons are resized, and a half-displayed page indicator. Lesson: the bugs that matter are state bugs (search state not clearing when the drawer hides, bottom-sheet state not updating) and scaling bugs (icon scaling leaking between surfaces). Both are directly testable in Marlow.
- **Lawnchair 15 Beta 2.1 (hotfix):** a toggle to disable icon closing animations (`GestureNavContract`) "which can resolve animation glitches on certain OEMs (like One UI)"; fixes for widgets failing to add when a configuration step is required on Android 14+; web-search timeout (offline search performance); Google Feed not showing (requires Lawnfeed 4.0); icon shadow not applied correctly. Lesson: app-closing animations are OEM-fragile — ship a kill-switch. And widget *configuration* is a first-class failure mode: a widget that needs setup must not fail to add on Android 14+.
- **Lawnchair 15 Beta 3 (Apr 2026, current at research time):** Nova Launcher backup import — home-screen and dock grid layout, widgets and folders, selected icon pack. Lesson: the backup format is the interoperability layer between launchers. Nova's backup is the de-facto standard other launchers import; Marlow should be able to *import* it (and, symmetrically, export something importable) even if it never clones Nova's features.
- **Play Store listing (updated 18 Apr 2026):** https://play.google.com/store/apps/details?id=app.lawnchair.play&hl=en-US — "Lawnchair is a free, open source home screen replacement based on Launcher3 from the Android open source project… still in beta."

**Takeaway for Marlow:** Rebase to the current Launcher3/AOSP behaviour first; treat Material You text-casing and colour matching as features; ship kill-switches for OEM-fragile animations; test widget configuration flows on Android 14+; and treat Nova-backup import as table stakes for winning switchers.

## C2. Niagara Launcher (Peter Huber; minimalist, one-handed list launcher)

Primary source: the Android 12 changelog post (by "timm0e", a Niagara team member, writing in place of Peter Huber), preserved in the 4PDA forum mirror of Niagara's changelog thread.

- **Changelog mirror:** https://4pda.to/forum/index.php?showtopic=916981&st=640
- **Play listing (developer: Peter Huber):** corroborated by APKMirror listings for Niagara Launcher ‧ Home Screen 1.15.x, "Offered By Peter Huber".

What the Android 12 post establishes:

- Material You was adopted *as a system-wide palette*, not as a skin: "Since Niagara Launcher resembles a significant part of your phone's interface, it should, of course, also make use of this palette." Until then only the alphabet selector, search bar, and Niagara Button had been wallpaper-coloured; the update extended the palette to backgrounds throughout the app — "a very subtle" change that "helps the UI to harmonize with the style of your wallpaper". While not on the home screen, Niagara applies a tinted overlay to the wallpaper to increase readability of foreground items.
- Crucially, Niagara **backported Material You to Android 5.0–12 on any device** — at a time when Google's own Material You was Pixel-only on Android 12. The team extracted the palette itself rather than waiting for the platform.
- **Stylised (themed) icons were deliberately *not* supported**: "In their current state, they create a very inconsistent look, since they are only available for Google apps, and the styling of these few apps is already inconsistent. Therefore, we have chosen not to support the native stylized icons for now." Lesson: a principled *no* is a design decision — shipping a half-covered themed-icon set looks worse than shipping none, because the inconsistency is the thing users see.
- Widgets in the new design "have rounded corners, and they also will in Niagara Launcher, even on older versions of Android" — the host enforces the corner shape regardless of OS version.
- Privacy by minimisation: weather needs only approximate location, because "the weather typically doesn't change that much in a 2-kilometer radius" — the launcher asks for the least permission that does the job.
- The post is also candid about process: writing changelogs takes time that "cannot be spent on development", so the team tried offloading the writing — a small, honest note on why launcher changelogs are often thin.

**Takeaway for Marlow:** Subtle, wallpaper-derived colour everywhere (not just accents) is what makes a launcher feel harmonious; enforce rounded widget corners in the host on every OS version; and if themed icons cannot be covered consistently, say so and ship none rather than a patchy set. Ask for approximate location, not precise, when the feature does not need more.

## C3. Nova Launcher (TeslaCoil / Kevin Barry; the customisation reference)

Primary sources: Kevin Barry's changelogs as reported contemporaneously by Android Police, XDA Developers, and 9to5Google. Nova's own blog/G+ posts are the origin; the press reports quote them verbatim.

- **Nova 4 / Material Design rebase (10 Mar 2015, Kevin Barry):** https://www.AndroidPolice.com/2015/03/10/nova-launcher-gets-a-huge-redesign-now-in-beta-material-design-new-code-base-and-tons-of-new-options/ and https://9to5google.com/2015/03/10/nova-launcher-beta-material/
  - "Full Material Design" (all settings screens and dialogs, more animations, Google Search as an overlay, Edit Shortcut dialog and quick menu tinted from the icon's colours); "Rebased on top of Launcher3"; individual icon layout settings (size, font) per surface (Desktop, Drawer, Dock, Folder); max icon size 150%; Widget Search; pull-to-search in the drawer; widgets given their own drawer, removed from the App Drawer; unlimited desktop pages (was 9); add/remove pages by dropping icons on the blank page; bulk folder editing; unread-badge shadows that scale with icon size; drawer folders reorderable by drag/drop.
  - Lesson: Nova's biggest releases are *rebases + systematisation* — one icon-size/font model applied per surface, one search entry point, one widget home — not a pile of one-off toggles. And tinting a dialog from the icon it edits is a small, cheap piece of polish that makes the launcher feel considered.
- **Nova 3.0 Beta 1 (May 2014, Kevin Barry):** https://www.droid-life.com/2014/05/27/nova-launcher-3-0-beta-1-released-brings-a-ton-of-enhancements-and-new-features/amp/
  - Subgrid positioning — "snap widgets/icons to half cells. Greater control of placement/size without shrinking/cropping app icons"; per-surface custom label colours; full 4×1 widget support in the dock; individually toggle widget padding (long-press → Padding); icons centred vertically in cells rather than top-aligned; phone landscape fills available space horizontally rather than reserving space for a hidden search bar/dock.
  - Lesson: the two ideas that still matter are *subgrid placement* (half-cell snapping beats shrinking icons) and *per-widget padding control* (the host must not force one padding on every widget). The landscape point is the same lesson as Part B §B6: don't reserve space for hidden chrome.
- **Nova 6.0 Beta 1 (Aug 2018, Kevin Barry via Google+):** https://www.XDA-developers.com/nova-launcher-6-0-beta-new-features/
  - Settings hierarchy reorganised (Dock options moved into Desktop; searchable settings); more Adaptive Icon options including a customisable shape; option to match folder/drawer icon size to desktop automatically; folder window styles (windowed/immersive) with background shapes sharing the adaptive-icon shape options (Round, Squircle, etc.); drawer search bar stylable like the desktop one; vertical/horizontal folder scrolling; icon resizing no longer requires Prime.
  - Lesson: consistency *across surfaces* is the recurring Nova theme — one icon size, one shape vocabulary, one search-bar style, everywhere. And moving a paid feature (icon resizing) to free is a reminder that the paywall line is a product decision, not a technical one.
- **Status note (2025–2026, for context, not a design claim):** Nova's team was cut in 2025 and Instabridge has owned Nova since January 2026; a 2026 comparison notes ads on the home screen and in the drawer since early 2026. Source: https://luwall.com/hybrid-launcher/fold-launchers — cited here only so Marlow does not treat Nova's current state as a stable reference; its *design* lessons above stand on the changelogs themselves.

**Takeaway for Marlow:** Copy Nova's *systems*, not its toggle count: one icon-size/shape model across desktop, drawer, dock, and folders; half-cell (subgrid) placement; per-widget padding; searchable settings; tint transient UI from the thing it edits. Do not copy the current ad-supported state.

## C4. Smart Launcher (Smart Launcher Team / Ginlemon; Vincenzo Colucci, "ginlemon")

Primary sources: the Smart Launcher blog (linked from the Play Store "What's new" text), Play Store listing, and Android Police coverage of the 6.5 update.

- **Play Store listing:** https://play.google.com/store/apps/details?id=ginlemon.flowerfree&hl=en_GB
- **Blog — 6.6 build 011 ("Clearer weather, more personal folders"):** https://www.smartlauncher.net/blog/smart-launcher-6-6-build-011-clearer-weather-more-personal-folders (linked from Play "What's new", June 2026)
- **Blog — "The Sticky Update":** https://www.smartlauncher.net/blog/the-sticky-update (linked from Play, April 2026)
- **Blog — 2025 wrap-up / 6.6:** https://www.smartlauncher.net/blog/2025wrapupsl66
- **Android Police, "Latest Smart Launcher update introduces a more customizable Google Discover substitute" (27 Nov 2024):** https://www.androidpolice.com/smart-launcher-65-update-announced-new-features/

What the writing establishes:

- **Ambient theme:** "Smart Launcher automatically changes the theme colors to match your wallpaper" — the Play listing's headline feature. The team treats wallpaper-derived colour as a core identity, not a setting.
- **6.5 (Nov 2024):** "Your Feed" — an RSS reader built because users were unhappy with Google Discover integration and the customisation limits of the News Page; translucent widgets with background blurring (Nothing-OS-like), with the team "looking for ways to extend this to third-party widgets too"; two new themes, Opaque Glass and a Material You theme "that uses the same hues as Pixel Launcher does to match your wallpaper"; KWGT partnership so Kustom widgets back up automatically with launcher settings. Lesson: when the platform feed cannot be customised, builders build their own feed — and blur is a *widget* treatment first, a theme second.
- **6.6 build 001 (Jan 2026):** Liquid Glass theme out of beta; Infinite Scroll on home pages *and* dock; pinch-and-rotate gestures without experimental flags; a fix for widgets resizing incorrectly; separate customisation for widgets vs launcher pages. Lesson: widget resize correctness is a recurring bug class across *every* launcher in this file (Lawnchair, Smart Launcher, Nova) — it deserves a dedicated test, not a passing glance.
- **"The Sticky Update" (Apr 2026):** drag-and-drop / copy-paste stickers and text on the home screen; preview feed content before adding a source; reset widget stacks to their default position on returning Home; refreshed Material You theme; AccuWeather as weather provider; improved shortcuts; better backups. Lesson: "reset the stack on return Home" is a small state rule with an outsized calmness payoff — the home screen should be predictable when you arrive, not wherever you left a stack.
- **6.6 build 011 (June 2026):** weather widgets with colour-coded icons warning about high UV; per-folder colours; a Security setting to disable analytics and crash reports; lifetime licences purchasable before a subscription ends. Lesson: per-folder colour is the same "one model, every surface" idea as Nova's shapes — and a visible analytics off-switch is a trust feature, not a footnote.
- **Older lineage (Smart Launcher 3, 2016):** "not based on the AOSP launcher" — an independent codebase; "Widget picker is now much faster" called out as a changelog line in 3.22; low resource use (RAM/battery) as a selling point. Lesson: picker *speed* is a feature users notice; a slow picker feels broken even if it is correct.

**Takeaway for Marlow:** Ambient/wallpaper colour as identity; blur as a widget treatment; widget-stack position resets on Home; separate widget vs page customisation; and treat widget-resize correctness and picker speed as first-class quality bars.

## C5. Microsoft Launcher (Microsoft Garage → Microsoft Launcher; formerly Arrow Launcher)

Primary sources: Microsoft's Garage-era changelogs as reported by Android Central, Windows Central, The Win Central, and MSPowerUser. Microsoft publishes no single design blog for the launcher; the changelog record *is* the writing.

- **Arrow → Microsoft Launcher (Oct 2017, Android Central):** https://www.androidcentral.com/microsoft-launcher-android-takes-over-arrow-new-features-design-tweaks
  - Arrow began as a Microsoft Garage project (employees' own-time experiments). On becoming Microsoft Launcher it gained the first application of the Fluent Design System outside Windows 10; "The Feed" (swipe-right feed: events, news, recent activities, favourite people, most-used apps, with a time-of-day greeting); "Continue on PC" (long-press a document/image to continue on Windows 10, rather than using the share target, which the team judged unintuitive); pinnable contacts; customisable grid, icon packs, backup/restore, disable animations.
  - Lesson: a feed's value is *glanceable personal context* (calendar + people + recents), not a news list; and a long-press action beats a share sheet when the destination is known.
- **Arrow 3.0 (Feb 2017):** https://www.windowscentral.com/microsofts-arrow-launcher-nabs-big-update-new-features-tow
  - Vertical *or* horizontal scrolling for the apps page; a brand-new utility page of pinnable "cards" (recent activity, favourite contents, recent apps, reminders, documents); daily Bing wallpapers. Performance claims from the product lead (James Hsu, Office Experience Org Mobile team): app launch 10× faster, 20% more memory efficient, 15% more battery efficient.
  - Lesson: give users the scroll-axis choice rather than picking one; cards that can be pinned as pages are a middle ground between a fixed feed and a freeform home screen.
- **Microsoft Launcher 4.5–4.9 (2018):** https://thewincentral.com/microsoft-launcher-android-updated-wit-new-feature-changelog-inside/?amp and https://thewincentral.com/microsoft-launcher-for-android-updated-with-improved-home-screen-more-changelog-inside/
  - 4.5: icon size adjustable in drawer, dock, and home screen (one control, three surfaces); blank pages in vertical mode; "let meeting attendees know you're running late" from the calendar card; Oreo shortcut creation. 4.6: Cortana in the launcher; Arabic/RTL support; widgets in the dock; pin a searched app/contact to the home screen. 4.9: improved home-screen import logic; alphabetical folder sorting; dock app/folder names; customisable Home-button gesture; a gesture to lock the home screen.
  - Lesson: import logic is worth iterating on (4.9 calls it out explicitly); RTL is not an afterthought for a launcher — the whole grid mirrors; and dock widgets + dock labels keep reappearing across launchers (Lawnchair 15 added the same).
- **Microsoft Launcher 6.2 (Jan 2021):** https://mspoweruser.com/da/microsoft-launcher-updated-2/
  - "Design improvement on app paddings in folder"; work-profile setup optimisation via the feed page; fix for icon-pack reset after restart; fix for swipe-up gesture failing to trigger the shortcuts page; battery/performance improvements.
  - Lesson: folder padding and icon-pack persistence across restarts are the kind of small, unglamorous correctness that decides whether a launcher feels solid — both are directly testable.

**Takeaway for Marlow:** A swipe-right feed should be personal context first (calendar/people/recents), not news; offer the scroll-axis choice; make import logic a maintained feature; support RTL mirroring from the start; and test folder padding and icon-pack persistence across restarts.

## C6. Android framework engineers — writing that is not a talk

- **Chris Banes and colleagues on gesture navigation / edge-to-edge** — see Part A, 2019 (Dev Summit) and Part B §B5. Banes's framing, as captured in the I/O 2019 app post, is that gesture support is *two* duties (edge-to-edge + conflict handling), not one.
- **Nick Butcher on adaptive icons** — see Part A, 2017. The design post is the reason the 66 dp safe zone exists as a shared contract.
- **Summers Pitman (Developer Relations Engineer) and Ivy Knight (Senior Design Advocate) on widget canonical layouts** — see Part A, 2025 Spotlight Week. Their point is that widget design starts in Figma against canonical layouts, not in code against a grid.
- **Jason Tang (Product Management), Diego Zuluaga (Developer Relations), Michael Mauzy (Developer Documentation) on predictive back** — see Part A, 2022. Their post is the clearest statement of the ahead-of-time back model and its deprecations.
- **What is missing:** no framework engineer has published a "how to build a launcher" guide. The closest primary material is the Launcher3 source itself (AOSP) and the docs above. Marlow should treat Launcher3 as the reference implementation and these talks/docs as the specification — and should not infer launcher-specific guidance that no source states.

---

# Part D — Checkable rules for Marlow

Each rule is testable on a device or in a test. "Pass" means the stated observable holds. The source in brackets is the justification; the full citation is in Parts A–C.

## Widgets (host duty)

- **R1 — Widget cells follow the platform sizing table.** A widget requesting *n* cells occupies 70×*n* − 30 dp (1 cell = 40 dp, 2 = 110 dp, 3 = 180 dp, 4 = 250 dp), with the guideline margins between bounding box and frame and padding between frame and controls. *Test:* place 1×1, 2×2, and 4×2 widgets; measure cell bounds and check icon-grid alignment. [App Widget Design Guidelines]
- **R2 — Widgets are rounded and padded by the host, on every OS version.** Widget frames use the Material You rounded-corner + padding treatment even where the provider did not supply it, and the treatment is uniform across all widgets. *Test:* place an old, un-updated widget; its corners are rounded and it does not draw to the cell edge. [I/O 2021 widget overhaul; Niagara Android 12 changelog]
- **R3 — Resize reflows, it does not just scale.** Growing a widget reveals more functionality/content; shrinking it removes secondary content — the widget never just stretches its bitmap. *Test:* resize a widget through at least three sizes; content changes at each step. [I/O 2021; blog.google widgets post]
- **R4 — The picker shows truthful previews.** On Android 15+, prefer a provider's generated preview (`setWidgetPreview` / RemoteViews) when supplied; otherwise show the static preview. Never substitute a stale screenshot or a generic placeholder. *Test:* add a Glance widget whose preview is generated; the picker preview matches the placed widget. [Android 15 features; I/O 2024]
- **R5 — The picker is searchable and fast.** Widget search finds widgets by name; the picker opens without a perceptible stall. *Test:* time picker open on a cold start; search "clock" and confirm results appear. [Nova 4 Widget Search; Smart Launcher 3 "widget picker is now much faster"; Spotlight Week "make it easy to find"]
- **R6 — Widget configuration cannot fail to add.** A widget that requires a configuration step adds successfully on Android 14+ (configuration completes, widget lands, no crash, no silent drop). *Test:* add every configurable widget in the test set on Android 14+; each lands exactly once. [Lawnchair 15 Beta 2.1]
- **R7 — Widget resize is correct, repeatedly.** Resizing a widget to every offered size and back leaves it at the right size with no clipping, overlap, or drift; resizing works after rotation/fold. *Test:* resize each test widget through all sizes twice, including across a fold/unfold. [Smart Launcher 6.6 build 001; Lawnchair 14 Beta 2]
- **R8 — Marlow's own widgets are Glance widgets with light and dark themes and dynamic colour.** No hand-rolled RemoteViews widget ships as a Marlow first-party widget. *Test:* inspect the widget implementation; toggle light/dark and change wallpaper; the widget re-themes. [I/O 2023 Glance beta; Glance codelab; Adapt-widgets doc]
- **R9 — A widget's point is visible without scrolling.** The key information/action is on the first surface; any scrollable list has a non-scrolling alternative. Widget stacks reset to their default position on returning Home. *Test:* read the widget at a glance; return Home from a scrolled stack and confirm position reset. [Adapt-widgets doc; Smart Launcher "Sticky Update"]

## Icons and shortcuts

- **R10 — One mask, one safe zone, everywhere.** Every app icon is rendered through the same adaptive-icon mask; the inner 66×66 dp safe zone is never clipped; no baked-in shadow or mask is added inside the icon. *Test:* screenshot icons under circle, squircle, and square masks; logos are whole in all three. [Adaptive icons doc; "Designing Adaptive Icons"]
- **R11 — Themed icons are honoured or explicitly declined.** If the system themed-icons setting is on and Marlow claims support, every app with a monochrome layer is tinted from the wallpaper palette; from Android 16 QPR 2, apps without a monochrome layer are auto-themed. If Marlow does not support themed icons, that is a documented, deliberate decision (Niagara's reasoning), not a silent gap. *Test:* enable themed icons; compare Marlow's home screen with Pixel Launcher's. [Adaptive icons doc; Niagara Android 12 changelog]
- **R12 — Shortcut menus follow the shortcut-icon guideline.** Long-press shows at most four actions; shortcut icons are circular, 48 dp total (44 dp live area + 2 dp padding), 24 dp centred Material icon, no shadows, vector only. *Test:* long-press five apps with shortcuts; count actions; measure icon geometry. [App Shortcuts Icon Design Guidelines v2.0]
- **R13 — Per-widget padding is controllable; icons are not shrunk to fit.** Widgets can opt out of forced padding (or the host's default padding is the guideline padding, not zero); placing an icon/widget never shrinks or crops an app icon to make it fit. *Test:* toggle widget padding on a test widget; place icons on a half-cell/subgrid boundary and confirm icons are full-size. [Nova 3.0 subgrid + padding; Nova 6.0]

## Colour, text, and the stock feeling

- **R14 — All launcher chrome derives from Material 3 colour roles.** Drawer, folders, search, settings, long-press menus, and widget frames use `primary`/`secondary`/`tertiary`/`surface`/`on*` roles (via `DynamicColors`), never hardcoded brand colours. A wallpaper change re-themes the whole launcher. *Test:* change wallpaper; screenshot every launcher surface; no surface keeps the old accent. [Material You AOSP page; dynamic-colour codelab]
- **R15 — Colour is subtle and everywhere, not loud and local.** Wallpaper-derived colour appears in backgrounds throughout the app (not only in the search bar or a single button), at the low saturation the system palette produces. *Test:* compare a Marlow screen with the same screen on Pixel Launcher; the palette family matches. [Niagara Android 12 changelog; Lawnchair 14 Beta 3 "matches the Android 14 and Pixel Launcher colors"]
- **R16 — Text is Sentence case, per Material You guidelines.** All launcher UI text (settings, menus, dialogs, empty states) uses Sentence case, not Title Case. *Test:* grep the UI strings; no Title Case headings remain. [Lawnchair 14 Beta 3, #4209]
- **R17 — A tinted overlay keeps foreground text readable over wallpaper.** Where text sits directly on the wallpaper (home screen, drawer scrim), a scrim/tint guarantees contrast in both light and dark wallpapers. *Test:* set a busy light wallpaper and a busy dark wallpaper; all labels remain legible. [Niagara Android 12 changelog]

## Gestures, back, and edge-to-edge

- **R18 — Marlow draws edge-to-edge and respects insets.** Content draws behind the status and navigation bars; tappable content is inset away from system bars; the wallpaper itself is not inset. *Test:* screenshot with gesture navigation on; no label or button sits under a system bar. [I/O '19 Dark Theme & Gesture Navigation; Dev Summit 2019 Banes/Shah]
- **R19 — Gesture-exclusion rects are narrow and justified.** Only a rect that is genuinely a launcher drag handle (e.g. an edge-started page swipe) is excluded from the back gesture; the home gesture is never excluded. *Test:* list every exclusion rect Marlow registers and the gesture it serves; each has a named owner. [Gesture-navigation doc; Dev Summit 2019]
- **R20 — Every overlay uses the ahead-of-time back model.** Drawer, folder, picker, long-press menu, and settings sub-screens declare back handling via `BackHandler` / `PredictiveBackHandler` (activity-compose 1.8.0+) with state-driven `enabled`; no screen uses `KeyEvent.KEYCODE_BACK` or `onBackPressed`. *Test:* grep for the deprecated APIs; open each overlay and start a back swipe — a preview animates. [Predictive-back doc; July 2022 blog post]
- **R21 — Back callbacks are single-responsibility and state-driven.** One callback per responsibility; `enabled` follows observable UI state (StateFlow / Compose State), not a conditional checked after the event; no callback runs business logic or logging. *Test:* review each callback's `enabled` expression; it reads state, not a branch. [Predictive-back doc, "Callback guidelines"]
- **R22 — At the home-screen root, back belongs to the system.** With no overlay open on the home screen, a back gesture plays the back-to-home system animation; Marlow does not intercept it, open a menu, or do nothing. *Test:* on the home root, swipe back; the system animation plays. [Predictive-back doc; I/O 2024 "predictive back leaves developer options"]
- **R23 — Predictive back works in Settings.** Navigating back through Marlow's settings shows the predictive animation (the Lawnchair 14 Beta 3 bar). *Test:* drill two levels into Settings and swipe back at each level. [Lawnchair 14 Beta 3]

## Large screens and foldables

- **R24 — Layout is a function of window size class, not device type.** Grid columns, drawer columns, dock, and widget placement derive from the current window size class (Compact/Medium/Expanded, plus Large/XLarge where applicable) via WindowManager metrics — never from "is tablet" / "is foldable" / a fixed aspect ratio. *Test:* run Marlow in split-screen at three widths; the layout changes at the class breakpoints, not at a device check. [Support different display sizes; trifolds doc]
- **R25 — Fold/unfold reflows in place without losing state.** Unfolding or folding re-lays out the home screen in the same activity: same page, same widget positions, no restart, no lost scroll/draft state. *Test:* unfold mid-scroll in the drawer and mid-drag of a widget; both survive. [Trifolds doc; I/O 2023 large-screen session]
- **R26 — Orientation comes from the occupied area, not the device.** Marlow never assumes rotation_0 = portrait (the original Pixel Fold opens in landscape). *Test:* launch on a landscape-foldable emulator/device; the grid is correct without a rotation. [Trifolds doc]
- **R27 — Tablet layout is a real layout.** On expanded widths the launcher and its settings adjust to the larger width (wider grid, two-pane settings where appropriate) rather than stretching the phone layout. *Test:* open Marlow on a tablet width; settings and drawer use the width. [Lawnchair 14 Beta 3 "Proper tablet support"]

## Search, feed, and import

- **R28 — Search is global and fast, with switchable providers.** Drawer search covers apps, contacts, files, settings, and the web (or documents which of those it covers); search providers are switchable; offline search does not hang on a web timeout. *Test:* search offline; results return promptly; switch provider and repeat. [Lawnchair 14; Lawnchair 15 Beta 2.1 web-search timeout fix]
- **R29 — Settings are searchable and organised.** Marlow's settings can be searched; related settings live together (e.g. all search settings on one screen). *Test:* search settings for "icon" and "widget" and land on the right screens. [Nova 6.0 searchable settings; Lawnchair 15 Beta 1 settings reorganisation]
- **R30 — Nova backup import works.** Marlow imports a Nova Launcher backup's home-screen/dock grid, widgets/folders, and selected icon pack (the Lawnchair 15 Beta 3 scope), or documents precisely what it does not import. *Test:* import a real Nova backup; grid, widgets, folders, and icon pack match. [Lawnchair 15 Beta 3]
- **R31 — A swipe-right feed, if shipped, is personal context first.** Calendar, people, and recents lead; news is secondary or absent. *Test:* open the feed with no news sources configured; it still shows useful personal context. [Microsoft Launcher "The Feed"; Smart Launcher "Your Feed"]
- **R32 — Folder padding and icon-pack persistence survive restarts.** Folder padding is correct after a restart, and the selected icon pack is still applied after a restart (no reset to default). *Test:* set a custom pack and folder padding, force-stop, relaunch, compare. [Microsoft Launcher 6.2 changelog]

## Performance and OEM robustness

- **R33 — App-closing animations have a kill-switch.** If Marlow animates app close/open transitions, a setting disables them — OEM gesture glitches (notably One UI) must be escapable without uninstalling. *Test:* toggle the animation off; launches/closes are instant and glitch-free on the test device. [Lawnchair 15 Beta 2.1 `GestureNavContract` toggle]
- **R34 — No OEM-skin crash class ships untested.** Marlow is exercised against the known launcher-hostile skins (One UI, ColorOS at minimum, via emulator or device) for drawer, widget add, and settings-with-wallpaper-preview paths. *Test:* run the smoke set on each skin; no crash on open/settings/widget-add. [Lawnchair 14 Beta 3 crash fixes]
- **R35 — State bugs are tested, not assumed away.** Search state clears when the drawer hides; bottom sheets reflect current state on open; customising an app that is being uninstalled does not crash; icon scaling on one surface does not leak to another. *Test:* script each of these four scenarios. [Lawnchair 15 Beta 2]

---

# Part E — Top 10 takeaways most likely to matter for a stock-feeling launcher

1. **The launcher is a host, not a showcase.** Its quality is measured by how faithfully it renders other people's widgets, icons, and shortcuts. Get the host contract right (R1, R2, R10, R12) before adding launcher-original features. [App Widget Design Guidelines; Adaptive icons doc]
2. **The Android 12 widget contract is the stock feeling.** Rounded corners + padding enforced by the host, responsive resize that reflows, truthful picker previews, dynamic colour. A launcher that misses any of these feels pre-2021. [I/O 2021]
3. **Derive every colour from the wallpaper via Material 3 roles — subtly, everywhere.** No hardcoded accents anywhere in launcher chrome; the palette should harmonise, not shout. [Material You AOSP page; Niagara Android 12 changelog]
4. **Back is a preview, not an event.** Use `PredictiveBackHandler`/`BackHandler` with state-driven `enabled` on every overlay; at the home root, let the system play back-to-home. [Predictive-back doc; July 2022 blog post; I/O 2024]
5. **Draw edge-to-edge and claim almost no exclusion rects.** Insets belong on content, not wallpaper; the home gesture is never excluded. [Dev Summit 2019, Banes/Shah; gesture-navigation doc]
6. **Layout is a function of window size class.** Never branch on device type; fold/unfold must reflow in place with state intact. [Support different display sizes; trifolds doc; I/O 2023]
7. **One icon model across every surface.** One mask, one safe zone, one shape vocabulary, one size model for desktop, drawer, dock, and folders — and themed icons honoured or explicitly declined. [Adaptive icons doc; Nova 6.0; Niagara]
8. **Widget add, configure, and resize must not fail.** Configuration-on-add (Android 14+), resize correctness, and stack-position reset on Home are the three widget bugs every launcher in this file fixed more than once. [Lawnchair 15 Beta 2.1; Smart Launcher 6.6]
9. **Rebase first, customise second.** Lawnchair's and Nova's biggest releases are AOSP/Launcher3 rebases with conformance work (Sentence case, Pixel colours, predictive back) called out as features. Stock-feeling is conformance, not invention. [Lawnchair 14 Beta 3; Nova 4]
10. **Import is a feature, and state bugs are the quality bar.** Nova-backup import wins switchers; search-state clearing, bottom-sheet state, icon-pack persistence across restarts, and OEM-skin crashes are what decide whether the launcher feels solid after a week, not on day one. [Lawnchair 15 Beta 3; Lawnchair 15 Beta 2; Microsoft Launcher 6.2]

---

## Sources consulted (all URLs verbatim as returned by search/fetch)

**Google talks, docs, and blog posts**
- https://developer.android.com/design/ui/mobile/guides/widgets/adapt
- https://developer.android.com/codelabs/glance
- https://developer.android.com/develop/ui/compose/system/icon_design_adaptive?authuser=4
- https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture?authuser=5
- https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav?hl=en&authuser=002
- https://developer.android.com/develop/ui/compose/layouts/adaptive/support-different-display-sizes?u=http%3A%2F%2Fdeveloper.android.com%2F%2Fcloud.google.com
- https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/trifolds-and-landscape-foldables?hl=en&authuser=5
- https://developer.android.com/about/versions/15/features?source=post_page-----26ee709b66b4--------------------------------
- https://developer.android.com/events/dev-summit?authuser=7
- https://developer.android.com/events/dev-summit/talks/evolving-tiles-to-wear-widgets-using-jetpack-glance-and-remote-compose
- https://developer.android.com/dev-summit/schedule/day1?nav=true
- http://developer.android.com/guide/practices/ui_guidelines/widget_design.html
- http://developers.android.com/static/shareables/design/app-shortcuts-design-guidelines.pdf
- https://source.android.com/docs/core/display/material?hl=en
- https://codelabs.developers.google.com/codelabs/apply-dynamic-color
- https://android-developers.googleblog.com/2022/07/prepare-your-app-to-support-predictive-back-gestures.html?hl=th
- https://android-developers.googleblog.com/2024/05/the-second-beta-of-android-15.html
- https://android-developers.googleblog.com/2023/05/14-things-to-know-for-android-developers-google-io.html?m=1
- https://android-developers.googleblog.com/2023/05/modern-android-development-at-google-io.html?hl=fr
- https://android-developers.googleblog.com/2026/05/17-things-android-developers-google-io.html
- https://android-developers.googleblog.com/2019/08/google-releases-source-code-for-google.html?m=0
- https://android-developers.googleblog.com/search?updated-max=2025-03-12T09:00:00-07:00&max-results=10&reverse-paginate=true&start=7&by-date=false&hl=fr
- https://blog.google/products-and-platforms/platforms/android/widgets-just-got-better-android-12/
- https://www.androidauthority.com/android-widgets-overhaul-remote-compose-3670122/
- https://9to5google.com/2025/03/03/android-widgets-play-store/?extended-comments=1
- https://www.XDA-Developers.com/google-android-12-widgets-overhaul/
- https://www.xda-developers.com/graphic-design-for-android-developers-creating-the-ideal-app-icon/

**Launcher builders**
- https://github.com/LawnchairLauncher/lawnchair/releases
- https://lawnchair.app/blog/lawnchair-15-beta-1
- https://telegram.me/s/lawnchairci
- https://play.google.com/store/apps/details?id=app.lawnchair.play&hl=en-US
- https://4pda.to/forum/index.php?showtopic=916981&st=640 (Niagara Android 12 changelog, forum mirror of the team's post)
- https://www.AndroidPolice.com/2015/03/10/nova-launcher-gets-a-huge-redesign-now-in-beta-material-design-new-code-base-and-tons-of-new-options/
- https://9to5google.com/2015/03/10/nova-launcher-beta-material/
- https://www.droid-life.com/2014/05/27/nova-launcher-3-0-beta-1-released-brings-a-ton-of-enhancements-and-new-features/amp/
- https://www.XDA-developers.com/nova-launcher-6-0-beta-new-features/
- https://play.google.com/store/apps/details?id=ginlemon.flowerfree&hl=en_GB
- https://www.smartlauncher.net/blog/smart-launcher-6-6-build-011-clearer-weather-more-personal-folders
- https://www.smartlauncher.net/blog/the-sticky-update
- https://www.smartlauncher.net/blog/2025wrapupsl66
- https://www.androidpolice.com/smart-launcher-65-update-announced-new-features/
- https://www.androidcentral.com/microsoft-launcher-android-takes-over-arrow-new-features-design-tweaks
- https://www.windowscentral.com/microsofts-arrow-launcher-nabs-big-update-new-features-tow
- https://thewincentral.com/microsoft-launcher-android-updated-wit-new-feature-changelog-inside/?amp
- https://thewincentral.com/microsoft-launcher-for-android-updated-with-improved-home-screen-more-changelog-inside/
- https://mspoweruser.com/da/microsoft-launcher-updated-2/
- https://luwall.com/hybrid-launcher/fold-launchers (Nova ownership/status context only)

## Gaps and limits (stated plainly)

- No Google talk is *about* building a launcher; launcher guidance was assembled from widget, gesture, Material You, and large-screen talks/docs. Any "launcher talk" claim beyond the sessions listed above would be invented.
- Exact YouTube video IDs were not verified for most I/O talks in the sources returned, so none are given as links. Titles and years are given; the reader should search the Android Developers YouTube channel by title.
- Niagara's primary changelog site was reached via a forum mirror (4PDA); the substance (Material You backport, themed-icon refusal, rounded widget corners, approximate location) is quoted from the team's own post as reproduced there.
- Microsoft Launcher has no single design blog; its section rests on Garage-era changelogs as reported by the press.
- I/O 2026 / Android 17 / Remote Compose material is very recent; host-side requirements should be re-verified against current Glance docs before implementation.
- No person in this file is credited with employment, education, or qualifications beyond what their own byline, Play Store listing, or release note states.
