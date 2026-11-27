# 02 — How stock launchers actually work and feel

**Project:** Marlow Launcher (`app.launcher`) — a Pixel-style Android home launcher, Compose + Material 3.
**Date:** 2026-11-26
**Scope:** Research only. No code was touched for this document.
**Method and honesty note:** Everything below comes from public sources cited inline — Google's own help pages and developer documentation, launcher developers' own blogs / changelogs / interviews, and independent reviews and APK teardowns. No claim in this file rests on private employment, insider access, or invented qualifications. Where public sources disagree, are thin, or describe a beta/flag-gated feature that may never have shipped, that is said in the text. A separate track is reading the AOSP Launcher3 source itself; this file deliberately does **not** duplicate that — it covers behaviour, history, teardowns, and builder writing.

---

## 1. What "stock" means — Pixel Launcher as the reference

"Stock" in this document does **not** mean AOSP Launcher3 bare. It means **Pixel Launcher as Google ships it on a Pixel** — Launcher3 plus Google's own layers (At a Glance / Smartspace, Pixel Search, Discover feed, Material You theming, Quickstep recents). One source puts the distinction plainly: Pixel Launcher is "Google's preferred Pixel interface rather than 'stock Android' in the sense of a universal, untouched Android experience" — AOSP is pure Android, Pixel Launcher adds Google's features on top, and is the closest to stock among manufacturer launchers without being stock itself.

- https://blog.rottenwifi.com/pixel-launcher-everything-you-need-to-know-about-googles-pixel-home-screen/
- https://en.namu.wiki/w/Pixel%20Launcher

That matters for Marlow: copying AOSP Launcher3 alone would feel *plainer* than a Pixel; copying Pixel means copying its **opinions** — including its restrictions. The stock feeling is not "maximum features". It is: a small number of strong opinions, applied consistently, with nothing half-finished visible.

### 1.1 The stock home, top to bottom

On a current Pixel the first home page, in order, is:

1. **At a Glance, pinned to the top of the main home screen.** It cannot be moved or removed (as shipped through Android 15 / early Android 16; a Canary toggle to remove it entirely was spotted in 2025 but was non-functional at the time — see §6.1). Even with every smart feature switched off it collapses to a static date/weather line and still occupies its row.
   - https://9to5google.com/2024/12/25/google-pixel-set-up/
   - https://www.androidpolice.com/not-fan-of-pixel-launcher/
   - https://www.howtogeek.com/google-is-about-to-fix-one-of-my-least-favorite-pixel-features/
2. **The workspace grid** below it — app icons, folders and widgets the user places.
3. **The dock (hotseat / favourites row)** — fixed at **5 icons** on Pixel Launcher. You can add/remove icons within that limit; you cannot add a second row, resize it, or move the search bar out of it.
   - https://techwiser.com/nova-launcher-vs-pixel-launcher-whats-the-difference/
   - https://unstore.io/discover/best-pixel-launcher-alternatives/
4. **The Google Search bar, permanently below the dock** (in the dock area, above the gesture handle). It cannot be moved, hidden or repositioned on Pixel Launcher.
   - https://www.androidpolice.com/not-fan-of-pixel-launcher/
   - https://techwiser.com/nova-launcher-vs-pixel-launcher-whats-the-difference/
5. **Swipe right (left-most page) = Google Discover feed.** Swipe left/right moves between home pages; swipe right past the first page reaches Discover. Disabling it leaves dead space — there is no replacement page.
   - https://www.androidauthority.com/pixel-launcher-setup-productivity-3522859/
   - https://unstore.io/discover/best-pixel-launcher-alternatives/

The two immovable anchors — At a Glance at the top, search at the bottom — are the single most complained-about thing about Pixel Launcher *and* the single most recognisable thing about it. Reviewers repeatedly call them "forced ads for Google services" and "a waste of space", while also conceding they are what makes a Pixel home screen instantly identifiable. Marlow does not have to copy the immovability; it does have to decide its anchors deliberately, because an empty top and an empty bottom read as "unfinished", not "clean".

- https://www.androidpolice.com/not-fan-of-pixel-launcher/
- https://innotechinsider.com/future-tech/pixel-launcher-wasted-screen-space-android/

### 1.2 Home grid and spacing

- **Grid options on Pixel (current): 2×2, 3×3, 4×4, 4×5, 5×5.** 9to5Google's first-setup guide lists exactly those five and recommends 5×5 for the most content. The maximum is 5×5 — no 5×6, no custom density, no placing icons between grid lines, no overlapping or stacking widgets.
  - https://9to5google.com/2024/12/25/google-pixel-set-up/
  - https://www.androidpolice.com/not-fan-of-pixel-launcher/
- **Grid applies to home and drawer together in effect:** the 5×5 setting "maximizes the size of the dock and apps list" (Android Authority productivity setup).
  - https://www.androidauthority.com/pixel-launcher-setup-productivity-3522859/
- **What you cannot do on Pixel:** place an icon or widget off-grid, overlap widgets, stack widgets. Samsung One UI and Nova both allow overlap/stacking; Pixel does not.
  - https://www.androidpolice.com/not-fan-of-pixel-launcher/

Spacing rhythm in practice: icons sit on a regular grid with generous, *even* gutters; labels are centred under icons; nothing is crammed to an edge. The "wasted space" complaint on a 6.8-inch phone at 5×5 is the flip side of the same rule — Pixel would rather leave even dead space than break the rhythm. For Marlow the lesson is: pick one gutter, one icon size and one label size per grid and hold them everywhere (home, drawer, folders, dock) — inconsistency between those surfaces is the fastest amateur tell.

### 1.3 Icon size, shape, consistency

- **Adaptive icons:** two layers (foreground + background), each 108×108 dp; the system clips them with a mask chosen by the launcher. The load-bearing artwork must fit in the centred **66×66 dp safe zone**; the logomark should occupy roughly 48–66 dp of the 108 dp canvas. Background carries colour/gradient only, no detail.
  - https://developer.android.com/distribute/aep/aep-req-theme-app-icons?authuser=69
  - https://github.com/ichintansoni/skills-master/blob/HEAD/skills/android/design/technologies/m3-app-icons/SKILL.md
  - https://codelabs.developers.google.com/design-android-launcher
- **Legacy launcher icon size: 48 dp square** (48 px mdpi, 72 hdpi, 96 xhdpi, 144 xxhdpi, 192 xxxhdpi).
  - https://android-developers.googleblog.com/2013/07/making-beautiful-android-app-icons.html?hl=th
- **Pixel mask is a circle by default.** Other OEMs use squircle / rounded square / teardrop. An icon that only looks right in one mask looks broken in the others — hence the safe-zone rule.
  - https://github.com/mohamedabdallah-14/prompt-to-asset/blob/HEAD/docs/research/09-app-icon-generation/9b-android-adaptive-themed-icons.md
- **Themed icons (Material You, Android 13+):** a third, `<monochrome>` layer — a single-colour silhouette. The system tints it from the wallpaper palette. It only works if the app developer supplied that layer; many did not, so enabling Themed Icons on Pixel historically produced a *mixed* home screen — some icons themed, some full-colour — which reviewers call "messy" and "broken-looking".
  - https://developer.android.com/distribute/aep/aep-req-theme-app-icons?authuser=69
  - https://www.androidpolice.com/not-fan-of-pixel-launcher/
  - https://www.androidpolice.com/android-12-review/
  - https://codelabs.developers.google.com/design-android-launcher
- **Pixel does not support third-party icon packs.** That is a deliberate Pixel limitation, not an Android one — Nova, Lawnchair, Smart Launcher and Microsoft Launcher all do.
  - https://www.androidpolice.com/not-fan-of-pixel-launcher/

**Marlow implication:** themed icons must be all-or-nothing in perception. Either force-theme every icon (the Android 16 QPR2 approach — see §6.1) or leave unthemed icons visibly unthemed and accept the mix; a half-themed grid reads as a bug. And whatever shape Marlow picks, it must survive the circular mask at 48 px — the small-size check, not the 512 px Play icon, is the real test.

### 1.4 The dock

- 5 icons, labels off in the dock (Pixel convention; Nova's Pixel-look guide sets dock label Off explicitly).
  - https://techwiser.com/nova-launcher-vs-pixel-launcher-whats-the-difference/
  - https://xdaforums.com/t/guide-pie-vertical-app-switcher-pixel-launcher-looknfeel.3830714/
- The dock is present on **every** home page (it is the hotseat, not a page-1 widget). The search bar sits below it on every page too — the 2017 Pixel 2 change moved search to the bottom and At a Glance to the top, with search "displayed on all the pages" while At a Glance "remains in the homepage itself".
  - https://www.thegoandroid.com/download-new-pixel-2-launcher-no-root-get-pixel-2-features/72332/
- **Default dock contents — NOT firmly established from public sources.** This is a gap, stated honestly: Google's help pages do not publish a canonical first-boot dock list. Screenshots and user setups commonly show Phone / Messages / Chrome / Photos / Camera (in some order); a Launcher3-fork README claims the "real Pixel Launcher's setup" is Phone, Messages, Gmail, Store, Browser, Camera (six items — inconsistent with the 5-icon dock, so treat that fork's claim with suspicion). Do not hard-code a "Pixel default dock" for Marlow from any of these; verify on a real Pixel or define Marlow's own curated dock and label it as Marlow's choice.
  - https://www.androidauthority.com/why-i-stick-with-default-pixel-launcher-3616782/ (a *user's* five: Phone, WhatsApp, Camera, Chrome, Spotify — not a factory default)
  - https://github.3154930252.workers.dev/android-hacker/Launcher3/tree/60e1f46523720a3cb19f4036c5e4bfe4c618c101 (fork claim, unverified)

### 1.5 The search pill — placement and behaviour

- **Placement:** fixed at the bottom, below the dock, on every home page. Pill-shaped, with the coloured "G" on the left and microphone / Lens icons on the right (exact trailing icons have varied by version).
  - https://www.androidpolice.com/not-fan-of-pixel-launcher/
  - https://www.thegoandroid.com/download-new-pixel-2-launcher-no-root-get-pixel-2-features/72332/
- **Classic behaviour (through Android 16 QPR1):** tapping it opened Pixel Launcher's own unified search — a translucent sheet with the wallpaper visible at the top, prompt "Search web and more", a row of app suggestions and previous queries, combining web suggestions with local results (installed apps, contacts, settings, files, conversations, tips).
  - https://www.androidheadlines.com/wp-json/wp/v2/posts/1255326
  - https://eztoolset.com/pixel-launcher-search-now-opens-the-google-app-its-intentional-not-a-bug/
- **The November 2025 change (Android 16 QPR2 / Google app 16.48–16.49):** Google intentionally rerouted the home-screen bar to open the full-screen Google app search UI instead, announced in the November 2025 Pixel Drop as "An upgraded home screen search bar" giving access to AI Mode from the bar. Local results (apps, contacts, settings shortcuts for Clock/Contacts/Tips/Play Store/Settings/Wallet) were reduced or absent from that surface. Pixel Launcher search itself remained reachable from the search bar at the top of the app drawer. Users could restore the old unified search only via an ADB device-config override (`enable_one_search`), which is not a consumer path.
  - https://www.androidheadlines.com/wp-json/wp/v2/posts/1255326
  - https://dataconomy.com/wp-json/wp/v2/posts/85754
  - https://eztoolset.com/pixel-launcher-search-now-opens-the-google-app-its-intentional-not-a-bug/
  - https://tms-outsource.com/blog/posts/android-app-drawer-vs-home-screen/
- **Android 16 QPR1 beta redesign:** a segmented search bar — smaller bar plus a separate round AI Mode button to the right, inside a larger pill — tested in beta, bringing the dock bar in line with the updated search-widget design. The drawer-top search bar did *not* get the segmented design at the same time.
  - https://www.androidauthority.com/android-16-qpr1-beta-2-segmented-search-bar-3567059/
- **"Swipe up to start search" setting:** Home settings → Apps list / Search settings → "Swipe up to start search" opens the drawer *with the search field focused and the keyboard up*. This is off by default in the classic flow; power users turn it on.
  - https://www.androidpolice.com/android-15-dp2-switches-up-pixel-launcher-settings/
  - https://www.howtogeek.com/youre-using-the-app-drawer-on-your-android-phone-wrong/

**Marlow implication:** search has *two* surfaces on Pixel and they are not the same — dock pill (web-first, now Google-app-first) vs drawer-top bar (device-first). Marlow should decide which surface does what and keep them distinct, rather than making both open the same screen. And: whatever the pill opens must open *fast* — the pill is the most-tapped element on the home screen.

### 1.6 At a Glance — what it shows, placement, typography

- **Placement:** top of the main home screen only (and on the lock screen, where its placement has been reworked repeatedly). It is *not technically a widget* — Android's widget APIs do not allow horizontally scrolling widgets, so At a Glance is a launcher-integrated Smartspace surface presented as a horizontally swiping carousel of complications.
  - https://www.androidauthority.com/pixel-lock-screen-android-16-3546942/
  - https://www.phonearena.com/news/new-pixel-lock-screen-look-includes-changes-for-at-a-glance-placement_id169745
- **Default / resting state:** day + date on the first line, weather (condition + temperature) beside/below it. Tapping the weather opens Pixel Weather (or the Google app's weather); tapping the date opens Calendar.
  - https://9to5google.com/2024/12/25/google-pixel-set-up/
  - https://9to5google.com/2025/03/25/pixel-weather-location/
- **Complications (swipe the carousel):** today/tomorrow weather, calendar events ("time to leave" for an event), reminders, package-delivery ETA, food/household order arrival, flight info, Now Playing song, air-quality alerts, doorbell (Ring/Nest) "who's at the door", timers/stopwatch, battery / connected-device info, earthquake alerts in supported regions. Which appear depends on permissions, apps installed, and server-side flags — there is no fixed list a user can rely on.
  - https://www.phonearena.com/news/new-pixel-lock-screen-look-includes-changes-for-at-a-glance-placement_id169745
  - https://www.androidauthority.com/pixel-lock-screen-android-16-3546942/
  - https://en.androidayuda.com/widget-pixel-2-android-p-beta/
- **Content rules (observed):** one complication at a time is primary; an upcoming calendar event *replaces* the date line ("if your calendar has an event scheduled for the next few hours, it will appear where the date should be"). At night a next-day forecast preview appears. Content is contextual and time-sensitive — it is not a static widget, and an At a Glance that always shows the same two lines feels dead.
  - https://en.androidayuda.com/widget-pixel-2-android-p-beta/
  - https://9to5google.com/2025/03/25/pixel-weather-location/
- **Customisation:** long-press At a Glance → Customize → gear icon → toggles per feature (weather, calendar, reminders, etc.). You can reduce it to day/date only; you cannot remove it (pre-Android 16 QPR2 Canary toggle — see §6.1).
  - https://9to5google.com/2024/12/25/google-pixel-set-up/
- **Typography:** large, light-weight date line; weather in the same size or slightly smaller; no card background, no border — text directly on the wallpaper. That "no chrome" treatment is why it reads as part of the wallpaper rather than as a widget, and why a boxed/card At a Glance clone instantly looks third-party.
- **Lock-screen divergence:** on the lock screen At a Glance sits above the clock when the clock is centred, below it when the clock is at the top; Android 16 Beta 4 tested splitting date/weather from the information complication to reduce clutter. Marlow is home-only, so this is context, not a requirement — but it shows Google itself considers the combined strip "a bit busy".
  - https://www.androidauthority.com/pixel-lock-screen-android-16-3546942/

### 1.7 Widget placement

- **How to add (Google's own instructions):** long-press any blank space → Widgets → expand an app (down arrow) → search for an app or widget → long-press the widget and drag it to the home screen. Some widgets resize: long-press the placed widget to reveal white resize borders, drag to resize.
  - https://support.google.com/pixelphone/answer/14852556
- **Placement rules:** widgets snap to the grid; they cannot be placed between grid lines or overlapped (Pixel). Resizing is to grid multiples. A widget that does not fit the free space cannot be dropped there — the launcher must show that *before* the drop, not after.
  - https://www.androidpolice.com/not-fan-of-pixel-launcher/
  - https://support.google.com/pixelphone/answer/14852556
- **Widget picker (Android 15):** recommendations are now categorised — Essentials, Social, Entertainment, Health & fitness, News & magazines, Suggested for you, Your Chill Zone, Weather — instead of a single unexplained recommendation at the top. A "+ Add" button places the widget in an empty spot without drag-and-drop. Both were flag-gated in Android 15 Beta 1 (Mishaal Rahman teardown via Android Authority) and the categorised picker was confirmed in the October 2024 Pixel Feature Drop.
  - https://www.androidauthority.com/android-15-pixel-launcher-changes-3434365/
  - https://www.cloudspress.com/android-15s-pixel-launcher-changes-app-animations-widget-suggestions-and-recents/
- **Android 16 (reported, not shipped at time of writing):** a "tiered widgets" flag suggesting the picker may show only a few widgets at the top with a "Show all" button. Treat as unconfirmed direction, not behaviour to copy.
  - https://zambiatek.com/2025/10/07/google-pixels-might-soon-force-you-to-dig-deeper-for-all-your-widgets/

### 1.8 Folders — look, open animation, naming

- **Create:** drag one app icon directly on top of another. That is the only creation method on Pixel — there is no "New folder" menu item, and there are **no folders in the app drawer**.
  - https://support.google.com/pixelphone/answer/14852556
  - https://blog.rottenwifi.com/how-to-create-app-folders-on-google-pixel-and-pixel-xl/
- **Look:** circular folder icon (since the 2016 refresh) showing a 2×2 mini-grid preview of the first four apps inside; tapping opens a popup/folder view over the home screen, dimming the rest. Folder preview is 2×2 only — Nova's "beyond the stock 2×2 grid" preview is listed as a Nova advantage.
  - https://www.apkmirror.com/apk/google-inc/pixel-launcher/
  - https://unstore.io/discover/best-pixel-launcher-alternatives/
- **Naming:** open the folder, tap the name/text at the bottom, type, confirm. Pixel suggests a name (in Android 11 DP1 teardowns, auto-folder naming was being tested). No nested folders — Pixel does not support them.
  - https://support.google.com/pixelphone/answer/14852556
  - https://www.pixelartworkshop.com/guides/how-to-organize-pixel
  - https://www.xda-developers.com/tag/google-pixel-launcher/ (Android 11 DP1: auto-folder naming; suggested apps on the dock)
- **Add/remove:** drag an app into the open folder (or onto the closed folder icon); drag out of the open folder to remove. Dragging the last app out dissolves the folder.
- **What feels cheap:** a folder that opens as a full-screen page, a folder preview that shows icons at a different scale from the home grid, a folder name that truncates, or a folder that animates with a different easing from the app-open animation. Stock folders feel like the home screen *folding open*, not navigating away.

### 1.9 The app drawer — layout, search, ordering, how you get there

- **How you get there:** swipe up from anywhere above the search bar on the home screen. There is no drawer button/icon on Pixel (the old faded dock / arrow was removed in Android 9 Pie). A short swipe up from the bottom edge with gesture navigation goes home; a swipe up *on the home screen itself* (above the search bar) opens the drawer; swipe-up-and-hold opens Recents. The three gestures are nearly identical and users confuse them — see §1.11.
  - https://support.google.com/pixelphone/answer/14852556
  - https://techcrunch.com/2016/10/04/googles-pixel-smartphones-get-a-new-look-with-pixel-launcher/
  - https://www.androidpolice.com/2020/07/09/android-11s-launcher-lets-you-swipe-up-on-secondary-homescreens-to-go-back-to-the-main-one/
- **Layout:** a vertical scrolling grid, alphabetical by default, **with no other ordering option** — "The master list of apps in the Launcher are alphabetized by default — they can't be arranged any other way!" (Google Pixel Community, locked informational answer). Home-screen icons are *not* re-alphabetised; the drawer is the master list, home pages are the custom display.
  - https://support.google.com/pixelphone/thread/305487540/how-can-i-alphabetize-apps?hl=en
  - https://blog.rottenwifi.com/how-can-i-put-my-apps-in-alphabetical-order/
- **Search:** a search bar at the top of the drawer. Typing filters the alphabetical grid; search also surfaces settings, contacts, files and shortcuts in the unified-search era. After the November 2025 change, drawer-top search remains the place Pixel Launcher search still lives.
  - https://tms-outsource.com/blog/posts/android-app-drawer-vs-home-screen/
  - https://eztoolset.com/pixel-launcher-search-now-opens-the-google-app-its-intentional-not-a-bug/
- **Suggestions row:** a row of suggested apps at the top of the drawer (and optionally on the home screen), built from recent use / frequency / routines. Pixel lets you toggle "Suggestions in all apps list" on/off; Android Police's critique (Aug 2025) says the row "can't be turned off" — the two disagree, which likely reflects version/settings-location drift (the toggle moved between Home settings → Search settings and a newer "Apps list settings" submenu in Android 15 DP2). Marlow should have *one* clearly labelled toggle and not move it.
  - https://www.androidauthority.com/pixel-launcher-setup-productivity-3522859/
  - https://www.androidpolice.com/not-fan-of-pixel-launcher/
  - https://www.androidpolice.com/android-15-dp2-switches-up-pixel-launcher-settings/
- **What Pixel drawer does NOT have:** folders, tabs/categories, custom order, hide-apps (except via Private Space, a locked secondary profile — described as "a bulky solution to a simple problem").
  - https://www.androidpolice.com/not-fan-of-pixel-launcher/
  - https://tms-outsource.com/blog/posts/android-app-drawer-vs-home-screen/
- **Label truncation:** Android 15 DP2 teardowns reported a possible "Show long app names" option to show app names on two lines in search results and the drawer — because Pixel "just trims the app names longer than nine characters". That nine-character trim is reported, not Google-documented; treat it as the observed stock behaviour to match or deliberately beat (two-line labels are the obvious fix, and Pixel itself was reportedly considering it).
  - https://www.androidpolice.com/android-15-dp2-switches-up-pixel-launcher-settings/

### 1.10 First-run experience — what Pixel asks, in what order

There is **no Pixel Launcher first-run wizard** in the way a third-party launcher has one. The first-run experience is the *Android setup* flow, and the launcher inherits its result:

1. **OS setup (SetupWizard):** language, Wi-Fi, Google account sign-in, restore from backup (which restores the *previous* Pixel Launcher layout, if the backup has one), fingerprint/face, Google Assistant/Gemini setup. The launcher is not yet in front of the user for most of this.
2. **First home screen:** the restored layout, or — on a clean setup with no restore — the factory default layout (At a Glance, default grid, dock, search bar). Google publishes no canonical list of which apps are pre-placed on a clean first boot (see §1.4 gap).
3. **What the launcher itself asks for, later and in context:** notification access (for dots/badges — see §1.12), widget permissions when a widget is added, Discover personalisation inside the feed. Pixel Launcher does not front-load a permission checklist.

For a **third-party launcher** (which is what Marlow is, even though it aims to feel stock), the first-run contract is different and is set by Android itself:

- **Since Android 10, a launcher can ask to become the Home app itself.** Open the new launcher, follow its setup, Android shows: "Set [launcher] as your default home app?" → "Set as default". Alternatively the user goes to Settings → Apps → Default apps → Home app and picks it. Opening a launcher from the drawer does *not* make it default; pressing Home still goes to the old home screen until the setting changes.
  - https://www.axolauncher.com/blog/how-to-change-default-launcher-android
  - https://www.tryreclaim.io/blog/change-default-launcher-android/
  - https://eztoolset.com/how-to-change-the-default-android-home-screen-launcher/

**Marlow implication (important):** the "first boot before the user has done anything" problem is Marlow's biggest stock-feel risk. A Pixel on first boot is *already* a complete home screen. A third-party launcher on first open is, by default, an empty grid plus a setup wizard — which is exactly what reads as amateur. Marlow's first run must therefore be: (a) ask for Home-app default **once**, early, in plain language, with a visible fallback path in Settings; (b) show a **complete, curated home screen immediately** — seeded dock, seeded page-0 apps/folders, a clock/weather element — *before* asking for anything optional; (c) defer every optional permission (notifications, contacts, etc.) to the moment its feature is first used. A wizard that asks five questions in front of a blank grid is the anti-stock experience. (This is also what the Marlow audit-fix build already moved toward — self-healing seed, curated page 0 + dock, clock widget, weather on by default — and the research supports keeping that direction.)

Third-party launchers' own first runs, for contrast: Niagara's setup is "select your favourite apps" and they appear as a vertical list — one question, then a finished home screen. Smart Launcher sorts apps into categories automatically on install so the drawer is organised "from day one, not after an hour of configuration". Both front-load *one* decision and deliver a finished screen; neither shows an empty grid.

- https://www.appcracy.com/app/niagara-launcher-home-screen.html
- https://free-apps-android.com/smart-launcher-6/

### 1.11 Overview / Recents relationship

- **Recents is part of the launcher on Pixel.** Since Android 9 Pie, the Overview/app-switcher is bundled with Pixel Launcher (Quickstep), not with the system: disabling Pixel Launcher with adb brought back the old vertical Overview. Google's own framing, quoted at the time: "architecturally what we've done is combine the All Apps and Overview spaces into one."
  - https://www.androidpolice.com/2018/05/14/android-ps-new-overview-app-switcher-part-launcher/
  - https://aospbooks.github.io/aosp-internal-book/49-launcher3/
- **Gestures (gesture navigation, Pixel default):** swipe up quickly and release → home (first home page, from any page since Android 11); swipe up and hold → Recents/Overview; swipe up on the home screen above the search bar → app drawer. With 3-button navigation, Overview is a button instead.
  - https://www.androidpolice.com/2020/07/09/android-11s-launcher-lets-you-swipe-up-on-secondary-homescreens-to-go-back-to-the-main-one/
  - https://tms-outsource.com/blog/posts/how-to-enable-recent-apps-button-in-android/
- **What a third-party launcher cannot do:** Quickstep — gesture navigation and the Recents screen — is reserved to the stock launcher. Third-party launchers are treated by the system as regular apps for this purpose. Google disabled gesture navigation entirely for third-party launchers in Android 10 betas, then restored it with limitations; the limitations persist. Consequences documented across 2024–2026: choppy/laggy swipe-up animations, Recents opening only after a longer swipe-up-and-hold with haptic, the swipe-up-to-home gesture sometimes needing two or three attempts on Lawnchair, loss of Select/Screenshot in Recents on Pixel with a third-party launcher, and some OEMs (notably Xiaomi) disabling gesture navigation outright with a third-party launcher. The root fix, QuickSwitch (a Magisk module that makes the third-party launcher the Recents provider), requires root.
  - https://www.howtogeek.com/gestures-dont-work-well-with-android-launchers-and-its-googles-fault/
  - https://www.androidpolice.com/third-party-android-launcher-developers-join-forces-voice-frustrations-to-google/
  - https://android.gadgethacks.com/how-to/android-10-gesture-controls-grayed-out-heres-fix-0200786/
  - https://techverge.io/download-lawnchair/
- **Launcher-developer response:** in 2024 the developers of Smart Launcher, Niagara, Nova and Lawnchair jointly ran a user survey to present to Google / the Android team about exactly these issues, and Niagara added a hub page listing issues affecting third-party launchers. Their stated position (Peter Huber): "Our main focus or our competition is every first-party launcher" and "We, as the launcher community, need to ensure third-party launchers need to be on par with first-party launchers."
  - https://www.androidpolice.com/third-party-android-launcher-developers-join-forces-voice-frustrations-to-google/
  - https://www.androidpolice.com/niagara-launcher-creator-peter-huber-interview/

**Marlow implication:** Marlow cannot *be* the Recents provider without root/QuickSwitch, and must not pretend otherwise. What it can do: never fight the system gesture (no custom swipe-up-from-bottom handling that competes with Home/Recents), make its own drawer gesture unmistakable (swipe up *on the workspace*, with a visible affordance), and keep app-open/close animations cheap and consistent so the handoff to the system Recents animation does not judder. The stock feeling here is largely *not breaking* what the system owns.

### 1.12 Long-press menus, notification dots / badges

- **Long-press an app icon** opens a popup menu: at the top, the app's active notifications (notification *previews* — condensed title/sender/message, no action buttons); below them, the app's **app shortcuts** (static + dynamic — e.g. Chrome: New tab, New Incognito tab; Keep: New text note; Photos: View screenshots; Play Store: My apps). Long-pressing a *shortcut* in that menu lets you drag it to the home screen as a one-tap icon. At the bottom of the menu: App info (i), and — where the app has one — a Widgets entry to add its widget directly.
  - https://developer.android.com/develop/ui/views/notifications/badges?authuser=8
  - https://9to5google.com/2024/12/25/google-pixel-set-up/
  - https://www.beebom.com/android-oreo-tricks/amp/
- **Notification dots (badges):** since Android 8.0 (API 26), a dot appears on the launcher icon when the app has an active notification. The dot signals **presence, not quantity** — it never shows a number on stock Pixel. Dismiss the notification in the shade and the dot disappears immediately. Long-pressing reveals the notifications in the popup above. Badges can be disabled per notification channel (`setShowBadge(false)`) and globally/per-app in Settings.
  - https://developer.android.com/develop/ui/views/notifications/badges?authuser=8
  - https://techetips.com/how-does-the-notification-dot-work-android-8-1/
  - https://www.androidcentral.com/android-o-notification-badges
- **Counts are a third-party invention:** Nova Prime's headline badge feature is a *count* (unread number, dynamic badges with profile pictures for calls) — stock Pixel never does this. A count badge on a Pixel-clone reads as Nova, not stock.
  - https://www.androidauthority.com/nova-launcher-885296/?amp
  - https://drfone.wondershare.com/android-transfer/nova-launcher-apk.html
- **Long-press empty space** opens the launcher's own menu: Wallpaper & style, Widgets, Home settings (in some versions a speech-bubble dialog; Android 12 gave its options individual colours and separators; Android 15 added an entry animation).
  - https://betawiki.net/index.php?title=Launcher_(Android)&direction=next&oldid=448918
  - https://support.google.com/pixelphone/answer/14852556
- **Long-press At a Glance** (to its immediate right) gives Dismiss / Settings / "About this content" — the last explains how At a Glance sources its content and lets you change the weather location.
  - https://www.phonearena.com/news/new-pixel-lock-screen-look-includes-changes-for-at-a-glance-placement_id169745
- **Pause app:** from the long-press menu / Recents, Digital Wellbeing "Pause app" greys the icon. (Tested in Android Q betas; shipped behaviour.)
  - https://www.xda-developers.com/tag/google-pixel-launcher/

### 1.13 Theming — Material You, themed icons

- **Material You (Android 12, Material 3):** the system extracts a palette from the wallpaper and applies it across system UI, Quick Settings, lock screen, Gboard, Discover, widgets and (where supported) icons. On Pixel, Wallpaper & style offers wallpaper, colours (wallpaper-derived palettes + "Basic colours"), Themed icons toggle, dark mode, and app grid. That is the whole theming surface — deliberately small.
  - https://www.androidpolice.com/android-12-review/
  - https://mobilesyrup.com/2021/07/23/android-12-ditches-theme-options-shape-icon-font-material-you/
- **What Material You replaced:** Android 10's Styles & Wallpapers app let users pick font, icon shape, status-bar icons and accent colour manually. Android 12 Beta 3 removed font, icon shape and status-bar icon options; Google's Issue Tracker response marked it "Won't Fix (Intended Behaviour)": "The custom style features (font, icon shape, icon pack, and accent color) in R are being replaced by the new dynamic theming feature that we are introducing in S. We see the new dynamic theming feature as more modern and intelligent."
  - https://mobilesyrup.com/2021/07/23/android-12-ditches-theme-options-shape-icon-font-material-you/
  - https://phandroid.com/2021/07/23/google-removes-font-and-icon-customization-in-android-12-thanks-to-material-you/
  - https://nerdschalk.com/cant-find-icon-shapes-on-android-12-heres-why-and-the-alternatives/
- **Themed icons, beta tag:** the toggle carried a "Beta" tag for years because it only themed apps whose developers supplied a monochrome layer — at Android 12 launch, not even all Google apps. The mix was the complaint, not the idea.
  - https://www.androidpolice.com/android-12-review/
- **Android 16 QPR2 (December 2025) reverses two removals:** icon-shape customisation returns (Wallpaper & style → Icons: circle, square, four-sided cookie, seven-sided cookie, arch) and **forced icon theming** arrives — every icon adapts to the Material colour and shape even if the developer did not implement it. The shapes apply to home, drawer and folders; Recents and Settings keep the default circle.
  - https://www.androidauthority.com/android-16-best-new-trick-icon-shape-themes-3622140/
  - http://businessghana.com/site/news/Technology/324691/Android-16-might-soon-bring-back-a-forgotten-customization-feature
- **Material 3 Expressive (Android 16 QPR1, September 2025):** the biggest design overhaul since Material You — springy/physics-based animations (spatial springs for movement, effects springs for colour/opacity), background blur in the shade / Quick Settings / app drawer / Recents / keyguard, "emphasized" typography, a 35-shape library with shape-morph animation, richer dynamic-colour palettes. Google's own description: "Expressive interfaces have an emotional impact, fostering connection by evoking a feeling or mood through visual design and interaction." Rolled out to Pixel 6 and newer + Pixel Tablet.
  - https://blog.google/products-and-platforms/platforms/android/material-3-expressive-android-wearos-launch/
  - https://9to5google.com/2025/05/13/android-16-material-3-expressive-redesign/
  - https://www.androidauthority.com/android-16-qpr1-features-3594123/
  - https://www.androidauthority.com/google-material-3-expressive-features-changes-availability-supported-devices-3556392/

**Marlow implication:** Marlow's theming should feel *automatic* first (wallpaper → palette, everywhere, without asking) and *manual* second. A settings page with twenty colour pickers before the wallpaper has even been applied is the opposite of stock. And any "expressive" motion Marlow adds should be spring physics on dismiss/return, not a new animation vocabulary — the system already owns the vocabulary.

---

## 2. Launcher-by-launcher notes

### 2.1 Nova Launcher — what it does best that Pixel doesn't

**Status warning (matters for Marlow's borrowing):** Nova is effectively unmaintained as a product. Acquired by analytics firm Branch in mid-2022; Branch laid off 100+ staff in 2024 leaving founder Kevin Barry as sole developer; Barry left Branch in September 2025 after being asked to stop work on the launcher and on his plan to open-source it; the stable release had not been updated since May 2024 (Nova 8, with a surprise update in October 2025). A 2026 retrospective reports Nova has since been acquired by Instabridge and is receiving updates again — treat current status as "in flux, verify before citing as a live reference". The *design lessons* survive the corporate history; the *codebase* is not a model to copy.

- https://www.androidauthority.com/nova-launcher-885296/?amp
- https://www.eyerys.com/articles/news/nexus-galaxy-nova-launcher-now-a-legacy-android-history
- https://mmkthecoolest.gitlab.io/blog/2026-07-28-the-questionable-future-of-nova-launcher/

**What Nova does best:**

- **Everything resizes.** Grid up to 12×12, icon size slider, label size/colour, dock icon count and pages, folder grid, drawer grid independent of home grid. "Every icon, folder, dock, and drawer element resizes, animates, and gesture-binds."
  - https://unstore.io/discover/best-pixel-launcher-alternatives/
  - https://www.androidpolice.com/not-fan-of-pixel-launcher/
- **Gestures as a first-class layer (Prime):** swipe up/down, two-finger swipe, double-tap, pinch — each mappable to any app, shortcut or launcher action; icon swipe actions (tap vs swipe on the same icon do different things). Pixel has swipe-down-for-notifications and swipe-up-for-drawer and nothing else.
  - https://www.androidauthority.com/nova-launcher-885296/?amp
  - https://techwiser.com/nova-launcher-vs-pixel-launcher-whats-the-difference/
- **Drawer groups / folders in the drawer, tabs, vertical or horizontal drawer, card background on/off.** Nova 8 added "Cards" in the drawer — media-player controls, Maps data, weather under the suggested-apps row.
  - https://www.androidpolice.com/nova-launcher-8-stable-update-play-store/
  - https://techwiser.com/nova-launcher-vs-pixel-launcher-whats-the-difference/
- **Sesame / Branch search ("microresults"):** searching a person's name offers to message them in the app of your choice, right from search; Nova Search forwards queries to DuckDuckGo/Google and surfaces app shortcuts from search. Branch's version is an offline library, opt-in and off by default in the 8.0.2 beta.
  - https://www.androidpolice.com/nova-launcher-8-stable-update-play-store/
  - https://www.androidpolice.com/nova-launcher-802-beta-branch-search/
- **Icon packs, properly:** third-party packs render at true native size; Nova 8 rebased on Launcher3 Android 12L, added Material You settings/icon theming and a fully redesigned settings page, Align Bottom immersive folders, new desktop swipe gestures.
  - https://www.androidpolice.com/nova-launcher-8-beta-material-you-redesigned-settings-menu/
  - https://unstore.io/discover/best-pixel-launcher-alternatives/
- **Backup/restore of the whole layout** — the feature Pixel has never had. Resetting a Pixel rebuilds the home screen from scratch; Nova restores it.
  - https://www.androidpolice.com/not-fan-of-pixel-launcher/
  - https://unstore.io/discover/best-pixel-launcher-alternatives/

**What feels cheap / where Nova loses the stock feeling:**

- **Density without discipline.** A 12×12 grid with 115% icons, overlapping widgets and five dock pages can be built — and usually looks it. Nova's freedom is its polish risk: nothing in Nova stops a user (or a default) looking cluttered.
- **Paywalled basics.** Gestures, unread-count badges, hide-apps and drawer groups are Prime ($4.99 one-time). The free tier is a demo of the real launcher, and users feel it.
  - https://www.androidauthority.com/nova-launcher-885296/?amp
- **The "Nova look" is a tell:** oversized icons (the classic Pixel-look recipe is icon size 115%, large padding, persistent search off, At a Glance 5×1 widget added manually) — a Nova home screen *imitating* Pixel is recognisably not Pixel, because the search bar, the widget and the drawer all sit a few dp off from Google's.
  - https://xdaforums.com/t/guide-pie-vertical-app-switcher-pixel-launcher-looknfeel.3830714/

**For Marlow, steal:** the *idea* of a gesture layer and a backup/restore, and the discipline that drawer grid and home grid are independent settings. Do not steal: unlimited grid density as a default, or paywalling gestures.

### 2.2 Niagara Launcher — what it does best that Pixel doesn't

**Origin (developer's own account, via interview):** Peter Huber, a computer-science postgraduate, started with a flashlight app, disliked app drawers, and built Niagara as a hobby project in grad school ("I'm not a fan of app drawers"). The team was ~7 people, mostly working students; by the December 2024 winter update the developers were graduating and devoting more time to it. Niagara launched in 2016 after two years of testing and four years of development (per Huber's Medium series). Its competition, in Huber's words, is "every first-party launcher, every pre-installed launcher" — not other custom launchers.

- https://www.androidpolice.com/niagara-launcher-creator-peter-huber-interview/
- https://medium.com/@peter-huber
- https://www.androidpolice.com/niagara-launcher-winter-update/

**What Niagara does best:**

- **It throws out the grid.** Home is a short text-first vertical list of favourites (no more than eight, in the Android Authority reviewer's setup), with a clock/date/weather header. Everything else is one swipe away via the **Wave Alphabet** — a letter strip along the screen edge that curves with the thumb, with haptic feedback for position. On a tall phone, nothing important is in the top corners.
  - https://www.appireview.com/bitpit.launcher
  - https://www.androidauthority.com/ditched-pixel-ui-for-niagara-launcher-3514424
- **Inline notifications:** notifications for each home-screen app appear *directly under that app* in the list — read and reply without opening the shade. Reviewers single this out more than any other Niagara feature.
  - https://www.appireview.com/bitpit.launcher
  - https://www.androidauthority.com/ditched-pixel-ui-for-niagara-launcher-3514424
- **Pop-ups replace folders:** swipe right on a favourite and it expands to show companion apps/shortcuts tucked underneath (camera → gallery + editor). No folder icons, no folder screens.
  - https://www.appireview.com/bitpit.launcher
- **Widget Stacks:** up to four widgets share one slot, swiped sideways. A music player, to-do list and clock coexist without taking over the page.
  - https://www.appireview.com/bitpit.launcher
- **Digital wellbeing as a design principle, not a settings page:** Usage Breaker (gentle reminder after a set interval in a distracting app, rather than Android's hard app-timer with its one-tap extend loophole), Notification Summary (bundles less important notifications), a monochrome option, and a deliberately calm list that "pulls at your attention less than a wall of colorful badges". Huber's Medium series includes "A Healthy Home Screen — Introducing the Digital Wellbeing Initiative and Notification Summary". Huber on Usage Breaker: "our goal is to gather feedback and assess their effectiveness. If they're well-received but demand more precise control, they could potentially outgrow the scope of a launcher."
  - https://www.androidpolice.com/niagara-launcher-winter-update/
  - https://medium.com/@peter-huber
  - https://www.appireview.com/bitpit.launcher
- **First run is one decision:** pick your favourite apps; they appear as a neat vertical list. No grid to fill, no empty pages.
  - https://www.appcracy.com/app/niagara-launcher-home-screen.html
- **Material You without the mess:** Niagara applies wallpaper-derived colour, but deliberately *did not* support native stylised (themed) icons, because "in their current state, they create a very inconsistent look, since they are only available for Google apps" — a design decision, stated in their own changelog, to refuse a half-finished system feature rather than ship the mix.
  - https://4pda.to/forum/index.php?showtopic=916981&st=640
- **Scale:** 4.8 stars / ~248,000 ratings, 10M+ downloads (AppiReview); 4.7 stars / ~160,000–694,000 ratings depending on source/date. Pro is $10/year or $30 one-time (prices have risen since; verify current).
  - https://www.appireview.com/bitpit.launcher
  - https://www.androidauthority.com/ditched-pixel-ui-for-niagara-launcher-3514424

**What feels cheap / where Niagara loses:**

- **It doesn't bend.** "Niagara has its own way of organizing your phone, it doesn't bend much." If a user's routine depends on multiple widgets, folders or an arranged grid, Niagara is the wrong tool — and it knows it.
  - https://www.appireview.com/bitpit.launcher
  - https://reviewed.app/app/niagara-launcher-home-screen/
- **No landscape support, limited widget support, limited third-party integration** (per review aggregations).
  - https://www.appcracy.com/app/niagara-launcher-home-screen.html
- **The calm is a trade:** a list gives less visual context than a page where related icons sit together; discovery of rarely-used apps is slower until the alphabet becomes muscle memory.

**For Marlow, steal:** inline notifications under the app (the single best notification idea any third-party launcher has shipped), widget stacks sharing one slot, the "one decision, then a finished screen" first run, and the courage to *refuse* a half-finished system feature (themed icons) rather than ship the mixed result. Do not steal: the list itself — Marlow is a grid launcher by decision.

### 2.3 Lawnchair — what it does best that Pixel doesn't

**What it is:** a free, open-source launcher built directly on AOSP Launcher3 — the same codebase Pixel Launcher is built on — so it has "the exact architectural DNA" of Pixel rather than an imitation layered over foreign software. Apache 2.0, ~14,000 GitHub stars, no ads, no trackers, no Pro tier. No stable release since 2018 by its own account; current line is betas (14 Beta 3 Aug 2024; 15 Beta 1 Jul 2025, Beta 2 Dec 2025, Beta 2.1 Feb 2026, Beta 3 Apr 2026) with nightly builds from a 16-dev branch.

- https://github.com/LawnchairLauncher/lawnchair/releases
- https://www.axolauncher.com/blog/lawnchair-launcher
- https://www.makeuseof.com/why-i-ditched-samsungs-home-screen-for-lawnchair-launcher/
- https://www.androidpolice.com/i-tested-over-20-launchers-but-this-one-made-my-android-phone-feel-like-true-pixel/

**What Lawnchair does best:**

- **Pixel look, Pixel's own knobs unlocked.** At a Glance strip at top, search bar in the dock, Material You colours, swipe-up drawer — *plus* remove At a Glance entirely, hide the search bar, swap icon packs, resize the grid, custom fonts (including uploading OTF/TTF), custom accent colour, dock background, more dock rows not locked to the home row count, double-tap-to-sleep, notification-badge customisation, status-bar hiding.
  - https://www.makeuseof.com/why-i-ditched-samsungs-home-screen-for-lawnchair-launcher/
  - https://www.androidpolice.com/i-tested-over-20-launchers-but-this-one-made-my-android-phone-feel-like-true-pixel/
- **Smartspacer integration (the At a Glance fix).** Lawnchair 14 added support for Kieron Quinn's Smartspacer SDK — a replacement At a Glance with plugins (Google Keep, Finance, Wallet loyalty cards, Health Connect, Uber, Amazon, Yahoo Sports), horizontal scrolling between complications, animations, and separate lock-screen configuration. The Lawnchair team's own blog: Smartspacer "provides a lot more features than Lawnchair's own implementation due to its dynamic requirements system and its plugins". Lawnchair's own At a Glance clone shows weather, time, battery and now-playing.
  - https://lawnchair.app/blog/lawnchair-14/
  - https://www.makeuseof.com/why-i-ditched-samsungs-home-screen-for-lawnchair-launcher/
  - https://github.com/LawnchairLauncher/lawnchair/releases
- **Global search that is actually global:** apps, system settings, contacts, locally stored files, web via Startpage (no tracking) — plus a choice of search providers (Startpage default in 14; later IronFox replaced Mull, Kagi fixed).
  - https://www.makeuseof.com/why-i-ditched-samsungs-home-screen-for-lawnchair-launcher/
  - https://github.com/LawnchairLauncher/lawnchair/releases/tag/v15.0.0-beta2
- **Drawer folders (Lawnchair 15 Beta 1, the most-requested feature):** create and reorder custom folders *in the app drawer* — the thing Pixel refuses to do. Plus infinite home-screen scrolling, reordering inside drawer folders, "clear all items from home screen", and Nova-backup import (Beta 3: home/dock grid, widgets, folders, icon pack).
  - https://github.com/LawnchairLauncher/lawnchair/releases
  - https://www.axolauncher.com/blog/lawnchair-launcher
- **QuickSwitch / Recents:** Lawnchair 14 supports QuickSwitch on Android 10–14, giving the smooth app open/close and Recents animations a third-party launcher normally cannot have — without root for the animations themselves, with root via the QuickSwitch Magisk module to become the Recents provider. Lawnchair 15 Beta 1 works with QuickSwitch Android 10–15 (initial release); Beta 2 only to 15 QPR0. A toggle to disable icon-closing animations was added in 15 Beta 2.1 for OEMs (e.g. One UI) where they glitch.
  - https://lawnchair.app/blog/lawnchair-14/
  - https://github.com/LawnchairLauncher/lawnchair/releases/tag/v15.0.0-beta2.1
- **Performance as a feature:** reviewers repeatedly report Lawnchair feeling lighter/faster than One UI on the same phone, with smoother page swipes and drawer pulls — because it strips manufacturer background services and transition layers.
  - https://www.makeuseof.com/why-i-ditched-samsungs-home-screen-for-lawnchair-launcher/
  - https://www.androidpolice.com/i-tested-over-20-launchers-but-this-one-made-my-android-phone-feel-like-true-pixel/

**What feels cheap / where Lawnchair loses:**

- **Perpetual beta.** Nothing marked stable since 2018; the Play listing says a stable version will come once it matches every feature of Lawnchair Legacy. Users must choose between three separate release channels (Play / GitHub / Nightly) with different package names and no upgrade path between them.
  - https://www.axolauncher.com/blog/lawnchair-launcher
- **Gesture friction remains.** Even Lawnchair's fans report the swipe-up-to-home gesture needing two or three attempts, and choppy swipe-up on some phones — "isn't really Lawnchair's fault", but the user feels it as Lawnchair's fault.
  - https://www.howtogeek.com/gestures-dont-work-well-with-android-launchers-and-its-googles-fault/
  - https://techverge.io/download-lawnchair/
- **The settings surface is large.** Fonts, grids, drawer, dock, search, gestures, badges, Smartspacer — the freedom is the point, but a first-time user faces a settings app, not a home screen.

**For Marlow, steal:** the *architecture lesson* (build on Launcher3 behaviour, not on a drawing of Pixel), Smartspacer-style pluggable At a Glance complications, drawer folders, and the "auto-show keyboard on drawer open" option (Lawnchair's version of "Swipe up to start search"). Do not steal: perpetual-beta signalling, or exposing every knob on first run.

### 2.4 Smart Launcher 6 — what it does best that Pixel doesn't

**Origin (founder's own account, via interview):** Vincenzo Colucci, a computer-science student in Bologna, was not an app developer when he started; he built the UI "simply based on what I liked about others I had used" — Linux desktop environments, and Sony's PSP ("very futuristic"). First version had no widgets at all, just icons; a side panel for widgets came later. Originally **Flower Launcher** — apps arranged radially "like the petals of a flower", because "it is harder to arrange icons in a grid if you don't have an exact match for the number of columns" — the flower layout is still available in settings and is cited as an influence on Nothing OS. Designer Giovanni Piemontese and server/dev Emilio Vitulano joined; for five to six years it was just the three of them. Smart Launcher 3 (last updated 2015/16) and Pro as separate apps caused so much confusion that in-app purchase upgrade was introduced. Now Smart Launcher SRL, Manfredonia, Italy; 50M+ downloads, 4.5–4.7 stars.

- https://www.androidpolice.com/smart-launcher-vincenzo-colucci-interview/
- https://play.google.com/store/apps/details?id=ginlemon.flowerfree&hl=en
- https://blog.talktech.info/post/smart-launcher-android-launcher-review

**What Smart Launcher does best:**

- **Automatic categorisation.** On install, apps are parsed (package metadata against a classified cloud database) and sorted into drawer tabs — Communication, Internet, Games, Media, Utilities, Settings (six in the AppiReview summary; Communication/Web/Games/Productivity in the Android Authority account). New installs are categorised instantly. The user never maintains folders. Miscategorisation is correctable by moving apps or creating custom categories.
  - https://www.appireview.com/ginlemon.flowerfree
  - https://www.androidauthority.com/my-favorite-android-launcher-3581681/
  - https://free-apps-android.com/smart-launcher-6/
- **Smart search + suggestions that learn.** Swipe down from the centre surfaces a search bar plus apps "it thinks I might like"; the reviewer reports suggestions were "chaotic at first, but after a few days… became very accurate" — Pocket Casts suggested after a morning bike ride. The search bar searches web, files and device from one place, and does quick calculations and contact queries.
  - https://www.androidauthority.com/my-favorite-android-launcher-3581681/
  - https://www.appireview.com/ginlemon.flowerfree
- **Pop-up widgets.** Assign a widget to an app icon; double-tap the icon to pop the widget open as a floating overlay, instead of the widget permanently occupying home-screen space. The Android Authority reviewer — self-described as not a widget person ("they look bad, clutter the homescreen, and have no consistency") — calls this the clever solution.
  - https://www.androidauthority.com/my-favorite-android-launcher-3581681/
  - https://www.appireview.com/ginlemon.flowerfree
- **Ambient Theme (not Material You).** Analyses luminance, saturation and dominant tones of the wallpaper and generates a palette across UI surfaces, folder backgrounds, search bars and widgets — with local contrast control, rather than relying solely on the system's Material You extraction. Plus a frosted-glass look the reviewer notes predates Apple's "liquid glass" talk, thousands of Google Fonts, icon shapes/sizes, blur/transparency controls, and Icon Pack Studio (design your own icon packs).
  - https://www.appireview.com/ginlemon.flowerfree
  - https://www.androidauthority.com/my-favorite-android-launcher-3581681/
  - https://play.google.com/store/apps/details?id=ginlemon.flowerfree&hl=en
- **One-handed by construction.** Primary interactive elements sit in the lower third of the display; modular pages (Home / Apps / Widgets / News) keep each screen to one job.
  - https://www.appireview.com/ginlemon.flowerfree
  - https://play.google.com/store/apps/details?id=ginlemon.flowerfree&hl=en
- **Practical extras done quietly:** encrypted hidden-apps vault (biometric/PIN), nightly backups, adaptive icons, Nova-backup import (6.6 line), Digital Wellbeing paused apps greyed out, archived apps marked with a cloud icon.
  - https://www.appireview.com/ginlemon.flowerfree
  - https://www.androidhire.com/best-android-launcher

**What feels cheap / where Smart Launcher loses:**

- **Best features are Pro.** Subscription (~$21 lifetime quoted in 2026 roundups; $2.49/mo in older listings) gates much of the above; the flower grid itself divides opinion.
  - https://www.androidauthority.com/my-favorite-android-launcher-3581681/
  - https://www.androidhire.com/best-android-launcher
- **Auto-categories guess wrong** — the system's core promise is also its recurring complaint; every wrong guess costs trust in the automation.
  - https://www.androidhire.com/best-android-launcher
  - https://weuseapps.com/smart-launcher-6-home-screen

**For Marlow, steal:** auto-categorisation as a *drawer view* (not a replacement for alphabetical — a tab alongside it), pop-up widgets on double-tap, and suggestions that are allowed to be wrong at first and get better (with the learning visible). Do not steal: hiding the good version behind Pro, or auto-categories with no manual override.

### 2.5 Microsoft Launcher — what it does best that Pixel doesn't

**Origin:** began as **Arrow Launcher**, a Microsoft Garage (internal incubator) experiment in 2015, built by the team behind Next Lock Screen after they found lock-screen capabilities they wanted were not possible there. The early design decision, per Microsoft's own Garage history, was to "forgo any form of customization" and lean on contextual computing. Rebranded as Microsoft Launcher in October 2017 as part of the Windows cross-device strategy (Continue on PC, Microsoft account sync); 15M installs within months. Version 5.0 (2018) added Timeline and the three-tab Feed (News / Glance / Timeline); 5.1 added To-Do/Sticky Notes sync and screen-time tracking; version 6 (2020) was a large rebuild — system-wide dark mode, landscape support, Cortana removed. Most phone-to-PC functions have since moved into Phone Link / Link to Windows.

- https://www.microsoft.com/en-us/garage/wall-of-fame/microsoft-launcher/
- https://www.androidpolice.com/i-didnt-expect-microsoft-to-nail-android-launcher/
- https://vectree.io/pdf/c/microsoft-launcher
- https://www.androidheadlines.com/2018/11/microsoft-launcher-5-0-available.html

**What Microsoft Launcher does best:**

- **Your Feed / Glance as a dashboard, not a news feed.** Swipe right from home to a page of *interactive cards for your own information* — Outlook calendar agenda, To Do lists (check items off in the feed), Sticky Notes synced with Windows, recent OneDrive documents (resume without opening the app), screen time, recent activity, frequently used apps. Every card is optional, reorderable, removable — "strip the feed down to nothing if you want". The current version dropped the News tab entirely and is Glance-only; reviewers call this the single biggest reason it "clicked".
  - https://www.androidpolice.com/i-didnt-expect-microsoft-to-nail-android-launcher/
  - https://www.appireview.com/com.microsoft.launcher
- **The expandable dock.** Swipe up on the dock and it expands into a tray with up to two additional rows of shortcuts *plus* quick toggles (Wi-Fi, Bluetooth, flashlight). It is the only launcher in this set that treats the dock as a drawer.
  - https://www.appireview.com/com.microsoft.launcher
- **Gestures (up to 10 patterns):** swipe up on home button → Gemini, pinch out → Recents, two-finger swipe down → Quick Settings, two-finger swipe up → notifications, and more — each mappable. The Android Authority reviewer calls this the favourite aspect, "easier to use with one hand".
  - https://www.androidauthority.com/microsoft-launcher-pixel-launcher-3541676/
- **Works without signing in.** The launcher "still works beautifully" with no Microsoft account; signing in unlocks the cross-device cards. No ads, no paid tier (4.7 stars, 1.6M+ ratings, 50M+ downloads).
  - https://www.androidpolice.com/i-didnt-expect-microsoft-to-nail-android-launcher/
  - https://www.appireview.com/com.microsoft.launcher
- **Customisation depth without the Nova sprawl:** grid sizes, icon scaling, labels, dock behaviour, feed, search behaviour, Bing daily wallpaper, family / screen-time cards.
  - https://www.androidpolice.com/i-didnt-expect-microsoft-to-nail-android-launcher/

**What feels cheap / where Microsoft Launcher loses:**

- **The feed's best cards need Microsoft services.** Without Outlook/To Do/OneDrive it is "a tidy, well-built launcher… rather than a standout". The News section was consistently the weakest part — "ad-laden garbage that's difficult to read", an "anemic" publisher list.
  - https://www.appireview.com/com.microsoft.launcher
  - https://www.androidauthority.com/microsoft-launcher-pixel-launcher-3541676/
- **Gesture-navigation hitches** on some phones (brief animation hitches reported in reviews).
  - https://www.appireview.com/com.microsoft.launcher

**For Marlow, steal:** the Glance-card feed (your own information, interactive, every card removable — as the model for Marlow's own feed page, not a news feed), and the expandable dock as an *idea* to evaluate (it conflicts with the 5-icon stock dock, so it would be a deliberate Marlow deviation, not a stock behaviour). Do not steal: a feed that is only good inside one company's ecosystem.

---

## 3. Pixel Launcher: history — what changed, what came and went, and why (where sources say so)

### 3.1 From Google Now Launcher to Pixel Launcher (2016)

- **Google Now Launcher** was the universal launcher; in 2016 Google abandoned it in favour of the exclusive **Nexus Launcher**, rebranded for the original Pixel. The cleanest Android experience became locked behind a hardware purchase — which is why Rootless Pixel Launcher (AmirZ, 2018) and later Lawnchair exist at all.
  - https://getapkfiles.co.uk/pixel-launcher-apk/
  - https://techcrunch.com/2016/10/04/googles-pixel-smartphones-get-a-new-look-with-pixel-launcher/
- **Launch feature set (Oct 2016, TechCrunch / APKMirror):** round icons, "G" tab for Assistant, long-press app shortcuts (draggable to home — "similar to iPhone's 3D Touch, but… these shortcuts can actually be dragged to the homescreen"), swipe up on the faded dock to reveal all apps A–Z (no drawer button), swipe down for notifications, swipe down on the fingerprint sensor for notifications, weather in the top-right corner, refreshed wallpaper picker, new circular folder presentation, Google Now/Feed one swipe right.
  - https://techcrunch.com/2016/10/04/googles-pixel-smartphones-get-a-new-look-with-pixel-launcher/
  - https://www.apkmirror.com/apk/google-inc/pixel-launcher/
- **Pixel 2 (2017):** the search bar moved to the bottom, At a Glance took the top. Search on all pages; At a Glance on the home page only. This is the layout still in place in 2026.
  - https://www.thegoandroid.com/download-new-pixel-2-launcher-no-root-get-pixel-2-features/72332/

### 3.2 Google Now → Feed → Discover; Feed widget → At a Glance

- Google Now (cards: weather, sport scores, appointments, parking) became the article-based Google Feed, then **Discover** (2018) — same fundamentals, topic headers, Follow button, evergreen/video cards, controls over how often a topic appears, multi-language.
  - https://www.androidcentral.com/google-feed-now-called-discover-and-available-googles-mobile-site
  - https://www.howtogeek.com/332049/how-to-customize-the-google-feed-and-make-it-actually-useful/
- The old **Feed widget** (descendant of the Google Now widget) was deprecated in Google app 8.39 (Nov 2018): users saw "Looking for weather, calendar, and other updates? Install the new At-a-Glance widget." At a Glance had launched on Pixel 2 and was being brought to more devices as a widget; it always notes day/date plus temperature/condition plus upcoming appointments and flights.
  - https://9to5google.com/2018/11/01/google-app-8-39-kills-feed-at-a-glance-widget/
- **Why it matters:** Google has repeatedly *replaced* a glanceable surface with a feed, then rebuilt a glanceable surface beside it. At a Glance is the third attempt at "what's next, in one line". Marlow should treat glance as a first-class surface with its own rules, not as a widget that happens to be at the top.

### 3.3 Android 9–11: gestures arrive, the drawer button dies, suggestions arrive

- **Android 9 Pie:** Material Design 2 in the launcher; the Nougat/Oreo drawer arrow removed; long-press on empty space now reveals a speech-bubble dialog with launcher options instead of a separate popup. **Recents becomes part of the launcher** (Quickstep) with a horizontal card switcher (see §1.11).
  - https://betawiki.net/index.php?title=Launcher_(Android)&direction=next&oldid=448918
  - https://www.androidpolice.com/2018/05/14/android-ps-new-overview-app-switcher-part-launcher/
- **Android 10:** gesture navigation; the swipe-down-anywhere-for-notifications gesture was tested in an unreleased Q beta and removed before release; Digital Wellbeing "pause app from the home screen" tested. Third-party launchers had gesture navigation disabled in betas 5–6 (see §1.11).
  - https://www.xda-developers.com/tag/google-pixel-launcher/
  - https://android.gadgethacks.com/how-to/android-10-gesture-controls-grayed-out-heres-fix-0200786/
- **Android 11:** swipe up quickly from *any* home page goes to the first home page (fixing the loss of the home-button "jump to first page"); swipe-up-and-hold is Recents. Teardowns of the Android 11 DP1 launcher hinted at: new actions in Recents Overview, hiding individual app suggestions, auto-folder naming, and suggested apps on the home-screen dock (the last shipped in some form as dock suggestions).
  - https://www.androidpolice.com/2020/07/09/android-11s-launcher-lets-you-swipe-up-on-secondary-homescreens-to-go-back-to-the-main-one/
  - https://www.xda-developers.com/tag/google-pixel-launcher/

### 3.4 Android 12–13: Material You, and the great removal

- **Android 12:** Material You; home screen gets a slightly bigger search bar and a new folder-preview design; themed icons arrive with a Beta tag and only for (some) Google apps; **font, icon-shape, icon-pack and accent-colour options removed** (see §1.13 for Google's stated reason — dynamic theming as "more modern and intelligent", marked Won't Fix / Intended Behaviour).
  - https://www.androidpolice.com/android-12-review/
  - https://mobilesyrup.com/2021/07/23/android-12-ditches-theme-options-shape-icon-font-material-you/
- **Android 12L / 13:** Pixel Launcher gains support for two independent home-screen layouts at once (Android 13 DP1 — foldable preparation); the search-apps bar turns light blue (13); the launcher search bar gets much faster and broader (Android 12 Beta 4: apps, shortcuts, widgets, settings, contacts, conversations, tips).
  - https://www.xda-developers.com/tag/google-pixel-launcher/
  - https://betawiki.net/index.php?title=Launcher_(Android)&direction=next&oldid=448918

### 3.5 Android 14–15: refinement, not reinvention

- **Android 14:** search bar and speech-bubble dialog rounded again.
  - https://betawiki.net/index.php?title=Launcher_(Android)&direction=next&oldid=448918
- **Android 15 (Beta 1 teardown, Rahman / Android Authority; confirmed elements in the Oct 2024 Feature Drop):** faster, springier swipe-up-to-close animation; categorised widget recommendations (Essentials / Social / Entertainment + five more in code); "+ Add" button in the widget picker; a small pill-shaped affordance next to the app name in Recents signalling App info / split-screen / pause. The widget-picker categories were the clearest confirmed change; the animation and Recents pill were flag-gated and their stable status was less certain at the time.
  - https://www.androidauthority.com/android-15-pixel-launcher-changes-3434365/
  - https://www.cloudspress.com/android-15s-pixel-launcher-changes-app-animations-widget-suggestions-and-recents/
  - https://charlesesten.s3.uk.io.cloud.ovh.net/news/android-15-to-bring-new-animations-and-ui-tweaks-to-the-pixel-launcher.html
- **Android 15 DP2:** "Swipe up to start search" moves from Home settings → Search settings to a new "Apps list settings" submenu; a possible "Show long app names" (two-line labels) spotted in code.
  - https://www.androidpolice.com/android-15-dp2-switches-up-pixel-launcher-settings/

### 3.6 Android 16–17: Expressive, and the return of the removed options

- **Android 16 QPR1 (Sept 2025):** Material 3 Expressive (see §1.13) — blur in drawer/Recents/shade, springy dismiss animations, new typography. Desktop Mode preview (Pixel 8+, USB-C display out) and Live Updates also arrive, but are not launcher-home behaviour.
  - https://www.androidauthority.com/android-16-qpr1-features-3594123/
- **Android 16 Beta 3–4 (teardowns):** "App shape & layout" menu returns — grid size under Layout, six icon styles (circle default, square, four-sided cookie, seven-sided cookie, arch, complex clover) — shape applies to home/drawer/folders only. The same beta cycle showed a cleaner lock-screen At a Glance split (date/weather separated from the information complication). Both were flag-gated; the icon-shape return was confirmed as shipped in QPR2.
  - http://businessghana.com/site/news/Technology/324691/Android-16-might-soon-bring-back-a-forgotten-customization-feature
  - https://www.tomsguide.com/phones/android-phones/android-16-redesign-revealed-in-massive-leak-how-real-is-it/
  - https://www.androidauthority.com/pixel-lock-screen-android-16-3546942/
- **Android 16 QPR2 / November 2025 Pixel Drop:** forced icon theming + icon shapes ship (see §1.13); home-screen search bar rerouted to the Google app with AI Mode (see §1.5); a segmented search bar (small bar + separate AI Mode button) tested in QPR1 beta 2.
  - https://www.androidauthority.com/android-16-best-new-trick-icon-shape-themes-3622140/
  - https://www.androidheadlines.com/wp-json/wp/v2/posts/1255326
  - https://www.androidauthority.com/android-16-qpr1-beta-2-segmented-search-bar-3567059/
- **Version numbering (APKMirror):** Pixel Launcher versions track the Android release — 13 (Sept 2023), 14 (Sept 2024), 15 (Aug 2026), 16 (Feb 2026), 17 (Oct 2026). The launcher is updated with the OS, not on its own Play Store cadence, which is why a "Pixel Launcher update" is usually an OS update.
  - https://www.apkmirror.com/apk/google-inc/pixel-launcher/

**The pattern for Marlow:** Google's launcher history is a cycle — add a manual customisation, remove it for an automatic system, then (years later) bring back a *better* version of the manual control once the automatic system can enforce consistency (forced theming). The lesson is not "never remove options"; it is "never ship the inconsistent middle state". Themed icons in 2021 shipped the mix; forced theming in 2025 fixed it. Marlow should ship the fixed state first.

---

## 4. Detailed teardowns — feature-by-feature behaviour, defaults, edge cases

### 4.1 Pixel — teardown notes (from Rahman / Android Authority / XDA teardowns)

| Area | Stock behaviour | Default | Edge case / failure mode |
|---|---|---|---|
| Grid | 2×2 / 3×3 / 4×4 / 4×5 / 5×5, via Wallpaper & style → App grid (bottom of the page) | 5×5 recommended by 9to5; factory default varies by device size | On a large phone 5×5 leaves visible dead bands; no denser option exists |
| Dock | 5 icons, no labels, search bar below on every page | 5 (contents not Google-documented — see §1.4) | Dragging a 6th icon in is refused, not wrapped |
| At a Glance | Top of main page only; carousel of 3 complications (date, weather, info); swipe to change | On; all features on | With all features off, still shows date/weather line — never disappears (pre-QPR2 Canary toggle) |
| Search (dock) | Pill, G + mic/Lens; classic = unified launcher sheet; Nov 2025 = Google app full-screen + AI Mode | Unified sheet (pre-Nov 2025) | After Nov 2025, local shortcuts (Clock, Contacts, Tips, Wallet…) missing from dock search; drawer search is the fallback |
| Search (drawer) | Bar at top of All Apps; filters alphabetical grid; surfaces settings/contacts/files | On | "Swipe up to start search" focuses field + keyboard; toggle has moved settings locations across releases |
| Drawer | Vertical grid, alphabetical only, no folders/tabs/custom order | Alphabetical, suggestions row on | Suggestions row toggle location drifted (Search settings → Apps list settings); Private Space can make the order look non-alphabetical |
| Suggestions | Top row of drawer (+ optional home row), from recency/frequency/routines | On in drawer, off on home (in the AA productivity setup) | Pin a suggestion (long-press → pin); block one (long-press → block); location-based suggestions reported in testing Oct 2026 |
| Folders | Drag-on-drag to create; circular 2×2 preview; tap name at bottom to rename; no nesting; home only | — | Dragging last app out dissolves the folder; auto-naming tested in Android 11 DP1 |
| Widgets | Long-press blank → Widgets → drag; resize via white borders; snap to grid; no overlap/stack | — | Widget that does not fit cannot be dropped; Android 15 adds "+ Add" auto-place + categorised recommendations; "tiered widgets" flag may hide most widgets behind "Show all" |
| Long-press app | Notifications preview on top, shortcuts below, drag-a-shortcut-to-home, App info, Widgets entry | — | Notification preview shows title/sender only — no action buttons (unlike the shade) |
| Dots | Dot = presence, never a number; long-press reveals notifications; per-channel + global + per-app off switches | On | Dot requires Notification access for Pixel Launcher; clearing storage resets the home layout (support-thread fix for missing dots) |
| Recents | Quickstep, part of launcher; swipe-up-hold; horizontal cards; swipe up on card to dismiss; pill affordance for App info/split/pause (Android 15) | Gesture nav | Third-party launcher loses Select/Screenshot in Recents; app-open animation is owned by the launcher |
| Discover | Left-most page; on/off in Home settings; off = dead space | On | Third-party launchers need Pixel Bridge / AIDLBridge + system-app install for Discover; without it the feed breaks |
| Themed icons | Wallpaper palette tint via monochrome layer; Beta tag for years | Off | Mixed themed/unthemed grid until forced theming (Android 16 QPR2) |

Sources for the table: §1 citations throughout, plus
- https://www.androidauthority.com/android-15-pixel-launcher-changes-3434365/
- https://www.androidpolice.com/android-15-dp2-switches-up-pixel-launcher-settings/
- https://support.google.com/pixelphone/thread/251979711/notification-dots-on-icons?hl=en
- https://www.androidauthority.com/android-16-qpr1-beta-2-segmented-search-bar-3567059/

### 4.2 Nova — teardown notes

| Area | Behaviour | Default / note |
|---|---|---|
| Grid | Home, drawer, folder, dock grids independent; up to 12×12; sub-grid positioning (place between lines); widget overlap allowed | Pixel-look recipe: 5×5, icon 115%, large padding |
| Dock | Multiple dock pages (swipe), icon count configurable, background on/off, search bar in dock below/above icons, labels off | 5 icons to match Pixel, or more |
| Drawer | Vertical / horizontal / list; card background on/off; groups + tabs + folders in drawer; "remember position" option | Vertical, frequently-used row on |
| Search | Nova Search + Sesame/Branch microresults; search in drawer; limit apps to one row (8.0.18); pull-to-search in drawer | Branch search off by default (8.0.2 beta) |
| Gestures | Pinch, swipe up/down, two-finger swipe, double-tap, icon swipe; per-icon swipe actions | Prime-gated |
| Badges | Dynamic counts (numbers, profile pictures for calls) via TeslaUnread historically; Prime-gated | Count, not dot — the Nova tell |
| Folders | Immersive folders, Align Bottom, custom preview grid beyond 2×2, folder icon from pack, swipe-to-open-first-app | — |
| Backup | Full backup/restore (layout, icons, settings) | — |
| Scroll | Scroll effects (cube, accordion, etc.), animation speed control, including off | — |

- https://www.androidpolice.com/nova-launcher-8-beta-material-you-redesigned-settings-menu/
- https://www.androidpolice.com/nova-launcher-8018-beta
- https://www.androidpolice.com/nova-launcher-802-beta-branch-search/
- https://xdaforums.com/t/guide-pie-vertical-app-switcher-pixel-launcher-looknfeel.3830714/

### 4.3 Niagara — teardown notes

| Area | Behaviour | Default / note |
|---|---|---|
| Home | Vertical text list, max 8 favourites in the reviewer's setup; header clock/date/weather (customisable); no grid, no pages | One page, always |
| Alphabet | Wave Alphabet on either vertical edge; thumb slide + haptic; tap a letter to jump | Hidden until touched (reviewer's setup) or always visible |
| Notifications | Inline under the app; read + reply in place; Notification Summary bundles low-priority ones | Requires notification access |
| Pop-ups | Swipe right on a favourite → companion apps/shortcuts expand underneath | Replaces folders |
| Widgets | Widget Stack: up to 4 widgets in one slot, swipe sideways; built-in calendar + weather widgets | — |
| Search | Advanced search: apps, contacts, web, instant calculations | Swipe up |
| Gestures | Double-tap to lock (accessibility service, sole purpose stated on Play listing); custom button (Pro) opens app/toggle | — |
| Wellbeing | Usage Breaker (interval reminder in distracting apps), hide apps, focus-simplify, monochrome | Pro for weather/calendar/Usage Breaker in some versions |
| Backup | Settings backup/restore added in the Dec 2024 winter update (configuration file) | Settings → Backup and Restore |
| Theming | Material You colours on all Android versions; Theme Studio; icon packs (Anycons); custom fonts; clock styles | Refused native themed icons (see §2.2) |
| Edge cases | No landscape; Private Space + app-archiving indicators added in Android 15-era betas; occasional touch misfires reported on some devices (Pixel 6a cited) | — |

- https://www.appireview.com/bitpit.launcher
- https://www.androidpolice.com/niagara-launcher-winter-update/
- https://www.androidpolice.com/niagara-launcher-creator-peter-huber-interview/
- https://www.apkmirror.com/apk/peter-huber/niagara-launcher-%f0%9f%94%b9-fresh-clean/niagara-launcher-%e2%80%a7-home-screen-1-15-18-release/#disqus_thread

### 4.4 Lawnchair — teardown notes

| Area | Behaviour | Default / note |
|---|---|---|
| Base | Launcher3 from the Android version it tracks (14 → Lawnchair 14; 15 → Lawnchair 15); QuickSwitch Android 10–14 (14), 10–15 initial (15 Beta 1), 10–15 QPR0 (15 Beta 2) | Beta, no stable since 2018 |
| Home | Pixel layout + removable At a Glance, hideable search bar, custom grid/rows/cols, icon size/shape picker, custom fonts (OTF/TTF upload), accent colour, dock background, extra dock rows | — |
| At a Glance | Own clone (weather/time/battery/now-playing) or Smartspacer SDK (plugins, horizontal scroll, animations); Smartspacer left-padding fixes in 14 Beta 3 / 15 | Smartspacer needs Shizuku/ADB (12+) or root (10–11) for enhanced mode; one-tap reconnect per reboot for Native Smartspace |
| Search | Global: apps, settings, contacts, files, web (Startpage default in 14; IronFox replaced Mull; Kagi fixed in 15 Beta 2); provider picker; auto-show keyboard on drawer open | — |
| Drawer | Folders in drawer (15 Beta 1, most-requested); reorder inside folders; infinite home scrolling (15 Beta 2); "clear all items from home screen" (15 Beta 2) | — |
| Feed | Google Discover via Lawnfeed + Pixel Bridge / AIDLBridge; Lawnfeed 4.0 fixed feed not showing (15 Beta 2.1) | Companion APK needed |
| Gestures | Configurable (double-tap sleep etc.); icon-closing-animation toggle (15 Beta 2.1, for One UI glitches) | — |
| Backup | Nova-backup import (15 Beta 3) | Own backup also exists |
| Bugs fixed in public changelogs | Widgets failing when config step required (Android 14+, fixed 15 Beta 2.1); web-search timeout offline (fixed); icon shadow not applied (fixed); search widget failing to launch Google app on 14+ (fixed 15 Beta 2) | Changelogs are a good model for what "polish" means in practice: each fix is a named, observable behaviour |

- https://lawnchair.app/blog/lawnchair-14/
- https://github.com/LawnchairLauncher/lawnchair/releases
- https://github.com/LawnchairLauncher/lawnchair/releases/tag/v15.0.0-beta2
- https://github.com/LawnchairLauncher/lawnchair/releases/tag/v15.0.0-beta2.1
- https://raw.githubusercontent.com/KieronQuinn/Smartspacer/main/FAQ.md
- https://www.makeuseof.com/why-i-ditched-samsungs-home-screen-for-lawnchair-launcher/

---

## 5. Design-engineering writing from people who built launchers

This section is deliberately *attributed*: each point is tied to the person/project who said it, in their own venue. No paraphrase is presented as a general "industry" truth.

### 5.1 Vincenzo Colucci, Smart Launcher — "build from what you liked"

- Started as a computer-science student in Bologna who "wasn't an app developer"; built the UI "simply based on what I liked about others I had used" — Linux desktop environments and the PSP. Lesson for Marlow: a coherent launcher can come from *taste applied consistently*, not from a feature checklist. Smart Launcher's first version had no widgets — icons only — and widgets arrived later as a side panel, because the home screen's job was launching, not displaying.
- The flower layout exists because "it is harder to arrange icons in a grid if you don't have an exact match for the number of columns on your grid" — a *layout* insight, not a visual one: a radial arrangement degrades gracefully when the count is not a neat rectangle; a grid does not. Marlow's grid should therefore have a defined behaviour for 1, 2, 3… icons in a row/folder, not just for full rows.
- "I realized I was not good at design, so I involved my friend Giovanni Piemontese. He is still the designer of the project." — the separation of engineering and design as a deliberate hire, early, on a three-person team.
- https://www.androidpolice.com/smart-launcher-vincenzo-colucci-interview/

### 5.2 Peter Huber, Niagara Launcher — "the competition is every first-party launcher"

- "Our main focus or our competition is every first-party launcher, every pre-installed launcher." / "We, as the launcher community, need to ensure third-party launchers need to be on par with first-party launchers." — the bar is not "better than Nova", it is "indistinguishable in polish from the thing that shipped in the box". That is the correct bar for Marlow.
- On gesture/recents breakage: "There are a lot of changes, and we are trying to report as many bugs to Google" and the team added a hub page listing issues affecting third-party apps, plus a user survey to present to Google. The engineering response to a platform limitation was *evidence collection*, not a workaround hack.
- On wellbeing features as a sandbox: "Right now, I view these features as a sort of sandbox — our goal is to gather feedback and assess their effectiveness. If they're well-received but demand more precise control, they could potentially outgrow the scope of a launcher." — ship the small version, measure, then decide whether it belongs in the launcher at all.
- On themed icons (Niagara changelog, Android 12 era): refused native stylised icons because "they create a very inconsistent look, since they are only available for Google apps" — *refusing* a feature is a design decision, and stating the reason publicly is part of the polish.
- Pro pricing is adjusted for purchasing power by country, and a Pro purchase can be linked to a Google Play account *or* an email — pricing as accessibility, not just monetisation.
- https://www.androidpolice.com/niagara-launcher-creator-peter-huber-interview/
- https://www.androidpolice.com/niagara-launcher-winter-update/
- https://4pda.to/forum/index.php?showtopic=916981&st=640

### 5.3 Kevin Barry, Nova Launcher — via the project's public history

- Barry built Nova in 2011 for Android 4.0 Ice Cream Sandwich / Galaxy Nexus, and ran it as a largely single-developer project for over a decade. Nova 8 (beta Jul 2022, stable May 2024) was rebased on Launcher3 Android 12L, redesigned its settings from the ground up for Material You, and added Branch search as an *opt-in, off-by-default* integration — Barry's public promise that "most of the Branch features would be opt-in" was kept in the 8.0.2 beta (the toggle shipped off).
- The end of Nova as an actively developed product is itself a design lesson: Barry left Branch in Sept 2025 after being asked to stop work on the launcher *and* on his months-long plan to open-source it (code cleaned, licences reviewed, legal advice taken). A launcher whose bus factor is one person and whose owner is an analytics company is a fragile dependency — Marlow should not design itself into dependence on any single third-party launcher's continued existence.
- https://www.androidpolice.com/nova-launcher-8-beta-material-you-redesigned-settings-menu/
- https://www.androidpolice.com/nova-launcher-802-beta-branch-search/
- https://www.eyerys.com/articles/news/nexus-galaxy-nova-launcher-now-a-legacy-android-history
- https://www.androidauthority.com/nova-launcher-885296/?amp

### 5.4 The Lawnchair team — building on Launcher3, in public

- Lawnchair 14 blog (Apr 2024, lawnchair.app): "Lawnchair now supports QuickSwitch on Android 10 to 14! … you can have a smoother experience when using Lawnchair without root." / Smartspacer support credited to Kieron Quinn implementing the initial PR — the team publicly credits a contributor for a flagship feature rather than absorbing it silently.
- The same blog lists theming additions as a group (hide dock, custom icon-shape picker, tweaked icon-pack picker, more padding/colour/size customisations, more fonts) — i.e. the *absence* of those controls in Pixel is treated as the feature list.
- Release discipline visible in the GitHub releases: each beta names what it fixes in observable terms (widgets failing on config step, web-search timeout offline, icon shadow, Google Feed not showing, race condition on uninstall-during-customise). The 15 Beta 2 notes also name new contributors individually. That changelog style — observable behaviour + named people — is the standard Marlow's own BUILD_LOG should be held to.
- https://lawnchair.app/blog/lawnchair-14/
- https://github.com/LawnchairLauncher/lawnchair/releases
- https://github.com/LawnchairLauncher/lawnchair/releases/tag/v15.0.0-beta2.1

### 5.5 Microsoft Garage — Arrow Launcher's founding decision

- Microsoft's own Garage history: the Arrow team "decided to forgo any form of customization" in the early design, to explore contextual computing instead — the opposite of Nova's premise, and the reason Arrow felt coherent despite doing less. It graduated from the Garage in Oct 2017 and "has become an important part of the Microsoft 365 offering". The lesson: *restraint is a feature decision*, and it needs to be made explicitly at the start, not discovered after a settings page has grown.
- https://www.microsoft.com/en-us/garage/wall-of-fame/microsoft-launcher/

### 5.6 Google — the platform's own statements

- Issue Tracker, Android 12: "The custom style features (font, icon shape, icon pack, and accent color) in R are being replaced by the new dynamic theming feature that we are introducing in S. We see the new dynamic theming feature as more modern and intelligent. A simple and delightful experience that we hope all users can get to enjoy." — Google's stated rationale for removing manual theming. Whether one agrees, the *reasoning pattern* is useful: replace a manual control with an automatic system only when the automatic system is at least as good for the common case.
  - https://mobilesyrup.com/2021/07/23/android-12-ditches-theme-options-shape-icon-font-material-you/
- Material 3 Expressive launch (Google blog, May 2025): "Expressive interfaces have an emotional impact, fostering connection by evoking a feeling or mood through visual design and interaction" and the motion system "makes interactions and transitions feel more alive, fluid, and natural" via spatial springs (movement) and effects springs (colour/opacity). Google's own framing is emotion + physics, not decoration.
  - https://blog.google/products-and-platforms/platforms/android/material-3-expressive-android-wearos-launch/
  - https://9to5google.com/2025/05/13/android-16-material-3-expressive-redesign/
- Android Developers, notification badges: "notification badges — also known as notification dots — appear on a launcher icon when the associated app has an active notification. Users can touch & hold the app icon to reveal the notifications, along with any app shortcuts." The platform doc treats dot + long-press preview + shortcuts as *one* interaction, not three features.
  - https://developer.android.com/develop/ui/views/notifications/badges?authuser=8

---

## 6. The details that separate a stock-feeling launcher from an amateur one

### 6.1 Above the fold, and the "empty home" problem

- **Above the fold on Pixel is:** At a Glance (top) → first rows of icons/folders → dock → search. There is no empty band, no "add widgets" placeholder, no tutorial card. A home screen that shows a placeholder where content will one day be is an admission that the launcher shipped before it was a home screen.
- **The empty-home problem is structural for third-party launchers:** a Pixel arrives with a finished home; a new launcher arrives with a grid and a wizard. Niagara and Smart Launcher solve it by front-loading *one* decision and delivering a finished screen (§1.10). Marlow's self-healing seed (curated page 0 + dock, clock widget, weather on) is the same solution in grid form — keep it, and test it by *factory-resetting Marlow's data and opening it cold*: the first frame must look like a phone, not a setup flow.
- **What sits above the fold is a decision, not an accident.** Pixel puts the *next thing* (At a Glance) at the top and the *most-used things* (dock) at the bottom — thumb-reach logic: glance at the top (read-only), act at the bottom (tappable). Smart Launcher states the same principle explicitly: "We moved the items you need to interact the most, in the bottom part of the screen where they are easier to reach."
  - https://play.google.com/store/apps/details?id=ginlemon.flowerfree&hl=en

### 6.2 Consistency rules (the amateur tells)

1. **One icon size, one label size, one gutter — everywhere.** Home, drawer, folders, dock, search results and long-press menus must agree. Nova's Pixel-look recipe has to set icon size to 115% in *three separate places* (desktop, drawer, dock) — the fact that it needs three settings is the tell that inconsistency is the default failure mode.
2. **One shape.** Every icon in the same mask, at the same scale. A folder preview at a different scale from the home grid, or a drawer icon larger than the home icon, breaks the illusion instantly.
3. **One label rule.** Pixel trims names longer than ~9 characters (reported); the stock fix being considered is two-line labels. Marlow must pick *one* rule (one line + ellipsis, or two lines) and apply it in home, drawer and folders — not one line in one place and two in another. And the rule must be *decided*, not emergent from text measurement differing per surface.
4. **One animation language.** App-open, folder-open, drawer-open and dismiss must share easing/duration. Material 3 Expressive's contribution is exactly this: spatial springs for movement, effects springs for colour/opacity, everywhere. A folder that fades while apps scale, or a drawer that slides while folders pop, reads as three launchers stitched together.
5. **One empty state.** No widgets → the widget picker is empty, not the home screen. No notifications → no dot, no placeholder, no "you're all caught up" card on the home screen. Pixel's home screen with no notifications is just a home screen.
6. **No developer asides in user-facing strings.** (Already fixed in Marlow's audit-fix build — keep it fixed.) Plan numbers, feature IDs and "beta" labels in Settings are the fastest way to signal "this is a prototype".
7. **Themed icons: all or nothing.** See §1.3 / §2.2 — a mixed grid is the single most visible "third-party" signal on a Pixel-clone home screen. Android 16 QPR2's forced theming is the stock answer; Marlow should match it or deliberately not offer themed icons.

### 6.3 First boot, before the user has done anything

- The launcher must be *complete* on first frame: wallpaper applied, palette derived, At a Glance (or Marlow equivalent) showing real date/weather, dock populated, page 0 populated, search pill present and tappable, drawer opening to a full alphabetical list. Anything that requires a network fetch must have a *designed* loading state (skeleton or cached value), not a blank.
- Permissions must be *deferred and contextual*: notification access when the first dot would appear, contacts when search first needs them — not a checklist before the home screen exists.
- The "Set as default" prompt is the one interruption allowed, and it is Android's prompt, not Marlow's — Marlow's job is to explain *in one line* why, then get out of the way.
- **Test:** cold-install Marlow, clear its data, open it, screenshot the first frame, and compare it side-by-side with a Pixel first-boot screenshot. If a stranger cannot tell which is the "real" home screen at a glance, Marlow has passed the first-boot test. If the Marlow frame has a wizard, a blank grid, or a "let's set up" card, it has not.

### 6.4 Gesture-navigation interplay (what Marlow must not break)

- Do not intercept the bottom-edge swipe-up (Home) or swipe-up-and-hold (Recents). Do not add a custom bottom gesture that competes with them.
- The drawer gesture must be *on the workspace*, visibly distinct: a swipe up starting above the search bar, ideally with a small pull-tab/chevron affordance on first use (Pixel removed its arrow in Pie and relies on learned behaviour — Marlow, as a new launcher, cannot rely on learned behaviour on first use).
- App open/close animations must hand off cleanly to the system Recents animation; judder at the handoff is perceived as *Marlow* being slow, even though the animation is the system's.
- See §1.11 for the platform limits (Quickstep reserved to the stock launcher; QuickSwitch needs root).

---

## 7. CHECKABLE RULES — testable against Marlow

Each rule states the test, the expected result, and the source that justifies it. "Source" is the citation; where a rule is a *judgement derived from* sources rather than a direct quote, it is marked [Derived].

### Home / grid / dock

1. **First-boot home is complete on first frame.** Cold-install, clear data, open Marlow: At a Glance (or equivalent) shows real date + weather, dock has 5 apps, page 0 has apps/folders, search pill is present. No wizard, blank grid or setup card is visible in the first frame. [Derived from §1.10; https://www.appcracy.com/app/niagara-launcher-home-screen.html ; https://free-apps-android.com/smart-launcher-6/]
2. **Grid options are exactly the Pixel set (2×2, 3×3, 4×4, 4×5, 5×5), default 5×5.** Changing grid re-flows icons without overlap or loss. https://9to5google.com/2024/12/25/google-pixel-set-up/
3. **Dock holds exactly 5 icons, labels off, present on every home page.** A 6th icon cannot be added (refused with feedback, not wrapped or silently dropped). https://techwiser.com/nova-launcher-vs-pixel-launcher-whats-the-difference/
4. **Search bar is below the dock on every home page**, pill-shaped, G on the left, mic/Lens on the right. https://www.androidpolice.com/not-fan-of-pixel-launcher/
5. **Icons, labels and gutters are identical on home, drawer, folders and search results** (same dp size, same label size, same spacing — measure screenshots, do not eyeball). [Derived from §6.2; https://github.com/ichintansoni/skills-master/blob/HEAD/skills/android/design/technologies/m3-app-icons/SKILL.md]
6. **Every icon survives the circular mask at 48 px.** Screenshot the home screen, crop one icon at 48×48 px under a circular mask: the mark is fully visible, not clipped, not cramped. [Derived from §1.3; https://codelabs.developers.google.com/design-android-launcher]
7. **No icon or widget can be placed off-grid or overlapping.** Attempting to drop between grid lines snaps to the nearest cell or is refused with feedback. https://www.androidpolice.com/not-fan-of-pixel-launcher/

### At a Glance

8. **At a Glance sits at the top of the main home page only**, with no card background or border — text directly on the wallpaper. https://9to5google.com/2024/12/25/google-pixel-set-up/ ; https://www.androidauthority.com/pixel-lock-screen-android-16-3546942/
9. **Resting state shows day + date and weather (condition + temperature).** Tapping weather opens weather; tapping date opens Calendar. https://9to5google.com/2025/03/25/pixel-weather-location/
10. **An upcoming calendar event replaces the date line** (it does not appear as a second row or a badge). https://en.androidayuda.com/widget-pixel-2-android-p-beta/
11. **At a Glance is a carousel, not a static widget:** swiping it cycles complications (today/tomorrow weather, events, reminders, etc.); the content changes with time/context. https://www.phonearena.com/news/new-pixel-lock-screen-look-includes-changes-for-at-a-glance-placement_id169745
12. **Long-press At a Glance offers Settings (per-feature toggles) and an explanation of where its content comes from.** Turning features off reduces it to day/date — it never shows a blank or an error. https://www.phonearena.com/news/new-pixel-lock-screen-look-includes-changes-for-at-a-glance-placement_id169745

### Search

13. **Tapping the dock pill opens search immediately** (no blank screen, no second tap). The surface it opens is *defined* (web-first or device-first) and is not the same screen as the drawer-top search — or, if it is the same, that is a deliberate, documented decision. [Derived from §1.5; https://www.androidheadlines.com/wp-json/wp/v2/posts/1255326]
14. **Drawer-top search filters the alphabetical grid as you type** and surfaces apps first; a query matching an installed app shows that app in the first row of results. https://tms-outsource.com/blog/posts/android-app-drawer-vs-home-screen/
15. **"Swipe up to start search" (if offered) opens the drawer with the field focused and the keyboard up.** One toggle, in one place, that does not move between releases. https://www.androidpolice.com/android-15-dp2-switches-up-pixel-launcher-settings/

### Drawer

16. **Swipe up from anywhere above the search bar on the home screen opens the drawer.** There is no drawer button. https://support.google.com/pixelphone/answer/14852556
17. **Drawer is a vertical scrolling grid, alphabetical by default, with no other ordering.** Home-screen order is independent and is not re-sorted. https://support.google.com/pixelphone/thread/305487540/how-can-i-alphabetize-apps?hl=en
18. **Suggestions row (if present) is a single clearly-labelled toggle** (on/off), sits at the top of the drawer, and can be pinned/blocked per app by long-press. https://www.androidauthority.com/pixel-launcher-setup-productivity-3522859/
19. **Every installed app appears in the drawer exactly once**, including apps not on any home page; uninstalling an app removes it from drawer and home simultaneously. [Derived from §1.9]
20. **Label rule is single and consistent:** pick one line + ellipsis *or* two lines, apply it in home, drawer and folders, and verify with a 20+-character app name in all three places. [Derived from §1.9; https://www.androidpolice.com/android-15-dp2-switches-up-pixel-launcher-settings/]

### Folders

21. **Folder is created only by dragging one icon onto another** (no "New folder" menu). The folder icon is circular with a 2×2 preview of the first four apps. https://support.google.com/pixelphone/answer/14852556 ; https://www.apkmirror.com/apk/google-inc/pixel-launcher/
22. **Opening a folder dims the home screen and shows the folder as an overlay, not a new page.** Closing returns to the exact same home-screen state. [Derived from §1.8]
23. **Renaming is: open folder → tap the name at the bottom → type → confirm.** The name does not truncate at the folder's width. https://support.google.com/pixelphone/answer/14852556
24. **No nested folders.** Dragging a folder into a folder is refused. https://www.pixelartworkshop.com/guides/how-to-organize-pixel
25. **Dragging the last app out dissolves the folder** (no empty folder remains). [Derived from §1.8]

### Widgets

26. **Add flow is: long-press blank space → Widgets → pick → drag to home (or tap "+ Add" to auto-place).** The widget snaps to grid; resizing shows visible resize handles/borders and snaps to grid multiples. https://support.google.com/pixelphone/answer/14852556 ; https://www.androidauthority.com/android-15-pixel-launcher-changes-3434365/
27. **A widget that does not fit the free space cannot be dropped there** — the refusal is visible *before* release, not after. [Derived from §1.7]
28. **Widget picker groups recommendations by category** (Essentials / Social / Entertainment / Weather / etc.) or, if Marlow does not do recommendations, shows a plain alphabetical per-app list with no fake "recommended" row. https://www.androidauthority.com/android-15-pixel-launcher-changes-3434365/

### Long-press / dots / shortcuts

29. **Long-press an app icon shows, in order: its active notifications (title/sender/message, no action buttons), then its app shortcuts, then App info and a Widgets entry.** https://developer.android.com/develop/ui/views/notifications/badges?authuser=8 ; https://9to5google.com/2024/12/25/google-pixel-set-up/
30. **A shortcut in the long-press menu can be dragged to the home screen** as a one-tap icon. https://9to5google.com/2024/12/25/google-pixel-set-up/
31. **Notification dot = presence, never a number.** An app with an active notification shows a dot; dismissing the notification in the shade removes the dot immediately. No count badge anywhere in the stock surfaces. https://developer.android.com/develop/ui/views/notifications/badges?authuser=8 ; https://techetips.com/how-does-the-notification-dot-work-android-8-1/
32. **Long-press empty space opens Wallpaper & style / Widgets / Home settings** — and nothing else (no debug, plan or developer entries in the user-facing menu). https://support.google.com/pixelphone/answer/14852556

### Theming

33. **Changing the wallpaper re-derives the palette everywhere** (home, drawer, folders, search, settings) without any further user action. https://www.androidpolice.com/not-fan-of-pixel-launcher/
34. **Themed icons are all-or-nothing:** either every icon on home + drawer + folders is themed (forced theming), or themed icons are not offered. A mixed themed/unthemed grid fails this rule. https://www.androidauthority.com/android-16-best-new-trick-icon-shape-themes-3622140/ ; https://www.androidpolice.com/android-12-review/
35. **One icon shape applies to home, drawer and folders together.** Changing shape does not leave Recents/settings on a different shape in a way the user can see as inconsistency (Pixel: Recents keeps the circle — if Marlow differs, it is a documented deviation). https://www.androidauthority.com/android-16-best-new-trick-icon-shape-themes-3622140/

### Gestures / Recents / first run

36. **Swipe up quickly from any home page goes to the first home page; swipe-up-and-hold opens Recents; swipe up on the workspace opens the drawer.** Marlow does not intercept the bottom-edge Home/Recents gestures. https://www.androidpolice.com/2020/07/09/android-11s-launcher-lets-you-swipe-up-on-secondary-homescreens-to-go-back-to-the-main-one/
37. **"Set as default" is asked once, early, in plain language**, via Android's own prompt (Settings → Apps → Default apps → Home app as the fallback path). Declining it leaves a complete, usable home screen. https://www.axolauncher.com/blog/how-to-change-default-launcher-android
38. **No permission is requested before the home screen exists.** Notification access, contacts etc. are requested contextually, at first use of the feature that needs them. [Derived from §1.10]
39. **App-open, folder-open, drawer-open and dismiss share one easing/duration** (spring physics per Material 3 Expressive, not four different animations). Verify by screen-recording all four and comparing frame curves. https://blog.google/products-and-platforms/platforms/android/material-3-expressive-android-wearos-launch/
40. **Discover/feed (if present) is swipe-right from the first page; disabling it leaves no dead page and no placeholder.** If Marlow's feed is a dashboard (Glance-style cards), every card is removable/reorderable and the feed works with no account signed in. https://www.androidpolice.com/i-didnt-expect-microsoft-to-nail-android-launcher/ ; https://unstore.io/discover/best-pixel-launcher-alternatives/

### Cross-cutting

41. **Backup/restore restores the full layout** (home, dock, folders, widgets, icon pack) on a new device — Pixel's absence of this is a known gap, and Marlow should not repeat it. https://www.androidpolice.com/not-fan-of-pixel-launcher/
42. **Cold-start performance:** home screen is interactive in under ~1 s from a cold start on a mid-range device; page swipes and drawer pulls do not drop frames in a screen recording. (Pixel's "responsive across the board" / fluid animations are its most consistently praised trait; a stuttering stock-clone is worse than a slow original.) https://www.androidpolice.com/not-fan-of-pixel-launcher/

---

## 8. The 10 rules most likely to matter for a stock-feeling launcher

1. **First frame is a finished phone.** Cold open shows a complete home — At a Glance with real date/weather, 5-icon dock, populated page 0, search pill. No wizard or blank grid is ever the first thing a user sees. (Rules 1, 37, 38)
2. **One grid, one gutter, one icon size, one label rule — everywhere.** Home, drawer, folders and search results must measure identically. Inconsistency between surfaces is the fastest amateur tell. (Rules 2, 5, 20)
3. **At a Glance is a living strip, not a widget.** No card, no border; date/weather resting state; a calendar event replaces the date line; swiping cycles real complications. (Rules 8–11)
4. **The dock is 5 icons, labels off, on every page; the search pill sits below it on every page.** These two anchors are what make a home screen read as "Pixel" at a glance — decide them deliberately, do not leave them floating. (Rules 3, 4)
5. **The drawer is alphabetical, vertical, and complete — and search is device-first.** No folders-in-drawer by default, no custom order, every app exactly once, typing filters instantly. (Rules 16, 17, 19, 14)
6. **Dots mean presence, never a number; long-press is notifications, then shortcuts, then App info.** A count badge is the Nova tell; a missing shortcut row is the amateur tell. (Rules 29, 31)
7. **Themed icons are all or nothing.** Ship forced theming or do not offer themed icons — a mixed grid reads as a bug, and every launcher that shipped the mix has been criticised for it. (Rule 34)
8. **Folders are drag-to-create, circular 2×2 preview, overlay-open, named at the bottom, never nested.** Any other folder behaviour (full-screen folder page, "New folder" menu, nested folders) is immediately non-stock. (Rules 21–24)
9. **Do not fight the system gesture.** Marlow does not own Home or Recents; it owns the workspace swipe-up to the drawer. Keep the handoff clean and the animations on one spring. (Rules 36, 39)
10. **Theming is automatic first.** Wallpaper → palette everywhere, with no questions asked; manual controls are second, few, and never in front of the home screen. (Rules 33, 35)

---

## 9. Sources

### Google — help, developer docs, blog

- https://support.google.com/pixelphone/answer/14852556 — Find & move your apps (Pixel Phone Help): swipe-up drawer, drag to move/group, folder naming, widget add/resize.
- https://support.google.com/pixelphone/thread/305487540/how-can-i-alphabetize-apps?hl=en — "The master list of apps in the Launcher are alphabetized by default."
- https://support.google.com/pixelphone/thread/251979711/notification-dots-on-icons?hl=en — Notification dots troubleshooting; clearing Pixel Launcher storage resets the home layout.
- https://developer.android.com/develop/ui/views/notifications/badges?authuser=8 — Modify a notification badge: dots since API 26, long-press reveals notifications + shortcuts.
- https://developer.android.com/distribute/aep/aep-req-theme-app-icons?authuser=69 — Themed App Icons guideline: `<monochrome>` layer required.
- https://codelabs.developers.google.com/design-android-launcher — Design and preview your app icons: adaptive colour, themed icons from Android 13.
- https://android-developers.googleblog.com/2013/07/making-beautiful-android-app-icons.html?hl=th — Launcher icons are 48 dp square.
- https://blog.google/products-and-platforms/platforms/android/material-3-expressive-android-wearos-launch/ — Material 3 Expressive launch (May 2025): springy animations, blur, glanceable information.
- https://www.microsoft.com/en-us/garage/wall-of-fame/microsoft-launcher/ — Microsoft Garage: Arrow Launcher history, "forgo any form of customization" early decision, Oct 2017 graduation.

### Pixel Launcher — reviews, teardowns, history

- https://www.androidpolice.com/not-fan-of-pixel-launcher/ — "I love my Pixel phone but not the launcher" (Aug 2025): performance + Material You praised; 5×5 ceiling, immovable At a Glance/search, no folders in drawer, no icon packs, no backup.
- https://9to5google.com/2024/12/25/google-pixel-set-up/ — First things to set up with a new Pixel: At a Glance, app grid 2×2–5×5, app shortcuts.
- https://www.androidauthority.com/pixel-launcher-setup-productivity-3522859/ — Pixel Launcher productivity setup: 5×5, themed icons, suggestions, swipe-up-to-search.
- https://www.androidauthority.com/why-i-stick-with-default-pixel-launcher-3616782/ — Five dock icons + five folders above them (a user's setup, not a factory default).
- https://innotechinsider.com/future-tech/pixel-launcher-wasted-screen-space-android/ — Pixel Launcher wasted space: immovable At a Glance, permanent dock search, 5×5 ceiling.
- https://techcrunch.com/2016/10/04/googles-pixel-smartphones-get-a-new-look-with-pixel-launcher/ — Pixel Launcher launch (Oct 2016): round icons, G tab, draggable shortcuts, swipe-up drawer.
- https://9to5google.com/2018/11/01/google-app-8-39-kills-feed-at-a-glance-widget/ — Feed widget deprecated, replaced by At a Glance.
- https://www.androidcentral.com/google-feed-now-called-discover-and-available-googles-mobile-site — Google Now → Feed → Discover.
- https://www.androidpolice.com/2018/05/14/android-ps-new-overview-app-switcher-part-launcher/ — Overview/Recents is part of the launcher (Quickstep).
- https://www.androidpolice.com/2020/07/09/android-11s-launcher-lets-you-swipe-up-on-secondary-homescreens-to-go-back-to-the-main-one/ — Swipe-up-home vs swipe-up-hold-Recents.
- https://www.androidpolice.com/android-15-dp2-switches-up-pixel-launcher-settings/ — "Swipe up to start search" moves; "Show long app names" / nine-character trim.
- https://www.androidauthority.com/android-15-pixel-launcher-changes-3434365/ — Android 15 Pixel Launcher: categorised widget recommendations, "+ Add" button, Recents pill (Rahman teardown).
- https://www.androidauthority.com/android-16-qpr1-features-3594123/ — Android 16 QPR1: Material 3 Expressive, blur in drawer/Recents/shade, springy animations.
- https://www.androidauthority.com/android-16-best-new-trick-icon-shape-themes-3622140/ — Android 16 QPR2: icon shapes return, forced icon theming.
- https://www.androidauthority.com/android-16-qpr1-beta-2-segmented-search-bar-3567059/ — Segmented search bar (small bar + separate AI Mode button) in QPR1 beta 2.
- https://www.androidauthority.com/pixel-lock-screen-android-16-3546942/ — At a Glance lock-screen placement; "isn't technically a widget".
- https://www.phonearena.com/news/new-pixel-lock-screen-look-includes-changes-for-at-a-glance-placement_id169745 — At a Glance complications list; long-press Dismiss/Settings/About.
- https://www.howtogeek.com/google-is-about-to-fix-one-of-my-least-favorite-pixel-features/ — Canary toggle to remove At a Glance entirely (non-functional when spotted).
- https://mobilesyrup.com/2021/07/23/android-12-ditches-theme-options-shape-icon-font-material-you/ — Android 12 removes font/icon-shape/status-bar options; Google Issue Tracker "Won't Fix (Intended Behaviour)".
- https://www.androidpolice.com/android-12-review/ — Android 12 review: themed icons Beta, only some Google apps; icon shapes/fonts removed.
- https://www.androidheadlines.com/wp-json/wp/v2/posts/1255326 — Google app replacing Pixel Launcher search (Nov 2025, intentional).
- https://eztoolset.com/pixel-launcher-search-now-opens-the-google-app-its-intentional-not-a-bug/ — Timeline of the Nov/Dec 2025 search change.
- https://www.apkmirror.com/apk/google-inc/pixel-launcher/ — Pixel Launcher version history (13–17) and 2016 feature list.
- https://betawiki.net/index.php?title=Launcher_(Android)&direction=next&oldid=448918 — Launcher history Android 9–16.
- https://www.xda-developers.com/tag/google-pixel-launcher/ — XDA Pixel Launcher tag: grid options, swipe-down gesture, Digital Wellbeing pause, Android 11 suggestions/auto-folder-naming.
- https://en.androidayuda.com/widget-pixel-2-android-p-beta/ — At a Glance: event replaces date; calendar/weather integration.
- https://9to5google.com/2025/03/25/pixel-weather-location/ — At a Glance / Pixel Weather location fix.
- https://www.thegoandroid.com/download-new-pixel-2-launcher-no-root-get-pixel-2-features/72332/ — Pixel 2: search to bottom, At a Glance to top; search on all pages.
- https://blog.rottenwifi.com/pixel-launcher-everything-you-need-to-know-about-googles-pixel-home-screen/ — Pixel Launcher guide; "Google's preferred Pixel interface rather than stock Android".
- https://blog.rottenwifi.com/how-to-create-app-folders-on-google-pixel-and-pixel-xl/ — Folders: drag-on-drag, home only, no drawer folders.
- https://tms-outsource.com/blog/posts/android-app-drawer-vs-home-screen/ — Drawer vs home; Nov 2025 search change; suggestions toggle.
- https://www.howtogeek.com/youre-using-the-app-drawer-on-your-android-phone-wrong/ — Auto-show keyboard in drawer (Lawnchair + Pixel "Swipe up to start search").
- https://developer.android.com/develop/ui/views/notifications/badges?authuser=8 — (duplicate, platform doc — listed once above).

### Third-party launchers — reviews, developer writing, changelogs

- https://techwiser.com/nova-launcher-vs-pixel-launcher-whats-the-difference/ — Nova vs Pixel: dock limited to 5 on Pixel, Nova grid/icon/dock/search/drawer control.
- https://unstore.io/discover/best-pixel-launcher-alternatives/ — Best Pixel Launcher alternatives 2026 (tested 7): Nova/Niagara/Lawnchair/Smart/Microsoft comparison table.
- https://www.androidauthority.com/nova-launcher-885296/?amp — Nova Launcher guide: Branch acquisition, layoffs, Barry's Sept 2025 departure; Prime gestures/badges/hide-apps.
- https://www.androidpolice.com/nova-launcher-8-beta-material-you-redesigned-settings-menu/ — Nova 8.0 beta: Material You, redesigned settings, Launcher3 12L rebase.
- https://www.androidpolice.com/nova-launcher-8-stable-update-play-store/ — Nova 8 stable: dynamic theming, Nova Search, Gemini, drawer Cards.
- https://www.androidpolice.com/nova-launcher-802-beta-branch-search/ — Nova 8.0.2 beta: Branch search microresults, opt-in off by default.
- https://www.androidpolice.com/nova-launcher-8018-beta — Nova 8.0.18 beta: single-row search results, dock placement, vertical dock background.
- https://www.eyerys.com/articles/news/nexus-galaxy-nova-launcher-now-a-legacy-android-history — Nova history 2011–2025; Barry asked to stop work and open-sourcing.
- https://mmkthecoolest.gitlab.io/blog/2026-07-28-the-questionable-future-of-nova-launcher/ — Nova future: acquisition, layoffs, Instabridge.
- https://www.appireview.com/bitpit.launcher — Niagara review: Wave Alphabet, inline notifications, pop-ups, widget stacks, 4.8★ / 248k ratings.
- https://www.androidauthority.com/ditched-pixel-ui-for-niagara-launcher-3514424 — "I ditched Pixel UI for Niagara": 8 favourites, monochrome, inline notifications.
- https://www.appcracy.com/app/niagara-launcher-home-screen.html — Niagara first run: select favourites → vertical list.
- https://www.androidpolice.com/niagara-launcher-creator-peter-huber-interview/ — Peter Huber interview: flashlight app → launcher, "competition is every first-party launcher", gesture-API frustrations, Private Space/archiving.
- https://www.androidpolice.com/niagara-launcher-winter-update/ — Niagara winter update: backups, Usage Breaker, devs graduating.
- https://medium.com/@peter-huber — Peter Huber's Medium: "From Flashlight to Home Screen", "A Healthy Home Screen", Anycons, foldables.
- https://4pda.to/forum/index.php?showtopic=916981&st=640 — Niagara changelog: Material You, refusal of native stylised icons.
- https://www.makeuseof.com/why-i-ditched-samsungs-home-screen-for-lawnchair-launcher/ — Lawnchair: Pixel look + Smartspacer, global search, fonts, gestures.
- https://www.androidpolice.com/i-tested-over-20-launchers-but-this-one-made-my-android-phone-feel-like-true-pixel/ — Lawnchair after testing 20+ launchers; dock/grid/drawer/fonts.
- https://lawnchair.app/blog/lawnchair-14/ — Lawnchair 14 blog: QuickSwitch, Smartspacer SDK, theming options.
- https://github.com/LawnchairLauncher/lawnchair/releases — Lawnchair releases: 14/15 betas, drawer folders, Nova import.
- https://github.com/LawnchairLauncher/lawnchair/releases/tag/v15.0.0-beta2 — Lawnchair 15 Beta 2: search-backend redesign, infinite scrolling, drawer-folder reorder.
- https://github.com/LawnchairLauncher/lawnchair/releases/tag/v15.0.0-beta2.1 — Lawnchair 15 Beta 2.1: icon-closing-animation toggle, widget-config fix, Lawnfeed 4.0.
- https://www.axolauncher.com/blog/lawnchair-launcher — Lawnchair 2026: no stable since 2018, three channels, ~14k GitHub stars.
- https://raw.githubusercontent.com/KieronQuinn/Smartspacer/main/FAQ.md — Smartspacer FAQ: enhanced mode needs Shizuku/ADB or root; reconnect per reboot.
- https://www.androidauthority.com/my-favorite-android-launcher-3581681/ — Smart Launcher 6 favourite: auto-categories, smart search, pop-up widgets, frosted glass.
- https://www.appireview.com/ginlemon.flowerfree — Smart Launcher 6: six drawer tabs, Ambient Theme, one-handed lower-third layout, pop-up widgets, hidden vault.
- https://free-apps-android.com/smart-launcher-6/ — Smart Launcher 6: organised from day one, 50M+ downloads, 4.7★.
- https://www.androidpolice.com/smart-launcher-vincenzo-colucci-interview/ — Vincenzo Colucci interview: Flower Launcher origin, PSP/Linux inspiration, three-person team.
- https://play.google.com/store/apps/details?id=ginlemon.flowerfree&hl=en — Smart Launcher 6 Play listing: auto-sort, Ambient theme, one-handed design.
- https://www.androidhire.com/best-android-launcher — Best Android launchers 2026: Smart Launcher 6.6, Nova import, auto-sort.
- https://www.androidpolice.com/i-didnt-expect-microsoft-to-nail-android-launcher/ — Microsoft Launcher: Arrow (2015) → rebrand 2017, Your Feed Glance-only, cards optional/reorderable.
- https://www.appireview.com/com.microsoft.launcher — Microsoft Launcher review: expandable dock, alphabetical drawer with jump bar, 4.7★ / 1.6M ratings.
- https://www.androidauthority.com/microsoft-launcher-pixel-launcher-3541676/ — Microsoft Launcher on a Pixel: 10 gestures, Glance vs Discover.
- https://vectree.io/pdf/c/microsoft-launcher — Microsoft Launcher history: Arrow, v6 rebuild (2020), Cortana removed, Phone Link migration.

### Platform / gesture / iconography

- https://www.howtogeek.com/gestures-dont-work-well-with-android-launchers-and-its-googles-fault/ — QuickStep reserved to stock launcher; Lawnchair swipe-up needing 2–3 attempts.
- https://www.androidpolice.com/third-party-android-launcher-developers-join-forces-voice-frustrations-to-google/ — Smart/Niagara/Nova/Lawnchair joint survey to Google (2024).
- https://android.gadgethacks.com/how-to/android-10-gesture-controls-grayed-out-heres-fix-0200786/ — Android 10 gestures greyed out with a third-party launcher.
- https://techverge.io/download-lawnchair/ — Lawnchair gestures choppy; QuickSwitch needs root.
- https://github.com/ichintansoni/skills-master/blob/HEAD/skills/android/design/technologies/m3-app-icons/SKILL.md — Adaptive icons: 108×108 dp layers, 66 dp safe zone, monochrome silhouette.
- https://github.com/mohamedabdallah-14/prompt-to-asset/blob/HEAD/docs/research/09-app-icon-generation/9b-android-adaptive-themed-icons.md — Adaptive/themed icon geometry; shape masks (circle Pixel, squircle Samsung).
- https://www.axolauncher.com/blog/how-to-change-default-launcher-android — Set-as-default flow since Android 10; Settings → Apps → Default apps → Home app.
- https://aospbooks.github.io/aosp-internal-book/49-launcher3/ — Launcher3 internals (architecture reference; source-level detail is the separate AOSP track's job).

### Gaps and uncertainties (do not present as settled fact)

- **Factory-default dock contents on a clean Pixel first boot** — Google publishes no canonical list; screenshot evidence and fork claims conflict (5 vs 6 items). Verify on a real Pixel before hard-coding a "Pixel default" for Marlow.
- **"Show long app names" / nine-character trim** — reported from an Android 15 DP2 teardown, not Google-documented; treat the trim as observed behaviour, the two-line fix as reported direction.
- **"Tiered widgets"** — a flag spotted in Android 16 DP code; no screenshots, no shipped behaviour at time of writing.
- **At a Glance Canary removal toggle** — spotted in a Canary build, non-functional when spotted; do not design around it shipping.
- **Nova's current status** — sources disagree by date (effectively dead Sept 2025 vs acquired by Instabridge and updating again in 2026). Verify before citing Nova as a live product.
- **Exact first-boot Pixel app set** — see dock gap above; the same caution applies to which widgets (if any) are pre-placed beyond At a Glance.
